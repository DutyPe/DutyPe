"""
API Routes for DutyPe AI Backend
"""

from .routes import router
from .chat_routes import router as chat_router

__all__ = ["router", "chat_router"]
