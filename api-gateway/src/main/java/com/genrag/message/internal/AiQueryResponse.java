package com.genrag.message.internal;

import java.util.List;

/**
 * Response body from the AI service's POST /query endpoint.
 * Mirrors QueryResponse in ai-service/app/models/query.py.
 *
 * {@code sources} is parsed but not yet surfaced to clients; it is kept here so the
 * contract stays explicit rather than being dropped at an untyped Map boundary.
 */
record AiQueryResponse(String query, String answer, List<String> sources) {}
