import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

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
  targetRole?: string;
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
    const created = await db.collection("announcements").add({
      title,
      message,
      type: body.type?.trim() || "INFO",
      targetRole: body.targetRole?.trim() || "ALL",
      isActive: true,
      priority: 1,
      createdAt: new Date(),
      updatedAt: new Date()
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
        updatedAt: new Date()
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
