import path from "node:path";

import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { CsvTable, MarketingShell, MarkdownView, MarketingFullView } from "@/components/marketing-shell";
import {
  getMarketingTree,
  getMarketingTreeWithContent,
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
    const loaded = getMarketingTreeWithContent();
    return (
      <AdminAuthGate>
        <MarketingShell
          tree={tree}
          title="Marketing & Growth"
          description="Every campaign, asset, input, output, playbook, channel, brand, go-to-market and research document — all rendered inline. Use the table of contents on the right to jump around."
        >
          <MarketingFullView tree={loaded} />
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
