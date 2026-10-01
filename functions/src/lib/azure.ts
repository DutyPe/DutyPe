/**
 * Microsoft Azure services used by DutyPe (paid from the Azure credits).
 *
 *   chatJson     Azure OpenAI chat completion that returns JSON (DutyPe AI, voice job parser)
 *   cosmosAdd    add one item to a Cosmos DB container (feedback, AI logs)
 *
 * Settings (functions/.env):
 *   AZURE_OPENAI_ENDPOINT    https://<name>.openai.azure.com
 *   AZURE_OPENAI_KEY
 *   AZURE_OPENAI_DEPLOYMENT  deployment name, e.g. dutype-chat
 *   AZURE_COSMOS_ENDPOINT    https://<name>.documents.azure.com:443/
 *   AZURE_COSMOS_KEY
 *   AZURE_COSMOS_DATABASE    default "dutype"
 *
 * Every call returns null / false when the service is not configured or fails, so callers keep
 * their existing fallbacks.
 */
import * as functions from "firebase-functions";
import { CosmosClient, type Container } from "@azure/cosmos";
import { randomUUID } from "crypto";

const OPENAI_API_VERSION = "2024-10-21";

export function azureOpenAiConfigured(): boolean {
  return Boolean(process.env.AZURE_OPENAI_ENDPOINT && process.env.AZURE_OPENAI_KEY && process.env.AZURE_OPENAI_DEPLOYMENT);
}

/** One prompt in, parsed JSON out; null when not configured, on timeout or on any error. */
export async function chatJson(
  prompt: string,
  options: { temperature?: number; maxTokens?: number; timeoutMs?: number } = {},
): Promise<unknown | null> {
  if (!azureOpenAiConfigured()) return null;
  const endpoint = process.env.AZURE_OPENAI_ENDPOINT!.replace(/\/+$/, "");
  const deployment = encodeURIComponent(process.env.AZURE_OPENAI_DEPLOYMENT!);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), options.timeoutMs ?? 15_000);
  try {
    const res = await fetch(`${endpoint}/openai/deployments/${deployment}/chat/completions?api-version=${OPENAI_API_VERSION}`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "api-key": process.env.AZURE_OPENAI_KEY! },
      body: JSON.stringify({
        messages: [
          { role: "system", content: "You always answer with a single valid JSON object and nothing else." },
          { role: "user", content: prompt },
        ],
        temperature: options.temperature ?? 0.2,
        max_tokens: options.maxTokens ?? 1024,
        response_format: { type: "json_object" },
      }),
      signal: controller.signal,
    });
    if (!res.ok) throw new Error(`Azure OpenAI ${res.status}: ${(await res.text()).slice(0, 200)}`);
    const body = await res.json() as { choices?: Array<{ message?: { content?: string } }> };
    const text = body.choices?.[0]?.message?.content || "";
    return JSON.parse(text);
  } catch (e) {
    functions.logger.warn("Azure OpenAI call failed", e);
    return null;
  } finally {
    clearTimeout(timer);
  }
}

// ─────────────────────────────── Cosmos DB ───────────────────────────────

/** Cosmos containers (database AZURE_COSMOS_DATABASE, partition key /uid). */
export const CosmosContainers = {
  FEEDBACK: "feedback",
  AI_LOGS: "ai_logs",
} as const;

let client: CosmosClient | null = null;

function container(name: string): Container | null {
  const endpoint = process.env.AZURE_COSMOS_ENDPOINT;
  const key = process.env.AZURE_COSMOS_KEY;
  if (!endpoint || !key) return null;
  client ??= new CosmosClient({ endpoint, key });
  return client.database(process.env.AZURE_COSMOS_DATABASE || "dutype").container(name);
}

export function cosmosConfigured(): boolean {
  return Boolean(process.env.AZURE_COSMOS_ENDPOINT && process.env.AZURE_COSMOS_KEY);
}

/** Adds one item (id and createdAt are filled in); false when not configured or on error. */
export async function cosmosAdd(name: string, item: Record<string, unknown>): Promise<boolean> {
  const c = container(name);
  if (!c) return false;
  try {
    await c.items.create({ id: randomUUID(), createdAt: new Date().toISOString(), ...item });
    return true;
  } catch (e) {
    functions.logger.warn(`Cosmos write to ${name} failed`, e);
    return false;
  }
}
