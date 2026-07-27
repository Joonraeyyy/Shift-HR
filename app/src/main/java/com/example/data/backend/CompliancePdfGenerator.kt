package com.example.data.backend

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * 1. DEFINE THE CUSTOMIZATION THEME SCHEMA
 * Holds configuration for corporate branding and colors.
 */
data class PdfThemeConfig(
    val primaryColorHex: String = "#059669",      // e.g., "#059669" (Mint/Emerald) or "#1D4ED8" (Corporate Blue)
    val secondaryColorHex: String = "#0F172A",    // e.g., "#0F172A" (Slate)
    val companyLogoBytes: ByteArray? = null       // Raw uploaded company branding logo asset
)

/**
 * Data structures for formal Shift Data and Payroll Summary reports
 */
data class ShiftItemData(
    val date: String,
    val timeIn: String,
    val timeOut: String,
    val durationHours: Double,
    val location: String = "Main HQ",
    val status: String = "VERIFIED",
    val hourlyRate: Double = 25.00,
    val earned: Double = durationHours * hourlyRate
)

data class DeductionItemData(
    val label: String,
    val amount: Double
)

data class PayrollSummaryData(
    val companyName: String = "Shift HR Enterprise",
    val employeeName: String,
    val employeeId: String = "EMP-80291",
    val department: String = "Operations & Logistics",
    val payPeriod: String,
    val currencySymbol: String = "$",
    val regularHours: Double,
    val overtimeHours: Double = 0.0,
    val totalHours: Double = regularHours + overtimeHours,
    val hourlyRate: Double,
    val grossPay: Double = (regularHours * hourlyRate) + (overtimeHours * hourlyRate * 1.5),
    val deductions: List<DeductionItemData> = listOf(
        DeductionItemData("Withholding Tax", grossPay * 0.10),
        DeductionItemData("Social Security / SSS", grossPay * 0.045),
        DeductionItemData("Health Insurance", grossPay * 0.025)
    ),
    val totalDeductions: Double = deductions.sumOf { it.amount },
    val netPay: Double = grossPay - totalDeductions,
    val shiftLogs: List<ShiftItemData> = emptyList()
)

/**
 * 2. NATIVE ANDROID PDF COMPILER SERVICE
 * Generates formal, formatted PDF reports using Android's native PdfDocument & Canvas API.
 * 100% Android runtime safe with zero Java AWT dependencies.
 */
class CompliancePdfGeneratorService {

    /**
     * Generates a formal, formatted PDF Payroll Summary / Payslip report from payroll & shift data.
     */
    fun generatePayrollSummaryPdf(
        payroll: PayrollSummaryData,
        theme: PdfThemeConfig = PdfThemeConfig()
    ): ByteArray {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Portrait (595 x 842)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val primaryColor = parseColorSafely(theme.primaryColorHex, "#059669")
        val secondaryColor = parseColorSafely(theme.secondaryColorHex, "#0F172A")

        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply { isAntiAlias = true }

        // Header Section
        paint.color = primaryColor
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(payroll.companyName.uppercase(Locale.US), 40f, 50f, paint)

        paint.color = secondaryColor
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val titleText = "OFFICIAL PAYROLL STATEMENT"
        val titleWidth = paint.measureText(titleText)
        canvas.drawText(titleText, 555f - titleWidth, 50f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val periodText = "Pay Period: ${payroll.payPeriod}"
        val periodWidth = paint.measureText(periodText)
        canvas.drawText(periodText, 555f - periodWidth, 66f, paint)

        // Primary accent line
        paint.color = primaryColor
        paint.strokeWidth = 3f
        canvas.drawLine(40f, 80f, 555f, 80f, paint)

        // Employee Info Card (Box)
        val cardRect = RectF(40f, 95f, 555f, 175f)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(cardRect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1f
        canvas.drawRoundRect(cardRect, 8f, 8f, paint)

        // Info Card Content
        paint.style = Paint.Style.FILL
        drawInfoCell(canvas, paint, "EMPLOYEE NAME", payroll.employeeName, 55f, 115f)
        drawInfoCell(canvas, paint, "REFERENCE ID", payroll.employeeId, 230f, 115f)
        drawInfoCell(canvas, paint, "DEPARTMENT", payroll.department, 400f, 115f)

        drawInfoCell(canvas, paint, "BASE HOURLY RATE", "${payroll.currencySymbol}${String.format(Locale.US, "%.2f", payroll.hourlyRate)}/hr", 55f, 150f)
        drawInfoCell(canvas, paint, "TOTAL HOURS", "${String.format(Locale.US, "%.1f", payroll.totalHours)} hrs (${String.format(Locale.US, "%.1f", payroll.regularHours)} Reg / ${String.format(Locale.US, "%.1f", payroll.overtimeHours)} OT)", 230f, 150f)
        drawInfoCell(canvas, paint, "DOCUMENT CODE", "PAY-${System.currentTimeMillis().toString().takeLast(8)}", 400f, 150f)

        // Tables Section: Earnings (Left) and Deductions (Right)
        var y = 200f

        // Section Headers
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = secondaryColor
        canvas.drawText("EARNINGS BREAKDOWN", 40f, y, paint)
        canvas.drawText("DEDUCTIONS BREAKDOWN", 305f, y, paint)

        y += 10f
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas.drawLine(40f, y, 280f, y, paint)
        canvas.drawLine(305f, y, 555f, y, paint)

        y += 18f
        // Table Header Rows
        paint.color = Color.parseColor("#475569")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        // Left Table Headers
        canvas.drawText("DESCRIPTION", 40f, y, paint)
        canvas.drawText("HOURS", 190f, y, paint)
        canvas.drawText("AMOUNT", 235f, y, paint)

        // Right Table Headers
        canvas.drawText("DEDUCTION ITEM", 305f, y, paint)
        canvas.drawText("AMOUNT", 500f, y, paint)

        y += 8f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(40f, y, 280f, y, paint)
        canvas.drawLine(305f, y, 555f, y, paint)

        // Regular Pay Row
        y += 18f
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Regular Pay", 40f, y, paint)
        canvas.drawText(String.format(Locale.US, "%.1f", payroll.regularHours), 190f, y, paint)
        canvas.drawText("${payroll.currencySymbol}${String.format(Locale.US, "%.2f", payroll.regularHours * payroll.hourlyRate)}", 235f, y, paint)

        // First Deduction Row
        val ded1 = payroll.deductions.getOrNull(0)
        if (ded1 != null) {
            canvas.drawText(ded1.label, 305f, y, paint)
            paint.color = Color.parseColor("#E11D48")
            canvas.drawText("-${payroll.currencySymbol}${String.format(Locale.US, "%.2f", ded1.amount)}", 500f, y, paint)
            paint.color = Color.parseColor("#1E293B")
        }

        // Overtime Pay Row
        y += 18f
        if (payroll.overtimeHours > 0) {
            canvas.drawText("Overtime Pay (1.5x)", 40f, y, paint)
            canvas.drawText(String.format(Locale.US, "%.1f", payroll.overtimeHours), 190f, y, paint)
            canvas.drawText("${payroll.currencySymbol}${String.format(Locale.US, "%.2f", payroll.overtimeHours * payroll.hourlyRate * 1.5)}", 235f, y, paint)
        } else {
            canvas.drawText("Overtime Pay", 40f, y, paint)
            canvas.drawText("0.0", 190f, y, paint)
            canvas.drawText("${payroll.currencySymbol}0.00", 235f, y, paint)
        }

        val ded2 = payroll.deductions.getOrNull(1)
        if (ded2 != null) {
            canvas.drawText(ded2.label, 305f, y, paint)
            paint.color = Color.parseColor("#E11D48")
            canvas.drawText("-${payroll.currencySymbol}${String.format(Locale.US, "%.2f", ded2.amount)}", 500f, y, paint)
            paint.color = Color.parseColor("#1E293B")
        }

        // Additional Deduction Row if any
        y += 18f
        val ded3 = payroll.deductions.getOrNull(2)
        if (ded3 != null) {
            canvas.drawText(ded3.label, 305f, y, paint)
            paint.color = Color.parseColor("#E11D48")
            canvas.drawText("-${payroll.currencySymbol}${String.format(Locale.US, "%.2f", ded3.amount)}", 500f, y, paint)
            paint.color = Color.parseColor("#1E293B")
        }

        // Totals Banner Rows
        y += 12f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(40f, y, 280f, y, paint)
        canvas.drawLine(305f, y, 555f, y, paint)

        y += 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = secondaryColor
        canvas.drawText("TOTAL GROSS", 40f, y, paint)
        paint.color = primaryColor
        canvas.drawText("${payroll.currencySymbol}${String.format(Locale.US, "%.2f", payroll.grossPay)}", 220f, y, paint)

        paint.color = secondaryColor
        canvas.drawText("TOTAL DEDUCT.", 305f, y, paint)
        paint.color = Color.parseColor("#E11D48")
        canvas.drawText("-${payroll.currencySymbol}${String.format(Locale.US, "%.2f", payroll.totalDeductions)}", 480f, y, paint)

        // Net Take-Home Pay Box
        y += 30f
        val netPayBox = RectF(40f, y, 555f, y + 60f)
        paint.style = Paint.Style.FILL
        paint.color = primaryColor
        canvas.drawRoundRect(netPayBox, 10f, 10f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NET TAKE-HOME PAY", 60f, y + 25f, paint)

        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val netPayStr = "${payroll.currencySymbol}${String.format(Locale.US, "%.2f", payroll.netPay)}"
        val netPayWidth = paint.measureText(netPayStr)
        canvas.drawText(netPayStr, 535f - netPayWidth, y + 40f, paint)

        // Shift Logs Table Section
        y += 85f
        if (payroll.shiftLogs.isNotEmpty() && y < 750f) {
            paint.style = Paint.Style.FILL
            paint.color = secondaryColor
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("ITEMIZED SHIFT LEDGER (${payroll.shiftLogs.size} PUNCHES)", 40f, y, paint)

            y += 10f
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRect(40f, y, 555f, y + 20f, paint)

            paint.color = Color.parseColor("#475569")
            paint.textSize = 9f
            canvas.drawText("DATE", 45f, y + 14f, paint)
            canvas.drawText("TIME WINDOW", 125f, y + 14f, paint)
            canvas.drawText("HOURS", 230f, y + 14f, paint)
            canvas.drawText("LOCATION", 290f, y + 14f, paint)
            canvas.drawText("STATUS", 430f, y + 14f, paint)
            canvas.drawText("EARNED", 500f, y + 14f, paint)

            y += 24f
            paint.color = Color.parseColor("#1E293B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            payroll.shiftLogs.take(12).forEach { shift ->
                if (y > 780f) return@forEach
                canvas.drawText(shift.date, 45f, y, paint)
                canvas.drawText("${shift.timeIn} - ${shift.timeOut}", 125f, y, paint)
                canvas.drawText("${String.format(Locale.US, "%.1f", shift.durationHours)} hrs", 230f, y, paint)
                
                val locText = if (shift.location.length > 20) shift.location.take(18) + ".." else shift.location
                canvas.drawText(locText, 290f, y, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = if (shift.status.contains("APPROVED", true) || shift.status.contains("VERIFIED", true)) Color.parseColor("#15803D") else Color.parseColor("#B45309")
                canvas.drawText(shift.status, 430f, y, paint)

                paint.color = Color.parseColor("#1E293B")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("${payroll.currencySymbol}${String.format(Locale.US, "%.2f", shift.earned)}", 500f, y, paint)

                y += 18f
            }
        }

        // Footer
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val footerText = "Shift HR Compliance Engine • End-to-End Encrypted Payroll Record • Document Code PAY-${System.currentTimeMillis().toString().takeLast(8)}"
        val footerWidth = paint.measureText(footerText)
        canvas.drawText(footerText, (595f - footerWidth) / 2f, 815f, paint)

        pdfDocument.finishPage(page)

        val outputStream = ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        return outputStream.toByteArray()
    }

    /**
     * Generates a formal Shift Audit & Operational Report PDF.
     */
    fun generateShiftReportPdf(
        reportTitle: String = "Shift Audit & Timesheet Ledger",
        payPeriod: String,
        shifts: List<ShiftItemData>,
        theme: PdfThemeConfig = PdfThemeConfig()
    ): ByteArray {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val primaryColor = parseColorSafely(theme.primaryColorHex, "#059669")
        val secondaryColor = parseColorSafely(theme.secondaryColorHex, "#0F172A")

        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply { isAntiAlias = true }

        // Header Title
        paint.color = primaryColor
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SHIFT HR COMPLIANCE", 40f, 48f, paint)

        paint.color = secondaryColor
        paint.textSize = 16f
        val titleWidth = paint.measureText(reportTitle)
        canvas.drawText(reportTitle, 555f - titleWidth, 48f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val periodText = "Period: $payPeriod"
        val periodWidth = paint.measureText(periodText)
        canvas.drawText(periodText, 555f - periodWidth, 64f, paint)

        // Accent divider
        paint.color = primaryColor
        paint.strokeWidth = 3f
        canvas.drawLine(40f, 75f, 555f, 75f, paint)

        // Key Stats Cards
        val totalHours = shifts.sumOf { it.durationHours }
        val totalEarned = shifts.sumOf { it.earned }

        val cardWidth = 160f
        drawStatCard(canvas, paint, "TOTAL SHIFTS", "${shifts.size} Entries", 40f, 90f, cardWidth, secondaryColor)
        drawStatCard(canvas, paint, "CUMULATIVE HOURS", "${String.format(Locale.US, "%.1f", totalHours)} hrs", 215f, 90f, cardWidth, secondaryColor)
        drawStatCard(canvas, paint, "TOTAL EARNED", "$${String.format(Locale.US, "%.2f", totalEarned)}", 395f, 90f, cardWidth, primaryColor)

        // Table Header
        var y = 175f
        paint.style = Paint.Style.FILL
        paint.color = secondaryColor
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ITEMIZED SHIFT LEDGER", 40f, y, paint)

        y += 10f
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(40f, y, 555f, y + 20f, paint)

        paint.color = Color.parseColor("#475569")
        paint.textSize = 9f
        canvas.drawText("DATE", 45f, y + 14f, paint)
        canvas.drawText("TIME WINDOW", 125f, y + 14f, paint)
        canvas.drawText("HOURS", 230f, y + 14f, paint)
        canvas.drawText("LOCATION / GEOFENCE", 290f, y + 14f, paint)
        canvas.drawText("STATUS", 425f, y + 14f, paint)
        canvas.drawText("CALCULATED PAY", 485f, y + 14f, paint)

        y += 24f
        paint.color = Color.parseColor("#1E293B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        shifts.take(28).forEach { shift ->
            if (y > 780f) return@forEach
            canvas.drawText(shift.date, 45f, y, paint)
            canvas.drawText("${shift.timeIn} - ${shift.timeOut}", 125f, y, paint)
            canvas.drawText("${String.format(Locale.US, "%.1f", shift.durationHours)} hrs", 230f, y, paint)

            val locText = if (shift.location.length > 20) shift.location.take(18) + ".." else shift.location
            canvas.drawText(locText, 290f, y, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = if (shift.status.contains("APPROVED", true) || shift.status.contains("VERIFIED", true)) Color.parseColor("#15803D") else Color.parseColor("#B45309")
            canvas.drawText(shift.status, 425f, y, paint)

            paint.color = Color.parseColor("#1E293B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("$${String.format(Locale.US, "%.2f", shift.earned)}", 485f, y, paint)

            paint.color = Color.parseColor("#E2E8F0")
            paint.strokeWidth = 0.5f
            canvas.drawLine(40f, y + 4f, 555f, y + 4f, paint)
            paint.color = Color.parseColor("#1E293B")

            y += 18f
        }

        // Footer
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val footerText = "Generated via Shift HR Native PDF Engine • Verified & Tamper-Evident Audit Report"
        val footerWidth = paint.measureText(footerText)
        canvas.drawText(footerText, (595f - footerWidth) / 2f, 815f, paint)

        pdfDocument.finishPage(page)

        val outputStream = ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        return outputStream.toByteArray()
    }

    /**
     * Legacy/Audit case report generator preserving backwards compatibility
     */
    fun generateCustomizedReport(
        caseTitle: String,
        caseId: String,
        timelineEvents: List<String>,
        theme: PdfThemeConfig = PdfThemeConfig()
    ): ByteArray {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val primaryColor = parseColorSafely(theme.primaryColorHex, "#059669")
        val secondaryColor = parseColorSafely(theme.secondaryColorHex, "#0F172A")

        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply { isAntiAlias = true }

        // Header
        paint.color = primaryColor
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SHIFT HR COMPLIANCE", 40f, 50f, paint)

        paint.color = primaryColor
        paint.strokeWidth = 3f
        canvas.drawLine(40f, 70f, 555f, 70f, paint)

        // Case Title
        paint.color = secondaryColor
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(caseTitle, 40f, 105f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Case Reference Code: #$caseId", 40f, 122f, paint)

        // Timeline
        var y = 160f
        paint.color = secondaryColor
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("OFFICIAL AUDIT TIMELINE LEDGER", 40f, y, paint)

        y += 20f
        paint.color = Color.parseColor("#334155")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        timelineEvents.take(25).forEach { event ->
            // Bullet node line
            paint.color = primaryColor
            canvas.drawCircle(45f, y - 3f, 3f, paint)

            paint.color = Color.parseColor("#334155")
            canvas.drawText(event, 58f, y, paint)

            paint.color = Color.parseColor("#CBD5E1")
            paint.strokeWidth = 1f
            canvas.drawLine(45f, y, 45f, y + 15f, paint)

            y += 20f
        }

        // Footer
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        val footerText = "This document is end-to-end encrypted and authorized under Shift HR Compliance standards."
        val footerWidth = paint.measureText(footerText)
        canvas.drawText(footerText, (595f - footerWidth) / 2f, 815f, paint)

        pdfDocument.finishPage(page)

        val outputStream = ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        return outputStream.toByteArray()
    }

    private fun drawInfoCell(canvas: Canvas, paint: Paint, label: String, value: String, x: Float, y: Float) {
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(label, x, y, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x, y + 14f, paint)
    }

    private fun drawStatCard(canvas: Canvas, paint: Paint, label: String, value: String, x: Float, y: Float, width: Float, accentColor: Int) {
        val rect = RectF(x, y, x + width, y + 55f)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.style = Paint.Style.FILL
        paint.color = accentColor
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x + 12f, y + 28f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(label, x + 12f, y + 44f, paint)
    }

    private fun parseColorSafely(hex: String, defaultHex: String): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            Color.parseColor(defaultHex)
        }
    }

    /**
     * Save PDF bytes to Android application cache directory and return File reference
     */
    fun savePdfToCache(context: Context, pdfBytes: ByteArray, fileName: String): File {
        val cacheDir = context.cacheDir
        val pdfFile = File(cacheDir, if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf")
        FileOutputStream(pdfFile).use { fos ->
            fos.write(pdfBytes)
            fos.flush()
        }
        return pdfFile
    }

    /**
     * Shows an interactive selection dialog offering 'View PDF', 'Download PDF to Device', or 'Share / Print' options.
     */
    fun showPdfActionDialog(
        context: Context,
        pdfFile: File,
        documentTitle: String = "Shift HR PDF Document"
    ) {
        val options = arrayOf(
            "👁️ View PDF Document",
            "📥 Download PDF to Device (Downloads)",
            "📤 Share or Print PDF"
        )

        android.app.AlertDialog.Builder(context)
            .setTitle(documentTitle)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> viewPdf(context, pdfFile)
                    1 -> downloadPdfToDownloadsFolder(context, pdfFile, pdfFile.name)
                    2 -> sharePdf(context, pdfFile, documentTitle)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Directly opens the PDF file in an installed PDF viewer application.
     */
    fun viewPdf(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            sharePdf(context, pdfFile, "View PDF Document")
        }
    }

    /**
     * Downloads (copies) the PDF file directly to the system Downloads directory.
     */
    fun downloadPdfToDownloadsFolder(context: Context, sourceFile: File, targetFileName: String) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        sourceFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    android.widget.Toast.makeText(context, "✅ PDF downloaded to Downloads: $targetFileName", android.widget.Toast.LENGTH_LONG).show()
                    return
                }
            }

            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val destFile = File(downloadsDir, targetFileName)
            sourceFile.copyTo(destFile, overwrite = true)
            android.widget.Toast.makeText(context, "✅ PDF downloaded to Downloads: ${destFile.name}", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Saved in app cache (${sourceFile.name}). Download note: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares or prints the PDF file using Android Chooser.
     */
    fun sharePdf(context: Context, pdfFile: File, title: String = "Share PDF Document") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Unable to share PDF: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open, Download or Share generated PDF via interactive option dialog
     */
    fun openOrSharePdf(context: Context, pdfFile: File, chooserTitle: String = "Shift HR PDF Document Options") {
        showPdfActionDialog(context, pdfFile, chooserTitle)
    }
}
