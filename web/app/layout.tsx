import type { Metadata } from "next";
import { IBM_Plex_Sans, Sora } from "next/font/google";
import { ReactNode } from "react";

import { SITE_URL, siteMeta } from "@/lib/public-site";

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
  openGraph: {
    title: "DutyPe",
    description: siteMeta.description,
    type: "website"
  }
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body className={`${bodyFont.variable} ${displayFont.variable}`}>{children}</body>
    </html>
  );
}
