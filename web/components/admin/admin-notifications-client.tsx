"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";

import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatDateTime } from "@/lib/firebase/firestore-helpers";

type NotificationPreset = "APP_UPDATE" | "MAINTENANCE" | "EMERGENCY" | "GENERAL";
type TargetRole = "ALL" | "WORKER" | "EMPLOYER";

type NotificationCampaignRow = {
  id: string;
  preset?: NotificationPreset;
  title?: string;
  message?: string;
  type?: string;
  targetRole?: TargetRole;
  sendPush?: boolean;
  recipientCount?: number;
  pushTopic?: string;
  createdAt?: unknown;
};

type NotificationFormState = {
  preset: NotificationPreset;
  title: string;
  message: string;
  targetRole: TargetRole;
  sendPush: boolean;
  deepLink: string;
};

const presetDefaults: Record<NotificationPreset, Pick<NotificationFormState, "title" | "message" | "sendPush" | "deepLink">> = {
  APP_UPDATE: {
    title: "App update available",
    message: "A new DutyPe update is available. Please update the app to continue using the latest features and fixes.",
    sendPush: true,
    deepLink: "https://play.google.com/store/apps/details?id=com.dutype.app"
  },
  MAINTENANCE: {
    title: "Maintenance notice",
    message: "DutyPe will be under maintenance for a short period. Please try again after the maintenance window.",
    sendPush: true,
    deepLink: "dutype://home"
  },
  EMERGENCY: {
    title: "Important emergency alert",
    message: "Please read this urgent DutyPe alert carefully and follow the instructions provided in the message.",
    sendPush: true,
    deepLink: "dutype://notifications"
  },
  GENERAL: {
    title: "DutyPe update",
    message: "Here is a general update from the DutyPe admin team.",
    sendPush: false,
    deepLink: "dutype://notifications"
  }
};

const presetLabels: Record<NotificationPreset, string> = {
  APP_UPDATE: "App update",
  MAINTENANCE: "Maintenance",
  EMERGENCY: "Emergency",
  GENERAL: "General notice"
};

const initialForm: NotificationFormState = {
  preset: "APP_UPDATE",
  ...presetDefaults.APP_UPDATE,
  targetRole: "ALL",
  deepLink: "https://play.google.com/store/apps/details?id=com.dutype.app"
};

function formatAudience(targetRole: TargetRole) {
  if (targetRole === "WORKER") {
    return "Workers only";
  }

  if (targetRole === "EMPLOYER") {
    return "Employers only";
  }

  return "All users";
}

export function AdminNotificationsClient() {
  const [campaigns, setCampaigns] = useState<NotificationCampaignRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<NotificationFormState>(initialForm);

  const expectedType = useMemo(() => (
    form.preset === "GENERAL" ? "GENERAL" : "SYSTEM_UPDATE"
  ), [form.preset]);

  async function loadCampaigns() {
    try {
      setLoading(true);
      const response = await adminApiFetch("/api/admin/notifications", { cache: "no-store" });
      const payload = (await response.json()) as { campaigns?: NotificationCampaignRow[]; error?: string };

      if (!response.ok) {
        throw new Error(payload.error || "Failed to load notifications.");
      }

      setCampaigns(payload.campaigns ?? []);
      setError(null);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Failed to load notifications.");
    } finally {
      setLoading(false);
    }
  }

  function applyPreset(preset: NotificationPreset) {
    const defaults = presetDefaults[preset];

    setForm((current) => ({
      ...current,
      preset,
      title: defaults.title,
      message: defaults.message,
      sendPush: defaults.sendPush,
      deepLink: defaults.deepLink
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      const response = await adminApiFetch("/api/admin/notifications", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          preset: form.preset,
          title: form.title.trim(),
          message: form.message.trim(),
          targetRole: form.targetRole,
          sendPush: form.sendPush,
          deepLink: form.deepLink.trim() || undefined
        })
      });

      const payload = (await response.json()) as { error?: string };
      if (!response.ok) {
        throw new Error(payload.error || "Failed to broadcast notification.");
      }

      await loadCampaigns();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Failed to broadcast notification.");
    } finally {
      setSubmitting(false);
    }
  }

  useEffect(() => {
    void loadCampaigns();
  }, []);

  return (
    <div className="admin-section-stack">
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Broadcast center</span>
            <h2>Send app alerts to every user or one role</h2>
          </div>
          <p>
            This creates Firestore inbox notifications for the selected audience and can also
            fan out push alerts through the existing FCM topics.
          </p>
        </div>

        <form className="editor-form" onSubmit={handleSubmit}>
          <label>
            <span>Preset</span>
            <select
              value={form.preset}
              onChange={(event) => applyPreset(event.target.value as NotificationPreset)}
            >
              {Object.entries(presetLabels).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>

          <label>
            <span>Audience</span>
            <select
              value={form.targetRole}
              onChange={(event) => setForm((current) => ({ ...current, targetRole: event.target.value as TargetRole }))}
            >
              <option value="ALL">All users</option>
              <option value="WORKER">Workers only</option>
              <option value="EMPLOYER">Employers only</option>
            </select>
          </label>

          <label>
            <span>Title</span>
            <input
              value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
              placeholder="App update available"
              required
            />
          </label>

          <label className="editor-form-wide">
            <span>Message</span>
            <textarea
              value={form.message}
              onChange={(event) => setForm((current) => ({ ...current, message: event.target.value }))}
              placeholder="Explain the alert clearly and briefly."
              rows={5}
              required
            />
          </label>

          <label>
            <span>Deep link</span>
            <input
              value={form.deepLink}
              onChange={(event) => setForm((current) => ({ ...current, deepLink: event.target.value }))}
              placeholder="dutype://home"
            />
          </label>

          <label>
            <span>Send push too</span>
            <select
              value={form.sendPush ? "yes" : "no"}
              onChange={(event) => setForm((current) => ({ ...current, sendPush: event.target.value === "yes" }))}
            >
              <option value="yes">Yes, send FCM too</option>
              <option value="no">No, inbox only</option>
            </select>
          </label>

          <div className="editor-form-actions">
            <button type="submit" className="button" disabled={submitting}>
              {submitting ? "Broadcasting..." : "Broadcast notification"}
            </button>
          </div>
        </form>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Recent broadcasts</span>
            <h2>Delivery history</h2>
          </div>
          <p>
            Recent admin notification campaigns and the audience they were sent to.
          </p>
        </div>

        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Current preset</span>
            <strong>{presetLabels[form.preset]}</strong>
          </div>
          <div className="product-summary-card">
            <span>Audience</span>
            <strong>{formatAudience(form.targetRole)}</strong>
          </div>
          <div className="product-summary-card">
            <span>Stored type</span>
            <strong>{expectedType}</strong>
          </div>
        </div>

        {loading ? <div className="empty-state">Loading notification campaigns from Firestore.</div> : null}
        {!loading && error ? <div className="empty-state">Unable to load notification campaigns. {error}</div> : null}
        {!loading && !error && campaigns.length === 0 ? (
          <div className="empty-state">No notification campaigns were sent yet.</div>
        ) : null}

        {!loading && !error && campaigns.length > 0 ? (
          <div className="admin-grid">
            {campaigns.map((campaign) => (
              <article key={campaign.id} className="admin-panel">
                <span className="card-kicker">{campaign.preset ?? campaign.type ?? "SYSTEM_UPDATE"}</span>
                <h3>{campaign.title ?? "Untitled broadcast"}</h3>
                <p>{campaign.message ?? "No message provided."}</p>
                <ul className="admin-list">
                  <li className="admin-item">
                    <strong>Audience</strong>
                    <span>{campaign.targetRole ?? "ALL"}</span>
                  </li>
                  <li className="admin-item">
                    <strong>Push</strong>
                    <span>{campaign.sendPush ? "Enabled" : "Inbox only"}</span>
                  </li>
                  <li className="admin-item">
                    <strong>Recipients</strong>
                    <span>{campaign.recipientCount ?? 0}</span>
                  </li>
                  <li className="admin-item">
                    <strong>Created</strong>
                    <span>{formatDateTime(campaign.createdAt)}</span>
                  </li>
                </ul>
              </article>
            ))}
          </div>
        ) : null}
      </section>
    </div>
  );
}