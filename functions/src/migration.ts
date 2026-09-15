import * as admin from "firebase-admin";
import { isDeepStrictEqual } from "util";
import { createHash } from "crypto";
import { publicProfile } from "./profiles";

export type MigrationPageOptions = {
  pageSize?: number;
  afterId?: string;
  apply?: boolean;
  collection?: "users" | "public_profiles";
  runId?: string;
};

export type ProfileMigrationItem = {
  userId: string;
  action: "create" | "replace" | "delete" | "unchanged";
  applied: boolean;
  inputDigest: string;
};

export type ReconciliationItem = {
  collection: "users" | "jobs";
  documentId: string;
  inputDigest: string;
  issues: string[];
  changes: Record<string, number | boolean | string>;
};

type Snapshot = admin.firestore.DocumentSnapshot;
const occupiedStates = new Set(["ACCEPTED", "IN_PROGRESS", "COMPLETED"]);
const applicationStates = new Set(["PENDING", "UNDER_REVIEW", "ACCEPTED", "IN_PROGRESS", "COMPLETED", "REJECTED", "WITHDRAWN"]);

function digest(sources: Snapshot[]): string {
  return createHash("sha256").update(JSON.stringify(sources.map(source => [
    source.ref.path, source.updateTime?.seconds ?? null, source.updateTime?.nanoseconds ?? null
  ]).sort((left, right) => String(left[0]).localeCompare(String(right[0]))))).digest("hex");
}

function moneyIsValid(value: unknown): value is number {
  return typeof value === "number" && value >= 0 && Number.isFinite(value) &&
    Number.isSafeInteger(Math.round(value * 100)) && Math.abs(value * 100 - Math.round(value * 100)) < 0.000001;
}

async function reconcileUser(transaction: admin.firestore.Transaction, database: admin.firestore.Firestore, userId: string): Promise<ReconciliationItem> {
  const [user, ledger] = await transaction.getAll(database.collection("users").doc(userId), database.collection("referral_stats").doc(userId));
  const sources: Snapshot[] = [user, ledger];
  const issues: string[] = [];
  const changes: ReconciliationItem["changes"] = {};
  const propose = (field: string, value: number | boolean | string) => {
    if (user.get(field) !== value) changes[field] = value;
  };
  if (!user.exists) issues.push("user_missing");
  if (!ledger.exists && user.get("referralStats") != null) issues.push("authoritative_referral_ledger_missing");
  if (ledger.exists) {
    if (!moneyIsValid(ledger.get("availableBalance"))) issues.push("invalid_authoritative_balance");
    for (const field of ["availableBalance", "totalEarnings", "pendingEarnings", "withdrawnAmount", "signupBonusAmount"]) {
      const value = ledger.get(field);
      if (value !== undefined) {
        if (!moneyIsValid(value)) issues.push(`invalid_ledger_${field}`);
        else propose(`referralStats.${field}`, value);
      }
    }
    for (const field of ["successfulReferrals", "totalReferrals", "pendingReferrals", "totalWithdrawals"]) {
      const value = ledger.get(field);
      if (value !== undefined) {
        if (!Number.isSafeInteger(value) || value < 0) issues.push(`invalid_ledger_${field}`);
        else propose(`referralStats.${field}`, value);
      }
    }
    const successfulReferrals = ledger.get("successfulReferrals");
    if (Number.isSafeInteger(successfulReferrals) && successfulReferrals >= 0) {
      propose("referralStats.canWithdraw", successfulReferrals >= 15 || [5, 10].includes(successfulReferrals));
    }
    if (user.get("referralStats.isBlocked") === true && ledger.get("isBlocked") !== true) issues.push("fraud_block_conflict_requires_review");
    if (ledger.get("isBlocked") === true) propose("referralStats.isBlocked", true);
    const available = ledger.get("availableBalance");
    const earned = ledger.get("totalEarnings");
    const withdrawn = ledger.get("withdrawnAmount") ?? 0;
    const pending = ledger.get("pendingEarnings") ?? 0;
    if ([available, earned, withdrawn, pending].every(moneyIsValid) &&
        Math.round((available + withdrawn + pending) * 100) !== Math.round(earned * 100)) {
      issues.push("authoritative_ledger_arithmetic_requires_review");
    }
  }
  const ratings = await transaction.get(database.collection("ratings").where("targetUserId", "==", userId).limit(201));
  sources.push(...ratings.docs);
  if (ratings.size > 200) issues.push("rating_set_exceeds_review_limit");
  const validReference = (value: unknown): value is string => typeof value === "string" && value.length > 0 && value.length <= 256 && !value.includes("/");
  const refs = new Map<string, admin.firestore.DocumentReference>();
  for (const rating of ratings.docs.slice(0, 200)) {
    for (const [field, collection] of [["applicationId", "job_applications"], ["raterId", "users"]]) {
      const id = rating.get(field);
      if (validReference(id)) refs.set(`${collection}/${id}`, database.collection(collection).doc(id));
    }
  }
  const related = refs.size ? await transaction.getAll(...refs.values()) : [];
  sources.push(...related);
  const byPath = new Map(related.map(document => [document.ref.path, document]));
  const seen = new Set<string>();
  const totals = { WORKER: { sum: 0, count: 0 }, EMPLOYER: { sum: 0, count: 0 } };
  for (const rating of ratings.docs.slice(0, 200)) {
    const application = byPath.get(`job_applications/${rating.get("applicationId")}`);
    const raterId = rating.get("raterId");
    const role = rating.get("targetRole");
    const stars = rating.get("rating");
    const unique = `${rating.get("applicationId")}:${raterId}`;
    const targetField = role === "WORKER" ? "workerId" : "employerId";
    const raterField = role === "WORKER" ? "employerId" : "workerId";
    const valid = (role === "WORKER" || role === "EMPLOYER") && application?.exists &&
      application.get("status") === "COMPLETED" && application.get(targetField) === userId &&
      application.get(raterField) === raterId && raterId !== userId && byPath.get(`users/${raterId}`)?.exists &&
      rating.get("jobId") === application.get("jobId") && Number.isInteger(stars) && stars >= 1 && stars <= 5;
    if (!valid) issues.push(`invalid_rating:${rating.id}`);
    if (seen.has(unique)) issues.push(`duplicate_rating:${rating.id}`);
    seen.add(unique);
    if (valid) {
      totals[role as "WORKER" | "EMPLOYER"].sum += stars;
      totals[role as "WORKER" | "EMPLOYER"].count += 1;
    }
  }
  for (const role of ["WORKER", "EMPLOYER"] as const) {
    const countField = role === "WORKER" ? "workerTotalRatings" : "totalRatings";
    const averageField = role === "WORKER" ? "workerAverageRating" : "averageRating";
    if (totals[role].count > 0 || user.get(countField) !== undefined || user.get(averageField) !== undefined) {
      propose(countField, totals[role].count);
      propose(averageField, totals[role].count ? totals[role].sum / totals[role].count : 0);
    }
  }
  return { collection: "users", documentId: userId, inputDigest: digest(sources), issues, changes };
}

async function reconcileJob(transaction: admin.firestore.Transaction, database: admin.firestore.Firestore, jobId: string): Promise<ReconciliationItem> {
  const job = await transaction.get(database.collection("jobs").doc(jobId));
  const applications = await transaction.get(database.collection("job_applications").where("jobId", "==", jobId).limit(501));
  const issues: string[] = [];
  const changes: ReconciliationItem["changes"] = {};
  if (!job.exists) issues.push("job_missing");
  if (applications.size > 500) issues.push("application_set_exceeds_review_limit");
  const workers = new Set<string>();
  let acceptedCount = 0;
  let applicationCount = 0;
  for (const application of applications.docs.slice(0, 500)) {
    const status = application.get("status");
    if (!applicationStates.has(status) || application.get("employerId") !== job.get("employerId") ||
        typeof application.get("workerId") !== "string" || !application.get("workerId")) issues.push(`invalid_application:${application.id}`);
    if (occupiedStates.has(status)) {
      acceptedCount += 1;
      if (workers.has(application.get("workerId"))) issues.push(`duplicate_hire:${application.id}`);
      workers.add(application.get("workerId"));
    }
    if (application.get("active") !== false && status !== "WITHDRAWN") applicationCount += 1;
  }
  const vacancies = job.get("vacancies");
  if (!Number.isSafeInteger(vacancies) || vacancies < 1) issues.push("invalid_vacancies");
  if (acceptedCount > vacancies) issues.push("existing_job_overbooked");
  const proposed: ReconciliationItem["changes"] = { acceptedCount, applicationCount, isFilled: acceptedCount >= vacancies };
  if ([undefined, "OPEN", "FILLED"].includes(job.get("vacancyStatus"))) proposed.vacancyStatus = acceptedCount >= vacancies ? "FILLED" : "OPEN";
  for (const [field, value] of Object.entries(proposed)) if (job.get(field) !== value) changes[field] = value;
  return { collection: "jobs", documentId: jobId, inputDigest: digest([job, ...applications.docs]), issues, changes };
}

export async function reconciliationPage(database: admin.firestore.Firestore, collection: "users" | "jobs", options: Omit<MigrationPageOptions, "collection" | "apply"> = {}) {
  if (collection !== "users" && collection !== "jobs") throw new Error("Unsupported reconciliation collection");
  const limit = pageSize(options.pageSize);
  let query = database.collection(collection).orderBy(admin.firestore.FieldPath.documentId()).limit(limit + 1);
  if (options.afterId) query = query.startAfter(options.afterId);
  const page = await query.get();
  const selected = page.docs.slice(0, limit);
  const items: ReconciliationItem[] = [];
  for (const document of selected) {
    items.push(await database.runTransaction(transaction => collection === "users"
      ? reconcileUser(transaction, database, document.id) : reconcileJob(transaction, database, document.id)));
  }
  return { collection, scanned: selected.length, items, nextCursor: page.size > limit ? selected[selected.length - 1].id : null };
}

export async function applyReconciliationItem(database: admin.firestore.Firestore, reviewed: ReconciliationItem, runId: string) {
  if (!/^[a-zA-Z0-9_-]{8,100}$/.test(runId)) throw new Error("A valid migration run ID is required");
  if (!reviewed.documentId || reviewed.documentId.includes("/") || !["users", "jobs"].includes(reviewed.collection)) throw new Error("Invalid reconciliation target");
  return database.runTransaction(async transaction => {
    const current = reviewed.collection === "users"
      ? await reconcileUser(transaction, database, reviewed.documentId)
      : await reconcileJob(transaction, database, reviewed.documentId);
    if (current.issues.length) return { status: "blocked", documentId: reviewed.documentId, issues: current.issues };
    if (current.inputDigest !== reviewed.inputDigest || !isDeepStrictEqual(current.changes, reviewed.changes)) throw new Error(`Stale reconciliation plan: ${reviewed.collection}/${reviewed.documentId}`);
    if (Object.keys(current.changes).length === 0) return { status: "unchanged", documentId: reviewed.documentId, issues: [] };
    const target = database.collection(reviewed.collection).doc(reviewed.documentId);
    const source = await transaction.get(target);
    const before: admin.firestore.DocumentData = {};
    const absent: string[] = [];
    for (const field of Object.keys(current.changes)) {
      const value = source.get(field);
      if (value === undefined) absent.push(field);
      else before[field] = value;
    }
    const receiptId = createHash("sha256").update(`${runId}:${target.path}:${current.inputDigest}`).digest("hex");
    transaction.create(database.collection("migration_audit").doc(receiptId), {
      runId, target: target.path, before, absent, after: current.changes,
      sourceDigest: current.inputDigest, createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    transaction.update(target, current.changes);
    return { status: "applied", documentId: reviewed.documentId, issues: [], receiptId };
  });
}

function pageSize(value = 50): number {
  if (!Number.isInteger(value) || value < 1 || value > 100) throw new Error("Page size must be between 1 and 100");
  return value;
}

async function migrateProfile(database: admin.firestore.Firestore, userId: string, apply: boolean, runId: string, expectedDigest?: string): Promise<ProfileMigrationItem> {
  if (!userId || userId.includes("/")) throw new Error("Invalid profile ID");
  return database.runTransaction(async transaction => {
    const userRef = database.collection("users").doc(userId);
    const publicRef = database.collection("public_profiles").doc(userId);
    const [user, existing] = await transaction.getAll(userRef, publicRef);
    const inputDigest = digest([user, existing]);
    if (expectedDigest && inputDigest !== expectedDigest) throw new Error(`Stale profile plan: ${userId}`);
    const desired = user.exists && user.get("isActive") !== false ? publicProfile(user.id, user.data()!) : null;
    const action: ProfileMigrationItem["action"] = desired === null
      ? existing.exists ? "delete" : "unchanged"
      : !existing.exists ? "create" : isDeepStrictEqual(existing.data(), desired) ? "unchanged" : "replace";
    if (apply && action !== "unchanged") {
      const receiptId = createHash("sha256").update(`${runId}:${publicRef.path}:${inputDigest}`).digest("hex");
      transaction.create(database.collection("migration_audit").doc(receiptId), {
        runId, target: publicRef.path, before: existing.data() || null, after: desired,
        sourceDigest: inputDigest, createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
      if (desired === null) transaction.delete(publicRef);
      else transaction.set(publicRef, desired);
    }
    return { userId, action, applied: apply && action !== "unchanged", inputDigest };
  });
}

export async function applyPublicProfileItem(database: admin.firestore.Firestore, reviewed: ProfileMigrationItem, runId: string) {
  if (!/^[a-zA-Z0-9_-]{8,100}$/.test(runId) || !/^[a-f0-9]{64}$/.test(reviewed.inputDigest)) throw new Error("A reviewed profile plan and run ID are required");
  return migrateProfile(database, reviewed.userId, true, runId, reviewed.inputDigest);
}

export async function backfillPublicProfilePage(database: admin.firestore.Firestore, options: MigrationPageOptions = {}) {
  const limit = pageSize(options.pageSize);
  const collection = options.collection || "users";
  if (collection !== "users" && collection !== "public_profiles") throw new Error("Unsupported migration collection");
  let query = database.collection(collection).orderBy(admin.firestore.FieldPath.documentId()).limit(limit + 1);
  if (options.afterId) query = query.startAfter(options.afterId);
  const page = await query.get();
  const selected = page.docs.slice(0, limit);
  const items: ProfileMigrationItem[] = [];
  for (const document of selected) {
    const result = await migrateProfile(database, document.id, options.apply === true, options.runId || "local-profile-rehearsal");
    items.push(result);
  }
  return {
    collection,
    mode: options.apply ? "apply" : "dry-run",
    scanned: selected.length,
    items,
    nextCursor: page.size > limit ? selected[selected.length - 1].id : null
  };
}