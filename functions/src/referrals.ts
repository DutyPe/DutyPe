/**
 * Referrals and the wallet — money works like a ledger.
 *
 *  • referral_stats/{uid}      the wallet: balance / lifetime / withdrawn in whole paise (server-only)
 *  • wallet_ledger/{eventId}   append-only entries; the id IS the event (referral_{uid}_referrer,
 *                              withdrawal_{id}, ...) so the same event can never be credited twice.
 *                              Every balance change writes its ledger entry in the same transaction.
 *  • referrals/{refereeUid}    one referral per new user (the id enforces it)
 *  • referral_codes/{CODE}     code → owner
 *
 *   registerReferral            at signup, with the code the new user typed (called by profiles.ts)
 *   completeReferral            when the new user's profile becomes complete → rewards
 *   ensureWallet / ensureWalletCallable   wallet + own code for every user
 *   requestWithdrawal           the worker/employer cashes out the full balance to UPI
 *   settleWithdrawal            admin marks a withdrawal paid or failed (failed = refund entry)
 *   getReferralLeaderboard      top referrers (first name + initial only)
 *   expirePendingReferrals      daily: pending referrals past expiresAt → EXPIRED
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { createHash, randomInt } from "crypto";
import { onCallSecured } from "./secure-callable";
import { fail, obj, str } from "./lib/input";
import { notify } from "./lib/notify";
import { getReferralConfig, isCallerAdmin } from "./app-config";
import {
  EmployerProfiles, ReferralCodes, Referrals, Values, WalletLedger, Wallets, WithdrawalDaily, Withdrawals,
  WorkerProfiles,
} from "./schema";

const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
type Tx = admin.firestore.Transaction;

const DAY_MS = 24 * 60 * 60 * 1000;
const REFERRAL_EXPIRY_MS = 30 * DAY_MS;
const MAX_REFERRALS_PER_HOUR = 10;
const MAX_WITHDRAWALS_PER_DAY = 5;
const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
/** Job-post credits an employer earns at these successful-referral counts. */
const EMPLOYER_CREDIT_MILESTONES: Record<number, number> = { 5: 5, 10: 10, 25: 25 };

const walletRef = (uid: string) => db.collection(Wallets.COLLECTION).doc(uid);
const ledgerRef = (eventId: string) => db.collection(WalletLedger.COLLECTION).doc(eventId);

// ───────────────────────────── ledger core ─────────────────────────────

interface WalletState { ref: admin.firestore.DocumentReference; balancePaise: number; data: admin.firestore.DocumentData }

async function readWallet(tx: Tx, uid: string): Promise<WalletState> {
  const snap = await tx.get(walletRef(uid));
  const data = snap.data() || {};
  return { ref: snap.ref, balancePaise: Number(data[Wallets.BALANCE_PAISE] || 0), data };
}

/**
 * Appends one ledger entry and moves the wallet balance by the same amount. The caller must have
 * checked (inside the same transaction) that the ledger id does not exist yet.
 */
function postEntry(tx: Tx, uid: string, wallet: WalletState, eventId: string, type: string, amountPaise: number, refId: string) {
  const after = wallet.balancePaise + amountPaise;
  tx.create(ledgerRef(eventId), {
    [WalletLedger.UID]: uid,
    [WalletLedger.TYPE]: type,
    [WalletLedger.AMOUNT_PAISE]: amountPaise,
    [WalletLedger.REF_ID]: refId,
    [WalletLedger.BALANCE_AFTER_PAISE]: after,
    [WalletLedger.CREATED_AT]: Timestamp.now(),
  });
  const update: Record<string, unknown> = {
    [Wallets.BALANCE_PAISE]: after,
    [Wallets.UPDATED_AT]: Timestamp.now(),
  };
  if (amountPaise > 0 && type !== Values.LedgerType.REFUND) {
    update[Wallets.LIFETIME_EARNED_PAISE] = FieldValue.increment(amountPaise);
  }
  if (type === Values.LedgerType.WITHDRAWAL) update[Wallets.WITHDRAWN_PAISE] = FieldValue.increment(-amountPaise);
  if (type === Values.LedgerType.REFUND) update[Wallets.WITHDRAWN_PAISE] = FieldValue.increment(-amountPaise);
  tx.set(wallet.ref, update, { merge: true });
  wallet.balancePaise = after;
}

// ───────────────────────────── wallet + code ─────────────────────────────

function randomCode(): string {
  return Array.from({ length: 8 }, () => CODE_ALPHABET[randomInt(CODE_ALPHABET.length)]).join("");
}

/** Creates the wallet and the user's own referral code if missing. Returns the code. */
export async function ensureWallet(uid: string, role: string): Promise<string> {
  const existing = await walletRef(uid).get();
  const code = String(existing.get(Wallets.REFERRAL_CODE) || "");
  if (code) return code;
  for (let attempt = 0; attempt < 8; attempt++) {
    const candidate = randomCode();
    try {
      return await db.runTransaction(async (tx) => {
        const [wallet, codeDoc] = await Promise.all([
          tx.get(walletRef(uid)), tx.get(db.collection(ReferralCodes.COLLECTION).doc(candidate)),
        ]);
        const current = String(wallet.get(Wallets.REFERRAL_CODE) || "");
        if (current) return current;
        if (codeDoc.exists) throw new Error("CODE_TAKEN");
        tx.create(codeDoc.ref, {
          [ReferralCodes.UID]: uid,
          [ReferralCodes.ROLE]: role,
          [ReferralCodes.ACTIVE]: true,
        });
        tx.set(wallet.ref, {
          [Wallets.REFERRAL_CODE]: candidate,
          [Wallets.BALANCE_PAISE]: Number(wallet.get(Wallets.BALANCE_PAISE) || 0),
          [Wallets.LIFETIME_EARNED_PAISE]: Number(wallet.get(Wallets.LIFETIME_EARNED_PAISE) || 0),
          [Wallets.WITHDRAWN_PAISE]: Number(wallet.get(Wallets.WITHDRAWN_PAISE) || 0),
          [Wallets.SUCCESSFUL_REFERRALS]: Number(wallet.get(Wallets.SUCCESSFUL_REFERRALS) || 0),
          [Wallets.AWARDED_MILESTONES]: wallet.get(Wallets.AWARDED_MILESTONES) || [],
          [Wallets.BLOCKED]: wallet.get(Wallets.BLOCKED) === true,
          [Wallets.UPDATED_AT]: Timestamp.now(),
        }, { merge: true });
        return candidate;
      });
    } catch (error) {
      if ((error as Error).message !== "CODE_TAKEN") throw error;
    }
  }
  throw new functions.https.HttpsError("internal", "Could not create a referral code");
}

/** The app asks for its own code (older accounts get one on first call). */
export const ensureWalletCallable = onCallSecured({}, async (_raw: unknown, context) => {
  const uid = context.auth!.uid;
  const role = String(context.auth!.token.role || "");
  const code = await ensureWallet(uid, role === Values.Role.EMPLOYER ? role : Values.Role.WORKER);
  return { referralCode: code };
});

// ───────────────────────────── referrals ─────────────────────────────

function normalizeCode(raw: unknown): string {
  return String(raw ?? "").toUpperCase().replace(/[^A-Z0-9]/g, "");
}

/**
 * Records that [uid] joined with [rawCode]. Never throws: a bad code must not fail registration.
 * The referral stays PENDING until the new user's profile is complete.
 */
const LATE_REFERRAL_WINDOW_MS = 7 * 24 * 60 * 60 * 1000;

export async function registerReferral(uid: string, rawCode: string): Promise<string | null> {
  const code = normalizeCode(rawCode);
  if (!/^[A-Z0-9]{6,10}$/.test(code)) return "Invalid referral code";
  try {
    const codeDoc = await db.collection(ReferralCodes.COLLECTION).doc(code).get();
    const referrerUid = String(codeDoc.get(ReferralCodes.UID) || "");
    if (!codeDoc.exists || codeDoc.get(ReferralCodes.ACTIVE) !== true || !referrerUid) return "Referral code not found";
    if (referrerUid === uid) return "You cannot use your own code";

    const hourAgo = Timestamp.fromMillis(Date.now() - 60 * 60 * 1000);
    const recent = await db.collection(Referrals.COLLECTION)
      .where(Referrals.REFERRER_UID, "==", referrerUid)
      .where(Referrals.CREATED_AT, ">=", hourAgo)
      .count().get();
    const fraudScore = recent.data().count >= MAX_REFERRALS_PER_HOUR ? 60 : 0;

    return await db.runTransaction(async (tx) => {
      const ref = db.collection(Referrals.COLLECTION).doc(uid);
      const [existing, referrerWallet] = await Promise.all([tx.get(ref), tx.get(walletRef(referrerUid))]);
      if (existing.exists) return "A referral code was already used";
      if (referrerWallet.get(Wallets.BLOCKED) === true) return "This referral code is not active";
      const now = Date.now();
      tx.create(ref, {
        [Referrals.REFERRER_UID]: referrerUid,
        [Referrals.CODE]: code,
        [Referrals.STATUS]: fraudScore > 0 ? Values.ReferralStatus.REJECTED : Values.ReferralStatus.PENDING,
        [Referrals.FRAUD_SCORE]: fraudScore,
        [Referrals.CREATED_AT]: Timestamp.fromMillis(now),
        [Referrals.EXPIRES_AT]: Timestamp.fromMillis(now + REFERRAL_EXPIRY_MS),
      });
      return null;
    });
  } catch (error) {
    functions.logger.warn(`registerReferral(${uid}) failed`, error);
    return "Referral could not be applied";
  }
}

/** A code entered after sign-up (profile setup). Allowed for 7 days after the account was created. */
export const applyReferralCode = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const role = String(context.auth!.token.role || "");
  if (role !== Values.Role.WORKER && role !== Values.Role.EMPLOYER) fail("failed-precondition", "Finish registration first");
  const code = str(obj(raw), "code", { min: 6, max: 20 });
  const created = Date.parse((await admin.auth().getUser(uid)).metadata.creationTime);
  if (Date.now() - created > LATE_REFERRAL_WINDOW_MS) fail("failed-precondition", "Referral codes can only be used in your first 7 days");
  const error = await registerReferral(uid, code);
  if (error) fail("invalid-argument", error);
  return { ok: true };
});

/**
 * The referred user did the real thing (a worker's first application, an employer's first job
 * post; called from applyToJob / postJob, one read when nothing is pending): pay the referrer (plus any milestone bonus), give the
 * new worker their signup bonus, and give employer referrers job-post credits at milestones.
 * Idempotent: the referral flips PENDING → COMPLETED inside the transaction and every credit's
 * ledger id is the event.
 */
export async function completeReferral(refereeUid: string, refereeRole: string): Promise<void> {
  const refDoc = db.collection(Referrals.COLLECTION).doc(refereeUid);
  const peek = await refDoc.get();
  if (!peek.exists || peek.get(Referrals.STATUS) !== Values.ReferralStatus.PENDING) return;
  const config = await getReferralConfig();

  const paid = await db.runTransaction(async (tx) => {
    const referral = await tx.get(refDoc);
    if (referral.get(Referrals.STATUS) !== Values.ReferralStatus.PENDING) return null;
    const referrerUid = String(referral.get(Referrals.REFERRER_UID));
    const expiresAt = (referral.get(Referrals.EXPIRES_AT) as admin.firestore.Timestamp | undefined)?.toMillis() ?? 0;
    const referrerCodeSnap = await tx.get(db.collection(ReferralCodes.COLLECTION).doc(String(referral.get(Referrals.CODE))));
    const referrerRole = String(referrerCodeSnap.get(ReferralCodes.ROLE) || Values.Role.WORKER);
    const rewardId = `referral_${refereeUid}_referrer`;
    const bonusId = `referral_${refereeUid}_referee`;
    const [referrer, referee, rewardEntry, bonusEntry] = await Promise.all([
      readWallet(tx, referrerUid), readWallet(tx, refereeUid), tx.get(ledgerRef(rewardId)), tx.get(ledgerRef(bonusId)),
    ]);
    const employerRef = db.collection(EmployerProfiles.COLLECTION).doc(referrerUid);

    if (expiresAt > 0 && Date.now() > expiresAt) {
      tx.update(refDoc, { [Referrals.STATUS]: Values.ReferralStatus.EXPIRED });
      return null;
    }
    if (referrer.data[Wallets.BLOCKED] === true) {
      tx.update(refDoc, { [Referrals.STATUS]: Values.ReferralStatus.REJECTED });
      return null;
    }

    const successful = Number(referrer.data[Wallets.SUCCESSFUL_REFERRALS] || 0) + 1;
    const awarded: number[] = Array.isArray(referrer.data[Wallets.AWARDED_MILESTONES]) ?
      referrer.data[Wallets.AWARDED_MILESTONES].map(Number) : [];
    tx.update(refDoc, { [Referrals.STATUS]: Values.ReferralStatus.COMPLETED, [Referrals.COMPLETED_AT]: Timestamp.now() });
    tx.set(referrer.ref, { [Wallets.SUCCESSFUL_REFERRALS]: successful }, { merge: true });

    let referrerPaise = 0;
    if (!rewardEntry.exists) {
      referrerPaise = Math.round(config.rewardPerReferral * 100);
      postEntry(tx, referrerUid, referrer, rewardId, Values.LedgerType.REFERRAL_REWARD, referrerPaise, refereeUid);
    }
    const milestoneRupees = Number(config.milestones?.[String(successful)] || 0);
    if (milestoneRupees > 0 && !awarded.includes(successful)) {
      const milestonePaise = Math.round(milestoneRupees * 100);
      postEntry(tx, referrerUid, referrer, `milestone_${referrerUid}_${successful}`, Values.LedgerType.MILESTONE, milestonePaise, String(successful));
      tx.set(referrer.ref, { [Wallets.AWARDED_MILESTONES]: FieldValue.arrayUnion(successful) }, { merge: true });
      referrerPaise += milestonePaise;
    }
    if (referrerRole === Values.Role.EMPLOYER && EMPLOYER_CREDIT_MILESTONES[successful]) {
      const S = EmployerProfiles.Subscription;
      tx.set(employerRef, {
        [EmployerProfiles.SUBSCRIPTION]: {
          [S.CREDITS]: { [S.CREDITS_NORMAL]: FieldValue.increment(EMPLOYER_CREDIT_MILESTONES[successful]) },
        },
      }, { merge: true });
    }

    let refereePaise = 0;
    if (refereeRole === Values.Role.WORKER && !bonusEntry.exists && config.signupBonus > 0) {
      refereePaise = Math.round(config.signupBonus * 100);
      postEntry(tx, refereeUid, referee, bonusId, Values.LedgerType.SIGNUP_BONUS, refereePaise, referrerUid);
    }
    return { referrerUid, referrerRole, referrerPaise, refereePaise };
  });

  if (!paid) return;
  if (paid.referrerPaise > 0) {
    await notify(paid.referrerUid, {
      type: "PAYMENT", templateId: "REFERRAL_REWARD_BASIC", role: paid.referrerRole,
      params: { amount: paid.referrerPaise / 100 }, data: { refId: refereeUid },
    });
  }
  if (paid.refereePaise > 0) {
    await notify(refereeUid, {
      type: "PAYMENT", templateId: "SIGNUP_BONUS", role: Values.Role.WORKER,
      params: { amount: paid.refereePaise / 100 }, data: {},
    });
  }
}

// ───────────────────────────── withdrawals ─────────────────────────────

const UPI_PATTERN = /^[a-zA-Z0-9._-]{2,256}@[a-zA-Z][a-zA-Z0-9.-]{1,64}$/;

/** Cash out the wallet to UPI (whole balance, capped per day). requestId makes a retry harmless. */
export const requestWithdrawal = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const upiId = str(data, "upiId", { min: 5, max: 100, pattern: UPI_PATTERN });
  const reqId = str(data, "requestId", { min: 8, max: 80, pattern: /^[A-Za-z0-9_-]+$/ });
  const config = await getReferralConfig();
  const minPaise = Math.round(config.minWithdrawal * 100);
  const dailyCapPaise = Math.round(config.maxWithdrawalPerDay * 100);
  const withdrawalId = createHash("sha256").update(`${uid}:${reqId}`).digest("hex").slice(0, 32);
  const day = new Date(Date.now() + 330 * 60 * 1000).toISOString().slice(0, 10);
  const withdrawalRef = db.collection(Withdrawals.COLLECTION).doc(withdrawalId);
  const dailyRef = db.collection(WithdrawalDaily.COLLECTION).doc(`${uid}_${day}`);

  const result = await db.runTransaction(async (tx) => {
    const [existing, wallet, daily] = await Promise.all([tx.get(withdrawalRef), readWallet(tx, uid), tx.get(dailyRef)]);
    if (existing.exists) return { withdrawalId, amountPaise: Number(existing.get(Withdrawals.AMOUNT_PAISE)), replay: true };
    if (wallet.data[Wallets.BLOCKED] === true) fail("permission-denied", "Your wallet is blocked. Contact support.");
    const dailyPaise = Number(daily.get(WithdrawalDaily.AMOUNT_PAISE) || 0);
    const dailyCount = Number(daily.get(WithdrawalDaily.REQUEST_COUNT) || 0);
    if (dailyCount >= MAX_WITHDRAWALS_PER_DAY) fail("resource-exhausted", "Daily withdrawal limit reached");
    // The whole balance, up to what is left of today's cap; the rest can be withdrawn tomorrow.
    const amountPaise = Math.min(wallet.balancePaise, dailyCapPaise - dailyPaise);
    if (wallet.balancePaise < minPaise) fail("failed-precondition", `You need at least ₹${config.minWithdrawal} to withdraw`);
    if (amountPaise < minPaise) fail("resource-exhausted", `You can withdraw up to ₹${config.maxWithdrawalPerDay} per day`);

    const now = Timestamp.now();
    tx.create(withdrawalRef, {
      [Withdrawals.UID]: uid,
      [Withdrawals.AMOUNT_PAISE]: amountPaise,
      [Withdrawals.UPI_ID]: upiId,
      [Withdrawals.STATUS]: Values.WithdrawalStatus.PENDING,
      [Withdrawals.CREATED_AT]: now,
    });
    postEntry(tx, uid, wallet, `withdrawal_${withdrawalId}`, Values.LedgerType.WITHDRAWAL, -amountPaise, withdrawalId);
    tx.set(dailyRef, {
      [WithdrawalDaily.AMOUNT_PAISE]: dailyPaise + amountPaise,
      [WithdrawalDaily.REQUEST_COUNT]: dailyCount + 1,
      [WithdrawalDaily.EXPIRE_AT]: Timestamp.fromMillis(Date.now() + 2 * DAY_MS),
    });
    return { withdrawalId, amountPaise, replay: false };
  });
  return result;
});

/**
 * Admin settles a withdrawal: COMPLETED (with the payout reference) or FAILED (money goes back to
 * the wallet as a REFUND entry). A settled withdrawal can never change again.
 */
export const settleWithdrawal = onCallSecured({ enforceAppCheck: false }, async (raw: unknown, context) => {
  if (!(await isCallerAdmin(context))) fail("permission-denied", "Admins only");
  const data = obj(raw);
  const withdrawalId = str(data, "withdrawalId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
  const status = String(data.status || "").toUpperCase();
  if (status !== Values.WithdrawalStatus.COMPLETED && status !== Values.WithdrawalStatus.FAILED &&
    status !== Values.WithdrawalStatus.PROCESSING) fail("invalid-argument", "status is invalid");
  const txnRef = str(data, "txnRef", { max: 100, optional: true });
  const failureReason = str(data, "failureReason", { max: 300, optional: true });
  if (status === Values.WithdrawalStatus.COMPLETED && !txnRef) fail("invalid-argument", "txnRef is required");

  const ref = db.collection(Withdrawals.COLLECTION).doc(withdrawalId);
  const outcome = await db.runTransaction(async (tx) => {
    const w = await tx.get(ref);
    if (!w.exists) fail("not-found", "Withdrawal not found");
    const current = String(w.get(Withdrawals.STATUS));
    if (current === Values.WithdrawalStatus.COMPLETED || current === Values.WithdrawalStatus.FAILED) {
      fail("failed-precondition", `Withdrawal is already ${current}`);
    }
    const uid = String(w.get(Withdrawals.UID));
    const amountPaise = Number(w.get(Withdrawals.AMOUNT_PAISE));
    const refundId = `refund_${withdrawalId}`;
    const [wallet, refund] = await Promise.all([readWallet(tx, uid), tx.get(ledgerRef(refundId))]);
    tx.update(ref, {
      [Withdrawals.STATUS]: status,
      ...(txnRef ? { [Withdrawals.TXN_REF]: txnRef } : {}),
      ...(failureReason ? { [Withdrawals.FAILURE_REASON]: failureReason } : {}),
      ...(status !== Values.WithdrawalStatus.PROCESSING ? { [Withdrawals.PROCESSED_AT]: Timestamp.now() } : {}),
    });
    if (status === Values.WithdrawalStatus.FAILED && !refund.exists) {
      postEntry(tx, uid, wallet, refundId, Values.LedgerType.REFUND, amountPaise, withdrawalId);
    }
    return { uid, amountPaise };
  });
  if (status !== Values.WithdrawalStatus.PROCESSING) {
    await notify(outcome.uid, {
      type: "PAYMENT",
      templateId: status === Values.WithdrawalStatus.COMPLETED ? "WITHDRAWAL_COMPLETED" : "WITHDRAWAL_FAILED",
      params: { amount: outcome.amountPaise / 100 },
      data: { withdrawalId, status },
    });
  }
  return { withdrawalId, status };
});

// ───────────────────────────── leaderboard + expiry ─────────────────────────────

let leaderboardCache: { at: number; rows: unknown[] } | null = null;

export const getReferralLeaderboard = onCallSecured({}, async () => {
  if (leaderboardCache && Date.now() - leaderboardCache.at < 10 * 60 * 1000) return { leaders: leaderboardCache.rows };
  const top = await db.collection(Wallets.COLLECTION)
    .where(Wallets.BLOCKED, "==", false)
    .orderBy(Wallets.SUCCESSFUL_REFERRALS, "desc")
    .limit(20).get();
  const profiles = top.empty ? [] : await db.getAll(...top.docs.map((d) => db.collection(WorkerProfiles.COLLECTION).doc(d.id)));
  const rows = top.docs.map((d, i) => {
    const full = String(profiles[i]?.get(WorkerProfiles.NAME) || "DutyPe user").trim().split(/\s+/);
    return {
      rank: i + 1,
      name: full.length > 1 ? `${full[0]} ${full[full.length - 1][0]}.` : full[0],
      successfulReferrals: Number(d.get(Wallets.SUCCESSFUL_REFERRALS) || 0),
    };
  });
  leaderboardCache = { at: Date.now(), rows };
  return { leaders: rows };
});

export const expirePendingReferrals = functions
  .region("asia-south1")
  .pubsub.schedule("every 24 hours")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const snap = await db.collection(Referrals.COLLECTION)
      .where(Referrals.STATUS, "==", Values.ReferralStatus.PENDING)
      .where(Referrals.EXPIRES_AT, "<=", Timestamp.now())
      .limit(500).get();
    if (snap.empty) return null;
    const batch = db.batch();
    snap.docs.forEach((d) => batch.update(d.ref, { [Referrals.STATUS]: Values.ReferralStatus.EXPIRED }));
    await batch.commit();
    return null;
  });
