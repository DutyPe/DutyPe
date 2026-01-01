"""
Enforcement and penalty Pydantic models
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from enum import Enum
from datetime import datetime


class PenaltyType(str, Enum):
    WARNING = "WARNING"
    VISIBILITY_REDUCTION = "VISIBILITY_REDUCTION"
    POSTING_COOLDOWN = "POSTING_COOLDOWN"
    INSTANT_JOB_DISABLED = "INSTANT_JOB_DISABLED"
    RUSH_JOB_DISABLED = "RUSH_JOB_DISABLED"
    DEPOSIT_REQUIRED = "DEPOSIT_REQUIRED"
    POSTING_LIMIT = "POSTING_LIMIT"
    TEMPORARY_SUSPENSION = "TEMPORARY_SUSPENSION"
    PERMANENT_BAN = "PERMANENT_BAN"


class EnforcementTrigger(str, Enum):
    SLA_MISS = "SLA_MISS"
    CANCELLATION = "CANCELLATION"
    GHOSTING = "GHOSTING"
    FRAUD_DETECTED = "FRAUD_DETECTED"
    WORKER_COMPLAINT = "WORKER_COMPLAINT"
    INSTANT_JOB_ABUSE = "INSTANT_JOB_ABUSE"
    REPEATED_OFFENSE = "REPEATED_OFFENSE"
    SCAM_POSTING = "SCAM_POSTING"


class EnforcementAction(BaseModel):
    """Single enforcement action"""
    action_id: str
    penalty_type: PenaltyType
    trigger: EnforcementTrigger
    employer_id: str
    severity: int = Field(..., ge=1, le=5)
    
    # Duration (if applicable)
    duration_hours: Optional[int] = None
    expires_at: Optional[datetime] = None
    
    # Details
    reason: str
    evidence: Dict[str, Any] = {}
    
    # Metadata
    created_at: datetime = Field(default_factory=datetime.utcnow)
    created_by: str = "SYSTEM"
    is_active: bool = True
    
    # Override tracking (for audit)
    was_overridden: bool = False
    override_reason: Optional[str] = None
    override_by: Optional[str] = None


class EnforcementResult(BaseModel):
    """Result of enforcement evaluation"""
    employer_id: str
    should_enforce: bool = False
    actions: List[EnforcementAction] = []
    
    # Current state
    current_penalties: List[PenaltyType] = []
    offense_count: int = 0
    
    # Escalation info
    is_escalation: bool = False
    previous_offense_count: int = 0
    
    # Summary
    summary: str
    
    class Config:
        json_schema_extra = {
            "example": {
                "employer_id": "emp_123",
                "should_enforce": True,
                "actions": [
                    {
                        "action_id": "act_001",
                        "penalty_type": "INSTANT_JOB_DISABLED",
                        "trigger": "SLA_MISS",
                        "employer_id": "emp_123",
                        "severity": 3,
                        "duration_hours": 168,
                        "reason": "3rd SLA miss in 14 days"
                    }
                ],
                "offense_count": 3,
                "is_escalation": True,
                "summary": "Instant job privilege removed for 7 days"
            }
        }
