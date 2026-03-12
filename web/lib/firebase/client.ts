import { FirebaseApp, getApp, getApps, initializeApp } from "firebase/app";
import { Auth, getAuth } from "firebase/auth";
import { Firestore, getFirestore } from "firebase/firestore";
import { Functions, getFunctions } from "firebase/functions";

import { firebaseConfig, hasFirebaseConfig } from "@/lib/firebase/config";

type FirebaseServices = {
  app: FirebaseApp;
  auth: Auth;
  db: Firestore;
  functions: Functions;
};

let cachedServices: FirebaseServices | null = null;

export function getFirebaseServices(): FirebaseServices | null {
  if (!hasFirebaseConfig) {
    return null;
  }

  if (cachedServices) {
    return cachedServices;
  }

  const app = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);

  cachedServices = {
    app,
    auth: getAuth(app),
    db: getFirestore(app),
    functions: getFunctions(app)
  };

  return cachedServices;
}
