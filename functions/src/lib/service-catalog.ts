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

export type CategoryId = "AC" | "CLEANING" | "ELECTRICIAN" | "PLUMBER" | "APPLIANCE";

export interface ServiceCategory {
  id: CategoryId;
  name: string;
  te: string;
  hi: string;
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
  categories: ServiceCategory[];
  services: ServiceItem[];
}

export const CATEGORIES: ServiceCategory[] = [
  { id: "AC", name: "AC Service & Repair", te: "AC సర్వీస్ & రిపేర్", hi: "AC सर्विस और रिपेयर" },
  { id: "CLEANING", name: "Home Cleaning", te: "ఇంటి క్లీనింగ్", hi: "घर की सफ़ाई" },
  { id: "ELECTRICIAN", name: "Electrician", te: "ఎలక్ట్రీషియన్", hi: "इलेक्ट्रीशियन" },
  { id: "PLUMBER", name: "Plumber", te: "ప్లంబర్", hi: "प्लंबर" },
  { id: "APPLIANCE", name: "Appliance & RO Repair", te: "అప్లయెన్స్ & RO రిపేర్", hi: "अप्लायंस और RO रिपेयर" },
];

const s = (
  id: string, category: CategoryId, name: string, te: string, hi: string, price: number, durationMin: number,
  includes: string, inspection = false,
): ServiceItem => ({ id, category, name, te, hi, price, durationMin, includes, ...(inspection ? { inspection } : {}) });

export const DEFAULT_SERVICES: ServiceItem[] = [
  s("ac_service", "AC", "AC service (foam-jet)", "AC సర్వీస్ (ఫోమ్-జెట్)", "AC सर्विस (फोम-जेट)", 449, 60,
    "Filter, coil and drain cleaning, cooling check. Split or window, one AC."),
  s("ac_repair_visit", "AC", "AC not cooling / repair visit", "AC కూలింగ్ లేదు / రిపేర్ విజిట్", "AC ठंडा नहीं / रिपेयर विज़िट", 199, 45,
    "Technician finds the problem and tells the repair price before starting.", true),
  s("ac_gas", "AC", "AC gas refill", "AC గ్యాస్ రీఫిల్", "AC गैस रिफिल", 2199, 90,
    "Leak check and gas top-up for one AC."),
  s("ac_install", "AC", "Split AC installation", "స్ప్లిట్ AC ఇన్‌స్టలేషన్", "स्प्लिट AC इंस्टॉलेशन", 1199, 120,
    "Indoor + outdoor unit fitting with existing pipe. Extra pipe/stand charged separately."),
  s("ac_uninstall", "AC", "AC uninstallation", "AC అన్‌ఇన్‌స్టలేషన్", "AC अनइंस्टॉलेशन", 599, 60,
    "Safe removal with gas pump-down."),

  s("clean_bathroom", "CLEANING", "Bathroom deep cleaning", "బాత్రూమ్ డీప్ క్లీనింగ్", "बाथरूम डीप क्लीनिंग", 399, 60,
    "One bathroom: tiles, taps, toilet and stain removal with machine."),
  s("clean_kitchen", "CLEANING", "Kitchen deep cleaning", "కిచెన్ డీప్ క్లీనింగ్", "किचन डीप क्लीनिंग", 1199, 150,
    "Slab, tiles, sink, chimney outside and cabinets outside."),
  s("clean_1bhk", "CLEANING", "Full home deep cleaning (1BHK)", "ఇల్లు మొత్తం డీప్ క్లీనింగ్ (1BHK)", "पूरे घर की डीप क्लीनिंग (1BHK)", 2999, 300,
    "All rooms, kitchen and bathroom, floors scrubbed, fans and windows."),
  s("clean_2bhk", "CLEANING", "Full home deep cleaning (2BHK)", "ఇల్లు మొత్తం డీప్ క్లీనింగ్ (2BHK)", "पूरे घर की डीप क्लीनिंग (2BHK)", 3999, 420,
    "All rooms, kitchen and bathrooms, floors scrubbed, fans and windows."),
  s("clean_sofa", "CLEANING", "Sofa cleaning (5 seats)", "సోఫా క్లీనింగ్ (5 సీట్లు)", "सोफ़ा सफ़ाई (5 सीट)", 699, 90,
    "Shampoo and vacuum cleaning of fabric sofa."),

  s("elec_fan", "ELECTRICIAN", "Fan installation / repair", "ఫ్యాన్ ఫిట్టింగ్ / రిపేర్", "पंखा फिटिंग / रिपेयर", 149, 30,
    "One ceiling or wall fan. Parts extra."),
  s("elec_switch", "ELECTRICIAN", "Switch / socket repair", "స్విచ్ / సాకెట్ రిపేర్", "स्विच / सॉकेट रिपेयर", 99, 20,
    "Up to 2 switches or sockets. Parts extra."),
  s("elec_light", "ELECTRICIAN", "Light / tube fitting", "లైట్ / ట్యూబ్ ఫిట్టింగ్", "लाइट / ट्यूब फिटिंग", 99, 20,
    "Up to 2 lights. Parts extra."),
  s("elec_mcb", "ELECTRICIAN", "MCB / fuse / wiring fault", "MCB / ఫ్యూజ్ / వైరింగ్ ఫాల్ట్", "MCB / फ्यूज़ / वायरिंग फॉल्ट", 199, 45,
    "Find and fix a tripping MCB or fuse. Parts extra."),
  s("elec_inverter", "ELECTRICIAN", "Inverter / stabiliser installation", "ఇన్వర్టర్ / స్టెబిలైజర్ ఫిట్టింగ్", "इन्वर्टर / स्टेबलाइज़र फिटिंग", 349, 60,
    "Installation and wiring of one unit."),
  s("elec_visit", "ELECTRICIAN", "Electrician visit (other work)", "ఎలక్ట్రీషియన్ విజిట్ (ఇతర పని)", "इलेक्ट्रीशियन विज़िट (अन्य काम)", 99, 30,
    "Electrician checks the problem and tells the price before starting.", true),

  s("plumb_tap", "PLUMBER", "Tap / mixer fitting", "ట్యాప్ / మిక్సర్ ఫిట్టింగ్", "नल / मिक्सर फिटिंग", 129, 30,
    "One tap or mixer. Parts extra."),
  s("plumb_leak", "PLUMBER", "Leakage repair", "లీకేజ్ రిపేర్", "लीकेज रिपेयर", 199, 45,
    "Pipe, tap or tank leak. Parts extra."),
  s("plumb_block", "PLUMBER", "Drain / toilet blockage", "డ్రెయిన్ / టాయిలెట్ బ్లాకేజ్", "नाली / टॉयलेट ब्लॉकेज", 299, 45,
    "Clear one blocked drain, sink or toilet."),
  s("plumb_tank", "PLUMBER", "Water tank cleaning (up to 1000 L)", "వాటర్ ట్యాంక్ క్లీనింగ్ (1000 L వరకు)", "पानी टंकी सफ़ाई (1000 L तक)", 699, 90,
    "Drain, scrub and disinfect one overhead or sump tank."),
  s("plumb_motor", "PLUMBER", "Water motor repair / fitting", "వాటర్ మోటార్ రిపేర్ / ఫిట్టింగ్", "पानी मोटर रिपेयर / फिटिंग", 349, 60,
    "Motor check, starter or fitting work. Parts extra."),

  s("ro_service", "APPLIANCE", "RO water purifier service", "RO వాటర్ ప్యూరిఫైయర్ సర్వీస్", "RO वाटर प्यूरीफायर सर्विस", 399, 45,
    "Cleaning, TDS check and pre-filter change. Other filters extra."),
  s("ro_install", "APPLIANCE", "RO installation / uninstallation", "RO ఇన్‌స్టలేషన్ / అన్‌ఇన్‌స్టలేషన్", "RO इंस्टॉलेशन / अनइंस्टॉलेशन", 349, 45,
    "Fitting or removal of one purifier."),
  s("wm_repair", "APPLIANCE", "Washing machine repair visit", "వాషింగ్ మెషిన్ రిపేర్ విజిట్", "वॉशिंग मशीन रिपेयर विज़िट", 199, 45,
    "Technician finds the problem and tells the repair price before starting.", true),
  s("fridge_repair", "APPLIANCE", "Fridge repair visit", "ఫ్రిజ్ రిపేర్ విజిట్", "फ्रिज रिपेयर विज़िट", 199, 45,
    "Technician finds the problem and tells the repair price before starting.", true),
  s("geyser", "APPLIANCE", "Geyser installation / repair", "గీజర్ ఫిట్టింగ్ / రిపేర్", "गीज़र फिटिंग / रिपेयर", 349, 45,
    "One geyser. Parts extra."),
];

export const DEFAULT_CONFIG: ServicesConfig = {
  bookingFee: 19,
  inspectionFee: 49,
  commissionPct: 10,
  upiId: "",
  upiName: "DutyPe",
  minTopup: 200,
  city: "Khammam",
  districtIds: [509],
  cityLat: 17.2473,
  cityLng: 80.1514,
  serviceRadiusKm: 25,
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
    categories: CATEGORIES,
    services,
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
