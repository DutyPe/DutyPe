/**
 * DutyPe Services catalog and money rules (pure; unit tested).
 *
 * Launch categories for Khammam (see docs in services.ts): AC, cleaning, electrician, plumber,
 * appliance & RO. Prices are starting points; admins override them in app_config/services.
 *
 * Money (no payment gateway): the customer pays the partner price + booking fee (+ approved
 * extras) in cash/UPI after the job. DutyPe takes its booking fee + commission from the partner's
 * prepaid credits when the job is completed.
 */
import { randomInt } from "crypto";

export type CategoryId =
  "AC" | "CLEANING" | "ELECTRICIAN" | "PLUMBER" | "APPLIANCE" | "CARPENTER" | "PAINTER" | "HOME_HELP" | "VEHICLE";

/**
 * BASIC work (cleaning, helpers, car wash) needs care and honesty, not training: any verified
 * worker can do it. SKILLED work (wiring, AC, plumbing, appliances, carpentry, painting) is risky
 * if done wrong, so the partner states experience and DutyPe checks the skill before approving
 * that category (phone test / photos of past work / certificate / trial job).
 */
export type SkillLevel = "BASIC" | "SKILLED";

export interface ServiceCategory {
  id: CategoryId;
  name: string;
  te: string;
  hi: string;
  skill: SkillLevel;
}

export interface ServiceItem {
  id: string;
  category: CategoryId;
  name: string;
  te: string;
  hi: string;
  /** Rupees. */
  price: number;
  /** Minutes the job usually takes. */
  durationMin: number;
  /** Visit + diagnosis; the final repair price is agreed on the spot (higher booking fee). */
  inspection?: boolean;
  includes: string;
  active?: boolean;
  /** What the customer keeps ready (ids of [ITEMS]); defaults to the category's list. */
  provide?: string[];
  /** What the partner brings (ids of [ITEMS]); defaults to the category's list. */
  bring?: string[];
}

/** Things to keep ready / bring, in three languages (ids used by services). */
export const ITEMS: Record<string, { en: string; te: string; hi: string }> = {
  BROOM: { en: "Broom", te: "చీపురు", hi: "झाड़ू" },
  MOP: { en: "Mop and bucket", te: "మాప్, బకెట్", hi: "पोछा और बाल्टी" },
  CLEANER: { en: "Floor / toilet cleaner liquid", te: "ఫ్లోర్ / టాయిలెట్ క్లీనర్", hi: "फ़र्श / टॉयलेट क्लीनर" },
  DISH_SOAP: { en: "Dish wash bar or liquid", te: "పాత్రలు కడిగే సబ్బు", hi: "बर्तन धोने का साबुन" },
  WATER: { en: "Water supply", te: "నీటి సదుపాయం", hi: "पानी" },
  POWER: { en: "Working power socket", te: "పనిచేసే కరెంట్ ప్లగ్", hi: "चालू बिजली सॉकेट" },
  LADDER: { en: "Ladder (for high work)", te: "నిచ్చెన (ఎత్తు పనికి)", hi: "सीढ़ी (ऊँचे काम के लिए)" },
  PARTS: { en: "Money for spare parts (paid at the bill price)", te: "విడిభాగాల డబ్బు (బిల్ ధరకే)", hi: "पुर्ज़ों का पैसा (बिल के दाम पर)" },
  NEW_ITEM: { en: "The new fan / light / tap / fitting to install", te: "బిగించాల్సిన కొత్త ఫ్యాన్ / లైట్ / ట్యాప్", hi: "लगाने वाला नया पंखा / लाइट / नल" },
  GROCERIES: { en: "Groceries, vegetables and gas stove", te: "సరుకులు, కూరగాయలు, గ్యాస్ స్టవ్", hi: "राशन, सब्ज़ी और गैस चूल्हा" },
  PAINT: { en: "Paint and putty (or ask the painter to buy, with bill)", te: "పెయింట్, పుట్టీ (లేదా బిల్‌తో పెయింటర్ కొంటారు)", hi: "पेंट और पुट्टी (या बिल के साथ पेंटर ख़रीदेगा)" },
  PACKING: { en: "Boxes and packing material", te: "బాక్సులు, ప్యాకింగ్ సామాను", hi: "डिब्बे और पैकिंग सामान" },
  CLEAR_SPACE: { en: "Clear space around the work area", te: "పని చేసే చోట ఖాళీ", hi: "काम की जगह खाली" },
  PETS_AWAY: { en: "Pets tied / kept away", te: "పెంపుడు జంతువులను దూరంగా ఉంచండి", hi: "पालतू जानवर दूर रखें" },
  TOOLKIT: { en: "Own tool kit", te: "సొంత టూల్ కిట్", hi: "अपना टूल किट" },
  TESTER: { en: "Line tester, insulated tools and gloves", te: "టెస్టర్, ఇన్సులేటెడ్ టూల్స్, గ్లౌజులు", hi: "टेस्टर, इंसुलेटेड औज़ार और दस्ताने" },
  DRILL: { en: "Drill machine", te: "డ్రిల్ మెషిన్", hi: "ड्रिल मशीन" },
  JET_PUMP: { en: "Jet pump and AC cleaning cover", te: "జెట్ పంప్, AC క్లీనింగ్ కవర్", hi: "जेट पंप और AC कवर" },
  GAS_KIT: { en: "Gas cylinder and pressure gauge", te: "గ్యాస్ సిలిండర్, ప్రెషర్ గేజ్", hi: "गैस सिलेंडर और प्रेशर गेज" },
  PIPE_TOOLS: { en: "Plumbing tools and sealing tape", te: "ప్లంబింగ్ టూల్స్, సీలింగ్ టేప్", hi: "प्लंबिंग औज़ार और सीलिंग टेप" },
  SCRUBBER: { en: "Scrubbing machine and cleaning chemicals", te: "స్క్రబ్బింగ్ మెషిన్, క్లీనింగ్ కెమికల్స్", hi: "स्क्रबिंग मशीन और केमिकल" },
  VACUUM: { en: "Vacuum / shampoo machine", te: "వాక్యూమ్ / షాంపూ మెషిన్", hi: "वैक्यूम / शैम्पू मशीन" },
  GLOVES: { en: "Gloves and clean clothes", te: "గ్లౌజులు, శుభ్రమైన దుస్తులు", hi: "दस्ताने और साफ़ कपड़े" },
  CAR_KIT: { en: "Car shampoo, microfibre cloths and vacuum", te: "కార్ షాంపూ, మైక్రోఫైబర్ క్లాత్, వాక్యూమ్", hi: "कार शैम्पू, माइक्रोफ़ाइबर कपड़ा, वैक्यूम" },
  PAINT_TOOLS: { en: "Brushes, rollers, sandpaper and sheets", te: "బ్రష్‌లు, రోలర్లు, సాండ్‌పేపర్, షీట్లు", hi: "ब्रश, रोलर, सैंडपेपर और चादर" },
  ROPES: { en: "Ropes / straps for lifting", te: "తాళ్లు / స్ట్రాప్స్", hi: "रस्सी / स्ट्रैप" },
};

/** Default "keep ready" / "partner brings" lists per category. */
const CATEGORY_ITEMS: Record<CategoryId, { provide: string[]; bring: string[] }> = {
  AC: { provide: ["POWER", "WATER", "LADDER", "CLEAR_SPACE"], bring: ["TOOLKIT", "JET_PUMP", "GLOVES"] },
  CLEANING: { provide: ["WATER", "POWER", "PETS_AWAY"], bring: ["SCRUBBER", "GLOVES"] },
  ELECTRICIAN: { provide: ["LADDER", "PARTS", "NEW_ITEM"], bring: ["TOOLKIT", "TESTER", "DRILL"] },
  PLUMBER: { provide: ["PARTS", "NEW_ITEM", "CLEAR_SPACE"], bring: ["TOOLKIT", "PIPE_TOOLS"] },
  APPLIANCE: { provide: ["POWER", "PARTS", "CLEAR_SPACE"], bring: ["TOOLKIT", "TESTER"] },
  CARPENTER: { provide: ["PARTS", "NEW_ITEM", "CLEAR_SPACE"], bring: ["TOOLKIT", "DRILL"] },
  PAINTER: { provide: ["PAINT", "CLEAR_SPACE", "LADDER"], bring: ["PAINT_TOOLS"] },
  HOME_HELP: { provide: ["CLEAR_SPACE"], bring: ["GLOVES"] },
  VEHICLE: { provide: ["WATER", "POWER"], bring: ["CAR_KIT"] },
};

export function provideFor(s: ServiceItem): string[] {
  return s.provide ?? CATEGORY_ITEMS[s.category].provide;
}

export function bringFor(s: ServiceItem): string[] {
  return s.bring ?? CATEGORY_ITEMS[s.category].bring;
}

export interface ServicesConfig {
  /** Rupees, fixed-price jobs. */
  bookingFee: number;
  /** Rupees, inspection visits. */
  inspectionFee: number;
  commissionPct: number;
  /** DutyPe's UPI ID for partner credit top-ups. */
  upiId: string;
  upiName: string;
  /** Smallest top-up, rupees. */
  minTopup: number;
  /** Area name shown in the app. */
  city: string;
  /** LGD district ids where DutyPe Services runs (lib/places.ts). Khammam = 509. */
  districtIds: number[];
  cityLat: number;
  cityLng: number;
  serviceRadiusKm: number;
  /** Rupees DutyPe takes from the partner per completed job (flat). */
  partnerFee: number;
  /** A partner's first completed job is free of the partner fee. */
  partnerFirstJobFree: boolean;
  /** A customer's first booking has no booking fee. */
  firstBookingFeeFree: boolean;
  /** Employers on a paid plan never pay the booking fee (plan benefit). */
  planMembersFeeFree: boolean;
  /** Admin-defined offers (festivals etc.); always capped so DutyPe never pays out. */
  coupons: Coupon[];
  categories: ServiceCategory[];
  services: ServiceItem[];
}

export interface Coupon {
  code: string;
  /** Shown to customers, e.g. "Dasara offer: ₹30 off". */
  title: string;
  type: "FLAT" | "PCT";
  /** Rupees (FLAT) or percent of the service price (PCT). */
  value: number;
  /** Rupees cap for PCT coupons. */
  maxOff?: number;
  /** Rupees: the service price must be at least this. */
  minOrder?: number;
  /** Epoch ms; 0/absent = no limit. */
  validFrom?: number;
  validTo?: number;
  firstBookingOnly?: boolean;
  /** Category ids it applies to; empty = all. */
  categories?: string[];
  /** Shown in the app's offers list. */
  visible?: boolean;
  active?: boolean;
}

export const CATEGORIES: ServiceCategory[] = [
  { id: "CLEANING", name: "Home Cleaning", te: "ఇంటి క్లీనింగ్", hi: "घर की सफ़ाई", skill: "BASIC" },
  { id: "AC", name: "AC Service & Repair", te: "AC సర్వీస్ & రిపేర్", hi: "AC सर्विस और रिपेयर", skill: "SKILLED" },
  { id: "ELECTRICIAN", name: "Electrician", te: "ఎలక్ట్రీషియన్", hi: "इलेक्ट्रीशियन", skill: "SKILLED" },
  { id: "PLUMBER", name: "Plumber", te: "ప్లంబర్", hi: "प्लंबर", skill: "SKILLED" },
  { id: "APPLIANCE", name: "Appliance & RO Repair", te: "అప్లయెన్స్ & RO రిపేర్", hi: "अप्लायंस और RO रिपेयर", skill: "SKILLED" },
  { id: "CARPENTER", name: "Carpenter", te: "కార్పెంటర్", hi: "बढ़ई", skill: "SKILLED" },
  { id: "PAINTER", name: "Painting", te: "పెయింటింగ్", hi: "पेंटिंग", skill: "SKILLED" },
  { id: "HOME_HELP", name: "Home Help & Shifting", te: "ఇంటి సహాయం & షిఫ్టింగ్", hi: "घर की मदद और शिफ़्टिंग", skill: "BASIC" },
  { id: "VEHICLE", name: "Car & Bike Wash", te: "కార్ & బైక్ వాష్", hi: "कार और बाइक वॉश", skill: "BASIC" },
];

export function skillOf(category: string): SkillLevel {
  return CATEGORIES.find((c) => c.id === category)?.skill ?? "SKILLED";
}

const s = (
  id: string, category: CategoryId, name: string, te: string, hi: string, price: number, durationMin: number,
  includes: string, extra: { inspection?: boolean; provide?: string[]; bring?: string[] } = {},
): ServiceItem => ({ id, category, name, te, hi, price, durationMin, includes, ...extra });
const VISIT = { inspection: true };

/**
 * Launch prices for Khammam (checked Oct 2026 against local shops on Sulekha / JustDial and
 * Urban Company Hyderabad, set a little below Hyderabad and in the middle of the local range,
 * so customers see a fair, fixed price and partners still earn well). Spare parts are always
 * extra, at the bill price. The admin can change any price.
 */
export const DEFAULT_SERVICES: ServiceItem[] = [
  // Cleaning (BASIC). Light cleaning: the customer gives broom / mop; deep cleaning: partner's machine.
  s("clean_sweep", "CLEANING", "House sweeping & mopping (2 hrs)", "ఇల్లు ఊడ్చడం & తుడవడం (2 గంటలు)", "घर की झाड़ू-पोछा (2 घंटे)", 299, 120,
    "Sweeping, mopping and dusting of all rooms, one helper for 2 hours.",
    { provide: ["BROOM", "MOP", "CLEANER", "WATER"], bring: ["GLOVES"] }),
  s("clean_utensils", "CLEANING", "Utensils washing (1 hr)", "పాత్రలు కడగడం (1 గంట)", "बर्तन धोना (1 घंटा)", 149, 60,
    "Washing and arranging utensils, one helper for 1 hour.", { provide: ["DISH_SOAP", "WATER"], bring: ["GLOVES"] }),
  s("clean_bathroom", "CLEANING", "Bathroom deep cleaning", "బాత్రూమ్ డీప్ క్లీనింగ్", "बाथरूम डीप क्लीनिंग", 399, 60,
    "One bathroom: tiles, taps, toilet and stain removal with machine."),
  s("clean_kitchen", "CLEANING", "Kitchen deep cleaning", "కిచెన్ డీప్ క్లీనింగ్", "किचन डीप क्लीनिंग", 1199, 150,
    "Slab, tiles, sink, chimney outside and cabinets outside."),
  s("clean_1bhk", "CLEANING", "Full home deep cleaning (1BHK)", "ఇల్లు మొత్తం డీప్ క్లీనింగ్ (1BHK)", "पूरे घर की डीप क्लीनिंग (1BHK)", 2799, 300,
    "All rooms, kitchen and bathroom, floors scrubbed, fans and windows."),
  s("clean_2bhk", "CLEANING", "Full home deep cleaning (2BHK)", "ఇల్లు మొత్తం డీప్ క్లీనింగ్ (2BHK)", "पूरे घर की डीप क्लीनिंग (2BHK)", 3699, 420,
    "All rooms, kitchen and bathrooms, floors scrubbed, fans and windows."),
  s("clean_sofa", "CLEANING", "Sofa cleaning (5 seats)", "సోఫా క్లీనింగ్ (5 సీట్లు)", "सोफ़ा सफ़ाई (5 सीट)", 649, 90,
    "Shampoo and vacuum cleaning of fabric sofa.", { provide: ["POWER", "WATER"], bring: ["VACUUM", "GLOVES"] }),
  s("clean_fans", "CLEANING", "Fans, cobwebs & windows (2 hrs)", "ఫ్యాన్లు, బూజు, కిటికీలు (2 గంటలు)", "पंखे, जाले और खिड़कियाँ (2 घंटे)", 349, 120,
    "Ceiling fans, cobwebs, window grills and glass of the house.", { provide: ["LADDER", "WATER", "CLEANER"], bring: ["GLOVES"] }),

  // AC (SKILLED)
  s("ac_service", "AC", "AC service (foam-jet)", "AC సర్వీస్ (ఫోమ్-జెట్)", "AC सर्विस (फोम-जेट)", 449, 60,
    "Filter, coil and drain cleaning, cooling check. Split or window, one AC."),
  s("ac_deep", "AC", "AC deep cleaning (indoor + outdoor)", "AC డీప్ క్లీనింగ్ (ఇండోర్ + అవుట్‌డోర్)", "AC डीप क्लीनिंग (इनडोर + आउटडोर)", 899, 90,
    "Indoor coil, blower and outdoor unit jet wash. One split AC."),
  s("ac_repair_visit", "AC", "AC not cooling / repair visit", "AC కూలింగ్ లేదు / రిపేర్ విజిట్", "AC ठंडा नहीं / रिपेयर विज़िट", 249, 45,
    "Technician finds the problem and tells the repair price before starting.", VISIT),
  s("ac_gas", "AC", "AC gas refill", "AC గ్యాస్ రీఫిల్", "AC गैस रिफिल", 2199, 90,
    "Leak check and gas top-up for one AC.", { bring: ["TOOLKIT", "GAS_KIT"] }),
  s("ac_install", "AC", "Split AC installation", "స్ప్లిట్ AC ఇన్‌స్టలేషన్", "स्प्लिट AC इंस्टॉलेशन", 1299, 120,
    "Indoor + outdoor unit fitting with existing pipe. Extra pipe/stand charged separately.", { bring: ["TOOLKIT", "DRILL", "GAS_KIT"] }),
  s("ac_uninstall", "AC", "AC uninstallation", "AC అన్‌ఇన్‌స్టలేషన్", "AC अनइंस्टॉलेशन", 599, 60,
    "Safe removal with gas pump-down."),

  // Electrician (SKILLED)
  s("elec_fan", "ELECTRICIAN", "Fan installation / repair", "ఫ్యాన్ ఫిట్టింగ్ / రిపేర్", "पंखा फिटिंग / रिपेयर", 149, 30,
    "One ceiling or wall fan. Parts extra."),
  s("elec_switch", "ELECTRICIAN", "Switch / socket repair", "స్విచ్ / సాకెట్ రిపేర్", "स्विच / सॉकेट रिपेयर", 99, 20,
    "Up to 2 switches or sockets. Parts extra."),
  s("elec_light", "ELECTRICIAN", "Light / tube fitting", "లైట్ / ట్యూబ్ ఫిట్టింగ్", "लाइट / ट्यूब फिटिंग", 99, 20,
    "Up to 2 lights. Parts extra."),
  s("elec_decor", "ELECTRICIAN", "Decorative light / chandelier fitting", "డెకరేటివ్ లైట్ / షాండ్లియర్", "डेकोरेटिव लाइट / झूमर", 299, 45,
    "One chandelier or decorative fixture."),
  s("elec_point", "ELECTRICIAN", "New socket / switch point", "కొత్త సాకెట్ / స్విచ్ పాయింట్", "नया सॉकेट / स्विच पॉइंट", 199, 45,
    "One new point from a nearby board, surface wiring. Wire extra."),
  s("elec_mcb", "ELECTRICIAN", "MCB / fuse / wiring fault", "MCB / ఫ్యూజ్ / వైరింగ్ ఫాల్ట్", "MCB / फ्यूज़ / वायरिंग फॉल्ट", 199, 45,
    "Find and fix a tripping MCB or fuse. Parts extra."),
  s("elec_inverter", "ELECTRICIAN", "Inverter / stabiliser installation", "ఇన్వర్టర్ / స్టెబిలైజర్ ఫిట్టింగ్", "इन्वर्टर / स्टेबलाइज़र फिटिंग", 349, 60,
    "Installation and wiring of one unit."),
  s("elec_tv", "ELECTRICIAN", "TV wall mounting", "TV వాల్ మౌంటింగ్", "TV वॉल माउंटिंग", 349, 45,
    "Mounting one TV up to 55 inch on the customer's bracket.", { provide: ["NEW_ITEM", "POWER"] }),
  s("elec_visit", "ELECTRICIAN", "Electrician visit (other work)", "ఎలక్ట్రీషియన్ విజిట్ (ఇతర పని)", "इलेक्ट्रीशियन विज़िट (अन्य काम)", 99, 30,
    "Electrician checks the problem and tells the price before starting.", VISIT),

  // Plumber (SKILLED)
  s("plumb_tap", "PLUMBER", "Tap / mixer fitting", "ట్యాప్ / మిక్సర్ ఫిట్టింగ్", "नल / मिक्सर फिटिंग", 129, 30,
    "One tap or mixer. Parts extra."),
  s("plumb_leak", "PLUMBER", "Leakage repair", "లీకేజ్ రిపేర్", "लीकेज रिपेयर", 199, 45,
    "Pipe, tap or tank leak. Parts extra."),
  s("plumb_block", "PLUMBER", "Drain / toilet blockage", "డ్రెయిన్ / టాయిలెట్ బ్లాకేజ్", "नाली / टॉयलेट ब्लॉकेज", 299, 45,
    "Clear one blocked drain, sink or toilet."),
  s("plumb_flush", "PLUMBER", "Flush tank repair", "ఫ్లష్ ట్యాంక్ రిపేర్", "फ्लश टैंक रिपेयर", 199, 40,
    "Fix one flush tank (parts extra)."),
  s("plumb_basin", "PLUMBER", "Wash basin / sink installation", "వాష్ బేసిన్ / సింక్ ఫిట్టింగ్", "वॉश बेसिन / सिंक फिटिंग", 399, 60,
    "Fitting of one basin or kitchen sink with waste pipe."),
  s("plumb_tank", "PLUMBER", "Water tank cleaning (up to 1000 L)", "వాటర్ ట్యాంక్ క్లీనింగ్ (1000 L వరకు)", "पानी टंकी सफ़ाई (1000 L तक)", 699, 90,
    "Drain, scrub and disinfect one overhead or sump tank.", { provide: ["LADDER", "POWER"], bring: ["SCRUBBER", "GLOVES"] }),
  s("plumb_motor", "PLUMBER", "Water motor repair / fitting", "వాటర్ మోటార్ రిపేర్ / ఫిట్టింగ్", "पानी मोटर रिपेयर / फिटिंग", 349, 60,
    "Motor check, starter or fitting work. Parts extra.", { bring: ["TOOLKIT", "TESTER", "PIPE_TOOLS"] }),

  // Appliance (SKILLED)
  s("ro_service", "APPLIANCE", "RO water purifier service", "RO వాటర్ ప్యూరిఫైయర్ సర్వీస్", "RO वाटर प्यूरीफायर सर्विस", 399, 45,
    "Cleaning, TDS check and pre-filter change. Other filters extra."),
  s("ro_install", "APPLIANCE", "RO installation / uninstallation", "RO ఇన్‌స్టలేషన్ / అన్‌ఇన్‌స్టలేషన్", "RO इंस्टॉलेशन / अनइंस्टॉलेशन", 349, 45,
    "Fitting or removal of one purifier.", { bring: ["TOOLKIT", "DRILL"] }),
  s("wm_repair", "APPLIANCE", "Washing machine repair visit", "వాషింగ్ మెషిన్ రిపేర్ విజిట్", "वॉशिंग मशीन रिपेयर विज़िट", 249, 45,
    "Technician finds the problem and tells the repair price before starting.", VISIT),
  s("fridge_repair", "APPLIANCE", "Fridge repair visit", "ఫ్రిజ్ రిపేర్ విజిట్", "फ्रिज रिपेयर विज़िट", 249, 45,
    "Technician finds the problem and tells the repair price before starting.", VISIT),
  s("tv_repair", "APPLIANCE", "TV repair visit", "TV రిపేర్ విజిట్", "TV रिपेयर विज़िट", 249, 45,
    "Technician checks the TV and tells the repair price before starting.", VISIT),
  s("mixer_repair", "APPLIANCE", "Mixer / grinder repair visit", "మిక్సీ / గ్రైండర్ రిపేర్ విజిట్", "मिक्सर / ग्राइंडर रिपेयर विज़िट", 149, 30,
    "Check and repair price for one mixer or grinder.", VISIT),
  s("chimney_clean", "APPLIANCE", "Kitchen chimney cleaning", "కిచెన్ చిమ్నీ క్లీనింగ్", "किचन चिमनी सफ़ाई", 699, 75,
    "Filter and inner cleaning of one chimney.", { bring: ["TOOLKIT", "SCRUBBER"] }),
  s("geyser", "APPLIANCE", "Geyser installation / repair", "గీజర్ ఫిట్టింగ్ / రిపేర్", "गीज़र फिटिंग / रिपेयर", 349, 45,
    "One geyser. Parts extra.", { bring: ["TOOLKIT", "TESTER", "DRILL"] }),

  // Carpenter (SKILLED)
  s("carp_lock", "CARPENTER", "Door lock fitting / repair", "డోర్ లాక్ ఫిట్టింగ్ / రిపేర్", "दरवाज़े का ताला फिटिंग / रिपेयर", 199, 40,
    "One lock or latch. New lock extra."),
  s("carp_door", "CARPENTER", "Door / window alignment & hinges", "డోర్ / కిటికీ అలైన్‌మెంట్, హింజెస్", "दरवाज़ा / खिड़की अलाइनमेंट, कब्ज़े", 249, 45,
    "Fix one door or window that does not close properly."),
  s("carp_curtain", "CARPENTER", "Curtain rod / bracket fitting", "కర్టెన్ రాడ్ / బ్రాకెట్ ఫిట్టింగ్", "पर्दे की रॉड / ब्रैकेट फिटिंग", 149, 30,
    "Up to 2 rods or brackets on the customer's material."),
  s("carp_assembly", "CARPENTER", "Bed / furniture assembly", "బెడ్ / ఫర్నిచర్ అసెంబ్లీ", "बेड / फ़र्नीचर असेंबली", 449, 90,
    "Assembly of one bed, wardrobe or table."),
  s("carp_visit", "CARPENTER", "Carpenter visit (repairs)", "కార్పెంటర్ విజిట్ (రిపేర్లు)", "बढ़ई विज़िट (रिपेयर)", 99, 30,
    "Carpenter checks the work and tells the price before starting.", VISIT),

  // Painting (SKILLED)
  s("paint_touchup", "PAINTER", "Wall touch-up painting (1 wall, labour)", "గోడ టచ్-అప్ పెయింటింగ్ (1 గోడ, కూలీ)", "दीवार टच-अप पेंटिंग (1 दीवार, मज़दूरी)", 699, 180,
    "Putty patches and two coats on one wall. Paint extra."),
  s("paint_visit", "PAINTER", "Painting / waterproofing quote visit", "పెయింటింగ్ / వాటర్‌ప్రూఫింగ్ కొటేషన్ విజిట్", "पेंटिंग / वॉटरप्रूफ़िंग कोटेशन विज़िट", 99, 30,
    "Painter measures the area and gives a written price before work.", { ...VISIT, provide: ["CLEAR_SPACE"], bring: ["TOOLKIT"] }),

  // Home help (BASIC)
  s("help_shifting", "HOME_HELP", "Shifting / loading helpers (2 people, 3 hrs)", "షిఫ్టింగ్ / లోడింగ్ హెల్పర్లు (2 మంది, 3 గంటలు)", "शिफ़्टिंग / लोडिंग हेल्पर (2 लोग, 3 घंटे)", 999, 180,
    "Two helpers to pack, carry and load. Vehicle not included.", { provide: ["PACKING", "CLEAR_SPACE"], bring: ["ROPES", "GLOVES"] }),
  s("help_cook", "HOME_HELP", "Cook for one meal (up to 6 people)", "ఒక పూట వంట (6 మంది వరకు)", "एक समय का खाना (6 लोगों तक)", 399, 150,
    "Home-style meal cooked in your kitchen.", { provide: ["GROCERIES", "DISH_SOAP"], bring: ["GLOVES"] }),
  s("help_garden", "HOME_HELP", "Garden / plants cleaning (2 hrs)", "గార్డెన్ / మొక్కల క్లీనింగ్ (2 గంటలు)", "बगीचा / पौधों की सफ़ाई (2 घंटे)", 349, 120,
    "Weeding, trimming and clearing leaves.", { provide: ["BROOM", "WATER"], bring: ["GLOVES"] }),
  s("help_festival", "HOME_HELP", "Festival decoration help (2 hrs)", "పండుగ అలంకరణ సహాయం (2 గంటలు)", "त्योहार सजावट में मदद (2 घंटे)", 349, 120,
    "Help with lights, flowers and arranging the house.", { provide: ["LADDER", "NEW_ITEM"], bring: ["GLOVES"] }),

  // Car & bike (BASIC)
  s("car_wash", "VEHICLE", "Car wash at home (outside + inside vacuum)", "ఇంటి వద్ద కార్ వాష్ (బయట + లోపల వాక్యూమ్)", "घर पर कार वॉश (बाहर + अंदर वैक्यूम)", 349, 60,
    "Foam wash, wipe and interior vacuum of one car."),
  s("bike_wash", "VEHICLE", "Bike / scooter wash at home", "ఇంటి వద్ద బైక్ / స్కూటర్ వాష్", "घर पर बाइक / स्कूटर वॉश", 149, 30,
    "Foam wash and wipe of one two-wheeler.", { provide: ["WATER"], bring: ["CAR_KIT"] }),
];

export const DEFAULT_CONFIG: ServicesConfig = {
  bookingFee: 19,
  inspectionFee: 49,
  commissionPct: 0,
  upiId: "",
  upiName: "DutyPe",
  minTopup: 200,
  city: "Khammam",
  districtIds: [509],
  cityLat: 17.2473,
  cityLng: 80.1514,
  serviceRadiusKm: 25,
  partnerFee: 19,
  partnerFirstJobFree: true,
  firstBookingFeeFree: true,
  planMembersFeeFree: true,
  coupons: [],
  categories: CATEGORIES,
  services: DEFAULT_SERVICES,
};

/** Defaults with the admin's overrides (app_config/services). Service overrides merge by id. */
export function mergeConfig(raw: unknown): ServicesConfig {
  const o = (raw && typeof raw === "object" ? raw : {}) as Record<string, unknown>;
  const num = (k: keyof ServicesConfig, min: number, max: number) => {
    const v = Number(o[k]);
    return Number.isFinite(v) && v >= min && v <= max ? v : DEFAULT_CONFIG[k] as number;
  };
  const str = (k: keyof ServicesConfig) => typeof o[k] === "string" ? String(o[k]).trim() : DEFAULT_CONFIG[k] as string;
  const overrides = new Map<string, Partial<ServiceItem>>();
  if (Array.isArray(o.services)) {
    for (const item of o.services as Array<Partial<ServiceItem>>) if (item && typeof item.id === "string") overrides.set(item.id, item);
  }
  const services: ServiceItem[] = DEFAULT_SERVICES.map((d) => {
    const ov = overrides.get(d.id);
    overrides.delete(d.id);
    if (!ov) return d;
    const price = Number(ov.price);
    return {
      ...d,
      ...(Number.isFinite(price) && price >= 0 && price <= 100_000 ? { price: Math.round(price) } : {}),
      ...(typeof ov.name === "string" && ov.name.trim() ? { name: ov.name.trim() } : {}),
      ...(typeof ov.includes === "string" ? { includes: ov.includes } : {}),
      ...(typeof ov.active === "boolean" ? { active: ov.active } : {}),
    };
  });
  // New services added only by the admin (need category, name and price).
  for (const ov of overrides.values()) {
    const category = CATEGORIES.find((c) => c.id === ov.category)?.id;
    const price = Number(ov.price);
    if (!category || typeof ov.name !== "string" || !ov.name.trim() || !Number.isFinite(price) || price < 0) continue;
    services.push({
      id: String(ov.id), category, name: ov.name.trim(), te: String(ov.te || ov.name), hi: String(ov.hi || ov.name),
      price: Math.round(price), durationMin: Number(ov.durationMin) || 60, includes: String(ov.includes || ""),
      ...(ov.inspection ? { inspection: true } : {}), ...(ov.active === false ? { active: false } : {}),
    });
  }
  return {
    bookingFee: num("bookingFee", 0, 500),
    inspectionFee: num("inspectionFee", 0, 500),
    commissionPct: num("commissionPct", 0, 50),
    upiId: str("upiId"),
    upiName: str("upiName") || "DutyPe",
    minTopup: num("minTopup", 1, 100_000),
    city: str("city") || DEFAULT_CONFIG.city,
    districtIds: Array.isArray(o.districtIds) && o.districtIds.length && o.districtIds.every((d) => Number.isInteger(d)) ?
      (o.districtIds as number[]) : DEFAULT_CONFIG.districtIds,
    cityLat: num("cityLat", -90, 90),
    cityLng: num("cityLng", -180, 180),
    serviceRadiusKm: num("serviceRadiusKm", 1, 200),
    partnerFee: num("partnerFee", 0, 500),
    partnerFirstJobFree: typeof o.partnerFirstJobFree === "boolean" ? o.partnerFirstJobFree : DEFAULT_CONFIG.partnerFirstJobFree,
    firstBookingFeeFree: typeof o.firstBookingFeeFree === "boolean" ? o.firstBookingFeeFree : DEFAULT_CONFIG.firstBookingFeeFree,
    planMembersFeeFree: typeof o.planMembersFeeFree === "boolean" ? o.planMembersFeeFree : DEFAULT_CONFIG.planMembersFeeFree,
    coupons: Array.isArray(o.coupons) ? (o.coupons as unknown[]).map(cleanCoupon).filter((c): c is Coupon => c !== null) : [],
    categories: CATEGORIES,
    services,
  };
}

function cleanCoupon(raw: unknown): Coupon | null {
  const c = (raw && typeof raw === "object" ? raw : {}) as Record<string, unknown>;
  const code = String(c.code || "").trim().toUpperCase();
  const value = Number(c.value);
  if (!/^[A-Z0-9]{3,20}$/.test(code) || !Number.isFinite(value) || value <= 0) return null;
  const type = c.type === "PCT" ? "PCT" : "FLAT";
  if (type === "PCT" && value > 100) return null;
  const n = (v: unknown) => (Number.isFinite(Number(v)) && Number(v) > 0 ? Number(v) : undefined);
  return {
    code,
    title: String(c.title || code).slice(0, 80),
    type,
    value,
    ...(n(c.maxOff) ? { maxOff: n(c.maxOff) } : {}),
    ...(n(c.minOrder) ? { minOrder: n(c.minOrder) } : {}),
    ...(n(c.validFrom) ? { validFrom: n(c.validFrom) } : {}),
    ...(n(c.validTo) ? { validTo: n(c.validTo) } : {}),
    ...(c.firstBookingOnly === true ? { firstBookingOnly: true } : {}),
    ...(Array.isArray(c.categories) && c.categories.length ? { categories: (c.categories as unknown[]).map(String) } : {}),
    visible: c.visible !== false,
    active: c.active !== false,
  };
}

export function findService(config: ServicesConfig, serviceId: string): ServiceItem | null {
  return config.services.find((x) => x.id === serviceId && x.active !== false) ?? null;
}

export function bookingFeeFor(config: ServicesConfig, service: ServiceItem): number {
  return service.inspection ? config.inspectionFee : config.bookingFee;
}

/** Paise DutyPe takes from the partner for a job: booking fee + commission on (price + extras). */
export function platformTakePaise(price: number, extras: number, bookingFee: number, commissionPct: number): number {
  const commission = Math.round(((price + extras) * 100 * commissionPct) / 100);
  return bookingFee * 100 + commission;
}

/** 4-digit start code the customer tells the partner on arrival. */
export function newStartOtp(): string {
  return String(randomInt(0, 10_000)).padStart(4, "0");
}

// ─────────────────────────────── offers & money ───────────────────────────────

export interface Quote {
  price: number;
  /** The listed booking fee (before any discount). */
  bookingFee: number;
  /** Rupees off for the customer (never more than DutyPe's take on the booking). */
  discount: number;
  discountLabel: string;
  couponCode: string;
  /** What the customer pays the partner (before extras). */
  total: number;
  /** Set when the coupon the customer typed cannot be used. */
  couponError?: string;
  /** Set when the coupon is valid but the first-booking offer is worth more (the better one is kept). */
  couponNote?: string;
}

/** Most DutyPe can give away on a booking: booking fee + the partner fee + commission on the price. */
export function maxDiscount(config: ServicesConfig, service: ServiceItem): number {
  return bookingFeeFor(config, service) + config.partnerFee + Math.floor((service.price * config.commissionPct) / 100);
}

/** Why a coupon cannot be used here, or null when it can. */
export function couponProblem(c: Coupon, service: ServiceItem, firstBooking: boolean, nowMs: number): string | null {
  if (c.active === false) return "This coupon is not active";
  if (c.validFrom && nowMs < c.validFrom) return "This offer has not started yet";
  if (c.validTo && nowMs > c.validTo) return "This offer has ended";
  if (c.firstBookingOnly && !firstBooking) return "This coupon is for your first booking only";
  if (c.minOrder && service.price < c.minOrder) return `This coupon needs a service of ₹${c.minOrder} or more`;
  if (c.categories?.length && !c.categories.includes(service.category)) return "This coupon is not for this service";
  return null;
}

function couponValue(c: Coupon, service: ServiceItem): number {
  const raw = c.type === "PCT" ? Math.floor((service.price * c.value) / 100) : c.value;
  return Math.max(0, Math.min(raw, c.maxOff ?? raw));
}

/**
 * The customer's price. First booking: the booking fee is free. A coupon replaces that if it is
 * worth more (offers never stack). Every discount is capped at [maxDiscount].
 */
export function quote(
  config: ServicesConfig, service: ServiceItem, firstBooking: boolean, couponCode: string, nowMs: number, planMember = false,
): Quote {
  const bookingFee = bookingFeeFor(config, service);
  const cap = maxDiscount(config, service);
  let discount = 0;
  let discountLabel = "";
  let code = "";
  let couponError: string | undefined;
  let couponNote: string | undefined;
  if (firstBooking && config.firstBookingFeeFree && bookingFee > 0) {
    discount = bookingFee;
    discountLabel = "First booking: no booking fee";
  } else if (planMember && config.planMembersFeeFree && bookingFee > 0) {
    discount = bookingFee;
    discountLabel = "DutyPe plan: no booking fee";
  }
  const typed = couponCode.trim().toUpperCase();
  if (typed) {
    const c = config.coupons.find((x) => x.code === typed);
    const problem = c ? couponProblem(c, service, firstBooking, nowMs) : "This coupon code is not valid";
    if (problem || !c) {
      couponError = problem || "This coupon code is not valid";
    } else {
      const value = Math.min(couponValue(c, service), cap);
      if (value > discount) {
        discount = value;
        discountLabel = c.title;
        code = c.code;
      } else {
        couponNote = "Your first-booking offer is already better";
      }
    }
  }
  discount = Math.min(discount, cap);
  return {
    price: service.price, bookingFee, discount, discountLabel, couponCode: code,
    total: service.price + bookingFee - discount, ...(couponError ? { couponError } : {}),
    ...(couponNote ? { couponNote } : {}),
  };
}

/**
 * Partner fee charged to the partner who accepts. Their first job is free, except for the part of a
 * customer discount that the booking fee + commission cannot cover (so DutyPe never pays out).
 */
export function partnerFeeFor(
  config: ServicesConfig, jobsCompleted: number, price: number, bookingFee: number, discount: number, commissionPct: number,
): number {
  const full = config.partnerFee;
  if (!(config.partnerFirstJobFree && jobsCompleted === 0)) return full;
  const covered = bookingFee + Math.floor((price * commissionPct) / 100);
  return Math.min(full, Math.max(0, discount - covered));
}

/** Paise DutyPe takes from the partner's credits for a job. Never negative. */
export function takePaise(
  price: number, extras: number, bookingFee: number, discount: number, partnerFee: number, commissionPct: number,
): number {
  const commission = Math.round(((price + extras) * 100 * commissionPct) / 100);
  return Math.max(0, (bookingFee - discount + partnerFee) * 100 + commission);
}
