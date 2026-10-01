/**
 * In-app feedback, stored in Azure Cosmos DB (container "feedback"), not Firestore.
 *
 *   submitFeedback({ rating 1-5, category?, text?, appVersion? })  →  { ok: true }
 *
 * The admin panel reads it from Cosmos (web: /admin/feedback).
 */
import { onCallSecured } from "./secure-callable";
import { fail, int, obj, str, text } from "./lib/input";
import { CosmosContainers, cosmosAdd, cosmosConfigured } from "./lib/azure";

export const submitFeedback = onCallSecured({ timeoutSeconds: 15 }, async (raw: unknown, context) => {
  if (!cosmosConfigured()) fail("unavailable", "Feedback is not available right now");
  const data = obj(raw);
  const saved = await cosmosAdd(CosmosContainers.FEEDBACK, {
    uid: context.auth!.uid,
    role: String(context.auth!.token.role || ""),
    rating: int(data, "rating", { min: 1, max: 5 }),
    category: str(data, "category", { max: 40, optional: true }),
    text: text(data, "text", { max: 1000, optional: true }),
    appVersion: str(data, "appVersion", { max: 40, optional: true }),
  });
  if (!saved) fail("unavailable", "Could not send feedback. Please try again.");
  return { ok: true };
});
