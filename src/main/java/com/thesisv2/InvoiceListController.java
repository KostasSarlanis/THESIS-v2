package com.thesisv2;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.Normalizer;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class InvoiceListController implements Initializable {

    @FXML private TextField SearchInvoiceId;
    @FXML private TextField SearchInvoiceType;
    @FXML private TextField SearchCustomerName;
    @FXML private TextField SearchCustomerTaxId;
    @FXML private TextField SearchGrandTotal;
    @FXML private TextField SearchNotes;
    @FXML private TextField SearchPaymentTerms;
    @FXML private TextField SearchAll;
    @FXML private Label ResultLabel;

    @FXML private TableView<InvoiceListRow> InvoiceTableView;
    @FXML private TableColumn<InvoiceListRow, Integer> ColumnInvoiceId;
    @FXML private TableColumn<InvoiceListRow, String> ColumnInvoiceType;
    @FXML private TableColumn<InvoiceListRow, String> ColumnCustomerName;
    @FXML private TableColumn<InvoiceListRow, String> ColumnCustomerTaxId;
    @FXML private TableColumn<InvoiceListRow, String> ColumnGrandTotal;
    @FXML private TableColumn<InvoiceListRow, String> ColumnNotes;
    @FXML private TableColumn<InvoiceListRow, String> ColumnPaymentTerms;

    @FXML private SplitPane SplitPaneControll;
    @FXML private VBox FilterPane;

    private final ObservableList<InvoiceListRow> invoiceRows = FXCollections.observableArrayList();

    private static final double FILTER_WIDTH = 220.0;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupFixedSidebar();
        setupTable();
        setupContextMenuAndDoubleClick();
        loadInvoices();
        setupFilters();
    }

    private void setupTable() {
        ColumnInvoiceId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getInvoiceId()));
        ColumnInvoiceType.setCellValueFactory(new PropertyValueFactory<>("invoiceType"));
        ColumnCustomerName.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        ColumnCustomerTaxId.setCellValueFactory(new PropertyValueFactory<>("customerTaxId"));
        ColumnGrandTotal.setCellValueFactory(new PropertyValueFactory<>("grandTotal"));
        ColumnNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));
        ColumnPaymentTerms.setCellValueFactory(new PropertyValueFactory<>("paymentTerms"));

        ColumnNotes.setCellFactory(tc -> wrapCell());
        ColumnPaymentTerms.setCellFactory(tc -> wrapCell());
    }

    private void setupFixedSidebar() {
        FilterPane.setMinWidth(FILTER_WIDTH);
        FilterPane.setPrefWidth(FILTER_WIDTH);
        FilterPane.setMaxWidth(FILTER_WIDTH);

        SplitPane.setResizableWithParent(FilterPane, false);

        Platform.runLater(() -> {
            double totalWidth = SplitPaneControll.getWidth();
            if (totalWidth > 0) {
                SplitPaneControll.setDividerPositions(FILTER_WIDTH / totalWidth);
            }
        });

        SplitPaneControll.widthProperty().addListener((obs, oldVal, newVal) -> {
            double totalWidth = newVal.doubleValue();
            if (totalWidth > 0) {
                SplitPaneControll.setDividerPositions(FILTER_WIDTH / totalWidth);
            }
        });
    }

    private TableCell<InvoiceListRow, String> wrapCell() {
        TableCell<InvoiceListRow, String> cell = new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setWrapText(true);
                }
            }
        };
        return cell;
    }

    private void loadInvoices() {
        invoiceRows.clear();

        String sql = """
                SELECT
                    invoice_id,
                    invoice_type,
                    customer_name,
                    customer_tax_id,
                    grand_total,
                    notes,
                    payment_terms
                FROM invoice_header
                ORDER BY invoice_id DESC
                """;

        try (Connection connection = new DBConnection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int invoiceId = rs.getInt("invoice_id");
                String invoiceType = rs.getString("invoice_type");
                String customerName = rs.getString("customer_name");
                String customerTaxId = rs.getString("customer_tax_id");

                BigDecimal grandTotalValue = rs.getBigDecimal("grand_total");
                String grandTotal = grandTotalValue == null ? "" : grandTotalValue.stripTrailingZeros().toPlainString();

                String notes = rs.getString("notes");
                String paymentTerms = rs.getString("payment_terms");

                invoiceRows.add(new InvoiceListRow(
                        invoiceId,
                        invoiceType,
                        customerName,
                        customerTaxId,
                        grandTotal,
                        notes,
                        paymentTerms
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Σφάλμα φόρτωσης", "Δεν ήταν δυνατή η φόρτωση των παραστατικών.");
        }
    }

    private void setupFilters() {
        FilteredList<InvoiceListRow> filteredData = new FilteredList<>(invoiceRows, b -> true);

        SearchInvoiceId.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchInvoiceType.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchCustomerName.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchCustomerTaxId.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchGrandTotal.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchNotes.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchPaymentTerms.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchAll.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));

        SortedList<InvoiceListRow> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(InvoiceTableView.comparatorProperty());
        InvoiceTableView.setItems(sortedData);

        ResultLabel.textProperty().bind(
                Bindings.size(sortedData).asString("Αποτελέσματα: %d")
        );
    }

    private void applyFilters(FilteredList<InvoiceListRow> filteredData) {
        String invoiceIdFilter = SearchInvoiceId.getText().trim();
        String invoiceTypeFilter = SearchInvoiceType.getText().trim();
        String customerNameFilter = SearchCustomerName.getText().trim();
        String customerTaxIdFilter = SearchCustomerTaxId.getText().trim();
        String grandTotalFilter = SearchGrandTotal.getText().trim();
        String notesFilter = SearchNotes.getText().trim();
        String paymentTermsFilter = SearchPaymentTerms.getText().trim();
        String allFilter = SearchAll.getText().trim();

        filteredData.setPredicate(invoice -> {
            if (!invoiceIdFilter.isBlank()
                    && !matchesWildcard(String.valueOf(invoice.getInvoiceId()), invoiceIdFilter, false)) {
                return false;
            }

            if (!invoiceTypeFilter.isBlank()
                    && !matchesWildcard(invoice.getInvoiceType(), invoiceTypeFilter, true)) {
                return false;
            }

            if (!customerNameFilter.isBlank()
                    && !matchesWildcard(invoice.getCustomerName(), customerNameFilter, true)) {
                return false;
            }

            if (!customerTaxIdFilter.isBlank()
                    && !matchesWildcard(invoice.getCustomerTaxId(), customerTaxIdFilter, true)) {
                return false;
            }

            if (!grandTotalFilter.isBlank()
                    && !matchesWildcard(invoice.getGrandTotal(), grandTotalFilter, false)) {
                return false;
            }

            if (!notesFilter.isBlank()
                    && !matchesWildcard(invoice.getNotes(), notesFilter, true)) {
                return false;
            }

            if (!paymentTermsFilter.isBlank()
                    && !matchesWildcard(invoice.getPaymentTerms(), paymentTermsFilter, true)) {
                return false;
            }

            if (!allFilter.isBlank()) {
                boolean matchesAny =
                        matchesWildcard(String.valueOf(invoice.getInvoiceId()), allFilter, false)
                                || matchesWildcard(invoice.getInvoiceType(), allFilter, true)
                                || matchesWildcard(invoice.getCustomerName(), allFilter, true)
                                || matchesWildcard(invoice.getCustomerTaxId(), allFilter, true)
                                || matchesWildcard(invoice.getGrandTotal(), allFilter, false)
                                || matchesWildcard(invoice.getNotes(), allFilter, true)
                                || matchesWildcard(invoice.getPaymentTerms(), allFilter, true);

                if (!matchesAny) {
                    return false;
                }
            }

            return true;
        });
    }

    private void setupContextMenuAndDoubleClick() {
        InvoiceTableView.setRowFactory(tv -> {
            TableRow<InvoiceListRow> row = new TableRow<>();

            ContextMenu contextMenu = new ContextMenu();
            MenuItem viewItem = new MenuItem("Προβολή");

            viewItem.setOnAction(event -> {
                InvoiceListRow selectedInvoice = row.getItem();
                if (selectedInvoice != null) {
                    InvoiceTableView.getSelectionModel().select(selectedInvoice);
                    openInvoiceReadOnly(selectedInvoice.getInvoiceId());
                }
            });

            contextMenu.getItems().add(viewItem);

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    InvoiceListRow selectedInvoice = row.getItem();
                    if (selectedInvoice != null) {
                        openInvoiceReadOnly(selectedInvoice.getInvoiceId());
                    }
                }
            });

            return row;
        });
    }

    private void openInvoiceReadOnly(int invoiceId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("create-invoice-view.fxml"));
            Parent root = loader.load();

            CreateInvoiceController controller = loader.getController();
            controller.loadInvoiceForView(invoiceId);
            controller.setReadOnlyMode(true);

            Stage stage = new Stage();
            stage.setTitle("Προβολή παραστατικού");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1400, 800));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Σφάλμα", "Δεν ήταν δυνατό το άνοιγμα του παραστατικού.");
        }
    }

    private String normalizeGreek(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized
                .replace('ς', 'σ')
                .toLowerCase();
    }

    private boolean matchesWildcard(String value, String filter, boolean greekSensitive) {
        String source = value == null ? "" : value;
        String pattern = filter == null ? "" : filter;

        if (greekSensitive) {
            source = normalizeGreek(source);
            pattern = normalizeGreek(pattern);
        } else {
            source = source.toLowerCase();
            pattern = pattern.toLowerCase();
        }

        String regex = pattern
                .replace(".", "\\.")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("+", "\\+")
                .replace("?", "\\?")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("|", "\\|")
                .replace("*", ".*");

        return Pattern.compile("^" + regex + "$", Pattern.DOTALL).matcher(source).find()
                || Pattern.compile(regex, Pattern.DOTALL).matcher(source).find();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}