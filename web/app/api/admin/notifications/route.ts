import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminAuth, getFirebaseAdminDb, getFirebaseAdminMessaging } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

type NotificationPreset = "APP_UPDATE" | "MAINTENANCE" | "EMERGENCY" | "GENERAL";
type TargetRole = "ALL" | "WORKER" | "EMPLOYER";

type CreateNotificationBody = {
  preset?: NotificationPreset;
  title?: string;
  message?: string;
  targetRole?: TargetRole;
  sendPush?: boolean;
  deepLink?: string;
};

const PLAY_STORE_PACKAGE_ID = "com.dutype.app";
const PLAY_STORE_WEB_URL = `https://play.google.com/store/apps/details?id=${PLAY_STORE_PACKAGE_ID}`;

function asRecord(value: unknown) {
  return (value ?? {}) as Record<string, unknown>;
}

function normalizeTargetRole(value: unknown): TargetRole {
  const candidate = String(value ?? "ALL").trim().toUpperCase();

  if (candidate === "WORKER" || candidate === "EMPLOYER") {
    return candidate;
  }

  return "ALL";
}

function normalizePreset(value: unknown): NotificationPreset {
  const candidate = String(value ?? "GENERAL").trim().toUpperCase();

  if (candidate === "APP_UPDATE" || candidate === "MAINTENANCE" || candidate === "EMERGENCY") {
    return candidate;
  }

  return "GENERAL";
}

function recipientsMatchAudience(doc: Record<string, unknown>, targetRole: TargetRole) {
  if (targetRole === "ALL") {
    return true;
  }

  const activeRole = String(doc.activeRole ?? "").trim().toUpperCase();
  const directRole = String(doc.role ?? "").trim().toUpperCase();
  const roles = Array.isArray(doc.roles)
    ? doc.roles.map((item) => String(item).trim().toUpperCase())
    : [];

  return activeRole === targetRole || directRole === targetRole || roles.includes(targetRole);
}

function resolvePushTopic(preset: NotificationPreset, targetRole: TargetRole) {
  if (targetRole === "WORKER") {
    return "workers";
  }

  if (targetRole === "EMPLOYER") {
    return "employers";
  }

  if (preset === "APP_UPDATE") {
    return "app_updates";
  }

  return "all_users";
}

function resolveNotificationType(preset: NotificationPreset) {
  return preset === "GENERAL" ? "GENERAL" : "SYSTEM_UPDATE";
}

async function fetchRecipients(targetRole: TargetRole) {
  const db = getFirebaseAdminDb();
  const auth = getFirebaseAdminAuth();

  const usersSnapshot = await db.collection("users").limit(5000).get();
  const firestoreUsers = usersSnapshot.docs.map((item) => ({ id: item.id, data: asRecord(item.data()) }));

  const authIds = new Set<string>();
  let pageToken: string | undefined;

  do {
    const page = await auth.listUsers(1000, pageToken);
    page.users.forEach((entry) => authIds.add(entry.uid));
    pageToken = page.pageToken;
  } while (pageToken);

  const recipients = new Set<string>();

  if (targetRole === "ALL") {
    firestoreUsers.forEach((recipient) => recipients.add(recipient.id));
    authIds.forEach((recipientId) => recipients.add(recipientId));
    return Array.from(recipients);
  }

  firestoreUsers
    .filter((recipient) => recipientsMatchAudience(recipient.data, targetRole))
    .forEach((recipient) => recipients.add(recipient.id));

  return Array.from(recipients);
}

async function createNotificationDocs(params: {
  recipients: string[];
  title: string;
  message: string;
  type: string;
  preset: NotificationPreset;
  targetRole: TargetRole;
  sendPush: boolean;
  deepLink: string | undefined;
  pushTopic: string;
}) {
  const db = getFirebaseAdminDb();
  const campaignRef = db.collection("admin_notification_campaigns").doc();
  const createdAt = new Date();

  const campaign = {
    title: params.title,
    message: params.message,
    type: params.type,
    preset: params.preset,
    targetRole: params.targetRole,
    sendPush: params.sendPush,
    deepLink: params.deepLink ?? "",
    pushTopic: params.pushTopic,
    recipientCount: params.recipients.length,
    createdAt,
    updatedAt: createdAt
  };

  await campaignRef.set(campaign);

  const chunks: string[][] = [];
  for (let index = 0; index < params.recipients.length; index += 400) {
    chunks.push(params.recipients.slice(index, index + 400));
  }

  for (const chunk of chunks) {
    const batch = db.batch();

    chunk.forEach((recipientId) => {
      const notificationRef = db.collection("notifications").doc();
      batch.set(notificationRef, {
        recipientId,
        title: params.title,
        message: params.message,
        type: params.type,
        targetRole: params.targetRole,
        data: {
          preset: params.preset,
          targetRole: params.targetRole,
          deepLink: params.deepLink ?? "",
          source: "ADMIN",
          pushTopic: params.pushTopic
        },
        createdAt,
        expiresAt: new Date(createdAt.getTime() + 45 * 24 * 60 * 60 * 1000),
        isRead: false
      });
    });

    await batch.commit();
  }

  return { campaignId: campaignRef.id, recipientCount: params.recipients.length };
}

async function sendPushBroadcast(params: {
  title: string;
  message: string;
  type: string;
  targetRole: TargetRole;
  deepLink: string | undefined;
  pushTopic: string;
}) {
  const messaging = getFirebaseAdminMessaging();

  await messaging.send({
    topic: params.pushTopic,
    notification: {
      title: params.title,
      body: params.message
    },
    data: {
      title: params.title,
      body: params.message,
      type: params.type,
      targetRole: params.targetRole,
      deepLink: params.deepLink ?? "",
      channel: "high_priority"
    },
    android: {
      priority: "high",
      notification: {
        channelId: "high_priority"
      }
    }
  });
}

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();
    const snapshot = await db
      .collection("admin_notification_campaigns")
      .orderBy("createdAt", "desc")
      .limit(100)
      .get();

    const campaigns = snapshot.docs.map((item) => ({
      id: item.id,
      ...(asRecord(item.data()) as Record<string, unknown>)
    }));

    return NextResponse.json({ campaigns });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load notification campaigns.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let body: CreateNotificationBody;

  try {
    body = (await request.json()) as CreateNotificationBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const preset = normalizePreset(body.preset);
  const title = body.title?.trim() ?? "";
  const message = body.message?.trim() ?? "";
  const targetRole = normalizeTargetRole(body.targetRole);
  const sendPush = body.sendPush !== false;
  const deepLink = body.deepLink?.trim() || undefined;
  const effectiveDeepLink = preset === "APP_UPDATE" ? PLAY_STORE_WEB_URL : deepLink;

  if (!title || !message) {
    return NextResponse.json({ error: "Title and message are required." }, { status: 400 });
  }

  try {
    const recipients = await fetchRecipients(targetRole);

    if (recipients.length === 0) {
      return NextResponse.json({ error: "No matching users were found for this audience." }, { status: 400 });
    }

    const pushTopic = resolvePushTopic(preset, targetRole);
    const type = resolveNotificationType(preset);

    const result = await createNotificationDocs({
      recipients,
      title,
      message,
      type,
      preset,
      targetRole,
      sendPush,
      deepLink: effectiveDeepLink,
      pushTopic
    });

    if (sendPush) {
      await sendPushBroadcast({
        title,
        message,
        type,
        targetRole,
        deepLink: effectiveDeepLink,
        pushTopic
      });
    }

    return NextResponse.json({ ok: true, ...result, pushTopic, type });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to broadcast notification.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}