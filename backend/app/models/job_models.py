"""
Job-related Pydantic models
"""

from pydantic import BaseModel, Field
from typing import List, Optional
from enum import Enum


class JobRiskLevel(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class PayType(str, Enum):
    HOURLY = "HOURLY"
    DAILY = "DAILY"
    WEEKLY = "WEEKLY"
    MONTHLY = "MONTHLY"


class JobType(str, Enum):
    INSTANT = "INSTANT"
    REGULAR = "REGULAR"
    RUSH = "RUSH"


class JobAnalysisRequest(BaseModel):
    """Request model for job analysis"""
    job_id: Optional[str] = None
    title: str = Field(..., min_length=3, max_length=200)
    description: str = Field(..., min_length=10, max_length=5000)
    pay_amount: float = Field(..., gt=0)
    pay_type: PayType
    location: str = Field(..., min_length=2)
    category: str
    contact_number: Optional[str] = None
    vacancies: int = Field(default=1, ge=1)
    urgency: JobType = JobType.REGULAR
    employer_id: str
    
    class Config:
        json_schema_extra = {
            "example": {
                "title": "Delivery Boy - Swiggy",
                "description": "Food delivery in Hyderabad. Bike required.",
                "pay_amount": 500,
                "pay_type": "DAILY",
                "location": "Hyderabad, Telangana",
                "category": "Delivery",
                "vacancies": 5,
                "urgency": "INSTANT",
                "employer_id": "emp_123"
            }
        }


class JobAnalysisResponse(BaseModel):
    """Response model for job analysis"""
    job_id: Optional[str] = None
    risk_score: int = Field(..., ge=0, le=100)
    risk_level: JobRiskLevel
    flags: List[str] = []
    reasoning: str
    confidence: int = Field(..., ge=0, le=100)
    should_block: bool = False
    requires_review: bool = False
    
    # Deterministic rule violations
    rule_violations: List[str] = []
    
    class Config:
        json_schema_extra = {
            "example": {
                "risk_score": 75,
                "risk_level": "HIGH",
                "flags": ["UNREALISTIC_PAY", "VAGUE_DESCRIPTION"],
                "reasoning": "Pay rate too high for category",
                "confidence": 85,
                "should_block": False,
                "requires_review": True,
                "rule_violations": []
            }
        }
