"use client";

import { useEffect, useMemo, useState } from "react";
import {
  collection,
  doc,
  addDoc,
  updateDoc,
  deleteDoc,
  onSnapshot,
  query,
  orderBy,
  serverTimestamp,
  Timestamp,
  limit,
} from "firebase/firestore";
import { getFirebaseServices } from "@/lib/firebase/client";
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  Users,
  Plus,
  Trash2,
  Edit3,
  Download,
  Search,
  CheckCircle2,
  AlertCircle,
  MapPin,
  ExternalLink,
  PieChart,
  X,
  FileSpreadsheet,
  Receipt,
  Layers
} from "lucide-react";

export type ExpenseCategory =
  | "POSTERS"
  | "DIGITAL_ADS"
  | "INFLUENCERS"
  | "FIELD_DESK"
  | "AUTO_PA"
  | "WHATSAPP_SMS"
  | "REFERRAL_PROMO"
  | "OTHER";

export interface ExpenseRecord {
  id: string;
  title: string;
  category: ExpenseCategory;
  amount: number;
  date: string;
  location: string;
  notes: string;
  receiptUrl?: string;
  usersGainedEstimate?: number;
  createdAt?: number;
}

const CATEGORY_CONFIG: Record<
  ExpenseCategory,
  { label: string; icon: string; badgeClass: string }
> = {
  POSTERS: {
    label: "Posters & Banners",
    icon: "🖨️",
    badgeClass: "adm-badge-amber",
  },
  DIGITAL_ADS: {
    label: "Meta / Instagram Ads",
    icon: "📱",
    badgeClass: "adm-badge-blue",
  },
  INFLUENCERS: {
    label: "Influencer Marketing",
    icon: "🌟",
    badgeClass: "adm-badge-purple",
  },
  FIELD_DESK: {
    label: "Field Desks & Canopies",
    icon: "⛱️",
    badgeClass: "adm-badge-emerald",
  },
  AUTO_PA: {
    label: "Auto Rickshaw PA Audio",
    icon: "📣",
    badgeClass: "adm-badge-rose",
  },
  WHATSAPP_SMS: {
    label: "WhatsApp / SMS Broadcasts",
    icon: "💬",
    badgeClass: "adm-badge-blue",
  },
  REFERRAL_PROMO: {
    label: "Referral & Bonus Rewards",
    icon: "🎁",
    badgeClass: "adm-badge-purple",
  },
  OTHER: {
    label: "Other Expenses",
    icon: "📦",
    badgeClass: "adm-badge-neutral",
  },
};

export function AdminExpensesClient() {
  const services = useMemo(() => getFirebaseServices(), []);

  // Live state
  const [expenses, setExpenses] = useState<ExpenseRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [totalUsers, setTotalUsers] = useState(0);
  const [subscriptionRevenue, setSubscriptionRevenue] = useState(0);
  const [serviceRevenue, setServiceRevenue] = useState(0);

  // Search & Filters
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<string>("ALL");

  // Dialog State
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingExpense, setEditingExpense] = useState<ExpenseRecord | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Form State
  const [formTitle, setFormTitle] = useState("");
  const [formCategory, setFormCategory] = useState<ExpenseCategory>("POSTERS");
  const [formAmount, setFormAmount] = useState("");
  const [formDate, setFormDate] = useState(() => new Date().toISOString().split("T")[0]);
  const [formLocation, setFormLocation] = useState("Khammam");
  const [formNotes, setFormNotes] = useState("");
  const [formReceiptUrl, setFormReceiptUrl] = useState("");
  const [formUsersEstimate, setFormUsersEstimate] = useState("");

  // 1. Listen for marketing expenses
  useEffect(() => {
    if (!services?.db) return;
    const q = query(
      collection(services.db, "marketing_expenses"),
      orderBy("date", "desc"),
      limit(250)
    );
    const unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        const rows: ExpenseRecord[] = snapshot.docs.map((docSnap) => {
          const data = docSnap.data();
          return {
            id: docSnap.id,
            title: String(data.title || "Untitled Expense"),
            category: (data.category as ExpenseCategory) || "OTHER",
            amount: Number(data.amount || 0),
            date: String(data.date || ""),
            location: String(data.location || "General"),
            notes: String(data.notes || ""),
            receiptUrl: data.receiptUrl ? String(data.receiptUrl) : undefined,
            usersGainedEstimate: data.usersGainedEstimate
              ? Number(data.usersGainedEstimate)
              : undefined,
            createdAt:
              data.createdAt instanceof Timestamp
                ? data.createdAt.toMillis()
                : undefined,
          };
        });
        setExpenses(rows);
        setLoading(false);
      },
      (err) => {
        console.error("Error loading marketing expenses:", err);
        setLoading(false);
      }
    );
    return () => unsubscribe();
  }, [services]);

  // 2. Listen for users count
  useEffect(() => {
    if (!services?.db) return;
    const q = query(collection(services.db, "users"), limit(2000));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      setTotalUsers(snapshot.size);
    });
    return () => unsubscribe();
  }, [services]);

  // 3. Listen for subscription revenue
  useEffect(() => {
    if (!services?.db) return;
    const q = query(collection(services.db, "subscription_payments"), limit(500));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      let sumPaise = 0;
      snapshot.forEach((docSnap) => {
        const d = docSnap.data();
        if (d.status === "approved" || d.status === "success") {
          sumPaise += Number(d.amountPaise || 0);
        }
      });
      setSubscriptionRevenue(Math.round(sumPaise / 100));
    });
    return () => unsubscribe();
  }, [services]);

  // 4. Listen for home services revenue
  useEffect(() => {
    if (!services?.db) return;
    const q = query(collection(services.db, "service_bookings"), limit(500));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      let sum = 0;
      snapshot.forEach((docSnap) => {
        const d = docSnap.data();
        if (d.status === "COMPLETED" || d.status === "PAID" || d.status === "CONFIRMED") {
          sum += Number(d.finalPrice || d.price || d.totalAmount || 0);
        }
      });
      setServiceRevenue(sum);
    });
    return () => unsubscribe();
  }, [services]);

  // Financial calculations
  const totalSpent = useMemo(
    () => expenses.reduce((acc, curr) => acc + curr.amount, 0),
    [expenses]
  );

  const totalRevenue = useMemo(
    () => subscriptionRevenue + serviceRevenue,
    [subscriptionRevenue, serviceRevenue]
  );

  const netROI = useMemo(() => totalRevenue - totalSpent, [totalRevenue, totalSpent]);

  const roiPercent = useMemo(() => {
    if (totalSpent === 0) return totalRevenue > 0 ? 100 : 0;
    return Math.round(((totalRevenue - totalSpent) / totalSpent) * 100);
  }, [totalRevenue, totalSpent]);

  const blendedCAC = useMemo(() => {
    if (totalUsers === 0) return 0;
    return Math.round(totalSpent / totalUsers);
  }, [totalSpent, totalUsers]);

  // Category breakdown
  const categoryStats = useMemo(() => {
    const stats: Record<ExpenseCategory, { total: number; count: number }> = {
      POSTERS: { total: 0, count: 0 },
      DIGITAL_ADS: { total: 0, count: 0 },
      INFLUENCERS: { total: 0, count: 0 },
      FIELD_DESK: { total: 0, count: 0 },
      AUTO_PA: { total: 0, count: 0 },
      WHATSAPP_SMS: { total: 0, count: 0 },
      REFERRAL_PROMO: { total: 0, count: 0 },
      OTHER: { total: 0, count: 0 },
    };

    expenses.forEach((e) => {
      if (stats[e.category]) {
        stats[e.category].total += e.amount;
        stats[e.category].count += 1;
      } else {
        stats.OTHER.total += e.amount;
        stats.OTHER.count += 1;
      }
    });

    return stats;
  }, [expenses]);

  // Filtered expenses
  const filteredExpenses = useMemo(() => {
    return expenses.filter((e) => {
      const matchesSearch =
        searchQuery.trim() === "" ||
        e.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        e.location.toLowerCase().includes(searchQuery.toLowerCase()) ||
        e.notes.toLowerCase().includes(searchQuery.toLowerCase());

      const matchesCat =
        selectedCategory === "ALL" || e.category === selectedCategory;

      return matchesSearch && matchesCat;
    });
  }, [expenses, searchQuery, selectedCategory]);

  const openAddModal = () => {
    setEditingExpense(null);
    setFormTitle("");
    setFormCategory("POSTERS");
    setFormAmount("");
    setFormDate(new Date().toISOString().split("T")[0]);
    setFormLocation("Khammam");
    setFormNotes("");
    setFormReceiptUrl("");
    setFormUsersEstimate("");
    setErrorMsg(null);
    setShowAddModal(true);
  };

  const openEditModal = (exp: ExpenseRecord) => {
    setEditingExpense(exp);
    setFormTitle(exp.title);
    setFormCategory(exp.category);
    setFormAmount(String(exp.amount));
    setFormDate(exp.date);
    setFormLocation(exp.location);
    setFormNotes(exp.notes);
    setFormReceiptUrl(exp.receiptUrl || "");
    setFormUsersEstimate(exp.usersGainedEstimate ? String(exp.usersGainedEstimate) : "");
    setErrorMsg(null);
    setShowAddModal(true);
  };

  const handleSaveExpense = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!services?.db) return;

    const parsedAmount = parseFloat(formAmount);
    if (!formTitle.trim()) {
      setErrorMsg("Please provide an expense title or campaign name.");
      return;
    }
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setErrorMsg("Please enter a valid amount in ₹.");
      return;
    }

    setSubmitting(true);
    setErrorMsg(null);

    try {
      const payload: Record<string, unknown> = {
        title: formTitle.trim(),
        category: formCategory,
        amount: parsedAmount,
        date: formDate,
        location: formLocation.trim() || "Khammam",
        notes: formNotes.trim(),
        updatedAt: serverTimestamp(),
      };
      if (formReceiptUrl.trim()) payload.receiptUrl = formReceiptUrl.trim();
      if (formUsersEstimate.trim() && !isNaN(parseInt(formUsersEstimate))) {
        payload.usersGainedEstimate = parseInt(formUsersEstimate);
      }

      if (editingExpense) {
        await updateDoc(
          doc(services.db, "marketing_expenses", editingExpense.id),
          payload
        );
        setSuccessMsg("Expense updated successfully!");
      } else {
        payload.createdAt = serverTimestamp();
        await addDoc(collection(services.db, "marketing_expenses"), payload);
        setSuccessMsg("Expense logged successfully!");
      }

      setShowAddModal(false);
      setTimeout(() => setSuccessMsg(null), 3000);
    } catch (err: unknown) {
      console.error("Save expense error:", err);
      const e = err as { message?: string };
      setErrorMsg(e?.message || "Failed to save expense. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteExpense = async (id: string, title: string) => {
    if (!services?.db) return;
    if (!window.confirm(`Are you sure you want to delete "${title}"?`)) return;

    try {
      await deleteDoc(doc(services.db, "marketing_expenses", id));
      setSuccessMsg("Expense deleted successfully.");
      setTimeout(() => setSuccessMsg(null), 2500);
    } catch (err) {
      console.error("Delete error:", err);
      alert("Failed to delete expense.");
    }
  };

  const exportCSV = () => {
    const headers = [
      "ID",
      "Date",
      "Title",
      "Category",
      "Amount (INR)",
      "Location",
      "Notes",
      "Estimated Users Gained",
    ];
    const rows = filteredExpenses.map((e) => [
      e.id,
      e.date,
      `"${e.title.replace(/"/g, '""')}"`,
      CATEGORY_CONFIG[e.category]?.label || e.category,
      e.amount,
      `"${e.location.replace(/"/g, '""')}"`,
      `"${e.notes.replace(/"/g, '""')}"`,
      e.usersGainedEstimate || 0,
    ]);

    const csvContent =
      "data:text/csv;charset=utf-8," +
      [headers.join(","), ...rows.map((r) => r.join(","))].join("\n");
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute(
      "download",
      `dutype_marketing_expenses_${new Date().toISOString().split("T")[0]}.csv`
    );
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const formatINR = (val: number) => {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
      maximumFractionDigits: 0,
    }).format(val);
  };

  return (
    <div className="adm-stack">
      {/* Toast Alert */}
      {successMsg && (
        <div className="adm-card" style={{ borderLeft: "4px solid #10b981", background: "#ecfdf5", padding: "12px 18px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10, color: "#065f46", fontSize: "0.875rem", fontWeight: 600 }}>
            <CheckCircle2 style={{ width: 18, height: 18, color: "#10b981", flexShrink: 0 }} />
            <span>{successMsg}</span>
          </div>
        </div>
      )}

      {/* Header Row */}
      <div className="adm-header-row">
        <div>
          <h1 className="adm-page-title">Marketing Expenses & ROI</h1>
          <p className="adm-page-subtitle">
            Track offline and digital CAC against real subscription and service booking revenue.
          </p>
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          <button
            type="button"
            className="adm-btn adm-btn-secondary"
            onClick={exportCSV}
            disabled={filteredExpenses.length === 0}
          >
            <Download style={{ width: 15, height: 15 }} />
            Export CSV
          </button>
          <button
            type="button"
            className="adm-btn adm-btn-primary"
            onClick={openAddModal}
          >
            <Plus style={{ width: 16, height: 16 }} />
            Log New Expense
          </button>
        </div>
      </div>

      {/* Top Metrics Grid */}
      <div className="adm-metrics-grid">
        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Total Marketing Spend</span>
            <div className="adm-metric-icon" style={{ background: "#fef2f2", color: "#ef4444" }}>
              <DollarSign style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{formatINR(totalSpent)}</div>
          <div className="adm-metric-sub">
            <span>{expenses.length} recorded line items</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Platform Gross Revenue</span>
            <div className="adm-metric-icon" style={{ background: "#ecfdf5", color: "#10b981" }}>
              <TrendingUp style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{formatINR(totalRevenue)}</div>
          <div className="adm-metric-sub">
            <span>₹{subscriptionRevenue.toLocaleString("en-IN")} subs · ₹{serviceRevenue.toLocaleString("en-IN")} svcs</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Net Return (ROI)</span>
            <div className="adm-metric-icon" style={{ background: netROI >= 0 ? "#ecfdf5" : "#fef2f2", color: netROI >= 0 ? "#10b981" : "#ef4444" }}>
              {netROI >= 0 ? <TrendingUp style={{ width: 18, height: 18 }} /> : <TrendingDown style={{ width: 18, height: 18 }} />}
            </div>
          </div>
          <div className="adm-metric-value" style={{ color: netROI >= 0 ? "#059669" : "#dc2626" }}>
            {netROI >= 0 ? `+${roiPercent}%` : `${roiPercent}%`}
          </div>
          <div className="adm-metric-sub">
            <span>{netROI >= 0 ? `+${formatINR(netROI)} profit` : `${formatINR(Math.abs(netROI))} deficit`}</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Blended Unit CAC</span>
            <div className="adm-metric-icon" style={{ background: "#eff6ff", color: "#2563eb" }}>
              <Users style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">₹{blendedCAC}</div>
          <div className="adm-metric-sub">
            <span>per registered user ({totalUsers} total users)</span>
          </div>
        </div>
      </div>

      {/* Category Distribution Card */}
      <div className="adm-card">
        <div className="adm-card-header">
          <h2 className="adm-card-title">
            <PieChart style={{ width: 18, height: 18, color: "var(--adm-primary)" }} />
            Spend Distribution by Channel
          </h2>
          <span style={{ fontSize: "0.8rem", color: "var(--adm-text-muted)" }}>
            Breakdown across print, digital, and grassroots channels
          </span>
        </div>
        <div className="adm-card-body" style={{ padding: "16px 22px" }}>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 12 }}>
            {(Object.keys(CATEGORY_CONFIG) as ExpenseCategory[]).map((cat) => {
              const item = categoryStats[cat];
              const pct = totalSpent > 0 ? Math.round((item.total / totalSpent) * 100) : 0;
              const cfg = CATEGORY_CONFIG[cat];
              return (
                <div
                  key={cat}
                  style={{
                    padding: "10px 14px",
                    borderRadius: 8,
                    background: "var(--adm-surface-subtle)",
                    border: "1px solid var(--adm-border)",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <span style={{ fontSize: "0.8rem", fontWeight: 600, color: "var(--adm-text-main)" }}>
                      {cfg.icon} {cfg.label}
                    </span>
                    <span className={`adm-badge ${cfg.badgeClass}`}>{pct}%</span>
                  </div>
                  <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.75rem", color: "var(--adm-text-muted)" }}>
                    <span style={{ fontWeight: 600, color: "var(--adm-text-main)" }}>{formatINR(item.total)}</span>
                    <span>{item.count} campaigns</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Toolbar / Search & Filter */}
      <div className="adm-toolbar">
        <div className="adm-search-wrap">
          <Search className="adm-search-icon" />
          <input
            type="text"
            className="adm-input-search"
            placeholder="Search campaigns, titles, notes or locations..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <select
          className="adm-select"
          value={selectedCategory}
          onChange={(e) => setSelectedCategory(e.target.value)}
        >
          <option value="ALL">All Channels</option>
          {(Object.keys(CATEGORY_CONFIG) as ExpenseCategory[]).map((cat) => (
            <option key={cat} value={cat}>
              {CATEGORY_CONFIG[cat].icon} {CATEGORY_CONFIG[cat].label}
            </option>
          ))}
        </select>
        <span style={{ fontSize: "0.825rem", color: "var(--adm-text-muted)", marginLeft: "auto", fontWeight: 500 }}>
          Showing <strong>{filteredExpenses.length}</strong> of {expenses.length} records
        </span>
      </div>

      {/* Data Table */}
      {loading ? (
        <div className="adm-card" style={{ padding: 32, display: "flex", flexDirection: "column", gap: 12 }}>
          <div className="adm-skeleton" style={{ height: 28, width: "30%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
        </div>
      ) : filteredExpenses.length === 0 ? (
        <div className="adm-card" style={{ padding: "48px 24px", textAlign: "center", display: "flex", flexDirection: "column", alignItems: "center", gap: 12 }}>
          <Receipt style={{ width: 44, height: 44, color: "var(--adm-text-subtle)" }} />
          <h3 style={{ margin: 0, fontSize: "1.1rem", color: "var(--adm-text-main)" }}>No expenses found</h3>
          <p style={{ margin: 0, fontSize: "0.875rem", color: "var(--adm-text-muted)", maxWidth: 380 }}>
            {searchQuery || selectedCategory !== "ALL"
              ? "No line items matched your active search query or channel filter."
              : "No marketing expenses recorded yet. Click 'Log New Expense' to record your first print run or campaign."}
          </p>
          {searchQuery || selectedCategory !== "ALL" ? (
            <button
              type="button"
              className="adm-btn adm-btn-secondary adm-btn-sm"
              onClick={() => {
                setSearchQuery("");
                setSelectedCategory("ALL");
              }}
            >
              Reset Filters
            </button>
          ) : (
            <button
              type="button"
              className="adm-btn adm-btn-primary adm-btn-sm"
              onClick={openAddModal}
            >
              <Plus style={{ width: 14, height: 14 }} /> Log Expense
            </button>
          )}
        </div>
      ) : (
        <div className="adm-table-wrap">
          <table className="adm-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Expense Title</th>
                <th>Channel</th>
                <th>Location</th>
                <th>Users Gained</th>
                <th>Amount (₹)</th>
                <th>Receipt</th>
                <th style={{ textAlign: "right" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredExpenses.map((exp) => {
                const cfg = CATEGORY_CONFIG[exp.category] || CATEGORY_CONFIG.OTHER;
                return (
                  <tr key={exp.id}>
                    <td style={{ whiteSpace: "nowrap", fontWeight: 500, color: "var(--adm-text-muted)" }}>
                      {exp.date}
                    </td>
                    <td>
                      <div style={{ fontWeight: 600, color: "var(--adm-text-main)" }}>{exp.title}</div>
                      {exp.notes ? (
                        <div style={{ fontSize: "0.775rem", color: "var(--adm-text-muted)", marginTop: 2 }}>
                          {exp.notes}
                        </div>
                      ) : null}
                    </td>
                    <td>
                      <span className={`adm-badge ${cfg.badgeClass}`}>
                        {cfg.icon} {cfg.label}
                      </span>
                    </td>
                    <td>
                      <span style={{ display: "inline-flex", alignItems: "center", gap: 4, fontSize: "0.8rem", color: "var(--adm-text-muted)" }}>
                        <MapPin style={{ width: 13, height: 13, color: "var(--adm-text-subtle)" }} />
                        {exp.location}
                      </span>
                    </td>
                    <td>
                      {exp.usersGainedEstimate ? (
                        <span className="adm-badge adm-badge-emerald">
                          +{exp.usersGainedEstimate} users
                        </span>
                      ) : (
                        <span style={{ color: "var(--adm-text-subtle)", fontSize: "0.8rem" }}>—</span>
                      )}
                    </td>
                    <td>
                      <strong style={{ fontSize: "0.95rem", color: "var(--adm-text-main)" }}>
                        {formatINR(exp.amount)}
                      </strong>
                    </td>
                    <td>
                      {exp.receiptUrl ? (
                        <a
                          href={exp.receiptUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="adm-btn adm-btn-ghost adm-btn-sm"
                          style={{ padding: "4px 8px" }}
                          title="View Bill / Receipt"
                        >
                          <Receipt style={{ width: 14, height: 14 }} />
                          <ExternalLink style={{ width: 12, height: 12 }} />
                        </a>
                      ) : (
                        <span style={{ color: "var(--adm-text-subtle)", fontSize: "0.8rem" }}>—</span>
                      )}
                    </td>
                    <td style={{ textAlign: "right" }}>
                      <div style={{ display: "inline-flex", gap: 6 }}>
                        <button
                          type="button"
                          className="adm-btn-icon"
                          title="Edit Expense"
                          onClick={() => openEditModal(exp)}
                        >
                          <Edit3 style={{ width: 14, height: 14 }} />
                        </button>
                        <button
                          type="button"
                          className="adm-btn-icon"
                          style={{ color: "#ef4444" }}
                          title="Delete Expense"
                          onClick={() => handleDeleteExpense(exp.id, exp.title)}
                        >
                          <Trash2 style={{ width: 14, height: 14 }} />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Add / Edit Modal */}
      {showAddModal && (
        <div className="adm-modal-backdrop" onClick={() => !submitting && setShowAddModal(false)}>
          <div className="adm-modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="adm-card-header">
              <h3 className="adm-card-title">
                {editingExpense ? "Edit Expense Entry" : "Log Marketing Expense"}
              </h3>
              <button
                type="button"
                className="adm-btn-icon"
                disabled={submitting}
                onClick={() => setShowAddModal(false)}
              >
                <X style={{ width: 16, height: 16 }} />
              </button>
            </div>
            <form onSubmit={handleSaveExpense}>
              <div className="adm-card-body" style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                {errorMsg && (
                  <div style={{ padding: "10px 14px", background: "#fef2f2", border: "1px solid #fecdd3", borderRadius: 8, color: "#b91c1c", fontSize: "0.825rem", display: "flex", alignItems: "center", gap: 8 }}>
                    <AlertCircle style={{ width: 16, height: 16, flexShrink: 0 }} />
                    <span>{errorMsg}</span>
                  </div>
                )}

                <div className="adm-form-group">
                  <label className="adm-form-label">Campaign / Expense Title *</label>
                  <input
                    type="text"
                    className="adm-input"
                    placeholder="e.g. 5,000 Wall Posters printing at Khammam Bus Stand"
                    value={formTitle}
                    onChange={(e) => setFormTitle(e.target.value)}
                    required
                  />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                  <div className="adm-form-group">
                    <label className="adm-form-label">Channel / Category *</label>
                    <select
                      className="adm-select"
                      value={formCategory}
                      onChange={(e) => setFormCategory(e.target.value as ExpenseCategory)}
                    >
                      {(Object.keys(CATEGORY_CONFIG) as ExpenseCategory[]).map((cat) => (
                        <option key={cat} value={cat}>
                          {CATEGORY_CONFIG[cat].icon} {CATEGORY_CONFIG[cat].label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="adm-form-group">
                    <label className="adm-form-label">Amount (₹ INR) *</label>
                    <input
                      type="number"
                      min="1"
                      step="any"
                      className="adm-input"
                      placeholder="e.g. 3500"
                      value={formAmount}
                      onChange={(e) => setFormAmount(e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                  <div className="adm-form-group">
                    <label className="adm-form-label">Expense Date *</label>
                    <input
                      type="date"
                      className="adm-input"
                      value={formDate}
                      onChange={(e) => setFormDate(e.target.value)}
                      required
                    />
                  </div>

                  <div className="adm-form-group">
                    <label className="adm-form-label">Location / Target Area</label>
                    <input
                      type="text"
                      className="adm-input"
                      placeholder="e.g. Khammam, Wyra, Kothagudem"
                      value={formLocation}
                      onChange={(e) => setFormLocation(e.target.value)}
                    />
                  </div>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                  <div className="adm-form-group">
                    <label className="adm-form-label">Estimated Users Acquired</label>
                    <input
                      type="number"
                      min="0"
                      className="adm-input"
                      placeholder="e.g. 150"
                      value={formUsersEstimate}
                      onChange={(e) => setFormUsersEstimate(e.target.value)}
                    />
                  </div>

                  <div className="adm-form-group">
                    <label className="adm-form-label">Receipt / Invoice Link (URL)</label>
                    <input
                      type="url"
                      className="adm-input"
                      placeholder="https://drive.google.com/..."
                      value={formReceiptUrl}
                      onChange={(e) => setFormReceiptUrl(e.target.value)}
                    />
                  </div>
                </div>

                <div className="adm-form-group">
                  <label className="adm-form-label">Campaign Notes / Vendor Details</label>
                  <textarea
                    className="adm-textarea"
                    rows={3}
                    placeholder="Vendor phone, print quality notes, auto rickshaw driver name, etc."
                    value={formNotes}
                    onChange={(e) => setFormNotes(e.target.value)}
                  />
                </div>
              </div>

              <div style={{ padding: "14px 22px", borderTop: "1px solid var(--adm-border)", display: "flex", justifyContent: "flex-end", gap: 10, background: "#f8fafc" }}>
                <button
                  type="button"
                  className="adm-btn adm-btn-secondary"
                  disabled={submitting}
                  onClick={() => setShowAddModal(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="adm-btn adm-btn-primary"
                  disabled={submitting}
                >
                  {submitting ? "Saving..." : editingExpense ? "Update Expense" : "Save Expense"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
