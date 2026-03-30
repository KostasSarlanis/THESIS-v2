package com.thesisv2;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

public class InvoiceListRow {

    private final IntegerProperty invoiceId = new SimpleIntegerProperty();
    private final StringProperty invoiceType = new SimpleStringProperty("");
    private final StringProperty customerName = new SimpleStringProperty("");
    private final StringProperty customerTaxId = new SimpleStringProperty("");
    private final StringProperty grandTotal = new SimpleStringProperty("");
    private final StringProperty notes = new SimpleStringProperty("");
    private final StringProperty paymentTerms = new SimpleStringProperty("");

    public InvoiceListRow(int invoiceId,
                          String invoiceType,
                          String customerName,
                          String customerTaxId,
                          String grandTotal,
                          String notes,
                          String paymentTerms) {
        setInvoiceId(invoiceId);
        setInvoiceType(invoiceType);
        setCustomerName(customerName);
        setCustomerTaxId(customerTaxId);
        setGrandTotal(grandTotal);
        setNotes(notes);
        setPaymentTerms(paymentTerms);
    }

    public int getInvoiceId() {
        return invoiceId.get();
    }

    public void setInvoiceId(int value) {
        invoiceId.set(value);
    }

    public IntegerProperty invoiceIdProperty() {
        return invoiceId;
    }

    public String getInvoiceType() {
        return invoiceType.get();
    }

    public void setInvoiceType(String value) {
        invoiceType.set(value == null ? "" : value);
    }

    public StringProperty invoiceTypeProperty() {
        return invoiceType;
    }

    public String getCustomerName() {
        return customerName.get();
    }

    public void setCustomerName(String value) {
        customerName.set(value == null ? "" : value);
    }

    public StringProperty customerNameProperty() {
        return customerName;
    }

    public String getCustomerTaxId() {
        return customerTaxId.get();
    }

    public void setCustomerTaxId(String value) {
        customerTaxId.set(value == null ? "" : value);
    }

    public StringProperty customerTaxIdProperty() {
        return customerTaxId;
    }

    public String getGrandTotal() {
        return grandTotal.get();
    }

    public void setGrandTotal(String value) {
        grandTotal.set(value == null ? "" : value);
    }

    public StringProperty grandTotalProperty() {
        return grandTotal;
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

    public String getPaymentTerms() {
        return paymentTerms.get();
    }

    public void setPaymentTerms(String value) {
        paymentTerms.set(value == null ? "" : value);
    }

    public StringProperty paymentTermsProperty() {
        return paymentTerms;
    }
}