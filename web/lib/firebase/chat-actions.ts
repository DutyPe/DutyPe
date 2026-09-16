import { httpsCallable } from "firebase/functions";

import type { ProductRole } from "@/lib/product/profile";

import { getFirebaseServices } from "./client";

type GetOrCreateConversationPayload = {
  jobId?: string;
  otherUserId: string;
};

type GetOrCreateConversationResult = {
  conversationId: string;
  isNew: boolean;
};

export async function getOrCreateConversationId(
  otherUserId: string,
  jobId?: string
): Promise<string> {
  const services = getFirebaseServices();

  if (!services) {
    throw new Error("Firebase is not configured for chat.");
  }

  const normalizedUserId = otherUserId.trim();
  if (!normalizedUserId) {
    throw new Error("Chat recipient is not available.");
  }

  const callable = httpsCallable<
    GetOrCreateConversationPayload,
    GetOrCreateConversationResult
  >(services.functions, "getOrCreateConversation");

  const response = await callable({
    otherUserId: normalizedUserId,
    ...(jobId?.trim() ? { jobId: jobId.trim() } : {})
  });

  const conversationId = response.data?.conversationId?.trim();
  if (!conversationId) {
    throw new Error("Conversation id is missing from the chat service response.");
  }

  return conversationId;
}

export function productConversationRoute(role: ProductRole, conversationId: string): string {
  const basePath =
    role === "WORKER" ? "/app/worker/messages" : "/app/employer/messages";

  return `${basePath}?conversation=${encodeURIComponent(conversationId)}`;
}
