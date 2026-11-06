import { NextRequest, NextResponse } from "next/server";
import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();
    const snapshot = await db
      .collection("hungr_surveys")
      .orderBy("createdAt", "desc")
      .limit(500)
      .get();

    let khammamCount = 0;
    let outsideKhammamCount = 0;
    let dailyEatersCount = 0;
    let highIntentCount = 0;

    const areaCounts: Record<string, number> = {};
    const categoryCounts: Record<string, number> = {};
    const cityCounts: Record<string, number> = {};

    const items = snapshot.docs.map((doc) => {
      const data = doc.data() as Record<string, unknown>;
      const city = String(data.city ?? "Khammam").trim();
      const area = String(data.area ?? "Unknown").trim();
      const frequency = String(data.eatingFrequency ?? "").toUpperCase();
      const isKhammam = city.toLowerCase().includes("khammam");

      if (isKhammam) {
        khammamCount++;
        areaCounts[area] = (areaCounts[area] ?? 0) + 1;
      } else {
        outsideKhammamCount++;
        cityCounts[city] = (cityCounts[city] ?? 0) + 1;
      }

      if (frequency === "DAILY") {
        dailyEatersCount++;
      }

      if (data.willingToPreOrder === true || data.willingToPreOrder === "YES_DEFINITELY") {
        highIntentCount++;
      }

      const cats = Array.isArray(data.preferredCategories) ? data.preferredCategories : [];
      for (const c of cats) {
        const catStr = String(c);
        categoryCounts[catStr] = (categoryCounts[catStr] ?? 0) + 1;
      }

      return {
        id: doc.id,
        ...data,
        createdAt: data.createdAt && typeof (data.createdAt as { toDate?: () => Date }).toDate === "function"
          ? (data.createdAt as { toDate: () => Date }).toDate().toISOString()
          : data.createdAt
      };
    });

    const sortedAreas = Object.entries(areaCounts)
      .map(([area, count]) => ({ area, count }))
      .sort((a, b) => b.count - a.count);

    const sortedCategories = Object.entries(categoryCounts)
      .map(([category, count]) => ({ category, count }))
      .sort((a, b) => b.count - a.count);

    const sortedCities = Object.entries(cityCounts)
      .map(([city, count]) => ({ city, count }))
      .sort((a, b) => b.count - a.count);

    return NextResponse.json({
      total: items.length,
      khammamCount,
      outsideKhammamCount,
      dailyEatersCount,
      highIntentCount,
      areas: sortedAreas,
      categories: sortedCategories,
      outsideCities: sortedCities,
      responses: items
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load Hungr surveys.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

export async function DELETE(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const body = (await request.json()) as { surveyId?: string };
    const surveyId = body.surveyId?.trim();
    if (!surveyId) {
      return NextResponse.json({ error: "Missing surveyId." }, { status: 400 });
    }

    const db = getFirebaseAdminDb();
    await db.collection("hungr_surveys").doc(surveyId).delete();
    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to delete survey.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
