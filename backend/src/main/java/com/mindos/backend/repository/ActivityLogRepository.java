package com.mindos.backend.repository;

import com.mindos.backend.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ActivityLog> findByUserIdAndAction(Long userId, String action);
    List<ActivityLog> findByUserIdAndEntityType(Long userId, String entityType);
}
