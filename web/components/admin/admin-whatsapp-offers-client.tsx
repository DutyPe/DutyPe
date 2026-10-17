"use client";

import { useEffect, useMemo, useState } from "react";
import { collection, limit, onSnapshot, orderBy, query, Timestamp } from "firebase/firestore";
import { httpsCallable } from "firebase/functions";
import { getFirebaseServices } from "@/lib/firebase/client";
import {
  MessageCircle,
  Send,
  Calculator,
  ShieldCheck,
  AlertTriangle,
  CheckCircle2,
  Users,
  Smartphone,
  Sparkles,
  RefreshCw,
  Clock,
  Layers,
  Info,
  DollarSign
} from "lucide-react";

type Audience = "ALL" | "WORKER" | "EMPLOYER" | "PARTNER";

type Result = {
  dryRun: boolean;
  matched: number;
  due: number;
  willSend: number;
  skippedRecent: number;
  ready?: boolean;
  sent?: number;
  failed?: number;
  errors?: string[];
};

type Campaign = {
  id: string;
  template: string;
  audience: string;
  sent: number;
  failed: number;
  matched: number;
  createdAt: number;
};

export function AdminWhatsappOffersClient() {
  const [template, setTemplate] = useState("dasara_festival_special_2026");
  const [langs, setLangs] = useState("te,en");
  const [params, setParams] = useState("FESTIVAL50\nValid till Sunday");
  const [audience, setAudience] = useState<Audience>("ALL");
  const [max, setMax] = useState(500);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState("");
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);

  useEffect(() => {
    const services = getFirebaseServices();
    if (!services) return;
    const q = query(collection(services.db, "whatsapp_campaigns"), orderBy("createdAt", "desc"), limit(15));
    return onSnapshot(
      q,
      (snap) =>
        setCampaigns(
          snap.docs.map((d) => {
            const x = d.data();
            return {
              id: d.id,
              template: String(x.template || ""),
              audience: String(x.audience || ""),
              sent: Number(x.sent || 0),
              failed: Number(x.failed || 0),
              matched: Number(x.matched || 0),
              createdAt: x.createdAt instanceof Timestamp ? x.createdAt.toMillis() : 0,
            };
          })
        ),
      () => setCampaigns([])
    );
  }, []);

  async function run(dryRun: boolean) {
    setError("");
    const cleanTemplate = template.trim();
    if (!/^[a-z0-9_]+$/.test(cleanTemplate)) {
      setError("Template name format invalid: small letters, numbers, and underscores only (as registered in Meta).");
      return;
    }

    if (
      !dryRun &&
      !window.confirm(
        `Are you sure you want to broadcast "${cleanTemplate}" on WhatsApp to up to ${
          result?.willSend ?? max
        } users? This will consume WhatsApp Meta marketing credits and cannot be reversed.`
      )
    ) {
      return;
    }

    setBusy(true);
    try {
      const services = getFirebaseServices();
      if (!services) throw new Error("Firebase client is not configured.");
      const res = await httpsCallable(
        services.functions,
        "sendWhatsappPromo"
      )({
        template: cleanTemplate,
        langs,
        params: params
          .split("\n")
          .map((p) => p.trim())
          .filter(Boolean),
        audience,
        limit: max,
        dryRun,
      });
      setResult(res.data as Result);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Campaign trigger failed.");
    } finally {
      setBusy(false);
    }
  }

  const parsedParamLines = useMemo(() => {
    return params
      .split("\n")
      .map((p) => p.trim())
      .filter(Boolean);
  }, [params]);

  const estimatedCost = useMemo(() => {
    const count = result?.willSend ?? max;
    return (count * 1.1).toFixed(0);
  }, [result, max]);

  const audienceLabel = useMemo(() => {
    if (audience === "WORKER") return "Workers";
    if (audience === "EMPLOYER") return "Employers / Customers";
    if (audience === "PARTNER") return "Service Partners";
    return "All Opted-in Users";
  }, [audience]);

  return (
    <div className="adm-stack">
      {/* Toast Alert */}
      {error && (
        <div className="adm-card" style={{ borderLeft: "4px solid #ef4444", background: "#fef2f2", padding: "12px 18px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10, color: "#b91c1c", fontSize: "0.875rem", fontWeight: 600 }}>
            <AlertTriangle style={{ width: 18, height: 18, color: "#ef4444", flexShrink: 0 }} />
            <span>{error}</span>
          </div>
        </div>
      )}

      {/* Header Row */}
      <div className="adm-header-row">
        <div>
          <h1 className="adm-page-title">WhatsApp Offers &amp; Campaigns</h1>
          <p className="adm-page-subtitle">
            Broadcast official Meta-approved WhatsApp festival greetings, discounts, and announcements to opted-in users.
          </p>
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          <span className="adm-badge adm-badge-emerald" style={{ padding: "6px 12px", fontSize: "0.8rem" }}>
            <ShieldCheck style={{ width: 14, height: 14 }} />
            Opt-in Protected &middot; 7-Day Frequency Cap
          </span>
        </div>
      </div>

      {/* Top Metrics Grid */}
      <div className="adm-metrics-grid">
        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Target Audience</span>
            <div className="adm-metric-icon" style={{ background: "#ecfdf5", color: "#10b981" }}>
              <Users style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{audienceLabel}</div>
          <div className="adm-metric-sub">
            <span>maximum limit: {max} users</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Estimated Meta Cost</span>
            <div className="adm-metric-icon" style={{ background: "#fef3c7", color: "#b45309" }}>
              <DollarSign style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">≈ ₹{estimatedCost}</div>
          <div className="adm-metric-sub">
            <span>@ ~₹1.10 per marketing conversation</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Ready To Deliver</span>
            <div className="adm-metric-icon" style={{ background: "#eff6ff", color: "#2563eb" }}>
              <Send style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{result?.willSend ?? "—"}</div>
          <div className="adm-metric-sub">
            <span>users verified after cooldown check</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Campaigns Broadcasted</span>
            <div className="adm-metric-icon">
              <MessageCircle style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{campaigns.length}</div>
          <div className="adm-metric-sub">
            <span>recent broadcast sessions logged</span>
          </div>
        </div>
      </div>

      {/* Campaign Studio: Form + Live Phone Simulation */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))", gap: 20 }}>
        {/* Left: Studio Controls */}
        <div className="adm-card">
          <div className="adm-card-header">
            <h2 className="adm-card-title">
              <MessageCircle style={{ width: 18, height: 18, color: "#10b981" }} />
              WhatsApp Campaign Composer
            </h2>
            <span className="adm-badge adm-badge-blue">Official WhatsApp Cloud API</span>
          </div>
          <div className="adm-card-body" style={{ display: "flex", flexDirection: "column", gap: 14 }}>
            <div className="adm-form-group">
              <label className="adm-form-label">
                <span>Template Name (Registered in Meta) *</span>
                <span style={{ fontSize: "0.72rem", color: "var(--adm-text-muted)" }}>[a-z0-9_] only</span>
              </label>
              <input
                type="text"
                className="adm-input"
                placeholder="e.g. dasara_offer_2026"
                value={template}
                onChange={(e) => setTemplate(e.target.value)}
                required
              />
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
              <div className="adm-form-group">
                <label className="adm-form-label">Languages (Priority Order)</label>
                <input
                  type="text"
                  className="adm-input"
                  placeholder="te,en"
                  value={langs}
                  onChange={(e) => setLangs(e.target.value)}
                />
              </div>

              <div className="adm-form-group">
                <label className="adm-form-label">Target Audience Role</label>
                <select
                  className="adm-select"
                  value={audience}
                  onChange={(e) => setAudience(e.target.value as Audience)}
                >
                  <option value="ALL">Everyone Who Opted In</option>
                  <option value="WORKER">Workers Only</option>
                  <option value="EMPLOYER">Employers / Customers</option>
                  <option value="PARTNER">DutyPe Service Partners</option>
                </select>
              </div>
            </div>

            <div className="adm-form-group">
              <label className="adm-form-label">
                <span>Audience Batch Size (≤ 1000)</span>
                <span style={{ fontSize: "0.75rem", fontWeight: 700, color: "var(--adm-primary)" }}>{max} recipients</span>
              </label>
              <input
                type="range"
                min="10"
                max="1000"
                step="10"
                value={max}
                onChange={(e) => setMax(Number(e.target.value))}
                style={{ width: "100%", accentColor: "var(--adm-primary)" }}
              />
            </div>

            <div className="adm-form-group">
              <label className="adm-form-label">
                <span>Template Parameters (One per line)</span>
                <span style={{ fontSize: "0.72rem", color: "var(--adm-text-muted)" }}>
                  Replaces &#123;&#123;1&#125;&#125;, &#123;&#123;2&#125;&#125;…
                </span>
              </label>
              <textarea
                className="adm-textarea"
                rows={3}
                placeholder={"FESTIVAL50\nValid till Sunday"}
                value={params}
                onChange={(e) => setParams(e.target.value)}
              />
            </div>

            {/* Dry Run & Send Actions */}
            <div style={{ display: "flex", gap: 10, marginTop: 6 }}>
              <button
                type="button"
                className="adm-btn adm-btn-secondary"
                disabled={busy}
                onClick={() => run(true)}
                style={{ flex: 1 }}
              >
                <Calculator style={{ width: 15, height: 15 }} />
                Count &amp; Dry Run
              </button>
              <button
                type="button"
                className="adm-btn adm-btn-primary"
                disabled={busy || !result?.dryRun || !result.willSend}
                onClick={() => run(false)}
                style={{ flex: 1 }}
              >
                <Send style={{ width: 15, height: 15 }} />
                Send Campaign
              </button>
            </div>

            {/* Dry Run Inspection Card */}
            {result && (
              <div
                style={{
                  marginTop: 6,
                  padding: 14,
                  borderRadius: 10,
                  background: "var(--adm-surface-subtle)",
                  border: "1px solid var(--adm-border)",
                  fontSize: "0.825rem",
                  display: "flex",
                  flexDirection: "column",
                  gap: 8,
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span style={{ fontWeight: 600, color: "var(--adm-text-main)" }}>
                    {result.dryRun ? "Simulation Inspection Results" : "Broadcast Execution Summary"}
                  </span>
                  <span className={`adm-badge ${result.dryRun ? "adm-badge-blue" : "adm-badge-emerald"}`}>
                    {result.dryRun ? "Dry Run Complete" : "Dispatched"}
                  </span>
                </div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, fontSize: "0.8rem" }}>
                  <div>Total Opted-in: <strong>{result.matched}</strong></div>
                  <div>Skipped (7-day rule): <strong>{result.skippedRecent}</strong></div>
                  <div>Final Target Count: <strong style={{ color: "var(--adm-primary)" }}>{result.willSend}</strong></div>
                  <div>Estimated Cost: <strong>≈ ₹{(result.willSend * 1.1).toFixed(0)}</strong></div>
                </div>
                {!result.dryRun && (
                  <div style={{ display: "flex", gap: 14, paddingTop: 4, borderTop: "1px solid var(--adm-border)" }}>
                    <span style={{ color: "#059669", fontWeight: 600 }}>Sent: {result.sent}</span>
                    <span style={{ color: "#dc2626", fontWeight: 600 }}>Failed: {result.failed}</span>
                  </div>
                )}
                {result.errors && result.errors.length > 0 && (
                  <div style={{ color: "#b91c1c", fontSize: "0.75rem", background: "#fef2f2", padding: 8, borderRadius: 6 }}>
                    {result.errors.join("\n")}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Right: WhatsApp Phone Simulation Mockup */}
        <div className="adm-card" style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: 24, background: "#f8fafc" }}>
          <div className="adm-phone-frame">
            <div className="adm-phone-topbar">
              <div style={{ width: 32, height: 32, borderRadius: "50%", background: "#087f68", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 700, fontSize: "0.75rem", color: "#ffffff" }}>
                DP
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600, lineHeight: 1.2 }}>DutyPe Official</div>
                <div style={{ fontSize: "0.7rem", color: "#8696a0" }}>Verified Business Account</div>
              </div>
            </div>

            <div className="adm-phone-chat-bg">
              <div className="adm-wa-bubble">
                <div style={{ fontSize: "0.72rem", color: "#8696a0", marginBottom: 4, fontWeight: 600, textTransform: "uppercase" }}>
                  Marketing Template &middot; {template || "template_name"}
                </div>
                <div>
                  Namaskaram! DutyPe lo meekosam special festival offer undi.
                </div>
                {parsedParamLines.map((line, idx) => (
                  <div key={idx} style={{ marginTop: 4, padding: "3px 6px", background: "rgba(255,255,255,0.1)", borderRadius: 4, fontWeight: 600, color: "#25d366" }}>
                    &#123;&#123;{idx + 1}&#125;&#125;: {line}
                  </div>
                ))}
                <div style={{ marginTop: 6, fontSize: "0.775rem" }}>
                  Book now on the DutyPe App or visit dutype.in!
                </div>
                <div style={{ textAlign: "right", fontSize: "0.65rem", color: "#8696a0", marginTop: 4 }}>
                  10:30 AM &middot; &#10003;&#10003;
                </div>
                <button type="button" className="adm-wa-button">
                  Stop promotions
                </button>
              </div>
            </div>
          </div>
          <p style={{ fontSize: "0.75rem", color: "var(--adm-text-muted)", marginTop: 14, textAlign: "center", maxWidth: 300 }}>
            Real-time preview of the Meta Cloud message balloon delivered to user devices.
          </p>
        </div>
      </div>

      {/* Meta Guidelines & Safety Card */}
      <div className="adm-card">
        <div className="adm-card-header">
          <h2 className="adm-card-title">
            <ShieldCheck style={{ width: 18, height: 18, color: "var(--adm-primary)" }} />
            WhatsApp Official Quality &amp; Compliance Guardrails
          </h2>
        </div>
        <div className="adm-card-body" style={{ padding: 18 }}>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: 14, fontSize: "0.825rem", color: "var(--adm-text-muted)" }}>
            <div style={{ display: "flex", gap: 8 }}>
              <CheckCircle2 style={{ width: 16, height: 16, color: "#10b981", flexShrink: 0, marginTop: 2 }} />
              <div><strong>Opt-in Verification:</strong> Messages are strictly limited to users who explicitly toggled on WhatsApp alerts in app settings.</div>
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              <CheckCircle2 style={{ width: 16, height: 16, color: "#10b981", flexShrink: 0, marginTop: 2 }} />
              <div><strong>7-Day Frequency Cap:</strong> No user receives more than 1 promotional message per 7 days, avoiding message fatigue.</div>
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              <CheckCircle2 style={{ width: 16, height: 16, color: "#10b981", flexShrink: 0, marginTop: 2 }} />
              <div><strong>Instant Opt-Out (STOP):</strong> Every template includes a &quot;Stop promotions&quot; button that instantly un-subscribes them.</div>
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              <CheckCircle2 style={{ width: 16, height: 16, color: "#10b981", flexShrink: 0, marginTop: 2 }} />
              <div><strong>Daily Tier Limits:</strong> WhatsApp limits new accounts to 1,000 business-initiated messages every 24 hours.</div>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Campaigns Table */}
      <div className="adm-card">
        <div className="adm-card-header">
          <h2 className="adm-card-title">
            <Clock style={{ width: 18, height: 18, color: "var(--adm-primary)" }} />
            Recent WhatsApp Broadcast Sessions
          </h2>
          <span style={{ fontSize: "0.8rem", color: "var(--adm-text-muted)" }}>
            Last {campaigns.length} campaigns executed
          </span>
        </div>
        {campaigns.length === 0 ? (
          <div style={{ padding: "36px 20px", textAlign: "center", color: "var(--adm-text-muted)", fontSize: "0.875rem" }}>
            No WhatsApp campaigns have been dispatched yet.
          </div>
        ) : (
          <div className="adm-table-wrap" style={{ border: "none", borderRadius: 0 }}>
            <table className="adm-table">
              <thead>
                <tr>
                  <th>Executed At</th>
                  <th>Template Name</th>
                  <th>Audience Role</th>
                  <th>Target Matched</th>
                  <th>Delivered</th>
                  <th>Failed</th>
                  <th style={{ textAlign: "right" }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {campaigns.map((c) => (
                  <tr key={c.id}>
                    <td style={{ whiteSpace: "nowrap", color: "var(--adm-text-muted)" }}>
                      {c.createdAt ? new Date(c.createdAt).toLocaleString("en-IN") : "—"}
                    </td>
                    <td>
                      <code style={{ fontWeight: 600, color: "var(--adm-text-main)", background: "var(--adm-surface-subtle)", padding: "2px 6px", borderRadius: 4 }}>
                        {c.template}
                      </code>
                    </td>
                    <td>
                      <span className="adm-badge adm-badge-neutral">{c.audience}</span>
                    </td>
                    <td><strong>{c.matched}</strong></td>
                    <td><span style={{ color: "#059669", fontWeight: 600 }}>{c.sent}</span></td>
                    <td><span style={{ color: c.failed > 0 ? "#dc2626" : "var(--adm-text-subtle)", fontWeight: 600 }}>{c.failed}</span></td>
                    <td style={{ textAlign: "right" }}>
                      <span className={`adm-badge ${c.sent > 0 ? "adm-badge-emerald" : "adm-badge-rose"}`}>
                        {c.sent > 0 ? "Completed" : "Failed"}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
