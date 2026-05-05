"use client";

import { useEffect, useMemo, useState } from "react";
import { doc, onSnapshot, serverTimestamp, setDoc, Timestamp } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";

type TargetSelection = "ALL" | "WORKER" | "EMPLOYER";

type AppUpdateConfig = {
  enabled: boolean;
  latestVersionCode: number;
  minSupportedVersionCode: number;
  latestVersionName: string;
  forceUpdate: boolean;
  title: string;
  titleTe: string;
  message: string;
  messageTe: string;
  buttonText: string;
  buttonTextTe: string;
  playStoreUrl: string;
  targetRoles: string[];
  updatedAt?: Timestamp | null;
  updatedBy?: string | null;
};

const DEFAULTS: AppUpdateConfig = {
  enabled: false,
  latestVersionCode: 0,
  minSupportedVersionCode: 0,
  latestVersionName: "",
  forceUpdate: false,
  title: "Update DutyPe",
  titleTe: "",
  message: "A new DutyPe update is available. Update now to get the latest jobs, fixes, and features.",
  messageTe: "",
  buttonText: "Update",
  buttonTextTe: "",
  playStoreUrl: "https://play.google.com/store/apps/details?id=com.dutype.app",
  targetRoles: ["WORKER", "EMPLOYER"]
};

function parseWholeNumber(value: string, fallback: number): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? Math.floor(parsed) : fallback;
}

function normalizeTargetRoles(value: unknown): string[] {
  const roles = Array.isArray(value) ? value.map(String) : [];
  const normalized = roles
    .map((role) => role.trim().toUpperCase())
    .filter((role) => role === "WORKER" || role === "EMPLOYER" || role === "ALL");

  return normalized.length > 0 ? normalized : DEFAULTS.targetRoles;
}

function targetSelectionFor(roles: string[]): TargetSelection {
  const normalized = normalizeTargetRoles(roles);
  if (normalized.includes("ALL") || (normalized.includes("WORKER") && normalized.includes("EMPLOYER"))) {
    return "ALL";
  }
  return normalized.includes("EMPLOYER") ? "EMPLOYER" : "WORKER";
}

function rolesForSelection(selection: TargetSelection): string[] {
  if (selection === "WORKER") return ["WORKER"];
  if (selection === "EMPLOYER") return ["EMPLOYER"];
  return ["WORKER", "EMPLOYER"];
}

export function AdminAppUpdateClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [config, setConfig] = useState<AppUpdateConfig>(DEFAULTS);
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<string>("");

  useEffect(() => {
    if (!services) return;

    const ref = doc(services.db, "app_config", "app_update");
    const unsubscribe = onSnapshot(
      ref,
      (snapshot) => {
        if (!snapshot.exists()) {
          setConfig(DEFAULTS);
          setLastUpdated("(never - defaults in use)");
          setDirty(false);
          return;
        }

        const data = snapshot.data() as Partial<AppUpdateConfig>;
        setConfig({
          enabled: Boolean(data.enabled ?? DEFAULTS.enabled),
          latestVersionCode: Number(data.latestVersionCode ?? DEFAULTS.latestVersionCode),
          minSupportedVersionCode: Number(data.minSupportedVersionCode ?? DEFAULTS.minSupportedVersionCode),
          latestVersionName: String(data.latestVersionName ?? DEFAULTS.latestVersionName),
          forceUpdate: Boolean(data.forceUpdate ?? DEFAULTS.forceUpdate),
          title: String(data.title ?? DEFAULTS.title),
          titleTe: String(data.titleTe ?? DEFAULTS.titleTe),
          message: String(data.message ?? DEFAULTS.message),
          messageTe: String(data.messageTe ?? DEFAULTS.messageTe),
          buttonText: String(data.buttonText ?? DEFAULTS.buttonText),
          buttonTextTe: String(data.buttonTextTe ?? DEFAULTS.buttonTextTe),
          playStoreUrl: String(data.playStoreUrl ?? DEFAULTS.playStoreUrl),
          targetRoles: normalizeTargetRoles(data.targetRoles)
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
        doc(services.db, "app_config", "app_update"),
        {
          enabled: config.enabled,
          latestVersionCode: Math.max(0, Math.floor(config.latestVersionCode)),
          minSupportedVersionCode: Math.max(0, Math.floor(config.minSupportedVersionCode)),
          latestVersionName: config.latestVersionName.trim(),
          forceUpdate: config.forceUpdate,
          title: config.title.trim(),
          titleTe: config.titleTe.trim(),
          message: config.message.trim(),
          messageTe: config.messageTe.trim(),
          buttonText: config.buttonText.trim(),
          buttonTextTe: config.buttonTextTe.trim(),
          playStoreUrl: config.playStoreUrl.trim(),
          targetRoles: normalizeTargetRoles(config.targetRoles),
          updatedAt: serverTimestamp(),
          updatedBy: services.auth.currentUser?.email ?? services.auth.currentUser?.uid ?? "admin"
        },
        { merge: true }
      );
      setMessage("Saved. Worker and employer home screens will pick this up from app_config/app_update.");
      setDirty(false);
    } catch (saveError: unknown) {
      setError(saveError instanceof Error ? saveError.message : "Failed to save app update config.");
    } finally {
      setSaving(false);
    }
  }

  function updateConfig(patch: Partial<AppUpdateConfig>) {
    setConfig((current) => ({ ...current, ...patch }));
    setDirty(true);
  }

  const targetSelection = targetSelectionFor(config.targetRoles);

  return (
    <section className="admin-card" style={{ maxWidth: 820 }}>
      <header style={{ marginBottom: 18 }}>
        <h2 style={{ margin: 0 }}>Android update prompt</h2>
        <p style={{ color: "#6b7280", fontSize: 13, marginTop: 4 }}>
          Controls the lightweight update dialog shown on worker and employer home screens.
        </p>
        {lastUpdated && (
          <p style={{ color: "#9ca3af", fontSize: 12, marginTop: 4 }}>
            Last updated: {lastUpdated}
          </p>
        )}
      </header>

      <div style={{ display: "grid", gap: 16 }}>
        <label style={{ display: "flex", gap: 10, alignItems: "flex-start", fontSize: 14 }}>
          <input
            type="checkbox"
            checked={config.enabled}
            onChange={(event) => updateConfig({ enabled: event.target.checked })}
            style={{ marginTop: 3 }}
          />
          <span>
            <strong>Show update prompt</strong>
            <small style={{ display: "block", color: "#6b7280", marginTop: 2 }}>
              If no version code is set, every targeted user sees the prompt until this is turned off.
            </small>
          </span>
        </label>

        <label style={{ display: "flex", gap: 10, alignItems: "flex-start", fontSize: 14 }}>
          <input
            type="checkbox"
            checked={config.forceUpdate}
            onChange={(event) => updateConfig({ forceUpdate: event.target.checked })}
            style={{ marginTop: 3 }}
          />
          <span>
            <strong>Do not allow cancel</strong>
            <small style={{ display: "block", color: "#6b7280", marginTop: 2 }}>
              Use this only for important updates. Users will only see the Update button.
            </small>
          </span>
        </label>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 14 }}>
          <NumberField
            label="Latest version code"
            value={config.latestVersionCode}
            help="Set this higher than the app version currently installed on phones."
            onChange={(value) => updateConfig({ latestVersionCode: value })}
          />
          <NumberField
            label="Minimum supported version code"
            value={config.minSupportedVersionCode}
            help="Users below this version cannot dismiss, even if the prompt is not forced."
            onChange={(value) => updateConfig({ minSupportedVersionCode: value })}
          />
          <TextField
            label="Latest version name"
            value={config.latestVersionName}
            placeholder="2.6.16"
            onChange={(value) => updateConfig({ latestVersionName: value })}
          />
          <label style={{ display: "flex", flexDirection: "column", gap: 5, fontSize: 13 }}>
            <span style={{ color: "#374151", fontWeight: 600 }}>Target users</span>
            <select
              value={targetSelection}
              onChange={(event) => updateConfig({ targetRoles: rolesForSelection(event.target.value as TargetSelection) })}
              style={{ padding: 8, borderRadius: 6, border: "1px solid #d1d5db", background: "white" }}
            >
              <option value="ALL">Workers and employers</option>
              <option value="WORKER">Workers only</option>
              <option value="EMPLOYER">Employers only</option>
            </select>
          </label>
        </div>

        <TextField
          label="Play Store URL"
          value={config.playStoreUrl}
          placeholder="https://play.google.com/store/apps/details?id=com.dutype.app"
          onChange={(value) => updateConfig({ playStoreUrl: value })}
        />

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: 14 }}>
          <TextField label="Title" value={config.title} onChange={(value) => updateConfig({ title: value })} />
          <TextField label="Telugu title" value={config.titleTe} onChange={(value) => updateConfig({ titleTe: value })} />
          <TextArea label="Message" value={config.message} onChange={(value) => updateConfig({ message: value })} />
          <TextArea label="Telugu message" value={config.messageTe} onChange={(value) => updateConfig({ messageTe: value })} />
          <TextField label="Button text" value={config.buttonText} onChange={(value) => updateConfig({ buttonText: value })} />
          <TextField label="Telugu button text" value={config.buttonTextTe} onChange={(value) => updateConfig({ buttonTextTe: value })} />
        </div>
      </div>

      <div style={{ marginTop: 22, display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
        <button
          type="button"
          disabled={!dirty || saving}
          onClick={handleSave}
          className="pill pill-link"
          style={{
            background: dirty ? "#2563eb" : "#e5e7eb",
            color: dirty ? "white" : "#6b7280",
            cursor: dirty && !saving ? "pointer" : "not-allowed",
            padding: "8px 20px"
          }}
        >
          {saving ? "Saving..." : dirty ? "Save changes" : "Saved"}
        </button>
        {message && <span style={{ color: "#059669", fontSize: 13 }}>{message}</span>}
        {error && <span style={{ color: "#dc2626", fontSize: 13 }}>{error}</span>}
      </div>
    </section>
  );
}

function TextField({
  label,
  value,
  placeholder,
  onChange
}: {
  label: string;
  value: string;
  placeholder?: string;
  onChange: (value: string) => void;
}) {
  return (
    <label style={{ display: "flex", flexDirection: "column", gap: 5, fontSize: 13 }}>
      <span style={{ color: "#374151", fontWeight: 600 }}>{label}</span>
      <input
        type="text"
        value={value}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
        style={{ padding: 8, borderRadius: 6, border: "1px solid #d1d5db" }}
      />
    </label>
  );
}


function NumberField({
  label,
  value,
  help,
  onChange
}: {
  label: string;
  value: number;
  help?: string;
  onChange: (value: number) => void;
}) {
  return (
    <label style={{ display: "flex", flexDirection: "column", gap: 5, fontSize: 13 }}>
      <span style={{ color: "#374151", fontWeight: 600 }}>{label}</span>
      <input
        type="number"
        min={0}
        value={value}
        onChange={(event) => onChange(parseWholeNumber(event.target.value, value))}
        style={{ padding: 8, borderRadius: 6, border: "1px solid #d1d5db" }}
      />
      {help && <small style={{ color: "#6b7280" }}>{help}</small>}
    </label>
  );
}

function TextArea({
  label,
  value,
  onChange
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label style={{ display: "flex", flexDirection: "column", gap: 5, fontSize: 13 }}>
      <span style={{ color: "#374151", fontWeight: 600 }}>{label}</span>
      <textarea
        value={value}
        rows={4}
        onChange={(event) => onChange(event.target.value)}
        style={{ padding: 8, borderRadius: 6, border: "1px solid #d1d5db", resize: "vertical" }}
      />
    </label>
  );
}
