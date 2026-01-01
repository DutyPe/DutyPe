"""
Worker Loss Prevention Service
Predicts and prevents worker harm before it happens
"""

from typing import Dict, Any, List, Optional
from datetime import datetime
import structlog

logger = structlog.get_logger()


class WorkerProtection:
    """
    Worker Loss Prevention System
    
    Predicts harm BEFORE it happens:
    - Travel + low trust employer = block
    - Document requests early = flag
    - Payment requests = instant block
    - New worker + risky job = extra protection
    """
    
    # Risk factors and weights
    RISK_FACTORS = {
        "requires_travel": 15,
        "low_employer_trust": 25,
        "new_employer": 10,
        "document_request": 20,
        "payment_request": 50,
        "off_platform_contact": 30,
        "high_job_risk": 20,
        "employer_ghosting_history": 25,
        "new_worker": 10,
        "instant_job": 5,
    }
    
    # Protection levels
    PROTECTION_LEVELS = {
        "NONE": 0,
        "SOFT_WARNING": 30,
        "STRONG_WARNING": 50,
        "REQUIRE_CONFIRMATION": 70,
        "BLOCK": 85
    }
    
    def __init__(self):
        # Worker protection history
        self.worker_history: Dict[str, Dict[str, Any]] = {}
    
    def assess_risk(
        self,
        worker_id: str,
        job_data: Dict[str, Any],
        employer_data: Dict[str, Any],
        worker_data: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Assess risk for a worker applying to a job
        
        Returns protection recommendation
        """
        risk_score = 0
        risk_factors = []
        
        # Job factors
        if job_data.get("requires_travel", False):
            risk_score += self.RISK_FACTORS["requires_travel"]
            risk_factors.append("JOB_REQUIRES_TRAVEL")
        
        if job_data.get("risk_score", 0) >= 60:
            risk_score += self.RISK_FACTORS["high_job_risk"]
            risk_factors.append("HIGH_JOB_RISK_SCORE")
        
        if job_data.get("urgency") == "INSTANT":
            risk_score += self.RISK_FACTORS["instant_job"]
            risk_factors.append("INSTANT_JOB")
        
        # Employer factors
        employer_score = employer_data.get("reliability_score", 50)
        if employer_score < 40:
            risk_score += self.RISK_FACTORS["low_employer_trust"]
            risk_factors.append("LOW_EMPLOYER_TRUST")
        
        if employer_data.get("account_age_days", 365) < 30:
            risk_score += self.RISK_FACTORS["new_employer"]
            risk_factors.append("NEW_EMPLOYER")
        
        if employer_data.get("ghosting_incidents", 0) >= 2:
            risk_score += self.RISK_FACTORS["employer_ghosting_history"]
            risk_factors.append("EMPLOYER_GHOSTING_HISTORY")
        
        # Worker factors
        if worker_data:
            if worker_data.get("account_age_days", 365) < 14:
                risk_score += self.RISK_FACTORS["new_worker"]
                risk_factors.append("NEW_WORKER_EXTRA_PROTECTION")
        
        # Determine protection level
        protection_level = "NONE"
        for level, threshold in sorted(
            self.PROTECTION_LEVELS.items(),
            key=lambda x: x[1],
            reverse=True
        ):
            if risk_score >= threshold:
                protection_level = level
                break
        
        # Generate recommendation
        recommendation = self._generate_recommendation(
            protection_level,
            risk_factors,
            job_data,
            employer_data
        )
        
        result = {
            "worker_id": worker_id,
            "risk_score": min(100, risk_score),
            "risk_factors": risk_factors,
            "protection_level": protection_level,
            "recommendation": recommendation,
            "should_block": protection_level == "BLOCK",
            "should_warn": protection_level in ["SOFT_WARNING", "STRONG_WARNING"],
            "require_confirmation": protection_level == "REQUIRE_CONFIRMATION",
            "assessed_at": datetime.utcnow().isoformat()
        }
        
        # Store in history
        self.worker_history[worker_id] = result
        
        if protection_level != "NONE":
            logger.info(
                "worker_protection_triggered",
                worker_id=worker_id,
                protection_level=protection_level,
                risk_score=risk_score
            )
        
        return result
    
    def _generate_recommendation(
        self,
        level: str,
        factors: List[str],
        job_data: Dict[str, Any],
        employer_data: Dict[str, Any]
    ) -> str:
        """Generate human-readable recommendation"""
        if level == "NONE":
            return "This job appears safe. Proceed with normal caution."
        
        if level == "BLOCK":
            return "⛔ This job is blocked for your protection. The combination of risk factors is too high."
        
        if level == "REQUIRE_CONFIRMATION":
            return "⚠️ High risk detected. Please confirm you understand the risks before applying."
        
        if level == "STRONG_WARNING":
            warnings = []
            if "LOW_EMPLOYER_TRUST" in factors:
                warnings.append(f"Employer has low trust score ({employer_data.get('reliability_score', 0)}/100)")
            if "EMPLOYER_GHOSTING_HISTORY" in factors:
                warnings.append("Employer has history of not responding to workers")
            if "JOB_REQUIRES_TRAVEL" in factors:
                warnings.append("Job requires travel - verify location before going")
            
            return "⚠️ Caution advised: " + "; ".join(warnings)
        
        if level == "SOFT_WARNING":
            return "ℹ️ Some risk factors detected. Review job details carefully before applying."
        
        return "Proceed with caution."
    
    def check_document_request(
        self,
        worker_id: str,
        employer_id: str,
        document_type: str
    ) -> Dict[str, Any]:
        """
        Check if document request is suspicious
        
        Suspicious patterns:
        - Aadhaar/PAN before interview
        - Bank details before hiring
        - Multiple documents at once
        """
        suspicious_early = ["aadhaar", "pan", "bank", "passport"]
        
        is_suspicious = any(
            doc in document_type.lower()
            for doc in suspicious_early
        )
        
        result = {
            "is_suspicious": is_suspicious,
            "document_type": document_type,
            "warning": None
        }
        
        if is_suspicious:
            result["warning"] = (
                "⚠️ Be careful! Legitimate employers don't ask for "
                f"{document_type} before interview/hiring. This could be a scam."
            )
            logger.warning(
                "suspicious_document_request",
                worker_id=worker_id,
                employer_id=employer_id,
                document_type=document_type
            )
        
        return result
    
    def check_payment_request(
        self,
        worker_id: str,
        employer_id: str,
        amount: float,
        reason: str
    ) -> Dict[str, Any]:
        """
        Check payment request - ALWAYS suspicious
        
        Workers should NEVER pay employers
        """
        logger.warning(
            "payment_request_detected",
            worker_id=worker_id,
            employer_id=employer_id,
            amount=amount,
            reason=reason
        )
        
        return {
            "is_scam": True,
            "amount": amount,
            "reason": reason,
            "warning": (
                "🚨 SCAM ALERT! Legitimate jobs NEVER ask workers to pay money. "
                f"Do NOT pay ₹{amount} for '{reason}'. Report this employer immediately."
            ),
            "action": "BLOCK_AND_REPORT"
        }


# Global instance
worker_protection = WorkerProtection()
