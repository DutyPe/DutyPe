"""
Employer Chatbot Service
AI-powered assistant for employers
"""

from typing import Dict, Any, Optional, List
from openai import AzureOpenAI
import structlog

from app.config import settings

logger = structlog.get_logger()


EMPLOYER_SYSTEM_PROMPT = """You are DutyPe's Employer Assistant - an AI that helps employers understand and improve on the platform.

YOUR ROLE:
- Explain employer scores and what affects them
- Clarify why penalties were applied
- Give actionable improvement tips
- Explain platform rules clearly

CORE PRINCIPLES:
1. Worker time > Employer convenience (non-negotiable)
2. Be fair but firm about rules
3. Never apologize for worker-protection rules
4. Encourage good employer behavior

PENALTY SYSTEM:
- 1st offense: Warning
- 2nd offense: Visibility reduced (72 hours)
- 3rd offense: Instant jobs disabled (7 days)
- 4th offense: Posting cooldown (14 days)
- 5th offense: Temporary suspension (30 days)

RESPONSE FORMAT:
- Professional but not cold
- Use bullet points for lists
- Always include actionable next steps"""


class EmployerChatbot:
    """Employer-focused AI Chatbot"""
    
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
            logger.info("employer_chatbot_initialized")
        else:
            logger.warning("employer_chatbot_no_credentials")

    async def chat(
        self,
        message: str,
        employer_id: str,
        context: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Process employer chat message"""
        if not self.client:
            return {
                "success": False,
                "reply": "AI service not configured. Please contact support.",
                "employer_id": employer_id
            }
        
        try:
            context_str = self._build_context(context)
            
            response = self.client.chat.completions.create(
                model=settings.azure_openai_deployment_name,
                messages=[
                    {"role": "system", "content": EMPLOYER_SYSTEM_PROMPT},
                    {"role": "user", "content": f"Employer asks: {message}\n\n{context_str}"}
                ],
                temperature=0.6,
                max_tokens=400
            )
            
            reply = response.choices[0].message.content
            logger.info("employer_chat_success", employer_id=employer_id)
            
            return {
                "success": True,
                "reply": reply,
                "employer_id": employer_id
            }
            
        except Exception as e:
            logger.error("employer_chat_error", error=str(e))
            return {
                "success": False,
                "reply": "Sorry, I'm having trouble right now. Please try again.",
                "error": str(e),
                "employer_id": employer_id
            }
    
    def _build_context(self, context: Optional[Dict[str, Any]]) -> str:
        """Build context string"""
        if not context:
            return ""
        
        parts = []
        if "score" in context:
            s = context["score"]
            parts.append(f"SCORE: {s.get('reliability_score', 'N/A')}/100 | Tier: {s.get('tier', 'N/A')}")
        
        if "metrics" in context:
            m = context["metrics"]
            parts.append(f"METRICS: Response Rate {m.get('response_rate', 'N/A')}% | SLA Misses: {m.get('sla_misses_30d', 0)} | Cancellations: {m.get('cancellations_30d', 0)}")
        
        if "penalties" in context and context["penalties"]:
            penalties = context["penalties"]
            parts.append(f"ACTIVE PENALTIES: {len(penalties)}")
        
        return "\n".join(parts)

    async def explain_score(
        self,
        score_data: Dict[str, Any],
        metrics_data: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Explain employer score"""
        if not self.client:
            return {"success": False, "explanation": "AI service not configured."}
        
        try:
            prompt = f"""Explain this employer's score:
Score: {score_data.get('reliability_score', 50)}/100
Tier: {score_data.get('tier', 'STANDARD')}
Response Rate: {metrics_data.get('response_rate', 0)}%
SLA Misses: {metrics_data.get('sla_misses_30d', 0)}
Cancellations: {metrics_data.get('cancellations_30d', 0)}

Explain: 1) What score means 2) What's hurting it 3) How to improve"""

            response = self.client.chat.completions.create(
                model=settings.azure_openai_deployment_name,
                messages=[
                    {"role": "system", "content": EMPLOYER_SYSTEM_PROMPT},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.5,
                max_tokens=300
            )
            
            return {
                "success": True,
                "explanation": response.choices[0].message.content,
                "score": score_data.get('reliability_score', 50)
            }
        except Exception as e:
            logger.error("score_explanation_error", error=str(e))
            return {"success": False, "explanation": f"Error: {str(e)}"}

    async def explain_penalty(self, penalty_data: Dict[str, Any]) -> Dict[str, Any]:
        """Explain penalty"""
        if not self.client:
            return {"success": False, "explanation": "AI service not configured."}
        
        try:
            prompt = f"""Explain this penalty:
Type: {penalty_data.get('type', 'Unknown')}
Reason: {penalty_data.get('reason', 'Unknown')}
Duration: {penalty_data.get('duration_hours', 'N/A')} hours
Expires: {penalty_data.get('expires', 'N/A')}

Explain: 1) Why applied 2) When lifted 3) How to prevent future penalties. Be firm but fair."""

            response = self.client.chat.completions.create(
                model=settings.azure_openai_deployment_name,
                messages=[
                    {"role": "system", "content": EMPLOYER_SYSTEM_PROMPT},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.5,
                max_tokens=250
            )
            
            return {
                "success": True,
                "explanation": response.choices[0].message.content,
                "penalty_type": penalty_data.get('type')
            }
        except Exception as e:
            logger.error("penalty_explanation_error", error=str(e))
            return {"success": False, "explanation": f"Error: {str(e)}"}

    async def get_improvement_tips(
        self,
        score_data: Dict[str, Any],
        metrics_data: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Get improvement tips"""
        if not self.client:
            return {"success": False, "tips": "AI service not configured.", "weak_areas": []}
        
        try:
            weak_areas = []
            if metrics_data.get('response_rate', 100) < 70:
                weak_areas.append("Low response rate")
            if metrics_data.get('sla_misses_30d', 0) > 0:
                weak_areas.append("SLA misses")
            if metrics_data.get('cancellations_30d', 0) > 0:
                weak_areas.append("Cancellations")
            
            prompt = f"""Give improvement tips for this employer:
Score: {score_data.get('reliability_score', 50)}/100
Weak Areas: {', '.join(weak_areas) if weak_areas else 'None'}
Response Rate: {metrics_data.get('response_rate', 0)}%

Give 3-5 specific, actionable tips. Be encouraging but realistic."""

            response = self.client.chat.completions.create(
                model=settings.azure_openai_deployment_name,
                messages=[
                    {"role": "system", "content": EMPLOYER_SYSTEM_PROMPT},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.6,
                max_tokens=300
            )
            
            return {
                "success": True,
                "tips": response.choices[0].message.content,
                "weak_areas": weak_areas,
                "current_score": score_data.get('reliability_score', 50)
            }
        except Exception as e:
            logger.error("improvement_tips_error", error=str(e))
            return {"success": False, "tips": f"Error: {str(e)}", "weak_areas": []}


# Global instance
employer_chatbot = EmployerChatbot()
