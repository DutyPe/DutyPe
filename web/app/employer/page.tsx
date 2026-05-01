import { AppRoutePage } from "@/components/public/app-route-page";
import { getBridgeMetadata } from "@/lib/bridge-metadata";

export const metadata = getBridgeMetadata({
  title: "Employer Hiring Flow - DutyPe",
  description: "Open the DutyPe employer hiring flow to post jobs, review applicants, and hire nearby workers.",
  keywords: ["hire workers", "post jobs", "employer hiring app", "local staff hiring"]
});

export default function EmployerRoutePage() {
  return (
    <AppRoutePage
      kind="employer"
      eyebrow="Employer route"
      icon="🏢"
      title="Post jobs and hire local workers in DutyPe."
      description="Open the employer hiring flow to publish openings, review applicants, and continue hiring inside DutyPe."
      launchHeadline="Open Employer Flow in DutyPe"
      launchDescription="Employer job posting, applicant review, and hiring actions continue inside the DutyPe app."
      bullets={[
        "Post local jobs for nearby workers",
        "Review worker applications and profiles",
        "Continue hiring with direct in-app actions"
      ]}
    />
  );
}