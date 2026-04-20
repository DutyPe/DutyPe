import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Interviews

Drop one markdown file per interview here, organized by segment:

\`\`\`
interviews/
  workers/         ← cooks, maids, helpers, drivers
  households/      ← apartment-residing employers
  smb/             ← PG / cloud kitchen / salon / tiffin owners
  rwa/             ← RWA admins
  ngo/             ← NGO/SHG partners
  internal/        ← founder retros, team learnings
\`\`\`

Filename: \`YYYY-MM-DD_firstname-pincode.md\` (e.g. \`2026-04-22_lakshmi-500081.md\`).

Use [interview_notes_template.md](../interview_notes_template.md).

**Quality bar:** 1 interview / week minimum from the founder during the 90-day plan. No exceptions.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
