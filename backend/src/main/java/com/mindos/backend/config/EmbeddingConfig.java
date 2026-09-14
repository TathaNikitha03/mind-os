package com.mindos.backend.config;

import com.mindos.backend.service.embedding.EmbeddingProvider;
import com.mindos.backend.service.embedding.LocalDeterministicEmbeddingProvider;
import com.mindos.backend.service.embedding.OpenAiEmbeddingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EmbeddingConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingConfig.class);

    @Bean
    @Primary
    public EmbeddingProvider primaryEmbeddingProvider(
            @Value("${embedding.provider:local}") String provider,
            LocalDeterministicEmbeddingProvider localProvider,
            OpenAiEmbeddingProvider openAiProvider) {

        String selected = provider.trim().toLowerCase();
        log.info("Configuring primary EmbeddingProvider: '{}'", selected);

        if ("openai".equals(selected)) {
            return openAiProvider;
        }

        // Default: local deterministic provider (zero cost, 1536 dim, offline-safe)
        return localProvider;
    }
}
