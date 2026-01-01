"""
Pydantic Models for DutyPe AI Backend
Request/Response schemas for API endpoints
"""

from .job_models import (
    JobAnalysisRequest,
    JobAnalysisResponse,
    JobRiskLevel
)
from .employer_models import (
    EmployerScoreRequest,
    EmployerScoreResponse,
    EmployerBehaviorMetrics
)
from .fraud_models import (
    FraudDetectionRequest,
    FraudDetectionResponse,
    ScamPattern
)
from .enforcement_models import (
    EnforcementAction,
    PenaltyType,
    EnforcementResult
)

__all__ = [
    "JobAnalysisRequest",
    "JobAnalysisResponse", 
    "JobRiskLevel",
    "EmployerScoreRequest",
    "EmployerScoreResponse",
    "EmployerBehaviorMetrics",
    "FraudDetectionRequest",
    "FraudDetectionResponse",
    "ScamPattern",
    "EnforcementAction",
    "PenaltyType",
    "EnforcementResult"
]
