# DutyPe Service 3D Isometric Clay Icon Prompts

This guide contains production-ready 3D isometric clay render image generation prompts for all **43+ home services** and **9 service categories** in DutyPe.

Generated in the signature **Urban Company / Pronto / Zepto** aesthetic:
- **Style**: Cute 3D isometric clay render, pastel matte finish, smooth tactile clay texture, soft ambient studio lighting, clean rim light, high detail.
- **Output Format**: Centered, isolated subject on transparent background (`--no frame, background, shadow cutoff`).
- **Engines**: Optimized for **ChatGPT (GPT-4o / DALL-E 3)** and **Midjourney v6**.

---

## 🤖 ChatGPT (DALL-E 3) Generation & Format Guide

### 1. Does ChatGPT generate WebP or PNG?
- **ChatGPT outputs PNG by default**: DALL-E 3 natively creates high-resolution **1024×1024 PNG** files (~800 KB to 1.2 MB).
- **The DutyPe Mobile App needs WebP**: For ultra-fast app loading (< 15ms) and 0 KB APK bloat, the app displays **512×512 WebP** files (~20 to 35 KB).

### 2. How to get your WebP files easily (Two Easy Ways):

#### ✨ Method A (Zero Extra Work — Recommended):
1. In ChatGPT, paste the prompt below. Download the image as a standard **PNG**.
2. Open the new **DutyPe Admin Panel** (`http://localhost:3000/admin/service-catalog` or on production web).
3. Drag & drop the ChatGPT PNG directly into the service box.
4. **The Admin Panel automatically converts the PNG to 512×512 WebP (~28 KB)** right inside your browser, displays the exact KB savings, and uploads it to Firebase Storage with one click!

#### 🐍 Method B (Ask ChatGPT to export WebP directly):
After ChatGPT generates the image in the chat, simply send this follow-up message:
```text
"Great! Now write a quick Python script to resize this image to 512x512 px with transparent background, compress it into a WebP file under 35 KB, and provide a download link."
```
ChatGPT's Python Code Interpreter will execute the script and give you a `.webp` download button immediately!

---

## 🎨 Master Template for ChatGPT

```text
Cute 3D isometric clay render of a [SUBJECT / PROPS / DETAILS], pastel matte finish, soft studio lighting, smooth clay texture, centered on an isolated pure transparent background, high detail, no frame, no ground shadow cutoff
```
*(If ChatGPT creates a solid white background, use the prompt: `"isolated subject floating on pure white background, easy to crop"` or use [remove.bg](https://remove.bg) / the DutyPe Admin auto-cleaner).*

---

## 📂 Category 1: Home Cleaning (`CLEANING`)

| Service ID | Service Name | Prompt |
| :--- | :--- | :--- |
| `clean_sweep` | House Sweeping & Mopping | `Cute 3D isometric clay render of a modern broom, a pastel bucket filled with foamy bubbles, and a mop standing upright with water drops, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `clean_utensils` | Utensils Washing | `Cute 3D isometric clay render of a small neat stack of clean ceramic plates, a shiny pan, bubbles, and a cute pastel sponge with dish soap bottle, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `clean_bathroom` | Bathroom Deep Cleaning | `Cute 3D isometric clay render of a sparkling white bathroom tile, toilet brush, spray bottle with cleaner fluid, bubbles, and a small rubber squeegee, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `clean_kitchen` | Kitchen Deep Cleaning | `Cute 3D isometric clay render of a miniature stainless steel kitchen chimney hood and gas stove top with sparkling tiles and yellow spray bottle, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `clean_1bhk` | Full Home Deep Cleaning (1BHK) | `Cute 3D isometric clay render of a miniature cozy single-bedroom apartment cutaway with cute vacuum cleaner, broom, and laundry basket, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `clean_2bhk` | Full Home Deep Cleaning (2BHK) | `Cute 3D isometric clay render of a miniature two-bedroom house layout with sparkling living room, vacuum cleaner, bucket, and cleaning cart, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `clean_sofa` | Sofa Cleaning (5 seats) | `Cute 3D isometric clay render of a pastel modern sofa armchair with a miniature upholstery vacuum cleaner wand and gentle steam bubbles, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `clean_fans` | Fans, Cobwebs & Windows | `Cute 3D isometric clay render of a ceiling fan with swirling wind lines, a duster on extendable pole, and a sparkling window grill, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `clean_balcony` | Balcony Deep Cleaning | `Cute 3D isometric clay render of a cozy apartment balcony with metal railing, floor scrub brush, water splash, and potted green plant, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `clean_fridge` | Fridge Deep Cleaning | `Cute 3D isometric clay render of an open pastel refrigerator with clean glass shelves, fresh lemons, sparkling clean door racks, and spray wipe, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `clean_windows` | Window & Mesh Cleaning | `Cute 3D isometric clay render of a clean sliding window frame with shiny glass reflection, spray bottle, and yellow wipe cloth, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `clean_kitchen_prep` | Kitchen Prep & Chopping | `Cute 3D isometric clay render of a wooden kitchen cutting board with neatly chopped carrots, bell peppers, fresh vegetables, and a chef knife, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `clean_wardrobe` | Wardrobe Organization | `Cute 3D isometric clay render of an open wooden wardrobe with neatly folded colorful pastel shirts, hangers, and small storage drawer bins, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |

---

## 📂 Category 2: AC Service & Repair (`AC`)

| Service ID | Service Name | Prompt |
| :--- | :--- | :--- |
| `ac_service` | AC Service (Foam-Jet) | `Cute 3D isometric clay render of a wall split air conditioner unit with ice-blue cooling breeze waves and high pressure foam cleaning jet gun, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `ac_deep` | AC Deep Cleaning (Indoor + Outdoor) | `Cute 3D isometric clay render of split AC indoor blower unit paired with outdoor compressor fan unit surrounded by fresh water splash and clean foam bubbles, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `ac_repair_visit` | AC Not Cooling / Repair Visit | `Cute 3D isometric clay render of an AC unit with a miniature wrench and multimeter tool, showing a subtle warning exclamation badge, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `ac_gas` | AC Gas Refill | `Cute 3D isometric clay render of a small pastel blue refrigerant gas cylinder tank connected with brass manifold gauge and rubber pipes to an AC valve, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `ac_install` | Split AC Installation | `Cute 3D isometric clay render of an AC indoor unit on a wall mount bracket, cordless power drill, screws, and copper tubing coil, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `ac_uninstall` | AC Uninstallation | `Cute 3D isometric clay render of an AC indoor unit carefully demounted with coiled copper pipes and brass tool accessories, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |

---

## 📂 Category 3: Electrician (`ELECTRICIAN`)

| Service ID | Service Name | Prompt |
| :--- | :--- | :--- |
| `elec_fan` | Fan Installation / Repair | `Cute 3D isometric clay render of a white 3-blade ceiling fan spinning with yellow screwdriver and wire nuts, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `elec_switch` | Switch / Socket Repair | `Cute 3D isometric clay render of a modern white modular electrical wall switchboard with rocker switches, indicator light, and yellow voltage line tester, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `elec_light` | Light / Tube Fitting | `Cute 3D isometric clay render of a glowing warm LED tube batten and a cute vintage tungsten bulb with soft electrical sparks, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `elec_decor` | Decorative Light / Chandelier | `Cute 3D isometric clay render of a luxury modern geometric hanging chandelier light with warm glowing bulbs, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `elec_point` | New Socket / Switch Point | `Cute 3D isometric clay render of an electrical wall junction box with colorful PVC insulated copper wires and three-pin plug socket, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `elec_mcb` | MCB / Fuse / Wiring Fault | `Cute 3D isometric clay render of an electrical distribution board with miniature circuit breaker (MCB) switches and safety lightning bolt icon, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `elec_inverter` | Inverter / Stabiliser Installation | `Cute 3D isometric clay render of a home power inverter unit with a heavy battery and digital LED voltage display screen, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `elec_tv` | TV Wall Mounting | `Cute 3D isometric clay render of a slim smart flat screen TV mounted on a metal wall swivel bracket with a mini spirit level tool on top, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `elec_visit` | Electrician Visit | `Cute 3D isometric clay render of a compact electrician tool pouch with digital multimeter, pliers, insulated screwdrivers, and black insulation tape roll, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |

---

## 📂 Category 4: Plumber (`PLUMBER`)

| Service ID | Service Name | Prompt |
| :--- | :--- | :--- |
| `plumb_tap` | Tap / Mixer Fitting | `Cute 3D isometric clay render of a sleek chrome bathroom water tap faucet with crystal clear water droplet, pipe wrench, and teflon tape, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `plumb_leak` | Leakage Repair | `Cute 3D isometric clay render of a PVC water pipe elbow joint with tiny water spray droplet and blue plumbing wrench, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `plumb_block` | Drain / Toilet Blockage | `Cute 3D isometric clay render of a classic red rubber sink plunger standing next to a chrome drain pipe with whirlpool water flow, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `plumb_flush` | Flush Tank Repair | `Cute 3D isometric clay render of an open ceramic toilet flush cistern tank with blue float ball valve and push flush button, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `plumb_basin` | Wash Basin / Sink Installation | `Cute 3D isometric clay render of a modern white ceramic wash basin with sleek faucet and chrome drain bottle trap underneath, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `plumb_tank` | Water Tank Cleaning (1000L) | `Cute 3D isometric clay render of a rooftop black overhead Sintex water tank with ladder, clean swirling blue water, and pressure washer hose, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `plumb_motor` | Water Motor Repair / Fitting | `Cute 3D isometric clay render of a compact electric water pump motor with pipe connections, pressure gauge, and power cord, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |

---

## 📂 Category 5: Appliance & RO Repair (`APPLIANCE`)

| Service ID | Service Name | Prompt |
| :--- | :--- | :--- |
| `ro_service` | RO Water Purifier Service | `Cute 3D isometric clay render of a modern kitchen wall RO water purifier with transparent water tank, sediment filter cylinder, and glass of water, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `ro_install` | RO Installation / Demounting | `Cute 3D isometric clay render of an RO purifier unit next to a cordless power drill, wall anchors, and thin white water tubing, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `wm_repair` | Washing Machine Repair | `Cute 3D isometric clay render of a front-load washing machine with glass porthole door showing spinning water and small repair wrench icon, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `fridge_repair` | Refrigerator Repair | `Cute 3D isometric clay render of a double-door refrigerator with snowflake cooling badge, temperature dial, and repair toolbox, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `tv_repair` | TV Repair Visit | `Cute 3D isometric clay render of a flat screen TV showing colorful test color bars with a magnifying glass and electronic circuit board icon, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `mixer_repair` | Mixer Grinder Repair | `Cute 3D isometric clay render of an Indian kitchen mixer grinder base with stainless steel blender jar and rubber coupler gasket, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `chimney_clean` | Kitchen Chimney Cleaning | `Cute 3D isometric clay render of a glass curved kitchen exhaust chimney with stainless steel baffle filter, degreasing foam spray, and brush, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `geyser` | Geyser Fitting / Repair | `Cute 3D isometric clay render of a cylindrical bathroom water geyser heater with red heat indicator light and flexible hot water pipes, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |

---

## 📂 Category 6: Carpenter (`CARPENTER`)

| Service ID | Service Name | Prompt |
| :--- | :--- | :--- |
| `carp_lock` | Door Lock Fitting / Repair | `Cute 3D isometric clay render of a wooden door section with modern brass mortise door handle lock and keyhole with shiny silver keys, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `carp_door` | Door & Window Hinges Alignment | `Cute 3D isometric clay render of a wooden panel door with metal butt hinges, wood plane tool, and chisel shaving wood curls, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `carp_curtain` | Curtain Rod / Bracket Fitting | `Cute 3D isometric clay render of a stylish metal curtain rod with finials, hanging fabric drape ring, and mounting brackets, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `carp_assembly` | Bed / Furniture Assembly | `Cute 3D isometric clay render of a modern wooden bed frame being assembled with allen key wrench, bolts, and wooden slats, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `carp_visit` | Carpenter Visit | `Cute 3D isometric clay render of a wooden carpenter toolbox with manual handsaw, wooden ruler, claw hammer, and measuring tape, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |

---

## 📂 Category 7: Painting (`PAINTER`)

| Service ID | Service Name | Prompt |
| :--- | :--- | :--- |
| `paint_touchup` | Wall Touch-up Painting | `Cute 3D isometric clay render of a paint roller dripping with smooth pastel paint, a metal paint tray, and putty spatula scraper, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `paint_visit` | Painting & Waterproofing Quote | `Cute 3D isometric clay render of an open paint can with brush, colorful color shade swatch card fan, and laser distance meter, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |

---

## 📂 Category 8: Home Help & Shifting (`HOME_HELP`)

| Service ID | Service Name | Prompt |
| :--- | :--- | :--- |
| `help_shifting` | Shifting / Loading Helpers | `Cute 3D isometric clay render of two brown cardboard moving boxes sealed with tape, a hand trolley cart, and packing bubble wrap, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `help_cook` | Cook for One Meal | `Cute 3D isometric clay render of an Indian stainless steel cooking kadai wok on a small flame with wooden spatula, fresh herbs, and steam, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `help_garden` | Garden & Plant Cleaning | `Cute 3D isometric clay render of a cute potted monstera plant, gardening trowel shovel, pruning shears, and pastel watering can, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `help_festival` | Festival Decoration Help | `Cute 3D isometric clay render of a traditional brass Indian diya oil lamp with warm golden flame, orange marigold flower garland, and fairy lights, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `help_hourly_1hr` | Hourly Helper (1 Hr) | `Cute 3D isometric clay render of a friendly helper figure clipboard checklist with clock timer and sparkling star icon, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `help_hourly_2hr` | Hourly Helper (2 Hrs) | `Cute 3D isometric clay render of an hourglass timer surrounded by neat storage baskets, feather duster, and organized boxes, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `help_laundry` | Laundry & Ironing Help | `Cute 3D isometric clay render of a modern pastel steam clothing iron standing next to a neat stack of folded pastel shirts on ironing board, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |

---

## 📂 Category 9: Car & Bike Wash (`VEHICLE`)

| Service ID | Service Name | Prompt |
| :--- | :--- | :--- |
| `car_wash` | Car Wash at Home | `Cute 3D isometric clay render of a compact modern hatchback car covered in rich white snow foam bubbles with microfibre wash mitt and pressure hose, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |
| `bike_wash` | Bike / Scooter Wash at Home | `Cute 3D isometric clay render of a modern motor scooter motorcycle covered in soap foam with a yellow bucket and water spray, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, high detail, centered, no frame --v 6.0` |

---

## 🏷️ Category Cards (9 Category Tile Prompts)

Use these 9 prompts for the Category Grid tiles:

1. **Home Cleaning**: `Cute 3D isometric clay render of a spray bottle, bucket with bubbles, and upright mop, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, centered, no frame --v 6.0`
2. **AC Service**: `Cute 3D isometric clay render of split air conditioner unit with icy breeze arrows and snowflake, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, centered, no frame --v 6.0`
3. **Electrician**: `Cute 3D isometric clay render of a light bulb, pliers, and yellow voltage line tester, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, centered, no frame --v 6.0`
4. **Plumber**: `Cute 3D isometric clay render of a shiny chrome water tap with water drop and pipe wrench, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, centered, no frame --v 6.0`
5. **Appliance & RO**: `Cute 3D isometric clay render of an RO purifier and washing machine drum, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, centered, no frame --v 6.0`
6. **Carpenter**: `Cute 3D isometric clay render of a claw hammer, wooden block, and hand saw, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, centered, no frame --v 6.0`
7. **Painting**: `Cute 3D isometric clay render of a paint can, paint roller with dripping paint, and color swatch, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, centered, no frame --v 6.0`
8. **Home Help & Shifting**: `Cute 3D isometric clay render of sealed cardboard moving boxes on trolley cart, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, centered, no frame --v 6.0`
9. **Car & Bike Wash**: `Cute 3D isometric clay render of a shiny car wheel and pressure foam spray gun with water droplets, pastel matte finish, soft studio lighting, smooth clay texture, transparent PNG background, centered, no frame --v 6.0`
