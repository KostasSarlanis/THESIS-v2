package com.thesisv2;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.Normalizer;
import java.util.ResourceBundle;

public class WarehouseListController implements Initializable {

    @FXML private TableView<WarehouseListPopulator> WarehouseTableView;
    @FXML private TableColumn<WarehouseListPopulator, Integer> ColumnWarehouseID;
    @FXML private TableColumn<WarehouseListPopulator, String> ColumnAddress;
    @FXML private TableColumn<WarehouseListPopulator, String> ColumnCity;
    @FXML private TableColumn<WarehouseListPopulator, String> ColumnCountry;
    @FXML private TextField SearchWarehouseID;
    @FXML private TextField SearchAddress;
    @FXML private TextField SearchCity;
    @FXML private TextField SearchCountry;
    @FXML private Label ResultLabel;
    @FXML private SplitPane SplitPaneControll;

    ObservableList<WarehouseListPopulator> warehouseObservableList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        //~~~~making the divider in splitpane dynamic~~~~~
        SplitPane.Divider divider = SplitPaneControll.getDividers().get(0);
        divider.positionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() != 0.12) divider.setPosition(0.12);
        });

        setupColumns();
        loadWarehouses();
        setupFiltering();
        setupContextMenuAndDoubleClick();
    }

    private void setupColumns() {
        ColumnWarehouseID.setCellValueFactory(new PropertyValueFactory<>("WarehouseID"));
        ColumnAddress.setCellValueFactory(new PropertyValueFactory<>("Address"));
        ColumnCity.setCellValueFactory(new PropertyValueFactory<>("City"));
        ColumnCountry.setCellValueFactory(new PropertyValueFactory<>("Country"));
    }

    private void loadWarehouses() {
        warehouseObservableList.clear();

        String sql = "SELECT WarehouseID, Address, City, Country FROM warehouses ORDER BY WarehouseID";

        try {
            DBConnection connect = new DBConnection();
            Connection connection = connect.getConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                warehouseObservableList.add(new WarehouseListPopulator(
                        rs.getInt("WarehouseID"),
                        rs.getString("Address"),
                        rs.getString("City"),
                        rs.getString("Country")
                ));
            }

            WarehouseTableView.setItems(warehouseObservableList);

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Σφάλμα φόρτωσης αποθηκών: " + e.getMessage()).showAndWait();
        }
    }

    private void setupFiltering() {
        javafx.collections.transformation.FilteredList<WarehouseListPopulator> filteredData =
                new javafx.collections.transformation.FilteredList<>(warehouseObservableList, b -> true);

        SearchWarehouseID.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchAddress.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchCity.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchCountry.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));

        javafx.collections.transformation.SortedList<WarehouseListPopulator> sortedData =
                new javafx.collections.transformation.SortedList<>(filteredData);

        sortedData.comparatorProperty().bind(WarehouseTableView.comparatorProperty());
        WarehouseTableView.setItems(sortedData);

        ResultLabel.textProperty().bind(
                Bindings.size(sortedData).asString("Αποτελέσματα: %d")
        );
    }

    private void applyFilters(javafx.collections.transformation.FilteredList<WarehouseListPopulator> filteredData) {
        String warehouseIdFilter = normalizeGreek(SearchWarehouseID.getText());
        String addressFilter = normalizeGreek(SearchAddress.getText());
        String cityFilter = normalizeGreek(SearchCity.getText());
        String countryFilter = normalizeGreek(SearchCountry.getText());

        filteredData.setPredicate(warehouse -> {
            if (!warehouseIdFilter.isBlank()) {
                if (!String.valueOf(warehouse.getWarehouseID()).contains(warehouseIdFilter)) {
                    return false;
                }
            }

            if (!addressFilter.isBlank()) {
                String address = normalizeGreek(warehouse.getAddress());
                if (!address.contains(addressFilter)) {
                    return false;
                }
            }

            if (!cityFilter.isBlank()) {
                String city = normalizeGreek(warehouse.getCity());
                if (!city.contains(cityFilter)) {
                    return false;
                }
            }

            if (!countryFilter.isBlank()) {
                String country = normalizeGreek(warehouse.getCountry());
                if (!country.contains(countryFilter)) {
                    return false;
                }
            }

            return true;
        });
    }

    private String normalizeGreek(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();

        normalized = normalized.replace('ς', 'σ');

        return normalized;
    }

    private void setupContextMenuAndDoubleClick() {
        WarehouseTableView.setRowFactory(tv -> {
            TableRow<WarehouseListPopulator> row = new TableRow<>();

            MenuItem openItem = new MenuItem("Άνοιγμα");
            openItem.setOnAction(event -> {
                WarehouseListPopulator selectedWarehouse = row.getItem();
                if (selectedWarehouse != null) {
                    openWarehouseProducts(selectedWarehouse);
                }
            });

            ContextMenu contextMenu = new ContextMenu(openItem);

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()
                        && event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2) {
                    openWarehouseProducts(row.getItem());
                }
            });

            return row;
        });
    }

    private void openWarehouseProducts(WarehouseListPopulator warehouse) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("product-list-view.fxml"));
            Parent root = loader.load();

            ProductListController controller = loader.getController();
            controller.openWithWarehouseFilter(String.valueOf(warehouse.getWarehouseID()));

            Stage stage = new Stage();
            stage.setTitle("Προϊόντα αποθήκης " + warehouse.getWarehouseID());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Σφάλμα ανοίγματος λίστας προϊόντων: " + e.getMessage()).showAndWait();
        }
    }
}