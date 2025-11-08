package com.example.lighthousedummydemo.service;

import com.example.lighthousedummydemo.model.AIResponse;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class LighthouseService {

    @Value("${lighthouse.api.key}")
    private String lighthouseApiKey;

    @Value("${lighthouse.api.url}")
    private String lighthouseApiUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Gson gson = new Gson();

    public void sendTraceToLighthouse(String prompt, AIResponse aiResponse) {
        try {
            // Build trace data
            Map<String, Object> traceData = new HashMap<>();
            traceData.put("prompt", prompt);
            traceData.put("response", aiResponse.getText());
            traceData.put("tokensUsed", aiResponse.getTokensUsed());
            traceData.put("costUsd", aiResponse.getCostUsd());
            traceData.put("latencyMs", aiResponse.getLatencyMs());
            traceData.put("provider", aiResponse.getProvider());

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(lighthouseApiUrl))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", lighthouseApiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(traceData)))
                    .build();

            // Send async (don't block the main request)
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            System.out.println("✅ Trace sent to Lighthouse successfully");
                        } else {
                            System.err.println("❌ Lighthouse trace failed: " + response.statusCode() +
                                    " - " + response.body());
                        }
                    })
                    .exceptionally(e -> {
                        System.err.println("❌ Lighthouse trace error: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("❌ Failed to send trace to Lighthouse: " + e.getMessage());
            e.printStackTrace();
        }
    }
}