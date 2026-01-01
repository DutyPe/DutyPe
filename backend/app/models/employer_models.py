"""
Employer-related Pydantic models
"""

from pydantic import BaseModel, Field
from typing import List, Optional
from enum import Enum
from datetime import datetime


class EmployerTier(str, Enum):
    NEW = "NEW"
    STANDARD = "STANDARD"
    TRUSTED = "TRUSTED"
    PREMIUM = "PREMIUM"
    RESTRICTED = "RESTRICTED"
    SUSPENDED = "SUSPENDED"


class EmployerBehaviorMetrics(BaseModel):
    """Employer behavior metrics for scoring"""
    employer_id: str
    total_jobs_posted: int = 0
    response_rate: float = Field(default=0, ge=0, le=100)
    avg_response_time_minutes: Optional[float] = None
    sla_misses_30d: int = 0
    cancellations_30d: int = 0
    worker_complaints: int = 0
    instant_job_abuse_count: int = 0
    account_age_days: int = 0
    total_hires: int = 0
    ghosting_incidents: int = 0
    
    class Config:
        json_schema_extra = {
            "example": {
                "employer_id": "emp_123",
                "total_jobs_posted": 25,
                "response_rate": 65.5,
                "avg_response_time_minutes": 180,
                "sla_misses_30d": 3,
                "cancellations_30d": 2,
                "worker_complaints": 1,
                "instant_job_abuse_count": 0,
                "account_age_days": 90,
                "total_hires": 15,
                "ghosting_incidents": 2
            }
        }


class EmployerScoreRequest(BaseModel):
    """Request model for employer scoring"""
    employer_id: str
    metrics: EmployerBehaviorMetrics
    include_history: bool = False


class EmployerScoreResponse(BaseModel):
    """Response model for employer scoring"""
    employer_id: str
    reliability_score: int = Field(..., ge=0, le=100)
    risk_level: str
    current_tier: EmployerTier
    behavior_flags: List[str] = []
    recommended_restrictions: List[str] = []
    reasoning: str
    
    # Specific scores
    response_score: int = Field(default=50, ge=0, le=100)
    reliability_score_detail: int = Field(default=50, ge=0, le=100)
    trust_score: int = Field(default=50, ge=0, le=100)
    
    # Privileges status
    can_post_instant_jobs: bool = True
    can_post_rush_jobs: bool = True
    posting_limit_per_day: int = 10
    requires_deposit: bool = False
    
    # Timestamps
    last_evaluated: datetime = Field(default_factory=datetime.utcnow)
    next_evaluation: Optional[datetime] = None
