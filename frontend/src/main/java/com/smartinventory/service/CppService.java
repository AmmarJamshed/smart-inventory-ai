package com.smartinventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.model.InventoryItem;
import com.smartinventory.model.Recommendation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for the C++ Analytics & Matching Engine (port 8080).
 * Handles analytics calculation, trade matching, and recommendations.
 */
public class CppService {

    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient   httpClient;
    private final ObjectMapper mapper;

    public CppService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        this.mapper = new ObjectMapper();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Analytics
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getAnalytics(List<InventoryItem> items) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        List<Map<String, Object>> inventory = new ArrayList<>();

        for (InventoryItem item : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("id",        item.getId());
            m.put("type",      safeStr(item.getType()));
            m.put("item",      safeStr(item.getItem()));
            m.put("quantity",  safeStr(item.getQuantity()));
            m.put("price",     item.getPrice());
            m.put("source",    safeStr(item.getSource()));
            m.put("timestamp", safeStr(item.getTimestamp()));
            inventory.add(m);
        }
        payload.put("inventory", inventory);

        JsonNode result = post("/analytics", payload);
        return mapper.convertValue(result, Map.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recommendations
    // ─────────────────────────────────────────────────────────────────────────

    public List<Recommendation> getRecommendations(List<InventoryItem> items) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        List<Map<String, Object>> inventory = new ArrayList<>();

        for (InventoryItem item : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("type",     safeStr(item.getType()));
            m.put("item",     safeStr(item.getItem()));
            m.put("quantity", safeStr(item.getQuantity()));
            m.put("price",    item.getPrice());
            m.put("source",   safeStr(item.getSource()));
            inventory.add(m);
        }
        payload.put("inventory", inventory);

        JsonNode result = post("/recommendations", payload);
        return parseRecommendations(result.get("recommendations"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trade Matching
    // ─────────────────────────────────────────────────────────────────────────

    public List<Recommendation> findMatches(String type, String item,
                                             String quantity, double targetPrice) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type",          type.toLowerCase());
        payload.put("item",          item);
        payload.put("quantity",      quantity);
        payload.put("target_price",  targetPrice);
        payload.put("location_zone", 3); // default zone

        JsonNode result = post("/match", payload);
        return parseRecommendations(result.get("matches"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reputation
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> calculateReputation(int totalTx, int successTx,
                                                    int disputeTotal, int disputeWins,
                                                    double avgResponseHours, double deliveryRate,
                                                    int daysActive) throws Exception {
        Map<String, Object> payload = Map.of(
            "total_transactions",     totalTx,
            "successful_transactions",successTx,
            "dispute_total",          disputeTotal,
            "dispute_wins",           disputeWins,
            "avg_response_hours",     avgResponseHours,
            "delivery_rate",          deliveryRate,
            "days_active",            daysActive
        );
        JsonNode result = post("/reputation", payload);
        return mapper.convertValue(result, Map.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Health
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

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private JsonNode post(String path, Object payload) throws Exception {
        String body = mapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return mapper.readTree(response.body());
        }
        throw new RuntimeException("Engine error " + response.statusCode() + ": " + response.body());
    }

    private List<Recommendation> parseRecommendations(JsonNode node) {
        List<Recommendation> result = new ArrayList<>();
        if (node == null || !node.isArray()) return result;

        for (JsonNode rec : node) {
            Recommendation r = new Recommendation();
            r.setType(     rec.has("type")      ? rec.get("type").asText()      : "Match");
            r.setPseudonym(rec.has("pseudonym")  ? rec.get("pseudonym").asText() : "Unknown");
            r.setScore(    rec.has("score")      ? rec.get("score").asDouble()   : 0.0);
            r.setReasoning(rec.has("reasoning")  ? rec.get("reasoning").asText() : "");
            result.add(r);
        }
        return result;
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }
}
