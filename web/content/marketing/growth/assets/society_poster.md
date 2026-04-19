# Society Notice-Board Poster (A4)

> Single page. Designed for the society notice board / lift mirror. Print spec: A4, 200 gsm, full colour, laminated. ~₹50/piece.

## Layout

```
[Top 1/4: bold headline in Telugu + English]
మీ society లో maid / cook కావాలా?
Need a maid or cook in your society?

[Middle 1/2: phone mockup with 3 sample job cards visible]
[Right side overlay text:]
✓ Phone OTP verified
✓ 5 km radius
✓ ₹0 commission
✓ Telugu support

[Below mockup:]
QR (large, 5×5 cm) — links to Play Store with
utm_source=society_poster&utm_campaign=<society_name>

[Bottom strip:]
Society partner: <Society Name>
WhatsApp help: +91 91217 06236
DutyPe · dutype.in · KGPV INNOVATION SOLUTIONS PRIVATE LIMITED
```

## Production rules

- Society name printed per-society (variable data field). Designer creates one master + per-society overlay.
- QR must be unique per society (utm_campaign). Cloud Function tracks attribution.
- Lamination is mandatory (poster lives on board for 30+ days, exposed to humidity).
- Replacement schedule: every 60 days OR when worker / employer count changes ≥ 25 % (whichever sooner).

## Distribution

- Founder hand-delivers first 30 posters personally (ride-along during RWA partnership pitch).
- Each delivery logged in `growth/campaigns/partnerships.csv` with `units_or_members` filled.
- Photo of mounted poster requested from RWA admin within 7 days.
