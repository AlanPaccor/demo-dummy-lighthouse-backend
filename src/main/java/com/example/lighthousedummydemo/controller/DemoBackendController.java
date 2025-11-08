package com.example.lighthousedummydemo.controller;

import com.example.lighthousedummydemo.model.AIResponse;
import com.example.lighthousedummydemo.service.AIService;
import com.example.lighthousedummydemo.service.LighthouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@CrossOrigin(origins = "*")
public class DemoBackendController {

    @Autowired
    private AIService aiService;

    @Autowired
    private LighthouseService lighthouseService;

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> executeQuery(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            String databaseConnectionId = request.get("databaseConnectionId"); // For AI context only

            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Prompt is required", "success", false));
            }

            // Call AI with database context (for better responses)
            AIResponse aiResponse = aiService.callAI(prompt, databaseConnectionId);

            // Send simple trace to Lighthouse (no hallucination detection)
            lighthouseService.sendTraceToLighthouse(prompt, aiResponse);

            // Return response to frontend
            Map<String, Object> response = new HashMap<>();
            response.put("response", aiResponse.getText());
            response.put("success", true);
            response.put("tokensUsed", aiResponse.getTokensUsed());
            response.put("costUsd", aiResponse.getCostUsd());
            response.put("latencyMs", aiResponse.getLatencyMs());
            response.put("provider", aiResponse.getProvider());
            // NO confidenceScore - that's only in the dashboard

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("success", false);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}