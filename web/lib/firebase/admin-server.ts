import "server-only";

import { readFileSync } from "node:fs";
import { isAbsolute, resolve } from "node:path";
import {
  applicationDefault,
  cert,
  getApp,
  getApps,
  initializeApp,
  type ServiceAccount
} from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";

import { firebaseConfig } from "@/lib/firebase/config";

const DEFAULT_APP_NAME = "[DEFAULT]";

type RawServiceAccount = Partial<ServiceAccount> & {
  project_id?: string;
  client_email?: string;
  private_key?: string;
};

function normalizePrivateKey(value: string | undefined) {
  return value?.replace(/\\n/g, "\n");
}

function normalizeServiceAccount(raw: RawServiceAccount): ServiceAccount | null {
  const projectId = raw.projectId ?? raw.project_id;
  const clientEmail = raw.clientEmail ?? raw.client_email;
  const privateKey = normalizePrivateKey(raw.privateKey ?? raw.private_key);

  if (!projectId || !clientEmail || !privateKey) {
    return null;
  }

  return {
    projectId,
    clientEmail,
    privateKey
  };
}

function readServiceAccountFromPath() {
  const configuredPath = process.env.FIREBASE_ADMIN_SERVICE_ACCOUNT_PATH;

  if (!configuredPath) {
    return null;
  }

  const resolvedPath = isAbsolute(configuredPath)
    ? configuredPath
    : resolve(process.cwd(), configuredPath);

  const fileContents = readFileSync(resolvedPath, "utf8");
  return normalizeServiceAccount(JSON.parse(fileContents) as RawServiceAccount);
}

function readServiceAccountFromJsonEnv() {
  const rawJson = process.env.FIREBASE_ADMIN_SERVICE_ACCOUNT_JSON;

  if (!rawJson) {
    return null;
  }

  return normalizeServiceAccount(JSON.parse(rawJson) as RawServiceAccount);
}

function readServiceAccountFromParts() {
  return normalizeServiceAccount({
    projectId: process.env.FIREBASE_ADMIN_PROJECT_ID ?? firebaseConfig.projectId,
    clientEmail: process.env.FIREBASE_ADMIN_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_ADMIN_PRIVATE_KEY
  });
}

function readServiceAccount() {
  return (
    readServiceAccountFromPath() ??
    readServiceAccountFromJsonEnv() ??
    readServiceAccountFromParts()
  );
}

export function isFirebaseAdminConfigured() {
  if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    return true;
  }

  if (process.env.FIREBASE_ADMIN_SERVICE_ACCOUNT_PATH || process.env.FIREBASE_ADMIN_SERVICE_ACCOUNT_JSON) {
    return true;
  }

  return Boolean(
    process.env.FIREBASE_ADMIN_CLIENT_EMAIL &&
      process.env.FIREBASE_ADMIN_PRIVATE_KEY &&
      (process.env.FIREBASE_ADMIN_PROJECT_ID ?? firebaseConfig.projectId)
  );
}

export function getFirebaseAdminApp() {
  const existingDefaultApp = getApps().find((existingApp) => existingApp.name === DEFAULT_APP_NAME);

  if (existingDefaultApp) {
    return getApp();
  }

  const serviceAccount = readServiceAccount();

  if (serviceAccount) {
    return initializeApp({
      credential: cert(serviceAccount),
      projectId: serviceAccount.projectId
    });
  }

  if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    return initializeApp({
      credential: applicationDefault(),
      projectId: process.env.FIREBASE_ADMIN_PROJECT_ID ?? firebaseConfig.projectId
    });
  }

  throw new Error(
    "Firebase Admin SDK is not configured. Set FIREBASE_ADMIN_SERVICE_ACCOUNT_PATH, FIREBASE_ADMIN_SERVICE_ACCOUNT_JSON, or the individual FIREBASE_ADMIN_* fields."
  );
}

export function getFirebaseAdminAuth() {
  return getAuth(getFirebaseAdminApp());
}

export function getFirebaseAdminDb() {
  return getFirestore(getFirebaseAdminApp());
}
