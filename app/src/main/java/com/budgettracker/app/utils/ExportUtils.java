package com.budgettracker.app.utils;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.budgettracker.app.data.model.Expense;
import com.budgettracker.app.data.model.Income;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for exporting transaction data to PDF.
 * All files are saved to the app-specific export folder.
 */
public class ExportUtils {

    private static final String EXPORT_FOLDER = "BudgetTracker";

    /**
     * Create a share Intent for an exported file using FileProvider.
     * Call this on the main thread after getting the export path.
     */
    public static Intent createShareIntent(Context context, String filePath, String mimeType) {
        File file = new File(filePath);
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("", uri));
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        return intent;
    }

    /**
     * Export income and expense lists to PDF.
     * @return file path on success, null on failure
     */
    public static String exportToPdf(Context context,
                                     List<Income> incomeList,
                                     List<Expense> expenseList,
                                     String currencySymbol,
                                     String reportTitle) {
        try {
            File exportDir = getExportDir(context);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(new Date());
            File pdfFile = new File(exportDir, "report_" + timestamp + ".pdf");

            PdfWriter writer = new PdfWriter(pdfFile);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            try {
                // Title
                Paragraph title = new Paragraph(reportTitle)
                        .setFontSize(18)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(10);
                document.add(title);

                // Generated date
                Paragraph date = new Paragraph("Generated: " + DateUtils.formatDateFull(System.currentTimeMillis()))
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20);
                document.add(date);

                // Summary
                double totalIncome = incomeList.stream().mapToDouble(Income::getAmount).sum();
                double totalExpense = expenseList.stream().mapToDouble(Expense::getAmount).sum();
                double balance = totalIncome - totalExpense;

                Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                        .useAllAvailableWidth();
                addSummaryCell(summaryTable, "Total Income", CurrencyUtils.format(totalIncome, currencySymbol), new DeviceRgb(76, 175, 80));
                addSummaryCell(summaryTable, "Total Expense", CurrencyUtils.format(totalExpense, currencySymbol), new DeviceRgb(244, 67, 54));
                addSummaryCell(summaryTable, "Balance", CurrencyUtils.format(balance, currencySymbol),
                        balance >= 0 ? new DeviceRgb(33, 150, 243) : new DeviceRgb(255, 87, 34));
                document.add(summaryTable);
                document.add(new Paragraph("\n"));

                // Income Table
                if (!incomeList.isEmpty()) {
                    document.add(new Paragraph("Income Transactions")
                            .setFontSize(14).setBold().setMarginTop(10));
                    Table incomeTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 3}))
                            .useAllAvailableWidth();
                    addHeaderRow(incomeTable, new String[]{"Source", "Amount", "Date", "Notes"});
                    for (Income income : incomeList) {
                        incomeTable.addCell(new Cell().add(new Paragraph(income.getSource())));
                        incomeTable.addCell(new Cell().add(new Paragraph(CurrencyUtils.format(income.getAmount(), currencySymbol))));
                        incomeTable.addCell(new Cell().add(new Paragraph(DateUtils.formatDate(income.getDate()))));
                        incomeTable.addCell(new Cell().add(new Paragraph(income.getNotes() != null ? income.getNotes() : "")));
                    }
                    document.add(incomeTable);
                    document.add(new Paragraph("\n"));
                }

                // Expense Table
                if (!expenseList.isEmpty()) {
                    document.add(new Paragraph("Expense Transactions")
                            .setFontSize(14).setBold().setMarginTop(10));
                    Table expenseTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 3}))
                            .useAllAvailableWidth();
                    addHeaderRow(expenseTable, new String[]{"Category", "Amount", "Date", "Notes"});
                    for (Expense expense : expenseList) {
                        expenseTable.addCell(new Cell().add(new Paragraph(expense.getCategoryName())));
                        expenseTable.addCell(new Cell().add(new Paragraph(CurrencyUtils.format(expense.getAmount(), currencySymbol))));
                        expenseTable.addCell(new Cell().add(new Paragraph(DateUtils.formatDate(expense.getDate()))));
                        expenseTable.addCell(new Cell().add(new Paragraph(expense.getNotes() != null ? expense.getNotes() : "")));
                    }
                    document.add(expenseTable);
                }

                document.close();
                pdfDoc.close();
                return pdfFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static File getExportDir(Context context) {
        // App-specific external storage — no WRITE_EXTERNAL_STORAGE permission needed on any Android version
        File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            // Fallback to internal storage if external not available
            dir = context.getFilesDir();
        }
        File exportDir = new File(dir, EXPORT_FOLDER);
        if (!exportDir.exists()) exportDir.mkdirs();
        return exportDir;
    }

    private static void addHeaderRow(Table table, String[] headers) {
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setBold())
                    .setBackgroundColor(new DeviceRgb(33, 150, 243))
                    .setFontColor(ColorConstants.WHITE));
        }
    }

    private static void addSummaryCell(Table table, String label, String value, DeviceRgb color) {
        Cell cell = new Cell()
                .add(new Paragraph(label).setFontSize(10).setFontColor(ColorConstants.WHITE))
                .add(new Paragraph(value).setFontSize(12).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(color)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(10);
        table.addCell(cell);
    }

}
