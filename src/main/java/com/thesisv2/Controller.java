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


public class Controller implements Initializable {

    //~~~setting up the columns~~~
    @FXML
    private TableView<ProductListPopulator> ProductTableView;

    @FXML
    private  TableColumn<ProductListPopulator, Integer> ColumnProductID;
    @FXML
    private  TableColumn<ProductListPopulator, String> ColumnProductDescription;
    @FXML
    private  TableColumn<ProductListPopulator, String > ColumnWarehouses;
    @FXML
    private  TableColumn<ProductListPopulator, Float> ColumnPurchasePrice;
    @FXML
    private  TableColumn<ProductListPopulator, Float> ColumnSellPrice;
    @FXML
    private  TableColumn<ProductListPopulator, Float> ColumnWholesalePrice;
    @FXML
    private  TableColumn<ProductListPopulator, Integer> ColumnStock;
    @FXML
    private  TableColumn<ProductListPopulator, Integer> ColumnPallets;
    @FXML
    private  TableColumn<ProductListPopulator, Integer> ColumnOutofpallet;
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

    ObservableList<ProductListPopulator> ProductListPopulatorObservableList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourcebundle){
        //making the divider in splitpane dynamic
        SplitPane.Divider divider = SplitPaneControll.getDividers().get(0);

        divider.positionProperty().addListener((obs, oldVal, newVal) ->{
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


        try{
            Statement statment = connect.connection.createStatement();
            ResultSet queryOutput = statment.executeQuery(productViewQuery);

            //~~~reads every line from the DB~~~
            while (queryOutput.next()){
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

    private void applyFilters(FilteredList<ProductListPopulator> filteredData) {

        String descriptionFilter = SearchDescription.getText().trim().toUpperCase();
        String allFilter = SearchAll.getText().trim().toUpperCase();
        String idFilter = SearchCode.getText().trim();
        String stockFilter = SearchStock.getText().trim();
        String operator = StockOperator.getValue();
        String warehouseFilter = SearchWarehouse.getText().trim();

        filteredData.setPredicate(product -> {

            if (!descriptionFilter.isBlank()) {
                String description = product.getProductDescription();
                if (description == null || !description.toUpperCase().contains(descriptionFilter)) { return false; }
            }

            if (!allFilter.isBlank()) {
                String description = product.getProductDescription();
                String warehouses = product.getWarehouses();
                String stock = String.valueOf(product.getTotalStock());
                String productId = String.valueOf(product.getProductID());


                boolean matchesDescription = description != null &&
                        description.toUpperCase().contains(allFilter);

                boolean matchesWarehouse = warehouses != null &&
                        warehouses.toUpperCase().contains(allFilter);

                boolean matchesStock = stock != null &&
                        stock.contains(allFilter);

                boolean matchesId = productId != null &&
                        productId.contains(allFilter);

                if (!matchesDescription && !matchesWarehouse && !matchesStock && !matchesId) { return false; }
            }

            if (!idFilter.isBlank()) {
                String productId = product.getProductID().toString();
                if (!productId.contains(idFilter)) { return false; }
            }

            if (!stockFilter.isBlank()) {
                try {
                    int filterValue = Integer.parseInt(stockFilter);
                    int productStock = product.getTotalStock();

                    switch (operator) {
                        case "<=":
                            if (!(productStock <= filterValue)) return false;
                            break;

                        case "=":
                            if (!(productStock == filterValue)) return false;
                            break;

                        case ">=":
                            if (!(productStock >= filterValue)) return false;
                            break;
                    }
                } catch (NumberFormatException e) {
                    return false; //invalid input -> show nothing
                }
            }

            if (!warehouseFilter.isBlank()){
                String warehouse = product.getWarehouses().toString();
                if (!warehouse.contains(warehouseFilter)){ return false; }
            }

            return true;
        });
    }



    //~~~~~ CLOSE HANDLER ~~~~~
    @FXML
    private void HandleCloseButton(ActionEvent event){
        Stage stage = (Stage) ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow();
        stage.close();
    }

    //~~~~ DELETE HANDLER ~~~~~
    @FXML
    private void HandleEditDelete(ActionEvent event){
        // Get selected row
        ProductListPopulator selected = ProductTableView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Δεν επιλέχθηκε είδος");
            alert.setHeaderText(null);
            alert.setContentText("Παρακαλώ επίλεξε ένα είδος από τον πίνακα για διαγραφή.");
            alert.showAndWait();
            return;
        }

        //Confirmation popup with product description
        String desc = selected.getProductDescription();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Επιβεβαίωση Διαγραφής");
        confirm.setHeaderText("Θέλεις σίγουρα να διαγράψεις το ακόλουθο είδος;");
        confirm.setContentText(desc == null ? "(χωρίς περιγραφή)" : desc);

        ButtonType yes = new ButtonType("Ναι", ButtonBar.ButtonData.YES);
        ButtonType no  = new ButtonType("Όχι", ButtonBar.ButtonData.NO);
        confirm.getButtonTypes().setAll(yes, no);

        var result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != yes) {
            return; //user cancelled
        }

        //Delete from DB
        int productId = selected.getProductID();

        DBConnection connect = new DBConnection();
        Connection connection = connect.getConnection();

        String deleteLinksSql = "DELETE FROM prod_warehouse_link WHERE PRODUCT = " + productId;
        String deleteProductSql = "DELETE FROM products WHERE ProductID = " + productId;

        try (Statement st = connection.createStatement()) {

            // If you have FK constraints, delete children first
            st.executeUpdate(deleteLinksSql);
            int affected = st.executeUpdate(deleteProductSql);

            if (affected > 0) {
                //Remove from the observable list (table updates automatically)
                ProductListPopulatorObservableList.remove(selected);

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Διαγραφή");
                ok.setHeaderText(null);
                ok.setContentText("Το είδος διαγράφηκε επιτυχώς.");
                ok.showAndWait();
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Σφάλμα");
                err.setHeaderText("Δεν έγινε διαγραφή");
                err.setContentText("Δεν βρέθηκε το είδος στη βάση (ίσως έχει ήδη διαγραφεί).");
                err.showAndWait();
            }

        } catch (SQLException e) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Σφάλμα Βάσης");
            err.setHeaderText("Αποτυχία διαγραφής");
            err.setContentText(e.getMessage());
            err.showAndWait();
            e.printStackTrace();
        }
    }

    //~~~~~ NEW HANDLER ~~~~~
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
            try { connection.rollback(); } catch (SQLException ignored) {}
            new Alert(Alert.AlertType.ERROR, "Σφάλμα βάσης: " + e.getMessage()).showAndWait();
            e.printStackTrace();
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    //~~~~~ NEW HANDLER HELPERS ~~~~~
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









}





