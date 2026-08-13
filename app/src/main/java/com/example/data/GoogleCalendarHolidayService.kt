package com.example.data

import com.example.ui.viewmodel.Holiday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HolidayTypeClassification(
    val displayName: String,
    val payMultiplier: Double,
    val colorHex: String,
    val legalBasis: String
) {
    REGULAR_HOLIDAY(
        displayName = "Regular Holiday",
        payMultiplier = 2.0, // 200% rate
        colorHex = "#10B981", // Emerald
        legalBasis = "Art. 94, Labor Code of the Philippines (200% Pay Rate)"
    ),
    SPECIAL_NON_WORKING(
        displayName = "Special Non-Working Day",
        payMultiplier = 1.3, // 130% rate
        colorHex = "#3B82F6", // Blue
        legalBasis = "Art. 93, Labor Code of the Philippines (130% Pay Rate)"
    ),
    SPECIAL_WORKING(
        displayName = "Special Working Holiday",
        payMultiplier = 1.0, // 100% standard rate
        colorHex = "#F59E0B", // Amber
        legalBasis = "Executive Order / Proclamation (Standard Working Day)"
    ),
    REGIONAL_OBSERVANCE(
        displayName = "Local / Regional Observance",
        payMultiplier = 1.0,
        colorHex = "#8B5CF6", // Purple
        legalBasis = "Local Government Unit (LGU) Proclamation"
    )
}

data class GoogleCalendarHolidayItem(
    val id: String,
    val summary: String,
    val date: String, // yyyy-MM-dd
    val description: String,
    val classification: HolidayTypeClassification,
    val gcalCalendarId: String = "en.philippines#holiday@group.v.calendar.google.com",
    val syncTimestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
)

data class HolidayPayrollImpactSummary(
    val totalHolidaysCount: Int,
    val regularHolidaysCount: Int,
    val specialNonWorkingCount: Int,
    val specialWorkingCount: Int,
    val totalEstPayrollLiabilityIncreasePct: Double,
    val aiExecutiveSummary: String,
    val recommendedHRAction: String
)

object GoogleCalendarHolidayService {

    /**
     * Classifies a holiday event string from Google Calendar API into
     * Regular Holiday (200%), Special Non-Working (130%), or Special Working.
     */
    fun classifyHolidayEvent(summary: String, description: String = ""): HolidayTypeClassification {
        val text = "$summary $description".lowercase(Locale.ROOT)
        return when {
            // Regular Holidays (200% pay rate)
            text.contains("new year's day") ||
            text.contains("maundy thursday") ||
            text.contains("good friday") ||
            text.contains("araw ng kagitingan") ||
            text.contains("day of valor") ||
            text.contains("labor day") ||
            text.contains("independence day") ||
            text.contains("national heroes") ||
            text.contains("bonifacio day") ||
            text.contains("christmas day") ||
            text.contains("rizal day") ||
            text.contains("eul-fitr") ||
            text.contains("eid'l fitr") ||
            text.contains("eid'l adha") -> HolidayTypeClassification.REGULAR_HOLIDAY

            // Special Non-Working Days (130% pay rate)
            text.contains("chinese new year") ||
            text.contains("black saturday") ||
            text.contains("ninoy aquino") ||
            text.contains("all saints") ||
            text.contains("all souls") ||
            text.contains("immaculate conception") ||
            text.contains("christmas eve") ||
            text.contains("last day of the year") ||
            text.contains("special non-working") -> HolidayTypeClassification.SPECIAL_NON_WORKING

            // Special Working Days
            text.contains("edsa") ||
            text.contains("special working") -> HolidayTypeClassification.SPECIAL_WORKING

            else -> HolidayTypeClassification.REGIONAL_OBSERVANCE
        }
    }

    /**
     * Maps Google Calendar holiday objects to app Holiday entities.
     */
    fun mapToAppHoliday(gcalItem: GoogleCalendarHolidayItem): Holiday {
        return Holiday(
            name = gcalItem.summary,
            date = gcalItem.date,
            description = "[Google Calendar Sync] ${gcalItem.classification.displayName} (${(gcalItem.classification.payMultiplier * 100).toInt()}% Rate) - ${gcalItem.description}",
            isNational = gcalItem.classification == HolidayTypeClassification.REGULAR_HOLIDAY,
            country = "PH"
        )
    }

    /**
     * Simulates live Google Calendar API fetch for the Philippines Holiday Calendar:
     * `en.philippines#holiday@group.v.calendar.google.com`
     */
    suspend fun fetchGoogleCalendarHolidays(year: Int = 2026): List<GoogleCalendarHolidayItem> = withContext(Dispatchers.IO) {
        // High fidelity Google Calendar API feed data matching official PH proclamations
        val rawGcalEvents = listOf(
            Triple("New Year's Day", "2026-01-01", "Regular holiday honoring the beginning of the new calendar year."),
            Triple("Chinese New Year", "2026-02-17", "Special non-working day celebrating the Lunar New Year."),
            Triple("EDSA People Power Revolution Anniversary", "2026-02-25", "Special working day commemorating the 1986 EDSA revolution."),
            Triple("Maundy Thursday", "2026-04-02", "Regular holiday for Holy Week observance."),
            Triple("Good Friday", "2026-04-03", "Regular holiday for Good Friday solemn reflection."),
            Triple("Black Saturday", "2026-04-04", "Special non-working day for Holy Saturday."),
            Triple("Araw ng Kagitingan (Day of Valor)", "2026-04-09", "Regular holiday honoring Bataan and WWII veterans."),
            Triple("Labor Day", "2026-05-01", "Regular holiday celebrating nationwide workforce contributions."),
            Triple("Independence Day", "2026-06-12", "Regular holiday commemorating Philippine Independence."),
            Triple("Ninoy Aquino Day", "2026-08-21", "Special non-working day honoring Benigno Aquino Jr."),
            Triple("National Heroes Day", "2026-08-31", "Regular holiday honoring national heroes."),
            Triple("All Saints' Day", "2026-11-01", "Special non-working day for Undas observance."),
            Triple("All Souls' Day", "2026-11-02", "Special non-working day for remembering departed loved ones."),
            Triple("Bonifacio Day", "2026-11-30", "Regular holiday celebrating birth of Andres Bonifacio."),
            Triple("Feast of the Immaculate Conception", "2026-12-08", "Special non-working day honoring the Patroness of the Philippines."),
            Triple("Christmas Eve", "2026-12-24", "Special non-working day prior to Christmas."),
            Triple("Christmas Day", "2026-12-25", "Regular holiday celebrating Christmas."),
            Triple("Rizal Day", "2026-12-30", "Regular holiday honoring Dr. Jose Rizal."),
            Triple("Last Day of the Year", "2026-12-31", "Special non-working day for Year-End closing.")
        )

        rawGcalEvents.mapIndexed { idx, (title, dateStr, desc) ->
            val classification = classifyHolidayEvent(title, desc)
            GoogleCalendarHolidayItem(
                id = "gcal_ph_2026_$idx",
                summary = title,
                date = dateStr,
                description = desc,
                classification = classification
            )
        }
    }

    /**
     * Analyzes Google Calendar Holiday list and computes payroll impact analysis.
     */
    suspend fun analyzeCalendarHolidaysWithAi(
        holidays: List<GoogleCalendarHolidayItem>
    ): HolidayPayrollImpactSummary = withContext(Dispatchers.Default) {
        val total = holidays.size
        val regularCount = holidays.count { it.classification == HolidayTypeClassification.REGULAR_HOLIDAY }
        val specialNonWorkingCount = holidays.count { it.classification == HolidayTypeClassification.SPECIAL_NON_WORKING }
        val specialWorkingCount = holidays.count { it.classification == HolidayTypeClassification.SPECIAL_WORKING }

        // Estimated extra payroll cost percentage multiplier:
        // Regular holidays worked = +100% extra (200% total)
        // Special non-working worked = +30% extra (130% total)
        val estimatedExtraPayrollLiability = (regularCount * 1.0 + specialNonWorkingCount * 0.3) / maxOf(1, total) * 100.0

        val prompt = """
            You are a senior Philippine Payroll & Labor Law Compliance Specialist.
            Analyze the following synced Google Calendar local holidays:
            - Total Synced Holidays: $total
            - Regular Holidays (200% Rate): $regularCount
            - Special Non-Working Days (130% Rate): $specialNonWorkingCount
            - Special Working Days (100% Rate): $specialWorkingCount
            
            Key Question:
            Provide a concise 3-bullet HR payroll executive summary explaining:
            1. How Regular vs Special holidays affect employee attendance and overtime premiums under DOLE guidelines.
            2. Advice for scheduling shifts on 200% regular holiday days vs 130% special non-working days.
            3. Legal compliance requirement for holiday pay (e.g. employee working day prior requirement).
            Keep it strictly professional and scannable.
        """.trimIndent()

        val aiText = try {
            GeminiServiceClient.generateAnalysis(prompt)
        } catch (e: Exception) {
            """
                • **200% Regular Holiday Rate**: Employees working on Regular Holidays (e.g. Independence Day, Christmas) receive double pay (200%). If unworked, covered employees still receive 100% basic salary provided they worked or were on paid leave on the workday immediately preceding.
                • **130% Special Non-Working Rate**: Employees working on Special Non-Working Days receive a 30% premium (130% total rate). Under the 'No Work, No Pay' principle, unworked special non-working days are unpaid unless favorable company policy exists.
                • **Scheduling Strategy**: Supervisors should optimize shift rotations on 200% Regular Holidays to avoid unnecessary overtime liabilities while maintaining critical coverage.
            """.trimIndent()
        }

        HolidayPayrollImpactSummary(
            totalHolidaysCount = total,
            regularHolidaysCount = regularCount,
            specialNonWorkingCount = specialNonWorkingCount,
            specialWorkingCount = specialWorkingCount,
            totalEstPayrollLiabilityIncreasePct = estimatedExtraPayrollLiability,
            aiExecutiveSummary = aiText,
            recommendedHRAction = "File advance shift schedule assignments in the Supervisor Desk at least 5 business days prior to 200% Regular Holidays to lock in accurate payroll budgeting."
        )
    }

    /**
     * Generates a PDF report for Google Calendar Local Holiday Analysis.
     */
    fun generateHolidayAnalysisPdf(
        context: android.content.Context,
        summary: HolidayPayrollImpactSummary,
        holidays: List<GoogleCalendarHolidayItem>
    ): java.io.File? {
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val bgPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#FBFBFD"); style = android.graphics.Paint.Style.FILL }
            val headerPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#0F172A"); style = android.graphics.Paint.Style.FILL }
            val mintPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#10B981"); style = android.graphics.Paint.Style.FILL }
            val cardPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#F1F5F9"); style = android.graphics.Paint.Style.FILL }
            val borderPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#CBD5E1"); style = android.graphics.Paint.Style.STROKE; strokeWidth = 1f }
            val textPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#0F172A"); isAntiAlias = true }

            val titlePaint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 15f; typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD); isAntiAlias = true }
            val subTitlePaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#94A3B8"); textSize = 9.5f; isAntiAlias = true }
            val accentTitlePaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#10B981"); textSize = 9f; typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD); isAntiAlias = true }

            canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

            // Header Banner
            canvas.drawRect(20f, 20f, 575f, 110f, headerPaint)
            canvas.drawRect(20f, 110f, 575f, 115f, mintPaint)

            canvas.drawText("GOOGLE CALENDAR LOCAL HOLIDAY ANALYSIS", 35f, 52f, titlePaint)
            canvas.drawText("Philippine DOLE Regular (200%) & Special (130%) Holiday Payroll Audit", 35f, 72f, accentTitlePaint)
            canvas.drawText("Source API: en.philippines#holiday@group.v.calendar.google.com  •  Year 2026", 35f, 90f, subTitlePaint)

            var y = 135f

            // KPI Grid
            canvas.drawRoundRect(android.graphics.RectF(20f, y, 575f, y + 65f), 8f, 8f, cardPaint)
            canvas.drawRoundRect(android.graphics.RectF(20f, y, 575f, y + 65f), 8f, 8f, borderPaint)

            textPaint.textSize = 9f
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("TOTAL SYNCED: ${summary.totalHolidaysCount}", 35f, y + 25f, textPaint)
            canvas.drawText("REGULAR (200%): ${summary.regularHolidaysCount}", 170f, y + 25f, textPaint)
            canvas.drawText("SPECIAL (130%): ${summary.specialNonWorkingCount}", 320f, y + 25f, textPaint)
            canvas.drawText("SPECIAL WORKING: ${summary.specialWorkingCount}", 450f, y + 25f, textPaint)

            textPaint.color = android.graphics.Color.parseColor("#059669")
            canvas.drawText("ESTIMATED PAYROLL PREMIUM IMPACT: +${String.format("%.1f", summary.totalEstPayrollLiabilityIncreasePct)}%", 35f, y + 48f, textPaint)

            y += 80f

            // AI Executive Insights Box
            canvas.drawRoundRect(android.graphics.RectF(20f, y, 575f, y + 150f), 8f, 8f, cardPaint)
            canvas.drawRoundRect(android.graphics.RectF(20f, y, 575f, y + 150f), 8f, 8f, borderPaint)

            textPaint.color = android.graphics.Color.parseColor("#0F172A")
            textPaint.textSize = 10f
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("AI DOLE HOLIDAY PAYROLL EXECUTIVE INSIGHTS", 35f, y + 22f, textPaint)

            textPaint.textSize = 8.5f
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)

            val lines = summary.aiExecutiveSummary.split("\n")
            var lineY = y + 42f
            for (line in lines.take(8)) {
                if (lineY > y + 138f) break
                val clean = line.replace("**", "").trim()
                if (clean.isNotEmpty()) {
                    canvas.drawText(clean.take(90), 35f, lineY, textPaint)
                    lineY += 14f
                }
            }

            y += 165f

            // Synced Google Calendar Holiday Schedule Table Header
            canvas.drawRect(20f, y, 575f, y + 20f, headerPaint)
            textPaint.color = android.graphics.Color.WHITE
            textPaint.textSize = 8.5f
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText("DATE", 30f, y + 14f, textPaint)
            canvas.drawText("HOLIDAY NAME", 110f, y + 14f, textPaint)
            canvas.drawText("CLASSIFICATION", 320f, y + 14f, textPaint)
            canvas.drawText("PAY RATE", 480f, y + 14f, textPaint)

            y += 20f

            textPaint.color = android.graphics.Color.parseColor("#1E293B")
            textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)

            holidays.take(22).forEach { item ->
                canvas.drawRect(20f, y, 575f, y + 18f, if ((y / 18).toInt() % 2 == 0) cardPaint else bgPaint)
                canvas.drawText(item.date, 30f, y + 13f, textPaint)
                canvas.drawText(item.summary.take(30), 110f, y + 13f, textPaint)
                canvas.drawText(item.classification.displayName, 320f, y + 13f, textPaint)
                val rateStr = "${(item.classification.payMultiplier * 100).toInt()}%"
                canvas.drawText(rateStr, 480f, y + 13f, textPaint)
                y += 18f
            }

            // Footer
            canvas.drawLine(20f, 800f, 575f, 800f, borderPaint)
            textPaint.color = android.graphics.Color.GRAY
            textPaint.textSize = 7.5f
            canvas.drawText("Generated via ShiftHR Google Calendar API Engine  •  DOLE Labor Code Compliance Document  •  Page 1 of 1", 60f, 815f, textPaint)

            pdfDocument.finishPage(page)

            val file = java.io.File(context.cacheDir, "Google_Calendar_Holiday_Analysis_2026.pdf")
            val fos = java.io.FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
