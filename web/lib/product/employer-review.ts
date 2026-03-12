import { formatTextList } from "@/lib/firebase/firestore-helpers";

import {
  normalizeProductApplication,
  type ProductApplication
} from "./marketplace";
import {
  normalizeProductVerification,
  type ProductVerificationStatus,
  type ProductWorkVerification
} from "./work-verification";

export type EmployerReviewWorkExperience = {
  company: string;
  description: string;
  endDate: string;
  id: string;
  position: string;
  startDate: string;
};

export type EmployerReviewEducation = {
  degree: string;
  endDate: string;
  id: string;
  institution: string;
  startDate: string;
};

export type EmployerReviewDocument = {
  fileName: string;
  fileType: string;
  fileUrl: string;
  id: string;
  name: string;
  type: string;
  uploadedAt: unknown;
};

export type EmployerReviewApplication = ProductApplication & {
  additionalDocuments: EmployerReviewDocument[];
  availability: string;
  completedAt: unknown;
  certifications: string[];
  education: EmployerReviewEducation[];
  expectedSalary: string;
  homeEntryJob: boolean;
  languages: string[];
  resumeUrl: string;
  skills: string[];
  skillsText: string;
  verification: ProductWorkVerification | null;
  verificationCode: string;
  verificationId: string;
  verificationStatus: ProductVerificationStatus | "";
  workStartedAt: unknown;
  workExperience: EmployerReviewWorkExperience[];
  workExperienceText: string;
  workerAadhaarVerified: boolean;
  workerBackgroundCheckPassed: boolean;
  workerDateOfBirth: string;
  workerEmail: string;
  workerGender: string;
  workerIdentityVerified: boolean;
  workerJobsInArea: number;
  workerLocalRating: number;
  workerPhone: string;
  workerPhoneVerified: boolean;
  workerProfileImageUrl: string;
  workerTotalReviews: number;
  workerLocation: string;
};

export type EmployerReviewWorkerProfile = {
  availability: string;
  certifications: string[];
  dateOfBirth: string;
  education: EmployerReviewEducation[];
  email: string;
  expectedSalary: string;
  fullName: string;
  gender: string;
  languages: string[];
  location: string;
  phone: string;
  profileImageUrl: string;
  resumeUrl: string;
  skills: string[];
  workExperience: EmployerReviewWorkExperience[];
  workExperienceText: string;
  workerId: string;
};

function stringValue(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}

function booleanValue(value: unknown, fallback = false): boolean {
  return typeof value === "boolean" ? value : fallback;
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

function objectList(value: unknown): Record<string, unknown>[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.filter((item): item is Record<string, unknown> => typeof item === "object" && item !== null);
}

function normalizeWorkExperience(value: unknown): EmployerReviewWorkExperience[] {
  return objectList(value).map((item, index) => ({
    company: stringValue(item.company),
    description: stringValue(item.description),
    endDate: stringValue(item.endDate),
    id: stringValue(item.id, `experience-${index}`),
    position: stringValue(item.position),
    startDate: stringValue(item.startDate)
  }));
}

function normalizeEducation(value: unknown): EmployerReviewEducation[] {
  return objectList(value).map((item, index) => ({
    degree: stringValue(item.degree),
    endDate: stringValue(item.endDate),
    id: stringValue(item.id, `education-${index}`),
    institution: stringValue(item.institution),
    startDate: stringValue(item.startDate)
  }));
}

function normalizeDocuments(value: unknown): EmployerReviewDocument[] {
  return objectList(value).map((item, index) => ({
    fileName: stringValue(item.fileName || item.name),
    fileType: stringValue(item.fileType || item.type, "OTHER"),
    fileUrl: stringValue(item.fileUrl || item.url),
    id: stringValue(item.id, `document-${index}`),
    name: stringValue(item.name || item.fileName),
    type: stringValue(item.type || item.fileType, "document"),
    uploadedAt: item.uploadedAt ?? Date.now()
  }));
}

export function normalizeEmployerReviewApplication(
  id: string,
  value: Record<string, unknown> | undefined
): EmployerReviewApplication {
  const payload = value ?? {};
  const base = normalizeProductApplication(id, payload);
  const verification = normalizeProductVerification(payload.verification);
  const verificationStatus = stringValue(
    payload.verificationStatus,
    verification?.status ?? ""
  ).toUpperCase();

  return {
    ...base,
    additionalDocuments: normalizeDocuments(payload.additionalDocuments),
    availability: stringValue(payload.availability),
    completedAt: payload.completedAt ?? null,
    certifications: formatTextList(payload.certifications),
    education: normalizeEducation(payload.education),
    expectedSalary: stringValue(payload.expectedSalary),
    homeEntryJob: booleanValue(payload.homeEntryJob),
    languages: formatTextList(payload.languages),
    resumeUrl: stringValue(payload.resumeUrl),
    skills: formatTextList(payload.skills),
    skillsText: stringValue(payload.skillsText, formatTextList(payload.skills).join(", ")),
    verification,
    verificationCode: stringValue(
      payload.verificationCode,
      verification?.verificationCode ?? ""
    ),
    verificationId: stringValue(payload.verificationId, verification?.verificationId ?? ""),
    verificationStatus:
      verificationStatus === "PENDING" ||
      verificationStatus === "VERIFIED" ||
      verificationStatus === "EXPIRED" ||
      verificationStatus === "CANCELLED"
        ? (verificationStatus as ProductVerificationStatus)
        : "",
    workStartedAt: payload.workStartedAt ?? null,
    workExperience: normalizeWorkExperience(payload.workExperience),
    workExperienceText: stringValue(payload.workExperienceText),
    workerAadhaarVerified: booleanValue(payload.workerAadhaarVerified),
    workerBackgroundCheckPassed: booleanValue(payload.workerBackgroundCheckPassed),
    workerDateOfBirth: stringValue(payload.workerDateOfBirth),
    workerEmail: stringValue(payload.workerEmail),
    workerGender: stringValue(payload.workerGender),
    workerIdentityVerified: booleanValue(payload.workerIdentityVerified),
    workerJobsInArea: numberValue(payload.workerJobsInArea),
    workerLocalRating: numberValue(payload.workerLocalRating),
    workerPhone: stringValue(payload.workerPhone),
    workerPhoneVerified: booleanValue(payload.workerPhoneVerified, true),
    workerProfileImageUrl: stringValue(payload.workerProfileImageUrl),
    workerTotalReviews: numberValue(payload.workerTotalReviews),
    workerLocation: stringValue(payload.workerLocation)
  };
}

export function mergeEmployerReviewApplicationWithProfile(
  application: EmployerReviewApplication,
  userProfile: Record<string, unknown> | null | undefined,
  workerProfile: Record<string, unknown> | null | undefined
): EmployerReviewApplication {
  const mergedProfile = {
    ...(workerProfile ?? {}),
    ...(userProfile ?? {})
  };

  const skills = formatTextList(mergedProfile.skills);
  const certifications = formatTextList(mergedProfile.certifications);
  const languages = formatTextList(mergedProfile.languages);
  const experienceText = stringValue(mergedProfile.experience, application.workExperienceText);

  return {
    ...application,
    availability: stringValue(mergedProfile.availability, application.availability),
    certifications: certifications.length > 0 ? certifications : application.certifications,
    education:
      normalizeEducation(mergedProfile.education).length > 0
        ? normalizeEducation(mergedProfile.education)
        : application.education,
    expectedSalary: stringValue(mergedProfile.expectedSalary, application.expectedSalary),
    languages: languages.length > 0 ? languages : application.languages,
    resumeUrl: stringValue(mergedProfile.resumeUrl, application.resumeUrl),
    skills: skills.length > 0 ? skills : application.skills,
    skillsText: skills.length > 0 ? skills.join(", ") : application.skillsText,
    workExperience:
      normalizeWorkExperience(mergedProfile.workExperience).length > 0
        ? normalizeWorkExperience(mergedProfile.workExperience)
        : application.workExperience,
    workExperienceText: experienceText,
    workerDateOfBirth: stringValue(mergedProfile.dateOfBirth, application.workerDateOfBirth),
    workerEmail: stringValue(mergedProfile.email, application.workerEmail),
    workerGender: stringValue(mergedProfile.gender, application.workerGender),
    workerLocation: stringValue(
      mergedProfile.address || mergedProfile.location,
      application.workerLocation
    ),
    workerName: stringValue(
      mergedProfile.fullName || mergedProfile.name || mergedProfile.displayName,
      application.workerName
    ),
    workerPhone: stringValue(
      mergedProfile.phone || mergedProfile.phoneNumber,
      application.workerPhone
    ),
    workerProfileImageUrl: stringValue(
      mergedProfile.profileImageUrl,
      application.workerProfileImageUrl
    )
  };
}

export function buildEmployerReviewWorkerProfile(
  workerId: string,
  userProfile: Record<string, unknown> | null | undefined,
  workerProfile: Record<string, unknown> | null | undefined,
  application?: EmployerReviewApplication | null
): EmployerReviewWorkerProfile {
  const mergedProfile = {
    ...(workerProfile ?? {}),
    ...(userProfile ?? {})
  };
  const applicationFallback = application ?? null;
  const profileSkills = formatTextList(mergedProfile.skills);
  const profileCertifications = formatTextList(mergedProfile.certifications);
  const profileLanguages = formatTextList(mergedProfile.languages);
  const profileExperience = normalizeWorkExperience(mergedProfile.workExperience);
  const profileEducation = normalizeEducation(mergedProfile.education);

  return {
    availability: stringValue(mergedProfile.availability, applicationFallback?.availability ?? ""),
    certifications:
      profileCertifications.length > 0 ? profileCertifications : applicationFallback?.certifications ?? [],
    dateOfBirth: stringValue(mergedProfile.dateOfBirth, applicationFallback?.workerDateOfBirth ?? ""),
    education: profileEducation.length > 0 ? profileEducation : applicationFallback?.education ?? [],
    email: stringValue(mergedProfile.email, applicationFallback?.workerEmail ?? ""),
    expectedSalary: stringValue(
      mergedProfile.expectedSalary,
      applicationFallback?.expectedSalary ?? ""
    ),
    fullName: stringValue(
      mergedProfile.fullName || mergedProfile.name || mergedProfile.displayName,
      applicationFallback?.workerName ?? "DutyPe worker"
    ),
    gender: stringValue(mergedProfile.gender, applicationFallback?.workerGender ?? ""),
    languages: profileLanguages.length > 0 ? profileLanguages : applicationFallback?.languages ?? [],
    location: stringValue(
      mergedProfile.address || mergedProfile.location,
      applicationFallback?.workerLocation ?? ""
    ),
    phone: stringValue(
      mergedProfile.phone || mergedProfile.phoneNumber,
      applicationFallback?.workerPhone ?? ""
    ),
    profileImageUrl: stringValue(
      mergedProfile.profileImageUrl,
      applicationFallback?.workerProfileImageUrl ?? ""
    ),
    resumeUrl: stringValue(mergedProfile.resumeUrl, applicationFallback?.resumeUrl ?? ""),
    skills: profileSkills.length > 0 ? profileSkills : applicationFallback?.skills ?? [],
    workExperience: profileExperience.length > 0 ? profileExperience : applicationFallback?.workExperience ?? [],
    workExperienceText: stringValue(
      mergedProfile.experience,
      applicationFallback?.workExperienceText ?? ""
    ),
    workerId
  };
}
