package com.example.dutype.utils

import com.example.dutype.employer.models.JobCategory

object JobCategoryResolver {
    private val aliases = mapOf(
        "ALL" to null,
        "ALL_JOBS" to null,
        "SHOP_HELPER" to JobCategory.HELPER,
        "HOUSEKEEPING" to JobCategory.MAID,
        "KITCHEN" to JobCategory.COOK,
        "EVENTS" to JobCategory.WAITER,
        "CONSTRUCTION" to JobCategory.HELPER,
        "FIELD_WORK" to JobCategory.FIELD_EXECUTIVE,
        "ADMIN" to JobCategory.OFFICE_STAFF,
        "OFFICE_ADMIN" to JobCategory.OFFICE_STAFF,
        "OFFICE_BOY" to JobCategory.HELPER,
        "CALL_CENTER" to JobCategory.CUSTOMER_SUPPORT,
        "CUSTOMER_SERVICE" to JobCategory.CUSTOMER_SUPPORT,
        "NURSE" to JobCategory.HEALTHCARE,
        "MEDICAL" to JobCategory.HEALTHCARE,
        "INSURANCE" to JobCategory.FINANCE,
        "BANKING" to JobCategory.FINANCE
    )

    /**
     * Trade words in Telugu and Hindi (script and common Roman spellings), so a search or a job
     * title in the worker's own language lands on the same category as the English word.
     */
    private val localWords: Map<JobCategory, List<String>> = mapOf(
        JobCategory.DELIVERY to listOf("డెలివరీ", "కొరియర్", "डिलीवरी", "कूरियर"),
        JobCategory.DRIVER to listOf("డ్రైవర్", "డ్రైవింగ్", "ड्राइवर", "ड्राईवर", "चालक"),
        JobCategory.COOK to listOf("వంట", "కుక్", "రసోయి", "खाना बनाने", "रसोइया", "कुक", "vanta", "rasoiya"),
        JobCategory.MAID to listOf("పనిమనిషి", "ఇంటి పని", "క్లీనింగ్", "నౌకరాణి", "नौकरानी", "घरेलू काम", "सफाई", "panimanishi", "safai"),
        JobCategory.SECURITY to listOf("సెక్యూరిటీ", "వాచ్‌మెన్", "వాచ్మెన్", "వాచ్‌మాన్", "గార్డ్", "सिक्योरिटी", "चौकीदार", "गार्ड", "chowkidar", "watchmen"),
        JobCategory.GARDENER to listOf("తోటమాలి", "తోట పని", "గార్డెనర్", "माली", "बागवानी"),
        JobCategory.CARETAKER to listOf("ఆయా", "సంరక్షణ", "కేర్‌టేకర్", "केयरटेकर", "देखभाल"),
        JobCategory.WAITER to listOf("వెయిటర్", "సర్వర్", "वेटर"),
        JobCategory.ELECTRICIAN to listOf("ఎలక్ట్రీషియన్", "ఎలక్ట్రిషియన్", "కరెంట్ పని", "इलेक्ट्रीशियन", "बिजली मिस्त्री"),
        JobCategory.PLUMBER to listOf("ప్లంబర్", "प्लंबर", "नलसाज"),
        JobCategory.PAINTER to listOf("పెయింటర్", "పెయింటింగ్", "రంగులు", "पेंटर", "रंगाई", "पुताई"),
        JobCategory.CARPENTER to listOf("వడ్రంగి", "కార్పెంటర్", "बढ़ई", "कारपेंटर"),
        JobCategory.RECEPTIONIST to listOf("రిసెప్షనిస్ట్", "రిసెప్షన్", "रिसेप्शनिस्ट", "रिसेप्शन"),
        JobCategory.CASHIER to listOf("క్యాషియర్", "బిల్లింగ్", "कैशियर", "बिलिंग"),
        JobCategory.PACKER to listOf("ప్యాకింగ్", "ప్యాకర్", "లోడింగ్", "హమాలీ", "पैकिंग", "लोडिंग", "हमाल"),
        JobCategory.SALES to listOf("సేల్స్", "అమ్మకాలు", "సేల్స్‌మెన్", "सेल्स", "बिक्री", "सेल्समैन"),
        JobCategory.TELECALLER to listOf("టెలికాలర్", "టెలీకాలర్", "కాలింగ్", "टेलीकॉलर", "टेलीकॉलिंग"),
        JobCategory.TEACHER to listOf("టీచర్", "ఉపాధ్యాయ", "ట్యూషన్", "शिक्षक", "टीचर", "ट्यूशन"),
        JobCategory.OFFICE_STAFF to listOf("ఆఫీస్ బాయ్", "ఆఫీస్", "ఆఫీసు", "ऑफिस", "ऑफ़िस", "चपरासी"),
        JobCategory.CUSTOMER_SUPPORT to listOf("కస్టమర్ సపోర్ట్", "కస్టమర్ సర్వీస్", "कस्टमर सपोर्ट", "कस्टमर सर्विस"),
        JobCategory.FIELD_EXECUTIVE to listOf("ఫీల్డ్", "फील्ड"),
        JobCategory.MARKETING to listOf("మార్కెటింగ్", "मार्केटिंग"),
        JobCategory.FINANCE to listOf("లోన్", "ఫైనాన్స్", "लोन", "फाइनेंस"),
        JobCategory.HEALTHCARE to listOf("నర్స్", "ఆసుపత్రి", "హాస్పిటల్", "ఫార్మసీ", "नर्स", "अस्पताल", "फार्मेसी"),
        JobCategory.BEAUTICIAN to listOf("బ్యూటీషియన్", "బ్యూటీ పార్లర్", "సెలూన్", "ब्यूटीशियन", "ब्यूटी पार्लर", "सैलून"),
        JobCategory.TAILOR to listOf("దర్జీ", "టైలర్", "కుట్టు", "दर्जी", "टेलर", "सिलाई"),
        JobCategory.MECHANIC to listOf("మెకానిక్", "मैकेनिक", "मेकैनिक"),
        JobCategory.DATA_ENTRY to listOf("డేటా ఎంట్రీ", "కంప్యూటర్ ఆపరేటర్", "डेटा एंट्री", "कंप्यूटर ऑपरेटर"),
        JobCategory.LEGAL to listOf("లాయర్", "న్యాయవాది", "वकील"),
        JobCategory.HELPER to listOf("హెల్పర్", "కూలీ", "కూలి", "మేస్త్రీ", "मजदूर", "मज़दूर", "हेल्पर", "coolie", "kooli", "mazdoor", "majdoor")
    )

    private val keywordRules: List<Pair<JobCategory, List<String>>> by lazy {
        englishRules.map { (category, words) -> category to (words + localWords[category].orEmpty()) }
    }

    private val englishRules: List<Pair<JobCategory, List<String>>> = listOf(
        JobCategory.DELIVERY to listOf("delivery", "courier", "swiggy", "zomato", "dunzo", "parcel", "last mile", "rider", "bike rider"),
        JobCategory.DRIVER to listOf("driver", "chauffeur", "cab", "taxi", "ola", "uber", "rapido", "truck"),
        JobCategory.COOK to listOf("cook", "chef", "kitchen", "tandoor", "chapati", "biryani", "catering"),
        JobCategory.MAID to listOf("maid", "housekeep", "domestic", "house cleaner", "cleaner", "sweeper", "janitor", "house help"),
        JobCategory.SECURITY to listOf("security", "guard", "watchman", "bouncer"),
        JobCategory.GARDENER to listOf("gardener", "garden", "landscap", "lawn", "horticult"),
        JobCategory.CARETAKER to listOf("caretaker", "care taker", "nanny", "babysitter", "childcare", "elder care", "caregiver", "ayah"),
        JobCategory.WAITER to listOf("waiter", "waitress", "steward", "bartender", "server ", "banquet", "cafe staff", "restaurant staff", "hotel staff"),
        JobCategory.ELECTRICIAN to listOf("electric", "wiring"),
        JobCategory.PLUMBER to listOf("plumb", "pipe fit"),
        JobCategory.PAINTER to listOf("painter", "painting"),
        JobCategory.CARPENTER to listOf("carpenter", "carpentry", "woodwork"),
        JobCategory.RECEPTIONIST to listOf("receptionist", "front desk", "front office"),
        JobCategory.CASHIER to listOf("cashier", "billing", "checkout", "counter"),
        JobCategory.PACKER to listOf("packer", "packing", "packaging", "warehouse", "loader", "loading", "unloading"),
        JobCategory.SALES to listOf("sales", "retail", "shop sales", "store sales", "sales associate", "sales executive"),
        JobCategory.TELECALLER to listOf("telecaller", "tele caller", "telesales", "tele sales", "calling", "call center", "bpo", "voice process"),
        JobCategory.TEACHER to listOf("teacher", "tutor", "teaching", "trainer", "instructor", "pre primary", "pre-primary", "primary school"),
        JobCategory.OFFICE_STAFF to listOf("office admin", "office staff", "admin", "back office", "office assistant", "clerk", "office boy", "peon"),
        JobCategory.CUSTOMER_SUPPORT to listOf("customer support", "customer service", "support executive", "process associate", "service associate"),
        JobCategory.FIELD_EXECUTIVE to listOf("field executive", "field officer", "field work", "field sales", "field service", "promoter", "brand promoter"),
        JobCategory.MARKETING to listOf("marketing", "business development", "bde", "bd executive", "relationship manager"),
        JobCategory.FINANCE to listOf("loan", "finance", "banking", "collection", "insurance", "credit card", "home loan"),
        JobCategory.HEALTHCARE to listOf("nurse", "nursing", "medical", "healthcare", "patient care", "ward boy", "pharmacy"),
        JobCategory.BEAUTICIAN to listOf("beautician", "beauty", "salon", "makeup", "hair", "spa"),
        JobCategory.TAILOR to listOf("tailor", "stitching", "sewing", "garment", "alteration"),
        JobCategory.MECHANIC to listOf("mechanic", "auto mechanic", "vehicle repair", "garage", "technician"),
        JobCategory.DATA_ENTRY to listOf("data entry", "computer operator", "typing", "excel operator", "mis executive"),
        JobCategory.LEGAL to listOf("advocate", "legal", "lawyer", "law clerk"),
        JobCategory.HELPER to listOf("helper", "assistant", "labour", "labor", "construction", "mason", "site work")
    )

    fun enumNameForDisplay(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null
        val token = raw.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')
        aliases[token]?.let { return it.name }
        if (aliases.containsKey(token)) return null
        JobCategory.entries.firstOrNull { it.name == token }?.let { return it.name }
        JobCategory.entries.firstOrNull { it.displayName.equals(raw, ignoreCase = true) }?.let { return it.name }
        return null
    }

    fun inferCategory(title: String, description: String = ""): JobCategory? {
        val text = "$title $description".lowercase()
        if (text.length < 3) return null
        return keywordRules.firstOrNull { (_, keywords) ->
            keywords.any { text.contains(it) }
        }?.first
    }

    fun inferCategoryName(title: String, description: String = "", explicit: String? = null): String {
        val explicitName = enumNameForDisplay(explicit)
        if (!explicitName.isNullOrBlank() && explicitName != JobCategory.OTHER.name) return explicitName
        return inferCategory(title, description)?.name ?: JobCategory.OTHER.name
    }

    fun matchesCategory(title: String, description: String, categoryName: String): Boolean {
        val expected = enumNameForDisplay(categoryName) ?: categoryName.uppercase()
        return inferCategory(title, description)?.name == expected
    }

    fun displayNameForName(categoryName: String): String {
        return runCatching { JobCategory.valueOf(categoryName.uppercase()).displayName }.getOrDefault(categoryName)
    }

    /**
     * Single job words (category names + keywords, 4+ letters) used by [JobQueryCorrector]
     * to fix typos such as "diver" -> "driver".
     */
    fun searchVocabulary(): Set<String> {
        val words = linkedSetOf<String>()
        fun add(value: String) {
            value.lowercase()
                .split(Regex("[^a-z]+"))
                .filter { it.length >= 4 }
                .forEach { words += it }
        }
        JobCategory.entries.forEach { add(it.displayName) }
        keywordRules.forEach { (_, keywords) -> keywords.forEach(::add) }
        add("mason welder fitter cleaner labour sweeper watchman chef peon nurse")
        return words
    }

    fun searchQueryTokens(query: String): List<String> {
        val tokens = linkedSetOf<String>()

        fun addTokens(value: String) {
            value.lowercase()
                .replace(Regex("[^a-z0-9]+"), " ")
                .split(' ')
                .map { it.trim() }
                .filter { it.length >= 2 }
                .forEach { tokens += it }
        }

        addTokens(query)

        val categoryName = enumNameForDisplay(query) ?: inferCategory(query)?.name
        if (!categoryName.isNullOrBlank()) {
            addTokens(categoryName)
            addTokens(displayNameForName(categoryName))
        }

        return tokens.take(10)
    }
}
