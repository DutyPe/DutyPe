"use client";

import { useEffect, useMemo, useState } from "react";
import { doc, getDoc, setDoc } from "firebase/firestore";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { getFirebaseServices } from "@/lib/firebase/client";
import Lottie from "lottie-react";
import { Plus, Trash2, Smartphone, Eye, Sparkles, Check, RefreshCw, ExternalLink, Upload, Film, FileCheck, AlertCircle } from "lucide-react";

export interface PromoBannerDef {
  id: string;
  headline: string;
  subheadline: string;
  cta: string;
  imageUrl?: string;
  lottieUrl?: string;
  fileSizeBytes?: number;
  bgStartColor: string;
  bgEndColor: string;
  targetServiceId?: string;
  targetCategoryId?: string;
  imageScale?: number;
  active: boolean;
}

export type BannerSurface = "services" | "worker" | "employer";

export const COLOR_PRESETS = [
  { label: "Midnight Slate", start: "#0F172A", end: "#1E3A8A" },
  { label: "Emerald Pro", start: "#064E3B", end: "#047857" },
  { label: "Ocean Sky", start: "#0F172A", end: "#0284C7" },
  { label: "Sunset Amber", start: "#451A03", end: "#D97706" },
  { label: "Crimson Flame", start: "#881337", end: "#E11D48" },
  { label: "Royal Indigo", start: "#1E1B4B", end: "#4F46E5" },
  { label: "Dark Onyx", start: "#111827", end: "#1F2937" },
  { label: "Forest Green", start: "#14532D", end: "#16A34A" },
];

const PRESET_BANNERS_BY_SURFACE: Record<BannerSurface, Array<{ label: string; banner: Omit<PromoBannerDef, "id"> }>> = {
  services: [
    {
      label: "❄️ AC Mega Service (Khammam Summer)",
      banner: {
        headline: "Khammam AC Deep Clean ⚡ Flat ₹499",
        subheadline: "Foam jet service & gas leak check in 59 mins",
        cta: "Book AC Service",
        bgStartColor: "#0F172A",
        bgEndColor: "#0369A1",
        targetCategoryId: "AC",
        active: true,
      }
    },
    {
      label: "✨ Festive Home Deep Cleaning",
      banner: {
        headline: "Sparkling Home Deep Cleaning · 25% Off",
        subheadline: "Kitchen, bathroom & sofa sanitization by pros",
        cta: "Book Cleaning",
        bgStartColor: "#022C22",
        bgEndColor: "#065F46",
        targetCategoryId: "CLEANING",
        active: true,
      }
    },
    {
      label: "⚡ 60-Second Instant Worker Dispatch",
      banner: {
        headline: "Need Urgent Worker? Dispatched in 60s",
        subheadline: "Helpers, technicians & drivers on demand in Khammam",
        cta: "Hire Instantly",
        bgStartColor: "#451A03",
        bgEndColor: "#9A3412",
        targetCategoryId: "HOME_HELP",
        active: true,
      }
    }
  ],
  worker: [
    {
      label: "⚡ Zero Commission (100% Earnings)",
      banner: {
        headline: "Direct Hiring • 100% Daily Earnings",
        subheadline: "DutyPe takes 0% cut from your work. Keep all of what you earn directly from employers.",
        cta: "Explore Jobs",
        bgStartColor: "#0F172A",
        bgEndColor: "#1E293B",
        targetCategoryId: "jobs",
        active: true
      }
    },
    {
      label: "🛡️ DutyPe Verified Badge",
      banner: {
        headline: "Get Hired 3x Faster Today",
        subheadline: "Complete your profile & verified trade skills to get priority recommendations and direct calls.",
        cta: "Complete Profile",
        bgStartColor: "#064E3B",
        bgEndColor: "#047857",
        targetCategoryId: "profile",
        active: true
      }
    },
    {
      label: "🎁 Referral Bonus ₹100",
      banner: {
        headline: "Earn ₹100 per Worker Referral",
        subheadline: "Invite fellow electricians, plumbers, cooks or drivers. Withdraw instant cash straight to UPI.",
        cta: "Invite & Earn",
        bgStartColor: "#78350F",
        bgEndColor: "#B45309",
        targetCategoryId: "refer_earn",
        active: true
      }
    }
  ],
  employer: [
    {
      label: "⚡ Hire in Minutes",
      banner: {
        headline: "Post a Job & Get Instant Calls",
        subheadline: "Connect with verified, reliable workers near your shop, factory, or home in minutes.",
        cta: "Post a Job",
        bgStartColor: "#0F172A",
        bgEndColor: "#1E293B",
        targetCategoryId: "post_job",
        active: true
      }
    },
    {
      label: "🚨 Urgent Needs (Workers in 60s)",
      banner: {
        headline: "Need Workers Today? Instant Matches",
        subheadline: "Broadcast an urgent need. Available workers get notified on their phones instantly.",
        cta: "Post Urgent Need",
        bgStartColor: "#881337",
        bgEndColor: "#BE123C",
        targetCategoryId: "urgent_need",
        active: true
      }
    },
    {
      label: "🎉 Referral Cashback ₹100",
      banner: {
        headline: "Earn ₹100 Cashback Every Time",
        subheadline: "Refer fellow business owners, contractors or shopkeepers to DutyPe and earn instant wallet balance.",
        cta: "Refer & Earn",
        bgStartColor: "#312E81",
        bgEndColor: "#4338CA",
        targetCategoryId: "refer_earn",
        active: true
      }
    }
  ]
};

const PRESET_BANNERS = PRESET_BANNERS_BY_SURFACE.services;

export interface BannerOptimizationResult {
  file: File;
  originalSizeKb: number;
  webpBlob: Blob;
  webpSizeKb: number;
  previewUrl: string;
  width: number;
  height: number;
  savingsPct: number;
}

// Convert any banner image to low-KB WebP directly in browser with smooth downscaling
export async function optimizeBannerImage(file: File): Promise<BannerOptimizationResult> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        let w = img.width;
        let h = img.height;
        // Banners look sharp on 1080p/1440p mobile displays at max 800px dimension
        const maxDim = 800;
        if (w > maxDim || h > maxDim) {
          if (w > h) {
            h = Math.round((h * maxDim) / w);
            w = maxDim;
          } else {
            w = Math.round((w * maxDim) / h);
            h = maxDim;
          }
        }
        const canvas = document.createElement("canvas");
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          reject(new Error("Unable to create canvas context"));
          return;
        }
        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = "high";
        ctx.drawImage(img, 0, 0, w, h);

        canvas.toBlob(
          (blob) => {
            if (!blob) {
              reject(new Error("WebP conversion failed"));
              return;
            }
            const originalKb = Math.round((file.size / 1024) * 10) / 10;
            const webpKb = Math.round((blob.size / 1024) * 10) / 10;
            const savings = Math.max(0, Math.round(((file.size - blob.size) / file.size) * 100));

            resolve({
              file,
              originalSizeKb: originalKb,
              webpBlob: blob,
              webpSizeKb: webpKb,
              previewUrl: URL.createObjectURL(blob),
              width: w,
              height: h,
              savingsPct: savings
            });
          },
          "image/webp",
          0.85
        );
      };
      img.onerror = () => reject(new Error("Failed to decode image"));
      img.src = e.target?.result as string;
    };
    reader.onerror = () => reject(new Error("Failed to read file"));
    reader.readAsDataURL(file);
  });
}

export function AdminBannersManager({
  onSaved,
  onError
}: {
  onSaved?: (msg: string) => void;
  onError?: (msg: string) => void;
} = {}) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [surface, setSurface] = useState<BannerSurface>("services");
  const [banners, setBanners] = useState<PromoBannerDef[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [selectedIdx, setSelectedIdx] = useState<number>(0);
  const [rawConfig, setRawConfig] = useState<Record<string, unknown>>({});
  const [optStats, setOptStats] = useState<{ originalKb: number; webpKb: number; savings: number } | null>(null);

  useEffect(() => {
    if (!services) return;
    void (async () => {
      setLoading(true);
      try {
        const targetDoc = surface === "services" ? "services" : "dynamic_features";
        const snap = await getDoc(doc(services.db, "app_config", targetDoc));
        const data = (snap.data() || {}) as Record<string, unknown>;
        setRawConfig(data);
        const listField = surface === "services"
          ? (data.promoBanners ?? data.banners)
          : surface === "worker"
            ? data.promoBanners
            : data.employerPromoBanners;
        const list = listField as PromoBannerDef[] | undefined;
        if (Array.isArray(list) && list.length > 0) {
          setBanners(list);
        } else {
          const presets = PRESET_BANNERS_BY_SURFACE[surface] || PRESET_BANNERS_BY_SURFACE.services;
          setBanners(presets.map((p, i) => ({ id: `banner_${surface}_${i + 1}`, ...p.banner })));
        }
        setSelectedIdx(0);
      } catch (e) {
        onError?.("Could not load promotional banners: " + (e instanceof Error ? e.message : String(e)));
      } finally {
        setLoading(false);
      }
    })();
  }, [services, surface, onError]);

  const activeBanner = banners[selectedIdx] || banners[0];

  function addBlankBanner() {
    const newId = `banner_${Date.now().toString(36)}`;
    const newB: PromoBannerDef = {
      id: newId,
      headline: "Special DutyPe Offer in Khammam",
      subheadline: "Verified doorstep services with 100% satisfaction",
      cta: "Book Service",
      bgStartColor: "#0F172A",
      bgEndColor: "#2563EB",
      targetCategoryId: "CLEANING",
      active: true
    };
    setBanners([...banners, newB]);
    setSelectedIdx(banners.length);
  }

  function addPreset(preset: typeof PRESET_BANNERS[0]) {
    const newId = `banner_${Date.now().toString(36)}`;
    const newB: PromoBannerDef = { id: newId, ...preset.banner };
    setBanners([...banners, newB]);
    setSelectedIdx(banners.length);
  }

  function updateBanner(idx: number, patch: Partial<PromoBannerDef>) {
    setBanners(banners.map((b, i) => (i === idx ? { ...b, ...patch } : b)));
  }

  function removeBanner(idx: number) {
    if (!confirm("Are you sure you want to remove this promotional banner?")) return;
    const next = banners.filter((_, i) => i !== idx);
    setBanners(next);
    setSelectedIdx(Math.max(0, idx - 1));
  }

  const [uploading, setUploading] = useState(false);
  const [lottieAnimationData, setLottieAnimationData] = useState<object | null>(null);

  // Fetch Lottie JSON if lottieUrl exists or imageUrl is a .json file
  useEffect(() => {
    const lottieTarget = activeBanner?.lottieUrl || (activeBanner?.imageUrl?.endsWith(".json") ? activeBanner.imageUrl : null);
    if (!lottieTarget) {
      setLottieAnimationData(null);
      return;
    }
    let cancelled = false;
    fetch(lottieTarget)
      .then((r) => r.json())
      .then((data) => {
        if (!cancelled) setLottieAnimationData(data);
      })
      .catch(() => {
        if (!cancelled) setLottieAnimationData(null);
      });
    return () => {
      cancelled = true;
    };
  }, [activeBanner?.lottieUrl, activeBanner?.imageUrl]);

  async function handleFileUpload(file: File) {
    if (!services || !activeBanner) return;
    setUploading(true);
    setOptStats(null);
    try {
      const isLottie = file.name.endsWith(".json") || file.type === "application/json";
      const bannerId = (activeBanner.id || `banner_${Date.now()}`).toLowerCase().replace(/[^a-z0-9_-]/g, "_");

      let uploadBlob: Blob = file;
      let uploadFileName = file.name;
      let contentType = file.type || "application/octet-stream";

      if (isLottie) {
        // Parse JSON locally for instant zero-latency preview
        const text = await file.text();
        try {
          const json = JSON.parse(text);
          setLottieAnimationData(json);
        } catch {
          // ignore parsing error
        }
        contentType = "application/json";
        uploadFileName = `${bannerId}_${Date.now()}.json`;
      } else {
        // Automatically optimize image (PNG, JPG, etc.) to low-KB WebP directly in browser
        const opt = await optimizeBannerImage(file);
        uploadBlob = opt.webpBlob;
        uploadFileName = `${bannerId}_${Date.now()}.webp`;
        contentType = "image/webp";
        setOptStats({
          originalKb: opt.originalSizeKb,
          webpKb: opt.webpSizeKb,
          savings: opt.savingsPct
        });
      }

      let downloadUrl = "";

      // 1. Try server-side admin upload API route (bypasses client storage permission limits & 403 errors)
      try {
        const formData = new FormData();
        formData.append("image", uploadBlob, uploadFileName);
        formData.append("id", bannerId);
        formData.append("type", "banner");

        const headers: Record<string, string> = {};
        if (services.auth.currentUser) {
          const idToken = await services.auth.currentUser.getIdToken();
          headers["Authorization"] = `Bearer ${idToken}`;
        }

        const apiRes = await fetch("/api/admin/service-catalog/upload-image", {
          method: "POST",
          headers,
          body: formData
        });

        if (apiRes.ok) {
          const resJson = await apiRes.json();
          downloadUrl = resJson.url;
        } else {
          const errData = await apiRes.json().catch(() => null);
          console.warn("Server-side banner upload returned non-200:", errData);
        }
      } catch (apiErr) {
        console.warn("Server-side banner upload failed, falling back to client upload:", apiErr);
      }

      // 2. Client-side fallback if server-side upload was not completed
      if (!downloadUrl) {
        const storageRef = ref(services.storage, `banners/${uploadFileName}`);
        await uploadBytes(storageRef, uploadBlob, {
          contentType,
          cacheControl: "public, max-age=31536000, immutable"
        });
        downloadUrl = await getDownloadURL(storageRef);
      }

      if (isLottie) {
        updateBanner(selectedIdx, {
          lottieUrl: downloadUrl,
          imageUrl: downloadUrl,
          fileSizeBytes: uploadBlob.size
        });
      } else {
        setLottieAnimationData(null);
        updateBanner(selectedIdx, {
          imageUrl: downloadUrl,
          lottieUrl: "",
          fileSizeBytes: uploadBlob.size
        });
      }
    } catch (e) {
      onError?.("Failed to upload file: " + (e instanceof Error ? e.message : String(e)));
    } finally {
      setUploading(false);
    }
  }

  async function saveBanners() {
    if (!services) return;
    setSaving(true);
    try {
      const cleanBanners = banners.map((b) => ({
        id: b.id.trim(),
        headline: b.headline.trim(),
        subheadline: b.subheadline.trim(),
        cta: b.cta.trim() || "Book now",
        bgStartColor: b.bgStartColor.trim() || "#0F172A",
        bgEndColor: b.bgEndColor.trim() || "#1E3A8A",
        targetCategoryId: b.targetCategoryId?.trim() || "",
        targetServiceId: b.targetServiceId?.trim() || "",
        imageUrl: b.imageUrl?.trim() || "",
        lottieUrl: b.lottieUrl?.trim() || "",
        fileSizeBytes: typeof b.fileSizeBytes === "number" ? b.fileSizeBytes : 0,
        imageScale: typeof b.imageScale === "number" ? b.imageScale : 1.0,
        active: b.active !== false
      }));

      const targetDoc = surface === "services" ? "services" : "dynamic_features";
      const fieldName = surface === "services"
        ? "promoBanners"
        : surface === "worker"
          ? "promoBanners"
          : "employerPromoBanners";

      const updatePayload: Record<string, unknown> = {
        ...rawConfig,
        [fieldName]: cleanBanners,
        updatedAt: Date.now()
      };
      if (surface === "services") {
        updatePayload.banners = cleanBanners;
      }

      await setDoc(doc(services.db, "app_config", targetDoc), updatePayload, { merge: true });
      onSaved?.(
        cleanBanners.length > 0
          ? `Promotional banners for ${surface.toUpperCase()} published successfully! Android app will update immediately.`
          : `All promotional banners for ${surface.toUpperCase()} removed.`
      );
    } catch (e) {
      onError?.("Failed to save banners: " + (e instanceof Error ? e.message : String(e)));
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div style={{ padding: 32, textAlign: "center", color: "#64748b" }}>
        Loading promotional banner configuration...
      </div>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Top Header & Overview */}
      <section className="admin-section" style={{ background: "#f8fafc", border: "1px solid #e2e8f0" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: 16 }}>
          <div>
            <h2 className="admin-section-title" style={{ display: "flex", alignItems: "center", gap: 8, margin: 0 }}>
              <Sparkles size={20} color="#2563eb" />
              Promotional Banners &amp; Carousel Hub
            </h2>
            <p style={{ margin: "4px 0 0 0", color: "#64748b", fontSize: 14, maxWidth: 720 }}>
              Configure top-of-screen promotional hero banners across DutyPe Home Services, Worker Home Screen, and Employer Home Screen.
              The mobile app automatically converts uploaded images to high-quality lightweight WebP (20–40 KB) and synchronizes header gradient colors.
            </p>
          </div>
          <button
            onClick={saveBanners}
            disabled={saving}
            className="btn btn-approve"
            style={{ display: "flex", alignItems: "center", gap: 6, padding: "8px 20px", fontWeight: 700, fontSize: 15 }}
          >
            {saving ? <RefreshCw className="animate-spin" size={16} /> : <Check size={16} />}
            {saving ? "Saving Banners..." : `Publish ${surface.toUpperCase()} Banners`}
          </button>
        </div>

        {/* Target Surface Selector */}
        <div style={{ marginTop: 16, display: "flex", gap: 10, borderBottom: "1px solid #cbd5e1", paddingBottom: 12 }}>
          {([
            ["services", "🛠️ DutyPe Home Services"],
            ["worker", "👷 Worker Home Screen"],
            ["employer", "🏢 Employer Home Screen"]
          ] as const).map(([s, label]) => (
            <button
              key={s}
              type="button"
              onClick={() => setSurface(s)}
              className="btn"
              style={{
                padding: "8px 16px",
                fontWeight: surface === s ? 700 : 500,
                background: surface === s ? "#1e293b" : "#ffffff",
                color: surface === s ? "#ffffff" : "#334155",
                border: surface === s ? "1px solid #0f172a" : "1px solid #cbd5e1",
                borderRadius: 8,
                fontSize: 13,
                cursor: "pointer"
              }}
            >
              {label}
            </button>
          ))}
        </div>

        {/* Quick Presets */}
        <div style={{ marginTop: 14, display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
          <span style={{ fontSize: 13, fontWeight: 600, color: "#475569" }}>One-Click Templates:</span>
          {(PRESET_BANNERS_BY_SURFACE[surface] || PRESET_BANNERS_BY_SURFACE.services).map((p, i) => (
            <button
              key={i}
              type="button"
              onClick={() => addPreset(p)}
              className="btn"
              style={{ fontSize: 12.5, padding: "4px 10px", background: "#fff", border: "1px solid #cbd5e1" }}
            >
              + {p.label}
            </button>
          ))}
          <button
            type="button"
            onClick={addBlankBanner}
            className="btn"
            style={{ fontSize: 12.5, padding: "4px 10px", background: "#eff6ff", borderColor: "#93c5fd", color: "#1d4ed8", fontWeight: 600 }}
          >
            <Plus size={14} style={{ display: "inline", verticalAlign: "middle", marginRight: 2 }} />
            Add Custom Slide
          </button>
        </div>
      </section>

      {/* Main Grid: Left is live preview & slide selector, Right is active slide editor */}
      <div style={{ display: "grid", gridTemplateColumns: "minmax(340px, 420px) 1fr", gap: 24, alignItems: "start" }}>
        {/* Left: Mobile App Simulator & Slide Selector */}
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: "#1e293b", textTransform: "uppercase", letterSpacing: "0.05em", display: "flex", alignItems: "center", gap: 6 }}>
            <Smartphone size={16} />
            Live Mobile App Header Preview
          </div>

          {/* Interactive Mobile Header Mockup */}
          {activeBanner && (
            <div
              style={{
                borderRadius: 24,
                overflow: "hidden",
                border: "4px solid #0f172a",
                boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)",
                background: "#f8fafc"
              }}
            >
              {/* Fake Mobile Status Bar */}
              <div
                style={{
                  background: activeBanner.bgStartColor,
                  color: "#fff",
                  padding: "10px 16px 6px",
                  display: "flex",
                  justifyContent: "space-between",
                  fontSize: 12,
                  fontWeight: 600,
                  transition: "background 0.4s ease"
                }}
              >
                <span>9:41</span>
                <span>📶 5G  🔋 98%</span>
              </div>

              {/* Dynamic Header & Location Bar */}
              <div
                style={{
                  background: activeBanner.bgStartColor,
                  color: "#fff",
                  padding: "6px 16px 14px",
                  transition: "background 0.4s ease"
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <div>
                    <div style={{ fontSize: 14, fontWeight: 800 }}>⚡ In 59 minutes</div>
                    <div style={{ fontSize: 11, opacity: 0.85 }}>📍 KHAMMAM CITY (WYRA ROAD) ▾</div>
                  </div>
                  <div style={{ display: "flex", gap: 6 }}>
                    <div style={{ width: 28, height: 28, borderRadius: 8, background: "rgba(255,255,255,0.2)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12 }}>🧾</div>
                    <div style={{ width: 28, height: 28, borderRadius: 8, background: "rgba(255,255,255,0.2)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12 }}>👤</div>
                  </div>
                </div>

                {/* Search Bar */}
                <div style={{ marginTop: 10, background: "#fff", borderRadius: 10, padding: "8px 12px", display: "flex", alignItems: "center", gap: 8, color: "#64748b", fontSize: 12 }}>
                  <span>🔍</span>
                  <span>Search &apos;AC repair&apos;, &apos;Electrician&apos;...</span>
                </div>
              </div>

              {/* Promo Banner Slide Body with Gradient */}
              <div
                style={{
                  background: `linear-gradient(135deg, ${activeBanner.bgStartColor} 0%, ${activeBanner.bgEndColor} 100%)`,
                  padding: "20px 18px",
                  color: "#ffffff",
                  position: "relative",
                  minHeight: 140,
                  display: "flex",
                  flexDirection: "column",
                  justifyContent: "space-between",
                  transition: "all 0.4s ease"
                }}
              >
                <div>
                  <div style={{ display: "inline-block", background: "rgba(255,255,255,0.18)", padding: "2px 8px", borderRadius: 20, fontSize: 10.5, fontWeight: 700, marginBottom: 8 }}>
                    FEATURED PROMOTION
                  </div>
                  <div style={{ fontSize: 17, fontWeight: 800, lineHeight: 1.25, maxWidth: "80%", textShadow: "0 1px 2px rgba(0,0,0,0.2)" }}>
                    {activeBanner.headline || "Headline Here"}
                  </div>
                </div>

                <div style={{ marginTop: 14, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <button
                    type="button"
                    style={{
                      background: "#ffffff",
                      color: "#0f172a",
                      border: "none",
                      borderRadius: 8,
                      padding: "6px 14px",
                      fontSize: 12,
                      fontWeight: 700,
                      cursor: "default"
                    }}
                  >
                    {activeBanner.cta || "Book Now"} →
                  </button>

                  {/* Pager Dots simulation */}
                  <div style={{ display: "flex", gap: 4 }}>
                    {banners.map((_, i) => (
                      <span
                        key={i}
                        style={{
                          width: i === selectedIdx ? 16 : 6,
                          height: 6,
                          borderRadius: 3,
                          background: i === selectedIdx ? "#fff" : "rgba(255,255,255,0.4)",
                          transition: "all 0.3s ease"
                        }}
                      />
                    ))}
                  </div>
                </div>

                {lottieAnimationData ? (
                  <div
                    style={{
                      position: "absolute",
                      right: 8,
                      top: 14,
                      width: Math.round(76 * (activeBanner.imageScale || 1.0)),
                      height: Math.round(76 * (activeBanner.imageScale || 1.0)),
                      filter: "drop-shadow(0 4px 6px rgba(0,0,0,0.25))",
                      pointerEvents: "none"
                    }}
                  >
                    <Lottie
                      animationData={lottieAnimationData}
                      loop
                      autoplay
                      style={{ width: "100%", height: "100%" }}
                    />
                  </div>
                ) : activeBanner.imageUrl ? (
                  <img
                    src={activeBanner.imageUrl}
                    alt="Promo"
                    style={{
                      position: "absolute",
                      right: 12,
                      top: 18,
                      width: Math.round(68 * (activeBanner.imageScale || 1.0)),
                      height: Math.round(68 * (activeBanner.imageScale || 1.0)),
                      objectFit: "contain",
                      filter: "drop-shadow(0 4px 6px rgba(0,0,0,0.25))",
                      transition: "all 0.2s ease"
                    }}
                  />
                ) : null}
              </div>

              {/* Fake Catalog Preview Below Banner */}
              <div style={{ padding: 14, background: "#f8fafc" }}>
                <div style={{ fontSize: 11, fontWeight: 700, color: "#64748b", textTransform: "uppercase" }}>Quick Categories</div>
                <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
                  {["⚡ AC", "🧹 Clean", "🔌 Electric", "🔧 Plumber"].map((cat, i) => (
                    <div key={i} style={{ flex: 1, background: "#fff", border: "1px solid #e2e8f0", borderRadius: 8, padding: "8px 4px", textAlign: "center", fontSize: 11, fontWeight: 600 }}>
                      {cat}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Slide Switcher List */}
          <div style={{ marginTop: 8 }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: "#64748b", marginBottom: 6 }}>All Carousel Slides ({banners.length}):</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
              {banners.map((b, i) => (
                <div
                  key={b.id || i}
                  onClick={() => setSelectedIdx(i)}
                  style={{
                    padding: "8px 12px",
                    borderRadius: 8,
                    border: i === selectedIdx ? "2px solid #2563eb" : "1px solid #e2e8f0",
                    background: i === selectedIdx ? "#eff6ff" : "#fff",
                    cursor: "pointer",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center"
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: 8, overflow: "hidden" }}>
                    <span
                      style={{
                        width: 14,
                        height: 14,
                        borderRadius: "50%",
                        background: `linear-gradient(135deg, ${b.bgStartColor}, ${b.bgEndColor})`,
                        flexShrink: 0
                      }}
                    />
                    <span style={{ fontSize: 13, fontWeight: i === selectedIdx ? 700 : 500, color: "#0f172A", whiteSpace: "nowrap", textOverflow: "ellipsis", overflow: "hidden" }}>
                      Slide {i + 1}: {b.headline}
                    </span>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", gap: 6, flexShrink: 0 }}>
                    {!b.active && <span style={{ fontSize: 10, background: "#fee2e2", color: "#dc2626", padding: "1px 6px", borderRadius: 4, fontWeight: 700 }}>OFF</span>}
                    <button
                      type="button"
                      onClick={(e) => { e.stopPropagation(); removeBanner(i); }}
                      style={{ background: "none", border: "none", color: "#ef4444", cursor: "pointer", padding: "2px 4px", borderRadius: 4 }}
                      title="Delete Slide"
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right: Slide Editor Form */}
        {activeBanner && (
          <div className="admin-section" style={{ background: "#ffffff", border: "1px solid #e2e8f0", padding: 20 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
              <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: "#0f172a" }}>
                Edit Slide {selectedIdx + 1}: &ldquo;{activeBanner.headline}&rdquo;
              </h3>
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <label style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 13, fontWeight: 600, cursor: "pointer" }}>
                  <input
                    type="checkbox"
                    checked={activeBanner.active !== false}
                    onChange={(e) => updateBanner(selectedIdx, { active: e.target.checked })}
                  />
                  Active in App Carousel
                </label>
                <button
                  type="button"
                  onClick={() => removeBanner(selectedIdx)}
                  style={{
                    background: "#fee2e2",
                    color: "#dc2626",
                    border: "1px solid #fca5a5",
                    borderRadius: 6,
                    padding: "4px 10px",
                    fontSize: 12,
                    fontWeight: 600,
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                    gap: 4
                  }}
                  title="Delete this banner slide"
                >
                  <Trash2 size={13} />
                  Delete Slide
                </button>
              </div>
            </div>

            {/* SECTION 1: CORE ESSENTIALS (WHAT REALLY MATTERS) */}
            <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: 10, padding: 16, marginBottom: 16 }}>
              <div style={{ fontSize: 13, fontWeight: 800, color: "#1e293b", marginBottom: 12, display: "flex", alignItems: "center", gap: 6 }}>
                <span>⚡</span>
                <span>Core Banner Essentials (Required)</span>
              </div>

              <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                {/* Headline with word & char counter */}
                <div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
                    <label style={{ fontSize: 12, fontWeight: 700, color: "#334155" }}>
                      Headline (Main bold offer) *
                    </label>
                    <span style={{
                      fontSize: 11,
                      fontWeight: 700,
                      color: activeBanner.headline.length > 36 ? "#dc2626" : "#2563eb"
                    }}>
                      {activeBanner.headline.length}/36 chars (Recommended: 4–6 words)
                    </span>
                  </div>
                  <input
                    type="text"
                    maxLength={40}
                    value={activeBanner.headline}
                    onChange={(e) => updateBanner(selectedIdx, { headline: e.target.value })}
                    placeholder="e.g. Khammam AC Service ⚡ Flat ₹499"
                    style={{
                      width: "100%",
                      padding: "8px 12px",
                      borderRadius: 6,
                      border: activeBanner.headline.length > 36 ? "1px solid #ef4444" : "1px solid #cbd5e1",
                      fontSize: 14,
                      fontWeight: 600
                    }}
                  />
                  <div style={{ fontSize: 11, color: "#64748b", marginTop: 4 }}>
                    💡 Keep it punchy! Appears in large bold text on the mobile banner card.
                  </div>
                </div>

                {/* CTA Button Text with character counter */}
                <div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
                    <label style={{ fontSize: 12, fontWeight: 700, color: "#334155" }}>
                      Action Button Text (Pill with animated running border) *
                    </label>
                    <span style={{
                      fontSize: 11,
                      fontWeight: 700,
                      color: activeBanner.cta.length > 14 ? "#dc2626" : "#2563eb"
                    }}>
                      {activeBanner.cta.length}/14 chars (Max 2 words)
                    </span>
                  </div>
                  <input
                    type="text"
                    maxLength={16}
                    value={activeBanner.cta}
                    onChange={(e) => updateBanner(selectedIdx, { cta: e.target.value })}
                    placeholder="e.g. Book Now"
                    style={{
                      width: "100%",
                      padding: "8px 12px",
                      borderRadius: 6,
                      border: activeBanner.cta.length > 14 ? "1px solid #ef4444" : "1px solid #cbd5e1",
                      fontSize: 14
                    }}
                  />
                  <div style={{ fontSize: 11, color: "#64748b", marginTop: 4 }}>
                    💡 Renders inside the high-converting animated glowing border pill button.
                  </div>
                </div>

                {/* Graphic / Animation Uploader (Lottie & Images) */}
                <div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                    <label style={{ fontSize: 12, fontWeight: 700, color: "#334155", display: "flex", alignItems: "center", gap: 6 }}>
                      <span>Banner Graphic or Lottie Animation</span>
                      {activeBanner.lottieUrl ? (
                        <span style={{ fontSize: 10, padding: "2px 7px", borderRadius: 12, background: "#f3e8ff", color: "#7e22ce", fontWeight: 800 }}>
                          ✨ LOTTIE ANIMATION
                        </span>
                      ) : activeBanner.imageUrl ? (
                        <span style={{ fontSize: 10, padding: "2px 7px", borderRadius: 12, background: "#eff6ff", color: "#1d4ed8", fontWeight: 800 }}>
                          🖼️ STATIC IMAGE
                        </span>
                      ) : null}
                    </label>
                    {activeBanner.fileSizeBytes ? (
                      <span
                        style={{
                          fontSize: 11,
                          fontWeight: 700,
                          padding: "2px 8px",
                          borderRadius: 12,
                          background:
                            activeBanner.fileSizeBytes < 60 * 1024
                              ? "#ecfdf5"
                              : activeBanner.fileSizeBytes < 150 * 1024
                              ? "#fffbeb"
                              : "#fef2f2",
                          color:
                            activeBanner.fileSizeBytes < 60 * 1024
                              ? "#047857"
                              : activeBanner.fileSizeBytes < 150 * 1024
                              ? "#b45309"
                              : "#b91c1c"
                        }}
                      >
                        Size: {(activeBanner.fileSizeBytes / 1024).toFixed(1)} KB{" "}
                        {activeBanner.fileSizeBytes < 60 * 1024
                          ? "✓ Optimal"
                          : activeBanner.fileSizeBytes < 150 * 1024
                          ? "⚠ Moderate"
                          : "⚠ High"}
                      </span>
                    ) : null}
                  </div>

                  {/* Direct File Upload Drop Zone */}
                  <div
                    style={{
                      border: "2px dashed #cbd5e1",
                      borderRadius: 8,
                      padding: "14px 16px",
                      background: "#ffffff",
                      textAlign: "center",
                      marginBottom: 10
                    }}
                  >
                    <input
                      type="file"
                      id="banner-file-upload"
                      accept=".json,.png,.webp,.jpg,.jpeg,.gif,.svg"
                      disabled={uploading}
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) handleFileUpload(file);
                        e.target.value = "";
                      }}
                      style={{ display: "none" }}
                    />
                    <label
                      htmlFor="banner-file-upload"
                      style={{
                        cursor: uploading ? "wait" : "pointer",
                        display: "flex",
                        flexDirection: "column",
                        alignItems: "center",
                        gap: 6
                      }}
                    >
                      <div
                        style={{
                          width: 40,
                          height: 40,
                          borderRadius: "50%",
                          background: "#eff6ff",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          color: "#2563eb"
                        }}
                      >
                        {uploading ? (
                          <RefreshCw className="animate-spin" size={20} />
                        ) : (
                          <Upload size={20} />
                        )}
                      </div>
                      <div style={{ fontSize: 13, fontWeight: 700, color: "#1e293b" }}>
                        {uploading ? "Compressing to WebP & Uploading..." : "Click to Upload Image or Lottie File"}
                      </div>
                      <div style={{ fontSize: 11, color: "#64748b" }}>
                        PNG / JPG images are <b>auto-converted to low-KB WebP</b> • Lottie <b>.json</b> animations supported
                      </div>
                    </label>
                  </div>

                  {/* Real-time WebP Compression Savings Badge */}
                  {optStats && (
                    <div
                      style={{
                        marginBottom: 10,
                        padding: "8px 12px",
                        background: "#f0fdf4",
                        border: "1px solid #bbf7d0",
                        borderRadius: 6,
                        display: "flex",
                        alignItems: "center",
                        gap: 8,
                        fontSize: 12,
                        color: "#166534"
                      }}
                    >
                      <Check size={16} color="#16a34a" />
                      <span>
                        <b>WebP Auto-Compression Applied:</b> {optStats.originalKb} KB &rarr;{" "}
                        <strong style={{ color: "#15803d" }}>{optStats.webpKb} KB</strong> ({optStats.savings}% size reduction, ultra-fast mobile loading!)
                      </span>
                    </div>
                  )}

                  {/* Direct URL Input Fallback */}
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <input
                      type="text"
                      value={activeBanner.lottieUrl || activeBanner.imageUrl || ""}
                      onChange={(e) => {
                        const val = e.target.value.trim();
                        const isJson = val.endsWith(".json");
                        updateBanner(selectedIdx, {
                          imageUrl: val,
                          lottieUrl: isJson ? val : "",
                          fileSizeBytes: undefined
                        });
                      }}
                      placeholder="Or paste asset URL (https://... or .json for Lottie)"
                      style={{
                        flex: 1,
                        padding: "7px 10px",
                        borderRadius: 6,
                        border: "1px solid #cbd5e1",
                        fontSize: 12
                      }}
                    />
                    {(activeBanner.imageUrl || activeBanner.lottieUrl) && (
                      <button
                        type="button"
                        onClick={() =>
                          updateBanner(selectedIdx, {
                            imageUrl: "",
                            lottieUrl: "",
                            fileSizeBytes: undefined
                          })
                        }
                        className="btn"
                        style={{
                          fontSize: 11,
                          padding: "6px 10px",
                          background: "#fef2f2",
                          color: "#dc2626",
                          borderColor: "#fecaca"
                        }}
                      >
                        Clear
                      </button>
                    )}
                  </div>

                  {/* Image/Lottie Sizing Feature */}
                  <div style={{ marginTop: 12, padding: 12, background: "#ffffff", borderRadius: 8, border: "1px solid #e2e8f0" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8, flexWrap: "wrap", gap: 8 }}>
                      <label style={{ fontSize: 12, fontWeight: 700, color: "#1e293b", display: "flex", alignItems: "center", gap: 6 }}>
                        <span>🔍 Graphic Display Scale:</span>
                        <span style={{ color: "#2563eb", fontWeight: 800 }}>{Math.round((activeBanner.imageScale ?? 1.0) * 100)}%</span>
                      </label>
                      <div style={{ display: "flex", gap: 6 }}>
                        <button
                          type="button"
                          onClick={() => updateBanner(selectedIdx, { imageScale: 0.8 })}
                          className="btn"
                          style={{
                            fontSize: 11,
                            padding: "3px 8px",
                            background: activeBanner.imageScale === 0.8 ? "#eff6ff" : "#fff",
                            borderColor: activeBanner.imageScale === 0.8 ? "#2563eb" : "#cbd5e1",
                            color: activeBanner.imageScale === 0.8 ? "#1d4ed8" : "#475569",
                            fontWeight: 600
                          }}
                        >
                          Compact (80%)
                        </button>
                        <button
                          type="button"
                          onClick={() => updateBanner(selectedIdx, { imageScale: 1.0 })}
                          className="btn"
                          style={{
                            fontSize: 11,
                            padding: "3px 8px",
                            background: (!activeBanner.imageScale || activeBanner.imageScale === 1.0) ? "#eff6ff" : "#fff",
                            borderColor: (!activeBanner.imageScale || activeBanner.imageScale === 1.0) ? "#2563eb" : "#cbd5e1",
                            color: (!activeBanner.imageScale || activeBanner.imageScale === 1.0) ? "#1d4ed8" : "#475569",
                            fontWeight: 600
                          }}
                        >
                          Standard (100%)
                        </button>
                        <button
                          type="button"
                          onClick={() => updateBanner(selectedIdx, { imageScale: 1.2 })}
                          className="btn"
                          style={{
                            fontSize: 11,
                            padding: "3px 8px",
                            background: activeBanner.imageScale === 1.2 ? "#eff6ff" : "#fff",
                            borderColor: activeBanner.imageScale === 1.2 ? "#2563eb" : "#cbd5e1",
                            color: activeBanner.imageScale === 1.2 ? "#1d4ed8" : "#475569",
                            fontWeight: 600
                          }}
                        >
                          Large (120%)
                        </button>
                      </div>
                    </div>
                    <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                      <span style={{ fontSize: 11, color: "#64748b" }}>70% (Smaller)</span>
                      <input
                        type="range"
                        min="0.7"
                        max="1.3"
                        step="0.05"
                        value={activeBanner.imageScale ?? 1.0}
                        onChange={(e) => updateBanner(selectedIdx, { imageScale: parseFloat(e.target.value) })}
                        style={{ flex: 1, accentColor: "#2563eb", cursor: "pointer" }}
                      />
                      <span style={{ fontSize: 11, color: "#64748b" }}>130% (Larger)</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* SECTION 2: OPTIONAL SETTINGS & ROUTING */}
            <div style={{ background: "#ffffff", border: "1px solid #e2e8f0", borderRadius: 10, padding: 16 }}>
              <div style={{ fontSize: 13, fontWeight: 800, color: "#475569", marginBottom: 12, display: "flex", alignItems: "center", gap: 6 }}>
                <span>🎨</span>
                <span>Optional Customizations &amp; Routing</span>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14 }}>

                {/* Target Navigation */}
                <div>
                  <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                    Target Category (Optional)
                  </label>
                  <select
                    value={activeBanner.targetCategoryId || ""}
                    onChange={(e) => updateBanner(selectedIdx, { targetCategoryId: e.target.value })}
                    style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  >
                    <option value="">None (Generic)</option>
                    <option value="AC">AC Service &amp; Repair</option>
                    <option value="CLEANING">Home Cleaning</option>
                    <option value="ELECTRICIAN">Electrician</option>
                    <option value="PLUMBER">Plumber</option>
                    <option value="APPLIANCE">Appliance &amp; RO Repair</option>
                    <option value="CARPENTER">Carpenter</option>
                    <option value="PAINTER">Painting</option>
                    <option value="HOME_HELP">Home Help &amp; Shifting</option>
                    <option value="VEHICLE">Car &amp; Bike Wash</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                    Target Specific Service ID (Optional)
                  </label>
                  <input
                    type="text"
                    value={activeBanner.targetServiceId || ""}
                    onChange={(e) => updateBanner(selectedIdx, { targetServiceId: e.target.value })}
                    placeholder="e.g. ac_service_split_1"
                    style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  />
                </div>

                {/* Gradient Colors */}
                <div>
                  <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                    Gradient Start (Also sets Mobile App Header!)
                  </label>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <input
                      type="color"
                      value={activeBanner.bgStartColor.startsWith("#") ? activeBanner.bgStartColor : "#0F172A"}
                      onChange={(e) => updateBanner(selectedIdx, { bgStartColor: e.target.value })}
                      style={{ width: 40, height: 36, padding: 0, border: "1px solid #cbd5e1", borderRadius: 4, cursor: "pointer" }}
                    />
                    <input
                      type="text"
                      value={activeBanner.bgStartColor}
                      onChange={(e) => updateBanner(selectedIdx, { bgStartColor: e.target.value })}
                      placeholder="#0F172A"
                      style={{ flex: 1, padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontFamily: "monospace" }}
                    />
                  </div>
                </div>

                <div>
                  <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                    Gradient End Color
                  </label>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <input
                      type="color"
                      value={activeBanner.bgEndColor.startsWith("#") ? activeBanner.bgEndColor : "#1E3A8A"}
                      onChange={(e) => updateBanner(selectedIdx, { bgEndColor: e.target.value })}
                      style={{ width: 40, height: 36, padding: 0, border: "1px solid #cbd5e1", borderRadius: 4, cursor: "pointer" }}
                    />
                    <input
                      type="text"
                      value={activeBanner.bgEndColor}
                      onChange={(e) => updateBanner(selectedIdx, { bgEndColor: e.target.value })}
                      placeholder="#1E3A8A"
                      style={{ flex: 1, padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontFamily: "monospace" }}
                    />
                  </div>
                </div>

                <div style={{ gridColumn: "span 2" }}>
                  <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                    Slide Unique ID (Optional)
                  </label>
                  <input
                    type="text"
                    value={activeBanner.id}
                    onChange={(e) => updateBanner(selectedIdx, { id: e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, "") })}
                    placeholder="e.g. banner_ac_summer"
                    style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  />
                </div>
              </div>
            </div>

            <div style={{ marginTop: 20, paddingTop: 16, borderTop: "1px solid #e2e8f0", display: "flex", justifyContent: "flex-end" }}>
              <button
                type="button"
                onClick={saveBanners}
                disabled={saving}
                className="btn btn-approve"
                style={{ padding: "8px 24px", fontWeight: 700 }}
              >
                {saving ? "Saving..." : "Save All Banners"}
              </button>
            </div>
          </div>
        )}

        {banners.length === 0 && (
          <div className="admin-section" style={{ background: "#ffffff", border: "2px dashed #cbd5e1", borderRadius: 12, padding: 48, textAlign: "center" }}>
            <div style={{ fontSize: 36, marginBottom: 12 }}>🏷️</div>
            <h3 style={{ margin: "0 0 8px 0", fontSize: 18, fontWeight: 700, color: "#1e293b" }}>No Promotional Banners</h3>
            <p style={{ margin: "0 0 20px 0", color: "#64748b", fontSize: 14, maxWidth: 460, marginLeft: "auto", marginRight: "auto" }}>
              All promotional banners have been removed. The mobile app will not show a promotional hero banner on the services home screen.
            </p>
            <div style={{ display: "flex", justifyContent: "center", gap: 12 }}>
              <button
                type="button"
                onClick={addBlankBanner}
                className="btn btn-primary"
                style={{ fontSize: 13, padding: "8px 16px" }}
              >
                + Create a Banner
              </button>
              <button
                type="button"
                onClick={saveBanners}
                disabled={saving}
                className="btn btn-approve"
                style={{ fontSize: 13, padding: "8px 16px" }}
              >
                {saving ? "Publishing..." : "Publish Cleared State to App"}
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Testing Instructions & Verification Guide */}
      <section className="admin-section" style={{ background: "#f0fdf4", border: "1px solid #bbf7d0" }}>
        <h4 style={{ margin: "0 0 8px 0", color: "#166534", fontSize: 15, fontWeight: 700, display: "flex", alignItems: "center", gap: 6 }}>
          <Check size={18} />
          How to Test Promotional Banners on Mobile:
        </h4>
        <ol style={{ margin: 0, paddingLeft: 20, color: "#15803d", fontSize: 13.5, lineHeight: 1.6 }}>
          <li>
            <b>Instant Mobile Sync:</b> Tap <i>&quot;Publish Banners to App&quot;</i> above. The configuration is immediately saved to Firestore <code style={{ background: "rgba(0,0,0,0.06)", padding: "1px 5px", borderRadius: 4 }}>app_config/services</code>.
          </li>
          <li>
            <b>Open DutyPe Android App:</b> Launch the app as an Employer or Customer. The home screen checks Firestore and renders the updated carousel at the top of the Services screen.
          </li>
          <li>
            <b>Observe Dynamic Header Colors:</b> Notice how as the carousel auto-scrolls through slides (every 5 seconds), the entire top status bar and search header smoothly morph their gradient color to match each banner!
          </li>
          <li>
            <b>Test Click-Through:</b> Tapping the banner button immediately navigates customers directly into the target category or service booking modal.
          </li>
        </ol>
      </section>
    </div>
  );
}
