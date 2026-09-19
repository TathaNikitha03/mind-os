package com.mindos.backend.service;

import com.mindos.backend.dto.DocumentResponse;
import com.mindos.backend.dto.TaskResponse;
import com.mindos.backend.entity.Document;
import com.mindos.backend.entity.Task;
import com.mindos.backend.entity.TaskDocument;
import com.mindos.backend.entity.User;
import com.mindos.backend.repository.DocumentRepository;
import com.mindos.backend.repository.TaskDocumentRepository;
import com.mindos.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskDocumentService {

    private final TaskDocumentRepository taskDocumentRepository;
    private final TaskRepository taskRepository;
    private final DocumentRepository documentRepository;
    private final TaskService taskService;

    public TaskDocumentService(
            TaskDocumentRepository taskDocumentRepository,
            TaskRepository taskRepository,
            DocumentRepository documentRepository,
            TaskService taskService
    ) {
        this.taskDocumentRepository = taskDocumentRepository;
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
        this.taskService = taskService;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsForTask(User user, Long taskId) {
        Task task = getTaskAndVerifyOwnership(user, taskId);
        List<TaskDocument> links = taskDocumentRepository.findByTaskId(task.getId());
        return links.stream().map(link -> mapToDocumentResponse(link.getDocument())).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksForDocument(User user, Long documentId) {
        Document document = getDocumentAndVerifyOwnership(user, documentId);
        List<TaskDocument> links = taskDocumentRepository.findByDocumentId(document.getId());
        return links.stream().map(link -> taskService.getTaskById(user, link.getTask().getId())).toList();
    }

    @Transactional
    public DocumentResponse attachDocumentToTask(User user, Long taskId, Long documentId) {
        Task task = getTaskAndVerifyOwnership(user, taskId);
        Document document = getDocumentAndVerifyOwnership(user, documentId);

        if (taskDocumentRepository.existsByTaskIdAndDocumentId(task.getId(), document.getId())) {
            return mapToDocumentResponse(document);
        }

        TaskDocument taskDocument = TaskDocument.builder()
                .task(task)
                .document(document)
                .build();

        taskDocumentRepository.save(taskDocument);
        return mapToDocumentResponse(document);
    }

    @Transactional
    public void detachDocumentFromTask(User user, Long taskId, Long documentId) {
        Task task = getTaskAndVerifyOwnership(user, taskId);
        Document document = getDocumentAndVerifyOwnership(user, documentId);

        taskDocumentRepository.deleteByTaskIdAndDocumentId(task.getId(), document.getId());
    }

    private Task getTaskAndVerifyOwnership(User user, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new SecurityException("You do not have permission to access this task.");
        }
        return task;
    }

    private Document getDocumentAndVerifyOwnership(User user, Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new SecurityException("You do not have permission to access this document.");
        }
        return document;
    }

    private DocumentResponse mapToDocumentResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .userId(doc.getUser() != null ? doc.getUser().getId() : null)
                .categoryId(doc.getCategory() != null ? doc.getCategory().getId() : null)
                .categoryName(doc.getCategory() != null ? doc.getCategory().getName() : null)
                .fileName(doc.getFileName())
                .title(doc.getTitle() != null ? doc.getTitle() : doc.getFileName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .description(doc.getDescription())
                .storageUrl(doc.getFileUrl())
                .status(doc.getStatus() != null ? doc.getStatus().name() : "READY")
                .uploadedAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
