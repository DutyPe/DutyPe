/**
 * DutyPe AI — the employer's voice assistant. One conversation endpoint:
 *
 *   dutypeAi({ message, lang, history })  →  { reply, action?, stats, locked? }
 *
 * The server first gathers the employer's real data (jobs, applicants, hires, urgent posts) and
 * the AI answers ONLY from those facts — counts and names are never invented. It may propose one
 * action (post a job / urgent need, hire, reject, close, renew, open a job); the server validates it
 * against the employer's own data, and the app runs it only after the employer says yes, through the
 * normal secured calls. Without DutyPe AI (plan / trial) simple questions are still answered from
 * the data, for free.
 */
import * as admin from "firebase-admin";
import { onCallSecured } from "./secure-callable";
import { fail, obj, str } from "./lib/input";
import { ai, cleanDraft, missingFields, refundAi, useAi, LOCKED, FORM, FREE_AI_TRIAL } from "./ai-hiring";
import { Applications, CATEGORY_KEYS, EmployerProfiles, InstantRequests, Jobs } from "./schema";
import { AZURE_COSMOS_SECRET, AZURE_OPENAI_SECRET, CosmosContainers, cosmosAdd } from "./lib/azure";

const db = admin.firestore();
const LANGS = ["en", "te", "hi"] as const;
type Lang = typeof LANGS[number];
const LANG_NAME: Record<Lang, string> = { en: "simple English", te: "simple Telugu (Telugu script)", hi: "simple Hindi (Devanagari)" };
const DAY_MS = 24 * 60 * 60 * 1000;
const IST_OFFSET_MS = 330 * 60 * 1000;
const URGENT_WINDOWS = ["right_now", "within_1_hour", "today", "tomorrow"];

export interface Facts {
  employerName: string;
  plan: { aiIncluded: boolean; freeAiLeft: number; jobCredits: number; urgentCredits: number };
  jobs: Array<{ id: string; title: string; status: string; category: string; vacancies: number; applicants: number; postedDaysAgo: number }>;
  applicants: Array<{ id: string; jobId: string; job: string; name: string; status: string; appliedToday: boolean }>;
  urgent: Array<{ id: string; title: string; status: string; needed: number; accepted: number }>;
  stats: { openJobs: number; appliedToday: number; waiting: number; hired: number; hiredNames: string[] };
}

const ms = (v: unknown) => (v as admin.firestore.Timestamp | undefined)?.toMillis?.() ?? 0;

/** The employer's own data, compact (about 130 document reads). */
export async function employerFacts(uid: string): Promise<Facts> {
  const S = EmployerProfiles.Subscription;
  const [profile, jobsSnap, appsSnap, urgentSnap] = await Promise.all([
    db.collection(EmployerProfiles.COLLECTION).doc(uid).get(),
    db.collection(Jobs.COLLECTION).where(Jobs.EMPLOYER_ID, "==", uid).orderBy(Jobs.CREATED_AT, "desc").limit(20).get(),
    db.collection(Applications.COLLECTION).where(Applications.EMPLOYER_ID, "==", uid).orderBy(Applications.CREATED_AT, "desc").limit(100).get(),
    db.collection(InstantRequests.COLLECTION).where(InstantRequests.EMPLOYER_ID, "==", uid).orderBy(InstantRequests.CREATED_AT, "desc").limit(10).get(),
  ]);
  const now = Date.now();
  const todayStart = Math.floor((now + IST_OFFSET_MS) / DAY_MS) * DAY_MS - IST_OFFSET_MS;
  const sub = obj(profile.get(EmployerProfiles.SUBSCRIPTION));
  const credits = obj(sub[S.CREDITS]);
  const titles = new Map(jobsSnap.docs.map((d) => [d.id, String(d.get(Jobs.TITLE) || "")]));
  const applicants = appsSnap.docs.map((a) => ({
    id: a.id,
    jobId: String(a.get(Applications.JOB_ID)),
    job: titles.get(String(a.get(Applications.JOB_ID))) || "",
    name: String(a.get(Applications.WORKER_NAME) || "Worker"),
    status: String(a.get(Applications.STATUS)),
    appliedToday: ms(a.get(Applications.CREATED_AT)) >= todayStart,
  }));
  const hired = applicants.filter((a) => a.status === "hired" || a.status === "completed");
  return {
    employerName: String(profile.get(EmployerProfiles.BUSINESS_NAME) || profile.get(EmployerProfiles.OWNER_NAME) || ""),
    plan: {
      aiIncluded: sub[S.AI] === true || sub[S.PLAN_ID] === "UNLIMITED_CAMPAIGN",
      freeAiLeft: Math.max(0, FREE_AI_TRIAL - Number(profile.get(EmployerProfiles.AI_TRIAL_USED) || 0)),
      jobCredits: Number(credits[S.CREDITS_NORMAL] || 0),
      urgentCredits: Number(credits[S.CREDITS_INSTANT] || 0),
    },
    jobs: jobsSnap.docs.map((d) => ({
      id: d.id,
      title: String(d.get(Jobs.TITLE) || ""),
      status: String(d.get(Jobs.STATUS) || ""),
      category: String(d.get(Jobs.CATEGORY) || ""),
      vacancies: Number(d.get(Jobs.VACANCIES) || 1),
      applicants: Number(d.get(Jobs.APPLICATION_COUNT) || 0),
      postedDaysAgo: Math.floor((now - ms(d.get(Jobs.CREATED_AT))) / DAY_MS),
    })),
    applicants,
    urgent: urgentSnap.docs.map((d) => ({
      id: d.id,
      title: String(d.get(InstantRequests.TITLE) || ""),
      status: String(d.get(InstantRequests.STATUS) || ""),
      needed: Number(d.get(InstantRequests.WORKERS_NEEDED) || 1),
      accepted: ((d.get(InstantRequests.SELECTED_WORKER_IDS) || []) as unknown[]).length,
    })),
    stats: {
      openJobs: jobsSnap.docs.filter((d) => d.get(Jobs.STATUS) === "open").length,
      appliedToday: applicants.filter((a) => a.appliedToday).length,
      waiting: applicants.filter((a) => a.status === "applied").length,
      hired: hired.length,
      hiredNames: hired.slice(0, 10).map((a) => a.name),
    },
  };
}

/** Answers from the data without AI: today's applications and hires. Null when not that kind of question. */
export function quickAnswer(message: string, f: Facts, l: Lang): string | null {
  const m = message.toLowerCase();
  const names = f.stats.hiredNames.slice(0, 5).join(", ");
  if (/hire|hired|నియమ|తీసుకున్న|रखा|हायर/.test(m)) {
    return {
      en: f.stats.hired ? `You have hired ${f.stats.hired}: ${names}.` : "You have not hired anyone yet.",
      te: f.stats.hired ? `మీరు ${f.stats.hired} మందిని తీసుకున్నారు: ${names}.` : "మీరు ఇంకా ఎవరినీ తీసుకోలేదు.",
      hi: f.stats.hired ? `आपने ${f.stats.hired} लोगों को रखा है: ${names}.` : "आपने अभी किसी को नहीं रखा है।",
    }[l];
  }
  if (/appl|apply|దరఖాస్తు|అప్లై|आवेदन|अप्लाई|how many|ఎంత మంది|कितने/.test(m)) {
    return {
      en: `${f.stats.appliedToday} applied today. ${f.stats.waiting} are waiting for your reply across ${f.stats.openJobs} open jobs.`,
      te: `ఈరోజు ${f.stats.appliedToday} మంది దరఖాస్తు చేశారు. ${f.stats.openJobs} ఓపెన్ పనులకు ${f.stats.waiting} మంది మీ జవాబు కోసం చూస్తున్నారు.`,
      hi: `आज ${f.stats.appliedToday} लोगों ने आवेदन किया। ${f.stats.openJobs} खुली नौकरियों पर ${f.stats.waiting} लोग आपके जवाब का इंतज़ार कर रहे हैं।`,
    }[l];
  }
  return null;
}

export interface AiAction { type: string; args: Record<string, unknown>; summary: string }

/** Keeps only actions that make sense for this employer's own data. */
export function validateAction(raw: unknown, f: Facts): AiAction | null {
  const a = obj(raw);
  const args = obj(a.args);
  const type = String(a.type || "");
  const job = f.jobs.find((j) => j.id === args.jobId);
  const app = f.applicants.find((x) => x.id === args.applicationId);
  switch (type) {
  case "post_job": {
    const draft = cleanDraft(args);
    if (missingFields(draft).length) return null;
    const pay = draft.payType === "NEGOTIABLE" ? "pay to discuss" : `₹${draft.payAmount} ${draft.payType.toLowerCase()}`;
    return { type, args: { ...draft }, summary: `Post: ${draft.vacancies} × ${draft.title}, ${pay}` };
  }
  case "post_urgent": {
    const category = String(args.category || "").toUpperCase();
    const title = String(args.title || "").trim().slice(0, 80);
    const workersNeeded = Math.round(Number(args.workersNeeded) || 0);
    const payPerPerson = Math.round(Number(args.payPerPerson) || 0);
    const window = URGENT_WINDOWS.includes(String(args.window)) ? String(args.window) : "right_now";
    if (title.length < 3 || workersNeeded < 1 || workersNeeded > 20 || payPerPerson < 1 || payPerPerson > 50_000) return null;
    return {
      type,
      args: {
        title, workersNeeded, payPerPerson, window,
        category: (CATEGORY_KEYS as readonly string[]).includes(category) ? category : "HELPER",
        durationText: String(args.durationText || "").slice(0, 40),
      },
      summary: `Urgent: ${workersNeeded} × ${title}, ₹${payPerPerson} each`,
    };
  }
  case "hire":
    return app && app.status === "applied" ? { type, args: { applicationId: app.id }, summary: `Hire ${app.name} for ${app.job}` } : null;
  case "reject":
    return app && (app.status === "applied" || app.status === "hired") ? { type, args: { applicationId: app.id }, summary: `Say no to ${app.name} for ${app.job}` } : null;
  case "close_job":
    return job && job.status === "open" ? { type, args: { jobId: job.id }, summary: `Close: ${job.title}` } : null;
  case "renew_job":
    return job && (job.status === "expired" || job.status === "closed") ? { type, args: { jobId: job.id }, summary: `Renew: ${job.title}` } : null;
  case "open_job":
    return job ? { type, args: { jobId: job.id }, summary: `Open: ${job.title}` } : null;
  default:
    return null;
  }
}

function lang(value: unknown): Lang {
  return (LANGS as readonly string[]).includes(String(value)) ? value as Lang : "en";
}

const BUSY: Record<"en" | "te" | "hi", string> = {
  en: "DutyPe AI is busy right now. Please try again in a minute. Your free try was not used.",
  te: "DutyPe AI ఇప్పుడు బిజీగా ఉంది. ఒక నిమిషం తర్వాత మళ్లీ ప్రయత్నించండి. మీ ఉచిత అవకాశం వాడలేదు.",
  hi: "DutyPe AI अभी व्यस्त है। एक मिनट बाद फिर कोशिश करें। आपका मुफ़्त मौका इस्तेमाल नहीं हुआ।",
};

export const dutypeAi = onCallSecured({ timeoutSeconds: 30, memory: "512MB", secrets: [AZURE_OPENAI_SECRET, AZURE_COSMOS_SECRET] }, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  if (context.auth?.token.role !== "EMPLOYER") fail("permission-denied", "DutyPe AI is for employers");
  const data = obj(raw);
  const message = str(data, "message", { min: 1, max: 1000 });
  const l = lang(data.lang);
  const history = (Array.isArray(data.history) ? data.history : []).slice(-6).map((h) => {
    const t = obj(h);
    return { from: t.role === "ai" ? "DutyPe AI" : "Employer", text: String(t.text || "").slice(0, 300) };
  });
  const facts = await employerFacts(uid);
  const access = await useAi(uid);
  if (access !== "plan" && access !== "trial") {
    return { reply: quickAnswer(message, facts, l) ?? LOCKED[access === "limit" ? "limit" : "upgrade"][l], action: null, stats: facts.stats, locked: access };
  }

  const prompt = `You are "DutyPe AI", the friendly hiring assistant inside the DutyPe app for small Indian employers.
Reply in ${LANG_NAME[l]}, in 1-3 short sentences that sound natural when read aloud. Be warm and practical.
Use ONLY these facts about this employer (never invent numbers, names or jobs): ${JSON.stringify({ ...facts, stats: undefined, summary: facts.stats })}
Conversation so far: ${JSON.stringify(history)}
Employer now says: ${JSON.stringify(message)}

You may propose ONE action; the employer will confirm before anything happens:
- post_job: a regular job. args: {title (short English), category (one of ${JSON.stringify(CATEGORY_KEYS)}), employmentType ${JSON.stringify(FORM.employmentTypes)}, payAmount (rupees, at most 50000), payType ${JSON.stringify(FORM.payTypes)}, vacancies, shift ${JSON.stringify(FORM.shifts)}, gender ${JSON.stringify(FORM.genders)}, experience ${JSON.stringify(FORM.experience)}, education ${JSON.stringify(FORM.education)}, perks ${JSON.stringify(FORM.perks)}, description (2-3 sentences in ${LANG_NAME[l]} from what they told you about the work)}
- post_urgent: same-day / short work, workers come quickly. args: {title, category, workersNeeded (1-20), payPerPerson (rupees), window ${JSON.stringify(URGENT_WINDOWS)}, durationText}
- hire / reject: args {applicationId} (from the facts)
- close_job / renew_job / open_job: args {jobId} (from the facts)
If they want to post but the work, the pay, or what the work involves (duties, timings, place) is missing, ask for it (no action yet). Pay is at most ₹50,000. When you propose an action, end the reply by asking them to confirm.
Return JSON: {"reply": "...", "action": null or {"type": "...", "args": {...}}}`;
  const reply0 = await ai.json(prompt);
  if (reply0 === null) {
    // The AI did not answer: give the action back and still answer counts / hires from the data.
    await refundAi(uid, access);
    const fallback = quickAnswer(message, facts, l) ?? BUSY[l];
    await logConversation(uid, l, message, fallback, null, true);
    return { reply: fallback, action: null, stats: facts.stats, aiDown: true };
  }
  const out = obj(reply0);
  const action = validateAction(out.action, facts);
  const reply = String(out.reply || "").trim().slice(0, 600) || (quickAnswer(message, facts, l) ?? {
    en: "Sorry, I did not get that. You can say: post a job, how many applied, or who is hired.",
    te: "క్షమించండి, అర్థం కాలేదు. మీరు: పని పోస్ట్ చేయి, ఎంత మంది దరఖాస్తు చేశారు, ఎవరిని తీసుకున్నాను అని అడగవచ్చు.",
    hi: "माफ़ कीजिए, समझ नहीं आया। आप कह सकते हैं: नौकरी पोस्ट करो, कितने लोगों ने आवेदन किया, किसे रखा है।",
  }[l]);
  await logConversation(uid, l, message, reply, action?.type ?? null, false);
  return { reply, action, stats: facts.stats };
});

/** What employers ask DutyPe AI, kept in Azure Cosmos DB (container "ai_logs") to improve the AI. */
async function logConversation(uid: string, l: Lang, message: string, reply: string, action: string | null, aiDown: boolean) {
  await cosmosAdd(CosmosContainers.AI_LOGS, { uid, lang: l, message, reply, action, aiDown });
}
