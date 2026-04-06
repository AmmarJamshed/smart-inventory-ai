#pragma once
#include <string>

struct ReputationInput {
    int    total_transactions;
    int    successful_transactions;
    int    dispute_total;
    int    dispute_wins;
    double avg_response_hours;   // average response time in hours
    double delivery_rate;        // 0-1 (on-time deliveries / total)
    int    days_active;
};

struct ReputationResult {
    double score;               // 0-100
    double success_rate;
    double dispute_win_rate;
    double response_speed_score;
    double delivery_reliability;
    double activity_level;
    std::string tier;           // Bronze / Silver / Gold / Platinum
    std::string badge;
};

// Reputation = 0.40 * success_rate
//            + 0.20 * dispute_win_rate
//            + 0.15 * response_speed
//            + 0.15 * delivery_reliability
//            + 0.10 * activity_level
ReputationResult calculate_reputation(const ReputationInput& input);

std::string get_reputation_tier(double score);
