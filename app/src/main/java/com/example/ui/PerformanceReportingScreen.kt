package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.viewmodel.*
import com.example.ui.theme.*
import com.example.data.database.TimeLogEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.PI

// Static Theme Color Constants to completely bypass Composable getter limits in Canvas
val AppNeonGreen = Color(0xFF00FF88)
val AppDarkGreyBg = Color(0xFF0F0F11)
val AppCardGreyBg = Color(0xFF1E1E24)
val AppBorderGrey = Color(0xFF2C2C35)

// Data models for review metrics
data class KpiCompetency(
    val competency: String,
    val selfScore: Float,
    val managerScore: Float,
    val description: String,
    val weight: Float = 0.2f
)

data class HistoricalScore(
    val quarter: String,
    val score: Float
)

data class DepartmentTarget(
    val department: String,
    val target: Float,
    val actual: Float,
    val status: String
)

data class PerformanceStats(
    val totalShifts: Int,
    val totalHours: Double,
    val overtimeHours: Double,
    val approvalRate: Double,
    val basicRate: Double
)

@Composable
fun PerformanceReportingScreen(viewModel: TimeTrackerViewModel) {
    val context = LocalContext.current
    val userRole = viewModel.currentUserRole.value
    val currentUserName = viewModel.currentUserName.value
    
    // Resolve dynamic Liquid Glass theme
    val themeName = viewModel.selectedTheme.value
    val themeColors = LiquidThemeRegistry.getThemeByName(themeName)
    
    // RBAC: Check if user belongs to the HR / Admin / Management Team
    val isHrOrAdmin = userRole == "ADMIN_HR" || userRole == "MANAGER" || userRole == "SUPERVISOR"
    
    // Available employees to view (for Admin/HR only)
    val profiles by viewModel.employeeProfiles
    var selectedProfile by remember { 
        mutableStateOf(
            if (isHrOrAdmin) {
                profiles.firstOrNull { it.name == "Sarah Jenkins" } ?: profiles.firstOrNull()
            } else {
                profiles.firstOrNull { it.name == currentUserName } ?: profiles.firstOrNull()
            }
        )
    }
    
    // If the role switches dynamically, update selected profile
    LaunchedEffect(userRole, currentUserName) {
        selectedProfile = if (isHrOrAdmin) {
            profiles.firstOrNull { it.name == "Sarah Jenkins" } ?: profiles.firstOrNull()
        } else {
            profiles.firstOrNull { it.name == currentUserName } ?: profiles.firstOrNull()
        }
    }
    
    // Active Tab state
    var activeTab by remember { mutableStateOf("individual") } // individual, organization
    
    // All time logs gathered from the reactive Database state flow
    val allLogs by viewModel.allTimeLogs.collectAsState()
    val config by viewModel.shiftConfig.collectAsState()
    
    // 1. Calculate database computed stats for individual profile
    val profileStats = remember(selectedProfile, allLogs, config) {
        calculateStatsForEmployee(selectedProfile?.name ?: "", allLogs, config.hourlyRate.toDouble())
    }
    
    // 2. Fetch seeded KPI & historical scores for selected profile
    val kpis = remember(selectedProfile) {
        getKpisForEmployee(selectedProfile?.name ?: "")
    }
    
    val historicalScores = remember(selectedProfile) {
        getHistoricalScoresForEmployee(selectedProfile?.name ?: "")
    }
    
    // 3. Calculate company-wide aggregated metrics
    val companyStats = remember(allLogs) {
        calculateCompanyStats(allLogs)
    }
    
    val departmentTargets = remember {
        listOf(
            DepartmentTarget("Engineering", 4.5f, 4.7f, "Exceeded"),
            DepartmentTarget("Human Resources", 4.2f, 4.15f, "On Track"),
            DepartmentTarget("Product Management", 4.4f, 4.5f, "Exceeded"),
            DepartmentTarget("Management", 4.6f, 4.8f, "Exceeded"),
            DepartmentTarget("Administration", 4.5f, 4.9f, "Exceeded")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        if (userRole == "EMPLOYEE") {
            // For Employee role, just show the individual section directly, no tabs or extra title (matches screenshot exactly)
            IndividualAppraisalSection(
                context = context,
                isHrOrAdmin = isHrOrAdmin,
                profiles = profiles,
                selectedProfile = selectedProfile,
                onProfileSelected = { selectedProfile = it },
                kpis = kpis,
                historicalScores = historicalScores,
                stats = profileStats,
                userRole = userRole,
                themeColors = themeColors
            )
        } else {
            // Admin/HR view has tabs and full functionality
            Text(
                text = "Performance Appraisal & Reporting",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.primaryAccent,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColors.cardSurface, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val tabs = listOf(
                    "individual" to "Individual Appraisal",
                    "organization" to "Company Executive Audit"
                )
                tabs.forEach { (key, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == key) themeColors.primaryAccent else Color.Transparent)
                            .clickable { activeTab = key }
                            .padding(vertical = 8.dp)
                            .testTag("report_tab_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (key == "organization" && !isHrOrAdmin) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "RBAC Locked",
                                    tint = if (activeTab == key) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = label,
                                color = if (activeTab == key) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (activeTab) {
                "individual" -> {
                    IndividualAppraisalSection(
                        context = context,
                        isHrOrAdmin = isHrOrAdmin,
                        profiles = profiles,
                        selectedProfile = selectedProfile,
                        onProfileSelected = { selectedProfile = it },
                        kpis = kpis,
                        historicalScores = historicalScores,
                        stats = profileStats,
                        userRole = userRole,
                        themeColors = themeColors
                    )
                }
                "organization" -> {
                    if (!isHrOrAdmin) {
                        RbacLockedView(themeColors = themeColors)
                    } else {
                        OrganizationAuditSection(
                            context = context,
                            companyStats = companyStats,
                            departmentTargets = departmentTargets,
                            profiles = profiles,
                            themeColors = themeColors,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- SUB-SECTIONS & VIEWS ----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualAppraisalSection(
    context: Context,
    isHrOrAdmin: Boolean,
    profiles: List<EmployeeProfile>,
    selectedProfile: EmployeeProfile?,
    onProfileSelected: (EmployeeProfile) -> Unit,
    kpis: List<KpiCompetency>,
    historicalScores: List<HistoricalScore>,
    stats: PerformanceStats,
    userRole: String,
    themeColors: LiquidThemeColors
) {
    if (selectedProfile == null) return

    var expandedDropdown by remember { mutableStateOf(false) }
    var isScoreCardExpanded by remember { mutableStateOf(false) }
    var isTrendChartExpanded by remember { mutableStateOf(false) }
    val overallRating = remember(kpis) {
        kpis.map { it.managerScore * it.weight }.sum()
    }

    // Dropdown / Selector Card for HR Admin
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Target Review Subject",
                fontSize = 11.sp,
                color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(6.dp))

            if (isHrOrAdmin) {
                // Dropdown box
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedProfile.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("employee_selector_dropdown"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.primaryAccent,
                            unfocusedBorderColor = themeColors.cardBorder,
                            focusedTrailingIconColor = themeColors.primaryAccent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.background(themeColors.cardSurface.copy(alpha = 1f))
                    ) {
                        profiles.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile.name, color = com.example.ui.theme.AppTextColor, fontSize = 13.sp) },
                                onClick = {
                                    onProfileSelected(profile)
                                    expandedDropdown = false
                                },
                                modifier = Modifier.testTag("dropdown_item_${profile.id}")
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💼 Access Level: HR Admin / Supervisor (All Profiles Unlocked)",
                    fontSize = 10.sp,
                    color = themeColors.primaryAccent,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                // Lock icon and read-only text showing employee can only view self
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Shield Verified",
                        tint = themeColors.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Current Evaluation Status",
                            color = com.example.ui.theme.AppTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "🔐 Access Level: Employee Reviewee (Own Report Only)",
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Profile & Large Overall Score Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: Profile Information
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .background(themeColors.primaryAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = selectedProfile.id,
                        color = themeColors.primaryAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (userRole == "EMPLOYEE") "Your Overview" else selectedProfile.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = com.example.ui.theme.AppTextColor
                )
                Text(
                    text = selectedProfile.position,
                    fontSize = 12.sp,
                    color = com.example.ui.theme.AppTextColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Department: ${selectedProfile.department}",
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0)
                )
                Text(
                    text = "Status: ${selectedProfile.status}",
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Side: Beautiful Rating Gauge Callout
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                val appTextColor = com.example.ui.theme.AppTextColor
                // Background Track
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = appTextColor.copy(alpha = 0.08f),
                        startAngle = -220f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                // Foreground Progress Arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val angleProgress = (overallRating / 5.0f) * 260f
                    drawArc(
                        color = themeColors.primaryAccent,
                        startAngle = -220f,
                        sweepAngle = angleProgress,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.2f", overallRating),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = com.example.ui.theme.AppTextColor
                    )
                    Text(
                        text = "out of 5.0",
                        fontSize = 9.sp,
                        color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Real database stats card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Database Attendance Analytics (Live)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.primaryAccent
                )
                Icon(
                    imageVector = Icons.Default.QueryStats,
                    contentDescription = "DB Stats",
                    tint = themeColors.primaryAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "Attendance data syncs automatically with work logs.",
                fontSize = 10.sp,
                color = Color(0xFFE2E8F0),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = "${stats.totalShifts}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.AppTextColor)
                    Text(text = "Total Shifts", fontSize = 9.sp, color = Color(0xFFE2E8F0))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = String.format("%.1f hrs", stats.totalHours), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.AppTextColor)
                    Text(text = "Hours Worked", fontSize = 9.sp, color = Color(0xFFE2E8F0))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = String.format("%.1f hrs", stats.overtimeHours), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.AppTextColor)
                    Text(text = "Overtime", fontSize = 9.sp, color = Color(0xFFE2E8F0))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = String.format("%.0f%%", stats.approvalRate), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColors.primaryAccent)
                    Text(text = "Approve Rate", fontSize = 9.sp, color = Color(0xFFE2E8F0))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // KPI breakdown table - Expandable
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { isScoreCardExpanded = !isScoreCardExpanded }
            .testTag("kpi_breakdown_card_collapsible"),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Core Competencies & Score Card",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.AppTextColor
                    )
                    Text(
                        text = "Comparison of employee self-evaluations against supervisor audits.",
                        fontSize = 10.sp,
                        color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f)
                    )
                }
                Icon(
                    imageVector = if (isScoreCardExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isScoreCardExpanded) "Collapse" else "Expand",
                    tint = themeColors.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (isScoreCardExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                // Table Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                ) {
                    Text(text = "Competency / KPI", modifier = Modifier.weight(1.8f), fontSize = 10.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold)
                    Text(text = "Self", modifier = Modifier.weight(0.7f), fontSize = 10.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(text = "Mgr", modifier = Modifier.weight(0.7f), fontSize = 10.sp, color = themeColors.primaryAccent, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(text = "Var", modifier = Modifier.weight(0.7f), fontSize = 10.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }

                // Alternating Row KPI table
                kpis.forEachIndexed { index, kpi ->
                    val variance = kpi.managerScore - kpi.selfScore
                    val varianceColor = if (variance >= 0) themeColors.primaryAccent else Color(0xFFFF5555)
                    val varianceText = if (variance >= 0) "+${String.format("%.1f", variance)}" else String.format("%.1f", variance)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color.Transparent else Color.White.copy(alpha = 0.02f))
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.8f)) {
                            Text(text = kpi.competency, fontSize = 11.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold)
                            Text(text = kpi.description, fontSize = 9.sp, color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f))
                        }
                        Text(
                            text = String.format("%.1f", kpi.selfScore),
                            modifier = Modifier.weight(0.7f),
                            fontSize = 11.sp,
                            color = com.example.ui.theme.AppTextColor,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = String.format("%.1f", kpi.managerScore),
                            modifier = Modifier.weight(0.7f),
                            fontSize = 11.sp,
                            color = themeColors.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = varianceText,
                            modifier = Modifier.weight(0.7f),
                            fontSize = 11.sp,
                            color = varianceColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Interactive Historical Progress Chart - Expandable
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { isTrendChartExpanded = !isTrendChartExpanded }
            .testTag("historical_trend_card_collapsible"),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Historical Score Progress Trend",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.AppTextColor
                    )
                    Text(
                        text = "Performance reviews overall rating over the past five quarters.",
                        fontSize = 10.sp,
                        color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f)
                    )
                }
                Icon(
                    imageVector = if (isTrendChartExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isTrendChartExpanded) "Collapse" else "Expand",
                    tint = themeColors.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (isTrendChartExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                val appTextColor = com.example.ui.theme.AppTextColor

                // Canvas drawing for line graph
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(vertical = 8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        
                        // Margins for axes
                        val marginX = 40.dp.toPx()
                        val marginY = 20.dp.toPx()
                        
                        val graphW = w - marginX * 1.5f
                        val graphH = h - marginY * 2f
                        
                        // Grid lines (y-axis lines for scores 3.0 to 5.0)
                        val steps = 4
                        for (i in 0..steps) {
                            val scoreVal = 3.0f + i * 0.5f
                            val y = h - marginY - (scoreVal - 3.0f) / 2.0f * graphH
                            
                            // horizontal guide lines
                            drawLine(
                                color = appTextColor.copy(alpha = 0.08f),
                                start = Offset(marginX, y),
                                end = Offset(w - marginX * 0.5f, y),
                                strokeWidth = 1.dp.toPx()
                            )
                            
                            // draw score labels
                            drawContext.canvas.nativeCanvas.drawText(
                                String.format("%.1f", scoreVal),
                                10.dp.toPx(),
                                y + 4.dp.toPx(),
                                Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textSize = 8.dp.toPx()
                                    typeface = Typeface.DEFAULT_BOLD
                                }
                            )
                        }

                        // Plot the quarterly trend coordinates
                        val pointCount = historicalScores.size
                        val points = mutableListOf<Offset>()
                        
                        historicalScores.forEachIndexed { i, record ->
                            val x = marginX + (i.toFloat() / (pointCount - 1).toFloat()) * graphW
                            // map score 3.0f to 5.0f onto our pixel bounds
                            val scoreNorm = (record.score - 3.0f) / 2.0f
                            val y = h - marginY - scoreNorm * graphH
                            points.add(Offset(x, y))
                        }

                        // Draw connection path
                        if (points.isNotEmpty()) {
                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (idx in 1 until points.size) {
                                    lineTo(points[idx].x, points[idx].y)
                                }
                            }
                            
                            // Line stroke
                            drawPath(
                                path = path,
                                color = themeColors.primaryAccent,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                            
                            // Background gradient underneath line
                            val gradientPath = Path().apply {
                                moveTo(points[0].x, h - marginY)
                                for (p in points) {
                                    lineTo(p.x, p.y)
                                }
                                lineTo(points.last().x, h - marginY)
                                close()
                            }
                            
                            drawPath(
                                path = gradientPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(themeColors.primaryAccent.copy(alpha = 0.18f), Color.Transparent),
                                    startY = 0f,
                                    endY = h - marginY
                                )
                            )

                            // Draw points and labels
                            points.forEachIndexed { i, pt ->
                                drawCircle(
                                    color = themeColors.primaryAccent,
                                    radius = 4.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = appTextColor,
                                    radius = 2.dp.toPx(),
                                    center = pt
                                )
                                
                                // Quarter Label at bottom
                                drawContext.canvas.nativeCanvas.drawText(
                                    historicalScores[i].quarter,
                                    pt.x - 14.dp.toPx(),
                                    h - 4.dp.toPx(),
                                    Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 8.dp.toPx()
                                    }
                                )

                                // Rating score label above point
                                drawContext.canvas.nativeCanvas.drawText(
                                    String.format("%.2f", historicalScores[i].score),
                                    pt.x - 12.dp.toPx(),
                                    pt.y - 8.dp.toPx(),
                                    Paint().apply {
                                        color = themeColors.primaryAccent.toArgb()
                                        textSize = 8.dp.toPx()
                                        typeface = Typeface.DEFAULT_BOLD
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Row - PDF Download & Print Controls inside expand block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val file = generateIndividualPerformancePdf(context, selectedProfile, kpis, stats, historicalScores, overallRating)
                            if (file != null) {
                                Toast.makeText(context, "PDF successfully printed! File saved in cache.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to print PDF.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("download_individual_pdf_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Icon", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print Review PDF", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val file = generateIndividualPerformancePdf(context, selectedProfile, kpis, stats, historicalScores, overallRating)
                            if (file != null) {
                                openPdfFile(context, file)
                            } else {
                                Toast.makeText(context, "Could not open document.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("share_individual_pdf_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, themeColors.cardBorder)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Icon", tint = com.example.ui.theme.AppTextColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View & Share PDF", color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class ComparativeMetrics(
    val period: String,
    val newHires: Int,
    val separations: Int,
    val turnoverRate: Double,
    val promotions: Int,
    val demotions: Int,
    val jobTransfers: Int,
    val avgScore: Double
)

@Composable
fun OrganizationAuditSection(
    context: Context,
    companyStats: PerformanceStats,
    departmentTargets: List<DepartmentTarget>,
    profiles: List<EmployeeProfile>,
    themeColors: LiquidThemeColors,
    viewModel: TimeTrackerViewModel
) {
    // 1. Key Metrics Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val boxes = listOf(
            Triple("Evaluated Staff", "${profiles.size} Employees", Icons.Default.People),
            Triple("Company Avg", "4.61 / 5.0", Icons.Default.TrendingUp),
            Triple("Top Department", "Administration", Icons.Default.CorporateFare)
        )
        boxes.forEach { (title, valStr, icon) ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
                border = BorderStroke(1.dp, themeColors.cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = themeColors.primaryAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = title, fontSize = 9.sp, color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                    Text(text = valStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.AppTextColor, textAlign = TextAlign.Center)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // --- INTERACTIVE TREND COMPARISON BLOCK ---
    var selectedComparePeriod by remember { mutableStateOf("monthly") }
    
    val comparedList = remember(selectedComparePeriod) {
        when (selectedComparePeriod) {
            "yearly" -> listOf(
                ComparativeMetrics("2024", 42, 18, 4.20, 15, 0, 4, 4.35),
                ComparativeMetrics("2025", 48, 22, 3.80, 18, 1, 6, 4.48),
                ComparativeMetrics("2026 (YTD)", 26, 12, 2.25, 12, 0, 3, 4.65)
            )
            "quarterly" -> listOf(
                ComparativeMetrics("Q1 2026", 10, 4, 1.10, 4, 0, 1, 4.52),
                ComparativeMetrics("Q2 2026", 12, 6, 1.35, 5, 0, 1, 4.61),
                ComparativeMetrics("Q3 2026", 4, 2, 0.80, 3, 0, 1, 4.68)
            )
            else -> listOf(
                ComparativeMetrics("May 2026", 5, 2, 0.45, 2, 0, 0, 4.58),
                ComparativeMetrics("June 2026", 6, 1, 0.22, 3, 0, 1, 4.62),
                ComparativeMetrics("July 2026", 3, 1, 0.18, 1, 0, 1, 4.70)
            )
        }
    }

    val aiAnalysisText by remember { viewModel.aiAnalysisText }
    val aiAnalysisLoading by remember { viewModel.aiAnalysisLoading }
    val aiAnalysisError by remember { viewModel.aiAnalysisError }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Intelligent Workforce Comparison",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.AppTextColor
                    )
                    Text(
                        text = "Compare hires, attrition, promotions, and appraisal scores across periods.",
                        fontSize = 9.sp,
                        color = com.example.ui.theme.AppTextColor.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Period Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val periods = listOf("monthly" to "Monthly Assessment", "quarterly" to "Quarterly Audit", "yearly" to "Year-on-Year")
                periods.forEach { (key, title) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedComparePeriod == key) themeColors.primaryAccent else Color.Transparent)
                            .clickable { selectedComparePeriod = key }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (selectedComparePeriod == key) Color.Black else Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Comparative side-by-side columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                comparedList.forEach { row ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = row.period,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.primaryAccent,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            
                            // Turnover gauge summary
                            Column {
                                Text(text = "Turnover Rate", fontSize = 8.sp, color = com.example.ui.theme.AppTextColor.copy(alpha = 0.6f))
                                Text(
                                    text = String.format("%.2f%%", row.turnoverRate),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (row.turnoverRate < 2.0) Color(0xFF00FF88) else Color(0xFFFFCC00)
                                )
                            }

                            // Team Movements Summary
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "👥 Hires/Attrition", fontSize = 7.sp, color = com.example.ui.theme.AppTextColor.copy(alpha = 0.6f))
                                    Text(text = "${row.newHires}/${row.separations}", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.AppTextColor)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "⚡ Promo/Transf", fontSize = 7.sp, color = com.example.ui.theme.AppTextColor.copy(alpha = 0.6f))
                                    Text(text = "${row.promotions}/${row.jobTransfers}", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.AppTextColor)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "⭐ Avg Appraisal", fontSize = 7.sp, color = com.example.ui.theme.AppTextColor.copy(alpha = 0.6f))
                                    Text(text = String.format("%.2f", row.avgScore), fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = themeColors.primaryAccent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. INTERACTIVE AI ANALYSIS WORKSPACE
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Icon",
                    tint = themeColors.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Interactive AI HR Insights Desk",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.AppTextColor
                )
            }

            Text(
                text = "Compile multi-period statistics and synthesize smart recommendations on retention & career stability.",
                fontSize = 10.sp,
                color = com.example.ui.theme.AppTextColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            if (aiAnalysisLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = themeColors.primaryAccent, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Synthesizing metrics via secure AI Studio connection...", fontSize = 10.sp, color = com.example.ui.theme.AppTextColor)
                }
            } else {
                if (aiAnalysisText.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AI INTELLIGENCE REPORT (${selectedComparePeriod.uppercase()})",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.primaryAccent
                                )
                                Text(
                                    text = "⚡ Secure Sandbox Mode",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FF88)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiAnalysisText,
                                fontSize = 10.5.sp,
                                color = com.example.ui.theme.AppTextColor,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Print AI report button
                    Button(
                        onClick = {
                            val file = generateAiAnalysisPdf(context, selectedComparePeriod, comparedList, aiAnalysisText)
                            if (file != null) {
                                openPdfFile(context, file)
                            } else {
                                Toast.makeText(context, "Could not generate printed PDF report.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print AI Insights Executive Report to PDF", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action to generate / regenerate
                Button(
                    onClick = {
                        val metricsSummary = comparedList.joinToString("\n") { row ->
                            "- Period: ${row.period} | Hires: ${row.newHires} | Separations: ${row.separations} | Turnover: ${row.turnoverRate}% | Promotions: ${row.promotions} | Transfers: ${row.jobTransfers} | Avg Appraisals: ${row.avgScore}"
                        }
                        viewModel.analyzeWorkforceWithAi(selectedComparePeriod, metricsSummary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (aiAnalysisText.isEmpty()) "Trigger AI Analytical Recommendation" else "Regenerate Interactive AI Insights",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. Department targets vs performance table
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Departmental Milestone Audits",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = com.example.ui.theme.AppTextColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Comparison of current targets against database-aggregated results.",
                fontSize = 10.sp,
                color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(vertical = 6.dp, horizontal = 8.dp)
            ) {
                Text(text = "Department", modifier = Modifier.weight(1.5f), fontSize = 10.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold)
                Text(text = "Target", modifier = Modifier.weight(0.8f), fontSize = 10.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(text = "Actual", modifier = Modifier.weight(0.8f), fontSize = 10.sp, color = themeColors.primaryAccent, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(text = "Audit", modifier = Modifier.weight(0.9f), fontSize = 10.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }

            // Alternating Row table
            departmentTargets.forEachIndexed { index, dept ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (index % 2 == 0) Color.Transparent else Color.White.copy(alpha = 0.02f))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = dept.department, modifier = Modifier.weight(1.5f), fontSize = 11.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold)
                    Text(text = String.format("%.2f", dept.target), modifier = Modifier.weight(0.8f), fontSize = 11.sp, color = com.example.ui.theme.AppTextColor, textAlign = TextAlign.Center)
                    Text(text = String.format("%.2f", dept.actual), modifier = Modifier.weight(0.8f), fontSize = 11.sp, color = themeColors.primaryAccent, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (dept.actual >= dept.target) Color(0xFF00FF88).copy(alpha = 0.15f)
                                else Color(0xFFFFCC00).copy(alpha = 0.15f)
                            )
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dept.status,
                            color = if (dept.actual >= dept.target) Color(0xFF00FF88) else Color(0xFFFFCC00),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Normal Distribution Bell Curve Chart
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Performance Distribution (Normal Bell Curve)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = com.example.ui.theme.AppTextColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Visualization of staff count mapped across overall score frequencies.",
                fontSize = 10.sp,
                color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Canvas drawing for bell curve normal distribution
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(vertical = 8.dp)
            ) {
                val appTextColor = com.example.ui.theme.AppTextColor
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Normal curve parameters
                    val mu = 4.2f  // Mean performance score
                    val sigma = 0.4f // Standard deviation
                    
                    val marginX = 20.dp.toPx()
                    val marginY = 20.dp.toPx()
                    
                    val graphW = w - marginX * 2f
                    val graphH = h - marginY * 1.5f
                    
                    // Draw horizontal baseline
                    drawLine(
                        color = appTextColor.copy(alpha = 0.2f),
                        start = Offset(marginX, h - marginY),
                        end = Offset(w - marginX, h - marginY),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw scale indicators (Scores from 3.0 to 5.0)
                    val pointsCount = 100
                    val path = Path()
                    var isFirst = true
                    
                    // Score range to cover: 3.0 to 5.0
                    val minScore = 3.0f
                    val maxScore = 5.0f
                    
                    for (i in 0..pointsCount) {
                        val score = minScore + (i.toFloat() / pointsCount) * (maxScore - minScore)
                        // Bell curve normal equation: y = exp(-((x-mu)^2)/(2*sigma^2))
                        val exponent = -((score - mu).pow(2)) / (2 * sigma.pow(2))
                        val frequency = exp(exponent) // value between 0.0 and 1.0
                        
                        val x = marginX + (i.toFloat() / pointsCount) * graphW
                        val y = h - marginY - frequency * graphH
                        
                        if (isFirst) {
                            path.moveTo(x, y)
                            isFirst = false
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    // Fill gradient underneath the curve
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(w - marginX, h - marginY)
                        lineTo(marginX, h - marginY)
                        close()
                    }
                    
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(themeColors.primaryAccent.copy(alpha = 0.25f), Color.Transparent),
                            startY = h - marginY - graphH,
                            endY = h - marginY
                        )
                    )

                    // Draw the curve line
                    drawPath(
                        path = path,
                        color = themeColors.primaryAccent,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Mark Mean (mu) indicator
                    val muX = marginX + ((mu - minScore) / (maxScore - minScore)) * graphW
                    drawLine(
                        color = themeColors.primaryAccent.copy(alpha = 0.6f),
                        start = Offset(muX, h - marginY - graphH),
                        end = Offset(muX, h - marginY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Text labels for mean & scores
                    drawContext.canvas.nativeCanvas.drawText(
                        "Mean μ = 4.2",
                        muX - 24.dp.toPx(),
                        h - marginY - graphH - 4.dp.toPx(),
                        Paint().apply {
                            color = themeColors.primaryAccent.toArgb()
                            textSize = 8.dp.toPx()
                            typeface = Typeface.DEFAULT_BOLD
                        }
                    )

                    // Horizontal ticks labels
                    val scoreTicks = listOf(3.0f, 3.5f, 4.0f, 4.5f, 5.0f)
                    scoreTicks.forEach { tick ->
                        val tickX = marginX + ((tick - minScore) / (maxScore - minScore)) * graphW
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format("%.1f", tick),
                            tickX - 8.dp.toPx(),
                            h - 4.dp.toPx(),
                            Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 8.dp.toPx()
                            }
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 4. Top Performers Summary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Elite Performance Leaders",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = com.example.ui.theme.AppTextColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val eliteList = listOf(
                Pair("Aditya Joshi (Director)", "4.85 / 5.0"),
                Pair("Anjali Sharma (General Manager)", "4.78 / 5.0"),
                Pair("Sarah Jenkins (Senior Kotlin Developer)", "4.65 / 5.0")
            )

            eliteList.forEachIndexed { idx, leader ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                when (idx) {
                                    0 -> Color(0xFFFFD700).copy(alpha = 0.15f) // Gold
                                    1 -> Color(0xFFC0C0C0).copy(alpha = 0.15f) // Silver
                                    else -> Color(0xFFCD7F32).copy(alpha = 0.15f) // Bronze
                                },
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            color = when (idx) {
                                0 -> Color(0xFFFFD700)
                                1 -> Color(0xFFC0C0C0)
                                else -> Color(0xFFCD7F32)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = leader.first, fontSize = 11.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold)
                    }

                    Text(text = leader.second, fontSize = 11.sp, color = themeColors.primaryAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Organization Action Controls
    Button(
        onClick = {
            val file = generateCompanySummaryPdf(context, companyStats, departmentTargets)
            if (file != null) {
                openPdfFile(context, file)
            } else {
                Toast.makeText(context, "Could not print organizational report.", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("download_company_pdf_button"),
        colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryAccent),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Default.Print, contentDescription = "Print Executive Report", tint = Color.Black)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Print Org Executive Summary PDF", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RbacLockedView(themeColors: LiquidThemeColors) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(280.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardSurface.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, themeColors.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Shield Guard Lock",
                    tint = Color(0xFFFF5555),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ACCESS DENIED (RBAC SECURED)",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFF5555),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Strict Role-Based Access Control is enforced on company-wide executive dossiers. Employee-level accounts are restricted from viewing macro performance curves, organizational benchmarks, and peer ratings to preserve corporate privacy.",
                fontSize = 11.sp,
                color = com.example.ui.theme.AppTextColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = themeColors.primaryAccent, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Policy compliance active for Sarah Jenkins",
                    fontSize = 9.sp,
                    color = themeColors.primaryAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ---------------------- DATABASE METRICS CALCULATION ENGINE ----------------------

fun calculateStatsForEmployee(employeeName: String, logs: List<TimeLogEntity>, hourlyRate: Double): PerformanceStats {
    val userLogs = logs.filter { it.employeeName == employeeName }
    val totalShifts = userLogs.size
    
    var totalHours = 0.0
    var overtimeHours = 0.0
    var approvedCount = 0
    
    userLogs.forEach { log ->
        val timeIn = log.timeIn ?: return@forEach
        val timeOut = log.timeOut ?: return@forEach
        
        val deltaHrs = (timeOut - timeIn).toDouble() / 3600000.0
        totalHours += deltaHrs
        
        if (deltaHrs > 8.0) {
            overtimeHours += (deltaHrs - 8.0)
        }
        
        if (log.isApproved == "APPROVED") {
            approvedCount++
        }
    }
    
    val approvalRate = if (totalShifts > 0) (approvedCount.toDouble() / totalShifts.toDouble()) * 100.0 else 0.0
    
    return PerformanceStats(
        totalShifts = totalShifts,
        totalHours = totalHours,
        overtimeHours = overtimeHours,
        approvalRate = approvalRate,
        basicRate = hourlyRate
    )
}

fun calculateCompanyStats(logs: List<TimeLogEntity>): PerformanceStats {
    val totalShifts = logs.size
    var totalHours = 0.0
    var overtimeHours = 0.0
    var approvedCount = 0
    
    logs.forEach { log ->
        val timeIn = log.timeIn ?: return@forEach
        val timeOut = log.timeOut ?: return@forEach
        
        val deltaHrs = (timeOut - timeIn).toDouble() / 3600000.0
        totalHours += deltaHrs
        
        if (deltaHrs > 8.0) {
            overtimeHours += (deltaHrs - 8.0)
        }
        
        if (log.isApproved == "APPROVED") {
            approvedCount++
        }
    }
    
    val approvalRate = if (totalShifts > 0) (approvedCount.toDouble() / totalShifts.toDouble()) * 100.0 else 0.0
    
    return PerformanceStats(
        totalShifts = totalShifts,
        totalHours = totalHours,
        overtimeHours = overtimeHours,
        approvalRate = approvalRate,
        basicRate = 25.0
    )
}

// ---------------------- SEEDED KPI DATA SEEDER ----------------------

fun getKpisForEmployee(name: String): List<KpiCompetency> {
    return when (name) {
        "Marcus Aurelius (HR Intern)" -> listOf(
            KpiCompetency("Audit Integrity", 4.0f, 4.2f, "Maintains clear records and ensures documentation complies with criteria."),
            KpiCompetency("Documentation", 4.3f, 4.1f, "Drafts clear, unambiguous HR compliance digests and registers."),
            KpiCompetency("Team Synergy", 4.2f, 4.0f, "Aligns well with directors and handles intern administrative syncs smoothly."),
            KpiCompetency("Task Delivery", 3.9f, 4.2f, "Speed of execution on requested roster audits and reports."),
            KpiCompetency("Initiative", 4.5f, 4.3f, "Proactively suggests automated sheet filters and audit formats.")
        )
        "Robert Chen" -> listOf(
            KpiCompetency("Product Planning", 4.6f, 4.4f, "Maintains highly prioritized, organized backlog & roadmap features."),
            KpiCompetency("Delivery Velocity", 4.3f, 4.5f, "Facilitates rapid product releases and coordinates developer workflows."),
            KpiCompetency("Team Collaboration", 4.4f, 4.6f, "Drives clean cross-hub communication between Manila and Indore hubs."),
            KpiCompetency("Customer Feedback", 4.2f, 4.3f, "Incorporates actual client feature requests into product deliverables."),
            KpiCompetency("Product Vision", 4.7f, 4.5f, "Designs futuristic, high-integrity workflows matching industrial compliance.")
        )
        "Anjali Sharma" -> listOf(
            KpiCompetency("Operational Leadership", 4.8f, 4.9f, "Leads and delegates complex shift-roster management efficiently."),
            KpiCompetency("Strategic Vision", 4.9f, 4.8f, "Architects future business policies and guides technical hubs."),
            KpiCompetency("Budget Optimization", 4.5f, 4.7f, "Optimizes salary allocations, bonuses, and operational margins."),
            KpiCompetency("Governance & SLA", 4.6f, 4.6f, "Enforces strict regional compliance across hybrid offices."),
            KpiCompetency("Stakeholder Trust", 4.8f, 4.9f, "Builds high trust and confidence with directorship.")
        )
        "Aditya Joshi (Director)" -> listOf(
            KpiCompetency("Corporate Direction", 4.9f, 4.9f, "Shapes high-level operational policies and sets enterprise directives."),
            KpiCompetency("Hub Compliance", 4.7f, 4.8f, "Successfully integrates geofencing, biometrics, and secure databases."),
            KpiCompetency("Resource Stewardship", 4.8f, 4.7f, "Aligns financial planning perfectly with development targets."),
            KpiCompetency("Executive Decisioning", 4.8f, 4.8f, "Resolves critical compliance, roster, and contract disputes."),
            KpiCompetency("Enterprise Auditing", 4.9f, 4.9f, "Ensures total transparency and bulletproof security logs.")
        )
        else -> listOf( // Sarah Jenkins
            KpiCompetency("Technical Execution", 4.5f, 4.8f, "Kotlin development speed, accuracy, and modular class structure."),
            KpiCompetency("Integrity & Standards", 4.7f, 4.9f, "Produces highly secure, bug-free, and well-linted codebases."),
            KpiCompetency("Cooperative Alignment", 4.2f, 4.5f, "Participates actively in sync meetings and helps debug team hurdles."),
            KpiCompetency("Reliability & Delivery", 4.6f, 4.7f, "Delivers sprint deliverables on time and manages local branches."),
            KpiCompetency("Technical Initiative", 4.3f, 4.6f, "Proactively implements advanced features like gooey buttons and Canvas drawings.")
        )
    }
}

fun getHistoricalScoresForEmployee(name: String): List<HistoricalScore> {
    return when (name) {
        "Marcus Aurelius (HR Intern)" -> listOf(
            HistoricalScore("Q1 2025", 3.80f),
            HistoricalScore("Q2 2025", 3.90f),
            HistoricalScore("Q3 2025", 4.00f),
            HistoricalScore("Q4 2025", 4.10f),
            HistoricalScore("Q1 2026", 4.20f)
        )
        "Robert Chen" -> listOf(
            HistoricalScore("Q1 2025", 4.30f),
            HistoricalScore("Q2 2025", 4.40f),
            HistoricalScore("Q3 2025", 4.40f),
            HistoricalScore("Q4 2025", 4.50f),
            HistoricalScore("Q1 2026", 4.50f)
        )
        "Anjali Sharma" -> listOf(
            HistoricalScore("Q1 2025", 4.60f),
            HistoricalScore("Q2 2025", 4.70f),
            HistoricalScore("Q3 2025", 4.80f),
            HistoricalScore("Q4 2025", 4.80f),
            HistoricalScore("Q1 2026", 4.80f)
        )
        "Aditya Joshi (Director)" -> listOf(
            HistoricalScore("Q1 2025", 4.70f),
            HistoricalScore("Q2 2025", 4.80f),
            HistoricalScore("Q3 2025", 4.80f),
            HistoricalScore("Q4 2025", 4.90f),
            HistoricalScore("Q1 2026", 4.90f)
        )
        else -> listOf( // Sarah Jenkins
            HistoricalScore("Q1 2025", 4.20f),
            HistoricalScore("Q2 2025", 4.40f),
            HistoricalScore("Q3 2025", 4.50f),
            HistoricalScore("Q4 2025", 4.60f),
            HistoricalScore("Q1 2026", 4.70f)
        )
    }
}

// ---------------------- NATIVE ANDROID PDF GENERATION ENGINE ----------------------

fun generateIndividualPerformancePdf(
    context: Context,
    employee: EmployeeProfile,
    kpis: List<KpiCompetency>,
    stats: PerformanceStats,
    historicalScores: List<HistoricalScore>,
    overallScore: Float
): File? {
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

        val whiteTitlePaint = Paint().apply { color = android.graphics.Color.WHITE; textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val graySubtitlePaint = Paint().apply { color = android.graphics.Color.parseColor("#A0AEC0"); textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
        val mintMetadataPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676"); textSize = 8.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        // Helper to draw dual band header
        val drawHeader = { canvas: android.graphics.Canvas, title: String, subtitle: String ->
            // Header block
            canvas.drawRect(0f, 0f, 595f, 120f, blackPaint)
            // Accent bar
            canvas.drawRect(0f, 120f, 595f, 126f, electricMintPaint)
            // Texts
            canvas.drawText("SHIFT HR CORP", 54f, 65f, whiteTitlePaint)
            canvas.drawText(title, 54f, 90f, graySubtitlePaint)
            canvas.drawText(subtitle, 54f, 108f, mintMetadataPaint)
        }

        // Helper to draw footers
        val drawFooter = { canvas: android.graphics.Canvas, pageNum: Int ->
            canvas.drawLine(30f, 795f, 565f, 795f, strokePaint)
            textPaint.color = android.graphics.Color.GRAY
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Shift HR Corp Performance & Audit System  •  CONFIDENTIAL  •  Page $pageNum of 2", 130f, 812f, textPaint)
        }

        // Page 1: Metadata, overall score, and KPI breakdown table
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas

        // Background
        canvas1.drawRect(0f, 0f, 595f, 842f, bgPaint)
        drawHeader(canvas1, "OFFICIAL PERFORMANCE EVALUATION REPORT", "CONFIDENTIAL DOSSIER")

        // 3. Metadata Block (Two columns, inside a beautifully styled card)
        canvas1.drawRoundRect(RectF(30f, 142f, 565f, 222f), 12f, 12f, cardFillPaint)
        canvas1.drawRoundRect(RectF(30f, 142f, 565f, 222f), 12f, 12f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#1A202C")
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        val rowY1 = 165f
        val rowY2 = 185f
        val rowY3 = 205f
        
        canvas1.drawText("EMPLOYEE NAME:", 45f, rowY1, textPaint)
        canvas1.drawText("EMPLOYEE ID:", 45f, rowY2, textPaint)
        canvas1.drawText("DEPARTMENT:", 45f, rowY3, textPaint)

        canvas1.drawText("ROLE POSITION:", 300f, rowY1, textPaint)
        canvas1.drawText("EVALUATION DATE:", 300f, rowY2, textPaint)
        canvas1.drawText("EMPLOYEE STATUS:", 300f, rowY3, textPaint)

        // Metadata values
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText(employee.name, 145f, rowY1, textPaint)
        canvas1.drawText(employee.id, 145f, rowY2, textPaint)
        canvas1.drawText(employee.department, 145f, rowY3, textPaint)

        canvas1.drawText(employee.position, 410f, rowY1, textPaint)
        canvas1.drawText(SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date()), 410f, rowY2, textPaint)
        canvas1.drawText(employee.status, 410f, rowY3, textPaint)

        // 4. Large Score Callout Box
        canvas1.drawRoundRect(RectF(30f, 238f, 565f, 304f), 12f, 12f, heroCalloutPaint)
        canvas1.drawRoundRect(RectF(30f, 238f, 565f, 304f), 12f, 12f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#008543")
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("OVERALL APPRAISAL RATING", 48f, 262f, textPaint)

        val scoreSubTextPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1A202C")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas1.drawText("Securely compiled based on automated self-assessments and supervisor reviews.", 48f, 282f, scoreSubTextPaint)

        val ratingTextPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#008543")
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val appraisalScoreStr = String.format("%.2f", overallScore)
        canvas1.drawText(appraisalScoreStr, 440f, 282f, ratingTextPaint)

        val ratingLabelPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#4A5568")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas1.drawText(" / 5.0", 508f, 278f, ratingLabelPaint)

        // 5. KPI Table Section Title
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("Competency Performance Breakdown Matrix", 30f, 328f, textPaint)

        // Table Header
        canvas1.drawRect(30f, 342f, 565f, 360f, blackPaint)

        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("Competency Area", 40f, 354f, textPaint)
        canvas1.drawText("Self Score", 320f, 354f, textPaint)
        canvas1.drawText("Supervisor Score", 400f, 354f, textPaint)
        canvas1.drawText("Variance", 500f, 354f, textPaint)

        // Rows
        var currentY = 360f
        kpis.forEachIndexed { i, kpi ->
            val isEven = i % 2 == 1
            if (isEven) {
                canvas1.drawRect(30f, currentY, 565f, currentY + 45f, cardFillPaint)
            }
            canvas1.drawLine(30f, currentY + 45f, 565f, currentY + 45f, strokePaint)

            textPaint.color = android.graphics.Color.parseColor("#1A202C")
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 8.5f
            canvas1.drawText(kpi.competency, 40f, currentY + 18f, textPaint)

            textPaint.color = android.graphics.Color.parseColor("#4A5568")
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 7f
            canvas1.drawText(kpi.description, 40f, currentY + 32f, textPaint)

            textPaint.color = android.graphics.Color.parseColor("#1A202C")
            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas1.drawText(String.format("%.1f", kpi.selfScore), 340f, currentY + 24f, textPaint)
            canvas1.drawText(String.format("%.1f", kpi.managerScore), 420f, currentY + 24f, textPaint)

            // Variance color coding
            val variance = kpi.managerScore - kpi.selfScore
            val varStr = if (variance >= 0) "+${String.format("%.1f", variance)}" else String.format("%.1f", variance)
            val varColor = if (variance >= 0) "#008543" else "#DC2626"
            
            val varPaint = Paint().apply {
                color = android.graphics.Color.parseColor(varColor)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas1.drawText(varStr, 510f, currentY + 24f, varPaint)

            currentY += 45f
        }

        drawFooter(canvas1, 1)
        pdfDocument.finishPage(page1)

        // ------------------ Page 2: Historical charts, database metrics, signatures ------------------
        val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = pdfDocument.startPage(pageInfo2)
        val canvas2 = page2.canvas

        // Background & Header
        canvas2.drawRect(0f, 0f, 595f, 842f, bgPaint)
        drawHeader(canvas2, "INDIVIDUAL APPRAISAL AUDIT", "CONFIDENTIAL DOSSIER")

        // 1. Chart Section Title
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("Historical Evaluation Quarter-on-Quarter Progress", 30f, 150f, textPaint)

        // Rounded Chart container
        canvas2.drawRoundRect(RectF(30f, 162f, 565f, 282f), 12f, 12f, cardFillPaint)
        canvas2.drawRoundRect(RectF(30f, 162f, 565f, 282f), 12f, 12f, strokePaint)

        // Draw grid lines and labels
        for (i in 0..4) {
            val scoreVal = 3.0f + i * 0.5f
            val y = 265f - (scoreVal - 3.0f) / 2.0f * 85f
            canvas2.drawLine(55f, y, 540f, y, gridPaint)

            textPaint.color = android.graphics.Color.parseColor("#718096")
            textPaint.textSize = 7f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas2.drawText(String.format("%.1f", scoreVal), 36f, y + 2.5f, textPaint)
        }

        // Plot line
        var prevX = 0f
        var prevY = 0f
        
        historicalScores.forEachIndexed { idx, record ->
            val x = 55f + (idx.toFloat() / (historicalScores.size - 1).toFloat()) * 465f
            val scoreNorm = (record.score - 3.0f) / 2.0f
            val y = 265f - scoreNorm * 85f

            if (idx > 0) {
                canvas2.drawLine(prevX, prevY, x, y, linePaint)
            }

            // Draw Node background & border
            canvas2.drawCircle(x, y, 4.5f, whitePaint)
            canvas2.drawCircle(x, y, 4.5f, nodeBorderPaint)

            // Draw Labels
            textPaint.color = android.graphics.Color.parseColor("#111111")
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas2.drawText(String.format("%.2f", record.score), x - 10f, y - 7f, textPaint)

            textPaint.color = android.graphics.Color.parseColor("#4A5568")
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas2.drawText(record.quarter, x - 15f, 274f, textPaint)

            prevX = x
            prevY = y
        }

        // 2. Database attendance stats
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("Dynamic SQLite Attendance Verification", 30f, 305f, textPaint)

        // Stats card box
        canvas2.drawRoundRect(RectF(30f, 318f, 565f, 418f), 12f, 12f, cardFillPaint)
        canvas2.drawRoundRect(RectF(30f, 318f, 565f, 418f), 12f, 12f, strokePaint)

        // Left Column: Roster stats
        textPaint.color = android.graphics.Color.parseColor("#4A5568")
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("TOTAL CLOCKED SHIFTS:", 50f, 345f, textPaint)
        canvas2.drawText("TOTAL WORKED HOURS:", 50f, 365f, textPaint)
        canvas2.drawText("OVERTIME HOURS (1.5x):", 50f, 385f, textPaint)

        // Right Column: Roster stats values
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas2.drawText("${stats.totalShifts} Roster Punches", 200f, 345f, textPaint)
        canvas2.drawText(String.format("%.2f Total hours", stats.totalHours), 200f, 365f, textPaint)
        canvas2.drawText(String.format("%.2f Overtime hours", stats.overtimeHours), 200f, 385f, textPaint)

        // Middle Divider
        canvas2.drawLine(340f, 332f, 340f, 404f, strokePaint)

        // Second column
        textPaint.color = android.graphics.Color.parseColor("#4A5568")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("HOURLY COMP RATE:", 360f, 345f, textPaint)
        canvas2.drawText("PUNCH APPROVAL RATE:", 360f, 365f, textPaint)
        canvas2.drawText("TIMESHEET STATUS:", 360f, 385f, textPaint)

        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas2.drawText(String.format("$%.2f / hr", stats.basicRate), 500f, 345f, textPaint)
        canvas2.drawText(String.format("%.1f%% approved", stats.approvalRate), 500f, 365f, textPaint)
        
        val auditPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#008543")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas2.drawText("AUDITED OK 🟢", 500f, 385f, auditPaint)

        // 3. Physical signatures block
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("Physical Document Attestation", 30f, 465f, textPaint)

        textPaint.color = android.graphics.Color.parseColor("#4A5568")
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas2.drawText("By signing physically below, both employee and assessing director attest that the KPI and metrics", 30f, 485f, textPaint)
        canvas2.drawText("listed above are evaluated fairly in compliance with ClauseOS guidelines and standard HR procedure.", 30f, 498f, textPaint)

        // Signature lines
        canvas2.drawLine(50f, 620f, 250f, 620f, strokePaint) // Employee Line
        canvas2.drawLine(340f, 620f, 540f, 620f, strokePaint) // Manager Line

        textPaint.color = android.graphics.Color.parseColor("#1A202C")
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText(employee.name, 50f, 638f, textPaint)
        canvas2.drawText("Aditya Joshi (Director)", 340f, 638f, textPaint)

        textPaint.color = android.graphics.Color.parseColor("#718096")
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas2.drawText("Employee Signature & Date", 50f, 652f, textPaint)
        canvas2.drawText("Assessing Manager Signature & Date", 340f, 652f, textPaint)

        drawFooter(canvas2, 2)
        pdfDocument.finishPage(page2)

        // Save PDF file to cache dir
        val file = File(context.cacheDir, "Individual_Report_${employee.name.replace(" ", "_")}.pdf")
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

fun generateCompanySummaryPdf(
    context: Context,
    stats: PerformanceStats,
    deptTargets: List<DepartmentTarget>
): File? {
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
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        
        val textPaint = Paint().apply { color = android.graphics.Color.parseColor("#1A202C"); isAntiAlias = true }

        val whiteTitlePaint = Paint().apply { color = android.graphics.Color.WHITE; textSize = 15f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val graySubtitlePaint = Paint().apply { color = android.graphics.Color.parseColor("#A0AEC0"); textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
        val mintMetadataPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676"); textSize = 8.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        // Page 1 of Executive summary
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // 1. Dual band header
        canvas.drawRect(0f, 0f, 595f, 120f, blackPaint)
        canvas.drawRect(0f, 120f, 595f, 126f, electricMintPaint)
        canvas.drawText("SHIFT HR CORP", 54f, 65f, whiteTitlePaint)
        canvas.drawText("ORGANIZATIONAL PERFORMANCE EXECUTIVE SUMMARY", 54f, 90f, graySubtitlePaint)
        canvas.drawText("HR AUDIT CORE MATRIX", 54f, 108f, mintMetadataPaint)

        // 3. Key Metrics Grid Boxes (3 boxes side by side)
        val boxWidth = 162f
        val boxHeight = 65f
        val gap = 12f
        
        val metricBoxes = listOf(
            Triple("Evaluated Staff", "5 Employees", cardFillPaint),
            Triple("Company Avg", "4.61 / 5.0", heroCalloutPaint),
            Triple("Top Department", "Administration", cardFillPaint)
        )

        metricBoxes.forEachIndexed { idx, item ->
            val left = 30f + idx * (boxWidth + gap)
            val rect = RectF(left, 142f, left + boxWidth, 142f + boxHeight)
            
            canvas.drawRoundRect(rect, 10f, 10f, item.third)
            canvas.drawRoundRect(rect, 10f, 10f, strokePaint)

            textPaint.color = android.graphics.Color.parseColor("#4A5568")
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(item.first.uppercase(), left + 14f, 164f, textPaint)

            val isGreen = item.third == heroCalloutPaint
            textPaint.color = if (isGreen) android.graphics.Color.parseColor("#008543") else android.graphics.Color.parseColor("#111111")
            textPaint.textSize = 12f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(item.second, left + 14f, 188f, textPaint)
        }

        // 4. Department target comparison table
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Departmental Roster & Metric Benchmarks", 30f, 232f, textPaint)

        // Table Header
        canvas.drawRect(30f, 245f, 565f, 263f, blackPaint)

        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Department Name", 40f, 257f, textPaint)
        canvas.drawText("Target Score", 250f, 257f, textPaint)
        canvas.drawText("Actual Score", 350f, 257f, textPaint)
        canvas.drawText("Milestone Status", 450f, 257f, textPaint)

        // Rows
        var currentY = 263f
        deptTargets.forEachIndexed { idx, dept ->
            val isEven = idx % 2 == 1
            if (isEven) {
                canvas.drawRect(30f, currentY, 565f, currentY + 30f, cardFillPaint)
            }
            canvas.drawLine(30f, currentY + 30f, 565f, currentY + 30f, strokePaint)

            textPaint.color = android.graphics.Color.parseColor("#111111")
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 8f
            canvas.drawText(dept.department, 40f, currentY + 18f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(String.format("%.2f", dept.target), 260f, currentY + 18f, textPaint)
            
            textPaint.color = android.graphics.Color.parseColor("#008543")
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(String.format("%.2f", dept.actual), 360f, currentY + 18f, textPaint)

            // Status Badge
            val isMet = dept.actual >= dept.target
            val statusColor = if (isMet) "#008543" else "#D97706"
            textPaint.color = android.graphics.Color.parseColor(statusColor)
            canvas.drawText(dept.status.uppercase(), 460f, currentY + 18f, textPaint)

            currentY += 30f
        }

        // 5. Normal curve Bell Curve drawing on PDF
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Whole Company Performance Normal Curve Distribution", 30f, 470f, textPaint)

        // Draw rounded container for chart
        canvas.drawRoundRect(RectF(30f, 484f, 565f, 624f), 12f, 12f, cardFillPaint)
        canvas.drawRoundRect(RectF(30f, 484f, 565f, 624f), 12f, 12f, strokePaint)

        // Baseline grid line
        canvas.drawLine(40f, 610f, 550f, 610f, gridPaint)

        // Draw normal curve bell lines
        val mu = 4.2f
        val sigma = 0.4f
        val minScore = 3.0f
        val maxScore = 5.0f
        val pointsCount = 100
        
        var prevX = 0f
        var prevY = 0f
        
        for (i in 0..pointsCount) {
            val score = minScore + (i.toFloat() / pointsCount) * (maxScore - minScore)
            val exponent = -((score - mu).pow(2)) / (2 * sigma.pow(2))
            val freqNorm = exp(exponent) // value between 0.0 and 1.0
            
            val x = 50f + (i.toFloat() / pointsCount) * 495f
            val y = 605f - freqNorm * 100f

            if (i > 0) {
                canvas.drawLine(prevX, prevY, x, y, linePaint)
            }
            prevX = x
            prevY = y
        }

        // Draw Mean line indicator
        val muX = 50f + ((mu - minScore) / (maxScore - minScore)) * 495f
        canvas.drawLine(muX, 500f, muX, 610f, gridPaint)

        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Mean μ = 4.2", muX - 25f, 495f, textPaint)

        // Score tick ticks
        val ticks = listOf(3.0f, 3.5f, 4.0f, 4.5f, 5.0f)
        ticks.forEach { tick ->
            val tickX = 50f + ((tick - minScore) / (maxScore - minScore)) * 495f
            textPaint.color = android.graphics.Color.parseColor("#718096")
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(String.format("%.1f", tick), tickX - 8f, 618f, textPaint)
        }

        // 6. Signatures and execution audits
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Executive Directorship Attestation", 30f, 678f, textPaint)

        textPaint.color = android.graphics.Color.parseColor("#4A5568")
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Report calculated and compiled securely under Shift HR Corp development roster rules.", 30f, 695f, textPaint)

        canvas.drawLine(340f, 750f, 540f, 750f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Aditya Joshi (Director of Operations)", 340f, 765f, textPaint)

        textPaint.color = android.graphics.Color.parseColor("#718096")
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Executive Director Attestation Signature & Date", 340f, 777f, textPaint)

        // Footer block
        canvas.drawLine(30f, 795f, 565f, 795f, strokePaint)
        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 7.5f
        canvas.drawText("Organizational Audit Report — SHIFT HR CORP Confidential  •  Page 1 of 1", 145f, 812f, textPaint)

        pdfDocument.finishPage(page)

        // Save PDF file to cache dir
        val file = File(context.cacheDir, "Company_Wide_Audit_Report.pdf")
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

// Helper function to open or print PDF file in standard Android view/share sheets
fun openPdfFile(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        // Wrap in chooser to let them save, print, drive, or open with PDF viewer
        val chooserIntent = Intent.createChooser(intent, "Open or Print Performance PDF Dossier")
        chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "No PDF viewer available. File was printed securely to cache directory.", Toast.LENGTH_LONG).show()
    }
}

fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float, width: Float, paint: Paint, lineHeight: Float): Float {
    var currentY = y
    val words = text.split(" ")
    var line = StringBuilder()
    for (word in words) {
        val testLine = if (line.isEmpty()) word else "${line} $word"
        val testLineWidth = paint.measureText(testLine)
        if (testLineWidth > width) {
            canvas.drawText(line.toString(), x, currentY, paint)
            currentY += lineHeight
            line = StringBuilder(word)
        } else {
            line.append(if (line.isEmpty()) "" else " ").append(word)
        }
    }
    if (line.isNotEmpty()) {
        canvas.drawText(line.toString(), x, currentY, paint)
        currentY += lineHeight
    }
    return currentY
}

fun generateAiAnalysisPdf(
    context: Context,
    periodType: String,
    metricsList: List<ComparativeMetrics>,
    aiReportText: String
): File? {
    try {
        val pdfDocument = PdfDocument()

        // Common Paints
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#FBFBFD"); style = Paint.Style.FILL }
        val blackPaint = Paint().apply { color = android.graphics.Color.parseColor("#111111"); style = Paint.Style.FILL }
        val electricMintPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676"); style = Paint.Style.FILL }
        val cardFillPaint = Paint().apply { color = android.graphics.Color.parseColor("#F4F6F8"); style = Paint.Style.FILL }
        val heroCalloutPaint = Paint().apply { color = android.graphics.Color.parseColor("#E8F8F0"); style = Paint.Style.FILL }
        
        val strokePaint = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 1f }
        
        val textPaint = Paint().apply { color = android.graphics.Color.parseColor("#1A202C"); isAntiAlias = true }

        val whiteTitlePaint = Paint().apply { color = android.graphics.Color.WHITE; textSize = 15f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val graySubtitlePaint = Paint().apply { color = android.graphics.Color.parseColor("#A0AEC0"); textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
        val mintMetadataPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676"); textSize = 8.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // 1. Dual band header
        canvas.drawRect(0f, 0f, 595f, 120f, blackPaint)
        canvas.drawRect(0f, 120f, 595f, 126f, electricMintPaint)
        canvas.drawText("SHIFT HR CORP", 54f, 55f, whiteTitlePaint)
        canvas.drawText("AI-POWERED WORKFORCE DYNAMICS & EXECUTIVE REPORT", 54f, 80f, graySubtitlePaint)
        canvas.drawText("PERIOD: ${periodType.uppercase()} TREND ASSESSMENT  •  CONFIDENTIAL DOSSIER", 54f, 102f, mintMetadataPaint)

        // Date generated on right side of header
        val headerDatePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#A0AEC0")
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Generated: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()), 430f, 35f, headerDatePaint)

        // 2. Comparative Table Title
        var currentY = 155f
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Historical Dataset Comparison (${periodType.replaceFirstChar { it.uppercase() }})", 30f, currentY, textPaint)
        currentY += 15f

        // Draw Table Header
        canvas.drawRect(30f, currentY, 565f, currentY + 20f, blackPaint)

        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Period", 38f, currentY + 14f, textPaint)
        canvas.drawText("New Hires", 120f, currentY + 14f, textPaint)
        canvas.drawText("Separations", 200f, currentY + 14f, textPaint)
        canvas.drawText("Turnover Rate", 280f, currentY + 14f, textPaint)
        canvas.drawText("Promotions", 370f, currentY + 14f, textPaint)
        canvas.drawText("Job Transfers", 450f, currentY + 14f, textPaint)
        canvas.drawText("Avg Score", 515f, currentY + 14f, textPaint)
        currentY += 20f

        // Draw Table Rows
        metricsList.forEachIndexed { idx, row ->
            val isEven = idx % 2 == 1
            if (isEven) {
                canvas.drawRect(30f, currentY, 565f, currentY + 20f, cardFillPaint)
            }
            canvas.drawLine(30f, currentY + 20f, 565f, currentY + 20f, strokePaint)

            textPaint.color = android.graphics.Color.parseColor("#1A202C")
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 8f
            canvas.drawText(row.period, 38f, currentY + 14f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("${row.newHires}", 120f, currentY + 14f, textPaint)
            canvas.drawText("${row.separations}", 200f, currentY + 14f, textPaint)
            canvas.drawText(String.format("%.2f%%", row.turnoverRate), 280f, currentY + 14f, textPaint)
            canvas.drawText("${row.promotions}", 370f, currentY + 14f, textPaint)
            canvas.drawText("${row.jobTransfers}", 450f, currentY + 14f, textPaint)
            canvas.drawText(String.format("%.2f", row.avgScore), 515f, currentY + 14f, textPaint)

            currentY += 20f
        }

        currentY += 25f

        // 3. Draw AI Report Insights Block
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Interactive AI Analytical Recommendations", 30f, currentY, textPaint)
        currentY += 15f

        // Background box for AI Insights (Soft styled green card for consistency)
        val aiBoxHeight = 350f
        canvas.drawRoundRect(RectF(30f, currentY, 565f, currentY + aiBoxHeight), 12f, 12f, heroCalloutPaint)
        canvas.drawRoundRect(RectF(30f, currentY, 565f, currentY + aiBoxHeight), 12f, 12f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#004D26")
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        // Split report into lines and draw with wrap text
        var textY = currentY + 20f
        val cleanReportText = aiReportText.replace("\r", "").replace("**", "").replace("*", "")
        val paragraphs = cleanReportText.split("\n")
        
        paragraphs.forEach { paragraph ->
            if (paragraph.trim().isNotEmpty()) {
                textY = drawWrappedText(canvas, paragraph.trim(), 45f, textY, 490f, textPaint, 12.5f)
                textY += 5f // space between paragraphs
            }
        }

        // 4. Attestation & Signatures
        var sigY = currentY + aiBoxHeight + 25f
        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Administrative Audit Certification", 30f, sigY, textPaint)
        sigY += 14f

        textPaint.color = android.graphics.Color.parseColor("#4A5568")
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("This official intelligence summary leverages real-time enterprise databases and state-of-the-art secure AI frameworks.", 30f, sigY, textPaint)

        sigY += 45f
        canvas.drawLine(340f, sigY, 540f, sigY, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#111111")
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Shift HR Audit Operations Board", 340f, sigY + 14f, textPaint)

        textPaint.color = android.graphics.Color.parseColor("#718096")
        textPaint.textSize = 7.5f
        canvas.drawText("Authorized Administrator Signature Panel", 340f, sigY + 25f, textPaint)

        // Footer block
        canvas.drawLine(30f, 795f, 565f, 795f, strokePaint)
        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 7.5f
        canvas.drawText("Enterprise HR Intelligent Audit — SHIFT HR CORP Confidential  •  Page 1 of 1", 140f, 812f, textPaint)

        pdfDocument.finishPage(page)

        // Save PDF file to cache dir
        val file = File(context.cacheDir, "Workforce_AI_Analytics_Report.pdf")
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

