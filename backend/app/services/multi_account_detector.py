"""
Multi-Account Detection Service
Detects scammers creating multiple accounts
"""

from typing import Dict, Any, List, Optional, Set
from datetime import datetime, timedelta
import hashlib
import structlog

logger = structlog.get_logger()


class MultiAccountDetector:
    """
    Detects multi-account abuse patterns
    
    Signals:
    - Same phone number across accounts
    - Same device ID
    - Same IP address patterns
    - Similar job posting patterns
    - Linked payment details
    """
    
    def __init__(self):
        # Phone -> employer_ids
        self.phone_map: Dict[str, Set[str]] = {}
        # Device -> employer_ids
        self.device_map: Dict[str, Set[str]] = {}
        # IP -> employer_ids
        self.ip_map: Dict[str, Set[str]] = {}
        # Employer -> signals
        self.employer_signals: Dict[str, Dict[str, Any]] = {}
    
    def _hash_identifier(self, identifier: str) -> str:
        """Hash sensitive identifiers"""
        return hashlib.sha256(identifier.encode()).hexdigest()[:16]
    
    def register_employer(
        self,
        employer_id: str,
        phone: Optional[str] = None,
        device_id: Optional[str] = None,
        ip_address: Optional[str] = None,
        email: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Register employer identifiers for tracking
        Returns any detected links to other accounts
        """
        linked_accounts: Set[str] = set()
        signals = []
        
        # Check phone
        if phone:
            phone_hash = self._hash_identifier(phone)
            if phone_hash in self.phone_map:
                existing = self.phone_map[phone_hash]
                if employer_id not in existing:
                    linked_accounts.update(existing)
                    signals.append(f"PHONE_MATCH: {len(existing)} other accounts")
                existing.add(employer_id)
            else:
                self.phone_map[phone_hash] = {employer_id}
        
        # Check device
        if device_id:
            device_hash = self._hash_identifier(device_id)
            if device_hash in self.device_map:
                existing = self.device_map[device_hash]
                if employer_id not in existing:
                    linked_accounts.update(existing)
                    signals.append(f"DEVICE_MATCH: {len(existing)} other accounts")
                existing.add(employer_id)
            else:
                self.device_map[device_hash] = {employer_id}
        
        # Check IP (less reliable, use with caution)
        if ip_address:
            ip_hash = self._hash_identifier(ip_address)
            if ip_hash in self.ip_map:
                existing = self.ip_map[ip_hash]
                if employer_id not in existing and len(existing) >= 3:
                    # Only flag if 3+ accounts from same IP
                    linked_accounts.update(existing)
                    signals.append(f"IP_CLUSTER: {len(existing)} other accounts")
                existing.add(employer_id)
            else:
                self.ip_map[ip_hash] = {employer_id}
        
        # Store signals
        self.employer_signals[employer_id] = {
            "employer_id": employer_id,
            "linked_accounts": list(linked_accounts),
            "signals": signals,
            "is_suspicious": len(linked_accounts) > 0,
            "risk_score": min(100, len(linked_accounts) * 30 + len(signals) * 20),
            "registered_at": datetime.utcnow()
        }
        
        if linked_accounts:
            logger.warning(
                "multi_account_detected",
                employer_id=employer_id,
                linked_count=len(linked_accounts),
                signals=signals
            )
        
        return self.employer_signals[employer_id]
    
    def check_employer(self, employer_id: str) -> Dict[str, Any]:
        """Check if employer has multi-account signals"""
        if employer_id in self.employer_signals:
            return self.employer_signals[employer_id]
        
        return {
            "employer_id": employer_id,
            "linked_accounts": [],
            "signals": [],
            "is_suspicious": False,
            "risk_score": 0
        }
    
    def get_account_cluster(self, employer_id: str) -> List[str]:
        """Get all accounts linked to this employer"""
        if employer_id not in self.employer_signals:
            return [employer_id]
        
        linked = set(self.employer_signals[employer_id].get("linked_accounts", []))
        linked.add(employer_id)
        
        # Expand to find all connected accounts
        expanded = True
        while expanded:
            expanded = False
            for emp_id in list(linked):
                if emp_id in self.employer_signals:
                    new_links = set(self.employer_signals[emp_id].get("linked_accounts", []))
                    before = len(linked)
                    linked.update(new_links)
                    if len(linked) > before:
                        expanded = True
        
        return list(linked)
    
    def report_fraud(self, employer_id: str, reason: str) -> Dict[str, Any]:
        """
        Report fraud for an employer
        Propagates risk to all linked accounts
        """
        cluster = self.get_account_cluster(employer_id)
        
        # Increase risk for all linked accounts
        for emp_id in cluster:
            if emp_id in self.employer_signals:
                self.employer_signals[emp_id]["risk_score"] = 100
                self.employer_signals[emp_id]["fraud_reported"] = True
                self.employer_signals[emp_id]["fraud_reason"] = reason
            else:
                self.employer_signals[emp_id] = {
                    "employer_id": emp_id,
                    "linked_accounts": [e for e in cluster if e != emp_id],
                    "signals": ["LINKED_TO_FRAUD"],
                    "is_suspicious": True,
                    "risk_score": 100,
                    "fraud_reported": True,
                    "fraud_reason": f"Linked to {employer_id}: {reason}"
                }
        
        logger.warning(
            "fraud_reported_cluster",
            employer_id=employer_id,
            cluster_size=len(cluster),
            reason=reason
        )
        
        return {
            "reported_employer": employer_id,
            "affected_accounts": cluster,
            "cluster_size": len(cluster)
        }


# Global instance
multi_account_detector = MultiAccountDetector()
