import { describe, it } from "node:test";
import * as assert from "node:assert/strict";

import { keywordsOf } from "../src/lib/keywords";

describe("keywordsOf", () => {
  it("splits title, company, area, district and category into lower-case words", () => {
    assert.deepEqual(
      keywordsOf("Night Security Guard", "Sai Comforts", "Wyra Road, Khammam", "Khammam", "OFFICE STAFF"),
      ["night", "security", "guard", "sai", "comforts", "wyra", "road", "khammam", "office", "staff"],
    );
  });
  it("keeps Telugu and Hindi words whole", () => {
    assert.deepEqual(keywordsOf("వంట మనిషి కావాలి", "रसोइया चाहिए"), ["వంట", "మనిషి", "కావాలి", "रसोइया", "चाहिए"]);
  });
  it("drops one-letter words and caps the list", () => {
    assert.deepEqual(keywordsOf("A b cd"), ["cd"]);
    assert.equal(keywordsOf(Array.from({ length: 60 }, (_, i) => `word${i}`).join(" ")).length, 40);
  });
});
