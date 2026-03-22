package com.thesisv2;

import javafx.beans.property.*;

public class InvoiceLineModel {

    private final IntegerProperty lineNo = new SimpleIntegerProperty();
    private final StringProperty itemCode = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final DoubleProperty quantity = new SimpleDoubleProperty(1.0);
    private final StringProperty unitName = new SimpleStringProperty("τεμ");
    private final DoubleProperty unitPrice = new SimpleDoubleProperty(0.0);
    private final DoubleProperty discountPercent = new SimpleDoubleProperty(0.0);
    private final DoubleProperty taxPercent = new SimpleDoubleProperty(24.0);
    private final DoubleProperty lineTotal = new SimpleDoubleProperty(0.0);

    public InvoiceLineModel() {
        recalculateLineTotal();
    }

    public InvoiceLineModel(int lineNo, String itemCode, String description, double quantity,
                            String unitName, double unitPrice, double discountPercent, double taxPercent) {
        setLineNo(lineNo);
        setItemCode(itemCode);
        setDescription(description);
        setQuantity(quantity);
        setUnitName(unitName);
        setUnitPrice(unitPrice);
        setDiscountPercent(discountPercent);
        setTaxPercent(taxPercent);
        recalculateLineTotal();
    }

    public void recalculateLineTotal() {
        double base = getQuantity() * getUnitPrice();
        double discountAmount = base * (getDiscountPercent() / 100.0);
        double taxable = base - discountAmount;
        double taxAmount = taxable * (getTaxPercent() / 100.0);
        setLineTotal(taxable + taxAmount);
    }

    public int getLineNo() { return lineNo.get(); }
    public void setLineNo(int value) { lineNo.set(value); }
    public IntegerProperty lineNoProperty() { return lineNo; }

    public String getItemCode() { return itemCode.get(); }
    public void setItemCode(String value) { itemCode.set(value); }
    public StringProperty itemCodeProperty() { return itemCode; }

    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value); }
    public StringProperty descriptionProperty() { return description; }

    public double getQuantity() { return quantity.get(); }
    public void setQuantity(double value) { quantity.set(value); }
    public DoubleProperty quantityProperty() { return quantity; }

    public String getUnitName() { return unitName.get(); }
    public void setUnitName(String value) { unitName.set(value); }
    public StringProperty unitNameProperty() { return unitName; }

    public double getUnitPrice() { return unitPrice.get(); }
    public void setUnitPrice(double value) { unitPrice.set(value); }
    public DoubleProperty unitPriceProperty() { return unitPrice; }

    public double getDiscountPercent() { return discountPercent.get(); }
    public void setDiscountPercent(double value) { discountPercent.set(value); }
    public DoubleProperty discountPercentProperty() { return discountPercent; }

    public double getTaxPercent() { return taxPercent.get(); }
    public void setTaxPercent(double value) { taxPercent.set(value); }
    public DoubleProperty taxPercentProperty() { return taxPercent; }

    public double getLineTotal() { return lineTotal.get(); }
    public void setLineTotal(double value) { lineTotal.set(value); }
    public DoubleProperty lineTotalProperty() { return lineTotal; }
}