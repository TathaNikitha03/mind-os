package com.mindos.backend.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Store an uploaded file and return the unique stored path / object key / URL reference.
     *
     * @param userId The ID of the authenticated user
     * @param file   The multipart file to store
     * @return Stored file reference (e.g. "user_101/uuid_filename.pdf" or S3 object key)
     */
    String storeFile(Long userId, MultipartFile file);

    /**
     * Load a stored file as a Spring Resource for downloading or streaming.
     *
     * @param fileReference Stored file reference or object key
     * @return Spring Resource
     */
    Resource loadFileAsResource(String fileReference);

    /**
     * Delete the physical file from storage.
     *
     * @param fileReference Stored file reference or object key
     * @return True if deleted or already absent
     */
    boolean deleteFile(String fileReference);

    /**
     * Return the active storage provider name (e.g. "LOCAL", "S3", "SUPABASE", "R2").
     */
    String getStorageType();
}
