package com.mindos.backend.repository;

import com.mindos.backend.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentEntityRepository extends JpaRepository<DocumentEntity, Long> {
    List<DocumentEntity> findByDocumentId(Long documentId);
    List<DocumentEntity> findByDocumentIdAndEntityType(Long documentId, String entityType);
}
