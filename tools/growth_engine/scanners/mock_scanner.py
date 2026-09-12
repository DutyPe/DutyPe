import random
from typing import List, Dict, Any
from ..db import insert_lead, is_post_scanned
from ..ai_qualifier import qualify_lead_with_ai

MOCK_POSTS = [
    {
        "author": "spice_hyderabad_restaurant",
        "caption": "URGENT HIRING! We need 2 South Indian Tiffin Cooks and 3 Waiters for our new branch in Madhapur, Hyderabad. Good salary + food + accommodation. Call or WhatsApp immediately at 9849012345. #hyderabadjobs #cookwanted #restauranthiring",
        "shortcode": "mock_reel_101"
    },
    {
        "author": "priya_sharma_hyd",
        "caption": "Looking for a full-time live-in house maid and cook in Gachibowli, Hyderabad. Experienced in vegetarian cooking and cleaning. Contact 8765432109. #maidservice #housemaid #cookwanted",
        "shortcode": "mock_reel_102"
    },
    {
        "author": "ramesh_driver_services",
        "caption": "I am an experienced personal driver with 8 years experience in automatic and manual luxury cars in Hyderabad and Cyberabad. Looking for personal or company driver duty. Call 9988776655. #driveravailable #lookingforjob #driverjob",
        "shortcode": "mock_reel_103"
    },
    {
        "author": "royal_security_nizamabad",
        "caption": "Urgent requirement for 5 Security Guards for commercial complex in Nizamabad. Age 25-45, 12 hours shift, salary 16,000. Contact: 9123456780. #nizamabadjobs #securityguardjobs #urgenthiring",
        "shortcode": "mock_reel_104"
    },
    {
        "author": "suresh_kumar_cook",
        "caption": "North & South Indian cook with 6 years hotel experience looking for hotel/mess daily or monthly vacancy in Vijayawada or Hyderabad. Contact 9550011223. #cookavailable #needjob",
        "shortcode": "mock_reel_105"
    },
    {
        "author": "urban_quick_delivery",
        "caption": "Hiring 10 Bike Delivery Executives for food and grocery delivery in Banjara Hills & Jubilee Hills. Earning up to 30,000/month + incentives. #deliveryboyjobs #hyderabadjobs #urgenthiring",
        "shortcode": "mock_reel_106"
    }
]

def seed_mock_leads() -> List[Dict[str, Any]]:
    """Seeds realistic sample leads into the database for immediate testing."""
    seeded = []
    for item in MOCK_POSTS:
        post_url = f"https://www.instagram.com/p/{item['shortcode']}/"
        if is_post_scanned(post_url):
            continue

        qualification = qualify_lead_with_ai(item["caption"], item["author"])
        if qualification:
            lead_record = {
                "platform": "instagram",
                "post_url": post_url,
                "author_username": item["author"],
                "lead_type": qualification.get("lead_type", "EMPLOYER"),
                "category_role": qualification.get("category_role", "Other"),
                "city": qualification.get("city", "Hyderabad"),
                "phone_number": qualification.get("phone_number", ""),
                "raw_caption": item["caption"],
                "summary": qualification.get("summary", ""),
                "comment_pitch": qualification.get("comment_pitch", ""),
                "dm_pitch": qualification.get("dm_pitch", "")
            }
            lead_id = insert_lead(lead_record)
            if lead_id:
                lead_record["id"] = lead_id
                seeded.append(lead_record)

    return seeded
