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
type TargetRole = "ALL" | "WORKER" | "EMPLOYER" | "GUEST";

const BROADCASTS = "broadcast_notifications";
const PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=com.dutype.app";
const INBOX_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const MAX_DIRECT_RECIPIENTS = 50;

function topicFor(preset: Preset, role: TargetRole): string {
  if (role === "GUEST") return "guest_users";
  if (role === "WORKER") return "workers";
  if (role === "EMPLOYER") return "employers";
  if (preset === "APP_UPDATE") return "app_updates";
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
  const role = (["WORKER", "EMPLOYER", "GUEST"].includes(String(body.targetRole)) ? body.targetRole : "ALL") as TargetRole;
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
      const pushTopic = topicFor(preset, role);
      const ref = await db.collection(BROADCASTS).add({
        title, message, preset, targetRole: role, topic: pushTopic,
        type: preset === "APP_UPDATE" ? "APP_UPDATE" : "GENERAL",
        ...(deepLink ? { deepLink } : {}),
        ...(translations ? { translations } : {}),
        createdAt: Timestamp.now()
      });

      // Dual delivery: Also deliver in-app inbox & direct push to targeted role users (up to 300)
      try {
        let userQuery = db.collection("users").limit(300);
        if (role === "EMPLOYER" || role === "WORKER") {
          userQuery = db.collection("users").where("role", "==", role).limit(300);
        }
        const userSnap = await userQuery.get();
        const uids = userSnap.docs.map((d) => d.id);

        if (uids.length > 0) {
          const now = Date.now();
          const batch = db.batch();
          uids.forEach((uid) => {
            batch.set(db.collection(Notifications.COLLECTION).doc(), {
              [Notifications.RECIPIENT_ID]: uid,
              [Notifications.TITLE]: title,
              [Notifications.BODY]: message,
              [Notifications.TYPE]: preset === "APP_UPDATE" ? "APP_UPDATE" : "GENERAL",
              [Notifications.DATA]: deepLink ? { deepLink } : {},
              [Notifications.READ]: false,
              [Notifications.CREATED_AT]: Timestamp.fromMillis(now),
              [Notifications.EXPIRE_AT]: Timestamp.fromMillis(now + INBOX_TTL_MS)
            });
          });
          await batch.commit();

          const tokenDocs = await db.getAll(...uids.map((uid) => db.collection(UserTokens.COLLECTION).doc(uid)));
          const userDocs = await db.getAll(...uids.map((uid) => db.collection("users").doc(uid)));
          const fcmTokens = Array.from(new Set(
            uids.map((_, idx) => {
              const t1 = String(tokenDocs[idx]?.get(UserTokens.FCM_TOKEN) ?? "").trim();
              const t2 = String(userDocs[idx]?.get("fcmToken") ?? "").trim();
              return t1 || t2;
            }).filter(Boolean)
          ));

          if (fcmTokens.length > 0) {
            for (let i = 0; i < fcmTokens.length; i += 500) {
              const chunk = fcmTokens.slice(i, i + 500);
              await getFirebaseAdminMessaging().sendEachForMulticast({
                tokens: chunk,
                notification: { title, body: message },
                data: {
                  title,
                  body: message,
                  message,
                  type: preset === "APP_UPDATE" ? "APP_UPDATE" : "GENERAL",
                  channel: "high_priority",
                  channel_id: "high_priority",
                  priority: "high",
                  ...(deepLink ? { deepLink } : {})
                },
                android: {
                  priority: "high",
                  notification: {
                    channelId: "high_priority",
                    title,
                    body: message,
                    icon: "ic_notification",
                    color: "#3B82F6",
                    sound: "default",
                    clickAction: "OPEN_ACTIVITY",
                    defaultSound: true,
                    defaultVibrateTimings: true,
                    visibility: "public"
                  }
                }
              });
            }
          }
        }
      } catch (dualErr) {
        console.error("Audience broadcast dual-delivery non-fatal error:", dualErr);
      }

      return NextResponse.json({ ok: true, campaignId: ref.id, pushTopic });
    }

    // Specific users: inbox doc + direct push each.
    const now = Date.now();
    const tokenDocs = await db.getAll(...recipientIds.map((uid) => db.collection(UserTokens.COLLECTION).doc(uid)));
    const userDocs = await db.getAll(...recipientIds.map((uid) => db.collection("users").doc(uid)));

    const notifType = preset === "APP_UPDATE" ? "APP_UPDATE" : "GENERAL";
    const batch = db.batch();
    recipientIds.forEach((uid) => batch.set(db.collection(Notifications.COLLECTION).doc(), {
      [Notifications.RECIPIENT_ID]: uid,
      [Notifications.TITLE]: title,
      [Notifications.BODY]: message,
      [Notifications.TYPE]: notifType,
      [Notifications.DATA]: deepLink ? { deepLink } : {},
      [Notifications.READ]: false,
      [Notifications.CREATED_AT]: Timestamp.fromMillis(now),
      [Notifications.EXPIRE_AT]: Timestamp.fromMillis(now + INBOX_TTL_MS)
    }));
    batch.set(db.collection(BROADCASTS).doc(), {
      title,
      message,
      preset,
      targetRole: "USERS",
      recipientCount: recipientIds.length,
      status: "sent",
      topic: "direct_users",
      createdAt: Timestamp.fromMillis(now)
    });
    await batch.commit();

    const fcmTokens = Array.from(new Set(
      recipientIds.map((_, idx) => {
        const t1 = String(tokenDocs[idx]?.get(UserTokens.FCM_TOKEN) ?? "").trim();
        const t2 = String(userDocs[idx]?.get("fcmToken") ?? "").trim();
        return t1 || t2;
      }).filter(Boolean)
    ));

    if (fcmTokens.length) {
      for (let i = 0; i < fcmTokens.length; i += 500) {
        const chunk = fcmTokens.slice(i, i + 500);
        await getFirebaseAdminMessaging().sendEachForMulticast({
          tokens: chunk,
          notification: { title, body: message },
          data: {
            title,
            body: message,
            message: message,
            type: notifType,
            channel: "high_priority",
            channel_id: "high_priority",
            priority: "high",
            ...(deepLink ? { deepLink } : {})
          },
          android: {
            priority: "high",
            notification: {
              channelId: "high_priority",
              title,
              body: message,
              icon: "ic_notification",
              color: "#3B82F6",
              sound: "default",
              clickAction: "OPEN_ACTIVITY",
              defaultSound: true,
              defaultVibrateTimings: true,
              visibility: "public"
            }
          }
        });
      }
    }

    const warning = fcmTokens.length === 0
      ? "Notice: Recipient has no registered FCM device token in user_tokens or users. In-app inbox was delivered, but system push cannot be delivered until the user opens the updated app to sync their device token."
      : undefined;

    return NextResponse.json({
      ok: true,
      recipientCount: recipientIds.length,
      pushed: fcmTokens.length,
      ...(warning ? { warning } : {})
    });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to send notification." }, { status: 500 });
  }
}

export const GET = cachedAdminGet(getUncached);
