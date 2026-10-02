"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.submitFeedback = void 0;
/**
 * In-app feedback, stored in Azure Cosmos DB (container "feedback"), not Firestore.
 *
 *   submitFeedback({ rating 1-5, category?, text?, appVersion? })  →  { ok: true }
 *
 * The admin panel reads it from Cosmos (web: /admin/feedback).
 */
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const azure_1 = require("./lib/azure");
exports.submitFeedback = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 15, secrets: [azure_1.AZURE_COSMOS_SECRET] }, async (raw, context) => {
    if (!(0, azure_1.cosmosConfigured)())
        (0, input_1.fail)("unavailable", "Feedback is not available right now");
    const data = (0, input_1.obj)(raw);
    const saved = await (0, azure_1.cosmosAdd)(azure_1.CosmosContainers.FEEDBACK, {
        uid: context.auth.uid,
        role: String(context.auth.token.role || ""),
        rating: (0, input_1.int)(data, "rating", { min: 1, max: 5 }),
        category: (0, input_1.str)(data, "category", { max: 40, optional: true }),
        text: (0, input_1.text)(data, "text", { max: 1000, optional: true }),
        appVersion: (0, input_1.str)(data, "appVersion", { max: 40, optional: true }),
    });
    if (!saved)
        (0, input_1.fail)("unavailable", "Could not send feedback. Please try again.");
    return { ok: true };
});
//# sourceMappingURL=feedback.js.map