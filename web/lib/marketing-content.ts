import fs from "node:fs";
import path from "node:path";

/**
 * Server-only helpers for the admin /marketing browser. Reads markdown/CSV
 * files that were moved from the repo-root /growth and /marketing folders
 * into web/content/marketing/. Only runs on the Node.js server (Next.js RSC).
 */

export const MARKETING_CONTENT_ROOT = path.join(
  process.cwd(),
  "content",
  "marketing"
);

export type MarketingTreeNode =
  | {
      kind: "dir";
      name: string;
      slug: string[];
      children: MarketingTreeNode[];
    }
  | {
      kind: "file";
      name: string;
      slug: string[];
      ext: ".md" | ".csv" | string;
      sizeBytes: number;
    };

const ALLOWED_EXTS = new Set([".md", ".csv"]);

function readDir(absDir: string, slugPrefix: string[]): MarketingTreeNode[] {
  if (!fs.existsSync(absDir)) {
    return [];
  }
  const entries = fs
    .readdirSync(absDir, { withFileTypes: true })
    .filter((e) => !e.name.startsWith("."))
    .sort((a, b) => {
      // Directories first, then files, both alphabetical
      if (a.isDirectory() !== b.isDirectory()) return a.isDirectory() ? -1 : 1;
      return a.name.localeCompare(b.name);
    });

  const nodes: MarketingTreeNode[] = [];
  for (const entry of entries) {
    const slug = [...slugPrefix, entry.name];
    const abs = path.join(absDir, entry.name);
    if (entry.isDirectory()) {
      const children = readDir(abs, slug);
      if (children.length === 0) continue;
      nodes.push({ kind: "dir", name: entry.name, slug, children });
    } else if (entry.isFile()) {
      const ext = path.extname(entry.name).toLowerCase();
      if (!ALLOWED_EXTS.has(ext)) continue;
      const stat = fs.statSync(abs);
      nodes.push({
        kind: "file",
        name: entry.name,
        slug,
        ext,
        sizeBytes: stat.size
      });
    }
  }
  return nodes;
}

export function getMarketingTree(): MarketingTreeNode[] {
  return readDir(MARKETING_CONTENT_ROOT, []);
}

/**
 * Resolve a slug array (from the URL) to an absolute path under the marketing
 * content root. Defends against path traversal: the resolved path must stay
 * inside MARKETING_CONTENT_ROOT.
 */
export function resolveMarketingFile(slug: string[]): string | null {
  const safeSegments = slug.filter(
    (s) => s.length > 0 && !s.includes("..") && !s.includes("/") && !s.includes("\\")
  );
  if (safeSegments.length === 0) return null;
  const abs = path.join(MARKETING_CONTENT_ROOT, ...safeSegments);
  const normalized = path.normalize(abs);
  if (
    !normalized.startsWith(MARKETING_CONTENT_ROOT + path.sep) &&
    normalized !== MARKETING_CONTENT_ROOT
  ) {
    return null;
  }
  if (!fs.existsSync(normalized) || !fs.statSync(normalized).isFile()) {
    return null;
  }
  return normalized;
}

export function readMarketingFile(absPath: string): string {
  return fs.readFileSync(absPath, "utf8");
}

/**
 * A "loaded" tree where every file node also carries its full text content.
 * Used by the /admin/marketing landing page so it can render every doc
 * inline without requiring per-file navigation.
 */
export type MarketingLoadedNode =
  | {
      kind: "dir";
      name: string;
      slug: string[];
      children: MarketingLoadedNode[];
    }
  | {
      kind: "file";
      name: string;
      slug: string[];
      ext: ".md" | ".csv" | string;
      sizeBytes: number;
      content: string;
    };

function loadDir(absDir: string, slugPrefix: string[]): MarketingLoadedNode[] {
  if (!fs.existsSync(absDir)) return [];
  const entries = fs
    .readdirSync(absDir, { withFileTypes: true })
    .filter((e) => !e.name.startsWith("."))
    .sort((a, b) => {
      if (a.isDirectory() !== b.isDirectory()) return a.isDirectory() ? -1 : 1;
      return a.name.localeCompare(b.name);
    });
  const nodes: MarketingLoadedNode[] = [];
  for (const entry of entries) {
    const slug = [...slugPrefix, entry.name];
    const abs = path.join(absDir, entry.name);
    if (entry.isDirectory()) {
      const children = loadDir(abs, slug);
      if (children.length === 0) continue;
      nodes.push({ kind: "dir", name: entry.name, slug, children });
    } else if (entry.isFile()) {
      const ext = path.extname(entry.name).toLowerCase();
      if (!ALLOWED_EXTS.has(ext)) continue;
      const stat = fs.statSync(abs);
      let content = "";
      try {
        content = fs.readFileSync(abs, "utf8");
      } catch {
        content = "";
      }
      nodes.push({
        kind: "file",
        name: entry.name,
        slug,
        ext,
        sizeBytes: stat.size,
        content,
      });
    }
  }
  return nodes;
}

export function getMarketingTreeWithContent(): MarketingLoadedNode[] {
  return loadDir(MARKETING_CONTENT_ROOT, []);
}
