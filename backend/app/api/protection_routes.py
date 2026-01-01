"""
Protection & Detection API Routes
Worker protection, silence detection, duplicates, multi-account
"""

from fastapi import APIRouter, HTTPException, Depends, Header
from pydantic import BaseModel, Field
from typing import Optional, Dict, Any, List
import structlog

from app.config import settings
from app.services.silence_detector import silence_detector
from app.services.duplicate_detector import duplicate_detector
from app.services.multi_account_detector import multi_account_detector
from app.services.worker_protection import worker_protection
from app.services.learning_service import learning_service
from app.services.regional_patterns import regional_patterns

logger = structlog.get_logger()

router = APIRouter(prefix="/api/v1/protection")


# Auth
async def verify_api_key(x_api_key: Optional[str] = Header(None)):
    if not settings.api_key:
        return True
    if x_api_key != settings.api_key:
        raise HTTPException(status_code=401, detail="Invalid API key")
    return True


# ============================================================
# SILENCE DETECTION
# ============================================================

class ApplicationViewRequest(BaseModel):
    application_id: str
    employer_id: str
    worker_id: str


@router.post("/silence/track-view")
async def track_application_view(
    request: ApplicationViewRequest,
    _: bool = Depends(verify_api_key)
):
    """Track when employer views an application"""
    result = silence_detector.track_view(
        request.application_id,
        request.employer_id,
        request.worker_id
    )
    return result


@router.post("/silence/track-response")
async def track_application_response(
    application_id: str,
    _: bool = Depends(verify_api_key)
):
    """Track when employer responds"""
    result = silence_detector.track_response(application_id)
    if not result:
        raise HTTPException(status_code=404, detail="Application not found")
    return result


@router.get("/silence/check/{application_id}")
async def check_silence(
    application_id: str,
    _: bool = Depends(verify_api_key)
):
    """Check silence status for application"""
    return silence_detector.check_silence(application_id)


@router.get("/silence/employers")
async def get_silent_employers(
    min_hours: int = 24,
    _: bool = Depends(verify_api_key)
):
    """Get all silent employers"""
    return silence_detector.get_silent_employers(min_hours)


@router.get("/silence/stats/{employer_id}")
async def get_employer_silence_stats(
    employer_id: str,
    _: bool = Depends(verify_api_key)
):
    """Get silence statistics for employer"""
    return silence_detector.get_employer_silence_stats(employer_id)


# ============================================================
# DUPLICATE DETECTION
# ============================================================

class DuplicateCheckRequest(BaseModel):
    job_id: str
    title: str
    description: str
    employer_id: str


@router.post("/duplicate/check")
async def check_duplicate(
    request: DuplicateCheckRequest,
    _: bool = Depends(verify_api_key)
):
    """Check if job is a duplicate"""
    return duplicate_detector.check_duplicate(
        request.job_id,
        request.title,
        request.description,
        request.employer_id
    )


@router.post("/duplicate/register")
async def register_job(
    request: DuplicateCheckRequest,
    _: bool = Depends(verify_api_key)
):
    """Register job for future duplicate checking"""
    fingerprint = duplicate_detector.register_job(
        request.job_id,
        request.title,
        request.description,
        request.employer_id
    )
    return {"fingerprint": fingerprint, "registered": True}


@router.get("/duplicate/stats/{employer_id}")
async def get_duplicate_stats(
    employer_id: str,
    _: bool = Depends(verify_api_key)
):
    """Get duplicate statistics for employer"""
    return duplicate_detector.get_employer_duplicate_stats(employer_id)


# ============================================================
# MULTI-ACCOUNT DETECTION
# ============================================================

class EmployerRegistrationRequest(BaseModel):
    employer_id: str
    phone: Optional[str] = None
    device_id: Optional[str] = None
    ip_address: Optional[str] = None
    email: Optional[str] = None


@router.post("/multi-account/register")
async def register_employer(
    request: EmployerRegistrationRequest,
    _: bool = Depends(verify_api_key)
):
    """Register employer for multi-account tracking"""
    return multi_account_detector.register_employer(
        request.employer_id,
        request.phone,
        request.device_id,
        request.ip_address,
        request.email
    )


@router.get("/multi-account/check/{employer_id}")
async def check_multi_account(
    employer_id: str,
    _: bool = Depends(verify_api_key)
):
    """Check if employer has multi-account signals"""
    return multi_account_detector.check_employer(employer_id)


@router.get("/multi-account/cluster/{employer_id}")
async def get_account_cluster(
    employer_id: str,
    _: bool = Depends(verify_api_key)
):
    """Get all linked accounts"""
    cluster = multi_account_detector.get_account_cluster(employer_id)
    return {"employer_id": employer_id, "cluster": cluster, "size": len(cluster)}


@router.post("/multi-account/report-fraud")
async def report_fraud(
    employer_id: str,
    reason: str,
    _: bool = Depends(verify_api_key)
):
    """Report fraud and propagate to linked accounts"""
    return multi_account_detector.report_fraud(employer_id, reason)


# ============================================================
# WORKER PROTECTION
# ============================================================

class WorkerRiskRequest(BaseModel):
    worker_id: str
    job: Dict[str, Any]
    employer: Dict[str, Any]
    worker: Optional[Dict[str, Any]] = None


@router.post("/worker/assess-risk")
async def assess_worker_risk(
    request: WorkerRiskRequest,
    _: bool = Depends(verify_api_key)
):
    """Assess risk for worker applying to job"""
    return worker_protection.assess_risk(
        request.worker_id,
        request.job,
        request.employer,
        request.worker
    )


@router.post("/worker/check-document")
async def check_document_request(
    worker_id: str,
    employer_id: str,
    document_type: str,
    _: bool = Depends(verify_api_key)
):
    """Check if document request is suspicious"""
    return worker_protection.check_document_request(
        worker_id,
        employer_id,
        document_type
    )


@router.post("/worker/check-payment")
async def check_payment_request(
    worker_id: str,
    employer_id: str,
    amount: float,
    reason: str,
    _: bool = Depends(verify_api_key)
):
    """Check payment request - ALWAYS suspicious"""
    return worker_protection.check_payment_request(
        worker_id,
        employer_id,
        amount,
        reason
    )


# ============================================================
# LEARNING SERVICE
# ============================================================

class ReportRequest(BaseModel):
    reporter_id: str
    reporter_type: str
    target_type: str
    target_id: str
    reason: str
    description: str
    evidence: Optional[Dict[str, Any]] = None


@router.post("/learning/report")
async def submit_report(
    request: ReportRequest,
    _: bool = Depends(verify_api_key)
):
    """Submit a report for learning"""
    return learning_service.submit_report(
        request.reporter_id,
        request.reporter_type,
        request.target_type,
        request.target_id,
        request.reason,
        request.description,
        request.evidence
    )


@router.post("/learning/confirm-scam")
async def confirm_scam(
    job_id: Optional[str] = None,
    employer_id: Optional[str] = None,
    scam_type: str = "UNKNOWN",
    details: str = "",
    _: bool = Depends(verify_api_key)
):
    """Confirm a scam for learning"""
    return learning_service.confirm_scam(job_id, employer_id, scam_type, details)


@router.post("/learning/false-positive")
async def report_false_positive(
    detection_type: str,
    target_id: str,
    original_score: int,
    feedback: str,
    _: bool = Depends(verify_api_key)
):
    """Report false positive"""
    return learning_service.report_false_positive(
        detection_type,
        target_id,
        original_score,
        feedback
    )


@router.get("/learning/stats")
async def get_learning_stats(_: bool = Depends(verify_api_key)):
    """Get learning statistics"""
    return learning_service.get_learning_stats()


@router.get("/learning/high-risk-employers")
async def get_high_risk_employers(
    min_reports: int = 3,
    _: bool = Depends(verify_api_key)
):
    """Get employers with multiple reports"""
    return learning_service.get_high_risk_employers(min_reports)


# ============================================================
# REGIONAL PATTERNS
# ============================================================

@router.post("/regional/detect")
async def detect_regional_scams(
    text: str,
    _: bool = Depends(verify_api_key)
):
    """Detect regional scam patterns"""
    return regional_patterns.detect_regional_scams(text)


@router.post("/regional/validate-pay")
async def validate_regional_pay(
    location: str,
    daily_rate: float,
    _: bool = Depends(verify_api_key)
):
    """Validate pay rate for region"""
    is_valid, reason = regional_patterns.validate_regional_pay(location, daily_rate)
    return {"is_valid": is_valid, "reason": reason}


@router.get("/regional/stats")
async def get_regional_stats(_: bool = Depends(verify_api_key)):
    """Get regional pattern statistics"""
    return regional_patterns.get_regional_scam_trends()