import { notFound } from "next/navigation";
import { CsvTable, MarkdownView } from "@/components/marketing-shell";
import { findMarketingDoc, getAllMarketingDocs } from "@/lib/marketing-content";

export const dynamicParams = false;

export function generateStaticParams() {
  return getAllMarketingDocs().map((doc) => ({ slug: doc.slug }));
}

export function generateMetadata({ params }: { params: { slug: string[] } }) {
  const doc = findMarketingDoc(params.slug);
  return { title: doc ? `${doc.title} · Marketing` : "Marketing" };
}

export default function Page({ params }: { params: { slug: string[] } }) {
  const doc = findMarketingDoc(params.slug);
  if (!doc) notFound();

  return (
    <div className="marketing-page-single">
      {doc.ext === ".csv" ? (
        <CsvTable csv={doc.content} />
      ) : (
        <MarkdownView source={doc.content} />
      )}
    </div>
  );
}
