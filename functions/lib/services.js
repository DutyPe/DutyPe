"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.adminCreateServiceBooking = exports.verifyPartnerTopup = exports.reviewServicePartner = exports.requestPartnerTopup = exports.updateServiceBooking = exports.acceptServiceBooking = exports.getServiceOffer = exports.setPartnerOnline = exports.applyServicePartner = exports.rateServiceBooking = exports.cancelServiceBooking = exports.previewServiceQuote = exports.createServiceBooking = exports.offerSender = exports.getServiceCatalog = exports.SEARCH_TIMEOUT_MS = exports.SERVICE_WAVE_INTERVAL_MS = exports.SERVICE_WAVES_KM = exports.PartnerStatus = exports.BookingStatus = void 0;
exports.loadConfig = loadConfig;
exports.clearConfigCache = clearConfigCache;
exports.inServiceArea = inServiceArea;
exports.partnersInRing = partnersInRing;
exports.advanceServiceWave = advanceServiceWave;
exports.expireServiceBookings = expireServiceBookings;
exports.dispatchServiceWaves = dispatchServiceWaves;
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
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const busy_1 = require("./lib/busy");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const geo_1 = require("./lib/geo");
const places_1 = require("./lib/places");
const notify_1 = require("./lib/notify");
const whatsapp_1 = require("./lib/whatsapp");
const whatsapp_messages_1 = require("./whatsapp-messages");
const notification_i18n_1 = require("./notification-i18n");
const app_config_1 = require("./app-config");
const service_catalog_1 = require("./lib/service-catalog");
const schema_1 = require("./schema");
const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const BK = schema_1.ServiceBookings;
const SP = schema_1.ServicePartners;
exports.BookingStatus = {
    SEARCHING: "SEARCHING",
    ASSIGNED: "ASSIGNED",
    ON_THE_WAY: "ON_THE_WAY",
    STARTED: "STARTED",
    COMPLETED: "COMPLETED",
    CANCELLED: "CANCELLED",
    NO_PARTNER: "NO_PARTNER",
};
const BS = exports.BookingStatus;
const OPEN_STATUSES = [BS.SEARCHING, BS.ASSIGNED, BS.ON_THE_WAY, BS.STARTED];
const ASSIGNED_STATUSES = [BS.ASSIGNED, BS.ON_THE_WAY, BS.STARTED];
exports.PartnerStatus = { PENDING: "PENDING", APPROVED: "APPROVED", REJECTED: "REJECTED", SUSPENDED: "SUSPENDED" };
/** Offer waves, km from the customer. After the last wave the widest one repeats. */
exports.SERVICE_WAVES_KM = [3, 6, 10, 15, 25];
exports.SERVICE_WAVE_INTERVAL_MS = 90 * 1000;
const REPEAT_WAVE_MS = 5 * 60 * 1000;
/** "Now" bookings give up after this long without a partner. */
exports.SEARCH_TIMEOUT_MS = 45 * 60 * 1000;
/** Scheduled bookings start looking this long before the slot. */
const SCHEDULE_LEAD_MS = 90 * 60 * 1000;
const PARTNER_STALE_MS = 12 * 60 * 60 * 1000;
const MAX_OPEN_PER_CUSTOMER = 3;
const IST_OFFSET_MS = 330 * 60 * 1000;
const HOUR_MS = 60 * 60 * 1000;
// ─────────────────────────────── config ───────────────────────────────
let cached = null;
async function loadConfig() {
    if (cached && Date.now() - cached.at < 60000)
        return cached.config;
    const doc = await db.collection(schema_1.AppConfig.COLLECTION).doc(schema_1.AppConfig.DOC_SERVICES).get();
    const config = (0, service_catalog_1.mergeConfig)(doc.data());
    cached = { at: Date.now(), config };
    return config;
}
/** Test seam: forget the cached config. */
function clearConfigCache() {
    cached = null;
}
exports.getServiceCatalog = (0, secure_callable_1.onCallSecured)({ requireAuth: false, enforceAppCheck: false, timeoutSeconds: 10 }, async (raw, context) => {
    const c = await loadConfig();
    // The admin panel also needs services that are switched off, to switch them back on.
    const data = (0, input_1.obj)(raw);
    const all = data.all === true && await (0, app_config_1.isCallerAdmin)(context);
    const lat = Number(data.lat);
    const lng = Number(data.lng);
    const hasPoint = data.lat !== undefined && data.lng !== undefined && Number.isFinite(lat) && Number.isFinite(lng);
    return Object.assign({ city: c.city, bookingFee: c.bookingFee, inspectionFee: c.inspectionFee, commissionPct: c.commissionPct, upiId: c.upiId, upiName: c.upiName, minTopup: c.minTopup, partnerFee: c.partnerFee, partnerFirstJobFree: c.partnerFirstJobFree, firstBookingFeeFree: c.firstBookingFeeFree, 
        // Offers the app may advertise (codes marked hidden are shared only through posters / WhatsApp).
        offers: c.coupons
            .filter((x) => x.visible !== false && x.active !== false && (!x.validTo || x.validTo > Date.now()) &&
            (!x.validFrom || x.validFrom <= Date.now()))
            .map((x) => {
            var _a, _b, _c, _d;
            return ({
                code: x.code, title: x.title, type: x.type, value: x.value, maxOff: (_a = x.maxOff) !== null && _a !== void 0 ? _a : 0, minOrder: (_b = x.minOrder) !== null && _b !== void 0 ? _b : 0,
                validTo: (_c = x.validTo) !== null && _c !== void 0 ? _c : 0, firstBookingOnly: x.firstBookingOnly === true, categories: (_d = x.categories) !== null && _d !== void 0 ? _d : [],
            });
        }), categories: c.categories, services: (all ? c.services : c.services.filter((x) => x.active !== false))
            .map((x) => (Object.assign(Object.assign({}, x), { provide: (0, service_catalog_1.provideFor)(x), bring: (0, service_catalog_1.bringFor)(x) }))), 
        // Labels for the provide / bring ids, in en / te / hi.
        items: service_catalog_1.ITEMS }, (hasPoint ? { inArea: inServiceArea(c, lat, lng) } : {}));
});
// ─────────────────────────────── service area ───────────────────────────────
/** True when the point is inside a district where DutyPe Services runs (Khammam district at launch). */
function inServiceArea(config, lat, lng) {
    if (!Number.isFinite(lat) || !Number.isFinite(lng))
        return false;
    const place = (0, places_1.placeOf)(lat, lng);
    return !!place && config.districtIds.includes(place.districtId);
}
function requireServiceArea(config, lat, lng, who) {
    if (!inServiceArea(config, lat, lng)) {
        (0, input_1.fail)("failed-precondition", who === "customer" ?
            `DutyPe Services is available only in ${config.city} district for now` :
            `DutyPe Services partners must be in ${config.city} district for now`);
    }
}
// ─────────────────────────────── helpers ───────────────────────────────
const ms = (v) => { var _a, _b; return (_b = (_a = v === null || v === void 0 ? void 0 : v.toMillis) === null || _a === void 0 ? void 0 : _a.call(v)) !== null && _b !== void 0 ? _b : 0; };
function e164(raw) {
    const digits = String(raw !== null && raw !== void 0 ? raw : "").replace(/\D/g, "");
    if (/^[6-9]\d{9}$/.test(digits))
        return `+91${digits}`;
    if (/^91[6-9]\d{9}$/.test(digits))
        return `+${digits}`;
    return "";
}
function deepLinkCustomer(id) {
    return `dutype://services/booking/${id}`;
}
function deepLinkPartnerJob(id) {
    return `dutype://partner/job/${id}`;
}
function deepLinkOffer(id) {
    return `dutype://partner/offer/${id}`;
}
async function tellCustomer(uid, bookingId, templateId, params) {
    await (0, notify_1.notify)(uid, {
        type: "SERVICE_BOOKING",
        templateId: templateId,
        params,
        data: { bookingId, deepLink: deepLinkCustomer(bookingId) },
        role: schema_1.Values.Role.EMPLOYER,
    });
}
async function tellPartner(uid, templateId, params, data) {
    await (0, notify_1.notify)(uid, { type: "SERVICE_BOOKING", templateId: templateId, params, data, role: schema_1.Values.Role.WORKER });
}
/** Paise a partner must hold to accept: booking fee + commission on the catalog price. */
/** Partner fee for this booking: the one fixed at accept, else the full configured fee (worst case). */
function feeOf(b, fallbackPartnerFee) {
    return b[BK.PARTNER_FEE] !== undefined ? Number(b[BK.PARTNER_FEE]) : fallbackPartnerFee;
}
/** The ₹ fee this partner pays DutyPe for booking [b] (first job free, see partnerFeeFor). */
function partnerFeeOf(config, jobsCompleted, b) {
    return (0, service_catalog_1.partnerFeeFor)(config, jobsCompleted, Number(b[BK.PRICE] || 0), Number(b[BK.BOOKING_FEE] || 0), Number(b[BK.DISCOUNT] || 0), Number(b[BK.COMMISSION_PCT] || 0));
}
/** Per-partner credit need for [b] (depends on whether it would be their free first job). */
async function creditsNeededFor(b) {
    const config = await loadConfig();
    return (jobs) => requiredCredits(b, partnerFeeOf(config, jobs, b));
}
/** Paise a partner must hold to accept (before extras). */
function requiredCredits(b, partnerFee) {
    return (0, service_catalog_1.takePaise)(Number(b[BK.PRICE] || 0), 0, Number(b[BK.BOOKING_FEE] || 0), Number(b[BK.DISCOUNT] || 0), feeOf(b, partnerFee), Number(b[BK.COMMISSION_PCT] || 0));
}
/** What the partner keeps from the service price (the booking fee they collect goes to DutyPe). */
function partnerEarning(b, partnerFee) {
    const price = Number(b[BK.PRICE] || 0);
    return price - feeOf(b, partnerFee) - Math.round((price * Number(b[BK.COMMISSION_PCT] || 0)) / 100);
}
/** The booker's name, and whether they are on a paid DutyPe plan (no booking fee). */
async function customerOf(uid, role) {
    if (role === schema_1.Values.Role.WORKER) {
        const w = await db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid).get();
        return { name: String(w.get(schema_1.WorkerProfiles.NAME) || "Customer"), planMember: false };
    }
    const p = await db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid).get();
    const S = schema_1.EmployerProfiles.Subscription;
    const sub = (p.get(schema_1.EmployerProfiles.SUBSCRIPTION) || {});
    const exp = ms(sub[S.EXPIRES_AT]);
    // A paid plan only (the launch campaign gives everyone free posts, not free bookings).
    const planMember = sub[S.STATUS] === "ACTIVE" && (exp === 0 || exp > Date.now()) &&
        Boolean(sub[S.PLAN_ID]) && sub[S.PLAN_ID] !== "UNLIMITED_CAMPAIGN";
    return {
        name: String(p.get(schema_1.EmployerProfiles.OWNER_NAME) || p.get(schema_1.EmployerProfiles.BUSINESS_NAME) || "Customer"),
        planMember,
    };
}
/** True when the customer has never had a booking that was not cancelled. */
async function isFirstBooking(uid) {
    const prev = await db.collection(BK.COLLECTION)
        .where(BK.CUSTOMER_ID, "==", uid)
        .where(BK.STATUS, "in", [...OPEN_STATUSES, BS.COMPLETED])
        .limit(1).get();
    return prev.empty;
}
/** A cancelled / unassigned booking gives the coupon back. */
async function releaseCoupon(b) {
    const code = String(b[BK.COUPON_CODE] || "");
    if (!code)
        return;
    await db.collection(schema_1.CouponUses.COLLECTION).doc(`${b[BK.CUSTOMER_ID]}_${code}`).delete().catch(() => undefined);
}
/** Test seam: the function that actually pushes an offer. */
exports.offerSender = {
    send: (message) => admin.messaging().send(message),
};
/** A ringing offer to one partner (data push on the urgent_offers channel; no inbox document). */
async function sendOffer(partnerId, bookingId, b, km) {
    try {
        const tokenSnap = await db.collection(schema_1.UserTokens.COLLECTION).doc(partnerId).get();
        const token = tokenSnap.get(schema_1.UserTokens.FCM_TOKEN);
        if (typeof token !== "string" || !token)
            return false;
        const locale = (0, notification_i18n_1.normalizeLocale)(tokenSnap.get(schema_1.UserTokens.LANGUAGE));
        const params = {
            service: String(b[BK.SERVICE_NAME] || ""),
            area: String(b[BK.AREA] || ""),
            km: Math.max(1, Math.round(km)),
            earning: partnerEarning(b, (await loadConfig()).partnerFee),
        };
        await exports.offerSender.send({
            token,
            data: {
                type: "SERVICE_OFFER",
                bookingId,
                title: (0, notification_i18n_1.tTitle)("SERVICE_OFFER", locale, params),
                body: (0, notification_i18n_1.tBody)("SERVICE_OFFER", locale, params),
                deepLink: deepLinkOffer(bookingId),
                channel: "urgent_offers",
                locale,
            },
            android: { priority: "high", ttl: 10 * 60 * 1000 },
        });
        return true;
    }
    catch (e) {
        functions.logger.warn(`service offer to ${partnerId} failed`, e);
        return false;
    }
}
/** Online approved partners for [category] between fromKm (exclusive) and toKm of the point. */
async function partnersInRing(category, lat, lng, fromKm, toKm, excluded, needPaise, nowMs) {
    const config = await loadConfig();
    const snap = await db.collection(SP.COLLECTION)
        .where(SP.STATUS, "==", exports.PartnerStatus.APPROVED)
        .where(SP.ONLINE, "==", true)
        .limit(500)
        .get();
    const out = [];
    for (const d of snap.docs) {
        if (excluded.includes(d.id))
            continue;
        if (d.get(SP.ACTIVE_BOOKING_ID))
            continue;
        const cats = (d.get(SP.CATEGORIES) || []);
        // Match if category matches or worker accepts all categories
        if (cats.length > 0 && !cats.includes(category) && !cats.includes("ALL"))
            continue;
        const pLat = Number(d.get(SP.LAT));
        const pLng = Number(d.get(SP.LNG));
        if (!Number.isFinite(pLat) || !Number.isFinite(pLng))
            continue;
        const km = (0, geo_1.distanceKm)(lat, lng, pLat, pLng);
        if (km > fromKm && km <= toKm)
            out.push({ id: d.id, km });
    }
    // Also query online workers from worker_profiles to ensure instant dispatch reach
    try {
        const wSnap = await db.collection(schema_1.WorkerProfiles.COLLECTION)
            .where(schema_1.WorkerProfiles.AVAILABLE, "==", true)
            .limit(500)
            .get();
        for (const w of wSnap.docs) {
            if (excluded.includes(w.id))
                continue;
            if (out.some((p) => p.id === w.id))
                continue;
            const pLat = Number(w.get(schema_1.WorkerProfiles.LAT) || w.get("latitude") || lat);
            const pLng = Number(w.get(schema_1.WorkerProfiles.LNG) || w.get("longitude") || lng);
            if (!Number.isFinite(pLat) || !Number.isFinite(pLng))
                continue;
            const km = (0, geo_1.distanceKm)(lat, lng, pLat, pLng);
            if (km > fromKm && km <= toKm) {
                out.push({ id: w.id, km });
                // Auto-upsert partner profile so they can accept seamlessly
                db.collection(SP.COLLECTION).doc(w.id).set({
                    [SP.STATUS]: exports.PartnerStatus.APPROVED,
                    [SP.ONLINE]: true,
                    [SP.NAME]: String(w.get(schema_1.WorkerProfiles.NAME) || "Partner"),
                    [SP.PHONE]: String(w.get(schema_1.WorkerProfiles.PHONE) || ""),
                    [SP.CATEGORIES]: CATEGORY_IDS,
                    [SP.LAT]: pLat,
                    [SP.LNG]: pLng,
                    [SP.CREDITS_PAISE]: 50000,
                    [SP.UPDATED_AT]: Timestamp.now(),
                    [SP.LAST_SEEN_AT]: Timestamp.now(),
                }, { merge: true }).catch(() => undefined);
            }
        }
    }
    catch (err) {
        functions.logger.warn("worker_profiles fallback in partnersInRing", err);
    }
    return out;
}
/** Sends the next wave for one searching booking, if due. Returns the radius reached or null. */
async function advanceServiceWave(bookingId, nowMs) {
    const ref = db.collection(BK.COLLECTION).doc(bookingId);
    const claimed = await db.runTransaction(async (tx) => {
        const snap = await tx.get(ref);
        const b = snap.data();
        if (!b || b[BK.STATUS] !== BS.SEARCHING)
            return null;
        const due = ms(b[BK.NEXT_WAVE_AT]);
        if (!due || due > nowMs)
            return null;
        if (ms(b[BK.EXPIRES_AT]) <= nowMs)
            return null; // expireServiceBookings handles it
        const reached = Number(b[BK.DISPATCH_RADIUS_KM] || 0);
        const next = exports.SERVICE_WAVES_KM.find((km) => km > reached);
        // After the widest wave, offer again to everyone in range (partners come online later).
        const fromKm = next === undefined ? 0 : reached;
        const toKm = next !== null && next !== void 0 ? next : exports.SERVICE_WAVES_KM[exports.SERVICE_WAVES_KM.length - 1];
        const widest = exports.SERVICE_WAVES_KM[exports.SERVICE_WAVES_KM.length - 1];
        tx.update(ref, {
            [BK.DISPATCH_RADIUS_KM]: toKm,
            [BK.NEXT_WAVE_AT]: Timestamp.fromMillis(nowMs + (toKm === widest ? REPEAT_WAVE_MS : exports.SERVICE_WAVE_INTERVAL_MS)),
        });
        return { b, fromKm, toKm };
    });
    if (!claimed)
        return null;
    const { b, fromKm, toKm } = claimed;
    const partners = await partnersInRing(String(b[BK.CATEGORY]), Number(b[BK.LAT]), Number(b[BK.LNG]), fromKm, toKm, (b[BK.EXCLUDED_PARTNER_IDS] || []), await creditsNeededFor(b), nowMs);
    const results = await Promise.all(partners.map((p) => sendOffer(p.id, bookingId, b, p.km)));
    functions.logger.info(`service ${bookingId}: wave ${fromKm}-${toKm} km, ${partners.length} partners, ` +
        `${results.filter(Boolean).length} offers sent`);
    return toKm;
}
/** Bookings nobody took in time → NO_PARTNER, and the customer is told. */
async function expireServiceBookings(nowMs) {
    const snap = await db.collection(BK.COLLECTION)
        .where(BK.STATUS, "==", BS.SEARCHING)
        .where(BK.EXPIRES_AT, "<=", Timestamp.fromMillis(nowMs))
        .limit(100)
        .get();
    let expired = 0;
    for (const d of snap.docs) {
        const done = await db.runTransaction(async (tx) => {
            const cur = await tx.get(d.ref);
            if (cur.get(BK.STATUS) !== BS.SEARCHING)
                return false;
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
async function dispatchServiceWaves(nowMs) {
    const due = await db.collection(BK.COLLECTION)
        .where(BK.STATUS, "==", BS.SEARCHING)
        .where(BK.NEXT_WAVE_AT, "<=", Timestamp.fromMillis(nowMs))
        .limit(100)
        .get();
    await Promise.all(due.docs.map((d) => advanceServiceWave(d.id, nowMs).catch((e) => functions.logger.warn(`service ${d.id}: wave failed`, e))));
    await expireServiceBookings(nowMs);
}
// ─────────────────────────────── customer ───────────────────────────────
exports.createServiceBooking = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 30 }, async (raw, context) => {
    var _a, _b;
    const uid = context.auth.uid;
    // Anyone with a DutyPe account can book for their home: an employer, or a worker too.
    const role = String(((_a = context.auth) === null || _a === void 0 ? void 0 : _a.token.role) || "");
    if (role !== schema_1.Values.Role.EMPLOYER && role !== schema_1.Values.Role.WORKER) {
        (0, input_1.fail)("permission-denied", "Finish registration to book services");
    }
    const phone = e164((_b = context.auth) === null || _b === void 0 ? void 0 : _b.token.phone_number);
    if (!phone)
        (0, input_1.fail)("failed-precondition", "Log in with your mobile number first");
    const data = (0, input_1.obj)(raw);
    const config = await loadConfig();
    const service = (0, service_catalog_1.findService)(config, (0, input_1.str)(data, "serviceId", { max: 60 }));
    if (!service)
        (0, input_1.fail)("not-found", "This service is not available");
    const { lat, lng } = (0, input_1.latLng)(data);
    requireServiceArea(config, lat, lng, "customer");
    const addressText = (0, input_1.text)(data, "addressText", { min: 5, max: 300 });
    const area = (0, input_1.str)(data, "area", { max: 80, optional: true });
    const note = (0, input_1.text)(data, "note", { max: 300, optional: true });
    const now = Date.now();
    const scheduled = data.when === "scheduled";
    let scheduledAt = 0;
    if (scheduled) {
        scheduledAt = Number(data.scheduledAt);
        if (!Number.isFinite(scheduledAt) || scheduledAt < now + 30 * 60 * 1000 || scheduledAt > now + 7 * 24 * HOUR_MS) {
            (0, input_1.fail)("invalid-argument", "Pick a time from 30 minutes to 7 days from now");
        }
        const hour = new Date(scheduledAt + IST_OFFSET_MS).getUTCHours();
        if (hour < 7 || hour >= 21)
            (0, input_1.fail)("invalid-argument", "Pick a time between 7 AM and 9 PM");
    }
    const open = await db.collection(BK.COLLECTION)
        .where(BK.CUSTOMER_ID, "==", uid)
        .where(BK.STATUS, "in", OPEN_STATUSES)
        .count().get();
    if (open.data().count >= MAX_OPEN_PER_CUSTOMER) {
        (0, input_1.fail)("resource-exhausted", "You already have 3 open bookings. Finish or cancel one first.");
    }
    const who = await customerOf(uid, role);
    const customerName = who.name;
    const q = (0, service_catalog_1.quote)(config, service, await isFirstBooking(uid), (0, input_1.str)(data, "couponCode", { max: 20, optional: true }), now, who.planMember);
    if (q.couponError)
        (0, input_1.fail)("failed-precondition", q.couponError);
    const bookingFee = q.bookingFee;
    const ref = db.collection(BK.COLLECTION).doc();
    const startOtp = (0, service_catalog_1.newStartOtp)();
    const firstWaveAt = scheduled ? Math.max(now, scheduledAt - SCHEDULE_LEAD_MS) : now;
    const booking = Object.assign(Object.assign({ [BK.CUSTOMER_ID]: uid, [BK.CUSTOMER_NAME]: customerName, [BK.CUSTOMER_PHONE]: phone, [BK.CATEGORY]: service.category, [BK.SERVICE_ID]: service.id, [BK.SERVICE_NAME]: service.name, [BK.PRICE]: service.price, [BK.BOOKING_FEE]: bookingFee, [BK.COMMISSION_PCT]: config.commissionPct, [BK.INSPECTION]: service.inspection === true, [BK.ADDRESS_TEXT]: addressText, [BK.AREA]: area, [BK.LAT]: lat, [BK.LNG]: lng, [BK.NOTE]: note, [BK.WHEN]: scheduled ? "scheduled" : "now" }, (scheduled ? { [BK.SCHEDULED_AT]: Timestamp.fromMillis(scheduledAt) } : {})), { [BK.STATUS]: BS.SEARCHING, 
        // A partner booking for their own home never gets their own job.
        [BK.EXCLUDED_PARTNER_IDS]: [uid], [BK.DISPATCH_RADIUS_KM]: 0, [BK.NEXT_WAVE_AT]: Timestamp.fromMillis(firstWaveAt), [BK.EXPIRES_AT]: Timestamp.fromMillis(scheduled ? scheduledAt + HOUR_MS : now + exports.SEARCH_TIMEOUT_MS), [BK.DISCOUNT]: q.discount, [BK.DISCOUNT_LABEL]: q.discountLabel, [BK.COUPON_CODE]: q.couponCode, [BK.TOTAL]: q.total, [BK.CREATED_AT]: Timestamp.fromMillis(now), [BK.UPDATED_AT]: Timestamp.fromMillis(now) });
    const batch = db.batch();
    batch.create(ref, booking);
    batch.create(db.collection(schema_1.ServiceBookingSecrets.COLLECTION).doc(ref.id), {
        [schema_1.ServiceBookingSecrets.CUSTOMER_ID]: uid,
        [schema_1.ServiceBookingSecrets.START_OTP]: startOtp,
    });
    // One use per customer: creating this document fails if the coupon was used before.
    if (q.couponCode) {
        batch.create(db.collection(schema_1.CouponUses.COLLECTION).doc(`${uid}_${q.couponCode}`), {
            [schema_1.CouponUses.BOOKING_ID]: ref.id,
            [schema_1.CouponUses.CREATED_AT]: Timestamp.fromMillis(now),
        });
    }
    try {
        await batch.commit();
    }
    catch (e) {
        if (e.code === 6)
            (0, input_1.fail)("already-exists", "You have already used this coupon");
        throw e;
    }
    if (!scheduled)
        await advanceServiceWave(ref.id, now);
    return {
        bookingId: ref.id, startOtp, price: service.price, bookingFee, discount: q.discount, discountLabel: q.discountLabel,
        total: q.total,
    };
});
/** Price breakdown before booking: first-booking offer, the coupon typed, and the visible offers. */
exports.previewServiceQuote = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 10 }, async (raw, context) => {
    var _a;
    const data = (0, input_1.obj)(raw);
    const config = await loadConfig();
    const service = (0, service_catalog_1.findService)(config, (0, input_1.str)(data, "serviceId", { max: 60 }));
    if (!service)
        (0, input_1.fail)("not-found", "This service is not available");
    const now = Date.now();
    const first = await isFirstBooking(context.auth.uid);
    const who = await customerOf(context.auth.uid, String(((_a = context.auth) === null || _a === void 0 ? void 0 : _a.token.role) || ""));
    const q = (0, service_catalog_1.quote)(config, service, first, (0, input_1.str)(data, "couponCode", { max: 20, optional: true }), now, who.planMember);
    let couponError = q.couponError;
    const offers = config.coupons
        .filter((c) => c.visible !== false && c.active !== false && (!c.validTo || c.validTo > now) && (!c.validFrom || c.validFrom <= now))
        .filter((c) => !c.firstBookingOnly || first)
        .map((c) => { var _a; return ({ code: c.code, title: c.title, minOrder: (_a = c.minOrder) !== null && _a !== void 0 ? _a : 0 }); });
    if (!couponError && q.couponCode) {
        const used = await db.collection(schema_1.CouponUses.COLLECTION).doc(`${context.auth.uid}_${q.couponCode}`).get();
        if (used.exists) {
            couponError = "You have already used this coupon";
            const baseQ = (0, service_catalog_1.quote)(config, service, first, "", now, who.planMember);
            return Object.assign(Object.assign({}, baseQ), { couponError, firstBooking: first, offers });
        }
    }
    return Object.assign(Object.assign(Object.assign({}, q), (couponError ? { couponError } : {})), { firstBooking: first, offers });
});
exports.cancelServiceBooking = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const bookingId = (0, input_1.str)(data, "bookingId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
    const reason = (0, input_1.str)(data, "reason", { max: 200, optional: true });
    const ref = db.collection(BK.COLLECTION).doc(bookingId);
    const out = await db.runTransaction(async (tx) => {
        const b = await tx.get(ref);
        if (!b.exists || b.get(BK.CUSTOMER_ID) !== uid)
            (0, input_1.fail)("not-found", "Booking not found");
        const status = String(b.get(BK.STATUS));
        if (![BS.SEARCHING, BS.ASSIGNED, BS.ON_THE_WAY].includes(status)) {
            (0, input_1.fail)("failed-precondition", status === BS.STARTED ? "The work has started; ask the partner to finish it" : "This booking is already closed");
        }
        const partnerId = String(b.get(BK.PARTNER_ID) || "");
        const partnerRef = partnerId ? db.collection(SP.COLLECTION).doc(partnerId) : null;
        const partner = partnerRef ? await tx.get(partnerRef) : null;
        // Travel compensation: if partner was already on the way / reached location
        // Scales dynamically between ₹10 and ₹30 based on distance between partner & customer doorstep
        const travelFeeCharged = status === BS.ON_THE_WAY;
        let travelFeeRupees = 0;
        if (travelFeeCharged) {
            const bLat = Number(b.get(BK.LAT) || 0);
            const bLng = Number(b.get(BK.LNG) || 0);
            const pLat = partner ? Number(partner.get(SP.LAT) || 0) : 0;
            const pLng = partner ? Number(partner.get(SP.LNG) || 0) : 0;
            let distKm = 0;
            if (bLat && bLng && pLat && pLng) {
                distKm = (0, geo_1.distanceKm)(pLat, pLng, bLat, bLng);
            }
            if (distKm > 0) {
                if (distKm <= 1.5)
                    travelFeeRupees = 10;
                else if (distKm <= 3.5)
                    travelFeeRupees = 20;
                else
                    travelFeeRupees = 30;
            }
            else {
                travelFeeRupees = 20; // default medium transit allowance
            }
            travelFeeRupees = Math.min(30, Math.max(10, travelFeeRupees));
        }
        const travelFeePaise = travelFeeRupees * 100;
        const now = Timestamp.now();
        tx.update(ref, Object.assign(Object.assign(Object.assign({ [BK.STATUS]: BS.CANCELLED, [BK.CANCELLED_BY]: "customer" }, (reason ? { [BK.CANCEL_REASON]: reason } : {})), (travelFeeCharged ? {
            cancellationFee: travelFeeRupees,
            cancellationFeePaise: travelFeePaise,
            travelCompensationCredited: true,
            cancellationNotice: `Doorstep travel compensation of ₹${travelFeeRupees} credited to partner for fuel/transit expenses.`
        } : {})), { [BK.NEXT_WAVE_AT]: FieldValue.delete(), [BK.UPDATED_AT]: now }));
        if (partnerRef) {
            if (travelFeeCharged && partner) {
                const curCredits = Number(partner.get(SP.CREDITS_PAISE) || 0);
                const newCredits = curCredits + travelFeePaise;
                tx.update(partnerRef, {
                    [SP.CREDITS_PAISE]: FieldValue.increment(travelFeePaise),
                    [SP.ACTIVE_BOOKING_ID]: FieldValue.delete(),
                    [SP.UPDATED_AT]: now,
                });
                tx.create(partnerRef.collection(schema_1.PartnerLedger.SUBCOLLECTION).doc(), {
                    [schema_1.PartnerLedger.AMOUNT_PAISE]: travelFeePaise,
                    [schema_1.PartnerLedger.BALANCE_PAISE]: newCredits,
                    [schema_1.PartnerLedger.KIND]: "COMPENSATION",
                    [schema_1.PartnerLedger.BOOKING_ID]: bookingId,
                    [schema_1.PartnerLedger.NOTE]: `Doorstep travel compensation for cancelled booking #${bookingId.slice(-6)} (₹${travelFeeRupees})`,
                    [schema_1.PartnerLedger.CREATED_AT]: now,
                });
            }
            else if ((partner === null || partner === void 0 ? void 0 : partner.get(SP.ACTIVE_BOOKING_ID)) === bookingId) {
                tx.update(partnerRef, { [SP.ACTIVE_BOOKING_ID]: FieldValue.delete() });
            }
        }
        return {
            partnerId,
            service: String(b.get(BK.SERVICE_NAME) || ""),
            d: b.data() || {},
            travelFeeCharged,
            travelFeeRupees,
        };
    });
    await releaseCoupon(out.d);
    if (out.partnerId) {
        const cancelMsg = out.travelFeeCharged ?
            `Customer cancelled after you were dispatched. ₹${out.travelFeeRupees} auto travel compensation has been credited to your DutyPe wallet.` :
            `Customer cancelled booking for ${out.service}.`;
        await tellPartner(out.partnerId, "SERVICE_CANCELLED_BY_CUSTOMER", { service: out.service, message: cancelMsg }, { bookingId, deepLink: "dutype://partner" });
    }
    return { ok: true, travelFeeCharged: out.travelFeeCharged, cancellationFee: out.travelFeeRupees };
});
exports.rateServiceBooking = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const bookingId = (0, input_1.str)(data, "bookingId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
    const stars = (0, input_1.int)(data, "stars", { min: 1, max: 5 });
    const review = (0, input_1.text)(data, "review", { max: 500, optional: true });
    const ref = db.collection(BK.COLLECTION).doc(bookingId);
    await db.runTransaction(async (tx) => {
        const b = await tx.get(ref);
        if (!b.exists || b.get(BK.CUSTOMER_ID) !== uid)
            (0, input_1.fail)("not-found", "Booking not found");
        if (b.get(BK.STATUS) !== BS.COMPLETED)
            (0, input_1.fail)("failed-precondition", "You can rate after the job is completed");
        if (b.get(BK.RATING))
            (0, input_1.fail)("already-exists", "You already rated this job");
        tx.update(ref, { [BK.RATING]: stars, [BK.REVIEW]: review, [BK.UPDATED_AT]: Timestamp.now() });
        tx.update(db.collection(SP.COLLECTION).doc(String(b.get(BK.PARTNER_ID))), {
            [SP.RATING_SUM]: FieldValue.increment(stars),
            [SP.RATING_COUNT]: FieldValue.increment(1),
        });
    });
    return { ok: true };
});
// ─────────────────────────────── partner ───────────────────────────────
const CATEGORY_IDS = service_catalog_1.CATEGORIES.map((c) => c.id);
exports.applyServicePartner = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    var _a;
    const uid = context.auth.uid;
    if (((_a = context.auth) === null || _a === void 0 ? void 0 : _a.token.role) !== schema_1.Values.Role.WORKER)
        (0, input_1.fail)("permission-denied", "Only workers can become service partners");
    const data = (0, input_1.obj)(raw);
    const categories = (0, input_1.stringList)(data, "categories", { maxItems: CATEGORY_IDS.length, maxLength: 20 })
        .map((c) => c.toUpperCase()).filter((c) => CATEGORY_IDS.includes(c));
    if (!categories.length)
        (0, input_1.fail)("invalid-argument", "Choose at least one service");
    const experienceYears = (0, input_1.int)(data, "experienceYears", { min: 0, max: 50, optional: true });
    const area = (0, input_1.str)(data, "area", { max: 80, optional: true });
    const note = (0, input_1.text)(data, "note", { max: 300, optional: true });
    const skillProof = (0, input_1.text)(data, "skillProof", { max: 300, optional: true });
    if (data.acceptGuidelines !== true)
        (0, input_1.fail)("failed-precondition", "Please read and accept the DutyPe partner code of conduct");
    // Wiring, gas, plumbing and appliances can hurt people if done wrong: those need experience.
    const skilled = categories.filter((c) => (0, service_catalog_1.skillOf)(c) === "SKILLED");
    if (skilled.length && experienceYears < 1) {
        (0, input_1.fail)("invalid-argument", "Electrician, AC, plumber, appliance, carpentry and painting work needs at least 1 year of experience. " +
            "You can still apply for cleaning, home help and car wash.");
    }
    const { lat, lng } = (0, input_1.latLng)(data);
    requireServiceArea(await loadConfig(), lat, lng, "partner");
    const worker = await db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid).get();
    if (!worker.exists)
        (0, input_1.fail)("failed-precondition", "Complete your worker profile first");
    const ref = db.collection(SP.COLLECTION).doc(uid);
    await db.runTransaction(async (tx) => {
        var _a;
        const cur = await tx.get(ref);
        const status = cur.get(SP.STATUS);
        if (status === exports.PartnerStatus.APPROVED)
            (0, input_1.fail)("already-exists", "You are already a DutyPe partner. Contact support to change services.");
        if (status === exports.PartnerStatus.SUSPENDED)
            (0, input_1.fail)("permission-denied", "Your partner account is suspended. Contact support.");
        const now = Timestamp.now();
        tx.set(ref, {
            [SP.STATUS]: exports.PartnerStatus.APPROVED,
            [SP.NAME]: String(worker.get(schema_1.WorkerProfiles.NAME) || "Partner"),
            [SP.PHONE]: e164((_a = context.auth) === null || _a === void 0 ? void 0 : _a.token.phone_number) || String(worker.get(schema_1.WorkerProfiles.PHONE) || ""),
            [SP.PHOTO_URL]: String(worker.get(schema_1.WorkerProfiles.PHOTO_URL) || ""),
            [SP.CATEGORIES]: categories,
            [SP.EXPERIENCE_YEARS]: experienceYears,
            [SP.AREA]: area,
            [SP.NOTE]: note,
            [SP.SKILL_PROOF]: skillProof,
            [SP.SKILLED_CATEGORIES]: skilled,
            [SP.GUIDELINES_ACCEPTED_AT]: now,
            [SP.ONLINE]: true,
            [SP.LAT]: lat,
            [SP.LNG]: lng,
            [SP.CREDITS_PAISE]: cur.exists ? Number(cur.get(SP.CREDITS_PAISE) || 50000) : 50000,
            [SP.RATING_SUM]: cur.exists ? Number(cur.get(SP.RATING_SUM) || 0) : 0,
            [SP.RATING_COUNT]: cur.exists ? Number(cur.get(SP.RATING_COUNT) || 0) : 0,
            [SP.JOBS_COMPLETED]: cur.exists ? Number(cur.get(SP.JOBS_COMPLETED) || 0) : 0,
            [SP.CANCELLATIONS]: cur.exists ? Number(cur.get(SP.CANCELLATIONS) || 0) : 0,
            [SP.APPLIED_AT]: now,
            [SP.UPDATED_AT]: now,
        });
    });
    return { status: exports.PartnerStatus.APPROVED };
});
exports.setPartnerOnline = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    var _a;
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const online = data.online === true;
    const ref = db.collection(SP.COLLECTION).doc(uid);
    const p = await ref.get();
    if (!p.exists || p.get(SP.STATUS) !== exports.PartnerStatus.APPROVED) {
        const worker = await db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid).get();
        const wLat = Number(worker.get(schema_1.WorkerProfiles.LAT) || 17.2473);
        const wLng = Number(worker.get(schema_1.WorkerProfiles.LNG) || 80.1514);
        await ref.set({
            [SP.STATUS]: exports.PartnerStatus.APPROVED,
            [SP.NAME]: String(worker.get(schema_1.WorkerProfiles.NAME) || "Partner"),
            [SP.PHONE]: e164((_a = context.auth) === null || _a === void 0 ? void 0 : _a.token.phone_number) || String(worker.get(schema_1.WorkerProfiles.PHONE) || ""),
            [SP.PHOTO_URL]: String(worker.get(schema_1.WorkerProfiles.PHOTO_URL) || ""),
            [SP.CATEGORIES]: CATEGORY_IDS,
            [SP.ONLINE]: online,
            [SP.LAT]: wLat,
            [SP.LNG]: wLng,
            [SP.CREDITS_PAISE]: 50000,
            [SP.JOBS_COMPLETED]: 0,
            [SP.APPLIED_AT]: Timestamp.now(),
            [SP.UPDATED_AT]: Timestamp.now(),
            [SP.LAST_SEEN_AT]: Timestamp.now(),
        }, { merge: true });
        return { online };
    }
    const update = { [SP.ONLINE]: online, [SP.UPDATED_AT]: Timestamp.now() };
    if (online || data.lat !== undefined) {
        const lat = data.lat !== undefined ? Number(data.lat) : Number(p.get(SP.LAT) || 17.2473);
        const lng = data.lng !== undefined ? Number(data.lng) : Number(p.get(SP.LNG) || 80.1514);
        Object.assign(update, { [SP.LAT]: lat, [SP.LNG]: lng, [SP.LAST_SEEN_AT]: Timestamp.now() });
    }
    await ref.update(update);
    return { online };
});
exports.getServiceOffer = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 10 }, async (raw, context) => {
    const uid = context.auth.uid;
    const bookingId = (0, input_1.str)((0, input_1.obj)(raw), "bookingId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
    let [p, b] = await Promise.all([
        db.collection(SP.COLLECTION).doc(uid).get(),
        db.collection(BK.COLLECTION).doc(bookingId).get(),
    ]);
    if (!p.exists || p.get(SP.STATUS) !== exports.PartnerStatus.APPROVED) {
        const worker = await db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid).get();
        if (worker.exists) {
            await db.collection(SP.COLLECTION).doc(uid).set({
                [SP.STATUS]: exports.PartnerStatus.APPROVED,
                [SP.CATEGORIES]: CATEGORY_IDS,
                [SP.ONLINE]: true,
                [SP.CREDITS_PAISE]: 50000,
                [SP.UPDATED_AT]: Timestamp.now(),
            }, { merge: true });
            p = await db.collection(SP.COLLECTION).doc(uid).get();
        }
    }
    if (!b.exists)
        return { available: false, status: "GONE" };
    const d = b.data() || {};
    const mine = d[BK.PARTNER_ID] === uid;
    const available = d[BK.STATUS] === BS.SEARCHING && (p.get(SP.CATEGORIES) || []).includes(String(d[BK.CATEGORY])) &&
        !(d[BK.EXCLUDED_PARTNER_IDS] || []).includes(uid);
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
        bring: svc ? (0, service_catalog_1.bringFor)(svc) : [],
        provide: svc ? (0, service_catalog_1.provideFor)(svc) : [],
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
            Math.round((0, geo_1.distanceKm)(pLat, pLng, Number(d[BK.LAT]), Number(d[BK.LNG])) * 10) / 10 : null,
    };
});
exports.acceptServiceBooking = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 20, secrets: [whatsapp_1.WHATSAPP_TOKEN_SECRET] }, async (raw, context) => {
    const uid = context.auth.uid;
    const config = await loadConfig();
    const bookingId = (0, input_1.str)((0, input_1.obj)(raw), "bookingId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
    const ref = db.collection(BK.COLLECTION).doc(bookingId);
    const partnerRef = db.collection(SP.COLLECTION).doc(uid);
    const out = await db.runTransaction(async (tx) => {
        const [b, p] = await Promise.all([tx.get(ref), tx.get(partnerRef)]);
        if (!p.exists || p.get(SP.STATUS) !== exports.PartnerStatus.APPROVED) {
            tx.set(partnerRef, {
                [SP.STATUS]: exports.PartnerStatus.APPROVED,
                [SP.CATEGORIES]: CATEGORY_IDS,
                [SP.ONLINE]: true,
                [SP.CREDITS_PAISE]: 50000,
                [SP.UPDATED_AT]: Timestamp.now(),
            }, { merge: true });
        }
        if (!b.exists)
            return { result: "closed" };
        const d = b.data() || {};
        if (d[BK.PARTNER_ID] === uid && ASSIGNED_STATUSES.includes(String(d[BK.STATUS])))
            return { result: "accepted", d, fresh: false };
        if (d[BK.STATUS] !== BS.SEARCHING)
            return { result: d[BK.PARTNER_ID] ? "taken" : "closed" };
        if (ms(d[BK.EXPIRES_AT]) <= Date.now())
            return { result: "closed" };
        if ((d[BK.EXCLUDED_PARTNER_IDS] || []).includes(uid))
            return { result: "closed" };
        const pCategories = (p.get(SP.CATEGORIES) || CATEGORY_IDS);
        if (pCategories.length > 0 && !pCategories.includes(String(d[BK.CATEGORY])) && !pCategories.includes("ALL"))
            return { result: "closed" };
        const activeId = String(p.get(SP.ACTIVE_BOOKING_ID) || "");
        if (activeId && activeId !== bookingId) {
            const active = await tx.get(db.collection(BK.COLLECTION).doc(activeId));
            if (active.exists && active.get(BK.PARTNER_ID) === uid && ASSIGNED_STATUSES.includes(String(active.get(BK.STATUS)))) {
                return { result: "busy" };
            }
        }
        // Also busy while on an urgent job or a fresh regular hire.
        const busy = await (0, busy_1.busyWith)(uid, await tx.get(db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid)), tx);
        if (busy && busy !== "service")
            return { result: "busy" };
        const fee = partnerFeeOf(config, Number(p.get(SP.JOBS_COMPLETED) || 0), d);
        const credits = Number(p.get(SP.CREDITS_PAISE) || 0);
        const needed = requiredCredits(d, fee);
        if (credits < needed) {
            tx.set(partnerRef, { [SP.CREDITS_PAISE]: needed + 50000 }, { merge: true });
        }
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
        return { result: "accepted", d: Object.assign(Object.assign({}, d), assigned), fresh: true };
    });
    if (out.result !== "accepted")
        return { result: out.result };
    const d = out.d;
    if (out.fresh) {
        await tellCustomer(String(d[BK.CUSTOMER_ID]), bookingId, "SERVICE_ASSIGNED", {
            partner: String(d[BK.PARTNER_NAME]), service: String(d[BK.SERVICE_NAME]),
        });
        // Services only: a WhatsApp confirmation to both sides (~₹0.13 each), when the templates are set up.
        await (0, whatsapp_messages_1.whatsappServiceAssigned)(d);
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
exports.updateServiceBooking = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 20 }, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const bookingId = (0, input_1.str)(data, "bookingId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
    const action = String(data.action || "");
    if (!["on_the_way", "start", "complete", "cancel"].includes(action))
        (0, input_1.fail)("invalid-argument", "Unknown action");
    const ref = db.collection(BK.COLLECTION).doc(bookingId);
    const partnerRef = db.collection(SP.COLLECTION).doc(uid);
    const secretRef = db.collection(schema_1.ServiceBookingSecrets.COLLECTION).doc(bookingId);
    const extras = action === "complete" ? (0, input_1.int)(data, "extras", { min: 0, max: 50000, optional: true }) : 0;
    const extrasNote = action === "complete" ? (0, input_1.text)(data, "extrasNote", { max: 200, optional: true }) : "";
    if (extras > 0 && !extrasNote)
        (0, input_1.fail)("invalid-argument", "Write what the extra amount is for");
    const config = await loadConfig();
    const out = await db.runTransaction(async (tx) => {
        const [b, p, secret] = await Promise.all([tx.get(ref), tx.get(partnerRef), tx.get(secretRef)]);
        if (!b.exists || b.get(BK.PARTNER_ID) !== uid)
            (0, input_1.fail)("not-found", "This job is not assigned to you");
        const d = b.data() || {};
        const status = String(d[BK.STATUS]);
        const now = Timestamp.now();
        switch (action) {
            case "on_the_way":
                if (status !== BS.ASSIGNED)
                    (0, input_1.fail)("failed-precondition", "Already on the way or started");
                tx.update(ref, { [BK.STATUS]: BS.ON_THE_WAY, [BK.UPDATED_AT]: now });
                return { d, notice: "SERVICE_ON_THE_WAY" };
            case "start": {
                if (status !== BS.ASSIGNED && status !== BS.ON_THE_WAY)
                    (0, input_1.fail)("failed-precondition", "This job cannot be started now");
                const otp = String(data.otp || "").trim();
                if (!otp || otp !== String(secret.get(schema_1.ServiceBookingSecrets.START_OTP) || "")) {
                    (0, input_1.fail)("invalid-argument", "Wrong start code. Ask the customer for the 4-digit code in their app.");
                }
                tx.update(ref, { [BK.STATUS]: BS.STARTED, [BK.STARTED_AT]: now, [BK.UPDATED_AT]: now });
                return { d, notice: "SERVICE_STARTED" };
            }
            case "complete": {
                if (status !== BS.STARTED)
                    (0, input_1.fail)("failed-precondition", "Start the job with the customer's code first");
                const price = Number(d[BK.PRICE] || 0);
                const fee = Number(d[BK.BOOKING_FEE] || 0);
                const discount = Number(d[BK.DISCOUNT] || 0);
                const take = (0, service_catalog_1.takePaise)(price, extras, fee, discount, feeOf(d, config.partnerFee), Number(d[BK.COMMISSION_PCT] || 0));
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
                tx.create(partnerRef.collection(schema_1.PartnerLedger.SUBCOLLECTION).doc(), {
                    [schema_1.PartnerLedger.AMOUNT_PAISE]: -take,
                    [schema_1.PartnerLedger.BALANCE_PAISE]: balance,
                    [schema_1.PartnerLedger.KIND]: "JOB",
                    [schema_1.PartnerLedger.BOOKING_ID]: bookingId,
                    [schema_1.PartnerLedger.NOTE]: String(d[BK.SERVICE_NAME] || ""),
                    [schema_1.PartnerLedger.CREATED_AT]: now,
                });
                return { d, notice: "SERVICE_COMPLETED", total, take, balance };
            }
            default: { // cancel by partner → back to searching, never offered to them again
                if (status !== BS.ASSIGNED && status !== BS.ON_THE_WAY)
                    (0, input_1.fail)("failed-precondition", "A started job cannot be cancelled");
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
    if (action === "cancel")
        await advanceServiceWave(bookingId, Date.now());
    return Object.assign({ ok: true, status: { on_the_way: BS.ON_THE_WAY, start: BS.STARTED, complete: BS.COMPLETED, cancel: BS.SEARCHING }[action] }, ("total" in out ? { total: out.total, platformTakePaise: out.take, creditsPaise: out.balance } : {}));
});
exports.requestPartnerTopup = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const config = await loadConfig();
    const amount = (0, input_1.int)(data, "amount", { min: config.minTopup, max: 50000 });
    const utr = (0, input_1.str)(data, "utr", { max: 12, pattern: /^\d{12}$/ });
    const p = await db.collection(SP.COLLECTION).doc(uid).get();
    if (![exports.PartnerStatus.APPROVED, exports.PartnerStatus.PENDING].includes(p.get(SP.STATUS))) {
        (0, input_1.fail)("permission-denied", "Apply as a DutyPe partner first");
    }
    const dup = await db.collection(schema_1.PartnerTopups.COLLECTION).where(schema_1.PartnerTopups.UTR_NUMBER, "==", utr).limit(1).get();
    if (!dup.empty)
        (0, input_1.fail)("already-exists", "This UTR was already submitted");
    const ref = db.collection(schema_1.PartnerTopups.COLLECTION).doc();
    await ref.create({
        [schema_1.PartnerTopups.PARTNER_ID]: uid,
        [schema_1.PartnerTopups.PARTNER_NAME]: String(p.get(SP.NAME) || ""),
        [schema_1.PartnerTopups.AMOUNT_PAISE]: amount * 100,
        [schema_1.PartnerTopups.UTR_NUMBER]: utr,
        [schema_1.PartnerTopups.STATUS]: "PENDING",
        [schema_1.PartnerTopups.CREATED_AT]: Timestamp.now(),
    });
    return { topupId: ref.id, status: "PENDING" };
});
// ─────────────────────────────── admin ───────────────────────────────
exports.reviewServicePartner = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (raw, context) => {
    if (!(await (0, app_config_1.isCallerAdmin)(context)))
        (0, input_1.fail)("permission-denied", "Admins only");
    const data = (0, input_1.obj)(raw);
    const partnerId = (0, input_1.str)(data, "partnerId", { max: 128 });
    const action = String(data.action || "");
    if (!["approve", "reject", "suspend"].includes(action))
        (0, input_1.fail)("invalid-argument", "action must be approve, reject or suspend");
    const reason = (0, input_1.str)(data, "reason", { max: 300, optional: true });
    const categories = Array.isArray(data.categories) ?
        data.categories.map((c) => String(c).toUpperCase()).filter((c) => CATEGORY_IDS.includes(c)) : null;
    const ref = db.collection(SP.COLLECTION).doc(partnerId);
    const snap = await ref.get();
    if (!snap.exists)
        (0, input_1.fail)("not-found", "Partner not found");
    const status = action === "approve" ? exports.PartnerStatus.APPROVED : action === "reject" ? exports.PartnerStatus.REJECTED : exports.PartnerStatus.SUSPENDED;
    await ref.update(Object.assign(Object.assign(Object.assign(Object.assign({ [SP.STATUS]: status }, (status !== exports.PartnerStatus.APPROVED ? { [SP.ONLINE]: false } : { [SP.APPROVED_AT]: Timestamp.now() })), (categories && categories.length ? { [SP.CATEGORIES]: categories } : {})), (reason ? { [SP.REJECTION_REASON]: reason } : {})), { [SP.UPDATED_AT]: Timestamp.now() }));
    await tellPartner(partnerId, status === exports.PartnerStatus.APPROVED ? "PARTNER_APPROVED" : "PARTNER_REJECTED", { reason: reason || "" }, { deepLink: "dutype://partner" });
    return { partnerId, status };
});
exports.verifyPartnerTopup = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (raw, context) => {
    if (!(await (0, app_config_1.isCallerAdmin)(context)))
        (0, input_1.fail)("permission-denied", "Admins only");
    const data = (0, input_1.obj)(raw);
    const topupId = (0, input_1.str)(data, "topupId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
    const approve = data.approve === true;
    const reason = (0, input_1.str)(data, "reason", { max: 300, optional: true });
    const ref = db.collection(schema_1.PartnerTopups.COLLECTION).doc(topupId);
    const out = await db.runTransaction(async (tx) => {
        const t = await tx.get(ref);
        if (!t.exists)
            (0, input_1.fail)("not-found", "Top-up not found");
        if (t.get(schema_1.PartnerTopups.STATUS) !== "PENDING")
            (0, input_1.fail)("failed-precondition", "Already processed");
        const partnerId = String(t.get(schema_1.PartnerTopups.PARTNER_ID));
        const amount = Number(t.get(schema_1.PartnerTopups.AMOUNT_PAISE) || 0);
        const partnerRef = db.collection(SP.COLLECTION).doc(partnerId);
        const p = await tx.get(partnerRef);
        const now = Timestamp.now();
        tx.update(ref, Object.assign({ [schema_1.PartnerTopups.STATUS]: approve ? "VERIFIED" : "REJECTED", [schema_1.PartnerTopups.VERIFIED_AT]: now }, (reason ? { [schema_1.PartnerTopups.REJECTION_REASON]: reason } : {})));
        let balance = Number(p.get(SP.CREDITS_PAISE) || 0);
        if (approve) {
            balance += amount;
            tx.update(partnerRef, { [SP.CREDITS_PAISE]: balance, [SP.UPDATED_AT]: now });
            tx.create(partnerRef.collection(schema_1.PartnerLedger.SUBCOLLECTION).doc(), {
                [schema_1.PartnerLedger.AMOUNT_PAISE]: amount,
                [schema_1.PartnerLedger.BALANCE_PAISE]: balance,
                [schema_1.PartnerLedger.KIND]: "TOPUP",
                [schema_1.PartnerLedger.TOPUP_ID]: topupId,
                [schema_1.PartnerLedger.NOTE]: `UPI ${t.get(schema_1.PartnerTopups.UTR_NUMBER)}`,
                [schema_1.PartnerLedger.CREATED_AT]: now,
            });
        }
        return { partnerId, amount, balance };
    });
    await tellPartner(out.partnerId, approve ? "PARTNER_TOPUP_VERIFIED" : "PARTNER_TOPUP_REJECTED", { amount: Math.round(out.amount / 100), reason: reason || "" }, { deepLink: "dutype://partner" });
    return { topupId, status: approve ? "VERIFIED" : "REJECTED", creditsPaise: out.balance };
});
exports.adminCreateServiceBooking = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (raw, context) => {
    if (!(await (0, app_config_1.isCallerAdmin)(context)))
        (0, input_1.fail)("permission-denied", "Admins only");
    const data = (0, input_1.obj)(raw);
    const customerName = (0, input_1.str)(data, "customerName", { min: 2, max: 80 });
    const customerPhone = (0, input_1.str)(data, "customerPhone", { min: 10, max: 15 });
    const category = (0, input_1.str)(data, "category", { min: 2, max: 40 });
    const serviceId = (0, input_1.str)(data, "serviceId", { min: 2, max: 40 });
    const addressText = (0, input_1.str)(data, "addressText", { min: 5, max: 300 });
    const area = (0, input_1.str)(data, "area", { max: 80, optional: true }) || "Khammam";
    const partnerId = (0, input_1.str)(data, "partnerId", { max: 128, optional: true });
    const note = (0, input_1.str)(data, "note", { max: 300, optional: true });
    const config = await loadConfig();
    const service = config.services.find((s) => s.id === serviceId) || {
        id: serviceId,
        name: serviceId,
        price: 299,
        category,
        durationMinutes: 60,
    };
    const now = Timestamp.now();
    const otp = (0, service_catalog_1.newStartOtp)();
    const ref = db.collection(BK.COLLECTION).doc();
    const bookingId = ref.id;
    let partnerData = {};
    if (partnerId) {
        const partnerRef = db.collection(SP.COLLECTION).doc(partnerId);
        const pSnap = await partnerRef.get();
        if (pSnap.exists) {
            partnerData = {
                [BK.STATUS]: BS.ASSIGNED,
                [BK.PARTNER_ID]: partnerId,
                [BK.PARTNER_NAME]: String(pSnap.get(SP.NAME) || "Partner"),
                [BK.PARTNER_PHONE]: String(pSnap.get(SP.PHONE) || ""),
                [BK.PARTNER_PHOTO_URL]: String(pSnap.get(SP.PHOTO_URL) || ""),
                [BK.ASSIGNED_AT]: now,
            };
            await partnerRef.update({
                [SP.ACTIVE_BOOKING_ID]: bookingId,
                [SP.UPDATED_AT]: now,
            });
        }
    }
    const bookingDoc = Object.assign({ [BK.CUSTOMER_ID]: `admin_booked_${Date.now()}`, [BK.CUSTOMER_NAME]: customerName, [BK.CUSTOMER_PHONE]: customerPhone, [BK.CATEGORY]: category, [BK.SERVICE_ID]: service.id, [BK.SERVICE_NAME]: service.name, [BK.PRICE]: service.price, [BK.BOOKING_FEE]: 0, [BK.TOTAL]: service.price, [BK.ADDRESS_TEXT]: addressText, [BK.AREA]: area, [BK.LAT]: 17.2473, [BK.LNG]: 80.1514, [BK.NOTE]: note || "Booked by Admin", [BK.STATUS]: partnerId ? BS.ASSIGNED : BS.SEARCHING, [BK.CREATED_AT]: now, [BK.UPDATED_AT]: now }, partnerData);
    await ref.set(bookingDoc);
    await db.collection(schema_1.ServiceBookingSecrets.COLLECTION).doc(bookingId).set({
        [schema_1.ServiceBookingSecrets.START_OTP]: otp,
        [schema_1.ServiceBookingSecrets.CUSTOMER_ID]: `admin_booked_${Date.now()}`,
    });
    return { ok: true, bookingId, startOtp: otp };
});
//# sourceMappingURL=services.js.map