# Playbook — WhatsApp Outreach From Contacts CSV

> Goal: tell real local workers and employers that DutyPe exists, without damaging trust, getting a number blocked, or sending spam.

## Decision

Do **not** build an auto-sender from the personal WhatsApp number `9121706236`.

Also do **not** rotate wording just to avoid WhatsApp spam detection. That is the wrong goal. The right goal is to message only people who have a reasonable reason to hear from DutyPe, keep the message clear, and give them an easy way to say no.

The safe feature to build is a **WhatsApp outreach assistant**, not a WhatsApp bot:

- imports the contacts CSV,
- cleans and deduplicates phone numbers,
- picks a small daily batch,
- generates one honest message draft per contact,
- opens WhatsApp manually for one person at a time,
- records `sent`, `replied`, `interested`, `not_interested`, and `do_not_contact`,
- blocks repeat sends to the same number.

## Best Ways

### 1. Best first step: manual founder outreach

Use `9121706236` only for manual founder-led outreach to people who are known, referred, recently contacted, or clearly relevant from a legitimate public/business source.

This is best for the first 50-100 people because replies teach what workers and employers actually understand.

Daily cap: **10-15 messages/day** from the personal number until reply quality is known.

### 2. Best scalable step: WhatsApp Business App

Create a separate WhatsApp Business number for DutyPe before scaling.

Use labels like:

- `worker_lead`
- `employer_lead`
- `sent_day_0`
- `replied`
- `do_not_contact`

Daily cap: **20/day for the first 14 days**, then increase only if reply rate and report risk are healthy.

### 3. Best compliant automation: WhatsApp Business Platform

Use the official WhatsApp Business Platform only after users have opted in or clearly requested updates. It supports approved templates, delivery tracking, and real compliance.

This is not needed for the first manual test. It is for later, after DutyPe knows which message works.

## What Not To Build

- Do not automate WhatsApp Web to send messages from a personal account.
- Do not send to the whole CSV automatically.
- Do not use wording variation as an anti-block tactic.
- Do not send to people from scraped residential groups.
- Do not message anyone who replied `stop`, `not interested`, `wrong number`, or complained.
- Do not send links first if the contact is cold. Ask permission first.

## CSV Rules

Use a working CSV like `growth/campaigns/outbound.csv` or a cleaned export from `docs/PHONE_NUMBERS.csv`.

Required columns:

| Column | Meaning |
|---|---|
| `phone` | WhatsApp number, preferably E.164 format like `+919121706236` |
| `name` | Person or business name if known |
| `role` | `worker`, `employer`, `rwa`, `ngo`, `unknown` |
| `city` | City/locality |
| `source` | Where the contact came from |
| `consent_status` | `known`, `referred`, `public_business`, `opted_in`, `unknown`, `do_not_contact` |
| `last_contacted_at` | Blank until first send |
| `status` | `not_started`, `drafted`, `sent`, `replied`, `interested`, `not_interested`, `do_not_contact` |
| `notes` | One real context detail, not fake personalization |

Never send when `consent_status` is `unknown` unless the person is being contacted manually for a legitimate one-off reason.

## First Test Batch

Start with **10 contacts**, not 30.

Pick in this order:

1. People who already know DutyPe or the founder.
2. Referred workers/employers.
3. Public business contacts where hiring/work need is obvious.
4. Everyone else later.

Send one at a time. Do not send to five people at once from the personal number.

Suggested pace:

| Time | Action |
|---|---|
| 10:00 | Send 1 message |
| 10:15 | Send next message only if no issue |
| 10:30 | Send next message |
| 10:45 | Send next message |
| 11:00 | Pause and check replies |
| Afternoon | Send remaining 5 only if the morning batch was normal |

If 2 people ignore and 1 person complains in the first 10, stop and rewrite the message.

## Daily Limits

For `9121706236` personal WhatsApp:

- Day 1-7: max **10-15 manual messages/day**.
- After that: max **20/day** if replies are healthy.
- Do not use this number for 30-40/day cold outreach.

For a warmed DutyPe WhatsApp Business number:

- First 14 days: max **20/day**.
- Later: max **30-40/day** only if reply rate is good and complaint rate is zero.
- Absolute cap: **50/day**.

## Message Copy

Use clear wording. Do not pretend the recipient asked for the message.

### Worker message

```text
Hi {name}, I’m Vamsi from DutyPe.

DutyPe is a free app for finding nearby work like helper, cook, maid, driver, electrician, plumber, and other local jobs.

No joining fee. No agent fee.

If you want, I can send the app link.
```

### Employer message

```text
Hi {name}, I’m Vamsi from DutyPe.

DutyPe helps local employers find nearby workers for urgent or regular work.

Posting is simple, and there is no hiring fee from DutyPe right now.

If useful, I can send the app link.
```

### Telugu worker message

```text
Hi {name}, నేను Vamsi, DutyPe నుంచి.

DutyPe లో nearby work కనుక్కోవచ్చు: helper, cook, maid, driver, electrician, plumber లాంటి పనులు.

Joining fee లేదు. Agent fee లేదు.

మీకు కావాలంటే app link పంపుతాను.
```

### If they say yes

```text
Thank you. Here is the DutyPe app link: {app_link}

If you need help using it, message me here.
```

### If they say stop / not interested

```text
No problem. I won’t message again. Thank you.
```

Then mark `status = do_not_contact`.

## Feature To Build

Build a small internal outreach assistant with these screens/actions:

1. **Import contacts CSV**
   - show total rows, valid phone numbers, duplicates, missing source, and do-not-contact rows.

2. **Daily queue**
   - first show 10 contacts only,
   - filter out contacted numbers,
   - filter out `do_not_contact`,
   - prefer `known`, `referred`, and `public_business`.

3. **Draft message**
   - choose worker/employer/Telugu template,
   - fill name and one real context detail,
   - never auto-send.

4. **Open WhatsApp manually**
   - create a `wa.me/{phone}?text={encodedMessage}` link,
   - user taps send manually inside WhatsApp.

5. **Log result**
   - `drafted`, `sent`, `replied`, `interested`, `not_interested`, `do_not_contact`,
   - store timestamp and notes.

6. **Safety guardrails**
   - no more than the configured daily cap,
   - no repeat sends within 7 days,
   - no sends outside 09:00-19:00 IST,
   - stop queue if complaint count > 0 for the day.

## Metrics

Review after every 50 sends:

- reply rate,
- interested rate,
- app installs from WhatsApp,
- worker profile completions,
- employer posts created,
- complaints / opt-outs.

Scale only if:

- reply rate is at least 10%,
- complaint rate is zero,
- at least one real activation comes from the batch.

## Final Recommendation

Do the first batch manually from `9121706236` with 10 known/referred/public-business contacts. Use the CSV only to prepare and track the queue. Do not build an auto-sender. If the first 50 manual messages produce real replies, move outreach to a separate WhatsApp Business number before increasing volume.
