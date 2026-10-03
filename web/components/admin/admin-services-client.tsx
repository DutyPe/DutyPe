"use client";

import { useEffect, useMemo, useState } from "react";
import { collection, doc, getDoc, limit, onSnapshot, orderBy, query, setDoc, Timestamp } from "firebase/firestore";
import { httpsCallable } from "firebase/functions";
import { getFirebaseServices } from "@/lib/firebase/client";

/**
 * DutyPe Services admin: approve partners, verify partner credit top-ups (UPI UTR), watch
 * bookings, and set fees / partner fee / coupons / UPI ID / prices (app_config/services).
 * Server logic: functions/src/services.ts (reviewServicePartner, verifyPartnerTopup).
 */

const millis = (v: unknown) => (v instanceof Timestamp ? v.toMillis() : typeof v === "number" ? v : 0);
const when = (ms: number) => (ms ? new Date(ms).toLocaleString("en-IN") : "");
const rupees = (paise: number) => `₹${(paise / 100).toLocaleString("en-IN")}`;

type Row = Record<string, unknown> & { id: string };
type Tab = "partners" | "topups" | "bookings" | "settings";

const CATEGORY_IDS = ["CLEANING", "AC", "ELECTRICIAN", "PLUMBER", "APPLIANCE", "CARPENTER", "PAINTER", "HOME_HELP", "VEHICLE"];

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
          <p>
            Call each applicant and check Aadhaar. <b>Basic</b> work (cleaning, home help, car wash) needs only ID and a polite call.
            <b>Skilled</b> work (⚡ marked) needs a skill check before you tick it: ask 3 practical questions on the phone
            (e.g. electrician: &quot;MCB keeps tripping – what do you check first?&quot;), ask for photos/videos of past work or an
            ITI / shop reference, and if unsure give a trial job at your own place. Untick categories you could not verify.
          </p>
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
                      <td>
                        {((p.categories || []) as string[]).map((c) => {
                          const skilled = ((p.skilledCategories || []) as string[]).includes(c);
                          const list = checked[p.id] ?? ((p.categories || []) as string[]).filter((x) => !((p.skilledCategories || []) as string[]).includes(x));
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
              <thead><tr><th>When</th><th>Service</th><th>Customer</th><th>Partner</th><th>Status</th><th>Total</th><th>Offer</th><th>DutyPe got</th><th>Rating</th></tr></thead>
              <tbody>
                {bookings.map((b) => (
                  <tr key={b.id}>
                    <td>{when(millis(b.createdAt))}{b.scheduledAt ? <div>Slot: {when(millis(b.scheduledAt))}</div> : null}</td>
                    <td>{String(b.serviceName)}<div style={{ fontSize: 12, opacity: 0.7 }}>{String(b.area || "")}</div></td>
                    <td>{String(b.customerName || "")}<div><a href={`tel:${String(b.customerPhone || "")}`}>{String(b.customerPhone || "")}</a></div></td>
                    <td>{String(b.partnerName || "–")}<div><a href={`tel:${String(b.partnerPhone || "")}`}>{String(b.partnerPhone || "")}</a></div></td>
                    <td>{String(b.status)}</td>
                    <td>₹{String(b.total ?? "")}</td>
                    <td>{Number(b.discount || 0) > 0 ? `−₹${String(b.discount)} ${String(b.couponCode || "first booking")}` : ""}</td>
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
  minTopup: number;
  coupons: Coupon[];
  services: Array<{ id: string; price?: number; active?: boolean }>;
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
  const [catalog, setCatalog] = useState<Array<{ id: string; category: string; name: string; price: number }>>([]);

  useEffect(() => {
    if (!services) return;
    void (async () => {
      const snap = await getDoc(doc(services.db, "app_config", "services"));
      const d = (snap.data() || {}) as Partial<Settings>;
      setRaw((snap.data() || {}) as Record<string, unknown>);
      setS({
        upiId: d.upiId || "",
        upiName: d.upiName || "DutyPe",
        bookingFee: d.bookingFee ?? 19,
        inspectionFee: d.inspectionFee ?? 49,
        commissionPct: d.commissionPct ?? 0,
        partnerFee: d.partnerFee ?? 19,
        partnerFirstJobFree: d.partnerFirstJobFree !== false,
        firstBookingFeeFree: d.firstBookingFeeFree !== false,
        minTopup: d.minTopup ?? 200,
        coupons: d.coupons || [],
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
        .map((x) => ({ id: x.id, ...(x.price !== undefined ? { price: x.price } : {}), ...(x.active === false ? { active: false } : {}) }))
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
  const check = (k: "partnerFirstJobFree" | "firstBookingFeeFree") => (
    <input type="checkbox" checked={s[k]} onChange={(e) => setS({ ...s, [k]: e.target.checked })} />
  );

  return (
    <section className="admin-section">
      <h2 className="admin-section-title">Prices &amp; settings</h2>
      <p>Partners pay their credit top-ups to this UPI ID. Fees apply to new bookings.</p>
      <div style={{ display: "grid", gridTemplateColumns: "260px 1fr", gap: 10, alignItems: "center", maxWidth: 640 }}>
        <label>UPI ID for top-ups</label>
        <input value={s.upiId} onChange={(e) => setS({ ...s, upiId: e.target.value.trim() })} placeholder="yourname@okaxis" />
        <label>UPI name</label>
        <input value={s.upiName} onChange={(e) => setS({ ...s, upiName: e.target.value })} />
        <label>Customer booking fee (₹)</label>{num("bookingFee")}
        <label>Inspection visit fee (₹)</label>{num("inspectionFee")}
        <label>Partner fee per job (₹, e.g. 9 or 19)</label>{num("partnerFee")}
        <label>Partner&apos;s first job free</label>{check("partnerFirstJobFree")}
        <label>Customer&apos;s first booking: no booking fee</label>{check("firstBookingFeeFree")}
        <label>Commission on price (%)</label>{num("commissionPct")}
        <label>Minimum top-up (₹)</label>{num("minTopup")}
      </div>

      <h3 style={{ marginTop: 24 }}>Coupons &amp; festive offers</h3>
      <p style={{ maxWidth: 760 }}>
        Offers never stack: a customer gets the first-booking offer or the best coupon. A discount is capped at
        DutyPe&apos;s own take on the booking (booking fee + partner fee{s.commissionPct ? " + commission" : ""}, now up to ₹{maxOff}),
        so an offer never costs DutyPe money and the partner still earns the full service price minus the ₹{s.partnerFee} fee.
        Each customer can use a coupon once. Hidden coupons are not shown in the app; share them on posters and WhatsApp.
      </p>
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

      <h3 style={{ marginTop: 24 }}>Service prices</h3>
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
