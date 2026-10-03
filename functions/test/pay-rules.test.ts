import { describe, it } from "node:test";
import * as assert from "node:assert/strict";
import { urgentPayProblem, vacancyPayProblem } from "../src/lib/pay-rules";

describe("pay rules", () => {
  it("regular vacancies are weekly or monthly only", () => {
    assert.equal(vacancyPayProblem("MONTHLY", "FULL_TIME", 13_000), null);
    assert.equal(vacancyPayProblem("WEEKLY", "PART_TIME", 1_500), null);
    assert.match(vacancyPayProblem("DAILY", "FULL_TIME", 600)!, /weekly or monthly/);
    assert.match(vacancyPayProblem("HOURLY", "PART_TIME", 100)!, /weekly or monthly/);
    assert.match(vacancyPayProblem("MONTHLY", "DAILY", 13_000)!, /full-time or part-time/);
  });

  it("keeps pay within local limits", () => {
    assert.equal(vacancyPayProblem("MONTHLY", "PART_TIME", 3_000), null, "part-time house help");
    assert.match(vacancyPayProblem("MONTHLY", "FULL_TIME", 5_000)!, /at least ₹8,000 a month/);
    assert.match(vacancyPayProblem("MONTHLY", "FULL_TIME", 45_000)!, /at most ₹40,000 a month/);
    assert.match(vacancyPayProblem("WEEKLY", "FULL_TIME", 1_500)!, /at least ₹2,000 a week/);
    assert.match(vacancyPayProblem("WEEKLY", "PART_TIME", 12_000)!, /at most ₹10,000 a week/);
  });

  it("urgent work is short, same-day pay", () => {
    assert.equal(urgentPayProblem(700), null);
    assert.ok(urgentPayProblem(150));
    assert.ok(urgentPayProblem(15_000), "a salary belongs in a job post");
  });
});
