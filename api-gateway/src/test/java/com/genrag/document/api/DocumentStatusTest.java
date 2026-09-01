package com.genrag.document.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain JUnit test — no Spring context, no database.
 *
 * Asserts invariants on {@link DocumentStatus} so they cannot silently
 * regress as new statuses are added. Run with:
 *
 * <pre>{@code
 *   ./mvnw -o -q -Dtest=DocumentStatusTest test
 * }</pre>
 */
class DocumentStatusTest {

    // ── Cardinality invariants ─────────────────────────────────────────────────

    @Test
    void getAllStatuses_returns19() {
        assertEquals(19, DocumentStatus.getAllStatuses().size(),
                "getAllStatuses() must return all 19 constants");
    }

    @Test
    void getInternalStatuses_returns18() {
        List<DocumentStatus> internal = DocumentStatus.getInternalStatuses();
        assertEquals(18, internal.size(),
                "getInternalStatuses() must return exactly the 18 pipeline statuses");
    }

    @Test
    void getInternalStatuses_excludesFailed() {
        assertFalse(
                DocumentStatus.getInternalStatuses().contains(DocumentStatus.FAILED),
                "FAILED is user-visible only and must not appear in getInternalStatuses()");
    }

    // ── getUserVisibleStatuses ordering ──────────────────────────────────────────
    // Ordering regressed once; the .sorted() call in getUserVisibleStatuses() is
    // what fixes it. This test keeps it fixed.

    @Test
    void getUserVisibleStatuses_isExactlyUploadedProcessedFailed_inThatOrder() {
        List<DocumentStatus> visible = DocumentStatus.getUserVisibleStatuses();
        assertEquals(
                List.of(DocumentStatus.UPLOADED, DocumentStatus.PROCESSED, DocumentStatus.FAILED),
                visible,
                "getUserVisibleStatuses() must return [UPLOADED, PROCESSED, FAILED] in declaration order");
    }

    // ── No-leak invariant ────────────────────────────────────────────────────────
    // Every status (including FAILED itself) must project into the user-visible set.

    @Test
    void everyStatus_projectsIntoUserVisibleSet() {
        List<DocumentStatus> visible = DocumentStatus.getUserVisibleStatuses();
        for (DocumentStatus status : DocumentStatus.getAllStatuses()) {
            DocumentStatus projected = status.toUserVisibleStatus();
            assertTrue(
                    visible.contains(projected),
                    status + ".toUserVisibleStatus() = " + projected +
                    " which is not in getUserVisibleStatuses()");
        }
    }

    // ── Spot checks ───────────────────────────────────────────────────────────────

    @Test
    void parsing_projectsToUploaded() {
        assertEquals(DocumentStatus.UPLOADED, DocumentStatus.PARSING.toUserVisibleStatus(),
                "PARSING is mid-pipeline; users should see UPLOADED");
    }

    @Test
    void accessFailed_projectsToFailed() {
        assertEquals(DocumentStatus.FAILED, DocumentStatus.ACCESS_FAILED.toUserVisibleStatus(),
                "ACCESS_FAILED is a failure state; users should see FAILED");
    }

    @Test
    void processed_projectsToProcessed() {
        assertEquals(DocumentStatus.PROCESSED, DocumentStatus.PROCESSED.toUserVisibleStatus(),
                "PROCESSED is terminal-success; users should see PROCESSED");
    }

    // -- State machine --------------------------------------------------------
    // The transition table is the graph in Docs/flow.mmd expressed in code.
    // These assert the two stay in step.

    /** The happy path, exactly as document_processor walks it. */
    private static final List<DocumentStatus> HAPPY_PATH = List.of(
            DocumentStatus.UPLOADING,
            DocumentStatus.UPLOADED,
            DocumentStatus.READY_FOR_PROCESSING,
            DocumentStatus.PARSING,
            DocumentStatus.PARSED,
            DocumentStatus.CHUNKING,
            DocumentStatus.CHUNKED,
            DocumentStatus.EMBEDDING,
            DocumentStatus.EMBEDDED,
            DocumentStatus.INDEXING,
            DocumentStatus.INDEXED,
            DocumentStatus.PROCESSED);

    @Test
    void happyPath_isLegalEndToEnd() {
        for (int i = 0; i < HAPPY_PATH.size() - 1; i++) {
            DocumentStatus from = HAPPY_PATH.get(i);
            DocumentStatus to = HAPPY_PATH.get(i + 1);
            assertTrue(from.canTransitionTo(to),
                    from + " -> " + to + " is on the happy path and must be legal");
        }
    }

    @Test
    void eachStage_mayFailFromItsInProgressStatus() {
        assertTrue(DocumentStatus.UPLOADING.canTransitionTo(DocumentStatus.UPLOADING_FAILED));
        assertTrue(DocumentStatus.UPLOADED.canTransitionTo(DocumentStatus.ACCESS_FAILED));
        assertTrue(DocumentStatus.PARSING.canTransitionTo(DocumentStatus.PARSING_FAILED));
        assertTrue(DocumentStatus.CHUNKING.canTransitionTo(DocumentStatus.CHUNKING_FAILED));
        assertTrue(DocumentStatus.EMBEDDING.canTransitionTo(DocumentStatus.EMBEDDING_FAILED));
        assertTrue(DocumentStatus.INDEXING.canTransitionTo(DocumentStatus.INDEXING_FAILED));
    }

    @Test
    void terminalStatuses_haveNoSuccessors() {
        for (DocumentStatus terminal : DocumentStatus.getTerminalStatuses()) {
            for (DocumentStatus next : DocumentStatus.getAllStatuses()) {
                assertFalse(terminal.canTransitionTo(next),
                        terminal + " is terminal but claims it can move to " + next);
            }
        }
    }

    @Test
    void terminalStatuses_areProcessedPlusTheSixStageFailures() {
        assertEquals(
                List.of(
                        DocumentStatus.UPLOADING_FAILED,
                        DocumentStatus.ACCESS_FAILED,
                        DocumentStatus.PARSING_FAILED,
                        DocumentStatus.CHUNKING_FAILED,
                        DocumentStatus.EMBEDDING_FAILED,
                        DocumentStatus.INDEXING_FAILED,
                        DocumentStatus.PROCESSED),
                DocumentStatus.getTerminalStatuses(),
                "the terminal set must be the 6 failures + PROCESSED, in declaration order");
    }

    /** This is the clobber that motivated the guard. */
    @Test
    void finishedDocument_cannotBeReopened() {
        assertFalse(DocumentStatus.PROCESSED.canTransitionTo(DocumentStatus.ACCESS_FAILED),
                "a re-run reporting ACCESS_FAILED must not overwrite a PROCESSED document");
        assertFalse(DocumentStatus.PROCESSED.canTransitionTo(DocumentStatus.READY_FOR_PROCESSING),
                "a PROCESSED document cannot re-enter the pipeline without an explicit reset");
        assertFalse(DocumentStatus.ACCESS_FAILED.canTransitionTo(DocumentStatus.READY_FOR_PROCESSING),
                "a failed document cannot restart itself either");
    }

    @Test
    void backwardsAndSkippingTransitions_areIllegal() {
        assertFalse(DocumentStatus.INDEXED.canTransitionTo(DocumentStatus.PARSING),
                "INDEXED must not fall back to PARSING");
        assertFalse(DocumentStatus.PARSING.canTransitionTo(DocumentStatus.INDEXED),
                "PARSING must not skip ahead to INDEXED");
        assertFalse(DocumentStatus.READY_FOR_PROCESSING.canTransitionTo(DocumentStatus.PROCESSED),
                "the pipeline cannot jump straight to PROCESSED");
    }

    @Test
    void noStatus_transitionsToItself() {
        for (DocumentStatus status : DocumentStatus.getAllStatuses()) {
            assertFalse(status.canTransitionTo(status),
                    status + " must not have a self-loop; a repeated report is not a transition");
        }
    }

    @Test
    void failed_isOutsideTheStateMachine() {
        assertTrue(DocumentStatus.FAILED.isTerminal(),
                "FAILED is user-visible only and is never a state to move out of");
        assertTrue(DocumentStatus.allowedPredecessorsOf(DocumentStatus.FAILED).isEmpty(),
                "nothing may transition to FAILED; it is never persisted");
    }

    // -- allowedPredecessorsOf is the inverse of canTransitionTo --------------

    @Test
    void allowedPredecessors_isExactlyTheInverseOfTheTable() {
        for (DocumentStatus next : DocumentStatus.getAllStatuses()) {
            Set<DocumentStatus> predecessors = DocumentStatus.allowedPredecessorsOf(next);
            for (DocumentStatus current : DocumentStatus.getAllStatuses()) {
                assertEquals(current.canTransitionTo(next), predecessors.contains(current),
                        "allowedPredecessorsOf(" + next + ") disagrees with "
                                + current + ".canTransitionTo(" + next + ")");
            }
        }
    }

    @Test
    void allowedPredecessors_neverIncludesATerminalStatus() {
        for (DocumentStatus next : DocumentStatus.getAllStatuses()) {
            for (DocumentStatus predecessor : DocumentStatus.allowedPredecessorsOf(next)) {
                assertFalse(predecessor.isTerminal(),
                        predecessor + " is terminal and must never be a legal predecessor of " + next);
            }
        }
    }

    @Test
    void everyInternalStatus_isReachableExceptTheEntryPoint() {
        for (DocumentStatus status : DocumentStatus.getInternalStatuses()) {
            if (status == DocumentStatus.UPLOADING) {
                continue;   // the entry point; nothing precedes it
            }
            assertFalse(DocumentStatus.allowedPredecessorsOf(status).isEmpty(),
                    status + " is unreachable - no status can transition to it");
        }
    }
}
