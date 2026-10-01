/**
 * AI for employers (hiring made easy).
 *
 *   aiShortlist        the best 3 of a job's first 50 applicants, each with a one-line reason in the
 *                      employer's language. A fixed, explainable score ranks everyone; the AI only
 *                      chooses among the top 8 and writes the reasons. Never auto-rejects anyone.
 *   nearbyWorkerCount  "23 drivers available within 5 km" before posting (count queries, ~1 read)
 *   aiJobAssistant     talk-to-post for regular jobs: merges what the employer said into a draft that
 *                      uses the posting form's exact options, asks for what is missing, summarises
 *
 * Plans: DutyPe AI is in the ₹199 / ₹299 plans (daily allowance) and every employer gets
 * [FREE_AI_TRIAL] free actions; without access the features fall back to the fixed score / rules.
 * Fairness: the AI never sees names, phone numbers, photos, gender, religion, caste or age — only
 * job-relevant facts — and every pick is shown with the facts behind it. Cost control: per-user
 * hourly caps and caching; without an AI key (or on any AI error) everything still works from
 * the fixed score / rules.
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { onCallSecured } from "./secure-callable";
import { azureOpenAiConfigured, chatJson } from "./lib/azure";
import { fail, obj, str, latLng } from "./lib/input";
import { coveringCells, decodeGeohash, distanceKm } from "./lib/geo";
import { Applications, CATEGORY_KEYS, EmployerProfiles, Idempotency, JobDetails, Jobs, WorkerCards, MAX_PAY_RUPEES } from "./schema";

const db = admin.firestore();
const { Timestamp } = admin.firestore;

const MAX_APPLICANTS = 50;
const AI_CANDIDATES = 8;
const PICKS = 3;
const SHORTLIST_CACHE_MS = 12 * 60 * 60 * 1000;
/** Free DutyPe AI actions for employers without an AI plan. */
export const FREE_AI_TRIAL = 10;
const DAY_MS = 24 * 60 * 60 * 1000;
const IST_OFFSET_MS = 330 * 60 * 1000;
const LANGS = ["en", "te", "hi"] as const;
type Lang = typeof LANGS[number];
const LANG_NAME: Record<Lang, string> = { en: "simple English", te: "simple Telugu (Telugu script)", hi: "simple Hindi (Devanagari)" };

// ─────────────────────────────── AI provider ───────────────────────────────

/** True when any AI provider is set up (Azure OpenAI first, Gemini as the fallback). */
export function aiConfigured(): boolean {
  return azureOpenAiConfigured() || Boolean(process.env.GEMINI_API_KEY);
}

/** Test seam: replaced in tests so no network call is made. */
export const ai = {
  /** Azure OpenAI when configured (paid from the Azure credits), otherwise Gemini. */
  async json(prompt: string): Promise<unknown | null> {
    if (azureOpenAiConfigured()) return chatJson(prompt);
    return geminiJson(prompt);
  },
};

async function geminiJson(prompt: string): Promise<unknown | null> {
  const key = process.env.GEMINI_API_KEY || "";
  if (!key) return null;
  const model = process.env.GEMINI_MODEL || "gemini-2.0-flash";
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 12_000);
  try {
    const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${key}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{ role: "user", parts: [{ text: prompt }] }],
        generationConfig: { temperature: 0.2, responseMimeType: "application/json", maxOutputTokens: 1024 },
      }),
      signal: controller.signal,
    });
    if (!res.ok) throw new Error(`Gemini ${res.status}`);
    const body = await res.json() as { candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }> };
    const text = body.candidates?.[0]?.content?.parts?.map((p) => p.text || "").join("") || "";
    return JSON.parse(text);
  } catch (e) {
    functions.logger.warn("Gemini call failed", e);
    return null;
  } finally {
    clearTimeout(timer);
  }
}

export type AiAccess = "plan" | "trial" | "upgrade" | "limit";

/**
 * Spends one DutyPe AI action if the employer may: an active AI plan (or the launch campaign) within
 * its daily allowance, else one of the free trial actions. "upgrade" / "limit" mean no AI this time.
 */
export async function useAi(uid: string): Promise<AiAccess> {
  const S = EmployerProfiles.Subscription;
  const profileRef = db.collection(EmployerProfiles.COLLECTION).doc(uid);
  const day = Math.floor((Date.now() + IST_OFFSET_MS) / DAY_MS);
  const dayRef = db.collection(Idempotency.COLLECTION).doc(`aiDay_${uid}_${day}`);
  return db.runTransaction(async (tx) => {
    const [profile, dayDoc] = await Promise.all([tx.get(profileRef), tx.get(dayRef)]);
    if (!profile.exists) return "upgrade";
    const sub = obj(profile.get(EmployerProfiles.SUBSCRIPTION));
    const expires = (sub[S.EXPIRES_AT] as admin.firestore.Timestamp | undefined)?.toMillis?.() ?? 0;
    const active = sub[S.STATUS] === "ACTIVE" && (expires === 0 || expires > Date.now());
    const campaign = active && sub[S.PLAN_ID] === "UNLIMITED_CAMPAIGN";
    if (active && (campaign || sub[S.AI] === true)) {
      const cap = Number(sub[S.AI_PER_DAY]) || 100;
      const used = Number(dayDoc.get(Idempotency.RESULT) || 0);
      if (used >= cap) return "limit";
      tx.set(dayRef, { [Idempotency.RESULT]: used + 1, [Idempotency.EXPIRE_AT]: Timestamp.fromMillis(Date.now() + 2 * DAY_MS) });
      return "plan";
    }
    const trialUsed = Number(profile.get(EmployerProfiles.AI_TRIAL_USED) || 0);
    if (trialUsed >= FREE_AI_TRIAL) return "upgrade";
    tx.update(profileRef, { [EmployerProfiles.AI_TRIAL_USED]: trialUsed + 1 });
    return "trial";
  });
}

const canUseAi = (a: AiAccess) => a === "plan" || a === "trial";

/** Gives back the action [useAi] spent when the AI then did not answer (key, quota or network). */
export async function refundAi(uid: string, access: AiAccess): Promise<void> {
  try {
    if (access === "trial") {
      await db.collection(EmployerProfiles.COLLECTION).doc(uid)
        .update({ [EmployerProfiles.AI_TRIAL_USED]: admin.firestore.FieldValue.increment(-1) });
    } else if (access === "plan") {
      const day = Math.floor((Date.now() + IST_OFFSET_MS) / DAY_MS);
      await db.collection(Idempotency.COLLECTION).doc(`aiDay_${uid}_${day}`)
        .set({ [Idempotency.RESULT]: admin.firestore.FieldValue.increment(-1) }, { merge: true });
    }
  } catch (e) {
    functions.logger.warn("AI refund failed", e);
  }
}

function lang(value: unknown): Lang {
  return (LANGS as readonly string[]).includes(String(value)) ? value as Lang : "en";
}

// ─────────────────────────────── shortlist ───────────────────────────────

/** "1-3 years" → 1, "Min. 6.0 months" → 0, "Freshers can apply" → 0. */
export function requiredYears(text: unknown): number {
  const s = String(text || "").toLowerCase();
  if (!s || /fresher|no experience|any/.test(s) || /month/.test(s)) return 0;
  const n = s.match(/\d+/);
  return n ? Math.min(20, Number(n[0])) : 0;
}

export interface Candidate {
  applicationId: string;
  workerId: string;
  name: string;
  photoUrl: string;
  score: number;
  distanceKm: number | null;
  facts: string[];
  /** Job-relevant facts only (what the AI sees). */
  profile: Record<string, unknown>;
}

/**
 * The fixed, explainable score (0–100-ish). Skill 35 · distance up to 20 · experience up to 15 ·
 * rating up to 10 · completed jobs up to 10 · recently active up to 8 · available 3 · no-shows −8 each.
 */
export function scoreCandidate(
  job: { category: string; lat: number; lng: number; requiredYears: number },
  card: admin.firestore.DocumentData,
  nowMs: number,
): { score: number; distanceKm: number | null; facts: string[]; profile: Record<string, unknown> } {
  const skills: string[] = Array.isArray(card[WorkerCards.SKILLS]) ? card[WorkerCards.SKILLS].map(String) : [];
  const facts: string[] = [];
  let score = 0;

  const hasSkill = job.category !== "OTHER" && skills.includes(job.category);
  if (hasSkill) { score += 35; facts.push("skill"); } else if (job.category === "OTHER") score += 15;

  let km: number | null = null;
  const points: Array<[number, number]> = [];
  if (Number.isFinite(Number(card[WorkerCards.LAT])) && card[WorkerCards.LAT] !== undefined) points.push([Number(card[WorkerCards.LAT]), Number(card[WorkerCards.LNG])]);
  if (typeof card[WorkerCards.CURRENT_CELL] === "string") {
    const b = decodeGeohash(card[WorkerCards.CURRENT_CELL]);
    points.push([(b.minLat + b.maxLat) / 2, (b.minLng + b.maxLng) / 2]);
  }
  if (points.length && Number.isFinite(job.lat)) km = Math.min(...points.map(([la, ln]) => distanceKm(job.lat, job.lng, la, ln)));
  score += km === null ? 5 : km <= 3 ? 20 : km <= 5 ? 17 : km <= 10 ? 12 : km <= 20 ? 6 : 0;

  const years = Number(card[WorkerCards.EXPERIENCE_YEARS] || 0);
  score += years >= job.requiredYears ? 10 + Math.min(5, years) : 10 * (years / Math.max(1, job.requiredYears));

  const rating = Number(card[WorkerCards.RATING] || 0);
  const ratingCount = Number(card[WorkerCards.RATING_COUNT] || 0);
  score += ratingCount > 0 ? Math.min(10, rating * 2) : 4;

  const done = Number(card[WorkerCards.JOBS_COMPLETED] || 0);
  score += Math.min(10, done * 2);
  const noShows = Number(card[WorkerCards.NO_SHOWS] || 0);
  score -= Math.min(24, noShows * 8);

  const lastActive = (card[WorkerCards.LAST_ACTIVE_AT] as admin.firestore.Timestamp | undefined)?.toMillis() ?? 0;
  const activeDays = lastActive ? (nowMs - lastActive) / 864e5 : Infinity;
  score += activeDays <= 1 ? 8 : activeDays <= 7 ? 5 : 0;
  if (card[WorkerCards.AVAILABLE] === true) score += 3;

  if (km !== null) facts.push(`${km < 1 ? "<1" : Math.round(km)} km`);
  if (years > 0) facts.push(`${years}+ yrs`);
  if (ratingCount > 0) facts.push(`★${rating.toFixed(1)}`);
  if (done > 0) facts.push(`${done} jobs done`);
  if (noShows > 0) facts.push(`${noShows} no-show`);
  return {
    score: Math.round(score),
    distanceKm: km === null ? null : Math.round(km * 10) / 10,
    facts,
    profile: {
      hasJobSkill: hasSkill, otherSkills: skills.filter((s) => s !== job.category).slice(0, 4),
      distanceKm: km === null ? "unknown" : Math.round(km), experienceYears: years,
      rating: ratingCount > 0 ? rating : "new", jobsCompleted: done, noShows,
      activeRecently: activeDays <= 7, availableNow: card[WorkerCards.AVAILABLE] === true,
    },
  };
}

function templateReason(c: Candidate, l: Lang): string {
  const f = c.profile as { hasJobSkill: boolean; distanceKm: unknown; experienceYears: number; jobsCompleted: number };
  const parts: Record<Lang, string[]> = {
    en: [f.hasJobSkill ? "Has the skill" : "", f.distanceKm !== "unknown" ? `${f.distanceKm} km away` : "", f.experienceYears ? `${f.experienceYears}+ years experience` : "", f.jobsCompleted ? `${f.jobsCompleted} jobs done` : ""],
    te: [f.hasJobSkill ? "ఈ పని తెలుసు" : "", f.distanceKm !== "unknown" ? `${f.distanceKm} కి.మీ దూరం` : "", f.experienceYears ? `${f.experienceYears}+ ఏళ్ల అనుభవం` : "", f.jobsCompleted ? `${f.jobsCompleted} పనులు పూర్తి` : ""],
    hi: [f.hasJobSkill ? "यह काम आता है" : "", f.distanceKm !== "unknown" ? `${f.distanceKm} किमी दूर` : "", f.experienceYears ? `${f.experienceYears}+ साल अनुभव` : "", f.jobsCompleted ? `${f.jobsCompleted} काम पूरे` : ""],
  };
  return parts[l].filter(Boolean).join(" · ");
}

export const aiShortlist = onCallSecured({ timeoutSeconds: 30, memory: "512MB" }, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const jobId = str(data, "jobId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
  const l = lang(data.lang);
  const [jobSnap, detailsSnap] = await Promise.all([
    db.collection(Jobs.COLLECTION).doc(jobId).get(),
    db.collection(JobDetails.COLLECTION).doc(jobId).get(),
  ]);
  if (!jobSnap.exists || jobSnap.get(Jobs.EMPLOYER_ID) !== uid) fail("permission-denied", "Not your job");

  const apps = (await db.collection(Applications.COLLECTION)
    .where(Applications.EMPLOYER_ID, "==", uid)
    .where(Applications.JOB_ID, "==", jobId)
    .orderBy(Applications.CREATED_AT, "asc")
    .limit(MAX_APPLICANTS)
    .get()).docs.filter((a) => ["applied", "hired"].includes(String(a.get(Applications.STATUS))));
  if (!apps.length) return { picks: [], considered: 0, byAi: false };

  const cacheRef = db.collection(Idempotency.COLLECTION).doc(`aiPicks_${jobId}_${l}`);
  const cached = await cacheRef.get();
  const signature = apps.map((a) => a.id).join(",");
  const cachedAt = (cached.get("at") as admin.firestore.Timestamp | undefined)?.toMillis() ?? 0;
  if (cached.exists && cached.get("signature") === signature && Date.now() - cachedAt < SHORTLIST_CACHE_MS) {
    return cached.get(Idempotency.RESULT);
  }

  const cards = await db.getAll(...apps.map((a) => db.collection(WorkerCards.COLLECTION).doc(String(a.get(Applications.WORKER_ID)))));
  const cardById = new Map(cards.map((c) => [c.id, c.data() || {}]));
  const jobFacts = {
    category: String(jobSnap.get(Jobs.CATEGORY) || "OTHER"),
    lat: Number(jobSnap.get(Jobs.LAT)),
    lng: Number(jobSnap.get(Jobs.LNG)),
    requiredYears: requiredYears(detailsSnap.get(JobDetails.EXPERIENCE_REQUIRED)),
  };
  const now = Date.now();
  const ranked: Candidate[] = apps.map((a) => {
    const workerId = String(a.get(Applications.WORKER_ID));
    const s = scoreCandidate(jobFacts, cardById.get(workerId) || {}, now);
    return {
      applicationId: a.id, workerId,
      name: String(a.get(Applications.WORKER_NAME) || "Worker"),
      photoUrl: String(a.get(Applications.WORKER_PHOTO) || ""),
      ...s,
    };
  }).sort((x, y) => y.score - x.score);

  const pool = ranked.slice(0, AI_CANDIDATES);
  let chosen: Array<{ c: Candidate; reason: string }> = [];
  let byAi = false;
  const access: AiAccess = pool.length > 1 ? await useAi(uid) : "plan";
  if (pool.length > 1 && canUseAi(access)) {
    const prompt = `You help a small Indian business owner hire for one job. Pick the best ${Math.min(PICKS, pool.length)} candidates, best first.
Job: ${JSON.stringify({
    title: jobSnap.get(Jobs.TITLE), category: jobFacts.category, payType: jobSnap.get(Jobs.PAY_TYPE),
    description: String(detailsSnap.get(JobDetails.DESCRIPTION) || "").slice(0, 600),
    experienceRequired: detailsSnap.get(JobDetails.EXPERIENCE_REQUIRED) || "", educationRequired: detailsSnap.get(JobDetails.EDUCATION_REQUIRED) || "",
  })}
Candidates (ids are anonymous; use only these facts): ${JSON.stringify(pool.map((c, i) => ({ id: `C${i + 1}`, fixedScore: c.score, ...c.profile })))}
Rules: judge only on job-relevant facts and the job's stated needs. Never consider or guess gender, religion, caste, age or names. Prefer having the skill, being near, reliability (jobs done, no no-shows) and experience.
Write each reason in ${LANG_NAME[l]}, at most 12 words, citing the facts (e.g. distance, experience, jobs done).
Return JSON: {"picks":[{"id":"C1","reason":"..."}]}`;
    const out = await ai.json(prompt) as { picks?: Array<{ id?: unknown; reason?: unknown }> } | null;
    const seen = new Set<number>();
    for (const p of out?.picks || []) {
      const idx = Number(String(p.id || "").replace(/\D/g, "")) - 1;
      if (!(idx >= 0 && idx < pool.length) || seen.has(idx)) continue;
      seen.add(idx);
      chosen.push({ c: pool[idx], reason: String(p.reason || "").slice(0, 140) || templateReason(pool[idx], l) });
      if (chosen.length >= PICKS) break;
    }
    byAi = chosen.length > 0;
    if (out === null) await refundAi(uid, access);
  }
  if (!chosen.length) chosen = ranked.slice(0, PICKS).map((c) => ({ c, reason: templateReason(c, l) }));

  const result = {
    picks: chosen.map(({ c, reason }) => ({
      applicationId: c.applicationId, workerId: c.workerId, name: c.name, photoUrl: c.photoUrl,
      score: c.score, distanceKm: c.distanceKm, facts: c.facts, reason,
    })),
    considered: apps.length,
    byAi,
    /** No AI plan / trial left: picks come from the fixed score; the app offers DutyPe AI. */
    aiLocked: access === "upgrade",
  };
  await cacheRef.set({
    [Idempotency.RESULT]: result, signature, at: Timestamp.fromMillis(now),
    [Idempotency.EXPIRE_AT]: Timestamp.fromMillis(now + 2 * SHORTLIST_CACHE_MS),
  });
  return result;
});

// ─────────────────────────────── nearby supply ───────────────────────────────

/**
 * Available workers with a skill near a point: within 5 km and within 10 km. Count queries over the
 * workers' matching keys (each ~1 read). A worker whose home and current area fall in different
 * 30-cell chunks can be counted twice, so the app shows it as "about".
 */
export const nearbyWorkerCount = onCallSecured({ timeoutSeconds: 15 }, async (raw: unknown) => {
  const data = obj(raw);
  const { lat, lng } = latLng(data);
  const category = String(data.category || "").toUpperCase();
  const tag = (CATEGORY_KEYS as readonly string[]).includes(category) && category !== "OTHER" ? category : "ANY";
  const count = async (cells: string[]) => {
    let total = 0;
    for (let i = 0; i < cells.length; i += 30) {
      const agg = await db.collection(WorkerCards.COLLECTION)
        .where(WorkerCards.AVAILABLE, "==", true)
        .where(WorkerCards.SKILL_CELLS, "array-contains-any", cells.slice(i, i + 30).map((c) => `${tag}_${c}`))
        .count().get();
      total += agg.data().count;
    }
    return total;
  };
  const near = coveringCells(lat, lng, 5, 5);
  const nearSet = new Set(near);
  const ring = coveringCells(lat, lng, 10, 5).filter((c) => !nearSet.has(c));
  const [within5, ring10] = await Promise.all([count(near), count(ring)]);
  return { category: tag, within5km: within5, within10km: within5 + ring10 };
});

// ─────────────────────────────── talk-to-post ───────────────────────────────

/** The posting form's exact options (PostJobScreen); the assistant must answer with these. */
export const FORM = {
  payTypes: ["DAILY", "WEEKLY", "MONTHLY", "HOURLY", "NEGOTIABLE"],
  employmentTypes: ["FULL_TIME", "PART_TIME", "DAILY"],
  shifts: ["DAY", "NIGHT", "ANY"],
  genders: ["Both", "Male", "Female"],
  experience: ["No Experience Required", "Fresher (Educated)", "1-3 years", "3-5 years", "5+ years"],
  education: ["No qualification required", "10th pass", "12th pass", "ITI", "Diploma", "Graduate", "Any qualification"],
  perks: ["Food Provided", "Transport", "Overtime Bonus", "Accommodation"],
} as const;

export interface JobDraft {
  title: string; category: string; employmentType: string; payAmount: number; payType: string; vacancies: number;
  shift: string; gender: string; experience: string; education: string; perks: string[]; description: string;
}

const pick = <T extends string>(value: unknown, options: readonly T[], fallback: T): T => {
  const v = String(value ?? "").trim().toLowerCase();
  return options.find((o) => o.toLowerCase() === v) ?? fallback;
};

/** Cleans any (AI or client) draft into valid form values. */
export function cleanDraft(d: Record<string, unknown>): JobDraft {
  const category = String(d.category || "").toUpperCase().replace(/[^A-Z_]/g, "");
  return {
    title: String(d.title || "").trim().slice(0, 80),
    category: (CATEGORY_KEYS as readonly string[]).includes(category) ? category : "",
    employmentType: pick(d.employmentType, FORM.employmentTypes, "FULL_TIME"),
    // Above the ₹50,000 limit counts as not given, so the assistant asks again.
    payAmount: ((n) => (n > MAX_PAY_RUPEES ? 0 : n))(Math.max(0, Math.round(Number(d.payAmount) || 0))),
    payType: pick(d.payType, FORM.payTypes, "MONTHLY"),
    vacancies: Math.max(1, Math.min(50, Math.round(Number(d.vacancies) || 1))),
    shift: pick(d.shift, FORM.shifts, "ANY"),
    gender: pick(d.gender, FORM.genders, "Both"),
    experience: pick(d.experience, FORM.experience, "No Experience Required"),
    education: pick(d.education, FORM.education, "No qualification required"),
    perks: (Array.isArray(d.perks) ? d.perks : []).map((p) => pick(p, FORM.perks, "" as typeof FORM.perks[number])).filter(Boolean),
    description: String(d.description || "").trim().slice(0, 1500),
  };
}

/** What still has to be asked before the job can be posted. */
export function missingFields(d: JobDraft): string[] {
  const out: string[] = [];
  if (!d.title || !d.category) out.push("job");
  if (d.payType !== "NEGOTIABLE" && d.payAmount <= 0) out.push("pay");
  // The post needs at least one line about the work (the server requires 10+ characters).
  if (d.description.trim().length < 10) out.push("about");
  return out;
}

export const LOCKED: Record<"upgrade" | "limit", Record<Lang, string>> = {
  upgrade: {
    en: "Your free DutyPe AI tries are used up. DutyPe AI comes with the ₹199 and ₹299 plans.",
    te: "మీ ఉచిత DutyPe AI అవకాశాలు అయిపోయాయి. DutyPe AI ₹199, ₹299 ప్లాన్లలో ఉంటుంది.",
    hi: "आपके मुफ़्त DutyPe AI मौके खत्म हो गए। DutyPe AI ₹199 और ₹299 प्लान में मिलता है।",
  },
  limit: {
    en: "You have used today's DutyPe AI limit. It resets tomorrow.",
    te: "ఈరోజు DutyPe AI పరిమితి అయిపోయింది. రేపు మళ్లీ వస్తుంది.",
    hi: "आज की DutyPe AI सीमा पूरी हो गई। कल फिर मिलेगी।",
  },
};

const QUESTIONS: Record<Lang, Record<string, string>> = {
  en: {
    job: "What work do you need people for?",
    pay: "How much will you pay: per day, per week or per month? (up to ₹50,000)",
    about: "Tell me a little about the work: what they will do, the timings and where.",
    ready: "Shall I fill the form so you can check and post it?",
  },
  te: {
    job: "మీకు ఏ పని కోసం మనుషులు కావాలి?",
    pay: "ఎంత జీతం ఇస్తారు? రోజుకా, వారానికా, నెలకా? (₹50,000 వరకు)",
    about: "పని గురించి కొంచెం చెప్పండి: ఏం చేయాలి, టైమింగ్స్, ఎక్కడ.",
    ready: "ఫారం నింపుతాను, చూసి పోస్ట్ చేయండి. సరేనా?",
  },
  hi: {
    job: "आपको किस काम के लिए लोग चाहिए?",
    pay: "कितनी तनख्वाह देंगे? रोज़ की, हफ़्ते की या महीने की? (₹50,000 तक)",
    about: "काम के बारे में थोड़ा बताइए: क्या करना है, टाइमिंग और जगह।",
    ready: "फ़ॉर्म भर दूँ? आप देखकर पोस्ट कर दीजिए।",
  },
};

export function summaryOf(d: JobDraft, l: Lang): string {
  const per = { DAILY: { en: "per day", te: "రోజుకు", hi: "प्रति दिन" }, WEEKLY: { en: "per week", te: "వారానికి", hi: "प्रति सप्ताह" },
    MONTHLY: { en: "per month", te: "నెలకు", hi: "प्रति माह" },
    HOURLY: { en: "per hour", te: "గంటకు", hi: "प्रति घंटा" }, NEGOTIABLE: { en: "pay to discuss", te: "జీతం మాట్లాడుకుందాం", hi: "तनख्वाह बात करके" } } as const;
  const payText = d.payType === "NEGOTIABLE" ? per.NEGOTIABLE[l] : `₹${d.payAmount} ${per[d.payType as keyof typeof per][l]}`;
  return `${d.vacancies} × ${d.title || "?"} · ${payText}`;
}

export const aiJobAssistant = onCallSecured({ timeoutSeconds: 30 }, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const transcript = str(data, "transcript", { max: 1500, optional: true });
  const l = lang(data.lang);
  let draft = cleanDraft(obj(data.draft));
  const access: AiAccess = transcript ? await useAi(uid) : "plan";
  if (!canUseAi(access)) {
    return { draft, missing: missingFields(draft), ready: false, locked: access, question: LOCKED[access === "limit" ? "limit" : "upgrade"][l], summary: summaryOf(draft, l) };
  }
  if (transcript) {
    const prompt = `You fill a job post for a small Indian employer from what they said (Telugu, Hindi, English or mixed).
Current draft: ${JSON.stringify(draft)}
They just said: ${JSON.stringify(transcript)}
Update the draft with anything new; keep earlier values unless they changed them. Use ONLY these values:
category: one of ${JSON.stringify(CATEGORY_KEYS)} (pick the closest; OTHER only if nothing fits)
employmentType: ${JSON.stringify(FORM.employmentTypes)}; payType: ${JSON.stringify(FORM.payTypes)}; shift: ${JSON.stringify(FORM.shifts)}
gender: ${JSON.stringify(FORM.genders)} (Both unless they clearly require one); experience: ${JSON.stringify(FORM.experience)}
education: ${JSON.stringify(FORM.education)}; perks: subset of ${JSON.stringify(FORM.perks)}
payAmount: rupees as a number (per the payType), at most 50000; vacancies: number of people (1-50)
title: short English job title (e.g. "Delivery Boy", "Shop Helper"), max 6 words
description: 2-4 short sentences in ${LANG_NAME[l]} describing the work, timings and place they mentioned. Do not invent facts: if they have said nothing about the work itself beyond the job name, leave it "".
Return JSON with exactly the draft keys.`;
    const out = await ai.json(prompt);
    if (out && typeof out === "object") draft = cleanDraft({ ...draft, ...(out as Record<string, unknown>) });
    else await refundAi(uid, access);
  }
  const missing = missingFields(draft);
  return {
    draft,
    missing,
    ready: missing.length === 0,
    question: QUESTIONS[l][missing[0] ?? "ready"],
    summary: summaryOf(draft, l),
  };
});
