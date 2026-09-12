import sqlite3
from datetime import datetime
from typing import List, Optional, Dict, Any
from .config import DB_PATH

def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    """Initialize database tables if they do not exist."""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS leads (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        platform TEXT DEFAULT 'instagram',
        post_url TEXT UNIQUE NOT NULL,
        author_username TEXT NOT NULL,
        lead_type TEXT NOT NULL,          -- 'EMPLOYER' or 'WORKER'
        category_role TEXT,              -- e.g. Cook, Driver, Maid, Security
        city TEXT,                       -- e.g. Hyderabad, Nizamabad
        phone_number TEXT,               -- Extracted phone number if present
        raw_caption TEXT,
        summary TEXT,                    -- Short summary of requirement
        comment_pitch TEXT,              -- AI generated comment
        dm_pitch TEXT,                   -- AI generated DM
        status TEXT DEFAULT 'NEW',       -- 'NEW', 'CONTACTED', 'CONVERTED', 'IGNORED'
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        contacted_at TIMESTAMP,
        notes TEXT
    )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_leads_status ON leads (status)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_leads_type ON leads (lead_type)")
    conn.commit()
    conn.close()

def is_post_scanned(post_url: str) -> bool:
    """Check if a post URL has already been processed to prevent duplicates."""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT 1 FROM leads WHERE post_url = ?", (post_url,))
    exists = cursor.fetchone() is not None
    conn.close()
    return exists

def insert_lead(lead: Dict[str, Any]) -> Optional[int]:
    """Insert a new qualified lead into the database."""
    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        cursor.execute("""
        INSERT INTO leads (
            platform, post_url, author_username, lead_type, category_role,
            city, phone_number, raw_caption, summary, comment_pitch, dm_pitch, status
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            lead.get("platform", "instagram"),
            lead["post_url"],
            lead["author_username"],
            lead.get("lead_type", "EMPLOYER"),
            lead.get("category_role", "Other"),
            lead.get("city", "Hyderabad"),
            lead.get("phone_number", ""),
            lead.get("raw_caption", ""),
            lead.get("summary", ""),
            lead.get("comment_pitch", ""),
            lead.get("dm_pitch", ""),
            "NEW"
        ))
        conn.commit()
        lead_id = cursor.lastrowid
        return lead_id
    except sqlite3.IntegrityError:
        return None  # Duplicate post
    finally:
        conn.close()

def get_leads(status: Optional[str] = None, lead_type: Optional[str] = None, limit: int = 50, offset: int = 0) -> List[Dict[str, Any]]:
    """Fetch leads with optional status and lead_type filters."""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    query = "SELECT * FROM leads WHERE 1=1"
    params = []
    
    if status:
        query += " AND status = ?"
        params.append(status)
    if lead_type:
        query += " AND lead_type = ?"
        params.append(lead_type)
        
    query += " ORDER BY id DESC LIMIT ? OFFSET ?"
    params.extend([limit, offset])
    
    cursor.execute(query, params)
    rows = [dict(row) for row in cursor.fetchall()]
    conn.close()
    return rows

def update_lead_status(lead_id: int, new_status: str, notes: Optional[str] = None) -> bool:
    """Update lead status (e.g. from NEW to CONTACTED or CONVERTED)."""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    contacted_at = datetime.now().isoformat() if new_status in ("CONTACTED", "CONVERTED") else None
    
    if notes:
        cursor.execute(
            "UPDATE leads SET status = ?, contacted_at = COALESCE(contacted_at, ?), notes = ? WHERE id = ?",
            (new_status, contacted_at, notes, lead_id)
        )
    else:
        cursor.execute(
            "UPDATE leads SET status = ?, contacted_at = COALESCE(contacted_at, ?) WHERE id = ?",
            (new_status, contacted_at, lead_id)
        )
    
    updated = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return updated

def get_stats() -> Dict[str, Any]:
    """Get high-level pipeline stats."""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    cursor.execute("SELECT COUNT(*) FROM leads")
    total_leads = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM leads WHERE status = 'NEW'")
    new_leads = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM leads WHERE status = 'CONTACTED'")
    contacted_leads = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM leads WHERE status = 'CONVERTED'")
    converted_leads = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM leads WHERE lead_type = 'EMPLOYER'")
    employer_leads = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM leads WHERE lead_type = 'WORKER'")
    worker_leads = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM leads WHERE phone_number IS NOT NULL AND phone_number != ''")
    leads_with_phone = cursor.fetchone()[0]
    
    conn.close()
    return {
        "total_leads": total_leads,
        "new_leads": new_leads,
        "contacted_leads": contacted_leads,
        "converted_leads": converted_leads,
        "employer_leads": employer_leads,
        "worker_leads": worker_leads,
        "leads_with_phone": leads_with_phone
    }
