import { PHASE_DEVELOPMENT_SERVER } from "next/constants.js";

const baseConfig = {
  reactStrictMode: true,
  experimental: {
    // Keep Firebase Admin and its Google/otel dependency tree as native Node externals.
    // This avoids unstable vendor chunk generation in Next 14 dev mode on Windows.
    serverComponentsExternalPackages: [
      "firebase-admin",
      "@google-cloud/firestore",
      "@google-cloud/storage",
      "@grpc/grpc-js",
      "@opentelemetry/api",
      "google-gax"
    ]
  },
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "firebasestorage.googleapis.com"
      },
      {
        protocol: "https",
        hostname: "lh3.googleusercontent.com"
      }
    ]
  },
  // Ensure /.well-known/assetlinks.json is served as application/json for
  // Android App Links Digital Asset Links verification (dutype.in).
  async headers() {
    return [
      {
        source: "/.well-known/assetlinks.json",
        headers: [
          { key: "Content-Type", value: "application/json" },
          { key: "Cache-Control", value: "public, max-age=3600" }
        ]
      }
    ];
  }
};

function shouldBuildStandalone() {
  const value = process.env.NEXT_STANDALONE;
  return value === "1" || value === "true";
}

export default function nextConfig(phase) {
  return {
    ...baseConfig,
    ...(shouldBuildStandalone() ? { output: "standalone" } : {}),
    // Keep dev output separate from production builds so the two modes
    // cannot corrupt each other's chunk graph.
    distDir: phase === PHASE_DEVELOPMENT_SERVER ? ".next-dev" : ".next"
  };
}
