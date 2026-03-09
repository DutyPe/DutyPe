"use strict";
/**
 * Scheduled Notifications - Enterprise Grade
 *
 * Server-side notification logic using Firebase Cloud Functions
 * Follows Swiggy/Zomato/LinkedIn architecture pattern
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.notifyNewApplication = exports.notifyApplicationStatusUpdate = exports.reEngageInactiveEmployers = exports.reEngageInactiveWorkers = exports.checkPendingApplications = exports.checkExpiringJobs = exports.checkBirthdays = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
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
/**
 * Send FCM notification to user
 */
async function sendFCMNotification(userId, payload) {
    var _a;
    try {
        // Get user's FCM token
        const tokenDoc = await admin.firestore()
            .collection('fcm_tokens')
            .doc(userId)
            .get();
        if (!tokenDoc.exists) {
            console.log(`No FCM token found for user ${userId}`);
            return false;
        }
        const token = (_a = tokenDoc.data()) === null || _a === void 0 ? void 0 : _a.token;
        if (!token) {
            console.log(`Empty FCM token for user ${userId}`);
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
        console.log(`✅ FCM notification sent to user ${userId}`);
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
/**
 * Check if it's quiet hours (10 PM - 8 AM)
 */
function isQuietHours() {
    const now = new Date();
    const hour = now.getHours();
    return hour >= 22 || hour < 8;
}
/**
 * Check if user's ACTIVE role matches the target role
 * ACTIVE ROLE NOTIFICATIONS:
 * - User only receives notifications for their currently active role
 * - If user is in WORKER mode → only worker notifications
 * - If user is in EMPLOYER mode → only employer notifications
 * - Birthday notifications sent to everyone regardless of active role
 */
function userActiveRoleMatches(user, targetRole) {
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
    console.log('🎂 ========== BIRTHDAY CHECK START ==========');
    console.log('🎂 Role: ALL USERS (Workers + Employers + Dual-Role)');
    // Skip during quiet hours
    if (isQuietHours()) {
        console.log('🎂 Quiet hours - skipping birthday check');
        return null;
    }
    const today = new Date();
    const todayDay = today.getDate();
    const todayMonth = today.getMonth() + 1;
    console.log(`🎂 Today's date: ${todayDay}/${todayMonth}/${today.getFullYear()}`);
    try {
        // Query all users (admin privileges - no permission errors!)
        const usersSnapshot = await admin.firestore()
            .collection('users')
            .limit(500)
            .get();
        console.log(`🎂 Checking ${usersSnapshot.size} users for birthdays`);
        let birthdayWishesSent = 0;
        let birthdaysFound = 0;
        for (const doc of usersSnapshot.docs) {
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
                console.log(`🎂 🎉 BIRTHDAY FOUND: ${fullName} (userId: ${userId})`);
                // Check if we can send notification
                if (await canSendNotification(userId, 'birthday', 24 * 60 * 60 * 1000)) {
                    const userName = fullName.split(' ')[0] || fullName;
                    const sent = await sendFCMNotification(userId, {
                        title: `🎂 Happy Birthday, ${userName}! 🎉`,
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
        console.log('🎂 ========== BIRTHDAY CHECK COMPLETE ==========');
        console.log(`🎂 Found ${birthdaysFound} birthdays, Sent ${birthdayWishesSent} notifications`);
        return null;
    }
    catch (error) {
        console.error('🎂 Error checking birthdays:', error);
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
    console.log('⏰ ========== EXPIRING JOBS CHECK START ==========');
    console.log('⏰ Role: EMPLOYER (active role only)');
    // Skip during quiet hours
    if (isQuietHours()) {
        console.log('⏰ Quiet hours - skipping expiring jobs check');
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
        console.log(`⏰ Found ${jobsSnapshot.size} jobs expiring in 24 hours`);
        let sentCount = 0;
        for (const doc of jobsSnapshot.docs) {
            const job = doc.data();
            const jobId = doc.id;
            const employerId = job.employerId;
            if (!employerId)
                continue;
            // Get employer user document to check active role
            const employerDoc = await admin.firestore()
                .collection('users')
                .doc(employerId)
                .get();
            if (!employerDoc.exists)
                continue;
            const employer = employerDoc.data();
            // Only send if user's ACTIVE role is EMPLOYER
            if (!userActiveRoleMatches(employer, 'EMPLOYER')) {
                console.log(`⏰ Skipping job ${jobId} - employer ${employerId} not in EMPLOYER mode`);
                continue;
            }
            // Check if we can send notification
            if (await canSendNotification(employerId, 'job_expiry', 24 * 60 * 60 * 1000)) {
                const hoursLeft = Math.floor((job.expiresAt - now) / (1000 * 60 * 60));
                const sent = await sendFCMNotification(employerId, {
                    title: '⏰ Job Expiring Soon',
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
        console.log(`⏰ Sent ${sentCount} job expiry notifications`);
        console.log('⏰ ========== EXPIRING JOBS CHECK COMPLETE ==========');
        return null;
    }
    catch (error) {
        console.error('⏰ Error checking expiring jobs:', error);
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
    console.log('📋 ========== PENDING APPLICATIONS CHECK START ==========');
    console.log('📋 Role: EMPLOYER (active role only)');
    // Skip during quiet hours
    if (isQuietHours()) {
        console.log('📋 Quiet hours - skipping pending applications check');
        return null;
    }
    const twoDaysAgo = Date.now() - (48 * 60 * 60 * 1000);
    try {
        // Query pending applications older than 48 hours
        const applicationsSnapshot = await admin.firestore()
            .collection('applications')
            .where('status', '==', 'PENDING')
            .where('appliedAt', '<', twoDaysAgo)
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
        console.log(`📋 Found ${employerApplications.size} employers with pending applications`);
        let sentCount = 0;
        for (const [employerId, count] of employerApplications.entries()) {
            // Get employer user document to check active role
            const employerDoc = await admin.firestore()
                .collection('users')
                .doc(employerId)
                .get();
            if (!employerDoc.exists)
                continue;
            const employer = employerDoc.data();
            // Only send if user's ACTIVE role is EMPLOYER
            if (!userActiveRoleMatches(employer, 'EMPLOYER')) {
                console.log(`📋 Skipping employer ${employerId} - not in EMPLOYER mode`);
                continue;
            }
            // Check if we can send notification
            if (await canSendNotification(employerId, 'pending_applications', 12 * 60 * 60 * 1000)) {
                const sent = await sendFCMNotification(employerId, {
                    title: '📋 Pending Applications',
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
        console.log(`📋 Sent ${sentCount} pending application reminders`);
        console.log('📋 ========== PENDING APPLICATIONS CHECK COMPLETE ==========');
        return null;
    }
    catch (error) {
        console.error('📋 Error checking pending applications:', error);
        return null;
    }
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
    console.log('💼 ========== WORKER RE-ENGAGEMENT START ==========');
    console.log('💼 Role: WORKER (active role only)');
    // Skip during quiet hours
    if (isQuietHours()) {
        console.log('💼 Quiet hours - skipping worker re-engagement');
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
                .collection('applications')
                .where('workerId', '==', userId)
                .orderBy('appliedAt', 'desc')
                .limit(1)
                .get();
            const shouldReEngage = lastAppSnapshot.empty ||
                (lastAppSnapshot.docs[0].data().appliedAt < threeDaysAgo);
            if (shouldReEngage) {
                const sent = await sendFCMNotification(userId, {
                    title: '💼 New Jobs Waiting For You!',
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
        console.log(`💼 Re-engaged ${reEngagedCount} inactive workers`);
        console.log('💼 ========== WORKER RE-ENGAGEMENT COMPLETE ==========');
        return null;
    }
    catch (error) {
        console.error('💼 Error re-engaging workers:', error);
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
    console.log('🏢 ========== EMPLOYER RE-ENGAGEMENT START ==========');
    console.log('🏢 Role: EMPLOYER (active role only)');
    // Skip during quiet hours
    if (isQuietHours()) {
        console.log('🏢 Quiet hours - skipping employer re-engagement');
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
                    title: '🏢 Ready to Hire?',
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
        console.log(`🏢 Re-engaged ${reEngagedCount} inactive employers`);
        console.log('🏢 ========== EMPLOYER RE-ENGAGEMENT COMPLETE ==========');
        return null;
    }
    catch (error) {
        console.error('🏢 Error re-engaging employers:', error);
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
    console.log(`📬 Application status changed: ${before.status} → ${newStatus} for worker ${workerId}`);
    // Determine notification message based on status
    let title = '';
    let body = '';
    let priority = 'high';
    switch (newStatus) {
        case 'ACCEPTED':
            title = '🎉 Application Accepted!';
            body = `Great news! Your application for "${jobTitle}" has been accepted. The employer will contact you soon.`;
            break;
        case 'REJECTED':
            title = '📋 Application Update';
            body = `Your application for "${jobTitle}" was not selected this time. Keep applying!`;
            priority = 'normal';
            break;
        case 'SHORTLISTED':
            title = '⭐ You\'re Shortlisted!';
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
            console.log(`✅ Application status notification sent to worker ${workerId}`);
        }
        return null;
    }
    catch (error) {
        console.error('Error sending application status notification:', error);
        return null;
    }
});
/**
 * Notify employer about new job applications
 * Triggered when worker applies to a job
 * ROLE: EMPLOYER ONLY (employers receive application notifications)
 */
exports.notifyNewApplication = functions.firestore
    .document('applications/{applicationId}')
    .onCreate(async (snapshot, context) => {
    const application = snapshot.data();
    const employerId = application.employerId;
    const workerName = application.workerName || 'A worker';
    const jobTitle = application.jobTitle || 'your job';
    console.log(`📬 New application from ${workerName} for job: ${jobTitle}`);
    try {
        const sent = await sendFCMNotification(employerId, {
            title: '📬 New Application Received!',
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
            console.log(`✅ New application notification sent to employer ${employerId}`);
        }
        return null;
    }
    catch (error) {
        console.error('Error sending new application notification:', error);
        return null;
    }
});
//# sourceMappingURL=scheduled-notifications.js.map