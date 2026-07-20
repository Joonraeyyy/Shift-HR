package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import com.example.data.WeatherResponse
import com.example.data.ForecastResponse
import com.example.data.WeatherServiceClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ShiftConfigEntity
import com.example.data.database.TimeLogEntity
import com.example.data.database.DossierDocumentEntity
import com.example.data.repository.TimeTrackerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Holiday data model
data class Holiday(
    val name: String,
    val date: String, // yyyy-MM-dd
    val description: String,
    val isNational: Boolean,
    val country: String = "ALL" // "PH", "IN", or "ALL"
)

data class CalendarEventNote(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // yyyy-MM-dd
    val title: String,
    val description: String,
    val time: String, // e.g., "10:00 AM"
    val syncWithGoogleCalendar: Boolean = true,
    val isNotificationActive: Boolean = true
)

class TimeTrackerViewModel(application: Application) : AndroidViewModel(application) {

    var calendarNotes = mutableStateOf<List<CalendarEventNote>>(listOf(
        CalendarEventNote(
            date = "2026-06-29",
            title = "HR Sync Meeting",
            description = "Discuss timesheet compliance audit workflow with directors",
            time = "10:00 AM",
            syncWithGoogleCalendar = true
        ),
        CalendarEventNote(
            date = "2026-06-30",
            title = "Payroll Review",
            description = "Finalize basic and overtime ledger calculation exports",
            time = "02:30 PM",
            syncWithGoogleCalendar = true
        )
    ))

    var activeSimulatedNotification = mutableStateOf<CalendarEventNote?>(null)

    fun addCalendarNote(date: String, title: String, description: String, time: String, sync: Boolean) {
        val newNote = CalendarEventNote(
            date = date,
            title = title,
            description = description,
            time = time,
            syncWithGoogleCalendar = sync
        )
        calendarNotes.value = calendarNotes.value + newNote
        
        // Trigger a simulated notification instantly
        if (sync) {
            activeSimulatedNotification.value = newNote
        }
    }

    fun deleteCalendarNote(id: String) {
        calendarNotes.value = calendarNotes.value.filter { it.id != id }
    }
    
    fun dismissActiveSimulatedNotification() {
        activeSimulatedNotification.value = null
    }

    private val repository: TimeTrackerRepository
    
    // UI state streams
    val allTimeLogs: StateFlow<List<TimeLogEntity>>
    val pendingTimeLogs: StateFlow<List<TimeLogEntity>>
    val shiftConfig: StateFlow<ShiftConfigEntity>
    val dossierDocuments: StateFlow<List<DossierDocumentEntity>>

    // Screen states
    var currentScreen = mutableStateOf("performance_reports") // clock, spreadsheet, hr_approval, settings, holidays
    var selfServiceTab = mutableStateOf("leave") // leave, correction, claims, profile
    var isAdminMode = mutableStateOf(false) // Toggle helper (updates based on login role)
    
    // Sign-in states
    var isLoggedIn = mutableStateOf(true)
    var currentUserRole = mutableStateOf("EMPLOYEE") // EMPLOYEE, SUPERVISOR, MANAGER, ADMIN_HR
    var currentUserName = mutableStateOf("Sarah Jenkins")
    var currentUserUsername = mutableStateOf("employee")
    
    // Filter states for spreadsheet
    var filterApproval = mutableStateOf("ALL") // ALL, PENDING, APPROVED, REJECTED
    var filterSync = mutableStateOf("ALL") // ALL, LOCAL, SYNCED
    var searchQuery = mutableStateOf("") // Search by employee name

    // Weather Forecast States
    var selectedWeatherCity = mutableStateOf("Manila")
    var weatherLoading = mutableStateOf(false)
    var weatherError = mutableStateOf<String?>(null)
    var currentWeather = mutableStateOf<WeatherResponse?>(null)
    var weatherForecast = mutableStateOf<ForecastResponse?>(null)

    // Dynamic Liquid Glass Theme State
    var selectedTheme = mutableStateOf("Emerald Glass")

    // --- RANKING BOARD MANAGEMENT STATES ---
    var postedRankingPeriod = mutableStateOf("Monthly") // Monthly, Every 3 Months, Quarterly, Yearly
    var lastPostedRankingNotification = mutableStateOf<String?>(null)

    fun postRankingPeriod(period: String) {
        postedRankingPeriod.value = period
        val msg = "Attention: New $period Rankings have been officially published and posted by Admin HR!"
        lastPostedRankingNotification.value = msg
        addNotification(
            title = "Ranking Board Published",
            message = "Admin HR has successfully posted the new $period rankings for review.",
            isAlert = true
        )
    }

    fun dismissRankingNotification() {
        lastPostedRankingNotification.value = null
    }

    // --- INTERACTIVE AI DATA ANALYSIS STATES ---
    var aiAnalysisText = mutableStateOf("")
    var aiAnalysisLoading = mutableStateOf(false)
    var aiAnalysisError = mutableStateOf<String?>(null)

    fun analyzeWorkforceWithAi(periodType: String, metricsSummary: String) {
        viewModelScope.launch {
            aiAnalysisLoading.value = true
            aiAnalysisError.value = null
            try {
                val prompt = """
                    You are an elite HR People Analytics AI assistant.
                    Analyze the following workforce comparison dataset for the period type: $periodType.
                    
                    Dataset details:
                    $metricsSummary
                    
                    Please provide a comprehensive, executive HR report containing:
                    1. 📈 WORKFORCE HIGHLIGHTS: A scannable evaluation of the talent trends (hires vs separations).
                    2. 🎯 RETENTION & MOVEMENTS: Key insight into promotions, job transfers, and turnover rates.
                    3. 💡 ACTIONABLE RECOMMENDATIONS: 2-3 specific recommendations to optimize stability and performance.
                    
                    Keep your tone highly professional, objective, and scannable. Write in a clear, formatted style. Limit to 200 words.
                """.trimIndent()
                
                val result = com.example.data.GeminiServiceClient.generateAnalysis(prompt)
                aiAnalysisText.value = result
            } catch (e: Exception) {
                // Fallback gracefully to professional local simulated analysis based on the selected period
                val simulatedReport = getSimulatedAnalysisForPeriod(periodType)
                aiAnalysisText.value = simulatedReport
                aiAnalysisError.value = "Fallback active: ${e.message}"
            } finally {
                aiAnalysisLoading.value = false
            }
        }
    }

    fun getSimulatedAnalysisForPeriod(periodType: String): String {
        return when (periodType.lowercase()) {
            "yearly" -> """
                📈 **WORKFORCE HIGHLIGHTS**
                - Talent onboarding expanded significantly by +14% year-over-year, leading to a headcount increase. 
                - Overall headcount remains healthy with a notable stabilization in the Engineering and Admin sectors.
                
                🎯 **RETENTION & MOVEMENTS**
                - Annualized turnover decreased from **4.2% (2024)** to **2.25% (2026 YTD)**, demonstrating exceptional retention.
                - Promotions surged to 12 active progressions, signaling a robust internal career progression framework.
                
                💡 **ACTIONABLE RECOMMENDATIONS**
                1. **Continuous Growth**: Institutionalize mentoring for the 26 new hires to sustain peak performance.
                2. **Skill Mapping**: Expand job-transfer opportunities to cross-train staff into high-priority operations.
            """.trimIndent()
            "quarterly" -> """
                📈 **WORKFORCE HIGHLIGHTS**
                - Q2 onboarding represented the peak expansion phase with 12 new hires, followed by a transition to Q3 consolidation (4 hires).
                - Net additions have balanced out normal seasonal attrition patterns.
                
                🎯 **RETENTION & MOVEMENTS**
                - Defended a low turnover rate of **0.8% in Q3**, down from Q2's 1.35%.
                - Internally mobilized 12 employees via promotions during the first three quarters, improving vertical talent pipeline health.
                
                💡 **ACTIONABLE RECOMMENDATIONS**
                1. **Onboarding Audit**: Review Q2 hiring feedback to refine integration structures.
                2. **Retention Focus**: Implement mid-year career pulse-checks to identify candidates for upcoming Q4 promotion cycles.
            """.trimIndent()
            else -> """
                📈 **WORKFORCE HIGHLIGHTS**
                - Onboarding velocity remained steady with 6 new hires in June and 3 in July.
                - Low attrition continues with only 1 separation reported in July, reflecting stable operational continuity.
                
                🎯 **RETENTION & MOVEMENTS**
                - July's turnover rate holds at a record low of **0.18%**, compared to 0.22% in June.
                - Core department alignment remains strong with 1 vertical promotion and 1 cross-departmental transfer executed.
                
                💡 **ACTIONABLE RECOMMENDATIONS**
                1. **July cohort support**: Provide targeted training support for July's new hires.
                2. **Role transfers**: Conduct structured surveys on July's job transfers to gauge role alignment and happiness.
            """.trimIndent()
        }
    }

    // Local / Offline Simulation states
    var isMockOffline = mutableStateOf(false) // Simulates device losing network connection
    var isSyncing = mutableStateOf(false)
    var syncMessage = mutableStateOf("")

    // Document scanner / multi-image export states
    var showExportSheet = mutableStateOf(false)
    var pendingScanUris = mutableStateOf<List<Uri>>(emptyList())
    var exportFileName = mutableStateOf("")
    var exportCategory = mutableStateOf(DossierCategory.IDENTITY)
    var exportOptimization = mutableStateOf("Standard (Recommended for HR)")
    var targetProfileId = mutableStateOf("")

    // Active log state
    private val _activeTimeLog = MutableStateFlow<TimeLogEntity?>(null)
    val activeTimeLog: StateFlow<TimeLogEntity?> = _activeTimeLog.asStateFlow()

    // Real-time Punch timing counters
    var activeTimerString = mutableStateOf("00:00:00")
    var breakTimerString = mutableStateOf("")
    private var timerJob: Job? = null

    // System Notifications list
    val notifications = mutableStateOf<List<NotificationItem>>(
        listOf(
            NotificationItem(
                id = "security_check_init",
                title = "SECURITY CHECK",
                message = "Logged in successfully as Sarah Jenkins (EMPLOYEE)",
                timestamp = System.currentTimeMillis(),
                isAlert = false
            )
        )
    )

    // --- REGIONAL & SCHEDULE SETTINGS ---
    var selectedCurrency = mutableStateOf("PHP") // USD or PHP
    var currentScheduleType = mutableStateOf("Weekly") // Weekly, 15 Days, Monthly

    fun getCurrencySymbol(): String {
        return if (selectedCurrency.value == "PHP") "₱" else "$"
    }

    // Active log for edit modal
    var selectedLogForEdit = mutableStateOf<TimeLogEntity?>(null)

    // --- CYBER CLOCK SECURITY / TELEMETRY STATES ---
    var geofenceRadius = mutableStateOf(100f) // Allowed radius in meters
    var simulatedDistance = mutableStateOf(25f) // Simulated employee distance in meters
    var isFaceRecognitionEnabled = mutableStateOf(true)
    var isDeviceVerificationEnabled = mutableStateOf(true)
    var isLiveLocationTrackingActive = mutableStateOf(true)
    
    // Security scan toggles
    var isMockGpsActive = mutableStateOf(false)
    var isRootedActive = mutableStateOf(false)
    var isImpossibleTravelTriggered = mutableStateOf(false)
    
    // Simulation parameters
    var registeredDeviceId = mutableStateOf("SECURE_MDM_ANDROID_84F9")
    var currentSimulatedDevice = mutableStateOf("SECURE_MDM_ANDROID_84F9") // or "UNAPPROVED_MOCK_PHONE"
    var currentSimulatedFaceName = mutableStateOf("Sarah Jenkins") // or "INTRUDER_FACE"
    
    // Interactive state for biometric dialog
    var showFaceScannerForAction = mutableStateOf<String?>(null)
    var isFaceScanMismatched = mutableStateOf(false)
    
    // Shift scheduler states
    var selectedShiftTemplate = mutableStateOf("Manila Dev Shift") // Manila Dev Shift, Indore Day Flex, Night Ops
    
    // Live field locations
    var liveFieldLat = mutableStateOf(14.5995)
    var liveFieldLng = mutableStateOf(120.9842)

    // Combined Holiday list for Manila / Philippines region
    var localHolidays = mutableStateOf<List<Holiday>>(listOf(
        Holiday("New Year's Day", "2026-01-01", "Global New Year and Philippine Regular Holiday", true, "PH"),
        Holiday("Chinese New Year (PH)", "2026-02-17", "Special Non-Working Day celebrating Chinese New Year in the Philippines", false, "PH"),
        Holiday("EDSA People Power Revolution Anniversary (PH)", "2026-02-25", "Special Working Day celebrating EDSA anniversary in the Philippines", false, "PH"),
        Holiday("Maundy Thursday (PH)", "2026-04-02", "Philippine Maundy Thursday local holiday reflection", true, "PH"),
        Holiday("Good Friday (PH)", "2026-04-03", "Good Friday solemn holiday recognized in the Philippines", true, "PH"),
        Holiday("Black Saturday (PH)", "2026-04-04", "Special Non-Working Day in the Philippines", false, "PH"),
        Holiday("Araw ng Kagitingan (PH)", "2026-04-09", "Philippine Day of Valor honoring local heroes", true, "PH"),
        Holiday("Labor Day (PH)", "2026-05-01", "Philippine Labor Day celebrating local workers", true, "PH"),
        Holiday("Independence Day (PH)", "2026-06-12", "Philippine Independence Day grand celebration", true, "PH"),
        Holiday("Ninoy Aquino Day (PH)", "2026-08-21", "Special Non-Working Day honoring Ninoy Aquino in the Philippines", false, "PH"),
        Holiday("National Heroes Day (PH)", "2026-08-31", "Philippine National Heroes Day holiday", true, "PH"),
        Holiday("All Saints' Day (PH)", "2026-11-01", "Special Non-Working Day in the Philippines", false, "PH"),
        Holiday("All Souls' Day (PH)", "2026-11-02", "Additional Special Non-Working Day in the Philippines", false, "PH"),
        Holiday("Bonifacio Day (PH)", "2026-11-30", "Philippine Andres Bonifacio celebration of courage", true, "PH"),
        Holiday("Feast of the Immaculate Conception (PH)", "2026-12-08", "Special Non-Working Day honoring the Feast of the Immaculate Conception in the Philippines", false, "PH"),
        Holiday("Christmas Eve (PH)", "2026-12-24", "Additional Special Non-Working Day in the Philippines", false, "PH"),
        Holiday("Christmas Day (PH)", "2026-12-25", "Global winter celebration and gifting in the Philippines", true, "PH"),
        Holiday("Rizal Day (PH)", "2026-12-30", "Philippine Jose Rizal national hero tribute day", true, "PH"),
        Holiday("Last Day of the Year (PH)", "2026-12-31", "Additional Special Non-Working Day in the Philippines", false, "PH")
    ))

    fun getUserCountryCode(): String {
        return "PH"
    }

    fun addCustomHoliday(name: String, date: String, description: String, isNational: Boolean, country: String = "ALL") {
        val currentHolidays = localHolidays.value.toMutableList()
        currentHolidays.add(Holiday(name, date, description, isNational, country))
        localHolidays.value = currentHolidays
    }

    var todayHoliday = mutableStateOf<Holiday?>(null)

    // SaaS States
    var companyName = mutableStateOf("Shift HR Corp")
    var companyCode = mutableStateOf("SHIFTHR")

    var registeredUsers = mutableStateOf(listOf(
        RegisteredUser("employee", "emp123", "Sarah Jenkins", "EMPLOYEE", "Shift HR Corp", "SHIFTHR"),
        RegisteredUser("supervisor", "super123", "Robert Chen", "SUPERVISOR", "Shift HR Corp", "SHIFTHR"),
        RegisteredUser("manager", "manager123", "Anjali Sharma", "MANAGER", "Shift HR Corp", "SHIFTHR"),
        RegisteredUser("admin", "admin123", "Aditya Joshi (Director)", "ADMIN_HR", "Shift HR Corp", "SHIFTHR")
    ))

    var leaveRequests = mutableStateOf<List<LeaveRequest>>(listOf(
        LeaveRequest(employeeName = "Sarah Jenkins", leaveType = "Sick Leave", startDate = "2026-07-01", endDate = "2026-07-02", reason = "Indore seasonal fever flu recovery", status = "APPROVED"),
        LeaveRequest(employeeName = "Marcus Aurelius (HR Intern)", leaveType = "Vacation Leave", startDate = "2026-07-10", endDate = "2026-07-15", reason = "Family beach trip in Palawan, PH", status = "PENDING"),
        LeaveRequest(employeeName = "Robert Chen", leaveType = "Shift Change", startDate = "2026-07-05", endDate = "2026-07-05", reason = "Requested Manila Dev Shift ➔ Night Ops", status = "PENDING")
    ))

    var correctionRequests = mutableStateOf<List<CorrectionRequest>>(listOf(
        CorrectionRequest(employeeName = "Robert Chen", date = "2026-06-25", requestedTimeIn = "08:00 AM", requestedTimeOut = "05:00 PM", reason = "Power cut at remote home, missed punch out button", status = "PENDING")
    ))

    var reimbursementRequests = mutableStateOf<List<ReimbursementRequest>>(listOf(
        ReimbursementRequest(employeeName = "Sarah Jenkins", title = "Home Office Ergonomic Office Chair", amount = 120.0, status = "APPROVED"),
        ReimbursementRequest(employeeName = "Robert Chen", title = "Remote Internet Broadband reimbursement", amount = 45.0, status = "PENDING")
    ))

    var taskAssignments = mutableStateOf<List<TaskAssignment>>(listOf(
        TaskAssignment(employeeName = "Sarah Jenkins", taskTitle = "Security Audit compliance logs", description = "Review and log secure ledger details on the main dashboard.", status = "PENDING"),
        TaskAssignment(employeeName = "Robert Chen", taskTitle = "Indore Hub operations sync", description = "Host the daily check-in with the HR Intern.", status = "COMPLETED")
    ))

    var announcements = mutableStateOf<List<Announcement>>(listOf(
        Announcement(title = "Indore Hub Hybrid Operations", content = "Flexible timings rules updated. Daily shift targets remain at 8 hours.", date = "2026-06-28"),
        Announcement(title = "Philippine Hub Synergies Integration", content = "Welcoming our Philippine remote workers with added PH local holiday calendar sync!", date = "2026-06-29")
    ))

    var auditLogs = mutableStateOf<List<AuditLog>>(listOf(
        AuditLog(timestamp = "2026-06-28 09:00 AM", user = "System", action = "System initialization completed successfully."),
        AuditLog(timestamp = "2026-06-28 10:15 AM", user = "Aditya Joshi (Director)", action = "Configured remote geofence guidelines to 100 meters.")
    ))

    var feelReports = mutableStateOf<List<FeelReport>>(listOf(
        FeelReport("Sarah Jenkins", 5, "Fabulous energy today, enjoying the hybrid flex sync!", "2026-06-28"),
        FeelReport("Marcus Aurelius (HR Intern)", 4, "Productive audit flow going on.", "2026-06-28")
    ))

    var loansList = mutableStateOf<List<LoanRecord>>(listOf(
        LoanRecord(employeeName = "Sarah Jenkins", type = "Salary Advance", amount = 500.0, monthlyDeduction = 50.0, remaining = 350.0)
    ))

    var teamSchedules = mutableStateOf<List<TeamSchedule>>(listOf(
        // Default schedules for June 2026 (matching the main calendar month)
        // Sarah Jenkins (Engineering)
        TeamSchedule("ts1", "Sarah Jenkins", "Engineering", "2026-06-25", "Manila Dev Shift"),
        TeamSchedule("ts2", "Sarah Jenkins", "Engineering", "2026-06-26", "Manila Dev Shift"),
        TeamSchedule("ts3", "Sarah Jenkins", "Engineering", "2026-06-27", "Off"),
        TeamSchedule("ts4", "Sarah Jenkins", "Engineering", "2026-06-28", "Off"),
        TeamSchedule("ts5", "Sarah Jenkins", "Engineering", "2026-06-29", "Manila Dev Shift"),
        TeamSchedule("ts6", "Sarah Jenkins", "Engineering", "2026-06-30", "Manila Dev Shift"),

        // Robert Chen (Product Management)
        TeamSchedule("ts7", "Robert Chen", "Product Management", "2026-06-25", "Indore Day Flex"),
        TeamSchedule("ts8", "Robert Chen", "Product Management", "2026-06-26", "Indore Day Flex"),
        TeamSchedule("ts9", "Robert Chen", "Product Management", "2026-06-27", "Off"),
        TeamSchedule("ts10", "Robert Chen", "Product Management", "2026-06-28", "Off"),
        TeamSchedule("ts11", "Robert Chen", "Product Management", "2026-06-29", "Indore Day Flex"),
        TeamSchedule("ts12", "Robert Chen", "Product Management", "2026-06-30", "Night Ops"),

        // Marcus Aurelius (HR Intern) (Human Resources)
        TeamSchedule("ts13", "Marcus Aurelius (HR Intern)", "Human Resources", "2026-06-25", "Indore Day Flex"),
        TeamSchedule("ts14", "Marcus Aurelius (HR Intern)", "Human Resources", "2026-06-26", "Indore Day Flex"),
        TeamSchedule("ts15", "Marcus Aurelius (HR Intern)", "Human Resources", "2026-06-27", "Off"),
        TeamSchedule("ts16", "Marcus Aurelius (HR Intern)", "Human Resources", "2026-06-28", "Off"),
        TeamSchedule("ts17", "Marcus Aurelius (HR Intern)", "Human Resources", "2026-06-29", "Indore Day Flex"),
        TeamSchedule("ts18", "Marcus Aurelius (HR Intern)", "Human Resources", "2026-06-30", "Indore Day Flex"),

        // Anjali Sharma (Management)
        TeamSchedule("ts19", "Anjali Sharma", "Management", "2026-06-25", "Manila Dev Shift"),
        TeamSchedule("ts20", "Anjali Sharma", "Management", "2026-06-26", "Manila Dev Shift"),
        TeamSchedule("ts21", "Anjali Sharma", "Management", "2026-06-27", "Off"),
        TeamSchedule("ts22", "Anjali Sharma", "Management", "2026-06-28", "Off"),
        TeamSchedule("ts23", "Anjali Sharma", "Management", "2026-06-29", "Manila Dev Shift"),
        TeamSchedule("ts24", "Anjali Sharma", "Management", "2026-06-30", "Manila Dev Shift"),

        // Aditya Joshi (Director) (Administration)
        TeamSchedule("ts25", "Aditya Joshi (Director)", "Administration", "2026-06-25", "Manila Dev Shift"),
        TeamSchedule("ts26", "Aditya Joshi (Director)", "Administration", "2026-06-26", "Manila Dev Shift"),
        TeamSchedule("ts27", "Aditya Joshi (Director)", "Administration", "2026-06-27", "Off"),
        TeamSchedule("ts28", "Aditya Joshi (Director)", "Administration", "2026-06-28", "Off"),
        TeamSchedule("ts29", "Aditya Joshi (Director)", "Administration", "2026-06-29", "Manila Dev Shift"),
        TeamSchedule("ts30", "Aditya Joshi (Director)", "Administration", "2026-06-30", "Manila Dev Shift")
    ))


    var employeeProfiles = mutableStateOf<List<EmployeeProfile>>(listOf(
        EmployeeProfile(
            id = "COS-2026-0012",
            name = "Sarah Jenkins",
            department = "Engineering",
            position = "Senior Kotlin Developer",
            status = "Regular",
            email = "sarah.j@clauseos.com",
            emergencyContactName = "David Jenkins (Father)",
            emergencyContactPhone = "+91 98765 43210",
            age = 29,
            phoneNumber = "+91 94451 22340",
            picture = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80",
            gender = "Female",
            vacationLeaveBalance = 15,
            sickLeaveBalance = 10,
            maternityLeaveBalance = 105,
            paternityLeaveBalance = 0,
            soloParentLeaveBalance = 7
        ),
        EmployeeProfile(
            id = "COS-2026-0013",
            name = "Marcus Aurelius (HR Intern)",
            department = "Human Resources",
            position = "HR Audit Associate",
            status = "Probationary",
            email = "marcus.a@clauseos.com",
            emergencyContactName = "Lucius Aurelius (Brother)",
            emergencyContactPhone = "+63 912 345 6789",
            age = 23,
            phoneNumber = "+63 915 567 8910",
            picture = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80",
            gender = "Male",
            vacationLeaveBalance = 12,
            sickLeaveBalance = 8,
            maternityLeaveBalance = 0,
            paternityLeaveBalance = 7,
            soloParentLeaveBalance = 0
        ),
        EmployeeProfile(
            id = "COS-2026-0014",
            name = "Robert Chen",
            department = "Product Management",
            position = "Lead Product Manager",
            status = "Regular",
            email = "robert.c@clauseos.com",
            emergencyContactName = "Emily Chen (Wife)",
            emergencyContactPhone = "+91 88888 77777",
            age = 35,
            phoneNumber = "+91 88877 66554",
            picture = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=150&q=80",
            gender = "Male",
            vacationLeaveBalance = 18,
            sickLeaveBalance = 12,
            maternityLeaveBalance = 0,
            paternityLeaveBalance = 7,
            soloParentLeaveBalance = 0
        ),
        EmployeeProfile(
            id = "COS-2026-0015",
            name = "Anjali Sharma",
            department = "Management",
            position = "General Manager",
            status = "Regular",
            email = "anjali.s@clauseos.com",
            emergencyContactName = "Vikram Sharma (Husband)",
            emergencyContactPhone = "+91 99999 88888",
            age = 42,
            phoneNumber = "+91 99911 22334",
            picture = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=150&q=80",
            gender = "Female",
            vacationLeaveBalance = 20,
            sickLeaveBalance = 15,
            maternityLeaveBalance = 105,
            paternityLeaveBalance = 0,
            soloParentLeaveBalance = 7
        ),
        EmployeeProfile(
            id = "COS-2026-0016",
            name = "Aditya Joshi (Director)",
            department = "Administration",
            position = "Director of Operations",
            status = "Regular",
            email = "aditya.j@clauseos.com",
            emergencyContactName = "Kiran Joshi (Wife)",
            emergencyContactPhone = "+91 11111 22222",
            age = 48,
            phoneNumber = "+91 11122 33445",
            picture = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=150&q=80",
            gender = "Male",
            vacationLeaveBalance = 25,
            sickLeaveBalance = 15,
            maternityLeaveBalance = 0,
            paternityLeaveBalance = 7,
            soloParentLeaveBalance = 7
        )
    ))

    // --- COMPLIANCE STATES ---
    var disciplinaryCases = mutableStateOf<List<DisciplinaryCase>>(listOf(
        DisciplinaryCase(
            employeeName = "Sarah Jenkins",
            infractionTitle = "Attendance Misalignment Alert",
            infractionDate = "2026-06-25",
            noticeToExplainDate = "2026-06-26",
            noticeToExplainContent = "Your geofence logging telemetry flagged a 200m mismatch on June 25. Please explain this discrepancy under the attendance policy Clause 4.1.",
            employeeExplanation = "I was working inside the concrete Engineering building which caused temporary GPS signal attenuation and drift.",
            employeeExplanationDate = "2026-06-27",
            status = "EXPLANATION_SUBMITTED",
            severity = "Low"
        ),
        DisciplinaryCase(
            employeeName = "Robert Chen",
            infractionTitle = "Confidentiality Policy Sync",
            infractionDate = "2026-06-10",
            noticeToExplainDate = "2026-06-11",
            noticeToExplainContent = "A public product roadmap leak was reported. Verify standard hands-off logging security.",
            employeeExplanation = "I performed a complete audit and confirmed no internal login credentials or plans were leaked from my devices.",
            employeeExplanationDate = "2026-06-12",
            noticeOfDecisionDate = "2026-06-14",
            noticeOfDecisionContent = "Case resolved. No policy violation found. Recommended routine security log hygiene.",
            status = "CASE_RESOLVED",
            severity = "High"
        )
    ))

    var offboardingClearances = mutableStateOf<List<OffboardingClearance>>(listOf(
        OffboardingClearance(
            employeeName = "Robert Chen",
            separationDate = "2026-08-31",
            department = "Product Management",
            itClearanceStatus = "CLEARED",
            financeClearanceStatus = "PENDING",
            adminClearanceStatus = "CLEARED",
            status = "IN_PROGRESS",
            comments = "Awaiting final travel expense liquidation."
        )
    ))

    var okrRecords = mutableStateOf<List<OkrRecord>>(listOf(
        OkrRecord(
            employeeName = "Sarah Jenkins",
            objective = "Architect Advanced Compliance Guardrails",
            keyResult = "Deliver five modular M3 self-service interfaces with validation",
            targetValue = "5 interfaces",
            currentValue = "3 interfaces",
            progress = 60,
            selfAppraisal = "I have integrated the Room database and styled the forms beautifully, with edge-to-edge Compose templates.",
            managerFeedback = "Excellent structural design. Ensure linter guidelines are fully met."
        ),
        OkrRecord(
            employeeName = "Marcus Aurelius (HR Intern)",
            objective = "Streamline HR Onboarding Audits",
            keyResult = "Audit 100% of employee profiles for mandatory tax/SSN entries",
            targetValue = "100%",
            currentValue = "100%",
            progress = 100,
            selfAppraisal = "Completed the audit for all current personnel.",
            managerFeedback = "Fantastic work, very detailed documentation!",
            status = "ACHIEVED"
        )
    ))

    // 2FA Security State
    var isTwoFactorEnabled = mutableStateOf(false)
    var hasVerifiedTwoFactor = mutableStateOf(false)

    // Productivity, Chat and Birthday features
    var selectedEmployeeForChart = mutableStateOf("Sarah Jenkins")
    var activeRecipient = mutableStateOf("Marcus Aurelius")
    var messagesList = mutableStateOf<List<Message>>(emptyList())
    var birthdayList = mutableStateOf<List<BirthdayInfo>>(emptyList())
    var ticketStatuses = androidx.compose.runtime.mutableStateMapOf<String, String>()

    val productivityRecords = mapOf(
        "Sarah Jenkins" to listOf(
            ProductivityRecord("Mon", 85, 8.2),
            ProductivityRecord("Tue", 92, 8.5),
            ProductivityRecord("Wed", 78, 7.5),
            ProductivityRecord("Thu", 95, 8.8),
            ProductivityRecord("Fri", 88, 8.0),
            ProductivityRecord("Sat", 0, 0.0),
            ProductivityRecord("Sun", 0, 0.0)
        ),
        "Marcus Aurelius (HR Intern)" to listOf(
            ProductivityRecord("Mon", 70, 7.0),
            ProductivityRecord("Tue", 75, 7.5),
            ProductivityRecord("Wed", 80, 8.0),
            ProductivityRecord("Thu", 68, 6.5),
            ProductivityRecord("Fri", 82, 7.8),
            ProductivityRecord("Sat", 0, 0.0),
            ProductivityRecord("Sun", 0, 0.0)
        ),
        "Robert Chen" to listOf(
            ProductivityRecord("Mon", 88, 8.0),
            ProductivityRecord("Tue", 90, 8.2),
            ProductivityRecord("Wed", 94, 8.6),
            ProductivityRecord("Thu", 92, 8.5),
            ProductivityRecord("Fri", 89, 8.1),
            ProductivityRecord("Sat", 0, 0.0),
            ProductivityRecord("Sun", 0, 0.0)
        ),
        "Anjali Sharma" to listOf(
            ProductivityRecord("Mon", 94, 8.5),
            ProductivityRecord("Tue", 96, 8.8),
            ProductivityRecord("Wed", 95, 8.7),
            ProductivityRecord("Thu", 98, 9.0),
            ProductivityRecord("Fri", 97, 8.9),
            ProductivityRecord("Sat", 0, 0.0),
            ProductivityRecord("Sun", 0, 0.0)
        ),
        "Aditya Joshi (Director)" to listOf(
            ProductivityRecord("Mon", 96, 8.8),
            ProductivityRecord("Tue", 98, 9.2),
            ProductivityRecord("Wed", 97, 9.0),
            ProductivityRecord("Thu", 99, 9.5),
            ProductivityRecord("Fri", 95, 8.9),
            ProductivityRecord("Sat", 0, 0.0),
            ProductivityRecord("Sun", 0, 0.0)
        )
    )

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TimeTrackerRepository(db.timeLogDao(), db.shiftConfigDao())

        dossierDocuments = db.dossierDao().getAllDocuments()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allTimeLogs = repository.allTimeLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        pendingTimeLogs = repository.pendingTimeLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Default Config Flow mapping
        shiftConfig = repository.shiftConfigFlow
            .map { it ?: ShiftConfigEntity() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShiftConfigEntity())

        // Start by prefilling databases if blank and determining today's holiday
        viewModelScope.launch {
            repository.prefillMockData()
            checkTodayHoliday()
            refreshActiveLog()
            startTimer()
            initMessages()
            initBirthdays()
            fetchWeatherForecast(selectedWeatherCity.value)
            addNotification("System", "Ready for secure login. Select a role to get started.", isAlert = false)
        }
    }

    private fun initMessages() {
        messagesList.value = listOf(
            Message(
                sender = "Marcus Aurelius (HR Intern)",
                recipient = "Sarah Jenkins",
                text = "Hi Sarah, do you have the compliance spreadsheet for the Indore hub ready for audit?",
                timestamp = "09:15 AM"
            ),
            Message(
                sender = "Sarah Jenkins",
                recipient = "Marcus Aurelius (HR Intern)",
                text = "Yes Marcus! Just finished logging. You can view it in the Ledger tab. It's completely synchronized.",
                timestamp = "09:18 AM"
            ),
            Message(
                sender = "Robert Chen",
                recipient = "Sarah Jenkins",
                text = "Excellent compliance score this week, Sarah! Keep up the great cycle-to-work pace.",
                timestamp = "10:30 AM"
            ),
            Message(
                sender = "Aditya Joshi (Director)",
                recipient = "All Employees",
                text = "Reminder: Please verify your remote location settings before punching in. Indore geo-fencing rules are active.",
                timestamp = "Yesterday"
            ),
            Message(
                sender = "Michael Vance",
                recipient = "All Employees",
                text = "Thanks for the early birthday greetings everyone! Indore sweets are on me at the break room! 🍬",
                timestamp = "10:05 AM"
            )
        )
    }

    private fun initBirthdays() {
        birthdayList.value = listOf(
            BirthdayInfo("Michael Vance", "Co-worker (QA)", "June 28", isToday = true, daysUntil = 0),
            BirthdayInfo("Sarah Jenkins", "Employee (Dev)", "June 30", isToday = false, daysUntil = 2),
            BirthdayInfo("Aditya Joshi (Director)", "Admin/HR", "July 05", isToday = false, daysUntil = 7),
            BirthdayInfo("Robert Chen", "Supervisor (PM)", "April 15", isToday = false, daysUntil = 290),
            BirthdayInfo("Anjali Sharma", "General Manager", "Sept 12", isToday = false, daysUntil = 76)
        )
    }

    fun sendMessage(recipient: String, text: String) {
        if (text.isBlank()) return
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeStr = sdf.format(Date())
        val newMsg = Message(
            sender = currentUserName.value,
            recipient = recipient,
            text = text,
            timestamp = timeStr
        )
        messagesList.value = messagesList.value + newMsg
    }

    fun sendCoverRequest(recipient: String, date: String, shiftName: String) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeStr = sdf.format(Date())
        val requester = currentUserName.value
        val text = "Hi! Could you please cover my shift on $date ($shiftName)?"
        val newMsg = Message(
            sender = requester,
            recipient = recipient,
            text = text,
            timestamp = timeStr,
            isSwapRequest = true,
            swapDate = date,
            swapShiftName = shiftName,
            swapStatus = "PENDING",
            swapRequester = requester,
            swapCoverer = ""
        )
        messagesList.value = messagesList.value + newMsg
    }

    fun respondToCoverRequest(messageId: String, accept: Boolean) {
        val currentMsgs = messagesList.value.toMutableList()
        val index = currentMsgs.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val msg = currentMsgs[index]
            val nextStatus = if (accept) "ACCEPTED" else "REJECTED"
            val updatedMsg = msg.copy(
                swapStatus = nextStatus,
                swapCoverer = currentUserName.value
            )
            currentMsgs[index] = updatedMsg
            messagesList.value = currentMsgs
            
            if (accept) {
                addNotification("Cover Request Accepted", "${currentUserName.value} accepted cover request for ${msg.swapRequester} on ${msg.swapDate}. Awaiting Supervisor approval.", isAlert = false)
            } else {
                addNotification("Cover Request Declined", "${currentUserName.value} declined cover request for ${msg.swapRequester} on ${msg.swapDate}.", isAlert = false)
            }
        }
    }

    fun approveShiftCover(messageId: String, approvedBy: String) {
        val currentMsgs = messagesList.value.toMutableList()
        val index = currentMsgs.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val msg = currentMsgs[index]
            val updatedMsg = msg.copy(swapStatus = "APPROVED")
            currentMsgs[index] = updatedMsg
            messagesList.value = currentMsgs
            
            // Reassign the shift!
            val currentSchedules = teamSchedules.value.toMutableList()
            val schedIndex = currentSchedules.indexOfFirst { 
                it.employeeName == msg.swapRequester && it.date == msg.swapDate 
            }
            if (schedIndex != -1) {
                val originalSched = currentSchedules[schedIndex]
                currentSchedules[schedIndex] = originalSched.copy(employeeName = msg.swapCoverer)
                teamSchedules.value = currentSchedules
                
                // Add logs & notifications
                addAuditLog(approvedBy, "Approved shift cover: ${msg.swapCoverer} covers ${msg.swapRequester} on ${msg.swapDate}.")
                addNotification("Shift Cover Approved", "${msg.swapCoverer} is now scheduled for ${msg.swapShiftName} on ${msg.swapDate}.", isAlert = false)
            }
        }
    }

    fun rejectShiftCover(messageId: String, rejectedBy: String) {
        val currentMsgs = messagesList.value.toMutableList()
        val index = currentMsgs.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val msg = currentMsgs[index]
            val updatedMsg = msg.copy(swapStatus = "REJECTED")
            currentMsgs[index] = updatedMsg
            messagesList.value = currentMsgs
            
            // Add logs & notifications
            addAuditLog(rejectedBy, "Rejected shift cover request for ${msg.swapRequester} on ${msg.swapDate}.")
            addNotification("Shift Cover Rejected", "Request for ${msg.swapCoverer} to cover ${msg.swapRequester} on ${msg.swapDate} was declined.", isAlert = true)
        }
    }

    private fun checkTodayHoliday() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val userCountry = getUserCountryCode()
        val holiday = localHolidays.value.find { 
            it.date == todayStr && (it.country == "ALL" || it.country == userCountry) 
        }
        todayHoliday.value = holiday
        holiday?.let { h ->
            val regionName = if (userCountry == "PH") "Philippines" else "India/Indore"
            addNotification("$regionName Holiday Today", "Today is ${h.name}! Holiday rates may apply. 🎉", isAlert = false)
        }
    }

    // Register New User
    fun registerUser(username: String, passcode: String, name: String, role: String, companyNameInput: String, companyCodeInput: String): String? {
        val normUser = username.trim().lowercase()
        if (normUser.isBlank() || passcode.trim().isBlank() || name.trim().isBlank()) {
            return "Please fill in all registration fields."
        }
        if (companyCodeInput.trim() != companyCode.value) {
            return "Invalid company registration code. (Hint: Use CLAUSE99)"
        }
        if (registeredUsers.value.any { it.username == normUser }) {
            return "Username already registered."
        }
        
        val newUser = RegisteredUser(
            username = normUser,
            passcode = passcode.trim(),
            name = name.trim(),
            role = role,
            companyName = companyNameInput.trim(),
            companyCode = companyCodeInput.trim()
        )
        registeredUsers.value = registeredUsers.value + newUser
        
        // Add profile as well!
        val newProfile = EmployeeProfile(
            id = "COS-2026-00" + (17 + employeeProfiles.value.size),
            name = name.trim(),
            department = "Unassigned",
            position = if (role == "ADMIN_HR") "HR Specialist" else "Associate",
            status = "Regular",
            email = "${normUser}@clauseos.com",
            emergencyContactName = "Emergency Contact Name",
            emergencyContactPhone = "Contact Number"
        )
        employeeProfiles.value = employeeProfiles.value + newProfile
        
        addAuditLog("System", "Registered new user: ${name.trim()} with role $role")
        addNotification("Security Check", "Registration successful! You can now log in.", isAlert = false)
        return null
    }

    // Role-based Multi-tier Login
    fun attemptLogin(username: String, passcode: String): String? {
        val normUser = username.trim().lowercase()
        val normPass = passcode.trim()

        val matched = registeredUsers.value.find { it.username == normUser && it.passcode == normPass }

        return if (matched != null) {
            currentUserRole.value = matched.role
            currentUserName.value = matched.name
            currentUserUsername.value = normUser
            isLoggedIn.value = true
            
            // Auto setup HR access toggle
            isAdminMode.value = (matched.role != "EMPLOYEE")
            currentScreen.value = if (matched.role == "EMPLOYEE") "clock" else "hr_approval"
            
            // Sync user profile name with config if employee
            if (matched.role == "EMPLOYEE") {
                viewModelScope.launch {
                    val config = repository.getShiftConfig()
                    repository.saveShiftConfig(config.copy(employeeName = matched.name))
                }
            }

            addAuditLog(matched.name, "Logged in successfully under secure role.")
            addNotification("Security Check", "Logged in successfully as ${matched.name} (${matched.role})", isAlert = false)
            refreshActiveLog()
            null // Return null on success
        } else {
            "Invalid username or password. (Hint: Try employee/emp123 or admin/admin123)"
        }
    }

    fun logout() {
        addAuditLog(currentUserName.value, "Logged out of current session.")
        isLoggedIn.value = false
        currentUserRole.value = "EMPLOYEE"
        currentUserName.value = "Sarah Jenkins"
        currentUserUsername.value = "employee"
        isAdminMode.value = false
        currentScreen.value = "clock"
        addNotification("Security Check", "Successfully signed out of session.", isAlert = false)
    }

    // Audit logs logger
    fun addAuditLog(user: String, action: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
        val dateStr = sdf.format(Date())
        val newLog = AuditLog(timestamp = dateStr, user = user, action = action)
        auditLogs.value = listOf(newLog) + auditLogs.value
    }

    // Leaves Management
    fun calculateDaysBetween(start: String, end: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val startDate = sdf.parse(start)
            val endDate = sdf.parse(end)
            if (startDate != null && endDate != null) {
                val diff = endDate.time - startDate.time
                val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
                if (days > 0) days else 1
            } else 1
        } catch (e: Exception) {
            1
        }
    }

    fun fileLeaveRequest(leaveType: String, startDate: String, endDate: String, reason: String): String? {
        val profile = employeeProfiles.value.find { it.name == currentUserName.value }
        if (profile == null) {
            return "Employee profile not found."
        }
        val duration = calculateDaysBetween(startDate, endDate)
        if (duration <= 0) {
            return "Invalid leave duration."
        }

        // Gender validations for statutory categories
        if (leaveType == "Maternity Leave" && profile.gender != "Female") {
            return "Maternity Leave is legally protected for Female employees only under statutory regulations."
        }
        if (leaveType == "Paternity Leave" && profile.gender != "Male") {
            return "Paternity Leave is legally protected for Male employees only under statutory regulations."
        }

        // Accrual checks
        val balance = when (leaveType) {
            "Vacation Leave" -> profile.vacationLeaveBalance
            "Sick Leave" -> profile.sickLeaveBalance
            "Maternity Leave" -> profile.maternityLeaveBalance
            "Paternity Leave" -> profile.paternityLeaveBalance
            "Solo Parent Leave" -> profile.soloParentLeaveBalance
            else -> 9999
        }
        if (duration > balance) {
            return "Insufficient $leaveType balance. Requested $duration days but only $balance days left."
        }

        val newReq = LeaveRequest(
            employeeName = currentUserName.value,
            leaveType = leaveType,
            startDate = startDate,
            endDate = endDate,
            reason = reason,
            status = "PENDING"
        )
        leaveRequests.value = leaveRequests.value + newReq
        addAuditLog(currentUserName.value, "Filed $leaveType from $startDate to $endDate ($duration days)")
        addNotification("Self-Service", "$leaveType request filed successfully.", isAlert = false)
        return null
    }

    fun approveLeave(id: String) {
        leaveRequests.value = leaveRequests.value.map { req ->
            if (req.id == id) {
                val duration = calculateDaysBetween(req.startDate, req.endDate)
                employeeProfiles.value = employeeProfiles.value.map { profile ->
                    if (profile.name == req.employeeName) {
                        when (req.leaveType) {
                            "Vacation Leave" -> profile.copy(vacationLeaveBalance = (profile.vacationLeaveBalance - duration).coerceAtLeast(0))
                            "Sick Leave" -> profile.copy(sickLeaveBalance = (profile.sickLeaveBalance - duration).coerceAtLeast(0))
                            "Maternity Leave" -> profile.copy(maternityLeaveBalance = (profile.maternityLeaveBalance - duration).coerceAtLeast(0))
                            "Paternity Leave" -> profile.copy(paternityLeaveBalance = (profile.paternityLeaveBalance - duration).coerceAtLeast(0))
                            "Solo Parent Leave" -> profile.copy(soloParentLeaveBalance = (profile.soloParentLeaveBalance - duration).coerceAtLeast(0))
                            else -> profile
                        }
                    } else profile
                }
                addAuditLog(currentUserName.value, "Approved ${req.leaveType} for ${req.employeeName} ($duration Days)")
                addNotification("Leave Approved", "${req.employeeName}'s ${req.leaveType} has been approved.", isAlert = false)
                req.copy(status = "APPROVED")
            } else req
        }
    }

    fun rejectLeave(id: String) {
        leaveRequests.value = leaveRequests.value.map {
            if (it.id == id) {
                addAuditLog(currentUserName.value, "Rejected leave for ${it.employeeName}")
                addNotification("Leave Rejected", "${it.employeeName}'s ${it.leaveType} was rejected.", isAlert = true)
                it.copy(status = "REJECTED")
            } else it
        }
    }

    // Attendance Correction & Punch Dispute Requests
    fun fileCorrectionRequest(
        date: String,
        requestedTimeIn: String,
        requestedTimeOut: String,
        reason: String,
        disputeType: String = "GPS Drift",
        isDispute: Boolean = false,
        telemetry: String = ""
    ) {
        val newReq = CorrectionRequest(
            employeeName = currentUserName.value,
            date = date,
            requestedTimeIn = requestedTimeIn,
            requestedTimeOut = requestedTimeOut,
            reason = reason,
            status = "PENDING",
            disputeType = disputeType,
            originalTelemetry = if (isDispute) telemetry else "Manual correction entry",
            isPunchDispute = isDispute
        )
        correctionRequests.value = correctionRequests.value + newReq
        addAuditLog(currentUserName.value, "Requested ${if (isDispute) "GPS dispute resolution" else "time correction"} for date $date")
        addNotification("Self-Service", "${if (isDispute) "Dispute ticket" else "Correction request"} submitted successfully.", isAlert = false)
    }

    fun parseDateTimeToMillis(dateStr: String, timeStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
            sdf.parse("$dateStr $timeStr")?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun approveCorrection(id: String) {
        correctionRequests.value = correctionRequests.value.map { req ->
            if (req.id == id) {
                addAuditLog(currentUserName.value, "Approved time correction dispute for ${req.employeeName} on ${req.date}")
                addNotification("Dispute Resolved", "Attendance dispute resolved for ${req.employeeName}.", isAlert = false)
                
                // Real DB update!
                viewModelScope.launch {
                    val timeInMs = parseDateTimeToMillis(req.date, req.requestedTimeIn)
                    val timeOutMs = parseDateTimeToMillis(req.date, req.requestedTimeOut)
                    
                    val existing = allTimeLogs.value.find { it.employeeName == req.employeeName && it.date == req.date }
                    val logToSave = existing?.copy(
                        timeIn = timeInMs,
                        timeOut = timeOutMs,
                        isApproved = "APPROVED"
                    ) ?: TimeLogEntity(
                        date = req.date,
                        employeeName = req.employeeName,
                        timeIn = timeInMs,
                        timeOut = timeOutMs,
                        isApproved = "APPROVED"
                    )
                    repository.insertOrUpdateTimeLog(logToSave)
                    refreshActiveLog()
                }
                
                req.copy(status = "APPROVED")
            } else req
        }
    }

    fun rejectCorrection(id: String) {
        correctionRequests.value = correctionRequests.value.map {
            if (it.id == id) {
                addAuditLog(currentUserName.value, "Rejected time correction for ${it.employeeName}")
                addNotification("Correction Rejected", "Attendance correction/dispute rejected for ${it.employeeName}.", isAlert = true)
                it.copy(status = "REJECTED")
            } else it
        }
    }

    // --- DISCIPLINARY & DUE PROCESS HELPERS ---
    fun issueNoticeToExplain(employeeName: String, infractionTitle: String, date: String, content: String, severity: String, link: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val newCase = DisciplinaryCase(
            employeeName = employeeName,
            infractionTitle = infractionTitle,
            infractionDate = date,
            noticeToExplainDate = todayStr,
            noticeToExplainContent = content,
            severity = severity,
            policyViabilityLink = link,
            status = "NOTICE_TO_EXPLAIN_ISSUED"
        )
        disciplinaryCases.value = disciplinaryCases.value + newCase
        addAuditLog(currentUserName.value, "Issued Notice to Explain to $employeeName for $infractionTitle")
        addNotification("Compliance Alert", "Disciplinary case logged for $employeeName.", isAlert = true)
    }

    fun submitEmployeeExplanation(caseId: String, explanation: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        disciplinaryCases.value = disciplinaryCases.value.map {
            if (it.id == caseId) {
                addAuditLog(currentUserName.value, "Submitted disciplinary explanation for ${it.employeeName}")
                it.copy(
                    employeeExplanation = explanation,
                    employeeExplanationDate = todayStr,
                    status = "EXPLANATION_SUBMITTED"
                )
            } else it
        }
    }

    fun issueNoticeOfDecision(caseId: String, decisionContent: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        disciplinaryCases.value = disciplinaryCases.value.map {
            if (it.id == caseId) {
                addAuditLog(currentUserName.value, "Issued Notice of Decision for ${it.employeeName}")
                addNotification("Case Resolved", "Disciplinary case for ${it.employeeName} has been updated.", isAlert = false)
                it.copy(
                    noticeOfDecisionContent = decisionContent,
                    noticeOfDecisionDate = todayStr,
                    status = "CASE_RESOLVED"
                )
            } else it
        }
    }

    // --- OFFBOARDING & CLEARANCES HELPERS ---
    fun startOffboarding(employeeName: String, separationDate: String, department: String) {
        val newClearance = OffboardingClearance(
            employeeName = employeeName,
            separationDate = separationDate,
            department = department,
            status = "IN_PROGRESS"
        )
        offboardingClearances.value = offboardingClearances.value + newClearance
        addAuditLog(currentUserName.value, "Initialized offboarding clearance tracker for $employeeName")
        addNotification("Offboarding", "Clearance workflow started for $employeeName.", isAlert = false)
    }

    fun updateDepartmentClearance(clearanceId: String, dept: String, isCleared: Boolean) {
        offboardingClearances.value = offboardingClearances.value.map {
            if (it.id == clearanceId) {
                val updated = when (dept) {
                    "IT" -> it.copy(itClearanceStatus = if (isCleared) "CLEARED" else "PENDING")
                    "Finance" -> it.copy(financeClearanceStatus = if (isCleared) "CLEARED" else "PENDING")
                    "Admin" -> it.copy(adminClearanceStatus = if (isCleared) "CLEARED" else "PENDING")
                    else -> it
                }
                
                // Auto check if fully cleared
                val fullyCleared = updated.itClearanceStatus == "CLEARED" &&
                                   updated.financeClearanceStatus == "CLEARED" &&
                                   updated.adminClearanceStatus == "CLEARED"
                
                updated.copy(
                    coeReady = fullyCleared,
                    finalPayReady = fullyCleared,
                    status = if (fullyCleared) "FULLY_CLEARED" else "IN_PROGRESS"
                )
            } else it
        }
    }

    // --- OKRS & CONTINUOUS APPRAISAL HELPERS ---
    fun logOkr(objective: String, keyResult: String, target: String, current: String, progress: Int) {
        val newOkr = OkrRecord(
            employeeName = currentUserName.value,
            objective = objective,
            keyResult = keyResult,
            targetValue = target,
            currentValue = current,
            progress = progress,
            status = "ACTIVE"
        )
        okrRecords.value = okrRecords.value + newOkr
        addAuditLog(currentUserName.value, "Logged OKR Objective: $objective")
    }

    fun updateOkrProgress(okrId: String, currentVal: String, progress: Int, selfAppraisal: String) {
        okrRecords.value = okrRecords.value.map {
            if (it.id == okrId) {
                it.copy(
                    currentValue = currentVal,
                    progress = progress,
                    selfAppraisal = selfAppraisal,
                    status = if (progress >= 100) "ACHIEVED" else "ACTIVE"
                )
            } else it
        }
    }

    fun submitManagerOkrFeedback(okrId: String, feedback: String) {
        okrRecords.value = okrRecords.value.map {
            if (it.id == okrId) {
                it.copy(managerFeedback = feedback)
            } else it
        }
    }

    fun getApprovedLeaveOnDate(employeeName: String, dateStr: String): LeaveRequest? {
        return leaveRequests.value.find { req ->
            if (req.employeeName == employeeName && req.status == "APPROVED") {
                val target = dateStr
                val start = req.startDate
                val end = req.endDate
                target >= start && target <= end
            } else false
        }
    }

    // Reimbursements Management
    fun fileReimbursement(title: String, amount: Double) {
        val newReq = ReimbursementRequest(
            employeeName = currentUserName.value,
            title = title,
            amount = amount,
            status = "PENDING"
        )
        reimbursementRequests.value = reimbursementRequests.value + newReq
        addAuditLog(currentUserName.value, "Requested reimbursement for: $title ($$amount)")
        addNotification("Self-Service", "Reimbursement claim submitted.", isAlert = false)
    }

    fun approveReimbursement(id: String) {
        reimbursementRequests.value = reimbursementRequests.value.map {
            if (it.id == id) {
                addAuditLog(currentUserName.value, "Approved reimbursement of $${it.amount} for ${it.employeeName}")
                addNotification("Reimbursement Release", "Claim approved for ${it.employeeName}.", isAlert = false)
                it.copy(status = "APPROVED")
            } else it
        }
    }

    fun rejectReimbursement(id: String) {
        reimbursementRequests.value = reimbursementRequests.value.map {
            if (it.id == id) {
                addAuditLog(currentUserName.value, "Rejected reimbursement of $${it.amount} for ${it.employeeName}")
                it.copy(status = "REJECTED")
            } else it
        }
    }

    // Assign Tasks
    fun assignTask(employeeName: String, title: String, description: String) {
        val newReq = TaskAssignment(
            employeeName = employeeName,
            taskTitle = title,
            description = description,
            status = "PENDING"
        )
        taskAssignments.value = taskAssignments.value + newReq
        addAuditLog(currentUserName.value, "Assigned new task to $employeeName: $title")
        addNotification("Task Assignment", "Assigned task to $employeeName.", isAlert = false)
    }

    fun completeTask(id: String) {
        taskAssignments.value = taskAssignments.value.map {
            if (it.id == id) {
                addAuditLog(currentUserName.value, "Completed task: ${it.taskTitle}")
                addNotification("Task Completed", "Task '${it.taskTitle}' is now complete.", isAlert = false)
                it.copy(status = "COMPLETED")
            } else it
        }
    }

    // Feel Report
    fun submitFeelReport(score: Int, note: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(Date())
        val newReq = FeelReport(
            employeeName = currentUserName.value,
            score = score,
            note = note,
            date = dateStr
        )
        feelReports.value = feelReports.value.filter { it.employeeName != currentUserName.value } + newReq
        addAuditLog(currentUserName.value, "Submitted daily feels rating: $score")
        addNotification("Core HR Feedback", "Thank you for reporting how you feel! HR monitors this closely.", isAlert = false)
    }

    // Loans Management
    fun submitLoanRequest(type: String, amount: Double) {
        val monthly = amount / 10.0 // 10-month division
        val newReq = LoanRecord(
            employeeName = currentUserName.value,
            type = type,
            amount = amount,
            monthlyDeduction = monthly,
            remaining = amount
        )
        loansList.value = loansList.value + newReq
        addAuditLog(currentUserName.value, "Requested $type of $$amount")
        addNotification("Payroll Loans", "$type of $$amount filed securely.", isAlert = false)
    }

    // Create Announcement
    fun createAnnouncement(title: String, content: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(Date())
        val newReq = Announcement(title = title, content = content, date = dateStr)
        announcements.value = listOf(newReq) + announcements.value
        addAuditLog(currentUserName.value, "Published company announcement: $title")
        addNotification("Company Bulletin", "NEW BULLET: $title", isAlert = false)
    }

    // Update Profile Information
    fun updateProfileInfo(
        emergencyContact: String,
        contactPhone: String,
        email: String,
        phoneNumber: String,
        address: String,
        preferredName: String,
        personalEmail: String,
        picture: String,
        emergencyRelationship: String
    ) {
        employeeProfiles.value = employeeProfiles.value.map {
            if (it.name == currentUserName.value) {
                it.copy(
                    emergencyContactName = emergencyContact,
                    emergencyContactPhone = contactPhone,
                    email = email,
                    phoneNumber = phoneNumber,
                    address = address,
                    preferredName = preferredName,
                    personalEmail = personalEmail,
                    picture = picture,
                    emergencyRelationship = emergencyRelationship
                )
            } else it
        }
        addAuditLog(currentUserName.value, "Updated emergency contact and personal profile details.")
        addNotification("Self-Service", "Successfully updated personal profile and contact details.", isAlert = false)
    }

    fun addDocumentToProfile(profileId: String, documentName: String) {
        employeeProfiles.value = employeeProfiles.value.map {
            if (it.id == profileId) {
                if (!it.documents.contains(documentName)) {
                    it.copy(documents = it.documents + documentName)
                } else {
                    it
                }
            } else {
                it
            }
        }
        val targetProfile = employeeProfiles.value.find { it.id == profileId }
        val name = targetProfile?.name ?: ""
        addAuditLog(currentUserName.value, "Uploaded document '$documentName' to $name's Dossier.")
        addNotification("Core HR", "Uploaded $documentName to $name's secure vault.", isAlert = false)
    }

    fun toggleOfflineMode(offline: Boolean) {
        isMockOffline.value = offline
        if (offline) {
            addNotification("Network", "Working in Offline Simulation. Logs stored locally in room database.", isAlert = true)
        } else {
            addNotification("Network", "Network restored. Performing background cloud sync...", isAlert = false)
            performSync()
        }
    }

    fun addNotification(title: String, message: String, isAlert: Boolean = false) {
        val newNotif = NotificationItem(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            isAlert = isAlert
        )
        notifications.value = listOf(newNotif) + notifications.value.take(19) // Keep last 20
    }

    fun dismissNotification(id: String) {
        notifications.value = notifications.value.filter { it.id != id }
    }

    fun refreshActiveLog() {
        viewModelScope.launch {
            // Find active log corresponding to the currently logged in employee
            val allLogsList = repository.allTimeLogs.first()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = sdf.format(Date())
            _activeTimeLog.value = allLogsList.find { 
                it.employeeName == currentUserName.value && it.date == dateStr && it.timeOut == null 
            }
        }
    }

    // Handles any clock punch event with built-in security checks
    fun handlePunch(actionType: String) {
        // 1. Rooted check
        if (isRootedActive.value) {
            addAuditLog(currentUserName.value, "BLOCKED PUNCH: Rooted/jailbroken device detected.")
            addNotification("CyberSec Integrity", "PUNCH BLOCKED: Device Integrity Check Failed (Device is rooted/jailbroken).", isAlert = true)
            return
        }

        // 2. Device register lock check
        if (isDeviceVerificationEnabled.value && currentSimulatedDevice.value != registeredDeviceId.value) {
            addAuditLog(currentUserName.value, "BLOCKED PUNCH: Unapproved hardware device '${currentSimulatedDevice.value}' used.")
            addNotification("Device Lock", "PUNCH BLOCKED: Terminal not registered for this account. Allowed device ID: ${registeredDeviceId.value}", isAlert = true)
            return
        }

        // 3. Geofencing check
        if (simulatedDistance.value > geofenceRadius.value) {
            addAuditLog(currentUserName.value, "BLOCKED PUNCH: Geofence violation. Distance: ${simulatedDistance.value}m, Max Radius: ${geofenceRadius.value}m.")
            addNotification("Geofence", "PUNCH BLOCKED: You are outside the geofence perimeter (${simulatedDistance.value}m from hub, perimeter is ${geofenceRadius.value}m).", isAlert = true)
            return
        }

        // 4. Biometric Face Recognition Check (If enabled and not yet completed)
        if (isFaceRecognitionEnabled.value && showFaceScannerForAction.value == null) {
            // Open the face recognition scan modal
            showFaceScannerForAction.value = actionType
            return
        }

        // 5. Impossible Travel Check (Simulation trigger)
        if (isImpossibleTravelTriggered.value) {
            addAuditLog(currentUserName.value, "SUSPICIOUS AUDIT: Impossible travel alert! Sarah Jenkins clocked in Manila (14.59) and Indore (22.71) in 2 mins.")
            addNotification("Suspicious Alert", "🚨 IMPOSSIBLE TRAVEL DETECTED: Logged simultaneous presence in Manila & Indore. Audit flag raised!", isAlert = true)
        }

        // 6. Mock GPS Check
        if (isMockGpsActive.value) {
            addAuditLog(currentUserName.value, "SUSPICIOUS AUDIT: Spoofed mock coordinates detected via MockProvider interface.")
            addNotification("Mock GPS Guard", "⚠️ SECURITY ALERT: Mock GPS provider active! Clock log flagged for HR manual inspection.", isAlert = true)
        }

        // Proceed to register punch
        viewModelScope.launch {
            // Ensure repository uses the correct logged-in user name
            val config = repository.getShiftConfig()
            if (config.employeeName != currentUserName.value) {
                repository.saveShiftConfig(config.copy(employeeName = currentUserName.value))
            }

            val result = repository.registerPunch(actionType, isMockOffline.value)
            refreshActiveLog()
            
            // Check for late check-in (clocking in after 09:15 AM)
            if (actionType == "TIME_IN") {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val min = Calendar.getInstance().get(Calendar.MINUTE)
                if (hour > 9 || (hour == 9 && min > 15)) {
                    addNotification("Late Shift Alert", "⏰ LATE ATTENDANCE: Checked in late at ${String.format("%02d:%02d", hour, min)} AM. Standard shift starts at 09:00 AM.", isAlert = true)
                } else {
                    addNotification("Shift Alert", "Checked in on-time! Have a productive shift.", isAlert = false)
                }
            }

            val isAlert = result.contains("Please") || result.contains("Already")
            addNotification("Punch Clock", result, isAlert = isAlert)

            // Trigger real-time policy alerts
            if (actionType == "LUNCH_OUT") {
                addNotification("Policy Guard", "Lunch break logged. Indore policy rules recommend 60 mins break.", isAlert = false)
            } else if (actionType == "BREAK_OUT") {
                addNotification("Policy Guard", "Tea/Coffee break logged. Rest up to 30 minutes.", isAlert = false)
            }
        }
    }

    // Secondary bypass handle for completed scans
    fun handlePunchAfterFaceRecognition(actionType: String) {
        showFaceScannerForAction.value = null
        if (isFaceScanMismatched.value) {
            addAuditLog(currentUserName.value, "BLOCKED PUNCH: Face match biometric verification mismatch.")
            addNotification("Security Shield", "PUNCH BLOCKED: Face Recognition Match failed. Unknown person or spoofing pattern detected.", isAlert = true)
            return
        }
        
        // Match succeeded, temporarily bypass face check and perform actual punch
        isFaceRecognitionEnabled.value = false
        handlePunch(actionType)
        isFaceRecognitionEnabled.value = true
    }

    // Simulates syncing unsynced records to cloud database
    fun performSync() {
        if (isMockOffline.value) {
            addNotification("Sync Error", "Cannot sync while offline. Disable Mock Offline in settings/header first.", isAlert = true)
            return
        }

        viewModelScope.launch {
            isSyncing.value = true
            syncMessage.value = "Establishing handshake..."
            delay(600)
            syncMessage.value = "Pushing offline transactions..."
            
            val count = repository.syncOfflineRecords()
            isSyncing.value = false
            
            if (count > 0) {
                addNotification("Payroll Sync", "Synchronized $count workout & punch sessions to central server!", isAlert = false)
            } else {
                addNotification("Payroll Sync", "Database is already fully up-to-date.", isAlert = false)
            }
        }
    }

    // HR Operations: Approve log
    fun approveLog(logId: Long) {
        viewModelScope.launch {
            val log = repository.getTimeLogById(logId)
            if (log != null) {
                val updated = log.copy(isApproved = "APPROVED", rejectionReason = null)
                repository.insertOrUpdateTimeLog(updated)
                addNotification("HR Approval", "Approved shift record for ${log.employeeName} (${log.date})", isAlert = false)
            }
        }
    }

    // HR Operations: Reject log
    fun rejectLog(logId: Long, reason: String) {
        viewModelScope.launch {
            val log = repository.getTimeLogById(logId)
            if (log != null) {
                val updated = log.copy(isApproved = "REJECTED", rejectionReason = reason)
                repository.insertOrUpdateTimeLog(updated)
                addNotification("HR Audit", "Rejected shift record for ${log.employeeName} (${log.date}): $reason", isAlert = true)
            }
        }
    }

    // HR Operations: Edit punch directly
    fun updateTimeLogDetails(log: TimeLogEntity) {
        viewModelScope.launch {
            repository.insertOrUpdateTimeLog(log)
            refreshActiveLog()
            addNotification("HR Operations", "Retroactively updated work records for ${log.employeeName}.", isAlert = false)
        }
    }

    // HR Operations: Delete log
    fun deleteLog(log: TimeLogEntity) {
        viewModelScope.launch {
            repository.deleteTimeLog(log)
            refreshActiveLog()
            addNotification("HR Operations", "Deleted shift record for ${log.employeeName}.", isAlert = true)
        }
    }

    // Configuration save
    fun saveConfig(config: ShiftConfigEntity) {
        viewModelScope.launch {
            repository.saveShiftConfig(config)
            refreshActiveLog()
            addNotification("Settings", "Custom rules and shift definitions updated successfully.", isAlert = false)
        }
    }

    // Clears all database logs
    fun clearAllLogs() {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            db.timeLogDao().clearAllLogs()
            refreshActiveLog()
            addNotification("System", "Completely purged historical databases.", isAlert = true)
        }
    }

    // Live Clock timer logic to compute elapsed work hours
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val log = _activeTimeLog.value
                val config = shiftConfig.value

                if (log != null && log.timeIn != null && log.timeOut == null) {
                    val now = System.currentTimeMillis()
                    val totalElapsed = now - log.timeIn
                    
                    val lunchElapsed = if (log.lunchOut != null) {
                        val end = log.lunchIn ?: now
                        end - log.lunchOut
                    } else 0L

                    val breakElapsed = if (log.breakOut != null) {
                        val end = log.breakIn ?: now
                        end - log.breakOut
                    } else 0L

                    val actualWorkMillis = totalElapsed - lunchElapsed - breakElapsed
                    activeTimerString.value = formatDuration(actualWorkMillis)

                    // Monitor Policy Breaches
                    if (log.breakOut != null && log.breakIn == null) {
                        val activeBreakMins = (now - log.breakOut) / 60000
                        val targetBreak = config.breakDurationMinutes
                        breakTimerString.value = "On Break: ${formatDuration(now - log.breakOut)}"
                        if (activeBreakMins >= targetBreak && activeBreakMins % 5 == 0L) {
                            addNotification("Break Warning", "Break duration is currently $activeBreakMins mins (max: $targetBreak).", isAlert = true)
                        }
                    } else {
                        breakTimerString.value = ""
                    }

                    if (log.lunchOut != null && log.lunchIn == null) {
                        val activeLunchMins = (now - log.lunchOut) / 60000
                        val targetLunch = config.lunchDurationMinutes
                        breakTimerString.value = "On Lunch: ${formatDuration(now - log.lunchOut)}"
                        if (activeLunchMins >= targetLunch && activeLunchMins % 10 == 0L) {
                            addNotification("Lunch Warning", "Lunch duration is currently $activeLunchMins mins (max: $targetLunch).", isAlert = true)
                        }
                    }

                    val targetHours = config.shiftDurationHours
                    val actualWorkHours = actualWorkMillis.toFloat() / 3600000f
                    if (actualWorkHours >= targetHours && actualWorkHours - targetHours < 0.01f) {
                        addNotification("Shift Complete", "Target shift duration reached! Clock out recommended.", isAlert = false)
                    } else if (actualWorkHours > targetHours) {
                        val otDiff = actualWorkHours - targetHours
                        // Notify on initial overtime entry
                        if (otDiff < 0.02f) {
                            addNotification("Overtime Notice", "⚠️ OVERTIME ACTIVE: You have exceeded your target shift. Standard rate overtime calculation is active.", isAlert = true)
                        }
                    }
                } else {
                    activeTimerString.value = "00:00:00"
                    breakTimerString.value = ""
                }
                delay(1000)
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    fun fetchWeatherForecast(city: String) {
        viewModelScope.launch {
            weatherLoading.value = true
            weatherError.value = null
            try {
                val apiKey = com.example.BuildConfig.OPENWEATHER_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_OPENWEATHER_API_KEY") {
                    loadMockWeather(city)
                } else {
                    val currentRes = WeatherServiceClient.api.getCurrentWeather(city, apiKey)
                    val forecastRes = WeatherServiceClient.api.getForecast(city, apiKey)
                    currentWeather.value = currentRes
                    weatherForecast.value = forecastRes
                    selectedWeatherCity.value = city
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadMockWeather(city)
            } finally {
                weatherLoading.value = false
            }
        }
    }

    // Update shift for a specific employee and date
    fun updateEmployeeShift(employeeName: String, department: String, date: String, newShift: String, updatedBy: String) {
        val currentList = teamSchedules.value.toMutableList()
        val index = currentList.indexOfFirst { it.employeeName == employeeName && it.date == date }
        if (index != -1) {
            currentList[index] = currentList[index].copy(shiftName = newShift)
        } else {
            currentList.add(TeamSchedule(
                id = java.util.UUID.randomUUID().toString(),
                employeeName = employeeName,
                department = department,
                date = date,
                shiftName = newShift
            ))
        }
        teamSchedules.value = currentList

        // Add audit log
        addAuditLog(updatedBy, "Reassigned schedule for $employeeName on $date to $newShift.")
        // Add notification
        addNotification("Schedule Reassigned", "Shift for $employeeName on $date updated to $newShift.", isAlert = false)
    }


    private fun loadMockWeather(city: String) {
        val cleanCityName = city.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val mockTemp = when (cleanCityName) {
            "Indore" -> 29.5
            "Manila" -> 31.0
            "London" -> 19.0
            "New York" -> 22.5
            "Tokyo" -> 25.0
            else -> 26.0
        }
        val mockStatus = when (cleanCityName) {
            "Indore" -> "Scattered Clouds"
            "Manila" -> "Tropical Showers"
            "London" -> "Overcast Drizzle"
            "New York" -> "Mostly Sunny"
            "Tokyo" -> "Clear Sky"
            else -> "Partly Cloudy"
        }
        val mockIcon = when (cleanCityName) {
            "Indore" -> "03d"
            "Manila" -> "10d"
            "London" -> "09d"
            "New York" -> "01d"
            "Tokyo" -> "01d"
            else -> "02d"
        }
        val mockHumidity = when (cleanCityName) {
            "Manila" -> 82
            "Indore" -> 60
            else -> 68
        }
        val mockWind = when (cleanCityName) {
            "Manila" -> 16.5
            "Indore" -> 8.0
            else -> 12.0
        }

        currentWeather.value = com.example.data.WeatherResponse(
            name = cleanCityName,
            main = com.example.data.MainData(
                temp = mockTemp,
                feelsLike = mockTemp + 2.5,
                tempMin = mockTemp - 3.0,
                tempMax = mockTemp + 3.0,
                humidity = mockHumidity,
                pressure = 1011
            ),
            weather = listOf(com.example.data.WeatherDescription(main = mockStatus, description = mockStatus, icon = mockIcon)),
            wind = com.example.data.WindData(speed = mockWind)
        )

        val forecastItems = listOf(
            com.example.data.ForecastItem(
                dt = System.currentTimeMillis() / 1000 + 10800,
                main = com.example.data.MainData(mockTemp - 1.0, mockTemp + 1.5, mockTemp - 2.0, mockTemp + 2.0, mockHumidity, 1011),
                weather = listOf(com.example.data.WeatherDescription(mockStatus, mockStatus, mockIcon)),
                dtTxt = "2026-06-29 18:00:00"
            ),
            com.example.data.ForecastItem(
                dt = System.currentTimeMillis() / 1000 + 21600,
                main = com.example.data.MainData(mockTemp - 2.0, mockTemp + 0.5, mockTemp - 3.0, mockTemp + 1.0, mockHumidity + 5, 1011),
                weather = listOf(com.example.data.WeatherDescription(mockStatus, mockStatus, mockIcon)),
                dtTxt = "2026-06-29 21:00:00"
            ),
            com.example.data.ForecastItem(
                dt = System.currentTimeMillis() / 1000 + 32400,
                main = com.example.data.MainData(mockTemp - 3.0, mockTemp - 1.0, mockTemp - 4.0, mockTemp, mockHumidity + 8, 1012),
                weather = listOf(com.example.data.WeatherDescription("Clear", "Clear", "01n")),
                dtTxt = "2026-06-30 00:00:00"
            ),
            com.example.data.ForecastItem(
                dt = System.currentTimeMillis() / 1000 + 43200,
                main = com.example.data.MainData(mockTemp - 4.0, mockTemp - 2.0, mockTemp - 5.0, mockTemp - 1.0, mockHumidity + 10, 1012),
                weather = listOf(com.example.data.WeatherDescription("Clear", "Clear", "01n")),
                dtTxt = "2026-06-30 03:00:00"
            ),
            com.example.data.ForecastItem(
                dt = System.currentTimeMillis() / 1000 + 54000,
                main = com.example.data.MainData(mockTemp - 1.5, mockTemp + 0.5, mockTemp - 3.0, mockTemp + 1.0, mockHumidity - 2, 1011),
                weather = listOf(com.example.data.WeatherDescription("Sunny", "Sunny", "01d")),
                dtTxt = "2026-06-30 06:00:00"
            ),
            com.example.data.ForecastItem(
                dt = System.currentTimeMillis() / 1000 + 64800,
                main = com.example.data.MainData(mockTemp + 0.5, mockTemp + 3.0, mockTemp - 1.0, mockTemp + 2.0, mockHumidity - 5, 1010),
                weather = listOf(com.example.data.WeatherDescription("Sunny", "Sunny", "01d")),
                dtTxt = "2026-06-30 09:00:00"
            )
        )

        weatherForecast.value = com.example.data.ForecastResponse(
            list = forecastItems,
            city = com.example.data.CityData(name = cleanCityName, country = when(cleanCityName) { "Manila" -> "PH" "Indore" -> "IN" "London" -> "UK" else -> "US" })
        )
        selectedWeatherCity.value = city
    }

    // --- COMPANY SURVEY MODULE STATES & METRIC ENGINES ---
    var surveys = mutableStateOf<List<CompanySurvey>>(listOf(
        CompanySurvey(
            id = "prepopulated-1",
            title = "Q2 2026 Shift-Timing & Flexibility Survey",
            description = "Gathering insights on remote shift tracking and timezone overlap adjustments.",
            isMandatory = false,
            questions = listOf(
                SurveyQuestion("q1", "Rate the flexibility of our current core hour policy (1-5)", QuestionType.RATING_1_TO_5),
                SurveyQuestion("q2", "Describe any scheduling conflicts you face with other teams", QuestionType.TEXT)
            ),
            responsesCount = 18
        ),
        CompanySurvey(
            id = "prepopulated-2",
            title = "Annual Workstation & Tooling Evaluation",
            description = "A standard compliance survey evaluating physical desk ergonomics and IDE licensing.",
            isMandatory = true,
            questions = listOf(
                SurveyQuestion("q3", "Rate your hardware/laptop performance for daily compilation (1-5)", QuestionType.RATING_1_TO_5),
                SurveyQuestion("q4", "What software tools do you require to improve daily output?", QuestionType.TEXT)
            ),
            responsesCount = 24
        )
    ))

    var surveyResponses = mutableStateOf<List<SurveyResponse>>(listOf(
        SurveyResponse("r1", "prepopulated-1", "Aditya Joshi", mapOf("q1" to "4", "q2" to "Occasional meetings outside standard hours.")),
        SurveyResponse("r2", "prepopulated-1", "Liam Vance", mapOf("q1" to "5", "q2" to "None, scheduling is very smooth.")),
        SurveyResponse("r3", "prepopulated-2", "Sophia Martinez", mapOf("q3" to "3", "q4" to "More memory/RAM for local Docker development."))
    ))

    var activeSurveyNotification = mutableStateOf<CompanySurvey?>(null)
    var showSurveyNotificationBanner = mutableStateOf(false)
    var activeCompletingSurvey = mutableStateOf<CompanySurvey?>(null)
    var completedSurveyIds = mutableStateOf<Set<String>>(setOf("prepopulated-2"))
    var activeDraftAnswers = mutableStateMapOf<String, String>()

    fun postSurvey(survey: CompanySurvey) {
        surveys.value = surveys.value + survey
        activeSurveyNotification.value = survey
        showSurveyNotificationBanner.value = true
        
        addNotification(
            title = "New Survey Broadcasted",
            message = "A new survey '${survey.title}' has been successfully published for employee completion.",
            isAlert = survey.isMandatory
        )
    }

    fun submitSurveyResponse(surveyId: String, answers: Map<String, String>) {
        val response = SurveyResponse(
            surveyId = surveyId,
            employeeName = currentUserName.value,
            answers = answers
        )
        surveyResponses.value = surveyResponses.value + response
        
        // Update response count in surveys
        surveys.value = surveys.value.map {
            if (it.id == surveyId) {
                it.copy(responsesCount = it.responsesCount + 1)
            } else {
                it
            }
        }
        
        // Mark as completed
        completedSurveyIds.value = completedSurveyIds.value + surveyId
        
        // Clear active completing survey
        if (activeCompletingSurvey.value?.id == surveyId) {
            activeCompletingSurvey.value = null
        }
        
        // Clear draft answers
        activeDraftAnswers.clear()
        
        addNotification(
            title = "Survey Submitted",
            message = "Thank you! Response recorded for: ${surveys.value.find { it.id == surveyId }?.title ?: ""}",
            isAlert = false
        )
    }

    fun deleteSurvey(surveyId: String) {
        surveys.value = surveys.value.filter { it.id != surveyId }
        surveyResponses.value = surveyResponses.value.filter { it.surveyId != surveyId }
        if (activeSurveyNotification.value?.id == surveyId) {
            activeSurveyNotification.value = null
            showSurveyNotificationBanner.value = false
        }
        if (activeCompletingSurvey.value?.id == surveyId) {
            activeCompletingSurvey.value = null
        }
    }
}


data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isAlert: Boolean
)

data class ProductivityRecord(
    val day: String,
    val score: Int,
    val hours: Double
)

data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val recipient: String,
    val text: String,
    val timestamp: String,
    val isSystem: Boolean = false,
    val isSwapRequest: Boolean = false,
    val swapDate: String = "",
    val swapShiftName: String = "",
    val swapStatus: String = "", // "PENDING", "ACCEPTED", "APPROVED", "REJECTED"
    val swapRequester: String = "",
    val swapCoverer: String = ""
)

data class BirthdayInfo(
    val name: String,
    val role: String,
    val dateStr: String,
    val isToday: Boolean,
    val daysUntil: Int
)
