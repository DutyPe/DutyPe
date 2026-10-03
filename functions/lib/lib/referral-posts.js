"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.REFERRAL_POST_VALID_MS = void 0;
exports.referralPostsLeft = referralPostsLeft;
exports.useReferralPost = useReferralPost;
exports.refundReferralPost = refundReferralPost;
/**
 * Referral free posts: every friend who joins with an employer's code and starts using DutyPe
 * gives that employer one free post (a normal vacancy or an urgent post), valid for 24 hours.
 */
const admin = require("firebase-admin");
const schema_1 = require("../schema");
exports.REFERRAL_POST_VALID_MS = 24 * 60 * 60 * 1000;
/** Free referral posts the employer can use right now. */
function referralPostsLeft(employer, nowMs) {
    if (!employer)
        return 0;
    const until = employer[schema_1.EmployerProfiles.REFERRAL_FREE_POSTS_UNTIL];
    const untilMs = until instanceof admin.firestore.Timestamp ? until.toMillis() : 0;
    return untilMs > nowMs ? Math.max(0, Number(employer[schema_1.EmployerProfiles.REFERRAL_FREE_POSTS] || 0)) : 0;
}
/** Uses one referral post (call only when [referralPostsLeft] > 0, inside the transaction). */
function useReferralPost(tx, ref) {
    tx.update(ref, schema_1.EmployerProfiles.REFERRAL_FREE_POSTS, admin.firestore.FieldValue.increment(-1));
}
/** Gives a referral post back (post deleted or cancelled before anyone was hired). */
function refundReferralPost(tx, ref) {
    tx.update(ref, schema_1.EmployerProfiles.REFERRAL_FREE_POSTS, admin.firestore.FieldValue.increment(1));
}
//# sourceMappingURL=referral-posts.js.map