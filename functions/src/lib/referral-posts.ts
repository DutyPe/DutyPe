/**
 * Referral free posts: every friend who joins with an employer's code and starts using DutyPe
 * gives that employer one free post (a normal vacancy or an urgent post), valid for 24 hours.
 */
import * as admin from "firebase-admin";
import { EmployerProfiles } from "../schema";

export const REFERRAL_POST_VALID_MS = 24 * 60 * 60 * 1000;

/** Free referral posts the employer can use right now. */
export function referralPostsLeft(employer: admin.firestore.DocumentData | undefined, nowMs: number): number {
  if (!employer) return 0;
  const until = employer[EmployerProfiles.REFERRAL_FREE_POSTS_UNTIL];
  const untilMs = until instanceof admin.firestore.Timestamp ? until.toMillis() : 0;
  return untilMs > nowMs ? Math.max(0, Number(employer[EmployerProfiles.REFERRAL_FREE_POSTS] || 0)) : 0;
}

/** Uses one referral post (call only when [referralPostsLeft] > 0, inside the transaction). */
export function useReferralPost(tx: admin.firestore.Transaction, ref: admin.firestore.DocumentReference): void {
  tx.update(ref, EmployerProfiles.REFERRAL_FREE_POSTS, admin.firestore.FieldValue.increment(-1));
}

/** Gives a referral post back (post deleted or cancelled before anyone was hired). */
export function refundReferralPost(tx: admin.firestore.Transaction, ref: admin.firestore.DocumentReference): void {
  tx.update(ref, EmployerProfiles.REFERRAL_FREE_POSTS, admin.firestore.FieldValue.increment(1));
}
