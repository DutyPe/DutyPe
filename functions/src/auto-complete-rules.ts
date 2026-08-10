/**
 * Pure timing rules for auto-completing work.
 *
 * A hired worker should not stay in limbo because the employer never pressed a button,
 * so the platform closes the job itself after a grace period. Kept free of Firestore so
 * the timings can be unit tested.
 */

export const AUTO_COMPLETE_RULES = {
  /** Regular vacancy jobs settle 6 hours after the worker is hired. */
  STANDARD_GRACE_MS: 6 * 60 * 60 * 1000,
  /** Instant/urgent work is same-day, so it settles 3 hours after the worker is accepted. */
  INSTANT_GRACE_MS: 3 * 60 * 60 * 1000,
  /** Nothing older than this is touched, so a backlog can never be closed retroactively. */
  MAX_LOOKBACK_MS: 30 * 24 * 60 * 60 * 1000,
};

export type WorkKind = "standard" | "instant";

export type AutoCompleteInput = {
  status: string;
  /** When the worker was hired (standard) or accepted (instant), in epoch ms. */
  startedAtMs: number;
  nowMs: number;
  kind: WorkKind;
};

export type AutoCompleteDecision =
  | { complete: true; graceMs: number }
  | { complete: false; reason: string };

export function graceMsFor(kind: WorkKind): number {
  return kind === "instant"
    ? AUTO_COMPLETE_RULES.INSTANT_GRACE_MS
    : AUTO_COMPLETE_RULES.STANDARD_GRACE_MS;
}

/** Statuses that mean "worker is on the job" for each kind of work. */
export function isActiveWorkStatus(status: string, kind: WorkKind): boolean {
  const normalized = String(status ?? "").toLowerCase().trim();
  return kind === "instant"
    ? normalized === "accepted"
    : normalized === "hired" || normalized === "accepted" || normalized === "in_progress";
}

export function shouldAutoComplete(input: AutoCompleteInput): AutoCompleteDecision {
  const { status, startedAtMs, nowMs, kind } = input;

  if (!isActiveWorkStatus(status, kind)) {
    return { complete: false, reason: "not-active" };
  }
  if (!Number.isFinite(startedAtMs) || startedAtMs <= 0) {
    return { complete: false, reason: "no-start-time" };
  }
  // A start time in the future means clock skew or bad data; never act on it.
  if (startedAtMs > nowMs) {
    return { complete: false, reason: "start-in-future" };
  }

  const elapsed = nowMs - startedAtMs;
  if (elapsed > AUTO_COMPLETE_RULES.MAX_LOOKBACK_MS) {
    return { complete: false, reason: "too-old" };
  }

  const graceMs = graceMsFor(kind);
  if (elapsed < graceMs) {
    return { complete: false, reason: "within-grace" };
  }

  return { complete: true, graceMs };
}

/** The cutoff to use in a Firestore range query, so the scan stays bounded. */
export function autoCompleteCutoffMs(nowMs: number, kind: WorkKind): number {
  return nowMs - graceMsFor(kind);
}

export function autoCompleteFloorMs(nowMs: number): number {
  return nowMs - AUTO_COMPLETE_RULES.MAX_LOOKBACK_MS;
}
