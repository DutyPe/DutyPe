import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

// Distance calculator (Haversine formula)
function distanceKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const toRadians = (degrees: number) => (degrees * Math.PI) / 180;
  const earthRadiusKm = 6371;

  const dLat = toRadians(lat2 - lat1);
  const dLng = toRadians(lng2 - lng1);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(lat1)) *
      Math.cos(toRadians(lat2)) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadiusKm * c;
}

// OpenStreetMap geocoder fallback
async function geocodeAddress(query: string): Promise<{ lat: number; lng: number } | null> {
  try {
    const cleanQuery = query
      .replace(/jobs\s+(in|near|at|around)\s+/i, "")
      .replace(/jobs/i, "")
      .trim();

    if (!cleanQuery) return null;

    const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(
      cleanQuery
    )}&format=json&limit=1`;

    const response = await fetch(url, {
      headers: {
        "User-Agent": "DutyPe-WhatsApp-Chatbot/1.0 (support@dutype.in)",
      },
    });

    if (!response.ok) {
      console.error("OSM Geocoding request failed status:", response.status);
      return null;
    }

    const data = (await response.json()) as any[];
    if (data && data.length > 0) {
      const lat = parseFloat(data[0].lat);
      const lng = parseFloat(data[0].lon);
      if (!isNaN(lat) && !isNaN(lng)) {
        return { lat, lng };
      }
    }
  } catch (error) {
    console.error("Error during address geocoding:", error);
  }
  return null;
}

// Send Message back via WhatsApp Business Cloud API
async function sendWhatsAppMessage(
  to: string,
  text: string,
  phoneNumberId: string,
  accessToken: string
) {
  try {
    const url = `https://graph.facebook.com/v18.0/${phoneNumberId}/messages`;
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        messaging_product: "whatsapp",
        recipient_type: "individual",
        to: to,
        type: "text",
        text: { body: text },
      }),
    });

    if (!response.ok) {
      const errBody = await response.text();
      console.error("Failed to send WhatsApp message. Response:", errBody);
    } else {
      console.log(`Message successfully sent to ${to}`);
    }
  } catch (error) {
    console.error("Error sending WhatsApp message:", error);
  }
}

// Fetch nearby jobs from Firestore and format response
async function getNearbyJobsResponse(lat: number, lng: number): Promise<string> {
  try {
    const db = getFirebaseAdminDb();
    const jobsSnap = await db
      .collection("jobmetadata")
      .where("status", "==", "ACTIVE")
      .limit(100)
      .get();

    const jobs: Array<{
      title: string;
      employerName?: string;
      locationName?: string;
      salary?: string;
      id: string;
      distance: number;
    }> = [];

    jobsSnap.forEach((doc) => {
      const data = doc.data();
      const jobLat = typeof data.latitude === "number" ? data.latitude : null;
      const jobLng = typeof data.longitude === "number" ? data.longitude : null;

      if (jobLat !== null && jobLng !== null) {
        const distance = distanceKm(lat, lng, jobLat, jobLng);
        if (distance <= 15) {
          jobs.push({
            title: data.title || "Job Opening",
            employerName: data.companyName || data.employerName || "Verified Employer",
            locationName: data.locality || data.city || "Nearby Area",
            salary: data.salaryRange || data.salary || "Best in industry",
            id: doc.id,
            distance: Math.round(distance * 10) / 10,
          });
        }
      }
    });

    jobs.sort((a, b) => a.distance - b.distance);

    if (jobs.length === 0) {
      return "😔 No active jobs found within 15 km of your location. We are constantly expanding! Please try searching for a different area.";
    }

    const topJobs = jobs.slice(0, 5);

    let responseText = `💼 Found ${jobs.length} Job(s) near you! Here are the top matches:\n\n`;
    topJobs.forEach((job, index) => {
      responseText += `${index + 1}. *${job.title}* (${job.distance} km away)\n`;
      responseText += `   🏢 ${job.employerName}\n`;
      responseText += `   📍 ${job.locationName}\n`;
      if (job.salary) responseText += `   💰 ${job.salary}\n`;
      responseText += `   🔗 Apply: https://dutype.in/jobs/${job.id}\n\n`;
    });

    if (jobs.length > 5) {
      responseText += `👉 View the rest of the matching jobs directly on our website: https://dutype.in/jobs\n\n`;
    }

    responseText += "No agent fees, no hidden charges. Apply directly!";
    return responseText;
  } catch (error) {
    console.error("Error fetching nearby jobs:", error);
    return "⚠️ Sorry, we encountered an error while searching for jobs. Please try again in a few moments.";
  }
}

// Ask Gemini AI (Free Tier) to answer conversational queries
async function askGemini(messageText: string): Promise<string> {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) return "";

  try {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`;
    const systemInstruction =
      "You are the official WhatsApp assistant for DutyPe, a 100% free local job portal in India connecting workers (delivery riders, drivers, helpers, maids, cooks, security guards) directly with employers with no agent fees.\n" +
      "Answer the user's message politely in a friendly, conversational manner. Keep your response extremely brief (max 2-3 sentences).\n" +
      "* If they are looking for jobs: invite them to share their location pin using WhatsApp's attach button (📎 or + -> Location -> Send Current Location) or type their location (e.g. 'jobs in Madhapur') so you can fetch nearby jobs for them.\n" +
      "* If they ask about fees: remind them that DutyPe is 100% free for all workers.\n" +
      "* Do NOT invent job listings. Always point them to search active jobs in their location.";

    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        contents: [
          {
            parts: [
              {
                text: `${systemInstruction}\n\nUser Message: ${messageText}`,
              },
            ],
          },
        ],
      }),
    });

    if (response.ok) {
      const data = (await response.json()) as any;
      const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;
      if (text) {
        return text.trim();
      }
    } else {
      const errText = await response.text();
      console.error("Gemini API call failed with response:", errText);
    }
  } catch (error) {
    console.error("Error during Gemini API call:", error);
  }
  return "";
}

// Custom help replies (OTP, Login, etc.)
function getCustomHelpReply(text: string): string | null {
  const cleanText = text.toLowerCase();

  // OTP issue check
  if (
    cleanText.includes("otp") ||
    cleanText.includes("verification code") ||
    cleanText.includes("verification") ||
    cleanText.includes("code") ||
    cleanText.includes("received code") ||
    cleanText.includes("get code")
  ) {
    return "📲 *OTP Verification Assistance*\n\n" +
           "If you are not receiving the OTP code, please try these quick steps:\n\n" +
           "1. Re-open/restart the DutyPe app on your phone.\n" +
           "2. Double-check that your phone number is entered correctly.\n" +
           "3. Ensure your mobile network signal is strong to receive SMS.\n" +
           "4. Wait 60 seconds and tap **Resend OTP**.\n\n" +
           "Let us know if you still face issues!";
  }

  // Login issue check
  if (
    cleanText.includes("login") ||
    cleanText.includes("log in") ||
    cleanText.includes("signin") ||
    cleanText.includes("sign in") ||
    cleanText.includes("unable to login") ||
    cleanText.includes("unable to sign in") ||
    cleanText.includes("can't login") ||
    cleanText.includes("cannot login") ||
    cleanText.includes("account issue")
  ) {
    return "🔑 *Login / Account Assistance*\n\n" +
           "If you are having trouble logging in, please check if you have created an account first:\n\n" +
           "1. Open the DutyPe app.\n" +
           "2. Click the **Create New Account** (Register/Sign Up) button instead of Login.\n" +
           "3. Fill in your details to create your profile.\n" +
           "4. If you already registered, ensure you are logging in with the same mobile number you signed up with.\n\n" +
           "This fixes login issues for 99% of our users!";
  }

  return null;
}

// Next.js API GET Handler (Verification)
export async function GET(request: Request) {
  const verifyToken = process.env.WHATSAPP_VERIFY_TOKEN ?? "DUTYPE_VERIFY_TOKEN_2026";
  
  const { searchParams } = new URL(request.url);
  const mode = searchParams.get("hub.mode");
  const token = searchParams.get("hub.verify_token");
  const challenge = searchParams.get("hub.challenge");

  if (mode === "subscribe" && token === verifyToken) {
    console.log("Webhook verified successfully by Meta via Next.js API.");
    return new Response(challenge, { status: 200 });
  } else {
    console.warn("Webhook verification failed: invalid token or mode.");
    return new Response("Forbidden", { status: 403 });
  }
}

// Next.js API POST Handler (Supports both Meta Webhook and AutoResponder Webserver)
export async function POST(request: Request) {
  const verifyToken = process.env.WHATSAPP_VERIFY_TOKEN ?? "DUTYPE_VERIFY_TOKEN_2026";
  const accessToken = process.env.WHATSAPP_ACCESS_TOKEN;
  const phoneNumberId = process.env.WHATSAPP_PHONE_NUMBER_ID;

  let isAutoResponder = false;

  try {
    const body = await request.json();

    // ============================================
    // CASE A: AutoResponder for WhatsApp Integration
    // ============================================
    if (body.query || body.message || body.sender) {
      isAutoResponder = true;

      // Extract message text safely to prevent type errors
      let bodyText = "";
      if (typeof body.message === "string") {
        bodyText = body.message;
      } else if (body.message && typeof body.message === "object" && typeof body.message.text === "string") {
        bodyText = body.message.text;
      } else if (typeof body.query === "string") {
        bodyText = body.query;
      } else if (body.query && typeof body.query === "object" && typeof body.query.text === "string") {
        bodyText = body.query.text;
      } else if (body.message) {
        bodyText = String(body.message);
      } else if (body.query) {
        bodyText = String(body.query);
      }
      bodyText = bodyText.trim();

      const sender = body.sender || "User";
      console.log(`Received AutoResponder request from ${sender}: "${bodyText}"`);

      // Check for custom help replies (OTP, Login, etc.)
      const helpReply = getCustomHelpReply(bodyText);
      if (helpReply) {
        return Response.json({
          replies: [{ message: helpReply }]
        });
      }

      // Check if message is a Google Maps location share (e.g., https://maps.google.com/?q=17.4483,78.3741)
      const mapsMatch = bodyText.match(/q=(-?\d+\.\d+)(?:,|%2C)(-?\d+\.\d+)/i);

      if (mapsMatch) {
        const lat = parseFloat(mapsMatch[1]);
        const lng = parseFloat(mapsMatch[2]);
        console.log(`AutoResponder parsed shared location: lat=${lat}, lng=${lng}`);
        
        const reply = await getNearbyJobsResponse(lat, lng);
        return Response.json({
          replies: [{ message: reply }]
        });
      }

      // Check if user is asking for jobs in a specific location
      const isJobRequest = /job/i.test(bodyText);
      const hasLocationKeyword = /(in|near|at|around|inside|locality|secunderabad|hyderabad|delhi|mumbai|bangalore|pune|noida|gurgaon|chennai|kolkata)/i.test(
        bodyText
      );

      if (isJobRequest && hasLocationKeyword) {
        const coords = await geocodeAddress(bodyText);
        if (coords) {
          const reply = await getNearbyJobsResponse(coords.lat, coords.lng);
          return Response.json({
            replies: [{ message: reply }]
          });
        } else {
          return Response.json({
            replies: [{
              message: "📍 Sorry, we couldn't pinpoint that location. Please share your location using the WhatsApp attach button (Location ➔ Send Your Current Location), or type a clearer location (e.g., 'jobs in Madhapur, Hyderabad')."
            }]
          });
        }
      }

      // General AI Greeting / Conversational Fallback
      let reply = "";
      if (process.env.GEMINI_API_KEY) {
        console.log("Calling Gemini AI to generate friendly reply for AutoResponder...");
        reply = await askGemini(bodyText);
      }

      if (!reply) {
        reply =
          "👋 Welcome to DutyPe Job Portal Support!\n\n" +
          "To instantly find active jobs near you without any agent fees:\n\n" +
          "1. 📍 *Share your Location pin* directly in this chat: tap the attach button (📎 or +) -> select *Location* -> send *Your Current Location*.\n\n" +
          "2. ✍️ Or type a specific location query, e.g., *'jobs in Madhapur'* or *'jobs near Kondapur'*.\n\n" +
          "We will instantly reply with matching jobs in your area!";
      }

      return Response.json({
        replies: [{ message: reply }]
      });
    }

    // ============================================
    // CASE B: Meta WhatsApp Cloud API Webhook Integration
    // ============================================
    if (body.object === "whatsapp_business_account") {
      if (!accessToken || !phoneNumberId) {
        console.error("Missing WHATSAPP_ACCESS_TOKEN or WHATSAPP_PHONE_NUMBER_ID in env.");
        return new Response("Env config missing", { status: 200 });
      }

      const entries = body.entry || [];
      for (const entry of entries) {
        const changes = entry.changes || [];
        for (const change of changes) {
          const value = change.value || {};
          const messages = value.messages || [];

          for (const message of messages) {
            const from = message.from;
            const messageId = message.id;

            console.log(`Received Meta WhatsApp event ${messageId} from ${from}`);

            if (message.type === "location" && message.location) {
              const lat = message.location.latitude;
              const lng = message.location.longitude;

              console.log(`User ${from} shared location: lat=${lat}, lng=${lng}`);
              const reply = await getNearbyJobsResponse(lat, lng);
              await sendWhatsAppMessage(from, reply, phoneNumberId, accessToken);
            }
            else if (message.type === "text" && message.text) {
              const bodyText = message.text.body ? message.text.body.trim() : "";
              console.log(`User ${from} sent text: "${bodyText}"`);

              // Check for custom help replies (OTP, Login, etc.)
              const helpReply = getCustomHelpReply(bodyText);
              if (helpReply) {
                await sendWhatsAppMessage(from, helpReply, phoneNumberId, accessToken);
                continue;
              }

              const isJobRequest = /job/i.test(bodyText);
              const hasLocationKeyword = /(in|near|at|around|inside|locality|secunderabad|hyderabad|delhi|mumbai|bangalore|pune|noida|gurgaon|chennai|kolkata)/i.test(
                bodyText
              );

              if (isJobRequest && hasLocationKeyword) {
                await sendWhatsAppMessage(
                  from,
                  "🔍 Searching for jobs near the specified location...",
                  phoneNumberId,
                  accessToken
                );

                const coords = await geocodeAddress(bodyText);
                if (coords) {
                  const reply = await getNearbyJobsResponse(coords.lat, coords.lng);
                  await sendWhatsAppMessage(from, reply, phoneNumberId, accessToken);
                } else {
                  await sendWhatsAppMessage(
                    from,
                    "📍 Sorry, we couldn't pinpoint that location. Please share your location using the WhatsApp attach button (📎 or + -> Location), or type a clearer location (e.g., 'jobs in Madhapur, Hyderabad').",
                    phoneNumberId,
                    accessToken
                  );
                }
              } else {
                let reply = "";
                if (process.env.GEMINI_API_KEY) {
                  console.log("Calling Gemini AI to generate friendly reply...");
                  reply = await askGemini(bodyText);
                }

                if (!reply) {
                  reply =
                    "👋 Welcome to DutyPe Job Portal Support!\n\n" +
                    "To instantly find active jobs near you without any agent fees:\n\n" +
                    "1. 📍 *Share your Location pin* directly in this chat: tap the attach button (📎 or +) -> select *Location* -> send *Your Current Location*.\n\n" +
                    "2. ✍️ Or type a specific location query, e.g., *'jobs in Madhapur'* or *'jobs near Kondapur'*.\n\n" +
                    "We will instantly reply with matching jobs in your area!";
                }

                await sendWhatsAppMessage(from, reply, phoneNumberId, accessToken);
              }
            }
          }
        }
      }
    }
  } catch (error) {
    console.error("Error processing Next.js webhook body:", error);
    if (isAutoResponder) {
      return Response.json({
        replies: [{ message: "⚠️ Sorry, we encountered an error while processing your request. Please try again." }]
      });
    }
  }

  if (isAutoResponder) {
    return Response.json({
      replies: [{ message: "👋 Hello! Please send a text query or location pin to search for jobs." }]
    });
  }

  return Response.json({ status: "success", message: "EVENT_RECEIVED" });
}

