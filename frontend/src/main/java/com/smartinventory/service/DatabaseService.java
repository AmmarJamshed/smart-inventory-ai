package com.smartinventory.service;

import com.smartinventory.model.Dispute;
import com.smartinventory.model.InventoryItem;
import com.smartinventory.model.Transaction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Direct SQLite database access layer for the JavaFX application.
 * Falls back gracefully when Python service is offline.
 */
public class DatabaseService {

    private static final String DB_DIR  = "D:\\SmartInventoryAI\\data";
    private static final String DB_FILE = DB_DIR + "\\inventory.db";
    private static final String JDBC    = "jdbc:sqlite:" + DB_FILE;

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DatabaseService() {
        ensureDatabaseExists();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Connection
    // ─────────────────────────────────────────────────────────────────────────

    private Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(JDBC);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA foreign_keys=ON");
        }
        return conn;
    }

    private void ensureDatabaseExists() {
        try {
            Files.createDirectories(Paths.get(DB_DIR));
            // Trigger DB creation & schema via the SQL file if it exists, else inline
            try (Connection conn = connect()) {
                createTablesIfNeeded(conn);
            }
        } catch (Exception e) {
            System.err.println("[DB] Init error: " + e.getMessage());
        }
    }

    private void createTablesIfNeeded(Connection conn) throws SQLException {
        String schemaPath = "D:\\SmartInventoryAI\\database\\schema.sql";
        Path p = Paths.get(schemaPath);

        if (Files.exists(p)) {
            try {
                String sql = Files.readString(p);
                try (Statement st = conn.createStatement()) {
                    conn.setAutoCommit(false);
                    // Execute statements one by one
                    for (String stmt : sql.split(";")) {
                        String trimmed = stmt.trim();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                            try { st.execute(trimmed); } catch (SQLException ignore) {}
                        }
                    }
                    conn.commit();
                    conn.setAutoCommit(true);
                }
                return;
            } catch (Exception e) {
                System.err.println("[DB] Schema file read error: " + e.getMessage());
            }
        }

        // Inline minimal schema fallback
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS inventory (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT NOT NULL DEFAULT 'unknown',
                    item TEXT NOT NULL,
                    quantity TEXT NOT NULL,
                    price REAL DEFAULT 0.0,
                    source TEXT DEFAULT 'unknown',
                    timestamp TEXT DEFAULT (datetime('now','localtime'))
                )""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    transaction_id TEXT PRIMARY KEY,
                    seller_did TEXT NOT NULL,
                    buyer_did TEXT NOT NULL,
                    item TEXT NOT NULL,
                    quantity TEXT NOT NULL,
                    price REAL NOT NULL,
                    timestamp TEXT DEFAULT (datetime('now','localtime')),
                    status TEXT DEFAULT 'pending'
                )""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS transaction_hashes (
                    transaction_id TEXT PRIMARY KEY,
                    hash TEXT NOT NULL,
                    created_at TEXT DEFAULT (datetime('now','localtime'))
                )""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS disputes (
                    dispute_id TEXT PRIMARY KEY,
                    transaction_id TEXT NOT NULL,
                    raised_by_did TEXT DEFAULT 'did:local:merchant001',
                    evidence TEXT NOT NULL,
                    status TEXT DEFAULT 'open',
                    resolution TEXT,
                    created_at TEXT DEFAULT (datetime('now','localtime'))
                )""");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inventory
    // ─────────────────────────────────────────────────────────────────────────

    public InventoryItem saveInventoryItem(InventoryItem item) throws Exception {
        String ts = item.getTimestamp() != null && !item.getTimestamp().isBlank()
                ? item.getTimestamp()
                : LocalDateTime.now().format(TIMESTAMP_FMT);
        item.setTimestamp(ts);

        String sql = "INSERT INTO inventory (type, item, quantity, price, source, timestamp) VALUES (?,?,?,?,?,?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, safeStr(item.getType()));
            ps.setString(2, safeStr(item.getItem()));
            ps.setString(3, safeStr(item.getQuantity()));
            ps.setDouble(4, item.getPrice());
            ps.setString(5, safeStr(item.getSource()));
            ps.setString(6, ts);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getInt(1));
            }
        }
        return item;
    }

    public List<InventoryItem> getAllInventoryItems() throws Exception {
        List<InventoryItem> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory ORDER BY timestamp DESC LIMIT 500";

        try (Connection conn = connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                InventoryItem item = new InventoryItem();
                item.setId(       rs.getInt("id"));
                item.setType(     rs.getString("type"));
                item.setItem(     rs.getString("item"));
                item.setQuantity( rs.getString("quantity"));
                item.setPrice(    rs.getDouble("price"));
                item.setSource(   rs.getString("source"));
                item.setTimestamp(rs.getString("timestamp"));
                list.add(item);
            }
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Transactions
    // ─────────────────────────────────────────────────────────────────────────

    public Transaction saveTransaction(Transaction tx) throws Exception {
        if (tx.getTransactionId() == null || tx.getTransactionId().isBlank()) {
            tx.setTransactionId("TX-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        }
        String ts = tx.getTimestamp() != null ? tx.getTimestamp()
                : LocalDateTime.now().format(TIMESTAMP_FMT);
        tx.setTimestamp(ts);

        String sql = """
            INSERT OR REPLACE INTO transactions
            (transaction_id, seller_did, buyer_did, item, quantity, price, timestamp, status)
            VALUES (?,?,?,?,?,?,?,?)""";

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tx.getTransactionId());
            ps.setString(2, safeStr(tx.getSellerDid()));
            ps.setString(3, safeStr(tx.getBuyerDid()));
            ps.setString(4, safeStr(tx.getItem()));
            ps.setString(5, safeStr(tx.getQuantity()));
            ps.setDouble(6, tx.getPrice());
            ps.setString(7, ts);
            ps.setString(8, tx.getStatus() != null ? tx.getStatus() : "pending");
            ps.executeUpdate();
        }

        // Store SHA-256 hash
        String hashInput = tx.getTransactionId() + ":" + tx.getSellerDid() + ":"
                + tx.getBuyerDid() + ":" + tx.getItem() + ":" + tx.getQuantity()
                + ":" + tx.getPrice() + ":" + ts;
        String hash = sha256(hashInput);
        tx.setHash(hash);

        String hashSql = "INSERT OR REPLACE INTO transaction_hashes (transaction_id, hash) VALUES (?,?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(hashSql)) {
            ps.setString(1, tx.getTransactionId());
            ps.setString(2, hash);
            ps.executeUpdate();
        }
        return tx;
    }

    public Transaction getTransaction(String txId) throws Exception {
        String sql = """
            SELECT t.*, h.hash FROM transactions t
            LEFT JOIN transaction_hashes h ON t.transaction_id = h.transaction_id
            WHERE t.transaction_id = ?""";

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Transaction tx = new Transaction();
                    tx.setTransactionId(rs.getString("transaction_id"));
                    tx.setSellerDid(    rs.getString("seller_did"));
                    tx.setBuyerDid(     rs.getString("buyer_did"));
                    tx.setItem(         rs.getString("item"));
                    tx.setQuantity(     rs.getString("quantity"));
                    tx.setPrice(        rs.getDouble("price"));
                    tx.setTimestamp(    rs.getString("timestamp"));
                    tx.setStatus(       rs.getString("status"));
                    tx.setHash(         rs.getString("hash"));
                    return tx;
                }
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Disputes
    // ─────────────────────────────────────────────────────────────────────────

    public Dispute createDispute(String txId, String evidence) throws Exception {
        String disputeId = "DIS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String ts = LocalDateTime.now().format(TIMESTAMP_FMT);

        String sql = """
            INSERT INTO disputes (dispute_id, transaction_id, raised_by_did, evidence, status, created_at)
            VALUES (?,?,?,?,?,?)""";

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, disputeId);
            ps.setString(2, txId);
            ps.setString(3, "did:local:merchant001");
            ps.setString(4, evidence);
            ps.setString(5, "open");
            ps.setString(6, ts);
            ps.executeUpdate();
        }

        return new Dispute(disputeId, txId, evidence, "open", "Pending", ts);
    }

    public List<Dispute> getAllDisputes() throws Exception {
        List<Dispute> list = new ArrayList<>();
        String sql = "SELECT * FROM disputes ORDER BY created_at DESC";

        try (Connection conn = connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Dispute(
                    rs.getString("dispute_id"),
                    rs.getString("transaction_id"),
                    rs.getString("evidence"),
                    rs.getString("status"),
                    rs.getString("resolution") != null ? rs.getString("resolution") : "Pending",
                    rs.getString("created_at")
                ));
            }
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "hash-error";
        }
    }

    private static String safeStr(String s) {
        return s != null ? s : "";
    }
}
