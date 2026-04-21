/**
 * Notification translations.
 *
 * Source of truth for every push/in-app notification body the backend sends.
 * Pattern: every CF resolves the recipient's language via `getUserLanguage`
 * (see user-language.ts), then calls `t(templateId, lang, params)` to render
 * title and body.
 *
 * Adding a new template:
 *   1. Add an entry below with both `en` and `te` strings.
 *   2. Use {placeholder} tokens — they'll be replaced from the params map.
 *   3. Call `tTitle(id, lang, params)` and `tBody(id, lang, params)` in the CF.
 *
 * Fallback: if a locale string is missing, the English copy is used. If a
 * template id is missing entirely, the literal id is returned (so it shows up
 * loudly in logs/UI rather than silently dropping).
 */
import * as admin from "firebase-admin";

export type SupportedLocale = "en" | "te";
export const SUPPORTED_LOCALES: SupportedLocale[] = ["en", "te"];
export const DEFAULT_LOCALE: SupportedLocale = "en";

interface LocalizedString {
  title: string;
  body: string;
}

type Translations = Record<string, Record<SupportedLocale, LocalizedString>>;

export const NOTIFICATION_TEMPLATES: Translations = {
  // ── application status ────────────────────────────────────────────
  APPLICATION_HIRED: {
    en: {
      title: "You're hired! 🎉",
      body: "An employer has accepted your application.",
    },
    te: {
      title: "మీరు ఎంపికయ్యారు! 🎉",
      body: "ఒక యజమాని మీ దరఖాస్తును అంగీకరించారు.",
    },
  },
  APPLICATION_SHORTLISTED: {
    en: {
      title: "You've been shortlisted",
      body: "An employer is reviewing your application.",
    },
    te: {
      title: "మీరు షార్ట్‌లిస్ట్ అయ్యారు",
      body: "ఒక యజమాని మీ దరఖాస్తును సమీక్షిస్తున్నారు.",
    },
  },
  APPLICATION_REJECTED: {
    en: {
      title: "Application update",
      body: "Your application wasn't selected this time.",
    },
    te: {
      title: "దరఖాస్తు అప్‌డేట్",
      body: "ఈసారి మీ దరఖాస్తు ఎంపిక కాలేదు.",
    },
  },
  APPLICATION_STATUS_OTHER: {
    en: {
      title: "Application update",
      body: "Status: {status}",
    },
    te: {
      title: "దరఖాస్తు అప్‌డేట్",
      body: "స్థితి: {status}",
    },
  },
  NEW_APPLICATION_RECEIVED: {
    en: {
      title: "New application received",
      body: "A worker has applied to your job posting.",
    },
    te: {
      title: "కొత్త దరఖాస్తు వచ్చింది",
      body: "ఒక కార్మికుడు మీ ఉద్యోగానికి దరఖాస్తు చేశారు.",
    },
  },

  // ── referral system ───────────────────────────────────────────────
  REFERRAL_REWARD_BASIC: {
    en: {
      title: "Referral Successful",
      body: "{name} joined using your code. You earned ₹{amount}.",
    },
    te: {
      title: "రిఫరల్ విజయవంతం",
      body: "{name} మీ కోడ్‌ని ఉపయోగించి చేరారు. మీరు ₹{amount} సంపాదించారు.",
    },
  },
  REFERRAL_REWARD_WITH_BONUS: {
    en: {
      title: "🎉 Referral Successful!",
      body: "{name} joined using your code. You earned ₹{amount} (includes ₹{bonus} milestone bonus!)",
    },
    te: {
      title: "🎉 రిఫరల్ విజయవంతం!",
      body: "{name} మీ కోడ్‌తో చేరారు. మీరు ₹{amount} సంపాదించారు (₹{bonus} మైలురాయి బోనస్ సహా!)",
    },
  },
  SIGNUP_BONUS: {
    en: {
      title: "🎁 Welcome Bonus",
      body: "You earned ₹{amount} for joining with a referral code.",
    },
    te: {
      title: "🎁 స్వాగత బోనస్",
      body: "రిఫరల్ కోడ్‌తో చేరినందుకు మీరు ₹{amount} సంపాదించారు.",
    },
  },

  // ── transactional reminders & re-engagement ────────────────────────
  JOB_EXPIRY_SOON: {
    en: {
      title: "⏰ Job Expiring Soon",
      body: "Your job \"{jobTitle}\" expires in {hoursLeft} hours. Renew it to keep receiving applications.",
    },
    te: {
      title: "⏰ ఉద్యోగం త్వరలో గడువు ముగుస్తుంది",
      body: "మీ \"{jobTitle}\" ఉద్యోగం {hoursLeft} గంటల్లో గడువు ముగుస్తుంది. దరఖాస్తులు రావడానికి దానిని పునరుద్ధరించండి.",
    },
  },
  EMPLOYER_PENDING_APPLICATIONS: {
    en: {
      title: "📋 Pending Applications",
      body: "You have {count} pending applications waiting for your review. Don't miss out on great candidates!",
    },
    te: {
      title: "📋 పెండింగ్ దరఖాస్తులు",
      body: "మీ సమీక్ష కోసం {count} దరఖాస్తులు పెండింగ్‌లో ఉన్నాయి. మంచి అభ్యర్థులను మిస్ అవ్వకండి!",
    },
  },
  WORKER_PENDING_APPLICATION: {
    en: {
      title: "⏰ Application Still Pending",
      body: "Your application for \"{jobTitle}\" has been pending for {daysPending} days. For faster updates, call the employer directly!",
    },
    te: {
      title: "⏰ దరఖాస్తు ఇంకా పెండింగ్‌లో ఉంది",
      body: "\"{jobTitle}\" కోసం మీ దరఖాస్తు {daysPending} రోజులుగా పెండింగ్‌లో ఉంది. వేగంగా అప్‌డేట్‌ల కోసం, యజమానిని నేరుగా కాల్ చేయండి!",
    },
  },
  WORKER_RE_ENGAGEMENT: {
    en: {
      title: "💼 New Jobs Waiting For You!",
      body: "Check out the latest job opportunities near you. Your next opportunity is just a tap away!",
    },
    te: {
      title: "💼 కొత్త ఉద్యోగాలు మీ కోసం వేచి ఉన్నాయి!",
      body: "మీ సమీపంలోని తాజా ఉద్యోగ అవకాశాలను చూడండి. మీ తదుపరి అవకాశం ఒక్క ట్యాప్ దూరంలో ఉంది!",
    },
  },
  EMPLOYER_RE_ENGAGEMENT: {
    en: {
      title: "🏢 Ready to Hire?",
      body: "Post a job and connect with thousands of qualified workers in your area. Hiring made easy!",
    },
    te: {
      title: "🏢 నియమించడానికి సిద్ధంగా ఉన్నారా?",
      body: "ఉద్యోగాన్ని పోస్ట్ చేయండి మరియు మీ ప్రాంతంలోని వేలాది నైపుణ్యం గల కార్మికులతో కనెక్ట్ అవ్వండి. సులభంగా నియామకం!",
    },
  },

  // ── scheduled smart engagement: workers ───────────────────────────
  SE_WORKER_FRESH_JOBS: {
    en: { title: "🎯 Fresh jobs matching your skills", body: "New openings nearby — apply early for the best chance." },
    te: { title: "🎯 మీ నైపుణ్యాలకు సరిపడే కొత్త ఉద్యోగాలు", body: "సమీపంలో కొత్త ఖాళీలు — ఉత్తమ అవకాశం కోసం ముందు దరఖాస్తు చేయండి." },
  },
  SE_WORKER_HIRING_TODAY: {
    en: { title: "💼 Employers are actively hiring today", body: "Don't miss out — check the latest openings now." },
    te: { title: "💼 యజమానులు ఈరోజు చురుకుగా నియమిస్తున్నారు", body: "మిస్ అవ్వకండి — తాజా ఖాళీలను ఇప్పుడే చూడండి." },
  },
  SE_WORKER_NEARBY_5KM: {
    en: { title: "📍 Jobs within 5 km of you", body: "Walk-in interviews available near your location." },
    te: { title: "📍 మీకు 5 కి.మీ. లోపు ఉద్యోగాలు", body: "మీ ప్రాంతంలో వాక్-ఇన్ ఇంటర్వ్యూలు అందుబాటులో ఉన్నాయి." },
  },
  SE_WORKER_URGENT_HIRE: {
    en: { title: "🔥 Urgent hire — apply before it fills up", body: "Some jobs posted today are closing fast." },
    te: { title: "🔥 అత్యవసర నియామకం — త్వరగా దరఖాస్తు చేయండి", body: "ఈరోజు పోస్ట్ చేసిన కొన్ని ఉద్యోగాలు త్వరగా మూసివేయబడుతున్నాయి." },
  },
  SE_WORKER_NEW_COMPANIES: {
    en: { title: "🌟 New companies just joined DutyPe", body: "Check their open positions before others do." },
    te: { title: "🌟 కొత్త కంపెనీలు DutyPeలో చేరాయి", body: "ఇతరుల కంటే ముందే వారి ఖాళీలను చూడండి." },
  },
  SE_WORKER_QUICK_ACTION: {
    en: { title: "⚡ One quick action can change your day", body: "Update your skills or apply to a job to stay visible." },
    te: { title: "⚡ ఒక చిన్న చర్య మీ రోజును మార్చగలదు", body: "మీ నైపుణ్యాలను అప్‌డేట్ చేయండి లేదా ఉద్యోగానికి దరఖాస్తు చేయండి." },
  },
  SE_WORKER_SMALL_STEPS: {
    en: { title: "📈 Small steps, big opportunities", body: "Keep your profile active — employers notice consistency." },
    te: { title: "📈 చిన్న అడుగులు, పెద్ద అవకాశాలు", body: "మీ ప్రొఫైల్‌ను చురుకుగా ఉంచండి — యజమానులు స్థిరత్వాన్ని గమనిస్తారు." },
  },
  SE_WORKER_STAND_OUT: {
    en: { title: "🏆 Stand out from other applicants", body: "Complete your profile to rank higher in search results." },
    te: { title: "🏆 ఇతర దరఖాస్తుదారుల నుండి ప్రత్యేకంగా నిలవండి", body: "శోధన ఫలితాల్లో పైకి రావడానికి మీ ప్రొఫైల్‌ను పూర్తి చేయండి." },
  },
  SE_WORKER_NEXT_JOB: {
    en: { title: "💪 Your next job could be one tap away", body: "Open DutyPe and see what's new for you." },
    te: { title: "💪 మీ తదుపరి ఉద్యోగం ఒక్క ట్యాప్ దూరంలో ఉండవచ్చు", body: "DutyPe తెరిచి మీ కోసం ఏమి కొత్తదో చూడండి." },
  },
  SE_WORKER_GOOD_MORNING: {
    en: { title: "🎉 Good morning! Ready to find work?", body: "Fresh daily and hourly jobs waiting for you." },
    te: { title: "🎉 శుభోదయం! పని వెతకడానికి సిద్ధంగా ఉన్నారా?", body: "మీ కోసం రోజువారీ మరియు గంట ఉద్యోగాలు ఎదురుచూస్తున్నాయి." },
  },
  SE_WORKER_EVENING_CHECK: {
    en: { title: "🌅 Evening check — any interviews coming up?", body: "Review your applications and prepare for tomorrow." },
    te: { title: "🌅 సాయంత్రం చెక్ — ఏవైనా ఇంటర్వ్యూలు ఉన్నాయా?", body: "మీ దరఖాస్తులను సమీక్షించి రేపటికి సిద్ధమవ్వండి." },
  },
  SE_WORKER_UNREAD_UPDATES: {
    en: { title: "📱 You have unread updates", body: "An employer may have responded to your application." },
    te: { title: "📱 మీకు చదవని అప్‌డేట్‌లు ఉన్నాయి", body: "ఒక యజమాని మీ దరఖాస్తుకు స్పందించి ఉండవచ్చు." },
  },

  // ── scheduled smart engagement: employers ─────────────────────────
  SE_EMPLOYER_REVIEW_CANDIDATES: {
    en: { title: "👀 Candidates waiting for your review", body: "Review applications now — don't lose top talent." },
    te: { title: "👀 అభ్యర్థులు మీ సమీక్ష కోసం ఎదురుచూస్తున్నారు", body: "ఇప్పుడు దరఖాస్తులను సమీక్షించండి — టాప్ టాలెంట్‌ను కోల్పోకండి." },
  },
  SE_EMPLOYER_NEW_APPLICATIONS: {
    en: { title: "📬 New applications on your job post", body: "Workers have applied — check their profiles today." },
    te: { title: "📬 మీ ఉద్యోగ పోస్ట్‌పై కొత్త దరఖాస్తులు", body: "కార్మికులు దరఖాస్తు చేశారు — వారి ప్రొఫైల్‌లను ఈరోజు చూడండి." },
  },
  SE_EMPLOYER_DONT_WAIT: {
    en: { title: "⏰ Don't keep applicants waiting", body: "Quick responses improve your hiring success rate." },
    te: { title: "⏰ దరఖాస్తుదారులను వేచి ఉంచకండి", body: "త్వరిత స్పందనలు మీ నియామక విజయ రేటును మెరుగుపరుస్తాయి." },
  },
  SE_EMPLOYER_REFRESH_POST: {
    en: { title: "🚀 Refresh your job post for more visibility", body: "Updated posts get 3x more applications." },
    te: { title: "🚀 ఎక్కువ దృశ్యమానత కోసం మీ ఉద్యోగ పోస్ట్‌ను రిఫ్రెష్ చేయండి", body: "అప్‌డేట్ చేసిన పోస్ట్‌లకు 3x ఎక్కువ దరఖాస్తులు వస్తాయి." },
  },
  SE_EMPLOYER_PERFORMANCE: {
    en: { title: "📊 Your job post performance", body: "See how many workers viewed and applied today." },
    te: { title: "📊 మీ ఉద్యోగ పోస్ట్ పనితీరు", body: "ఈరోజు ఎంతమంది కార్మికులు చూసారో, దరఖాస్తు చేశారో చూడండి." },
  },
  SE_EMPLOYER_TIP_SALARY: {
    en: { title: "💡 Tip: Add salary range to attract more workers", body: "Posts with clear pay get 50% more applications." },
    te: { title: "💡 చిట్కా: ఎక్కువ మంది కార్మికులను ఆకర్షించడానికి జీతం పరిధిని జోడించండి", body: "స్పష్టమైన చెల్లింపు ఉన్న పోస్ట్‌లకు 50% ఎక్కువ దరఖాస్తులు వస్తాయి." },
  },
  SE_EMPLOYER_HIRE_FASTER: {
    en: { title: "🏆 Hire faster with DutyPe", body: "Take one hiring action today to keep momentum." },
    te: { title: "🏆 DutyPe తో వేగంగా నియమించండి", body: "మొమెంటంను కొనసాగించడానికి ఈరోజు ఒక నియామక చర్య తీసుకోండి." },
  },
  SE_EMPLOYER_FOLLOWUP: {
    en: { title: "📞 Have you contacted your shortlisted candidates?", body: "Quick follow-up prevents candidate drop-off." },
    te: { title: "📞 మీ షార్ట్‌లిస్ట్ చేసిన అభ్యర్థులను సంప్రదించారా?", body: "త్వరిత ఫాలో-అప్ అభ్యర్థి డ్రాప్-ఆఫ్‌ను నివారిస్తుంది." },
  },
  SE_EMPLOYER_RESPOND_24H: {
    en: { title: "✅ Great employers respond within 24 hours", body: "Stay on top of your applications to build trust." },
    te: { title: "✅ గొప్ప యజమానులు 24 గంటలలోపు స్పందిస్తారు", body: "విశ్వాసం పెంచడానికి మీ దరఖాస్తులపై దృష్టి పెట్టండి." },
  },

  // ── guest engagement (broadcast topic, no recipient lookup) ──────
  SE_GUEST_MORNING_NEW_JOBS: {
    en: { title: "💼 New jobs near you are waiting!", body: "Login to apply in one tap — don't miss out." },
    te: { title: "💼 మీ సమీపంలో కొత్త ఉద్యోగాలు ఎదురుచూస్తున్నాయి!", body: "ఒక్క ట్యాప్‌తో దరఖాస్తు చేయడానికి లాగిన్ అవ్వండి — మిస్ కాకండి." },
  },
  SE_GUEST_MORNING_GOOD_MORNING: {
    en: { title: "🌅 Good morning! Fresh jobs just posted", body: "Sign in to see openings near your location." },
    te: { title: "🌅 శుభోదయం! తాజా ఉద్యోగాలు పోస్ట్ అయ్యాయి", body: "మీ ప్రాంతంలోని ఖాళీలను చూడటానికి సైన్ ఇన్ అవ్వండి." },
  },
  SE_GUEST_MORNING_SKILLS: {
    en: { title: "🎯 Your skills are in demand today", body: "Create your profile and get matched instantly." },
    te: { title: "🎯 ఈరోజు మీ నైపుణ్యాలకు డిమాండ్ ఉంది", body: "మీ ప్రొఫైల్ సృష్టించండి మరియు తక్షణమే మ్యాచ్ అవ్వండి." },
  },
  SE_GUEST_AFTERNOON_FILLING: {
    en: { title: "🔥 Jobs filling up fast today", body: "Sign in and apply before they're gone." },
    te: { title: "🔥 ఈరోజు ఉద్యోగాలు త్వరగా నిండిపోతున్నాయి", body: "అవి అయిపోకముందే సైన్ ఇన్ అయి దరఖాస్తు చేయండి." },
  },
  SE_GUEST_AFTERNOON_HIRING: {
    en: { title: "⚡ Employers are hiring RIGHT NOW", body: "One-tap apply — login to get started." },
    te: { title: "⚡ యజమానులు ఇప్పుడే నియమిస్తున్నారు", body: "ఒక్క ట్యాప్ దరఖాస్తు — ప్రారంభించడానికి లాగిన్ అవ్వండి." },
  },
  SE_GUEST_AFTERNOON_WALKIN: {
    en: { title: "📍 Walk-in interviews near you", body: "Sign in to see which companies are hiring today." },
    te: { title: "📍 మీ సమీపంలో వాక్-ఇన్ ఇంటర్వ్యూలు", body: "ఏ కంపెనీలు ఈరోజు నియమిస్తున్నాయో చూడటానికి సైన్ ఇన్ అవ్వండి." },
  },
  SE_GUEST_EVENING_PROFILE: {
    en: { title: "🔓 Complete your profile, unlock matches", body: "Personalised job recommendations are waiting — sign in now." },
    te: { title: "🔓 మీ ప్రొఫైల్‌ను పూర్తి చేయండి, మ్యాచ్‌లను అన్‌లాక్ చేయండి", body: "వ్యక్తిగతీకరించిన ఉద్యోగ సిఫార్సులు ఎదురుచూస్తున్నాయి — ఇప్పుడే సైన్ ఇన్ అవ్వండి." },
  },
  SE_GUEST_EVENING_TOMORROW: {
    en: { title: "🌟 Tomorrow could be your first day at work", body: "Sign in tonight, apply, and get hired tomorrow." },
    te: { title: "🌟 రేపు మీ మొదటి పని రోజు కావచ్చు", body: "ఈ రాత్రి సైన్ ఇన్ అయి దరఖాస్తు చేయండి, రేపు ఉద్యోగం పొందండి." },
  },
  SE_GUEST_EVENING_THOUSANDS: {
    en: { title: "💪 Thousands found jobs on DutyPe", body: "Join them — create your profile in under 2 minutes." },
    te: { title: "💪 వేల మంది DutyPeలో ఉద్యోగాలు పొందారు", body: "వారిలో చేరండి — 2 నిమిషాలలో మీ ప్రొఫైల్ సృష్టించండి." },
  },

  // ── birthday ─────────────────────────────────────────────────────
  BIRTHDAY: {
    en: {
      title: "🎂 Happy Birthday, {name}! 🎉",
      body: "Wishing you a wonderful birthday filled with joy and success! May this year bring you amazing opportunities. - Team DutyPe",
    },
    te: {
      title: "🎂 జన్మదిన శుభాకాంక్షలు, {name}! 🎉",
      body: "మీకు ఆనందం, విజయంతో నిండిన అద్భుతమైన జన్మదిన శుభాకాంక్షలు! ఈ సంవత్సరం మీకు అద్భుతమైన అవకాశాలను తీసుకురావాలని కోరుకుంటున్నాము. - DutyPe బృందం",
    },
  },
  // ── moderation ───────────────────────────────────────────────────
  JOB_UNDER_REVIEW: {
    en: {
      title: "Job Under Review",
      body: "Your job \"{title}\" needs manual review due to duplicate signals.",
    },
    te: {
      title: "ఉద్యోగం సమీక్షలో ఉంది",
      body: "డూప్లికేట్ సిగ్నల్‌ల కారణంగా మీ ఉద్యోగం \"{title}\" మాన్యువల్ సమీక్ష అవసరం.",
    },
  },
  JOB_HIDDEN_REPORTS: {
    en: {
      title: "Job Hidden for Review",
      body: "Your job \"{title}\" has been hidden due to community reports.",
    },
    te: {
      title: "ఉద్యోగం సమీక్ష కోసం దాచబడింది",
      body: "కమ్యూనిటీ నివేదికల కారణంగా మీ ఉద్యోగం \"{title}\" దాచబడింది.",
    },
  },
};

/** Maps a worker smart-engagement scheduled-template index → template id. */
export const SE_WORKER_POOL: Array<{ id: keyof typeof NOTIFICATION_TEMPLATES; deepLink: string; timeOfDay?: "morning" | "afternoon" | "evening" }> = [
  { id: "SE_WORKER_FRESH_JOBS", deepLink: "dutype://jobs" },
  { id: "SE_WORKER_HIRING_TODAY", deepLink: "dutype://jobs" },
  { id: "SE_WORKER_NEARBY_5KM", deepLink: "dutype://jobs" },
  { id: "SE_WORKER_URGENT_HIRE", deepLink: "dutype://jobs" },
  { id: "SE_WORKER_NEW_COMPANIES", deepLink: "dutype://jobs" },
  { id: "SE_WORKER_QUICK_ACTION", deepLink: "dutype://profile" },
  { id: "SE_WORKER_SMALL_STEPS", deepLink: "dutype://profile" },
  { id: "SE_WORKER_STAND_OUT", deepLink: "dutype://profile" },
  { id: "SE_WORKER_NEXT_JOB", deepLink: "dutype://jobs" },
  { id: "SE_WORKER_GOOD_MORNING", deepLink: "dutype://jobs", timeOfDay: "morning" },
  { id: "SE_WORKER_EVENING_CHECK", deepLink: "dutype://my-jobs", timeOfDay: "evening" },
  { id: "SE_WORKER_UNREAD_UPDATES", deepLink: "dutype://notifications" },
];

export const SE_EMPLOYER_POOL: Array<{ id: keyof typeof NOTIFICATION_TEMPLATES; deepLink: string; timeOfDay?: "morning" | "afternoon" | "evening" }> = [
  { id: "SE_EMPLOYER_REVIEW_CANDIDATES", deepLink: "dutype://applications" },
  { id: "SE_EMPLOYER_NEW_APPLICATIONS", deepLink: "dutype://applications" },
  { id: "SE_EMPLOYER_DONT_WAIT", deepLink: "dutype://applications" },
  { id: "SE_EMPLOYER_REFRESH_POST", deepLink: "dutype://post-job" },
  { id: "SE_EMPLOYER_PERFORMANCE", deepLink: "dutype://employer/home" },
  { id: "SE_EMPLOYER_TIP_SALARY", deepLink: "dutype://post-job" },
  { id: "SE_EMPLOYER_HIRE_FASTER", deepLink: "dutype://employer/home" },
  { id: "SE_EMPLOYER_FOLLOWUP", deepLink: "dutype://applications" },
  { id: "SE_EMPLOYER_RESPOND_24H", deepLink: "dutype://applications" },
];

export const SE_GUEST_POOL: Array<{ id: keyof typeof NOTIFICATION_TEMPLATES; timeOfDay: "morning" | "afternoon" | "evening" }> = [
  { id: "SE_GUEST_MORNING_NEW_JOBS", timeOfDay: "morning" },
  { id: "SE_GUEST_MORNING_GOOD_MORNING", timeOfDay: "morning" },
  { id: "SE_GUEST_MORNING_SKILLS", timeOfDay: "morning" },
  { id: "SE_GUEST_AFTERNOON_FILLING", timeOfDay: "afternoon" },
  { id: "SE_GUEST_AFTERNOON_HIRING", timeOfDay: "afternoon" },
  { id: "SE_GUEST_AFTERNOON_WALKIN", timeOfDay: "afternoon" },
  { id: "SE_GUEST_EVENING_PROFILE", timeOfDay: "evening" },
  { id: "SE_GUEST_EVENING_TOMORROW", timeOfDay: "evening" },
  { id: "SE_GUEST_EVENING_THOUSANDS", timeOfDay: "evening" },
];

export function normalizeLocale(value: unknown): SupportedLocale {
  const raw = String(value ?? "").trim().toLowerCase();
  if (raw === "te") return "te";
  return DEFAULT_LOCALE;
}

function applyParams(template: string, params?: Record<string, string | number>): string {
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (_, key) =>
    params[key] !== undefined && params[key] !== null ? String(params[key]) : `{${key}}`
  );
}

function pickLocalized(templateId: string, locale: SupportedLocale): LocalizedString | null {
  const entry = NOTIFICATION_TEMPLATES[templateId];
  if (!entry) return null;
  return entry[locale] ?? entry[DEFAULT_LOCALE] ?? null;
}

export function tTitle(
  templateId: string,
  locale: SupportedLocale,
  params?: Record<string, string | number>
): string {
  const localized = pickLocalized(templateId, locale);
  if (!localized) return templateId;
  return applyParams(localized.title, params);
}

export function tBody(
  templateId: string,
  locale: SupportedLocale,
  params?: Record<string, string | number>
): string {
  const localized = pickLocalized(templateId, locale);
  if (!localized) return templateId;
  return applyParams(localized.body, params);
}

/**
 * Resolves the recipient's preferred locale from `users/{uid}.language`.
 * Returns "en" when the field is absent, malformed, or the user doc is missing.
 * Caller must pass an initialised admin Firestore instance.
 */
export async function getUserLanguage(
  db: admin.firestore.Firestore,
  userId: string
): Promise<SupportedLocale> {
  if (!userId) return DEFAULT_LOCALE;
  try {
    const snap = await db.collection("users").doc(userId).get();
    if (!snap.exists) return DEFAULT_LOCALE;
    return normalizeLocale(snap.get("language"));
  } catch {
    return DEFAULT_LOCALE;
  }
}

/**
 * Bulk-resolve languages for many users in a single round-trip.
 * Returns a map { userId -> locale }; missing users default to "en".
 */
export async function getUserLanguagesBulk(
  db: admin.firestore.Firestore,
  userIds: string[]
): Promise<Map<string, SupportedLocale>> {
  const out = new Map<string, SupportedLocale>();
  if (userIds.length === 0) return out;

  const unique = Array.from(new Set(userIds.filter(Boolean)));
  for (let i = 0; i < unique.length; i += 30) {
    const chunk = unique.slice(i, i + 30);
    const refs = chunk.map((id) => db.collection("users").doc(id));
    const docs = await db.getAll(...refs);
    docs.forEach((doc) => {
      out.set(doc.id, doc.exists ? normalizeLocale(doc.get("language")) : DEFAULT_LOCALE);
    });
  }

  unique.forEach((id) => {
    if (!out.has(id)) out.set(id, DEFAULT_LOCALE);
  });
  return out;
}

/**
 * Topic naming for language-specific FCM broadcasts. Android subscribes to
 * `${baseTopic}_${language}` so admin broadcasts can deliver the right copy.
 */
export function localizedTopic(baseTopic: string, locale: SupportedLocale): string {
  return `${baseTopic}_${locale}`;
}
