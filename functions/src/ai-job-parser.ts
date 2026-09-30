import * as functions from "firebase-functions";
import { onCallSecured } from "./secure-callable";

const VALID_CATEGORIES = [
  "Cook",
  "Electrician",
  "Plumber",
  "Loading Helper",
  "Driver",
  "Security",
  "Carpenter",
  "Delivery",
  "Painter",
  "Cleaner / Maid",
  "Other Work",
] as const;

export interface VoiceJobParseRequest {
  transcript: string;
  currentInput?: {
    title?: string;
    category?: string;
    workersNeeded?: number;
    perPersonPayment?: number;
    budgetText?: string;
    durationText?: string;
    addressText?: string;
  };
  userLanguage?: string;
}

export interface VoiceJobParseResponse {
  success: boolean;
  isComplete: boolean;
  title: string;
  category: string;
  workersNeeded: number;
  perPersonPayment: number;
  budgetText: string;
  durationText: string;
  addressText: string;
  missingFields: string[];
  clarificationQuestion: string;
  summaryText: string;
}

export const parseVoiceJobDetails = onCallSecured<VoiceJobParseRequest, VoiceJobParseResponse>(
  { requireAuth: false, enforceAppCheck: false, timeoutSeconds: 25 },
  async (data) => {
    const rawTranscript = String(data?.transcript || "").trim();
    if (!rawTranscript) {
      return {
        success: false,
        isComplete: false,
        title: "",
        category: "Other Work",
        workersNeeded: 1,
        perPersonPayment: 0,
        budgetText: "",
        durationText: "Full Day (8 hrs)",
        addressText: "",
        missingFields: ["category", "wage"],
        clarificationQuestion: "Aapko kis kaam ke liye worker chahiye aur kitna payment denge?",
        summaryText: "",
      };
    }

    const current = data.currentInput || {};
    const apiKey = process.env.GEMINI_API_KEY || "";

    if (apiKey) {
      try {
        const aiResult = await callGeminiForJobExtraction(rawTranscript, current, apiKey);
        if (aiResult) {
          return aiResult;
        }
      } catch (err: unknown) {
        functions.logger.warn("Gemini voice parse failed, using rule-based fallback", err);
      }
    }

    // Deterministic rule-based fallback (offline / zero-token fallback)
    return parseRuleBasedJobDetails(rawTranscript, current);
  }
);

async function callGeminiForJobExtraction(
  transcript: string,
  current: Record<string, unknown>,
  apiKey: string
): Promise<VoiceJobParseResponse | null> {
  const prompt = `You are an Indian hyperlocal recruitment assistant for the DutyPe app.
Extract blue-collar job posting details from an employer's spoken voice transcript.
The transcript may be in colloquial Hindi, Telugu, Hinglish, Teluglish, or Indian English.

Supported Categories (MUST be one of these exact values):
- "Cook"
- "Electrician"
- "Plumber"
- "Loading Helper"
- "Driver"
- "Security"
- "Carpenter"
- "Delivery"
- "Painter"
- "Cleaner / Maid"
- "Other Work"

Previous extracted state (merge with this if valid):
${JSON.stringify(current)}

New spoken transcript:
"${transcript}"

Extract the following fields into JSON:
- "category": Best matching category from the list above. Default to "Loading Helper" if general labor, or "Other Work" if unspecified.
- "title": Short clean job title in English (e.g. "2 Helpers for Godown", "Urgent Electrician Needed").
- "workersNeeded": Integer count of workers needed. Default to 1 if unspecified.
- "perPersonPayment": Number (in Indian Rupees ₹). E.g., if user says "700 rupaye" or "700 isthanu" -> 700. If unspecified, return 0.
- "budgetText": Formatted string e.g. "₹700 / Day".
- "durationText": Work duration e.g. "Full Day (8 hrs)", "Half Day (4 hrs)", "2-3 Days". Default to "Full Day (8 hrs)".
- "addressText": Specific locality, landmark or address if mentioned, else "".
- "missingFields": Array of strings of what is still strictly missing to post the job. Required fields are:
    * "category" (if completely unclear)
    * "wage" (if perPersonPayment <= 0)
- "isComplete": boolean (true if category is identified and perPersonPayment > 0).
- "clarificationQuestion": If missingFields is not empty, a very polite, short, natural 1-sentence question in the user's spoken language (Hindi/Telugu/English) asking for what is missing. For example:
    * If wage is missing in Hindi: "Kaam samajh gaya. Aap per day kitna payment denge?"
    * If wage is missing in Telugu: "Work ardamaindi. Roju ki entha payment istharu?"
    * If complete: "Kya ise post kar dein?"
- "summaryText": A 1-sentence natural summary in user's language e.g. "2 Helpers kal subah, ₹700 per day".

Respond with pure JSON only without markdown or code fences.`;

  const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${apiKey}`;

  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig: {
        responseMimeType: "application/json",
        temperature: 0.1,
      },
    }),
  });

  if (!response.ok) {
    functions.logger.warn(`Gemini API returned status ${response.status}`);
    return null;
  }

  const json: unknown = await response.json();
  const textContent = (json as { candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }> })
    ?.candidates?.[0]?.content?.parts?.[0]?.text;

  if (!textContent) return null;

  try {
    const parsed = JSON.parse(textContent);
    const category = VALID_CATEGORIES.includes(parsed.category) ? parsed.category : "Other Work";
    const payment = Number(parsed.perPersonPayment) || 0;
    const workers = Math.max(1, Math.min(20, Number(parsed.workersNeeded) || 1));
    const isComplete = payment > 0 && category !== "Other Work";

    return {
      success: true,
      isComplete,
      title: String(parsed.title || `${workers} ${category} Needed`),
      category,
      workersNeeded: workers,
      perPersonPayment: payment,
      budgetText: payment > 0 ? `₹${payment} / Day` : "",
      durationText: String(parsed.durationText || "Full Day (8 hrs)"),
      addressText: String(parsed.addressText || ""),
      missingFields: isComplete ? [] : (parsed.missingFields || ["wage"]),
      clarificationQuestion: String(parsed.clarificationQuestion || (payment <= 0 ? "Aap per day kitna payment denge?" : "")),
      summaryText: String(parsed.summaryText || `${workers} ${category} · ₹${payment}/day`),
    };
  } catch (err: unknown) {
    functions.logger.warn("Failed to parse Gemini JSON response", err);
    return null;
  }
}

function parseRuleBasedJobDetails(
  transcript: string,
  current: Record<string, unknown>
): VoiceJobParseResponse {
  const lower = transcript.toLowerCase();

  // Category detection
  let category = String(current.category || "");
  if (!category || category === "Other Work") {
    if (lower.includes("electric") || lower.includes("wiring") || lower.includes("current")) {
      category = "Electrician";
    } else if (lower.includes("plumb") || lower.includes("pipe") || lower.includes("tap") || lower.includes("motor")) {
      category = "Plumber";
    } else if (lower.includes("cook") || lower.includes("rasoi") || lower.includes("khana") || lower.includes("vantam")) {
      category = "Cook";
    } else if (lower.includes("driver") || lower.includes("gaadi") || lower.includes("auto") || lower.includes("car")) {
      category = "Driver";
    } else if (lower.includes("helper") || lower.includes("labour") || lower.includes("labor") || lower.includes("loading") || lower.includes("godown") || lower.includes("coolie")) {
      category = "Loading Helper";
    } else if (lower.includes("clean") || lower.includes("safai") || lower.includes("maid") || lower.includes("jhadu")) {
      category = "Cleaner / Maid";
    } else if (lower.includes("security") || lower.includes("guard") || lower.includes("watchman")) {
      category = "Security";
    } else if (lower.includes("paint") || lower.includes("rang")) {
      category = "Painter";
    } else if (lower.includes("carpenter") || lower.includes("wood") || lower.includes("badhai")) {
      category = "Carpenter";
    } else if (lower.includes("deliver")) {
      category = "Delivery";
    } else {
      category = "Loading Helper";
    }
  }

  // Workers count extraction
  let workersNeeded = Number(current.workersNeeded) || 1;
  const countMatch = lower.match(/(\d+)\s*(helper|worker|person|man|people|members|mandhi|log)/);
  if (countMatch && countMatch[1]) {
    workersNeeded = Math.max(1, Math.min(20, parseInt(countMatch[1], 10)));
  } else if (lower.includes("ek ") || lower.includes("oka ") || lower.includes("one ")) {
    workersNeeded = 1;
  } else if (lower.includes("do ") || lower.includes("rendu ") || lower.includes("two ")) {
    workersNeeded = 2;
  } else if (lower.includes("teen ") || lower.includes("moodu ") || lower.includes("three ")) {
    workersNeeded = 3;
  } else if (lower.includes("chaar ") || lower.includes("naalugu ") || lower.includes("four ")) {
    workersNeeded = 4;
  }

  // Payment extraction
  let payment = Number(current.perPersonPayment) || 0;
  const payMatch = lower.match(/(?:₹|rs\.?|rupees|rupaye|isthanu|denge)?\s*(\d{3,5})\s*(?:₹|rs\.?|rupees|rupaye|per\s*day|roju|daily)?/);
  if (payMatch && payMatch[1]) {
    const parsedAmount = parseInt(payMatch[1], 10);
    if (parsedAmount >= 200 && parsedAmount <= 10000) {
      payment = parsedAmount;
    }
  }

  const isComplete = payment > 0;
  const missingFields: string[] = [];
  let clarificationQuestion = "";

  if (payment <= 0) {
    missingFields.push("wage");
    clarificationQuestion = lower.includes("kavali") || lower.includes("isthanu")
      ? "Work ardamaindi. Roju ki entha payment istharu?"
      : "Kaam samajh gaya. Aap per day kitna payment denge?";
  }

  return {
    success: true,
    isComplete,
    title: `${workersNeeded} ${category} Needed`,
    category,
    workersNeeded,
    perPersonPayment: payment,
    budgetText: payment > 0 ? `₹${payment} / Day` : "",
    durationText: "Full Day (8 hrs)",
    addressText: String(current.addressText || ""),
    missingFields,
    clarificationQuestion,
    summaryText: `${workersNeeded} ${category} · ${payment > 0 ? `₹${payment}/day` : "Payment missing"}`,
  };
}
