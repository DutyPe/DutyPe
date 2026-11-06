import { NextRequest, NextResponse } from "next/server";
import { FieldValue } from "firebase-admin/firestore";

import { getAdminActor, logAdminAudit, requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

const KHAMMAM_LAT = 17.2473;
const KHAMMAM_LNG = 80.1514;

export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request, { sensitive: true });
  if (unauthorized) return unauthorized;
  const actor = await getAdminActor(request);

  let body: Record<string, any>;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const { partnerId, name, phone, categories, area, lat, lng } = body;
  if (!partnerId || !phone) {
    return NextResponse.json({ error: "partnerId and phone are required" }, { status: 400 });
  }

  const db = getFirebaseAdminDb();
  const partnerRef = db.collection("service_partners").doc(partnerId);
  const existing = await partnerRef.get();

  const partnerData: Record<string, any> = {
    name: name || "Service Partner",
    phone: phone,
    categories: Array.isArray(categories) && categories.length > 0 ? categories : ["ALL"],
    status: "APPROVED",
    online: true,
    lat: typeof lat === "number" ? lat : KHAMMAM_LAT,
    lng: typeof lng === "number" ? lng : KHAMMAM_LNG,
    area: area || "Khammam",
    approvedAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
    cancellations: 0
  };

  if (!existing.exists) {
    partnerData.createdAt = FieldValue.serverTimestamp();
    partnerData.creditsPaise = 10000; // Rs 100 welcome credit
    partnerData.jobsCompleted = 0;
  }

  await partnerRef.set(partnerData, { merge: true });

  if (actor) {
    void logAdminAudit("ASSIGN_SERVICE_PARTNER", actor, { partnerId, name, phone, categories: partnerData.categories }, request);
  }

  return NextResponse.json({ ok: true, partner: { id: partnerId, ...partnerData } });
}
