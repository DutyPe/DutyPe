import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();

/**
 * Aggregates the total number of call button taps across the platform.
 *
 * This function triggers whenever a call-session document is created.
 * Each create corresponds to one call tap, and the total is rolled up into
 * system_metrics/global for dashboards.
 */
export const onCallLogged = functions.firestore
  .document("job_call_sessions/{sessionId}")
  .onCreate(async (snapshot) => {
    const afterData = snapshot.data();
    if (!afterData) {
      return null;
    }

    const increment = typeof afterData.callButtonTaps === "number" && afterData.callButtonTaps > 0
      ? afterData.callButtonTaps
      : 1;
    const metricsRef = db.collection("system_metrics").doc("global");

    functions.logger.info(`Aggregating ${increment} call tap(s).`);
    await metricsRef.set({
      totalCallButtonTaps: admin.firestore.FieldValue.increment(increment)
    }, { merge: true });
    return null;
  });