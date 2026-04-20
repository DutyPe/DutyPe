import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminAuth, getFirebaseAdminDb, getFirebaseAdminMessaging } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

type NotificationPreset = "APP_UPDATE" | "MAINTENANCE" | "EMERGENCY" | "GENERAL";
type TargetRole = "ALL" | "WORKER" | "EMPLOYER";

type SupportedLocale = "en" | "te";
const SUPPORTED_LOCALES: SupportedLocale[] = ["en", "te"];
const DEFAULT_LOCALE: SupportedLocale = "en";

type LocalizedCopy = { title?: string; message?: string };
type TranslationsMap = Partial<Record<SupportedLocale, LocalizedCopy>>;

type CreateNotificationBody = {
  preset?: NotificationPreset;
  title?: string;
  message?: string;
  targetRole?: TargetRole;
  sendPush?: boolean;
  deepLink?: string;
  recipientIds?: string[];
  /**
   * Optional per-language overrides. When supplied, each recipient's stored
   * `language` decides which copy is written to their inbox and pushed to
   * their device. Topic broadcasts fan out to `${topic}_en` and `${topic}_te`.
   */
  translations?: TranslationsMap;
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

function normalizeLocale(value: unknown): SupportedLocale {
  const raw = String(value ?? "").trim().toLowerCase();
  if (raw === "te") return "te";
  return DEFAULT_LOCALE;
}

function sanitizeTranslations(input: unknown): TranslationsMap | undefined {
  if (!input || typeof input !== "object") return undefined;
  const map = input as Record<string, unknown>;
  const out: TranslationsMap = {};
  for (const lang of SUPPORTED_LOCALES) {
    const entry = map[lang];
    if (!entry || typeof entry !== "object") continue;
    const e = entry as Record<string, unknown>;
    const title = typeof e.title === "string" ? e.title.trim() : "";
    const message = typeof e.message === "string" ? e.message.trim() : "";
    if (title || message) {
      out[lang] = { title: title || undefined, message: message || undefined };
    }
  }
  return Object.keys(out).length ? out : undefined;
}

function pickLocalized(
  translations: TranslationsMap | undefined,
  locale: SupportedLocale,
  fallbackTitle: string,
  fallbackMessage: string
): { title: string; message: string } {
  const t = translations?.[locale] ?? translations?.[DEFAULT_LOCALE];
  return {
    title: (t?.title && t.title.trim()) || fallbackTitle,
    message: (t?.message && t.message.trim()) || fallbackMessage,
  };
}

async function fetchUserLanguageMap(userIds: string[]): Promise<Map<string, SupportedLocale>> {
  const db = getFirebaseAdminDb();
  const out = new Map<string, SupportedLocale>();
  const unique = Array.from(new Set(userIds.filter(Boolean)));
  for (let i = 0; i < unique.length; i += 30) {
    const chunk = unique.slice(i, i + 30);
    const refs = chunk.map((id) => db.collection("users").doc(id));
    const docs = await db.getAll(...refs);
    docs.forEach((doc) => {
      out.set(doc.id, doc.exists ? normalizeLocale((doc.data() ?? {}).language) : DEFAULT_LOCALE);
    });
  }
  unique.forEach((id) => {
    if (!out.has(id)) out.set(id, DEFAULT_LOCALE);
  });
  return out;
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
  translations?: TranslationsMap;
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
    translations: params.translations ?? null,
    createdAt,
    updatedAt: createdAt
  };

  await campaignRef.set(campaign);

  // When translations are provided, look up each recipient's language so the
  // inbox copy matches what the device push will show.
  const languageMap = params.translations
    ? await fetchUserLanguageMap(params.recipients)
    : null;

  const chunks: string[][] = [];
  for (let index = 0; index < params.recipients.length; index += 400) {
    chunks.push(params.recipients.slice(index, index + 400));
  }

  for (const chunk of chunks) {
    const batch = db.batch();

    chunk.forEach((recipientId) => {
      const locale = languageMap?.get(recipientId) ?? DEFAULT_LOCALE;
      const { title, message } = pickLocalized(
        params.translations,
        locale,
        params.title,
        params.message
      );
      const notificationRef = db.collection("notifications").doc();
      batch.set(notificationRef, {
        recipientId,
        title,
        message,
        type: params.type,
        targetRole: params.targetRole,
        locale,
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

async function sendPushToUsers(params: {
  userIds: string[];
  title: string;
  message: string;
  type: string;
  targetRole: TargetRole;
  deepLink: string | undefined;
  translations?: TranslationsMap;
}) {
  const db = getFirebaseAdminDb();
  const messaging = getFirebaseAdminMessaging();

  // Collect (token, locale) pairs so we can group by locale when translations
  // are present. When no translations are given, every locale resolves to the
  // same fallback copy.
  const byLocale: Record<SupportedLocale, string[]> = { en: [], te: [] };
  for (let i = 0; i < params.userIds.length; i += 30) {
    const chunk = params.userIds.slice(i, i + 30);
    const refs = chunk.map((id) => db.collection("users").doc(id));
    const docs = await db.getAll(...refs);
    docs.forEach((doc) => {
      if (!doc.exists) return;
      const data = (doc.data() ?? {}) as Record<string, unknown>;
      const token = typeof data.fcmToken === "string" ? data.fcmToken.trim() : "";
      if (!token) return;
      const locale = normalizeLocale(data.language);
      byLocale[locale].push(token);
    });
  }

  for (const lang of SUPPORTED_LOCALES) {
    const tokens = byLocale[lang];
    if (tokens.length === 0) continue;

    const { title, message } = pickLocalized(
      params.translations,
      lang,
      params.title,
      params.message
    );

    for (let i = 0; i < tokens.length; i += 500) {
      const batch = tokens.slice(i, i + 500);
      await messaging.sendEachForMulticast({
        tokens: batch,
        notification: { title, body: message },
        data: {
          title,
          body: message,
          type: params.type,
          targetRole: params.targetRole,
          deepLink: params.deepLink ?? "",
          channel: "high_priority",
          locale: lang,
        },
        android: {
          priority: "high",
          notification: { channelId: "high_priority" }
        }
      });
    }
  }
}

async function sendPushBroadcast(params: {
  title: string;
  message: string;
  type: string;
  targetRole: TargetRole;
  deepLink: string | undefined;
  pushTopic: string;
  translations?: TranslationsMap;
}) {
  const messaging = getFirebaseAdminMessaging();

  const sendOne = async (topic: string, title: string, message: string, locale: string) => {
    await messaging.send({
      topic,
      notification: { title, body: message },
      data: {
        title,
        body: message,
        type: params.type,
        targetRole: params.targetRole,
        deepLink: params.deepLink ?? "",
        channel: "high_priority",
        locale,
      },
      android: {
        priority: "high",
        notification: { channelId: "high_priority" }
      }
    });
  };

  if (params.translations) {
    for (const lang of SUPPORTED_LOCALES) {
      const { title, message } = pickLocalized(
        params.translations,
        lang,
        params.title,
        params.message
      );
      await sendOne(`${params.pushTopic}_${lang}`, title, message, lang);
    }
    return;
  }

  await sendOne(params.pushTopic, params.title, params.message, DEFAULT_LOCALE);
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
  const translations = sanitizeTranslations(body.translations);

  if (!title || !message) {
    return NextResponse.json({ error: "Title and message are required." }, { status: 400 });
  }

  try {
    const explicitIds = Array.isArray(body.recipientIds)
      ? body.recipientIds.map((id) => String(id).trim()).filter(Boolean)
      : [];
    const recipients = explicitIds.length > 0
      ? Array.from(new Set(explicitIds))
      : await fetchRecipients(targetRole);

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
      pushTopic,
      translations
    });

    if (sendPush) {
      if (explicitIds.length > 0) {
        // Targeted users: look up FCM tokens and send by token (topic targets the whole role)
        await sendPushToUsers({
          userIds: recipients,
          title,
          message,
          type,
          targetRole,
          deepLink: effectiveDeepLink,
          translations
        });
      } else {
        await sendPushBroadcast({
          title,
          message,
          type,
          targetRole,
          deepLink: effectiveDeepLink,
          pushTopic,
          translations
        });
      }
    }

    return NextResponse.json({ ok: true, ...result, pushTopic, type });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to broadcast notification.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}