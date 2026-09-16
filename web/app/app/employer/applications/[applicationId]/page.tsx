import { EmployerApplicationDetailClient } from "@/components/product/employer-review";
import { ProductRoleBoundary } from "@/components/product/product-shell";

type EmployerApplicationDetailPageProps = {
  params: {
    applicationId: string;
  };
};

export default function EmployerApplicationDetailPage({
  params
}: EmployerApplicationDetailPageProps) {
  return (
    <ProductRoleBoundary
      currentPath="/app/employer/applications"
      description="Review one worker application with profile context, timeline, and direct employer actions."
      requiredRole="EMPLOYER"
      title="Application detail"
    >
      {(session) => (
        <EmployerApplicationDetailClient
          applicationId={params.applicationId}
          session={session}
        />
      )}
    </ProductRoleBoundary>
  );
}
