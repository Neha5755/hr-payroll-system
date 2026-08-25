package com.hrms.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.hrms.entity.Payslip;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/** Generates a single payslip PDF for one employee for one pay period. */
@Service
public class PdfGeneratorService {

    @Value("${app.storage.payslip-dir}")
    private String payslipDir;

    public String generatePayslipPdf(Payslip payslip) throws IOException {
        File dir = new File(payslipDir + "/" + payslip.getPayPeriodYear() + "/"
                + String.format("%02d", payslip.getPayPeriodMonth()));
        if (!dir.exists()) dir.mkdirs();

        String fileName = payslip.getEmployee().getEmployeeCode() + "_"
                + payslip.getPayPeriodYear() + "_" + String.format("%02d", payslip.getPayPeriodMonth()) + ".pdf";
        File file = new File(dir, fileName);

        try (PdfWriter writer = new PdfWriter(new FileOutputStream(file));
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            var employee = payslip.getEmployee();
            String monthName = Month.of(payslip.getPayPeriodMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            // Header
            document.add(new Paragraph("Payslip")
                    .setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Pay Period: " + monthName + " " + payslip.getPayPeriodYear())
                    .setTextAlignment(TextAlignment.CENTER).setFontSize(11).setMarginBottom(15));

            // Employee details table
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth().setMarginBottom(15);
            addInfoRow(infoTable, "Employee ID", employee.getEmployeeCode());
            addInfoRow(infoTable, "Name", employee.getFullName());
            addInfoRow(infoTable, "Department", employee.getDepartment() != null ? employee.getDepartment().getName() : "-");
            addInfoRow(infoTable, "Designation", employee.getDesignation() != null ? employee.getDesignation() : "-");
            document.add(infoTable);

            // Earnings & Deductions side-by-side
            Table earnDeductTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();

            Table earnings = new Table(UnitValue.createPercentArray(new float[]{2, 1})).useAllAvailableWidth();
            earnings.addHeaderCell(headerCell("Earnings"));
            earnings.addHeaderCell(headerCell("Amount (₹)"));
            addRow(earnings, "Basic Salary", payslip.getBasicSalary());
            addRow(earnings, "HRA", payslip.getHra());
            addRow(earnings, "Special Allowance", payslip.getSpecialAllowance());
            addBoldRow(earnings, "Gross Salary", payslip.getGrossSalary());

            Table deductions = new Table(UnitValue.createPercentArray(new float[]{2, 1})).useAllAvailableWidth();
            deductions.addHeaderCell(headerCell("Deductions"));
            deductions.addHeaderCell(headerCell("Amount (₹)"));
            addRow(deductions, "Provident Fund (PF)", payslip.getPfDeduction());
            addRow(deductions, "ESI", payslip.getEsiDeduction());
            addRow(deductions, "Professional Tax", payslip.getProfessionalTax());
            addRow(deductions, "Other Deductions", payslip.getOtherDeductions());
            addBoldRow(deductions, "Total Deductions", payslip.getTotalDeductions());

            earnDeductTable.addCell(new Cell().add(earnings).setBorder(Border.NO_BORDER));
            earnDeductTable.addCell(new Cell().add(deductions).setBorder(Border.NO_BORDER));
            document.add(earnDeductTable);

            // Net pay
            document.add(new Paragraph("Net Pay: ₹" + payslip.getNetSalary())
                    .setBold().setFontSize(14).setMarginTop(15)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY).setPadding(8));

            // Leave summary
            document.add(new Paragraph("Leave Summary").setBold().setFontSize(13).setMarginTop(15));
            Table leaveTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1})).useAllAvailableWidth();
            leaveTable.addHeaderCell(headerCell("Leave Type"));
            leaveTable.addHeaderCell(headerCell("Used (period)"));
            leaveTable.addHeaderCell(headerCell("Balance Remaining"));
            leaveTable.addHeaderCell(headerCell(""));

            leaveTable.addCell(new Cell().add(new Paragraph("Casual Leave (CL)")));
            leaveTable.addCell(new Cell().add(new Paragraph(String.valueOf(payslip.getClUsed()))));
            leaveTable.addCell(new Cell().add(new Paragraph(String.valueOf(payslip.getClRemaining()))));
            leaveTable.addCell(new Cell().add(new Paragraph("")));

            leaveTable.addCell(new Cell().add(new Paragraph("Sick Leave (SL)")));
            leaveTable.addCell(new Cell().add(new Paragraph(String.valueOf(payslip.getSlUsed()))));
            leaveTable.addCell(new Cell().add(new Paragraph(String.valueOf(payslip.getSlRemaining()))));
            leaveTable.addCell(new Cell().add(new Paragraph("")));

            leaveTable.addCell(new Cell().add(new Paragraph("Earned Leave (EL)")));
            leaveTable.addCell(new Cell().add(new Paragraph(String.valueOf(payslip.getElUsed()))));
            leaveTable.addCell(new Cell().add(new Paragraph(String.valueOf(payslip.getElRemaining()))));
            leaveTable.addCell(new Cell().add(new Paragraph("")));

            document.add(leaveTable);

            document.add(new Paragraph("This is a system-generated payslip and does not require a signature.")
                    .setFontSize(8).setItalic().setMarginTop(20).setTextAlignment(TextAlignment.CENTER));
        }

        return file.getAbsolutePath();
    }

    private void addInfoRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(value)).setBorder(Border.NO_BORDER));
    }

    private Cell headerCell(String text) {
        return new Cell().add(new Paragraph(text).setBold())
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setFontColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(0.5f));
    }

    private void addRow(Table table, String label, BigDecimal amount) {
        table.addCell(new Cell().add(new Paragraph(label)));
        table.addCell(new Cell().add(new Paragraph(amount.toString())));
    }

    private void addBoldRow(Table table, String label, BigDecimal amount) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()));
        table.addCell(new Cell().add(new Paragraph(amount.toString()).setBold()));
    }
}
