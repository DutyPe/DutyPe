const ADMIN_DOMAIN = "@dutype.com";
const ADMIN_ROLE = "ADMIN";

const LEGACY_ADMIN_EMAILS = new Set([
  "admin@dutype.com",
  "vamsi@dutype.com",
  "vamsib298@gmail.com",
  "dutpyein@gmail.com"
]);

export const ADMIN_POLICY_SUMMARY =
  "Firebase custom claims (`admin: true` or an `ADMIN` role claim), or the legacy admin email allowlist";

type AdminAuthorizationSource = "custom_claims" | "legacy_email_allowlist";

type AdminIdentityLike = {
  activeRole?: unknown;
  admin?: unknown;
  email?: string | null | undefined;
  role?: unknown;
  roles?: unknown;
};

function normalizeEmail(email: string | null | undefined) {
  return email?.trim().toLowerCase() ?? "";
}

export function isAdminEmail(email: string | null | undefined) {
  const normalized = normalizeEmail(email);

  if (!normalized) {
    return false;
  }

  return normalized.endsWith(ADMIN_DOMAIN) || LEGACY_ADMIN_EMAILS.has(normalized);
}

function normalizeRole(value: unknown) {
  return typeof value === "string" ? value.trim().toUpperCase() : "";
}

function normalizeRoles(value: unknown) {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .filter((item): item is string => typeof item === "string")
    .map((item) => item.trim().toUpperCase())
    .filter(Boolean);
}

export function hasAdminCustomClaims(identity: AdminIdentityLike | null | undefined) {
  if (!identity) {
    return false;
  }

  if (identity.admin === true) {
    return true;
  }

  if (normalizeRole(identity.role) === ADMIN_ROLE) {
    return true;
  }

  if (normalizeRole(identity.activeRole) === ADMIN_ROLE) {
    return true;
  }

  return normalizeRoles(identity.roles).includes(ADMIN_ROLE);
}

export function getAdminAuthorization(identity: AdminIdentityLike | null | undefined): {
  isAuthorized: boolean;
  matchedBy: AdminAuthorizationSource | null;
} {
  if (hasAdminCustomClaims(identity)) {
    return {
      isAuthorized: true,
      matchedBy: "custom_claims"
    };
  }

  if (isAdminEmail(identity?.email)) {
    return {
      isAuthorized: true,
      matchedBy: "legacy_email_allowlist"
    };
  }

  return {
    isAuthorized: false,
    matchedBy: null
  };
}

export function formatAdminAuthorizationSource(
  source: AdminAuthorizationSource | null | undefined
) {
  switch (source) {
    case "custom_claims":
      return "Firebase custom claims";
    case "legacy_email_allowlist":
      return "legacy admin email allowlist";
    default:
      return "unknown admin policy";
  }
}
