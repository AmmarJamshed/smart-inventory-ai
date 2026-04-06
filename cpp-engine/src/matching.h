#pragma once
#include <string>
#include <vector>

struct TradeOffer {
    std::string offer_id;
    std::string user_did;
    std::string pseudonym;
    std::string type;           // buy | sell
    std::string item;
    std::string quantity;
    double      target_price;
    int         location_zone;  // 1-10 (lower = closer)
    double      urgency;        // 0-1
    double      freshness;      // 0-1
    double      reputation;     // 0-100
};

struct MatchRequest {
    std::string type;           // what the requester wants to do
    std::string item;
    std::string quantity;
    double      target_price;
    int         location_zone;
    double      urgency;
};

struct MatchResult {
    std::string pseudonym;
    std::string type;
    double      score;          // 0-1
    double      price_advantage;
    double      distance_score;
    double      reputation_score;
    std::string reasoning;
    std::string offer_id;
};

// Weights for match score calculation
// Score = 0.35*distance + 0.25*price_advantage + 0.20*reputation + 0.10*urgency + 0.10*freshness
double calculate_match_score(const TradeOffer& offer, const MatchRequest& req);

std::vector<MatchResult> find_best_matches(
    const std::vector<TradeOffer>& offers,
    const MatchRequest& req,
    int top_n = 5
);

std::string build_reasoning(const TradeOffer& offer, const MatchResult& result);
