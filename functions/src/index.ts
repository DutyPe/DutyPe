/**
 * DutyPe Cloud Functions (asia-south1). Every module owns the collections it writes; see each file's header.
 */
import * as admin from "firebase-admin";

admin.initializeApp();

export * from "./profiles";
export * from "./jobs";
export * from "./applications";
export * from "./workers";
export * from "./instant";
export * from "./urgent";
export * from "./ratings";
export * from "./referrals";
export * from "./subscriptions";
export * from "./broadcast";
export * from "./scheduled-notifications";
export { updateReferralConfig, getReferralConfigCallable } from "./app-config";
export * from "./ai-job-parser";
export * from "./ai-hiring";
export * from "./dutype-ai";
