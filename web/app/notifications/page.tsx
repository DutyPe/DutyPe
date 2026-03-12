import { AppRoutePage } from "@/components/public/app-route-page";
import { bridgeMetadata } from "@/lib/bridge-metadata";

export const metadata = bridgeMetadata;

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
