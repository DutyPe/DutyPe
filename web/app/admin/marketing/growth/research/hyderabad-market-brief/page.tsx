import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Hyderabad Market Brief — DutyPe wedge

> Outside-in snapshot for the founding city. Drives pincode picks, partnership lists, and demand-side targeting. Refresh every 90 days.

---

## Why Hyderabad first

- Telugu-dominant metro → asymmetric advantage with Telugu-localised UI already shipped.
- Dense apartment-cluster geography (Cyberabad, IT Corridor) → high household-employer concentration in 5–10 pincodes.
- Active migrant labour supply from coastal AP and Telangana districts → strong cook/maid/helper supply pool.
- Cheaper field operations than Bangalore or Mumbai (founder cost-per-naka-hour is half).
- Less price-sensitive employer base than tier-2 cities (PG owners, IT-sector households, F&B SMBs).

---

## Pincode shortlist (founder picks 3 of these 5 for week-1)

| Pincode | Area | Demand drivers | Supply drivers | Why for wedge |
|---|---|---|---|---|
| 500032 | Gachibowli | IT employees, high-rise residential, large apartment complexes | Migrant women workers from coastal AP, Telangana | Apartment density = household demand spike; nakas exist near IIT/UoH |
| 500081 | Madhapur / HITEC City | IT, OYO/PG concentration, F&B SMBs | Same as above; daily-wage commute | Highest employer concentration per square km |
| 500084 | Kondapur | Mixed residential + IT support | Settled local + migrant blend | Repeat-hire cycles strongest (long-tenure households) |
| 500049 | Miyapur | Lower-mid-income residential, Metro stop | Strong daily-wage supply, low employer per capita | Test: supply-rich, demand-light pincode |
| 500072 | Kukatpally | Dense residential, schools, salons | Strong supply, multiple nakas | Salon / tiffin SMB density high |

**Recommended start: 500032 + 500081 + 500084.** Two demand-rich (Gachibowli, Madhapur) + one repeat-hire-rich (Kondapur). Avoid starting in Miyapur until supply→demand pull is proven elsewhere.

---

## Demand-side estimate (rough order of magnitude)

> These are working hypotheses, not census data. Validate with field within first 14 days.

| Segment | Estimated count in 3 wedge pincodes | Hire frequency | Annual hire events |
|---|---|---|---|
| Households needing maid/cook | ~25 000 | 1.5×/yr | ~37 500 |
| PG / co-living operators | ~400 | 6×/yr | ~2 400 |
| Cloud kitchens / tiffin centres | ~200 | 8×/yr | ~1 600 |
| Small F&B (cafes, juice shops, dhabas) | ~600 | 4×/yr | ~2 400 |
| Salons / beauty parlours | ~300 | 3×/yr | ~900 |
| **TOTAL annual hire events** | | | **~44 800** |

Even capturing 1 % of this (≈ 450 hires/yr in 3 pincodes) = 9 hires/week — on track for the 60-fills-per-week day-90 goal once efficiency compounds.

---

## Supply-side estimate

| Segment | Estimated active in 3 wedge pincodes | Switch frequency | Annual switch events |
|---|---|---|---|
| Active cooks (women, 22–45) | ~5 000 | 1.2×/yr | ~6 000 |
| Active maids | ~12 000 | 1.5×/yr | ~18 000 |
| Helpers (male, 18–40) | ~6 000 | 2×/yr | ~12 000 |
| Drivers | ~3 500 | 1×/yr | ~3 500 |
| Delivery (gig, eligible to switch) | ~5 000 | High but locked into platforms | ~unclear |

**Wedge target: cook + maid → ~17 000 active workers, ~24 000 switch events/year.** Capture rate target: 5 % activation in 90 days = 850 active workers in app.

---

## Operating notes

- **Naka points:** Madhapur Police Station chowrasta, Gachibowli Stadium signal, Kondapur signal at Botanical Gardens, Miyapur Metro stop, Kukatpally Y-junction. Best 6–9 am.
- **RWA admin sources:** MyGate society directory (free public listing), Facebook "Hyderabad Apartment Owners" groups, Apartment Adda directory. Aim for ≥ 30 admin contacts per wedge pincode.
- **PG owner sources:** Google Maps "PG near <area>" + JustDial scrape. Cap 200 prospects per pincode.
- **Tiffin / cloud kitchen sources:** Swiggy / Zomato listings filtered by area + Instagram local food pages.
- **Government:** Telangana Department of Labour has monthly labour-fairs. Worth one founder visit/quarter.
- **NGOs to approach (initial list):** SAATH, Pratham, Mahita Society, ASMITA Resource Centre for Women, MV Foundation. (Verify each before outreach.)

---

## Competitive ground reality (refresh from field, not from press releases)

- Apna posters are visible at one Madhapur naka but not consistently maintained.
- WorkIndia presence is marginal in Hyderabad nakas (more visible in Bangalore).
- Local agencies dominate cook/maid hiring for older households — this is the segment to *not* fight over yet; focus on younger / IT-sector households who already use apps.
- WhatsApp groups dominate apartment-resident hiring — this is the segment to *win* via RWA partnerships.

---

## Risks specific to the city

- **Monsoon (Jun–Sep)**: naka activity drops. Plan to lean harder on RWA + outbound during these months.
- **Sankranti (Jan)** and **Bonalu (Jul–Aug)**: workers travel to native villages for 1–3 weeks; supply tightens. Pre-festival demand spikes for cooks and maids — exploitable.
- **Rapidly changing IT layoff cycles**: household budgets compress → maid switching frequency increases. Watch quarterly.

---

## Next-city candidates (do NOT pursue until day-90 gates are met)

| City | Pros | Cons |
|---|---|---|
| Vijayawada | Telugu-first, strong NRI / household demand, lower competitive intensity | Smaller absolute market |
| Bangalore | Largest BLW market, dense apartments | Hindi+Kannada UI needed, expensive ops, brutal competition |
| Visakhapatnam | Telugu-first, untapped, port-city demand | Smaller, less alpha |
| Pune | Apartment-dense, IT corridor | Marathi/Hindi UI needed |

Recommended order if all gates met: Vijayawada → Visakhapatnam → Bangalore.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
