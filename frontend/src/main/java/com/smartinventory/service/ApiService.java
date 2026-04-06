package com.smartinventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.model.ParsedResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Client for the Python FastAPI AI service (port 8000).
 * Handles NLP parsing and inventory persistence via the AI layer.
 */
public class ApiService {

    private static final String BASE_URL = "http://localhost:8000";

    private final HttpClient   httpClient;
    private final ObjectMapper mapper;

    public ApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NLP Parsing
    // ─────────────────────────────────────────────────────────────────────────

    public ParsedResult parseText(String text) throws Exception {
        String body = mapper.writeValueAsString(Map.of("text", text));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/parse"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(35)) // Ollama can take ~30s
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return mapper.readValue(response.body(), ParsedResult.class);
        }
        throw new RuntimeException("AI Service error " + response.statusCode() + ": " + response.body());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inventory persistence via Python layer
    // ─────────────────────────────────────────────────────────────────────────

    public void saveInventoryViaPython(Map<String, Object> item) throws Exception {
        String body = mapper.writeValueAsString(item);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/inventory"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Save error " + response.statusCode() + ": " + response.body());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Health check
    // ─────────────────────────────────────────────────────────────────────────

    public boolean checkHealth() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
