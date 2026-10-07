"use client";

import { useEffect, useMemo, useState } from "react";
import { doc, getDoc, setDoc } from "firebase/firestore";
import { getFirebaseServices } from "@/lib/firebase/client";
import { Plus, Trash2, Check, RefreshCw, Layers, Sparkles, AlertCircle, ChevronDown, ChevronRight, Tag, ArrowUp, ArrowDown, Star, Search, X } from "lucide-react";

export interface CategoryDef {
  id: string;
  name: string;
  te: string;
  hi: string;
  skill: "BASIC" | "SKILLED";
  imageUrl?: string;
}

export interface ServiceOptionDef {
  id: string;
  title: string;
  price: number;
  originalPrice?: number;
  durationMin?: number;
  description?: string;
}

export interface ServiceItemDef {
  id: string;
  category: string;
  name: string;
  te?: string;
  hi?: string;
  price: number;
  originalPrice?: number;
  durationMin: number;
  inspection?: boolean;
  includes: string;
  inclusions?: string[];
  exclusions?: string[];
  imageUrl?: string;
  options?: ServiceOptionDef[];
  active?: boolean;
}

const DEFAULT_CATEGORIES: CategoryDef[] = [
  { id: "CLEANING", name: "Home Cleaning", te: "ఇంటి క్లీనింగ్", hi: "घर की सफाई", skill: "BASIC" },
  { id: "AC", name: "AC Service & Repair", te: "ఏసీ సర్వీస్", hi: "एसी सर्विस", skill: "SKILLED" },
  { id: "ELECTRICIAN", name: "Electrician", te: "ఎలక్ట్రీషియన్", hi: "इलेक्ट्रीशियन", skill: "SKILLED" },
  { id: "PLUMBER", name: "Plumber", te: "ప్లంబర్", hi: "प्लम्बर", skill: "SKILLED" },
  { id: "APPLIANCE", name: "Appliance & RO Repair", te: "ఉపకరణాలు & ఆర్వో", hi: "उपकरण और आरओ", skill: "SKILLED" },
  { id: "CARPENTER", name: "Carpenter", te: "కార్పెంటర్", hi: "बढ़ई", skill: "SKILLED" },
  { id: "PAINTER", name: "Painting", te: "పెయింటింగ్", hi: "पेंटिंग", skill: "SKILLED" },
  { id: "HOME_HELP", name: "Home Help & Shifting", te: "హెల్పర్స్ & షిఫ్టింగ్", hi: "हेल्पर्स और शिफ्टिंग", skill: "BASIC" },
  { id: "VEHICLE", name: "Car & Bike Wash", te: "కార్ & బైక్ వాష్", hi: "कार और बाइक वॉश", skill: "BASIC" }
];

const DEFAULT_SERVICES: ServiceItemDef[] = [
  // Cleaning
  { id: "clean_sweep", category: "CLEANING", name: "House sweeping & mopping (2 hrs)", te: "ఇల్లు ఊడ్చడం & తుడవడం (2 గంటలు)", hi: "घर की झाड़ू-पोछा (2 घंटे)", price: 299, durationMin: 120, includes: "Sweeping, mopping and dusting of all rooms, one helper for 2 hours.", active: true },
  { id: "clean_utensils", category: "CLEANING", name: "Utensils washing (1 hr)", te: "పాత్రలు కడగడం (1 గంట)", hi: "बर्तन धोना (1 घंटा)", price: 149, durationMin: 60, includes: "Washing and arranging utensils, one helper for 1 hour.", active: true },
  { id: "clean_bathroom", category: "CLEANING", name: "Bathroom deep cleaning", te: "బాత్రూమ్ డీప్ క్లీనింగ్", hi: "बाथरूम डीप क्लीनिंग", price: 399, durationMin: 60, includes: "One bathroom: tiles, taps, toilet and stain removal with machine.", active: true },
  { id: "clean_kitchen", category: "CLEANING", name: "Kitchen deep cleaning", te: "కిచెన్ డీప్ క్లీనింగ్", hi: "किचन डीप क्लीनिंग", price: 1199, durationMin: 150, includes: "Slab, tiles, sink, chimney outside and cabinets outside.", active: true },
  { id: "clean_1bhk", category: "CLEANING", name: "Full home deep cleaning (1BHK)", te: "ఇల్లు మొత్తం డీప్ క్లీనింగ్ (1BHK)", hi: "पूरे घर की डीप क्लीनिंग (1BHK)", price: 2799, durationMin: 300, includes: "All rooms, kitchen and bathroom, floors scrubbed, fans and windows.", active: true },
  { id: "clean_2bhk", category: "CLEANING", name: "Full home deep cleaning (2BHK)", te: "ఇల్లు మొత్తం డీప్ క్లీనింగ్ (2BHK)", hi: "पूरे घर की डीప్ క్లీనింగ్ (2BHK)", price: 3699, durationMin: 420, includes: "All rooms, kitchen and bathrooms, floors scrubbed, fans and windows.", active: true },
  { id: "clean_sofa", category: "CLEANING", name: "Sofa cleaning (5 seats)", te: "సోఫా క్లీనింగ్ (5 సీట్లు)", hi: "सोफ़ा सफ़ाई (5 सीट)", price: 649, durationMin: 90, includes: "Shampoo and vacuum cleaning of fabric sofa.", active: true },
  { id: "clean_fans", category: "CLEANING", name: "Fans, cobwebs & windows (2 hrs)", te: "ఫ్యాన్లు, బూజు, కిటికీలు (2 గంటలు)", hi: "पंखे, जाले और खिड़कियाँ (2 घंटे)", price: 349, durationMin: 120, includes: "Ceiling fans, cobwebs, window grills and glass of the house.", active: true },
  { id: "clean_balcony", category: "CLEANING", name: "Balcony deep cleaning", te: "బాల్కనీ డీప్ క్లీనింగ్", hi: "बालकनी डीप क्लीनिंग", price: 249, durationMin: 45, includes: "Floor scrubbing, railing wipe and dust removal for one balcony.", active: true },
  { id: "clean_fridge", category: "CLEANING", name: "Fridge deep cleaning", te: "ఫ్రిజ్ డీప్ క్లీనింగ్", hi: "फ्रिज डीप क्लीनिंग", price: 299, durationMin: 45, includes: "Shelves, trays, exterior wipe and interior sanitization of one refrigerator.", active: true },
  { id: "clean_windows", category: "CLEANING", name: "Window & mesh cleaning (up to 4 windows)", te: "కిటికీలు & మెష్ క్లీనింగ్", hi: "खिड़कियाँ और जाली सफ़ाई", price: 299, durationMin: 60, includes: "Mesh dusting, grill washing and glass wipe for up to 4 windows.", active: true },
  { id: "clean_kitchen_prep", category: "CLEANING", name: "Kitchen prep & chopping (1 hr)", te: "కిచెన్ ప్రిపరేషన్ & కూరగాయలు తరగడం (1 గంట)", hi: "किचन प्रेप और सब्ज़ी कटिंग (1 घंटा)", price: 149, durationMin: 60, includes: "Vegetable washing, chopping, kneading and kitchen counter organizing.", active: true },
  { id: "clean_wardrobe", category: "CLEANING", name: "Complete wardrobe organization (2 hrs)", te: "వార్డ్‌రోబ్ ఆర్గనైజేషన్ (2 గంటలు)", hi: "वार्डरोब संगठन (2 घंटे)", price: 349, durationMin: 120, includes: "Folding, categorizing and neat organizing of one master wardrobe.", active: true },

  // AC
  { id: "ac_service", category: "AC", name: "AC service (foam-jet)", te: "AC సర్వీస్ (ఫోమ్-జెట్)", hi: "AC सर्विस (फोम-जेट)", price: 449, durationMin: 60, includes: "Filter, coil and drain cleaning, cooling check. Split or window, one AC.", active: true },
  { id: "ac_deep", category: "AC", name: "AC deep cleaning (indoor + outdoor)", te: "AC డీప్ క్లీనింగ్ (ఇండోర్ + అవుట్‌డోర్)", hi: "AC डीप क्लीनिंग (इनडोर + आउटडोर)", price: 899, durationMin: 90, includes: "Indoor coil, blower and outdoor unit jet wash. One split AC.", active: true },
  { id: "ac_repair_visit", category: "AC", name: "AC not cooling / repair visit", te: "AC కూలింగ్ లేదు / రిపేర్ విజిట్", hi: "AC ठंडा नहीं / रिपेयर विज़िट", price: 249, durationMin: 45, includes: "Technician finds the problem and tells the repair price before starting.", inspection: true, active: true },
  { id: "ac_gas", category: "AC", name: "AC gas refill", te: "AC గ్యాస్ రీఫిల్", hi: "AC गैस रिफिल", price: 2199, durationMin: 90, includes: "Leak check and gas top-up for one AC.", active: true },
  { id: "ac_install", category: "AC", name: "Split AC installation", te: "స్ప్లిట్ AC ఇన్‌స్టలేషన్", hi: "स्प्लिट AC इंस्टॉलेशन", price: 1299, durationMin: 120, includes: "Indoor + outdoor unit fitting with existing pipe.", active: true },
  { id: "ac_uninstall", category: "AC", name: "AC uninstallation", te: "AC అన్‌ఇన్‌స్టలేషన్", hi: "AC अनइंस्टॉलेशन", price: 599, durationMin: 60, includes: "Safe removal with gas pump-down.", active: true },

  // Electrician
  { id: "elec_fan", category: "ELECTRICIAN", name: "Fan installation / repair", te: "ఫ్యాన్ ఫిట్టింగ్ / రిపేర్", hi: "पंखा फिटिंग / रिपेयर", price: 149, durationMin: 30, includes: "One ceiling or wall fan. Parts extra.", active: true },
  { id: "elec_switch", category: "ELECTRICIAN", name: "Switch / socket repair", te: "స్విచ్ / సాకెట్ రిపేర్", hi: "స్విచ్ / सॉकेट रिपेयर", price: 99, durationMin: 20, includes: "Up to 2 switches or sockets. Parts extra.", active: true },
  { id: "elec_light", category: "ELECTRICIAN", name: "Light / tube fitting", te: "లైట్ / ట్యూబ్ ఫిట్టింగ్", hi: "लाइट / ट्यूब फिटिंग", price: 99, durationMin: 20, includes: "Up to 2 lights. Parts extra.", active: true },
  { id: "elec_decor", category: "ELECTRICIAN", name: "Decorative light / chandelier fitting", te: "డెకరేటివ్ లైట్ / షాండ్లియర్", hi: "डेकोरेटिव लाइट / झूमर", price: 299, durationMin: 45, includes: "One chandelier or decorative fixture.", active: true },
  { id: "elec_point", category: "ELECTRICIAN", name: "New socket / switch point", te: "కొత్త సాకెట్ / స్విచ్ పాయింట్", hi: "नया सॉकेट / स्विच पॉइंट", price: 199, durationMin: 45, includes: "One new point from a nearby board.", active: true },
  { id: "elec_mcb", category: "ELECTRICIAN", name: "MCB / fuse / wiring fault", te: "MCB / ఫ్యూజ్ / వైరింగ్ ఫాల్ట్", hi: "MCB / फ्यूज़ / वायरिंग फॉल्ट", price: 199, durationMin: 45, includes: "Find and fix a tripping MCB or fuse.", active: true },
  { id: "elec_inverter", category: "ELECTRICIAN", name: "Inverter / stabiliser installation", te: "ఇన్వర్టర్ / స్టెబిలైజర్ ఫిట్టింగ్", hi: "इन्वर्टर / स्टेबलाइज़र फिटिंग", price: 349, durationMin: 60, includes: "Installation and wiring of one unit.", active: true },
  { id: "elec_tv", category: "ELECTRICIAN", name: "TV wall mounting", te: "TV వాల్ మౌంటింగ్", hi: "TV वॉल माउंटिंग", price: 349, durationMin: 45, includes: "Mounting one TV up to 55 inch.", active: true },
  { id: "elec_visit", category: "ELECTRICIAN", name: "Electrician visit (other work)", te: "ఎలక్ట్రీషియన్ విజిట్ (ఇతర పని)", hi: "इलेक्ट्रीशियन विज़िट (अन्य काम)", price: 99, durationMin: 30, includes: "Electrician checks the problem and tells the price before starting.", inspection: true, active: true },

  // Plumber
  { id: "plumb_tap", category: "PLUMBER", name: "Tap / mixer fitting", te: "ట్యాప్ / మిక్సర్ ఫిట్టింగ్", hi: "नल / मिक्सर फिटिंग", price: 129, durationMin: 30, includes: "One tap or mixer. Parts extra.", active: true },
  { id: "plumb_leak", category: "PLUMBER", name: "Leakage repair", te: "లీకేజ్ రిపేర్", hi: "लीकेज रिपेयर", price: 199, durationMin: 45, includes: "Pipe, tap or tank leak. Parts extra.", active: true },
  { id: "plumb_block", category: "PLUMBER", name: "Drain / toilet blockage", te: "డ్రెయిన్ / టాయిలెట్ బ్లాకేజ్", hi: "नाली / टॉयलेट ब्लॉकेज", price: 299, durationMin: 45, includes: "Clear one blocked drain, sink or toilet.", active: true },
  { id: "plumb_flush", category: "PLUMBER", name: "Flush tank repair", te: "ఫ్లష్ ట్యాంక్ రిపేర్", hi: "फ्लश टैंक रिपेयर", price: 199, durationMin: 40, includes: "Fix one flush tank (parts extra).", active: true },
  { id: "plumb_basin", category: "PLUMBER", name: "Wash basin / sink installation", te: "వాష్ బేసిన్ / సింక్ ఫిట్టింగ్", hi: "वॉश बेसिन / सिंक फिटिंग", price: 399, durationMin: 60, includes: "Fitting of one basin or kitchen sink with waste pipe.", active: true },
  { id: "plumb_tank", category: "PLUMBER", name: "Water tank cleaning (up to 1000 L)", te: "వాటర్ ట్యాంక్ క్లీనింగ్ (1000 L వరకు)", hi: "पानी टंकी सफ़ाई (1000 L तक)", price: 699, durationMin: 90, includes: "Drain, scrub and disinfect one overhead or sump tank.", active: true },
  { id: "plumb_motor", category: "PLUMBER", name: "Water motor repair / fitting", te: "వాటర్ మోటార్ రిపేర్ / ఫిట్టింగ్", hi: "पानी मोटर रिपेयर / फिटिंग", price: 349, durationMin: 60, includes: "Motor check, starter or fitting work.", active: true },

  // Appliance
  { id: "ro_service", category: "APPLIANCE", name: "RO water purifier service", te: "RO వాటర్ ప్యూరిఫైయర్ సర్వీస్", hi: "RO वाटर प्यूरीफायर सर्विस", price: 399, durationMin: 45, includes: "Cleaning, TDS check and pre-filter change.", active: true },
  { id: "ro_install", category: "APPLIANCE", name: "RO installation / uninstallation", te: "RO ఇన్‌స్టలేషన్ / అన్‌ఇన్‌స్టలేషన్", hi: "RO इंस्टॉलेशन / अनइंस्टॉलेशन", price: 349, durationMin: 45, includes: "Fitting or removal of one purifier.", active: true },
  { id: "wm_repair", category: "APPLIANCE", name: "Washing machine repair visit", te: "వాషింగ్ మెషిన్ రిపేర్ విజిట్", hi: "वॉशिंग मशीन रिपेयर विज़िट", price: 249, durationMin: 45, includes: "Technician finds the problem and tells the repair price.", inspection: true, active: true },
  { id: "fridge_repair", category: "APPLIANCE", name: "Fridge repair visit", te: "ఫ్రిజ్ రిపేర్ విజిట్", hi: "फ्रिज रिपेयर विज़िट", price: 249, durationMin: 45, includes: "Technician finds the problem and tells the repair price.", inspection: true, active: true },
  { id: "tv_repair", category: "APPLIANCE", name: "TV repair visit", te: "TV రిపేర్ విజిట్", hi: "TV रिपेयर विज़िट", price: 249, durationMin: 45, includes: "Technician checks the TV and tells the repair price.", inspection: true, active: true },
  { id: "chimney_clean", category: "APPLIANCE", name: "Kitchen chimney cleaning", te: "కిచెన్ చిమ్నీ క్లీనింగ్", hi: "किचन चिमनी सफ़ाई", price: 699, durationMin: 75, includes: "Filter and inner cleaning of one chimney.", active: true },
  { id: "geyser", category: "APPLIANCE", name: "Geyser installation / repair", te: "గీజర్ ఫిట్టింగ్ / రిపేర్", hi: "गीज़र फिटिंग / रिपेयर", price: 349, durationMin: 45, includes: "One geyser. Parts extra.", active: true },

  // Carpenter
  { id: "carp_lock", category: "CARPENTER", name: "Door lock fitting / repair", te: "డోర్ లాక్ ఫిట్టింగ్ / రిపేర్", hi: "दरवाज़े का ताला फिटिंग / रिपेयर", price: 199, durationMin: 40, includes: "One lock or latch.", active: true },
  { id: "carp_door", category: "CARPENTER", name: "Door / window alignment & hinges", te: "డోర్ / కిటికీ అలైన్‌మెంట్, హింజెస్", hi: "दरवाज़ा / खिड़की अलाइनमेंट, कब्ज़े", price: 249, durationMin: 45, includes: "Fix one door or window that does not close properly.", active: true },
  { id: "carp_assembly", category: "CARPENTER", name: "Bed / furniture assembly", te: "బెడ్ / ఫర్నిచర్ అసెంబ్లీ", hi: "बेड / फ़र्नीचर असेंबली", price: 449, durationMin: 90, includes: "Assembly of one bed, wardrobe or table.", active: true },

  // Painting
  { id: "paint_touchup", category: "PAINTER", name: "Wall touch-up painting (1 wall)", te: "గోడ టచ్-అప్ పెయింటింగ్ (1 గోడ)", hi: "दीवार टच-अप पेंटिंग (1 दीवार)", price: 699, durationMin: 180, includes: "Putty patches and two coats on one wall.", active: true },
  { id: "paint_visit", category: "PAINTER", name: "Painting / waterproofing quote visit", te: "పెయింటింగ్ / వాటర్‌ప్రూఫింగ్ కొటేషన్ విజిట్", hi: "पेंटिंग / वॉटरप्रूफ़िंग कोटेशन विज़िट", price: 99, durationMin: 30, includes: "Painter measures the area and gives written price.", inspection: true, active: true },

  // Home Help
  { id: "help_shifting", category: "HOME_HELP", name: "Shifting / loading helpers (2 people, 3 hrs)", te: "షిఫ్టింగ్ / లోడింగ్ హెల్పర్లు (2 మంది, 3 గంటలు)", hi: "शिफ़्टिंग / लोडिंग हेल्पर (2 लोग, 3 घंटे)", price: 999, durationMin: 180, includes: "Two helpers to pack, carry and load.", active: true },
  { id: "help_cook", category: "HOME_HELP", name: "Cook for one meal (up to 6 people)", te: "ఒక పూట వంట (6 మంది వరకు)", hi: "एक समय का खाना (6 लोगों तक)", price: 399, durationMin: 150, includes: "Home-style meal cooked in your kitchen.", active: true },
  { id: "help_garden", category: "HOME_HELP", name: "Garden / plants cleaning (2 hrs)", te: "గార్డెన్ / మొక్కల క్లీనింగ్ (2 గంటలు)", hi: "बगीचा / पौधों की सफ़ाई (2 घंटे)", price: 349, durationMin: 120, includes: "Weeding, trimming and clearing leaves.", active: true },
  { id: "help_hourly_1hr", category: "HOME_HELP", name: "Hourly home helper (1 hr)", te: "గంటల ప్రాతిపదికన ఇంటి సహాయం (1 గంట)", hi: "प्रति घंटा घरेलू मदद (1 घंटा)", price: 149, durationMin: 60, includes: "One helper for general house work.", active: true },
  { id: "help_hourly_2hr", category: "HOME_HELP", name: "Hourly home helper (2 hrs)", te: "గంటల ప్రాతిపదికన ఇంటి సహాయం (2 గంటలు)", hi: "प्रति घंटा घरेलू मदद (2 घंटे)", price: 249, durationMin: 120, includes: "One helper for up to 2 hours.", active: true },

  // Vehicle
  { id: "car_wash", category: "VEHICLE", name: "Car wash at home (outside + inside vacuum)", te: "ఇంటి వద్ద కార్ వాష్ (బయట + లోపల వాక్యూమ్)", hi: "घर पर कार वॉश (बाहर + अंदर वैक्यूम)", price: 349, durationMin: 60, includes: "Foam wash, wipe and interior vacuum of one car.", active: true },
  { id: "bike_wash", category: "VEHICLE", name: "Bike / scooter wash at home", te: "ఇంటి వద్ద బైక్ / స్కూటర్ వాష్", hi: "घर पर बाइक / स्कूटर वॉश", price: 149, durationMin: 30, includes: "Foam wash and wipe of one two-wheeler.", active: true }
];

export function AdminCategoriesServicesManager({
  onSaved,
  onError
}: {
  onSaved: (msg: string) => void;
  onError: (msg: string) => void;
}) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [categories, setCategories] = useState<CategoryDef[]>([]);
  const [servicesList, setServicesList] = useState<ServiceItemDef[]>([]);
  const [rawConfig, setRawConfig] = useState<Record<string, unknown>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Category Form State
  const [showAddCategory, setShowAddCategory] = useState(false);
  const [newCatId, setNewCatId] = useState("");
  const [newCatName, setNewCatName] = useState("");
  const [newCatTe, setNewCatTe] = useState("");
  const [newCatHi, setNewCatHi] = useState("");
  const [newCatSkill, setNewCatSkill] = useState<"BASIC" | "SKILLED">("SKILLED");
  const [newCatImg, setNewCatImg] = useState("");

  // Service Form State
  const [selectedCatFilter, setSelectedCatFilter] = useState<string>("ALL");
  const [showAddService, setShowAddService] = useState(false);
  const [editingService, setEditingService] = useState<ServiceItemDef | null>(null);

  // Most Booked Services State
  const [mostBookedIds, setMostBookedIds] = useState<string[]>([]);
  const [mostBookedSearch, setMostBookedSearch] = useState<string>("");
  const [mostBookedCatFilter, setMostBookedCatFilter] = useState<string>("ALL");

  useEffect(() => {
    if (!services) return;
    void (async () => {
      setLoading(true);
      try {
        const snap = await getDoc(doc(services.db, "app_config", "services"));
        const data = (snap.data() || {}) as Record<string, unknown>;
        setRawConfig(data);

        // Merge default categories with custom Firestore categories
        const firestoreCats = Array.isArray(data.categories) ? (data.categories as CategoryDef[]) : [];
        const catMap = new Map<string, CategoryDef>();
        DEFAULT_CATEGORIES.forEach((c) => catMap.set(c.id.toUpperCase(), c));
        firestoreCats.forEach((c) => catMap.set(c.id.toUpperCase(), { ...catMap.get(c.id.toUpperCase()), ...c }));
        setCategories(Array.from(catMap.values()));

        // Merge default services with custom/override Firestore services
        const firestoreSvcs = Array.isArray(data.services) ? (data.services as ServiceItemDef[]) : [];
        const svcMap = new Map<string, ServiceItemDef>();
        DEFAULT_SERVICES.forEach((s) => svcMap.set(s.id.toLowerCase(), s));
        firestoreSvcs.forEach((s) => svcMap.set(s.id.toLowerCase(), { ...svcMap.get(s.id.toLowerCase()), ...s }));
        setServicesList(Array.from(svcMap.values()));

        // Most Booked Services
        const firestoreMostBooked = Array.isArray(data.mostBookedServiceIds) ? (data.mostBookedServiceIds as string[]) : [];
        setMostBookedIds(firestoreMostBooked);
      } catch (e) {
        onError("Could not load categories & services: " + (e instanceof Error ? e.message : String(e)));
      } finally {
        setLoading(false);
      }
    })();
  }, [services, onError]);

  // Add Category Handler
  async function handleAddCategory() {
    if (!newCatId.trim() || !newCatName.trim()) {
      alert("Please provide Category ID and Name");
      return;
    }
    const cleanId = newCatId.trim().toUpperCase().replace(/[^A-Z0-9_]/g, "_");
    const newEntry: CategoryDef = {
      id: cleanId,
      name: newCatName.trim(),
      te: newCatTe.trim() || newCatName.trim(),
      hi: newCatHi.trim() || newCatName.trim(),
      skill: newCatSkill,
      imageUrl: newCatImg.trim() || undefined
    };

    const nextCats = [...categories.filter((c) => c.id !== cleanId), newEntry];
    setCategories(nextCats);
    setShowAddCategory(false);
    setNewCatId("");
    setNewCatName("");
    setNewCatTe("");
    setNewCatHi("");
    setNewCatImg("");

    // Persist to Firestore
    await commitCatalogToFirestore(nextCats, servicesList);
    onSaved(`Category "${newEntry.name}" added successfully!`);
  }

  // Save or Update Service Handler
  async function handleSaveService(svc: ServiceItemDef) {
    const nextSvcs = [...servicesList.filter((s) => s.id !== svc.id), svc];
    setServicesList(nextSvcs);
    setEditingService(null);
    setShowAddService(false);

    await commitCatalogToFirestore(categories, nextSvcs);
    onSaved(`Service "${svc.name}" with ${svc.options?.length || 0} variant options saved!`);
  }

  // Delete Service Handler
  async function handleDeleteService(id: string) {
    if (!confirm("Are you sure you want to delete this service?")) return;
    const nextSvcs = servicesList.filter((s) => s.id !== id);
    setServicesList(nextSvcs);
    await commitCatalogToFirestore(categories, nextSvcs);
    onSaved("Service removed.");
  }

  async function commitCatalogToFirestore(cats: CategoryDef[], svcs: ServiceItemDef[]) {
    if (!services) return;
    setSaving(true);
    try {
      await setDoc(
        doc(services.db, "app_config", "services"),
        {
          ...rawConfig,
          categories: cats,
          services: svcs
        },
        { merge: true }
      );
    } catch (e) {
      onError("Failed to save to Firestore: " + (e instanceof Error ? e.message : String(e)));
    } finally {
      setSaving(false);
    }
  }

  function toggleMostBooked(id: string) {
    const cleanId = id.toLowerCase();
    if (mostBookedIds.map((x) => x.toLowerCase()).includes(cleanId)) {
      setMostBookedIds(mostBookedIds.filter((x) => x.toLowerCase() !== cleanId));
    } else {
      setMostBookedIds([...mostBookedIds, id]);
    }
  }

  function moveMostBooked(index: number, direction: -1 | 1) {
    const target = index + direction;
    if (target < 0 || target >= mostBookedIds.length) return;
    const next = [...mostBookedIds];
    const [moved] = next.splice(index, 1);
    next.splice(target, 0, moved);
    setMostBookedIds(next);
  }

  async function handleSaveMostBooked() {
    if (!services) return;
    setSaving(true);
    try {
      await setDoc(
        doc(services.db, "app_config", "services"),
        {
          ...rawConfig,
          mostBookedServiceIds: mostBookedIds
        },
        { merge: true }
      );
      onSaved(`Saved ${mostBookedIds.length} Most Booked Services! The mobile app updates immediately.`);
    } catch (e) {
      onError("Failed to save Most Booked Services: " + (e instanceof Error ? e.message : String(e)));
    } finally {
      setSaving(false);
    }
  }

  const filteredServices = selectedCatFilter === "ALL"
    ? servicesList
    : servicesList.filter((s) => s.category.toUpperCase() === selectedCatFilter.toUpperCase());

  const pinnedServices = useMemo(() => {
    const map = new Map<string, ServiceItemDef>();
    servicesList.forEach((s) => map.set(s.id.toLowerCase(), s));
    return mostBookedIds.map((id) => {
      const match = map.get(id.toLowerCase());
      if (match) return match;
      return {
        id,
        category: "CUSTOM",
        name: id,
        price: 0,
        durationMin: 0,
        includes: "",
        active: true
      };
    });
  }, [mostBookedIds, servicesList]);

  const selectableServices = useMemo(() => {
    return servicesList.filter((s) => {
      const matchCat = mostBookedCatFilter === "ALL" || s.category.toUpperCase() === mostBookedCatFilter.toUpperCase();
      const q = mostBookedSearch.trim().toLowerCase();
      const matchSearch = !q || s.name.toLowerCase().includes(q) || s.id.toLowerCase().includes(q) || s.category.toLowerCase().includes(q);
      return matchCat && matchSearch;
    });
  }, [servicesList, mostBookedCatFilter, mostBookedSearch]);

  if (loading) {
    return (
      <div style={{ padding: 32, textAlign: "center", color: "#64748b" }}>
        Loading service categories and items...
      </div>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Overview & Quick Actions */}
      <section className="admin-section" style={{ background: "#f8fafc", border: "1px solid #e2e8f0" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: 16 }}>
          <div>
            <h2 className="admin-section-title" style={{ display: "flex", alignItems: "center", gap: 8, margin: 0 }}>
              <Layers size={20} color="#7c3aed" />
              Service Categories &amp; Multi-Variant Packages
            </h2>
            <p style={{ margin: "4px 0 0 0", color: "#64748b", fontSize: 14, maxWidth: 720 }}>
              Add new local service categories (e.g. Salon, Car Wash) and create bookable service listings with
              multi-variant options (e.g. 1 Split AC ₹499 vs 2 Split ACs ₹899 vs Window AC ₹399).
              Everything updates the DutyPe mobile app catalog in real-time.
            </p>
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            <button
              onClick={() => setShowAddCategory(true)}
              className="btn"
              style={{ background: "#7c3aed", color: "#fff", fontWeight: 600, display: "flex", alignItems: "center", gap: 6 }}
            >
              <Plus size={16} />
              + Add New Category
            </button>
            <button
              onClick={() => {
                setEditingService({
                  id: `svc_${Date.now().toString(36)}`,
                  category: categories[0]?.id || "AC",
                  name: "",
                  te: "",
                  hi: "",
                  price: 299,
                  originalPrice: 399,
                  durationMin: 60,
                  includes: "",
                  inclusions: [],
                  exclusions: [],
                  options: [],
                  active: true
                });
                setShowAddService(true);
              }}
              className="btn btn-approve"
              style={{ fontWeight: 600, display: "flex", alignItems: "center", gap: 6 }}
            >
              <Plus size={16} />
              + Add Service (with Packages)
            </button>
          </div>
        </div>
      </section>

      {/* Add New Category Modal / Card */}
      {showAddCategory && (
        <section className="admin-section" style={{ background: "#ffffff", border: "2px solid #7c3aed", borderRadius: 12, padding: 20 }}>
          <h3 style={{ margin: "0 0 16px 0", fontSize: 16, fontWeight: 700, color: "#7c3aed" }}>
            Add New Service Category to DutyPe App
          </h3>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 12 }}>
            <div>
              <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                Category ID (UPPERCASE) *
              </label>
              <input
                type="text"
                value={newCatId}
                onChange={(e) => setNewCatId(e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, "_"))}
                placeholder="e.g. SALON or CAR_WASH"
                style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontFamily: "monospace" }}
              />
            </div>
            <div>
              <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                English Name *
              </label>
              <input
                type="text"
                value={newCatName}
                onChange={(e) => setNewCatName(e.target.value)}
                placeholder="e.g. Salon &amp; Men&apos;s Grooming"
                style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
              />
            </div>
            <div>
              <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                Telugu Label (తెలుగు)
              </label>
              <input
                type="text"
                value={newCatTe}
                onChange={(e) => setNewCatTe(e.target.value)}
                placeholder="e.g. సెలూన్ &amp; గ్రూమింగ్"
                style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
              />
            </div>
            <div>
              <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                Hindi Label (हिंदी)
              </label>
              <input
                type="text"
                value={newCatHi}
                onChange={(e) => setNewCatHi(e.target.value)}
                placeholder="e.g. सैलून और ग्रूमिंग"
                style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
              />
            </div>
            <div>
              <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                Skill Level
              </label>
              <select
                value={newCatSkill}
                onChange={(e) => setNewCatSkill(e.target.value as "BASIC" | "SKILLED")}
                style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
              >
                <option value="BASIC">Basic (Cleaning, Car Wash, Helper)</option>
                <option value="SKILLED">Skilled ⚡ (AC, Electrician, Plumber)</option>
              </select>
            </div>
            <div>
              <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                3D Clay Render URL (Optional)
              </label>
              <input
                type="text"
                value={newCatImg}
                onChange={(e) => setNewCatImg(e.target.value)}
                placeholder="https://firebasestorage.../clay_icon.webp"
                style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
              />
            </div>
          </div>
          <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, marginTop: 16 }}>
            <button type="button" onClick={() => setShowAddCategory(false)} className="btn">
              Cancel
            </button>
            <button type="button" onClick={handleAddCategory} className="btn" style={{ background: "#7c3aed", color: "#fff", fontWeight: 700 }}>
              Save Category to App
            </button>
          </div>
        </section>
      )}

      {/* Categories Horizontal Carousel Cards */}
      <section className="admin-section">
        <h3 style={{ margin: "0 0 12px 0", fontSize: 15, fontWeight: 700, color: "#1e293b" }}>
          Live App Categories ({categories.length})
        </h3>
        <div style={{ display: "flex", gap: 10, overflowX: "auto", paddingBottom: 8 }}>
          {categories.map((c) => (
            <div
              key={c.id}
              style={{
                flexShrink: 0,
                width: 170,
                padding: "12px 14px",
                borderRadius: 10,
                border: "1px solid #e2e8f0",
                background: "#ffffff",
                boxShadow: "0 1px 3px rgba(0,0,0,0.05)"
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span style={{ fontSize: 10, fontWeight: 800, padding: "2px 6px", borderRadius: 4, background: c.skill === "SKILLED" ? "#fef3c7" : "#dcfce7", color: c.skill === "SKILLED" ? "#b45309" : "#15803d" }}>
                  {c.skill === "SKILLED" ? "⚡ SKILLED" : "BASIC"}
                </span>
                <span style={{ fontSize: 11, color: "#94a3b8", fontFamily: "monospace" }}>{c.id}</span>
              </div>
              <div style={{ fontWeight: 700, fontSize: 13, marginTop: 8, color: "#0f172a" }}>{c.name}</div>
              <div style={{ fontSize: 11.5, color: "#64748b" }}>{c.te}</div>
            </div>
          ))}
        </div>
      </section>

      {/* Most Booked Services (Featured on Mobile App Home) */}
      <section className="admin-section" style={{ background: "#ffffff", border: "1px solid #e2e8f0", borderRadius: 12, padding: 20 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: 12, marginBottom: 16 }}>
          <div>
            <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: "#0f172a", display: "flex", alignItems: "center", gap: 8 }}>
              <Sparkles size={18} color="#f59e0b" />
              Most Booked Services (Featured on Mobile App Home)
            </h3>
            <p style={{ margin: "4px 0 0 0", color: "#64748b", fontSize: 13.5 }}>
              Choose and reorder the services shown in the &quot;Most booked services&quot; section on the mobile app home screen.
              {mostBookedIds.length === 0 ? " (Currently defaulting to the first 8 active services)" : ` (${mostBookedIds.length} pinned)`}
            </p>
          </div>
          <button
            type="button"
            onClick={handleSaveMostBooked}
            disabled={saving}
            className="btn"
            style={{
              background: "#059669",
              color: "#fff",
              fontWeight: 700,
              display: "flex",
              alignItems: "center",
              gap: 6,
              padding: "8px 16px",
              boxShadow: "0 1px 2px rgba(0,0,0,0.05)"
            }}
          >
            <Check size={16} />
            {saving ? "Saving..." : "Save Most Booked to App"}
          </button>
        </div>

        {/* Current Pinned Services Strip */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: "#334155", marginBottom: 8, display: "flex", alignItems: "center", gap: 6 }}>
            <span>Currently Featured ({pinnedServices.length})</span>
            {pinnedServices.length > 0 && (
              <span style={{ fontSize: 11, fontWeight: 500, color: "#64748b" }}>
                — use arrows to reorder, click × to unpin
              </span>
            )}
          </div>
          {pinnedServices.length === 0 ? (
            <div style={{ padding: "14px 18px", borderRadius: 8, background: "#f8fafc", border: "1px dashed #cbd5e1", fontSize: 13, color: "#64748b" }}>
              No custom services pinned yet. The mobile app automatically displays the top 8 catalog services. Click services below to pin specific ones.
            </div>
          ) : (
            <div style={{ display: "flex", gap: 10, overflowX: "auto", paddingBottom: 8 }}>
              {pinnedServices.map((svc, idx) => (
                <div
                  key={`${svc.id}_${idx}`}
                  style={{
                    flexShrink: 0,
                    width: 220,
                    padding: "10px 12px",
                    borderRadius: 10,
                    border: "1.5px solid #fde68a",
                    background: "#fffbeb",
                    boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
                    position: "relative"
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                    <span style={{ fontSize: 10, fontWeight: 800, padding: "2px 6px", borderRadius: 4, background: "#f59e0b", color: "#ffffff" }}>
                      #{idx + 1}
                    </span>
                    <div style={{ display: "flex", gap: 3 }}>
                      <button
                        type="button"
                        onClick={() => moveMostBooked(idx, -1)}
                        disabled={idx === 0}
                        style={{ border: "none", background: "transparent", cursor: idx === 0 ? "not-allowed" : "pointer", opacity: idx === 0 ? 0.3 : 1, padding: 2 }}
                        title="Move Earlier"
                      >
                        <ArrowUp size={14} color="#78350f" />
                      </button>
                      <button
                        type="button"
                        onClick={() => moveMostBooked(idx, 1)}
                        disabled={idx === pinnedServices.length - 1}
                        style={{ border: "none", background: "transparent", cursor: idx === pinnedServices.length - 1 ? "not-allowed" : "pointer", opacity: idx === pinnedServices.length - 1 ? 0.3 : 1, padding: 2 }}
                        title="Move Later"
                      >
                        <ArrowDown size={14} color="#78350f" />
                      </button>
                      <button
                        type="button"
                        onClick={() => toggleMostBooked(svc.id)}
                        style={{ border: "none", background: "transparent", cursor: "pointer", padding: 2, marginLeft: 4 }}
                        title="Remove from Most Booked"
                      >
                        <X size={15} color="#dc2626" />
                      </button>
                    </div>
                  </div>
                  <div style={{ fontWeight: 700, fontSize: 13, color: "#0f172a", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                    {svc.name}
                  </div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 4, fontSize: 11.5, color: "#78350f" }}>
                    <span style={{ fontWeight: 600 }}>₹{svc.price}</span>
                    <span style={{ fontSize: 10.5, color: "#92400e" }}>{svc.category}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Quick Service Picker */}
        <div style={{ borderTop: "1px solid #f1f5f9", paddingTop: 14 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 10, marginBottom: 12 }}>
            <span style={{ fontSize: 13, fontWeight: 700, color: "#334155" }}>
              Add / Toggle Services for &quot;Most Booked&quot;:
            </span>
            <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
              <div style={{ position: "relative" }}>
                <input
                  type="text"
                  value={mostBookedSearch}
                  onChange={(e) => setMostBookedSearch(e.target.value)}
                  placeholder="Search service..."
                  style={{
                    padding: "5px 10px 5px 28px",
                    borderRadius: 6,
                    border: "1px solid #cbd5e1",
                    fontSize: 12.5,
                    width: 170
                  }}
                />
                <Search size={14} color="#94a3b8" style={{ position: "absolute", left: 8, top: 8 }} />
              </div>
              <select
                value={mostBookedCatFilter}
                onChange={(e) => setMostBookedCatFilter(e.target.value)}
                style={{ padding: "5px 10px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 12.5 }}
              >
                <option value="ALL">All Categories</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </div>
          </div>

          <div style={{ display: "flex", flexWrap: "wrap", gap: 8, maxHeight: 200, overflowY: "auto", padding: "4px 2px" }}>
            {selectableServices.map((svc) => {
              const isPinned = mostBookedIds.map((x) => x.toLowerCase()).includes(svc.id.toLowerCase());
              return (
                <button
                  key={svc.id}
                  type="button"
                  onClick={() => toggleMostBooked(svc.id)}
                  style={{
                    display: "inline-flex",
                    alignItems: "center",
                    gap: 6,
                    padding: "6px 12px",
                    borderRadius: 20,
                    fontSize: 12,
                    fontWeight: isPinned ? 700 : 500,
                    border: isPinned ? "1.5px solid #f59e0b" : "1px solid #cbd5e1",
                    background: isPinned ? "#fef3c7" : "#f8fafc",
                    color: isPinned ? "#92400e" : "#334155",
                    cursor: "pointer",
                    transition: "all 0.15s ease"
                  }}
                >
                  {isPinned ? <Star size={13} fill="#f59e0b" color="#f59e0b" /> : <Plus size={13} color="#64748b" />}
                  <span>{svc.name}</span>
                  <span style={{ fontSize: 11, opacity: 0.8 }}>₹{svc.price}</span>
                </button>
              );
            })}
          </div>
        </div>
      </section>

      {/* Service Listings & Multi-Variant Options Editor */}
      <section className="admin-section">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16, flexWrap: "wrap", gap: 10 }}>
          <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: "#0f172a" }}>
            Service Offerings &amp; Multi-Variant Packages ({filteredServices.length})
          </h3>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span style={{ fontSize: 12.5, fontWeight: 600, color: "#475569" }}>Filter by Category:</span>
            <select
              value={selectedCatFilter}
              onChange={(e) => setSelectedCatFilter(e.target.value)}
              style={{ padding: "6px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
            >
              <option value="ALL">All Categories ({servicesList.length})</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.name} ({c.id})</option>
              ))}
            </select>
          </div>
        </div>

        {/* Services Table */}
        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Category</th>
                <th>Service Name</th>
                <th>Starting Price</th>
                <th>MRP / Original</th>
                <th>Duration</th>
                <th>Packages / Variants</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredServices.length === 0 ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: "center", padding: 24, color: "#64748b" }}>
                    No custom services found for this category. Click &quot;+ Add Service (with Packages)&quot; above to create one.
                  </td>
                </tr>
              ) : (
                filteredServices.map((svc) => {
                  const discount = svc.originalPrice && svc.originalPrice > svc.price
                    ? Math.round(((svc.originalPrice - svc.price) / svc.originalPrice) * 100)
                    : 0;
                  const optionCount = svc.options?.length || 0;
                  return (
                    <tr key={svc.id}>
                      <td><span style={{ fontWeight: 700, fontSize: 12, padding: "2px 6px", background: "#f1f5f9", borderRadius: 4 }}>{svc.category}</span></td>
                      <td>
                        <strong>{svc.name}</strong>
                        {svc.te && <div style={{ fontSize: 11.5, color: "#64748b" }}>{svc.te}</div>}
                        <div style={{ fontSize: 11, color: "#94a3b8", fontFamily: "monospace" }}>{svc.id}</div>
                      </td>
                      <td><span style={{ fontWeight: 700, color: "#0f172a" }}>₹{svc.price}</span></td>
                      <td>
                        {svc.originalPrice ? (
                          <span>
                            <span style={{ textDecoration: "line-through", color: "#94a3b8", fontSize: 12 }}>₹{svc.originalPrice}</span>
                            {discount > 0 && <span style={{ marginLeft: 6, fontSize: 11, fontWeight: 700, color: "#16a34a" }}>{discount}% OFF</span>}
                          </span>
                        ) : "–"}
                      </td>
                      <td>{svc.durationMin} mins</td>
                      <td>
                        {optionCount > 0 ? (
                          <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
                            <span style={{ fontWeight: 700, color: "#2563eb", fontSize: 12 }}>
                              📦 {optionCount} Packages / Options:
                            </span>
                            {svc.options?.slice(0, 2).map((opt) => (
                              <span key={opt.id} style={{ fontSize: 11, color: "#475569" }}>
                                • {opt.title}: <b>₹{opt.price}</b>
                              </span>
                            ))}
                            {optionCount > 2 && <span style={{ fontSize: 10.5, color: "#94a3b8" }}>+{optionCount - 2} more...</span>}
                          </div>
                        ) : (
                          <span style={{ fontSize: 12, color: "#94a3b8" }}>Single price only</span>
                        )}
                      </td>
                      <td>
                        <span style={{ fontSize: 11, fontWeight: 700, padding: "2px 8px", borderRadius: 12, background: svc.active !== false ? "#dcfce7" : "#fee2e2", color: svc.active !== false ? "#15803d" : "#dc2626" }}>
                          {svc.active !== false ? "ACTIVE" : "INACTIVE"}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
                          {(() => {
                            const isPinned = mostBookedIds.map((x) => x.toLowerCase()).includes(svc.id.toLowerCase());
                            return (
                              <button
                                type="button"
                                onClick={() => toggleMostBooked(svc.id)}
                                className="btn"
                                style={{
                                  fontSize: 11.5,
                                  padding: "3px 8px",
                                  display: "flex",
                                  alignItems: "center",
                                  gap: 4,
                                  background: isPinned ? "#fef3c7" : "#f8fafc",
                                  color: isPinned ? "#b45309" : "#64748b",
                                  border: isPinned ? "1px solid #fde68a" : "1px solid #e2e8f0",
                                  fontWeight: isPinned ? 700 : 500
                                }}
                                title={isPinned ? "Remove from Most Booked" : "Pin to Most Booked Carousel"}
                              >
                                <Star size={12} fill={isPinned ? "#f59e0b" : "none"} color={isPinned ? "#f59e0b" : "#94a3b8"} />
                                {isPinned ? "Pinned" : "Pin"}
                              </button>
                            );
                          })()}
                          <button
                            type="button"
                            onClick={() => { setEditingService(svc); setShowAddService(true); }}
                            className="btn"
                            style={{ fontSize: 12, padding: "3px 8px" }}
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDeleteService(svc.id)}
                            className="btn"
                            style={{ fontSize: 12, padding: "3px 8px", color: "#dc2626" }}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </section>

      {/* Service Editor with Multi-Variant Options Modal / Drawer */}
      {showAddService && editingService && (
        <ServiceEditorModal
          service={editingService}
          categories={categories}
          onSave={handleSaveService}
          onClose={() => { setEditingService(null); setShowAddService(false); }}
        />
      )}
    </div>
  );
}

// Modal for editing a service with its Multi-Variant options (packages)
function ServiceEditorModal({
  service,
  categories,
  onSave,
  onClose
}: {
  service: ServiceItemDef;
  categories: CategoryDef[];
  onSave: (svc: ServiceItemDef) => void;
  onClose: () => void;
}) {
  const [s, setS] = useState<ServiceItemDef>({ ...service });
  const [options, setOptions] = useState<ServiceOptionDef[]>(service.options || []);
  const [inclusionsText, setInclusionsText] = useState((service.inclusions || []).join("\n"));
  const [exclusionsText, setExclusionsText] = useState((service.exclusions || []).join("\n"));

  function addOption() {
    const optId = `opt_${Date.now().toString(36)}`;
    setOptions([
      ...options,
      {
        id: optId,
        title: `Package ${options.length + 1} (e.g. 2 Units / Deep Clean)`,
        price: s.price,
        originalPrice: s.originalPrice,
        durationMin: s.durationMin,
        description: "Full service with extended warranty"
      }
    ]);
  }

  function updateOption(idx: number, patch: Partial<ServiceOptionDef>) {
    setOptions(options.map((opt, i) => (i === idx ? { ...opt, ...patch } : opt)));
  }

  function removeOption(idx: number) {
    setOptions(options.filter((_, i) => i !== idx));
  }

  function handleSave() {
    if (!s.name.trim()) {
      alert("Service name is required");
      return;
    }
    const cleanInclusions = inclusionsText.split("\n").map((t) => t.trim()).filter(Boolean);
    const cleanExclusions = exclusionsText.split("\n").map((t) => t.trim()).filter(Boolean);

    onSave({
      ...s,
      name: s.name.trim(),
      te: s.te?.trim() || s.name.trim(),
      hi: s.hi?.trim() || s.name.trim(),
      price: Number(s.price) || 0,
      originalPrice: s.originalPrice ? Number(s.originalPrice) : undefined,
      durationMin: Number(s.durationMin) || 60,
      inclusions: cleanInclusions,
      exclusions: cleanExclusions,
      options: options.map((opt) => ({
        id: opt.id.trim(),
        title: opt.title.trim(),
        price: Number(opt.price) || 0,
        originalPrice: opt.originalPrice ? Number(opt.originalPrice) : undefined,
        durationMin: Number(opt.durationMin) || 60,
        description: opt.description?.trim() || ""
      }))
    });
  }

  return (
    <div style={{ position: "fixed", inset: 0, background: "rgba(15, 23, 42, 0.65)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 9999, padding: 16 }}>
      <div style={{ background: "#ffffff", borderRadius: 16, width: "100%", maxWidth: 840, maxHeight: "90vh", overflowY: "auto", padding: 24, boxShadow: "0 25px 50px -12px rgba(0, 0, 0, 0.25)" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: "1px solid #e2e8f0", paddingBottom: 14, marginBottom: 16 }}>
          <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "#0f172a" }}>
            {service.name ? `Edit Service: ${service.name}` : "Create New Service with Multi-Variant Options"}
          </h3>
          <button type="button" onClick={onClose} style={{ background: "none", border: "none", fontSize: 20, cursor: "pointer", color: "#64748b" }}>✕</button>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14 }}>
          <div>
            <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>Category *</label>
            <select
              value={s.category}
              onChange={(e) => setS({ ...s, category: e.target.value })}
              style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
            >
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.name} ({c.id})</option>
              ))}
            </select>
          </div>

          <div>
            <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>Service ID (lowercase key) *</label>
            <input
              type="text"
              value={s.id}
              onChange={(e) => setS({ ...s, id: e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, "_") })}
              placeholder="e.g. ac_foam_jet_clean"
              style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontFamily: "monospace" }}
            />
          </div>

          <div style={{ gridColumn: "span 2" }}>
            <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>Service Name (English) *</label>
            <input
              type="text"
              value={s.name}
              onChange={(e) => setS({ ...s, name: e.target.value })}
              placeholder="e.g. AC Deep Foam Jet Cleaning"
              style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>Telugu Label (తెలుగు)</label>
            <input
              type="text"
              value={s.te || ""}
              onChange={(e) => setS({ ...s, te: e.target.value })}
              placeholder="e.g. ఏసీ డీప్ ఫోమ్ క్లీనింగ్"
              style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>Hindi Label (हिंदी)</label>
            <input
              type="text"
              value={s.hi || ""}
              onChange={(e) => setS({ ...s, hi: e.target.value })}
              placeholder="e.g. एसी डीप फोम जेट क्लीनिंग"
              style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>Base Offer Price (₹) *</label>
            <input
              type="number"
              value={String(s.price)}
              onChange={(e) => setS({ ...s, price: Number(e.target.value) || 0 })}
              style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>Original MRP (₹) (For Strikethrough &amp; % OFF)</label>
            <input
              type="number"
              value={s.originalPrice ? String(s.originalPrice) : ""}
              onChange={(e) => setS({ ...s, originalPrice: Number(e.target.value) || undefined })}
              placeholder="e.g. 699"
              style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>Duration (Minutes)</label>
            <input
              type="number"
              value={String(s.durationMin)}
              onChange={(e) => setS({ ...s, durationMin: Number(e.target.value) || 60 })}
              style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>Service Image URL</label>
            <input
              type="text"
              value={s.imageUrl || ""}
              onChange={(e) => setS({ ...s, imageUrl: e.target.value })}
              placeholder="https://firebasestorage.../service.webp"
              style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
            />
          </div>

          <div style={{ gridColumn: "span 2" }}>
            <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>Short Summary / Description</label>
            <input
              type="text"
              value={s.includes}
              onChange={(e) => setS({ ...s, includes: e.target.value })}
              placeholder="e.g. 2x power jet cleaning for indoor and outdoor coils + filter sanitization"
              style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
            />
          </div>

          {/* Inclusions & Exclusions */}
          <div>
            <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#15803d", marginBottom: 4 }}>What&apos;s Included (1 per line)</label>
            <textarea
              rows={3}
              value={inclusionsText}
              onChange={(e) => setInclusionsText(e.target.value)}
              placeholder="High pressure indoor wash&#10;Outdoor condenser cleaning&#10;Gas leak detection test"
              style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #bbf7d0", fontSize: 13 }}
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#b91c1c", marginBottom: 4 }}>What&apos;s Excluded (1 per line)</label>
            <textarea
              rows={3}
              value={exclusionsText}
              onChange={(e) => setExclusionsText(e.target.value)}
              placeholder="Spare parts replacement&#10;Gas refill (charged extra per kg)"
              style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #fecaca", fontSize: 13 }}
            />
          </div>
        </div>

        {/* Multi-Variant Options / Packages Section */}
        <div style={{ marginTop: 24, padding: 16, background: "#f8fafc", borderRadius: 12, border: "1px solid #e2e8f0" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
            <div>
              <h4 style={{ margin: 0, fontSize: 15, fontWeight: 700, color: "#0f172a" }}>
                Multi-Variant Options &amp; Packages ({options.length})
              </h4>
              <p style={{ margin: "2px 0 0 0", fontSize: 12, color: "#64748b" }}>
                Allows customers to pick different variants (e.g., 1 AC unit vs 2 AC units, or Express vs Deep Clean)
              </p>
            </div>
            <button
              type="button"
              onClick={addOption}
              className="btn"
              style={{ background: "#2563eb", color: "#fff", fontWeight: 600, fontSize: 12.5 }}
            >
              + Add Variant Package
            </button>
          </div>

          {options.length === 0 ? (
            <div style={{ textAlign: "center", padding: 16, color: "#94a3b8", fontSize: 13 }}>
              No variant packages added. This service will be booked at its base price (₹{s.price}).
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {options.map((opt, idx) => (
                <div key={opt.id || idx} style={{ background: "#ffffff", border: "1px solid #cbd5e1", borderRadius: 8, padding: 12 }}>
                  <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr 1fr 1fr auto", gap: 8, alignItems: "center" }}>
                    <div>
                      <label style={{ display: "block", fontSize: 11, color: "#64748b", fontWeight: 600 }}>Option Title *</label>
                      <input
                        type="text"
                        value={opt.title}
                        onChange={(e) => updateOption(idx, { title: e.target.value })}
                        placeholder="e.g. 2 Split AC Units"
                        style={{ width: "100%", padding: "6px 8px", borderRadius: 4, border: "1px solid #cbd5e1", fontSize: 13 }}
                      />
                    </div>
                    <div>
                      <label style={{ display: "block", fontSize: 11, color: "#64748b", fontWeight: 600 }}>Offer Price (₹)</label>
                      <input
                        type="number"
                        value={String(opt.price)}
                        onChange={(e) => updateOption(idx, { price: Number(e.target.value) || 0 })}
                        style={{ width: "100%", padding: "6px 8px", borderRadius: 4, border: "1px solid #cbd5e1", fontSize: 13 }}
                      />
                    </div>
                    <div>
                      <label style={{ display: "block", fontSize: 11, color: "#64748b", fontWeight: 600 }}>MRP (₹)</label>
                      <input
                        type="number"
                        value={opt.originalPrice ? String(opt.originalPrice) : ""}
                        onChange={(e) => updateOption(idx, { originalPrice: Number(e.target.value) || undefined })}
                        placeholder="e.g. 1199"
                        style={{ width: "100%", padding: "6px 8px", borderRadius: 4, border: "1px solid #cbd5e1", fontSize: 13 }}
                      />
                    </div>
                    <div>
                      <label style={{ display: "block", fontSize: 11, color: "#64748b", fontWeight: 600 }}>Mins</label>
                      <input
                        type="number"
                        value={String(opt.durationMin || 60)}
                        onChange={(e) => updateOption(idx, { durationMin: Number(e.target.value) || 60 })}
                        style={{ width: "100%", padding: "6px 8px", borderRadius: 4, border: "1px solid #cbd5e1", fontSize: 13 }}
                      />
                    </div>
                    <div>
                      <label style={{ display: "block", fontSize: 11, color: "transparent" }}>X</label>
                      <button
                        type="button"
                        onClick={() => removeOption(idx)}
                        style={{ background: "#fee2e2", border: "none", color: "#dc2626", borderRadius: 4, padding: "7px 10px", cursor: "pointer", fontWeight: 700 }}
                        title="Remove Option"
                      >
                        ✕
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div style={{ marginTop: 24, display: "flex", justifyContent: "flex-end", gap: 10 }}>
          <button type="button" onClick={onClose} className="btn" style={{ padding: "8px 16px" }}>
            Cancel
          </button>
          <button type="button" onClick={handleSave} className="btn btn-approve" style={{ padding: "8px 24px", fontWeight: 700 }}>
            Save Service &amp; Packages
          </button>
        </div>
      </div>
    </div>
  );
}
