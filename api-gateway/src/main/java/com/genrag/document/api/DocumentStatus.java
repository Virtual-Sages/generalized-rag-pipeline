package com.genrag.document.api;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Lifecycle of a document moving through the API Gateway (Orchestrator) and
 * the FastAPI (AI Service). Eighteen statuses drive the pipeline; three are
 * also the only ones ever shown to a user. UPLOADED and PROCESSED serve both
 * roles, FAILED is user-facing only.
 *
 * <p>The Orchestrator is the only writer of this status. The AI Service never
 * updates it directly; it reports progress to the status-update endpoint and
 * the Orchestrator performs the write.
 */
public enum DocumentStatus {

    UPLOADING, UPLOADED, UPLOADING_FAILED,
    READY_FOR_PROCESSING, ACCESS_FAILED,
    PARSING, PARSED, PARSING_FAILED,
    CHUNKING, CHUNKED, CHUNKING_FAILED,
    EMBEDDING, EMBEDDED, EMBEDDING_FAILED,
    INDEXING, INDEXED, INDEXING_FAILED,
    PROCESSED,

    /** Roll-up only: shown to users, never recorded by the pipeline. */
    FAILED;

    /** Statuses that exist only for the user, never as a pipeline state. */
    private static final Set<DocumentStatus> USER_VISIBLE_ONLY = EnumSet.of(FAILED);

    /**
     * Single source of truth for the internal -> user-visible projection.
     * Exhaustive on purpose: a new constant will not compile until mapped.
     *
     * @return the status a user should be shown for this internal status
     */
    public DocumentStatus toUserVisibleStatus() {
        switch (this) {
            case UPLOADING:
            case UPLOADED:
            case READY_FOR_PROCESSING:
            case PARSING:
            case PARSED:
            case CHUNKING:
            case CHUNKED:
            case EMBEDDING:
            case EMBEDDED:
            case INDEXING:
            case INDEXED:
                return UPLOADED;

            case PROCESSED:
                return PROCESSED;

            case UPLOADING_FAILED:
            case ACCESS_FAILED:
            case PARSING_FAILED:
            case CHUNKING_FAILED:
            case EMBEDDING_FAILED:
            case INDEXING_FAILED:
            case FAILED:
                return FAILED;

            default:
                throw new IllegalStateException("Unhandled DocumentStatus: " + this);
        }
    }

    /**
     * The three statuses a user can see, derived from the projection above so
     * the two can never disagree. Sorted into declaration order, which is the
     * lifecycle order UPLOADED -> PROCESSED -> FAILED.
     *
     * @return the user-visible statuses
     */
    public static List<DocumentStatus> getUserVisibleStatuses() {
        return Arrays.stream(values())
                .map(DocumentStatus::toUserVisibleStatus)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * The eighteen statuses the pipeline can actually record.
     *
     * @return the statuses that may be persisted against a document
     */
    public static List<DocumentStatus> getInternalStatuses() {
        return Arrays.stream(values())
                .filter(status -> !USER_VISIBLE_ONLY.contains(status))
                .toList();
    }

    /**
     * Every constant in this enum.
     *
     * @return all statuses, internal and user-visible alike
     */
    public static List<DocumentStatus> getAllStatuses() {
        return List.of(values());
    }
}
