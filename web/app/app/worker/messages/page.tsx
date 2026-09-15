import { WorkerAppActions } from "@/components/public/job-discovery";
import { SiteShell } from "@/components/site-shell";

export default function WorkerMessagesPage() {
  return (
    <SiteShell>
      <section className="directory-page-header">
        <h1>Contact employers in the DutyPe app</h1>
        <p>Continue your job applications and conversations on Android.</p>
        <WorkerAppActions />
      </section>
    </SiteShell>
  );
}
