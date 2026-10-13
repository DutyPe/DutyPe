package com.example.dutype.homeservices

/**
 * Baseline default services catalog for DutyPe.
 * Provides instant 0ms offline availability on app cold-start (matching Swiggy / Urban Company),
 * preventing any blank screens or heavy blocking shimmers while fresh data is synced in the background.
 *
 * Curated to the 8 core home services:
 * 1. Home Cleaning (CLEANING)
 * 2. Electrician (ELECTRICIAN)
 * 3. Plumber (PLUMBER)
 * 4. Carpenter (CARPENTER)
 * 5. Painting (PAINTER)
 * 6. Home Help & Shifting (HOME_HELP)
 * 7. Cook & Maid (COOK_MAID)
 * 8. Babysitting & Child Care (BABYSITTER)
 */
object DefaultServicesCatalog {

    val ACTIVE_CUSTOMER_CATEGORY_IDS = setOf(
        "CLEANING",
        "ELECTRICIAN",
        "PLUMBER",
        "CARPENTER",
        "PAINTER",
        "HOME_HELP",
        "COOK_MAID",
        "BABYSITTER"
    )

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
            )
        )

        val services = listOf(
            // --- CLEANING ---
            ServiceItem(
                id = "bathroom_clean",
                category = "CLEANING",
                name = "Bathroom Deep Cleaning",
                te = "బాత్‌రూమ్ డీప్ క్లీనింగ్",
                hi = "बाथरूम डीप क्लीनिंग",
                price = 349,
                originalPrice = 499,
                durationMin = 60,
                inspection = false,
                includes = "Tile scrubbing, toilet bowl descaling, tap buffing, floor stains removal and mirror polishing.",
                rating = 4.8,
                ratingCount = "22k+",
                options = listOf(
                    ServiceOption(id = "bath_1", title = "1 Bathroom", price = 349, originalPrice = 499, durationMin = 60),
                    ServiceOption(id = "bath_2", title = "2 Bathrooms (Combo)", price = 599, originalPrice = 899, durationMin = 110)
                ),
                inclusions = listOf(
                    "Deep tile scrubbing & hard water stain removal",
                    "Toilet bowl & commode acid descaling",
                    "Tap, shower & chrome fixture buffing",
                    "Mirror polish & exhaust fan surface wipe",
                    "Floor disinfection & drain trap clearance"
                ),
                exclusions = listOf(
                    "Plumbing pipe repairs or tap leakage fix",
                    "Broken tile replacement or regrouting",
                    "Harsh acid chemicals (eco cleaner brought by partner)"
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
                ratingCount = "9k+",
                inclusions = listOf(
                    "Gas stove & kitchen countertop degreasing",
                    "Exhaust fan & tile wall oil stain removal",
                    "Kitchen sink descaling & drain wash",
                    "Cabinet exterior wiping & sanitization"
                ),
                exclusions = listOf(
                    "Inside cabinet utensil emptying / cleaning",
                    "Refrigerator or microwave internal deep cleaning",
                    "Chimney motor dismantling / internal motor wash"
                )
            ),
            ServiceItem(
                id = "clean_sweep",
                category = "CLEANING",
                name = "House Sweeping & Mopping (2 hrs)",
                te = "ఇల్లు ఊడ్చడం & తుడవడం (2 గంటలు)",
                hi = "घर की झाड़ू-पोछा (2 घंटे)",
                price = 299,
                originalPrice = 399,
                durationMin = 120,
                inspection = false,
                includes = "Sweeping, mopping and dusting of all rooms with floor disinfectant.",
                rating = 4.8,
                ratingCount = "15k+",
                inclusions = listOf(
                    "Complete dry sweeping of all rooms & halls",
                    "Wet disinfectant mopping of all floors",
                    "Surface dusting of tables, TV unit & window sills",
                    "Trash bag tying & doorstep disposal"
                ),
                exclusions = listOf(
                    "Washing heavy utensils or clothes",
                    "Moving heavy wardrobes or cots",
                    "Deep scrubbing of stubborn wall marks"
                )
            ),
            ServiceItem(
                id = "clean_sofa",
                category = "CLEANING",
                name = "Sofa Shampoo & Cleaning (5 Seats)",
                te = "సోఫా క్లీనింగ్ (5 సీట్లు)",
                hi = "सोफ़ा सफ़ाई (5 सीट)",
                price = 649,
                originalPrice = 999,
                durationMin = 90,
                inspection = false,
                includes = "Fabric deep shampooing, dust extraction, sanitization and stain removal.",
                rating = 4.9,
                ratingCount = "11k+",
                inclusions = listOf(
                    "High-suction dust & allergen extraction",
                    "Fabric foam shampooing & spot stain treatment",
                    "Anti-bacterial sanitization & mild deodorization"
                ),
                exclusions = listOf(
                    "Leather/suede re-coloring or tear stitching",
                    "100% removal of permanent ink, turmeric or oil marks",
                    "Instant drying (sofa takes 3–5 hours to air dry)"
                )
            ),
            ServiceItem(
                id = "clean_1bhk",
                category = "CLEANING",
                name = "Full Home Deep Cleaning (1BHK)",
                te = "ఇల్లు మొత్తం డీప్ క్లీనింగ్ (1BHK)",
                hi = "पूरे घर की डीप क्लीनिंग (1BHK)",
                price = 899,
                originalPrice = 1299,
                durationMin = 240,
                inspection = false,
                includes = "All rooms, kitchen and bathroom scrubbed, windows, fans and balcony.",
                rating = 4.9,
                ratingCount = "7k+",
                inclusions = listOf(
                    "Bedroom, living hall & dining deep cleaning",
                    "Complete bathroom deep scrub & descaling",
                    "Kitchen countertop, sink & exhaust wiping",
                    "Ceiling fan blades, switchboards & window grill dusting",
                    "Floor disinfectant mopping & balcony wash"
                ),
                exclusions = listOf(
                    "Wall repainting or putty patch work",
                    "Moving heavy almirahs or structural furniture",
                    "Terrace or outer building facade washing"
                )
            ),
            ServiceItem(
                id = "clean_2bhk",
                category = "CLEANING",
                name = "Full Home Deep Cleaning (2BHK)",
                te = "2 BHK ఇల్లు మొత్తం డీప్ క్లీనింగ్",
                hi = "पूरे घर की डीप क्लीनिंग (2BHK)",
                price = 1299,
                originalPrice = 1799,
                durationMin = 300,
                inspection = false,
                includes = "2 bedrooms, hall, kitchen, 2 bathrooms scrubbed, floors, fans, windows, balcony.",
                rating = 4.9,
                ratingCount = "5k+",
                inclusions = listOf(
                    "2 bedrooms, hall & dining hall deep cleaning",
                    "2 full bathrooms deep scrubbing & descaling",
                    "Kitchen degreasing, sink, slab & exterior cabinets",
                    "Ceiling fans, switchboards, window glass & balcony",
                    "Disinfectant floor scrub & trash disposal"
                ),
                exclusions = listOf(
                    "Wall repainting or putty patch work",
                    "Moving heavy immovable furniture",
                    "Terrace or exterior building facade washing"
                )
            ),

            // --- ELECTRICIAN ---
            ServiceItem(
                id = "fan_install",
                category = "ELECTRICIAN",
                name = "Ceiling Fan Installation / Replacement",
                te = "సీలింగ్ ఫ్యాన్ బిగించడం / రిపేర్",
                hi = "सीलिंग फैन लगाना",
                price = 129,
                originalPrice = 199,
                durationMin = 30,
                inspection = false,
                includes = "Unboxing, blade assembly, safe ceiling hook mounting, wiring and balance testing.",
                rating = 4.9,
                ratingCount = "15k+",
                inclusions = listOf(
                    "Unboxing & aerodynamic blade assembly",
                    "Ceiling hook mount & down-rod connection",
                    "Safety wire connection & insulation taping",
                    "Speed regulator test & wobble-free balance check"
                ),
                exclusions = listOf(
                    "Ceiling fan unit cost or capacitor replacement",
                    "Civil anchor drilling on raw concrete ceilings",
                    "New concealed wiring conduit from main switchboard"
                )
            ),
            ServiceItem(
                id = "switchboard_repair",
                category = "ELECTRICIAN",
                name = "Switchboard / Socket / MCB Repair",
                te = "స్విచ్‌బోర్డ్ / MCB రిపేర్",
                hi = "स्विचबोर्ड / MCB रिपेयर",
                price = 99,
                originalPrice = 149,
                durationMin = 30,
                inspection = false,
                includes = "Short circuit check, loose contact fix, switch replacement and voltage check.",
                rating = 4.8,
                ratingCount = "11k+",
                inclusions = listOf(
                    "Diagnosis of sparking, burning smell or tripped MCB",
                    "Replacement of faulty switch, 3-pin socket or indicator",
                    "Tightening phase, neutral & ground terminal screws",
                    "Voltage check with multimeter & safe load test"
                ),
                exclusions = listOf(
                    "Cost of new modular switches, sockets or MCBs",
                    "Full house electrical rewiring or wall chiseling"
                )
            ),
            ServiceItem(
                id = "elec_light",
                category = "ELECTRICIAN",
                name = "Light / Tube / Chandelier Fitting",
                te = "లైట్ / ట్యూబ్ ఫిట్టింగ్",
                hi = "लाइट / ट्यूब फिटिंग",
                price = 99,
                originalPrice = 149,
                durationMin = 20,
                inspection = false,
                includes = "Mounting up to 2 lights, bulb holder, tube frame and wire testing.",
                rating = 4.8,
                ratingCount = "9k+",
                inclusions = listOf(
                    "Mounting up to 2 LED battens, tube lights or surface spots",
                    "Hanging decorative chandelier on existing ceiling hook",
                    "Wiring strip, wire nut connection & voltage test"
                ),
                exclusions = listOf(
                    "Light fixtures, bulbs or LED driver transformer cost",
                    "False ceiling gyro cutting or fresh POP remodeling"
                )
            ),
            ServiceItem(
                id = "elec_tv",
                category = "ELECTRICIAN",
                name = "TV Wall Mounting (up to 55\")",
                te = "TV వాల్ మౌంటింగ్",
                hi = "TV वॉल माउंटिंग",
                price = 349,
                originalPrice = 499,
                durationMin = 45,
                inspection = false,
                includes = "Precision drilling, level align, heavy wall bracket mount and cable connection.",
                rating = 4.9,
                ratingCount = "8k+",
                inclusions = listOf(
                    "Precision wall marking & spirit-level bubble alignment",
                    "Heavy hammer drilling & grip wall anchor installation",
                    "Secure mounting of TV panel (up to 55 inches)",
                    "Connecting set-top box, HDMI & power cables"
                ),
                exclusions = listOf(
                    "TV wall mount bracket hardware (billed at MRP if needed)",
                    "Concealed in-wall wire ducting or wall chiseling",
                    "TV display repair or motherboard internal servicing"
                )
            ),
            ServiceItem(
                id = "elec_visit",
                category = "ELECTRICIAN",
                name = "Electrician Diagnosis & Inspection Visit",
                te = "ఎలక్ట్రీషియన్ విజిట్ (సమస్య పరిశీలన)",
                hi = "इलेक्ट्रीशियन विज़िट (जांच)",
                price = 99,
                originalPrice = 149,
                durationMin = 30,
                inspection = true,
                includes = "Doorstep electrician visit, fault finding and honest on-spot price estimate.",
                rating = 4.8,
                ratingCount = "13k+",
                inclusions = listOf(
                    "Doorstep visit by verified electrician",
                    "Thorough short-circuit tracing & load diagnosis",
                    "Upfront price quotation before commencing repair",
                    "Visit fee adjusted against final repair bill upon work acceptance"
                ),
                exclusions = listOf(
                    "Free replacement components, wires or materials",
                    "Major rewiring labor (quoted separately post-inspection)"
                )
            ),

            // --- PLUMBER ---
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
                ratingCount = "14k+",
                inclusions = listOf(
                    "Tap disassembly, spindle or washer replacement",
                    "Teflon thread sealing tape & drip arrest",
                    "Flush tank siphon valve or inlet float calibration",
                    "Water pressure balancing & shutoff test"
                ),
                exclusions = listOf(
                    "Cost of new brass tap, bib cock or internal flush kit",
                    "Concealed in-wall pipe joint leakage repairs"
                )
            ),
            ServiceItem(
                id = "plumb_block",
                category = "PLUMBER",
                name = "Drain / Sink / Toilet Blockage Removal",
                te = "డ్రెయిన్ / టాయిలెట్ బ్లాకేజ్ తొలగింపు",
                hi = "नाली / टॉयलेट ब्लॉकेज हटाना",
                price = 229,
                originalPrice = 349,
                durationMin = 45,
                inspection = false,
                includes = "Clearing choked kitchen sink, floor drain or commode pipe using plumbing tools.",
                rating = 4.8,
                ratingCount = "10k+",
                inclusions = listOf(
                    "Kitchen sink, wash basin or floor drain choke removal",
                    "Flexible plumbing snake wire rod clearance",
                    "Chemical drain cleaner treatment & degreasing",
                    "Complete water flow speed testing after clearing"
                ),
                exclusions = listOf(
                    "Main underground sewer line or septic tank pump-out",
                    "Excavating or breaking concrete floor pipes"
                )
            ),
            ServiceItem(
                id = "plumb_basin",
                category = "PLUMBER",
                name = "Wash Basin / Sink Installation",
                te = "వాష్ బేసిన్ / సింక్ ఫిట్టింగ్",
                hi = "वॉश बेसिन / सिंक फिटिंग",
                price = 399,
                originalPrice = 599,
                durationMin = 60,
                inspection = false,
                includes = "Wall mounting, waste coupling connection, tap setup and leak test.",
                rating = 4.9,
                ratingCount = "6k+",
                inclusions = listOf(
                    "Wall bracket anchoring & spirit-level basin alignment",
                    "Waste coupling, bottle trap & flexible drain pipe fitting",
                    "Angle stopcock & pillar cock installation",
                    "Silicone bead waterproof joint sealing"
                ),
                exclusions = listOf(
                    "Wash basin ceramic unit or vanity counter cabinet cost",
                    "Granite slab counter cutout carpentry or stone cutting"
                )
            ),
            ServiceItem(
                id = "plumb_tank",
                category = "PLUMBER",
                name = "Water Tank Cleaning (up to 1000L)",
                te = "వాటర్ ట్యాంక్ క్లీనింగ్ (1000 L వరకు)",
                hi = "पानी टंकी सफ़ाई (1000 L तक)",
                price = 699,
                originalPrice = 999,
                durationMin = 90,
                inspection = false,
                includes = "Complete water drainage, sludge removal, machine scrub and potassium perm. disinfect.",
                rating = 4.9,
                ratingCount = "8k+",
                inclusions = listOf(
                    "Complete drainage of residual muddy bottom water",
                    "Sediment, algae & heavy sludge manual extraction",
                    "Anti-bacterial scrubbing of interior walls & floor",
                    "Potassium permanganate sterilization & final wash"
                ),
                exclusions = listOf(
                    "Plastic tank crack welding or physical tank repairs",
                    "Automatic water-level sensor switch installation"
                )
            ),
            ServiceItem(
                id = "plumb_visit",
                category = "PLUMBER",
                name = "Plumber Diagnosis & Inspection Visit",
                te = "ప్లంబర్ విజిట్ (సమస్య పరిశీలన)",
                hi = "प्लंबर विज़िट (जांच)",
                price = 99,
                originalPrice = 149,
                durationMin = 30,
                inspection = true,
                includes = "Doorstep plumbing inspection, root cause diagnosis and direct price quote.",
                rating = 4.8,
                ratingCount = "11k+",
                inclusions = listOf(
                    "Doorstep visit by verified professional plumber",
                    "Pipeline leak, seepage or pressure drop diagnosis",
                    "Honest transparent repair estimate before starting work",
                    "Inspection fee adjusted against final repair upon acceptance"
                ),
                exclusions = listOf(
                    "Replacement pipes, brass fittings or consumable parts",
                    "Major bathroom remodeling (quoted separately)"
                )
            ),

            // --- CARPENTER ---
            ServiceItem(
                id = "carp_lock",
                category = "CARPENTER",
                name = "Door Lock Installation / Repair",
                te = "డోర్ లాక్ ఫిట్టింగ్ / రిపేర్",
                hi = "दरवाज़े का ताला फिटिंग / रिपेयर",
                price = 199,
                originalPrice = 299,
                durationMin = 40,
                inspection = false,
                includes = "Mortise lock, cylindrical handle or latch fix and strike plate alignment.",
                rating = 4.8,
                ratingCount = "9k+",
                inclusions = listOf(
                    "Cylindrical lock, mortise lock or handle replacement",
                    "Wood chiseling & latch strike plate alignment",
                    "Smooth key rotation, latch lubricate & locking test"
                ),
                exclusions = listOf(
                    "Cost of new lock body, brass handle or cylinder",
                    "Digital / biometric smart lock circuit installation"
                )
            ),
            ServiceItem(
                id = "carp_door",
                category = "CARPENTER",
                name = "Door / Window Alignment & Hinges Fix",
                te = "డోర్ / కిటికీ అలైన్‌మెంట్, హింజెస్",
                hi = "दरवाज़ा / खिड़की अलाइनमेंट, कब्ज़े",
                price = 249,
                originalPrice = 349,
                durationMin = 45,
                inspection = false,
                includes = "Planer smoothing for jammed door, hinge tightening and smooth closing test.",
                rating = 4.8,
                ratingCount = "7k+",
                inclusions = listOf(
                    "Wood planer edging for swollen or sticking doors",
                    "Tightening loose hinges & replacing damaged screws",
                    "Tower bolt, latch & magnetic door catcher fixing"
                ),
                exclusions = listOf(
                    "Full door frame (chowkhat) structural replacement",
                    "Door surface polishing, painting or laminating"
                )
            ),
            ServiceItem(
                id = "carp_curtain",
                category = "CARPENTER",
                name = "Curtain Rod / Blind / Bracket Fitting",
                te = "కర్టెన్ రాడ్ / బ్రాకెట్ ఫిట్టింగ్",
                hi = "पर्दे की रॉड / ब्रैकेट फिटिंग",
                price = 149,
                originalPrice = 249,
                durationMin = 30,
                inspection = false,
                includes = "Up to 2 curtain rods, bracket drilling, level positioning and curtain hang.",
                rating = 4.9,
                ratingCount = "6k+",
                inclusions = listOf(
                    "Measurement & spirit level marking for up to 2 curtain rods",
                    "Wall drilling with rawl plugs & metal bracket mounting",
                    "Rod insertion, finials tightening & ring hang test"
                ),
                exclusions = listOf(
                    "Curtain rods, finials, brackets or curtain fabric cost",
                    "Motorized automated smart track installation"
                )
            ),
            ServiceItem(
                id = "carp_assembly",
                category = "CARPENTER",
                name = "Bed / Wardrobe / Furniture Assembly",
                te = "బెడ్ / ఫర్నిచర్ అసెంబ్లీ",
                hi = "बेड / फ़र्नीचर असेंबली",
                price = 449,
                originalPrice = 699,
                durationMin = 90,
                inspection = false,
                includes = "Full assembly of king/queen bed, study table or modular flat-pack wardrobe.",
                rating = 4.9,
                ratingCount = "8k+",
                inclusions = listOf(
                    "Flat-pack furniture unboxing & blueprint manual review",
                    "Assembly of Bed (King/Queen), Study Desk or Wardrobe",
                    "Tightening minifix cams, wooden dowels & drawer runners",
                    "Structural alignment, stability & level wobble check"
                ),
                exclusions = listOf(
                    "Cutting or customizing pre-laminated engineered wood",
                    "Wall anchor fasteners if absent from furniture pack"
                )
            ),
            ServiceItem(
                id = "carp_visit",
                category = "CARPENTER",
                name = "Carpenter Diagnosis & Minor Repair Visit",
                te = "కార్పెంటర్ విజిట్ (రిపేర్లు)",
                hi = "बढ़ई विज़िट (जांच)",
                price = 149,
                originalPrice = 199,
                durationMin = 30,
                inspection = true,
                includes = "Inspection of furniture damage, measurements and on-spot repair estimate.",
                rating = 4.8,
                ratingCount = "10k+",
                inclusions = listOf(
                    "Doorstep visit by experienced skilled carpenter",
                    "Inspection of broken furniture, wobbling chairs or kitchen cabinets",
                    "Accurate measurement & direct labor quote",
                    "Visit charge adjusted against final repair invoice upon acceptance"
                ),
                exclusions = listOf(
                    "Cost of new plywood, sunmica laminate or hardware parts"
                )
            ),

            // --- PAINTER ---
            ServiceItem(
                id = "paint_touchup",
                category = "PAINTER",
                name = "Wall Touch-up & Patch Painting (1 Wall)",
                te = "గోడ టచ్-అప్ పెయింటింగ్ (1 గోడ)",
                hi = "दीवार टच-अप पेंटिंग (1 दीवार)",
                price = 599,
                originalPrice = 799,
                durationMin = 180,
                inspection = false,
                includes = "Wall crack filling, sanding putty patches and 2 roller coats on one wall.",
                rating = 4.8,
                ratingCount = "5k+",
                inclusions = listOf(
                    "Nail hole filling, hairline crack repair & putty sanding",
                    "2 uniform roller coats of emulsion on 1 designated wall",
                    "Painter masking tape protection for switch plates & floor borders"
                ),
                exclusions = listOf(
                    "Paint bucket, primer & putty materials (customer provides color choice)",
                    "Deep wall dampness/water seepage chemical treatment"
                )
            ),
            ServiceItem(
                id = "paint_visit",
                category = "PAINTER",
                name = "Home Painting & Waterproofing Quote Visit",
                te = "పెయింటింగ్ / వాటర్‌ప్రూఫింగ్ కొటేషన్ విజిట్",
                hi = "पेंटिंग / वॉटरप्रूफ़िंग कोटेशन विज़िट",
                price = 99,
                originalPrice = 149,
                durationMin = 30,
                inspection = true,
                includes = "Laser area measurement, wall dampness inspection and transparent written quote.",
                rating = 4.9,
                ratingCount = "8k+",
                inclusions = listOf(
                    "Complete site inspection & laser tape sq. ft. area calculation",
                    "Wall dampness check & surface condition assessment",
                    "Transparent written quotation with paint grade recommendations",
                    "Visit charge waived / adjusted against final painting project"
                ),
                exclusions = listOf(
                    "Immediate paint execution on same day (consultation & quote visit)"
                )
            ),

            // --- HOME HELP & SHIFTING ---
            ServiceItem(
                id = "help_shifting",
                category = "HOME_HELP",
                name = "Shifting & Loading Helpers (2 Helpers, 3 hrs)",
                te = "షిఫ్టింగ్ / లోడింగ్ హెల్పర్లు (2 మంది, 3 గంటలు)",
                hi = "शिफ़्टिंग / लोडिंग हेल्पर (2 लोग, 3 घंटे)",
                price = 999,
                originalPrice = 1499,
                durationMin = 180,
                inspection = false,
                includes = "Two verified strong helpers to safely lift, load, pack and transport boxes/furniture.",
                rating = 4.9,
                ratingCount = "14k+",
                inclusions = listOf(
                    "2 verified strong helpers for up to 3 hours",
                    "Heavy lifting: cots, almirahs, sofas, washing machines & boxes",
                    "Safe loading & unloading onto transport vehicle (Tata Ace / Auto)",
                    "Arranging items into designated rooms at destination"
                ),
                exclusions = listOf(
                    "Transport truck / auto vehicle hire fare",
                    "Packaging material costs (carton boxes, bubble wrap & tape)",
                    "Complex electrical dismantling of AC or geyser units"
                )
            ),
            ServiceItem(
                id = "help_hourly_1hr",
                category = "HOME_HELP",
                name = "Hourly Home Helper / Heavy Lifting (1 hr)",
                te = "గంటల ప్రాతిపదికన ఇంటి సహాయం (1 గంట)",
                hi = "प्रति घंटा घरेलू मदद (1 घंटा)",
                price = 149,
                originalPrice = 199,
                durationMin = 60,
                inspection = false,
                includes = "One helper for general heavy chores, sorting, luggage carrying, errands or yard work.",
                rating = 4.8,
                ratingCount = "12k+",
                inclusions = listOf(
                    "1 helper for 1 hour of physical household assistance",
                    "Luggage carrying, moving heavy furniture or planters",
                    "Store room shifting or domestic sorting help"
                ),
                exclusions = listOf(
                    "Specialized technician repair work (no wiring or plumbing)",
                    "Handling hazardous chemicals or roof-top climbing"
                )
            ),
            ServiceItem(
                id = "help_hourly_2hr",
                category = "HOME_HELP",
                name = "Hourly Home Helper / Organize (2 hrs)",
                te = "గంటల ప్రాతిపదికన ఇంటి సహాయం (2 గంటలు)",
                hi = "प्रति घंटा घरेलू मदद (2 घंटे)",
                price = 249,
                originalPrice = 349,
                durationMin = 120,
                inspection = false,
                includes = "Two full hours of dedicated household assistance, box unpacking or rearranging.",
                rating = 4.8,
                ratingCount = "9k+",
                inclusions = listOf(
                    "2 dedicated hours of physical home helper support",
                    "Room rearranging, box unpacking & luggage lifting",
                    "Balcony, terrace or garage tidying & domestic chores"
                ),
                exclusions = listOf(
                    "Electrical wiring, masonry or plumbing installations",
                    "Disposal of heavy concrete building debris"
                )
            ),
            ServiceItem(
                id = "help_garden",
                category = "HOME_HELP",
                name = "Garden & Plants Weeding / Cleaning (2 hrs)",
                te = "గార్డెన్ / మొక్కల క్లీనింగ్ (2 గంటలు)",
                hi = "बगीचा / पौधों की सफ़ाई (2 घंटे)",
                price = 349,
                originalPrice = 499,
                durationMin = 120,
                inspection = false,
                includes = "Garden weeding, trimming overgrown shrubs, watering and dry leaf clearance.",
                rating = 4.7,
                ratingCount = "4k+",
                inclusions = listOf(
                    "Manual weeding of wild grass & lawn cleanup",
                    "Trimming overgrown hedges, bushes & plant pot pruning",
                    "Sweeping dry fallen leaves & packing into garden trash bags",
                    "Watering plants and loosening soil beds"
                ),
                exclusions = listOf(
                    "Felling large mature trees or chainsaw operations",
                    "Supply of new nursery plants, red soil or manure bags"
                )
            ),

            // --- COOK & MAID ---
            ServiceItem(
                id = "cook_one_meal",
                category = "COOK_MAID",
                name = "Home Cook for One Meal (up to 6 people)",
                te = "ఒక పూట వంటమనిషి (6 మంది వరకు)",
                hi = "एक समय का खाना (6 लोगों तक)",
                price = 299,
                originalPrice = 399,
                durationMin = 120,
                inspection = false,
                includes = "Fresh home-style cooking (curry, dal, rice, rotis). Counter cleaned after cooking.",
                rating = 4.9,
                ratingCount = "16k+",
                inclusions = listOf(
                    "Fresh home-style cooking for up to 6 family members",
                    "Choice of dishes: 1 Curry/Sabzi, 1 Dal/Sambar, Rice, Phulkas/Rotis",
                    "Vegetable peeling, cutting & prep work",
                    "Gas stove wiped & cooking platform sanitized after cooking"
                ),
                exclusions = listOf(
                    "Groceries, oil, spices, vegetables & gas (100% customer provided)",
                    "Washing accumulated utensils from earlier meals",
                    "Table serving / dining service"
                )
            ),
            ServiceItem(
                id = "maid_house_help",
                category = "COOK_MAID",
                name = "Maid House Help & Utensil Cleaning (2 hrs)",
                te = "మేడ్ ఇంటి పని & గిన్నెలు (2 గంటలు)",
                hi = "घरेलू काम और बर्तन (2 घंटे)",
                price = 249,
                originalPrice = 349,
                durationMin = 120,
                inspection = false,
                includes = "Washing all utensils, slab sanitizing, kitchen sink shine and light floor sweep.",
                rating = 4.8,
                ratingCount = "11k+",
                inclusions = listOf(
                    "Washing & scrub-cleaning all accumulated kitchen utensils & cutlery",
                    "Kitchen countertop sanitization & sink descaling",
                    "Drying & orderly stacking of cleaned utensils",
                    "Kitchen floor sweeping & damp mop wipe"
                ),
                exclusions = listOf(
                    "Hand-washing family clothes laundry or fabric ironing",
                    "Deep scrubbing of kitchen chimney internals"
                )
            ),
            ServiceItem(
                id = "cook_maid_combo",
                category = "COOK_MAID",
                name = "Cook & Maid Combo Service (3 hrs)",
                te = "వంటమనిషి & మేడ్ కాంబో (3 గంటలు)",
                hi = "कुक और मेड कॉम्बो सर्विस (3 घंटे)",
                price = 549,
                originalPrice = 799,
                durationMin = 180,
                inspection = false,
                includes = "Fresh meal cooked plus full utensils washing, kitchen cleanup and counter wipe.",
                rating = 4.9,
                ratingCount = "8k+",
                inclusions = listOf(
                    "Full 3-hour dedicated kitchen service: meal cooking + utensil washing",
                    "Fresh home meal preparation (Rice, Dal/Sambar, Curry & Rotis)",
                    "Complete scrub washing of all cooking and dining pots",
                    "Stove, countertop degreasing & kitchen floor mopping"
                ),
                exclusions = listOf(
                    "Groceries, vegetables, meat & gas (customer provided)",
                    "Non-kitchen household bedroom / bathroom cleaning"
                )
            ),

            // --- BABYSITTING & CHILD CARE ---
            ServiceItem(
                id = "baby_sitting_2hr",
                category = "BABYSITTER",
                name = "Babysitting & Child Care (2 hrs)",
                te = "పిల్లల సంరక్షణ & బేబీ సిట్టింగ్ (2 గంటలు)",
                hi = "बच्चों की देखभाल (2 घंटे)",
                price = 299,
                originalPrice = 449,
                durationMin = 120,
                inspection = false,
                includes = "Caring supervision, story reading, feeding support, playtime and safe monitoring.",
                rating = 4.9,
                ratingCount = "7k+",
                inclusions = listOf(
                    "Attentive, safe childcare supervision for 2 hours",
                    "Engaging indoor playtime, storytelling, drawing & games",
                    "Assisting with pre-cooked meals/snacks & drinking water",
                    "Gentle nap-time monitoring & basic diaper care"
                ),
                exclusions = listOf(
                    "Cooking meals or household cleaning/maid chores",
                    "Administering medicines without written parental instruction",
                    "Taking child outside the residential gated area"
                )
            ),
            ServiceItem(
                id = "baby_sitting_4hr",
                category = "BABYSITTER",
                name = "Babysitter Half-Day Care (4 hrs)",
                te = "హాఫ్ డే బేబీ సిట్టింగ్ (4 గంటలు)",
                hi = "हाफ डे बेबीसिटिंग (4 घंटे)",
                price = 549,
                originalPrice = 799,
                durationMin = 240,
                inspection = false,
                includes = "Four hours of patient attention, nap supervision, activity games and gentle care.",
                rating = 4.9,
                ratingCount = "5k+",
                inclusions = listOf(
                    "4 hours of patient, caring child attention",
                    "Indoor learning activities, books reading & creative games",
                    "Feeding assistance, hand hygiene & diaper changing",
                    "Scheduled nap supervision in safe crib/bed"
                ),
                exclusions = listOf(
                    "Household cooking or laundry washing",
                    "Leaving the home without parent's authorization"
                )
            ),
            ServiceItem(
                id = "baby_sitting_day",
                category = "BABYSITTER",
                name = "Full Day Nanny & Child Support (8 hrs)",
                te = "ఫుల్ డే బేబీ సిట్టర్ కేర్ (8 గంటలు)",
                hi = "फुल डे नैनी और चाइल्ड केयर (8 घंटे)",
                price = 999,
                originalPrice = 1499,
                durationMin = 480,
                inspection = false,
                includes = "Complete day assistance, meals feeding, diaper change, interactive play and safety oversight.",
                rating = 5.0,
                ratingCount = "4k+",
                inclusions = listOf(
                    "Full 8 hours of dedicated day nanny & childcare support",
                    "Feeding all scheduled meals, milk & snack routines",
                    "Multiple diaper changes, sponge bath & clothes changing",
                    "Interactive play, physical safety monitoring & peaceful nap routine"
                ),
                exclusions = listOf(
                    "Heavy domestic house maid work or family cooking",
                    "Unsupervised outings away from home premises"
                )
            )
        )

        val promoBanners = listOf(
            PromoBanner(
                id = "hero_clean_fest",
                headline = "Spotless Home Deep Cleaning",
                subheadline = "Sparkling clean bathrooms & kitchens from ₹299",
                cta = "Book now",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/banners%2Fclean_promo.webp?alt=media",
                bgStartColor = "#064E3B",
                bgEndColor = "#047857",
                targetCategoryId = "CLEANING"
            ),
            PromoBanner(
                id = "hero_repair_fest",
                headline = "Doorstep Electricians & Plumbers",
                subheadline = "Quick 30-min doorstep arrival starting ₹99",
                cta = "Book now",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/banners%2Felectrician.webp?alt=media",
                bgStartColor = "#0F172A",
                bgEndColor = "#1E3A8A",
                targetCategoryId = "ELECTRICIAN"
            ),
            PromoBanner(
                id = "hero_help_fest",
                headline = "Verified Shifting & Helpers",
                subheadline = "Safe packing, loading & house helpers from ₹149",
                cta = "Explore",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/banners%2Fhome_help.webp?alt=media",
                bgStartColor = "#7C2D12",
                bgEndColor = "#9A3412",
                targetCategoryId = "HOME_HELP"
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
            ),
            PromoOffer(
                code = "DASARA50",
                title = "Dasara Special: Flat ₹50 OFF",
                type = "FLAT",
                value = 50,
                minOrder = 249,
                firstBookingOnly = false
            )
        )

        return ServicesCatalog(
            city = "Khammam",
            bookingFee = 19,
            inspectionFee = 19,
            commissionPct = 0,
            upiId = "dutypein@ybl",
            upiName = "DutyPe Services",
            minTopup = 100,
            categories = categories,
            services = services,
            promoBanners = promoBanners,
            offers = offers,
            mostBookedServiceIds = listOf("bathroom_clean", "fan_install", "tap_leak_fix", "help_shifting")
        )
    }
}
