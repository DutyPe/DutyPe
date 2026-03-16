"use client";

import { useEffect, useMemo, useState } from "react";
import {
  collection,
  doc,
  getDocs,
  limit,
  orderBy,
  query,
  serverTimestamp,
  updateDoc,
  writeBatch
} from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import {
  normalizeApplicationRecord,
  normalizeUserRecord,
  type NormalizedApplication,
  type NormalizedUser
} from "@/lib/firebase/admin-normalizers";
import { formatDate } from "@/lib/firebase/firestore-helpers";

type ApplicationRow = {
  id: string;
  workerId?: string;
  workerName?: string;
  workerPhone?: string;
  workerEmail?: string;
  applicantName?: string;
  jobTitle?: string;
  status?: string;
  appliedAt?: unknown;
};

export function AdminApplicationsClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [applications, setApplications] = useState<NormalizedApplication[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  useEffect(() => {
    async function loadApplications() {
      if (!services) {
        setError("Firebase is not configured.");
        setLoading(false);
        return;
      }

      try {
        const snapshot = await getDocs(
          query(collection(services.db, "job_applications"), orderBy("appliedAt", "desc"), limit(100))
        );

        const rawApplications = snapshot.docs.map((item) => ({
          id: item.id,
          ...(item.data() as Omit<ApplicationRow, "id">)
        }));

        const workerIds = [...new Set(rawApplications.map((entry) => entry.workerId).filter(Boolean) as string[])];

        const userById = new Map<string, NormalizedUser>();

        if (workerIds.length > 0) {
          const usersSnapshot = await getDocs(query(collection(services.db, "users"), limit(1000)));

          usersSnapshot.docs.forEach((userDoc) => {
            if (!workerIds.includes(userDoc.id)) {
              return;
            }

            const raw = userDoc.data() as Record<string, unknown>;
            userById.set(userDoc.id, normalizeUserRecord(userDoc.id, raw));
          });
        }

        const normalizedApplications = rawApplications.map((entry) =>
          normalizeApplicationRecord(entry.id, entry as Record<string, unknown>, entry.workerId ? userById.get(entry.workerId) : undefined)
        );

        const backfillBatch = writeBatch(services.db);
        let backfillCount = 0;

        rawApplications.forEach((entry, index) => {
          const normalized = normalizedApplications[index];
          const updates: Record<string, unknown> = {};

          if ((!entry.workerName || !entry.workerName.trim()) && normalized.workerName) {
            updates.workerName = normalized.workerName;
          }

          if ((!entry.workerPhone || !entry.workerPhone.trim()) && normalized.workerPhone) {
            updates.workerPhone = normalized.workerPhone;
          }

          if ((!entry.workerEmail || !entry.workerEmail.trim()) && normalized.workerEmail) {
            updates.workerEmail = normalized.workerEmail;
          }

          if (Object.keys(updates).length > 0) {
            backfillBatch.set(doc(services.db, "job_applications", entry.id), updates, { merge: true });
            backfillCount += 1;
          }
        });

        if (backfillCount > 0) {
          await backfillBatch.commit();
        }

        setApplications(normalizedApplications);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Failed to load applications.");
      } finally {
        setLoading(false);
      }
    }

    void loadApplications();
  }, [services]);

  if (loading) {
    return <div className="empty-state">Loading applications from Firestore.</div>;
  }

  if (error) {
    return <div className="empty-state">Unable to load applications. {error}</div>;
  }

  if (applications.length === 0) {
    return <div className="empty-state">No applications were returned from Firestore.</div>;
  }

  async function handleStatusChange(applicationId: string, status: string) {
    if (!services || updatingId) {
      return;
    }

    setUpdatingId(applicationId);
    setError(null);

    try {
      await updateDoc(doc(services.db, "job_applications", applicationId), {
        status,
        updatedAt: serverTimestamp()
      });

      setApplications((current) =>
        current.map((item) =>
          item.id === applicationId
            ? {
                ...item,
                status
              }
            : item
        )
      );
    } catch (updateError) {
      setError(updateError instanceof Error ? updateError.message : "Failed to update status.");
    } finally {
      setUpdatingId(null);
    }
  }

  return (
    <>
      {error ? <div className="admin-error">{error}</div> : null}

      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>Worker</th>
              <th>Phone</th>
              <th>Job</th>
              <th>Status</th>
              <th>Applied</th>
            </tr>
          </thead>
          <tbody>
            {applications.map((application) => (
              <tr key={application.id}>
                <td>{application.workerName || shortId(application.workerId)}</td>
                <td>{application.workerPhone || shortId(application.workerId)}</td>
                <td>{application.jobTitle || "Untitled job"}</td>
                <td>
                  <div className="admin-status-cell">
                    <span className={`status-pill ${statusTone(application.status)}`}>
                      {application.status || "PENDING"}
                    </span>
                    <select
                      className="admin-inline-select"
                      value={application.status || "PENDING"}
                      onChange={(event) => {
                        void handleStatusChange(application.id, event.target.value);
                      }}
                      disabled={updatingId === application.id}
                    >
                      {APPLICATION_STATUSES.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </div>
                </td>
                <td>{formatDate(application.appliedAt) !== "N/A" ? formatDate(application.appliedAt) : "Recently"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

const APPLICATION_STATUSES = ["PENDING", "SHORTLISTED", "ACCEPTED", "REJECTED", "WITHDRAWN"];

function statusTone(status: string | undefined) {
  switch (status) {
    case "ACCEPTED":
      return "success";
    case "REJECTED":
      return "danger";
    case "PENDING":
      return "warning";
    default:
      return "neutral";
  }
}

function shortId(value: string | undefined) {
  if (!value) {
    return "N/A";
  }

  return value.length > 12 ? `${value.slice(0, 8)}...` : value;
}
