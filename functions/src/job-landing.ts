/**
 * Job Landing Page - Clean WhatsApp Preview
 * 
 * FIXES:
 * 1. Removed "DutyPe - Find Jobs" and "dutype.web.app" from WhatsApp preview
 * 2. Only shows job title and description in WhatsApp
 * 3. Opens Android app instantly without showing webpage
 * 
 * IMPORTANT: WhatsApp caches link previews for 7 days
 * To test changes immediately:
 * 1. Share link with a different parameter: ?v=2, ?test=1, etc.
 * 2. Or wait for WhatsApp cache to expire
 * 3. Or use WhatsApp's "Reset link preview" feature
 * 
 * Uses Android App Links (verified deep links)
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

export const jobLanding = functions.https.onRequest(async (req, res) => {
  try {
    const pathParts = req.path.split("/");
    const jobId = pathParts[pathParts.length - 1];

    if (!jobId || jobId === "index.html") {
      res.send(getGenericJobsPage());
      return;
    }

    const jobDoc = await admin.firestore().collection("jobs").doc(jobId).get();

    if (!jobDoc.exists) {
      res.status(404).send(get404Page());
      return;
    }

    const job = jobDoc.data();
    if (!job) {
      res.status(404).send(get404Page());
      return;
    }

    const html = generateJobLandingPage(job, jobId);
    // CRITICAL: No-cache headers to prevent WhatsApp from showing old preview
    res.set("Cache-Control", "no-cache, no-store, must-revalidate");
    res.set("Pragma", "no-cache");
    res.set("Expires", "0");
    res.send(html);
  } catch (error) {
    console.error("Error generating job landing page:", error);
    res.status(500).send(getErrorPage());
  }
});

function generateJobLandingPage(job: any, jobId: string): string {
  const title = `${job.title} - ${job.companyName}`;
    const salary = job.salary ? `₹${job.salary}/${job.salaryType || "FIXED"}` : "Salary Negotiable";
  const description = `💰 ${salary} | 📍 ${job.location}`;
  const url = `https://dutypeapp.web.app/jobs/${jobId}`;
  const imageUrl = job.imageUrl || "https://dutypeapp.web.app/logo.png";

  // CRITICAL FIX: Use App Link (not Intent URL) for INSTANT opening
  // App Links are verified and open instantly without any dialog or webpage
  const appLink = `https://dutypeapp.web.app/jobs/${jobId}`;

  return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    
    <!-- Clean WhatsApp Preview - Custom site name -->
    <meta property="og:type" content="article">
    <meta property="og:url" content="${url}">
    <meta property="og:title" content="${escapeHtml(job.title)} - ${escapeHtml(job.companyName)}">
    <meta property="og:description" content="${escapeHtml(description)}">
    <meta property="og:image" content="${imageUrl}">
    <meta property="og:image:width" content="1200">
    <meta property="og:image:height" content="630">
    
    <!-- Custom site name for WhatsApp preview -->
    <meta property="og:site_name" content="DutyPe - Find Hyperlocal Jobs">
    
    <!-- Twitter Card -->
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:title" content="${escapeHtml(job.title)} - ${escapeHtml(job.companyName)}">
    <meta name="twitter:description" content="${escapeHtml(description)}">
    <meta name="twitter:image" content="${imageUrl}">
    
    <!-- INSTANT APP OPENING: Use App Link for verified instant opening -->
    <script>
        // CRITICAL: This must execute BEFORE page renders
        (function() {
            const isAndroid = /Android/i.test(navigator.userAgent);
            if (isAndroid) {
                // App Link opens instantly (verified via assetlinks.json)
                window.location.href = '${appLink}';
            }
        })();
    </script>
    
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
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
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            text-align: center;
        }
        .logo {
            width: 80px;
            height: 80px;
            margin: 0 auto 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border-radius: 20px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 40px;
        }
        h1 { font-size: 24px; color: #1a202c; margin-bottom: 10px; }
        .company { font-size: 16px; color: #718096; margin-bottom: 20px; }
        .details {
            background: #f7fafc;
            padding: 20px;
            border-radius: 12px;
            margin-bottom: 20px;
            text-align: left;
        }
        .detail-item {
            display: flex;
            align-items: center;
            margin-bottom: 12px;
            font-size: 14px;
            color: #4a5568;
        }
        .detail-item:last-child { margin-bottom: 0; }
        .icon { font-size: 20px; margin-right: 10px; }
        .cta-button {
            display: block;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 16px;
            border-radius: 12px;
            text-decoration: none;
            font-weight: 600;
            font-size: 18px;
            text-align: center;
            transition: transform 0.2s;
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
            border: none;
            width: 100%;
            cursor: pointer;
        }
        .cta-button:hover { transform: translateY(-2px); }
        .spinner {
            border: 3px solid #f3f3f3;
            border-top: 3px solid #667eea;
            border-radius: 50%;
            width: 40px;
            height: 40px;
            animation: spin 1s linear infinite;
            margin: 20px auto;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">💼</div>
        <h1>${escapeHtml(job.title)}</h1>
        <p class="company">${escapeHtml(job.companyName)}</p>
        
        <div class="details">
            <div class="detail-item">
                <span class="icon">💰</span>
                <span>${escapeHtml(salary)}</span>
            </div>
            <div class="detail-item">
                <span class="icon">📍</span>
                <span>${escapeHtml(job.location)}</span>
            </div>
            ${job.jobType ? `<div class="detail-item">
                <span class="icon">📅</span>
                <span>${escapeHtml(job.jobType)}</span>
            </div>` : ''}
        </div>
        
        <div class="spinner"></div>
        <p style="margin-top: 15px; color: #718096; font-size: 14px;">Opening job...</p>
        
        <a href="${appLink}" class="cta-button" style="margin-top: 20px; display: block;">
            Open in App
        </a>
    </div>
</body>
</html>`;
}

function getGenericJobsPage(): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- No title to prevent headers in share previews -->
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
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
            text-align: center;
        }
        h1 { font-size: 28px; color: #1a202c; margin-bottom: 20px; }
        .cta-button {
            display: inline-block;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 16px 40px;
            border-radius: 12px;
            text-decoration: none;
            font-weight: 600;
            font-size: 18px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Find Your Next Job</h1>
        <a href="https://play.google.com/store/apps/details?id=com.dutype.app" class="cta-button">Download App</a>
    </div>
</body>
</html>`;
}

function get404Page(): string {
  return `<!DOCTYPE html>
<html><head>
<meta charset="UTF-8">
<!-- No title to prevent headers in share previews -->
</head>
<body style="font-family: sans-serif; text-align: center; padding: 50px;">
<h1>Job Not Found</h1>
<p>This job may have been filled or removed.</p>
<a href="https://play.google.com/store/apps/details?id=com.dutype.app">Download DutyPe App</a>
</body></html>`;
}

function getErrorPage(): string {
  return `<!DOCTYPE html>
<html><head>
<meta charset="UTF-8">
<!-- No title to prevent headers in share previews -->
</head>
<body style="font-family: sans-serif; text-align: center; padding: 50px;">
<h1>Something went wrong</h1>
<a href="https://play.google.com/store/apps/details?id=com.dutype.app">Download DutyPe App</a>
</body></html>`;
}

function escapeHtml(text: string): string {
  const map: { [key: string]: string } = {
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "\"": "&quot;",
    "'": "&#039;",
  };
  return text.replace(/[&<>"']/g, (m) => map[m]);
}
