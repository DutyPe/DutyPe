"use client";

import { useEffect, useMemo, useState } from "react";

import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";

type InstantHelpMetrics = {
  totalRequests: number;
  openRequests: number;
  filledRequests: number;
  completedRequests: number;
  failedRequests: number;
  expiredRequests: number;
  responseCount: number;
  avgResponsesPerRequest: number;
  filledRate: number;
  expiredRate: number;
  avgTimeToFirstResponseMinutes: number | null;
};

type InstantRequestRow = {
  id: string;
  title: string;
  category: string;
  status: string;
  employerName: string;
  employerPhone?: string;
  perPersonPayment?: number;
  totalPayment?: number;
  durationText?: string;
  scheduleLabel?: string;
  scheduledAt?: number;
  responseCount: number;
  callCount: number;
  notifiedWorkerCount: number;
  createdAt: number;
  expiresAt: number;
  firstResponseAt: number;
  timeToFirstResponseMinutes: number | null;
};

type InstantResponseRow = {
  id: string;
  requestId: string;
  workerId: string;
  employerId: string;
  workerName: string;
  status: string;
  createdAt: number;
  updatedAt: number;
};

type InstantHelpPayload = {
  metrics: InstantHelpMetrics;
  recentRequests: InstantRequestRow[];
  recentResponses: InstantResponseRow[];
};

function formatDate(value: number) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}

function statusLabel(value: string) {
  return value.replace(/_/g, " ").replace(/^./, (match) => match.toUpperCase());
}

const emptyPayload: InstantHelpPayload = {
  metrics: {
    totalRequests: 0,
    openRequests: 0,
    filledRequests: 0,
    completedRequests: 0,
    failedRequests: 0,
    expiredRequests: 0,
    responseCount: 0,
    avgResponsesPerRequest: 0,
    filledRate: 0,
    expiredRate: 0,
    avgTimeToFirstResponseMinutes: null
  },
  recentRequests: [],
  recentResponses: []
};

export function AdminInstantHelpClient() {
  const [state, setState] = useState<{
    loading: boolean;
    error: string | null;
    payload: InstantHelpPayload;
  }>({
    loading: true,
    error: null,
    payload: emptyPayload
  });

  async function load() {
    setState((current) => ({ ...current, loading: true, error: null }));
    try {
      const response = await adminApiFetch("/api/admin/instant-help", { cache: "no-store" });
      const payload = (await response.json().catch(() => null)) as (InstantHelpPayload & { error?: string }) | null;
      if (!response.ok) {
        throw new Error(payload?.error ?? "Failed to load instant-help metrics.");
      }
      setState({ loading: false, error: null, payload: payload ?? emptyPayload });
    } catch (error) {
      setState({
        loading: false,
        error: error instanceof Error ? error.message : "Failed to load instant-help metrics.",
        payload: emptyPayload
      });
    }
  }

  useEffect(() => {
    void load();
  }, []);

  const metrics = state.payload.metrics;
  const health = useMemo(() => {
    if (metrics.totalRequests === 0) return "No urgent requests yet";
    if (metrics.filledRate >= 60) return "Healthy fill rate";
    if (metrics.expiredRate >= 30) return "Expiry needs attention";
    return "Watch response speed";
  }, [metrics.expiredRate, metrics.filledRate, metrics.totalRequests]);

  return (
    <div className="admin-section-stack">
      {state.error && (
        <div className="admin-alert error">{state.error}</div>
      )}

      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-icon" style={{ background: "#fff7ed" }}>IR</div>
          <div className="admin-stat-info">
            <strong>{metrics.totalRequests}</strong>
            <span>Total urgent requests</span>
            <small>{health}</small>
          </div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-icon" style={{ background: "#dcfce7" }}>FR</div>
          <div className="admin-stat-info">
            <strong>{metrics.filledRate}%</strong>
            <span>Filled rate</span>
            <small>{metrics.filledRequests} filled or completed</small>
          </div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-icon" style={{ background: "#dbeafe" }}>RS</div>
          <div className="admin-stat-info">
            <strong>{metrics.responseCount}</strong>
            <span>Worker responses</span>
            <small>{metrics.avgResponsesPerRequest} average per request</small>
          </div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-icon" style={{ background: "#fee2e2" }}>EX</div>
          <div className="admin-stat-info">
            <strong>{metrics.expiredRate}%</strong>
            <span>Expired rate</span>
            <small>{metrics.expiredRequests} expired</small>
          </div>
        </div>
      </div>

      <div className="admin-stats-grid small">
        <div className="admin-stat-card compact">
          <strong>{metrics.openRequests}</strong>
          <span>Open</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{metrics.completedRequests}</strong>
          <span>Completed</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{metrics.failedRequests}</strong>
          <span>Failed/cancelled</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{metrics.avgTimeToFirstResponseMinutes ?? "-"}</strong>
          <span>Avg first response min</span>
        </div>
      </div>

      <div className="admin-section">
        <div className="admin-section-header">
          <h2 className="admin-section-title">Recent urgent requests</h2>
          <button className="admin-topbar-action" type="button" onClick={() => void load()} disabled={state.loading}>
            {state.loading ? "Refreshing" : "Refresh"}
          </button>
        </div>
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Request</th>
                <th>Status</th>
                <th>Responses</th>
                <th>Notified</th>
                <th>First response</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {state.payload.recentRequests.length === 0 ? (
                <tr>
                  <td colSpan={6}>No urgent requests found.</td>
                </tr>
              ) : state.payload.recentRequests.map((request) => (
                <tr key={request.id}>
                  <td>
                    <strong>{request.title}</strong>
                    {request.perPersonPayment ? <span style={{ marginLeft: "6px", color: "#16a34a", fontWeight: 600 }}>₹{request.perPersonPayment}</span> : null}
                    <br />
                    <small>
                      {request.category || "-"} · {request.employerName || "Employer"}
                      {request.durationText ? ` · ${request.durationText}` : ""}
                      {request.scheduleLabel ? ` · 🕒 ${request.scheduleLabel}` : ""}
                    </small>
                  </td>
                  <td>{statusLabel(request.status)}</td>
                  <td>{request.responseCount}</td>
                  <td>{request.notifiedWorkerCount}</td>
                  <td>{request.timeToFirstResponseMinutes ?? "-"}</td>
                  <td>{formatDate(request.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="admin-section">
        <div className="admin-section-header">
          <h2 className="admin-section-title">Recent worker responses</h2>
        </div>
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Worker</th>
                <th>Status</th>
                <th>Request</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {state.payload.recentResponses.length === 0 ? (
                <tr>
                  <td colSpan={4}>No urgent responses found.</td>
                </tr>
              ) : state.payload.recentResponses.map((response) => (
                <tr key={response.id}>
                  <td>{response.workerName}</td>
                  <td>{statusLabel(response.status)}</td>
                  <td><code>{response.requestId}</code></td>
                  <td>{formatDate(response.updatedAt || response.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
