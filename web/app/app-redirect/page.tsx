import { AppRoutePage } from "@/components/public/app-route-page";
import { getBridgeMetadata } from "@/lib/bridge-metadata";
import { type DeepLinkKind } from "@/lib/public-site";

type Props = {
  searchParams?: {
    kind?: string;
    id?: string;
  };
};

const supportedKinds = new Set<DeepLinkKind>([
  "home",
  "job",
  "worker",
  "employer",
  "refer",
  "application",
  "chat",
  "profile",
  "notifications"
]);

export const metadata = getBridgeMetadata({
  title: "Open DutyPe App",
  description: "Launch DutyPe app routes for jobs, profiles, referrals, notifications, and chat.",
  keywords: ["open app", "job app redirect", "deep link jobs", "DutyPe app"]
});

export default function AppRedirectPage({ searchParams }: Props) {
  const requestedKind = searchParams?.kind ?? "home";
  const kind = supportedKinds.has(requestedKind as DeepLinkKind)
    ? (requestedKind as DeepLinkKind)
    : "home";

  return (
    <AppRoutePage
      kind={kind}
      entityId={searchParams?.id}
      eyebrow="App redirect"
      icon="📱"
      title="Opening DutyPe..."
      description="If the app does not open automatically, use the download link below."
      launchHeadline="Open DutyPe App"
      launchDescription="This route preserves the old generic app redirect page from the legacy public site."
      bullets={[
        "Apply to jobs in seconds",
        "Find hyperlocal opportunities near you",
        "Continue inside the live app experience"
      ]}
    />
  );
}
