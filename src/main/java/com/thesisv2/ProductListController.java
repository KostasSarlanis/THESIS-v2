package com.thesisv2;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.geometry.Insets;
import java.util.regex.Pattern;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.beans.binding.Bindings;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.PreparedStatement;
import java.util.*;


public class ProductListController implements Initializable {

    //~~~setting up the columns~~~
    @FXML
    private TableView<ProductListPopulator> ProductTableView;
    @FXML
    private TableColumn<ProductListPopulator, Integer> ColumnProductID;
    @FXML
    private TableColumn<ProductListPopulator, String> ColumnProductDescription;
    @FXML
    private TableColumn<ProductListPopulator, String> ColumnWarehouses;
    @FXML
    private TableColumn<ProductListPopulator, Float> ColumnPurchasePrice;
    @FXML
    private TableColumn<ProductListPopulator, Float> ColumnSellPrice;
    @FXML
    private TableColumn<ProductListPopulator, Float> ColumnWholesalePrice;
    @FXML
    private TableColumn<ProductListPopulator, Integer> ColumnStock;
    @FXML
    private TableColumn<ProductListPopulator, Integer> ColumnPallets;
    @FXML
    private TableColumn<ProductListPopulator, Integer> ColumnOutofpallet;
    @FXML
    private TableColumn<ProductListPopulator, Integer> ColumnPalletsize;
    @FXML
    private TextField SearchDescription;
    @FXML
    private TextField SearchAll;
    @FXML
    private TextField SearchCode;
    @FXML
    private TextField SearchWarehouse;
    @FXML
    private TextField SearchStock;
    @FXML
    private ComboBox<String> StockOperator;
    @FXML
    private Label ResultLabel;
    @FXML
    private SplitPane SplitPaneControll;

    @FXML
    private MenuItem ButtonClose;
    @FXML
    private MenuItem EditDelete;
    @FXML
    private MenuItem EditEdit;


    ObservableList<ProductListPopulator> ProductListPopulatorObservableList = FXCollections.observableArrayList();

    private void setupContextMenu() {
        ProductTableView.setRowFactory(tv -> {
            TableRow<ProductListPopulator> row = new TableRow<>();

            ContextMenu contextMenu = new ContextMenu();

            MenuItem editItem = new MenuItem("Edit");
            MenuItem deleteItem = new MenuItem("Delete");

            editItem.setOnAction(event -> {
                ProductListPopulator selectedProduct = row.getItem();
                if (selectedProduct != null) {
                    ProductTableView.getSelectionModel().select(selectedProduct);
                    openEditDialog(selectedProduct);
                }
            });

            deleteItem.setOnAction(event -> {
                ProductListPopulator selectedProduct = row.getItem();
                if (selectedProduct != null) {
                    ProductTableView.getSelectionModel().select(selectedProduct);
                    confirmAndDelete(selectedProduct);
                }
            });

            contextMenu.getItems().addAll(editItem, deleteItem);

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            return row;
        });
    }


    @Override
    public void initialize(URL url, ResourceBundle resourcebundle) {
        setupContextMenu();
        //making the divider in splitpane dynamic
        SplitPane.Divider divider = SplitPaneControll.getDividers().get(0);

        divider.positionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() != 0.12) divider.setPosition(0.12);
        });

        //connecting to database
        DBConnection connect = new DBConnection();
        Connection connection = connect.getConnection();

        //~~~setting up a var. with the query~~~
        String productViewQuery = "SELECT\n" +
                "    p.ProductID,\n" +
                "    p.ProductDescription,\n" +
                "    (\n" +
                "       SELECT GROUP_CONCAT(DISTINCT WAREHOUSE ORDER BY WAREHOUSE)\n" +
                "       FROM prod_warehouse_link l\n" +
                "       WHERE l.PRODUCT = p.ProductID && l.STOCK > 0\n" +
                "    ) AS Warehouses,\n" +
                "    p.PurchasedPrice,\n" +
                "    p.SellPrice,\n" +
                "    p.WholesalePrice,\n" +
                "    COALESCE(t.TotalStock, 0) AS TotalStock,\n" +
                "    COALESCE(t.TotalPallets, 0) AS TotalPallets,\n" +
                "    COALESCE(t.OutOfPallet, 0) AS OutOfPallet,\n" +
                "    p.PalletSize\n" +
                "FROM products p\n" +
                "\n" +
                "LEFT JOIN (\n" +
                "    SELECT\n" +
                "        PRODUCT,\n" +
                "        SUM(STOCK) AS TotalStock,\n" +
                "        SUM(FLOOR(STOCK / (SELECT PalletSize FROM products WHERE ProductID = PRODUCT))) AS TotalPallets,\n" +
                "        SUM(STOCK % (SELECT PalletSize FROM products WHERE ProductID = PRODUCT)) AS OutOfPallet\n" +
                "    FROM prod_warehouse_link\n" +
                "    GROUP BY PRODUCT\n" +
                ") t ON t.PRODUCT = p.ProductID;";


        try {
            Statement statment = connect.connection.createStatement();
            ResultSet queryOutput = statment.executeQuery(productViewQuery);

            //~~~reads every line from the DB~~~
            while (queryOutput.next()) {
                Integer queryProductID = queryOutput.getInt("ProductID");
                String queryProductDescription = queryOutput.getString("ProductDescription");
                String queryWarehouses = queryOutput.getString("Warehouses");
                Float queryPurchasedPrice = queryOutput.getFloat("PurchasedPrice");
                Float querySellPrice = queryOutput.getFloat("SellPrice");
                Float queryWholesalePrice = queryOutput.getFloat("WholesalePrice");
                Integer queryTotalStock = queryOutput.getInt("TotalStock");
                Integer queryTotalPallets = queryOutput.getInt("TotalPallets");
                Integer queryOutOfPallet = queryOutput.getInt("OutOfPallet");
                Integer queryPalletSize = queryOutput.getInt("PalletSize");

                //~~~Populating the observable list~~~
                ProductListPopulatorObservableList.add(new ProductListPopulator(queryProductID, queryProductDescription, queryWarehouses,
                        queryPurchasedPrice, querySellPrice, queryWholesalePrice, queryTotalStock,
                        queryTotalPallets, queryOutOfPallet, queryPalletSize));
            }

            //~~~PropertyValueFactory corresponds to the new ProducListPopulator fields~~~
            ColumnProductID.setCellValueFactory(new PropertyValueFactory<>("ProductID"));
            ColumnProductDescription.setCellValueFactory(new PropertyValueFactory<>("ProductDescription"));
            ColumnWarehouses.setCellValueFactory(new PropertyValueFactory<>("Warehouses"));
            ColumnPurchasePrice.setCellValueFactory(new PropertyValueFactory<>("PurchasedPrice"));
            ColumnSellPrice.setCellValueFactory(new PropertyValueFactory<>("SellPrice"));
            ColumnWholesalePrice.setCellValueFactory(new PropertyValueFactory<>("WholesalePrice"));
            ColumnStock.setCellValueFactory(new PropertyValueFactory<>("TotalStock"));
            ColumnPallets.setCellValueFactory(new PropertyValueFactory<>("TotalPallets"));
            ColumnOutofpallet.setCellValueFactory(new PropertyValueFactory<>("OutOfPallet"));
            ColumnPalletsize.setCellValueFactory(new PropertyValueFactory<>("PalletSize"));

            //~~~initializing value of stock operator~~~
            StockOperator.setValue("=");

            //~~~filtering~~~
            FilteredList<ProductListPopulator> filteredData =
                    new FilteredList<>(ProductListPopulatorObservableList, b -> true);

            SearchDescription.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
            SearchAll.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
            SearchCode.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
            SearchStock.textProperty().addListener((obs, oldVal, newval) -> applyFilters(filteredData));
            SearchWarehouse.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));

            SortedList<ProductListPopulator> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(ProductTableView.comparatorProperty());

            ProductTableView.setItems(sortedData);

            //counter for the results
            ResultLabel.textProperty().bind(
                    javafx.beans.binding.Bindings.size(sortedData).asString("Αποτελέσματα: %d")
            );


        } catch (SQLException e) {
            System.out.println("Error in getting data and setting the table.");
            Logger.getLogger(ProductListPopulator.class.getName()).log(Level.SEVERE, null, e);
            e.printStackTrace();
        }

    }

    //          ~~~~~ REMOVES GREEK TONES FROM TEXT ~~~~~                                                                  .
    private String removeGreekTones(String text) {
        if (text == null) return "";

        return text
                .replace('ά', 'α')
                .replace('έ', 'ε')
                .replace('ή', 'η')
                .replace('ί', 'ι')
                .replace('ό', 'ο')
                .replace('ύ', 'υ')
                .replace('ώ', 'ω')
                .replace('ϊ', 'ι')
                .replace('ΐ', 'ι')
                .replace('ϋ', 'υ')
                .replace('ΰ', 'υ')
                .replace('ς', 'σ');
    }

    private void applyFilters(FilteredList<ProductListPopulator> filteredData) {

        String descriptionFilter = SearchDescription.getText().trim();
        String allFilter = SearchAll.getText().trim();
        String idFilter = SearchCode.getText().trim();
        String stockFilter = SearchStock.getText().trim();
        String operator = StockOperator.getValue();
        String warehouseFilter = SearchWarehouse.getText().trim();

        filteredData.setPredicate(product -> {

            if (!descriptionFilter.isBlank()) {
                String description = product.getProductDescription();
                if (!matchesWildcard(description, descriptionFilter, true)) {
                    return false;
                }
            }

            if (!allFilter.isBlank()) {
                String description = product.getProductDescription();
                String warehouses = product.getWarehouses();
                String stock = String.valueOf(product.getTotalStock());
                String productId = String.valueOf(product.getProductID());

                boolean matchesDescription = matchesWildcard(description, allFilter, true);
                boolean matchesWarehouse = matchesWildcard(warehouses, allFilter, true);
                boolean matchesStock = matchesWildcard(stock, allFilter, false);
                boolean matchesId = matchesWildcard(productId, allFilter, false);

                if (!matchesDescription && !matchesWarehouse && !matchesStock && !matchesId) {
                    return false;
                }
            }

            if (!idFilter.isBlank()) {
                String productId = String.valueOf(product.getProductID());
                if (!matchesWildcard(productId, idFilter, false)) {
                    return false;
                }
            }

            if (!stockFilter.isBlank()) {
                try {
                    int filterValue = Integer.parseInt(stockFilter);
                    int productStock = product.getTotalStock();

                    switch (operator) {
                        case "<=":
                            if (productStock > filterValue) return false;
                            break;
                        case "=":
                            if (productStock != filterValue) return false;
                            break;
                        case ">=":
                            if (productStock < filterValue) return false;
                            break;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }

            if (!warehouseFilter.isBlank()) {
                String warehouse = product.getWarehouses();
                if (!matchesWildcard(warehouse, warehouseFilter, true)) {
                    return false;
                }
            }

            return true;
        });
    }



    //~~~~ DELETE HANDLER ~~~~~                                                                                        .
    private void confirmAndDelete(ProductListPopulator product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Product");
        alert.setHeaderText("Delete Product ID: " + product.getProductID());
        alert.setContentText("Are you sure?");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteProductById(product.getProductID());
        }
    }

    private void deleteProductById(int productId) {
        DBConnection connect = new DBConnection();
        Connection connection = connect.getConnection();

        String deleteLinksSql = "DELETE FROM prod_warehouse_link WHERE PRODUCT = ?";
        String deleteProductSql = "DELETE FROM products WHERE ProductID = ?";

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement ps1 = connection.prepareStatement(deleteLinksSql);
                 PreparedStatement ps2 = connection.prepareStatement(deleteProductSql)) {

                ps1.setInt(1, productId);
                ps1.executeUpdate();

                ps2.setInt(1, productId);
                int affected = ps2.executeUpdate();

                connection.commit();

                if (affected > 0) {
                    ProductListPopulatorObservableList.removeIf(p -> p.getProductID() == productId);
                    ProductTableView.refresh();

                    new Alert(Alert.AlertType.INFORMATION, "Το είδος διαγράφηκε επιτυχώς.").showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Δεν βρέθηκε το είδος στη βάση.").showAndWait();
                }
            }

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            new Alert(Alert.AlertType.ERROR, "Σφάλμα βάσης: " + e.getMessage()).showAndWait();
            e.printStackTrace();
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    @FXML
    private void HandleEditDelete(ActionEvent event) {
        ProductListPopulator selected = ProductTableView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Δεν επιλέχθηκε είδος");
            alert.setHeaderText(null);
            alert.setContentText("Παρακαλώ επίλεξε ένα είδος από τον πίνακα για διαγραφή.");
            alert.showAndWait();
            return;
        }

        confirmAndDelete(selected);
    }

    //~~~~~ NEW HANDLER ~~~~~                                                                                          .
    @FXML
    private void HandleEditNew(ActionEvent event) {

        DBConnection connect = new DBConnection();
        Connection connection = connect.getConnection();

        //load warehouse names for the dropdowns (see helper below)
        List<String> allWarehouses;
        try {
            allWarehouses = loadWarehouses(connection);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Δεν μπόρεσα να φορτώσω αποθήκες: " + e.getMessage()).showAndWait();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Νέο προϊόν");
        dialog.setHeaderText("Καταχώριση νέου προϊόντος");
        dialog.setResizable(true);

        ButtonType saveBtn = new ButtonType("Αποθήκευση", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        //product fields (no ProductID)
        TextField tfDescription = new TextField();
        TextField tfPurchase = new TextField();
        TextField tfSell = new TextField();
        TextField tfWholesale = new TextField();
        TextField tfPalletSize = new TextField();

        //container for warehouse-stock rows
        VBox stockRowsBox = new VBox(8);
        stockRowsBox.setFillWidth(true);
        ScrollPane stockScroll = new ScrollPane(stockRowsBox);
        stockScroll.setFitToWidth(true);
        stockScroll.setFitToHeight(true);
        stockScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Button btnAddRow = new Button("Προσθήκη αποθήκης");
        btnAddRow.setOnAction(ae -> stockRowsBox.getChildren().add(createWarehouseStockRow(allWarehouses, stockRowsBox)));

        //start with 1 row
        stockRowsBox.getChildren().add(createWarehouseStockRow(allWarehouses, stockRowsBox));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        int r = 0;
        grid.add(new Label("Περιγραφή:"), 0, r);
        grid.add(tfDescription, 1, r++);

        grid.add(new Label("Τιμή αγοράς:"), 0, r);
        grid.add(tfPurchase, 1, r++);

        grid.add(new Label("Τιμή λιανικής:"), 0, r);
        grid.add(tfSell, 1, r++);

        grid.add(new Label("Τιμή χονδρικής:"), 0, r);
        grid.add(tfWholesale, 1, r++);

        grid.add(new Label("Τεμ. ανά παλέτα:"), 0, r);
        grid.add(tfPalletSize, 1, r++);

        grid.add(new Separator(), 0, r++, 2, 1);

        grid.add(new Label("Απόθεμα ανά αποθήκη:"), 0, r++, 2, 1);
        grid.add(btnAddRow, 0, r++, 2, 1);
        grid.add(stockScroll, 0, r, 2, 1);

        GridPane.setVgrow(stockScroll, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefSize(350, 600);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHalignment(HPos.CENTER);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveBtn) return;

        //validate and parse
        String description = tfDescription.getText().trim();
        if (description.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Η περιγραφή είναι υποχρεωτική.").showAndWait();
            return;
        }

        float purchase, sell, wholesale;
        int palletSize;
        try {
            purchase = Float.parseFloat(tfPurchase.getText().trim());
            sell = Float.parseFloat(tfSell.getText().trim());
            wholesale = Float.parseFloat(tfWholesale.getText().trim());
            palletSize = Integer.parseInt(tfPalletSize.getText().trim());
            if (palletSize <= 0) throw new NumberFormatException("PalletSize must be > 0");
        } catch (NumberFormatException nfe) {
            new Alert(Alert.AlertType.ERROR, "Λάθος τιμές αριθμών.").showAndWait();
            return;
        }

        //read warehouse rows into a map warehouse -> stock
        Map<String, Integer> warehouseStock = new LinkedHashMap<>();
        for (var node : stockRowsBox.getChildren()) {
            if (!(node instanceof HBox)) continue;
            HBox row = (HBox) node;

            @SuppressWarnings("unchecked")
            ComboBox<String> cb = (ComboBox<String>) row.getChildren().get(0);
            TextField tfStock = (TextField) row.getChildren().get(1);

            String wh = cb.getValue();
            String stockText = tfStock.getText().trim();

            // allow empty rows -> skip
            if ((wh == null || wh.isBlank()) && stockText.isBlank()) continue;

            if (wh == null || wh.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Διάλεξε αποθήκη σε όλες τις γραμμές.").showAndWait();
                return;
            }

            int stock;
            try {
                stock = Integer.parseInt(stockText);
                if (stock < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING, "Το stock πρέπει να είναι ακέραιος αριθμός ≥ 0.").showAndWait();
                return;
            }

            if (warehouseStock.containsKey(wh)) {
                new Alert(Alert.AlertType.WARNING, "Η αποθήκη \"" + wh + "\" έχει δηλωθεί δύο φορές.").showAndWait();
                return;
            }

            warehouseStock.put(wh, stock);
        }

        //insert to DB (products + prod_warehouse_link)
        try {
            connection.setAutoCommit(false);

            int newId = findFirstAvailableProductId(connection);

            String insertProduct =
                    "INSERT INTO products (ProductID, ProductDescription, PurchasedPrice, SellPrice, WholesalePrice, PalletSize) " +
                            "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = connection.prepareStatement(insertProduct)) {
                ps.setInt(1, newId);
                ps.setString(2, description);
                ps.setFloat(3, purchase);
                ps.setFloat(4, sell);
                ps.setFloat(5, wholesale);
                ps.setInt(6, palletSize);
                ps.executeUpdate();
            }

            if (!warehouseStock.isEmpty()) {
                String insertLink =
                        "INSERT INTO prod_warehouse_link (PRODUCT, WAREHOUSE, STOCK) VALUES (?, ?, ?)";

                try (PreparedStatement ps = connection.prepareStatement(insertLink)) {
                    for (var entry : warehouseStock.entrySet()) {
                        ps.setInt(1, newId);
                        ps.setString(2, entry.getKey());
                        ps.setInt(3, entry.getValue());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            connection.commit();

            //update UI list quickly (compute totals from inserted data)
            int totalStock = warehouseStock.values().stream().mapToInt(Integer::intValue).sum();
            int totalPallets = palletSize > 0 ? warehouseStock.values().stream().mapToInt(s -> s / palletSize).sum() : 0;
            int outOfPallet = palletSize > 0 ? warehouseStock.values().stream().mapToInt(s -> s % palletSize).sum() : 0;
            String warehousesStr = warehouseStock.entrySet().stream()
                    .filter(e -> e.getValue() != null && e.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .sorted()
                    .reduce((a, b) -> a + "," + b)
                    .orElse(null);

            ProductListPopulatorObservableList.add(
                    new ProductListPopulator(newId, description, warehousesStr, purchase, sell, wholesale,
                            totalStock, totalPallets, outOfPallet, palletSize)
            );

            new Alert(Alert.AlertType.INFORMATION, "Το προϊόν καταχωρήθηκε με ID: " + newId).showAndWait();

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            new Alert(Alert.AlertType.ERROR, "Σφάλμα βάσης: " + e.getMessage()).showAndWait();
            e.printStackTrace();
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    //~~~~~ NEW HANDLER HELPERS ~~~~~                                                                                  .
    private boolean matchesWildcard(String value, String filter, boolean ignoreCase) {
        if (filter == null || filter.isBlank()) return true;
        if (value == null) return false;

        String sourceValue = value;
        String sourceFilter = filter;

        if (ignoreCase) {
            sourceValue = removeGreekTones(sourceValue.toLowerCase(new Locale("el", "GR")));
            sourceFilter = removeGreekTones(sourceFilter.toLowerCase(new Locale("el", "GR")));
        }

        // If there is no *, do normal contains search
        if (!sourceFilter.contains("*")) {
            return sourceValue.contains(sourceFilter);
        }

        // Collapse multiple * into one *
        sourceFilter = sourceFilter.replaceAll("\\*+", "*");

        StringBuilder regexBuilder = new StringBuilder("^");

        String[] parts = sourceFilter.split("\\*", -1);

        for (int i = 0; i < parts.length; i++) {
            regexBuilder.append(Pattern.quote(parts[i]));
            if (i < parts.length - 1) {
                regexBuilder.append(".*");
            }
        }

        regexBuilder.append("$");

        String regex = regexBuilder.toString();

        return Pattern.compile(regex, Pattern.UNICODE_CASE)
                .matcher(sourceValue)
                .matches();
    }

    private HBox createWarehouseStockRow(List<String> warehouses, VBox container) {
        ComboBox<String> cbWarehouse = new ComboBox<>();
        cbWarehouse.getItems().addAll(warehouses);
        cbWarehouse.setPrefWidth(180);

        TextField tfStock = new TextField();
        tfStock.setPromptText("Stock");
        tfStock.setPrefWidth(90);

        Button btnRemove = new Button("—");
        btnRemove.setOnAction(e -> container.getChildren().remove(btnRemove.getParent()));

        HBox row = new HBox(8, cbWarehouse, tfStock, btnRemove);
        return row;
    }

    private List<String> loadWarehouses(Connection connection) throws SQLException {
        //uses existing warehouse values from link table
        String sql = "SELECT DISTINCT WAREHOUSE FROM prod_warehouse_link ORDER BY WAREHOUSE";
        List<String> list = new ArrayList<>();

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String w = rs.getString(1);
                if (w != null && !w.isBlank()) list.add(w);
            }
        }

        return list;
    }

    private int findFirstAvailableProductId(Connection connection) throws SQLException {
        String sql =
                "SELECT CASE " +
                        "  WHEN NOT EXISTS (SELECT 1 FROM products WHERE ProductID = 1) THEN 1 " +
                        "  ELSE ( " +
                        "    SELECT MIN(p1.ProductID) + 1 " +
                        "    FROM products p1 " +
                        "    LEFT JOIN products p2 ON p2.ProductID = p1.ProductID + 1 " +
                        "    WHERE p2.ProductID IS NULL " +
                        "  ) " +
                        "END AS nextId";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("nextId");
        }
        return 1;
    }

    //~~~~~ EDIT HANDLER ~~~~~                                                                                         .
    private void openEditDialog(ProductListPopulator selected) {
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Δεν επιλέχθηκε είδος");
            alert.setHeaderText(null);
            alert.setContentText("Παρακαλώ επίλεξε ένα είδος από τον πίνακα για επεξεργασία.");
            alert.showAndWait();
            return;
        }

        DBConnection connect = new DBConnection();
        Connection connection = connect.getConnection();

        List<String> allWarehouses;
        Map<String, Integer> existingWarehouseStock;

        try {
            allWarehouses = loadWarehouses(connection);
            existingWarehouseStock = loadWarehouseStockForProduct(connection, selected.getProductID());
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Δεν μπόρεσα να φορτώσω τα στοιχεία του προϊόντος: " + e.getMessage()).showAndWait();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Επεξεργασία προϊόντος");
        dialog.setHeaderText("Επεξεργασία στοιχείων προϊόντος");
        dialog.setResizable(true);

        ButtonType saveBtn = new ButtonType("Αποθήκευση", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField tfProductId = new TextField(String.valueOf(selected.getProductID()));
        tfProductId.setEditable(false);
        tfProductId.setDisable(true);

        TextField tfDescription = new TextField(selected.getProductDescription() != null ? selected.getProductDescription() : "");
        TextField tfPurchase = new TextField(String.valueOf(selected.getPurchasedPrice()));
        TextField tfSell = new TextField(String.valueOf(selected.getSellPrice()));
        TextField tfWholesale = new TextField(String.valueOf(selected.getWholesalePrice()));
        TextField tfPalletSize = new TextField(String.valueOf(selected.getPalletSize()));

        VBox stockRowsBox = new VBox(8);
        stockRowsBox.setFillWidth(true);

        ScrollPane stockScroll = new ScrollPane(stockRowsBox);
        stockScroll.setFitToWidth(true);
        stockScroll.setFitToHeight(true);
        stockScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Button btnAddRow = new Button("Προσθήκη αποθήκης");
        btnAddRow.setOnAction(ae -> stockRowsBox.getChildren().add(createWarehouseStockRow(allWarehouses, stockRowsBox)));

        if (existingWarehouseStock.isEmpty()) {
            stockRowsBox.getChildren().add(createWarehouseStockRow(allWarehouses, stockRowsBox));
        } else {
            for (Map.Entry<String, Integer> entry : existingWarehouseStock.entrySet()) {
                stockRowsBox.getChildren().add(
                        createWarehouseStockRowWithValues(allWarehouses, stockRowsBox, entry.getKey(), entry.getValue())
                );
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        int r = 0;
        grid.add(new Label("Product ID:"), 0, r);
        grid.add(tfProductId, 1, r++);

        grid.add(new Label("Περιγραφή:"), 0, r);
        grid.add(tfDescription, 1, r++);

        grid.add(new Label("Τιμή αγοράς:"), 0, r);
        grid.add(tfPurchase, 1, r++);

        grid.add(new Label("Τιμή λιανικής:"), 0, r);
        grid.add(tfSell, 1, r++);

        grid.add(new Label("Τιμή χονδρικής:"), 0, r);
        grid.add(tfWholesale, 1, r++);

        grid.add(new Label("Τεμ. ανά παλέτα:"), 0, r);
        grid.add(tfPalletSize, 1, r++);

        grid.add(new Separator(), 0, r++, 2, 1);

        grid.add(new Label("Απόθεμα ανά αποθήκη:"), 0, r++, 2, 1);
        grid.add(btnAddRow, 0, r++, 2, 1);
        grid.add(stockScroll, 0, r, 2, 1);

        GridPane.setVgrow(stockScroll, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefSize(350, 600);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHalignment(HPos.CENTER);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveBtn) return;

        String description = tfDescription.getText().trim();
        if (description.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Η περιγραφή είναι υποχρεωτική.").showAndWait();
            return;
        }

        float purchase, sell, wholesale;
        int palletSize;

        try {
            purchase = Float.parseFloat(tfPurchase.getText().trim());
            sell = Float.parseFloat(tfSell.getText().trim());
            wholesale = Float.parseFloat(tfWholesale.getText().trim());
            palletSize = Integer.parseInt(tfPalletSize.getText().trim());

            if (palletSize <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.ERROR, "Λάθος τιμές αριθμών.").showAndWait();
            return;
        }

        Map<String, Integer> warehouseStock = new LinkedHashMap<>();

        for (var node : stockRowsBox.getChildren()) {
            if (!(node instanceof HBox)) continue;
            HBox row = (HBox) node;

            @SuppressWarnings("unchecked")
            ComboBox<String> cb = (ComboBox<String>) row.getChildren().get(0);
            TextField tfStock = (TextField) row.getChildren().get(1);

            String wh = cb.getValue();
            String stockText = tfStock.getText().trim();

            if ((wh == null || wh.isBlank()) && stockText.isBlank()) continue;

            if (wh == null || wh.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Διάλεξε αποθήκη σε όλες τις γραμμές.").showAndWait();
                return;
            }

            int stock;
            try {
                stock = Integer.parseInt(stockText);
                if (stock < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING, "Το stock πρέπει να είναι ακέραιος αριθμός ≥ 0.").showAndWait();
                return;
            }

            if (warehouseStock.containsKey(wh)) {
                new Alert(Alert.AlertType.WARNING, "Η αποθήκη \"" + wh + "\" έχει δηλωθεί δύο φορές.").showAndWait();
                return;
            }

            warehouseStock.put(wh, stock);
        }

        int productId = selected.getProductID();

        try {
            connection.setAutoCommit(false);

            String updateProductSql =
                    "UPDATE products SET ProductDescription = ?, PurchasedPrice = ?, SellPrice = ?, WholesalePrice = ?, PalletSize = ? " +
                            "WHERE ProductID = ?";

            try (PreparedStatement ps = connection.prepareStatement(updateProductSql)) {
                ps.setString(1, description);
                ps.setFloat(2, purchase);
                ps.setFloat(3, sell);
                ps.setFloat(4, wholesale);
                ps.setInt(5, palletSize);
                ps.setInt(6, productId);
                ps.executeUpdate();
            }

            String deleteLinksSql = "DELETE FROM prod_warehouse_link WHERE PRODUCT = ?";
            try (PreparedStatement ps = connection.prepareStatement(deleteLinksSql)) {
                ps.setInt(1, productId);
                ps.executeUpdate();
            }

            if (!warehouseStock.isEmpty()) {
                String insertLinkSql = "INSERT INTO prod_warehouse_link (PRODUCT, WAREHOUSE, STOCK) VALUES (?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(insertLinkSql)) {
                    for (Map.Entry<String, Integer> entry : warehouseStock.entrySet()) {
                        ps.setInt(1, productId);
                        ps.setString(2, entry.getKey());
                        ps.setInt(3, entry.getValue());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            connection.commit();

            int totalStock = warehouseStock.values().stream().mapToInt(Integer::intValue).sum();
            int totalPallets = warehouseStock.values().stream().mapToInt(s -> s / palletSize).sum();
            int outOfPallet = warehouseStock.values().stream().mapToInt(s -> s % palletSize).sum();

            String warehousesStr = warehouseStock.entrySet().stream()
                    .filter(e -> e.getValue() != null && e.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .sorted()
                    .reduce((a, b) -> a + "," + b)
                    .orElse(null);

            selected.setProductDescription(description);
            selected.setPurchasedPrice(purchase);
            selected.setSellPrice(sell);
            selected.setWholesalePrice(wholesale);
            selected.setWarehouses(warehousesStr);
            selected.setTotalStock(totalStock);
            selected.setTotalPallets(totalPallets);
            selected.setOutOfPallet(outOfPallet);
            selected.setPalletSize(palletSize);

            ProductTableView.refresh();

            new Alert(Alert.AlertType.INFORMATION, "Το προϊόν ενημερώθηκε επιτυχώς.").showAndWait();

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            new Alert(Alert.AlertType.ERROR, "Σφάλμα βάσης: " + e.getMessage()).showAndWait();
            e.printStackTrace();
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    @FXML
    private void HandleEditEdit(ActionEvent event) {
        ProductListPopulator selected = ProductTableView.getSelectionModel().getSelectedItem();
        openEditDialog(selected);
    }

    //~~~~~ EDIT HADLER HELPERS ~~~~~                                                                                  .
    private Map<String, Integer> loadWarehouseStockForProduct(Connection connection, int productId) throws SQLException {
        String sql = "SELECT WAREHOUSE, STOCK FROM prod_warehouse_link WHERE PRODUCT = ? ORDER BY WAREHOUSE";
        Map<String, Integer> result = new LinkedHashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, productId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String warehouse = rs.getString("WAREHOUSE");
                    int stock = rs.getInt("STOCK");
                    result.put(warehouse, stock);
                }
            }
        }

        return result;
    }

    private HBox createWarehouseStockRowWithValues(List<String> warehouses, VBox container, String selectedWarehouse, int stockValue) {
        ComboBox<String> cbWarehouse = new ComboBox<>();
        cbWarehouse.getItems().addAll(warehouses);
        cbWarehouse.setPrefWidth(180);
        cbWarehouse.setValue(selectedWarehouse);

        TextField tfStock = new TextField(String.valueOf(stockValue));
        tfStock.setPromptText("Stock");
        tfStock.setPrefWidth(90);

        Button btnRemove = new Button("-");
        btnRemove.setOnAction(e -> container.getChildren().remove(btnRemove.getParent()));

        return new HBox(8, cbWarehouse, tfStock, btnRemove);
    }











}