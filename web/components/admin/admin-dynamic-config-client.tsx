"use client";

import { useEffect, useMemo, useState } from "react";
import { doc, getDoc, onSnapshot, serverTimestamp, setDoc, Timestamp } from "firebase/firestore";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";

import { getFirebaseServices } from "@/lib/firebase/client";
import { LottiePreview } from "./lottie-preview";

type DynamicConfig = {
  isDirectCallEnabled: boolean;
  isUrgentJobsEnabled: boolean;
  activeLauncherIcon: "default" | "birthday" | "independence";
  promoBannerUrl: string;
  employerPromoBannerUrl: string;
  lottieLoadingUrl: string;
  primaryColor: string; // Worker Primary Color
  headerTextColor: string; // Worker Header Text Color
  employerPrimaryColor: string; // Employer Primary Color
  headerLottieUrl: string;
  launchPromoEnabled: boolean;
  launchPromoMediaType: "IMAGE" | "ANIMATION";
  launchPromoBannerUrl: string;
  launchPromoAnimationUrl: string;
  launchPromoTitle: string;
  launchPromoActionRoute: string;
  launchPromoDurationSec: number;
  launchPromoButtonText: string;
  inAppDialogEnabled: boolean;
  inAppDialogImageUrl: string;
  inAppDialogTitle: string;
  inAppDialogActionRoute: string;
  inAppDialogTargetAudience: "ALL" | "WORKER" | "EMPLOYER" | "SERVICES";
  updatedAt?: Timestamp | null;
  updatedBy?: string | null;
};

const DEFAULTS: DynamicConfig = {
  isDirectCallEnabled: true,
  isUrgentJobsEnabled: true,
  activeLauncherIcon: "default",
  promoBannerUrl: "",
  employerPromoBannerUrl: "",
  lottieLoadingUrl: "",
  primaryColor: "#1E3A8A",
  headerTextColor: "#FFFFFF",
  employerPrimaryColor: "#0F0F0F",
  headerLottieUrl: "",
  launchPromoEnabled: false,
  launchPromoMediaType: "IMAGE",
  launchPromoBannerUrl: "",
  launchPromoAnimationUrl: "",
  launchPromoTitle: "",
  launchPromoActionRoute: "",
  launchPromoDurationSec: 5,
  launchPromoButtonText: "",
  inAppDialogEnabled: false,
  inAppDialogImageUrl: "",
  inAppDialogTitle: "",
  inAppDialogActionRoute: "",
  inAppDialogTargetAudience: "ALL"
};

async function compressToTarget4GKb(
  file: File,
  targetMaxKb: number = 60,
  isVerticalTarget: boolean = false
): Promise<{ blob: Blob; sizeKb: number; width: number; height: number; savingsPct: number; isNonVerticalWarning?: boolean }> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = async () => {
        const origW = img.width;
        const origH = img.height;
        const isNonVertical = isVerticalTarget && (origW >= origH || origW / origH > 0.70);

        let maxWidth = isVerticalTarget ? 1080 : 1280;
        let maxHeight = isVerticalTarget ? 1920 : 1280;
        let w = origW;
        let h = origH;
        if (w > maxWidth || h > maxHeight) {
          const ratio = Math.min(maxWidth / w, maxHeight / h);
          w = Math.round(w * ratio);
          h = Math.round(h * ratio);
        }

        const canvas = document.createElement("canvas");
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext("2d");
        if (!ctx) return reject(new Error("Canvas context creation failed"));
        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = "high";
        ctx.drawImage(img, 0, 0, w, h);

        let quality = 0.82;
        let blob: Blob | null = null;
        for (let i = 0; i < 5; i++) {
          blob = await new Promise<Blob | null>((res) => canvas.toBlob(res, "image/webp", quality));
          if (!blob) break;
          const currentKb = blob.size / 1024;
          if (currentKb <= targetMaxKb || quality <= 0.45) break;
          quality -= 0.12;
        }

        const finalBlob = blob || file;
        const finalKb = Math.round((finalBlob.size / 1024) * 10) / 10;
        const savings = Math.max(0, Math.round(((file.size - finalBlob.size) / file.size) * 100));

        resolve({
          blob: finalBlob,
          sizeKb: finalKb,
          width: origW,
          height: origH,
          savingsPct: savings,
          isNonVerticalWarning: isNonVertical
        });
      };
      img.onerror = () => reject(new Error("Failed to load image"));
      img.src = e.target?.result as string;
    };
    reader.onerror = () => reject(new Error("Failed to read file"));
    reader.readAsDataURL(file);
  });
}

export function AdminDynamicConfigClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [config, setConfig] = useState<DynamicConfig>(DEFAULTS);
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<string>("");

  // Clean Navigation Tabs
  const [mainTab, setMainTab] = useState<"promos" | "campaign" | "theme" | "features">("promos");
  const [themeRoleTab, setThemeRoleTab] = useState<"worker" | "employer">("worker");

  // Launch Promo Dimension Warning & Info
  const [launchPromoWarning, setLaunchPromoWarning] = useState<string | null>(null);
  const [launchPromoInfo, setLaunchPromoInfo] = useState<{ width: number; height: number; kb: number } | null>(null);

  // Dynamic Campaign / Promo Microsite State
  const [campaign, setCampaign] = useState({
    title: "Hungr - Morning Tiffins at Shop Prices",
    tagline: "Pre-order hot morning breakfast directly from top local Khammam tiffin centers at exact shop counter prices with zero markup.",
    categoryBadge: "EXCLUSIVE LAUNCH",
    rating: "4.9",
    mediaType: "IMAGE" as "IMAGE" | "LOTTIE",
    heroImageUrl: "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=1000&auto=format&fit=crop&q=80",
    videoUrl: "",
    aboutDescription: "DutyPe is launching Hungr: a revolutionary community-powered morning food delivery platform. Order your favorite Idli, Dosa, Vada & pure fruit juices the previous evening between 5 PM - 11 PM. Our batch-delivery vehicles deliver straight to your apartment security gate by 7:30 AM at genuine offline shop prices.",
    address: "Khammam Town & Residential Apartments, Telangana",
    discountOfferTitle: "Exclusive VIP Member Voucher (₹50 OFF)",
    discountOfferCode: "HUNGR50",
    discountOfferTerms: "Valid on first 3 breakfast pre-orders when joining priority waitlist",
    highlights: [
      "🥞 Idli, Dosa, Vada & Juices at ₹30 - ₹40 (Exact Offline Shop Rate)",
      "🛵 Batch Delivery to Apartment Security Gate (0 Delivery Markup)",
      "⏰ Pre-Order Window: 5:00 PM – 11:00 PM every evening",
      "✨ 100% Fresh from Top Rated Local Khammam Tiffin Centers"
    ],
    primaryButtonLabel: "Join VIP Priority Waitlist",
    primaryButtonAction: "hungr",
    secondaryButtonLabel: "WhatsApp Inquiry",
    secondaryButtonAction: "https://wa.me/918019151847?text=Hi%20DutyPe,%20I%20want%20to%20know%20more%20about%20Hungr",
    active: true
  });
  const [newHighlight, setNewHighlight] = useState("");
  const [savingCampaign, setSavingCampaign] = useState(false);
  const [campaignMessage, setCampaignMessage] = useState<string | null>(null);

  // Load Active Dynamic Campaign from Firestore
  useEffect(() => {
    if (!services) return;
    const fetchCampaign = async () => {
      try {
        const snap = await getDoc(doc(services.db, "promo_showcases", "active_campaign"));
        if (snap.exists()) {
          const d = snap.data();
          const btns = (d.actionButtons as Array<{ label: string; actionUrl: string; isPrimary: boolean }>) || [];
          const prim = btns.find((b) => b.isPrimary) || btns[0] || { label: "Join VIP Waitlist", actionUrl: "hungr" };
          const sec = btns.find((b) => !b.isPrimary) || btns[1] || { label: "WhatsApp Inquiry", actionUrl: "https://wa.me/918019151847" };
          const isLottie = String(d.heroImageUrl || d.videoUrl || "").includes(".json");
          setCampaign({
            title: String(d.title || "Hungr - Morning Tiffins at Shop Prices"),
            tagline: String(d.tagline || ""),
            categoryBadge: String(d.categoryBadge || "EXCLUSIVE LAUNCH"),
            rating: String(d.rating || "4.9"),
            mediaType: isLottie ? "LOTTIE" : "IMAGE",
            heroImageUrl: String(d.heroImageUrl || ""),
            videoUrl: String(d.videoUrl || ""),
            aboutDescription: String(d.aboutDescription || ""),
            address: String(d.address || ""),
            discountOfferTitle: String(d.discountOfferTitle || ""),
            discountOfferCode: String(d.discountOfferCode || ""),
            discountOfferTerms: String(d.discountOfferTerms || ""),
            highlights: Array.isArray(d.highlights) && d.highlights.length > 0 ? d.highlights : [
              "🥞 Idli, Dosa, Vada & Juices at ₹30 - ₹40 (Exact Offline Shop Rate)",
              "🛵 Batch Delivery to Apartment Security Gate (0 Delivery Markup)",
              "⏰ Pre-Order Window: 5:00 PM – 11:00 PM every evening",
              "✨ 100% Fresh from Top Rated Local Khammam Tiffin Centers"
            ],
            primaryButtonLabel: String(prim.label || "Join VIP Priority Waitlist"),
            primaryButtonAction: String(prim.actionUrl || "hungr"),
            secondaryButtonLabel: String(sec.label || "WhatsApp Inquiry"),
            secondaryButtonAction: String(sec.actionUrl || "https://wa.me/918019151847"),
            active: d.active !== false
          });
        }
      } catch (err) {
        console.warn("Could not load active_campaign:", err);
      }
    };
    void fetchCampaign();
  }, [services]);

  async function handleSaveCampaign() {
    if (!services) return;
    setSavingCampaign(true);
    setCampaignMessage(null);
    try {
      const payload = {
        id: "active_campaign",
        title: campaign.title.trim(),
        tagline: campaign.tagline.trim(),
        categoryBadge: campaign.categoryBadge.trim(),
        rating: campaign.rating.trim(),
        heroImageUrl: campaign.mediaType === "LOTTIE" ? (campaign.videoUrl.trim() || campaign.heroImageUrl.trim()) : campaign.heroImageUrl.trim(),
        videoUrl: campaign.mediaType === "LOTTIE" ? campaign.videoUrl.trim() : "",
        aboutDescription: campaign.aboutDescription.trim(),
        address: campaign.address.trim(),
        mapLink: "",
        discountOfferTitle: campaign.discountOfferTitle.trim(),
        discountOfferCode: campaign.discountOfferCode.trim(),
        discountOfferTerms: campaign.discountOfferTerms.trim(),
        highlights: campaign.highlights.filter((h) => h.trim().length > 0),
        galleryImages: [],
        actionButtons: [
          {
            label: campaign.primaryButtonLabel.trim(),
            icon: campaign.primaryButtonAction.includes("hungr") ? "hungr" : campaign.primaryButtonAction.includes("wa.me") ? "whatsapp" : "call",
            actionUrl: campaign.primaryButtonAction.trim(),
            isPrimary: true
          },
          {
            label: campaign.secondaryButtonLabel.trim(),
            icon: campaign.secondaryButtonAction.includes("wa.me") ? "whatsapp" : "call",
            actionUrl: campaign.secondaryButtonAction.trim(),
            isPrimary: false
          }
        ],
        active: true,
        updatedAt: serverTimestamp()
      };

      await setDoc(doc(services.db, "promo_showcases", "active_campaign"), payload, { merge: true });
      setCampaignMessage("✓ Dynamic Campaign published! All devices opening 'dynamic_campaign' route will immediately see this updated page.");
    } catch (err: unknown) {
      setCampaignMessage(err instanceof Error ? err.message : "Failed to publish campaign.");
    } finally {
      setSavingCampaign(false);
    }
  }

  // Real-time listener for Dynamic Features Config
  useEffect(() => {
    if (!services) return;

    const unsubscribe = onSnapshot(
      doc(services.db, "app_config", "dynamic_features"),
      (snapshot) => {
        if (!snapshot.exists()) {
          setConfig(DEFAULTS);
          setLastUpdated("");
          setDirty(false);
          return;
        }

        const data = snapshot.data();
        const launcherIcon = String(data.activeLauncherIcon ?? DEFAULTS.activeLauncherIcon).toLowerCase();
        const normalizedLauncherIcon: DynamicConfig["activeLauncherIcon"] =
          launcherIcon === "independence"
            ? "independence"
            : launcherIcon === "birthday"
              ? "birthday"
              : "default";

        setConfig({
          isDirectCallEnabled: Boolean(data.isDirectCallEnabled ?? DEFAULTS.isDirectCallEnabled),
          isUrgentJobsEnabled: Boolean(data.isUrgentJobsEnabled ?? DEFAULTS.isUrgentJobsEnabled),
          activeLauncherIcon: normalizedLauncherIcon,
          promoBannerUrl: String(data.promoBannerUrl ?? DEFAULTS.promoBannerUrl),
          employerPromoBannerUrl: String(data.employerPromoBannerUrl ?? DEFAULTS.employerPromoBannerUrl),
          lottieLoadingUrl: String(data.lottieLoadingUrl ?? DEFAULTS.lottieLoadingUrl),
          primaryColor: String(data.primaryColor ?? DEFAULTS.primaryColor),
          headerTextColor: String(data.headerTextColor ?? DEFAULTS.headerTextColor),
          employerPrimaryColor: String(data.employerPrimaryColor ?? DEFAULTS.employerPrimaryColor),
          headerLottieUrl: String(data.headerLottieUrl ?? DEFAULTS.headerLottieUrl),
          launchPromoEnabled: Boolean(data.launchPromoEnabled ?? DEFAULTS.launchPromoEnabled),
          launchPromoMediaType: ((data.launchPromoMediaType ?? DEFAULTS.launchPromoMediaType) as DynamicConfig["launchPromoMediaType"]),
          launchPromoBannerUrl: String(data.launchPromoBannerUrl ?? DEFAULTS.launchPromoBannerUrl),
          launchPromoAnimationUrl: String(data.launchPromoAnimationUrl ?? DEFAULTS.launchPromoAnimationUrl),
          launchPromoTitle: String(data.launchPromoTitle ?? DEFAULTS.launchPromoTitle),
          launchPromoActionRoute: String(data.launchPromoActionRoute ?? DEFAULTS.launchPromoActionRoute),
          launchPromoDurationSec: Math.max(3, Math.min(10, Number(data.launchPromoDurationSec ?? DEFAULTS.launchPromoDurationSec))),
          launchPromoButtonText: String(data.launchPromoButtonText ?? DEFAULTS.launchPromoButtonText),
          inAppDialogEnabled: Boolean(data.inAppDialogEnabled ?? DEFAULTS.inAppDialogEnabled),
          inAppDialogImageUrl: String(data.inAppDialogImageUrl ?? DEFAULTS.inAppDialogImageUrl),
          inAppDialogTitle: String(data.inAppDialogTitle ?? DEFAULTS.inAppDialogTitle),
          inAppDialogActionRoute: String(data.inAppDialogActionRoute ?? DEFAULTS.inAppDialogActionRoute),
          inAppDialogTargetAudience: ((data.inAppDialogTargetAudience ?? DEFAULTS.inAppDialogTargetAudience) as DynamicConfig["inAppDialogTargetAudience"])
        });

        const updatedAt = (data.updatedAt as Timestamp | undefined)?.toDate?.();
        setLastUpdated(updatedAt ? `${updatedAt.toLocaleString()} by ${data.updatedBy ?? "admin"}` : "");
        setDirty(false);
      },
      (snapshotError) => {
        setError(snapshotError.message);
      }
    );

    return () => unsubscribe();
  }, [services]);

  async function handleSave() {
    if (!services) {
      setError("Firebase is not configured.");
      return;
    }

    setSaving(true);
    setError(null);
    setMessage(null);

    try {
      await setDoc(
        doc(services.db, "app_config", "dynamic_features"),
        {
          isDirectCallEnabled: config.isDirectCallEnabled,
          isUrgentJobsEnabled: config.isUrgentJobsEnabled,
          activeLauncherIcon: config.activeLauncherIcon,
          promoBannerUrl: config.promoBannerUrl.trim(),
          employerPromoBannerUrl: config.employerPromoBannerUrl.trim(),
          lottieLoadingUrl: config.lottieLoadingUrl.trim(),
          primaryColor: config.primaryColor.trim(),
          headerTextColor: config.headerTextColor.trim(),
          employerPrimaryColor: config.employerPrimaryColor.trim(),
          headerLottieUrl: config.headerLottieUrl.trim(),
          launchPromoEnabled: config.launchPromoEnabled,
          launchPromoMediaType: config.launchPromoMediaType,
          launchPromoBannerUrl: config.launchPromoBannerUrl.trim(),
          launchPromoAnimationUrl: config.launchPromoAnimationUrl.trim(),
          launchPromoTitle: config.launchPromoTitle.trim(),
          launchPromoActionRoute: config.launchPromoActionRoute.trim(),
          launchPromoDurationSec: Math.max(3, Math.min(10, Number(config.launchPromoDurationSec) || 5)),
          launchPromoButtonText: config.launchPromoButtonText.trim(),
          inAppDialogEnabled: config.inAppDialogEnabled,
          inAppDialogImageUrl: config.inAppDialogImageUrl.trim(),
          inAppDialogTitle: config.inAppDialogTitle.trim(),
          inAppDialogActionRoute: config.inAppDialogActionRoute.trim(),
          inAppDialogTargetAudience: config.inAppDialogTargetAudience,
          updatedAt: serverTimestamp(),
          updatedBy: services.auth.currentUser?.email ?? services.auth.currentUser?.uid ?? "admin"
        },
        { merge: true }
      );

      // Seamlessly sync launch promo to app_config/launch so any legacy readers update
      await setDoc(
        doc(services.db, "app_config", "launch"),
        {
          version: Date.now(),
          enabled: config.launchPromoEnabled,
          appIcon: {
            enabled: config.activeLauncherIcon !== "default",
            activeIconId: config.activeLauncherIcon
          },
          promos: config.launchPromoEnabled && (config.launchPromoBannerUrl || config.launchPromoAnimationUrl) ? [
            {
              id: "launch_promo_campaign",
              type: "fullscreen",
              priority: 100,
              imageUrl: config.launchPromoBannerUrl.trim(),
              title: { en: config.launchPromoTitle.trim() || "DutyPe Special Offer" },
              ctaLabel: { en: config.launchPromoButtonText.trim() || "Check Now" },
              deepLink: config.launchPromoActionRoute.trim() || "services",
              dismissible: true,
              frequency: { maxShows: 5, minHoursBetween: 4 }
            }
          ] : []
        },
        { merge: true }
      );

      setMessage("✓ Configuration saved! Pushed to all user devices in real-time.");
      setDirty(false);
    } catch (saveError: unknown) {
      setError(saveError instanceof Error ? saveError.message : "Failed to save dynamic config.");
    } finally {
      setSaving(false);
    }
  }

  async function handleFileUpload(file: File, field: keyof DynamicConfig) {
    if (!services) {
      setError("Firebase is not configured.");
      return;
    }

    setSaving(true);
    setError(null);
    setMessage(null);

    try {
      let uploadBlob: Blob = file;
      let fileName = `${field}_${Date.now()}`;
      let contentType = file.type || "application/octet-stream";

      if (file.type.startsWith("image/") || /\.(png|jpe?g|webp)$/i.test(file.name)) {
        const isLaunchPromo = field === "launchPromoBannerUrl";
        const opt = await compressToTarget4GKb(file, 60, isLaunchPromo);
        uploadBlob = opt.blob;
        fileName = `${fileName}.webp`;
        contentType = "image/webp";

        if (isLaunchPromo) {
          setLaunchPromoInfo({ width: opt.width, height: opt.height, kb: opt.sizeKb });
          if (opt.isNonVerticalWarning) {
            setLaunchPromoWarning(`⚠️ Non-Vertical Image Detected (${opt.width}×${opt.height}px, ratio ${(opt.width / opt.height).toFixed(2)}). Launch promo banners take over the full mobile screen (Blinkit/Zepto style). For best display, use a 9:16 vertical portrait format (1080×1920 px recommended) so the banner covers the screen without letterboxing.`);
          } else {
            setLaunchPromoWarning(null);
          }
        }

        setMessage(`⚡ Ultra-Fast 4G WebP: ${opt.sizeKb} KB (saved ${opt.savingsPct}% data). Loads in <120ms. Uploading...`);
      } else if (file.name.endsWith(".json") || file.type.includes("json")) {
        const text = await file.text();
        let parsed: any;
        try {
          parsed = JSON.parse(text);
        } catch {
          throw new Error("Invalid JSON: The file is not a valid JSON document.");
        }
        if (!parsed.v && !parsed.layers) {
          throw new Error("Invalid Lottie schema: Missing required 'v' or 'layers' fields in JSON.");
        }
        const minified = JSON.stringify(parsed);
        uploadBlob = new Blob([minified], { type: "application/json" });
        fileName = `${fileName}.json`;
        contentType = "application/json";
        const minifiedKb = Math.round((uploadBlob.size / 1024) * 10) / 10;
        const savings = Math.max(0, Math.round(((file.size - uploadBlob.size) / file.size) * 100));
        setMessage(`⚡ Minified Lottie Animation JSON: ${minifiedKb} KB (saved ${savings}%). Uploading...`);
      } else {
        const fileExt = file.name.split('.').pop();
        fileName = `${fileName}.${fileExt}`;
      }

      const storageRef = ref(services.storage, `dynamic_assets/${fileName}`);
      await uploadBytes(storageRef, uploadBlob, {
        contentType,
        cacheControl: "public, max-age=31536000, immutable"
      });
      const url = await getDownloadURL(storageRef);

      setConfig((prev) => ({ ...prev, [field]: url }));
      setDirty(true);
      setMessage(`✓ Successfully uploaded and optimized asset for ${field}`);
    } catch (uploadError: unknown) {
      setError(uploadError instanceof Error ? uploadError.message : "Failed to upload file.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div style={{ maxWidth: 1040, margin: "0 auto", paddingBottom: 60 }}>
      {/* Header Bar */}
      <div style={{
        background: "#ffffff",
        borderRadius: 16,
        padding: "24px 28px",
        boxShadow: "0 1px 3px rgba(0, 0, 0, 0.05), 0 10px 15px -5px rgba(0, 0, 0, 0.02)",
        marginBottom: 20,
        border: "1px solid #e2e8f0",
        display: "flex",
        flexWrap: "wrap",
        justifyContent: "space-between",
        alignItems: "center",
        gap: 16
      }}>
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <span style={{ fontSize: 24 }}>✨</span>
            <h1 style={{ margin: 0, fontSize: 20, fontWeight: 700, color: "#0f172a", letterSpacing: "-0.01em" }}>
              Dynamic App Features &amp; Campaigns
            </h1>
            <span style={{
              fontSize: 11,
              fontWeight: 700,
              padding: "3px 9px",
              borderRadius: 20,
              backgroundColor: "#f0fdf4",
              color: "#166534",
              border: "1px solid #bbf7d0"
            }}>
              ● LIVE DEVICE SYNC
            </span>
          </div>
          <p style={{ margin: "4px 0 0 0", fontSize: 13, color: "#64748b" }}>
            Control app-open launch promo takeovers, in-app flyer dialogs, dynamic campaign landing microsites, themes &amp; branding in real-time.
          </p>
          {lastUpdated && (
            <p style={{ margin: "6px 0 0 0", fontSize: 11, color: "#94a3b8" }}>
              Last synchronized: {lastUpdated}
            </p>
          )}
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          {dirty && (
            <span style={{ fontSize: 12, fontWeight: 600, color: "#d97706", background: "#fef3c7", padding: "4px 10px", borderRadius: 8 }}>
              Unsaved changes
            </span>
          )}
          <button
            type="button"
            onClick={handleSave}
            disabled={!dirty || saving}
            style={{
              padding: "10px 22px",
              borderRadius: 10,
              backgroundColor: !dirty || saving ? "#cbd5e1" : "#2563eb",
              color: !dirty || saving ? "#64748b" : "#ffffff",
              border: "none",
              cursor: !dirty || saving ? "not-allowed" : "pointer",
              fontWeight: 600,
              fontSize: 14,
              boxShadow: dirty && !saving ? "0 4px 6px -1px rgba(37, 99, 235, 0.3)" : "none",
              transition: "all 0.15s ease"
            }}
          >
            {saving ? "Saving Changes..." : "💾 Save Config"}
          </button>
        </div>
      </div>

      {/* Global Status Notifications */}
      {error && (
        <div style={{
          padding: "12px 16px",
          backgroundColor: "#fef2f2",
          border: "1px solid #fecaca",
          color: "#991b1b",
          borderRadius: 10,
          fontSize: 13,
          marginBottom: 16,
          fontWeight: 500,
          display: "flex",
          alignItems: "center",
          gap: 8
        }}>
          <span>❌</span>
          <span>{error}</span>
        </div>
      )}

      {message && (
        <div style={{
          padding: "12px 16px",
          backgroundColor: "#f0fdf4",
          border: "1px solid #bbf7d0",
          color: "#166534",
          borderRadius: 10,
          fontSize: 13,
          marginBottom: 16,
          fontWeight: 500,
          display: "flex",
          alignItems: "center",
          gap: 8
        }}>
          <span>✓</span>
          <span>{message}</span>
        </div>
      )}

      {/* Modern Clean Tabs Bar */}
      <div style={{
        display: "flex",
        background: "#ffffff",
        borderRadius: 12,
        padding: 4,
        gap: 6,
        marginBottom: 20,
        border: "1px solid #e2e8f0",
        boxShadow: "0 1px 2px rgba(0,0,0,0.04)"
      }}>
        {[
          { id: "promos", label: "🚀 Popups & Promos", count: (config.launchPromoEnabled ? 1 : 0) + (config.inAppDialogEnabled ? 1 : 0) },
          { id: "campaign", label: "🌟 Dynamic Campaign Landing Page", badge: "dynamic_campaign" },
          { id: "theme", label: "🎨 App Branding & Theme", badge: config.activeLauncherIcon },
          { id: "features", label: "⚡ Feature Flags & Banners" }
        ].map((tab) => {
          const isSelected = mainTab === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => setMainTab(tab.id as any)}
              style={{
                flex: 1,
                padding: "10px 14px",
                borderRadius: 8,
                border: "none",
                background: isSelected ? "#0f172a" : "transparent",
                color: isSelected ? "#ffffff" : "#475569",
                fontWeight: isSelected ? 700 : 500,
                fontSize: 13,
                cursor: "pointer",
                transition: "all 0.15s ease",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: 8
              }}
            >
              <span>{tab.label}</span>
              {typeof tab.count === "number" && tab.count > 0 && (
                <span style={{
                  fontSize: 11,
                  fontWeight: 700,
                  padding: "1px 6px",
                  borderRadius: 10,
                  backgroundColor: isSelected ? "#3b82f6" : "#e2e8f0",
                  color: isSelected ? "#ffffff" : "#0f172a"
                }}>
                  {tab.count} active
                </span>
              )}
              {tab.badge && (
                <span style={{
                  fontSize: 10,
                  fontWeight: 600,
                  padding: "1px 5px",
                  borderRadius: 4,
                  backgroundColor: isSelected ? "rgba(255,255,255,0.2)" : "#f1f5f9",
                  color: isSelected ? "#ffffff" : "#64748b"
                }}>
                  {tab.badge}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* ========================================================================= */}
      {/* TAB 1: POPUPS & PROMOS (Launch Promo & In-App Dialog Banner)               */}
      {/* ========================================================================= */}
      {mainTab === "promos" && (
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
          {/* Top Banner Notice */}
          <div style={{
            background: "#eff6ff",
            border: "1px solid #bfdbfe",
            borderRadius: 12,
            padding: "14px 18px",
            display: "flex",
            alignItems: "flex-start",
            gap: 12
          }}>
            <span style={{ fontSize: 20 }}>💡</span>
            <div>
              <div style={{ fontSize: 13, fontWeight: 700, color: "#1e3a8a" }}>
                Completely Independent Banners
              </div>
              <div style={{ fontSize: 12, color: "#3b82f6", marginTop: 2, lineHeight: 1.5 }}>
                You can independently enable the <b>Launch Promo Banner</b> (app open splash takeover), the <b>In-App Targeted Dialog Banner</b> (modal flyer on home screen), both simultaneously, or neither. Whichever boxes you check will display exclusively in the mobile app.
              </div>
            </div>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20 }}>
            {/* CARD 1: LAUNCH PROMO BANNER */}
            <div style={{
              background: "#ffffff",
              borderRadius: 16,
              padding: 24,
              border: config.launchPromoEnabled ? "2px solid #22c55e" : "1px solid #e2e8f0",
              boxShadow: "0 1px 3px rgba(0,0,0,0.05)"
            }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 14 }}>
                <div>
                  <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    <span style={{ fontSize: 20 }}>🚀</span>
                    <h2 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: "#0f172a" }}>
                      1. Launch Promo Banner
                    </h2>
                  </div>
                  <p style={{ margin: "4px 0 0 0", fontSize: 12, color: "#64748b" }}>
                    Blinkit, BigBasket &amp; Zepto-style full screen takeover on app open before the splash.
                  </p>
                </div>
                <span style={{
                  fontSize: 11,
                  fontWeight: 700,
                  padding: "3px 8px",
                  borderRadius: 20,
                  backgroundColor: config.launchPromoEnabled ? "#f0fdf4" : "#f1f5f9",
                  color: config.launchPromoEnabled ? "#15803d" : "#64748b",
                  border: config.launchPromoEnabled ? "1px solid #bbf7d0" : "1px solid #e2e8f0"
                }}>
                  {config.launchPromoEnabled ? "🟢 LIVE ON OPEN" : "⚪ DISABLED"}
                </span>
              </div>

              {/* Toggle Switch */}
              <label style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                cursor: "pointer",
                padding: "12px 14px",
                borderRadius: 10,
                backgroundColor: config.launchPromoEnabled ? "#f0fdf4" : "#f8fafc",
                border: "1px solid " + (config.launchPromoEnabled ? "#86efac" : "#e2e8f0"),
                marginBottom: 16
              }}>
                <input
                  type="checkbox"
                  checked={config.launchPromoEnabled}
                  onChange={(e) => {
                    setConfig({ ...config, launchPromoEnabled: e.target.checked });
                    setDirty(true);
                  }}
                  style={{ width: 18, height: 18, accentColor: "#22c55e", cursor: "pointer" }}
                />
                <div>
                  <span style={{ fontSize: 13, fontWeight: 700, color: config.launchPromoEnabled ? "#15803d" : "#334155" }}>
                    Enable Launch Promo Banner
                  </span>
                  <div style={{ fontSize: 11, color: "#64748b" }}>
                    {config.launchPromoEnabled ? "Active: Takes over screen on open" : "Leave unchecked to show standard splash"}
                  </div>
                </div>
              </label>

              {/* Media Type Selector */}
              <div style={{ marginBottom: 14 }}>
                <span style={{ fontSize: 12, fontWeight: 600, color: "#475569", display: "block", marginBottom: 6 }}>
                  Media Format
                </span>
                <div style={{ display: "flex", gap: 8 }}>
                  {(["IMAGE", "ANIMATION"] as const).map((type) => {
                    const isSelected = config.launchPromoMediaType === type;
                    return (
                      <button
                        key={type}
                        type="button"
                        onClick={() => { setConfig({ ...config, launchPromoMediaType: type }); setDirty(true); }}
                        style={{
                          flex: 1,
                          padding: "8px 12px",
                          borderRadius: 8,
                          border: isSelected ? "1.5px solid #2563eb" : "1px solid #cbd5e1",
                          background: isSelected ? "#eff6ff" : "#ffffff",
                          color: isSelected ? "#1d4ed8" : "#475569",
                          fontSize: 12,
                          fontWeight: 600,
                          cursor: "pointer"
                        }}
                      >
                        {type === "IMAGE" ? "🖼️ Image / GIF" : "🎬 Lottie JSON"}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Image Upload & Dimension Guidance */}
              {config.launchPromoMediaType === "IMAGE" ? (
                <div style={{ marginBottom: 14 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: 6 }}>
                    <span style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>
                      Launch Promo Vertical Image (WebP)
                    </span>
                    <span style={{ fontSize: 11, color: "#64748b", fontWeight: 500 }}>
                      Format: 9:16 Vertical Portrait (1080×1920)
                    </span>
                  </div>

                  <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
                    <input
                      type="url"
                      value={config.launchPromoBannerUrl}
                      placeholder="https://cdn.dutype.app/banners/splash-sale.webp"
                      onChange={(e) => { setConfig({ ...config, launchPromoBannerUrl: e.target.value }); setDirty(true); }}
                      style={{ flex: 1, padding: "8px 10px", borderRadius: 8, border: "1px solid #cbd5e1", fontSize: 13 }}
                    />
                    <label style={{
                      padding: "8px 12px",
                      background: "#f1f5f9",
                      border: "1px solid #cbd5e1",
                      borderRadius: 8,
                      fontSize: 12,
                      fontWeight: 600,
                      cursor: "pointer",
                      display: "flex",
                      alignItems: "center"
                    }}>
                      Upload
                      <input
                        type="file"
                        accept="image/*"
                        style={{ display: "none" }}
                        onChange={(e) => { if (e.target.files?.[0]) handleFileUpload(e.target.files[0], "launchPromoBannerUrl"); }}
                      />
                    </label>
                  </div>

                  {/* Non-Vertical Dimension Warning Alert */}
                  {launchPromoWarning && (
                    <div style={{
                      padding: "10px 12px",
                      backgroundColor: "#fffbeb",
                      border: "1px solid #fde68a",
                      color: "#92400e",
                      borderRadius: 8,
                      fontSize: 12,
                      marginBottom: 10,
                      lineHeight: 1.4
                    }}>
                      {launchPromoWarning}
                    </div>
                  )}

                  {config.launchPromoBannerUrl && (
                    <div style={{
                      display: "flex",
                      alignItems: "flex-start",
                      gap: 12,
                      background: "#f8fafc",
                      padding: 10,
                      borderRadius: 10,
                      border: "1px solid #e2e8f0"
                    }}>
                      <img
                        src={config.launchPromoBannerUrl}
                        alt="Launch promo"
                        onLoad={(e) => {
                          const img = e.currentTarget;
                          const aspect = img.naturalWidth / img.naturalHeight;
                          setLaunchPromoInfo({ width: img.naturalWidth, height: img.naturalHeight, kb: 45 });
                          if (img.naturalWidth >= img.naturalHeight || aspect > 0.70) {
                            setLaunchPromoWarning(`⚠️ Non-Vertical Image (${img.naturalWidth}×${img.naturalHeight}px). For edge-to-edge coverage on all phones, please use a 9:16 vertical image (1080×1920 px).`);
                          } else {
                            setLaunchPromoWarning(null);
                          }
                        }}
                        style={{
                          width: 80,
                          height: 140,
                          borderRadius: 6,
                          objectFit: "contain",
                          backgroundColor: "#000000",
                          border: "1px solid #cbd5e1"
                        }}
                      />
                      <div style={{ display: "flex", flexDirection: "column", gap: 6, flex: 1 }}>
                        <span style={{ fontSize: 11, fontWeight: 700, color: "#166534", background: "#f0fdf4", padding: "2px 6px", borderRadius: 4, width: "fit-content" }}>
                          ✓ High-Speed 4G WebP
                        </span>
                        {launchPromoInfo && (
                          <span style={{ fontSize: 11, color: "#64748b" }}>
                            Dimensions: {launchPromoInfo.width} × {launchPromoInfo.height} px
                          </span>
                        )}
                        <span style={{ fontSize: 11, color: "#64748b" }}>
                          Renders 100% full image without cutoffs
                        </span>
                        <button
                          type="button"
                          onClick={() => { setConfig({ ...config, launchPromoBannerUrl: "" }); setDirty(true); setLaunchPromoWarning(null); }}
                          style={{
                            padding: "3px 8px",
                            fontSize: 11,
                            color: "#ef4444",
                            border: "1px solid #fca5a5",
                            borderRadius: 6,
                            background: "transparent",
                            cursor: "pointer",
                            width: "fit-content"
                          }}
                        >
                          Remove Image
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div style={{ marginBottom: 14 }}>
                  <span style={{ fontSize: 12, fontWeight: 600, color: "#475569", display: "block", marginBottom: 6 }}>
                    Launch Animation JSON URL (Lottie)
                  </span>
                  <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
                    <input
                      type="url"
                      value={config.launchPromoAnimationUrl}
                      placeholder="https://assets.dutype.app/animations/launch.json"
                      onChange={(e) => { setConfig({ ...config, launchPromoAnimationUrl: e.target.value }); setDirty(true); }}
                      style={{ flex: 1, padding: "8px 10px", borderRadius: 8, border: "1px solid #cbd5e1", fontSize: 13 }}
                    />
                    <label style={{
                      padding: "8px 12px",
                      background: "#f1f5f9",
                      border: "1px solid #cbd5e1",
                      borderRadius: 8,
                      fontSize: 12,
                      fontWeight: 600,
                      cursor: "pointer"
                    }}>
                      Upload
                      <input
                        type="file"
                        accept="application/json"
                        style={{ display: "none" }}
                        onChange={(e) => { if (e.target.files?.[0]) handleFileUpload(e.target.files[0], "launchPromoAnimationUrl"); }}
                      />
                    </label>
                  </div>
                  {config.launchPromoAnimationUrl && (
                    <div style={{ width: 140, height: 140, border: "1px solid #cbd5e1", borderRadius: 8, overflow: "hidden", background: "#000" }}>
                      <LottiePreview url={config.launchPromoAnimationUrl} />
                    </div>
                  )}
                </div>
              )}

              {/* Controls: Duration, CTA Button & Target Route */}
              <div style={{ display: "flex", flexDirection: "column", gap: 12, borderTop: "1px solid #f1f5f9", paddingTop: 14 }}>
                <label style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                  <span style={{ fontSize: 12, fontWeight: 600, color: "#334155" }}>⏱️ Countdown Duration (3 - 10s)</span>
                  <select
                    value={config.launchPromoDurationSec}
                    onChange={(e) => { setConfig({ ...config, launchPromoDurationSec: Number(e.target.value) || 5 }); setDirty(true); }}
                    style={{ padding: "8px 10px", borderRadius: 8, border: "1px solid #cbd5e1", fontSize: 13, background: "#fff" }}
                  >
                    <option value={3}>3 Seconds (Fast)</option>
                    <option value={4}>4 Seconds</option>
                    <option value={5}>5 Seconds (Recommended)</option>
                    <option value={6}>6 Seconds</option>
                    <option value={7}>7 Seconds</option>
                    <option value={8}>8 Seconds</option>
                    <option value={10}>10 Seconds (Maximum)</option>
                  </select>
                </label>

                <label style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                  <span style={{ fontSize: 12, fontWeight: 600, color: "#334155" }}>🔘 Optional Floating CTA Button</span>
                  <input
                    type="text"
                    value={config.launchPromoButtonText}
                    placeholder="e.g. Order Now / Join Waitlist (Leave empty for no button)"
                    onChange={(e) => { setConfig({ ...config, launchPromoButtonText: e.target.value }); setDirty(true); }}
                    style={{ padding: "8px 10px", borderRadius: 8, border: "1px solid #cbd5e1", fontSize: 13 }}
                  />
                </label>

                <label style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                  <span style={{ fontSize: 12, fontWeight: 600, color: "#334155" }}>🎯 Target Route on Tap (Optional)</span>
                  <select
                    value={config.launchPromoActionRoute}
                    onChange={(e) => { setConfig({ ...config, launchPromoActionRoute: e.target.value }); setDirty(true); }}
                    style={{ padding: "8px 10px", borderRadius: 8, border: "1px solid #cbd5e1", fontSize: 13, background: "#fff" }}
                  >
                    <option value="">None (Splash Only / View Only)</option>
                    <option value="dynamic_campaign">🌟 Dynamic Campaign Landing Page (Loads full rich campaign microsite)</option>
                    <option value="hungr">🥞 Hungr Pre-Order Survey</option>
                    <option value="services">🛠️ Home Services Catalog</option>
                    <option value="jobs">💼 Browse Jobs (Worker)</option>
                    <option value="post_job">📝 Post a Job (Employer)</option>
                    <option value="urgent_need">⚡ Urgent Worker Need</option>
                    <option value="refer_earn">🎁 Refer &amp; Earn Bonus</option>
                    <option value="partner">🤝 Join as Partner</option>
                  </select>
                </label>
              </div>
            </div>

            {/* CARD 2: IN-APP TARGETED DIALOG BANNER */}
            <div style={{
              background: "#ffffff",
              borderRadius: 16,
              padding: 24,
              border: config.inAppDialogEnabled ? "2px solid #3b82f6" : "1px solid #e2e8f0",
              boxShadow: "0 1px 3px rgba(0,0,0,0.05)"
            }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 14 }}>
                <div>
                  <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    <span style={{ fontSize: 20 }}>📢</span>
                    <h2 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: "#0f172a" }}>
                      2. In-App Targeted Dialog Banner
                    </h2>
                  </div>
                  <p style={{ margin: "4px 0 0 0", fontSize: 12, color: "#64748b" }}>
                    Uncropped flyer, ad or alert modal that appears on home screen for targeted user roles.
                  </p>
                </div>
                <span style={{
                  fontSize: 11,
                  fontWeight: 700,
                  padding: "3px 8px",
                  borderRadius: 20,
                  backgroundColor: config.inAppDialogEnabled ? "#eff6ff" : "#f1f5f9",
                  color: config.inAppDialogEnabled ? "#1d4ed8" : "#64748b",
                  border: config.inAppDialogEnabled ? "1px solid #bfdbfe" : "1px solid #e2e8f0"
                }}>
                  {config.inAppDialogEnabled ? "🔵 ACTIVE MODAL" : "⚪ DISABLED"}
                </span>
              </div>

              {/* Toggle Switch */}
              <label style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                cursor: "pointer",
                padding: "12px 14px",
                borderRadius: 10,
                backgroundColor: config.inAppDialogEnabled ? "#eff6ff" : "#f8fafc",
                border: "1px solid " + (config.inAppDialogEnabled ? "#93c5fd" : "#e2e8f0"),
                marginBottom: 16
              }}>
                <input
                  type="checkbox"
                  checked={config.inAppDialogEnabled}
                  onChange={(e) => {
                    setConfig({ ...config, inAppDialogEnabled: e.target.checked });
                    setDirty(true);
                  }}
                  style={{ width: 18, height: 18, accentColor: "#2563eb", cursor: "pointer" }}
                />
                <div>
                  <span style={{ fontSize: 13, fontWeight: 700, color: config.inAppDialogEnabled ? "#1d4ed8" : "#334155" }}>
                    Enable In-App Targeted Dialog Banner
                  </span>
                  <div style={{ fontSize: 11, color: "#64748b" }}>
                    {config.inAppDialogEnabled ? "Active: Shows modal card on home screen" : "Disabled"}
                  </div>
                </div>
              </label>

              {/* Role Selection */}
              <div style={{ marginBottom: 14 }}>
                <span style={{ fontSize: 12, fontWeight: 600, color: "#475569", display: "block", marginBottom: 6 }}>
                  Target Audience (Role Filter)
                </span>
                <select
                  value={config.inAppDialogTargetAudience}
                  onChange={(e) => {
                    setConfig({ ...config, inAppDialogTargetAudience: e.target.value as any });
                    setDirty(true);
                  }}
                  style={{ width: "100%", padding: "8px 10px", borderRadius: 8, border: "1px solid #cbd5e1", fontSize: 13, background: "#fff" }}
                >
                  <option value="ALL">🌐 All Roles (Worker, Employer &amp; Services Homes)</option>
                  <option value="WORKER">👷 Workers Only (Worker Home Screen)</option>
                  <option value="EMPLOYER">🏢 Employers Only (Employer Home Screen)</option>
                  <option value="SERVICES">🛠️ Services Customers Only (Home Services Screen)</option>
                </select>
              </div>

              {/* Flyer Image URL with Uncropped Fit */}
              <div style={{ marginBottom: 14 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: 6 }}>
                  <span style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>
                    Media Asset (Image, Lottie Animation .json, YouTube, or Instagram)
                  </span>
                  <span style={{ fontSize: 11, color: "#059669", fontWeight: 600 }}>
                    WebP, Lottie, YouTube
                  </span>
                </div>

                <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
                  <input
                    type="url"
                    value={config.inAppDialogImageUrl}
                    placeholder="Image URL, Lottie (.json), YouTube, or Instagram link"
                    onChange={(e) => { setConfig({ ...config, inAppDialogImageUrl: e.target.value }); setDirty(true); }}
                    style={{ flex: 1, padding: "8px 10px", borderRadius: 8, border: "1px solid #cbd5e1", fontSize: 13 }}
                  />
                  <label style={{
                    padding: "8px 12px",
                    background: "#f1f5f9",
                    border: "1px solid #cbd5e1",
                    borderRadius: 8,
                    fontSize: 12,
                    fontWeight: 600,
                    cursor: "pointer"
                  }}>
                    Upload
                    <input
                      type="file"
                      accept="image/*"
                      style={{ display: "none" }}
                      onChange={(e) => { if (e.target.files?.[0]) handleFileUpload(e.target.files[0], "inAppDialogImageUrl"); }}
                    />
                  </label>
                </div>

                {config.inAppDialogImageUrl && (
                  <div style={{
                    display: "flex",
                    alignItems: "flex-start",
                    gap: 12,
                    background: "#f8fafc",
                    padding: 10,
                    borderRadius: 10,
                    border: "1px solid #e2e8f0"
                  }}>
                    <img
                      src={config.inAppDialogImageUrl}
                      alt="Dialog promo preview"
                      style={{
                        width: 100,
                        height: 120,
                        borderRadius: 8,
                        objectFit: "contain",
                        backgroundColor: "#ffffff",
                        border: "1px solid #cbd5e1"
                      }}
                    />
                    <div style={{ display: "flex", flexDirection: "column", gap: 4, flex: 1 }}>
                      <span style={{ fontSize: 12, fontWeight: 700, color: "#0f172a" }}>
                        Clean Image Fit
                      </span>
                      <span style={{ fontSize: 11, color: "#64748b" }}>
                        Preserves 100% of your flyer image with rounded corners. No bottom text clutter or extra buttons.
                      </span>
                      <button
                        type="button"
                        onClick={() => { setConfig({ ...config, inAppDialogImageUrl: "" }); setDirty(true); }}
                        style={{
                          padding: "3px 8px",
                          fontSize: 11,
                          color: "#ef4444",
                          border: "1px solid #fca5a5",
                          borderRadius: 6,
                          background: "transparent",
                          cursor: "pointer",
                          width: "fit-content",
                          marginTop: 4
                        }}
                      >
                        Remove Image
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* Target Route on Flyer Click */}
              <div style={{ display: "flex", flexDirection: "column", gap: 12, borderTop: "1px solid #f1f5f9", paddingTop: 14 }}>
                <label style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                  <span style={{ fontSize: 12, fontWeight: 600, color: "#334155" }}>🎯 Target Route on Flyer Tap (Optional)</span>
                  <select
                    value={config.inAppDialogActionRoute}
                    onChange={(e) => { setConfig({ ...config, inAppDialogActionRoute: e.target.value }); setDirty(true); }}
                    style={{ padding: "8px 10px", borderRadius: 8, border: "1px solid #cbd5e1", fontSize: 13, background: "#fff" }}
                  >
                    <option value="">None (View-Only / Informational Flyer with no redirect)</option>
                    <option value="dynamic_campaign">🌟 Dynamic Campaign Landing Page (Loads full rich campaign microsite)</option>
                    <option value="hungr">🥞 Hungr Pre-Order Survey</option>
                    <option value="services">🛠️ Home Services Catalog</option>
                    <option value="jobs">💼 Browse Jobs (Worker)</option>
                    <option value="post_job">📝 Post a Job (Employer)</option>
                    <option value="urgent_need">⚡ Urgent Worker Need</option>
                    <option value="refer_earn">🎁 Refer &amp; Earn Bonus</option>
                    <option value="partner">🤝 Join as Service Partner</option>
                  </select>
                  <span style={{ fontSize: 11, color: "#64748b" }}>
                    Tapping the flyer redirects users to this screen. Leave empty for view-only.
                  </span>
                </label>

                <label style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                  <span style={{ fontSize: 12, fontWeight: 600, color: "#334155" }}>Internal Campaign Note / Reference</span>
                  <input
                    type="text"
                    value={config.inAppDialogTitle}
                    placeholder="e.g. Festival Ad Campaign / Partner Launch"
                    onChange={(e) => { setConfig({ ...config, inAppDialogTitle: e.target.value }); setDirty(true); }}
                    style={{ padding: "8px 10px", borderRadius: 8, border: "1px solid #cbd5e1", fontSize: 13 }}
                  />
                  <span style={{ fontSize: 11, color: "#64748b" }}>
                    Internal admin reference only. Not displayed as bottom text.
                  </span>
                </label>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 2: DYNAMIC CAMPAIGN LANDING PAGE BUILDER                              */}
      {/* ========================================================================= */}
      {mainTab === "campaign" && (
        <div style={{
          background: "#ffffff",
          borderRadius: 16,
          padding: 24,
          border: "2px solid #6366f1",
          boxShadow: "0 1px 3px rgba(0,0,0,0.05)"
        }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: "1px solid #e2e8f0", paddingBottom: 16, marginBottom: 20 }}>
            <div>
              <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                <span style={{ fontSize: 22 }}>🌟</span>
                <h2 style={{ margin: 0, fontSize: 17, fontWeight: 700, color: "#1e1b4b" }}>
                  Dynamic Campaign &amp; Ad Landing Microsite
                </h2>
                <span style={{ fontSize: 11, background: "#e0e7ff", color: "#3730a3", fontWeight: 700, padding: "2px 8px", borderRadius: 12 }}>
                  SINGLE ROUTE: dynamic_campaign
                </span>
              </div>
              <p style={{ margin: "4px 0 0 0", fontSize: 12, color: "#475569" }}>
                When users tap any banner, dialog, or announcement configured with route <code>dynamic_campaign</code>, the app renders this full rich showcase page.
              </p>
            </div>
            <button
              type="button"
              onClick={() => void handleSaveCampaign()}
              disabled={savingCampaign}
              style={{
                padding: "9px 18px",
                borderRadius: 10,
                background: "#4f46e5",
                color: "#ffffff",
                fontWeight: 600,
                fontSize: 13,
                border: "none",
                cursor: "pointer",
                boxShadow: "0 2px 4px rgba(79, 70, 229, 0.2)"
              }}
            >
              {savingCampaign ? "Publishing..." : "💾 Save & Publish Campaign"}
            </button>
          </div>

          {campaignMessage && (
            <div style={{ background: "#ecfdf5", border: "1px solid #10b981", borderRadius: 8, padding: 12, fontSize: 13, color: "#065f46", marginBottom: 16, fontWeight: 500 }}>
              {campaignMessage}
            </div>
          )}

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginBottom: 16 }}>
            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#334155", fontWeight: 600 }}>Campaign Headline / Title *</span>
              <input
                type="text"
                value={campaign.title}
                placeholder="e.g. Hungr - Morning Tiffins at Shop Prices"
                onChange={(e) => setCampaign({ ...campaign, title: e.target.value })}
                style={{ padding: 10, borderRadius: 8, border: "1px solid #cbd5e1", outline: "none", fontSize: 14 }}
              />
            </label>

            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#334155", fontWeight: 600 }}>Category Tag / Badge</span>
              <input
                type="text"
                value={campaign.categoryBadge}
                placeholder="e.g. EXCLUSIVE LAUNCH, PARTNER AD, SPECIAL WISH"
                onChange={(e) => setCampaign({ ...campaign, categoryBadge: e.target.value })}
                style={{ padding: 10, borderRadius: 8, border: "1px solid #cbd5e1", outline: "none", fontSize: 14 }}
              />
            </label>
          </div>

          <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13, marginBottom: 16 }}>
            <span style={{ color: "#334155", fontWeight: 600 }}>Tagline / Catchphrase</span>
            <input
              type="text"
              value={campaign.tagline}
              placeholder="e.g. Fresh hot breakfast delivered to your apartment security gate at exact offline shop prices."
              onChange={(e) => setCampaign({ ...campaign, tagline: e.target.value })}
              style={{ padding: 10, borderRadius: 8, border: "1px solid #cbd5e1", outline: "none", fontSize: 14 }}
            />
          </label>

          {/* Hero Media Type & URL */}
          <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: 10, padding: 16, marginBottom: 16 }}>
            <div style={{ display: "flex", gap: 16, alignItems: "center", marginBottom: 12 }}>
              <span style={{ fontSize: 13, fontWeight: 600, color: "#334155" }}>Hero Media Type:</span>
              <label style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 13, cursor: "pointer" }}>
                <input
                  type="radio"
                  name="campaignMediaType"
                  checked={campaign.mediaType === "IMAGE"}
                  onChange={() => setCampaign({ ...campaign, mediaType: "IMAGE" })}
                />
                Static Banner Image (Compressed 30-60KB WebP)
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 13, cursor: "pointer" }}>
                <input
                  type="radio"
                  name="campaignMediaType"
                  checked={campaign.mediaType === "LOTTIE"}
                  onChange={() => setCampaign({ ...campaign, mediaType: "LOTTIE" })}
                />
                Lottie Animation (Full-Motion Vector)
              </label>
            </div>

            {campaign.mediaType === "IMAGE" ? (
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                <div style={{ display: "flex", gap: 10 }}>
                  <input
                    type="url"
                    value={campaign.heroImageUrl}
                    placeholder="e.g. https://cdn.dutype.app/banners/hungr-cover.webp"
                    onChange={(e) => setCampaign({ ...campaign, heroImageUrl: e.target.value })}
                    style={{ flex: 1, padding: 8, borderRadius: 8, border: "1px solid #cbd5e1", outline: "none", fontSize: 13 }}
                  />
                  <label style={{ padding: "8px 12px", background: "#ffffff", border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 12, fontWeight: 600, cursor: "pointer" }}>
                    Upload
                    <input
                      type="file"
                      accept="image/*"
                      style={{ display: "none" }}
                      onChange={async (e) => {
                        if (e.target.files?.[0] && services) {
                          try {
                            const file = e.target.files[0];
                            const compressed = await compressToTarget4GKb(file);
                            const fileRef = ref(services.storage, `showcase_media/${Date.now()}_${file.name.replace(/\.[^/.]+$/, "")}.webp`);
                            await uploadBytes(fileRef, compressed.blob, { contentType: "image/webp" });
                            const url = await getDownloadURL(fileRef);
                            setCampaign({ ...campaign, heroImageUrl: url });
                          } catch (uploadErr) {
                            alert("Upload failed: " + uploadErr);
                          }
                        }
                      }}
                    />
                  </label>
                </div>
                {campaign.heroImageUrl && (
                  <img src={campaign.heroImageUrl} alt="Hero Preview" style={{ width: 300, height: 140, objectFit: "cover", borderRadius: 8, border: "1px solid #cbd5e1" }} />
                )}
              </div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                <input
                  type="url"
                  value={campaign.videoUrl}
                  placeholder="e.g. https://assets5.lottiefiles.com/packages/lf20_m6cuL6.json"
                  onChange={(e) => setCampaign({ ...campaign, videoUrl: e.target.value })}
                  style={{ padding: 8, borderRadius: 8, border: "1px solid #cbd5e1", outline: "none", fontSize: 13 }}
                />
                {campaign.videoUrl && (
                  <div style={{ maxWidth: 220, border: "1px solid #cbd5e1", borderRadius: 8, overflow: "hidden", background: "#000" }}>
                    <LottiePreview url={campaign.videoUrl} />
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Highlights / Features list */}
          <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: 10, padding: 16, marginBottom: 16 }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: "#334155", display: "block", marginBottom: 8 }}>
              Key Highlight Bullets / Offer Cards
            </span>
            <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 10 }}>
              {campaign.highlights.map((hl, idx) => (
                <div key={idx} style={{ display: "flex", gap: 8, alignItems: "center" }}>
                  <span style={{ fontSize: 13, color: "#64748b" }}>•</span>
                  <input
                    type="text"
                    value={hl}
                    onChange={(e) => {
                      const next = [...campaign.highlights];
                      next[idx] = e.target.value;
                      setCampaign({ ...campaign, highlights: next });
                    }}
                    style={{ flex: 1, padding: "7px 10px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                  />
                  <button
                    type="button"
                    onClick={() => {
                      const next = campaign.highlights.filter((_, i) => i !== idx);
                      setCampaign({ ...campaign, highlights: next });
                    }}
                    style={{ color: "#ef4444", background: "none", border: "none", cursor: "pointer", fontSize: 16 }}
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              <input
                type="text"
                value={newHighlight}
                placeholder="e.g. 🎁 Free morning delivery to security gate"
                onChange={(e) => setNewHighlight(e.target.value)}
                style={{ flex: 1, padding: "7px 10px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
              />
              <button
                type="button"
                onClick={() => {
                  if (newHighlight.trim()) {
                    setCampaign({ ...campaign, highlights: [...campaign.highlights, newHighlight.trim()] });
                    setNewHighlight("");
                  }
                }}
                style={{ padding: "7px 14px", background: "#f1f5f9", border: "1px solid #cbd5e1", borderRadius: 6, fontSize: 12, fontWeight: 600, cursor: "pointer" }}
              >
                + Add Bullet
              </button>
            </div>
          </div>

          {/* Full Story / Description */}
          <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13, marginBottom: 16 }}>
            <span style={{ color: "#334155", fontWeight: 600 }}>Detailed Story / About Announcement</span>
            <textarea
              rows={4}
              value={campaign.aboutDescription}
              placeholder="Write the full story, company intro, birthday wishes, or special event description..."
              onChange={(e) => setCampaign({ ...campaign, aboutDescription: e.target.value })}
              style={{ padding: 10, borderRadius: 8, border: "1px solid #cbd5e1", outline: "none", fontSize: 13, resize: "vertical" }}
            />
          </label>

          {/* Special Voucher Offer Card */}
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14, marginBottom: 16 }}>
            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#334155", fontWeight: 600 }}>Voucher Offer Title</span>
              <input
                type="text"
                value={campaign.discountOfferTitle}
                placeholder="e.g. Exclusive VIP Voucher (₹50 OFF)"
                onChange={(e) => setCampaign({ ...campaign, discountOfferTitle: e.target.value })}
                style={{ padding: 8, borderRadius: 6, border: "1px solid #cbd5e1", outline: "none", fontSize: 13 }}
              />
            </label>

            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#334155", fontWeight: 600 }}>Voucher Coupon Code (1-Tap Copy)</span>
              <input
                type="text"
                value={campaign.discountOfferCode}
                placeholder="e.g. HUNGR50"
                onChange={(e) => setCampaign({ ...campaign, discountOfferCode: e.target.value.toUpperCase() })}
                style={{ padding: 8, borderRadius: 6, border: "1px solid #cbd5e1", outline: "none", fontSize: 13, fontFamily: "monospace" }}
              />
            </label>
          </div>

          {/* Action Buttons */}
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14 }}>
            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#334155", fontWeight: 600 }}>Primary Action Button</span>
              <div style={{ display: "flex", gap: 6 }}>
                <input
                  type="text"
                  value={campaign.primaryButtonLabel}
                  placeholder="Label (e.g. Join VIP Waitlist)"
                  onChange={(e) => setCampaign({ ...campaign, primaryButtonLabel: e.target.value })}
                  style={{ flex: 1, padding: 6, borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                />
                <input
                  type="text"
                  value={campaign.primaryButtonAction}
                  placeholder="Route / Link (hungr or https://...)"
                  onChange={(e) => setCampaign({ ...campaign, primaryButtonAction: e.target.value })}
                  style={{ flex: 1, padding: 6, borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                />
              </div>
            </label>

            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#334155", fontWeight: 600 }}>Secondary Action Button</span>
              <div style={{ display: "flex", gap: 6 }}>
                <input
                  type="text"
                  value={campaign.secondaryButtonLabel}
                  placeholder="Label (e.g. WhatsApp Us)"
                  onChange={(e) => setCampaign({ ...campaign, secondaryButtonLabel: e.target.value })}
                  style={{ flex: 1, padding: 6, borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                />
                <input
                  type="text"
                  value={campaign.secondaryButtonAction}
                  placeholder="WhatsApp or Call (https://wa.me/...)"
                  onChange={(e) => setCampaign({ ...campaign, secondaryButtonAction: e.target.value })}
                  style={{ flex: 1, padding: 6, borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                />
              </div>
            </label>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 3: APP BRANDING & THEME                                               */}
      {/* ========================================================================= */}
      {mainTab === "theme" && (
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
          {/* Launcher Icon Section */}
          <div style={{
            background: "#ffffff",
            borderRadius: 16,
            padding: 24,
            border: "1px solid #e2e8f0",
            boxShadow: "0 1px 3px rgba(0,0,0,0.05)"
          }}>
            <h2 style={{ margin: "0 0 6px 0", fontSize: 16, fontWeight: 700, color: "#0f172a" }}>
              App Launcher Icon
            </h2>
            <p style={{ margin: "0 0 16px 0", fontSize: 12, color: "#64748b" }}>
              Changes the app icon on the user&apos;s home screen remotely.
            </p>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(130px, 1fr))", gap: 14 }}>
              {[
                { id: "default", name: "Default (Purple)", color: "linear-gradient(135deg, #7c3aed, #4f46e5)", icon: "D" },
                { id: "birthday", name: "Birthday Special", color: "linear-gradient(135deg, #f59e0b, #d97706)", icon: "🎂" },
                { id: "independence", name: "Independence", color: "linear-gradient(135deg, #f97316, #16a34a)", icon: "🇮🇳" }
              ].map((opt) => {
                const isSelected = config.activeLauncherIcon === opt.id;
                return (
                  <button
                    key={opt.id}
                    type="button"
                    onClick={() => { setConfig({ ...config, activeLauncherIcon: opt.id as any }); setDirty(true); }}
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      alignItems: "center",
                      padding: 16,
                      borderRadius: 12,
                      border: isSelected ? "2.5px solid #4f46e5" : "1px solid #e2e8f0",
                      backgroundColor: isSelected ? "#f5f3ff" : "#f8fafc",
                      cursor: "pointer",
                      transition: "all 0.15s ease",
                      outline: "none"
                    }}
                  >
                    <div style={{
                      width: 52,
                      height: 52,
                      borderRadius: 14,
                      background: opt.color,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      color: "#fff",
                      fontWeight: "bold",
                      fontSize: 22,
                      marginBottom: 10,
                      boxShadow: "0 4px 6px -1px rgba(0,0,0,0.1)"
                    }}>
                      {opt.icon}
                    </div>
                    <span style={{ fontSize: 12, fontWeight: isSelected ? 700 : 500, color: isSelected ? "#4f46e5" : "#475569" }}>
                      {opt.name}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Role Theme Colors & Lottie Assets */}
          <div style={{
            background: "#ffffff",
            borderRadius: 16,
            padding: 24,
            border: "1px solid #e2e8f0",
            boxShadow: "0 1px 3px rgba(0,0,0,0.05)"
          }}>
            <div style={{ display: "flex", borderBottom: "1px solid #e2e8f0", gap: 12, marginBottom: 18 }}>
              <button
                type="button"
                onClick={() => setThemeRoleTab("worker")}
                style={{
                  padding: "8px 16px",
                  fontWeight: 600,
                  fontSize: 13,
                  border: "none",
                  background: "none",
                  borderBottom: themeRoleTab === "worker" ? "2px solid #2563eb" : "2px solid transparent",
                  color: themeRoleTab === "worker" ? "#2563eb" : "#64748b",
                  cursor: "pointer"
                }}
              >
                👷 Worker App Theme
              </button>
              <button
                type="button"
                onClick={() => setThemeRoleTab("employer")}
                style={{
                  padding: "8px 16px",
                  fontWeight: 600,
                  fontSize: 13,
                  border: "none",
                  background: "none",
                  borderBottom: themeRoleTab === "employer" ? "2px solid #2563eb" : "2px solid transparent",
                  color: themeRoleTab === "employer" ? "#2563eb" : "#64748b",
                  cursor: "pointer"
                }}
              >
                🏢 Employer App Theme
              </button>
            </div>

            {themeRoleTab === "worker" ? (
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
                <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
                  <span style={{ color: "#334155", fontWeight: 600 }}>Worker Primary Color</span>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <input
                      type="color"
                      value={config.primaryColor}
                      onChange={(e) => { setConfig({ ...config, primaryColor: e.target.value }); setDirty(true); }}
                      style={{ width: 44, height: 44, padding: 0, border: "1px solid #cbd5e1", borderRadius: 8, cursor: "pointer", background: "none" }}
                    />
                    <input
                      type="text"
                      value={config.primaryColor}
                      onChange={(e) => { setConfig({ ...config, primaryColor: e.target.value }); setDirty(true); }}
                      style={{ flex: 1, padding: 8, borderRadius: 6, border: "1px solid #cbd5e1", fontFamily: "monospace", fontSize: 13 }}
                    />
                  </div>
                </label>

                <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
                  <span style={{ color: "#334155", fontWeight: 600 }}>Worker Header Text Color</span>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <input
                      type="color"
                      value={config.headerTextColor}
                      onChange={(e) => { setConfig({ ...config, headerTextColor: e.target.value }); setDirty(true); }}
                      style={{ width: 44, height: 44, padding: 0, border: "1px solid #cbd5e1", borderRadius: 8, cursor: "pointer", background: "none" }}
                    />
                    <input
                      type="text"
                      value={config.headerTextColor}
                      onChange={(e) => { setConfig({ ...config, headerTextColor: e.target.value }); setDirty(true); }}
                      style={{ flex: 1, padding: 8, borderRadius: 6, border: "1px solid #cbd5e1", fontFamily: "monospace", fontSize: 13 }}
                    />
                  </div>
                </label>
              </div>
            ) : (
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
                <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
                  <span style={{ color: "#334155", fontWeight: 600 }}>Employer Primary Color</span>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <input
                      type="color"
                      value={config.employerPrimaryColor}
                      onChange={(e) => { setConfig({ ...config, employerPrimaryColor: e.target.value }); setDirty(true); }}
                      style={{ width: 44, height: 44, padding: 0, border: "1px solid #cbd5e1", borderRadius: 8, cursor: "pointer", background: "none" }}
                    />
                    <input
                      type="text"
                      value={config.employerPrimaryColor}
                      onChange={(e) => { setConfig({ ...config, employerPrimaryColor: e.target.value }); setDirty(true); }}
                      style={{ flex: 1, padding: 8, borderRadius: 6, border: "1px solid #cbd5e1", fontFamily: "monospace", fontSize: 13 }}
                    />
                  </div>
                </label>

                <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
                  <span style={{ color: "#334155", fontWeight: 600 }}>Employer Header Lottie URL</span>
                  <input
                    type="url"
                    value={config.headerLottieUrl}
                    placeholder="https://assets.dutype.app/animations/header.json"
                    onChange={(e) => { setConfig({ ...config, headerLottieUrl: e.target.value }); setDirty(true); }}
                    style={{ padding: 8, borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                  />
                </label>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 4: FEATURE FLAGS & BANNERS                                            */}
      {/* ========================================================================= */}
      {mainTab === "features" && (
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
          {/* Feature Flags */}
          <div style={{
            background: "#ffffff",
            borderRadius: 16,
            padding: 24,
            border: "1px solid #e2e8f0",
            boxShadow: "0 1px 3px rgba(0,0,0,0.05)"
          }}>
            <h2 style={{ margin: "0 0 16px 0", fontSize: 16, fontWeight: 700, color: "#0f172a" }}>
              Mobile App Feature Switches
            </h2>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14 }}>
              <label style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                cursor: "pointer",
                padding: "12px 14px",
                borderRadius: 10,
                border: "1px solid #e2e8f0",
                background: config.isDirectCallEnabled ? "#f0fdf4" : "#f8fafc"
              }}>
                <input
                  type="checkbox"
                  checked={config.isDirectCallEnabled}
                  onChange={(e) => { setConfig({ ...config, isDirectCallEnabled: e.target.checked }); setDirty(true); }}
                  style={{ width: 18, height: 18, accentColor: "#22c55e" }}
                />
                <div>
                  <span style={{ fontSize: 13, fontWeight: 600, color: "#0f172a" }}>
                    Enable Direct Calls
                  </span>
                  <div style={{ fontSize: 11, color: "#64748b" }}>
                    Allows workers &amp; employers to call directly
                  </div>
                </div>
              </label>

              <label style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                cursor: "pointer",
                padding: "12px 14px",
                borderRadius: 10,
                border: "1px solid #e2e8f0",
                background: config.isUrgentJobsEnabled ? "#f0fdf4" : "#f8fafc"
              }}>
                <input
                  type="checkbox"
                  checked={config.isUrgentJobsEnabled}
                  onChange={(e) => { setConfig({ ...config, isUrgentJobsEnabled: e.target.checked }); setDirty(true); }}
                  style={{ width: 18, height: 18, accentColor: "#22c55e" }}
                />
                <div>
                  <span style={{ fontSize: 13, fontWeight: 600, color: "#0f172a" }}>
                    Enable Urgent/Instant Jobs
                  </span>
                  <div style={{ fontSize: 11, color: "#64748b" }}>
                    Enables the urgent jobs toggle and broadcast
                  </div>
                </div>
              </label>
            </div>
          </div>

          {/* In-App Hero Banners */}
          <div style={{
            background: "#ffffff",
            borderRadius: 16,
            padding: 24,
            border: "1px solid #e2e8f0",
            boxShadow: "0 1px 3px rgba(0,0,0,0.05)"
          }}>
            <h2 style={{ margin: "0 0 16px 0", fontSize: 16, fontWeight: 700, color: "#0f172a" }}>
              In-App Home Hero Banners
            </h2>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
              {/* Worker Banner */}
              <div>
                <span style={{ fontSize: 13, fontWeight: 600, color: "#334155", display: "block", marginBottom: 6 }}>
                  Worker Home Banner URL
                </span>
                <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
                  <input
                    type="url"
                    value={config.promoBannerUrl}
                    placeholder="https://cdn.dutype.app/banners/worker_promo.webp"
                    onChange={(e) => { setConfig({ ...config, promoBannerUrl: e.target.value }); setDirty(true); }}
                    style={{ flex: 1, padding: "8px 10px", borderRadius: 8, border: "1px solid #cbd5e1", fontSize: 13 }}
                  />
                  <label style={{ padding: "8px 12px", background: "#f1f5f9", border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 12, fontWeight: 600, cursor: "pointer" }}>
                    Upload
                    <input
                      type="file"
                      accept="image/*"
                      style={{ display: "none" }}
                      onChange={(e) => { if (e.target.files?.[0]) handleFileUpload(e.target.files[0], "promoBannerUrl"); }}
                    />
                  </label>
                </div>
                {config.promoBannerUrl && (
                  <img src={config.promoBannerUrl} alt="Worker Banner" style={{ width: "100%", height: 90, objectFit: "cover", borderRadius: 8, border: "1px solid #cbd5e1" }} />
                )}
              </div>

              {/* Employer Banner */}
              <div>
                <span style={{ fontSize: 13, fontWeight: 600, color: "#334155", display: "block", marginBottom: 6 }}>
                  Employer Home Banner URL
                </span>
                <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
                  <input
                    type="url"
                    value={config.employerPromoBannerUrl}
                    placeholder="https://cdn.dutype.app/banners/employer_promo.webp"
                    onChange={(e) => { setConfig({ ...config, employerPromoBannerUrl: e.target.value }); setDirty(true); }}
                    style={{ flex: 1, padding: "8px 10px", borderRadius: 8, border: "1px solid #cbd5e1", fontSize: 13 }}
                  />
                  <label style={{ padding: "8px 12px", background: "#f1f5f9", border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 12, fontWeight: 600, cursor: "pointer" }}>
                    Upload
                    <input
                      type="file"
                      accept="image/*"
                      style={{ display: "none" }}
                      onChange={(e) => { if (e.target.files?.[0]) handleFileUpload(e.target.files[0], "employerPromoBannerUrl"); }}
                    />
                  </label>
                </div>
                {config.employerPromoBannerUrl && (
                  <img src={config.employerPromoBannerUrl} alt="Employer Banner" style={{ width: "100%", height: 90, objectFit: "cover", borderRadius: 8, border: "1px solid #cbd5e1" }} />
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Sticky Bottom Save Action Bar */}
      <div style={{
        position: "sticky",
        bottom: 20,
        zIndex: 40,
        background: "rgba(15, 23, 42, 0.94)",
        backdropFilter: "blur(8px)",
        borderRadius: 14,
        padding: "14px 20px",
        marginTop: 24,
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        boxShadow: "0 10px 25px -5px rgba(0, 0, 0, 0.2)"
      }}>
        <div style={{ color: "#ffffff", fontSize: 13 }}>
          {dirty ? (
            <span style={{ color: "#facc15", fontWeight: 600 }}>
              ● You have unsaved changes in dynamic configuration
            </span>
          ) : (
            <span style={{ color: "#94a3b8" }}>
              ✓ All dynamic features synchronized with mobile apps
            </span>
          )}
        </div>

        <button
          type="button"
          onClick={handleSave}
          disabled={!dirty || saving}
          style={{
            padding: "8px 20px",
            borderRadius: 8,
            backgroundColor: !dirty || saving ? "#334155" : "#3b82f6",
            color: !dirty || saving ? "#64748b" : "#ffffff",
            border: "none",
            cursor: !dirty || saving ? "not-allowed" : "pointer",
            fontWeight: 700,
            fontSize: 13,
            transition: "all 0.15s ease"
          }}
        >
          {saving ? "Saving Changes..." : "Save Config Now"}
        </button>
      </div>
    </div>
  );
}
