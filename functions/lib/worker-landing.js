"use strict";
/**
 * Worker Profile Landing Page - Firebase Cloud Function
 *
 * Generates dynamic landing pages for worker profiles with:
 * - Open Graph meta tags for rich previews
 * - Android Intent URLs for reliable app opening
 * - Fallback to Play Store if app not installed
 *
 * Industry Standard: LinkedIn, Indeed, Naukri pattern
 *
 * URL Pattern: https://dutype-860ac.web.app/worker/{workerId}
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.workerLanding = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
// Initialize Firestore (only if not already initialized)
if (!admin.apps.length) {
    admin.initializeApp();
}
const db = admin.firestore();
/**
 * Worker Profile Landing Page Handler
 *
 * Generates SEO-friendly landing page with Open Graph tags
 * Uses Android Intent URLs for reliable app opening (LinkedIn/Instagram pattern)
 */
exports.workerLanding = functions.https.onRequest(async (req, res) => {
    try {
        // Extract worker ID from path: /worker/USER_123
        const pathParts = req.path.split("/").filter((p) => p);
        const workerId = pathParts[pathParts.length - 1];
        if (!workerId) {
            res.status(400).send("Worker ID is required");
            return;
        }
        // Fetch worker data from the canonical profile collection.
        const workerDoc = await db.collection("worker_profiles").doc(workerId).get();
        if (!workerDoc.exists) {
            res.status(404).send("Worker not found");
            return;
        }
        const workerData = workerDoc.data();
        if (!workerData) {
            res.status(404).send("Worker data not found");
            return;
        }
        // Extract worker details
        const workerName = workerData.fullName || "Professional Worker";
        const workerPhone = workerData.phone || "";
        const workerSkills = workerData.skills || "";
        const workerExperience = workerData.experience || "";
        const profileImageUrl = workerData.profileImageUrl || "";
        const isVerified = workerData.isVerified || false;
        const completedJobs = workerData.completedJobsCount || 0;
        const rating = workerData.averageRating || 0;
        // Format skills for display
        const skillsList = Array.isArray(workerSkills)
            ? workerSkills.map((s) => String(s).trim()).filter(Boolean).slice(0, 3)
            : String(workerSkills || "").split(",").map((s) => s.trim()).filter(Boolean).slice(0, 3);
        const primarySkill = skillsList[0] || "Professional Worker";
        const skillsText = skillsList.join(", ");
        // Create description
        const description = `${workerName} - ${primarySkill}${isVerified ? " ✓ Verified" : ""}${completedJobs > 0 ? ` | ${completedJobs} jobs completed` : ""}${rating > 0 ? ` | ${rating.toFixed(1)}⭐` : ""}`;
        // Android Intent URL (Industry Standard - LinkedIn, Instagram, Uber pattern)
        // This is the MOST RELIABLE way to open Android apps
        const intentUrl = `intent://worker/${workerId}#Intent;scheme=dutype;package=com.dutype.app;S.browser_fallback_url=https://play.google.com/store/apps/details?id=com.dutype.app;end`;
        // Generate HTML with Open Graph tags and auto-redirect
        const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="facebook-domain-verification" content="pm8w20h5ejx9kch4n7w6ijcbh3gfi8" />
    
    <!-- Primary Meta Tags -->
    <title>${workerName} - ${primarySkill} | DutyPe</title>
    <meta name="title" content="${workerName} - ${primarySkill} | DutyPe">
    <meta name="description" content="${description}">
    
    <!-- Open Graph / Facebook -->
    <meta property="og:type" content="profile">
    <meta property="og:url" content="https://dutype-860ac.web.app/worker/${workerId}">
    <meta property="og:title" content="${workerName} - ${primarySkill}">
    <meta property="og:description" content="${description}">
    ${profileImageUrl ? `<meta property="og:image" content="${profileImageUrl}">` : ""}
    
    <!-- Twitter Card -->
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:url" content="https://dutype-860ac.web.app/worker/${workerId}">
    <meta name="twitter:title" content="${workerName} - ${primarySkill}">
    <meta name="twitter:description" content="${description}">
    ${profileImageUrl ? `<meta name="twitter:image" content="${profileImageUrl}">` : ""}
    
    <!-- Android App Links -->
    <meta name="mobile-web-app-capable" content="yes">
    <meta name="theme-color" content="#1F2937">
    
    <!-- Auto-redirect to app using Android Intent URL -->
    <script>
        // Detect if user is on mobile
        const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
        
        if (isMobile) {
            // Use Android Intent URL for reliable app opening
            window.location.href = "${intentUrl}";
        }
    </script>
    
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
        
        .container {
            background: white;
            border-radius: 20px;
            padding: 40px;
            max-width: 500px;
            width: 100%;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            text-align: center;
        }
        
        .profile-image {
            width: 120px;
            height: 120px;
            border-radius: 50%;
            object-fit: cover;
            border: 4px solid #667eea;
            margin-bottom: 20px;
        }
        
        .placeholder-image {
            width: 120px;
            height: 120px;
            border-radius: 50%;
            background: #f3f4f6;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 20px;
            font-size: 48px;
            color: #9ca3af;
        }
        
        h1 {
            color: #1f2937;
            font-size: 28px;
            margin-bottom: 8px;
        }
        
        .title {
            color: #6b7280;
            font-size: 18px;
            margin-bottom: 16px;
        }
        
        .verified {
            display: inline-block;
            background: #10b981;
            color: white;
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 14px;
            font-weight: 600;
            margin-bottom: 20px;
        }
        
        .skills {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            justify-content: center;
            margin-bottom: 24px;
        }
        
        .skill {
            background: #f3f4f6;
            color: #1f2937;
            padding: 8px 16px;
            border-radius: 8px;
            font-size: 14px;
        }
        
        .stats {
            display: flex;
            justify-content: center;
            gap: 24px;
            margin-bottom: 32px;
            padding: 20px;
            background: #f9fafb;
            border-radius: 12px;
        }
        
        .stat {
            text-align: center;
        }
        
        .stat-value {
            font-size: 24px;
            font-weight: bold;
            color: #1f2937;
        }
        
        .stat-label {
            font-size: 12px;
            color: #6b7280;
            margin-top: 4px;
        }
        
        .cta-button {
            display: inline-block;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 16px 32px;
            border-radius: 12px;
            text-decoration: none;
            font-weight: 600;
            font-size: 16px;
            transition: transform 0.2s;
        }
        
        .cta-button:hover {
            transform: translateY(-2px);
        }
        
        .footer {
            margin-top: 24px;
            color: #9ca3af;
            font-size: 14px;
        }
        
        .loading {
            color: #667eea;
            font-size: 16px;
            margin-top: 20px;
        }
    </style>
</head>
<body>
    <div class="container">
        ${profileImageUrl
            ? `<img src="${profileImageUrl}" alt="${workerName}" class="profile-image">`
            : '<div class="placeholder-image">👤</div>'}
        
        <h1>${workerName}</h1>
        <div class="title">${primarySkill}</div>
        
        ${isVerified ? '<div class="verified">✓ DutyPe Verified</div>' : ""}
        
        ${skillsList.length > 0
            ? `
        <div class="skills">
            ${skillsList.map((skill) => `<div class="skill">${skill}</div>`).join("")}
        </div>
        `
            : ""}
        
        ${completedJobs > 0 || rating > 0
            ? `
        <div class="stats">
            ${completedJobs > 0
                ? `
            <div class="stat">
                <div class="stat-value">${completedJobs}</div>
                <div class="stat-label">Jobs Completed</div>
            </div>
            `
                : ""}
            ${rating > 0
                ? `
            <div class="stat">
                <div class="stat-value">${rating.toFixed(1)} ⭐</div>
                <div class="stat-label">Rating</div>
            </div>
            `
                : ""}
        </div>
        `
            : ""}
        
        <a href="${intentUrl}" class="cta-button">
            📲 View Profile on DutyPe
        </a>
        
        <div class="loading">Opening DutyPe app...</div>
        
        <div class="footer">
            Powered by DutyPe - India's #1 Local Jobs Platform
        </div>
    </div>
</body>
</html>
    `;
        // Set cache headers for performance
        res.set("Cache-Control", "public, max-age=300, s-maxage=600");
        res.status(200).send(html);
    }
    catch (error) {
        console.error("Error generating worker landing page:", error);
        res.status(500).send("Internal server error");
    }
});
//# sourceMappingURL=worker-landing.js.map