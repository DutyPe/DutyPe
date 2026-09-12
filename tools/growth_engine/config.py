import os
from pathlib import Path
from dotenv import load_dotenv

# Load .env file from the growth engine folder if present
BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env")

# Google Gemini API Key (Free tier from Google AI Studio)
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

# SQLite Database Path
DB_PATH = BASE_DIR / "leads.db"

# Target Hashtags for Lead Discovery
EMPLOYER_HASHTAGS = [
    "hyderabadjobs",
    "hiringhyderabad",
    "nizamabadjobs",
    "cookwanted",
    "drivervacancy",
    "maidvacancy",
    "maidservice",
    "restauranthiring",
    "urgenthiringhyderabad",
    "staffwanted",
    "deliveryboyjobs",
    "securityguardjobs",
    "hoteljobs"
]

WORKER_HASHTAGS = [
    "driveravailable",
    "cookavailable",
    "needjobhyderabad",
    "househelp",
    "lookingforjob",
    "deliveryboy",
    "hotelstaff",
    "dailywages",
    "electricianavailable",
    "plumberavailable"
]

# Supported Cities
SUPPORTED_CITIES = [
    "Hyderabad",
    "Nizamabad",
    "Vijayawada",
    "Visakhapatnam",
    "Bengaluru",
    "Kolkata",
    "Mumbai",
    "Pune"
]

# Default DutyPe App Link & WhatsApp Help Line
DUTYPE_APP_URL = "https://dutype.in"
DUTYPE_COMMUNITY_URL = "https://chat.whatsapp.com/ITnhw0jk2G0I9TNlDCaNQI?s=cl&p=a&ilr=4"
