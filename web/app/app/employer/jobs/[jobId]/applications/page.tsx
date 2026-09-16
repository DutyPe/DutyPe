import { EmployerApplicationsClient } from "@/components/product/employer-app";
import { ProductRoleBoundary } from "@/components/product/product-shell";

type EmployerJobApplicationsPageProps = {
  params: {
    jobId: string;
  };
};

export default function EmployerJobApplicationsPage({
  params
}: EmployerJobApplicationsPageProps) {
  return (
    <ProductRoleBoundary
      currentPath="/app/employer/jobs"
      description="Review applicants for one employer job with search, status filters, and action controls."
      requiredRole="EMPLOYER"
      title="Job applicants"
    >
      {(session) => (
        <EmployerApplicationsClient
          backHref="/app/employer/jobs"
          jobId={params.jobId}
          session={session}
        />
      )}
    </ProductRoleBoundary>
  );
}
