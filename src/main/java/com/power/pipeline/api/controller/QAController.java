package com.power.pipeline.api.controller;

import com.power.pipeline.api.service.DataQAService;
import com.power.pipeline.api.service.BedrockQAService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QAController {

    private final DataQAService qaService;
    private final BedrockQAService bedrockQAService;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(
            @RequestBody Map<String, String> request,
            @RequestParam(defaultValue = "rule") String mode) {

        String question = request.get("question");

        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Question is required"));
        }

        // Choose service based on mode
        String answer;
        String actualMode;

        if ("llm".equalsIgnoreCase(mode) || "bedrock".equalsIgnoreCase(mode)) {
            answer = bedrockQAService.answerQuestion(question);
            actualMode = "llm (AWS Bedrock - Claude Sonnet 4.5)";
        } else {
            answer = qaService.answerQuestion(question);
            actualMode = "rule-based (default)";
        }

        return ResponseEntity.ok(Map.of(
                "question", question,
                "answer", answer,
                "mode", actualMode
        ));
    }

    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> getHelp() {
        return ResponseEntity.ok(Map.of(
                "description", "Q&A agent that answers questions about the power data and communicates data quality limitations",
                "usage", "POST /api/qa/ask with JSON body: {\"question\": \"your question here\"}",
                "modes", Map.of(
                    "rule", "Fast keyword-based matching (default, production-ready)",
                    "llm", "AWS Bedrock Claude Sonnet 4.5 (optional, requires AWS setup)"
                ),
                "examples", Map.of(
                    "default", "POST /api/qa/ask (uses rule-based)",
                    "llm", "POST /api/qa/ask?mode=llm (uses AWS Bedrock)"
                ),
                "exampleQuestions", new String[]{
                        "Which state had the highest shortage?",
                        "What is the total shortage?",
                        "How reliable is this data?",
                        "What are the data sources?",
                        "What are the limitations?"
                }
        ));
    }
}
