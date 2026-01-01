"""
Chat API Routes for Worker and Employer Chatbots
"""

from fastapi import APIRouter, HTTPException, Depends, Header
from pydantic import BaseModel, Field
from typing import Optional, Dict, Any, List
import structlog

from app.config import settings
from app.services.worker_chatbot import worker_chatbot
from app.services.employer_chatbot import employer_chatbot

logger = structlog.get_logger()

router = APIRouter(prefix="/api/v1/chat")


# ============================================================
# REQUEST/RESPONSE MODELS
# ============================================================

class ChatMessage(BaseModel):
    """Chat message request"""
    message: str = Field(..., min_length=1, max_length=1000)
    user_id: str
    context: Optional[Dict[str, Any]] = None


class ChatResponse(BaseModel):
    """Chat response"""
    success: bool
    reply: str
    user_id: str
    error: Optional[str] = None


class JobSafetyRequest(BaseModel):
    """Job safety check request"""
    job: Dict[str, Any]
    employer: Optional[Dict[str, Any]] = None


class ApplicationStatusRequest(BaseModel):
    """Application status explanation request"""
    application: Dict[str, Any]
    employer: Optional[Dict[str, Any]] = None


class ScoreExplanationRequest(BaseModel):
    """Score explanation request"""
    score: Dict[str, Any]
    metrics: Dict[str, Any]


class PenaltyExplanationRequest(BaseModel):
    """Penalty explanation request"""
    penalty: Dict[str, Any]


# ============================================================
# API KEY AUTHENTICATION
# ============================================================

async def verify_api_key(x_api_key: Optional[str] = Header(None)):
    """Verify API key"""
    if not settings.api_key:
        return True
    if x_api_key != settings.api_key:
        raise HTTPException(status_code=401, detail="Invalid API key")
    return True


# ============================================================
# WORKER CHATBOT ENDPOINTS
# ============================================================

@router.post("/worker", response_model=ChatResponse)
async def worker_chat(
    request: ChatMessage,
    _: bool = Depends(verify_api_key)
):
    """
    Worker chatbot endpoint
    
    Send a message and get AI response focused on worker needs:
    - Job safety questions
    - Application status
    - Employer warnings
    - Platform guidance
    """
    try:
        result = await worker_chatbot.chat(
            message=request.message,
            worker_id=request.user_id,
            context=request.context
        )
        return ChatResponse(
            success=result["success"],
            reply=result["reply"],
            user_id=request.user_id,
            error=result.get("error")
        )
    except Exception as e:
        logger.error("worker_chat_endpoint_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/worker/job-safety")
async def check_job_safety(
    request: JobSafetyRequest,
    _: bool = Depends(verify_api_key)
):
    """
    Quick job safety check for workers
    
    Returns simple safety assessment with risk indicators
    """
    try:
        result = await worker_chatbot.check_job_safety(
            job_data=request.job,
            employer_data=request.employer
        )
        return result
    except Exception as e:
        logger.error("job_safety_endpoint_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/worker/application-status")
async def explain_application_status(
    request: ApplicationStatusRequest,
    _: bool = Depends(verify_api_key)
):
    """
    Explain application status to worker
    
    Provides simple explanation of what the status means
    and what to expect next
    """
    try:
        result = await worker_chatbot.explain_application_status(
            application_data=request.application,
            employer_data=request.employer
        )
        return result
    except Exception as e:
        logger.error("application_status_endpoint_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))


# ============================================================
# EMPLOYER CHATBOT ENDPOINTS
# ============================================================

@router.post("/employer", response_model=ChatResponse)
async def employer_chat(
    request: ChatMessage,
    _: bool = Depends(verify_api_key)
):
    """
    Employer chatbot endpoint
    
    Send a message and get AI response focused on employer needs:
    - Score explanations
    - Penalty reasons
    - Improvement tips
    - Platform rules
    """
    try:
        result = await employer_chatbot.chat(
            message=request.message,
            employer_id=request.user_id,
            context=request.context
        )
        return ChatResponse(
            success=result["success"],
            reply=result["reply"],
            user_id=request.user_id,
            error=result.get("error")
        )
    except Exception as e:
        logger.error("employer_chat_endpoint_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/employer/explain-score")
async def explain_employer_score(
    request: ScoreExplanationRequest,
    _: bool = Depends(verify_api_key)
):
    """
    Explain employer score in detail
    
    Provides breakdown of what affects the score
    and how to improve
    """
    try:
        result = await employer_chatbot.explain_score(
            score_data=request.score,
            metrics_data=request.metrics
        )
        return result
    except Exception as e:
        logger.error("score_explanation_endpoint_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/employer/explain-penalty")
async def explain_employer_penalty(
    request: PenaltyExplanationRequest,
    _: bool = Depends(verify_api_key)
):
    """
    Explain why a penalty was applied
    
    Provides clear explanation of the penalty,
    when it expires, and how to prevent future penalties
    """
    try:
        result = await employer_chatbot.explain_penalty(
            penalty_data=request.penalty
        )
        return result
    except Exception as e:
        logger.error("penalty_explanation_endpoint_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/employer/improvement-tips")
async def get_improvement_tips(
    request: ScoreExplanationRequest,
    _: bool = Depends(verify_api_key)
):
    """
    Get personalized improvement tips
    
    Analyzes employer metrics and provides
    specific, actionable tips to improve score
    """
    try:
        result = await employer_chatbot.get_improvement_tips(
            score_data=request.score,
            metrics_data=request.metrics
        )
        return result
    except Exception as e:
        logger.error("improvement_tips_endpoint_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))
