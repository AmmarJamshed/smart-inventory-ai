package com.smartinventory.service;

import com.smartinventory.model.ParsedResult;

import java.util.*;
import java.util.regex.*;

/**
 * Self-contained NLP parser — zero Python, zero internet, zero downloads.
 * Supports English + Urdu/Roman-Urdu inventory messages.
 *
 * Examples:
 *   "Bought 10kg chicken @ 650/kg"  → type=bought, item=chicken, qty=10kg, price=650
 *   "Aaj 2kg gosht zaya hogaya"     → type=waste,  item=beef,    qty=2kg,  price=0
 *   "5 dozen anday bech diya 180"   → type=sold,   item=eggs,    qty=5 dozen, price=180
 */
public class NlpParser {

    // ── Type keyword vocabulary ──────────────────────────────────────────────
    private static final Map<String, List<String>> TYPE_KEYWORDS = new LinkedHashMap<>();
    static {
        TYPE_KEYWORDS.put("waste", Arrays.asList(
            "waste", "wasted", "zaya", "barbaad", "kharab", "spoiled", "expired",
            "expiry", "phenk", "feka", "rot", "rotten", "nahi chala", "kharab hogaya"
        ));
        TYPE_KEYWORDS.put("bought", Arrays.asList(
            "bought", "buy", "purchase", "kharida", "khareed", "liya", "mila",
            "le liya", "mangwaya", "order", "aaya", "mil gaya", "received", "incoming"
        ));
        TYPE_KEYWORDS.put("sold", Arrays.asList(
            "sold", "sell", "sale", "becha", "bech diya", "farokht", "nikala",
            "de diya", "customer ko", "outgoing", "dispatch"
        ));
        TYPE_KEYWORDS.put("transfer", Arrays.asList(
            "transfer", "bheja", "shift", "move", "bhej diya", "branch",
            "relocated", "doosri jagah", "moved"
        ));
    }

    // ── Unit vocabulary ──────────────────────────────────────────────────────
    private static final List<String> UNITS = Arrays.asList(
        "kilogram", "kilograms", "kilo", "kilos", "kg",
        "gram", "grams", "g",
        "liter", "liters", "litre", "litres", "ltr", "l",
        "milliliter", "ml",
        "piece", "pieces", "pcs", "pc",
        "dozen", "dzn", "dz",
        "box", "boxes",
        "bag", "bags",
        "pack", "packs", "packet", "packets",
        "sack", "sacks",
        "bottle", "bottles",
        "can", "cans",
        "tray", "trays"
    );

    // ── Known food/restaurant items ──────────────────────────────────────────
    private static final Map<String, String> ITEM_MAP = new LinkedHashMap<>();
    static {
        // Meats
        ITEM_MAP.put("chicken",    "chicken");  ITEM_MAP.put("murgh",   "chicken");
        ITEM_MAP.put("beef",       "beef");     ITEM_MAP.put("gosht",   "beef");
        ITEM_MAP.put("mutton",     "mutton");   ITEM_MAP.put("bakra",   "mutton");
        ITEM_MAP.put("lamb",       "lamb");
        ITEM_MAP.put("fish",       "fish");     ITEM_MAP.put("machli",  "fish");
        ITEM_MAP.put("prawn",      "prawn");    ITEM_MAP.put("jhinga",  "prawn");
        // Staples
        ITEM_MAP.put("rice",       "rice");     ITEM_MAP.put("chawal",  "rice");
        ITEM_MAP.put("flour",      "flour");    ITEM_MAP.put("aata",    "flour");
        ITEM_MAP.put("maida",      "maida");
        ITEM_MAP.put("sugar",      "sugar");    ITEM_MAP.put("cheeni",  "sugar");
        ITEM_MAP.put("salt",       "salt");     ITEM_MAP.put("namak",   "salt");
        ITEM_MAP.put("oil",        "oil");      ITEM_MAP.put("tel",     "oil");
        ITEM_MAP.put("ghee",       "ghee");
        ITEM_MAP.put("butter",     "butter");
        // Dairy
        ITEM_MAP.put("milk",       "milk");     ITEM_MAP.put("doodh",   "milk");
        ITEM_MAP.put("eggs",       "eggs");     ITEM_MAP.put("anday",   "eggs");
        ITEM_MAP.put("egg",        "eggs");     ITEM_MAP.put("anda",    "eggs");
        ITEM_MAP.put("yogurt",     "yogurt");   ITEM_MAP.put("dahi",    "yogurt");
        ITEM_MAP.put("cream",      "cream");    ITEM_MAP.put("malai",   "cream");
        // Vegetables
        ITEM_MAP.put("tomato",     "tomato");   ITEM_MAP.put("tamatar", "tomato");
        ITEM_MAP.put("onion",      "onion");    ITEM_MAP.put("pyaz",    "onion");
        ITEM_MAP.put("potato",     "potato");   ITEM_MAP.put("aloo",    "potato");
        ITEM_MAP.put("vegetables", "vegetables"); ITEM_MAP.put("sabzi", "vegetables");
        ITEM_MAP.put("carrot",     "carrot");   ITEM_MAP.put("gajar",   "carrot");
        ITEM_MAP.put("spinach",    "spinach");  ITEM_MAP.put("palak",   "spinach");
        ITEM_MAP.put("peas",       "peas");     ITEM_MAP.put("matar",   "peas");
        ITEM_MAP.put("ginger",     "ginger");   ITEM_MAP.put("adrak",   "ginger");
        ITEM_MAP.put("garlic",     "garlic");   ITEM_MAP.put("lehsan",  "garlic");
        ITEM_MAP.put("chilli",     "chilli");   ITEM_MAP.put("mirch",   "chilli");
        ITEM_MAP.put("coriander",  "coriander"); ITEM_MAP.put("dhaniya","coriander");
        ITEM_MAP.put("mint",       "mint");     ITEM_MAP.put("podina",  "mint");
        // Bakery
        ITEM_MAP.put("bread",      "bread");    ITEM_MAP.put("roti",    "bread");
        ITEM_MAP.put("naan",       "naan");
        // Pulses
        ITEM_MAP.put("lentils",    "lentils");  ITEM_MAP.put("dal",     "lentils");
        ITEM_MAP.put("beans",      "beans");    ITEM_MAP.put("lobia",   "beans");
        // Spices
        ITEM_MAP.put("masala",     "masala");
        ITEM_MAP.put("pepper",     "pepper");
        // Drinks
        ITEM_MAP.put("water",      "water");    ITEM_MAP.put("pani",    "water");
        ITEM_MAP.put("juice",      "juice");
        // Other
        ITEM_MAP.put("charcoal",   "charcoal"); ITEM_MAP.put("koyla",   "charcoal");
        ITEM_MAP.put("wood",       "wood");     ITEM_MAP.put("lakri",   "wood");
        ITEM_MAP.put("packaging",  "packaging");
    }

    // ── Source vocabulary ────────────────────────────────────────────────────
    private static final Map<String, String> SOURCE_MAP = new LinkedHashMap<>();
    static {
        SOURCE_MAP.put("market",   "market");   SOURCE_MAP.put("bazaar",  "market");
        SOURCE_MAP.put("bazar",    "market");   SOURCE_MAP.put("mandi",   "market");
        SOURCE_MAP.put("supplier", "supplier"); SOURCE_MAP.put("vendor",  "supplier");
        SOURCE_MAP.put("thekedar", "supplier"); SOURCE_MAP.put("sarabaan","supplier");
        SOURCE_MAP.put("company",  "supplier");
        SOURCE_MAP.put("farm",     "farm");     SOURCE_MAP.put("khet",    "farm");
        SOURCE_MAP.put("direct",   "farm");     SOURCE_MAP.put("fresh",   "farm");
        SOURCE_MAP.put("store",    "store");    SOURCE_MAP.put("dukaan",  "store");
        SOURCE_MAP.put("godown",   "store");    SOURCE_MAP.put("warehouse","store");
        SOURCE_MAP.put("customer", "customer"); SOURCE_MAP.put("grahak",  "customer");
        SOURCE_MAP.put("branch",   "branch");   SOURCE_MAP.put("outlet",  "branch");
    }

    // ── Compiled regex patterns ──────────────────────────────────────────────
    private static final String UNIT_REGEX;
    private static final Pattern QTY_PATTERN;
    private static final Pattern PRICE_AT_PATTERN    = Pattern.compile("@\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern PRICE_RS_PATTERN    = Pattern.compile("(?:rs\\.?|rupay|rupees|₨|pkr)\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_PER_PATTERN   = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:per|/unit|/kg|/pc|/piece|/dozen|/liter|/pack)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_SUFFIX_PATTERN= Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:rupay|rupees|rs)", Pattern.CASE_INSENSITIVE);

    static {
        // Build unit regex sorted longest-first to avoid short matches winning
        StringBuilder sb = new StringBuilder();
        UNITS.stream()
             .sorted(Comparator.comparingInt(String::length).reversed())
             .forEach(u -> sb.append(u).append("|"));
        UNIT_REGEX = sb.substring(0, sb.length() - 1);
        QTY_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(" + UNIT_REGEX + ")\\b",
            Pattern.CASE_INSENSITIVE
        );
    }

    // ── Public parse method ──────────────────────────────────────────────────

    public static ParsedResult parse(String text) {
        if (text == null || text.isBlank()) {
            return emptyResult();
        }
        String lower = text.toLowerCase().trim();

        String type     = detectType(lower);
        String quantity = extractQuantity(lower);
        double price    = extractPrice(lower);
        String item     = extractItem(lower, quantity);
        String source   = extractSource(lower, type);

        ParsedResult r = new ParsedResult();
        r.setType(type);
        r.setItem(item);
        r.setQuantity(quantity);
        r.setPrice(price);
        r.setSource(source);
        r.setNeedsConfirmation(false); // built-in parser is trusted
        return r;
    }

    // ── Type detection ───────────────────────────────────────────────────────

    private static String detectType(String lower) {
        String padded = " " + lower + " ";
        for (Map.Entry<String, List<String>> entry : TYPE_KEYWORDS.entrySet()) {
            for (String kw : entry.getValue()) {
                String paddedKw = " " + kw + " ";
                if (kw.contains(" ")) {
                    if (padded.contains(kw)) return entry.getKey();
                } else {
                    if (padded.contains(paddedKw)) return entry.getKey();
                }
            }
        }
        return "bought"; // default for restaurant context
    }

    // ── Quantity extraction ──────────────────────────────────────────────────

    private static String extractQuantity(String lower) {
        Matcher m = QTY_PATTERN.matcher(lower);
        if (m.find()) {
            return m.group().trim();
        }
        // Try standalone number
        Matcher num = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\b").matcher(lower);
        if (num.find()) {
            String n = num.group(1);
            // Skip prices that follow @ or rs
            int pos = num.start();
            String before = lower.substring(Math.max(0, pos - 4), pos).trim();
            if (!before.endsWith("@") && !before.contains("rs")) {
                return n;
            }
        }
        return "1";
    }

    // ── Price extraction ─────────────────────────────────────────────────────

    private static double extractPrice(String lower) {
        for (Pattern p : new Pattern[]{
                PRICE_AT_PATTERN, PRICE_RS_PATTERN,
                PRICE_PER_PATTERN, PRICE_SUFFIX_PATTERN}) {
            Matcher m = p.matcher(lower);
            if (m.find()) {
                try { return Double.parseDouble(m.group(1)); } catch (NumberFormatException ignored) {}
            }
        }
        return 0.0;
    }

    // ── Item extraction ──────────────────────────────────────────────────────

    private static String extractItem(String lower, String quantity) {
        // 1. Check vocabulary (sorted longest key first for specificity)
        List<Map.Entry<String, String>> entries = new ArrayList<>(ITEM_MAP.entrySet());
        entries.sort((a, b) -> b.getKey().length() - a.getKey().length());
        for (Map.Entry<String, String> e : entries) {
            String padded = " " + lower + " ";
            if (padded.contains(" " + e.getKey() + " ")) {
                return e.getValue();
            }
        }

        // 2. Word before quantity
        if (quantity != null && !quantity.equals("1")) {
            int idx = lower.indexOf(quantity.toLowerCase());
            if (idx > 0) {
                String[] wordsBefore = lower.substring(0, idx).trim().split("\\s+");
                for (int i = wordsBefore.length - 1; i >= 0; i--) {
                    String w = wordsBefore[i];
                    if (w.length() > 2 && w.matches("[a-zA-Z]+") &&
                        !isStopWord(w)) {
                        return w;
                    }
                }
            }
        }

        // 3. Any significant content word
        String[] words = lower.split("\\s+");
        for (String w : words) {
            if (w.length() > 3 && w.matches("[a-zA-Z]+") && !isStopWord(w)) {
                return w;
            }
        }
        return "item";
    }

    // ── Source extraction ────────────────────────────────────────────────────

    private static String extractSource(String lower, String type) {
        String padded = " " + lower + " ";
        for (Map.Entry<String, String> e : SOURCE_MAP.entrySet()) {
            if (padded.contains(" " + e.getKey() + " ")) {
                return e.getValue();
            }
        }
        return switch (type) {
            case "sold"     -> "customer";
            case "transfer" -> "branch";
            case "waste"    -> "internal";
            default         -> "market";
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "the", "and", "aur", "kal", "aaj", "tha", "hai", "ka", "ko", "ki",
        "se", "mein", "per", "karo", "kiya", "gaya", "hogaya", "from", "with",
        "this", "that", "today", "yesterday", "for", "some", "has", "have",
        "bought", "sold", "waste", "buy", "sell", "kharida", "becha", "zaya"
    ));

    private static boolean isStopWord(String w) {
        return STOP_WORDS.contains(w.toLowerCase()) || w.matches("\\d+");
    }

    private static ParsedResult emptyResult() {
        ParsedResult r = new ParsedResult();
        r.setType("bought"); r.setItem("item");
        r.setQuantity("1");  r.setPrice(0.0);
        r.setSource("market"); r.setNeedsConfirmation(true);
        return r;
    }
}
