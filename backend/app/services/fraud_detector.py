"""
Fraud Detection Service
Specialized scam and fraud pattern detection
"""

from typing import Dict, Any, List
import structlog
import re

from app.services.ai_service import AIService
from app.services.rules_engine import rules_engine
from app.models.fraud_models import (
    FraudDetectionRequest,
    FraudDetectionResponse,
    ScamPattern,
    ScamType
)

logger = structlog.get_logger()


# India-specific scam patterns (regex)
SCAM_PATTERNS = {
    ScamType.WFH_SCAM: [
        r"work\s*from\s*home",
        r"wfh",
        r"घर\s*बैठे",
        r"home\s*based",
        r"earn\s*from\s*home",
    ],
    ScamType.DATA_ENTRY_SCAM: [
        r"data\s*entry",
        r"typing\s*job",
        r"copy\s*paste",
        r"form\s*filling",
        r"captcha",
    ],
    ScamType.INVESTMENT_SCAM: [
        r"invest(ment)?\s*required",
        r"security\s*deposit",
        r"registration\s*fee",
        r"joining\s*fee",
        r"pay\s*first",
    ],
    ScamType.UNREALISTIC_PAY: [
        r"earn\s*₹?\s*\d{5,}",
        r"lakhs?\s*per\s*month",
        r"unlimited\s*earning",
        r"high\s*income",
    ],
    ScamType.CONTACT_HARVEST: [
        r"whatsapp\s*only",
        r"telegram\s*only",
        r"call\s*directly",
        r"dm\s*me",
        r"message\s*on\s*\d{10}",
    ],
    ScamType.MULTI_LEVEL_MARKETING: [
        r"network\s*marketing",
        r"mlm",
        r"referral\s*income",
        r"chain\s*marketing",
        r"pyramid",
    ],
    ScamType.FAKE_COMPANY: [
        r"mnc\s*company",
        r"top\s*company",
        r"reputed\s*company",
        r"international\s*company",
    ],
}


class FraudDetector:
    """
    Fraud Detection Service
    
    Two-layer detection:
    1. Deterministic pattern matching (fast, reliable)
    2. AI analysis for complex patterns (thorough)
    
    Deterministic rules have VETO power.
    """
    
    def __init__(self):
        self.ai_service = AIService()
        self.scam_patterns = SCAM_PATTERNS
    
    def detect_patterns_deterministic(self, text: str) -> List[ScamPattern]:
        """
        Detect scam patterns using regex (deterministic)
        Fast and reliable for known patterns
        """
        text_lower = text.lower()
        detected = []
        
        for scam_type, patterns in self.scam_patterns.items():
            for pattern in patterns:
                match = re.search(pattern, text_lower)
                if match:
                    detected.append(ScamPattern(
                        pattern_type=scam_type,
                        confidence=95,  # High confidence for regex match
                        matched_text=match.group(),
                        severity="HIGH" if scam_type in [
                            ScamType.INVESTMENT_SCAM,
                            ScamType.REGISTRATION_FEE,
                            ScamType.ADVANCE_PAYMENT
                        ] else "MEDIUM"
                    ))
                    break  # One match per type is enough
        
        return detected
    
    def calculate_fraud_probability(
        self,
        deterministic_patterns: List[ScamPattern],
        ai_probability: int
    ) -> int:
        """
        Calculate final fraud probability
        Deterministic patterns weigh more than AI
        """
        if not deterministic_patterns:
            return ai_probability
        
        # Base probability from deterministic detection
        det_probability = min(100, len(deterministic_patterns) * 30)
        
        # High severity patterns increase probability
        high_severity_count = sum(
            1 for p in deterministic_patterns if p.severity == "HIGH"
        )
        det_probability += high_severity_count * 20
        
        # Combine with AI (deterministic weighted higher)
        final = int(det_probability * 0.7 + ai_probability * 0.3)
        
        return min(100, final)
    
    async def detect(
        self,
        request: FraudDetectionRequest
    ) -> FraudDetectionResponse:
        """
        Detect fraud in text content
        
        Args:
            request: Fraud detection request
            
        Returns:
            Complete fraud detection response
        """
        text = request.text
        
        # STEP 1: Deterministic pattern detection
        det_patterns = self.detect_patterns_deterministic(text)
        
        # STEP 2: Check banned keywords
        has_banned, banned_keywords = rules_engine.check_banned_keywords(text)
        suspicious_keywords = rules_engine.check_suspicious_keywords(text)
        
        # STEP 3: AI analysis for complex patterns
        ai_result = await self.ai_service.detect_scam_patterns(
            text,
            request.context
        )
        
        # STEP 4: Combine results
        all_patterns = det_patterns.copy()
        
        # Add AI-detected patterns (if not already found)
        ai_patterns = ai_result.get("detected_patterns", [])
        existing_types = {p.pattern_type for p in det_patterns}
        
        for ai_pattern in ai_patterns:
            try:
                pattern_type = ScamType(ai_pattern) if isinstance(ai_pattern, str) else ai_pattern
                if pattern_type not in existing_types:
                    all_patterns.append(ScamPattern(
                        pattern_type=pattern_type,
                        confidence=ai_result.get("scam_probability", 50),
                        severity="MEDIUM"
                    ))
            except (ValueError, KeyError):
                pass  # Skip unknown pattern types
        
        # STEP 5: Calculate final probability
        ai_probability = ai_result.get("scam_probability", 0)
        final_probability = self.calculate_fraud_probability(
            det_patterns,
            ai_probability
        )
        
        # STEP 6: Determine actions
        is_fraud = final_probability >= 70 or has_banned
        should_block = final_probability >= 85 or has_banned
        should_flag = final_probability >= 50 and not should_block
        requires_review = 40 <= final_probability < 70
        
        # Extract scam types
        scam_types = list(set(p.pattern_type for p in all_patterns))
        
        # Generate reasoning
        if is_fraud:
            reasoning = f"Fraud detected with {final_probability}% confidence. "
            if det_patterns:
                reasoning += f"Patterns: {', '.join(p.pattern_type.value for p in det_patterns)}"
        else:
            reasoning = ai_result.get("reasoning", "No fraud detected")
        
        logger.info(
            "fraud_detection_complete",
            is_fraud=is_fraud,
            probability=final_probability,
            pattern_count=len(all_patterns),
            should_block=should_block
        )
        
        return FraudDetectionResponse(
            is_fraud=is_fraud,
            fraud_probability=final_probability,
            detected_patterns=all_patterns,
            scam_types=scam_types,
            reasoning=reasoning,
            should_block=should_block,
            should_flag=should_flag,
            requires_manual_review=requires_review,
            banned_keywords_found=banned_keywords,
            suspicious_keywords_found=suspicious_keywords
        )


# Global instance
fraud_detector = FraudDetector()
