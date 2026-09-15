"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getApplicationContact = exports.getPublicProfile = exports.syncPublicProfile = void 0;
exports.publicProfile = publicProfile;
exports.refreshPublicProfile = refreshPublicProfile;
const admin = require("firebase-admin");
const functions = require("firebase-functions");
const validation_1 = require("./validation");
const db = admin.firestore();
const publicStrings = [
    "fullName", "name", "displayName", "profileImageUrl", "companyName", "businessName", "bio",
    "skills", "experience", "availability", "expectedSalary", "trustTier", "role", "activeRole"
];
const publicNumbers = ["averageRating", "totalRatings", "workerAverageRating", "workerTotalRatings", "createdAt"];
function publicProfile(userId, user) {
    const profile = { id: userId };
    for (const field of publicStrings) {
        if (typeof user[field] === "string")
            profile[field] = user[field].slice(0, 5000);
    }
    for (const field of publicNumbers) {
        if (typeof user[field] === "number" && Number.isFinite(user[field]))
            profile[field] = user[field];
    }
    for (const field of ["isActive", "profileCompleted"]) {
        if (typeof user[field] === "boolean")
            profile[field] = user[field];
    }
    if (Array.isArray(user.roles))
        profile.roles = user.roles.filter((role) => role === "WORKER" || role === "EMPLOYER");
    if (Array.isArray(user.skills))
        profile.skills = user.skills.filter((skill) => typeof skill === "string").slice(0, 100).join(", ");
    return profile;
}
async function refreshPublicProfile(userId) {
    return db.runTransaction(async (transaction) => {
        const user = await transaction.get(db.collection("users").doc(userId));
        const destination = db.collection("public_profiles").doc(userId);
        if (!user.exists || user.get("isActive") === false) {
            transaction.delete(destination);
            return null;
        }
        const profile = publicProfile(userId, user.data());
        transaction.set(destination, profile);
        return profile;
    });
}
exports.syncPublicProfile = functions.firestore.document("users/{userId}").onWrite(async (_, context) => {
    await refreshPublicProfile(context.params.userId);
});
exports.getPublicProfile = functions.https.onCall(async (data, context) => {
    if (!context.auth)
        throw new functions.https.HttpsError("unauthenticated", "Sign in to view this profile");
    const userId = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.userId, "userId", { required: true, maxLength: 128, pattern: /^[a-zA-Z0-9_-]+$/ });
    return { profile: await refreshPublicProfile(userId) };
});
exports.getApplicationContact = functions.https.onCall(async (data, context) => {
    var _a;
    const userId = (_a = context.auth) === null || _a === void 0 ? void 0 : _a.uid;
    if (!userId)
        throw new functions.https.HttpsError("unauthenticated", "Sign in to view application contacts");
    const applicationId = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.applicationId, "applicationId", { required: true, maxLength: 256, pattern: /^[a-zA-Z0-9_-]+$/ });
    return db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(db.collection("job_applications").doc(applicationId));
        const application = snapshot.data();
        if (!application || (application.workerId !== userId && application.employerId !== userId)) {
            throw new functions.https.HttpsError("permission-denied", "Only application participants may access contacts");
        }
        if (application.active === false || ["REJECTED", "WITHDRAWN"].includes(application.status)) {
            throw new functions.https.HttpsError("permission-denied", "Contact access is no longer available for this application");
        }
        const targetUserId = application.workerId === userId ? application.employerId : application.workerId;
        const target = await transaction.get(db.collection("users").doc(targetUserId));
        if (!target.exists)
            throw new functions.https.HttpsError("not-found", "Profile not found");
        const profile = publicProfile(targetUserId, target.data());
        for (const field of ["phone", "email"]) {
            if (typeof target.get(field) === "string")
                profile[field] = target.get(field);
        }
        return { profile };
    });
});
//# sourceMappingURL=profiles.js.map