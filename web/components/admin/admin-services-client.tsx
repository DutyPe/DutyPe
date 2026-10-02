"use client";

import { useEffect, useMemo, useState } from "react";
import { collection, doc, getDoc, limit, onSnapshot, orderBy, query, setDoc, Timestamp } from "firebase/firestore";
import { httpsCallable } from "firebase/functions";
import { getFirebaseServices } from "@/lib/firebase/client";

/**
 * DutyPe Services admin: approve partners, verify partner credit top-ups (UPI UTR), watch
 * bookings, and set fees / commission / UPI ID / prices (app_config/services).
 * Server logic: functions/src/services.ts (reviewServicePartner, verifyPartnerTopup).
 */

const millis = (v: unknown) => (v instanceof Timestamp ? v.toMillis() : typeof v === "number" ? v : 0);
const when = (ms: number) => (ms ? new Date(ms).toLocaleString("en-IN") : "");
const rupees = (paise: number) => `₹${(paise / 100).toLocaleString("en-IN")}`;

type Row = Record<string, unknown> & { id: string };
type Tab = "partners" | "topups" | "bookings" | "settings";

const CATEGORY_IDS = ["AC", "CLEANING", "ELECTRICIAN", "PLUMBER", "APPLIANCE"];

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

  const partners = useLive("service_partners", "updatedAt");
  const topups = useLive("partner_topups", "createdAt");
  const bookings = useLive("service_bookings", "createdAt", 300);

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
    if (!confirm(`${action.toUpperCase()} ${String(p.name)} (${String(p.phone)})?`)) return;
    void run("Saving...", () => httpsCallable(services.functions, "reviewServicePartner")({ partnerId: p.id, action, reason }));
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
      </div>

      {tab === "partners" && (
        <section className="admin-section">
          <h2 className="admin-section-title">Service partners</h2>
          <p>Call each applicant, check Aadhaar and skills, then approve. Only approved partners get jobs.</p>
          <div className="admin-table-container">
            <table className="admin-table">
              <thead>
                <tr><th>Partner</th><th>Services</th><th>Status</th><th>Credits</th><th>Jobs / rating</th><th>Action</th></tr>
              </thead>
              <tbody>
                {[...partners].sort((a, b) => (a.status === "PENDING" ? -1 : 0) - (b.status === "PENDING" ? -1 : 0)).map((p) => {
                  const count = Number(p.ratingCount || 0);
                  return (
                    <tr key={p.id}>
                      <td>
                        <strong>{String(p.name || "")}</strong>
                        <div><a href={`tel:${String(p.phone || "")}`}>{String(p.phone || "")}</a></div>
                        <div>{String(p.area || "")} · {String(p.experienceYears ?? 0)} yrs</div>
                        <div style={{ fontSize: 12, opacity: 0.7 }}>Applied {when(millis(p.appliedAt))}</div>
                      </td>
                      <td>{((p.categories || []) as string[]).join(", ")}</td>
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
                        <div style={{ display: "flex", gap: 6, marginTop: 6 }}>
                          {p.status !== "APPROVED" && <button className="btn btn-approve" onClick={() => review(p, "approve")}>Approve</button>}
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
          <h2 className="admin-section-title">Bookings</h2>
          <div className="admin-table-container">
            <table className="admin-table">
              <thead><tr><th>When</th><th>Service</th><th>Customer</th><th>Partner</th><th>Status</th><th>Total</th><th>DutyPe got</th><th>Rating</th></tr></thead>
              <tbody>
                {bookings.map((b) => (
                  <tr key={b.id}>
                    <td>{when(millis(b.createdAt))}{b.scheduledAt ? <div>Slot: {when(millis(b.scheduledAt))}</div> : null}</td>
                    <td>{String(b.serviceName)}<div style={{ fontSize: 12, opacity: 0.7 }}>{String(b.area || "")}</div></td>
                    <td>{String(b.customerName || "")}<div><a href={`tel:${String(b.customerPhone || "")}`}>{String(b.customerPhone || "")}</a></div></td>
                    <td>{String(b.partnerName || "–")}<div><a href={`tel:${String(b.partnerPhone || "")}`}>{String(b.partnerPhone || "")}</a></div></td>
                    <td>{String(b.status)}</td>
                    <td>₹{String(b.total ?? "")}</td>
                    <td>{b.platformTakePaise ? rupees(Number(b.platformTakePaise)) : "–"}</td>
                    <td>{b.rating ? `${String(b.rating)}★` : ""}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {tab === "settings" && <ServicesSettings onSaved={(m) => setMessage(m)} onError={(m) => setError(m)} />}
    </div>
  );
}

type Settings = {
  upiId: string;
  upiName: string;
  bookingFee: number;
  inspectionFee: number;
  commissionPct: number;
  minTopup: number;
  services: Array<{ id: string; price?: number; active?: boolean }>;
};

function ServicesSettings({ onSaved, onError }: { onSaved: (m: string) => void; onError: (m: string) => void }) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [s, setS] = useState<Settings | null>(null);
  const [catalog, setCatalog] = useState<Array<{ id: string; category: string; name: string; price: number }>>([]);

  useEffect(() => {
    if (!services) return;
    void (async () => {
      const snap = await getDoc(doc(services.db, "app_config", "services"));
      const d = (snap.data() || {}) as Partial<Settings>;
      setS({
        upiId: d.upiId || "",
        upiName: d.upiName || "DutyPe",
        bookingFee: d.bookingFee ?? 19,
        inspectionFee: d.inspectionFee ?? 49,
        commissionPct: d.commissionPct ?? 10,
        minTopup: d.minTopup ?? 200,
        services: d.services || [],
      });
      try {
        const res = await httpsCallable(services.functions, "getServiceCatalog")({ all: true });
        const list = ((res.data as { services?: unknown[] }).services || []) as Array<{ id: string; category: string; name: string; price: number }>;
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
  const setActive = (id: string, active: boolean) => {
    const rest = s.services.filter((x) => x.id !== id);
    setS({ ...s, services: [...rest, { ...(override(id) || { id }), active }] });
  };

  async function save() {
    if (!services || !s) return;
    try {
      const clean = s.services
        .map((x) => ({ id: x.id, ...(x.price !== undefined ? { price: x.price } : {}), ...(x.active === false ? { active: false } : {}) }))
        .filter((x) => Object.keys(x).length > 1);
      await setDoc(doc(services.db, "app_config", "services"), { ...s, services: clean }, { merge: false });
      onSaved("Saved. The app picks up new prices within ~10 minutes.");
    } catch (e: unknown) {
      onError(e instanceof Error ? e.message : "Save failed");
    }
  }

  const num = (k: keyof Settings) => (
    <input type="number" value={String(s[k])} onChange={(e) => setS({ ...s, [k]: Number(e.target.value) })} style={{ width: 100 }} />
  );

  return (
    <section className="admin-section">
      <h2 className="admin-section-title">Prices &amp; settings</h2>
      <p>Partners pay their credit top-ups to this UPI ID. Fees and commission apply to new bookings.</p>
      <div style={{ display: "grid", gridTemplateColumns: "220px 1fr", gap: 10, alignItems: "center", maxWidth: 560 }}>
        <label>UPI ID for top-ups</label>
        <input value={s.upiId} onChange={(e) => setS({ ...s, upiId: e.target.value.trim() })} placeholder="yourname@okaxis" />
        <label>UPI name</label>
        <input value={s.upiName} onChange={(e) => setS({ ...s, upiName: e.target.value })} />
        <label>Booking fee (₹)</label>{num("bookingFee")}
        <label>Inspection visit fee (₹)</label>{num("inspectionFee")}
        <label>Commission (%)</label>{num("commissionPct")}
        <label>Minimum top-up (₹)</label>{num("minTopup")}
      </div>
      <h3 style={{ marginTop: 20 }}>Service prices</h3>
      <div className="admin-table-container">
        <table className="admin-table">
          <thead><tr><th>Category</th><th>Service</th><th>Price (₹)</th><th>Offered</th></tr></thead>
          <tbody>
            {CATEGORY_IDS.flatMap((cat) => catalog.filter((c) => c.category === cat)).map((c) => {
              const o = override(c.id);
              return (
                <tr key={c.id}>
                  <td>{c.category}</td>
                  <td>{c.name}</td>
                  <td>
                    <input
                      type="number"
                      value={o?.price !== undefined ? String(o.price) : ""}
                      placeholder={String(c.price)}
                      onChange={(e) => setPrice(c.id, e.target.value)}
                      style={{ width: 100 }}
                    />
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
