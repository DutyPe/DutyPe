import { NextRequest, NextResponse } from "next/server";
import { Timestamp } from "firebase-admin/firestore";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

const VALID_TYPE = new Set(["INFO", "SUCCESS", "WARNING", "ERROR", "FEATURE", "PROMOTION"]);
const VALID_PRIORITY = new Set(["LOW", "MEDIUM", "NORMAL", "HIGH", "URGENT"]);

function normalizeTargetRoleForStorage(value: unknown): string | null {
  const candidate = String(value ?? "").trim().toUpperCase();
  if (candidate === "WORKER" || candidate === "EMPLOYER") {
    return candidate.toLowerCase();
  }
  return null;
}

function asRecord(value: unknown) {
  return (value ?? {}) as Record<string, unknown>;
}

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();
    const snapshot = await db
      .collection("announcements")
      .orderBy("createdAt", "desc")
      .limit(100)
      .get();

    const announcements = snapshot.docs.map((item) => ({
      id: item.id,
      ...(asRecord(item.data()) as Record<string, unknown>)
    }));

    return NextResponse.json({ announcements });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load announcements.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type CreateAnnouncementBody = {
  title?: string;
  message?: string;
  type?: string;
  priority?: string;
  targetRole?: string;
  imageUrl?: string;
  deepLink?: string;
  actionText?: string;
  isDismissible?: boolean;
  expiresInDays?: number;
};

export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let body: CreateAnnouncementBody;

  try {
    body = (await request.json()) as CreateAnnouncementBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const title = body.title?.trim() ?? "";
  const message = body.message?.trim() ?? "";

  if (!title || !message) {
    return NextResponse.json({ error: "Title and message are required." }, { status: 400 });
  }

  try {
    const db = getFirebaseAdminDb();
    const now = new Date();
    const expiresInDays = Math.max(1, Math.min(365, Number(body.expiresInDays) || 30));
    const endDateValue = new Date(now.getTime() + expiresInDays * 24 * 60 * 60 * 1000);

    const typeRaw = (body.type?.trim() || "INFO").toUpperCase();
    const announcementType = VALID_TYPE.has(typeRaw) ? typeRaw : "INFO";

    const priorityRaw = (body.priority?.trim() || "NORMAL").toUpperCase();
    const announcementPriority = VALID_PRIORITY.has(priorityRaw) ? priorityRaw : "NORMAL";

    const targetRole = normalizeTargetRoleForStorage(body.targetRole);
    const imageUrl = body.imageUrl?.trim() || "";
    const deepLink = body.deepLink?.trim() || "";
    const actionText = body.actionText?.trim() || (deepLink ? "View" : "");
    const isDismissible = body.isDismissible !== false;

    const created = await db.collection("announcements").add({
      title,
      message,
      type: announcementType,
      priority: announcementPriority,
      targetRole,
      imageUrl,
      actionText,
      actionRoute: deepLink,
      isDismissible,
      isActive: true,
      createdBy: "admin",
      createdAt: Timestamp.fromDate(now),
      expiresAt: Timestamp.fromDate(endDateValue)
    });

    return NextResponse.json({ ok: true, announcementId: created.id });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to create announcement.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type UpdateAnnouncementBody = {
  announcementId?: string;
  isActive?: boolean;
};

export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let body: UpdateAnnouncementBody;

  try {
    body = (await request.json()) as UpdateAnnouncementBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const announcementId = body.announcementId?.trim();

  if (!announcementId || typeof body.isActive !== "boolean") {
    return NextResponse.json({ error: "Missing announcementId or isActive." }, { status: 400 });
  }

  try {
    const db = getFirebaseAdminDb();
    await db.collection("announcements").doc(announcementId).set(
      {
        isActive: body.isActive,
        updatedAt: Timestamp.fromDate(new Date())
      },
      { merge: true }
    );

    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to update announcement.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type DeleteAnnouncementBody = {
  announcementId?: string;
};

export async function DELETE(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let body: DeleteAnnouncementBody;

  try {
    body = (await request.json()) as DeleteAnnouncementBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const announcementId = body.announcementId?.trim();

  if (!announcementId) {
    return NextResponse.json({ error: "Missing announcementId." }, { status: 400 });
  }

  try {
    const db = getFirebaseAdminDb();
    await db.collection("announcements").doc(announcementId).delete();
    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to delete announcement.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
