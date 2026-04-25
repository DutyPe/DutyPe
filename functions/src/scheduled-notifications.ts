/**
 * Scheduled Notifications - Enterprise Grade
 * 
 * Server-side notification logic using Firebase Cloud Functions
 * Follows Swiggy/Zomato/LinkedIn architecture pattern
 */

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { getUserLanguage, getUserDisplayName, tTitle, tBody, SE_WORKER_POOL, SE_EMPLOYER_POOL, SE_GUEST_POOL, SUPPORTED_LOCALES, localizedTopic } from './notification-i18n';

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
  return user.activeRole === targetRole;
}

function simpleHash(input: string): number {
  let hash = 0;
  for (let i = 0; i < input.length; i++) {
    hash = ((hash << 5) - hash) + input.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
}

// Time-of-day tags so messages like "Good morning" only fire in the morning,
// "Evening check" only fires in the evening, etc. Untagged messages can fire
// at any time of day.
type TimeOfDay = 'morning' | 'afternoon' | 'evening';

function currentTimeOfDay(): TimeOfDay {
  const hour = new Date().getHours();
  if (hour < 12) return 'morning';
  if (hour < 17) return 'afternoon';
  return 'evening';
}

function latLngFrom(value: any): { lat: number; lng: number } | null {
  if (!value || typeof value !== 'object') return null;
  const lat = Number(value.lat ?? value.latitude);
  const lng = Number(value.lng ?? value.longitude);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
  if (lat < -90 || lat > 90 || lng < -180 || lng > 180 || (lat === 0 && lng === 0)) return null;
  return { lat, lng };
}

function distanceKm(a: { lat: number; lng: number }, b: { lat: number; lng: number }): number {
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const earthKm = 6371;
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const lat1 = toRad(a.lat);
  const lat2 = toRad(b.lat);
  const h = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
  return 2 * earthKm * Math.asin(Math.sqrt(h));
}

async function getWorkerLocation(userId: string, user: any): Promise<{ lat: number; lng: number; label: string } | null> {
  const direct = latLngFrom(user.location) || latLngFrom(user.currentLocation) || latLngFrom(user.lastKnownLocation);
  if (direct) {
    return { ...direct, label: String(user.city || user.locationText || user.addressText || 'your area') };
  }

  const profile = await admin.firestore().collection('worker_profiles').doc(userId).get().catch(() => null);
  const data = profile && profile.exists ? profile.data() || {} : {};
  const profileLoc = latLngFrom(data.location) || latLngFrom(data.currentLocation) || latLngFrom(data.lastKnownLocation);
  if (!profileLoc) return null;
  return { ...profileLoc, label: String(data.city || data.locationText || data.addressText || 'your area') };
}

// Smart-engagement template pools (titles + bodies + deep links per language)
// live in `notification-i18n.ts` as SE_WORKER_POOL / SE_EMPLOYER_POOL.

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
    const role = user.activeRole;

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

    const fullPool = role === 'WORKER' ? SE_WORKER_POOL : SE_EMPLOYER_POOL;
    // Filter by time-of-day so e.g. "Good morning" never fires at 8 PM.
    const tod = currentTimeOfDay();
    const pool = fullPool.filter((m) => !m.timeOfDay || m.timeOfDay === tod);
    const messageIndex = (simpleHash(userId) + dayOfMonth + slot) % pool.length;
    const picked = pool[messageIndex];

    const locale = await getUserLanguage(admin.firestore(), userId);
    const title = tTitle(picked.id, locale);
    const body = tBody(picked.id, locale);

    const sent = await sendFCMNotification(userId, {
      title,
      body,
      data: {
        type: 'SMART_ENGAGEMENT',
        role,
        slot: slot.toString(),
        deepLink: picked.deepLink,
        locale,
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

export const workerNearbyJobsMorning = functions.pubsub
  .schedule('30 10 * * *')
  .timeZone('Asia/Kolkata')
  .onRun(async () => {
    if (isQuietHours()) {
      console.log('📍 Nearby jobs: quiet hours - skipping');
      return null;
    }

    const [usersSnapshot, jobsSnapshot] = await Promise.all([
      admin.firestore().collection('users').where('activeRole', '==', 'WORKER').limit(200).get(),
      admin.firestore().collection('jobmetadata').where('status', '==', 'open').orderBy('createdAt', 'desc').limit(250).get(),
    ]);

    const jobs = jobsSnapshot.docs.map((doc) => {
      const data = doc.data();
      const loc = latLngFrom(data.location);
      return loc ? { id: doc.id, ...loc, title: String(data.title || 'job'), city: String(data.companyCity || data.addressText || '') } : null;
    }).filter(Boolean) as Array<{ id: string; lat: number; lng: number; title: string; city: string }>;

    let sentCount = 0;
    for (const doc of usersSnapshot.docs) {
      const userId = doc.id;
      const user = doc.data();
      const workerLocation = await getWorkerLocation(userId, user);
      if (!workerLocation) continue;

      const nearby = jobs
        .map((job) => ({ ...job, km: distanceKm(workerLocation, job) }))
        .filter((job) => job.km <= 25)
        .sort((a, b) => a.km - b.km);

      if (nearby.length === 0) continue;

      const notificationType = 'worker_nearby_jobs';
      const dedupeKey = `${notificationType}_${new Date().toISOString().slice(0, 10)}`;
      const alreadySentToday = await admin.firestore()
        .collection('notification_tracking')
        .doc(userId)
        .collection('sent')
        .where('key', '==', dedupeKey)
        .limit(1)
        .get();
      if (!alreadySentToday.empty) continue;
      if (!await canSendNotification(userId, notificationType, 20 * 60 * 60 * 1000)) continue;

      const closest = nearby[0];
      const place = workerLocation.label && workerLocation.label !== 'your area'
        ? workerLocation.label
        : (closest.city.split(',')[0] || 'near you');
      const title = `Jobs are waiting in ${place}`;
      const body = `${nearby.length} nearby openings, including ${closest.title} about ${Math.max(1, Math.round(closest.km))} km away.`;

      const sent = await sendFCMNotification(userId, {
        title,
        body,
        data: {
          type: 'NEW_JOB_ALERT',
          jobId: closest.id,
          deepLink: `dutype://job/${closest.id}`,
        },
        priority: 'normal',
        channel: 'medium_priority',
      });

      if (sent) {
        await trackNotificationSentWithKey(userId, notificationType, dedupeKey);
        sentCount++;
      }
    }

    console.log(`📍 Nearby jobs: sent ${sentCount} location-based notifications`);
    return null;
  });

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
    console.log('[BIRTHDAY] ========== BIRTHDAY CHECK START ==========');
    console.log('[BIRTHDAY] Role: ALL USERS (Workers + Employers + Dual-Role)');
    
    // Skip during quiet hours
    if (isQuietHours()) {
      console.log('[BIRTHDAY] Quiet hours - skipping birthday check');
      return null;
    }
    
    const today = new Date();
    const todayDay = today.getDate();
    const todayMonth = today.getMonth() + 1;
    
    console.log(`[BIRTHDAY] Today's date: ${todayDay}/${todayMonth}/${today.getFullYear()}`);
    
    try {
      // Query all users (admin privileges - no permission errors!)
      const usersSnapshot = await admin.firestore()
        .collection('users')
        .limit(500)
        .get();
      
      console.log(`[BIRTHDAY] Checking ${usersSnapshot.size} users for birthdays`);
      
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
          console.log(`[BIRTHDAY] Birthday found: ${fullName} (userId: ${userId})`);
          
          // Check if we can send notification
          if (await canSendNotification(userId, 'birthday', 24 * 60 * 60 * 1000)) {
            const userName = fullName.split(' ')[0] || fullName;
            const locale = await getUserLanguage(admin.firestore(), userId);

            const sent = await sendFCMNotification(userId, {
              title: tTitle('BIRTHDAY', locale, { name: userName }),
              body: tBody('BIRTHDAY', locale, { name: userName }),
              data: {
                type: 'BIRTHDAY',
                userName: userName,
                action: 'birthday_wish',
                deepLink: 'dutype://profile',
                locale,
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
      
      console.log('[BIRTHDAY] ========== BIRTHDAY CHECK COMPLETE ==========');
      console.log(`[BIRTHDAY] Found ${birthdaysFound} birthdays, Sent ${birthdayWishesSent} notifications`);
      
      return null;
    } catch (error) {
      console.error('[BIRTHDAY] Error checking birthdays:', error);
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
      // Query job_details (employerId + expiresAt now live there).
      const detailsSnapshot = await admin.firestore()
        .collection('job_details')
        .where('expiresAt', '<', tomorrow)
        .where('expiresAt', '>', now)
        .limit(100)
        .get();
      
      console.log(`â° Found ${detailsSnapshot.size} job_details expiring in 24 hours`);
      
      let sentCount = 0;
      
      for (const detailsDoc of detailsSnapshot.docs) {
        const details = detailsDoc.data();
        const jobId = detailsDoc.id;
        const employerId = details.employerId;
        const expiresAt = details.expiresAt;
        
        if (!employerId) continue;
        
        // Fetch jobmetadata to verify status == 'open' and get title.
        const metaDoc = await admin.firestore().collection('jobmetadata').doc(jobId).get();
        if (!metaDoc.exists) continue;
        const job = metaDoc.data() || {};
        if (job.status !== 'open') continue;
        
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
          const hoursLeft = Math.floor((expiresAt - now) / (1000 * 60 * 60));
          const locale = await getUserLanguage(admin.firestore(), employerId);
          const recipient = await getUserDisplayName(admin.firestore(), employerId);
          const tParams = { jobTitle: job.title, hoursLeft, recipient };

          const sent = await sendFCMNotification(employerId, {
            title: tTitle('JOB_EXPIRY_SOON', locale, tParams),
            body: tBody('JOB_EXPIRY_SOON', locale, tParams),
            data: {
              type: 'JOB_EXPIRY',
              jobId: jobId,
              jobTitle: job.title,
              hoursLeft: hoursLeft.toString(),
              deepLink: `dutype://job/${jobId}`,
              locale
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
    console.log('[PENDING_APPLICATIONS] ========== PENDING APPLICATIONS CHECK START ==========');
    console.log('[PENDING_APPLICATIONS] Role: EMPLOYER (active role only)');
    
    // Skip during quiet hours
    if (isQuietHours()) {
      console.log('[PENDING_APPLICATIONS] Quiet hours - skipping pending applications check');
      return null;
    }
    
    const twoDaysAgo = Date.now() - (48 * 60 * 60 * 1000);
    const twoDaysAgoTs = admin.firestore.Timestamp.fromMillis(twoDaysAgo);
    
    try {
      // Query pending applications older than 48 hours
      const applicationsSnapshot = await admin.firestore()
        .collection('applications')
        .where('status', '==', 'applied')
        .where('createdAt', '<', twoDaysAgoTs)
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
      
      console.log(`[PENDING_APPLICATIONS] Found ${employerApplications.size} employers with pending applications`);
      
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
          console.log(`[PENDING_APPLICATIONS] Skipping employer ${employerId} - not in EMPLOYER mode`);
          continue;
        }
        
        // Check if we can send notification
        if (await canSendNotification(employerId, 'pending_applications', 12 * 60 * 60 * 1000)) {
          const locale = await getUserLanguage(admin.firestore(), employerId);
          const recipient = await getUserDisplayName(admin.firestore(), employerId);
          const tParams = { count, recipient };
          const sent = await sendFCMNotification(employerId, {
            title: tTitle('EMPLOYER_PENDING_APPLICATIONS', locale, tParams),
            body: tBody('EMPLOYER_PENDING_APPLICATIONS', locale, tParams),
            data: {
              type: 'PENDING_APPLICATIONS',
              count: count.toString(),
              deepLink: 'dutype://applications',
              locale
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
      
      console.log(`[PENDING_APPLICATIONS] Sent ${sentCount} pending application reminders`);

      // Run expired notifications cleanup in the same scheduled cycle.
      const deleted = await cleanupExpiredNotificationsBatch();
      if (deleted > 0) {
        console.log(`🧹 Cleanup: deleted ${deleted} expired notifications`);
      }

      console.log('[PENDING_APPLICATIONS] ========== PENDING APPLICATIONS CHECK COMPLETE ==========');
      
      return null;
    } catch (error) {
      console.error('[PENDING_APPLICATIONS] Error checking pending applications:', error);
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
      const oneDayAgoTs = admin.firestore.Timestamp.fromMillis(oneDayAgo);
      
      const applicationsSnapshot = await admin.firestore()
        .collection('applications')
        .where('status', '==', 'applied')
        .where('createdAt', '<', oneDayAgoTs)
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
        const createdAt = app.createdAt?.toMillis ? app.createdAt.toMillis() : 0;
        
        // Check if user's ACTIVE role is WORKER
        const userDoc = await admin.firestore().collection('users').doc(workerId).get();
        if (!userDoc.exists) continue;
        
        const userData = userDoc.data();
        const activeRole = userData?.activeRole || 'WORKER';
        
        // Only send to users whose ACTIVE role is WORKER
        if (activeRole !== 'WORKER') {
          console.log(`â° Skipping ${workerId} - active role is ${activeRole}, not WORKER`);
          continue;
        }
        
        // Get job details
        const jobDoc = await admin.firestore().collection('jobmetadata').doc(jobId).get();
        if (!jobDoc.exists) continue;
        
        const jobData = jobDoc.data();
        const jobTitle = jobData?.title || 'Job';
        const employerPhone = jobData?.employerPhone || '';
        
        // Calculate days pending
        const daysPending = Math.floor((Date.now() - createdAt) / (24 * 60 * 60 * 1000));
        
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
        const locale = await getUserLanguage(admin.firestore(), workerId);
        const recipient = await getUserDisplayName(admin.firestore(), workerId);
        const tParams = { jobTitle, daysPending, recipient };
        const sent = await sendFCMNotification(workerId, {
          title: tTitle('WORKER_PENDING_APPLICATION', locale, tParams),
          body: tBody('WORKER_PENDING_APPLICATION', locale, tParams),
          data: {
            type: 'WORKER_PENDING_APPLICATION',
            jobId: jobId,
            applicationId: applicationId,
            employerId: employerId,
            daysPending: daysPending.toString(),
            deepLink: `dutype://job/${jobId}`,  // Opens job description screen
            action: 'view_job',
            locale
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
    console.log('[WORKER_RE_ENGAGEMENT] ========== WORKER RE-ENGAGEMENT START ==========');
    console.log('[WORKER_RE_ENGAGEMENT] Role: WORKER (active role only)');
    
    // Skip during quiet hours
    if (isQuietHours()) {
      console.log('[WORKER_RE_ENGAGEMENT] Quiet hours - skipping worker re-engagement');
      return null;
    }
    
    const threeDaysAgo = Date.now() - (3 * 24 * 60 * 60 * 1000);
    const threeDaysAgoTs = admin.firestore.Timestamp.fromMillis(threeDaysAgo);
    
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
          .collection('applications')
          .where('workerId', '==', userId)
          .orderBy('createdAt', 'desc')
          .limit(1)
          .get();
        
        const shouldReEngage = lastAppSnapshot.empty || 
          (lastAppSnapshot.docs[0].data().createdAt < threeDaysAgoTs);
        
        if (shouldReEngage) {
          const locale = await getUserLanguage(admin.firestore(), userId);
          const recipient = await getUserDisplayName(admin.firestore(), userId);
          const sent = await sendFCMNotification(userId, {
            title: tTitle('WORKER_RE_ENGAGEMENT', locale, { recipient }),
            body: tBody('WORKER_RE_ENGAGEMENT', locale, { recipient }),
            data: {
              type: 'RE_ENGAGEMENT',
              deepLink: 'dutype://jobs',
              locale
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
      
      console.log(`[WORKER_RE_ENGAGEMENT] Re-engaged ${reEngagedCount} inactive workers`);
      console.log('[WORKER_RE_ENGAGEMENT] ========== WORKER RE-ENGAGEMENT COMPLETE ==========');
      
      return null;
    } catch (error) {
      console.error('[WORKER_RE_ENGAGEMENT] Error re-engaging workers:', error);
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
    console.log('[EMPLOYER_RE_ENGAGEMENT] ========== EMPLOYER RE-ENGAGEMENT START ==========');
    console.log('[EMPLOYER_RE_ENGAGEMENT] Role: EMPLOYER (active role only)');
    
    // Skip during quiet hours
    if (isQuietHours()) {
      console.log('[EMPLOYER_RE_ENGAGEMENT] Quiet hours - skipping employer re-engagement');
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
        
        // Check last job post (employerId now lives in job_details).
        const lastJobSnapshot = await admin.firestore()
          .collection('job_details')
          .where('employerId', '==', userId)
          .orderBy('createdAt', 'desc')
          .limit(1)
          .get();

        const shouldReEngage = lastJobSnapshot.empty ||
          (lastJobSnapshot.docs[0].data().createdAt < fifteenDaysAgo);
        
        if (shouldReEngage) {
          const locale = await getUserLanguage(admin.firestore(), userId);
          const recipient = await getUserDisplayName(admin.firestore(), userId);
          const sent = await sendFCMNotification(userId, {
            title: tTitle('EMPLOYER_RE_ENGAGEMENT', locale, { recipient }),
            body: tBody('EMPLOYER_RE_ENGAGEMENT', locale, { recipient }),
            data: {
              type: 'RE_ENGAGEMENT',
              deepLink: 'dutype://post-job',
              locale
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
      
      console.log(`[EMPLOYER_RE_ENGAGEMENT] Re-engaged ${reEngagedCount} inactive employers`);
      console.log('[EMPLOYER_RE_ENGAGEMENT] ========== EMPLOYER RE-ENGAGEMENT COMPLETE ==========');
      
      return null;
    } catch (error) {
      console.error('[EMPLOYER_RE_ENGAGEMENT] Error re-engaging employers:', error);
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
    
    console.log(`[APPLICATION_STATUS] Application status changed: ${before.status} -> ${newStatus} for worker ${workerId}`);
    
    // Determine notification template based on status; localised per-recipient.
    let templateId = '';
    let priority: 'high' | 'normal' = 'high';

    switch (newStatus) {
      case 'ACCEPTED':
      case 'hired':
        templateId = 'APPLICATION_HIRED';
        break;
      case 'REJECTED':
      case 'rejected':
        templateId = 'APPLICATION_REJECTED';
        priority = 'normal';
        break;
      case 'WITHDRAWN':
      case 'withdrawn':
        templateId = 'APPLICATION_WITHDRAWN';
        priority = 'normal';
        break;
      case 'SHORTLISTED':
      case 'shortlisted':
        templateId = 'APPLICATION_SHORTLISTED';
        break;
      default:
        return null; // Don't notify for other status changes
    }

    const locale = await getUserLanguage(admin.firestore(), workerId);
    const tParams = { jobTitle };
    const title = tTitle(templateId, locale, tParams);
    const body = tBody(templateId, locale, tParams);
    
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
 *
 * Templates and the rotation pool now live in `notification-i18n.ts`
 * (SE_GUEST_POOL) so each language gets its own topic + copy.
 */
async function sendGuestEngagementTopicMessage(): Promise<void> {
  if (isQuietHours()) {
    console.log('👥 Guest engagement: quiet hours — skipping');
    return;
  }

  const hour = new Date().getHours();
  const tod: TimeOfDay = hour < 12 ? 'morning' : hour < 17 ? 'afternoon' : 'evening';
  const pool = SE_GUEST_POOL.filter((m) => m.timeOfDay === tod);
  if (pool.length === 0) {
    console.log('👥 Guest engagement: no templates for time-of-day', tod);
    return;
  }

  const now = new Date();
  const startOfYear = new Date(now.getFullYear(), 0, 0);
  const dayOfYear = Math.floor((now.getTime() - startOfYear.getTime()) / 86400000);
  const picked = pool[dayOfYear % pool.length];

  // Fan out to per-language topics so devices that already chose Telugu get
  // the Telugu copy and devices on English get the English copy. Android
  // subscribes to `guest_users_${lang}` in addition to the legacy plain topic.
  for (const lang of SUPPORTED_LOCALES) {
    const title = tTitle(picked.id, lang);
    const body = tBody(picked.id, lang);
    const topic = localizedTopic('guest_users', lang);

    try {
      await admin.messaging().send({
        topic,
        notification: { title, body },
        data: {
          type: 'GUEST_ENGAGEMENT',
          deepLink: 'dutype://login',
          channel: 'medium_priority',
          locale: lang,
        },
        android: {
          priority: 'normal',
          notification: {
            channelId: 'medium_priority',
            clickAction: 'FLUTTER_NOTIFICATION_CLICK',
          },
        },
      });
      console.log(`✅ Guest engagement (${lang}) sent to ${topic}: "${title}"`);
    } catch (error) {
      console.error(`❌ Failed to send guest engagement to ${topic}:`, error);
    }
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
    
    console.log(`[NEW_APPLICATION] New application from ${workerName} for job: ${jobTitle}`);
    
    try {
      const sent = await sendFCMNotification(employerId, {
        title: 'New Application Received!',
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

