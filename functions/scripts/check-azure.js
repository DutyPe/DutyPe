/**
 * Checks the Azure setup end to end, from your PC, before/after deploying:
 *   1. Azure OpenAI answers a small JSON prompt with the dutype-chat deployment
 *   2. Cosmos DB: writes, reads back and deletes a test item in feedback, ai_logs, admin_activity
 *
 * Endpoints come from functions/.env.dutype-860ac; the keys are read from the Firebase secrets
 * (firebase functions:secrets:access) and never printed.
 *
 * Run from the functions folder:  node scripts/check-azure.js
 */
const { execSync } = require("child_process");
const fs = require("fs");
const path = require("path");
const { CosmosClient } = require("@azure/cosmos");

const env = Object.fromEntries(
  fs.readFileSync(path.join(__dirname, "..", ".env.dutype-860ac"), "utf8")
    .split(/\r?\n/)
    .filter((line) => /^[A-Z_]+=/.test(line))
    .map((line) => [line.slice(0, line.indexOf("=")), line.slice(line.indexOf("=") + 1).trim()]),
);
for (const key of ["AZURE_OPENAI_KEY", "AZURE_COSMOS_KEY"]) {
  if (env[key]) {
    console.error(`✗ Remove ${key} from .env.dutype-860ac — keys must only be Firebase secrets.`);
    process.exit(1);
  }
}

const localEnvPath = path.join(__dirname, "..", ".env");
const localEnv = fs.existsSync(localEnvPath)
  ? Object.fromEntries(
      fs.readFileSync(localEnvPath, "utf8")
        .split(/\r?\n/)
        .filter((line) => /^[A-Z_]+=/.test(line))
        .map((line) => [
          line.slice(0, line.indexOf("=")),
          line.slice(line.indexOf("=") + 1).replace(/^["']|["']$/g, "").trim(),
        ]),
    )
  : {};

function secret(name) {
  if (process.env[name]) return process.env[name];
  if (localEnv[name]) return localEnv[name];
  try {
    return execSync(`firebase functions:secrets:access ${name} --project dutype-860ac`, {
      encoding: "utf8", stdio: ["ignore", "pipe", "pipe"],
    }).trim();
  } catch {
    console.error(`✗ Could not read secret ${name}. Run: firebase functions:secrets:set ${name} or add to functions/.env`);
    process.exit(1);
  }
}

async function checkOpenAi() {
  const endpoint = env.AZURE_OPENAI_ENDPOINT.replace(/\/+$/, "");
  const res = await fetch(
    `${endpoint}/openai/deployments/${env.AZURE_OPENAI_DEPLOYMENT}/chat/completions?api-version=2024-10-21`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json", "api-key": secret("AZURE_OPENAI_KEY") },
      body: JSON.stringify({
        messages: [{ role: "user", content: 'Reply as JSON {"reply": "..."} with a one-line hello in Telugu.' }],
        max_tokens: 60,
        response_format: { type: "json_object" },
      }),
    },
  );
  if (!res.ok) throw new Error(`Azure OpenAI ${res.status}: ${(await res.text()).slice(0, 300)}`);
  const body = await res.json();
  console.log(`✓ Azure OpenAI (${env.AZURE_OPENAI_DEPLOYMENT}) replied:`, body.choices[0].message.content);
}

async function checkCosmos() {
  const client = new CosmosClient({ endpoint: env.AZURE_COSMOS_ENDPOINT, key: secret("AZURE_COSMOS_KEY") });
  const db = client.database(env.AZURE_COSMOS_DATABASE || "dutype");
  for (const name of ["feedback", "ai_logs", "admin_activity"]) {
    const container = db.container(name);
    const id = `check-${Date.now()}`;
    await container.items.create({ id, uid: "check-azure", createdAt: new Date().toISOString() });
    const { resource } = await container.item(id, "check-azure").read();
    if (!resource) throw new Error(`Cosmos ${name}: wrote but could not read back`);
    await container.item(id, "check-azure").delete();
    console.log(`✓ Cosmos DB ${name}: write, read and delete work`);
  }
}

(async () => {
  try {
    await checkOpenAi();
  } catch (e) {
    console.error("✗ Azure OpenAI:", e.message);
    process.exitCode = 1;
  }
  try {
    await checkCosmos();
  } catch (e) {
    console.error("✗ Cosmos DB:", e.message);
    process.exitCode = 1;
  }
})();
