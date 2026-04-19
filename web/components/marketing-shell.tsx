"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ReactNode, useMemo, useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

import type { MarketingTreeNode } from "@/lib/marketing-content";

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
