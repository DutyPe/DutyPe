"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ReactNode, useMemo, useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

import type { MarketingTreeNode, MarketingLoadedNode } from "@/lib/marketing-content";

function fileEmoji(ext: string): string {
  if (ext === ".csv") return "📊";
  return "📄";
}

function TreeNodeView({
  node,
  defaultOpen
}: {
  node: MarketingTreeNode;
  defaultOpen: boolean;
}) {
  const pathname = usePathname();
  const [open, setOpen] = useState(defaultOpen);

  if (node.kind === "dir") {
    return (
      <li className="marketing-tree-dir">
        <button
          type="button"
          className="marketing-tree-dir-toggle"
          onClick={() => setOpen((v) => !v)}
        >
          <span className="marketing-tree-caret">{open ? "▾" : "▸"}</span>
          <span className="marketing-tree-folder">📁 {node.name}</span>
        </button>
        {open && (
          <ul className="marketing-tree-children">
            {node.children.map((child) => (
              <TreeNodeView
                key={child.slug.join("/")}
                node={child}
                defaultOpen={false}
              />
            ))}
          </ul>
        )}
      </li>
    );
  }

  const href = `/admin/marketing/${node.slug.map(encodeURIComponent).join("/")}`;
  const isActive = pathname === href;

  return (
    <li>
      <Link
        href={href}
        className={`marketing-tree-file ${isActive ? "active" : ""}`}
      >
        <span>{fileEmoji(node.ext)}</span>
        <span>{node.name}</span>
      </Link>
    </li>
  );
}

export function MarketingShell({
  tree,
  title,
  description,
  children
}: {
  tree: MarketingTreeNode[];
  title: string;
  description?: string;
  children: ReactNode;
}) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const flatCount = useMemo(() => {
    let n = 0;
    const walk = (nodes: MarketingTreeNode[]) => {
      for (const node of nodes) {
        if (node.kind === "file") n++;
        else walk(node.children);
      }
    };
    walk(tree);
    return n;
  }, [tree]);

  return (
    <div className="marketing-layout">
      {sidebarOpen && (
        <div
          className="marketing-overlay"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      <aside className={`marketing-sidebar ${sidebarOpen ? "open" : ""}`}>
        <div className="marketing-sidebar-header">
          <Link href="/admin/marketing" className="marketing-brand">
            <span className="marketing-brand-mark">MK</span>
            <div>
              <strong>Marketing &amp; Growth</strong>
              <small>{flatCount} documents</small>
            </div>
          </Link>
          <button
            type="button"
            className="marketing-sidebar-close"
            onClick={() => setSidebarOpen(false)}
            aria-label="Close sidebar"
          >
            ✕
          </button>
        </div>

        <nav className="marketing-sidebar-nav">
          <ul className="marketing-tree-root">
            {tree.map((node) => (
              <TreeNodeView
                key={node.slug.join("/")}
                node={node}
                defaultOpen
              />
            ))}
          </ul>
        </nav>

        <div className="marketing-sidebar-footer">
          <Link href="/admin" className="marketing-sidebar-back">
            ← Back to Admin
          </Link>
        </div>
      </aside>

      <main className="marketing-content">
        <header className="marketing-topbar">
          <button
            type="button"
            className="marketing-menu-btn"
            onClick={() => setSidebarOpen(true)}
            aria-label="Open menu"
          >
            <span /><span /><span />
          </button>
          <div>
            <h1>{title}</h1>
            {description && <p>{description}</p>}
          </div>
        </header>

        <div className="marketing-body">{children}</div>
      </main>
    </div>
  );
}

export function CsvTable({ csv }: { csv: string }) {
  // Tiny CSV parser: handles quoted fields with embedded commas/newlines.
  const rows: string[][] = [];
  let cur: string[] = [];
  let field = "";
  let inQuotes = false;
  for (let i = 0; i < csv.length; i++) {
    const ch = csv[i];
    if (inQuotes) {
      if (ch === '"') {
        if (csv[i + 1] === '"') {
          field += '"';
          i++;
        } else {
          inQuotes = false;
        }
      } else {
        field += ch;
      }
    } else if (ch === '"') {
      inQuotes = true;
    } else if (ch === ",") {
      cur.push(field);
      field = "";
    } else if (ch === "\n" || ch === "\r") {
      if (ch === "\r" && csv[i + 1] === "\n") i++;
      cur.push(field);
      rows.push(cur);
      cur = [];
      field = "";
    } else {
      field += ch;
    }
  }
  if (field.length > 0 || cur.length > 0) {
    cur.push(field);
    rows.push(cur);
  }

  if (rows.length === 0) return <p>(empty)</p>;
  const [header, ...body] = rows;

  return (
    <div className="marketing-csv-wrap">
      <table className="marketing-csv">
        <thead>
          <tr>
            {header.map((h, i) => (
              <th key={i}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {body
            .filter((r) => r.some((c) => c.trim().length > 0))
            .map((row, ri) => (
              <tr key={ri}>
                {row.map((cell, ci) => (
                  <td key={ci}>{cell}</td>
                ))}
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  );
}

/**
 * Renders a markdown document with GitHub-flavored markdown (tables, task
 * lists, strikethrough, autolinks). Used by /admin/marketing/[[...slug]] to
 * display .md files from the moved /growth and /marketing folders.
 */
export function MarkdownView({ source }: { source: string }) {
  return (
    <article className="marketing-doc-md">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{source}</ReactMarkdown>
    </article>
  );
}

/**
 * Landing index for /admin/marketing — surfaces every section and its docs
 * as clickable cards so users can see at a glance what playbooks, strategies,
 * campaigns, and assets live under /growth and /marketing.
 */
export function MarketingIndex({ tree }: { tree: MarketingTreeNode[] }) {
  const fileHref = (node: MarketingTreeNode) =>
    `/admin/marketing/${node.slug.map(encodeURIComponent).join("/")}`;

  const sectionFor = (
    rootName: string,
    rootDir: MarketingTreeNode | undefined
  ) => {
    if (!rootDir || rootDir.kind !== "dir") {
      return (
        <section className="marketing-index-section" key={rootName}>
          <h2>{rootName}</h2>
          <p className="marketing-index-empty">
            (no content found for {rootName})
          </p>
        </section>
      );
    }
    const subdirs = rootDir.children.filter(
      (c): c is Extract<MarketingTreeNode, { kind: "dir" }> => c.kind === "dir"
    );
    const looseFiles = rootDir.children.filter(
      (c): c is Extract<MarketingTreeNode, { kind: "file" }> => c.kind === "file"
    );

    return (
      <section className="marketing-index-section" key={rootName}>
        <h2>📁 {rootDir.name}</h2>
        <div className="marketing-index-groups">
          {subdirs.map((sub) => {
            const fileChildren = sub.children.filter(
              (c): c is Extract<MarketingTreeNode, { kind: "file" }> =>
                c.kind === "file"
            );
            return (
              <div className="marketing-index-card" key={sub.slug.join("/")}>
                <h3>📂 {sub.name}</h3>
                <small>{fileChildren.length} document{fileChildren.length === 1 ? "" : "s"}</small>
                <ul>
                  {fileChildren.map((file) => (
                    <li key={file.slug.join("/")}>
                      <Link href={fileHref(file)}>
                        <span>{fileEmoji(file.ext)}</span>
                        <span>{file.name}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            );
          })}
          {looseFiles.length > 0 && (
            <div className="marketing-index-card">
              <h3>📄 Top-level docs</h3>
              <ul>
                {looseFiles.map((file) => (
                  <li key={file.slug.join("/")}>
                    <Link href={fileHref(file)}>
                      <span>{fileEmoji(file.ext)}</span>
                      <span>{file.name}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </section>
    );
  };

  const growthDir = tree.find((n) => n.kind === "dir" && n.name === "growth");
  const marketingDir = tree.find((n) => n.kind === "dir" && n.name === "marketing");
  const others = tree.filter(
    (n) => !(n.kind === "dir" && (n.name === "growth" || n.name === "marketing"))
  );

  return (
    <div className="marketing-index">
      <p className="marketing-index-intro">
        All campaigns, assets, inputs, outputs, playbooks, channels, brand,
        go-to-market and research documents. Pick any document below or use the
        sidebar tree.
      </p>
      {sectionFor("growth", growthDir)}
      {sectionFor("marketing", marketingDir)}
      {others.map((node) =>
        node.kind === "dir" ? sectionFor(node.name, node) : null
      )}
    </div>
  );
}


// ============================================================
// MarketingFullView - renders EVERY file inline on landing page
// ============================================================
function slugifyAnchor(parts: string[]): string {
  return parts.join("-").toLowerCase().replace(/[^a-z0-9-]+/g, "-");
}

function sectionIcon(name: string): string {
  const map: Record<string, string> = {
    assets: "\ud83c\udfa8",
    campaigns: "\ud83d\udce3",
    inputs: "\ud83d\udce5",
    outputs: "\ud83d\udce4",
    playbooks: "\ud83d\udcd8",
    research: "\ud83d\udd2c",
    brand: "\ud83e\udea3",
    channels: "\ud83d\udce1",
    "go-to-market": "\ud83d\ude80",
    growth: "\ud83c\udf31",
    marketing: "\ud83d\udce2",
  };
  return map[name.toLowerCase()] ?? "\ud83d\udcc1";
}

function FileBlock({ file }: { file: Extract<MarketingLoadedNode, { kind: "file" }> }) {
  const id = slugifyAnchor(file.slug);
  const title = file.name.replace(/\.(md|csv)$/i, "");
  return (
    <details id={id} className="marketing-full-file" open>
      <summary>
        <span className="marketing-full-file-icon">{fileEmoji(file.ext)}</span>
        <span className="marketing-full-file-title">{title}</span>
      </summary>
      <div className="marketing-full-file-body">
        {file.ext === ".csv" ? (
          <CsvTable csv={file.content} />
        ) : (
          <MarkdownView source={file.content} />
        )}
      </div>
    </details>
  );
}

function DirSection({
  dir,
  depth,
}: {
  dir: Extract<MarketingLoadedNode, { kind: "dir" }>;
  depth: number;
}) {
  const subdirs = dir.children.filter(
    (c): c is Extract<MarketingLoadedNode, { kind: "dir" }> => c.kind === "dir"
  );
  const files = dir.children.filter(
    (c): c is Extract<MarketingLoadedNode, { kind: "file" }> => c.kind === "file"
  );
  const id = slugifyAnchor(dir.slug);
  const Heading = (depth === 0 ? "h2" : depth === 1 ? "h3" : "h4") as
    | "h2"
    | "h3"
    | "h4";
  return (
    <section
      id={id}
      className={`marketing-full-section marketing-full-depth-${depth}`}
    >
      <Heading className="marketing-full-heading">
        <span>{sectionIcon(dir.name)}</span>
        <span>{dir.name}</span>
        <small>
          {files.length > 0 && `${files.length} doc${files.length === 1 ? "" : "s"}`}
          {files.length > 0 && subdirs.length > 0 && " \u00b7 "}
          {subdirs.length > 0 && `${subdirs.length} subfolder${subdirs.length === 1 ? "" : "s"}`}
        </small>
      </Heading>
      {files.length > 0 && (
        <div className="marketing-full-files">
          {files.map((f) => (
            <FileBlock key={f.slug.join("/")} file={f} />
          ))}
        </div>
      )}
      {subdirs.map((sub) => (
        <DirSection key={sub.slug.join("/")} dir={sub} depth={depth + 1} />
      ))}
    </section>
  );
}

function buildToc(nodes: MarketingLoadedNode[]): ReactNode {
  return (
    <ul>
      {nodes.map((n) => {
        if (n.kind !== "dir") return null;
        return (
          <li key={n.slug.join("/")}>
            <a href={`#${slugifyAnchor(n.slug)}`}>
              {sectionIcon(n.name)} {n.name}
            </a>
            {n.children.some((c) => c.kind === "dir") && buildToc(n.children)}
          </li>
        );
      })}
    </ul>
  );
}

/**
 * MarketingFullView - landing page that renders EVERY markdown/CSV doc
 * inline so the user can scroll through assets, campaigns, inputs, outputs,
 * playbooks, research, brand, channels, go-to-market, etc., all visually
 * on a single screen.
 */
export function MarketingFullView({ tree }: { tree: MarketingLoadedNode[] }) {
  const totals = useMemo(() => {
    let files = 0;
    let dirs = 0;
    const walk = (ns: MarketingLoadedNode[]) => {
      for (const n of ns) {
        if (n.kind === "file") files++;
        else {
          dirs++;
          walk(n.children);
        }
      }
    };
    walk(tree);
    return { files, dirs };
  }, [tree]);

  return (
    <div className="marketing-full">
      <aside className="marketing-full-toc">
        <h3>On this page</h3>
        <small>
          {totals.files} documents � {totals.dirs} folders
        </small>
        {buildToc(tree)}
      </aside>
      <div className="marketing-full-stream">
        {tree.map((node) =>
          node.kind === "dir" ? (
            <DirSection key={node.slug.join("/")} dir={node} depth={0} />
          ) : (
            <FileBlock key={node.slug.join("/")} file={node} />
          )
        )}
      </div>
    </div>
  );
}
