package com.smartinventory.controller;

import com.smartinventory.model.*;
import com.smartinventory.service.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MainController implements Initializable {

    // ── Navigation ───────────────────────────────────────────────────────────
    @FXML private Button navDashboard;
    @FXML private Button navAnalytics;
    @FXML private Button navTrade;
    @FXML private Button navDisputes;
    @FXML private Button darkModeToggle;
    @FXML private Label  userPseudonymLabel;
    @FXML private Label  reputationLabel;

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private Label  pageTitleLabel;
    @FXML private Circle pythonStatusDot;
    @FXML private Circle cppStatusDot;
    @FXML private Label  pythonStatusLabel;
    @FXML private Label  cppStatusLabel;

    // ── Views ─────────────────────────────────────────────────────────────────
    @FXML private ScrollPane dashboardView;
    @FXML private ScrollPane analyticsView;
    @FXML private ScrollPane tradeView;
    @FXML private ScrollPane disputesView;

    // ── Dashboard: Input card ─────────────────────────────────────────────────
    @FXML private TextArea inputArea;
    @FXML private Label    statusLabel;

    // ── Dashboard: Stat cards ─────────────────────────────────────────────────
    @FXML private Label totalItemsLabel;
    @FXML private Label wasteCountLabel;
    @FXML private Label totalValueLabel;
    @FXML private Label todayCountLabel;

    // ── Dashboard: Inventory table ────────────────────────────────────────────
    @FXML private TableView<InventoryItem>          inventoryTable;
    @FXML private TableColumn<InventoryItem, String> typeCol;
    @FXML private TableColumn<InventoryItem, String> itemCol;
    @FXML private TableColumn<InventoryItem, String> quantityCol;
    @FXML private TableColumn<InventoryItem, Double> priceCol;
    @FXML private TableColumn<InventoryItem, String> sourceCol;
    @FXML private TableColumn<InventoryItem, String> timestampCol;

    // ── Dashboard: Recommendations ────────────────────────────────────────────
    @FXML private VBox recommendationPanel;

    // ── Analytics ─────────────────────────────────────────────────────────────
    @FXML private PieChart              categoryPieChart;
    @FXML private BarChart<String, Number> dailyBarChart;
    @FXML private LineChart<String, Number> trendLineChart;
    @FXML private Label wastePctLabel;
    @FXML private Label lostValueLabel;
    @FXML private Label avgWasteLabel;

    // ── Trade ─────────────────────────────────────────────────────────────────
    @FXML private ComboBox<String> tradeTypeCombo;
    @FXML private TextField        tradeItemField;
    @FXML private TextField        tradeQtyField;
    @FXML private TextField        tradePriceField;
    @FXML private VBox             matchesPanel;

    // ── Disputes ──────────────────────────────────────────────────────────────
    @FXML private TextField                      disputeTxIdField;
    @FXML private TextArea                       evidenceArea;
    @FXML private TableView<Dispute>             disputesTable;
    @FXML private TableColumn<Dispute, String>   dIdCol;
    @FXML private TableColumn<Dispute, String>   dTxCol;
    @FXML private TableColumn<Dispute, String>   dStatusCol;
    @FXML private TableColumn<Dispute, String>   dResolutionCol;

    // ── Services & State ──────────────────────────────────────────────────────
    private ApiService      apiService;
    private CppService      cppService;
    private DatabaseService dbService;

    private final ObservableList<InventoryItem> inventoryData = FXCollections.observableArrayList();
    private final ObservableList<Dispute>       disputeData   = FXCollections.observableArrayList();

    private boolean darkMode = false;

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ─────────────────────────────────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        apiService = new ApiService();
        cppService = new CppService();
        dbService  = new DatabaseService();

        setupInventoryTable();
        setupDisputesTable();
        setupTradeCombo();

        loadInventoryFromDb();
        startStatusPolling();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Table setup
    // ─────────────────────────────────────────────────────────────────────────

    private void setupInventoryTable() {
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        itemCol.setCellValueFactory(new PropertyValueFactory<>("item"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        sourceCol.setCellValueFactory(new PropertyValueFactory<>("source"));
        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Colored type badges
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(type.toUpperCase());
                String color = switch (type.toLowerCase()) {
                    case "bought"   -> "#4CAF50";
                    case "sold"     -> "#2196F3";
                    case "waste"    -> "#FF9800";
                    case "transfer" -> "#9C27B0";
                    default         -> "#757575";
                };
                badge.setStyle(
                    "-fx-background-color:" + color + "22;" +
                    "-fx-text-fill:" + color + ";" +
                    "-fx-background-radius:12;" +
                    "-fx-padding:2 8;" +
                    "-fx-font-weight:bold;" +
                    "-fx-font-size:11;"
                );
                setGraphic(badge);
                setText(null);
            }
        });

        // Price formatted
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) { setText(null); return; }
                setText(price > 0 ? String.format("₨%.0f", price) : "—");
            }
        });

        // Waste row highlighting
        inventoryTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("waste-row");
                if (!empty && item != null && "waste".equalsIgnoreCase(item.getType())) {
                    getStyleClass().add("waste-row");
                }
            }
        });

        inventoryTable.setItems(inventoryData);
        inventoryTable.setPlaceholder(new Label("No records yet. Add your first inventory entry above."));
    }

    private void setupDisputesTable() {
        dIdCol.setCellValueFactory(new PropertyValueFactory<>("disputeId"));
        dTxCol.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
        dStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        dResolutionCol.setCellValueFactory(new PropertyValueFactory<>("resolution"));

        dStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label lbl = new Label(status.toUpperCase());
                String color = switch (status.toLowerCase()) {
                    case "open"         -> "#FF9800";
                    case "under_review" -> "#2196F3";
                    case "resolved"     -> "#4CAF50";
                    case "dismissed"    -> "#757575";
                    default             -> "#9E9E9E";
                };
                lbl.setStyle("-fx-background-color:" + color + "22;-fx-text-fill:" + color +
                             ";-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:11;-fx-font-weight:bold;");
                setGraphic(lbl);
                setText(null);
            }
        });

        disputesTable.setItems(disputeData);
        disputesTable.setPlaceholder(new Label("No disputes filed."));
    }

    private void setupTradeCombo() {
        tradeTypeCombo.setItems(FXCollections.observableArrayList("Sell", "Buy"));
        tradeTypeCombo.setValue("Sell");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Input processing
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void processInput() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) {
            setStatus("Please enter an inventory message.", "error");
            return;
        }

        setStatus("Parsing with AI...", "processing");

        Task<ParsedResult> task = new Task<>() {
            @Override
            protected ParsedResult call() throws Exception {
                return apiService.parseText(text);
            }
        };

        task.setOnSucceeded(e -> showConfirmationDialog(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            setStatus("AI offline — using fallback parser.", "error");
            // Create basic result manually
            ParsedResult fallback = buildFallbackResult(text);
            showConfirmationDialog(fallback);
        });

        daemonThread(task);
    }

    private ParsedResult buildFallbackResult(String text) {
        ParsedResult r = new ParsedResult();
        String lower = text.toLowerCase();
        r.setType(lower.contains("waste") || lower.contains("zaya") ? "waste"
                : lower.contains("sold") || lower.contains("becha") ? "sold"
                : "bought");
        r.setItem("item");
        r.setQuantity("1");
        r.setPrice(0.0);
        r.setSource("unknown");
        r.setNeedsConfirmation(true);
        return r;
    }

    private void showConfirmationDialog(ParsedResult result) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Inventory Entry");
        alert.setHeaderText("Review parsed data before saving:");

        String content = String.format(
            "  Type:      %s%n  Item:      %s%n  Quantity:  %s%n  Price:     ₨%.2f%n  Source:    %s%s",
            result.getType(), result.getItem(), result.getQuantity(),
            result.getPrice(), result.getSource(),
            result.isNeedsConfirmation()
                ? "\n\n  ⚠ Parsed by rule engine — please verify." : ""
        );
        alert.setContentText(content);
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> btn = alert.showAndWait();
        if (btn.isPresent() && btn.get() == ButtonType.OK) {
            saveInventoryItem(result);
        } else {
            setStatus("Cancelled.", "");
        }
    }

    private void saveInventoryItem(ParsedResult result) {
        Task<InventoryItem> task = new Task<>() {
            @Override
            protected InventoryItem call() throws Exception {
                InventoryItem item = new InventoryItem();
                item.setType(     result.getType());
                item.setItem(     result.getItem());
                item.setQuantity( result.getQuantity());
                item.setPrice(    result.getPrice());
                item.setSource(   result.getSource());
                item.setTimestamp(LocalDateTime.now().format(TS_FMT));
                return dbService.saveInventoryItem(item);
            }
        };

        task.setOnSucceeded(e -> {
            inventoryData.add(0, task.getValue());
            clearInput();
            setStatus("✓ Saved successfully!", "success");
            updateStats();
            refreshCharts();
            updateRecommendations();
            // Flash the table row
            animateNewRow();
        });

        task.setOnFailed(e -> setStatus("Save failed: " + task.getException().getMessage(), "error"));
        daemonThread(task);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data loading
    // ─────────────────────────────────────────────────────────────────────────

    private void loadInventoryFromDb() {
        Task<List<InventoryItem>> task = new Task<>() {
            @Override
            protected List<InventoryItem> call() throws Exception {
                return dbService.getAllInventoryItems();
            }
        };

        task.setOnSucceeded(e -> {
            inventoryData.setAll(task.getValue());
            updateStats();
            refreshCharts();
            updateRecommendations();
        });

        task.setOnFailed(e ->
            setStatus("DB load error: " + task.getException().getMessage(), "error"));

        daemonThread(task);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats & Charts
    // ─────────────────────────────────────────────────────────────────────────

    private void updateStats() {
        int    total      = inventoryData.size();
        long   wasteCnt   = inventoryData.stream().filter(i -> "waste".equalsIgnoreCase(i.getType())).count();
        double totalVal   = inventoryData.stream()
                .filter(i -> "bought".equalsIgnoreCase(i.getType()))
                .mapToDouble(InventoryItem::getPrice).sum();
        String today      = LocalDate.now().toString();
        long   todayCnt   = inventoryData.stream()
                .filter(i -> i.getTimestamp() != null && i.getTimestamp().startsWith(today))
                .count();
        double wastePct   = total > 0 ? (wasteCnt * 100.0 / total) : 0;
        double lostVal    = inventoryData.stream()
                .filter(i -> "waste".equalsIgnoreCase(i.getType()))
                .mapToDouble(InventoryItem::getPrice).sum();

        totalItemsLabel.setText(String.valueOf(total));
        wasteCountLabel.setText(String.valueOf(wasteCnt));
        totalValueLabel.setText(String.format("₨%.0f", totalVal));
        todayCountLabel.setText(String.valueOf(todayCnt));

        wastePctLabel.setText(String.format("%.1f%%", wastePct));
        lostValueLabel.setText(String.format("₨%.0f", lostVal));
        avgWasteLabel.setText(String.valueOf(wasteCnt > 0 ? wasteCnt : 0));
    }

    private void refreshCharts() {
        if (inventoryData.isEmpty()) return;

        // ── Pie chart: by type ───────────────────────────────────────────────
        Map<String, Long> byType = inventoryData.stream()
                .collect(Collectors.groupingBy(
                    i -> i.getType() != null ? i.getType() : "unknown",
                    Collectors.counting()
                ));
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        byType.forEach((type, count) -> pieData.add(new PieChart.Data(
                type.substring(0, 1).toUpperCase() + type.substring(1) + " (" + count + ")", count)));
        categoryPieChart.setData(pieData);
        categoryPieChart.setLabelsVisible(true);

        // ── Bar chart: last 7 days ────────────────────────────────────────────
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Items");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.toString();
            long cnt = inventoryData.stream()
                    .filter(it -> it.getTimestamp() != null && it.getTimestamp().startsWith(dateStr))
                    .count();
            String dayLabel = date.getDayOfWeek().toString().substring(0, 3);
            series.getData().add(new XYChart.Data<>(dayLabel, cnt));
        }
        dailyBarChart.getData().setAll(series);

        // ── Line chart: value trend ────────────────────────────────────────────
        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
        lineSeries.setName("Value (₨)");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.toString();
            double val = inventoryData.stream()
                    .filter(it -> it.getTimestamp() != null && it.getTimestamp().startsWith(dateStr)
                               && "bought".equalsIgnoreCase(it.getType()))
                    .mapToDouble(InventoryItem::getPrice)
                    .sum();
            lineSeries.getData().add(new XYChart.Data<>(date.getDayOfWeek().toString().substring(0, 3), val));
        }
        trendLineChart.getData().setAll(lineSeries);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recommendations
    // ─────────────────────────────────────────────────────────────────────────

    private void updateRecommendations() {
        recommendationPanel.getChildren().clear();

        if (inventoryData.isEmpty()) {
            addEmptyMsg(recommendationPanel, "Add inventory to get AI recommendations.");
            return;
        }

        Task<List<Recommendation>> task = new Task<>() {
            @Override
            protected List<Recommendation> call() throws Exception {
                return cppService.getRecommendations(inventoryData);
            }
        };

        task.setOnSucceeded(e -> {
            recommendationPanel.getChildren().clear();
            List<Recommendation> recs = task.getValue();
            if (recs.isEmpty()) {
                addEmptyMsg(recommendationPanel, "No recommendations available yet.");
            } else {
                recs.forEach(rec -> recommendationPanel.getChildren().add(buildRecommendationCard(rec)));
            }
        });

        task.setOnFailed(e -> {
            recommendationPanel.getChildren().clear();
            addEmptyMsg(recommendationPanel, "C++ Engine offline — recommendations unavailable.");
        });

        daemonThread(task);
    }

    private VBox buildRecommendationCard(Recommendation rec) {
        VBox card = new VBox(6);
        card.getStyleClass().add("recommendation-item");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label(rec.getType());
        typeLabel.getStyleClass().add("recommendation-title");

        Label scoreLabel = new Label(String.format("%.0f%%", rec.getScore() * 100));
        scoreLabel.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#4CAF50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label pseudonym = new Label("→ " + rec.getPseudonym());
        pseudonym.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1B5E20;");

        header.getChildren().addAll(typeLabel, spacer, pseudonym, scoreLabel);

        Label reason = new Label(rec.getReasoning());
        reason.setStyle("-fx-font-size:12;-fx-text-fill:#555;-fx-wrap-text:true;");
        reason.setWrapText(true);

        card.getChildren().addAll(header, reason);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trade matching
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void findTradeMatches() {
        String type  = tradeTypeCombo.getValue();
        String item  = tradeItemField.getText().trim();
        String qty   = tradeQtyField.getText().trim();
        String price = tradePriceField.getText().trim();

        if (item.isEmpty()) {
            showAlert("Please enter an item name to search for matches.", Alert.AlertType.WARNING);
            return;
        }

        matchesPanel.getChildren().clear();
        Label loading = new Label("Finding best matches...");
        loading.setStyle("-fx-text-fill:#888;-fx-font-size:13;-fx-padding:16;");
        matchesPanel.getChildren().add(loading);

        double targetPrice = 0;
        try { targetPrice = Double.parseDouble(price.replaceAll("[^\\d.]", "")); } catch (Exception ignored) {}
        final double finalPrice = targetPrice;

        Task<List<Recommendation>> task = new Task<>() {
            @Override
            protected List<Recommendation> call() throws Exception {
                return cppService.findMatches(type, item, qty.isEmpty() ? "1" : qty, finalPrice);
            }
        };

        task.setOnSucceeded(e -> {
            matchesPanel.getChildren().clear();
            List<Recommendation> matches = task.getValue();
            if (matches.isEmpty()) {
                addEmptyMsg(matchesPanel, "No matches found for: " + item);
            } else {
                for (int i = 0; i < matches.size(); i++) {
                    matchesPanel.getChildren().add(buildMatchCard(matches.get(i), i + 1));
                }
            }
        });

        task.setOnFailed(e -> {
            matchesPanel.getChildren().clear();
            addEmptyMsg(matchesPanel, "Matching engine offline. C++ service not running.");
        });

        daemonThread(task);
    }

    private HBox buildMatchCard(Recommendation match, int rank) {
        HBox card = new HBox(14);
        card.getStyleClass().add("match-item");
        card.setAlignment(Pos.CENTER_LEFT);

        // Rank badge
        Label rankBadge = new Label("#" + rank);
        rankBadge.setStyle("-fx-background-color:#F5F7FA;-fx-text-fill:#888;" +
                "-fx-font-size:14;-fx-font-weight:bold;" +
                "-fx-min-width:36;-fx-alignment:center;");

        // Score circle
        VBox scoreBox = new VBox(2);
        scoreBox.setAlignment(Pos.CENTER);
        scoreBox.setMinWidth(56);
        Label scoreNum = new Label(String.format("%.0f", match.getScore() * 100));
        scoreNum.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:#4CAF50;");
        Label scoreUnit = new Label("score");
        scoreUnit.setStyle("-fx-font-size:10;-fx-text-fill:#AAA;");
        scoreBox.getChildren().addAll(scoreNum, scoreUnit);

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(match.getPseudonym());
        name.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#222;");
        Label typeLabel = new Label(match.getType().toUpperCase() + " offer");
        typeLabel.setStyle("-fx-font-size:11;-fx-text-fill:#4CAF50;-fx-font-weight:bold;");
        Label reason = new Label(match.getReasoning());
        reason.setStyle("-fx-font-size:11;-fx-text-fill:#777;");
        reason.setWrapText(true);
        info.getChildren().addAll(name, typeLabel, reason);

        // Contact button
        Button contactBtn = new Button("Contact");
        contactBtn.getStyleClass().add("primary-btn");
        contactBtn.setStyle("-fx-padding:7 14;");
        contactBtn.setOnAction(e ->
            showAlert("Contact request sent to " + match.getPseudonym() +
                      ".\nYour DID: did:local:merchant001\nThey will respond via the Trade channel.",
                      Alert.AlertType.INFORMATION));

        card.getChildren().addAll(rankBadge, scoreBox, info, contactBtn);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Disputes
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void lookupTransaction() {
        String txId = disputeTxIdField.getText().trim();
        if (txId.isEmpty()) {
            showAlert("Enter a Transaction ID to look up.", Alert.AlertType.WARNING);
            return;
        }

        Task<Transaction> task = new Task<>() {
            @Override
            protected Transaction call() throws Exception {
                return dbService.getTransaction(txId);
            }
        };

        task.setOnSucceeded(e -> {
            Transaction tx = task.getValue();
            if (tx == null) {
                showAlert("Transaction not found: " + txId, Alert.AlertType.WARNING);
            } else {
                evidenceArea.setText(
                    "Transaction found:\n" +
                    "  Item:   " + tx.getItem() + "\n" +
                    "  Qty:    " + tx.getQuantity() + "\n" +
                    "  Price:  ₨" + tx.getPrice() + "\n" +
                    "  Status: " + tx.getStatus() + "\n" +
                    "  Hash:   " + (tx.getHash() != null ? tx.getHash().substring(0, 16) + "..." : "N/A")
                );
            }
        });

        daemonThread(task);
    }

    @FXML
    private void submitDispute() {
        String txId     = disputeTxIdField.getText().trim();
        String evidence = evidenceArea.getText().trim();

        if (txId.isEmpty() || evidence.isEmpty()) {
            showAlert("Please enter Transaction ID and evidence description.", Alert.AlertType.WARNING);
            return;
        }

        Task<Dispute> task = new Task<>() {
            @Override
            protected Dispute call() throws Exception {
                return dbService.createDispute(txId, evidence);
            }
        };

        task.setOnSucceeded(e -> {
            Dispute d = task.getValue();
            disputeData.add(0, d);
            disputeTxIdField.clear();
            evidenceArea.clear();
            showAlert("Dispute filed successfully!\nDispute ID: " + d.getDisputeId() +
                      "\nStatus: Open — under review.", Alert.AlertType.INFORMATION);
        });

        task.setOnFailed(e ->
            showAlert("Failed to submit dispute: " + task.getException().getMessage(),
                      Alert.AlertType.ERROR));

        daemonThread(task);
    }

    private void loadDisputes() {
        Task<List<Dispute>> task = new Task<>() {
            @Override
            protected List<Dispute> call() throws Exception {
                return dbService.getAllDisputes();
            }
        };
        task.setOnSucceeded(e -> disputeData.setAll(task.getValue()));
        daemonThread(task);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void showDashboard() { switchView("Dashboard",  dashboardView);  activateNav(navDashboard); }
    @FXML private void showAnalytics()  { switchView("Analytics",  analyticsView);  activateNav(navAnalytics);  refreshCharts(); }
    @FXML private void showTrade()      { switchView("Trade",      tradeView);      activateNav(navTrade); }
    @FXML private void showDisputes()   { switchView("Disputes",   disputesView);   activateNav(navDisputes);  loadDisputes(); }

    private void switchView(String title, ScrollPane view) {
        pageTitleLabel.setText(title);
        List.of(dashboardView, analyticsView, tradeView, disputesView)
            .forEach(v -> { v.setVisible(false); v.setManaged(false); });
        view.setVisible(true);
        view.setManaged(true);
    }

    private void activateNav(Button active) {
        List.of(navDashboard, navAnalytics, navTrade, navDisputes)
            .forEach(b -> b.getStyleClass().remove("nav-active"));
        active.getStyleClass().add("nav-active");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dark mode
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void toggleDarkMode() {
        darkMode = !darkMode;
        var root = inputArea.getScene().getRoot();
        if (darkMode) {
            root.getStyleClass().add("dark-mode");
            darkModeToggle.setText("☀  Light Mode");
        } else {
            root.getStyleClass().remove("dark-mode");
            darkModeToggle.setText("🌙  Dark Mode");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Service health polling
    // ─────────────────────────────────────────────────────────────────────────

    private void startStatusPolling() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(15), e -> checkServiceStatus()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        checkServiceStatus(); // immediate first check
    }

    private void checkServiceStatus() {
        Task<boolean[]> task = new Task<>() {
            @Override
            protected boolean[] call() {
                return new boolean[]{ apiService.checkHealth(), cppService.checkHealth() };
            }
        };

        task.setOnSucceeded(e -> {
            boolean[] up = task.getValue();
            updateStatusDot(pythonStatusDot, pythonStatusLabel, up[0], "AI Service");
            updateStatusDot(cppStatusDot,    cppStatusLabel,    up[1], "Engine");
        });

        daemonThread(task);
    }

    private void updateStatusDot(Circle dot, Label label, boolean online, String name) {
        dot.setFill(online ? Color.web("#4CAF50") : Color.web("#EF5350"));
        label.setText(name + (online ? " ✓" : " ✗"));
        label.setStyle("-fx-text-fill:" + (online ? "#4CAF50" : "#EF5350") + ";-fx-font-size:11;");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void clearInput()   { inputArea.clear(); setStatus("", ""); }
    @FXML private void refreshTable() { loadInventoryFromDb(); }

    private void setStatus(String msg, String type) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.getStyleClass().removeAll("status-processing", "status-success", "status-error");
            if (!type.isEmpty()) statusLabel.getStyleClass().add("status-" + type);
        });
    }

    private void showAlert(String msg, Alert.AlertType alertType) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle("Smart Inventory AI");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    private void addEmptyMsg(VBox container, String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill:#AAAAAA;-fx-font-size:13;-fx-padding:20;");
        lbl.setWrapText(true);
        container.getChildren().add(lbl);
    }

    private void animateNewRow() {
        if (inventoryData.isEmpty()) return;
        inventoryTable.scrollTo(0);
        FadeTransition ft = new FadeTransition(Duration.millis(600));
        ft.setFromValue(0.3);
        ft.setToValue(1.0);
        ft.setNode(inventoryTable);
        ft.play();
    }

    private void daemonThread(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
