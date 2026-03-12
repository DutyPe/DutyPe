import { EmployerEditJobClient } from "@/components/product/employer-app";
import { ProductRoleBoundary } from "@/components/product/product-shell";

type EmployerEditJobPageProps = {
  params: {
    jobId: string;
  };
};

export default function EmployerEditJobPage({ params }: EmployerEditJobPageProps) {
  return (
    <ProductRoleBoundary
      currentPath="/app/employer/jobs"
      description="Update an employer job with the same saved-location workflow and edit window used in Android."
      requiredRole="EMPLOYER"
      title="Edit job"
    >
      {(session) => <EmployerEditJobClient jobId={params.jobId} session={session} />}
    </ProductRoleBoundary>
  );
}
