package com.thesisv2;

import com.sun.jdi.FloatValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.Connection;


public abstract class Controller implements Initializable {

    //setting up the columns
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
    private  TableColumn<ProductListPopulator, Integer> CollumnStock;
    @FXML
    private  TableColumn<ProductListPopulator, Integer> ColumnPallets;
    @FXML
    private  TableColumn<ProductListPopulator, Integer> ColumnOutofpallet;
    @FXML
    private TableColumn<ProductListPopulator, Integer> ColumnPalletsize;

    ObservableList<ProductListPopulator> ProductListPopulatorObservableList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourcebundle){
        DBConnection connect = new DBConnection();
        Connection connection = connect.getConnection();

        //setting up a var. with the query
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

            //reads every line from the DB
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

                //Populating the observable list
                ProductListPopulatorObservableList.add(new ProductListPopulator(queryProductID, queryProductDescription, queryWarehouses,
                        queryPurchasedPrice, querySellPrice, queryWholesalePrice, queryTotalStock,
                        queryTotalPallets, queryOutOfPallet, queryPalletSize));
            }

            //PropertyValueFactory corresponds to the new ProducListPopulator fields
            ColumnProductID.setCellValueFactory(new PropertyValueFactory<>("ProductID"));
            ColumnProductDescription.setCellValueFactory(new PropertyValueFactory<>("ProductDescription"));
            ColumnWarehouses.setCellValueFactory(new PropertyValueFactory<>("Warehouses"));
            ColumnPurchasePrice.setCellValueFactory(new PropertyValueFactory<>("ProductPrice"));
            ColumnSellPrice.setCellValueFactory(new PropertyValueFactory<>("SellPrice"));
            ColumnWholesalePrice.setCellValueFactory(new PropertyValueFactory<>("PWholesalePrice"));
            CollumnStock.setCellValueFactory(new PropertyValueFactory<>("TotalStock"));
            ColumnPallets.setCellValueFactory(new PropertyValueFactory<>("TotalPallets"));
            ColumnOutofpallet.setCellValueFactory(new PropertyValueFactory<>("OutOfPallet"));
            ColumnPalletsize.setCellValueFactory(new PropertyValueFactory<>("PalletSize"));

            ProductTableView.setItems(ProductListPopulatorObservableList);

        } catch (SQLException e) {
            System.out.println("error!!!!!!!!!!!!");
            Logger.getLogger(ProductListPopulator.class.getName()).log(Level.SEVERE, null, e);
            e.printStackTrace();
        }


    }

}






