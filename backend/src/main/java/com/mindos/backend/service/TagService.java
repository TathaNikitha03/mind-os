package com.mindos.backend.service;

import com.mindos.backend.dto.TagRequest;
import com.mindos.backend.dto.TagResponse;
import com.mindos.backend.entity.Tag;
import com.mindos.backend.entity.Task;
import com.mindos.backend.entity.User;
import com.mindos.backend.repository.TagRepository;
import com.mindos.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final TaskRepository taskRepository;

    public TagService(TagRepository tagRepository, TaskRepository taskRepository) {
        this.tagRepository = tagRepository;
        this.taskRepository = taskRepository;
    }

    public static String normalizeTagName(String rawName) {
        if (rawName == null) return "";
        return rawName.trim().replaceAll("^#+", "").toLowerCase();
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getUserTags(User user) {
        List<Tag> tags = tagRepository.findByUserIdOrderByNameAsc(user.getId());
        return tags.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public TagResponse getTagById(User user, Long tagId) {
        Tag tag = getTagAndVerifyOwnership(user, tagId);
        return mapToResponse(tag);
    }

    @Transactional
    public TagResponse createTag(User user, TagRequest request) {
        if (request == null || request.getName() == null) {
            throw new IllegalArgumentException("Tag name cannot be empty.");
        }

        String normalizedName = normalizeTagName(request.getName());
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be empty.");
        }

        if (tagRepository.existsByUserIdAndName(user.getId(), normalizedName)) {
            throw new IllegalArgumentException("Tag '" + normalizedName + "' already exists for this user.");
        }

        Tag tag = Tag.builder()
                .user(user)
                .name(normalizedName)
                .build();

        Tag saved = tagRepository.save(tag);
        return mapToResponse(saved);
    }

    @Transactional
    public TagResponse updateTag(User user, Long tagId, TagRequest request) {
        Tag tag = getTagAndVerifyOwnership(user, tagId);

        if (request == null || request.getName() == null) {
            throw new IllegalArgumentException("Tag name cannot be empty.");
        }

        String normalizedName = normalizeTagName(request.getName());
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be empty.");
        }

        if (!tag.getName().equalsIgnoreCase(normalizedName) &&
                tagRepository.existsByUserIdAndName(user.getId(), normalizedName)) {
            throw new IllegalArgumentException("Tag '" + normalizedName + "' already exists for this user.");
        }

        tag.setName(normalizedName);
        Tag updated = tagRepository.save(tag);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteTag(User user, Long tagId) {
        Tag tag = getTagAndVerifyOwnership(user, tagId);

        // Disassociate tag from all tasks belonging to this tag without deleting the tasks
        List<Task> tasksWithTag = taskRepository.findByTagsId(tagId);
        for (Task task : tasksWithTag) {
            task.getTags().removeIf(t -> t.getId().equals(tagId));
            taskRepository.save(task);
        }

        tagRepository.delete(tag);
    }

    public Tag getTagAndVerifyOwnership(User user, Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found with ID: " + tagId));

        if (!tag.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not have permission to access or modify this tag.");
        }

        return tag;
    }

    public TagResponse mapToResponse(Tag tag) {
        long taskCount = taskRepository.countByTagsId(tag.getId());
        return TagResponse.builder()
                .id(tag.getId())
                .userId(tag.getUser().getId())
                .name(tag.getName())
                .createdAt(tag.getCreatedAt())
                .taskCount(taskCount)
                .build();
    }
}
