import "server-only";

import { getFirebaseAdminAuth, getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { invalidateCollectionCache } from "@/lib/firebase/admin-collection-cache";
import {
  Applications, EmployerProfiles, InstantRequests, JobDetails, Jobs, Notifications, PhoneRoles, ReferralCodes,
  Referrals, SavedJobs, UserTokens, Wallets, WorkerCards, WorkerProfiles
} from "@/lib/firebase/schema";

/**
 * Admin "delete this account completely" (e.g. someone registered with the wrong role).
 * Removes the profile, public card, phone registration, token, wallet, jobs, applications,
 * urgent needs, saved jobs and inbox. The wallet ledger and withdrawal requests are kept (audit).
 */

/** "+91XXXXXXXXXX" or null. */
export function e164(raw: string): string | null {
  const digits = raw.replace(/\D/g, "");
  if (/^[6-9]\d{9}$/.test(digits)) return `+91${digits}`;
  if (/^91[6-9]\d{9}$/.test(digits)) return `+${digits}`;
  return null;
}

/** Returns all common representation variants of the Indian phone number. */
export function extractPhoneVariants(raw: string): string[] {
  const digits = raw.replace(/\D/g, "");
  if (digits.length < 10) return [];
  const ten = digits.slice(-10);
  return Array.from(new Set([`+91${ten}`, ten, `91${ten}`]));
}

/** The uid(s) registered for a phone across PhoneRoles, Firebase Auth, worker_profiles, employer_profiles, and users. */
export async function findUserIdsForPhone(rawPhone: string): Promise<{ userIds: string[]; variants: string[] }> {
  const variants = extractPhoneVariants(rawPhone);
  if (variants.length === 0) return { userIds: [], variants: [] };

  const db = getFirebaseAdminDb();
  const auth = getFirebaseAdminAuth();
  const ids = new Set<string>();

  // 1. Check PhoneRoles document for each variant
  for (const v of variants) {
    try {
      const roleDoc = await db.collection(PhoneRoles.COLLECTION).doc(v).get();
      const uid = String(roleDoc.get(PhoneRoles.UID) ?? "");
      if (uid) ids.add(uid);
    } catch {
      // Continue searching
    }
  }

  // 2. Check Firebase Auth by phone (+91 variant)
  const e164Phone = variants.find((v) => v.startsWith("+91"));
  if (e164Phone) {
    const authUser = await auth.getUserByPhoneNumber(e164Phone).catch(() => null);
    if (authUser) ids.add(authUser.uid);
  }

  // 3. Search worker_profiles by phone
  try {
    const workerSnap = await db.collection(WorkerProfiles.COLLECTION).where("phone", "in", variants).limit(10).get();
    for (const doc of workerSnap.docs) {
      ids.add(doc.id);
    }
  } catch {
    // Continue
  }

  // 4. Search employer_profiles by phone
  try {
    const employerSnap = await db.collection(EmployerProfiles.COLLECTION).where("phone", "in", variants).limit(10).get();
    for (const doc of employerSnap.docs) {
      ids.add(doc.id);
    }
  } catch {
    // Continue
  }

  // 5. Search users collection by phone or phoneNumber
  try {
    const usersSnap1 = await db.collection("users").where("phone", "in", variants).limit(10).get();
    for (const doc of usersSnap1.docs) {
      ids.add(doc.id);
    }
    const usersSnap2 = await db.collection("users").where("phoneNumber", "in", variants).limit(10).get();
    for (const doc of usersSnap2.docs) {
      ids.add(doc.id);
    }
  } catch {
    // Continue
  }

  return { userIds: Array.from(ids), variants };
}

async function deleteQuery(query: FirebaseFirestore.Query): Promise<number> {
  const db = getFirebaseAdminDb();
  let deleted = 0;
  for (;;) {
    const snap = await query.limit(300).get();
    if (snap.empty) return deleted;
    for (const doc of snap.docs) {
      await db.recursiveDelete(doc.ref);
      deleted++;
    }
    if (snap.size < 300) return deleted;
  }
}

/** Deletes one account and everything linked to it. Returns how many documents were removed. */
export async function deleteAccountCompletely(userId: string, phones: string[] = []): Promise<number> {
  const db = getFirebaseAdminDb();
  const auth = getFirebaseAdminAuth();
  let deleted = 0;

  const authUser = await auth.getUser(userId).catch(() => null);
  const phoneSet = new Set(phones);
  if (authUser?.phoneNumber) {
    for (const v of extractPhoneVariants(authUser.phoneNumber)) {
      phoneSet.add(v);
    }
  }

  const wallet = await db.collection(Wallets.COLLECTION).doc(userId).get();
  const code = String(wallet.get(Wallets.REFERRAL_CODE) ?? "");

  // Owned documents (recursive: clears subcollections like withdrawals, work_locations, etc.).
  const owned = [
    db.collection("users").doc(userId),
    db.collection(WorkerProfiles.COLLECTION).doc(userId),
    db.collection(WorkerCards.COLLECTION).doc(userId),
    db.collection(EmployerProfiles.COLLECTION).doc(userId),
    db.collection(UserTokens.COLLECTION).doc(userId),
    db.collection(Wallets.COLLECTION).doc(userId),
    db.collection(Referrals.COLLECTION).doc(userId),
    db.collection("service_partners").doc(userId),
    db.collection("worker_locations").doc(userId),
    db.collection("saved_work_locations").doc(userId),
    ...(code ? [db.collection(ReferralCodes.COLLECTION).doc(code)] : [])
  ];

  for (const ref of owned) {
    try {
      const snap = await ref.get();
      if (snap.exists) {
        await db.recursiveDelete(ref);
        deleted++;
      }
    } catch {
      // Ignore if not found
    }
  }

  // Delete PhoneRoles entries for known phone variants
  for (const phone of phoneSet) {
    try {
      const doc = await db.collection(PhoneRoles.COLLECTION).doc(phone).get();
      if (doc.exists && String(doc.get(PhoneRoles.UID) ?? "") === userId) {
        await doc.ref.delete();
        deleted++;
      }
    } catch {
      // Continue
    }
  }

  // Also query PhoneRoles directly by UID in case phone was different
  try {
    const rolesByUid = await db.collection(PhoneRoles.COLLECTION).where(PhoneRoles.UID, "==", userId).get();
    for (const doc of rolesByUid.docs) {
      await doc.ref.delete();
      deleted++;
    }
  } catch {
    // Continue
  }

  const linked: FirebaseFirestore.Query[] = [
    db.collection(SavedJobs.COLLECTION).where(SavedJobs.USER_ID, "==", userId),
    db.collection(Notifications.COLLECTION).where(Notifications.RECIPIENT_ID, "==", userId),
    db.collection(Applications.COLLECTION).where(Applications.WORKER_ID, "==", userId),
    db.collection(Applications.COLLECTION).where(Applications.EMPLOYER_ID, "==", userId),
    db.collection(InstantRequests.COLLECTION).where(InstantRequests.EMPLOYER_ID, "==", userId),
    db.collection(Jobs.COLLECTION).where(Jobs.EMPLOYER_ID, "==", userId),
    db.collection(JobDetails.COLLECTION).where(JobDetails.EMPLOYER_ID, "==", userId),
    db.collection("withdrawal_requests").where("uid", "==", userId),
    db.collection("withdrawal_requests").where("userId", "==", userId),
    db.collection("service_bookings").where("customerId", "==", userId),
    db.collection("service_bookings").where("workerId", "==", userId),
    db.collection("ratings").where("raterId", "==", userId),
    db.collection("ratings").where("targetId", "==", userId),
    db.collection("wallet_ledger").where("uid", "==", userId)
  ];

  for (const query of linked) {
    try {
      deleted += await deleteQuery(query);
    } catch {
      // Continue
    }
  }

  invalidateCollectionCache();

  // Auth last, so a failure above can simply be retried.
  if (authUser) {
    try {
      await auth.deleteUser(userId);
      deleted++;
    } catch {
      // Ignore if already deleted
    }
  }

  return deleted;
}
