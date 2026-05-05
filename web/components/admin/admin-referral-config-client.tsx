"use client";

import { useEffect, useMemo, useState } from "react";
import { doc, onSnapshot, Timestamp } from "firebase/firestore";
import { httpsCallable, getFunctions } from "firebase/functions";
import { getApp } from "firebase/app";

import { getFirebaseServices } from "@/lib/firebase/client";

type ReferralConfig = {
  rewardPerReferral: number;
  signupBonus: number;
  employerSignupBonus: number;
  employerSignupBonusEnabled: boolean;
  employerUnlimitedJobPostingEnabled: boolean;
  welcomeBonusCampaignId: string;
  minWithdrawal: number;
  maxWithdrawalPerDay: number;
  milestones: Record<string, number>;
  withdrawalMilestones: number[];
  updatedAt?: Timestamp | null;
  updatedBy?: string | null;
};

const DEFAULTS: ReferralConfig = {
  rewardPerReferral: 25,
  signupBonus: 25,
  employerSignupBonus: 10,
  employerSignupBonusEnabled: true,
  employerUnlimitedJobPostingEnabled: true,
  welcomeBonusCampaignId: "welcome_bonus_v1",
  minWithdrawal: 50,
  maxWithdrawalPerDay: 1000,
  milestones: { "5": 50, "10": 100, "15": 150, "25": 250, "50": 500, "100": 1000 },
  withdrawalMilestones: [5, 10, 15],
};

function parseNumber(value: string, fallback: number): number {
  const n = Number(value);
  return Number.isFinite(n) && n >= 0 ? n : fallback;
}

export function AdminReferralConfigClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [config, setConfig] = useState<ReferralConfig>(DEFAULTS);
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<string>("");

  // Realtime listener on /app_config/referral — this is the same doc the
  // Android app listens to, so admins see the exact live values.
  useEffect(() => {
    if (!services) return;
    const ref = doc(services.db, "app_config", "referral");
    const unsub = onSnapshot(ref, (snap) => {
      if (!snap.exists()) {
        setConfig(DEFAULTS);
        setLastUpdated("(never — defaults in use)");
        return;
      }
      const data = snap.data() as Partial<ReferralConfig>;
      setConfig({
        rewardPerReferral: Number(data.rewardPerReferral ?? DEFAULTS.rewardPerReferral),
        signupBonus: Number(data.signupBonus ?? DEFAULTS.signupBonus),
        employerSignupBonus: Number(data.employerSignupBonus ?? DEFAULTS.employerSignupBonus),
        employerSignupBonusEnabled: Boolean(data.employerSignupBonusEnabled ?? DEFAULTS.employerSignupBonusEnabled),
        employerUnlimitedJobPostingEnabled: Boolean(data.employerUnlimitedJobPostingEnabled ?? DEFAULTS.employerUnlimitedJobPostingEnabled),
        welcomeBonusCampaignId: String(data.welcomeBonusCampaignId ?? DEFAULTS.welcomeBonusCampaignId),
        minWithdrawal: Number(data.minWithdrawal ?? DEFAULTS.minWithdrawal),
        maxWithdrawalPerDay: Number(data.maxWithdrawalPerDay ?? DEFAULTS.maxWithdrawalPerDay),
        milestones: { ...DEFAULTS.milestones, ...(data.milestones ?? {}) },
        withdrawalMilestones: Array.isArray(data.withdrawalMilestones)
          ? data.withdrawalMilestones.map((v) => Number(v)).filter(Number.isFinite)
          : DEFAULTS.withdrawalMilestones,
      });
      const ts = (data.updatedAt as Timestamp | undefined)?.toDate?.();
      setLastUpdated(ts ? `${ts.toLocaleString()} by ${data.updatedBy ?? "unknown"}` : "");
      setDirty(false);
    });
    return () => unsub();
  }, [services]);

  async function handleSave() {
    if (!services) {
      setError("Firebase not configured");
      return;
    }
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const functions = getFunctions(getApp(), "asia-south1");
      const update = httpsCallable(functions, "updateReferralConfig");
      await update({
        rewardPerReferral: config.rewardPerReferral,
        signupBonus: config.signupBonus,
        employerSignupBonus: config.employerSignupBonus,
        employerSignupBonusEnabled: config.employerSignupBonusEnabled,
        employerUnlimitedJobPostingEnabled: config.employerUnlimitedJobPostingEnabled,
        welcomeBonusCampaignId: config.welcomeBonusCampaignId.trim() || DEFAULTS.welcomeBonusCampaignId,
        minWithdrawal: config.minWithdrawal,
        maxWithdrawalPerDay: config.maxWithdrawalPerDay,
        milestones: config.milestones,
        withdrawalMilestones: config.withdrawalMilestones,
      });
      setMessage("Saved. Android app users will see the new values within a few seconds.");
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "Failed to save";
      setError(msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="admin-card" style={{ maxWidth: 720 }}>
      <header style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>Referral rewards</h2>
        <p style={{ color: "#6b7280", fontSize: 13, marginTop: 4 }}>
          Changes take effect immediately for the Cloud Functions (60s cache) and
          within ~1 second for the Android Refer &amp; Earn screen (realtime listener).
        </p>
        {lastUpdated && (
          <p style={{ color: "#9ca3af", fontSize: 12, marginTop: 4 }}>
            Last updated: {lastUpdated}
          </p>
        )}
      </header>

      <div className="grid" style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
        <NumberField
          label="Reward per successful referral (₹)"
          value={config.rewardPerReferral}
          onChange={(v) => { setConfig({ ...config, rewardPerReferral: v }); setDirty(true); }}
        />
        <NumberField
          label="Worker signup bonus (₹)"
          value={config.signupBonus}
          onChange={(v) => { setConfig({ ...config, signupBonus: v }); setDirty(true); }}
        />
        <NumberField
          label="Employer signup bonus (₹)"
          value={config.employerSignupBonus}
          onChange={(v) => { setConfig({ ...config, employerSignupBonus: v }); setDirty(true); }}
        />
        <label style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 13 }}>
          <span style={{ color: "#374151", fontWeight: 500 }}>Welcome campaign id</span>
          <input
            type="text"
            value={config.welcomeBonusCampaignId}
            onChange={(e) => { setConfig({ ...config, welcomeBonusCampaignId: e.target.value }); setDirty(true); }}
            style={{ padding: 8, borderRadius: 6, border: "1px solid #d1d5db" }}
          />
        </label>
        <NumberField
          label="Minimum withdrawal (₹)"
          value={config.minWithdrawal}
          onChange={(v) => { setConfig({ ...config, minWithdrawal: v }); setDirty(true); }}
        />
        <NumberField
          label="Max withdrawal per day (₹)"
          value={config.maxWithdrawalPerDay}
          onChange={(v) => { setConfig({ ...config, maxWithdrawalPerDay: v }); setDirty(true); }}
        />
      </div>

      <div style={{ marginTop: 18, display: "grid", gap: 10 }}>
        <label style={{ display: "flex", gap: 10, alignItems: "flex-start", fontSize: 13 }}>
          <input
            type="checkbox"
            checked={config.employerSignupBonusEnabled}
            onChange={(e) => { setConfig({ ...config, employerSignupBonusEnabled: e.target.checked }); setDirty(true); }}
            style={{ marginTop: 3 }}
          />
          <span>
            <strong>Give employer cash signup bonus</strong>
            <small style={{ display: "block", color: "#6b7280", marginTop: 2 }}>
              New employers get the current employer amount once. Existing users keep their old credited amount.
            </small>
          </span>
        </label>
        <label style={{ display: "flex", gap: 10, alignItems: "flex-start", fontSize: 13 }}>
          <input
            type="checkbox"
            checked={config.employerUnlimitedJobPostingEnabled}
            onChange={(e) => { setConfig({ ...config, employerUnlimitedJobPostingEnabled: e.target.checked }); setDirty(true); }}
            style={{ marginTop: 3 }}
          />
          <span>
            <strong>Unlock unlimited job posting for new employers</strong>
            <small style={{ display: "block", color: "#6b7280", marginTop: 2 }}>
              Stored on the employer profile and referral stats as a durable welcome entitlement.
            </small>
          </span>
        </label>
      </div>

      <div style={{ marginTop: 24 }}>
        <h3 style={{ marginBottom: 8, fontSize: 14, fontWeight: 600 }}>Milestone bonuses</h3>
        {Object.entries(config.milestones).sort(([a], [b]) => Number(a) - Number(b)).map(([count, amount]) => (
          <div key={count} style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 6 }}>
            <span style={{ width: 120 }}>{count} referrals →</span>
            <input
              type="number"
              value={amount}
              min={0}
              onChange={(e) => {
                setConfig({
                  ...config,
                  milestones: { ...config.milestones, [count]: parseNumber(e.target.value, amount) },
                });
                setDirty(true);
              }}
              style={{ width: 100, padding: 6, borderRadius: 6, border: "1px solid #d1d5db" }}
            />
            <span style={{ color: "#6b7280", fontSize: 12 }}>₹</span>
          </div>
        ))}
      </div>

      <div style={{ marginTop: 24, display: "flex", gap: 12, alignItems: "center" }}>
        <button
          type="button"
          disabled={!dirty || saving}
          onClick={handleSave}
          className="pill pill-link"
          style={{
            background: dirty ? "#2563eb" : "#e5e7eb",
            color: dirty ? "white" : "#6b7280",
            cursor: dirty && !saving ? "pointer" : "not-allowed",
            padding: "8px 20px",
          }}
        >
          {saving ? "Saving…" : dirty ? "Save changes" : "Saved"}
        </button>
        {message && <span style={{ color: "#059669", fontSize: 13 }}>{message}</span>}
        {error && <span style={{ color: "#dc2626", fontSize: 13 }}>{error}</span>}
      </div>
    </section>
  );
}

function NumberField({ label, value, onChange }: { label: string; value: number; onChange: (v: number) => void }) {
  return (
    <label style={{ display: "flex", flexDirection: "column", gap: 4, fontSize: 13 }}>
      <span style={{ color: "#374151", fontWeight: 500 }}>{label}</span>
      <input
        type="number"
        min={0}
        value={value}
        onChange={(e) => onChange(parseNumber(e.target.value, value))}
        style={{ padding: 8, borderRadius: 6, border: "1px solid #d1d5db" }}
      />
    </label>
  );
}
