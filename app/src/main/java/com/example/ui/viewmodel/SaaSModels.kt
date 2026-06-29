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
    val status: String = "PENDING" // PENDING, APPROVED, REJECTED
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
    val documents: List<String> = listOf("Employment_Contract.pdf", "Passport_Scan.pdf", "Tax_Declaration.pdf")
)

data class RegisteredUser(
    val username: String,
    val passcode: String,
    val name: String,
    val role: String,
    val companyName: String,
    val companyCode: String
)

