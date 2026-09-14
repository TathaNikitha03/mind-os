package com.mindos.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_dependencies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "depends_on_task_id"}))
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depends_on_task_id", nullable = false)
    private Task dependsOnTask;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TaskDependency() {}

    public TaskDependency(Long id, Task task, Task dependsOnTask, LocalDateTime createdAt) {
        this.id = id;
        this.task = task;
        this.dependsOnTask = dependsOnTask;
        this.createdAt = createdAt;
    }

    public static TaskDependencyBuilder builder() {
        return new TaskDependencyBuilder();
    }

    public static class TaskDependencyBuilder {
        private Long id;
        private Task task;
        private Task dependsOnTask;
        private LocalDateTime createdAt;

        public TaskDependencyBuilder id(Long id) { this.id = id; return this; }
        public TaskDependencyBuilder task(Task task) { this.task = task; return this; }
        public TaskDependencyBuilder dependsOnTask(Task dependsOnTask) { this.dependsOnTask = dependsOnTask; return this; }
        public TaskDependencyBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public TaskDependency build() {
            return new TaskDependency(id, task, dependsOnTask, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public Task getDependsOnTask() { return dependsOnTask; }
    public void setDependsOnTask(Task dependsOnTask) { this.dependsOnTask = dependsOnTask; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
