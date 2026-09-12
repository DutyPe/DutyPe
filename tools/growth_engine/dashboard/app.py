from pathlib import Path
from typing import Optional
from fastapi import FastAPI, Request, Query
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.templating import Jinja2Templates
from pydantic import BaseModel

from ..db import init_db, get_leads, get_stats, update_lead_status
from ..scanners.instagram_scanner import scan_instagram_hashtags
from ..scanners.mock_scanner import seed_mock_leads

app = FastAPI(title="DutyPe Growth Engine")

TEMPLATES_DIR = Path(__file__).resolve().parent / "templates"
templates = Jinja2Templates(directory=str(TEMPLATES_DIR))

# Ensure database is created on startup
@app.on_event("startup")
def on_startup():
    init_db()

class StatusUpdateRequest(BaseModel):
    status: str
    notes: Optional[str] = None

@app.get("/", response_class=HTMLResponse)
def index(request: Request, lead_type: Optional[str] = Query(None)):
    init_db()
    leads = get_leads(lead_type=lead_type, limit=100)
    stats = get_stats()
    return templates.TemplateResponse(
        "index.html",
        {
            "request": request,
            "leads": leads,
            "stats": stats,
            "current_type": lead_type
        }
    )

@app.get("/api/leads")
def api_get_leads(status: Optional[str] = None, lead_type: Optional[str] = None):
    return {"leads": get_leads(status=status, lead_type=lead_type)}

@app.get("/api/stats")
def api_get_stats():
    return get_stats()

@app.post("/api/leads/{lead_id}/status")
def api_update_status(lead_id: int, payload: StatusUpdateRequest):
    success = update_lead_status(lead_id, payload.status, payload.notes)
    return {"success": success}

@app.post("/api/scan")
def api_trigger_scan():
    discovered = scan_instagram_hashtags(max_posts_per_tag=5, delay_seconds=1.5)
    return {"status": "success", "count": len(discovered), "leads": discovered}

@app.post("/api/seed-mock")
def api_seed_mock():
    seeded = seed_mock_leads()
    return {"status": "success", "count": len(seeded)}
