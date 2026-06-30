package com.thesisv2;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.net.URL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class Controller {
    //~~~~~~ MAKE IT FASTER ~~~~~
    @FXML private  TabPane MainTabPane;
    @FXML private Tab WarehousesTab;
    @FXML private Tab CreateInvoiceTab;
    @FXML private Tab MovementTab;
    @FXML private Tab MovementListTab;
    @FXML private Tab InvoiceListTab;

    @FXML
    private void initialize() {
        MainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {

            if (newTab == WarehousesTab) {
                loadTabOnce(WarehousesTab, "Warehouse-list-view.fxml");
            }

            else if (newTab == CreateInvoiceTab) {
                loadTabOnce(CreateInvoiceTab, "create-invoice-view.fxml");
            }

            else if (newTab == MovementTab) {
                loadTabOnce(MovementTab, "inner-movement-view.fxml");
            }

            else if (newTab == MovementListTab) {
                loadTabOnce(MovementListTab, "movment-list-view.fxml");
            }

            else if (newTab == InvoiceListTab) {
                loadTabOnce(InvoiceListTab, "invoice-list-view.fxml");
            }
        });
    }

    private void loadTabOnce(Tab tab, String fxmlFile) {
        if (tab.getContent() != null) {
            return;
        }

        try {
            URL resource = getClass().getResource(fxmlFile);

            if (resource == null) {
                tab.setContent(new Label("Δεν βρέθηκε το αρχείο: " + fxmlFile));
                return;
            }

            Parent content = FXMLLoader.load(resource);
            tab.setContent(content);

        } catch (Exception e) {
            e.printStackTrace();
            tab.setContent(new Label("Σφάλμα φόρτωσης: " + fxmlFile));
        }
    }

    //~~~~~ CLOSE HANDLER ~~~~~
    @FXML
    private void HandleCloseButton(ActionEvent event) {
        Stage stage = (Stage) ((MenuItem) event.getSource()).getParentPopup().getOwnerWindow();
        stage.close();
    }

    //~~~~~ DATABASE SETTINGS HANDLER ~~~~~
    @FXML
    private void HandleDatabaseSettings(ActionEvent event) {
        try {
            boolean saved = AppConfig.showDatabaseSettingsDialog();

            if (saved) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Ρυθμίσεις βάσης");
                alert.setHeaderText(null);
                alert.setContentText("Οι ρυθμίσεις αποθηκεύτηκαν επιτυχώς.");
                alert.showAndWait();
            }

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Σφάλμα");
            alert.setHeaderText("Αποτυχία ανοίγματος ρυθμίσεων βάσης");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    //~~~~~ COMPANY SETTINGS HANDLER ~~~~~
    @FXML
    private void HandleCompanySettings(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Στοιχεία εταιρείας");
        dialog.setHeaderText("Επεξεργασία στοιχείων εκδότη");
        dialog.setResizable(true);

        ButtonType saveBtn = new ButtonType("Αποθήκευση", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField sellerNameField = new TextField();
        TextField sellerAddressField = new TextField();
        TextField sellerCityField = new TextField();
        TextField sellerPostalCodeField = new TextField();
        TextField sellerCountryField = new TextField();
        TextField sellerTaxIdField = new TextField();
        TextField sellerEmailField = new TextField();
        TextField sellerPhoneField = new TextField();

        loadCompanyInfoIntoFields(
                sellerNameField,
                sellerAddressField,
                sellerCityField,
                sellerPostalCodeField,
                sellerCountryField,
                sellerTaxIdField,
                sellerEmailField,
                sellerPhoneField
        );

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        grid.setAlignment(Pos.CENTER_LEFT);

        int r = 0;
        grid.add(new Label("Επωνυμία:"), 0, r);
        grid.add(sellerNameField, 1, r++);

        grid.add(new Label("Διεύθυνση:"), 0, r);
        grid.add(sellerAddressField, 1, r++);

        grid.add(new Label("Πόλη:"), 0, r);
        grid.add(sellerCityField, 1, r++);

        grid.add(new Label("Τ.Κ.:"), 0, r);
        grid.add(sellerPostalCodeField, 1, r++);

        grid.add(new Label("Χώρα:"), 0, r);
        grid.add(sellerCountryField, 1, r++);

        grid.add(new Label("ΑΦΜ:"), 0, r);
        grid.add(sellerTaxIdField, 1, r++);

        grid.add(new Label("Email:"), 0, r);
        grid.add(sellerEmailField, 1, r++);

        grid.add(new Label("Τηλέφωνο:"), 0, r);
        grid.add(sellerPhoneField, 1, r++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefSize(450, 420);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveBtn) {
            return;
        }

        if (sellerNameField.getText().trim().isBlank()) {
            showWarning("Έλεγχος", "Η επωνυμία είναι υποχρεωτική.");
            return;
        }

        saveCompanyInfo(
                sellerNameField.getText().trim(),
                sellerAddressField.getText().trim(),
                sellerCityField.getText().trim(),
                sellerPostalCodeField.getText().trim(),
                sellerCountryField.getText().trim(),
                sellerTaxIdField.getText().trim(),
                sellerEmailField.getText().trim(),
                sellerPhoneField.getText().trim()
        );

        showInfo("Στοιχεία εταιρείας", "Τα στοιχεία εταιρείας αποθηκεύτηκαν επιτυχώς.");
    }

    private void loadCompanyInfoIntoFields(
            TextField sellerNameField,
            TextField sellerAddressField,
            TextField sellerCityField,
            TextField sellerPostalCodeField,
            TextField sellerCountryField,
            TextField sellerTaxIdField,
            TextField sellerEmailField,
            TextField sellerPhoneField
    ) {
        String sql = "SELECT * FROM company_info WHERE id = 1";

        try (Connection connection = new DBConnection().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                sellerNameField.setText(rs.getString("seller_name"));
                sellerAddressField.setText(rs.getString("seller_address"));
                sellerCityField.setText(rs.getString("seller_city"));
                sellerPostalCodeField.setText(rs.getString("seller_postal_code"));
                sellerCountryField.setText(rs.getString("seller_country"));
                sellerTaxIdField.setText(rs.getString("seller_tax_id"));
                sellerEmailField.setText(rs.getString("seller_email"));
                sellerPhoneField.setText(rs.getString("seller_phone"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Σφάλμα", "Δεν ήταν δυνατή η φόρτωση των στοιχείων εταιρείας.");
        }
    }

    private void saveCompanyInfo(
            String sellerName,
            String sellerAddress,
            String sellerCity,
            String sellerPostalCode,
            String sellerCountry,
            String sellerTaxId,
            String sellerEmail,
            String sellerPhone
    ) {
        String updateSql = """
            UPDATE company_info
            SET seller_name = ?,
                seller_address = ?,
                seller_city = ?,
                seller_postal_code = ?,
                seller_country = ?,
                seller_tax_id = ?,
                seller_email = ?,
                seller_phone = ?
            WHERE id = 1
            """;

        try (Connection connection = new DBConnection().getConnection();
             PreparedStatement stmt = connection.prepareStatement(updateSql)) {

            stmt.setString(1, sellerName);
            stmt.setString(2, sellerAddress);
            stmt.setString(3, sellerCity);
            stmt.setString(4, sellerPostalCode);
            stmt.setString(5, sellerCountry);
            stmt.setString(6, sellerTaxId);
            stmt.setString(7, sellerEmail);
            stmt.setString(8, sellerPhone);

            int affected = stmt.executeUpdate();

            if (affected == 0) {
                throw new RuntimeException("Δεν βρέθηκε εγγραφή εταιρείας με id = 1.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Αποτυχία αποθήκευσης στοιχείων εταιρείας.", e);
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}