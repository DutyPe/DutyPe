"use strict";
/**
 * Scheduled Notifications - Enterprise Grade
 *
 * Server-side notification logic using Firebase Cloud Functions
 * Follows Swiggy/Zomato/LinkedIn architecture pattern
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.cleanupExpiredNotifications = exports.notifyNewApplication = exports.guestEngagementEvening = exports.guestEngagementAfternoon = exports.guestEngagementMorning = exports.notifyApplicationStatusUpdate = exports.reEngageInactiveEmployers = exports.reEngageInactiveWorkers = exports.smartEngagementEvening = exports.smartEngagementAfternoon = exports.smartEngagementMorning = exports.remindWorkersPendingApplications = exports.checkPendingApplications = exports.checkExpiringJobs = exports.checkBirthdays = exports.workerNearbyJobsMorning = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const notification_i18n_1 = require("./notification-i18n");
// ============================================
// HELPER FUNCTIONS
// ============================================
/**
 * Check if user can receive notification (deduplication + rate limiting)
 */
async function canSendNotification(userId, notificationType, minInterval) {
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
    }
    catch (error) {
        console.error('Error checking notification eligibility:', error);
        return false;
    }
}
/**
 * Track notification sent
 */
async function trackNotificationSent(userId, notificationType) {
    try {
        await admin.firestore()
            .collection('notification_tracking')
            .doc(userId)
            .collection('sent')
            .add({
            type: notificationType,
            sentAt: Date.now()
        });
    }
    catch (error) {
        console.error('Error tracking notification:', error);
    }
}
async function trackNotificationSentWithKey(userId, notificationType, key) {
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
    }
    catch (error) {
        console.error('Error tracking notification with key:', error);
    }
}
/**
 * Delete expired notification inbox documents in batches.
 */
async function cleanupExpiredNotificationsBatch() {
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
async function sendFCMNotification(userId, payload) {
    var _a;
    try {
        // Get user's FCM token from the canonical token collection.
        const tokenDoc = await admin.firestore()
            .collection('user_tokens')
            .doc(userId)
            .get();
        if (!tokenDoc.exists) {
            console.log(`No token document found for ${userId}`);
            return false;
        }
        const token = (_a = tokenDoc.data()) === null || _a === void 0 ? void 0 : _a.fcmToken;
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
        console.log(`FCM notification sent to user ${userId}`);
        return true;
    }
    catch (error) {
        console.error(`Error sending FCM notification to ${userId}:`, error);
        return false;
    }
}
/**
 * Parse date of birth to day and month
 */
function parseDateOfBirth(dateOfBirth) {
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
    }
    catch (error) {
        console.warn(`Could not parse date of birth: ${dateOfBirth}`);
        return null;
    }
}
/** Current hour (0-23) in India Standard Time, independent of the server time zone. */
function istHour() {
    const hourStr = new Intl.DateTimeFormat('en-GB', {
        timeZone: 'Asia/Kolkata',
        hour: '2-digit',
        hour12: false,
    }).format(new Date());
    return parseInt(hourStr, 10) % 24;
}
/**
 * Check if it's quiet hours in IST (10 PM - 8 AM).
 *
 * NOTE: Cloud Functions run in UTC, so `new Date().getHours()` returns UTC hours.
 * We must convert to Asia/Kolkata before comparing, otherwise the quiet window is
 * shifted by 5.5 hours and night notifications (e.g. ~2 AM IST) slip through.
 */
function isQuietHours() {
    const hour = istHour();
    return hour >= 22 || hour < 8;
}
/**
 * Check if user's ACTIVE role matches the target role
 * ACTIVE ROLE NOTIFICATIONS:
 * - User only receives notifications for their currently active role
 * - If user is in WORKER mode -> only worker notifications
 * - If user is in EMPLOYER mode -> only employer notifications
 * - Birthday notifications sent to everyone regardless of active role
 */
function profileRoleMatches(profile, targetRole) {
    const role = String((profile === null || profile === void 0 ? void 0 : profile.role) || targetRole).toUpperCase();
    return role === targetRole;
}
function simpleHash(input) {
    let hash = 0;
    for (let i = 0; i < input.length; i++) {
        hash = ((hash << 5) - hash) + input.charCodeAt(i);
        hash |= 0;
    }
    return Math.abs(hash);
}
function currentTimeOfDay() {
    const hour = istHour();
    if (hour < 12)
        return 'morning';
    if (hour < 17)
        return 'afternoon';
    return 'evening';
}
function latLngFrom(value) {
    var _a, _b;
    if (!value || typeof value !== 'object')
        return null;
    const lat = Number((_a = value.lat) !== null && _a !== void 0 ? _a : value.latitude);
    const lng = Number((_b = value.lng) !== null && _b !== void 0 ? _b : value.longitude);
    if (!Number.isFinite(lat) || !Number.isFinite(lng))
        return null;
    if (lat < -90 || lat > 90 || lng < -180 || lng > 180 || (lat === 0 && lng === 0))
        return null;
    return { lat, lng };
}
function distanceKm(a, b) {
    const toRad = (deg) => (deg * Math.PI) / 180;
    const earthKm = 6371;
    const dLat = toRad(b.lat - a.lat);
    const dLng = toRad(b.lng - a.lng);
    const lat1 = toRad(a.lat);
    const lat2 = toRad(b.lat);
    const h = Math.sin(dLat / 2) ** 2 +
        Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
    return 2 * earthKm * Math.asin(Math.sqrt(h));
}
async function getWorkerLocation(userId, user) {
    const direct = latLngFrom(user.location) || latLngFrom(user.currentLocation) || latLngFrom(user.lastKnownLocation);
    if (direct) {
        return Object.assign(Object.assign({}, direct), { label: String(user.city || user.locationText || user.addressText || 'your area') });
    }
    const profile = await admin.firestore().collection('worker_profiles').doc(userId).get().catch(() => null);
    const data = profile && profile.exists ? profile.data() || {} : {};
    const profileLoc = latLngFrom(data.location) || latLngFrom(data.currentLocation) || latLngFrom(data.lastKnownLocation);
    if (!profileLoc)
        return null;
    return Object.assign(Object.assign({}, profileLoc), { label: String(data.city || data.locationText || data.addressText || 'your area') });
}
// Smart-engagement template pools (titles + bodies + deep links per language)
// live in `notification-i18n.ts` as SE_WORKER_POOL / SE_EMPLOYER_POOL.
async function sendRoleSpecificSmartEngagement(slot) {
    if (isQuietHours()) {
        console.log('🧠 Smart engagement: quiet hours - skipping');
        return;
    }
    const [workerSnapshot, employerSnapshot] = await Promise.all([
        admin.firestore().collection('worker_profiles').limit(250).get(),
        admin.firestore().collection('employer_profiles').limit(250).get(),
    ]);
    const profileDocs = [
        ...workerSnapshot.docs.map((doc) => ({ doc, role: 'WORKER' })),
        ...employerSnapshot.docs.map((doc) => ({ doc, role: 'EMPLOYER' })),
    ];
    let sentCount = 0;
    const dayOfMonth = new Date().getDate();
    for (const { doc, role } of profileDocs) {
        const userId = doc.id;
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
        const fullPool = role === 'WORKER' ? notification_i18n_1.SE_WORKER_POOL : notification_i18n_1.SE_EMPLOYER_POOL;
        // Filter by time-of-day so e.g. "Good morning" never fires at 8 PM.
        const tod = currentTimeOfDay();
        const pool = fullPool.filter((m) => !m.timeOfDay || m.timeOfDay === tod);
        const messageIndex = (simpleHash(userId) + dayOfMonth + slot) % pool.length;
        const picked = pool[messageIndex];
        const locale = await (0, notification_i18n_1.getUserLanguage)(admin.firestore(), userId);
        const title = (0, notification_i18n_1.tTitle)(picked.id, locale);
        const body = (0, notification_i18n_1.tBody)(picked.id, locale);
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
exports.workerNearbyJobsMorning = functions.pubsub
    .schedule('30 10 * * *')
    .timeZone('Asia/Kolkata')
    .onRun(async () => {
    if (isQuietHours()) {
        console.log('📍 Nearby jobs: quiet hours - skipping');
        return null;
    }
    const [workersSnapshot, jobsSnapshot] = await Promise.all([
        admin.firestore().collection('worker_profiles').limit(200).get(),
        admin.firestore().collection('jobmetadata').where('status', '==', 'open').orderBy('createdAt', 'desc').limit(250).get(),
    ]);
    const jobs = jobsSnapshot.docs.map((doc) => {
        const data = doc.data();
        const loc = latLngFrom(data.location);
        return loc ? Object.assign(Object.assign({ id: doc.id }, loc), { title: String(data.title || 'job'), city: String(data.companyCity || data.addressText || '') }) : null;
    }).filter(Boolean);
    let sentCount = 0;
    for (const doc of workersSnapshot.docs) {
        const userId = doc.id;
        const user = doc.data();
        const workerLocation = await getWorkerLocation(userId, user);
        if (!workerLocation)
            continue;
        const nearby = jobs
            .map((job) => (Object.assign(Object.assign({}, job), { km: distanceKm(workerLocation, job) })))
            .filter((job) => job.km <= 25)
            .sort((a, b) => a.km - b.km);
        if (nearby.length === 0)
            continue;
        const notificationType = 'worker_nearby_jobs';
        const dedupeKey = `${notificationType}_${new Date().toISOString().slice(0, 10)}`;
        const alreadySentToday = await admin.firestore()
            .collection('notification_tracking')
            .doc(userId)
            .collection('sent')
            .where('key', '==', dedupeKey)
            .limit(1)
            .get();
        if (!alreadySentToday.empty)
            continue;
        if (!await canSendNotification(userId, notificationType, 20 * 60 * 60 * 1000))
            continue;
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
exports.checkBirthdays = functions.pubsub
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
        const [workersSnapshot, employersSnapshot] = await Promise.all([
            admin.firestore().collection('worker_profiles').limit(500).get(),
            admin.firestore().collection('employer_profiles').limit(500).get(),
        ]);
        const profileDocs = [...workersSnapshot.docs, ...employersSnapshot.docs];
        console.log(`[BIRTHDAY] Checking ${profileDocs.length} profiles for birthdays`);
        let birthdayWishesSent = 0;
        let birthdaysFound = 0;
        for (const doc of profileDocs) {
            const user = doc.data();
            const userId = doc.id;
            const dateOfBirth = user.dateOfBirth;
            const fullName = user.fullName || 'Friend';
            if (!dateOfBirth)
                continue;
            const parsed = parseDateOfBirth(dateOfBirth);
            if (!parsed)
                continue;
            // Check if today is their birthday
            if (parsed.day === todayDay && parsed.month === todayMonth) {
                birthdaysFound++;
                console.log(`[BIRTHDAY] Birthday found: ${fullName} (userId: ${userId})`);
                // Check if we can send notification
                if (await canSendNotification(userId, 'birthday', 24 * 60 * 60 * 1000)) {
                    const userName = fullName.split(' ')[0] || fullName;
                    const locale = await (0, notification_i18n_1.getUserLanguage)(admin.firestore(), userId);
                    const sent = await sendFCMNotification(userId, {
                        title: (0, notification_i18n_1.tTitle)('BIRTHDAY', locale, { name: userName }),
                        body: (0, notification_i18n_1.tBody)('BIRTHDAY', locale, { name: userName }),
                        data: {
                            type: 'BIRTHDAY',
                            userName: userName,
                            action: 'birthday_wish',
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
    }
    catch (error) {
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
exports.checkExpiringJobs = functions.pubsub
    .schedule('0 * * * *') // Every hour at minute 0
    .timeZone('Asia/Kolkata')
    .onRun(async (context) => {
    console.log('[EXPIRING_JOBS] ========== CHECK START ==========');
    console.log('[EXPIRING_JOBS] Role: EMPLOYER (active role only)');
    // Skip during quiet hours
    if (isQuietHours()) {
        console.log('[EXPIRING_JOBS] Quiet hours - skipping check');
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
        console.log(`[EXPIRING_JOBS] Found ${detailsSnapshot.size} job_details expiring in 24 hours`);
        let sentCount = 0;
        for (const detailsDoc of detailsSnapshot.docs) {
            const details = detailsDoc.data();
            const jobId = detailsDoc.id;
            const employerId = details.employerId;
            const expiresAt = details.expiresAt;
            if (!employerId)
                continue;
            // Fetch jobmetadata to verify status == 'open' and get title.
            const metaDoc = await admin.firestore().collection('jobmetadata').doc(jobId).get();
            if (!metaDoc.exists)
                continue;
            const job = metaDoc.data() || {};
            if (job.status !== 'open')
                continue;
            // Get employer profile to verify the role still exists.
            const employerDoc = await admin.firestore()
                .collection('employer_profiles')
                .doc(employerId)
                .get();
            if (!employerDoc.exists)
                continue;
            const employer = employerDoc.data();
            if (!profileRoleMatches(employer, 'EMPLOYER')) {
                console.log(`[EXPIRING_JOBS] Skipping job ${jobId} - employer ${employerId} profile role mismatch`);
                continue;
            }
            // Check if we can send notification
            if (await canSendNotification(employerId, 'job_expiry', 24 * 60 * 60 * 1000)) {
                const hoursLeft = Math.floor((expiresAt - now) / (1000 * 60 * 60));
                const locale = await (0, notification_i18n_1.getUserLanguage)(admin.firestore(), employerId);
                const recipient = await (0, notification_i18n_1.getUserDisplayName)(admin.firestore(), employerId);
                const tParams = { jobTitle: job.title, hoursLeft, recipient };
                const sent = await sendFCMNotification(employerId, {
                    title: (0, notification_i18n_1.tTitle)('JOB_EXPIRY_SOON', locale, tParams),
                    body: (0, notification_i18n_1.tBody)('JOB_EXPIRY_SOON', locale, tParams),
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
        console.log(`[EXPIRING_JOBS] Sent ${sentCount} job expiry notifications`);
        console.log('[EXPIRING_JOBS] ========== CHECK COMPLETE ==========');
        return null;
    }
    catch (error) {
        console.error('[EXPIRING_JOBS] Error checking expiring jobs:', error);
        return null;
    }
});
/**
 * Check pending applications and remind employers
 * Runs every 6 hours
 * ROLE: EMPLOYER ONLY (only employers review applications)
 * ACTIVE ROLE: Only sends to users whose ACTIVE role is EMPLOYER
 */
exports.checkPendingApplications = functions.pubsub
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
        const employerApplications = new Map();
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
            // Get employer profile to verify the role still exists.
            const employerDoc = await admin.firestore()
                .collection('employer_profiles')
                .doc(employerId)
                .get();
            if (!employerDoc.exists)
                continue;
            const employer = employerDoc.data();
            if (!profileRoleMatches(employer, 'EMPLOYER')) {
                console.log(`[PENDING_APPLICATIONS] Skipping employer ${employerId} - profile role mismatch`);
                continue;
            }
            // Check if we can send notification
            if (await canSendNotification(employerId, 'pending_applications', 12 * 60 * 60 * 1000)) {
                const locale = await (0, notification_i18n_1.getUserLanguage)(admin.firestore(), employerId);
                const recipient = await (0, notification_i18n_1.getUserDisplayName)(admin.firestore(), employerId);
                const tParams = { count, recipient };
                const sent = await sendFCMNotification(employerId, {
                    title: (0, notification_i18n_1.tTitle)('EMPLOYER_PENDING_APPLICATIONS', locale, tParams),
                    body: (0, notification_i18n_1.tBody)('EMPLOYER_PENDING_APPLICATIONS', locale, tParams),
                    data: {
                        type: 'PENDING_APPLICATIONS',
                        count: count.toString(),
                        deepLink: 'dutype://employer/applications',
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
    }
    catch (error) {
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
exports.remindWorkersPendingApplications = functions.pubsub
    .schedule('every 6 hours')
    .timeZone('Asia/Kolkata')
    .onRun(async (context) => {
    var _a;
    console.log('[WORKER_PENDING_APPLICATIONS] ========== REMINDERS START ==========');
    console.log('[WORKER_PENDING_APPLICATIONS] Role: WORKER (active role only)');
    console.log('[WORKER_PENDING_APPLICATIONS] Smart Logic: One notification per job, no quiet hours');
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
        console.log(`[WORKER_PENDING_APPLICATIONS] Found ${applicationsSnapshot.size} pending applications older than 24 hours`);
        let sentCount = 0;
        // Process each application individually (one notification per job)
        for (const doc of applicationsSnapshot.docs) {
            const app = doc.data();
            const workerId = app.workerId;
            const jobId = app.jobId;
            const applicationId = doc.id;
            const employerId = app.employerId;
            const createdAt = ((_a = app.createdAt) === null || _a === void 0 ? void 0 : _a.toMillis) ? app.createdAt.toMillis() : 0;
            const workerDoc = await admin.firestore().collection('worker_profiles').doc(workerId).get();
            if (!workerDoc.exists)
                continue;
            const workerData = workerDoc.data();
            if (!profileRoleMatches(workerData, 'WORKER')) {
                console.log(`[WORKER_PENDING_APPLICATIONS] Skipping ${workerId} - profile role mismatch`);
                continue;
            }
            // Get job details
            const jobDoc = await admin.firestore().collection('jobmetadata').doc(jobId).get();
            if (!jobDoc.exists)
                continue;
            const jobData = jobDoc.data();
            const jobTitle = (jobData === null || jobData === void 0 ? void 0 : jobData.title) || 'Job';
            const employerPhone = (jobData === null || jobData === void 0 ? void 0 : jobData.employerPhone) || '';
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
                    console.log(`[WORKER_PENDING_APPLICATIONS] Skipping ${applicationId} for ${workerId} - last sent ${hoursSinceLast.toFixed(1)}h ago`);
                    continue;
                }
            }
            // Send notification
            const locale = await (0, notification_i18n_1.getUserLanguage)(admin.firestore(), workerId);
            const recipient = await (0, notification_i18n_1.getUserDisplayName)(admin.firestore(), workerId);
            const tParams = { jobTitle, daysPending, recipient };
            const sent = await sendFCMNotification(workerId, {
                title: (0, notification_i18n_1.tTitle)('WORKER_PENDING_APPLICATION', locale, tParams),
                body: (0, notification_i18n_1.tBody)('WORKER_PENDING_APPLICATION', locale, tParams),
                data: {
                    type: 'WORKER_PENDING_APPLICATION',
                    jobId: jobId,
                    applicationId: applicationId,
                    employerId: employerId,
                    daysPending: daysPending.toString(),
                    deepLink: `dutype://job/${jobId}`,
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
                console.log(`[WORKER_PENDING_APPLICATIONS] Sent notification to worker ${workerId} for job ${jobId} (${jobTitle})`);
            }
        }
        console.log(`[WORKER_PENDING_APPLICATIONS] Sent ${sentCount} worker pending application reminders`);
        console.log('[WORKER_PENDING_APPLICATIONS] ========== REMINDERS COMPLETE ==========');
        return null;
    }
    catch (error) {
        console.error('[WORKER_PENDING_APPLICATIONS] Error sending worker pending application reminders:', error);
        return null;
    }
});
/**
 * Smart role-specific engagement notifications
 * 3 slots/day to keep active users engaged with relevant actions.
 */
exports.smartEngagementMorning = functions.pubsub
    .schedule('0 9 * * *')
    .timeZone('Asia/Kolkata')
    .onRun(async () => {
    console.log('🧠 ===== SMART ENGAGEMENT MORNING =====');
    await sendRoleSpecificSmartEngagement(0);
    return null;
});
exports.smartEngagementAfternoon = functions.pubsub
    .schedule('0 15 * * *')
    .timeZone('Asia/Kolkata')
    .onRun(async () => {
    console.log('🧠 ===== SMART ENGAGEMENT AFTERNOON =====');
    await sendRoleSpecificSmartEngagement(1);
    return null;
});
exports.smartEngagementEvening = functions.pubsub
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
exports.reEngageInactiveWorkers = functions.pubsub
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
        const workersSnapshot = await admin.firestore()
            .collection('worker_profiles')
            .limit(200)
            .get();
        let reEngagedCount = 0;
        for (const doc of workersSnapshot.docs) {
            const user = doc.data();
            const userId = doc.id;
            if (!profileRoleMatches(user, 'WORKER')) {
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
                const locale = await (0, notification_i18n_1.getUserLanguage)(admin.firestore(), userId);
                const recipient = await (0, notification_i18n_1.getUserDisplayName)(admin.firestore(), userId);
                const sent = await sendFCMNotification(userId, {
                    title: (0, notification_i18n_1.tTitle)('WORKER_RE_ENGAGEMENT', locale, { recipient }),
                    body: (0, notification_i18n_1.tBody)('WORKER_RE_ENGAGEMENT', locale, { recipient }),
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
    }
    catch (error) {
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
exports.reEngageInactiveEmployers = functions.pubsub
    .schedule('every 12 hours')
    .timeZone('Asia/Kolkata')
    .onRun(async (context) => {
    var _a, _b, _c, _d, _e, _f, _g;
    console.log('[EMPLOYER_RE_ENGAGEMENT] ========== EMPLOYER RE-ENGAGEMENT START ==========');
    console.log('[EMPLOYER_RE_ENGAGEMENT] Role: EMPLOYER (active role only)');
    // Skip during quiet hours
    if (isQuietHours()) {
        console.log('[EMPLOYER_RE_ENGAGEMENT] Quiet hours - skipping employer re-engagement');
        return null;
    }
    const fifteenDaysAgo = Date.now() - (15 * 24 * 60 * 60 * 1000);
    try {
        const employersSnapshot = await admin.firestore()
            .collection('employer_profiles')
            .limit(200)
            .get();
        let reEngagedCount = 0;
        for (const doc of employersSnapshot.docs) {
            const user = doc.data();
            const userId = doc.id;
            if (!profileRoleMatches(user, 'EMPLOYER')) {
                continue;
            }
            // Check if we can send notification
            if (!await canSendNotification(userId, 're_engagement', 24 * 60 * 60 * 1000)) {
                continue;
            }
            // Check last job post from slim jobmetadata. It carries employerId +
            // createdAt and avoids treating detail-only documents as activity.
            const lastJobSnapshot = await admin.firestore()
                .collection('jobmetadata')
                .where('employerId', '==', userId)
                .orderBy('createdAt', 'desc')
                .limit(1)
                .get();
            const rawCreatedAt = (_a = lastJobSnapshot.docs[0]) === null || _a === void 0 ? void 0 : _a.data().createdAt;
            const lastCreatedAtMillis = typeof rawCreatedAt === 'number'
                ? rawCreatedAt
                : (_g = (_c = (_b = rawCreatedAt === null || rawCreatedAt === void 0 ? void 0 : rawCreatedAt.toMillis) === null || _b === void 0 ? void 0 : _b.call(rawCreatedAt)) !== null && _c !== void 0 ? _c : (_f = (_e = (_d = rawCreatedAt === null || rawCreatedAt === void 0 ? void 0 : rawCreatedAt.toDate) === null || _d === void 0 ? void 0 : _d.call(rawCreatedAt)) === null || _e === void 0 ? void 0 : _e.getTime) === null || _f === void 0 ? void 0 : _f.call(_e)) !== null && _g !== void 0 ? _g : 0;
            const shouldReEngage = lastJobSnapshot.empty ||
                lastCreatedAtMillis < fifteenDaysAgo;
            if (shouldReEngage) {
                const locale = await (0, notification_i18n_1.getUserLanguage)(admin.firestore(), userId);
                const recipient = await (0, notification_i18n_1.getUserDisplayName)(admin.firestore(), userId);
                const sent = await sendFCMNotification(userId, {
                    title: (0, notification_i18n_1.tTitle)('EMPLOYER_RE_ENGAGEMENT', locale, { recipient }),
                    body: (0, notification_i18n_1.tBody)('EMPLOYER_RE_ENGAGEMENT', locale, { recipient }),
                    data: {
                        type: 'RE_ENGAGEMENT',
                        deepLink: 'dutype://employer/post-job',
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
    }
    catch (error) {
        console.error('[EMPLOYER_RE_ENGAGEMENT] Error re-engaging employers:', error);
        return null;
    }
});
/**
 * Notify workers about application status updates
 * Triggered when employer updates application status
 * ROLE: WORKER ONLY (workers receive application updates)
 */
exports.notifyApplicationStatusUpdate = functions.firestore
    .document('applications/{applicationId}')
    .onUpdate(async (_change, context) => {
    console.log(`[APPLICATION_STATUS] Direct-FCM trigger skipped for ${context.params.applicationId}; notification-fanout owns application status notifications`);
    return null;
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
async function sendGuestEngagementTopicMessage() {
    if (isQuietHours()) {
        console.log('👥 Guest engagement: quiet hours — skipping');
        return;
    }
    const hour = istHour();
    const tod = hour < 12 ? 'morning' : hour < 17 ? 'afternoon' : 'evening';
    const pool = notification_i18n_1.SE_GUEST_POOL.filter((m) => m.timeOfDay === tod);
    if (pool.length === 0) {
        console.log('👥 Guest engagement: no templates for time-of-day', tod);
        return;
    }
    const now = new Date();
    const startOfYear = new Date(now.getFullYear(), 0, 0);
    const dayOfYear = Math.floor((now.getTime() - startOfYear.getTime()) / 86400000);
    const picked = pool[dayOfYear % pool.length];
    // Fan out to per-language topics so devices that already chose Telugu get
    // the Telugu copy and devices on English get the English copy.
    for (const lang of notification_i18n_1.SUPPORTED_LOCALES) {
        const title = (0, notification_i18n_1.tTitle)(picked.id, lang);
        const body = (0, notification_i18n_1.tBody)(picked.id, lang);
        const topic = (0, notification_i18n_1.localizedTopic)('guest_users', lang);
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
        }
        catch (error) {
            console.error(`❌ Failed to send guest engagement to ${topic}:`, error);
        }
    }
}
/**
 * Morning slot  08:00 IST  – "New jobs near you are waiting!"
 */
exports.guestEngagementMorning = functions.pubsub
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
exports.guestEngagementAfternoon = functions.pubsub
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
exports.guestEngagementEvening = functions.pubsub
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
exports.notifyNewApplication = functions.firestore
    .document('applications/{applicationId}')
    .onCreate(async (_snapshot, context) => {
    console.log(`[NEW_APPLICATION] Direct-FCM trigger skipped for ${context.params.applicationId}; notification-fanout owns new application notifications`);
    return null;
});
/**
 * Cleanup expired notifications from Firestore inbox.
 * Runs daily and deletes notifications where expiresAt <= now.
 */
exports.cleanupExpiredNotifications = functions.pubsub
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
    }
    catch (error) {
        console.error('🧹 Error cleaning expired notifications:', error);
        return null;
    }
});
//# sourceMappingURL=scheduled-notifications.js.map