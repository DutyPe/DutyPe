"""
Employer Silence Detection Service
Tracks employers who view applications but don't respond
"""

from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta
import structlog

from app.config import settings

logger = structlog.get_logger()


class SilenceDetector:
    """
    Detects employer silence patterns
    
    Silence = Employer viewed application but didn't respond
    This is a PENALTY-WORTHY offense in DutyPe
    
    Principle: Employer silence is unacceptable
    """
    
    # Silence thresholds (in hours)
    SILENCE_WARNING = 12      # Warning after 12 hours
    SILENCE_PENALTY = 24      # Penalty after 24 hours
    SILENCE_SEVERE = 48       # Severe penalty after 48 hours
    
    def __init__(self):
        # In-memory tracking (use Firebase in production)
        self.tracking: Dict[str, Dict[str, Any]] = {}
    
    def track_view(
        self,
        application_id: str,
        employer_id: str,
        worker_id: str
    ) -> Dict[str, Any]:
        """Track when employer views an application"""
        now = datetime.utcnow()
        
        self.tracking[application_id] = {
            "application_id": application_id,
            "employer_id": employer_id,
            "worker_id": worker_id,
            "viewed_at": now,
            "responded_at": None,
            "is_silent": True,
            "silence_hours": 0
        }
        
        logger.info(
            "application_viewed",
            application_id=application_id,
            employer_id=employer_id
        )
        
        return self.tracking[application_id]
    
    def track_response(self, application_id: str) -> Optional[Dict[str, Any]]:
        """Track when employer responds"""
        if application_id not in self.tracking:
            return None
        
        now = datetime.utcnow()
        record = self.tracking[application_id]
        record["responded_at"] = now
        record["is_silent"] = False
        
        # Calculate response time
        if record["viewed_at"]:
            delta = now - record["viewed_at"]
            record["response_time_hours"] = delta.total_seconds() / 3600
        
        logger.info(
            "application_responded",
            application_id=application_id,
            response_time_hours=record.get("response_time_hours", 0)
        )
        
        return record
    
    def check_silence(self, application_id: str) -> Dict[str, Any]:
        """Check silence status for an application"""
        if application_id not in self.tracking:
            return {"found": False}
        
        record = self.tracking[application_id]
        
        if not record["is_silent"]:
            return {
                "found": True,
                "is_silent": False,
                "response_time_hours": record.get("response_time_hours", 0)
            }
        
        # Calculate current silence duration
        now = datetime.utcnow()
        if record["viewed_at"]:
            delta = now - record["viewed_at"]
            silence_hours = delta.total_seconds() / 3600
            record["silence_hours"] = silence_hours
            
            # Determine severity
            if silence_hours >= self.SILENCE_SEVERE:
                severity = "SEVERE"
            elif silence_hours >= self.SILENCE_PENALTY:
                severity = "PENALTY"
            elif silence_hours >= self.SILENCE_WARNING:
                severity = "WARNING"
            else:
                severity = "OK"
            
            return {
                "found": True,
                "is_silent": True,
                "silence_hours": round(silence_hours, 1),
                "severity": severity,
                "employer_id": record["employer_id"]
            }
        
        return {"found": True, "is_silent": True, "severity": "UNKNOWN"}
    
    def get_silent_employers(self, min_hours: int = 24) -> List[Dict[str, Any]]:
        """Get all employers who have been silent for min_hours"""
        now = datetime.utcnow()
        silent = []
        
        for app_id, record in self.tracking.items():
            if not record["is_silent"]:
                continue
            
            if record["viewed_at"]:
                delta = now - record["viewed_at"]
                hours = delta.total_seconds() / 3600
                
                if hours >= min_hours:
                    silent.append({
                        "application_id": app_id,
                        "employer_id": record["employer_id"],
                        "worker_id": record["worker_id"],
                        "silence_hours": round(hours, 1),
                        "viewed_at": record["viewed_at"].isoformat()
                    })
        
        return silent
    
    def get_employer_silence_stats(self, employer_id: str) -> Dict[str, Any]:
        """Get silence statistics for an employer"""
        total_views = 0
        total_responses = 0
        total_silent = 0
        total_silence_hours = 0
        
        for record in self.tracking.values():
            if record["employer_id"] != employer_id:
                continue
            
            total_views += 1
            
            if record["is_silent"]:
                total_silent += 1
                if record.get("silence_hours"):
                    total_silence_hours += record["silence_hours"]
            else:
                total_responses += 1
        
        response_rate = (total_responses / total_views * 100) if total_views > 0 else 0
        avg_silence = (total_silence_hours / total_silent) if total_silent > 0 else 0
        
        return {
            "employer_id": employer_id,
            "total_views": total_views,
            "total_responses": total_responses,
            "total_silent": total_silent,
            "response_rate": round(response_rate, 1),
            "avg_silence_hours": round(avg_silence, 1),
            "silence_score": min(100, total_silent * 10)  # Higher = worse
        }


# Global instance
silence_detector = SilenceDetector()
