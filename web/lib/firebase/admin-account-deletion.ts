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

/** The uid registered for a phone (phoneRoles, else the Auth phone login). */
export async function findUserIdsForPhone(rawPhone: string): Promise<{ userIds: string[]; variants: string[] }> {
  const phone = e164(rawPhone);
  if (!phone) return { userIds: [], variants: [] };
  const ids = new Set<string>();
  const roleDoc = await getFirebaseAdminDb().collection(PhoneRoles.COLLECTION).doc(phone).get();
  const uid = String(roleDoc.get(PhoneRoles.UID) ?? "");
  if (uid) ids.add(uid);
  const authUser = await getFirebaseAdminAuth().getUserByPhoneNumber(phone).catch(() => null);
  if (authUser) ids.add(authUser.uid);
  return { userIds: Array.from(ids), variants: [phone] };
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
  const authPhone = authUser?.phoneNumber ? e164(authUser.phoneNumber) : null;
  if (authPhone) phoneSet.add(authPhone);

  const wallet = await db.collection(Wallets.COLLECTION).doc(userId).get();
  const code = String(wallet.get(Wallets.REFERRAL_CODE) ?? "");

  // Owned documents (recursive: unlocks, work_locations).
  const owned = [
    db.collection(WorkerProfiles.COLLECTION).doc(userId),
    db.collection(WorkerCards.COLLECTION).doc(userId),
    db.collection(EmployerProfiles.COLLECTION).doc(userId),
    db.collection(UserTokens.COLLECTION).doc(userId),
    db.collection(Wallets.COLLECTION).doc(userId),
    db.collection(Referrals.COLLECTION).doc(userId),
    ...(code ? [db.collection(ReferralCodes.COLLECTION).doc(code)] : [])
  ];
  for (const ref of owned) {
    if ((await ref.get()).exists) {
      await db.recursiveDelete(ref);
      deleted++;
    }
  }

  for (const phone of phoneSet) {
    const doc = await db.collection(PhoneRoles.COLLECTION).doc(phone).get();
    if (doc.exists && String(doc.get(PhoneRoles.UID) ?? "") === userId) {
      await doc.ref.delete();
      deleted++;
    }
  }

  const linked: FirebaseFirestore.Query[] = [
    db.collection(SavedJobs.COLLECTION).where(SavedJobs.USER_ID, "==", userId),
    db.collection(Notifications.COLLECTION).where(Notifications.RECIPIENT_ID, "==", userId),
    db.collection(Applications.COLLECTION).where(Applications.WORKER_ID, "==", userId),
    db.collection(Applications.COLLECTION).where(Applications.EMPLOYER_ID, "==", userId),
    db.collection(InstantRequests.COLLECTION).where(InstantRequests.EMPLOYER_ID, "==", userId),
    // Deleting a card also clears its cell counts and applications (onJobWritten).
    db.collection(Jobs.COLLECTION).where(Jobs.EMPLOYER_ID, "==", userId),
    db.collection(JobDetails.COLLECTION).where(JobDetails.EMPLOYER_ID, "==", userId)
  ];
  for (const query of linked) deleted += await deleteQuery(query);

  invalidateCollectionCache();

  // Auth last, so a failure above can simply be retried.
  if (authUser) {
    await auth.deleteUser(userId);
    deleted++;
  }
  return deleted;
}
