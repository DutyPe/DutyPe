/**
 * Scheduled Notifications - Enterprise Grade
 * 
 * Server-side notification logic using Firebase Cloud Functions
 * Follows Swiggy/Zomato/LinkedIn architecture pattern
 */

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

// ============================================
// HELPER FUNCTIONS
// ============================================

/**
 * Check if user can receive notification (deduplication + rate limiting)
 */
async function canSendNotification(
  userId: string,
  notificationType: string,
  minInterval: number
): Promise<boolean> {
  try {
    // Check last notification of this type
    const lastNotificationSnapshot = await admin.firestore()
      .collection('notification_tracking')
      .doc(userId)
      .collection('sent')
      .where('type', '==', notificationType)
      .orderBy('sentAt', 'desc')
      .limit(1)
      .get();
    
    if (!lastNotificationSnapshot.empty) {
      const lastSentAt = lastNotificationSnapshot.docs[0].data().sentAt || 0;
      const timeSinceLastNotification = Date.now() - lastSentAt;
      
      if (timeSinceLastNotification < minInterval) {
        console.log(`Deduplication: Skipping ${notificationType} for ${userId}`);
        return false;
      }
    }
    
    // Check daily rate limit (max 3 notifications per day)
    const todayStart = new Date();
    todayStart.setHours(0, 0, 0, 0);
    
    const todayNotificationsSnapshot = await admin.firestore()
      .collection('notification_tracking')
      .doc(userId)
      .collection('sent')
      .where('sentAt', '>', todayStart.getTime())
      .get();
    
    if (todayNotificationsSnapshot.size >= 3) {
      console.log(`Rate limit: User ${userId} reached daily limit`);
      return false;
    }
    
    return true;
  } catch (error) {
    console.error('Error checking notification eligibility:', error);
    return false;
  }
}

/**
 * Track notification sent
 */
async function trackNotificationSent(userId: string, notificationType: string): Promise<void> {
  try {
    await admin.firestore()
      .collection('notification_tracking')
      .doc(userId)
      .collection('sent')
      .add({
        type: notificationType,
        sentAt: Date.now()
      });
  } catch (error) {
    console.error('Error tracking notification:', error);
  }
}

async function trackNotificationSentWithKey(userId: string, notificationType: string, key: string): Promise<void> {
  try {
    await admin.firestore()
      .collection('notification_tracking')
      .doc(userId)
      .collection('sent')
      .add({
        type: notificationType,
        key,
        sentAt: Date.now()
      });
  } catch (error) {
    console.error('Error tracking notification with key:', error);
  }
}

/**
 * Delete expired notification inbox documents in batches.
 */
async function cleanupExpiredNotificationsBatch(): Promise<number> {
  const now = Date.now();
  const batchSize = 450;
  let totalDeleted = 0;

  while (true) {
    const snapshot = await admin.firestore()
      .collection('notifications')
      .where('expiresAt', '<=', now)
      .limit(batchSize)
      .get();

    if (snapshot.empty) {
      break;
    }

    const batch = admin.firestore().batch();
    snapshot.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();

    totalDeleted += snapshot.size;

    if (snapshot.size < batchSize) {
      break;
    }
  }

  return totalDeleted;
}

/**
 * Send FCM notification to user
 */
async function sendFCMNotification(
  userId: string,
  payload: {
    title: string;
    body: string;
    data: { [key: string]: string };
    priority: 'high' | 'normal';
    channel: string;
  }
): Promise<boolean> {
  try {
    // Get user's FCM token from users collection
    const userDoc = await admin.firestore()
      .collection('users')
      .doc(userId)
      .get();
    
    if (!userDoc.exists) {
      console.log(`No user found for ${userId}`);
      return false;
    }
    
    const token = userDoc.data()?.fcmToken;
    if (!token) {
      console.log(`No FCM token for user ${userId}`);
      return false;
    }
    
    // Send FCM message
    await admin.messaging().send({
      token: token,
      notification: {
        title: payload.title,
        body: payload.body
      },
      data: payload.data,
      android: {
        priority: payload.priority,
        notification: {
          channelId: payload.channel,
          sound: 'default',
          priority: payload.priority === 'high' ? 'high' : 'default',
          defaultSound: true,
          defaultVibrateTimings: true
        }
      }
    });
    
    console.log(`âœ… FCM notification sent to user ${userId}`);
    return true;
  } catch (error) {
    console.error(`Error sending FCM notification to ${userId}:`, error);
    return false;
  }
}

/**
 * Parse date of birth to day and month
 */
function parseDateOfBirth(dateOfBirth: string): { day: number; month: number } | null {
  try {
    const parts = dateOfBirth.split(/[/-]/);
    
    // Format: DD/MM/YYYY or DD-MM-YYYY
    if (parts.length === 3 && parts[0].length <= 2) {
      return {
        day: parseInt(parts[0]),
        month: parseInt(parts[1])
      };
    }
    
    // Format: YYYY-MM-DD
    if (parts.length === 3 && parts[0].length === 4) {
      return {
        day: parseInt(parts[2]),
        month: parseInt(parts[1])
      };
    }
    
    return null;
  } catch (error) {
    console.warn(`Could not parse date of birth: ${dateOfBirth}`);
    return null;
  }
}

/**
 * Check if it's quiet hours (10 PM - 8 AM)
 */
function isQuietHours(): boolean {
  const now = new Date();
  const hour = now.getHours();
  return hour >= 22 || hour < 8;
}

/**
 * Check if user's ACTIVE role matches the target role
 * ACTIVE ROLE NOTIFICATIONS:
 * - User only receives notifications for their currently active role
 * - If user is in WORKER mode â†’ only worker notifications
 * - If user is in EMPLOYER mode â†’ only employer notifications
 * - Birthday notifications sent to everyone regardless of active role
 */
function userActiveRoleMatches(user: any, targetRole: string): boolean {
  // Check new activeRole field (dual-role support)
  if (user.activeRole) {
    return user.activeRole === targetRole;
  }
  
  // Fallback to old single role field (backward compatibility)
  if (user.role) {
    return user.role === targetRole;
  }
  
  return false;
}

function simpleHash(input: string): number {
  let hash = 0;
  for (let i = 0; i < input.length; i++) {
    hash = ((hash << 5) - hash) + input.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
}

const WORKER_SMART_ENGAGEMENT_MESSAGES = [
  {
    title: '🎯 Fresh jobs are matching your profile',
    body: 'Open DutyPe now and apply early to improve your chances.',
    deepLink: 'dutype://jobs',
  },
  {
    title: '⚡ Quick reminder: complete one action today',
    body: 'Update profile skills or apply to one job to stay visible.',
    deepLink: 'dutype://profile',
  },
  {
    title: '📈 Small daily steps build bigger opportunities',
    body: 'Check new nearby openings and keep your momentum going.',
    deepLink: 'dutype://jobs',
  },
];

const EMPLOYER_SMART_ENGAGEMENT_MESSAGES = [
  {
    title: '👀 Candidates are waiting for your review',
    body: 'Review applications now to hire faster and avoid drop-offs.',
    deepLink: 'dutype://applications',
  },
  {
    title: '🚀 A quick update can improve response quality',
    body: 'Refresh one job post today to attract better-fit workers.',
    deepLink: 'dutype://post-job',
  },
  {
    title: '📊 Consistent activity improves hiring outcomes',
    body: 'Open DutyPe and take one hiring action right now.',
    deepLink: 'dutype://employer/home',
  },
];

async function sendRoleSpecificSmartEngagement(slot: number): Promise<void> {
  if (isQuietHours()) {
    console.log('🧠 Smart engagement: quiet hours - skipping');
    return;
  }

  const usersSnapshot = await admin.firestore()
    .collection('users')
    .limit(500)
    .get();

  let sentCount = 0;
  const dayOfMonth = new Date().getDate();

  for (const doc of usersSnapshot.docs) {
    const user = doc.data();
    const userId = doc.id;
    const role = user.activeRole || user.role;

    if (role !== 'WORKER' && role !== 'EMPLOYER') {
      continue;
    }

    const notificationType = `smart_engagement_${role.toLowerCase()}_${slot}`;
    const dedupeKey = `${notificationType}_${new Date().toISOString().slice(0, 10)}`;

    const alreadySentToday = await admin.firestore()
      .collection('notification_tracking')
      .doc(userId)
      .collection('sent')
      .where('key', '==', dedupeKey)
      .limit(1)
      .get();

    if (!alreadySentToday.empty) {
      continue;
    }

    const allowed = await canSendNotification(userId, notificationType, 4 * 60 * 60 * 1000);
    if (!allowed) {
      continue;
    }

    const pool = role === 'WORKER' ? WORKER_SMART_ENGAGEMENT_MESSAGES : EMPLOYER_SMART_ENGAGEMENT_MESSAGES;
    const messageIndex = (simpleHash(userId) + dayOfMonth + slot) % pool.length;
    const message = pool[messageIndex];

    const sent = await sendFCMNotification(userId, {
      title: message.title,
      body: message.body,
      data: {
        type: 'SMART_ENGAGEMENT',
        role,
        slot: slot.toString(),
        deepLink: message.deepLink,
      },
      priority: 'normal',
      channel: 'medium_priority',
    });

    if (sent) {
      await trackNotificationSentWithKey(userId, notificationType, dedupeKey);
      sentCount++;
    }
  }

  console.log(`🧠 Smart engagement slot ${slot}: sent ${sentCount} notifications`);
}

// ============================================
// SCHEDULED FUNCTIONS
// ============================================

/**
 * Check birthdays and send wishes
 * Runs every 3 hours
 * ROLE: Both WORKER and EMPLOYER (everyone gets birthday wishes!)
 */
export const checkBirthdays = functions.pubsub
  .schedule('every 3 hours')
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    console.log('ðŸŽ‚ ========== BIRTHDAY CHECK START ==========');
    console.log('ðŸŽ‚ Role: ALL USERS (Workers + Employers + Dual-Role)');
    
    // Skip during quiet hours
    if (isQuietHours()) {
      console.log('ðŸŽ‚ Quiet hours - skipping birthday check');
      return null;
    }
    
    const today = new Date();
    const todayDay = today.getDate();
    const todayMonth = today.getMonth() + 1;
    
    console.log(`ðŸŽ‚ Today's date: ${todayDay}/${todayMonth}/${today.getFullYear()}`);
    
    try {
      // Query all users (admin privileges - no permission errors!)
      const usersSnapshot = await admin.firestore()
        .collection('users')
        .limit(500)
        .get();
      
      console.log(`ðŸŽ‚ Checking ${usersSnapshot.size} users for birthdays`);
      
      let birthdayWishesSent = 0;
      let birthdaysFound = 0;
      
      for (const doc of usersSnapshot.docs) {
        const user = doc.data();
        const userId = doc.id;
        const dateOfBirth = user.dateOfBirth;
        const fullName = user.fullName || 'Friend';
        
        if (!dateOfBirth) continue;
        
        const parsed = parseDateOfBirth(dateOfBirth);
        if (!parsed) continue;
        
        // Check if today is their birthday
        if (parsed.day === todayDay && parsed.month === todayMonth) {
          birthdaysFound++;
          console.log(`ðŸŽ‚ ðŸŽ‰ BIRTHDAY FOUND: ${fullName} (userId: ${userId})`);
          
          // Check if we can send notification
          if (await canSendNotification(userId, 'birthday', 24 * 60 * 60 * 1000)) {
            const userName = fullName.split(' ')[0] || fullName;
            
            const sent = await sendFCMNotification(userId, {
              title: `ðŸŽ‚ Happy Birthday, ${userName}! ðŸŽ‰`,
              body: 'Wishing you a wonderful birthday filled with joy and success! May this year bring you amazing opportunities. - Team DutyPe',
              data: {
                type: 'BIRTHDAY',
                userName: userName,
                action: 'birthday_wish',
                deepLink: 'dutype://profile'
              },
              priority: 'high',
              channel: 'high_priority'
            });
            
            if (sent) {
              await trackNotificationSent(userId, 'birthday');
              birthdayWishesSent++;
            }
          }
        }
      }
      
      console.log('ðŸŽ‚ ========== BIRTHDAY CHECK COMPLETE ==========');
      console.log(`ðŸŽ‚ Found ${birthdaysFound} birthdays, Sent ${birthdayWishesSent} notifications`);
      
      return null;
    } catch (error) {
      console.error('ðŸŽ‚ Error checking birthdays:', error);
      return null;
    }
  });

/**
 * Check jobs expiring in next 24 hours
 * Runs every 1 hour
 * ROLE: EMPLOYER ONLY (only employers post jobs)
 * ACTIVE ROLE: Only sends to users whose ACTIVE role is EMPLOYER
 */
export const checkExpiringJobs = functions.pubsub
  .schedule('0 * * * *') // Every hour at minute 0
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    console.log('â° ========== EXPIRING JOBS CHECK START ==========');
    console.log('â° Role: EMPLOYER (active role only)');
    
    // Skip during quiet hours
    if (isQuietHours()) {
      console.log('â° Quiet hours - skipping expiring jobs check');
      return null;
    }
    
    const now = Date.now();
    const tomorrow = now + (24 * 60 * 60 * 1000);
    
    try {
      // Query jobs expiring in 24 hours
      const jobsSnapshot = await admin.firestore()
        .collection('jobs')
        .where('isActive', '==', true)
        .where('isFilled', '==', false)
        .where('expiresAt', '<', tomorrow)
        .where('expiresAt', '>', now)
        .limit(100)
        .get();
      
      console.log(`â° Found ${jobsSnapshot.size} jobs expiring in 24 hours`);
      
      let sentCount = 0;
      
      for (const doc of jobsSnapshot.docs) {
        const job = doc.data();
        const jobId = doc.id;
        const employerId = job.employerId;
        
        if (!employerId) continue;
        
        // Get employer user document to check active role
        const employerDoc = await admin.firestore()
          .collection('users')
          .doc(employerId)
          .get();
        
        if (!employerDoc.exists) continue;
        
        const employer = employerDoc.data();
        
        // Only send if user's ACTIVE role is EMPLOYER
        if (!userActiveRoleMatches(employer, 'EMPLOYER')) {
          console.log(`â° Skipping job ${jobId} - employer ${employerId} not in EMPLOYER mode`);
          continue;
        }
        
        // Check if we can send notification
        if (await canSendNotification(employerId, 'job_expiry', 24 * 60 * 60 * 1000)) {
          const hoursLeft = Math.floor((job.expiresAt - now) / (1000 * 60 * 60));
          
          const sent = await sendFCMNotification(employerId, {
            title: 'â° Job Expiring Soon',
            body: `Your job "${job.title}" expires in ${hoursLeft} hours. Renew it to keep receiving applications.`,
            data: {
              type: 'JOB_EXPIRY',
              jobId: jobId,
              jobTitle: job.title,
              hoursLeft: hoursLeft.toString(),
              deepLink: `dutype://job/${jobId}`
            },
            priority: 'high',
            channel: 'high_priority'
          });
          
          if (sent) {
            await trackNotificationSent(employerId, 'job_expiry');
            sentCount++;
          }
        }
      }
      
      console.log(`â° Sent ${sentCount} job expiry notifications`);
      console.log('â° ========== EXPIRING JOBS CHECK COMPLETE ==========');
      
      return null;
    } catch (error) {
      console.error('â° Error checking expiring jobs:', error);
      return null;
    }
  });

/**
 * Check pending applications and remind employers
 * Runs every 6 hours
 * ROLE: EMPLOYER ONLY (only employers review applications)
 * ACTIVE ROLE: Only sends to users whose ACTIVE role is EMPLOYER
 */
export const checkPendingApplications = functions.pubsub
  .schedule('every 6 hours')
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    console.log('ðŸ“‹ ========== PENDING APPLICATIONS CHECK START ==========');
    console.log('ðŸ“‹ Role: EMPLOYER (active role only)');
    
    // Skip during quiet hours
    if (isQuietHours()) {
      console.log('ðŸ“‹ Quiet hours - skipping pending applications check');
      return null;
    }
    
    const twoDaysAgo = Date.now() - (48 * 60 * 60 * 1000);
    
    try {
      // Query pending applications older than 48 hours
      const applicationsSnapshot = await admin.firestore()
        .collection('job_applications')
        .where('status', '==', 'PENDING')
        .where('appliedAt', '<', twoDaysAgo)
        .limit(100)
        .get();
      
      // Group by employer
      const employerApplications = new Map<string, number>();
      
      applicationsSnapshot.docs.forEach(doc => {
        const app = doc.data();
        const employerId = app.employerId;
        if (employerId) {
          employerApplications.set(employerId, (employerApplications.get(employerId) || 0) + 1);
        }
      });
      
      console.log(`ðŸ“‹ Found ${employerApplications.size} employers with pending applications`);
      
      let sentCount = 0;
      
      for (const [employerId, count] of employerApplications.entries()) {
        // Get employer user document to check active role
        const employerDoc = await admin.firestore()
          .collection('users')
          .doc(employerId)
          .get();
        
        if (!employerDoc.exists) continue;
        
        const employer = employerDoc.data();
        
        // Only send if user's ACTIVE role is EMPLOYER
        if (!userActiveRoleMatches(employer, 'EMPLOYER')) {
          console.log(`ðŸ“‹ Skipping employer ${employerId} - not in EMPLOYER mode`);
          continue;
        }
        
        // Check if we can send notification
        if (await canSendNotification(employerId, 'pending_applications', 12 * 60 * 60 * 1000)) {
          const sent = await sendFCMNotification(employerId, {
            title: 'ðŸ“‹ Pending Applications',
            body: `You have ${count} pending application${count > 1 ? 's' : ''} waiting for your review. Don't miss out on great candidates!`,
            data: {
              type: 'PENDING_APPLICATIONS',
              count: count.toString(),
              deepLink: 'dutype://applications'
            },
            priority: 'normal',
            channel: 'medium_priority'
          });
          
          if (sent) {
            await trackNotificationSent(employerId, 'pending_applications');
            sentCount++;
          }
        }
      }
      
      console.log(`ðŸ“‹ Sent ${sentCount} pending application reminders`);

      // Run expired notifications cleanup in the same scheduled cycle.
      const deleted = await cleanupExpiredNotificationsBatch();
      if (deleted > 0) {
        console.log(`🧹 Cleanup: deleted ${deleted} expired notifications`);
      }

      console.log('ðŸ“‹ ========== PENDING APPLICATIONS CHECK COMPLETE ==========');
      
      return null;
    } catch (error) {
      console.error('ðŸ“‹ Error checking pending applications:', error);
      return null;
    }
  });

/**
 * Remind workers about pending applications after 1 day
 * Runs every 6 hours
 * ROLE: WORKER ONLY (reminds workers their application is still pending)
 * SMART LOGIC: One notification per job, no quiet hours, rate limit per job
 */
export const remindWorkersPendingApplications = functions.pubsub
  .schedule('every 6 hours')
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    console.log('â° ========== WORKER PENDING APPLICATION REMINDERS START ==========');
    console.log('â° Role: WORKER (active role only)');
    console.log('â° Smart Logic: One notification per job, no quiet hours');
    
    // NO QUIET HOURS CHECK - Send anytime for urgent job updates
    
    try {
      // Query applications that are pending for more than 24 hours
      const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
      
      const applicationsSnapshot = await admin.firestore()
        .collection('job_applications')
        .where('status', '==', 'PENDING')
        .where('appliedAt', '<', oneDayAgo)
        .get();
      
      console.log(`â° Found ${applicationsSnapshot.size} pending applications older than 24 hours`);
      
      let sentCount = 0;
      
      // Process each application individually (one notification per job)
      for (const doc of applicationsSnapshot.docs) {
        const app = doc.data();
        const workerId = app.workerId;
        const jobId = app.jobId;
        const applicationId = doc.id;
        const employerId = app.employerId;
        const appliedAt = app.appliedAt || 0;
        
        // Check if user's ACTIVE role is WORKER
        const userDoc = await admin.firestore().collection('users').doc(workerId).get();
        if (!userDoc.exists) continue;
        
        const userData = userDoc.data();
        const activeRole = userData?.activeRole || userData?.role || 'WORKER';
        
        // Only send to users whose ACTIVE role is WORKER
        if (activeRole !== 'WORKER') {
          console.log(`â° Skipping ${workerId} - active role is ${activeRole}, not WORKER`);
          continue;
        }
        
        // Get job details
        const jobDoc = await admin.firestore().collection('jobs').doc(jobId).get();
        if (!jobDoc.exists) continue;
        
        const jobData = jobDoc.data();
        const jobTitle = jobData?.title || 'Job';
        const employerPhone = jobData?.employerPhone || '';
        
        // Calculate days pending
        const daysPending = Math.floor((Date.now() - appliedAt) / (24 * 60 * 60 * 1000));
        
        // SMART RATE LIMITING: send at most once every 24 hours for this specific application
        const notificationKey = `worker_pending_app_${applicationId}`;
        
        const existingNotifications = await admin.firestore()
          .collection('notification_tracking')
          .doc(workerId)
          .collection('sent')
          .where('key', '==', notificationKey)
          .limit(10)
          .get();

        if (!existingNotifications.empty) {
          const latestSentAt = existingNotifications.docs
            .map((d) => d.data().sentAt || 0)
            .reduce((max, value) => value > max ? value : max, 0);

          const hoursSinceLast = (Date.now() - latestSentAt) / (60 * 60 * 1000);
          if (hoursSinceLast < 24) {
            console.log(`â° Skipping ${applicationId} for ${workerId} - last sent ${hoursSinceLast.toFixed(1)}h ago`);
            continue;
          }
        }
        
        // Send notification
        const sent = await sendFCMNotification(workerId, {
          title: 'â° Application Still Pending',
          body: `Your application for "${jobTitle}" has been pending for ${daysPending} day${daysPending > 1 ? 's' : ''}. For faster updates, call the employer directly!`,
          data: {
            type: 'WORKER_PENDING_APPLICATION',
            jobId: jobId,
            applicationId: applicationId,
            employerId: employerId,
            daysPending: daysPending.toString(),
            deepLink: `dutype://job/${jobId}`,  // Opens job description screen
            action: 'view_job'
          },
          priority: 'normal',
          channel: 'low_priority'
        });
        
        if (sent) {
          // Track this specific notification to prevent duplicates
          await admin.firestore()
            .collection('notification_tracking')
            .doc(workerId)
            .collection('sent')
            .add({
              key: notificationKey,
              type: 'worker_pending_application',
              jobId: jobId,
              applicationId: applicationId,
              sentAt: Date.now(),
              timestamp: admin.firestore.FieldValue.serverTimestamp()
            });
          
          sentCount++;
          console.log(`â° Sent notification to worker ${workerId} for job ${jobId} (${jobTitle})`);
        }
      }
      
      console.log(`â° Sent ${sentCount} worker pending application reminders`);
      console.log('â° ========== WORKER PENDING APPLICATION REMINDERS COMPLETE ==========');
      
      return null;
    } catch (error) {
      console.error('â° Error sending worker pending application reminders:', error);
      return null;
    }
  });

/**
 * Smart role-specific engagement notifications
 * 3 slots/day to keep active users engaged with relevant actions.
 */
export const smartEngagementMorning = functions.pubsub
  .schedule('0 9 * * *')
  .timeZone('Asia/Kolkata')
  .onRun(async () => {
    console.log('🧠 ===== SMART ENGAGEMENT MORNING =====');
    await sendRoleSpecificSmartEngagement(0);
    return null;
  });

export const smartEngagementAfternoon = functions.pubsub
  .schedule('0 15 * * *')
  .timeZone('Asia/Kolkata')
  .onRun(async () => {
    console.log('🧠 ===== SMART ENGAGEMENT AFTERNOON =====');
    await sendRoleSpecificSmartEngagement(1);
    return null;
  });

export const smartEngagementEvening = functions.pubsub
  .schedule('0 20 * * *')
  .timeZone('Asia/Kolkata')
  .onRun(async () => {
    console.log('🧠 ===== SMART ENGAGEMENT EVENING =====');
    await sendRoleSpecificSmartEngagement(2);
    return null;
  });

/**
 * Re-engage inactive workers
 * Runs every 6 hours
 * ROLE: WORKER ONLY (only workers apply to jobs)
 * ACTIVE ROLE: Only sends to users whose ACTIVE role is WORKER
 */
export const reEngageInactiveWorkers = functions.pubsub
  .schedule('every 6 hours')
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    console.log('ðŸ’¼ ========== WORKER RE-ENGAGEMENT START ==========');
    console.log('ðŸ’¼ Role: WORKER (active role only)');
    
    // Skip during quiet hours
    if (isQuietHours()) {
      console.log('ðŸ’¼ Quiet hours - skipping worker re-engagement');
      return null;
    }
    
    const threeDaysAgo = Date.now() - (3 * 24 * 60 * 60 * 1000);
    
    try {
      // Query ALL users (we'll filter by active role in code)
      const usersSnapshot = await admin.firestore()
        .collection('users')
        .limit(200)
        .get();
      
      let reEngagedCount = 0;
      
      for (const doc of usersSnapshot.docs) {
        const user = doc.data();
        const userId = doc.id;
        
        // Check if user's ACTIVE role is WORKER
        if (!userActiveRoleMatches(user, 'WORKER')) {
          continue;
        }
        
        // Check if we can send notification
        if (!await canSendNotification(userId, 're_engagement', 24 * 60 * 60 * 1000)) {
          continue;
        }
        
        // Check last application
        const lastAppSnapshot = await admin.firestore()
          .collection('job_applications')
          .where('workerId', '==', userId)
          .orderBy('appliedAt', 'desc')
          .limit(1)
          .get();
        
        const shouldReEngage = lastAppSnapshot.empty || 
          (lastAppSnapshot.docs[0].data().appliedAt < threeDaysAgo);
        
        if (shouldReEngage) {
          const sent = await sendFCMNotification(userId, {
            title: 'ðŸ’¼ New Jobs Waiting For You!',
            body: 'Check out the latest job opportunities near you. Your next opportunity is just a tap away!',
            data: {
              type: 'RE_ENGAGEMENT',
              deepLink: 'dutype://jobs'
            },
            priority: 'normal',
            channel: 'low_priority'
          });
          
          if (sent) {
            await trackNotificationSent(userId, 're_engagement');
            reEngagedCount++;
          }
        }
      }
      
      console.log(`ðŸ’¼ Re-engaged ${reEngagedCount} inactive workers`);
      console.log('ðŸ’¼ ========== WORKER RE-ENGAGEMENT COMPLETE ==========');
      
      return null;
    } catch (error) {
      console.error('ðŸ’¼ Error re-engaging workers:', error);
      return null;
    }
  });

/**
 * Re-engage inactive employers
 * Runs every 12 hours
 * ROLE: EMPLOYER ONLY (only employers post jobs)
 * ACTIVE ROLE: Only sends to users whose ACTIVE role is EMPLOYER
 */
export const reEngageInactiveEmployers = functions.pubsub
  .schedule('every 12 hours')
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    console.log('ðŸ¢ ========== EMPLOYER RE-ENGAGEMENT START ==========');
    console.log('ðŸ¢ Role: EMPLOYER (active role only)');
    
    // Skip during quiet hours
    if (isQuietHours()) {
      console.log('ðŸ¢ Quiet hours - skipping employer re-engagement');
      return null;
    }
    
    const fifteenDaysAgo = Date.now() - (15 * 24 * 60 * 60 * 1000);
    
    try {
      // Query ALL users (we'll filter by active role in code)
      const usersSnapshot = await admin.firestore()
        .collection('users')
        .limit(200)
        .get();
      
      let reEngagedCount = 0;
      
      for (const doc of usersSnapshot.docs) {
        const user = doc.data();
        const userId = doc.id;
        
        // Check if user's ACTIVE role is EMPLOYER
        if (!userActiveRoleMatches(user, 'EMPLOYER')) {
          continue;
        }
        
        // Check if we can send notification
        if (!await canSendNotification(userId, 're_engagement', 24 * 60 * 60 * 1000)) {
          continue;
        }
        
        // Check last job post
        const lastJobSnapshot = await admin.firestore()
          .collection('jobs')
          .where('employerId', '==', userId)
          .orderBy('postedAt', 'desc')
          .limit(1)
          .get();
        
        const shouldReEngage = lastJobSnapshot.empty || 
          (lastJobSnapshot.docs[0].data().postedAt < fifteenDaysAgo);
        
        if (shouldReEngage) {
          const sent = await sendFCMNotification(userId, {
            title: 'ðŸ¢ Ready to Hire?',
            body: 'Post a job and connect with thousands of qualified workers in your area. Hiring made easy!',
            data: {
              type: 'RE_ENGAGEMENT',
              deepLink: 'dutype://post-job'
            },
            priority: 'normal',
            channel: 'low_priority'
          });
          
          if (sent) {
            await trackNotificationSent(userId, 're_engagement');
            reEngagedCount++;
          }
        }
      }
      
      console.log(`ðŸ¢ Re-engaged ${reEngagedCount} inactive employers`);
      console.log('ðŸ¢ ========== EMPLOYER RE-ENGAGEMENT COMPLETE ==========');
      
      return null;
    } catch (error) {
      console.error('ðŸ¢ Error re-engaging employers:', error);
      return null;
    }
  });

/**
 * Notify workers about application status updates
 * Triggered when employer updates application status
 * ROLE: WORKER ONLY (workers receive application updates)
 */
export const notifyApplicationStatusUpdate = functions.firestore
  .document('applications/{applicationId}')
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    
    // Only notify if status changed
    if (before.status === after.status) {
      return null;
    }
    
    const workerId = after.workerId;
    const jobTitle = after.jobTitle || 'a job';
    const newStatus = after.status;
    
    console.log(`ðŸ“¬ Application status changed: ${before.status} â†’ ${newStatus} for worker ${workerId}`);
    
    // Determine notification message based on status
    let title = '';
    let body = '';
    let priority: 'high' | 'normal' = 'high';
    
    switch (newStatus) {
      case 'ACCEPTED':
        title = 'ðŸŽ‰ Application Accepted!';
        body = `Great news! Your application for "${jobTitle}" has been accepted. The employer will contact you soon.`;
        break;
      case 'REJECTED':
        title = 'ðŸ“‹ Application Update';
        body = `Your application for "${jobTitle}" was not selected this time. Keep applying!`;
        priority = 'normal';
        break;
      case 'SHORTLISTED':
        title = 'â­ You\'re Shortlisted!';
        body = `Congratulations! You've been shortlisted for "${jobTitle}". The employer may contact you soon.`;
        break;
      default:
        return null; // Don't notify for other status changes
    }
    
    try {
      const sent = await sendFCMNotification(workerId, {
        title,
        body,
        data: {
          type: 'APPLICATION_STATUS',
          applicationId: context.params.applicationId,
          jobId: after.jobId || '',
          status: newStatus,
          deepLink: `dutype://application/${context.params.applicationId}`
        },
        priority,
        channel: 'high_priority'
      });
      
      if (sent) {
        console.log(`âœ… Application status notification sent to worker ${workerId}`);
      }
      
      return null;
    } catch (error) {
      console.error('Error sending application status notification:', error);
      return null;
    }
  });

// ============================================
// GUEST ENGAGEMENT TOPIC MESSAGES
// ============================================

/**
 * Guest user engagement - sends FCM topic message to "guest_users" topic
 * three times per day (morning, afternoon, evening) on a rotating message set.
 *
 * The Android app subscribes unauthenticated installs to this topic at launch
 * and on logout, and unsubscribes immediately on sign-in.
 *
 * Messages rotate: jobs-waiting → fresh-openings → complete-profile
 * Quiet hours (10 PM – 8 AM) are respected.
 */
const GUEST_MESSAGES = [
  {
    title: '💼 New jobs near you are waiting!',
    body: 'Login to apply in one tap — don\'t miss out.',
  },
  {
    title: '🌟 50+ fresh openings posted today',
    body: 'Sign in and grab your chance before they fill up.',
  },
  {
    title: '🔓 Complete your profile, unlock matches',
    body: 'Personalised job recommendations are waiting for you — sign in now.',
  },
];

async function sendGuestEngagementTopicMessage(): Promise<void> {
  if (isQuietHours()) {
    console.log('👥 Guest engagement: quiet hours — skipping');
    return;
  }

  const hour = new Date().getHours();
  // Cycle through 3 messages based on time slot
  // 08-12 → slot 0, 12-17 → slot 1, 17-22 → slot 2
  let slot = 0;
  if (hour >= 12 && hour < 17) slot = 1;
  else if (hour >= 17) slot = 2;

  const { title, body } = GUEST_MESSAGES[slot];

  try {
    await admin.messaging().send({
      topic: 'guest_users',
      notification: { title, body },
      data: {
        type: 'GUEST_ENGAGEMENT',
        deepLink: 'dutype://login',
        channel: 'medium_priority',
      },
      android: {
        priority: 'normal',
        notification: {
          channelId: 'medium_priority',
          clickAction: 'FLUTTER_NOTIFICATION_CLICK',
        },
      },
    });
    console.log(`✅ Guest engagement topic message sent: "${title}"`);
  } catch (error) {
    console.error('❌ Failed to send guest engagement topic message:', error);
  }
}

/**
 * Morning slot  08:00 IST  – "New jobs near you are waiting!"
 */
export const guestEngagementMorning = functions.pubsub
  .schedule('0 8 * * *')
  .timeZone('Asia/Kolkata')
  .onRun(async () => {
    console.log('👥 ===== GUEST ENGAGEMENT — MORNING =====');
    await sendGuestEngagementTopicMessage();
    return null;
  });

/**
 * Afternoon slot  13:00 IST  – "50+ fresh openings posted today"
 */
export const guestEngagementAfternoon = functions.pubsub
  .schedule('0 13 * * *')
  .timeZone('Asia/Kolkata')
  .onRun(async () => {
    console.log('👥 ===== GUEST ENGAGEMENT — AFTERNOON =====');
    await sendGuestEngagementTopicMessage();
    return null;
  });

/**
 * Evening slot  19:00 IST  – "Complete your profile, unlock matches"
 */
export const guestEngagementEvening = functions.pubsub
  .schedule('0 19 * * *')
  .timeZone('Asia/Kolkata')
  .onRun(async () => {
    console.log('👥 ===== GUEST ENGAGEMENT — EVENING =====');
    await sendGuestEngagementTopicMessage();
    return null;
  });

/**
 * Notify employer about new job applications
 * Triggered when worker applies to a job
 * ROLE: EMPLOYER ONLY (employers receive application notifications)
 */
export const notifyNewApplication = functions.firestore
  .document('applications/{applicationId}')
  .onCreate(async (snapshot, context) => {
    const application = snapshot.data();
    const employerId = application.employerId;
    const workerName = application.workerName || 'A worker';
    const jobTitle = application.jobTitle || 'your job';
    
    console.log(`ðŸ“¬ New application from ${workerName} for job: ${jobTitle}`);
    
    try {
      const sent = await sendFCMNotification(employerId, {
        title: 'ðŸ“¬ New Application Received!',
        body: `${workerName} has applied for "${jobTitle}". Review their profile now!`,
        data: {
          type: 'NEW_APPLICATION',
          applicationId: context.params.applicationId,
          jobId: application.jobId || '',
          workerId: application.workerId || '',
          deepLink: `dutype://application/${context.params.applicationId}`
        },
        priority: 'high',
        channel: 'high_priority'
      });
      
      if (sent) {
        console.log(`âœ… New application notification sent to employer ${employerId}`);
      }
      
      return null;
    } catch (error) {
      console.error('Error sending new application notification:', error);
      return null;
    }
  });

/**
 * Cleanup expired notifications from Firestore inbox.
 * Runs daily and deletes notifications where expiresAt <= now.
 */
export const cleanupExpiredNotifications = functions.pubsub
  .schedule('30 2 * * *')
  .timeZone('Asia/Kolkata')
  .onRun(async () => {
    console.log('🧹 ===== CLEANUP EXPIRED NOTIFICATIONS START =====');
    const now = Date.now();
    const batchSize = 450;
    let totalDeleted = 0;

    try {
      while (true) {
        const snapshot = await admin.firestore()
          .collection('notifications')
          .where('expiresAt', '<=', now)
          .limit(batchSize)
          .get();

        if (snapshot.empty) {
          break;
        }

        const batch = admin.firestore().batch();
        snapshot.docs.forEach((doc) => batch.delete(doc.ref));
        await batch.commit();

        totalDeleted += snapshot.size;
        console.log(`🧹 Deleted ${snapshot.size} expired notifications in this batch`);

        if (snapshot.size < batchSize) {
          break;
        }
      }

      console.log(`🧹 ===== CLEANUP COMPLETE. Total deleted: ${totalDeleted} =====`);
      return null;
    } catch (error) {
      console.error('🧹 Error cleaning expired notifications:', error);
      return null;
    }
  });

