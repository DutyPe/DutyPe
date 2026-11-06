import { NextRequest, NextResponse } from "next/server";
import { Timestamp } from "firebase-admin/firestore";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

function sanitizePhone(raw: unknown): string {
  const digits = String(raw ?? "").replace(/\D/g, "");
  if (!digits) return "";
  if (digits.length === 10) return `+91${digits}`;
  if (digits.length === 12 && digits.startsWith("91")) return `+${digits}`;
  if (digits.startsWith("0") && digits.length === 11) return `+91${digits.slice(1)}`;
  return `+${digits}`;
}

export async function POST(request: NextRequest) {
  try {
    const body = (await request.json()) as Record<string, unknown>;

    const name = String(body.name ?? "").trim();
    const rawPhone = String(body.phone ?? "").trim();
    const phone = sanitizePhone(rawPhone);

    if (!phone || phone.length < 12) {
      return NextResponse.json({ error: "Please provide a valid 10-digit WhatsApp phone number." }, { status: 400 });
    }

    const city = String(body.city ?? "Khammam").trim() || "Khammam";
    const area = String(body.area ?? "").trim();
    const apartmentName = String(body.apartmentName ?? "").trim();
    const eatingFrequency = String(body.eatingFrequency ?? "DAILY").trim();
    const willingToPreOrder = body.willingToPreOrder !== false;
    const preferredCategories = Array.isArray(body.preferredCategories)
      ? body.preferredCategories.map(String).filter(Boolean)
      : [];
    const comments = String(body.comments ?? "").trim();
    const source = String(body.source ?? "WEB_WAITLIST").trim();

    const forwardedFor = request.headers.get("x-forwarded-for") || "";
    const ip = forwardedFor.split(",")[0]?.trim() || request.headers.get("cf-connecting-ip") || "unknown";
    const userAgent = request.headers.get("user-agent") || "unknown";

    const db = getFirebaseAdminDb();
    const docId = `hungr_${phone.replace(/\D/g, "")}`;

    const surveyData = {
      surveyId: docId,
      name: name || "Anonymous Foodie",
      phone,
      city,
      area: area || "Khammam Town",
      apartmentName: apartmentName || null,
      eatingFrequency,
      willingToPreOrder,
      preferredCategories: preferredCategories.length > 0 ? preferredCategories : ["TIFFIN_IDLI_DOSA"],
      comments: comments || null,
      source,
      ipAddress: ip,
      userAgent: userAgent.slice(0, 200),
      status: "VIP_WAITLIST",
      voucherEligible: true,
      voucherCode: "HUNGR50",
      createdAt: Timestamp.now(),
      updatedAt: Timestamp.now()
    };

    // Use set with merge so if the user submits again, it updates their preferences idempotently
    await db.collection("hungr_surveys").doc(docId).set(surveyData, { merge: true });

    return NextResponse.json({
      ok: true,
      surveyId: docId,
      message: "Successfully joined the Hungr VIP Early Access waitlist!",
      voucherCode: "HUNGR50"
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to record survey response.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
