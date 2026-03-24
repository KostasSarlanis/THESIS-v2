package com.thesisv2;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

public class CreateInvoiceController implements Initializable {

    @FXML private SplitPane SplitPaneControll;

    @FXML private TextField InvoiceIdField;
    @FXML private ComboBox<String> InvoiceTypeCombo;
    @FXML private ComboBox<String> InvoiceStatusCombo;
    @FXML private DatePicker IssueDatePicker;
    @FXML private DatePicker DueDatePicker;
    @FXML private ComboBox<String> CurrencyCombo;
    @FXML private ComboBox<String> LanguageCombo;

    @FXML private TextField SellerNameField;
    @FXML private TextField SellerAddressField;
    @FXML private TextField SellerCityField;
    @FXML private TextField SellerPostalCodeField;
    @FXML private TextField SellerCountryField;
    @FXML private TextField SellerTaxIdField;
    @FXML private TextField SellerEmailField;
    @FXML private TextField SellerPhoneField;

    @FXML private TextField CustomerNameField;
    @FXML private TextField CustomerAddressField;
    @FXML private TextField CustomerCityField;
    @FXML private TextField CustomerPostalCodeField;
    @FXML private TextField CustomerCountryField;
    @FXML private TextField CustomerTaxIdField;
    @FXML private TextField CustomerEmailField;
    @FXML private TextField CustomerPhoneField;

    @FXML private TextArea NotesArea;
    @FXML private TextArea PaymentTermsArea;

    @FXML private Label HeaderInfoLabel;

    @FXML private TableView<InvoiceLineModel> InvoiceLinesTable;
    @FXML private TableColumn<InvoiceLineModel, Integer> ColLineNo;
    @FXML private TableColumn<InvoiceLineModel, String> ColItemCode;
    @FXML private TableColumn<InvoiceLineModel, String> ColDescription;
    @FXML private TableColumn<InvoiceLineModel, Integer> ColQuantity;
    @FXML private TableColumn<InvoiceLineModel, String> ColUnit;
    @FXML private TableColumn<InvoiceLineModel, Double> ColUnitPrice;
    @FXML private TableColumn<InvoiceLineModel, Double> ColDiscount;
    @FXML private TableColumn<InvoiceLineModel, Double> ColTax;
    @FXML private TableColumn<InvoiceLineModel, Double> ColLineTotal;

    @FXML private TextField SubtotalField;
    @FXML private TextField DiscountTotalField;
    @FXML private TextField TaxTotalField;
    @FXML private TextField GrandTotalField;
    @FXML private TextField OverallDiscountPercentField;
    @FXML private ComboBox<Integer> WarehouseCombo;

    private final ObservableList<InvoiceLineModel> invoiceLines = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        SplitPane.Divider divider = SplitPaneControll.getDividers().get(0);
        divider.positionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() != 0.33) {
                divider.setPosition(0.33);
            }
        });

        setupCombos();
        loadWarehouses();
        setupTable();
        setDefaults();
        setFixedSellerDetails();
        OverallDiscountPercentField.textProperty().addListener((obs, oldVal, newVal) -> recalculateTotals());
        updateHeaderInfo();
    }

    private void setupCombos() {
        InvoiceTypeCombo.setItems(FXCollections.observableArrayList(
                "INVOICE", "PROFORMA", "CREDIT_NOTE", "DELIVERY_NOTE", "RECEIPT"
        ));
        InvoiceStatusCombo.setItems(FXCollections.observableArrayList(
                "DRAFT", "FINAL", "PAID", "CANCELLED"
        ));
        CurrencyCombo.setItems(FXCollections.observableArrayList(
                "EUR", "USD", "GBP"
        ));
        LanguageCombo.setItems(FXCollections.observableArrayList(
                "el-GR", "en-US"
        ));
    }

    private void setDefaults() {
        InvoiceIdField.clear();
        InvoiceTypeCombo.setValue("INVOICE");
        InvoiceStatusCombo.setValue("DRAFT");
        IssueDatePicker.setValue(LocalDate.now());
        DueDatePicker.setValue(LocalDate.now().plusDays(30));
        CurrencyCombo.setValue("EUR");
        LanguageCombo.setValue("el-GR");
        OverallDiscountPercentField.setText("0");

        if (WarehouseCombo != null && !WarehouseCombo.getItems().isEmpty()) {
            WarehouseCombo.setValue(WarehouseCombo.getItems().get(0));
        }

        invoiceLines.clear();
        ensureExtraEmptyLine();
        recalculateTotals();
    }

    private void setupTable() {
        InvoiceLinesTable.setEditable(true);

        ColLineNo.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getLineNo()));
        ColItemCode.setCellValueFactory(cell -> cell.getValue().itemCodeProperty());
        ColDescription.setCellValueFactory(cell -> cell.getValue().descriptionProperty());
        ColQuantity.setCellValueFactory(cell -> cell.getValue().quantityProperty().asObject());
        ColUnit.setCellValueFactory(cell -> cell.getValue().unitNameProperty());
        ColUnitPrice.setCellValueFactory(cell -> cell.getValue().unitPriceProperty().asObject());
        ColDiscount.setCellValueFactory(cell -> cell.getValue().discountPercentProperty().asObject());
        ColTax.setCellValueFactory(cell -> cell.getValue().taxPercentProperty().asObject());
        ColLineTotal.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(round2(cell.getValue().getLineTotal())));

        ColLineNo.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        ColItemCode.setCellFactory(TextFieldTableCell.forTableColumn());
        ColDescription.setCellFactory(TextFieldTableCell.forTableColumn());
        ColUnit.setCellFactory(TextFieldTableCell.forTableColumn());
        javafx.util.StringConverter<Double> flexibleDoubleConverter = createFlexibleDoubleConverter();
        ColQuantity.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        ColUnitPrice.setCellFactory(TextFieldTableCell.forTableColumn(flexibleDoubleConverter));
        ColDiscount.setCellFactory(TextFieldTableCell.forTableColumn(flexibleDoubleConverter));
        ColTax.setCellFactory(TextFieldTableCell.forTableColumn(flexibleDoubleConverter));
        ColLineNo.setOnEditCommit(event -> event.getRowValue().setLineNo(event.getNewValue()));

        ColItemCode.setOnEditCommit(event -> {
            InvoiceLineModel line = event.getRowValue();
            String newCode = event.getNewValue() == null ? "" : event.getNewValue().trim();

            line.setItemCode(newCode);

            if (newCode.isBlank()) {
                ensureExtraEmptyLine();
                refreshTableAndTotals();
                return;
            }

            // Custom manual line
            if (newCode.equals("0")) {
                line.setItemCode("0");

                if (line.getUnitName() == null || line.getUnitName().isBlank()) {
                    line.setUnitName("Τεμ");
                }

                ensureExtraEmptyLine();
                refreshTableAndTotals();
                return;
            }

            ProductLookupResult product = findProductByCode(newCode);

            if (product != null) {
                line.setItemCode(product.getCode());
                line.setDescription(product.getDescription());
                line.setUnitPrice(product.getSellPrice());

                if (line.getUnitName() == null || line.getUnitName().isBlank()) {
                    line.setUnitName("Τεμ");
                }

                line.recalculateLineTotal();
                ensureExtraEmptyLine();
                refreshTableAndTotals();
            } else {
                showWarning("Προϊόν", "Δεν βρέθηκε προϊόν με κωδικό " + newCode + ".");
                ensureExtraEmptyLine();
                refreshTableAndTotals();
            }
        });

        ColDescription.setOnEditCommit(event -> {
            event.getRowValue().setDescription(event.getNewValue());
            event.getRowValue().recalculateLineTotal();
            ensureExtraEmptyLine();
            refreshTableAndTotals();
        });

        ColQuantity.setOnEditCommit(event -> {
            Integer newValue = event.getNewValue();
            event.getRowValue().setQuantity(newValue == null ? 0 : newValue);
            event.getRowValue().recalculateLineTotal();
            ensureExtraEmptyLine();
            refreshTableAndTotals();
        });

        ColUnit.setOnEditCommit(event -> event.getRowValue().setUnitName(event.getNewValue()));
        ColUnitPrice.setOnEditCommit(event -> {
            event.getRowValue().setUnitPrice(safeDouble(event.getNewValue()));
            event.getRowValue().recalculateLineTotal();
            ensureExtraEmptyLine();
            refreshTableAndTotals();
        });
        ColDiscount.setOnEditCommit(event -> {
            event.getRowValue().setDiscountPercent(safeDouble(event.getNewValue()));
            event.getRowValue().recalculateLineTotal();
            ensureExtraEmptyLine();
            refreshTableAndTotals();
        });
        ColTax.setOnEditCommit(event -> {
            event.getRowValue().setTaxPercent(safeDouble(event.getNewValue()));
            event.getRowValue().recalculateLineTotal();
            ensureExtraEmptyLine();
            refreshTableAndTotals();
        });

        InvoiceLinesTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(InvoiceLineModel item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("");
                } else if (item.isInsufficientStock()) {
                    setStyle("-fx-background-color: #ffdddd; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });

        InvoiceLinesTable.setItems(invoiceLines);

    }

    private Double parseFlexibleDouble(String text) {
        if (text == null || text.trim().isBlank()) {
            return 0.0;
        }

        String normalized = text.trim().replace(",", ".");
        return Double.parseDouble(normalized);
    }

    private javafx.util.StringConverter<Double> createFlexibleDoubleConverter() {
        return new javafx.util.StringConverter<>() {
            @Override
            public String toString(Double value) {
                if (value == null) {
                    return "";
                }
                if (value == Math.floor(value)) {
                    return String.format("%.0f", value);
                }
                return String.valueOf(value);
            }

            @Override
            public Double fromString(String text) {
                return parseFlexibleDouble(text);
            }
        };
    }

    @FXML
    private void handleAddLine(javafx.event.ActionEvent event) {
        int nextLineNo = invoiceLines.size() + 1;
        invoiceLines.add(new InvoiceLineModel(nextLineNo, "", "", 1, "τεμ", 0.0, 0.0, 24.0));
        ensureExtraEmptyLine();
        refreshTableAndTotals();
    }

    @FXML
    private void handleRemoveSelectedLine(javafx.event.ActionEvent event) {
        InvoiceLineModel selected = InvoiceLinesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Επιλογή γραμμής", "Επίλεξε πρώτα μια γραμμή για διαγραφή.");
            return;
        }

        invoiceLines.remove(selected);
        resequenceLines();
        ensureExtraEmptyLine();
        refreshTableAndTotals();
    }

    @FXML
    private void handleClearAll(javafx.event.ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Καθαρισμός");
        alert.setHeaderText("Καθαρισμός όλων των πεδίων");
        alert.setContentText("Θέλεις να διαγραφούν όλα τα πεδία και οι γραμμές;");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            clearFormFields();
            setDefaults();
            setFixedSellerDetails();
        }
    }

    @FXML
    private void handleSaveInvoice(javafx.event.ActionEvent event) {
        if (!validateBeforeSave()) {
            return;
        }

        Connection connection = null;
        try {
            DBConnection connect = new DBConnection();
            connection = connect.getConnection();
            connection.setAutoCommit(false);

            if (!validateStockForSelectedWarehouse(connection)){
                connection.rollback();
                return;
            }

            String insertHeaderSql = """
            INSERT INTO invoice_header (
                invoice_type, invoice_status, issue_date, due_date, currency_code, language_code,
                seller_name, seller_address, seller_city, seller_postal_code, seller_country, seller_tax_id, seller_email, seller_phone,
                customer_name, customer_address, customer_city, customer_postal_code, customer_country, customer_tax_id, customer_email, customer_phone,
                subtotal, overall_discount_percent, discount_total, tax_total, grand_total, notes, payment_terms, source_warehouse
            ) VALUES (?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?, ?,
                      ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement headerStmt = connection.prepareStatement(insertHeaderSql, Statement.RETURN_GENERATED_KEYS);

            headerStmt.setString(1, InvoiceTypeCombo.getValue());
            headerStmt.setString(2, InvoiceStatusCombo.getValue());
            headerStmt.setDate(3, Date.valueOf(IssueDatePicker.getValue()));

            if (DueDatePicker.getValue() != null) {
                headerStmt.setDate(4, Date.valueOf(DueDatePicker.getValue()));
            } else {
                headerStmt.setNull(4, Types.DATE);
            }

            headerStmt.setString(5, CurrencyCombo.getValue());
            headerStmt.setString(6, LanguageCombo.getValue());

            headerStmt.setString(7, SellerNameField.getText());
            headerStmt.setString(8, SellerAddressField.getText());
            headerStmt.setString(9, SellerCityField.getText());
            headerStmt.setString(10, SellerPostalCodeField.getText());
            headerStmt.setString(11, SellerCountryField.getText());
            headerStmt.setString(12, SellerTaxIdField.getText());
            headerStmt.setString(13, SellerEmailField.getText());
            headerStmt.setString(14, SellerPhoneField.getText());

            headerStmt.setString(15, CustomerNameField.getText());
            headerStmt.setString(16, CustomerAddressField.getText());
            headerStmt.setString(17, CustomerCityField.getText());
            headerStmt.setString(18, CustomerPostalCodeField.getText());
            headerStmt.setString(19, CustomerCountryField.getText());
            headerStmt.setString(20, CustomerTaxIdField.getText());
            headerStmt.setString(21, CustomerEmailField.getText());
            headerStmt.setString(22, CustomerPhoneField.getText());

            headerStmt.setBigDecimal(23, bd(SubtotalField.getText()));
            headerStmt.setBigDecimal(24, bd(getOverallDiscountPercent()));
            headerStmt.setBigDecimal(25, bd(DiscountTotalField.getText()));
            headerStmt.setBigDecimal(26, bd(TaxTotalField.getText()));
            headerStmt.setBigDecimal(27, bd(GrandTotalField.getText()));
            headerStmt.setString(28, NotesArea.getText());
            headerStmt.setString(29, PaymentTermsArea.getText());
            headerStmt.setString(30, String.valueOf(WarehouseCombo.getValue()));

            headerStmt.executeUpdate();

            int invoiceId;
            ResultSet rs = headerStmt.getGeneratedKeys();
            if (rs.next()) {
                invoiceId = rs.getInt(1);
            } else {
                throw new SQLException("Δεν επιστράφηκε invoice_id.");
            }

            String insertLineSql = """
                    INSERT INTO invoice_line (
                        invoice_id, line_no, item_code, description, quantity, unit_name,
                        unit_price, discount_percent, tax_percent, line_total
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            PreparedStatement lineStmt = connection.prepareStatement(insertLineSql);

            int actualLineNo = 1;
            int selectedWarehouse = WarehouseCombo.getValue();

            for (InvoiceLineModel line : invoiceLines) {
                if (isLineEmpty(line)) {
                    continue;
                }

                line.recalculateLineTotal();

                lineStmt.setInt(1, invoiceId);
                lineStmt.setInt(2, actualLineNo++);
                lineStmt.setString(3, line.getItemCode());
                lineStmt.setString(4, line.getDescription());
                lineStmt.setInt(5, line.getQuantity());
                lineStmt.setString(6, line.getUnitName());
                lineStmt.setBigDecimal(7, bd(line.getUnitPrice()));
                lineStmt.setBigDecimal(8, bd(line.getDiscountPercent()));
                lineStmt.setBigDecimal(9, bd(line.getTaxPercent()));
                lineStmt.setBigDecimal(10, bd(line.getLineTotal()));
                lineStmt.addBatch();

                if (line.getItemCode() != null
                        && !line.getItemCode().isBlank()
                        && !"0".equals(line.getItemCode())) {
                    reduceStock(connection, line.getItemCode(), selectedWarehouse, line.getQuantity());
                }
            }

            lineStmt.executeBatch();
            connection.commit();

            InvoiceIdField.setText(formatInvoiceId(invoiceId));
            showInfo("Αποθήκευση", "Το παραστατικό αποθηκεύτηκε με αριθμό " + formatInvoiceId(invoiceId) + ".");

        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            showError("Σφάλμα αποθήκευσης", e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handlePickProduct(javafx.event.ActionEvent event) {
        InvoiceLineModel selectedLine = InvoiceLinesTable.getSelectionModel().getSelectedItem();
        if (selectedLine == null) {
            showWarning("Επιλογή γραμμής", "Επίλεξε πρώτα μια γραμμή τιμολογίου.");
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("product-list-view.fxml"));
            javafx.scene.Parent root = loader.load();

            ProductListController controller = loader.getController();
            controller.setSelectionMode(true);
            controller.setOnProductSelected(product -> {
                selectedLine.setItemCode(String.valueOf(product.getProductID()));
                selectedLine.setDescription(product.getProductDescription());
                selectedLine.setUnitPrice(product.getSellPrice());
                selectedLine.setUnitName("τεμ");
                selectedLine.setQuantity(selectedLine.getQuantity() <= 0 ? 1 : selectedLine.getQuantity());
                selectedLine.recalculateLineTotal();

                ensureExtraEmptyLine();
                refreshTableAndTotals();
            });

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Επιλογή προϊόντος");
            stage.setScene(new javafx.scene.Scene(root, 1200, 700));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Σφάλμα", "Δεν ήταν δυνατό το άνοιγμα της λίστας προϊόντων.");
        }
    }

    @FXML
    private void handleSavePdf(javafx.event.ActionEvent event) {
        if (!validateBeforeExport()) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Αποθήκευση PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        String invoiceNumber = InvoiceIdField.getText().isBlank() ? "draft" : InvoiceIdField.getText();
        fileChooser.setInitialFileName("invoice_" + invoiceNumber + ".pdf");

        File file = fileChooser.showSaveDialog(InvoiceLinesTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            PdfInvoiceService pdfService = new PdfInvoiceService();
            pdfService.exportInvoiceToPdf(
                    file,
                    InvoiceIdField.getText().isBlank() ? "000000" : InvoiceIdField.getText(),
                    InvoiceTypeCombo.getValue(),
                    InvoiceStatusCombo.getValue(),
                    IssueDatePicker.getValue(),
                    DueDatePicker.getValue(),
                    CurrencyCombo.getValue(),
                    WarehouseCombo.getValue(),

                    SellerNameField.getText(), SellerAddressField.getText(), SellerCityField.getText(),
                    SellerPostalCodeField.getText(), SellerCountryField.getText(), SellerTaxIdField.getText(),
                    SellerEmailField.getText(), SellerPhoneField.getText(),

                    CustomerNameField.getText(), CustomerAddressField.getText(), CustomerCityField.getText(),
                    CustomerPostalCodeField.getText(), CustomerCountryField.getText(), CustomerTaxIdField.getText(),
                    CustomerEmailField.getText(), CustomerPhoneField.getText(),

                    invoiceLines,
                    SubtotalField.getText(),
                    OverallDiscountPercentField.getText(),
                    DiscountTotalField.getText(),
                    TaxTotalField.getText(),
                    GrandTotalField.getText(),
                    NotesArea.getText(),
                    PaymentTermsArea.getText()
            );

            showInfo("PDF", "Το PDF αποθηκεύτηκε επιτυχώς.");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Σφάλμα PDF", e.getMessage());
        }
    }

    @FXML
    private void handlePrintInvoice(javafx.event.ActionEvent event) {
        if (!validateBeforeExport()) {
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showError("Εκτύπωση", "Δεν ήταν δυνατή η δημιουργία εργασίας εκτύπωσης.");
            return;
        }

        boolean proceed = job.showPrintDialog(InvoiceLinesTable.getScene().getWindow());
        if (!proceed) {
            return;
        }

        Node printableNode = createPrintableNode();

        boolean printed = job.printPage(printableNode);
        if (printed) {
            job.endJob();
            showInfo("Εκτύπωση", "Η εκτύπωση στάλθηκε στον εκτυπωτή.");
        } else {
            showError("Εκτύπωση", "Η εκτύπωση απέτυχε.");
        }
    }

    private Node createPrintableNode() {
        VBox root = new VBox(12);
        root.setStyle("-fx-background-color: white; -fx-padding: 24;");

        Label title = new Label("ΠΑΡΑΣΤΑΤΙΚΟ " + InvoiceTypeCombo.getValue());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        GridPane topGrid = new GridPane();
        topGrid.setHgap(24);
        topGrid.setVgap(6);

        VBox sellerBox = new VBox(4);
        sellerBox.getChildren().addAll(
                sectionLabel("Εκδότης"),
                valueLabel(SellerNameField.getText()),
                valueLabel(SellerAddressField.getText()),
                valueLabel(SellerCityField.getText() + " " + SellerPostalCodeField.getText()),
                valueLabel(SellerCountryField.getText()),
                valueLabel("ΑΦΜ: " + SellerTaxIdField.getText()),
                valueLabel("Τηλ: " + SellerPhoneField.getText()),
                valueLabel("Email: " + SellerEmailField.getText())
        );

        VBox buyerBox = new VBox(4);
        buyerBox.getChildren().addAll(
                sectionLabel("Πελάτης"),
                valueLabel(CustomerNameField.getText()),
                valueLabel(CustomerAddressField.getText()),
                valueLabel(CustomerCityField.getText() + " " + CustomerPostalCodeField.getText()),
                valueLabel(CustomerCountryField.getText()),
                valueLabel("ΑΦΜ: " + CustomerTaxIdField.getText()),
                valueLabel("Τηλ: " + CustomerPhoneField.getText()),
                valueLabel("Email: " + CustomerEmailField.getText())
        );

        VBox infoBox = new VBox(4);
        infoBox.getChildren().addAll(
                sectionLabel("Στοιχεία Παραστατικού"),
                valueLabel("Αριθμός: " + (InvoiceIdField.getText().isBlank() ? "000000" : InvoiceIdField.getText())),
                valueLabel("Ημερομηνία: " + IssueDatePicker.getValue()),
                valueLabel("Λήξη: " + DueDatePicker.getValue()),
                valueLabel("Αποθήκη: " + WarehouseCombo.getValue()),
                valueLabel("Νόμισμα: " + CurrencyCombo.getValue()),
                valueLabel("Κατάσταση: " + InvoiceStatusCombo.getValue())
        );

        topGrid.add(sellerBox, 0, 0);
        topGrid.add(buyerBox, 1, 0);
        topGrid.add(infoBox, 2, 0);

        GridPane linesGrid = new GridPane();
        linesGrid.setHgap(12);
        linesGrid.setVgap(6);

        Label h1 = headerLabel("Α/Α");
        Label h2 = headerLabel("Περιγραφή");
        Label h3 = headerLabel("Ποσ.");
        Label h4 = headerLabel("Τιμή");
        Label h5 = headerLabel("Σύνολο");

        linesGrid.add(h1, 0, 0);
        linesGrid.add(h2, 1, 0);
        linesGrid.add(h3, 2, 0);
        linesGrid.add(h4, 3, 0);
        linesGrid.add(h5, 4, 0);

        int row = 1;
        for (InvoiceLineModel line : invoiceLines) {
            if (isLineEmpty(line)) continue;

            linesGrid.add(valueLabel(String.valueOf(line.getLineNo())), 0, row);
            linesGrid.add(valueLabel(line.getDescription()), 1, row);
            linesGrid.add(valueLabel(formatNumber(line.getQuantity())), 2, row);
            linesGrid.add(valueLabel(formatMoney(line.getUnitPrice())), 3, row);
            linesGrid.add(valueLabel(formatMoney(line.getLineTotal())), 4, row);
            row++;
        }

        VBox totalsBox = new VBox(4);
        totalsBox.setStyle("-fx-alignment: center-right;");
        totalsBox.getChildren().addAll(
                valueLabel("Υποσύνολο: " + SubtotalField.getText() + " " + CurrencyCombo.getValue()),
                valueLabel("Έκπτωση %: " + OverallDiscountPercentField.getText()),
                valueLabel("Σύνολο έκπτωσης: " + DiscountTotalField.getText() + " " + CurrencyCombo.getValue()),
                valueLabel("Σύνολο φόρου: " + TaxTotalField.getText() + " " + CurrencyCombo.getValue()),
                boldValueLabel("Γενικό σύνολο: " + GrandTotalField.getText() + " " + CurrencyCombo.getValue())
        );

        root.getChildren().addAll(
                title,
                new Separator(),
                topGrid,
                new Separator(),
                linesGrid,
                new Separator(),
                totalsBox
        );

        return root;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        return label;
    }

    private Label headerLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        return label;
    }

    private Label valueLabel(String text) {
        Label label = new Label(text == null ? "" : text);
        label.setStyle("-fx-font-size: 11px;");
        return label;
    }

    private Label boldValueLabel(String text) {
        Label label = new Label(text == null ? "" : text);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        return label;
    }

    private boolean validateBeforeSave() {
        if (!validateWarehouseSelected()) {
            return false;
        }

        if (SellerNameField.getText().isBlank()) {
            showWarning("Έλεγχος", "Το πεδίο εκδότη είναι υποχρεωτικό.");
            return false;
        }
        if (CustomerNameField.getText().isBlank()) {
            showWarning("Έλεγχος", "Το πεδίο πελάτη είναι υποχρεωτικό.");
            return false;
        }
        if (IssueDatePicker.getValue() == null) {
            showWarning("Έλεγχος", "Η ημερομηνία έκδοσης είναι υποχρεωτική.");
            return false;
        }

        boolean hasAtLeastOneRealLine = false;

        for (InvoiceLineModel line : invoiceLines) {
            if (isLineEmpty(line)) {
                continue;
            }

            hasAtLeastOneRealLine = true;

            if (line.getDescription() == null || line.getDescription().isBlank()) {
                showWarning("Έλεγχος", "Κάθε γραμμή πρέπει να έχει περιγραφή.");
                return false;
            }
        }

        if (!hasAtLeastOneRealLine) {
            showWarning("Έλεγχος", "Πρέπει να υπάρχει τουλάχιστον μία γραμμή.");
            return false;
        }

        double overallDiscountPercent = getOverallDiscountPercent();
        if (overallDiscountPercent > 100) {
            showWarning("Ελεγχος", "Η συνολική έκπτωση δεν μπορεί να είναι πάνω απο 100%.");
            return false;
        }
        return true;
    }

    private boolean isLineEmpty(InvoiceLineModel line) {
        return (line.getItemCode() == null || line.getItemCode().isBlank())
                && (line.getDescription() == null || line.getDescription().isBlank())
                && line.getUnitPrice() == 0.0;
    }

    private boolean validateBeforeExport() {
        if (!validateWarehouseSelected()){
            return false;
        }
        boolean hasAtLeastOneRealLine = false;

        for (InvoiceLineModel line : invoiceLines) {
            if (!isLineEmpty(line)) {
                hasAtLeastOneRealLine = true;
                break;
            }
        }

        if (!hasAtLeastOneRealLine) {
            showWarning("Έλεγχος", "Δεν υπάρχουν γραμμές για εξαγωγή/εκτύπωση.");
            return false;
        }

        return true;
    }

    private void recalculateTotals() {
        double subtotal = 0.0;
        double lineDiscountTotal = 0.0;

        for (InvoiceLineModel line : invoiceLines) {
            if (isLineEmpty(line)) {
                continue;
            }

            double base = line.getQuantity() * line.getUnitPrice();
            double lineDiscount = base * (line.getDiscountPercent() / 100.0);

            subtotal += base;
            lineDiscountTotal += lineDiscount;
        }

        double overallDiscountPercent = 0.0;
        try {
            overallDiscountPercent = parseFlexibleDouble(OverallDiscountPercentField.getText());
            if (overallDiscountPercent < 0) {
                overallDiscountPercent = 0.0;
            }
        } catch (Exception e) {
            overallDiscountPercent = 0.0;
        }

        double afterLineDiscount = subtotal - lineDiscountTotal;
        double overallDiscountAmount = afterLineDiscount * (overallDiscountPercent / 100.0);
        double discountTotal = lineDiscountTotal + overallDiscountAmount;

        double taxableAmount = afterLineDiscount - overallDiscountAmount;
        double taxTotal = 0.0;

        for (InvoiceLineModel line : invoiceLines) {
            if (isLineEmpty(line)) {
                continue;
            }

            double base = line.getQuantity() * line.getUnitPrice();
            double lineDiscount = base * (line.getDiscountPercent() / 100.0);
            double lineAfterDiscount = base - lineDiscount;

            double proportionalOverallDiscount = 0.0;
            if (afterLineDiscount > 0) {
                proportionalOverallDiscount = (lineAfterDiscount / afterLineDiscount) * overallDiscountAmount;
            }

            double taxableLine = lineAfterDiscount - proportionalOverallDiscount;
            double tax = taxableLine * (line.getTaxPercent() / 100.0);

            taxTotal += tax;
        }

        double grandTotal = taxableAmount + taxTotal;

        SubtotalField.setText(formatMoney(subtotal));
        DiscountTotalField.setText(formatMoney(discountTotal));
        TaxTotalField.setText(formatMoney(taxTotal));
        GrandTotalField.setText(formatMoney(grandTotal));
    }

    private void refreshTableAndTotals() {
        InvoiceLinesTable.refresh();
        recalculateTotals();
        updateHeaderInfo();
    }

    private void updateHeaderInfo() {
        long realLines = invoiceLines.stream().filter(line -> !isLineEmpty(line)).count();
        HeaderInfoLabel.setText("Γραμμές: " + realLines);
    }

    private void resequenceLines() {
        for (int i = 0; i < invoiceLines.size(); i++) {
            invoiceLines.get(i).setLineNo(i + 1);
        }
    }

    private void clearFormFields() {
        InvoiceIdField.clear();

        CustomerNameField.clear();
        CustomerAddressField.clear();
        CustomerCityField.clear();
        CustomerPostalCodeField.clear();
        CustomerCountryField.clear();
        CustomerTaxIdField.clear();
        CustomerEmailField.clear();
        CustomerPhoneField.clear();

        NotesArea.clear();
        PaymentTermsArea.clear();

        invoiceLines.clear();

        SubtotalField.clear();
        DiscountTotalField.clear();
        OverallDiscountPercentField.clear();
        TaxTotalField.clear();
        GrandTotalField.clear();
    }

    private String formatInvoiceId(int invoiceId) {
        return String.format("%06d", invoiceId);
    }

    private String formatMoney(double value) {
        return String.format("%.2f", round2(value));
    }

    private String formatNumber(double value) {
        return String.format("%.3f", value);
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal bd(String text) {
        return new BigDecimal(text == null || text.isBlank() ? "0.00" : text).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
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

    private void setFixedSellerDetails() {
        SellerNameField.setText("SARLANIS S.A.");
        SellerAddressField.setText("My house");
        SellerCityField.setText("Chalkida");
        SellerPostalCodeField.setText("34132");
        SellerCountryField.setText("Greece");
        SellerTaxIdField.setText("6912345678");
        SellerEmailField.setText("info@sarlanisSA.gr");
        SellerPhoneField.setText("2221012345");
    }

    private static class ProductLookupResult {
        private final String code;
        private final String description;
        private final double sellPrice;

        public ProductLookupResult(String code, String description, double sellPrice) {
            this.code = code;
            this.description = description;
            this.sellPrice = sellPrice;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public double getSellPrice() {
            return sellPrice;
        }
    }

    private ProductLookupResult findProductByCode(String productCode) {
        String sql = """
            SELECT ProductID, ProductDescription, SellPrice
            FROM products
            WHERE ProductID = ?
            """;

        try {
            DBConnection connect = new DBConnection();
            Connection connection = connect.getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql);

            stmt.setString(1, productCode);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new ProductLookupResult(
                        rs.getString("ProductID"),
                        rs.getString("ProductDescription"),
                        rs.getDouble("SellPrice")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void ensureExtraEmptyLine() {
        if (invoiceLines.isEmpty()) {
            invoiceLines.add(new InvoiceLineModel(1, "", "", 1, "τεμ", 0.0, 0.0, 24.0));
            return;
        }

        InvoiceLineModel last = invoiceLines.get(invoiceLines.size() - 1);

        boolean lastIsEmpty =
                (last.getItemCode() == null || last.getItemCode().isBlank()) &&
                        (last.getDescription() == null || last.getDescription().isBlank()) &&
                        last.getUnitPrice() == 0.0;

        if (!lastIsEmpty) {
            invoiceLines.add(new InvoiceLineModel(invoiceLines.size() + 1, "", "", 1, "τεμ", 0.0, 0.0, 24.0));
        }

        resequenceLines();
    }

    private double getOverallDiscountPercent() {
        try {
            double value = parseFlexibleDouble(OverallDiscountPercentField.getText());
            return Math.max(0.0, value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void loadWarehouses() {
        String sql = """
        SELECT DISTINCT WAREHOUSE
        FROM prod_warehouse_link
        WHERE WAREHOUSE IS NOT NULL
          AND TRIM(WAREHOUSE) <> ''
        ORDER BY WAREHOUSE
    """;

        try {
            DBConnection connect = new DBConnection();
            try (Connection connection = connect.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                ObservableList<Integer> warehouses = FXCollections.observableArrayList();

                while (rs.next()) {
                    warehouses.add(rs.getInt("WAREHOUSE"));
                }

                WarehouseCombo.setItems(warehouses);

                if (!warehouses.isEmpty()) {
                    WarehouseCombo.setValue(warehouses.get(0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Αποθήκες", "Δεν ήταν δυνατή η φόρτωση των αποθηκών.");
        }
    }

    private boolean validateWarehouseSelected() {
        if (WarehouseCombo.getValue() == null) {
            showWarning("Αποθήκη", "Επίλεξε αποθήκη.");
            return false;
        }
        return true;
    }

    private double getAvailableStock(Connection connection, String itemCode, int warehouse) throws SQLException {
        String sql = """
        SELECT COALESCE(STOCK, 0)
        FROM prod_warehouse_link
        WHERE PRODUCT = ? AND WAREHOUSE = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(itemCode));
            stmt.setInt(2, warehouse);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }

        return 0.0;
    }

    private void reduceStock(Connection connection, String itemCode, int warehouse, int quantity) throws SQLException {
        String sql = """
        UPDATE prod_warehouse_link
        SET STOCK = STOCK - ?
        WHERE PRODUCT = ? AND WAREHOUSE = ? AND STOCK >= ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, Integer.parseInt(itemCode));
            stmt.setInt(3, warehouse);
            stmt.setInt(4, quantity);

            int updatedRows = stmt.executeUpdate();

            if (updatedRows == 0) {
                throw new SQLException("Ανεπαρκές απόθεμα για προϊόν " + itemCode + " στην αποθήκη " + warehouse + ".");
            }
        }
    }

    private boolean validateStockForSelectedWarehouse(Connection connection) throws SQLException {
        Integer warehouse = WarehouseCombo.getValue();

        if (warehouse == null) {
            showWarning("Αποθήκη", "Επίλεξε αποθήκη.");
            return false;
        }

        boolean ok = true;

        for (InvoiceLineModel line : invoiceLines) {
            line.setInsufficientStock(false);

            if (isLineEmpty(line)) {
                continue;
            }

            if (line.getItemCode() == null || line.getItemCode().isBlank()) {
                continue;
            }

            if ("0".equals(line.getItemCode())) {
                continue;
            }

            double available = getAvailableStock(connection, line.getItemCode(), warehouse);
            int requested = line.getQuantity();

            if (requested > available) {
                line.setInsufficientStock(true);
                ok = false;
            }
        }

        InvoiceLinesTable.refresh();

        if (!ok) {
            showWarning("Απόθεμα", "Υπάρχουν γραμμές με ανεπαρκές απόθεμα στην επιλεγμένη αποθήκη.");
        }

        return ok;
    }









}