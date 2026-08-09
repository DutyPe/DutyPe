"use client";

import { useEffect, useState, useMemo } from "react";
import { ContactItem } from "@/app/api/admin/whatsapp-contacts/route";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";

type ResponseData = {
  workers: ContactItem[];
  employers: ContactItem[];
  counts: {
    totalWorkers: number;
    totalEmployers: number;
    totalContacts: number;
  };
  error?: string;
};

export function WhatsAppContactsClient() {
  const [data, setData] = useState<ResponseData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [activeTab, setActiveTab] = useState<"WORKER" | "EMPLOYER">("WORKER");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedLocation, setSelectedLocation] = useState("ALL");
  const [selectedIds, setSelectedIds] = useState<Set<String>>(new Set());

  const [copyFeedback, setCopyFeedback] = useState<string | null>(null);
  const [broadcastMessage, setBroadcastMessage] = useState(
    "🚨 *DutyPe Emergency Hiring & Job Alert*\n\nHello! Join the official DutyPe WhatsApp Channel for instant daily job postings and verified worker requirements.\n\n👉 Join Channel: https://whatsapp.com/channel/0029VbBdNOQ1iUxZMmvg8t2G"
  );

  useEffect(() => {
    async function loadContacts() {
      try {
        setLoading(true);
        const res = await adminApiFetch("/api/admin/whatsapp-contacts");
        if (!res.ok) {
          throw new Error(`Server returned ${res.status}`);
        }
        const json: ResponseData = await res.json();
        if (json.error) {
          throw new Error(json.error);
        }
        setData(json);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load contacts.");
      } finally {
        setLoading(false);
      }
    }

    loadContacts();
  }, []);

  const currentList = useMemo(() => {
    if (!data) return [];
    return activeTab === "WORKER" ? data.workers : data.employers;
  }, [data, activeTab]);

  const locationOptions = useMemo(() => {
    const locs = new Set<string>();
    currentList.forEach((c) => {
      if (c.location) locs.add(c.location);
    });
    return ["ALL", ...Array.from(locs).sort()];
  }, [currentList]);

  const filteredContacts = useMemo(() => {
    return currentList.filter((item) => {
      const q = searchQuery.toLowerCase().trim();
      const matchesSearch =
        !q ||
        item.name.toLowerCase().includes(q) ||
        item.phone.toLowerCase().includes(q) ||
        (item.companyName && item.companyName.toLowerCase().includes(q)) ||
        (item.category && item.category.toLowerCase().includes(q)) ||
        (item.skills && item.skills.some((s) => s.toLowerCase().includes(q))) ||
        (item.location && item.location.toLowerCase().includes(q));

      const matchesLocation =
        selectedLocation === "ALL" || item.location === selectedLocation;

      return matchesSearch && matchesLocation;
    });
  }, [currentList, searchQuery, selectedLocation]);

  function triggerFeedback(msg: string) {
    setCopyFeedback(msg);
    setTimeout(() => setCopyFeedback(null), 3000);
  }

  function handleSelectAll() {
    if (selectedIds.size === filteredContacts.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(filteredContacts.map((c) => c.id)));
    }
  }

  function toggleSelect(id: string) {
    const next = new Set(selectedIds);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    setSelectedIds(next);
  }

  const activeSelectedContacts = useMemo(() => {
    if (selectedIds.size === 0) return filteredContacts;
    return filteredContacts.filter((c) => selectedIds.has(c.id));
  }, [filteredContacts, selectedIds]);

  function copyNumbers(format: "COMMA" | "LINE" | "RAW_10") {
    if (activeSelectedContacts.length === 0) {
      alert("No contacts available to copy.");
      return;
    }

    let result = "";
    if (format === "COMMA") {
      result = activeSelectedContacts.map((c) => c.phone).join(", ");
    } else if (format === "LINE") {
      result = activeSelectedContacts.map((c) => c.phone).join("\n");
    } else if (format === "RAW_10") {
      result = activeSelectedContacts
        .map((c) => (c.rawPhone.length >= 10 ? c.rawPhone.slice(-10) : c.rawPhone))
        .join(", ");
    }

    navigator.clipboard.writeText(result);
    triggerFeedback(`Copied ${activeSelectedContacts.length} ${activeTab.toLowerCase()} phone numbers!`);
  }

  function copyFormattedInvite() {
    const text = `${broadcastMessage}\n\n📱 *DutyPe ${activeTab === "WORKER" ? "Worker" : "Employer"} Network* (${activeSelectedContacts.length} Contacts)`;
    navigator.clipboard.writeText(text);
    triggerFeedback("Broadcast message copied to clipboard!");
  }

  function exportCSV() {
    if (activeSelectedContacts.length === 0) return;

    const headers = ["Name", "Phone", "Role", "Company / Category", "Location", "Joined Date"];
    const rows = activeSelectedContacts.map((c) => [
      `"${c.name.replace(/"/g, '""')}"`,
      `"${c.phone}"`,
      `"${c.role}"`,
      `"${(c.companyName || c.category || "").replace(/"/g, '""')}"`,
      `"${(c.location || "").replace(/"/g, '""')}"`,
      `"${c.joinedAt || ""}"`
    ]);

    const csvContent = "data:text/csv;charset=utf-8," + [headers.join(","), ...rows.map((e) => e.join(","))].join("\n");
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `dutype_${activeTab.toLowerCase()}_contacts_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    triggerFeedback(`Downloaded CSV with ${activeSelectedContacts.length} contacts!`);
  }

  if (loading) {
    return (
      <div style={{ padding: "40px", textAlign: "center", color: "#94a3b8" }}>
        <div style={{ fontSize: "24px", marginBottom: "12px" }}>⏳ Loading WhatsApp Broadcast Contacts...</div>
        <p>Fetching verified Worker & Employer phone records from Firestore...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: "30px", background: "#451a1a", border: "1px solid #f87171", borderRadius: "12px", color: "#fca5a5" }}>
        <h3>⚠️ Error Loading Contacts</h3>
        <p>{error}</p>
      </div>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
      {/* Top Banner & Tab Controls */}
      <div
        style={{
          background: "linear-gradient(135deg, #0f172a 0%, #1e293b 100%)",
          padding: "24px",
          borderRadius: "16px",
          border: "1px solid #334155",
          boxShadow: "0 10px 25px -5px rgba(0, 0, 0, 0.3)"
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "16px" }}>
          <div>
            <h2 style={{ margin: 0, fontSize: "24px", color: "#f8fafc", fontWeight: "700" }}>
              📱 WhatsApp Community & Broadcast Contacts Hub
            </h2>
            <p style={{ margin: "6px 0 0 0", color: "#94a3b8", fontSize: "14px" }}>
              One-click phone export & group broadcast tools for emergency hiring, job alerts, and updates.
            </p>
          </div>

          {/* Role Toggle Tabs */}
          <div style={{ display: "flex", background: "#0f172a", padding: "6px", borderRadius: "12px", border: "1px solid #334155" }}>
            <button
              onClick={() => {
                setActiveTab("WORKER");
                setSelectedIds(new Set());
              }}
              style={{
                padding: "10px 20px",
                borderRadius: "8px",
                border: "none",
                background: activeTab === "WORKER" ? "#2563eb" : "transparent",
                color: activeTab === "WORKER" ? "#ffffff" : "#94a3b8",
                fontWeight: "600",
                fontSize: "14px",
                cursor: "pointer",
                transition: "all 0.2s ease"
              }}
            >
              👷 Worker Contacts ({data?.counts.totalWorkers ?? 0})
            </button>
            <button
              onClick={() => {
                setActiveTab("EMPLOYER");
                setSelectedIds(new Set());
              }}
              style={{
                padding: "10px 20px",
                borderRadius: "8px",
                border: "none",
                background: activeTab === "EMPLOYER" ? "#16a34a" : "transparent",
                color: activeTab === "EMPLOYER" ? "#ffffff" : "#94a3b8",
                fontWeight: "600",
                fontSize: "14px",
                cursor: "pointer",
                transition: "all 0.2s ease"
              }}
            >
              🏢 Employer Contacts ({data?.counts.totalEmployers ?? 0})
            </button>
          </div>
        </div>

        {/* Copy Success Feedback Notification */}
        {copyFeedback && (
          <div
            style={{
              marginTop: "16px",
              padding: "12px 18px",
              background: "#065f46",
              color: "#34d399",
              borderRadius: "8px",
              fontWeight: "600",
              fontSize: "14px",
              display: "flex",
              alignItems: "center",
              gap: "8px"
            }}
          >
            ✅ {copyFeedback}
          </div>
        )}
      </div>

      {/* Action Toolbar */}
      <div
        style={{
          background: "#1e293b",
          padding: "20px",
          borderRadius: "14px",
          border: "1px solid #334155",
          display: "flex",
          flexDirection: "column",
          gap: "16px"
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px" }}>
          <span style={{ fontSize: "15px", fontWeight: "600", color: "#e2e8f0" }}>
            ⚡ Quick Export Tools {selectedIds.size > 0 ? `(${selectedIds.size} Selected)` : `(All ${filteredContacts.length})`}
          </span>

          <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
            <button
              onClick={() => copyNumbers("COMMA")}
              style={{
                padding: "8px 14px",
                background: "#2563eb",
                color: "#fff",
                border: "none",
                borderRadius: "8px",
                fontWeight: "600",
                fontSize: "13px",
                cursor: "pointer"
              }}
            >
              📋 Copy Comma-Separated (+91...)
            </button>

            <button
              onClick={() => copyNumbers("LINE")}
              style={{
                padding: "8px 14px",
                background: "#0891b2",
                color: "#fff",
                border: "none",
                borderRadius: "8px",
                fontWeight: "600",
                fontSize: "13px",
                cursor: "pointer"
              }}
            >
              📄 Copy Line-Separated
            </button>

            <button
              onClick={() => copyNumbers("RAW_10")}
              style={{
                padding: "8px 14px",
                background: "#4f46e5",
                color: "#fff",
                border: "none",
                borderRadius: "8px",
                fontWeight: "600",
                fontSize: "13px",
                cursor: "pointer"
              }}
            >
              🔢 Copy 10-Digit Numbers
            </button>

            <button
              onClick={exportCSV}
              style={{
                padding: "8px 14px",
                background: "#16a34a",
                color: "#fff",
                border: "none",
                borderRadius: "8px",
                fontWeight: "600",
                fontSize: "13px",
                cursor: "pointer"
              }}
            >
              📊 Export CSV File
            </button>
          </div>
        </div>

        {/* Broadcast Message Customizer */}
        <div style={{ background: "#0f172a", padding: "14px", borderRadius: "10px", border: "1px solid #334155" }}>
          <label style={{ display: "block", fontSize: "13px", fontWeight: "600", color: "#94a3b8", marginBottom: "8px" }}>
            💬 Pre-Formatted WhatsApp Broadcast Message:
          </label>
          <textarea
            value={broadcastMessage}
            onChange={(e) => setBroadcastMessage(e.target.value)}
            rows={2}
            style={{
              width: "100%",
              background: "#1e293b",
              color: "#f8fafc",
              border: "1px solid #475569",
              borderRadius: "8px",
              padding: "10px",
              fontSize: "13px",
              fontFamily: "inherit",
              resize: "vertical"
            }}
          />
          <div style={{ marginTop: "8px", display: "flex", justifyContent: "flex-end" }}>
            <button
              onClick={copyFormattedInvite}
              style={{
                padding: "6px 12px",
                background: "#d97706",
                color: "#fff",
                border: "none",
                borderRadius: "6px",
                fontWeight: "600",
                fontSize: "12px",
                cursor: "pointer"
              }}
            >
              📋 Copy Broadcast Post Text
            </button>
          </div>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div
        style={{
          display: "flex",
          gap: "12px",
          alignItems: "center",
          flexWrap: "wrap",
          background: "#1e293b",
          padding: "16px",
          borderRadius: "12px",
          border: "1px solid #334155"
        }}
      >
        <div style={{ flex: "1 1 300px" }}>
          <input
            type="text"
            placeholder={`Search ${activeTab === "WORKER" ? "workers" : "employers"} by name, phone, company, or skill...`}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: "100%",
              padding: "10px 14px",
              background: "#0f172a",
              border: "1px solid #475569",
              borderRadius: "8px",
              color: "#f8fafc",
              fontSize: "14px"
            }}
          />
        </div>

        <div style={{ flex: "0 0 200px" }}>
          <select
            value={selectedLocation}
            onChange={(e) => setSelectedLocation(e.target.value)}
            style={{
              width: "100%",
              padding: "10px 14px",
              background: "#0f172a",
              border: "1px solid #475569",
              borderRadius: "8px",
              color: "#f8fafc",
              fontSize: "14px"
            }}
          >
            {locationOptions.map((loc) => (
              <option key={loc} value={loc}>
                {loc === "ALL" ? "📍 All Locations" : `📍 ${loc}`}
              </option>
            ))}
          </select>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          <button
            onClick={handleSelectAll}
            style={{
              padding: "10px 14px",
              background: "#334155",
              color: "#e2e8f0",
              border: "none",
              borderRadius: "8px",
              fontWeight: "600",
              fontSize: "13px",
              cursor: "pointer"
            }}
          >
            {selectedIds.size === filteredContacts.length && filteredContacts.length > 0
              ? "Deselect All"
              : `Select All (${filteredContacts.length})`}
          </button>
        </div>
      </div>

      {/* Contacts List Grid */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(340px, 1fr))", gap: "16px" }}>
        {filteredContacts.length === 0 ? (
          <div
            style={{
              gridColumn: "1 / -1",
              padding: "40px",
              textAlign: "center",
              background: "#1e293b",
              borderRadius: "14px",
              border: "1px dashed #475569",
              color: "#94a3b8"
            }}
          >
            🔍 No {activeTab.toLowerCase()} contacts match your search query or location filter.
          </div>
        ) : (
          filteredContacts.map((contact) => {
            const isSelected = selectedIds.has(contact.id);
            const waNumber = contact.rawPhone.length >= 10 ? (contact.rawPhone.startsWith("91") ? contact.rawPhone : `91${contact.rawPhone.slice(-10)}`) : contact.rawPhone;
            const waUrl = `https://wa.me/${waNumber}?text=Hi%20${encodeURIComponent(contact.name)},%20DutyPe%20hiring%20update:`;

            return (
              <div
                key={contact.id}
                onClick={() => toggleSelect(contact.id)}
                style={{
                  background: isSelected ? "#1e3a8a" : "#1e293b",
                  border: isSelected ? "2px solid #3b82f6" : "1px solid #334155",
                  borderRadius: "14px",
                  padding: "18px",
                  cursor: "pointer",
                  transition: "all 0.15s ease",
                  display: "flex",
                  flexDirection: "column",
                  justifyContent: "space-between",
                  position: "relative"
                }}
              >
                {/* Header info */}
                <div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "10px" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                      <div
                        style={{
                          width: "42px",
                          height: "42px",
                          borderRadius: "50%",
                          background: contact.role === "WORKER" ? "#2563eb" : "#16a34a",
                          color: "#fff",
                          fontWeight: "bold",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          fontSize: "18px"
                        }}
                      >
                        {contact.name.charAt(0).toUpperCase()}
                      </div>

                      <div>
                        <h4 style={{ margin: 0, fontSize: "16px", color: "#f8fafc", fontWeight: "700" }}>
                          {contact.name}
                        </h4>
                        {contact.companyName && contact.companyName !== contact.name && (
                          <div style={{ fontSize: "13px", color: "#cbd5e1", marginTop: "2px" }}>
                            🏢 {contact.companyName}
                          </div>
                        )}
                      </div>
                    </div>

                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={() => {}}
                      style={{ width: "18px", height: "18px", cursor: "pointer" }}
                    />
                  </div>

                  {/* Phone & Meta */}
                  <div style={{ marginTop: "14px", display: "flex", flexDirection: "column", gap: "6px" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "15px", fontWeight: "600", color: "#38bdf8" }}>
                      📞 {contact.phone}
                    </div>

                    {contact.category && (
                      <div style={{ fontSize: "13px", color: "#94a3b8" }}>
                        🛠️ {contact.category}
                      </div>
                    )}

                    {contact.skills && contact.skills.length > 0 && (
                      <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginTop: "4px" }}>
                        {contact.skills.slice(0, 3).map((s, idx) => (
                          <span
                            key={idx}
                            style={{
                              padding: "2px 8px",
                              background: "#334155",
                              color: "#cbd5e1",
                              borderRadius: "6px",
                              fontSize: "11px",
                              fontWeight: "500"
                            }}
                          >
                            {s}
                          </span>
                        ))}
                      </div>
                    )}

                    <div style={{ fontSize: "12px", color: "#64748b", marginTop: "6px" }}>
                      📍 {contact.location}
                    </div>
                  </div>
                </div>

                {/* Quick Buttons */}
                <div style={{ marginTop: "16px", paddingTop: "12px", borderTop: "1px solid #334155", display: "flex", gap: "8px" }} onClick={(e) => e.stopPropagation()}>
                  <button
                    onClick={() => {
                      navigator.clipboard.writeText(contact.phone);
                      triggerFeedback(`Copied ${contact.name}'s phone number (${contact.phone})`);
                    }}
                    style={{
                      flex: 1,
                      padding: "8px",
                      background: "#334155",
                      color: "#f8fafc",
                      border: "none",
                      borderRadius: "8px",
                      fontSize: "12px",
                      fontWeight: "600",
                      cursor: "pointer"
                    }}
                  >
                    📋 Copy Phone
                  </button>

                  <a
                    href={waUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{
                      flex: 1,
                      padding: "8px",
                      background: "#25d366",
                      color: "#fff",
                      borderRadius: "8px",
                      fontSize: "12px",
                      fontWeight: "600",
                      textAlign: "center",
                      textDecoration: "none"
                    }}
                  >
                    💬 WhatsApp Chat
                  </a>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
