import sys
import io

# Ensure UTF-8 output encoding on Windows terminals
if sys.stdout and hasattr(sys.stdout, "reconfigure"):
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

from .db import init_db, get_stats, get_leads
from .scanners.instagram_scanner import scan_instagram_hashtags
from .scanners.mock_scanner import seed_mock_leads

def print_help():
    print("""
=====================================================
🚀 DutyPe Social Growth & Lead Acquisition Engine
=====================================================
Usage:
  python -m tools.growth_engine.main dashboard     # Launch the interactive Web Command Center
  python -m tools.growth_engine.main scan          # Run Instagram live hashtag scanner
  python -m tools.growth_engine.main seed          # Seed sample test leads into database
  python -m tools.growth_engine.main stats         # View current lead stats in terminal
=====================================================
""")

def run():
    init_db()
    args = sys.argv[1:]
    
    if not args or args[0] in ("help", "-h", "--help"):
        print_help()
        return

    cmd = args[0].lower()

    if cmd == "dashboard":
        try:
            import uvicorn
        except ImportError:
            print("❌ 'uvicorn' or 'fastapi' not installed. Run: pip install -r tools/growth_engine/requirements.txt")
            return
        print("🌐 Starting DutyPe Growth Command Center on http://localhost:8000 ...")
        uvicorn.run("tools.growth_engine.dashboard.app:app", host="127.0.0.1", port=8000, reload=True)

    elif cmd == "scan":
        print("🔎 Starting live Instagram hashtag scan...")
        leads = scan_instagram_hashtags()
        print(f"✅ Scan finished! Discovered {len(leads)} new qualified leads.")

    elif cmd == "seed":
        print("🌱 Seeding realistic test leads...")
        seeded = seed_mock_leads()
        print(f"✅ Seeded {len(seeded)} sample leads into database.")

    elif cmd == "stats":
        stats = get_stats()
        print("\n📊 DutyPe Lead Pipeline Statistics:")
        for k, v in stats.items():
            print(f"  • {k.replace('_', ' ').title()}: {v}")
        print()

    else:
        print(f"❌ Unknown command: '{cmd}'")
        print_help()

if __name__ == "__main__":
    run()
