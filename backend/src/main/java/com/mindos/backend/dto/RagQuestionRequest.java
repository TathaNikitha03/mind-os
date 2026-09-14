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
public class RagQuestionRequest {

    @NotBlank(message = "Question cannot be blank")
    @Size(max = 1000, message = "Question cannot exceed 1000 characters")
    private String question;

    private Integer topK;

    private Double threshold;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }

    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }

    public static RagQuestionRequestBuilder builder() {
        return new RagQuestionRequestBuilder();
    }

    public static class RagQuestionRequestBuilder {
        private String question;
        private Integer topK;
        private Double threshold;

        RagQuestionRequestBuilder() {}

        public RagQuestionRequestBuilder question(String question) { this.question = question; return this; }
        public RagQuestionRequestBuilder topK(Integer topK) { this.topK = topK; return this; }
        public RagQuestionRequestBuilder threshold(Double threshold) { this.threshold = threshold; return this; }

        public RagQuestionRequest build() {
            RagQuestionRequest req = new RagQuestionRequest();
            req.setQuestion(this.question);
            req.setTopK(this.topK);
            req.setThreshold(this.threshold);
            return req;
        }
    }
}
