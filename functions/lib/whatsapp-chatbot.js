"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.sendEmployerWhatsAppCheckIn = exports.whatsappWebhook = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const db = admin.firestore();
// Distance calculator (Haversine formula)
function distanceKm(lat1, lng1, lat2, lng2) {
    const toRadians = (degrees) => (degrees * Math.PI) / 180;
    const earthRadiusKm = 6371;
    const dLat = toRadians(lat2 - lat1);
    const dLng = toRadians(lng2 - lng1);
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(toRadians(lat1)) *
            Math.cos(toRadians(lat2)) *
            Math.sin(dLng / 2) *
            Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadiusKm * c;
}
// OpenStreetMap geocoder fallback
async function geocodeAddress(query) {
    try {
        // Remove "jobs in" or "jobs near" prefixes to make query clean
        const cleanQuery = query
            .replace(/jobs\s+(in|near|at|around)\s+/i, "")
            .replace(/jobs/i, "")
            .trim();
        if (!cleanQuery)
            return null;
        const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(cleanQuery)}&format=json&limit=1`;
        const response = await fetch(url, {
            headers: {
                "User-Agent": "DutyPe-WhatsApp-Chatbot/1.0 (support@dutype.in)",
            },
        });
        if (!response.ok) {
            functions.logger.error("OSM Geocoding request failed status:", response.status);
            return null;
        }
        const data = (await response.json());
        if (data && data.length > 0) {
            const lat = parseFloat(data[0].lat);
            const lng = parseFloat(data[0].lon);
            if (!isNaN(lat) && !isNaN(lng)) {
                return { lat, lng };
            }
        }
    }
    catch (error) {
        functions.logger.error("Error during address geocoding:", error);
    }
    return null;
}
// Send Message back via WhatsApp Business Cloud API
async function sendWhatsAppMessage(to, text, phoneNumberId, accessToken) {
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
            functions.logger.error("Failed to send WhatsApp message. Response:", errBody);
        }
        else {
            functions.logger.info(`Message successfully sent to ${to}`);
        }
    }
    catch (error) {
        functions.logger.error("Error sending WhatsApp message:", error);
    }
}
// Ask Gemini AI (Free Tier) to answer conversational queries
async function askGemini(messageText) {
    var _a, _b, _c, _d, _e;
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey)
        return "";
    try {
        const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`;
        const systemInstruction = "You are the official WhatsApp assistant for DutyPe, a 100% free local job portal in India connecting workers (delivery riders, drivers, helpers, maids, cooks, security guards) directly with employers with no agent fees.\n" +
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
            const data = (await response.json());
            const text = (_e = (_d = (_c = (_b = (_a = data === null || data === void 0 ? void 0 : data.candidates) === null || _a === void 0 ? void 0 : _a[0]) === null || _b === void 0 ? void 0 : _b.content) === null || _c === void 0 ? void 0 : _c.parts) === null || _d === void 0 ? void 0 : _d[0]) === null || _e === void 0 ? void 0 : _e.text;
            if (text) {
                return text.trim();
            }
        }
        else {
            const errText = await response.text();
            functions.logger.error("Gemini API call failed with response:", errText);
        }
    }
    catch (error) {
        functions.logger.error("Error during Gemini API call:", error);
    }
    return "";
}
// Fetch nearby jobs from Firestore and format response
async function getNearbyJobsResponse(lat, lng) {
    try {
        // Query active jobs (limit to 100 to prevent loading too much data into memory)
        const jobsSnap = await db
            .collection("jobmetadata")
            .where("status", "==", "ACTIVE")
            .limit(100)
            .get();
        const jobs = [];
        jobsSnap.forEach((doc) => {
            const data = doc.data();
            const jobLat = typeof data.latitude === "number" ? data.latitude : null;
            const jobLng = typeof data.longitude === "number" ? data.longitude : null;
            if (jobLat !== null && jobLng !== null) {
                const distance = distanceKm(lat, lng, jobLat, jobLng);
                // Filter jobs within 15 km radius
                if (distance <= 15) {
                    jobs.push({
                        title: data.title || "Job Opening",
                        employerName: data.companyName || data.employerName || "Verified Employer",
                        locationName: data.locality || data.city || "Nearby Area",
                        salary: data.salaryRange || data.salary || "Best in industry",
                        id: doc.id,
                        distance: Math.round(distance * 10) / 10, // Round to 1 decimal place
                    });
                }
            }
        });
        // Sort jobs by proximity (closest first)
        jobs.sort((a, b) => a.distance - b.distance);
        if (jobs.length === 0) {
            return "😔 No active jobs found within 15 km of your location. We are constantly expanding! Please try searching for a different area.";
        }
        // Limit to top 5 jobs
        const topJobs = jobs.slice(0, 5);
        let responseText = `💼 Found ${jobs.length} Job(s) near you! Here are the top matches:\n\n`;
        topJobs.forEach((job, index) => {
            responseText += `${index + 1}. *${job.title}* (${job.distance} km away)\n`;
            responseText += `   🏢 ${job.employerName}\n`;
            responseText += `   📍 ${job.locationName}\n`;
            if (job.salary)
                responseText += `   💰 ${job.salary}\n`;
            responseText += `   🔗 Apply: https://dutype.in/jobs/${job.id}\n\n`;
        });
        if (jobs.length > 5) {
            responseText += `👉 View the rest of the matching jobs directly on our website: https://dutype.in/jobs\n\n`;
        }
        responseText += "No agent fees, no hidden charges. Apply directly!";
        return responseText;
    }
    catch (error) {
        functions.logger.error("Error fetching nearby jobs:", error);
        return "⚠️ Sorry, we encountered an error while searching for jobs. Please try again in a few moments.";
    }
}
// Webhook HTTP handler
exports.whatsappWebhook = functions.https.onRequest(async (req, res) => {
    var _a;
    const verifyToken = (_a = process.env.WHATSAPP_VERIFY_TOKEN) !== null && _a !== void 0 ? _a : "DUTYPE_VERIFY_TOKEN_2026";
    const accessToken = process.env.WHATSAPP_ACCESS_TOKEN;
    const phoneNumberId = process.env.WHATSAPP_PHONE_NUMBER_ID;
    // Handle Webhook Verification (GET request)
    if (req.method === "GET") {
        const mode = req.query["hub.mode"];
        const token = req.query["hub.verify_token"];
        const challenge = req.query["hub.challenge"];
        if (mode === "subscribe" && token === verifyToken) {
            functions.logger.info("Webhook verified successfully by Meta.");
            res.status(200).send(challenge);
            return;
        }
        else {
            functions.logger.warn("Webhook verification failed: invalid token or mode.");
            res.status(403).send("Forbidden");
            return;
        }
    }
    // Handle incoming message events (POST request)
    if (req.method === "POST") {
        const body = req.body;
        if (!accessToken || !phoneNumberId) {
            functions.logger.error("Missing WHATSAPP_ACCESS_TOKEN or WHATSAPP_PHONE_NUMBER_ID env vars.");
            res.status(200).send("Env config missing");
            return;
        }
        if (body.object === "whatsapp_business_account") {
            try {
                const entries = body.entry || [];
                for (const entry of entries) {
                    const changes = entry.changes || [];
                    for (const change of changes) {
                        const value = change.value || {};
                        const messages = value.messages || [];
                        for (const message of messages) {
                            const from = message.from; // Sender phone number
                            const messageId = message.id;
                            functions.logger.info(`Received event ${messageId} from ${from}`);
                            // Handle Location Share Message
                            if (message.type === "location" && message.location) {
                                const lat = message.location.latitude;
                                const lng = message.location.longitude;
                                functions.logger.info(`User ${from} shared location: lat=${lat}, lng=${lng}`);
                                const reply = await getNearbyJobsResponse(lat, lng);
                                await sendWhatsAppMessage(from, reply, phoneNumberId, accessToken);
                            }
                            // Handle Interactive Button Reply (Employer Check-in & Stop Calls)
                            else if (message.type === "interactive" && message.interactive) {
                                const interactive = message.interactive;
                                const buttonReply = interactive.button_reply;
                                const buttonId = (buttonReply === null || buttonReply === void 0 ? void 0 : buttonReply.id) || "";
                                functions.logger.info(`User ${from} clicked interactive button: ${buttonId}`);
                                if (buttonId.startsWith("HIRED_STOP_")) {
                                    const parts = buttonId.replace("HIRED_STOP_", "").split("_");
                                    const jobId = parts[0];
                                    const applicationId = parts[1];
                                    if (applicationId) {
                                        await db.collection("applications").doc(applicationId).set({
                                            status: "HIRED",
                                            notes: "Hired via WhatsApp check-in (calls stopped)",
                                            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                                        }, { merge: true });
                                    }
                                    if (jobId) {
                                        const jobUpdates = {
                                            status: "filled",
                                            callsStopped: true,
                                            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                                        };
                                        await db.collection("jobs").doc(jobId).set(jobUpdates, { merge: true });
                                        await db.collection("jobmetadata").doc(jobId).set(jobUpdates, { merge: true });
                                        await db.collection("job_details").doc(jobId).set(jobUpdates, { merge: true });
                                    }
                                    const reply = "🎉 Mubarak! Candidate marked as HIRED on DutyPe.\n\n" +
                                        "🛑 *Incoming calls STOPPED.* You will no longer receive phone calls from other workers for this position.\n\n" +
                                        "If you ever need to hire again, you can reopen this job anytime on the DutyPe app or reply *REOPEN* here.";
                                    await sendWhatsAppMessage(from, reply, phoneNumberId, accessToken);
                                }
                                else if (buttonId.startsWith("HIRED_KEEP_")) {
                                    const parts = buttonId.replace("HIRED_KEEP_", "").split("_");
                                    const applicationId = parts[1];
                                    if (applicationId) {
                                        await db.collection("applications").doc(applicationId).set({
                                            status: "HIRED",
                                            notes: "Hired via WhatsApp check-in (keep open)",
                                            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                                        }, { merge: true });
                                    }
                                    const reply = "🎉 Candidate marked as HIRED on DutyPe!\n\n" +
                                        "📞 Your job remains *ACTIVE & OPEN* to receive more candidate calls for additional vacancies.";
                                    await sendWhatsAppMessage(from, reply, phoneNumberId, accessToken);
                                }
                                else if (buttonId.startsWith("NOT_HIRED_")) {
                                    const parts = buttonId.replace("NOT_HIRED_", "").split("_");
                                    const applicationId = parts[1];
                                    if (applicationId) {
                                        await db.collection("applications").doc(applicationId).set({
                                            status: "REJECTED",
                                            notes: "Not suitable via WhatsApp check-in",
                                            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                                        }, { merge: true });
                                    }
                                    const reply = "Got it! Feedback recorded. 👍\n\nDutyPe is matching other qualified local candidates for your opening.";
                                    await sendWhatsAppMessage(from, reply, phoneNumberId, accessToken);
                                }
                            }
                            // Handle Text Message
                            else if (message.type === "text" && message.text) {
                                const bodyText = message.text.body ? message.text.body.trim() : "";
                                functions.logger.info(`User ${from} sent text: "${bodyText}"`);
                                const cleanBody = bodyText.toLowerCase().trim();
                                // 1. Reopen job via WhatsApp command
                                if (cleanBody === "reopen") {
                                    const checkinSnap = await db
                                        .collection("whatsapp_employer_checkins")
                                        .where("employerPhone", "==", from)
                                        .orderBy("createdAt", "desc")
                                        .limit(1)
                                        .get();
                                    if (!checkinSnap.empty) {
                                        const cData = checkinSnap.docs[0].data();
                                        if (cData.jobId) {
                                            const reopenUpdates = {
                                                status: "open",
                                                callsStopped: false,
                                                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                                            };
                                            await db.collection("jobs").doc(cData.jobId).set(reopenUpdates, { merge: true });
                                            await db.collection("jobmetadata").doc(cData.jobId).set(reopenUpdates, { merge: true });
                                            await db.collection("job_details").doc(cData.jobId).set(reopenUpdates, { merge: true });
                                            await sendWhatsAppMessage(from, "✅ Your job has been REOPENED! Workers can now call you again.", phoneNumberId, accessToken);
                                            continue;
                                        }
                                    }
                                }
                                // 2. Fallback numbered replies for check-ins (1, 2, 3)
                                if (cleanBody === "1" || cleanBody === "2" || cleanBody === "3" || cleanBody.includes("hired") || cleanBody.includes("not hired")) {
                                    const pendingCheckinSnap = await db
                                        .collection("whatsapp_employer_checkins")
                                        .where("employerPhone", "==", from)
                                        .where("status", "==", "PENDING")
                                        .orderBy("createdAt", "desc")
                                        .limit(1)
                                        .get();
                                    if (!pendingCheckinSnap.empty) {
                                        const checkinDoc = pendingCheckinSnap.docs[0];
                                        const checkin = checkinDoc.data();
                                        const isOption1 = cleanBody === "1" || cleanBody.includes("stop");
                                        const isOption2 = cleanBody === "2" || cleanBody.includes("keep");
                                        if (isOption1) {
                                            if (checkin.applicationId) {
                                                await db.collection("applications").doc(checkin.applicationId).set({ status: "HIRED", notes: "Hired via WhatsApp (calls stopped)", updatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
                                            }
                                            if (checkin.jobId) {
                                                const jobUpdates = { status: "filled", callsStopped: true, updatedAt: admin.firestore.FieldValue.serverTimestamp() };
                                                await db.collection("jobs").doc(checkin.jobId).set(jobUpdates, { merge: true });
                                                await db.collection("jobmetadata").doc(checkin.jobId).set(jobUpdates, { merge: true });
                                                await db.collection("job_details").doc(checkin.jobId).set(jobUpdates, { merge: true });
                                            }
                                            await checkinDoc.ref.update({ status: "COMPLETED_HIRED_STOP", respondedAt: admin.firestore.FieldValue.serverTimestamp() });
                                            await sendWhatsAppMessage(from, "🎉 Mubarak! Candidate marked as HIRED. Incoming calls STOPPED. Reply REOPEN anytime if you need to hire again.", phoneNumberId, accessToken);
                                            continue;
                                        }
                                        else if (isOption2) {
                                            if (checkin.applicationId) {
                                                await db.collection("applications").doc(checkin.applicationId).set({ status: "HIRED", notes: "Hired via WhatsApp (keep open)", updatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
                                            }
                                            await checkinDoc.ref.update({ status: "COMPLETED_HIRED_KEEP", respondedAt: admin.firestore.FieldValue.serverTimestamp() });
                                            await sendWhatsAppMessage(from, "🎉 Candidate marked as HIRED! Your job remains OPEN for more calls.", phoneNumberId, accessToken);
                                            continue;
                                        }
                                        else {
                                            if (checkin.applicationId) {
                                                await db.collection("applications").doc(checkin.applicationId).set({ status: "REJECTED", notes: "Not suitable via WhatsApp", updatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
                                            }
                                            await checkinDoc.ref.update({ status: "COMPLETED_NOT_HIRED", respondedAt: admin.firestore.FieldValue.serverTimestamp() });
                                            await sendWhatsAppMessage(from, "Got it! Feedback recorded. We'll keep sending you more qualified candidates.", phoneNumberId, accessToken);
                                            continue;
                                        }
                                    }
                                }
                                const isJobRequest = /job/i.test(bodyText);
                                const hasLocationKeyword = /(in|near|at|around|inside|locality|secunderabad|hyderabad|delhi|mumbai|bangalore|pune|noida|gurgaon|chennai|kolkata)/i.test(bodyText);
                                if (isJobRequest && hasLocationKeyword) {
                                    // User asked for jobs in a specific location
                                    await sendWhatsAppMessage(from, "🔍 Searching for jobs near the specified location...", phoneNumberId, accessToken);
                                    const coords = await geocodeAddress(bodyText);
                                    if (coords) {
                                        const reply = await getNearbyJobsResponse(coords.lat, coords.lng);
                                        await sendWhatsAppMessage(from, reply, phoneNumberId, accessToken);
                                    }
                                    else {
                                        await sendWhatsAppMessage(from, "📍 Sorry, we couldn't pinpoint that location. Please share your location using the WhatsApp attach button (📎 or + -> Location), or type a clearer location (e.g., 'jobs in Madhapur, Hyderabad').", phoneNumberId, accessToken);
                                    }
                                }
                                else {
                                    let reply = "";
                                    if (process.env.GEMINI_API_KEY) {
                                        functions.logger.info("Calling Gemini AI to generate friendly reply...");
                                        reply = await askGemini(bodyText);
                                    }
                                    if (!reply) {
                                        // General Help / Greeting Message Fallback
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
            catch (err) {
                functions.logger.error("Error processing webhook payload:", err);
            }
            res.status(200).send("EVENT_RECEIVED");
            return;
        }
        res.status(404).send("Not Found");
        return;
    }
    res.status(405).send("Method Not Allowed");
});
// Helper to send WhatsApp Employer Check-In
async function sendEmployerWhatsAppCheckIn(toPhone, workerName, jobTitle, jobId, applicationId, phoneNumberId, accessToken) {
    const token = accessToken || process.env.WHATSAPP_ACCESS_TOKEN;
    const phoneId = phoneNumberId || process.env.WHATSAPP_PHONE_NUMBER_ID;
    if (!token || !phoneId) {
        functions.logger.warn("WhatsApp credentials not configured for check-in.");
        return false;
    }
    const cleanDigits = toPhone.replace(/\D/g, "");
    const formattedPhone = cleanDigits.length === 10 ? `91${cleanDigits}` : cleanDigits;
    await db.collection("whatsapp_employer_checkins").doc(`${formattedPhone}_${applicationId}`).set({
        employerPhone: formattedPhone,
        workerName,
        jobTitle,
        jobId,
        applicationId,
        status: "PENDING",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    const url = `https://graph.facebook.com/v18.0/${phoneId}/messages`;
    const body = {
        messaging_product: "whatsapp",
        recipient_type: "individual",
        to: formattedPhone,
        type: "interactive",
        interactive: {
            type: "button",
            body: {
                text: `Namaste! 🙏 Yesterday *${workerName}* contacted you regarding *${jobTitle}* on DutyPe.\n\nDid you hire them?`,
            },
            action: {
                buttons: [
                    {
                        type: "reply",
                        reply: {
                            id: `HIRED_STOP_${jobId}_${applicationId}`,
                            title: "✅ Hired (Stop Calls)",
                        },
                    },
                    {
                        type: "reply",
                        reply: {
                            id: `HIRED_KEEP_${jobId}_${applicationId}`,
                            title: "🤝 Hired (Keep Open)",
                        },
                    },
                    {
                        type: "reply",
                        reply: {
                            id: `NOT_HIRED_${jobId}_${applicationId}`,
                            title: "❌ Not Suitable",
                        },
                    },
                ],
            },
        },
    };
    try {
        const res = await fetch(url, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(body),
        });
        if (!res.ok) {
            // Fallback text message if interactive message fails
            const fallbackText = `Namaste! 🙏 Yesterday *${workerName}* contacted you regarding *${jobTitle}* on DutyPe.\n\n` +
                `Did you hire them? Reply with a number:\n` +
                `1️⃣ - Hired (Stop calls)\n` +
                `2️⃣ - Hired (Keep open for more)\n` +
                `3️⃣ - Not Suitable`;
            await sendWhatsAppMessage(formattedPhone, fallbackText, phoneId, token);
        }
        return true;
    }
    catch (err) {
        functions.logger.error("Failed to send WhatsApp check-in:", err);
        return false;
    }
}
exports.sendEmployerWhatsAppCheckIn = sendEmployerWhatsAppCheckIn;
//# sourceMappingURL=whatsapp-chatbot.js.map