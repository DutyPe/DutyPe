"""
Regional Scam Pattern Detection
India-specific fraud patterns in multiple languages
"""

from typing import Dict, Any, List, Tuple
import re
import structlog

logger = structlog.get_logger()


class RegionalPatterns:
    """
    Regional scam pattern detection for India
    
    Supports:
    - Hindi
    - Telugu
    - Tamil
    - Hinglish (Hindi + English mix)
    - Regional variations
    """
    
    # Hindi scam keywords
    HINDI_SCAM_KEYWORDS = {
        # WFH scams
        "घर बैठे": "WFH_SCAM",
        "घर से काम": "WFH_SCAM",
        "ऑनलाइन काम": "WFH_SCAM",
        "घर बैठे कमाएं": "WFH_SCAM",
        "घर बैठे पैसे": "WFH_SCAM",
        
        # Money scams
        "रजिस्ट्रेशन फीस": "REGISTRATION_FEE",
        "जॉइनिंग फीस": "REGISTRATION_FEE",
        "सिक्योरिटी डिपॉजिट": "ADVANCE_PAYMENT",
        "पहले पैसे": "ADVANCE_PAYMENT",
        "एडवांस": "ADVANCE_PAYMENT",
        
        # Fake urgency
        "तुरंत जॉइन": "FAKE_URGENCY",
        "आज ही": "FAKE_URGENCY",
        "सीमित सीटें": "FAKE_URGENCY",
        "जल्दी करें": "FAKE_URGENCY",
        
        # Unrealistic promises
        "लाखों कमाएं": "UNREALISTIC_PAY",
        "असीमित कमाई": "UNREALISTIC_PAY",
        "गारंटीड इनकम": "UNREALISTIC_PAY",
    }
    
    # Telugu scam keywords
    TELUGU_SCAM_KEYWORDS = {
        # WFH scams
        "ఇంటి నుండి పని": "WFH_SCAM",
        "ఆన్‌లైన్ పని": "WFH_SCAM",
        "ఇంట్లో కూర్చొని": "WFH_SCAM",
        
        # Money scams
        "రిజిస్ట్రేషన్ ఫీజు": "REGISTRATION_FEE",
        "జాయినింగ్ ఫీజు": "REGISTRATION_FEE",
        "అడ్వాన్స్": "ADVANCE_PAYMENT",
        
        # Fake urgency
        "వెంటనే జాయిన్": "FAKE_URGENCY",
        "ఈ రోజే": "FAKE_URGENCY",
    }
    
    # Tamil scam keywords
    TAMIL_SCAM_KEYWORDS = {
        # WFH scams
        "வீட்டிலிருந்து வேலை": "WFH_SCAM",
        "ஆன்லைன் வேலை": "WFH_SCAM",
        
        # Money scams
        "பதிவு கட்டணம்": "REGISTRATION_FEE",
        "முன்பணம்": "ADVANCE_PAYMENT",
    }
    
    # Hinglish patterns (common in India)
    HINGLISH_PATTERNS = {
        r"ghar\s*baithe": "WFH_SCAM",
        r"ghar\s*se\s*kaam": "WFH_SCAM",
        r"online\s*earning": "WFH_SCAM",
        r"paisa\s*kamao": "UNREALISTIC_PAY",
        r"registration\s*fee": "REGISTRATION_FEE",
        r"joining\s*fee": "REGISTRATION_FEE",
        r"advance\s*payment": "ADVANCE_PAYMENT",
        r"security\s*deposit": "ADVANCE_PAYMENT",
        r"turant\s*join": "FAKE_URGENCY",
        r"aaj\s*hi": "FAKE_URGENCY",
        r"limited\s*seat": "FAKE_URGENCY",
        r"whatsapp\s*pe": "OFF_PLATFORM",
        r"telegram\s*pe": "OFF_PLATFORM",
        r"direct\s*call": "OFF_PLATFORM",
    }
    
    # Regional salary expectations (daily rates in INR)
    REGIONAL_PAY_LIMITS = {
        "delhi": {"min": 400, "max": 2000},
        "mumbai": {"min": 450, "max": 2500},
        "bangalore": {"min": 400, "max": 2000},
        "hyderabad": {"min": 350, "max": 1800},
        "chennai": {"min": 350, "max": 1800},
        "kolkata": {"min": 300, "max": 1500},
        "pune": {"min": 350, "max": 1800},
        "ahmedabad": {"min": 300, "max": 1500},
        "tier2": {"min": 250, "max": 1200},
        "tier3": {"min": 200, "max": 1000},
        "default": {"min": 250, "max": 1500},
    }
    
    def __init__(self):
        self.all_keywords = {}
        self._compile_patterns()
    
    def _compile_patterns(self):
        """Compile all regional patterns"""
        self.all_keywords.update(self.HINDI_SCAM_KEYWORDS)
        self.all_keywords.update(self.TELUGU_SCAM_KEYWORDS)
        self.all_keywords.update(self.TAMIL_SCAM_KEYWORDS)
        
        # Compile Hinglish regex patterns
        self.hinglish_compiled = {
            re.compile(pattern, re.IGNORECASE): scam_type
            for pattern, scam_type in self.HINGLISH_PATTERNS.items()
        }
    
    def detect_regional_scams(self, text: str) -> Dict[str, Any]:
        """
        Detect scam patterns in regional languages
        
        Returns detected patterns and scam types
        """
        detected = []
        scam_types = set()
        
        # Check direct keyword matches
        text_lower = text.lower()
        for keyword, scam_type in self.all_keywords.items():
            if keyword in text or keyword.lower() in text_lower:
                detected.append({
                    "keyword": keyword,
                    "scam_type": scam_type,
                    "language": self._detect_language(keyword)
                })
                scam_types.add(scam_type)
        
        # Check Hinglish patterns
        for pattern, scam_type in self.hinglish_compiled.items():
            if pattern.search(text):
                match = pattern.search(text)
                detected.append({
                    "keyword": match.group(),
                    "scam_type": scam_type,
                    "language": "HINGLISH"
                })
                scam_types.add(scam_type)
        
        # Calculate risk score
        risk_score = min(100, len(detected) * 25)
        
        return {
            "detected_patterns": detected,
            "scam_types": list(scam_types),
            "risk_score": risk_score,
            "is_scam": risk_score >= 50,
            "languages_detected": list(set(d["language"] for d in detected))
        }
    
    def _detect_language(self, text: str) -> str:
        """Detect language of text"""
        # Hindi Unicode range
        if re.search(r'[\u0900-\u097F]', text):
            return "HINDI"
        # Telugu Unicode range
        if re.search(r'[\u0C00-\u0C7F]', text):
            return "TELUGU"
        # Tamil Unicode range
        if re.search(r'[\u0B80-\u0BFF]', text):
            return "TAMIL"
        return "ENGLISH"
    
    def validate_regional_pay(
        self,
        location: str,
        daily_rate: float
    ) -> Tuple[bool, str]:
        """
        Validate pay rate for a region
        
        Returns (is_valid, reason)
        """
        location_lower = location.lower()
        
        # Find matching region
        limits = self.REGIONAL_PAY_LIMITS["default"]
        for region, region_limits in self.REGIONAL_PAY_LIMITS.items():
            if region in location_lower:
                limits = region_limits
                break
        
        if daily_rate < limits["min"]:
            return False, f"Pay ₹{daily_rate}/day is below minimum ₹{limits['min']} for {location}"
        
        if daily_rate > limits["max"]:
            return False, f"Pay ₹{daily_rate}/day exceeds maximum ₹{limits['max']} for {location}"
        
        return True, "Pay rate valid for region"
    
    def get_regional_scam_trends(self) -> Dict[str, Any]:
        """Get regional scam pattern statistics"""
        return {
            "hindi_patterns": len(self.HINDI_SCAM_KEYWORDS),
            "telugu_patterns": len(self.TELUGU_SCAM_KEYWORDS),
            "tamil_patterns": len(self.TAMIL_SCAM_KEYWORDS),
            "hinglish_patterns": len(self.HINGLISH_PATTERNS),
            "total_patterns": len(self.all_keywords) + len(self.HINGLISH_PATTERNS),
            "supported_languages": ["Hindi", "Telugu", "Tamil", "Hinglish", "English"]
        }


# Global instance
regional_patterns = RegionalPatterns()
