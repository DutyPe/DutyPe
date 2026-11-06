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
  DollarSign,
  PhoneCall,
  Flame,
  Check
} from "lucide-react";

type BroadcastMode = "EMPLOYER_OFFER" | "GENERAL_PROMO";
type EmployerTarget = "ALL_FIRESTORE" | "CUSTOM_LIST";
type AudienceTarget = "EMPLOYERS" | "WORKERS" | "BOTH";
type CityFilter = "ALL" | "Khammam" | "Hyderabad" | "Warangal" | "Vijayawada" | "Visakhapatnam" | "Guntur" | "Tirupati" | "Karimnagar" | "Nizamabad" | "Kothagudem" | "CUSTOM";
type TemplatePreset = "QUICK_HIRE" | "FLIPKART_SAVINGS" | "ZOMATO_SHORTAGE" | "AMAZON_VOLUME" | "CUSTOM";
type Audience = "ALL" | "WORKER" | "EMPLOYER" | "PARTNER";

type Result = {
  ok?: boolean;
  dryRun: boolean;
  matched: number;
  due?: number;
  willSend: number;
  teluguCount?: number;
  englishCount?: number;
  employerCount?: number;
  workerCount?: number;
  skippedRecent?: number;
  ready?: boolean;
  sent?: number;
  failed?: number;
  errors?: string[];
  phone?: string;
  template?: string;
  params?: string[];
  sample?: Array<{ phone: string; name: string; city: string; role?: string; isTeluguState?: boolean }>;
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
  const [mode, setMode] = useState<BroadcastMode>("EMPLOYER_OFFER");

  // Employer Launch Offer State
  const [preset, setPreset] = useState<TemplatePreset>("QUICK_HIRE");
  const [empTemplate, setEmpTemplate] = useState("dutype_employer_quick_hire");
  const [englishTemplate, setEnglishTemplate] = useState("");
  const [routingMode, setRoutingMode] = useState<"AUTO" | "TELUGU_ONLY" | "ENGLISH_ONLY" | "ALL_TELUGU" | "ALL_ENGLISH">("AUTO");
  const [audienceTarget, setAudienceTarget] = useState<AudienceTarget>("EMPLOYERS");
  const [targetState, setTargetState] = useState<"ALL" | "TELANGANA">("TELANGANA");
  const [targetCity, setTargetCity] = useState<CityFilter>("ALL");
  const [customCityInput, setCustomCityInput] = useState("");
  const [empLang, setEmpLang] = useState("en");
  const [paramCount, setParamCount] = useState<"0" | "1" | "2">("2");
  const [headerType, setHeaderType] = useState<"AUTO" | "IMAGE" | "NONE">("AUTO");
  const [headerImageUrl, setHeaderImageUrl] = useState("");
  const [empName, setEmpName] = useState("Business Owner");
  const [empCity, setEmpCity] = useState("Telangana");
  const [empLink, setEmpLink] = useState("https://dutype.in/employer/post-job");
  const [testPhone, setTestPhone] = useState("");
  const [empTarget, setEmpTarget] = useState<EmployerTarget>("ALL_FIRESTORE");
  const [customPhonesText, setCustomPhonesText] = useState("");
  const [fetchingTg, setFetchingTg] = useState(false);
  const [tgEmployersResult, setTgEmployersResult] = useState<{ count: number; sample: Array<{ phone: string; name: string; city: string; area: string; source: string }> } | null>(null);

  function applyPreset(p: TemplatePreset) {
    setPreset(p);
    if (p === "QUICK_HIRE" || p === "FLIPKART_SAVINGS") {
      setEmpTemplate("dutype_employer_quick_hire");
      setEnglishTemplate("");
      setParamCount("2");
    } else if (p === "ZOMATO_SHORTAGE") {
      setEmpTemplate("dutype_employer_staff_shortage");
      setEnglishTemplate("");
      setParamCount("2");
    } else if (p === "AMAZON_VOLUME") {
      setEmpTemplate("dutype_employer_launch_offer");
      setEnglishTemplate("");
      setParamCount("2");
    }
  }

  async function fetchTelanganaEmployersAction() {
    setError("");
    setFetchingTg(true);
    try {
      const services = getFirebaseServices();
      if (!services) throw new Error("Firebase client is not configured.");
      const res = await httpsCallable(services.functions, "fetchTelanganaEmployers")({});
      const data = res.data as { ok: boolean; count: number; employers: Array<{ phone: string; name: string; city: string; area: string; source: string }> };
      if (data.ok && data.employers) {
        const phones = data.employers.map((e) => e.phone).join("\n");
        setCustomPhonesText(phones);
        setEmpTarget("CUSTOM_LIST");
        setTargetState("TELANGANA");
        setRoutingMode("ALL_TELUGU");
        setEmpTemplate("dutype_employer_quick_hire");
        setTgEmployersResult({
          count: data.count,
          sample: data.employers.slice(0, 10)
        });
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to fetch Telangana employers.");
    } finally {
      setFetchingTg(false);
    }
  }

  // General Promo State
  const [promoTemplate, setPromoTemplate] = useState("dasara_festival_special_2026");
  const [promoLangs, setPromoLangs] = useState("te,en");
  const [promoParams, setPromoParams] = useState("FESTIVAL50\nValid till Sunday");
  const [promoAudience, setPromoAudience] = useState<Audience>("EMPLOYER");
  const [promoMax, setPromoMax] = useState(500);

  // Status & Logs
  const [busy, setBusy] = useState(false);
  const [testResult, setTestResult] = useState<Result | null>(null);
  const [bulkResult, setBulkResult] = useState<Result | null>(null);
  const [error, setError] = useState("");
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);

  useEffect(() => {
    const services = getFirebaseServices();
    if (!services) return;
    const q = query(collection(services.db, "whatsapp_campaigns"), orderBy("createdAt", "desc"), limit(20));
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

  // Send Single Test WhatsApp Message
  async function sendSingleTest() {
    setError("");
    setTestResult(null);
    if (!testPhone.trim()) {
      setError("Please enter a test phone number (e.g. 9876543210).");
      return;
    }
    setBusy(true);
    try {
      const services = getFirebaseServices();
      if (!services) throw new Error("Firebase client is not configured.");
      const computedParams = paramCount === "0" ? [] : paramCount === "1" ? [empName.trim()] : [empName.trim(), empCity.trim()];
      const res = await httpsCallable(
        services.functions,
        "sendWhatsappEmployerBroadcast"
      )({
        isSingleTest: true,
        testPhone: testPhone.trim(),
        template: empTemplate.trim(),
        lang: empLang.trim(),
        name: empName.trim(),
        city: empCity.trim(),
        postJobUrl: empLink.trim(),
        params: computedParams,
        headerType,
        headerImageUrl: headerImageUrl.trim() || undefined,
        dryRun: false,
      });
      setTestResult(res.data as Result);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Test message failed.");
    } finally {
      setBusy(false);
    }
  }

  // Send Bulk Employer Broadcast
  async function runEmployerBroadcast(dryRun: boolean) {
    setError("");
    const parsedCustom = customPhonesText
      .split(/[\n,]/)
      .map((p) => p.trim())
      .filter((p) => p.length >= 10);

    if (empTarget === "CUSTOM_LIST" && parsedCustom.length === 0) {
      setError("Please paste at least one valid phone number in the custom list.");
      return;
    }

    if (
      !dryRun &&
      !window.confirm(
        `Are you sure you want to broadcast "${empTemplate}" to ${
          empTarget === "CUSTOM_LIST" ? parsedCustom.length + " custom numbers" : "all registered employers"
        }? This will consume official WhatsApp marketing conversation credits.`
      )
    ) {
      return;
    }

    setBusy(true);
    try {
      const services = getFirebaseServices();
      if (!services) throw new Error("Firebase client is not configured.");
      const computedParams = paramCount === "0" ? [] : paramCount === "1" ? [empName.trim()] : [empName.trim(), empCity.trim()];
      const activeCity = targetCity === "CUSTOM" ? customCityInput.trim() : targetCity;
      const res = await httpsCallable(
        services.functions,
        "sendWhatsappEmployerBroadcast"
      )({
        isSingleTest: false,
        template: empTemplate.trim(),
        englishTemplate: englishTemplate.trim() || undefined,
        routingMode,
        audienceTarget: empTarget === "CUSTOM_LIST" ? "EMPLOYERS" : audienceTarget,
        targetState: empTarget === "CUSTOM_LIST" ? "ALL" : targetState,
        targetCity: activeCity,
        lang: empLang.trim(),
        name: empName.trim(),
        city: empCity.trim(),
        postJobUrl: empLink.trim(),
        params: computedParams,
        headerType,
        headerImageUrl: headerImageUrl.trim() || undefined,
        customPhones: empTarget === "CUSTOM_LIST" ? parsedCustom : [],
        dryRun,
      });
      setBulkResult(res.data as Result);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Broadcast failed.");
    } finally {
      setBusy(false);
    }
  }

  // Run General Festival Promo
  async function runGeneralPromo(dryRun: boolean) {
    setError("");
    const cleanTemplate = promoTemplate.trim();
    if (!/^[a-z0-9_]+$/.test(cleanTemplate)) {
      setError("Template name format invalid: small letters, numbers, and underscores only.");
      return;
    }

    if (
      !dryRun &&
      !window.confirm(
        `Are you sure you want to broadcast "${cleanTemplate}" to up to ${promoMax} users?`
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
        langs: promoLangs,
        params: promoParams
          .split("\n")
          .map((p) => p.trim())
          .filter(Boolean),
        audience: promoAudience,
        limit: promoMax,
        dryRun,
      });
      setBulkResult(res.data as Result);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Campaign trigger failed.");
    } finally {
      setBusy(false);
    }
  }

  const customPhoneCount = useMemo(() => {
    return customPhonesText
      .split(/[\n,]/)
      .map((p) => p.trim())
      .filter((p) => p.length >= 10).length;
  }, [customPhonesText]);

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
          <h1 className="adm-page-title">WhatsApp Broadcast Studio</h1>
          <p className="adm-page-subtitle">
            Send approved Meta WhatsApp templates to employers and candidates with zero friction deep-linking.
          </p>
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          <span className="adm-badge adm-badge-emerald" style={{ padding: "6px 12px", fontSize: "0.8rem" }}>
            <ShieldCheck style={{ width: 14, height: 14 }} />
            Meta Cloud API Verified &middot; WABA: 1285842094620024
          </span>
        </div>
      </div>

      {/* Tab Switcher */}
      <div style={{ display: "flex", gap: 8, borderBottom: "1px solid var(--adm-border)", paddingBottom: 10 }}>
        <button
          type="button"
          onClick={() => { setMode("EMPLOYER_OFFER"); setError(""); setBulkResult(null); }}
          className={`adm-btn ${mode === "EMPLOYER_OFFER" ? "adm-btn-primary" : "adm-btn-secondary"}`}
          style={{ display: "flex", alignItems: "center", gap: 8 }}
        >
          <Flame style={{ width: 16, height: 16, color: mode === "EMPLOYER_OFFER" ? "#ffffff" : "#f59e0b" }} />
          Employer Launch Offer (48-Hr Trial / ₹49)
        </button>
        <button
          type="button"
          onClick={() => { setMode("GENERAL_PROMO"); setError(""); setBulkResult(null); }}
          className={`adm-btn ${mode === "GENERAL_PROMO" ? "adm-btn-primary" : "adm-btn-secondary"}`}
          style={{ display: "flex", alignItems: "center", gap: 8 }}
        >
          <Sparkles style={{ width: 16, height: 16, color: mode === "GENERAL_PROMO" ? "#ffffff" : "#8b5cf6" }} />
          General Marketing &amp; Festival Promos
        </button>
      </div>

      {/* Mode 1: Employer Launch Offer Broadcast Studio */}
      {mode === "EMPLOYER_OFFER" && (
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          {/* Hero Action: Fast Target Telangana Employers */}
          <div className="adm-card" style={{ borderLeft: "4px solid #10b981", background: "linear-gradient(135deg, #ecfdf5 0%, #ffffff 100%)", padding: "16px 20px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 14 }}>
              <div>
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <span className="adm-badge adm-badge-emerald" style={{ fontWeight: 700, padding: "3px 8px" }}>Primary Growth Target</span>
                  <h3 style={{ fontSize: "1.05rem", fontWeight: 700, margin: 0, color: "#065f46" }}>
                    Target Telangana State Employers
                  </h3>
                </div>
                <p style={{ margin: "5px 0 0", fontSize: "0.825rem", color: "#047857" }}>
                  Fetches all registered and verified employers across Hyderabad, Khammam, Warangal, Karimnagar, Nizamabad, Suryapet, and active business job posters into the broadcast list.
                </p>
              </div>
              <button
                type="button"
                className="adm-btn adm-btn-primary"
                disabled={fetchingTg || busy}
                onClick={fetchTelanganaEmployersAction}
                style={{
                  background: "linear-gradient(135deg, #10b981 0%, #059669 100%)",
                  display: "flex",
                  alignItems: "center",
                  gap: 8,
                  padding: "10px 18px",
                  fontWeight: 700,
                  boxShadow: "0 2px 8px rgba(5,150,105,0.25)",
                  cursor: "pointer",
                  color: "#ffffff"
                }}
              >
                <RefreshCw style={{ width: 16, height: 16, animation: fetchingTg ? "spin 1s linear infinite" : "none" }} />
                {fetchingTg ? "Fetching Telangana Employers..." : "🎯 Fetch Telangana Employers"}
              </button>
            </div>
            {tgEmployersResult && (
              <div style={{ marginTop: 12, padding: "10px 14px", background: "#ffffff", borderRadius: 8, border: "1px solid #a7f3d0", fontSize: "0.825rem", color: "#065f46" }}>
                <strong>🎉 Successfully fetched {tgEmployersResult.count} Telangana Employers!</strong> Numbers loaded into the broadcast list with template <code>dutype_employer_quick_hire</code>.
                <div style={{ marginTop: 6, fontSize: "0.75rem", color: "#047857" }}>
                  Top locations: {Array.from(new Set(tgEmployersResult.sample.map((s) => s.city))).slice(0, 7).join(", ")} and more.
                </div>
              </div>
            )}
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))", gap: 20 }}>
          {/* Left Column: Test & Bulk Controls */}
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            {/* Box 1: Single Phone Test */}
            <div className="adm-card" style={{ borderLeft: "4px solid #3b82f6" }}>
              <div className="adm-card-header">
                <h2 className="adm-card-title">
                  <PhoneCall style={{ width: 18, height: 18, color: "#3b82f6" }} />
                  1. Send Single Test WhatsApp Message
                </h2>
                <span className="adm-badge adm-badge-blue">Test Single Number</span>
              </div>
              <div className="adm-card-body" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                <p style={{ fontSize: "0.825rem", color: "var(--adm-text-muted)" }}>
                  Verify the exact template delivery on your personal phone before broadcasting to the entire database.
                </p>

                <div className="adm-form-group">
                  <label className="adm-form-label">Recipient Test Phone Number *</label>
                  <div style={{ display: "flex", gap: 8 }}>
                    <input
                      type="text"
                      className="adm-input"
                      placeholder="e.g. 9876543210 or +919876543210"
                      value={testPhone}
                      onChange={(e) => setTestPhone(e.target.value)}
                      style={{ flex: 1 }}
                    />
                    <button
                      type="button"
                      className="adm-btn adm-btn-primary"
                      disabled={busy || !testPhone.trim()}
                      onClick={sendSingleTest}
                      style={{ background: "#2563eb", whiteSpace: "nowrap" }}
                    >
                      <Send style={{ width: 15, height: 15 }} />
                      Send 1 Test
                    </button>
                  </div>
                </div>

                {testResult && (
                  <div
                    style={{
                      padding: "10px 14px",
                      borderRadius: 8,
                      background: testResult.ok ? "#ecfdf5" : "#fef2f2",
                      border: `1px solid ${testResult.ok ? "#a7f3d0" : "#fecaca"}`,
                      color: testResult.ok ? "#065f46" : "#991b1b",
                      fontSize: "0.825rem",
                      display: "flex",
                      alignItems: "center",
                      gap: 8,
                    }}
                  >
                    <CheckCircle2 style={{ width: 16, height: 16, flexShrink: 0 }} />
                    <span>
                      {testResult.ok
                        ? `Success! WhatsApp template sent to ${testResult.phone}. Check your phone now.`
                        : "Message sending failed."}
                    </span>
                  </div>
                )}
              </div>
            </div>

            {/* Box 2: Bulk Broadcast */}
            <div className="adm-card" style={{ borderLeft: "4px solid #10b981" }}>
              <div className="adm-card-header">
                <h2 className="adm-card-title">
                  <Users style={{ width: 18, height: 18, color: "#10b981" }} />
                  2. Broadcast to Employers (Bulk)
                </h2>
                <span className="adm-badge adm-badge-emerald">Meta Approved Template</span>
              </div>
              <div className="adm-card-body" style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                {/* Campaign Template Strategy Preset */}
                <div className="adm-form-group">
                  <label className="adm-form-label">
                    <span>Campaign Template Strategy</span>
                    <span className="adm-badge adm-badge-emerald" style={{ fontSize: "0.7rem", padding: "2px 6px" }}>Primary</span>
                  </label>
                  <select
                    className="adm-select"
                    value={preset}
                    onChange={(e) => applyPreset(e.target.value as TemplatePreset)}
                  >
                    <option value="QUICK_HIRE">🚀 Quick Hire Hook: dutype_employer_quick_hire (₹49 / 0% Agency Fee / 48-Hr Trial) [Meta Approved]</option>
                    <option value="FLIPKART_SAVINGS">🌟 Flipkart-Style: Massive Savings Hook (₹49 vs Agency Fee)</option>
                    <option value="ZOMATO_SHORTAGE">⏰ Zomato-Style: Staff Shortage Hook (Immediate Need)</option>
                    <option value="AMAZON_VOLUME">⚡ Amazon-Style: Volume Alert (500+ Verified Workers)</option>
                    <option value="CUSTOM">⚙️ Custom Template ID</option>
                  </select>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
                  <div className="adm-form-group">
                    <label className="adm-form-label">Primary Template ID *</label>
                    <input
                      type="text"
                      className="adm-input"
                      value={empTemplate}
                      onChange={(e) => setEmpTemplate(e.target.value)}
                      placeholder="e.g. dutype_employer_quick_hire"
                    />
                    <span style={{ fontSize: "0.72rem", color: "#64748b" }}>Telugu template or unified template name</span>
                  </div>
                  <div className="adm-form-group">
                    <label className="adm-form-label">English Template ID (Optional)</label>
                    <input
                      type="text"
                      className="adm-input"
                      value={englishTemplate}
                      onChange={(e) => setEnglishTemplate(e.target.value)}
                      placeholder="Only if using 2 separate templates"
                    />
                    <span style={{ fontSize: "0.72rem", color: "#64748b" }}>Leave blank if using 1 template with 2 languages</span>
                  </div>
                </div>

                <div className="adm-form-group">
                  <label className="adm-form-label">Language &amp; Regional State Routing *</label>
                  <select
                    className="adm-select"
                    value={routingMode}
                    onChange={(e) => setRoutingMode(e.target.value as any)}
                  >
                    <option value="AUTO">✨ Auto-Detect by State (AP &amp; Telangana in Telugu, Others in English)</option>
                    <option value="TELUGU_ONLY">🎯 Filter: AP &amp; Telangana Profiles Only (in Telugu)</option>
                    <option value="ENGLISH_ONLY">🌐 Filter: Other States Only (in English)</option>
                    <option value="ALL_TELUGU">All in Telugu (te)</option>
                    <option value="ALL_ENGLISH">All in English (en)</option>
                  </select>
                  <span style={{ fontSize: "0.75rem", color: "#64748b" }}>
                    {routingMode === "AUTO"
                      ? "Automatically detects profile state/city: Telangana & AP receive Telugu, other metros receive English."
                      : routingMode === "TELUGU_ONLY"
                      ? "Only targets profiles located in Andhra Pradesh & Telangana."
                      : "Sends using the selected language configuration."}
                  </span>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
                  <div className="adm-form-group">
                    <label className="adm-form-label">Header Media (Image)</label>
                    <select
                      className="adm-select"
                      value={headerType}
                      onChange={(e) => setHeaderType(e.target.value as "AUTO" | "IMAGE" | "NONE")}
                    >
                      <option value="AUTO">✨ Auto-Detect (Branded Banner for quick_hire)</option>
                      <option value="IMAGE">🖼️ Required Image Header (Meta Media Template)</option>
                      <option value="NONE">🚫 None (Plain Text Template, No Media Header)</option>
                    </select>
                    <span style={{ fontSize: "0.72rem", color: "#64748b" }}>Required if template was created with an Image Header (prevents error 132012)</span>
                  </div>
                  <div className="adm-form-group">
                    <label className="adm-form-label">Custom Image URL (Optional)</label>
                    <input
                      type="text"
                      className="adm-input"
                      value={headerImageUrl}
                      onChange={(e) => setHeaderImageUrl(e.target.value)}
                      placeholder="Leave blank for official DutyPe 16:9 banner"
                    />
                    <span style={{ fontSize: "0.72rem", color: "#64748b" }}>Defaults to https://dutype-860ac.web.app/dutype_branded_marketing_banner.jpg</span>
                  </div>
                </div>

                <div className="adm-form-group">
                  <label className="adm-form-label">Template Parameters (Meta Configuration)</label>
                  <select
                    className="adm-select"
                    value={paramCount}
                    onChange={(e) => setParamCount(e.target.value as "0" | "1" | "2")}
                  >
                    <option value="2">2 Parameters: &#123;&#123;1&#125;&#125; Name + &#123;&#123;2&#125;&#125; City</option>
                    <option value="1">1 Parameter: &#123;&#123;1&#125;&#125; Name only</option>
                    <option value="0">0 Parameters: Static Template (No variables)</option>
                  </select>
                  <span style={{ fontSize: "0.75rem", color: "#64748b" }}>
                    Select &quot;0 Parameters&quot; if your Meta template has no curly braces, preventing error 132000.
                  </span>
                </div>

                {/* Parameters */}
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
                  <div className="adm-form-group">
                    <label className="adm-form-label">Fallback Name Parameter &#123;&#123;1&#125;&#125;</label>
                    <input
                      type="text"
                      className="adm-input"
                      value={empName}
                      onChange={(e) => setEmpName(e.target.value)}
                      placeholder="Employer / Business Owner"
                    />
                  </div>
                  <div className="adm-form-group">
                    <label className="adm-form-label">Fallback City Parameter &#123;&#123;2&#125;&#125;</label>
                    <input
                      type="text"
                      className="adm-input"
                      value={empCity}
                      onChange={(e) => setEmpCity(e.target.value)}
                      placeholder="Khammam"
                    />
                  </div>
                </div>

                <div className="adm-form-group">
                  <label className="adm-form-label">Deep Link URL &#123;&#123;3&#125;&#125; (Zero-Friction CTA)</label>
                  <input
                    type="text"
                    className="adm-input"
                    value={empLink}
                    onChange={(e) => setEmpLink(e.target.value)}
                  />
                </div>

                {/* Target Audience Source */}
                <div className="adm-form-group">
                  <label className="adm-form-label">Target Audience Source</label>
                  <div style={{ display: "flex", gap: 14, marginTop: 4 }}>
                    <label style={{ display: "flex", alignItems: "center", gap: 6, fontSize: "0.85rem", cursor: "pointer" }}>
                      <input
                        type="radio"
                        checked={empTarget === "ALL_FIRESTORE"}
                        onChange={() => setEmpTarget("ALL_FIRESTORE")}
                      />
                      <span>Registered Profiles in Database</span>
                    </label>
                    <label style={{ display: "flex", alignItems: "center", gap: 6, fontSize: "0.85rem", cursor: "pointer" }}>
                      <input
                        type="radio"
                        checked={empTarget === "CUSTOM_LIST"}
                        onChange={() => setEmpTarget("CUSTOM_LIST")}
                      />
                      <span>Custom Phone Numbers List</span>
                    </label>
                  </div>
                </div>

                {/* Database Target Options: Role & Exact City */}
                {empTarget === "ALL_FIRESTORE" && (
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, background: "var(--adm-surface-subtle)", padding: 12, borderRadius: 8, border: "1px solid var(--adm-border)" }}>
                    <div className="adm-form-group">
                      <label className="adm-form-label">Database Profile Group</label>
                      <select
                        className="adm-select"
                        value={audienceTarget}
                        onChange={(e) => setAudienceTarget(e.target.value as AudienceTarget)}
                      >
                        <option value="EMPLOYERS">Employers Only (employer_profiles)</option>
                        <option value="WORKERS">Workers Only (worker_profiles)</option>
                        <option value="BOTH">Both Employers &amp; Workers</option>
                      </select>
                    </div>
                    <div className="adm-form-group">
                      <label className="adm-form-label">Exact City / Location Filter</label>
                      <select
                        className="adm-select"
                        value={targetCity}
                        onChange={(e) => setTargetCity(e.target.value as CityFilter)}
                      >
                        <option value="ALL">All Cities &amp; Districts</option>
                        <option value="Khammam">Khammam (District &amp; Town)</option>
                        <option value="Hyderabad">Hyderabad &amp; Secunderabad</option>
                        <option value="Warangal">Warangal / Hanamkonda</option>
                        <option value="Vijayawada">Vijayawada &amp; Krishna</option>
                        <option value="Visakhapatnam">Visakhapatnam (Vizag)</option>
                        <option value="Guntur">Guntur &amp; Amaravati</option>
                        <option value="Tirupati">Tirupati</option>
                        <option value="Karimnagar">Karimnagar</option>
                        <option value="Nizamabad">Nizamabad</option>
                        <option value="Kothagudem">Kothagudem &amp; Bhadrachalam</option>
                        <option value="CUSTOM">Custom City...</option>
                      </select>
                    </div>
                  </div>
                )}

                {empTarget === "ALL_FIRESTORE" && targetCity === "CUSTOM" && (
                  <div className="adm-form-group">
                    <label className="adm-form-label">Custom City Name to Filter</label>
                    <input
                      type="text"
                      className="adm-input"
                      placeholder="e.g. Suryapet, Nalgonda, Ongole"
                      value={customCityInput}
                      onChange={(e) => setCustomCityInput(e.target.value)}
                    />
                  </div>
                )}

                {empTarget === "CUSTOM_LIST" && (
                  <div className="adm-form-group">
                    <label className="adm-form-label">
                      <span>Paste Phone Numbers (One per line or comma-separated)</span>
                      <span style={{ fontSize: "0.75rem", fontWeight: 700, color: "var(--adm-primary)" }}>
                        {customPhoneCount} numbers detected
                      </span>
                    </label>
                    <textarea
                      className="adm-textarea"
                      rows={4}
                      placeholder="9876543210&#10;9988776655&#10;+919123456789"
                      value={customPhonesText}
                      onChange={(e) => setCustomPhonesText(e.target.value)}
                    />
                  </div>
                )}

                {/* Bulk Actions */}
                <div style={{ display: "flex", gap: 10, marginTop: 4 }}>
                  <button
                    type="button"
                    className="adm-btn adm-btn-secondary"
                    disabled={busy}
                    onClick={() => runEmployerBroadcast(true)}
                    style={{ flex: 1 }}
                  >
                    <Calculator style={{ width: 15, height: 15 }} />
                    Dry Run / Count
                  </button>
                  <button
                    type="button"
                    className="adm-btn adm-btn-primary"
                    disabled={busy || !bulkResult?.dryRun || !bulkResult.willSend}
                    onClick={() => runEmployerBroadcast(false)}
                    style={{ flex: 1, background: "#10b981" }}
                  >
                    <Send style={{ width: 15, height: 15 }} />
                    Broadcast ({bulkResult?.willSend ?? "Bulk"})
                  </button>
                </div>

                {/* Bulk Result Card */}
                {bulkResult && (
                  <div
                    style={{
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
                        {bulkResult.dryRun ? "Dry Run Verification Summary" : "Broadcast Execution Summary"}
                      </span>
                      <span className={`adm-badge ${bulkResult.dryRun ? "adm-badge-blue" : "adm-badge-emerald"}`}>
                        {bulkResult.dryRun ? "Ready to Send" : "Sent"}
                      </span>
                    </div>
                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
                      <div>Total Verified Targets: <strong>{bulkResult.willSend ?? bulkResult.matched}</strong></div>
                      <div>Estimated Meta Cost: <strong>≈ ₹{((bulkResult.willSend ?? bulkResult.matched) * 1.1).toFixed(0)}</strong></div>
                      {bulkResult.teluguCount !== undefined && (
                        <div>Telugu (AP &amp; TG): <strong>{bulkResult.teluguCount}</strong></div>
                      )}
                      {bulkResult.englishCount !== undefined && (
                        <div>English (Other States): <strong>{bulkResult.englishCount}</strong></div>
                      )}
                      {bulkResult.employerCount !== undefined && (
                        <div>Employers: <strong>{bulkResult.employerCount}</strong></div>
                      )}
                      {bulkResult.workerCount !== undefined && (
                        <div>Workers: <strong>{bulkResult.workerCount}</strong></div>
                      )}
                    </div>

                    {/* Sample Recipients Table */}
                    {bulkResult.sample && bulkResult.sample.length > 0 && (
                      <div style={{ marginTop: 6, borderTop: "1px solid var(--adm-border)", paddingTop: 8 }}>
                        <div style={{ fontSize: "0.75rem", fontWeight: 600, color: "var(--adm-text-muted)", marginBottom: 4 }}>
                          Sample Recipients ({bulkResult.sample.length}):
                        </div>
                        <div style={{ maxHeight: 140, overflowY: "auto", fontSize: "0.72rem" }}>
                          {bulkResult.sample.map((s, idx) => (
                            <div key={idx} style={{ display: "flex", justifyContent: "space-between", padding: "3px 0", borderBottom: "1px dashed var(--adm-border)" }}>
                              <span><strong>{s.name}</strong> ({s.phone})</span>
                              <span style={{ color: "var(--adm-primary)", fontWeight: 600 }}>
                                📍 {s.city} &middot; <span style={{ textTransform: "capitalize" }}>{s.role?.toLowerCase()}</span>
                              </span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {!bulkResult.dryRun && (
                      <div style={{ display: "flex", gap: 14, paddingTop: 4, borderTop: "1px solid var(--adm-border)" }}>
                        <span style={{ color: "#059669", fontWeight: 600 }}>Sent: {bulkResult.sent}</span>
                        <span style={{ color: "#dc2626", fontWeight: 600 }}>Failed: {bulkResult.failed}</span>
                      </div>
                    )}
                    {bulkResult.errors && bulkResult.errors.length > 0 && (
                      <div style={{ color: "#b91c1c", fontSize: "0.75rem", background: "#fef2f2", padding: 8, borderRadius: 6 }}>
                        {bulkResult.errors.join("\n")}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Right Column: WhatsApp Live Message Preview */}
          <div className="adm-card" style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: 24, background: "#f8fafc" }}>
            <div className="adm-phone-frame" style={{ maxWidth: 360, width: "100%" }}>
              <div className="adm-phone-topbar">
                <div style={{ width: 34, height: 34, borderRadius: "50%", background: "#087f68", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 700, fontSize: "0.8rem", color: "#ffffff" }}>
                  DP
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 600, fontSize: "0.875rem", lineHeight: 1.2 }}>DutyPe Hiring</div>
                  <div style={{ fontSize: "0.7rem", color: "#8696a0" }}>Verified Business Account</div>
                </div>
              </div>

              <div className="adm-phone-chat-bg" style={{ padding: 14 }}>
                <div className="adm-wa-bubble" style={{ fontSize: "0.85rem", lineHeight: 1.45 }}>
                  <div style={{ fontSize: "0.7rem", color: "#8696a0", marginBottom: 6, fontWeight: 600, textTransform: "uppercase" }}>
                    Template &middot; {empTemplate} ({routingMode === "ALL_TELUGU" || routingMode === "AUTO" ? "Telugu / Bilingual" : "English"})
                  </div>

                  {preset === "QUICK_HIRE" || preset === "FLIPKART_SAVINGS" ? (
                    <>
                      <div style={{ marginBottom: 8 }}>
                        {routingMode === "ALL_TELUGU" || routingMode === "AUTO" ? (
                          <>నమస్తే <strong>{empName || "{{1}}"}</strong> 💼,</>
                        ) : (
                          <>Hello <strong>{empName || "{{1}}"}</strong> 💼,</>
                        )}
                      </div>
                      <div style={{ marginBottom: 8 }}>
                        {routingMode === "ALL_TELUGU" || routingMode === "AUTO" ? (
                          <><strong>{empCity || "{{2}}"}</strong> లో ఖరీదైన ఏజెన్సీలకు వేల రూపాయలు చెల్లించకండి!</>
                        ) : (
                          <>Skip expensive hiring agencies in <strong>{empCity || "{{2}}"}</strong>!</>
                        )}
                      </div>
                      <div style={{ marginBottom: 8 }}>
                        {routingMode === "ALL_TELUGU" || routingMode === "AUTO" ? (
                          <>సేల్స్ స్టాఫ్, క్యాషియర్లు, వంటవారు, డ్రైవర్లు, ఆఫీస్ వర్కర్లను డైరెక్ట్‌గా నియమించుకోండి — <strong>*0% ఏజెన్సీ ఫీజు*</strong>.</>
                        ) : (
                          <>Hire verified sales executives, cashiers, cooks, drivers, &amp; office staff directly on phone — <strong>*Zero middleman fee*</strong>.</>
                        )}
                      </div>
                      <div style={{ marginBottom: 10 }}>
                        {routingMode === "ALL_TELUGU" || routingMode === "AUTO" ? (
                          <>కేవలం <strong>*₹49*</strong> తో లేదా <strong>*48 గంటల ఉచిత ట్రయల్*</strong> తో 2 నిమిషాల్లో పోస్ట్ చేయండి 👇</>
                        ) : (
                          <>Post your vacancy in 2 mins for just <strong>*₹49*</strong> (or test risk-free with <strong>*48-Hr Free Trial*</strong>) 👇</>
                        )}
                      </div>
                    </>
                  ) : preset === "ZOMATO_SHORTAGE" ? (
                    <>
                      <div style={{ marginBottom: 8 }}>
                        {routingMode === "ALL_TELUGU" || routingMode === "AUTO" ? (
                          <>నమస్తే <strong>{empName || "{{1}}"}</strong> 👋,</>
                        ) : (
                          <>Hello <strong>{empName || "{{1}}"}</strong> 👋,</>
                        )}
                      </div>
                      <div style={{ marginBottom: 8 }}>
                        {routingMode === "ALL_TELUGU" || routingMode === "AUTO" ? (
                          <><strong>{empCity || "{{2}}"}</strong> లో మీ షాప్, హోటల్ లేదా ఆఫీస్‌కి అర్జెంట్‌గా పనివారు కావాలా? ⏰</>
                        ) : (
                          <>Staff shortage at your shop, hotel, or office in <strong>{empCity || "{{2}}"}</strong>? ⏰</>
                        )}
                      </div>
                      <div style={{ marginBottom: 8 }}>
                        {routingMode === "ALL_TELUGU" || routingMode === "AUTO" ? (
                          <><strong>*500+ వెరిఫైడ్ వర్కర్లు*</strong> (సేల్స్, బిల్లింగ్, వంటవారు, హెల్పర్లు, డ్రైవర్లు) <strong>*0% కమీషన్‌తో*</strong> సిద్ధంగా ఉన్నారు.</>
                        ) : (
                          <><strong>*500+ verified local staff*</strong> (sales, billing, cooks, helpers, drivers, office boys) are ready to join today with <strong>*0% commission*</strong>.</>
                        )}
                      </div>
                      <div style={{ marginBottom: 10 }}>
                        {routingMode === "ALL_TELUGU" || routingMode === "AUTO" ? (
                          <>కేవలం <strong>*₹49*</strong> తో లేదా <strong>*48 గంటల ఉచిత ట్రయల్*</strong> తో ఇప్పుడే పోస్ట్ చేయండి 👇</>
                        ) : (
                          <>Post vacancy for just <strong>*₹49*</strong> or activate your <strong>*48-Hr Free Trial*</strong> below 👇</>
                        )}
                      </div>
                    </>
                  ) : (
                    <>
                      <div style={{ marginBottom: 8 }}>
                        {routingMode === "ALL_TELUGU" || routingMode === "AUTO" ? (
                          <>నమస్తే <strong>{empName || "{{1}}"}</strong>,</>
                        ) : (
                          <>Hello <strong>{empName || "{{1}}"}</strong>,</>
                        )}
                      </div>
                      <div style={{ marginBottom: 8 }}>
                        {routingMode === "ALL_TELUGU" || routingMode === "AUTO" ? (
                          <><strong>{empCity || "{{2}}"}</strong> లో మీ వ్యాపారానికి 500+ వెరిఫైడ్ వర్కర్లు సిద్ధంగా ఉన్నారు.</>
                        ) : (
                          <>500+ verified local workers in <strong>{empCity || "{{2}}"}</strong> are ready to join your business.</>
                        )}
                      </div>
                      <div style={{ marginBottom: 10 }}>
                        {routingMode === "ALL_TELUGU" || routingMode === "AUTO" ? (
                          <>కేవలం <strong>*₹49*</strong> తో లేదా <strong>*48 గంటల ఉచిత ట్రయల్*</strong> తో జాబ్ పోస్ట్ చేయండి.</>
                        ) : (
                          <>Post vacancy for just <strong>*₹49*</strong> or start your <strong>*48-Hr Free Trial*</strong>.</>
                        )}
                      </div>
                    </>
                  )}

                  <div style={{ background: "#ecfdf5", padding: "6px 8px", borderRadius: 6, border: "1px solid #a7f3d0", fontSize: "0.75rem", color: "#065f46", marginBottom: 6 }}>
                    ✓ Short copy: 0% truncation &middot; No &ldquo;... Read more&rdquo; fold
                  </div>
                  <div style={{ textAlign: "right", fontSize: "0.65rem", color: "#8696a0", marginTop: 6 }}>
                    10:30 AM &middot; &#10003;&#10003;
                  </div>
                  <button type="button" className="adm-wa-button" style={{ marginTop: 8 }}>
                    {routingMode === "ALL_TELUGU" || (routingMode === "AUTO" && empLang !== "en")
                      ? "ఇప్పుడే జాబ్ పోస్ట్ చేయండి 📲"
                      : "Post Job for ₹49 🚀"}
                  </button>
                </div>
              </div>
            </div>
            <p style={{ fontSize: "0.75rem", color: "var(--adm-text-muted)", marginTop: 14, textAlign: "center", maxWidth: 320 }}>
              Live WhatsApp chat bubble rendered exactly as received by the employer on Android or iPhone.
            </p>
          </div>
        </div>
      </div>
    )}

      {/* Mode 2: General Festival & Marketing Promos */}
      {mode === "GENERAL_PROMO" && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))", gap: 20 }}>
          <div className="adm-card">
            <div className="adm-card-header">
              <h2 className="adm-card-title">
                <Sparkles style={{ width: 18, height: 18, color: "#8b5cf6" }} />
                General Promo Composer
              </h2>
              <span className="adm-badge adm-badge-blue">Standard Offers</span>
            </div>
            <div className="adm-card-body" style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              <div className="adm-form-group">
                <label className="adm-form-label">Template Name (Registered in Meta) *</label>
                <input
                  type="text"
                  className="adm-input"
                  value={promoTemplate}
                  onChange={(e) => setPromoTemplate(e.target.value)}
                />
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                <div className="adm-form-group">
                  <label className="adm-form-label">Languages</label>
                  <input
                    type="text"
                    className="adm-input"
                    value={promoLangs}
                    onChange={(e) => setPromoLangs(e.target.value)}
                  />
                </div>
                <div className="adm-form-group">
                  <label className="adm-form-label">Target Audience Role</label>
                  <select
                    className="adm-select"
                    value={promoAudience}
                    onChange={(e) => setPromoAudience(e.target.value as Audience)}
                  >
                    <option value="EMPLOYER">Employers / Customers</option>
                    <option value="WORKER">Workers Only</option>
                    <option value="ALL">Everyone Who Opted In</option>
                    <option value="PARTNER">Service Partners</option>
                  </select>
                </div>
              </div>

              <div className="adm-form-group">
                <label className="adm-form-label">
                  <span>Batch Size Limit</span>
                  <span style={{ fontSize: "0.75rem", fontWeight: 700, color: "var(--adm-primary)" }}>{promoMax} users</span>
                </label>
                <input
                  type="range"
                  min="10"
                  max="1000"
                  step="10"
                  value={promoMax}
                  onChange={(e) => setPromoMax(Number(e.target.value))}
                  style={{ width: "100%", accentColor: "var(--adm-primary)" }}
                />
              </div>

              <div className="adm-form-group">
                <label className="adm-form-label">Template Parameters (One per line)</label>
                <textarea
                  className="adm-textarea"
                  rows={3}
                  value={promoParams}
                  onChange={(e) => setPromoParams(e.target.value)}
                />
              </div>

              <div style={{ display: "flex", gap: 10, marginTop: 6 }}>
                <button
                  type="button"
                  className="adm-btn adm-btn-secondary"
                  disabled={busy}
                  onClick={() => runGeneralPromo(true)}
                  style={{ flex: 1 }}
                >
                  <Calculator style={{ width: 15, height: 15 }} />
                  Count &amp; Dry Run
                </button>
                <button
                  type="button"
                  className="adm-btn adm-btn-primary"
                  disabled={busy || !bulkResult?.dryRun || !bulkResult.willSend}
                  onClick={() => runGeneralPromo(false)}
                  style={{ flex: 1 }}
                >
                  <Send style={{ width: 15, height: 15 }} />
                  Send Campaign
                </button>
              </div>
            </div>
          </div>

          {/* Right: Preview for General Promo */}
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
                    Promo &middot; {promoTemplate}
                  </div>
                  <div>Namaskaram! DutyPe lo special offer undi:</div>
                  {promoParams.split("\n").filter(Boolean).map((line, idx) => (
                    <div key={idx} style={{ marginTop: 4, padding: "3px 6px", background: "rgba(255,255,255,0.1)", borderRadius: 4, fontWeight: 600, color: "#25d366" }}>
                      &#123;&#123;{idx + 1}&#125;&#125;: {line}
                    </div>
                  ))}
                  <div style={{ textAlign: "right", fontSize: "0.65rem", color: "#8696a0", marginTop: 4 }}>
                    10:30 AM &middot; &#10003;&#10003;
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

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
