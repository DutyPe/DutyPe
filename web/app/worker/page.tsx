import { AppRoutePage } from "@/components/public/app-route-page";
import { getBridgeMetadata } from "@/lib/bridge-metadata";

export const metadata = getBridgeMetadata({
  title: "Worker Profile - DutyPe",
  description: "Open worker profile and continue with hiring, trust, and contact flow in DutyPe app.",
  keywords: ["worker profile", "hire workers", "worker details", "open worker app"]
});

export default function WorkerRoutePage() {
  return (
    <AppRoutePage
      kind="worker"
      eyebrow="Worker route"
      icon="👷"
      title="Open worker profile in DutyPe."
      description="View worker details, trust signals, and hiring actions in the live app experience."
      launchHeadline="Open Worker Profile in DutyPe"
      launchDescription="Worker profile details and communication continue inside the DutyPe app."
      bullets={[
        "View verified worker profile details",
        "Check ratings and trust signals",
        "Continue in-app contact and hiring flow"
      ]}
    />
  );
}
