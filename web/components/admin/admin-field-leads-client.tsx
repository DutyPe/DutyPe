"use client";

import { useEffect, useMemo, useState } from "react";
import { collection, doc, limit, onSnapshot, orderBy, query, setDoc, Timestamp, updateDoc } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import { SITE_URL } from "@/lib/public-site";

/**
 * Umbrella-desk field registration (functions/src/field-leads.ts):
 *  - Agents: create a code; the agent opens dutype.in/join?agent=CODE on their phone.
 *  - Leads: everyone they registered; JOINED is set automatically when that phone signs up.
 *  - Pay agents per JOINED person (fake / duplicate forms never become JOINED).
 */

type Row = Record<string, unknown> & { id: string };
const millis = (v: unknown) => (v instanceof Timestamp ? v.toMillis() : 0);
const istDay = (ms = Date.now()) => new Date(ms + 330 * 60 * 1000).toISOString().slice(0, 10);
const STATUSES = ["NEW", "CALLED", "JOINED", "NOT_INTERESTED"];
const ROLE_LABEL: Record<string, string> = { WORKER: "Worker", PARTNER: "Service partner", EMPLOYER: "Employer", CUSTOMER: "Customer" };

function useLive(path: string, order: string, max = 500): Row[] {
  const services = useMemo(() => getFirebaseServices(), []);
  const [rows, setRows] = useState<Row[]>([]);
  useEffect(() => {
    if (!services) return;
    return onSnapshot(query(collection(services.db, path), orderBy(order, "desc"), limit(max)),
      (snap) => setRows(snap.docs.map((d) => ({ id: d.id, ...d.data() }))), () => setRows([]));
  }, [services, path, order, max]);
  return rows;
}

export function AdminFieldLeadsClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const agents = useLive("field_agents", "createdAt", 100);
  const leads = useLive("field_leads", "createdAt", 1000);
  const [agentFilter, setAgentFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [payoutRate, setPayoutRate] = useState(50);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);
  const [form, setForm] = useState({ code: "", name: "", phone: "" });
  const [msg, setMsg] = useState<string | null>(null);

  const today = istDay();
  const shown = leads.filter((l) => {
    if (agentFilter && l.agentCode !== agentFilter) return false;
    if (statusFilter && l.status !== statusFilter) return false;
    if (roleFilter && l.role !== roleFilter) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      const name = String(l.name || "").toLowerCase();
      const phone = String(l.phone || "").toLowerCase();
      const area = String(l.area || "").toLowerCase();
      if (!name.includes(q) && !phone.includes(q) && !area.includes(q)) return false;
    }
    return true;
  });

  const totalJoinedAllAgents = agents.reduce((sum, a) => sum + Number(a.joined || 0), 0);
  const totalLeadsAllAgents = agents.reduce((sum, a) => sum + Number(a.leads || 0), 0);
  const totalPayoutAllAgents = totalJoinedAllAgents * payoutRate;

  async function addAgent() {
    if (!services) return;
    const code = form.code.trim().toUpperCase();
    if (!/^[A-Z0-9]{3,20}$/.test(code) || form.name.trim().length < 2) {
      setMsg("Code: 3–20 letters/digits (e.g. RAVI01). Name is required.");
      return;
    }
    if (agents.some((a) => a.id === code)) {
      setMsg(`${code} already exists`);
      return;
    }
    await setDoc(doc(services.db, "field_agents", code), {
      name: form.name.trim(), phone: form.phone.trim(), active: true, leads: 0, joined: 0, daily: {}, createdAt: Timestamp.now(),
    });
    setForm({ code: "", name: "", phone: "" });
    setMsg(`Agent ${code} added. Their link: ${SITE_URL}/join?agent=${code}`);
  }

  async function setActive(a: Row, active: boolean) {
    if (!services) return;
    await updateDoc(doc(services.db, "field_agents", a.id), { active });
  }

  async function setStatus(l: Row, status: string) {
    if (!services) return;
    await updateDoc(doc(services.db, "field_leads", l.id), { status, updatedAt: Timestamp.now() });
  }

  function copyAgentLink(code: string) {
    const link = `${SITE_URL}/join?agent=${code}`;
    navigator.clipboard.writeText(link);
    setCopiedCode(code);
    setTimeout(() => setCopiedCode(null), 2500);
  }

  function exportCsv() {
    const head = ["createdAt", "agentCode", "role", "name", "phone", "area", "skills", "status", "note"];
    const lines = shown.map((l) => [
      new Date(millis(l.createdAt)).toISOString(), l.agentCode, l.role, l.name, l.phone, l.area,
      ((l.skills || []) as string[]).join(" "), l.status, l.note,
    ].map((v) => `"${String(v ?? "").replace(/"/g, '""')}"`).join(","));
    const blob = new Blob([[head.join(","), ...lines].join("\n")], { type: "text/csv" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = `field-leads-${today}.csv`;
    a.click();
  }

  return (
    <div>
      {msg && <div className="alert-banner" style={{ marginBottom: 12 }}>{msg}</div>}

      {/* Summary KPI Cards */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 12, marginBottom: 16 }}>
        <div style={{ background: "#fff", padding: "14px 18px", borderRadius: 10, border: "1px solid #e2e8f0" }}>
          <div style={{ fontSize: 12, color: "#64748b", fontWeight: 600 }}>Active Agents</div>
          <div style={{ fontSize: 22, fontWeight: 700, color: "#0f172a", marginTop: 4 }}>
            {agents.filter((a) => a.active).length} / {agents.length}
          </div>
        </div>
        <div style={{ background: "#fff", padding: "14px 18px", borderRadius: 10, border: "1px solid #e2e8f0" }}>
          <div style={{ fontSize: 12, color: "#64748b", fontWeight: 600 }}>Total Leads</div>
          <div style={{ fontSize: 22, fontWeight: 700, color: "#0f172a", marginTop: 4 }}>
            {totalLeadsAllAgents}
          </div>
        </div>
        <div style={{ background: "#fff", padding: "14px 18px", borderRadius: 10, border: "1px solid #e2e8f0" }}>
          <div style={{ fontSize: 12, color: "#16a34a", fontWeight: 600 }}>Verified Joined</div>
          <div style={{ fontSize: 22, fontWeight: 700, color: "#16a34a", marginTop: 4 }}>
            {totalJoinedAllAgents}
          </div>
        </div>
        <div style={{ background: "#fff", padding: "14px 18px", borderRadius: 10, border: "1px solid #e2e8f0" }}>
          <div style={{ fontSize: 12, color: "#0284c7", fontWeight: 600 }}>Total Agent Payout</div>
          <div style={{ fontSize: 22, fontWeight: 700, color: "#0284c7", marginTop: 4 }}>
            ₹{totalPayoutAllAgents.toLocaleString("en-IN")}
          </div>
        </div>
      </div>

      <section className="admin-section">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 10 }}>
          <h2 className="admin-section-title" style={{ margin: 0 }}>Field agents &amp; Payouts</h2>
          <div style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13 }}>
            <span style={{ color: "#475569", fontWeight: 600 }}>Payout per Joined Lead:</span>
            <span>₹</span>
            <input
              type="number"
              value={payoutRate}
              onChange={(e) => setPayoutRate(Math.max(0, Number(e.target.value)))}
              style={{ width: 70, padding: "4px 8px", borderRadius: 6, border: "1px solid #cbd5e1" }}
            />
          </div>
        </div>
        <p style={{ marginTop: 8 }}>
          Create a code for each person at an umbrella desk. They register people at <b>{SITE_URL}/join?agent=CODE</b> and help them
          install the app. Pay per <b>Joined</b> (that phone number signed up in the app).
        </p>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 12 }}>
          <input placeholder="Code (e.g. RAVI01)" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} />
          <input placeholder="Agent name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <input placeholder="Agent phone" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          <button className="btn btn-approve" onClick={() => void addAgent()}>Add agent</button>
        </div>
        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Agent</th>
                <th>Today</th>
                <th>Total Registered</th>
                <th>Joined (Verified)</th>
                <th>Join Rate</th>
                <th>Calculated Payout</th>
                <th>Link</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {agents.map((a) => {
                const total = Number(a.leads || 0);
                const joined = Number(a.joined || 0);
                const daily = (a.daily || {}) as Record<string, number>;
                const payout = joined * payoutRate;
                return (
                  <tr key={a.id}>
                    <td><b>{a.id}</b>{a.active ? "" : " (off)"}</td>
                    <td>{String(a.name || "")}<div style={{ fontSize: 12, color: "#64748b" }}>{String(a.phone || "")}</div></td>
                    <td>{daily[today] || 0}</td>
                    <td>{total}</td>
                    <td><b style={{ color: "#16a34a" }}>{joined}</b></td>
                    <td>{total ? Math.round((joined / total) * 100) + "%" : "–"}</td>
                    <td><b style={{ color: "#0284c7" }}>₹{payout.toLocaleString("en-IN")}</b></td>
                    <td>
                      <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
                        <a href={`/join?agent=${a.id}`} target="_blank" rel="noreferrer" style={{ fontSize: 12 }}>Form</a>
                        <button
                          className="btn"
                          style={{ padding: "2px 8px", fontSize: 11 }}
                          onClick={() => copyAgentLink(a.id)}
                        >
                          {copiedCode === a.id ? "Copied!" : "Copy Link"}
                        </button>
                      </div>
                    </td>
                    <td>
                      <button className="btn" onClick={() => void setActive(a, !a.active)}>{a.active ? "Turn off" : "Turn on"}</button>
                    </td>
                  </tr>
                );
              })}
              {agents.length === 0 && <tr><td colSpan={9}>No agents yet.</td></tr>}
            </tbody>
          </table>
        </div>
      </section>

      <section className="admin-section">
        <h2 className="admin-section-title">Registrations ({shown.length})</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 12, alignItems: "center" }}>
          <input
            type="search"
            placeholder="Search by name, phone, or area..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ minWidth: 240, padding: "6px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
          />
          <select value={agentFilter} onChange={(e) => setAgentFilter(e.target.value)}>
            <option value="">All agents</option>
            {agents.map((a) => <option key={a.id} value={a.id}>{a.id}</option>)}
          </select>
          <select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
            <option value="">Everyone</option>
            {Object.entries(ROLE_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">Any status</option>
            {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
          <button className="btn" onClick={exportCsv}>Export CSV</button>
        </div>
        <div className="admin-table-container">
          <table className="admin-table">
            <thead><tr><th>When</th><th>Who</th><th>Phone</th><th>Skills / Area</th><th>Agent</th><th>Status</th></tr></thead>
            <tbody>
              {shown.map((l) => (
                <tr key={l.id}>
                  <td>{new Date(millis(l.createdAt)).toLocaleString("en-IN")}</td>
                  <td><b>{String(l.name || "")}</b><div style={{ fontSize: 12, color: "#64748b" }}>{ROLE_LABEL[String(l.role)] || String(l.role)}</div></td>
                  <td><a href={`tel:${String(l.phone || "")}`}>{String(l.phone || "")}</a></td>
                  <td>{((l.skills || []) as string[]).join(", ")}<div style={{ fontSize: 12, color: "#64748b" }}>{String(l.area || "")}</div>
                    {l.note ? <div style={{ fontSize: 12, opacity: 0.7 }}>{String(l.note)}</div> : null}</td>
                  <td><b>{String(l.agentCode || "")}</b></td>
                  <td>
                    {l.status === "JOINED" ? <b style={{ color: "#16a34a", background: "#dcfce7", padding: "3px 8px", borderRadius: 4, fontSize: 12 }}>JOINED</b> : (
                      <select value={String(l.status || "NEW")} onChange={(e) => void setStatus(l, e.target.value)}>
                        {STATUSES.filter((s) => s !== "JOINED").map((s) => <option key={s} value={s}>{s}</option>)}
                      </select>
                    )}
                  </td>
                </tr>
              ))}
              {shown.length === 0 && <tr><td colSpan={6}>No registrations yet matching filters.</td></tr>}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
