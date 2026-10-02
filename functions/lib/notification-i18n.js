"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SE_GUEST_POOL = exports.SE_EMPLOYER_POOL = exports.SE_WORKER_POOL = exports.NOTIFICATION_TEMPLATES = exports.DEFAULT_LOCALE = exports.SUPPORTED_LOCALES = void 0;
exports.normalizeLocale = normalizeLocale;
exports.tTitle = tTitle;
exports.tBody = tBody;
exports.getUserLanguage = getUserLanguage;
exports.getUserDisplayName = getUserDisplayName;
exports.getUserLanguagesBulk = getUserLanguagesBulk;
exports.localizedTopic = localizedTopic;
exports.SUPPORTED_LOCALES = ["en", "te", "hi"];
exports.DEFAULT_LOCALE = "en";
exports.NOTIFICATION_TEMPLATES = {
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
    // ── instant help (urgent same-day work) ───────────────────────────
    INSTANT_REQUEST_NEARBY: {
        en: { title: "Urgent job within {km} km: {title}", body: "₹{pay} per person · {area} · {needed} needed. Tap Accept to take it." },
        te: { title: "{km} కి.మీ లోపు అత్యవసర పని: {title}", body: "ఒక్కరికి ₹{pay} · {area} · {needed} మంది కావాలి. తీసుకోవడానికి అంగీకరించు నొక్కండి." },
        hi: { title: "{km} किमी के अंदर तुरंत काम: {title}", body: "प्रति व्यक्ति ₹{pay} · {area} · {needed} लोग चाहिए। लेने के लिए स्वीकार दबाएँ।" },
    },
    URGENT_ACCEPTED: {
        en: { title: "{workerName} accepted your urgent job", body: "{title}: {count} of {needed} workers coming. Call them from the app." },
        te: { title: "{workerName} మీ అత్యవసర పనిని అంగీకరించారు", body: "{title}: {needed} మందిలో {count} మంది వస్తున్నారు. యాప్ నుండి కాల్ చేయండి." },
        hi: { title: "{workerName} ने आपका तुरंत काम स्वीकार किया", body: "{title}: {needed} में से {count} वर्कर आ रहे हैं। ऐप से कॉल करें।" },
    },
    URGENT_CANCELLED: {
        en: { title: "Urgent job cancelled", body: "The employer cancelled {title}. You can accept other jobs now." },
        te: { title: "అత్యవసర పని రద్దయింది", body: "యజమాని {title}ను రద్దు చేశారు. మీరు ఇప్పుడు ఇతర పనులు తీసుకోవచ్చు." },
        hi: { title: "तुरंत काम रद्द हुआ", body: "नियोक्ता ने {title} रद्द कर दिया। अब आप दूसरे काम ले सकते हैं।" },
    },
    SERVICE_OFFER: {
        en: { title: "New job: {service}", body: "{area} · {km} km away · you earn ₹{earning}. Tap to accept." },
        te: { title: "కొత్త పని: {service}", body: "{area} · {km} కి.మీ · మీకు ₹{earning}. అంగీకరించడానికి నొక్కండి." },
        hi: { title: "नया काम: {service}", body: "{area} · {km} किमी · आपकी कमाई ₹{earning}। स्वीकार करने के लिए टैप करें।" },
    },
    SERVICE_ASSIGNED: {
        en: { title: "{partner} is your DutyPe partner", body: "{service}: your partner accepted. Share your start code only when they arrive." },
        te: { title: "{partner} మీ DutyPe పార్ట్‌నర్", body: "{service}: పార్ట్‌నర్ అంగీకరించారు. వారు వచ్చినప్పుడే స్టార్ట్ కోడ్ చెప్పండి." },
        hi: { title: "{partner} आपके DutyPe पार्टनर हैं", body: "{service}: पार्टनर ने स्वीकार किया। उनके आने पर ही स्टार्ट कोड बताएं।" },
    },
    SERVICE_ON_THE_WAY: {
        en: { title: "{partner} is on the way", body: "Your {service} partner is coming. Keep your start code ready." },
        te: { title: "{partner} వస్తున్నారు", body: "మీ {service} పార్ట్‌నర్ వస్తున్నారు. స్టార్ట్ కోడ్ సిద్ధంగా ఉంచండి." },
        hi: { title: "{partner} रास्ते में हैं", body: "आपके {service} पार्टनर आ रहे हैं। स्टार्ट कोड तैयार रखें।" },
    },
    SERVICE_STARTED: {
        en: { title: "Work started", body: "{partner} started your {service}." },
        te: { title: "పని మొదలైంది", body: "{partner} మీ {service} మొదలుపెట్టారు." },
        hi: { title: "काम शुरू हुआ", body: "{partner} ने आपका {service} शुरू किया।" },
    },
    SERVICE_COMPLETED: {
        en: { title: "Job done! Pay ₹{total} to your partner", body: "{service} is complete. Pay {partner} by cash or UPI and rate the work." },
        te: { title: "పని పూర్తయింది! పార్ట్‌నర్‌కు ₹{total} చెల్లించండి", body: "{service} పూర్తయింది. {partner}కు నగదు లేదా UPIతో చెల్లించి రేటింగ్ ఇవ్వండి." },
        hi: { title: "काम पूरा! पार्टनर को ₹{total} दें", body: "{service} पूरा हुआ। {partner} को नकद या UPI से भुगतान करें और रेटिंग दें।" },
    },
    SERVICE_PARTNER_CANCELLED: {
        en: { title: "Finding you another partner", body: "Your partner could not come for {service}. We are finding another one now." },
        te: { title: "మరో పార్ట్‌నర్‌ను వెతుకుతున్నాం", body: "{service}కు మీ పార్ట్‌నర్ రాలేకపోయారు. ఇప్పుడు మరొకరిని వెతుకుతున్నాం." },
        hi: { title: "आपके लिए दूसरा पार्टनर ढूंढ रहे हैं", body: "{service} के लिए आपके पार्टनर नहीं आ सके। हम दूसरा पार्टनर ढूंढ रहे हैं।" },
    },
    SERVICE_NO_PARTNER: {
        en: { title: "No partner available right now", body: "Sorry, no partner could take {service}. Please book again or choose a later time." },
        te: { title: "ఇప్పుడు పార్ట్‌నర్ అందుబాటులో లేరు", body: "క్షమించండి, {service}కు పార్ట్‌నర్ దొరకలేదు. మళ్లీ బుక్ చేయండి లేదా తర్వాతి సమయం ఎంచుకోండి." },
        hi: { title: "अभी कोई पार्टनर उपलब्ध नहीं", body: "माफ़ कीजिए, {service} के लिए पार्टनर नहीं मिला। फिर से बुक करें या बाद का समय चुनें।" },
    },
    SERVICE_CANCELLED_BY_CUSTOMER: {
        en: { title: "Booking cancelled", body: "The customer cancelled {service}. You can take new jobs now." },
        te: { title: "బుకింగ్ రద్దయింది", body: "కస్టమర్ {service}ను రద్దు చేశారు. మీరు ఇప్పుడు కొత్త పనులు తీసుకోవచ్చు." },
        hi: { title: "बुकिंग रद्द", body: "ग्राहक ने {service} रद्द किया। अब आप नए काम ले सकते हैं।" },
    },
    PARTNER_APPROVED: {
        en: { title: "You are a DutyPe partner now!", body: "Add credits and go Online to start getting home service jobs." },
        te: { title: "మీరు ఇప్పుడు DutyPe పార్ట్‌నర్!", body: "క్రెడిట్స్ జోడించి ఆన్‌లైన్‌కి వెళ్లండి, ఇంటి సర్వీస్ పనులు రావడం మొదలవుతుంది." },
        hi: { title: "अब आप DutyPe पार्टनर हैं!", body: "क्रेडिट जोड़ें और ऑनलाइन जाएं, घर की सर्विस के काम मिलने लगेंगे।" },
    },
    PARTNER_REJECTED: {
        en: { title: "Partner application update", body: "Your partner application was not approved. {reason}" },
        te: { title: "పార్ట్‌నర్ దరఖాస్తు అప్‌డేట్", body: "మీ పార్ట్‌నర్ దరఖాస్తు ఆమోదం పొందలేదు. {reason}" },
        hi: { title: "पार्टनर आवेदन अपडेट", body: "आपका पार्टनर आवेदन स्वीकृत नहीं हुआ। {reason}" },
    },
    PARTNER_TOPUP_VERIFIED: {
        en: { title: "₹{amount} credits added", body: "Your UPI payment is verified. Go Online to get jobs." },
        te: { title: "₹{amount} క్రెడిట్స్ జోడించబడ్డాయి", body: "మీ UPI చెల్లింపు ధృవీకరించబడింది. పనుల కోసం ఆన్‌లైన్‌కి వెళ్లండి." },
        hi: { title: "₹{amount} क्रेडिट जोड़े गए", body: "आपका UPI भुगतान सत्यापित हुआ। काम पाने के लिए ऑनलाइन जाएं।" },
    },
    PARTNER_TOPUP_REJECTED: {
        en: { title: "Top-up not verified", body: "We could not verify your ₹{amount} payment. {reason}" },
        te: { title: "టాప్-అప్ ధృవీకరించబడలేదు", body: "మీ ₹{amount} చెల్లింపును ధృవీకరించలేకపోయాం. {reason}" },
        hi: { title: "टॉप-अप सत्यापित नहीं", body: "हम आपके ₹{amount} भुगतान को सत्यापित नहीं कर सके। {reason}" },
    },
    URGENT_REMOVED: {
        en: { title: "You were removed from an urgent job", body: "The employer removed you from {title}. You can accept other jobs now." },
        te: { title: "అత్యవసర పని నుండి మిమ్మల్ని తొలగించారు", body: "యజమాని మిమ్మల్ని {title} నుండి తొలగించారు. మీరు ఇతర పనులు తీసుకోవచ్చు." },
        hi: { title: "आपको तुरंत काम से हटाया गया", body: "नियोक्ता ने आपको {title} से हटा दिया। आप दूसरे काम ले सकते हैं।" },
    },
    INSTANT_RESPONSE_RECEIVED: {
        en: { title: "{workerName} can help", body: "A worker responded to your urgent need {title}." },
        te: { title: "{workerName} సహాయం చేయగలరు", body: "మీ అత్యవసర అవసరం {title}కి ఒక వర్కర్ స్పందించారు." },
        hi: { title: "{workerName} मदद कर सकते हैं", body: "आपकी तुरंत ज़रूरत {title} पर एक वर्कर ने जवाब दिया।" },
    },
    INSTANT_SELECTED: {
        en: { title: "Hi {recipient}, you're selected! 🎉", body: "The employer selected you for {title}. Please reach on time." },
        te: { title: "హాయ్ {recipient}, మీరు ఎంపికయ్యారు! 🎉", body: "{title} కోసం యజమాని మిమ్మల్ని ఎంచుకున్నారు. సమయానికి చేరుకోండి." },
        hi: { title: "नमस्ते {recipient}, आपको चुना गया! 🎉", body: "{title} के लिए नियोक्ता ने आपको चुना है। समय पर पहुँचें।" },
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
    WITHDRAWAL_COMPLETED: {
        en: {
            title: "Withdrawal sent",
            body: "₹{amount} has been sent to your UPI account.",
        },
        te: {
            title: "డబ్బు పంపబడింది",
            body: "₹{amount} మీ UPI ఖాతాకు పంపబడింది.",
        },
        hi: {
            title: "निकासी भेज दी गई",
            body: "₹{amount} आपके UPI खाते में भेज दिए गए हैं।",
        },
    },
    SUBSCRIPTION_ACTIVATED: {
        en: {
            title: "Plan activated",
            body: "Your payment is verified. Job post credits have been added.",
        },
        te: {
            title: "ప్లాన్ యాక్టివేట్ అయింది",
            body: "మీ చెల్లింపు ధృవీకరించబడింది. జాబ్ పోస్ట్ క్రెడిట్‌లు జోడించబడ్డాయి.",
        },
        hi: {
            title: "प्लान सक्रिय हुआ",
            body: "आपका भुगतान सत्यापित हो गया। जॉब पोस्ट क्रेडिट जोड़ दिए गए हैं।",
        },
    },
    SUBSCRIPTION_REJECTED: {
        en: {
            title: "Payment not verified",
            body: "We could not verify your payment. {reason}",
        },
        te: {
            title: "చెల్లింపు ధృవీకరించబడలేదు",
            body: "మీ చెల్లింపును ధృవీకరించలేకపోయాము. {reason}",
        },
        hi: {
            title: "भुगतान सत्यापित नहीं हुआ",
            body: "हम आपका भुगतान सत्यापित नहीं कर सके। {reason}",
        },
    },
    WITHDRAWAL_FAILED: {
        en: {
            title: "Withdrawal failed",
            body: "We could not send ₹{amount}. The money is back in your wallet.",
        },
        te: {
            title: "విత్‌డ్రా విఫలమైంది",
            body: "₹{amount} పంపలేకపోయాము. డబ్బు మీ వాలెట్‌కు తిరిగి వచ్చింది.",
        },
        hi: {
            title: "निकासी विफल",
            body: "₹{amount} नहीं भेजे जा सके। पैसे आपके वॉलेट में वापस आ गए हैं।",
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
    WORK_AUTO_COMPLETED: {
        en: {
            title: "Hi {recipient}, \"{title}\" is marked complete",
            body: "This job was closed automatically. If something went wrong, report it from the job.",
        },
        te: {
            title: "హాయ్ {recipient}, \"{title}\" పూర్తయినట్లు గుర్తించబడింది",
            body: "ఈ ఉద్యోగం ఆటోమేటిక్‌గా ముగించబడింది. ఏదైనా సమస్య ఉంటే ఉద్యోగం నుండి నివేదించండి.",
        },
        hi: {
            title: "नमस्ते {recipient}, \"{title}\" पूरा चिह्नित हो गया",
            body: "यह नौकरी अपने आप बंद कर दी गई। कुछ गड़बड़ हुई हो तो नौकरी से शिकायत करें।",
        },
    },
};
/** Maps a worker smart-engagement scheduled-template index → template id. */
exports.SE_WORKER_POOL = [
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
exports.SE_EMPLOYER_POOL = [
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
exports.SE_GUEST_POOL = [
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
function normalizeLocale(value) {
    const raw = String(value !== null && value !== void 0 ? value : "").trim().toLowerCase();
    if (raw === "te")
        return "te";
    if (raw === "hi")
        return "hi";
    return exports.DEFAULT_LOCALE;
}
function applyParams(template, params) {
    const replaced = template.replace(/\{(\w+)\}/g, (_, key) => {
        if (!params)
            return "";
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
function pickLocalized(templateId, locale) {
    var _a, _b;
    const entry = exports.NOTIFICATION_TEMPLATES[templateId];
    if (!entry)
        return null;
    return (_b = (_a = entry[locale]) !== null && _a !== void 0 ? _a : entry[exports.DEFAULT_LOCALE]) !== null && _b !== void 0 ? _b : null;
}
function tTitle(templateId, locale, params) {
    const localized = pickLocalized(templateId, locale);
    if (!localized)
        return templateId;
    return applyParams(localized.title, params);
}
function tBody(templateId, locale, params) {
    const localized = pickLocalized(templateId, locale);
    if (!localized)
        return templateId;
    return applyParams(localized.body, params);
}
/**
 * Resolves the recipient's preferred locale from `user_tokens/{uid}.language`.
 * Returns "en" when the field is absent, malformed, or the token doc is missing.
 * Caller must pass an initialised admin Firestore instance.
 */
async function getUserLanguage(db, userId) {
    if (!userId)
        return exports.DEFAULT_LOCALE;
    try {
        const snap = await db.collection("user_tokens").doc(userId).get();
        if (!snap.exists)
            return exports.DEFAULT_LOCALE;
        return normalizeLocale(snap.get("language"));
    }
    catch (_a) {
        return exports.DEFAULT_LOCALE;
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
async function getUserDisplayName(db, userId, fallback = "") {
    var _a, _b, _c;
    if (!userId)
        return fallback;
    try {
        const [workerSnap, employerSnap] = await Promise.all([
            db.collection("worker_profiles").doc(userId).get(),
            db.collection("employer_profiles").doc(userId).get(),
        ]);
        const fullName = String((_b = (_a = workerSnap.get("name")) !== null && _a !== void 0 ? _a : employerSnap.get("ownerName")) !== null && _b !== void 0 ? _b : "").trim();
        if (!fullName)
            return fallback;
        // First token only — keeps notification bodies concise and avoids
        // awkward surnames in the greeting.
        const firstName = (_c = fullName.split(/\s+/)[0]) !== null && _c !== void 0 ? _c : fullName;
        return firstName.length > 24 ? firstName.slice(0, 24) : firstName;
    }
    catch (_d) {
        return fallback;
    }
}
/**
 * Bulk-resolve languages for many users in a single round-trip.
 * Returns a map { userId -> locale }; missing token docs default to "en".
 */
async function getUserLanguagesBulk(db, userIds) {
    const out = new Map();
    if (userIds.length === 0)
        return out;
    const unique = Array.from(new Set(userIds.filter(Boolean)));
    for (let i = 0; i < unique.length; i += 30) {
        const chunk = unique.slice(i, i + 30);
        const refs = chunk.map((id) => db.collection("user_tokens").doc(id));
        const docs = await db.getAll(...refs);
        docs.forEach((doc) => {
            out.set(doc.id, doc.exists ? normalizeLocale(doc.get("language")) : exports.DEFAULT_LOCALE);
        });
    }
    unique.forEach((id) => {
        if (!out.has(id))
            out.set(id, exports.DEFAULT_LOCALE);
    });
    return out;
}
/**
 * Topic naming for language-specific FCM broadcasts. Android subscribes to
 * `${baseTopic}_${language}` so admin broadcasts can deliver the right copy.
 */
function localizedTopic(baseTopic, locale) {
    return `${baseTopic}_${locale}`;
}
//# sourceMappingURL=notification-i18n.js.map