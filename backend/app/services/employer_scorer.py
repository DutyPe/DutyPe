"""
Employer Behavior Scoring Service
Tracks and scores employer reliability
"""

from typing import Dict, Any, Optional
from datetime import datetime
import structlog

from app.services.ai_service import AIService
from app.services.rules_engine import rules_engine
from app.models.employer_models import (
    EmployerScoreRequest,
    EmployerScoreResponse,
    EmployerBehaviorMetrics,
    EmployerTier
)

logger = structlog.get_logger()


# Tier thresholds
TIER_THRESHOLDS = {
    "PREMIUM": {"min_score": 90, "min_hires": 50, "min_age_days": 180},
    "TRUSTED": {"min_score": 75, "min_hires": 20, "min_age_days": 90},
    "STANDARD": {"min_score": 50, "min_hires": 5, "min_age_days": 30},
    "NEW": {"min_score": 0, "min_hires": 0, "min_age_days": 0},
}


class EmployerScorer:
    """
    Employer Behavior Scoring
    
    Scores are calculated from:
    1. Response metrics (deterministic)
    2. Compliance metrics (deterministic)
    3. AI behavior analysis (advisory)
    
    Final tier is ALWAYS deterministic based on score.
    """
    
    def __init__(self):
        self.ai_service = AIService()
    
    def calculate_response_score(self, metrics: EmployerBehaviorMetrics) -> int:
        """Calculate score based on response behavior"""
        score = 100
        
        # Response rate penalty
        if metrics.response_rate < 50:
            score -= 30
        elif metrics.response_rate < 70:
            score -= 15
        elif metrics.response_rate < 85:
            score -= 5
        
        # Response time penalty
        if metrics.avg_response_time_minutes:
            if metrics.avg_response_time_minutes > 1440:  # > 24 hours
                score -= 25
            elif metrics.avg_response_time_minutes > 480:  # > 8 hours
                score -= 15
            elif metrics.avg_response_time_minutes > 120:  # > 2 hours
                score -= 5
        
        return max(0, score)
    
    def calculate_reliability_score(self, metrics: EmployerBehaviorMetrics) -> int:
        """Calculate score based on reliability metrics"""
        score = 100
        
        # SLA misses (heavy penalty)
        score -= metrics.sla_misses_30d * 10
        
        # Cancellations
        score -= metrics.cancellations_30d * 8
        
        # Ghosting (heaviest penalty - worker time wasted)
        score -= metrics.ghosting_incidents * 15
        
        # Complaints
        score -= metrics.worker_complaints * 5
        
        # Instant job abuse
        score -= metrics.instant_job_abuse_count * 12
        
        return max(0, score)
    
    def calculate_trust_score(self, metrics: EmployerBehaviorMetrics) -> int:
        """Calculate trust score based on positive signals"""
        score = 50  # Start neutral
        
        # Account age bonus
        if metrics.account_age_days > 365:
            score += 20
        elif metrics.account_age_days > 180:
            score += 15
        elif metrics.account_age_days > 90:
            score += 10
        elif metrics.account_age_days > 30:
            score += 5
        
        # Hire count bonus
        if metrics.total_hires > 100:
            score += 25
        elif metrics.total_hires > 50:
            score += 20
        elif metrics.total_hires > 20:
            score += 15
        elif metrics.total_hires > 10:
            score += 10
        elif metrics.total_hires > 5:
            score += 5
        
        return min(100, score)
    
    def determine_tier(
        self,
        overall_score: int,
        metrics: EmployerBehaviorMetrics,
        has_active_penalties: bool = False
    ) -> EmployerTier:
        """Determine employer tier based on score and metrics"""
        
        # Active penalties = RESTRICTED
        if has_active_penalties:
            return EmployerTier.RESTRICTED
        
        # Check each tier from highest to lowest
        for tier_name, thresholds in TIER_THRESHOLDS.items():
            if (
                overall_score >= thresholds["min_score"] and
                metrics.total_hires >= thresholds["min_hires"] and
                metrics.account_age_days >= thresholds["min_age_days"]
            ):
                return EmployerTier(tier_name)
        
        return EmployerTier.NEW
    
    async def score_employer(
        self,
        request: EmployerScoreRequest
    ) -> EmployerScoreResponse:
        """
        Calculate comprehensive employer score
        
        Args:
            request: Scoring request with metrics
            
        Returns:
            Complete employer score response
        """
        metrics = request.metrics
        
        # STEP 1: Calculate deterministic scores
        response_score = self.calculate_response_score(metrics)
        reliability_score = self.calculate_reliability_score(metrics)
        trust_score = self.calculate_trust_score(metrics)
        
        # STEP 2: Get AI analysis (advisory only)
        ai_result = await self.ai_service.analyze_employer_behavior({
            "employer_id": metrics.employer_id,
            "total_jobs": metrics.total_jobs_posted,
            "response_rate": metrics.response_rate,
            "avg_response_time_minutes": metrics.avg_response_time_minutes,
            "sla_misses": metrics.sla_misses_30d,
            "cancellations": metrics.cancellations_30d,
            "complaints": metrics.worker_complaints,
            "instant_job_abuse": metrics.instant_job_abuse_count > 0,
            "account_age_days": metrics.account_age_days
        })
        
        # STEP 3: Calculate overall score (weighted)
        # Reliability is most important (worker protection)
        overall_score = int(
            reliability_score * 0.45 +
            response_score * 0.35 +
            trust_score * 0.20
        )
        
        # STEP 4: Check for enforcement actions
        enforcement = rules_engine.evaluate_employer(
            metrics.employer_id,
            metrics.model_dump()
        )
        
        # Only restrict if there are serious penalties (severity >= 3)
        has_serious_penalties = any(
            action.severity >= 3 for action in enforcement.actions
        ) if enforcement.actions else False
        
        # STEP 5: Determine tier
        tier = self.determine_tier(
            overall_score,
            metrics,
            has_active_penalties=has_serious_penalties
        )
        
        # STEP 6: Determine privileges
        can_post_instant = (
            tier not in [EmployerTier.RESTRICTED, EmployerTier.SUSPENDED, EmployerTier.NEW] and
            metrics.instant_job_abuse_count == 0
        )
        can_post_rush = tier not in [EmployerTier.RESTRICTED, EmployerTier.SUSPENDED]
        
        # Posting limits by tier
        posting_limits = {
            EmployerTier.PREMIUM: 20,
            EmployerTier.TRUSTED: 15,
            EmployerTier.STANDARD: 10,
            EmployerTier.NEW: 5,
            EmployerTier.RESTRICTED: 3,
            EmployerTier.SUSPENDED: 0
        }
        
        # Determine risk level
        if overall_score >= 75:
            risk_level = "LOW"
        elif overall_score >= 50:
            risk_level = "MEDIUM"
        elif overall_score >= 25:
            risk_level = "HIGH"
        else:
            risk_level = "CRITICAL"
        
        logger.info(
            "employer_scoring_complete",
            employer_id=metrics.employer_id,
            overall_score=overall_score,
            tier=tier.value,
            risk_level=risk_level
        )
        
        return EmployerScoreResponse(
            employer_id=metrics.employer_id,
            reliability_score=overall_score,
            risk_level=risk_level,
            current_tier=tier,
            behavior_flags=ai_result.get("behavior_flags", []),
            recommended_restrictions=ai_result.get("recommended_restrictions", []),
            reasoning=ai_result.get("reasoning", "Scoring complete"),
            response_score=response_score,
            reliability_score_detail=reliability_score,
            trust_score=trust_score,
            can_post_instant_jobs=can_post_instant,
            can_post_rush_jobs=can_post_rush,
            posting_limit_per_day=posting_limits.get(tier, 10),
            requires_deposit=tier == EmployerTier.RESTRICTED
        )


# Global instance
employer_scorer = EmployerScorer()
