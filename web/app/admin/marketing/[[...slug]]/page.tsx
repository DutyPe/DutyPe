import path from "node:path";

import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { CsvTable, MarketingShell, MarkdownView, MarketingIndex } from "@/components/marketing-shell";
import {
  getMarketingTree,
  readMarketingFile,
  resolveMarketingFile
} from "@/lib/marketing-content";

export const dynamic = "force-dynamic";

export default function MarketingDocPage({
  params
}: {
  params: { slug?: string[] };
}) {
  const tree = getMarketingTree();
  const slug = params.slug ?? [];

  if (slug.length === 0) {
    return (
      <AdminAuthGate>
        <MarketingShell
          tree={tree}
          title="Marketing & Growth"
          description="Browse the consolidated marketing playbooks, growth strategy outputs, campaigns, research, and brand assets that used to live in /growth and /marketing."
        >
          <MarketingIndex tree={tree} />
        </MarketingShell>
      </AdminAuthGate>
    );
  }

  const abs = resolveMarketingFile(slug);
  if (!abs) {
    return (
      <AdminAuthGate>
        <MarketingShell tree={tree} title="Not found">
          <div className="marketing-empty">
            <h2>That document does not exist</h2>
            <p>The path <code>{slug.join("/")}</code> could not be resolved.</p>
          </div>
        </MarketingShell>
      </AdminAuthGate>
    );
  }

  const ext = path.extname(abs).toLowerCase();
  const raw = readMarketingFile(abs);
  const fileName = slug[slug.length - 1];
  const breadcrumb = slug.slice(0, -1).join(" / ");

  return (
    <AdminAuthGate>
      <MarketingShell
        tree={tree}
        title={fileName}
        description={breadcrumb || undefined}
      >
        {ext === ".csv" ? (
          <CsvTable csv={raw} />
        ) : (
          <MarkdownView source={raw} />
        )}
      </MarketingShell>
    </AdminAuthGate>
  );
}
