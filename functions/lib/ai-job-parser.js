"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.parseVoiceJobDetails = void 0;
const functions = require("firebase-functions");
const secure_callable_1 = require("./secure-callable");
const ai_hiring_1 = require("./ai-hiring");
const azure_1 = require("./lib/azure");
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
];
exports.parseVoiceJobDetails = (0, secure_callable_1.onCallSecured)(
// Logged-in users of the real app only: every call spends AI tokens.
{ timeoutSeconds: 25, enforceAppCheck: false, secrets: [azure_1.AZURE_OPENAI_SECRET] }, async (data, context) => {
    const rawTranscript = String((data === null || data === void 0 ? void 0 : data.transcript) || "").trim();
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
    const configured = (0, ai_hiring_1.aiConfigured)();
    // AI (Azure OpenAI, else Gemini) only with DutyPe AI (plan or free trial); otherwise the built-in rules below.
    const access = configured ? await (0, ai_hiring_1.useAi)(context.auth.uid) : "upgrade";
    if (configured && (access === "plan" || access === "trial")) {
        try {
            const aiResult = await aiJobExtraction(rawTranscript, current);
            if (aiResult) {
                return aiResult;
            }
        }
        catch (err) {
            functions.logger.warn("AI voice parse failed, using rule-based fallback", err);
        }
    }
    // Deterministic rule-based fallback (offline / zero-token fallback)
    return parseRuleBasedJobDetails(rawTranscript, current);
});
async function aiJobExtraction(transcript, current) {
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
    const result = await ai_hiring_1.ai.json(prompt);
    if (!result || typeof result !== "object")
        return null;
    try {
        const parsed = result; // eslint-disable-line @typescript-eslint/no-explicit-any
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
    }
    catch (err) {
        functions.logger.warn("Failed to read AI JSON response", err);
        return null;
    }
}
function parseRuleBasedJobDetails(transcript, current) {
    const lower = transcript.toLowerCase();
    // Category detection
    let category = String(current.category || "");
    if (!category || category === "Other Work") {
        if (lower.includes("electric") || lower.includes("wiring") || lower.includes("current")) {
            category = "Electrician";
        }
        else if (lower.includes("plumb") || lower.includes("pipe") || lower.includes("tap") || lower.includes("motor")) {
            category = "Plumber";
        }
        else if (lower.includes("cook") || lower.includes("rasoi") || lower.includes("khana") || lower.includes("vantam")) {
            category = "Cook";
        }
        else if (lower.includes("driver") || lower.includes("gaadi") || lower.includes("auto") || lower.includes("car")) {
            category = "Driver";
        }
        else if (lower.includes("helper") || lower.includes("labour") || lower.includes("labor") || lower.includes("loading") || lower.includes("godown") || lower.includes("coolie")) {
            category = "Loading Helper";
        }
        else if (lower.includes("clean") || lower.includes("safai") || lower.includes("maid") || lower.includes("jhadu")) {
            category = "Cleaner / Maid";
        }
        else if (lower.includes("security") || lower.includes("guard") || lower.includes("watchman")) {
            category = "Security";
        }
        else if (lower.includes("paint") || lower.includes("rang")) {
            category = "Painter";
        }
        else if (lower.includes("carpenter") || lower.includes("wood") || lower.includes("badhai")) {
            category = "Carpenter";
        }
        else if (lower.includes("deliver")) {
            category = "Delivery";
        }
        else {
            category = "Loading Helper";
        }
    }
    // Workers count extraction
    let workersNeeded = Number(current.workersNeeded) || 1;
    const countMatch = lower.match(/(\d+)\s*(helper|worker|person|man|people|members|mandhi|log)/);
    if (countMatch && countMatch[1]) {
        workersNeeded = Math.max(1, Math.min(20, parseInt(countMatch[1], 10)));
    }
    else if (lower.includes("ek ") || lower.includes("oka ") || lower.includes("one ")) {
        workersNeeded = 1;
    }
    else if (lower.includes("do ") || lower.includes("rendu ") || lower.includes("two ")) {
        workersNeeded = 2;
    }
    else if (lower.includes("teen ") || lower.includes("moodu ") || lower.includes("three ")) {
        workersNeeded = 3;
    }
    else if (lower.includes("chaar ") || lower.includes("naalugu ") || lower.includes("four ")) {
        workersNeeded = 4;
    }
    // Payment extraction
    let payment = Number(current.perPersonPayment) || 0;
    const payMatch = lower.match(/(?:₹|rs\.?|rupees|rupaye|isthanu|denge)?\s*(\d{3,5})\s*(?:₹|rs\.?|rupees|rupaye|per\s*day|roju|daily)?/);
    if (payMatch && payMatch[1]) {
        const parsedAmount = parseInt(payMatch[1], 10);
        if (parsedAmount >= 200 && parsedAmount <= 25000) {
            payment = parsedAmount;
        }
    }
    else if (lower.includes("five hundred") || lower.includes("paanch sau") || lower.includes("aidu vandalu")) {
        payment = 500;
    }
    else if (lower.includes("six hundred") || lower.includes("che sau") || lower.includes("aaru vandalu")) {
        payment = 600;
    }
    else if (lower.includes("seven hundred") || lower.includes("saat sau") || lower.includes("yeedu vandalu") || lower.includes("yedu vandalu")) {
        payment = 700;
    }
    else if (lower.includes("eight hundred") || lower.includes("aath sau") || lower.includes("enimidi vandalu") || lower.includes("enimidhi vandalu")) {
        payment = 800;
    }
    else if (lower.includes("nine hundred") || lower.includes("nau sau") || lower.includes("tommidi vandalu")) {
        payment = 900;
    }
    else if (lower.includes("thousand") || lower.includes("hazaar") || lower.includes("hazar") || lower.includes("veyi") || lower.includes("veyyi")) {
        payment = 1000;
    }
    else if (lower.includes("fifteen hundred") || lower.includes("pandrah sau") || lower.includes("padihenu vandalu")) {
        payment = 1500;
    }
    else if (lower.includes("two thousand") || lower.includes("do hazar") || lower.includes("rendu velu")) {
        payment = 2000;
    }
    const isComplete = payment > 0;
    const missingFields = [];
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
//# sourceMappingURL=ai-job-parser.js.map