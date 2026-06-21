package com.dashboard.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.dashboard.db.DatabaseManager;

import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.time.LocalDate;

/**
 * Generates a UCC-style result slip PDF for a student.
 */
public class PdfExporter {

    private static final Font TITLE_FONT =
            new Font(Font.FontFamily.HELVETICA, 16,
                    Font.BOLD, BaseColor.WHITE);
    private static final Font HEADER_FONT =
            new Font(Font.FontFamily.HELVETICA, 11,
                    Font.BOLD);
    private static final Font NORMAL_FONT =
            new Font(Font.FontFamily.HELVETICA, 10);
    private static final Font SMALL_FONT =
            new Font(Font.FontFamily.HELVETICA, 9,
                    Font.NORMAL, BaseColor.GRAY);

    private static final BaseColor UCC_BLUE =
            new BaseColor(26, 31, 54);
    private static final BaseColor UCC_GREEN =
            new BaseColor(46, 204, 113);
    private static final BaseColor TABLE_HEADER =
            new BaseColor(240, 242, 245);

    /**
     * Exports a student result slip to the given file path.
     *
     * @param studentId  the student's DB id
     * @param outputPath full path e.g. "/Users/paul/result_slip.pdf"
     */
    public static void exportResultSlip(
            int studentId, String outputPath)
            throws Exception {

        DatabaseManager db = DatabaseManager.getInstance();
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc,
                new FileOutputStream(outputPath));
        doc.open();

        // ── Header Banner ─────────────────────────────────────────
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(UCC_BLUE);
        headerCell.setPadding(16);
        headerCell.setBorder(Rectangle.NO_BORDER);

        Paragraph ucc = new Paragraph(
                "UNIVERSITY OF CAPE COAST", TITLE_FONT);
        ucc.setAlignment(Element.ALIGN_CENTER);
        Paragraph sub = new Paragraph(
                "School of Physical Sciences — "
                        + "Department of Computer Science",
                new Font(Font.FontFamily.HELVETICA, 10,
                        Font.NORMAL, BaseColor.LIGHT_GRAY));
        sub.setAlignment(Element.ALIGN_CENTER);
        Paragraph slipTitle = new Paragraph(
                "STUDENT RESULT SLIP",
                new Font(Font.FontFamily.HELVETICA, 13,
                        Font.BOLD, UCC_GREEN));
        slipTitle.setAlignment(Element.ALIGN_CENTER);

        headerCell.addElement(ucc);
        headerCell.addElement(sub);
        headerCell.addElement(slipTitle);
        headerTable.addCell(headerCell);
        doc.add(headerTable);
        doc.add(Chunk.NEWLINE);

        // ── Student Info ──────────────────────────────────────────
        ResultSet student = db.getStudentByEmail(
                SessionManager.getInstance().getEmail());

        if (student != null && student.next()) {
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(8);

            addInfoCell(infoTable, "Name:",
                    student.getString("name"));
            addInfoCell(infoTable, "Student No:",
                    student.getString("student_number"));
            addInfoCell(infoTable, "Programme:",
                    "BSc. " + student.getString("major"));
            addInfoCell(infoTable, "Year:",
                    "Year " + student.getInt("year"));
            addInfoCell(infoTable, "Semester:",
                    "2025/2026 Second Semester");
            addInfoCell(infoTable, "Date Generated:",
                    LocalDate.now().toString());

            doc.add(infoTable);
        }

        doc.add(Chunk.NEWLINE);

        // ── Results Table ─────────────────────────────────────────
        Paragraph resultsTitle = new Paragraph(
                "COURSE RESULTS", HEADER_FONT);
        resultsTitle.setSpacingBefore(8);
        resultsTitle.setSpacingAfter(6);
        doc.add(resultsTitle);

        PdfPTable resultsTable = new PdfPTable(6);
        resultsTable.setWidthPercentage(100);
        resultsTable.setWidths(
                new float[]{1.2f, 3f, 1f, 1.2f, 1.2f, 1.5f});

        // table headers
        String[] headers = {
                "Code", "Course Name", "Credits",
                "Score", "Grade", "Points"
        };
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(
                    new Phrase(h, HEADER_FONT));
            cell.setBackgroundColor(TABLE_HEADER);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            resultsTable.addCell(cell);
        }

        // results rows
        double totalPoints = 0;
        int    count       = 0;
        double highest     = -1;
        double lowest      = 101;

        ResultSet grades = db.getTranscriptForStudent(studentId);
        boolean hasGrades = false;

        while (grades.next()) {
            hasGrades = true;
            double score  = grades.getDouble("score");
            double points = grades.getDouble("grade_points");
            totalPoints  += points;
            count++;
            if (score > highest) highest = score;
            if (score < lowest)  lowest  = score;

            boolean isPass = score >= 50;
            BaseColor rowColor = isPass
                    ? BaseColor.WHITE
                    : new BaseColor(255, 235, 235);

            addResultRow(resultsTable,
                    grades.getString("code"),
                    grades.getString("name"),
                    String.valueOf(grades.getInt("credits")),
                    String.format("%.1f", score),
                    grades.getString("letter_grade"),
                    String.format("%.1f", points),
                    rowColor);
        }

        if (!hasGrades) {
            PdfPCell empty = new PdfPCell(
                    new Phrase("No grades recorded yet.",
                            SMALL_FONT));
            empty.setColspan(6);
            empty.setPadding(10);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            resultsTable.addCell(empty);
        }

        doc.add(resultsTable);
        doc.add(Chunk.NEWLINE);

        // ── GPA Summary ───────────────────────────────────────────
        if (count > 0) {
            double cgpa = Math.round(
                    (totalPoints / count) * 100.0) / 100.0;
            String remark = cgpa >= 3.7 ? "First Class"
                    : cgpa >= 3.0 ? "Second Class Upper"
                    : cgpa >= 2.0 ? "Second Class Lower"
                    : cgpa >= 1.0 ? "Third Class" : "Fail";

            PdfPTable gpaTable = new PdfPTable(4);
            gpaTable.setWidthPercentage(100);
            gpaTable.setSpacingBefore(4);

            addInfoCell(gpaTable, "Courses Graded:",
                    String.valueOf(count));
            addInfoCell(gpaTable, "Highest Score:",
                    String.format("%.1f", highest));
            addInfoCell(gpaTable, "Lowest Score:",
                    String.format("%.1f", lowest));
            addInfoCell(gpaTable, "Cumulative GPA:",
                    String.format("%.2f — %s", cgpa, remark));

            doc.add(gpaTable);
        }

        // ── Footer ────────────────────────────────────────────────
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);

        Paragraph footer = new Paragraph(
                "This is a computer-generated document. "
                        + "University of Cape Coast — "
                        + LocalDate.now(),
                SMALL_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
        System.out.println("✔ Result slip exported: "
                + outputPath);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private static void addInfoCell(PdfPTable table,
                                    String label, String value) {
        PdfPCell labelCell = new PdfPCell(
                new Phrase(label, SMALL_FONT));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setPadding(6);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(
                new Phrase(value != null ? value : "--",
                        NORMAL_FONT));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setPadding(6);
        table.addCell(valueCell);
    }

    private static void addResultRow(PdfPTable table,
                                     String code, String name, String credits,
                                     String score, String grade, String points,
                                     BaseColor bgColor) {

        String[] values = {
                code, name, credits, score, grade, points
        };
        for (int i = 0; i < values.length; i++) {
            PdfPCell cell = new PdfPCell(
                    new Phrase(values[i], NORMAL_FONT));
            cell.setBackgroundColor(bgColor);
            cell.setPadding(7);
            cell.setHorizontalAlignment(
                    i == 1 ? Element.ALIGN_LEFT
                            : Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }
}