"use client";

import { useEffect, useState } from "react";

type CallFeedbackRow = {
  id: string;
  jobId?: string;
  workerId?: string;
  jobTitle?: string;
  companyName?: string;
  spokeWithEmployer?: boolean;
  jobAvailability?: string;
  jobOfferAccepted?: boolean;
  createdAt?: { toDate?: () => Date } | string | number;
};

function formatDate(value: CallFeedbackRow["createdAt"]) {
  if (
    value &&
    typeof value === "object" &&
    "toDate" in value &&
    typeof value.toDate === "function"
  ) {
    return value.toDate().toLocaleString();
  }
  if (typeof value === "string" || typeof value === "number") {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? "—" : date.toLocaleString();
  }
  return "—";
}

function yesNo(value?: boolean) {
  if (value === true) return "Yes";
  if (value === false) return "No";
  return "—";
}

export function RecentCallFeedback() {
  const [rows, setRows] = useState<CallFeedbackRow[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        setIsLoading(true);
        const response = await fetch("/api/admin/call-feedback");
        if (response.ok) {
          const data = await response.json();
          setRows(Array.isArray(data.callFeedback) ? data.callFeedback : []);
        }
      } catch (error) {
        console.error("Failed to fetch recent call feedback", error);
        setRows([]);
      } finally {
        setIsLoading(false);
      }
    }
    load();
  }, []);

  return (
    <section className="marketing-panel marketing-panel-spaced">
      <div className="marketing-panel-header">
        <h2>Recent call sessions</h2>
        <p>Latest call taps and worker feedback stored in Firestore.</p>
      </div>
      {isLoading ? (
        <div className="marketing-panel-message">Loading call sessions…</div>
      ) : rows.length === 0 ? (
        <div className="marketing-panel-message">No call sessions yet.</div>
      ) : (
        <div className="marketing-table-scroll">
          <table className="marketing-data-table">
            <thead>
              <tr>
                <th>Job</th>
                <th>Worker</th>
                <th>Spoke</th>
                <th>Availability</th>
                <th>Hired</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td>
                    <strong>{row.jobTitle || row.jobId || "—"}</strong>
                    <div className="marketing-data-subtext">
                      {row.companyName || "—"}
                    </div>
                  </td>
                  <td>{row.workerId || "—"}</td>
                  <td>{yesNo(row.spokeWithEmployer)}</td>
                  <td>{row.jobAvailability || "—"}</td>
                  <td>{yesNo(row.jobOfferAccepted)}</td>
                  <td>{formatDate(row.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
