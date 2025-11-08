package com.example.lighthousedummydemo.service;

import com.example.lighthousedummydemo.model.AIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AIService {

    @Autowired
    private DatabaseService databaseService;

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}")
    private String geminiApiUrl;

    public AIResponse callAI(String prompt, String databaseConnectionId) {
        long startTime = System.currentTimeMillis();

        try {
            // Get database context if databaseConnectionId is provided
            String databaseContext = "";
            if (databaseConnectionId != null && !databaseConnectionId.trim().isEmpty()) {
                try {
                    databaseContext = databaseService.getDatabaseContext(prompt, databaseConnectionId);
                    System.out.println("Database context retrieved: " +
                            databaseContext.substring(0, Math.min(200, databaseContext.length())) + "...");
                } catch (Exception e) {
                    System.err.println("Error getting database context: " + e.getMessage());
                    databaseContext = "Error retrieving database context: " + e.getMessage();
                }
            }

            // Build enhanced prompt with database context
            String enhancedPrompt = buildEnhancedPrompt(prompt, databaseContext);

            // Prepare request body
            Map<String, Object> contents = new HashMap<>();
            contents.put("contents", new Object[]{
                    Map.of("parts", new Object[]{Map.of("text", enhancedPrompt)})
            });

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(contents, headers);

            // Make API call with ?key= param instead of Bearer token
            RestTemplate restTemplate = new RestTemplate();
            String fullUrl = geminiApiUrl + "?key=" + geminiApiKey;

            ResponseEntity<Map> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            String textResponse = extractGeminiResponse(response.getBody());

            long latencyMs = System.currentTimeMillis() - startTime;
            int tokensUsed = estimateTokens(enhancedPrompt, textResponse);

            // Calculate cost based on tokens and provider
            double costUsd = calculateCost(tokensUsed, "gemini");

            return new AIResponse(textResponse, tokensUsed, costUsd, latencyMs, "gemini");

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            return new AIResponse(
                    "Error calling Gemini API: " + e.getMessage(),
                    0,
                    0.0,
                    latencyMs,
                    "gemini"
            );
        }
    }

    private String buildEnhancedPrompt(String userPrompt, String databaseContext) {
        if (databaseContext == null || databaseContext.trim().isEmpty()) {
            return userPrompt;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("You have access to a database with the following context:\n\n");
        prompt.append(databaseContext);
        prompt.append("\n\n");
        prompt.append("User Question: ").append(userPrompt);
        prompt.append("\n\n");
        prompt.append("Please answer the user's question based on the database context provided above. ");
        prompt.append("If the information is not available in the database context, please say so clearly. ");
        prompt.append("Use the actual data from the database to answer the question.");

        return prompt.toString();
    }

    private String extractGeminiResponse(Map responseBody) {
        try {
            var candidates = (Iterable<Map>) responseBody.get("candidates");
            if (candidates != null) {
                for (Map candidate : candidates) {
                    Map content = (Map) candidate.get("content");
                    if (content != null && content.get("parts") instanceof Iterable parts) {
                        for (Object part : parts) {
                            Map partMap = (Map) part;
                            if (partMap.containsKey("text")) {
                                return (String) partMap.get("text");
                            }
                        }
                    }
                }
            }
            return "No response text found from Gemini API.";
        } catch (Exception e) {
            return "Error parsing Gemini response: " + e.getMessage();
        }
    }

    private int estimateTokens(String prompt, String response) {
        return (int) Math.ceil((prompt.length() + response.length()) / 4.0);
    }

    /**
     * Calculate cost in USD based on tokens and provider
     * Gemini 2.0 Flash pricing (as of 2024):
     * - Input: $0.075 per 1M tokens
     * - Output: $0.30 per 1M tokens
     */
    private double calculateCost(int totalTokens, String provider) {
        if (totalTokens <= 0) {
            return 0.0;
        }

        // For Gemini pricing
        if ("gemini".equalsIgnoreCase(provider)) {
            // Estimate: 70% input tokens, 30% output tokens (typical ratio)
            int inputTokens = (int) (totalTokens * 0.7);
            int outputTokens = totalTokens - inputTokens;

            // Gemini 2.0 Flash pricing
            double inputCost = (inputTokens / 1_000_000.0) * 0.075;
            double outputCost = (outputTokens / 1_000_000.0) * 0.30;

            return inputCost + outputCost;
        }

        // Default fallback (very rough estimate)
        return (totalTokens / 1_000_000.0) * 0.10; // $0.10 per 1M tokens average
    }
}