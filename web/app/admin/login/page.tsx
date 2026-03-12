import { Metadata } from "next";
import { redirect } from "next/navigation";

import { AdminLoginClient } from "@/components/admin/admin-login-client";
import { getAdminSession } from "@/lib/firebase/admin-session";

export const metadata: Metadata = {
  title: "Admin Login",
  description: "Firebase-backed admin sign-in for the DutyPe web app."
};

export default async function AdminLoginPage() {
  const session = await getAdminSession();

  if (session) {
    redirect("/admin");
  }

  return <AdminLoginClient />;
}
