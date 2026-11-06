import { HungrWaitlistClient } from "@/components/hungr/hungr-waitlist-client";

export const metadata = {
  title: "Hungr | Morning Tiffins & Snacks at Offline Shop Prices",
  description: "Pre-order hot tiffins, juices & evening snacks at exact shop rates (₹30-₹40). Batch delivered fresh to your gate by 7:30 AM with zero markup.",
  openGraph: {
    title: "Hungr — Morning Tiffins at Exact Shop Price",
    description: "Save ₹60 per order. Pre-order local Khammam tiffins & fresh juices at shop price delivered to your apartment gate.",
    url: "https://dutype.in/hungr"
  }
};

export default function HungrPage() {
  return <HungrWaitlistClient />;
}
