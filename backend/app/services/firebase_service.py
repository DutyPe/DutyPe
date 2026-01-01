"""
Firebase Firestore Service
Store and retrieve scores, penalties, and fraud signals
"""

import firebase_admin
from firebase_admin import credentials, firestore
from typing import Dict, Any, Optional, List
from datetime import datetime, timedelta
import structlog

from app.config import settings

logger = structlog.get_logger()


class FirebaseService:
    """Firebase Firestore integration for DutyPe AI Backend"""
    
    def __init__(self):
        self.db = None
        self._init_firebase()
    
    def _init_firebase(self):
        """Initialize Firebase Admin SDK"""
        try:
            if not firebase_admin._apps:
                if settings.firebase_credentials_path:
                    cred = credentials.Certificate(settings.firebase_credentials_path)
                    firebase_admin.initialize_app(cred, {
                        'projectId': settings.firebase_project_id
                    })
                else:
                    # Use default credentials (for Cloud Run)
                    firebase_admin.initialize_app()
            
            self.db = firestore.client()
            logger.info("firebase_initialized")
        except Exception as e:
            logger.warning("firebase_init_failed", error=str(e))
            self.db = None
    
    # ============================================================
    # EMPLOYER SCORES
    # ============================================================
    
    async def save_employer_score(self, employer_id: str, score_data: Dict[str, Any]) -> bool:
        """Save employer score to Firestore"""
        if not self.db:
            return False
        
        try:
            doc_ref = self.db.collection('employer_scores').document(employer_id)
            score_data['updated_at'] = datetime.utcnow()
            score_data['employer_id'] = employer_id
            doc_ref.set(score_data, merge=True)
            logger.info("employer_score_saved", employer_id=employer_id)
            return True
        except Exception as e:
            logger.error("employer_score_save_failed", error=str(e))
            return False
    
    async def get_employer_score(self, employer_id: str) -> Optional[Dict[str, Any]]:
        """Get employer score from Firestore"""
        if not self.db:
            return None
        
        try:
            doc = self.db.collection('employer_scores').document(employer_id).get()
            if doc.exists:
                return doc.to_dict()
            return None
        except Exception as e:
            logger.error("employer_score_get_failed", error=str(e))
            return None
    
    # ============================================================
    # PENALTIES
    # ============================================================
    
    async def save_penalty(self, employer_id: str, penalty_data: Dict[str, Any]) -> str:
        """Save penalty to Firestore"""
        if not self.db:
            return ""
        
        try:
            penalty_data['employer_id'] = employer_id
            penalty_data['created_at'] = datetime.utcnow()
            penalty_data['is_active'] = True
            
            doc_ref = self.db.collection('penalties').add(penalty_data)
            penalty_id = doc_ref[1].id
            
            # Also update employer's active penalties
            emp_ref = self.db.collection('employer_scores').document(employer_id)
            emp_ref.set({
                'active_penalties': firestore.ArrayUnion([penalty_id])
            }, merge=True)
            
            logger.info("penalty_saved", employer_id=employer_id, penalty_id=penalty_id)
            return penalty_id
        except Exception as e:
            logger.error("penalty_save_failed", error=str(e))
            return ""
    
    async def get_active_penalties(self, employer_id: str) -> List[Dict[str, Any]]:
        """Get active penalties for employer"""
        if not self.db:
            return []
        
        try:
            penalties = self.db.collection('penalties')\
                .where('employer_id', '==', employer_id)\
                .where('is_active', '==', True)\
                .stream()
            
            return [p.to_dict() for p in penalties]
        except Exception as e:
            logger.error("penalties_get_failed", error=str(e))
            return []
    
    async def expire_penalties(self) -> int:
        """Expire penalties that have passed their expiry time"""
        if not self.db:
            return 0
        
        try:
            now = datetime.utcnow()
            expired = self.db.collection('penalties')\
                .where('is_active', '==', True)\
                .where('expires_at', '<=', now)\
                .stream()
            
            count = 0
            for penalty in expired:
                penalty.reference.update({'is_active': False})
                count += 1
            
            if count > 0:
                logger.info("penalties_expired", count=count)
            return count
        except Exception as e:
            logger.error("penalties_expire_failed", error=str(e))
            return 0
    
    # ============================================================
    # JOB RISK SCORES
    # ============================================================
    
    async def save_job_risk(self, job_id: str, risk_data: Dict[str, Any]) -> bool:
        """Save job risk score"""
        if not self.db:
            return False
        
        try:
            doc_ref = self.db.collection('job_risk_scores').document(job_id)
            risk_data['job_id'] = job_id
            risk_data['analyzed_at'] = datetime.utcnow()
            doc_ref.set(risk_data)
            return True
        except Exception as e:
            logger.error("job_risk_save_failed", error=str(e))
            return False
    
    # ============================================================
    # FRAUD SIGNALS
    # ============================================================
    
    async def save_fraud_signal(self, signal_data: Dict[str, Any]) -> str:
        """Save fraud detection signal"""
        if not self.db:
            return ""
        
        try:
            signal_data['detected_at'] = datetime.utcnow()
            doc_ref = self.db.collection('fraud_signals').add(signal_data)
            return doc_ref[1].id
        except Exception as e:
            logger.error("fraud_signal_save_failed", error=str(e))
            return ""
    
    # ============================================================
    # SILENCE TRACKING
    # ============================================================
    
    async def track_application_view(self, application_id: str, employer_id: str) -> bool:
        """Track when employer views an application"""
        if not self.db:
            return False
        
        try:
            doc_ref = self.db.collection('application_tracking').document(application_id)
            doc_ref.set({
                'application_id': application_id,
                'employer_id': employer_id,
                'viewed_at': datetime.utcnow(),
                'responded_at': None,
                'is_silent': True
            }, merge=True)
            return True
        except Exception as e:
            logger.error("application_view_track_failed", error=str(e))
            return False
    
    async def track_application_response(self, application_id: str) -> bool:
        """Track when employer responds to application"""
        if not self.db:
            return False
        
        try:
            doc_ref = self.db.collection('application_tracking').document(application_id)
            doc_ref.update({
                'responded_at': datetime.utcnow(),
                'is_silent': False
            })
            return True
        except Exception as e:
            logger.error("application_response_track_failed", error=str(e))
            return False
    
    async def get_silent_employers(self, hours: int = 24) -> List[Dict[str, Any]]:
        """Get employers who viewed but didn't respond within hours"""
        if not self.db:
            return []
        
        try:
            cutoff = datetime.utcnow() - timedelta(hours=hours)
            silent = self.db.collection('application_tracking')\
                .where('is_silent', '==', True)\
                .where('viewed_at', '<=', cutoff)\
                .stream()
            
            return [s.to_dict() for s in silent]
        except Exception as e:
            logger.error("silent_employers_get_failed", error=str(e))
            return []

# Global instance
firebase_service = FirebaseService()
