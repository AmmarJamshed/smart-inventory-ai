/*
 * Smart Inventory AI — C++ Analytics & Matching Engine
 * Port: 8080
 * Requires: httplib.h (cpp-httplib), json.hpp (nlohmann/json)
 * Download via setup.bat or manually place in cpp-engine/include/
 */

#include "httplib.h"
#include "json.hpp"
#include "analytics.h"
#include "matching.h"
#include "reputation.h"

#include <iostream>
#include <sstream>
#include <chrono>
#include <iomanip>

using json = nlohmann::json;

// ─────────────────────────────────────────────────────────────────────────────
// JSON deserialization helpers
// ─────────────────────────────────────────────────────────────────────────────

InventoryRecord record_from_json(const json& j) {
    InventoryRecord r{};
    r.id        = j.value("id", 0);
    r.type      = j.value("type", "unknown");
    r.item      = j.value("item", "");
    r.quantity  = j.value("quantity", "1");
    r.price     = j.value("price", 0.0);
    r.source    = j.value("source", "");
    r.timestamp = j.value("timestamp", "");
    return r;
}

TradeOffer offer_from_json(const json& j) {
    TradeOffer o{};
    o.offer_id      = j.value("offer_id", "");
    o.user_did      = j.value("user_did", "");
    o.pseudonym     = j.value("pseudonym", "Unknown");
    o.type          = j.value("type", "sell");
    o.item          = j.value("item", "");
    o.quantity      = j.value("quantity", "1");
    o.target_price  = j.value("target_price", 0.0);
    o.location_zone = j.value("location_zone", 5);
    o.urgency       = j.value("urgency", 0.5);
    o.freshness     = j.value("freshness", 1.0);
    o.reputation    = j.value("reputation_score", 50.0);
    return o;
}

// ─────────────────────────────────────────────────────────────────────────────
// JSON serialization helpers
// ─────────────────────────────────────────────────────────────────────────────

json analytics_to_json(const AnalyticsResult& a) {
    return {
        {"total_items",         a.total_items},
        {"bought_count",        a.bought_count},
        {"sold_count",          a.sold_count},
        {"waste_count",         a.waste_count},
        {"transfer_count",      a.transfer_count},
        {"waste_percentage",    std::round(a.waste_percentage * 10) / 10},
        {"total_bought_value",  std::round(a.total_bought_value * 100) / 100},
        {"total_sold_value",    std::round(a.total_sold_value * 100) / 100},
        {"avg_price",           std::round(a.avg_price * 100) / 100},
        {"today_count",         a.today_count},
        {"lost_value",          std::round(a.lost_value * 100) / 100},
    };
}

json match_to_json(const MatchResult& m) {
    return {
        {"pseudonym",        m.pseudonym},
        {"type",             m.type},
        {"score",            std::round(m.score * 1000) / 1000},
        {"price_advantage",  std::round(m.price_advantage * 1000) / 1000},
        {"distance_score",   std::round(m.distance_score * 1000) / 1000},
        {"reputation_score", std::round(m.reputation_score * 1000) / 1000},
        {"reasoning",        m.reasoning},
        {"offer_id",         m.offer_id},
    };
}

json reputation_to_json(const ReputationResult& r) {
    return {
        {"score",                std::round(r.score * 10) / 10},
        {"tier",                 r.tier},
        {"badge",                r.badge},
        {"success_rate",         std::round(r.success_rate * 1000) / 1000},
        {"dispute_win_rate",     std::round(r.dispute_win_rate * 1000) / 1000},
        {"response_speed_score", std::round(r.response_speed_score * 1000) / 1000},
        {"delivery_reliability", std::round(r.delivery_reliability * 1000) / 1000},
        {"activity_level",       std::round(r.activity_level * 1000) / 1000},
    };
}

// ─────────────────────────────────────────────────────────────────────────────
// Logging helper
// ─────────────────────────────────────────────────────────────────────────────

void log(const std::string& method, const std::string& path, int status) {
    auto now = std::chrono::system_clock::now();
    auto t   = std::chrono::system_clock::to_time_t(now);
    struct tm* tm_info = localtime(&t);
    char buf[20];
    strftime(buf, sizeof(buf), "%H:%M:%S", tm_info);
    std::cout << "[" << buf << "] " << method << " " << path << " → " << status << "\n";
}

// ─────────────────────────────────────────────────────────────────────────────
// Mock trade offers (fallback if no offers provided)
// ─────────────────────────────────────────────────────────────────────────────

std::vector<TradeOffer> get_default_offers() {
    return {
        {"off-001","did:local:t042","Trader #042",  "sell","chicken", "50kg", 620.0, 2, 0.8, 0.95, 88.5},
        {"off-002","did:local:m118","Merchant #118","buy", "chicken", "20kg", 650.0, 3, 0.7, 0.90, 72.0},
        {"off-003","did:local:s007","Supplier #007","sell","beef",    "100kg",850.0, 1, 0.5, 0.85, 95.2},
        {"off-004","did:local:b033","Buyer #033",   "buy", "beef",    "10kg", 900.0, 4, 0.9, 0.80, 61.5},
        {"off-005","did:local:w009","Wholesaler #09","sell","mutton", "200kg",1100.0,2, 0.4, 0.70, 83.0},
        {"off-006","did:local:t042","Trader #042",  "sell","rice",   "500kg",110.0, 2, 0.3, 0.60, 88.5},
        {"off-007","did:local:s007","Supplier #007","sell","flour",  "300kg", 85.0, 1, 0.5, 0.75, 95.2},
        {"off-008","did:local:m118","Merchant #118","buy", "vegetables","100kg",80.0,3,0.8, 0.95, 72.0},
        {"off-009","did:local:w009","Wholesaler #09","sell","oil",   "50L",  220.0, 2, 0.6, 0.88, 83.0},
        {"off-010","did:local:b033","Buyer #033",   "buy", "eggs",   "20dz", 180.0, 4, 0.7, 0.92, 61.5},
    };
}

// ─────────────────────────────────────────────────────────────────────────────
// Recommendation generation from inventory data
// ─────────────────────────────────────────────────────────────────────────────

json generate_recommendations(const std::vector<InventoryRecord>& records) {
    json recs = json::array();
    if (records.empty()) return recs;

    // Find items with high waste → recommend selling quickly
    std::map<std::string, int> waste_count, total_count;
    for (const auto& r : records) {
        total_count[r.item]++;
        if (r.type == "waste") waste_count[r.item]++;
    }

    for (const auto& [item, total] : total_count) {
        int waste = waste_count.count(item) ? waste_count[item] : 0;
        double waste_pct = (double)waste / total * 100.0;

        if (waste_pct > 20.0) {
            recs.push_back({
                {"type",       "Sell Urgently"},
                {"pseudonym",  "Buyer #033"},
                {"score",      0.82},
                {"reasoning",  item + " has " + std::to_string((int)waste_pct) + "% waste. "
                               "Buyer #033 is offering ₨900/kg nearby. High urgency recommended."},
            });
        }
    }

    // Find most purchased item → recommend best supplier
    std::string top_item;
    int top_count = 0;
    for (const auto& [item, count] : total_count) {
        if (count > top_count) { top_count = count; top_item = item; }
    }
    if (!top_item.empty()) {
        recs.push_back({
            {"type",       "Best Supplier"},
            {"pseudonym",  "Supplier #007"},
            {"score",      0.95},
            {"reasoning",  "Supplier #007 offers " + top_item + " at competitive prices. "
                           "Reputation ★95.2. Fastest response time in your zone."},
        });
    }

    if (recs.empty()) {
        recs.push_back({
            {"type",      "Best Seller Nearby"},
            {"pseudonym", "Trader #042"},
            {"score",     0.88},
            {"reasoning", "Trader #042 has consistent supply at good prices. "
                          "Distance score: high. Trust score: ★88.5."},
        });
    }

    return recs;
}

// ─────────────────────────────────────────────────────────────────────────────
// Main: REST server setup
// ─────────────────────────────────────────────────────────────────────────────

int main() {
    httplib::Server svr;

    // CORS
    svr.set_default_headers({
        {"Access-Control-Allow-Origin",  "*"},
        {"Access-Control-Allow-Headers", "Content-Type"},
        {"Access-Control-Allow-Methods", "GET, POST, OPTIONS"},
    });
    svr.Options(".*", [](const httplib::Request&, httplib::Response& res) {
        res.status = 204;
    });

    // ── Health check ─────────────────────────────────────────────────────────
    svr.Get("/health", [](const httplib::Request&, httplib::Response& res) {
        res.set_content(R"({"status":"ok","service":"C++ Engine","version":"1.0.0"})",
                        "application/json");
        log("GET", "/health", 200);
    });

    // ── Analytics ─────────────────────────────────────────────────────────────
    svr.Post("/analytics", [](const httplib::Request& req, httplib::Response& res) {
        try {
            auto body = json::parse(req.body);
            std::vector<InventoryRecord> records;

            if (body.contains("inventory") && body["inventory"].is_array()) {
                for (const auto& item : body["inventory"])
                    records.push_back(record_from_json(item));
            }

            auto analytics = calculate_analytics(records);
            auto categories = calculate_category_stats(records);

            json cat_arr = json::array();
            for (const auto& c : categories) {
                cat_arr.push_back({
                    {"item",        c.item},
                    {"count",       c.count},
                    {"total_value", std::round(c.total_value * 100) / 100},
                    {"waste_pct",   std::round(c.waste_pct * 10) / 10},
                });
            }

            json response = analytics_to_json(analytics);
            response["categories"] = cat_arr;
            res.set_content(response.dump(), "application/json");
            log("POST", "/analytics", 200);
        } catch (const std::exception& e) {
            res.status = 400;
            res.set_content(json{{"error", e.what()}}.dump(), "application/json");
            log("POST", "/analytics", 400);
        }
    });

    // ── Match ─────────────────────────────────────────────────────────────────
    svr.Post("/match", [](const httplib::Request& req, httplib::Response& res) {
        try {
            auto body = json::parse(req.body);

            MatchRequest match_req{};
            match_req.type          = body.value("type", "buy");
            match_req.item          = body.value("item", "");
            match_req.quantity      = body.value("quantity", "1");
            match_req.target_price  = body.value("target_price", 0.0);
            match_req.location_zone = body.value("location_zone", 3);
            match_req.urgency       = body.value("urgency", 0.5);

            // Use provided offers or fall back to defaults
            std::vector<TradeOffer> offers;
            if (body.contains("offers") && body["offers"].is_array()) {
                for (const auto& o : body["offers"])
                    offers.push_back(offer_from_json(o));
            } else {
                offers = get_default_offers();
            }

            // Filter by item name (fuzzy: substring match)
            std::string item_lower = match_req.item;
            std::transform(item_lower.begin(), item_lower.end(), item_lower.begin(), ::tolower);

            std::vector<TradeOffer> filtered;
            for (const auto& o : offers) {
                std::string o_item = o.item;
                std::transform(o_item.begin(), o_item.end(), o_item.begin(), ::tolower);
                if (item_lower.empty() || o_item.find(item_lower) != std::string::npos)
                    filtered.push_back(o);
            }

            auto matches = find_best_matches(filtered, match_req);

            json matches_arr = json::array();
            for (const auto& m : matches)
                matches_arr.push_back(match_to_json(m));

            res.set_content(json{{"matches", matches_arr}}.dump(), "application/json");
            log("POST", "/match", 200);
        } catch (const std::exception& e) {
            res.status = 400;
            res.set_content(json{{"error", e.what()}}.dump(), "application/json");
            log("POST", "/match", 400);
        }
    });

    // ── Reputation ────────────────────────────────────────────────────────────
    svr.Post("/reputation", [](const httplib::Request& req, httplib::Response& res) {
        try {
            auto body = json::parse(req.body);

            ReputationInput input{};
            input.total_transactions    = body.value("total_transactions", 0);
            input.successful_transactions = body.value("successful_transactions", 0);
            input.dispute_total         = body.value("dispute_total", 0);
            input.dispute_wins          = body.value("dispute_wins", 0);
            input.avg_response_hours    = body.value("avg_response_hours", 24.0);
            input.delivery_rate         = body.value("delivery_rate", 0.8);
            input.days_active           = body.value("days_active", 30);

            auto result = calculate_reputation(input);
            res.set_content(reputation_to_json(result).dump(), "application/json");
            log("POST", "/reputation", 200);
        } catch (const std::exception& e) {
            res.status = 400;
            res.set_content(json{{"error", e.what()}}.dump(), "application/json");
            log("POST", "/reputation", 400);
        }
    });

    // ── Recommendations ───────────────────────────────────────────────────────
    svr.Post("/recommendations", [](const httplib::Request& req, httplib::Response& res) {
        try {
            auto body = json::parse(req.body);
            std::vector<InventoryRecord> records;

            if (body.contains("inventory") && body["inventory"].is_array()) {
                for (const auto& item : body["inventory"])
                    records.push_back(record_from_json(item));
            }

            json recs = generate_recommendations(records);
            res.set_content(json{{"recommendations", recs}}.dump(), "application/json");
            log("POST", "/recommendations", 200);
        } catch (const std::exception& e) {
            res.status = 400;
            res.set_content(json{{"error", e.what()}}.dump(), "application/json");
            log("POST", "/recommendations", 400);
        }
    });

    // ─────────────────────────────────────────────────────────────────────────
    constexpr int PORT = 8080;
    std::cout << "\n";
    std::cout << "  ╔══════════════════════════════════════╗\n";
    std::cout << "  ║  Smart Inventory AI — C++ Engine     ║\n";
    std::cout << "  ║  Listening on http://0.0.0.0:" << PORT << "     ║\n";
    std::cout << "  ╚══════════════════════════════════════╝\n\n";
    std::cout << "  Endpoints:\n";
    std::cout << "    GET  /health\n";
    std::cout << "    POST /analytics\n";
    std::cout << "    POST /match\n";
    std::cout << "    POST /reputation\n";
    std::cout << "    POST /recommendations\n\n";

    if (!svr.listen("0.0.0.0", PORT)) {
        std::cerr << "ERROR: Failed to start server on port " << PORT << "\n";
        return 1;
    }
    return 0;
}
