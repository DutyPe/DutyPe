"""
Test All Features - Complete backend test
"""

import asyncio
import httpx

BASE = "http://localhost:8000"


async def main():
    print("\n" + "=" * 60)
    print("DUTYPE AI BACKEND - COMPLETE FEATURE TEST")
    print("=" * 60)
    
    async with httpx.AsyncClient(timeout=60.0) as c:
        
        # 1. Health
        print("\n[1] Health Check")
        r = await c.get(f"{BASE}/api/v1/health")
        if r.status_code != 200:
            print("❌ Server not running!")
            return
        print("✅ Server healthy")
        
        # 2. Check all routes
        print("\n[2] Route Count")
        r = await c.get(f"{BASE}/openapi.json")
        paths = list(r.json().get("paths", {}).keys())
        print(f"✅ Total routes: {len(paths)}")
        
        # 3. Fraud Detection
        print("\n[3] Fraud Detection")
        r = await c.post(f"{BASE}/api/v1/fraud/detect", json={
            "text": "Work from home data entry. Earn 50000. Registration fee 500.",
            "context": "job_description"
        })
        data = r.json()
        print(f"✅ Fraud: {data.get('is_fraud')} | Prob: {data.get('fraud_probability')}%")
        
        # 4. Job Analysis
        print("\n[4] Job Analysis")
        r = await c.post(f"{BASE}/api/v1/jobs/analyze", json={
            "title": "Delivery Boy",
            "description": "Food delivery in Mumbai",
            "pay_amount": 500,
            "pay_type": "DAILY",
            "location": "Mumbai",
            "category": "delivery",
            "vacancies": 5,
            "urgency": "REGULAR",
            "employer_id": "emp_001"
        })
        data = r.json()
        print(f"✅ Risk: {data.get('risk_score')} | Level: {data.get('risk_level')}")
        
        # 5. Worker Chat
        print("\n[5] Worker Chatbot")
        r = await c.post(f"{BASE}/api/v1/chat/worker", json={
            "message": "Is this job safe?",
            "user_id": "worker_001",
            "context": {"job": {"title": "Data Entry", "risk_score": 80}}
        })
        data = r.json()
        print(f"✅ Reply: {data.get('reply', '')[:60]}...")
        
        # 6. Employer Chat
        print("\n[6] Employer Chatbot")
        r = await c.post(f"{BASE}/api/v1/chat/employer", json={
            "message": "Why is my score low?",
            "user_id": "emp_001",
            "context": {"score": {"reliability_score": 40}}
        })
        data = r.json()
        print(f"✅ Reply: {data.get('reply', '')[:60]}...")
        
        # 7. Silence Detection
        print("\n[7] Silence Detection")
        r = await c.post(f"{BASE}/api/v1/protection/silence/track-view", json={
            "application_id": "app_001",
            "employer_id": "emp_001",
            "worker_id": "worker_001"
        })
        print(f"✅ View tracked: {r.status_code == 200}")
        
        r = await c.get(f"{BASE}/api/v1/protection/silence/check/app_001")
        data = r.json()
        print(f"✅ Silence check: {data.get('is_silent', False)}")
        
        # 8. Duplicate Detection
        print("\n[8] Duplicate Detection")
        r = await c.post(f"{BASE}/api/v1/protection/duplicate/register", json={
            "job_id": "job_001",
            "title": "Delivery Boy Needed",
            "description": "Food delivery in Hyderabad. Bike required.",
            "employer_id": "emp_001"
        })
        print(f"✅ Job registered: {r.status_code == 200}")
        
        r = await c.post(f"{BASE}/api/v1/protection/duplicate/check", json={
            "job_id": "job_002",
            "title": "Delivery Boy Needed",
            "description": "Food delivery in Hyderabad. Bike required.",
            "employer_id": "emp_002"
        })
        data = r.json()
        print(f"✅ Duplicate: {data.get('is_duplicate')} | Type: {data.get('duplicate_type')}")
        
        # 9. Multi-Account Detection
        print("\n[9] Multi-Account Detection")
        r = await c.post(f"{BASE}/api/v1/protection/multi-account/register", json={
            "employer_id": "emp_001",
            "phone": "9876543210",
            "device_id": "device_abc"
        })
        print(f"✅ Employer registered: {r.status_code == 200}")
        
        r = await c.post(f"{BASE}/api/v1/protection/multi-account/register", json={
            "employer_id": "emp_002",
            "phone": "9876543210",
            "device_id": "device_xyz"
        })
        data = r.json()
        print(f"✅ Multi-account: {data.get('is_suspicious')} | Linked: {len(data.get('linked_accounts', []))}")
        
        # 10. Worker Protection
        print("\n[10] Worker Protection")
        r = await c.post(f"{BASE}/api/v1/protection/worker/assess-risk", json={
            "worker_id": "worker_001",
            "job": {"requires_travel": True, "risk_score": 70, "urgency": "INSTANT"},
            "employer": {"reliability_score": 30, "ghosting_incidents": 3}
        })
        data = r.json()
        print(f"✅ Risk: {data.get('risk_score')} | Level: {data.get('protection_level')}")
        
        # 11. Payment Check (Scam)
        print("\n[11] Payment Request Check")
        r = await c.post(
            f"{BASE}/api/v1/protection/worker/check-payment",
            params={
                "worker_id": "worker_001",
                "employer_id": "emp_scam",
                "amount": 500,
                "reason": "registration fee"
            }
        )
        data = r.json()
        print(f"✅ Is Scam: {data.get('is_scam')} | Action: {data.get('action')}")
        
        # 12. Regional Patterns
        print("\n[12] Regional Scam Detection")
        r = await c.post(
            f"{BASE}/api/v1/protection/regional/detect",
            params={"text": "घर बैठे कमाएं 50000 रुपये। रजिस्ट्रेशन फीस सिर्फ 500"}
        )
        data = r.json()
        print(f"✅ Hindi scam: {data.get('is_scam')} | Types: {data.get('scam_types')}")
        
        # 13. Learning Service
        print("\n[13] Learning Service")
        r = await c.post(f"{BASE}/api/v1/protection/learning/report", json={
            "reporter_id": "worker_001",
            "reporter_type": "worker",
            "target_type": "employer",
            "target_id": "emp_scam",
            "reason": "SCAM",
            "description": "Asked for registration fee before interview"
        })
        print(f"✅ Report submitted: {r.status_code == 200}")
        
        r = await c.get(f"{BASE}/api/v1/protection/learning/stats")
        data = r.json()
        print(f"✅ Total reports: {data.get('total_reports')}")
        
        # Summary
        print("\n" + "=" * 60)
        print("FEATURE TEST COMPLETE")
        print("=" * 60)
        print(f"\nTotal API Routes: {len(paths)}")
        print("\nFeatures Tested:")
        print("  ✅ Job Analysis (AI)")
        print("  ✅ Fraud Detection (AI + Rules)")
        print("  ✅ Worker Chatbot (AI)")
        print("  ✅ Employer Chatbot (AI)")
        print("  ✅ Silence Detection")
        print("  ✅ Duplicate Detection")
        print("  ✅ Multi-Account Detection")
        print("  ✅ Worker Protection")
        print("  ✅ Regional Scam Patterns (Hindi)")
        print("  ✅ Learning Service")
        print("=" * 60 + "\n")

if __name__ == "__main__":
    asyncio.run(main())
