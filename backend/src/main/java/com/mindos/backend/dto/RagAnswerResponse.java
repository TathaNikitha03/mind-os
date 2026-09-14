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
public class RagAnswerResponse {

    private String question;
    private String answer;
    private List<RagSource> sources;
    private boolean hasContext;
    private String modelName;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<RagSource> getSources() { return sources; }
    public void setSources(List<RagSource> sources) { this.sources = sources; }

    public boolean isHasContext() { return hasContext; }
    public void setHasContext(boolean hasContext) { this.hasContext = hasContext; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public static RagAnswerResponseBuilder builder() {
        return new RagAnswerResponseBuilder();
    }

    public static class RagAnswerResponseBuilder {
        private String question;
        private String answer;
        private List<RagSource> sources;
        private boolean hasContext;
        private String modelName;

        RagAnswerResponseBuilder() {}

        public RagAnswerResponseBuilder question(String question) { this.question = question; return this; }
        public RagAnswerResponseBuilder answer(String answer) { this.answer = answer; return this; }
        public RagAnswerResponseBuilder sources(List<RagSource> sources) { this.sources = sources; return this; }
        public RagAnswerResponseBuilder hasContext(boolean hasContext) { this.hasContext = hasContext; return this; }
        public RagAnswerResponseBuilder modelName(String modelName) { this.modelName = modelName; return this; }

        public RagAnswerResponse build() {
            RagAnswerResponse resp = new RagAnswerResponse();
            resp.setQuestion(this.question);
            resp.setAnswer(this.answer);
            resp.setSources(this.sources != null ? this.sources : List.of());
            resp.setHasContext(this.hasContext);
            resp.setModelName(this.modelName);
            return resp;
        }
    }
}
