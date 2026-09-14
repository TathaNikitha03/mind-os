package com.mindos.backend.config;

import com.mindos.backend.service.llm.LlmProvider;
import com.mindos.backend.service.llm.LocalDeterministicLlmProvider;
import com.mindos.backend.service.llm.OpenAiLlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Bean
    @Primary
    public LlmProvider primaryLlmProvider(
            @Value("${llm.provider:local}") String provider,
            LocalDeterministicLlmProvider localProvider,
            OpenAiLlmProvider openAiProvider) {

        String selected = provider.trim().toLowerCase();
        log.info("Configuring primary LlmProvider: '{}'", selected);

        if ("openai".equals(selected)) {
            return openAiProvider;
        }

        // Default: local deterministic provider (offline safe, zero API cost)
        return localProvider;
    }
}
