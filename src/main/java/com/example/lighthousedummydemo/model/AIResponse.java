package com.example.lighthousedummydemo.model;

public class AIResponse {
    private String text;
    private int tokensUsed;
    private double costUsd;
    private long latencyMs;
    private String provider;

    public AIResponse() {}

    public AIResponse(String text, int tokensUsed, double costUsd, long latencyMs, String provider) {
        this.text = text;
        this.tokensUsed = tokensUsed;
        this.costUsd = costUsd;
        this.latencyMs = latencyMs;
        this.provider = provider;
    }

    // Getters and Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(int tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    public double getCostUsd() {
        return costUsd;
    }

    public void setCostUsd(double costUsd) {
        this.costUsd = costUsd;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}