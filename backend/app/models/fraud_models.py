"""
Fraud detection Pydantic models
"""

from pydantic import BaseModel, Field
from typing import List, Optional
from enum import Enum


class ScamType(str, Enum):
    WFH_SCAM = "WFH_SCAM"
    DATA_ENTRY_SCAM = "DATA_ENTRY_SCAM"
    INVESTMENT_SCAM = "INVESTMENT_SCAM"
    REGISTRATION_FEE = "REGISTRATION_FEE"
    DOCUMENT_HARVEST = "DOCUMENT_HARVEST"
    CONTACT_HARVEST = "CONTACT_HARVEST"
    FAKE_COMPANY = "FAKE_COMPANY"
    UNREALISTIC_PAY = "UNREALISTIC_PAY"
    ADVANCE_PAYMENT = "ADVANCE_PAYMENT"
    MULTI_LEVEL_MARKETING = "MLM"


class ScamPattern(BaseModel):
    """Individual scam pattern detected"""
    pattern_type: ScamType
    confidence: int = Field(..., ge=0, le=100)
    matched_text: Optional[str] = None
    severity: str = "MEDIUM"


class FraudDetectionRequest(BaseModel):
    """Request model for fraud detection"""
    text: str = Field(..., min_length=5, max_length=10000)
    context: str = "job_description"
    employer_id: Optional[str] = None
    check_duplicates: bool = True
    
    class Config:
        json_schema_extra = {
            "example": {
                "text": "Work from home data entry job. Earn 50000 per month.",
                "context": "job_description",
                "employer_id": "emp_123"
            }
        }


class FraudDetectionResponse(BaseModel):
    """Response model for fraud detection"""
    is_fraud: bool = False
    fraud_probability: int = Field(default=0, ge=0, le=100)
    detected_patterns: List[ScamPattern] = []
    scam_types: List[ScamType] = []
    reasoning: str
    
    # Action recommendations (for rules engine)
    should_block: bool = False
    should_flag: bool = False
    requires_manual_review: bool = False
    
    # Keyword matches (deterministic)
    banned_keywords_found: List[str] = []
    suspicious_keywords_found: List[str] = []
    
    class Config:
        json_schema_extra = {
            "example": {
                "is_fraud": True,
                "fraud_probability": 92,
                "detected_patterns": [
                    {
                        "pattern_type": "WFH_SCAM",
                        "confidence": 95,
                        "matched_text": "work from home",
                        "severity": "HIGH"
                    }
                ],
                "scam_types": ["WFH_SCAM", "DATA_ENTRY_SCAM"],
                "reasoning": "Classic WFH data entry scam pattern",
                "should_block": True,
                "banned_keywords_found": ["work from home", "data entry"]
            }
        }
