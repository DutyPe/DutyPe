"use client";

import { useState } from "react";
import {
  Utensils,
  MapPin,
  Clock,
  Sparkles,
  ArrowRight,
  CheckCircle2,
  Share2,
  ShieldCheck,
  ChevronRight,
  TrendingDown,
  Gift,
  HelpCircle,
  Phone
} from "lucide-react";

const KHAMMAM_AREAS = [
  "Mamillagudem",
  "Wyra Road",
  "Moti Nagar",
  "Balaji Nagar",
  "NSP Colony",
  "Rotary Nagar",
  "Trunk Road",
  "Gandhi Chowk",
  "Prakash Nagar",
  "Ricco Bazar",
  "Balanagar",
  "Other Area in Khammam"
];

const FOOD_ITEMS = [
  { id: "TIFFIN_IDLI_DOSA", label: "Idli, Dosa, Vada & Poori", icon: "🥞", desc: "Fresh hot morning tiffins" },
  { id: "FRESH_JUICE", label: "Fresh Juices & Milkshakes", icon: "🥤", desc: "Mosambi, Orange, Badam milk" },
  { id: "FILTER_COFFEE", label: "Filter Coffee & Masala Chai", icon: "☕", desc: "Traditional South Indian aroma" },
  { id: "HEALTHY_MEALS", label: "Healthy Sprouts & Millet Bowls", icon: "🥗", desc: "Nutrient-rich fitness breakfast" },
  { id: "EVENING_SNACKS", label: "Evening Snacks & Bajji", icon: "🥟", desc: "Mirchi bajji, samosa & pakoda" }
];

export function HungrWaitlistClient() {
  const [step, setStep] = useState(1);
  const [submitting, setSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Form State
  const [eatingFrequency, setEatingFrequency] = useState("DAILY");
  const [willingToPreOrder, setWillingToPreOrder] = useState("YES_DEFINITELY");
  const [selectedFoods, setSelectedFoods] = useState<string[]>([
    "TIFFIN_IDLI_DOSA",
    "FRESH_JUICE"
  ]);
  const [isKhammam, setIsKhammam] = useState(true);
  const [selectedArea, setSelectedArea] = useState("Mamillagudem");
  const [customArea, setCustomArea] = useState("");
  const [otherCity, setOtherCity] = useState("");
  const [otherArea, setOtherArea] = useState("");
  const [apartmentName, setApartmentName] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");

  function toggleFood(id: string) {
    setSelectedFoods((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErrorMessage(null);

    const cleanPhone = phone.replace(/\D/g, "");
    if (!cleanPhone || cleanPhone.length < 10) {
      setErrorMessage("Please enter a valid 10-digit WhatsApp number.");
      return;
    }

    if (!name.trim()) {
      setErrorMessage("Please enter your name.");
      return;
    }

    const finalCity = isKhammam ? "Khammam" : otherCity.trim() || "Outside Khammam";
    const finalArea = isKhammam
      ? selectedArea === "Other Area in Khammam"
        ? customArea.trim() || "Khammam Town"
        : selectedArea
      : otherArea.trim() || "Town Area";

    setSubmitting(true);
    try {
      const response = await fetch("/api/hungr/survey", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          name: name.trim(),
          phone: cleanPhone,
          city: finalCity,
          area: finalArea,
          apartmentName: apartmentName.trim() || null,
          eatingFrequency,
          willingToPreOrder: willingToPreOrder === "YES_DEFINITELY" || willingToPreOrder === "SOMETIMES",
          preferredCategories: selectedFoods,
          source: "WEB_WAITLIST"
        })
      });

      const resData = (await response.json()) as { error?: string };
      if (!response.ok) {
        throw new Error(resData.error || "Failed to submit survey.");
      }

      setIsSuccess(true);
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : "Something went wrong. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  function handleShareWhatsApp() {
    const text = encodeURIComponent(
      `🥞 Hey! Have you heard of Hungr? They are delivering morning tiffins (Idli/Dosa) and juices at exact offline shop prices (₹35) right to apartment gates with ₹0 surge markup!\n\nJoin the waitlist with me so our apartment gets batch delivery on Day 1: https://dutype.in/hungr`
    );
    window.open(`https://api.whatsapp.com/send?text=${text}`, "_blank");
  }

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "linear-gradient(180deg, #FFF7ED 0%, #FFFFFF 30%, #F8FAFC 100%)",
        color: "#0F172A",
        fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
      }}
    >
      {/* Top Header */}
      <header
        style={{
          borderBottom: "1px solid #FED7AA",
          background: "rgba(255, 255, 255, 0.85)",
          backdropFilter: "blur(10px)",
          position: "sticky",
          top: 0,
          zIndex: 40
        }}
      >
        <div
          style={{
            maxWidth: 680,
            margin: "0 auto",
            padding: "14px 20px",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center"
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span
              style={{
                fontSize: 22,
                background: "#FF6B00",
                color: "#FFFFFF",
                borderRadius: "50%",
                width: 34,
                height: 34,
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                boxShadow: "0 2px 8px rgba(255, 107, 0, 0.3)"
              }}
            >
              🥞
            </span>
            <div>
              <span style={{ fontSize: 18, fontWeight: 900, color: "#EA580C", letterSpacing: -0.5 }}>hungr</span>
              <span style={{ fontSize: 11, color: "#9A3412", marginLeft: 4, fontWeight: 600 }}>by DutyPe</span>
            </div>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <span
              style={{
                fontSize: 11,
                background: "#FFEDD5",
                color: "#C2410C",
                fontWeight: 700,
                padding: "3px 10px",
                borderRadius: 20
              }}
            >
              VIP WAITLIST
            </span>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main style={{ maxWidth: 580, margin: "0 auto", padding: "24px 18px 60px" }}>
        {!isSuccess ? (
          <div>
            {/* Hero Value Prop */}
            <div style={{ textAlign: "center", marginBottom: 28 }}>
              <div
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: 6,
                  background: "#FEF3C7",
                  border: "1px solid #FDE68A",
                  borderRadius: 20,
                  padding: "4px 12px",
                  fontSize: 12,
                  fontWeight: 700,
                  color: "#92400E",
                  marginBottom: 12
                }}
              >
                <Sparkles style={{ width: 14, height: 14, color: "#D97706" }} />
                Pre-Order 5 PM–11 PM · Delivery at 7:30 AM
              </div>

              <h1
                style={{
                  fontSize: "clamp(24px, 5vw, 32px)",
                  fontWeight: 900,
                  lineHeight: 1.2,
                  color: "#1E293B",
                  marginBottom: 10
                }}
              >
                Morning Tiffins & Juices at <span style={{ color: "#EA580C" }}>Exact Shop Prices</span>
              </h1>

              <p style={{ fontSize: 14, color: "#64748B", margin: "0 auto", maxWidth: 460, lineHeight: 1.5 }}>
                No ₹45 delivery fee. No 30% restaurant markup. Batch delivered right to your apartment gate or ground floor security.
              </p>
            </div>

            {/* Price Comparison Card */}
            <div
              style={{
                background: "#FFFFFF",
                border: "1px solid #E2E8F0",
                borderRadius: 16,
                padding: "16px 18px",
                marginBottom: 24,
                boxShadow: "0 4px 16px rgba(0,0,0,0.03)"
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
                <span style={{ fontSize: 12, fontWeight: 700, color: "#64748B", textTransform: "uppercase", letterSpacing: 0.5 }}>
                  Cost Comparison: 1 Plate Hot Idli
                </span>
                <span
                  style={{
                    fontSize: 11,
                    background: "#DCFCE7",
                    color: "#166534",
                    fontWeight: 700,
                    padding: "2px 8px",
                    borderRadius: 12
                  }}
                >
                  SAVE ₹60+ EVERY DAY
                </span>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
                {/* Hungr Price */}
                <div
                  style={{
                    background: "#FFF7ED",
                    border: "1.5px solid #FDBA74",
                    borderRadius: 12,
                    padding: "12px 14px"
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: 4, marginBottom: 4 }}>
                    <span style={{ fontSize: 14 }}>🥞</span>
                    <span style={{ fontSize: 12, fontWeight: 800, color: "#C2410C" }}>Hungr Pre-Order</span>
                  </div>
                  <div style={{ fontSize: 26, fontWeight: 900, color: "#EA580C" }}>
                    ₹35
                  </div>
                  <div style={{ fontSize: 11, color: "#9A3412", marginTop: 2, fontWeight: 500 }}>
                    Exact offline stall rate (₹0 markup)
                  </div>
                </div>

                {/* Swiggy/Zomato Price */}
                <div
                  style={{
                    background: "#F8FAFC",
                    border: "1px solid #E2E8F0",
                    borderRadius: 12,
                    padding: "12px 14px",
                    opacity: 0.85
                  }}
                >
                  <div style={{ fontSize: 12, fontWeight: 700, color: "#64748B", marginBottom: 4 }}>
                    Other Apps
                  </div>
                  <div style={{ fontSize: 26, fontWeight: 900, color: "#64748B", textDecoration: "line-through" }}>
                    ₹95+
                  </div>
                  <div style={{ fontSize: 11, color: "#94A3B8", marginTop: 2 }}>
                    Item markup + ₹45 surge delivery
                  </div>
                </div>
              </div>
            </div>

            {/* Stepper Progress Bar */}
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 20 }}>
              {[1, 2, 3, 4].map((i) => (
                <div
                  key={i}
                  style={{
                    flex: 1,
                    height: 5,
                    borderRadius: 3,
                    marginRight: i === 4 ? 0 : 6,
                    background: step >= i ? "#EA580C" : "#E2E8F0",
                    transition: "all 0.3s ease"
                  }}
                />
              ))}
            </div>

            {errorMessage && (
              <div
                style={{
                  background: "#FEF2F2",
                  border: "1px solid #FECACA",
                  color: "#991B1B",
                  padding: "10px 14px",
                  borderRadius: 10,
                  fontSize: 13,
                  marginBottom: 16
                }}
              >
                {errorMessage}
              </div>
            )}

            {/* Step 1: Frequency & Willingness */}
            {step === 1 && (
              <div
                style={{
                  background: "#FFFFFF",
                  border: "1px solid #E2E8F0",
                  borderRadius: 18,
                  padding: "20px 18px",
                  boxShadow: "0 4px 20px rgba(0,0,0,0.04)"
                }}
              >
                <div style={{ fontSize: 12, fontWeight: 700, color: "#EA580C", marginBottom: 4 }}>
                  STEP 1 OF 4 · YOUR HABITS
                </div>
                <h2 style={{ fontSize: 18, fontWeight: 800, color: "#1E293B", marginBottom: 14 }}>
                  How often do you eat or order morning breakfast outside?
                </h2>

                <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 20 }}>
                  {[
                    { id: "DAILY", label: "🔥 Almost Every Morning (Daily Power Eater)" },
                    { id: "3_TO_4_DAYS", label: "⚡ 3–4 Days a week" },
                    { id: "WEEKENDS", label: "✨ Weekends only" },
                    { id: "RARELY", label: "🌱 Rarely / Occasional cravings" }
                  ].map((opt) => (
                    <button
                      key={opt.id}
                      type="button"
                      onClick={() => setEatingFrequency(opt.id)}
                      style={{
                        padding: "12px 14px",
                        borderRadius: 12,
                        border: eatingFrequency === opt.id ? "2px solid #EA580C" : "1px solid #E2E8F0",
                        background: eatingFrequency === opt.id ? "#FFF7ED" : "#FFFFFF",
                        color: eatingFrequency === opt.id ? "#C2410C" : "#1E293B",
                        fontWeight: eatingFrequency === opt.id ? 700 : 500,
                        fontSize: 14,
                        textAlign: "left",
                        cursor: "pointer",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center"
                      }}
                    >
                      {opt.label}
                      {eatingFrequency === opt.id && <CheckCircle2 style={{ width: 18, height: 18, color: "#EA580C" }} />}
                    </button>
                  ))}
                </div>

                <h3 style={{ fontSize: 15, fontWeight: 700, color: "#1E293B", marginBottom: 10 }}>
                  If fresh tiffins are delivered to your gate at exact shop rate (₹35), will you pre-order?
                </h3>

                <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 20 }}>
                  {[
                    { id: "YES_DEFINITELY", label: "✅ Yes, definitely! That saves time & money." },
                    { id: "SOMETIMES", label: "🤔 2–3 times a week when busy." },
                    { id: "MAYBE", label: "👀 Maybe, if taste & quality matches my favorite spot." }
                  ].map((opt) => (
                    <button
                      key={opt.id}
                      type="button"
                      onClick={() => setWillingToPreOrder(opt.id)}
                      style={{
                        padding: "12px 14px",
                        borderRadius: 12,
                        border: willingToPreOrder === opt.id ? "2px solid #EA580C" : "1px solid #E2E8F0",
                        background: willingToPreOrder === opt.id ? "#FFF7ED" : "#FFFFFF",
                        color: willingToPreOrder === opt.id ? "#C2410C" : "#1E293B",
                        fontWeight: willingToPreOrder === opt.id ? 700 : 500,
                        fontSize: 14,
                        textAlign: "left",
                        cursor: "pointer"
                      }}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>

                <button
                  type="button"
                  onClick={() => setStep(2)}
                  style={{
                    width: "100%",
                    padding: "14px 20px",
                    background: "#EA580C",
                    color: "#FFFFFF",
                    border: "none",
                    borderRadius: 12,
                    fontSize: 15,
                    fontWeight: 700,
                    cursor: "pointer",
                    display: "flex",
                    justifyContent: "center",
                    alignItems: "center",
                    gap: 8,
                    boxShadow: "0 4px 12px rgba(234, 88, 12, 0.3)"
                  }}
                >
                  Continue to Food Choices
                  <ArrowRight style={{ width: 16, height: 16 }} />
                </button>
              </div>
            )}

            {/* Step 2: Food Preferences */}
            {step === 2 && (
              <div
                style={{
                  background: "#FFFFFF",
                  border: "1px solid #E2E8F0",
                  borderRadius: 18,
                  padding: "20px 18px",
                  boxShadow: "0 4px 20px rgba(0,0,0,0.04)"
                }}
              >
                <div style={{ fontSize: 12, fontWeight: 700, color: "#EA580C", marginBottom: 4 }}>
                  STEP 2 OF 4 · WHAT YOU CRAVE
                </div>
                <h2 style={{ fontSize: 18, fontWeight: 800, color: "#1E293B", marginBottom: 8 }}>
                  What items would you pre-order most often?
                </h2>
                <p style={{ fontSize: 13, color: "#64748B", marginBottom: 16 }}>
                  Select all that apply. We onboard only top-rated local spots based on these choices.
                </p>

                <div style={{ display: "flex", flexDirection: "column", gap: 10, marginBottom: 22 }}>
                  {FOOD_ITEMS.map((item) => {
                    const isSelected = selectedFoods.includes(item.id);
                    return (
                      <div
                        key={item.id}
                        onClick={() => toggleFood(item.id)}
                        style={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "space-between",
                          padding: "12px 14px",
                          borderRadius: 12,
                          border: isSelected ? "2px solid #EA580C" : "1px solid #E2E8F0",
                          background: isSelected ? "#FFF7ED" : "#FFFFFF",
                          cursor: "pointer"
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                          <span style={{ fontSize: 24 }}>{item.icon}</span>
                          <div>
                            <div style={{ fontSize: 14, fontWeight: 700, color: isSelected ? "#C2410C" : "#1E293B" }}>
                              {item.label}
                            </div>
                            <div style={{ fontSize: 12, color: "#64748B" }}>{item.desc}</div>
                          </div>
                        </div>
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => {}}
                          style={{ width: 18, height: 18, accentColor: "#EA580C" }}
                        />
                      </div>
                    );
                  })}
                </div>

                <div style={{ display: "flex", gap: 10 }}>
                  <button
                    type="button"
                    onClick={() => setStep(1)}
                    style={{
                      flex: 1,
                      padding: "13px 16px",
                      background: "#F1F5F9",
                      color: "#475569",
                      border: "none",
                      borderRadius: 12,
                      fontSize: 14,
                      fontWeight: 600,
                      cursor: "pointer"
                    }}
                  >
                    Back
                  </button>
                  <button
                    type="button"
                    onClick={() => setStep(3)}
                    style={{
                      flex: 2,
                      padding: "13px 16px",
                      background: "#EA580C",
                      color: "#FFFFFF",
                      border: "none",
                      borderRadius: 12,
                      fontSize: 15,
                      fontWeight: 700,
                      cursor: "pointer",
                      display: "flex",
                      justifyContent: "center",
                      alignItems: "center",
                      gap: 8,
                      boxShadow: "0 4px 12px rgba(234, 88, 12, 0.3)"
                    }}
                  >
                    Set Location
                    <ArrowRight style={{ width: 16, height: 16 }} />
                  </button>
                </div>
              </div>
            )}

            {/* Step 3: Location (Khammam & Outside Khammam) */}
            {step === 3 && (
              <div
                style={{
                  background: "#FFFFFF",
                  border: "1px solid #E2E8F0",
                  borderRadius: 18,
                  padding: "20px 18px",
                  boxShadow: "0 4px 20px rgba(0,0,0,0.04)"
                }}
              >
                <div style={{ fontSize: 12, fontWeight: 700, color: "#EA580C", marginBottom: 4 }}>
                  STEP 3 OF 4 · NEIGHBORHOOD
                </div>
                <h2 style={{ fontSize: 18, fontWeight: 800, color: "#1E293B", marginBottom: 12 }}>
                  Where should we batch deliver?
                </h2>

                {/* City Toggle */}
                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr",
                    background: "#F1F5F9",
                    padding: 4,
                    borderRadius: 12,
                    marginBottom: 16
                  }}
                >
                  <button
                    type="button"
                    onClick={() => setIsKhammam(true)}
                    style={{
                      padding: "9px 12px",
                      borderRadius: 9,
                      border: "none",
                      fontSize: 13,
                      fontWeight: isKhammam ? 700 : 500,
                      background: isKhammam ? "#FFFFFF" : "transparent",
                      color: isKhammam ? "#EA580C" : "#64748B",
                      cursor: "pointer",
                      boxShadow: isKhammam ? "0 2px 6px rgba(0,0,0,0.06)" : "none"
                    }}
                  >
                    📍 Khammam City
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsKhammam(false)}
                    style={{
                      padding: "9px 12px",
                      borderRadius: 9,
                      border: "none",
                      fontSize: 13,
                      fontWeight: !isKhammam ? 700 : 500,
                      background: !isKhammam ? "#FFFFFF" : "transparent",
                      color: !isKhammam ? "#9333EA" : "#64748B",
                      cursor: "pointer",
                      boxShadow: !isKhammam ? "0 2px 6px rgba(0,0,0,0.06)" : "none"
                    }}
                  >
                    🚀 Other City (Outside)
                  </button>
                </div>

                {isKhammam ? (
                  <div>
                    <label style={{ fontSize: 13, fontWeight: 600, color: "#334155", display: "block", marginBottom: 6 }}>
                      Select your locality / road in Khammam:
                    </label>
                    <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 14 }}>
                      {KHAMMAM_AREAS.map((area) => (
                        <button
                          key={area}
                          type="button"
                          onClick={() => setSelectedArea(area)}
                          style={{
                            padding: "6px 12px",
                            borderRadius: 20,
                            fontSize: 12,
                            fontWeight: selectedArea === area ? 700 : 500,
                            border: selectedArea === area ? "1.5px solid #EA580C" : "1px solid #E2E8F0",
                            background: selectedArea === area ? "#FFF7ED" : "#FFFFFF",
                            color: selectedArea === area ? "#C2410C" : "#475569",
                            cursor: "pointer"
                          }}
                        >
                          {area}
                        </button>
                      ))}
                    </div>

                    {selectedArea === "Other Area in Khammam" && (
                      <div style={{ marginBottom: 14 }}>
                        <input
                          type="text"
                          placeholder="Type your area or colony name"
                          value={customArea}
                          onChange={(e) => setCustomArea(e.target.value)}
                          style={{
                            width: "100%",
                            padding: "10px 14px",
                            borderRadius: 10,
                            border: "1px solid #CBD5E1",
                            fontSize: 13,
                            outline: "none"
                          }}
                        />
                      </div>
                    )}
                  </div>
                ) : (
                  <div style={{ display: "flex", flexDirection: "column", gap: 12, marginBottom: 16 }}>
                    <div>
                      <label style={{ fontSize: 13, fontWeight: 600, color: "#334155", display: "block", marginBottom: 4 }}>
                        Your City:
                      </label>
                      <input
                        type="text"
                        placeholder="e.g. Hyderabad, Vijayawada, Warangal..."
                        value={otherCity}
                        onChange={(e) => setOtherCity(e.target.value)}
                        style={{
                          width: "100%",
                          padding: "10px 14px",
                          borderRadius: 10,
                          border: "1px solid #CBD5E1",
                          fontSize: 13,
                          outline: "none"
                        }}
                      />
                    </div>
                    <div>
                      <label style={{ fontSize: 13, fontWeight: 600, color: "#334155", display: "block", marginBottom: 4 }}>
                        Your Locality / Colony:
                      </label>
                      <input
                        type="text"
                        placeholder="e.g. Madhapur, Benz Circle, Hanamkonda..."
                        value={otherArea}
                        onChange={(e) => setOtherArea(e.target.value)}
                        style={{
                          width: "100%",
                          padding: "10px 14px",
                          borderRadius: 10,
                          border: "1px solid #CBD5E1",
                          fontSize: 13,
                          outline: "none"
                        }}
                      />
                    </div>
                  </div>
                )}

                {/* Apartment or Colony Name */}
                <div style={{ marginBottom: 20 }}>
                  <label style={{ fontSize: 13, fontWeight: 600, color: "#334155", display: "block", marginBottom: 4 }}>
                    Apartment / Gated Community / Street Name:
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Sri Ram Towers, Flat 302 or Lakeview Colony"
                    value={apartmentName}
                    onChange={(e) => setApartmentName(e.target.value)}
                    style={{
                      width: "100%",
                      padding: "10px 14px",
                      borderRadius: 10,
                      border: "1px solid #CBD5E1",
                      fontSize: 13,
                      outline: "none"
                    }}
                  />
                  <span style={{ fontSize: 11, color: "#64748B", marginTop: 4, display: "block" }}>
                    💡 Apartments with 8+ waitlist members get launched first with free priority delivery.
                  </span>
                </div>

                <div style={{ display: "flex", gap: 10 }}>
                  <button
                    type="button"
                    onClick={() => setStep(2)}
                    style={{
                      flex: 1,
                      padding: "13px 16px",
                      background: "#F1F5F9",
                      color: "#475569",
                      border: "none",
                      borderRadius: 12,
                      fontSize: 14,
                      fontWeight: 600,
                      cursor: "pointer"
                    }}
                  >
                    Back
                  </button>
                  <button
                    type="button"
                    onClick={() => setStep(4)}
                    style={{
                      flex: 2,
                      padding: "13px 16px",
                      background: "#EA580C",
                      color: "#FFFFFF",
                      border: "none",
                      borderRadius: 12,
                      fontSize: 15,
                      fontWeight: 700,
                      cursor: "pointer",
                      display: "flex",
                      justifyContent: "center",
                      alignItems: "center",
                      gap: 8,
                      boxShadow: "0 4px 12px rgba(234, 88, 12, 0.3)"
                    }}
                  >
                    Claim VIP Pass
                    <ArrowRight style={{ width: 16, height: 16 }} />
                  </button>
                </div>
              </div>
            )}

            {/* Step 4: Contact & VIP Lead Capture */}
            {step === 4 && (
              <form
                onSubmit={handleSubmit}
                style={{
                  background: "#FFFFFF",
                  border: "1px solid #E2E8F0",
                  borderRadius: 18,
                  padding: "20px 18px",
                  boxShadow: "0 4px 20px rgba(0,0,0,0.04)"
                }}
              >
                <div style={{ fontSize: 12, fontWeight: 700, color: "#EA580C", marginBottom: 4 }}>
                  FINAL STEP · VIP ACCESS
                </div>
                <h2 style={{ fontSize: 18, fontWeight: 800, color: "#1E293B", marginBottom: 8 }}>
                  Claim ₹50 Free Delivery Voucher
                </h2>
                <p style={{ fontSize: 13, color: "#64748B", marginBottom: 16 }}>
                  We will send your launch invite & voucher code strictly via WhatsApp on launch day. No spam.
                </p>

                <div style={{ display: "flex", flexDirection: "column", gap: 14, marginBottom: 20 }}>
                  <div>
                    <label style={{ fontSize: 13, fontWeight: 600, color: "#334155", display: "block", marginBottom: 4 }}>
                      Your Full Name:
                    </label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Kiran Kumar"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      style={{
                        width: "100%",
                        padding: "11px 14px",
                        borderRadius: 10,
                        border: "1px solid #CBD5E1",
                        fontSize: 14,
                        outline: "none"
                      }}
                    />
                  </div>

                  <div>
                    <label style={{ fontSize: 13, fontWeight: 600, color: "#334155", display: "block", marginBottom: 4 }}>
                      WhatsApp Phone Number:
                    </label>
                    <div style={{ display: "flex", gap: 8 }}>
                      <span
                        style={{
                          padding: "11px 12px",
                          background: "#F1F5F9",
                          borderRadius: 10,
                          fontSize: 14,
                          fontWeight: 700,
                          color: "#475569"
                        }}
                      >
                        +91
                      </span>
                      <input
                        type="tel"
                        required
                        maxLength={10}
                        placeholder="10-digit number"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value.replace(/\D/g, ""))}
                        style={{
                          flex: 1,
                          padding: "11px 14px",
                          borderRadius: 10,
                          border: "1px solid #CBD5E1",
                          fontSize: 14,
                          outline: "none"
                        }}
                      />
                    </div>
                  </div>

                  {/* Reward Card */}
                  <div
                    style={{
                      background: "linear-gradient(135deg, #FFF7ED 0%, #FEF3C7 100%)",
                      border: "1px dashed #F59E0B",
                      borderRadius: 12,
                      padding: "12px 14px",
                      display: "flex",
                      alignItems: "center",
                      gap: 10
                    }}
                  >
                    <Gift style={{ width: 22, height: 22, color: "#D97706", flexShrink: 0 }} />
                    <div style={{ fontSize: 12, color: "#92400E", lineHeight: 1.4 }}>
                      <strong>Launch Voucher: HUNGR50</strong> — Valid for ₹50 off & Free Delivery on your first order.
                    </div>
                  </div>
                </div>

                <div style={{ display: "flex", gap: 10 }}>
                  <button
                    type="button"
                    onClick={() => setStep(3)}
                    style={{
                      flex: 1,
                      padding: "13px 16px",
                      background: "#F1F5F9",
                      color: "#475569",
                      border: "none",
                      borderRadius: 12,
                      fontSize: 14,
                      fontWeight: 600,
                      cursor: "pointer"
                    }}
                  >
                    Back
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    style={{
                      flex: 2,
                      padding: "13px 16px",
                      background: "#EA580C",
                      color: "#FFFFFF",
                      border: "none",
                      borderRadius: 12,
                      fontSize: 15,
                      fontWeight: 700,
                      cursor: "pointer",
                      boxShadow: "0 4px 12px rgba(234, 88, 12, 0.3)"
                    }}
                  >
                    {submitting ? "Joining..." : "Join VIP Waitlist 🎉"}
                  </button>
                </div>
              </form>
            )}
          </div>
        ) : (
          /* Celebration & Success View */
          <div
            style={{
              background: "#FFFFFF",
              border: "1px solid #E2E8F0",
              borderRadius: 24,
              padding: "36px 20px",
              textAlign: "center",
              boxShadow: "0 10px 30px rgba(0,0,0,0.06)"
            }}
          >
            <div
              style={{
                width: 64,
                height: 64,
                borderRadius: "50%",
                background: "#DCFCE7",
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                marginBottom: 16
              }}
            >
              <CheckCircle2 style={{ width: 36, height: 36, color: "#16A34A" }} />
            </div>

            <div
              style={{
                fontSize: 12,
                fontWeight: 700,
                color: "#16A34A",
                textTransform: "uppercase",
                letterSpacing: 1,
                marginBottom: 6
              }}
            >
              VIP Access Confirmed
            </div>

            <h2 style={{ fontSize: 24, fontWeight: 900, color: "#1E293B", marginBottom: 10 }}>
              You&apos;re on the Hungr Priority List!
            </h2>

            <p style={{ fontSize: 14, color: "#64748B", maxWidth: 420, margin: "0 auto 20px", lineHeight: 1.5 }}>
              Thanks, <strong>{name}</strong>! We have registered your location in{" "}
              <strong>{isKhammam ? selectedArea : otherCity}</strong>. When morning batch deliveries start, you&apos;ll be among the first notified on WhatsApp.
            </p>

            {/* Voucher Box */}
            <div
              style={{
                background: "#FFF7ED",
                border: "2px dashed #EA580C",
                borderRadius: 14,
                padding: "16px",
                maxWidth: 320,
                margin: "0 auto 24px"
              }}
            >
              <div style={{ fontSize: 11, fontWeight: 700, color: "#9A3412", textTransform: "uppercase" }}>
                YOUR LAUNCH VOUCHER CODE
              </div>
              <div style={{ fontSize: 28, fontWeight: 900, color: "#EA580C", letterSpacing: 2, margin: "6px 0" }}>
                HUNGR50
              </div>
              <div style={{ fontSize: 12, color: "#C2410C" }}>
                ₹50 Free Credit + Free Gate Delivery
              </div>
            </div>

            {/* Viral WhatsApp Share Callout */}
            <div
              style={{
                background: "#F8FAFC",
                border: "1px solid #E2E8F0",
                borderRadius: 16,
                padding: "18px 16px",
                marginBottom: 20
              }}
            >
              <h3 style={{ fontSize: 15, fontWeight: 800, color: "#1E293B", marginBottom: 6 }}>
                Want your apartment launched first?
              </h3>
              <p style={{ fontSize: 12, color: "#64748B", margin: "0 0 14px", lineHeight: 1.4 }}>
                Share this link in your apartment / colony WhatsApp group. Routes with 8+ residents start on Day 1!
              </p>
              <button
                type="button"
                onClick={handleShareWhatsApp}
                style={{
                  width: "100%",
                  padding: "13px 18px",
                  background: "#25D366",
                  color: "#FFFFFF",
                  border: "none",
                  borderRadius: 12,
                  fontSize: 14,
                  fontWeight: 700,
                  cursor: "pointer",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  gap: 8,
                  boxShadow: "0 4px 12px rgba(37, 211, 102, 0.3)"
                }}
              >
                <Share2 style={{ width: 16, height: 16 }} />
                Share on WhatsApp Group
              </button>
            </div>

            <div style={{ fontSize: 12, color: "#94A3B8" }}>
              Questions or partner suggestions? WhatsApp us at{" "}
              <a href="https://wa.me/918019151847" style={{ color: "#EA580C", fontWeight: 600 }}>
                +91 8019151847
              </a>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
