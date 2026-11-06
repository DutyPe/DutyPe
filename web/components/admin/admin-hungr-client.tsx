"use client";

import { useEffect, useMemo, useState } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatDateTime } from "@/lib/firebase/firestore-helpers";
import {
  Utensils,
  MapPin,
  TrendingUp,
  Users,
  Search,
  Filter,
  Download,
  ExternalLink,
  MessageCircle,
  Trash2,
  RefreshCw,
  Flame,
  Coffee,
  CheckCircle2,
  AlertCircle
} from "lucide-react";

type SurveyRow = {
  id: string;
  name?: string;
  phone?: string;
  city?: string;
  area?: string;
  apartmentName?: string | null;
  eatingFrequency?: string;
  willingToPreOrder?: boolean | string;
  preferredCategories?: string[];
  comments?: string | null;
  source?: string;
  voucherCode?: string;
  createdAt?: unknown;
};

type AreaStat = {
  area: string;
  count: number;
};

type CategoryStat = {
  category: string;
  count: number;
};

type CityStat = {
  city: string;
  count: number;
};

const CATEGORY_LABELS: Record<string, string> = {
  TIFFIN_IDLI_DOSA: "🥞 Idli, Dosa & Vada",
  FRESH_JUICE: "🥤 Fresh Fruit Juices & Shakes",
  FILTER_COFFEE: "☕ Filter Coffee & Tea",
  HEALTHY_MEALS: "🥗 Healthy Sprouts & Salads",
  EVENING_SNACKS: "🥟 Evening Bajji & Samosa"
};

const FREQUENCY_LABELS: Record<string, string> = {
  DAILY: "🔥 Daily Power Eater",
  "3_TO_4_DAYS": "⚡ 3-4 Days / Week",
  WEEKENDS: "✨ Weekends Only",
  RARELY: "🌱 Occasional"
};

export function AdminHungrClient() {
  const [surveys, setSurveys] = useState<SurveyRow[]>([]);
  const [areas, setAreas] = useState<AreaStat[]>([]);
  const [categories, setCategories] = useState<CategoryStat[]>([]);
  const [outsideCities, setOutsideCities] = useState<CityStat[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const [searchTerm, setSearchTerm] = useState("");
  const [cityFilter, setCityFilter] = useState("ALL");
  const [frequencyFilter, setFrequencyFilter] = useState("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 12;

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const res = await adminApiFetch("/api/admin/hungr");
      const json = await res.json() as {
        responses?: SurveyRow[];
        areas?: AreaStat[];
        categories?: CategoryStat[];
        outsideCities?: CityStat[];
        error?: string;
      };

      if (!res.ok) {
        throw new Error(json.error || "Failed to load Hungr intelligence data.");
      }

      setSurveys(json.responses || []);
      setAreas(json.areas || []);
      setCategories(json.categories || []);
      setOutsideCities(json.outsideCities || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load Hungr survey data.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  async function handleDelete(surveyId: string) {
    if (!confirm("Delete this waitlist response?")) return;
    try {
      const res = await adminApiFetch("/api/admin/hungr", {
        method: "DELETE",
        body: JSON.stringify({ surveyId })
      });
      if (!res.ok) throw new Error("Failed to delete record.");
      setSurveys((cur) => cur.filter((s) => s.id !== surveyId));
      setActionSuccess("Response deleted successfully.");
      setTimeout(() => setActionSuccess(null), 3000);
    } catch (err) {
      alert(err instanceof Error ? err.message : "Could not delete.");
    }
  }

  const filtered = useMemo(() => {
    const q = searchTerm.toLowerCase().trim();
    return surveys.filter((item) => {
      const matchesSearch =
        !q ||
        (item.name || "").toLowerCase().includes(q) ||
        (item.phone || "").toLowerCase().includes(q) ||
        (item.area || "").toLowerCase().includes(q) ||
        (item.apartmentName || "").toLowerCase().includes(q) ||
        (item.city || "").toLowerCase().includes(q);

      const isKhammam = (item.city || "").toLowerCase().includes("khammam");
      const matchesCity =
        cityFilter === "ALL" ||
        (cityFilter === "KHAMMAM" && isKhammam) ||
        (cityFilter === "OUTSIDE" && !isKhammam);

      const matchesFrequency =
        frequencyFilter === "ALL" ||
        (item.eatingFrequency || "").toUpperCase() === frequencyFilter;

      return matchesSearch && matchesCity && matchesFrequency;
    });
  }, [surveys, searchTerm, cityFilter, frequencyFilter]);

  const totalPages = Math.ceil(filtered.length / pageSize) || 1;
  const paginatedRows = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filtered.slice(start, start + pageSize);
  }, [filtered, currentPage, pageSize]);

  function exportCSV() {
    if (filtered.length === 0) return;
    const headers = ["Name", "Phone", "City", "Area", "Apartment", "Eating Frequency", "Preferred Foods", "Created At"];
    const rows = filtered.map((r) => [
      `"${r.name || ""}"`,
      `"${r.phone || ""}"`,
      `"${r.city || ""}"`,
      `"${r.area || ""}"`,
      `"${r.apartmentName || ""}"`,
      `"${r.eatingFrequency || ""}"`,
      `"${(r.preferredCategories || []).join(", ")}"`,
      `"${r.createdAt || ""}"`
    ]);

    const csvContent = "data:text/csv;charset=utf-8," + [headers.join(","), ...rows.map((e) => e.join(","))].join("\n");
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `hungr_waitlist_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  const khammamTotal = surveys.filter((s) => (s.city || "").toLowerCase().includes("khammam")).length;
  const outsideTotal = surveys.length - khammamTotal;
  const dailyTotal = surveys.filter((s) => (s.eatingFrequency || "").toUpperCase() === "DAILY").length;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
      {/* Top Banner & Quick Links */}
      <div
        className="adm-card"
        style={{
          background: "linear-gradient(135deg, #FFF7ED 0%, #FFEDD5 50%, #FED7AA 100%)",
          borderColor: "#FDBA74",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          flexWrap: "wrap",
          gap: 16,
          padding: "20px 24px"
        }}
      >
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 6 }}>
            <span
              style={{
                fontSize: 22,
                background: "#FF6B00",
                color: "#FFFFFF",
                borderRadius: "50%",
                width: 36,
                height: 36,
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                fontWeight: "bold"
              }}
            >
              🥞
            </span>
            <h1 style={{ fontSize: 22, fontWeight: 800, color: "#9A3412", margin: 0 }}>
              Hungr Pre-Order Demand Intelligence
            </h1>
            <span
              style={{
                background: "#EA580C",
                color: "#FFFFFF",
                fontSize: 11,
                fontWeight: 700,
                padding: "2px 8px",
                borderRadius: 20,
                letterSpacing: 0.5
              }}
            >
              PMF ENGINE
            </span>
          </div>
          <p style={{ margin: 0, fontSize: 13, color: "#C2410C", maxWidth: 640 }}>
            Batch pre-ordering tiffins, juices & snacks at <strong>exact offline shop price (₹0 markup)</strong> with next-morning gate delivery. Live demand signals from Khammam neighborhoods & expanding towns.
          </p>
        </div>

        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <a
            href="/hungr"
            target="_blank"
            rel="noopener noreferrer"
            className="adm-btn"
            style={{
              background: "#FFFFFF",
              color: "#C2410C",
              border: "1px solid #FDBA74",
              display: "inline-flex",
              alignItems: "center",
              gap: 6,
              fontWeight: 600
            }}
          >
            <ExternalLink style={{ width: 15, height: 15 }} />
            Open Live Web Survey (/hungr)
          </a>
          <button
            onClick={() => void loadData()}
            className="adm-btn"
            style={{
              background: "#9A3412",
              color: "#FFFFFF",
              border: "none",
              display: "inline-flex",
              alignItems: "center",
              gap: 6,
              fontWeight: 600
            }}
          >
            <RefreshCw style={{ width: 14, height: 14 }} className={loading ? "animate-spin" : ""} />
            Refresh
          </button>
        </div>
      </div>

      {actionSuccess && (
        <div style={{ background: "#ECFDF5", border: "1px solid #A7F3D0", padding: "10px 16px", borderRadius: 8, color: "#065F46", fontSize: 13 }}>
          {actionSuccess}
        </div>
      )}

      {error && (
        <div style={{ background: "#FEF2F2", border: "1px solid #FECACA", padding: "10px 16px", borderRadius: 8, color: "#991B1B", fontSize: 13 }}>
          {error}
        </div>
      )}

      {/* KPI Cards */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(210px, 1fr))", gap: 14 }}>
        <div className="adm-card" style={{ padding: "16px 20px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontSize: 13, color: "var(--adm-text-muted)", fontWeight: 500 }}>Total Responses</span>
            <Users style={{ width: 18, height: 18, color: "#FF6B00" }} />
          </div>
          <div style={{ fontSize: 28, fontWeight: 800, marginTop: 8, color: "var(--adm-text)" }}>
            {surveys.length}
          </div>
          <div style={{ fontSize: 12, color: "#16A34A", marginTop: 4, fontWeight: 600 }}>
            100% Verified Phone Leads
          </div>
        </div>

        <div className="adm-card" style={{ padding: "16px 20px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontSize: 13, color: "var(--adm-text-muted)", fontWeight: 500 }}>Khammam Local</span>
            <MapPin style={{ width: 18, height: 18, color: "#2563EB" }} />
          </div>
          <div style={{ fontSize: 28, fontWeight: 800, marginTop: 8, color: "#2563EB" }}>
            {khammamTotal}
          </div>
          <div style={{ fontSize: 12, color: "var(--adm-text-muted)", marginTop: 4 }}>
            {surveys.length > 0 ? Math.round((khammamTotal / surveys.length) * 100) : 0}% of total demand
          </div>
        </div>

        <div className="adm-card" style={{ padding: "16px 20px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontSize: 13, color: "var(--adm-text-muted)", fontWeight: 500 }}>Outside Khammam</span>
            <TrendingUp style={{ width: 18, height: 18, color: "#9333EA" }} />
          </div>
          <div style={{ fontSize: 28, fontWeight: 800, marginTop: 8, color: "#9333EA" }}>
            {outsideTotal}
          </div>
          <div style={{ fontSize: 12, color: "var(--adm-text-muted)", marginTop: 4 }}>
            Expansion interest captured
          </div>
        </div>

        <div className="adm-card" style={{ padding: "16px 20px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontSize: 13, color: "var(--adm-text-muted)", fontWeight: 500 }}>Daily Power Eaters</span>
            <Flame style={{ width: 18, height: 18, color: "#DC2626" }} />
          </div>
          <div style={{ fontSize: 28, fontWeight: 800, marginTop: 8, color: "#DC2626" }}>
            {dailyTotal}
          </div>
          <div style={{ fontSize: 12, color: "var(--adm-text-muted)", marginTop: 4 }}>
            {surveys.length > 0 ? Math.round((dailyTotal / surveys.length) * 100) : 0}% eat breakfast outside daily
          </div>
        </div>
      </div>

      {/* Demand Insights: Areas & Preferred Food Breakdown */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: 16 }}>
        {/* Khammam Neighborhood Clusters */}
        <div className="adm-card">
          <div className="adm-card-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <h2 className="adm-card-title" style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 15 }}>
              <MapPin style={{ width: 18, height: 18, color: "#2563EB" }} />
              Khammam Locality Demand Clusters
            </h2>
            <span style={{ fontSize: 12, color: "var(--adm-text-muted)" }}>Route Density Ranking</span>
          </div>
          <div style={{ padding: "16px 20px", display: "flex", flexDirection: "column", gap: 12 }}>
            {areas.length === 0 ? (
              <div style={{ fontSize: 13, color: "var(--adm-text-muted)", textAlign: "center", padding: "20px 0" }}>
                No area data recorded yet.
              </div>
            ) : (
              areas.slice(0, 7).map((item, idx) => {
                const max = areas[0]?.count || 1;
                const percent = Math.round((item.count / max) * 100);
                return (
                  <div key={item.area}>
                    <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13, marginBottom: 4 }}>
                      <span style={{ fontWeight: 600 }}>
                        {idx === 0 ? "🥇 " : idx === 1 ? "🥈 " : idx === 2 ? "🥉 " : "• "}
                        {item.area}
                      </span>
                      <span style={{ fontWeight: 700, color: "var(--adm-primary)" }}>{item.count} leads</span>
                    </div>
                    <div style={{ height: 6, width: "100%", background: "var(--adm-border)", borderRadius: 3, overflow: "hidden" }}>
                      <div style={{ height: "100%", width: `${percent}%`, background: idx === 0 ? "#FF6B00" : "#2563EB", borderRadius: 3 }} />
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Most Demanded Food Items */}
        <div className="adm-card">
          <div className="adm-card-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <h2 className="adm-card-title" style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 15 }}>
              <Utensils style={{ width: 18, height: 18, color: "#FF6B00" }} />
              Demanded Food Categories
            </h2>
            <span style={{ fontSize: 12, color: "var(--adm-text-muted)" }}>Vendor Alignment</span>
          </div>
          <div style={{ padding: "16px 20px", display: "flex", flexDirection: "column", gap: 12 }}>
            {categories.length === 0 ? (
              <div style={{ fontSize: 13, color: "var(--adm-text-muted)", textAlign: "center", padding: "20px 0" }}>
                No category data recorded yet.
              </div>
            ) : (
              categories.map((item) => {
                const max = categories[0]?.count || 1;
                const percent = Math.round((item.count / max) * 100);
                const label = CATEGORY_LABELS[item.category] || item.category;
                return (
                  <div key={item.category}>
                    <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13, marginBottom: 4 }}>
                      <span style={{ fontWeight: 600 }}>{label}</span>
                      <span style={{ fontWeight: 700, color: "#D97706" }}>{item.count} votes</span>
                    </div>
                    <div style={{ height: 6, width: "100%", background: "var(--adm-border)", borderRadius: 3, overflow: "hidden" }}>
                      <div style={{ height: "100%", width: `${percent}%`, background: "#F59E0B", borderRadius: 3 }} />
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* Data Table Filter & Controls */}
      <div className="adm-card">
        <div
          className="adm-card-header"
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            flexWrap: "wrap",
            gap: 12
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <h2 className="adm-card-title" style={{ margin: 0 }}>
              Waitlist Leads & User Feedback ({filtered.length})
            </h2>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
            <div style={{ position: "relative" }}>
              <Search
                style={{
                  position: "absolute",
                  left: 10,
                  top: "50%",
                  transform: "translateY(-50%)",
                  width: 14,
                  height: 14,
                  color: "var(--adm-text-muted)"
                }}
              />
              <input
                type="text"
                placeholder="Search name, phone, area, apartment..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="adm-input"
                style={{ paddingLeft: 30, fontSize: 13, width: 220 }}
              />
            </div>

            <select
              value={cityFilter}
              onChange={(e) => setCityFilter(e.target.value)}
              className="adm-input"
              style={{ fontSize: 13, width: 140 }}
            >
              <option value="ALL">All Cities</option>
              <option value="KHAMMAM">Khammam Only</option>
              <option value="OUTSIDE">Outside Khammam</option>
            </select>

            <select
              value={frequencyFilter}
              onChange={(e) => setFrequencyFilter(e.target.value)}
              className="adm-input"
              style={{ fontSize: 13, width: 140 }}
            >
              <option value="ALL">All Frequencies</option>
              <option value="DAILY">Daily Power Eaters</option>
              <option value="3_TO_4_DAYS">3-4 Days/Week</option>
              <option value="WEEKENDS">Weekends</option>
            </select>

            <button
              onClick={exportCSV}
              className="adm-btn"
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: 6,
                fontSize: 13,
                fontWeight: 600,
                border: "1px solid var(--adm-border)"
              }}
            >
              <Download style={{ width: 14, height: 14 }} />
              Export CSV
            </button>
          </div>
        </div>

        {/* Table Content */}
        <div style={{ overflowX: "auto" }}>
          <table className="adm-table" style={{ width: "100%", textAlign: "left", fontSize: 13 }}>
            <thead>
              <tr>
                <th style={{ padding: "12px 16px" }}>User & Phone</th>
                <th style={{ padding: "12px 16px" }}>City & Area</th>
                <th style={{ padding: "12px 16px" }}>Apartment / Landmark</th>
                <th style={{ padding: "12px 16px" }}>Frequency</th>
                <th style={{ padding: "12px 16px" }}>Preferred Food</th>
                <th style={{ padding: "12px 16px" }}>Registered</th>
                <th style={{ padding: "12px 16px", textAlign: "right" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", padding: 32, color: "var(--adm-text-muted)" }}>
                    Loading survey data...
                  </td>
                </tr>
              ) : paginatedRows.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", padding: 32, color: "var(--adm-text-muted)" }}>
                    No responses matched your filters.
                  </td>
                </tr>
              ) : (
                paginatedRows.map((row) => {
                  const isKhammam = (row.city || "").toLowerCase().includes("khammam");
                  const cleanPhone = (row.phone || "").replace(/\D/g, "");
                  const whatsappUrl = `https://wa.me/${cleanPhone}?text=Hi%20${encodeURIComponent(
                    row.name || "there"
                  )}!%20Thank%20you%20for%20joining%20the%20Hungr%20waitlist.%20Your%20%E2%82%B950%20free%20delivery%20voucher%20HUNGR50%20is%20ready!`;

                  return (
                    <tr key={row.id}>
                      <td style={{ padding: "12px 16px" }}>
                        <div style={{ fontWeight: 600, color: "var(--adm-text)" }}>{row.name || "Anonymous"}</div>
                        <div style={{ fontSize: 12, color: "var(--adm-text-muted)", fontFamily: "monospace" }}>
                          {row.phone || "—"}
                        </div>
                      </td>

                      <td style={{ padding: "12px 16px" }}>
                        <div style={{ fontWeight: 600, color: isKhammam ? "#2563EB" : "#9333EA" }}>
                          {row.area || "—"}
                        </div>
                        <div style={{ fontSize: 11, color: "var(--adm-text-muted)" }}>{row.city || "Khammam"}</div>
                      </td>

                      <td style={{ padding: "12px 16px" }}>
                        <span style={{ color: row.apartmentName ? "var(--adm-text)" : "var(--adm-text-muted)" }}>
                          {row.apartmentName || "—"}
                        </span>
                      </td>

                      <td style={{ padding: "12px 16px" }}>
                        <span
                          style={{
                            fontSize: 11,
                            padding: "3px 8px",
                            borderRadius: 12,
                            fontWeight: 600,
                            background:
                              row.eatingFrequency === "DAILY"
                                ? "#FEE2E2"
                                : row.eatingFrequency === "3_TO_4_DAYS"
                                ? "#FEF3C7"
                                : "#E0E7FF",
                            color:
                              row.eatingFrequency === "DAILY"
                                ? "#991B1B"
                                : row.eatingFrequency === "3_TO_4_DAYS"
                                ? "#92400E"
                                : "#3730A3"
                          }}
                        >
                          {FREQUENCY_LABELS[row.eatingFrequency || ""] || row.eatingFrequency || "Normal"}
                        </span>
                      </td>

                      <td style={{ padding: "12px 16px" }}>
                        <div style={{ display: "flex", flexWrap: "wrap", gap: 4, maxWidth: 220 }}>
                          {(row.preferredCategories || []).map((cat) => (
                            <span
                              key={cat}
                              style={{
                                fontSize: 11,
                                padding: "2px 6px",
                                borderRadius: 4,
                                background: "rgba(0,0,0,0.04)",
                                color: "var(--adm-text)"
                              }}
                            >
                              {cat === "TIFFIN_IDLI_DOSA"
                                ? "🥞 Tiffin"
                                : cat === "FRESH_JUICE"
                                ? "🥤 Juice"
                                : cat === "FILTER_COFFEE"
                                ? "☕ Coffee"
                                : cat === "HEALTHY_MEALS"
                                ? "🥗 Healthy"
                                : "🥟 Snacks"}
                            </span>
                          ))}
                        </div>
                      </td>

                      <td style={{ padding: "12px 16px", fontSize: 12, color: "var(--adm-text-muted)" }}>
                        {formatDateTime(row.createdAt)}
                      </td>

                      <td style={{ padding: "12px 16px", textAlign: "right" }}>
                        <div style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                          {cleanPhone && (
                            <a
                              href={whatsappUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="adm-btn"
                              title="Chat on WhatsApp"
                              style={{
                                padding: "4px 8px",
                                background: "#DCFCE7",
                                color: "#166534",
                                border: "1px solid #86EFAC"
                              }}
                            >
                              <MessageCircle style={{ width: 14, height: 14 }} />
                            </a>
                          )}
                          <button
                            onClick={() => void handleDelete(row.id)}
                            className="adm-btn"
                            title="Delete"
                            style={{
                              padding: "4px 8px",
                              background: "#FEE2E2",
                              color: "#991B1B",
                              border: "1px solid #FCA5A5"
                            }}
                          >
                            <Trash2 style={{ width: 14, height: 14 }} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div
            style={{
              padding: "12px 16px",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              borderTop: "1px solid var(--adm-border)"
            }}
          >
            <span style={{ fontSize: 12, color: "var(--adm-text-muted)" }}>
              Page {currentPage} of {totalPages}
            </span>
            <div style={{ display: "flex", gap: 6 }}>
              <button
                disabled={currentPage <= 1}
                onClick={() => setCurrentPage((c) => c - 1)}
                className="adm-btn"
                style={{ fontSize: 12, padding: "4px 10px" }}
              >
                Previous
              </button>
              <button
                disabled={currentPage >= totalPages}
                onClick={() => setCurrentPage((c) => c + 1)}
                className="adm-btn"
                style={{ fontSize: 12, padding: "4px 10px" }}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
