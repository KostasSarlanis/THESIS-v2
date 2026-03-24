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
        title.setSpacingAfter(10f);
        document.add(title);

        PdfPTable topTable = new PdfPTable(new float[]{3f, 3f, 2.4f});
        topTable.setWidthPercentage(100);
        topTable.setSpacingAfter(12f);

        PdfPCell sellerCell = new PdfPCell();
        sellerCell.setPadding(8f);
        sellerCell.setVerticalAlignment(Element.ALIGN_TOP);
        sellerCell.addElement(new Paragraph("Seller / Εκδότης", sectionFont));
        sellerCell.addElement(new Paragraph(nullSafe(sellerName), normalFont));
        sellerCell.addElement(new Paragraph(
                nullSafe(sellerAddress) + ", " +
                        nullSafe(sellerCity) + ", " +
                        nullSafe(sellerPostalCode) + ", " +
                        nullSafe(sellerCountry), normalFont));
        sellerCell.addElement(new Paragraph("Tax ID: " + nullSafe(sellerTaxId), normalFont));
        sellerCell.addElement(new Paragraph("Phone: " + nullSafe(sellerPhone), normalFont));
        sellerCell.addElement(new Paragraph("Email: " + nullSafe(sellerEmail), normalFont));

        PdfPCell buyerCell = new PdfPCell();
        buyerCell.setPadding(8f);
        buyerCell.setVerticalAlignment(Element.ALIGN_TOP);
        buyerCell.addElement(new Paragraph("Buyer / Πελάτης", sectionFont));
        buyerCell.addElement(new Paragraph(nullSafe(customerName), normalFont));
        buyerCell.addElement(new Paragraph(
                nullSafe(customerAddress) + ", " +
                        nullSafe(customerCity) + ", " +
                        nullSafe(customerPostalCode) + ", " +
                        nullSafe(customerCountry), normalFont));
        buyerCell.addElement(new Paragraph("Tax ID: " + nullSafe(customerTaxId), normalFont));
        buyerCell.addElement(new Paragraph("Phone: " + nullSafe(customerPhone), normalFont));
        buyerCell.addElement(new Paragraph("Email: " + nullSafe(customerEmail), normalFont));

        PdfPCell infoCell = new PdfPCell();
        infoCell.setPadding(8f);
        infoCell.setVerticalAlignment(Element.ALIGN_TOP);
        infoCell.addElement(new Paragraph("Invoice Info", sectionFont));
        infoCell.addElement(new Paragraph("No: " + nullSafe(invoiceId), normalFont));
        infoCell.addElement(new Paragraph("Type: " + nullSafe(invoiceType), normalFont));
        infoCell.addElement(new Paragraph("Status: " + nullSafe(invoiceStatus), normalFont));
        infoCell.addElement(new Paragraph("Issue: " + String.valueOf(issueDate), normalFont));
        infoCell.addElement(new Paragraph("Due: " + String.valueOf(dueDate), normalFont));
        infoCell.addElement(new Paragraph("Warehouse: " + (warehouse == null ? "-" : warehouse), normalFont));
        infoCell.addElement(new Paragraph("Currency: " + nullSafe(currencyCode), normalFont));

        topTable.addCell(sellerCell);
        topTable.addCell(buyerCell);
        topTable.addCell(infoCell);

        document.add(topTable);

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
            table.addCell(nullSafe(String.valueOf(line.getQuantity())));
            table.addCell(String.format("%.2f", line.getUnitPrice()));
            table.addCell(String.format("%.2f", line.getLineTotal()));
        }

        sellerCell.setBorder(Rectangle.BOX);
        buyerCell.setBorder(Rectangle.BOX);
        infoCell.setBorder(Rectangle.BOX);

        document.add(table);

        document.add(new Paragraph(" "));
        PdfPTable totalsTable = new PdfPTable(new float[]{3f, 1.5f});
        totalsTable.setWidthPercentage(42);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setSpacingBefore(10f);
        totalsTable.setSpacingAfter(10f);

        addTotalsRow(totalsTable, "Subtotal", subtotal + " " + currencyCode, normalFont);
        addTotalsRow(totalsTable, "Discount %", nullSafe(overallDiscountPercent), normalFont);
        addTotalsRow(totalsTable, "Discount Total", discountTotal + " " + currencyCode, normalFont);
        addTotalsRow(totalsTable, "Tax Total", taxTotal + " " + currencyCode, normalFont);
        addTotalsRow(totalsTable, "Grand Total", grandTotal + " " + currencyCode, sectionFont);

        document.add(totalsTable);
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Notes / Σημειώσεις", sectionFont));
        document.add(new Paragraph(nullSafe(notes), normalFont));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Payment Terms / Όροι Πληρωμής", sectionFont));
        document.add(new Paragraph(nullSafe(paymentTerms), normalFont));

        document.close();
    }

    private void addTotalsRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell left = new PdfPCell(new Phrase(label, font));
        left.setBorder(Rectangle.NO_BORDER);
        left.setHorizontalAlignment(Element.ALIGN_LEFT);
        left.setPadding(4f);

        PdfPCell right = new PdfPCell(new Phrase(value, font));
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setPadding(4f);

        table.addCell(left);
        table.addCell(right);
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