import { FirebaseApp, getApp, getApps, initializeApp } from "firebase/app";
import { Auth, getAuth } from "firebase/auth";
import { Firestore, getFirestore } from "firebase/firestore";
import { Functions, getFunctions } from "firebase/functions";
import { FirebaseStorage, getStorage } from "firebase/storage";

import { firebaseConfig, hasFirebaseConfig } from "@/lib/firebase/config";

const DEFAULT_APP_NAME = "[DEFAULT]";

type FirebaseServices = {
  app: FirebaseApp;
  auth: Auth;
  db: Firestore;
  functions: Functions;
  storage: FirebaseStorage;
};

let cachedServices: FirebaseServices | null = null;

export function getFirebaseServices(): FirebaseServices | null {
  if (!hasFirebaseConfig) {
    return null;
  }

  if (cachedServices) {
    return cachedServices;
  }

  const app =
    getApps().find((existingApp) => existingApp.name === DEFAULT_APP_NAME) ??
    initializeApp(firebaseConfig);

  cachedServices = {
    app,
    auth: getAuth(app),
    db: getFirestore(app),
    functions: getFunctions(app),
    storage: getStorage(app)
  };

  return cachedServices;
}
