import { AppRoutePage } from "@/components/public/app-route-page";
import { getBridgeMetadata } from "@/lib/bridge-metadata";

export const metadata = getBridgeMetadata({
  title: "Post a Job Vacancy - DutyPe Employer",
  description: "Post a job vacancy on DutyPe and hire verified local workers within 10 minutes. Zero commission, direct phone hiring.",
  keywords: ["hire workers", "post jobs", "employer hiring app", "local staff hiring", "dutype post job"]
});

export default function EmployerPostJobRoutePage() {
  return (
    <AppRoutePage
      kind="employer"
      eyebrow="Quick Hiring"
      icon="📢"
      title="Post your vacancy and hire verified staff today."
      description="Connect directly with over 300+ verified local workers near you. Zero commission, direct phone hiring within 10 minutes."
      launchHeadline="Open Post Job in DutyPe App"
      launchDescription="Continue inside the DutyPe Android app to publish your vacancy in less than 2 minutes."
      bullets={[
        "48-Hour Free Trial or post for ₹49",
        "Direct phone calls & WhatsApp with applicants",
        "Over 300+ verified local workers ready to join"
      ]}
    />
  );
}
