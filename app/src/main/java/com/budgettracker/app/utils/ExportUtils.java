package com.budgettracker.app.utils;

import android.content.Context;
import android.os.Environment;

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

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for exporting transaction data to PDF and Excel.
 * All files saved to Downloads/BudgetTracker/ folder.
 */
public class ExportUtils {

    private static final String EXPORT_FOLDER = "BudgetTracker";

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
            File exportDir = getExportDir();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(new Date());
            File pdfFile = new File(exportDir, "report_" + timestamp + ".pdf");

            PdfWriter writer = new PdfWriter(pdfFile);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

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
                Table expenseTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 3}))
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
            return pdfFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Export income and expenses to Excel (.xlsx).
     */
    public static String exportToExcel(Context context,
                                       List<Income> incomeList,
                                       List<Expense> expenseList,
                                       String currencySymbol) {
        try {
            File exportDir = getExportDir();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(new Date());
            File excelFile = new File(exportDir, "report_" + timestamp + ".xlsx");

            Workbook workbook = new XSSFWorkbook();

            // Income sheet
            Sheet incomeSheet = workbook.createSheet("Income");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = incomeSheet.createRow(0);
            String[] incomeHeaders = {"ID", "Source", "Amount", "Date", "Notes"};
            for (int i = 0; i < incomeHeaders.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(incomeHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowNum = 1;
            for (Income income : incomeList) {
                Row row = incomeSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(income.getId());
                row.createCell(1).setCellValue(income.getSource());
                row.createCell(2).setCellValue(income.getAmount());
                row.createCell(3).setCellValue(DateUtils.formatDate(income.getDate()));
                row.createCell(4).setCellValue(income.getNotes() != null ? income.getNotes() : "");
            }
            for (int i = 0; i < incomeHeaders.length; i++) incomeSheet.autoSizeColumn(i);

            // Expense sheet
            Sheet expenseSheet = workbook.createSheet("Expenses");
            Row expHeaderRow = expenseSheet.createRow(0);
            String[] expHeaders = {"ID", "Category", "Amount", "Date", "Notes"};
            for (int i = 0; i < expHeaders.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = expHeaderRow.createCell(i);
                cell.setCellValue(expHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            int expRowNum = 1;
            for (Expense expense : expenseList) {
                Row row = expenseSheet.createRow(expRowNum++);
                row.createCell(0).setCellValue(expense.getId());
                row.createCell(1).setCellValue(expense.getCategoryName());
                row.createCell(2).setCellValue(expense.getAmount());
                row.createCell(3).setCellValue(DateUtils.formatDate(expense.getDate()));
                row.createCell(4).setCellValue(expense.getNotes() != null ? expense.getNotes() : "");
            }
            for (int i = 0; i < expHeaders.length; i++) expenseSheet.autoSizeColumn(i);

            // Summary sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            double totalIncome = incomeList.stream().mapToDouble(Income::getAmount).sum();
            double totalExpense = expenseList.stream().mapToDouble(Expense::getAmount).sum();
            summarySheet.createRow(0).createCell(0).setCellValue("Budget Tracker Report");
            summarySheet.createRow(1).createCell(0).setCellValue("Total Income: " + CurrencyUtils.format(totalIncome, currencySymbol));
            summarySheet.createRow(2).createCell(0).setCellValue("Total Expense: " + CurrencyUtils.format(totalExpense, currencySymbol));
            summarySheet.createRow(3).createCell(0).setCellValue("Balance: " + CurrencyUtils.format(totalIncome - totalExpense, currencySymbol));

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
            workbook.close();
            return excelFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static File getExportDir() {
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                EXPORT_FOLDER);
        if (!dir.exists()) dir.mkdirs();
        return dir;
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

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
