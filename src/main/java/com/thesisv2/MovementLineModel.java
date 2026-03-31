package com.thesisv2;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MovementLineModel {

    private final IntegerProperty lineNo = new SimpleIntegerProperty();
    private final StringProperty productId = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final IntegerProperty quantity = new SimpleIntegerProperty(1);

    public MovementLineModel() {
    }

    public MovementLineModel(int lineNo, String productId, String description, int quantity) {
        setLineNo(lineNo);
        setProductId(productId);
        setDescription(description);
        setQuantity(quantity);
    }

    public int getLineNo() {
        return lineNo.get();
    }

    public void setLineNo(int value) {
        lineNo.set(value);
    }

    public IntegerProperty lineNoProperty() {
        return lineNo;
    }

    public String getProductId() {
        return productId.get();
    }

    public void setProductId(String value) {
        productId.set(value == null ? "" : value);
    }

    public StringProperty productIdProperty() {
        return productId;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String value) {
        description.set(value == null ? "" : value);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public int getQuantity() {
        return quantity.get();
    }

    public void setQuantity(int value) {
        quantity.set(Math.max(value, 0));
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }
}