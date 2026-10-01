import "server-only";

import { FieldPath, Timestamp, type DocumentData, type Query } from "firebase-admin/firestore";
import { NextRequest, NextResponse } from "next/server";

import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

const MAX_LIMIT = 200;

export type ListOptions = {
  /** Field to sort newest-first by; omit to page by document id. */
  orderBy?: string;
  /** Response key holding the rows (the admin viewer reads this). */
  dataKey?: string;
};

function jsonSafe(value: unknown): unknown {
  if (value instanceof Timestamp) return value.toDate().toISOString();
  if (Array.isArray(value)) return value.map(jsonSafe);
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value as Record<string, unknown>).map(([k, v]) => [k, jsonSafe(v)]));
  }
  return value;
}

function row(id: string, data: DocumentData | undefined) {
  return { id, ...(jsonSafe(data ?? {}) as Record<string, unknown>) };
}

/** "true"/"false"/numbers are compared as such; everything else as a string. */
function parseValue(raw: string): unknown {
  if (raw === "true") return true;
  if (raw === "false") return false;
  if (/^-?\d+(\.\d+)?$/.test(raw)) return Number(raw);
  return raw;
}

/**
 * One page of a collection for the admin panel — never the whole collection.
 *   ?id=DOC              exact document
 *   ?field=F&value=V     equality filter (single-field index)
 *   ?limit=N             page size (≤ 200)
 *   ?after=DOC_ID        next page (cursor = last row's id)
 */
export async function listCollection(request: NextRequest, collection: string, options: ListOptions = {}) {
  const dataKey = options.dataKey ?? "items";
  try {
    const db = getFirebaseAdminDb();
    const params = new URL(request.url).searchParams;
    const col = db.collection(collection);

    const id = params.get("id")?.trim();
    if (id) {
      const doc = await col.doc(id).get();
      return NextResponse.json({ [dataKey]: doc.exists ? [row(doc.id, doc.data())] : [], nextCursor: null });
    }

    const limit = Math.min(MAX_LIMIT, Math.max(1, Number(params.get("limit")) || 100));
    const field = params.get("field")?.trim();
    const value = params.get("value");
    let query: Query = col;
    if (field && value !== null) query = query.where(field, "==", parseValue(value));
    // An equality filter plus a different sort field would need a composite index per pair.
    const orderField = field ? undefined : options.orderBy;
    query = orderField ? query.orderBy(orderField, "desc") : query.orderBy(FieldPath.documentId());

    const after = params.get("after");
    if (after) {
      const cursor = await col.doc(after).get();
      if (cursor.exists) query = query.startAfter(cursor);
    }
    const snap = await query.limit(limit).get();
    const items = snap.docs.map((d) => row(d.id, d.data()));
    return NextResponse.json({ [dataKey]: items, nextCursor: snap.size === limit ? snap.docs.at(-1)!.id : null });
  } catch (error) {
    const message = error instanceof Error ? error.message : `Failed to load ${collection}.`;
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
