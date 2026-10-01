import "server-only";

import { CosmosClient, type Container } from "@azure/cosmos";
import { randomUUID } from "node:crypto";
import { NextRequest, NextResponse } from "next/server";

/**
 * Azure Cosmos DB (database AZURE_COSMOS_DATABASE, default "dutype"; partition key /uid).
 * Holds data that is not linked to any Firestore collection: in-app feedback, DutyPe AI
 * conversation logs and the admin activity log.
 *
 * Env: AZURE_COSMOS_ENDPOINT, AZURE_COSMOS_KEY, AZURE_COSMOS_DATABASE (optional).
 */
export const CosmosContainers = {
  FEEDBACK: "feedback",
  AI_LOGS: "ai_logs",
  ADMIN_ACTIVITY: "admin_activity"
} as const;

const MAX_LIMIT = 200;
let client: CosmosClient | null = null;

function container(name: string): Container | null {
  const endpoint = process.env.AZURE_COSMOS_ENDPOINT;
  const key = process.env.AZURE_COSMOS_KEY;
  if (!endpoint || !key) return null;
  client ??= new CosmosClient({ endpoint, key });
  return client.database(process.env.AZURE_COSMOS_DATABASE || "dutype").container(name);
}

/** Adds one item (id and createdAt filled in). Never throws: logging must not break an admin action. */
export async function cosmosAdd(name: string, item: Record<string, unknown>) {
  const c = container(name);
  if (!c) return false;
  try {
    await c.items.create({ id: randomUUID(), createdAt: new Date().toISOString(), ...item });
    return true;
  } catch (error) {
    console.warn(`Cosmos write to ${name} failed`, error);
    return false;
  }
}

/**
 * One page of a Cosmos container for the admin panel, newest first (same shape as listCollection).
 *   ?id=ID               exact item
 *   ?field=F&value=V     equality filter
 *   ?limit=N             page size (≤ 200)
 *   ?after=TOKEN         next page (continuation token)
 */
export async function listCosmos(request: NextRequest, name: string, dataKey = "items") {
  const c = container(name);
  if (!c) {
    return NextResponse.json(
      { error: "Azure Cosmos DB is not configured (AZURE_COSMOS_ENDPOINT / AZURE_COSMOS_KEY)." },
      { status: 503 }
    );
  }
  try {
    const params = new URL(request.url).searchParams;
    const limit = Math.min(MAX_LIMIT, Math.max(1, Number(params.get("limit")) || 100));
    const id = params.get("id")?.trim();
    const field = params.get("field")?.trim();
    const value = params.get("value");

    const where: string[] = [];
    const parameters: Array<{ name: string; value: string | number | boolean }> = [];
    if (id) {
      where.push("c.id = @id");
      parameters.push({ name: "@id", value: id });
    } else if (field && value !== null) {
      if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(field)) {
        return NextResponse.json({ error: "Invalid field name." }, { status: 400 });
      }
      where.push(`c.${field} = @value`);
      const parsed = value === "true" ? true : value === "false" ? false : /^-?\d+(\.\d+)?$/.test(value) ? Number(value) : value;
      parameters.push({ name: "@value", value: parsed });
    }
    const query = `SELECT * FROM c${where.length ? ` WHERE ${where.join(" AND ")}` : ""} ORDER BY c.createdAt DESC`;

    const page = await c.items
      .query({ query, parameters }, { maxItemCount: limit, continuationToken: params.get("after") || undefined })
      .fetchNext();
    const items = page.resources.map((item: Record<string, unknown>) =>
      Object.fromEntries(Object.entries(item).filter(([key]) => !key.startsWith("_")))
    );
    return NextResponse.json({ [dataKey]: items, nextCursor: page.continuationToken ?? null });
  } catch (error) {
    const message = error instanceof Error ? error.message : `Failed to load ${name}.`;
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
