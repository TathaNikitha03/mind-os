package com.mindos.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindos.backend.dto.AiTaskSuggestionResponse;
import com.mindos.backend.dto.TaskSuggestionDto;
import com.mindos.backend.enums.TaskPriority;
import com.mindos.backend.service.llm.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiTaskService {

    private static final Logger log = LoggerFactory.getLogger(AiTaskService.class);

    private final LlmProvider llmProvider;
    private final ObjectMapper objectMapper;

    public static final String TASK_ASSISTANT_SYSTEM_PROMPT = """
            You are a smart Task Assistant for MIND OS.
            Break down the user's goal into a structured, actionable task list.
            
            Return ONLY a raw JSON object with this exact structure (no markdown fences, no backticks, no extra text):
            {
              "analysisSummary": "A brief 1-2 sentence overview of what needs to be done.",
              "suggestedTasks": [
                {
                  "title": "Clear actionable title",
                  "description": "Details on how to accomplish this step",
                  "priority": "HIGH",
                  "estimatedMinutes": 30,
                  "suggestedCategory": "Academics & Studies"
                }
              ]
            }
            """;

    public AiTaskService(LlmProvider llmProvider, ObjectMapper objectMapper) {
        this.llmProvider = llmProvider;
        this.objectMapper = objectMapper;
    }

    public AiTaskSuggestionResponse analyzeGoal(String userGoal) {
        if (userGoal == null || userGoal.trim().isEmpty()) {
            throw new IllegalArgumentException("Goal cannot be blank.");
        }

        String goalTrimmed = userGoal.trim();
        log.info("Analyzing task goal: {}", goalTrimmed);

        // 1. If an external LLM provider is active (OpenAI, Gemini), attempt LLM generation
        if (!"local".equalsIgnoreCase(llmProvider.getProviderName())) {
            try {
                String rawResponse = llmProvider.generateAnswer(
                        TASK_ASSISTANT_SYSTEM_PROMPT,
                        goalTrimmed,
                        "User Goal: " + goalTrimmed
                );

                if (rawResponse != null && !rawResponse.trim().isEmpty()) {
                    String cleanJson = stripMarkdownFences(rawResponse);
                    AiTaskSuggestionResponse response = objectMapper.readValue(cleanJson, AiTaskSuggestionResponse.class);
                    if (response != null && response.getSuggestedTasks() != null && !response.getSuggestedTasks().isEmpty()) {
                        log.info("Successfully generated {} tasks via LLM provider: {}",
                                response.getSuggestedTasks().size(), llmProvider.getProviderName());
                        return response;
                    }
                }
            } catch (Exception e) {
                log.warn("External LLM task decomposition failed ({}), falling back to smart local synthesis", e.getMessage());
            }
        }

        // 2. Deterministic, high-quality local task breakdown engine
        log.info("Generating structured task breakdown via MindOS Local Intelligence Engine for: {}", goalTrimmed);
        return generateLocalTaskBreakdown(goalTrimmed);
    }

    private String stripMarkdownFences(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    public AiTaskSuggestionResponse generateLocalTaskBreakdown(String goal) {
        String lower = goal.toLowerCase();
        AiTaskSuggestionResponse response = new AiTaskSuggestionResponse();
        List<TaskSuggestionDto> tasks = new ArrayList<>();

        if (lower.contains("java") || lower.contains("interview") || lower.contains("dsa") || lower.contains("algorithm") || lower.contains("leetcode")) {
            response.setAnalysisSummary("Structured preparation roadmap covering Java OOP, core collections, algorithmic problem solving, and mock interview practice.");
            tasks.add(new TaskSuggestionDto(
                    "Review Core Java OOP, Collections & Memory Model",
                    "Deep-dive into HashMap internals, ArrayList vs LinkedList, GC algorithms, and Java memory management.",
                    TaskPriority.HIGH, 45, "Academics & Studies"));
            tasks.add(new TaskSuggestionDto(
                    "Solve 3-5 Algorithmic Coding Challenges",
                    "Practice high-frequency interview problems on Arrays, Strings, Two Pointers, and Binary Trees.",
                    TaskPriority.HIGH, 60, "Academics & Studies"));
            tasks.add(new TaskSuggestionDto(
                    "Review Concurrency, Multithreading & Spring Boot Concepts",
                    "Study volatile, synchronized, ExecutorService, Spring bean lifecycles, and dependency injection.",
                    TaskPriority.MEDIUM, 45, "Work & Projects"));
            tasks.add(new TaskSuggestionDto(
                    "Conduct 30-Minute Timed Mock Interview & Behavioral Pitch",
                    "Practice answering 'Tell me about yourself', past architectural decisions, and STAR format project answers.",
                    TaskPriority.MEDIUM, 30, "Academics & Studies"));

        } else if (lower.contains("machine learning") || lower.contains("ml") || lower.contains("lab") || lower.contains("model") || lower.contains("dataset") || lower.contains("ai")) {
            response.setAnalysisSummary("Machine learning lab workflow covering data preprocessing, model tuning, confusion matrix evaluation, and final report compilation.");
            tasks.add(new TaskSuggestionDto(
                    "Data Exploration & Feature Preprocessing",
                    "Inspect missing values, scale numerical features, encode categorical variables, and perform train-test split.",
                    TaskPriority.MEDIUM, 40, "Academics & Studies"));
            tasks.add(new TaskSuggestionDto(
                    "Train & Benchmark Baseline Models",
                    "Run Decision Trees, Random Forests, and Logistic Regression; tune hyper-parameters using cross-validation.",
                    TaskPriority.HIGH, 55, "Academics & Studies"));
            tasks.add(new TaskSuggestionDto(
                    "Evaluate Metrics & Generate Confusion Matrix",
                    "Calculate precision, recall, F1-score, plot confusion matrix heatmap, and record accuracy metrics.",
                    TaskPriority.HIGH, 40, "Academics & Studies"));
            tasks.add(new TaskSuggestionDto(
                    "Compile ML Lab Report & Format PDF Submission",
                    "Document methodology, observation tables, graph screenshots, and final conclusions for submission.",
                    TaskPriority.MEDIUM, 30, "Academics & Studies"));

        } else if (lower.contains("website") || lower.contains("launch") || lower.contains("deploy") || lower.contains("frontend") || lower.contains("backend") || lower.contains("app") || lower.contains("project")) {
            response.setAnalysisSummary("End-to-end launch checklist covering feature completion, responsive UI polish, testing, and production deployment.");
            tasks.add(new TaskSuggestionDto(
                    "Complete Core Feature Integration & Endpoint Wiring",
                    "Connect frontend API handlers to backend services and verify all REST endpoints return expected payloads.",
                    TaskPriority.HIGH, 60, "Work & Projects"));
            tasks.add(new TaskSuggestionDto(
                    "UI Polish, Responsiveness & Cross-Browser Validation",
                    "Check layout on mobile and desktop viewports, refine CSS animations, and test dark mode contrast.",
                    TaskPriority.MEDIUM, 45, "Work & Projects"));
            tasks.add(new TaskSuggestionDto(
                    "Automated Testing & Edge-Case Bug Verification",
                    "Run unit tests, test edge cases (empty states, network timeouts, invalid inputs), and resolve regressions.",
                    TaskPriority.HIGH, 45, "Work & Projects"));
            tasks.add(new TaskSuggestionDto(
                    "Final Production Build, Environment Secrets & Deployment",
                    "Package build artifacts, verify database connection strings, and deploy to hosting server.",
                    TaskPriority.MEDIUM, 35, "Work & Projects"));

        } else if (lower.contains("exam") || lower.contains("study") || lower.contains("test") || lower.contains("quiz") || lower.contains("course") || lower.contains("lecture")) {
            response.setAnalysisSummary("High-yield study sprint utilizing active recall, spaced practice questions, and rapid revision sheets.");
            tasks.add(new TaskSuggestionDto(
                    "High-Yield Concept Review & Formula Sheet",
                    "Review primary textbook chapters, lecture notes, and write down critical formulas and definitions.",
                    TaskPriority.HIGH, 50, "Academics & Studies"));
            tasks.add(new TaskSuggestionDto(
                    "Active Recall Practice: Solve Past Year Papers",
                    "Work through at least 2 full past exam papers under realistic timed examination conditions.",
                    TaskPriority.HIGH, 60, "Academics & Studies"));
            tasks.add(new TaskSuggestionDto(
                    "Address Difficult Concepts & Weak Topic Areas",
                    "Re-read textbook explanations and watch focused tutorials for topics where mistakes were made.",
                    TaskPriority.MEDIUM, 40, "Academics & Studies"));
            tasks.add(new TaskSuggestionDto(
                    "Quick Summary Flashcard Review Before Rest",
                    "Run through 15-minute key term flashcards to reinforce long-term memory consolidation.",
                    TaskPriority.LOW, 20, "Academics & Studies"));

        } else if (lower.contains("fitness") || lower.contains("workout") || lower.contains("gym") || lower.contains("diet") || lower.contains("health") || lower.contains("exercise") || lower.contains("running")) {
            response.setAnalysisSummary("Balanced fitness and wellness routine structured for strength, cardiovascular endurance, and recovery.");
            tasks.add(new TaskSuggestionDto(
                    "Dynamic Warm-Up & Mobility Sequence",
                    "Joint circles, foam rolling, and light cardio to raise heart rate and prepare joints.",
                    TaskPriority.LOW, 15, "Health & Fitness"));
            tasks.add(new TaskSuggestionDto(
                    "Main Strength & Conditioning Workout",
                    "Execute primary compound movements (squats, presses, pulls) followed by targeted accessory sets.",
                    TaskPriority.HIGH, 50, "Health & Fitness"));
            tasks.add(new TaskSuggestionDto(
                    "Hydration, Post-Workout Protein & Meal Prep",
                    "Consume protein-rich post-workout meal and prepare healthy meals for the rest of the day.",
                    TaskPriority.MEDIUM, 30, "Health & Fitness"));
            tasks.add(new TaskSuggestionDto(
                    "Static Stretching, Deep Breathing & Recovery",
                    "15 minutes of lower back and hamstring stretches, hydration check, and sleep routine prep.",
                    TaskPriority.LOW, 15, "Health & Fitness"));

        } else if (lower.contains("trip") || lower.contains("travel") || lower.contains("weekend") || lower.contains("vacation") || lower.contains("event")) {
            response.setAnalysisSummary("Comprehensive trip coordination plan covering itinerary bookings, travel packing, and schedule confirmation.");
            tasks.add(new TaskSuggestionDto(
                    "Confirm Itinerary, Transport & Accommodation Bookings",
                    "Verify reservation codes, check-in times, ticket downloads, and flight or train schedules.",
                    TaskPriority.HIGH, 40, "Personal & Life"));
            tasks.add(new TaskSuggestionDto(
                    "Pack Luggage, Essentials, Chargers & Toiletries",
                    "Follow packing checklist: clothing, electronics, portable power bank, medications, and IDs.",
                    TaskPriority.MEDIUM, 45, "Personal & Life"));
            tasks.add(new TaskSuggestionDto(
                    "House Prep & Pre-Departure Checklist",
                    "Check appliances, take out trash, water plants, lock windows, and set thermostat.",
                    TaskPriority.LOW, 20, "Personal & Life"));

        } else {
            // General multi-phase decomposition
            String cleanTitle = goal.length() > 40 ? goal.substring(0, 37) + "..." : goal;
            response.setAnalysisSummary(String.format("Action-oriented plan to accomplish \"%s\" systematically through structured preparation, focused execution, and final review.", cleanTitle));
            tasks.add(new TaskSuggestionDto(
                    "Phase 1: Clarify Objectives & Requirements for " + cleanTitle,
                    "List all prerequisites, define the exact definition of done, and assemble necessary materials.",
                    TaskPriority.HIGH, 30, "General"));
            tasks.add(new TaskSuggestionDto(
                    "Phase 2: Core Focused Execution & Milestones",
                    "Tackle the most important and high-impact deliverable without distractions.",
                    TaskPriority.HIGH, 60, "General"));
            tasks.add(new TaskSuggestionDto(
                    "Phase 3: Review Quality, Refine & Address Edge Cases",
                    "Check completed work against requirements, correct flaws, and polish the output.",
                    TaskPriority.MEDIUM, 40, "General"));
            tasks.add(new TaskSuggestionDto(
                    "Phase 4: Wrap-Up, Documentation & Next Steps",
                    "Archive project notes, share deliverables if needed, and log achievements in MIND OS.",
                    TaskPriority.LOW, 20, "General"));
        }

        response.setSuggestedTasks(tasks);
        return response;
    }
}
