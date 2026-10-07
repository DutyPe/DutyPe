"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CosmosContainers = exports.AZURE_COSMOS_SECRET = exports.AZURE_OPENAI_SECRET = void 0;
exports.azureOpenAiConfigured = azureOpenAiConfigured;
exports.chatJson = chatJson;
exports.cosmosConfigured = cosmosConfigured;
exports.cosmosAdd = cosmosAdd;
/**
 * Microsoft Azure services used by DutyPe (paid from the Azure credits).
 *
 *   chatJson     Azure OpenAI chat completion that returns JSON (DutyPe AI, voice job parser)
 *   cosmosAdd    add one item to a Cosmos DB container (feedback, AI logs)
 *
 * Settings: endpoints and names are in functions/.env.dutype-860ac (committed, not secret);
 * the two keys are Firebase secrets (Secret Manager), never in a file:
 *   firebase functions:secrets:set AZURE_OPENAI_KEY
 *   firebase functions:secrets:set AZURE_COSMOS_KEY
 *
 * Every call returns null / false when the service is not configured or fails, so callers keep
 * their existing fallbacks.
 */
const functions = require("firebase-functions");
const cosmos_1 = require("@azure/cosmos");
const crypto_1 = require("crypto");
const OPENAI_API_VERSION = "2024-10-21";
/** Secret names to bind on the functions that call Azure OpenAI / Cosmos DB. */
exports.AZURE_OPENAI_SECRET = "AZURE_OPENAI_KEY";
exports.AZURE_COSMOS_SECRET = "AZURE_COSMOS_KEY";
function azureOpenAiConfigured() {
    return Boolean(process.env.AZURE_OPENAI_ENDPOINT && process.env.AZURE_OPENAI_KEY && process.env.AZURE_OPENAI_DEPLOYMENT);
}
/** One prompt in, parsed JSON out; null when not configured, on timeout or on any error. */
async function chatJson(prompt, options = {}) {
    var _a, _b, _c, _d, _e, _f;
    if (!azureOpenAiConfigured())
        return null;
    const endpoint = process.env.AZURE_OPENAI_ENDPOINT.replace(/\/+$/, "");
    const deployment = encodeURIComponent(process.env.AZURE_OPENAI_DEPLOYMENT);
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), (_a = options.timeoutMs) !== null && _a !== void 0 ? _a : 15000);
    try {
        const res = await fetch(`${endpoint}/openai/deployments/${deployment}/chat/completions?api-version=${OPENAI_API_VERSION}`, {
            method: "POST",
            headers: { "Content-Type": "application/json", "api-key": process.env.AZURE_OPENAI_KEY },
            body: JSON.stringify({
                messages: [
                    { role: "system", content: "You always answer with a single valid JSON object and nothing else." },
                    { role: "user", content: prompt },
                ],
                temperature: (_b = options.temperature) !== null && _b !== void 0 ? _b : 0.2,
                max_tokens: (_c = options.maxTokens) !== null && _c !== void 0 ? _c : 1024,
                response_format: { type: "json_object" },
            }),
            signal: controller.signal,
        });
        if (!res.ok)
            throw new Error(`Azure OpenAI ${res.status}: ${(await res.text()).slice(0, 200)}`);
        const body = await res.json();
        const text = ((_f = (_e = (_d = body.choices) === null || _d === void 0 ? void 0 : _d[0]) === null || _e === void 0 ? void 0 : _e.message) === null || _f === void 0 ? void 0 : _f.content) || "";
        return JSON.parse(text);
    }
    catch (e) {
        functions.logger.warn("Azure OpenAI call failed", e);
        return null;
    }
    finally {
        clearTimeout(timer);
    }
}
// ─────────────────────────────── Cosmos DB ───────────────────────────────
/** Cosmos containers (database AZURE_COSMOS_DATABASE, partition key /uid). */
exports.CosmosContainers = {
    FEEDBACK: "feedback",
    AI_LOGS: "ai_logs",
};
let client = null;
function container(name) {
    const endpoint = process.env.AZURE_COSMOS_ENDPOINT;
    const key = process.env.AZURE_COSMOS_KEY;
    if (!endpoint || !key)
        return null;
    client !== null && client !== void 0 ? client : (client = new cosmos_1.CosmosClient({ endpoint, key }));
    return client.database(process.env.AZURE_COSMOS_DATABASE || "dutype").container(name);
}
function cosmosConfigured() {
    return Boolean(process.env.AZURE_COSMOS_ENDPOINT && process.env.AZURE_COSMOS_KEY);
}
/** Adds one item (id and createdAt are filled in); false when not configured or on error. */
async function cosmosAdd(name, item) {
    var _a;
    const c = container(name);
    if (!c)
        return false;
    try {
        await c.items.create(Object.assign({ id: (0, crypto_1.randomUUID)(), createdAt: new Date().toISOString() }, item));
        return true;
    }
    catch (e) {
        const err = e;
        if ((err === null || err === void 0 ? void 0 : err.code) === 404 || (err === null || err === void 0 ? void 0 : err.statusCode) === 404 || ((_a = err === null || err === void 0 ? void 0 : err.message) === null || _a === void 0 ? void 0 : _a.includes("NotFound"))) {
            try {
                const endpoint = process.env.AZURE_COSMOS_ENDPOINT;
                const key = process.env.AZURE_COSMOS_KEY;
                if (endpoint && key) {
                    client !== null && client !== void 0 ? client : (client = new cosmos_1.CosmosClient({ endpoint, key }));
                    await client.database(process.env.AZURE_COSMOS_DATABASE || "dutype").containers.createIfNotExists({
                        id: name,
                        partitionKey: { paths: ["/uid"] }
                    });
                    const retryC = container(name);
                    if (retryC) {
                        await retryC.items.create(Object.assign({ id: (0, crypto_1.randomUUID)(), createdAt: new Date().toISOString() }, item));
                        return true;
                    }
                }
            }
            catch (retryErr) {
                functions.logger.warn(`Failed to auto-create Cosmos container ${name}`, retryErr);
            }
        }
        functions.logger.warn(`Cosmos write to ${name} failed`, e);
        return false;
    }
}
//# sourceMappingURL=azure.js.map