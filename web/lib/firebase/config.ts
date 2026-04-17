const fallbackConfig = {
  apiKey: "AIzaSyDFIzb8G90t3OjUShiceM8AeEdKzJ640_c",
  authDomain: "dutype-860ac.firebaseapp.com",
  projectId: "dutype-860ac",
  storageBucket: "dutype-860ac.firebasestorage.app",
  messagingSenderId: "1024525807494",
  appId: "1:1024525807494:web:b425aaecaf4c873235c607"
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
