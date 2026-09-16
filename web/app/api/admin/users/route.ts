import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { normalizeUserRecord } from "@/lib/firebase/admin-normalizers";
import {
  getFirebaseAdminAuth,
  getFirebaseAdminDb
} from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

type AuthUserSummary = {
  uid: string;
  email: string;
  phone: string;
  displayName: string;
  disabled: boolean;
  createdAt: string;
  lastSignInAt: string;
};

function asRecord(value: unknown) {
  return (value ?? {}) as Record<string, unknown>;
}

function firstNonEmptyString(...values: unknown[]) {
  for (const value of values) {
    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }
  }

  return "";
}

function normalizeRole(value: unknown) {
  const role = firstNonEmptyString(value).toUpperCase();

  if (role === "WORKER" || role === "EMPLOYER" || role === "ADMIN") {
    return role;
  }

  return role ? "OTHER" : "";
}

function normalizePhoneForDoc(value: unknown) {
  const raw = firstNonEmptyString(value);
  if (!raw) return "";
  if (/^\+[1-9]\d{6,14}$/.test(raw)) return raw;

  const digits = raw.replace(/\D/g, "");
  if (digits.length === 10) return `+91${digits}`;
  if (digits.length >= 7 && digits.length <= 15) return `+${digits}`;
  return raw;
}

function getStateByPincode(pin: number): string | null {
  const p2 = Math.floor(pin / 10000);
  const p3 = Math.floor(pin / 1000);

  if (p3 === 403) return "Goa";
  if (p2 === 11) return "Delhi NCR";
  if (p2 === 12 || p2 === 13) return "Haryana";
  if (p2 === 14 || p2 === 15) return "Punjab";
  if (p2 === 16) return "Punjab";
  if (p2 === 17) return "Himachal Pradesh";
  if (p2 === 18 || p2 === 19) return "Jammu & Kashmir";
  if (p2 === 24 || p2 === 26) return "Uttarakhand";
  if (p2 >= 20 && p2 <= 28) return "Uttar Pradesh";
  if (p2 >= 30 && p2 <= 34) return "Rajasthan";
  if (p2 >= 36 && p2 <= 39) return "Gujarat";
  if (p2 >= 40 && p2 <= 44) return "Maharashtra";
  if (p2 >= 45 && p2 <= 48) return "Madhya Pradesh";
  if (p2 === 49) return "Chhattisgarh";
  if (p2 === 50) return "Telangana";
  if (p2 >= 51 && p2 <= 53) return "Andhra Pradesh";
  if (p2 >= 56 && p2 <= 59) return "Karnataka";
  if (p2 >= 60 && p2 <= 64) return "Tamil Nadu";
  if (p2 >= 67 && p2 <= 69) return "Kerala";
  if (p2 >= 70 && p2 <= 74) return "West Bengal";
  if (p2 >= 75 && p2 <= 77) return "Odisha";
  if (p2 === 78) return "Assam";
  if (p2 === 79) return "North East";
  if (p2 >= 81 && p2 <= 83) return "Jharkhand";
  if (p2 === 80 || p2 === 84 || p2 === 85) return "Bihar";
  return null;
}

function getStateByCoordinates(lat: number, lng: number): string | null {
  if (lat >= 28.35 && lat <= 28.92 && lng >= 76.84 && lng <= 77.45) return "Delhi NCR";
  if (lat >= 15.8 && lat <= 19.9 && lng >= 77.2 && lng <= 81.8) {
    if (lng > 80.6 && lat < 18.2) return "Andhra Pradesh";
    return "Telangana";
  }
  if (lat >= 13.4 && lat <= 19.2 && lng >= 76.8 && lng <= 84.8) {
    if (lat >= 15.8 && lat <= 19.9 && lng >= 77.2 && lng <= 81.0) return "Telangana";
    return "Andhra Pradesh";
  }
  if (lat >= 14.9 && lat <= 15.8 && lng >= 73.7 && lng <= 74.4) return "Goa";
  if (lat >= 11.5 && lat <= 18.5 && lng >= 74.0 && lng <= 78.6) return "Karnataka";
  if (lat >= 8.0 && lat <= 13.5 && lng >= 76.2 && lng <= 80.4) return "Tamil Nadu";
  if (lat >= 8.2 && lat <= 12.8 && lng >= 74.8 && lng <= 77.4) return "Kerala";
  if (lat >= 15.6 && lat <= 22.0 && lng >= 72.6 && lng <= 80.9) return "Maharashtra";
  if (lat >= 20.1 && lat <= 24.7 && lng >= 68.1 && lng <= 74.5) return "Gujarat";
  if (lat >= 23.0 && lat <= 30.2 && lng >= 69.5 && lng <= 78.3) return "Rajasthan";
  if (lat >= 23.8 && lat <= 30.4 && lng >= 77.0 && lng <= 84.6) return "Uttar Pradesh";
  if (lat >= 24.3 && lat <= 27.5 && lng >= 83.3 && lng <= 88.3) return "Bihar";
  if (lat >= 21.5 && lat <= 27.2 && lng >= 85.8 && lng <= 89.9) return "West Bengal";
  if (lat >= 21.3 && lat <= 26.9 && lng >= 74.0 && lng <= 82.8) return "Madhya Pradesh";
  if (lat >= 17.8 && lat <= 22.6 && lng >= 81.4 && lng <= 87.5) return "Odisha";
  if (lat >= 29.5 && lat <= 32.5 && lng >= 73.8 && lng <= 76.9) return "Punjab";
  if (lat >= 27.6 && lat <= 30.9 && lng >= 74.5 && lng <= 77.6) return "Haryana";
  if (lat >= 22.0 && lat <= 25.3 && lng >= 83.3 && lng <= 87.9) return "Jharkhand";
  if (lat >= 17.8 && lat <= 24.1 && lng >= 80.2 && lng <= 84.4) return "Chhattisgarh";
  if (lat >= 28.7 && lat <= 31.5 && lng >= 77.6 && lng <= 81.1) return "Uttarakhand";
  if (lat >= 30.4 && lat <= 33.2 && lng >= 75.6 && lng <= 79.0) return "Himachal Pradesh";
  if (lat >= 24.1 && lat <= 28.0 && lng >= 89.7 && lng <= 96.0) return "Assam";
  if (lat >= 32.3 && lat <= 35.5 && lng >= 73.4 && lng <= 77.0) return "Jammu & Kashmir";
  return null;
}

const STATE_KEYWORDS_LIST: Array<{ state: string; keywords: string[] }> = [
  {
    state: "Telangana",
    keywords: [
      "telangana", "hyderabad", "secunderabad", "cyberabad", "warangal", "nizamabad",
      "karimnagar", "khammam", "mahbubnagar", "nalgonda", "adilabad", "suryapet",
      "siddipet", "miryalaguda", "jagtial", "mancherial", "ramagundam", "kothagudem",
      "kamareddy", "medak", "sangareddy", "rangareddy", "hitec city", "madhapur",
      "gachibowli", "kukatpally", "dilsukhnagar", "ameerpet", "kondapur", "miyapur",
      "nallagandla", "lingampally", "serilingampalle", "patancheru", "shamshabad",
      "uppal", "lb nagar", "begumpet", "jubilee hills", "banjara hills", "manikonda",
      "bowenpally", "alwal", "tarnaka", "malkajgiri", "boduppal", "cherlapally",
      "jeedimetla", "balanagar", "kompally", "medchal", "ghatkesar", "tellapur",
      "kokapet", "chandanagar", "hafeezpet", "bachupally", "attapur"
    ]
  },
  {
    state: "Andhra Pradesh",
    keywords: [
      "andhra pradesh", "andhra", "visakhapatnam", "vizag", "vijayawada", "guntur",
      "nellore", "kurnool", "rajahmundry", "kakinada", "tirupati", "anantapur",
      "kadapa", "vizianagaram", "eluru", "ongole", "nandyal", "machilipatnam",
      "adoni", "tenali", "proddatur", "chittoor", "hindupur", "bhimavaram",
      "amaravati", "srikakulam", "anakapalle", "tadipatri", "palasa", "chirala",
      "dharmavaram", "gudivada", "narasaraopet", "mangalagiri", "madanapalle"
    ]
  },
  {
    state: "Karnataka",
    keywords: [
      "karnataka", "bengaluru", "bangalore", "mysore", "mysuru", "hubli", "dharwad",
      "mangalore", "mangaluru", "belgaum", "belagavi", "gulbarga", "kalaburagi",
      "davanagere", "bellary", "ballari", "shimoga", "shivamogga", "tumkur",
      "tumakuru", "raichur", "bidar", "whitefield", "electronic city", "koramangala",
      "indiranagar", "hsr layout", "marathahalli", "btm layout", "jayanagar", "yelahanka"
    ]
  },
  {
    state: "Tamil Nadu",
    keywords: [
      "tamil nadu", "tamilnadu", "tamil", "chennai", "madras", "coimbatore",
      "madurai", "tiruchirappalli", "trichy", "salem", "tirunelveli", "tiruppur",
      "erode", "vellore", "thoothukudi", "dindigul", "thanjavur", "ranipet",
      "sivakasi", "karur", "ooty", "hosur", "kanchipuram", "nagercoil", "cuddalore",
      "tambaram", "avadi", "velachery", "anna nagar", "t nagar"
    ]
  },
  {
    state: "Maharashtra",
    keywords: [
      "maharashtra", "mumbai", "bombay", "pune", "nagpur", "thane", "nashik",
      "kalyan", "dombivli", "vasai", "virar", "aurangabad", "chhatrapati sambhajinagar",
      "navi mumbai", "solapur", "mira-bhayandar", "bhiwandi", "amravati", "nanded",
      "kolhapur", "ulhasnagar", "sangli", "malegaon", "akola", "latur", "dhule",
      "ahmednagar", "chandrapur", "parbhani", "jalna", "panvel", "pimpri", "chinchwad",
      "andheri", "borivali", "dadar", "bandra", "powai", "hinjewadi", "wakad"
    ]
  },
  {
    state: "Delhi NCR",
    keywords: [
      "delhi", "new delhi", "ncr", "noida", "greater noida", "ghaziabad", "gurugram",
      "gurgaon", "faridabad", "dwarka", "rohini", "saket", "connaught place",
      "janakpuri", "laxmi nagar", "karol bagh", "vasant kunj"
    ]
  },
  {
    state: "Kerala",
    keywords: [
      "kerala", "kochi", "cochin", "thiruvananthapuram", "trivandrum", "kozhikode",
      "calicut", "thrissur", "kollam", "palakkad", "alappuzha", "kannur",
      "kottayam", "malappuram", "kasaragod", "ernakulam", "wayanad", "idukki"
    ]
  },
  {
    state: "Gujarat",
    keywords: [
      "gujarat", "ahmedabad", "surat", "vadodara", "baroda", "rajkot", "bhavnagar",
      "jamnagar", "junagadh", "gandhinagar", "anand", "navsari", "morbi",
      "nadiad", "surendranagar", "bharuch", "mehsana", "bhuj", "vapi", "valsad"
    ]
  },
  {
    state: "West Bengal",
    keywords: [
      "west bengal", "bengal", "kolkata", "calcutta", "howrah", "durgapur",
      "asansol", "siliguri", "bardhaman", "malda", "baharampur", "habra",
      "kharagpur", "shantipur", "dankuni", "haldia", "darjeeling", "jalpaiguri"
    ]
  },
  {
    state: "Uttar Pradesh",
    keywords: [
      "uttar pradesh", "lucknow", "kanpur", "varanasi", "banaras", "kashi",
      "agra", "prayagraj", "allahabad", "meerut", "bareilly", "aligarh",
      "moradabad", "saharanpur", "gorakhpur", "jhansi", "muzaffarnagar", "mathura",
      "ayodhya", "firozabad", "budaun", "rampur", "shahjahanpur", "farrukhabad"
    ]
  },
  {
    state: "Rajasthan",
    keywords: [
      "rajasthan", "jaipur", "jodhpur", "kota", "bikaner", "ajmer", "udaipur",
      "bhilwara", "alwar", "bharatpur", "sikar", "pali", "sri ganganagar", "kishangarh"
    ]
  },
  {
    state: "Bihar",
    keywords: [
      "bihar", "patna", "gaya", "bhagalpur", "muzaffarpur", "purnia", "darbhanga",
      "bihar sharif", "arrah", "begusarai", "katihar", "munger", "chhapra",
      "danapur", "saharsa", "sasaram", "hajipur", "siwan", "motihari"
    ]
  },
  {
    state: "Madhya Pradesh",
    keywords: [
      "madhya pradesh", "indore", "bhopal", "jabalpur", "gwalior", "ujjain",
      "sagar", "dewas", "satna", "ratlam", "rewa", "katni", "singrauli", "burhanpur", "khandwa"
    ]
  },
  {
    state: "Punjab",
    keywords: [
      "punjab", "ludhiana", "amritsar", "jalandhar", "patiala", "bathinda",
      "mohali", "sas nagar", "hoshiarpur", "batala", "pathankot", "moga", "abohar", "chandigarh"
    ]
  },
  {
    state: "Haryana",
    keywords: [
      "haryana", "panipat", "ambala", "yamunanagar", "rohtak", "hisar", "karnal",
      "sonipat", "panchkula", "sirsa", "bhiwani", "bahadurgarh", "jind"
    ]
  },
  {
    state: "Odisha",
    keywords: [
      "odisha", "orissa", "bhubaneswar", "cuttack", "rourkela", "berhampur",
      "sambalpur", "puri", "balasore", "bhadrak", "baripada", "jharsuguda"
    ]
  },
  {
    state: "Jharkhand",
    keywords: [
      "jharkhand", "ranchi", "jamshedpur", "dhanbad", "bokaro", "deoghar",
      "hazaribagh", "giridih", "ramgarh", "medininagar"
    ]
  },
  {
    state: "Assam",
    keywords: [
      "assam", "guwahati", "silchar", "dibrugarh", "jorhat", "nagaon", "tinsukia", "tezpur"
    ]
  },
  {
    state: "Chhattisgarh",
    keywords: [
      "chhattisgarh", "raipur", "bhilai", "bilaspur", "korba", "rajnandgaon", "durg"
    ]
  },
  {
    state: "Uttarakhand",
    keywords: [
      "uttarakhand", "uttaranchal", "dehradun", "haridwar", "roorkee", "haldwani", "rudrapur", "rishikesh"
    ]
  },
  {
    state: "Goa",
    keywords: ["goa", "panaji", "margao", "vasco", "mapusa", "ponda"]
  },
  {
    state: "Himachal Pradesh",
    keywords: ["himachal pradesh", "himachal", "shimla", "dharamshala", "solan", "mandi", "kullu", "manali"]
  },
  {
    state: "Jammu & Kashmir",
    keywords: ["jammu and kashmir", "jammu & kashmir", "kashmir", "jammu", "srinagar", "anantnag", "baramulla"]
  },
  {
    state: "Puducherry",
    keywords: ["puducherry", "pondicherry"]
  },
  {
    state: "Tripura",
    keywords: ["tripura", "agartala"]
  },
  {
    state: "Meghalaya",
    keywords: ["meghalaya", "shillong"]
  },
  {
    state: "Manipur",
    keywords: ["manipur", "imphal"]
  },
  {
    state: "Nagaland",
    keywords: ["nagaland", "kohima", "dimapur"]
  },
  {
    state: "Mizoram",
    keywords: ["mizoram", "aizawl"]
  },
  {
    state: "Sikkim",
    keywords: ["sikkim", "gangtok"]
  },
  {
    state: "Arunachal Pradesh",
    keywords: ["arunachal pradesh", "arunachal", "itanagar"]
  }
];

function getTelecomCircleByPhone(phone: string): string | null {
  const digits = phone.replace(/\D/g, "");
  if (digits.length < 10) return null;
  const num = digits.slice(-10);
  const p4 = parseInt(num.slice(0, 4), 10);

  if (
    (p4 >= 9848 && p4 <= 9849) || p4 === 9866 || p4 === 9948 || p4 === 9949 || p4 === 9989 ||
    p4 === 9959 || p4 === 9701 || p4 === 9705 || p4 === 9618 || p4 === 9603 || p4 === 9676 ||
    p4 === 9505 || p4 === 9550 || p4 === 9581 || p4 === 9000 || p4 === 9010 || p4 === 9030 ||
    p4 === 9052 || p4 === 9177 || p4 === 9160 || p4 === 8978 || p4 === 8985 || (p4 >= 8331 && p4 <= 8333) ||
    p4 === 8341 || p4 === 8374 || p4 === 8790 || p4 === 8712 || p4 === 8500 || (p4 >= 8184 && p4 <= 8187) ||
    p4 === 8179 || p4 === 8142 || p4 === 8143 || p4 === 8125 || p4 === 8121 || p4 === 8106 ||
    p4 === 8096 || p4 === 8099 || p4 === 8019 || p4 === 8008 || p4 === 7032 || p4 === 7036 ||
    p4 === 7093 || p4 === 7095 || p4 === 7207 || p4 === 7382 || p4 === 7386 || p4 === 7396 ||
    p4 === 7337 || (p4 >= 7659 && p4 <= 7661) || (p4 >= 7671 && p4 <= 7675) || p4 === 7702 ||
    (p4 >= 7729 && p4 <= 7732) || p4 === 7780 || p4 === 7794 || p4 === 7799 || p4 === 7815 ||
    p4 === 7842 || p4 === 7893 || p4 === 7981 || p4 === 7989 || p4 === 7993 || p4 === 7995 || p4 === 7997 ||
    (p4 >= 9440 && p4 <= 9441) || (p4 >= 9490 && p4 <= 9494)
  ) {
    return "Telangana";
  }

  if (
    (p4 >= 9844 && p4 <= 9845) || p4 === 9880 || p4 === 9886 || (p4 >= 9900 && p4 <= 9902) ||
    p4 === 9945 || p4 === 9972 || p4 === 9980 || (p4 >= 9731 && p4 <= 9743) || p4 === 9611 ||
    p4 === 9620 || p4 === 9632 || p4 === 9663 || p4 === 9686 || p4 === 9008 || p4 === 9035 ||
    p4 === 9036 || p4 === 9060 || p4 === 9066 || p4 === 9141 || p4 === 9148 || p4 === 9164 ||
    p4 === 8970 || p4 === 8971 || p4 === 8951 || p4 === 8884 || p4 === 8880 || p4 === 8861 ||
    p4 === 8867 || p4 === 8762 || p4 === 8746 || p4 === 8722 || p4 === 8710 || (p4 >= 8494 && p4 <= 8497) ||
    p4 === 8453 || p4 === 8431 || p4 === 8277 || p4 === 8197 || (p4 >= 8150 && p4 <= 8152) ||
    p4 === 8147 || p4 === 8123 || p4 === 8105 || p4 === 8095 || p4 === 8088 || p4 === 8050 ||
    p4 === 7022 || p4 === 7026 || p4 === 7204 || p4 === 7259 || p4 === 7338 || p4 === 7349 ||
    p4 === 7353 || p4 === 7406 || p4 === 7411 || p4 === 7618 || p4 === 7619 || p4 === 7624 ||
    p4 === 7676 || p4 === 7760 || p4 === 7795 || p4 === 7829 || p4 === 7892 || p4 === 7899 ||
    p4 === 7975 || p4 === 7996
  ) {
    return "Karnataka";
  }

  if (
    (p4 >= 9840 && p4 <= 9841) || p4 === 9884 || (p4 >= 9940 && p4 <= 9941) || (p4 >= 9789 && p4 <= 9791) ||
    p4 === 9710 || p4 === 9600 || p4 === 9003 || p4 === 9042 || p4 === 9043 || p4 === 9176 ||
    p4 === 8939 || p4 === 8870 || p4 === 8667 || p4 === 8668 || p4 === 8610 || p4 === 8428 ||
    p4 === 8248 || p4 === 8148 || p4 === 8144 || p4 === 8124 || p4 === 8122 || p4 === 8110 ||
    p4 === 8056 || p4 === 8072 || p4 === 7010 || p4 === 7200 || p4 === 7299 || p4 === 7339 ||
    p4 === 7358 || p4 === 7373 || p4 === 7397 || p4 === 7418 || p4 === 7448 || p4 === 7449 ||
    p4 === 7550 || p4 === 7548 || p4 === 7667 || p4 === 7708 || (p4 >= 7810 && p4 <= 7812) ||
    (p4 >= 7824 && p4 <= 7826) || p4 === 7845 || p4 === 7871 || p4 === 7904
  ) {
    return "Tamil Nadu";
  }

  if (
    (p4 >= 9820 && p4 <= 9823) || p4 === 9819 || p4 === 9833 || p4 === 9850 || p4 === 9860 ||
    p4 === 9890 || p4 === 9892 || (p4 >= 9920 && p4 <= 9923) || p4 === 9930 || p4 === 9960 ||
    p4 === 9969 || p4 === 9970 || p4 === 9975 || p4 === 9987 || (p4 >= 9762 && p4 <= 9767) ||
    p4 === 9769 || p4 === 9773 || p4 === 9702 || p4 === 9619 || p4 === 9604 || p4 === 9607 ||
    p4 === 9623 || p4 === 9637 || p4 === 9657 || p4 === 9665 || p4 === 9673 || p4 === 9689 ||
    p4 === 9167 || p4 === 9152 || p4 === 9011 || (p4 >= 9021 && p4 <= 9022) || p4 === 9028 ||
    p4 === 9049 || p4 === 9075 || p4 === 9096 || p4 === 9112 || p4 === 9130 || p4 === 9145 ||
    p4 === 9146 || p4 === 9158 || p4 === 9172 || p4 === 9175 || p4 === 8879 || p4 === 8805 ||
    p4 === 8806 || p4 === 8830 || (p4 >= 8855 && p4 <= 8857) || p4 === 8888 || p4 === 8655 ||
    p4 === 8652 || (p4 >= 8451 && p4 <= 8454) || (p4 >= 8422 && p4 <= 8425) || p4 === 8291 ||
    p4 === 8108 || p4 === 8104 || p4 === 8080 || p4 === 8082 || p4 === 7045 || p4 === 7208 ||
    p4 === 7506 || p4 === 7710 || p4 === 7715 || p4 === 7718 || p4 === 7738 || p4 === 7777 || p4 === 7977
  ) {
    return "Maharashtra";
  }

  if (
    p4 === 9810 || p4 === 9811 || p4 === 9818 || p4 === 9868 || p4 === 9871 || p4 === 9873 ||
    p4 === 9891 || p4 === 9899 || p4 === 9910 || p4 === 9911 || p4 === 9953 || p4 === 9958 ||
    p4 === 9971 || p4 === 9990 || p4 === 9999 || p4 === 9711 || (p4 >= 9716 && p4 <= 9718) ||
    p4 === 9650 || p4 === 9654 || p4 === 9540 || p4 === 9555 || p4 === 9560 || p4 === 9582 ||
    p4 === 9599 || p4 === 9013 || p4 === 9015 || p4 === 9136 || p4 === 8800 || p4 === 8826 ||
    p4 === 8860 || (p4 >= 8743 && p4 <= 8745) || p4 === 8750 || p4 === 8527 || (p4 >= 8586 && p4 <= 8588) ||
    p4 === 8447 || p4 === 8448 || (p4 >= 8373 && p4 <= 8377) || p4 === 8368 || p4 === 8130 ||
    p4 === 8178 || p4 === 7011 || p4 === 7042 || (p4 >= 7290 && p4 <= 7292) || p4 === 7303 ||
    p4 === 7428 || p4 === 7503 || p4 === 7531 || p4 === 7532 || p4 === 7827 || p4 === 7838 || p4 === 7982
  ) {
    return "Delhi NCR";
  }

  return null;
}

function inferState(data: {
  state?: unknown;
  city?: unknown;
  address?: unknown;
  locationText?: unknown;
  location?: unknown;
  businessAddress?: unknown;
  businessLocation?: unknown;
  lat?: unknown;
  lng?: unknown;
  phone?: unknown;
}): string {
  // 1. Explicit state field matching
  const rawState = String(data.state ?? "").trim().toLowerCase();
  if (rawState) {
    for (const item of STATE_KEYWORDS_LIST) {
      if (item.keywords.some(kw => rawState === kw || rawState.includes(kw))) {
        return item.state;
      }
    }
    if (rawState === "ts" || rawState === "tg") return "Telangana";
    if (rawState === "ap") return "Andhra Pradesh";
    if (rawState === "ka") return "Karnataka";
    if (rawState === "tn") return "Tamil Nadu";
    if (rawState === "mh") return "Maharashtra";
    if (rawState === "dl") return "Delhi NCR";
    if (rawState === "kl") return "Kerala";
    if (rawState === "gj") return "Gujarat";
    if (rawState === "wb") return "West Bengal";
    if (rawState === "up") return "Uttar Pradesh";
    if (rawState === "rj") return "Rajasthan";
    if (rawState === "br") return "Bihar";
    if (rawState === "mp") return "Madhya Pradesh";
    if (rawState === "pb") return "Punjab";
    if (rawState === "hr") return "Haryana";
    if (rawState === "od" || rawState === "or") return "Odisha";
    if (rawState === "jh") return "Jharkhand";
    if (rawState === "as") return "Assam";
    if (rawState === "cg" || rawState === "ct") return "Chhattisgarh";
    if (rawState === "uk") return "Uttarakhand";
    if (rawState === "ga") return "Goa";
    if (rawState === "hp") return "Himachal Pradesh";
    if (rawState === "jk") return "Jammu & Kashmir";
  }

  // 2. Combined text from address, businessAddress, city, location
  const candidateTexts: string[] = [];
  if (data.city) candidateTexts.push(String(data.city));
  if (data.address) candidateTexts.push(String(data.address));
  if (data.businessAddress) candidateTexts.push(String(data.businessAddress));
  if (data.locationText) candidateTexts.push(String(data.locationText));
  if (typeof data.location === "string") candidateTexts.push(data.location);
  if (typeof data.location === "object" && data.location !== null) {
    const locObj = data.location as Record<string, unknown>;
    if (locObj.address) candidateTexts.push(String(locObj.address));
    if (locObj.city) candidateTexts.push(String(locObj.city));
    if (locObj.state) candidateTexts.push(String(locObj.state));
    if (locObj.area) candidateTexts.push(String(locObj.area));
  }
  if (typeof data.businessLocation === "object" && data.businessLocation !== null) {
    const bLocObj = data.businessLocation as Record<string, unknown>;
    if (bLocObj.address) candidateTexts.push(String(bLocObj.address));
    if (bLocObj.city) candidateTexts.push(String(bLocObj.city));
    if (bLocObj.state) candidateTexts.push(String(bLocObj.state));
  }

  const combinedText = candidateTexts.map(v => v.toLowerCase()).join(" ");

  // 3. Match by Indian 6-digit PIN Code
  const pinMatch = combinedText.match(/\b([1-9][0-9]{5})\b/);
  if (pinMatch) {
    const pin = parseInt(pinMatch[1], 10);
    const pinState = getStateByPincode(pin);
    if (pinState) return pinState;
  }

  // 4. Match by city / district / area keywords across all states
  for (const item of STATE_KEYWORDS_LIST) {
    if (item.keywords.some(kw => combinedText.includes(kw))) {
      return item.state;
    }
  }

  // 5. Match by GPS Coordinates (latitude, longitude)
  let lat = typeof data.lat === "number" ? data.lat : parseFloat(String(data.lat || ""));
  let lng = typeof data.lng === "number" ? data.lng : parseFloat(String(data.lng || ""));

  if ((isNaN(lat) || isNaN(lng) || lat === 0 || lng === 0) && typeof data.location === "object" && data.location !== null) {
    const loc = data.location as Record<string, unknown>;
    lat = Number(loc.lat ?? loc.latitude);
    lng = Number(loc.lng ?? loc.longitude);
  }

  if ((isNaN(lat) || isNaN(lng) || lat === 0 || lng === 0) && typeof data.businessLocation === "object" && data.businessLocation !== null) {
    const bLoc = data.businessLocation as Record<string, unknown>;
    lat = Number(bLoc.lat ?? bLoc.latitude);
    lng = Number(bLoc.lng ?? bLoc.longitude);
  }

  if (!isNaN(lat) && !isNaN(lng) && lat > 6.0 && lat < 38.0 && lng > 68.0 && lng < 98.0) {
    const geoState = getStateByCoordinates(lat, lng);
    if (geoState) return geoState;
  }

  // 6. Telecom circle fallback for phone-only users who haven't completed profile
  if (data.phone) {
    const circle = getTelecomCircleByPhone(String(data.phone));
    if (circle) return circle;
  }

  return "Other / Unknown";
}

function withId(id: string, value: Record<string, unknown> | null) {
  return value ? { id, ...value } : null;
}

function extractCityFromAddress(addressText?: unknown): string {
  if (!addressText || typeof addressText !== "string") return "";
  const lower = addressText.toLowerCase();
  for (const item of STATE_KEYWORDS_LIST) {
    for (const kw of item.keywords) {
      if (
        kw === item.state.toLowerCase() ||
        kw.length <= 3 ||
        kw === "north east" ||
        kw === "bengal"
      ) continue;
      if (lower.includes(kw)) {
        return kw.split(" ").map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(" ");
      }
    }
  }
  return "";
}

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();
    const auth = getFirebaseAdminAuth();

    const authUsersById = new Map<string, AuthUserSummary>();
    let pageToken: string | undefined;

    do {
      const page = await auth.listUsers(1000, pageToken);
      page.users.forEach((entry) => {
        authUsersById.set(entry.uid, {
          uid: entry.uid,
          email: entry.email ?? "",
          phone: entry.phoneNumber ?? "",
          displayName: entry.displayName ?? "",
          disabled: entry.disabled,
          createdAt: entry.metadata.creationTime ?? "",
          lastSignInAt: entry.metadata.lastSignInTime ?? ""
        });
      });
      pageToken = page.pageToken;
    } while (pageToken);

    const [
      usersSnapshot,
      phoneRolesSnapshot,
      referralCodesSnapshot,
      workerProfilesSnapshot,
      employerProfilesSnapshot
    ] =
      await Promise.all([
        db.collection("users").limit(5000).get(),
        db.collection("phoneRoles").limit(5000).get(),
        db.collection("referral_codes").limit(5000).get(),
        db.collection("worker_profiles").limit(5000).get(),
        db.collection("employer_profiles").limit(5000).get()
      ]);

    const userDocsById = new Map<string, Record<string, unknown>>();
    usersSnapshot.forEach((item) => {
      userDocsById.set(item.id, asRecord(item.data()));
    });

    const phoneRoleDocsByUid = new Map<string, Array<Record<string, unknown> & { docId: string }>>();
    let phoneRolesMissingUid = 0;
    phoneRolesSnapshot.forEach((item) => {
      const raw = asRecord(item.data());
      const uid = typeof raw.uid === "string" ? raw.uid.trim() : "";
      if (uid) {
        const existing = phoneRoleDocsByUid.get(uid) ?? [];
        existing.push({ ...raw, docId: item.id });
        phoneRoleDocsByUid.set(uid, existing);
      } else {
        phoneRolesMissingUid += 1;
      }
    });

    const referralCodeByUserId = new Map<string, string>();
    const referralCodeDocsByUserId = new Map<string, Array<Record<string, unknown>>>();
    referralCodesSnapshot.forEach((item) => {
      const raw = asRecord(item.data());
      const userId = typeof raw.userId === "string" ? raw.userId : "";
      const code = typeof raw.code === "string" && raw.code.trim() ? raw.code.trim() : item.id;

      if (userId && code) {
        referralCodeByUserId.set(userId, code);
        const existing = referralCodeDocsByUserId.get(userId) ?? [];
        existing.push({ id: item.id, ...raw });
        referralCodeDocsByUserId.set(userId, existing);
      }
    });

    const workerProfileById = new Map<string, Record<string, unknown>>();
    workerProfilesSnapshot.forEach((item) => {
      workerProfileById.set(item.id, asRecord(item.data()));
    });

    const employerProfileById = new Map<string, Record<string, unknown>>();
    employerProfilesSnapshot.forEach((item) => {
      employerProfileById.set(item.id, asRecord(item.data()));
    });

    const userIds = new Set<string>([
      ...userDocsById.keys(),
      ...authUsersById.keys(),
      ...phoneRoleDocsByUid.keys(),
      ...workerProfileById.keys(),
      ...employerProfileById.keys(),
      ...referralCodeByUserId.keys()
    ]);

    const users = Array.from(userIds).map((userId) => {
      const rawDoc = userDocsById.get(userId) ?? {};
      const phoneRoleDocs = phoneRoleDocsByUid.get(userId) ?? [];
      const phoneRole = phoneRoleDocs[0] ?? null;
      const authUser = authUsersById.get(userId);
      const workerProfile = workerProfileById.get(userId) ?? null;
      const employerProfile = employerProfileById.get(userId) ?? null;
      const sourceRoles = {
        phoneRoles: normalizeRole(phoneRole?.role),
        users: normalizeRole(rawDoc.role) || normalizeRole(rawDoc.activeRole),
        worker_profiles: normalizeRole(workerProfile?.role),
        employer_profiles: normalizeRole(employerProfile?.role)
      };
      const canonicalRole = sourceRoles.phoneRoles || sourceRoles.users ||
        sourceRoles.worker_profiles || sourceRoles.employer_profiles || "";
      const roleMismatch = new Set(Object.values(sourceRoles).filter(Boolean)).size > 1;
      const roleSource = sourceRoles.phoneRoles
        ? "phoneRoles"
        : sourceRoles.users
          ? "users"
          : sourceRoles.worker_profiles
            ? "worker_profiles"
            : sourceRoles.employer_profiles
              ? "employer_profiles"
              : "missing";

      const merged = {
        ...rawDoc,
        role: canonicalRole || rawDoc.role,
        activeRole: canonicalRole || rawDoc.activeRole,
        fullName: firstNonEmptyString(
          rawDoc.fullName,
          rawDoc.name,
          phoneRole?.name,
          phoneRole?.fullName,
          workerProfile?.fullName,
          workerProfile?.name,
          employerProfile?.fullName,
          employerProfile?.name,
          employerProfile?.companyName,
          authUser?.displayName
        ),
        phone: firstNonEmptyString(
          rawDoc.phone,
          rawDoc.phoneNumber,
          phoneRole?.phoneNumber,
          phoneRole?.phone,
          workerProfile?.phone,
          workerProfile?.phoneNumber,
          employerProfile?.phone,
          employerProfile?.phoneNumber,
          authUser?.phone
        ),
        email: firstNonEmptyString(
          rawDoc.email,
          workerProfile?.email,
          employerProfile?.email,
          authUser?.email
        ),
        referralCode: firstNonEmptyString(rawDoc.referralCode, referralCodeByUserId.get(userId))
      };

      const normalized = normalizeUserRecord(userId, merged, {
        workerProfile,
        employerProfile,
        referralCodeByUserId: referralCodeByUserId.get(userId)
      });

      const workerLoc = (typeof workerProfile?.location === "object" && workerProfile?.location !== null)
        ? (workerProfile.location as Record<string, unknown>)
        : null;
      const employerBLoc = (typeof employerProfile?.businessLocation === "object" && employerProfile?.businessLocation !== null)
        ? (employerProfile.businessLocation as Record<string, unknown>)
        : null;
      const rawLoc = (typeof rawDoc.location === "object" && rawDoc.location !== null)
        ? (rawDoc.location as Record<string, unknown>)
        : null;

      const state = inferState({
        state: rawDoc.state || workerProfile?.state || employerProfile?.state,
        city: rawDoc.city || rawDoc.companyCity || workerProfile?.city || employerProfile?.city || employerProfile?.companyCity,
        address: rawDoc.address || rawDoc.businessAddress || rawDoc.addressText || workerLoc?.address || workerProfile?.address || workerProfile?.locationText || employerProfile?.businessAddress || employerProfile?.companyAddress || employerProfile?.address,
        locationText: rawDoc.locationText || workerProfile?.locationText || employerProfile?.locationText,
        location: rawDoc.location || workerProfile?.location || employerProfile?.location,
        businessAddress: employerProfile?.businessAddress || rawDoc.businessAddress,
        businessLocation: employerProfile?.businessLocation || rawDoc.businessLocation,
        lat: rawDoc.lat || rawLoc?.lat || workerLoc?.lat || workerProfile?.lat || employerBLoc?.lat || employerProfile?.lat,
        lng: rawDoc.lng || rawLoc?.lng || workerLoc?.lng || workerProfile?.lng || employerBLoc?.lng || employerProfile?.lng,
        phone: normalized.phone
      });

      const city = firstNonEmptyString(
        rawDoc.city,
        rawDoc.companyCity,
        workerProfile?.city,
        employerProfile?.city,
        employerProfile?.companyCity,
        typeof workerLoc?.city === "string" ? workerLoc.city : "",
        typeof employerBLoc?.city === "string" ? employerBLoc.city : "",
        extractCityFromAddress(
          rawDoc.address ||
          rawDoc.businessAddress ||
          workerLoc?.address ||
          workerProfile?.address ||
          employerProfile?.businessAddress ||
          employerProfile?.companyAddress
        )
      );

      return {
        id: userId,
        fullName: normalized.fullName,
        name: normalized.fullName,
        phone: normalized.phone,
        email: normalized.email,
        role: normalized.role,
        activeRole: normalized.activeRole,
        roles: normalized.roles,
        roleSource,
        state,
        city,
        phoneRoleDocId: phoneRole?.docId ?? "",
        phoneRoleUid: phoneRole?.uid ?? "",
        phoneRoleRole: phoneRole?.role ?? "",
        phoneRoleName: phoneRole?.name ?? "",
        phoneRolePhoneNumber: phoneRole?.phoneNumber ?? "",
        phoneRoleCreatedAt: phoneRole?.createdAt ?? null,
        phoneRoleUpdatedAt: phoneRole?.updatedAt ?? null,
        workerProfileRole: workerProfile?.role ?? "",
        employerProfileRole: employerProfile?.role ?? "",
        canonicalRole: canonicalRole || "MISSING",
        sourceRoles,
        roleMismatch,
        phoneRoleDuplicateCount: phoneRoleDocs.length,
        referralCode: normalized.referralCode,
        createdAt: normalized.joinedAt,
        isBanned: rawDoc.isBanned === true,
        isVerified: rawDoc.isVerified === true,
        hasUserDoc: userDocsById.has(userId),
        hasPhoneRole: Boolean(phoneRole),
        hasAuthUser: authUsersById.has(userId),
        hasWorkerProfile: Boolean(workerProfile),
        hasEmployerProfile: Boolean(employerProfile),
        hasReferralCodeDoc: (referralCodeDocsByUserId.get(userId) ?? []).length > 0,
        firebaseFields: {
          phoneRoles: phoneRoleDocs,
          users: withId(userId, userDocsById.get(userId) ?? null),
          auth: authUser ?? null,
          worker_profiles: withId(userId, workerProfile),
          employer_profiles: withId(userId, employerProfile),
          referral_codes: referralCodeDocsByUserId.get(userId) ?? []
        }
      };
    }).sort((a, b) => String(b.createdAt ?? "").localeCompare(String(a.createdAt ?? "")));

    const roleCounts = users.reduce(
      (acc, user) => {
        const role = user.canonicalRole;
        if (role === "WORKER") acc.workers += 1;
        else if (role === "EMPLOYER") acc.employers += 1;
        else if (role === "ADMIN") acc.admins += 1;
        else if (role === "MISSING") acc.missing += 1;
        else acc.other += 1;
        return acc;
      },
      { workers: 0, employers: 0, admins: 0, missing: 0, other: 0 }
    );

    const stateCounts = users.reduce<Record<string, { workers: number; employers: number; admins: number; total: number }>>(
      (acc, user) => {
        const st = user.state || "Other / Unknown";
        if (!acc[st]) {
          acc[st] = { workers: 0, employers: 0, admins: 0, total: 0 };
        }
        acc[st].total += 1;
        if (user.canonicalRole === "WORKER" || user.role === "WORKER" || user.activeRole === "WORKER") {
          acc[st].workers += 1;
        } else if (user.canonicalRole === "EMPLOYER" || user.role === "EMPLOYER" || user.activeRole === "EMPLOYER") {
          acc[st].employers += 1;
        } else if (user.canonicalRole === "ADMIN") {
          acc[st].admins += 1;
        }
        return acc;
      },
      {}
    );

    const sourceCounts = {
      identities: users.length,
      users: usersSnapshot.size,
      phoneRoles: phoneRolesSnapshot.size,
      authUsers: authUsersById.size,
      workerProfiles: workerProfilesSnapshot.size,
      employerProfiles: employerProfilesSnapshot.size,
      referralCodes: referralCodesSnapshot.size
    };

    const integrityCounts = {
      missingPhoneRole: users.filter((user) => !user.hasPhoneRole).length,
      missingAuthUser: users.filter((user) => !user.hasAuthUser).length,
      roleMismatch: users.filter((user) => user.roleMismatch).length,
      duplicatePhoneRoleUsers: Array.from(phoneRoleDocsByUid.values()).filter((docs) => docs.length > 1).length,
      phoneRolesMissingUid
    };

    return NextResponse.json({ users, sourceCounts, roleCounts, stateCounts, integrityCounts });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load users.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type UpdateUserBody = {
  userId?: string;
  newRole?: string;
  fullName?: string;
  phone?: string;
  isBanned?: boolean;
  banReason?: string;
  isVerified?: boolean;
};

export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let body: UpdateUserBody;

  try {
    body = (await request.json()) as UpdateUserBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const userId = body.userId?.trim();
  const newRole = body.newRole?.trim().toUpperCase();
  const fullName = typeof body.fullName === "string" ? body.fullName.trim() : undefined;
  const phone = typeof body.phone === "string" ? normalizePhoneForDoc(body.phone) : undefined;
  const isBanned = typeof body.isBanned === "boolean" ? body.isBanned : undefined;
  const isVerified = typeof body.isVerified === "boolean" ? body.isVerified : undefined;
  const banReason = typeof body.banReason === "string" ? body.banReason.trim() : undefined;

  if (!userId) {
    return NextResponse.json({ error: "Invalid userId." }, { status: 400 });
  }

  const hasRoleUpdate = Boolean(newRole);
  if (hasRoleUpdate && newRole !== "WORKER" && newRole !== "EMPLOYER") {
    return NextResponse.json({ error: "Invalid role." }, { status: 400 });
  }

  if (
    !hasRoleUpdate &&
    fullName === undefined &&
    phone === undefined &&
    isBanned === undefined &&
    isVerified === undefined
  ) {
    return NextResponse.json({ error: "No updatable fields were provided." }, { status: 400 });
  }

  try {
    const db = getFirebaseAdminDb();
    const auth = getFirebaseAdminAuth();
    const ref = db.collection("users").doc(userId);

    const payload: Record<string, unknown> = {};

    if (hasRoleUpdate && newRole) {
      payload.role = newRole;
      payload.activeRole = newRole;
    }

    if (fullName !== undefined) {
      payload.fullName = fullName;
    }

    if (phone !== undefined) {
      payload.phone = phone;
      payload.phoneNumber = phone;
    }

    if (isBanned !== undefined) {
      payload.isBanned = isBanned;
      payload.bannedAt = isBanned ? new Date() : null;
      if (isBanned && banReason) payload.banReason = banReason;
      if (!isBanned) payload.banReason = null;
    }

    if (isVerified !== undefined) {
      payload.isVerified = isVerified;
      payload.verifiedAt = isVerified ? new Date() : null;
    }

    if (Object.keys(payload).length > 0) {
      await ref.set(payload, { merge: true });
    }

    const phoneRolePayload: Record<string, unknown> = {};
    if (hasRoleUpdate && newRole) {
      phoneRolePayload.role = newRole;
    }
    if (fullName !== undefined) {
      phoneRolePayload.name = fullName;
    }
    if (phone !== undefined) {
      phoneRolePayload.phoneNumber = phone;
    }

    if (Object.keys(phoneRolePayload).length > 0) {
      const phoneRoleMatches = await db.collection("phoneRoles")
        .where("uid", "==", userId)
        .limit(5)
        .get();
      const withTimestamp = { ...phoneRolePayload, uid: userId, updatedAt: new Date() };

      if (!phoneRoleMatches.empty) {
        const batch = db.batch();
        phoneRoleMatches.docs.forEach((doc) => {
          if (phone && doc.id !== phone) {
            batch.set(
              db.collection("phoneRoles").doc(phone),
              { ...doc.data(), ...withTimestamp, phoneNumber: phone },
              { merge: true }
            );
            batch.delete(doc.ref);
          } else {
            batch.set(doc.ref, withTimestamp, { merge: true });
          }
        });
        await batch.commit();
      } else if (phone && newRole) {
        await db.collection("phoneRoles").doc(phone).set(
          {
            phoneNumber: phone,
            role: newRole,
            ...(fullName ? { name: fullName } : {}),
            uid: userId,
            createdAt: new Date(),
            updatedAt: new Date()
          },
          { merge: true }
        );
      }
    }

    const profilePayload: Record<string, unknown> = {};
    if (hasRoleUpdate && newRole) {
      profilePayload.role = newRole;
    }
    if (fullName !== undefined) {
      profilePayload.fullName = fullName;
    }
    if (phone !== undefined) {
      profilePayload.phone = phone;
    }

    if (Object.keys(profilePayload).length > 0) {
      const withProfileTimestamp = {
        ...profilePayload,
        userId,
        updatedAt: new Date()
      };

      if (hasRoleUpdate && newRole) {
        const profileCollection = newRole === "EMPLOYER" ? "employer_profiles" : "worker_profiles";
        await db.collection(profileCollection).doc(userId).set(
          withProfileTimestamp,
          { merge: true }
        );
      } else {
        const [workerProfile, employerProfile] = await Promise.all([
          db.collection("worker_profiles").doc(userId).get(),
          db.collection("employer_profiles").doc(userId).get()
        ]);
        const batch = db.batch();
        if (workerProfile.exists) {
          batch.set(workerProfile.ref, withProfileTimestamp, { merge: true });
        }
        if (employerProfile.exists) {
          batch.set(employerProfile.ref, withProfileTimestamp, { merge: true });
        }
        if (workerProfile.exists || employerProfile.exists) {
          await batch.commit();
        }
      }
    }

    if (isBanned !== undefined) {
      try {
        await auth.updateUser(userId, { disabled: isBanned });
      } catch {
        // Best-effort: doc update is the source of truth.
      }
    }

    if (fullName !== undefined) {
      try {
        await auth.updateUser(userId, { displayName: fullName || undefined });
      } catch {
        // Keep users-doc update as source of truth if auth update fails.
      }
    }

    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to update role.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type DeleteUserBody = {
  userId?: string;
};

export async function DELETE(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let body: DeleteUserBody;

  try {
    body = (await request.json()) as DeleteUserBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const userId = body.userId?.trim();

  if (!userId) {
    return NextResponse.json({ error: "Missing userId." }, { status: 400 });
  }

  try {
    const db = getFirebaseAdminDb();
    const auth = getFirebaseAdminAuth();

    await Promise.all([
      db.collection("users").doc(userId).delete(),
      db.collection("worker_profiles").doc(userId).delete(),
      db.collection("employer_profiles").doc(userId).delete(),
      db.collection("referral_stats").doc(userId).delete()
    ]);

    const referralCodes = await db.collection("referral_codes").where("userId", "==", userId).get();
    if (!referralCodes.empty) {
      const batch = db.batch();
      referralCodes.docs.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();
    }

    try {
      await auth.deleteUser(userId);
    } catch (error) {
      const code = (error as { code?: string })?.code;
      if (code !== "auth/user-not-found") {
        throw error;
      }
    }

    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to delete user.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
