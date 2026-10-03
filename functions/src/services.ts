/**
 * DutyPe Services — Urban Company style home services (launch city: Khammam).
 *
 * Customer (EMPLOYER role) books a fixed-price service from the catalog; nearby verified partners
 * (WORKER role, approved by an admin) get a ringing offer in widening waves; the first to accept
 * gets the job. Status: SEARCHING → ASSIGNED → ON_THE_WAY → STARTED (customer's 4-digit code) →
 * COMPLETED, or CANCELLED / NO_PARTNER.
 *
 * Money without a payment gateway: the customer pays the partner the price + booking fee (+ extras
 * they approved) in cash or UPI after the job. On completion DutyPe takes its booking fee +
 * commission from the partner's prepaid credits (service_partners.creditsPaise). Partners top up
 * by paying DutyPe's UPI ID and entering the UTR; an admin verifies it (like subscriptions).
 *
 *   getServiceCatalog          categories, services, fees (defaults + app_config/services)
 *   createServiceBooking       customer books; wave 1 goes out at once (scheduled: 90 min before)
 *   cancelServiceBooking       customer cancels before the job starts
 *   rateServiceBooking         customer rates a completed job
 *   applyServicePartner        worker applies with categories
 *   setPartnerOnline           partner goes online/offline with their location
 *   getServiceOffer            partner opens an offer (no customer contact until accepted)
 *   acceptServiceBooking       first partner to accept gets the job (transaction)
 *   updateServiceBooking       partner: on_the_way / start (code) / complete (extras) / cancel
 *   requestPartnerTopup        partner reports a UPI payment (UTR) for credits
 *   reviewServicePartner       admin: approve / reject / suspend a partner
 *   verifyPartnerTopup         admin: approve / reject a top-up
 *   dispatchServiceWaves()     called every minute from urgent.ts's scheduler (no extra job)
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { busyWith } from "./lib/busy";
import { onCallSecured } from "./secure-callable";
import { fail, int, latLng, obj, str, stringList, text } from "./lib/input";
import { distanceKm } from "./lib/geo";
import { placeOf } from "./lib/places";
import { notify } from "./lib/notify";
import { normalizeLocale, tBody, tTitle } from "./notification-i18n";
import { isCallerAdmin } from "./app-config";
import {
  CATEGORIES, ITEMS, bringFor, findService, mergeConfig, newStartOtp, partnerFeeFor, provideFor, quote, skillOf, takePaise,
  type ServicesConfig,
} from "./lib/service-catalog";
import {
  AppConfig, CouponUses, EmployerProfiles, PartnerLedger, PartnerTopups, ServiceBookingSecrets, ServiceBookings, ServicePartners,
  UserTokens, Values, WorkerProfiles,
} from "./schema";

const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const BK = ServiceBookings;
const SP = ServicePartners;

export const BookingStatus = {
  SEARCHING: "SEARCHING",
  ASSIGNED: "ASSIGNED",
  ON_THE_WAY: "ON_THE_WAY",
  STARTED: "STARTED",
  COMPLETED: "COMPLETED",
  CANCELLED: "CANCELLED",
  NO_PARTNER: "NO_PARTNER",
} as const;
const BS = BookingStatus;
const OPEN_STATUSES: string[] = [BS.SEARCHING, BS.ASSIGNED, BS.ON_THE_WAY, BS.STARTED];
const ASSIGNED_STATUSES: string[] = [BS.ASSIGNED, BS.ON_THE_WAY, BS.STARTED];

export const PartnerStatus = { PENDING: "PENDING", APPROVED: "APPROVED", REJECTED: "REJECTED", SUSPENDED: "SUSPENDED" } as const;

/** Offer waves, km from the customer. After the last wave the widest one repeats. */
export const SERVICE_WAVES_KM = [3, 6, 10, 15, 25] as const;
export const SERVICE_WAVE_INTERVAL_MS = 90 * 1000;
const REPEAT_WAVE_MS = 5 * 60 * 1000;
/** "Now" bookings give up after this long without a partner. */
export const SEARCH_TIMEOUT_MS = 45 * 60 * 1000;
/** Scheduled bookings start looking this long before the slot. */
const SCHEDULE_LEAD_MS = 90 * 60 * 1000;
const PARTNER_STALE_MS = 12 * 60 * 60 * 1000;
const MAX_OPEN_PER_CUSTOMER = 3;
const IST_OFFSET_MS = 330 * 60 * 1000;
const HOUR_MS = 60 * 60 * 1000;

// ─────────────────────────────── config ───────────────────────────────

let cached: { at: number; config: ServicesConfig } | null = null;

export async function loadConfig(): Promise<ServicesConfig> {
  if (cached && Date.now() - cached.at < 60_000) return cached.config;
  const doc = await db.collection(AppConfig.COLLECTION).doc(AppConfig.DOC_SERVICES).get();
  const config = mergeConfig(doc.data());
  cached = { at: Date.now(), config };
  return config;
}

/** Test seam: forget the cached config. */
export function clearConfigCache(): void {
  cached = null;
}

export const getServiceCatalog = onCallSecured({ requireAuth: false, enforceAppCheck: false, timeoutSeconds: 10 }, async (raw: unknown, context) => {
  const c = await loadConfig();
  // The admin panel also needs services that are switched off, to switch them back on.
  const data = obj(raw);
  const all = data.all === true && await isCallerAdmin(context);
  const lat = Number(data.lat);
  const lng = Number(data.lng);
  const hasPoint = data.lat !== undefined && data.lng !== undefined && Number.isFinite(lat) && Number.isFinite(lng);
  return {
    city: c.city,
    bookingFee: c.bookingFee,
    inspectionFee: c.inspectionFee,
    commissionPct: c.commissionPct,
    upiId: c.upiId,
    upiName: c.upiName,
    minTopup: c.minTopup,
    partnerFee: c.partnerFee,
    partnerFirstJobFree: c.partnerFirstJobFree,
    firstBookingFeeFree: c.firstBookingFeeFree,
    // Offers the app may advertise (codes marked hidden are shared only through posters / WhatsApp).
    offers: c.coupons
      .filter((x) => x.visible !== false && x.active !== false && (!x.validTo || x.validTo > Date.now()) &&
        (!x.validFrom || x.validFrom <= Date.now()))
      .map((x) => ({
        code: x.code, title: x.title, type: x.type, value: x.value, maxOff: x.maxOff ?? 0, minOrder: x.minOrder ?? 0,
        validTo: x.validTo ?? 0, firstBookingOnly: x.firstBookingOnly === true, categories: x.categories ?? [],
      })),
    categories: c.categories,
    services: (all ? c.services : c.services.filter((x) => x.active !== false))
      .map((x) => ({ ...x, provide: provideFor(x), bring: bringFor(x) })),
    // Labels for the provide / bring ids, in en / te / hi.
    items: ITEMS,
    // Only when the app sent a location: is it inside the service area?
    ...(hasPoint ? { inArea: inServiceArea(c, lat, lng) } : {}),
  };
});

// ─────────────────────────────── service area ───────────────────────────────

/** True when the point is inside a district where DutyPe Services runs (Khammam district at launch). */
export function inServiceArea(config: ServicesConfig, lat: number, lng: number): boolean {
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return false;
  const place = placeOf(lat, lng);
  return !!place && config.districtIds.includes(place.districtId);
}

function requireServiceArea(config: ServicesConfig, lat: number, lng: number, who: "customer" | "partner"): void {
  if (!inServiceArea(config, lat, lng)) {
    fail("failed-precondition", who === "customer" ?
      `DutyPe Services is available only in ${config.city} district for now` :
      `DutyPe Services partners must be in ${config.city} district for now`);
  }
}

// ─────────────────────────────── helpers ───────────────────────────────

const ms = (v: unknown) => (v as admin.firestore.Timestamp | undefined)?.toMillis?.() ?? 0;

function e164(raw: unknown): string {
  const digits = String(raw ?? "").replace(/\D/g, "");
  if (/^[6-9]\d{9}$/.test(digits)) return `+91${digits}`;
  if (/^91[6-9]\d{9}$/.test(digits)) return `+${digits}`;
  return "";
}

function deepLinkCustomer(id: string) {
  return `dutype://services/booking/${id}`;
}

function deepLinkPartnerJob(id: string) {
  return `dutype://partner/job/${id}`;
}

function deepLinkOffer(id: string) {
  return `dutype://partner/offer/${id}`;
}

async function tellCustomer(uid: string, bookingId: string, templateId: string, params: Record<string, string | number>) {
  await notify(uid, {
    type: "SERVICE_BOOKING",
    templateId: templateId as never,
    params,
    data: { bookingId, deepLink: deepLinkCustomer(bookingId) },
    role: Values.Role.EMPLOYER,
  });
}

async function tellPartner(uid: string, templateId: string, params: Record<string, string | number>, data: Record<string, string>) {
  await notify(uid, { type: "SERVICE_BOOKING", templateId: templateId as never, params, data, role: Values.Role.WORKER });
}

/** Paise a partner must hold to accept: booking fee + commission on the catalog price. */
/** Partner fee for this booking: the one fixed at accept, else the full configured fee (worst case). */
function feeOf(b: admin.firestore.DocumentData, fallbackPartnerFee: number): number {
  return b[BK.PARTNER_FEE] !== undefined ? Number(b[BK.PARTNER_FEE]) : fallbackPartnerFee;
}

/** The ₹ fee this partner pays DutyPe for booking [b] (first job free, see partnerFeeFor). */
function partnerFeeOf(config: ServicesConfig, jobsCompleted: number, b: FirebaseFirestore.DocumentData): number {
  return partnerFeeFor(config, jobsCompleted, Number(b[BK.PRICE] || 0), Number(b[BK.BOOKING_FEE] || 0),
    Number(b[BK.DISCOUNT] || 0), Number(b[BK.COMMISSION_PCT] || 0));
}

/** Per-partner credit need for [b] (depends on whether it would be their free first job). */
async function creditsNeededFor(b: admin.firestore.DocumentData): Promise<(jobsCompleted: number) => number> {
  const config = await loadConfig();
  return (jobs) => requiredCredits(b, partnerFeeOf(config, jobs, b));
}

/** Paise a partner must hold to accept (before extras). */
function requiredCredits(b: admin.firestore.DocumentData, partnerFee: number): number {
  return takePaise(Number(b[BK.PRICE] || 0), 0, Number(b[BK.BOOKING_FEE] || 0), Number(b[BK.DISCOUNT] || 0),
    feeOf(b, partnerFee), Number(b[BK.COMMISSION_PCT] || 0));
}

/** What the partner keeps from the service price (the booking fee they collect goes to DutyPe). */
function partnerEarning(b: admin.firestore.DocumentData, partnerFee: number): number {
  const price = Number(b[BK.PRICE] || 0);
  return price - feeOf(b, partnerFee) - Math.round((price * Number(b[BK.COMMISSION_PCT] || 0)) / 100);
}

/** The booker's name, and whether they are on a paid DutyPe plan (no booking fee). */
async function customerOf(uid: string, role: string): Promise<{ name: string; planMember: boolean }> {
  if (role === Values.Role.WORKER) {
    const w = await db.collection(WorkerProfiles.COLLECTION).doc(uid).get();
    return { name: String(w.get(WorkerProfiles.NAME) || "Customer"), planMember: false };
  }
  const p = await db.collection(EmployerProfiles.COLLECTION).doc(uid).get();
  const S = EmployerProfiles.Subscription;
  const sub = (p.get(EmployerProfiles.SUBSCRIPTION) || {}) as Record<string, unknown>;
  const exp = ms(sub[S.EXPIRES_AT]);
  // A paid plan only (the launch campaign gives everyone free posts, not free bookings).
  const planMember = sub[S.STATUS] === "ACTIVE" && (exp === 0 || exp > Date.now()) &&
    Boolean(sub[S.PLAN_ID]) && sub[S.PLAN_ID] !== "UNLIMITED_CAMPAIGN";
  return {
    name: String(p.get(EmployerProfiles.OWNER_NAME) || p.get(EmployerProfiles.BUSINESS_NAME) || "Customer"),
    planMember,
  };
}

/** True when the customer has never had a booking that was not cancelled. */
async function isFirstBooking(uid: string): Promise<boolean> {
  const prev = await db.collection(BK.COLLECTION)
    .where(BK.CUSTOMER_ID, "==", uid)
    .where(BK.STATUS, "in", [...OPEN_STATUSES, BS.COMPLETED])
    .limit(1).get();
  return prev.empty;
}

/** A cancelled / unassigned booking gives the coupon back. */
async function releaseCoupon(b: admin.firestore.DocumentData): Promise<void> {
  const code = String(b[BK.COUPON_CODE] || "");
  if (!code) return;
  await db.collection(CouponUses.COLLECTION).doc(`${b[BK.CUSTOMER_ID]}_${code}`).delete().catch(() => undefined);
}

/** Test seam: the function that actually pushes an offer. */
export const offerSender = {
  send: (message: admin.messaging.Message): Promise<string> => admin.messaging().send(message),
};

/** A ringing offer to one partner (data push on the urgent_offers channel; no inbox document). */
async function sendOffer(partnerId: string, bookingId: string, b: admin.firestore.DocumentData, km: number): Promise<boolean> {
  try {
    const tokenSnap = await db.collection(UserTokens.COLLECTION).doc(partnerId).get();
    const token = tokenSnap.get(UserTokens.FCM_TOKEN);
    if (typeof token !== "string" || !token) return false;
    const locale = normalizeLocale(tokenSnap.get(UserTokens.LANGUAGE));
    const params = {
      service: String(b[BK.SERVICE_NAME] || ""),
      area: String(b[BK.AREA] || ""),
      km: Math.max(1, Math.round(km)),
      earning: partnerEarning(b, (await loadConfig()).partnerFee),
    };
    await offerSender.send({
      token,
      data: {
        type: "SERVICE_OFFER",
        bookingId,
        title: tTitle("SERVICE_OFFER", locale, params),
        body: tBody("SERVICE_OFFER", locale, params),
        deepLink: deepLinkOffer(bookingId),
        channel: "urgent_offers",
        locale,
      },
      android: { priority: "high", ttl: 10 * 60 * 1000 },
    });
    return true;
  } catch (e) {
    functions.logger.warn(`service offer to ${partnerId} failed`, e);
    return false;
  }
}

/** Online approved partners for [category] between fromKm (exclusive) and toKm of the point. */
export async function partnersInRing(
  category: string, lat: number, lng: number, fromKm: number, toKm: number, excluded: string[], needPaise: number | ((jobsCompleted: number) => number), nowMs: number,
): Promise<Array<{ id: string; km: number }>> {
  const config = await loadConfig();
  const snap = await db.collection(SP.COLLECTION)
    .where(SP.STATUS, "==", PartnerStatus.APPROVED)
    .where(SP.ONLINE, "==", true)
    .where(SP.CATEGORIES, "array-contains", category)
    .limit(500)
    .get();
  const out: Array<{ id: string; km: number }> = [];
  for (const d of snap.docs) {
    if (excluded.includes(d.id)) continue;
    if (d.get(SP.ACTIVE_BOOKING_ID)) continue;
    if (nowMs - ms(d.get(SP.LAST_SEEN_AT)) > PARTNER_STALE_MS) continue;
    const need = typeof needPaise === "number" ? needPaise : needPaise(Number(d.get(SP.JOBS_COMPLETED) || 0));
    if (Number(d.get(SP.CREDITS_PAISE) || 0) < need) continue;
    const pLat = Number(d.get(SP.LAT));
    const pLng = Number(d.get(SP.LNG));
    if (!Number.isFinite(pLat) || !Number.isFinite(pLng)) continue;
    const km = distanceKm(lat, lng, pLat, pLng);
    if (km > fromKm && km <= toKm && inServiceArea(config, pLat, pLng)) out.push({ id: d.id, km });
  }
  return out;
}

/** Sends the next wave for one searching booking, if due. Returns the radius reached or null. */
export async function advanceServiceWave(bookingId: string, nowMs: number): Promise<number | null> {
  const ref = db.collection(BK.COLLECTION).doc(bookingId);
  const claimed = await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const b = snap.data();
    if (!b || b[BK.STATUS] !== BS.SEARCHING) return null;
    const due = ms(b[BK.NEXT_WAVE_AT]);
    if (!due || due > nowMs) return null;
    if (ms(b[BK.EXPIRES_AT]) <= nowMs) return null; // expireServiceBookings handles it
    const reached = Number(b[BK.DISPATCH_RADIUS_KM] || 0);
    const next = SERVICE_WAVES_KM.find((km) => km > reached);
    // After the widest wave, offer again to everyone in range (partners come online later).
    const fromKm = next === undefined ? 0 : reached;
    const toKm = next ?? SERVICE_WAVES_KM[SERVICE_WAVES_KM.length - 1];
    const widest = SERVICE_WAVES_KM[SERVICE_WAVES_KM.length - 1];
    tx.update(ref, {
      [BK.DISPATCH_RADIUS_KM]: toKm,
      [BK.NEXT_WAVE_AT]: Timestamp.fromMillis(nowMs + (toKm === widest ? REPEAT_WAVE_MS : SERVICE_WAVE_INTERVAL_MS)),
    });
    return { b, fromKm, toKm };
  });
  if (!claimed) return null;
  const { b, fromKm, toKm } = claimed;
  const partners = await partnersInRing(String(b[BK.CATEGORY]), Number(b[BK.LAT]), Number(b[BK.LNG]), fromKm, toKm,
    (b[BK.EXCLUDED_PARTNER_IDS] || []) as string[], await creditsNeededFor(b), nowMs);
  const results = await Promise.all(partners.map((p) => sendOffer(p.id, bookingId, b, p.km)));
  functions.logger.info(`service ${bookingId}: wave ${fromKm}-${toKm} km, ${partners.length} partners, ` +
    `${results.filter(Boolean).length} offers sent`);
  return toKm;
}

/** Bookings nobody took in time → NO_PARTNER, and the customer is told. */
export async function expireServiceBookings(nowMs: number): Promise<number> {
  const snap = await db.collection(BK.COLLECTION)
    .where(BK.STATUS, "==", BS.SEARCHING)
    .where(BK.EXPIRES_AT, "<=", Timestamp.fromMillis(nowMs))
    .limit(100)
    .get();
  let expired = 0;
  for (const d of snap.docs) {
    const done = await db.runTransaction(async (tx) => {
      const cur = await tx.get(d.ref);
      if (cur.get(BK.STATUS) !== BS.SEARCHING) return false;
      tx.update(d.ref, {
        [BK.STATUS]: BS.NO_PARTNER,
        [BK.NEXT_WAVE_AT]: FieldValue.delete(),
        [BK.UPDATED_AT]: Timestamp.fromMillis(nowMs),
      });
      return true;
    });
    if (done) {
      expired++;
      await releaseCoupon(d.data());
      await tellCustomer(String(d.get(BK.CUSTOMER_ID)), d.id, "SERVICE_NO_PARTNER", { service: String(d.get(BK.SERVICE_NAME) || "") });
    }
  }
  return expired;
}

/** Every minute (from urgent.ts's scheduler): due waves and expiries. */
export async function dispatchServiceWaves(nowMs: number): Promise<void> {
  const due = await db.collection(BK.COLLECTION)
    .where(BK.STATUS, "==", BS.SEARCHING)
    .where(BK.NEXT_WAVE_AT, "<=", Timestamp.fromMillis(nowMs))
    .limit(100)
    .get();
  await Promise.all(due.docs.map((d) => advanceServiceWave(d.id, nowMs).catch((e) =>
    functions.logger.warn(`service ${d.id}: wave failed`, e))));
  await expireServiceBookings(nowMs);
}

// ─────────────────────────────── customer ───────────────────────────────

export const createServiceBooking = onCallSecured({ timeoutSeconds: 30 }, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  // Anyone with a DutyPe account can book for their home: an employer, or a worker too.
  const role = String(context.auth?.token.role || "");
  if (role !== Values.Role.EMPLOYER && role !== Values.Role.WORKER) {
    fail("permission-denied", "Finish registration to book services");
  }
  const phone = e164(context.auth?.token.phone_number);
  if (!phone) fail("failed-precondition", "Log in with your mobile number first");
  const data = obj(raw);
  const config = await loadConfig();
  const service = findService(config, str(data, "serviceId", { max: 60 }));
  if (!service) fail("not-found", "This service is not available");
  const { lat, lng } = latLng(data);
  requireServiceArea(config, lat, lng, "customer");
  const addressText = text(data, "addressText", { min: 5, max: 300 });
  const area = str(data, "area", { max: 80, optional: true });
  const note = text(data, "note", { max: 300, optional: true });
  const now = Date.now();
  const scheduled = data.when === "scheduled";
  let scheduledAt = 0;
  if (scheduled) {
    scheduledAt = Number(data.scheduledAt);
    if (!Number.isFinite(scheduledAt) || scheduledAt < now + 30 * 60 * 1000 || scheduledAt > now + 7 * 24 * HOUR_MS) {
      fail("invalid-argument", "Pick a time from 30 minutes to 7 days from now");
    }
    const hour = new Date(scheduledAt + IST_OFFSET_MS).getUTCHours();
    if (hour < 7 || hour >= 21) fail("invalid-argument", "Pick a time between 7 AM and 9 PM");
  }

  const open = await db.collection(BK.COLLECTION)
    .where(BK.CUSTOMER_ID, "==", uid)
    .where(BK.STATUS, "in", OPEN_STATUSES)
    .count().get();
  if (open.data().count >= MAX_OPEN_PER_CUSTOMER) {
    fail("resource-exhausted", "You already have 3 open bookings. Finish or cancel one first.");
  }

  const who = await customerOf(uid, role);
  const customerName = who.name;
  const q = quote(config, service, await isFirstBooking(uid), str(data, "couponCode", { max: 20, optional: true }), now, who.planMember);
  if (q.couponError) fail("failed-precondition", q.couponError);
  const bookingFee = q.bookingFee;
  const ref = db.collection(BK.COLLECTION).doc();
  const startOtp = newStartOtp();
  const firstWaveAt = scheduled ? Math.max(now, scheduledAt - SCHEDULE_LEAD_MS) : now;
  const booking = {
    [BK.CUSTOMER_ID]: uid,
    [BK.CUSTOMER_NAME]: customerName,
    [BK.CUSTOMER_PHONE]: phone,
    [BK.CATEGORY]: service.category,
    [BK.SERVICE_ID]: service.id,
    [BK.SERVICE_NAME]: service.name,
    [BK.PRICE]: service.price,
    [BK.BOOKING_FEE]: bookingFee,
    [BK.COMMISSION_PCT]: config.commissionPct,
    [BK.INSPECTION]: service.inspection === true,
    [BK.ADDRESS_TEXT]: addressText,
    [BK.AREA]: area,
    [BK.LAT]: lat,
    [BK.LNG]: lng,
    [BK.NOTE]: note,
    [BK.WHEN]: scheduled ? "scheduled" : "now",
    ...(scheduled ? { [BK.SCHEDULED_AT]: Timestamp.fromMillis(scheduledAt) } : {}),
    [BK.STATUS]: BS.SEARCHING,
    // A partner booking for their own home never gets their own job.
    [BK.EXCLUDED_PARTNER_IDS]: [uid],
    [BK.DISPATCH_RADIUS_KM]: 0,
    [BK.NEXT_WAVE_AT]: Timestamp.fromMillis(firstWaveAt),
    [BK.EXPIRES_AT]: Timestamp.fromMillis(scheduled ? scheduledAt + HOUR_MS : now + SEARCH_TIMEOUT_MS),
    [BK.DISCOUNT]: q.discount,
    [BK.DISCOUNT_LABEL]: q.discountLabel,
    [BK.COUPON_CODE]: q.couponCode,
    [BK.TOTAL]: q.total,
    [BK.CREATED_AT]: Timestamp.fromMillis(now),
    [BK.UPDATED_AT]: Timestamp.fromMillis(now),
  };
  const batch = db.batch();
  batch.create(ref, booking);
  batch.create(db.collection(ServiceBookingSecrets.COLLECTION).doc(ref.id), {
    [ServiceBookingSecrets.CUSTOMER_ID]: uid,
    [ServiceBookingSecrets.START_OTP]: startOtp,
  });
  // One use per customer: creating this document fails if the coupon was used before.
  if (q.couponCode) {
    batch.create(db.collection(CouponUses.COLLECTION).doc(`${uid}_${q.couponCode}`), {
      [CouponUses.BOOKING_ID]: ref.id,
      [CouponUses.CREATED_AT]: Timestamp.fromMillis(now),
    });
  }
  try {
    await batch.commit();
  } catch (e) {
    if ((e as { code?: number }).code === 6) fail("already-exists", "You have already used this coupon");
    throw e;
  }
  if (!scheduled) await advanceServiceWave(ref.id, now);
  return {
    bookingId: ref.id, startOtp, price: service.price, bookingFee, discount: q.discount, discountLabel: q.discountLabel,
    total: q.total,
  };
});

/** Price breakdown before booking: first-booking offer, the coupon typed, and the visible offers. */
export const previewServiceQuote = onCallSecured({ timeoutSeconds: 10 }, async (raw: unknown, context) => {
  const data = obj(raw);
  const config = await loadConfig();
  const service = findService(config, str(data, "serviceId", { max: 60 }));
  if (!service) fail("not-found", "This service is not available");
  const now = Date.now();
  const first = await isFirstBooking(context.auth!.uid);
  const who = await customerOf(context.auth!.uid, String(context.auth?.token.role || ""));
  const q = quote(config, service, first, str(data, "couponCode", { max: 20, optional: true }), now, who.planMember);
  let couponError = q.couponError;
  if (!couponError && q.couponCode) {
    const used = await db.collection(CouponUses.COLLECTION).doc(`${context.auth!.uid}_${q.couponCode}`).get();
    if (used.exists) couponError = "You have already used this coupon";
  }
  const offers = config.coupons
    .filter((c) => c.visible !== false && c.active !== false && (!c.validTo || c.validTo > now) && (!c.validFrom || c.validFrom <= now))
    .filter((c) => !c.firstBookingOnly || first)
    .map((c) => ({ code: c.code, title: c.title, minOrder: c.minOrder ?? 0 }));
  return { ...q, ...(couponError ? { couponError } : {}), firstBooking: first, offers };
});

export const cancelServiceBooking = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const bookingId = str(data, "bookingId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
  const reason = str(data, "reason", { max: 200, optional: true });
  const ref = db.collection(BK.COLLECTION).doc(bookingId);
  const out = await db.runTransaction(async (tx) => {
    const b = await tx.get(ref);
    if (!b.exists || b.get(BK.CUSTOMER_ID) !== uid) fail("not-found", "Booking not found");
    const status = String(b.get(BK.STATUS));
    if (![BS.SEARCHING, BS.ASSIGNED, BS.ON_THE_WAY].includes(status as never)) {
      fail("failed-precondition", status === BS.STARTED ? "The work has started; ask the partner to finish it" : "This booking is already closed");
    }
    const partnerId = String(b.get(BK.PARTNER_ID) || "");
    const partnerRef = partnerId ? db.collection(SP.COLLECTION).doc(partnerId) : null;
    const partner = partnerRef ? await tx.get(partnerRef) : null;
    tx.update(ref, {
      [BK.STATUS]: BS.CANCELLED,
      [BK.CANCELLED_BY]: "customer",
      ...(reason ? { [BK.CANCEL_REASON]: reason } : {}),
      [BK.NEXT_WAVE_AT]: FieldValue.delete(),
      [BK.UPDATED_AT]: Timestamp.now(),
    });
    if (partnerRef && partner?.get(SP.ACTIVE_BOOKING_ID) === bookingId) {
      tx.update(partnerRef, { [SP.ACTIVE_BOOKING_ID]: FieldValue.delete() });
    }
    return { partnerId, service: String(b.get(BK.SERVICE_NAME) || ""), d: b.data() || {} };
  });
  await releaseCoupon(out.d);
  if (out.partnerId) {
    await tellPartner(out.partnerId, "SERVICE_CANCELLED_BY_CUSTOMER", { service: out.service },
      { bookingId, deepLink: "dutype://partner" });
  }
  return { ok: true };
});

export const rateServiceBooking = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const bookingId = str(data, "bookingId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
  const stars = int(data, "stars", { min: 1, max: 5 });
  const review = text(data, "review", { max: 500, optional: true });
  const ref = db.collection(BK.COLLECTION).doc(bookingId);
  await db.runTransaction(async (tx) => {
    const b = await tx.get(ref);
    if (!b.exists || b.get(BK.CUSTOMER_ID) !== uid) fail("not-found", "Booking not found");
    if (b.get(BK.STATUS) !== BS.COMPLETED) fail("failed-precondition", "You can rate after the job is completed");
    if (b.get(BK.RATING)) fail("already-exists", "You already rated this job");
    tx.update(ref, { [BK.RATING]: stars, [BK.REVIEW]: review, [BK.UPDATED_AT]: Timestamp.now() });
    tx.update(db.collection(SP.COLLECTION).doc(String(b.get(BK.PARTNER_ID))), {
      [SP.RATING_SUM]: FieldValue.increment(stars),
      [SP.RATING_COUNT]: FieldValue.increment(1),
    });
  });
  return { ok: true };
});

// ─────────────────────────────── partner ───────────────────────────────

const CATEGORY_IDS = CATEGORIES.map((c) => c.id) as string[];

export const applyServicePartner = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  if (context.auth?.token.role !== Values.Role.WORKER) fail("permission-denied", "Only workers can become service partners");
  const data = obj(raw);
  const categories = stringList(data, "categories", { maxItems: CATEGORY_IDS.length, maxLength: 20 })
    .map((c) => c.toUpperCase()).filter((c) => CATEGORY_IDS.includes(c));
  if (!categories.length) fail("invalid-argument", "Choose at least one service");
  const experienceYears = int(data, "experienceYears", { min: 0, max: 50, optional: true });
  const area = str(data, "area", { max: 80, optional: true });
  const note = text(data, "note", { max: 300, optional: true });
  const skillProof = text(data, "skillProof", { max: 300, optional: true });
  if (data.acceptGuidelines !== true) fail("failed-precondition", "Please read and accept the DutyPe partner code of conduct");
  // Wiring, gas, plumbing and appliances can hurt people if done wrong: those need experience.
  const skilled = categories.filter((c) => skillOf(c) === "SKILLED");
  if (skilled.length && experienceYears < 1) {
    fail("invalid-argument", "Electrician, AC, plumber, appliance, carpentry and painting work needs at least 1 year of experience. " +
      "You can still apply for cleaning, home help and car wash.");
  }
  const { lat, lng } = latLng(data);
  requireServiceArea(await loadConfig(), lat, lng, "partner");
  const worker = await db.collection(WorkerProfiles.COLLECTION).doc(uid).get();
  if (!worker.exists) fail("failed-precondition", "Complete your worker profile first");
  const ref = db.collection(SP.COLLECTION).doc(uid);
  await db.runTransaction(async (tx) => {
    const cur = await tx.get(ref);
    const status = cur.get(SP.STATUS);
    if (status === PartnerStatus.APPROVED) fail("already-exists", "You are already a DutyPe partner. Contact support to change services.");
    if (status === PartnerStatus.SUSPENDED) fail("permission-denied", "Your partner account is suspended. Contact support.");
    const now = Timestamp.now();
    tx.set(ref, {
      [SP.STATUS]: PartnerStatus.PENDING,
      [SP.NAME]: String(worker.get(WorkerProfiles.NAME) || "Partner"),
      [SP.PHONE]: e164(context.auth?.token.phone_number) || String(worker.get(WorkerProfiles.PHONE) || ""),
      [SP.PHOTO_URL]: String(worker.get(WorkerProfiles.PHOTO_URL) || ""),
      [SP.CATEGORIES]: categories,
      [SP.EXPERIENCE_YEARS]: experienceYears,
      [SP.AREA]: area,
      [SP.NOTE]: note,
      [SP.SKILL_PROOF]: skillProof,
      [SP.SKILLED_CATEGORIES]: skilled,
      [SP.GUIDELINES_ACCEPTED_AT]: now,
      [SP.ONLINE]: false,
      [SP.LAT]: lat,
      [SP.LNG]: lng,
      [SP.CREDITS_PAISE]: cur.exists ? Number(cur.get(SP.CREDITS_PAISE) || 0) : 0,
      [SP.RATING_SUM]: cur.exists ? Number(cur.get(SP.RATING_SUM) || 0) : 0,
      [SP.RATING_COUNT]: cur.exists ? Number(cur.get(SP.RATING_COUNT) || 0) : 0,
      [SP.JOBS_COMPLETED]: cur.exists ? Number(cur.get(SP.JOBS_COMPLETED) || 0) : 0,
      [SP.CANCELLATIONS]: cur.exists ? Number(cur.get(SP.CANCELLATIONS) || 0) : 0,
      [SP.APPLIED_AT]: now,
      [SP.UPDATED_AT]: now,
    });
  });
  return { status: PartnerStatus.PENDING };
});

export const setPartnerOnline = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const online = data.online === true;
  const ref = db.collection(SP.COLLECTION).doc(uid);
  const p = await ref.get();
  if (p.get(SP.STATUS) !== PartnerStatus.APPROVED) fail("permission-denied", "Your partner account is not approved yet");
  const update: Record<string, unknown> = { [SP.ONLINE]: online, [SP.UPDATED_AT]: Timestamp.now() };
  if (online || data.lat !== undefined) {
    const { lat, lng } = latLng(data);
    if (online) requireServiceArea(await loadConfig(), lat, lng, "partner");
    Object.assign(update, { [SP.LAT]: lat, [SP.LNG]: lng, [SP.LAST_SEEN_AT]: Timestamp.now() });
  }
  await ref.update(update);
  return { online };
});

export const getServiceOffer = onCallSecured({ timeoutSeconds: 10 }, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const bookingId = str(obj(raw), "bookingId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
  const [p, b] = await Promise.all([
    db.collection(SP.COLLECTION).doc(uid).get(),
    db.collection(BK.COLLECTION).doc(bookingId).get(),
  ]);
  if (p.get(SP.STATUS) !== PartnerStatus.APPROVED) fail("permission-denied", "Your partner account is not approved yet");
  if (!b.exists) return { available: false, status: "GONE" };
  const d = b.data() || {};
  const mine = d[BK.PARTNER_ID] === uid;
  const available = d[BK.STATUS] === BS.SEARCHING && ((p.get(SP.CATEGORIES) || []) as string[]).includes(String(d[BK.CATEGORY])) &&
    !((d[BK.EXCLUDED_PARTNER_IDS] || []) as string[]).includes(uid);
  const pLat = Number(p.get(SP.LAT));
  const pLng = Number(p.get(SP.LNG));
  const config = await loadConfig();
  const fee = mine && d[BK.PARTNER_FEE] !== undefined ? Number(d[BK.PARTNER_FEE]) :
    partnerFeeOf(config, Number(p.get(SP.JOBS_COMPLETED) || 0), d);
  const svc = config.services.find((x) => x.id === d[BK.SERVICE_ID]);
  return {
    available,
    mine,
    status: d[BK.STATUS],
    serviceName: d[BK.SERVICE_NAME],
    category: d[BK.CATEGORY],
    price: d[BK.PRICE],
    bookingFee: d[BK.BOOKING_FEE],
    earning: partnerEarning(d, fee),
    partnerFee: fee,
    // What to carry and what the customer keeps ready (ids; labels in getServiceCatalog.items).
    bring: svc ? bringFor(svc) : [],
    provide: svc ? provideFor(svc) : [],
    discount: Number(d[BK.DISCOUNT] || 0),
    customerTotal: Number(d[BK.TOTAL] || 0),
    requiredCreditsPaise: requiredCredits(d, fee),
    creditsPaise: Number(p.get(SP.CREDITS_PAISE) || 0),
    inspection: d[BK.INSPECTION] === true,
    area: d[BK.AREA],
    note: d[BK.NOTE],
    when: d[BK.WHEN],
    scheduledAt: ms(d[BK.SCHEDULED_AT]),
    distanceKm: Number.isFinite(pLat) && Number.isFinite(pLng) ?
      Math.round(distanceKm(pLat, pLng, Number(d[BK.LAT]), Number(d[BK.LNG])) * 10) / 10 : null,
  };
});

export const acceptServiceBooking = onCallSecured({ timeoutSeconds: 20 }, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const config = await loadConfig();
  const bookingId = str(obj(raw), "bookingId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
  const ref = db.collection(BK.COLLECTION).doc(bookingId);
  const partnerRef = db.collection(SP.COLLECTION).doc(uid);
  const out = await db.runTransaction(async (tx) => {
    const [b, p] = await Promise.all([tx.get(ref), tx.get(partnerRef)]);
    if (p.get(SP.STATUS) !== PartnerStatus.APPROVED) return { result: "not_partner" as const };
    if (!b.exists) return { result: "closed" as const };
    const d = b.data() || {};
    if (d[BK.PARTNER_ID] === uid && ASSIGNED_STATUSES.includes(String(d[BK.STATUS]))) return { result: "accepted" as const, d, fresh: false };
    if (d[BK.STATUS] !== BS.SEARCHING) return { result: d[BK.PARTNER_ID] ? "taken" as const : "closed" as const };
    if (ms(d[BK.EXPIRES_AT]) <= Date.now()) return { result: "closed" as const };
    if (((d[BK.EXCLUDED_PARTNER_IDS] || []) as string[]).includes(uid)) return { result: "closed" as const };
    if (!((p.get(SP.CATEGORIES) || []) as string[]).includes(String(d[BK.CATEGORY]))) return { result: "closed" as const };
    const activeId = String(p.get(SP.ACTIVE_BOOKING_ID) || "");
    if (activeId && activeId !== bookingId) {
      const active = await tx.get(db.collection(BK.COLLECTION).doc(activeId));
      if (active.exists && active.get(BK.PARTNER_ID) === uid && ASSIGNED_STATUSES.includes(String(active.get(BK.STATUS)))) {
        return { result: "busy" as const };
      }
    }
    // Also busy while on an urgent job or a fresh regular hire.
    const busy = await busyWith(uid, await tx.get(db.collection(WorkerProfiles.COLLECTION).doc(uid)), tx);
    if (busy && busy !== "service") return { result: "busy" as const };
    const fee = partnerFeeOf(config, Number(p.get(SP.JOBS_COMPLETED) || 0), d);
    if (Number(p.get(SP.CREDITS_PAISE) || 0) < requiredCredits(d, fee)) return { result: "low_credits" as const };
    const now = Timestamp.now();
    const ratingCount = Number(p.get(SP.RATING_COUNT) || 0);
    const rating = ratingCount ? Math.round((Number(p.get(SP.RATING_SUM) || 0) / ratingCount) * 10) / 10 : 0;
    const assigned = {
      [BK.STATUS]: BS.ASSIGNED,
      [BK.PARTNER_ID]: uid,
      [BK.PARTNER_NAME]: String(p.get(SP.NAME) || "Partner"),
      [BK.PARTNER_PHONE]: String(p.get(SP.PHONE) || ""),
      [BK.PARTNER_PHOTO_URL]: String(p.get(SP.PHOTO_URL) || ""),
      [BK.PARTNER_RATING]: rating,
      [BK.PARTNER_FEE]: fee,
      [BK.NEXT_WAVE_AT]: FieldValue.delete(),
      [BK.ASSIGNED_AT]: now,
      [BK.UPDATED_AT]: now,
    };
    tx.update(ref, assigned);
    tx.update(partnerRef, { [SP.ACTIVE_BOOKING_ID]: bookingId, [SP.UPDATED_AT]: now });
    return { result: "accepted" as const, d: { ...d, ...assigned }, fresh: true };
  });
  if (out.result !== "accepted") return { result: out.result };
  const d = out.d;
  if (out.fresh) {
    await tellCustomer(String(d[BK.CUSTOMER_ID]), bookingId, "SERVICE_ASSIGNED", {
      partner: String(d[BK.PARTNER_NAME]), service: String(d[BK.SERVICE_NAME]),
    });
  }
  return {
    result: "accepted",
    bookingId,
    serviceName: d[BK.SERVICE_NAME],
    customerName: d[BK.CUSTOMER_NAME],
    customerPhone: d[BK.CUSTOMER_PHONE],
    addressText: d[BK.ADDRESS_TEXT],
    lat: d[BK.LAT],
    lng: d[BK.LNG],
  };
});

export const updateServiceBooking = onCallSecured({ timeoutSeconds: 20 }, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const bookingId = str(data, "bookingId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
  const action = String(data.action || "");
  if (!["on_the_way", "start", "complete", "cancel"].includes(action)) fail("invalid-argument", "Unknown action");
  const ref = db.collection(BK.COLLECTION).doc(bookingId);
  const partnerRef = db.collection(SP.COLLECTION).doc(uid);
  const secretRef = db.collection(ServiceBookingSecrets.COLLECTION).doc(bookingId);
  const extras = action === "complete" ? int(data, "extras", { min: 0, max: 50_000, optional: true }) : 0;
  const extrasNote = action === "complete" ? text(data, "extrasNote", { max: 200, optional: true }) : "";
  if (extras > 0 && !extrasNote) fail("invalid-argument", "Write what the extra amount is for");
  const config = await loadConfig();

  const out = await db.runTransaction(async (tx) => {
    const [b, p, secret] = await Promise.all([tx.get(ref), tx.get(partnerRef), tx.get(secretRef)]);
    if (!b.exists || b.get(BK.PARTNER_ID) !== uid) fail("not-found", "This job is not assigned to you");
    const d = b.data() || {};
    const status = String(d[BK.STATUS]);
    const now = Timestamp.now();
    switch (action) {
    case "on_the_way":
      if (status !== BS.ASSIGNED) fail("failed-precondition", "Already on the way or started");
      tx.update(ref, { [BK.STATUS]: BS.ON_THE_WAY, [BK.UPDATED_AT]: now });
      return { d, notice: "SERVICE_ON_THE_WAY" };
    case "start": {
      if (status !== BS.ASSIGNED && status !== BS.ON_THE_WAY) fail("failed-precondition", "This job cannot be started now");
      const otp = String(data.otp || "").trim();
      if (!otp || otp !== String(secret.get(ServiceBookingSecrets.START_OTP) || "")) {
        fail("invalid-argument", "Wrong start code. Ask the customer for the 4-digit code in their app.");
      }
      tx.update(ref, { [BK.STATUS]: BS.STARTED, [BK.STARTED_AT]: now, [BK.UPDATED_AT]: now });
      return { d, notice: "SERVICE_STARTED" };
    }
    case "complete": {
      if (status !== BS.STARTED) fail("failed-precondition", "Start the job with the customer's code first");
      const price = Number(d[BK.PRICE] || 0);
      const fee = Number(d[BK.BOOKING_FEE] || 0);
      const discount = Number(d[BK.DISCOUNT] || 0);
      const take = takePaise(price, extras, fee, discount, feeOf(d, config.partnerFee), Number(d[BK.COMMISSION_PCT] || 0));
      const total = Math.max(0, price + fee - discount) + extras;
      const balance = Number(p.get(SP.CREDITS_PAISE) || 0) - take;
      tx.update(ref, {
        [BK.STATUS]: BS.COMPLETED,
        [BK.EXTRAS]: extras,
        [BK.EXTRAS_NOTE]: extrasNote,
        [BK.TOTAL]: total,
        [BK.PLATFORM_TAKE_PAISE]: take,
        [BK.COMPLETED_AT]: now,
        [BK.UPDATED_AT]: now,
      });
      tx.update(partnerRef, {
        [SP.CREDITS_PAISE]: balance,
        [SP.JOBS_COMPLETED]: FieldValue.increment(1),
        [SP.ACTIVE_BOOKING_ID]: FieldValue.delete(),
        [SP.UPDATED_AT]: now,
      });
      tx.create(partnerRef.collection(PartnerLedger.SUBCOLLECTION).doc(), {
        [PartnerLedger.AMOUNT_PAISE]: -take,
        [PartnerLedger.BALANCE_PAISE]: balance,
        [PartnerLedger.KIND]: "JOB",
        [PartnerLedger.BOOKING_ID]: bookingId,
        [PartnerLedger.NOTE]: String(d[BK.SERVICE_NAME] || ""),
        [PartnerLedger.CREATED_AT]: now,
      });
      return { d, notice: "SERVICE_COMPLETED", total, take, balance };
    }
    default: { // cancel by partner → back to searching, never offered to them again
      if (status !== BS.ASSIGNED && status !== BS.ON_THE_WAY) fail("failed-precondition", "A started job cannot be cancelled");
      tx.update(ref, {
        [BK.STATUS]: BS.SEARCHING,
        [BK.PARTNER_ID]: FieldValue.delete(),
        [BK.PARTNER_NAME]: FieldValue.delete(),
        [BK.PARTNER_PHONE]: FieldValue.delete(),
        [BK.PARTNER_PHOTO_URL]: FieldValue.delete(),
        [BK.PARTNER_RATING]: FieldValue.delete(),
        [BK.EXCLUDED_PARTNER_IDS]: FieldValue.arrayUnion(uid),
        [BK.DISPATCH_RADIUS_KM]: 0,
        [BK.NEXT_WAVE_AT]: now,
        // Give the search a fresh window (at least 30 minutes).
        [BK.EXPIRES_AT]: Timestamp.fromMillis(Math.max(ms(d[BK.EXPIRES_AT]), Date.now() + 30 * 60 * 1000)),
        [BK.UPDATED_AT]: now,
      });
      tx.update(partnerRef, {
        [SP.ACTIVE_BOOKING_ID]: FieldValue.delete(),
        [SP.CANCELLATIONS]: FieldValue.increment(1),
        [SP.UPDATED_AT]: now,
      });
      return { d, notice: "SERVICE_PARTNER_CANCELLED" };
    }
    }
  });

  const d = out.d;
  await tellCustomer(String(d[BK.CUSTOMER_ID]), bookingId, out.notice, {
    partner: String(d[BK.PARTNER_NAME] || ""), service: String(d[BK.SERVICE_NAME] || ""), total: "total" in out ? Number(out.total) : 0,
  });
  if (action === "cancel") await advanceServiceWave(bookingId, Date.now());
  return {
    ok: true,
    status: { on_the_way: BS.ON_THE_WAY, start: BS.STARTED, complete: BS.COMPLETED, cancel: BS.SEARCHING }[action],
    ...("total" in out ? { total: out.total, platformTakePaise: out.take, creditsPaise: out.balance } : {}),
  };
});

export const requestPartnerTopup = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const config = await loadConfig();
  const amount = int(data, "amount", { min: config.minTopup, max: 50_000 });
  const utr = str(data, "utr", { max: 12, pattern: /^\d{12}$/ });
  const p = await db.collection(SP.COLLECTION).doc(uid).get();
  if (![PartnerStatus.APPROVED, PartnerStatus.PENDING].includes(p.get(SP.STATUS))) {
    fail("permission-denied", "Apply as a DutyPe partner first");
  }
  const dup = await db.collection(PartnerTopups.COLLECTION).where(PartnerTopups.UTR_NUMBER, "==", utr).limit(1).get();
  if (!dup.empty) fail("already-exists", "This UTR was already submitted");
  const ref = db.collection(PartnerTopups.COLLECTION).doc();
  await ref.create({
    [PartnerTopups.PARTNER_ID]: uid,
    [PartnerTopups.PARTNER_NAME]: String(p.get(SP.NAME) || ""),
    [PartnerTopups.AMOUNT_PAISE]: amount * 100,
    [PartnerTopups.UTR_NUMBER]: utr,
    [PartnerTopups.STATUS]: "PENDING",
    [PartnerTopups.CREATED_AT]: Timestamp.now(),
  });
  return { topupId: ref.id, status: "PENDING" };
});

// ─────────────────────────────── admin ───────────────────────────────

export const reviewServicePartner = onCallSecured({ enforceAppCheck: false }, async (raw: unknown, context) => {
  if (!(await isCallerAdmin(context))) fail("permission-denied", "Admins only");
  const data = obj(raw);
  const partnerId = str(data, "partnerId", { max: 128 });
  const action = String(data.action || "");
  if (!["approve", "reject", "suspend"].includes(action)) fail("invalid-argument", "action must be approve, reject or suspend");
  const reason = str(data, "reason", { max: 300, optional: true });
  const categories = Array.isArray(data.categories) ?
    (data.categories as unknown[]).map((c) => String(c).toUpperCase()).filter((c) => CATEGORY_IDS.includes(c)) : null;
  const ref = db.collection(SP.COLLECTION).doc(partnerId);
  const snap = await ref.get();
  if (!snap.exists) fail("not-found", "Partner not found");
  const status = action === "approve" ? PartnerStatus.APPROVED : action === "reject" ? PartnerStatus.REJECTED : PartnerStatus.SUSPENDED;
  await ref.update({
    [SP.STATUS]: status,
    ...(status !== PartnerStatus.APPROVED ? { [SP.ONLINE]: false } : { [SP.APPROVED_AT]: Timestamp.now() }),
    ...(categories && categories.length ? { [SP.CATEGORIES]: categories } : {}),
    ...(reason ? { [SP.REJECTION_REASON]: reason } : {}),
    [SP.UPDATED_AT]: Timestamp.now(),
  });
  await tellPartner(partnerId, status === PartnerStatus.APPROVED ? "PARTNER_APPROVED" : "PARTNER_REJECTED", { reason: reason || "" },
    { deepLink: "dutype://partner" });
  return { partnerId, status };
});

export const verifyPartnerTopup = onCallSecured({ enforceAppCheck: false }, async (raw: unknown, context) => {
  if (!(await isCallerAdmin(context))) fail("permission-denied", "Admins only");
  const data = obj(raw);
  const topupId = str(data, "topupId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
  const approve = data.approve === true;
  const reason = str(data, "reason", { max: 300, optional: true });
  const ref = db.collection(PartnerTopups.COLLECTION).doc(topupId);
  const out = await db.runTransaction(async (tx) => {
    const t = await tx.get(ref);
    if (!t.exists) fail("not-found", "Top-up not found");
    if (t.get(PartnerTopups.STATUS) !== "PENDING") fail("failed-precondition", "Already processed");
    const partnerId = String(t.get(PartnerTopups.PARTNER_ID));
    const amount = Number(t.get(PartnerTopups.AMOUNT_PAISE) || 0);
    const partnerRef = db.collection(SP.COLLECTION).doc(partnerId);
    const p = await tx.get(partnerRef);
    const now = Timestamp.now();
    tx.update(ref, {
      [PartnerTopups.STATUS]: approve ? "VERIFIED" : "REJECTED",
      [PartnerTopups.VERIFIED_AT]: now,
      ...(reason ? { [PartnerTopups.REJECTION_REASON]: reason } : {}),
    });
    let balance = Number(p.get(SP.CREDITS_PAISE) || 0);
    if (approve) {
      balance += amount;
      tx.update(partnerRef, { [SP.CREDITS_PAISE]: balance, [SP.UPDATED_AT]: now });
      tx.create(partnerRef.collection(PartnerLedger.SUBCOLLECTION).doc(), {
        [PartnerLedger.AMOUNT_PAISE]: amount,
        [PartnerLedger.BALANCE_PAISE]: balance,
        [PartnerLedger.KIND]: "TOPUP",
        [PartnerLedger.TOPUP_ID]: topupId,
        [PartnerLedger.NOTE]: `UPI ${t.get(PartnerTopups.UTR_NUMBER)}`,
        [PartnerLedger.CREATED_AT]: now,
      });
    }
    return { partnerId, amount, balance };
  });
  await tellPartner(out.partnerId, approve ? "PARTNER_TOPUP_VERIFIED" : "PARTNER_TOPUP_REJECTED",
    { amount: Math.round(out.amount / 100), reason: reason || "" }, { deepLink: "dutype://partner" });
  return { topupId, status: approve ? "VERIFIED" : "REJECTED", creditsPaise: out.balance };
});
