package com.mindos.backend.service;

import com.mindos.backend.dto.TaskRequest;
import com.mindos.backend.dto.TaskResponse;
import com.mindos.backend.dto.TaskStatisticsResponse;
import com.mindos.backend.entity.Category;
import com.mindos.backend.entity.Tag;
import com.mindos.backend.entity.Task;
import com.mindos.backend.entity.User;
import com.mindos.backend.enums.TaskPriority;
import com.mindos.backend.enums.TaskStatus;
import com.mindos.backend.repository.CategoryRepository;
import com.mindos.backend.repository.TagRepository;
import com.mindos.backend.repository.TaskDependencyRepository;
import com.mindos.backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final TaskDependencyRepository taskDependencyRepository;



    @Transactional(readOnly = true)
    public TaskStatisticsResponse getTaskStatistics(User user) {
        List<Task> tasks = taskRepository.findByUserId(user.getId());
        List<TaskResponse> taskResponses = tasks.stream().map(this::mapToResponse).toList();

        long totalTasks = taskResponses.size();
        long completedTasks = 0;
        long inProgressTasks = 0;
        long todoTasks = 0;
        long overdueTasks = 0;
        long todayTasks = 0;
        long blockedTasks = 0;

        Map<String, Long> priorityBreakdown = new LinkedHashMap<>();
        priorityBreakdown.put("HIGH", 0L);
        priorityBreakdown.put("MEDIUM", 0L);
        priorityBreakdown.put("LOW", 0L);
        priorityBreakdown.put("high", 0L);
        priorityBreakdown.put("medium", 0L);
        priorityBreakdown.put("low", 0L);

        Map<String, Long> categoryBreakdown = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();

        List<TaskResponse> todayList = new ArrayList<>();
        List<TaskResponse> upcomingList = new ArrayList<>();
        List<TaskResponse> overdueList = new ArrayList<>();
        List<TaskResponse> blockedList = new ArrayList<>();

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            TaskResponse resp = taskResponses.get(i);

            // Status counts
            if (task.getStatus() == TaskStatus.COMPLETED) {
                completedTasks++;
            } else if (task.getStatus() == TaskStatus.IN_PROGRESS) {
                inProgressTasks++;
            } else if (task.getStatus() == TaskStatus.TODO) {
                todoTasks++;
            }

            // Priority counts
            if (task.getPriority() != null) {
                String pUpper = task.getPriority().name();
                String pLower = pUpper.toLowerCase();
                priorityBreakdown.put(pUpper, priorityBreakdown.getOrDefault(pUpper, 0L) + 1);
                priorityBreakdown.put(pLower, priorityBreakdown.getOrDefault(pLower, 0L) + 1);
            }

            // Category counts
            String catName = (task.getCategory() != null && task.getCategory().getName() != null && !task.getCategory().getName().isBlank())
                    ? task.getCategory().getName()
                    : "General";
            categoryBreakdown.put(catName, categoryBreakdown.getOrDefault(catName, 0L) + 1);

            // Date & Overdue checks
            boolean isCompleted = task.getStatus() == TaskStatus.COMPLETED;
            LocalDateTime taskDueDate = task.getDueDate();

            if (taskDueDate != null) {
                LocalDate taskLocalDate = taskDueDate.toLocalDate();
                if (taskLocalDate.isEqual(today)) {
                    todayTasks++;
                    todayList.add(resp);
                } else if (taskLocalDate.isBefore(today)) {
                    if (!isCompleted) {
                        overdueTasks++;
                        overdueList.add(resp);
                    }
                } else if (taskLocalDate.isAfter(today)) {
                    if (!isCompleted) {
                        upcomingList.add(resp);
                    }
                }
            }

            // Blocked check
            if (resp.isBlocked() && !isCompleted) {
                blockedTasks++;
                blockedList.add(resp);
            }
        }

        // Sort upcoming tasks: nearest due date first
        upcomingList.sort((a, b) -> {
            if (a.getDueDate() == null || a.getDueDate().isBlank()) return 1;
            if (b.getDueDate() == null || b.getDueDate().isBlank()) return -1;
            return a.getDueDate().compareTo(b.getDueDate());
        });

        // Sort overdue tasks: oldest due date first
        overdueList.sort((a, b) -> {
            if (a.getDueDate() == null || a.getDueDate().isBlank()) return 1;
            if (b.getDueDate() == null || b.getDueDate().isBlank()) return -1;
            return a.getDueDate().compareTo(b.getDueDate());
        });

        // Calculate progress percentage
        double completionPercentage = 0.0;
        if (totalTasks > 0) {
            completionPercentage = Math.round(((double) completedTasks / (double) totalTasks) * 100.0 * 10.0) / 10.0;
        }

        return TaskStatisticsResponse.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .inProgressTasks(inProgressTasks)
                .todoTasks(todoTasks)
                .overdueTasks(overdueTasks)
                .todayTasks(todayTasks)
                .blockedTasks(blockedTasks)
                .completionPercentage(completionPercentage)
                .priorityBreakdown(priorityBreakdown)
                .categoryBreakdown(categoryBreakdown)
                .todayTasksList(todayList)
                .upcomingTasksList(upcomingList)
                .overdueTasksList(overdueList)
                .blockedTasksList(blockedList)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getUserTasks(User user) {
        List<Task> tasks = taskRepository.findByUserId(user.getId());
        return tasks.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(User user, Long taskId) {
        Task task = getTaskAndVerifyOwnership(user, taskId);
        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse createTask(User user, TaskRequest request) {
        Category category = resolveCategory(user, request.getCategory());
        List<Tag> tags = resolveTags(user, request.getTags());

        LocalDateTime dueDate = parseDueDate(request.getDueDate());

        Task task = Task.builder()
                .user(user)
                .category(category)
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO)
                .dueDate(dueDate)
                .estimatedMinutes(request.getEstimatedMinutes())
                .tags(tags)
                .build();

        if (task.getStatus() == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }

        Task saved = taskRepository.save(task);
        return mapToResponse(saved);
    }

    @Transactional
    public TaskResponse updateTask(User user, Long taskId, TaskRequest request) {
        Task task = getTaskAndVerifyOwnership(user, taskId);

        Category category = resolveCategory(user, request.getCategory());
        List<Tag> tags = resolveTags(user, request.getTags());
        LocalDateTime dueDate = parseDueDate(request.getDueDate());

        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        task.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM);
        
        if (request.getStatus() != null) {
            if (request.getStatus() == TaskStatus.COMPLETED && task.getStatus() != TaskStatus.COMPLETED) {
                task.setCompletedAt(LocalDateTime.now());
            } else if (request.getStatus() != TaskStatus.COMPLETED) {
                task.setCompletedAt(null);
            }
            task.setStatus(request.getStatus());
        }

        task.setDueDate(dueDate);
        task.setEstimatedMinutes(request.getEstimatedMinutes());
        task.setCategory(category);
        task.setTags(tags);

        Task updated = taskRepository.save(task);
        return mapToResponse(updated);
    }

    @Transactional
    public TaskResponse completeTask(User user, Long taskId) {
        Task task = getTaskAndVerifyOwnership(user, taskId);
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        Task updated = taskRepository.save(task);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteTask(User user, Long taskId) {
        Task task = getTaskAndVerifyOwnership(user, taskId);
        taskRepository.delete(task);
    }

    private Task getTaskAndVerifyOwnership(User user, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not have permission to access or modify this task.");
        }

        return task;
    }

    private Category resolveCategory(User user, String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return null;
        String name = categoryName.trim();
        Optional<Category> catOpt = categoryRepository.findByUserIdAndName(user.getId(), name);
        if (catOpt.isPresent()) return catOpt.get();

        Category newCat = Category.builder()
                .user(user)
                .name(name)
                .description("Category for " + name)
                .build();
        return categoryRepository.save(newCat);
    }

    @Transactional
    public TaskResponse addTagToTask(User user, Long taskId, Long tagId) {
        Task task = getTaskAndVerifyOwnership(user, taskId);
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found with ID: " + tagId));

        if (!tag.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not have permission to use this tag.");
        }

        if (task.getTags() == null) {
            task.setTags(new ArrayList<>());
        }

        boolean alreadyPresent = task.getTags().stream().anyMatch(t -> t.getId().equals(tag.getId()));
        if (!alreadyPresent) {
            task.getTags().add(tag);
            task = taskRepository.save(task);
        }

        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse removeTagFromTask(User user, Long taskId, Long tagId) {
        Task task = getTaskAndVerifyOwnership(user, taskId);
        if (task.getTags() != null) {
            task.getTags().removeIf(t -> t.getId().equals(tagId));
            task = taskRepository.save(task);
        }
        return mapToResponse(task);
    }

    private List<Tag> resolveTags(User user, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return new ArrayList<>();
        List<Tag> result = new ArrayList<>();
        List<String> seenNames = new ArrayList<>();

        for (String tagName : tagNames) {
            if (tagName == null) continue;
            String name = tagName.trim().replaceAll("^#+", "").toLowerCase();
            if (name.isBlank() || seenNames.contains(name)) continue;
            seenNames.add(name);

            Optional<Tag> tagOpt = tagRepository.findByUserIdAndName(user.getId(), name);
            Tag tag = tagOpt.orElseGet(() -> tagRepository.save(Tag.builder().user(user).name(name).build()));
            result.add(tag);
        }
        return result;
    }

    private LocalDateTime parseDueDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return LocalDateTime.now();
        try {
            if (dateStr.contains("T")) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
            }
            LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return LocalDateTime.of(localDate, LocalTime.of(18, 0));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    public TaskResponse mapToResponse(Task task) {
        List<String> tagList = task.getTags() != null ? task.getTags().stream().map(Tag::getName).toList() : new ArrayList<>();
        String categoryName = task.getCategory() != null ? task.getCategory().getName() : "";

        String dateFormatted = "";
        String timeFormatted = "";
        if (task.getDueDate() != null) {
            dateFormatted = task.getDueDate().toLocalDate().toString();
            timeFormatted = task.getDueDate().toLocalTime().toString().substring(0, 5);
        }

        int dependencyCount = 0;
        int uncompletedCount = 0;
        boolean isBlocked = false;

        if (taskDependencyRepository != null && task.getId() != null) {
            List<com.mindos.backend.entity.TaskDependency> deps = taskDependencyRepository.findByTaskId(task.getId());
            dependencyCount = deps.size();
            uncompletedCount = (int) deps.stream().filter(d -> d.getDependsOnTask().getStatus() != TaskStatus.COMPLETED).count();
            isBlocked = uncompletedCount > 0 && task.getStatus() != TaskStatus.COMPLETED;
        }

        return TaskResponse.builder()
                .id(task.getId())
                .userId(task.getUser().getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .status(task.getStatus())
                .dueDate(dateFormatted)
                .dueTime(timeFormatted)
                .estimatedMinutes(task.getEstimatedMinutes())
                .category(categoryName)
                .tags(tagList)
                .dependencyCount(dependencyCount)
                .uncompletedDependencyCount(uncompletedCount)
                .blocked(isBlocked)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
