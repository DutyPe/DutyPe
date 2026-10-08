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
  Edit2,
  Download,
  Search,
  Filter,
  CheckCircle2,
  AlertCircle,
  Calendar,
  MapPin,
  FileText,
  PieChart,
  Layers,
  ArrowUpRight,
  Sparkles,
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
  { label: string; icon: string; color: string; bg: string; border: string }
> = {
  POSTERS: {
    label: "Posters & Banners",
    icon: "🖨️",
    color: "text-amber-400",
    bg: "bg-amber-500/10",
    border: "border-amber-500/20",
  },
  DIGITAL_ADS: {
    label: "Meta / Instagram Ads",
    icon: "📱",
    color: "text-blue-400",
    bg: "bg-blue-500/10",
    border: "border-blue-500/20",
  },
  INFLUENCERS: {
    label: "Influencer Marketing",
    icon: "🌟",
    color: "text-purple-400",
    bg: "bg-purple-500/10",
    border: "border-purple-500/20",
  },
  FIELD_DESK: {
    label: "Field Desks & Canopies",
    icon: "⛱️",
    color: "text-emerald-400",
    bg: "bg-emerald-500/10",
    border: "border-emerald-500/20",
  },
  AUTO_PA: {
    label: "Auto Rickshaw PA Audio",
    icon: "📣",
    color: "text-rose-400",
    bg: "bg-rose-500/10",
    border: "border-rose-500/20",
  },
  WHATSAPP_SMS: {
    label: "WhatsApp / SMS Broadcasts",
    icon: "💬",
    color: "text-teal-400",
    bg: "bg-teal-500/10",
    border: "border-teal-500/20",
  },
  REFERRAL_PROMO: {
    label: "Referral & Bonus Rewards",
    icon: "🎁",
    color: "text-pink-400",
    bg: "bg-pink-500/10",
    border: "border-pink-500/20",
  },
  OTHER: {
    label: "Other Expenses",
    icon: "📦",
    color: "text-slate-400",
    bg: "bg-slate-500/10",
    border: "border-slate-500/20",
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
  const [dateFilter, setDateFilter] = useState<string>("ALL");

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
        setSuccessMsg("Expense added successfully!");
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

  return (
    <div className="space-y-6">
      {/* Toast notifications */}
      {successMsg && (
        <div className="flex items-center gap-3 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 px-4 py-3 rounded-xl animate-in fade-in duration-200">
          <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
          <p className="text-sm font-medium">{successMsg}</p>
        </div>
      )}

      {/* Top Header & Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            <span>Marketing Spend &amp; Revenue ROI</span>
            <span className="text-xs px-2.5 py-1 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20 font-semibold">
              Live Tracker
            </span>
          </h2>
          <p className="text-sm text-slate-400 mt-1">
            Log marketing investments (posters, ads, influencers) and compare directly with platform revenue and user growth.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={exportCSV}
            className="flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-medium bg-slate-800 text-slate-200 border border-slate-700 hover:bg-slate-700 transition"
          >
            <Download className="w-4 h-4" />
            <span>Export CSV</span>
          </button>
          <button
            onClick={openAddModal}
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold bg-emerald-500 text-slate-950 hover:bg-emerald-400 shadow-lg shadow-emerald-500/20 transition"
          >
            <Plus className="w-4 h-4" />
            <span>Add Expense</span>
          </button>
        </div>
      </div>

      {/* KPI Overview Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        {/* Total Spent */}
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Total Spent
            </span>
            <span className="p-2 rounded-xl bg-rose-500/10 text-rose-400 border border-rose-500/20">
              <DollarSign className="w-4 h-4" />
            </span>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-black text-white">
              ₹{totalSpent.toLocaleString("en-IN")}
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Across {expenses.length} campaigns &amp; items
            </p>
          </div>
        </div>

        {/* Total Revenue */}
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Total Revenue
            </span>
            <span className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <TrendingUp className="w-4 h-4" />
            </span>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-black text-emerald-400">
              ₹{totalRevenue.toLocaleString("en-IN")}
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Subs: ₹{subscriptionRevenue.toLocaleString()} · Services: ₹{serviceRevenue.toLocaleString()}
            </p>
          </div>
        </div>

        {/* Net Profit / Net ROI */}
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Net ROI
            </span>
            <span
              className={`p-2 rounded-xl border ${
                netROI >= 0
                  ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20"
                  : "bg-amber-500/10 text-amber-400 border-amber-500/20"
              }`}
            >
              {netROI >= 0 ? (
                <ArrowUpRight className="w-4 h-4" />
              ) : (
                <TrendingDown className="w-4 h-4" />
              )}
            </span>
          </div>
          <div className="mt-3">
            <div
              className={`text-2xl font-black ${
                netROI >= 0 ? "text-emerald-400" : "text-amber-400"
              }`}
            >
              {netROI >= 0 ? "+" : ""}₹{netROI.toLocaleString("en-IN")}
            </div>
            <p className="text-xs text-slate-400 mt-1">
              ROI: {roiPercent >= 0 ? `+${roiPercent}%` : `${roiPercent}%`}
            </p>
          </div>
        </div>

        {/* Users Acquired */}
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Users Acquired
            </span>
            <span className="p-2 rounded-xl bg-blue-500/10 text-blue-400 border border-blue-500/20">
              <Users className="w-4 h-4" />
            </span>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-black text-blue-400">
              {totalUsers.toLocaleString("en-IN")}
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Registered workers &amp; employers
            </p>
          </div>
        </div>

        {/* Blended CAC */}
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Blended CAC
            </span>
            <span className="p-2 rounded-xl bg-purple-500/10 text-purple-400 border border-purple-500/20">
              <Sparkles className="w-4 h-4" />
            </span>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-black text-purple-400">
              ₹{blendedCAC.toLocaleString("en-IN")}
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Per acquired user in Khammam
            </p>
          </div>
        </div>
      </div>

      {/* Category Spend Breakdown */}
      <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <PieChart className="w-4 h-4 text-emerald-400" />
            <span>Spend Distribution by Channel</span>
          </h3>
          <span className="text-xs text-slate-400">
            Total Allocated: ₹{totalSpent.toLocaleString("en-IN")}
          </span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-3 pt-1">
          {(Object.keys(CATEGORY_CONFIG) as ExpenseCategory[]).map((catKey) => {
            const conf = CATEGORY_CONFIG[catKey];
            const data = categoryStats[catKey];
            const percentage =
              totalSpent > 0 ? Math.round((data.total / totalSpent) * 100) : 0;

            return (
              <div
                key={catKey}
                onClick={() =>
                  setSelectedCategory(selectedCategory === catKey ? "ALL" : catKey)
                }
                className={`p-3 rounded-xl border cursor-pointer transition text-center ${
                  selectedCategory === catKey
                    ? `${conf.bg} ${conf.border} ring-2 ring-emerald-500/40`
                    : "bg-slate-950/60 border-slate-800/80 hover:bg-slate-800/40"
                }`}
              >
                <div className="text-xl mb-1">{conf.icon}</div>
                <div className="text-xs font-semibold text-slate-300 truncate">
                  {conf.label}
                </div>
                <div className={`text-sm font-bold mt-1 ${conf.color}`}>
                  ₹{data.total.toLocaleString("en-IN")}
                </div>
                <div className="text-[10px] text-slate-500 mt-0.5">
                  {percentage}% · {data.count} items
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Controls & Search */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-slate-900/60 p-3 rounded-2xl border border-slate-800">
        <div className="flex-1 relative">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search by title, location, or notes..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-emerald-500 transition"
          />
        </div>

        <div className="flex items-center gap-2">
          <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-950 border border-slate-800 rounded-xl">
            <Filter className="w-3.5 h-3.5 text-slate-400" />
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="bg-transparent text-xs text-slate-300 focus:outline-none cursor-pointer"
            >
              <option value="ALL">All Channels</option>
              {(Object.keys(CATEGORY_CONFIG) as ExpenseCategory[]).map((c) => (
                <option key={c} value={c}>
                  {CATEGORY_CONFIG[c].label}
                </option>
              ))}
            </select>
          </div>

          {(searchQuery || selectedCategory !== "ALL") && (
            <button
              onClick={() => {
                setSearchQuery("");
                setSelectedCategory("ALL");
              }}
              className="text-xs text-slate-400 hover:text-white px-2.5 py-1.5 transition"
            >
              Reset
            </button>
          )}
        </div>
      </div>

      {/* Expenses Table */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900 overflow-hidden">
        {loading ? (
          <div className="py-16 text-center text-slate-400">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-2 border-emerald-500 border-t-transparent mb-3" />
            <p className="text-sm">Loading marketing expenses...</p>
          </div>
        ) : filteredExpenses.length === 0 ? (
          <div className="py-16 text-center text-slate-400 space-y-3">
            <div className="p-3 bg-slate-800/50 rounded-full w-12 h-12 flex items-center justify-center mx-auto text-xl">
              💸
            </div>
            <p className="text-base font-semibold text-slate-200">
              No marketing expenses logged yet
            </p>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              Start by adding your first expense: physical posters, auto announcements, Instagram ads, or influencer collaborations.
            </p>
            <button
              onClick={openAddModal}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold bg-emerald-500 text-slate-950 hover:bg-emerald-400 transition mt-2"
            >
              <Plus className="w-3.5 h-3.5" />
              Add First Expense
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-950/60 text-slate-400 uppercase text-[11px] font-semibold tracking-wider border-b border-slate-800">
                <tr>
                  <th className="py-3.5 px-4">Date</th>
                  <th className="py-3.5 px-4">Campaign / Title</th>
                  <th className="py-3.5 px-4">Category</th>
                  <th className="py-3.5 px-4">Location</th>
                  <th className="py-3.5 px-4">Amount</th>
                  <th className="py-3.5 px-4">Est. Users Gained</th>
                  <th className="py-3.5 px-4">Notes</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {filteredExpenses.map((exp) => {
                  const conf = CATEGORY_CONFIG[exp.category] || CATEGORY_CONFIG.OTHER;
                  return (
                    <tr
                      key={exp.id}
                      className="hover:bg-slate-800/30 transition group"
                    >
                      <td className="py-3.5 px-4 whitespace-nowrap text-xs text-slate-300 font-mono">
                        {exp.date}
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-white">{exp.title}</div>
                        {exp.receiptUrl && (
                          <a
                            href={exp.receiptUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-[11px] text-blue-400 hover:underline flex items-center gap-1 mt-0.5"
                          >
                            <span>Receipt / Proof</span>
                            <ArrowUpRight className="w-3 h-3" />
                          </a>
                        )}
                      </td>
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <span
                          className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${conf.bg} ${conf.color} ${conf.border}`}
                        >
                          <span>{conf.icon}</span>
                          <span>{conf.label}</span>
                        </span>
                      </td>
                      <td className="py-3.5 px-4 whitespace-nowrap text-xs text-slate-300">
                        <span className="flex items-center gap-1">
                          <MapPin className="w-3 h-3 text-slate-500" />
                          <span>{exp.location}</span>
                        </span>
                      </td>
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <span className="font-bold text-white text-base">
                          ₹{exp.amount.toLocaleString("en-IN")}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 whitespace-nowrap text-xs text-slate-300">
                        {exp.usersGainedEstimate ? (
                          <span className="inline-flex items-center gap-1 text-emerald-400 font-medium">
                            <Users className="w-3 h-3" />
                            <span>+{exp.usersGainedEstimate} users</span>
                          </span>
                        ) : (
                          <span className="text-slate-500">—</span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-xs text-slate-400 max-w-xs truncate">
                        {exp.notes || <span className="text-slate-600">—</span>}
                      </td>
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => openEditModal(exp)}
                            className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition"
                            title="Edit"
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleDeleteExpense(exp.id, exp.title)}
                            className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 rounded-lg transition"
                            title="Delete"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
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
      </div>

      {/* Add / Edit Expense Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-lg rounded-2xl bg-slate-900 border border-slate-800 shadow-2xl overflow-hidden">
            <div className="p-5 border-b border-slate-800 flex items-center justify-between">
              <div>
                <h3 className="text-lg font-bold text-white">
                  {editingExpense ? "Edit Marketing Expense" : "Add Marketing Expense"}
                </h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  Track money spent on posters, influencer marketing, digital ads, or field ops.
                </p>
              </div>
              <button
                onClick={() => setShowAddModal(false)}
                className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveExpense} className="p-5 space-y-4">
              {errorMsg && (
                <div className="flex items-center gap-2 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs">
                  <AlertCircle className="w-4 h-4 flex-shrink-0" />
                  <span>{errorMsg}</span>
                </div>
              )}

              {/* Title */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Title / Campaign Name *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. 500 Posters - Wyra Road & Rythu Bazaar"
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-emerald-500 transition"
                />
              </div>

              {/* Category & Amount Row */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Channel / Category *
                  </label>
                  <select
                    value={formCategory}
                    onChange={(e) => setFormCategory(e.target.value as ExpenseCategory)}
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 focus:outline-none focus:border-emerald-500 transition cursor-pointer"
                  >
                    {(Object.keys(CATEGORY_CONFIG) as ExpenseCategory[]).map((k) => (
                      <option key={k} value={k}>
                        {CATEGORY_CONFIG[k].icon} {CATEGORY_CONFIG[k].label}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Amount (₹) *
                  </label>
                  <input
                    type="number"
                    min="1"
                    step="any"
                    required
                    placeholder="e.g. 3500"
                    value={formAmount}
                    onChange={(e) => setFormAmount(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-emerald-500 transition font-bold"
                  />
                </div>
              </div>

              {/* Date & Location Row */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Date of Spend *
                  </label>
                  <input
                    type="date"
                    required
                    value={formDate}
                    onChange={(e) => setFormDate(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 focus:outline-none focus:border-emerald-500 transition cursor-pointer"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Target Area / Location
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Khammam Bus Stand, Wyra"
                    value={formLocation}
                    onChange={(e) => setFormLocation(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-emerald-500 transition"
                  />
                </div>
              </div>

              {/* Estimated Users Gained & Receipt URL */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Est. Users Acquired
                  </label>
                  <input
                    type="number"
                    min="0"
                    placeholder="e.g. 80"
                    value={formUsersEstimate}
                    onChange={(e) => setFormUsersEstimate(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-emerald-500 transition"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Receipt / Proof Link
                  </label>
                  <input
                    type="url"
                    placeholder="https://drive... or bill URL"
                    value={formReceiptUrl}
                    onChange={(e) => setFormReceiptUrl(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-emerald-500 transition"
                  />
                </div>
              </div>

              {/* Notes */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Notes &amp; Outcomes
                </label>
                <textarea
                  rows={2}
                  placeholder="e.g. Distributed with auto driver team; saw high maid & cleaner signups."
                  value={formNotes}
                  onChange={(e) => setFormNotes(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-emerald-500 transition"
                />
              </div>

              {/* Actions */}
              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 rounded-xl text-sm font-medium bg-slate-800 text-slate-300 hover:bg-slate-700 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="flex items-center gap-2 px-5 py-2 rounded-xl text-sm font-semibold bg-emerald-500 text-slate-950 hover:bg-emerald-400 disabled:opacity-50 shadow-lg shadow-emerald-500/20 transition"
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
