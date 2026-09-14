package com.mindos.backend.repository;

import com.mindos.backend.entity.AiInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiInteractionRepository extends JpaRepository<AiInteraction, Long> {
    List<AiInteraction> findByUserId(Long userId);
    List<AiInteraction> findByUserIdAndSessionId(Long userId, String sessionId);
    List<AiInteraction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
