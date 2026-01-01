"""
Debug Test - Run this after starting server
"""

import asyncio
import httpx

BASE = "http://localhost:8000"


async def main():
    print("\n" + "=" * 50)
    print("DUTYPE AI BACKEND - DEBUG TEST")
    print("=" * 50)
    
    async with httpx.AsyncClient(timeout=60.0) as c:
        
        # Health
        print("\n[1] Health Check")
        try:
            r = await c.get(f"{BASE}/api/v1/health")
            print(f"    ✅ {r.status_code} - {r.json()['status']}")
        except Exception as e:
            print(f"    ❌ Server not running: {e}")
            print("\n    Start server: python main.py")
            return
        
        # Check routes exist
        print("\n[2] Checking Routes")
        r = await c.get(f"{BASE}/openapi.json")
        paths = list(r.json().get("paths", {}).keys())
        chat_routes = [p for p in paths if "chat" in p]
        print(f"    Total routes: {len(paths)}")
        print(f"    Chat routes: {chat_routes}")
        
        if not chat_routes:
            print("\n    ⚠️ Chat routes missing! Restart server.")
            return
        
        # Keyword check (no AI)
        print("\n[3] Keyword Check (No AI)")
        r = await c.post(f"{BASE}/api/v1/fraud/check-keywords", params={"text": "work from home"})
        print(f"    Status: {r.status_code}")
        if r.status_code == 200:
            print(f"    Banned: {r.json().get('has_banned_keywords')}")
        
        # Worker Chat (AI)
        print("\n[4] Worker Chat (AI)")
        r = await c.post(f"{BASE}/api/v1/chat/worker", json={
            "message": "Is this job safe?",
            "user_id": "test_worker",
            "context": {"job": {"title": "Data Entry", "risk_score": 80, "risk_level": "HIGH"}}
        })
        print(f"    Status: {r.status_code}")
        if r.status_code == 200:
            data = r.json()
            reply = data.get("reply", "")[:80]
            print(f"    Success: {data.get('success')}")
            print(f"    Reply: {reply}...")
        else:
            print(f"    Error: {r.text[:100]}")
        
        # Employer Chat (AI)
        print("\n[5] Employer Chat (AI)")
        r = await c.post(f"{BASE}/api/v1/chat/employer", json={
            "message": "Why is my score low?",
            "user_id": "test_employer",
            "context": {"score": {"reliability_score": 40, "tier": "RESTRICTED"}}
        })
        print(f"    Status: {r.status_code}")
        if r.status_code == 200:
            data = r.json()
            reply = data.get("reply", "")[:80]
            print(f"    Success: {data.get('success')}")
            print(f"    Reply: {reply}...")
        else:
            print(f"    Error: {r.text[:100]}")
        
        # Job Analysis (AI)
        print("\n[6] Job Analysis (AI)")
        r = await c.post(f"{BASE}/api/v1/jobs/analyze", json={
            "title": "Delivery Boy",
            "description": "Food delivery job",
            "pay_amount": 500,
            "pay_type": "DAILY",
            "location": "Mumbai",
            "category": "delivery",
            "vacancies": 5,
            "urgency": "REGULAR",
            "employer_id": "emp_001"
        })
        print(f"    Status: {r.status_code}")
        if r.status_code == 200:
            data = r.json()
            print(f"    Risk Score: {data.get('risk_score')}")
            print(f"    Risk Level: {data.get('risk_level')}")
        else:
            print(f"    Error: {r.text[:100]}")
    
    print("\n" + "=" * 50)
    print("TEST COMPLETE")
    print("=" * 50 + "\n")


if __name__ == "__main__":
    asyncio.run(main())
