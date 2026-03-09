#!/bin/bash

# Deploy SEO pages and security fixes to Firebase

echo "🚀 Deploying SEO pages and security fixes..."

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null
then
    echo "❌ Firebase CLI not found. Install it with: npm install -g firebase-tools"
    exit 1
fi

# Deploy hosting only
echo "📦 Deploying to Firebase Hosting..."
firebase deploy --only hosting

if [ $? -eq 0 ]; then
    echo "✅ Deployment successful!"
    echo ""
    echo "📋 Next Steps:"
    echo "1. Go to Google Search Console: https://search.google.com/search-console"
    echo "2. Submit sitemap: https://dutypein.web.app/sitemap.xml"
    echo "3. Request indexing for new SEO pages"
    echo "4. Check Security Issues section"
    echo "5. Request review if harmful content warning exists"
    echo ""
    echo "🔗 New SEO Pages:"
    echo "   - https://dutypein.web.app/jobs-near-me.html"
    echo "   - https://dutypein.web.app/driver-jobs.html"
    echo "   - https://dutypein.web.app/maid-jobs.html"
    echo "   - https://dutypein.web.app/delivery-jobs.html"
    echo "   - https://dutypein.web.app/driver-jobs-hyderabad.html"
else
    echo "❌ Deployment failed!"
    exit 1
fi
