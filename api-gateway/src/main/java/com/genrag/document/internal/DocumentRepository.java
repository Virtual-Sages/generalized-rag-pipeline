package com.genrag.document.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.genrag.document.api.DocumentStatus;

import java.util.Collection;
import java.util.UUID;
import java.util.List;

/**
 * Repository for performing database operations on documents.
 */
@Repository
public interface DocumentRepository extends  JpaRepository<DocumentEntity, UUID>{
    /**
     * Retrieves all documents belonging to a user, ordered by
     * the most recently updated document first.
     *
     * @param userId the unique identifier of the user
     * @return list of documents ordered by updated date in descending order
     */
    List<DocumentEntity> findByUser_IdOrderByUpdatedAtDesc(UUID userId);

    /**
     * Records a status against a document only if its current status may
     * legally transition to it.
     *
     * <p>The legality check is the WHERE clause rather than a separate read,
     * so two concurrent reports cannot both observe a valid current status and
     * both write. This blocks a write onto a finished document and an
     * out-of-order report alike: a terminal status has no legal successor, so
     * it never appears among the allowed predecessors of anything.
     *
     * <p>{@code updatedAt} is set explicitly because a bulk JPQL update
     * bypasses Hibernate's {@code @UpdateTimestamp}, and
     * {@link #findByUser_IdOrderByUpdatedAtDesc} sorts on it.
     *
     * @param id the document to update
     * @param status the status being reported
     * @param allowedPredecessors the statuses that may transition to
     *        {@code status}, from
     *        {@link DocumentStatus#allowedPredecessorsOf(DocumentStatus)}
     * @return 1 if the status was recorded, 0 if the transition was illegal
     */
    @Modifying
    @Query("""
            UPDATE DocumentEntity d
               SET d.status = :status, d.updatedAt = CURRENT_TIMESTAMP
             WHERE d.id = :id
               AND d.status IN :allowedPredecessors
            """)
    int updateStatusIfAllowed(
            @Param("id") UUID id,
            @Param("status") DocumentStatus status,
            @Param("allowedPredecessors") Collection<DocumentStatus> allowedPredecessors);
}