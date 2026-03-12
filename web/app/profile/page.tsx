import { AppRoutePage } from "@/components/public/app-route-page";
import { bridgeMetadata } from "@/lib/bridge-metadata";

export const metadata = bridgeMetadata;

export default function ProfileRoutePage() {
  return (
    <AppRoutePage
      kind="profile"
      eyebrow="Profile route"
      icon="👤"
      title="Open your profile in DutyPe."
      description="Profile editing, verification, trust signals, and account actions stay inside the app."
      bullets={[
        "Edit worker or employer profile",
        "Manage verification details",
        "Continue in the live product flow"
      ]}
    />
  );
}
