package com.mindos.backend.repository;

import com.mindos.backend.entity.Document;
import com.mindos.backend.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserId(Long userId);
    List<Document> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Document> findByUserIdAndStatus(Long userId, DocumentStatus status);
    List<Document> findByCategoryId(Long categoryId);
    Optional<Document> findByIdAndUserId(Long id, Long userId);
}

