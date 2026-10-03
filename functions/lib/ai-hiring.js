"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.aiJobAssistant = exports.LOCKED = exports.FORM = exports.nearbyWorkerCount = exports.aiShortlist = exports.ai = exports.FREE_AI_TRIAL = void 0;
exports.aiConfigured = aiConfigured;
exports.useAi = useAi;
exports.refundAi = refundAi;
exports.requiredYears = requiredYears;
exports.scoreCandidate = scoreCandidate;
exports.cleanDraft = cleanDraft;
exports.missingFields = missingFields;
exports.summaryOf = summaryOf;
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
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const secure_callable_1 = require("./secure-callable");
const azure_1 = require("./lib/azure");
const input_1 = require("./lib/input");
const geo_1 = require("./lib/geo");
const schema_1 = require("./schema");
const db = admin.firestore();
const { Timestamp } = admin.firestore;
const MAX_APPLICANTS = 50;
const AI_CANDIDATES = 8;
const PICKS = 3;
const SHORTLIST_CACHE_MS = 12 * 60 * 60 * 1000;
/** Free DutyPe AI actions for employers without an AI plan. */
exports.FREE_AI_TRIAL = 10;
const DAY_MS = 24 * 60 * 60 * 1000;
const IST_OFFSET_MS = 330 * 60 * 1000;
const LANGS = ["en", "te", "hi"];
const LANG_NAME = { en: "simple English", te: "simple Telugu (Telugu script)", hi: "simple Hindi (Devanagari)" };
// ─────────────────────────────── AI provider ───────────────────────────────
/** True when any AI provider is set up (Azure OpenAI first, Gemini as the fallback). */
function aiConfigured() {
    return (0, azure_1.azureOpenAiConfigured)() || Boolean(process.env.GEMINI_API_KEY);
}
/** Test seam: replaced in tests so no network call is made. */
exports.ai = {
    /** Azure OpenAI when configured (paid from the Azure credits), otherwise Gemini. */
    async json(prompt) {
        if ((0, azure_1.azureOpenAiConfigured)())
            return (0, azure_1.chatJson)(prompt);
        return geminiJson(prompt);
    },
};
async function geminiJson(prompt) {
    var _a, _b, _c, _d;
    const key = process.env.GEMINI_API_KEY || "";
    if (!key)
        return null;
    const model = process.env.GEMINI_MODEL || "gemini-2.0-flash";
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 12000);
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
        if (!res.ok)
            throw new Error(`Gemini ${res.status}`);
        const body = await res.json();
        const text = ((_d = (_c = (_b = (_a = body.candidates) === null || _a === void 0 ? void 0 : _a[0]) === null || _b === void 0 ? void 0 : _b.content) === null || _c === void 0 ? void 0 : _c.parts) === null || _d === void 0 ? void 0 : _d.map((p) => p.text || "").join("")) || "";
        return JSON.parse(text);
    }
    catch (e) {
        functions.logger.warn("Gemini call failed", e);
        return null;
    }
    finally {
        clearTimeout(timer);
    }
}
/**
 * Spends one DutyPe AI action if the employer may: an active AI plan (or the launch campaign) within
 * its daily allowance, else one of the free trial actions. "upgrade" / "limit" mean no AI this time.
 */
async function useAi(uid) {
    const S = schema_1.EmployerProfiles.Subscription;
    const profileRef = db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid);
    const day = Math.floor((Date.now() + IST_OFFSET_MS) / DAY_MS);
    const dayRef = db.collection(schema_1.Idempotency.COLLECTION).doc(`aiDay_${uid}_${day}`);
    return db.runTransaction(async (tx) => {
        var _a, _b, _c;
        const [profile, dayDoc] = await Promise.all([tx.get(profileRef), tx.get(dayRef)]);
        if (!profile.exists)
            return "upgrade";
        const sub = (0, input_1.obj)(profile.get(schema_1.EmployerProfiles.SUBSCRIPTION));
        const expires = (_c = (_b = (_a = sub[S.EXPIRES_AT]) === null || _a === void 0 ? void 0 : _a.toMillis) === null || _b === void 0 ? void 0 : _b.call(_a)) !== null && _c !== void 0 ? _c : 0;
        const active = sub[S.STATUS] === "ACTIVE" && (expires === 0 || expires > Date.now());
        const campaign = active && sub[S.PLAN_ID] === "UNLIMITED_CAMPAIGN";
        if (active && (campaign || sub[S.AI] === true)) {
            const cap = Number(sub[S.AI_PER_DAY]) || 100;
            const used = Number(dayDoc.get(schema_1.Idempotency.RESULT) || 0);
            if (used >= cap)
                return "limit";
            tx.set(dayRef, { [schema_1.Idempotency.RESULT]: used + 1, [schema_1.Idempotency.EXPIRE_AT]: Timestamp.fromMillis(Date.now() + 2 * DAY_MS) });
            return "plan";
        }
        const trialUsed = Number(profile.get(schema_1.EmployerProfiles.AI_TRIAL_USED) || 0);
        if (trialUsed >= exports.FREE_AI_TRIAL)
            return "upgrade";
        tx.update(profileRef, { [schema_1.EmployerProfiles.AI_TRIAL_USED]: trialUsed + 1 });
        return "trial";
    });
}
const canUseAi = (a) => a === "plan" || a === "trial";
/** Gives back the action [useAi] spent when the AI then did not answer (key, quota or network). */
async function refundAi(uid, access) {
    try {
        if (access === "trial") {
            await db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid)
                .update({ [schema_1.EmployerProfiles.AI_TRIAL_USED]: admin.firestore.FieldValue.increment(-1) });
        }
        else if (access === "plan") {
            const day = Math.floor((Date.now() + IST_OFFSET_MS) / DAY_MS);
            await db.collection(schema_1.Idempotency.COLLECTION).doc(`aiDay_${uid}_${day}`)
                .set({ [schema_1.Idempotency.RESULT]: admin.firestore.FieldValue.increment(-1) }, { merge: true });
        }
    }
    catch (e) {
        functions.logger.warn("AI refund failed", e);
    }
}
function lang(value) {
    return LANGS.includes(String(value)) ? value : "en";
}
// ─────────────────────────────── shortlist ───────────────────────────────
/** "1-3 years" → 1, "Min. 6.0 months" → 0, "Freshers can apply" → 0. */
function requiredYears(text) {
    const s = String(text || "").toLowerCase();
    if (!s || /fresher|no experience|any/.test(s) || /month/.test(s))
        return 0;
    const n = s.match(/\d+/);
    return n ? Math.min(20, Number(n[0])) : 0;
}
/**
 * The fixed, explainable score (0–100-ish). Skill 35 · distance up to 20 · experience up to 15 ·
 * rating up to 10 · completed jobs up to 10 · recently active up to 8 · available 3 · no-shows −8 each.
 */
function scoreCandidate(job, card, nowMs) {
    var _a, _b;
    const skills = Array.isArray(card[schema_1.WorkerCards.SKILLS]) ? card[schema_1.WorkerCards.SKILLS].map(String) : [];
    const facts = [];
    let score = 0;
    const hasSkill = job.category !== "OTHER" && skills.includes(job.category);
    if (hasSkill) {
        score += 35;
        facts.push("skill");
    }
    else if (job.category === "OTHER")
        score += 15;
    let km = null;
    const points = [];
    if (Number.isFinite(Number(card[schema_1.WorkerCards.LAT])) && card[schema_1.WorkerCards.LAT] !== undefined)
        points.push([Number(card[schema_1.WorkerCards.LAT]), Number(card[schema_1.WorkerCards.LNG])]);
    if (typeof card[schema_1.WorkerCards.CURRENT_CELL] === "string") {
        const b = (0, geo_1.decodeGeohash)(card[schema_1.WorkerCards.CURRENT_CELL]);
        points.push([(b.minLat + b.maxLat) / 2, (b.minLng + b.maxLng) / 2]);
    }
    if (points.length && Number.isFinite(job.lat))
        km = Math.min(...points.map(([la, ln]) => (0, geo_1.distanceKm)(job.lat, job.lng, la, ln)));
    score += km === null ? 5 : km <= 3 ? 20 : km <= 5 ? 17 : km <= 10 ? 12 : km <= 20 ? 6 : 0;
    const years = Number(card[schema_1.WorkerCards.EXPERIENCE_YEARS] || 0);
    score += years >= job.requiredYears ? 10 + Math.min(5, years) : 10 * (years / Math.max(1, job.requiredYears));
    const rating = Number(card[schema_1.WorkerCards.RATING] || 0);
    const ratingCount = Number(card[schema_1.WorkerCards.RATING_COUNT] || 0);
    score += ratingCount > 0 ? Math.min(10, rating * 2) : 4;
    const done = Number(card[schema_1.WorkerCards.JOBS_COMPLETED] || 0);
    score += Math.min(10, done * 2);
    const noShows = Number(card[schema_1.WorkerCards.NO_SHOWS] || 0);
    score -= Math.min(24, noShows * 8);
    const lastActive = (_b = (_a = card[schema_1.WorkerCards.LAST_ACTIVE_AT]) === null || _a === void 0 ? void 0 : _a.toMillis()) !== null && _b !== void 0 ? _b : 0;
    const activeDays = lastActive ? (nowMs - lastActive) / 864e5 : Infinity;
    score += activeDays <= 1 ? 8 : activeDays <= 7 ? 5 : 0;
    if (card[schema_1.WorkerCards.AVAILABLE] === true)
        score += 3;
    if (km !== null)
        facts.push(`${km < 1 ? "<1" : Math.round(km)} km`);
    if (years > 0)
        facts.push(`${years}+ yrs`);
    if (ratingCount > 0)
        facts.push(`★${rating.toFixed(1)}`);
    if (done > 0)
        facts.push(`${done} jobs done`);
    if (noShows > 0)
        facts.push(`${noShows} no-show`);
    return {
        score: Math.round(score),
        distanceKm: km === null ? null : Math.round(km * 10) / 10,
        facts,
        profile: {
            hasJobSkill: hasSkill, otherSkills: skills.filter((s) => s !== job.category).slice(0, 4),
            distanceKm: km === null ? "unknown" : Math.round(km), experienceYears: years,
            rating: ratingCount > 0 ? rating : "new", jobsCompleted: done, noShows,
            activeRecently: activeDays <= 7, availableNow: card[schema_1.WorkerCards.AVAILABLE] === true,
        },
    };
}
function templateReason(c, l) {
    const f = c.profile;
    const parts = {
        en: [f.hasJobSkill ? "Has the skill" : "", f.distanceKm !== "unknown" ? `${f.distanceKm} km away` : "", f.experienceYears ? `${f.experienceYears}+ years experience` : "", f.jobsCompleted ? `${f.jobsCompleted} jobs done` : ""],
        te: [f.hasJobSkill ? "ఈ పని తెలుసు" : "", f.distanceKm !== "unknown" ? `${f.distanceKm} కి.మీ దూరం` : "", f.experienceYears ? `${f.experienceYears}+ ఏళ్ల అనుభవం` : "", f.jobsCompleted ? `${f.jobsCompleted} పనులు పూర్తి` : ""],
        hi: [f.hasJobSkill ? "यह काम आता है" : "", f.distanceKm !== "unknown" ? `${f.distanceKm} किमी दूर` : "", f.experienceYears ? `${f.experienceYears}+ साल अनुभव` : "", f.jobsCompleted ? `${f.jobsCompleted} काम पूरे` : ""],
    };
    return parts[l].filter(Boolean).join(" · ");
}
exports.aiShortlist = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 30, memory: "512MB", enforceAppCheck: false, secrets: [azure_1.AZURE_OPENAI_SECRET] }, async (raw, context) => {
    var _a, _b;
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const jobId = (0, input_1.str)(data, "jobId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
    const l = lang(data.lang);
    const [jobSnap, detailsSnap] = await Promise.all([
        db.collection(schema_1.Jobs.COLLECTION).doc(jobId).get(),
        db.collection(schema_1.JobDetails.COLLECTION).doc(jobId).get(),
    ]);
    if (!jobSnap.exists || jobSnap.get(schema_1.Jobs.EMPLOYER_ID) !== uid)
        (0, input_1.fail)("permission-denied", "Not your job");
    const apps = (await db.collection(schema_1.Applications.COLLECTION)
        .where(schema_1.Applications.EMPLOYER_ID, "==", uid)
        .where(schema_1.Applications.JOB_ID, "==", jobId)
        .orderBy(schema_1.Applications.CREATED_AT, "asc")
        .limit(MAX_APPLICANTS)
        .get()).docs.filter((a) => ["applied", "hired"].includes(String(a.get(schema_1.Applications.STATUS))));
    if (!apps.length)
        return { picks: [], considered: 0, byAi: false };
    const cacheRef = db.collection(schema_1.Idempotency.COLLECTION).doc(`aiPicks_${jobId}_${l}`);
    const cached = await cacheRef.get();
    const signature = apps.map((a) => a.id).join(",");
    const cachedAt = (_b = (_a = cached.get("at")) === null || _a === void 0 ? void 0 : _a.toMillis()) !== null && _b !== void 0 ? _b : 0;
    if (cached.exists && cached.get("signature") === signature && Date.now() - cachedAt < SHORTLIST_CACHE_MS) {
        return cached.get(schema_1.Idempotency.RESULT);
    }
    const cards = await db.getAll(...apps.map((a) => db.collection(schema_1.WorkerCards.COLLECTION).doc(String(a.get(schema_1.Applications.WORKER_ID)))));
    const cardById = new Map(cards.map((c) => [c.id, c.data() || {}]));
    const jobFacts = {
        category: String(jobSnap.get(schema_1.Jobs.CATEGORY) || "OTHER"),
        lat: Number(jobSnap.get(schema_1.Jobs.LAT)),
        lng: Number(jobSnap.get(schema_1.Jobs.LNG)),
        requiredYears: requiredYears(detailsSnap.get(schema_1.JobDetails.EXPERIENCE_REQUIRED)),
    };
    const now = Date.now();
    const ranked = apps.map((a) => {
        const workerId = String(a.get(schema_1.Applications.WORKER_ID));
        const s = scoreCandidate(jobFacts, cardById.get(workerId) || {}, now);
        return Object.assign({ applicationId: a.id, workerId, name: String(a.get(schema_1.Applications.WORKER_NAME) || "Worker"), photoUrl: String(a.get(schema_1.Applications.WORKER_PHOTO) || "") }, s);
    }).sort((x, y) => y.score - x.score);
    const pool = ranked.slice(0, AI_CANDIDATES);
    let chosen = [];
    let byAi = false;
    const access = pool.length > 1 ? await useAi(uid) : "plan";
    if (pool.length > 1 && canUseAi(access)) {
        const prompt = `You help a small Indian business owner hire for one job. Pick the best ${Math.min(PICKS, pool.length)} candidates, best first.
Job: ${JSON.stringify({
            title: jobSnap.get(schema_1.Jobs.TITLE), category: jobFacts.category, payType: jobSnap.get(schema_1.Jobs.PAY_TYPE),
            description: String(detailsSnap.get(schema_1.JobDetails.DESCRIPTION) || "").slice(0, 600),
            experienceRequired: detailsSnap.get(schema_1.JobDetails.EXPERIENCE_REQUIRED) || "", educationRequired: detailsSnap.get(schema_1.JobDetails.EDUCATION_REQUIRED) || "",
        })}
Candidates (ids are anonymous; use only these facts): ${JSON.stringify(pool.map((c, i) => (Object.assign({ id: `C${i + 1}`, fixedScore: c.score }, c.profile))))}
Rules: judge only on job-relevant facts and the job's stated needs. Never consider or guess gender, religion, caste, age or names. Prefer having the skill, being near, reliability (jobs done, no no-shows) and experience.
Write each reason in ${LANG_NAME[l]}, at most 12 words, citing the facts (e.g. distance, experience, jobs done).
Return JSON: {"picks":[{"id":"C1","reason":"..."}]}`;
        const out = await exports.ai.json(prompt);
        const seen = new Set();
        for (const p of (out === null || out === void 0 ? void 0 : out.picks) || []) {
            const idx = Number(String(p.id || "").replace(/\D/g, "")) - 1;
            if (!(idx >= 0 && idx < pool.length) || seen.has(idx))
                continue;
            seen.add(idx);
            chosen.push({ c: pool[idx], reason: String(p.reason || "").slice(0, 140) || templateReason(pool[idx], l) });
            if (chosen.length >= PICKS)
                break;
        }
        byAi = chosen.length > 0;
        if (out === null)
            await refundAi(uid, access);
    }
    if (!chosen.length)
        chosen = ranked.slice(0, PICKS).map((c) => ({ c, reason: templateReason(c, l) }));
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
        [schema_1.Idempotency.RESULT]: result, signature, at: Timestamp.fromMillis(now),
        [schema_1.Idempotency.EXPIRE_AT]: Timestamp.fromMillis(now + 2 * SHORTLIST_CACHE_MS),
    });
    return result;
});
// ─────────────────────────────── nearby supply ───────────────────────────────
/**
 * Available workers with a skill near a point: within 5 km and within 10 km. Count queries over the
 * workers' matching keys (each ~1 read). A worker whose home and current area fall in different
 * 30-cell chunks can be counted twice, so the app shows it as "about".
 */
exports.nearbyWorkerCount = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 15 }, async (raw) => {
    const data = (0, input_1.obj)(raw);
    const { lat, lng } = (0, input_1.latLng)(data);
    const category = String(data.category || "").toUpperCase();
    const tag = schema_1.CATEGORY_KEYS.includes(category) && category !== "OTHER" ? category : "ANY";
    const count = async (cells) => {
        let total = 0;
        for (let i = 0; i < cells.length; i += 30) {
            const agg = await db.collection(schema_1.WorkerCards.COLLECTION)
                .where(schema_1.WorkerCards.AVAILABLE, "==", true)
                .where(schema_1.WorkerCards.SKILL_CELLS, "array-contains-any", cells.slice(i, i + 30).map((c) => `${tag}_${c}`))
                .count().get();
            total += agg.data().count;
        }
        return total;
    };
    const near = (0, geo_1.coveringCells)(lat, lng, 5, 5);
    const nearSet = new Set(near);
    const ring = (0, geo_1.coveringCells)(lat, lng, 10, 5).filter((c) => !nearSet.has(c));
    const [within5, ring10] = await Promise.all([count(near), count(ring)]);
    return { category: tag, within5km: within5, within10km: within5 + ring10 };
});
// ─────────────────────────────── talk-to-post ───────────────────────────────
/** The posting form's exact options (PostJobScreen); the assistant must answer with these. */
exports.FORM = {
    // Regular jobs only: weekly or monthly pay, full- or part-time (daily work is an urgent post).
    payTypes: ["MONTHLY", "WEEKLY"],
    employmentTypes: ["FULL_TIME", "PART_TIME"],
    shifts: ["DAY", "NIGHT", "ANY"],
    genders: ["Both", "Male", "Female"],
    experience: ["No Experience Required", "Fresher (Educated)", "1-3 years", "3-5 years", "5+ years"],
    education: ["No qualification required", "10th pass", "12th pass", "ITI", "Diploma", "Graduate", "Any qualification"],
    perks: ["Food Provided", "Transport", "Overtime Bonus", "Accommodation"],
};
const pick = (value, options, fallback) => {
    var _a;
    const v = String(value !== null && value !== void 0 ? value : "").trim().toLowerCase();
    return (_a = options.find((o) => o.toLowerCase() === v)) !== null && _a !== void 0 ? _a : fallback;
};
/** Cleans any (AI or client) draft into valid form values. */
function cleanDraft(d) {
    const category = String(d.category || "").toUpperCase().replace(/[^A-Z_]/g, "");
    return {
        title: String(d.title || "").trim().slice(0, 80),
        category: schema_1.CATEGORY_KEYS.includes(category) ? category : "",
        employmentType: pick(d.employmentType, exports.FORM.employmentTypes, "FULL_TIME"),
        // Above the ₹50,000 limit counts as not given, so the assistant asks again.
        payAmount: ((n) => (n > schema_1.MAX_PAY_RUPEES ? 0 : n))(Math.max(0, Math.round(Number(d.payAmount) || 0))),
        payType: pick(d.payType, exports.FORM.payTypes, "MONTHLY"),
        vacancies: Math.max(1, Math.min(50, Math.round(Number(d.vacancies) || 1))),
        shift: pick(d.shift, exports.FORM.shifts, "ANY"),
        gender: pick(d.gender, exports.FORM.genders, "Both"),
        experience: pick(d.experience, exports.FORM.experience, "No Experience Required"),
        education: pick(d.education, exports.FORM.education, "No qualification required"),
        perks: (Array.isArray(d.perks) ? d.perks : []).map((p) => pick(p, exports.FORM.perks, "")).filter(Boolean),
        description: String(d.description || "").trim().slice(0, 1500),
    };
}
/** What still has to be asked before the job can be posted. */
function missingFields(d) {
    const out = [];
    if (!d.title || !d.category)
        out.push("job");
    if (d.payType !== "NEGOTIABLE" && d.payAmount <= 0)
        out.push("pay");
    // The post needs at least one line about the work (the server requires 10+ characters).
    if (d.description.trim().length < 10)
        out.push("about");
    return out;
}
exports.LOCKED = {
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
const QUESTIONS = {
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
function summaryOf(d, l) {
    const per = { DAILY: { en: "per day", te: "రోజుకు", hi: "प्रति दिन" }, WEEKLY: { en: "per week", te: "వారానికి", hi: "प्रति सप्ताह" },
        MONTHLY: { en: "per month", te: "నెలకు", hi: "प्रति माह" },
        HOURLY: { en: "per hour", te: "గంటకు", hi: "प्रति घंटा" }, NEGOTIABLE: { en: "pay to discuss", te: "జీతం మాట్లాడుకుందాం", hi: "तनख्वाह बात करके" } };
    const payText = d.payType === "NEGOTIABLE" ? per.NEGOTIABLE[l] : `₹${d.payAmount} ${per[d.payType][l]}`;
    return `${d.vacancies} × ${d.title || "?"} · ${payText}`;
}
exports.aiJobAssistant = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 30, enforceAppCheck: false, secrets: [azure_1.AZURE_OPENAI_SECRET] }, async (raw, context) => {
    var _a;
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const transcript = (0, input_1.str)(data, "transcript", { max: 1500, optional: true });
    const l = lang(data.lang);
    let draft = cleanDraft((0, input_1.obj)(data.draft));
    const access = transcript ? await useAi(uid) : "plan";
    if (!canUseAi(access)) {
        return { draft, missing: missingFields(draft), ready: false, locked: access, question: exports.LOCKED[access === "limit" ? "limit" : "upgrade"][l], summary: summaryOf(draft, l) };
    }
    if (transcript) {
        const prompt = `You fill a job post for a small Indian employer from what they said (Telugu, Hindi, English or mixed).
Current draft: ${JSON.stringify(draft)}
They just said: ${JSON.stringify(transcript)}
Update the draft with anything new; keep earlier values unless they changed them. Use ONLY these values:
category: one of ${JSON.stringify(schema_1.CATEGORY_KEYS)} (pick the closest; OTHER only if nothing fits)
employmentType: ${JSON.stringify(exports.FORM.employmentTypes)}; payType: ${JSON.stringify(exports.FORM.payTypes)}; shift: ${JSON.stringify(exports.FORM.shifts)}
gender: ${JSON.stringify(exports.FORM.genders)} (Both unless they clearly require one); experience: ${JSON.stringify(exports.FORM.experience)}
education: ${JSON.stringify(exports.FORM.education)}; perks: subset of ${JSON.stringify(exports.FORM.perks)}
payAmount: rupees as a number (per the payType): monthly 3000-40000 (full-time at least 8000), weekly 1000-10000 (full-time at least 2000); vacancies: number of people (1-50)
title: short English job title (e.g. "Delivery Boy", "Shop Helper"), max 6 words
description: 2-4 short sentences in ${LANG_NAME[l]} describing the work, timings and place they mentioned. Do not invent facts: if they have said nothing about the work itself beyond the job name, leave it "".
Return JSON with exactly the draft keys.`;
        const out = await exports.ai.json(prompt);
        if (out && typeof out === "object")
            draft = cleanDraft(Object.assign(Object.assign({}, draft), out));
        else
            await refundAi(uid, access);
    }
    const missing = missingFields(draft);
    return {
        draft,
        missing,
        ready: missing.length === 0,
        question: QUESTIONS[l][(_a = missing[0]) !== null && _a !== void 0 ? _a : "ready"],
        summary: summaryOf(draft, l),
    };
});
//# sourceMappingURL=ai-hiring.js.map