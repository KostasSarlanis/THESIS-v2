package com.thesisv2;

import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;

public class PdfMovementService {

    public void exportMovementToPdf(
            File file,
            String movementId,
            String movementType,
            LocalDate movementDate,
            Integer sourceWarehouse,
            Integer destinationWarehouse,
            String notes,
            List<MovementLineModel> lines
    ) throws Exception {

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

        Paragraph title = new Paragraph("ΚΙΝΗΣΗ ΑΠΟΘΗΚΗΣ", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10f);
        document.add(title);

        PdfPTable topTable = new PdfPTable(new float[]{3f, 3f, 2.4f});
        topTable.setWidthPercentage(100);
        topTable.setSpacingAfter(12f);

        PdfPCell sourceCell = new PdfPCell();
        sourceCell.setPadding(8f);
        sourceCell.setVerticalAlignment(Element.ALIGN_TOP);
        sourceCell.addElement(new Paragraph("Αποθήκη Προέλευσης", sectionFont));
        sourceCell.addElement(new Paragraph(
                sourceWarehouse == null ? "-" : String.valueOf(sourceWarehouse),
                normalFont
        ));

        PdfPCell destinationCell = new PdfPCell();
        destinationCell.setPadding(8f);
        destinationCell.setVerticalAlignment(Element.ALIGN_TOP);
        destinationCell.addElement(new Paragraph("Αποθήκη Προορισμού", sectionFont));
        destinationCell.addElement(new Paragraph(
                destinationWarehouse == null ? "-" : String.valueOf(destinationWarehouse),
                normalFont
        ));

        PdfPCell infoCell = new PdfPCell();
        infoCell.setPadding(8f);
        infoCell.setVerticalAlignment(Element.ALIGN_TOP);
        infoCell.addElement(new Paragraph("Στοιχεία Κίνησης", sectionFont));
        infoCell.addElement(new Paragraph("No: " + nullSafe(movementId), normalFont));
        infoCell.addElement(new Paragraph("Type: " + nullSafe(movementType), normalFont));
        infoCell.addElement(new Paragraph("Date: " + String.valueOf(movementDate), normalFont));

        topTable.addCell(sourceCell);
        topTable.addCell(destinationCell);
        topTable.addCell(infoCell);

        sourceCell.setBorder(Rectangle.BOX);
        destinationCell.setBorder(Rectangle.BOX);
        infoCell.setBorder(Rectangle.BOX);

        document.add(topTable);

        PdfPTable table = new PdfPTable(new float[]{1.0f, 1.8f, 4.6f, 1.6f});
        table.setWidthPercentage(100);

        addHeaderCell(table, "Α/Α");
        addHeaderCell(table, "Κωδικός");
        addHeaderCell(table, "Περιγραφή");
        addHeaderCell(table, "Ποσότητα");

        int totalQuantity = 0;

        for (MovementLineModel line : lines) {
            if (line == null) {
                continue;
            }

            String productId = nullSafe(line.getProductId()).trim();
            String description = nullSafe(line.getDescription()).trim();

            if (productId.isBlank() && description.isBlank()) {
                continue;
            }

            table.addCell(String.valueOf(line.getLineNo()));
            table.addCell(productId);
            table.addCell(description);
            table.addCell(String.valueOf(line.getQuantity()));

            totalQuantity += line.getQuantity();
        }

        document.add(table);

        document.add(new Paragraph(" "));

        PdfPTable totalsTable = new PdfPTable(new float[]{3f, 1.5f});
        totalsTable.setWidthPercentage(42);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setSpacingBefore(10f);
        totalsTable.setSpacingAfter(10f);

        addTotalsRow(totalsTable, "Συνολική Ποσότητα", String.valueOf(totalQuantity), sectionFont);

        document.add(totalsTable);
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Σχόλια / Notes", sectionFont));
        document.add(new Paragraph(nullSafe(notes), normalFont));

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