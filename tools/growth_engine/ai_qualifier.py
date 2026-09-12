import re
import json
from typing import Optional, Dict, Any
from .config import GEMINI_API_KEY, DUTYPE_APP_URL, DUTYPE_COMMUNITY_URL

# Initialize Gemini if key is provided and library is installed
gemini_model = None
if GEMINI_API_KEY:
    try:
        import google.generativeai as genai
        genai.configure(api_key=GEMINI_API_KEY)
        gemini_model = genai.GenerativeModel("gemini-1.5-flash")
    except ImportError:
        print("⚠️ 'google-generativeai' package not installed. Using heuristic qualification fallback.")
        gemini_model = None

def extract_phone_regex(text: str) -> Optional[str]:
    """Fallback phone number regex extractor for Indian mobile numbers."""
    if not text:
        return None
    patterns = [
        r'(?:\+91[\-\s]?)?[6789]\d{9}',
        r'\b\d{5}[\s\-]?\d{5}\b'
    ]
    for pattern in patterns:
        match = re.search(pattern, text)
        if match:
            clean_num = re.sub(r'[\s\-+]', '', match.group(0))
            if clean_num.startswith('91') and len(clean_num) == 12:
                clean_num = clean_num[2:]
            if len(clean_num) == 10:
                return clean_num
    return None

def fallback_heuristic_qualifier(caption: str, author: str) -> Optional[Dict[str, Any]]:
    """Heuristic fallback qualifier when Gemini API key is not configured."""
    caption_lower = caption.lower()
    
    # Common blue-collar keywords
    roles = {
        "cook": "Cook / Chef",
        "chef": "Cook / Chef",
        "maid": "House Maid / Helper",
        "housemaid": "House Maid / Helper",
        "driver": "Driver",
        "driving": "Driver",
        "delivery": "Delivery Executive",
        "security": "Security Guard",
        "waiter": "Restaurant Waiter",
        "cleaner": "Cleaning Staff",
        "hotel": "Hotel / Restaurant Staff",
        "office boy": "Office Boy / Peon",
        "electrician": "Electrician",
        "plumber": "Plumber"
    }
    
    matched_role = None
    for kw, role_name in roles.items():
        if kw in caption_lower:
            matched_role = role_name
            break
            
    if not matched_role:
        return None  # Irrelevant
        
    is_employer = any(w in caption_lower for w in ["hiring", "wanted", "vacancy", "urgent requirement", "opening", "need", "looking for"])
    is_worker = any(w in caption_lower for w in ["available", "need job", "looking for job", "experienced driver", "experienced cook"])
    
    if not is_employer and not is_worker:
        is_employer = True  # Default to employer hiring
        
    lead_type = "WORKER" if is_worker and not is_employer else "EMPLOYER"
    
    # City detection
    city = "Hyderabad"
    for c in ["hyderabad", "nizamabad", "vijayawada", "visakhapatnam", "vizag", "bengaluru", "bangalore", "kolkata", "mumbai", "pune"]:
        if c in caption_lower:
            city = c.capitalize()
            break
            
    phone = extract_phone_regex(caption)
    
    if lead_type == "EMPLOYER":
        summary = f"Hiring for {matched_role} in {city}"
        comment_pitch = f"Need a verified {matched_role} in 15 mins? Post on DutyPe with zero hassle! 🚀"
        dm_pitch = f"Hello! Saw your hiring post for {matched_role} in {city}. You can post your vacancy on DutyPe (https://dutype.in) to connect directly with background-verified staff nearby with zero agent commission."
    else:
        summary = f"Job Seeker: {matched_role} looking for work in {city}"
        comment_pitch = f"Looking for daily or full-time {matched_role} duties? Download DutyPe app to get direct hiring with same-day payouts! 💼"
        dm_pitch = f"Hello! If you are looking for verified {matched_role} jobs in {city}, join our official DutyPe Jobs WhatsApp group: {DUTYPE_COMMUNITY_URL} or install DutyPe app to get hired directly."
        
    return {
        "lead_type": lead_type,
        "category_role": matched_role,
        "city": city,
        "phone_number": phone or "",
        "summary": summary,
        "comment_pitch": comment_pitch,
        "dm_pitch": dm_pitch
    }

def qualify_lead_with_ai(caption: str, author: str) -> Optional[Dict[str, Any]]:
    """
    Uses Gemini 1.5 Flash to analyze the post, extract key fields, and draft contextual pitches.
    Falls back gracefully to heuristic matching if API key is not present.
    """
    if not caption or len(caption.strip()) < 5:
        return None
        
    if not gemini_model:
        return fallback_heuristic_qualifier(caption, author)
        
    prompt = f"""
    You are an expert Lead Intelligence & Growth Outreach Agent for DutyPe (India's hyperlocal blue-collar hiring platform).
    
    Analyze this Instagram post caption:
    Author: @{author}
    Caption:
    \"\"\"{caption}\"\"\"
    
    Categories we support: Cook, Maid/Househelp, Driver, Delivery, Security Guard, Waiter/Hotel Staff, Office Boy, Electrician, Plumber, Painter, Caretaker, General Labour.
    
    Tasks:
    1. Determine if this post is:
       - 'EMPLOYER': Someone hiring or looking for staff (e.g. household, restaurant, office, shop).
       - 'WORKER': A person seeking a job/work opportunity.
       - 'IRRELEVANT': Not related to local/blue-collar jobs (e.g. IT/software jobs, general memes, unrelated advertisements).
    2. If IRRELEVANT, return JSON: {{"is_relevant": false}}
    3. If RELEVANT, extract:
       - lead_type: "EMPLOYER" or "WORKER"
       - category_role: Most specific role name
       - city: Location/City name (default to "Hyderabad" if not specified)
       - phone_number: Indian 10-digit mobile number if present in text, else empty string ""
       - summary: 1-line crisp summary of the vacancy/requirement
       - comment_pitch: Short, friendly, non-spammy comment under 18 words pitching DutyPe.
       - dm_pitch: Polite, high-converting direct message under 45 words inviting them to DutyPe (include https://dutype.in).
    
    Return ONLY a valid JSON object matching this schema:
    {{
      "is_relevant": true,
      "lead_type": "EMPLOYER" | "WORKER",
      "category_role": "string",
      "city": "string",
      "phone_number": "string",
      "summary": "string",
      "comment_pitch": "string",
      "dm_pitch": "string"
    }}
    """
    
    try:
        response = gemini_model.generate_content(
            prompt,
            generation_config={"response_mime_type": "application/json"}
        )
        data = json.loads(response.text)
        if not data.get("is_relevant", False):
            return None
            
        # Extract phone fallback if AI missed it
        if not data.get("phone_number"):
            data["phone_number"] = extract_phone_regex(caption) or ""
            
        return data
    except Exception as e:
        print(f"⚠️ Gemini API fallback to heuristics: {e}")
        return fallback_heuristic_qualifier(caption, author)
