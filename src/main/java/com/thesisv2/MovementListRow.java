package com.thesisv2;

import javafx.beans.property.*;

public class MovementListRow {

    private final IntegerProperty movementId = new SimpleIntegerProperty();
    private final StringProperty movementType = new SimpleStringProperty("");
    private final StringProperty movementDate = new SimpleStringProperty("");
    private final StringProperty sourceWarehouse = new SimpleStringProperty("");
    private final StringProperty destinationWarehouse = new SimpleStringProperty("");
    private final StringProperty notes = new SimpleStringProperty("");

    public MovementListRow(int movementId,
                           String movementType,
                           String movementDate,
                           String sourceWarehouse,
                           String destinationWarehouse,
                           String notes) {
        setMovementId(movementId);
        setMovementType(movementType);
        setMovementDate(movementDate);
        setSourceWarehouse(sourceWarehouse);
        setDestinationWarehouse(destinationWarehouse);
        setNotes(notes);
    }

    public int getMovementId() {
        return movementId.get();
    }

    public void setMovementId(int value) {
        movementId.set(value);
    }

    public IntegerProperty movementIdProperty() {
        return movementId;
    }

    public String getMovementType() {
        return movementType.get();
    }

    public void setMovementType(String value) {
        movementType.set(value == null ? "" : value);
    }

    public StringProperty movementTypeProperty() {
        return movementType;
    }

    public String getMovementDate() {
        return movementDate.get();
    }

    public void setMovementDate(String value) {
        movementDate.set(value == null ? "" : value);
    }

    public StringProperty movementDateProperty() {
        return movementDate;
    }

    public String getSourceWarehouse() {
        return sourceWarehouse.get();
    }

    public void setSourceWarehouse(String value) {
        sourceWarehouse.set(value == null ? "" : value);
    }

    public StringProperty sourceWarehouseProperty() {
        return sourceWarehouse;
    }

    public String getDestinationWarehouse() {
        return destinationWarehouse.get();
    }

    public void setDestinationWarehouse(String value) {
        destinationWarehouse.set(value == null ? "" : value);
    }

    public StringProperty destinationWarehouseProperty() {
        return destinationWarehouse;
    }

    public String getNotes() {
        return notes.get();
    }

    public void setNotes(String value) {
        notes.set(value == null ? "" : value);
    }

    public StringProperty notesProperty() {
        return notes;
    }
}