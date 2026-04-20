import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Reviews input folder

Drop the following here so we can mine them for messaging:

- \`play_store_reviews.csv\` — export from Play Console (Reviews → Export). Columns: Date, Rating, Review, Reply, Country, App Version.
- \`whatsapp_support_log.md\` — paste the last 30 support conversations (anonymise phone numbers).
- \`worker_interviews.md\` — notes from any 1-on-1 with a worker user.
- \`employer_interviews.md\` — notes from any 1-on-1 with an employer user.

We use these in:
- Outbound copy (real verbatim phrases convert better than ad-agency copy).
- Landing-page proof block.
- ASO description rewrite.
- Objection-handling scripts.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
