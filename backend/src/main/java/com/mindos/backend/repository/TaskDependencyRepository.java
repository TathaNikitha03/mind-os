package com.mindos.backend.repository;

import com.mindos.backend.entity.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {
    List<TaskDependency> findByTaskId(Long taskId);
    List<TaskDependency> findByDependsOnTaskId(Long dependsOnTaskId);
    Optional<TaskDependency> findByTaskIdAndDependsOnTaskId(Long taskId, Long dependsOnTaskId);
    boolean existsByTaskIdAndDependsOnTaskId(Long taskId, Long dependsOnTaskId);
    long countByTaskId(Long taskId);
    long countByDependsOnTaskId(Long dependsOnTaskId);
}
