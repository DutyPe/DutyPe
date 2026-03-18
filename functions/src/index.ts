import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { validateString, validateMessage, validateEnum, checkRateLimit, validateUserId } from "./validation";

// Initialize Firebase Admin SDK
admin.initializeApp();

// ============================================
// SCHEDULED NOTIFICATIONS (Enterprise Grade)
// ============================================
export * from './scheduled-notifications';
export { cleanupExpiredNotifications } from './scheduled-notifications';
export * from './referral-system';
export * from './job-landing';
export * from './worker-landing';
export * from './employer-landing';

const db = admin.firestore();
const messaging = admin.messaging();

// ============================================
// RATE LIMITING CONSTANTS (P0 FIX #2)
// ============================================
const RATE_LIMITS = {
  FREE_USER: {
    JOBS_PER_HOUR: 2,
    JOBS_PER_DAY: 5,
  },
  PAID_USER: {
    JOBS_PER_HOUR: 10,
    JOBS_PER_DAY: 50,
  },
};

const ONE_HOUR_MS = 60 * 60 * 1000;
const ONE_DAY_MS = 24 * 60 * 60 * 1000;

// Topic constants (must match Android app)
const TOPIC_ALL_USERS = "all_users";
const TOPIC_WORKERS = "workers";
const TOPIC_EMPLOYERS = "employers";
const TOPIC_APP_UPDATES = "app_updates";

// ============================================
// P0 FIX #2: RATE LIMITING FOR JOB POSTS
// ============================================
// Prevents bots from spamming thousands of jobs
// Free users: 2 jobs/hour, 5 jobs/day
// Paid users: 10 jobs/hour, 50 jobs/day

/**
 * Rate Limiter - Triggered when a new job is created
 * Checks if user has exceeded their posting limit
 * If exceeded: Deletes job, flags user, sends alert
 */
export const enforceJobRateLimit = functions.firestore
  .document("jobs/{jobId}")
  .onCreate(async (snapshot, context) => {
    const job = snapshot.data();
    const jobId = context.params.jobId;
    const employerId = job.employerId;

    if (!employerId) {
      functions.logger.warn(`Job ${jobId} has no employerId, skipping rate limit`);
      return null;
    }

    functions.logger.info(`🛡️ RATE LIMIT: Checking job ${jobId} by employer ${employerId}`);

    try {
      const now = Date.now();
      const oneHourAgo = now - ONE_HOUR_MS;
      const oneDayAgo = now - ONE_DAY_MS;
      const oneHourAgoTs = admin.firestore.Timestamp.fromMillis(oneHourAgo);
      const oneDayAgoTs = admin.firestore.Timestamp.fromMillis(oneDayAgo);

      // Check if user is paid (default to free limits - subscriptions collection removed)
      const isPaidUser = false;
      const limits = RATE_LIMITS.FREE_USER;

      functions.logger.info(`🛡️ RATE LIMIT: User ${employerId} is ${isPaidUser ? "PAID" : "FREE"}`);

      // Count jobs posted in last hour
      const hourlyJobsSnapshot = await db.collection("jobs")
        .where("employerId", "==", employerId)
        .where("createdAt", ">", oneHourAgoTs)
        .get();

      const jobsInHour = hourlyJobsSnapshot.size;

      // Count jobs posted in last day
      const dailyJobsSnapshot = await db.collection("jobs")
        .where("employerId", "==", employerId)
        .where("createdAt", ">", oneDayAgoTs)
        .get();

      const jobsInDay = dailyJobsSnapshot.size;

      functions.logger.info(`🛡️ RATE LIMIT: User ${employerId} - Jobs in hour: ${jobsInHour}/${limits.JOBS_PER_HOUR}, Jobs in day: ${jobsInDay}/${limits.JOBS_PER_DAY}`);

      // Check hourly limit
      if (jobsInHour > limits.JOBS_PER_HOUR) {
        functions.logger.warn(`🛡️ RATE LIMIT: ⛔ HOURLY LIMIT EXCEEDED for ${employerId}`);
        
        // Delete the job
        await snapshot.ref.delete();
        
        functions.logger.warn(`🛡️ RATE LIMIT: Hourly violation logged for ${employerId}`);

        return { deleted: true, reason: "HOURLY_LIMIT_EXCEEDED" };
      }

      // Check daily limit
      if (jobsInDay > limits.JOBS_PER_DAY) {
        functions.logger.warn(`🛡️ RATE LIMIT: ⛔ DAILY LIMIT EXCEEDED for ${employerId}`);
        
        // Delete the job
        await snapshot.ref.delete();
        
        functions.logger.warn(`🛡️ RATE LIMIT: Daily violation logged for ${employerId}`);

        return { deleted: true, reason: "DAILY_LIMIT_EXCEEDED" };
      }

      functions.logger.info(`🛡️ RATE LIMIT: ✅ Job ${jobId} passed rate limit check`);
      return { deleted: false };

    } catch (error) {
      functions.logger.error(`🛡️ RATE LIMIT: Error checking rate limit for ${employerId}:`, error);
      // On error, allow the job (fail open) but log for investigation
      return null;
    }
  });

/**
 * Send broadcast notification to all users or specific role
 * Triggered when a document is created in "broadcast_notifications" collection
 * 
 * Document fields:
 * - title: string (required)
 * - message: string (required)
 * - topic: string (optional) - "all_users", "workers", "employers", "app_updates"
 *                              defaults to "all_users"
 * - type: string (optional) - notification type for app handling
 */
export const sendBroadcastNotification = functions.firestore
  .document("broadcast_notifications/{notificationId}")
  .onCreate(async (snapshot, context) => {
    const notification = snapshot.data();
    const notificationId = context.params.notificationId;

    functions.logger.info(`Processing broadcast notification: ${notificationId}`, notification);

    const title = notification.title || "DutyPe";
    const message = notification.message || "";
    const topic = notification.topic || TOPIC_ALL_USERS;
    const type = notification.type || "broadcast";

    // Validate topic
    const validTopics = [TOPIC_ALL_USERS, TOPIC_WORKERS, TOPIC_EMPLOYERS, TOPIC_APP_UPDATES];
    if (!validTopics.includes(topic)) {
      functions.logger.error(`Invalid topic: ${topic}`);
      await snapshot.ref.update({
        error: `Invalid topic: ${topic}. Valid topics: ${validTopics.join(", ")}`,
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      return null;
    }

    try {
      // Build the FCM message for topic
      const topicMessage: admin.messaging.Message = {
        topic: topic,
        data: {
          notificationId: notificationId,
          title: title,
          message: message,
          body: message,
          type: type,
          click_action: "FLUTTER_NOTIFICATION_CLICK",
        },
        android: {
          priority: "high",
          notification: {
            title: title,
            body: message,
            icon: "ic_notification",
            color: "#3B82F6",
            sound: "default",
            clickAction: "OPEN_ACTIVITY",
          },
        },
      };

      // Send to topic
      const response = await messaging.send(topicMessage);
      functions.logger.info(`Broadcast notification sent to topic ${topic}: ${response}`);

      // Update document with sent status
      await snapshot.ref.update({
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        fcmMessageId: response,
        status: "sent",
      });

      return response;
    } catch (error) {
      functions.logger.error("Error sending broadcast notification:", error);
      
      await snapshot.ref.update({
        error: String(error),
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        status: "failed",
      });
      
      return null;
    }
  });

/**
 * Triggered when a new notification document is created in Firestore
 * Sends push notification to the recipient's device
 * 
 * DEDUPLICATION STRATEGY:
 * 1. Check if sentAt exists (already processed)
 * 2. Use transaction to atomically check + mark as processing
 * 3. Check for duplicate notifications in last 5 seconds (same title + recipient)
 */
export const sendPushNotification = functions.firestore
  .document("notifications/{notificationId}")
  .onCreate(async (snapshot, context) => {
    const notification = snapshot.data();
    const notificationId = context.params.notificationId;

    functions.logger.info(`📬 FCM: Processing notification ${notificationId}`, {
      title: notification.title,
      type: notification.type,
      recipientId: notification.recipientId
    });

    // DEDUPLICATION CHECK 1: Already sent
    if (notification.sentAt) {
      functions.logger.warn(`📬 FCM: ⚠️ Notification ${notificationId} already sent, skipping duplicate`);
      return null;
    }

    const recipientId = notification.recipientId;
    if (!recipientId) {
      functions.logger.warn("📬 FCM: No recipientId in notification, skipping");
      return null;
    }

    try {
      // DEDUPLICATION CHECK 2: Transaction-based lock to prevent race conditions
      const lockResult = await db.runTransaction(async (transaction) => {
        const notifRef = snapshot.ref;
        const freshDoc = await transaction.get(notifRef);
        
        if (!freshDoc.exists) {
          functions.logger.warn(`📬 FCM: Notification ${notificationId} deleted before processing`);
          return { locked: false, reason: "deleted" };
        }
        
        const freshData = freshDoc.data();
        
        // Check if already marked as processing or sent
        if (freshData?.sentAt || freshData?.processing) {
          functions.logger.warn(`📬 FCM: ⚠️ Notification ${notificationId} already processing/sent`);
          return { locked: false, reason: "already_processing" };
        }
        
        // Mark as processing to prevent duplicate execution
        transaction.update(notifRef, {
          processing: true,
          processingStartedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        
        return { locked: true };
      });
      
      if (!lockResult.locked) {
        functions.logger.info(`📬 FCM: Skipping notification ${notificationId} - ${lockResult.reason}`);
        return null;
      }

      // DEDUPLICATION CHECK 3: Check for duplicate notifications in last 5 seconds
      const fiveSecondsAgo = Date.now() - 5000;
      const duplicateCheck = await db.collection("notifications")
        .where("recipientId", "==", recipientId)
        .where("title", "==", notification.title)
        .where("type", "==", notification.type)
        .where("createdAt", ">", fiveSecondsAgo)
        .limit(5)
        .get();
      
      if (duplicateCheck.size > 1) {
        // Found duplicates - only process the first one (oldest)
        const sortedDocs = duplicateCheck.docs.sort((a, b) => {
          const aTime = a.data().createdAt || 0;
          const bTime = b.data().createdAt || 0;
          return aTime - bTime;
        });
        
        const firstDocId = sortedDocs[0].id;
        if (notificationId !== firstDocId) {
          functions.logger.warn(`📬 FCM: ⚠️ Duplicate notification detected! Skipping ${notificationId}, keeping ${firstDocId}`);
          
          // Mark this as duplicate and skip
          await snapshot.ref.update({
            processing: false,
            sentAt: admin.firestore.FieldValue.serverTimestamp(),
            skipped: true,
            skipReason: "DUPLICATE_NOTIFICATION",
            duplicateOf: firstDocId
          });
          
          return null;
        }
      }

      // Get recipient's FCM token from users collection
      const userDoc = await db.collection("users").doc(recipientId).get();
      
      if (!userDoc.exists) {
        functions.logger.warn(`📬 FCM: No user found for: ${recipientId}`);
        await snapshot.ref.update({
          processing: false,
          sentAt: admin.firestore.FieldValue.serverTimestamp(),
          error: "NO_FCM_TOKEN"
        });
        return null;
      }

      const userData = userDoc.data();
      if (!userData?.fcmToken) {
        functions.logger.warn(`📬 FCM: No FCM token for user: ${recipientId}`);
        await snapshot.ref.update({
          processing: false,
          sentAt: admin.firestore.FieldValue.serverTimestamp(),
          error: "INACTIVE_FCM_TOKEN"
        });
        return null;
      }

      const fcmToken = userData.fcmToken;

      // Extract deep link from notification data
      const deepLink = notification.data?.deepLink || "";
      
      // Map notification type to Android channel ID
      const notificationType = notification.type || "general";
      const highPriorityTypes = ["BIRTHDAY", "JOB_EXPIRY", "APPLICATION_STATUS", "JOB_ALERT", "NEW_APPLICATION", "APPLICATION_WITHDRAWN", "WORKER_HIRED", "PROFILE_COMPLETE", "JOB_POSTED", "JOB_PAUSED", "SHORTLISTED", "REJECTED", "APPLICATION_STATUS_UPDATE", "WELCOME"];
      const mediumPriorityTypes = ["PENDING_APPLICATIONS", "JOB_RECOMMENDATION", "REMINDER", "INTERVIEW_SCHEDULED"];
      let channelId = "low_priority";
      if (highPriorityTypes.includes(notificationType)) {
        channelId = "high_priority";
      } else if (mediumPriorityTypes.includes(notificationType)) {
        channelId = "medium_priority";
      }

      // Build the FCM message.
      // DATA-ONLY message (no android.notification block) so that onMessageReceived()
      // is ALWAYS called by DutyPeMessagingService regardless of whether the app is
      // in foreground, background, or killed. This gives the app full control over
      // how the notification is displayed and ensures deep-links work correctly.
      const message: admin.messaging.Message = {
        token: fcmToken,
        data: {
          notificationId: notificationId,
          title: notification.title || "DutyPe",
          message: notification.message || "",
          body: notification.message || "",
          type: notificationType,
          deepLink: deepLink,
          channel: channelId,
        },
        android: {
          priority: "high",
        },
      };

      // Send the notification
      const response = await messaging.send(message);
      functions.logger.info(`📬 FCM: ✅ Notification sent successfully: ${response}`);

      // Update notification document with sent status
      await snapshot.ref.update({
        processing: false,
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        fcmMessageId: response,
      });

      return response;
    } catch (error) {
      functions.logger.error(`📬 FCM: ❌ Error sending notification ${notificationId}:`, error);
      
      // Update notification with error status
      await snapshot.ref.update({
        processing: false,
        error: String(error),
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      
      return null;
    }
  });


// ============================================
// P1 FIX #6: DUPLICATE JOB DETECTION
// ============================================
// Detects and flags duplicate/similar job postings
// Uses contact number matching + text similarity

/**
 * Calculate text similarity using Jaccard index
 */
function calculateTextSimilarity(text1: string, text2: string): number {
  if (!text1 || !text2) return 0;
  
  const words1 = new Set(text1.toLowerCase().split(/\s+/).filter(w => w.length > 3));
  const words2 = new Set(text2.toLowerCase().split(/\s+/).filter(w => w.length > 3));
  
  if (words1.size === 0 || words2.size === 0) return 0;
  
  const intersection = new Set([...words1].filter(x => words2.has(x)));
  const union = new Set([...words1, ...words2]);
  
  return intersection.size / union.size;
}

/**
 * Duplicate Job Detection - Triggered on job creation
 * Checks for:
 * 1. Same contact number from different user (suspicious)
 * 2. Similar description text (>70% match)
 * 3. Same title + location combination
 */
export const detectDuplicateJob = functions.firestore
  .document("jobs/{jobId}")
  .onCreate(async (snapshot, context) => {
    const job = snapshot.data();
    const jobId = context.params.jobId;
    const employerId = job.employerId;

    functions.logger.info(`🔍 DUPLICATE CHECK: Analyzing job ${jobId}`);

    try {
      let fraudScore = 0;
      const signals: string[] = [];
      const now = Date.now();
      const twentyFourHoursAgo = now - ONE_DAY_MS;
      const twentyFourHoursAgoTs = admin.firestore.Timestamp.fromMillis(twentyFourHoursAgo);

      // CHECK 1: Same contact number from DIFFERENT user (HIGH SUSPICION)
      if (job.contactNumber) {
        const sameContactJobs = await db.collection("jobs")
          .where("contactNumber", "==", job.contactNumber)
          .where("createdAt", ">", twentyFourHoursAgoTs)
          .limit(10)
          .get();

        const differentUserSameContact = sameContactJobs.docs.filter(
          doc => doc.data().employerId !== employerId && doc.id !== jobId
        );

        if (differentUserSameContact.length > 0) {
          fraudScore += 50;
          signals.push("SAME_CONTACT_DIFFERENT_USER");
          functions.logger.warn(`🔍 DUPLICATE: Same contact ${job.contactNumber} used by different user!`);
        }
      }

      // CHECK 2: Similar description (>70% match)
      const recentJobs = await db.collection("jobs")
        .where("createdAt", ">", twentyFourHoursAgoTs)
        .where("employerId", "!=", employerId)
        .limit(50)
        .get();

      for (const recentJob of recentJobs.docs) {
        if (recentJob.id === jobId) continue;
        
        const similarity = calculateTextSimilarity(
          job.description || "",
          recentJob.data().description || ""
        );

        if (similarity > 0.7) {
          fraudScore += 40;
          signals.push(`SIMILAR_DESCRIPTION_${Math.round(similarity * 100)}%`);
          functions.logger.warn(`🔍 DUPLICATE: ${Math.round(similarity * 100)}% similar to job ${recentJob.id}`);
          break; // One match is enough
        }
      }

      // CHECK 3: Same title + same area (within 1km)
      const jobLat = typeof job.latitude === "number"
        ? job.latitude
        : typeof job.location?.lat === "number"
          ? job.location.lat
          : null;
      const jobLng = typeof job.longitude === "number"
        ? job.longitude
        : typeof job.location?.lng === "number"
          ? job.location.lng
          : null;

      if (job.title && jobLat !== null && jobLng !== null) {
        const sameTitleJobs = await db.collection("jobs")
          .where("title", "==", job.title)
          .where("createdAt", ">", twentyFourHoursAgoTs)
          .limit(20)
          .get();

        for (const sameTitleJob of sameTitleJobs.docs) {
          if (sameTitleJob.id === jobId) continue;
          const otherJob = sameTitleJob.data();
          
          const otherLat = typeof otherJob.latitude === "number"
            ? otherJob.latitude
            : typeof otherJob.location?.lat === "number"
              ? otherJob.location.lat
              : null;
          const otherLng = typeof otherJob.longitude === "number"
            ? otherJob.longitude
            : typeof otherJob.location?.lng === "number"
              ? otherJob.location.lng
              : null;

          if (otherLat !== null && otherLng !== null) {
            // Simple distance check (approximate)
            const latDiff = Math.abs(jobLat - otherLat);
            const lngDiff = Math.abs(jobLng - otherLng);
            const isNearby = latDiff < 0.01 && lngDiff < 0.01; // ~1km
            
            if (isNearby && otherJob.employerId !== employerId) {
              fraudScore += 30;
              signals.push("SAME_TITLE_SAME_AREA");
              functions.logger.warn(`🔍 DUPLICATE: Same title "${job.title}" in same area`);
              break;
            }
          }
        }
      }

      // DETERMINE ACTION based on fraud score
      let moderationStatus = "AUTO_APPROVED";
      
      if (fraudScore >= 70) {
        // HIGH RISK: Auto-reject and hide
        moderationStatus = "AUTO_REJECTED";
        await snapshot.ref.update({
          status: "closed",
          moderationStatus: "AUTO_REJECTED",
          moderationReason: signals.join(", "),
          fraudScore: fraudScore,
        });
        
        functions.logger.warn(`🔍 DUPLICATE: ⛔ Job ${jobId} AUTO-REJECTED (score: ${fraudScore})`);
        
      } else if (fraudScore >= 40) {
        // MEDIUM RISK: Send to moderation queue
        moderationStatus = "PENDING_REVIEW";
        await snapshot.ref.update({
          moderationStatus: "PENDING_REVIEW",
          moderationReason: signals.join(", "),
          fraudScore: fraudScore,
        });
        
        // Create in-app notification instead of using legacy moderation queue collection.
        await db.collection("notifications").add({
          recipientId: employerId,
          title: "Job Under Review",
          message: `Your job \"${job.title}\" needs manual review due to duplicate signals.`,
          type: "MODERATION_REVIEW_REQUIRED",
          data: {
            jobId: jobId,
            fraudScore: fraudScore,
          },
          isRead: false,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        
        functions.logger.info(`🔍 DUPLICATE: ⚠️ Job ${jobId} sent to moderation (score: ${fraudScore})`);
        
      } else {
        // LOW RISK: Auto-approve
        await snapshot.ref.update({
          moderationStatus: "AUTO_APPROVED",
          fraudScore: fraudScore,
        });
        functions.logger.info(`🔍 DUPLICATE: ✅ Job ${jobId} passed duplicate check (score: ${fraudScore})`);
      }

      return { fraudScore, signals, moderationStatus };

    } catch (error) {
      functions.logger.error(`🔍 DUPLICATE: Error checking job ${jobId}:`, error);
      return null;
    }
  });


// ============================================
// P1 FIX #7: IP ADDRESS TRACKING & ANALYSIS
// ============================================
// Tracks IP addresses for fraud detection
// Detects scam networks operating from same location

/**
 * Log user activity with IP address
 * Called from Android app via HTTPS callable function
 */
export const logUserActivity = functions.https.onCall(async (data, context) => {
  // Activity logging removed - Firebase Analytics handles this
  return { success: true };
});


// ============================================
// P1 FIX #10: MODERATION QUEUE SYSTEM
// ============================================
// Jobs with medium fraud score go to moderation queue
// Admin approves/rejects before job goes live

/**
 * Process moderation decision
 * Called when admin approves or rejects a job in moderation queue
 */
export const processModerationDecision = functions.firestore
  .document("jobs/{jobId}")
  .onUpdate(async () => {
    // Legacy moderation queue path removed in final schema.
    return null;
  });




// ============================================
// P1: COMMUNITY REPORTING SYSTEM
// ============================================
// 3 reports = auto-hide job
// Crowd-sourced moderation for clean platform

const AUTO_HIDE_THRESHOLD = 3;

/**
 * Process job report and auto-hide if threshold reached
 * Triggered when a new report is created
 */
export const processJobReport = functions.firestore
  .document("job_reports/{reportId}")
  .onCreate(async (snapshot, context) => {
    const report = snapshot.data();
    const reportId = context.params.reportId;
    const jobId = report.jobId;

    if (!jobId) {
      functions.logger.warn(`Report ${reportId} has no jobId, skipping`);
      return null;
    }

    functions.logger.info(`🚨 REPORT: Processing report ${reportId} for job ${jobId}`);

    try {
      // Get job document
      const jobRef = db.collection("jobs").doc(jobId);
      const jobDoc = await jobRef.get();

      if (!jobDoc.exists) {
        functions.logger.warn(`Job ${jobId} not found`);
        return null;
      }

      const jobData = jobDoc.data();

      // Count reports from job_reports collection
      const reportsSnapshot = await db.collection("job_reports")
        .where("jobId", "==", jobId)
        .get();
      const currentReportCount = reportsSnapshot.size;

      // Check if threshold reached
      if (currentReportCount >= AUTO_HIDE_THRESHOLD) {
        functions.logger.warn(`🚨 REPORT: Job ${jobId} reached ${currentReportCount} reports - AUTO-HIDING`);

        // Deactivate the job
        await jobRef.update({
          status: "closed",
          moderationStatus: "HIDDEN_BY_REPORTS",
        });

        // Notify employer instead of creating legacy moderation queue documents.
        if (jobData?.employerId) {
          await db.collection("notifications").add({
            recipientId: jobData.employerId,
            title: "Job Hidden for Review",
            message: `Your job \"${jobData.title}\" has been hidden due to community reports.`,
            type: "JOB_HIDDEN",
            data: {
              jobId: jobId,
              reportCount: currentReportCount,
            },
            isRead: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
          });
        }

        
      }

      functions.logger.info(`🚨 REPORT: Job ${jobId} now has ${currentReportCount} reports`);
      return { success: true, reportCount: currentReportCount };

    } catch (error) {
      functions.logger.error(`🚨 REPORT: Error processing report:`, error);
      return null;
    }
  });

/**
 * Get report statistics for admin dashboard
 */
export const getReportStats = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  try {
    const now = Date.now();
    const oneDayAgo = now - (24 * 60 * 60 * 1000);
    const oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000);

    // Get reports from last 24 hours
    const dailyReports = await db.collection("job_reports")
      .where("timestamp", ">", oneDayAgo)
      .get();

    // Get reports from last week
    const weeklyReports = await db.collection("job_reports")
      .where("timestamp", ">", oneWeekAgo)
      .get();

    return {
      dailyReports: dailyReports.size,
      weeklyReports: weeklyReports.size,
      pendingModeration: 0,
    };

  } catch (error) {
    functions.logger.error("Error getting report stats:", error);
    throw new functions.https.HttpsError("internal", "Failed to get report stats");
  }
});


// ============================================
// METADATA UPDATE FUNCTIONS
// ============================================

/**
 * Scheduled function to update platform metadata
 * Runs every hour to update job counts, category stats, etc.
 */
export const updatePlatformMetadata = functions.pubsub
  .schedule("every 1 hours")
  .onRun(async (context) => {
    functions.logger.info("📊 METADATA: disabled (metadata collection removed in final schema)");
    return null;
  });

/**
 * Trigger to update metadata when a new job is created
 */
export const updateMetadataOnJobCreate = functions.firestore
  .document("jobs/{jobId}")
  .onCreate(async (snapshot, context) => {
    functions.logger.info("📊 METADATA: update on job create skipped (metadata removed)");
    return null;
  });

/**
 * Trigger to update metadata when a job is deleted
 */
export const updateMetadataOnJobDelete = functions.firestore
  .document("jobs/{jobId}")
  .onDelete(async (snapshot, context) => {
    functions.logger.info("📊 METADATA: update on job delete skipped (metadata removed)");
    return null;
  });

/**
 * Trigger to update user count when a new user registers
 */
export const updateMetadataOnUserCreate = functions.firestore
  .document("users/{userId}")
  .onCreate(async (snapshot, context) => {
    functions.logger.info("📊 METADATA: update on user create skipped (metadata removed)");
    return null;
  });


// ============================================
// ENTERPRISE REFERRAL SYSTEM EXPORTS
// ============================================
// Import and re-export referral system functions
export {
  onUserProfileComplete,
  applyReferralCode,
  onReferredUserProfileComplete,
  expirePendingReferrals,
  requestWithdrawal,
  detectReferralFraud,
  getReferralStats,
  getReferralHistory,
  getReferralLeaderboard
} from "./referral-system";




// ============================================
// EXPORT JOB POSTING FUNCTIONS
// ============================================
export * from "./job-posting";
