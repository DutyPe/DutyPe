"""
FastAPI Routes for DutyPe AI Backend
"""

from fastapi import APIRouter, HTTPException, Depends, Header
from typing import Optional
import structlog

from app.config import settings
from app.models.job_models import JobAnalysisRequest, JobAnalysisResponse
from app.models.employer_models import EmployerScoreRequest, EmployerScoreResponse
from app.models.fraud_models import FraudDetectionRequest, FraudDetectionResponse
from app.services.job_analyzer import job_analyzer
from app.services.employer_scorer import employer_scorer
from app.services.fraud_detector import fraud_detector
from app.services.rules_engine import rules_engine

logger = structlog.get_logger()

router = APIRouter(prefix="/api/v1")


# ============================================================
# API KEY AUTHENTICATION
# ============================================================

async def verify_api_key(x_api_key: Optional[str] = Header(None)):
    """Verify API key for protected endpoints"""
    if not settings.api_key:
        return True  # No API key configured = development mode
    
    if x_api_key != settings.api_key:
        raise HTTPException(status_code=401, detail="Invalid API key")
    return True


# ============================================================
# HEALTH CHECK
# ============================================================

@router.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "dutype-ai-backend",
        "version": "1.0.0"
    }


# ============================================================
# JOB ANALYSIS ENDPOINTS
# ============================================================

@router.post("/jobs/analyze", response_model=JobAnalysisResponse)
async def analyze_job(
    request: JobAnalysisRequest,
    _: bool = Depends(verify_api_key)
):
    """
    Analyze a job posting for risk
    
    Returns risk score, flags, and whether to block/review
    """
    try:
        result = await job_analyzer.analyze(request)
        logger.info(
            "job_analyzed",
            job_id=request.job_id,
            risk_score=result.risk_score,
            should_block=result.should_block
        )
        return result
    except Exception as e:
        logger.error("job_analysis_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/jobs/quick-check")
async def quick_check_job(
    title: str,
    description: str,
    _: bool = Depends(verify_api_key)
):
    """
    Quick deterministic check for job posting
    Use during job creation for real-time feedback
    """
    try:
        result = await job_analyzer.quick_check(title, description)
        return result
    except Exception as e:
        logger.error("quick_check_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))


# ============================================================
# EMPLOYER SCORING ENDPOINTS
# ============================================================

@router.post("/employers/score", response_model=EmployerScoreResponse)
async def score_employer(
    request: EmployerScoreRequest,
    _: bool = Depends(verify_api_key)
):
    """
    Calculate employer reliability score
    
    Returns score, tier, and privilege status
    """
    try:
        result = await employer_scorer.score_employer(request)
        logger.info(
            "employer_scored",
            employer_id=request.employer_id,
            score=result.reliability_score,
            tier=result.current_tier.value
        )
        return result
    except Exception as e:
        logger.error("employer_scoring_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/employers/{employer_id}/privileges")
async def get_employer_privileges(
    employer_id: str,
    _: bool = Depends(verify_api_key)
):
    """
    Get current privileges for an employer
    Quick lookup without full scoring
    """
    # This would typically fetch from database
    # For now, return default privileges
    return {
        "employer_id": employer_id,
        "can_post_instant_jobs": True,
        "can_post_rush_jobs": True,
        "posting_limit_per_day": 10,
        "requires_deposit": False,
        "message": "Fetch from database in production"
    }



# ============================================================
# FRAUD DETECTION ENDPOINTS
# ============================================================

@router.post("/fraud/detect", response_model=FraudDetectionResponse)
async def detect_fraud(
    request: FraudDetectionRequest,
    _: bool = Depends(verify_api_key)
):
    """
    Detect fraud/scam patterns in text
    
    Returns fraud probability and detected patterns
    """
    try:
        result = await fraud_detector.detect(request)
        logger.info(
            "fraud_detected",
            is_fraud=result.is_fraud,
            probability=result.fraud_probability,
            should_block=result.should_block
        )
        return result
    except Exception as e:
        logger.error("fraud_detection_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/fraud/check-keywords")
async def check_keywords(
    text: str,
    _: bool = Depends(verify_api_key)
):
    """
    Quick keyword check (deterministic only)
    No AI involved - fast response
    """
    has_banned, banned = rules_engine.check_banned_keywords(text)
    suspicious = rules_engine.check_suspicious_keywords(text)
    
    return {
        "has_banned_keywords": has_banned,
        "banned_keywords": banned,
        "suspicious_keywords": suspicious,
        "should_block": has_banned
    }


# ============================================================
# RULES ENGINE ENDPOINTS
# ============================================================

@router.post("/rules/validate-job")
async def validate_job_rules(
    job_data: dict,
    employer_tier: str = "STANDARD",
    _: bool = Depends(verify_api_key)
):
    """
    Validate job against deterministic rules only
    No AI - pure rule-based validation
    """
    try:
        result = rules_engine.validate_job_posting(job_data, employer_tier)
        return result
    except Exception as e:
        logger.error("rule_validation_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/rules/validate-pay")
async def validate_pay_rate(
    category: str,
    pay_amount: float,
    pay_type: str,
    _: bool = Depends(verify_api_key)
):
    """
    Validate pay rate for a category
    """
    is_valid, reason = rules_engine.validate_pay_rate(category, pay_amount, pay_type)
    return {
        "is_valid": is_valid,
        "reason": reason,
        "category": category,
        "pay_amount": pay_amount,
        "pay_type": pay_type
    }


@router.get("/rules/banned-keywords")
async def get_banned_keywords(_: bool = Depends(verify_api_key)):
    """Get list of banned keywords"""
    return {
        "banned_keywords": list(rules_engine.banned_keywords),
        "suspicious_keywords": list(rules_engine.suspicious_keywords)
    }


# ============================================================
# ENFORCEMENT ENDPOINTS
# ============================================================

@router.post("/enforcement/evaluate")
async def evaluate_enforcement(
    employer_id: str,
    metrics: dict,
    _: bool = Depends(verify_api_key)
):
    """
    Evaluate employer for enforcement actions
    
    Returns recommended penalties based on behavior
    """
    try:
        result = rules_engine.evaluate_employer(employer_id, metrics)
        return result.model_dump()
    except Exception as e:
        logger.error("enforcement_evaluation_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))