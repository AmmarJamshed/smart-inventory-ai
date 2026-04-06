package com.smartinventory.service;

import com.smartinventory.model.InventoryItem;
import com.smartinventory.model.Recommendation;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Pure-Java analytics + matching + reputation engine.
 * Replaces the C++ REST service — zero external processes.
 *
 * Matching formula:  0.35*distance + 0.25*price_adv + 0.20*reputation + 0.10*urgency + 0.10*freshness
 * Reputation formula:0.40*success_rate + 0.20*dispute_win_rate + 0.15*response_speed
 *                   + 0.15*delivery_reliability + 0.10*activity_level
 */
public class LocalEngineService {

    // ── Mock trade participants (simulates B2B market) ────────────────────────
    private static final List<MockTrader> TRADERS = Arrays.asList(
        new MockTrader("Trader #042",    "sell", List.of("chicken","beef","mutton"),   620.0, 2, 0.80, 0.95, 88.5),
        new MockTrader("Merchant #118",  "buy",  List.of("chicken","vegetables","oil"),650.0, 3, 0.70, 0.90, 72.0),
        new MockTrader("Supplier #007",  "sell", List.of("beef","flour","rice","oil"), 850.0, 1, 0.50, 0.85, 95.2),
        new MockTrader("Buyer #033",     "buy",  List.of("beef","mutton","fish"),      900.0, 4, 0.90, 0.80, 61.5),
        new MockTrader("Wholesaler #09", "sell", List.of("mutton","rice","flour","sugar"),1100.0,2,0.40,0.70,83.0),
        new MockTrader("Trader #042",    "sell", List.of("rice","flour","sugar"),      110.0, 2, 0.30, 0.60, 88.5),
        new MockTrader("Supplier #007",  "sell", List.of("flour","salt","masala"),     85.0,  1, 0.50, 0.75, 95.2),
        new MockTrader("Merchant #118",  "buy",  List.of("vegetables","tomato","onion"),80.0, 3, 0.80, 0.95, 72.0),
        new MockTrader("Wholesaler #09", "sell", List.of("oil","ghee","butter"),       220.0, 2, 0.60, 0.88, 83.0),
        new MockTrader("Buyer #033",     "buy",  List.of("eggs","milk","cream"),       180.0, 4, 0.70, 0.92, 61.5),
        new MockTrader("FreshFarm #01",  "sell", List.of("chicken","eggs","milk","vegetables"),580.0,1,0.85,0.98,91.0),
        new MockTrader("Restaurant #55", "buy",  List.of("chicken","beef","mutton","fish"),700.0,3,0.75,0.85,78.5)
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Analytics
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> calculateAnalytics(List<InventoryItem> items) {
        int total     = items.size();
        long bought   = items.stream().filter(i -> "bought".equalsIgnoreCase(i.getType())).count();
        long sold     = items.stream().filter(i -> "sold".equalsIgnoreCase(i.getType())).count();
        long waste    = items.stream().filter(i -> "waste".equalsIgnoreCase(i.getType())).count();
        long transfer = items.stream().filter(i -> "transfer".equalsIgnoreCase(i.getType())).count();

        double boughtValue = items.stream()
            .filter(i -> "bought".equalsIgnoreCase(i.getType()))
            .mapToDouble(i -> i.getPrice() * parseQuantityNum(i.getQuantity()))
            .sum();

        double soldValue = items.stream()
            .filter(i -> "sold".equalsIgnoreCase(i.getType()))
            .mapToDouble(i -> i.getPrice() * parseQuantityNum(i.getQuantity()))
            .sum();

        double wasteValue = items.stream()
            .filter(i -> "waste".equalsIgnoreCase(i.getType()))
            .mapToDouble(i -> i.getPrice() * parseQuantityNum(i.getQuantity()))
            .sum();

        double avgPrice = items.stream()
            .filter(i -> i.getPrice() > 0)
            .mapToDouble(InventoryItem::getPrice)
            .average().orElse(0);

        String today = LocalDate.now().toString();
        long todayCount = items.stream()
            .filter(i -> i.getTimestamp() != null && i.getTimestamp().startsWith(today))
            .count();

        double wastePct = total > 0 ? (waste * 100.0 / total) : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_items",        total);
        result.put("bought_count",       bought);
        result.put("sold_count",         sold);
        result.put("waste_count",        waste);
        result.put("transfer_count",     transfer);
        result.put("waste_percentage",   Math.round(wastePct * 10.0) / 10.0);
        result.put("total_bought_value", Math.round(boughtValue * 100.0) / 100.0);
        result.put("total_sold_value",   Math.round(soldValue  * 100.0) / 100.0);
        result.put("lost_value",         Math.round(wasteValue * 100.0) / 100.0);
        result.put("avg_price",          Math.round(avgPrice   * 100.0) / 100.0);
        result.put("today_count",        todayCount);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recommendations
    // ─────────────────────────────────────────────────────────────────────────

    public List<Recommendation> getRecommendations(List<InventoryItem> items) {
        List<Recommendation> recs = new ArrayList<>();
        if (items.isEmpty()) return recs;

        // Find high-waste items → recommend selling quickly
        Map<String, Long> wasteByItem  = items.stream()
            .filter(i -> "waste".equalsIgnoreCase(i.getType()))
            .collect(Collectors.groupingBy(i -> normalize(i.getItem()), Collectors.counting()));
        Map<String, Long> totalByItem  = items.stream()
            .collect(Collectors.groupingBy(i -> normalize(i.getItem()), Collectors.counting()));

        for (Map.Entry<String, Long> e : totalByItem.entrySet()) {
            String item  = e.getKey();
            long   total = e.getValue();
            long   waste = wasteByItem.getOrDefault(item, 0L);
            double pct   = total > 0 ? (waste * 100.0 / total) : 0;

            if (pct > 15 && waste > 0) {
                // Find best buyer for this item
                MockTrader buyer = findBestTrader("buy", item, 0, TRADERS);
                if (buyer != null) {
                    Recommendation r = new Recommendation();
                    r.setType("Sell Urgently ⚡");
                    r.setPseudonym(buyer.name);
                    r.setScore(0.85);
                    r.setReasoning(String.format(
                        "%s has %.0f%% waste. %s is buying at ₨%.0f — "
                        + "Reputation ★%.1f · Distance zone %d",
                        capitalize(item), pct, buyer.name, buyer.price,
                        buyer.reputation, buyer.zone));
                    recs.add(r);
                    if (recs.size() >= 2) break;
                }
            }
        }

        // Find top purchased item → recommend best supplier
        totalByItem.entrySet().stream()
            .filter(e -> items.stream().anyMatch(
                i -> normalize(i.getItem()).equals(e.getKey())
                  && "bought".equalsIgnoreCase(i.getType())))
            .max(Map.Entry.comparingByValue())
            .ifPresent(e -> {
                MockTrader supplier = findBestTrader("sell", e.getKey(), 0, TRADERS);
                if (supplier != null && recs.stream().noneMatch(r -> r.getPseudonym().equals(supplier.name))) {
                    Recommendation r = new Recommendation();
                    r.setType("Best Supplier 🏆");
                    r.setPseudonym(supplier.name);
                    r.setScore(supplier.reputation / 100.0);
                    r.setReasoning(String.format(
                        "Top supplier for %s at ₨%.0f/unit. "
                        + "Reputation ★%.1f · Zone %d · %.0f%% freshness",
                        capitalize(e.getKey()), supplier.price,
                        supplier.reputation, supplier.zone, supplier.freshness * 100));
                    recs.add(r);
                }
            });

        // General recommendation if nothing specific
        if (recs.isEmpty()) {
            Recommendation r = new Recommendation();
            r.setType("Best Supplier Nearby 📍");
            r.setPseudonym("Supplier #007");
            r.setScore(0.95);
            r.setReasoning("Supplier #007 has consistent stock & fastest delivery in zone 1. "
                         + "Reputation ★95.2 · Trusted by 130+ transactions.");
            recs.add(r);
        }

        return recs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trade Matching
    // ─────────────────────────────────────────────────────────────────────────

    public List<Recommendation> findMatches(String tradeType, String itemName,
                                             String quantity, double targetPrice) {
        String counterType = "sell".equalsIgnoreCase(tradeType) ? "buy" : "sell";
        String normItem    = normalize(itemName);
        int    reqZone     = 3; // default requester zone

        List<ScoredTrader> scored = new ArrayList<>();

        for (MockTrader t : TRADERS) {
            if (!counterType.equalsIgnoreCase(t.type)) continue;
            boolean itemMatch = t.items.stream()
                .anyMatch(i -> normalize(i).contains(normItem)
                            || normItem.contains(normalize(i)));
            if (!itemMatch && !normItem.isEmpty()) continue;

            double distScore  = normalizeDistance(reqZone, t.zone);
            double priceAdv   = computePriceAdvantage(t.price, targetPrice, counterType);
            double repNorm    = t.reputation / 100.0;
            double matchScore = 0.35 * distScore
                              + 0.25 * priceAdv
                              + 0.20 * repNorm
                              + 0.10 * t.urgency
                              + 0.10 * t.freshness;

            scored.add(new ScoredTrader(t, matchScore, distScore, priceAdv));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));

        return scored.stream()
            .limit(5)
            .map(s -> {
                Recommendation r = new Recommendation();
                r.setType(capitalize(s.trader.type) + " offer");
                r.setPseudonym(s.trader.name);
                r.setScore(Math.round(s.score * 1000.0) / 1000.0);
                r.setReasoning(buildMatchReasoning(s));
                return r;
            })
            .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reputation
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> calculateReputation(int totalTx, int successTx,
                                                    int disputeTotal, int disputeWins,
                                                    double avgResponseHours,
                                                    double deliveryRate, int daysActive) {
        double successRate    = totalTx > 0 ? (double) successTx / totalTx : 0.5;
        double disputeWinRate = disputeTotal > 0 ? (double) disputeWins / disputeTotal : 0.8;
        double responseSpeed  = clamp(1.0 - avgResponseHours / 48.0);
        double delivery       = clamp(deliveryRate);
        double activity       = clamp(0.5 * clamp(daysActive / 365.0)
                              + 0.5 * clamp(Math.log1p(totalTx) / Math.log1p(200)));

        double raw   = 0.40 * successRate + 0.20 * disputeWinRate
                     + 0.15 * responseSpeed + 0.15 * delivery + 0.10 * activity;
        double score = clamp(raw * 100.0, 0, 100);

        String tier  = score >= 90 ? "Platinum" : score >= 75 ? "Gold"
                     : score >= 55 ? "Silver" : "Bronze";
        String badge = score >= 90 ? "⭐ Elite Trader" : score >= 75 ? "✓ Trusted Trader"
                     : score >= 55 ? "~ Regular Trader" : "! New Trader";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score",    Math.round(score * 10.0) / 10.0);
        result.put("tier",     tier);
        result.put("badge",    badge);
        result.put("success_rate",    Math.round(successRate    * 1000) / 1000.0);
        result.put("dispute_win_rate",Math.round(disputeWinRate * 1000) / 1000.0);
        result.put("response_speed",  Math.round(responseSpeed  * 1000) / 1000.0);
        result.put("delivery",        Math.round(delivery       * 1000) / 1000.0);
        result.put("activity",        Math.round(activity       * 1000) / 1000.0);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private MockTrader findBestTrader(String type, String item, double price,
                                      List<MockTrader> traders) {
        String normItem = normalize(item);
        return traders.stream()
            .filter(t -> type.equalsIgnoreCase(t.type))
            .filter(t -> t.items.stream().anyMatch(i -> normalize(i).contains(normItem)
                                                     || normItem.contains(normalize(i))))
            .max(Comparator.comparingDouble(t -> t.reputation))
            .orElse(null);
    }

    private double normalizeDistance(int req, int offer) {
        return Math.max(0.1, 1.0 - Math.abs(req - offer) * 0.1);
    }

    private double computePriceAdvantage(double offerPrice, double reqPrice, String type) {
        if (reqPrice <= 0 || offerPrice <= 0) return 0.5;
        double diff = (reqPrice - offerPrice) / reqPrice;
        return "sell".equalsIgnoreCase(type)
               ? clamp(0.5 + diff) : clamp(0.5 - diff);
    }

    private String buildMatchReasoning(ScoredTrader s) {
        String dist = s.distScore >= 0.8 ? "Very close" : s.distScore >= 0.5 ? "Nearby" : "Far";
        String price = s.priceAdv >= 0.7 ? "Excellent price" : s.priceAdv >= 0.5 ? "Good price" : "Negotiable price";
        String rep = s.trader.reputation >= 85 ? "Highly trusted"
                   : s.trader.reputation >= 65 ? "Trusted" : "New trader";
        return String.format("%s (zone %d). %s ₨%.0f/unit. %s ★%.1f",
            dist, s.trader.zone, price, s.trader.price,
            rep, s.trader.reputation);
    }

    private static double parseQuantityNum(String qty) {
        if (qty == null) return 1.0;
        try {
            Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(qty);
            return m.find() ? Double.parseDouble(m.group(1)) : 1.0;
        } catch (Exception e) { return 1.0; }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().trim();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    // ── Inner records ──────────────────────────────────────────────────────────

    private static class MockTrader {
        final String name; final String type; final List<String> items;
        final double price; final int zone; final double urgency;
        final double freshness; final double reputation;
        MockTrader(String n, String t, List<String> i, double p, int z,
                   double u, double f, double r) {
            name=n; type=t; items=i; price=p; zone=z; urgency=u; freshness=f; reputation=r;
        }
    }

    private static class ScoredTrader {
        final MockTrader trader; final double score, distScore, priceAdv;
        ScoredTrader(MockTrader t, double s, double d, double p) {
            trader=t; score=s; distScore=d; priceAdv=p;
        }
    }
}
