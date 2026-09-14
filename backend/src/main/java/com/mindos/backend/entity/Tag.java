package com.mindos.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"}))
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Tag() {}

    public static TagBuilder builder() { return new TagBuilder(); }

    public static class TagBuilder {
        private Long id;
        private User user;
        private String name;

        public TagBuilder id(Long id) { this.id = id; return this; }
        public TagBuilder user(User user) { this.user = user; return this; }
        public TagBuilder name(String name) { this.name = name; return this; }

        public Tag build() {
            Tag t = new Tag();
            t.setId(id);
            t.setUser(user);
            t.setName(name);
            return t;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
