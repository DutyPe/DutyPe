import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Screenshots input folder

Drop the latest production screenshots here, named:

- \`worker_01_home.png\` … \`worker_08_*.png\`
- \`employer_01_home.png\` … \`employer_08_*.png\`
- \`feature_graphic.png\` (1024×500)
- \`app_icon_512.png\` (Play Store)

These are referenced by:
- \`outputs/app_store_growth_plan.md\` (screenshot sequencing strategy).
- \`outputs/landing_page_recommendations.md\` (proof block).
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
