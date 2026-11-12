import { NextRequest, NextResponse } from "next/server";
import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { RateLimitTiers } from "@/lib/security/admin-rate-limiter";

export const runtime = "nodejs";

const SYSTEM_PROMPT = `You are an expert OCR and job information extraction AI for DutyPe, a verified local job platform in Telangana & South India.
Analyze this job poster, flyer, banner, newspaper clipping, or notice board photo carefully. The text may be in Telugu, English, or a mix.

Extract the information accurately and return ONLY a valid JSON object with the following fields:
- "title": (string) Clear, standard job title in English (e.g. "Shop Helper", "Cook / Kitchen Helper", "Delivery Boy", "Driver", "Security Guard", "Billing Cashier", "Tailor", "Sales Executive")
- "companyName": (string) Shop, hotel, business, or firm name
- "vacancies": (number) Total number of open positions mentioned (integer, default 1 if unspecified)
- "salary": (number) Base pay amount in INR as a clean integer without symbols (e.g. 9000, 15000). Set to 0 if negotiable or unstated
- "salaryType": (string) "MONTHLY" or "WEEKLY" (default "MONTHLY")
- "contactNumber": (string) 10-digit Indian mobile number extracted from the image
- "addressText": (string) Full address including street, landmark, market/bazar, city/town, and state (e.g. "Kaman Bazar, Khammam, Telangana")
- "description": (string) Concise, factual, honest job description in Telugu or English. Mention job responsibilities, timing, and "Call for more details / పూర్తి వివరాల కోసం సంప్రదించండి". STRICT RULE: Do NOT include exaggerated promises, fake guarantees, or hype.
- "category": (string) Best matching DutyPe schema category from: COOK, MAID, DRIVER, HELPER, SECURITY, GARDENER, CARETAKER, DELIVERY, WAITER, ELECTRICIAN, PLUMBER, PAINTER, CARPENTER, RECEPTIONIST, CASHIER, PACKER, SALES, TELECALLER, TEACHER, OFFICE_STAFF, CUSTOMER_SUPPORT, FIELD_EXECUTIVE, MARKETING, FINANCE, HEALTHCARE, BEAUTICIAN, TAILOR, MECHANIC, DATA_ENTRY, LEGAL, TUTOR, EVENT_STAFF, PROMOTER, PHARMACY, CONSTRUCTION, OTHER
- "shiftTiming": (string) "DAY", "NIGHT", or "ANY"
- "gender": (string) "ANY", "MALE", or "FEMALE"
- "workType": (string) "FULL_TIME" or "PART_TIME"
- "experienceRequired": (string) e.g. "No Experience Required", "1-2 years"
- "educationRequired": (string) e.g. "No qualification required", "10th pass", "Intermediate"

Return ONLY the raw JSON string. Do not wrap in markdown \`\`\`json or add explanations.`;

export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request, RateLimitTiers.MEDIA_UPLOAD);
  if (unauthorized) return unauthorized;

  try {
    const body = await request.json();
    const { imageBase64, mimeType = "image/jpeg", apiKeyOverride } = body;

    if (!imageBase64 || typeof imageBase64 !== "string") {
      return NextResponse.json({ error: "Missing imageBase64 in request body." }, { status: 400 });
    }

    // Clean base64 data URL prefix if present
    const cleanBase64 = imageBase64.replace(/^data:[^;]+;base64,/, "").trim();

    const apiKey = (apiKeyOverride || process.env.GEMINI_API_KEY || process.env.NEXT_PUBLIC_GEMINI_API_KEY || "")
      .replace(/['"]/g, "")
      .trim();

    if (!apiKey) {
      return NextResponse.json(
        { error: "Gemini API key is not configured. Please add GEMINI_API_KEY in .env or provide apiKeyOverride." },
        { status: 500 }
      );
    }

    // Try Gemini 2.0 Flash first, fallback to Gemini 1.5 Flash
    const models = ["gemini-2.0-flash", "gemini-1.5-flash"];
    let rawText = "";
    let lastError: Error | null = null;

    for (const model of models) {
      try {
        const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
        const response = await fetch(url, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            contents: [
              {
                parts: [
                  { text: SYSTEM_PROMPT },
                  {
                    inline_data: {
                      mime_type: mimeType,
                      data: cleanBase64
                    }
                  }
                ]
              }
            ],
            generationConfig: {
              temperature: 0.1,
              response_mime_type: "application/json"
            }
          })
        });

        if (!response.ok) {
          const errBody = await response.text();
          throw new Error(`Gemini API (${model}) failed with status ${response.status}: ${errBody}`);
        }

        const data = await response.json();
        rawText = data?.candidates?.[0]?.content?.parts?.[0]?.text || "";
        if (rawText) break;
      } catch (err: any) {
        lastError = err;
        console.warn(`Failed with model ${model}, trying next...`, err.message);
      }
    }

    if (!rawText) {
      throw lastError || new Error("No response received from Gemini AI model.");
    }

    // Strip any possible markdown fence
    const sanitized = rawText.replace(/```json/gi, "").replace(/```/g, "").trim();
    const parsed = JSON.parse(sanitized);

    return NextResponse.json({
      ok: true,
      extracted: parsed
    });
  } catch (error: any) {
    console.error("AI Job Extraction error:", error);
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Failed to extract job details from image." },
      { status: 500 }
    );
  }
}
