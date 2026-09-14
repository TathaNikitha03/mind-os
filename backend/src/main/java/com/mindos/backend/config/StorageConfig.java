package com.mindos.backend.config;

import com.mindos.backend.service.storage.LocalStorageService;
import com.mindos.backend.service.storage.S3CompatibleStorageService;
import com.mindos.backend.service.storage.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class StorageConfig {

    @Value("${storage.provider:local}")
    private String storageProvider;

    @Bean
    @Primary
    public StorageService storageService(
            LocalStorageService localStorageService,
            S3CompatibleStorageService s3CompatibleStorageService
    ) {
        if ("s3".equalsIgnoreCase(storageProvider) ||
            "supabase".equalsIgnoreCase(storageProvider) ||
            "r2".equalsIgnoreCase(storageProvider)) {
            return s3CompatibleStorageService;
        }
        return localStorageService;
    }
}
