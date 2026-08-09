package com.genrag.document.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface DocumentRepository extends  JpaRepository<DocumentEntity, UUID>{
    List<DocumentEntity> findByUser_IdOrderByUpdatedAtDesc(UUID userId);
}