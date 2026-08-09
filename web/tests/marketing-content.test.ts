import { describe, expect, it } from "vitest";
import {
  findMarketingDoc,
  getAllMarketingDocs,
  resolveMarketingFile,
  toDocSlug,
} from "@/lib/marketing-content";

describe("toDocSlug", () => {
  it("strips the extension from the final segment only", () => {
    expect(toDocSlug(["growth", "assets", "README.md"])).toEqual([
      "growth",
      "assets",
      "readme",
    ]);
  });

  it("converts underscores to hyphens so old URLs keep working", () => {
    expect(toDocSlug(["growth", "assets", "pamphlet_telugu.md"])).toEqual([
      "growth",
      "assets",
      "pamphlet-telugu",
    ]);
  });

  it("slugifies intermediate directories too", () => {
    expect(
      toDocSlug(["growth", "assets", "play_store_screenshots", "README.md"])
    ).toEqual(["growth", "assets", "play-store-screenshots", "readme"]);
  });

  it("leaves .csv docs addressable", () => {
    expect(toDocSlug(["growth", "inputs", "current_metrics.csv"])).toEqual([
      "growth",
      "inputs",
      "current-metrics",
    ]);
  });
});

describe("getAllMarketingDocs", () => {
  const docs = getAllMarketingDocs();

  it("loads every document from web/content", () => {
    expect(docs.length).toBe(76);
  });

  it("produces unique slugs so no document silently shadows another", () => {
    const slugs = docs.map((d) => d.slug.join("/"));
    expect(new Set(slugs).size).toBe(slugs.length);
  });

  it("never emits an empty document", () => {
    expect(docs.filter((d) => d.content.trim() === "")).toEqual([]);
  });

  it("only exposes .md and .csv", () => {
    expect([...new Set(docs.map((d) => d.ext))].sort()).toEqual([".csv", ".md"]);
  });

  it("keeps source encoding intact (no UTF-8 mojibake)", () => {
    const corrupted = docs.filter((d) => d.content.includes("â€"));
    expect(corrupted.map((d) => d.slug.join("/"))).toEqual([]);
  });
});

describe("findMarketingDoc", () => {
  it("resolves every generated slug back to its document", () => {
    const unresolved = getAllMarketingDocs()
      .map((d) => d.slug)
      .filter((slug) => findMarketingDoc(slug) === null);
    expect(unresolved).toEqual([]);
  });

  it("resolves a known legacy URL", () => {
    const doc = findMarketingDoc(["growth", "assets", "readme"]);
    expect(doc?.title).toBe("README");
  });

  it("returns null for an unknown slug", () => {
    expect(findMarketingDoc(["growth", "assets", "does-not-exist"])).toBeNull();
  });
});

describe("resolveMarketingFile path traversal guard", () => {
  it.each([
    [["..", "..", "package.json"]],
    [["growth", "..", "..", ".env"]],
    [["growth/../../secret"]],
  ])("rejects %j", (slug) => {
    expect(resolveMarketingFile(slug as string[])).toBeNull();
  });

  it("rejects an empty slug", () => {
    expect(resolveMarketingFile([])).toBeNull();
  });
});
