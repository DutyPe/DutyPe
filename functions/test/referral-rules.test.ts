import { describe, it } from "node:test";
import * as assert from "node:assert/strict";

import {
  calculateTier,
  canWithdraw,
  evaluateWithdrawal,
  generateIdempotencyKey,
  getConfiguredMilestoneBonus,
  getMilestoneBonus,
  normalizeReferralCodeInput,
  scoreReferralFraud,
  timestampToMillis,
  welcomeBonusAmountForRole,
  WithdrawalInput,
} from "../src/referral-rules";

function withdrawal(overrides: Partial<WithdrawalInput> = {}): WithdrawalInput {
  return {
    amount: 100,
    availableBalance: 100,
    minWithdrawal: 100,
    maxWithdrawalPerDay: 1000,
    alreadyWithdrawnToday: 0,
    canWithdrawFlag: true,
    paymentMethod: "UPI",
    upiId: "someone@upi",
    bankDetails: null,
    ...overrides,
  };
}

describe("evaluateWithdrawal", () => {
  it("allows a clean full-balance UPI withdrawal", () => {
    const result = evaluateWithdrawal(withdrawal());
    assert.deepEqual(result, { ok: true, amount: 100 });
  });

  it("rejects non-positive and non-numeric amounts", () => {
    for (const amount of [0, -50, NaN, Infinity, "100" as unknown as number, null as unknown as number]) {
      const result = evaluateWithdrawal(withdrawal({ amount }));
      assert.equal(result.ok, false, `amount ${String(amount)} should be rejected`);
    }
  });

  it("refuses to pay out when the stored canWithdraw flag is false", () => {
    const result = evaluateWithdrawal(withdrawal({ canWithdrawFlag: false }));
    assert.equal(result.ok, false);
  });

  it("enforces the Rs.100 floor even when config asks for less", () => {
    // A misconfigured or tampered remote config must not lower the payout floor.
    const result = evaluateWithdrawal(
      withdrawal({ minWithdrawal: 1, amount: 5, availableBalance: 5 })
    );
    assert.equal(result.ok, false);
    assert.match((result as { error: string }).error, /Minimum withdrawal is Rs.100/);
  });

  it("honours a higher configured minimum", () => {
    const result = evaluateWithdrawal(
      withdrawal({ minWithdrawal: 500, amount: 200, availableBalance: 200 })
    );
    assert.equal(result.ok, false);
    assert.match((result as { error: string }).error, /Rs.500/);
  });

  it("never lets a user withdraw more than their balance", () => {
    const result = evaluateWithdrawal(withdrawal({ amount: 5000, availableBalance: 100 }));
    assert.equal(result.ok, false);
    assert.match((result as { error: string }).error, /Insufficient balance/);
  });

  it("requires the wallet to be emptied in full", () => {
    const result = evaluateWithdrawal(withdrawal({ amount: 150, availableBalance: 400 }));
    assert.equal(result.ok, false);
    assert.match((result as { error: string }).error, /full available balance/);
  });

  it("tolerates sub-paisa drift below the balance", () => {
    const result = evaluateWithdrawal(withdrawal({ amount: 499.995, availableBalance: 500 }));
    assert.equal(result.ok, true);
  });

  it("never allows more than the balance, even within the drift tolerance", () => {
    // The balance check runs before the drift check on purpose: overdrawing by a
    // fraction is still overdrawing.
    const result = evaluateWithdrawal(withdrawal({ amount: 500.004, availableBalance: 500 }));
    assert.equal(result.ok, false);
    assert.match((result as { error: string }).error, /Insufficient balance/);
  });

  it("rejects drift larger than one paisa", () => {
    const result = evaluateWithdrawal(withdrawal({ amount: 499.5, availableBalance: 500 }));
    assert.equal(result.ok, false);
    assert.match((result as { error: string }).error, /full available balance/);
  });

  it("enforces the daily payout cap across earlier withdrawals", () => {
    const result = evaluateWithdrawal(
      withdrawal({ amount: 600, availableBalance: 600, alreadyWithdrawnToday: 500 })
    );
    assert.equal(result.ok, false);
    assert.match((result as { error: string }).error, /Daily limit is Rs.1000/);
  });

  it("allows a withdrawal that lands exactly on the daily cap", () => {
    const result = evaluateWithdrawal(
      withdrawal({ amount: 500, availableBalance: 500, alreadyWithdrawnToday: 500 })
    );
    assert.equal(result.ok, true);
  });

  it("requires a UPI id for UPI payouts", () => {
    for (const upiId of [null, undefined, "", "   "]) {
      const result = evaluateWithdrawal(withdrawal({ upiId }));
      assert.equal(result.ok, false, `upiId ${JSON.stringify(upiId)} should be rejected`);
    }
  });

  it("requires account number and IFSC for bank payouts", () => {
    const base = withdrawal({ paymentMethod: "BANK_TRANSFER", upiId: null });
    assert.equal(evaluateWithdrawal({ ...base, bankDetails: null }).ok, false);
    assert.equal(
      evaluateWithdrawal({ ...base, bankDetails: { accountNumber: "123456789", ifscCode: "" } }).ok,
      false
    );
    assert.equal(
      evaluateWithdrawal({ ...base, bankDetails: { accountNumber: "", ifscCode: "HDFC0001234" } }).ok,
      false
    );
    assert.equal(
      evaluateWithdrawal({
        ...base,
        bankDetails: { accountNumber: "123456789", ifscCode: "HDFC0001234" },
      }).ok,
      true
    );
  });
});

describe("canWithdraw", () => {
  it("is inclusive at the threshold", () => {
    assert.equal(canWithdraw(100, 100), true);
    assert.equal(canWithdraw(99.99, 100), false);
  });
});

describe("scoreReferralFraud", () => {
  const clean = {
    referralsInLastHour: 0,
    sameDeviceReferralsToday: 0,
    sameIpReferralsToday: 0,
    hasIpAddress: true,
    totalReferrals: 0,
    rejectedReferrals: 0,
  };

  it("passes a clean referral with no signals", () => {
    const result = scoreReferralFraud(clean);
    assert.equal(result.allowed, true);
    assert.equal(result.fraudScore, 0);
    assert.equal(result.needsReview, false);
    assert.deepEqual(result.signals, []);
  });

  it("does not flag velocity at exactly the threshold", () => {
    const result = scoreReferralFraud({ ...clean, referralsInLastHour: 10 });
    assert.deepEqual(result.signals, []);
  });

  it("flags velocity above the threshold", () => {
    const result = scoreReferralFraud({ ...clean, referralsInLastHour: 11 });
    assert.deepEqual(result.signals, ["HIGH_VELOCITY"]);
    assert.equal(result.fraudScore, 40);
    assert.equal(result.needsReview, true);
  });

  it("hard-blocks at the same-IP limit regardless of score", () => {
    const result = scoreReferralFraud({ ...clean, sameIpReferralsToday: 5 });
    assert.equal(result.allowed, false);
    assert.equal(result.rejectionReason, "SAME_IP_LIMIT");
    assert.ok(result.signals.includes("SAME_IP_LIMIT"));
  });

  it("ignores IP signals when no IP was captured", () => {
    const result = scoreReferralFraud({ ...clean, hasIpAddress: false, sameIpReferralsToday: 99 });
    assert.equal(result.allowed, true);
    assert.deepEqual(result.signals, []);
  });

  it("blocks once the combined score reaches 70", () => {
    // 40 velocity + 50 same-device = 90.
    const result = scoreReferralFraud({
      ...clean,
      referralsInLastHour: 11,
      sameDeviceReferralsToday: 3,
    });
    assert.equal(result.allowed, false);
    assert.equal(result.fraudScore, 90);
  });

  it("needs a meaningful sample before using rejection rate", () => {
    const small = scoreReferralFraud({ ...clean, totalReferrals: 10, rejectedReferrals: 10 });
    assert.deepEqual(small.signals, []);

    const large = scoreReferralFraud({ ...clean, totalReferrals: 11, rejectedReferrals: 10 });
    assert.deepEqual(large.signals, ["HIGH_REJECTION_RATE"]);
  });
});

describe("calculateTier", () => {
  it("maps counts to tiers at every boundary", () => {
    const cases: Array<[number, string]> = [
      [0, "BRONZE"],
      [4, "BRONZE"],
      [5, "SILVER"],
      [9, "SILVER"],
      [10, "GOLD"],
      [24, "GOLD"],
      [25, "PLATINUM"],
      [49, "PLATINUM"],
      [50, "DIAMOND"],
      [99, "DIAMOND"],
      [100, "ELITE"],
      [5000, "ELITE"],
    ];
    for (const [count, tier] of cases) {
      assert.equal(calculateTier(count), tier, `${count} should be ${tier}`);
    }
  });

  it("falls back to BRONZE for junk input", () => {
    assert.equal(calculateTier(NaN), "BRONZE");
    assert.equal(calculateTier(-5), "BRONZE");
  });
});

describe("milestone bonuses", () => {
  it("returns the built-in bonus for known milestones only", () => {
    assert.equal(getMilestoneBonus(5), 50);
    assert.equal(getMilestoneBonus(100), 1000);
    assert.equal(getMilestoneBonus(7), 0);
  });

  it("prefers remote config over the built-in table", () => {
    assert.equal(getConfiguredMilestoneBonus(5, { "5": 75 }), 75);
  });

  it("allows config to disable a milestone with zero", () => {
    assert.equal(getConfiguredMilestoneBonus(5, { "5": 0 }), 0);
  });

  it("falls back when config holds junk or a negative payout", () => {
    assert.equal(getConfiguredMilestoneBonus(5, { "5": -10 }), 50);
    assert.equal(getConfiguredMilestoneBonus(5, { "5": "abc" as unknown as number }), 50);
    assert.equal(getConfiguredMilestoneBonus(5, {}), 50);
  });
});

describe("welcomeBonusAmountForRole", () => {
  it("pays workers the configured bonus", () => {
    assert.equal(welcomeBonusAmountForRole(25, "WORKER"), 25);
    assert.equal(welcomeBonusAmountForRole(25, "worker"), 25);
  });

  it("never pays employers a signup bonus", () => {
    assert.equal(welcomeBonusAmountForRole(25, "EMPLOYER"), 0);
    assert.equal(welcomeBonusAmountForRole(25, "employer"), 0);
  });

  it("clamps negative configuration to zero", () => {
    assert.equal(welcomeBonusAmountForRole(-100, "WORKER"), 0);
  });

  it("treats a missing role as worker", () => {
    assert.equal(welcomeBonusAmountForRole(25, ""), 25);
  });
});

describe("timestampToMillis", () => {
  it("reads Firestore Timestamps", () => {
    assert.equal(timestampToMillis({ toMillis: () => 1_700_000_000_000 }), 1_700_000_000_000);
  });

  it("reads Dates", () => {
    const date = new Date("2026-01-01T00:00:00Z");
    assert.equal(timestampToMillis(date), date.getTime());
  });

  it("promotes second-epoch numbers to milliseconds", () => {
    assert.equal(timestampToMillis(1_700_000_000), 1_700_000_000_000);
  });

  it("passes millisecond-epoch numbers through", () => {
    assert.equal(timestampToMillis(1_700_000_000_000), 1_700_000_000_000);
  });

  it("returns 0 for empty values", () => {
    assert.equal(timestampToMillis(null), 0);
    assert.equal(timestampToMillis(undefined), 0);
    assert.equal(timestampToMillis(""), 0);
  });
});

describe("referral code handling", () => {
  it("strips punctuation and uppercases", () => {
    assert.equal(normalizeReferralCodeInput(" duty-1a2b "), "DUTY1A2B");
    assert.equal(normalizeReferralCodeInput("duty_1a.2b"), "DUTY1A2B");
  });

  it("survives empty input", () => {
    assert.equal(normalizeReferralCodeInput(""), "");
    assert.equal(normalizeReferralCodeInput(null as unknown as string), "");
  });

  it("builds a stable idempotency key per referrer/referred pair", () => {
    assert.equal(generateIdempotencyKey("a", "b"), "a_b");
    assert.notEqual(generateIdempotencyKey("a", "b"), generateIdempotencyKey("b", "a"));
  });
});
