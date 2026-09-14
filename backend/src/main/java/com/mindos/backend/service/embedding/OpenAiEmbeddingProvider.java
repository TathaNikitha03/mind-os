package com.mindos.backend.service.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service("openAiEmbeddingProvider")
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingProvider.class);

    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final int dimension;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiEmbeddingProvider(
            @Value("${embedding.api-key:}") String apiKey,
            @Value("${embedding.model:text-embedding-3-small}") String modelName,
            @Value("${embedding.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${embedding.dimension:1536}") int dimension) {
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.baseUrl = baseUrl;
        this.dimension = dimension;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public float[] generateEmbedding(String text) {
        List<float[]> results = generateEmbeddings(List.of(text));
        if (results.isEmpty()) {
            throw new RuntimeException("OpenAI API returned empty embedding vector.");
        }
        return results.get(0);
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI API key is missing. Please configure EMBEDDING_API_KEY.");
        }

        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            Map<String, Object> requestPayload = Map.of(
                    "model", modelName,
                    "input", texts,
                    "encoding_format", "float"
            );

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestPayload, headers);
            String url = baseUrl + "/embeddings";

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("OpenAI API returned HTTP status: " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode dataNode = root.path("data");

            if (!dataNode.isArray() || dataNode.isEmpty()) {
                throw new RuntimeException("OpenAI API response missing 'data' array.");
            }

            List<float[]> embeddings = new ArrayList<>();
            for (JsonNode item : dataNode) {
                JsonNode vecNode = item.path("embedding");
                if (vecNode.isArray()) {
                    float[] vec = new float[vecNode.size()];
                    for (int i = 0; i < vecNode.size(); i++) {
                        vec[i] = (float) vecNode.get(i).asDouble();
                    }
                    embeddings.add(vec);
                }
            }

            return embeddings;
        } catch (Exception e) {
            log.error("Failed to generate embeddings from OpenAI: {}", e.getMessage());
            throw new RuntimeException("Embedding generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public int getDimension() {
        return dimension;
    }
}
