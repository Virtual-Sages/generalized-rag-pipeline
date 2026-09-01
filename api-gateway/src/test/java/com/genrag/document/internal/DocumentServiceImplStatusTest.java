package com.genrag.document.internal;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.genrag.document.api.DocumentStatus;
import com.genrag.document.internal.storage.StorageService;
import com.genrag.user.internal.UserRepository;

import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the state-machine guard on
 * {@link DocumentServiceImpl#updateStatus}.
 *
 * <p>Whether the conditional UPDATE itself matches the right rows is a
 * database concern and belongs to a query test. What is asserted here is the
 * behaviour around it: that validation still rejects, that an illegal
 * transition is a no-op rather than an error, and that the predecessor set
 * handed to the query is the one the state machine derives.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceImplStatusTest {

    private static final UUID DOCUMENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock private DocumentRepository documentRepository;
    @Mock private StorageService storageService;
    @Mock private UserRepository userRepository;
    @Mock private RestTemplate restTemplate;

    @Captor private ArgumentCaptor<Collection<DocumentStatus>> predecessorsCaptor;

    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentServiceImpl(
                documentRepository, storageService, userRepository, restTemplate,
                "http://localhost:8000");
    }

    /** Puts a document at {@code current} in the repository. */
    private void documentIsAt(DocumentStatus current) {
        DocumentEntity document = new DocumentEntity();
        document.setStatus(current);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
    }

    // -- Validation still comes first -----------------------------------------

    @Test
    void failed_isRejectedBeforeTouchingTheRepository() {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> documentService.updateStatus(DOCUMENT_ID, DocumentStatus.FAILED));

        assertTrue(thrown.getMessage().contains("not a valid internal status"),
                "message should explain FAILED is user-visible only, was: " + thrown.getMessage());
        verify(documentRepository, never()).updateStatusIfAllowed(any(), any(), anyCollection());
    }

    @Test
    void unknownDocument_throws() {
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> documentService.updateStatus(DOCUMENT_ID, DocumentStatus.PARSING));

        assertTrue(thrown.getMessage().contains("Document not found"),
                "message should name the missing document, was: " + thrown.getMessage());
        verify(documentRepository, never()).updateStatusIfAllowed(any(), any(), anyCollection());
    }

    // -- The guard ------------------------------------------------------------

    @Test
    void legalTransition_isWritten() {
        documentIsAt(DocumentStatus.READY_FOR_PROCESSING);
        when(documentRepository.updateStatusIfAllowed(
                eq(DOCUMENT_ID), eq(DocumentStatus.PARSING), anyCollection()))
                .thenReturn(1);

        assertDoesNotThrow(() -> documentService.updateStatus(DOCUMENT_ID, DocumentStatus.PARSING));

        verify(documentRepository).updateStatusIfAllowed(
                eq(DOCUMENT_ID), eq(DocumentStatus.PARSING), anyCollection());
    }

    @Test
    void illegalTransition_isIgnoredNotRejected() {
        documentIsAt(DocumentStatus.PROCESSED);
        when(documentRepository.updateStatusIfAllowed(
                eq(DOCUMENT_ID), eq(DocumentStatus.ACCESS_FAILED), anyCollection()))
                .thenReturn(0);

        // A stale report is a fire-and-forget reporter behaving correctly
        // against a pipeline that moved on. It must not surface as a 400.
        assertDoesNotThrow(
                () -> documentService.updateStatus(DOCUMENT_ID, DocumentStatus.ACCESS_FAILED),
                "an illegal transition must be a silent no-op, not an error");
    }

    // -- The query gets the derived predecessor set ---------------------------

    @Test
    void theQueryReceivesTheDerivedPredecessors() {
        documentIsAt(DocumentStatus.READY_FOR_PROCESSING);
        when(documentRepository.updateStatusIfAllowed(
                eq(DOCUMENT_ID), eq(DocumentStatus.PARSING), anyCollection()))
                .thenReturn(1);

        documentService.updateStatus(DOCUMENT_ID, DocumentStatus.PARSING);

        verify(documentRepository).updateStatusIfAllowed(
                eq(DOCUMENT_ID), eq(DocumentStatus.PARSING), predecessorsCaptor.capture());

        Collection<DocumentStatus> passed = predecessorsCaptor.getValue();
        assertEquals(DocumentStatus.allowedPredecessorsOf(DocumentStatus.PARSING), Set.copyOf(passed),
                "the guard must use the state machine's predecessors, not a hand-written list");
        assertTrue(passed.contains(DocumentStatus.READY_FOR_PROCESSING),
                "PARSING is only reachable from READY_FOR_PROCESSING");
        assertFalse(passed.contains(DocumentStatus.PROCESSED),
                "a finished document must never be a legal predecessor");
    }

    @Test
    void aTerminalTargetIsOnlyReachableFromItsOwnStage() {
        documentIsAt(DocumentStatus.PARSING);
        when(documentRepository.updateStatusIfAllowed(
                eq(DOCUMENT_ID), eq(DocumentStatus.PARSING_FAILED), anyCollection()))
                .thenReturn(1);

        documentService.updateStatus(DOCUMENT_ID, DocumentStatus.PARSING_FAILED);

        verify(documentRepository).updateStatusIfAllowed(
                eq(DOCUMENT_ID), eq(DocumentStatus.PARSING_FAILED), predecessorsCaptor.capture());

        assertEquals(Set.of(DocumentStatus.PARSING), Set.copyOf(predecessorsCaptor.getValue()),
                "only a document mid-parse can be recorded as PARSING_FAILED");
    }
}
