export const PLAY_STORE_URL =
  "https://play.google.com/store/apps/details?id=com.dutype.app";
export const APP_STORE_URL = "https://apps.apple.com/app/dutype";
export const SUPPORT_EMAIL = "dutypein@gmail.com";
export const FEEDBACK_EMAIL = "dutypefeedback@gmail.com";
export const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL || "https://dutypeapp.web.app";

export const siteMeta = {
  name: "DutyPe",
  strapline: "Local jobs near you",
  description:
    "Find local jobs near you. Connect workers with employers instantly. No middlemen, no fees."
};

export const primaryNav = [
  { href: "/", label: "Home" },
  { href: "/jobs", label: "Jobs" },
  { href: "/jobs-near-me", label: "Jobs Near Me" },
  { href: "/safety", label: "Safety" },
  { href: "/faq", label: "FAQ" },
  { href: "/contact", label: "Contact" },
  { href: "/app", label: "Open App" }
];

export const footerGroups = [
  {
    title: "Browse",
    links: [
      { href: "/jobs", label: "All jobs" },
      { href: "/delivery-jobs", label: "Delivery jobs" },
      { href: "/driver-jobs", label: "Driver jobs" },
      { href: "/maid-jobs", label: "Maid jobs" }
    ]
  },
  {
    title: "Support",
    links: [
      { href: "/contact", label: "Contact us" },
      { href: "/faq", label: "FAQ" },
      { href: "/safety", label: "Safety" },
      { href: `mailto:${SUPPORT_EMAIL}`, label: SUPPORT_EMAIL }
    ]
  },
  {
    title: "Legal",
    links: [
      { href: "/privacy", label: "Privacy" },
      { href: "/terms", label: "Terms" },
      { href: "/refund", label: "Refunds" }
    ]
  }
];

export const homeBenefits = [
  {
    title: "Jobs near you",
    description:
      "Find jobs within walking distance or a short commute. DutyPe is built for hyperlocal work discovery."
  },
  {
    title: "Instant apply",
    description:
      "Workers should be able to move fast. The old website promised one-tap applying, and the new site keeps that product story."
  },
  {
    title: "Daily payments",
    description:
      "Many openings are daily-wage, weekly-pay, or shift-based roles where fast cash flow matters."
  },
  {
    title: "Safe and verified",
    description:
      "Safety messaging is core to this product. Verified employers, report flows, and trust signals need to be visible everywhere."
  },
  {
    title: "Direct chat",
    description:
      "Workers and employers should be able to connect directly without middlemen or offline fee traps."
  },
  {
    title: "Free for workers",
    description:
      "The platform promise is simple: job seekers should never have to pay to find or apply for work."
  }
];

export const homeCategories = [
  { href: "/jobs-near-me", label: "Jobs Near Me" },
  { href: "/driver-jobs", label: "Driver Jobs" },
  { href: "/maid-jobs", label: "Maid Jobs" },
  { href: "/delivery-jobs", label: "Delivery Jobs" },
  { href: "/part-time-jobs", label: "Part-Time Jobs" },
  { href: "/warehouse-jobs", label: "Warehouse Jobs" }
];

export const cityLandingTargets = [
  "Hyderabad",
  "Bangalore",
  "Delhi",
  "Mumbai",
  "Vijayawada",
  "Warangal"
];

export type LegacyPageTone = "default" | "highlight" | "warning" | "neutral";

export type LegacyPageBlock =
  | {
      kind: "copy";
      title: string;
      paragraphs: string[];
      tone?: LegacyPageTone;
    }
  | {
      kind: "list";
      title: string;
      intro?: string;
      items: string[];
      tone?: LegacyPageTone;
    }
  | {
      kind: "faq";
      title: string;
      items: { question: string; answer: string }[];
      tone?: LegacyPageTone;
    }
  | {
      kind: "contact";
      title: string;
      items: {
        label: string;
        value: string;
        note: string;
        href?: string;
      }[];
      tone?: LegacyPageTone;
    }
  | {
      kind: "table";
      title: string;
      intro?: string;
      columns: [string, string];
      rows: [string, string][];
      tone?: LegacyPageTone;
    };

export type LegacyPageDescriptor = {
  slug: string;
  title: string;
  description: string;
  eyebrow: string;
  intro: string;
  highlights: string[];
  blocks: LegacyPageBlock[];
  ctaTitle?: string;
  ctaCopy?: string;
  ctaHref?: string;
  ctaLabel?: string;
};

export type PublicJobRouteData = {
  id: string;
  title: string;
  company: string;
  location: string;
  salary: string;
  shift: string;
  summary: string;
  trustSignals: string[];
  features: string[];
};

export type PublicWorkerRouteData = {
  id: string;
  name: string;
  role: string;
  city: string;
  summary: string;
  trustSignals: string[];
  features: string[];
};

export type PublicEmployerRouteData = {
  id: string;
  company: string;
  industry: string;
  city: string;
  summary: string;
  trustSignals: string[];
  features: string[];
};

export type DeepLinkKind =
  | "home"
  | "job"
  | "worker"
  | "employer"
  | "refer"
  | "application"
  | "chat"
  | "profile"
  | "notifications";

type CategoryDetails = {
  label: string;
  shortLabel: string;
  intro: string;
  salary: string;
  roles: string[];
};

const cityNames: Record<string, string> = {
  hyderabad: "Hyderabad",
  bangalore: "Bangalore",
  delhi: "Delhi",
  mumbai: "Mumbai",
  vijayawada: "Vijayawada",
  warangal: "Warangal",
  anantapur: "Anantapur",
  guntur: "Guntur",
  kakinada: "Kakinada",
  karimnagar: "Karimnagar",
  khammam: "Khammam",
  nellore: "Nellore",
  nizamabad: "Nizamabad",
  rajahmundry: "Rajahmundry",
  tirupati: "Tirupati"
};

const categoryDetails: Record<string, CategoryDetails> = {
  delivery: {
    label: "Delivery jobs",
    shortLabel: "Delivery",
    intro:
      "Fast-moving roles for parcel, grocery, food, and hyperlocal logistics partners.",
    salary: "Usually Rs 14,000 to Rs 28,000 per month plus incentives.",
    roles: [
      "Food delivery partner",
      "Grocery delivery rider",
      "Parcel pickup executive",
      "Last-mile delivery associate"
    ]
  },
  driver: {
    label: "Driver jobs",
    shortLabel: "Driver",
    intro:
      "Driving openings across personal driver, commercial vehicle, and local transport roles.",
    salary: "Usually Rs 16,000 to Rs 30,000 per month depending on vehicle and shift.",
    roles: [
      "Personal driver",
      "Cab driver",
      "Delivery van driver",
      "Commercial vehicle driver"
    ]
  },
  maid: {
    label: "Maid jobs",
    shortLabel: "Maid",
    intro:
      "Household support openings including cleaning, routine upkeep, and family assistance.",
    salary: "Usually Rs 10,000 to Rs 22,000 per month depending on schedule.",
    roles: [
      "Part-time maid",
      "Full-time maid",
      "House cleaner",
      "Live-in domestic helper"
    ]
  },
  cook: {
    label: "Cook jobs",
    shortLabel: "Cook",
    intro:
      "Cooking roles for homes, hostels, tiffin centers, cafes, and small kitchens.",
    salary: "Usually Rs 12,000 to Rs 26,000 per month depending on cuisine and hours.",
    roles: [
      "Home cook",
      "Breakfast cook",
      "Tiffin cook",
      "Assistant kitchen cook"
    ]
  },
  cleaner: {
    label: "Cleaner jobs",
    shortLabel: "Cleaner",
    intro:
      "Cleaning roles across homes, offices, clinics, apartments, and commercial buildings.",
    salary: "Usually Rs 10,000 to Rs 20,000 per month.",
    roles: [
      "Office cleaner",
      "House cleaner",
      "Apartment maintenance cleaner",
      "Clinic cleaning staff"
    ]
  },
  helper: {
    label: "Helper jobs",
    shortLabel: "Helper",
    intro:
      "General helper roles where reliability, availability, and local access matter most.",
    salary: "Usually Rs 11,000 to Rs 22,000 per month.",
    roles: [
      "Shop helper",
      "Packing helper",
      "Office helper",
      "Operations helper"
    ]
  },
  "daily-wage": {
    label: "Daily wage jobs",
    shortLabel: "Daily wage",
    intro:
      "Short-cycle work where workers want fast payouts and employers need quick staffing.",
    salary: "Usually Rs 500 to Rs 1,200 per day depending on role and city.",
    roles: [
      "Daily shift helper",
      "Daily cleaning work",
      "Festival staffing",
      "Temporary loading work"
    ]
  },
  "part-time": {
    label: "Part-time jobs",
    shortLabel: "Part-time",
    intro:
      "Flexible jobs for students, homemakers, and workers balancing multiple responsibilities.",
    salary: "Usually Rs 6,000 to Rs 18,000 per month depending on shift pattern.",
    roles: [
      "Evening shift staff",
      "Morning housekeeping",
      "Weekend support",
      "Flexible delivery work"
    ]
  },
  peon: {
    label: "Peon jobs",
    shortLabel: "Peon",
    intro:
      "Office-running roles focused on file movement, supplies, support errands, and basic upkeep.",
    salary: "Usually Rs 10,000 to Rs 18,000 per month.",
    roles: [
      "Office peon",
      "School office assistant",
      "Admin runner",
      "Document support staff"
    ]
  },
  security: {
    label: "Security jobs",
    shortLabel: "Security",
    intro:
      "Security guard and watchman roles across apartments, offices, warehouses, and retail.",
    salary: "Usually Rs 13,000 to Rs 24,000 per month.",
    roles: [
      "Security guard",
      "Apartment watchman",
      "Night shift guard",
      "Retail security staff"
    ]
  },
  warehouse: {
    label: "Warehouse jobs",
    shortLabel: "Warehouse",
    intro:
      "Logistics and backend operations jobs in fulfillment, sorting, packing, and dispatch.",
    salary: "Usually Rs 12,000 to Rs 24,000 per month.",
    roles: [
      "Warehouse picker",
      "Packing associate",
      "Sorting staff",
      "Dispatch helper"
    ]
  }
};

const staticPages: Record<string, LegacyPageDescriptor> = {
  privacy: {
    slug: "privacy",
    title: "Privacy Policy",
    description:
      "Learn what DutyPe collects, how data is used, and the privacy rights available to workers and employers.",
    eyebrow: "Legal",
    intro:
      "We collect only what is needed to connect workers with employers, support trust checks, and keep the marketplace safe.",
    highlights: [
      "We never sell personal data.",
      "Location is meant for nearby job discovery.",
      "Account deletion and notification controls remain part of the product promise."
    ],
    blocks: [
      {
        kind: "copy",
        title: "Summary",
        paragraphs: [
          "We collect only what is necessary to connect workers with employers. We never sell your data. You can request deletion of your account."
        ],
        tone: "highlight"
      },
      {
        kind: "list",
        title: "Information we collect",
        intro: "When you use DutyPe, the platform may collect:",
        items: [
          "Account information such as phone number, name, and optional profile photo.",
          "Location data to show jobs near you while the app is being used.",
          "Job application activity and application status updates.",
          "Basic device information for app quality, security, and optimization."
        ]
      },
      {
        kind: "list",
        title: "How we use it",
        items: [
          "Show relevant jobs near your location.",
          "Connect workers with employers.",
          "Send job alerts, chat updates, and application updates.",
          "Prevent fraud, abuse, and unsafe activity.",
          "Improve service quality and marketplace reliability."
        ]
      },
      {
        kind: "list",
        title: "Sharing and storage",
        items: [
          "Employers can see worker profile details when a worker applies to a job.",
          "Service providers such as Firebase, Google Cloud, maps, and analytics support the platform.",
          "Information may be disclosed if required by law or to protect users from harm.",
          "Data is transmitted using encrypted connections and stored on secure cloud systems."
        ]
      },
      {
        kind: "list",
        title: "Your rights",
        items: [
          "Request access to your stored data.",
          "Correct inaccurate profile information.",
          "Delete your account and associated data.",
          "Disable notifications and location access from your settings."
        ]
      }
    ],
    ctaTitle: "Privacy questions",
    ctaCopy: `Email ${SUPPORT_EMAIL} for privacy, deletion, or account-data requests.`,
    ctaHref: `mailto:${SUPPORT_EMAIL}`,
    ctaLabel: "Email support"
  },
  terms: {
    slug: "terms",
    title: "Terms of Service",
    description:
      "DutyPe connects workers with employers. The platform is not an employer, and misuse, fraud, or worker-charging behavior is prohibited.",
    eyebrow: "Legal",
    intro:
      "DutyPe is a local jobs marketplace. Be accurate, be respectful, and never use the platform to exploit workers.",
    highlights: [
      "Workers should never pay to get a job.",
      "Employers must post genuine openings and pay as promised.",
      "Referral rewards are promotional and subject to verification."
    ],
    blocks: [
      {
        kind: "copy",
        title: "Platform role",
        paragraphs: [
          "DutyPe connects workers and employers. It is a marketplace platform, not an employer or employment agency."
        ],
        tone: "highlight"
      },
      {
        kind: "list",
        title: "Basic eligibility",
        items: [
          "Users must be at least 18 years old.",
          "Profiles and job posts must use accurate information.",
          "A valid phone number is required for account operation."
        ]
      },
      {
        kind: "list",
        title: "Worker rules",
        items: [
          "Apply only to jobs you genuinely want.",
          "Provide honest skill and experience details.",
          "Show up on time and communicate clearly when hired.",
          "Never pay any fee to get a job."
        ]
      },
      {
        kind: "list",
        title: "Employer rules",
        items: [
          "Post only genuine job opportunities.",
          "Provide accurate pay, role, location, and schedule details.",
          "Do not charge workers any fee.",
          "Pay workers as promised and communicate professionally."
        ]
      },
      {
        kind: "list",
        title: "Prohibited activity",
        items: [
          "Fake jobs or fake applications.",
          "Harassment, abuse, or intimidation.",
          "Charging workers for access to jobs.",
          "Spam, bots, or referral fraud.",
          "Multiple fraudulent accounts or misrepresentation."
        ],
        tone: "warning"
      },
      {
        kind: "list",
        title: "Referral and compliance notes",
        items: [
          "Referral rewards are promotional credits that may be withdrawn after verification.",
          "Fraudulent referrals can lead to suspension and forfeiture of rewards.",
          "Referral earnings may be taxable under Indian law.",
          "KYC and PAN requirements may apply depending on annual withdrawal volume."
        ]
      }
    ],
    ctaTitle: "Questions about the terms",
    ctaCopy: `Reach ${SUPPORT_EMAIL} for legal or policy questions.`,
    ctaHref: `mailto:${SUPPORT_EMAIL}`,
    ctaLabel: "Contact DutyPe"
  },
  safety: {
    slug: "safety",
    title: "Safety and Security",
    description:
      "Stay alert for fake jobs, never pay for a role, and use DutyPe's trust signals and reporting tools to stay safe.",
    eyebrow: "Safety",
    intro:
      "Worker safety is core to the DutyPe product. The website should carry the same warnings, trust framing, and reporting guidance as the app.",
    highlights: [
      "Never pay for jobs.",
      "Verify before you go.",
      "Report suspicious activity quickly."
    ],
    blocks: [
      {
        kind: "copy",
        title: "DutyPe safety promise",
        paragraphs: [
          "We are committed to a safer marketplace for workers and employers. Suspicious posts, scams, and abusive behavior should be reported immediately."
        ],
        tone: "highlight"
      },
      {
        kind: "copy",
        title: "Most important rule",
        paragraphs: [
          "Legitimate employers should never ask workers to pay registration fees, security deposits, or other charges to get a job."
        ],
        tone: "warning"
      },
      {
        kind: "list",
        title: "Before you meet anyone",
        items: [
          "Research the company and job location.",
          "Share the job details and meeting location with someone you trust.",
          "Prefer first meetings in public places.",
          "Leave if anything feels wrong or rushed."
        ]
      },
      {
        kind: "list",
        title: "Red flags",
        items: [
          "Unusually high pay for vague work.",
          "Pressure to decide immediately.",
          "Requests for money or documents before hiring.",
          "Refusal to share company details.",
          "Communication only through private messaging with no clear company identity."
        ],
        tone: "warning"
      },
      {
        kind: "list",
        title: "How DutyPe helps",
        items: [
          "Verified employer and trust signals.",
          "In-app reporting tools.",
          "AI and rules-based scam detection.",
          "In-app chat and application history.",
          "Fast review of suspicious activity."
        ]
      }
    ],
    ctaTitle: "Need to report abuse",
    ctaCopy: `Email ${SUPPORT_EMAIL} or use the in-app report flow when something looks unsafe.`,
    ctaHref: `mailto:${SUPPORT_EMAIL}`,
    ctaLabel: "Report an issue"
  },
  refund: {
    slug: "refund",
    title: "Refund and Cancellation Policy",
    description:
      "Workers use DutyPe for free. Employer subscription refunds, cancellations, and processing timelines are summarized here.",
    eyebrow: "Legal",
    intro:
      "DutyPe is free for workers. Refund and cancellation rules mainly apply to employer-side purchases and subscription plans.",
    highlights: [
      "Workers never pay to find jobs.",
      "Unused purchases requested quickly may be refunded.",
      "Subscription cancellation is supported from the app."
    ],
    blocks: [
      {
        kind: "copy",
        title: "Free for workers",
        paragraphs: [
          "DutyPe is 100 percent free for job seekers. No worker should ever need to pay to browse or apply for jobs."
        ],
        tone: "highlight"
      },
      {
        kind: "table",
        title: "Refund eligibility",
        columns: ["Situation", "Refund"],
        rows: [
          ["Request within 24 hours of purchase and unused", "Full refund"],
          ["Technical issue prevented service usage", "Full or partial refund"],
          ["Duplicate payment", "Full refund of duplicate charge"],
          ["Service actively used for more than 7 days", "No refund"],
          ["Account terminated for policy violation", "No refund"]
        ]
      },
      {
        kind: "list",
        title: "How to request a refund",
        items: [
          "Open the DutyPe app and use Settings to Contact Us.",
          `Or email ${SUPPORT_EMAIL}.`,
          "Include the registered phone number and payment details.",
          "Describe the issue clearly to speed up review."
        ]
      },
      {
        kind: "list",
        title: "Processing timeline",
        items: [
          "Refund requests are usually reviewed within 3 to 5 business days.",
          "Approved refunds are processed within 7 to 10 business days.",
          "Refunds go back to the original payment method.",
          "Bank processing can add extra delay."
        ]
      }
    ],
    ctaTitle: "Refund support",
    ctaCopy: `Send refund or billing questions to ${SUPPORT_EMAIL}.`,
    ctaHref: `mailto:${SUPPORT_EMAIL}`,
    ctaLabel: "Email billing support"
  },
  contact: {
    slug: "contact",
    title: "Contact DutyPe",
    description:
      "Reach DutyPe for support, privacy issues, abuse reports, refunds, legal questions, and product feedback.",
    eyebrow: "Support",
    intro:
      "Choose the right contact path so queries move faster. Abuse, privacy, and billing should be routed clearly.",
    highlights: [
      "General support and abuse reports use the main support inbox.",
      "Feedback can go to a separate email path.",
      "In-app support should remain the fastest option."
    ],
    blocks: [
      {
        kind: "contact",
        title: "Contact channels",
        items: [
          {
            label: "General support",
            value: SUPPORT_EMAIL,
            note: "Questions about jobs, accounts, and troubleshooting.",
            href: `mailto:${SUPPORT_EMAIL}`
          },
          {
            label: "Privacy concerns",
            value: SUPPORT_EMAIL,
            note: "Account deletion, data access, and privacy questions.",
            href: `mailto:${SUPPORT_EMAIL}`
          },
          {
            label: "Refunds and billing",
            value: SUPPORT_EMAIL,
            note: "Payment issues, cancellations, and employer billing.",
            href: `mailto:${SUPPORT_EMAIL}`
          },
          {
            label: "Feedback",
            value: FEEDBACK_EMAIL,
            note: "Product ideas, complaints, and usability feedback.",
            href: `mailto:${FEEDBACK_EMAIL}`
          }
        ]
      },
      {
        kind: "copy",
        title: "Response time",
        paragraphs: [
          "Support usually responds within 24 to 48 hours on business days."
        ]
      },
      {
        kind: "copy",
        title: "In-app support",
        paragraphs: [
          "For the fastest path, workers and employers should use the app support entry point in Settings."
        ]
      }
    ],
    ctaTitle: "Open the app for support",
    ctaCopy: "Use the in-app support path when you need quicker account-specific help.",
    ctaHref: "/app",
    ctaLabel: "Open DutyPe app"
  },
  faq: {
    slug: "faq",
    title: "Frequently Asked Questions",
    description:
      "Answers for workers, employers, privacy, payments, and platform safety on DutyPe.",
    eyebrow: "Support",
    intro:
      "This FAQ carries the core worker and employer guidance from the old hosted site, but in a cleaner route system.",
    highlights: [
      "DutyPe is free for workers.",
      "Workers should never pay for jobs.",
      "Employers can manage jobs and applicants from the app."
    ],
    blocks: [
      {
        kind: "faq",
        title: "For workers",
        items: [
          {
            question: "Is DutyPe free for workers?",
            answer:
              "Yes. DutyPe is intended to be 100 percent free for job seekers."
          },
          {
            question: "How do I apply for a job?",
            answer:
              "Browse nearby jobs, open a role, and apply through the app so your profile is shared with the employer."
          },
          {
            question: "Should I ever pay to get a job?",
            answer:
              "No. If someone asks you for money, treat it as suspicious and report it."
          },
          {
            question: "Can I apply to multiple jobs?",
            answer: "Yes. Workers can apply to multiple jobs without a fixed limit."
          }
        ]
      },
      {
        kind: "faq",
        title: "For employers",
        items: [
          {
            question: "How do I post a job?",
            answer:
              "Sign in as an employer, complete the profile, and use the Post Job flow in the app."
          },
          {
            question: "Is posting free?",
            answer:
              "Basic posting may be free, while premium visibility or advanced access can depend on subscriptions."
          },
          {
            question: "How do I contact applicants?",
            answer:
              "Use the employer dashboard to review applications and contact workers through supported product flows."
          }
        ]
      },
      {
        kind: "faq",
        title: "Privacy and billing",
        items: [
          {
            question: "How do I delete my account?",
            answer:
              "Use account settings or contact support for deletion and privacy requests."
          },
          {
            question: "How do refunds work?",
            answer:
              "Refund eligibility depends on timing, usage, and billing status. See the refund policy for details."
          }
        ]
      }
    ],
    ctaTitle: "Still need help",
    ctaCopy: `Reach ${SUPPORT_EMAIL} if your question is not covered here.`,
    ctaHref: "/contact",
    ctaLabel: "Contact DutyPe"
  }
};

function titleCaseFromSlug(value: string) {
  return value
    .split("-")
    .filter(Boolean)
    .map((part) => cityNames[part] ?? part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function normalizeCity(citySlug: string) {
  return cityNames[citySlug] ?? titleCaseFromSlug(citySlug);
}

function getCategorySlug(slug: string) {
  const keys = Object.keys(categoryDetails).sort((left, right) => right.length - left.length);
  for (const key of keys) {
    const prefix = `${key}-jobs`;
    if (slug === prefix) {
      return { category: key, city: null as string | null };
    }
    if (slug.startsWith(`${prefix}-`)) {
      return {
        category: key,
        city: slug.slice(prefix.length + 1)
      };
    }
  }
  return null;
}

function generateCityJobsPage(slug: string): LegacyPageDescriptor {
  const citySlug = slug.replace(/^jobs-in-/, "");
  const city = normalizeCity(citySlug);

  return {
    slug,
    title: `Jobs in ${city}`,
    description: `Find local jobs in ${city} including delivery, driver, maid, helper, warehouse, and part-time roles with DutyPe.`,
    eyebrow: "City landing page",
    intro:
      `Browse local work in ${city} without relying on dozens of disconnected HTML files. This route replaces the old static city page pattern with one reusable Next.js surface.`,
    highlights: [
      `${city} workers want nearby roles with clear pay and clear location.`,
      "Hyperlocal search, safe contact, and trust cues matter more than generic job boards.",
      "Open the app for live listings and application status."
    ],
    blocks: [
      {
        kind: "list",
        title: `Popular roles in ${city}`,
        items: [
          `Delivery jobs in ${city}`,
          `Driver jobs in ${city}`,
          `Maid and cleaning jobs in ${city}`,
          `Cook and helper jobs in ${city}`,
          `Warehouse and part-time jobs in ${city}`
        ]
      },
      {
        kind: "list",
        title: `Why workers choose DutyPe in ${city}`,
        items: [
          "Nearby jobs instead of broad nationwide listings.",
          "Direct worker-employer connection without middlemen.",
          "Safety messaging around fake jobs and fee scams.",
          "Fast app-based application flow and status tracking."
        ]
      },
      {
        kind: "list",
        title: "Typical pay ranges",
        items: [
          "Entry support roles often start near Rs 10,000 to Rs 14,000 per month.",
          "Delivery and driving roles can move into higher monthly totals with incentives.",
          "Part-time and daily-wage roles vary based on shift length and area."
        ]
      }
    ],
    ctaTitle: `Open live jobs in ${city}`,
    ctaCopy: "The app is the live source for current openings, trust signals, and application actions.",
    ctaHref: "/app/worker/jobs",
    ctaLabel: "Browse live jobs"
  };
}

function generateJobsNearMePage(): LegacyPageDescriptor {
  return {
    slug: "jobs-near-me",
    title: "Jobs Near Me",
    description:
      "Find nearby jobs across delivery, housekeeping, driving, warehouse, and part-time work with DutyPe.",
    eyebrow: "Discovery",
    intro:
      "The old site had a dedicated jobs-near-me landing page. In the new system, it becomes a reusable discovery route focused on nearby work and faster conversion into the app.",
    highlights: [
      "Nearby roles reduce commute friction.",
      "Workers need clear pay, location, and trust cues.",
      "This route should funnel users cleanly into the app."
    ],
    blocks: [
      {
        kind: "list",
        title: "What workers usually want",
        items: [
          "Jobs close to home.",
          "Simple apply flow with no long forms.",
          "Clear salary, shift, and employer details.",
          "Protection from fake jobs or fee traps."
        ]
      },
      {
        kind: "list",
        title: "Popular nearby role types",
        items: [
          "Delivery and driver jobs",
          "Maid, cleaner, and housekeeping jobs",
          "Cook and helper jobs",
          "Warehouse and daily-wage jobs",
          "Part-time local roles"
        ]
      }
    ],
    ctaTitle: "See live nearby jobs",
    ctaCopy: "Use the worker app flow for live listings, saved jobs, and applications.",
    ctaHref: "/app/worker/jobs",
    ctaLabel: "Open worker jobs"
  };
}

function generateCategoryPage(
  slug: string,
  category: string,
  city: string | null
): LegacyPageDescriptor {
  const details = categoryDetails[category];
  const cityLabel = city ? normalizeCity(city) : null;
  const title = cityLabel ? `${details.label} in ${cityLabel}` : details.label;

  return {
    slug,
    title,
    description: cityLabel
      ? `Find ${details.shortLabel.toLowerCase()} openings in ${cityLabel} with DutyPe.`
      : `Find ${details.shortLabel.toLowerCase()} openings near you with DutyPe.`,
    eyebrow: cityLabel ? "Category and city page" : "Category page",
    intro: cityLabel
      ? `${details.intro} This route replaces the old ${slug}.html page with a reusable Next page for ${cityLabel}.`
      : `${details.intro} This route replaces the old standalone category page with a reusable React template.`,
    highlights: [
      details.salary,
      "Direct app-open flow matters more than static HTML.",
      "Workers need trust, safety, and nearby availability signals."
    ],
    blocks: [
      {
        kind: "list",
        title: cityLabel
          ? `${details.shortLabel} roles people search in ${cityLabel}`
          : `Common ${details.shortLabel.toLowerCase()} roles`,
        items: details.roles
      },
      {
        kind: "list",
        title: "Why DutyPe fits this category",
        items: [
          "Fast local discovery and application flow.",
          "Clear role, pay, and shift framing.",
          "Worker safety messaging around fake jobs and advance fees.",
          "Clean handoff into the mobile app for live actions."
        ]
      },
      {
        kind: "list",
        title: "What to check before applying",
        items: [
          "Exact area and travel distance.",
          "Shift timing and weekly off details.",
          "Whether pay is monthly, daily, or incentive-based.",
          "Employer credibility and communication quality."
        ]
      }
    ],
    ctaTitle: "Continue in the app",
    ctaCopy: "The live marketplace, applications, and chat all happen in the DutyPe product app.",
    ctaHref: "/app",
    ctaLabel: "Open DutyPe"
  };
}

export function resolveLegacyPage(slug: string): LegacyPageDescriptor | null {
  if (staticPages[slug]) {
    return staticPages[slug];
  }

  if (slug === "jobs-near-me") {
    return generateJobsNearMePage();
  }

  if (slug.startsWith("jobs-in-")) {
    return generateCityJobsPage(slug);
  }

  const categoryMatch = getCategorySlug(slug);
  if (categoryMatch) {
    return generateCategoryPage(slug, categoryMatch.category, categoryMatch.city);
  }

  return null;
}

export function getKnownLegacySlugs() {
  const citySlugs = Object.keys(cityNames).map((city) => `jobs-in-${city}`);
  const categorySlugs = Object.keys(categoryDetails).flatMap((category) => {
    const prefix = `${category}-jobs`;
    return [prefix, ...Object.keys(cityNames).map((city) => `${prefix}-${city}`)];
  });

  return Array.from(
    new Set([...Object.keys(staticPages), "jobs-near-me", ...citySlugs, ...categorySlugs])
  ).sort();
}

export function buildDeepLinkBundle(kind: DeepLinkKind, entityId?: string) {
  const normalizedId = entityId?.trim();
  const appScheme =
    kind === "home"
      ? "dutype://home"
      : normalizedId
        ? `dutype://${kind}/${normalizedId}`
        : `dutype://${kind}`;

  const intentPath =
    kind === "home"
      ? "home"
      : normalizedId
        ? `${kind}/${normalizedId}`
        : kind;

  const androidIntent = `intent://${intentPath}#Intent;scheme=dutype;package=com.dutype.app;S.browser_fallback_url=${encodeURIComponent(
    PLAY_STORE_URL
  )};end`;

  return {
    appScheme,
    androidIntent,
    playStoreUrl: PLAY_STORE_URL,
    appStoreUrl: APP_STORE_URL
  };
}

export function getPublicJobRouteData(jobId: string): PublicJobRouteData {
  const normalized = titleCaseFromSlug(jobId);
  return {
    id: jobId,
    title: normalized,
    company: "DutyPe partner employer",
    location: "Local route, exact area inside the app",
    salary: "Pay details available in live listing",
    shift: "Shift details available in live listing",
    summary:
      "Open the DutyPe app to view the full job, employer trust signals, and application actions.",
    trustSignals: [
      "Clear location and pay should be visible before applying.",
      "Workers should never pay to get access to a job.",
      "Live listing status belongs in the app, not in stale static HTML."
    ],
    features: [
      "One-tap app handoff",
      "Nearby job discovery",
      "Employer trust and reporting signals"
    ]
  };
}

export function getPublicWorkerRouteData(workerId: string): PublicWorkerRouteData {
  const normalized = titleCaseFromSlug(workerId);
  return {
    id: workerId,
    name: normalized,
    role: "Worker profile",
    city: "Location shared safely inside the app",
    summary:
      "Open the DutyPe app to view verified profile details, ratings, and hiring actions.",
    trustSignals: [
      "Keep personal contact details inside the app.",
      "Public routes should show trust signals, not sensitive fields.",
      "Hiring actions should stay inside the product flow."
    ],
    features: [
      "Verified profiles",
      "Ratings and reviews",
      "Safer direct contact"
    ]
  };
}

export function getPublicEmployerRouteData(employerId: string): PublicEmployerRouteData {
  const normalized = titleCaseFromSlug(employerId);
  return {
    id: employerId,
    company: normalized,
    industry: "Local hiring business",
    city: "City and job areas inside the app",
    summary:
      "Open the DutyPe app to see live jobs, trust signals, and employer-side actions.",
    trustSignals: [
      "Workers should be able to inspect employer credibility.",
      "Public employer pages should route cleanly into live job listings.",
      "Trust tier, response behavior, and active jobs matter more than static copy."
    ],
    features: [
      "Live openings",
      "Trust and response signals",
      "In-app hiring actions"
    ]
  };
}
