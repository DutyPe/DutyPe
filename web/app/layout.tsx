import type { Metadata } from "next";
import { IBM_Plex_Sans, Sora } from "next/font/google";
import { ReactNode } from "react";

import { SITE_URL, cityLandingTargets, coreSeoKeywords, siteMeta } from "@/lib/public-site";

import "./globals.css";

const bodyFont = IBM_Plex_Sans({
  subsets: ["latin"],
  variable: "--font-body",
  weight: ["400", "500", "600", "700"]
});

const displayFont = Sora({
  subsets: ["latin"],
  variable: "--font-display"
});

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: "DutyPe",
    template: "%s | DutyPe"
  },
  description: siteMeta.description,
  keywords: [
    ...coreSeoKeywords,
    ...cityLandingTargets.map((city) => `jobs in ${city.toLowerCase()}`),
    "job app for workers",
    "free job search for workers",
    "hire local staff"
  ],
  applicationName: "DutyPe",
  icons: {
    icon: [
      { url: "/favicon.ico", sizes: "any" },
      { url: "/icon.png", type: "image/png", sizes: "192x192" },
      { url: "/icon.svg", type: "image/svg+xml" }
    ],
    shortcut: "/favicon.ico",
    apple: "/apple-icon.png"
  },
  alternates: {
    canonical: "/"
  },
  openGraph: {
    title: "DutyPe",
    description: siteMeta.description,
    type: "website",
    siteName: "DutyPe",
    url: SITE_URL
  },
  twitter: {
    card: "summary_large_image",
    title: "DutyPe",
    description: siteMeta.description
  },
  verification: {
    google: process.env.GOOGLE_SITE_VERIFICATION
  }
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body className={`${bodyFont.variable} ${displayFont.variable}`}>
        {children}
      </body>
    </html>
  );
}
