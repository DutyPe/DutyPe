"use client";

import { useEffect, useMemo, useState, useRef } from "react";
import { doc, getDoc, onSnapshot, setDoc } from "firebase/firestore";
import { ref, uploadBytes, getDownloadURL, deleteObject } from "firebase/storage";
import { httpsCallable } from "firebase/functions";
import {
  Upload,
  Image as ImageIcon,
  CheckCircle2,
  AlertCircle,
  Copy,
  Check,
  RefreshCw,
  Search,
  Filter,
  Plus,
  Trash2,
  ExternalLink,
  Sparkles,
  Smartphone,
  Eye,
  ArrowRight,
  Info
} from "lucide-react";
import { getFirebaseServices } from "@/lib/firebase/client";

export interface ServiceCategoryDef {
  id: string;
  name: string;
  te: string;
  hi: string;
  skill: "BASIC" | "SKILLED";
  imageUrl?: string;
}

export interface ServiceOptionDef {
  id: string;
  title: string;
  price: number;
  originalPrice?: number;
  durationMin?: number;
  description?: string;
}

export interface ServiceItemDef {
  id: string;
  category: string;
  name: string;
  te: string;
  hi: string;
  price: number;
  originalPrice?: number;
  durationMin: number;
  inspection?: boolean;
  includes: string;
  active?: boolean;
  imageUrl?: string;
  isCustom?: boolean;
  options?: ServiceOptionDef[];
}

interface ImageOptimizationResult {
  file: File;
  originalSizeKb: number;
  webpBlob: Blob;
  webpSizeKb: number;
  previewUrl: string;
  width: number;
  height: number;
  savingsPct: number;
}

const DEFAULT_CATEGORIES: ServiceCategoryDef[] = [
  { id: "CLEANING", name: "Home Cleaning", te: "ఇంటి క్లీనింగ్", hi: "घर की सफाई", skill: "BASIC" },
  { id: "AC", name: "AC Service & Repair", te: "ఏసీ సర్వీస్", hi: "एसी सर्विस", skill: "SKILLED" },
  { id: "ELECTRICIAN", name: "Electrician", te: "ఎలక్ట్రీషియన్", hi: "इलेक्ट्रीशियन", skill: "SKILLED" },
  { id: "PLUMBER", name: "Plumber", te: "ప్లంబర్", hi: "प्लम्बर", skill: "SKILLED" },
  { id: "APPLIANCE", name: "Appliance & RO Repair", te: "ఉపకరణాలు & ఆర్వో", hi: "उपकरण और आरओ", skill: "SKILLED" },
  { id: "CARPENTER", name: "Carpenter", te: "కార్పెంటర్", hi: "बढ़ई", skill: "SKILLED" },
  { id: "PAINTER", name: "Painting", te: "పెయింటింగ్", hi: "पेंटिंग", skill: "SKILLED" },
  { id: "HOME_HELP", name: "Home Help & Shifting", te: "హెల్పర్స్ & షిఫ్టింగ్", hi: "हेल्पर्स और शिफ्टिंग", skill: "BASIC" },
  { id: "VEHICLE", name: "Car & Bike Wash", te: "కార్ & బైక్ వాష్", hi: "कार और बाइक वॉश", skill: "BASIC" }
];

const DEFAULT_CATEGORY_IDS = new Set(DEFAULT_CATEGORIES.map((c) => c.id));

const CATEGORY_NAMES: Record<string, string> = {
  CLEANING: "Home Cleaning",
  AC: "AC Service & Repair",
  ELECTRICIAN: "Electrician",
  PLUMBER: "Plumber",
  APPLIANCE: "Appliance & RO Repair",
  CARPENTER: "Carpenter",
  PAINTER: "Painting",
  HOME_HELP: "Home Help & Shifting",
  VEHICLE: "Car & Bike Wash"
};

const BUCKET_NAME = "dutype-860ac.firebasestorage.app";

// Helper to convert any image file to 512x512 WebP directly in browser
async function optimizeImageForMobile(file: File): Promise<ImageOptimizationResult> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        let w = img.width;
        let h = img.height;
        const maxDim = 512;
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
        // Draw image smoothly
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

export function AdminServiceCatalogClient() {
  const services = useMemo(() => getFirebaseServices(), []);

  const [categories, setCategories] = useState<ServiceCategoryDef[]>([]);
  const [servicesList, setServicesList] = useState<ServiceItemDef[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Active Tab: "services" | "categories"
  const [activeTab, setActiveTab] = useState<"services" | "categories">("services");
  const [categoryFilter, setCategoryFilter] = useState<string>("ALL");
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [imageFilter, setImageFilter] = useState<"ALL" | "HAS_IMAGE" | "NO_IMAGE">("ALL");

  // Real-time Optimization & Upload State per item ID
  const [pendingUploads, setPendingUploads] = useState<Record<string, ImageOptimizationResult>>({});
  const [uploadingId, setUploadingId] = useState<string | null>(null);

  // Editing state
  const [editingItem, setEditingItem] = useState<ServiceItemDef | null>(null);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [copiedPromptId, setCopiedPromptId] = useState<string | null>(null);

  // New Custom Service form state
  const [newService, setNewService] = useState<Partial<ServiceItemDef>>({
    category: "CLEANING",
    price: 299,
    durationMin: 60,
    inspection: false,
    active: true,
    includes: ""
  });

  // New Category form state
  const [isAddCategoryModalOpen, setIsAddCategoryModalOpen] = useState(false);
  const [newCategory, setNewCategory] = useState<Partial<ServiceCategoryDef>>({
    skill: "SKILLED"
  });

  // Load catalog and overrides
  useEffect(() => {
    if (!services) return;
    loadCatalog();
  }, [services]);

  async function loadCatalog() {
    if (!services) return;
    setLoading(true);
    setError(null);
    try {
      // 1. Fetch full catalog from Cloud Function
      let cloudCats: ServiceCategoryDef[] = [];
      let cloudSvcs: ServiceItemDef[] = [];

      try {
        const res = await httpsCallable(services.functions, "getServiceCatalog")({ all: true });
        const data = res.data as { categories?: ServiceCategoryDef[]; services?: ServiceItemDef[] };
        cloudCats = data.categories || [];
        cloudSvcs = data.services || [];
      } catch (fnErr) {
        console.warn("Function getServiceCatalog failed, falling back to direct Firestore fetch:", fnErr);
      }

      // 2. Read live Firestore overrides doc
      const snap = await getDoc(doc(services.db, "app_config", "services"));
      const docData = (snap.data() || {}) as {
        services?: Partial<ServiceItemDef>[];
        categories?: Partial<ServiceCategoryDef>[];
      };

      const svcOverrides = new Map<string, Partial<ServiceItemDef>>();
      if (Array.isArray(docData.services)) {
        for (const item of docData.services) {
          if (item?.id) svcOverrides.set(item.id, item);
        }
      }

      const catOverrides = new Map<string, Partial<ServiceCategoryDef>>();
      if (Array.isArray(docData.categories)) {
        for (const c of docData.categories) {
          if (c?.id) catOverrides.set(c.id, c);
        }
      }

      // Merge categories (defaults + cloud cats + Firestore overrides)
      const baseCats = cloudCats.length > 0 ? cloudCats : DEFAULT_CATEGORIES;
      const mergedCats = baseCats.map((cat) => {
        const ov = catOverrides.get(cat.id);
        return {
          ...cat,
          name: (typeof ov?.name === "string" && ov.name.trim()) || cat.name,
          te: (typeof ov?.te === "string" && ov.te.trim()) || cat.te,
          hi: (typeof ov?.hi === "string" && ov.hi.trim()) || cat.hi,
          skill: ov?.skill || cat.skill,
          imageUrl: ov?.imageUrl || cat.imageUrl || ""
        };
      });

      // Append any custom categories in docData that weren't in baseCats
      for (const [id, ov] of catOverrides.entries()) {
        if (!mergedCats.some((c) => c.id.toUpperCase() === id.toUpperCase()) && ov.name) {
          mergedCats.push({
            id: id.toUpperCase(),
            name: ov.name,
            te: ov.te || ov.name,
            hi: ov.hi || ov.name,
            skill: ov.skill === "BASIC" ? "BASIC" : "SKILLED",
            imageUrl: ov.imageUrl || ""
          });
        }
      }

      // Merge services
      const mergedSvcs = cloudSvcs.map((svc) => {
        const ov = svcOverrides.get(svc.id);
        return {
          ...svc,
          price: ov?.price ?? svc.price,
          originalPrice: ov?.originalPrice ?? svc.originalPrice,
          name: ov?.name ?? svc.name,
          te: ov?.te ?? svc.te,
          hi: ov?.hi ?? svc.hi,
          active: ov?.active ?? (svc.active !== false),
          imageUrl: ov?.imageUrl ?? svc.imageUrl ?? "",
          includes: ov?.includes ?? svc.includes,
          options: Array.isArray(ov?.options) ? ov.options : (Array.isArray(svc.options) ? svc.options : [])
        };
      });

      // Append any custom services in docData that weren't in cloudSvcs
      for (const [id, ov] of svcOverrides.entries()) {
        if (!mergedSvcs.some((s) => s.id.toLowerCase() === id.toLowerCase()) && ov.name) {
          mergedSvcs.push({
            id: id.toLowerCase(),
            category: ov.category || "CLEANING",
            name: ov.name,
            te: ov.te || ov.name,
            hi: ov.hi || ov.name,
            price: Number(ov.price) || 299,
            originalPrice: ov.originalPrice ? Number(ov.originalPrice) : undefined,
            durationMin: Number(ov.durationMin) || 60,
            inspection: ov.inspection === true,
            includes: ov.includes || "",
            active: ov.active !== false,
            imageUrl: ov.imageUrl || "",
            isCustom: true,
            options: Array.isArray(ov.options) ? ov.options : []
          });
        }
      }

      setCategories(mergedCats);
      setServicesList(mergedSvcs);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load service catalog.");
    } finally {
      setLoading(false);
    }
  }

  // Handle local file selection and convert to WebP with metrics
  async function handleFileSelect(id: string, file: File) {
    setError(null);
    try {
      const result = await optimizeImageForMobile(file);
      setPendingUploads((prev) => ({ ...prev, [id]: result }));
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Error reading image file.");
    }
  }

  // Execute upload to Firebase Storage and update Firestore catalog
  async function uploadImageToStorage(id: string, isCategory: boolean = false) {
    if (!services) return;
    const opt = pendingUploads[id];
    if (!opt) return;

    setUploadingId(id);
    setError(null);
    setMessage(null);

    const fileName = `${id.toLowerCase()}.webp`;

    try {
      let publicUrl = "";

      // 1. Try server-side upload API (bypasses client storage permission limits)
      try {
        const formData = new FormData();
        formData.append("image", opt.webpBlob, fileName);
        formData.append("id", id);
        formData.append("type", isCategory ? "category" : "service");

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
          publicUrl = resJson.url;
        } else {
          const errData = await apiRes.json().catch(() => null);
          console.warn("Server-side upload returned non-200:", errData);
        }
      } catch (apiErr) {
        console.warn("Server-side upload failed, falling back to client upload:", apiErr);
      }

      // 2. Client-side fallback if server-side upload was not completed
      if (!publicUrl) {
        const storagePrefix = isCategory ? "categories" : "services";
        const storageRef = ref(services.storage, `${storagePrefix}/${fileName}`);

        await uploadBytes(storageRef, opt.webpBlob, {
          contentType: "image/webp",
          cacheControl: "public, max-age=31536000, immutable"
        });

        try {
          publicUrl = await getDownloadURL(storageRef);
        } catch {
          publicUrl = `https://firebasestorage.googleapis.com/v0/b/${BUCKET_NAME}/o/${encodeURIComponent(
            `${storagePrefix}/${fileName}`
          )}?alt=media`;
        }

        // Update Firestore app_config/services (if client fallback was used)
        const docRef = doc(services.db, "app_config", "services");
        const snap = await getDoc(docRef);
        const data = (snap.data() || {}) as Record<string, unknown>;

        if (isCategory) {
          const existingCats = Array.isArray(data.categories) ? [...data.categories] : [];
          const idx = existingCats.findIndex((c: any) => String(c?.id).toLowerCase() === id.toLowerCase());
          if (idx >= 0) {
            existingCats[idx] = { ...existingCats[idx], imageUrl: publicUrl };
          } else {
            existingCats.push({ id, imageUrl: publicUrl });
          }
          await setDoc(docRef, { ...data, categories: existingCats }, { merge: true });
        } else {
          const existingSvcs = Array.isArray(data.services) ? [...data.services] : [];
          const idx = existingSvcs.findIndex((s: any) => String(s?.id).toLowerCase() === id.toLowerCase());
          if (idx >= 0) {
            existingSvcs[idx] = { ...existingSvcs[idx], imageUrl: publicUrl };
          } else {
            existingSvcs.push({ id, imageUrl: publicUrl });
          }
          await setDoc(docRef, { ...data, services: existingSvcs }, { merge: true });
        }
      }

      if (isCategory) {
        setCategories((prev) =>
          prev.map((c) => (c.id.toLowerCase() === id.toLowerCase() ? { ...c, imageUrl: publicUrl } : c))
        );
      } else {
        setServicesList((prev) =>
          prev.map((s) => (s.id.toLowerCase() === id.toLowerCase() ? { ...s, imageUrl: publicUrl } : s))
        );
      }

      // Clear pending upload
      setPendingUploads((prev) => {
        const next = { ...prev };
        delete next[id];
        return next;
      });

      setMessage(
        `✓ Uploaded ${fileName} (${opt.webpSizeKb} KB). Updated on cloud & cached for mobile users!`
      );
    } catch (upErr: unknown) {
      setError(upErr instanceof Error ? upErr.message : "Failed to upload image.");
    } finally {
      setUploadingId(null);
    }
  }

  // Revert/Remove custom image back to vector fallback
  async function removeCustomImage(id: string, isCategory: boolean = false) {
    if (!services) return;
    if (!confirm(`Revert ${id} to default vector icon?`)) return;

    setUploadingId(id);
    try {
      const docRef = doc(services.db, "app_config", "services");
      const snap = await getDoc(docRef);
      const data = (snap.data() || {}) as Record<string, unknown>;

      if (isCategory) {
        const existingCats = Array.isArray(data.categories) ? [...data.categories] : [];
        const updated = existingCats.filter((c: any) => c.id !== id);
        await setDoc(docRef, { ...data, categories: updated }, { merge: true });
        setCategories((prev) =>
          prev.map((c) => (c.id === id ? { ...c, imageUrl: "" } : c))
        );
      } else {
        const existingSvcs = Array.isArray(data.services) ? [...data.services] : [];
        const updated = existingSvcs.map((s: any) => (s.id === id ? { ...s, imageUrl: "" } : s));
        await setDoc(docRef, { ...data, services: updated }, { merge: true });
        setServicesList((prev) =>
          prev.map((s) => (s.id === id ? { ...s, imageUrl: "" } : s))
        );
      }

      setMessage(`Reverted ${id} to vector icon.`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to remove custom image.");
    } finally {
      setUploadingId(null);
    }
  }

  // Save changes to service details (Price, Name, Active)
  async function saveServiceDetails(updated: ServiceItemDef) {
    if (!services) return;
    setSaving(true);
    setError(null);
    setMessage(null);

    try {
      const docRef = doc(services.db, "app_config", "services");
      const snap = await getDoc(docRef);
      const data = (snap.data() || {}) as Record<string, unknown>;

      const existingSvcs = Array.isArray(data.services) ? [...data.services] : [];
      const idx = existingSvcs.findIndex((s: any) => s.id === updated.id);

      const entry = {
        id: updated.id,
        category: updated.category,
        name: updated.name.trim(),
        te: updated.te?.trim() || updated.name.trim(),
        hi: updated.hi?.trim() || updated.name.trim(),
        price: Number(updated.price) || 0,
        originalPrice: updated.originalPrice ? Number(updated.originalPrice) : undefined,
        durationMin: Number(updated.durationMin) || 60,
        includes: updated.includes || "",
        active: updated.active !== false,
        imageUrl: updated.imageUrl || "",
        options: updated.options || []
      };

      if (idx >= 0) {
        existingSvcs[idx] = { ...existingSvcs[idx], ...entry };
      } else {
        existingSvcs.push(entry);
      }

      await setDoc(docRef, { ...data, services: existingSvcs }, { merge: true });

      setServicesList((prev) =>
        prev.map((s) => (s.id === updated.id ? { ...s, ...entry } : s))
      );
      setEditingItem(null);
      setMessage(`Saved service: ${updated.name}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to save service details.");
    } finally {
      setSaving(false);
    }
  }

  // Add new service
  async function createCustomService() {
    if (!services) return;
    const id = (newService.id || "").trim().toLowerCase().replace(/[^a-z0-9_]/g, "_");
    const name = (newService.name || "").trim();
    if (!id || !name) {
      alert("Please enter a valid Service ID and Name.");
      return;
    }
    if (servicesList.some((s) => s.id === id)) {
      alert(`Service ID "${id}" already exists.`);
      return;
    }

    setSaving(true);
    try {
      const docRef = doc(services.db, "app_config", "services");
      const snap = await getDoc(docRef);
      const data = (snap.data() || {}) as Record<string, unknown>;
      const existingSvcs = Array.isArray(data.services) ? [...data.services] : [];

      const created: ServiceItemDef = {
        id,
        category: newService.category || "CLEANING",
        name,
        te: newService.te?.trim() || name,
        hi: newService.hi?.trim() || name,
        price: Number(newService.price) || 299,
        originalPrice: newService.originalPrice ? Number(newService.originalPrice) : undefined,
        durationMin: Number(newService.durationMin) || 60,
        inspection: newService.inspection === true,
        includes: newService.includes?.trim() || "",
        active: newService.active !== false,
        imageUrl: newService.imageUrl || "",
        isCustom: true,
        options: newService.options || []
      };

      existingSvcs.push(created);
      await setDoc(docRef, { ...data, services: existingSvcs }, { merge: true });

      setServicesList((prev) => [...prev, created]);
      setIsAddModalOpen(false);
      setNewService({
        category: "CLEANING",
        price: 299,
        durationMin: 60,
        inspection: false,
        active: true,
        includes: ""
      });
      setMessage(`Added new service: ${name}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to create service.");
    } finally {
      setSaving(false);
    }
  }

  // Add new category
  async function createCustomCategory() {
    if (!services) return;
    const id = (newCategory.id || "").trim().toUpperCase().replace(/[^A-Z0-9_]/g, "_");
    const name = (newCategory.name || "").trim();
    if (!id || !name) {
      alert("Please enter a valid Category ID and Name.");
      return;
    }
    if (categories.some((c) => c.id.toUpperCase() === id)) {
      alert(`Category ID "${id}" already exists.`);
      return;
    }

    setSaving(true);
    try {
      const docRef = doc(services.db, "app_config", "services");
      const snap = await getDoc(docRef);
      const data = (snap.data() || {}) as Record<string, unknown>;
      const existingCats = Array.isArray(data.categories) ? [...data.categories] : [];

      const created: ServiceCategoryDef = {
        id,
        name,
        te: newCategory.te?.trim() || name,
        hi: newCategory.hi?.trim() || name,
        skill: newCategory.skill === "BASIC" ? "BASIC" : "SKILLED",
        imageUrl: newCategory.imageUrl || ""
      };

      existingCats.push(created);
      await setDoc(docRef, { ...data, categories: existingCats }, { merge: true });

      setCategories((prev) => [...prev, created]);
      setIsAddCategoryModalOpen(false);
      setNewCategory({ skill: "SKILLED" });
      setMessage(`Added new category: ${name} (${id})`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to create category.");
    } finally {
      setSaving(false);
    }
  }

  // Delete custom category
  async function deleteCustomCategory(id: string) {
    if (!services) return;
    const cat = categories.find((c) => c.id === id);
    if (!confirm(`Are you sure you want to delete category "${cat?.name || id}"?`)) return;

    setSaving(true);
    try {
      const docRef = doc(services.db, "app_config", "services");
      const snap = await getDoc(docRef);
      const data = (snap.data() || {}) as Record<string, unknown>;
      const existingCats = Array.isArray(data.categories) ? [...data.categories] : [];
      const filtered = existingCats.filter((c: any) => String(c?.id).toUpperCase() !== id.toUpperCase());

      await setDoc(docRef, { ...data, categories: filtered }, { merge: true });
      setCategories((prev) => prev.filter((c) => c.id.toUpperCase() !== id.toUpperCase()));
      setMessage(`Removed category: ${id}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete category.");
    } finally {
      setSaving(false);
    }
  }

  // Delete custom service
  async function deleteCustomService(id: string) {
    if (!services) return;
    const svc = servicesList.find((s) => s.id === id);
    if (!confirm(`Are you sure you want to delete service "${svc?.name || id}"?`)) return;

    setSaving(true);
    try {
      const docRef = doc(services.db, "app_config", "services");
      const snap = await getDoc(docRef);
      const data = (snap.data() || {}) as Record<string, unknown>;
      const existingSvcs = Array.isArray(data.services) ? [...data.services] : [];
      const filtered = existingSvcs.filter((s: any) => String(s?.id).toLowerCase() !== id.toLowerCase());

      await setDoc(docRef, { ...data, services: filtered }, { merge: true });
      setServicesList((prev) => prev.filter((s) => s.id.toLowerCase() !== id.toLowerCase()));
      setMessage(`Removed service: ${id}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete service.");
    } finally {
      setSaving(false);
    }
  }

  // Copy ChatGPT Master Prompt to clipboard
  function copyChatGptPrompt(name: string, id: string) {
    const promptText = `Cute 3D isometric clay render of ${name}, pastel matte finish, smooth clay texture, soft studio lighting, transparent PNG background, high detail, centered, no frame --v 6.0\n\n[Instruction: Create an isolated 3D clay asset on a pure transparent background (PNG or WebP), cropped 512x512 with 10% padding for DutyPe Home Services mobile app icon]`;
    navigator.clipboard.writeText(promptText);
    setCopiedPromptId(id);
    setTimeout(() => setCopiedPromptId(null), 2500);
  }

  // Filtered lists
  const filteredServices = servicesList.filter((s) => {
    if (categoryFilter !== "ALL" && s.category !== categoryFilter) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const match =
        s.name.toLowerCase().includes(q) ||
        s.id.toLowerCase().includes(q) ||
        (s.te && s.te.toLowerCase().includes(q));
      if (!match) return false;
    }
    if (imageFilter === "HAS_IMAGE" && !s.imageUrl) return false;
    if (imageFilter === "NO_IMAGE" && s.imageUrl) return false;
    return true;
  });

  const servicesWithImagesCount = servicesList.filter((s) => Boolean(s.imageUrl)).length;
  const categoriesWithImagesCount = categories.filter((c) => Boolean(c.imageUrl)).length;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24, maxWidth: 1200 }}>
      {/* Top Notification Alerts */}
      {message && (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 10,
            padding: "12px 16px",
            backgroundColor: "#ecfdf5",
            border: "1px solid #10b981",
            borderRadius: 8,
            color: "#065f46",
            fontSize: 14
          }}
        >
          <CheckCircle2 size={18} color="#10b981" />
          <span>{message}</span>
        </div>
      )}
      {error && (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 10,
            padding: "12px 16px",
            backgroundColor: "#fef2f2",
            border: "1px solid #ef4444",
            borderRadius: 8,
            color: "#991b1b",
            fontSize: 14
          }}
        >
          <AlertCircle size={18} color="#ef4444" />
          <span>{error}</span>
        </div>
      )}

      {/* Hero Stats Card */}
      <section
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
          gap: 16
        }}
      >
        <div
          style={{
            background: "#fff",
            padding: 20,
            borderRadius: 12,
            border: "1px solid #e2e8f0",
            boxShadow: "0 1px 3px rgba(0,0,0,0.05)"
          }}
        >
          <div style={{ color: "#64748b", fontSize: 13, fontWeight: 500 }}>Total Services</div>
          <div style={{ fontSize: 26, fontWeight: 700, color: "#0f172a", marginTop: 4 }}>
            {servicesList.length}
          </div>
          <div style={{ fontSize: 12, color: "#10b981", marginTop: 4, fontWeight: 500 }}>
            {servicesWithImagesCount} with 3D clay images ({Math.round((servicesWithImagesCount / (servicesList.length || 1)) * 100)}%)
          </div>
        </div>

        <div
          style={{
            background: "#fff",
            padding: 20,
            borderRadius: 12,
            border: "1px solid #e2e8f0",
            boxShadow: "0 1px 3px rgba(0,0,0,0.05)"
          }}
        >
          <div style={{ color: "#64748b", fontSize: 13, fontWeight: 500 }}>Service Categories</div>
          <div style={{ fontSize: 26, fontWeight: 700, color: "#0f172a", marginTop: 4 }}>
            {categories.length}
          </div>
          <div style={{ fontSize: 12, color: "#0ea5e9", marginTop: 4, fontWeight: 500 }}>
            {categoriesWithImagesCount} with 3D icons ({Math.round((categoriesWithImagesCount / (categories.length || 1)) * 100)}%)
          </div>
        </div>

        <div
          style={{
            background: "#fff",
            padding: 20,
            borderRadius: 12,
            border: "1px solid #e2e8f0",
            boxShadow: "0 1px 3px rgba(0,0,0,0.05)"
          }}
        >
          <div style={{ color: "#64748b", fontSize: 13, fontWeight: 500 }}>App Payload Optimizer</div>
          <div style={{ fontSize: 20, fontWeight: 700, color: "#10b981", marginTop: 6 }}>
            ~25–35 KB / image
          </div>
          <div style={{ fontSize: 12, color: "#64748b", marginTop: 4 }}>
            Direct in-browser WebP 512×512 encoder
          </div>
        </div>

        <div
          style={{
            background: "#fff",
            padding: 20,
            borderRadius: 12,
            border: "1px solid #e2e8f0",
            boxShadow: "0 1px 3px rgba(0,0,0,0.05)"
          }}
        >
          <div style={{ color: "#64748b", fontSize: 13, fontWeight: 500 }}>Estimated Load Time</div>
          <div style={{ fontSize: 26, fontWeight: 700, color: "#0f172a", marginTop: 4 }}>
            &lt; 15 ms
          </div>
          <div style={{ fontSize: 12, color: "#64748b", marginTop: 4 }}>
            Fast 4G CDN edge delivery in India
          </div>
        </div>
      </section>

      {/* ChatGPT & Upload Instructions Guide */}
      <section
        style={{
          background: "#f8fafc",
          border: "1px solid #e2e8f0",
          borderRadius: 12,
          padding: 20
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 12 }}>
          <Sparkles size={20} color="#7c3aed" />
          <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: "#1e1b4b" }}>
            ChatGPT (DALL-E 3) Generation &amp; Upload Instructions
          </h3>
        </div>

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
            gap: 16,
            fontSize: 13,
            color: "#334155",
            lineHeight: 1.5
          }}
        >
          <div style={{ background: "#fff", padding: 14, borderRadius: 8, border: "1px solid #e2e8f0" }}>
            <strong style={{ color: "#0f172a", display: "block", marginBottom: 6 }}>
              1. Prompting ChatGPT
            </strong>
            Copy the prompt using the <b>&quot;Copy Prompt&quot;</b> button on any service card below. Paste it into ChatGPT.
            ChatGPT outputs a high-resolution 1024×1024 PNG (~800 KB).
          </div>

          <div style={{ background: "#fff", padding: 14, borderRadius: 8, border: "1px solid #e2e8f0" }}>
            <strong style={{ color: "#0f172a", display: "block", marginBottom: 6 }}>
              2. Drop Any Format Here (PNG / WebP / JPG)
            </strong>
            You do <b>not</b> need to convert manually! Drop the raw ChatGPT PNG into this upload box. Our browser engine instantly converts it to a <b>512×512 WebP (~25 KB)</b> with 95%+ size savings.
          </div>

          <div style={{ background: "#fff", padding: 14, borderRadius: 8, border: "1px solid #e2e8f0" }}>
            <strong style={{ color: "#0f172a", display: "block", marginBottom: 6 }}>
              3. Instant Mobile Cache &amp; Zero APK Bloat
            </strong>
            Once uploaded, the image updates in Firestore. All Android users instantly see the new 3D render in the app. Coil caches it on the phone disk permanently with 0ms offline retrieval.
          </div>
        </div>
      </section>

      {/* Tabs & Controls */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 12 }}>
        <div style={{ display: "flex", gap: 8 }}>
          <button
            onClick={() => setActiveTab("services")}
            style={{
              padding: "8px 18px",
              borderRadius: 8,
              border: "none",
              cursor: "pointer",
              fontWeight: 600,
              fontSize: 14,
              backgroundColor: activeTab === "services" ? "#0f172a" : "#e2e8f0",
              color: activeTab === "services" ? "#fff" : "#334155"
            }}
          >
            Services ({servicesList.length})
          </button>
          <button
            onClick={() => setActiveTab("categories")}
            style={{
              padding: "8px 18px",
              borderRadius: 8,
              border: "none",
              cursor: "pointer",
              fontWeight: 600,
              fontSize: 14,
              backgroundColor: activeTab === "categories" ? "#0f172a" : "#e2e8f0",
              color: activeTab === "categories" ? "#fff" : "#334155"
            }}
          >
            Categories ({categories.length})
          </button>
        </div>

        {activeTab === "services" && (
          <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <button
              onClick={() => setIsAddModalOpen(true)}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 6,
                padding: "8px 16px",
                borderRadius: 8,
                backgroundColor: "#10b981",
                color: "#fff",
                border: "none",
                fontWeight: 600,
                fontSize: 13,
                cursor: "pointer"
              }}
            >
              <Plus size={16} />
              Add Custom Service
            </button>
            <button
              onClick={() => void loadCatalog()}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 6,
                padding: "8px 12px",
                borderRadius: 8,
                backgroundColor: "#fff",
                border: "1px solid #cbd5e1",
                color: "#475569",
                fontSize: 13,
                cursor: "pointer"
              }}
            >
              <RefreshCw size={14} />
              Refresh
            </button>
          </div>
        )}

        {activeTab === "categories" && (
          <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <button
              onClick={() => setIsAddCategoryModalOpen(true)}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 6,
                padding: "8px 16px",
                borderRadius: 8,
                backgroundColor: "#10b981",
                color: "#fff",
                border: "none",
                fontWeight: 600,
                fontSize: 13,
                cursor: "pointer"
              }}
            >
              <Plus size={16} />
              Add Category
            </button>
            <button
              onClick={() => void loadCatalog()}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 6,
                padding: "8px 12px",
                borderRadius: 8,
                backgroundColor: "#fff",
                border: "1px solid #cbd5e1",
                color: "#475569",
                fontSize: 13,
                cursor: "pointer"
              }}
            >
              <RefreshCw size={14} />
              Refresh
            </button>
          </div>
        )}
      </div>

      {/* Search & Filters */}
      {activeTab === "services" && (
        <div
          style={{
            display: "flex",
            gap: 12,
            flexWrap: "wrap",
            alignItems: "center",
            background: "#fff",
            padding: 14,
            borderRadius: 10,
            border: "1px solid #e2e8f0"
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 8, flex: "1 1 240px" }}>
            <Search size={16} color="#64748b" />
            <input
              type="text"
              placeholder="Search by name, ID, or Telugu name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{
                width: "100%",
                padding: "8px 12px",
                border: "1px solid #cbd5e1",
                borderRadius: 6,
                fontSize: 13
              }}
            />
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span style={{ fontSize: 13, color: "#64748b", fontWeight: 500 }}>Category:</span>
            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              style={{
                padding: "8px 12px",
                border: "1px solid #cbd5e1",
                borderRadius: 6,
                fontSize: 13,
                backgroundColor: "#fff"
              }}
            >
              <option value="ALL">All Categories ({servicesList.length})</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name} ({servicesList.filter((s) => s.category.toUpperCase() === cat.id.toUpperCase()).length})
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span style={{ fontSize: 13, color: "#64748b", fontWeight: 500 }}>Image:</span>
            <select
              value={imageFilter}
              onChange={(e) => setImageFilter(e.target.value as any)}
              style={{
                padding: "8px 12px",
                border: "1px solid #cbd5e1",
                borderRadius: 6,
                fontSize: 13,
                backgroundColor: "#fff"
              }}
            >
              <option value="ALL">All Items</option>
              <option value="HAS_IMAGE">Has 3D Image ({servicesWithImagesCount})</option>
              <option value="NO_IMAGE">Missing Image ({servicesList.length - servicesWithImagesCount})</option>
            </select>
          </div>
        </div>
      )}

      {/* Loading Indicator */}
      {loading && (
        <div style={{ textAlign: "center", padding: "40px 0", color: "#64748b" }}>
          <RefreshCw size={24} className="spin" style={{ margin: "0 auto 8px auto" }} />
          <div>Loading catalog and cloud storage links...</div>
        </div>
      )}

      {/* SERVICES TAB */}
      {!loading && activeTab === "services" && (
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          {filteredServices.length === 0 && (
            <div style={{ textAlign: "center", padding: 48, background: "#fff", borderRadius: 12, border: "1px solid #e2e8f0" }}>
              <Info size={28} color="#94a3b8" style={{ margin: "0 auto 8px" }} />
              <div style={{ fontSize: 15, fontWeight: 600, color: "#334155" }}>No services found</div>
              <div style={{ fontSize: 13, color: "#64748b", marginTop: 4 }}>Try adjusting your search or category filter.</div>
            </div>
          )}

          {filteredServices.map((svc) => {
            const pending = pendingUploads[svc.id];
            const isUploading = uploadingId === svc.id;
            const hasCustomImage = Boolean(svc.imageUrl);

            return (
              <div
                key={svc.id}
                style={{
                  background: "#fff",
                  borderRadius: 12,
                  border: "1px solid #e2e8f0",
                  padding: 18,
                  display: "grid",
                  gridTemplateColumns: "100px 1fr 340px",
                  gap: 20,
                  alignItems: "start",
                  boxShadow: "0 1px 3px rgba(0,0,0,0.03)"
                }}
              >
                {/* 1. Mobile App Live Preview Box */}
                <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 6 }}>
                  <div
                    style={{
                      width: 84,
                      height: 84,
                      borderRadius: 14,
                      background: "#f8fafc",
                      border: "1px solid #e2e8f0",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      position: "relative",
                      overflow: "hidden"
                    }}
                  >
                    {pending?.previewUrl ? (
                      <img
                        src={pending.previewUrl}
                        alt="Preview"
                        style={{ width: "100%", height: "100%", objectFit: "contain" }}
                      />
                    ) : svc.imageUrl ? (
                      <img
                        src={svc.imageUrl}
                        alt={svc.name}
                        style={{ width: "100%", height: "100%", objectFit: "contain" }}
                        onError={(e) => {
                          (e.target as HTMLImageElement).style.display = "none";
                        }}
                      />
                    ) : (
                      <div style={{ textAlign: "center", padding: 6 }}>
                        <ImageIcon size={28} color="#94a3b8" />
                        <span style={{ fontSize: 9.5, color: "#94a3b8", display: "block", marginTop: 2 }}>
                          Vector Icon
                        </span>
                      </div>
                    )}

                    {pending && (
                      <span
                        style={{
                          position: "absolute",
                          bottom: 2,
                          background: "#3b82f6",
                          color: "#fff",
                          fontSize: 9,
                          fontWeight: 700,
                          padding: "1px 4px",
                          borderRadius: 4
                        }}
                      >
                        NEW
                      </span>
                    )}
                  </div>
                  <span style={{ fontSize: 10.5, color: "#64748b", fontWeight: 500, textAlign: "center" }}>
                    App Preview
                  </span>
                </div>

                {/* 2. Service Information & CRUD */}
                <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
                    <span
                      style={{
                        fontSize: 11,
                        fontWeight: 700,
                        padding: "2px 8px",
                        borderRadius: 4,
                        background: "#e0f2fe",
                        color: "#0369a1"
                      }}
                    >
                      {CATEGORY_NAMES[svc.category] || svc.category}
                    </span>
                    <span style={{ fontSize: 12, color: "#64748b", fontFamily: "monospace" }}>
                      ID: {svc.id}
                    </span>
                    {svc.active === false && (
                      <span
                        style={{
                          fontSize: 10.5,
                          fontWeight: 600,
                          padding: "2px 6px",
                          borderRadius: 4,
                          background: "#fee2e2",
                          color: "#b91c1c"
                        }}
                      >
                        OFFER PAUSED
                      </span>
                    )}
                  </div>

                  <div>
                    <h4 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: "#0f172a" }}>
                      {svc.name}
                    </h4>
                    {svc.te && (
                      <span style={{ fontSize: 13, color: "#475569", display: "block", marginTop: 2 }}>
                        {svc.te}
                      </span>
                    )}
                  </div>

                  <p style={{ margin: 0, fontSize: 12.5, color: "#64748b", lineHeight: 1.4 }}>
                    {svc.includes || "No special description."}
                  </p>

                  <div style={{ display: "flex", alignItems: "center", gap: 16, marginTop: 4 }}>
                    <span style={{ fontSize: 15, fontWeight: 700, color: "#0f172a" }}>
                      ₹{svc.price}
                    </span>
                    <span style={{ fontSize: 12, color: "#475569" }}>
                      ⏱ {svc.durationMin} mins
                    </span>
                    {svc.inspection && (
                      <span style={{ fontSize: 11, color: "#d97706", fontWeight: 600 }}>
                        • Inspection Visit
                      </span>
                    )}
                  </div>

                  {/* Copy Prompt Button for ChatGPT */}
                  <div style={{ marginTop: 6 }}>
                    <button
                      onClick={() => copyChatGptPrompt(svc.name, svc.id)}
                      style={{
                        display: "inline-flex",
                        alignItems: "center",
                        gap: 6,
                        background: "#f1f5f9",
                        border: "1px solid #cbd5e1",
                        padding: "5px 10px",
                        borderRadius: 6,
                        fontSize: 12,
                        color: "#334155",
                        cursor: "pointer",
                        fontWeight: 500
                      }}
                    >
                      {copiedPromptId === svc.id ? (
                        <>
                          <Check size={14} color="#10b981" />
                          <span style={{ color: "#10b981", fontWeight: 600 }}>Copied Prompt for ChatGPT!</span>
                        </>
                      ) : (
                        <>
                          <Copy size={14} />
                          <span>Copy ChatGPT Prompt</span>
                        </>
                      )}
                    </button>
                  </div>
                </div>

                {/* 3. Image Upload, Real-Time KB Meter & CRUD Actions */}
                <div
                  style={{
                    background: "#f8fafc",
                    border: "1px solid #e2e8f0",
                    borderRadius: 10,
                    padding: 14,
                    display: "flex",
                    flexDirection: "column",
                    gap: 10
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <span style={{ fontSize: 12, fontWeight: 700, color: "#0f172a" }}>
                      3D Clay Render Asset
                    </span>
                    {hasCustomImage && (
                      <span style={{ fontSize: 10.5, color: "#10b981", fontWeight: 600 }}>
                        Active on Cloud
                      </span>
                    )}
                  </div>

                  {/* If a new file is selected, show optimization meter */}
                  {pending ? (
                    <div
                      style={{
                        background: "#ecfdf5",
                        border: "1px solid #a7f3d0",
                        borderRadius: 8,
                        padding: 10,
                        fontSize: 12,
                        color: "#065f46"
                      }}
                    >
                      <div style={{ fontWeight: 700, marginBottom: 4 }}>Ready to Upload:</div>
                      <div>• Original Upload: <b>{pending.originalSizeKb} KB</b></div>
                      <div>• Optimized WebP: <b>{pending.webpSizeKb} KB</b> (saved {pending.savingsPct}%)</div>
                      <div>• Resolution: <b>{pending.width}×{pending.height} px</b></div>
                      <div>• In-App Load: <b>&lt; 15 ms on 4G</b></div>

                      <div style={{ display: "flex", gap: 8, marginTop: 10 }}>
                        <button
                          onClick={() => void uploadImageToStorage(svc.id, false)}
                          disabled={isUploading}
                          style={{
                            flex: 1,
                            padding: "7px 12px",
                            borderRadius: 6,
                            background: "#10b981",
                            color: "#fff",
                            border: "none",
                            fontWeight: 600,
                            fontSize: 12,
                            cursor: "pointer"
                          }}
                        >
                          {isUploading ? "Uploading..." : "Confirm & Save"}
                        </button>
                        <button
                          onClick={() => {
                            setPendingUploads((prev) => {
                              const next = { ...prev };
                              delete next[svc.id];
                              return next;
                            });
                          }}
                          disabled={isUploading}
                          style={{
                            padding: "7px 10px",
                            borderRadius: 6,
                            background: "#fff",
                            border: "1px solid #cbd5e1",
                            color: "#64748b",
                            fontSize: 12,
                            cursor: "pointer"
                          }}
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : (
                    <>
                      {/* Dropzone / Upload button */}
                      <label
                        style={{
                          border: "2px dashed #cbd5e1",
                          borderRadius: 8,
                          padding: "12px 10px",
                          textAlign: "center",
                          cursor: "pointer",
                          display: "block",
                          background: "#fff",
                          transition: "border 0.2s"
                        }}
                      >
                        <input
                          type="file"
                          accept="image/png, image/webp, image/jpeg"
                          style={{ display: "none" }}
                          onChange={(e) => {
                            const file = e.target.files?.[0];
                            if (file) handleFileSelect(svc.id, file);
                          }}
                        />
                        <Upload size={18} color="#64748b" style={{ margin: "0 auto 4px" }} />
                        <span style={{ fontSize: 11.5, color: "#334155", fontWeight: 600, display: "block" }}>
                          {hasCustomImage ? "Upload New / Replace" : "Select Image to Upload"}
                        </span>
                        <span style={{ fontSize: 10, color: "#94a3b8" }}>
                          PNG / WebP (Auto-optimized to 512×512)
                        </span>
                      </label>

                      {hasCustomImage && (
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                          <a
                            href={svc.imageUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            style={{
                              fontSize: 11,
                              color: "#0284c7",
                              display: "inline-flex",
                              alignItems: "center",
                              gap: 4,
                              textDecoration: "none"
                            }}
                          >
                            View Cloud File <ExternalLink size={12} />
                          </a>
                          <button
                            onClick={() => void removeCustomImage(svc.id, false)}
                            disabled={isUploading}
                            style={{
                              fontSize: 11,
                              color: "#ef4444",
                              background: "none",
                              border: "none",
                              cursor: "pointer",
                              padding: 0
                            }}
                          >
                            Revert to Vector Icon
                          </button>
                        </div>
                      )}
                    </>
                  )}

                  {/* Edit Price / Details Quick Action */}
                  <div style={{ borderTop: "1px solid #e2e8f0", paddingTop: 8, marginTop: 4 }}>
                    <button
                      onClick={() => setEditingItem(svc)}
                      style={{
                        width: "100%",
                        padding: "6px 10px",
                        borderRadius: 6,
                        background: "#fff",
                        border: "1px solid #cbd5e1",
                        fontSize: 12,
                        fontWeight: 600,
                        color: "#334155",
                        cursor: "pointer"
                      }}
                    >
                      Edit Price &amp; Info
                    </button>
                    {svc.isCustom && (
                      <button
                        onClick={() => void deleteCustomService(svc.id)}
                        disabled={saving}
                        style={{
                          width: "100%",
                          marginTop: 6,
                          padding: "6px 10px",
                          borderRadius: 6,
                          background: "#fff",
                          border: "1px solid #fca5a5",
                          fontSize: 12,
                          fontWeight: 600,
                          color: "#dc2626",
                          cursor: "pointer",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          gap: 6
                        }}
                      >
                        <Trash2 size={13} />
                        Delete Service
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* CATEGORIES TAB */}
      {!loading && activeTab === "categories" && (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(360px, 1fr))",
            gap: 16
          }}
        >
          {categories.map((cat) => {
            const pending = pendingUploads[cat.id];
            const isUploading = uploadingId === cat.id;
            const hasCustomImage = Boolean(cat.imageUrl);
            const count = servicesList.filter((s) => s.category === cat.id).length;

            return (
              <div
                key={cat.id}
                style={{
                  background: "#fff",
                  borderRadius: 12,
                  border: "1px solid #e2e8f0",
                  padding: 18,
                  display: "flex",
                  flexDirection: "column",
                  gap: 14,
                  boxShadow: "0 1px 3px rgba(0,0,0,0.03)"
                }}
              >
                <div style={{ display: "flex", gap: 14, alignItems: "center" }}>
                  <div
                    style={{
                      width: 64,
                      height: 64,
                      borderRadius: 14,
                      background: "#f8fafc",
                      border: "1px solid #e2e8f0",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      overflow: "hidden"
                    }}
                  >
                    {pending?.previewUrl ? (
                      <img
                        src={pending.previewUrl}
                        alt="Preview"
                        style={{ width: "100%", height: "100%", objectFit: "contain" }}
                      />
                    ) : cat.imageUrl ? (
                      <img
                        src={cat.imageUrl}
                        alt={cat.name}
                        style={{ width: "100%", height: "100%", objectFit: "contain" }}
                      />
                    ) : (
                      <ImageIcon size={26} color="#94a3b8" />
                    )}
                  </div>

                  <div style={{ flex: 1 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                      <span
                        style={{
                          fontSize: 10,
                          fontWeight: 700,
                          padding: "2px 6px",
                          borderRadius: 4,
                          background: cat.skill === "BASIC" ? "#dcfce7" : "#fef3c7",
                          color: cat.skill === "BASIC" ? "#166534" : "#92400e"
                        }}
                      >
                        {cat.skill}
                      </span>
                      <span style={{ fontSize: 11, color: "#64748b", fontFamily: "monospace" }}>
                        {cat.id}
                      </span>
                    </div>
                    <h4 style={{ margin: "4px 0 0", fontSize: 16, fontWeight: 700, color: "#0f172a" }}>
                      {cat.name}
                    </h4>
                    <span style={{ fontSize: 12, color: "#64748b" }}>
                      {cat.te} · {count} services
                    </span>
                  </div>
                </div>

                {/* ChatGPT Prompt Copy */}
                <div>
                  <button
                    onClick={() => copyChatGptPrompt(cat.name, cat.id)}
                    style={{
                      width: "100%",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      gap: 6,
                      background: "#f8fafc",
                      border: "1px solid #cbd5e1",
                      padding: "6px 10px",
                      borderRadius: 6,
                      fontSize: 12,
                      color: "#334155",
                      cursor: "pointer",
                      fontWeight: 500
                    }}
                  >
                    {copiedPromptId === cat.id ? (
                      <>
                        <Check size={14} color="#10b981" />
                        <span style={{ color: "#10b981", fontWeight: 600 }}>Copied Prompt for ChatGPT!</span>
                      </>
                    ) : (
                      <>
                        <Copy size={14} />
                        <span>Copy ChatGPT Prompt</span>
                      </>
                    )}
                  </button>
                </div>

                {/* Upload Section */}
                <div style={{ borderTop: "1px solid #e2e8f0", paddingTop: 10 }}>
                  {pending ? (
                    <div
                      style={{
                        background: "#ecfdf5",
                        border: "1px solid #a7f3d0",
                        borderRadius: 8,
                        padding: 10,
                        fontSize: 12,
                        color: "#065f46"
                      }}
                    >
                      <div style={{ fontWeight: 700, marginBottom: 4 }}>WebP Optimization:</div>
                      <div>• Upload: <b>{pending.originalSizeKb} KB</b> → WebP: <b>{pending.webpSizeKb} KB</b></div>
                      <div>• Saved: <b>{pending.savingsPct}%</b></div>
                      <div style={{ display: "flex", gap: 8, marginTop: 10 }}>
                        <button
                          onClick={() => void uploadImageToStorage(cat.id, true)}
                          disabled={isUploading}
                          style={{
                            flex: 1,
                            padding: "6px 12px",
                            borderRadius: 6,
                            background: "#10b981",
                            color: "#fff",
                            border: "none",
                            fontWeight: 600,
                            fontSize: 12,
                            cursor: "pointer"
                          }}
                        >
                          {isUploading ? "Uploading..." : "Save WebP"}
                        </button>
                        <button
                          onClick={() => {
                            setPendingUploads((prev) => {
                              const next = { ...prev };
                              delete next[cat.id];
                              return next;
                            });
                          }}
                          disabled={isUploading}
                          style={{
                            padding: "6px 10px",
                            borderRadius: 6,
                            background: "#fff",
                            border: "1px solid #cbd5e1",
                            color: "#64748b",
                            fontSize: 12,
                            cursor: "pointer"
                          }}
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div>
                      <label
                        style={{
                          border: "1.5px dashed #cbd5e1",
                          borderRadius: 6,
                          padding: "8px 10px",
                          textAlign: "center",
                          cursor: "pointer",
                          display: "block",
                          background: "#f8fafc"
                        }}
                      >
                        <input
                          type="file"
                          accept="image/png, image/webp, image/jpeg"
                          style={{ display: "none" }}
                          onChange={(e) => {
                            const file = e.target.files?.[0];
                            if (file) handleFileSelect(cat.id, file);
                          }}
                        />
                        <span style={{ fontSize: 11.5, color: "#334155", fontWeight: 600 }}>
                          {hasCustomImage ? "Replace 3D Category Image" : "Upload Category 3D Image"}
                        </span>
                      </label>
                      {hasCustomImage && (
                        <div style={{ display: "flex", justifyContent: "space-between", marginTop: 6 }}>
                          <a
                            href={cat.imageUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            style={{ fontSize: 11, color: "#0284c7", textDecoration: "none" }}
                          >
                            View Cloud URL
                          </a>
                          <button
                            onClick={() => void removeCustomImage(cat.id, true)}
                            style={{ fontSize: 11, color: "#ef4444", background: "none", border: "none", cursor: "pointer" }}
                          >
                            Revert
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* Delete Custom Category Button */}
                {!DEFAULT_CATEGORY_IDS.has(cat.id) && (
                  <div style={{ borderTop: "1px solid #fee2e2", paddingTop: 8 }}>
                    <button
                      onClick={() => void deleteCustomCategory(cat.id)}
                      disabled={saving}
                      style={{
                        width: "100%",
                        padding: "6px 10px",
                        borderRadius: 6,
                        background: "#fff",
                        border: "1px solid #fca5a5",
                        fontSize: 12,
                        fontWeight: 600,
                        color: "#dc2626",
                        cursor: "pointer",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        gap: 6
                      }}
                    >
                      <Trash2 size={13} />
                      Delete Category
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* EDIT SERVICE DETAILS MODAL */}
      {editingItem && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            backgroundColor: "rgba(0,0,0,0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
            padding: 20
          }}
        >
          <div
            style={{
              background: "#fff",
              borderRadius: 12,
              padding: 24,
              maxWidth: 580,
              width: "100%",
              maxHeight: "90vh",
              overflowY: "auto",
              boxShadow: "0 20px 25px -5px rgba(0,0,0,0.1)"
            }}
          >
            <h3 style={{ margin: "0 0 16px", fontSize: 18, fontWeight: 700, color: "#0f172a" }}>
              Edit Service: {editingItem.name}
            </h3>

            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>English Name</label>
                <input
                  type="text"
                  value={editingItem.name}
                  onChange={(e) => setEditingItem({ ...editingItem, name: e.target.value })}
                  style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                />
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 12 }}>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Base Price (₹)</label>
                  <input
                    type="number"
                    value={editingItem.price}
                    onChange={(e) => setEditingItem({ ...editingItem, price: Number(e.target.value) })}
                    style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>MRP / Strike (₹)</label>
                  <input
                    type="number"
                    value={editingItem.originalPrice || ""}
                    placeholder={String(Math.round(editingItem.price * 1.25))}
                    onChange={(e) => setEditingItem({ ...editingItem, originalPrice: Number(e.target.value) || undefined })}
                    style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Duration (Mins)</label>
                  <input
                    type="number"
                    value={editingItem.durationMin}
                    onChange={(e) => setEditingItem({ ...editingItem, durationMin: Number(e.target.value) })}
                    style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  />
                </div>
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Telugu Label</label>
                <input
                  type="text"
                  value={editingItem.te || ""}
                  onChange={(e) => setEditingItem({ ...editingItem, te: e.target.value })}
                  style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                />
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Includes / Description</label>
                <textarea
                  rows={2}
                  value={editingItem.includes || ""}
                  onChange={(e) => setEditingItem({ ...editingItem, includes: e.target.value })}
                  style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                />
              </div>

              {/* Service Variants & Options Builder */}
              <div style={{ borderTop: "1px solid #e2e8f0", paddingTop: 12 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                  <div>
                    <span style={{ fontSize: 13, fontWeight: 700, color: "#0f172a" }}>Service Variants / Options</span>
                    <p style={{ margin: "2px 0 0", fontSize: 11, color: "#64748b" }}>
                      E.g. 1 Split AC (₹499), 2 Split ACs (₹899). If configured, app offers a choice bottom sheet!
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      const currentOpts = editingItem.options || [];
                      const nextIdx = currentOpts.length + 1;
                      const newOpt: ServiceOptionDef = {
                        id: `${editingItem.id}_opt_${nextIdx}`,
                        title: `${editingItem.name} - Option ${nextIdx}`,
                        price: editingItem.price,
                        originalPrice: editingItem.originalPrice,
                        durationMin: editingItem.durationMin
                      };
                      setEditingItem({ ...editingItem, options: [...currentOpts, newOpt] });
                    }}
                    style={{
                      padding: "4px 8px",
                      borderRadius: 6,
                      background: "#eff6ff",
                      color: "#2563eb",
                      border: "1px solid #bfdbfe",
                      fontSize: 11.5,
                      fontWeight: 600,
                      cursor: "pointer",
                      display: "inline-flex",
                      alignItems: "center",
                      gap: 4
                    }}
                  >
                    <Plus size={13} /> Add Variant
                  </button>
                </div>

                {(editingItem.options || []).length === 0 ? (
                  <div style={{ background: "#f8fafc", padding: 10, borderRadius: 6, fontSize: 11.5, color: "#94a3b8", textAlign: "center" }}>
                    Standard fixed price service (no multiple options configured).
                  </div>
                ) : (
                  <div style={{ display: "flex", flexDirection: "column", gap: 8, maxHeight: 180, overflowY: "auto" }}>
                    {(editingItem.options || []).map((opt, idx) => (
                      <div
                        key={opt.id || idx}
                        style={{
                          display: "grid",
                          gridTemplateColumns: "2fr 1fr 1fr 28px",
                          gap: 6,
                          alignItems: "center",
                          background: "#f8fafc",
                          padding: "6px 8px",
                          borderRadius: 6,
                          border: "1px solid #e2e8f0"
                        }}
                      >
                        <input
                          type="text"
                          placeholder="Option Title (e.g. 2 Units)"
                          value={opt.title}
                          onChange={(e) => {
                            const updated = [...(editingItem.options || [])];
                            updated[idx] = { ...updated[idx], title: e.target.value };
                            setEditingItem({ ...editingItem, options: updated });
                          }}
                          style={{ padding: "4px 6px", fontSize: 12, borderRadius: 4, border: "1px solid #cbd5e1" }}
                        />
                        <input
                          type="number"
                          placeholder="Price"
                          value={opt.price}
                          onChange={(e) => {
                            const updated = [...(editingItem.options || [])];
                            updated[idx] = { ...updated[idx], price: Number(e.target.value) };
                            setEditingItem({ ...editingItem, options: updated });
                          }}
                          style={{ padding: "4px 6px", fontSize: 12, borderRadius: 4, border: "1px solid #cbd5e1" }}
                        />
                        <input
                          type="number"
                          placeholder="MRP"
                          value={opt.originalPrice || ""}
                          onChange={(e) => {
                            const updated = [...(editingItem.options || [])];
                            updated[idx] = { ...updated[idx], originalPrice: Number(e.target.value) || undefined };
                            setEditingItem({ ...editingItem, options: updated });
                          }}
                          style={{ padding: "4px 6px", fontSize: 12, borderRadius: 4, border: "1px solid #cbd5e1" }}
                        />
                        <button
                          type="button"
                          onClick={() => {
                            const updated = (editingItem.options || []).filter((_, i) => i !== idx);
                            setEditingItem({ ...editingItem, options: updated });
                          }}
                          style={{ background: "none", border: "none", color: "#ef4444", cursor: "pointer", padding: 2 }}
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div style={{ display: "flex", alignItems: "center", gap: 10, marginTop: 4 }}>
                <input
                  type="checkbox"
                  id="activeCheck"
                  checked={editingItem.active !== false}
                  onChange={(e) => setEditingItem({ ...editingItem, active: e.target.checked })}
                />
                <label htmlFor="activeCheck" style={{ fontSize: 13, fontWeight: 600, color: "#0f172a" }}>
                  Active (Offered to customers in app)
                </label>
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Custom Image URL (CDN / Storage)</label>
                <input
                  type="text"
                  value={editingItem.imageUrl || ""}
                  onChange={(e) => setEditingItem({ ...editingItem, imageUrl: e.target.value })}
                  placeholder="https://..."
                  style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 12 }}
                />
              </div>
            </div>

            <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 20 }}>
              <button
                onClick={() => setEditingItem(null)}
                style={{
                  padding: "8px 16px",
                  borderRadius: 6,
                  border: "1px solid #cbd5e1",
                  background: "#fff",
                  color: "#64748b",
                  cursor: "pointer"
                }}
              >
                Cancel
              </button>
              <button
                onClick={() => void saveServiceDetails(editingItem)}
                disabled={saving}
                style={{
                  padding: "8px 18px",
                  borderRadius: 6,
                  border: "none",
                  background: "#0f172a",
                  color: "#fff",
                  fontWeight: 600,
                  cursor: "pointer"
                }}
              >
                {saving ? "Saving..." : "Save Changes"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* CREATE CUSTOM SERVICE MODAL */}
      {isAddModalOpen && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            backgroundColor: "rgba(0,0,0,0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
            padding: 20
          }}
        >
          <div
            style={{
              background: "#fff",
              borderRadius: 12,
              padding: 24,
              maxWidth: 580,
              width: "100%",
              maxHeight: "90vh",
              overflowY: "auto",
              boxShadow: "0 20px 25px -5px rgba(0,0,0,0.1)"
            }}
          >
            <h3 style={{ margin: "0 0 16px", fontSize: 18, fontWeight: 700, color: "#0f172a" }}>
              Add New Home Service
            </h3>

            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Category</label>
                  <select
                    value={newService.category}
                    onChange={(e) => setNewService({ ...newService, category: e.target.value })}
                    style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  >
                    {categories.map((cat) => (
                      <option key={cat.id} value={cat.id}>
                        {cat.name} ({cat.id})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Unique ID (e.g. clean_garden)</label>
                  <input
                    type="text"
                    value={newService.id || ""}
                    onChange={(e) => setNewService({ ...newService, id: e.target.value })}
                    placeholder="e.g. clean_window"
                    style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  />
                </div>
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>English Name</label>
                <input
                  type="text"
                  value={newService.name || ""}
                  onChange={(e) => setNewService({ ...newService, name: e.target.value })}
                  placeholder="e.g. Balcony Pressure Wash"
                  style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                />
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 12 }}>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Offer Price (₹)</label>
                  <input
                    type="number"
                    value={newService.price}
                    onChange={(e) => setNewService({ ...newService, price: Number(e.target.value) })}
                    style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>MRP / Strike (₹)</label>
                  <input
                    type="number"
                    value={newService.originalPrice || ""}
                    placeholder={String(Math.round((newService.price || 299) * 1.25))}
                    onChange={(e) => setNewService({ ...newService, originalPrice: Number(e.target.value) || undefined })}
                    style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Duration (Mins)</label>
                  <input
                    type="number"
                    value={newService.durationMin}
                    onChange={(e) => setNewService({ ...newService, durationMin: Number(e.target.value) })}
                    style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  />
                </div>
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Telugu Label</label>
                <input
                  type="text"
                  value={newService.te || ""}
                  onChange={(e) => setNewService({ ...newService, te: e.target.value })}
                  placeholder="e.g. బాల్కనీ ప్రెషర్ వాష్"
                  style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                />
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Includes / Details</label>
                <textarea
                  rows={2}
                  value={newService.includes || ""}
                  onChange={(e) => setNewService({ ...newService, includes: e.target.value })}
                  placeholder="What is included in this service..."
                  style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                />
              </div>

              {/* Service Variants & Options Builder */}
              <div style={{ borderTop: "1px solid #e2e8f0", paddingTop: 12 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                  <div>
                    <span style={{ fontSize: 13, fontWeight: 700, color: "#0f172a" }}>Service Variants &amp; Options</span>
                    <p style={{ margin: "2px 0 0", fontSize: 11, color: "#64748b" }}>
                      E.g. 1 Unit (₹299), 2 Units (₹499). If configured, app offers a choice bottom sheet!
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      const currentOpts = newService.options || [];
                      const nextIdx = currentOpts.length + 1;
                      const svcId = newService.id || "svc";
                      const newOpt: ServiceOptionDef = {
                        id: `${svcId}_opt_${nextIdx}`,
                        title: `Option ${nextIdx}`,
                        price: newService.price || 299,
                        originalPrice: newService.originalPrice,
                        durationMin: newService.durationMin || 60
                      };
                      setNewService({ ...newService, options: [...currentOpts, newOpt] });
                    }}
                    style={{
                      padding: "4px 8px",
                      borderRadius: 6,
                      background: "#eff6ff",
                      color: "#2563eb",
                      border: "1px solid #bfdbfe",
                      fontSize: 11.5,
                      fontWeight: 600,
                      cursor: "pointer",
                      display: "inline-flex",
                      alignItems: "center",
                      gap: 4
                    }}
                  >
                    <Plus size={13} /> Add Variant
                  </button>
                </div>

                {(newService.options || []).length === 0 ? (
                  <div style={{ background: "#f8fafc", padding: 10, borderRadius: 6, fontSize: 11.5, color: "#94a3b8", textAlign: "center" }}>
                    Standard fixed price service (no multiple options configured).
                  </div>
                ) : (
                  <div style={{ display: "flex", flexDirection: "column", gap: 8, maxHeight: 180, overflowY: "auto" }}>
                    {(newService.options || []).map((opt, idx) => (
                      <div
                        key={opt.id || idx}
                        style={{
                          display: "grid",
                          gridTemplateColumns: "2fr 1fr 1fr 28px",
                          gap: 6,
                          alignItems: "center",
                          background: "#f8fafc",
                          padding: "6px 8px",
                          borderRadius: 6,
                          border: "1px solid #e2e8f0"
                        }}
                      >
                        <input
                          type="text"
                          placeholder="Option Title (e.g. 2 Units)"
                          value={opt.title}
                          onChange={(e) => {
                            const updated = [...(newService.options || [])];
                            updated[idx] = { ...updated[idx], title: e.target.value };
                            setNewService({ ...newService, options: updated });
                          }}
                          style={{ padding: "4px 6px", fontSize: 12, borderRadius: 4, border: "1px solid #cbd5e1" }}
                        />
                        <input
                          type="number"
                          placeholder="Price"
                          value={opt.price}
                          onChange={(e) => {
                            const updated = [...(newService.options || [])];
                            updated[idx] = { ...updated[idx], price: Number(e.target.value) };
                            setNewService({ ...newService, options: updated });
                          }}
                          style={{ padding: "4px 6px", fontSize: 12, borderRadius: 4, border: "1px solid #cbd5e1" }}
                        />
                        <input
                          type="number"
                          placeholder="MRP"
                          value={opt.originalPrice || ""}
                          onChange={(e) => {
                            const updated = [...(newService.options || [])];
                            updated[idx] = { ...updated[idx], originalPrice: Number(e.target.value) || undefined };
                            setNewService({ ...newService, options: updated });
                          }}
                          style={{ padding: "4px 6px", fontSize: 12, borderRadius: 4, border: "1px solid #cbd5e1" }}
                        />
                        <button
                          type="button"
                          onClick={() => {
                            const updated = (newService.options || []).filter((_, i) => i !== idx);
                            setNewService({ ...newService, options: updated });
                          }}
                          style={{ background: "none", border: "none", color: "#ef4444", cursor: "pointer", padding: 2 }}
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 20 }}>
              <button
                onClick={() => setIsAddModalOpen(false)}
                style={{
                  padding: "8px 16px",
                  borderRadius: 6,
                  border: "1px solid #cbd5e1",
                  background: "#fff",
                  color: "#64748b",
                  cursor: "pointer"
                }}
              >
                Cancel
              </button>
              <button
                onClick={() => void createCustomService()}
                disabled={saving}
                style={{
                  padding: "8px 18px",
                  borderRadius: 6,
                  border: "none",
                  background: "#10b981",
                  color: "#fff",
                  fontWeight: 600,
                  cursor: "pointer"
                }}
              >
                {saving ? "Creating..." : "Create Service"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* CREATE CUSTOM CATEGORY MODAL */}
      {isAddCategoryModalOpen && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            backgroundColor: "rgba(0,0,0,0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
            padding: 20
          }}
        >
          <div
            style={{
              background: "#fff",
              borderRadius: 12,
              padding: 24,
              maxWidth: 520,
              width: "100%",
              boxShadow: "0 20px 25px -5px rgba(0,0,0,0.1)"
            }}
          >
            <h3 style={{ margin: "0 0 16px", fontSize: 18, fontWeight: 700, color: "#0f172a" }}>
              Add New Service Category
            </h3>

            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Category ID (e.g. PEST_CONTROL)</label>
                  <input
                    type="text"
                    value={newCategory.id || ""}
                    onChange={(e) => setNewCategory({ ...newCategory, id: e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, "_") })}
                    placeholder="e.g. PEST_CONTROL"
                    style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  />
                </div>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Skill Level</label>
                  <select
                    value={newCategory.skill || "SKILLED"}
                    onChange={(e) => setNewCategory({ ...newCategory, skill: e.target.value as "BASIC" | "SKILLED" })}
                    style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                  >
                    <option value="BASIC">BASIC (Helpers, Cleaning, Movers)</option>
                    <option value="SKILLED">SKILLED (Technicians, Plumbers, Trades)</option>
                  </select>
                </div>
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Category Name (English)</label>
                <input
                  type="text"
                  value={newCategory.name || ""}
                  onChange={(e) => setNewCategory({ ...newCategory, name: e.target.value })}
                  placeholder="e.g. Pest Control &amp; Disinfection"
                  style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                />
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Telugu Label</label>
                <input
                  type="text"
                  value={newCategory.te || ""}
                  onChange={(e) => setNewCategory({ ...newCategory, te: e.target.value })}
                  placeholder="e.g. కీటకాల నియంత్రణ"
                  style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                />
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Hindi Label (Optional)</label>
                <input
                  type="text"
                  value={newCategory.hi || ""}
                  onChange={(e) => setNewCategory({ ...newCategory, hi: e.target.value })}
                  placeholder="e.g. कीट नियंत्रण"
                  style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1" }}
                />
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: "#475569" }}>Optional Image / CDN URL</label>
                <input
                  type="text"
                  value={newCategory.imageUrl || ""}
                  onChange={(e) => setNewCategory({ ...newCategory, imageUrl: e.target.value })}
                  placeholder="https://... (or upload from category card after creating)"
                  style={{ width: "100%", padding: "8px 10px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 12 }}
                />
              </div>
            </div>

            <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 20 }}>
              <button
                onClick={() => setIsAddCategoryModalOpen(false)}
                style={{
                  padding: "8px 16px",
                  borderRadius: 6,
                  border: "1px solid #cbd5e1",
                  background: "#fff",
                  color: "#64748b",
                  cursor: "pointer"
                }}
              >
                Cancel
              </button>
              <button
                onClick={() => void createCustomCategory()}
                disabled={saving}
                style={{
                  padding: "8px 18px",
                  borderRadius: 6,
                  border: "none",
                  background: "#10b981",
                  color: "#fff",
                  fontWeight: 600,
                  cursor: "pointer"
                }}
              >
                {saving ? "Creating..." : "Create Category"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
