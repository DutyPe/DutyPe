"use client";

import { useState } from "react";
import {
  Target,
  TrendingUp,
  DollarSign,
  Users,
  MapPin,
  CheckSquare,
  Square,
  ChevronDown,
  ChevronRight,
  Wrench,
  Megaphone,
  Search,
  ShieldCheck,
  AlertTriangle,
  Lightbulb,
  BookOpen,
  Rocket,
  BarChart3,
  Clock,
  Star,
  Zap,
  Heart,
  Copy,
  Check,
  Store,
  Building,
  PhoneCall,
  MessageSquare,
  ShieldAlert,
  HelpCircle
} from "lucide-react";

// ─── Types ───────────────────────────────────────────────────────────────────
type ChecklistItem = { id: string; text: string; done: boolean };
type Section = {
  id: string;
  title: string;
  icon: React.ElementType;
  color: string;
  bg: string;
  content: React.ReactNode;
};

// ─── Persistent checklist hook (localStorage) ────────────────────────────────
function useChecklist(key: string, defaults: { id: string; text: string }[]) {
  const stored = typeof window !== "undefined" ? localStorage.getItem(key) : null;
  const initial: ChecklistItem[] = stored
    ? JSON.parse(stored)
    : defaults.map((d) => ({ ...d, done: false }));

  const [items, setItems] = useState<ChecklistItem[]>(initial);

  function toggle(id: string) {
    setItems((prev) => {
      const next = prev.map((item) =>
        item.id === id ? { ...item, done: !item.done } : item
      );
      if (typeof window !== "undefined") localStorage.setItem(key, JSON.stringify(next));
      return next;
    });
  }

  const done = items.filter((i) => i.done).length;
  return { items, toggle, done, total: items.length };
}

// ─── Checklist Component ──────────────────────────────────────────────────────
function Checklist({ storageKey, defaults }: { storageKey: string; defaults: { id: string; text: string }[] }) {
  const { items, toggle, done, total } = useChecklist(storageKey, defaults);
  const pct = Math.round((done / total) * 100);

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "12px" }}>
        <div style={{ flex: 1, background: "#e2e8f0", borderRadius: "999px", height: "6px" }}>
          <div
            style={{
              width: `${pct}%`,
              height: "100%",
              borderRadius: "999px",
              background: "linear-gradient(90deg, #087f68, #10b981)",
              transition: "width 0.3s ease"
            }}
          />
        </div>
        <span style={{ fontSize: "12px", fontWeight: 700, color: "#087f68", whiteSpace: "nowrap" }}>
          {done}/{total} done ({pct}%)
        </span>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
        {items.map((item) => (
          <label
            key={item.id}
            style={{
              display: "flex",
              alignItems: "flex-start",
              gap: "10px",
              cursor: "pointer",
              padding: "10px 12px",
              borderRadius: "10px",
              background: item.done ? "#f0fdf4" : "#f8fafc",
              border: `1px solid ${item.done ? "#86efac" : "#e2e8f0"}`,
              transition: "all 0.15s ease"
            }}
            onClick={() => toggle(item.id)}
          >
            {item.done ? (
              <CheckSquare size={16} style={{ color: "#087f68", flexShrink: 0, marginTop: "1px" }} />
            ) : (
              <Square size={16} style={{ color: "#94a3b8", flexShrink: 0, marginTop: "1px" }} />
            )}
            <span
              style={{
                fontSize: "13.5px",
                color: item.done ? "#64748b" : "#1e293b",
                textDecoration: item.done ? "line-through" : "none",
                lineHeight: "1.5"
              }}
            >
              {item.text}
            </span>
          </label>
        ))}
      </div>
    </div>
  );
}

// ─── Metric Card ─────────────────────────────────────────────────────────────
function MetricCard({
  label,
  formula,
  target,
  why,
  color
}: {
  label: string;
  formula: string;
  target: string;
  why: string;
  color: string;
}) {
  return (
    <div
      style={{
        background: "#fff",
        border: "1px solid #e2e8f0",
        borderRadius: "12px",
        padding: "16px",
        borderLeft: `4px solid ${color}`
      }}
    >
      <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "6px" }}>{label}</div>
      <div
        style={{
          fontFamily: "monospace",
          fontSize: "12px",
          background: "#f1f5f9",
          padding: "6px 10px",
          borderRadius: "6px",
          color: "#334155",
          marginBottom: "8px"
        }}
      >
        {formula}
      </div>
      <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "8px" }}>
        <span
          style={{
            background: "#fef3c7",
            color: "#92400e",
            padding: "2px 8px",
            borderRadius: "999px",
            fontSize: "11px",
            fontWeight: 600
          }}
        >
          🎯 Target: {target}
        </span>
      </div>
      <div style={{ fontSize: "12.5px", color: "#64748b", lineHeight: "1.5" }}>{why}</div>
    </div>
  );
}

// ─── Info Row ─────────────────────────────────────────────────────────────────
function InfoRow({ icon: Icon, label, value, color }: { icon: React.ElementType; label: string; value: string; color: string }) {
  return (
    <div style={{ display: "flex", alignItems: "flex-start", gap: "12px", padding: "10px 0", borderBottom: "1px solid #f1f5f9" }}>
      <div
        style={{
          width: "32px",
          height: "32px",
          borderRadius: "8px",
          background: `${color}18`,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          flexShrink: 0
        }}
      >
        <Icon size={15} style={{ color }} />
      </div>
      <div>
        <div style={{ fontSize: "11px", color: "#94a3b8", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.5px" }}>{label}</div>
        <div style={{ fontSize: "13.5px", color: "#1e293b", marginTop: "2px", lineHeight: "1.5" }}>{value}</div>
      </div>
    </div>
  );
}

// ─── Collapsible Section ──────────────────────────────────────────────────────
function PlaybookSection({
  title,
  icon: Icon,
  color,
  bg,
  children,
  defaultOpen = false
}: {
  title: string;
  icon: React.ElementType;
  color: string;
  bg: string;
  children: React.ReactNode;
  defaultOpen?: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <div
      style={{
        background: "#fff",
        border: "1px solid #e2e8f0",
        borderRadius: "16px",
        overflow: "hidden",
        marginBottom: "16px"
      }}
    >
      <button
        onClick={() => setOpen(!open)}
        style={{
          width: "100%",
          display: "flex",
          alignItems: "center",
          gap: "14px",
          padding: "18px 20px",
          background: open ? "#f8fafc" : "#fff",
          border: "none",
          cursor: "pointer",
          textAlign: "left",
          borderBottom: open ? "1px solid #e2e8f0" : "none"
        }}
      >
        <div
          style={{
            width: "38px",
            height: "38px",
            borderRadius: "10px",
            background: bg,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            flexShrink: 0
          }}
        >
          <Icon size={19} style={{ color }} />
        </div>
        <span style={{ fontWeight: 700, fontSize: "15px", color: "#0f172a", flex: 1 }}>{title}</span>
        {open ? (
          <ChevronDown size={18} style={{ color: "#94a3b8" }} />
        ) : (
          <ChevronRight size={18} style={{ color: "#94a3b8" }} />
        )}
      </button>
      {open && <div style={{ padding: "20px" }}>{children}</div>}
    </div>
  );
}

// ─── Missing Feature Card ─────────────────────────────────────────────────────
function FeatureCard({
  priority,
  title,
  description,
  impact,
  effort
}: {
  priority: "P0" | "P1" | "P2";
  title: string;
  description: string;
  impact: string;
  effort: string;
}) {
  const colors: Record<string, { bg: string; color: string; label: string }> = {
    P0: { bg: "#fee2e2", color: "#dc2626", label: "Critical — Do Now" },
    P1: { bg: "#fef3c7", color: "#d97706", label: "High — This Week" },
    P2: { bg: "#e0f2fe", color: "#0284c7", label: "Medium — Next Sprint" }
  };
  const c = colors[priority];

  return (
    <div
      style={{
        background: "#fff",
        border: "1px solid #e2e8f0",
        borderRadius: "12px",
        padding: "16px",
        borderLeft: `4px solid ${c.color}`
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "8px" }}>
        <span
          style={{
            background: c.bg,
            color: c.color,
            padding: "2px 8px",
            borderRadius: "999px",
            fontSize: "10.5px",
            fontWeight: 700
          }}
        >
          {priority} · {c.label}
        </span>
      </div>
      <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "6px" }}>{title}</div>
      <div style={{ fontSize: "13px", color: "#475569", lineHeight: "1.55", marginBottom: "10px" }}>{description}</div>
      <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
        <span style={{ background: "#f0fdf4", color: "#166534", padding: "2px 8px", borderRadius: "6px", fontSize: "11px", fontWeight: 600 }}>
          💥 Impact: {impact}
        </span>
        <span style={{ background: "#f1f5f9", color: "#475569", padding: "2px 8px", borderRadius: "6px", fontSize: "11px", fontWeight: 600 }}>
          ⚙️ Effort: {effort}
        </span>
      </div>
    </div>
  );
}

// ─── Copyable Script Box ──────────────────────────────────────────────────────
function CopyableScriptBox({
  title,
  teluguText,
  englishText,
  tip
}: {
  title: string;
  teluguText: string;
  englishText: string;
  tip?: string;
}) {
  const [copied, setCopied] = useState<"te" | "en" | null>(null);

  function copy(text: string, lang: "te" | "en") {
    if (typeof navigator !== "undefined" && navigator.clipboard) {
      navigator.clipboard.writeText(text);
      setCopied(lang);
      setTimeout(() => setCopied(null), 2000);
    }
  }

  return (
    <div
      style={{
        background: "#f8fafc",
        border: "1px solid #e2e8f0",
        borderRadius: "12px",
        padding: "16px",
        marginBottom: "14px"
      }}
    >
      <div
        style={{
          fontWeight: 700,
          fontSize: "14px",
          color: "#0f172a",
          marginBottom: "10px",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center"
        }}
      >
        <span>{title}</span>
      </div>

      {/* Telugu Script */}
      <div
        style={{
          background: "#f0fdf4",
          border: "1px solid #bbf7d0",
          borderRadius: "8px",
          padding: "12px",
          marginBottom: "10px"
        }}
      >
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "6px"
          }}
        >
          <span
            style={{
              fontSize: "11px",
              fontWeight: 700,
              color: "#166534",
              textTransform: "uppercase",
              letterSpacing: "0.5px"
            }}
          >
            🗣️ Telugu Pitch (Direct &amp; Natural)
          </span>
          <button
            type="button"
            onClick={() => copy(teluguText, "te")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "4px",
              fontSize: "11.5px",
              fontWeight: 600,
              background: "#dcfce7",
              color: "#166534",
              border: "none",
              borderRadius: "6px",
              padding: "4px 8px",
              cursor: "pointer"
            }}
          >
            {copied === "te" ? <Check size={12} /> : <Copy size={12} />}
            {copied === "te" ? "Copied!" : "Copy Telugu"}
          </button>
        </div>
        <p
          style={{
            fontSize: "13.5px",
            color: "#14532d",
            margin: 0,
            lineHeight: "1.65",
            whiteSpace: "pre-wrap"
          }}
        >
          {teluguText}
        </p>
      </div>

      {/* English Script */}
      <div
        style={{
          background: "#eff6ff",
          border: "1px solid #bfdbfe",
          borderRadius: "8px",
          padding: "12px"
        }}
      >
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "6px"
          }}
        >
          <span
            style={{
              fontSize: "11px",
              fontWeight: 700,
              color: "#1e40af",
              textTransform: "uppercase",
              letterSpacing: "0.5px"
            }}
          >
            🗣️ English Translation
          </span>
          <button
            type="button"
            onClick={() => copy(englishText, "en")}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "4px",
              fontSize: "11.5px",
              fontWeight: 600,
              background: "#dbeafe",
              color: "#1e40af",
              border: "none",
              borderRadius: "6px",
              padding: "4px 8px",
              cursor: "pointer"
            }}
          >
            {copied === "en" ? <Check size={12} /> : <Copy size={12} />}
            {copied === "en" ? "Copied!" : "Copy English"}
          </button>
        </div>
        <p
          style={{
            fontSize: "13px",
            color: "#1e3a8a",
            margin: 0,
            lineHeight: "1.65",
            whiteSpace: "pre-wrap"
          }}
        >
          {englishText}
        </p>
      </div>

      {tip && (
        <div
          style={{
            marginTop: "10px",
            fontSize: "12px",
            color: "#64748b",
            display: "flex",
            alignItems: "flex-start",
            gap: "6px"
          }}
        >
          <Lightbulb size={14} style={{ color: "#eab308", flexShrink: 0, marginTop: "2px" }} />
          <span>
            <strong>Founder Pro-Tip:</strong> {tip}
          </span>
        </div>
      )}
    </div>
  );
}

// ─── Objection Q&A Card ───────────────────────────────────────────────────────
function ObjectionQACard({
  question,
  teluguQuestion,
  psychology,
  exactAnswerTelugu,
  exactAnswerEnglish,
  doNotSay
}: {
  question: string;
  teluguQuestion: string;
  psychology: string;
  exactAnswerTelugu: string;
  exactAnswerEnglish: string;
  doNotSay: string;
}) {
  const [open, setOpen] = useState(false);

  return (
    <div
      style={{
        background: "#fff",
        border: "1px solid #e2e8f0",
        borderRadius: "10px",
        marginBottom: "10px",
        overflow: "hidden"
      }}
    >
      <button
        type="button"
        onClick={() => setOpen(!open)}
        style={{
          width: "100%",
          padding: "14px 16px",
          display: "flex",
          alignItems: "center",
          gap: "10px",
          background: open ? "#f8fafc" : "#fff",
          border: "none",
          cursor: "pointer",
          textAlign: "left"
        }}
      >
        <span style={{ fontSize: "16px", flexShrink: 0 }}>❓</span>
        <div style={{ flex: 1 }}>
          <div style={{ fontWeight: 700, fontSize: "13.5px", color: "#0f172a" }}>{question}</div>
          <div style={{ fontSize: "12px", color: "#64748b", marginTop: "2px" }}>{teluguQuestion}</div>
        </div>
        {open ? <ChevronDown size={16} color="#64748b" /> : <ChevronRight size={16} color="#64748b" />}
      </button>

      {open && (
        <div style={{ padding: "14px 16px", borderTop: "1px solid #e2e8f0", background: "#fcfdfe" }}>
          {/* Psychology / Root Doubt */}
          <div
            style={{
              background: "#fef3c7",
              borderRadius: "6px",
              padding: "8px 12px",
              fontSize: "12px",
              color: "#78350f",
              marginBottom: "12px",
              lineHeight: "1.5"
            }}
          >
            🧠 <strong>Why they ask this (Root Fear):</strong> {psychology}
          </div>

          {/* Telugu Exact Answer */}
          <div
            style={{
              background: "#f0fdf4",
              border: "1px solid #bbf7d0",
              borderRadius: "8px",
              padding: "10px 12px",
              marginBottom: "10px"
            }}
          >
            <div style={{ fontSize: "11px", fontWeight: 700, color: "#166534", marginBottom: "4px" }}>
              🗣️ Telugu Reply (Speak this naturally with confidence):
            </div>
            <div style={{ fontSize: "13px", color: "#14532d", lineHeight: "1.6" }}>
              {exactAnswerTelugu}
            </div>
          </div>

          {/* English Exact Answer */}
          <div
            style={{
              background: "#eff6ff",
              border: "1px solid #bfdbfe",
              borderRadius: "8px",
              padding: "10px 12px",
              marginBottom: "10px"
            }}
          >
            <div style={{ fontSize: "11px", fontWeight: 700, color: "#1e40af", marginBottom: "4px" }}>
              🗣️ English Meaning &amp; Rationale:
            </div>
            <div style={{ fontSize: "12.5px", color: "#1e3a8a", lineHeight: "1.6" }}>
              {exactAnswerEnglish}
            </div>
          </div>

          {/* Fatal Mistake / What NEVER to say */}
          <div
            style={{
              fontSize: "12px",
              color: "#b91c1c",
              background: "#fee2e2",
              padding: "6px 10px",
              borderRadius: "6px",
              lineHeight: "1.5"
            }}
          >
            ⛔ <strong>NEVER say this (Dealbreaker):</strong> {doNotSay}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Main Component ───────────────────────────────────────────────────────────
export function FounderPlaybookClient() {
  return (
    <div style={{ maxWidth: "860px" }}>
      {/* Header */}
      <div
        style={{
          background: "linear-gradient(135deg, #0f172a 0%, #1e3a8a 100%)",
          borderRadius: "16px",
          padding: "28px 24px",
          marginBottom: "24px",
          color: "#fff"
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "12px", marginBottom: "10px" }}>
          <BookOpen size={28} style={{ color: "#93c5fd" }} />
          <div>
            <h1 style={{ fontSize: "22px", fontWeight: 800, margin: 0 }}>DutyPe Founder Playbook</h1>
            <p style={{ fontSize: "13px", color: "#93c5fd", margin: "4px 0 0" }}>
              Khammam Launch · Business Fundamentals · Missing Features Tracker
            </p>
          </div>
        </div>
        <div
          style={{
            background: "rgba(255,255,255,0.08)",
            borderRadius: "10px",
            padding: "12px 16px",
            fontSize: "13px",
            color: "#e2e8f0",
            lineHeight: "1.6",
            marginTop: "12px"
          }}
        >
          💡 <strong>Remember this every day:</strong> Khammam has NO service app. You are the first.
          That means you have <strong>zero competition</strong> locally but also <strong>zero existing demand</strong> — you must create it.
          Density before expansion. Execution before marketing.
        </div>
      </div>

      {/* ─── SECTION 0: The First 72 Hours — Exact Step-by-Step Action Plan ─── */}
      <PlaybookSection
        title="🔥 Step 1: What to Do RIGHT NOW (Your First 72-Hour Khammam Blueprint)"
        icon={Zap}
        color="#dc2626"
        bg="#fee2e2"
        defaultOpen={true}
      >
        <div style={{ fontSize: "13.5px", color: "#334155", lineHeight: "1.7", marginBottom: "18px" }}>
          Don&apos;t get stuck in analysis paralysis or writing code. Your code is already built and deployed.
          Here is your <strong>hour-by-hour operational blueprint for the next 72 hours</strong>. Follow this in exact order:
        </div>

        {/* 72-Hour Action Cards */}
        <div style={{ display: "grid", gap: "12px", marginBottom: "20px" }}>
          <div style={{ background: "#fef2f2", border: "1px solid #fecaca", borderRadius: "10px", padding: "14px" }}>
            <div style={{ fontWeight: 700, fontSize: "14px", color: "#991b1b", marginBottom: "4px" }}>
              ⚡ DAY 1: Lock in Supply (Morning 9:00 AM – 2:00 PM)
            </div>
            <div style={{ fontSize: "13px", color: "#7f1d1d", lineHeight: "1.6" }}>
              1. Walk down <strong>Wyra Road</strong> and visit 3 prominent electrical and sanitary hardware stores.<br />
              2. Meet the shop owner: <em>&quot;Anna, I run DutyPe in Khammam. I am giving ₹100 for every verified electrician/plumber who joins. Can you give me numbers of your 5 best guys?&quot;</em><br />
              3. Collect 10 phone numbers (5 electricians, 5 plumbers).
            </div>
          </div>

          <div style={{ background: "#eff6ff", border: "1px solid #bfdbfe", borderRadius: "10px", padding: "14px" }}>
            <div style={{ fontWeight: 700, fontSize: "14px", color: "#1e40af", marginBottom: "4px" }}>
              ⚡ DAY 1: In-Person Onboarding (Afternoon 2:30 PM – 6:30 PM)
            </div>
            <div style={{ fontSize: "13px", color: "#1e3a8a", lineHeight: "1.6" }}>
              1. Call each technician and meet them at their local tea point or shop.<br />
              2. Pitch using the <strong>60-Second Telugu Script</strong> below: <em>Zero fees, keep your private jobs, extra cash in free hours.</em><br />
              3. Personally install the DutyPe Partner app on their phones. Show them the big <em>Accept</em> and <em>Start</em> buttons.<br />
              4. Approve them in your Admin Panel on the spot! Target: <strong>6 to 8 active partners on Day 1</strong>.
            </div>
          </div>

          <div style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "10px", padding: "14px" }}>
            <div style={{ fontWeight: 700, fontSize: "14px", color: "#166534", marginBottom: "4px" }}>
              ⚡ DAY 2: Test the Machine (Morning 10:00 AM – 1:00 PM)
            </div>
            <div style={{ fontSize: "13px", color: "#14532d", lineHeight: "1.6" }}>
              1. Do a <strong>live test booking</strong> yourself from your house or office.<br />
              2. Check if the nearest partner&apos;s phone rings with the loud offer sound.<br />
              3. Have them tap <em>Accept</em>, test the 4-digit Start OTP, and verify that the whole flow is flawless.
            </div>
          </div>

          <div style={{ background: "#fffbeb", border: "1px solid #fde68a", borderRadius: "10px", padding: "14px" }}>
            <div style={{ fontWeight: 700, fontSize: "14px", color: "#92400e", marginBottom: "4px" }}>
              ⚡ DAY 2 &amp; 3: Spark Demand in ONE Colony (Balaji Nagar / Wyra Rd)
            </div>
            <div style={{ fontSize: "13px", color: "#78350f", lineHeight: "1.6" }}>
              1. Print 100 A5 flyers (black &amp; white is fine): <em>&quot;DutyPe: Doorstep Electrician &amp; Plumber in 59 mins in Khammam. First booking free.&quot;</em><br />
              2. Tie up with 2 apartment security guards (give ₹50 mobile recharge to distribute to 20 flats).<br />
              3. Post the <strong>Telugu WhatsApp Broadcast Template</strong> in 3 local colony WhatsApp groups.
            </div>
          </div>

          <div style={{ background: "#faf5ff", border: "1px solid #e9d5ff", borderRadius: "10px", padding: "14px" }}>
            <div style={{ fontWeight: 700, fontSize: "14px", color: "#6b21a8", marginBottom: "4px" }}>
              ⚡ FIRST 10 BOOKINGS: Founder Hand-Holding Rule
            </div>
            <div style={{ fontSize: "13px", color: "#581c87", lineHeight: "1.6" }}>
              When the first 10 bookings happen, <strong>call the customer immediately</strong>: <em>&quot;Hi sir/madam, this is Vamsi from DutyPe Khammam. Our verified partner Raju is arriving in 20 minutes.&quot;</em><br />
              Accompany the technician if possible. Take a photo, make sure the work is 100% perfect, and ask for a 5-star Google review.
            </div>
          </div>
        </div>

        {/* 72-Hour Interactive Checklist */}
        <div style={{ marginTop: "14px" }}>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "10px" }}>
            ✅ Your 72-Hour Execution Checklist (Tick off as you complete):
          </div>
          <Checklist
            storageKey="playbook_72hours_blueprint"
            defaults={[
              { id: "h72_1", text: "Visit 3 hardware shops on Wyra Road, Khammam and pitch the ₹100 partner referral" },
              { id: "h72_2", text: "Collect phone numbers of at least 8 tradesmen (electricians + plumbers + AC tech)" },
              { id: "h72_3", text: "Meet 5 technicians in person at their tea stalls and install DutyPe Partner app" },
              { id: "h72_4", text: "Approve all 5 partners in Admin Panel under 'Partners' tab" },
              { id: "h72_5", text: "Perform 1 complete live test booking with Start OTP to verify partner sound alert" },
              { id: "h72_6", text: "Print 100 A5 flyers with app QR code for Balaji Nagar / Wyra Rd apartments" },
              { id: "h72_7", text: "Give security guards at 2 apartment buildings flyers to distribute to residents" },
              { id: "h72_8", text: "Post the Telugu WhatsApp template on WhatsApp status and in 2 colony groups" },
              { id: "h72_9", text: "Personally oversee the first 3 customer bookings to ensure 100% 5-star satisfaction" }
            ]}
          />
        </div>
      </PlaybookSection>

      {/* ─── SECTION 1: Khammam Launch from Scratch ─── */}
      <PlaybookSection
        title="🗺️ How to Launch in Khammam From Scratch"
        icon={MapPin}
        color="#087f68"
        bg="#e8f5ef"
        defaultOpen={true}
      >
        <div style={{ fontSize: "13.5px", color: "#334155", lineHeight: "1.7", marginBottom: "20px" }}>
          Khammam has no competitor. That&apos;s your massive advantage. But a blank market means you also need to create awareness.
          The strategy is <strong>Supply → Demand → Density</strong> in one small area before expanding.
        </div>

        <div style={{ marginBottom: "20px" }}>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "12px" }}>
            📍 Phase 1: Pick One Colony (Week 1–2)
          </div>
          <Checklist
            storageKey="playbook_phase1"
            defaults={[
              { id: "p1_1", text: "Pick ONE colony or area to start — Wyra Rd, Balaji Nagar, or Kothagudem area" },
              { id: "p1_2", text: "Never start with all of Khammam — density in one area beats thin coverage everywhere" },
              { id: "p1_3", text: "Walk that colony, note the type of apartments (independent houses vs flats)" },
              { id: "p1_4", text: "Find and join the local WhatsApp colony group or Facebook group" },
              { id: "p1_5", text: "Talk to 10 people — ask what home service they last struggled to find" }
            ]}
          />
        </div>

        <div style={{ marginBottom: "20px" }}>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "12px" }}>
            🔧 Phase 2: Recruit Workers FIRST (Week 2–3)
          </div>
          <Checklist
            storageKey="playbook_phase2"
            defaults={[
              { id: "p2_1", text: "Find 3 electricians in Khammam — visit local hardware shops, ask owners for referrals" },
              { id: "p2_2", text: "Find 3 plumbers — visit plumber supply shops on main road" },
              { id: "p2_3", text: "Find 2 AC service guys — ask in Khammam's electronics market area" },
              { id: "p2_4", text: "Find 2 home cleaners — women's SHG groups, local NGOs, domestic help agencies" },
              { id: "p2_5", text: "Install the app with them in person. Verify their ID, show them how booking works" },
              { id: "p2_6", text: "Set realistic expectations — first 2 weeks may have 0 bookings, that's okay" },
              { id: "p2_7", text: "Promise them: if DutyPe sends the booking, they get 100% of the agreed price" },
              { id: "p2_8", text: "Offer to do their first 3 bookings with them (accompany or call to verify)" }
            ]}
          />
        </div>

        <div style={{ marginBottom: "20px" }}>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "12px" }}>
            📣 Phase 3: Create First Demand (Week 3–4)
          </div>
          <Checklist
            storageKey="playbook_phase3"
            defaults={[
              { id: "p3_1", text: "Post in colony WhatsApp group: 'DutyPe – Book home services in Khammam in 59 mins'" },
              { id: "p3_2", text: "Print 50 A5 flyers with QR code linking to app download — distribute in that colony only" },
              { id: "p3_3", text: "Offer first 10 customers FREE booking fee (you absorb the ₹29-49 fee yourself)" },
              { id: "p3_4", text: "Ask every first customer to share a WhatsApp status after the service is done" },
              { id: "p3_5", text: "Post before/after service photos on Instagram with #Khammam tag" },
              { id: "p3_6", text: "Do NOT run paid ads yet — word of mouth first, ads waste money at this scale" }
            ]}
          />
        </div>

        <div>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "12px" }}>
            📈 Phase 4: Expand Only When Ready (Week 5+)
          </div>
          <div
            style={{
              background: "#fef9c3",
              border: "1px solid #fde68a",
              borderRadius: "10px",
              padding: "14px",
              fontSize: "13px",
              color: "#78350f",
              lineHeight: "1.6",
              marginBottom: "12px"
            }}
          >
            ⚠️ <strong>Only expand to the next colony when you reach 3+ bookings per day in the current one</strong>.
            Expanding too early = workers sit idle = workers leave = you lose supply = you can&apos;t serve new customers = death spiral.
          </div>
          <Checklist
            storageKey="playbook_phase4"
            defaults={[
              { id: "p4_1", text: "Reach 3 bookings/day in colony 1 before touching colony 2" },
              { id: "p4_2", text: "Add workers in colony 2 before marketing there" },
              { id: "p4_3", text: "Track fill rate — if a customer books and no worker accepts within 30 mins, that's a crisis" },
              { id: "p4_4", text: "After 10 successful bookings, ask every customer for a Google Play review" },
              { id: "p4_5", text: "Start one paid WhatsApp ad (₹200-500/day) only after you have 5+ verified workers" }
            ]}
          />
        </div>
      </PlaybookSection>

      {/* ─── SECTION 2: How to Find Workers ─── */}
      <PlaybookSection
        title="🔍 How to Find & Onboard Service Workers"
        icon={Search}
        color="#7c3aed"
        bg="#f3e8ff"
      >
        <div style={{ display: "grid", gap: "12px" }}>
          <InfoRow
            icon={MapPin}
            color="#7c3aed"
            label="Electricians & Plumbers"
            value="Go to local hardware shops on Wyra Road or Balaji Nagar main market. Ask the shop owner — they know all the local tradesmen. Offer ₹100 referral to the shop owner for each worker who joins."
          />
          <InfoRow
            icon={Wrench}
            color="#dc2626"
            label="AC Service Technicians"
            value="Visit Samsung, LG, Voltas, Daikin service centres in Khammam. Ask technicians who work independently (not just company employees). Local electronics shops on BSNL Road area."
          />
          <InfoRow
            icon={Heart}
            color="#db2777"
            label="Home Cleaners"
            value="Contact Khammam district women's SHGs (Self Help Groups). Visit AP/TS social welfare offices. Talk to apartment complex guards — they often know domestic help networks."
          />
          <InfoRow
            icon={Star}
            color="#d97706"
            label="Carpenters & Painters"
            value="Construction material shops, timber yards near bus stand, Pragathi Nagar industrial area. Also check OLX Khammam listings for 'carpenter' — many post there looking for work."
          />
          <InfoRow
            icon={Zap}
            color="#0284c7"
            label="Car/Bike Wash"
            value="Talk to people washing vehicles near petrol bunks, apartment parking lots. Many operate informally and want steady work."
          />
          <InfoRow
            icon={Lightbulb}
            color="#087f68"
            label="The Magic Question to Ask Workers"
            value='"How much do you earn per month right now? DutyPe can bring you consistent bookings without you waiting. You keep 80% of every booking directly."'
          />
        </div>
      </PlaybookSection>

      {/* ─── SECTION 2.1: Complete Worker / Partner Pitch & Objection Battlecard ─── */}
      <PlaybookSection
        title="👷 Worker / Partner Pitch &amp; Objection Battlecard (Telugu &amp; English)"
        icon={Users}
        color="#087f68"
        bg="#e8f5ef"
        defaultOpen={true}
      >
        <div style={{ fontSize: "13.5px", color: "#334155", lineHeight: "1.7", marginBottom: "20px" }}>
          Workers in Khammam are skeptical of apps. They fear hidden fees, losing their private customers, or getting exploited.
          Your pitch must focus on <strong>pure extra income during their idle hours</strong> with zero risk and zero fees.
        </div>

        {/* What Workers Get - 5 Core Value Pillars */}
        <div style={{ marginBottom: "24px" }}>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "12px" }}>
            💎 What the Worker Gets (Their Exact Benefits)
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: "12px" }}>
            <div style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "10px", padding: "14px" }}>
              <div style={{ fontWeight: 700, fontSize: "13.5px", color: "#166534", marginBottom: "4px" }}>
                💰 Zero Joining Fee &amp; Extra Cash
              </div>
              <div style={{ fontSize: "12.5px", color: "#14532d", lineHeight: "1.5" }}>
                100% free registration. No deposit, no monthly rental. Keeps 80–100% of the job price. First jobs have zero commission.
              </div>
            </div>

            <div style={{ background: "#eff6ff", border: "1px solid #bfdbfe", borderRadius: "10px", padding: "14px" }}>
              <div style={{ fontWeight: 700, fontSize: "13.5px", color: "#1e40af", marginBottom: "4px" }}>
                🕒 100% Flexible Schedule
              </div>
              <div style={{ fontSize: "12.5px", color: "#1e3a8a", lineHeight: "1.5" }}>
                Never leave existing private customers! Turn the app ON only when idle (e.g. 2 PM to 5 PM or weekends). Turn OFF anytime.
              </div>
            </div>

            <div style={{ background: "#fef3c7", border: "1px solid #fde68a", borderRadius: "10px", padding: "14px" }}>
              <div style={{ fontWeight: 700, fontSize: "13.5px", color: "#92400e", marginBottom: "4px" }}>
                🛡️ No Price Bargaining &amp; Instant Pay
              </div>
              <div style={{ fontSize: "12.5px", color: "#78350f", lineHeight: "1.5" }}>
                Standard fixed price set in app. Customer agrees upfront and gives Start OTP. Direct payment via PhonePe/GPay or cash right upon completion.
              </div>
            </div>

            <div style={{ background: "#fce7f3", border: "1px solid #fbcfe8", borderRadius: "10px", padding: "14px" }}>
              <div style={{ fontWeight: 700, fontSize: "13.5px", color: "#9d174d", marginBottom: "4px" }}>
                📍 Hyperlocal (Near Khammam Home)
              </div>
              <div style={{ fontSize: "12.5px", color: "#831843", lineHeight: "1.5" }}>
                Orders dispatched only within 3–4 km of their location. Zero petrol waste traveling to distant villages.
              </div>
            </div>

            <div style={{ background: "#f3e8ff", border: "1px solid #e9d5ff", borderRadius: "10px", padding: "14px" }}>
              <div style={{ fontWeight: 700, fontSize: "13.5px", color: "#6b21a8", marginBottom: "4px" }}>
                ⭐ Respect &amp; DutyPe Partner Identity
              </div>
              <div style={{ fontSize: "12.5px", color: "#581c87", lineHeight: "1.5" }}>
                Digital Verified Partner card, ratings, and company recognition. Customers treat them as professional technicians, not casual day labourers.
              </div>
            </div>
          </div>
        </div>

        {/* 60-Second Field Pitch Scripts */}
        <div style={{ marginBottom: "24px" }}>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "12px" }}>
            🗣️ Word-by-Word 60-Second Onboarding Pitches
          </div>

          <CopyableScriptBox
            title="1. Electrician / Plumber Pitch (At Hardware Shops &amp; Addas)"
            teluguText={`అన్నా నమస్తే! మీరు ఎలక్ట్రీషియన్/ప్లంబర్ పనులు చేస్తారు కదా.

మీ దగ్గర పనులు లేని టైమ్‌లో లేదా మధ్యాహ్నం ఖాళీగా ఉన్నప్పుడు ఎక్స్ట్రా ఆర్డర్లు కావాలా?

ఖమ్మం సిటీ కోసం "డ్యూటీపే" (DutyPe) అనే సర్వీస్ యాప్ వచ్చింది అన్నా.
దీంట్లో జాయిన్ అవ్వడానికి ఒక్క రూపాయి కూడా ఫీజు లేదు. మీరు మీ పాత కస్టమర్లను వదులుకోవాల్సిన పనిలేదు.

మీకు ఎప్పుడు ఖాళీగా ఉంటే అప్పుడు మాత్రమే యాప్ ఆన్ చేసుకోవచ్చు. మీ ఇంటి దగ్గర్లోనే 3-4 కి.మీ లోపల ఉండే కాలనీల నుంచి పనులు వస్తాయి.

కస్టమర్ యాప్ లో చూసి ఫిక్స్‌డ్ రేటు ఇస్తారు — ఎలాంటి బేరాలాడటం ఉండదు. పని అవ్వగానే డబ్బులు వెంటనే మీ ఫోన్‌పే/గూగుల్‌పే లేదా క్యాష్ గా చేతికి వస్తాయి.

రండి అన్నా, ఒక్క 2 నిమిషాల్లో మీ ఫోన్ లో యాప్ వేసి చూపిస్తాను. నచ్చకపోతే తీసేయవచ్చు!`}
            englishText={`Greetings brother! You do electrical/plumbing work right?

When you don&apos;t have work or during idle afternoon hours, would you like extra orders?

DutyPe app has launched specifically for Khammam city. There is ZERO joining fee. You do NOT need to leave your regular private customers.

Turn on the app only when you are free. You will get jobs within 3-4 km of your area (Wyra Rd, Balaji Nagar, etc.).

Customers pay standard fixed catalog rates — zero bargaining arguments. Once the job is completed, money is paid instantly to your PhonePe/GPay or cash.

Come brother, let me show you the app on your phone in 2 minutes. If you don&apos;t like it, you can delete it anytime!`}
            tip="Stand beside them, take their smartphone in your hand politely, and guide their finger to enter their phone number and OTP. Don't just send a link."
          />

          <CopyableScriptBox
            title="2. AC Service Technician Pitch (At Electronic Centers &amp; Repair Hubs)"
            teluguText={`నమస్తే అన్నా! సమ్మర్ లో లేదా రెగ్యులర్ గా AC సర్వీసింగ్, గ్యాస్ ఫిల్లింగ్, రిపేర్ పనుల కోసం డ్యూటీపే లో డైరెక్ట్ ఆర్డర్లు వస్తున్నాయి.

బయట షాపులు లేదా ఏజెంట్లు మీ కష్టంలో 40% కమీషన్ కట్ చేసుకుంటారు. కానీ డ్యూటీపే లో 80% కంటే ఎక్కువ మీ జేబులోనే ఉంటుంది.

ఇందులో రేట్లు ముందే ఫిక్స్ అయి ఉంటాయి. ఖమ్మం సిటీ లోని పెద్ద కాలనీల నుంచి వచ్చే జెన్యూన్ కస్టమర్లకు మిమ్మల్ని కనెక్ట్ చేస్తాము.

మీరు రోజుకు కేవలం 2 ఎక్స్ట్రా AC సర్వీస్ లు చేసినా నెలకు ₹15,000 నుండి ₹25,000 అదనపు ఆదాయం వస్తుంది అన్నా!`}
            englishText={`Greetings brother! For AC servicing, gas charging, and maintenance, direct orders are being booked on DutyPe.

Outside middlemen and dealers take away 40% of your labor. On DutyPe, over 80% stays directly in your pocket.

Standard fixed rates are shown to customers upfront. We connect you with verified homeowners in Khammam&apos;s prime colonies.

Even if you complete just 2 extra AC services a day in your free time, you make an additional ₹15,000 to ₹25,000 per month!`}
            tip="AC technicians love transparency on spare parts. Explain to them that spare parts charges are paid separately by customer."
          />

          <CopyableScriptBox
            title="3. Home Cleaners, Painters &amp; Carpenters Pitch"
            teluguText={`అక్కా/అన్నా నమస్తే! ఇళ్లల్లో డీప్ క్లీనింగ్, సోఫా క్లీనింగ్, పెయింటింగ్ టచ్-అప్ పనులకు మంచి డిమాండ్ ఉంది.

రోజు కూలీ కంటే 2-3 రెట్లు ఎక్కువ సంపాదించవచ్చు. డ్యూటీపే ద్వారా సేఫ్ గా ఉండే ఫ్యామిలీ ఇళ్లల్లోనే పనులు ఇస్తాము.

కస్టమర్ మొబైల్ నంబర్ వెరిఫై అయి ఉంటుంది కాబట్టి లేడీస్ కి కూడా 100% సేఫ్ మరియు గౌరవప్రదమైన పని. ఒక్కసారి రిజిస్టర్ చేసుకోండి.`}
            englishText={`Sister/Brother greetings! There is huge demand for house deep cleaning, sofa cleaning, and touch-up painting in Khammam.

You can earn 2-3x more than standard daily coolie wages. Through DutyPe, you only get jobs in safe, verified family households.

Every customer&apos;s identity is verified, making it 100% safe, secure, and respectable. Let&apos;s register your profile today.`}
            tip="For women cleaners, emphasize safety, OTP protection, and verified family homes. Safety is their #1 decision factor."
          />
        </div>

        {/* The 8 Toughest Worker Objections */}
        <div>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "12px" }}>
            🥊 The 8 Toughest Worker Objections &amp; Bulletproof Answers
          </div>

          <ObjectionQACard
            question="1. 'Why should I install your app? I already have regular customers.'"
            teluguQuestion="నాకు ఆల్రెడీ తెలిసిన కస్టమర్లు ఉన్నారు, నేను ఎందుకు మీ యాప్ వాడాలి? నాకేం లాభం?"
            psychology="He thinks you want him to leave his existing customers or that the app will control his time like a slave."
            exactAnswerTelugu="అన్నా, మీ పాత కస్టమర్లను వదులుకోమని మేము అస్సలు చెప్పడం లేదు! మీ రెగ్యులర్ పనులు మీరు యధావిధిగా చేసుకోండి. కానీ మధ్యాహ్నం 1 నుండి 4 గంటల మధ్య లేదా కొన్ని రోజుల్లో మీకు పనులు ఉండవు కదా? ఆ ఖాళీ సమయంలో కూర్చునే బదులు డ్యూటీపే ఆన్ చేసుకుంటే మీ ఇంటి పక్కనే ₹400-500 పని దొరుకుతుంది. ఇది మీ పాత ఆదాయానికి అదనపు బోనస్ మాత్రమే!"
            exactAnswerEnglish="Brother, we are not asking you to leave your existing customers at all! Keep 100% of your regular work. But during idle hours or slow days when you sit waiting, DutyPe brings you extra jobs right near your house. It is pure bonus income on top of what you already make."
            doNotSay="Stop doing outside work and work only for DutyPe."
          />

          <ObjectionQACard
            question="2. 'Are you taking commission from my hard-earned daily wages?'"
            teluguQuestion="నేను కష్టపడి పని చేస్తే మీరేమైనా కమిషన్ కట్ చేసుకుంటారా?"
            psychology="Fears predatory 30-40% cuts like food delivery apps, feels platform is taking away his sweat equity."
            exactAnswerTelugu="అన్నా, మీ మొదటి పనులకు మేము ఒక్క రూపాయి కూడా కమిషన్ తీసుకోము — 100% మీకే! ఆ తర్వాత కూడా, కస్టమర్ ఇచ్చే రేటులో 80% పైగా డైరెక్ట్ గా మీ జేబులోనే ఉంటుంది. మేము కేవలం ప్లాట్‌ఫామ్ ఖర్చు కోసం చిన్న ఫీజు మాత్రమే ఉంచుతాము. మీరు ఖాళీగా కూర్చుంటే ₹0 వస్తుంది. అదే డ్యూటీపే లో ₹500 పని వస్తే ₹400 మీ చేతికి వస్తుంది కదా! లాభమా నష్టమా మీరే చెప్పండి."
            exactAnswerEnglish="Brother, for your initial jobs we take 0% commission — you keep 100%! Even after that, you keep 80%+ directly in your pocket. If you sit idle without work, you make ₹0. If DutyPe brings you an extra ₹500 job and you take home ₹400+, is that profit or loss? You decide."
            doNotSay="Yes, our company policy deducts 20% platform tax from every worker."
          />

          <ObjectionQACard
            question="3. 'What if the customer bargains after I reach, or refuses to pay?'"
            teluguQuestion="నేను వెళ్ళిన తర్వాత కస్టమర్ రేటు తగ్గించమని అడిగితే లేదా డబ్బులు ఇవ్వకపోతే?"
            psychology="Tired of cheap customers who haggle after 2 hours of heavy physical labor."
            exactAnswerTelugu="డ్యూటీపే లో అలాంటి గొడవలే ఉండవు అన్నా! కస్టమర్ యాప్ లో రేటు చూసి, ఒప్పుకున్నాకే బుకింగ్ కన్ఫర్మ్ అవుతుంది. మీరు పని ప్రారంభించే ముందు కస్టమర్ 4 అంకెల స్టార్ట్ OTP చెప్తారు. రేటు విషయంలో కస్టమర్ తో మీరు వాదించాల్సిన పనిలేదు — 'యాప్ లో ఫిక్స్ చేసిన కంపెనీ రేటు అండి' అని చెబితే చాలు. ఎవరైనా ఇబ్బంది పెడితే మా హెల్ప్‌లైన్ నంబర్ కి కాల్ చేయండి, మేము చూసుకుంటాము."
            exactAnswerEnglish="In DutyPe that problem is eliminated! The customer sees the fixed rate in the app and agrees before booking. Before you start work, customer provides a 4-digit Start OTP. You never have to argue — you just say 'This is the standard company catalog rate'. If any dispute arises, our local helpline steps in immediately."
            doNotSay="You have to settle the rate directly with the customer."
          />

          <ObjectionQACard
            question="4. 'Will you force me to travel 15-20 kilometers away into rural villages?'"
            teluguQuestion="నన్ను ఊరు బయటకి లేదా చాలా దూరం వెళ్ళమని అడుగుతారా?"
            psychology="Wasting expensive petrol (₹105/litre) on distant calls that don't pay well."
            exactAnswerTelugu="అస్సలు ఉండదు అన్నా! డ్యూటీపే ఖమ్మం సిటీ లోపల మీ ఇంటి చుట్టుపక్కల 3 నుండి 4 కిలోమీటర్ల లోపల మాత్రమే మీకు పనులు ఇస్తుంది (ఉదాహరణకు వైరా రోడ్, బాలాజీ నగర్, గాంధీ చౌక్). మీకు ఇష్టం లేకపోతే లేదా దూరం అనిపిస్తే ఆర్డర్ రిజెక్ట్ చేసే పూర్తి స్వేచ్ఛ మీకు ఉంటుంది. ఎలాంటి ఫైన్ ఉండదు."
            exactAnswerEnglish="Never! DutyPe operates strictly within a 3-4 km radius of your location in Khammam. If you don&apos;t want to take an order, you have 100% freedom to reject it. Zero penalties."
            doNotSay="You must accept all orders assigned to you anywhere in the district."
          />

          <ObjectionQACard
            question="5. 'I don't know English or complicated apps. Can I operate it?'"
            teluguQuestion="నాకు ఇంగ్లీష్ రాదు, పెద్దగా చదువుకోలేదు, యాప్ అర్థం అవుతుందా?"
            psychology="Shame or embarrassment about tech illiteracy; fear of tapping wrong buttons."
            exactAnswerTelugu="అన్నా, మా యాప్ మొత్తం అచ్చ తెలుగు లోనే ఉంటుంది! పెద్ద పెద్ద చదువులు అవసరం లేదు. కేవలం రెండు బటన్లు ఉంటాయి: 'పనిని అంగీకరించు' (Accept) మరియు 'పనిని ప్రారంభించు' (Start). ఒక 5వ తరగతి పిల్లాడు కూడా సులభంగా వాడొచ్చు. ఇప్పుడే మీ ఫోన్ లో పెట్టి ప్రాక్టికల్ గా నేర్పిస్తాను రండి."
            exactAnswerEnglish="Brother, our app is 100% in clean, simple Telugu! No technical knowledge needed. It has just two big buttons: Accept Job and Start Job. Let me install it right now and guide you step-by-step in 60 seconds."
            doNotSay="Our mobile application is built with modern high-tech architecture."
          />

          <ObjectionQACard
            question="6. 'What if I install the app and get zero bookings for 10 days?'"
            teluguQuestion="యాప్ వేసుకుంటే పనులు రాకపోతే ఏంటి?"
            psychology="Skepticism that this is just another dead app taking up phone space."
            exactAnswerTelugu="అన్నా, అందుకే కదా నేను డైరెక్ట్ గా మీ దగ్గరికి వచ్చాను! ఈ వారమే ఖమ్మం లోని ప్రధాన కాలనీల్లో మరియు అపార్ట్‌మెంట్లలో పెద్ద ఎత్తున ప్రచారం మొదలుపెడుతున్నాము. కస్టమర్లు బుక్ చేసినప్పుడు వారికి అందించడానికి మీలాంటి నమ్మకమైన నిపుణులు మాకు కావాలి. యాప్ మీ ఫోన్ లో ఉంచుకోవడానికి మీరు ఒక్క రూపాయి కూడా చెల్లించట్లేదు. పని వస్తే డబ్బులు వస్తాయి, లేదంటే మీరు ఏమీ కోల్పోరు!"
            exactAnswerEnglish="Brother, that is exactly why I came to see you in person! This week we are launching marketing directly across Khammam colonies. We need trusted technicians like you ready to receive those orders. Keeping the app costs you ₹0. When orders come, you make money. You lose nothing."
            doNotSay="I guarantee you will get 15 orders tomorrow morning."
          />

          <ObjectionQACard
            question="7. 'Is DutyPe a fraud company? Why are you asking for my details?'"
            teluguQuestion="ఇది ఏమైనా ఫ్రాడ్ కంపెనీనా? నా వివరాలు ఎందుకు అడుగుతున్నారు?"
            psychology="Fear of cyber crime, OTP scams, fake loan apps asking for Aadhaar."
            exactAnswerTelugu="అన్నా, ఇది తెలంగాణలో రిజిస్టర్ అయిన లోకల్ కంపెనీ. నా పేరు ఇదిగోండి, నా ఆధార్, నా ఫోన్ నంబర్, మా ఆఫీస్ వివరాలు ఇవిగోండి. మేము మీ బ్యాంక్ OTP లు లేదా డబ్బులు అస్సలు అడగడం లేదు! కస్టమర్ల ఇళ్లల్లోకి వెళ్ళేటప్పుడు వాళ్లకు నమ్మకం కలగడానికి మీ ఫోటో మరియు సర్వీస్ కేటగిరీ మాత్రమే యాప్ లో చూపిస్తాము."
            exactAnswerEnglish="Brother, DutyPe is a registered local Telangana company. Here is my personal contact, WhatsApp, and credentials. We never ask for your bank OTP or any registration fee! We only verify your phone and trade so customers feel 100% safe opening their home doors."
            doNotSay="You must give your Aadhaar card immediately or you cannot work."
          />

          <ObjectionQACard
            question="8. 'How and when do I get my money?'"
            teluguQuestion="నా డబ్బులు నాకు ఎప్పుడు, ఎలా వస్తాయి?"
            psychology="Fear of delayed company settlements, having to wait 30 days for hard-earned wages."
            exactAnswerTelugu="పని పూర్తయిన మరుక్షణమే డబ్బులు మీ చేతికి వస్తాయి అన్నా! కస్టమర్ పని చూసి సంతృప్తి చెందిన వెంటనే మీ PhonePe, Google Pay కి లేదా డైరెక్ట్ గా క్యాష్ గా మీ చేతికే ఇస్తారు. వారాలు, నెలలు కంపెనీ ఇచ్చే వరకు ఆగాల్సిన పనే లేదు!"
            exactAnswerEnglish="Immediately upon completing the job, brother! The moment the work is finished, the customer pays directly to your PhonePe/GPay or cash into your hand. Zero waiting for weekly company settlements."
            doNotSay="Payouts are settled on the 1st of every month via NEFT."
          />
        </div>
      </PlaybookSection>

      {/* ─── SECTION 3: Before Marketing Checklist ─── */}
      <PlaybookSection
        title="✅ Before You Spend ₹1 on Marketing — Check These"
        icon={ShieldCheck}
        color="#d97706"
        bg="#fef3c7"
      >
        <div
          style={{
            background: "#fef9c3",
            border: "1px solid #fde68a",
            borderRadius: "10px",
            padding: "14px",
            fontSize: "13px",
            color: "#78350f",
            marginBottom: "16px",
            lineHeight: "1.6"
          }}
        >
          ⚠️ <strong>If even one of these is not ready, paid marketing will burn money and damage your reputation.</strong>
          Urban Company wasted crores in cities before they fixed their supply chain first.
        </div>
        <Checklist
          storageKey="playbook_premarketing"
          defaults={[
            { id: "pm_1", text: "Minimum 5 verified workers in target area across at least 3 categories" },
            { id: "pm_2", text: "All workers have the app installed, know how to accept bookings" },
            { id: "pm_3", text: "Test booking done — you personally booked a service end-to-end and it worked" },
            { id: "pm_4", text: "Payment flow tested — customer paid, booking confirmed, worker notified" },
            { id: "pm_5", text: "Rating & review flow tested — after service, customer sees review prompt" },
            { id: "pm_6", text: "Support flow ready — when something goes wrong, how does customer reach you? (WhatsApp number added in app)" },
            { id: "pm_7", text: "App is live on Google Play — anyone can download without issues" },
            { id: "pm_8", text: "Your response time is < 10 minutes during business hours (9am-9pm)" },
            { id: "pm_9", text: "Cancellation policy decided — what happens if worker cancels last minute?" },
            { id: "pm_10", text: "Pricing is set and matches what workers have agreed to receive" }
          ]}
        />
      </PlaybookSection>

      {/* ─── SECTION 4: While Marketing ─── */}
      <PlaybookSection
        title="📣 While Marketing — What To Do"
        icon={Megaphone}
        color="#e11d48"
        bg="#ffe4e6"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          {[
            {
              channel: "WhatsApp Groups (Free)",
              what: "Join colony groups, apartment groups, professional groups in Khammam. Share weekly: 'DutyPe available for [service] in [area] — book now'. Never spam — max 1 post per week per group.",
              roi: "Free, high conversion for known service needs"
            },
            {
              channel: "Instagram & Facebook (₹0 initially)",
              what: "Post before/after photos of completed services. Tag #Khammam #KhammamCity. Ask every happy customer to share. 3-5 posts per week consistently.",
              roi: "Builds trust, organic reach, takes 4-8 weeks to show results"
            },
            {
              channel: "Google Maps (Free)",
              what: 'Register "DutyPe Services Khammam" on Google My Business. Add your address, service categories, phone. Ask every customer to leave a Google review. This is HUGE for local search.',
              roi: "People search 'plumber near me Khammam' — you show up"
            },
            {
              channel: "Pamphlets / Flyers (₹500–2000)",
              what: "A5 size, black & white is fine. QR code linking to app. Print 200 initially, distribute ONLY in target colony. Include: 'First booking fee free' offer.",
              roi: "Best for cold areas where no one knows DutyPe yet"
            },
            {
              channel: "Referral Program (Built into app!)",
              what: "Activate the referral feature. Tell first 10 customers: 'Refer a friend, both get ₹X off'. Word of mouth in Khammam is powerful — people trust friends' recommendations.",
              roi: "Lowest CAC channel. Every referral booking costs you referral fee only."
            },
            {
              channel: "Meta Ads / Google Ads (₹200-500/day)",
              what: "ONLY start when you have 5+ workers and 10+ successful bookings. Target: Khammam, 25-45 age, home owners. Start with 'Book plumber in 59 mins Khammam' ads.",
              roi: "High reach but needs proven supply before running"
            }
          ].map((item, i) => (
            <div
              key={i}
              style={{
                background: "#f8fafc",
                border: "1px solid #e2e8f0",
                borderRadius: "10px",
                padding: "14px"
              }}
            >
              <div style={{ fontWeight: 700, fontSize: "13.5px", color: "#0f172a", marginBottom: "6px" }}>
                📱 {item.channel}
              </div>
              <div style={{ fontSize: "13px", color: "#334155", lineHeight: "1.55", marginBottom: "8px" }}>
                {item.what}
              </div>
              <div
                style={{
                  background: "#e8f5ef",
                  color: "#065f46",
                  padding: "4px 10px",
                  borderRadius: "6px",
                  fontSize: "11.5px",
                  fontWeight: 600,
                  display: "inline-block"
                }}
              >
                💰 ROI: {item.roi}
              </div>
            </div>
          ))}
        </div>
      </PlaybookSection>

      {/* ─── SECTION 4.1: Customer / Employer Pitch & Objection Battlecard ─── */}
      <PlaybookSection
        title="🏢 Customer / Employer Pitch &amp; Objection Battlecard (Telugu &amp; English)"
        icon={Building}
        color="#2563eb"
        bg="#dbeafe"
        defaultOpen={true}
      >
        <div style={{ fontSize: "13.5px", color: "#334155", lineHeight: "1.7", marginBottom: "20px" }}>
          In Khammam, homeowners are accustomed to calling their familiar street mechanic. But when that mechanic doesn&apos;t pick up, delays for 2 days, or quotes arbitrary high prices, customers feel helpless.
          Your pitch must highlight <strong>speed (59 minutes), fixed upfront rates, and trusted safety</strong>.
        </div>

        {/* What Customers Get - 5 Core Value Pillars */}
        <div style={{ marginBottom: "24px" }}>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "12px" }}>
            💎 What the Customer Gets (Their Exact Benefits)
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: "12px" }}>
            <div style={{ background: "#eff6ff", border: "1px solid #bfdbfe", borderRadius: "10px", padding: "14px" }}>
              <div style={{ fontWeight: 700, fontSize: "13.5px", color: "#1e40af", marginBottom: "4px" }}>
                ⚡ 59-Minute Doorstep Arrival
              </div>
              <div style={{ fontSize: "12.5px", color: "#1e3a8a", lineHeight: "1.5" }}>
                No begging local mechanics or waiting 2 days. Verified technician arrives right when you need them.
              </div>
            </div>

            <div style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "10px", padding: "14px" }}>
              <div style={{ fontWeight: 700, fontSize: "13.5px", color: "#166534", marginBottom: "4px" }}>
                🏷️ Fixed Upfront Transparent Rates
              </div>
              <div style={{ fontSize: "12.5px", color: "#14532d", lineHeight: "1.5" }}>
                Standard catalog rates listed in the app. Zero surprise bills, zero post-work bargaining arguments.
              </div>
            </div>

            <div style={{ background: "#fef3c7", border: "1px solid #fde68a", borderRadius: "10px", padding: "14px" }}>
              <div style={{ fontWeight: 700, fontSize: "13.5px", color: "#92400e", marginBottom: "4px" }}>
                🛡️ Verified Pros &amp; OTP Safety
              </div>
              <div style={{ fontSize: "12.5px", color: "#78350f", lineHeight: "1.5" }}>
                Identity verified with Aadhaar, verified phone, and photo. Work starts only after customer gives secure Start OTP. 100% safe for families.
              </div>
            </div>

            <div style={{ background: "#f3e8ff", border: "1px solid #e9d5ff", borderRadius: "10px", padding: "14px" }}>
              <div style={{ fontWeight: 700, fontSize: "13.5px", color: "#6b21a8", marginBottom: "4px" }}>
                🔧 30-Day Service Guarantee
              </div>
              <div style={{ fontSize: "12.5px", color: "#581c87", lineHeight: "1.5" }}>
                If the same issue recurs within 30 days, DutyPe provides a free technician revisit to fix it.
              </div>
            </div>

            <div style={{ background: "#fce7f3", border: "1px solid #fbcfe8", borderRadius: "10px", padding: "14px" }}>
              <div style={{ fontWeight: 700, fontSize: "13.5px", color: "#9d174d", marginBottom: "4px" }}>
                💳 Zero Advance: Pay After Satisfaction
              </div>
              <div style={{ fontSize: "12.5px", color: "#831843", lineHeight: "1.5" }}>
                Never pay upfront! Inspect the completed work first, then pay directly via UPI (PhonePe/GPay) or Cash.
              </div>
            </div>
          </div>
        </div>

        {/* 60-Second Customer Pitches */}
        <div style={{ marginBottom: "24px" }}>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "12px" }}>
            🗣️ Word-by-Word 60-Second Customer Pitches
          </div>

          <CopyableScriptBox
            title="1. Homemakers &amp; Families Pitch (Wyra Rd / Balaji Nagar Residential)"
            teluguText={`నమస్తే అండి! ఇంట్లో ఫ్యాన్ తిరగకపోయినా, ట్యాప్ లీక్ అయినా, స్విచ్ బోర్డు కాలినా లేదా AC కూలింగ్ రాకపోయినా ఎలక్ట్రీషియన్ లేదా ప్లంబర్ కోసం రోజుల తరబడి ఎదురుచూడాల్సిన పనిలేదు.

ఖమ్మం సిటీ కోసం "డ్యూటీపే" (DutyPe) యాప్ వచ్చింది. ఒక్క క్లిక్ తో వెరిఫైడ్ టెక్నీషియన్ 59 నిమిషాల్లో మీ ఇంటికి వస్తారు.

రేట్లు ముందే యాప్ లో కనిపిస్తాయి — ఎలాంటి బేరాలాడటం లేదా ఎక్కువ అడగడం ఉండదు. పని పూర్తయ్యాక మీరు సంతృప్తి చెందాకే ఫోన్‌పే లేదా క్యాష్ గా డబ్బులు ఇవ్వండి. 30 రోజుల సర్వీస్ గ్యారెంటీ కూడా ఉంటుంది.

ఒక్కసారి యాప్ డౌన్‌లోడ్ చేసుకోండి అండి, మీ మొదటి సర్వీస్ బుకింగ్ ఫీజు ఉచితం!`}
            englishText={`Greetings madam/sir! When a fan stops, a tap leaks, or your AC stops cooling, you no longer have to wait days for a local mechanic to show up.

DutyPe app is now live in Khammam. With one tap, verified technicians arrive at your doorstep in 59 minutes.

Standard fixed prices are shown upfront in the app — zero bargaining arguments. Pay via PhonePe or cash only after you inspect the finished work. Backed by a 30-day service guarantee.

Download the app today — your first booking fee is completely free!`}
            tip="Show them the mobile app screen on your phone with the 3D icons. Visuals build instant credibility with families."
          />

          <CopyableScriptBox
            title="2. Shop Owners, Small Businesses &amp; Clinics Pitch"
            teluguText={`సార్ నమస్తే! మీ షాప్ లేదా క్లినిక్ లో అర్జెంట్ గా లైట్లు పోయినా, స్విచ్ బోర్డు కాలినా లేదా AC ఆగిపోయినా కస్టమర్లకు చాలా ఇబ్బంది అవుతుంది కదా.

డ్యూటీపే లో అర్జెంట్ సర్వీస్ బుక్ చేసుకుంటే అరగంటలో ప్రొఫెషనల్ టెక్నీషియన్ వచ్చి పని చేసి వెళ్తారు.

ఫిక్స్‌డ్ రేట్లు, ఎలాంటి హెడేక్ ఉండదు. రెగ్యులర్ గా మీ షాప్ లేదా ఆఫీస్ మెయింటెనెన్స్ కోసం కూడా నమ్మకంగా వాడుకోవచ్చు.`}
            englishText={`Sir greetings! In your shop, showroom, or clinic, if lights trip or AC breaks down, it disrupts business immediately.

Booking urgent service on DutyPe gets a professional technician at your shop within 30-45 minutes.

Standard fixed rates, zero hassle. Ideal for regular, dependable commercial electrical and plumbing maintenance.`}
            tip="Hand them a DutyPe business card with the QR code. Shop owners stick it near their billing counter."
          />

          <CopyableScriptBox
            title="3. Working Professionals &amp; Techies Pitch"
            teluguText={`హలో ఫ్రెండ్! ఆఫీస్ వర్క్ లేదా బిజీ షెడ్యూల్ లో ఉన్నప్పుడు ఇంటి పనుల కోసం రోడ్ల మీద ఎలక్ట్రీషియన్లను వెతకడం పెద్ద తలనొప్పి.

అర్బన్ కంపెనీ హైదరాబాద్ లో ఎలా పనిచేస్తుందో, ఖమ్మంలో డ్యూటీపే సేమ్ అదే ప్రొఫెషనల్ సర్వీస్ ఇస్తుంది!

లైవ్ ఆర్డర్ స్టేటస్, స్టార్ట్ OTP, సేఫ్టీ గ్యారెంటీ తో మీ కన్వీనియంట్ టైమ్ కి సులభంగా బుక్ చేసుకోవచ్చు. ట్రై చేయండి!`}
            englishText={`Hello friend! When you are busy with work, hunting for roadside technicians in Khammam is a major hassle.

Just like Urban Company in Hyderabad or Bangalore, DutyPe provides the exact same seamless professional doorstep service right here in Khammam!

OTP safety, live updates, transparent pricing. Schedule anytime at your convenience. Try it once!`}
            tip="Mentioning 'Urban Company for Khammam' immediately clicks with tech-savvy people and working couples."
          />
        </div>

        {/* The 8 Toughest Customer Objections */}
        <div>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "12px" }}>
            🥊 The 8 Toughest Customer Objections &amp; Bulletproof Answers
          </div>

          <ObjectionQACard
            question="1. 'Why should I book on DutyPe when I already have my local electrician's number?'"
            teluguQuestion="మా వీధి ఎలక్ట్రీషియన్ నంబర్ నా దగ్గర ఉంది, మీ యాప్ ఎందుకు వాడాలి?"
            psychology="Habit and inertia. Thinks calling his known electrician is easier, until an emergency strikes."
            exactAnswerTelugu="మేడమ్/సార్, మీ లోకల్ ఎలక్ట్రీషియన్ ఎప్పుడైనా ఫోన్ ఎత్తకపోతే లేదా 'రెండు రోజుల తర్వాత వస్తాను' అని అంటే మీరు ఇబ్బంది పడతారు కదా? అంతేకాకుండా పని చిన్నదైనా ₹300-500 ఇష్టమొచ్చిన రేటు అడుగుతారు. డ్యూటీపే లో ఎప్పుడైనా బుక్ చేసుకోండి — 59 నిమిషాల్లో ప్రొఫెషనల్ టెక్నీషియన్ మీ ఇంటికి వస్తారు. రేట్లు ముందే తెలుస్తాయి, 30 రోజుల గ్యారెంటీ కూడా ఉంటుంది!"
            exactAnswerEnglish="Madam/Sir, what happens when your local electrician doesn&apos;t pick up the phone or says &apos;I can only come after 2 days&apos;? Plus, they quote arbitrary rates without warranty. On DutyPe, verified technicians arrive in 59 minutes at fixed rates with a 30-day service guarantee!"
            doNotSay="Your local electrician is bad or overcharging you."
          />

          <ObjectionQACard
            question="2. 'What if the worker steals something from my house or damages my TV/fridge?'"
            teluguQuestion="మా ఇంట్లోకి వచ్చే వర్కర్ ఏదైనా దొంగతనం చేసినా లేదా వస్తువు పాడుచేసినా ఎవరు బాధ్యత?"
            psychology="Safety and security of family members and expensive home electronics."
            exactAnswerTelugu="మేడమ్, డ్యూటీపే లో చేరే ప్రతి టెక్నీషియన్ ఆధార్ కార్డు, మొబైల్ నంబర్ మరియు లోకల్ అడ్రస్ మేము క్షుణ్ణంగా వెరిఫై చేస్తాము. టెక్నీషియన్ మీ ఇంటికి వచ్చేటప్పుడు వారి ఫోటో, పేరు మీ యాప్ లో స్పష్టంగా కనిపిస్తాయి. మీరు 4-అంకెల స్టార్ట్ OTP చెప్తేనే పని ప్రారంభమవుతుంది. ఏదైనా పొరపాటు జరిగినా లోకల్ గా మా హెల్ప్‌లైన్ మరియు కస్టమర్ ప్రొటెక్షన్ సపోర్ట్ ఉంటుంది."
            exactAnswerEnglish="Madam, every single DutyPe technician undergoes Aadhaar verification, phone verification, and trade check. You see their photo and name in the app. Work starts strictly with a 4-digit OTP. If any issue arises, DutyPe local support is directly accountable."
            doNotSay="We are just a tech software platform, we are not responsible for workers."
          />

          <ObjectionQACard
            question="3. 'Is DutyPe more expensive than roadside technicians?'"
            teluguQuestion="బయట కంటే మీ దగ్గర రేట్లు ఎక్కువా?"
            psychology="Price sensitivity in tier-2/tier-3 cities; fear of startup markup."
            exactAnswerTelugu="అస్సలు కాదు అండి! నిజానికి బయట కంటే డ్యూటీపే లోనే మీకు తక్కువ ఖర్చు అవుతుంది. బయట వాళ్ళు విజిటింగ్ కే ₹200 అడిగి, చిన్న స్విచ్ మార్చడానికి ₹400 అడుగుతారు. డ్యూటీపే లో స్విచ్ రిపేర్ ₹99, AC సర్వీస్ ₹399 లాంటి ప్రామాణికమైన రేట్లు ముందే కనిపిస్తాయి. ఒక్క రూపాయి కూడా దాచిన ఛార్జీ ఉండదు."
            exactAnswerEnglish="Not at all! In reality, DutyPe is more economical and honest. Roadside mechanics charge ₹200 just to visit and quote random prices. On DutyPe, transparent fixed rates are shown upfront (e.g. Switch repair ₹99, AC service ₹399). Zero hidden charges."
            doNotSay="We are a premium platform, so our prices are higher than local workers."
          />

          <ObjectionQACard
            question="4. 'What if the technician does poor work and the problem repeats after 2 days?'"
            teluguQuestion="పని సరిగ్గా చేయకపోతే? రెండు రోజుల తర్వాత మళ్ళీ రిపేర్ వస్తే?"
            psychology="Burned by local mechanics who took cash and never answered the phone when the switch or tap broke again."
            exactAnswerTelugu="డ్యూటీపే లో ప్రతి బుకింగ్ కి 30-రోజుల సర్వీస్ రీ-వర్క్ గ్యారెంటీ ఉంటుంది అండి! 30 రోజుల్లోపు అదే సమస్య మళ్ళీ వస్తే, మా టెక్నీషియన్ వచ్చి ఉచితంగా సరిచేస్తారు. అంతేకాకుండా మీ రేటింగ్ ఆధారంగానే టెక్నీషియన్లకు భవిష్యత్తులో పనులు వస్తాయి కాబట్టి వాళ్ళు చాలా జాగ్రత్తగా, క్వాలిటీగా పని చేస్తారు."
            exactAnswerEnglish="Every DutyPe booking includes a 30-day rework warranty! If the same problem recurs within 30 days, a technician revisits and rectifies it for free. Furthermore, worker ratings directly affect their livelihood on DutyPe, so they maintain the highest standard of craftsmanship."
            doNotSay="Once the worker leaves, you have to book again and pay."
          />

          <ObjectionQACard
            question="5. 'Do I have to pay in advance before seeing the technician?'"
            teluguQuestion="నేను ముందుగానే డబ్బులు కట్టాలా?"
            psychology="Fear of online payment fraud or technician not showing up after taking money."
            exactAnswerTelugu="ముందుగా ఒక్క రూపాయి కూడా కట్టాల్సిన అవసరం లేదు అండి! టెక్నీషియన్ మీ ఇంటికి వచ్చి, మీ కళ్లముందే పని పూర్తి చేసి, మీరు సంతృప్తి చెందిన తర్వాత మాత్రమే PhonePe, Google Pay లేదా క్యాష్ గా చెల్లించవచ్చు."
            exactAnswerEnglish="Zero advance payment! You inspect the completed service with your own eyes, and only when 100% satisfied do you pay via PhonePe/GPay or cash."
            doNotSay="You must pay the full service cost online before booking."
          />

          <ObjectionQACard
            question="6. 'DutyPe is new in Khammam. How can we trust an unknown app?'"
            teluguQuestion="ఖమ్మంలో కొత్త యాప్ కదా, మేము ఎలా నమ్మాలి?"
            psychology="Small town skepticism toward new apps; prefers known community trust."
            exactAnswerTelugu="సార్/మేడమ్, డ్యూటీపే ఖమ్మం మరియు తెలంగాణ నగరాల కోసమే ప్రత్యేకంగా రూపొందించబడింది. వైరా రోడ్, బాలాజీ నగర్ లాంటి మన స్థానిక ప్రాంతాల్లోని నిపుణులనే మేము వెరిఫై చేసి ఆన్‌బోర్డ్ చేశాము. ఒక్క చిన్న ₹99 పనికి ట్రై చేసి చూడండి — మా సర్వీస్ మరియు స్పీడ్ మీకు నచ్చకపోతే మీ బుకింగ్ ఫీజు మేము వాపస్ ఇస్తాము!"
            exactAnswerEnglish="Sir/Madam, DutyPe is custom-built for Khammam and Telangana towns. We have verified and onboarded local technicians right from Wyra Road, Balaji Nagar, and Gandhi Chowk. Try it once for a small ₹99 service — if you don&apos;t love the experience, we refund your booking fee immediately!"
            doNotSay="We are a venture-funded unicorn from Bangalore."
          />

          <ObjectionQACard
            question="7. 'What if the technician doesn't turn up on time?'"
            teluguQuestion="వర్కర్ టైమ్‌కి రాకపోతే నేను ఏం చేయాలి?"
            psychology="Frustration with casual, unaccountable workmen who promise 10 AM and show up at 4 PM."
            exactAnswerTelugu="యాప్ లో మీరు బుక్ చేసిన వెంటనే టెక్నీషియన్ ఎక్కడ ఉన్నారో లైవ్ లో తెలుస్తుంది. ఒకవేళ టెక్నీషియన్ 15 నిమిషాల్లోపు రాకపోతే మా సిస్టమ్ ఆటోమేటిక్ గా వేరే దగ్గరలో ఉన్న టెక్నీషియన్ కి అసైన్ చేస్తుంది. మా లోకల్ సపోర్ట్ టీమ్ ఎప్పుడూ అందుబాటులో ఉంటుంది."
            exactAnswerEnglish="The moment you book, you receive technician assignment and arrival updates. If any technician is delayed, our system immediately reassigns the nearest available partner, and our local helpline assists you in real-time."
            doNotSay="Technicians sometimes get stuck in traffic, you have to wait."
          />

          <ObjectionQACard
            question="8. 'How are spare parts charges handled? Will the worker cheat on parts prices?'"
            teluguQuestion="స్పేర్ పార్ట్స్ కి విడిగా ఛార్జ్ చేస్తారా? ఎక్కువ రేటు వేస్తే?"
            psychology="Mechanics padding bill with duplicate or overpriced spare parts."
            exactAnswerTelugu="మేడమ్/సార్, సర్వీస్ లేబర్ ఛార్జ్ యాప్ లో ఫిక్స్ అయి ఉంటుంది. ఏదైనా పైప్, వైర్ లేదా స్విచ్ మార్చాల్సి వస్తే టెక్నీషియన్ షాప్ అసలు బిల్లు మీకు చూపిస్తారు. లేదా మీరే స్వయంగా దుకాణంలో కొని ఇచ్చినా టెక్నీషియన్ దాన్ని ఫిట్ చేసి ఇస్తారు! 100% పారదర్శకత."
            exactAnswerEnglish="Madam/Sir, the labor fee is strictly fixed in the app. If any spare part (pipe, wire, capacitor) is required, the technician must show the authentic shop bill, or you can purchase the part yourself and the technician will install it! 100% transparent."
            doNotSay="The technician decides spare parts rates at his discretion."
          />
        </div>
      </PlaybookSection>

      {/* ─── SECTION 4.2: Khammam Ground Tactics & WhatsApp Scripts ─── */}
      <PlaybookSection
        title="🎯 Khammam Ground Tactics &amp; Ready WhatsApp Scripts"
        icon={Target}
        color="#dc2626"
        bg="#fee2e2"
      >
        <div style={{ fontSize: "13.5px", color: "#334155", lineHeight: "1.7", marginBottom: "20px" }}>
          Ground guerilla tactics that work in Khammam without high ad spend. Focus on high-trust local nodes:
          <strong> Hardware shop owners, Apartment security guards, and Colony WhatsApp groups</strong>.
        </div>

        {/* 3 Ground Tactics */}
        <div style={{ display: "flex", flexDirection: "column", gap: "14px", marginBottom: "24px" }}>
          <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: "12px", padding: "16px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "8px" }}>
              <Store size={20} style={{ color: "#d97706" }} />
              <strong style={{ fontSize: "14px", color: "#0f172a" }}>Tactic 1: The Hardware Store Affiliate (Wyra Rd &amp; Balaji Nagar)</strong>
            </div>
            <div style={{ fontSize: "13px", color: "#475569", lineHeight: "1.6", marginBottom: "10px" }}>
              Hardware and electrical store owners know every tradesman in Khammam. Visit 5 stores on Wyra Road.
              Offer the owner <strong>₹100 for every verified technician</strong> who downloads DutyPe Partner app via their store QR code.
              Give them a laminated counter standee: <em>&quot;ఎలక్ట్రీషియన్ లేదా ప్లంబర్ పనుల కోసం డ్యూటీపే లో ఉచితంగా చేరండి.&quot;</em>
            </div>
            <div style={{ background: "#fef3c7", padding: "8px 12px", borderRadius: "6px", fontSize: "12px", color: "#92400e" }}>
              🎯 <strong>Result:</strong> 5 shops × 4 technicians each = 20 verified local partners in 72 hours!
            </div>
          </div>

          <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: "12px", padding: "16px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "8px" }}>
              <Building size={20} style={{ color: "#2563eb" }} />
              <strong style={{ fontSize: "14px", color: "#0f172a" }}>Tactic 2: Apartment RWA &amp; Security Guard Network</strong>
            </div>
            <div style={{ fontSize: "13px", color: "#475569", lineHeight: "1.6", marginBottom: "10px" }}>
              Meet the Secretary/President of 3 gated communities or apartment buildings.
              Offer a <strong>&quot;Free Home Safety Checkup&quot;</strong> (free switchboard &amp; tap inspection for 15 flats).
              Give the apartment security guard ₹50 mobile recharge per 5 residents who install the customer app from their flyer.
            </div>
            <div style={{ background: "#eff6ff", padding: "8px 12px", borderRadius: "6px", fontSize: "12px", color: "#1e40af" }}>
              🎯 <strong>Result:</strong> Concentrated density of 50+ households in one building = zero travel time for workers!
            </div>
          </div>

          <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: "12px", padding: "16px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "8px" }}>
              <PhoneCall size={20} style={{ color: "#087f68" }} />
              <strong style={{ fontSize: "14px", color: "#0f172a" }}>Tactic 3: The First 10 Bookings Blitz (Founder Hand-Holding)</strong>
            </div>
            <div style={{ fontSize: "13px", color: "#475569", lineHeight: "1.6", marginBottom: "10px" }}>
              For the first 10 customer bookings, <strong>personally accompany the technician</strong> or call the customer 5 minutes before arrival.
              Take a before &amp; after photo, ask for a 5-star Google review on the spot, and give the customer a ₹50 discount coupon for their neighbor.
            </div>
            <div style={{ background: "#f0fdf4", padding: "8px 12px", borderRadius: "6px", fontSize: "12px", color: "#166534" }}>
              🎯 <strong>Result:</strong> 10 delighted customers create 30 referral leads via word-of-mouth in 1 week.
            </div>
          </div>
        </div>

        {/* Ready-to-Copy WhatsApp Broadcast Templates */}
        <div>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "12px" }}>
            📲 1-Tap Copy WhatsApp Status &amp; Broadcast Templates
          </div>

          <CopyableScriptBox
            title="Template 1: For Homeowners &amp; Colony WhatsApp Groups"
            teluguText={`🏠 ఖమ్మం ప్రజల కోసం శుభవార్త! 

ఇంట్లో ఎలక్ట్రీషియన్, ప్లంబర్ లేదా AC సర్వీస్ కావాలా?
రోడ్ల మీద వెతకడం, గంటల తరబడి వేచి చూడడం ఇక అవసరం లేదు!

👉 "DutyPe" యాప్ ద్వారా కేవలం 59 నిమిషాల్లో వెరిఫైడ్ టెక్నీషియన్ మీ ఇంటికే వస్తారు!

✅ ముందే ఫిక్స్ చేసిన సరసమైన రేట్లు
✅ 30 రోజుల సర్వీస్ గ్యారెంటీ
✅ పని అయ్యాక మాత్రమే పేమెంట్

ఇప్పుడే యాప్ డౌన్‌లోడ్ చేసుకోండి:
🔗 https://dutype.in/download
📞 హెల్ప్‌లైన్: +91 8008008000`}
            englishText={`🏠 Great news for Khammam residents!

Need an Electrician, Plumber, or AC technician at home?
No more hunting roadside or waiting days!

👉 Through DutyPe app, verified professional technicians arrive at your doorstep in 59 minutes!

✅ Standard upfront honest rates
✅ 30-day rework warranty
✅ Pay only after 100% satisfaction

Download now:
🔗 https://dutype.in/download
📞 Helpline: +91 8008008000`}
            tip="Post this on your WhatsApp Status and share in colony groups every Wednesday and Saturday morning at 8:30 AM."
          />

          <CopyableScriptBox
            title="Template 2: Worker / Partner Recruitment Broadcast"
            teluguText={`👷 ఖమ్మం ఎలక్ట్రీషియన్, ప్లంబర్ మరియు AC మెకానిక్ అన్నలకు గమనిక!

మీరు రోజూ ఖాళీగా ఉండే సమయంలో అదనపు ఆదాయం సంపాదించాలనుకుంటున్నారా?

DutyPe పార్టనర్ గా ఉచితంగా జాయిన్ అవ్వండి:
💰 జీరో జాయినింగ్ ఫీజు — ఉచిత రిజిస్ట్రేషన్
🕒 మీకు నచ్చిన సమయంలోనే పనులు
📍 మీ ఇంటికి 3-4 కి.మీ లోపలే ఆర్డర్లు
💵 పని పూర్తవగానే వెంటనే డైరెక్ట్ పేమెంట్

ఆసక్తి ఉన్నవారు వెంటనే కాల్ లేదా వాట్సాప్ చేయండి:
📞 +91 8008008000`}
            englishText={`👷 Attention Khammam Electricians, Plumbers &amp; AC Technicians!

Want to earn extra income during your idle hours?

Join DutyPe as a verified partner for free:
💰 Zero joining fee — Free registration
🕒 Work at your own convenient hours
📍 Orders within 3-4 km of your area
💵 Instant direct payment after job completion

Interested partners contact now:
📞 +91 8008008000`}
            tip="Broadcast to your personal contacts list and ask local hardware shop owners to forward to their tradesmen groups."
          />
        </div>
      </PlaybookSection>

      {/* ─── SECTION 5: Business Fundamentals ─── */}
      <PlaybookSection
        title="📊 Business Fundamentals You Must Know"
        icon={BarChart3}
        color="#0284c7"
        bg="#e0f2fe"
      >
        <div style={{ marginBottom: "20px" }}>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "4px" }}>
            The Cold Start Problem — Every Marketplace Faces This
          </div>
          <div style={{ fontSize: "13.5px", color: "#334155", lineHeight: "1.65", marginBottom: "12px" }}>
            Customers won&apos;t use you if there are no workers. Workers won&apos;t join if there are no customers.
            <strong> Your job is to manually break this loop</strong> by building supply first in a tiny area,
            then bringing demand to that same tiny area. Never try to do both at city scale at once.
          </div>
          <div
            style={{
              background: "#f1f5f9",
              borderRadius: "10px",
              padding: "14px",
              fontFamily: "monospace",
              fontSize: "12.5px",
              color: "#1e293b",
              lineHeight: "1.8"
            }}
          >
            <strong>Supply First:</strong> 10 workers in 1 colony<br />
            → <strong>Demand:</strong> Market ONLY in that colony<br />
            → <strong>3 bookings/day:</strong> Expand to colony 2<br />
            → Repeat → City-wide density → Sustainable
          </div>
        </div>

        <div style={{ marginBottom: "20px" }}>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "12px" }}>
            Key Metrics You Must Track Every Day
          </div>
          <div style={{ display: "grid", gap: "10px" }}>
            <MetricCard
              label="Fill Rate"
              formula="Bookings Fulfilled ÷ Bookings Requested × 100"
              target="> 85%"
              why="If fill rate is below 85%, you don't have enough workers. Stop marketing and recruit more supply first."
              color="#087f68"
            />
            <MetricCard
              label="LTV : CAC Ratio"
              formula="(Avg bookings per customer × Margin per booking) ÷ Cost to acquire customer"
              target="> 3:1"
              why="If it costs you ₹200 to acquire a customer and they book once (₹60 margin), you're losing money. Need repeat bookings."
              color="#0284c7"
            />
            <MetricCard
              label="D30 Retention"
              formula="% of customers who book again within 30 days of first booking"
              target="> 25%"
              why="If less than 1 in 4 customers comes back, your service quality needs fixing before scaling."
              color="#7c3aed"
            />
            <MetricCard
              label="Booking-to-Revenue per Month"
              formula="Total bookings × Average commission per booking"
              target="Cover your Firebase + operational costs"
              why="Know exactly how many bookings per month you need to break even. Start with that as your first milestone."
              color="#d97706"
            />
            <MetricCard
              label="Worker Activity Rate"
              formula="Active workers (accepted ≥1 booking in 7 days) ÷ Total registered workers"
              target="> 60%"
              why="If workers aren't taking bookings, your supply is fake. They may have quit silently. Call them every week."
              color="#e11d48"
            />
          </div>
        </div>

        <div>
          <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "12px" }}>
            Unit Economics — Know This Before Spending on Ads
          </div>
          <div
            style={{
              background: "#0f172a",
              borderRadius: "12px",
              padding: "20px",
              fontFamily: "monospace",
              fontSize: "12.5px",
              color: "#e2e8f0",
              lineHeight: "2"
            }}
          >
            <div style={{ color: "#94a3b8" }}>{"// Example: AC Service Booking"}</div>
            <div><span style={{ color: "#86efac" }}>Customer pays:</span>  ₹500</div>
            <div><span style={{ color: "#86efac" }}>Your commission:</span> ₹100 (20%)</div>
            <div><span style={{ color: "#86efac" }}>Worker earns:</span>   ₹400 (80%)</div>
            <div><span style={{ color: "#fcd34d" }}>Payment processing:</span> ₹6 (~1.2%)</div>
            <div><span style={{ color: "#fcd34d" }}>Firebase infra/booking:</span> ₹2</div>
            <div style={{ borderTop: "1px solid #334155", paddingTop: "8px", marginTop: "8px" }}>
              <span style={{ color: "#f87171" }}>Gross profit/booking:</span> <strong style={{ color: "#fff" }}>₹92</strong>
            </div>
            <div><span style={{ color: "#f87171" }}>Fixed cost/month:</span> Firebase ~₹500, Misc ~₹500</div>
            <div style={{ color: "#94a3b8", marginTop: "8px" }}>
              {"// Break-even = ₹1000 ÷ ₹92 = ~11 bookings/month"}
            </div>
            <div style={{ color: "#86efac", marginTop: "4px" }}>
              {"// 1 booking/day = ₹92 × 30 = ₹2,760/month profit"}
            </div>
          </div>
        </div>
      </PlaybookSection>

      {/* ─── SECTION 6: Demand-Supply Economics ─── */}
      <PlaybookSection
        title="⚖️ Demand-Supply Economics for DutyPe"
        icon={TrendingUp}
        color="#7c3aed"
        bg="#f3e8ff"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          {[
            {
              title: "Too Little Supply → Customer Waits → Bad Review → Never Returns",
              desc: "If a customer books and waits 2+ hours, they'll never use DutyPe again AND tell 5 people it's bad. Always have more supply than demand in early days.",
              type: "danger"
            },
            {
              title: "Too Much Supply → Workers Idle → Workers Quit → Supply Disappears",
              desc: "If workers sign up and never get bookings, they'll stop checking the app. Call every worker who hasn't had a booking in 5 days and keep them warm.",
              type: "warning"
            },
            {
              title: "Pricing Power — As You Grow, You Control Rates",
              desc: "Early days: keep prices low to build habit. After 6 months with 50+ bookings/month in Khammam: you can increase commission % by 2-3% without losing workers.",
              type: "success"
            },
            {
              title: "Weekend Surge Is Real — Prepare For It",
              desc: "80% of home service bookings happen Saturday-Sunday 9am-2pm. Make sure your best workers are available these hours. Consider a small weekend bonus (₹50 extra per booking) to incentivize availability.",
              type: "info"
            }
          ].map((item, i) => {
            const colors: Record<string, { bg: string; border: string; icon: string }> = {
              danger: { bg: "#fef2f2", border: "#fecaca", icon: "🚨" },
              warning: { bg: "#fffbeb", border: "#fde68a", icon: "⚠️" },
              success: { bg: "#f0fdf4", border: "#86efac", icon: "✅" },
              info: { bg: "#eff6ff", border: "#bfdbfe", icon: "💡" }
            };
            const c = colors[item.type];
            return (
              <div key={i} style={{ background: c.bg, border: `1px solid ${c.border}`, borderRadius: "10px", padding: "14px" }}>
                <div style={{ fontWeight: 700, fontSize: "13.5px", color: "#0f172a", marginBottom: "6px" }}>
                  {c.icon} {item.title}
                </div>
                <div style={{ fontSize: "13px", color: "#334155", lineHeight: "1.55" }}>{item.desc}</div>
              </div>
            );
          })}
        </div>
      </PlaybookSection>

      {/* ─── SECTION 7: Basic Finance ─── */}
      <PlaybookSection
        title="💰 Basic Finance — What You Must Track"
        icon={DollarSign}
        color="#d97706"
        bg="#fef3c7"
      >
        <div style={{ display: "grid", gap: "14px" }}>
          <InfoRow
            icon={Clock}
            color="#d97706"
            label="Runway"
            value="How many months can you operate at zero revenue? Know this number always. If runway < 3 months, don't spend on marketing — focus on your first 10 bookings and revenue."
          />
          <InfoRow
            icon={DollarSign}
            color="#087f68"
            label="Revenue vs Cash Flow"
            value="Revenue = money earned. Cash Flow = money actually in bank. If customers pay via UPI instantly and you pay workers same day — cash flow = revenue. Keep it simple early."
          />
          <InfoRow
            icon={TrendingUp}
            color="#7c3aed"
            label="Payback Period"
            value="If CAC (cost to acquire one customer) = ₹150, and each booking earns ₹90, you need 2 bookings to recover. If customers only book once → you lose money. Focus on repeat bookings."
          />
          <InfoRow
            icon={BarChart3}
            color="#0284c7"
            label="Don't Spend on Growth Until Unit Economics Are Positive"
            value="First get one area profitable (revenue > costs from that area). THEN reinvest profits to expand. Spending on ads when unit economics are negative = burning money faster."
          />
          <InfoRow
            icon={ShieldCheck}
            color="#e11d48"
            label="Tax & Compliance (Simple Version)"
            value="Register as a proprietorship or LLP. Get GST registration when revenue crosses ₹20L. Keep all UPI transaction records. Pay TDS on payments to workers above ₹30,000/year. Get a CA after 6 months."
          />
        </div>
      </PlaybookSection>

      {/* ─── SECTION 8: Missing Features ─── */}
      <PlaybookSection
        title="🚀 App Features Currently Missing — Build These Next"
        icon={Rocket}
        color="#dc2626"
        bg="#fee2e2"
        defaultOpen={true}
      >
        <div
          style={{
            background: "#fff7ed",
            border: "1px solid #fed7aa",
            borderRadius: "10px",
            padding: "12px 16px",
            marginBottom: "20px",
            fontSize: "13px",
            color: "#9a3412",
            lineHeight: "1.6"
          }}
        >
          🔥 These are the gaps between DutyPe and Urban Company / Swiggy. Each one directly impacts revenue or retention.
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          <FeatureCard
            priority="P0"
            title="Post-Booking Rating Prompt (Customer Side)"
            description="After every completed booking, the customer must be prompted to rate the worker and leave a review. Currently there is no guaranteed rating flow after a service completes. Without ratings, there is no trust signal for future customers."
            impact="Trust, Retention, Worker Quality"
            effort="1 day"
          />
          <FeatureCard
            priority="P0"
            title="Worker Accept/Reject Notification with Timeout"
            description="When a customer books, the nearest worker must get an instant push notification. If they don't accept within 10 minutes, it should auto-assign to the next available worker. No fallback = lost booking."
            impact="Fill Rate, Customer Satisfaction"
            effort="2-3 days"
          />
          <FeatureCard
            priority="P0"
            title="Real-Time Booking Status for Customer"
            description="After booking, customer should see: 'Searching for worker…' → 'Worker found: Raju, AC Technician' → 'Worker on the way' → 'Work in progress' → 'Completed'. Currently the status flow is not clearly shown."
            impact="Customer Experience, Repeat Bookings"
            effort="2 days"
          />
          <FeatureCard
            priority="P1"
            title="Worker Location on Map (Live Tracking)"
            description="Once worker accepts and is en route, customer should see live location of worker on a map (like Swiggy/Zomato delivery tracking). This single feature massively reduces 'where is my worker?' support calls."
            impact="Customer Trust, Support Load Reduction"
            effort="4-5 days"
          />
          <FeatureCard
            priority="P1"
            title="Rescheduling / Cancellation Flow"
            description="Customer cannot currently reschedule a booking for a different time. They can only cancel. A proper reschedule flow with worker re-matching is missing. Cancellations without penalty hurt worker income."
            impact="Flexibility, Worker Retention"
            effort="2 days"
          />
          <FeatureCard
            priority="P1"
            title="Customer Address Book (Save Multiple Addresses)"
            description="Customers should be able to save 'Home', 'Parents House', 'Office' as separate addresses. Right now every booking requires re-entering location. Friction = lower repeat bookings."
            impact="Repeat Bookings, UX"
            effort="1 day"
          />
          <FeatureCard
            priority="P1"
            title="Admin: Service Booking Analytics Dashboard"
            description="Admin panel currently has no view of: total bookings today, fill rate, average response time, bookings by category, revenue this week. Without this data you're flying blind."
            impact="Business Intelligence, Decision Making"
            effort="2-3 days"
          />
          <FeatureCard
            priority="P1"
            title="Worker Earnings Dashboard"
            description="Workers need to see their daily/weekly/monthly earnings, number of completed jobs, and pending payouts clearly in the app. This drives retention — workers who can see their income growth stay."
            impact="Worker Retention"
            effort="1-2 days"
          />
          <FeatureCard
            priority="P1"
            title="Service History for Customers"
            description="Every customer should see a clean list of all their past bookings with service type, worker name, date, amount paid, and ability to rebook the same worker for the same service."
            impact="Repeat Bookings (Rebook = zero CAC)"
            effort="1 day"
          />
          <FeatureCard
            priority="P2"
            title="In-App Chat (Customer ↔ Worker)"
            description="After booking is accepted, customer and worker should be able to message each other within the app without sharing personal phone numbers. This builds safety and professionalism."
            impact="Trust, Safety, Professional Image"
            effort="3-4 days"
          />
          <FeatureCard
            priority="P2"
            title="Scheduled Bookings (Book for Later)"
            description="Customer should be able to book a service for a specific date/time (e.g. 'AC service this Saturday 10am'). Right now only instant bookings are supported."
            impact="Planned Use Cases, Higher Ticket Services"
            effort="2-3 days"
          />
          <FeatureCard
            priority="P2"
            title="Service Packages / Subscription Plans"
            description="Offer monthly packages: 'Home Cleaning 4x/month for ₹999'. This creates recurring revenue, reduces CAC per booking, and creates habit. Used heavily by Urban Company."
            impact="Recurring Revenue, LTV"
            effort="5-7 days"
          />
          <FeatureCard
            priority="P2"
            title="Worker Verification Badge (ID + Selfie)"
            description="Workers should be able to upload Aadhaar/PAN + selfie in-app. After admin verification they get a 'Verified by DutyPe' badge on their profile. This massively builds customer trust in a new city."
            impact="Customer Trust in New Market"
            effort="2 days"
          />
          <FeatureCard
            priority="P2"
            title="SOS / Emergency Contact during Active Booking"
            description="During a home service (stranger in customer's home), there should be a visible SOS button that alerts a designated contact. This is a safety feature that differentiates you from informal booking."
            impact="Safety, Trust for Women Customers"
            effort="1 day"
          />
        </div>
      </PlaybookSection>

      {/* ─── SECTION 8.5: Domestic Cleaning & Dishwashing Logistics, Safety & Geofencing ─── */}
      <PlaybookSection
        title="🧹 Home Services Operations: Cleaning & Dishwashing Logistics, Safety & Zero-Cost Geofencing"
        icon={ShieldCheck}
        color="#0284c7"
        bg="#e0f2fe"
        defaultOpen={true}
      >
        <div style={{ fontSize: "13.5px", color: "#334155", lineHeight: "1.7", marginBottom: "18px" }}>
          Ground-level operational blueprint for launching home cleaning and dishwashing services in Khammam with local women workers, ensuring complete travel safety, zero cash leakage, and ₹0-cost boundary enforcement.
        </div>

        {/* 3 Pillars Grid */}
        <div style={{ display: "grid", gap: "16px", marginBottom: "20px" }}>
          {/* Pillar 1: Worker Travel & Logistics */}
          <div style={{ background: "#f8fafc", border: "1px solid #cbd5e1", borderRadius: "12px", padding: "16px" }}>
            <div style={{ fontWeight: 800, fontSize: "15px", color: "#0369a1", marginBottom: "8px", display: "flex", alignItems: "center", gap: "8px" }}>
              🛵 1. Worker Travel & Logistics in Khammam (Micro-Zone Clustering)
            </div>
            <div style={{ fontSize: "13px", color: "#334155", lineHeight: "1.7" }}>
              <p style={{ margin: "0 0 8px 0" }}>
                <strong>The Reality:</strong> Most domestic cleaning women in Khammam rely on local share-autos (₹15–₹20) or walking. If an order is 7 km away across town, they will decline or arrive exhausted.
              </p>
              <ul style={{ margin: "0", paddingLeft: "18px" }}>
                <li><strong>Micro-Zone Tagging:</strong> Divide Khammam into 3 zones: <em>Zone A (Central / Mayuri / Kaman)</em>, <em>Zone B (Wyra Rd / NSP Colony / Raparthi Nagar)</em>, and <em>Zone C (Khanapuram / Rotary Nagar)</em>. Only dispatch bookings to workers living within 2–3 km of the customer.</li>
                <li><strong>Transparent Travel Allowance:</strong> Add a transparent ₹30–₹40 conveyance fee paid by the customer on the bill that goes 100% to the worker for auto fare.</li>
                <li><strong>Buddy / Squad System for Deep Cleaning:</strong> For multi-hour full house cleaning, dispatch a 2-woman squad or pair with a DutyPe field runner on bike who drops them off safely.</li>
                <li><strong>Strict Daytime Window:</strong> Restrict home domestic cleaning duties to <strong>8:00 AM – 6:30 PM only</strong> for worker security.</li>
              </ul>
            </div>
          </div>

          {/* Pillar 2: Commission & Money Safety */}
          <div style={{ background: "#f0fdf4", border: "1px solid #86efac", borderRadius: "12px", padding: "16px" }}>
            <div style={{ fontWeight: 800, fontSize: "15px", color: "#15803d", marginBottom: "8px", display: "flex", alignItems: "center", gap: "8px" }}>
              🛡️ 2. Bulletproof Commission & Anti-Leakage System
            </div>
            <div style={{ fontSize: "13px", color: "#1e3a8a", lineHeight: "1.7" }}>
              <p style={{ margin: "0 0 8px 0", color: "#166534" }}>
                <strong>The Problem:</strong> In maid/cleaning services, cash leakage happens when customers pay the maid directly, bypassing DutyPe commission or dealing privately.
              </p>
              <ul style={{ margin: "0", paddingLeft: "18px", color: "#166534" }}>
                <li><strong>Digital Pre-Payment / Escrow (Primary):</strong> Customer pays via UPI (PhonePe / GPay) upfront. DutyPe holds funds, releases 80% to worker upon completion OTP, and keeps 20% commission automatically. Zero chasing needed.</li>
                <li><strong>Prepaid Security Wallet (For Cash Orders):</strong> If Cash on Delivery is allowed, deduct DutyPe&apos;s 20% commission from the partner&apos;s DutyPe wallet balance upfront when the job is accepted. The worker keeps 100% of the customer&apos;s cash. If the wallet drops below ₹0, new jobs freeze until a top-up.</li>
                <li><strong>Stopping Private Off-Platform Hiring:</strong> Market the <em>DutyPe Guarantee</em>: If a maid falls sick, DutyPe sends an instant replacement in 30 minutes; private maids leave customers stranded. Give workers milestone bonuses (e.g. ₹400 bonus on 12 verified weekly jobs) so they prefer the app.</li>
              </ul>
            </div>
          </div>

          {/* Pillar 3: Zero-Cost Geofencing */}
          <div style={{ background: "#eff6ff", border: "1px solid #93c5fd", borderRadius: "12px", padding: "16px" }}>
            <div style={{ fontWeight: 800, fontSize: "15px", color: "#1d4ed8", marginBottom: "8px", display: "flex", alignItems: "center", gap: "8px" }}>
              📍 3. Zero-Cost Geofencing: Haversine Formula + Leaflet OSM (₹0 API Bills)
            </div>
            <div style={{ fontSize: "13px", color: "#1e3a8a", lineHeight: "1.7" }}>
              <p style={{ margin: "0 0 8px 0" }}>
                <strong>Zero API Cost:</strong> Google Maps distance matrix charges $5–$10 per 1,000 calls. DutyPe computes exact spherical distance locally on device and in Cloud Functions in 0.001ms with Haversine math at <strong>₹0 cost forever</strong>.
              </p>
              <ul style={{ margin: "0", paddingLeft: "18px" }}>
                <li><strong>Center & Radius:</strong> Khammam City Center: <code>17.2473° N, 80.1514° E</code> with a 15.0 km radius covering all urban zones.</li>
                <li><strong>Worker Duty Lock:</strong> Workers outside 15 km cannot go online and see a polite <em>&quot;Outside Serviceable Area&quot;</em> bottom sheet.</li>
                <li><strong>Customer Waitlist Lead Capture:</strong> Out-of-area customers get an encouraging <em>&quot;Coming Soon to Your Area&quot;</em> card with a &quot;Notify Me&quot; button that records demand hotspots for future expansion.</li>
                <li><strong>Admin Visual Map:</strong> Interactive Leaflet.js + OpenStreetMap displays green/yellow/red booking pins and dynamic radius circles with zero commercial license fees.</li>
              </ul>
            </div>
          </div>
        </div>
      </PlaybookSection>

      {/* ─── SECTION 9: Daily Habits ─── */}
      <PlaybookSection
        title="🌅 Daily Founder Habits — Do These Every Day"
        icon={Clock}
        color="#087f68"
        bg="#e8f5ef"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          {[
            "Check: How many bookings happened yesterday? What was the fill rate?",
            "Call any worker who hasn't been active in 5 days. Keep them warm.",
            "Reply to every customer support message within 2 hours",
            "Post one piece of content on Instagram (before/after service, tip, testimonial)",
            "Track your app's Firebase console — any errors? Any crashes?",
            "Ask one happy customer: 'Did you refer anyone?'",
            "Review your Firestore: any bookings stuck in 'pending' for > 2 hours?",
            "Check if any new worker signed up — call them personally and onboard them",
          ].map((habit, i) => (
            <div
              key={i}
              style={{
                display: "flex",
                alignItems: "flex-start",
                gap: "10px",
                padding: "10px 12px",
                background: "#f8fafc",
                borderRadius: "8px",
                fontSize: "13px",
                color: "#334155"
              }}
            >
              <span style={{ color: "#087f68", fontWeight: 700, fontSize: "12px", flexShrink: 0, marginTop: "1px" }}>
                {String(i + 1).padStart(2, "0")}
              </span>
              {habit}
            </div>
          ))}
        </div>
      </PlaybookSection>

      {/* Footer */}
      <div
        style={{
          background: "#f8fafc",
          borderRadius: "12px",
          padding: "20px",
          textAlign: "center",
          border: "1px solid #e2e8f0",
          color: "#64748b",
          fontSize: "13px",
          lineHeight: "1.7"
        }}
      >
        <AlertTriangle size={18} style={{ color: "#d97706", marginBottom: "8px", display: "block", margin: "0 auto 8px" }} />
        <strong style={{ color: "#0f172a" }}>The #1 mistake early founders make:</strong> Building features instead of getting customers.<br />
        After reading this — close the laptop. Go talk to 10 people in Khammam today.
        <br /><br />
        <strong style={{ color: "#087f68" }}>First goal: 1 booking. Not 100. Not 10. Just 1. Then 2. Then 5. Then 10/day.</strong>
      </div>
    </div>
  );
}
