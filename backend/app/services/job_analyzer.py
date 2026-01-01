"""
Job Risk Analyzer Service
Combines AI detection + Rules Engine validation
"""

from typing import Dict, Any
import structlog

from app.services.ai_service import AIService
from app.services.rules_engine import rules_engine
from app.models.job_models import JobAnalysisRequest, JobAnalysisResponse, JobRiskLevel

logger = structlog.get_logger()


class JobAnalyzer:
    """
    Job Risk Analyzer
    
    Flow:
    1. Rules Engine checks deterministic rules (keywords, pay, etc.)
    2. If passes rules, AI analyzes for patterns
    3. Combined score determines final risk level
    
    IMPORTANT: Rules Engine has VETO power over AI.
    If rules say block, it's blocked regardless of AI.
    """
    
    def __init__(self):
        self.ai_service = AIService()
    
    async def analyze(
        self,
        request: JobAnalysisRequest,
        employer_tier: str = "STANDARD"
    ) -> JobAnalysisResponse:
        """
        Analyze job posting for risk
        
        Args:
            request: Job analysis request
            employer_tier: Employer's current tier
            
        Returns:
            Complete risk assessment
        """
        job_data = request.model_dump()
        
        # STEP 1: Deterministic rules check (FIRST)
        rules_result = rules_engine.validate_job_posting(job_data, employer_tier)
        
        # If rules say block, block immediately (no AI needed)
        if rules_result["should_block"]:
            logger.warning(
                "job_blocked_by_rules",
                job_title=request.title,
                violations=rules_result["violations"]
            )
            return JobAnalysisResponse(
                job_id=request.job_id,
                risk_score=100,
                risk_level=JobRiskLevel.CRITICAL,
                flags=["RULES_VIOLATION"] + rules_result["violations"],
                reasoning="Blocked by deterministic rules",
                confidence=100,
                should_block=True,
                requires_review=False,
                rule_violations=rules_result["violations"]
            )
        
        # STEP 2: AI analysis for pattern detection
        ai_result = await self.ai_service.analyze_job_posting(job_data)
        
        # STEP 3: Combine scores
        ai_risk_score = ai_result.get("risk_score", 50)
        
        # Add penalty for rule violations (even if not blocking)
        rule_penalty = len(rules_result["violations"]) * 15
        rule_penalty += len(rules_result["suspicious_keywords"]) * 10
        
        # Final score (capped at 100)
        final_score = min(100, ai_risk_score + rule_penalty)
        
        # Determine risk level
        if final_score >= 80:
            risk_level = JobRiskLevel.CRITICAL
            should_block = True
        elif final_score >= 60:
            risk_level = JobRiskLevel.HIGH
            should_block = False
        elif final_score >= 40:
            risk_level = JobRiskLevel.MEDIUM
            should_block = False
        else:
            risk_level = JobRiskLevel.LOW
            should_block = False
        
        # Combine flags
        all_flags = ai_result.get("flags", []) + rules_result["violations"]
        
        # Determine if review needed
        requires_review = (
            rules_result["requires_review"] or 
            risk_level in [JobRiskLevel.HIGH, JobRiskLevel.MEDIUM]
        )
        
        logger.info(
            "job_analysis_complete",
            job_title=request.title,
            risk_score=final_score,
            risk_level=risk_level.value,
            should_block=should_block
        )
        
        return JobAnalysisResponse(
            job_id=request.job_id,
            risk_score=final_score,
            risk_level=risk_level,
            flags=all_flags,
            reasoning=ai_result.get("reasoning", "Analysis complete"),
            confidence=ai_result.get("confidence", 70),
            should_block=should_block,
            requires_review=requires_review,
            rule_violations=rules_result["violations"]
        )
    
    async def quick_check(self, title: str, description: str) -> Dict[str, Any]:
        """
        Quick deterministic check without AI
        Use for real-time validation during job posting
        """
        full_text = f"{title} {description}"
        
        has_banned, banned = rules_engine.check_banned_keywords(full_text)
        suspicious = rules_engine.check_suspicious_keywords(full_text)
        
        return {
            "is_blocked": has_banned,
            "banned_keywords": banned,
            "suspicious_keywords": suspicious,
            "needs_ai_review": len(suspicious) > 0 and not has_banned
        }


# Global instance
job_analyzer = JobAnalyzer()
