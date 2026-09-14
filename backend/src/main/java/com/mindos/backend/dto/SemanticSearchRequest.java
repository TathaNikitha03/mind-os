package com.mindos.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemanticSearchRequest {

    @NotBlank(message = "Search query is required")
    @Size(max = 1000, message = "Query cannot exceed 1000 characters")
    private String query;

    private Integer topK;

    private Double threshold;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }

    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }

    public static SemanticSearchRequestBuilder builder() {
        return new SemanticSearchRequestBuilder();
    }

    public static class SemanticSearchRequestBuilder {
        private String query;
        private Integer topK;
        private Double threshold;

        SemanticSearchRequestBuilder() {}

        public SemanticSearchRequestBuilder query(String query) { this.query = query; return this; }
        public SemanticSearchRequestBuilder topK(Integer topK) { this.topK = topK; return this; }
        public SemanticSearchRequestBuilder threshold(Double threshold) { this.threshold = threshold; return this; }

        public SemanticSearchRequest build() {
            SemanticSearchRequest req = new SemanticSearchRequest();
            req.setQuery(this.query);
            req.setTopK(this.topK);
            req.setThreshold(this.threshold);
            return req;
        }
    }
}
