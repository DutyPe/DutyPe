import { Metadata } from "next";

import { AdminLoginClient } from "@/components/admin/admin-login-client";

export const metadata: Metadata = {
  title: "Admin Login",
  description: "Firebase-backed admin sign-in for the DutyPe web app."
};

export default function AdminLoginPage() {
  return <AdminLoginClient />;
}
