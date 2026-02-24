package com.thesisv2;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


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

    ObservableList<ProductListPopulator> ProductListPopulatorObservableList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourcebundle){
        DBConnection connect = new DBConnection();
        Connection connection = connect.getConnection();

        //~~~setting up a var. with the query~~~
        String productViewQuery = "SELECT\n" +
                "    p.ProductID,\n" +
                "    p.ProductDescription,\n" +
                "    (\n" +
                "       SELECT GROUP_CONCAT(DISTINCT WAREHOUSE ORDER BY WAREHOUSE)\n" +
                "       FROM prod_warehouse_link l\n" +
                "       WHERE l.PRODUCT = p.ProductID\n" +
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

            if (!stockFilter.isBlank()){
                String stock = product.getTotalStock().toString();
                if (!stock.contains(stockFilter)){ return false; }
            }

            if (!warehouseFilter.isBlank()){
                String warehouse = product.getWarehouses().toString();
                if (!warehouse.contains(warehouseFilter)){ return false; }
            }

            return true;
        });
    }
}







