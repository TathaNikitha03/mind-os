package com.mindos.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_interactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String query;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String response;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
