import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Voice & Tone

> One voice across founder, ops, push, WhatsApp, Play Store, and PR. If a piece of copy can't pass these rules, it doesn't ship.

## Voice (always)

- **Plain.** No HR jargon, no startup-isms, no "synergy / leverage / scale."
- **Direct.** Get to the point in the first sentence.
- **Humble.** We are 1 200 users in 3 pincodes, not "India's leading platform."
- **Bilingual without code-switching gymnastics.** Telugu copy is full Telugu. English copy is full English. Code-switching only in WhatsApp where natural ("apply cheyandi → tap here").
- **Numbers over adjectives.** "47 cooks active in your pincode" beats "lots of nearby workers."

## Tone by audience

| Audience | Tone |
|---|---|
| Worker (in app + push) | Warm, respectful, action-oriented. Telugu by default. Address as \`మీరు\`. |
| Worker (WhatsApp from ops) | Like a friendly older sibling explaining a thing. Voice notes welcome. |
| Employer SMB | Confident, time-saving, ROI-led. English by default. |
| Employer household | Polite, trust-led. Telugu for Hyd locals, English fallback. |
| RWA admin | Peer-to-peer, never salesy. "I'm not selling anything" early. |
| NGO partner | Mission-aligned, women-first language, never patronising. |
| Investor / partner | Numbers-led, brutally honest, no hype. |
| Journalist | Story-led, on-record, never embargoed. |
| 1-star reviewer | Calm, factual, fix-the-thing, don't argue. |

## Words we use

- "Worker" (not "labourer," not "domestic help" — too patronising).
- "Employer" (not "boss," not "client").
- "Hire" (not "recruit").
- "Verified" (only when we mean phone-OTP — never imply BGV).
- "Nearby" (5 km radius, not "city-wide").
- "Free" (only where it's actually free, today).

## Words we don't use

- "Lakhs of users" (until it's true).
- "AI-powered" (we are not).
- "Disruptive" (overused, meaningless).
- "Background-verified" (we are not — we are phone-OTP-verified).
- "Salary guarantee" (we don't guarantee).
- "Highest paid" (unfalsifiable).
- "Game-changing," "revolutionary," "transformative" — banned.

## Sentence length

- Headlines ≤ 6 words.
- WhatsApp opening ≤ 6 sentences.
- Push body ≤ 110 chars.
- Press one-pager: every section ≤ 60 words.

## Calls-to-action — preferred verbs

- "Install free"
- "Post a job"
- "Apply now"
- "Open the app"
- "Get the WhatsApp link"

Never use:
- "Learn more"
- "Sign up today!" (with the exclamation)
- "Click here"
- "Don't miss out"
- "Limited time" (unless literally true)

## Emoji policy

- Maximum **one** per message / push.
- Allowed: 🙏 (thank you / namaste), ✓ (proof point), ✅ (verified), 🎉 (celebration on real wins), 📍 (location).
- Banned: 🔥 💪 🚀 ✨ 💯 (founder bro speak).

## Founder LinkedIn / X voice

- First-person.
- One observation, one number, one decision per post.
- No hashtags except in rare special cases (#opentowork, #hiring as data).
- Never repost startup-influencer content.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
