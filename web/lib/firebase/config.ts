const fallbackConfig = {
  apiKey: "AIzaSyCO_0gcUBy0m290p8UPoEOqT_XWc1Gccdo",
  authDomain: "dutypeapp.firebaseapp.com",
  projectId: "dutypeapp",
  storageBucket: "dutypeapp.firebasestorage.app",
  messagingSenderId: "1062348180452",
  appId: "1:1062348180452:web:b48915e74d2469ad0095f1"
};

export const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY ?? fallbackConfig.apiKey,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN ?? fallbackConfig.authDomain,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID ?? fallbackConfig.projectId,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET ?? fallbackConfig.storageBucket,
  messagingSenderId:
    process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID ?? fallbackConfig.messagingSenderId,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID ?? fallbackConfig.appId
};

export const hasFirebaseConfig = Object.values(firebaseConfig).every(Boolean);
