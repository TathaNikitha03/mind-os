package com.mindos.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_documents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "document_id"}))
public class TaskDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TaskDocument() {}

    public TaskDocument(Long id, Task task, Document document, LocalDateTime createdAt) {
        this.id = id;
        this.task = task;
        this.document = document;
        this.createdAt = createdAt;
    }

    public static TaskDocumentBuilder builder() {
        return new TaskDocumentBuilder();
    }

    public static class TaskDocumentBuilder {
        private Long id;
        private Task task;
        private Document document;
        private LocalDateTime createdAt;

        public TaskDocumentBuilder id(Long id) { this.id = id; return this; }
        public TaskDocumentBuilder task(Task task) { this.task = task; return this; }
        public TaskDocumentBuilder document(Document document) { this.document = document; return this; }
        public TaskDocumentBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public TaskDocument build() {
            return new TaskDocument(id, task, document, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
