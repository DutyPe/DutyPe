package com.example.dutype.homeservices

/**
 * Baseline default services catalog for DutyPe.
 * Provides instant 0ms offline availability on app cold-start (matching Swiggy / Urban Company),
 * preventing any blank screens or heavy blocking shimmers while fresh data is synced in the background.
 */
object DefaultServicesCatalog {

    fun get(): ServicesCatalog {
        val categories = listOf(
            ServiceCategory(
                id = "CLEANING",
                name = "Home Cleaning",
                te = "ఇంటి క్లీనింగ్",
                hi = "घर की सफ़ाई",
                skill = "BASIC",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Fcleaning.webp?alt=media"
            ),
            ServiceCategory(
                id = "AC",
                name = "AC Service & Repair",
                te = "AC సర్వీస్ & రిపేర్",
                hi = "AC सर्विस और रिपेयर",
                skill = "SKILLED",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Fac.webp?alt=media"
            ),
            ServiceCategory(
                id = "ELECTRICIAN",
                name = "Electrician",
                te = "ఎలక్ట్రీషియన్",
                hi = "इलेक्ट्रीशियन",
                skill = "SKILLED",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Felectrician.webp?alt=media"
            ),
            ServiceCategory(
                id = "PLUMBER",
                name = "Plumber",
                te = "ప్లంబర్",
                hi = "प्लंबर",
                skill = "SKILLED",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Fplumber.webp?alt=media"
            ),
            ServiceCategory(
                id = "APPLIANCE",
                name = "Appliance & RO Repair",
                te = "అప్లయెన్స్ & RO రిపేర్",
                hi = "अप्लायंस और RO रिपेयर",
                skill = "SKILLED",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Fappliance.webp?alt=media"
            ),
            ServiceCategory(
                id = "CARPENTER",
                name = "Carpenter",
                te = "కార్పెంటర్",
                hi = "बढ़ई",
                skill = "SKILLED",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Fcarpenter.webp?alt=media"
            ),
            ServiceCategory(
                id = "PAINTER",
                name = "Painting",
                te = "పెయింటింగ్",
                hi = "पेंटिंग",
                skill = "SKILLED",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Fpainter.webp?alt=media"
            ),
            ServiceCategory(
                id = "HOME_HELP",
                name = "Home Help & Shifting",
                te = "ఇంటి సహాయం & షిఫ్టింగ్",
                hi = "घर की मदद और शिफ़्टिंग",
                skill = "BASIC",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Fhome_help.webp?alt=media"
            ),
            ServiceCategory(
                id = "VEHICLE",
                name = "Car & Bike Wash",
                te = "కార్ & బైక్ వాష్",
                hi = "कार और बाइक वॉश",
                skill = "BASIC",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Fvehicle.webp?alt=media"
            ),
            ServiceCategory(
                id = "BEAUTICIAN",
                name = "Beautician & Salon",
                te = "బ్యూటీషియన్ & సెలూన్",
                hi = "ब्यूटीशियन और सैलून",
                skill = "SKILLED",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Fbeautician.webp?alt=media"
            ),
            ServiceCategory(
                id = "MEHENDI",
                name = "Mehendi & Bridal Art",
                te = "మెహందీ & బ్రైడల్ డిజైన్",
                hi = "मेहंदी और ब्राइडल आर्ट",
                skill = "SKILLED",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Fmehendi.webp?alt=media"
            ),
            ServiceCategory(
                id = "COOK_MAID",
                name = "Cook & Maid",
                te = "వంటమనిషి & మేడ్",
                hi = "रसोइया और कुक",
                skill = "BASIC",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Fcook.webp?alt=media"
            ),
            ServiceCategory(
                id = "BABYSITTER",
                name = "Babysitting & Child Care",
                te = "పిల్లల సంరక్షణ & బేబీ సిట్టింగ్",
                hi = "बच्चों की देखभाल और बेबीसिटिंग",
                skill = "BASIC",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Fbabysitter.webp?alt=media"
            ),
            ServiceCategory(
                id = "ELDERLY_CARE",
                name = "Elderly & Patient Care",
                te = "వృద్ధుల సంరక్షణ & పేషెంట్ కేర్",
                hi = "बुजुर्गों और मरीजों की देखभाल",
                skill = "BASIC",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Felderly_care.webp?alt=media"
            ),
            ServiceCategory(
                id = "TAILORING",
                name = "Ladies Tailoring",
                te = "లేడీస్ టైలరింగ్",
                hi = "लेडीज टेलरिंग",
                skill = "SKILLED",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2Ftailoring.webp?alt=media"
            )
        )

        val services = listOf(
            ServiceItem(
                id = "ac_service",
                category = "AC",
                name = "Split AC Deep Jet Service",
                te = "స్ప్లిట్ AC డీప్ జెట్ సర్వీస్",
                hi = "स्प्लिट AC डीप जेट सर्विस",
                price = 399,
                originalPrice = 599,
                durationMin = 45,
                inspection = false,
                includes = "Power jet indoor cooling coil cleaning, outdoor unit wash, gas check and cooling verification.",
                rating = 4.9,
                ratingCount = "18k+",
                options = listOf(
                    ServiceOption(id = "ac_1", title = "1 Split AC", price = 399, originalPrice = 599, durationMin = 45),
                    ServiceOption(id = "ac_2", title = "2 Split ACs (Combo)", price = 749, originalPrice = 1198, durationMin = 90)
                )
            ),
            ServiceItem(
                id = "ac_repair_visit",
                category = "AC",
                name = "AC Repair & Diagnosis Visit",
                te = "AC రిపేర్ & సమస్య పరిశీలన",
                hi = "AC रिपेयर और जांच विज़िट",
                price = 149,
                originalPrice = 199,
                durationMin = 30,
                inspection = true,
                includes = "Complete diagnostic check, gas leakage detection, PCB test and on-spot repair estimate.",
                rating = 4.8,
                ratingCount = "12k+"
            ),
            ServiceItem(
                id = "bathroom_clean",
                category = "CLEANING",
                name = "Bathroom Deep Cleaning",
                te = "బాత్‌రూమ్ డీప్ క్లీనింగ్",
                hi = "बाथरूम डीप क्लीनिंग",
                price = 299,
                originalPrice = 449,
                durationMin = 60,
                inspection = false,
                includes = "Tile scrubbing, toilet bowl descaling, tap buffing, floor stains removal and mirror polishing.",
                rating = 4.8,
                ratingCount = "22k+",
                options = listOf(
                    ServiceOption(id = "bath_1", title = "1 Bathroom", price = 299, originalPrice = 449, durationMin = 60),
                    ServiceOption(id = "bath_2", title = "2 Bathrooms (Combo)", price = 549, originalPrice = 899, durationMin = 110)
                )
            ),
            ServiceItem(
                id = "kitchen_clean",
                category = "CLEANING",
                name = "Kitchen Deep Cleaning",
                te = "కిచెన్ డీప్ క్లీనింగ్",
                hi = "किचन डीप क्लीनिंग",
                price = 499,
                originalPrice = 799,
                durationMin = 90,
                inspection = false,
                includes = "Exhaust fan degreasing, slab scrubbing, gas stove clean, sink descaling and exterior cabinet wipe.",
                rating = 4.8,
                ratingCount = "9k+"
            ),
            ServiceItem(
                id = "fan_install",
                category = "ELECTRICIAN",
                name = "Ceiling Fan Installation / Replacement",
                te = "సీలింగ్ ఫ్యాన్ బిగించడం",
                hi = "सीलिंग फैन लगाना",
                price = 129,
                originalPrice = 199,
                durationMin = 30,
                inspection = false,
                includes = "Unboxing, blade assembly, safe ceiling hook mounting, wiring and balance testing.",
                rating = 4.9,
                ratingCount = "15k+"
            ),
            ServiceItem(
                id = "switchboard_repair",
                category = "ELECTRICIAN",
                name = "Switchboard / MCB Repair",
                te = "స్విచ్‌బోర్డ్ / MCB రిపేర్",
                hi = "स्विचबोर्ड / MCB रिपेयर",
                price = 99,
                originalPrice = 149,
                durationMin = 30,
                inspection = false,
                includes = "Short circuit check, loose contact fix, switch replacement and voltage check.",
                rating = 4.8,
                ratingCount = "11k+"
            ),
            ServiceItem(
                id = "tap_leak_fix",
                category = "PLUMBER",
                name = "Tap Leak / Flush Tank Fix",
                te = "ట్యాప్ లీకేజ్ / ఫ్లష్ రిపేర్",
                hi = "नल लीकेज / फ्लश रिपेयर",
                price = 99,
                originalPrice = 149,
                durationMin = 30,
                inspection = false,
                includes = "Washer replacement, cartridge fix, Teflon sealing and water flow tuning.",
                rating = 4.8,
                ratingCount = "14k+"
            ),
            ServiceItem(
                id = "washing_machine_repair",
                category = "APPLIANCE",
                name = "Washing Machine Repair Visit",
                te = "వాషింగ్ మెషిన్ రిపేర్ విజిట్",
                hi = "वाशिंग मशीन रिपेयर विज़िट",
                price = 149,
                originalPrice = 199,
                durationMin = 45,
                inspection = true,
                includes = "Drum spin test, drain motor inspection, error code diagnosis and fair quote.",
                rating = 4.7,
                ratingCount = "8k+"
            )
        )

        val promoBanners = listOf(
            PromoBanner(
                id = "hero_ac_fest",
                headline = "Summer Cooling Deals",
                subheadline = "Split AC deep jet service starting ₹399",
                cta = "Book now",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/banners%2Fac_summer.webp?alt=media",
                bgStartColor = "#0F172A",
                bgEndColor = "#1E3A8A",
                targetServiceId = "ac_service"
            ),
            PromoBanner(
                id = "hero_clean_fest",
                headline = "Spotless Home Cleaning",
                subheadline = "Bathrooms & kitchens deep cleaned in 60 mins",
                cta = "Explore",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/banners%2Fclean_promo.webp?alt=media",
                bgStartColor = "#064E3B",
                bgEndColor = "#047857",
                targetCategoryId = "CLEANING"
            )
        )

        val offers = listOf(
            PromoOffer(
                code = "FIRST50",
                title = "Flat ₹50 OFF on first booking",
                type = "FLAT",
                value = 50,
                minOrder = 199,
                firstBookingOnly = true
            )
        )

        return ServicesCatalog(
            city = "Khammam",
            bookingFee = 19,
            inspectionFee = 29,
            commissionPct = 10,
            upiId = "dutype@icici",
            upiName = "DutyPe Services",
            minTopup = 100,
            categories = categories,
            services = services,
            promoBanners = promoBanners,
            offers = offers,
            mostBookedServiceIds = listOf("ac_service", "bathroom_clean", "fan_install", "tap_leak_fix")
        )
    }
}
