package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.TimeTrackerViewModel
import com.example.ui.theme.*
import androidx.compose.animation.core.*

// Data Models
data class Top5Employee(
    val id: String,
    val name: String,
    val department: String,
    val team: String,
    val position: String,
    val avatarUrl: String,
    val monthlyMetrics: Map<String, Top5Metrics>
)

data class Top5Metrics(
    val punctualityRate: Float,       // P: Punctuality Rate (0 to 100)
    val scheduleAdherence: Float,     // A: Schedule Adherence (0 to 100)
    val telemetryIntegrity: Float,    // T: Telemetry Integrity (0 to 100) - AI automatically detected
    val milestonesCompletion: Float   // M: Task/Milestone Completion (0 to 100)
) {
    val performanceScore: Float
        get() = (0.30f * punctualityRate) + (0.30f * scheduleAdherence) + (0.20f * telemetryIntegrity) + (0.20f * milestonesCompletion)
}



fun getAggregatedMetrics(employee: Top5Employee, period: String, targetMonth: String): Top5Metrics {
    val monthsToAggregate = when (period) {
        "Monthly" -> listOf(targetMonth)
        "Every 3 Months" -> listOf("May 2026", "June 2026", "July 2026")
        "Quarterly" -> listOf("May 2026", "June 2026")
        "Yearly" -> listOf("May 2026", "June 2026", "July 2026")
        else -> listOf(targetMonth)
    }
    
    val metricsList = monthsToAggregate.mapNotNull { employee.monthlyMetrics[it] }
    if (metricsList.isEmpty()) {
        return Top5Metrics(90f, 90f, 100f, 90f)
    }
    
    val avgPunctuality = metricsList.map { it.punctualityRate }.average().toFloat()
    val avgAdherence = metricsList.map { it.scheduleAdherence }.average().toFloat()
    val avgTelemetry = metricsList.map { it.telemetryIntegrity }.average().toFloat()
    val avgMilestones = metricsList.map { it.milestonesCompletion }.average().toFloat()
    
    return Top5Metrics(avgPunctuality, avgAdherence, avgTelemetry, avgMilestones)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Top5DashboardScreen(viewModel: TimeTrackerViewModel) {
    val context = LocalContext.current
    
    // Resolve dynamic Liquid Glass theme
    val themeName = viewModel.selectedTheme.value
    val themeColors = LiquidThemeRegistry.getThemeByName(themeName)

    // Enforce role-based view partitioning
    val userRole = viewModel.currentUserRole.value
    val currentUserName = viewModel.currentUserName.value
    val isSupervisor = userRole == "MANAGER" || userRole == "SUPERVISOR" || userRole == "ADMIN_HR"
    val currentViewMode = if (isSupervisor) "supervisor" else "leaderboard"
    
    // Loaded state from global ViewModel
    val activeRankingPeriod = viewModel.postedRankingPeriod.value
    val lastPostedRankingNotification = viewModel.lastPostedRankingNotification.value
    
    // Filter states
    var selectedDeptFilter by remember { mutableStateOf("All") }
    var selectedTeamFilter by remember { mutableStateOf("All") }
    var selectedMonthFilter by remember { mutableStateOf("July 2026") }
    
    // Selected Dashboard Pillar (for Supervisor View)
    var activePillar by remember { mutableStateOf("overall") } // overall, performance, attendance, punctuality, telemetry
    
    // Dropdown visibility states
    var deptExpanded by remember { mutableStateOf(false) }
    var teamExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    // Seeded Employee Data with current user injected dynamically if they are an Employee
    val employees = remember(currentUserName, userRole) {
        val baseList = getSeededDashboardEmployees()
        if (userRole == "EMPLOYEE" && !baseList.any { it.name.equals(currentUserName, ignoreCase = true) }) {
            baseList + Top5Employee(
                id = "COS-2026-9999",
                name = currentUserName,
                department = "Engineering",
                team = "Core Platform",
                position = "Software Contender",
                avatarUrl = "",
                monthlyMetrics = mapOf(
                    "May 2026" to Top5Metrics(88f, 85f, 100f, 82f),
                    "June 2026" to Top5Metrics(90f, 92f, 100f, 88f),
                    "July 2026" to Top5Metrics(94f, 93f, 100f, 95f) // Almost Top 5! Excellent standing
                )
            )
        } else {
            baseList
        }
    }
    
    // Filter lists
    val departments = listOf("All", "Engineering", "Human Resources", "Product Management", "Management", "Administration")
    val teams = listOf("All", "Core Platform", "HR Ops", "Leadership", "Tech Ops", "Support")
    val months = listOf("July 2026", "June 2026", "May 2026")

    // Filter and compute ranked employees
    val filteredAndRanked = remember(selectedDeptFilter, selectedTeamFilter, selectedMonthFilter, activeRankingPeriod, activePillar, currentViewMode, employees) {
        employees.filter { emp ->
            (selectedDeptFilter == "All" || emp.department.equals(selectedDeptFilter, ignoreCase = true)) &&
            (selectedTeamFilter == "All" || emp.team.equals(selectedTeamFilter, ignoreCase = true))
        }.map { emp ->
            val metrics = getAggregatedMetrics(emp, activeRankingPeriod, selectedMonthFilter)
            emp to metrics
        }.sortedWith { a, b ->
            if (currentViewMode == "leaderboard") {
                // Leaderboard always ranks by the overall weighted Performance Score
                b.second.performanceScore.compareTo(a.second.performanceScore)
            } else {
                when (activePillar) {
                    "overall" -> b.second.performanceScore.compareTo(a.second.performanceScore)
                    "performance" -> b.second.milestonesCompletion.compareTo(a.second.milestonesCompletion)
                    "attendance" -> b.second.scheduleAdherence.compareTo(a.second.scheduleAdherence)
                    "punctuality" -> b.second.punctualityRate.compareTo(a.second.punctualityRate)
                    "telemetry" -> b.second.telemetryIntegrity.compareTo(a.second.telemetryIntegrity)
                    else -> b.second.performanceScore.compareTo(a.second.performanceScore)
                }
            }
        }.take(5) // Top 5
    }

    // Dynamic Overview metrics based on current filters and active period
    val currentSummaryStats = remember(selectedDeptFilter, selectedTeamFilter, selectedMonthFilter, activeRankingPeriod, employees) {
        val activeMetrics = employees.filter { emp ->
            (selectedDeptFilter == "All" || emp.department.equals(selectedDeptFilter, ignoreCase = true)) &&
            (selectedTeamFilter == "All" || emp.team.equals(selectedTeamFilter, ignoreCase = true))
        }.map { getAggregatedMetrics(it, activeRankingPeriod, selectedMonthFilter) }

        if (activeMetrics.isEmpty()) {
            SummaryStats(0f, 0f, 0f, 0f, 0f)
        } else {
            SummaryStats(
                avgPunctuality = activeMetrics.map { it.punctualityRate }.average().toFloat(),
                avgAdherence = activeMetrics.map { it.scheduleAdherence }.average().toFloat(),
                avgTelemetry = activeMetrics.map { it.telemetryIntegrity }.average().toFloat(),
                avgMilestones = activeMetrics.map { it.milestonesCompletion }.average().toFloat(),
                avgScore = activeMetrics.map { it.performanceScore }.average().toFloat()
            )
        }
    }

    // Calculate logged-in user's rank status
    val allRankedList = remember(selectedMonthFilter, activeRankingPeriod, employees) {
        employees.map { emp ->
            val m = getAggregatedMetrics(emp, activeRankingPeriod, selectedMonthFilter)
            emp to m
        }.sortedByDescending { it.second.performanceScore }
    }
    val userRankIndex = allRankedList.indexOfFirst { it.first.name.equals(currentUserName, ignoreCase = true) }
    val userRank = if (userRankIndex != -1) userRankIndex + 1 else 6

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color.Transparent) // Let underlying LiquidGlassBackground shine through!
            .padding(16.dp)
    ) {
        // Ranking Posted Notification Banner (shows for both HR and employees if active)
        if (lastPostedRankingNotification != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("ranking_posted_notif_banner"),
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.primaryAccent.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, themeColors.primaryAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(themeColors.primaryAccent.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Notification",
                            tint = themeColors.primaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OFFICIAL RANKING PUBLISHED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = themeColors.primaryAccent,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = lastPostedRankingNotification,
                            fontSize = 12.sp,
                            color = themeColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.dismissRankingNotification() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = themeColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (isSupervisor) {
            // ================== SUPERVISOR VIEW MODE ==================
            
            // FILTER BAR - Clean layout using drop-downs and clear tags
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
                border = BorderStroke(1.dp, themeColors.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filters",
                            tint = themeColors.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "FILTER AND SCOPE ENGINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.primaryAccent,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Department Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            ExposedDropdownMenuBox(
                                expanded = deptExpanded,
                                onExpandedChange = { deptExpanded = !deptExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedDeptFilter,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Department", fontSize = 10.sp, color = themeColors.textSecondary) },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = themeColors.textPrimary, fontSize = 12.sp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = themeColors.primaryAccent,
                                        unfocusedBorderColor = themeColors.cardBorder,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("top5_dept_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = deptExpanded,
                                    onDismissRequest = { deptExpanded = false },
                                    modifier = Modifier.background(themeColors.bgGradientEnd)
                                ) {
                                    departments.forEach { dept ->
                                        DropdownMenuItem(
                                            text = { Text(dept, color = themeColors.textPrimary, fontSize = 12.sp) },
                                            onClick = {
                                                selectedDeptFilter = dept
                                                deptExpanded = false
                                            },
                                            modifier = Modifier.testTag("top5_dept_item_$dept")
                                        )
                                    }
                                }
                            }
                        }

                        // Team Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            ExposedDropdownMenuBox(
                                expanded = teamExpanded,
                                onExpandedChange = { teamExpanded = !teamExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedTeamFilter,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Team / Unit", fontSize = 10.sp, color = themeColors.textSecondary) },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = themeColors.textPrimary, fontSize = 12.sp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = themeColors.primaryAccent,
                                        unfocusedBorderColor = themeColors.cardBorder,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("top5_team_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = teamExpanded,
                                    onDismissRequest = { teamExpanded = false },
                                    modifier = Modifier.background(themeColors.bgGradientEnd)
                                ) {
                                    teams.forEach { team ->
                                        DropdownMenuItem(
                                            text = { Text(team, color = themeColors.textPrimary, fontSize = 12.sp) },
                                            onClick = {
                                                selectedTeamFilter = team
                                                teamExpanded = false
                                            },
                                            modifier = Modifier.testTag("top5_team_item_$team")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Month Filter Selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = monthExpanded,
                            onExpandedChange = { monthExpanded = !monthExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedMonthFilter,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Target Calendar Month", fontSize = 10.sp, color = themeColors.textSecondary) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = themeColors.textPrimary, fontSize = 12.sp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = themeColors.primaryAccent,
                                    unfocusedBorderColor = themeColors.cardBorder,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("top5_month_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = monthExpanded,
                                onDismissRequest = { monthExpanded = false },
                                modifier = Modifier.background(themeColors.bgGradientEnd)
                            ) {
                                months.forEach { month ->
                                    DropdownMenuItem(
                                        text = { Text(month, color = themeColors.textPrimary, fontSize = 12.sp) },
                                        onClick = {
                                            selectedMonthFilter = month
                                            monthExpanded = false
                                        },
                                        modifier = Modifier.testTag("top5_month_item_$month")
                                    )
                                }
                            }
                        }
                    }

                    // Ranking Interval Control for HR / Supervisor
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(themeColors.cardBorder.copy(alpha = 0.5f)))
                    
                    var pendingPeriodState by remember { mutableStateOf(viewModel.postedRankingPeriod.value) }
                    var periodDropdownExpanded by remember { mutableStateOf(false) }
                    val periods = listOf("Monthly", "Every 3 Months", "Quarterly", "Yearly")
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "HR RANKING DISPLAY PUBLICATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.textSecondary,
                            letterSpacing = 1.sp
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1.3f)) {
                                ExposedDropdownMenuBox(
                                    expanded = periodDropdownExpanded,
                                    onExpandedChange = { periodDropdownExpanded = !periodDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = pendingPeriodState,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Display Period", fontSize = 9.sp, color = themeColors.textSecondary) },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = themeColors.textPrimary, fontSize = 11.sp),
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodDropdownExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = themeColors.primaryAccent,
                                            unfocusedBorderColor = themeColors.cardBorder,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                            .testTag("top5_period_dropdown")
                                    )
                                    ExposedDropdownMenu(
                                        expanded = periodDropdownExpanded,
                                        onDismissRequest = { periodDropdownExpanded = false },
                                        modifier = Modifier.background(themeColors.bgGradientEnd)
                                    ) {
                                        periods.forEach { p ->
                                            DropdownMenuItem(
                                                text = { Text(p, color = themeColors.textPrimary, fontSize = 11.sp) },
                                                onClick = {
                                                    pendingPeriodState = p
                                                    periodDropdownExpanded = false
                                                },
                                                modifier = Modifier.testTag("top5_period_item_$p")
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.postRankingPeriod(pendingPeriodState)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("post_ranking_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Publish,
                                    contentDescription = "Publish",
                                    modifier = Modifier.size(16.dp),
                                    tint = themeColors.bgGradientStart
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Post Rank",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.bgGradientStart,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // CORE BEHAVIORAL PILLARS SELECTION GRID / SCROLLABLE ROW
            Text(
                text = "SELECT CORE COMPLIANCE PILLAR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.textSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ScrollableTabRow(
                selectedTabIndex = getPillarIndex(activePillar),
                containerColor = themeColors.cardSurface,
                contentColor = themeColors.primaryAccent,
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, themeColors.cardBorder, RoundedCornerShape(8.dp))
                    .padding(bottom = 12.dp)
            ) {
                val pillarTabs = listOf(
                    "overall" to "Overall Score",
                    "performance" to "Performance (M)",
                    "attendance" to "Attendance (A)",
                    "punctuality" to "Punctuality (P)",
                    "telemetry" to "AI Telemetry (T)"
                )
                pillarTabs.forEachIndexed { idx, (key, label) ->
                    Tab(
                        selected = activePillar == key,
                        onClick = { activePillar = key },
                        text = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = getPillarIcon(key),
                                    contentDescription = label,
                                    tint = if (activePillar == key) themeColors.primaryAccent else themeColors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        selectedContentColor = themeColors.primaryAccent,
                        unselectedContentColor = themeColors.textSecondary,
                        modifier = Modifier.testTag("top5_tab_$key")
                    )
                }
            }

            // ANALYTICS CARD - Summary & Dynamic insights based on chosen pillar
            AnalyticsOverviewCard(activePillar, currentSummaryStats, selectedDeptFilter, selectedTeamFilter, selectedMonthFilter, themeColors)

            Spacer(modifier = Modifier.height(14.dp))

            // THE RANKED LIST HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOP 5 EMPLOYEES: ${activePillar.uppercase()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = themeColors.primaryAccent,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Calculation Period: $activeRankingPeriod",
                        fontSize = 10.sp,
                        color = themeColors.secondaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${filteredAndRanked.size} Records",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary
                )
            }

            // TOP 5 EMPLOYEES RENDERER
            if (filteredAndRanked.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(themeColors.cardSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, themeColors.cardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Inbox, contentDescription = "Empty", tint = themeColors.textSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching records found for this selection.", color = themeColors.textSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredAndRanked.forEachIndexed { rank, (employee, metrics) ->
                        EmployeeRankCard(
                            rank = rank + 1,
                            employee = employee,
                            metrics = metrics,
                            pillar = activePillar,
                            isAnonymized = false,
                            themeColors = themeColors
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // DYNAMIC HR STRATEGIC ACTION PANEL
            ActionStrategicPanel(activePillar, selectedDeptFilter, selectedMonthFilter, currentSummaryStats, themeColors)
            
        } else {
            // ================== EMPLOYEE VIEW MODE ==================
            
            // Dynamic Employee KPI Metric Dashboard Section
            val currentEmpStats = remember(employees, currentUserName) {
                employees.find { it.name.equals(currentUserName, ignoreCase = true) }
                    ?: Top5Employee(
                        id = "EMP-NEW",
                        name = currentUserName,
                        department = "Engineering",
                        team = "Core Platform",
                        position = "Software Engineer",
                        avatarUrl = "",
                        monthlyMetrics = mapOf("July 2026" to Top5Metrics(92f, 90f, 100f, 85f))
                    )
            }

            // Clean Employee KPI Dashboard Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
                border = BorderStroke(1.dp, themeColors.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(themeColors.primaryAccent.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = themeColors.primaryAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                text = currentUserName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.textPrimary
                            )
                            Text(
                                text = "${currentEmpStats.position} • ${currentEmpStats.department}",
                                fontSize = 12.sp,
                                color = themeColors.textSecondary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(themeColors.cardBorder.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "KEY PERFORMANCE INDICATORS (${if (activeRankingPeriod == "Monthly") selectedMonthFilter.uppercase() else activeRankingPeriod.uppercase()})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.primaryAccent,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    val userMetrics = getAggregatedMetrics(currentEmpStats, activeRankingPeriod, selectedMonthFilter)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Overall Score Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(themeColors.primaryAccent.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .border(1.dp, themeColors.primaryAccent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("OVERALL", fontSize = 8.sp, color = themeColors.textSecondary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%.1f", userMetrics.performanceScore),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = themeColors.primaryAccent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Rank #$userRank Overall", fontSize = 8.sp, color = themeColors.textSecondary)
                        }
                        
                        // Details Column
                        Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Punctuality
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Punctuality", fontSize = 11.sp, color = themeColors.textSecondary)
                                Text("${userMetrics.punctualityRate.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                            }
                            // Adherence
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Schedule Adherence", fontSize = 11.sp, color = themeColors.textSecondary)
                                Text("${userMetrics.scheduleAdherence.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                            }
                            // Compliance Integrity
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Compliance Integrity", fontSize = 11.sp, color = themeColors.textSecondary)
                                Text("${userMetrics.telemetryIntegrity.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                            }
                            // Milestones
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Milestone Progress", fontSize = 11.sp, color = themeColors.textSecondary)
                                Text("${userMetrics.milestonesCompletion.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.textPrimary)
                            }
                        }
                    }
                }
            }

            // Liquid podium splash graphic for the Top 3
            LiquidPodiumSplash(top3 = filteredAndRanked, themeColors = themeColors)

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = "MONTHLY HIGHLIGHTS: ${selectedMonthFilter.uppercase()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = themeColors.primaryAccent,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // Ranked List
            if (filteredAndRanked.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(themeColors.cardSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, themeColors.cardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records available.", color = themeColors.textSecondary, fontSize = 12.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredAndRanked.forEachIndexed { rank, (employee, metrics) ->
                        EmployeeRankCard(
                            rank = rank + 1,
                            employee = employee,
                            metrics = metrics,
                            pillar = "overall",
                            isAnonymized = true,
                            themeColors = themeColors
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- SUB-COMPOSABLES ----------------------

@Composable
fun AnalyticsOverviewCard(
    pillar: String,
    stats: SummaryStats,
    dept: String,
    team: String,
    month: String,
    themeColors: LiquidThemeColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "WORKFORCE INTELLIGENCE SUMMARY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.textSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Circular Metric Representation
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(themeColors.cardBorder, CircleShape)
                        .border(2.dp, themeColors.primaryAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val mainVal = when (pillar) {
                            "overall" -> String.format("%.1f", stats.avgScore)
                            "performance" -> "${stats.avgMilestones.toInt()}%"
                            "attendance" -> "${stats.avgAdherence.toInt()}%"
                            "punctuality" -> "${stats.avgPunctuality.toInt()}%"
                            "telemetry" -> "${stats.avgTelemetry.toInt()}%"
                            else -> "0"
                        }
                        Text(
                            text = mainVal,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = themeColors.primaryAccent
                        )
                        Text(
                            text = when (pillar) {
                                "overall" -> "Score Avg"
                                "performance" -> "Milestones"
                                "attendance" -> "Adherence"
                                "punctuality" -> "Punctual"
                                "telemetry" -> "AI Telem."
                                else -> "Average"
                            },
                            fontSize = 8.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }

                // Summary Text Explanation
                Column(modifier = Modifier.weight(1f)) {
                    val explanation = getPillarOverviewDescription(pillar, stats, dept, team, month)
                    Text(
                        text = explanation,
                        fontSize = 12.sp,
                        color = themeColors.textPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmployeeRankCard(
    rank: Int,
    employee: Top5Employee,
    metrics: Top5Metrics,
    pillar: String,
    isAnonymized: Boolean,
    themeColors: LiquidThemeColors
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top5_rank_card_$rank"),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, if (rank == 1) themeColors.primaryAccent.copy(alpha = 0.5f) else themeColors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank Badge Number or Medal
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFFD700).copy(alpha = 0.15f) // Gold Highlight
                            2 -> Color(0xFFC0C0C0).copy(alpha = 0.15f) // Silver Highlight
                            3 -> Color(0xFFCD7F32).copy(alpha = 0.15f) // Bronze Highlight
                            else -> themeColors.cardBorder
                        },
                        CircleShape
                    )
                    .border(
                        1.dp,
                        when (rank) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> themeColors.cardBorder
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (rank) {
                        1 -> "🥇"
                        2 -> "🥈"
                        3 -> "🥉"
                        else -> "$rank"
                    },
                    fontSize = if (rank <= 3) 14.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (rank <= 3) Color.White else themeColors.textSecondary
                )
            }

            // Employee Avatar & Meta Data
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(themeColors.cardBorder, CircleShape)
                    .border(1.dp, themeColors.cardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = themeColors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Name & Department details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isAnonymized) "Active Contributor • Rank #$rank" else "${employee.position} • ${employee.team}",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Pillar-Specific Metric Rendering with visual bars
            Column(
                modifier = Modifier.width(110.dp),
                horizontalAlignment = Alignment.End
            ) {
                val valueStr: String
                val progressVal: Float
                val barColor: Color
                val bottomLabel: String

                if (isAnonymized) {
                    // Leaderboard displays only total score to enforce Privacy Guard
                    valueStr = String.format("%.1f pts", metrics.performanceScore)
                    progressVal = metrics.performanceScore / 100f
                    barColor = themeColors.primaryAccent
                    bottomLabel = "Aggregated Score"
                } else {
                    when (pillar) {
                        "overall" -> {
                            valueStr = String.format("%.1f pts", metrics.performanceScore)
                            progressVal = metrics.performanceScore / 100f
                            barColor = themeColors.primaryAccent
                            bottomLabel = "P:${metrics.punctualityRate.toInt()}% A:${metrics.scheduleAdherence.toInt()}% M:${metrics.milestonesCompletion.toInt()}%"
                        }
                        "performance" -> {
                            valueStr = "${metrics.milestonesCompletion.toInt()}%"
                            progressVal = metrics.milestonesCompletion / 100f
                            barColor = themeColors.primaryAccent // Unified dynamic style
                            bottomLabel = "Milestone Progress"
                        }
                        "attendance" -> {
                            valueStr = "${metrics.scheduleAdherence.toInt()}%"
                            progressVal = metrics.scheduleAdherence / 100f
                            barColor = themeColors.secondaryAccent
                            bottomLabel = "Schedule Adherence"
                        }
                        "punctuality" -> {
                            valueStr = "${metrics.punctualityRate.toInt()}%"
                            progressVal = metrics.punctualityRate / 100f
                            barColor = themeColors.primaryAccent
                            bottomLabel = "Punctuality Rate"
                        }
                        "telemetry" -> {
                            valueStr = "${metrics.telemetryIntegrity.toInt()}%"
                            progressVal = metrics.telemetryIntegrity / 100f
                            barColor = themeColors.secondaryAccent
                            bottomLabel = "AI GPS Integrity"
                        }
                        else -> {
                            valueStr = "N/A"
                            progressVal = 0f
                            barColor = themeColors.textSecondary
                            bottomLabel = ""
                        }
                    }
                }

                Text(
                    text = valueStr,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = barColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Beautiful small horizontal progress bar
                LinearProgressIndicator(
                    progress = { progressVal.coerceIn(0f, 1f) },
                    color = barColor,
                    trackColor = themeColors.cardBorder,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = bottomLabel,
                    fontSize = 8.sp,
                    color = themeColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ActionStrategicPanel(
    pillar: String,
    dept: String,
    month: String,
    stats: SummaryStats,
    themeColors: LiquidThemeColors
) {
    var showCampaignSuccess by remember { mutableStateOf(false) }
    var isDownloadingAudit by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isDownloadingAudit) {
        if (isDownloadingAudit) {
            downloadProgress = 0f
            while (downloadProgress < 1.0f) {
                kotlinx.coroutines.delay(100)
                downloadProgress += 0.05f
            }
            isDownloadingAudit = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = themeColors.primaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "AI STRATEGIC & COMPLIANCE PLAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.primaryAccent,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            val recommendation = when (pillar) {
                "overall" -> {
                    "Overall scores remain incredibly positive (Avg: ${String.format("%.1f", stats.avgScore)} pts) for $month. We recommend maintaining current shift allocations and sending celebratory automated Slack notifications."
                }
                "performance" -> {
                    "Performance milestones averages ${stats.avgMilestones.toInt()}%. Work progress is strong. Consider highlighting Top 5 performers in the team bulletin board to promote operational excellence."
                }
                "attendance" -> {
                    "Attendance adherence sits at ${stats.avgAdherence.toInt()}%. High coordination observed. Standard protocols are fully compliant; no manual supervisor adjustments are required."
                }
                "punctuality" -> {
                    "Punctuality rate is ${stats.avgPunctuality.toInt()}%. We suggest maintaining positive reinforcement for employees showing 100% on-time records."
                }
                "telemetry" -> {
                    "Our AI geofencing scanner automatically audited telemetry logs for all departments. Zero simulation or GPS spoofing attempts detected. Check-in compliance is in perfect state."
                }
                else -> ""
            }

            Text(
                text = recommendation,
                fontSize = 12.sp,
                color = themeColors.textSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (showCampaignSuccess) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.primaryAccent.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, themeColors.primaryAccent.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = themeColors.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "COMPLIANCE CAMPAIGN ENGAGED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.primaryAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🚀 Campaign broadcast active! Shift incentives (+12% Indore premium), automated Kudos on Slack, and high-punctuality push notices have been dispatched.",
                            fontSize = 10.sp,
                            color = themeColors.textSecondary,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { showCampaignSuccess = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Dismiss", fontSize = 10.sp, color = themeColors.primaryAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isDownloadingAudit) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColors.cardBorder.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "📑 EXPORTING SECURE AUDIT LEDGER...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.primaryAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            color = themeColors.primaryAccent,
                            trackColor = themeColors.cardBorder,
                            modifier = Modifier.fillMaxWidth().height(4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val currentHash = remember(downloadProgress) {
                            java.util.UUID.nameUUIDFromBytes(downloadProgress.toString().toByteArray()).toString().take(16)
                        }
                        Text(
                            text = "Geofence Ledger Hash: SHA256-${currentHash.uppercase()} • ${(downloadProgress * 100).toInt()}%",
                            fontSize = 8.sp,
                            color = themeColors.textSecondary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showCampaignSuccess = true },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryAccent),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text("Trigger Campaign", color = TextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { isDownloadingAudit = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColors.primaryAccent),
                    border = BorderStroke(1.dp, themeColors.primaryAccent),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text("Download PDF Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LiquidPodiumSplash(
    top3: List<Pair<Top5Employee, Top5Metrics>>,
    themeColors: LiquidThemeColors
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_splash")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.primaryAccent.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val primaryColor = themeColors.primaryAccent
            val secondaryColor = themeColors.secondaryAccent
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val path = androidx.compose.ui.graphics.Path()
                
                path.moveTo(0f, height)
                for (x in 0..width.toInt() step 5) {
                    val relativeX = x.toFloat() / width
                    val y = height - 60f + 
                            (kotlin.math.sin(relativeX * 2 * Math.PI + waveOffset) * 12f).toFloat() +
                            (kotlin.math.cos(relativeX * 4 * Math.PI - waveOffset * 1.5f) * 6f).toFloat()
                    path.lineTo(x.toFloat(), y)
                }
                path.lineTo(width, height)
                path.close()
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.15f),
                            secondaryColor.copy(alpha = 0.3f)
                        )
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Rank 2 (Silver)
                if (top3.size > 1) {
                    val emp2 = top3[1]
                    PodiumColumn(
                        employee = emp2.first,
                        score = emp2.second.performanceScore,
                        rank = 2,
                        podiumHeight = 65.dp,
                        color = Color(0xFFC0C0C0),
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Rank 1 (Gold)
                if (top3.isNotEmpty()) {
                    val emp1 = top3[0]
                    PodiumColumn(
                        employee = emp1.first,
                        score = emp1.second.performanceScore,
                        rank = 1,
                        podiumHeight = 90.dp,
                        color = Color(0xFFFFD700),
                        themeColors = themeColors,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                // Rank 3 (Bronze)
                if (top3.size > 2) {
                    val emp3 = top3[2]
                    PodiumColumn(
                        employee = emp3.first,
                        score = emp3.second.performanceScore,
                        rank = 3,
                        podiumHeight = 45.dp,
                        color = Color(0xFFCD7F32),
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PodiumColumn(
    employee: Top5Employee,
    score: Float,
    rank: Int,
    podiumHeight: androidx.compose.ui.unit.Dp,
    color: Color,
    themeColors: LiquidThemeColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Initials Avatar with solid metallic badge styling
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color, CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val initials = employee.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
            Text(
                text = initials,
                color = Color(0xFF090D16), // Perfect dark contrast on gold/silver/bronze
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Compact Name
        Text(
            text = employee.name.substringBefore(" "),
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = themeColors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Aggregated Score
        Text(
            text = String.format("%.1f pts", score),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.primaryAccent,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Podium Block with solid, high-contrast theme-synced background
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight),
            colors = CardDefaults.cardColors(containerColor = themeColors.bgGradientEnd),
            border = BorderStroke(2.dp, color.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when (rank) {
                            1 -> "🏆"
                            2 -> "🥈"
                            3 -> "🥉"
                            else -> ""
                        },
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "RANK $rank",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = color,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// Helper Class to compute summary metrics
data class SummaryStats(
    val avgPunctuality: Float,
    val avgAdherence: Float,
    val avgTelemetry: Float,
    val avgMilestones: Float,
    val avgScore: Float
)

// Helper mapping keys to Icons
fun getPillarIcon(key: String): ImageVector {
    return when (key) {
        "overall" -> Icons.Default.Leaderboard
        "performance" -> Icons.Default.CheckCircle
        "attendance" -> Icons.Default.Schedule
        "punctuality" -> Icons.Default.Timer
        "telemetry" -> Icons.Default.AutoAwesome
        else -> Icons.Default.HelpOutline
    }
}

fun getPillarIndex(key: String): Int {
    return when (key) {
        "overall" -> 0
        "performance" -> 1
        "attendance" -> 2
        "punctuality" -> 3
        "telemetry" -> 4
        else -> 0
    }
}

fun getPillarOverviewDescription(
    pillar: String,
    stats: SummaryStats,
    dept: String,
    team: String,
    month: String
): String {
    val prefix = if (dept == "All") "Overall, across all company teams," else "Specifically within $dept's $team Unit,"
    return when (pillar) {
        "overall" -> "$prefix the average integrated Performance Score for $month sits at ${String.format("%.1f", stats.avgScore)} pts. This aggregates punctuality, schedule adherence, AI-verified telemetry integrity, and daily milestone completion."
        "performance" -> "$prefix the average Task Milestone Completion rate is ${stats.avgMilestones.toInt()}%. Work progress is extremely robust, led by key performers exceeding daily targets."
        "attendance" -> "$prefix the average Schedule Adherence is ${stats.avgAdherence.toInt()}%. Employees are highly aligned with scheduled hours and break allocations."
        "punctuality" -> "$prefix punctuality rate is ${stats.avgPunctuality.toInt()}%. The vast majority of check-ins occurred exactly on or before the scheduled shift start time."
        "telemetry" -> "$prefix AI Telemetry Integrity is verified at ${stats.avgTelemetry.toInt()}%. The geofencing scanner automatically audited all check-ins, finding zero GPS spoofing or location anomalies."
        else -> ""
    }
}

// Data generator for 10 mock employee profiles across May, June, July 2026
fun getSeededDashboardEmployees(): List<Top5Employee> {
    return listOf(
        Top5Employee(
            id = "COS-2026-0012",
            name = "Sarah Jenkins",
            department = "Engineering",
            team = "Core Platform",
            position = "Senior Kotlin Developer",
            avatarUrl = "",
            monthlyMetrics = mapOf(
                "May 2026" to Top5Metrics(95f, 94f, 100f, 95f),
                "June 2026" to Top5Metrics(96f, 95f, 100f, 97f),
                "July 2026" to Top5Metrics(98f, 97f, 100f, 99f)
            )
        ),
        Top5Employee(
            id = "COS-2026-0013",
            name = "Marcus Aurelius (HR Intern)",
            department = "Human Resources",
            team = "HR Ops",
            position = "HR Audit Associate",
            avatarUrl = "",
            monthlyMetrics = mapOf(
                "May 2026" to Top5Metrics(80f, 82f, 85f, 78f),
                "June 2026" to Top5Metrics(82f, 84f, 90f, 80f),
                "July 2026" to Top5Metrics(85f, 88f, 90f, 82f)
            )
        ),
        Top5Employee(
            id = "COS-2026-0014",
            name = "Robert Chen",
            department = "Product Management",
            team = "Core Platform",
            position = "Lead Product Manager",
            avatarUrl = "",
            monthlyMetrics = mapOf(
                "May 2026" to Top5Metrics(90f, 89f, 95f, 90f),
                "June 2026" to Top5Metrics(92f, 91f, 100f, 92f),
                "July 2026" to Top5Metrics(94f, 93f, 100f, 94f)
            )
        ),
        Top5Employee(
            id = "COS-2026-0015",
            name = "Anjali Sharma",
            department = "Management",
            team = "Leadership",
            position = "General Manager",
            avatarUrl = "",
            monthlyMetrics = mapOf(
                "May 2026" to Top5Metrics(95f, 96f, 100f, 96f),
                "June 2026" to Top5Metrics(96f, 97f, 100f, 97f),
                "July 2026" to Top5Metrics(97f, 98f, 100f, 98f)
            )
        ),
        Top5Employee(
            id = "COS-2026-0016",
            name = "Aditya Joshi (Director)",
            department = "Administration",
            team = "Leadership",
            position = "Director of Operations",
            avatarUrl = "",
            monthlyMetrics = mapOf(
                "May 2026" to Top5Metrics(94f, 94f, 100f, 94f),
                "June 2026" to Top5Metrics(95f, 95f, 100f, 96f),
                "July 2026" to Top5Metrics(96f, 96f, 100f, 97f)
            )
        ),
        Top5Employee(
            id = "COS-2026-0017",
            name = "Priya Patel",
            department = "Engineering",
            team = "Core Platform",
            position = "Senior Backend Engineer",
            avatarUrl = "",
            monthlyMetrics = mapOf(
                "May 2026" to Top5Metrics(92f, 93f, 100f, 92f),
                "June 2026" to Top5Metrics(94f, 95f, 100f, 94f),
                "July 2026" to Top5Metrics(95f, 96f, 100f, 96f)
            )
        ),
        Top5Employee(
            id = "COS-2026-0018",
            name = "David Kim",
            department = "Product Management",
            team = "Core Platform",
            position = "UX Product Designer",
            avatarUrl = "",
            monthlyMetrics = mapOf(
                "May 2026" to Top5Metrics(87f, 88f, 90f, 85f),
                "June 2026" to Top5Metrics(89f, 90f, 95f, 88f),
                "July 2026" to Top5Metrics(91f, 92f, 95f, 90f)
            )
        ),
        Top5Employee(
            id = "COS-2026-0019",
            name = "Elena Rostova",
            department = "Human Resources",
            team = "HR Ops",
            position = "Recruiting Specialist",
            avatarUrl = "",
            monthlyMetrics = mapOf(
                "May 2026" to Top5Metrics(89f, 90f, 100f, 88f),
                "June 2026" to Top5Metrics(91f, 92f, 100f, 90f),
                "July 2026" to Top5Metrics(90f, 92f, 100f, 92f)
            )
        ),
        Top5Employee(
            id = "COS-2026-0020",
            name = "Kofi Mensah",
            department = "Engineering",
            team = "Tech Ops",
            position = "DevOps Engineer",
            avatarUrl = "",
            monthlyMetrics = mapOf(
                "May 2026" to Top5Metrics(90f, 89f, 100f, 90f),
                "June 2026" to Top5Metrics(92f, 91f, 100f, 92f),
                "July 2026" to Top5Metrics(94f, 93f, 100f, 94f)
            )
        ),
        Top5Employee(
            id = "COS-2026-0021",
            name = "Sofia Bianchi",
            department = "Administration",
            team = "Support",
            position = "Customer Support Specialist",
            avatarUrl = "",
            monthlyMetrics = mapOf(
                "May 2026" to Top5Metrics(88f, 87f, 95f, 84f),
                "June 2026" to Top5Metrics(90f, 89f, 100f, 86f),
                "July 2026" to Top5Metrics(92f, 91f, 100f, 88f)
            )
        )
    )
}
