import { NextRequest } from "next/server";

import { CosmosContainers, listCosmos } from "@/lib/azure/cosmos";
import { cachedAdminGet } from "@/lib/firebase/admin-response-cache";

export const runtime = "nodejs";

// Stored in Azure Cosmos DB, not Firestore. One page at a time (newest first).
export const GET = cachedAdminGet((request: NextRequest) => listCosmos(request, CosmosContainers.AI_LOGS));
