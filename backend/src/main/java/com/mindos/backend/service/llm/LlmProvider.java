package com.mindos.backend.service.llm;

public interface LlmProvider {

    /**
     * Generate grounded answer using system instructions, user question, and retrieved context.
     *
     * @param systemPrompt System instructions enforcing grounding and knowledge boundaries
     * @param userQuestion The user's query
     * @param context      Retrieved document chunks formatted as context
     * @return Generated answer text
     */
    String generateAnswer(String systemPrompt, String userQuestion, String context);

    /**
     * Provider identifier (e.g. "local", "openai", "gemini")
     */
    String getProviderName();

    /**
     * Active model name (e.g. "gpt-4o-mini", "local-grounded-extractor")
     */
    String getModelName();
}
