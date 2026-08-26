package com.genrag.document.api;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
     * The state machine: for each status, the statuses it may legally move to.
     * This is the same graph drawn in {@code Docs/flow.mmd}, expressed in code
     * so it can be enforced rather than only documented.
     *
     * <p>A status with no successors is terminal. Populated in a static block
     * because an enum constant cannot reference another constant from its own
     * constructor.
     *
     * <p>Note that {@code UPLOADING} and {@code UPLOADED} are written directly
     * by the upload path, not through {@code updateStatus}; they appear here so
     * the graph is complete.
     */
    private static final Map<DocumentStatus, Set<DocumentStatus>> TRANSITIONS =
            new EnumMap<>(DocumentStatus.class);

    static {
        TRANSITIONS.put(UPLOADING,            EnumSet.of(UPLOADED, UPLOADING_FAILED));
        TRANSITIONS.put(UPLOADED,             EnumSet.of(READY_FOR_PROCESSING, ACCESS_FAILED));
        TRANSITIONS.put(READY_FOR_PROCESSING, EnumSet.of(PARSING));
        TRANSITIONS.put(PARSING,              EnumSet.of(PARSED, PARSING_FAILED));
        TRANSITIONS.put(PARSED,               EnumSet.of(CHUNKING));
        TRANSITIONS.put(CHUNKING,             EnumSet.of(CHUNKED, CHUNKING_FAILED));
        TRANSITIONS.put(CHUNKED,              EnumSet.of(EMBEDDING));
        TRANSITIONS.put(EMBEDDING,            EnumSet.of(EMBEDDED, EMBEDDING_FAILED));
        TRANSITIONS.put(EMBEDDED,             EnumSet.of(INDEXING));
        TRANSITIONS.put(INDEXING,             EnumSet.of(INDEXED, INDEXING_FAILED));
        TRANSITIONS.put(INDEXED,              EnumSet.of(PROCESSED));

        // Terminal: the pipeline is over, in success or in failure.
        TRANSITIONS.put(PROCESSED,        EnumSet.noneOf(DocumentStatus.class));
        TRANSITIONS.put(UPLOADING_FAILED, EnumSet.noneOf(DocumentStatus.class));
        TRANSITIONS.put(ACCESS_FAILED,    EnumSet.noneOf(DocumentStatus.class));
        TRANSITIONS.put(PARSING_FAILED,   EnumSet.noneOf(DocumentStatus.class));
        TRANSITIONS.put(CHUNKING_FAILED,  EnumSet.noneOf(DocumentStatus.class));
        TRANSITIONS.put(EMBEDDING_FAILED, EnumSet.noneOf(DocumentStatus.class));
        TRANSITIONS.put(INDEXING_FAILED,  EnumSet.noneOf(DocumentStatus.class));

        // Never persisted, so it is never a state to move out of.
        TRANSITIONS.put(FAILED, EnumSet.noneOf(DocumentStatus.class));
    }

    /**
     * Whether this status may legally move to {@code next}.
     *
     * @param next the status being reported
     * @return true if the transition is an edge in the state machine
     */
    public boolean canTransitionTo(DocumentStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    /**
     * Whether this status ends the pipeline, meaning it has no outgoing
     * transitions. Terminal statuses are PROCESSED and the six per-stage
     * failures.
     *
     * @return true if no further status may be recorded against a document
     *         in this status
     */
    public boolean isTerminal() {
        return TRANSITIONS.get(this).isEmpty();
    }

    /**
     * The statuses from which {@code next} may legally be reached, derived by
     * inverting {@link #TRANSITIONS} so there is no second graph to maintain.
     *
     * <p>Used as the guard on the status write: an UPDATE that matches only
     * these statuses cannot record an illegal transition, and needs no
     * separate read to decide.
     *
     * @param next the status being reported
     * @return the statuses that may transition to {@code next}; empty if it is
     *         unreachable
     */
    public static Set<DocumentStatus> allowedPredecessorsOf(DocumentStatus next) {
        return Arrays.stream(values())
                .filter(current -> current.canTransitionTo(next))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DocumentStatus.class)));
    }

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
     * The statuses that end the pipeline, derived from {@link #isTerminal()}.
     *
     * @return the terminal statuses, in declaration order
     */
    public static List<DocumentStatus> getTerminalStatuses() {
        return getInternalStatuses().stream()
                .filter(DocumentStatus::isTerminal)
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
