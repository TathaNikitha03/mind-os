package com.mindos.backend.repository;

import com.mindos.backend.entity.AiExtraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiExtractionRepository extends JpaRepository<AiExtraction, Long> {
    List<AiExtraction> findByUserId(Long userId);
    List<AiExtraction> findByDocumentId(Long documentId);
    List<AiExtraction> findByUserIdAndExtractionType(Long userId, String extractionType);
}
