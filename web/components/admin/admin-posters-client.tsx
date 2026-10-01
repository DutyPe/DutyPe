"use client";

import { Noto_Sans_Telugu } from "next/font/google";
import QRCode from "qrcode";
import { useEffect, useMemo, useState } from "react";

import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatCurrencyRange } from "@/lib/firebase/firestore-helpers";
import { PLAY_STORE_URL, SITE_URL } from "@/lib/public-site";

/**
 * Print studio for offline marketing in Telangana & Andhra Pradesh.
 * Telugu-first A4 posters with a scannable QR, optional area name, referral code,
 * contact number and tear-off strips. Printing isolates the chosen poster.
 */

const telugu = Noto_Sans_Telugu({
  subsets: ["telugu", "latin"],
  weight: ["400", "600", "700", "800"],
  display: "swap"
});

type Section = "workers" | "employers" | "jobs";
type LangMode = "both" | "te" | "en";
type ThemeKey = "emerald" | "blue" | "saffron";

type PosterJob = {
  id: string;
  title?: string;
  companyName?: string;
  location?: { lat?: number; lng?: number } | string;
  addressText?: string;
  companyCity?: string;
  payAmount?: number | string;
  salary?: number | string;
  payType?: string;
  salaryType?: string;
  vacancies?: number;
  category?: string;
  jobType?: string;
  shift?: string;
  shiftTiming?: string;
  experienceRequired?: string;
  contactNumber?: string;
  status?: string;
};

type Bilingual = { te: string; en: string };

type Settings = {
  lang: LangMode;
  theme: ThemeKey;
  area: string;
  referralCode: string;
  phone: string;
  tearOffs: boolean;
};

const THEMES: Record<ThemeKey, { name: string; primary: string; deep: string; soft: string; accent: string }> = {
  emerald: { name: "Green", primary: "#047857", deep: "#064e3b", soft: "#ecfdf5", accent: "#f59e0b" },
  blue: { name: "Blue", primary: "#1d4ed8", deep: "#1e3a8a", soft: "#eff6ff", accent: "#f59e0b" },
  saffron: { name: "Orange", primary: "#c2410c", deep: "#7c2d12", soft: "#fff7ed", accent: "#15803d" }
};

type Template = {
  id: string;
  audience: "workers" | "employers";
  badge: Bilingual;
  headline: Bilingual;
  subhead: Bilingual;
  points: Array<{ icon: string; text: Bilingual }>;
  chipsTitle: Bilingual;
  chips: Bilingual[];
  steps: Bilingual[];
};

const WORKER_CHIPS: Bilingual[] = [
  { te: "డ్రైవర్", en: "Driver" }, { te: "డెలివరీ", en: "Delivery" }, { te: "వంట", en: "Cook" },
  { te: "సెక్యూరిటీ", en: "Security" }, { te: "హెల్పర్", en: "Helper" }, { te: "సేల్స్", en: "Sales" },
  { te: "క్లీనింగ్", en: "Cleaning" }, { te: "ఆఫీస్ స్టాఫ్", en: "Office staff" }
];

const TEMPLATES: Template[] = [
  {
    id: "worker-find-job",
    audience: "workers",
    badge: { te: "ఉద్యోగార్థులకు", en: "For job seekers" },
    headline: { te: "ఉద్యోగం కావాలా?", en: "Looking for a job?" },
    subhead: { te: "మీ ఇంటి దగ్గరలోనే రోజువారీ & నెలవారీ ఉద్యోగాలు", en: "Daily & monthly jobs near your home" },
    points: [
      { icon: "🆓", text: { te: "పూర్తిగా ఉచితం — ఎలాంటి ఫీజు లేదు", en: "100% free — no fees" } },
      { icon: "🤝", text: { te: "మధ్యవర్తి లేరు — యజమానితో నేరుగా మాట్లాడండి", en: "No middleman — talk to the employer directly" } },
      { icon: "📍", text: { te: "మీ దగ్గర్లో ఉన్న ఉద్యోగాలు మాత్రమే", en: "Only jobs close to you" } },
      { icon: "⚡", text: { te: "ఒక్క క్లిక్‌తో అప్లై చేయండి", en: "Apply with one tap" } }
    ],
    chipsTitle: { te: "ఈ పనులు అందుబాటులో ఉన్నాయి", en: "Jobs available" },
    chips: WORKER_CHIPS,
    steps: [
      { te: "QR స్కాన్ చేయండి", en: "Scan the QR" },
      { te: "ప్రొఫైల్ పూర్తి చేయండి", en: "Complete profile" },
      { te: "ఉద్యోగానికి అప్లై చేయండి", en: "Apply for jobs" }
    ]
  },
  {
    id: "worker-daily-work",
    audience: "workers",
    badge: { te: "రోజువారీ పని", en: "Daily work" },
    headline: { te: "ఈరోజే పని కావాలా?", en: "Need work today?" },
    subhead: { te: "అర్జెంట్ పనులు — రోజు కూలీ వెంటనే", en: "Urgent jobs — daily wages, fast" },
    points: [
      { icon: "🔔", text: { te: "కొత్త పనులు వచ్చిన వెంటనే నోటిఫికేషన్", en: "Instant alert for new jobs" } },
      { icon: "💵", text: { te: "జీతం ముందే కనిపిస్తుంది", en: "See the pay before you apply" } },
      { icon: "📞", text: { te: "యజమానికి నేరుగా కాల్ చేయండి", en: "Call the employer directly" } },
      { icon: "🎁", text: { te: "ఫ్రెండ్స్‌ని ఇన్వైట్ చేసి డబ్బు సంపాదించండి", en: "Invite friends and earn money" } }
    ],
    chipsTitle: { te: "ఈ పనులు ఎక్కువగా ఉన్నాయి", en: "Popular jobs" },
    chips: [
      { te: "లోడింగ్", en: "Loading" }, { te: "కన్స్ట్రక్షన్", en: "Construction" }, { te: "హోటల్", en: "Hotel" },
      { te: "ఈవెంట్స్", en: "Events" }, { te: "గోడౌన్", en: "Warehouse" }, { te: "క్లీనింగ్", en: "Cleaning" }
    ],
    steps: [
      { te: "QR స్కాన్ చేయండి", en: "Scan the QR" },
      { te: "మీ పని ఎంచుకోండి", en: "Pick your work" },
      { te: "కాల్ చేసి జాయిన్ అవ్వండి", en: "Call & join" }
    ]
  },
  {
    id: "employer-need-workers",
    audience: "employers",
    badge: { te: "యజమానులకు", en: "For business owners" },
    headline: { te: "పనివాళ్లు కావాలా?", en: "Need workers?" },
    subhead: { te: "దగ్గర్లో ఉన్న పనివాళ్లను నిమిషాల్లో పొందండి", en: "Get nearby workers in minutes" },
    points: [
      { icon: "📝", text: { te: "2 నిమిషాల్లో జాబ్ పోస్ట్ చేయండి", en: "Post a job in 2 minutes" } },
      { icon: "📞", text: { te: "పనివాళ్లు నేరుగా మీకు కాల్ చేస్తారు", en: "Workers call you directly" } },
      { icon: "📍", text: { te: "మీ ఏరియాలోని పనివాళ్లే", en: "Workers from your own area" } },
      { icon: "✅", text: { te: "ప్రొఫైల్ చూసి సెలెక్ట్ చేయండి", en: "Check profiles, then hire" } }
    ],
    chipsTitle: { te: "ఎవరికి ఉపయోగం?", en: "Useful for" },
    chips: [
      { te: "షాపులు", en: "Shops" }, { te: "హోటళ్లు", en: "Hotels" }, { te: "PGలు", en: "PGs" },
      { te: "ఆఫీసులు", en: "Offices" }, { te: "గోడౌన్లు", en: "Warehouses" }, { te: "ఇళ్లు", en: "Homes" }
    ],
    steps: [
      { te: "QR స్కాన్ చేయండి", en: "Scan the QR" },
      { te: "జాబ్ పోస్ట్ చేయండి", en: "Post your job" },
      { te: "కాల్స్ అందుకోండి", en: "Receive calls" }
    ]
  }
];

/** Telugu names for common job categories (job posters). */
const CATEGORY_TE: Record<string, string> = {
  driver: "డ్రైవర్", cook: "వంటమనిషి", maid: "పనిమనిషి", helper: "హెల్పర్", security: "సెక్యూరిటీ గార్డ్",
  delivery: "డెలివరీ బాయ్", waiter: "వెయిటర్", electrician: "ఎలక్ట్రీషియన్", plumber: "ప్లంబర్",
  painter: "పెయింటర్", carpenter: "కార్పెంటర్", sales: "సేల్స్", cashier: "క్యాషియర్", packer: "ప్యాకర్",
  tailor: "టైలర్", mechanic: "మెకానిక్", teacher: "టీచర్", telecaller: "టెలికాలర్", gardener: "తోటమాలి",
  receptionist: "రిసెప్షనిస్ట్", "office staff": "ఆఫీస్ స్టాఫ్", beautician: "బ్యూటీషియన్",
  "data entry": "డేటా ఎంట్రీ", caretaker: "కేర్‌టేకర్", marketing: "మార్కెటింగ్", "customer support": "కస్టమర్ సపోర్ట్"
};

function categoryTe(job: PosterJob): string {
  const keys = [job.category, job.jobType, job.title].filter(Boolean).map((v) => String(v).toLowerCase());
  for (const key of keys) {
    for (const [name, te] of Object.entries(CATEGORY_TE)) {
      if (key.includes(name)) return te;
    }
  }
  return "";
}

function jobLocation(job: PosterJob): string {
  if (job.addressText?.trim()) return job.addressText.trim();
  if (job.companyCity?.trim()) return job.companyCity.trim();
  if (typeof job.location === "string" && job.location.trim()) return job.location.trim();
  return "";
}

function shiftTe(value: string): string {
  const v = value.toLowerCase();
  if (v.includes("night")) return "నైట్ షిఫ్ట్";
  if (v.includes("day") || v.includes("morning")) return "డే షిఫ్ట్";
  if (v.includes("any") || v.includes("flex") || v.includes("both")) return "ఏ షిఫ్ట్ అయినా";
  return "";
}

function withReferrer(url: string, settings: Settings, campaign: string): string {
  const params = new URLSearchParams({
    utm_source: "poster",
    utm_campaign: campaign,
    ...(settings.area ? { utm_content: settings.area.slice(0, 40) } : {}),
    ...(settings.referralCode ? { utm_term: settings.referralCode } : {})
  });
  if (url.startsWith(PLAY_STORE_URL)) return `${url}&referrer=${encodeURIComponent(params.toString())}`;
  return `${url}${url.includes("?") ? "&" : "?"}${params.toString()}`;
}

function useQr(value: string): string {
  const [dataUrl, setDataUrl] = useState("");
  useEffect(() => {
    let active = true;
    QRCode.toDataURL(value, { margin: 1, width: 640, errorCorrectionLevel: "M" })
      .then((url) => { if (active) setDataUrl(url); })
      .catch(() => { if (active) setDataUrl(""); });
    return () => { active = false; };
  }, [value]);
  return dataUrl;
}

/** Bilingual text: Telugu big, English small underneath (or one of them only). */
function Bi({ text, lang, teClass, enClass }: { text: Bilingual; lang: LangMode; teClass: string; enClass: string }) {
  if (lang === "en") return <span className={teClass}>{text.en}</span>;
  if (lang === "te") return <span className={teClass}>{text.te}</span>;
  return (
    <>
      <span className={teClass}>{text.te}</span>
      <span className={enClass}>{text.en}</span>
    </>
  );
}

function pick(text: Bilingual, lang: LangMode) {
  return lang === "en" ? text.en : text.te;
}

// ─────────────────────────────────────────────────────────────────────────────
// Posters
// ─────────────────────────────────────────────────────────────────────────────

function PosterFrame({ settings, children, qr, qrCaption }: {
  settings: Settings;
  children: React.ReactNode;
  qr: string;
  qrCaption: Bilingual;
}) {
  const theme = THEMES[settings.theme];
  const lang = settings.lang;
  return (
    <article
      className={`dp-poster ${telugu.className}`}
      style={{
        ["--p" as string]: theme.primary,
        ["--pd" as string]: theme.deep,
        ["--ps" as string]: theme.soft,
        ["--pa" as string]: theme.accent
      }}
    >
      <header className="dp-top">
        <div className="dp-brand">
          <span className="dp-logo">D</span>
          <div>
            <strong>DutyPe</strong>
            <span>{lang === "en" ? "Local jobs app" : "లోకల్ జాబ్స్ యాప్"}</span>
          </div>
        </div>
        {settings.area && (
          <span className="dp-area">📍 {settings.area}</span>
        )}
      </header>

      <div className="dp-body">{children}</div>

      <footer className="dp-cta">
        <div className="dp-qr">
          {qr ? <img src={qr} alt="QR code" /> : <div className="dp-qr-empty" />}
        </div>
        <div className="dp-cta-text">
          <Bi text={qrCaption} lang={lang} teClass="dp-cta-te" enClass="dp-cta-en" />
          <span className="dp-cta-store">▶ Google Play → <b>DutyPe</b></span>
          {settings.referralCode && (
            <span className="dp-code">
              {lang === "en" ? "Referral code" : "రిఫరల్ కోడ్"}: <b>{settings.referralCode}</b>
            </span>
          )}
          {settings.phone && (
            <span className="dp-phone">📞 {settings.phone}</span>
          )}
        </div>
      </footer>

      {settings.tearOffs && (
        <div className="dp-tears">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="dp-tear">
              <b>DutyPe App</b>
              <span>Play Store</span>
              {settings.referralCode ? <span>Code {settings.referralCode}</span> : null}
              {settings.phone ? <span>{settings.phone}</span> : null}
            </div>
          ))}
        </div>
      )}
    </article>
  );
}

function TemplatePoster({ template, settings }: { template: Template; settings: Settings }) {
  const lang = settings.lang;
  const qr = useQr(withReferrer(PLAY_STORE_URL, settings, template.id));
  return (
    <PosterFrame
      settings={settings}
      qr={qr}
      qrCaption={
        template.audience === "workers"
          ? { te: "స్కాన్ చేసి ఉచితంగా డౌన్‌లోడ్ చేయండి", en: "Scan to download free" }
          : { te: "స్కాన్ చేసి జాబ్ పోస్ట్ చేయండి", en: "Scan to post your job" }
      }
    >
      <span className="dp-badge">{pick(template.badge, lang)}</span>
      <h1 className="dp-headline">
        <Bi text={template.headline} lang={lang} teClass="dp-h-te" enClass="dp-h-en" />
      </h1>
      <p className="dp-sub">
        <Bi text={template.subhead} lang={lang} teClass="dp-sub-te" enClass="dp-sub-en" />
      </p>

      <ul className="dp-points">
        {template.points.map((point) => (
          <li key={point.text.en}>
            <span className="dp-point-icon">{point.icon}</span>
            <span className="dp-point-text">
              <Bi text={point.text} lang={lang} teClass="dp-pt-te" enClass="dp-pt-en" />
            </span>
          </li>
        ))}
      </ul>

      <div className="dp-chips-block">
        <span className="dp-chips-title">{pick(template.chipsTitle, lang)}</span>
        <div className="dp-chips">
          {template.chips.map((chip) => (
            <span key={chip.en} className="dp-chip">{lang === "en" ? chip.en : chip.te}</span>
          ))}
        </div>
      </div>

      <ol className="dp-steps">
        {template.steps.map((step, index) => (
          <li key={step.en}>
            <span className="dp-step-no">{index + 1}</span>
            <span className="dp-step-text">{pick(step, lang)}</span>
          </li>
        ))}
      </ol>
    </PosterFrame>
  );
}

function JobPoster({ job, settings }: { job: PosterJob; settings: Settings }) {
  const lang = settings.lang;
  const qr = useQr(withReferrer(`${SITE_URL.replace(/\/$/, "")}/jobs/${job.id}`, settings, "job"));
  const pay = formatCurrencyRange(job.payAmount ?? job.salary, job.payType ?? job.salaryType);
  const te = categoryTe(job);
  const shift = job.shiftTiming || job.shift || "";
  const location = jobLocation(job);
  const title = job.title?.trim() || "Job opening";
  const facts: Array<{ icon: string; label: Bilingual; value: string }> = [
    { icon: "👥", label: { te: "ఖాళీలు", en: "Vacancies" }, value: job.vacancies ? String(job.vacancies) : (lang === "en" ? "Multiple" : "చాలా ఉన్నాయి") },
    { icon: "🕐", label: { te: "షిఫ్ట్", en: "Shift" }, value: (lang !== "en" && shiftTe(shift)) || shift || (lang === "en" ? "Flexible" : "ఫ్లెక్సిబుల్") },
    { icon: "🎯", label: { te: "అనుభవం", en: "Experience" }, value: job.experienceRequired || (lang === "en" ? "Freshers welcome" : "ఫ్రెషర్స్ కూడా ఓకే") },
    { icon: "💼", label: { te: "పని రకం", en: "Job type" }, value: job.jobType || (lang === "en" ? "Full-time" : "ఫుల్ టైమ్") }
  ];

  return (
    <PosterFrame
      settings={{ ...settings, phone: settings.phone || job.contactNumber || "" }}
      qr={qr}
      qrCaption={{ te: "స్కాన్ చేసి వెంటనే అప్లై చేయండి", en: "Scan to apply now" }}
    >
      <span className="dp-badge dp-badge-hot">🔥 {lang === "en" ? "Hiring now" : "ఉద్యోగ అవకాశం"}</span>
      <h1 className="dp-headline dp-job-title">
        {lang !== "en" && te ? (
          <>
            <span className="dp-h-te">{te} కావాలి</span>
            <span className="dp-h-en">{title}</span>
          </>
        ) : (
          <span className="dp-h-te">{title}</span>
        )}
      </h1>
      <p className="dp-company">{job.companyName || "DutyPe employer"}{location ? ` · 📍 ${location}` : ""}</p>

      <div className="dp-pay">
        <span>{lang === "en" ? "Salary" : lang === "te" ? "జీతం" : "జీతం · Salary"}</span>
        <strong>{pay || (lang === "en" ? "Best in market" : "మంచి జీతం")}</strong>
      </div>

      <div className="dp-facts">
        {facts.map((fact) => (
          <div key={fact.label.en} className="dp-fact">
            <span className="dp-fact-label">{fact.icon} {lang === "both" ? `${fact.label.te} · ${fact.label.en}` : pick(fact.label, lang)}</span>
            <span className="dp-fact-value">{fact.value}</span>
          </div>
        ))}
      </div>

      <ol className="dp-steps">
        {[
          { te: "QR స్కాన్ చేయండి", en: "Scan the QR" },
          { te: "జాబ్ వివరాలు చూడండి", en: "See job details" },
          { te: "అప్లై / కాల్ చేయండి", en: "Apply or call" }
        ].map((step, index) => (
          <li key={step.en}>
            <span className="dp-step-no">{index + 1}</span>
            <span className="dp-step-text">{pick(step, lang)}</span>
          </li>
        ))}
      </ol>
    </PosterFrame>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Studio
// ─────────────────────────────────────────────────────────────────────────────

const SETTINGS_KEY = "dutype-poster-settings-v1";

export function AdminPostersClient({ initialJobId }: { initialJobId?: string }) {
  const [section, setSection] = useState<Section>(initialJobId ? "jobs" : "workers");
  const [settings, setSettings] = useState<Settings>({
    lang: "both", theme: "emerald", area: "", referralCode: "", phone: "", tearOffs: true
  });
  const [templateId, setTemplateId] = useState(TEMPLATES[0].id);
  const [jobs, setJobs] = useState<PosterJob[] | null>(null);
  const [jobsError, setJobsError] = useState<string | null>(null);
  const [selectedJobId, setSelectedJobId] = useState(initialJobId ?? "");
  const [search, setSearch] = useState("");

  // Remember the print settings on this device.
  useEffect(() => {
    try {
      const saved = window.localStorage.getItem(SETTINGS_KEY);
      if (saved) setSettings((current) => ({ ...current, ...JSON.parse(saved) }));
    } catch { /* ignore */ }
  }, []);
  useEffect(() => {
    try { window.localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings)); } catch { /* ignore */ }
  }, [settings]);

  // Jobs are loaded ONLY when the Job posters tab is opened (no reads for worker/employer posters).
  useEffect(() => {
    if (section !== "jobs" || jobs !== null) return;
    let active = true;
    (async () => {
      try {
        const response = await adminApiFetch("/api/admin/jobs", { cache: "no-store" });
        const payload = (await response.json()) as { jobs?: PosterJob[]; error?: string };
        if (!response.ok) throw new Error(payload.error || "Failed to load jobs.");
        if (!active) return;
        const open = (payload.jobs ?? []).filter((job) => !job.status || job.status === "open");
        setJobs(open);
        setSelectedJobId((current) => current || open[0]?.id || "");
      } catch (error) {
        if (active) {
          setJobs([]);
          setJobsError(error instanceof Error ? error.message : "Failed to load jobs.");
        }
      }
    })();
    return () => { active = false; };
  }, [section, jobs]);

  const audienceTemplates = TEMPLATES.filter((t) => t.audience === section);
  const template = audienceTemplates.find((t) => t.id === templateId) ?? audienceTemplates[0];

  const filteredJobs = useMemo(() => {
    const needle = search.trim().toLowerCase();
    const rows = jobs ?? [];
    if (!needle) return rows;
    return rows.filter((job) =>
      [job.title, job.companyName, jobLocation(job), job.category, job.jobType]
        .filter(Boolean).some((v) => String(v).toLowerCase().includes(needle))
    );
  }, [jobs, search]);
  const selectedJob = (jobs ?? []).find((job) => job.id === selectedJobId) ?? filteredJobs[0];

  function update<K extends keyof Settings>(key: K, value: Settings[K]) {
    setSettings((current) => ({ ...current, [key]: value }));
  }

  function printNow() {
    document.body.classList.add("dp-printing");
    const done = () => document.body.classList.remove("dp-printing");
    window.addEventListener("afterprint", done, { once: true });
    window.setTimeout(() => {
      window.print();
      window.setTimeout(done, 1500);
    }, 80);
  }

  return (
    <section className="dp-studio">
      <style>{STUDIO_CSS}</style>

      <aside className="dp-panel">
        <div className="dp-tabs">
          {([
            ["workers", "👷 Workers", "పనివాళ్ల కోసం"],
            ["employers", "🏪 Employers", "యజమానుల కోసం"],
            ["jobs", "📌 Job posters", "పోస్ట్ చేసిన జాబ్స్"]
          ] as Array<[Section, string, string]>).map(([key, label, te]) => (
            <button
              key={key}
              type="button"
              className={`dp-tab ${section === key ? "on" : ""}`}
              onClick={() => {
                setSection(key);
                const first = TEMPLATES.find((t) => t.audience === key);
                if (first) setTemplateId(first.id);
              }}
            >
              <b>{label}</b>
              <small className={telugu.className}>{te}</small>
            </button>
          ))}
        </div>

        {section !== "jobs" && audienceTemplates.length > 1 && (
          <div className="dp-field">
            <label>Design</label>
            <div className="dp-seg">
              {audienceTemplates.map((t) => (
                <button key={t.id} type="button" className={template?.id === t.id ? "on" : ""} onClick={() => setTemplateId(t.id)}>
                  <span className={telugu.className}>{t.headline.te}</span>
                </button>
              ))}
            </div>
          </div>
        )}

        {section === "jobs" && (
          <div className="dp-field">
            <label>Job</label>
            <input placeholder="Search job, company or area" value={search} onChange={(e) => setSearch(e.target.value)} />
            <select value={selectedJob?.id ?? ""} onChange={(e) => setSelectedJobId(e.target.value)} disabled={!filteredJobs.length}>
              {filteredJobs.map((job) => (
                <option key={job.id} value={job.id}>{job.title || "Untitled"} — {job.companyName || "Company"}</option>
              ))}
            </select>
            {jobs === null && <small>Loading open jobs…</small>}
            {jobsError && <small className="dp-err">{jobsError}</small>}
          </div>
        )}

        <div className="dp-field">
          <label>Language</label>
          <div className="dp-seg">
            {([["both", "తెలుగు + English"], ["te", "తెలుగు"], ["en", "English"]] as Array<[LangMode, string]>).map(([key, label]) => (
              <button key={key} type="button" className={settings.lang === key ? "on" : ""} onClick={() => update("lang", key)}>
                <span className={telugu.className}>{label}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="dp-field">
          <label>Colour</label>
          <div className="dp-swatches">
            {(Object.keys(THEMES) as ThemeKey[]).map((key) => (
              <button
                key={key}
                type="button"
                title={THEMES[key].name}
                className={settings.theme === key ? "on" : ""}
                style={{ background: THEMES[key].primary }}
                onClick={() => update("theme", key)}
              />
            ))}
          </div>
        </div>

        <div className="dp-field">
          <label>Area / town (optional)</label>
          <input className={telugu.className} placeholder="e.g. కూకట్‌పల్లి / Kukatpally" value={settings.area} maxLength={40} onChange={(e) => update("area", e.target.value)} />
        </div>
        <div className="dp-field">
          <label>Referral code (optional)</label>
          <input placeholder="e.g. DUTY5RNB" value={settings.referralCode} maxLength={12} onChange={(e) => update("referralCode", e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ""))} />
        </div>
        <div className="dp-field">
          <label>Phone / WhatsApp (optional)</label>
          <input placeholder="e.g. 85007 17800" value={settings.phone} maxLength={16} onChange={(e) => update("phone", e.target.value)} />
        </div>
        <label className="dp-check">
          <input type="checkbox" checked={settings.tearOffs} onChange={(e) => update("tearOffs", e.target.checked)} />
          Tear-off strips at the bottom
        </label>

        <button type="button" className="dp-print" onClick={printNow} disabled={section === "jobs" && !selectedJob}>
          🖨️ Print A4 poster
        </button>
        <p className="dp-hint">Tip: in the print dialog choose <b>A4</b>, <b>Margins: None</b> and turn on <b>Background graphics</b>.</p>
      </aside>

      <div className="dp-stage">
        <div className="dp-scale">
          {section === "jobs"
            ? (selectedJob ? <JobPoster job={selectedJob} settings={settings} /> : <div className="dp-empty">{jobs === null ? "Loading…" : "No open jobs found."}</div>)
            : (template ? <TemplatePoster template={template} settings={settings} /> : null)}
        </div>
      </div>
    </section>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Styles (A4 = 210 × 297 mm; preview is scaled, print is 1:1)
// ─────────────────────────────────────────────────────────────────────────────

const STUDIO_CSS = `
.dp-studio { display: grid; grid-template-columns: 320px 1fr; gap: 24px; align-items: start; }
@media (max-width: 1000px) { .dp-studio { grid-template-columns: 1fr; } }
.dp-panel { position: sticky; top: 16px; background: #fff; border: 1px solid #e2e8f0; border-radius: 16px; padding: 16px; display: flex; flex-direction: column; gap: 14px; }
.dp-tabs { display: grid; gap: 6px; }
.dp-tab { text-align: left; border: 1px solid #e2e8f0; background: #fff; border-radius: 12px; padding: 10px 12px; cursor: pointer; display: flex; flex-direction: column; gap: 2px; }
.dp-tab b { font-size: 14px; color: #0f172a; }
.dp-tab small { font-size: 12px; color: #64748b; }
.dp-tab.on { border-color: #047857; background: #ecfdf5; }
.dp-field { display: flex; flex-direction: column; gap: 6px; }
.dp-field label { font-size: 12px; font-weight: 600; color: #475569; text-transform: uppercase; letter-spacing: .04em; }
.dp-field input, .dp-field select { border: 1px solid #cbd5e1; border-radius: 10px; padding: 9px 10px; font-size: 14px; background: #fff; }
.dp-seg { display: flex; flex-wrap: wrap; gap: 6px; }
.dp-seg button { border: 1px solid #cbd5e1; background: #fff; border-radius: 999px; padding: 6px 12px; font-size: 13px; cursor: pointer; }
.dp-seg button.on { background: #0f172a; color: #fff; border-color: #0f172a; }
.dp-swatches { display: flex; gap: 10px; }
.dp-swatches button { width: 30px; height: 30px; border-radius: 50%; border: 3px solid #fff; box-shadow: 0 0 0 1px #cbd5e1; cursor: pointer; }
.dp-swatches button.on { box-shadow: 0 0 0 2px #0f172a; }
.dp-check { display: flex; gap: 8px; align-items: center; font-size: 14px; color: #0f172a; }
.dp-print { background: #0f172a; color: #fff; border: 0; border-radius: 12px; padding: 13px; font-size: 15px; font-weight: 700; cursor: pointer; }
.dp-print:disabled { opacity: .5; cursor: not-allowed; }
.dp-hint { font-size: 12px; color: #64748b; margin: 0; }
.dp-err { color: #dc2626; }
.dp-stage { background: #e2e8f0; border-radius: 16px; padding: 24px; overflow: auto; }
.dp-scale { width: calc(210mm * .62); height: calc(297mm * .62); margin: 0 auto; }
.dp-scale > .dp-poster { transform: scale(.62); transform-origin: top left; box-shadow: 0 20px 50px rgba(15,23,42,.25); }
.dp-empty { padding: 40px; color: #475569; }

.dp-poster { width: 210mm; height: 297mm; background: #fff; color: #0f172a; position: relative; overflow: hidden; display: flex; flex-direction: column; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
.dp-top { background: var(--pd); color: #fff; display: flex; justify-content: space-between; align-items: center; padding: 9mm 12mm 7mm; }
.dp-brand { display: flex; align-items: center; gap: 4mm; }
.dp-logo { width: 13mm; height: 13mm; border-radius: 3.5mm; background: #fff; color: var(--pd); display: grid; place-items: center; font-weight: 800; font-size: 8mm; }
.dp-brand strong { display: block; font-size: 8mm; line-height: 1; letter-spacing: -.02em; }
.dp-brand span { font-size: 3.6mm; opacity: .85; }
.dp-area { background: rgba(255,255,255,.14); border: .3mm solid rgba(255,255,255,.35); border-radius: 99mm; padding: 2mm 5mm; font-size: 4.2mm; font-weight: 700; }
.dp-body { flex: 1; padding: 9mm 12mm 0; display: flex; flex-direction: column; gap: 6mm; background: linear-gradient(180deg, var(--ps) 0, #fff 70mm); }
.dp-badge { align-self: flex-start; background: var(--p); color: #fff; border-radius: 99mm; padding: 1.8mm 5mm; font-size: 4.2mm; font-weight: 700; }
.dp-badge-hot { background: #dc2626; }
.dp-headline { margin: 0; display: flex; flex-direction: column; gap: 1mm; }
.dp-h-te { font-size: 17mm; font-weight: 800; line-height: 1.15; color: var(--pd); letter-spacing: -.01em; }
.dp-h-en { font-size: 8mm; font-weight: 700; color: #334155; line-height: 1.1; }
.dp-job-title .dp-h-te { font-size: 14mm; }
.dp-sub { margin: 0; display: flex; flex-direction: column; gap: .5mm; }
.dp-sub-te { font-size: 6.4mm; font-weight: 600; color: #1e293b; line-height: 1.35; }
.dp-sub-en { font-size: 4.4mm; color: #64748b; }
.dp-company { margin: 0; font-size: 5.2mm; font-weight: 600; color: #334155; }
.dp-points { list-style: none; margin: 0; padding: 0; display: grid; gap: 3.2mm; }
.dp-points li { display: flex; gap: 4mm; align-items: center; background: #fff; border: .35mm solid #e2e8f0; border-left: 1.6mm solid var(--p); border-radius: 3mm; padding: 3mm 4mm; }
.dp-point-icon { font-size: 7.5mm; width: 9mm; text-align: center; }
.dp-point-text { display: flex; flex-direction: column; }
.dp-pt-te { font-size: 5.3mm; font-weight: 700; line-height: 1.3; }
.dp-pt-en { font-size: 3.7mm; color: #64748b; }
.dp-chips-block { display: flex; flex-direction: column; gap: 2.5mm; }
.dp-chips-title { font-size: 4.2mm; font-weight: 700; color: #475569; }
.dp-chips { display: flex; flex-wrap: wrap; gap: 2.2mm; }
.dp-chip { background: var(--ps); color: var(--pd); border: .35mm solid var(--p); border-radius: 99mm; padding: 1.5mm 4.2mm; font-size: 4.4mm; font-weight: 700; }
.dp-steps { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(3, 1fr); gap: 3mm; }
.dp-steps li { display: flex; align-items: center; gap: 2.5mm; background: #f8fafc; border-radius: 3mm; padding: 2.8mm; }
.dp-step-no { flex: none; width: 8mm; height: 8mm; border-radius: 50%; background: var(--pa); color: #fff; display: grid; place-items: center; font-weight: 800; font-size: 4.4mm; }
.dp-step-text { font-size: 3.9mm; font-weight: 700; line-height: 1.25; }
.dp-pay { background: var(--pd); color: #fff; border-radius: 4mm; padding: 5mm 6mm; display: flex; justify-content: space-between; align-items: center; }
.dp-pay span { font-size: 4.6mm; opacity: .9; font-weight: 600; }
.dp-pay strong { font-size: 10mm; font-weight: 800; }
.dp-facts { display: grid; grid-template-columns: 1fr 1fr; gap: 3mm; }
.dp-fact { border: .35mm solid #e2e8f0; border-radius: 3mm; padding: 3mm 4mm; display: flex; flex-direction: column; gap: 1mm; }
.dp-fact-label { font-size: 3.6mm; color: #64748b; font-weight: 600; }
.dp-fact-value { font-size: 5mm; font-weight: 800; }
.dp-cta { margin: 6mm 12mm 0; display: flex; gap: 6mm; align-items: center; border: .6mm solid var(--p); border-radius: 5mm; padding: 4.5mm; background: #fff; }
.dp-qr { flex: none; width: 42mm; height: 42mm; background: #fff; border-radius: 2mm; }
.dp-qr img, .dp-qr-empty { width: 100%; height: 100%; display: block; }
.dp-cta-text { display: flex; flex-direction: column; gap: 1.4mm; }
.dp-cta-te { font-size: 7mm; font-weight: 800; color: var(--pd); line-height: 1.2; }
.dp-cta-en { font-size: 4.2mm; color: #475569; font-weight: 600; }
.dp-cta-store { font-size: 4.2mm; color: #0f172a; }
.dp-code { align-self: flex-start; background: var(--pa); color: #fff; border-radius: 2mm; padding: 1.2mm 3mm; font-size: 4.4mm; }
.dp-phone { font-size: 5mm; font-weight: 800; }
.dp-tears { margin-top: auto; display: grid; grid-template-columns: repeat(8, 1fr); border-top: .5mm dashed #94a3b8; height: 36mm; }
.dp-tear { border-right: .4mm dashed #94a3b8; writing-mode: vertical-rl; transform: rotate(180deg); display: flex; flex-direction: column; justify-content: center; gap: 1mm; padding: 2mm; font-size: 3.2mm; text-align: center; }
.dp-tear:last-child { border-right: 0; }
.dp-tear b { font-size: 3.6mm; color: var(--pd); }
.dp-poster:not(:has(.dp-tears)) .dp-cta { margin-bottom: 10mm; margin-top: auto; }

@media print {
  @page { size: A4 portrait; margin: 0; }
  body.dp-printing * { visibility: hidden !important; }
  body.dp-printing .dp-poster, body.dp-printing .dp-poster * { visibility: visible !important; }
  body.dp-printing .dp-scale { width: auto; height: auto; }
  body.dp-printing .dp-scale > .dp-poster { position: fixed; left: 0; top: 0; transform: none; box-shadow: none; }
}
`;
