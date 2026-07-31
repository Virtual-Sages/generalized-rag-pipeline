package com.genrag.chat.internal;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, UUID>{
    List<ChatEntity> findByUser_IdOrderByUpdatedAtDesc(UUID userId);
}