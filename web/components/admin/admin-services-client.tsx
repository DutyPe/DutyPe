"use client";

import { useEffect, useMemo, useState } from "react";
import { collection, doc, getDoc, limit, onSnapshot, orderBy, query, setDoc, Timestamp } from "firebase/firestore";
import { httpsCallable } from "firebase/functions";
import { getFirebaseServices } from "@/lib/firebase/client";
import { AdminBannersManager } from "./admin-banners-manager";
import { AdminCategoriesServicesManager } from "./admin-categories-services-manager";

/**
 * DutyPe Services admin: approve partners, verify partner credit top-ups (UPI UTR), watch
 * bookings, manage promotional banners, add categories & services with multi-variant options,
 * and set fees / partner fee / coupons / UPI ID / prices (app_config/services).
 * Server logic: functions/src/services.ts (reviewServicePartner, verifyPartnerTopup).
 */

const millis = (v: unknown) => (v instanceof Timestamp ? v.toMillis() : typeof v === "number" ? v : 0);
const when = (ms: number) => (ms ? new Date(ms).toLocaleString("en-IN") : "");
const rupees = (paise: number) => `₹${(paise / 100).toLocaleString("en-IN")}`;

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
  return R * c;
}

type Row = Record<string, unknown> & { id: string };
type Tab = "partners" | "topups" | "bookings" | "catalog" | "settings";

const CATEGORY_IDS = [
  "CLEANING",
  "ELECTRICIAN",
  "PLUMBER",
  "CARPENTER",
  "PAINTER",
  "HOME_HELP",
  "COOK_MAID",
  "BABYSITTER"
];

function useLive(path: string, order: string, max = 200): Row[] {
  const services = useMemo(() => getFirebaseServices(), []);
  const [rows, setRows] = useState<Row[]>([]);
  useEffect(() => {
    if (!services) return;
    const q = query(collection(services.db, path), orderBy(order, "desc"), limit(max));
    return onSnapshot(q, (snap) => setRows(snap.docs.map((d) => ({ id: d.id, ...d.data() }))), () => setRows([]));
  }, [services, path, order, max]);
  return rows;
}

export function AdminServicesClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [tab, setTab] = useState<Tab>("partners");
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reasons, setReasons] = useState<Record<string, string>>({});
  // Categories the admin has verified for each partner (unticked skilled ones are not approved).
  const [checked, setChecked] = useState<Record<string, string[]>>({});

  const partners = useLive("service_partners", "updatedAt");
  const topups = useLive("partner_topups", "createdAt");
  const bookings = useLive("service_bookings", "createdAt", 300);

  const [bookingFilter, setBookingFilter] = useState<"ALL" | "TODAY" | "PAYMENT_PENDING" | "ACTIVE" | "COMPLETED" | "CANCELLED">("ALL");
  const [partnerSearch, setPartnerSearch] = useState("");
  const [partnerStatusFilter, setPartnerStatusFilter] = useState<"ALL" | "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED">("ALL");
  const [partnerPage, setPartnerPage] = useState(1);
  const PARTNER_PAGE_SIZE = 25;

  const [assigningBooking, setAssigningBooking] = useState<Row | null>(null);
  const [assignSearch, setAssignSearch] = useState("");
  const [assignFilter, setAssignFilter] = useState<"ALL" | "MATCHING" | "ONLINE">("MATCHING");
  const [assignLoading, setAssignLoading] = useState(false);

  const startOfTodayMs = useMemo(() => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d.getTime();
  }, []);

  const bookingAnalytics = useMemo(() => {
    const total = bookings.length;
    const today = bookings.filter((b) => millis(b.createdAt) >= startOfTodayMs).length;
    const pendingPayment = bookings.filter((b) => b.status === "PAYMENT_PENDING").length;
    const completed = bookings.filter((b) => b.status === "COMPLETED");
    const active = bookings.filter((b) => ["SEARCHING", "ASSIGNED", "ON_THE_WAY", "STARTED"].includes(String(b.status)));
    const cancelled = bookings.filter((b) => b.status === "CANCELLED" || b.status === "NO_PARTNER");
    const fillRate = total > 0 ? Math.round((completed.length / Math.max(1, total - active.length)) * 100) : 0;
    const totalGMV = completed.reduce((sum, b) => sum + Number(b.total || 0), 0);
    const platformRevenue = Math.round(completed.reduce((sum, b) => sum + (Number(b.platformTakePaise || 0) / 100), 0));
    const rated = bookings.filter((b) => Number(b.rating || 0) > 0);
    const avgRating = rated.length > 0 ? (rated.reduce((sum, b) => sum + Number(b.rating || 0), 0) / rated.length).toFixed(1) : "–";

    // Category breakdown
    const categoryCounts: Record<string, { count: number; gmv: number }> = {};
    for (const b of bookings) {
      const cat = String(b.category || "OTHER");
      if (!categoryCounts[cat]) categoryCounts[cat] = { count: 0, gmv: 0 };
      categoryCounts[cat].count += 1;
      if (b.status === "COMPLETED") categoryCounts[cat].gmv += Number(b.total || 0);
    }

    return {
      total,
      today,
      pendingPayment,
      completed: completed.length,
      active: active.length,
      cancelled: cancelled.length,
      fillRate,
      totalGMV,
      platformRevenue,
      rated: rated.length,
      avgRating,
      categoryCounts
    };
  }, [bookings, startOfTodayMs]);

  const filteredBookings = useMemo(() => {
    if (bookingFilter === "TODAY") return bookings.filter((b) => millis(b.createdAt) >= startOfTodayMs);
    if (bookingFilter === "PAYMENT_PENDING") return bookings.filter((b) => b.status === "PAYMENT_PENDING");
    if (bookingFilter === "ACTIVE") return bookings.filter((b) => ["SEARCHING", "ASSIGNED", "ON_THE_WAY", "STARTED"].includes(String(b.status)));
    if (bookingFilter === "COMPLETED") return bookings.filter((b) => b.status === "COMPLETED");
    if (bookingFilter === "CANCELLED") return bookings.filter((b) => b.status === "CANCELLED" || b.status === "NO_PARTNER");
    return bookings;
  }, [bookings, bookingFilter, startOfTodayMs]);

  const last24hMs = useMemo(() => Date.now() - 24 * 60 * 60 * 1000, []);

  const partnerAnalytics = useMemo(() => {
    const approved = partners.filter((p) => p.status === "APPROVED");
    const working = approved.filter((p) => !!p.activeBookingId);
    const available = approved.filter((p) => p.online && !p.activeBookingId);
    const offline = approved.filter((p) => !p.online);
    return {
      total: partners.length,
      approved: approved.length,
      working: working.length,
      available: available.length,
      offline: offline.length,
      workingPartners: working,
    };
  }, [partners]);

  const filteredPartners = useMemo(() => {
    let list = [...partners].sort((a, b) => (a.status === "PENDING" ? -1 : 0) - (b.status === "PENDING" ? -1 : 0));
    if (partnerStatusFilter !== "ALL") {
      list = list.filter((p) => p.status === partnerStatusFilter);
    }
    const q = partnerSearch.trim().toLowerCase();
    if (q) {
      list = list.filter((p) =>
        String(p.name || "").toLowerCase().includes(q) ||
        String(p.phone || "").toLowerCase().includes(q) ||
        String(p.area || "").toLowerCase().includes(q) ||
        ((p.categories || []) as string[]).some((c) => String(c).toLowerCase().includes(q))
      );
    }
    return list;
  }, [partners, partnerStatusFilter, partnerSearch]);

  const totalPartnerPages = Math.max(1, Math.ceil(filteredPartners.length / PARTNER_PAGE_SIZE));
  const currentPartnerPage = Math.min(partnerPage, totalPartnerPages);
  const paginatedPartners = useMemo(() => {
    const start = (currentPartnerPage - 1) * PARTNER_PAGE_SIZE;
    return filteredPartners.slice(start, start + PARTNER_PAGE_SIZE);
  }, [filteredPartners, currentPartnerPage]);

  const stats24h = useMemo(() => {
    const list = bookings.filter((b) => millis(b.createdAt) >= last24hMs);
    const completed = list.filter((b) => b.status === "COMPLETED");
    const active = list.filter((b) => ["SEARCHING", "ASSIGNED", "ON_THE_WAY", "STARTED"].includes(String(b.status)));
    const gmv = completed.reduce((sum, b) => sum + Number(b.total || 0), 0);
    return {
      total: list.length,
      completed: completed.length,
      active: active.length,
      gmv,
    };
  }, [bookings, last24hMs]);

  const [showBookModal, setShowBookModal] = useState(false);
  const [bookForm, setBookForm] = useState({
    customerName: "",
    customerPhone: "",
    category: "CLEANING",
    serviceName: "Room Cleaning & Mopping",
    addressText: "",
    area: "Wyra Road",
    partnerId: "",
    note: "",
  });
  const [bookedResult, setBookedResult] = useState<{ bookingId: string; startOtp: string } | null>(null);
  const [submittingBooking, setSubmittingBooking] = useState(false);

  async function handleAdminCreateBooking(e: React.FormEvent) {
    e.preventDefault();
    if (!services) return;
    if (!bookForm.customerName.trim() || !bookForm.customerPhone.trim() || !bookForm.addressText.trim()) {
      alert("Please fill in customer name, phone number, and address.");
      return;
    }
    setSubmittingBooking(true);
    try {
      const call = httpsCallable(services.functions, "adminCreateServiceBooking");
      const res = await call({
        customerName: bookForm.customerName.trim(),
        customerPhone: bookForm.customerPhone.trim(),
        category: bookForm.category,
        serviceId: bookForm.serviceName.toLowerCase().replace(/[^a-z0-9]/g, "_"),
        addressText: bookForm.addressText.trim(),
        area: bookForm.area.trim(),
        partnerId: bookForm.partnerId || undefined,
        note: bookForm.note.trim(),
      });
      const data = res.data as { ok: boolean; bookingId: string; startOtp: string };
      setBookedResult(data);
      setMessage(`Booking created successfully! Order ID: ${data.bookingId}, Start OTP: ${data.startOtp}`);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to create booking");
    } finally {
      setSubmittingBooking(false);
    }
  }

  async function run(label: string, fn: () => Promise<unknown>) {
    setMessage(label);
    setError(null);
    try {
      await fn();
      setMessage("Done.");
    } catch (e: unknown) {
      setMessage(null);
      setError(e instanceof Error ? e.message : "Failed");
    }
  }

  function review(p: Row, action: "approve" | "reject" | "suspend") {
    if (!services) return;
    const reason = (reasons[p.id] || "").trim();
    if (action !== "approve" && !reason) {
      alert("Write a reason first (the partner sees it).");
      return;
    }
    const requested = (p.categories || []) as string[];
    const skilled = (p.skilledCategories || []) as string[];
    // By default only BASIC categories are approved; skilled ones need the admin's tick after the skill check.
    const approved = checked[p.id] ?? requested.filter((c) => !skilled.includes(c));
    if (action === "approve" && !approved.length) {
      alert("Tick at least one category you verified.");
      return;
    }
    const what = action === "approve" ? ` for ${approved.join(", ")}` : "";
    if (!confirm(`${action.toUpperCase()} ${String(p.name)} (${String(p.phone)})${what}?`)) return;
    void run("Saving...", () => httpsCallable(services.functions, "reviewServicePartner")({
      partnerId: p.id, action, reason, ...(action === "approve" ? { categories: approved } : {}),
    }));
  }

  function verify(t: Row, approve: boolean) {
    if (!services) return;
    const reason = (reasons[t.id] || "").trim();
    if (!approve && !reason) {
      alert("Write a reason first (the partner sees it).");
      return;
    }
    const ok = approve ?
      confirm(`Did you receive ${rupees(Number(t.amountPaise))} with UTR ${String(t.utrNumber)}? Credits will be added.`) :
      confirm(`Reject UTR ${String(t.utrNumber)}?`);
    if (!ok) return;
    void run("Saving...", () => httpsCallable(services.functions, "verifyPartnerTopup")({ topupId: t.id, approve, reason }));
  }

  function approveBooking(b: Row) {
    if (!services) return;
    if (!confirm(`Approve platform fee payment for booking ${b.id.slice(0, 8)} (${String(b.serviceName)}) and dispatch to nearby technicians?`)) return;
    void run("Activating booking...", () => httpsCallable(services.functions, "approveBookingPayment")({ bookingId: b.id }));
  }

  const approvedPartners = useMemo(() => {
    return partners.filter((p) => p.status === "APPROVED");
  }, [partners]);

  const filteredAssignPartners = useMemo(() => {
    if (!assigningBooking) return [];
    const bookingCat = String(assigningBooking.category || "").toUpperCase();
    let list = approvedPartners;

    if (assignFilter === "MATCHING") {
      list = list.filter((p) => {
        const cats = ((p.categories || []) as string[]).map((c) => c.toUpperCase());
        return cats.includes("ALL") || cats.includes(bookingCat);
      });
    } else if (assignFilter === "ONLINE") {
      list = list.filter((p) => !!p.online);
    }

    if (assignSearch.trim()) {
      const q = assignSearch.trim().toLowerCase();
      list = list.filter((p) =>
        String(p.name || "").toLowerCase().includes(q) ||
        String(p.phone || "").toLowerCase().includes(q) ||
        String(p.area || "").toLowerCase().includes(q) ||
        ((p.categories || []) as string[]).some((c) => String(c).toLowerCase().includes(q))
      );
    }

    return [...list].sort((a, b) => {
      const aCats = ((a.categories || []) as string[]).map((c) => c.toUpperCase());
      const bCats = ((b.categories || []) as string[]).map((c) => c.toUpperCase());
      const aMatches = aCats.includes("ALL") || aCats.includes(bookingCat) ? 1 : 0;
      const bMatches = bCats.includes("ALL") || bCats.includes(bookingCat) ? 1 : 0;
      if (bMatches !== aMatches) return bMatches - aMatches;
      const aOnline = a.online ? 1 : 0;
      const bOnline = b.online ? 1 : 0;
      if (bOnline !== aOnline) return bOnline - aOnline;
      return Number(b.jobsCompleted || 0) - Number(a.jobsCompleted || 0);
    });
  }, [approvedPartners, assigningBooking, assignFilter, assignSearch]);

  async function assignPartner(bookingId: string, partnerId: string, partnerName: string) {
    if (!services) return;
    if (!confirm(`Assign approved partner "${partnerName}" to booking #${bookingId.slice(-6)}?`)) return;
    setAssignLoading(true);
    try {
      const call = httpsCallable(services.functions, "adminAssignBookingPartner");
      await call({ bookingId, partnerId });
      setMessage(`Successfully assigned ${partnerName} to booking #${bookingId.slice(-6)}!`);
      setAssigningBooking(null);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to assign partner");
    } finally {
      setAssignLoading(false);
    }
  }

  const pendingPartners = partners.filter((p) => p.status === "PENDING").length;
  const pendingTopups = topups.filter((t) => t.status === "PENDING").length;
  const openBookings = bookings.filter((b) => ["SEARCHING", "ASSIGNED", "ON_THE_WAY", "STARTED"].includes(String(b.status))).length;

  return (
    <div className="admin-section-stack">
      {message && <div className="alert-banner info">{message}</div>}
      {error && <div className="alert-banner error">{error}</div>}

      <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 16 }}>
        {([
          ["partners", `Partners (${pendingPartners} pending)`],
          ["topups", `Credit top-ups (${pendingTopups} pending)`],
          ["bookings", `Bookings (${openBookings} open)`],
          ["catalog", "📦 Categories & Services"],
          ["settings", "Prices & settings"],
        ] as Array<[Tab, string]>).map(([id, label]) => (
          <button
            key={id}
            onClick={() => { setTab(id); setMessage(null); setError(null); }}
            className="btn"
            style={{ fontWeight: tab === id ? 700 : 400, borderBottom: tab === id ? "2px solid #2563eb" : "2px solid transparent" }}
          >
            {label}
          </button>
        ))}
        <a
          href="/admin/service-catalog"
          className="btn"
          style={{
            marginLeft: "auto",
            display: "inline-flex",
            alignItems: "center",
            gap: 6,
            background: "#7c3aed",
            color: "#fff",
            textDecoration: "none",
            fontWeight: 600,
            borderRadius: 6,
            padding: "6px 14px"
          }}
        >
          ✨ 3D Clay Icons &amp; Catalog Manager ↗
        </a>
      </div>

      {/* Real-time Worker Duty & 24h Activity Metrics (Requirements 4 & 5) */}
      <div style={{ background: "#0f172a", borderRadius: "12px", padding: "16px 20px", marginBottom: "20px", color: "#fff" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 12, marginBottom: 14 }}>
          <div>
            <h3 style={{ margin: 0, fontSize: "16px", fontWeight: 800, color: "#fff", display: "flex", alignItems: "center", gap: 8 }}>
              <span>📍 Khammam Services Operations Radar</span>
              <span style={{ fontSize: "11px", fontWeight: 600, background: "#059669", color: "#fff", padding: "2px 8px", borderRadius: "12px" }}>LIVE 24/7</span>
            </h3>
            <p style={{ margin: "4px 0 0", fontSize: "12.5px", color: "#94a3b8" }}>
              Monitor live domestic workers on duty, 24h booking volume, and directly assign orders to hired staff.
            </p>
          </div>
          <button
            type="button"
            onClick={() => { setShowBookModal(true); setBookedResult(null); }}
            style={{
              background: "#059669",
              color: "#fff",
              border: "none",
              padding: "9px 18px",
              borderRadius: "8px",
              fontWeight: 700,
              fontSize: "13px",
              cursor: "pointer",
              display: "inline-flex",
              alignItems: "center",
              gap: "6px",
              boxShadow: "0 2px 8px rgba(5, 150, 105, 0.4)"
            }}
          >
            ➕ Book Service for Customer
          </button>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(130px, 1fr))", gap: "10px" }}>
          {/* Card 1: Working on Job */}
          <div style={{ background: "rgba(16, 185, 129, 0.12)", border: "1px solid rgba(16, 185, 129, 0.3)", borderRadius: "10px", padding: "12px" }}>
            <div style={{ fontSize: "11px", color: "#6ee7b7", fontWeight: 700, textTransform: "uppercase" }}>🟢 WORKING ON JOB</div>
            <div style={{ fontSize: "22px", fontWeight: 800, color: "#34d399", marginTop: "2px" }}>{partnerAnalytics.working}</div>
            <div style={{ fontSize: "11px", color: "#a7f3d0", marginTop: "2px" }}>In progress right now</div>
          </div>

          {/* Card 2: Available Online */}
          <div style={{ background: "rgba(245, 158, 11, 0.12)", border: "1px solid rgba(245, 158, 11, 0.3)", borderRadius: "10px", padding: "12px" }}>
            <div style={{ fontSize: "11px", color: "#fcd34d", fontWeight: 700, textTransform: "uppercase" }}>🟡 AVAILABLE ONLINE</div>
            <div style={{ fontSize: "22px", fontWeight: 800, color: "#fbbf24", marginTop: "2px" }}>{partnerAnalytics.available}</div>
            <div style={{ fontSize: "11px", color: "#fde68a", marginTop: "2px" }}>Duty ON in Khammam</div>
          </div>

          {/* Card 3: Off-duty / Not Working */}
          <div style={{ background: "rgba(148, 163, 184, 0.12)", border: "1px solid rgba(148, 163, 184, 0.3)", borderRadius: "10px", padding: "12px" }}>
            <div style={{ fontSize: "11px", color: "#cbd5e1", fontWeight: 700, textTransform: "uppercase" }}>⚪ OFF-DUTY / IDLE</div>
            <div style={{ fontSize: "22px", fontWeight: 800, color: "#e2e8f0", marginTop: "2px" }}>{partnerAnalytics.offline}</div>
            <div style={{ fontSize: "11px", color: "#94a3b8", marginTop: "2px" }}>Duty OFF</div>
          </div>

          {/* Card 4: 24h Orders */}
          <div style={{ background: "rgba(59, 130, 246, 0.12)", border: "1px solid rgba(59, 130, 246, 0.3)", borderRadius: "10px", padding: "12px" }}>
            <div style={{ fontSize: "11px", color: "#93c5fd", fontWeight: 700, textTransform: "uppercase" }}>⏱️ 24H BOOKINGS</div>
            <div style={{ fontSize: "22px", fontWeight: 800, color: "#60a5fa", marginTop: "2px" }}>{stats24h.total}</div>
            <div style={{ fontSize: "11px", color: "#bfdbfe", marginTop: "2px" }}>{stats24h.completed} completed</div>
          </div>

          {/* Card 5: 24h Volume */}
          <div style={{ background: "rgba(168, 85, 247, 0.12)", border: "1px solid rgba(168, 85, 247, 0.3)", borderRadius: "10px", padding: "12px" }}>
            <div style={{ fontSize: "11px", color: "#d8b4fe", fontWeight: 700, textTransform: "uppercase" }}>💰 24H GMV</div>
            <div style={{ fontSize: "22px", fontWeight: 800, color: "#c084fc", marginTop: "2px" }}>₹{stats24h.gmv.toLocaleString("en-IN")}</div>
            <div style={{ fontSize: "11px", color: "#e9d5ff", marginTop: "2px" }}>Delivered order value</div>
          </div>

          {/* Card 6: Total Approved */}
          <div style={{ background: "rgba(255, 255, 255, 0.06)", border: "1px solid rgba(255, 255, 255, 0.15)", borderRadius: "10px", padding: "12px" }}>
            <div style={{ fontSize: "11px", color: "#cbd5e1", fontWeight: 700, textTransform: "uppercase" }}>👥 APPROVED STAFF</div>
            <div style={{ fontSize: "22px", fontWeight: 800, color: "#fff", marginTop: "2px" }}>{partnerAnalytics.approved}</div>
            <div style={{ fontSize: "11px", color: "#94a3b8", marginTop: "2px" }}>of {partnerAnalytics.total} registered</div>
          </div>
        </div>

        {/* Live List of Workers on Job */}
        {partnerAnalytics.workingPartners.length > 0 && (
          <div style={{ marginTop: 14, paddingTop: 12, borderTop: "1px solid rgba(255, 255, 255, 0.12)" }}>
            <div style={{ fontSize: "12px", fontWeight: 700, color: "#34d399", marginBottom: 6 }}>
              🟢 Workers Currently on Active Duty ({partnerAnalytics.workingPartners.length}):
            </div>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              {partnerAnalytics.workingPartners.map((wp) => (
                <span
                  key={wp.id}
                  style={{
                    background: "rgba(255, 255, 255, 0.08)",
                    border: "1px solid rgba(16, 185, 129, 0.4)",
                    borderRadius: "6px",
                    padding: "4px 10px",
                    fontSize: "12px",
                    color: "#fff",
                    display: "inline-flex",
                    alignItems: "center",
                    gap: 6
                  }}
                >
                  <strong>{String(wp.name)}</strong> ({String(wp.phone)}) · Order: #{String(wp.activeBookingId).slice(-6)}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      {tab === "partners" && (
        <section className="admin-section">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 12 }}>
            <div>
              <h2 className="admin-section-title" style={{ margin: 0 }}>Service partners</h2>
              <div style={{ fontSize: 13, color: "#64748b", marginTop: 4 }}>
                Showing {filteredPartners.length === 0 ? 0 : (currentPartnerPage - 1) * PARTNER_PAGE_SIZE + 1}–{Math.min(currentPartnerPage * PARTNER_PAGE_SIZE, filteredPartners.length)} of {filteredPartners.length} partners (25 per page)
              </div>
            </div>

            {/* Search & Filter Controls */}
            <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
              <input
                type="text"
                placeholder="🔍 Search name, phone, area..."
                value={partnerSearch}
                onChange={(e) => {
                  setPartnerSearch(e.target.value);
                  setPartnerPage(1);
                }}
                style={{
                  padding: "7px 12px",
                  borderRadius: 8,
                  border: "1px solid #cbd5e1",
                  fontSize: 13,
                  minWidth: 220
                }}
              />
              <select
                value={partnerStatusFilter}
                onChange={(e) => {
                  setPartnerStatusFilter(e.target.value as any);
                  setPartnerPage(1);
                }}
                style={{
                  padding: "7px 12px",
                  borderRadius: 8,
                  border: "1px solid #cbd5e1",
                  fontSize: 13,
                  background: "#fff"
                }}
              >
                <option value="ALL">All Statuses ({partners.length})</option>
                <option value="PENDING">Pending Approval ({partners.filter(p => p.status === "PENDING").length})</option>
                <option value="APPROVED">Approved ({partners.filter(p => p.status === "APPROVED").length})</option>
                <option value="REJECTED">Rejected ({partners.filter(p => p.status === "REJECTED").length})</option>
                <option value="SUSPENDED">Suspended ({partners.filter(p => p.status === "SUSPENDED").length})</option>
              </select>

              {/* Top Pagination Buttons */}
              <div style={{ display: "inline-flex", alignItems: "center", gap: 4, marginLeft: 4 }}>
                <button
                  type="button"
                  disabled={currentPartnerPage <= 1}
                  onClick={() => setPartnerPage(p => Math.max(1, p - 1))}
                  style={{
                    padding: "6px 12px",
                    borderRadius: 6,
                    border: "1px solid #cbd5e1",
                    background: currentPartnerPage <= 1 ? "#f1f5f9" : "#fff",
                    color: currentPartnerPage <= 1 ? "#94a3b8" : "#0f172a",
                    cursor: currentPartnerPage <= 1 ? "not-allowed" : "pointer",
                    fontSize: 12,
                    fontWeight: 600
                  }}
                >
                  ◀ Prev
                </button>
                <span style={{ fontSize: 12, color: "#475569", fontWeight: 600, padding: "0 6px" }}>
                  {currentPartnerPage} / {totalPartnerPages}
                </span>
                <button
                  type="button"
                  disabled={currentPartnerPage >= totalPartnerPages}
                  onClick={() => setPartnerPage(p => Math.min(totalPartnerPages, p + 1))}
                  style={{
                    padding: "6px 12px",
                    borderRadius: 6,
                    border: "1px solid #cbd5e1",
                    background: currentPartnerPage >= totalPartnerPages ? "#f1f5f9" : "#fff",
                    color: currentPartnerPage >= totalPartnerPages ? "#94a3b8" : "#0f172a",
                    cursor: currentPartnerPage >= totalPartnerPages ? "not-allowed" : "pointer",
                    fontSize: 12,
                    fontWeight: 600
                  }}
                >
                  Next ▶
                </button>
              </div>
            </div>
          </div>

          <p style={{ marginTop: 12 }}>
            Call each applicant and check Aadhaar. <b>Basic</b> work (cleaning, home help, car wash) needs only ID and a polite call.
            <b>Skilled</b> work (⚡ marked) needs a skill check before you tick it: ask 3 practical questions on the phone
            (e.g. electrician: &quot;MCB keeps tripping – what do you check first?&quot;), ask for photos/videos of past work or an
            ITI / shop reference, and if unsure give a trial job at your own place. Untick categories you could not verify.
          </p>
          <div className="admin-table-container">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Partner</th>
                  <th>Location &amp; GPS Trace</th>
                  <th>Services</th>
                  <th>Status</th>
                  <th>Credits</th>
                  <th>Jobs / rating</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {paginatedPartners.map((p) => {
                  const count = Number(p.ratingCount || 0);
                  const pLat = typeof p.lat === "number" ? p.lat : undefined;
                  const pLng = typeof p.lng === "number" ? p.lng : undefined;
                  const distKm = calcDistanceKm(pLat, pLng);
                  const isInsideZone = distKm !== null && distKm <= 15.0;
                  return (
                    <tr key={p.id}>
                      <td>
                        <strong>{String(p.name || "")}</strong>
                        <div><a href={`tel:${String(p.phone || "")}`}>{String(p.phone || "")}</a></div>
                        <div>{String(p.experienceYears ?? 0)} yrs experience</div>
                        <div style={{ fontSize: 12, opacity: 0.7 }}>Applied {when(millis(p.appliedAt))}</div>
                        {p.note ? (
                          <div style={{ fontSize: 11, color: "#475569", marginTop: 4, background: "#f1f5f9", padding: "2px 6px", borderRadius: 4 }}>
                            📝 {String(p.note)}
                          </div>
                        ) : null}
                      </td>
                      <td style={{ minWidth: 190 }}>
                        <div style={{ fontWeight: 600, color: "#0f172a", fontSize: 13 }}>
                          📍 {String(p.area || "Area not specified")}
                        </div>
                        {pLat !== undefined && pLng !== undefined ? (
                          <div style={{ marginTop: 3 }}>
                            <div style={{ fontSize: 11, color: "#64748b", fontFamily: "monospace" }}>
                              {pLat.toFixed(4)}, {pLng.toFixed(4)}
                            </div>
                            <div style={{ marginTop: 4 }}>
                              {isInsideZone ? (
                                <span style={{
                                  display: "inline-flex",
                                  alignItems: "center",
                                  gap: 4,
                                  fontSize: 11,
                                  fontWeight: 700,
                                  background: "#ecfdf5",
                                  color: "#059669",
                                  border: "1px solid #a7f3d0",
                                  borderRadius: 4,
                                  padding: "2px 6px"
                                }}>
                                  ✓ Khammam ({distKm?.toFixed(1)} km)
                                </span>
                              ) : (
                                <span style={{
                                  display: "inline-flex",
                                  alignItems: "center",
                                  gap: 4,
                                  fontSize: 11,
                                  fontWeight: 700,
                                  background: "#fef2f2",
                                  color: "#dc2626",
                                  border: "1px solid #fecaca",
                                  borderRadius: 4,
                                  padding: "2px 6px"
                                }}>
                                  ⚠️ Outside Zone ({distKm?.toFixed(1)} km)
                                </span>
                              )}
                            </div>
                            <div style={{ marginTop: 4 }}>
                              <a
                                href={`https://www.google.com/maps?q=${pLat},${pLng}`}
                                target="_blank"
                                rel="noopener noreferrer"
                                style={{
                                  display: "inline-flex",
                                  alignItems: "center",
                                  gap: 3,
                                  fontSize: 11.5,
                                  fontWeight: 600,
                                  color: "#2563eb",
                                  textDecoration: "none"
                                }}
                              >
                                🗺️ View on Maps ↗
                              </a>
                            </div>
                          </div>
                        ) : (
                          <span style={{ fontSize: 11, color: "#94a3b8", display: "inline-block", marginTop: 4 }}>
                            No GPS captured
                          </span>
                        )}
                      </td>
                      <td>
                        {(() => {
                          const isApproved = p.status === "APPROVED";
                          const partnerCats = (p.categories || []) as string[];
                          const list = checked[p.id] ?? (isApproved ? partnerCats : partnerCats.filter((x) => !((p.skilledCategories || []) as string[]).includes(x)));
                          const allAvailableToToggle = Array.from(new Set([...partnerCats, ...list]));
                          return (
                            <>
                              {allAvailableToToggle.map((c) => {
                                const skilled = ((p.skilledCategories || []) as string[]).includes(c);
                                return (
                                  <label key={c} style={{ display: "block", whiteSpace: "nowrap" }}>
                                    <input
                                      type="checkbox"
                                      checked={list.includes(c)}
                                      onChange={(e) => setChecked({ ...checked, [p.id]: e.target.checked ? [...list, c] : list.filter((x) => x !== c) })}
                                    />{" "}
                                    {skilled ? "⚡ " : ""}{c}
                                  </label>
                                );
                              })}
                              <div style={{ marginTop: 6 }}>
                                <select
                                  value=""
                                  onChange={(e) => {
                                    const val = e.target.value;
                                    if (val && !list.includes(val)) {
                                      setChecked({ ...checked, [p.id]: [...list, val] });
                                    }
                                  }}
                                  style={{ fontSize: 11, padding: "2px 4px", borderRadius: 4, border: "1px solid #cbd5e1", maxWidth: 130 }}
                                >
                                  <option value="">+ Add Skill</option>
                                  {CATEGORY_IDS.filter((c) => !list.includes(c)).map((c) => (
                                    <option key={c} value={c}>{c}</option>
                                  ))}
                                </select>
                              </div>
                            </>
                          );
                        })()}
                        {p.skillProof ? <div style={{ fontSize: 12, marginTop: 4 }}>Proof: {String(p.skillProof)}</div> : null}
                        {p.guidelinesAcceptedAt ? <div style={{ fontSize: 12, opacity: 0.7 }}>✓ accepted code of conduct</div> : null}
                      </td>
                      <td>{String(p.status)}{p.online ? " · 🟢 online" : ""}</td>
                      <td>{rupees(Number(p.creditsPaise || 0))}</td>
                      <td>{String(p.jobsCompleted || 0)} jobs · {count ? (Number(p.ratingSum) / count).toFixed(1) + "★" : "–"} · {String(p.cancellations || 0)} cancels</td>
                      <td>
                        <input
                          placeholder="Reason (for reject/suspend)"
                          value={reasons[p.id] || ""}
                          onChange={(e) => setReasons({ ...reasons, [p.id]: e.target.value })}
                          style={{ width: 180 }}
                        />
                        <div style={{ display: "flex", gap: 6, marginTop: 6, flexWrap: "wrap" }}>
                          {p.status !== "APPROVED" && <button className="btn btn-approve" onClick={() => review(p, "approve")}>Approve</button>}
                          {p.status === "APPROVED" && (
                            <button
                              className="btn"
                              style={{ background: "#0284c7", color: "#fff", borderColor: "#0284c7" }}
                              onClick={() => review(p, "approve")}
                              title="Save verified skills"
                            >
                              Update Skills
                            </button>
                          )}
                          {p.status === "PENDING" && <button className="btn" onClick={() => review(p, "reject")}>Reject</button>}
                          {p.status === "APPROVED" && <button className="btn" onClick={() => review(p, "suspend")}>Suspend</button>}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Bottom Pagination Bar */}
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 14, padding: "8px 4px", flexWrap: "wrap", gap: 8 }}>
            <div style={{ fontSize: 13, color: "#64748b" }}>
              Showing Page {currentPartnerPage} of {totalPartnerPages} ({filteredPartners.length} total partners)
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <button
                type="button"
                disabled={currentPartnerPage <= 1}
                onClick={() => {
                  setPartnerPage(p => Math.max(1, p - 1));
                  window.scrollTo({ top: 400, behavior: "smooth" });
                }}
                style={{
                  padding: "6px 14px",
                  borderRadius: 6,
                  border: "1px solid #cbd5e1",
                  background: currentPartnerPage <= 1 ? "#f1f5f9" : "#fff",
                  color: currentPartnerPage <= 1 ? "#94a3b8" : "#0f172a",
                  cursor: currentPartnerPage <= 1 ? "not-allowed" : "pointer",
                  fontSize: 13,
                  fontWeight: 600
                }}
              >
                ◀ Previous 25
              </button>
              <button
                type="button"
                disabled={currentPartnerPage >= totalPartnerPages}
                onClick={() => {
                  setPartnerPage(p => Math.min(totalPartnerPages, p + 1));
                  window.scrollTo({ top: 400, behavior: "smooth" });
                }}
                style={{
                  padding: "6px 14px",
                  borderRadius: 6,
                  border: "1px solid #cbd5e1",
                  background: currentPartnerPage >= totalPartnerPages ? "#f1f5f9" : "#fff",
                  color: currentPartnerPage >= totalPartnerPages ? "#94a3b8" : "#0f172a",
                  cursor: currentPartnerPage >= totalPartnerPages ? "not-allowed" : "pointer",
                  fontSize: 13,
                  fontWeight: 600
                }}
              >
                Next 25 ▶
              </button>
            </div>
          </div>
        </section>
      )}

      {tab === "topups" && (
        <section className="admin-section">
          <h2 className="admin-section-title">Partner credit top-ups</h2>
          <p>Check the UTR in your UPI / bank app before approving. Approving adds the amount to the partner&apos;s credits.</p>
          <div className="admin-table-container">
            <table className="admin-table">
              <thead><tr><th>Partner</th><th>Amount</th><th>UTR</th><th>Status</th><th>Action</th></tr></thead>
              <tbody>
                {topups.map((t) => (
                  <tr key={t.id}>
                    <td>{String(t.partnerName || t.partnerId)}<div style={{ fontSize: 12, opacity: 0.7 }}>{when(millis(t.createdAt))}</div></td>
                    <td>{rupees(Number(t.amountPaise || 0))}</td>
                    <td><code>{String(t.utrNumber)}</code></td>
                    <td>{String(t.status)}{t.rejectionReason ? ` · ${String(t.rejectionReason)}` : ""}</td>
                    <td>
                      {t.status === "PENDING" && (
                        <>
                          <input
                            placeholder="Reason (for reject)"
                            value={reasons[t.id] || ""}
                            onChange={(e) => setReasons({ ...reasons, [t.id]: e.target.value })}
                            style={{ width: 160 }}
                          />
                          <div style={{ display: "flex", gap: 6, marginTop: 6 }}>
                            <button className="btn btn-approve" onClick={() => verify(t, true)}>Approve</button>
                            <button className="btn" onClick={() => verify(t, false)}>Reject</button>
                          </div>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {tab === "bookings" && (
        <section className="admin-section">
          {/* Header & Filter Controls */}
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px", flexWrap: "wrap", gap: "10px" }}>
            <div>
              <h2 className="admin-section-title" style={{ margin: 0 }}>📊 Service Bookings &amp; Real-Time Analytics</h2>
              <p style={{ fontSize: "12.5px", color: "#64748b", margin: "2px 0 0" }}>Live tracking of customer requests, fill rates, GMV, and technician performance.</p>
            </div>
            <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
              {(["ALL", "TODAY", "PAYMENT_PENDING", "ACTIVE", "COMPLETED", "CANCELLED"] as const).map((f) => (
                <button
                  key={f}
                  type="button"
                  onClick={() => setBookingFilter(f)}
                  style={{
                    padding: "5px 12px",
                    borderRadius: "20px",
                    border: "1px solid",
                    borderColor: bookingFilter === f ? "#087f68" : "#cbd5e1",
                    background: bookingFilter === f ? "#087f68" : "#fff",
                    color: bookingFilter === f ? "#fff" : "#475569",
                    fontSize: "12px",
                    fontWeight: 600,
                    cursor: "pointer",
                    transition: "all 0.15s ease"
                  }}
                >
                  {f === "ALL" ? `All (${bookings.length})` :
                   f === "TODAY" ? `Today (${bookingAnalytics.today})` :
                   f === "PAYMENT_PENDING" ? `Pending Approval (${bookingAnalytics.pendingPayment})` :
                   f === "ACTIVE" ? `Active (${bookingAnalytics.active})` : f}
                </button>
              ))}
            </div>
          </div>

          {/* KPI Analytics Cards */}
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))", gap: "10px", marginBottom: "20px" }}>
            <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: "10px", padding: "12px" }}>
              <div style={{ fontSize: "11px", color: "#64748b", fontWeight: 700, textTransform: "uppercase" }}>TOTAL BOOKINGS</div>
              <div style={{ fontSize: "20px", fontWeight: 800, color: "#0f172a", marginTop: "2px" }}>{bookingAnalytics.total}</div>
              <div style={{ fontSize: "11px", color: "#059669", marginTop: "2px", fontWeight: 600 }}>🟢 {bookingAnalytics.today} today</div>
            </div>

            <div style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "10px", padding: "12px" }}>
              <div style={{ fontSize: "11px", color: "#166534", fontWeight: 700, textTransform: "uppercase" }}>FILL RATE</div>
              <div style={{ fontSize: "20px", fontWeight: 800, color: "#166534", marginTop: "2px" }}>{bookingAnalytics.fillRate}%</div>
              <div style={{ fontSize: "11px", color: "#15803d", marginTop: "2px", fontWeight: 600 }}>{bookingAnalytics.completed} completed</div>
            </div>

            <div style={{ background: "#eff6ff", border: "1px solid #bfdbfe", borderRadius: "10px", padding: "12px" }}>
              <div style={{ fontSize: "11px", color: "#1e40af", fontWeight: 700, textTransform: "uppercase" }}>ACTIVE LIVE</div>
              <div style={{ fontSize: "20px", fontWeight: 800, color: "#1e40af", marginTop: "2px" }}>{bookingAnalytics.active}</div>
              <div style={{ fontSize: "11px", color: "#2563eb", marginTop: "2px", fontWeight: 600 }}>⚡ en route / in progress</div>
            </div>

            <div style={{ background: "#faf5ff", border: "1px solid #e9d5ff", borderRadius: "10px", padding: "12px" }}>
              <div style={{ fontSize: "11px", color: "#6b21a8", fontWeight: 700, textTransform: "uppercase" }}>TOTAL GMV</div>
              <div style={{ fontSize: "20px", fontWeight: 800, color: "#6b21a8", marginTop: "2px" }}>₹{bookingAnalytics.totalGMV.toLocaleString("en-IN")}</div>
              <div style={{ fontSize: "11px", color: "#7e22ce", marginTop: "2px", fontWeight: 600 }}>Gross order value</div>
            </div>

            <div style={{ background: "#fffbeb", border: "1px solid #fde68a", borderRadius: "10px", padding: "12px" }}>
              <div style={{ fontSize: "11px", color: "#92400e", fontWeight: 700, textTransform: "uppercase" }}>DUTYPE TAKE</div>
              <div style={{ fontSize: "20px", fontWeight: 800, color: "#92400e", marginTop: "2px" }}>₹{bookingAnalytics.platformRevenue.toLocaleString("en-IN")}</div>
              <div style={{ fontSize: "11px", color: "#b45309", marginTop: "2px", fontWeight: 600 }}>Platform credits cut</div>
            </div>

            <div style={{ background: "#fef2f2", border: "1px solid #fecaca", borderRadius: "10px", padding: "12px" }}>
              <div style={{ fontSize: "11px", color: "#991b1b", fontWeight: 700, textTransform: "uppercase" }}>AVG RATING</div>
              <div style={{ fontSize: "20px", fontWeight: 800, color: "#991b1b", marginTop: "2px" }}>{bookingAnalytics.avgRating} ★</div>
              <div style={{ fontSize: "11px", color: "#b91c1c", marginTop: "2px", fontWeight: 600 }}>from {bookingAnalytics.rated} reviews</div>
            </div>
          </div>

          {/* Category Share Chips */}
          {Object.keys(bookingAnalytics.categoryCounts).length > 0 && (
            <div style={{ background: "#fff", border: "1px solid #e2e8f0", borderRadius: "10px", padding: "12px 16px", marginBottom: "16px" }}>
              <div style={{ fontSize: "12px", fontWeight: 700, color: "#475569", marginBottom: "8px" }}>📦 Bookings by Category:</div>
              <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
                {Object.entries(bookingAnalytics.categoryCounts).map(([cat, data]) => (
                  <span
                    key={cat}
                    style={{
                      background: "#f1f5f9",
                      padding: "4px 10px",
                      borderRadius: "6px",
                      fontSize: "12px",
                      color: "#1e293b",
                      display: "flex",
                      alignItems: "center",
                      gap: "6px"
                    }}
                  >
                    <strong>{cat}</strong>: {data.count} bookings {data.gmv > 0 ? `(₹${data.gmv.toLocaleString("en-IN")})` : ""}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Bookings Table */}
          <div className="admin-table-container">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>When</th>
                  <th>Service</th>
                  <th>Customer</th>
                  <th>Partner</th>
                  <th>Status</th>
                  <th>Total</th>
                  <th>Offer</th>
                  <th>DutyPe Take</th>
                  <th>Rating</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredBookings.length === 0 ? (
                  <tr>
                    <td colSpan={10} style={{ textAlign: "center", padding: "24px", color: "#94a3b8" }}>
                      No bookings found for filter &apos;{bookingFilter}&apos;.
                    </td>
                  </tr>
                ) : (
                  filteredBookings.map((b) => {
                    const st = String(b.status);
                    const pillClass =
                      st === "COMPLETED"
                        ? "status-pill success"
                        : st === "SEARCHING"
                          ? "status-pill warning"
                          : st === "ASSIGNED" || st === "ON_THE_WAY" || st === "STARTED"
                            ? "status-pill"
                            : "status-pill danger";

                    return (
                      <tr key={b.id}>
                        <td>
                          <div>{when(millis(b.createdAt))}</div>
                          {b.scheduledAt ? (
                            <div style={{ fontSize: "11px", color: "#0284c7", fontWeight: 600 }}>
                              📅 Slot: {when(millis(b.scheduledAt))}
                            </div>
                          ) : null}
                        </td>
                        <td>
                          <strong>{String(b.serviceName)}</strong>
                          <div style={{ fontSize: 11.5, color: "#64748b" }}>{String(b.area || "")}</div>
                        </td>
                        <td>
                          <div>{String(b.customerName || "Customer")}</div>
                          <div style={{ fontSize: "12px" }}>
                            <a href={`tel:${String(b.customerPhone || "")}`} style={{ color: "#087f68" }}>
                              {String(b.customerPhone || "")}
                            </a>
                          </div>
                        </td>
                        <td>
                          {b.partnerName ? (
                            <div>
                              <strong style={{ color: "#0f172a" }}>{String(b.partnerName)}</strong>
                              {b.partnerPhone ? (
                                <div style={{ fontSize: "12px" }}>
                                  <a href={`tel:${String(b.partnerPhone || "")}`} style={{ color: "#0284c7" }}>
                                    📞 {String(b.partnerPhone || "")}
                                  </a>
                                </div>
                              ) : null}
                            </div>
                          ) : (
                            <button
                              type="button"
                              onClick={() => {
                                setAssigningBooking(b);
                                setAssignSearch("");
                                setAssignFilter("MATCHING");
                              }}
                              style={{
                                background: "#eff6ff",
                                color: "#2563eb",
                                border: "1px dashed #93c5fd",
                                borderRadius: "6px",
                                padding: "4px 8px",
                                fontSize: "11.5px",
                                fontWeight: 700,
                                cursor: "pointer"
                              }}
                            >
                              + Assign Partner
                            </button>
                          )}
                        </td>
                        <td>
                          <span className={pillClass} style={{ fontSize: "11px" }}>
                            {st}
                          </span>
                        </td>
                        <td>
                          <strong>₹{String(b.total ?? "")}</strong>
                        </td>
                        <td>
                          {Number(b.discount || 0) > 0 ? (
                            <span style={{ color: "#087f68", fontSize: "11.5px", fontWeight: 600 }}>
                              −₹{String(b.discount)} {String(b.couponCode || "first booking")}
                            </span>
                          ) : (
                            <span style={{ color: "#94a3b8" }}>–</span>
                          )}
                        </td>
                        <td>{b.platformTakePaise ? rupees(Number(b.platformTakePaise)) : "–"}</td>
                        <td>
                          {b.rating ? (
                            <span style={{ color: "#d97706", fontWeight: 700 }}>
                              {String(b.rating)} ★
                            </span>
                          ) : (
                            <span style={{ color: "#cbd5e1" }}>–</span>
                          )}
                        </td>
                        <td>
                          <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                            {st === "PAYMENT_PENDING" && (
                              <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                                <span style={{ fontSize: "11px", color: "#b45309", fontWeight: 600 }}>
                                  {b.utr ? `UTR: ${String(b.utr)}` : "Awaiting UTR"}
                                </span>
                                <button
                                  type="button"
                                  onClick={() => approveBooking(b)}
                                  style={{
                                    background: "#087f68",
                                    color: "#fff",
                                    border: "none",
                                    borderRadius: "6px",
                                    padding: "4px 8px",
                                    fontSize: "11px",
                                    fontWeight: 600,
                                    cursor: "pointer",
                                    whiteSpace: "nowrap"
                                  }}
                                >
                                  Approve &amp; Go Live
                                </button>
                              </div>
                            )}
                            {st !== "COMPLETED" && st !== "CANCELLED" && (
                              <button
                                type="button"
                                onClick={() => {
                                  setAssigningBooking(b);
                                  setAssignSearch("");
                                  setAssignFilter("MATCHING");
                                }}
                                style={{
                                  background: b.partnerId ? "#f8fafc" : "#0284c7",
                                  color: b.partnerId ? "#0284c7" : "#fff",
                                  border: b.partnerId ? "1px solid #bae6fd" : "none",
                                  borderRadius: "6px",
                                  padding: "5px 10px",
                                  fontSize: "11.5px",
                                  fontWeight: 700,
                                  cursor: "pointer",
                                  whiteSpace: "nowrap",
                                  display: "inline-flex",
                                  alignItems: "center",
                                  gap: "4px"
                                }}
                              >
                                👤 {b.partnerId ? "Reassign Partner" : "Assign Partner"}
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}



      {tab === "catalog" && (
        <AdminCategoriesServicesManager
          onSaved={(m) => setMessage(m)}
          onError={(m) => setError(m)}
        />
      )}

      {tab === "settings" && <ServicesSettings onSaved={(m) => setMessage(m)} onError={(m) => setError(m)} />}

      {/* Modal: Assign Approved Partner with Skills to Booking */}
      {assigningBooking && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(15, 23, 42, 0.7)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 9999,
            padding: 16,
            backdropFilter: "blur(4px)"
          }}
          onClick={(e) => {
            if (e.target === e.currentTarget && !assignLoading) setAssigningBooking(null);
          }}
        >
          <div
            style={{
              background: "#fff",
              borderRadius: "16px",
              padding: "24px",
              maxWidth: "680px",
              width: "100%",
              maxHeight: "90vh",
              overflowY: "auto",
              boxShadow: "0 25px 50px -12px rgba(0, 0, 0, 0.25)",
              display: "flex",
              flexDirection: "column",
              gap: "16px"
            }}
          >
            {/* Modal Header */}
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
              <div>
                <h3 style={{ margin: 0, fontSize: "18px", fontWeight: 800, color: "#0f172a" }}>
                  👤 Assign Approved Partner
                </h3>
                <p style={{ margin: "4px 0 0", fontSize: "13px", color: "#64748b" }}>
                  Select an approved technician with verified skills for this booking.
                </p>
              </div>
              <button
                type="button"
                disabled={assignLoading}
                onClick={() => setAssigningBooking(null)}
                style={{
                  background: "none",
                  border: "none",
                  fontSize: "22px",
                  cursor: assignLoading ? "not-allowed" : "pointer",
                  color: "#64748b"
                }}
              >
                ✕
              </button>
            </div>

            {/* Booking Details Summary Card */}
            <div
              style={{
                background: "#f8fafc",
                border: "1px solid #e2e8f0",
                borderRadius: "12px",
                padding: "14px 16px"
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 8 }}>
                <div>
                  <div style={{ fontSize: "14.5px", fontWeight: 700, color: "#0f172a" }}>
                    {String(assigningBooking.serviceName)}
                  </div>
                  <div style={{ fontSize: "12px", color: "#64748b", marginTop: 2 }}>
                    Category: <strong style={{ color: "#0284c7" }}>{String(assigningBooking.category)}</strong> • Order #{assigningBooking.id.slice(-6)}
                  </div>
                </div>
                <div style={{ textAlign: "right" }}>
                  <div style={{ fontSize: "16px", fontWeight: 800, color: "#059669" }}>
                    ₹{String(assigningBooking.total ?? "")}
                  </div>
                  <div style={{ fontSize: "11px", color: "#64748b" }}>
                    {String(assigningBooking.status)}
                  </div>
                </div>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "8px", marginTop: "12px", paddingTop: "10px", borderTop: "1px solid #e2e8f0", fontSize: "12px" }}>
                <div>
                  <span style={{ color: "#64748b" }}>Customer: </span>
                  <strong>{String(assigningBooking.customerName || "Customer")}</strong> (
                  <a href={`tel:${String(assigningBooking.customerPhone || "")}`} style={{ color: "#0284c7" }}>
                    {String(assigningBooking.customerPhone || "")}
                  </a>)
                </div>
                <div>
                  <span style={{ color: "#64748b" }}>Location: </span>
                  <strong>{String(assigningBooking.addressText || assigningBooking.area || "Khammam")}</strong>
                </div>
                <div>
                  <span style={{ color: "#64748b" }}>When: </span>
                  <strong>{assigningBooking.when === "SCHEDULED" ? when(millis(assigningBooking.scheduledAt)) : "Immediate (Now)"}</strong>
                </div>
                <div>
                  <span style={{ color: "#64748b" }}>Currently Assigned: </span>
                  <strong style={{ color: assigningBooking.partnerName ? "#0284c7" : "#d97706" }}>
                    {assigningBooking.partnerName ? `${String(assigningBooking.partnerName)} (${String(assigningBooking.partnerPhone || "")})` : "None (Awaiting Partner)"}
                  </strong>
                </div>
              </div>
            </div>

            {/* Filter and Search Controls */}
            <div style={{ display: "flex", gap: "8px", flexDirection: "column" }}>
              <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", alignItems: "center", justifyContent: "space-between" }}>
                <input
                  type="text"
                  placeholder="🔍 Search approved partners by name, phone or skill..."
                  value={assignSearch}
                  onChange={(e) => setAssignSearch(e.target.value)}
                  style={{
                    flex: 1,
                    minWidth: "200px",
                    padding: "8px 12px",
                    borderRadius: "8px",
                    border: "1px solid #cbd5e1",
                    fontSize: "13px"
                  }}
                />
                <div style={{ display: "flex", gap: "6px" }}>
                  {(["MATCHING", "ALL", "ONLINE"] as const).map((f) => (
                    <button
                      key={f}
                      type="button"
                      onClick={() => setAssignFilter(f)}
                      style={{
                        padding: "6px 12px",
                        borderRadius: "8px",
                        border: "1px solid",
                        borderColor: assignFilter === f ? "#0284c7" : "#cbd5e1",
                        background: assignFilter === f ? "#0284c7" : "#fff",
                        color: assignFilter === f ? "#fff" : "#475569",
                        fontSize: "12px",
                        fontWeight: 600,
                        cursor: "pointer"
                      }}
                    >
                      {f === "MATCHING" ? `Matching Skills (${approvedPartners.filter(p => {
                        const cats = ((p.categories || []) as string[]).map(c => c.toUpperCase());
                        return cats.includes("ALL") || cats.includes(String(assigningBooking.category).toUpperCase());
                      }).length})` :
                       f === "ALL" ? `All Approved (${approvedPartners.length})` :
                       `Online (${approvedPartners.filter(p => !!p.online).length})`}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Partners List */}
            <div style={{ display: "flex", flexDirection: "column", gap: "10px", maxHeight: "360px", overflowY: "auto", paddingRight: "4px" }}>
              {filteredAssignPartners.length === 0 ? (
                <div style={{ textAlign: "center", padding: "32px 16px", color: "#64748b", background: "#f8fafc", borderRadius: "10px" }}>
                  <div style={{ fontSize: "28px", marginBottom: "6px" }}>🔍</div>
                  <div style={{ fontWeight: 600 }}>No approved partners found matching this filter</div>
                  <div style={{ fontSize: "12px", marginTop: "4px" }}>
                    Try switching filter to &apos;All Approved&apos; or check the Partners tab to verify partner skills.
                  </div>
                </div>
              ) : (
                filteredAssignPartners.map((p) => {
                  const bookingCat = String(assigningBooking.category || "").toUpperCase();
                  const partnerCats = ((p.categories || []) as string[]).map(c => c.toUpperCase());
                  const matchesCategory = partnerCats.includes("ALL") || partnerCats.includes(bookingCat);
                  const isAssignedToThis = assigningBooking.partnerId === p.id;
                  const isBusyWithOther = !!p.activeBookingId && p.activeBookingId !== assigningBooking.id;
                  const pCount = Number(p.ratingCount || 0);
                  const pRating = pCount ? (Number(p.ratingSum || 0) / pCount).toFixed(1) : "–";

                  return (
                    <div
                      key={p.id}
                      style={{
                        border: "1px solid",
                        borderColor: isAssignedToThis ? "#86efac" : matchesCategory ? "#bfdbfe" : "#e2e8f0",
                        background: isAssignedToThis ? "#f0fdf4" : matchesCategory ? "#f8fafc" : "#fff",
                        borderRadius: "12px",
                        padding: "12px 14px",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        gap: "12px",
                        transition: "all 0.15s ease"
                      }}
                    >
                      <div style={{ flex: 1 }}>
                        <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                          <strong style={{ fontSize: "14px", color: "#0f172a" }}>{String(p.name || "Partner")}</strong>
                          <span style={{ fontSize: "12px", color: "#0284c7" }}>
                            <a href={`tel:${String(p.phone || "")}`} style={{ color: "#0284c7", textDecoration: "none" }}>
                              📞 {String(p.phone || "")}
                            </a>
                          </span>
                          <span
                            style={{
                              fontSize: "11px",
                              padding: "2px 6px",
                              borderRadius: "12px",
                              fontWeight: 600,
                              background: p.online ? "#ecfdf5" : "#f1f5f9",
                              color: p.online ? "#059669" : "#64748b",
                              border: `1px solid ${p.online ? "#a7f3d0" : "#cbd5e1"}`
                            }}
                          >
                            {p.online ? "🟢 Online" : "⚪ Offline"}
                          </span>
                          {isBusyWithOther && (
                            <span
                              style={{
                                fontSize: "11px",
                                padding: "2px 6px",
                                borderRadius: "12px",
                                fontWeight: 600,
                                background: "#fef3c7",
                                color: "#b45309",
                                border: "1px solid #fde68a"
                              }}
                            >
                              ⚠️ Busy on #{String(p.activeBookingId).slice(-6)}
                            </span>
                          )}
                        </div>

                        <div style={{ fontSize: "12px", color: "#64748b", marginTop: "3px" }}>
                          📍 {String(p.area || "Khammam")} • {pRating !== "–" ? `★ ${pRating}` : "New"} ({String(p.jobsCompleted || 0)} jobs completed) • {String(p.experienceYears ?? 0)} yrs exp
                        </div>

                        {/* Verified Skills Badges */}
                        <div style={{ display: "flex", gap: "4px", flexWrap: "wrap", marginTop: "6px" }}>
                          <span style={{ fontSize: "11px", color: "#475569", fontWeight: 600, alignSelf: "center", marginRight: 2 }}>
                            Verified Skills:
                          </span>
                          {((p.categories || []) as string[]).length === 0 ? (
                            <span style={{ fontSize: "11px", color: "#94a3b8" }}>No categories assigned</span>
                          ) : (
                            ((p.categories || []) as string[]).map((c) => {
                              const isSkillMatch = c.toUpperCase() === bookingCat || c.toUpperCase() === "ALL";
                              return (
                                <span
                                  key={c}
                                  style={{
                                    fontSize: "10.5px",
                                    padding: "2px 6px",
                                    borderRadius: "4px",
                                    fontWeight: 700,
                                    background: isSkillMatch ? "#dcfce7" : "#f1f5f9",
                                    color: isSkillMatch ? "#15803d" : "#475569",
                                    border: `1px solid ${isSkillMatch ? "#86efac" : "#cbd5e1"}`
                                  }}
                                >
                                  {isSkillMatch ? "✓ " : ""}{c}
                                </span>
                              );
                            })
                          )}
                        </div>
                      </div>

                      {/* Action Button */}
                      <div>
                        {isAssignedToThis ? (
                          <div style={{ fontSize: "12px", fontWeight: 700, color: "#16a34a", padding: "6px 12px", background: "#dcfce7", borderRadius: "8px", border: "1px solid #86efac", textAlign: "center" }}>
                            Assigned ✓
                          </div>
                        ) : (
                          <button
                            type="button"
                            disabled={assignLoading}
                            onClick={() => assignPartner(assigningBooking.id, p.id, String(p.name || "Partner"))}
                            style={{
                              background: matchesCategory ? "#0284c7" : "#475569",
                              color: "#fff",
                              border: "none",
                              borderRadius: "8px",
                              padding: "7px 14px",
                              fontSize: "12px",
                              fontWeight: 700,
                              cursor: assignLoading ? "not-allowed" : "pointer",
                              whiteSpace: "nowrap",
                              boxShadow: "0 1px 2px rgba(0, 0, 0, 0.05)"
                            }}
                          >
                            {assignLoading ? "Assigning..." : assigningBooking.partnerId ? "Reassign" : "Assign Partner"}
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Bottom Modal Footer */}
            <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px", borderTop: "1px solid #e2e8f0", paddingTop: "14px" }}>
              <button
                type="button"
                disabled={assignLoading}
                onClick={() => setAssigningBooking(null)}
                style={{
                  padding: "8px 16px",
                  borderRadius: "8px",
                  border: "1px solid #cbd5e1",
                  background: "#fff",
                  color: "#475569",
                  fontSize: "13px",
                  fontWeight: 600,
                  cursor: assignLoading ? "not-allowed" : "pointer"
                }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Book Service for Customer & Directly Assign Worker */}
      {showBookModal && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0, 0, 0, 0.65)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 9999,
            padding: 16
          }}
          onClick={(e) => { if (e.target === e.currentTarget) setShowBookModal(false); }}
        >
          <div
            style={{
              background: "#fff",
              borderRadius: "16px",
              padding: "24px",
              maxWidth: "540px",
              width: "100%",
              maxHeight: "90vh",
              overflowY: "auto",
              boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.3)"
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
              <h3 style={{ margin: 0, fontSize: "18px", fontWeight: 800, color: "#0f172a" }}>
                🧹 Book Service &amp; Assign Worker
              </h3>
              <button
                type="button"
                onClick={() => setShowBookModal(false)}
                style={{ background: "none", border: "none", fontSize: "20px", cursor: "pointer", color: "#64748b" }}
              >
                ✕
              </button>
            </div>

            {bookedResult ? (
              <div style={{ background: "#ecfdf5", border: "1px solid #a7f3d0", borderRadius: "12px", padding: "18px", textAlign: "center" }}>
                <div style={{ fontSize: "28px" }}>✅</div>
                <h4 style={{ margin: "8px 0 4px", color: "#065f46", fontSize: "17px", fontWeight: 800 }}>Booking Confirmed!</h4>
                <p style={{ margin: 0, fontSize: "13px", color: "#047857" }}>
                  Booking ID: <strong>{bookedResult.bookingId}</strong>
                </p>
                <div style={{ background: "#fff", border: "2px dashed #059669", borderRadius: "10px", padding: "12px", margin: "14px 0" }}>
                  <div style={{ fontSize: "12px", color: "#64748b", fontWeight: 700, textTransform: "uppercase" }}>CUSTOMER 4-DIGIT START CODE</div>
                  <div style={{ fontSize: "32px", fontWeight: 900, color: "#059669", letterSpacing: "4px", marginTop: "2px" }}>
                    {bookedResult.startOtp}
                  </div>
                  <div style={{ fontSize: "11.5px", color: "#475569" }}>
                    Give this code to the customer so the worker can start the job.
                  </div>
                </div>
                <button
                  type="button"
                  className="btn"
                  onClick={() => { setShowBookModal(false); setBookedResult(null); }}
                  style={{ background: "#059669", color: "#fff", fontWeight: 700, padding: "8px 20px" }}
                >
                  Done
                </button>
              </div>
            ) : (
              <form onSubmit={handleAdminCreateBooking} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                  <div>
                    <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#475569", marginBottom: "4px" }}>
                      Customer Name *
                    </label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Ramesh Babu"
                      value={bookForm.customerName}
                      onChange={(e) => setBookForm({ ...bookForm, customerName: e.target.value })}
                      style={{ width: "100%", padding: "8px 10px", borderRadius: "6px", border: "1px solid #cbd5e1", fontSize: "13px" }}
                    />
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#475569", marginBottom: "4px" }}>
                      Customer Mobile *
                    </label>
                    <input
                      type="tel"
                      required
                      placeholder="e.g. 9876543210"
                      value={bookForm.customerPhone}
                      onChange={(e) => setBookForm({ ...bookForm, customerPhone: e.target.value })}
                      style={{ width: "100%", padding: "8px 10px", borderRadius: "6px", border: "1px solid #cbd5e1", fontSize: "13px" }}
                    />
                  </div>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                  <div>
                    <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#475569", marginBottom: "4px" }}>
                      Category *
                    </label>
                    <select
                      value={bookForm.category}
                      onChange={(e) => setBookForm({ ...bookForm, category: e.target.value })}
                      style={{ width: "100%", padding: "8px 10px", borderRadius: "6px", border: "1px solid #cbd5e1", fontSize: "13px" }}
                    >
                      <option value="CLEANING">Cleaning &amp; Housekeeping</option>
                      <option value="HOME_HELP">Domestic Help / Cooking</option>
                      <option value="ELECTRICIAN">Electrician</option>
                      <option value="PLUMBER">Plumber</option>
                      <option value="APPLIANCE">Appliance Repair</option>
                      <option value="VEHICLE">Car &amp; Bike Wash</option>
                    </select>
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#475569", marginBottom: "4px" }}>
                      Specific Service *
                    </label>
                    <select
                      value={bookForm.serviceName}
                      onChange={(e) => setBookForm({ ...bookForm, serviceName: e.target.value })}
                      style={{ width: "100%", padding: "8px 10px", borderRadius: "6px", border: "1px solid #cbd5e1", fontSize: "13px" }}
                    >
                      <option value="Room Cleaning & Mopping">Room Cleaning &amp; Mopping (₹199)</option>
                      <option value="Deep Kitchen Cleaning">Deep Kitchen Cleaning (₹399)</option>
                      <option value="Dishwashing">Dishwashing &amp; Kitchen Sink (₹149)</option>
                      <option value="Bathroom Cleaning">Bathroom &amp; Toilet Cleaning (₹249)</option>
                      <option value="Full House Deep Cleaning">Full House Deep Cleaning (₹999)</option>
                    </select>
                  </div>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                  <div>
                    <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#475569", marginBottom: "4px" }}>
                      Khammam Locality *
                    </label>
                    <select
                      value={bookForm.area}
                      onChange={(e) => setBookForm({ ...bookForm, area: e.target.value })}
                      style={{ width: "100%", padding: "8px 10px", borderRadius: "6px", border: "1px solid #cbd5e1", fontSize: "13px" }}
                    >
                      <option value="Wyra Road">Wyra Road</option>
                      <option value="Mamillagudem">Mamillagudem</option>
                      <option value="Gandhi Chowk">Gandhi Chowk</option>
                      <option value="Rotary Nagar">Rotary Nagar</option>
                      <option value="Nehru Nagar">Nehru Nagar</option>
                      <option value="Bank Colony">Bank Colony</option>
                      <option value="Khammam Bus Stand Area">Bus Stand Area</option>
                    </select>
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#475569", marginBottom: "4px" }}>
                      Directly Assign to Worker
                    </label>
                    <select
                      value={bookForm.partnerId}
                      onChange={(e) => setBookForm({ ...bookForm, partnerId: e.target.value })}
                      style={{ width: "100%", padding: "8px 10px", borderRadius: "6px", border: "1px solid #cbd5e1", fontSize: "13px" }}
                    >
                      <option value="">— Auto-Dispatch (Radar Wave) —</option>
                      {partners.filter((p) => p.status === "APPROVED").map((p) => (
                        <option key={p.id} value={p.id}>
                          {String(p.name)} ({String(p.phone)}) {p.online ? "🟢 Online" : "⚪ Offline"}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#475569", marginBottom: "4px" }}>
                    Full Customer Address *
                  </label>
                  <textarea
                    required
                    rows={2}
                    placeholder="House/Flat No, Landmark, Khammam..."
                    value={bookForm.addressText}
                    onChange={(e) => setBookForm({ ...bookForm, addressText: e.target.value })}
                    style={{ width: "100%", padding: "8px 10px", borderRadius: "6px", border: "1px solid #cbd5e1", fontSize: "13px" }}
                  />
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#475569", marginBottom: "4px" }}>
                    Admin Note / Instructions
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Call before reaching, customer requested female cleaner"
                    value={bookForm.note}
                    onChange={(e) => setBookForm({ ...bookForm, note: e.target.value })}
                    style={{ width: "100%", padding: "8px 10px", borderRadius: "6px", border: "1px solid #cbd5e1", fontSize: "13px" }}
                  />
                </div>

                <div style={{ display: "flex", gap: "10px", marginTop: "10px" }}>
                  <button
                    type="button"
                    onClick={() => setShowBookModal(false)}
                    style={{ flex: 1, padding: "10px", borderRadius: "8px", border: "1px solid #cbd5e1", background: "#f8fafc", fontWeight: 600, cursor: "pointer" }}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submittingBooking}
                    style={{
                      flex: 2,
                      padding: "10px",
                      borderRadius: "8px",
                      border: "none",
                      background: "#059669",
                      color: "#fff",
                      fontWeight: 700,
                      cursor: submittingBooking ? "not-allowed" : "pointer"
                    }}
                  >
                    {submittingBooking ? "Booking..." : "Confirm & Assign Order"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

type Coupon = {
  code: string;
  title: string;
  type: "FLAT" | "PCT";
  value: number;
  maxOff?: number;
  minOrder?: number;
  validFrom?: number;
  validTo?: number;
  firstBookingOnly?: boolean;
  visible?: boolean;
  active?: boolean;
};

type Settings = {
  upiId: string;
  upiName: string;
  bookingFee: number;
  inspectionFee: number;
  commissionPct: number;
  partnerFee: number;
  partnerFirstJobFree: boolean;
  firstBookingFeeFree: boolean;
  planMembersFeeFree: boolean;
  minTopup: number;
  coupons: Coupon[];
  services: Array<{ id: string; price?: number; originalPrice?: number; active?: boolean }>;
};

const DAY = 24 * 60 * 60 * 1000;
const toDate = (ms?: number) => (ms ? new Date(ms + 330 * 60 * 1000).toISOString().slice(0, 10) : "");
/** Date input (IST day) → epoch ms at 00:00 IST, or 23:59:59 IST for an end date. */
const fromDate = (v: string, end = false) => (v ? Date.parse(`${v}T00:00:00+05:30`) + (end ? DAY - 1000 : 0) : undefined);

/** Ready-made festive offers (dates are this year's; edit before saving). */
const PRESETS: Array<{ label: string; coupon: Coupon }> = [
  { label: "Dasara ₹30 off", coupon: { code: "DASARA30", title: "Dasara offer ₹30 off", type: "FLAT", value: 30, minOrder: 300 } },
  { label: "Diwali 10% (max ₹35)", coupon: { code: "DIWALI10", title: "Diwali 10% off", type: "PCT", value: 10, maxOff: 35, minOrder: 250 } },
  { label: "Sankranti ₹25 off", coupon: { code: "SANKRANTI25", title: "Sankranti offer ₹25 off", type: "FLAT", value: 25 } },
  { label: "Weekend ₹20 off", coupon: { code: "WEEKEND20", title: "Weekend ₹20 off", type: "FLAT", value: 20, minOrder: 200 } },
  { label: "Welcome (first booking) ₹35", coupon: { code: "WELCOME35", title: "Welcome ₹35 off", type: "FLAT", value: 35, firstBookingOnly: true } },
];

function ServicesSettings({ onSaved, onError }: { onSaved: (m: string) => void; onError: (m: string) => void }) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [s, setS] = useState<Settings | null>(null);
  const [raw, setRaw] = useState<Record<string, unknown>>({});
  const [catalog, setCatalog] = useState<Array<{ id: string; category: string; name: string; price: number; originalPrice?: number }>>([]);

  useEffect(() => {
    if (!services) return;
    void (async () => {
      const snap = await getDoc(doc(services.db, "app_config", "services"));
      const d = (snap.data() || {}) as Partial<Settings>;
      setRaw((snap.data() || {}) as Record<string, unknown>);
      setS({
        upiId: d.upiId?.trim() ? d.upiId : "dutypein@ybl",
        upiName: d.upiName || "DutyPe",
        bookingFee: d.bookingFee ?? 19,
        inspectionFee: d.inspectionFee ?? 49,
        commissionPct: d.commissionPct ?? 0,
        partnerFee: d.partnerFee ?? 19,
        partnerFirstJobFree: d.partnerFirstJobFree !== false,
        firstBookingFeeFree: d.firstBookingFeeFree !== false,
        planMembersFeeFree: d.planMembersFeeFree !== false,
        minTopup: d.minTopup ?? 200,
        coupons: d.coupons || [],
        services: d.services || [],
      });
      try {
        const res = await httpsCallable(services.functions, "getServiceCatalog")({ all: true });
        const rawList = ((res.data as { services?: unknown[] }).services || []) as Array<{ id: string; category: string; name: string; price: number; originalPrice?: number }>;
        const list = rawList.filter((c) => CATEGORY_IDS.includes(c.category));
        setCatalog(list);
      } catch {
        setCatalog([]);
      }
    })();
  }, [services]);

  if (!s) return <p>Loading…</p>;

  const override = (id: string) => s.services.find((x) => x.id === id);
  const setPrice = (id: string, price: string) => {
    const rest = s.services.filter((x) => x.id !== id);
    const cur = override(id) || { id };
    const value = Number(price);
    setS({ ...s, services: [...rest, { ...cur, price: Number.isFinite(value) && price !== "" ? value : undefined }] });
  };
  const setOriginalPrice = (id: string, originalPrice: string) => {
    const rest = s.services.filter((x) => x.id !== id);
    const cur = override(id) || { id };
    const value = Number(originalPrice);
    setS({ ...s, services: [...rest, { ...cur, originalPrice: Number.isFinite(value) && originalPrice !== "" ? value : undefined }] });
  };
  const setActive = (id: string, active: boolean) => {
    const rest = s.services.filter((x) => x.id !== id);
    setS({ ...s, services: [...rest, { ...(override(id) || { id }), active }] });
  };
  const setCoupon = (i: number, patch: Partial<Coupon>) =>
    setS({ ...s, coupons: s.coupons.map((c, j) => (j === i ? { ...c, ...patch } : c)) });
  const addCoupon = (c: Coupon) => {
    if (s.coupons.some((x) => x.code === c.code)) return onError(`Coupon ${c.code} already exists`);
    setS({ ...s, coupons: [...s.coupons, { visible: true, active: true, ...c }] });
  };
  // A customer never gets more off than DutyPe takes on the booking, so offers never cost DutyPe money.
  const maxOff = s.bookingFee + s.partnerFee;

  async function save() {
    if (!services || !s) return;
    const codes = s.coupons.map((c) => c.code.trim().toUpperCase());
    const bad = codes.find((c) => !/^[A-Z0-9]{3,20}$/.test(c));
    if (bad !== undefined) return onError(`Coupon code "${bad}" must be 3–20 letters or digits`);
    if (new Set(codes).size !== codes.length) return onError("Coupon codes must be different");
    try {
      const clean = s.services
        .map((x) => ({
          id: x.id,
          ...(x.price !== undefined ? { price: x.price } : {}),
          ...(x.originalPrice !== undefined ? { originalPrice: x.originalPrice } : {}),
          ...(x.active === false ? { active: false } : {}),
        }))
        .filter((x) => Object.keys(x).length > 1);
      const coupons = s.coupons.map((c) => {
        const out: Record<string, unknown> = {
          code: c.code.trim().toUpperCase(), title: c.title.trim() || c.code, type: c.type, value: Number(c.value) || 0,
          visible: c.visible !== false, active: c.active !== false,
        };
        if (c.maxOff) out.maxOff = Number(c.maxOff);
        if (c.minOrder) out.minOrder = Number(c.minOrder);
        if (c.validFrom) out.validFrom = c.validFrom;
        if (c.validTo) out.validTo = c.validTo;
        if (c.firstBookingOnly) out.firstBookingOnly = true;
        return out;
      });
      // Keep fields this screen does not edit (city, districtIds, ...).
      await setDoc(doc(services.db, "app_config", "services"), { ...raw, ...s, coupons, services: clean }, { merge: false });
      onSaved("Saved. The app picks up new prices and offers within ~10 minutes.");
    } catch (e: unknown) {
      onError(e instanceof Error ? e.message : "Save failed");
    }
  }

  const num = (k: "bookingFee" | "inspectionFee" | "commissionPct" | "partnerFee" | "minTopup") => (
    <input type="number" value={String(s[k])} onChange={(e) => setS({ ...s, [k]: Number(e.target.value) })} style={{ width: 100 }} />
  );
  const check = (k: "partnerFirstJobFree" | "firstBookingFeeFree" | "planMembersFeeFree") => (
    <input type="checkbox" checked={s[k]} onChange={(e) => setS({ ...s, [k]: e.target.checked })} />
  );

  return (
    <section className="admin-section">
      <h2 className="admin-section-title">Prices &amp; settings</h2>
      <p>Partners pay their credit top-ups to this UPI ID. Fees apply to new bookings.</p>
      <div style={{ display: "grid", gridTemplateColumns: "260px 1fr", gap: 10, alignItems: "center", maxWidth: 640 }}>
        <label>UPI ID for top-ups</label>
        <input value={s.upiId} onChange={(e) => setS({ ...s, upiId: e.target.value.trim() })} placeholder="dutypein@ybl" />
        <label>UPI name</label>
        <input value={s.upiName} onChange={(e) => setS({ ...s, upiName: e.target.value })} />
        <label>Customer booking fee (₹)</label>{num("bookingFee")}
        <label>Inspection visit fee (₹)</label>{num("inspectionFee")}
        <label>Partner fee per job (₹, e.g. 9 or 19)</label>{num("partnerFee")}
        <label>Partner&apos;s first job free</label>{check("partnerFirstJobFree")}
        <label>Customer&apos;s first booking: no booking fee</label>{check("firstBookingFeeFree")}
        <label>Employers on a paid plan: no booking fee</label>{check("planMembersFeeFree")}
        <label>Commission on price (%)</label>{num("commissionPct")}
        <label>Minimum top-up (₹)</label>{num("minTopup")}
      </div>

      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 28, flexWrap: "wrap", gap: 10 }}>
        <div>
          <h3 style={{ margin: 0 }}>Coupons &amp; festive offers</h3>
          <p style={{ margin: "4px 0 0", maxWidth: 760, fontSize: 13, color: "#64748b" }}>
            Offers never stack: a customer gets the first-booking offer or the best coupon. A discount is capped at
            DutyPe&apos;s own take on the booking (booking fee + partner fee{s.commissionPct ? " + commission" : ""}, now up to ₹{maxOff}),
            so an offer never costs DutyPe money and the partner still earns the full service price minus the ₹{s.partnerFee} fee.
            Each customer can use a coupon once. Hidden coupons are not shown in the app; share them on posters and WhatsApp.
          </p>
        </div>
        <button className="btn btn-approve" onClick={() => void save()} style={{ padding: "8px 18px", fontWeight: 600, display: "flex", alignItems: "center", gap: 6 }}>
          💾 Save Coupons Now
        </button>
      </div>

      {/* Active Coupons Summary */}
      {(() => {
        const activeList = s.coupons.filter((c) => c.active !== false && c.code.trim().length > 0);
        return (
          <div style={{ margin: "14px 0", padding: "12px 16px", background: "#f8fafc", borderRadius: 8, border: "1px solid #e2e8f0" }}>
            <div style={{ fontSize: 13, fontWeight: 700, color: "#1e293b", marginBottom: 8, display: "flex", alignItems: "center", gap: 6 }}>
              <span>🏷️ Active Coupons Live in App ({activeList.length}):</span>
            </div>
            {activeList.length === 0 ? (
              <div style={{ fontSize: 13, color: "#94a3b8" }}>No active coupons currently. Add one below and click &quot;Save Coupons Now&quot;.</div>
            ) : (
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                {activeList.map((c, idx) => (
                  <div key={c.code || idx} style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: 6, padding: "6px 12px", display: "flex", alignItems: "center", gap: 8 }}>
                    <span style={{ fontWeight: 800, color: "#15803d", letterSpacing: 0.5, fontFamily: "monospace", fontSize: 13 }}>{c.code}</span>
                    <span style={{ fontSize: 12, color: "#166534", fontWeight: 600 }}>
                      {c.type === "PCT" ? `${c.value}% OFF` : `₹${c.value} FLAT OFF`}
                      {c.maxOff ? ` (max ₹${c.maxOff})` : ""}
                    </span>
                    {c.firstBookingOnly && (
                      <span style={{ background: "#fef3c7", color: "#92400e", fontSize: 10, fontWeight: 700, padding: "2px 6px", borderRadius: 4 }}>
                        New Users Only
                      </span>
                    )}
                    {c.minOrder ? (
                      <span style={{ fontSize: 11, color: "#64748b" }}>Min ₹{c.minOrder}</span>
                    ) : null}
                  </div>
                ))}
              </div>
            )}
          </div>
        );
      })()}

      <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 10 }}>
        {PRESETS.map((p) => (
          <button key={p.coupon.code} className="btn" onClick={() => addCoupon(p.coupon)}>+ {p.label}</button>
        ))}
        <button className="btn" onClick={() => addCoupon({ code: `OFFER${s.coupons.length + 1}`, title: "Special offer", type: "FLAT", value: 20 })}>+ Blank coupon</button>
      </div>
      <div className="admin-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Code</th><th>Title (shown to customers)</th><th>Type</th><th>Value</th><th>Max off ₹</th><th>Min order ₹</th>
              <th>From</th><th>To</th><th>First booking only</th><th>Show in app</th><th>Active</th><th></th>
            </tr>
          </thead>
          <tbody>
            {s.coupons.length === 0 && (
              <tr><td colSpan={12}>No coupons yet. Add a festive offer above.</td></tr>
            )}
            {s.coupons.map((c, i) => {
              const worth = c.type === "PCT" ? (c.maxOff || 0) : c.value;
              return (
                <tr key={i}>
                  <td><input value={c.code} onChange={(e) => setCoupon(i, { code: e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, "") })} style={{ width: 120 }} /></td>
                  <td><input value={c.title} onChange={(e) => setCoupon(i, { title: e.target.value })} style={{ width: 200 }} /></td>
                  <td>
                    <select value={c.type} onChange={(e) => setCoupon(i, { type: e.target.value as Coupon["type"] })}>
                      <option value="FLAT">₹ off</option>
                      <option value="PCT">% off</option>
                    </select>
                  </td>
                  <td>
                    <input type="number" value={String(c.value)} onChange={(e) => setCoupon(i, { value: Number(e.target.value) })} style={{ width: 70 }} />
                    {worth > maxOff && <div style={{ color: "#b45309", fontSize: 12 }}>Capped at ₹{maxOff}</div>}
                  </td>
                  <td><input type="number" value={c.maxOff ? String(c.maxOff) : ""} onChange={(e) => setCoupon(i, { maxOff: Number(e.target.value) || undefined })} style={{ width: 70 }} /></td>
                  <td><input type="number" value={c.minOrder ? String(c.minOrder) : ""} onChange={(e) => setCoupon(i, { minOrder: Number(e.target.value) || undefined })} style={{ width: 80 }} /></td>
                  <td><input type="date" value={toDate(c.validFrom)} onChange={(e) => setCoupon(i, { validFrom: fromDate(e.target.value) })} /></td>
                  <td><input type="date" value={toDate(c.validTo ? c.validTo - DAY + 1000 : undefined)} onChange={(e) => setCoupon(i, { validTo: fromDate(e.target.value, true) })} /></td>
                  <td><input type="checkbox" checked={!!c.firstBookingOnly} onChange={(e) => setCoupon(i, { firstBookingOnly: e.target.checked })} /></td>
                  <td><input type="checkbox" checked={c.visible !== false} onChange={(e) => setCoupon(i, { visible: e.target.checked })} /></td>
                  <td><input type="checkbox" checked={c.active !== false} onChange={(e) => setCoupon(i, { active: e.target.checked })} /></td>
                  <td><button className="btn" onClick={() => setS({ ...s, coupons: s.coupons.filter((_, j) => j !== i) })}>Remove</button></td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <button className="btn btn-approve" onClick={() => void save()} style={{ marginTop: 10, marginBottom: 20 }}>💾 Save Coupons</button>

      <h3 style={{ marginTop: 24 }}>Service prices &amp; discounts</h3>
      <p style={{ maxWidth: 760, fontSize: 13, color: "#64748b" }}>
        Set offer prices (e.g. ₹199) and original MRP (e.g. ₹299). Customers see the strikethrough price and percentage off in the app.
      </p>
      <div className="admin-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Category</th>
              <th>Service</th>
              <th>Offer Price (₹)</th>
              <th>MRP / Original (₹)</th>
              <th>Customer Sees</th>
              <th>Offered</th>
            </tr>
          </thead>
          <tbody>
            {CATEGORY_IDS.flatMap((cat) => catalog.filter((c) => c.category === cat)).map((c) => {
              const o = override(c.id);
              const offerPrice = o?.price !== undefined ? o.price : c.price;
              const origPrice = o?.originalPrice !== undefined ? o.originalPrice : (c as { originalPrice?: number }).originalPrice;
              const mrp = origPrice && origPrice > offerPrice ? origPrice : Math.round(offerPrice * 1.25);
              const discount = mrp > offerPrice ? Math.round(((mrp - offerPrice) / mrp) * 100) : 0;
              return (
                <tr key={c.id}>
                  <td><b>{c.category}</b></td>
                  <td>{c.name}</td>
                  <td>
                    <input
                      type="number"
                      value={o?.price !== undefined ? String(o.price) : ""}
                      placeholder={String(c.price)}
                      onChange={(e) => setPrice(c.id, e.target.value)}
                      style={{ width: 90 }}
                    />
                  </td>
                  <td>
                    <input
                      type="number"
                      value={o?.originalPrice !== undefined ? String(o.originalPrice) : ""}
                      placeholder={String(Math.round(offerPrice * 1.25))}
                      onChange={(e) => setOriginalPrice(c.id, e.target.value)}
                      style={{ width: 90 }}
                    />
                  </td>
                  <td>
                    <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                      <span style={{ fontWeight: 600 }}>₹{offerPrice}</span>
                      <span style={{ textDecoration: "line-through", color: "#94a3b8", fontSize: 12 }}>₹{mrp}</span>
                      {discount > 0 && (
                        <span style={{ background: "#dcfce7", color: "#15803d", fontSize: 11, fontWeight: 700, padding: "2px 6px", borderRadius: 4 }}>
                          {discount}% OFF
                        </span>
                      )}
                    </div>
                  </td>
                  <td><input type="checkbox" checked={o?.active !== false} onChange={(e) => setActive(c.id, e.target.checked)} /></td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <button className="btn btn-approve" onClick={() => void save()} style={{ marginTop: 12 }}>Save settings</button>
    </section>
  );
}
