"""
Azure OpenAI Service for DutyPe
GPT-4o-mini integration for job analysis and fraud detection
"""

from openai import AzureOpenAI
from typing import Dict, Any, Optional, List
import json
import structlog

from app.config import settings

logger = structlog.get_logger()


class AIService:
    """
    Core AI Service using Azure OpenAI GPT-4o-mini
    
    IMPORTANT: AI is for DETECTION only, not ENFORCEMENT.
    All penalties and actions are handled by the Rules Engine.
    """
    
    def __init__(self):
        self.client = AzureOpenAI(
            api_key=settings.azure_openai_api_key,
            api_version=settings.azure_openai_api_version,
            azure_endpoint=settings.azure_openai_endpoint
        )
        self.deployment_name = settings.azure_openai_deployment_name
        
        # System prompt for DutyPe AI
        self.system_prompt = """You are DutyPe's AI Trust Analyzer.

Your role is to analyze job postings and employer behavior to protect blue-collar workers in India.

CORE PRINCIPLES (NON-NEGOTIABLE):
1. Worker time > Employer convenience
2. Employer silence is unacceptable
3. Trust must be enforced with consequences
4. Detect fraud, scams, and time-wasting patterns

YOU MUST:
- Analyze job descriptions for scam indicators
- Detect fake urgency patterns
- Identify unrealistic pay rates
- Flag suspicious employer behavior
- Be direct and factual in assessments

YOU MUST NOT:
- Be polite at the cost of accuracy
- Ignore red flags
- Give benefit of doubt to suspicious patterns
- Recommend actions (only detect and score)

OUTPUT FORMAT:
Always respond in valid JSON format with:
- risk_score (0-100)
- risk_level (LOW/MEDIUM/HIGH/CRITICAL)
- flags (list of detected issues)
- reasoning (brief explanation)
- confidence (0-100)"""

    async def analyze_job_posting(self, job_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Analyze a job posting for fraud/scam indicators
        
        Args:
            job_data: Job posting data including title, description, pay, location
            
        Returns:
            Risk assessment with score, flags, and reasoning
        """
        try:
            prompt = f"""Analyze this job posting for fraud/scam indicators:

TITLE: {job_data.get('title', 'N/A')}
DESCRIPTION: {job_data.get('description', 'N/A')}
PAY: ₹{job_data.get('pay_amount', 'N/A')} {job_data.get('pay_type', '')}
LOCATION: {job_data.get('location', 'N/A')}
CATEGORY: {job_data.get('category', 'N/A')}
CONTACT: {job_data.get('contact_number', 'N/A')}
VACANCIES: {job_data.get('vacancies', 'N/A')}
URGENCY: {job_data.get('urgency', 'NORMAL')}

Check for:
1. WFH/Online/Data Entry scam keywords
2. Unrealistic pay for the category
3. Vague or copy-paste descriptions
4. Excessive urgency language
5. Off-platform contact requests
6. Advance payment hints
7. Too many vacancies (>20 suspicious)

Respond in JSON format."""

            response = self.client.chat.completions.create(
                model=self.deployment_name,
                messages=[
                    {"role": "system", "content": self.system_prompt},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.1,  # Low temperature for consistent analysis
                max_tokens=500,
                response_format={"type": "json_object"}
            )
            
            result = json.loads(response.choices[0].message.content)
            logger.info("job_analysis_complete", job_title=job_data.get('title'), risk_score=result.get('risk_score'))
            return result
            
        except Exception as e:
            logger.error("job_analysis_failed", error=str(e))
            return {
                "risk_score": 50,
                "risk_level": "MEDIUM",
                "flags": ["ANALYSIS_ERROR"],
                "reasoning": f"Analysis failed: {str(e)}",
                "confidence": 0
            }

    async def analyze_employer_behavior(self, employer_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Analyze employer behavior patterns
        
        Args:
            employer_data: Employer metrics including response times, cancellations, etc.
            
        Returns:
            Behavior assessment with reliability score
        """
        try:
            prompt = f"""Analyze this employer's behavior pattern:

EMPLOYER ID: {employer_data.get('employer_id', 'N/A')}
TOTAL JOBS POSTED: {employer_data.get('total_jobs', 0)}
RESPONSE RATE: {employer_data.get('response_rate', 0)}%
AVG RESPONSE TIME: {employer_data.get('avg_response_time_minutes', 'N/A')} minutes
SLA MISSES (30 days): {employer_data.get('sla_misses', 0)}
CANCELLATIONS (30 days): {employer_data.get('cancellations', 0)}
WORKER COMPLAINTS: {employer_data.get('complaints', 0)}
INSTANT JOB ABUSE: {employer_data.get('instant_job_abuse', False)}
ACCOUNT AGE (days): {employer_data.get('account_age_days', 0)}

Evaluate:
1. Is this employer reliable?
2. Are they wasting worker time?
3. Do they abuse instant jobs?
4. Should their privileges be restricted?

Respond in JSON with:
- reliability_score (0-100)
- risk_level (LOW/MEDIUM/HIGH/CRITICAL)
- behavior_flags (list)
- recommended_restrictions (list)
- reasoning"""

            response = self.client.chat.completions.create(
                model=self.deployment_name,
                messages=[
                    {"role": "system", "content": self.system_prompt},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.1,
                max_tokens=500,
                response_format={"type": "json_object"}
            )
            
            result = json.loads(response.choices[0].message.content)
            logger.info("employer_analysis_complete", employer_id=employer_data.get('employer_id'), reliability_score=result.get('reliability_score'))
            return result
            
        except Exception as e:
            logger.error("employer_analysis_failed", error=str(e))
            return {
                "reliability_score": 50,
                "risk_level": "MEDIUM",
                "behavior_flags": ["ANALYSIS_ERROR"],
                "recommended_restrictions": [],
                "reasoning": f"Analysis failed: {str(e)}"
            }

    async def detect_scam_patterns(self, text: str, context: str = "job_description") -> Dict[str, Any]:
        """
        Detect scam patterns in text content
        
        Args:
            text: Text to analyze
            context: Context type (job_description, message, etc.)
            
        Returns:
            Scam detection results
        """
        try:
            prompt = f"""Analyze this {context} for scam patterns common in India's blue-collar job market:

TEXT: {text}

KNOWN SCAM PATTERNS TO CHECK:
1. Work from home / WFH promises
2. Data entry / typing jobs
3. Online earning schemes
4. Investment requirements
5. Registration fees
6. Document collection scams
7. "Earn ₹50,000 from home" type claims
8. WhatsApp/Telegram group redirects
9. Fake company names
10. Unrealistic salary promises

Respond in JSON with:
- is_scam (boolean)
- scam_probability (0-100)
- detected_patterns (list)
- scam_type (if applicable)
- reasoning"""

            response = self.client.chat.completions.create(
                model=self.deployment_name,
                messages=[
                    {"role": "system", "content": self.system_prompt},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.1,
                max_tokens=400,
                response_format={"type": "json_object"}
            )
            
            result = json.loads(response.choices[0].message.content)
            logger.info("scam_detection_complete", is_scam=result.get('is_scam'), probability=result.get('scam_probability'))
            return result
            
        except Exception as e:
            logger.error("scam_detection_failed", error=str(e))
            return {
                "is_scam": False,
                "scam_probability": 0,
                "detected_patterns": ["DETECTION_ERROR"],
                "scam_type": None,
                "reasoning": f"Detection failed: {str(e)}"
            }
