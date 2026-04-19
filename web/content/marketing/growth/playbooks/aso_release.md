# Playbook — Play Store Listing Release

> Goal: ship an ASO update with zero conversion regression.

## Pre-flight checklist

- [ ] Title within 30 chars.
- [ ] Short description within 80 chars.
- [ ] Long description ≤ 4 000 chars.
- [ ] All 8 screenshots exported at correct resolution.
- [ ] Telugu listing complete (do not publish English-only update if Telugu localised listing exists — Play will warn).
- [ ] Hindi listing reviewed (text only; no UI).
- [ ] Feature graphic 1024×500 ready.
- [ ] Privacy policy URL alive (https://dutype.in/privacy).
- [ ] Data safety form re-confirmed.
- [ ] App version number bumped if any APK / AAB change is co-released.

## Pre-publish A/B test setup

- If only listing copy changing → use Play Console **Store Listing Experiments** (no APK release needed).
- If feature graphic / icon → run as separate experiment.
- One concurrent experiment max.

## Publish

- Publish during India morning hours (9–11 am) — fastest review turnaround.
- Wait 6–24 hours for Play review approval. **Do not refresh other listings during review.**

## Post-publish (first 7 days)

- Day 1: snapshot conversion baseline (visitor→install rate).
- Day 3: check Crashlytics for spike (sometimes a listing-only update bumps installs and exposes a latent crash).
- Day 7: read experiment significance. Decide rollout or revert.

## Rollout

- If experiment winner is significant (≥ 95 % confidence and ≥ 5 % uplift) → roll to 100 %.
- If inconclusive → keep collecting up to 28 days, then decide.
- If losing → revert.

## Rollback procedure

- Play Console → Store Listing Experiments → Revert.
- Update reverts within 30–60 min.
- Document outcome in `growth/campaigns/experiments.csv`.

## Cadence

- Maximum 1 listing change per 14 days (Play penalises high-frequency changes).
- Plan ASO sprints quarterly.
- Localised listings reviewed monthly for stale numbers (e.g., "active in 5 cities" should match reality).
