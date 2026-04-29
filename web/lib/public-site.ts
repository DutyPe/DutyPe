export const PLAY_STORE_URL =
  "https://play.google.com/store/apps/details?id=com.dutype.app";
export const APP_STORE_URL = "https://apps.apple.com/app/dutype";
export const SUPPORT_EMAIL = "dutypein@gmail.com";
export const FEEDBACK_EMAIL = "dutypefeedback@gmail.com";
export const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL || "https://dutype.in";

export const siteMeta = {
  name: "DutyPe",
  companyName: "KGPV INNOVATION SOLUTIONS PRIVATE LIMITED",
  strapline: "Local jobs near you",
  description:
    "Find local jobs near you. Connect workers with employers instantly. No middlemen, no fees.",
  supportEmail: SUPPORT_EMAIL,
  registeredOffice: "India"
};

export const coreSeoKeywords = [
  "jobs near me",
  "jobs near me hiring now",
  "jobs near me for freshers",
  "jobs near me no experience",
  "jobs near me 10th pass",
  "jobs near me 12th pass",
  "part time jobs near me",
  "night shift jobs near me",
  "daily wage jobs near me",
  "local jobs",
  "local jobs hiring immediately",
  "delivery jobs near me",
  "driver jobs near me",
  "maid jobs near me",
  "cook jobs near me",
  "helper jobs near me",
  "security guard jobs near me",
  "warehouse jobs near me",
  "cleaner jobs near me",
  "peon jobs near me",
  "retail jobs near me",
  "blue collar jobs india",
  "worker hiring app",
  "hire workers near me",
  "job search app india",
  "free job app",
  "no middleman jobs",
  "verified employer jobs",
  "instant job apply",
  "job vacancy near me",
  "DutyPe",
  "DutyPe app",
  "dutype jobs"
];

export const primaryNav = [
  { href: "/", label: "Home" },
  { href: "/jobs", label: "Jobs" },
  { href: "/jobs-near-me", label: "Jobs Near Me" },
  { href: "/safety", label: "Safety" },
  { href: "/faq", label: "FAQ" },
  { href: "/contact", label: "Contact" }
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
      { href: "/refund", label: "Refunds" },
      { href: "/accountdeletion", label: "Delete Account" }
    ]
  }
];

export const homeBenefits = [
  {
    title: "Jobs near you",
    description:
      "Find jobs within walking distance or a short commute. DutyPe shows you openings based on your exact location — 1 km, 5 km, or 10 km radius."
  },
  {
    title: "Instant apply",
    description:
      "Apply to jobs with one tap. No lengthy forms, no waiting. Your profile is shared directly with the employer so you can get hired faster."
  },
  {
    title: "Daily payments",
    description:
      "Many jobs offer daily or weekly payments. Get paid for your work quickly without waiting for monthly salary cycles."
  },
  {
    title: "Safe and verified",
    description:
      "All employers are verified for your safety. Report suspicious activity anytime. Your safety is our top priority — never pay to get a job."
  },
  {
    title: "Direct chat",
    description:
      "Chat directly with employers. No middlemen taking your money. Negotiate terms and discuss job details instantly through the app."
  },
  {
    title: "Free for workers",
    description:
      "100 percent free for job seekers. Never pay to find a job on DutyPe. We believe finding work should be accessible to everyone."
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

export const homeStats = [
  { value: "10K+", label: "Active Jobs" },
  { value: "50K+", label: "Workers" },
  { value: "5K+", label: "Employers" },
  { value: "100+", label: "Cities" }
];

export const cityLandingTargets = [
  "Hyderabad",
  "Bangalore",
  "Delhi",
  "Mumbai",
  "Pune",
  "Chennai",
  "Kolkata",
  "Ahmedabad",
  "Jaipur",
  "Lucknow",
  "Noida",
  "Gurgaon",
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
  seoKeywords?: string[];
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
  bengaluru: "Bengaluru",
  delhi: "Delhi",
  mumbai: "Mumbai",
  pune: "Pune",
  chennai: "Chennai",
  kolkata: "Kolkata",
  ahmedabad: "Ahmedabad",
  jaipur: "Jaipur",
  lucknow: "Lucknow",
  chandigarh: "Chandigarh",
  indore: "Indore",
  bhopal: "Bhopal",
  nagpur: "Nagpur",
  surat: "Surat",
  kochi: "Kochi",
  coimbatore: "Coimbatore",
  visakhapatnam: "Visakhapatnam",
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
  tirupati: "Tirupati",
  noida: "Noida",
  gurgaon: "Gurgaon",
  ghaziabad: "Ghaziabad",
  faridabad: "Faridabad",
  thane: "Thane",
  "navi-mumbai": "Navi Mumbai",
  mysore: "Mysore",
  mangalore: "Mangalore",
  hubli: "Hubli",
  madurai: "Madurai",
  trichy: "Trichy",
  vadodara: "Vadodara",
  rajkot: "Rajkot",
  patna: "Patna",
  ranchi: "Ranchi",
  bhubaneswar: "Bhubaneswar",
  dehradun: "Dehradun"
};

const cityLocalAreas: Record<string, string[]> = {
  hyderabad: [
    "Madhapur",
    "Gachibowli",
    "Kondapur",
    "Kukatpally",
    "Miyapur",
    "Ameerpet",
    "LB Nagar",
    "Uppal",
    "Secunderabad",
    "Banjara Hills",
    "Jubilee Hills",
    "Manikonda"
  ],
  vijayawada: [
    "Benz Circle",
    "Auto Nagar",
    "Governorpet",
    "Patamata",
    "Moghalrajpuram",
    "Kanuru",
    "Poranki",
    "Bhavanipuram"
  ],
  warangal: [
    "Hanamkonda",
    "Kazipet",
    "Subedari",
    "Nakkalagutta",
    "Kothawada",
    "Fort Warangal"
  ],
  khammam: [
    "Wyra Road",
    "Mamillagudem",
    "Khanapuram Haveli",
    "Burhanpuram",
    "Mustafa Nagar",
    "Nehru Nagar"
  ],
  karimnagar: [
    "Mukrampura",
    "Kothirampur",
    "Jyothi Nagar",
    "Mankammathota",
    "Srinagar Colony",
    "Rekurthi"
  ],
  kurnool: [
    "Nandyal Road",
    "Kallur",
    "Ashok Nagar",
    "B Camp",
    "Budhwar Peta",
    "Joharapuram"
  ],
  visakhapatnam: [
    "Dwaraka Nagar",
    "MVP Colony",
    "Gajuwaka",
    "Madhurawada",
    "Akkayyapalem",
    "Seethammadhara"
  ],
  guntur: [
    "Brodipet",
    "Arundelpet",
    "Lakshmipuram",
    "Kothapeta",
    "Nallapadu",
    "Amaravathi Road"
  ],
  tirupati: [
    "Tiruchanur",
    "Renigunta Road",
    "Korlagunta",
    "M R Palli",
    "Leela Mahal Circle",
    "Alipiri"
  ],
  kakinada: [
    "Jagannaickpur",
    "Sarpavaram",
    "Bhanugudi",
    "Ramanayyapeta",
    "Indrapalem",
    "Port Area"
  ],
  nellore: [
    "Dargamitta",
    "Balaji Nagar",
    "Stonehouse Pet",
    "Magunta Layout",
    "Haranathapuram",
    "Nawabpet"
  ],
  nizamabad: [
    "Bodhan Road",
    "Kanteshwar",
    "Dichpally",
    "Armoor Road",
    "Subhash Nagar",
    "Vinayak Nagar"
  ],
  rajahmundry: [
    "Danavaipeta",
    "AV Appa Rao Road",
    "Kambala Cheruvu",
    "Morampudi",
    "Alcot Gardens",
    "Seethampeta"
  ],
  anantapur: [
    "Sapthagiri Circle",
    "Ram Nagar",
    "Old Town",
    "Rudrampeta",
    "Srinivas Nagar",
    "Housing Board"
  ]
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
  },
  retail: {
    label: "Retail jobs",
    shortLabel: "Retail",
    intro:
      "Sales, cashier, and store assistant roles across supermarkets, showrooms, malls, and local shops.",
    salary: "Usually Rs 10,000 to Rs 25,000 per month.",
    roles: [
      "Sales associate",
      "Cashier",
      "Store keeper",
      "Supermarket staff",
      "Showroom assistant",
      "Mall staff"
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
      "We collect only what is needed to connect workers with employers, support safety checks, and keep the marketplace reliable.",
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
      },
      {
        kind: "copy",
        title: "Location data",
        paragraphs: [
          "Location is collected only when the app is open. You can disable location access in your device settings at any time."
        ]
      },
      {
        kind: "copy",
        title: "Children's privacy",
        paragraphs: [
          "DutyPe is not intended for users under 18 years of age. We do not knowingly collect personal information from children."
        ],
        tone: "warning"
      },
      {
        kind: "copy",
        title: "Policy updates",
        paragraphs: [
          "Last updated: January 11, 2026. We may update this privacy policy from time to time. Changes will be reflected on this page with a revised date."
        ],
        tone: "neutral"
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
      "DutyPe is a local jobs marketplace connecting workers with employers directly. Use the platform honestly and safely.",
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
      },
      {
        kind: "list",
        title: "Referral program details",
        intro: "DutyPe offers a referral rewards program subject to the following terms:",
        items: [
          "All registered users can participate in the referral program.",
          "Reward: Rs 25 per successful referral when the referred user completes their profile.",
          "Milestone bonuses: Additional rewards at 5, 10, 15, 25, and 50 successful referrals.",
          "Minimum Rs 50 required for withdrawal.",
          "Withdrawals are processed within 3 to 5 business days.",
          "This is a legitimate referral program. You earn rewards for referring new users, not for recruiting people to recruit others.",
          "Referral rewards are promotional credits that can be withdrawn as cash after verification.",
          "All referrals are subject to verification to prevent fraud.",
          "DutyPe reserves the right to modify, suspend, or terminate the referral program at any time."
        ]
      },
      {
        kind: "list",
        title: "Prohibited referral activities",
        items: [
          "Creating fake accounts to claim referral rewards.",
          "Using automated tools or bots to generate referrals.",
          "Spamming or unsolicited mass messaging.",
          "Misrepresenting the referral program or DutyPe services.",
          "Any fraudulent activity will result in immediate account suspension and forfeiture of all rewards."
        ],
        tone: "warning"
      },
      {
        kind: "list",
        title: "Tax compliance and RBI guidelines",
        intro: "Referral rewards are considered taxable income under Indian tax laws.",
        items: [
          "Users are solely responsible for reporting and paying taxes on referral earnings.",
          "KYC verification required for withdrawals exceeding Rs 10,000 per year.",
          "PAN card details required for withdrawals exceeding Rs 50,000 per year.",
          "Tax Deducted at Source (TDS) may be applicable as per Indian Income Tax Act.",
          "All payment transactions comply with Reserve Bank of India (RBI) guidelines.",
          "TDS deductions will be reflected in your Form 26AS for tax filing."
        ]
      },
      {
        kind: "table",
        title: "Withdrawal limits and verification tiers",
        columns: ["Tier", "Requirement"],
        rows: [
          ["Up to Rs 10,000 per year", "Basic verification (phone number, email)"],
          ["Rs 10,001 to Rs 50,000 per year", "KYC verification required (Aadhaar or Voter ID)"],
          ["Above Rs 50,000 per year", "PAN card mandatory plus full KYC"],
          ["Maximum withdrawal", "Rs 1,000 per day"]
        ]
      },
      {
        kind: "copy",
        title: "Governing law",
        paragraphs: [
          "These terms are governed by the laws of India. Any disputes will be resolved in the courts of Hyderabad, Telangana."
        ]
      },
      {
        kind: "copy",
        title: "Terms updates",
        paragraphs: [
          "Last updated: January 11, 2026. These terms may be updated from time to time. Continued use of DutyPe after changes constitutes acceptance."
        ],
        tone: "neutral"
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
      "Worker safety is at the heart of DutyPe. Follow these guidelines to protect yourself from fake jobs, scams, and unsafe situations.",
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
          "Communication only through private messaging with no clear company identity.",
          "Requests for personal documents before hiring.",
          "Interview location is a private residence.",
          "Communication only through personal WhatsApp."
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
      },
      {
        kind: "contact",
        title: "Emergency contacts",
        items: [
          {
            label: "Police",
            value: "100",
            note: "For immediate safety situations."
          },
          {
            label: "Women helpline",
            value: "1091",
            note: "National Commission for Women helpline."
          },
          {
            label: "Cyber crime",
            value: "1930",
            note: "National Cyber Crime helpline."
          },
          {
            label: "DutyPe support",
            value: SUPPORT_EMAIL,
            note: "Report issues by email or using the in-app report flow.",
            href: `mailto:${SUPPORT_EMAIL}`
          }
        ]
      },
      {
        kind: "list",
        title: "Data security",
        items: [
          "All data is encrypted in transit and at rest.",
          "We use secure cloud infrastructure through Google Cloud.",
          "Regular security audits and updates.",
          "Your personal data is never sold to third parties.",
          "You can delete your account and data anytime."
        ]
      },
      {
        kind: "list",
        title: "Community guidelines",
        items: [
          "Treat everyone with respect.",
          "Be honest in your profile and communications.",
          "Honor your commitments.",
          "Report violations to keep the community safe.",
          "Help others by sharing your experiences."
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
      },
      {
        kind: "list",
        title: "Subscription cancellation",
        items: [
          "You can cancel your subscription anytime from the app.",
          "Cancellation takes effect at the end of the current billing period.",
          "You retain access until the subscription expires.",
          "No partial refunds for unused days in the current period."
        ]
      },
      {
        kind: "list",
        title: "Auto-renewal",
        items: [
          "Subscriptions auto-renew unless cancelled.",
          "You will receive a reminder before renewal.",
          "Cancel at least 24 hours before renewal to avoid charges."
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
      "Choose the right contact path so your queries get resolved faster. For the quickest response, use the in-app support option.",
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
            label: "Legal",
            value: SUPPORT_EMAIL,
            note: "Legal inquiries and compliance.",
            href: `mailto:${SUPPORT_EMAIL}`
          },
          {
            label: "Report abuse",
            value: SUPPORT_EMAIL,
            note: "Report fake jobs, scams, or harassment.",
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
      },
      {
        kind: "copy",
        title: "Business address",
        paragraphs: [
          "DutyPe, Hyderabad, Telangana, India — 500001."
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
      "Answers to the most common questions from workers, employers, and users about DutyPe, jobs, payments, and safety.",
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
          },
          {
            question: "How do I know if my application was seen?",
            answer:
              "Go to the My Jobs tab to see all your applications and their status including Pending, Under Review, and Accepted."
          },
          {
            question: "How do I withdraw my application?",
            answer:
              "Go to My Jobs, find your application, and tap Withdraw. You can withdraw anytime before being hired."
          },
          {
            question: "How do I report a suspicious job?",
            answer:
              "Open the job listing, tap the Report button, select a reason, and submit. We review all reports within 24 hours."
          },
          {
            question: "Can I change my phone number?",
            answer:
              `Contact support at ${SUPPORT_EMAIL} to change your registered phone number.`
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
              "Use the employer dashboard to review applications and contact workers through in-app chat or call features."
          },
          {
            question: "Can I edit my job after posting?",
            answer:
              "Yes. You can edit job details anytime from your job management screen."
          },
          {
            question: "How do I mark a position as filled?",
            answer:
              "Go to your job listing and tap Mark as Filled. This will stop new applications."
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
          },
          {
            question: "What payment methods are accepted?",
            answer:
              "We accept UPI, credit and debit cards, net banking, and wallets through Razorpay."
          },
          {
            question: "How do I cancel my subscription?",
            answer:
              "Go to Settings, then Subscription, then Cancel. Your access continues until the end of the billing period."
          },
          {
            question: "How do I get a refund?",
            answer:
              `Email ${SUPPORT_EMAIL} within 24 hours of purchase. See our Refund Policy for full details.`
          }
        ]
      }
    ],
    ctaTitle: "Still need help",
    ctaCopy: `Reach ${SUPPORT_EMAIL} if your question is not covered here.`,
    ctaHref: "/contact",
    ctaLabel: "Contact DutyPe"
  },
  "account-deletion": {
    slug: "account-deletion",
    title: "Request Account Deletion",
    description:
      "Request deletion of your DutyPe account and all associated personal data. We process deletion requests within 7 business days.",
    eyebrow: "Privacy",
    intro:
      "You have the right to request deletion of your DutyPe account and personal data at any time. Send us an email and we will process your request.",
    highlights: [
      "Your data deletion request will be processed within 7 business days.",
      "All personal data including profile, applications, and chat history will be permanently removed.",
      "This action cannot be undone."
    ],
    blocks: [
      {
        kind: "copy",
        title: "How to request account deletion",
        paragraphs: [
          `To request deletion of your account and all associated data, send an email to ${SUPPORT_EMAIL} with the subject line "Account Deletion Request".`,
          "Include the phone number registered with your DutyPe account so we can locate and verify your account.",
          "You will receive a confirmation email once your request has been received and another when the deletion is complete."
        ],
        tone: "highlight"
      },
      {
        kind: "list",
        title: "What gets deleted",
        intro: "When your account is deleted, the following data is permanently removed:",
        items: [
          "Your profile information (name, phone number, photo, skills, and preferences).",
          "All job applications and application history.",
          "Chat messages and communication history.",
          "Referral data and earnings history.",
          "Saved jobs and notification preferences.",
          "Any employer-posted jobs and associated applicant data (for employer accounts)."
        ]
      },
      {
        kind: "list",
        title: "What we may retain",
        intro: "Certain data may be retained for legal and compliance purposes:",
        items: [
          "Transaction records as required by Indian tax and financial regulations.",
          "Abuse and safety reports to protect the community.",
          "Aggregated, anonymised analytics that cannot identify you."
        ],
        tone: "neutral"
      },
      {
        kind: "table",
        title: "Deletion timeline",
        columns: ["Step", "Timeframe"],
        rows: [
          ["Request received", "Confirmation email within 24 hours"],
          ["Account verification", "1 to 2 business days"],
          ["Data deletion", "3 to 5 business days after verification"],
          ["Final confirmation", "Email sent once deletion is complete"]
        ]
      },
      {
        kind: "copy",
        title: "Important notes",
        paragraphs: [
          "Account deletion is permanent and cannot be reversed. You will need to create a new account if you wish to use DutyPe again.",
          "If you have any pending payments or active disputes, those must be resolved before your account can be deleted.",
          "For employer accounts, all active job postings will be closed and applicants will be notified."
        ],
        tone: "warning"
      },
      {
        kind: "contact",
        title: "Send your deletion request",
        items: [
          {
            label: "Account deletion",
            value: SUPPORT_EMAIL,
            note: "Email with subject: Account Deletion Request. Include your registered phone number.",
            href: `mailto:${SUPPORT_EMAIL}?subject=Account%20Deletion%20Request`
          }
        ]
      }
    ],
    ctaTitle: "Ready to delete your account?",
    ctaCopy: `Send an email to ${SUPPORT_EMAIL} with the subject \"Account Deletion Request\" and your registered phone number.`,
    ctaHref: `mailto:${SUPPORT_EMAIL}?subject=Account%20Deletion%20Request`,
    ctaLabel: "Request deletion"
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

function getCityLocalAreas(citySlug: string) {
  return cityLocalAreas[citySlug] ?? [];
}

function buildCitySeoKeywords(city: string, areas: string[]) {
  return [
    `jobs in ${city}`,
    `${city} jobs near me`,
    `part time jobs in ${city}`,
    `local jobs in ${city}`,
    `job vacancy in ${city}`,
    `daily wage jobs in ${city}`,
    `delivery jobs in ${city}`,
    `driver jobs in ${city}`,
    `maid jobs in ${city}`,
    `cook jobs in ${city}`,
    `security guard jobs in ${city}`,
    `warehouse jobs in ${city}`,
    ...areas.flatMap((area) => [
      `jobs near ${area}`,
      `part time jobs in ${area}`,
      `delivery jobs in ${area}`,
      `driver jobs in ${area}`,
      `maid jobs in ${area}`,
      `cook jobs in ${area}`
    ])
  ];
}

function buildCategorySeoKeywords(details: CategoryDetails, cityLabel: string | null, areas: string[]) {
  const roleTerms = details.roles.flatMap((role) => [
    `${role.toLowerCase()} jobs`,
    `${role.toLowerCase()} vacancy`
  ]);

  if (!cityLabel) {
    return [
      `${details.shortLabel.toLowerCase()} jobs near me`,
      `${details.shortLabel.toLowerCase()} vacancy near me`,
      `part time ${details.shortLabel.toLowerCase()} jobs near me`,
      `${details.shortLabel.toLowerCase()} jobs for freshers`,
      ...roleTerms
    ];
  }

  return [
    `${details.shortLabel.toLowerCase()} jobs in ${cityLabel}`,
    `${details.shortLabel.toLowerCase()} vacancy in ${cityLabel}`,
    `${details.shortLabel.toLowerCase()} jobs near me ${cityLabel}`,
    `part time ${details.shortLabel.toLowerCase()} jobs in ${cityLabel}`,
    `${details.shortLabel.toLowerCase()} jobs for freshers in ${cityLabel}`,
    ...roleTerms.map((term) => `${term} in ${cityLabel}`),
    ...areas.flatMap((area) => [
      `${details.shortLabel.toLowerCase()} jobs in ${area}`,
      `${details.shortLabel.toLowerCase()} vacancy near ${area}`,
      `part time ${details.shortLabel.toLowerCase()} jobs in ${area}`
    ])
  ];
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
  const localAreas = getCityLocalAreas(citySlug);

  const categoryList = Object.entries(categoryDetails);
  const topCategories = categoryList.slice(0, 8);

  return {
    slug,
    title: `Jobs in ${city} — Local Hiring Near You`,
    description: `Find ${topCategories.map(([, d]) => d.shortLabel.toLowerCase()).join(", ")} and other local jobs in ${city}. Apply free on DutyPe — no middlemen, no fees, verified employers.`,
    eyebrow: `${city} Jobs`,
    intro:
      `Looking for work in ${city}? DutyPe lists delivery, driver, maid, cook, helper, security, warehouse, retail, part-time, and daily-wage jobs posted by verified local employers in ${city}. Apply directly from your phone — 100% free for workers.`,
    highlights: [
      `Jobs within 1 km, 5 km, and 10 km of your location in ${city}.`,
      "No registration fees. No middlemen. Direct employer contact.",
      "Get notified instantly when a new job is posted near you."
    ],
    seoKeywords: buildCitySeoKeywords(city, localAreas),
    blocks: [
      {
        kind: "table",
        title: `Jobs in ${city} by category`,
        intro: `Popular job categories and typical salary ranges in ${city}:`,
        columns: ["Category", "Typical salary & common roles"],
        rows: topCategories.map(([, d]) => [
          `${d.label} in ${city}`,
          `${d.salary} — ${d.roles.join(", ")}`
        ])
      },
      {
        kind: "list",
        title: `Why workers in ${city} use DutyPe`,
        items: [
          `Hyperlocal search shows only jobs near your area in ${city} — not generic nationwide listings.`,
          "One-tap apply sends your profile to the employer instantly — no forms, no calls, no agency fees.",
          "Verified employers with clear pay, shift timings, and location details on every listing.",
          `Many employers in ${city} respond within 24 hours and hire within 48 hours.`,
          "In-app chat lets you message employers directly before and after applying.",
          `Daily-wage and part-time options for workers in ${city} who need flexible schedules.`
        ]
      },
      {
        kind: "faq",
        title: `Jobs in ${city} — frequently asked questions`,
        items: [
          {
            question: `How do I find jobs near me in ${city}?`,
            answer: `Download DutyPe, enable location, and browse jobs within 1 km, 5 km, or 10 km of your area in ${city}. Apply with one tap.`
          },
          {
            question: `Is DutyPe free for workers in ${city}?`,
            answer: "Yes. DutyPe is 100% free for job seekers. You will never be asked to pay to apply or get hired."
          },
          {
            question: `What types of jobs are available in ${city}?`,
            answer: `DutyPe lists delivery, driver, maid, cook, helper, security, cleaner, warehouse, retail, part-time, daily-wage, and peon jobs in ${city}.`
          },
          {
            question: `Can freshers find jobs in ${city}?`,
            answer: "Yes. Many employers on DutyPe hire freshers and 10th/12th pass candidates for entry-level roles."
          },
          {
            question: `Are there night shift or weekend jobs in ${city}?`,
            answer: `Yes. Filter by shift type in the app to find night shift, weekend, evening, and morning jobs in ${city}.`
          }
        ]
      },
      {
        kind: "list",
        title: `Popular job searches in ${city}`,
        items: [
          `Part time jobs in ${city}`,
          `Delivery jobs in ${city}`,
          `Driver jobs in ${city}`,
          `Maid jobs in ${city}`,
          `Jobs in ${city} for freshers`,
          `Night shift jobs in ${city}`,
          `Jobs in ${city} no experience`,
          `Jobs in ${city} 10th pass`,
          `Daily wage jobs in ${city}`,
          `Jobs in ${city} for women`
        ]
      },
      ...(localAreas.length
        ? [
            {
              kind: "list" as const,
              title: `Jobs by local area in ${city}`,
              intro: `Use these area searches to find jobs closer to your commute in ${city}:`,
              items: localAreas.flatMap((area) => [
                `Jobs near ${area}`,
                `Part time jobs in ${area}`,
                `Delivery, driver, cook, maid, helper, retail, and security jobs in ${area}`
              ])
            },
            {
              kind: "faq" as const,
              title: `${city} local job search FAQ`,
              items: [
                {
                  question: `Can I search jobs by area in ${city}?`,
                  answer: `Yes. DutyPe is built for nearby hiring, so workers can search around local areas like ${localAreas.slice(0, 4).join(", ")} and other neighborhoods in ${city}.`
                },
                {
                  question: `Which ${city} areas are useful for local job search?`,
                  answer: `Popular search areas include ${localAreas.join(", ")}. Open the app and enable location to see the closest verified openings.`
                }
              ]
            }
          ]
        : [])
    ],
    ctaTitle: `Find jobs in ${city} now`,
    ctaCopy: `Download DutyPe and see live job openings near you in ${city}. Free for all workers.`,
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
      "Find local jobs across delivery, driving, housekeeping, cooking, security, warehouse, and part-time roles near your location. DutyPe connects you with verified employers instantly.",
    highlights: [
      "Nearby roles reduce commute friction.",
      "Workers need clear pay, location, and trust cues.",
      "100 percent free for job seekers."
    ],
    seoKeywords: [
      "jobs near me",
      "part time jobs near me",
      "delivery jobs near me",
      "driver jobs near me",
      "maid jobs near me",
      "cook jobs near me",
      "security jobs near me",
      "warehouse jobs near me",
      "daily wage jobs near me",
      "jobs hiring immediately near me",
      "10th pass jobs near me",
      "freshers jobs near me"
    ],
    blocks: [
      {
        kind: "table",
        title: "Jobs near me by category",
        intro: "Find the right job type near your location with salary ranges and common roles.",
        columns: ["Category", "Salary range and common roles"],
        rows: [
          ["Driver jobs", "Rs 15,000 to Rs 35,000 per month — Delivery driver, personal driver, cab driver, truck driver, auto driver, night driver"],
          ["Maid jobs", "Rs 8,000 to Rs 25,000 per month — House maid, part-time maid, full-time maid, live-in maid, babysitter, elderly care"],
          ["Delivery jobs", "Rs 12,000 to Rs 30,000 per month plus incentives — Food delivery, package delivery, courier, bike delivery, grocery delivery, medicine delivery"],
          ["Cook jobs", "Rs 10,000 to Rs 40,000 per month — Home cook, restaurant cook, part-time cook, tiffin service, chef, catering"],
          ["Helper jobs", "Rs 8,000 to Rs 20,000 per month — Office helper, shop helper, construction helper, general helper, kitchen helper, warehouse helper"],
          ["Security jobs", "Rs 12,000 to Rs 25,000 per month — Security guard, night security, watchman, building security, bouncer, CCTV operator"],
          ["Cleaner jobs", "Rs 8,000 to Rs 18,000 per month — Office cleaner, hospital cleaner, hotel housekeeping, mall cleaner, school cleaner, deep cleaning"],
          ["Peon jobs", "Rs 10,000 to Rs 18,000 per month — Office peon, school peon, hospital peon, government peon, bank peon, court peon"],
          ["Warehouse jobs", "Rs 12,000 to Rs 25,000 per month — Warehouse worker, forklift operator, packing, loading, inventory, logistics"],
          ["Part-time jobs", "Rs 5,000 to Rs 20,000 per month — Evening jobs, weekend jobs, morning jobs, student jobs, freelance jobs, home-based jobs"],
          ["Daily wage jobs", "Rs 500 to Rs 1,500 per day — Construction labor, loading and unloading, agricultural, event staffing, painting, plumbing helper"],
          ["Retail jobs", "Rs 10,000 to Rs 25,000 per month — Sales associate, cashier, store keeper, supermarket, showroom, mall jobs"]
        ]
      },
      {
        kind: "list",
        title: "Why choose DutyPe",
        items: [
          "Instant job alerts — get notified immediately when jobs are posted near your location.",
          "One-tap apply — apply to multiple jobs in seconds without lengthy forms.",
          "Verified employers — all employers are verified for your safety and security.",
          "Daily wage jobs — find jobs with same-day payment options.",
          "Part-time and full-time — flexible job options that fit your schedule.",
          "No hidden fees — 100 percent free for job seekers, always.",
          "Direct contact — chat directly with employers and get hired faster.",
          "Location-based search — find jobs within 1 km, 5 km, or 10 km radius."
        ]
      },
      {
        kind: "list",
        title: "How to find jobs on DutyPe",
        intro: "Follow these steps to start finding jobs near you:",
        items: [
          "Download the DutyPe app from the Google Play Store.",
          "Create your profile — add your skills, experience, and location in 2 minutes.",
          "Enable location — allow location access to see jobs near you automatically.",
          "Browse jobs — see listings within 1 km, 5 km, or 10 km from your location.",
          "Apply instantly — one-tap apply to multiple jobs without filling forms repeatedly.",
          "Chat with employers — direct messaging with employers for faster hiring.",
          "Get hired — receive job offers and start working within 24 to 48 hours."
        ]
      },
      {
        kind: "list",
        title: "Popular job searches",
        items: [
          "Part time jobs near me for students",
          "Night shift jobs near me",
          "Work from home jobs near me",
          "Weekend jobs near me",
          "Jobs near me no experience required",
          "High paying jobs near me",
          "Immediate joining jobs near me",
          "Jobs near me for 12th pass",
          "Jobs near me for 10th pass",
          "Jobs near me for freshers",
          "Jobs near me with accommodation",
          "Jobs near me with food provided",
          "Jobs near me hiring immediately",
          "Jobs near me same day payment",
          "Jobs near me for women"
        ]
      },
      {
        kind: "list",
        title: "Browse jobs by city",
        items: [
          "Jobs in Hyderabad",
          "Jobs in Bangalore",
          "Jobs in Mumbai",
          "Jobs in Delhi",
          "Jobs in Vijayawada",
          "Jobs in Tirupati",
          "Jobs in Warangal",
          "Jobs in Guntur"
        ]
      },
      {
        kind: "faq",
        title: "Jobs near me FAQ",
        items: [
          {
            question: "How do I find jobs near me?",
            answer: "Download the DutyPe app, enable location, and browse jobs within your preferred radius."
          },
          {
            question: "Are there jobs near me for freshers?",
            answer: "Yes. Many employers on DutyPe hire freshers for delivery, helper, cleaner, and daily wage roles."
          },
          {
            question: "Can I find part-time jobs near me?",
            answer: "Yes. DutyPe lists many part-time, evening, weekend, and flexible-shift jobs across cities."
          },
          {
            question: "Do I need experience to apply?",
            answer: "Many openings do not require prior experience. Look for roles tagged entry-level or no-experience."
          },
          {
            question: "Is DutyPe available in my city?",
            answer: "DutyPe is growing across Indian cities. Enable your location to see available jobs nearby."
          },
          {
            question: "How fast can I get hired?",
            answer: "Many employers review applications within 24 to 48 hours. Some daily-wage jobs hire on the same day."
          }
        ]
      }
    ],
    ctaTitle: "See live nearby jobs",
    ctaCopy: "Download DutyPe and find verified jobs near your location today.",
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
  const cityAreas = city ? getCityLocalAreas(city) : [];
  const title = cityLabel
    ? `${details.label} in ${cityLabel} — Apply Free`
    : `${details.label} — Find ${details.shortLabel} Work Near You`;
  const locationLabel = cityLabel ?? "your area";

  return {
    slug,
    title,
    description: cityLabel
      ? `Find ${details.shortLabel.toLowerCase()} jobs in ${cityLabel}. ${details.salary} Apply free on DutyPe — verified employers, no middlemen, instant apply.`
      : `Find ${details.shortLabel.toLowerCase()} jobs near you. ${details.salary} Apply free on DutyPe — verified employers, no fees for workers.`,
    eyebrow: cityLabel ? `${details.shortLabel} Jobs · ${cityLabel}` : `${details.shortLabel} Jobs`,
    intro: cityLabel
      ? `${details.intro} Browse the latest ${details.shortLabel.toLowerCase()} openings in ${cityLabel} and apply directly through DutyPe — free for all workers, no agencies, no fees.`
      : `${details.intro} Browse verified ${details.shortLabel.toLowerCase()} openings near your location and apply with one tap through DutyPe.`,
    highlights: [
      details.salary,
      `Apply to ${details.shortLabel.toLowerCase()} jobs in ${locationLabel} with one tap — no forms, no calls.`,
      "100% free for workers. Verified employers only."
    ],
    seoKeywords: buildCategorySeoKeywords(details, cityLabel, cityAreas),
    blocks: [
      {
        kind: "table",
        title: cityLabel
          ? `${details.shortLabel} roles and pay in ${cityLabel}`
          : `${details.shortLabel} roles and typical pay`,
        intro: `Common ${details.shortLabel.toLowerCase()} positions available on DutyPe:`,
        columns: ["Role", "Details"],
        rows: details.roles.map((role) => [
          cityLabel ? `${role} in ${cityLabel}` : role,
          `${details.salary} Direct employer contact, verified listings.`
        ])
      },
      {
        kind: "list",
        title: `Why choose DutyPe for ${details.shortLabel.toLowerCase()} jobs`,
        items: [
          `Hyperlocal search — see only ${details.shortLabel.toLowerCase()} jobs near your exact location, not city-wide spam.`,
          "One-tap apply — your profile goes directly to the employer. No paperwork, no agency calls.",
          "Verified employers — every job listing shows clear pay, shift, and location details.",
          `Daily, weekly, and monthly pay options for ${details.shortLabel.toLowerCase()} roles.`,
          "In-app chat — message employers directly before and after applying.",
          "Safety first — report suspicious listings and DutyPe reviews within 24 hours."
        ]
      },
      ...(cityLabel && cityAreas.length
        ? [
            {
              kind: "list" as const,
              title: `${details.shortLabel} jobs by area in ${cityLabel}`,
              intro: `Search by neighborhood to find ${details.shortLabel.toLowerCase()} work closer to your commute in ${cityLabel}:`,
              items: cityAreas.flatMap((area) => [
                `${details.shortLabel} jobs in ${area}`,
                `Part time ${details.shortLabel.toLowerCase()} jobs in ${area}`,
                `${details.shortLabel} vacancy near ${area}`
              ])
            },
            {
              kind: "copy" as const,
              title: `Common ${cityLabel} ${details.shortLabel.toLowerCase()} searches`,
              paragraphs: [
                `Common searches include ${details.shortLabel.toLowerCase()} jobs near me in ${cityLabel}, ${details.shortLabel.toLowerCase()} vacancy in ${cityLabel}, and part time ${details.shortLabel.toLowerCase()} jobs near ${cityAreas.slice(0, 3).join(", ")}.`,
                `Workers can compare nearby openings by pay, shift, and area before applying. Employers can reach candidates who already want this role in the same city.`
              ],
              tone: "highlight" as const
            }
          ]
        : []),
      {
        kind: "faq",
        title: `${details.shortLabel} jobs — common questions`,
        items: [
          {
            question: `How much do ${details.shortLabel.toLowerCase()} jobs pay?`,
            answer: details.salary
          },
          {
            question: `Do I need experience for ${details.shortLabel.toLowerCase()} jobs?`,
            answer: `Many ${details.shortLabel.toLowerCase()} jobs on DutyPe do not require prior experience. Employers often hire freshers and train on the job.`
          },
          {
            question: `How do I apply for ${details.shortLabel.toLowerCase()} jobs${cityLabel ? ` in ${cityLabel}` : ""}?`,
            answer: `Download DutyPe, create your profile, enable location, and apply to ${details.shortLabel.toLowerCase()} jobs with one tap. It's 100% free.`
          },
          {
            question: `Are there part-time ${details.shortLabel.toLowerCase()} jobs?`,
            answer: `Yes. DutyPe lists full-time, part-time, daily-wage, and flexible ${details.shortLabel.toLowerCase()} openings. Filter by shift type in the app.`
          },
          {
            question: `Is DutyPe free for ${details.shortLabel.toLowerCase()} job seekers?`,
            answer: "Yes. DutyPe is completely free for all workers. You will never be charged to browse, apply, or get hired."
          }
        ]
      },
      {
        kind: "list",
        title: `Popular ${details.shortLabel.toLowerCase()} job searches`,
        items: [
          `${details.shortLabel} jobs near me`,
          `${details.shortLabel} jobs ${cityLabel ?? "in my city"}`,
          `Part time ${details.shortLabel.toLowerCase()} jobs`,
          `${details.shortLabel} jobs for freshers`,
          `${details.shortLabel} jobs no experience`,
          `Night shift ${details.shortLabel.toLowerCase()} jobs`,
          `${details.shortLabel} jobs for women`,
          `${details.shortLabel} jobs 10th pass`
        ]
      }
    ],
    ctaTitle: `Find ${details.shortLabel.toLowerCase()} jobs${cityLabel ? ` in ${cityLabel}` : ""} now`,
    ctaCopy: `Download DutyPe and see live ${details.shortLabel.toLowerCase()} job openings near you. Free for all workers.`,
    ctaHref: "/app/worker/jobs",
    ctaLabel: "Browse live jobs"
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
