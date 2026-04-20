import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Play Store Screenshots — Export Specs

Drop the final exported PNG files here, sized **1080 × 1920** (portrait, 9:16).

## Naming convention

\`\`\`
en_01_worker_hero.png
en_02_worker_local.png
en_03_worker_apply.png
en_04_worker_refer.png
en_05_employer_post.png
en_06_employer_applications.png
en_07_employer_qr.png
en_08_employer_support.png

te_01_worker_hero.png
te_02_worker_local.png
… (same sequence in Telugu)
\`\`\`

## Master Figma source

Keep the editable source in \`growth/assets/play_store_screenshots/source/\` (Figma file URL noted in the README of that subfolder once created).

## Headlines per slot

See [growth/outputs/app_store_growth_plan.md](../../outputs/app_store_growth_plan.md) section "Screenshots (8 slots — sequence is the message)" for the exact overlay copy.

## Compression

- PNG, ≤ 8 MB each (Play Store limit).
- Use \`pngquant --quality=70-85 --strip\` to compress before upload.

## Localisation rule

- Telugu listing must use Telugu-overlay screenshots.
- Hindi listing should use English-overlay screenshots (since the app UI itself is not Hindi yet) plus a Hindi caption inside the listing description.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
