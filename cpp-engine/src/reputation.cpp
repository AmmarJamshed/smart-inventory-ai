#include "reputation.h"
#include <algorithm>
#include <cmath>

// ─────────────────────────────────────────────────────────────────────────────
// Reputation Formula (0-100):
//   score = 100 * (
//       0.40 * success_rate
//     + 0.20 * dispute_win_rate
//     + 0.15 * response_speed_score
//     + 0.15 * delivery_reliability
//     + 0.10 * activity_level
//   )
// ─────────────────────────────────────────────────────────────────────────────

static double clamp(double v, double lo = 0.0, double hi = 1.0) {
    return std::max(lo, std::min(hi, v));
}

static double normalize_response_speed(double avg_hours) {
    // Best: < 1h → 1.0, Worst: > 48h → 0.0
    if (avg_hours <= 0) return 0.5; // unknown
    return clamp(1.0 - (avg_hours / 48.0));
}

static double normalize_activity(int days_active, int tx_count) {
    // Combines days active (max benefit at 365) and transaction volume
    double days_score = clamp(days_active / 365.0);
    double tx_score   = clamp(std::log1p(tx_count) / std::log1p(200)); // 200 tx = max
    return 0.5 * days_score + 0.5 * tx_score;
}

ReputationResult calculate_reputation(const ReputationInput& input) {
    ReputationResult result;

    // Component calculations
    result.success_rate = (input.total_transactions > 0)
        ? clamp(static_cast<double>(input.successful_transactions) / input.total_transactions)
        : 0.5;

    result.dispute_win_rate = (input.dispute_total > 0)
        ? clamp(static_cast<double>(input.dispute_wins) / input.dispute_total)
        : 0.8; // no disputes = assume trustworthy

    result.response_speed_score  = normalize_response_speed(input.avg_response_hours);
    result.delivery_reliability  = clamp(input.delivery_rate);
    result.activity_level        = normalize_activity(input.days_active, input.total_transactions);

    double raw_score =
        0.40 * result.success_rate
      + 0.20 * result.dispute_win_rate
      + 0.15 * result.response_speed_score
      + 0.15 * result.delivery_reliability
      + 0.10 * result.activity_level;

    result.score = clamp(raw_score * 100.0, 0.0, 100.0);
    result.tier  = get_reputation_tier(result.score);

    // Assign badge
    if (result.score >= 90)
        result.badge = "⭐ Elite Trader";
    else if (result.score >= 75)
        result.badge = "✓ Trusted Trader";
    else if (result.score >= 55)
        result.badge = "~ Regular Trader";
    else
        result.badge = "! New Trader";

    return result;
}

std::string get_reputation_tier(double score) {
    if (score >= 90) return "Platinum";
    if (score >= 75) return "Gold";
    if (score >= 55) return "Silver";
    return "Bronze";
}
