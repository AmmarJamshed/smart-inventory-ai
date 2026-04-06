#pragma once
#include <string>
#include <vector>

struct InventoryRecord {
    int         id;
    std::string type;       // bought | sold | waste | transfer
    std::string item;
    std::string quantity;
    double      price;
    std::string source;
    std::string timestamp;
};

struct AnalyticsResult {
    int    total_items;
    int    bought_count;
    int    sold_count;
    int    waste_count;
    int    transfer_count;
    double waste_percentage;
    double total_bought_value;
    double total_sold_value;
    double avg_price;
    int    today_count;
    double lost_value;       // value of wasted items
};

struct CategoryStat {
    std::string item;
    int         count;
    double      total_value;
    double      waste_pct;
};

AnalyticsResult   calculate_analytics(const std::vector<InventoryRecord>& records);
std::vector<CategoryStat> calculate_category_stats(const std::vector<InventoryRecord>& records);
double            calculate_waste_percentage(const std::vector<InventoryRecord>& records);
double            calculate_total_value(const std::vector<InventoryRecord>& records);
