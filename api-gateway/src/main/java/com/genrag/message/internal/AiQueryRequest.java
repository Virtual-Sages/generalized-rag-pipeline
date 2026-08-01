package com.genrag.message.internal;

/**
 * Request body for the AI service's POST /query endpoint.
 * Mirrors QueryRequest in ai-service/app/models/query.py.
 */
record AiQueryRequest(String query) {}
