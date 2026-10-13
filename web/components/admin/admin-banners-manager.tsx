"use client";

import { useEffect, useMemo, useState } from "react";
import { doc, getDoc, setDoc, collection, getDocs, deleteDoc } from "firebase/firestore";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { getFirebaseServices } from "@/lib/firebase/client";
import Lottie from "lottie-react";
import {
  Plus,
  Trash2,
  Smartphone,
  Eye,
  Sparkles,
  Check,
  RefreshCw,
  ExternalLink,
  Upload,
  Film,
  FileCheck,
  AlertCircle,
  Store,
  Phone,
  MessageCircle,
  MapPin,
  Globe,
  Star,
  Copy,
  Edit3,
  X,
  Tag,
  Share2
} from "lucide-react";

export interface PromoBannerDef {
  id: string;
  headline: string;
  subheadline: string;
  cta: string;
  imageUrl?: string;
  lottieUrl?: string;
  fileSizeBytes?: number;
  bgStartColor: string;
  bgEndColor: string;
  targetServiceId?: string;
  targetCategoryId?: string;
  targetShowcaseId?: string;
  imageScale?: number;
  active: boolean;
}

export interface PromoShowcaseButtonDef {
  label: string;
  icon: "call" | "whatsapp" | "maps" | "url" | string;
  actionUrl: string;
  isPrimary: boolean;
}

export interface PromoShowcaseDef {
  id: string;
  title: string;
  tagline: string;
  categoryBadge: string;
  rating: string;
  heroImageUrl: string;
  videoUrl: string;
  aboutDescription: string;
  address: string;
  mapLink: string;
  discountOfferTitle: string;
  discountOfferCode: string;
  discountOfferTerms: string;
  highlights: string[];
  galleryImages: string[];
  actionButtons: PromoShowcaseButtonDef[];
  district?: string;
  scope?: "DISTRICT" | "STATE" | "NATIONAL" | string;
  active?: boolean;
  updatedAt?: number;
}

export const EMPTY_SHOWCASE: PromoShowcaseDef = {
  id: "",
  title: "",
  tagline: "",
  categoryBadge: "RESTAURANT",
  rating: "4.8",
  heroImageUrl: "",
  videoUrl: "",
  aboutDescription: "",
  address: "",
  mapLink: "",
  discountOfferTitle: "",
  discountOfferCode: "",
  discountOfferTerms: "",
  highlights: [],
  galleryImages: [],
  district: "Khammam",
  scope: "DISTRICT",
  active: true,
  actionButtons: [
    { label: "Call Store", icon: "call", actionUrl: "tel:+91", isPrimary: true },
    { label: "WhatsApp", icon: "whatsapp", actionUrl: "https://wa.me/91", isPrimary: false }
  ]
};

export const PRESET_SHOWCASES: PromoShowcaseDef[] = [
  {
    id: "paradise_biryani_khammam",
    title: "Paradise Biryani & Grill",
    tagline: "Authentic Nizami Dum Biryani, Sizzling Kebabs & AC Family Dining",
    categoryBadge: "RESTAURANT",
    rating: "4.8",
    district: "Khammam",
    scope: "DISTRICT",
    active: true,
    heroImageUrl: "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=800&q=80",
    videoUrl: "",
    aboutDescription: "Khammam's favorite destination for royal Hyderabadi Dum Biryani cooked with fragrant aged basmati rice, tender farm-fresh meat, and secret Nizami spices. Featuring comfortable family seating, ambient AC dining hall, and lightning-fast takeaway service.",
    address: "Beside RTC Bus Stand, Wyra Road, Khammam, Telangana 507001",
    mapLink: "https://maps.google.com/?q=Paradise+Biryani+Khammam",
    discountOfferTitle: "Flat 20% Off Food Bill",
    discountOfferCode: "PARADISE20",
    discountOfferTerms: "Valid on dine-in & takeaway bills above ₹499. Show this voucher code to billing staff before bill generation.",
    highlights: [
      "AC Family Dining Hall",
      "Authentic Hyderabadi Dum Cooking",
      "Free Valet Parking",
      "100% Halal Certified Meat",
      "Party & Banquet Hall Available",
      "Open 11:30 AM to 11:00 PM"
    ],
    galleryImages: [
      "https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=600&q=80",
      "https://images.unsplash.com/photo-1544025162-d76694265947?w=600&q=80",
      "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=600&q=80"
    ],
    actionButtons: [
      { label: "Call for Table", icon: "call", actionUrl: "tel:+918742223456", isPrimary: true },
      { label: "WhatsApp Order", icon: "whatsapp", actionUrl: "https://wa.me/918742223456?text=Hi%20Paradise%20Biryani,%20I%20would%20like%20to%20order", isPrimary: false },
      { label: "Directions", icon: "maps", actionUrl: "https://maps.google.com/?q=Paradise+Biryani+Wyra+Road+Khammam", isPrimary: false }
    ]
  },
  {
    id: "lotus_grand_mall_khammam",
    title: "Lotus Grand Shopping Mall",
    tagline: "Fashion, Electronics, Food Court & 4K Cinema Under One Roof",
    categoryBadge: "SHOPPING MALL",
    rating: "4.7",
    heroImageUrl: "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?w=800&q=80",
    videoUrl: "",
    aboutDescription: "Spread over 4 expansive floors in the heart of Khammam, Lotus Grand Mall brings together top international fashion brands, state-of-the-art multi-cuisine food court, and gaming zones for complete family entertainment.",
    address: "Mayuri Centre, Bypass Road, Khammam, Telangana 507002",
    mapLink: "https://maps.google.com/?q=Lotus+Mall+Khammam",
    discountOfferTitle: "Extra ₹500 Off On Shopping",
    discountOfferCode: "LOTUSFEST",
    discountOfferTerms: "Valid across fashion & lifestyle outlets on shopping of ₹2999+. Show voucher at store counter.",
    highlights: [
      "50+ Leading Retail Brands",
      "4-Screen Multiplex Cinema",
      "Multicuisine Food Court",
      "Kids VR Gaming Arcade",
      "2-Level Basement Parking"
    ],
    galleryImages: [
      "https://images.unsplash.com/photo-1567401893414-76b7b1e5a7a5?w=600&q=80",
      "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=600&q=80"
    ],
    actionButtons: [
      { label: "Call Helpdesk", icon: "call", actionUrl: "tel:+918742234567", isPrimary: false },
      { label: "Get Directions", icon: "maps", actionUrl: "https://maps.google.com/?q=Mayuri+Centre+Khammam", isPrimary: true },
      { label: "Mall Directory", icon: "url", actionUrl: "https://dutype.com", isPrimary: false }
    ]
  },
  {
    id: "khammam_bike_care_service",
    title: "Khammam Multi-Brand Bike Clinic",
    tagline: "Computerized Tuning, Foam Wash & Genuine Spares with 6-Month Warranty",
    categoryBadge: "AUTO CARE",
    rating: "4.9",
    heroImageUrl: "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?w=800&q=80",
    videoUrl: "",
    aboutDescription: "Specialized service center for Royal Enfield, KTM, Yamaha, Honda, and electric 2-wheelers. Equipped with computerized diagnostic scanners, ultrasonic injector cleaning, and certified master mechanics.",
    address: "Near Mamatha Hospital Junction, Khammam, Telangana 507002",
    mapLink: "https://maps.google.com/?q=Mamatha+Hospital+Khammam",
    discountOfferTitle: "Free Foam Wash + 15% Off Service",
    discountOfferCode: "BIKECLEAN",
    discountOfferTerms: "Valid on complete general bike service booking. Show code during vehicle check-in.",
    highlights: [
      "Computerized Fuel Injection Tuning",
      "Genuine OEM Spare Parts",
      "High-Pressure Foam Wash",
      "Free Pickup & Drop in 5 km",
      "6-Month Service Warranty"
    ],
    galleryImages: [
      "https://images.unsplash.com/photo-1486006920555-c77dce18193b?w=600&q=80"
    ],
    actionButtons: [
      { label: "Call Garage", icon: "call", actionUrl: "tel:+918742245678", isPrimary: true },
      { label: "WhatsApp Service", icon: "whatsapp", actionUrl: "https://wa.me/918742245678?text=Hi%20Bike%20Clinic,%20I%20want%20to%20book%20service", isPrimary: false },
      { label: "Directions", icon: "maps", actionUrl: "https://maps.google.com/?q=Mamatha+Hospital+Khammam", isPrimary: false }
    ]
  }
];

export type BannerSurface = "services" | "worker" | "employer";

export const COLOR_PRESETS = [
  { label: "Midnight Slate", start: "#0F172A", end: "#1E3A8A" },
  { label: "Emerald Pro", start: "#064E3B", end: "#047857" },
  { label: "Ocean Sky", start: "#0F172A", end: "#0284C7" },
  { label: "Sunset Amber", start: "#451A03", end: "#D97706" },
  { label: "Crimson Flame", start: "#881337", end: "#E11D48" },
  { label: "Royal Indigo", start: "#1E1B4B", end: "#4F46E5" },
  { label: "Dark Onyx", start: "#111827", end: "#1F2937" },
  { label: "Forest Green", start: "#14532D", end: "#16A34A" },
];

const PRESET_BANNERS_BY_SURFACE: Record<BannerSurface, Array<{ label: string; banner: Omit<PromoBannerDef, "id"> }>> = {
  services: [
    {
      label: "❄️ AC Mega Service (Khammam Summer)",
      banner: {
        headline: "Khammam AC Deep Clean ⚡ Flat ₹499",
        subheadline: "Foam jet service & gas leak check in 59 mins",
        cta: "Book AC Service",
        bgStartColor: "#0F172A",
        bgEndColor: "#0369A1",
        targetCategoryId: "AC",
        active: true,
      }
    },
    {
      label: "✨ Festive Home Deep Cleaning",
      banner: {
        headline: "Sparkling Home Deep Cleaning · 25% Off",
        subheadline: "Kitchen, bathroom & sofa sanitization by pros",
        cta: "Book Cleaning",
        bgStartColor: "#022C22",
        bgEndColor: "#065F46",
        targetCategoryId: "CLEANING",
        active: true,
      }
    },
    {
      label: "⚡ 60-Second Instant Worker Dispatch",
      banner: {
        headline: "Need Urgent Worker? Dispatched in 60s",
        subheadline: "Helpers, technicians & drivers on demand in Khammam",
        cta: "Hire Instantly",
        bgStartColor: "#451A03",
        bgEndColor: "#9A3412",
        targetCategoryId: "HOME_HELP",
        active: true,
      }
    },
    {
      label: "🏪 Partner With Us (Promote Shop / Brand)",
      banner: {
        headline: "Promote Your Shop or Brand on DutyPe",
        subheadline: "Reach 5,000+ local customers in your district. Tap to partner with us.",
        cta: "Partner With Us",
        bgStartColor: "#0F172A",
        bgEndColor: "#1D4ED8",
        targetCategoryId: "promote_business",
        active: true,
      }
    }
  ],
  worker: [
    {
      label: "⚡ Zero Commission (100% Earnings)",
      banner: {
        headline: "Direct Hiring • 100% Daily Earnings",
        subheadline: "DutyPe takes 0% cut from your work. Keep all of what you earn directly from employers.",
        cta: "Explore Jobs",
        bgStartColor: "#0F172A",
        bgEndColor: "#1E293B",
        targetCategoryId: "jobs",
        active: true
      }
    },
    {
      label: "🛡️ DutyPe Verified Badge",
      banner: {
        headline: "Get Hired 3x Faster Today",
        subheadline: "Complete your profile & verified trade skills to get priority recommendations and direct calls.",
        cta: "Complete Profile",
        bgStartColor: "#064E3B",
        bgEndColor: "#047857",
        targetCategoryId: "profile",
        active: true
      }
    },
    {
      label: "🎁 Referral Bonus ₹100",
      banner: {
        headline: "Earn ₹100 per Worker Referral",
        subheadline: "Invite fellow electricians, plumbers, cooks or drivers. Withdraw instant cash straight to UPI.",
        cta: "Invite & Earn",
        bgStartColor: "#78350F",
        bgEndColor: "#B45309",
        targetCategoryId: "refer_earn",
        active: true
      }
    },
    {
      label: "🏪 Partner With Us (Promote Shop / Brand)",
      banner: {
        headline: "Promote Your Shop or Business",
        subheadline: "Reach thousands of customers and workers across your district on DutyPe.",
        cta: "Partner With Us",
        bgStartColor: "#0F172A",
        bgEndColor: "#1D4ED8",
        targetCategoryId: "promote_business",
        active: true
      }
    }
  ],
  employer: [
    {
      label: "⚡ Hire in Minutes",
      banner: {
        headline: "Post a Job & Get Instant Calls",
        subheadline: "Connect with verified, reliable workers near your shop, factory, or home in minutes.",
        cta: "Post a Job",
        bgStartColor: "#0F172A",
        bgEndColor: "#1E293B",
        targetCategoryId: "post_job",
        active: true
      }
    },
    {
      label: "🚨 Urgent Needs (Workers in 60s)",
      banner: {
        headline: "Need Workers Today? Instant Matches",
        subheadline: "Broadcast an urgent need. Available workers get notified on their phones instantly.",
        cta: "Post Urgent Need",
        bgStartColor: "#881337",
        bgEndColor: "#BE123C",
        targetCategoryId: "urgent_need",
        active: true
      }
    },
    {
      label: "🎉 Referral Cashback ₹100",
      banner: {
        headline: "Earn ₹100 Cashback Every Time",
        subheadline: "Refer fellow business owners, contractors or shopkeepers to DutyPe and earn instant wallet balance.",
        cta: "Refer & Earn",
        bgStartColor: "#312E81",
        bgEndColor: "#4338CA",
        targetCategoryId: "refer_earn",
        active: true
      }
    },
    {
      label: "🏪 Partner With Us (Promote Shop / Brand)",
      banner: {
        headline: "Promote Your Business or Brand",
        subheadline: "Reach 5,000+ local consumers & verified workers daily on DutyPe.",
        cta: "Partner With Us",
        bgStartColor: "#0F172A",
        bgEndColor: "#1D4ED8",
        targetCategoryId: "promote_business",
        active: true
      }
    }
  ]
};

const PRESET_BANNERS = PRESET_BANNERS_BY_SURFACE.services;

export interface BannerOptimizationResult {
  file: File;
  originalSizeKb: number;
  webpBlob: Blob;
  webpSizeKb: number;
  previewUrl: string;
  width: number;
  height: number;
  savingsPct: number;
}

// Convert any banner image to low-KB WebP directly in browser with smooth downscaling
export async function optimizeBannerImage(file: File): Promise<BannerOptimizationResult> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        let w = img.width;
        let h = img.height;
        // Banners look sharp on 1080p/1440p mobile displays at max 800px dimension
        const maxDim = 800;
        if (w > maxDim || h > maxDim) {
          if (w > h) {
            h = Math.round((h * maxDim) / w);
            w = maxDim;
          } else {
            w = Math.round((w * maxDim) / h);
            h = maxDim;
          }
        }
        const canvas = document.createElement("canvas");
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          reject(new Error("Unable to create canvas context"));
          return;
        }
        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = "high";
        ctx.drawImage(img, 0, 0, w, h);

        canvas.toBlob(
          (blob) => {
            if (!blob) {
              reject(new Error("WebP conversion failed"));
              return;
            }
            const originalKb = Math.round((file.size / 1024) * 10) / 10;
            const webpKb = Math.round((blob.size / 1024) * 10) / 10;
            const savings = Math.max(0, Math.round(((file.size - blob.size) / file.size) * 100));

            resolve({
              file,
              originalSizeKb: originalKb,
              webpBlob: blob,
              webpSizeKb: webpKb,
              previewUrl: URL.createObjectURL(blob),
              width: w,
              height: h,
              savingsPct: savings
            });
          },
          "image/webp",
          0.85
        );
      };
      img.onerror = () => reject(new Error("Failed to decode image"));
      img.src = e.target?.result as string;
    };
    reader.onerror = () => reject(new Error("Failed to read file"));
    reader.readAsDataURL(file);
  });
}

export function AdminBannersManager({
  onSaved,
  onError
}: {
  onSaved?: (msg: string) => void;
  onError?: (msg: string) => void;
} = {}) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [mainTab, setMainTab] = useState<"banners" | "showcases">("banners");
  const [surface, setSurface] = useState<BannerSurface>("services");
  const [banners, setBanners] = useState<PromoBannerDef[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [selectedIdx, setSelectedIdx] = useState<number>(0);
  const [rawConfig, setRawConfig] = useState<Record<string, unknown>>({});
  const [optStats, setOptStats] = useState<{ originalKb: number; webpKb: number; savings: number } | null>(null);

  // Dynamic Brand & Shop Showcases State
  const [showcases, setShowcases] = useState<PromoShowcaseDef[]>([]);
  const [loadingShowcases, setLoadingShowcases] = useState(false);
  const [editingShowcase, setEditingShowcase] = useState<PromoShowcaseDef | null>(null);
  const [showcaseSaving, setShowcaseSaving] = useState(false);
  const [showcaseUploading, setShowcaseUploading] = useState(false);
  const [newHighlight, setNewHighlight] = useState("");
  const [newGalleryUrl, setNewGalleryUrl] = useState("");
  const [linkToBannerOnSave, setLinkToBannerOnSave] = useState(false);

  // Load existing Showcases from Firestore collection promo_showcases
  const loadShowcases = async () => {
    if (!services) return;
    try {
      setLoadingShowcases(true);
      const snap = await getDocs(collection(services.db, "promo_showcases"));
      const list = snap.docs.map((d) => ({ id: d.id, ...(d.data() as object) } as PromoShowcaseDef));
      setShowcases(list);
    } catch (e) {
      console.warn("Could not fetch promo_showcases:", e);
    } finally {
      setLoadingShowcases(false);
    }
  };

  useEffect(() => {
    void loadShowcases();
  }, [services]);

  useEffect(() => {
    if (!services) return;
    void (async () => {
      setLoading(true);
      try {
        const targetDoc = surface === "services" ? "services" : "dynamic_features";
        const snap = await getDoc(doc(services.db, "app_config", targetDoc));
        const data = (snap.data() || {}) as Record<string, unknown>;
        setRawConfig(data);
        const listField = surface === "services"
          ? (data.promoBanners ?? data.banners)
          : surface === "worker"
            ? data.promoBanners
            : data.employerPromoBanners;
        const list = listField as PromoBannerDef[] | undefined;
        if (Array.isArray(list) && list.length > 0) {
          setBanners(list);
        } else {
          const presets = PRESET_BANNERS_BY_SURFACE[surface] || PRESET_BANNERS_BY_SURFACE.services;
          setBanners(presets.map((p, i) => ({ id: `banner_${surface}_${i + 1}`, ...p.banner })));
        }
        setSelectedIdx(0);
      } catch (e) {
        onError?.("Could not load promotional banners: " + (e instanceof Error ? e.message : String(e)));
      } finally {
        setLoading(false);
      }
    })();
  }, [services, surface, onError]);

  const activeBanner = banners[selectedIdx] || banners[0];

  function addBlankBanner() {
    const newId = `banner_${Date.now().toString(36)}`;
    const newB: PromoBannerDef = {
      id: newId,
      headline: "Special DutyPe Offer in Khammam",
      subheadline: "Verified doorstep services with 100% satisfaction",
      cta: "Book Service",
      bgStartColor: "#0F172A",
      bgEndColor: "#2563EB",
      targetCategoryId: "CLEANING",
      active: true
    };
    setBanners([...banners, newB]);
    setSelectedIdx(banners.length);
  }

  function addPreset(preset: typeof PRESET_BANNERS_BY_SURFACE.services[0]) {
    const newId = `banner_${Date.now().toString(36)}`;
    const newB: PromoBannerDef = { id: newId, ...preset.banner };
    setBanners([...banners, newB]);
    setSelectedIdx(banners.length);
  }

  function updateBanner(idx: number, patch: Partial<PromoBannerDef>) {
    setBanners(banners.map((b, i) => (i === idx ? { ...b, ...patch } : b)));
  }

  function removeBanner(idx: number) {
    if (!confirm("Are you sure you want to remove this promotional banner?")) return;
    const next = banners.filter((_, i) => i !== idx);
    setBanners(next);
    setSelectedIdx(Math.max(0, idx - 1));
  }

  const [uploading, setUploading] = useState(false);
  const [lottieAnimationData, setLottieAnimationData] = useState<object | null>(null);

  // Fetch Lottie JSON if lottieUrl exists or imageUrl is a .json file
  useEffect(() => {
    const lottieTarget = activeBanner?.lottieUrl || (activeBanner?.imageUrl?.endsWith(".json") ? activeBanner.imageUrl : null);
    if (!lottieTarget) {
      setLottieAnimationData(null);
      return;
    }
    let cancelled = false;
    fetch(lottieTarget)
      .then((r) => r.json())
      .then((data) => {
        if (!cancelled) setLottieAnimationData(data);
      })
      .catch(() => {
        if (!cancelled) setLottieAnimationData(null);
      });
    return () => {
      cancelled = true;
    };
  }, [activeBanner?.lottieUrl, activeBanner?.imageUrl]);

  async function handleFileUpload(file: File) {
    if (!services || !activeBanner) return;
    setUploading(true);
    setOptStats(null);
    try {
      const isLottie = file.name.endsWith(".json") || file.type === "application/json";
      const bannerId = (activeBanner.id || `banner_${Date.now()}`).toLowerCase().replace(/[^a-z0-9_-]/g, "_");

      let uploadBlob: Blob = file;
      let uploadFileName = file.name;
      let contentType = file.type || "application/octet-stream";

      if (isLottie) {
        const text = await file.text();
        try {
          const json = JSON.parse(text);
          setLottieAnimationData(json);
        } catch {
          // ignore parsing error
        }
        contentType = "application/json";
        uploadFileName = `${bannerId}_${Date.now()}.json`;
      } else {
        const opt = await optimizeBannerImage(file);
        uploadBlob = opt.webpBlob;
        uploadFileName = `${bannerId}_${Date.now()}.webp`;
        contentType = "image/webp";
        setOptStats({
          originalKb: opt.originalSizeKb,
          webpKb: opt.webpSizeKb,
          savings: opt.savingsPct
        });
      }

      let downloadUrl = "";

      try {
        const formData = new FormData();
        formData.append("image", uploadBlob, uploadFileName);
        formData.append("id", bannerId);
        formData.append("type", "banner");

        const headers: Record<string, string> = {};
        if (services.auth.currentUser) {
          const idToken = await services.auth.currentUser.getIdToken();
          headers["Authorization"] = `Bearer ${idToken}`;
        }

        const apiRes = await fetch("/api/admin/service-catalog/upload-image", {
          method: "POST",
          headers,
          body: formData
        });

        if (apiRes.ok) {
          const resJson = await apiRes.json();
          downloadUrl = resJson.url;
        } else {
          const errData = await apiRes.json().catch(() => null);
          console.warn("Server-side banner upload returned non-200:", errData);
        }
      } catch (apiErr) {
        console.warn("Server-side banner upload failed, falling back to client upload:", apiErr);
      }

      if (!downloadUrl) {
        const storageRef = ref(services.storage, `banners/${uploadFileName}`);
        await uploadBytes(storageRef, uploadBlob, {
          contentType,
          cacheControl: "public, max-age=31536000, immutable"
        });
        downloadUrl = await getDownloadURL(storageRef);
      }

      if (isLottie) {
        updateBanner(selectedIdx, {
          lottieUrl: downloadUrl,
          imageUrl: downloadUrl,
          fileSizeBytes: uploadBlob.size
        });
      } else {
        setLottieAnimationData(null);
        updateBanner(selectedIdx, {
          imageUrl: downloadUrl,
          lottieUrl: "",
          fileSizeBytes: uploadBlob.size
        });
      }
    } catch (e) {
      onError?.("Failed to upload file: " + (e instanceof Error ? e.message : String(e)));
    } finally {
      setUploading(false);
    }
  }

  async function saveBanners() {
    if (!services) return;
    setSaving(true);
    try {
      const cleanBanners = banners.map((b) => ({
        id: b.id.trim(),
        headline: b.headline.trim(),
        subheadline: b.subheadline.trim(),
        cta: b.cta.trim() || "Book now",
        bgStartColor: b.bgStartColor.trim() || "#0F172A",
        bgEndColor: b.bgEndColor.trim() || "#1E3A8A",
        targetCategoryId: b.targetCategoryId?.trim() || "",
        targetServiceId: b.targetServiceId?.trim() || "",
        targetShowcaseId: b.targetShowcaseId?.trim() || "",
        imageUrl: b.imageUrl?.trim() || "",
        lottieUrl: b.lottieUrl?.trim() || "",
        fileSizeBytes: typeof b.fileSizeBytes === "number" ? b.fileSizeBytes : 0,
        imageScale: typeof b.imageScale === "number" ? b.imageScale : 1.0,
        active: b.active !== false
      }));

      const targetDoc = surface === "services" ? "services" : "dynamic_features";
      const fieldName = surface === "services"
        ? "promoBanners"
        : surface === "worker"
          ? "promoBanners"
          : "employerPromoBanners";

      const updatePayload: Record<string, unknown> = {
        ...rawConfig,
        [fieldName]: cleanBanners,
        updatedAt: Date.now()
      };
      if (surface === "services") {
        updatePayload.banners = cleanBanners;
      }

      await setDoc(doc(services.db, "app_config", targetDoc), updatePayload, { merge: true });
      onSaved?.(
        cleanBanners.length > 0
          ? `Promotional banners for ${surface.toUpperCase()} published successfully! Android app will update immediately.`
          : `All promotional banners for ${surface.toUpperCase()} removed.`
      );
    } catch (e) {
      onError?.("Failed to save banners: " + (e instanceof Error ? e.message : String(e)));
    } finally {
      setSaving(false);
    }
  }

  // Showcase Handlers
  function openNewShowcase(preset?: PromoShowcaseDef, linkToActive = false) {
    const base = preset ? JSON.parse(JSON.stringify(preset)) : JSON.parse(JSON.stringify(EMPTY_SHOWCASE));
    if (!preset) {
      base.id = `showcase_${Date.now().toString(36)}`;
    }
    setEditingShowcase(base);
    setLinkToBannerOnSave(linkToActive);
  }

  function openEditShowcase(sc: PromoShowcaseDef, linkToActive = false) {
    setEditingShowcase(JSON.parse(JSON.stringify(sc)));
    setLinkToBannerOnSave(linkToActive);
  }

  async function handleShowcaseSave() {
    if (!services || !editingShowcase) return;
    const rawId = editingShowcase.id.trim().toLowerCase().replace(/[^a-z0-9_-]/g, "_");
    if (!rawId) {
      onError?.("Showcase ID cannot be empty. Please enter a unique ID (e.g. paradise_biryani).");
      return;
    }
    if (!editingShowcase.title.trim()) {
      onError?.("Business / Brand title is required.");
      return;
    }

    setShowcaseSaving(true);
    try {
      const payload: PromoShowcaseDef = {
        id: rawId,
        title: editingShowcase.title.trim(),
        tagline: editingShowcase.tagline.trim(),
        categoryBadge: editingShowcase.categoryBadge.trim() || "BRAND",
        rating: editingShowcase.rating.trim() || "4.8",
        heroImageUrl: editingShowcase.heroImageUrl.trim(),
        videoUrl: editingShowcase.videoUrl.trim(),
        aboutDescription: editingShowcase.aboutDescription.trim(),
        address: editingShowcase.address.trim(),
        mapLink: editingShowcase.mapLink.trim(),
        discountOfferTitle: editingShowcase.discountOfferTitle.trim(),
        discountOfferCode: editingShowcase.discountOfferCode.trim(),
        discountOfferTerms: editingShowcase.discountOfferTerms.trim(),
        highlights: (editingShowcase.highlights || []).map((h) => h.trim()).filter(Boolean),
        galleryImages: (editingShowcase.galleryImages || []).map((g) => g.trim()).filter(Boolean),
        actionButtons: (editingShowcase.actionButtons || []).map((b) => ({
          label: b.label.trim() || "Contact",
          icon: b.icon.trim() || "call",
          actionUrl: b.actionUrl.trim(),
          isPrimary: !!b.isPrimary
        })),
        district: editingShowcase.district?.trim() || "Khammam",
        scope: editingShowcase.scope || "DISTRICT",
        active: editingShowcase.active !== false,
        updatedAt: Date.now()
      };

      await setDoc(doc(services.db, "promo_showcases", rawId), payload, { merge: true });

      setShowcases((prev) => {
        const idx = prev.findIndex((s) => s.id === rawId);
        if (idx >= 0) {
          const next = [...prev];
          next[idx] = payload;
          return next;
        }
        return [payload, ...prev];
      });

      if (linkToBannerOnSave && activeBanner) {
        updateBanner(selectedIdx, { targetShowcaseId: rawId });
      }

      onSaved?.(`Brand Showcase "${payload.title}" published! Mobile app loads this immediately.`);
      setEditingShowcase(null);
    } catch (e) {
      onError?.("Failed to save showcase: " + (e instanceof Error ? e.message : String(e)));
    } finally {
      setShowcaseSaving(false);
    }
  }

  async function handleDeleteShowcase(id: string) {
    if (!services) return;
    if (!confirm(`Permanently delete Showcase "${id}"? Banners linking to this ID will no longer open this showcase.`)) return;
    try {
      await deleteDoc(doc(services.db, "promo_showcases", id));
      setShowcases((prev) => prev.filter((s) => s.id !== id));
      setBanners((prev) => prev.map((b) => (b.targetShowcaseId === id ? { ...b, targetShowcaseId: "" } : b)));
      onSaved?.(`Showcase "${id}" has been deleted.`);
    } catch (e) {
      onError?.("Failed to delete showcase: " + (e instanceof Error ? e.message : String(e)));
    }
  }

  async function handleShowcaseImageUpload(file: File, target: "hero" | "gallery") {
    if (!services || !editingShowcase) return;
    setShowcaseUploading(true);
    try {
      const opt = await optimizeBannerImage(file);
      const scId = (editingShowcase.id || "showcase").toLowerCase().replace(/[^a-z0-9_-]/g, "_");
      const uploadFileName = `${scId}_${target}_${Date.now()}.webp`;

      let downloadUrl = "";
      try {
        const formData = new FormData();
        formData.append("image", opt.webpBlob, uploadFileName);
        formData.append("id", scId);
        formData.append("type", "banner");

        const headers: Record<string, string> = {};
        if (services.auth.currentUser) {
          const idToken = await services.auth.currentUser.getIdToken();
          headers["Authorization"] = `Bearer ${idToken}`;
        }

        const apiRes = await fetch("/api/admin/service-catalog/upload-image", {
          method: "POST",
          headers,
          body: formData
        });

        if (apiRes.ok) {
          const resJson = await apiRes.json();
          downloadUrl = resJson.url;
        }
      } catch (err) {
        console.warn("Showcase server upload failed, falling back to client upload:", err);
      }

      if (!downloadUrl) {
        const storageRef = ref(services.storage, `showcases/${uploadFileName}`);
        await uploadBytes(storageRef, opt.webpBlob, {
          contentType: "image/webp",
          cacheControl: "public, max-age=31536000, immutable"
        });
        downloadUrl = await getDownloadURL(storageRef);
      }

      if (target === "hero") {
        setEditingShowcase({ ...editingShowcase, heroImageUrl: downloadUrl });
      } else {
        setEditingShowcase({
          ...editingShowcase,
          galleryImages: [...(editingShowcase.galleryImages || []), downloadUrl]
        });
      }
    } catch (e) {
      onError?.("Failed to upload image: " + (e instanceof Error ? e.message : String(e)));
    } finally {
      setShowcaseUploading(false);
    }
  }

  if (loading) {
    return (
      <div style={{ padding: 32, textAlign: "center", color: "#64748b" }}>
        Loading promotional banner configuration...
      </div>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Top Header & Overview */}
      <section className="admin-section" style={{ background: "#f8fafc", border: "1px solid #e2e8f0" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: 16 }}>
          <div>
            <h2 className="admin-section-title" style={{ display: "flex", alignItems: "center", gap: 8, margin: 0 }}>
              <Sparkles size={20} color="#2563eb" />
              Promotional Banners &amp; Showcase Hub
            </h2>
            <p style={{ margin: "4px 0 0 0", color: "#64748b", fontSize: 14, maxWidth: 720 }}>
              Configure top-of-screen promotional hero banners across DutyPe Home Services, Worker Home Screen, and Employer Home Screen,
              or create full-screen Brand &amp; Shop Showcase microsite pages for local Khammam businesses!
            </p>
          </div>
          {mainTab === "banners" ? (
            <button
              onClick={saveBanners}
              disabled={saving}
              className="btn btn-approve"
              style={{ display: "flex", alignItems: "center", gap: 6, padding: "8px 20px", fontWeight: 700, fontSize: 15 }}
            >
              {saving ? <RefreshCw className="animate-spin" size={16} /> : <Check size={16} />}
              {saving ? "Saving Banners..." : `Publish ${surface.toUpperCase()} Banners`}
            </button>
          ) : (
            <button
              type="button"
              onClick={() => openNewShowcase()}
              className="btn btn-approve"
              style={{ display: "flex", alignItems: "center", gap: 6, padding: "8px 20px", fontWeight: 700, fontSize: 15 }}
            >
              <Plus size={16} />
              + Create New Showcase
            </button>
          )}
        </div>

        {/* Top Mode Switcher */}
        <div style={{ marginTop: 16, display: "flex", gap: 10, borderBottom: "1px solid #cbd5e1", paddingBottom: 12 }}>
          <button
            type="button"
            onClick={() => setMainTab("banners")}
            style={{
              padding: "9px 18px",
              borderRadius: 8,
              border: mainTab === "banners" ? "2px solid #2563eb" : "1px solid #cbd5e1",
              background: mainTab === "banners" ? "#eff6ff" : "#fff",
              color: mainTab === "banners" ? "#1d4ed8" : "#475569",
              fontWeight: mainTab === "banners" ? 800 : 600,
              fontSize: 13.5,
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              gap: 8
            }}
          >
            <Sparkles size={16} />
            Promotional Carousel Banners ({banners.length})
          </button>
          <button
            type="button"
            onClick={() => setMainTab("showcases")}
            style={{
              padding: "9px 18px",
              borderRadius: 8,
              border: mainTab === "showcases" ? "2px solid #16a34a" : "1px solid #cbd5e1",
              background: mainTab === "showcases" ? "#f0fdf4" : "#fff",
              color: mainTab === "showcases" ? "#15803d" : "#475569",
              fontWeight: mainTab === "showcases" ? 800 : 600,
              fontSize: 13.5,
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              gap: 8
            }}
          >
            <Store size={16} />
            🏪 Brand &amp; Shop Showcase Microsites ({showcases.length})
          </button>
        </div>

        {mainTab === "banners" && (
          <>
            {/* Target Surface Selector */}
            <div style={{ marginTop: 14, display: "flex", gap: 10, borderBottom: "1px solid #e2e8f0", paddingBottom: 12 }}>
              {([
                ["services", "🛠️ DutyPe Home Services"],
                ["worker", "👷 Worker Home Screen"],
                ["employer", "🏢 Employer Home Screen"]
              ] as const).map(([s, label]) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => setSurface(s)}
                  className="btn"
                  style={{
                    padding: "7px 14px",
                    fontWeight: surface === s ? 700 : 500,
                    background: surface === s ? "#1e293b" : "#ffffff",
                    color: surface === s ? "#ffffff" : "#334155",
                    border: surface === s ? "1px solid #0f172a" : "1px solid #cbd5e1",
                    borderRadius: 8,
                    fontSize: 12.5,
                    cursor: "pointer"
                  }}
                >
                  {label}
                </button>
              ))}
            </div>

            {/* Quick Presets */}
            <div style={{ marginTop: 12, display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
              <span style={{ fontSize: 13, fontWeight: 600, color: "#475569" }}>One-Click Templates:</span>
              {(PRESET_BANNERS_BY_SURFACE[surface] || PRESET_BANNERS_BY_SURFACE.services).map((p, i) => (
                <button
                  key={i}
                  type="button"
                  onClick={() => addPreset(p)}
                  className="btn"
                  style={{ fontSize: 12, padding: "4px 10px", background: "#fff", border: "1px solid #cbd5e1" }}
                >
                  + {p.label}
                </button>
              ))}
              <button
                type="button"
                onClick={addBlankBanner}
                className="btn"
                style={{ fontSize: 12, padding: "4px 10px", background: "#eff6ff", borderColor: "#93c5fd", color: "#1d4ed8", fontWeight: 600 }}
              >
                <Plus size={14} style={{ display: "inline", verticalAlign: "middle", marginRight: 2 }} />
                Add Custom Slide
              </button>
            </div>
          </>
        )}
      </section>

      {/* Main Grid: Left is live preview & slide selector, Right is active slide editor */}
      {mainTab === "banners" && (
        <div style={{ display: "grid", gridTemplateColumns: "minmax(340px, 420px) 1fr", gap: 24, alignItems: "start" }}>
        {/* Left: Mobile App Simulator & Slide Selector */}
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: "#1e293b", textTransform: "uppercase", letterSpacing: "0.05em", display: "flex", alignItems: "center", gap: 6 }}>
            <Smartphone size={16} />
            Live Mobile App Header Preview
          </div>

          {/* Interactive Mobile Header Mockup */}
          {activeBanner && (
            <div
              style={{
                borderRadius: 24,
                overflow: "hidden",
                border: "4px solid #0f172a",
                boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)",
                background: "#f8fafc"
              }}
            >
              {/* Fake Mobile Status Bar */}
              <div
                style={{
                  background: activeBanner.bgStartColor,
                  color: "#fff",
                  padding: "10px 16px 6px",
                  display: "flex",
                  justifyContent: "space-between",
                  fontSize: 12,
                  fontWeight: 600,
                  transition: "background 0.4s ease"
                }}
              >
                <span>9:41</span>
                <span>📶 5G  🔋 98%</span>
              </div>

              {/* Dynamic Header & Location Bar */}
              <div
                style={{
                  background: activeBanner.bgStartColor,
                  color: "#fff",
                  padding: "6px 16px 14px",
                  transition: "background 0.4s ease"
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <div>
                    <div style={{ fontSize: 14, fontWeight: 800 }}>⚡ In 59 minutes</div>
                    <div style={{ fontSize: 11, opacity: 0.85 }}>📍 KHAMMAM CITY (WYRA ROAD) ▾</div>
                  </div>
                  <div style={{ display: "flex", gap: 6 }}>
                    <div style={{ width: 28, height: 28, borderRadius: 8, background: "rgba(255,255,255,0.2)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12 }}>🧾</div>
                    <div style={{ width: 28, height: 28, borderRadius: 8, background: "rgba(255,255,255,0.2)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12 }}>👤</div>
                  </div>
                </div>

                {/* Search Bar */}
                <div style={{ marginTop: 10, background: "#fff", borderRadius: 10, padding: "8px 12px", display: "flex", alignItems: "center", gap: 8, color: "#64748b", fontSize: 12 }}>
                  <span>🔍</span>
                  <span>Search &apos;AC repair&apos;, &apos;Electrician&apos;...</span>
                </div>
              </div>

              {/* Promo Banner Slide Body with Gradient */}
              <div
                style={{
                  background: `linear-gradient(135deg, ${activeBanner.bgStartColor} 0%, ${activeBanner.bgEndColor} 100%)`,
                  padding: "20px 18px",
                  color: "#ffffff",
                  position: "relative",
                  minHeight: 140,
                  display: "flex",
                  flexDirection: "column",
                  justifyContent: "space-between",
                  transition: "all 0.4s ease"
                }}
              >
                <div>
                  <div style={{ display: "inline-block", background: "rgba(255,255,255,0.18)", padding: "2px 8px", borderRadius: 20, fontSize: 10.5, fontWeight: 700, marginBottom: 8 }}>
                    FEATURED PROMOTION
                  </div>
                  <div style={{ fontSize: 17, fontWeight: 800, lineHeight: 1.25, maxWidth: "80%", textShadow: "0 1px 2px rgba(0,0,0,0.2)" }}>
                    {activeBanner.headline || "Headline Here"}
                  </div>
                </div>

                <div style={{ marginTop: 14, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <button
                    type="button"
                    style={{
                      background: "#ffffff",
                      color: "#0f172a",
                      border: "none",
                      borderRadius: 8,
                      padding: "6px 14px",
                      fontSize: 12,
                      fontWeight: 700,
                      cursor: "default"
                    }}
                  >
                    {activeBanner.cta || "Book Now"} →
                  </button>

                  {/* Pager Dots simulation */}
                  <div style={{ display: "flex", gap: 4 }}>
                    {banners.map((_, i) => (
                      <span
                        key={i}
                        style={{
                          width: i === selectedIdx ? 16 : 6,
                          height: 6,
                          borderRadius: 3,
                          background: i === selectedIdx ? "#fff" : "rgba(255,255,255,0.4)",
                          transition: "all 0.3s ease"
                        }}
                      />
                    ))}
                  </div>
                </div>

                {lottieAnimationData ? (
                  <div
                    style={{
                      position: "absolute",
                      right: 8,
                      top: 14,
                      width: Math.round(76 * (activeBanner.imageScale || 1.0)),
                      height: Math.round(76 * (activeBanner.imageScale || 1.0)),
                      filter: "drop-shadow(0 4px 6px rgba(0,0,0,0.25))",
                      pointerEvents: "none"
                    }}
                  >
                    <Lottie
                      animationData={lottieAnimationData}
                      loop
                      autoplay
                      style={{ width: "100%", height: "100%" }}
                    />
                  </div>
                ) : activeBanner.imageUrl ? (
                  <img
                    src={activeBanner.imageUrl}
                    alt="Promo"
                    style={{
                      position: "absolute",
                      right: 12,
                      top: 18,
                      width: Math.round(68 * (activeBanner.imageScale || 1.0)),
                      height: Math.round(68 * (activeBanner.imageScale || 1.0)),
                      objectFit: "contain",
                      filter: "drop-shadow(0 4px 6px rgba(0,0,0,0.25))",
                      transition: "all 0.2s ease"
                    }}
                  />
                ) : null}
              </div>

              {/* Fake Catalog Preview Below Banner */}
              <div style={{ padding: 14, background: "#f8fafc" }}>
                <div style={{ fontSize: 11, fontWeight: 700, color: "#64748b", textTransform: "uppercase" }}>Quick Categories</div>
                <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
                  {["⚡ AC", "🧹 Clean", "🔌 Electric", "🔧 Plumber"].map((cat, i) => (
                    <div key={i} style={{ flex: 1, background: "#fff", border: "1px solid #e2e8f0", borderRadius: 8, padding: "8px 4px", textAlign: "center", fontSize: 11, fontWeight: 600 }}>
                      {cat}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Slide Switcher List */}
          <div style={{ marginTop: 8 }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: "#64748b", marginBottom: 6 }}>All Carousel Slides ({banners.length}):</div>
            <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
              {banners.map((b, i) => (
                <div
                  key={b.id || i}
                  onClick={() => setSelectedIdx(i)}
                  style={{
                    padding: "8px 12px",
                    borderRadius: 8,
                    border: i === selectedIdx ? "2px solid #2563eb" : "1px solid #e2e8f0",
                    background: i === selectedIdx ? "#eff6ff" : "#fff",
                    cursor: "pointer",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center"
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: 8, overflow: "hidden" }}>
                    <span
                      style={{
                        width: 14,
                        height: 14,
                        borderRadius: "50%",
                        background: `linear-gradient(135deg, ${b.bgStartColor}, ${b.bgEndColor})`,
                        flexShrink: 0
                      }}
                    />
                    <span style={{ fontSize: 13, fontWeight: i === selectedIdx ? 700 : 500, color: "#0f172A", whiteSpace: "nowrap", textOverflow: "ellipsis", overflow: "hidden" }}>
                      Slide {i + 1}: {b.headline}
                    </span>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", gap: 6, flexShrink: 0 }}>
                    {!b.active && <span style={{ fontSize: 10, background: "#fee2e2", color: "#dc2626", padding: "1px 6px", borderRadius: 4, fontWeight: 700 }}>OFF</span>}
                    <button
                      type="button"
                      onClick={(e) => { e.stopPropagation(); removeBanner(i); }}
                      style={{ background: "none", border: "none", color: "#ef4444", cursor: "pointer", padding: "2px 4px", borderRadius: 4 }}
                      title="Delete Slide"
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right: Slide Editor Form */}
        {activeBanner && (
          <div className="admin-section" style={{ background: "#ffffff", border: "1px solid #e2e8f0", padding: 20 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
              <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: "#0f172a" }}>
                Edit Slide {selectedIdx + 1}: &ldquo;{activeBanner.headline}&rdquo;
              </h3>
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <label style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 13, fontWeight: 600, cursor: "pointer" }}>
                  <input
                    type="checkbox"
                    checked={activeBanner.active !== false}
                    onChange={(e) => updateBanner(selectedIdx, { active: e.target.checked })}
                  />
                  Active in App Carousel
                </label>
                <button
                  type="button"
                  onClick={() => removeBanner(selectedIdx)}
                  style={{
                    background: "#fee2e2",
                    color: "#dc2626",
                    border: "1px solid #fca5a5",
                    borderRadius: 6,
                    padding: "4px 10px",
                    fontSize: 12,
                    fontWeight: 600,
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                    gap: 4
                  }}
                  title="Delete this banner slide"
                >
                  <Trash2 size={13} />
                  Delete Slide
                </button>
              </div>
            </div>

            {/* SECTION 1: CORE ESSENTIALS (WHAT REALLY MATTERS) */}
            <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: 10, padding: 16, marginBottom: 16 }}>
              <div style={{ fontSize: 13, fontWeight: 800, color: "#1e293b", marginBottom: 12, display: "flex", alignItems: "center", gap: 6 }}>
                <span>⚡</span>
                <span>Core Banner Essentials (Required)</span>
              </div>

              <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                {/* Headline with word & char counter */}
                <div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
                    <label style={{ fontSize: 12, fontWeight: 700, color: "#334155" }}>
                      Headline (Main bold offer) *
                    </label>
                    <span style={{
                      fontSize: 11,
                      fontWeight: 700,
                      color: activeBanner.headline.length > 36 ? "#dc2626" : "#2563eb"
                    }}>
                      {activeBanner.headline.length}/36 chars (Recommended: 4–6 words)
                    </span>
                  </div>
                  <input
                    type="text"
                    maxLength={40}
                    value={activeBanner.headline}
                    onChange={(e) => updateBanner(selectedIdx, { headline: e.target.value })}
                    placeholder="e.g. Khammam AC Service ⚡ Flat ₹499"
                    style={{
                      width: "100%",
                      padding: "8px 12px",
                      borderRadius: 6,
                      border: activeBanner.headline.length > 36 ? "1px solid #ef4444" : "1px solid #cbd5e1",
                      fontSize: 14,
                      fontWeight: 600
                    }}
                  />
                  <div style={{ fontSize: 11, color: "#64748b", marginTop: 4 }}>
                    💡 Keep it punchy! Appears in large bold text on the mobile banner card.
                  </div>
                </div>

                {/* CTA Button Text with character counter */}
                <div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
                    <label style={{ fontSize: 12, fontWeight: 700, color: "#334155" }}>
                      Action Button Text (Pill with animated running border) *
                    </label>
                    <span style={{
                      fontSize: 11,
                      fontWeight: 700,
                      color: activeBanner.cta.length > 14 ? "#dc2626" : "#2563eb"
                    }}>
                      {activeBanner.cta.length}/14 chars (Max 2 words)
                    </span>
                  </div>
                  <input
                    type="text"
                    maxLength={16}
                    value={activeBanner.cta}
                    onChange={(e) => updateBanner(selectedIdx, { cta: e.target.value })}
                    placeholder="e.g. Book Now"
                    style={{
                      width: "100%",
                      padding: "8px 12px",
                      borderRadius: 6,
                      border: activeBanner.cta.length > 14 ? "1px solid #ef4444" : "1px solid #cbd5e1",
                      fontSize: 14
                    }}
                  />
                  <div style={{ fontSize: 11, color: "#64748b", marginTop: 4 }}>
                    💡 Renders inside the high-converting animated glowing border pill button.
                  </div>
                </div>

                {/* Graphic / Animation Uploader (Lottie & Images) */}
                <div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                    <label style={{ fontSize: 12, fontWeight: 700, color: "#334155", display: "flex", alignItems: "center", gap: 6 }}>
                      <span>Banner Graphic or Lottie Animation</span>
                      {activeBanner.lottieUrl ? (
                        <span style={{ fontSize: 10, padding: "2px 7px", borderRadius: 12, background: "#f3e8ff", color: "#7e22ce", fontWeight: 800 }}>
                          ✨ LOTTIE ANIMATION
                        </span>
                      ) : activeBanner.imageUrl ? (
                        <span style={{ fontSize: 10, padding: "2px 7px", borderRadius: 12, background: "#eff6ff", color: "#1d4ed8", fontWeight: 800 }}>
                          🖼️ STATIC IMAGE
                        </span>
                      ) : null}
                    </label>
                    {activeBanner.fileSizeBytes ? (
                      <span
                        style={{
                          fontSize: 11,
                          fontWeight: 700,
                          padding: "2px 8px",
                          borderRadius: 12,
                          background:
                            activeBanner.fileSizeBytes < 60 * 1024
                              ? "#ecfdf5"
                              : activeBanner.fileSizeBytes < 150 * 1024
                              ? "#fffbeb"
                              : "#fef2f2",
                          color:
                            activeBanner.fileSizeBytes < 60 * 1024
                              ? "#047857"
                              : activeBanner.fileSizeBytes < 150 * 1024
                              ? "#b45309"
                              : "#b91c1c"
                        }}
                      >
                        Size: {(activeBanner.fileSizeBytes / 1024).toFixed(1)} KB{" "}
                        {activeBanner.fileSizeBytes < 60 * 1024
                          ? "✓ Optimal"
                          : activeBanner.fileSizeBytes < 150 * 1024
                          ? "⚠ Moderate"
                          : "⚠ High"}
                      </span>
                    ) : null}
                  </div>

                  {/* Direct File Upload Drop Zone */}
                  <div
                    style={{
                      border: "2px dashed #cbd5e1",
                      borderRadius: 8,
                      padding: "14px 16px",
                      background: "#ffffff",
                      textAlign: "center",
                      marginBottom: 10
                    }}
                  >
                    <input
                      type="file"
                      id="banner-file-upload"
                      accept=".json,.png,.webp,.jpg,.jpeg,.gif,.svg"
                      disabled={uploading}
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) handleFileUpload(file);
                        e.target.value = "";
                      }}
                      style={{ display: "none" }}
                    />
                    <label
                      htmlFor="banner-file-upload"
                      style={{
                        cursor: uploading ? "wait" : "pointer",
                        display: "flex",
                        flexDirection: "column",
                        alignItems: "center",
                        gap: 6
                      }}
                    >
                      <div
                        style={{
                          width: 40,
                          height: 40,
                          borderRadius: "50%",
                          background: "#eff6ff",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          color: "#2563eb"
                        }}
                      >
                        {uploading ? (
                          <RefreshCw className="animate-spin" size={20} />
                        ) : (
                          <Upload size={20} />
                        )}
                      </div>
                      <div style={{ fontSize: 13, fontWeight: 700, color: "#1e293b" }}>
                        {uploading ? "Compressing to WebP & Uploading..." : "Click to Upload Image or Lottie File"}
                      </div>
                      <div style={{ fontSize: 11, color: "#64748b" }}>
                        PNG / JPG images are <b>auto-converted to low-KB WebP</b> • Lottie <b>.json</b> animations supported
                      </div>
                    </label>
                  </div>

                  {/* Real-time WebP Compression Savings Badge */}
                  {optStats && (
                    <div
                      style={{
                        marginBottom: 10,
                        padding: "8px 12px",
                        background: "#f0fdf4",
                        border: "1px solid #bbf7d0",
                        borderRadius: 6,
                        display: "flex",
                        alignItems: "center",
                        gap: 8,
                        fontSize: 12,
                        color: "#166534"
                      }}
                    >
                      <Check size={16} color="#16a34a" />
                      <span>
                        <b>WebP Auto-Compression Applied:</b> {optStats.originalKb} KB &rarr;{" "}
                        <strong style={{ color: "#15803d" }}>{optStats.webpKb} KB</strong> ({optStats.savings}% size reduction, ultra-fast mobile loading!)
                      </span>
                    </div>
                  )}

                  {/* Direct URL Input Fallback */}
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <input
                      type="text"
                      value={activeBanner.lottieUrl || activeBanner.imageUrl || ""}
                      onChange={(e) => {
                        const val = e.target.value.trim();
                        const isJson = val.endsWith(".json");
                        updateBanner(selectedIdx, {
                          imageUrl: val,
                          lottieUrl: isJson ? val : "",
                          fileSizeBytes: undefined
                        });
                      }}
                      placeholder="Or paste asset URL (https://... or .json for Lottie)"
                      style={{
                        flex: 1,
                        padding: "7px 10px",
                        borderRadius: 6,
                        border: "1px solid #cbd5e1",
                        fontSize: 12
                      }}
                    />
                    {(activeBanner.imageUrl || activeBanner.lottieUrl) && (
                      <button
                        type="button"
                        onClick={() =>
                          updateBanner(selectedIdx, {
                            imageUrl: "",
                            lottieUrl: "",
                            fileSizeBytes: undefined
                          })
                        }
                        className="btn"
                        style={{
                          fontSize: 11,
                          padding: "6px 10px",
                          background: "#fef2f2",
                          color: "#dc2626",
                          borderColor: "#fecaca"
                        }}
                      >
                        Clear
                      </button>
                    )}
                  </div>

                  {/* Image/Lottie Sizing Feature */}
                  <div style={{ marginTop: 12, padding: 12, background: "#ffffff", borderRadius: 8, border: "1px solid #e2e8f0" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8, flexWrap: "wrap", gap: 8 }}>
                      <label style={{ fontSize: 12, fontWeight: 700, color: "#1e293b", display: "flex", alignItems: "center", gap: 6 }}>
                        <span>🔍 Graphic Display Scale:</span>
                        <span style={{ color: "#2563eb", fontWeight: 800 }}>{Math.round((activeBanner.imageScale ?? 1.0) * 100)}%</span>
                      </label>
                      <div style={{ display: "flex", gap: 6 }}>
                        <button
                          type="button"
                          onClick={() => updateBanner(selectedIdx, { imageScale: 0.8 })}
                          className="btn"
                          style={{
                            fontSize: 11,
                            padding: "3px 8px",
                            background: activeBanner.imageScale === 0.8 ? "#eff6ff" : "#fff",
                            borderColor: activeBanner.imageScale === 0.8 ? "#2563eb" : "#cbd5e1",
                            color: activeBanner.imageScale === 0.8 ? "#1d4ed8" : "#475569",
                            fontWeight: 600
                          }}
                        >
                          Compact (80%)
                        </button>
                        <button
                          type="button"
                          onClick={() => updateBanner(selectedIdx, { imageScale: 1.0 })}
                          className="btn"
                          style={{
                            fontSize: 11,
                            padding: "3px 8px",
                            background: (!activeBanner.imageScale || activeBanner.imageScale === 1.0) ? "#eff6ff" : "#fff",
                            borderColor: (!activeBanner.imageScale || activeBanner.imageScale === 1.0) ? "#2563eb" : "#cbd5e1",
                            color: (!activeBanner.imageScale || activeBanner.imageScale === 1.0) ? "#1d4ed8" : "#475569",
                            fontWeight: 600
                          }}
                        >
                          Standard (100%)
                        </button>
                        <button
                          type="button"
                          onClick={() => updateBanner(selectedIdx, { imageScale: 1.2 })}
                          className="btn"
                          style={{
                            fontSize: 11,
                            padding: "3px 8px",
                            background: activeBanner.imageScale === 1.2 ? "#eff6ff" : "#fff",
                            borderColor: activeBanner.imageScale === 1.2 ? "#2563eb" : "#cbd5e1",
                            color: activeBanner.imageScale === 1.2 ? "#1d4ed8" : "#475569",
                            fontWeight: 600
                          }}
                        >
                          Large (120%)
                        </button>
                      </div>
                    </div>
                    <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                      <span style={{ fontSize: 11, color: "#64748b" }}>70% (Smaller)</span>
                      <input
                        type="range"
                        min="0.7"
                        max="1.3"
                        step="0.05"
                        value={activeBanner.imageScale ?? 1.0}
                        onChange={(e) => updateBanner(selectedIdx, { imageScale: parseFloat(e.target.value) })}
                        style={{ flex: 1, accentColor: "#2563eb", cursor: "pointer" }}
                      />
                      <span style={{ fontSize: 11, color: "#64748b" }}>130% (Larger)</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* SECTION 2: OPTIONAL SETTINGS & ROUTING */}
            <div style={{ background: "#ffffff", border: "1px solid #e2e8f0", borderRadius: 10, padding: 16 }}>
              <div style={{ fontSize: 13, fontWeight: 800, color: "#475569", marginBottom: 12, display: "flex", alignItems: "center", gap: 6 }}>
                <span>🎨</span>
                <span>Optional Customizations &amp; Routing</span>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14 }}>

                {/* Target Navigation */}
                <div>
                  <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                    Target Category (Optional)
                  </label>
                  <select
                    value={activeBanner.targetCategoryId || ""}
                    onChange={(e) => updateBanner(selectedIdx, { targetCategoryId: e.target.value })}
                    style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  >
                    <option value="">None (Generic)</option>
                    <option value="AC">AC Service &amp; Repair</option>
                    <option value="CLEANING">Home Cleaning</option>
                    <option value="ELECTRICIAN">Electrician</option>
                    <option value="PLUMBER">Plumber</option>
                    <option value="APPLIANCE">Appliance &amp; RO Repair</option>
                    <option value="CARPENTER">Carpenter</option>
                    <option value="PAINTER">Painting</option>
                    <option value="HOME_HELP">Home Help &amp; Shifting</option>
                    <option value="VEHICLE">Car &amp; Bike Wash</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                    Target Specific Service ID (Optional)
                  </label>
                  <input
                    type="text"
                    value={activeBanner.targetServiceId || ""}
                    onChange={(e) => updateBanner(selectedIdx, { targetServiceId: e.target.value })}
                    placeholder="e.g. ac_service_split_1"
                    style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  />
                </div>

                <div style={{ gridColumn: "span 2", background: "#f0fdf4", border: "1px solid #bbf7d0", padding: 14, borderRadius: 10 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8, flexWrap: "wrap", gap: 8 }}>
                    <label style={{ fontSize: 13, fontWeight: 800, color: "#166534", display: "flex", alignItems: "center", gap: 6 }}>
                      <span>🏪</span>
                      <span>Target Brand / Shop Showcase Microsite Page (Optional)</span>
                    </label>
                    <div style={{ display: "flex", gap: 6 }}>
                      {activeBanner.targetShowcaseId && (
                        <button
                          type="button"
                          onClick={() => {
                            const found = showcases.find((s) => s.id === activeBanner.targetShowcaseId);
                            if (found) {
                              openEditShowcase(found, true);
                            } else {
                              openNewShowcase({ ...EMPTY_SHOWCASE, id: activeBanner.targetShowcaseId || "" }, true);
                            }
                          }}
                          className="btn"
                          style={{ fontSize: 11, padding: "4px 8px", background: "#ecfdf5", borderColor: "#a7f3d0", color: "#047857", fontWeight: 700 }}
                        >
                          ✏️ Edit This Showcase
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => openNewShowcase(undefined, true)}
                        className="btn"
                        style={{ fontSize: 11, padding: "4px 8px", background: "#dcfce7", borderColor: "#86efac", color: "#15803d", fontWeight: 700 }}
                      >
                        ✨ + Create New Showcase
                      </button>
                    </div>
                  </div>

                  <select
                    value={activeBanner.targetShowcaseId || ""}
                    onChange={(e) => updateBanner(selectedIdx, { targetShowcaseId: e.target.value })}
                    style={{ width: "100%", padding: "9px 12px", borderRadius: 8, border: "1px solid #86efac", background: "#fff", fontSize: 13, fontWeight: 600, color: "#0f172a" }}
                  >
                    <option value="">None (Standard Category / In-App Route)</option>
                    {showcases.map((sc) => (
                      <option key={sc.id} value={sc.id}>
                        [{sc.categoryBadge || "BRAND"}] {sc.title} (ID: {sc.id})
                      </option>
                    ))}
                  </select>

                  {/* Active showcase preview pill */}
                  {activeBanner.targetShowcaseId && (
                    <div style={{ marginTop: 10, padding: "8px 12px", background: "#fff", border: "1px solid #bbf7d0", borderRadius: 8, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        {showcases.find((s) => s.id === activeBanner.targetShowcaseId)?.heroImageUrl ? (
                          <img
                            src={showcases.find((s) => s.id === activeBanner.targetShowcaseId)?.heroImageUrl}
                            alt="Showcase Hero"
                            style={{ width: 36, height: 36, borderRadius: 6, objectFit: "cover" }}
                          />
                        ) : (
                          <div style={{ width: 36, height: 36, borderRadius: 6, background: "#d1fae5", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 18 }}>
                            🏪
                          </div>
                        )}
                        <div>
                          <div style={{ fontSize: 12, fontWeight: 700, color: "#065f46" }}>
                            {showcases.find((s) => s.id === activeBanner.targetShowcaseId)?.title || activeBanner.targetShowcaseId}
                          </div>
                          <div style={{ fontSize: 11, color: "#047857" }}>
                            {showcases.find((s) => s.id === activeBanner.targetShowcaseId)?.tagline || `Custom Showcase ID: ${activeBanner.targetShowcaseId}`}
                          </div>
                        </div>
                      </div>
                      <span style={{ fontSize: 10.5, background: "#dcfce7", color: "#166534", padding: "2px 8px", borderRadius: 12, fontWeight: 800 }}>
                        LINKED TO BANNER
                      </span>
                    </div>
                  )}

                  <div style={{ fontSize: 11, color: "#15803d", marginTop: 8 }}>
                    When users tap this banner on the Android app, it opens a rich Brand Landing Page with hero visual, video, story, voucher code, Google Maps navigation &amp; contact buttons.
                  </div>
                </div>

                {/* Gradient Colors */}
                <div>
                  <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                    Gradient Start (Also sets Mobile App Header!)
                  </label>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <input
                      type="color"
                      value={activeBanner.bgStartColor.startsWith("#") ? activeBanner.bgStartColor : "#0F172A"}
                      onChange={(e) => updateBanner(selectedIdx, { bgStartColor: e.target.value })}
                      style={{ width: 40, height: 36, padding: 0, border: "1px solid #cbd5e1", borderRadius: 4, cursor: "pointer" }}
                    />
                    <input
                      type="text"
                      value={activeBanner.bgStartColor}
                      onChange={(e) => updateBanner(selectedIdx, { bgStartColor: e.target.value })}
                      placeholder="#0F172A"
                      style={{ flex: 1, padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontFamily: "monospace" }}
                    />
                  </div>
                </div>

                <div>
                  <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                    Gradient End Color
                  </label>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <input
                      type="color"
                      value={activeBanner.bgEndColor.startsWith("#") ? activeBanner.bgEndColor : "#1E3A8A"}
                      onChange={(e) => updateBanner(selectedIdx, { bgEndColor: e.target.value })}
                      style={{ width: 40, height: 36, padding: 0, border: "1px solid #cbd5e1", borderRadius: 4, cursor: "pointer" }}
                    />
                    <input
                      type="text"
                      value={activeBanner.bgEndColor}
                      onChange={(e) => updateBanner(selectedIdx, { bgEndColor: e.target.value })}
                      placeholder="#1E3A8A"
                      style={{ flex: 1, padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontFamily: "monospace" }}
                    />
                  </div>
                </div>

                <div style={{ gridColumn: "span 2" }}>
                  <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#475569", marginBottom: 4 }}>
                    Slide Unique ID (Optional)
                  </label>
                  <input
                    type="text"
                    value={activeBanner.id}
                    onChange={(e) => updateBanner(selectedIdx, { id: e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, "") })}
                    placeholder="e.g. banner_ac_summer"
                    style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  />
                </div>
              </div>
            </div>

            <div style={{ marginTop: 20, paddingTop: 16, borderTop: "1px solid #e2e8f0", display: "flex", justifyContent: "flex-end" }}>
              <button
                type="button"
                onClick={saveBanners}
                disabled={saving}
                className="btn btn-approve"
                style={{ padding: "8px 24px", fontWeight: 700 }}
              >
                {saving ? "Saving..." : "Save All Banners"}
              </button>
            </div>
          </div>
        )}

        {banners.length === 0 && (
          <div className="admin-section" style={{ background: "#ffffff", border: "2px dashed #cbd5e1", borderRadius: 12, padding: 48, textAlign: "center" }}>
            <div style={{ fontSize: 36, marginBottom: 12 }}>🏷️</div>
            <h3 style={{ margin: "0 0 8px 0", fontSize: 18, fontWeight: 700, color: "#1e293b" }}>No Promotional Banners</h3>
            <p style={{ margin: "0 0 20px 0", color: "#64748b", fontSize: 14, maxWidth: 460, marginLeft: "auto", marginRight: "auto" }}>
              All promotional banners have been removed. The mobile app will not show a promotional hero banner on the services home screen.
            </p>
            <div style={{ display: "flex", justifyContent: "center", gap: 12 }}>
              <button
                type="button"
                onClick={addBlankBanner}
                className="btn btn-primary"
                style={{ fontSize: 13, padding: "8px 16px" }}
              >
                + Create a Banner
              </button>
              <button
                type="button"
                onClick={saveBanners}
                disabled={saving}
                className="btn btn-approve"
                style={{ fontSize: 13, padding: "8px 16px" }}
              >
                {saving ? "Publishing..." : "Publish Cleared State to App"}
              </button>
            </div>
          </div>
        )}
      </div>
      )}

      {/* Brand & Shop Showcases Hub */}
      {mainTab === "showcases" && (
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
          {/* Showcases Action Bar */}
          <section className="admin-section" style={{ background: "#f8fafc", border: "1px solid #e2e8f0" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 16 }}>
              <div>
                <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: "#0f172a", display: "flex", alignItems: "center", gap: 8 }}>
                  <Store size={18} color="#16a34a" />
                  Local Brand &amp; Shop Showcase Microsites ({showcases.length})
                </h3>
                <p style={{ margin: "4px 0 0 0", color: "#64748b", fontSize: 13 }}>
                  Custom in-app landing pages for featured restaurants, shopping malls, clinics, and showrooms in Khammam.
                </p>
              </div>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                <button
                  type="button"
                  onClick={() => openNewShowcase()}
                  className="btn btn-approve"
                  style={{ display: "flex", alignItems: "center", gap: 6, fontWeight: 700 }}
                >
                  <Plus size={16} />
                  + Create New Showcase
                </button>
              </div>
            </div>

            {/* Quick Templates */}
            <div style={{ marginTop: 14, display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap", borderTop: "1px solid #e2e8f0", paddingTop: 12 }}>
              <span style={{ fontSize: 12.5, fontWeight: 600, color: "#475569" }}>One-Click Khammam Presets:</span>
              {PRESET_SHOWCASES.map((ps) => (
                <button
                  key={ps.id}
                  type="button"
                  onClick={() => openNewShowcase(ps)}
                  className="btn"
                  style={{ fontSize: 12, padding: "5px 10px", background: "#fff", border: "1px solid #cbd5e1" }}
                >
                  + {ps.title} ({ps.categoryBadge})
                </button>
              ))}
            </div>
          </section>

          {/* Grid of Showcases */}
          {loadingShowcases ? (
            <div style={{ padding: 48, textAlign: "center", color: "#64748b" }}>
              <RefreshCw className="animate-spin" size={24} style={{ margin: "0 auto 8px" }} />
              Loading brand showcases...
            </div>
          ) : showcases.length === 0 ? (
            <div className="admin-section" style={{ background: "#fff", border: "2px dashed #cbd5e1", borderRadius: 12, padding: 48, textAlign: "center" }}>
              <div style={{ fontSize: 40, marginBottom: 12 }}>🏪</div>
              <h3 style={{ margin: "0 0 8px 0", fontSize: 18, fontWeight: 700, color: "#1e293b" }}>No Brand Showcases Yet</h3>
              <p style={{ margin: "0 0 20px 0", color: "#64748b", fontSize: 14, maxWidth: 500, marginInline: "auto" }}>
                Promote popular local businesses on DutyPe! Create a rich interactive showcase microsite with videos, photos, discount coupons, and direct contact buttons.
              </p>
              <div style={{ display: "flex", justifyContent: "center", gap: 10, flexWrap: "wrap" }}>
                <button
                  type="button"
                  onClick={() => openNewShowcase(PRESET_SHOWCASES[0])}
                  className="btn btn-primary"
                  style={{ fontSize: 13, padding: "8px 16px" }}
                >
                  🍽️ Load Paradise Biryani Preset
                </button>
                <button
                  type="button"
                  onClick={() => openNewShowcase(PRESET_SHOWCASES[1])}
                  className="btn btn-primary"
                  style={{ fontSize: 13, padding: "8px 16px" }}
                >
                  🛍️ Load Lotus Mall Preset
                </button>
                <button
                  type="button"
                  onClick={() => openNewShowcase()}
                  className="btn"
                  style={{ fontSize: 13, padding: "8px 16px", background: "#fff", border: "1px solid #cbd5e1" }}
                >
                  + Custom Blank Showcase
                </button>
              </div>
            </div>
          ) : (
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(340px, 1fr))", gap: 20 }}>
              {showcases.map((sc) => {
                const linkedBannersCount = banners.filter((b) => b.targetShowcaseId === sc.id).length;
                return (
                  <div
                    key={sc.id}
                    className="admin-section"
                    style={{
                      background: "#fff",
                      border: "1px solid #e2e8f0",
                      borderRadius: 12,
                      overflow: "hidden",
                      padding: 0,
                      display: "flex",
                      flexDirection: "column"
                    }}
                  >
                    {/* Card Cover */}
                    <div style={{ position: "relative", height: 140, background: "#0f172a" }}>
                      {sc.heroImageUrl ? (
                        <img
                          src={sc.heroImageUrl}
                          alt={sc.title}
                          style={{ width: "100%", height: "100%", objectFit: "cover", opacity: 0.85 }}
                        />
                      ) : (
                        <div style={{ width: "100%", height: "100%", display: "flex", alignItems: "center", justifyContent: "center", color: "#64748b", fontSize: 32 }}>
                          🏪
                        </div>
                      )}
                      <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(15,23,42,0.95), transparent 60%)" }} />

                      <div style={{ position: "absolute", top: 12, left: 12, display: "flex", gap: 6, flexWrap: "wrap" }}>
                        <span style={{ background: "rgba(15,23,42,0.85)", color: "#38bdf8", padding: "3px 8px", borderRadius: 6, fontSize: 11, fontWeight: 700, border: "1px solid rgba(56,189,248,0.3)" }}>
                          {sc.categoryBadge || "FEATURED"}
                        </span>
                        <span style={{ background: "rgba(234,179,8,0.2)", color: "#facc15", padding: "3px 8px", borderRadius: 6, fontSize: 11, fontWeight: 700, border: "1px solid rgba(250,204,21,0.4)", display: "flex", alignItems: "center", gap: 3 }}>
                          <Star size={11} fill="#facc15" />
                          {sc.rating || "4.8"}
                        </span>
                      </div>

                      {linkedBannersCount > 0 && (
                        <div style={{ position: "absolute", top: 12, right: 12 }}>
                          <span style={{ background: "#22c55e", color: "#fff", padding: "3px 8px", borderRadius: 12, fontSize: 10, fontWeight: 800 }}>
                            🔗 {linkedBannersCount} {linkedBannersCount === 1 ? "BANNER" : "BANNERS"} LINKED
                          </span>
                        </div>
                      )}

                      <div style={{ position: "absolute", bottom: 10, left: 14, right: 14 }}>
                        <h4 style={{ margin: 0, color: "#fff", fontSize: 16, fontWeight: 700, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                          {sc.title}
                        </h4>
                        <div style={{ color: "#94a3b8", fontSize: 12, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                          ID: <code style={{ color: "#38bdf8" }}>{sc.id}</code>
                        </div>
                      </div>
                    </div>

                    {/* Card Content */}
                    <div style={{ padding: 14, flex: 1, display: "flex", flexDirection: "column", gap: 8 }}>
                      <p style={{ margin: 0, fontSize: 12.5, color: "#475569", lineHeight: 1.4, display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden" }}>
                        {sc.tagline || sc.aboutDescription || "No tagline provided."}
                      </p>

                      {sc.discountOfferCode && (
                        <div style={{ background: "#f0fdf4", border: "1px dashed #86efac", borderRadius: 6, padding: "6px 10px", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                          <span style={{ fontSize: 11.5, color: "#166534", fontWeight: 700 }}>
                            🎁 {sc.discountOfferTitle || "Special Offer"}:
                          </span>
                          <span style={{ background: "#dcfce7", color: "#15803d", padding: "1px 6px", borderRadius: 4, fontFamily: "monospace", fontSize: 11, fontWeight: 800 }}>
                            {sc.discountOfferCode}
                          </span>
                        </div>
                      )}

                      {sc.address && (
                        <div style={{ fontSize: 11.5, color: "#64748b", display: "flex", alignItems: "flex-start", gap: 4 }}>
                          <MapPin size={13} style={{ flexShrink: 0, marginTop: 2, color: "#94a3b8" }} />
                          <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{sc.address}</span>
                        </div>
                      )}

                      <div style={{ display: "flex", gap: 12, fontSize: 11, color: "#64748b", marginTop: "auto", paddingTop: 8, borderTop: "1px solid #f1f5f9" }}>
                        <span>✨ {(sc.highlights || []).length} Highlights</span>
                        <span>📷 {(sc.galleryImages || []).length} Photos</span>
                        <span>⚡ {(sc.actionButtons || []).length} Actions</span>
                      </div>
                    </div>

                    {/* Card Actions Footer */}
                    <div style={{ padding: "10px 14px", background: "#f8fafc", borderTop: "1px solid #e2e8f0", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <button
                        type="button"
                        onClick={() => openEditShowcase(sc)}
                        className="btn"
                        style={{ fontSize: 12, padding: "5px 12px", background: "#eff6ff", borderColor: "#93c5fd", color: "#1d4ed8", fontWeight: 700, display: "flex", alignItems: "center", gap: 4 }}
                      >
                        <Edit3 size={13} />
                        Edit Microsite
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDeleteShowcase(sc.id)}
                        className="btn"
                        style={{ fontSize: 12, padding: "5px 10px", background: "#fef2f2", borderColor: "#fecaca", color: "#dc2626" }}
                        title="Delete Showcase"
                      >
                        <Trash2 size={13} />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Testing Instructions & Verification Guide */}
      <section className="admin-section" style={{ background: "#f0fdf4", border: "1px solid #bbf7d0" }}>
        <h4 style={{ margin: "0 0 8px 0", color: "#166534", fontSize: 15, fontWeight: 700, display: "flex", alignItems: "center", gap: 6 }}>
          <Check size={18} />
          How Promotional Banners &amp; Brand Showcases Work on Mobile:
        </h4>
        <ol style={{ margin: 0, paddingLeft: 20, color: "#15803d", fontSize: 13.5, lineHeight: 1.6 }}>
          <li>
            <b>Instant Mobile Sync:</b> Save banners or showcases above. The configuration is immediately saved to Firestore collections <code style={{ background: "rgba(0,0,0,0.06)", padding: "1px 5px", borderRadius: 4 }}>app_config</code> and <code style={{ background: "rgba(0,0,0,0.06)", padding: "1px 5px", borderRadius: 4 }}>promo_showcases</code>.
          </li>
          <li>
            <b>Open DutyPe Android App:</b> Launch the app as an Employer, Worker, or Customer. The home screen checks Firestore and renders the updated carousel at the top of the screen.
          </li>
          <li>
            <b>Observe Dynamic Header Colors:</b> Notice how as the carousel auto-scrolls through slides (every 5 seconds), the entire top status bar and search header smoothly morph their gradient color to match each banner!
          </li>
          <li>
            <b>One-Tap Brand Landing Page:</b> When users tap any promotional banner linked to a Showcase ID (e.g., <i>paradise_biryani_khammam</i>), the app smoothly navigates directly to the rich Brand Showcase page complete with hero photos, voucher codes, Google Maps navigation, and Call/WhatsApp buttons!
          </li>
        </ol>
      </section>

      {/* Dynamic Showcase Microsite Builder & Editor Modal */}
      {editingShowcase && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(15, 23, 42, 0.75)",
            backdropFilter: "blur(4px)",
            zIndex: 9999,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: 16
          }}
        >
          <div
            style={{
              background: "#ffffff",
              borderRadius: 16,
              width: "100%",
              maxWidth: 1240,
              maxHeight: "94vh",
              display: "flex",
              flexDirection: "column",
              boxShadow: "0 25px 50px -12px rgba(0, 0, 0, 0.25)",
              overflow: "hidden"
            }}
          >
            {/* Modal Header */}
            <div
              style={{
                padding: "16px 24px",
                borderBottom: "1px solid #e2e8f0",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                background: "#f8fafc"
              }}
            >
              <div>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <Store size={22} color="#16a34a" />
                  <h3 style={{ margin: 0, fontSize: 18, fontWeight: 800, color: "#0f172a" }}>
                    Brand &amp; Shop Showcase Microsite Builder
                  </h3>
                  <span
                    style={{
                      background: "#dcfce7",
                      color: "#166534",
                      padding: "2px 8px",
                      borderRadius: 12,
                      fontSize: 11,
                      fontWeight: 700
                    }}
                  >
                    Mobile Landing Page
                  </span>
                </div>
                <div style={{ fontSize: 12.5, color: "#64748b", marginTop: 2 }}>
                  Create or edit full showcase page in Firestore <code style={{ color: "#2563eb" }}>promo_showcases/{editingShowcase.id || "ID"}</code>
                </div>
              </div>

              {/* Template quick-loaders & Close */}
              <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                <span style={{ fontSize: 12, fontWeight: 600, color: "#64748b" }}>Quick Template:</span>
                <button
                  type="button"
                  onClick={() => {
                    const preset = PRESET_SHOWCASES[0];
                    setEditingShowcase({ ...preset, id: editingShowcase.id || preset.id });
                  }}
                  className="btn"
                  style={{ fontSize: 11.5, padding: "4px 8px", background: "#fff", border: "1px solid #cbd5e1" }}
                >
                  🍽️ Restaurant
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const preset = PRESET_SHOWCASES[1];
                    setEditingShowcase({ ...preset, id: editingShowcase.id || preset.id });
                  }}
                  className="btn"
                  style={{ fontSize: 11.5, padding: "4px 8px", background: "#fff", border: "1px solid #cbd5e1" }}
                >
                  🛍️ Mall
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const preset = PRESET_SHOWCASES[2];
                    setEditingShowcase({ ...preset, id: editingShowcase.id || preset.id });
                  }}
                  className="btn"
                  style={{ fontSize: 11.5, padding: "4px 8px", background: "#fff", border: "1px solid #cbd5e1" }}
                >
                  🚗 Auto Care
                </button>
                <button
                  type="button"
                  onClick={() => setEditingShowcase(null)}
                  style={{
                    background: "none",
                    border: "none",
                    cursor: "pointer",
                    color: "#64748b",
                    padding: 4,
                    display: "flex",
                    alignItems: "center"
                  }}
                >
                  <X size={20} />
                </button>
              </div>
            </div>

            {/* Modal Two-Column Body */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 380px", flex: 1, minHeight: 0 }}>
              {/* Left Column: Form Controls */}
              <div style={{ padding: 24, overflowY: "auto", display: "flex", flexDirection: "column", gap: 20 }}>
                {/* 1. Basic Identity */}
                <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: 10, padding: 16 }}>
                  <div style={{ fontSize: 13, fontWeight: 800, color: "#1e293b", marginBottom: 12, display: "flex", alignItems: "center", gap: 6 }}>
                    <span>🏷️</span>
                    <span>1. Business Identity &amp; Badges</span>
                  </div>

                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                    <div>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 4 }}>
                        Showcase ID (Slug, e.g. paradise_biryani) *
                      </label>
                      <input
                        type="text"
                        value={editingShowcase.id}
                        onChange={(e) =>
                          setEditingShowcase({
                            ...editingShowcase,
                            id: e.target.value.toLowerCase().replace(/[^a-z0-9_-]/g, "_")
                          })
                        }
                        placeholder="e.g. paradise_biryani_khammam"
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13, fontFamily: "monospace" }}
                      />
                    </div>

                    <div>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 4 }}>
                        Category Badge (e.g. RESTAURANT, MALL)
                      </label>
                      <input
                        type="text"
                        value={editingShowcase.categoryBadge}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, categoryBadge: e.target.value.toUpperCase() })}
                        placeholder="e.g. RESTAURANT or SHOPPING MALL"
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                      />
                    </div>

                    <div>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 4 }}>
                        Business / Brand Title *
                      </label>
                      <input
                        type="text"
                        value={editingShowcase.title}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, title: e.target.value })}
                        placeholder="e.g. Paradise Biryani & Grill"
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13, fontWeight: 600 }}
                      />
                    </div>

                    <div>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 4 }}>
                        Customer Rating (e.g. 4.8)
                      </label>
                      <input
                        type="text"
                        value={editingShowcase.rating}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, rating: e.target.value })}
                        placeholder="4.8"
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                      />
                    </div>

                    <div style={{ gridColumn: "span 2" }}>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 4 }}>
                        Tagline / Catchphrase
                      </label>
                      <input
                        type="text"
                        value={editingShowcase.tagline}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, tagline: e.target.value })}
                        placeholder="e.g. Authentic Nizami Dum Biryani, Sizzling Kebabs & AC Family Dining"
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                      />
                    </div>

                    <div>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 4 }}>
                        Target District (e.g. Khammam or ALL)
                      </label>
                      <input
                        type="text"
                        value={editingShowcase.district ?? "Khammam"}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, district: e.target.value })}
                        placeholder="e.g. Khammam, Warangal, or ALL"
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                      />
                    </div>

                    <div>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 4 }}>
                        Geographic Scope &amp; Tier
                      </label>
                      <select
                        value={editingShowcase.scope ?? "DISTRICT"}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, scope: e.target.value })}
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13, background: "#fff" }}
                      >
                        <option value="DISTRICT">Tier 1: Local District (e.g. Khammam only)</option>
                        <option value="STATE">Tier 2: State-Level Regional (Telangana / AP)</option>
                        <option value="NATIONAL">Tier 3: National Big Brand (Pan-India)</option>
                      </select>
                    </div>

                    <div style={{ gridColumn: "span 2", display: "flex", alignItems: "center", gap: 10, padding: "8px 12px", background: "#f1f5f9", borderRadius: 8 }}>
                      <input
                        type="checkbox"
                        id="showcase_active_toggle"
                        checked={editingShowcase.active !== false}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, active: e.target.checked })}
                        style={{ width: 16, height: 16, cursor: "pointer" }}
                      />
                      <label htmlFor="showcase_active_toggle" style={{ fontSize: 12.5, fontWeight: 700, color: "#1e293b", cursor: "pointer" }}>
                        Active &amp; Live on App (Uncheck to pause/hide promotion)
                      </label>
                    </div>
                  </div>
                </div>

                {/* 2. Visual Media: Hero & Gallery */}
                <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: 10, padding: 16 }}>
                  <div style={{ fontSize: 13, fontWeight: 800, color: "#1e293b", marginBottom: 12, display: "flex", alignItems: "center", gap: 6 }}>
                    <span>📷</span>
                    <span>2. Visual Media: Hero Photo, Video &amp; Gallery</span>
                  </div>

                  <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                    <div>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 4 }}>
                        Hero Cover Image URL
                      </label>
                      <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                        <input
                          type="text"
                          value={editingShowcase.heroImageUrl}
                          onChange={(e) => setEditingShowcase({ ...editingShowcase, heroImageUrl: e.target.value })}
                          placeholder="https://images.unsplash.com/... or upload image"
                          style={{ flex: 1, padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 12.5 }}
                        />
                        <label
                          className="btn"
                          style={{
                            fontSize: 12,
                            padding: "7px 12px",
                            background: "#eff6ff",
                            color: "#1d4ed8",
                            borderColor: "#bfdbfe",
                            fontWeight: 700,
                            cursor: "pointer",
                            display: "flex",
                            alignItems: "center",
                            gap: 4
                          }}
                        >
                          <Upload size={14} />
                          {showcaseUploading ? "Compressing..." : "Upload Photo"}
                          <input
                            type="file"
                            accept="image/*"
                            style={{ display: "none" }}
                            disabled={showcaseUploading}
                            onChange={(e) => {
                              const f = e.target.files?.[0];
                              if (f) void handleShowcaseImageUpload(f, "hero");
                            }}
                          />
                        </label>
                      </div>
                    </div>

                    <div>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 4 }}>
                        Promo Video URL (Optional: YouTube or Direct MP4)
                      </label>
                      <input
                        type="text"
                        value={editingShowcase.videoUrl}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, videoUrl: e.target.value })}
                        placeholder="https://www.youtube.com/watch?v=... or .mp4 URL"
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 12.5 }}
                      />
                    </div>

                    {/* Gallery Images */}
                    <div>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                        <label style={{ fontSize: 12, fontWeight: 700, color: "#334155" }}>
                          Photo Gallery ({(editingShowcase.galleryImages || []).length} photos)
                        </label>
                        <label
                          className="btn"
                          style={{
                            fontSize: 11,
                            padding: "3px 8px",
                            background: "#eff6ff",
                            color: "#1d4ed8",
                            borderColor: "#bfdbfe",
                            fontWeight: 700,
                            cursor: "pointer",
                            display: "flex",
                            alignItems: "center",
                            gap: 4
                          }}
                        >
                          <Plus size={12} />
                          Upload to Gallery
                          <input
                            type="file"
                            accept="image/*"
                            style={{ display: "none" }}
                            disabled={showcaseUploading}
                            onChange={(e) => {
                              const f = e.target.files?.[0];
                              if (f) void handleShowcaseImageUpload(f, "gallery");
                            }}
                          />
                        </label>
                      </div>

                      {/* Gallery thumbnails */}
                      <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 8 }}>
                        {(editingShowcase.galleryImages || []).map((imgUrl, gIdx) => (
                          <div
                            key={gIdx}
                            style={{
                              position: "relative",
                              width: 64,
                              height: 64,
                              borderRadius: 6,
                              overflow: "hidden",
                              border: "1px solid #cbd5e1"
                            }}
                          >
                            <img src={imgUrl} alt="Gallery item" style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                            <button
                              type="button"
                              onClick={() => {
                                const next = [...(editingShowcase.galleryImages || [])];
                                next.splice(gIdx, 1);
                                setEditingShowcase({ ...editingShowcase, galleryImages: next });
                              }}
                              style={{
                                position: "absolute",
                                top: 2,
                                right: 2,
                                background: "rgba(0,0,0,0.6)",
                                color: "#fff",
                                border: "none",
                                borderRadius: "50%",
                                width: 18,
                                height: 18,
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                cursor: "pointer",
                                fontSize: 10
                              }}
                            >
                              ✕
                            </button>
                          </div>
                        ))}
                      </div>

                      {/* Add photo by URL */}
                      <div style={{ display: "flex", gap: 8 }}>
                        <input
                          type="text"
                          value={newGalleryUrl}
                          onChange={(e) => setNewGalleryUrl(e.target.value)}
                          placeholder="Or paste image URL (https://...)"
                          style={{ flex: 1, padding: "6px 10px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 12 }}
                        />
                        <button
                          type="button"
                          onClick={() => {
                            if (!newGalleryUrl.trim()) return;
                            setEditingShowcase({
                              ...editingShowcase,
                              galleryImages: [...(editingShowcase.galleryImages || []), newGalleryUrl.trim()]
                            });
                            setNewGalleryUrl("");
                          }}
                          className="btn"
                          style={{ fontSize: 12, padding: "6px 12px" }}
                        >
                          Add URL
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                {/* 3. About Description & Highlights */}
                <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: 10, padding: 16 }}>
                  <div style={{ fontSize: 13, fontWeight: 800, color: "#1e293b", marginBottom: 12, display: "flex", alignItems: "center", gap: 6 }}>
                    <span>📝</span>
                    <span>3. Business Story &amp; Key Highlights</span>
                  </div>

                  <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                    <div>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 4 }}>
                        About Business Story
                      </label>
                      <textarea
                        rows={4}
                        value={editingShowcase.aboutDescription}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, aboutDescription: e.target.value })}
                        placeholder="Detailed description of what makes this business special, history, specialties, timings..."
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13, lineHeight: 1.5 }}
                      />
                    </div>

                    <div>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 6 }}>
                        Key Highlights (Bullet Points / Chips)
                      </label>
                      <div style={{ display: "flex", gap: 6, flexWrap: "wrap", marginBottom: 8 }}>
                        {(editingShowcase.highlights || []).map((h, hIdx) => (
                          <span
                            key={hIdx}
                            style={{
                              background: "#e2e8f0",
                              color: "#1e293b",
                              padding: "4px 10px",
                              borderRadius: 16,
                              fontSize: 12,
                              fontWeight: 600,
                              display: "flex",
                              alignItems: "center",
                              gap: 6
                            }}
                          >
                            <span>✓ {h}</span>
                            <button
                              type="button"
                              onClick={() => {
                                const next = [...(editingShowcase.highlights || [])];
                                next.splice(hIdx, 1);
                                setEditingShowcase({ ...editingShowcase, highlights: next });
                              }}
                              style={{ background: "none", border: "none", cursor: "pointer", color: "#64748b", padding: 0 }}
                            >
                              ✕
                            </button>
                          </span>
                        ))}
                      </div>

                      <div style={{ display: "flex", gap: 8 }}>
                        <input
                          type="text"
                          value={newHighlight}
                          onChange={(e) => setNewHighlight(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === "Enter" && newHighlight.trim()) {
                              e.preventDefault();
                              setEditingShowcase({
                                ...editingShowcase,
                                highlights: [...(editingShowcase.highlights || []), newHighlight.trim()]
                              });
                              setNewHighlight("");
                            }
                          }}
                          placeholder="e.g. Free Valet Parking, AC Family Dining, 100% Halal Certified"
                          style={{ flex: 1, padding: "6px 10px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 12 }}
                        />
                        <button
                          type="button"
                          onClick={() => {
                            if (!newHighlight.trim()) return;
                            setEditingShowcase({
                              ...editingShowcase,
                              highlights: [...(editingShowcase.highlights || []), newHighlight.trim()]
                            });
                            setNewHighlight("");
                          }}
                          className="btn"
                          style={{ fontSize: 12, padding: "6px 12px" }}
                        >
                          Add Chip
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                {/* 4. Location & Map Navigation */}
                <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: 10, padding: 16 }}>
                  <div style={{ fontSize: 13, fontWeight: 800, color: "#1e293b", marginBottom: 12, display: "flex", alignItems: "center", gap: 6 }}>
                    <span>📍</span>
                    <span>4. Location &amp; Google Maps Navigation</span>
                  </div>

                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                    <div style={{ gridColumn: "span 2" }}>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 4 }}>
                        Physical Store / Shop Address
                      </label>
                      <input
                        type="text"
                        value={editingShowcase.address}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, address: e.target.value })}
                        placeholder="e.g. Beside RTC Bus Stand, Wyra Road, Khammam, Telangana 507001"
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                      />
                    </div>

                    <div style={{ gridColumn: "span 2" }}>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 4 }}>
                        Google Maps Link (Opens GPS turn-by-turn navigation in app)
                      </label>
                      <input
                        type="text"
                        value={editingShowcase.mapLink}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, mapLink: e.target.value })}
                        placeholder="https://maps.google.com/?q=Paradise+Biryani+Khammam"
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                      />
                    </div>
                  </div>
                </div>

                {/* 5. Special Promotional Voucher / Coupon */}
                <div style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: 10, padding: 16 }}>
                  <div style={{ fontSize: 13, fontWeight: 800, color: "#166534", marginBottom: 12, display: "flex", alignItems: "center", gap: 6 }}>
                    <span>🎁</span>
                    <span>5. Exclusive Discount Voucher / Offer (Optional)</span>
                  </div>

                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                    <div>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#166534", marginBottom: 4 }}>
                        Discount Offer Title
                      </label>
                      <input
                        type="text"
                        value={editingShowcase.discountOfferTitle}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, discountOfferTitle: e.target.value })}
                        placeholder="e.g. Flat 20% Off Food Bill"
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #86efac", background: "#fff", fontSize: 13 }}
                      />
                    </div>

                    <div>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#166534", marginBottom: 4 }}>
                        Voucher Promo Code
                      </label>
                      <input
                        type="text"
                        value={editingShowcase.discountOfferCode}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, discountOfferCode: e.target.value.toUpperCase() })}
                        placeholder="e.g. PARADISE20"
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #86efac", background: "#fff", fontSize: 13, fontFamily: "monospace", fontWeight: 700 }}
                      />
                    </div>

                    <div style={{ gridColumn: "span 2" }}>
                      <label style={{ display: "block", fontSize: 12, fontWeight: 700, color: "#166534", marginBottom: 4 }}>
                        Offer Terms &amp; Conditions
                      </label>
                      <input
                        type="text"
                        value={editingShowcase.discountOfferTerms}
                        onChange={(e) => setEditingShowcase({ ...editingShowcase, discountOfferTerms: e.target.value })}
                        placeholder="e.g. Valid on dine-in above ₹499. Show code to billing staff before bill generation."
                        style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid #86efac", background: "#fff", fontSize: 12 }}
                      />
                    </div>
                  </div>
                </div>

                {/* 6. Sticky Action Buttons (Call, WhatsApp, Maps, URL) */}
                <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: 10, padding: 16 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
                    <div style={{ fontSize: 13, fontWeight: 800, color: "#1e293b", display: "flex", alignItems: "center", gap: 6 }}>
                      <span>⚡</span>
                      <span>6. Mobile Bottom Action Buttons (CTA Bar)</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        setEditingShowcase({
                          ...editingShowcase,
                          actionButtons: [
                            ...(editingShowcase.actionButtons || []),
                            { label: "New Button", icon: "call", actionUrl: "tel:+91", isPrimary: false }
                          ]
                        });
                      }}
                      className="btn"
                      style={{ fontSize: 11, padding: "4px 8px", background: "#eff6ff", color: "#1d4ed8", borderColor: "#bfdbfe", fontWeight: 700 }}
                    >
                      + Add Action Button
                    </button>
                  </div>

                  <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                    {(editingShowcase.actionButtons || []).map((btn, bIdx) => (
                      <div
                        key={bIdx}
                        style={{
                          background: "#ffffff",
                          border: "1px solid #cbd5e1",
                          borderRadius: 8,
                          padding: 12,
                          display: "grid",
                          gridTemplateColumns: "140px 100px 1fr auto auto",
                          gap: 8,
                          alignItems: "center"
                        }}
                      >
                        <input
                          type="text"
                          value={btn.label}
                          onChange={(e) => {
                            const next = [...(editingShowcase.actionButtons || [])];
                            next[bIdx] = { ...next[bIdx], label: e.target.value };
                            setEditingShowcase({ ...editingShowcase, actionButtons: next });
                          }}
                          placeholder="Button Label"
                          style={{ padding: "6px 8px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 12, fontWeight: 600 }}
                        />

                        <select
                          value={btn.icon}
                          onChange={(e) => {
                            const next = [...(editingShowcase.actionButtons || [])];
                            next[bIdx] = { ...next[bIdx], icon: e.target.value };
                            setEditingShowcase({ ...editingShowcase, actionButtons: next });
                          }}
                          style={{ padding: "6px 8px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 12 }}
                        >
                          <option value="call">📞 Call</option>
                          <option value="whatsapp">💬 WhatsApp</option>
                          <option value="maps">📍 Maps</option>
                          <option value="url">🌐 Website</option>
                        </select>

                        <input
                          type="text"
                          value={btn.actionUrl}
                          onChange={(e) => {
                            const next = [...(editingShowcase.actionButtons || [])];
                            next[bIdx] = { ...next[bIdx], actionUrl: e.target.value };
                            setEditingShowcase({ ...editingShowcase, actionButtons: next });
                          }}
                          placeholder="tel:+91..., https://wa.me/..., https://maps... or website"
                          style={{ padding: "6px 8px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 12 }}
                        />

                        <label style={{ display: "flex", alignItems: "center", gap: 4, fontSize: 11, fontWeight: 600, color: "#334155", cursor: "pointer", whiteSpace: "nowrap" }}>
                          <input
                            type="checkbox"
                            checked={btn.isPrimary}
                            onChange={(e) => {
                              const next = [...(editingShowcase.actionButtons || [])];
                              next[bIdx] = { ...next[bIdx], isPrimary: e.target.checked };
                              setEditingShowcase({ ...editingShowcase, actionButtons: next });
                            }}
                          />
                          Primary
                        </label>

                        <button
                          type="button"
                          onClick={() => {
                            const next = [...(editingShowcase.actionButtons || [])];
                            next.splice(bIdx, 1);
                            setEditingShowcase({ ...editingShowcase, actionButtons: next });
                          }}
                          style={{ background: "none", border: "none", color: "#ef4444", cursor: "pointer", padding: 4 }}
                          title="Remove Button"
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {/* Right Column: Live Dark Mode Mobile App Simulator */}
              <div
                style={{
                  background: "#0b1120",
                  borderLeft: "1px solid #e2e8f0",
                  padding: 16,
                  overflowY: "auto",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center"
                }}
              >
                <div style={{ fontSize: 11, fontWeight: 700, color: "#94a3b8", textTransform: "uppercase", letterSpacing: "0.05em", marginBottom: 12, display: "flex", alignItems: "center", gap: 6 }}>
                  <Smartphone size={14} />
                  Live Mobile Landing Page
                </div>

                {/* Mobile Phone Mockup */}
                <div
                  style={{
                    width: 320,
                    borderRadius: 28,
                    overflow: "hidden",
                    border: "4px solid #1e293b",
                    boxShadow: "0 25px 50px -12px rgba(0,0,0,0.5)",
                    background: "#0f172a",
                    color: "#ffffff",
                    display: "flex",
                    flexDirection: "column",
                    minHeight: 560
                  }}
                >
                  {/* Status Bar */}
                  <div style={{ padding: "8px 16px 4px", display: "flex", justifyContent: "space-between", fontSize: 11, color: "#94a3b8", background: "#0f172a" }}>
                    <span>9:41</span>
                    <span>5G 🔋</span>
                  </div>

                  {/* Top Bar with Back & Category */}
                  <div style={{ padding: "6px 14px 10px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div style={{ width: 28, height: 28, borderRadius: "50%", background: "#1e293b", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12 }}>
                      ←
                    </div>
                    <span style={{ fontSize: 10, background: "rgba(56,189,248,0.15)", color: "#38bdf8", padding: "2px 8px", borderRadius: 10, fontWeight: 700, border: "1px solid rgba(56,189,248,0.3)" }}>
                      {editingShowcase.categoryBadge || "FEATURED"}
                    </span>
                    <div style={{ display: "flex", alignItems: "center", gap: 2, fontSize: 11, color: "#facc15", fontWeight: 700 }}>
                      <Star size={11} fill="#facc15" />
                      {editingShowcase.rating || "4.8"}
                    </div>
                  </div>

                  {/* Scrollable Screen Content */}
                  <div style={{ flex: 1, overflowY: "auto", padding: "0 14px 16px", display: "flex", flexDirection: "column", gap: 12 }}>
                    {/* Hero Image */}
                    <div style={{ position: "relative", height: 160, borderRadius: 14, overflow: "hidden", background: "#1e293b" }}>
                      {editingShowcase.heroImageUrl ? (
                        <img src={editingShowcase.heroImageUrl} alt="Hero" style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                      ) : (
                        <div style={{ width: "100%", height: "100%", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 32 }}>
                          🏪
                        </div>
                      )}
                      {editingShowcase.videoUrl && (
                        <div style={{ position: "absolute", bottom: 8, right: 8, background: "rgba(0,0,0,0.7)", color: "#fff", padding: "3px 8px", borderRadius: 12, fontSize: 10, display: "flex", alignItems: "center", gap: 4 }}>
                          ▶ Watch Video
                        </div>
                      )}
                    </div>

                    {/* Title & Tagline */}
                    <div>
                      <h4 style={{ margin: 0, fontSize: 17, fontWeight: 800, color: "#fff" }}>
                        {editingShowcase.title || "Brand Name"}
                      </h4>
                      <p style={{ margin: "3px 0 0", fontSize: 11.5, color: "#94a3b8", lineHeight: 1.3 }}>
                        {editingShowcase.tagline || "Brand tagline and specialties appear here."}
                      </p>
                    </div>

                    {/* Voucher Card */}
                    {editingShowcase.discountOfferCode && (
                      <div
                        style={{
                          background: "#064e3b",
                          border: "1px dashed #34d399",
                          borderRadius: 10,
                          padding: 10,
                          color: "#fff"
                        }}
                      >
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
                          <span style={{ fontSize: 11.5, fontWeight: 800, color: "#6ee7b7" }}>
                            🎁 {editingShowcase.discountOfferTitle || "Special Offer"}
                          </span>
                          <span style={{ background: "#047857", color: "#ecfdf5", padding: "1px 6px", borderRadius: 4, fontFamily: "monospace", fontSize: 10, fontWeight: 800 }}>
                            {editingShowcase.discountOfferCode}
                          </span>
                        </div>
                        <div style={{ fontSize: 9.5, color: "#a7f3d0" }}>
                          {editingShowcase.discountOfferTerms || "Show this code at counter"}
                        </div>
                      </div>
                    )}

                    {/* Highlights */}
                    {(editingShowcase.highlights || []).length > 0 && (
                      <div style={{ display: "flex", gap: 4, flexWrap: "wrap" }}>
                        {editingShowcase.highlights.map((h, i) => (
                          <span key={i} style={{ background: "#1e293b", color: "#cbd5e1", padding: "2px 7px", borderRadius: 8, fontSize: 9.5, fontWeight: 600 }}>
                            ✓ {h}
                          </span>
                        ))}
                      </div>
                    )}

                    {/* About */}
                    {editingShowcase.aboutDescription && (
                      <div style={{ background: "#1e293b", borderRadius: 10, padding: 10 }}>
                        <div style={{ fontSize: 11, fontWeight: 700, color: "#94a3b8", marginBottom: 3 }}>About Store</div>
                        <div style={{ fontSize: 10.5, color: "#cbd5e1", lineHeight: 1.4 }}>
                          {editingShowcase.aboutDescription}
                        </div>
                      </div>
                    )}

                    {/* Gallery Preview */}
                    {(editingShowcase.galleryImages || []).length > 0 && (
                      <div>
                        <div style={{ fontSize: 11, fontWeight: 700, color: "#94a3b8", marginBottom: 6 }}>Photo Gallery</div>
                        <div style={{ display: "flex", gap: 6, overflowX: "auto" }}>
                          {editingShowcase.galleryImages.map((g, i) => (
                            <img key={i} src={g} alt="Gallery" style={{ width: 60, height: 60, borderRadius: 8, objectFit: "cover", flexShrink: 0 }} />
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Address */}
                    {editingShowcase.address && (
                      <div style={{ background: "#1e293b", borderRadius: 10, padding: 10, display: "flex", alignItems: "flex-start", gap: 6 }}>
                        <MapPin size={13} style={{ color: "#38bdf8", flexShrink: 0, marginTop: 1 }} />
                        <div style={{ fontSize: 10.5, color: "#cbd5e1" }}>{editingShowcase.address}</div>
                      </div>
                    )}
                  </div>

                  {/* Sticky Mobile Bottom Bar */}
                  <div
                    style={{
                      background: "#1e293b",
                      borderTop: "1px solid #334155",
                      padding: "8px 12px",
                      display: "flex",
                      gap: 8
                    }}
                  >
                    {(editingShowcase.actionButtons || []).length > 0 ? (
                      editingShowcase.actionButtons.map((btn, i) => (
                        <div
                          key={i}
                          style={{
                            flex: 1,
                            background: btn.isPrimary ? "#2563eb" : "#334155",
                            color: "#ffffff",
                            padding: "8px 6px",
                            borderRadius: 8,
                            fontSize: 10.5,
                            fontWeight: 700,
                            textAlign: "center",
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap"
                          }}
                        >
                          {btn.icon === "call" ? "📞 " : btn.icon === "whatsapp" ? "💬 " : btn.icon === "maps" ? "📍 " : "🌐 "}
                          {btn.label}
                        </div>
                      ))
                    ) : (
                      <div style={{ flex: 1, background: "#2563eb", color: "#fff", padding: "8px", borderRadius: 8, fontSize: 11, textAlign: "center", fontWeight: 700 }}>
                        Contact Store
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Modal Footer */}
            <div
              style={{
                padding: "14px 24px",
                borderTop: "1px solid #e2e8f0",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                background: "#f8fafc"
              }}
            >
              <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13, fontWeight: 600, color: "#334155", cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={linkToBannerOnSave}
                  onChange={(e) => setLinkToBannerOnSave(e.target.checked)}
                />
                <span>🔗 Auto-link this showcase page to active banner slide ({activeBanner?.headline || "Slide"})</span>
              </label>

              <div style={{ display: "flex", gap: 12 }}>
                <button
                  type="button"
                  onClick={() => setEditingShowcase(null)}
                  className="btn"
                  style={{ padding: "8px 18px", fontSize: 13 }}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() => void handleShowcaseSave()}
                  disabled={showcaseSaving}
                  className="btn btn-approve"
                  style={{ padding: "8px 24px", fontSize: 14, fontWeight: 700, display: "flex", alignItems: "center", gap: 6 }}
                >
                  {showcaseSaving ? <RefreshCw className="animate-spin" size={16} /> : <Check size={16} />}
                  {showcaseSaving ? "Publishing Showcase..." : "Save & Publish Showcase Microsite"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
