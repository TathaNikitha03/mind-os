package com.mindos.backend.repository;

import com.mindos.backend.entity.Reminder;
import com.mindos.backend.enums.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUserId(Long userId);
    List<Reminder> findByTaskId(Long taskId);
    List<Reminder> findByStatus(ReminderStatus status);
    List<Reminder> findByStatusAndReminderTimeBefore(ReminderStatus status, LocalDateTime time);
}
