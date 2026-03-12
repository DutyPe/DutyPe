"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import {
  collection,
  deleteDoc,
  doc,
  onSnapshot,
  orderBy,
  query,
  updateDoc,
  where,
  writeBatch
} from "firebase/firestore";
import { httpsCallable } from "firebase/functions";

import { formatDateTime } from "@/lib/firebase/firestore-helpers";
import { getFirebaseServices } from "@/lib/firebase/client";
import {
  conversationPeer,
  isNotificationRelevant,
  normalizeConversation,
  normalizeMessage,
  normalizeNotification,
  notificationTypeLabel,
  sortByTimestampAsc,
  sortByTimestampDesc,
  unreadConversationCount,
  type ProductConversation,
  type ProductMessage,
  type ProductNotification
} from "@/lib/product/communication";
import { productRoleLabel, type ProductRole } from "@/lib/product/profile";

import type { ProductSession } from "./use-product-session";

type SharedProps = {
  session: ProductSession;
};

type RoleProps = SharedProps & {
  role: ProductRole;
};

function notificationRoute(role: ProductRole, notification: ProductNotification) {
  if (notification.type === "CHAT_MESSAGE") {
    return {
      href:
        role === "WORKER"
          ? `/app/worker/messages${notification.conversationId ? `?conversation=${encodeURIComponent(notification.conversationId)}` : ""}`
          : `/app/employer/messages${notification.conversationId ? `?conversation=${encodeURIComponent(notification.conversationId)}` : ""}`,
      label: "Open messages"
    };
  }

  if (
    role === "WORKER" &&
    ["APPLICATION_STATUS", "APPLICATION_STATUS_UPDATE", "INTERVIEW_SCHEDULED", "WORKER_HIRED"].includes(
      notification.type
    )
  ) {
    return {
      href: "/app/worker/my-jobs",
      label: "Track application"
    };
  }

  if (role === "WORKER" && notification.type === "PROFILE_COMPLETE") {
    return {
      href: "/app/worker/profile",
      label: "Complete profile"
    };
  }

  if (
    role === "EMPLOYER" &&
    ["NEW_APPLICATION", "WORKER_HIRED"].includes(notification.type)
  ) {
    return {
      href: "/app/employer/applications",
      label: "Review applications"
    };
  }

  if (
    role === "EMPLOYER" &&
    ["JOB_POSTED", "JOB_PAUSED"].includes(notification.type)
  ) {
    return {
      href: "/app/employer/jobs",
      label: "Manage jobs"
    };
  }

  return {
    href: role === "WORKER" ? "/app/worker" : "/app/employer",
    label: `Open ${productRoleLabel(role).toLowerCase()} dashboard`
  };
}

function MessageThread({
  currentUserId,
  messages
}: {
  currentUserId: string;
  messages: ProductMessage[];
}) {
  if (messages.length === 0) {
    return (
      <div className="empty-state">
        No messages yet. When the first message lands, this thread will update in real time.
      </div>
    );
  }

  return (
    <div className="message-thread">
      {messages.map((message) => {
        const mine = message.senderId === currentUserId;

        return (
          <article
            key={message.id}
            className={`message-bubble ${mine ? "mine" : "theirs"}`}
          >
            <p>{message.message || "Message content unavailable."}</p>
            <div className="message-stamp-row">
              <span className="message-stamp">
                {mine ? "Sent" : "Received"} {formatDateTime(message.createdAt)}
              </span>
              <span className="message-stamp">
                {message.type === "TEXT" ? "Text" : notificationTypeLabel(message.type)}
              </span>
            </div>
          </article>
        );
      })}
    </div>
  );
}

function NotificationCard({
  busy,
  notification,
  onDelete,
  onMarkRead,
  role
}: {
  busy: boolean;
  notification: ProductNotification;
  onDelete: () => Promise<void>;
  onMarkRead: () => Promise<void>;
  role: ProductRole;
}) {
  const route = notificationRoute(role, notification);

  return (
    <article className={`card notification-card ${notification.isRead ? "is-read" : "is-unread"}`}>
      <div className="notification-card-head">
        <div>
          <span className="card-kicker">{notificationTypeLabel(notification.type)}</span>
          <h3>{notification.title}</h3>
        </div>
        <span className={`status-pill ${notification.isRead ? "neutral" : "warning"}`}>
          {notification.isRead ? "Read" : "Unread"}
        </span>
      </div>

      <p className="market-card-copy">{notification.message || "DutyPe generated an update for this account."}</p>

      <div className="notification-meta-grid">
        <div className="market-meta-item">
          <span>Arrived</span>
          <strong>{formatDateTime(notification.createdAt)}</strong>
        </div>
        <div className="market-meta-item">
          <span>Type</span>
          <strong>{notificationTypeLabel(notification.type)}</strong>
        </div>
      </div>

      <div className="pill-row">
        {notification.conversationId ? <span className="pill">Conversation linked</span> : null}
        {notification.applicationId || notification.data.applicationId ? (
          <span className="pill">Application update</span>
        ) : null}
        {notification.jobId || notification.data.jobId ? <span className="pill">Job context</span> : null}
      </div>

      <div className="button-row compact market-card-actions">
        <Link href={route.href} className="button">
          {route.label}
        </Link>
        {!notification.isRead ? (
          <button
            type="button"
            className="button ghost"
            disabled={busy}
            onClick={() => void onMarkRead()}
          >
            {busy ? "Updating..." : "Mark read"}
          </button>
        ) : null}
        <button
          type="button"
          className="button ghost"
          disabled={busy}
          onClick={() => void onDelete()}
        >
          {busy ? "Updating..." : "Dismiss"}
        </button>
      </div>
    </article>
  );
}

function ProductMessagesClient({ role, session }: RoleProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const searchParams = useSearchParams();
  const [conversations, setConversations] = useState<ProductConversation[]>([]);
  const [messages, setMessages] = useState<ProductMessage[]>([]);
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null);
  const [draft, setDraft] = useState("");
  const [loadingConversations, setLoadingConversations] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const requestedConversationId = searchParams.get("conversation");
  const currentUserId = session.user?.uid ?? "";
  const selectedConversation =
    conversations.find((conversation) => conversation.id === selectedConversationId) ?? null;
  const unreadCount = currentUserId
    ? unreadConversationCount(conversations, currentUserId)
    : 0;
  const selectedPeer = selectedConversation && currentUserId
    ? conversationPeer(selectedConversation, currentUserId)
    : null;

  useEffect(() => {
    if (!services || !session.user) {
      setLoadingConversations(false);
      return;
    }

    setError(null);
    setLoadingConversations(true);

    const unsubscribe = onSnapshot(
      query(
        collection(services.db, "conversations"),
        where("participants", "array-contains", session.user.uid)
      ),
      (snapshot) => {
        const nextConversations = sortByTimestampDesc(
          snapshot.docs.map((item) =>
            normalizeConversation(item.id, item.data() as Record<string, unknown>)
          ),
          "updatedAt"
        );

        setConversations(nextConversations);
        setSelectedConversationId((current) => {
          if (current && nextConversations.some((conversation) => conversation.id === current)) {
            return current;
          }

          return nextConversations[0]?.id ?? null;
        });
        setLoadingConversations(false);
      },
      (snapshotError) => {
        setError(
          snapshotError instanceof Error
            ? snapshotError.message
            : "Failed to load conversations."
        );
        setLoadingConversations(false);
      }
    );

    return () => unsubscribe();
  }, [services, session.user]);

  useEffect(() => {
    if (
      requestedConversationId &&
      conversations.some((conversation) => conversation.id === requestedConversationId)
    ) {
      setSelectedConversationId(requestedConversationId);
    }
  }, [conversations, requestedConversationId]);

  useEffect(() => {
    if (!services || !selectedConversationId) {
      setMessages([]);
      setLoadingMessages(false);
      return;
    }

    setMessages([]);
    setDraft("");
    setLoadingMessages(true);

    const unsubscribe = onSnapshot(
      query(
        collection(services.db, "messages"),
        where("conversationId", "==", selectedConversationId),
        orderBy("createdAt", "asc")
      ),
      (snapshot) => {
        setMessages(
          sortByTimestampAsc(
            snapshot.docs.map((item) =>
              normalizeMessage(item.id, item.data() as Record<string, unknown>)
            ),
            "createdAt"
          )
        );
        setLoadingMessages(false);
      },
      (snapshotError) => {
        setError(
          snapshotError instanceof Error
            ? snapshotError.message
            : "Failed to load messages."
        );
        setLoadingMessages(false);
      }
    );

    return () => unsubscribe();
  }, [selectedConversationId, services]);

  useEffect(() => {
    if (!services || !session.user || !selectedConversationId || !selectedConversation) {
      return;
    }

    if ((selectedConversation.unreadCount[session.user.uid] ?? 0) === 0) {
      return;
    }

    const markRead = httpsCallable<{ conversationId: string }, { success: boolean }>(
      services.functions,
      "markMessagesAsRead"
    );

    void markRead({ conversationId: selectedConversationId }).catch((markError) => {
      setError(
        markError instanceof Error ? markError.message : "Failed to mark messages as read."
      );
    });
  }, [selectedConversation, selectedConversationId, services, session.user]);

  async function handleSendMessage() {
    if (!services || !selectedConversationId || !draft.trim()) {
      return;
    }

    try {
      setSending(true);
      setError(null);

      const sendMessage = httpsCallable<
        { conversationId: string; message: string; type: string },
        { success: boolean }
      >(services.functions, "sendChatMessage");

      await sendMessage({
        conversationId: selectedConversationId,
        message: draft.trim(),
        type: "TEXT"
      });

      setDraft("");
    } catch (sendError) {
      setError(sendError instanceof Error ? sendError.message : "Failed to send message.");
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="product-section-stack">
      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Conversations</span>
            <strong>{conversations.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>Unread messages</span>
            <strong>{unreadCount}</strong>
          </div>
          <div className="product-summary-card">
            <span>Mode</span>
            <strong>{productRoleLabel(role)}</strong>
          </div>
        </div>

        <div className="product-insight-grid">
          <article className="product-summary-card">
            <span>Realtime thread</span>
            <strong>Live Firestore + Functions</strong>
            <p>
              Conversations sync from `conversations` and `messages`, while message send and
              read receipts use the existing Cloud Functions backend.
            </p>
          </article>

          <article className="product-summary-card">
            <span>Current selection</span>
            <strong>{selectedPeer?.name ?? "No conversation selected"}</strong>
            <p>
              {selectedConversation
                ? `Unread on this thread: ${selectedConversation.unreadCount[currentUserId] ?? 0}.`
                : "Pick a conversation to open the two-column chat view."}
            </p>
          </article>
        </div>

        {error ? <div className="callout">Messages error: {error}</div> : null}
      </section>

      <section className="section communication-section">
        <div className="section-header">
          <div>
            <span className="tag">Messages</span>
            <h2>In-app conversations</h2>
          </div>
          <p>
            This mirrors the Android chat flow with a conversation rail, message thread,
            unread state, and message composer in one responsive layout.
          </p>
        </div>

        <div className="communication-shell">
          <aside className="conversation-panel">
            <div className="conversation-panel-head">
              <span className="card-kicker">Thread list</span>
              <h3>{conversations.length > 0 ? "Open a conversation" : "No conversations yet"}</h3>
              <p>
                {loadingConversations
                  ? "Loading your latest conversations."
                  : "Conversation previews update live as new messages arrive."}
              </p>
            </div>

            {loadingConversations ? (
              <div className="empty-state">Loading conversations.</div>
            ) : conversations.length === 0 ? (
              <div className="empty-state">
                No conversations have started yet for this account.
              </div>
            ) : (
              <div className="conversation-list">
                {conversations.map((conversation) => {
                  const peer = conversationPeer(conversation, currentUserId);
                  const active = conversation.id === selectedConversationId;
                  const conversationUnread = conversation.unreadCount[currentUserId] ?? 0;

                  return (
                    <button
                      key={conversation.id}
                      type="button"
                      className={`conversation-item ${active ? "active" : ""}`}
                      onClick={() => setSelectedConversationId(conversation.id)}
                    >
                      <div className="conversation-item-top">
                        <div>
                          <strong>{peer.name}</strong>
                          <span>{peer.role === "UNKNOWN" ? "DutyPe user" : peer.role}</span>
                        </div>
                        {conversationUnread > 0 ? (
                          <span className="conversation-unread">{conversationUnread}</span>
                        ) : null}
                      </div>
                      <p>
                        {conversation.lastMessage ||
                          "This conversation is ready for the first message."}
                      </p>
                      <div className="conversation-item-bottom">
                        <span>{formatDateTime(conversation.lastMessageAt)}</span>
                        {conversation.jobId ? <span>Job-linked thread</span> : <span>General thread</span>}
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </aside>

          <div className="message-panel">
            {selectedConversation ? (
              <>
                <div className="message-panel-head">
                  <div>
                    <span className="card-kicker">Live thread</span>
                    <h3>{selectedPeer?.name ?? "DutyPe contact"}</h3>
                    <p>
                      {selectedPeer?.role === "UNKNOWN"
                        ? "DutyPe conversation"
                        : `${selectedPeer?.role} conversation`}{" "}
                      {selectedConversation.jobId ? "linked to a job context." : "without a job link."}
                    </p>
                  </div>
                  <div className="pill-row">
                    <span className="pill">{messages.length} messages</span>
                    <span className="pill">
                      {(selectedConversation.unreadCount[currentUserId] ?? 0) === 0
                        ? "Read up to date"
                        : `${selectedConversation.unreadCount[currentUserId] ?? 0} unread`}
                    </span>
                  </div>
                </div>

                {loadingMessages ? (
                  <div className="empty-state">Loading thread.</div>
                ) : (
                  <MessageThread currentUserId={currentUserId} messages={messages} />
                )}

                <div className="message-composer">
                  <label className="inline-field">
                    <span>Reply in this conversation</span>
                    <textarea
                      rows={4}
                      value={draft}
                      onChange={(event) => setDraft(event.target.value)}
                      placeholder="Type a clear update, question, or next step."
                    />
                  </label>
                  <div className="button-row compact">
                    <button
                      type="button"
                      className="button"
                      disabled={sending || !draft.trim()}
                      onClick={() => void handleSendMessage()}
                    >
                      {sending ? "Sending..." : "Send message"}
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <div className="empty-state">
                Choose a conversation from the left rail to open the message thread.
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}

function ProductNotificationsClient({ role, session }: RoleProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [notifications, setNotifications] = useState<ProductNotification[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [markingAll, setMarkingAll] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!services || !session.user) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    const unsubscribe = onSnapshot(
      query(
        collection(services.db, "notifications"),
        where("recipientId", "==", session.user.uid)
      ),
      (snapshot) => {
        const nextNotifications = sortByTimestampDesc(
          snapshot.docs
            .map((item) =>
              normalizeNotification(item.id, item.data() as Record<string, unknown>)
            )
            .filter((notification) => isNotificationRelevant(notification, role)),
          "createdAt"
        );

        setNotifications(nextNotifications);
        setLoading(false);
      },
      (snapshotError) => {
        setError(
          snapshotError instanceof Error
            ? snapshotError.message
            : "Failed to load notifications."
        );
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, [role, services, session.user]);

  const unreadCount = notifications.filter((notification) => !notification.isRead).length;

  async function handleMarkRead(notificationId: string) {
    if (!services) {
      return;
    }

    try {
      setBusyId(notificationId);
      await updateDoc(doc(services.db, "notifications", notificationId), {
        isRead: true
      });
    } catch (updateError) {
      setError(
        updateError instanceof Error
          ? updateError.message
          : "Failed to mark notification as read."
      );
    } finally {
      setBusyId(null);
    }
  }

  async function handleDelete(notificationId: string) {
    if (!services) {
      return;
    }

    try {
      setBusyId(notificationId);
      await deleteDoc(doc(services.db, "notifications", notificationId));
    } catch (deleteError) {
      setError(
        deleteError instanceof Error
          ? deleteError.message
          : "Failed to delete notification."
      );
    } finally {
      setBusyId(null);
    }
  }

  async function handleMarkAllRead() {
    if (!services || unreadCount === 0) {
      return;
    }

    try {
      setMarkingAll(true);
      const batch = writeBatch(services.db);

      notifications
        .filter((notification) => !notification.isRead)
        .forEach((notification) => {
          batch.update(doc(services.db, "notifications", notification.id), {
            isRead: true
          });
        });

      await batch.commit();
    } catch (batchError) {
      setError(
        batchError instanceof Error
          ? batchError.message
          : "Failed to mark all notifications as read."
      );
    } finally {
      setMarkingAll(false);
    }
  }

  return (
    <div className="product-section-stack">
      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Total notifications</span>
            <strong>{notifications.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>Unread</span>
            <strong>{unreadCount}</strong>
          </div>
          <div className="product-summary-card">
            <span>Mode</span>
            <strong>{productRoleLabel(role)}</strong>
          </div>
        </div>

        <div className="product-insight-grid">
          <article className="product-summary-card">
            <span>Notification source</span>
            <strong>Realtime Firestore feed</strong>
            <p>
              This inbox listens to the same `notifications` collection Android uses and
              filters it by role-specific product meaning.
            </p>
          </article>

          <article className="product-summary-card">
            <span>Attention needed</span>
            <strong>{unreadCount > 0 ? "Clear the newest updates" : "Inbox is caught up"}</strong>
            <p>
              {unreadCount > 0
                ? "Review unread updates first so application status, new messages, or job changes do not get buried."
                : "Everything currently visible in this feed has already been read."}
            </p>
          </article>
        </div>

        <div className="button-row">
          <button
            type="button"
            className="button"
            disabled={markingAll || unreadCount === 0}
            onClick={() => void handleMarkAllRead()}
          >
            {markingAll ? "Updating..." : "Mark all read"}
          </button>
        </div>

        {error ? <div className="callout">Notifications error: {error}</div> : null}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Notifications</span>
            <h2>Product inbox</h2>
          </div>
          <p>
            Updates stay grouped in one responsive feed so workers and employers can move
            from a notification straight into the right app route.
          </p>
        </div>

        {loading ? (
          <div className="empty-state">Loading notification feed.</div>
        ) : notifications.length === 0 ? (
          <div className="empty-state">No notifications are available for this role yet.</div>
        ) : (
          <div className="notification-feed">
            {notifications.map((notification) => (
              <NotificationCard
                key={notification.id}
                busy={busyId === notification.id}
                notification={notification}
                onDelete={() => handleDelete(notification.id)}
                onMarkRead={() => handleMarkRead(notification.id)}
                role={role}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export function WorkerMessagesClient({ session }: SharedProps) {
  return <ProductMessagesClient role="WORKER" session={session} />;
}

export function WorkerNotificationsClient({ session }: SharedProps) {
  return <ProductNotificationsClient role="WORKER" session={session} />;
}

export function EmployerMessagesClient({ session }: SharedProps) {
  return <ProductMessagesClient role="EMPLOYER" session={session} />;
}

export function EmployerNotificationsClient({ session }: SharedProps) {
  return <ProductNotificationsClient role="EMPLOYER" session={session} />;
}
