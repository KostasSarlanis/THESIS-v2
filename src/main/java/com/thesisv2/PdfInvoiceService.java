package com.thesisv2;

import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;

public class PdfInvoiceService {

    public void exportInvoiceToPdf(
            File file,
            String invoiceId,
            String invoiceType,
            String invoiceStatus,
            LocalDate issueDate,
            LocalDate dueDate,
            String currencyCode,
            Integer warehouse,

            String sellerName, String sellerAddress, String sellerCity,
            String sellerPostalCode, String sellerCountry, String sellerTaxId,
            String sellerEmail, String sellerPhone,

            String customerName, String customerAddress, String customerCity,
            String customerPostalCode, String customerCountry, String customerTaxId,
            String customerEmail, String customerPhone,

            ObservableList<InvoiceLineModel> lines,
            String subtotal,
            String overallDiscountPercent,
            String discountTotal,
            String taxTotal,
            String grandTotal,
            String notes,
            String paymentTerms
    ) throws Exception {

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        Paragraph title = new Paragraph("INVOICE / ΠΑΡΑΣΤΑΤΙΚΟ", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Invoice ID: " + invoiceId, normalFont));
        document.add(new Paragraph("Type: " + invoiceType, normalFont));
        document.add(new Paragraph("Status: " + invoiceStatus, normalFont));
        document.add(new Paragraph("Issue Date: " + issueDate, normalFont));
        document.add(new Paragraph("Due Date: " + dueDate, normalFont));
        document.add(new Paragraph("Currency: " + currencyCode, normalFont));

        document.add(new Paragraph("Αποθήκη: " + (warehouse == null ? "-" : warehouse)));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Seller / Εκδότης", sectionFont));
        document.add(new Paragraph(sellerName, normalFont));
        document.add(new Paragraph(sellerAddress + ", " + sellerCity + ", " + sellerPostalCode + ", " + sellerCountry, normalFont));
        document.add(new Paragraph("Tax ID: " + sellerTaxId, normalFont));
        document.add(new Paragraph("Email: " + sellerEmail + " | Phone: " + sellerPhone, normalFont));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Customer / Πελάτης", sectionFont));
        document.add(new Paragraph(customerName, normalFont));
        document.add(new Paragraph(customerAddress + ", " + customerCity + ", " + customerPostalCode + ", " + customerCountry, normalFont));
        document.add(new Paragraph("Tax ID: " + customerTaxId, normalFont));
        document.add(new Paragraph("Email: " + customerEmail + " | Phone: " + customerPhone, normalFont));

        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(new float[]{1.0f, 1.8f, 4.0f, 1.4f, 1.4f, 1.4f});
        table.setWidthPercentage(100);

        addHeaderCell(table, "Α/Α");
        addHeaderCell(table, "Κωδικός");
        addHeaderCell(table, "Περιγραφή");
        addHeaderCell(table, "Ποσότητα");
        addHeaderCell(table, "Τιμή");
        addHeaderCell(table, "Σύνολο");

        for (InvoiceLineModel line : lines) {
            table.addCell(String.valueOf(line.getLineNo()));
            table.addCell(nullSafe(line.getItemCode()));
            table.addCell(nullSafe(line.getDescription()));
            table.addCell(String.format("%.3f", line.getQuantity()));
            table.addCell(String.format("%.2f", line.getUnitPrice()));
            table.addCell(String.format("%.2f", line.getLineTotal()));
        }

        document.add(table);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Υποσύνολο / Subtotal: " + subtotal + " " + currencyCode, normalFont));
        document.add(new Paragraph("Συνολική έκπτωση % / Overall Discount %: " + nullSafe(overallDiscountPercent), normalFont));
        document.add(new Paragraph("Σύνολο έκπτωσης / Discount Total: " + discountTotal + " " + currencyCode, normalFont));
        document.add(new Paragraph("Σύνολο έκπτωσης / Discount Total: " + discountTotal + " " + currencyCode, normalFont));
        document.add(new Paragraph("Σύνολο φόρου / Tax Total: " + taxTotal + " " + currencyCode, normalFont));
        document.add(new Paragraph("Γενικό σύνολο / Grand Total: " + grandTotal + " " + currencyCode, sectionFont));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Notes / Σημειώσεις", sectionFont));
        document.add(new Paragraph(nullSafe(notes), normalFont));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Payment Terms / Όροι Πληρωμής", sectionFont));
        document.add(new Paragraph(nullSafe(paymentTerms), normalFont));

        document.close();
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private String nullSafe(String text) {
        return text == null ? "" : text;
    }
}