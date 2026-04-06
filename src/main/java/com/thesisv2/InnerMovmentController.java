package com.thesisv2;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.stage.FileChooser;
import java.io.File;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import java.awt.print.PrinterJob;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class InnerMovmentController implements Initializable {

    @FXML private SplitPane SplitPaneControll;

    @FXML private ComboBox<String> MovementTypeCombo;
    @FXML private DatePicker MovementDatePicker;
    @FXML private ComboBox<Integer> SourceWarehouseCombo;
    @FXML private ComboBox<Integer> DestinationWarehouseCombo;
    @FXML private TextArea NotesArea;

    @FXML private Label HeaderInfoLabel;

    @FXML private TableView<MovementLineModel> MovementLinesTable;
    @FXML private TableColumn<MovementLineModel, Integer> ColLineNo;
    @FXML private TableColumn<MovementLineModel, String> ColProductId;
    @FXML private TableColumn<MovementLineModel, String> ColDescription;
    @FXML private TableColumn<MovementLineModel, Integer> ColQuantity;

    @FXML private Button AddLineButton;
    @FXML private Button RemoveLineButton;
    @FXML private Button SaveButton;
    @FXML private Button ClearButton;
    @FXML private Button SavePdfButton;
    @FXML private Button PrintButton;

    private boolean readOnlyMode = false;
    private Integer currentMovementId = null;
    private boolean editMode = false;
    private final ObservableList<MovementLineModel> movementLines = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (SplitPaneControll != null && !SplitPaneControll.getDividers().isEmpty()) {
            SplitPane.Divider divider = SplitPaneControll.getDividers().get(0);
            divider.positionProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() != 0.28) {
                    divider.setPosition(0.28);
                }
            });
        }

        setupCombos();
        loadWarehouses();
        setupTable();
        setDefaults();
        updateMovementTypeState();
    }

    private void setupCombos() {
        MovementTypeCombo.setItems(FXCollections.observableArrayList(
                "ΠΑΡΑΛΑΒΗ",
                "ΕΝΔΟΔΙΑΚΙΝΗΣΗ",
                "ΚΑΤΑΣΤΡΟΦΗ"
        ));

        MovementTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateMovementTypeState());
    }

    private void setDefaults() {
        currentMovementId = null;
        editMode = false;
        readOnlyMode = false;

        MovementTypeCombo.setValue("ΕΝΔΟΔΙΑΚΙΝΗΣΗ");
        MovementDatePicker.setValue(LocalDate.now());
        NotesArea.clear();

        if (!SourceWarehouseCombo.getItems().isEmpty()) {
            SourceWarehouseCombo.setValue(SourceWarehouseCombo.getItems().get(0));
        } else {
            SourceWarehouseCombo.setValue(null);
        }

        if (!DestinationWarehouseCombo.getItems().isEmpty()) {
            DestinationWarehouseCombo.setValue(DestinationWarehouseCombo.getItems().get(0));
        } else {
            DestinationWarehouseCombo.setValue(null);
        }

        movementLines.clear();
        ensureExtraEmptyLine();
        updateMovementTypeState();
        HeaderInfoLabel.setText("Ενδοδιακίνηση");
    }

    private void updateMovementTypeState() {
        String type = MovementTypeCombo.getValue();

        if ("ΠΑΡΑΛΑΒΗ".equals(type)) {
            SourceWarehouseCombo.setDisable(true);
            SourceWarehouseCombo.setValue(null);

            DestinationWarehouseCombo.setDisable(false);
        } else if ("ΚΑΤΑΣΤΡΟΦΗ".equals(type)) {
            SourceWarehouseCombo.setDisable(false);

            DestinationWarehouseCombo.setDisable(true);
            DestinationWarehouseCombo.setValue(null);
        } else {
            SourceWarehouseCombo.setDisable(false);
            DestinationWarehouseCombo.setDisable(false);
        }
    }

    private void setupTable() {
        MovementLinesTable.setEditable(true);

        ColLineNo.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getLineNo()));
        ColProductId.setCellValueFactory(cell -> cell.getValue().productIdProperty());
        ColDescription.setCellValueFactory(cell -> cell.getValue().descriptionProperty());
        ColQuantity.setCellValueFactory(cell -> cell.getValue().quantityProperty().asObject());

        ColLineNo.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        ColProductId.setCellFactory(TextFieldTableCell.forTableColumn());
        ColDescription.setCellFactory(TextFieldTableCell.forTableColumn());
        ColQuantity.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

        ColLineNo.setOnEditCommit(event -> event.getRowValue().setLineNo(event.getNewValue()));

        ColProductId.setOnEditCommit(event -> {
            MovementLineModel line = event.getRowValue();
            String newCode = event.getNewValue() == null ? "" : event.getNewValue().trim();

            line.setProductId(newCode);

            if (newCode.isBlank()) {
                ensureExtraEmptyLine();
                refreshTable();
                return;
            }

            ProductLookupResult product = findProductByCode(newCode);

            if (product != null) {
                line.setProductId(product.productId());
                line.setDescription(product.description());
            } else {
                showWarning("Προϊόν", "Δεν βρέθηκε προϊόν με κωδικό " + newCode + ".");
            }

            ensureExtraEmptyLine();
            refreshTable();
        });

        ColDescription.setOnEditCommit(event -> {
            event.getRowValue().setDescription(event.getNewValue());
            ensureExtraEmptyLine();
            refreshTable();
        });

        ColQuantity.setOnEditCommit(event -> {
            Integer newValue = event.getNewValue();
            event.getRowValue().setQuantity(newValue == null ? 0 : newValue);
            ensureExtraEmptyLine();
            refreshTable();
        });

        MovementLinesTable.setItems(movementLines);
    }

    private void loadWarehouses() {
        String sql = """
                SELECT WarehouseID
                FROM warehouses
                ORDER BY WarehouseID
                """;

        try {
            DBConnection connect = new DBConnection();
            try (Connection connection = connect.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                ObservableList<Integer> warehouses = FXCollections.observableArrayList();

                while (rs.next()) {
                    warehouses.add(rs.getInt("WarehouseID"));
                }

                SourceWarehouseCombo.setItems(FXCollections.observableArrayList(warehouses));
                DestinationWarehouseCombo.setItems(FXCollections.observableArrayList(warehouses));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Αποθήκες", "Δεν ήταν δυνατή η φόρτωση των αποθηκών.");
        }
    }

    @FXML
    private void handleAddLine(javafx.event.ActionEvent event) {
        if (readOnlyMode) return;
        int nextLineNo = movementLines.size() + 1;
        movementLines.add(new MovementLineModel(nextLineNo, "", "", 0));
        ensureExtraEmptyLine();
        refreshTable();
    }

    @FXML
    private void handleRemoveSelectedLine(javafx.event.ActionEvent event) {
        if (readOnlyMode) return;
        MovementLineModel selected = MovementLinesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning("Επιλογή γραμμής", "Επίλεξε πρώτα μια γραμμή για διαγραφή.");
            return;
        }

        movementLines.remove(selected);
        resequenceLines();
        ensureExtraEmptyLine();
        refreshTable();
    }

    @FXML
    private void handleClearAll(javafx.event.ActionEvent event) {
        if (readOnlyMode) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Καθαρισμός");
        alert.setHeaderText("Καθαρισμός όλων των πεδίων");
        alert.setContentText("Θέλεις να διαγραφούν όλα τα πεδία και οι γραμμές;");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            setDefaults();
        }
    }

    @FXML
    private void handleSaveMovement(javafx.event.ActionEvent event) {
        if (readOnlyMode) return;

        if (!validateBeforeSave()) {
            return;
        }

        Connection connection = null;

        try {
            DBConnection connect = new DBConnection();
            connection = connect.getConnection();
            connection.setAutoCommit(false);

            int movementId;

            if (editMode && currentMovementId != null) {
                reverseStockChanges(connection, currentMovementId);
                updateMovementHeader(connection, currentMovementId);
                deleteMovementLines(connection, currentMovementId);
                insertMovementLines(connection, currentMovementId);
                applyStockChanges(connection);
                movementId = currentMovementId;
            } else {
                movementId = insertMovementHeader(connection);
                insertMovementLines(connection, movementId);
                applyStockChanges(connection);
            }

            connection.commit();

            if (editMode) {
                closeCurrentWindow();
            } else {
                    showInfo("Αποθήκευση", "Η κίνηση αποθηκεύτηκε επιτυχώς.");
                    setDefaults();
                }

        } catch (Exception e) {
            e.printStackTrace();

            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (Exception rollbackEx) {
                rollbackEx.printStackTrace();
            }

            showError("Σφάλμα αποθήκευσης",
                    e.getMessage() == null ? "Αποτυχία αποθήκευσης." : e.getMessage());

        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void reverseStockChanges(Connection connection, int movementId) throws Exception {
        String headerSql = """
            SELECT movement_type, source_warehouse, destination_warehouse
            FROM movement_header
            WHERE movement_id = ?
            """;

        String lineSql = """
            SELECT product_id, quantity
            FROM movement_line
            WHERE movement_id = ?
            ORDER BY line_no
            """;

        String type;
        Integer sourceWarehouse;
        Integer destinationWarehouse;

        try (PreparedStatement headerStmt = connection.prepareStatement(headerSql)) {
            headerStmt.setInt(1, movementId);

            try (ResultSet rs = headerStmt.executeQuery()) {
                if (!rs.next()) {
                    throw new Exception("Δεν βρέθηκε η αρχική κίνηση για αντιστροφή.");
                }

                type = rs.getString("movement_type");
                sourceWarehouse = (Integer) rs.getObject("source_warehouse");
                destinationWarehouse = (Integer) rs.getObject("destination_warehouse");
            }
        }

        try (PreparedStatement lineStmt = connection.prepareStatement(lineSql)) {
            lineStmt.setInt(1, movementId);

            try (ResultSet rs = lineStmt.executeQuery()) {
                while (rs.next()) {
                    int productId = rs.getInt("product_id");
                    int quantity = rs.getInt("quantity");

                    if ("ΠΑΡΑΛΑΒΗ".equals(type)) {
                        removeStock(connection, productId, destinationWarehouse, quantity);
                    } else if ("ΕΝΔΟΔΙΑΚΙΝΗΣΗ".equals(type)) {
                        addStock(connection, productId, sourceWarehouse, quantity);
                        removeStock(connection, productId, destinationWarehouse, quantity);
                    } else if ("ΚΑΤΑΣΤΡΟΦΗ".equals(type)) {
                        addStock(connection, productId, sourceWarehouse, quantity);
                    }
                }
            }
        }
    }

    private int insertMovementHeader(Connection connection) throws Exception {
        String insertHeaderSql = """
            INSERT INTO movement_header (
                movement_type, movement_date, source_warehouse, destination_warehouse, notes
            ) VALUES (?, ?, ?, ?, ?)
            """;

        try (PreparedStatement headerStmt = connection.prepareStatement(insertHeaderSql, Statement.RETURN_GENERATED_KEYS)) {
            headerStmt.setString(1, MovementTypeCombo.getValue());
            headerStmt.setDate(2, Date.valueOf(MovementDatePicker.getValue()));

            if (SourceWarehouseCombo.getValue() != null) {
                headerStmt.setInt(3, SourceWarehouseCombo.getValue());
            } else {
                headerStmt.setNull(3, Types.SMALLINT);
            }

            if (DestinationWarehouseCombo.getValue() != null) {
                headerStmt.setInt(4, DestinationWarehouseCombo.getValue());
            } else {
                headerStmt.setNull(4, Types.SMALLINT);
            }

            headerStmt.setString(5, NotesArea.getText());
            headerStmt.executeUpdate();

            try (ResultSet rs = headerStmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new Exception("Δεν επιστράφηκε movement_id.");
    }

    private void updateMovementHeader(Connection connection, int movementId) throws Exception {
        String updateHeaderSql = """
            UPDATE movement_header
            SET movement_type = ?, movement_date = ?, source_warehouse = ?, destination_warehouse = ?, notes = ?
            WHERE movement_id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(updateHeaderSql)) {
            stmt.setString(1, MovementTypeCombo.getValue());
            stmt.setDate(2, Date.valueOf(MovementDatePicker.getValue()));

            if (SourceWarehouseCombo.getValue() != null) {
                stmt.setInt(3, SourceWarehouseCombo.getValue());
            } else {
                stmt.setNull(3, Types.SMALLINT);
            }

            if (DestinationWarehouseCombo.getValue() != null) {
                stmt.setInt(4, DestinationWarehouseCombo.getValue());
            } else {
                stmt.setNull(4, Types.SMALLINT);
            }

            stmt.setString(5, NotesArea.getText());
            stmt.setInt(6, movementId);

            stmt.executeUpdate();
        }
    }

    private void deleteMovementLines(Connection connection, int movementId) throws Exception {
        String sql = "DELETE FROM movement_line WHERE movement_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, movementId);
            stmt.executeUpdate();
        }
    }

    private void insertMovementLines(Connection connection, int movementId) throws Exception {
        String insertLineSql = """
            INSERT INTO movement_line (
                movement_id, line_no, product_id, description, quantity
            ) VALUES (?, ?, ?, ?, ?)
            """;

        try (PreparedStatement lineStmt = connection.prepareStatement(insertLineSql)) {
            int actualLineNo = 1;

            for (MovementLineModel line : movementLines) {
                if (isLineEmpty(line)) {
                    continue;
                }

                lineStmt.setInt(1, movementId);
                lineStmt.setInt(2, actualLineNo++);
                lineStmt.setInt(3, Integer.parseInt(line.getProductId()));
                lineStmt.setString(4, line.getDescription());
                lineStmt.setInt(5, line.getQuantity());
                lineStmt.addBatch();
            }

            lineStmt.executeBatch();
        }
    }

    private void applyStockChanges(Connection connection) throws Exception {
        String type = MovementTypeCombo.getValue();
        Integer sourceWarehouse = SourceWarehouseCombo.getValue();
        Integer destinationWarehouse = DestinationWarehouseCombo.getValue();

        for (MovementLineModel line : movementLines) {
            if (isLineEmpty(line)) {
                continue;
            }

            int productId = Integer.parseInt(line.getProductId());
            int quantity = line.getQuantity();

            if ("ΠΑΡΑΛΑΒΗ".equals(type)) {
                addStock(connection, productId, destinationWarehouse, quantity);
            } else if ("ΕΝΔΟΔΙΑΚΙΝΗΣΗ".equals(type)) {
                removeStock(connection, productId, sourceWarehouse, quantity);
                addStock(connection, productId, destinationWarehouse, quantity);
            } else if ("ΚΑΤΑΣΤΡΟΦΗ".equals(type)) {
                removeStock(connection, productId, sourceWarehouse, quantity);
            }
        }
    }

    private void addStock(Connection connection, int productId, Integer warehouseId, int quantity) throws Exception {
        if (warehouseId == null) {
            throw new Exception("Δεν έχει οριστεί αποθήκη προορισμού.");
        }

        String selectSql = """
                SELECT STOCK
                FROM prod_warehouse_link
                WHERE PRODUCT = ? AND WAREHOUSE = ?
                """;

        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setInt(1, productId);
            selectStmt.setInt(2, warehouseId);

            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    int currentStock = rs.getInt("STOCK");

                    String updateSql = """
                            UPDATE prod_warehouse_link
                            SET STOCK = ?
                            WHERE PRODUCT = ? AND WAREHOUSE = ?
                            """;

                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, currentStock + quantity);
                        updateStmt.setInt(2, productId);
                        updateStmt.setInt(3, warehouseId);
                        updateStmt.executeUpdate();
                    }
                } else {
                    String insertSql = """
                            INSERT INTO prod_warehouse_link (PRODUCT, WAREHOUSE, STOCK)
                            VALUES (?, ?, ?)
                            """;

                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, productId);
                        insertStmt.setInt(2, warehouseId);
                        insertStmt.setInt(3, quantity);
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }

    private void removeStock(Connection connection, int productId, Integer warehouseId, int quantity) throws Exception {
        if (warehouseId == null) {
            throw new Exception("Δεν έχει οριστεί αποθήκη προέλευσης.");
        }

        String selectSql = """
                SELECT STOCK
                FROM prod_warehouse_link
                WHERE PRODUCT = ? AND WAREHOUSE = ?
                """;

        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setInt(1, productId);
            selectStmt.setInt(2, warehouseId);

            try (ResultSet rs = selectStmt.executeQuery()) {
                if (!rs.next()) {
                    throw new Exception("Δεν υπάρχει απόθεμα για το προϊόν " + productId + " στην αποθήκη " + warehouseId + ".");
                }

                int currentStock = rs.getInt("STOCK");

                if (currentStock < quantity) {
                    throw new Exception("Μη επαρκές απόθεμα για το προϊόν " + productId + " στην αποθήκη " + warehouseId + ".");
                }

                String updateSql = """
                        UPDATE prod_warehouse_link
                        SET STOCK = ?
                        WHERE PRODUCT = ? AND WAREHOUSE = ?
                        """;

                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, currentStock - quantity);
                    updateStmt.setInt(2, productId);
                    updateStmt.setInt(3, warehouseId);
                    updateStmt.executeUpdate();
                }
            }
        }
    }

    private boolean validateBeforeSave() {
        if (MovementTypeCombo.getValue() == null || MovementTypeCombo.getValue().isBlank()) {
            showWarning("Τύπος κίνησης", "Επίλεξε τύπο κίνησης.");
            return false;
        }

        if (MovementDatePicker.getValue() == null) {
            showWarning("Ημερομηνία", "Επίλεξε ημερομηνία.");
            return false;
        }

        String type = MovementTypeCombo.getValue();

        if ("ΠΑΡΑΛΑΒΗ".equals(type)) {
            if (DestinationWarehouseCombo.getValue() == null) {
                showWarning("Αποθήκη προορισμού", "Επίλεξε αποθήκη προορισμού.");
                return false;
            }
        }

        if ("ΚΑΤΑΣΤΡΟΦΗ".equals(type)) {
            if (SourceWarehouseCombo.getValue() == null) {
                showWarning("Αποθήκη προέλευσης", "Επίλεξε αποθήκη προέλευσης.");
                return false;
            }
        }

        if ("ΕΝΔΟΔΙΑΚΙΝΗΣΗ".equals(type)) {
            if (SourceWarehouseCombo.getValue() == null || DestinationWarehouseCombo.getValue() == null) {
                showWarning("Αποθήκες", "Επίλεξε αποθήκη προέλευσης και αποθήκη προορισμού.");
                return false;
            }

            if (SourceWarehouseCombo.getValue().equals(DestinationWarehouseCombo.getValue())) {
                showWarning("Αποθήκες", "Η αποθήκη προέλευσης και προορισμού δεν μπορεί να είναι ίδια.");
                return false;
            }
        }

        boolean hasValidLine = false;

        for (MovementLineModel line : movementLines) {
            if (isLineEmpty(line)) {
                continue;
            }

            hasValidLine = true;

            if (line.getProductId() == null || line.getProductId().isBlank()) {
                showWarning("Γραμμές", "Υπάρχει γραμμή χωρίς κωδικό προϊόντος.");
                return false;
            }

            try {
                Integer.parseInt(line.getProductId());
            } catch (Exception e) {
                showWarning("Κωδικός προϊόντος", "Ο κωδικός προϊόντος πρέπει να είναι αριθμός.");
                return false;
            }

            if (line.getDescription() == null || line.getDescription().isBlank()) {
                showWarning("Περιγραφή", "Υπάρχει γραμμή χωρίς περιγραφή.");
                return false;
            }

            if (line.getQuantity() <= 0) {
                showWarning("Ποσότητα", "Η ποσότητα πρέπει να είναι μεγαλύτερη από το μηδέν.");
                return false;
            }
        }

        if (!hasValidLine) {
            showWarning("Γραμμές", "Πρόσθεσε τουλάχιστον μία γραμμή.");
            return false;
        }

        return true;
    }

    private ProductLookupResult findProductByCode(String productCode) {
        String sql = """
                SELECT ProductID, ProductDescription
                FROM products
                WHERE ProductID = ?
                """;

        try {
            int code = Integer.parseInt(productCode);

            DBConnection connect = new DBConnection();
            try (Connection connection = connect.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setInt(1, code);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new ProductLookupResult(
                                String.valueOf(rs.getInt("ProductID")),
                                rs.getString("ProductDescription")
                        );
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private void ensureExtraEmptyLine() {
        if (movementLines.isEmpty()) {
            movementLines.add(new MovementLineModel(1, "", "", 0));
            return;
        }

        MovementLineModel last = movementLines.get(movementLines.size() - 1);

        if (!isLineEmpty(last)) {
            movementLines.add(new MovementLineModel(movementLines.size() + 1, "", "", 0));
        }

        resequenceLines();
    }

    private boolean isLineEmpty(MovementLineModel line) {
        String productId = line.getProductId() == null ? "" : line.getProductId().trim();
        String description = line.getDescription() == null ? "" : line.getDescription().trim();

        return productId.isBlank() && description.isBlank();
    }

    private void resequenceLines() {
        for (int i = 0; i < movementLines.size(); i++) {
            movementLines.get(i).setLineNo(i + 1);
        }
    }

    private void refreshTable() {
        resequenceLines();
        MovementLinesTable.refresh();
    }

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private record ProductLookupResult(String productId, String description) {
    }

    public void setReadOnlyMode(boolean readOnly) {
        this.readOnlyMode = readOnly;

        MovementTypeCombo.setDisable(readOnly);
        MovementDatePicker.setDisable(readOnly);

        NotesArea.setEditable(!readOnly);
        MovementLinesTable.setEditable(!readOnly);

        if (AddLineButton != null) AddLineButton.setDisable(readOnly);
        if (RemoveLineButton != null) RemoveLineButton.setDisable(readOnly);
        if (SaveButton != null) SaveButton.setDisable(readOnly);
        if (ClearButton != null) ClearButton.setDisable(readOnly);

        if (readOnly) {
            SourceWarehouseCombo.setDisable(true);
            DestinationWarehouseCombo.setDisable(true);
            HeaderInfoLabel.setText("Προβολή κίνησης");
        } else {
            updateMovementTypeState();
        }
    }

    public void loadMovementForView(int movementId) {
        currentMovementId = movementId;

        String headerSql = """
            SELECT *
            FROM movement_header
            WHERE movement_id = ?
            """;

        String lineSql = """
            SELECT *
            FROM movement_line
            WHERE movement_id = ?
            ORDER BY line_no
            """;

        try (Connection connection = new DBConnection().getConnection();
             PreparedStatement headerStmt = connection.prepareStatement(headerSql);
             PreparedStatement lineStmt = connection.prepareStatement(lineSql)) {

            headerStmt.setInt(1, movementId);

            try (ResultSet rs = headerStmt.executeQuery()) {
                if (!rs.next()) {
                    showError("Δεν βρέθηκε", "Η κίνηση δεν βρέθηκε.");
                    return;
                }

                MovementTypeCombo.setValue(rs.getString("movement_type"));

                Date movementDate = rs.getDate("movement_date");
                if (movementDate != null) {
                    MovementDatePicker.setValue(movementDate.toLocalDate());
                }

                Integer sourceWarehouse = (Integer) rs.getObject("source_warehouse");
                Integer destinationWarehouse = (Integer) rs.getObject("destination_warehouse");

                SourceWarehouseCombo.setValue(sourceWarehouse);
                DestinationWarehouseCombo.setValue(destinationWarehouse);

                NotesArea.setText(rs.getString("notes"));
            }

            movementLines.clear();

            lineStmt.setInt(1, movementId);

            try (ResultSet rsLines = lineStmt.executeQuery()) {
                while (rsLines.next()) {
                    movementLines.add(new MovementLineModel(
                            rsLines.getInt("line_no"),
                            String.valueOf(rsLines.getInt("product_id")),
                            rsLines.getString("description"),
                            rsLines.getInt("quantity")
                    ));
                }
            }

            ensureExtraEmptyLine();
            refreshTable();
            HeaderInfoLabel.setText("Κίνηση: " + movementId);
            updateMovementTypeState();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Σφάλμα φόρτωσης", "Δεν ήταν δυνατή η φόρτωση της κίνησης.");
        }
    }

    public void loadMovementForEdit(int movementId) {
        currentMovementId = movementId;
        editMode = true;
        readOnlyMode = false;

        String headerSql = """
            SELECT *
            FROM movement_header
            WHERE movement_id = ?
            """;

        String lineSql = """
            SELECT *
            FROM movement_line
            WHERE movement_id = ?
            ORDER BY line_no
            """;

        try (Connection connection = new DBConnection().getConnection();
             PreparedStatement headerStmt = connection.prepareStatement(headerSql);
             PreparedStatement lineStmt = connection.prepareStatement(lineSql)) {

            headerStmt.setInt(1, movementId);

            try (ResultSet rs = headerStmt.executeQuery()) {
                if (!rs.next()) {
                    showError("Δεν βρέθηκε", "Η κίνηση δεν βρέθηκε.");
                    return;
                }

                MovementTypeCombo.setValue(rs.getString("movement_type"));

                Date movementDate = rs.getDate("movement_date");
                if (movementDate != null) {
                    MovementDatePicker.setValue(movementDate.toLocalDate());
                }

                Integer sourceWarehouse = (Integer) rs.getObject("source_warehouse");
                Integer destinationWarehouse = (Integer) rs.getObject("destination_warehouse");

                SourceWarehouseCombo.setValue(sourceWarehouse);
                DestinationWarehouseCombo.setValue(destinationWarehouse);

                NotesArea.setText(rs.getString("notes"));
            }

            movementLines.clear();

            lineStmt.setInt(1, movementId);

            try (ResultSet rsLines = lineStmt.executeQuery()) {
                while (rsLines.next()) {
                    movementLines.add(new MovementLineModel(
                            rsLines.getInt("line_no"),
                            String.valueOf(rsLines.getInt("product_id")),
                            rsLines.getString("description"),
                            rsLines.getInt("quantity")
                    ));
                }
            }

            ensureExtraEmptyLine();
            refreshTable();
            updateMovementTypeState();
            HeaderInfoLabel.setText("Επεξεργασία κίνησης: " + movementId);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Σφάλμα φόρτωσης", "Δεν ήταν δυνατή η φόρτωση της κίνησης.");
        }
    }

    private void closeCurrentWindow(){
        if (SaveButton != null && SaveButton.getScene() != null && SaveButton.getScene().getWindow() != null){
            ((javafx.stage.Stage) SaveButton.getScene().getWindow()).close();
        }
    }

    @FXML
    private void handleSavePdf(javafx.event.ActionEvent event) {
        if (!validateBeforeExport()) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Αποθήκευση PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        String movementNumber = currentMovementId == null ? "draft" : String.valueOf(currentMovementId);
        System.out.println(movementNumber);
        fileChooser.setInitialFileName("movement_" + movementNumber + ".pdf");

        File file = fileChooser.showSaveDialog(MovementLinesTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            PdfMovementService pdfService = new PdfMovementService();
            pdfService.exportMovementToPdf(
                    file,
                    currentMovementId == null ? "000000" : String.valueOf(currentMovementId),
                    MovementTypeCombo.getValue(),
                    MovementDatePicker.getValue(),
                    SourceWarehouseCombo.getValue(),
                    DestinationWarehouseCombo.getValue(),
                    NotesArea.getText(),
                    getRealMovementLines()
            );

            showInfo("PDF", "Το PDF της κίνησης αποθηκεύτηκε επιτυχώς.");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Σφάλμα PDF", e.getMessage());
        }
    }

    @FXML
    private void handlePrint(javafx.event.ActionEvent event) {
        if (!validateBeforeExport()) {
            return;
        }

        File tempPdf = null;

        try {
            String movementIdForPrint = currentMovementId == null
                    ? "000000"
                    : String.valueOf(currentMovementId);

            tempPdf = File.createTempFile("movement_" + movementIdForPrint + "_", ".pdf");

            PdfMovementService pdfService = new PdfMovementService();
            pdfService.exportMovementToPdf(
                    tempPdf,
                    movementIdForPrint,
                    MovementTypeCombo.getValue(),
                    MovementDatePicker.getValue(),
                    SourceWarehouseCombo.getValue(),
                    DestinationWarehouseCombo.getValue(),
                    NotesArea.getText(),
                    getRealMovementLines()
            );

            printPdfFile(tempPdf);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Σφάλμα εκτύπωσης", e.getMessage());
        } finally {
            if (tempPdf != null) {
                try {
                    Files.deleteIfExists(tempPdf.toPath());
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void printPdfFile(File pdfFile) throws Exception {
        if (pdfFile == null || !pdfFile.exists()) {
            throw new Exception("Το προσωρινό PDF δεν βρέθηκε.");
        }

        PrinterJob job = PrinterJob.getPrinterJob();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            job.setPageable(new PDFPageable(document));

            boolean accepted = job.printDialog();
            if (!accepted) {
                return;
            }

            job.print();
        }
    }

    private boolean validateBeforeExport() {
        boolean hasAtLeastOneRealLine = false;

        for (MovementLineModel line : movementLines) {
            if (!isLineEmpty(line)) {
                hasAtLeastOneRealLine = true;
                break;
            }
        }

        if (!hasAtLeastOneRealLine) {
            showWarning("Έλεγχος", "Δεν υπάρχουν γραμμές για εξαγωγή/εκτύπωση.");
            return false;
        }

        if (MovementTypeCombo.getValue() == null || MovementTypeCombo.getValue().isBlank()) {
            showWarning("Έλεγχος", "Δεν έχει οριστεί τύπος κίνησης.");
            return false;
        }

        if (MovementDatePicker.getValue() == null) {
            showWarning("Έλεγχος", "Δεν έχει οριστεί ημερομηνία.");
            return false;
        }

        return true;
    }

    private ObservableList<MovementLineModel> getRealMovementLines() {
        return movementLines.stream()
                .filter(line -> line != null)
                .filter(line -> !isLineEmpty(line))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }




}














