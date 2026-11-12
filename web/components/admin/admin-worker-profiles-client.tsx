"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import {
  HardHat,
  Search,
  Phone,
  MessageSquare,
  ShieldCheck,
  ShieldAlert,
  MapPin,
  RefreshCw,
  ExternalLink,
  CheckCircle,
  Briefcase,
  Star,
  Users
} from "lucide-react";

const KHAMMAM_LAT = 17.2473;
const KHAMMAM_LNG = 80.1514;

function calcDistanceKm(lat1?: number, lon1?: number, lat2 = KHAMMAM_LAT, lon2 = KHAMMAM_LNG): number | null {
  if (lat1 === undefined || lon1 === undefined || isNaN(lat1) || isNaN(lon1)) return null;
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c);
}

export type WorkerProfile = {
  id: string;
  name?: string;
  phone?: string;
  skills?: string[];
  area?: string;
  address?: string;
  lat?: number;
  lng?: number;
  experienceYears?: number;
  available?: boolean;
  blocked?: boolean;
  education?: string;
  gender?: string;
  dateOfBirth?: string;
  aadhaarMasked?: string;
  aadhaarVerified?: boolean;
  isAadhaarVerified?: boolean;
  verifiedLegalName?: string;
  createdAt?: string;
  updatedAt?: string;
};

const SERVICE_CATEGORIES = [
  "ALL",
  "CLEANING",
  "AC",
  "ELECTRICIAN",
  "PLUMBER",
  "CARPENTER",
  "PAINTER",
  "APPLIANCE",
  "HOME_HELP",
  "VEHICLE"
];

export function AdminWorkerProfilesClient() {
  const [profiles, setProfiles] = useState<WorkerProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [locationFilter, setLocationFilter] = useState<"ALL" | "KHAMMAM">("KHAMMAM");
  const [skillFilter, setSkillFilter] = useState<string>("ALL");
  const [partnerModalWorker, setPartnerModalWorker] = useState<WorkerProfile | null>(null);
  const [selectedCategories, setSelectedCategories] = useState<string[]>(["ALL"]);
  const [assigning, setAssigning] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const loadProfiles = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await adminApiFetch("/api/admin/worker-profiles?limit=5000", { cache: "no-store" });
      const json = await res.json();
      if (!res.ok) throw new Error(json.error || "Failed to load worker profiles");
      setProfiles(json.profiles || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load profiles");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadProfiles();
  }, [loadProfiles]);

  const khammamWorkersCount = useMemo(() => {
    return profiles.filter((p) => {
      const textMatch = [p.name, p.area, p.address].filter(Boolean).some((s) => s!.toLowerCase().includes("khammam"));
      const dist = calcDistanceKm(p.lat, p.lng);
      return textMatch || (dist !== null && dist <= 50);
    }).length;
  }, [profiles]);

  const filteredProfiles = useMemo(() => {
    return profiles.filter((p) => {
      const dist = calcDistanceKm(p.lat, p.lng);
      const isKhammam = [p.name, p.area, p.address].filter(Boolean).some((s) => s!.toLowerCase().includes("khammam")) || (dist !== null && dist <= 50);

      if (locationFilter === "KHAMMAM" && !isKhammam) {
        return false;
      }

      if (skillFilter !== "ALL") {
        if (!p.skills?.some((s) => s.toLowerCase().includes(skillFilter.toLowerCase()))) {
          return false;
        }
      }

      if (search.trim()) {
        const query = search.trim().toLowerCase();
        const matchesName = p.name?.toLowerCase().includes(query);
        const matchesPhone = p.phone?.includes(query);
        const matchesArea = p.area?.toLowerCase().includes(query) || p.address?.toLowerCase().includes(query);
        const matchesSkills = p.skills?.some((s) => s.toLowerCase().includes(query));
        if (!matchesName && !matchesPhone && !matchesArea && !matchesSkills) {
          return false;
        }
      }

      return true;
    }).sort((a, b) => {
      const distA = calcDistanceKm(a.lat, a.lng) ?? 999;
      const distB = calcDistanceKm(b.lat, b.lng) ?? 999;
      return distA - distB;
    });
  }, [profiles, locationFilter, skillFilter, search]);

  async function handleAssignServicePartner() {
    if (!partnerModalWorker) return;
    setAssigning(true);
    setSuccessMsg(null);
    try {
      const res = await fetch("/api/admin/assign-service-partner", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          partnerId: partnerModalWorker.id,
          name: partnerModalWorker.name || "Service Partner",
          phone: partnerModalWorker.phone,
          categories: selectedCategories,
          area: partnerModalWorker.area || partnerModalWorker.address || "Khammam",
          lat: partnerModalWorker.lat ?? KHAMMAM_LAT,
          lng: partnerModalWorker.lng ?? KHAMMAM_LNG
        })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Failed to assign partner");
      setSuccessMsg(`✓ ${partnerModalWorker.name} successfully assigned as Service Partner!`);
      setPartnerModalWorker(null);
      setTimeout(() => setSuccessMsg(null), 5000);
    } catch (err) {
      alert(err instanceof Error ? err.message : "Error assigning service partner");
    } finally {
      setAssigning(false);
    }
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
      {/* Banner / Notice */}
      {successMsg && (
        <div style={{ background: "#ecfdf5", border: "1px solid #a7f3d0", color: "#065f46", padding: "12px 16px", borderRadius: 8, display: "flex", alignItems: "center", gap: 8, fontWeight: 600 }}>
          <CheckCircle style={{ width: 18, height: 18, color: "#10b981" }} />
          {successMsg}
        </div>
      )}

      {/* KPI Cards */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 12 }}>
        <div className="adm-card" style={{ padding: "16px 20px" }}>
          <span style={{ fontSize: 12, fontWeight: 700, color: "var(--adm-text-muted)", textTransform: "uppercase" }}>Total Workers</span>
          <div style={{ fontSize: "1.75rem", fontWeight: 800, color: "var(--adm-text-main)", marginTop: 4 }}>
            {profiles.length}
          </div>
        </div>

        <div className="adm-card" style={{ padding: "16px 20px", border: "2px solid #38bdf8", background: "#f0f9ff" }}>
          <span style={{ fontSize: 12, fontWeight: 700, color: "#0369a1", textTransform: "uppercase" }}>📍 Khammam Workers</span>
          <div style={{ fontSize: "1.75rem", fontWeight: 800, color: "#0284c7", marginTop: 4 }}>
            {khammamWorkersCount}
          </div>
        </div>

        <div className="adm-card" style={{ padding: "16px 20px" }}>
          <span style={{ fontSize: 12, fontWeight: 700, color: "var(--adm-text-muted)", textTransform: "uppercase" }}>Aadhaar e-KYC Done</span>
          <div style={{ fontSize: "1.75rem", fontWeight: 800, color: "#16a34a", marginTop: 4 }}>
            {profiles.filter((p) => p.aadhaarMasked || p.aadhaarVerified).length}
          </div>
        </div>

        <div className="adm-card" style={{ padding: "16px 20px" }}>
          <span style={{ fontSize: 12, fontWeight: 700, color: "var(--adm-text-muted)", textTransform: "uppercase" }}>Available Now</span>
          <div style={{ fontSize: "1.75rem", fontWeight: 800, color: "#4f46e5", marginTop: 4 }}>
            {profiles.filter((p) => p.available !== false).length}
          </div>
        </div>
      </div>

      {/* Controls & Filter Toolbar */}
      <div className="adm-card" style={{ padding: "14px 18px", display: "flex", flexWrap: "wrap", alignItems: "center", gap: 12 }}>
        {/* Location Toggle */}
        <div style={{ display: "inline-flex", borderRadius: 8, padding: 3, background: "var(--adm-surface-subtle)", border: "1px solid var(--adm-border)" }}>
          <button
            type="button"
            onClick={() => setLocationFilter("KHAMMAM")}
            style={{
              padding: "6px 14px",
              borderRadius: 6,
              fontSize: "0.85rem",
              fontWeight: 700,
              border: "none",
              cursor: "pointer",
              background: locationFilter === "KHAMMAM" ? "#0284c7" : "transparent",
              color: locationFilter === "KHAMMAM" ? "#ffffff" : "var(--adm-text-muted)"
            }}
          >
            📍 Khammam ({khammamWorkersCount})
          </button>
          <button
            type="button"
            onClick={() => setLocationFilter("ALL")}
            style={{
              padding: "6px 14px",
              borderRadius: 6,
              fontSize: "0.85rem",
              fontWeight: 700,
              border: "none",
              cursor: "pointer",
              background: locationFilter === "ALL" ? "#0f172a" : "transparent",
              color: locationFilter === "ALL" ? "#ffffff" : "var(--adm-text-muted)"
            }}
          >
            All Cities ({profiles.length})
          </button>
        </div>

        {/* Search */}
        <div style={{ display: "flex", alignItems: "center", gap: 8, flex: 1, minWidth: 220, position: "relative" }}>
          <Search style={{ width: 16, height: 16, color: "var(--adm-text-muted)", position: "absolute", left: 10 }} />
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name, phone, trade, area..."
            style={{
              width: "100%",
              padding: "7px 12px 7px 32px",
              borderRadius: 8,
              border: "1px solid var(--adm-border)",
              fontSize: "0.875rem"
            }}
          />
        </div>

        {/* Skill Dropdown */}
        <select
          value={skillFilter}
          onChange={(e) => setSkillFilter(e.target.value)}
          style={{
            padding: "7px 12px",
            borderRadius: 8,
            border: "1px solid var(--adm-border)",
            fontSize: "0.875rem",
            background: "#ffffff"
          }}
        >
          <option value="ALL">All Skills / Trades</option>
          <option value="ELECTRICIAN">Electrician</option>
          <option value="PLUMBER">Plumber</option>
          <option value="CLEAN">Cleaner / Maid</option>
          <option value="DRIVER">Driver</option>
          <option value="DELIVERY">Delivery</option>
          <option value="HELPER">Helper / Peon</option>
          <option value="CARPENTER">Carpenter</option>
          <option value="PAINTER">Painter</option>
          <option value="COOK">Cook</option>
          <option value="SECURITY">Security</option>
        </select>

        {/* Refresh */}
        <button
          type="button"
          onClick={() => void loadProfiles()}
          className="adm-btn adm-btn-secondary"
          style={{ padding: "7px 12px", display: "inline-flex", alignItems: "center", gap: 6 }}
        >
          <RefreshCw style={{ width: 14, height: 14 }} className={loading ? "animate-spin" : ""} />
          Refresh
        </button>
      </div>

      {/* Profiles Table */}
      {loading && profiles.length === 0 ? (
        <div className="adm-card" style={{ padding: 40, textAlign: "center", color: "var(--adm-text-muted)" }}>
          Loading worker profiles...
        </div>
      ) : filteredProfiles.length === 0 ? (
        <div className="adm-card" style={{ padding: 40, textAlign: "center" }}>
          <Users style={{ width: 40, height: 40, color: "var(--adm-text-muted)", margin: "0 auto 12px" }} />
          <h3 style={{ margin: 0 }}>No workers matched</h3>
          <p style={{ color: "var(--adm-text-muted)", fontSize: 14, marginTop: 4 }}>
            Try resetting your search query or location filter.
          </p>
        </div>
      ) : (
        <div className="adm-table-wrap">
          <table className="adm-table">
            <thead>
              <tr>
                <th>Worker</th>
                <th>Contact &amp; Check</th>
                <th>Skills &amp; Exp</th>
                <th>Location / Distance</th>
                <th>Aadhaar KYC</th>
                <th style={{ textAlign: "right" }}>Assign Partner</th>
              </tr>
            </thead>
            <tbody>
              {filteredProfiles.map((w) => {
                const dist = calcDistanceKm(w.lat, w.lng);
                const isKhammam = [w.name, w.area, w.address].filter(Boolean).some((s) => s!.toLowerCase().includes("khammam")) || (dist !== null && dist <= 50);
                const cleanPhone = (w.phone || "").replace(/[^0-9]/g, "");

                return (
                  <tr key={w.id}>
                    <td>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <div
                          style={{
                            width: 38,
                            height: 38,
                            borderRadius: "50%",
                            background: isKhammam ? "#e0f2fe" : "#f1f5f9",
                            color: isKhammam ? "#0369a1" : "#475569",
                            border: `2px solid ${isKhammam ? "#7dd3fc" : "#cbd5e1"}`,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            fontWeight: 700,
                            fontSize: "0.85rem"
                          }}
                        >
                          {w.name ? w.name.slice(0, 2).toUpperCase() : "W"}
                        </div>
                        <div>
                          <div style={{ fontWeight: 700, color: "var(--adm-text-main)" }}>
                            {w.name || "Unnamed Worker"}
                          </div>
                          <div style={{ fontSize: "0.72rem", color: "var(--adm-text-muted)" }}>
                            UID: {w.id.slice(0, 10)}...
                          </div>
                        </div>
                      </div>
                    </td>

                    <td>
                      <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                        <span style={{ fontWeight: 600, fontSize: "0.88rem", color: "#0f172a" }}>
                          {w.phone || "No phone"}
                        </span>
                        {cleanPhone ? (
                          <div style={{ display: "inline-flex", gap: 6 }}>
                            <a
                              href={`tel:${w.phone}`}
                              className="adm-btn adm-btn-secondary"
                              style={{
                                fontSize: "0.75rem",
                                padding: "3px 8px",
                                display: "inline-flex",
                                alignItems: "center",
                                gap: 4,
                                textDecoration: "none"
                              }}
                              title="Call Worker"
                            >
                              <Phone style={{ width: 12, height: 12, color: "#2563eb" }} />
                              Call
                            </a>
                            <a
                              href={`https://wa.me/${cleanPhone}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="adm-btn adm-btn-secondary"
                              style={{
                                fontSize: "0.75rem",
                                padding: "3px 8px",
                                display: "inline-flex",
                                alignItems: "center",
                                gap: 4,
                                textDecoration: "none",
                                color: "#16a34a"
                              }}
                              title="WhatsApp Worker"
                            >
                              <MessageSquare style={{ width: 12, height: 12, color: "#16a34a" }} />
                              WhatsApp
                            </a>
                          </div>
                        ) : null}
                      </div>
                    </td>

                    <td>
                      <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                        <div style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
                          {w.skills && w.skills.length > 0 ? (
                            w.skills.map((s) => (
                              <span
                                key={s}
                                style={{
                                  fontSize: "0.7rem",
                                  fontWeight: 600,
                                  background: "#f1f5f9",
                                  color: "#334155",
                                  padding: "2px 6px",
                                  borderRadius: 4
                                }}
                              >
                                {s}
                              </span>
                            ))
                          ) : (
                            <span style={{ fontSize: "0.75rem", color: "#94a3b8" }}>General Labour</span>
                          )}
                        </div>
                        <div style={{ fontSize: "0.72rem", color: "#64748b" }}>
                          {w.experienceYears ? `${w.experienceYears} yrs exp` : "Fresh / Ready"}
                        </div>
                      </div>
                    </td>

                    <td>
                      <div style={{ display: "flex", flexDirection: "column", gap: 3 }}>
                        <div style={{ display: "inline-flex", alignItems: "center", gap: 4, fontWeight: 600, fontSize: "0.82rem", color: "#0f172a" }}>
                          <MapPin style={{ width: 13, height: 13, color: isKhammam ? "#0284c7" : "#94a3b8" }} />
                          {w.area || w.address || "Area not specified"}
                        </div>
                        {dist !== null ? (
                          <span
                            style={{
                              display: "inline-flex",
                              alignItems: "center",
                              width: "fit-content",
                              fontSize: "0.7rem",
                              fontWeight: 700,
                              background: dist <= 25 ? "#ecfdf5" : "#fef2f2",
                              color: dist <= 25 ? "#059669" : "#dc2626",
                              padding: "1px 6px",
                              borderRadius: 4
                            }}
                          >
                            {dist <= 25 ? `✓ Khammam (${dist} km)` : `Outside (${dist} km)`}
                          </span>
                        ) : (
                          <span style={{ fontSize: "0.7rem", color: "#94a3b8" }}>Khammam Local</span>
                        )}
                      </div>
                    </td>

                    <td>
                      {w.aadhaarMasked || w.aadhaarVerified ? (
                        <div style={{ display: "inline-flex", alignItems: "center", gap: 4, fontSize: "0.72rem", fontWeight: 700, color: "#15803d", background: "#f0fdf4", border: "1px solid #bbf7d0", padding: "2px 6px", borderRadius: 4 }}>
                          <ShieldCheck style={{ width: 13, height: 13, color: "#16a34a" }} />
                          {w.aadhaarMasked || "Verified"}
                        </div>
                      ) : (
                        <div style={{ display: "inline-flex", alignItems: "center", gap: 4, fontSize: "0.72rem", color: "#b45309", background: "#fffbeb", border: "1px solid #fef3c7", padding: "2px 6px", borderRadius: 4 }}>
                          <ShieldAlert style={{ width: 13, height: 13, color: "#d97706" }} />
                          Pending
                        </div>
                      )}
                    </td>

                    <td style={{ textAlign: "right" }}>
                      <button
                        type="button"
                        onClick={() => {
                          setPartnerModalWorker(w);
                          setSelectedCategories(w.skills && w.skills.length > 0 ? ["ALL", ...w.skills] : ["ALL"]);
                        }}
                        className="adm-btn adm-btn-primary"
                        style={{
                          fontSize: "0.78rem",
                          padding: "6px 12px",
                          display: "inline-flex",
                          alignItems: "center",
                          gap: 6
                        }}
                      >
                        <Star style={{ width: 13, height: 13 }} />
                        Assign Partner
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Assign Service Partner Modal */}
      {partnerModalWorker && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 9999,
            padding: 16
          }}
        >
          <div
            className="adm-card"
            style={{
              background: "#ffffff",
              maxWidth: 480,
              width: "100%",
              padding: 24,
              borderRadius: 14,
              boxShadow: "0 20px 25px -5px rgba(0,0,0,0.2)"
            }}
          >
            <h3 style={{ margin: "0 0 8px 0", fontSize: "1.2rem", fontWeight: 700 }}>
              Assign as Service Partner
            </h3>
            <p style={{ margin: "0 0 16px 0", fontSize: "0.85rem", color: "#64748b" }}>
              Approve <strong>{partnerModalWorker.name}</strong> ({partnerModalWorker.phone}) as an official DutyPe Service Partner in Khammam.
            </p>

            <div style={{ marginBottom: 16 }}>
              <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: 8, color: "#334155" }}>
                Select Approved Service Categories:
              </label>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, maxHeight: 200, overflowY: "auto", padding: 8, border: "1px solid #e2e8f0", borderRadius: 8 }}>
                {SERVICE_CATEGORIES.map((cat) => {
                  const isChecked = selectedCategories.includes(cat);
                  return (
                    <label key={cat} style={{ display: "flex", alignItems: "center", gap: 6, fontSize: "0.8rem", cursor: "pointer" }}>
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setSelectedCategories([...selectedCategories, cat]);
                          } else {
                            setSelectedCategories(selectedCategories.filter((c) => c !== cat));
                          }
                        }}
                      />
                      {cat}
                    </label>
                  );
                })}
              </div>
            </div>

            <div style={{ background: "#f8fafc", padding: "10px 14px", borderRadius: 8, fontSize: "0.8rem", color: "#475569", marginBottom: 20 }}>
              📍 Default Location: <strong>{partnerModalWorker.area || "Khammam Center"}</strong> (17.2473, 80.1514)
              <br />
              🎁 Welcome Credits: <strong>₹100 (10,000 paise)</strong>
            </div>

            <div style={{ display: "flex", justifyContent: "flex-end", gap: 10 }}>
              <button
                type="button"
                onClick={() => setPartnerModalWorker(null)}
                className="adm-btn adm-btn-secondary"
                disabled={assigning}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => void handleAssignServicePartner()}
                className="adm-btn adm-btn-primary"
                disabled={assigning}
                style={{ display: "inline-flex", alignItems: "center", gap: 6 }}
              >
                {assigning ? "Assigning..." : "Confirm & Make Partner"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
