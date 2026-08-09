"use client";

import { useEffect, useState } from "react";

export function CallStatsDisplay() {
  const [totalCalls, setTotalCalls] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function fetchStats() {
      try {
        setIsLoading(true);
        const response = await fetch("/api/stats");
        if (response.ok) {
          const data = await response.json();
          setTotalCalls(data.totalCalls);
        }
      } catch (error) {
        console.error("Failed to fetch call stats", error);
        setTotalCalls(0);
      } finally {
        setIsLoading(false);
      }
    }
    fetchStats();
  }, []);

  return (
    <div className="marketing-hero-stat">
      {isLoading ? (
        <strong>…</strong>
      ) : (
        <strong>
          {totalCalls !== null
            ? new Intl.NumberFormat("en-IN").format(totalCalls)
            : "N/A"}
        </strong>
      )}
      <span>Total Calls Made</span>
    </div>
  );
}
