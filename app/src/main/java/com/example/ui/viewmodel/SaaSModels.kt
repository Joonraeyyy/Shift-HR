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

data class Announcement(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val date: String
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
    val shiftName: String // "Manila Dev Shift", "Indore Day Flex", "Night Ops", "Off"
)

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


