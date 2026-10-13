"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Copy, Check, Phone, ExternalLink, ShieldAlert, ShieldCheck, RefreshCw, Search, Wallet, TrendingUp, ArrowDownCircle, Users } from "lucide-react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import type { ReferralStatRow } from "@/app/api/admin/referral-stats/route";

type SummaryMetrics = {
  totalWallets: number;
  totalBalanceRupees: number;
  totalEarnedRupees: number;
  totalWithdrawnRupees: number;
  totalReferrals: number;
};

type Payload = {
  items?: ReferralStatRow[];
  summary?: SummaryMetrics;
  nextCursor?: string | null;
  error?: string;
};

export function AdminReferralStatsClient() {
  const [items, setItems] = useState<ReferralStatRow[]>([]);
  const [summary, setSummary] = useState<SummaryMetrics | null>(null);
  const [nextCursor, setNextCursor] = useState<string | null>(null);

  const [searchQuery, setSearchQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState<"" | "WORKER" | "EMPLOYER">("");
  const [statusFilter, setStatusFilter] = useState<"" | "false" | "true">("");

  const [loading, setLoading] = useState(true);
  const [busyUid, setBusyUid] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);
  const [copiedPhone, setCopiedPhone] = useState<string | null>(null);
  const [copiedUid, setCopiedUid] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const load = useCallback(async (params: { q?: string; role?: string; blocked?: string; after?: string; append?: boolean }) => {
    setLoading(true);
    setError(null);
    try {
      const sp = new URLSearchParams();
      if (params.q?.trim()) sp.set("q", params.q.trim());
      if (params.role) sp.set("role", params.role);
      if (params.blocked) sp.set("blocked", params.blocked);
      if (params.after) sp.set("after", params.after);
      sp.set("t", String(Date.now()));

      const res = await adminApiFetch(`/api/admin/referral-stats?${sp.toString()}`, { cache: "no-store" });
      const data = (await res.json()) as Payload;
      if (!res.ok) throw new Error(data.error || "Failed to load referral stats.");

      setItems((prev) => (params.append ? [...prev, ...(data.items ?? [])] : data.items ?? []));
      if (data.summary) setSummary(data.summary);
      setNextCursor(data.nextCursor ?? null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load referral stats.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load({ role: roleFilter, blocked: statusFilter });
  }, [roleFilter, statusFilter, load]);

  function handleSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    void load({ q: searchQuery, role: roleFilter, blocked: statusFilter });
  }

  function handleClearSearch() {
    setSearchQuery("");
    void load({ q: "", role: roleFilter, blocked: statusFilter });
  }

  async function toggleBlocked(row: ReferralStatRow) {
    const nextBlocked = !row.blocked;
    const actionLabel = nextBlocked ? "Block & Freeze Wallet" : "Unblock Wallet";
    if (!window.confirm(`${actionLabel} for ${row.name || row.phone || row.uid}?`)) return;

    setBusyUid(row.uid);
    setError(null);
    try {
      const res = await adminApiFetch("/api/admin/referral-stats", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ uid: row.uid, blocked: nextBlocked })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Failed to update wallet status.");

      setItems((prev) =>
        prev.map((item) => (item.uid === row.uid ? { ...item, blocked: nextBlocked } : item))
      );
      setMessage(`Wallet for ${row.name || row.phone || row.uid} is now ${nextBlocked ? "BLOCKED" : "ACTIVE"}.`);
      setTimeout(() => setMessage(null), 4000);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update wallet.");
    } finally {
      setBusyUid(null);
    }
  }

  function copyText(text: string, type: "code" | "phone" | "uid") {
    if (!navigator.clipboard) return;
    navigator.clipboard.writeText(text);
    if (type === "code") {
      setCopiedCode(text);
      setTimeout(() => setCopiedCode(null), 2000);
    } else if (type === "phone") {
      setCopiedPhone(text);
      setTimeout(() => setCopiedPhone(null), 2000);
    } else {
      setCopiedUid(text);
      setTimeout(() => setCopiedUid(null), 2000);
    }
  }

  return (
    <div className="space-y-6">
      {/* Messages */}
      {message && (
        <div className="rounded-lg bg-emerald-50 border border-emerald-200 p-4 text-sm text-emerald-800 flex items-center gap-2">
          <Check className="h-4 w-4 text-emerald-600 shrink-0" />
          <span>{message}</span>
        </div>
      )}
      {error && (
        <div className="rounded-lg bg-red-50 border border-red-200 p-4 text-sm text-red-800 flex items-center justify-between">
          <span>{error}</span>
          <button onClick={() => setError(null)} className="text-red-600 hover:text-red-800 text-xs font-semibold">
            Dismiss
          </button>
        </div>
      )}

      {/* Summary KPI Cards */}
      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
          <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex items-center gap-2 text-xs font-medium text-slate-500">
              <Users className="h-4 w-4 text-slate-400" />
              <span>Total Wallets</span>
            </div>
            <div className="mt-2 text-2xl font-bold text-slate-900">{summary.totalWallets}</div>
            <div className="mt-1 text-xs text-slate-400">Registered users</div>
          </div>

          <div className="rounded-xl border border-emerald-200 bg-emerald-50/50 p-4 shadow-sm">
            <div className="flex items-center gap-2 text-xs font-medium text-emerald-700">
              <Wallet className="h-4 w-4 text-emerald-600" />
              <span>Current Balances</span>
            </div>
            <div className="mt-2 text-2xl font-bold text-emerald-800">₹{summary.totalBalanceRupees.toLocaleString("en-IN")}</div>
            <div className="mt-1 text-xs text-emerald-600">Available to withdraw</div>
          </div>

          <div className="rounded-xl border border-blue-200 bg-blue-50/50 p-4 shadow-sm">
            <div className="flex items-center gap-2 text-xs font-medium text-blue-700">
              <TrendingUp className="h-4 w-4 text-blue-600" />
              <span>Lifetime Earned</span>
            </div>
            <div className="mt-2 text-2xl font-bold text-blue-800">₹{summary.totalEarnedRupees.toLocaleString("en-IN")}</div>
            <div className="mt-1 text-xs text-blue-600">Total credited to date</div>
          </div>

          <div className="rounded-xl border border-purple-200 bg-purple-50/50 p-4 shadow-sm">
            <div className="flex items-center gap-2 text-xs font-medium text-purple-700">
              <ArrowDownCircle className="h-4 w-4 text-purple-600" />
              <span>Total Withdrawn</span>
            </div>
            <div className="mt-2 text-2xl font-bold text-purple-800">₹{summary.totalWithdrawnRupees.toLocaleString("en-IN")}</div>
            <div className="mt-1 text-xs text-purple-600">Successfully cashed out</div>
          </div>

          <div className="rounded-xl border border-amber-200 bg-amber-50/50 p-4 shadow-sm col-span-2 md:col-span-1">
            <div className="flex items-center gap-2 text-xs font-medium text-amber-700">
              <Users className="h-4 w-4 text-amber-600" />
              <span>Total Referrals</span>
            </div>
            <div className="mt-2 text-2xl font-bold text-amber-800">{summary.totalReferrals}</div>
            <div className="mt-1 text-xs text-amber-600">Successful conversions</div>
          </div>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm space-y-3">
        <form onSubmit={handleSearchSubmit} className="flex flex-col md:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search by Mobile number (+91...), Referral Code, Name, or UID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
            />
          </div>

          <div className="flex items-center gap-2">
            <select
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value as "" | "WORKER" | "EMPLOYER")}
              className="px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
            >
              <option value="">All Roles</option>
              <option value="WORKER">Workers Only</option>
              <option value="EMPLOYER">Employers Only</option>
            </select>

            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as "" | "false" | "true")}
              className="px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
            >
              <option value="">All Statuses</option>
              <option value="false">Active Only</option>
              <option value="true">Blocked Only</option>
            </select>

            <button
              type="submit"
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-medium text-sm rounded-lg transition-colors flex items-center gap-1.5"
            >
              <Search className="h-4 w-4" />
              <span>Search</span>
            </button>

            {searchQuery && (
              <button
                type="button"
                onClick={handleClearSearch}
                className="px-3 py-2 border border-slate-300 hover:bg-slate-50 text-slate-700 font-medium text-sm rounded-lg transition-colors"
              >
                Clear
              </button>
            )}

            <button
              type="button"
              onClick={() => void load({ q: searchQuery, role: roleFilter, blocked: statusFilter })}
              title="Refresh"
              className="p-2 border border-slate-300 hover:bg-slate-50 text-slate-600 rounded-lg transition-colors"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin text-emerald-600" : ""}`} />
            </button>
          </div>
        </form>
      </div>

      {/* Main Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-xs font-semibold text-slate-600 uppercase tracking-wider">
              <tr>
                <th className="py-3.5 px-4">User & Phone</th>
                <th className="py-3.5 px-4">Role</th>
                <th className="py-3.5 px-4">Referral Code</th>
                <th className="py-3.5 px-4 text-right">Balance (₹)</th>
                <th className="py-3.5 px-4 text-right">Lifetime Earned (₹)</th>
                <th className="py-3.5 px-4 text-right">Withdrawn (₹)</th>
                <th className="py-3.5 px-4 text-center">Referrals</th>
                <th className="py-3.5 px-4 text-center">Status</th>
                <th className="py-3.5 px-4 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {items.length === 0 && !loading && (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-slate-500">
                    No referral stats found matching the filters.
                  </td>
                </tr>
              )}

              {items.map((row) => (
                <tr key={row.uid} className="hover:bg-slate-50/70 transition-colors">
                  {/* User & Phone */}
                  <td className="py-3.5 px-4">
                    <div className="font-semibold text-slate-900">
                      {row.name || row.resolvedName || <span className="text-slate-400 italic">No name provided</span>}
                    </div>
                    {row.phone ? (
                      <div className="flex items-center gap-1.5 mt-0.5">
                        <a
                          href={`tel:${row.phone}`}
                          className="font-mono text-xs text-emerald-700 hover:underline flex items-center gap-1"
                        >
                          <Phone className="h-3 w-3" />
                          <span>{row.phone}</span>
                        </a>
                        <button
                          type="button"
                          onClick={() => copyText(row.phone, "phone")}
                          className="p-0.5 text-slate-400 hover:text-slate-700 rounded transition-colors"
                          title="Copy phone"
                        >
                          {copiedPhone === row.phone ? (
                            <Check className="h-3 w-3 text-emerald-600" />
                          ) : (
                            <Copy className="h-3 w-3" />
                          )}
                        </button>
                      </div>
                    ) : (
                      <div className="text-xs text-slate-400 font-mono">UID: {row.uid.slice(0, 10)}...</div>
                    )}
                  </td>

                  {/* Role */}
                  <td className="py-3.5 px-4">
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold ${
                        row.role === "EMPLOYER"
                          ? "bg-purple-100 text-purple-800"
                          : "bg-blue-100 text-blue-800"
                      }`}
                    >
                      {row.role}
                    </span>
                  </td>

                  {/* Referral Code */}
                  <td className="py-3.5 px-4">
                    {row.referralCode ? (
                      <div className="flex items-center gap-1.5">
                        <Link
                          href={`/admin/referral-codes?q=${row.referralCode}`}
                          className="font-mono font-bold text-xs bg-slate-100 hover:bg-slate-200 text-slate-800 px-2 py-1 rounded transition-colors flex items-center gap-1"
                          title="View code in Referral Codes"
                        >
                          <span>{row.referralCode}</span>
                          <ExternalLink className="h-3 w-3 text-slate-400" />
                        </Link>
                        <button
                          type="button"
                          onClick={() => copyText(row.referralCode, "code")}
                          className="p-1 text-slate-400 hover:text-slate-700 rounded transition-colors"
                          title="Copy Code"
                        >
                          {copiedCode === row.referralCode ? (
                            <Check className="h-3.5 w-3.5 text-emerald-600" />
                          ) : (
                            <Copy className="h-3.5 w-3.5" />
                          )}
                        </button>
                      </div>
                    ) : (
                      <span className="text-slate-400 text-xs italic">None</span>
                    )}
                  </td>

                  {/* Balance in Rupees */}
                  <td className="py-3.5 px-4 text-right">
                    <span
                      className={`font-semibold font-mono text-sm ${
                        row.balanceRupees > 0
                          ? "text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded"
                          : "text-slate-600"
                      }`}
                    >
                      ₹{row.balanceRupees.toLocaleString("en-IN", { minimumFractionDigits: 0, maximumFractionDigits: 2 })}
                    </span>
                  </td>

                  {/* Lifetime Earned in Rupees */}
                  <td className="py-3.5 px-4 text-right font-mono text-slate-800">
                    ₹{row.lifetimeEarnedRupees.toLocaleString("en-IN", { minimumFractionDigits: 0, maximumFractionDigits: 2 })}
                  </td>

                  {/* Withdrawn in Rupees */}
                  <td className="py-3.5 px-4 text-right font-mono text-slate-500">
                    ₹{row.withdrawnRupees.toLocaleString("en-IN", { minimumFractionDigits: 0, maximumFractionDigits: 2 })}
                  </td>

                  {/* Successful Referrals */}
                  <td className="py-3.5 px-4 text-center">
                    <span className="inline-flex items-center justify-center min-w-[24px] h-6 px-1.5 rounded-full text-xs font-bold bg-amber-100 text-amber-800">
                      {row.successfulReferrals}
                    </span>
                  </td>

                  {/* Status */}
                  <td className="py-3.5 px-4 text-center">
                    {row.blocked ? (
                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-800">
                        <ShieldAlert className="h-3 w-3" />
                        <span>Blocked</span>
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
                        <ShieldCheck className="h-3 w-3" />
                        <span>Active</span>
                      </span>
                    )}
                  </td>

                  {/* Actions */}
                  <td className="py-3.5 px-4 text-center">
                    <div className="flex items-center justify-center gap-1.5">
                      <button
                        type="button"
                        onClick={() => toggleBlocked(row)}
                        disabled={busyUid === row.uid}
                        className={`text-xs px-2.5 py-1 rounded font-medium transition-colors ${
                          row.blocked
                            ? "bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-300"
                            : "bg-red-50 hover:bg-red-100 text-red-700 border border-red-200"
                        }`}
                      >
                        {busyUid === row.uid ? "..." : row.blocked ? "Unblock" : "Block"}
                      </button>
                      <button
                        type="button"
                        onClick={() => copyText(row.uid, "uid")}
                        className="p-1 text-slate-400 hover:text-slate-700 rounded transition-colors"
                        title="Copy UID"
                      >
                        {copiedUid === row.uid ? (
                          <Check className="h-3.5 w-3.5 text-emerald-600" />
                        ) : (
                          <Copy className="h-3.5 w-3.5" />
                        )}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Load More Pagination */}
        {nextCursor && (
          <div className="p-4 bg-slate-50 border-t border-slate-200 flex justify-center">
            <button
              type="button"
              onClick={() => void load({ q: searchQuery, role: roleFilter, blocked: statusFilter, after: nextCursor, append: true })}
              disabled={loading}
              className="px-4 py-2 border border-slate-300 hover:bg-white text-slate-700 text-sm font-medium rounded-lg transition-colors shadow-sm disabled:opacity-50"
            >
              {loading ? "Loading..." : "Load More Users"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
