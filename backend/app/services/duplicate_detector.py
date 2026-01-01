"""
Duplicate Job Detection Service
Detects copy-paste and repeated job postings
"""

from typing import Dict, Any, List, Optional, Tuple
from datetime import datetime, timedelta
import hashlib
import re
import structlog

logger = structlog.get_logger()


class DuplicateDetector:
    """
    Detects duplicate and copy-paste job postings
    
    Patterns detected:
    - Exact duplicates (same text)
    - Near duplicates (similar text)
    - Repeated postings (same employer, same job)
    - Cross-employer duplicates (scam templates)
    """
    
    # Similarity threshold (0-1)
    SIMILARITY_THRESHOLD = 0.85
    
    def __init__(self):
        # Job fingerprints: hash -> job_data
        self.fingerprints: Dict[str, Dict[str, Any]] = {}
        # Employer job history: employer_id -> [job_hashes]
        self.employer_history: Dict[str, List[str]] = {}
    
    def _normalize_text(self, text: str) -> str:
        """Normalize text for comparison"""
        # Lowercase
        text = text.lower()
        # Remove extra whitespace
        text = re.sub(r'\s+', ' ', text)
        # Remove special characters
        text = re.sub(r'[^\w\s]', '', text)
        # Remove numbers (phone numbers, salaries vary)
        text = re.sub(r'\d+', '', text)
        return text.strip()
    
    def _generate_fingerprint(self, title: str, description: str) -> str:
        """Generate fingerprint hash for job"""
        normalized = self._normalize_text(f"{title} {description}")
        return hashlib.md5(normalized.encode()).hexdigest()
    
    def _calculate_similarity(self, text1: str, text2: str) -> float:
        """Calculate Jaccard similarity between two texts"""
        words1 = set(self._normalize_text(text1).split())
        words2 = set(self._normalize_text(text2).split())
        
        if not words1 or not words2:
            return 0.0
        
        intersection = words1.intersection(words2)
        union = words1.union(words2)
        
        return len(intersection) / len(union)
    
    def check_duplicate(
        self,
        job_id: str,
        title: str,
        description: str,
        employer_id: str
    ) -> Dict[str, Any]:
        """
        Check if job is a duplicate
        
        Returns:
            - is_duplicate: bool
            - duplicate_type: EXACT | NEAR | REPEATED | TEMPLATE
            - original_job_id: if duplicate found
            - similarity: 0-1 score
        """
        fingerprint = self._generate_fingerprint(title, description)
        full_text = f"{title} {description}"
        
        result = {
            "is_duplicate": False,
            "duplicate_type": None,
            "original_job_id": None,
            "similarity": 0.0,
            "fingerprint": fingerprint
        }
        
        # Check exact duplicate
        if fingerprint in self.fingerprints:
            original = self.fingerprints[fingerprint]
            result["is_duplicate"] = True
            result["duplicate_type"] = "EXACT"
            result["original_job_id"] = original.get("job_id")
            result["similarity"] = 1.0
            
            # Same employer = REPEATED, different = TEMPLATE
            if original.get("employer_id") == employer_id:
                result["duplicate_type"] = "REPEATED"
            else:
                result["duplicate_type"] = "TEMPLATE"
            
            logger.warning(
                "duplicate_detected",
                job_id=job_id,
                duplicate_type=result["duplicate_type"],
                original_job_id=result["original_job_id"]
            )
            return result
        
        # Check near duplicates
        for fp, job_data in self.fingerprints.items():
            if fp == fingerprint:
                continue
            
            original_text = f"{job_data.get('title', '')} {job_data.get('description', '')}"
            similarity = self._calculate_similarity(full_text, original_text)
            
            if similarity >= self.SIMILARITY_THRESHOLD:
                result["is_duplicate"] = True
                result["duplicate_type"] = "NEAR"
                result["original_job_id"] = job_data.get("job_id")
                result["similarity"] = round(similarity, 2)
                
                if job_data.get("employer_id") == employer_id:
                    result["duplicate_type"] = "REPEATED"
                elif similarity > 0.95:
                    result["duplicate_type"] = "TEMPLATE"
                
                logger.warning(
                    "near_duplicate_detected",
                    job_id=job_id,
                    similarity=similarity
                )
                return result
        
        # Check employer repeat posting frequency
        if employer_id in self.employer_history:
            recent_count = len(self.employer_history[employer_id])
            if recent_count >= 5:
                # Check if posting similar jobs repeatedly
                for old_fp in self.employer_history[employer_id][-5:]:
                    if old_fp in self.fingerprints:
                        old_job = self.fingerprints[old_fp]
                        old_text = f"{old_job.get('title', '')} {old_job.get('description', '')}"
                        sim = self._calculate_similarity(full_text, old_text)
                        if sim >= 0.7:
                            result["is_duplicate"] = True
                            result["duplicate_type"] = "REPEATED"
                            result["similarity"] = round(sim, 2)
                            result["original_job_id"] = old_job.get("job_id")
                            return result
        
        return result
    
    def register_job(
        self,
        job_id: str,
        title: str,
        description: str,
        employer_id: str
    ) -> str:
        """Register a job for future duplicate checking"""
        fingerprint = self._generate_fingerprint(title, description)
        
        self.fingerprints[fingerprint] = {
            "job_id": job_id,
            "title": title,
            "description": description,
            "employer_id": employer_id,
            "registered_at": datetime.utcnow()
        }
        
        # Track employer history
        if employer_id not in self.employer_history:
            self.employer_history[employer_id] = []
        self.employer_history[employer_id].append(fingerprint)
        
        # Keep only last 20 jobs per employer
        if len(self.employer_history[employer_id]) > 20:
            self.employer_history[employer_id] = self.employer_history[employer_id][-20:]
        
        return fingerprint
    
    def get_employer_duplicate_stats(self, employer_id: str) -> Dict[str, Any]:
        """Get duplicate statistics for an employer"""
        if employer_id not in self.employer_history:
            return {
                "employer_id": employer_id,
                "total_jobs": 0,
                "unique_jobs": 0,
                "duplicate_rate": 0
            }
        
        history = self.employer_history[employer_id]
        unique = len(set(history))
        total = len(history)
        
        return {
            "employer_id": employer_id,
            "total_jobs": total,
            "unique_jobs": unique,
            "duplicate_rate": round((total - unique) / total * 100, 1) if total > 0 else 0
        }


# Global instance
duplicate_detector = DuplicateDetector()
