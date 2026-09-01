package com.genrag.document.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.genrag.document.api.DocumentService;
import com.genrag.document.api.dto.StatusUpdateRequest;

/**
 * Internal endpoint consumed by the AI Service to report pipeline
 * stage transitions.
 *
 * <p>This controller is intentionally unauthenticated — security is
 * delegated to network isolation (the AI Service runs on the same
 * private network as the Orchestrator). No {@code @AuthenticationPrincipal}
 * is present here.
 *
 * <p>Adding a shared-secret header later is a ~15-line change
 * (one servlet filter + one config key).
 */
@RestController
@RequestMapping("/api/internal")
public class InternalDocumentController {

    private final DocumentService documentService;

    public InternalDocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Receives a status update from the AI Service.
     *
     * <p>Returns {@code 204 No Content} on success. Returns {@code 400 Bad Request}
     * if the document does not exist or if the requested status is not a valid
     * internal status (e.g. {@code FAILED}, which is user-visible only).
     *
     * @param request the status update containing documentId and new status
     * @return 204 on success, 400 on validation failure
     */
    @PostMapping("/status-update")
    public ResponseEntity<Void> updateStatus(@RequestBody StatusUpdateRequest request) {
        documentService.updateStatus(request.documentId(), request.status());
        return ResponseEntity.noContent().build();
    }

    /**
     * Maps {@link IllegalArgumentException} thrown by
     * {@link DocumentService#updateStatus} to a {@code 400 Bad Request}.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
