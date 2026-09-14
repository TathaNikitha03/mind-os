package com.mindos.backend.repository;

import com.mindos.backend.entity.Task;
import com.mindos.backend.enums.TaskPriority;
import com.mindos.backend.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserId(Long userId);
    List<Task> findByUserIdAndStatus(Long userId, TaskStatus status);
    List<Task> findByUserIdAndPriority(Long userId, TaskPriority priority);
    List<Task> findByUserIdAndStatusAndPriority(Long userId, TaskStatus status, TaskPriority priority);
    List<Task> findByUserIdAndDueDateBefore(Long userId, LocalDateTime dateTime);
    List<Task> findByCategoryId(Long categoryId);
    long countByCategoryId(Long categoryId);
    List<Task> findByTagsId(Long tagId);
    long countByTagsId(Long tagId);
    List<Task> findByUserIdAndTagsId(Long userId, Long tagId);
    long countByUserIdAndStatus(Long userId, TaskStatus status);
}

