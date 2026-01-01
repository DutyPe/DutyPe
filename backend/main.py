"""
DutyPe AI Backend - Main Application
Trust & Enforcement Engine for Worker Protection
"""

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import structlog
import logging

from app.config import settings

# Configure structured logging
structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.JSONRenderer()
    ],
    wrapper_class=structlog.stdlib.BoundLogger,
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
    cache_logger_on_first_use=True,
)

logging.basicConfig(
    format="%(message)s",
    level=getattr(logging, settings.log_level.upper())
)

logger = structlog.get_logger()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events"""
    logger.info(
        "application_starting",
        host=settings.api_host,
        port=settings.api_port,
        debug=settings.api_debug,
        azure_endpoint=settings.azure_openai_endpoint[:30] + "..." if settings.azure_openai_endpoint else "NOT SET"
    )
    yield
    logger.info("application_shutting_down")


# Create FastAPI app
app = FastAPI(
    title="DutyPe AI Backend",
    description="""
    AI-powered Trust & Enforcement Engine for DutyPe.
    
    ## Core Principles
    - **Worker time > Employer convenience**
    - **LLM = Detection | Rules Engine = Enforcement**
    - **Automation > Manual support**
    
    ## Features
    - Job Risk Scoring
    - Employer Behavior Analysis
    - Fraud/Scam Detection
    - Worker & Employer Chatbots
    """,
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allow all for development
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Import and include routers
from app.api.routes import router as main_router
from app.api.chat_routes import router as chat_router
from app.api.protection_routes import router as protection_router

app.include_router(main_router)
app.include_router(chat_router)
app.include_router(protection_router)


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": "DutyPe AI Backend",
        "version": "1.0.0",
        "status": "running",
        "docs": "/docs",
        "endpoints": {
            "health": "/api/v1/health",
            "jobs": "/api/v1/jobs/analyze",
            "fraud": "/api/v1/fraud/detect",
            "employers": "/api/v1/employers/score",
            "worker_chat": "/api/v1/chat/worker",
            "employer_chat": "/api/v1/chat/employer",
            "protection": "/api/v1/protection/*",
            "silence": "/api/v1/protection/silence/*",
            "duplicates": "/api/v1/protection/duplicate/*",
            "multi_account": "/api/v1/protection/multi-account/*",
            "learning": "/api/v1/protection/learning/*",
            "regional": "/api/v1/protection/regional/*"
        }
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=settings.api_host,
        port=settings.api_port,
        reload=True
    )