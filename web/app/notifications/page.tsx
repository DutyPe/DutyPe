import { AppRoutePage } from "@/components/public/app-route-page";
import { getBridgeMetadata } from "@/lib/bridge-metadata";

export const metadata = getBridgeMetadata({
  title: "Job Notifications - DutyPe",
  description: "Open DutyPe notifications for job alerts, application status updates, and hiring messages.",
  keywords: ["job alerts", "application updates", "hiring notifications", "job notification app"]
});

export default function NotificationsRoutePage() {
  return (
    <AppRoutePage
      kind="notifications"
      eyebrow="Notifications route"
      icon="🔔"
      title="Open your notifications in DutyPe."
      description="Job alerts, application updates, and trust notifications continue inside the app."
      bullets={[
        "View live application updates",
        "See new nearby job alerts",
        "Continue in the app"
      ]}
    />
  );
}
