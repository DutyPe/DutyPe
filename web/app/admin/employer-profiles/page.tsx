"use client";

import { useState } from "react";
import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCollectionViewer } from "@/components/admin/admin-collection-viewer";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { ShieldAlert, RefreshCw, CheckCircle2 } from "lucide-react";

export default function AdminEmployerProfilesPage() {
  const [cleaning, setCleaning] = useState(false);
  const [resultMessage, setResultMessage] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const handleCleanCampaign = async () => {
    if (!confirm("Are you sure you want to remove UNLIMITED_CAMPAIGN from all employers? Their status will become NONE and they will need to buy a Razorpay plan to post jobs or unlock contacts.")) {
      return;
    }
    setCleaning(true);
    setResultMessage(null);
    try {
      const res = await adminApiFetch("/api/admin/clean-campaign-subscriptions", {
        method: "POST",
      });
      const data = await res.json();
      if (res.ok && data.ok) {
        setResultMessage(`Success: ${data.message}`);
        setRefreshKey((k) => k + 1);
      } else {
        setResultMessage(data.error || "Failed to clean up campaign subscriptions.");
      }
    } catch (e: any) {
      setResultMessage(`Error: ${e?.message || "Operation failed"}`);
    } finally {
      setCleaning(false);
    }
  };

  return (
    <AdminAuthGate>
      <AdminShell title="Employer Profiles" description="Current employer_profiles collection fields.">
        <div className="mb-6 p-4 rounded-xl border border-amber-200 bg-amber-50/80 dark:bg-amber-950/30 dark:border-amber-800/50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <ShieldAlert className="w-5 h-5 text-amber-600 dark:text-amber-400 mt-0.5 shrink-0" />
            <div>
              <h4 className="text-sm font-semibold text-amber-900 dark:text-amber-200">
                Subscription Plan Enforcement (Razorpay Protection)
              </h4>
              <p className="text-xs text-amber-700 dark:text-amber-300/80 mt-0.5">
                Reset any active &quot;UNLIMITED_CAMPAIGN&quot; subscriptions to &quot;NONE&quot;. Employers must purchase an active Razorpay plan to post jobs, hire instantly, and unlock candidate contacts.
              </p>
              {resultMessage && (
                <p className="text-xs font-semibold text-emerald-700 dark:text-emerald-400 mt-2 flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4" /> {resultMessage}
                </p>
              )}
            </div>
          </div>
          <button
            onClick={handleCleanCampaign}
            disabled={cleaning}
            className="px-4 py-2 text-xs font-semibold rounded-lg bg-amber-600 hover:bg-amber-700 text-white transition-colors flex items-center gap-2 whitespace-nowrap shadow-sm disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${cleaning ? "animate-spin" : ""}`} />
            {cleaning ? "Resetting Subscriptions..." : "Reset All UNLIMITED_CAMPAIGN"}
          </button>
        </div>

        <AdminCollectionViewer
          key={refreshKey}
          apiPath="/api/admin/employer-profiles"
          dataKey="profiles"
          label="Employer Profiles"
        />
      </AdminShell>
    </AdminAuthGate>
  );
}
