"""
Test script for DutyPe Chatbots
Tests Worker and Employer AI chatbots
"""

import asyncio
import httpx
import json

BASE_URL = "http://localhost:8000/api/v1/chat"


async def test_worker_chatbot():
    """Test worker chatbot conversations"""
    print("\n" + "=" * 60)
    print("   WORKER CHATBOT TESTS")
    print("=" * 60)
    
    test_messages = [
        {
            "name": "Job Safety Question",
            "message": "Is this job safe? The pay seems too good",
            "context": {
                "job": {
                    "title": "Data Entry Work",
                    "pay_amount": 50000,
                    "pay_type": "MONTHLY",
                    "risk_score": 85,
                    "risk_level": "HIGH"
                }
            }
        },
        {
            "name": "Application Status Query",
            "message": "Why hasn't the employer responded to my application?",
            "context": {
                "application": {
                    "status": "PENDING",
                    "applied_date": "2 days ago",
                    "viewed": True,
                    "days_waiting": 2
                },
                "employer": {
                    "response_rate": 35,
                    "ghosting_incidents": 3
                }
            }
        },
        {
            "name": "Employer Trust Question",
            "message": "Can I trust this employer?",
            "context": {
                "employer": {
                    "reliability_score": 45,
                    "response_rate": 40,
                    "tier": "RESTRICTED",
                    "ghosting_incidents": 4,
                    "sla_misses": 5
                }
            }
        },
        {
            "name": "Hindi Question",
            "message": "Yeh job safe hai kya? Mujhe travel karna padega",
            "context": {
                "job": {
                    "title": "Delivery Boy",
                    "location": "Mumbai",
                    "risk_score": 25,
                    "risk_level": "LOW"
                }
            }
        },
        {
            "name": "General Help",
            "message": "How do I know if a job is a scam?",
            "context": None
        }
    ]
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        for test in test_messages:
            print(f"\n--- {test['name']} ---")
            print(f"Worker: {test['message']}")
            try:
                response = await client.post(
                    f"{BASE_URL}/worker",
                    json={
                        "message": test["message"],
                        "user_id": "worker_test_001",
                        "context": test["context"]
                    }
                )
                result = response.json()
                print(f"AI: {result.get('reply', 'No response')}")
            except Exception as e:
                print(f"Error: {e}")


async def test_worker_job_safety():
    """Test job safety check endpoint"""
    print("\n" + "=" * 60)
    print("   JOB SAFETY CHECK")
    print("=" * 60)
    
    test_jobs = [
        {
            "name": "Safe Job",
            "job": {
                "title": "Delivery Boy - Swiggy",
                "pay_amount": 500,
                "pay_type": "DAILY",
                "risk_score": 15,
                "risk_level": "LOW"
            }
        },
        {
            "name": "Risky Job",
            "job": {
                "title": "Online Data Entry",
                "pay_amount": 40000,
                "pay_type": "MONTHLY",
                "risk_score": 90,
                "risk_level": "CRITICAL"
            },
            "employer": {
                "reliability_score": 30,
                "response_rate": 20
            }
        }
    ]
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        for test in test_jobs:
            print(f"\n--- {test['name']} ---")
            try:
                response = await client.post(
                    f"{BASE_URL}/worker/job-safety",
                    json={
                        "job": test["job"],
                        "employer": test.get("employer")
                    }
                )
                result = response.json()
                print(f"Safety Check: {result.get('safety_check', 'No response')}")
            except Exception as e:
                print(f"Error: {e}")


async def test_employer_chatbot():
    """Test employer chatbot conversations"""
    print("\n" + "=" * 60)
    print("   EMPLOYER CHATBOT TESTS")
    print("=" * 60)
    
    test_messages = [
        {
            "name": "Score Question",
            "message": "Why is my score so low?",
            "context": {
                "score": {
                    "reliability_score": 42,
                    "response_score": 35,
                    "trust_score": 50,
                    "tier": "RESTRICTED",
                    "risk_level": "HIGH"
                },
                "metrics": {
                    "response_rate": 30,
                    "avg_response_time_minutes": 2880,
                    "sla_misses_30d": 5,
                    "cancellations_30d": 3,
                    "ghosting_incidents": 2
                }
            }
        },
        {
            "name": "Penalty Question",
            "message": "Why can't I post instant jobs anymore?",
            "context": {
                "penalties": [
                    {
                        "type": "INSTANT_JOB_DISABLED",
                        "reason": "3rd SLA miss in 14 days",
                        "expires": "7 days"
                    }
                ],
                "privileges": {
                    "can_post_instant": False,
                    "can_post_rush": True,
                    "posting_limit": 5
                }
            }
        },
        {
            "name": "Improvement Question",
            "message": "How can I improve my employer rating?",
            "context": {
                "score": {
                    "reliability_score": 55,
                    "tier": "STANDARD"
                },
                "metrics": {
                    "response_rate": 60,
                    "sla_misses_30d": 2,
                    "cancellations_30d": 1
                }
            }
        },
        {
            "name": "Rules Question",
            "message": "What happens if I cancel a confirmed job?",
            "context": None
        }
    ]
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        for test in test_messages:
            print(f"\n--- {test['name']} ---")
            print(f"Employer: {test['message']}")
            try:
                response = await client.post(
                    f"{BASE_URL}/employer",
                    json={
                        "message": test["message"],
                        "user_id": "employer_test_001",
                        "context": test["context"]
                    }
                )
                result = response.json()
                print(f"AI: {result.get('reply', 'No response')}")
            except Exception as e:
                print(f"Error: {e}")


async def test_employer_score_explanation():
    """Test score explanation endpoint"""
    print("\n" + "=" * 60)
    print("   EMPLOYER SCORE EXPLANATION")
    print("=" * 60)
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            response = await client.post(
                f"{BASE_URL}/employer/explain-score",
                json={
                    "score": {
                        "reliability_score": 48,
                        "tier": "RESTRICTED"
                    },
                    "metrics": {
                        "response_rate": 40,
                        "sla_misses_30d": 4,
                        "cancellations_30d": 2,
                        "ghosting_incidents": 3
                    }
                }
            )
            result = response.json()
            print(f"Explanation:\n{result.get('explanation', 'No response')}")
        except Exception as e:
            print(f"Error: {e}")


async def test_employer_penalty_explanation():
    """Test penalty explanation endpoint"""
    print("\n" + "=" * 60)
    print("   PENALTY EXPLANATION")
    print("=" * 60)
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            response = await client.post(
                f"{BASE_URL}/employer/explain-penalty",
                json={
                    "penalty": {
                        "type": "POSTING_COOLDOWN",
                        "reason": "4th SLA miss in 30 days",
                        "trigger": "SLA_MISS",
                        "offense_count": 4,
                        "duration_hours": 336,
                        "expires": "14 days from now"
                    }
                }
            )
            result = response.json()
            print(f"Explanation:\n{result.get('explanation', 'No response')}")
        except Exception as e:
            print(f"Error: {e}")


async def test_improvement_tips():
    """Test improvement tips endpoint"""
    print("\n" + "=" * 60)
    print("   IMPROVEMENT TIPS")
    print("=" * 60)
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            response = await client.post(
                f"{BASE_URL}/employer/improvement-tips",
                json={
                    "score": {
                        "reliability_score": 55,
                        "tier": "STANDARD"
                    },
                    "metrics": {
                        "response_rate": 55,
                        "avg_response_time_minutes": 1440,
                        "sla_misses_30d": 2,
                        "cancellations_30d": 1,
                        "ghosting_incidents": 1
                    }
                }
            )
            result = response.json()
            print(f"Tips:\n{result.get('tips', 'No response')}")
            print(f"\nWeak Areas: {result.get('weak_areas', [])}")
        except Exception as e:
            print(f"Error: {e}")


async def main():
    """Run all chatbot tests"""
    print("\n" + "=" * 60)
    print("   DUTYPE CHATBOT TEST SUITE")
    print("=" * 60)
    
    # Check server health first
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get("http://localhost:8000/api/v1/health")
            if response.status_code != 200:
                print("\n❌ Server not healthy!")
                return
            print("\n✅ Server is running")
        except Exception as e:
            print(f"\n❌ Server not running! Start with: python main.py")
            print(f"Error: {e}")
            return
    
    # Run tests
    await test_worker_chatbot()
    await test_worker_job_safety()
    await test_employer_chatbot()
    await test_employer_score_explanation()
    await test_employer_penalty_explanation()
    await test_improvement_tips()
    
    print("\n" + "=" * 60)
    print("   CHATBOT TESTS COMPLETE")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(main())
