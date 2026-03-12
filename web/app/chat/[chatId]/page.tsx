import { AppRoutePage } from "@/components/public/app-route-page";
import { getBridgeMetadata } from "@/lib/bridge-metadata";

type Props = {
  params: {
    chatId: string;
  };
};

export const metadata = getBridgeMetadata({
  title: "Job Chat - DutyPe",
  description: "Continue worker-employer job chat inside DutyPe with safety controls and job context.",
  keywords: ["job chat", "worker employer chat", "hiring conversation", "in-app messaging"]
});

export default function ChatRoutePage({ params }: Props) {
  return (
    <AppRoutePage
      kind="chat"
      entityId={params.chatId}
      eyebrow="Chat route"
      icon="💬"
      title="Continue this conversation in DutyPe."
      description="Chat belongs in the live product where worker and employer context already exist."
      bullets={[
        "Open the matching chat thread",
        "Use in-app safety and reporting controls",
        "Keep conversation history tied to the job flow"
      ]}
    />
  );
}
