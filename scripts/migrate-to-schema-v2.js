/**
 * One-time migration of the live DutyPe Firestore data to the new schema (FirestoreSchema.kt /
 * functions/src/schema.ts). Keeps every user, profile, job, application, wallet balance and
 * referral; rewrites each document whole (old fields disappear); drops collections of removed
 * features. Take a backup first (scratch backup script) — this script does not.
 *
 *   node scripts/migrate-to-schema-v2.js            dry run: reads everything, prints what it would do
 *   node scripts/migrate-to-schema-v2.js --apply    writes
 *
 * Needs serviceAccountKey.json.json (git-ignored) in the repo root.
 * After it runs, the deployed triggers build worker_cards, employer_cards and job place/keywords.
 */
const path = require("path");
const admin = require(path.join(__dirname, "../functions/node_modules/firebase-admin"));
const { encodeGeohash } = require(path.join(__dirname, "../functions/lib/lib/geo.js"));

const APPLY = process.argv.includes("--apply");
admin.initializeApp({ credential: admin.credential.cert(require(path.join(__dirname, "../serviceAccountKey.json.json"))) });
const db = admin.firestore();
const { Timestamp, FieldValue } = admin.firestore;
const NOW = Date.now();
const DAY = 864e5;

// ───────────────────────────── helpers ─────────────────────────────

const str = (v, max = 200) => (typeof v === "string" ? v : v == null ? "" : String(v)).trim().slice(0, max);
const num = (v) => (typeof v === "number" && Number.isFinite(v) ? v : Number(v)) || 0;
function ts(v, fallbackMs) {
  if (v instanceof Timestamp) return v;
  if (typeof v === "number" && v > 0) return Timestamp.fromMillis(v < 1e12 ? v * 1000 : v);
  if (typeof v === "string" && !Number.isNaN(Date.parse(v))) return Timestamp.fromMillis(Date.parse(v));
  return fallbackMs ? Timestamp.fromMillis(fallbackMs) : null;
}
function e164(raw) {
  const d = String(raw || "").replace(/\D/g, "");
  if (/^[6-9]\d{9}$/.test(d)) return `+91${d}`;
  if (/^91[6-9]\d{9}$/.test(d)) return `+${d}`;
  if (/^0[6-9]\d{9}$/.test(d)) return `+91${d.slice(1)}`;
  return null;
}
const mobile10 = (raw) => (e164(raw) || "").slice(3);
const validPoint = (lat, lng) => Number.isFinite(lat) && Number.isFinite(lng) && Math.abs(lat) <= 90 && Math.abs(lng) <= 180 && !(lat === 0 && lng === 0);
function point(...candidates) {
  for (const c of candidates) {
    if (!c) continue;
    const lat = num(c.lat ?? c.latitude);
    const lng = num(c.lng ?? c.longitude);
    if (validPoint(lat, lng)) return { lat, lng };
  }
  return null;
}
const areaOf = (address) => str(String(address || "").split(",").map((p) => p.trim()).find((p) => p && !/^\d+$/.test(p)) || "", 60);

// Category keys and the app's keyword rules (app/.../utils/JobCategoryResolver.kt), same order.
const CATEGORY_KEYS = ["COOK", "MAID", "DRIVER", "HELPER", "SECURITY", "GARDENER", "CARETAKER", "DELIVERY", "WAITER",
  "ELECTRICIAN", "PLUMBER", "PAINTER", "CARPENTER", "RECEPTIONIST", "CASHIER", "PACKER", "SALES", "TELECALLER", "TEACHER",
  "OFFICE_STAFF", "CUSTOMER_SUPPORT", "FIELD_EXECUTIVE", "MARKETING", "FINANCE", "HEALTHCARE", "BEAUTICIAN", "TAILOR",
  "MECHANIC", "DATA_ENTRY", "LEGAL", "OTHER"];
const ALIASES = { SHOP_HELPER: "HELPER", HOUSEKEEPING: "MAID", KITCHEN: "COOK", EVENTS: "WAITER", CONSTRUCTION: "HELPER",
  FIELD_WORK: "FIELD_EXECUTIVE", ADMIN: "OFFICE_STAFF", OFFICE_ADMIN: "OFFICE_STAFF", OFFICE_BOY: "HELPER",
  CALL_CENTER: "CUSTOMER_SUPPORT", CUSTOMER_SERVICE: "CUSTOMER_SUPPORT", NURSE: "HEALTHCARE", MEDICAL: "HEALTHCARE",
  INSURANCE: "FINANCE", BANKING: "FINANCE", HOTEL_RESTAURANT: "WAITER", DELIVERY_PARTNER: "DELIVERY",
  HOUSE_CLEANING: "MAID", DEEP_CLEANING: "MAID", PACKING_HELPER: "PACKER", SERVING_STAFF: "WAITER" };
const RULES = [
  ["DELIVERY", ["delivery", "courier", "swiggy", "zomato", "dunzo", "parcel", "last mile", "rider", "bike rider"]],
  ["DRIVER", ["driver", "driving", "chauffeur", "cab", "taxi", "ola", "uber", "rapido", "truck"]],
  ["COOK", ["cook", "chef", "kitchen", "tandoor", "chapati", "biryani", "catering"]],
  ["MAID", ["maid", "housekeep", "domestic", "house cleaner", "cleaner", "cleaning", "sweeper", "janitor", "house help"]],
  ["SECURITY", ["security", "guard", "watchman", "bouncer"]],
  ["GARDENER", ["gardener", "garden", "landscap", "lawn", "horticult"]],
  ["CARETAKER", ["caretaker", "care taker", "nanny", "babysitter", "childcare", "elder care", "elderly care", "caregiver", "ayah"]],
  ["WAITER", ["waiter", "waitress", "steward", "bartender", "server ", "banquet", "cafe staff", "restaurant", "hotel"]],
  ["ELECTRICIAN", ["electric", "wiring"]],
  ["PLUMBER", ["plumb", "pipe fit"]],
  ["PAINTER", ["painter", "painting"]],
  ["CARPENTER", ["carpenter", "carpentry", "woodwork"]],
  ["RECEPTIONIST", ["receptionist", "front desk", "front office"]],
  ["CASHIER", ["cashier", "billing", "checkout", "counter"]],
  ["PACKER", ["packer", "packing", "packaging", "warehouse", "loader", "loading", "unloading", "picker", "logistics"]],
  ["SALES", ["sales", "retail", "shop sales", "store sales", "sales associate", "sales executive"]],
  ["TELECALLER", ["telecaller", "tele caller", "telesales", "tele sales", "calling", "call center", "bpo", "voice process"]],
  ["TEACHER", ["teacher", "tutor", "teaching", "trainer", "instructor", "pre primary", "pre-primary", "primary school"]],
  ["OFFICE_STAFF", ["office admin", "office staff", "admin", "back office", "office assistant", "clerk", "office boy", "peon"]],
  ["CUSTOMER_SUPPORT", ["customer support", "customer service", "support executive", "process associate", "service associate"]],
  ["FIELD_EXECUTIVE", ["field executive", "field officer", "field work", "field sales", "field service", "promoter", "brand promoter"]],
  ["MARKETING", ["marketing", "business development", "bde", "bd executive", "relationship manager"]],
  ["FINANCE", ["loan", "finance", "banking", "collection", "insurance", "credit card", "home loan"]],
  ["HEALTHCARE", ["nurse", "nursing", "medical", "healthcare", "patient care", "ward boy", "pharmacy"]],
  ["BEAUTICIAN", ["beautician", "beauty", "salon", "makeup", "hair", "spa"]],
  ["TAILOR", ["tailor", "tailoring", "stitching", "sewing", "garment", "alteration"]],
  ["MECHANIC", ["mechanic", "auto mechanic", "vehicle repair", "garage", "technician"]],
  ["DATA_ENTRY", ["data entry", "computer operator", "typing", "excel", "mis executive"]],
  ["LEGAL", ["advocate", "legal", "lawyer", "law clerk"]],
  ["HELPER", ["helper", "assistant", "labour", "labor", "construction", "mason", "site work"]],
];
function categoryKey(explicit, ...texts) {
  const token = String(explicit || "").toUpperCase().replace(/[^A-Z0-9]+/g, "_").replace(/^_|_$/g, "");
  if (token && token !== "OTHER") {
    if (CATEGORY_KEYS.includes(token)) return token;
    if (ALIASES[token]) return ALIASES[token];
  }
  const text = [explicit, ...texts].map((t) => String(t || "")).join(" ").toLowerCase();
  const hit = RULES.find(([, words]) => words.some((w) => text.includes(w)));
  return hit ? hit[0] : "OTHER";
}
const skillKeys = (skills) => [...new Set((Array.isArray(skills) ? skills : []).map((s) => categoryKey(s, s)).filter((k) => k !== "OTHER"))].slice(0, 10);

function experienceYears(v) {
  const s = String(v || "").toLowerCase();
  if (!s || s.includes("less than") || s.includes("fresher") || s.includes("no experience")) return 0;
  if (s.includes("more than 5") || s.includes("5+")) return 5;
  const n = s.match(/\d+/);
  return n ? Math.min(60, Number(n[0])) : 0;
}
const gender = (v) => { const s = String(v || "").trim().toLowerCase(); return s === "male" ? "Male" : s === "female" ? "Female" : ""; };
const jobGender = (v) => { const s = String(v || "").trim().toLowerCase(); return s === "male" ? "MALE" : s === "female" ? "FEMALE" : "ANY"; };

/** Old free-text salary → { payAmount, payType, lossy } (lossy: the original text is kept in the description). */
function pay(salary, salaryType) {
  const text = str(salary, 80);
  const nums = (text.replace(/,/g, "").match(/\d+(?:\.\d+)?/g) || []).map(Number).filter((n) => n > 0);
  let type = String(salaryType || "").toUpperCase();
  if (/hour/i.test(text)) type = "HOURLY";
  let amount = nums.length ? Math.round(Math.min(...nums)) : 0;
  let lossy = nums.length !== 1 || /\+|[a-z]/i.test(text.replace(/\/?(hour|month|day)/i, ""));
  if (type === "WEEKLY") { amount = amount * 4; type = "MONTHLY"; lossy = true; }
  if (type === "TASK") { type = "DAILY"; lossy = true; }
  if (!["DAILY", "MONTHLY", "HOURLY"].includes(type)) type = "MONTHLY";
  if (!amount) return { payAmount: 0, payType: "NEGOTIABLE", lossy: !!text };
  return { payAmount: Math.min(amount, 10_000_000), payType: type, lossy };
}
const employmentType = (v) => {
  const s = String(v || "").toUpperCase();
  if (s.startsWith("PART")) return "PART_TIME";
  if (s.includes("TEMPORARY") || s.includes("DAILY")) return "DAILY";
  return "FULL_TIME";
};
const jobStatus = (meta) => {
  if (meta.isFilled === true) return "filled";
  const s = String(meta.status || "open").toLowerCase();
  return { open: "open", expired: "expired", filled: "filled", closed: "closed", paused: "closed" }[s] || "open";
};
const APP_STATUS = { completed: "completed", accepted: "hired", hired: "hired", withdrawn: "withdrawn", applied: "applied",
  under_review: "applied", pending: "applied", shortlisted: "applied", rejected: "rejected" };

// ───────────────────────────── plan ─────────────────────────────

const writes = [];   // [ref, data]
const deletes = [];  // refs
const stats = {};
const bump = (k, n = 1) => { stats[k] = (stats[k] || 0) + n; };
const set = (ref, data) => writes.push([ref, data]);
const all = async (c) => (await db.collection(c).get()).docs;
const byId = (docs) => new Map(docs.map((d) => [d.id, d.data()]));

(async () => {
  console.log(APPLY ? "APPLY: writing changes" : "DRY RUN: nothing is written");
  const [users, phoneRoles, workers, employers] = await Promise.all(["users", "phoneRoles", "worker_profiles", "employer_profiles"].map(all));
  const userById = byId(users);
  const workerById = byId(workers);

  // phoneRoles: E.164 ids, {uid, role} only.
  const roleOf = new Map();
  const phoneOf = new Map();
  const finalPhoneIds = new Map();
  for (const d of phoneRoles) {
    const x = d.data();
    const id = e164(d.id) || e164(x.phoneNumber);
    const role = String(x.role || "").toUpperCase();
    if (!id || !x.uid || !["WORKER", "EMPLOYER"].includes(role)) { bump("phoneRoles.dropped (no valid phone/uid/role)"); deletes.push(d.ref); continue; }
    if (finalPhoneIds.has(id) && finalPhoneIds.get(id) !== x.uid) { bump("phoneRoles.conflict skipped"); continue; }
    finalPhoneIds.set(id, x.uid);
    roleOf.set(x.uid, role);
    phoneOf.set(x.uid, id);
    set(db.collection("phoneRoles").doc(id), { uid: x.uid, role });
    if (id !== d.id) { deletes.push(d.ref); bump("phoneRoles.re-keyed to +91"); }
    bump(`phoneRoles.${role}`);
  }
  // Profiles that lost their phone registration: register them again from the profile, the old
  // users doc or, failing those, the Firebase login account's phone number.
  const authPhone = async (uid) => { try { return (await admin.auth().getUser(uid)).phoneNumber || ""; } catch { return ""; } };
  for (const [docs, role] of [[employers, "EMPLOYER"], [workers, "WORKER"]]) {
    for (const d of docs) {
      if (roleOf.has(d.id)) continue;
      const id = e164(d.get("phone")) || e164(userById.get(d.id)?.phone) || e164(userById.get(d.id)?.phoneNumber) ||
        e164(await authPhone(d.id));
      if (!id || finalPhoneIds.has(id)) { bump(`orphan ${role.toLowerCase()} profile left unregistered`); continue; }
      finalPhoneIds.set(id, d.id); roleOf.set(d.id, role); phoneOf.set(d.id, id);
      set(db.collection("phoneRoles").doc(id), { uid: d.id, role });
      bump("phoneRoles.restored for orphan profile");
    }
  }

  // worker_profiles
  const workerSkill = new Map();
  const workerPhoto = new Map();
  // Everyone registered as a worker, plus worker profiles of people now registered as employers
  // (kept in the new shape so no data is lost; the app shows them only their registered role).
  const workerUids = [...new Set([...[...roleOf].filter(([, r]) => r === "WORKER").map(([u]) => u), ...workers.map((d) => d.id)])];
  for (const uid of workerUids) {
    const w = workerById.get(uid) || {};
    const u = userById.get(uid) || {};
    if (!workerById.has(uid)) bump("worker_profiles.created from users");
    const loc = point(w.location, u.location, { lat: u.latitude, lng: u.longitude });
    const skills = skillKeys(w.skills);
    const address = str(w.address || u.address, 300);
    const doc = {
      name: str(w.fullName || w.name || u.fullName || u.name, 80),
      phone: phoneOf.get(uid) || e164(w.phone) || "",
      photoUrl: str(w.profileImageUrl || u.profileImageUrl, 1000),
      gender: gender(w.gender),
      dateOfBirth: str(w.dateOfBirth, 20),
      education: str(w.educationQualification, 60),
      experienceYears: experienceYears(w.experience),
      skills,
      bio: str(w.bio, 1000),
      address,
      area: areaOf(address),
      ...(loc ? { lat: loc.lat, lng: loc.lng, geohash: encodeGeohash(loc.lat, loc.lng) } : {}),
      available: skills.length > 0,
      blocked: false,
      createdAt: ts(w.createdAt || u.createdAt, NOW),
      updatedAt: Timestamp.fromMillis(NOW),
    };
    for (const k of Object.keys(doc)) if (doc[k] === "") delete doc[k];
    set(db.collection("worker_profiles").doc(uid), doc);
    workerSkill.set(uid, skills[0] || "");
    workerPhoto.set(uid, doc.photoUrl || "");
    bump("worker_profiles");
    if (!skills.length) bump("worker_profiles without a known skill (Offline until they add one)");
    if (!loc) bump("worker_profiles without a location");
  }
  for (const d of workers) if (roleOf.get(d.id) !== "WORKER") bump("worker_profiles kept for people registered as employers / unregistered");

  // employer_profiles (+ unlocks, work_locations)
  for (const d of employers) {
    if (roleOf.get(d.id) !== "EMPLOYER") bump("employer_profiles kept for people registered as workers / unregistered");
    const e = d.data();
    const u = userById.get(d.id) || {};
    const owner = str(e.fullName || u.fullName || u.name, 80);
    const company = str(e.companyName || u.companyName, 100);
    const type = ["INDIVIDUAL", "COMPANY"].includes(e.employerType) ? e.employerType :
      company && company.toLowerCase() !== owner.toLowerCase() ? "COMPANY" : "INDIVIDUAL";
    const loc = point(e.businessLocation, { lat: u.businessLatitude, lng: u.businessLongitude }, u.location);
    const address = str(e.businessAddress || u.businessAddress || u.address, 500);
    const old = e.subscription || {};
    const campaign = e.unlimitedJobPostingGranted === true;
    const expiresMs = ts(old.expiryDate)?.toMillis() || 0;
    const paidActive = String(old.status || "").toUpperCase() === "ACTIVE" && (!expiresMs || expiresMs > NOW);
    const subscription = paidActive || !campaign ? {
      planId: str(old.planId, 40),
      status: String(old.status || "NONE").toUpperCase(),
      ...(ts(old.startDate) ? { startAt: ts(old.startDate) } : {}),
      ...(expiresMs ? { expiresAt: Timestamp.fromMillis(expiresMs) } : {}),
      credits: { normal: num(old.credits?.normal), instant: num(old.credits?.instant) },
    } : { planId: "UNLIMITED_CAMPAIGN", status: "ACTIVE", startAt: ts(e.unlimitedJobPostingGrantedAt, NOW), credits: { normal: 0, instant: 0 } };
    const doc = {
      employerType: type,
      ownerName: owner || company,
      businessName: type === "COMPANY" ? company : "",
      businessType: str(e.industry, 40),
      gstin: "",
      phone: phoneOf.get(d.id) || e164(e.phone) || "",
      photoUrl: str(e.profileImageUrl || u.profileImageUrl, 1000),
      address,
      area: areaOf(address),
      ...(loc ? { lat: loc.lat, lng: loc.lng, geohash: encodeGeohash(loc.lat, loc.lng) } : {}),
      subscription,
      freeUrgentPostsUsed: num(e.freeUrgentJobsPosted),
      verified: e.isVerified === true,
      rating: num(e.rating),
      ratingCount: num(e.totalRatings),
      totalHires: num(e.totalHires),
      blocked: false,
      createdAt: ts(e.createdAt || u.createdAt, NOW),
      updatedAt: Timestamp.fromMillis(NOW),
    };
    for (const k of ["businessName", "businessType", "gstin", "phone", "photoUrl", "address", "area"]) if (doc[k] === "") delete doc[k];
    set(d.ref, doc);
    bump(`employer_profiles.${type}`);
    if (subscription.planId === "UNLIMITED_CAMPAIGN") bump("employer_profiles with unlimited campaign");
    for (const w of new Set(Array.isArray(e.unlockedContacts) ? e.unlockedContacts : [])) {
      set(d.ref.collection("unlocks").doc(String(w)), { jobId: "", createdAt: Timestamp.fromMillis(NOW) });
      bump("employer unlocks");
    }
    for (const w of (await d.ref.collection("work_locations").get()).docs) {
      const x = w.data();
      const p = point(x);
      if (!p) { deletes.push(w.ref); bump("work_locations dropped (no point)"); continue; }
      set(w.ref, { label: str(x.label, 80) || "Work location", address: str(x.address, 500), lat: p.lat, lng: p.lng });
      bump("work_locations");
    }
  }

  // jobs: card + details + contact
  const [metas, details, applications] = await Promise.all(["jobmetadata", "job_details", "applications"].map(all));
  const detailById = byId(details);
  const appCount = new Map();
  for (const a of applications) appCount.set(a.get("jobId"), (appCount.get(a.get("jobId")) || 0) + 1);
  const jobIds = new Set();
  const categories = {};
  for (const d of metas) {
    const m = d.data();
    const det = detailById.get(d.id) || {};
    const employerId = str(m.employerId || det.employerId, 128);
    if (!employerId) { deletes.push(d.ref); bump("jobs dropped (no employer)"); continue; }
    jobIds.add(d.id);
    const title = str(m.title || det.title, 80) || "Job";
    const loc = point(m.location, det.location);
    const p = pay(m.salary ?? det.salary, m.salaryType ?? det.salaryType);
    const category = categoryKey(m.category ?? det.category, title);
    categories[category] = (categories[category] || 0) + 1;
    const created = ts(m.createdAt || det.createdAt, NOW);
    const card = {
      employerId,
      title,
      category,
      employmentType: employmentType(m.jobType ?? det.jobType),
      companyName: str(m.companyName || det.companyName, 100) || "Employer",
      payAmount: p.payAmount,
      payType: p.payType,
      vacancies: Math.min(50, Math.max(1, Math.round(num(m.vacancies ?? det.vacancies) || 1))),
      urgency: String(m.urgency || "").toUpperCase() === "HIGH" ? "HIGH" : "NORMAL",
      shift: /day/i.test(String(m.shiftTiming || "")) ? "DAY" : /night/i.test(String(m.shiftTiming || "")) ? "NIGHT" : "ANY",
      area: str(m.companyCity || det.companyCity, 60) || areaOf(m.addressText || det.addressText) || "India",
      ...(loc ? { lat: loc.lat, lng: loc.lng, geohash: encodeGeohash(loc.lat, loc.lng), cell: encodeGeohash(loc.lat, loc.lng, 5) } : {}),
      status: jobStatus(m),
      applicationCount: appCount.get(d.id) || 0,
      createdAt: created,
      expiresAt: ts(m.expiresAt || det.expiresAt, created.toMillis() + 30 * DAY),
      ...(str(m.jobImageUrl || det.jobImageUrl, 1000) ? { photoUrl: str(m.jobImageUrl || det.jobImageUrl, 1000) } : {}),
    };
    const salaryNote = p.lossy ? `\n\nSalary: ${str(m.salary ?? det.salary, 80)}${m.salaryType ? ` (${String(m.salaryType).toLowerCase()})` : ""}` : "";
    const education = str(String(det.educationRequired || "").split("\n")[0], 60);
    set(d.ref, card);
    set(db.collection("job_details").doc(d.id), {
      employerId,
      description: (str(det.description, 2000 - salaryNote.length) + salaryNote).trim() || title,
      addressText: str(det.addressText || m.addressText, 200) || card.area,
      gender: jobGender(det.gender),
      experienceRequired: str(String(det.experienceRequired || "").split("\n")[0], 60),
      educationRequired: /[:📢]/u.test(education) ? "" : education,
      benefits: (Array.isArray(det.benefits) ? det.benefits : []).map((b) => str(b, 30)).filter(Boolean).slice(0, 8),
    });
    const contact = mobile10(det.contactNumber);
    if (contact) set(db.collection("job_contacts").doc(d.id), { employerId, contactNumber: contact });
    else bump("jobs without a valid contact number");
    bump(`jobs.${card.status}`);
    if (!loc) bump("jobs without a location (not in nearby feeds)");
    if (p.lossy) bump("jobs whose salary text was kept in the description");
  }
  stats["job categories"] = Object.entries(categories).sort((a, b) => b[1] - a[1]).map(([k, v]) => `${k}=${v}`).join(", ");
  for (const d of details) if (!jobIds.has(d.id)) { deletes.push(d.ref); bump("job_details orphan deleted"); }

  // applications
  for (const a of applications) {
    const x = a.data();
    if (!jobIds.has(x.jobId) || !x.workerId) { deletes.push(a.ref); bump("applications dropped (job gone)"); continue; }
    const status = APP_STATUS[String(x.status || "").toLowerCase()] || "applied";
    const created = ts(x.createdAt, NOW);
    set(db.collection("applications").doc(`${x.jobId}_${x.workerId}`), {
      jobId: x.jobId, workerId: x.workerId, employerId: str(x.employerId, 128), status,
      createdAt: created, updatedAt: created,
      ...(status === "hired" || status === "completed" ? { hiredAt: created } : {}),
      ...(status === "completed" ? { completedAt: created } : {}),
      callCount: 0,
      workerName: str(x.workerName, 80) || "Worker",
      workerPhoto: workerPhoto.get(x.workerId) || "",
      workerSkill: workerSkill.get(x.workerId) || "",
    });
    bump(`applications.${status}`);
  }

  // saved_jobs: same shape; drop ones whose job is gone
  for (const s of await all("saved_jobs")) {
    if (!jobIds.has(s.get("jobId"))) { deletes.push(s.ref); bump("saved_jobs dropped (job gone)"); continue; }
    set(s.ref, { userId: s.get("userId"), jobId: s.get("jobId"), createdAt: ts(s.get("createdAt"), NOW) });
    bump("saved_jobs");
  }

  // notifications: keep the last 30 days in the new shape (they auto-delete after 30 days)
  for (const n of await all("notifications")) {
    const x = n.data();
    const created = ts(x.createdAt || x.sentAt, 0);
    if (!created || created.toMillis() < NOW - 30 * DAY || !x.recipientId) { deletes.push(n.ref); bump("notifications older than 30 days deleted"); continue; }
    set(n.ref, { recipientId: x.recipientId, title: str(x.title, 200), body: str(x.message, 1000), type: str(x.type, 40) || "GENERAL",
      data: x.data && typeof x.data === "object" ? x.data : {}, read: x.isRead === true, createdAt: created,
      expireAt: Timestamp.fromMillis(created.toMillis() + 30 * DAY) });
    bump("notifications kept");
  }

  // user_tokens
  for (const t of await all("user_tokens")) {
    set(t.ref, { fcmToken: str(t.get("fcmToken"), 4096), language: ["en", "te", "hi"].includes(t.get("language")) ? t.get("language") : "en",
      updatedAt: ts(t.get("updatedAt"), NOW) });
    bump("user_tokens");
  }

  // referral codes, wallets (+ opening ledger entry), referrals, withdrawals
  for (const c of await all("referral_codes")) {
    const uid = c.get("userId");
    if (!uid) { deletes.push(c.ref); continue; }
    set(c.ref, { uid, role: roleOf.get(uid) || String(c.get("userRole") || "WORKER").toUpperCase(), active: c.get("isActive") !== false });
    bump("referral_codes");
  }
  let totalPaise = 0;
  for (const w of await all("referral_stats")) {
    const x = w.data();
    const balancePaise = Math.max(0, Math.round(num(x.availableBalance) * 100));
    totalPaise += balancePaise;
    set(w.ref, {
      referralCode: str(x.referralCode, 20),
      balancePaise,
      lifetimeEarnedPaise: Math.max(balancePaise, Math.round(num(x.totalEarnings) * 100)),
      withdrawnPaise: Math.round(num(x.withdrawnAmount) * 100),
      successfulReferrals: num(x.successfulReferrals),
      awardedMilestones: Array.isArray(x.awardedMilestones) ? x.awardedMilestones.map(Number).filter(Number.isFinite) : [],
      blocked: x.isBlocked === true,
      updatedAt: Timestamp.fromMillis(NOW),
    });
    if (balancePaise > 0) {
      set(db.collection("wallet_ledger").doc(`migration_${w.id}`), { uid: w.id, type: "ADJUSTMENT", amountPaise: balancePaise,
        refId: "schema-v2-opening-balance", balanceAfterPaise: balancePaise, createdAt: Timestamp.fromMillis(NOW) });
      bump("wallets with an opening balance");
    }
    for (const s of await w.ref.listCollections()) { deletes.push(s); bump("wallet audit_logs subcollections deleted"); }
    bump("wallets");
  }
  stats["total wallet balance (₹)"] = (totalPaise / 100).toFixed(2);
  for (const r of await all("referrals")) {
    const x = r.data();
    const referee = x.referredUserId || x.referredId;
    if (!referee) { deletes.push(r.ref); continue; }
    set(db.collection("referrals").doc(referee), {
      referrerUid: x.referrerId || x.referrerUserId, code: str(x.referralCode, 20),
      status: String(x.status || "PENDING").toUpperCase(), fraudScore: num(x.fraudScore),
      createdAt: ts(x.createdAt, NOW), ...(ts(x.completedAt) ? { completedAt: ts(x.completedAt) } : {}),
      expiresAt: ts(x.expiresAt, NOW + 30 * DAY),
    });
    if (r.id !== referee) deletes.push(r.ref);
    bump("referrals");
  }
  for (const w of await all("withdrawal_requests")) {
    const x = w.data();
    set(w.ref, { uid: x.userId, amountPaise: Math.round(num(x.amount) * 100), upiId: str(x.upiId, 100), status: String(x.status || "PENDING").toUpperCase(),
      txnRef: str(x.transactionId, 100), failureReason: "", createdAt: ts(x.createdAt, NOW),
      ...(ts(x.processedAt || x.paidAt) ? { processedAt: ts(x.processedAt || x.paidAt) } : {}) });
    bump(`withdrawals.${String(x.status).toUpperCase()}`);
  }
  for (const s of await all("subscription_payment_requests")) {
    const x = s.data();
    set(s.ref, { employerId: x.employerId, planId: str(x.planId, 40), amountPaise: Math.round(num(x.amount) * 100), upiIdUsed: str(x.upiIdUsed, 100),
      utrNumber: str(x.utrNumber, 20), screenshotUrl: str(x.screenshotUrl, 1000), status: String(x.status || "PENDING").toUpperCase(),
      ...(x.rejectionReason ? { rejectionReason: str(x.rejectionReason, 300) } : {}), createdAt: ts(x.requestTimestamp || x.createdAt, NOW),
      ...(ts(x.verifiedTimestamp) ? { verifiedAt: ts(x.verifiedTimestamp) } : {}) });
    bump("subscription payments");
  }

  // ratings, reports, announcements, QR codes: same ids, new fields
  for (const r of await all("ratings")) {
    const x = r.data();
    set(r.ref, { raterId: x.fromUserId, targetId: x.toUserId, stars: Math.min(5, Math.max(1, Math.round(num(x.rating)))),
      review: str(x.review, 500), tags: Array.isArray(x.tags) ? x.tags.slice(0, 10) : [], createdAt: ts(x.createdAt, NOW) });
    bump("ratings");
  }
  for (const r of await all("job_reports")) {
    const x = r.data();
    set(r.ref, { jobId: x.jobId, reporterId: x.reporterId, reason: str(x.reportType, 30) || "OTHER", note: str(x.description, 500),
      status: str(x.status, 20) || "open", createdAt: ts(x.createdAt, NOW) });
    bump("job_reports");
  }
  for (const a of await all("announcements")) {
    const x = a.data();
    set(a.ref, { title: str(x.title, 200), message: str(x.message, 1000), type: str(x.type, 40), priority: num(x.priority),
      targetRole: str(x.targetRole, 20), actionRoute: str(x.actionRoute, 200), active: x.isActive === true,
      createdAt: ts(x.createdAt, NOW), ...(ts(x.expiresAt) ? { expiresAt: ts(x.expiresAt) } : {}) });
    bump("announcements");
  }
  for (const q of await all("active_qr_codes")) {
    set(q.ref, { imageUrl: str(q.get("imageUrl"), 1000), label: str(q.get("label"), 100), active: q.get("isActive") !== false, createdAt: ts(q.get("createdAt"), NOW) });
    bump("active_qr_codes");
  }

  // Collections of removed features (all in the backup).
  const dropped = ["users", "jobs", "employer_job_cards", "public_profiles", "job_call_feedback", "job_call_sessions",
    "instant_requests", "instant_responses", "worker_availability", "worker_job_requests", "referral_events",
    "system_metrics", "withdrawal_daily"];
  for (const c of dropped) {
    const n = (await db.collection(c).count().get()).data().count;
    if (n) { deletes.push(db.collection(c)); stats[`collection deleted: ${c}`] = n; }
  }

  console.log(JSON.stringify(stats, null, 1));
  console.log(`\n${writes.length} document writes, ${deletes.length} deletes (collections count as one)`);
  if (!APPLY) return;

  // ── apply: writes first (whole-document replace), then deletes, then role claims ──
  const writer = db.bulkWriter();
  writer.onWriteError((e) => { console.error("write failed", e.documentRef.path, e.message); return e.failedAttempts < 5; });
  for (const [ref, data] of writes) writer.set(ref, data);
  await writer.close();
  console.log("writes done");
  for (const target of deletes) {
    if (target instanceof admin.firestore.CollectionReference) await db.recursiveDelete(target);
    else await target.delete();
  }
  console.log("deletes done");

  // The role is a custom claim on the login token (rules + functions read it).
  const uids = [...roleOf.keys()];
  let claimed = 0;
  for (let i = 0; i < uids.length; i += 100) {
    const { users: accounts } = await admin.auth().getUsers(uids.slice(i, i + 100).map((uid) => ({ uid })));
    for (const account of accounts) {
      const role = roleOf.get(account.uid);
      if (account.customClaims?.role === role) continue;
      await admin.auth().setCustomUserClaims(account.uid, { ...(account.customClaims || {}), role });
      claimed++;
    }
  }
  console.log(`role claims set on ${claimed} accounts`);
})().catch((e) => { console.error(e); process.exit(1); });
