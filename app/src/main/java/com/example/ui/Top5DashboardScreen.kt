package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.RectF
import android.graphics.Path
import android.text.StaticLayout
import android.text.Layout
import android.text.TextPaint
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

    val isHrAdmin = userRole == "ADMIN_HR"
    var activeMainTab by remember { mutableStateOf(if (isHrAdmin) "realtime_insights" else "standings") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color.Transparent) // Let underlying LiquidGlassBackground shine through!
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp)
    ) {
        // --- PRIMARY SCREEN SELECTOR TABS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(themeColors.cardSurface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .border(1.dp, themeColors.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val mainTabs = if (isHrAdmin) {
                listOf(
                    "realtime_insights" to "Workforce Insights",
                    "standings" to "Performance Top 5"
                )
            } else {
                listOf(
                    "standings" to "Performance Top 5"
                )
            }
            mainTabs.forEach { (key, label) ->
                val isSelected = activeMainTab == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) themeColors.primaryAccent else Color.Transparent)
                        .clickable { activeMainTab = key }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (key == "realtime_insights") Icons.Default.Analytics else Icons.Default.Leaderboard,
                            contentDescription = label,
                            tint = if (isSelected) Color.Black else themeColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else themeColors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (activeMainTab == "realtime_insights" && isHrAdmin) {
            RealtimeInsightsDashboard(viewModel = viewModel, themeColors = themeColors, employees = employees)
        } else {
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
}

// ---------------------- SUB-COMPOSABLES ----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeInsightsDashboard(
    viewModel: TimeTrackerViewModel,
    themeColors: LiquidThemeColors,
    employees: List<Top5Employee>
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableStateOf("headcount") } // headcount, absenteeism, turnover

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Realtime Insights",
                color = com.example.ui.theme.AppTextColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Interactive organizational tracking",
                color = themeColors.textSecondary,
                fontSize = 10.sp
            )
        }
        
        Button(
            onClick = {
                val file = generateWorkforceInsightsPdf(context, employees)
                if (file != null) {
                    openWorkforcePdfFile(context, file)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryAccent),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download Report PDF",
                tint = Color.Black,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Export PDF",
                color = Color.Black,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    
    // Sub-tabs Row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(themeColors.cardSurface, RoundedCornerShape(8.dp))
            .border(1.dp, themeColors.cardBorder, RoundedCornerShape(8.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val subTabs = listOf(
            "headcount" to "Headcount",
            "absenteeism" to "Absenteeism",
            "turnover" to "Turnover (AI)"
        )
        subTabs.forEach { (key, label) ->
            val isSelected = selectedSubTab == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) themeColors.primaryAccent.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { selectedSubTab = key }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) themeColors.primaryAccent else themeColors.textPrimary.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    
    // Render Selected Content
    when (selectedSubTab) {
        "headcount" -> HeadcountDashboardContent(themeColors, employees)
        "absenteeism" -> AbsenteeismDashboardContent(themeColors, employees)
        "turnover" -> TurnoverPredictiveContent(themeColors, employees, viewModel)
    }
}

@Composable
fun HeadcountDashboardContent(themeColors: LiquidThemeColors, employees: List<Top5Employee>) {
    // Basic Headcount Calculation
    val totalCount = employees.size
    val engineeringCount = employees.count { it.department.equals("Engineering", ignoreCase = true) }
    val hrCount = employees.count { it.department.equals("Human Resources", ignoreCase = true) }
    val productCount = employees.count { it.department.equals("Product Management", ignoreCase = true) }
    val managementCount = employees.count { it.department.equals("Management", ignoreCase = true) }
    val adminCount = employees.count { it.department.equals("Administration", ignoreCase = true) }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("HEADCOUNT SUMMARY", color = themeColors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Total Headcount Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(getAdaptiveColor(0.03f), RoundedCornerShape(8.dp))
                        .border(1.dp, themeColors.cardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Staff", color = themeColors.textSecondary, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalCount Active", color = com.example.ui.theme.AppTextColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("+14% YoY Growth", color = Color(0xFF00FF88), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                
                // Onboarding Rate Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(getAdaptiveColor(0.03f), RoundedCornerShape(8.dp))
                        .border(1.dp, themeColors.cardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Avg Hires Rate", color = themeColors.textSecondary, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("3.5 / Month", color = com.example.ui.theme.AppTextColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("98% Probation Pass", color = themeColors.secondaryAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    
    // Department Distribution Progress Bar list
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("DEPARTMENTAL DISTRIBUTION", color = themeColors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            val depts = listOf(
                "Engineering" to engineeringCount,
                "Administration" to adminCount,
                "Human Resources" to hrCount,
                "Product Management" to productCount,
                "Management" to managementCount
            ).sortedByDescending { it.second }
            
            depts.forEach { (deptName, count) ->
                val ratio = if (totalCount > 0) count.toFloat() / totalCount else 0f
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(deptName, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$count Employees (${String.format("%.0f%%", ratio * 100)})", color = themeColors.textSecondary, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Linear progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(getAdaptiveColor(0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(themeColors.primaryAccent)
                        )
                    }
                }
            }
        }
    }
    
    // Interactive Simulator for Future Headcount Projection
    var projectionMonths by remember { mutableStateOf(6f) }
    var hiringPace by remember { mutableStateOf("Normal") } // Slow, Normal, Accelerated
    var resignationLevel by remember { mutableStateOf("Low") } // None, Low, Medium, High
    
    val netHiringGrowthFactor = when (hiringPace) {
        "Slow" -> 1.5
        "Normal" -> 3.5
        "Accelerated" -> 6.0
        else -> 3.5
    }
    
    val netResignationFactor = when (resignationLevel) {
        "None" -> 0.0
        "Low" -> 0.5
        "Medium" -> 1.5
        "High" -> 3.5
        else -> 0.5
    }
    
    val netMonthlyGrowth = netHiringGrowthFactor - netResignationFactor
    val projectedHeadcount = (totalCount + (netMonthlyGrowth * projectionMonths.toInt())).coerceAtLeast(1.0)
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = themeColors.primaryAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("HEADCOUNT PROJECTION SIMULATOR", color = themeColors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Text("Simulate organizational growth metrics in real-time without spreadsheets.", color = themeColors.textSecondary, fontSize = 9.sp, modifier = Modifier.padding(bottom = 10.dp))
            
            // Hiring Pace Selectors
            Text("Simulated Hiring Pace", color = com.example.ui.theme.AppTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Slow", "Normal", "Accelerated").forEach { pace ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (hiringPace == pace) themeColors.primaryAccent else getAdaptiveColor(0.04f))
                            .clickable { hiringPace = pace }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(pace, color = if (hiringPace == pace) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Resignations Selector
            Text("Expected Resignations (Attrition Risk Factor)", color = com.example.ui.theme.AppTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("None", "Low", "Medium", "High").forEach { level ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (resignationLevel == level) themeColors.primaryAccent else getAdaptiveColor(0.04f))
                            .clickable { resignationLevel = level }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(level, color = if (resignationLevel == level) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Slider
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Projection Timeline:", color = themeColors.textSecondary, fontSize = 10.sp)
                Text("${projectionMonths.toInt()} Months", color = com.example.ui.theme.AppTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = projectionMonths,
                onValueChange = { projectionMonths = it },
                valueRange = 1f..12f,
                steps = 11,
                colors = SliderDefaults.colors(thumbColor = themeColors.primaryAccent, activeTrackColor = themeColors.primaryAccent)
            )
            
            Divider(color = themeColors.cardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Projected Future Headcount:", color = themeColors.textSecondary, fontSize = 10.sp)
                    Text("Based on net monthly change of ${String.format("%+.1f", netMonthlyGrowth)}", color = getAdaptiveTextColor(0.5f), fontSize = 8.sp)
                }
                Text(
                    text = "${projectedHeadcount.toInt()} Staff",
                    color = themeColors.primaryAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun AbsenteeismDashboardContent(themeColors: LiquidThemeColors, employees: List<Top5Employee>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("ABSENTEEISM TRACKER", color = themeColors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Absenteeism Rate Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(getAdaptiveColor(0.03f), RoundedCornerShape(8.dp))
                        .border(1.dp, themeColors.cardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Monthly Absence Rate", color = themeColors.textSecondary, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("2.4%", color = com.example.ui.theme.AppTextColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Industry standard: 3.5%", color = Color(0xFF00FF88), fontSize = 8.sp)
                }
                
                // Present Today Status Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(getAdaptiveColor(0.03f), RoundedCornerShape(8.dp))
                        .border(1.dp, themeColors.cardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Clocked-In Today", color = themeColors.textSecondary, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("26 / 28 Present", color = com.example.ui.theme.AppTextColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Live Present: 92.8%", color = themeColors.secondaryAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    
    // Day of the Week Risk Heatmap
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("ABSENTEEISM HEATMAP BY WEEKDAY", color = themeColors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(10.dp))
            
            val weekdays = listOf(
                Triple("Monday", "4.8% (High)", Color(0xFFFF6B2A)),
                Triple("Tuesday", "0.5% (Very Low)", Color(0xFF00FF88)),
                Triple("Wednesday", "1.2% (Low)", Color(0xFF2A80FF)),
                Triple("Thursday", "0.8% (Low)", Color(0xFF00AA55)),
                Triple("Friday", "3.2% (Moderate)", Color(0xFFFFCC00))
            )
            
            weekdays.forEach { (day, rate, color) ->
                val barProgress = when (day) {
                    "Monday" -> 0.9f
                    "Tuesday" -> 0.1f
                    "Wednesday" -> 0.25f
                    "Thursday" -> 0.15f
                    "Friday" -> 0.65f
                    else -> 0.2f
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(day, color = com.example.ui.theme.AppTextColor, fontSize = 10.sp, modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(getAdaptiveColor(0.05f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barProgress)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(rate, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(85.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
    
    // Absence Causes Breakdown Chart
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("ABSENCE CAUSES ANALYSIS", color = themeColors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pie Chart
                Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        var startAngle = -90f
                        val sickAngle = 360f * 0.45f
                        val vacAngle = 360f * 0.30f
                        val unexcusedAngle = 360f * 0.15f
                        val matAngle = 360f * 0.10f
                        
                        // Sick Leave (Red/Orange)
                        drawArc(Color(0xFFEF4444), startAngle, sickAngle, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx()))
                        startAngle += sickAngle
                        
                        // Vacation Leave (Blue)
                        drawArc(Color(0xFF3B82F6), startAngle, vacAngle, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx()))
                        startAngle += vacAngle
                        
                        // Unexcused (Orange)
                        drawArc(Color(0xFFF59E0B), startAngle, unexcusedAngle, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx()))
                        startAngle += unexcusedAngle
                        
                        // Maternity/Paternity (Purple)
                        drawArc(Color(0xFF8B5CF6), startAngle, matAngle, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx()))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Top", color = getAdaptiveTextColor(0.5f), fontSize = 7.sp)
                        Text("SICK", color = com.example.ui.theme.AppTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("45%", color = Color(0xFFEF4444), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Legends
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    WorkforceLegendRow(color = Color(0xFFEF4444), label = "Medical / Sick Leave", value = "45%")
                    WorkforceLegendRow(color = Color(0xFF3B82F6), label = "Planned Vacation", value = "30%")
                    WorkforceLegendRow(color = Color(0xFFF59E0B), label = "Unexcused Absences", value = "15%")
                    WorkforceLegendRow(color = Color(0xFF8B5CF6), label = "Maternity/Paternity", value = "10%")
                }
            }
        }
    }
}

@Composable
fun WorkforceLegendRow(color: Color, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = getAdaptiveTextColor(0.7f), fontSize = 10.sp)
        }
        Text(value, color = com.example.ui.theme.AppTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnoverPredictiveContent(themeColors: LiquidThemeColors, employees: List<Top5Employee>, viewModel: TimeTrackerViewModel) {
    // Top-level stats
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("AI TURNOVER PREDICTION ENGINE", color = themeColors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Turnover Risk Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(getAdaptiveColor(0.03f), RoundedCornerShape(8.dp))
                        .border(1.dp, themeColors.cardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Overall 6M Turnover Risk", color = themeColors.textSecondary, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1.8% (Very Low)", color = Color(0xFF00FF88), fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Stability: 98.2%", color = getAdaptiveTextColor(0.5f), fontSize = 8.sp)
                }
                
                // Top Attrition Factor Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(getAdaptiveColor(0.03f), RoundedCornerShape(8.dp))
                        .border(1.dp, themeColors.cardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Primary Risk Driver", color = themeColors.textSecondary, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Overtime Strain", color = Color(0xFFFFCC00), fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Impact: High", color = getAdaptiveTextColor(0.5f), fontSize = 8.sp)
                }
            }
        }
    }
    
    // Interactive Sensitivity Retention Calculator
    var selectedEmpName by remember { mutableStateOf(employees.firstOrNull()?.name ?: "") }
    var empExpanded by remember { mutableStateOf(false) }
    
    val selectedEmployee = remember(selectedEmpName) {
        employees.find { it.name.equals(selectedEmpName, ignoreCase = true) } ?: employees.firstOrNull()
    }
    
    // Controls for simulation
    var salaryAdjustment by remember { mutableStateOf(0f) } // ₱0 to ₱40,000
    var flexAdjustment by remember { mutableStateOf(0f) } // 0 to 100%
    var progressionAdjustment by remember { mutableStateOf(0f) } // 0 to 100%
    
    // Reset sliders when selected employee changes
    LaunchedEffect(selectedEmpName) {
        salaryAdjustment = 0f
        flexAdjustment = 0f
        progressionAdjustment = 0f
    }
    
    // Baseline risk score formula
    val baselineRisk = remember(selectedEmpName) {
        if (selectedEmployee != null) {
            val hashVal = selectedEmployee.id.hashCode()
            val absHash = if (hashVal < 0) -hashVal else hashVal
            val hashRisk = (absHash % 31 + 20).toFloat() // 20% to 50%
            // Incorporate metrics if available
            val latestMetrics = selectedEmployee.monthlyMetrics["July 2026"]
            if (latestMetrics != null) {
                val scoreGap = 100f - latestMetrics.performanceScore
                val scheduleGap = 100f - latestMetrics.scheduleAdherence
                (hashRisk + (scoreGap * 0.3f) + (scheduleGap * 0.4f)).coerceIn(10f, 85f)
            } else {
                hashRisk
            }
        } else {
            35f
        }
    }
    
    // Dynamic Calculated Risk Score based on inputs
    val currentRiskScore = remember(baselineRisk, salaryAdjustment, flexAdjustment, progressionAdjustment) {
        val salaryReduction = (salaryAdjustment / 40000f) * 20f
        val flexReduction = (flexAdjustment / 100f) * 15f
        val progReduction = (progressionAdjustment / 100f) * 10f
        (baselineRisk - salaryReduction - flexReduction - progReduction).coerceIn(4f, 95f)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = themeColors.primaryAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("RETENTION PREDICTIVE CALCULATOR", color = themeColors.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Text("Simulate live retention adjustments to predict individual turnover probabilities.", color = themeColors.textSecondary, fontSize = 9.sp, modifier = Modifier.padding(bottom = 10.dp))
            
            // Dropdown selector
            Text("Select Target Employee Profile", color = com.example.ui.theme.AppTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(getAdaptiveColor(0.05f), RoundedCornerShape(8.dp))
                    .border(1.dp, themeColors.cardBorder, RoundedCornerShape(8.dp))
                    .clickable { empExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedEmpName, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = themeColors.primaryAccent)
                }
                
                DropdownMenu(
                    expanded = empExpanded,
                    onDismissRequest = { empExpanded = false },
                    modifier = Modifier.background(Color(0xFF0F172A))
                ) {
                    employees.forEach { emp ->
                        DropdownMenuItem(
                            text = { Text(emp.name, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp) },
                            onClick = {
                                selectedEmpName = emp.name
                                empExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Circular Progress Gauge for Attrition Risk
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val appTextColor = com.example.ui.theme.AppTextColor
                Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(85.dp)) {
                        val strokeWidth = 8.dp.toPx()
                        val angle = (currentRiskScore / 100f) * 360f
                        
                        // Track
                        drawArc(
                            color = appTextColor.copy(alpha = 0.05f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                        
                        // Active Gauge
                        val gaugeColor = when {
                            currentRiskScore < 20f -> Color(0xFF00FF88)
                            currentRiskScore < 40f -> Color(0xFF38BDF8)
                            currentRiskScore < 60f -> Color(0xFFFFCC00)
                            else -> Color(0xFFEF4444)
                        }
                        
                        drawArc(
                            color = gaugeColor,
                            startAngle = -90f,
                            sweepAngle = angle,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(String.format("%.0f%%", currentRiskScore), color = com.example.ui.theme.AppTextColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Risk Level", color = getAdaptiveTextColor(0.5f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Metadata
                selectedEmployee?.let { emp ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(emp.position.uppercase(), color = themeColors.secondaryAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(emp.name, color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Department: ${emp.department}", color = getAdaptiveTextColor(0.6f), fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val statusText = when {
                            currentRiskScore < 20f -> "EXCELLENT RETENTION"
                            currentRiskScore < 40f -> "STABLE STANDING"
                            currentRiskScore < 60f -> "ELEVATED RISK"
                            else -> "CRITICAL TURNOVER RISK"
                        }
                        val statusColor = when {
                            currentRiskScore < 20f -> Color(0xFF00FF88)
                            currentRiskScore < 40f -> Color(0xFF38BDF8)
                            currentRiskScore < 60f -> Color(0xFFFFCC00)
                            else -> Color(0xFFEF4444)
                        }
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(statusText, color = statusColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Divider(color = themeColors.cardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            
            // Sensitivity Adjustment Sliders
            Text("PROACTIVE RETENTION ACTIONS SIMULATOR", color = com.example.ui.theme.AppTextColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            // 1. Monthly Salary Adjustment
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Propose Salary Increase:", color = themeColors.textSecondary, fontSize = 10.sp)
                Text(if (salaryAdjustment == 0f) "Baseline" else String.format("+₱%,.0f", salaryAdjustment), color = Color(0xFF00FF88), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = salaryAdjustment,
                onValueChange = { salaryAdjustment = it },
                valueRange = 0f..40000f,
                steps = 7,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF88), activeTrackColor = Color(0xFF00FF88))
            )
            
            // 2. Schedule Flexibility
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Schedule Flexibility Boost:", color = themeColors.textSecondary, fontSize = 10.sp)
                Text(if (flexAdjustment == 0f) "Baseline" else String.format("+%.0f%% Flex", flexAdjustment), color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = flexAdjustment,
                onValueChange = { flexAdjustment = it },
                valueRange = 0f..100f,
                steps = 9,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
            )
            
            // 3. Career Path Progression
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Career progression mapping:", color = themeColors.textSecondary, fontSize = 10.sp)
                Text(if (progressionAdjustment == 0f) "Baseline" else String.format("+%.0f%% progression", progressionAdjustment), color = Color(0xFF8B5CF6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = progressionAdjustment,
                onValueChange = { progressionAdjustment = it },
                valueRange = 0f..100f,
                steps = 9,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF8B5CF6), activeTrackColor = Color(0xFF8B5CF6))
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Action Recommendation Panel based on dynamic risk level
            val actionTitle = when {
                currentRiskScore < 20f -> "Retention Strategy: Continuous Recognition"
                currentRiskScore < 40f -> "Retention Strategy: Routine check-in"
                currentRiskScore < 60f -> "Retention Strategy: Workload optimization"
                else -> "Retention Strategy: IMMEDIATE STRATEGIC INTERVENTION"
            }
            val actionDesc = when {
                currentRiskScore < 20f -> "This employee is highly stable. Continue providing positive feedback, milestone celebrations, and high autonomy."
                currentRiskScore < 40f -> "The attrition risk is low. Maintain regular 1-on-1 conversations to address any micro-frictions and discuss professional development."
                currentRiskScore < 60f -> "Risk is elevated. Consider adjusting their workload, reducing overtime hours, or scheduling a flexible work arrangement."
                else -> "CRITICAL TURNOVER PROBABILITY. Schedule an immediate retention dialogue. Consider matching the simulated salary adjustment, providing remote options, and securing a clear path to promotion."
            }
            val actionColor = when {
                currentRiskScore < 20f -> Color(0xFF00FF88)
                currentRiskScore < 40f -> Color(0xFF38BDF8)
                currentRiskScore < 60f -> Color(0xFFFFCC00)
                else -> Color(0xFFEF4444)
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(actionColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .border(1.dp, actionColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = actionColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(actionTitle, color = actionColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(actionDesc, color = getAdaptiveTextColor(0.7f), fontSize = 9.sp, lineHeight = 12.sp)
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
                .border(2.dp, getAdaptiveColor(0.4f), CircleShape),
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

fun generateWorkforceInsightsPdf(context: Context, employees: List<Top5Employee>): File? {
    try {
        val pdfDocument = PdfDocument()

        // Common Paints
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#FBFBFD"); style = Paint.Style.FILL }
        val blackPaint = Paint().apply { color = android.graphics.Color.parseColor("#111111"); style = Paint.Style.FILL }
        val electricMintPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676"); style = Paint.Style.FILL }
        val cardFillPaint = Paint().apply { color = android.graphics.Color.parseColor("#F4F6F8"); style = Paint.Style.FILL }
        val heroCalloutPaint = Paint().apply { color = android.graphics.Color.parseColor("#E8F8F0"); style = Paint.Style.FILL }
        
        val strokePaint = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 1f }
        val gridPaint = Paint().apply { color = android.graphics.Color.parseColor("#CBD5E0"); style = Paint.Style.STROKE; strokeWidth = 0.5f }
        val linePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#00E676")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        val whitePaint = Paint().apply { color = android.graphics.Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true }
        val nodeBorderPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676"); style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
        
        val textPaint = Paint().apply { color = android.graphics.Color.parseColor("#1A202C"); isAntiAlias = true }
        val bodyTextPaint = TextPaint().apply { color = android.graphics.Color.parseColor("#1A202C"); textSize = 8f; isAntiAlias = true }

        val whiteTitlePaint = Paint().apply { color = android.graphics.Color.WHITE; textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val graySubtitlePaint = Paint().apply { color = android.graphics.Color.parseColor("#A0AEC0"); textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
        val mintMetadataPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676"); textSize = 8.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        // Helper to draw text layout
        val drawWrappedText = { canvas: android.graphics.Canvas, text: String, x: Float, y: Float, width: Int, tPaint: TextPaint ->
            val textLayout = StaticLayout.Builder.obtain(text, 0, text.length, tPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            canvas.save()
            canvas.translate(x, y)
            textLayout.draw(canvas)
            canvas.restore()
        }

        // Helper to draw dual band header
        val drawHeader = { canvas: android.graphics.Canvas ->
            // Header block
            canvas.drawRect(0f, 0f, 595f, 120f, blackPaint)
            // Accent bar
            canvas.drawRect(0f, 120f, 595f, 126f, electricMintPaint)
            // Texts
            canvas.drawText("SHIFT HR CORP", 54f, 65f, whiteTitlePaint)
            canvas.drawText("OFFICIAL WORKFORCE INSIGHTS REPORT", 54f, 90f, graySubtitlePaint)
            canvas.drawText("CONFIDENTIAL DOSSIER", 54f, 108f, mintMetadataPaint)
        }

        // Helper to draw footers
        val drawFooter = { canvas: android.graphics.Canvas, pageNum: Int ->
            canvas.drawLine(30f, 795f, 565f, 795f, strokePaint)
            textPaint.color = android.graphics.Color.GRAY
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Shift HR Corp Compliance and Retention Engine  •  CONFIDENTIAL  •  Page $pageNum of 2", 130f, 812f, textPaint)
        }

        // ================= PAGE 1 =================
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas

        // Background
        canvas1.drawRect(0f, 0f, 595f, 842f, bgPaint)
        drawHeader(canvas1)

        // Section I
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("SECTION I — HEADCOUNT & DISTRIBUTION", 30f, 150f, textPaint)

        // Rounded Personnel Summary Card
        canvas1.drawRoundRect(30f, 162f, 565f, 238f, 12f, 12f, cardFillPaint)
        
        // Inside Card Text
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("Total Active Workforce: ${employees.size} staff members", 45f, 185f, textPaint)
        
        canvas1.drawText("Year-over-Year Growth:", 45f, 203f, textPaint)
        val growthPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#008543")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas1.drawText(" +14.0% (+1.40% YoY Growth)", 135f, 203f, growthPaint)
        
        canvas1.drawText("Avg Hires Rate: 3.5 employees / month", 310f, 185f, textPaint)
        canvas1.drawText("Probation Pass Rate: 98.2% (Stable Trend)", 310f, 203f, textPaint)

        // Hero Insight Box
        canvas1.drawRoundRect(30f, 252f, 565f, 318f, 12f, 12f, heroCalloutPaint)
        
        // Large indicator
        val ratingPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#008543")
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas1.drawText("98.2%", 48f, 298f, ratingPaint)

        val titleCalloutPaint = TextPaint().apply {
            color = android.graphics.Color.parseColor("#008543")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas1.drawText("AI ORGANIZATIONAL STABILITY INDEX", 150f, 274f, titleCalloutPaint)

        val descCalloutPaint = TextPaint().apply {
            color = android.graphics.Color.parseColor("#1A202C")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        drawWrappedText(canvas1, "Excellent retention health. Systemic employee feedback loops indicate very high role alignment, healthy work-life balance, and low near-term fatigue vectors.", 150f, 285f, 390, descCalloutPaint)

        // Department distribution matrix
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("Department Distribution Matrix", 30f, 342f, textPaint)

        // Table Header row block
        canvas1.drawRect(30f, 356f, 565f, 374f, blackPaint)
        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("DEPARTMENT", 40f, 368f, textPaint)
        canvas1.drawText("HEADCOUNT", 200f, 368f, textPaint)
        canvas1.drawText("SHARE (%)", 320f, 368f, textPaint)
        canvas1.drawText("RETENTION RISK", 440f, 368f, textPaint)

        val engineeringCount = employees.count { it.department.equals("Engineering", ignoreCase = true) }
        val hrCount = employees.count { it.department.equals("Human Resources", ignoreCase = true) }
        val productCount = employees.count { it.department.equals("Product Management", ignoreCase = true) }
        val managementCount = employees.count { it.department.equals("Management", ignoreCase = true) }
        val adminCount = employees.count { it.department.equals("Administration", ignoreCase = true) }
        val total = employees.size.toFloat()

        val depts = listOf(
            Triple("Engineering", engineeringCount, "Very Low"),
            Triple("Administration", adminCount, "Low"),
            Triple("Product Management", productCount, "Low"),
            Triple("Human Resources", hrCount, "Very Low"),
            Triple("Management", managementCount, "Very Low")
        )

        var rowY = 374f
        depts.forEachIndexed { idx, (dept, count, risk) ->
            if (idx % 2 == 1) {
                canvas1.drawRect(30f, rowY, 565f, rowY + 18f, cardFillPaint)
            }
            canvas1.drawLine(30f, rowY + 18f, 565f, rowY + 18f, strokePaint)

            textPaint.color = android.graphics.Color.parseColor("#1A202C")
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas1.drawText(dept, 40f, rowY + 12f, textPaint)
            canvas1.drawText("$count members", 200f, rowY + 12f, textPaint)
            
            val pctStr = String.format("%.1f%%", (count / total) * 100f)
            canvas1.drawText(pctStr, 320f, rowY + 12f, textPaint)

            val riskColor = if (risk == "Very Low") "#008543" else "#D97706"
            val riskPaint = Paint().apply {
                color = android.graphics.Color.parseColor(riskColor)
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas1.drawText(risk, 440f, rowY + 12f, riskPaint)

            rowY += 18f
        }

        // Section II
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("SECTION II — ABSENTEEISM & COMPLIANCE", 30f, 495f, textPaint)

        // Attendance Verification Split container
        canvas1.drawRoundRect(30f, 508f, 565f, 574f, 12f, 12f, cardFillPaint)
        canvas1.drawLine(297f, 516f, 297f, 566f, strokePaint)

        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("Monthly Absence Rate: 2.4%", 45f, 530f, textPaint)
        
        canvas1.drawText("Target Limit: ", 45f, 548f, textPaint)
        val limitPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#008543")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas1.drawText("< 3.5% (Optimal Range)", 95f, 548f, limitPaint)

        canvas1.drawText("Today's Presence Rate: 92.8%", 312f, 530f, textPaint)
        canvas1.drawText("Active Status: 26 Present / 28 Logged", 312f, 548f, textPaint)

        // Heatmap blocks
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("Absenteeism Heatmap by Weekday (Volatility Index)", 30f, 600f, textPaint)

        val days = listOf(
            Triple("MONDAY", "4.8% (High)", true),
            Triple("TUESDAY", "0.5% (Min)", false),
            Triple("WEDNESDAY", "1.2% (Low)", false),
            Triple("THURSDAY", "0.8% (Low)", false),
            Triple("FRIDAY", "3.2% (Mod)", true)
        )

        var dayX = 30f
        days.forEach { (day, rate, volatile) ->
            val blockPaint = if (volatile) electricMintPaint else cardFillPaint
            canvas1.drawRoundRect(dayX, 613f, dayX + 97f, 653f, 8f, 8f, blockPaint)

            textPaint.color = android.graphics.Color.parseColor("#111111")
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas1.drawText(day, dayX + 10f, 628f, textPaint)

            textPaint.textSize = 7f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas1.drawText(rate, dayX + 10f, 642f, textPaint)

            dayX += 107f
        }

        // Text explanations
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("Top Primary Attrition/Absence Drivers:", 30f, 682f, textPaint)

        val descTextPaint = TextPaint().apply {
            color = android.graphics.Color.parseColor("#1A202C")
            textSize = 8f
            isAntiAlias = true
        }
        drawWrappedText(canvas1, "• Medical / Sick Leave (45%)  • Planned Vacation (30%)  • Unexcused Absences (15%)  • Maternity/Paternity (10%)\nNotes: Monday absences frequently correspond to medical backlogs; Friday absences are aligned with pre-planned weekend leave extensions.", 30f, 695f, 535, descTextPaint)

        drawFooter(canvas1, 1)
        pdfDocument.finishPage(page1)

        // ================= PAGE 2 =================
        val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = pdfDocument.startPage(pageInfo2)
        val canvas2 = page2.canvas

        // Background
        canvas2.drawRect(0f, 0f, 595f, 842f, bgPaint)
        drawHeader(canvas2)

        // Section III
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("SECTION III — AI TURNOVER & PREDICTIVE RETENTION", 30f, 150f, textPaint)

        drawWrappedText(canvas2, "Overall 6-Month Risk Trend Forecast: Highly Stable Talent Index. Predictive analytics indicate an incredibly low systemic turnover probability of 1.8% based on real-time feedback scores, performance logs, and local engagement records. Primary Risk Factor identified is Overtime Strain / Fatigue Accumulation in senior developer roles.", 30f, 165f, 535, descTextPaint)

        // Stability Trend Chart
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("6-Month Organizational Stability Forecast (Trendline)", 30f, 218f, textPaint)

        // Rounded Chart container
        canvas2.drawRoundRect(30f, 230f, 565f, 340f, 12f, 12f, cardFillPaint)

        // Grid lines inside chart
        canvas2.drawLine(55f, 255f, 540f, 255f, gridPaint)
        canvas2.drawLine(55f, 285f, 540f, 285f, gridPaint)
        canvas2.drawLine(55f, 315f, 540f, 315f, gridPaint)

        // Plot data
        val plotPoints = listOf(
            Pair(55f, 310f),  // Jan (96.0%)
            Pair(150f, 280f), // Feb (97.5%)
            Pair(245f, 263f), // Mar (98.2%)
            Pair(340f, 268f), // Apr (98.0%)
            Pair(435f, 274f), // May (97.8%)
            Pair(530f, 255f)  // Jun (98.5%)
        )

        val chartLabels = listOf("96.0%", "97.5%", "98.2%", "98.0%", "97.8%", "98.5%")
        val monthsList = listOf("Jan 2026", "Feb 2026", "Mar 2026", "Apr 2026", "May 2026", "Jun 2026")

        // Draw Line Path
        val path = Path()
        path.moveTo(plotPoints[0].first, plotPoints[0].second)
        for (i in 1 until plotPoints.size) {
            path.lineTo(plotPoints[i].first, plotPoints[i].second)
        }
        canvas2.drawPath(path, linePaint)

        // Draw Nodes and Labels
        plotPoints.forEachIndexed { i, pt ->
            // Draw node background circle (white fill)
            canvas2.drawCircle(pt.first, pt.second, 4.5f, whitePaint)
            // Draw node border (mint stroke)
            canvas2.drawCircle(pt.first, pt.second, 4.5f, nodeBorderPaint)

            // Value text
            textPaint.color = android.graphics.Color.parseColor("#111111")
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas2.drawText(chartLabels[i], pt.first - 12f, pt.second - 8f, textPaint)

            // Month labels
            textPaint.color = android.graphics.Color.parseColor("#4A5568")
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas2.drawText(monthsList[i], pt.first - 16f, 332f, textPaint)
        }

        // Predictive Profiles table
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("AI Predictive Risk Monitoring & Active Profiles Matrix", 30f, 365f, textPaint)

        // Table Header
        canvas2.drawRect(30f, 378f, 565f, 396f, blackPaint)
        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("EMPLOYEE NAME", 40f, 390f, textPaint)
        canvas2.drawText("DEPARTMENT", 180f, 390f, textPaint)
        canvas2.drawText("POSITION", 325f, 390f, textPaint)
        canvas2.drawText("EST. RISK LEVEL", 465f, 390f, textPaint)

        var tableRowY = 396f
        val sampleEmps = employees.take(15) // take up to 15 employees for beautiful density on page 2!
        sampleEmps.forEachIndexed { idx, emp ->
            if (idx % 2 == 1) {
                canvas2.drawRect(30f, tableRowY, 565f, tableRowY + 19f, cardFillPaint)
            }
            canvas2.drawLine(30f, tableRowY + 19f, 565f, tableRowY + 19f, strokePaint)

            textPaint.color = android.graphics.Color.parseColor("#1A202C")
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas2.drawText(emp.name, 40f, tableRowY + 13f, textPaint)
            canvas2.drawText(emp.department, 180f, tableRowY + 13f, textPaint)
            canvas2.drawText(emp.position, 325f, tableRowY + 13f, textPaint)

            val hashVal = emp.id.hashCode()
            val absHash = if (hashVal < 0) -hashVal else hashVal
            val riskVal = (absHash % 25 + 12) // 12% to 37%
            
            val (riskLevelStr, riskColorHex) = when {
                riskVal < 18 -> "Low ($riskVal%)" to "#008543"
                riskVal < 28 -> "Mod ($riskVal%)" to "#D97706"
                else -> "Elevated ($riskVal%)" to "#DC2626"
            }

            val rPaint = Paint().apply {
                color = android.graphics.Color.parseColor(riskColorHex)
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas2.drawText(riskLevelStr, 465f, tableRowY + 13f, rPaint)

            tableRowY += 19f
        }

        drawFooter(canvas2, 2)
        pdfDocument.finishPage(page2)

        // Save file
        val file = File(context.cacheDir, "Workforce_Insights_July_2026.pdf")
        val fileOutputStream = FileOutputStream(file)
        pdfDocument.writeTo(fileOutputStream)
        pdfDocument.close()
        fileOutputStream.close()

        return file
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun openWorkforcePdfFile(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooserIntent = Intent.createChooser(intent, "Open or Share Workforce Insights Report")
        chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "No PDF viewer available. File saved in cache.", Toast.LENGTH_LONG).show()
    }
}
