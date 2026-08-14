package com.example.ui.viewmodel

import java.util.UUID

data class LeaveRequest(
    val id: String = UUID.randomUUID().toString(),
    val employeeName: String,
    val leaveType: String, // Sick Leave, Vacation Leave
    val startDate: String,
    val endDate: String,
    val reason: String,
    val status: String = "PENDING" // PENDING, APPROVED, REJECTED
)

data class CorrectionRequest(
    val id: String = UUID.randomUUID().toString(),
    val employeeName: String,
    val date: String,
    val requestedTimeIn: String,
    val requestedTimeOut: String,
    val reason: String,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val disputeType: String = "GPS Drift", // GPS Drift, NFC Error, False Positive, Anti-Spoofing Mismatch
    val originalTelemetry: String = "GPS Accuracy: 180m (Required < 50m)",
    val isPunchDispute: Boolean = false,
    val systemRemarks: String = ""
)

data class ReimbursementRequest(
    val id: String = UUID.randomUUID().toString(),
    val employeeName: String,
    val title: String,
    val amount: Double,
    val status: String = "PENDING" // PENDING, APPROVED, REJECTED
)

data class TaskAssignment(
    val id: String = UUID.randomUUID().toString(),
    val employeeName: String,
    val taskTitle: String,
    val description: String,
    val status: String = "PENDING" // PENDING, COMPLETED
)

data class BulletinComment(
    val id: String = UUID.randomUUID().toString(),
    val authorName: String,
    val authorRole: String = "EMPLOYEE",
    val text: String,
    val timestamp: String
)

data class Announcement(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String = "",
    val content: String,
    val date: String,
    val category: String = "Wellness", // e.g., Calm, Ritual, Sleep, Motion, Health, Fitness, Milestone, Culture
    val imageUrl: String = "", // URL or Content URI
    val drawableResName: String = "", // e.g. "img_hr_mind_reset", "img_hr_morning_flow", "img_hr_team_summit"
    val author: String = "HR Leadership",
    val galleryImages: List<String> = emptyList(),
    val likesCount: Int = 12,
    val celebrateCount: Int = 8,
    val isLiked: Boolean = false,
    val isCelebrated: Boolean = false,
    val comments: List<BulletinComment> = emptyList(),
    val isPinned: Boolean = false
)

data class AuditLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val user: String,
    val action: String
)

data class FeelReport(
    val employeeName: String,
    val score: Int, // 1 to 5
    val note: String,
    val date: String
)

data class LoanRecord(
    val id: String = UUID.randomUUID().toString(),
    val employeeName: String,
    val type: String,
    val amount: Double,
    val monthlyDeduction: Double,
    val remaining: Double
)

data class EmployeeProfile(
    val id: String, // COS-2026-XXXX
    val name: String,
    val department: String,
    val position: String,
    val status: String, // Regular, Probationary, Contractual
    val email: String,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val age: Int = 28,
    val phoneNumber: String = "+91 98765 43210",
    val picture: String = "",
    val documents: List<String> = listOf("Employment_Contract.pdf", "Passport_Scan.pdf", "Tax_Declaration.pdf"),

    // --- NEW PROFESSIONAL STRUCTURE PROPERTIES WITH DEFAULTS ---
    // 👤 Basic Information
    val preferredName: String = "",
    val gender: String = "Male",
    val dob: String = "1997-05-15",
    val civilStatus: String = "Single",
    val nationality: String = "Indian",

    // 💼 Employment Information
    val team: String = "Core Platform",
    val dateHired: String = "2023-01-15",
    val supervisor: String = "Aditya Joshi (Director)",
    val workLocation: String = "Indore Hub",
    val shiftSchedule: String = "Manila Dev Shift",

    // 📞 Contact Information
    val personalEmail: String = "personal@gmail.com",
    val address: String = "123 Hub Street, Indore",
    val emergencyRelationship: String = "Spouse",

    // 🪪 Government & Payroll Information
    val taxId: String = "TIN-987-654-321",
    val ssn: String = "SSN-12-3456789-0",
    val healthInsurance: String = "HMO-77621-A",
    val housingFund: String = "HF-9092-111",
    val bankName: String = "State Bank of Indore",
    val bankAccount: String = "99827162551",

    // 📱 Attendance & Device
    val registeredDevice: String = "SECURE_MDM_ANDROID_84F9",
    val digitalId: String = "DIGI-ID-7721",
    val nfcId: String = "NFC-TAG-772A",
    val attendanceStatus: String = "Checked Out",
    val assignedLocation: String = "Indore Hub",
    val lastLogin: String = "2026-06-30 08:30 AM",

    // 📅 Leave & Attendance Summary
    val vacationLeaveBalance: Int = 15,
    val sickLeaveBalance: Int = 10,
    val otherLeaveBalances: Int = 5,
    val maternityLeaveBalance: Int = 105,
    val paternityLeaveBalance: Int = 7,
    val soloParentLeaveBalance: Int = 7,
    val totalLate: Int = 2,
    val absences: Int = 0,
    val overtimeHours: Double = 4.5,

    // Account & Security
    val username: String = "",
    val twoFactorEnabled: Boolean = true,
    val loginHistory: List<String> = listOf("2026-06-30 08:30 AM - Android 14", "2026-06-29 09:15 AM - Android 14"),
    val accountStatus: String = "Active"
)

data class RegisteredUser(
    val username: String,
    val passcode: String,
    val name: String,
    val role: String,
    val companyName: String,
    val companyCode: String
)

data class TeamSchedule(
    val id: String,
    val employeeName: String,
    val department: String,
    val date: String, // format "YYYY-MM-DD", e.g., "2026-06-29"
    val shiftName: String, // "Manila Dev Shift", "Indore Day Flex", "Night Ops", "Free Custom Shift", "Off"
    val customStartTime: String? = null, // e.g. "10:30 AM"
    val customBreakTime: String? = null, // e.g. "02:30 PM"
    val customEndTime: String? = null,   // e.g. "07:30 PM"
    val isOvernight: Boolean = false
)

object CustomShiftCalculator {
    fun parseTimeToMinutes(hour24: Int, minute: Int): Int {
        return (hour24 * 60 + minute) % 1440
    }

    fun parseTimeStringToMinutes(timeStr: String): Int {
        val clean = timeStr.trim().uppercase(java.util.Locale.US)
        val isPm = clean.contains("PM")
        val isAm = clean.contains("AM")
        val rawTime = clean.replace("AM", "").replace("PM", "").trim()
        val parts = rawTime.split(":")
        if (parts.size >= 2) {
            var h = parts[0].trim().toIntOrNull() ?: 9
            val m = parts[1].trim().toIntOrNull() ?: 0
            if (isPm && h < 12) h += 12
            if (isAm && h == 12) h = 0
            return (h * 60 + m) % 1440
        }
        return 540 // Default 9:00 AM (540 mins)
    }

    fun formatMinutesTo12Hr(totalMinutes: Int): String {
        val minsInDay = (totalMinutes % 1440 + 1440) % 1440
        val hour24 = minsInDay / 60
        val minute = minsInDay % 60
        val isPm = hour24 >= 12
        val hour12 = when (val h = hour24 % 12) {
            0 -> 12
            else -> h
        }
        return String.format(java.util.Locale.US, "%02d:%02d %s", hour12, minute, if (isPm) "PM" else "AM")
    }

    data class ShiftBlockResult(
        val startTimeStr: String,       // e.g. "10:30 AM"
        val breakStartTimeStr: String,  // e.g. "02:30 PM"
        val breakEndTimeStr: String,    // e.g. "03:30 PM"
        val endTimeStr: String,         // e.g. "07:30 PM"
        val startMins: Int,
        val breakStartMins: Int,
        val endMins: Int,
        val isOvernight: Boolean,
        val summaryText: String,         // "10:30 AM - 07:30 PM"
        val subText: String              // "(8h Work • 1h Break at 02:30 PM)"
    )

    fun calculate9HourBlock(
        startHour24: Int,
        startMinute: Int,
        breakOffsetHours: Double = 4.0 // default 4 hours after start
    ): ShiftBlockResult {
        val startMins = parseTimeToMinutes(startHour24, startMinute)
        val breakStartMins = (startMins + (breakOffsetHours * 60).toInt())
        val breakEndMins = breakStartMins + 60
        val totalEndMins = startMins + (9 * 60) // 8h work + 1h break = 9h block

        val isOvernight = (startMins + 9 * 60) >= 1440

        val startStr = formatMinutesTo12Hr(startMins)
        val breakStartStr = formatMinutesTo12Hr(breakStartMins)
        val breakEndStr = formatMinutesTo12Hr(breakEndMins)
        val endStr = formatMinutesTo12Hr(totalEndMins)

        val summaryText = "$startStr - $endStr"
        val overnightLabel = if (isOvernight) " [Overnight]" else ""
        val subText = "(8h Work • 1h Break at $breakStartStr)$overnightLabel"

        return ShiftBlockResult(
            startTimeStr = startStr,
            breakStartTimeStr = breakStartStr,
            breakEndTimeStr = breakEndStr,
            endTimeStr = endStr,
            startMins = startMins,
            breakStartMins = breakStartMins,
            endMins = totalEndMins,
            isOvernight = isOvernight,
            summaryText = summaryText,
            subText = subText
        )
    }

    fun calculate9HourBlockFromTimeStr(timeStr: String, breakOffsetHours: Double = 4.0): ShiftBlockResult {
        val totalMins = parseTimeStringToMinutes(timeStr)
        val h = totalMins / 60
        val m = totalMins % 60
        return calculate9HourBlock(h, m, breakOffsetHours)
    }
}

// --- COMPLIANCE WORKFLOW MODELS ---

data class DisciplinaryCase(
    val id: String = UUID.randomUUID().toString(),
    val employeeName: String,
    val infractionTitle: String,
    val infractionDate: String,
    val noticeToExplainDate: String,
    val noticeToExplainContent: String,
    val employeeExplanation: String = "",
    val employeeExplanationDate: String = "",
    val noticeOfDecisionDate: String = "",
    val noticeOfDecisionContent: String = "",
    val status: String = "NOTICE_TO_EXPLAIN_ISSUED", // NOTICE_TO_EXPLAIN_ISSUED, EXPLANATION_SUBMITTED, NOTICE_OF_DECISION_ISSUED, CASE_RESOLVED
    val severity: String = "Medium", // Low, Medium, High
    val policyViabilityLink: String = "https://handbook.shifthr.corp/policies/attendance-irregularities"
)

data class OffboardingClearance(
    val id: String = UUID.randomUUID().toString(),
    val employeeName: String,
    val separationDate: String, // e.g. 2026-07-31
    val department: String,
    val itClearanceStatus: String = "PENDING", // PENDING, CLEARED, N/A
    val financeClearanceStatus: String = "PENDING", // PENDING, CLEARED, N/A
    val adminClearanceStatus: String = "PENDING", // PENDING, CLEARED, N/A
    val coeReady: Boolean = false,
    val finalPayReady: Boolean = false,
    val status: String = "IN_PROGRESS", // IN_PROGRESS, FULLY_CLEARED
    val comments: String = ""
)

data class OkrRecord(
    val id: String = UUID.randomUUID().toString(),
    val employeeName: String,
    val objective: String,
    val keyResult: String,
    val targetValue: String,
    val currentValue: String,
    val progress: Int, // 0 to 100
    val selfAppraisal: String = "",
    val managerFeedback: String = "",
    val status: String = "ACTIVE" // ACTIVE, ACHIEVED, DEFERRED
)

// Define structured HR categories
enum class DossierCategory(val label: String) {
    IDENTITY("Government Issued ID"),
    MEDICAL("Medical Certificate / Leaves"),
    FINANCIAL("Reimbursement Receipts"),
    CONTRACTUAL("Signed Agreements")
}

// Data model tracking the scanned output natively inside Core HR
data class ScannedDossierDocument(
    val fileId: String,
    val fileName: String,
    val localUri: String,
    val fileCategory: DossierCategory,
    val dateCaptured: String = "2026-07-13" // Captures active fiscal period
)

fun getCategoryForFile(fileName: String): DossierCategory {
    return when {
        fileName.startsWith("IDENTITY_", ignoreCase = true) || fileName.contains("passport", ignoreCase = true) -> DossierCategory.IDENTITY
        fileName.startsWith("MEDICAL_", ignoreCase = true) || fileName.contains("medical", ignoreCase = true) -> DossierCategory.MEDICAL
        fileName.startsWith("FINANCIAL_", ignoreCase = true) || fileName.contains("tax", ignoreCase = true) || fileName.contains("receipt", ignoreCase = true) -> DossierCategory.FINANCIAL
        else -> DossierCategory.CONTRACTUAL
    }
}

data class DoleHoursBreakdown(
    var regular: Double = 0.0,
    var nd: Double = 0.0,
    var ot: Double = 0.0,
    var otNd: Double = 0.0
)

data class DoleEarningsBreakdown(
    val regularPay: Double,
    val ndPay: Double,
    val otPay: Double,
    val otNdPay: Double,
    val totalGrossPay: Double
)

data class DoleCalculationResult(
    val hours: DoleHoursBreakdown,
    val earnings: DoleEarningsBreakdown,
    val baseMultiplier: Double,
    val otMultiplier: Double,
    val ndPremium: Double,
    val totalWorkedBeforeMeal: Double,
    val totalWorkedAfterMeal: Double
)

fun calculateDoleShiftPay(
    clockIn: java.util.Date,
    clockOut: java.util.Date,
    isRestDay: Boolean,
    holidayType: String?, // "REGULAR", "SPECIAL", or null/""
    hourlyRate: Double
): DoleCalculationResult {
    val totalWorkedMillis = clockOut.time - clockIn.time
    var totalWorkedHours = totalWorkedMillis.toDouble() / 3600000.0
    val originalTotalHours = totalWorkedHours
    
    if (totalWorkedHours > 5.0) {
        totalWorkedHours -= 1.0 // Subtract the mandatory 60-min unpaid meal break
    }
    totalWorkedHours = maxOf(0.0, totalWorkedHours)

    // Determine day multiplier factors based on Article 93 and 94
    val baseMultiplier = when (holidayType) {
        "REGULAR" -> if (isRestDay) 2.60 else 2.00
        "SPECIAL" -> if (isRestDay) 1.50 else 1.30
        else -> if (isRestDay) 1.30 else 1.0
    }

    // Compounding premiums
    val otMultiplier = if (baseMultiplier == 1.0) 1.25 else 1.30
    val ndPremium = 0.10 // 10% premium for Night Diff hours

    val hoursBreakdown = DoleHoursBreakdown()

    val cal = java.util.Calendar.getInstance()
    cal.time = clockIn
    
    var hourIndex = 0.0
    
    while (cal.time.before(clockOut)) {
        // Unpaid meal break jump halfway through
        if (originalTotalHours > 5.0 && hourIndex == 4.0) {
            cal.add(java.util.Calendar.HOUR_OF_DAY, 1)
            if (!cal.time.before(clockOut)) {
                break
            }
        }

        val hourOfDay = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val isNdHour = (hourOfDay >= 22 || hourOfDay < 6)
        val isOvertime = (hourIndex >= 8.0)

        val stepSize = 0.25

        if (isOvertime) {
            if (isNdHour) {
                hoursBreakdown.otNd += stepSize
            } else {
                hoursBreakdown.ot += stepSize
            }
        } else {
            if (isNdHour) {
                hoursBreakdown.nd += stepSize
            } else {
                hoursBreakdown.regular += stepSize
            }
        }

        hourIndex += stepSize
        cal.add(java.util.Calendar.MINUTE, 15)
    }

    val hourlyBasePay = hourlyRate * baseMultiplier

    val payRegular = hoursBreakdown.regular * hourlyBasePay
    val payNd = hoursBreakdown.nd * hourlyBasePay * (1 + ndPremium)
    val payOt = hoursBreakdown.ot * hourlyBasePay * otMultiplier
    val payOtNd = hoursBreakdown.otNd * hourlyBasePay * otMultiplier * (1 + ndPremium)
    val totalGross = payRegular + payNd + payOt + payOtNd

    return DoleCalculationResult(
        hours = hoursBreakdown,
        earnings = DoleEarningsBreakdown(
            regularPay = payRegular,
            ndPay = payNd,
            otPay = payOt,
            otNdPay = payOtNd,
            totalGrossPay = totalGross
        ),
        baseMultiplier = baseMultiplier,
        otMultiplier = otMultiplier,
        ndPremium = ndPremium,
        totalWorkedBeforeMeal = originalTotalHours,
        totalWorkedAfterMeal = totalWorkedHours
    )
}

// --- COMPANY SURVEY MODULE DATA MODELS ---
enum class QuestionType { TEXT, RATING_1_TO_5 }

data class SurveyQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val type: QuestionType
)

data class CompanySurvey(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val isMandatory: Boolean,
    val questions: List<SurveyQuestion>,
    val responsesCount: Int = 0
)

data class SurveyResponse(
    val id: String = java.util.UUID.randomUUID().toString(),
    val surveyId: String,
    val employeeName: String,
    val answers: Map<String, String> // questionId to answer text or rating
)





