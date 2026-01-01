"""
Deterministic Rules Engine for DutyPe
ENFORCEMENT happens here, NOT in AI.

This is the ONLY place where penalties are applied.
AI detects → Rules Engine enforces.
"""

from typing import Dict, Any, List, Optional, Tuple
from datetime import datetime, timedelta
import structlog
import re

from app.models.enforcement_models import (
    EnforcementAction,
    EnforcementResult,
    PenaltyType,
    EnforcementTrigger
)
from app.models.employer_models import EmployerTier

logger = structlog.get_logger()


# ============================================================
# BANNED KEYWORDS - INSTANT BLOCK (NO AI NEEDED)
# ============================================================
BANNED_KEYWORDS = {
    # WFH/Online Scams
    "work from home", "wfh", "work at home", "home based job",
    "online job", "online work", "online earning", "earn from home",
    "घर बैठे कमाएं", "घर से काम",
    
    # Data Entry Scams
    "data entry", "typing job", "copy paste", "form filling",
    "ad posting", "captcha entry", "survey job",
    
    # Investment/MLM Scams
    "investment required", "registration fee", "joining fee",
    "security deposit", "advance payment", "pay first",
    "network marketing", "mlm", "referral income",
    
    # Fake Urgency
    "limited slots", "only today", "urgent hiring 100+",
    
    # Contact Harvesting
    "whatsapp only", "telegram only", "call directly",
}

SUSPICIOUS_KEYWORDS = {
    "immediate joining", "spot joining", "walk-in",
    "no experience", "freshers only", "10th pass",
    "unlimited earning", "high income", "earn lakhs",
    "part time", "flexible hours", "own timing",
}


# ============================================================
# PAY RATE VALIDATION (Category-wise)
# ============================================================
PAY_LIMITS = {
    # category: (min_daily, max_daily, max_monthly)
    "delivery": (300, 1500, 35000),
    "driver": (400, 2000, 50000),
    "security": (350, 1000, 25000),
    "housekeeping": (300, 800, 20000),
    "cook": (400, 1500, 35000),
    "helper": (300, 800, 20000),
    "warehouse": (350, 1000, 25000),
    "retail": (350, 1200, 30000),
    "telecaller": (300, 1000, 25000),
    "default": (250, 2000, 50000),
}


# ============================================================
# PENALTY ESCALATION MATRIX
# ============================================================
PENALTY_MATRIX = {
    # (trigger, offense_count): (penalty_type, duration_hours, severity)
    (EnforcementTrigger.SLA_MISS, 1): (PenaltyType.WARNING, None, 1),
    (EnforcementTrigger.SLA_MISS, 2): (PenaltyType.VISIBILITY_REDUCTION, 72, 2),
    (EnforcementTrigger.SLA_MISS, 3): (PenaltyType.INSTANT_JOB_DISABLED, 168, 3),
    (EnforcementTrigger.SLA_MISS, 4): (PenaltyType.POSTING_COOLDOWN, 336, 4),
    (EnforcementTrigger.SLA_MISS, 5): (PenaltyType.TEMPORARY_SUSPENSION, 720, 5),
    
    (EnforcementTrigger.CANCELLATION, 1): (PenaltyType.WARNING, None, 1),
    (EnforcementTrigger.CANCELLATION, 2): (PenaltyType.VISIBILITY_REDUCTION, 48, 2),
    (EnforcementTrigger.CANCELLATION, 3): (PenaltyType.INSTANT_JOB_DISABLED, 168, 3),
    (EnforcementTrigger.CANCELLATION, 4): (PenaltyType.DEPOSIT_REQUIRED, None, 4),
    
    (EnforcementTrigger.GHOSTING, 1): (PenaltyType.VISIBILITY_REDUCTION, 48, 2),
    (EnforcementTrigger.GHOSTING, 2): (PenaltyType.INSTANT_JOB_DISABLED, 168, 3),
    (EnforcementTrigger.GHOSTING, 3): (PenaltyType.TEMPORARY_SUSPENSION, 336, 4),
    
    (EnforcementTrigger.FRAUD_DETECTED, 1): (PenaltyType.TEMPORARY_SUSPENSION, 720, 5),
    (EnforcementTrigger.FRAUD_DETECTED, 2): (PenaltyType.PERMANENT_BAN, None, 5),
    
    (EnforcementTrigger.INSTANT_JOB_ABUSE, 1): (PenaltyType.INSTANT_JOB_DISABLED, 168, 3),
    (EnforcementTrigger.INSTANT_JOB_ABUSE, 2): (PenaltyType.INSTANT_JOB_DISABLED, 720, 4),
    (EnforcementTrigger.INSTANT_JOB_ABUSE, 3): (PenaltyType.RUSH_JOB_DISABLED, None, 5),
}


class RulesEngine:
    """
    Deterministic Rules Engine
    
    CRITICAL: This is the ONLY component that applies penalties.
    AI provides recommendations, Rules Engine decides.
    """
    
    def __init__(self):
        self.banned_keywords = BANNED_KEYWORDS
        self.suspicious_keywords = SUSPICIOUS_KEYWORDS
        self.pay_limits = PAY_LIMITS
        self.penalty_matrix = PENALTY_MATRIX
    
    # ============================================================
    # KEYWORD CHECKING (DETERMINISTIC)
    # ============================================================
    
    def check_banned_keywords(self, text: str) -> Tuple[bool, List[str]]:
        """Check for banned keywords - instant block"""
        text_lower = text.lower()
        found = []
        for keyword in self.banned_keywords:
            if keyword in text_lower:
                found.append(keyword)
        return len(found) > 0, found
    
    def check_suspicious_keywords(self, text: str) -> List[str]:
        """Check for suspicious keywords - flag for review"""
        text_lower = text.lower()
        found = []
        for keyword in self.suspicious_keywords:
            if keyword in text_lower:
                found.append(keyword)
        return found
    
    # ============================================================
    # PAY VALIDATION (DETERMINISTIC)
    # ============================================================
    
    def validate_pay_rate(
        self, 
        category: str, 
        pay_amount: float, 
        pay_type: str
    ) -> Tuple[bool, str]:
        """
        Validate pay rate against category limits
        Returns: (is_valid, reason)
        """
        limits = self.pay_limits.get(category.lower(), self.pay_limits["default"])
        min_daily, max_daily, max_monthly = limits
        
        # Normalize to daily rate
        if pay_type == "HOURLY":
            daily_rate = pay_amount * 8
        elif pay_type == "DAILY":
            daily_rate = pay_amount
        elif pay_type == "WEEKLY":
            daily_rate = pay_amount / 6
        elif pay_type == "MONTHLY":
            daily_rate = pay_amount / 26
            if pay_amount > max_monthly:
                return False, f"Monthly pay ₹{pay_amount} exceeds limit ₹{max_monthly}"
        else:
            daily_rate = pay_amount
        
        if daily_rate < min_daily:
            return False, f"Daily rate ₹{daily_rate:.0f} below minimum ₹{min_daily}"
        if daily_rate > max_daily:
            return False, f"Daily rate ₹{daily_rate:.0f} exceeds maximum ₹{max_daily}"
        
        return True, "Pay rate valid"
    
    # ============================================================
    # VACANCY VALIDATION
    # ============================================================
    
    def validate_vacancies(self, vacancies: int, employer_tier: str) -> Tuple[bool, str]:
        """Validate vacancy count based on employer tier"""
        limits = {
            "NEW": 5,
            "STANDARD": 10,
            "TRUSTED": 25,
            "PREMIUM": 50,
            "RESTRICTED": 3,
        }
        max_vacancies = limits.get(employer_tier, 10)
        
        if vacancies > max_vacancies:
            return False, f"Vacancies {vacancies} exceeds limit {max_vacancies} for {employer_tier}"
        if vacancies > 20 and employer_tier not in ["TRUSTED", "PREMIUM"]:
            return False, "High vacancy count requires TRUSTED tier"
        
        return True, "Vacancy count valid"

    
    # ============================================================
    # PENALTY CALCULATION (DETERMINISTIC)
    # ============================================================
    
    def calculate_penalty(
        self,
        trigger: EnforcementTrigger,
        offense_count: int,
        employer_id: str
    ) -> Optional[EnforcementAction]:
        """
        Calculate penalty based on trigger and offense count
        Uses deterministic matrix - NO AI involvement
        """
        # Cap offense count at 5 for matrix lookup
        lookup_count = min(offense_count, 5)
        
        key = (trigger, lookup_count)
        if key not in self.penalty_matrix:
            # Default to highest penalty for that trigger
            for i in range(5, 0, -1):
                if (trigger, i) in self.penalty_matrix:
                    key = (trigger, i)
                    break
            else:
                return None
        
        penalty_type, duration_hours, severity = self.penalty_matrix[key]
        
        expires_at = None
        if duration_hours:
            expires_at = datetime.utcnow() + timedelta(hours=duration_hours)
        
        return EnforcementAction(
            action_id=f"act_{datetime.utcnow().timestamp()}",
            penalty_type=penalty_type,
            trigger=trigger,
            employer_id=employer_id,
            severity=severity,
            duration_hours=duration_hours,
            expires_at=expires_at,
            reason=f"{trigger.value} - offense #{offense_count}",
            evidence={"offense_count": offense_count}
        )
    
    # ============================================================
    # EMPLOYER EVALUATION (DETERMINISTIC)
    # ============================================================
    
    def evaluate_employer(
        self,
        employer_id: str,
        metrics: Dict[str, Any],
        ai_recommendation: Optional[Dict[str, Any]] = None
    ) -> EnforcementResult:
        """
        Evaluate employer and determine enforcement actions
        
        AI recommendation is ADVISORY only.
        Final decision is deterministic based on metrics.
        """
        actions = []
        current_penalties = []
        
        # Check SLA misses
        sla_misses = metrics.get("sla_misses_30d", 0)
        if sla_misses > 0:
            action = self.calculate_penalty(
                EnforcementTrigger.SLA_MISS,
                sla_misses,
                employer_id
            )
            if action:
                actions.append(action)
                current_penalties.append(action.penalty_type)
        
        # Check cancellations
        cancellations = metrics.get("cancellations_30d", 0)
        if cancellations > 0:
            action = self.calculate_penalty(
                EnforcementTrigger.CANCELLATION,
                cancellations,
                employer_id
            )
            if action:
                actions.append(action)
                current_penalties.append(action.penalty_type)
        
        # Check ghosting
        ghosting = metrics.get("ghosting_incidents", 0)
        if ghosting > 0:
            action = self.calculate_penalty(
                EnforcementTrigger.GHOSTING,
                ghosting,
                employer_id
            )
            if action:
                actions.append(action)
                current_penalties.append(action.penalty_type)
        
        # Check instant job abuse
        instant_abuse = metrics.get("instant_job_abuse_count", 0)
        if instant_abuse > 0:
            action = self.calculate_penalty(
                EnforcementTrigger.INSTANT_JOB_ABUSE,
                instant_abuse,
                employer_id
            )
            if action:
                actions.append(action)
                current_penalties.append(action.penalty_type)
        
        # Determine if enforcement needed
        should_enforce = len(actions) > 0
        
        # Calculate total offense count
        total_offenses = sla_misses + cancellations + ghosting + instant_abuse
        
        # Generate summary
        if should_enforce:
            summary = f"Enforcement required: {len(actions)} action(s) for {total_offenses} offense(s)"
        else:
            summary = "No enforcement required"
        
        logger.info(
            "employer_evaluation_complete",
            employer_id=employer_id,
            should_enforce=should_enforce,
            action_count=len(actions)
        )
        
        return EnforcementResult(
            employer_id=employer_id,
            should_enforce=should_enforce,
            actions=actions,
            current_penalties=list(set(current_penalties)),
            offense_count=total_offenses,
            is_escalation=total_offenses > 3,
            summary=summary
        )
    
    # ============================================================
    # JOB VALIDATION (DETERMINISTIC)
    # ============================================================
    
    def validate_job_posting(
        self,
        job_data: Dict[str, Any],
        employer_tier: str = "STANDARD"
    ) -> Dict[str, Any]:
        """
        Validate job posting against deterministic rules
        Returns validation result with any violations
        """
        violations = []
        should_block = False
        requires_review = False
        
        title = job_data.get("title", "")
        description = job_data.get("description", "")
        full_text = f"{title} {description}"
        
        # Check banned keywords
        has_banned, banned_found = self.check_banned_keywords(full_text)
        if has_banned:
            violations.append(f"BANNED_KEYWORDS: {', '.join(banned_found)}")
            should_block = True
        
        # Check suspicious keywords
        suspicious_found = self.check_suspicious_keywords(full_text)
        if suspicious_found:
            violations.append(f"SUSPICIOUS_KEYWORDS: {', '.join(suspicious_found)}")
            requires_review = True
        
        # Validate pay rate
        pay_valid, pay_reason = self.validate_pay_rate(
            job_data.get("category", "default"),
            job_data.get("pay_amount", 0),
            job_data.get("pay_type", "DAILY")
        )
        if not pay_valid:
            violations.append(f"PAY_VIOLATION: {pay_reason}")
            requires_review = True
        
        # Validate vacancies
        vac_valid, vac_reason = self.validate_vacancies(
            job_data.get("vacancies", 1),
            employer_tier
        )
        if not vac_valid:
            violations.append(f"VACANCY_VIOLATION: {vac_reason}")
            requires_review = True
        
        # Check for off-platform contact
        contact_patterns = [
            r"whatsapp", r"telegram", r"call\s+\d{10}",
            r"dm\s+me", r"message\s+directly"
        ]
        for pattern in contact_patterns:
            if re.search(pattern, full_text.lower()):
                violations.append("OFF_PLATFORM_CONTACT")
                should_block = True
                break
        
        return {
            "is_valid": len(violations) == 0,
            "violations": violations,
            "should_block": should_block,
            "requires_review": requires_review,
            "banned_keywords": banned_found if has_banned else [],
            "suspicious_keywords": suspicious_found
        }


# Global instance
rules_engine = RulesEngine()
