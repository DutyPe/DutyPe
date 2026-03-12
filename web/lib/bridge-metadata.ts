import type { Metadata } from "next";

import { SITE_URL, coreSeoKeywords } from "@/lib/public-site";

type BridgeMetaInput = {
  title: string;
  description: string;
  keywords?: string[];
};

export function getBridgeMetadata(input: BridgeMetaInput): Metadata {
  const keywords = [...coreSeoKeywords, ...(input.keywords ?? [])];

  return {
    title: {
      absolute: input.title
    },
    description: input.description,
    keywords,
    alternates: {
      canonical: "/"
    },
    openGraph: {
      title: input.title,
      description: input.description,
      type: "website",
      url: SITE_URL,
      images: []
    },
    twitter: {
      card: "summary",
      title: input.title,
      description: input.description,
      images: []
    }
  };
}

export const bridgeMetadata: Metadata = {
  ...getBridgeMetadata({
    title: "DutyPe",
    description: "Open DutyPe app routes for jobs, worker profiles, employer profiles, referrals, notifications, and chats.",
    keywords: ["app redirect", "open app route", "deep link"]
  })
};
