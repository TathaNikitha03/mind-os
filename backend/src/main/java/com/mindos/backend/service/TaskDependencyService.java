package com.mindos.backend.service;

import com.mindos.backend.dto.TaskDependencyRequest;
import com.mindos.backend.dto.TaskDependencyResponse;
import com.mindos.backend.entity.Task;
import com.mindos.backend.entity.TaskDependency;
import com.mindos.backend.entity.User;
import com.mindos.backend.enums.TaskStatus;
import com.mindos.backend.repository.TaskDependencyRepository;
import com.mindos.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TaskDependencyService {

    private final TaskDependencyRepository taskDependencyRepository;
    private final TaskRepository taskRepository;

    public TaskDependencyService(TaskDependencyRepository taskDependencyRepository, TaskRepository taskRepository) {
        this.taskDependencyRepository = taskDependencyRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskDependencyResponse> getDependencies(User user, Long taskId) {
        Task task = getTaskAndVerifyOwnership(user, taskId);
        List<TaskDependency> dependencies = taskDependencyRepository.findByTaskId(task.getId());
        return dependencies.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskDependencyResponse> getDependents(User user, Long taskId) {
        Task task = getTaskAndVerifyOwnership(user, taskId);
        List<TaskDependency> dependents = taskDependencyRepository.findByDependsOnTaskId(task.getId());
        return dependents.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public TaskDependencyResponse addDependency(User user, Long taskId, TaskDependencyRequest request) {
        if (request == null || request.getDependsOnTaskId() == null) {
            throw new IllegalArgumentException("Prerequisite task ID (dependsOnTaskId) is required.");
        }

        Long dependsOnTaskId = request.getDependsOnTaskId();

        // 1. A task cannot depend on itself
        if (taskId.equals(dependsOnTaskId)) {
            throw new IllegalArgumentException("A task cannot depend on itself.");
        }

        // 2. Verify ownership of both tasks
        Task task = getTaskAndVerifyOwnership(user, taskId);
        Task dependsOnTask = getTaskAndVerifyOwnership(user, dependsOnTaskId);

        // 3. Do not allow duplicate dependencies
        if (taskDependencyRepository.existsByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId)) {
            throw new IllegalArgumentException("Dependency already exists between these tasks.");
        }

        // 4. Prevent direct & indirect circular dependencies
        if (isReachable(dependsOnTaskId, taskId)) {
            throw new IllegalArgumentException("Circular dependency detected: Task '" + dependsOnTask.getTitle() + "' already depends directly or indirectly on Task '" + task.getTitle() + "'.");
        }

        TaskDependency dependency = TaskDependency.builder()
                .task(task)
                .dependsOnTask(dependsOnTask)
                .build();

        TaskDependency saved = taskDependencyRepository.save(dependency);
        return mapToResponse(saved);
    }

    @Transactional
    public void removeDependency(User user, Long taskId, Long dependencyId) {
        Task task = getTaskAndVerifyOwnership(user, taskId);
        TaskDependency dependency = taskDependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new IllegalArgumentException("Dependency not found with ID: " + dependencyId));

        if (!dependency.getTask().getId().equals(task.getId())) {
            throw new IllegalArgumentException("Dependency ID " + dependencyId + " does not belong to Task ID " + taskId);
        }

        taskDependencyRepository.delete(dependency);
    }

    @Transactional(readOnly = true)
    public boolean isTaskBlocked(Long taskId) {
        List<TaskDependency> dependencies = taskDependencyRepository.findByTaskId(taskId);
        return dependencies.stream().anyMatch(dep -> dep.getDependsOnTask().getStatus() != TaskStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public int countUncompletedDependencies(Long taskId) {
        List<TaskDependency> dependencies = taskDependencyRepository.findByTaskId(taskId);
        return (int) dependencies.stream().filter(dep -> dep.getDependsOnTask().getStatus() != TaskStatus.COMPLETED).count();
    }

    @Transactional(readOnly = true)
    public int countDependencies(Long taskId) {
        return (int) taskDependencyRepository.countByTaskId(taskId);
    }

    /**
     * Checks if targetTaskId is reachable from startTaskId through the dependency graph.
     * If reachable, adding targetTaskId -> startTaskId would create a cycle.
     */
    public boolean isReachable(Long startTaskId, Long targetTaskId) {
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();

        queue.add(startTaskId);
        visited.add(startTaskId);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current.equals(targetTaskId)) {
                return true;
            }

            List<TaskDependency> directDeps = taskDependencyRepository.findByTaskId(current);
            for (TaskDependency dep : directDeps) {
                Long next = dep.getDependsOnTask().getId();
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return false;
    }

    private Task getTaskAndVerifyOwnership(User user, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not have permission to access or modify this task.");
        }

        return task;
    }

    public TaskDependencyResponse mapToResponse(TaskDependency dep) {
        Task prerequisite = dep.getDependsOnTask();
        boolean isCompleted = prerequisite.getStatus() == TaskStatus.COMPLETED;
        String categoryName = prerequisite.getCategory() != null ? prerequisite.getCategory().getName() : "";

        return TaskDependencyResponse.builder()
                .id(dep.getId())
                .taskId(dep.getTask().getId())
                .taskTitle(dep.getTask().getTitle())
                .dependsOnTaskId(prerequisite.getId())
                .dependsOnTaskTitle(prerequisite.getTitle())
                .dependsOnTaskPriority(prerequisite.getPriority())
                .dependsOnTaskStatus(prerequisite.getStatus())
                .dependsOnTaskCategory(categoryName)
                .isCompleted(isCompleted)
                .createdAt(dep.getCreatedAt())
                .build();
    }
}
