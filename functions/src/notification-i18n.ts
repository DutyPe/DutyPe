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

export type SupportedLocale = "en" | "te" | "hi";
export const SUPPORTED_LOCALES: SupportedLocale[] = ["en", "te", "hi"];
export const DEFAULT_LOCALE: SupportedLocale = "en";

interface LocalizedString {
  title: string;
  body: string;
}

// English is mandatory; other locales are optional so a template can be
// translated incrementally. pickLocalized falls back to English per template.
type Translations = Record<
  string,
  { en: LocalizedString } & Partial<Record<SupportedLocale, LocalizedString>>
>;

export const NOTIFICATION_TEMPLATES: Translations = {
  // ── application status ────────────────────────────────────────────
  // `{recipient}` resolves to the worker's first name when known (see
  // getUserDisplayName). When unknown, the greeting collapses cleanly.
  APPLICATION_HIRED: {
    en: {
      title: "Hi {recipient}, you're hired! 🎉",
      body: "An employer has accepted your application.",
    },
    te: {
      title: "హాయ్ {recipient}, మీరు ఎంపికయ్యారు! 🎉",
      body: "ఒక యజమాని మీ దరఖాస్తును అంగీకరించారు.",
    },
    hi: {
      title: "नमस्ते {recipient}, आपको नौकरी मिल गई! 🎉",
      body: "एक नियोक्ता ने आपका आवेदन स्वीकार कर लिया है।",
    },
  },
  APPLICATION_SHORTLISTED: {
    en: {
      title: "Hi {recipient}, you've been shortlisted",
      body: "An employer is reviewing your application.",
    },
    te: {
      title: "హాయ్ {recipient}, మీరు షార్ట్‌లిస్ట్ అయ్యారు",
      body: "ఒక యజమాని మీ దరఖాస్తును సమీక్షిస్తున్నారు.",
    },
    hi: {
      title: "नमस्ते {recipient}, आप शॉर्टलिस्ट हुए",
      body: "एक नियोक्ता आपका आवेदन देख रहा है।",
    },
  },
  APPLICATION_REJECTED: {
    en: {
      title: "Hi {recipient}, application update",
      body: "Your application wasn't selected this time.",
    },
    te: {
      title: "హాయ్ {recipient}, దరఖాస్తు అప్‌డేట్",
      body: "ఈసారి మీ దరఖాస్తు ఎంపిక కాలేదు.",
    },
    hi: {
      title: "नमस्ते {recipient}, आवेदन अपडेट",
      body: "इस बार आपका आवेदन चयनित नहीं हुआ।",
    },
  },
  APPLICATION_WITHDRAWN: {
    en: {
      title: "Changed your mind? Happens!",
      body: "You withdrew this application. Let's find the right job for you.",
    },
    te: {
      title: "పరవాలేదు, మీ నిర్ణయం మార్చుకున్నారా?",
      body: "మీరు ఈ దరఖాస్తును ఉపసంహరించుకున్నారు. మీకు సరైన ఉద్యోగం కనుగొందాం.",
    },
    hi: {
      title: "मन बदल गया? कोई बात नहीं!",
      body: "आपने यह आवेदन वापस ले लिया। आइए आपके लिए सही नौकरी ढूंढें।",
    },
  },
  APPLICATION_STATUS_OTHER: {
    en: {
      title: "Hi {recipient}, application update",
      body: "Status: {status}",
    },
    te: {
      title: "హాయ్ {recipient}, దరఖాస్తు అప్‌డేట్",
      body: "స్థితి: {status}",
    },
    hi: {
      title: "नमस्ते {recipient}, आवेदन अपडेट",
      body: "स्थिति: {status}",
    },
  },
  NEW_APPLICATION_RECEIVED: {
    en: {
      title: "Hi {recipient}, new application received",
      body: "{workerName} applied to your job {jobTitle}.",
    },
    te: {
      title: "హాయ్ {recipient}, కొత్త దరఖాస్తు వచ్చింది",
      body: "{workerName} మీ ఉద్యోగానికి {jobTitle} దరఖాస్తు చేశారు.",
    },
    hi: {
      title: "नमस्ते {recipient}, नया आवेदन मिला",
      body: "{workerName} ने आपकी नौकरी {jobTitle} के लिए आवेदन किया है।",
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
    hi: {
      title: "रेफरल सफल",
      body: "{name} आपके कोड से जुड़े। आपने ₹{amount} कमाए।",
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
    hi: {
      title: "🎉 रेफरल सफल!",
      body: "{name} आपके कोड से जुड़े। आपने ₹{amount} कमाए (₹{bonus} मीलका बोनस सहित!)",
    },
  },
  SIGNUP_BONUS: {
    en: {
      title: "Welcome Bonus",
      body: "You earned ₹{amount} for joining DutyPe.",
    },
    te: {
      title: "స్వాగత బోనస్",
      body: "DutyPeలో చేరినందుకు మీరు ₹{amount} సంపాదించారు.",
    },
    hi: {
      title: "स्वागत बोनस",
      body: "DutyPe से जुड़ने पर आपने ₹{amount} कमाए।",
    },
  },
  EMPLOYER_WELCOME_BENEFIT: {
    en: {
      title: "🚀 Welcome Offer Unlocked",
      body: "Unlimited Job Posting is unlocked! Find the best talent for your business and post your first job now.",
    },
    te: {
      title: "🚀 స్వాగత ఆఫర్ అన్‌లాక్ అయింది",
      body: "అపరిమిత ఉద్యోగ పోస్టింగ్ అన్‌లాక్ అయింది! మీ వ్యాపారం కోసం ఉత్తమ నైపుణ్యాన్ని కనుగొనండి మరియు మీ మొదటి ఉద్యోగాన్ని ఇప్పుడే పోస్ట్ చేయండి.",
    },
    hi: {
      title: "🚀 स्वागत ऑफ़र अनलॉक",
      body: "असीमित जॉब पोस्टिंग अनलॉक हुई! अपने व्यवसाय के लिए सबसे अच्छे लोग खोजें और अभी पहली नौकरी पोस्ट करें।",
    },
  },
  EMPLOYER_WELCOME_BONUS: {
    en: {
      title: "🚀 Welcome Offer Unlocked",
      body: "Unlimited Job Posting is unlocked! Find the best talent for your business and post your first job now.",
    },
    te: {
      title: "🚀 స్వాగత ఆఫర్ అన్‌లాక్ అయింది",
      body: "అపరిమిత ఉద్యోగ పోస్టింగ్ అన్‌లాక్ అయింది! మీ వ్యాపారం కోసం ఉత్తమ నైపుణ్యాన్ని కనుగొనండి మరియు మీ మొదటి ఉద్యోగాన్ని ఇప్పుడే పోస్ట్ చేయండి.",
    },
    hi: {
      title: "🚀 स्वागत ऑफ़र अनलॉक",
      body: "असीमित जॉब पोस्टिंग अनलॉक हुई! अपने व्यवसाय के लिए सबसे अच्छे लोग खोजें और अभी पहली नौकरी पोस्ट करें।",
    },
  },

  // ── transactional reminders & re-engagement ────────────────────────
  JOB_EXPIRY_SOON: {
    en: {
      title: "⏰ Hi {recipient}, job expiring soon",
      body: "Your job \"{jobTitle}\" expires in {hoursLeft} hours. Renew it to keep receiving applications.",
    },
    te: {
      title: "⏰ హాయ్ {recipient}, ఉద్యోగం త్వరలో గడువు ముగుస్తుంది",
      body: "మీ \"{jobTitle}\" ఉద్యోగం {hoursLeft} గంటల్లో గడువు ముగుస్తుంది. దరఖాస్తులు రావడానికి దానిని పునరుద్ధరించండి.",
    },
    hi: {
      title: "⏰ नमस्ते {recipient}, नौकरी जल्द समाप्त हो रही है",
      body: "आपकी नौकरी \"{jobTitle}\" {hoursLeft} घंटे में समाप्त हो जाएगी। आवेदन पाते रहने के लिए इसे नवीनीकृत करें।",
    },
  },
  EMPLOYER_PENDING_APPLICATIONS: {
    en: {
      title: "📋 Hi {recipient}, you have pending applications",
      body: "{count} applications are waiting for your review. Don't miss out on great candidates!",
    },
    te: {
      title: "📋 హాయ్ {recipient}, వేచి ఉన్న దరఖాస్తులు ఉన్నాయి",
      body: "మీ సమీక్ష కోసం {count} దరఖాస్తులు పెండింగ్‌లో ఉన్నాయి. మంచి అభ్యర్థులను మిస్ అవ్వకండి!",
    },
    hi: {
      title: "📋 नमस्ते {recipient}, आपके पास लंबित आवेदन हैं",
      body: "{count} आवेदन आपकी समीक्षा का इंतज़ार कर रहे हैं। अच्छे उम्मीदवार न छूटें!",
    },
  },
  WORKER_PENDING_APPLICATION: {
    en: {
      title: "⏰ Hi {recipient}, your application is still pending",
      body: "Your application for \"{jobTitle}\" has been pending for {daysPending} days. For faster updates, call the employer directly!",
    },
    te: {
      title: "⏰ హాయ్ {recipient}, మీ దరఖాస్తు ఇంకా పెండింగ్‌లో ఉంది",
      body: "\"{jobTitle}\" కోసం మీ దరఖాస్తు {daysPending} రోజులుగా పెండింగ్‌లో ఉంది. వేగంగా అప్‌డేట్‌ల కోసం, యజమానిని నేరుగా కాల్ చేయండి!",
    },
    hi: {
      title: "⏰ नमस्ते {recipient}, आपका आवेदन अभी भी लंबित है",
      body: "\"{jobTitle}\" के लिए आपका आवेदन {daysPending} दिनों से लंबित है। तेज़ अपडेट के लिए नियोक्ता को सीधे कॉल करें!",
    },
  },
  WORKER_RE_ENGAGEMENT: {
    en: {
      title: "💼 Hi {recipient}, new jobs are waiting for you!",
      body: "Check out the latest job opportunities near you. Your next opportunity is just a tap away!",
    },
    te: {
      title: "💼 హాయ్ {recipient}, కొత్త ఉద్యోగాలు మీ కోసం వేచి ఉన్నాయి!",
      body: "మీ సమీపంలోని తాజా ఉద్యోగ అవకాశాలను చూడండి. మీ తదుపరి అవకాశం ఒక్క ట్యాప్ దూరంలో ఉంది!",
    },
    hi: {
      title: "💼 नमस्ते {recipient}, नई नौकरियां आपका इंतज़ार कर रही हैं!",
      body: "अपने आसपास की नई नौकरी के अवसर देखें। आपका अगला मौका बस एक टैप दूर है!",
    },
  },
  EMPLOYER_RE_ENGAGEMENT: {
    en: {
      title: "🏢 Hi {recipient}, ready to hire?",
      body: "Post a job and connect with thousands of qualified workers in your area. Hiring made easy!",
    },
    te: {
      title: "🏢 హాయ్ {recipient}, నియమించడానికి సిద్ధంగా ఉన్నారా?",
      body: "ఉద్యోగాన్ని పోస్ట్ చేయండి మరియు మీ ప్రాంతంలోని వేలాది నైపుణ్యం గల కార్మికులతో కనెక్ట్ అవ్వండి. సులభంగా నియామకం!",
    },
    hi: {
      title: "🏢 नमस्ते {recipient}, हायर करने के लिए तैयार हैं?",
      body: "नौकरी पोस्ट करें और अपने क्षेत्र के हजारों कुशल कामगारों से जुड़ें। हायरिंग अब आसान!",
    },
  },

  // ── scheduled smart engagement: workers ───────────────────────────
  SE_WORKER_FRESH_JOBS: {
    en: { title: "🎯 Fresh jobs matching your skills", body: "New openings nearby — apply early for the best chance." },
    te: { title: "🎯 మీ నైపుణ్యాలకు సరిపడే కొత్త ఉద్యోగాలు", body: "సమీపంలో కొత్త ఖాళీలు — ఉత్తమ అవకాశం కోసం ముందు దరఖాస్తు చేయండి." },
    hi: { title: "🎯 आपके कौशल से मेल खाती नई नौकरियां", body: "आसपास नई रिक्तियां — बेहतर मौके के लिए जल्दी आवेदन करें।" },
  },
  SE_WORKER_HIRING_TODAY: {
    en: { title: "💼 Employers are actively hiring today", body: "Don't miss out — check the latest openings now." },
    te: { title: "💼 యజమానులు ఈరోజు చురుకుగా నియమిస్తున్నారు", body: "మిస్ అవ్వకండి — తాజా ఖాళీలను ఇప్పుడే చూడండి." },
    hi: { title: "💼 नियोक्ता आज सक्रिय रूप से हायर कर रहे हैं", body: "चूकें नहीं — नई रिक्तियां अभी देखें।" },
  },
  SE_WORKER_NEARBY_5KM: {
    en: { title: "📍 Jobs within 5 km of you", body: "Walk-in interviews available near your location." },
    te: { title: "📍 మీకు 5 కి.మీ. లోపు ఉద్యోగాలు", body: "మీ ప్రాంతంలో వాక్-ఇన్ ఇంటర్వ్యూలు అందుబాటులో ఉన్నాయి." },
    hi: { title: "📍 आपसे 5 किमी के भीतर नौकरियां", body: "आपके आसपास वॉक-इन इंटरव्यू उपलब्ध हैं।" },
  },
  SE_WORKER_URGENT_HIRE: {
    en: { title: "🔥 Urgent hire — apply before it fills up", body: "Some jobs posted today are closing fast." },
    te: { title: "🔥 అత్యవసర నియామకం — త్వరగా దరఖాస్తు చేయండి", body: "ఈరోజు పోస్ట్ చేసిన కొన్ని ఉద్యోగాలు త్వరగా మూసివేయబడుతున్నాయి." },
    hi: { title: "🔥 तुरंत हायरिंग — भरने से पहले आवेदन करें", body: "आज पोस्ट की गई कुछ नौकरियां जल्दी बंद हो रही हैं।" },
  },
  SE_WORKER_NEW_COMPANIES: {
    en: { title: "🌟 New companies just joined DutyPe", body: "Check their open positions before others do." },
    te: { title: "🌟 కొత్త కంపెనీలు DutyPeలో చేరాయి", body: "ఇతరుల కంటే ముందే వారి ఖాళీలను చూడండి." },
    hi: { title: "🌟 नई कंपनियां अभी DutyPe से जुड़ी हैं", body: "दूसरों से पहले उनकी रिक्तियां देखें।" },
  },
  SE_WORKER_QUICK_ACTION: {
    en: { title: "⚡ One quick action can change your day", body: "Update your skills or apply to a job to stay visible." },
    te: { title: "⚡ ఒక చిన్న చర్య మీ రోజును మార్చగలదు", body: "మీ నైపుణ్యాలను అప్‌డేట్ చేయండి లేదా ఉద్యోగానికి దరఖాస్తు చేయండి." },
    hi: { title: "⚡ एक छोटा कदम आपका दिन बदल सकता है", body: "अपने कौशल अपडेट करें या किसी नौकरी के लिए आवेदन करें।" },
  },
  SE_WORKER_SMALL_STEPS: {
    en: { title: "📈 Small steps, big opportunities", body: "Keep your profile active — employers notice consistency." },
    te: { title: "📈 చిన్న అడుగులు, పెద్ద అవకాశాలు", body: "మీ ప్రొఫైల్‌ను చురుకుగా ఉంచండి — యజమానులు స్థిరత్వాన్ని గమనిస్తారు." },
    hi: { title: "📈 छोटे कदम, बड़े अवसर", body: "अपनी प्रोफ़ाइल सक्रिय रखें — नियोक्ता नियमितता नोट करते हैं।" },
  },
  SE_WORKER_STAND_OUT: {
    en: { title: "🏆 Stand out from other applicants", body: "Complete your profile to rank higher in search results." },
    te: { title: "🏆 ఇతర దరఖాస్తుదారుల నుండి ప్రత్యేకంగా నిలవండి", body: "శోధన ఫలితాల్లో పైకి రావడానికి మీ ప్రొఫైల్‌ను పూర్తి చేయండి." },
    hi: { title: "🏆 दूसरे आवेदकों से आगे रहें", body: "खोज परिणामों में ऊपर आने के लिए अपनी प्रोफ़ाइल पूरी करें।" },
  },
  SE_WORKER_NEXT_JOB: {
    en: { title: "💪 Your next job could be one tap away", body: "Open DutyPe and see what's new for you." },
    te: { title: "💪 మీ తదుపరి ఉద్యోగం ఒక్క ట్యాప్ దూరంలో ఉండవచ్చు", body: "DutyPe తెరిచి మీ కోసం ఏమి కొత్తదో చూడండి." },
    hi: { title: "💪 आपकी अगली नौकरी एक टैप दूर हो सकती है", body: "DutyPe खोलें और देखें आपके लिए क्या नया है।" },
  },
  SE_WORKER_GOOD_MORNING: {
    en: { title: "🎉 Good morning! Ready to find work?", body: "Fresh daily and hourly jobs waiting for you." },
    te: { title: "🎉 శుభోదయం! పని వెతకడానికి సిద్ధంగా ఉన్నారా?", body: "మీ కోసం రోజువారీ మరియు గంట ఉద్యోగాలు ఎదురుచూస్తున్నాయి." },
    hi: { title: "🎉 सुप्रभात! काम खोजने के लिए तैयार?", body: "आपके लिए रोज़ाना और घंटे के काम इंतज़ार कर रहे हैं।" },
  },
  SE_WORKER_EVENING_CHECK: {
    en: { title: "🌅 Evening check — any interviews coming up?", body: "Review your applications and prepare for tomorrow." },
    te: { title: "🌅 సాయంత్రం చెక్ — ఏవైనా ఇంటర్వ్యూలు ఉన్నాయా?", body: "మీ దరఖాస్తులను సమీక్షించి రేపటికి సిద్ధమవ్వండి." },
    hi: { title: "🌅 शाम की जांच — कोई इंटरव्यू आ रहा है?", body: "अपने आवेदन देखें और कल की तैयारी करें।" },
  },
  SE_WORKER_UNREAD_UPDATES: {
    en: { title: "📱 You have unread updates", body: "An employer may have responded to your application." },
    te: { title: "📱 మీకు చదవని అప్‌డేట్‌లు ఉన్నాయి", body: "ఒక యజమాని మీ దరఖాస్తుకు స్పందించి ఉండవచ్చు." },
    hi: { title: "📱 आपके पास अनपढ़े अपडेट हैं", body: "हो सकता है किसी नियोक्ता ने आपके आवेदन का जवाब दिया हो।" },
  },

  // ── scheduled smart engagement: employers ─────────────────────────
  SE_EMPLOYER_REVIEW_CANDIDATES: {
    en: { title: "👀 Candidates waiting for your review", body: "Review applications now — don't lose top talent." },
    te: { title: "👀 అభ్యర్థులు మీ సమీక్ష కోసం ఎదురుచూస్తున్నారు", body: "ఇప్పుడు దరఖాస్తులను సమీక్షించండి — టాప్ టాలెంట్‌ను కోల్పోకండి." },
    hi: { title: "👀 उम्मीदवार आपकी समीक्षा का इंतज़ार कर रहे हैं", body: "अभी आवेदन देखें — अच्छे लोग न खोएं।" },
  },
  SE_EMPLOYER_NEW_APPLICATIONS: {
    en: { title: "📬 New applications on your job post", body: "Workers have applied — check their profiles today." },
    te: { title: "📬 మీ ఉద్యోగ పోస్ట్‌పై కొత్త దరఖాస్తులు", body: "కార్మికులు దరఖాస్తు చేశారు — వారి ప్రొఫైల్‌లను ఈరోజు చూడండి." },
    hi: { title: "📬 आपकी जॉब पोस्ट पर नए आवेदन", body: "कामगारों ने आवेदन किया है — आज उनकी प्रोफ़ाइल देखें।" },
  },
  SE_EMPLOYER_DONT_WAIT: {
    en: { title: "⏰ Don't keep applicants waiting", body: "Quick responses improve your hiring success rate." },
    te: { title: "⏰ దరఖాస్తుదారులను వేచి ఉంచకండి", body: "త్వరిత స్పందనలు మీ నియామక విజయ రేటును మెరుగుపరుస్తాయి." },
    hi: { title: "⏰ आवेदकों को इंतज़ार न करवाएं", body: "तेज़ जवाब आपकी हायरिंग सफलता बढ़ाते हैं।" },
  },
  SE_EMPLOYER_REFRESH_POST: {
    en: { title: "🚀 Refresh your job post for more visibility", body: "Updated posts get 3x more applications." },
    te: { title: "🚀 ఎక్కువ దృశ్యమానత కోసం మీ ఉద్యోగ పోస్ట్‌ను రిఫ్రెష్ చేయండి", body: "అప్‌డేట్ చేసిన పోస్ట్‌లకు 3x ఎక్కువ దరఖాస్తులు వస్తాయి." },
    hi: { title: "🚀 अधिक दिखने के लिए अपनी जॉब पोस्ट रिफ्रेश करें", body: "अपडेट की गई पोस्ट को 3 गुना अधिक आवेदन मिलते हैं।" },
  },
  SE_EMPLOYER_PERFORMANCE: {
    en: { title: "📊 Your job post performance", body: "See how many workers viewed and applied today." },
    te: { title: "📊 మీ ఉద్యోగ పోస్ట్ పనితీరు", body: "ఈరోజు ఎంతమంది కార్మికులు చూసారో, దరఖాస్తు చేశారో చూడండి." },
    hi: { title: "📊 आपकी जॉब पोस्ट का प्रदर्शन", body: "देखें आज कितने कामगारों ने देखा और आवेदन किया।" },
  },
  SE_EMPLOYER_TIP_SALARY: {
    en: { title: "💡 Tip: Add salary range to attract more workers", body: "Posts with clear pay get 50% more applications." },
    te: { title: "💡 చిట్కా: ఎక్కువ మంది కార్మికులను ఆకర్షించడానికి జీతం పరిధిని జోడించండి", body: "స్పష్టమైన చెల్లింపు ఉన్న పోస్ట్‌లకు 50% ఎక్కువ దరఖాస్తులు వస్తాయి." },
    hi: { title: "💡 सुझाव: अधिक कामगारों को आकर्षित करने के लिए वेतन सीमा जोड़ें", body: "स्पष्ट वेतन वाली पोस्ट को 50% अधिक आवेदन मिलते हैं।" },
  },
  SE_EMPLOYER_HIRE_FASTER: {
    en: { title: "🏆 Hire faster with DutyPe", body: "Take one hiring action today to keep momentum." },
    te: { title: "🏆 DutyPe తో వేగంగా నియమించండి", body: "మొమెంటంను కొనసాగించడానికి ఈరోజు ఒక నియామక చర్య తీసుకోండి." },
    hi: { title: "🏆 DutyPe के साथ तेज़ी से हायर करें", body: "गति बनाए रखने के लिए आज एक हायरिंग कदम उठाएं।" },
  },
  SE_EMPLOYER_FOLLOWUP: {
    en: { title: "📞 Have you contacted your shortlisted candidates?", body: "Quick follow-up prevents candidate drop-off." },
    te: { title: "📞 మీ షార్ట్‌లిస్ట్ చేసిన అభ్యర్థులను సంప్రదించారా?", body: "త్వరిత ఫాలో-అప్ అభ్యర్థి డ్రాప్-ఆఫ్‌ను నివారిస్తుంది." },
    hi: { title: "📞 क्या आपने शॉर्टलिस्ट उम्मीदवारों से संपर्क किया?", body: "तुरंत फ़ॉलो-अप से उम्मीदवार छोड़कर नहीं जाते।" },
  },
  SE_EMPLOYER_RESPOND_24H: {
    en: { title: "✅ Great employers respond within 24 hours", body: "Stay on top of your applications to build trust." },
    te: { title: "✅ గొప్ప యజమానులు 24 గంటలలోపు స్పందిస్తారు", body: "విశ్వాసం పెంచడానికి మీ దరఖాస్తులపై దృష్టి పెట్టండి." },
    hi: { title: "✅ अच्छे नियोक्ता 24 घंटों में जवाब देते हैं", body: "भरोसा बनाने के लिए अपने आवेदनों पर नज़र रखें।" },
  },

  // ── guest engagement (broadcast topic, no recipient lookup) ──────
  SE_GUEST_MORNING_NEW_JOBS: {
    en: { title: "💼 New jobs near you are waiting!", body: "Login to apply in one tap — don't miss out." },
    te: { title: "💼 మీ సమీపంలో కొత్త ఉద్యోగాలు ఎదురుచూస్తున్నాయి!", body: "ఒక్క ట్యాప్‌తో దరఖాస్తు చేయడానికి లాగిన్ అవ్వండి — మిస్ కాకండి." },
    hi: { title: "💼 आपके पास नई नौकरियां इंतज़ार कर रही हैं!", body: "एक टैप में आवेदन करने के लिए लॉगिन करें — चूकें नहीं।" },
  },
  SE_GUEST_MORNING_GOOD_MORNING: {
    en: { title: "🌅 Good morning! Fresh jobs just posted", body: "Sign in to see openings near your location." },
    te: { title: "🌅 శుభోదయం! తాజా ఉద్యోగాలు పోస్ట్ అయ్యాయి", body: "మీ ప్రాంతంలోని ఖాళీలను చూడటానికి సైన్ ఇన్ అవ్వండి." },
    hi: { title: "🌅 सुप्रभात! नई नौकरियां अभी पोस्ट हुईं", body: "अपने आसपास की रिक्तियां देखने के लिए साइन इन करें।" },
  },
  SE_GUEST_MORNING_SKILLS: {
    en: { title: "🎯 Your skills are in demand today", body: "Create your profile and get matched instantly." },
    te: { title: "🎯 ఈరోజు మీ నైపుణ్యాలకు డిమాండ్ ఉంది", body: "మీ ప్రొఫైల్ సృష్టించండి మరియు తక్షణమే మ్యాచ్ అవ్వండి." },
    hi: { title: "🎯 आज आपके कौशल की मांग है", body: "अपनी प्रोफ़ाइल बनाएं और तुरंत मैच पाएं।" },
  },
  SE_GUEST_AFTERNOON_FILLING: {
    en: { title: "🔥 Jobs filling up fast today", body: "Sign in and apply before they're gone." },
    te: { title: "🔥 ఈరోజు ఉద్యోగాలు త్వరగా నిండిపోతున్నాయి", body: "అవి అయిపోకముందే సైన్ ఇన్ అయి దరఖాస్తు చేయండి." },
    hi: { title: "🔥 आज नौकरियां तेज़ी से भर रही हैं", body: "ख़त्म होने से पहले साइन इन करके आवेदन करें।" },
  },
  SE_GUEST_AFTERNOON_HIRING: {
    en: { title: "⚡ Employers are hiring RIGHT NOW", body: "One-tap apply — login to get started." },
    te: { title: "⚡ యజమానులు ఇప్పుడే నియమిస్తున్నారు", body: "ఒక్క ట్యాప్ దరఖాస్తు — ప్రారంభించడానికి లాగిన్ అవ్వండి." },
    hi: { title: "⚡ नियोक्ता अभी हायर कर रहे हैं", body: "एक टैप में आवेदन — शुरू करने के लिए लॉगिन करें।" },
  },
  SE_GUEST_AFTERNOON_WALKIN: {
    en: { title: "📍 Walk-in interviews near you", body: "Sign in to see which companies are hiring today." },
    te: { title: "📍 మీ సమీపంలో వాక్-ఇన్ ఇంటర్వ్యూలు", body: "ఏ కంపెనీలు ఈరోజు నియమిస్తున్నాయో చూడటానికి సైన్ ఇన్ అవ్వండి." },
    hi: { title: "📍 आपके पास वॉक-इन इंटरव्यू", body: "देखने के लिए साइन इन करें कि आज कौन सी कंपनियां हायर कर रही हैं।" },
  },
  SE_GUEST_EVENING_PROFILE: {
    en: { title: "🔓 Complete your profile, unlock matches", body: "Personalised job recommendations are waiting — sign in now." },
    te: { title: "🔓 మీ ప్రొఫైల్‌ను పూర్తి చేయండి, మ్యాచ్‌లను అన్‌లాక్ చేయండి", body: "వ్యక్తిగతీకరించిన ఉద్యోగ సిఫార్సులు ఎదురుచూస్తున్నాయి — ఇప్పుడే సైన్ ఇన్ అవ్వండి." },
    hi: { title: "🔓 प्रोफ़ाइल पूरी करें, मैच अनलॉक करें", body: "व्यक्तिगत नौकरी सिफारिशें इंतज़ार कर रही हैं — अभी साइन इन करें।" },
  },
  SE_GUEST_EVENING_TOMORROW: {
    en: { title: "🌟 Tomorrow could be your first day at work", body: "Sign in tonight, apply, and get hired tomorrow." },
    te: { title: "🌟 రేపు మీ మొదటి పని రోజు కావచ్చు", body: "ఈ రాత్రి సైన్ ఇన్ అయి దరఖాస్తు చేయండి, రేపు ఉద్యోగం పొందండి." },
    hi: { title: "🌟 कल आपका पहला कामकाजी दिन हो सकता है", body: "आज रात साइन इन करें, आवेदन करें और कल नौकरी पाएं।" },
  },
  SE_GUEST_EVENING_THOUSANDS: {
    en: { title: "💪 Thousands found jobs on DutyPe", body: "Join them — create your profile in under 2 minutes." },
    te: { title: "💪 వేల మంది DutyPeలో ఉద్యోగాలు పొందారు", body: "వారిలో చేరండి — 2 నిమిషాలలో మీ ప్రొఫైల్ సృష్టించండి." },
    hi: { title: "💪 हज़ारों लोगों ने DutyPe पर नौकरी पाई", body: "उनसे जुड़ें — 2 मिनट में अपनी प्रोफ़ाइल बनाएं।" },
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
    hi: {
      title: "🎂 जन्मदिन मुबारक, {name}! 🎉",
      body: "आपको खुशियों और सफलता से भरे जन्मदिन की शुभकामनाएं! यह साल आपके लिए शानदार अवसर लाए। - DutyPe टीम",
    },
  },
  // ── moderation ───────────────────────────────────────────────────
  JOB_UNDER_REVIEW: {
    en: {
      title: "Hi {recipient}, your job is under review",
      body: "Your job \"{title}\" needs manual review due to duplicate signals.",
    },
    te: {
      title: "హాయ్ {recipient}, మీ ఉద్యోగం సమీక్షలో ఉంది",
      body: "డూప్లికేట్ సిగ్నల్‌ల కారణంగా మీ ఉద్యోగం \"{title}\" మాన్యువల్ సమీక్ష అవసరం.",
    },
    hi: {
      title: "नमस्ते {recipient}, आपकी नौकरी समीक्षा में है",
      body: "डुप्लिकेट संकेतों के कारण आपकी नौकरी \"{title}\" की मैन्युअल समीक्षा आवश्यक है।",
    },
  },
  JOB_HIDDEN_REPORTS: {
    en: {
      title: "Hi {recipient}, your job was hidden for review",
      body: "Your job \"{title}\" has been hidden due to community reports.",
    },
    te: {
      title: "హాయ్ {recipient}, మీ ఉద్యోగం సమీక్ష కోసం దాచబడింది",
      body: "కమ్యూనిటీ నివేదికల కారణంగా మీ ఉద్యోగం \"{title}\" దాచబడింది.",
    },
    hi: {
      title: "नमस्ते {recipient}, आपकी नौकरी समीक्षा के लिए छिपाई गई",
      body: "सामुदायिक रिपोर्ट के कारण आपकी नौकरी \"{title}\" छिपा दी गई है।",
    },
  },
  NON_PAYMENT_REPORTED: {    en: {
      title: "Hi {recipient}, a payment complaint was raised",
      body: "A worker reported not being paid for \"{title}\". Please settle it or reply to support.",
    },
    te: {
      title: "హాయ్ {recipient}, చెల్లింపు ఫిర్యాదు వచ్చింది",
      body: "\"{title}\" కోసం చెల్లింపు అందలేదని ఒక కార్మికుడు నివేదించారు. దయచేసి చెల్లించండి లేదా సపోర్ట్‌కు సమాధానం ఇవ్వండి.",
    },
    hi: {
      title: "नमस्ते {recipient}, भुगतान की शिकायत मिली है",
      body: "एक कर्मचारी ने \"{title}\" के लिए भुगतान न मिलने की शिकायत की है। कृपया भुगतान करें या सहायता को जवाब दें।",
    },
  },
  WORK_SUBMITTED_CONFIRM: {
    en: {
      title: "Hi {recipient}, please confirm the work",
      body: "{worker} marked the work as done. Confirm it so they can be rated and paid.",
    },
    te: {
      title: "హాయ్ {recipient}, దయచేసి పనిని నిర్ధారించండి",
      body: "{worker} పని పూర్తయినట్లు గుర్తించారు. రేటింగ్, చెల్లింపు కోసం దయచేసి నిర్ధారించండి.",
    },
    hi: {
      title: "नमस्ते {recipient}, कृपया काम की पुष्टि करें",
      body: "{worker} ने काम पूरा होने का निशान लगाया है। रेटिंग और भुगतान के लिए कृपया पुष्टि करें।",
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
  { id: "SE_WORKER_NEXT_JOB", deepLink: "dutype://jobs" },
  { id: "SE_WORKER_GOOD_MORNING", deepLink: "dutype://jobs", timeOfDay: "morning" },
  { id: "SE_WORKER_EVENING_CHECK", deepLink: "dutype://my-jobs", timeOfDay: "evening" },
  { id: "SE_WORKER_UNREAD_UPDATES", deepLink: "dutype://notifications" },
];

export const SE_EMPLOYER_POOL: Array<{ id: keyof typeof NOTIFICATION_TEMPLATES; deepLink: string; timeOfDay?: "morning" | "afternoon" | "evening" }> = [
  { id: "SE_EMPLOYER_REVIEW_CANDIDATES", deepLink: "dutype://employer/applications" },
  { id: "SE_EMPLOYER_NEW_APPLICATIONS", deepLink: "dutype://employer/applications" },
  { id: "SE_EMPLOYER_DONT_WAIT", deepLink: "dutype://employer/applications" },
  { id: "SE_EMPLOYER_REFRESH_POST", deepLink: "dutype://employer/post-job" },
  { id: "SE_EMPLOYER_PERFORMANCE", deepLink: "dutype://employer/home" },
  { id: "SE_EMPLOYER_TIP_SALARY", deepLink: "dutype://employer/post-job" },
  { id: "SE_EMPLOYER_HIRE_FASTER", deepLink: "dutype://employer/home" },
  { id: "SE_EMPLOYER_FOLLOWUP", deepLink: "dutype://employer/applications" },
  { id: "SE_EMPLOYER_RESPOND_24H", deepLink: "dutype://employer/applications" },
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
  if (raw === "hi") return "hi";
  return DEFAULT_LOCALE;
}

function applyParams(template: string, params?: Record<string, string | number>): string {
  const replaced = template.replace(/\{(\w+)\}/g, (_, key) => {
    if (!params) return "";
    const value = params[key];
    return value !== undefined && value !== null ? String(value) : "";
  });
  // Clean up artefacts left when a placeholder resolves to empty — e.g.
  // "Hi , welcome" → "Hi, welcome", "Hi , " → "", double spaces → single.
  return replaced
    .replace(/\s+,/g, ",")
    .replace(/,\s*,/g, ",")
    .replace(/\s{2,}/g, " ")
    .replace(/^\s*[,;:\-]\s*/g, "")
    .trim();
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
 * Resolves the recipient's preferred locale from `user_tokens/{uid}.language`.
 * Returns "en" when the field is absent, malformed, or the token doc is missing.
 * Caller must pass an initialised admin Firestore instance.
 */
export async function getUserLanguage(
  db: admin.firestore.Firestore,
  userId: string
): Promise<SupportedLocale> {
  if (!userId) return DEFAULT_LOCALE;
  try {
    const snap = await db.collection("user_tokens").doc(userId).get();
    if (!snap.exists) return DEFAULT_LOCALE;
    return normalizeLocale(snap.get("language"));
  } catch {
    return DEFAULT_LOCALE;
  }
}

/**
 * Returns the recipient's display name for use in notification bodies.
 * Prefers the first word of `fullName` (e.g. "Rahul" from "Rahul Kumar")
 * so the copy reads naturally. Falls back to the supplied default when the
 * profile doc is missing or has no name.
 *
 * Use this wherever a notification is addressed directly to a user so the
 * copy feels personal ("Hi Rahul, a new job matches…") instead of generic.
 */
export async function getUserDisplayName(
  db: admin.firestore.Firestore,
  userId: string,
  fallback: string = ""
): Promise<string> {
  if (!userId) return fallback;
  try {
    const [workerSnap, employerSnap] = await Promise.all([
      db.collection("worker_profiles").doc(userId).get(),
      db.collection("employer_profiles").doc(userId).get(),
    ]);
    const fullName = String(
      workerSnap.get("fullName") ?? employerSnap.get("fullName") ?? ""
    ).trim();
    if (!fullName) return fallback;
    // First token only — keeps notification bodies concise and avoids
    // awkward surnames in the greeting.
    const firstName = fullName.split(/\s+/)[0] ?? fullName;
    return firstName.length > 24 ? firstName.slice(0, 24) : firstName;
  } catch {
    return fallback;
  }
}

/**
 * Bulk-resolve languages for many users in a single round-trip.
 * Returns a map { userId -> locale }; missing token docs default to "en".
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
    const refs = chunk.map((id) => db.collection("user_tokens").doc(id));
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
