import { FieldJoinForm } from "@/components/public/field-join-form";

export const metadata = {
  title: "DutyPe help desk registration",
  description: "Register for jobs, urgent work, home services or as a DutyPe service partner.",
  robots: { index: false, follow: false }
};

export default function JoinPage() {
  return <FieldJoinForm />;
}
