/**
 * Search words for a job: lower-case words (any script — English, Telugu, Hindi) of its title,
 * company, area, district and category. The app searches with `keywords array-contains <word>`,
 * using the same splitting (JobFeedPager.searchWord), so both sides must stay in step.
 */
const SPLIT = /[^\p{L}\p{M}\p{N}]+/u;
const MAX_KEYWORDS = 40;

export function keywordsOf(...parts: unknown[]): string[] {
  const out = new Set<string>();
  for (const part of parts) {
    for (const word of String(part ?? "").normalize("NFC").toLowerCase().split(SPLIT)) {
      if (word.length >= 2) out.add(word);
      if (out.size >= MAX_KEYWORDS) return Array.from(out);
    }
  }
  return Array.from(out);
}
