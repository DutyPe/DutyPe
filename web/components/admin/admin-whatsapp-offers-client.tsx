"use client";

import { useEffect, useState } from "react";
import { collection, limit, onSnapshot, orderBy, query, Timestamp } from "firebase/firestore";
import { httpsCallable } from "firebase/functions";
import { getFirebaseServices } from "@/lib/firebase/client";

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

const input = { padding: 8, borderRadius: 6, border: "1px solid #d1d5db", width: "100%" } as const;
const label = { display: "flex", flexDirection: "column", gap: 4, fontSize: 13 } as const;

export function AdminWhatsappOffersClient() {
  const [template, setTemplate] = useState("");
  const [langs, setLangs] = useState("te,en");
  const [params, setParams] = useState("");
  const [audience, setAudience] = useState<Audience>("ALL");
  const [max, setMax] = useState(500);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState("");
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);

  useEffect(() => {
    const services = getFirebaseServices();
    if (!services) return;
    const q = query(collection(services.db, "whatsapp_campaigns"), orderBy("createdAt", "desc"), limit(10));
    return onSnapshot(q, (snap) => setCampaigns(snap.docs.map((d) => {
      const x = d.data();
      return {
        id: d.id,
        template: String(x.template || ""),
        audience: String(x.audience || ""),
        sent: Number(x.sent || 0),
        failed: Number(x.failed || 0),
        matched: Number(x.matched || 0),
        createdAt: x.createdAt instanceof Timestamp ? x.createdAt.toMillis() : 0
      };
    })), () => setCampaigns([]));
  }, []);

  async function run(dryRun: boolean) {
    setError("");
    if (!/^[a-z0-9_]+$/.test(template.trim())) {
      setError("Template name: small letters, numbers and _ only, exactly as approved in Meta.");
      return;
    }
    if (!dryRun && !window.confirm(`Send "${template}" on WhatsApp to up to ${result?.willSend ?? max} people? This cannot be undone.`)) return;
    setBusy(true);
    try {
      const services = getFirebaseServices();
      if (!services) throw new Error("Firebase is not configured");
      const res = await httpsCallable(services.functions, "sendWhatsappPromo")({
        template: template.trim(),
        langs,
        params: params.split("\n").map((p) => p.trim()).filter(Boolean),
        audience,
        limit: max,
        dryRun
      });
      setResult(res.data as Result);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div style={{ display: "grid", gap: 16, maxWidth: 820 }}>
      <section className="admin-card">
        <h2 style={{ margin: 0 }}>Send an offer</h2>
        <p style={{ color: "#6b7280", fontSize: 13, marginTop: 4 }}>
          Goes only to users who switched on <b>Settings → WhatsApp offers &amp; wishes</b>. Each person gets at most one
          offer every 7 days; anyone who replies STOP is switched off automatically. Always press <b>Count</b> first.
        </p>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14, marginTop: 12 }}>
          <label style={label}>
            <span>Template name (approved in Meta, category Marketing)</span>
            <input style={input} value={template} onChange={(e) => setTemplate(e.target.value)} placeholder="dasara_offer_2026" />
          </label>
          <label style={label}>
            <span>Approved languages (first = default)</span>
            <input style={input} value={langs} onChange={(e) => setLangs(e.target.value)} placeholder="te,en" />
          </label>
          <label style={label}>
            <span>Who</span>
            <select style={input} value={audience} onChange={(e) => setAudience(e.target.value as Audience)}>
              <option value="ALL">Everyone who opted in</option>
              <option value="WORKER">Workers</option>
              <option value="EMPLOYER">Employers / customers</option>
              <option value="PARTNER">Service partners</option>
            </select>
          </label>
          <label style={label}>
            <span>Maximum people this time (≤ 1000)</span>
            <input style={input} type="number" min={1} max={1000} value={max} onChange={(e) => setMax(Math.max(1, Math.min(1000, Number(e.target.value) || 1)))} />
          </label>
          <label style={{ ...label, gridColumn: "1 / -1" }}>
            <span>Template values {"{{1}}"}, {"{{2}}"}… one per line (same for everyone, e.g. the coupon code)</span>
            <textarea style={{ ...input, minHeight: 70 }} value={params} onChange={(e) => setParams(e.target.value)} placeholder={"DASARA50\n12 Oct"} />
          </label>
        </div>
        <div style={{ display: "flex", gap: 10, marginTop: 14 }}>
          <button className="button ghost" disabled={busy} onClick={() => run(true)}>Count (no send)</button>
          <button className="button" disabled={busy || !result?.dryRun || !result.willSend} onClick={() => run(false)}>
            Send on WhatsApp
          </button>
        </div>
        {error && <p style={{ color: "#dc2626", fontSize: 13 }}>{error}</p>}
        {result && (
          <div style={{ marginTop: 12, fontSize: 14, background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: 8, padding: 12 }}>
            <div>Opted in: <b>{result.matched}</b> · Got an offer in the last 7 days (skipped): <b>{result.skippedRecent}</b></div>
            <div>{result.dryRun ? "Will send to" : "Tried"}: <b>{result.willSend}</b> (≈ ₹{(result.willSend * 1.1).toFixed(0)} at ~₹1.1 per marketing message)</div>
            {result.dryRun && result.ready === false && <div style={{ color: "#dc2626" }}>WhatsApp is not set up on the server yet.</div>}
            {!result.dryRun && <div>Sent: <b>{result.sent}</b> · Failed: <b>{result.failed}</b></div>}
            {result.errors?.length ? <pre style={{ whiteSpace: "pre-wrap", fontSize: 12, color: "#b91c1c" }}>{result.errors.join("\n")}</pre> : null}
          </div>
        )}
      </section>

      <section className="admin-card">
        <h3 style={{ marginTop: 0 }}>Rules that keep our WhatsApp number safe</h3>
        <ul style={{ fontSize: 13, lineHeight: 1.7, color: "#374151", paddingLeft: 18 }}>
          <li>Offers only to opted-in users; never buy or upload number lists.</li>
          <li>Add Meta&apos;s <b>&quot;Stop promotions&quot;</b> quick-reply button to every marketing template.</li>
          <li>One message per person per week at most; festivals and real offers only.</li>
          <li>Urgent work and vacancies are never sent on WhatsApp — workers get free push notifications. Service bookings get an automatic WhatsApp confirmation (utility).</li>
          <li>The login codes use the same number — if Quality goes Red in WhatsApp Manager, stop offers for a week.</li>
          <li>Meta&apos;s starting limit is 1,000 people a day for messages we start (codes + confirmations + offers).</li>
        </ul>
      </section>

      <section className="admin-card">
        <h3 style={{ marginTop: 0 }}>Recent sends</h3>
        {campaigns.length === 0 ? <p style={{ color: "#6b7280", fontSize: 13 }}>None yet.</p> : (
          <table style={{ width: "100%", fontSize: 13, borderCollapse: "collapse" }}>
            <thead><tr style={{ textAlign: "left" }}><th>When</th><th>Template</th><th>Who</th><th>Opted in</th><th>Sent</th><th>Failed</th></tr></thead>
            <tbody>
              {campaigns.map((c) => (
                <tr key={c.id} style={{ borderTop: "1px solid #e5e7eb" }}>
                  <td>{c.createdAt ? new Date(c.createdAt).toLocaleString("en-IN") : "—"}</td>
                  <td>{c.template}</td><td>{c.audience}</td><td>{c.matched}</td><td>{c.sent}</td><td>{c.failed}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
