"""
Worker Chatbot Service
AI-powered assistant for workers
"""

from typing import Dict, Any, Optional
from openai import AzureOpenAI
import structlog

from app.config import settings

logger = structlog.get_logger()

WORKER_SYSTEM_PROMPT = """You are DutyPe's Worker Assistant - a helpful AI that protects blue-collar workers in India.

YOUR ROLE:
- Help workers understand job safety
- Explain application status clearly
- Warn about risky employers
- Guide workers on using DutyPe

CORE PRINCIPLES:
1. Worker time > Employer convenience
2. Always be honest about risks
3. Never hide employer red flags
4. Speak simply - many workers have basic education
5. Support Hindi/English mix (Hinglish)

TONE: Friendly, simple language, direct, protective of worker interests.

RESPONSE FORMAT:
- Keep responses short (2-4 sentences)
- Use emojis sparingly (✅ ⚠️ ❌)
- Always end with helpful next step"""


class WorkerChatbot:
    """Worker-focused AI Chatbot"""
    
    def __init__(self):
        self.client = None
        self._init_client()
    
    def _init_client(self):
        """Initialize Azure OpenAI client"""
        if settings.azure_openai_api_key and settings.azure_openai_endpoint:
            self.client = AzureOpenAI(
                api_key=settings.azure_openai_api_key,
                api_version=settings.azure_openai_api_version,
                azure_endpoint=settings.azure_openai_endpoint
            )
            logger.info("worker_chatbot_initialized")
        else:
            logger.warning("worker_chatbot_no_credentials")

    async def chat(
        self,
        message: str,
        worker_id: str,
        context: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Process worker chat message"""
        if not self.client:
            return {
                "success": False,
                "reply": "AI service not configured. Please contact support.",
                "worker_id": worker_id
            }
        
        try:
            context_str = self._build_context(context)
            
            response = self.client.chat.completions.create(
                model=settings.azure_openai_deployment_name,
                messages=[
                    {"role": "system", "content": WORKER_SYSTEM_PROMPT},
                    {"role": "user", "content": f"Worker asks: {message}\n\n{context_str}"}
                ],
                temperature=0.7,
                max_tokens=300
            )
            
            reply = response.choices[0].message.content
            logger.info("worker_chat_success", worker_id=worker_id)
            
            return {
                "success": True,
                "reply": reply,
                "worker_id": worker_id
            }
            
        except Exception as e:
            logger.error("worker_chat_error", error=str(e))
            return {
                "success": False,
                "reply": "Sorry, I'm having trouble right now. Please try again.",
                "error": str(e),
                "worker_id": worker_id
            }
    
    def _build_context(self, context: Optional[Dict[str, Any]]) -> str:
        """Build context string"""
        if not context:
            return ""
        
        parts = []
        if "job" in context:
            job = context["job"]
            parts.append(f"JOB: {job.get('title', 'N/A')} | Risk: {job.get('risk_score', 'N/A')}/100 | Level: {job.get('risk_level', 'N/A')}")
        
        if "employer" in context:
            emp = context["employer"]
            parts.append(f"EMPLOYER: Score {emp.get('reliability_score', 'N/A')}/100 | Response Rate: {emp.get('response_rate', 'N/A')}%")
        
        if "application" in context:
            app = context["application"]
            parts.append(f"APPLICATION: Status {app.get('status', 'N/A')} | Viewed: {app.get('viewed', False)}")
        
        return "\n".join(parts)

    async def check_job_safety(
        self,
        job_data: Dict[str, Any],
        employer_data: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Quick job safety check"""
        if not self.client:
            return {"success": False, "safety_check": "AI service not configured."}
        
        try:
            prompt = f"""Job safety check:
Title: {job_data.get('title', 'Unknown')}
Pay: ₹{job_data.get('pay_amount', 0)} {job_data.get('pay_type', '')}
Risk Score: {job_data.get('risk_score', 50)}/100

Give a 2-sentence safety assessment. Use ✅ safe, ⚠️ caution, ❌ avoid."""

            response = self.client.chat.completions.create(
                model=settings.azure_openai_deployment_name,
                messages=[
                    {"role": "system", "content": WORKER_SYSTEM_PROMPT},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.5,
                max_tokens=100
            )
            
            return {
                "success": True,
                "safety_check": response.choices[0].message.content,
                "risk_score": job_data.get('risk_score', 50)
            }
        except Exception as e:
            logger.error("job_safety_error", error=str(e))
            return {"success": False, "safety_check": f"Error: {str(e)}"}

    async def explain_application_status(
        self,
        application_data: Dict[str, Any],
        employer_data: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Explain application status"""
        if not self.client:
            return {"success": False, "explanation": "AI service not configured."}
        
        try:
            prompt = f"""Explain this application status simply:
Status: {application_data.get('status', 'unknown')}
Days Waiting: {application_data.get('days_waiting', 0)}
Employer Response Rate: {employer_data.get('response_rate', 'Unknown') if employer_data else 'Unknown'}%

Give simple explanation and what worker should do next."""

            response = self.client.chat.completions.create(
                model=settings.azure_openai_deployment_name,
                messages=[
                    {"role": "system", "content": WORKER_SYSTEM_PROMPT},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.6,
                max_tokens=150
            )
            
            return {
                "success": True,
                "explanation": response.choices[0].message.content,
                "status": application_data.get('status')
            }
        except Exception as e:
            logger.error("application_status_error", error=str(e))
            return {"success": False, "explanation": f"Error: {str(e)}"}


# Global instance
worker_chatbot = WorkerChatbot()
