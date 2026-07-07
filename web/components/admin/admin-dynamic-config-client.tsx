"use client";

import { useEffect, useMemo, useState } from "react";
import { doc, onSnapshot, serverTimestamp, setDoc, Timestamp } from "firebase/firestore";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";

import { getFirebaseServices } from "@/lib/firebase/client";
import { LottiePreview } from "./lottie-preview";

type DynamicConfig = {
  isDirectCallEnabled: boolean;
  isUrgentJobsEnabled: boolean;
  activeLauncherIcon: "default" | "diwali" | "independence";
  promoBannerUrl: string;
  employerPromoBannerUrl: string;
  lottieLoadingUrl: string;
  primaryColor: string; // Worker Primary Color
  employerPrimaryColor: string; // Employer Primary Color
  headerLottieUrl: string;
  launchPromoEnabled: boolean;
  launchPromoMediaType: "IMAGE" | "ANIMATION";
  launchPromoBannerUrl: string;
  launchPromoAnimationUrl: string;
  launchPromoBackgroundColor: string;
  launchPromoStatusBarColor: string;
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
  employerPrimaryColor: "#0F0F0F",
  headerLottieUrl: "",
  launchPromoEnabled: false,
  launchPromoMediaType: "IMAGE",
  launchPromoBannerUrl: "",
  launchPromoAnimationUrl: "",
  launchPromoBackgroundColor: "#FFFFFF",
  launchPromoStatusBarColor: "#FFFFFF"
};

export function AdminDynamicConfigClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [config, setConfig] = useState<DynamicConfig>(DEFAULTS);
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<string>("");
  const [activeTab, setActiveTab] = useState<"worker" | "employer">("worker");

  useEffect(() => {
    if (!services) return;

    const ref = doc(services.db, "app_config", "dynamic_features");
    const unsubscribe = onSnapshot(
      ref,
      (snapshot) => {
        if (!snapshot.exists()) {
          setConfig(DEFAULTS);
          setLastUpdated("(never - defaults in use)");
          setDirty(false);
          return;
        }

        const data = snapshot.data() as Partial<DynamicConfig>;
        const launcherIcon = String(data.activeLauncherIcon ?? DEFAULTS.activeLauncherIcon).toLowerCase();
        const normalizedLauncherIcon: DynamicConfig["activeLauncherIcon"] =
          launcherIcon === "independence"
            ? "independence"
            : launcherIcon === "diwali" || launcherIcon === "christmas" || launcherIcon === "ramadan"
              ? "diwali"
              : "default";

        setConfig({
          isDirectCallEnabled: Boolean(data.isDirectCallEnabled ?? DEFAULTS.isDirectCallEnabled),
          isUrgentJobsEnabled: Boolean(data.isUrgentJobsEnabled ?? DEFAULTS.isUrgentJobsEnabled),
          activeLauncherIcon: normalizedLauncherIcon,
          promoBannerUrl: String(data.promoBannerUrl ?? DEFAULTS.promoBannerUrl),
          employerPromoBannerUrl: String(data.employerPromoBannerUrl ?? DEFAULTS.employerPromoBannerUrl),
          lottieLoadingUrl: String(data.lottieLoadingUrl ?? DEFAULTS.lottieLoadingUrl),
          primaryColor: String(data.primaryColor ?? DEFAULTS.primaryColor),
          employerPrimaryColor: String(data.employerPrimaryColor ?? DEFAULTS.employerPrimaryColor),
          headerLottieUrl: String(data.headerLottieUrl ?? DEFAULTS.headerLottieUrl),
          launchPromoEnabled: Boolean(data.launchPromoEnabled ?? DEFAULTS.launchPromoEnabled),
          launchPromoMediaType: ((data.launchPromoMediaType ?? DEFAULTS.launchPromoMediaType) as DynamicConfig["launchPromoMediaType"]),
          launchPromoBannerUrl: String(data.launchPromoBannerUrl ?? DEFAULTS.launchPromoBannerUrl),
          launchPromoAnimationUrl: String(data.launchPromoAnimationUrl ?? DEFAULTS.launchPromoAnimationUrl),
          launchPromoBackgroundColor: String(data.launchPromoBackgroundColor ?? DEFAULTS.launchPromoBackgroundColor),
          launchPromoStatusBarColor: String(data.launchPromoStatusBarColor ?? DEFAULTS.launchPromoStatusBarColor)
        });

        const updatedAt = (data.updatedAt as Timestamp | undefined)?.toDate?.();
        setLastUpdated(updatedAt ? `${updatedAt.toLocaleString()} by ${data.updatedBy ?? "unknown"}` : "");
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
            employerPrimaryColor: config.employerPrimaryColor.trim(),
            headerLottieUrl: config.headerLottieUrl.trim(),
            launchPromoEnabled: config.launchPromoEnabled,
            launchPromoMediaType: config.launchPromoMediaType,
            launchPromoBannerUrl: config.launchPromoBannerUrl.trim(),
            launchPromoAnimationUrl: config.launchPromoAnimationUrl.trim(),
            launchPromoBackgroundColor: config.launchPromoBackgroundColor.trim(),
            launchPromoStatusBarColor: config.launchPromoStatusBarColor.trim(),
            updatedAt: serverTimestamp(),
            updatedBy: services.auth.currentUser?.email ?? services.auth.currentUser?.uid ?? "admin"
          },
          { merge: true }
      );
      setMessage("Saved. The changes will instantly push to all active user devices in real-time.");
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
      const fileExt = file.name.split('.').pop();
      const fileName = `${field}_${Date.now()}.${fileExt}`;
      const storageRef = ref(services.storage, `dynamic_assets/${fileName}`);

      await uploadBytes(storageRef, file);
      const url = await getDownloadURL(storageRef);

      setConfig((prev) => ({ ...prev, [field]: url }));
      setDirty(true);
      setMessage(`Successfully uploaded to ${field}`);
    } catch (uploadError: unknown) {
      setError(uploadError instanceof Error ? uploadError.message : "Failed to upload file.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="admin-card" style={{ maxWidth: 720, padding: 24, backgroundColor: "#fff", borderRadius: 8, boxShadow: "0 1px 3px 0 rgba(0, 0, 0, 0.1)" }}>
      <header style={{ marginBottom: 24 }}>
        <h2 style={{ margin: 0, fontSize: 18, fontWeight: 600 }}>Dynamic App Features &amp; Theme Configurator</h2>
        <p style={{ color: "#6b7280", fontSize: 13, marginTop: 4 }}>
          Tweak feature flags, update launcher icons, and manage loading animations remotely.
        </p>
        {lastUpdated && (
          <p style={{ color: "#9ca3af", fontSize: 12, marginTop: 4 }}>
            Last updated: {lastUpdated}
          </p>
        )}
      </header>

      {error && (
        <div style={{ padding: 12, backgroundColor: "#fee2e2", color: "#b91c1c", borderRadius: 6, fontSize: 13, marginBottom: 16 }}>
          {error}
        </div>
      )}

      {message && (
        <div style={{ padding: 12, backgroundColor: "#d1fae5", color: "#065f46", borderRadius: 6, fontSize: 13, marginBottom: 16 }}>
          {message}
        </div>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
        
        {/* Launcher Icon Selection at top (common to both roles) */}
        <div style={{ borderBottom: "1px solid #e5e7eb", paddingBottom: 24 }}>
          <h3 style={{ margin: "0 0 12px 0", fontSize: 14, fontWeight: 600, color: "#374151" }}>Common App Launcher Icon</h3>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(110px, 1fr))", gap: 12 }}>
            {[
              { id: "default", name: "Default (Purple)", color: "linear-gradient(135deg, #7c3aed, #4f46e5)", icon: "D" },
              { id: "diwali", name: "Diwali (Gold)", color: "linear-gradient(135deg, #f59e0b, #d97706)", icon: "🪔" },
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
                    padding: 12,
                    borderRadius: 12,
                    border: isSelected ? "2.5px solid #4f46e5" : "1px solid #d1d5db",
                    backgroundColor: isSelected ? "#f5f3ff" : "#f9fafb",
                    cursor: "pointer",
                    transition: "all 0.15s ease",
                    outline: "none"
                  }}
                >
                  <div style={{
                    width: 48,
                    height: 48,
                    borderRadius: 12,
                    background: opt.color,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "#fff",
                    fontWeight: "bold",
                    fontSize: 20,
                    marginBottom: 8,
                    boxShadow: "0 2px 4px rgba(0,0,0,0.1)"
                  }}>
                    {opt.icon}
                  </div>
                  <span style={{ fontSize: 11, fontWeight: isSelected ? 600 : 500, color: isSelected ? "#4f46e5" : "#4b5563" }}>
                    {opt.name}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        <div style={{ borderBottom: "1px solid #e5e7eb", paddingBottom: 24 }}>
          <h3 style={{ margin: "0 0 12px 0", fontSize: 14, fontWeight: 600, color: "#374151" }}>Launch Promo Banner</h3>
          <p style={{ margin: "0 0 12px 0", fontSize: 12, color: "#6b7280" }}>
            Show a full-screen event or deals banner before the splash screen on app open. Leave it off to keep the normal splash.
          </p>

          <label style={{ display: "flex", alignItems: "center", gap: 10, cursor: "pointer", marginBottom: 16 }}>
            <input
              type="checkbox"
              checked={config.launchPromoEnabled}
              onChange={(e) => { setConfig({ ...config, launchPromoEnabled: e.target.checked }); setDirty(true); }}
            />
            <span style={{ fontSize: 14, fontWeight: 500, color: "#374151" }}>Enable launch promo banner</span>
          </label>

          <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
            {(["IMAGE", "ANIMATION"] as const).map((type) => {
              const isSelected = config.launchPromoMediaType === type;
              return (
                <button
                  key={type}
                  type="button"
                  onClick={() => { setConfig({ ...config, launchPromoMediaType: type }); setDirty(true); }}
                  style={{
                    padding: "8px 12px",
                    borderRadius: 999,
                    border: isSelected ? "1px solid #4f46e5" : "1px solid #d1d5db",
                    background: isSelected ? "#eef2ff" : "#fff",
                    color: isSelected ? "#4338ca" : "#374151",
                    fontSize: 12,
                    fontWeight: 600,
                    cursor: "pointer"
                  }}
                >
                  {type === "IMAGE" ? "Image / GIF" : "Lottie animation"}
                </button>
              );
            })}
          </div>

          <p style={{ margin: "0 0 16px 0", fontSize: 12, color: "#6b7280" }}>
            Recommended: use WebP or PNG for static promos, and Lottie JSON for animations. GIF is acceptable for simple loops but is heavier and less battery-friendly.
          </p>

          {config.launchPromoMediaType === "IMAGE" ? (
            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13, marginBottom: 16 }}>
              <span style={{ color: "#374151", fontWeight: 500 }}>Launch Promo Image URL</span>
              <div style={{ display: "flex", gap: 10 }}>
                <input
                  type="url"
                  value={config.launchPromoBannerUrl}
                  placeholder="e.g. https://cdn.dutype.app/festivals/big-sale-banner.webp"
                  onChange={(e) => { setConfig({ ...config, launchPromoBannerUrl: e.target.value }); setDirty(true); }}
                  style={{ flex: 1, padding: 8, borderRadius: 6, border: "1px solid #d1d5db", outline: "none", fontSize: 14 }}
                />
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => { if (e.target.files?.[0]) handleFileUpload(e.target.files[0], "launchPromoBannerUrl"); }}
                />
              </div>
              {config.launchPromoBannerUrl && (
                <div style={{ marginTop: 8, display: "flex", alignItems: "flex-start", gap: 10 }}>
                  <img src={config.launchPromoBannerUrl} alt="Launch promo preview" style={{ maxWidth: 220, borderRadius: 6, objectFit: "cover" }} />
                  <button type="button" onClick={() => { setConfig({ ...config, launchPromoBannerUrl: "" }); setDirty(true); }} style={{ padding: "4px 8px", fontSize: 12, color: "#ef4444", border: "1px solid #ef4444", borderRadius: 4, background: "transparent", cursor: "pointer" }}>Delete</button>
                </div>
              )}
            </label>
          ) : (
            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13, marginBottom: 16 }}>
              <span style={{ color: "#374151", fontWeight: 500 }}>Launch Promo Animation JSON URL</span>
              <div style={{ display: "flex", gap: 10 }}>
                <input
                  type="url"
                  value={config.launchPromoAnimationUrl}
                  placeholder="e.g. https://lottie.host/your-animation.json"
                  onChange={(e) => { setConfig({ ...config, launchPromoAnimationUrl: e.target.value }); setDirty(true); }}
                  style={{ flex: 1, padding: 8, borderRadius: 6, border: "1px solid #d1d5db", outline: "none", fontSize: 14 }}
                />
                <input
                  type="file"
                  accept="application/json"
                  onChange={(e) => { if (e.target.files?.[0]) handleFileUpload(e.target.files[0], "launchPromoAnimationUrl"); }}
                />
              </div>
              {config.launchPromoAnimationUrl && (
                <div style={{ marginTop: 8, display: "flex", alignItems: "flex-start", gap: 10 }}>
                  <LottiePreview url={config.launchPromoAnimationUrl} />
                  <button type="button" onClick={() => { setConfig({ ...config, launchPromoAnimationUrl: "" }); setDirty(true); }} style={{ padding: "4px 8px", fontSize: 12, color: "#ef4444", border: "1px solid #ef4444", borderRadius: 4, background: "transparent", cursor: "pointer" }}>Delete</button>
                </div>
              )}
            </label>
          )}

          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: 16 }}>
            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#374151", fontWeight: 500 }}>Launch Promo Background Color</span>
              <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
                <input
                  type="color"
                  value={config.launchPromoBackgroundColor}
                  onChange={(e) => { setConfig({ ...config, launchPromoBackgroundColor: e.target.value }); setDirty(true); }}
                  style={{ width: 44, height: 44, padding: 0, border: "1px solid #d1d5db", borderRadius: 8, cursor: "pointer", backgroundColor: "transparent" }}
                />
                <input
                  type="text"
                  value={config.launchPromoBackgroundColor}
                  placeholder="#FFFFFF"
                  onChange={(e) => { setConfig({ ...config, launchPromoBackgroundColor: e.target.value }); setDirty(true); }}
                  style={{ flex: 1, padding: 8, borderRadius: 6, border: "1px solid #d1d5db", outline: "none", fontSize: 14, fontFamily: "monospace" }}
                />
              </div>
            </label>

            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#374151", fontWeight: 500 }}>Launch Promo Status Bar Color</span>
              <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
                <input
                  type="color"
                  value={config.launchPromoStatusBarColor}
                  onChange={(e) => { setConfig({ ...config, launchPromoStatusBarColor: e.target.value }); setDirty(true); }}
                  style={{ width: 44, height: 44, padding: 0, border: "1px solid #d1d5db", borderRadius: 8, cursor: "pointer", backgroundColor: "transparent" }}
                />
                <input
                  type="text"
                  value={config.launchPromoStatusBarColor}
                  placeholder="#FFFFFF"
                  onChange={(e) => { setConfig({ ...config, launchPromoStatusBarColor: e.target.value }); setDirty(true); }}
                  style={{ flex: 1, padding: 8, borderRadius: 6, border: "1px solid #d1d5db", outline: "none", fontSize: 14, fontFamily: "monospace" }}
                />
              </div>
            </label>
          </div>
        </div>

        {/* Tab Headers */}
        <div style={{ display: "flex", gap: 8, borderBottom: "1px solid #e5e7eb" }}>
          <button
            type="button"
            onClick={() => setActiveTab("worker")}
            style={{
              padding: "8px 16px",
              fontWeight: 600,
              fontSize: 14,
              border: "none",
              background: "none",
              borderBottom: activeTab === "worker" ? "2px solid #4f46e5" : "2px solid transparent",
              color: activeTab === "worker" ? "#4f46e5" : "#6b7280",
              cursor: "pointer"
            }}
          >
            Worker App settings
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("employer")}
            style={{
              padding: "8px 16px",
              fontWeight: 600,
              fontSize: 14,
              border: "none",
              background: "none",
              borderBottom: activeTab === "employer" ? "2px solid #4f46e5" : "2px solid transparent",
              color: activeTab === "employer" ? "#4f46e5" : "#6b7280",
              cursor: "pointer"
            }}
          >
            Employer App settings
          </button>
        </div>

        {/* Tab Content */}
        {activeTab === "worker" ? (
          <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
            {/* Worker Theme Color */}
            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#374151", fontWeight: 600 }}>Worker App Theme Primary Color</span>
              <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
                <input
                  type="color"
                  value={config.primaryColor}
                  onChange={(e) => { setConfig({ ...config, primaryColor: e.target.value }); setDirty(true); }}
                  style={{ width: 44, height: 44, padding: 0, border: "1px solid #d1d5db", borderRadius: 8, cursor: "pointer", backgroundColor: "transparent" }}
                />
                <input
                  type="text"
                  value={config.primaryColor}
                  placeholder="#1E3A8A"
                  onChange={(e) => { setConfig({ ...config, primaryColor: e.target.value }); setDirty(true); }}
                  style={{ flex: 1, padding: 8, borderRadius: 6, border: "1px solid #d1d5db", outline: "none", fontSize: 14, fontFamily: "monospace" }}
                />
              </div>
            </label>

            {/* Toggle Switches */}
            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              <label style={{ display: "flex", alignItems: "center", gap: 10, cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={config.isDirectCallEnabled}
                  onChange={(e) => { setConfig({ ...config, isDirectCallEnabled: e.target.checked }); setDirty(true); }}
                />
                <span style={{ fontSize: 14, fontWeight: 500, color: "#374151" }}>Enable Direct Employer Calls</span>
              </label>
              
              <label style={{ display: "flex", alignItems: "center", gap: 10, cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={config.isUrgentJobsEnabled}
                  onChange={(e) => { setConfig({ ...config, isUrgentJobsEnabled: e.target.checked }); setDirty(true); }}
                />
                <span style={{ fontSize: 14, fontWeight: 500, color: "#374151" }}>Enable Urgent/Instant Jobs switch</span>
              </label>
            </div>

            {/* Promo Banner URL */}
            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#374151", fontWeight: 500 }}>Promotional Banner Image URL</span>
              <div style={{ display: "flex", gap: 10 }}>
                <input
                  type="url"
                  value={config.promoBannerUrl}
                  placeholder="e.g. https://cdn.dutype.app/festivals/diwali_banner.png"
                  onChange={(e) => { setConfig({ ...config, promoBannerUrl: e.target.value }); setDirty(true); }}
                  style={{ flex: 1, padding: 8, borderRadius: 6, border: "1px solid #d1d5db", outline: "none", fontSize: 14 }}
                />
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => { if (e.target.files?.[0]) handleFileUpload(e.target.files[0], "promoBannerUrl"); }}
                />
              </div>
              {config.promoBannerUrl && (
                <div style={{ marginTop: 8, display: "flex", alignItems: "flex-start", gap: 10 }}>
                  <img src={config.promoBannerUrl} alt="Preview" style={{ maxWidth: 200, borderRadius: 4 }} />
                  <button type="button" onClick={() => { setConfig({ ...config, promoBannerUrl: "" }); setDirty(true); }} style={{ padding: "4px 8px", fontSize: 12, color: "#ef4444", border: "1px solid #ef4444", borderRadius: 4, background: "transparent", cursor: "pointer" }}>Delete</button>
                </div>
              )}
            </label>

            {/* Header Lottie URL */}
            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#374151", fontWeight: 500 }}>Lottie Background Animation JSON URL (OTA Header Asset)</span>
              <div style={{ display: "flex", gap: 10 }}>
                <input
                  type="url"
                  value={config.headerLottieUrl}
                  placeholder="e.g. https://lottie.host/82dfcb82-c6b6-455b-b996-5381f2118311/dD1v7vI8Lz.json"
                  onChange={(e) => { setConfig({ ...config, headerLottieUrl: e.target.value }); setDirty(true); }}
                  style={{ flex: 1, padding: 8, borderRadius: 6, border: "1px solid #d1d5db", outline: "none", fontSize: 14 }}
                />
                <input
                  type="file"
                  accept="application/json"
                  onChange={(e) => { if (e.target.files?.[0]) handleFileUpload(e.target.files[0], "headerLottieUrl"); }}
                />
              </div>
              {config.headerLottieUrl && (
                <div style={{ marginTop: 8, display: "flex", alignItems: "flex-start", gap: 10 }}>
                  <LottiePreview url={config.headerLottieUrl} />
                  <button type="button" onClick={() => { setConfig({ ...config, headerLottieUrl: "" }); setDirty(true); }} style={{ padding: "4px 8px", fontSize: 12, color: "#ef4444", border: "1px solid #ef4444", borderRadius: 4, background: "transparent", cursor: "pointer" }}>Delete</button>
                </div>
              )}
            </label>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
            {/* Employer Theme Color */}
            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#374151", fontWeight: 600 }}>Employer App Theme Primary Color</span>
              <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
                <input
                  type="color"
                  value={config.employerPrimaryColor}
                  onChange={(e) => { setConfig({ ...config, employerPrimaryColor: e.target.value }); setDirty(true); }}
                  style={{ width: 44, height: 44, padding: 0, border: "1px solid #d1d5db", borderRadius: 8, cursor: "pointer", backgroundColor: "transparent" }}
                />
                <input
                  type="text"
                  value={config.employerPrimaryColor}
                  placeholder="#0F0F0F"
                  onChange={(e) => { setConfig({ ...config, employerPrimaryColor: e.target.value }); setDirty(true); }}
                  style={{ flex: 1, padding: 8, borderRadius: 6, border: "1px solid #d1d5db", outline: "none", fontSize: 14, fontFamily: "monospace" }}
                />
              </div>
            </label>

            {/* Lottie Loading JSON URL */}
            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#374151", fontWeight: 500 }}>Lottie Loading JSON URL (OTA Loader Asset)</span>
              <div style={{ display: "flex", gap: 10 }}>
                <input
                  type="url"
                  value={config.lottieLoadingUrl}
                  placeholder="e.g. https://assets.dutype.app/animations/sparklers.json"
                  onChange={(e) => { setConfig({ ...config, lottieLoadingUrl: e.target.value }); setDirty(true); }}
                  style={{ flex: 1, padding: 8, borderRadius: 6, border: "1px solid #d1d5db", outline: "none", fontSize: 14 }}
                />
                <input
                  type="file"
                  accept="application/json"
                  onChange={(e) => { if (e.target.files?.[0]) handleFileUpload(e.target.files[0], "lottieLoadingUrl"); }}
                />
              </div>
              {config.lottieLoadingUrl && (
                <div style={{ marginTop: 8, display: "flex", alignItems: "flex-start", gap: 10 }}>
                  <LottiePreview url={config.lottieLoadingUrl} />
                  <button type="button" onClick={() => { setConfig({ ...config, lottieLoadingUrl: "" }); setDirty(true); }} style={{ padding: "4px 8px", fontSize: 12, color: "#ef4444", border: "1px solid #ef4444", borderRadius: 4, background: "transparent", cursor: "pointer" }}>Delete</button>
                </div>
              )}
            </label>

            {/* Employer Promotional Banner URL */}
            <label style={{ display: "flex", flexDirection: "column", gap: 6, fontSize: 13 }}>
              <span style={{ color: "#374151", fontWeight: 500 }}>Employer Promotional Banner Image URL</span>
              <div style={{ display: "flex", gap: 10 }}>
                <input
                  type="url"
                  value={config.employerPromoBannerUrl}
                  placeholder="e.g. https://cdn.dutype.app/festivals/employer_diwali_banner.png"
                  onChange={(e) => { setConfig({ ...config, employerPromoBannerUrl: e.target.value }); setDirty(true); }}
                  style={{ flex: 1, padding: 8, borderRadius: 6, border: "1px solid #d1d5db", outline: "none", fontSize: 14 }}
                />
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => { if (e.target.files?.[0]) handleFileUpload(e.target.files[0], "employerPromoBannerUrl"); }}
                />
              </div>
              {config.employerPromoBannerUrl && (
                <div style={{ marginTop: 8, display: "flex", alignItems: "flex-start", gap: 10 }}>
                  <img src={config.employerPromoBannerUrl} alt="Preview" style={{ maxWidth: 200, borderRadius: 4 }} />
                  <button type="button" onClick={() => { setConfig({ ...config, employerPromoBannerUrl: "" }); setDirty(true); }} style={{ padding: "4px 8px", fontSize: 12, color: "#ef4444", border: "1px solid #ef4444", borderRadius: 4, background: "transparent", cursor: "pointer" }}>Delete</button>
                </div>
              )}
            </label>
          </div>
        )}

      </div>

      <footer style={{ marginTop: 24, paddingTop: 16, borderTop: "1px solid #e5e7eb", display: "flex", justifyContent: "flex-end", gap: 12 }}>
        <button
          onClick={handleSave}
          disabled={!dirty || saving}
          style={{
            padding: "8px 16px",
            borderRadius: 6,
            backgroundColor: !dirty || saving ? "#e5e7eb" : "#4f46e5",
            color: !dirty || saving ? "#9ca3af" : "#fff",
            border: "none",
            cursor: !dirty || saving ? "not-allowed" : "pointer",
            fontWeight: 500,
            fontSize: 14
          }}
        >
          {saving ? "Saving..." : "Save Config"}
        </button>
      </footer>
    </section>
  );
}
