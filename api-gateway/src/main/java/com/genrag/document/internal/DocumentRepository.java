package com.genrag.document.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}