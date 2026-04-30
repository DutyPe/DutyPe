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
  authors: [{ name: siteMeta.companyName }],
  creator: siteMeta.companyName,
  publisher: siteMeta.companyName,
  category: "employment",
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
      {
        url: "/playstore.png",
        type: "image/png",
        sizes: "512x512"
      }
    ],
    shortcut: "/playstore.png",
    apple: "/playstore.png"
  },
  alternates: {
    canonical: "/"
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      "max-image-preview": "large",
      "max-snippet": -1,
      "max-video-preview": -1
    }
  },
  openGraph: {
    title: "DutyPe",
    description: siteMeta.description,
    type: "website",
    siteName: "DutyPe",
    url: SITE_URL,
    locale: "en_IN"
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
      <body className={`${bodyFont.variable} ${displayFont.variable}`} suppressHydrationWarning>{children}</body>
    </html>
  );
}
