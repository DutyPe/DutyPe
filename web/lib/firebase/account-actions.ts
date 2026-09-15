import type { User } from "firebase/auth";
import { doc, runTransaction, type Firestore } from "firebase/firestore";

import { extractProductRoles, type ProductRole } from "@/lib/product/profile";

export function normalizeSignInPhone(value: string): string | null {
  const compact = value.trim().replace(/[\s()-]/g, "");
  if (/^[6-9]\d{9}$/.test(compact)) return `+91${compact}`;
  if (/^91[6-9]\d{9}$/.test(compact)) return `+${compact}`;
  return /^\+[1-9]\d{7,14}$/.test(compact) ? compact : null;
}

export async function ensureProductAccount(db: Firestore, user: User, role: ProductRole) {
  if (!user.uid) throw new Error("An authenticated account is required.");
  const userRef = doc(db, "users", user.uid);

  await runTransaction(db, async (transaction) => {
    const accountSnapshot = await transaction.get(userRef);
    const existing = accountSnapshot.data() ?? {};
    const now = Date.now();
    const fullName = existing.fullName || existing.name || user.displayName || "";
    const email = existing.email || user.email || "";
    const phone = existing.phone || user.phoneNumber || "";
    const roles = [...new Set([...extractProductRoles(existing), role])];

    if (accountSnapshot.exists()) {
      transaction.update(userRef, {
        roles, role, activeRole: role, lastLoginAt: now, updatedAt: now,
        ...(!existing.fullName && fullName ? { fullName, name: fullName } : {}),
        ...(!existing.email && email ? { email } : {}),
        ...(!existing.phone && phone ? { phone } : {})
      });
    } else {
      transaction.set(userRef, {
        id: user.uid, roles, role, activeRole: role,
        fullName, name: fullName, email, phone,
        contactEmail: email, contactPhone: phone,
        companyName: "", businessAddress: "", address: "",
        latitude: 0, longitude: 0, businessLatitude: 0, businessLongitude: 0,
        profileImageUrl: user.photoURL || "", profileCompleted: false,
        savedJobs: [], workLocations: [], trustTier: "NEW", isActive: true,
        createdAt: now, lastLoginAt: now, updatedAt: now
      });
    }

  });
}