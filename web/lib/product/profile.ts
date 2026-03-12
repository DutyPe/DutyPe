export type ProductRole = "WORKER" | "EMPLOYER";

export type ProductWorkLocation = {
  addedAt?: number;
  address?: string;
  id?: string;
  label?: string;
  latitude?: number;
  longitude?: number;
  usageCount?: number;
};

export type ProductUserProfile = {
  id?: string;
  activeRole?: string;
  address?: string;
  bio?: string;
  businessAddress?: string;
  businessLatitude?: number;
  businessLongitude?: number;
  companyName?: string;
  companySize?: string;
  contactEmail?: string;
  contactPhone?: string;
  createdAt?: unknown;
  currentLocationAddress?: string;
  currentLocationLabel?: string;
  dateOfBirth?: string;
  email?: string | null;
  experience?: string;
  fullName?: string;
  gender?: string;
  industry?: string;
  isActive?: boolean;
  latitude?: number;
  longitude?: number;
  name?: string;
  phone?: string;
  profileCompleted?: boolean;
  profileImageUrl?: string | null;
  referralCode?: string | null;
  role?: string;
  roles?: string[];
  savedJobs?: string[];
  skills?: string;
  trustTier?: string;
  updatedAt?: unknown;
  website?: string;
  workLocations?: ProductWorkLocation[];
};

export const PRODUCT_ROLES: ProductRole[] = ["WORKER", "EMPLOYER"];

export function normalizeProductRole(value: unknown): ProductRole | null {
  if (typeof value !== "string") {
    return null;
  }

  const normalized = value.trim().toUpperCase();
  return normalized === "WORKER" || normalized === "EMPLOYER" ? normalized : null;
}

export function extractProductRoles(profile: ProductUserProfile | null | undefined): ProductRole[] {
  const candidates = [
    normalizeProductRole(profile?.activeRole),
    normalizeProductRole(profile?.role),
    ...(Array.isArray(profile?.roles) ? profile.roles.map(normalizeProductRole) : [])
  ];

  return [...new Set(candidates.filter(Boolean) as ProductRole[])];
}

export function getActiveProductRole(profile: ProductUserProfile | null | undefined): ProductRole | null {
  const activeRole = normalizeProductRole(profile?.activeRole);

  if (activeRole) {
    return activeRole;
  }

  const roles = extractProductRoles(profile);
  return roles[0] ?? null;
}

export function productRoleLabel(role: ProductRole) {
  return role === "WORKER" ? "Worker" : "Employer";
}

export function productRolePath(role: ProductRole) {
  return role === "WORKER" ? "/app/worker" : "/app/employer";
}

export function displayProfileName(profile: ProductUserProfile | null | undefined) {
  return (
    profile?.fullName?.trim() ||
    profile?.name?.trim() ||
    profile?.companyName?.trim() ||
    profile?.email?.trim() ||
    "DutyPe user"
  );
}
