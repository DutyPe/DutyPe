"use client";

import Link from "next/link";
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

import { formatDateTime } from "@/lib/firebase/firestore-helpers";
import { getFirebaseServices } from "@/lib/firebase/client";
import {
  isNotificationRelevant,
  normalizeNotification,
  notificationTypeLabel,
  sortByTimestampDesc,
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
  if (
    role === "WORKER" &&
    [
      "APPLICATION_STATUS",
      "APPLICATION_STATUS_UPDATE",
      "INTERVIEW_SCHEDULED",
      "WORKER_HIRED"
    ].includes(notification.type)
  ) {
    return { href: "/app/worker/my-jobs", label: "Track application" };
  }

  if (role === "WORKER" && notification.type === "PROFILE_COMPLETE") {
    return { href: "/app/worker/profile", label: "Complete profile" };
  }

  if (role === "EMPLOYER" && ["NEW_APPLICATION", "WORKER_HIRED"].includes(notification.type)) {
    return { href: "/app/employer/applications", label: "Review applications" };
  }

  if (role === "EMPLOYER" && notification.type === "JOB_POSTED") {
    return { href: "/app/employer/jobs", label: "Manage jobs" };
  }

  return {
    href: role === "WORKER" ? "/app/worker" : "/app/employer",
    label: `Open ${productRoleLabel(role).toLowerCase()} dashboard`
  };
}

function NotificationsClient({ session, role }: RoleProps) {
  const services = getFirebaseServices();
  const [notifications, setNotifications] = useState<ProductNotification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!services || !session.user) {
      setLoading(false);
      return;
    }

    const ref = collection(services.db, "notifications");
    const q = query(
      ref,
      where("recipientId", "==", session.user.uid),
      orderBy("createdAt", "desc")
    );

    const unsub = onSnapshot(
      q,
      (snapshot) => {
        const items = snapshot.docs.map((entry) =>
          normalizeNotification(entry.id, entry.data() as Record<string, unknown>)
        );
        setNotifications(sortByTimestampDesc(items, "createdAt"));
        setLoading(false);
      },
      (snapshotError) => {
        setError(snapshotError.message);
        setLoading(false);
      }
    );

    return () => unsub();
  }, [services, session.user]);

  const visible = useMemo(
    () => notifications.filter((entry) => isNotificationRelevant(entry, role)),
    [notifications, role]
  );

  const unreadCount = useMemo(
    () => visible.filter((entry) => !entry.isRead).length,
    [visible]
  );

  async function handleMarkRead(notificationId: string) {
    if (!services) return;
    try {
      setBusy(true);
      await updateDoc(doc(services.db, "notifications", notificationId), {
        isRead: true,
        readAt: new Date()
      });
    } catch (markError) {
      setError(markError instanceof Error ? markError.message : "Failed to mark as read.");
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete(notificationId: string) {
    if (!services) return;
    try {
      setBusy(true);
      await deleteDoc(doc(services.db, "notifications", notificationId));
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Failed to delete.");
    } finally {
      setBusy(false);
    }
  }

  async function handleMarkAllRead() {
    if (!services || visible.length === 0) return;
    try {
      setBusy(true);
      const batch = writeBatch(services.db);
      visible
        .filter((entry) => !entry.isRead)
        .forEach((entry) => {
          batch.update(doc(services.db, "notifications", entry.id), {
            isRead: true,
            readAt: new Date()
          });
        });
      await batch.commit();
    } catch (batchError) {
      setError(batchError instanceof Error ? batchError.message : "Failed to update notifications.");
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <section className="section">
        <div className="empty-state">Loading notifications...</div>
      </section>
    );
  }

  return (
    <div className="product-section-stack">
      {error ? (
        <section className="section">
          <p className="error-message">{error}</p>
        </section>
      ) : null}

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Notifications</span>
            <h2>{productRoleLabel(role)} inbox</h2>
          </div>
          <p>
            {visible.length} total · {unreadCount} unread
          </p>
        </div>

        {visible.length === 0 ? (
          <div className="empty-state">No notifications yet.</div>
        ) : (
          <>
            <div className="button-row">
              <button
                type="button"
                className="table-action"
                onClick={() => void handleMarkAllRead()}
                disabled={busy || unreadCount === 0}
              >
                Mark all as read
              </button>
            </div>

            <ul className="card-list">
              {visible.map((notification) => {
                const route = notificationRoute(role, notification);
                return (
                  <li key={notification.id} className={notification.isRead ? "card" : "card unread"}>
                    <div className="card-header">
                      <strong>{notification.title}</strong>
                      <span className="tag muted">{notificationTypeLabel(notification.type)}</span>
                    </div>
                    <p>{notification.message}</p>
                    <div className="card-meta">
                      <span>{formatDateTime(notification.createdAt)}</span>
                    </div>
                    <div className="button-row compact">
                      <Link className="table-action" href={route.href}>
                        {route.label}
                      </Link>
                      {!notification.isRead ? (
                        <button
                          type="button"
                          className="table-action"
                          onClick={() => void handleMarkRead(notification.id)}
                          disabled={busy}
                        >
                          Mark read
                        </button>
                      ) : null}
                      <button
                        type="button"
                        className="table-action danger"
                        onClick={() => void handleDelete(notification.id)}
                        disabled={busy}
                      >
                        Delete
                      </button>
                    </div>
                  </li>
                );
              })}
            </ul>
          </>
        )}
      </section>
    </div>
  );
}

export function WorkerNotificationsClient({ session }: SharedProps) {
  return <NotificationsClient session={session} role="WORKER" />;
}

export function EmployerNotificationsClient({ session }: SharedProps) {
  return <NotificationsClient session={session} role="EMPLOYER" />;
}
