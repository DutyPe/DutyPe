"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.whatsappWebhook = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
/**
 * WhatsApp Webhook for receiving real-time delivery status updates and failure reasons from Meta Cloud API.
 * Deployed to us-central1 to match the webhook URL registered on phone 1285842094620024:
 * https://us-central1-dutype-860ac.cloudfunctions.net/whatsappWebhook
 */
exports.whatsappWebhook = functions
    .region("us-central1")
    .https.onRequest(async (req, res) => {
    var _a, _b;
    // 1. Meta Webhook Verification Challenge (GET) or View Logs
    if (req.method === "GET") {
        if (req.query["view_logs"] === "true") {
            const snap = await admin.firestore().collection("whatsapp_delivery_logs").orderBy("receivedAt", "desc").limit(10).get();
            const logs = snap.docs.map(d => (Object.assign({ id: d.id }, d.data())));
            res.status(200).json(logs);
            return;
        }
        const mode = req.query["hub.mode"];
        const token = req.query["hub.verify_token"];
        const challenge = req.query["hub.challenge"];
        functions.logger.info("WhatsApp Webhook GET verification:", { mode, token, challenge });
        if (mode === "subscribe") {
            res.status(200).send(challenge);
            return;
        }
        res.status(200).send(challenge || "OK");
        return;
    }
    // 2. Incoming Status Updates & Messages (POST)
    if (req.method === "POST") {
        try {
            const body = req.body;
            functions.logger.info("WhatsApp Webhook POST Event:", JSON.stringify(body));
            const entry = (_a = body === null || body === void 0 ? void 0 : body.entry) === null || _a === void 0 ? void 0 : _a[0];
            const change = (_b = entry === null || entry === void 0 ? void 0 : entry.changes) === null || _b === void 0 ? void 0 : _b[0];
            const value = change === null || change === void 0 ? void 0 : change.value;
            const statuses = value === null || value === void 0 ? void 0 : value.statuses;
            if (Array.isArray(statuses)) {
                for (const s of statuses) {
                    functions.logger.warn(`WhatsApp Message Status: [${s.status}] for ${s.recipient_id}, id: ${s.id}`, {
                        errors: s.errors || null,
                        pricing: s.pricing || null,
                        conversation: s.conversation || null,
                    });
                    await admin.firestore().collection("whatsapp_delivery_logs").doc(s.id || String(Date.now())).set({
                        recipient: s.recipient_id || null,
                        status: s.status,
                        timestamp: s.timestamp || null,
                        errors: s.errors || null,
                        pricing: s.pricing || null,
                        conversation: s.conversation || null,
                        raw: s,
                        receivedAt: admin.firestore.FieldValue.serverTimestamp(),
                    }, { merge: true });
                }
            }
        }
        catch (err) {
            functions.logger.error("Error handling WhatsApp webhook POST:", err);
        }
        res.status(200).send("EVENT_RECEIVED");
        return;
    }
    res.status(405).send("Method Not Allowed");
});
//# sourceMappingURL=whatsapp-webhook.js.map