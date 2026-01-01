"""
Learning Loop Service
Learns from worker reports and feedback to improve detection
"""

from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta
from collections import defaultdict
import structlog

logger = structlog.get_logger()


class LearningService:
    """
    Learning Loop for continuous improvement
    
    Learns from:
    - Worker reports
    - False positive feedback
    - Scam confirmations
    - Employer behavior patterns
    """
    
    def __init__(self):
        # Report storage
        self.reports: List[Dict[str, Any]] = []
        # Keyword frequency from reports
        self.reported_keywords: Dict[str, int] = defaultdict(int)
        # Employer report counts
        self.employer_reports: Dict[str, int] = defaultdict(int)
        # False positive tracking
        self.false_positives: List[Dict[str, Any]] = []
        # Confirmed scams
        self.confirmed_scams: List[Dict[str, Any]] = []
        # Pattern learning
        self.learned_patterns: Dict[str, float] = {}
    
    def submit_report(
        self,
        reporter_id: str,
        reporter_type: str,  # "worker" or "employer"
        target_type: str,    # "job", "employer", "worker"
        target_id: str,
        reason: str,
        description: str,
        evidence: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Submit a report for learning"""
        report = {
            "report_id": f"rep_{datetime.utcnow().timestamp()}",
            "reporter_id": reporter_id,
            "reporter_type": reporter_type,
            "target_type": target_type,
            "target_id": target_id,
            "reason": reason,
            "description": description,
            "evidence": evidence or {},
            "submitted_at": datetime.utcnow(),
            "status": "PENDING",
            "learned": False
        }
        
        self.reports.append(report)
        
        # Extract keywords for learning
        self._extract_keywords(description)
        
        # Track employer reports
        if target_type == "employer":
            self.employer_reports[target_id] += 1
        
        logger.info(
            "report_submitted",
            report_id=report["report_id"],
            target_type=target_type,
            reason=reason
        )
        
        return report
    
    def _extract_keywords(self, text: str):
        """Extract and count keywords from report text"""
        # Simple keyword extraction
        words = text.lower().split()
        scam_indicators = [
            "scam", "fraud", "fake", "money", "pay", "fee",
            "deposit", "advance", "whatsapp", "telegram",
            "wfh", "home", "online", "typing", "data entry",
            "ghost", "respond", "silent", "cancel"
        ]
        
        for word in words:
            for indicator in scam_indicators:
                if indicator in word:
                    self.reported_keywords[indicator] += 1
    
    def confirm_scam(
        self,
        job_id: Optional[str] = None,
        employer_id: Optional[str] = None,
        scam_type: str = "UNKNOWN",
        details: str = ""
    ) -> Dict[str, Any]:
        """Confirm a scam for learning"""
        confirmation = {
            "job_id": job_id,
            "employer_id": employer_id,
            "scam_type": scam_type,
            "details": details,
            "confirmed_at": datetime.utcnow()
        }
        
        self.confirmed_scams.append(confirmation)
        
        # Learn from confirmation
        self._learn_from_scam(confirmation)
        
        logger.info(
            "scam_confirmed",
            employer_id=employer_id,
            scam_type=scam_type
        )
        
        return confirmation
    
    def _learn_from_scam(self, scam: Dict[str, Any]):
        """Learn patterns from confirmed scam"""
        scam_type = scam.get("scam_type", "UNKNOWN")
        
        # Increase weight for this scam type
        if scam_type not in self.learned_patterns:
            self.learned_patterns[scam_type] = 1.0
        else:
            self.learned_patterns[scam_type] = min(
                2.0,
                self.learned_patterns[scam_type] + 0.1
            )
    
    def report_false_positive(
        self,
        detection_type: str,
        target_id: str,
        original_score: int,
        feedback: str
    ) -> Dict[str, Any]:
        """Report a false positive detection"""
        fp = {
            "detection_type": detection_type,
            "target_id": target_id,
            "original_score": original_score,
            "feedback": feedback,
            "reported_at": datetime.utcnow()
        }
        
        self.false_positives.append(fp)
        
        # Adjust learned patterns
        if detection_type in self.learned_patterns:
            self.learned_patterns[detection_type] = max(
                0.5,
                self.learned_patterns[detection_type] - 0.05
            )
        
        logger.info(
            "false_positive_reported",
            detection_type=detection_type,
            target_id=target_id
        )
        
        return fp
    
    def get_pattern_weights(self) -> Dict[str, float]:
        """Get current learned pattern weights"""
        return dict(self.learned_patterns)
    
    def get_high_risk_employers(self, min_reports: int = 3) -> List[Dict[str, Any]]:
        """Get employers with multiple reports"""
        high_risk = []
        
        for employer_id, count in self.employer_reports.items():
            if count >= min_reports:
                high_risk.append({
                    "employer_id": employer_id,
                    "report_count": count,
                    "risk_level": "HIGH" if count >= 5 else "MEDIUM"
                })
        
        return sorted(high_risk, key=lambda x: x["report_count"], reverse=True)
    
    def get_trending_keywords(self, top_n: int = 10) -> List[Dict[str, Any]]:
        """Get most reported keywords"""
        sorted_keywords = sorted(
            self.reported_keywords.items(),
            key=lambda x: x[1],
            reverse=True
        )
        
        return [
            {"keyword": k, "count": v}
            for k, v in sorted_keywords[:top_n]
        ]
    
    def get_learning_stats(self) -> Dict[str, Any]:
        """Get overall learning statistics"""
        return {
            "total_reports": len(self.reports),
            "confirmed_scams": len(self.confirmed_scams),
            "false_positives": len(self.false_positives),
            "unique_keywords": len(self.reported_keywords),
            "employers_reported": len(self.employer_reports),
            "learned_patterns": len(self.learned_patterns),
            "top_keywords": self.get_trending_keywords(5),
            "high_risk_employers": len(self.get_high_risk_employers())
        }


# Global instance
learning_service = LearningService()
