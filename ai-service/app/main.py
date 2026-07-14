from fastapi import FastAPI

from app.api.routes.query import router as query_router

app = FastAPI(title="RAG AI Service")

app.include_router(query_router)
