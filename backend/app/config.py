"""
Configuration Management for DutyPe AI Backend
"""

from pydantic_settings import BaseSettings
from typing import List


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""
    
    # Azure OpenAI (matching .env names exactly)
    azure_openai_api_key: str = ""
    azure_openai_endpoint: str = ""
    azure_openai_deployment_name: str = "gpt-4o-mini"
    azure_openai_api_version: str = "2025-01-01-preview"
    
    # Firebase
    firebase_project_id: str = ""
    firebase_credentials_path: str = "./firebase-credentials.json"
    
    # API
    api_host: str = "0.0.0.0"
    api_port: int = 8000
    api_debug: bool = False
    api_key: str = ""
    
    # CORS
    allowed_origins: str = "http://localhost:3000"
    
    # Rate Limiting
    rate_limit_requests: int = 100
    rate_limit_window: int = 60
    
    # Logging
    log_level: str = "INFO"
    
    @property
    def allowed_origins_list(self) -> List[str]:
        return [origin.strip() for origin in self.allowed_origins.split(",")]
    
    model_config = {
        "env_file": ".env",
        "env_file_encoding": "utf-8",
        "extra": "ignore"
    }


# Global settings instance
settings = Settings()
