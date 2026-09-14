package com.mindos.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemanticSearchResponse {

    private String query;
    private List<SemanticSearchResult> results;
    private Integer totalResults;
    private String modelName;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public List<SemanticSearchResult> getResults() { return results; }
    public void setResults(List<SemanticSearchResult> results) {
        this.results = results;
        this.totalResults = results != null ? results.size() : 0;
    }

    public Integer getTotalResults() { return totalResults; }
    public void setTotalResults(Integer totalResults) { this.totalResults = totalResults; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public static SemanticSearchResponseBuilder builder() {
        return new SemanticSearchResponseBuilder();
    }

    public static class SemanticSearchResponseBuilder {
        private String query;
        private List<SemanticSearchResult> results;
        private Integer totalResults;
        private String modelName;

        SemanticSearchResponseBuilder() {}

        public SemanticSearchResponseBuilder query(String query) { this.query = query; return this; }
        public SemanticSearchResponseBuilder results(List<SemanticSearchResult> results) {
            this.results = results;
            this.totalResults = results != null ? results.size() : 0;
            return this;
        }
        public SemanticSearchResponseBuilder totalResults(Integer totalResults) { this.totalResults = totalResults; return this; }
        public SemanticSearchResponseBuilder modelName(String modelName) { this.modelName = modelName; return this; }

        public SemanticSearchResponse build() {
            SemanticSearchResponse resp = new SemanticSearchResponse();
            resp.setQuery(this.query);
            resp.setResults(this.results != null ? this.results : List.of());
            resp.setTotalResults(this.totalResults != null ? this.totalResults : (this.results != null ? this.results.size() : 0));
            resp.setModelName(this.modelName);
            return resp;
        }
    }
}
