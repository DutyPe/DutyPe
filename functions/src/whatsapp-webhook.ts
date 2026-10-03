import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { createHmac, timingSafeEqual } from "crypto";

/**
 * Optional settings (functions/.env.<project>):
 *   WHATSAPP_VERIFY_TOKEN  the "Verify token" typed in Meta's webhook setup; when set, only Meta can verify.
 *   WHATSAPP_APP_SECRET    the Meta app secret; when set, POSTs must carry a valid X-Hub-Signature-256.
 *   WHATSAPP_LOGS_KEY      a long random string; ?view_logs=true&key=... shows the last 10 statuses.
 *                          Without it the logs endpoint is off (it would expose customers' numbers).
 */
const mask = (phone: unknown) => {
  const p = String(phone || "");
  return p.length > 4 ? `${"*".repeat(p.length - 4)}${p.slice(-4)}` : p;
};

function sameText(a: string, b: string): boolean {
  const x = Buffer.from(a);
  const y = Buffer.from(b);
  return x.length === y.length && timingSafeEqual(x, y);
}

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
        const key = process.env.WHATSAPP_LOGS_KEY || "";
        if (!key || !sameText(String(req.query["key"] || ""), key)) {
          res.status(403).send("Forbidden");
          return;
        }
        const snap = await admin.firestore().collection("whatsapp_delivery_logs").orderBy("receivedAt", "desc").limit(10).get();
        const logs = snap.docs.map((d) => {
          const x = d.data();
          return { id: d.id, recipient: mask(x.recipient), status: x.status, timestamp: x.timestamp, errors: x.errors || null };
        });
        res.status(200).json(logs);
        return;
      }
      const mode = req.query["hub.mode"];
      const token = req.query["hub.verify_token"];
      const challenge = req.query["hub.challenge"];
      functions.logger.info("WhatsApp Webhook GET verification:", { mode });
      const expected = process.env.WHATSAPP_VERIFY_TOKEN || "";
      if (mode === "subscribe" && (!expected || sameText(String(token || ""), expected))) {
        res.status(200).send(challenge);
        return;
      }
      res.status(403).send("Forbidden");
      return;
    }

    // 2. Incoming Status Updates & Messages (POST)
    if (req.method === "POST") {
      // Only Meta can post statuses: check the signature when the app secret is configured.
      const appSecret = process.env.WHATSAPP_APP_SECRET || "";
      if (appSecret) {
        const sig = String(req.header("x-hub-signature-256") || "");
        const expectedSig = "sha256=" + createHmac("sha256", appSecret).update(req.rawBody || Buffer.from("")).digest("hex");
        if (!sameText(sig, expectedSig)) {
          res.status(401).send("Bad signature");
          return;
        }
      }
      try {
        const body = req.body;
        functions.logger.info("WhatsApp Webhook POST event", { statuses: body?.entry?.[0]?.changes?.[0]?.value?.statuses?.length ?? 0 });

        const entry = body?.entry?.[0];
        const change = entry?.changes?.[0];
        const value = change?.value;
        const statuses = value?.statuses;

        if (Array.isArray(statuses)) {
          for (const s of statuses) {
            functions.logger.warn(`WhatsApp Message Status: [${s.status}] for ${mask(s.recipient_id)}, id: ${s.id}`, {
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
