package com.mindos.backend.repository;

import com.mindos.backend.entity.TaskDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskDocumentRepository extends JpaRepository<TaskDocument, Long> {

    List<TaskDocument> findByTaskId(Long taskId);

    List<TaskDocument> findByDocumentId(Long documentId);

    boolean existsByTaskIdAndDocumentId(Long taskId, Long documentId);

    Optional<TaskDocument> findByTaskIdAndDocumentId(Long taskId, Long documentId);

    long countByTaskId(Long taskId);

    long countByDocumentId(Long documentId);

    void deleteByTaskIdAndDocumentId(Long taskId, Long documentId);
}
