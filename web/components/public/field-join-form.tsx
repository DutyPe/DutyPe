"use client";

import { useEffect, useMemo, useState } from "react";
import { httpsCallable } from "firebase/functions";

import { getFirebaseServices } from "@/lib/firebase/client";
import { PLAY_STORE_URL } from "@/lib/public-site";

/**
 * Umbrella-desk registration form (dutype.in/join?agent=CODE). Used on the field agent's phone,
 * so it is one column, big buttons, Telugu first. Server: submitFieldLead (functions/src/field-leads.ts).
 */

type Role = "WORKER" | "PARTNER" | "EMPLOYER" | "CUSTOMER";

const ROLES: Array<{ id: Role; te: string; en: string; icon: string }> = [
  { id: "WORKER", te: "పని కావాలి", en: "Need a job", icon: "💼" },
  { id: "PARTNER", te: "సర్వీస్ పార్ట్నర్", en: "Service partner", icon: "🔧" },
  { id: "EMPLOYER", te: "మనిషి కావాలి", en: "Need staff", icon: "🏪" },
  { id: "CUSTOMER", te: "ఇంటి సేవ కావాలి", en: "Need a home service", icon: "🏠" },
];

const SERVICE_SKILLS: Array<[string, string]> = [
  ["CLEANING", "క్లీనింగ్ / Cleaning"], ["HOME_HELP", "ఇంటి సహాయం / Home help"], ["VEHICLE", "కార్ వాష్ / Car wash"],
  ["ELECTRICIAN", "ఎలక్ట్రీషియన్ ⚡"], ["PLUMBER", "ప్లంబర్ ⚡"], ["AC", "AC టెక్నీషియన్ ⚡"],
  ["APPLIANCE", "అప్లయెన్స్ రిపేర్ ⚡"], ["CARPENTER", "కార్పెంటర్ ⚡"], ["PAINTER", "పెయింటర్ ⚡"],
];

const JOB_SKILLS: Array<[string, string]> = [
  ["DRIVER", "డ్రైవర్ / Driver"], ["DELIVERY", "డెలివరీ / Delivery"], ["HELPER", "హెల్పర్ / Helper"],
  ["COOK", "వంట / Cook"], ["MAID", "పనిమనిషి / House help"], ["SECURITY", "సెక్యూరిటీ / Security"],
  ["SALES", "సేల్స్ / Sales"], ["PACKER", "ప్యాకర్ / Packer"], ["LOADING", "లోడింగ్ / Loading"],
  ["WAITER", "వెయిటర్ / Waiter"], ["CONSTRUCTION", "మేస్త్రి / Construction"], ["OFFICE", "ఆఫీస్ / Office"],
];

const box: React.CSSProperties = { width: "100%", padding: "14px 12px", fontSize: 17, borderRadius: 12, border: "1px solid #cbd5e1", boxSizing: "border-box" };

export function FieldJoinForm() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [agent, setAgent] = useState("");
  const [role, setRole] = useState<Role | null>(null);
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [area, setArea] = useState("");
  const [note, setNote] = useState("");
  const [skills, setSkills] = useState<string[]>([]);
  const [consent, setConsent] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState<{ name: string; duplicate: boolean; today: number } | null>(null);

  useEffect(() => {
    const code = new URLSearchParams(window.location.search).get("agent") || "";
    setAgent(code.toUpperCase());
  }, []);

  const skillList = role === "PARTNER" ? SERVICE_SKILLS : role === "WORKER" ? [...JOB_SKILLS, ...SERVICE_SKILLS.slice(0, 3)] : [];
  const digits = phone.replace(/\D/g, "").slice(-10);
  const valid = agent.length >= 3 && role && name.trim().length >= 2 && /^[6-9]\d{9}$/.test(digits) && consent &&
    (role !== "PARTNER" || skills.length > 0);

  async function submit() {
    if (!services || !valid) return;
    setBusy(true);
    setError(null);
    try {
      const res = await httpsCallable(services.functions, "submitFieldLead")({
        agentCode: agent, role, name: name.trim(), phone: digits, area: area.trim(), note: note.trim(), skills, consent,
      });
      const d = res.data as { duplicate?: boolean; agentToday?: number };
      setDone({ name: name.trim(), duplicate: !!d.duplicate, today: Number(d.agentToday || 0) });
      setRole(null); setName(""); setPhone(""); setArea(""); setNote(""); setSkills([]); setConsent(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not save. Check internet and try again.");
    } finally {
      setBusy(false);
    }
  }

  const appLink = `${PLAY_STORE_URL}&referrer=${encodeURIComponent(`utm_source=desk&utm_medium=agent&utm_campaign=${agent || "desk"}`)}`;

  return (
    <main style={{ maxWidth: 480, margin: "0 auto", padding: "16px 16px 40px", fontFamily: "system-ui, 'Noto Sans Telugu', sans-serif", background: "#fff", minHeight: "100vh" }}>
      <header style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 12 }}>
        <span style={{ width: 40, height: 40, borderRadius: 10, background: "#1d4ed8", color: "#fff", display: "grid", placeItems: "center", fontWeight: 800, fontSize: 22 }}>D</span>
        <div>
          <div style={{ fontWeight: 800, fontSize: 20 }}>DutyPe హెల్ప్ డెస్క్</div>
          <div style={{ fontSize: 13, color: "#64748b" }}>Free registration · ఉచిత రిజిస్ట్రేషన్ {agent ? `· Agent ${agent}` : ""}</div>
        </div>
      </header>

      {!agent && (
        <div style={{ background: "#fef2f2", color: "#b91c1c", padding: 12, borderRadius: 12, marginBottom: 12 }}>
          Agent code missing. Open the link from the admin panel: /join?agent=YOURCODE
        </div>
      )}

      {done && (
        <div style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", padding: 14, borderRadius: 14, marginBottom: 16 }}>
          <div style={{ fontWeight: 800, color: "#15803d", fontSize: 17 }}>
            ✓ {done.name} {done.duplicate ? "already registered — details updated" : "registered"}
          </div>
          <div style={{ fontSize: 14, marginTop: 6 }}>
            Now help them install DutyPe and log in with the <b>same number</b> → it counts as Joined.
          </div>
          <a href={appLink} style={{ display: "inline-block", marginTop: 10, background: "#16a34a", color: "#fff", padding: "10px 14px", borderRadius: 10, fontWeight: 700, textDecoration: "none" }}>
            ▶ Open DutyPe on Play Store
          </a>
          <div style={{ fontSize: 12, color: "#64748b", marginTop: 8 }}>Today by {agent}: {done.today}</div>
        </div>
      )}

      <div style={{ fontWeight: 700, margin: "8px 0" }}>ఎవరు? / Who is registering?</div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 14 }}>
        {ROLES.map((r) => (
          <button
            key={r.id}
            onClick={() => { setRole(r.id); setSkills([]); }}
            style={{
              padding: "14px 8px", borderRadius: 14, fontSize: 15, cursor: "pointer", textAlign: "center",
              border: role === r.id ? "2px solid #1d4ed8" : "1px solid #cbd5e1", background: role === r.id ? "#eff6ff" : "#fff",
            }}
          >
            <div style={{ fontSize: 24 }}>{r.icon}</div>
            <div style={{ fontWeight: 700 }}>{r.te}</div>
            <div style={{ fontSize: 12, color: "#64748b" }}>{r.en}</div>
          </button>
        ))}
      </div>

      {role && (
        <div style={{ display: "grid", gap: 12 }}>
          <input style={box} placeholder="పేరు / Name" value={name} onChange={(e) => setName(e.target.value)} />
          <input style={box} placeholder="మొబైల్ నంబర్ / Mobile (10 digits)" inputMode="numeric" value={phone} onChange={(e) => setPhone(e.target.value)} />
          <input style={box} placeholder="ఏరియా / Area (e.g. Wyra Road)" value={area} onChange={(e) => setArea(e.target.value)} />

          {skillList.length > 0 && (
            <div>
              <div style={{ fontWeight: 700, marginBottom: 6 }}>
                {role === "PARTNER" ? "ఏ పనులు చేస్తారు? (⚡ = skill check by phone)" : "ఏ పని కావాలి? / Work wanted"}
              </div>
              <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
                {skillList.map(([id, label]) => {
                  const on = skills.includes(id);
                  return (
                    <button
                      key={id}
                      onClick={() => setSkills(on ? skills.filter((s) => s !== id) : [...skills, id])}
                      style={{
                        padding: "10px 12px", borderRadius: 999, fontSize: 14, cursor: "pointer",
                        border: on ? "2px solid #16a34a" : "1px solid #cbd5e1", background: on ? "#f0fdf4" : "#fff",
                      }}
                    >
                      {on ? "✓ " : ""}{label}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          <textarea style={{ ...box, minHeight: 70 }} placeholder="Note (experience, need, timings)" value={note} onChange={(e) => setNote(e.target.value)} />

          <label style={{ display: "flex", gap: 10, alignItems: "flex-start", fontSize: 14, lineHeight: 1.4 }}>
            <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} style={{ width: 22, height: 22, marginTop: 2 }} />
            <span>
              DutyPe నుండి ఫోన్ / WhatsApp ద్వారా ఉద్యోగాలు, సేవల గురించి సమాచారం పొందడానికి నేను అంగీకరిస్తున్నాను.
              <br />
              <span style={{ color: "#64748b" }}>I agree DutyPe can call / WhatsApp me about jobs and services. (Read this aloud and tick only if they say yes.)</span>
            </span>
          </label>

          {error && <div style={{ color: "#b91c1c" }}>{error}</div>}
          <button
            disabled={!valid || busy}
            onClick={() => void submit()}
            style={{
              padding: "16px", fontSize: 18, fontWeight: 800, borderRadius: 14, border: "none", cursor: valid ? "pointer" : "default",
              background: valid && !busy ? "#1d4ed8" : "#94a3b8", color: "#fff",
            }}
          >
            {busy ? "Saving…" : "రిజిస్టర్ / Register"}
          </button>
          <div style={{ fontSize: 12, color: "#64748b" }}>
            Registration is free. Never pay anyone for a job. · రిజిస్ట్రేషన్ ఉచితం. ఉద్యోగం కోసం ఎవరికీ డబ్బు ఇవ్వకండి.
          </div>
        </div>
      )}
    </main>
  );
}
