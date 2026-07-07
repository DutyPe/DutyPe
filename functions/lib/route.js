"use strict";
var _a;
Object.defineProperty(exports, "__esModule", { value: true });
exports.GET = void 0;
const server_1 = require("next/server");
const admin = require("firebase-admin");
// Initialize Firebase Admin SDK (ensure this is done only once)
if (!admin.apps.length) {
    try {
        admin.initializeApp({
            credential: admin.credential.cert({
                projectId: process.env.FIREBASE_PROJECT_ID,
                clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
                // The private key needs to be properly formatted.
                privateKey: (_a = process.env.FIREBASE_PRIVATE_KEY) === null || _a === void 0 ? void 0 : _a.replace(/\\n/g, '\n'),
            }),
        });
    }
    catch (error) {
        console.error('Firebase admin initialization error', error);
    }
}
const db = admin.firestore();
async function GET() {
    try {
        const metricsDoc = await db.collection('system_metrics').doc('global').get();
        if (!metricsDoc.exists) {
            return server_1.NextResponse.json({ totalCalls: 0 });
        }
        const data = metricsDoc.data();
        const totalCalls = (data === null || data === void 0 ? void 0 : data.totalCallButtonTaps) || 0;
        // Return the data with caching headers for performance
        return server_1.NextResponse.json({ totalCalls }, {
            headers: { 'Cache-Control': 'public, s-maxage=300, stale-while-revalidate=600' }
        });
    }
    catch (error) {
        console.error('Error fetching system metrics:', error);
        return new server_1.NextResponse('Internal Server Error', { status: 500 });
    }
}
exports.GET = GET;
//# sourceMappingURL=route.js.map