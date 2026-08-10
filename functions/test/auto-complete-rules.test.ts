import { describe, it } from "node:test";
import * as assert from "node:assert/strict";

import {
  AUTO_COMPLETE_RULES,
  autoCompleteCutoffMs,
  autoCompleteFloorMs,
  graceMsFor,
  isActiveWorkStatus,
  shouldAutoComplete,
  WorkKind,
} from "../src/auto-complete-rules";

const NOW = Date.parse("2026-08-10T12:00:00Z");
const HOUR = 60 * 60 * 1000;

function input(overrides: Partial<Parameters<typeof shouldAutoComplete>[0]> = {}) {
  return {
    status: "hired",
    startedAtMs: NOW - 7 * HOUR,
    nowMs: NOW,
    kind: "standard" as WorkKind,
    ...overrides,
  };
}

describe("grace periods", () => {
  it("gives regular jobs six hours", () => {
    assert.equal(graceMsFor("standard"), 6 * HOUR);
  });

  it("gives instant work three hours", () => {
    assert.equal(graceMsFor("instant"), 3 * HOUR);
  });
});

describe("shouldAutoComplete for regular jobs", () => {
  it("does not close a job that was just hired", () => {
    const result = shouldAutoComplete(input({ startedAtMs: NOW - 1 * HOUR }));
    assert.deepEqual(result, { complete: false, reason: "within-grace" });
  });

  it("holds until the sixth hour has fully elapsed", () => {
    const justUnder = shouldAutoComplete(input({ startedAtMs: NOW - 6 * HOUR + 1 }));
    assert.equal(justUnder.complete, false);

    const exactly = shouldAutoComplete(input({ startedAtMs: NOW - 6 * HOUR }));
    assert.equal(exactly.complete, true);
  });

  it("closes a job hired more than six hours ago", () => {
    const result = shouldAutoComplete(input({ startedAtMs: NOW - 7 * HOUR }));
    assert.equal(result.complete, true);
  });

  it("accepts the legacy hired-equivalent statuses", () => {
    for (const status of ["hired", "accepted", "in_progress", "HIRED"]) {
      assert.equal(shouldAutoComplete(input({ status })).complete, true, status);
    }
  });

  it("never touches terminal or pre-hire states", () => {
    for (const status of ["applied", "shortlisted", "completed", "rejected", "withdrawn", ""]) {
      const result = shouldAutoComplete(input({ status }));
      assert.deepEqual(result, { complete: false, reason: "not-active" }, status);
    }
  });
});

describe("shouldAutoComplete for instant work", () => {
  const instant = (overrides = {}) => input({ kind: "instant", status: "accepted", ...overrides });

  it("closes instant work three hours after acceptance", () => {
    assert.equal(shouldAutoComplete(instant({ startedAtMs: NOW - 3 * HOUR })).complete, true);
  });

  it("holds instant work inside the three hour window", () => {
    const result = shouldAutoComplete(instant({ startedAtMs: NOW - 2 * HOUR }));
    assert.deepEqual(result, { complete: false, reason: "within-grace" });
  });

  it("would still be inside the window under the regular six hour rule", () => {
    // Guards against instant work silently inheriting the standard grace period.
    const at4h = NOW - 4 * HOUR;
    assert.equal(shouldAutoComplete(instant({ startedAtMs: at4h })).complete, true);
    assert.equal(
      shouldAutoComplete(input({ startedAtMs: at4h, status: "hired" })).complete,
      false
    );
  });

  it("ignores hired, which is not an instant-side status", () => {
    const result = shouldAutoComplete(instant({ status: "hired" }));
    assert.deepEqual(result, { complete: false, reason: "not-active" });
  });
});

describe("bad or hostile timestamps", () => {
  it("refuses to act without a start time", () => {
    for (const startedAtMs of [0, NaN, -1, Infinity]) {
      const result = shouldAutoComplete(input({ startedAtMs }));
      assert.equal(result.complete, false, String(startedAtMs));
    }
  });

  it("refuses a start time in the future", () => {
    const result = shouldAutoComplete(input({ startedAtMs: NOW + HOUR }));
    assert.deepEqual(result, { complete: false, reason: "start-in-future" });
  });

  it("leaves very old records alone rather than mass-closing a backlog", () => {
    const result = shouldAutoComplete(input({ startedAtMs: NOW - 400 * 24 * HOUR }));
    assert.deepEqual(result, { complete: false, reason: "too-old" });
  });

  it("still closes something just inside the lookback window", () => {
    const result = shouldAutoComplete(
      input({ startedAtMs: NOW - AUTO_COMPLETE_RULES.MAX_LOOKBACK_MS + HOUR })
    );
    assert.equal(result.complete, true);
  });
});

describe("query bounds", () => {
  it("derives a cutoff matching the grace period", () => {
    assert.equal(autoCompleteCutoffMs(NOW, "standard"), NOW - 6 * HOUR);
    assert.equal(autoCompleteCutoffMs(NOW, "instant"), NOW - 3 * HOUR);
  });

  it("derives a floor that bounds the scan", () => {
    assert.equal(autoCompleteFloorMs(NOW), NOW - AUTO_COMPLETE_RULES.MAX_LOOKBACK_MS);
    assert.ok(autoCompleteFloorMs(NOW) < autoCompleteCutoffMs(NOW, "standard"));
  });

  it("anything at the cutoff is decided as complete", () => {
    // The query and the decision have to agree, or the sweep would fetch rows it skips.
    const cutoff = autoCompleteCutoffMs(NOW, "instant");
    assert.equal(
      shouldAutoComplete({ status: "accepted", startedAtMs: cutoff, nowMs: NOW, kind: "instant" }).complete,
      true
    );
  });
});

describe("isActiveWorkStatus", () => {
  it("is case and whitespace tolerant", () => {
    assert.equal(isActiveWorkStatus("  Hired ", "standard"), true);
    assert.equal(isActiveWorkStatus("ACCEPTED", "instant"), true);
  });
});
