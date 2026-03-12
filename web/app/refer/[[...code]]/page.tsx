import { AppRoutePage } from "@/components/public/app-route-page";
import { bridgeMetadata } from "@/lib/bridge-metadata";

type Props = {
  params: {
    code?: string[];
  };
};

export const metadata = bridgeMetadata;

export default function ReferPage({ params }: Props) {
  const referralCode = params.code?.[0] ?? "No code";

  return (
    <AppRoutePage
      kind="refer"
      entityId={referralCode === "No code" ? undefined : referralCode}
      eyebrow="Referral invite"
      icon="🎁"
      title="You're invited to join DutyPe."
      description="Open the app with this referral link to continue sign-up and reward verification."
      valueLabel="Referral code"
      value={referralCode}
      valueHint="Sign-up bonus: Rs 25"
      launchHeadline="Open your referral invite in DutyPe"
      launchDescription="Referral redemption, profile completion, and reward verification continue inside the app."
      bullets={[
        "Find local jobs near you",
        "Apply instantly with one tap",
        "Earn referral rewards after verification"
      ]}
    />
  );
}
