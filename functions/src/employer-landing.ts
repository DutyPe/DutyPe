/**
 * Employer Profile Landing Page - Firebase Cloud Function
 * 
 * Generates dynamic landing pages for employer profiles with:
 * - Open Graph meta tags for rich previews
 * - Android Intent URLs for reliable app opening
 * - Fallback to Play Store if app not installed
 * 
 * Industry Standard: LinkedIn, Indeed, Naukri pattern
 * 
 * URL Pattern: https://dutypeapp.web.app/employer/{employerId}
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Firestore is already initialized in worker-landing.ts
const db = admin.firestore();

/**
 * Employer Profile Landing Page Handler
 * 
 * Generates SEO-friendly landing page with Open Graph tags
 * Uses Android Intent URLs for reliable app opening (LinkedIn/Instagram pattern)
 */
export const employerLanding = functions.https.onRequest(async (req, res) => {
  try {
    // Extract employer ID from path: /employer/USER_123
    const pathParts = req.path.split("/").filter((p) => p);
    const employerId = pathParts[pathParts.length - 1];

    if (!employerId) {
      res.status(400).send("Employer ID is required");
      return;
    }

    // Fetch employer data from Firestore
    const employerDoc = await db.collection("users").doc(employerId).get();

    if (!employerDoc.exists) {
      res.status(404).send("Employer not found");
      return;
    }

    const employerData = employerDoc.data();
    if (!employerData) {
      res.status(404).send("Employer data not found");
      return;
    }

    // Extract employer details
    const companyName = employerData.companyName || employerData.fullName || "Company";
    const companyPhone = employerData.phone || employerData.phoneNumber || "";
    const trustTier = employerData.trustTier || "NEW";
    const profileImageUrl = employerData.profileImageUrl || "";
    const postedJobsCount = employerData.postedJobsCount || 0;
    const companyRating = employerData.companyRating || 0;

    // Trust badge text
    const trustBadgeText = trustTier === "GOLD" ? "🏆 Gold Verified" :
                          trustTier === "SILVER" ? "🥈 Silver Verified" :
                          trustTier === "BRONZE" ? "🥉 Bronze Verified" :
                          "✅ Verified";

    // Create description
    const description = `${companyName} - ${trustBadgeText} Employer${
      postedJobsCount > 0 ? ` | ${postedJobsCount} jobs posted` : ""
    }${companyRating > 0 ? ` | ${companyRating.toFixed(1)}⭐` : ""}`;

    // Android Intent URL (Industry Standard - LinkedIn, Instagram, Uber pattern)
    const intentUrl = `intent://employer/${employerId}#Intent;scheme=dutype;package=com.dutype.app;S.browser_fallback_url=https://play.google.com/store/apps/details?id=com.dutype.app;end`;

    // Generate HTML with Open Graph tags and auto-redirect
    const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    
    <!-- Primary Meta Tags -->
    <title>${companyName} - ${trustBadgeText} | DutyPe</title>
    <meta name="title" content="${companyName} - ${trustBadgeText} | DutyPe">
    <meta name="description" content="${description}">
    
    <!-- Open Graph / Facebook -->
    <meta property="og:type" content="business.business">
    <meta property="og:url" content="https://dutypeapp.web.app/employer/${employerId}">
    <meta property="og:title" content="${companyName} - ${trustBadgeText}">
    <meta property="og:description" content="${description}">
    ${profileImageUrl ? `<meta property="og:image" content="${profileImageUrl}">` : ""}
    
    <!-- Twitter Card -->
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:url" content="https://dutypeapp.web.app/employer/${employerId}">
    <meta name="twitter:title" content="${companyName} - ${trustBadgeText}">
    <meta name="twitter:description" content="${description}">
    ${profileImageUrl ? `<meta name="twitter:image" content="${profileImageUrl}">` : ""}
    
    <!-- Android App Links -->
    <meta name="mobile-web-app-capable" content="yes">
    <meta name="theme-color" content="#3B82F6">
    
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
            background: linear-gradient(135deg, #3B82F6 0%, #1E40AF 100%);
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
            border: 4px solid #3B82F6;
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
            background: linear-gradient(135deg, #3B82F6 0%, #1E40AF 100%);
            color: white;
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 14px;
            font-weight: 600;
            margin-bottom: 20px;
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
            background: linear-gradient(135deg, #3B82F6 0%, #1E40AF 100%);
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
            color: #3B82F6;
            font-size: 16px;
            margin-top: 20px;
        }
    </style>
</head>
<body>
    <div class="container">
        ${
          profileImageUrl
            ? `<img src="${profileImageUrl}" alt="${companyName}" class="profile-image">`
            : '<div class="placeholder-image">🏢</div>'
        }
        
        <h1>${companyName}</h1>
        <div class="title">Verified Employer</div>
        
        <div class="verified">${trustBadgeText}</div>
        
        ${
          postedJobsCount > 0 || companyRating > 0
            ? `
        <div class="stats">
            ${
              postedJobsCount > 0
                ? `
            <div class="stat">
                <div class="stat-value">${postedJobsCount}</div>
                <div class="stat-label">Jobs Posted</div>
            </div>
            `
                : ""
            }
            ${
              companyRating > 0
                ? `
            <div class="stat">
                <div class="stat-value">${companyRating.toFixed(1)} ⭐</div>
                <div class="stat-label">Rating</div>
            </div>
            `
                : ""
            }
        </div>
        `
            : ""
        }
        
        <a href="${intentUrl}" class="cta-button">
            📲 View Jobs on DutyPe
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
  } catch (error) {
    console.error("Error generating employer landing page:", error);
    res.status(500).send("Internal server error");
  }
});
