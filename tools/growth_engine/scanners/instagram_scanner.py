import time
from typing import List, Dict, Any
from ..config import EMPLOYER_HASHTAGS, WORKER_HASHTAGS
from ..db import is_post_scanned, insert_lead
from ..ai_qualifier import qualify_lead_with_ai

def scan_instagram_hashtags(hashtags: List[str] = None, max_posts_per_tag: int = 10, delay_seconds: float = 2.0) -> List[Dict[str, Any]]:
    """
    Scans public Instagram hashtags without requiring an account login.
    Extracts relevant hiring & worker leads, runs AI qualification, and persists to DB.
    """
    try:
        import instaloader
    except ImportError:
        print("❌ 'instaloader' package is not installed. Please run: pip install instaloader")
        return []

    if hashtags is None:
        hashtags = EMPLOYER_HASHTAGS[:5] + WORKER_HASHTAGS[:3]

    L = instaloader.Instaloader(
        download_pictures=False,
        download_videos=False,
        download_comments=False,
        save_metadata=False,
        compress_json=False
    )

    discovered_leads = []

    for tag in hashtags:
        print(f"🔎 Scanning Instagram #{tag}...")
        try:
            posts = instaloader.Hashtag.from_name(L.context, tag).get_posts()
            count = 0
            
            for post in posts:
                if count >= max_posts_per_tag:
                    break

                post_url = f"https://www.instagram.com/p/{post.shortcode}/"

                # Deduplication check
                if is_post_scanned(post_url):
                    count += 1
                    continue

                caption = post.caption or ""
                author = post.owner_username

                # Run AI Qualification
                qualification = qualify_lead_with_ai(caption, author)

                if qualification:
                    lead_record = {
                        "platform": "instagram",
                        "post_url": post_url,
                        "author_username": author,
                        "lead_type": qualification.get("lead_type", "EMPLOYER"),
                        "category_role": qualification.get("category_role", "Other"),
                        "city": qualification.get("city", "Hyderabad"),
                        "phone_number": qualification.get("phone_number", ""),
                        "raw_caption": caption,
                        "summary": qualification.get("summary", ""),
                        "comment_pitch": qualification.get("comment_pitch", ""),
                        "dm_pitch": qualification.get("dm_pitch", "")
                    }

                    lead_id = insert_lead(lead_record)
                    if lead_id:
                        lead_record["id"] = lead_id
                        discovered_leads.append(lead_record)
                        print(f"  ✨ [New Lead #{lead_id}] {lead_record['lead_type']}: {lead_record['category_role']} in {lead_record['city']} by @{author}")

                count += 1
                time.sleep(delay_seconds)

        except Exception as e:
            print(f"  ⚠️ Error scanning #{tag}: {e}")
            time.sleep(5)

    return discovered_leads
