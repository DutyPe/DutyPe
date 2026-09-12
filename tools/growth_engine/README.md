# 🚀 DutyPe Social Growth & Lead Acquisition Engine

A scalable, account-safe lead intelligence and outreach engine that automatically discovers **Employers hiring staff** and **Workers seeking jobs** on Instagram, extracts roles/cities/phone numbers with AI, generates high-converting pitches, and provides a 1-click dispatch dashboard.

---

## ⚡ Quick Start

### 1. Install Dependencies
```bash
cd tools/growth_engine
pip install -r requirements.txt
```

### 2. Configure Gemini AI (Optional - Free)
Create a `.env` file or set the `GEMINI_API_KEY` environment variable:
```env
GEMINI_API_KEY=your_free_gemini_api_key_from_google_ai_studio
```
*(Note: If no key is set, the engine automatically uses built-in heuristic pattern matching!)*

### 3. Launch the Growth Dashboard
```bash
python -m tools.growth_engine.main dashboard
```
Open **http://localhost:8000** in your browser.

---

## 🎯 Command-Line Usage

* **Launch Web UI**:
  ```bash
  python -m tools.growth_engine.main dashboard
  ```
* **Run Live Instagram Scan**:
  ```bash
  python -m tools.growth_engine.main scan
  ```
* **Load Sample Leads (Demo mode)**:
  ```bash
  python -m tools.growth_engine.main seed
  ```
* **View Lead Pipeline Stats**:
  ```bash
  python -m tools.growth_engine.main stats
  ```

---

## 🛡️ Why This Is 100% Ban-Safe
1. **Zero Bot Flagging**: Scrapes publicly accessible hashtag feeds without logging into or risking your main Instagram account.
2. **AI-Tailored Pitches**: Never sends duplicate or copy-pasted comments. Every pitch is contextually customized for the exact job role (Cook, Maid, Driver, etc.).
3. **1-Click WhatsApp & Instagram**: When phone numbers are detected in captions or posters, the dashboard gives you a direct 1-click **"Chat on WhatsApp"** button with the pitch prefilled!
