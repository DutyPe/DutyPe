import { NextRequest } from "next/server";

import { cachedAdminGet } from "@/lib/firebase/admin-response-cache";
import { listCollection } from "@/lib/firebase/admin-list";
import { WorkerProfiles } from "@/lib/firebase/schema";

export const runtime = "nodejs";

// One page at a time (newest first); cached briefly, cleared on any admin write.
export const GET = cachedAdminGet((request: NextRequest) =>
  listCollection(request, WorkerProfiles.COLLECTION, { orderBy: WorkerProfiles.CREATED_AT, dataKey: "profiles" })
);
