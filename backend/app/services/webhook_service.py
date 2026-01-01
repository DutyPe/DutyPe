"""
Webhook Service
Send notifications to Android app and external services
"""

import httpx
from typing import Dict, Any, List, Optional
from datetime import datetime
import structlog
import asyncio

from app.config import settings

logger = structlog.get_logger()


class WebhookService:
    """
    Webhook service for sending notifications
    
    Sends to:
    - Android app (FCM)
    - External monitoring
    - Slack/Discord alerts
    """
    
    def __init__(self):
        self.webhooks: Dict[str, str] = {}
        self.pending_notifications: List[Dict[str, Any]] = []
    
    def register_webhook(self, name: str, url: str) -> bool:
        """Register a webhook endpoint"""
        self.webhooks[name] = url
        logger.info("webhook_registered", name=name)
        return True
    
    async def send_webhook(
        self,
        webhook_name: str,
        event_type: str,
        payload: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Send webhook notification"""
        if webhook_name not in self.webhooks:
            return {"success": False, "error": "Webhook not registered"}
        
        url = self.webhooks[webhook_name]
        
        data = {
            "event_type": event_type,
            "payload": payload,
            "timestamp": datetime.utcnow().isoformat(),
            "source": "dutype-ai-backend"
        }
        
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.post(url, json=data)
                
                logger.info(
                    "webhook_sent",
                    webhook=webhook_name,
                    event=event_type,
                    status=response.status_code
                )
                
                return {
                    "success": response.status_code < 400,
                    "status_code": response.status_code
                }
        except Exception as e:
            logger.error("webhook_failed", webhook=webhook_name, error=str(e))
            return {"success": False, "error": str(e)}
    
    async def notify_penalty(
        self,
        employer_id: str,
        penalty_type: str,
        reason: str,
        duration_hours: Optional[int] = None
    ) -> Dict[str, Any]:
        """Notify about penalty applied"""
        payload = {
            "employer_id": employer_id,
            "penalty_type": penalty_type,
            "reason": reason,
            "duration_hours": duration_hours,
            "action_required": True
        }
        
        # Queue for Android notification
        self.pending_notifications.append({
            "type": "PENALTY",
            "target": employer_id,
            "payload": payload
        })
        
        # Send to registered webhooks
        results = []
        for name in self.webhooks:
            result = await self.send_webhook(name, "PENALTY_APPLIED", payload)
            results.append({"webhook": name, **result})
        
        return {
            "notified": True,
            "webhook_results": results,
            "queued_for_push": True
        }
    
    async def notify_fraud_detected(
        self,
        job_id: Optional[str],
        employer_id: str,
        fraud_type: str,
        confidence: int
    ) -> Dict[str, Any]:
        """Notify about fraud detection"""
        payload = {
            "job_id": job_id,
            "employer_id": employer_id,
            "fraud_type": fraud_type,
            "confidence": confidence,
            "action": "BLOCKED" if confidence >= 85 else "FLAGGED"
        }
        
        results = []
        for name in self.webhooks:
            result = await self.send_webhook(name, "FRAUD_DETECTED", payload)
            results.append({"webhook": name, **result})
        
        return {"notified": True, "webhook_results": results}
    
    async def notify_worker_protection(
        self,
        worker_id: str,
        protection_level: str,
        risk_factors: List[str]
    ) -> Dict[str, Any]:
        """Notify about worker protection trigger"""
        payload = {
            "worker_id": worker_id,
            "protection_level": protection_level,
            "risk_factors": risk_factors
        }
        
        self.pending_notifications.append({
            "type": "WORKER_PROTECTION",
            "target": worker_id,
            "payload": payload
        })
        
        return {"queued": True, "protection_level": protection_level}
    
    async def notify_silence_detected(
        self,
        employer_id: str,
        application_id: str,
        silence_hours: float
    ) -> Dict[str, Any]:
        """Notify about employer silence"""
        payload = {
            "employer_id": employer_id,
            "application_id": application_id,
            "silence_hours": silence_hours,
            "warning": f"No response for {silence_hours:.1f} hours"
        }
        
        # Notify employer
        self.pending_notifications.append({
            "type": "SILENCE_WARNING",
            "target": employer_id,
            "payload": payload
        })
        
        return {"notified": True, "silence_hours": silence_hours}
    
    def get_pending_notifications(self, target_id: Optional[str] = None) -> List[Dict[str, Any]]:
        """Get pending notifications (for polling)"""
        if target_id:
            return [n for n in self.pending_notifications if n["target"] == target_id]
        return self.pending_notifications
    
    def clear_notifications(self, target_id: str) -> int:
        """Clear notifications for a target"""
        before = len(self.pending_notifications)
        self.pending_notifications = [
            n for n in self.pending_notifications
            if n["target"] != target_id
        ]
        return before - len(self.pending_notifications)

# Global instance
webhook_service = WebhookService()
