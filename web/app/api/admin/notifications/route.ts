import { NextRequest, NextResponse } from "next/server";
import { Timestamp } from "firebase-admin/firestore";

import { cachedAdminGet } from "@/lib/firebase/admin-response-cache";
import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb, getFirebaseAdminMessaging } from "@/lib/firebase/admin-server";
import { Notifications, UserTokens } from "@/lib/firebase/schema";

export const runtime = "nodejs";

/**
 * Admin notifications, built to cost the same at any user count:
 *   • Audience broadcasts (ALL / WORKER / EMPLOYER) are FCM topic messages. The route writes one
 *     broadcast_notifications doc; the `sendBroadcastNotification` Cloud Function sends it to the
 *     per-language topics. That doc is also the campaign log shown here.
 *   • Messages to specific users (≤ 50 uids) write each user's inbox doc and push to their token.
 */
type Preset = "APP_UPDATE" | "MAINTENANCE" | "EMERGENCY" | "GENERAL";
type TargetRole = "ALL" | "WORKER" | "EMPLOYER";

const BROADCASTS = "broadcast_notifications";
const PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=com.dutype.app";
const INBOX_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const MAX_DIRECT_RECIPIENTS = 50;

function topicFor(preset: Preset, role: TargetRole): string {
  if (preset === "APP_UPDATE") return "app_updates";
  if (role === "WORKER") return "workers";
  if (role === "EMPLOYER") return "employers";
  return "all_users";
}

async function getUncached() {
  try {
    const snap = await getFirebaseAdminDb().collection(BROADCASTS).orderBy("createdAt", "desc").limit(100).get();
    const campaigns = snap.docs.map((d) => {
      const data = d.data();
      const created = data.createdAt;
      return {
        id: d.id,
        title: data.title ?? "",
        message: data.message ?? "",
        preset: data.preset ?? "GENERAL",
        type: data.type ?? "broadcast",
        targetRole: data.targetRole ?? "ALL",
        sendPush: true,
        recipientCount: data.recipientCount ?? null,
        status: data.status ?? "queued",
        createdAt: created instanceof Timestamp ? created.toDate().toISOString() : null
      };
    });
    return NextResponse.json({ campaigns });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to load campaigns." }, { status: 500 });
  }
}

export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;
  let body: Record<string, unknown>;
  try { body = await request.json(); } catch { return NextResponse.json({ error: "Invalid request body." }, { status: 400 }); }

  const preset = (["APP_UPDATE", "MAINTENANCE", "EMERGENCY"].includes(String(body.preset)) ? body.preset : "GENERAL") as Preset;
  const role = (["WORKER", "EMPLOYER"].includes(String(body.targetRole)) ? body.targetRole : "ALL") as TargetRole;
  const title = String(body.title ?? "").trim().slice(0, 120);
  const message = String(body.message ?? "").trim().slice(0, 500);
  const deepLink = preset === "APP_UPDATE" ? PLAY_STORE_WEB_URL : String(body.deepLink ?? "").trim().slice(0, 300);
  const translations = body.translations && typeof body.translations === "object" ? body.translations : null;
  if (!title || !message) return NextResponse.json({ error: "Title and message are required." }, { status: 400 });

  const recipientIds = Array.isArray(body.recipientIds)
    ? Array.from(new Set(body.recipientIds.map((id) => String(id).trim()).filter(Boolean)))
    : [];
  if (recipientIds.length > MAX_DIRECT_RECIPIENTS) {
    return NextResponse.json({ error: `Send to at most ${MAX_DIRECT_RECIPIENTS} specific users; use an audience broadcast for more.` }, { status: 400 });
  }

  const db = getFirebaseAdminDb();
  try {
    if (recipientIds.length === 0) {
      const ref = await db.collection(BROADCASTS).add({
        title, message, preset, targetRole: role, topic: topicFor(preset, role),
        type: preset === "APP_UPDATE" ? "APP_UPDATE" : "GENERAL",
        ...(deepLink ? { deepLink } : {}),
        ...(translations ? { translations } : {}),
        createdAt: Timestamp.now()
      });
      return NextResponse.json({ ok: true, campaignId: ref.id, pushTopic: topicFor(preset, role) });
    }

    // Specific users: inbox doc + direct push each.
    const now = Date.now();
    const tokens = await db.getAll(...recipientIds.map((uid) => db.collection(UserTokens.COLLECTION).doc(uid)));
    const batch = db.batch();
    recipientIds.forEach((uid) => batch.set(db.collection(Notifications.COLLECTION).doc(), {
      [Notifications.RECIPIENT_ID]: uid,
      [Notifications.TITLE]: title,
      [Notifications.BODY]: message,
      [Notifications.TYPE]: "GENERAL",
      [Notifications.DATA]: deepLink ? { deepLink } : {},
      [Notifications.READ]: false,
      [Notifications.CREATED_AT]: Timestamp.fromMillis(now),
      [Notifications.EXPIRE_AT]: Timestamp.fromMillis(now + INBOX_TTL_MS)
    }));
    batch.set(db.collection(BROADCASTS).doc(), {
      title, message, preset, targetRole: "USERS", recipientCount: recipientIds.length, status: "sent", createdAt: Timestamp.fromMillis(now)
    });
    await batch.commit();
    const fcmTokens = tokens.map((t) => String(t.get(UserTokens.FCM_TOKEN) ?? "")).filter(Boolean);
    if (fcmTokens.length) {
      await getFirebaseAdminMessaging().sendEachForMulticast({
        tokens: fcmTokens,
        data: { title, body: message, type: "GENERAL", channel: "high_priority", ...(deepLink ? { deepLink } : {}) },
        android: { priority: "high" }
      });
    }
    return NextResponse.json({ ok: true, recipientCount: recipientIds.length, pushed: fcmTokens.length });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to send notification." }, { status: 500 });
  }
}

export const GET = cachedAdminGet(getUncached);
