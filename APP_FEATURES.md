# DutyPe - Product Roadmap & Detailed Feature Specification

**Core Pillars:**
1.  **Hyper-Local:** 100% location-dependent. If it's not nearby, it doesn't exist.
2.  **Non-IT / Blue Collar:** Interfaces designed for low-literacy users. Visual, voice-first, and simple.
3.  **Zero Fraud:** Aggressive filtering of fake jobs, scams, and spam. "Genuinity" is the product.

---

## 📦 Version 1.0: The "Trusted Handshake" (Foundation Release)
**Goal:** Create a safe, verified connection between a local employer and a local worker.
**Fraud Prevention Strategy:** Restrict input freedom to prevent spam.

### 1. The "Anti-Fraud" Job Posting Engine (Employer Side)
*Why: Scammers thriv    e on free-text fields. We remove them.*
- **[ ] Structured Job Titles (No Free Text):**
  - **Feature:** Employers CANNOT type a job title. They must select from a pre-set list (e.g., *Driver, Maid, Cook, Security Guard, Loader, Sales Helper*).
  - **Logic:** This eliminates "Earn ₹50,000/day working from home" scams instantly.
- **[ ] Location Consistency Check:**
  - **Feature:** Compare the Employer's current GPS location with the "Job Location" pin they set.
  - **Logic:** If the distance > 50km, flag as "Suspicious" (prevents remote scam centers from posting local jobs).
- **[ ] Pay Rate Guardrails:**
  - **Feature:** Min/Max validation on salary fields.
  - **Logic:** Error if someone tries to offer ₹10,000 for 1 hour of cleaning. Keeps expectations realistic.

### 2. Hyper-Local Discovery (Worker Side)
*Why: A cleaner won't travel 20km for a 2-hour shift.*
- **[ ] The "Radially Sorted" Feed:**
  - **Algorithm:** `Sort By: Distance (Ascending)` is the *only* default.
  - **Visuals:** Show distance prominently: "📍 **400 meters away**".
- **[ ] Map-First Interface:**
  - **Feature:** A full-screen map showing job pins.
  - **Non-IT Focus:** Workers recognize landmarks ("Oh, a job near the big temple") better than street names.

### 3. Voice-First Profile & Apply
*Why: Typing is friction for non-IT workers. Speaking is natural.*
- **[ ] Audio Job Descriptions:**
  - **Feature:** Employer can record a 30s audio clip: "Need someone to unload a truck at 5 PM."
- **[ ] "Listen" Button for Workers:**
  - **Feature:** Text-to-Speech button next to job details for workers who struggle to read English/Hindi.
- **[ ] The Micro-Profile:**
  - **Feature:** No Resumes. Profile is just: Photo + Name + Phone + 3 Skill Tags + "Years of Experience".

---

## ⚙️ Version 1.1: Operations & "Show-Up" Reliability
**Goal:** Solve the biggest problem in blue-collar gigs: **Attrition & No-Shows.**

### 4. Commitment Architecture
- **[ ] The "Promise" Token:**
  - **Feature:** When a worker accepts a job, they get a digital "Job Card".
  - **Logic:** It looks like a ticket/pass. Psychology: "I have a ticket, I must go."
- **[ ] Penalties for Flaking:**
  - **Logic:** If a worker accepts but doesn't show up, their account gets a "Strike". 3 Strikes = 7-day suspension.
  - **Why:** Enforces seriousness.

### 5. Hyper-Local Communication
- **[ ] Voice Chat:**
  - **Feature:** In-app audio messages (like WhatsApp voice notes).
  - **Why:** Essential for explaining precise directions ("Come to the back gate, near the blue tree").
- **[ ] Location Sharing:**
  - **Feature:** "Send Current Location" button in chat.

---

## 🛡️ Version 1.2: The "Iron Dome" (Trust & Safety)
**Goal:** Make the platform hostile to bad actors and safe for women/vulnerable workers.

### 6. Deep Verification Layer
- **[ ] Aadhaar OCR & Face Match:**
  - **Feature:** Scan Aadhaar card -> Extract Name -> Compare with Selfie.
  - **Status:** Marks profile as "Government Verified" (Green Tick).
- **[ ] Employer Business Verification:**
  - **Feature:** Employers posting > 3 jobs must upload a photo of their Shop/Office board or Visiting Card.
  - **Why:** Proves they are a real business entity, not a scammer.

### 7. The "Vouch" System (Social Proof)
- **[ ] Past Employer Ratings:**
  - **Feature:** Simple question after job: "Did they show up on time?" (Yes/No).
  - **Logic:** Build a "Reliability Score" (%) visible to future employers.
- **[ ] Community Reporting:**
  - **Feature:** "Report Fake Job" button. If 3 workers report a job, it is auto-hidden until manual review.

---

## 💰 Version 2.0: Fintech & Sustainable Growth
**Goal:** Monetize value, not access.

### 8. Frictionless Payments
- **[ ] "Escrow" Lite (Trust Pay):**
  - **Feature:** Employer deposits money into the app *before* the shift starts.
  - **Logic:** Money is released to Worker only when they complete the shift.
  - **Why:** Solves "Worker fear of not getting paid" and "Employer fear of worker running away".

### 9. Monetization for "Power Users"
- **[ ] Urgent Hiring Fee:** Employer pays ₹49 to blast notification to 500 nearby workers instantly.
- **[ ] Contact Unlock:** First 3 applicants are free. Employer pays to see contact details of the 4th+ applicant.
