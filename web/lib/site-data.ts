export type PlatformLayer = {
  title: string;
  summary: string;
  tech: string[];
  output: string;
};

export type RolloutTrack = {
  title: string;
  summary: string;
  routes: string[];
  outcome: string;
};

export type RouteBlueprint = {
  title: string;
  href: string;
  summary: string;
  note: string;
};

type JobPageData = {
  id: string;
  title: string;
  company: string;
  location: string;
  salary: string;
  shift: string;
  summary: string;
  trustBadge: string;
  signals: string[];
  nextSteps: string[];
};

type WorkerPageData = {
  id: string;
  name: string;
  role: string;
  city: string;
  trustBadge: string;
  completionRate: string;
  jobsCompleted: string;
  summary: string;
  strengths: string[];
  visibilityRules: string[];
};

type EmployerPageData = {
  id: string;
  company: string;
  industry: string;
  city: string;
  trustTier: string;
  responseRate: string;
  jobsPosted: string;
  summary: string;
  hiringSignals: string[];
  controls: string[];
};

export const siteMeta = {
  name: "DutyPe",
  strapline: "React web layer for an Android-first local jobs platform",
  description:
    "Next.js foundation for DutyPe marketing, SEO landing pages, public deep-link routes, and a future admin console.",
  repoSnapshot: {
    kotlinFiles: 310,
    pythonFiles: 32,
    functionFiles: 9,
    htmlFiles: 61
  }
};

export const platformLayers: PlatformLayer[] = [
  {
    title: "Android product",
    summary:
      "The real product is the Kotlin plus Jetpack Compose app for workers and employers, with deep links, onboarding, chat, ratings, and role-based flows.",
    tech: ["Kotlin", "Jetpack Compose", "Hilt", "Room", "WorkManager"],
    output: "Mobile-first hiring experience"
  },
  {
    title: "Firebase core",
    summary:
      "Firestore, Auth, Messaging, Storage, Hosting, and Cloud Functions carry the operational platform and event-driven business logic.",
    tech: ["Firestore", "Auth", "FCM", "Storage", "Hosting", "Cloud Functions"],
    output: "Realtime marketplace backbone"
  },
  {
    title: "AI trust backend",
    summary:
      "FastAPI plus Azure OpenAI handles detection, fraud signals, worker protection, and explanation endpoints. Enforcement stays rules-driven.",
    tech: ["Python", "FastAPI", "Azure OpenAI", "Pydantic"],
    output: "Fraud and trust decision support"
  },
  {
    title: "Current web layer",
    summary:
      "The repo already has static HTML pages, app-open redirect pages, and a vanilla Firebase admin panel. This is what the React app should replace step by step.",
    tech: ["HTML", "CSS", "JavaScript", "Firebase Hosting"],
    output: "SEO pages and admin stopgap"
  }
];

export const rolloutTracks: RolloutTrack[] = [
  {
    title: "Public growth site",
    summary:
      "Replace the hand-built marketing and category pages with reusable React layouts, route metadata, and a single content system.",
    routes: ["/", "/jobs", "/jobs/[jobId]"],
    outcome: "SEO surface that is maintainable instead of duplicated"
  },
  {
    title: "Profile deep-link pages",
    summary:
      "Move worker and employer public pages into a server-rendered route system that can emit clean metadata and open the Android app reliably.",
    routes: ["/worker/[workerId]", "/employer/[employerId]"],
    outcome: "Better social share previews and route consistency"
  },
  {
    title: "Admin console",
    summary:
      "Replace the current browser-import Firebase admin pages with a structured dashboard that can later use proper auth guards and shared components.",
    routes: ["/admin", "/admin/users", "/admin/jobs", "/admin/applications", "/admin/referrals", "/admin/notifications", "/admin/announcements"],
    outcome: "Operational UI that can actually scale"
  }
];

export const routeBlueprints: RouteBlueprint[] = [
  {
    title: "Sample public job route",
    href: "/jobs/demo-delivery-partner",
    summary: "Shows how a public job page can be SSR-friendly and app-link-aware.",
    note: "Use Firestore job data and generate route metadata server-side later."
  },
  {
    title: "Sample worker profile route",
    href: "/worker/demo-worker",
    summary: "Blueprint for public worker cards, trust signals, and deep-link opening.",
    note: "This should eventually replace the current split between Hosting rewrites and Cloud Function HTML."
  },
  {
    title: "Sample employer route",
    href: "/employer/demo-employer",
    summary: "Employer public profile and hiring trust surface.",
    note: "Useful for shareable employer pages and job credibility."
  },
  {
    title: "Admin dashboard start",
    href: "/admin",
    summary: "Operational workspace for jobs, applications, announcements, and moderation.",
    note: "Current repo already has admin HTML pages; this route set is the React migration path."
  }
];

export const cityLandingTargets = [
  "Hyderabad",
  "Bangalore",
  "Delhi",
  "Mumbai",
  "Vijayawada",
  "Warangal"
];

export const adminModules = [
  {
    title: "Users and roles",
    summary: "Review account mix, referral-code issuance, and the transition from single-role to dual-role users."
  },
  {
    title: "Jobs and moderation",
    summary: "Flag risky jobs, verify metadata, review expiry state, and manage visibility."
  },
  {
    title: "Applications operations",
    summary: "Track response SLA, employer silence, and high-priority worker application queues."
  },
  {
    title: "Referrals and withdrawals",
    summary: "Monitor reward completion and process payout requests without leaving the admin workspace."
  },
  {
    title: "Announcements",
    summary: "Control carousels, banners, push-notification copy, and deep-link campaigns."
  }
];

export const sampleModerationQueue = [
  {
    title: "Delivery Partner",
    city: "Hyderabad",
    risk: "High",
    reason: "Urgency language plus suspicious contact reuse"
  },
  {
    title: "House Cook",
    city: "Bangalore",
    risk: "Medium",
    reason: "Missing pay clarity and incomplete location fields"
  },
  {
    title: "Warehouse Helper",
    city: "Delhi",
    risk: "Low",
    reason: "Needs category normalization before publishing"
  }
];

export const sampleApplications = [
  {
    worker: "Rani Kumari",
    role: "Cleaner",
    state: "Needs employer response",
    age: "Viewed 19 hours ago"
  },
  {
    worker: "Imran Shaik",
    role: "Delivery Partner",
    state: "Shortlisted",
    age: "Updated 42 minutes ago"
  },
  {
    worker: "Lakshmi Devi",
    role: "Profile incomplete",
    state: "Needs worker correction",
    age: "Applied today"
  }
];

export const sampleAnnouncements = [
  {
    title: "Referral boost week",
    channel: "Worker home banner",
    status: "Ready to publish"
  },
  {
    title: "Employer response SLA reminder",
    channel: "Push plus in-app",
    status: "Copy draft"
  },
  {
    title: "App update 2.4 rollout",
    channel: "Play Store deep link",
    status: "Live in Android only"
  }
];

const jobSamples: Record<string, JobPageData> = {
  "demo-delivery-partner": {
    id: "demo-delivery-partner",
    title: "Delivery Partner",
    company: "QuickDrop Logistics",
    location: "Madhapur, Hyderabad",
    salary: "Rs 18,000 to Rs 26,000 per month",
    shift: "Flexible day shift",
    summary:
      "Public job pages should feel trustworthy, rank well, and open the app without relying on brittle static HTML templates.",
    trustBadge: "AI reviewed plus rule-validated",
    signals: [
      "Clear salary range instead of vague earnings copy",
      "Hyperlocal location and shift details",
      "Structured metadata for shares and search"
    ],
    nextSteps: [
      "Read job document from Firestore by slug or document id",
      "Emit route metadata from server data",
      "Attach Android App Link and Play Store fallback"
    ]
  },
  "demo-home-chef": {
    id: "demo-home-chef",
    title: "Home Cook",
    company: "Urban Family Services",
    location: "Indiranagar, Bangalore",
    salary: "Rs 14,500 to Rs 19,000 per month",
    shift: "Morning household shift",
    summary:
      "This route shape is ready for category-city SEO pages and shareable job cards without duplicating dozens of standalone HTML files.",
    trustBadge: "Employer score visible",
    signals: [
      "Category-first SEO path",
      "Reusable content blocks",
      "Structured app-open CTA"
    ],
    nextSteps: [
      "Connect to Firestore job collection",
      "Add schema.org job posting metadata",
      "Reuse route design for city landing pages"
    ]
  }
};

const workerSamples: Record<string, WorkerPageData> = {
  "demo-worker": {
    id: "demo-worker",
    name: "Rani Kumari",
    role: "Housekeeping and cleaning specialist",
    city: "Hyderabad",
    trustBadge: "DutyPe verified profile",
    completionRate: "96 percent profile completeness",
    jobsCompleted: "128 completed jobs",
    summary:
      "Worker public routes should show enough trust and capability information for employers while still pushing the real action into the Android app.",
    strengths: [
      "High completion consistency",
      "Role-aligned skills and local availability",
      "Share-friendly public identity surface"
    ],
    visibilityRules: [
      "Respect worker privacy fields",
      "Show only approved public profile data",
      "Deep-link employer into the app for contact or hiring"
    ]
  }
};

const employerSamples: Record<string, EmployerPageData> = {
  "demo-employer": {
    id: "demo-employer",
    company: "GreenKart Retail",
    industry: "Retail operations",
    city: "Bangalore",
    trustTier: "Silver trust tier",
    responseRate: "89 percent response rate",
    jobsPosted: "42 active and historical postings",
    summary:
      "Employer public pages can improve credibility for job seekers and provide a clean landing surface for shared jobs and company trust signals.",
    hiringSignals: [
      "Visible trust tier and response discipline",
      "Role-specific hiring footprint",
      "Direct bridge into job listings"
    ],
    controls: [
      "Later connect to employer score service",
      "Surface response SLA and moderation state",
      "Link public route to employer-owned active jobs"
    ]
  }
};

function titleFromSlug(value: string): string {
  return value
    .split("-")
    .filter(Boolean)
    .map((part) => part[0]?.toUpperCase() + part.slice(1))
    .join(" ");
}

export function getJobPageData(jobId: string): JobPageData {
  if (jobSamples[jobId]) {
    return jobSamples[jobId];
  }

  const title = titleFromSlug(jobId);
  return {
    id: jobId,
    title,
    company: "DutyPe Partner Employer",
    location: "Hyperlocal route placeholder",
    salary: "Salary to be fetched from Firestore",
    shift: "Shift data pending backend integration",
    summary:
      "This route is scaffolded and ready to be backed by live Firestore job content and deep-link metadata.",
    trustBadge: "React route scaffold",
    signals: [
      "Server-rendered route path is ready",
      "Metadata can be generated per job",
      "App-open CTA can be standardized"
    ],
    nextSteps: [
      "Fetch the matching job document",
      "Map Firestore fields into route props",
      "Replace placeholder content with real job data"
    ]
  };
}

export function getWorkerPageData(workerId: string): WorkerPageData {
  if (workerSamples[workerId]) {
    return workerSamples[workerId];
  }

  const name = titleFromSlug(workerId);
  return {
    id: workerId,
    name,
    role: "Worker profile route placeholder",
    city: "City pending profile lookup",
    trustBadge: "Public profile shell",
    completionRate: "Profile metrics pending",
    jobsCompleted: "History pending",
    summary:
      "This route is ready to become the React replacement for current worker public landing pages.",
    strengths: [
      "Server-rendered public profile route",
      "Controlled visibility for public fields",
      "Consistent app-open entry point"
    ],
    visibilityRules: [
      "Read only public-safe fields",
      "Keep contact actions inside the app",
      "Hide sensitive worker attributes by default"
    ]
  };
}

export function getEmployerPageData(employerId: string): EmployerPageData {
  if (employerSamples[employerId]) {
    return employerSamples[employerId];
  }

  const company = titleFromSlug(employerId);
  return {
    id: employerId,
    company,
    industry: "Employer profile route placeholder",
    city: "City pending employer lookup",
    trustTier: "Trust tier pending",
    responseRate: "Response metrics pending",
    jobsPosted: "Job stats pending",
    summary:
      "This route is ready to absorb employer trust data from Firestore and the scoring system.",
    hiringSignals: [
      "Public employer credibility layer",
      "Shareable profile metadata",
      "Job-listing entry point"
    ],
    controls: [
      "Connect to employer score service later",
      "Pull active jobs by employer id",
      "Expose moderation-safe public information only"
    ]
  };
}
