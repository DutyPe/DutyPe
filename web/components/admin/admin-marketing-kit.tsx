"use client";

import { Noto_Sans_Devanagari, Noto_Sans_Telugu } from "next/font/google";
import QRCode from "qrcode";
import { useEffect, useMemo, useRef, useState } from "react";
import { httpsCallable } from "firebase/functions";

import { getFirebaseServices } from "@/lib/firebase/client";
import { PLAY_STORE_URL } from "@/lib/public-site";

/**
 * Marketing kit: one design system, every format and audience.
 *
 *  - Print posters (A4, 300 dpi PNG → print), Instagram feed (4:5, 1080×1350), square (1080×1080)
 *    and Story (9:16, 1080×1920), drawn on a canvas so the download is the exact pixel size.
 *  - Audiences: home-service customers, employers, workers, service partners, festive offer.
 *  - WhatsApp templates in English / Telugu / Hindi for broadcasts and status.
 *
 * Practices followed: one message per design, big headline in the reader's language, a single
 * call to action with a QR, offer shown in a high-contrast box; Instagram text kept inside the
 * 4:5 centre and the Story safe zone (top 14% / bottom 35% clear); WhatsApp copy short, leading
 * with the point, one CTA, sent only to people who agreed to hear from DutyPe.
 */

const telugu = Noto_Sans_Telugu({ subsets: ["telugu", "latin"], weight: ["400", "700", "800"], display: "swap" });
const deva = Noto_Sans_Devanagari({ subsets: ["devanagari", "latin"], weight: ["400", "700", "800"], display: "swap" });

type Lang = "en" | "te" | "hi";
type Audience = "customers" | "employers" | "workers" | "partners" | "festive";
type Format = "a4" | "feed" | "square" | "story";
type ThemeKey = "blue" | "green" | "orange" | "purple";

const FORMATS: Record<Format, { label: string; w: number; h: number; file: string }> = {
  a4: { label: "Print poster (A4)", w: 2480, h: 3508, file: "poster-a4" },
  feed: { label: "Instagram post 4:5", w: 1080, h: 1350, file: "insta-post" },
  square: { label: "Square post 1:1", w: 1080, h: 1080, file: "insta-square" },
  story: { label: "Instagram / WhatsApp story 9:16", w: 1080, h: 1920, file: "story" },
};

const THEMES: Record<ThemeKey, { name: string; from: string; to: string; accent: string }> = {
  blue: { name: "Blue", from: "#1e3a8a", to: "#0ea5e9", accent: "#fde68a" },
  green: { name: "Green", from: "#064e3b", to: "#10b981", accent: "#fde68a" },
  orange: { name: "Saffron", from: "#7c2d12", to: "#f97316", accent: "#fef3c7" },
  purple: { name: "Purple", from: "#4c1d95", to: "#a855f7", accent: "#fde68a" },
};

const AUDIENCE_THEME: Record<Audience, ThemeKey> = {
  customers: "blue", employers: "purple", workers: "green", partners: "orange", festive: "orange",
};

type Copy = { badge: string; headline: string; sub: string; points: string[]; offer: string; cta: string };

type Ctx = { city: string; partnerFee: number; firstFree: boolean; coupon: CouponLite | null; from: Record<string, number> };
type CouponLite = { code: string; title: string; type: string; value: number; maxOff: number; minOrder: number };

const off = (c: CouponLite) => (c.type === "PCT" ? `${c.value}%` : `₹${c.value}`);
const fromP = (x: Ctx, cat: string, lang: Lang) => {
  const p = x.from[cat];
  if (!p) return "";
  return lang === "te" ? ` ₹${p} నుండి` : lang === "hi" ? ` ₹${p} से` : ` from ₹${p}`;
};

/** All words on the designs, per audience and language. */
function copyFor(a: Audience, lang: Lang, x: Ctx): Copy {
  const coupon = x.coupon;
  const customerOffer = coupon ?
    { en: `Use code ${coupon.code} · ${off(coupon)} OFF`, te: `కోడ్ ${coupon.code} వాడండి · ${off(coupon)} తగ్గింపు`, hi: `कोड ${coupon.code} लगाएँ · ${off(coupon)} छूट` }[lang] :
    x.firstFree ? { en: "First booking: ₹0 booking fee", te: "మొదటి బుకింగ్: ₹0 బుకింగ్ ఫీజు", hi: "पहली बुकिंग: ₹0 बुकिंग फ़ीस" }[lang] :
      { en: "Fixed prices · pay after the work", te: "ఫిక్స్‌డ్ ధరలు · పని తర్వాత చెల్లింపు", hi: "तय दाम · काम के बाद भुगतान" }[lang];
  const t: Record<Audience, Record<Lang, Copy>> = {
    customers: {
      en: {
        badge: `Now in ${x.city}`,
        headline: "AC, cleaning, electrician & plumber at your door",
        sub: "Verified pros · fixed prices · pay after the work",
        points: [`❄️ AC service${fromP(x, "AC", lang)}`, `🧹 Home & bathroom cleaning${fromP(x, "CLEANING", lang)}`, "🔌 Electrician & 🚰 plumber, fast"],
        offer: customerOffer, cta: "Scan · Book in 30 seconds",
      },
      te: {
        badge: `ఇప్పుడు ${x.city}లో`,
        headline: "AC, క్లీనింగ్, ఎలక్ట్రీషియన్, ప్లంబర్ – మీ ఇంటికే",
        sub: "ధృవీకరించిన నిపుణులు · ఫిక్స్‌డ్ ధరలు · పని తర్వాత చెల్లించండి",
        points: [`❄️ AC సర్వీస్${fromP(x, "AC", lang)}`, `🧹 ఇల్లు & బాత్‌రూమ్ క్లీనింగ్${fromP(x, "CLEANING", lang)}`, "🔌 ఎలక్ట్రీషియన్ & 🚰 ప్లంబర్ త్వరగా"],
        offer: customerOffer, cta: "స్కాన్ చేయండి · 30 సెకన్లలో బుక్",
      },
      hi: {
        badge: `अब ${x.city} में`,
        headline: "AC, सफ़ाई, इलेक्ट्रीशियन, प्लंबर – आपके घर पर",
        sub: "सत्यापित प्रोफेशनल · तय दाम · काम के बाद भुगतान",
        points: [`❄️ AC सर्विस${fromP(x, "AC", lang)}`, `🧹 घर और बाथरूम की सफ़ाई${fromP(x, "CLEANING", lang)}`, "🔌 इलेक्ट्रीशियन और 🚰 प्लंबर, जल्दी"],
        offer: customerOffer, cta: "स्कैन करें · 30 सेकंड में बुक करें",
      },
    },
    employers: {
      en: {
        badge: "For shops, offices & homes",
        headline: "Need staff? Get workers near you today",
        sub: "Post a job free · urgent workers in minutes · call them directly",
        points: ["📝 3 free job posts every day", "⚡ Urgent workers in minutes", "🎁 Refer a friend, get a free post"],
        offer: "Free to start · no agents", cta: "Scan · Post your first job free",
      },
      te: {
        badge: "షాపులు, ఆఫీసులు & ఇళ్ల కోసం",
        headline: "సిబ్బంది కావాలా? ఈరోజే దగ్గర్లో కార్మికులు",
        sub: "ఉచితంగా జాబ్ పోస్ట్ · నిమిషాల్లో అర్జెంట్ కార్మికులు · నేరుగా కాల్",
        points: ["📝 రోజుకు 3 ఉచిత జాబ్ పోస్ట్‌లు", "⚡ నిమిషాల్లో అర్జెంట్ కార్మికులు", "🎁 స్నేహితుడిని రిఫర్ చేస్తే ఉచిత పోస్ట్"],
        offer: "ఉచితంగా మొదలుపెట్టండి · ఏజెంట్లు లేరు", cta: "స్కాన్ · మొదటి జాబ్ ఉచితంగా పోస్ట్",
      },
      hi: {
        badge: "दुकान, ऑफ़िस और घर के लिए",
        headline: "स्टाफ़ चाहिए? आज ही पास के कामगार पाएँ",
        sub: "मुफ़्त जॉब पोस्ट · मिनटों में अर्जेंट कामगार · सीधे कॉल",
        points: ["📝 रोज़ 3 मुफ़्त जॉब पोस्ट", "⚡ मिनटों में अर्जेंट कामगार", "🎁 दोस्त को रेफ़र करें, मुफ़्त पोस्ट पाएँ"],
        offer: "मुफ़्त शुरुआत · कोई एजेंट नहीं", cta: "स्कैन करें · पहली जॉब मुफ़्त पोस्ट करें",
      },
    },
    workers: {
      en: {
        badge: "100% free for workers",
        headline: "Jobs near you. Free.",
        sub: "Driver, cook, helper, sales, delivery, security & more",
        points: ["📍 Jobs in your own area", "📞 Call the employer directly", "💰 No fees, no agents"],
        offer: "Daily work & full-time jobs", cta: "Scan · Find a job today",
      },
      te: {
        badge: "కార్మికులకు పూర్తిగా ఉచితం",
        headline: "మీ దగ్గర్లోనే ఉద్యోగాలు. ఉచితం.",
        sub: "డ్రైవర్, వంట, హెల్పర్, సేల్స్, డెలివరీ, సెక్యూరిటీ & మరిన్ని",
        points: ["📍 మీ ఏరియాలోనే పనులు", "📞 యజమానికి నేరుగా కాల్", "💰 ఫీజు లేదు, ఏజెంట్ లేరు"],
        offer: "రోజువారీ పని & ఫుల్-టైమ్ ఉద్యోగాలు", cta: "స్కాన్ · ఈరోజే పని వెతకండి",
      },
      hi: {
        badge: "कामगारों के लिए 100% मुफ़्त",
        headline: "आपके पास नौकरियाँ. मुफ़्त.",
        sub: "ड्राइवर, कुक, हेल्पर, सेल्स, डिलीवरी, सिक्योरिटी और भी",
        points: ["📍 अपने इलाक़े में काम", "📞 मालिक को सीधे कॉल", "💰 कोई फ़ीस नहीं, कोई एजेंट नहीं"],
        offer: "रोज़ का काम और फुल-टाइम नौकरी", cta: "स्कैन करें · आज ही काम पाएँ",
      },
    },
    partners: {
      en: {
        badge: `DutyPe partners · ${x.city}`,
        headline: "Electrician? Plumber? AC technician? Earn more",
        sub: `Get home-service jobs near you in ${x.city}`,
        points: [`🧾 Only ₹${x.partnerFee} per job · first job free`, "📲 Jobs come straight to your phone", "💵 The customer pays you directly"],
        offer: "Join free · verified badge", cta: "Scan · Become a partner",
      },
      te: {
        badge: `DutyPe పార్ట్నర్లు · ${x.city}`,
        headline: "ఎలక్ట్రీషియనా? ప్లంబరా? AC టెక్నీషియనా? ఎక్కువ సంపాదించండి",
        sub: `${x.city}లో మీ దగ్గర్లోని ఇంటి సర్వీస్ పనులు పొందండి`,
        points: [`🧾 ఒక్కో పనికి ₹${x.partnerFee} మాత్రమే · మొదటి పని ఉచితం`, "📲 పనులు నేరుగా మీ ఫోన్‌కి", "💵 కస్టమర్ నేరుగా మీకే చెల్లిస్తారు"],
        offer: "ఉచితంగా చేరండి · వెరిఫైడ్ బ్యాడ్జ్", cta: "స్కాన్ · పార్ట్నర్‌గా చేరండి",
      },
      hi: {
        badge: `DutyPe पार्टनर · ${x.city}`,
        headline: "इलेक्ट्रीशियन? प्लंबर? AC टेक्नीशियन? ज़्यादा कमाएँ",
        sub: `${x.city} में अपने पास घरेलू सर्विस का काम पाएँ`,
        points: [`🧾 हर काम पर सिर्फ़ ₹${x.partnerFee} · पहला काम मुफ़्त`, "📲 काम सीधे आपके फ़ोन पर", "💵 ग्राहक सीधे आपको भुगतान करता है"],
        offer: "मुफ़्त जुड़ें · वेरिफ़ाइड बैज", cta: "स्कैन करें · पार्टनर बनें",
      },
    },
    festive: {
      en: {
        badge: coupon ? coupon.title : "Festive offer",
        headline: coupon ? `${off(coupon)} OFF home services this festival` : "Festival-ready homes, with DutyPe",
        sub: "Deep cleaning, AC service, electrical & plumbing – before the guests arrive",
        points: ["🪔 Full home & kitchen cleaning", "❄️ AC service & repairs", "💡 Lights, wiring & taps fixed"],
        offer: customerOffer, cta: "Scan · Book your slot now",
      },
      te: {
        badge: coupon ? coupon.title : "పండుగ ఆఫర్",
        headline: coupon ? `పండుగకు ఇంటి సేవలపై ${off(coupon)} తగ్గింపు` : "పండుగకు ఇల్లు రెడీ – DutyPeతో",
        sub: "డీప్ క్లీనింగ్, AC సర్వీస్, ఎలక్ట్రికల్ & ప్లంబింగ్ – అతిథులు వచ్చే ముందే",
        points: ["🪔 ఇల్లు & కిచెన్ పూర్తి క్లీనింగ్", "❄️ AC సర్వీస్ & రిపేర్లు", "💡 లైట్లు, వైరింగ్ & ట్యాప్‌లు సరిచేయడం"],
        offer: customerOffer, cta: "స్కాన్ · ఇప్పుడే స్లాట్ బుక్ చేయండి",
      },
      hi: {
        badge: coupon ? coupon.title : "त्योहार ऑफ़र",
        headline: coupon ? `त्योहार पर घरेलू सेवाओं पर ${off(coupon)} छूट` : "त्योहार के लिए घर तैयार – DutyPe के साथ",
        sub: "डीप क्लीनिंग, AC सर्विस, बिजली और प्लंबिंग – मेहमानों के आने से पहले",
        points: ["🪔 पूरे घर और किचन की सफ़ाई", "❄️ AC सर्विस और रिपेयर", "💡 लाइट, वायरिंग और नल ठीक"],
        offer: customerOffer, cta: "स्कैन करें · अभी स्लॉट बुक करें",
      },
    },
  };
  return t[a][lang];
}

// ─────────────────────────── canvas drawing ───────────────────────────

function roundRect(c: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  c.beginPath();
  c.moveTo(x + r, y);
  c.arcTo(x + w, y, x + w, y + h, r);
  c.arcTo(x + w, y + h, x, y + h, r);
  c.arcTo(x, y + h, x, y, r);
  c.arcTo(x, y, x + w, y, r);
  c.closePath();
}

/** Wraps [text] to [maxW]; returns the lines. */
function wrap(c: CanvasRenderingContext2D, text: string, maxW: number): string[] {
  const words = text.split(" ");
  const lines: string[] = [];
  let line = "";
  for (const w of words) {
    const next = line ? `${line} ${w}` : w;
    if (c.measureText(next).width > maxW && line) {
      lines.push(line);
      line = w;
    } else line = next;
  }
  if (line) lines.push(line);
  return lines;
}

type DrawOpts = { copy: Copy; theme: ThemeKey; format: Format; qr: HTMLImageElement | null; area: string; phone: string; code: string; family: string };

function draw(canvas: HTMLCanvasElement, o: DrawOpts) {
  const { w: W, h: H } = FORMATS[o.format];
  canvas.width = W;
  canvas.height = H;
  const c = canvas.getContext("2d");
  if (!c) return;
  const u = W / 1080;
  const th = THEMES[o.theme];
  const F = (weight: number, size: number) => `${weight} ${Math.round(size * u)}px ${o.family}`;
  const story = o.format === "story";
  const square = o.format === "square";
  const pad = (o.format === "feed" ? 90 : 70) * u; // 4:5 → keep text inside the 3:4 centre

  // Background
  const g = c.createLinearGradient(0, 0, W, H);
  g.addColorStop(0, th.from);
  g.addColorStop(1, th.to);
  c.fillStyle = g;
  c.fillRect(0, 0, W, H);
  // Soft circles for depth
  c.globalAlpha = 0.12;
  c.fillStyle = "#ffffff";
  c.beginPath(); c.arc(W * 0.95, H * 0.08, 260 * u, 0, Math.PI * 2); c.fill();
  c.beginPath(); c.arc(W * 0.02, H * 0.9, 320 * u, 0, Math.PI * 2); c.fill();
  c.globalAlpha = 1;

  let y = story ? H * 0.14 : 70 * u;
  const maxW = W - pad * 2;

  // Brand row
  c.fillStyle = "#ffffff";
  roundRect(c, pad, y, 76 * u, 76 * u, 20 * u); c.fill();
  c.fillStyle = th.from;
  c.font = F(800, 50); c.textBaseline = "middle"; c.textAlign = "center";
  c.fillText("D", pad + 38 * u, y + 40 * u);
  c.textAlign = "left";
  c.fillStyle = "#ffffff";
  c.font = F(800, 44);
  c.fillText("DutyPe", pad + 96 * u, y + 38 * u);
  if (o.area) {
    c.font = F(700, 28);
    const t = `📍 ${o.area}`;
    const tw = c.measureText(t).width;
    c.fillStyle = "rgba(255,255,255,0.18)";
    roundRect(c, W - pad - tw - 40 * u, y + 12 * u, tw + 40 * u, 54 * u, 27 * u); c.fill();
    c.fillStyle = "#ffffff";
    c.fillText(t, W - pad - tw - 20 * u, y + 40 * u);
  }
  y += 76 * u + (square ? 30 : 50) * u;

  // Badge
  c.font = F(700, 30);
  const bw = c.measureText(o.copy.badge).width + 44 * u;
  c.fillStyle = th.accent;
  roundRect(c, pad, y, bw, 58 * u, 29 * u); c.fill();
  c.fillStyle = th.from;
  c.fillText(o.copy.badge, pad + 22 * u, y + 30 * u);
  y += 58 * u + 30 * u;

  // Headline
  const hSize = square ? 70 : story ? 76 : o.format === "a4" ? 74 : 82;
  c.font = F(800, hSize);
  c.fillStyle = "#ffffff";
  c.textBaseline = "top";
  for (const line of wrap(c, o.copy.headline, maxW)) {
    c.fillText(line, pad, y);
    y += hSize * 1.22 * u;
  }
  y += 10 * u;

  // Sub
  c.font = F(400, square ? 32 : 36);
  c.fillStyle = "rgba(255,255,255,0.9)";
  for (const line of wrap(c, o.copy.sub, maxW)) {
    c.fillText(line, pad, y);
    y += (square ? 42 : 48) * u;
  }
  y += 24 * u;

  // Points (not on square / story: keep those to one message)
  const cardH = 300 * u;
  if (!square && !story) {
    for (const p of o.copy.points) {
      // Only as many points as fit above the offer box and the QR card.
      if (y + 100 * u + 140 * u + cardH + 60 * u > H) break;
      c.fillStyle = "rgba(255,255,255,0.14)";
      roundRect(c, pad, y, maxW, 84 * u, 22 * u); c.fill();
      c.fillStyle = "#ffffff";
      c.font = F(700, 36);
      c.fillText(p, pad + 28 * u, y + 22 * u);
      y += 100 * u;
    }
    y += 10 * u;
  }

  // Offer box
  c.font = F(800, square ? 38 : 42);
  const offerLines = wrap(c, o.copy.offer, maxW - 60 * u);
  const oh = offerLines.length * 54 * u + 40 * u;
  c.fillStyle = th.accent;
  roundRect(c, pad, y, maxW, oh, 26 * u); c.fill();
  c.fillStyle = "#7c2d12";
  let oy = y + 22 * u;
  for (const l of offerLines) { c.fillText(`${offerLines.indexOf(l) === 0 ? "🎁 " : ""}${l}`, pad + 30 * u, oy); oy += 54 * u; }
  y += oh + 30 * u;

  // QR card at the bottom (story: stays above the bottom 35%)
  // Story: below the content, from 42% down; it may reach 80% (the reply bar sits in the last ~15%).
  const cardY = story ? Math.min(Math.max(y, H * 0.42), H * 0.8 - cardH) :
    Math.min(Math.max(y, H - cardH - 60 * u), H - cardH - 20 * u);
  c.fillStyle = "#ffffff";
  roundRect(c, pad, cardY, maxW, cardH, 30 * u); c.fill();
  const qs = cardH - 50 * u;
  if (o.qr) c.drawImage(o.qr, pad + 25 * u, cardY + 25 * u, qs, qs);
  const tx = pad + qs + 55 * u;
  const tw = maxW - qs - 80 * u;
  c.fillStyle = "#0f172a";
  c.font = F(800, 38);
  let ty = cardY + 34 * u;
  for (const l of wrap(c, o.copy.cta, tw).slice(0, 2)) { c.fillText(l, tx, ty); ty += 48 * u; }
  c.font = F(700, 28);
  c.fillStyle = "#475569";
  c.fillText("▶ Google Play → DutyPe", tx, ty + 6 * u);
  ty += 46 * u;
  if (o.code) { c.fillStyle = th.from; c.fillText(`Code: ${o.code}`, tx, ty); ty += 40 * u; }
  if (o.phone) { c.fillStyle = "#0f172a"; c.fillText(`📞 ${o.phone}`, tx, ty); }
}

// ─────────────────────────── WhatsApp templates ───────────────────────────

function whatsappTemplates(lang: Lang, x: Ctx, link: string, code: string): Array<{ title: string; text: string }> {
  const cp = x.coupon;
  const offerEn = cp ? `Use code *${cp.code}* for ${off(cp)} off.` : x.firstFree ? "Your first booking has *no booking fee*." : "Fixed prices, pay after the work.";
  const offerTe = cp ? `*${cp.code}* కోడ్‌తో ${off(cp)} తగ్గింపు.` : x.firstFree ? "మొదటి బుకింగ్‌కు *బుకింగ్ ఫీజు లేదు*." : "ఫిక్స్‌డ్ ధరలు, పని తర్వాత చెల్లింపు.";
  const offerHi = cp ? `कोड *${cp.code}* से ${off(cp)} छूट.` : x.firstFree ? "पहली बुकिंग पर *कोई बुकिंग फ़ीस नहीं*." : "तय दाम, काम के बाद भुगतान.";
  const ref = code ? { en: ` Use my code ${code} when you join.`, te: ` చేరేటప్పుడు నా కోడ్ ${code} వాడండి.`, hi: ` जुड़ते समय मेरा कोड ${code} डालें.` }[lang] : "";
  const T: Record<Lang, Array<{ title: string; text: string }>> = {
    en: [
      { title: "Home services (customers)", text: `Hi {{name}} 👋\nAC service, cleaning, electrician or plumber at home in ${x.city} – verified pros, fixed prices.\n${offerEn}\nBook in 30 sec: ${link}\n\nReply STOP to opt out.` },
      { title: "Festive offer", text: `🪔 Festival offer, {{name}}!\nGet your home festival-ready: deep cleaning, AC service, lights & taps fixed.\n${offerEn}\nBook now: ${link}\n\nReply STOP to opt out.` },
      { title: "Employers – hire staff", text: `Hi {{name}}, need staff for your shop or office?\nPost a job free on DutyPe and get workers near you today. Need someone right now? Use Urgent and workers respond in minutes.${ref}\nStart: ${link}` },
      { title: "Workers – find jobs", text: `Looking for work? 💼\nDutyPe shows jobs near you – driver, cook, helper, sales, delivery and more. Call the employer directly. 100% free, no agents.${ref}\nDownload: ${link}` },
      { title: "Partners – electricians, plumbers, AC techs", text: `Electrician / plumber / AC technician in ${x.city}? 🔧\nGet home-service jobs on your phone with DutyPe. The customer pays you directly; DutyPe takes only ₹${x.partnerFee} per job and your first job is free.\nJoin free: ${link}` },
      { title: "Referral (share with friends)", text: `I use DutyPe for staff and home services in ${x.city}. Join with my link and we both benefit 🎁${ref}\n${link}` },
    ],
    te: [
      { title: "ఇంటి సేవలు (కస్టమర్లు)", text: `నమస్తే {{name}} గారు 👋\n${x.city}లో ఇంటికే AC సర్వీస్, క్లీనింగ్, ఎలక్ట్రీషియన్, ప్లంబర్ – ధృవీకరించిన నిపుణులు, ఫిక్స్‌డ్ ధరలు.\n${offerTe}\n30 సెకన్లలో బుక్ చేయండి: ${link}\n\nవద్దనుకుంటే STOP అని పంపండి.` },
      { title: "పండుగ ఆఫర్", text: `🪔 పండుగ ఆఫర్, {{name}} గారు!\nఇల్లు పండుగకు రెడీ: డీప్ క్లీనింగ్, AC సర్వీస్, లైట్లు & ట్యాప్‌లు రిపేర్.\n${offerTe}\nఇప్పుడే బుక్ చేయండి: ${link}\n\nవద్దనుకుంటే STOP అని పంపండి.` },
      { title: "యజమానులు – సిబ్బంది కావాలా", text: `నమస్తే {{name}} గారు, మీ షాప్ / ఆఫీస్‌కి సిబ్బంది కావాలా?\nDutyPeలో ఉచితంగా జాబ్ పోస్ట్ చేయండి, ఈరోజే దగ్గర్లో కార్మికులు. ఇప్పుడే మనిషి కావాలంటే అర్జెంట్ పోస్ట్ – నిమిషాల్లో స్పందన.${ref}\nమొదలుపెట్టండి: ${link}` },
      { title: "కార్మికులు – ఉద్యోగాలు", text: `పని కోసం చూస్తున్నారా? 💼\nDutyPeలో మీ దగ్గర్లోని ఉద్యోగాలు – డ్రైవర్, వంట, హెల్పర్, సేల్స్, డెలివరీ & మరిన్ని. యజమానికి నేరుగా కాల్. పూర్తిగా ఉచితం, ఏజెంట్లు లేరు.${ref}\nడౌన్‌లోడ్: ${link}` },
      { title: "పార్ట్నర్లు – ఎలక్ట్రీషియన్, ప్లంబర్, AC", text: `${x.city}లో ఎలక్ట్రీషియన్ / ప్లంబర్ / AC టెక్నీషియనా? 🔧\nDutyPeతో ఇంటి సర్వీస్ పనులు మీ ఫోన్‌కే. కస్టమర్ నేరుగా మీకే చెల్లిస్తారు; ఒక్కో పనికి DutyPe ₹${x.partnerFee} మాత్రమే, మొదటి పని ఉచితం.\nఉచితంగా చేరండి: ${link}` },
      { title: "రిఫరల్ (స్నేహితులకు)", text: `${x.city}లో సిబ్బంది, ఇంటి సేవల కోసం నేను DutyPe వాడుతున్నాను. నా లింక్‌తో చేరండి 🎁${ref}\n${link}` },
    ],
    hi: [
      { title: "घरेलू सेवाएँ (ग्राहक)", text: `नमस्ते {{name}} जी 👋\n${x.city} में घर पर AC सर्विस, सफ़ाई, इलेक्ट्रीशियन, प्लंबर – सत्यापित प्रोफेशनल, तय दाम.\n${offerHi}\n30 सेकंड में बुक करें: ${link}\n\nबंद करने के लिए STOP भेजें.` },
      { title: "त्योहार ऑफ़र", text: `🪔 त्योहार ऑफ़र, {{name}} जी!\nघर को त्योहार के लिए तैयार करें: डीप क्लीनिंग, AC सर्विस, लाइट और नल ठीक.\n${offerHi}\nअभी बुक करें: ${link}\n\nबंद करने के लिए STOP भेजें.` },
      { title: "मालिक – स्टाफ़ चाहिए", text: `नमस्ते {{name}} जी, दुकान या ऑफ़िस के लिए स्टाफ़ चाहिए?\nDutyPe पर मुफ़्त जॉब पोस्ट करें और आज ही पास के कामगार पाएँ. अभी किसी की ज़रूरत? अर्जेंट पोस्ट करें, मिनटों में जवाब.${ref}\nशुरू करें: ${link}` },
      { title: "कामगार – नौकरी", text: `काम ढूँढ रहे हैं? 💼\nDutyPe पर आपके पास की नौकरियाँ – ड्राइवर, कुक, हेल्पर, सेल्स, डिलीवरी और भी. मालिक को सीधे कॉल. 100% मुफ़्त, कोई एजेंट नहीं.${ref}\nडाउनलोड: ${link}` },
      { title: "पार्टनर – इलेक्ट्रीशियन, प्लंबर, AC", text: `${x.city} में इलेक्ट्रीशियन / प्लंबर / AC टेक्नीशियन हैं? 🔧\nDutyPe से घरेलू सर्विस का काम सीधे फ़ोन पर. ग्राहक सीधे आपको भुगतान करता है; DutyPe हर काम पर सिर्फ़ ₹${x.partnerFee} लेता है और पहला काम मुफ़्त.\nमुफ़्त जुड़ें: ${link}` },
      { title: "रेफ़रल (दोस्तों के लिए)", text: `${x.city} में स्टाफ़ और घरेलू सेवाओं के लिए मैं DutyPe इस्तेमाल करता हूँ. मेरे लिंक से जुड़ें 🎁${ref}\n${link}` },
    ],
  };
  return T[lang];
}

// ─────────────────────────── component ───────────────────────────

export function AdminMarketingKit() {
  const services = useMemo(() => getFirebaseServices(), []);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [audience, setAudience] = useState<Audience>("customers");
  const [format, setFormat] = useState<Format>("feed");
  const [lang, setLang] = useState<Lang>("te");
  const [theme, setTheme] = useState<ThemeKey | "auto">("auto");
  const [area, setArea] = useState("Khammam");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [couponCode, setCouponCode] = useState("");
  const [ctx, setCtx] = useState<Ctx>({ city: "Khammam", partnerFee: 19, firstFree: true, coupon: null, from: {} });
  const [coupons, setCoupons] = useState<CouponLite[]>([]);
  const [qr, setQr] = useState<HTMLImageElement | null>(null);
  const [copied, setCopied] = useState<string | null>(null);

  // Live prices, partner fee and offers from the services config.
  useEffect(() => {
    if (!services) return;
    void (async () => {
      try {
        const res = await httpsCallable(services.functions, "getServiceCatalog")({});
        const d = res.data as {
          city?: string; partnerFee?: number; firstBookingFeeFree?: boolean;
          offers?: CouponLite[]; services?: Array<{ category: string; price: number; inspection?: boolean }>;
        };
        const from: Record<string, number> = {};
        for (const s of d.services || []) {
          if (s.inspection) continue;
          from[s.category] = Math.min(from[s.category] ?? Infinity, s.price);
        }
        setCoupons(d.offers || []);
        setCtx((c) => ({ ...c, city: d.city || c.city, partnerFee: d.partnerFee ?? c.partnerFee, firstFree: d.firstBookingFeeFree !== false, from }));
      } catch {
        // Offline / not deployed: the kit still works with defaults.
      }
    })();
  }, [services]);

  const coupon = coupons.find((c) => c.code === couponCode) || null;
  const x: Ctx = { ...ctx, coupon };
  const campaign = `${audience}_${format}_${lang}`;
  const link = `${PLAY_STORE_URL}&referrer=${encodeURIComponent(`utm_source=offline&utm_medium=${format === "a4" ? "poster" : "social"}&utm_campaign=${campaign}${code ? `&ref=${code}` : ""}`)}`;
  const shortLink = PLAY_STORE_URL;

  useEffect(() => {
    let alive = true;
    QRCode.toDataURL(link, { margin: 1, width: 600, errorCorrectionLevel: "M" }).then((url) => {
      const img = new Image();
      img.onload = () => alive && setQr(img);
      img.src = url;
    }).catch(() => setQr(null));
    return () => { alive = false; };
  }, [link]);

  const family = `${telugu.style.fontFamily}, ${deva.style.fontFamily}, "Noto Color Emoji", sans-serif`;
  const copy = copyFor(audience, lang, x);
  const themeKey = theme === "auto" ? AUDIENCE_THEME[audience] : theme;

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    let alive = true;
    // Canvas text needs the web fonts loaded first.
    void Promise.all([
      document.fonts.load(`800 40px ${telugu.style.fontFamily}`),
      document.fonts.load(`800 40px ${deva.style.fontFamily}`),
      document.fonts.load(`400 40px ${telugu.style.fontFamily}`),
    ]).catch(() => null).then(() => {
      if (alive) draw(canvas, { copy, theme: themeKey, format, qr, area, phone, code, family });
    });
    return () => { alive = false; };
  }, [copy, themeKey, format, qr, area, phone, code, family]);

  function download() {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const a = document.createElement("a");
    a.href = canvas.toDataURL("image/png");
    a.download = `dutype-${audience}-${FORMATS[format].file}-${lang}.png`;
    a.click();
  }

  function printPoster() {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const w = window.open("", "_blank");
    if (!w) return;
    w.document.write(`<!doctype html><title>DutyPe poster</title><style>@page{size:A4;margin:0}html,body{margin:0}img{width:100%;height:auto;display:block}</style><img src="${canvas.toDataURL("image/png")}" onload="setTimeout(()=>window.print(),300)">`);
    w.document.close();
  }

  async function copyText(t: string, id: string) {
    try {
      await navigator.clipboard.writeText(t);
      setCopied(id);
      setTimeout(() => setCopied(null), 1500);
    } catch {
      setCopied(null);
    }
  }

  const btn = (on: boolean) => ({
    padding: "6px 12px", borderRadius: 999, border: `1px solid ${on ? "#1d4ed8" : "#cbd5e1"}`,
    background: on ? "#1d4ed8" : "#fff", color: on ? "#fff" : "#0f172a", cursor: "pointer", fontSize: 13,
  });
  const templates = whatsappTemplates(lang, x, shortLink, code);

  return (
    <section className="admin-section" style={{ marginTop: 32 }}>
      <h2 className="admin-section-title">Marketing kit – posters, Instagram &amp; WhatsApp</h2>
      <p style={{ maxWidth: 820 }}>
        Pick who it is for, the format and the language. Prices, the partner fee and offers come live from DutyPe Services settings.
        Every QR links to the Play Store with a campaign tag, so installs from each poster or post can be counted.
      </p>

      <div style={{ display: "grid", gap: 10, marginBottom: 16 }}>
        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
          <b style={{ width: 90 }}>For</b>
          {([["customers", "Home-service customers"], ["employers", "Employers"], ["workers", "Workers"], ["partners", "Service partners"], ["festive", "Festive offer"]] as Array<[Audience, string]>).map(([k, l]) => (
            <button key={k} style={btn(audience === k)} onClick={() => setAudience(k)}>{l}</button>
          ))}
        </div>
        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
          <b style={{ width: 90 }}>Format</b>
          {(Object.keys(FORMATS) as Format[]).map((k) => (
            <button key={k} style={btn(format === k)} onClick={() => setFormat(k)}>{FORMATS[k].label}</button>
          ))}
        </div>
        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
          <b style={{ width: 90 }}>Language</b>
          {([["te", "తెలుగు"], ["en", "English"], ["hi", "हिन्दी"]] as Array<[Lang, string]>).map(([k, l]) => (
            <button key={k} style={btn(lang === k)} onClick={() => setLang(k)}>{l}</button>
          ))}
          <b style={{ width: 70, marginLeft: 16 }}>Colour</b>
          <button style={btn(theme === "auto")} onClick={() => setTheme("auto")}>Auto</button>
          {(Object.keys(THEMES) as ThemeKey[]).map((k) => (
            <button key={k} style={btn(theme === k)} onClick={() => setTheme(k)}>{THEMES[k].name}</button>
          ))}
        </div>
        <div style={{ display: "flex", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
          <label>Area <input value={area} onChange={(e) => setArea(e.target.value)} style={{ width: 140 }} /></label>
          <label>Phone <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="optional" style={{ width: 140 }} /></label>
          <label>Referral code <input value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} placeholder="optional" style={{ width: 120 }} /></label>
          <label>
            Offer{" "}
            <select value={couponCode} onChange={(e) => setCouponCode(e.target.value)}>
              <option value="">{ctx.firstFree ? "First booking ₹0 fee" : "No coupon"}</option>
              {coupons.map((c) => <option key={c.code} value={c.code}>{c.code} – {c.title}</option>)}
            </select>
          </label>
        </div>
      </div>

      <div style={{ display: "flex", gap: 24, flexWrap: "wrap", alignItems: "flex-start" }}>
        <div>
          <canvas
            ref={canvasRef}
            style={{ width: format === "a4" ? 360 : format === "story" ? 300 : 400, height: "auto", borderRadius: 12, boxShadow: "0 8px 30px rgba(0,0,0,.18)" }}
          />
          <div style={{ display: "flex", gap: 8, marginTop: 10 }}>
            <button className="btn btn-approve" onClick={download}>Download PNG ({FORMATS[format].w}×{FORMATS[format].h})</button>
            {format === "a4" && <button className="btn" onClick={printPoster}>Print</button>}
          </div>
        </div>
        <div style={{ maxWidth: 420, fontSize: 14, lineHeight: 1.5 }}>
          <h3>Where to use it</h3>
          <ul>
            <li><b>A4 poster</b>: print on 130–170 gsm, put at eye level near hardware / electrical shops, apartment notice boards, tea stalls, bus stops. One poster = one audience.</li>
            <li><b>Instagram 4:5</b>: feed post. Text stays inside the centre so the 3:4 grid crop does not cut it. Add the caption from a WhatsApp template.</li>
            <li><b>Story 9:16</b>: Instagram & WhatsApp Status. Top 14% and bottom 35% are kept clear for the app buttons.</li>
            <li><b>Square</b>: Facebook, WhatsApp groups, Google Business posts.</li>
            <li>Post Telugu first in Khammam; use the Hindi version near markets and construction sites with migrant workers.</li>
          </ul>
        </div>
      </div>

      <h3 style={{ marginTop: 28 }}>WhatsApp templates ({lang === "te" ? "తెలుగు" : lang === "hi" ? "हिन्दी" : "English"})</h3>
      <p style={{ maxWidth: 820 }}>
        Send only to people who agreed to hear from DutyPe (customers, partners, employers who gave their number). Use a WhatsApp Business
        broadcast list (it reaches only contacts who saved your number) or, for real bulk sending, submit these as Marketing templates in the
        WhatsApp Business API with {"{{name}}"} as the variable and a “Book now” / “Download” button. Keep one call to action, lead with the
        offer, and send at most one promotional message a week.
      </p>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))", gap: 12 }}>
        {templates.map((t, i) => (
          <div key={t.title} style={{ border: "1px solid #e2e8f0", borderRadius: 12, padding: 12, background: "#f0fdf4" }}>
            <b>{t.title}</b>
            <pre style={{ whiteSpace: "pre-wrap", fontFamily: "inherit", fontSize: 13, margin: "8px 0" }}>{t.text}</pre>
            <div style={{ display: "flex", gap: 8 }}>
              <button className="btn" onClick={() => void copyText(t.text, `w${i}`)}>{copied === `w${i}` ? "Copied ✓" : "Copy"}</button>
              <a className="btn" href={`https://wa.me/?text=${encodeURIComponent(t.text.replace(/\{\{name\}\}/g, ""))}`} target="_blank" rel="noreferrer">Open in WhatsApp</a>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
