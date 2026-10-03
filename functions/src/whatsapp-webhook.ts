import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

/**
 * WhatsApp Webhook for receiving real-time delivery status updates and failure reasons from Meta Cloud API.
 * Deployed to us-central1 to match the webhook URL registered on phone 1285842094620024:
 * https://us-central1-dutype-860ac.cloudfunctions.net/whatsappWebhook
 */
export const whatsappWebhook = functions
  .region("us-central1")
  .https.onRequest(async (req, res) => {
    // 1. Meta Webhook Verification Challenge (GET) or View Logs
    if (req.method === "GET") {
      if (req.query["view_logs"] === "true") {
        const snap = await admin.firestore().collection("whatsapp_delivery_logs").orderBy("receivedAt", "desc").limit(10).get();
        const logs = snap.docs.map(d => ({ id: d.id, ...d.data() }));
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

        const entry = body?.entry?.[0];
        const change = entry?.changes?.[0];
        const value = change?.value;
        const statuses = value?.statuses;

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
      } catch (err) {
        functions.logger.error("Error handling WhatsApp webhook POST:", err);
      }
      res.status(200).send("EVENT_RECEIVED");
      return;
    }

    res.status(405).send("Method Not Allowed");
  });
