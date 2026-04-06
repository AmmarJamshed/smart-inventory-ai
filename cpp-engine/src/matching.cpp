#include "matching.h"
#include <algorithm>
#include <cmath>
#include <sstream>
#include <iomanip>

// ─────────────────────────────────────────────────────────────────────────────
// Match Score Formula:
//   score = 0.35 * distance_score
//         + 0.25 * price_advantage
//         + 0.20 * reputation_norm
//         + 0.10 * urgency
//         + 0.10 * freshness
// ─────────────────────────────────────────────────────────────────────────────

static double normalize_distance(int req_zone, int offer_zone) {
    int diff = std::abs(req_zone - offer_zone);
    // zone diff 0 → 1.0, zone diff 9 → 0.1
    return std::max(0.1, 1.0 - (diff * 0.1));
}

static double compute_price_advantage(double offer_price, double req_price, const std::string& counterpart_type) {
    if (req_price <= 0 || offer_price <= 0) return 0.5; // neutral

    double diff_pct = (req_price - offer_price) / req_price;

    if (counterpart_type == "sell") {
        // Buyer wants to buy cheap: higher advantage if offer is cheaper
        return std::min(1.0, std::max(0.0, 0.5 + diff_pct));
    } else {
        // Seller wants to sell high: higher advantage if offer price is higher
        return std::min(1.0, std::max(0.0, 0.5 - diff_pct));
    }
}

double calculate_match_score(const TradeOffer& offer, const MatchRequest& req) {
    double distance    = normalize_distance(req.location_zone, offer.location_zone);
    double price_adv   = compute_price_advantage(offer.target_price, req.target_price, offer.type);
    double rep_norm    = offer.reputation / 100.0;
    double urgency     = offer.urgency;
    double freshness   = offer.freshness;

    return (0.35 * distance)
         + (0.25 * price_adv)
         + (0.20 * rep_norm)
         + (0.10 * urgency)
         + (0.10 * freshness);
}

// ─────────────────────────────────────────────────────────────────────────────

std::string build_reasoning(const TradeOffer& offer, const MatchResult& result) {
    std::ostringstream oss;

    // Distance
    if (result.distance_score >= 0.8)
        oss << "Very close (zone " << offer.location_zone << "). ";
    else if (result.distance_score >= 0.5)
        oss << "Moderate distance. ";
    else
        oss << "Far location. ";

    // Price
    if (result.price_advantage >= 0.7)
        oss << "Excellent price match";
    else if (result.price_advantage >= 0.5)
        oss << "Good price";
    else
        oss << "Price negotiable";

    if (offer.target_price > 0) {
        oss << " (₨" << std::fixed << std::setprecision(0) << offer.target_price << "/unit). ";
    } else {
        oss << ". ";
    }

    // Reputation
    if (offer.reputation >= 85)
        oss << "Highly trusted (★" << std::fixed << std::setprecision(1) << offer.reputation << ").";
    else if (offer.reputation >= 65)
        oss << "Trusted trader (★" << std::fixed << std::setprecision(1) << offer.reputation << ").";
    else
        oss << "New trader (★" << std::fixed << std::setprecision(1) << offer.reputation << ").";

    return oss.str();
}

// ─────────────────────────────────────────────────────────────────────────────

std::vector<MatchResult> find_best_matches(
    const std::vector<TradeOffer>& offers,
    const MatchRequest& req,
    int top_n)
{
    std::vector<MatchResult> results;

    for (const auto& offer : offers) {
        // Skip same type (we need counterpart)
        if (offer.type == req.type) continue;

        double score       = calculate_match_score(offer, req);
        double distance    = normalize_distance(req.location_zone, offer.location_zone);
        double price_adv   = compute_price_advantage(offer.target_price, req.target_price, offer.type);

        MatchResult mr;
        mr.pseudonym       = offer.pseudonym;
        mr.type            = offer.type;
        mr.score           = score;
        mr.price_advantage = price_adv;
        mr.distance_score  = distance;
        mr.reputation_score= offer.reputation / 100.0;
        mr.offer_id        = offer.offer_id;
        mr.reasoning       = build_reasoning(offer, mr);

        results.push_back(mr);
    }

    // Sort by score descending
    std::sort(results.begin(), results.end(), [](const MatchResult& a, const MatchResult& b) {
        return a.score > b.score;
    });

    if (static_cast<int>(results.size()) > top_n)
        results.resize(top_n);

    return results;
}
