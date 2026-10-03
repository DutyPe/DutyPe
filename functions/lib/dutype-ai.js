"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.dutypeAi = void 0;
exports.employerFacts = employerFacts;
exports.quickAnswer = quickAnswer;
exports.validateAction = validateAction;
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
const admin = require("firebase-admin");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const ai_hiring_1 = require("./ai-hiring");
const schema_1 = require("./schema");
const referral_posts_1 = require("./lib/referral-posts");
const pay_rules_1 = require("./lib/pay-rules");
const services_1 = require("./services");
const azure_1 = require("./lib/azure");
const db = admin.firestore();
const LANGS = ["en", "te", "hi"];
const LANG_NAME = { en: "simple English", te: "simple Telugu (Telugu script)", hi: "simple Hindi (Devanagari)" };
const DAY_MS = 24 * 60 * 60 * 1000;
const IST_OFFSET_MS = 330 * 60 * 1000;
const URGENT_WINDOWS = ["right_now", "within_1_hour", "today", "tomorrow"];
/** Same limits as jobs.ts / instant.ts (free posts per day, free urgent posts in total). */
const FREE_JOB_POSTS_PER_DAY = 3;
const FREE_URGENT_POSTS = 3;
const ms = (v) => { var _a, _b; return (_b = (_a = v === null || v === void 0 ? void 0 : v.toMillis) === null || _a === void 0 ? void 0 : _a.call(v)) !== null && _b !== void 0 ? _b : 0; };
/** The employer's own data, compact (about 130 document reads). */
async function employerFacts(uid) {
    const S = schema_1.EmployerProfiles.Subscription;
    const nowMs = Date.now();
    const day = new Date(nowMs + IST_OFFSET_MS).toISOString().slice(0, 10);
    const [profile, jobsSnap, appsSnap, urgentSnap, quota, bookingsSnap, servicesConfig] = await Promise.all([
        db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid).get(),
        db.collection(schema_1.Jobs.COLLECTION).where(schema_1.Jobs.EMPLOYER_ID, "==", uid).orderBy(schema_1.Jobs.CREATED_AT, "desc").limit(20).get(),
        db.collection(schema_1.Applications.COLLECTION).where(schema_1.Applications.EMPLOYER_ID, "==", uid).orderBy(schema_1.Applications.CREATED_AT, "desc").limit(100).get(),
        db.collection(schema_1.InstantRequests.COLLECTION).where(schema_1.InstantRequests.EMPLOYER_ID, "==", uid).orderBy(schema_1.InstantRequests.CREATED_AT, "desc").limit(10).get(),
        db.collection(schema_1.Idempotency.COLLECTION).doc(`postQuota_${uid}_${day}`).get(),
        db.collection(schema_1.ServiceBookings.COLLECTION).where(schema_1.ServiceBookings.CUSTOMER_ID, "==", uid)
            .orderBy(schema_1.ServiceBookings.CREATED_AT, "desc").limit(5).get().catch(() => null),
        (0, services_1.loadConfig)().catch(() => null),
    ]);
    const now = Date.now();
    const todayStart = Math.floor((now + IST_OFFSET_MS) / DAY_MS) * DAY_MS - IST_OFFSET_MS;
    const sub = (0, input_1.obj)(profile.get(schema_1.EmployerProfiles.SUBSCRIPTION));
    const credits = (0, input_1.obj)(sub[S.CREDITS]);
    const titles = new Map(jobsSnap.docs.map((d) => [d.id, String(d.get(schema_1.Jobs.TITLE) || "")]));
    const applicants = appsSnap.docs.map((a) => ({
        id: a.id,
        jobId: String(a.get(schema_1.Applications.JOB_ID)),
        job: titles.get(String(a.get(schema_1.Applications.JOB_ID))) || "",
        name: String(a.get(schema_1.Applications.WORKER_NAME) || "Worker"),
        status: String(a.get(schema_1.Applications.STATUS)),
        appliedToday: ms(a.get(schema_1.Applications.CREATED_AT)) >= todayStart,
    }));
    const hired = applicants.filter((a) => a.status === "hired" || a.status === "completed");
    const expiresAt = ms(sub[S.EXPIRES_AT]);
    const active = sub[S.STATUS] === "ACTIVE" && (expiresAt === 0 || expiresAt > nowMs);
    const owner = String(profile.get(schema_1.EmployerProfiles.OWNER_NAME) || "").trim();
    return {
        employerName: String(profile.get(schema_1.EmployerProfiles.BUSINESS_NAME) || owner || ""),
        firstName: owner.split(/\s+/)[0] || "",
        area: String(profile.get(schema_1.EmployerProfiles.AREA) || ""),
        plan: {
            aiIncluded: sub[S.AI] === true || sub[S.PLAN_ID] === "UNLIMITED_CAMPAIGN",
            freeAiLeft: Math.max(0, ai_hiring_1.FREE_AI_TRIAL - Number(profile.get(schema_1.EmployerProfiles.AI_TRIAL_USED) || 0)),
            jobCredits: Number(credits[S.CREDITS_NORMAL] || 0),
            urgentCredits: Number(credits[S.CREDITS_INSTANT] || 0),
            planId: String(sub[S.PLAN_ID] || "NONE"),
            active,
            expiresInDays: active && expiresAt ? Math.max(0, Math.ceil((expiresAt - nowMs) / DAY_MS)) : null,
            freeJobPostsLeftToday: Math.max(0, FREE_JOB_POSTS_PER_DAY - Number(quota.get(schema_1.Idempotency.RESULT) || 0)),
            freeUrgentPostsLeft: Math.max(0, FREE_URGENT_POSTS - Number(profile.get(schema_1.EmployerProfiles.FREE_URGENT_POSTS_USED) || 0)),
            referralFreePosts: (0, referral_posts_1.referralPostsLeft)(profile.data(), nowMs),
        },
        jobs: jobsSnap.docs.map((d) => ({
            id: d.id,
            title: String(d.get(schema_1.Jobs.TITLE) || ""),
            status: String(d.get(schema_1.Jobs.STATUS) || ""),
            category: String(d.get(schema_1.Jobs.CATEGORY) || ""),
            vacancies: Number(d.get(schema_1.Jobs.VACANCIES) || 1),
            applicants: Number(d.get(schema_1.Jobs.APPLICATION_COUNT) || 0),
            postedDaysAgo: Math.floor((now - ms(d.get(schema_1.Jobs.CREATED_AT))) / DAY_MS),
        })),
        applicants,
        urgent: urgentSnap.docs.map((d) => ({
            id: d.id,
            title: String(d.get(schema_1.InstantRequests.TITLE) || ""),
            status: String(d.get(schema_1.InstantRequests.STATUS) || ""),
            needed: Number(d.get(schema_1.InstantRequests.WORKERS_NEEDED) || 1),
            accepted: (d.get(schema_1.InstantRequests.SELECTED_WORKER_IDS) || []).length,
        })),
        homeServices: {
            bookings: ((bookingsSnap === null || bookingsSnap === void 0 ? void 0 : bookingsSnap.docs) || []).map((d) => ({
                id: d.id,
                service: String(d.get(schema_1.ServiceBookings.SERVICE_NAME) || ""),
                status: String(d.get(schema_1.ServiceBookings.STATUS) || ""),
                total: Number(d.get(schema_1.ServiceBookings.TOTAL) || 0),
                partner: String(d.get(schema_1.ServiceBookings.PARTNER_NAME) || ""),
                when: new Date(ms(d.get(schema_1.ServiceBookings.CREATED_AT)) + IST_OFFSET_MS).toISOString().slice(0, 16).replace("T", " "),
            })),
            catalog: ((servicesConfig === null || servicesConfig === void 0 ? void 0 : servicesConfig.services) || []).filter((x) => x.active !== false)
                .map((x) => ({ id: x.id, name: x.name, category: x.category, price: x.price })),
            city: (servicesConfig === null || servicesConfig === void 0 ? void 0 : servicesConfig.city) || "Khammam",
        },
        stats: {
            openJobs: jobsSnap.docs.filter((d) => d.get(schema_1.Jobs.STATUS) === "open").length,
            appliedToday: applicants.filter((a) => a.appliedToday).length,
            waiting: applicants.filter((a) => a.status === "applied").length,
            hired: hired.length,
            hiredNames: hired.slice(0, 10).map((a) => a.name),
        },
    };
}
/** Answers from the data without AI: today's applications and hires. Null when not that kind of question. */
function quickAnswer(message, f, l) {
    const m = message.toLowerCase();
    const names = f.stats.hiredNames.slice(0, 5).join(", ");
    if (/hire|hired|నియమ|తీసుకున్న|रखा|हायर/.test(m)) {
        return {
            en: f.stats.hired ? `You have hired ${f.stats.hired}: ${names}.` : "You have not hired anyone yet.",
            te: f.stats.hired ? `మీరు ${f.stats.hired} మందిని తీసుకున్నారు: ${names}.` : "మీరు ఇంకా ఎవరినీ తీసుకోలేదు.",
            hi: f.stats.hired ? `आपने ${f.stats.hired} लोगों को रखा है: ${names}.` : "आपने अभी किसी को नहीं रखा है।",
        }[l];
    }
    if (/credit|plan|free post|ఉచిత|క్రెడిట్|ప్లాన్|मुफ़्त|क्रेडिट|प्लान/.test(m)) {
        const p = f.plan;
        const posts = p.freeJobPostsLeftToday + p.referralFreePosts;
        return {
            en: `You can post ${posts} more job${posts === 1 ? "" : "s"} free today and ${p.freeUrgentPostsLeft} free urgent post${p.freeUrgentPostsLeft === 1 ? "" : "s"}. ` +
                `Plan credits: ${p.jobCredits} job, ${p.urgentCredits} urgent.`,
            te: `ఈరోజు ఇంకా ${posts} జాబ్‌లు, ${p.freeUrgentPostsLeft} అర్జెంట్ పోస్ట్‌లు ఉచితంగా చేయవచ్చు. ప్లాన్ క్రెడిట్స్: ${p.jobCredits} జాబ్, ${p.urgentCredits} అర్జెంట్.`,
            hi: `आज आप ${posts} और जॉब और ${p.freeUrgentPostsLeft} अर्जेंट पोस्ट मुफ़्त कर सकते हैं। प्लान क्रेडिट: ${p.jobCredits} जॉब, ${p.urgentCredits} अर्जेंट।`,
        }[l];
    }
    if (/service|booking|ac |plumber|electrician|cleaning|సర్వీస్|బుకింగ్|सर्विस|बुकिंग/.test(m) && f.homeServices.bookings.length) {
        const b = f.homeServices.bookings[0];
        return {
            en: `Your latest home service: ${b.service}, status ${b.status.toLowerCase().replace(/_/g, " ")}${b.partner ? `, partner ${b.partner}` : ""}.`,
            te: `మీ తాజా ఇంటి సేవ: ${b.service}, స్థితి ${b.status.toLowerCase().replace(/_/g, " ")}${b.partner ? `, పార్ట్నర్ ${b.partner}` : ""}.`,
            hi: `आपकी आख़िरी घरेलू सेवा: ${b.service}, स्थिति ${b.status.toLowerCase().replace(/_/g, " ")}${b.partner ? `, पार्टनर ${b.partner}` : ""}.`,
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
/** Keeps only actions that make sense for this employer's own data. */
function validateAction(raw, f) {
    const a = (0, input_1.obj)(raw);
    const args = (0, input_1.obj)(a.args);
    const type = String(a.type || "");
    const job = f.jobs.find((j) => j.id === args.jobId);
    const app = f.applicants.find((x) => x.id === args.applicationId);
    switch (type) {
        case "post_job": {
            const draft = (0, ai_hiring_1.cleanDraft)(args);
            if ((0, ai_hiring_1.missingFields)(draft).length)
                return null;
            if ((0, pay_rules_1.vacancyPayProblem)(draft.payType, draft.employmentType, draft.payAmount))
                return null;
            const pay = draft.payType === "NEGOTIABLE" ? "pay to discuss" : `₹${draft.payAmount} ${draft.payType.toLowerCase()}`;
            return { type, args: Object.assign({}, draft), summary: `Post: ${draft.vacancies} × ${draft.title}, ${pay}` };
        }
        case "post_urgent": {
            const category = String(args.category || "").toUpperCase();
            const title = String(args.title || "").trim().slice(0, 80);
            const workersNeeded = Math.round(Number(args.workersNeeded) || 0);
            const payPerPerson = Math.round(Number(args.payPerPerson) || 0);
            const window = URGENT_WINDOWS.includes(String(args.window)) ? String(args.window) : "right_now";
            if (title.length < 3 || workersNeeded < 1 || workersNeeded > 20 || (0, pay_rules_1.urgentPayProblem)(payPerPerson))
                return null;
            return {
                type,
                args: {
                    title, workersNeeded, payPerPerson, window,
                    category: schema_1.CATEGORY_KEYS.includes(category) ? category : "HELPER",
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
        case "book_service": {
            const svc = f.homeServices.catalog.find((x) => x.id === args.serviceId);
            return svc ? { type, args: { serviceId: svc.id }, summary: `Book ${svc.name} (₹${svc.price}) at home` } : null;
        }
        case "open_service_booking": {
            const b = f.homeServices.bookings.find((x) => x.id === args.bookingId);
            return b ? { type, args: { bookingId: b.id }, summary: `Open booking: ${b.service}` } : null;
        }
        default:
            return null;
    }
}
function lang(value) {
    return LANGS.includes(String(value)) ? value : "en";
}
const BUSY = {
    en: "DutyPe AI is busy right now. Please try again in a minute. Your free try was not used.",
    te: "DutyPe AI ఇప్పుడు బిజీగా ఉంది. ఒక నిమిషం తర్వాత మళ్లీ ప్రయత్నించండి. మీ ఉచిత అవకాశం వాడలేదు.",
    hi: "DutyPe AI अभी व्यस्त है। एक मिनट बाद फिर कोशिश करें। आपका मुफ़्त मौका इस्तेमाल नहीं हुआ।",
};
exports.dutypeAi = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 30, memory: "512MB", enforceAppCheck: false, secrets: [azure_1.AZURE_OPENAI_SECRET, azure_1.AZURE_COSMOS_SECRET] }, async (raw, context) => {
    var _a, _b, _c, _d, _e;
    const uid = context.auth.uid;
    if (((_a = context.auth) === null || _a === void 0 ? void 0 : _a.token.role) !== "EMPLOYER")
        (0, input_1.fail)("permission-denied", "DutyPe AI is for employers");
    const data = (0, input_1.obj)(raw);
    const message = (0, input_1.str)(data, "message", { min: 1, max: 1000 });
    const l = lang(data.lang);
    const history = (Array.isArray(data.history) ? data.history : []).slice(-6).map((h) => {
        const t = (0, input_1.obj)(h);
        return { from: t.role === "ai" ? "DutyPe AI" : "Employer", text: String(t.text || "").slice(0, 300) };
    });
    const facts = await employerFacts(uid);
    const access = await (0, ai_hiring_1.useAi)(uid);
    if (access !== "plan" && access !== "trial") {
        return { reply: (_b = quickAnswer(message, facts, l)) !== null && _b !== void 0 ? _b : ai_hiring_1.LOCKED[access === "limit" ? "limit" : "upgrade"][l], action: null, stats: facts.stats, locked: access };
    }
    const prompt = `You are "DutyPe AI", the friendly hiring assistant inside the DutyPe app for small Indian employers.
Reply in ${LANG_NAME[l]}, in 1-3 short sentences that sound natural when read aloud. Be warm and practical.
Use ONLY these facts about this employer (never invent numbers, names or jobs): ${JSON.stringify(Object.assign(Object.assign({}, facts), { stats: undefined, summary: facts.stats }))}
Conversation so far: ${JSON.stringify(history)}
Employer now says: ${JSON.stringify(message)}

You may propose ONE action; the employer will confirm before anything happens:
- post_job: a regular job. args: {title (short English), category (one of ${JSON.stringify(schema_1.CATEGORY_KEYS)}), employmentType ${JSON.stringify(ai_hiring_1.FORM.employmentTypes)}, payAmount (rupees, at most 50000), payType ${JSON.stringify(ai_hiring_1.FORM.payTypes)}, vacancies, shift ${JSON.stringify(ai_hiring_1.FORM.shifts)}, gender ${JSON.stringify(ai_hiring_1.FORM.genders)}, experience ${JSON.stringify(ai_hiring_1.FORM.experience)}, education ${JSON.stringify(ai_hiring_1.FORM.education)}, perks ${JSON.stringify(ai_hiring_1.FORM.perks)}, description (2-3 sentences in ${LANG_NAME[l]} from what they told you about the work)}
- post_urgent: same-day / short work (hours to 2 days), workers come quickly. args: {title, category, workersNeeded (1-20), payPerPerson (rupees, 200-2000), window ${JSON.stringify(URGENT_WINDOWS)}, durationText}
Regular jobs (post_job) are only weekly or monthly paid: monthly ₹3,000-40,000 (full-time at least ₹8,000), weekly ₹1,000-10,000. Daily or hourly work is post_urgent. If the pay they say is outside these limits, explain kindly and ask again.
- hire / reject: args {applicationId} (from the facts)
- close_job / renew_job / open_job: args {jobId} (from the facts)
- book_service: a home service (AC repair, cleaning, electrician, plumber, appliance repair) at their home in ${facts.homeServices.city}, from homeServices.catalog. args {serviceId}. The booking screen opens; they confirm the address and time there.
- open_service_booking: args {bookingId} (from homeServices.bookings)
Posting a job by talking: collect the details in a natural conversation, ONE short question at a time, in this order — what work / role, how many people, pay (amount and per day / month), timings or shift, and what the work involves. Use what they already said; never ask again for something they told you. When you have enough, read back a one-line summary and propose post_job (or post_urgent if they need people today / right now). Pay is at most ₹50,000.
Answer questions about their jobs, applicants, hires, urgent posts, plan, credits, free posts left and home-service bookings exactly from the facts (use the real numbers and names). If something is not in the facts, say you don't have it.
Address them by first name now and then (${JSON.stringify(facts.firstName)}). When you propose an action, end the reply by asking them to confirm.
Return JSON: {"reply": "...", "action": null or {"type": "...", "args": {...}}}`;
    const reply0 = await ai_hiring_1.ai.json(prompt);
    if (reply0 === null) {
        // The AI did not answer: give the action back and still answer counts / hires from the data.
        await (0, ai_hiring_1.refundAi)(uid, access);
        const fallback = (_c = quickAnswer(message, facts, l)) !== null && _c !== void 0 ? _c : BUSY[l];
        await logConversation(uid, l, message, fallback, null, true);
        return { reply: fallback, action: null, stats: facts.stats, aiDown: true };
    }
    const out = (0, input_1.obj)(reply0);
    const action = validateAction(out.action, facts);
    const reply = String(out.reply || "").trim().slice(0, 600) || ((_d = quickAnswer(message, facts, l)) !== null && _d !== void 0 ? _d : {
        en: "Sorry, I did not get that. You can say: post a job, how many applied, or who is hired.",
        te: "క్షమించండి, అర్థం కాలేదు. మీరు: పని పోస్ట్ చేయి, ఎంత మంది దరఖాస్తు చేశారు, ఎవరిని తీసుకున్నాను అని అడగవచ్చు.",
        hi: "माफ़ कीजिए, समझ नहीं आया। आप कह सकते हैं: नौकरी पोस्ट करो, कितने लोगों ने आवेदन किया, किसे रखा है।",
    }[l]);
    await logConversation(uid, l, message, reply, (_e = action === null || action === void 0 ? void 0 : action.type) !== null && _e !== void 0 ? _e : null, false);
    return { reply, action, stats: facts.stats };
});
/** What employers ask DutyPe AI, kept in Azure Cosmos DB (container "ai_logs") to improve the AI. */
async function logConversation(uid, l, message, reply, action, aiDown) {
    await (0, azure_1.cosmosAdd)(azure_1.CosmosContainers.AI_LOGS, { uid, lang: l, message, reply, action, aiDown });
}
//# sourceMappingURL=dutype-ai.js.map