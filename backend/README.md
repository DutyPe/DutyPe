# DutyPe AI Backend

## Overview
AI-powered Trust & Enforcement Engine for DutyPe - India's hyperlocal blue-collar job platform.

## Core Principle
> **Worker time > Employer convenience**
> **LLM = Detection | Rules Engine = Enforcement**

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    DutyPe AI Backend                            │
├─────────────────────────────────────────────────────────────────┤
│  API LAYER (FastAPI)                                            │
│  ├── /api/v1/jobs/analyze      - Job risk scoring               │
│  ├── /api/v1/employers/score   - Employer behavior scoring      │
│  ├── /api/v1/fraud/detect      - Fraud pattern detection        │
│  └── /api/v1/moderation/review - Content moderation             │
├─────────────────────────────────────────────────────────────────┤
│  AI LAYER (Azure OpenAI GPT-4o-mini)                            │
│  ├── Job Description Analyzer                                   │
│  ├── Scam Pattern Detector                                      │
│  ├── Employer Behavior Analyzer                                 │
│  └── Decision Support System                                    │
├─────────────────────────────────────────────────────────────────┤
│  RULES ENGINE (Deterministic)                                   │
│  ├── Keyword Ban System                                         │
│  ├── Pay Rate Validator                                         │
│  ├── SLA Enforcement                                            │
│  └── Penalty Calculator                                         │
├─────────────────────────────────────────────────────────────────┤
│  DATA LAYER (Firebase Firestore)                                │
│  ├── employer_scores                                            │
│  ├── fraud_signals                                              │
│  ├── job_risk_scores                                            │
│  └── moderation_queue                                           │
└─────────────────────────────────────────────────────────────────┘
```

## Tech Stack
- **Language**: Python 3.11+
- **Framework**: FastAPI
- **AI Model**: Azure OpenAI GPT-4o-mini
- **Database**: Firebase Firestore
- **Deployment**: Docker / Cloud Run

## Features

### 1. Job Risk Scoring
- Scam keyword detection
- Pay rate validation
- Location consistency check
- Duplicate detection
- Urgency pattern analysis

### 2. Employer Behavior Scoring
- Response time tracking
- SLA compliance
- Cancellation rate
- Worker feedback analysis
- Trust tier calculation

### 3. Fraud Detection
- WFH/Online scam blocking
- Fake urgency detection
- Contact harvesting detection
- Multi-account detection

### 4. Auto-Enforcement
- Visibility reduction
- Posting cooldowns
- Instant job privilege removal
- Account suspension

## Non-Negotiable Principles
1. Worker time > Employer convenience
2. Employer silence is unacceptable
3. Trust must be enforced with consequences
4. Automation > Manual support
5. Fewer high-quality jobs > Many low-quality jobs

## Setup Instructions

### 1. Create Virtual Environment
```bash
cd backend
python -m venv venv
venv\Scripts\activate  # Windows
# source venv/bin/activate  # Linux/Mac
```

### 2. Install Dependencies
```bash
pip install -r requirements.txt
```

### 3. Configure Environment
```bash
copy .env.example .env
# Edit .env with your Azure OpenAI credentials
```

### 4. Run Development Server
```bash
python main.py
# Or: uvicorn main:app --reload
```

### 5. Access API Docs
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## Docker Deployment
```bash
docker build -t dutype-ai-backend .
docker run -p 8000:8000 --env-file .env dutype-ai-backend
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/jobs/analyze` | POST | Analyze job for risk |
| `/api/v1/jobs/quick-check` | POST | Quick keyword check |
| `/api/v1/employers/score` | POST | Score employer behavior |
| `/api/v1/fraud/detect` | POST | Detect fraud patterns |
| `/api/v1/rules/validate-job` | POST | Validate against rules |
| `/api/v1/enforcement/evaluate` | POST | Evaluate penalties |
