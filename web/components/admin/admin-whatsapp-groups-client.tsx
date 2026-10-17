"use client";

import { useCallback, useEffect, useState } from "react";
import { MessageSquare, Download, Copy, Check, Users, Phone, Search, RefreshCw, Send, ShieldCheck, HardHat, Building2, Wrench } from "lucide-react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";

type ContactRow = {
  id: string;
  role: "WORKER" | "EMPLOYER" | "PARTNER";
  name: string;
  businessName?: string;
  phone: string;
  area?: string;
  skills?: string[];
  trade?: string;
  createdAt?: string;
};

export function AdminWhatsappGroupsClient() {
  const [segment, setSegment] = useState<"WORKER" | "EMPLOYER" | "PARTNER">("WORKER");
  const [selectedSkill, setSelectedSkill] = useState<string>("ALL");
  const [search, setSearch] = useState("");
  const [contacts, setContacts] = useState<ContactRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [copiedType, setCopiedType] = useState<string | null>(null);
  const [presetMessage, setPresetMessage] = useState(
    "నమస్కారం! DutyPe లో కొత్త పనులు మరియు అవకాశాలు అందుబాటులో ఉన్నాయి. యాప్ ఓపెన్ చేసి చూడండి: https://dutype.in"
  );

  const fetchContacts = useCallback(async () => {
    setLoading(true);
    try {
      if (segment === "PARTNER") {
        const res = await adminApiFetch("/api/admin/worker-profiles?limit=200", { cache: "no-store" });
        const data = await res.json();
        const profiles = (data.profiles ?? []).filter((p: any) => p.isPartner === true || (Array.isArray(p.categories) && p.categories.length > 0));
        const partners = profiles.map((p: any) => ({
          id: p.id,
          role: "PARTNER" as const,
          name: p.name || "Service Partner",
          phone: p.phone || "",
          area: p.area || "Khammam",
          skills: p.categories || p.skills || [],
          trade: (p.categories || p.skills || []).join(", ")
        }));
        setContacts(partners);
      } else {
        const sp = new URLSearchParams();
        sp.set("role", segment);
        sp.set("limit", "200");
        const res = await adminApiFetch(`/api/admin/users?${sp.toString()}`, { cache: "no-store" });
        const data = await res.json();
        const users = (data.users ?? []).map((u: any) => ({
          id: u.id,
          role: u.role,
          name: u.name || u.businessName || "User",
          businessName: u.businessName,
          phone: u.phone || "",
          area: u.area || "",
          skills: u.skills || [],
          trade: (u.skills || []).join(", ")
        }));
        setContacts(users);
      }
    } catch {
      setContacts([]);
    } finally {
      setLoading(false);
    }
  }, [segment]);

  useEffect(() => {
    void fetchContacts();
  }, [fetchContacts]);

  // Filter contacts
  const filtered = contacts.filter((c) => {
    if (!c.phone) return false;
    if (search.trim()) {
      const q = search.toLowerCase();
      const matchName = c.name.toLowerCase().includes(q) || (c.businessName && c.businessName.toLowerCase().includes(q));
      const matchPhone = c.phone.includes(q);
      const matchArea = c.area && c.area.toLowerCase().includes(q);
      const matchSkill = c.skills && c.skills.some((s) => s.toLowerCase().includes(q));
      if (!matchName && !matchPhone && !matchArea && !matchSkill) return false;
    }
    if (selectedSkill !== "ALL" && c.skills) {
      if (!c.skills.some((s) => s.toUpperCase() === selectedSkill)) return false;
    }
    return true;
  });

  // Unique phone numbers
  const uniquePhones = Array.from(new Set(filtered.map((c) => c.phone.replace(/\D/g, "").slice(-10)).filter((p) => p.length === 10)));

  // Copy comma-separated
  function copyCsvPhones() {
    if (!uniquePhones.length) return;
    navigator.clipboard.writeText(uniquePhones.join(", "));
    setCopiedType("csv");
    setTimeout(() => setCopiedType(null), 2000);
  }

  // Copy with +91
  function copyE164Phones() {
    if (!uniquePhones.length) return;
    navigator.clipboard.writeText(uniquePhones.map((p) => `+91${p}`).join("\n"));
    setCopiedType("e164");
    setTimeout(() => setCopiedType(null), 2000);
  }

  // Export CSV File
  function downloadCsvFile() {
    if (!filtered.length) return;
    const header = "Name,Phone,Role,Trade_Skills,Area\n";
    const rows = filtered
      .map((c) => `"${c.name.replace(/"/g, '""')}","${c.phone}","${c.role}","${(c.skills || []).join(";") }","${c.area || ""}"`)
      .join("\n");
    const blob = new Blob([header + rows], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `DutyPe_${segment}_Contacts_${Date.now()}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  // Export vCard (.vcf)
  function downloadVcfFile() {
    if (!filtered.length) return;
    const vcards = filtered
      .map((c) => {
        const cleanPhone = "+91" + c.phone.replace(/\D/g, "").slice(-10);
        return [
          "BEGIN:VCARD",
          "VERSION:3.0",
          `FN:DutyPe ${c.role} - ${c.name}`,
          `TEL;TYPE=CELL:${cleanPhone}`,
          `NOTE:DutyPe ${c.role} in ${c.area || "Khammam"}. Trade: ${(c.skills || []).join(", ")}`,
          "END:VCARD"
        ].join("\n");
      })
      .join("\n");
    const blob = new Blob([vcards], { type: "text/vcard;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `DutyPe_${segment}_Contacts_${Date.now()}.vcf`;
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="space-y-6">
      {/* Segment Selector Tabs */}
      <div className="flex flex-wrap items-center gap-2 border-b border-slate-200 pb-3">
        <button
          onClick={() => { setSegment("WORKER"); setSelectedSkill("ALL"); }}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-lg font-semibold text-sm transition-colors ${
            segment === "WORKER"
              ? "bg-emerald-600 text-white shadow-sm"
              : "bg-white text-slate-700 hover:bg-slate-100 border border-slate-200"
          }`}
        >
          <HardHat className="h-4 w-4" />
          <span>Workers & Daily Wage</span>
        </button>

        <button
          onClick={() => { setSegment("EMPLOYER"); setSelectedSkill("ALL"); }}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-lg font-semibold text-sm transition-colors ${
            segment === "EMPLOYER"
              ? "bg-purple-600 text-white shadow-sm"
              : "bg-white text-slate-700 hover:bg-slate-100 border border-slate-200"
          }`}
        >
          <Building2 className="h-4 w-4" />
          <span>Employers & Businesses</span>
        </button>

        <button
          onClick={() => { setSegment("PARTNER"); setSelectedSkill("ALL"); }}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-lg font-semibold text-sm transition-colors ${
            segment === "PARTNER"
              ? "bg-blue-600 text-white shadow-sm"
              : "bg-white text-slate-700 hover:bg-slate-100 border border-slate-200"
          }`}
        >
          <Wrench className="h-4 w-4" />
          <span>Doorstep Home Partners</span>
        </button>

        <button
          onClick={() => void fetchContacts()}
          className="ml-auto p-2 border border-slate-300 hover:bg-slate-50 text-slate-600 rounded-lg transition-colors"
          title="Refresh"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin text-emerald-600" : ""}`} />
        </button>
      </div>

      {/* Metrics & Export Action Toolbar */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm">
          <div className="flex items-center gap-2 text-xs font-semibold text-slate-500">
            <Users className="h-4 w-4 text-slate-400" />
            <span>Matched Contacts</span>
          </div>
          <div className="mt-2 text-2xl font-bold text-slate-900">{filtered.length}</div>
          <div className="text-xs text-slate-400">{uniquePhones.length} verified phone numbers</div>
        </div>

        <div className="bg-emerald-50/60 rounded-xl border border-emerald-200 p-4 shadow-sm md:col-span-3 flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="text-sm font-bold text-emerald-900">One-Click Contact Export for Broadcasts</div>
            <div className="text-xs text-emerald-700 mt-0.5">
              Import contacts straight into WhatsApp Business, bulk broadcast tools, or phone address book.
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={copyCsvPhones}
              className="px-3 py-1.5 bg-white border border-emerald-300 hover:bg-emerald-50 text-emerald-800 text-xs font-semibold rounded-lg shadow-sm flex items-center gap-1.5 transition-colors"
            >
              {copiedType === "csv" ? <Check className="h-3.5 w-3.5 text-emerald-600" /> : <Copy className="h-3.5 w-3.5" />}
              <span>{copiedType === "csv" ? "Copied Numbers!" : "Copy Numbers (CSV)"}</span>
            </button>

            <button
              onClick={copyE164Phones}
              className="px-3 py-1.5 bg-white border border-emerald-300 hover:bg-emerald-50 text-emerald-800 text-xs font-semibold rounded-lg shadow-sm flex items-center gap-1.5 transition-colors"
            >
              {copiedType === "e164" ? <Check className="h-3.5 w-3.5 text-emerald-600" /> : <Copy className="h-3.5 w-3.5" />}
              <span>{copiedType === "e164" ? "Copied List!" : "Copy +91 List"}</span>
            </button>

            <button
              onClick={downloadVcfFile}
              className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-lg shadow-sm flex items-center gap-1.5 transition-colors"
            >
              <Download className="h-3.5 w-3.5" />
              <span>Export vCard (.vcf)</span>
            </button>

            <button
              onClick={downloadCsvFile}
              className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold rounded-lg shadow-sm flex items-center gap-1.5 transition-colors"
            >
              <Download className="h-3.5 w-3.5" />
              <span>Download Excel/CSV</span>
            </button>
          </div>
        </div>
      </div>

      {/* Search & Preset Message */}
      <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm space-y-4">
        <div className="flex flex-col md:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Filter by name, phone number, area, or trade skill..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>
          {search && (
            <button
              onClick={() => setSearch("")}
              className="px-3 py-2 border border-slate-300 text-slate-700 text-sm font-medium rounded-lg hover:bg-slate-50"
            >
              Clear
            </button>
          )}
        </div>

        {/* Message Template Tester */}
        <div className="bg-slate-50 border border-slate-200 rounded-lg p-3">
          <div className="text-xs font-semibold text-slate-600 mb-1 flex items-center gap-1.5">
            <MessageSquare className="h-3.5 w-3.5 text-emerald-600" />
            <span>Preset Click-to-Chat WhatsApp Message:</span>
          </div>
          <div className="flex gap-2">
            <input
              type="text"
              value={presetMessage}
              onChange={(e) => setPresetMessage(e.target.value)}
              className="flex-1 px-3 py-1.5 border border-slate-300 rounded text-xs bg-white focus:outline-none focus:ring-1 focus:ring-emerald-500"
            />
          </div>
        </div>
      </div>

      {/* Contacts List Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-xs font-semibold text-slate-600 uppercase tracking-wider">
              <tr>
                <th className="py-3 px-4">Contact</th>
                <th className="py-3 px-4">Phone</th>
                <th className="py-3 px-4">Trade / Skills</th>
                <th className="py-3 px-4">Location</th>
                <th className="py-3 px-4 text-center">Direct WhatsApp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filtered.length === 0 && !loading && (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-500">
                    No contacts found in this segment.
                  </td>
                </tr>
              )}
              {filtered.slice(0, 100).map((c) => {
                const cleanPhone = c.phone.replace(/\D/g, "").slice(-10);
                const waUrl = cleanPhone
                  ? `https://wa.me/91${cleanPhone}?text=${encodeURIComponent(presetMessage)}`
                  : null;

                return (
                  <tr key={c.id} className="hover:bg-slate-50/70 transition-colors">
                    <td className="py-3 px-4">
                      <div className="font-semibold text-slate-900">{c.name}</div>
                      {c.businessName && <div className="text-xs text-slate-500">{c.businessName}</div>}
                    </td>
                    <td className="py-3 px-4">
                      <a href={`tel:${c.phone}`} className="font-mono text-xs text-emerald-700 hover:underline flex items-center gap-1">
                        <Phone className="h-3 w-3" />
                        <span>{c.phone}</span>
                      </a>
                    </td>
                    <td className="py-3 px-4">
                      {c.trade ? (
                        <span className="text-xs text-slate-700 bg-slate-100 px-2 py-0.5 rounded font-medium">
                          {c.trade}
                        </span>
                      ) : (
                        <span className="text-xs text-slate-400 italic">General</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-600">
                      {c.area || "Khammam"}
                    </td>
                    <td className="py-3 px-4 text-center">
                      {waUrl ? (
                        <a
                          href={waUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="inline-flex items-center gap-1 px-2.5 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-300 rounded text-xs font-semibold transition-colors"
                        >
                          <Send className="h-3 w-3 text-emerald-600" />
                          <span>Chat</span>
                        </a>
                      ) : (
                        <span className="text-xs text-slate-400">—</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
