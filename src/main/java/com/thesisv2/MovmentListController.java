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

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.Normalizer;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class MovmentListController implements Initializable {

    @FXML private TextField SearchMovementId;
    @FXML private TextField SearchMovementType;
    @FXML private TextField SearchMovementDate;
    @FXML private TextField SearchSourceWarehouse;
    @FXML private TextField SearchDestinationWarehouse;
    @FXML private TextField SearchNotes;
    @FXML private TextField SearchAll;
    @FXML private Label ResultLabel;

    @FXML private TableView<MovementListRow> MovementTableView;
    @FXML private TableColumn<MovementListRow, Integer> ColumnMovementId;
    @FXML private TableColumn<MovementListRow, String> ColumnMovementType;
    @FXML private TableColumn<MovementListRow, String> ColumnMovementDate;
    @FXML private TableColumn<MovementListRow, String> ColumnSourceWarehouse;
    @FXML private TableColumn<MovementListRow, String> ColumnDestinationWarehouse;
    @FXML private TableColumn<MovementListRow, String> ColumnNotes;

    @FXML private SplitPane SplitPaneControll;
    @FXML private VBox FilterPane;

    private final ObservableList<MovementListRow> movementRows = FXCollections.observableArrayList();
    private static final double FILTER_WIDTH = 220.0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupFixedSidebar();
        setupTable();
        setupContextMenuAndDoubleClick();
        loadMovements();
        setupFilters();
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

    private void setupTable() {
        ColumnMovementId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getMovementId()));
        ColumnMovementType.setCellValueFactory(new PropertyValueFactory<>("movementType"));
        ColumnMovementDate.setCellValueFactory(new PropertyValueFactory<>("movementDate"));
        ColumnSourceWarehouse.setCellValueFactory(new PropertyValueFactory<>("sourceWarehouse"));
        ColumnDestinationWarehouse.setCellValueFactory(new PropertyValueFactory<>("destinationWarehouse"));
        ColumnNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        ColumnNotes.setCellFactory(tc -> wrapCell());
    }

    private TableCell<MovementListRow, String> wrapCell() {
        return new TableCell<>() {
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
    }

    private void loadMovements() {
        movementRows.clear();

        String sql = """
                SELECT
                    movement_id,
                    movement_type,
                    movement_date,
                    source_warehouse,
                    destination_warehouse,
                    notes
                FROM movement_header
                ORDER BY movement_id DESC
                """;

        try (Connection connection = new DBConnection().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int movementId = rs.getInt("movement_id");
                String movementType = rs.getString("movement_type");
                String movementDate = rs.getDate("movement_date") == null ? "" : rs.getDate("movement_date").toString();

                Integer sourceWarehouse = (Integer) rs.getObject("source_warehouse");
                Integer destinationWarehouse = (Integer) rs.getObject("destination_warehouse");

                String notes = rs.getString("notes");

                movementRows.add(new MovementListRow(
                        movementId,
                        movementType,
                        movementDate,
                        sourceWarehouse == null ? "" : String.valueOf(sourceWarehouse),
                        destinationWarehouse == null ? "" : String.valueOf(destinationWarehouse),
                        notes
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Σφάλμα φόρτωσης", "Δεν ήταν δυνατή η φόρτωση των κινήσεων.");
        }
    }

    private void setupFilters() {
        FilteredList<MovementListRow> filteredData = new FilteredList<>(movementRows, b -> true);

        SearchMovementId.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchMovementType.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchMovementDate.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchSourceWarehouse.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchDestinationWarehouse.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchNotes.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));
        SearchAll.textProperty().addListener((obs, oldVal, newVal) -> applyFilters(filteredData));

        SortedList<MovementListRow> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(MovementTableView.comparatorProperty());
        MovementTableView.setItems(sortedData);

        ResultLabel.textProperty().bind(
                Bindings.size(sortedData).asString("Αποτελέσματα: %d")
        );
    }

    private void applyFilters(FilteredList<MovementListRow> filteredData) {
        String movementIdFilter = SearchMovementId.getText().trim();
        String movementTypeFilter = SearchMovementType.getText().trim();
        String movementDateFilter = SearchMovementDate.getText().trim();
        String sourceWarehouseFilter = SearchSourceWarehouse.getText().trim();
        String destinationWarehouseFilter = SearchDestinationWarehouse.getText().trim();
        String notesFilter = SearchNotes.getText().trim();
        String allFilter = SearchAll.getText().trim();

        filteredData.setPredicate(movement -> {
            if (!movementIdFilter.isBlank()
                    && !matchesWildcard(String.valueOf(movement.getMovementId()), movementIdFilter, false)) {
                return false;
            }

            if (!movementTypeFilter.isBlank()
                    && !matchesWildcard(movement.getMovementType(), movementTypeFilter, true)) {
                return false;
            }

            if (!movementDateFilter.isBlank()
                    && !matchesWildcard(movement.getMovementDate(), movementDateFilter, false)) {
                return false;
            }

            if (!sourceWarehouseFilter.isBlank()
                    && !matchesWildcard(movement.getSourceWarehouse(), sourceWarehouseFilter, false)) {
                return false;
            }

            if (!destinationWarehouseFilter.isBlank()
                    && !matchesWildcard(movement.getDestinationWarehouse(), destinationWarehouseFilter, false)) {
                return false;
            }

            if (!notesFilter.isBlank()
                    && !matchesWildcard(movement.getNotes(), notesFilter, true)) {
                return false;
            }

            if (!allFilter.isBlank()) {
                boolean matchesAny =
                        matchesWildcard(String.valueOf(movement.getMovementId()), allFilter, false)
                                || matchesWildcard(movement.getMovementType(), allFilter, true)
                                || matchesWildcard(movement.getMovementDate(), allFilter, false)
                                || matchesWildcard(movement.getSourceWarehouse(), allFilter, false)
                                || matchesWildcard(movement.getDestinationWarehouse(), allFilter, false)
                                || matchesWildcard(movement.getNotes(), allFilter, true);

                if (!matchesAny) {
                    return false;
                }
            }

            return true;
        });
    }

    private void setupContextMenuAndDoubleClick() {
        MovementTableView.setRowFactory(tv -> {
            TableRow<MovementListRow> row = new TableRow<>();

            ContextMenu contextMenu = new ContextMenu();

            MenuItem viewItem = new MenuItem("Προβολή");
            MenuItem editItem = new MenuItem("Επεξεργασία");

            viewItem.setOnAction(event -> {
                MovementListRow selectedMovement = row.getItem();
                if (selectedMovement != null) {
                    MovementTableView.getSelectionModel().select(selectedMovement);
                    openMovementReadOnly(selectedMovement.getMovementId());
                }
            });

            editItem.setOnAction(event -> {
                MovementListRow selectedMovement = row.getItem();
                if (selectedMovement != null) {
                    MovementTableView.getSelectionModel().select(selectedMovement);
                    openMovementForEdit(selectedMovement.getMovementId());
                }
            });

            contextMenu.getItems().addAll(viewItem, editItem);

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    MovementListRow selectedMovement = row.getItem();
                    if (selectedMovement != null) {
                        openMovementReadOnly(selectedMovement.getMovementId());
                    }
                }
            });

            return row;
        });
    }

    private void openMovementReadOnly(int movementId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("inner-movement-view.fxml"));
            Parent root = loader.load();

            InnerMovmentController controller = loader.getController();
            controller.loadMovementForView(movementId);
            controller.setReadOnlyMode(true);

            Stage stage = new Stage();
            stage.setTitle("Προβολή κίνησης");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1400, 800));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Σφάλμα", "Δεν ήταν δυνατό το άνοιγμα της κίνησης.");
        }
    }

    private void openMovementForEdit(int movementId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("inner-movement-view.fxml"));
            Parent root = loader.load();

            InnerMovmentController controller = loader.getController();
            controller.loadMovementForEdit(movementId);

            Stage stage = new Stage();
            stage.setTitle("Επεξεργασία κίνησης");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1400, 800));
            stage.showAndWait();

            loadMovements();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Σφάλμα", "Δεν ήταν δυνατό το άνοιγμα της κίνησης για επεξεργασία.");
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

    public void refreshMovement(){
        loadMovements();
    }

    @FXML
    private void handleRefreshMovements(javafx.event.ActionEvent event){
        loadMovements();
    }





}













