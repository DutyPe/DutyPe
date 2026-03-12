import {
  collection,
  getDocs,
  limit,
  query,
  type Firestore,
  where
} from "firebase/firestore";

export type ProductVerificationStatus = "PENDING" | "VERIFIED" | "EXPIRED" | "CANCELLED";

export type ProductVerificationLocation = {
  lat: number;
  lng: number;
};

export type ProductWorkVerification = {
  applicationId: string;
  employerId: string;
  employerName: string;
  expiresAt: number;
  generatedAt: number;
  jobId: string;
  jobTitle: string;
  qrCodeData: string;
  status: ProductVerificationStatus;
  verificationCode: string;
  verificationId: string;
  verifiedAt: number | null;
  verifiedByEmployerId: string | null;
  verifiedLocation: ProductVerificationLocation | null;
  workerId: string;
  workerName: string;
};

type CreatePendingWorkVerificationInput = {
  applicationId: string;
  db: Firestore;
  employerId: string;
  employerName: string;
  jobId: string;
  jobTitle: string;
  workerId: string;
  workerName: string;
};

const verificationStatuses: ProductVerificationStatus[] = [
  "PENDING",
  "VERIFIED",
  "EXPIRED",
  "CANCELLED"
];

function stringValue(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}

function numberValue(value: unknown, fallback = 0): number {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === "string" && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
  }

  return fallback;
}

function statusValue(value: unknown): ProductVerificationStatus {
  const candidate = typeof value === "string" ? value.trim().toUpperCase() : "";
  return verificationStatuses.includes(candidate as ProductVerificationStatus)
    ? (candidate as ProductVerificationStatus)
    : "PENDING";
}

function locationValue(value: unknown): ProductVerificationLocation | null {
  if (!value || typeof value !== "object") {
    return null;
  }

  const payload = value as Record<string, unknown>;
  const lat = numberValue(payload.lat ?? payload.latitude);
  const lng = numberValue(payload.lng ?? payload.longitude);

  if (!Number.isFinite(lat) || !Number.isFinite(lng) || (lat === 0 && lng === 0)) {
    return null;
  }

  return { lat, lng };
}

function randomVerificationCode(): string {
  const chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
  let code = "";

  for (let index = 0; index < 6; index += 1) {
    code += chars[Math.floor(Math.random() * chars.length)];
  }

  return `DTP-${code}`;
}

export async function generateUniqueVerificationCode(db: Firestore): Promise<string> {
  for (let attempt = 0; attempt < 5; attempt += 1) {
    const candidate = randomVerificationCode();
    const snapshot = await getDocs(
      query(
        collection(db, "job_applications"),
        where("verificationCode", "==", candidate),
        limit(1)
      )
    );

    if (snapshot.empty) {
      return candidate;
    }
  }

  return randomVerificationCode();
}

export function createVerificationQrData(
  verificationId: string,
  jobId: string,
  workerId: string,
  verificationCode: string
): string {
  return [
    "DUTYPE_VERIFY",
    verificationId,
    jobId,
    workerId,
    verificationCode,
    Date.now()
  ].join("|");
}

export async function createPendingWorkVerification(
  input: CreatePendingWorkVerificationInput
): Promise<ProductWorkVerification> {
  const generatedAt = Date.now();
  const verificationCode = await generateUniqueVerificationCode(input.db);
  const verificationId = `${input.jobId}_${input.workerId}_${generatedAt}`;

  return {
    applicationId: input.applicationId,
    employerId: input.employerId,
    employerName: input.employerName,
    expiresAt: generatedAt + 2 * 60 * 60 * 1000,
    generatedAt,
    jobId: input.jobId,
    jobTitle: input.jobTitle,
    qrCodeData: createVerificationQrData(
      verificationId,
      input.jobId,
      input.workerId,
      verificationCode
    ),
    status: "PENDING",
    verificationCode,
    verificationId,
    verifiedAt: null,
    verifiedByEmployerId: null,
    verifiedLocation: null,
    workerId: input.workerId,
    workerName: input.workerName
  };
}

export function normalizeProductVerification(value: unknown): ProductWorkVerification | null {
  if (!value || typeof value !== "object") {
    return null;
  }

  const payload = value as Record<string, unknown>;
  const verificationId = stringValue(payload.verificationId);

  if (!verificationId) {
    return null;
  }

  return {
    applicationId: stringValue(payload.applicationId),
    employerId: stringValue(payload.employerId),
    employerName: stringValue(payload.employerName),
    expiresAt: numberValue(payload.expiresAt),
    generatedAt: numberValue(payload.generatedAt),
    jobId: stringValue(payload.jobId),
    jobTitle: stringValue(payload.jobTitle),
    qrCodeData: stringValue(payload.qrCodeData),
    status: statusValue(payload.status),
    verificationCode: stringValue(payload.verificationCode),
    verificationId,
    verifiedAt:
      payload.verifiedAt == null
        ? null
        : numberValue(payload.verifiedAt, Date.now()),
    verifiedByEmployerId: stringValue(payload.verifiedByEmployerId) || null,
    verifiedLocation: locationValue(payload.verifiedLocation),
    workerId: stringValue(payload.workerId),
    workerName: stringValue(payload.workerName)
  };
}

export function verificationIsExpired(
  verification: ProductWorkVerification | null | undefined,
  now = Date.now()
): boolean {
  if (!verification) {
    return false;
  }

  return verification.expiresAt > 0 && verification.expiresAt < now;
}

export function verificationExpiryText(
  verification: ProductWorkVerification | null | undefined,
  now = Date.now()
): string {
  if (!verification) {
    return "Verification not generated";
  }

  if (verification.status === "VERIFIED") {
    return "Verified";
  }

  if (verificationIsExpired(verification, now)) {
    return "Expired";
  }

  const remainingMs = Math.max(0, verification.expiresAt - now);
  const remainingMinutes = Math.floor(remainingMs / (60 * 1000));

  if (remainingMinutes < 60) {
    return `${remainingMinutes} min remaining`;
  }

  const hours = Math.floor(remainingMinutes / 60);
  const minutes = remainingMinutes % 60;

  return `${hours}h ${minutes}m remaining`;
}
