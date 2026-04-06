#include "analytics.h"
#include <algorithm>
#include <cmath>
#include <map>
#include <sstream>
#include <numeric>

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

static std::string today_prefix() {
    // Returns YYYY-MM-DD for today (simple C approach)
    time_t t = time(nullptr);
    struct tm* tm_info = localtime(&t);
    char buf[16];
    strftime(buf, sizeof(buf), "%Y-%m-%d", tm_info);
    return std::string(buf);
}

static double parse_quantity_value(const std::string& qty) {
    // Extract numeric portion of quantity string (e.g. "10kg" → 10.0)
    try {
        return std::stod(qty);
    } catch (...) {
        size_t i = 0;
        while (i < qty.size() && (std::isdigit(qty[i]) || qty[i] == '.')) ++i;
        if (i > 0) {
            try { return std::stod(qty.substr(0, i)); } catch (...) {}
        }
    }
    return 1.0;
}

// ─────────────────────────────────────────────────────────────────────────────

AnalyticsResult calculate_analytics(const std::vector<InventoryRecord>& records) {
    AnalyticsResult result{};
    result.total_items = static_cast<int>(records.size());

    std::string today = today_prefix();
    double price_sum = 0.0;
    int    priced_count = 0;

    for (const auto& r : records) {
        if (r.type == "bought") {
            ++result.bought_count;
            result.total_bought_value += r.price * parse_quantity_value(r.quantity);
        } else if (r.type == "sold") {
            ++result.sold_count;
            result.total_sold_value += r.price * parse_quantity_value(r.quantity);
        } else if (r.type == "waste") {
            ++result.waste_count;
            result.lost_value += r.price * parse_quantity_value(r.quantity);
        } else if (r.type == "transfer") {
            ++result.transfer_count;
        }

        if (r.price > 0) {
            price_sum += r.price;
            ++priced_count;
        }

        if (!r.timestamp.empty() && r.timestamp.substr(0, 10) == today) {
            ++result.today_count;
        }
    }

    result.waste_percentage = result.total_items > 0
        ? (static_cast<double>(result.waste_count) / result.total_items) * 100.0
        : 0.0;

    result.avg_price = priced_count > 0 ? price_sum / priced_count : 0.0;

    return result;
}

// ─────────────────────────────────────────────────────────────────────────────

std::vector<CategoryStat> calculate_category_stats(const std::vector<InventoryRecord>& records) {
    std::map<std::string, CategoryStat> stats_map;

    for (const auto& r : records) {
        auto& s = stats_map[r.item];
        s.item = r.item;
        ++s.count;
        s.total_value += r.price * parse_quantity_value(r.quantity);
        if (r.type == "waste") {
            s.waste_pct += 1.0; // will normalize below
        }
    }

    std::vector<CategoryStat> result;
    for (auto& [name, stat] : stats_map) {
        if (stat.count > 0) {
            stat.waste_pct = (stat.waste_pct / stat.count) * 100.0;
        }
        result.push_back(stat);
    }

    // Sort by total value descending
    std::sort(result.begin(), result.end(), [](const CategoryStat& a, const CategoryStat& b) {
        return a.total_value > b.total_value;
    });

    return result;
}

// ─────────────────────────────────────────────────────────────────────────────

double calculate_waste_percentage(const std::vector<InventoryRecord>& records) {
    if (records.empty()) return 0.0;
    int waste = 0;
    for (const auto& r : records)
        if (r.type == "waste") ++waste;
    return (static_cast<double>(waste) / records.size()) * 100.0;
}

double calculate_total_value(const std::vector<InventoryRecord>& records) {
    double total = 0.0;
    for (const auto& r : records)
        if (r.type == "bought")
            total += r.price * parse_quantity_value(r.quantity);
    return total;
}
