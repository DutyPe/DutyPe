import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# DutyPe – Founder Notes (input file)

> A free-form journal. Things only the founder knows. Update weekly.

## Why this product exists
- ____

## What I've personally seen workers say (verbatim)
- ____

## What I've personally seen employers say (verbatim)
- ____

## What's working (3 things)
- ____
- ____
- ____

## What's broken (3 things)
- ____
- ____
- ____

## What I'm afraid to admit publicly
- ____

## What I refuse to do
- e.g. "I will not pay influencers."
- e.g. "I will not become a staffing agency."
- ____

## Constraints
- Team size: ____
- Cash in bank: ____
- Months of runway at current burn: ____
- Hours per week I personally have for growth: ____
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
