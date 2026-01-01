"""
Test script for DutyPe AI Backend
Tests the Azure OpenAI GPT-4o-mini integration
"""

import asyncio
import httpx
import json

BASE_URL = "http://localhost:8000/api/v1"


async def test_health():
    """Test health endpoint"""
    async with httpx.AsyncClient() as client:
        response = await client.get(f"{BASE_URL}/health")
        print("=" * 50)
        print("HEALTH CHECK")
        print("=" * 50)
        print(f"Status: {response.status_code}")
        print(json.dumps(response.json(), indent=2))
        return response.status_code == 200


async def test_job_analysis():
    """Test job analysis with various scenarios"""
    print("\n" + "=" * 50)
    print("JOB ANALYSIS TESTS")
    print("=" * 50)
    
    test_cases = [
        {
            "name": "LEGITIMATE JOB",
            "data": {
                "title": "Delivery Boy - Zomato",
                "description": "Food delivery in Hyderabad. Must have bike and smartphone. 8 hour shifts.",
                "pay_amount": 500,
                "pay_type": "DAILY",
                "location": "Hyderabad, Telangana",
                "category": "delivery",
                "vacancies": 5,
                "urgency": "REGULAR",
                "employer_id": "emp_001"
            }
        },
        {
            "name": "SCAM JOB - WFH",
            "data": {
                "title": "Work From Home Data Entry",
                "description": "Earn 50000 per month from home. Simple typing job. No experience needed.",
                "pay_amount": 50000,
                "pay_type": "MONTHLY",
                "location": "Anywhere",
                "category": "data entry",
                "vacancies": 100,
                "urgency": "INSTANT",
                "employer_id": "emp_002"
            }
        },
        {
            "name": "SUSPICIOUS JOB",
            "data": {
                "title": "Urgent Hiring - Security Guard",
                "description": "Immediate joining. Walk-in interview. Freshers welcome. High salary.",
                "pay_amount": 25000,
                "pay_type": "MONTHLY",
                "location": "Delhi",
                "category": "security",
                "vacancies": 50,
                "urgency": "INSTANT",
                "employer_id": "emp_003"
            }
        },
        {
            "name": "CONTACT HARVEST SCAM",
            "data": {
                "title": "Driver Needed",
                "description": "Contact on WhatsApp only 9876543210. Good salary. Call directly for interview.",
                "pay_amount": 600,
                "pay_type": "DAILY",
                "location": "Mumbai",
                "category": "driver",
                "vacancies": 3,
                "urgency": "REGULAR",
                "employer_id": "emp_004"
            }
        }
    ]
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        for test in test_cases:
            print(f"\n--- {test['name']} ---")
            try:
                response = await client.post(
                    f"{BASE_URL}/jobs/analyze",
                    json=test["data"]
                )
                result = response.json()
                print(f"Risk Score: {result.get('risk_score', 'N/A')}")
                print(f"Risk Level: {result.get('risk_level', 'N/A')}")
                print(f"Should Block: {result.get('should_block', 'N/A')}")
                print(f"Flags: {result.get('flags', [])}")
                print(f"Reasoning: {result.get('reasoning', 'N/A')[:100]}...")
            except Exception as e:
                print(f"Error: {e}")


async def test_fraud_detection():
    """Test fraud detection"""
    print("\n" + "=" * 50)
    print("FRAUD DETECTION TESTS")
    print("=" * 50)
    
    test_texts = [
        {
            "name": "WFH SCAM",
            "text": "Work from home and earn lakhs per month. Data entry job. Registration fee 500 only.",
            "context": "job_description"
        },
        {
            "name": "MLM SCAM",
            "text": "Join our network marketing team. Referral income unlimited. Investment required 10000.",
            "context": "job_description"
        },
        {
            "name": "LEGITIMATE",
            "text": "Looking for experienced cook for restaurant. 6 days work. Salary 18000 per month.",
            "context": "job_description"
        }
    ]
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        for test in test_texts:
            print(f"\n--- {test['name']} ---")
            try:
                response = await client.post(
                    f"{BASE_URL}/fraud/detect",
                    json={"text": test["text"], "context": test["context"]}
                )
                result = response.json()
                print(f"Is Fraud: {result.get('is_fraud', 'N/A')}")
                print(f"Probability: {result.get('fraud_probability', 'N/A')}%")
                print(f"Should Block: {result.get('should_block', 'N/A')}")
                print(f"Banned Keywords: {result.get('banned_keywords_found', [])}")
                print(f"Scam Types: {result.get('scam_types', [])}")
            except Exception as e:
                print(f"Error: {e}")


async def test_employer_scoring():
    """Test employer scoring"""
    print("\n" + "=" * 50)
    print("EMPLOYER SCORING TESTS")
    print("=" * 50)
    
    test_employers = [
        {
            "name": "GOOD EMPLOYER",
            "data": {
                "employer_id": "emp_good",
                "metrics": {
                    "employer_id": "emp_good",
                    "total_jobs_posted": 50,
                    "response_rate": 92,
                    "avg_response_time_minutes": 60,
                    "sla_misses_30d": 0,
                    "cancellations_30d": 1,
                    "worker_complaints": 0,
                    "instant_job_abuse_count": 0,
                    "account_age_days": 180,
                    "total_hires": 45,
                    "ghosting_incidents": 0
                }
            }
        },
        {
            "name": "BAD EMPLOYER",
            "data": {
                "employer_id": "emp_bad",
                "metrics": {
                    "employer_id": "emp_bad",
                    "total_jobs_posted": 30,
                    "response_rate": 25,
                    "avg_response_time_minutes": 2880,
                    "sla_misses_30d": 5,
                    "cancellations_30d": 8,
                    "worker_complaints": 3,
                    "instant_job_abuse_count": 2,
                    "account_age_days": 60,
                    "total_hires": 5,
                    "ghosting_incidents": 4
                }
            }
        }
    ]
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        for test in test_employers:
            print(f"\n--- {test['name']} ---")
            try:
                response = await client.post(
                    f"{BASE_URL}/employers/score",
                    json=test["data"]
                )
                result = response.json()
                print(f"Reliability Score: {result.get('reliability_score', 'N/A')}")
                print(f"Risk Level: {result.get('risk_level', 'N/A')}")
                print(f"Tier: {result.get('current_tier', 'N/A')}")
                print(f"Can Post Instant: {result.get('can_post_instant_jobs', 'N/A')}")
                print(f"Requires Deposit: {result.get('requires_deposit', 'N/A')}")
                print(f"Behavior Flags: {result.get('behavior_flags', [])}")
            except Exception as e:
                print(f"Error: {e}")


async def test_quick_keyword_check():
    """Test quick keyword check (no AI)"""
    print("\n" + "=" * 50)
    print("QUICK KEYWORD CHECK (Deterministic)")
    print("=" * 50)
    
    async with httpx.AsyncClient() as client:
        response = await client.post(
            f"{BASE_URL}/fraud/check-keywords",
            params={"text": "Work from home data entry job. Earn from home easily."}
        )
        print("Text: 'Work from home data entry job'")
        print(json.dumps(response.json(), indent=2))


async def test_pay_validation():
    """Test pay rate validation"""
    print("\n" + "=" * 50)
    print("PAY VALIDATION (Deterministic)")
    print("=" * 50)
    
    test_cases = [
        {"category": "delivery", "pay_amount": 500, "pay_type": "DAILY"},
        {"category": "delivery", "pay_amount": 5000, "pay_type": "DAILY"},  # Too high
        {"category": "security", "pay_amount": 100, "pay_type": "DAILY"},   # Too low
    ]
    
    async with httpx.AsyncClient() as client:
        for test in test_cases:
            response = await client.post(
                f"{BASE_URL}/rules/validate-pay",
                params=test
            )
            result = response.json()
            print(f"\n{test['category']} - ₹{test['pay_amount']}/{test['pay_type']}")
            print(f"Valid: {result.get('is_valid')} | Reason: {result.get('reason')}")


async def main():
    """Run all tests"""
    print("\n" + "=" * 60)
    print("   DUTYPE AI BACKEND - TEST SUITE")
    print("=" * 60)
    
    # Health check first
    healthy = await test_health()
    if not healthy:
        print("\n❌ Server not running! Start with: python main.py")
        return
    
    # Deterministic tests (no AI)
    await test_quick_keyword_check()
    await test_pay_validation()
    
    # AI-powered tests
    print("\n" + "=" * 60)
    print("   AI-POWERED TESTS (Azure OpenAI GPT-4o-mini)")
    print("=" * 60)
    
    await test_job_analysis()
    await test_fraud_detection()
    await test_employer_scoring()
    
    print("\n" + "=" * 60)
    print("   TESTS COMPLETE")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(main())
