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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.viewmodel.*
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
        Text(
            text = "Performance Appraisal & Reporting",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppNeonGreen,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Tabs Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppCardGreyBg, RoundedCornerShape(10.dp))
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
                        .background(if (activeTab == key) AppNeonGreen else Color.Transparent)
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
                    userRole = userRole
                )
            }
            "organization" -> {
                if (!isHrOrAdmin) {
                    // RBAC SECURED BLOCKED LOCK CARD
                    RbacLockedView()
                } else {
                    OrganizationAuditSection(
                        context = context,
                        companyStats = companyStats,
                        departmentTargets = departmentTargets,
                        profiles = profiles
                    )
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
    userRole: String
) {
    if (selectedProfile == null) return

    var expandedDropdown by remember { mutableStateOf(false) }
    val overallRating = remember(kpis) {
        kpis.map { it.managerScore * it.weight }.sum()
    }

    // Dropdown / Selector Card for HR Admin
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = AppCardGreyBg),
        border = BorderStroke(1.dp, AppBorderGrey)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Target Review Subject",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f),
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
                            focusedBorderColor = AppNeonGreen,
                            unfocusedBorderColor = AppBorderGrey,
                            focusedTrailingIconColor = AppNeonGreen
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.background(AppCardGreyBg)
                    ) {
                        profiles.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile.name, color = Color.White, fontSize = 13.sp) },
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
                    color = AppNeonGreen,
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
                        tint = AppNeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = selectedProfile.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "🔐 Access Level: Employee Reviewee (Own Report Only)",
                            color = Color.White.copy(alpha = 0.5f),
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
        colors = CardDefaults.cardColors(containerColor = AppCardGreyBg),
        border = BorderStroke(1.dp, AppBorderGrey)
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
                        .background(AppNeonGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = selectedProfile.id,
                        color = AppNeonGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedProfile.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = selectedProfile.position,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Department: ${selectedProfile.department}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = "Status: ${selectedProfile.status}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Side: Beautiful Rating Gauge Callout
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.08f),
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
                        color = AppNeonGreen,
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
                        color = Color.White
                    )
                    Text(
                        text = "out of 5.0",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.5f)
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
        colors = CardDefaults.cardColors(containerColor = AppCardGreyBg),
        border = BorderStroke(1.dp, AppBorderGrey)
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
                    color = AppNeonGreen
                )
                Icon(
                    imageVector = Icons.Default.QueryStats,
                    contentDescription = "DB Stats",
                    tint = AppNeonGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "Calculated in real-time from active SQLite ledger logs.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = "${stats.totalShifts}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Total Shifts", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = String.format("%.1f hrs", stats.totalHours), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Hours Worked", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = String.format("%.1f hrs", stats.overtimeHours), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Overtime", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = String.format("%.0f%%", stats.approvalRate), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppNeonGreen)
                    Text(text = "Approve Rate", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // KPI breakdown table
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = AppCardGreyBg),
        border = BorderStroke(1.dp, AppBorderGrey)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Core Competencies & Score Card",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Comparison of employee self-evaluations against supervisor audits.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Table Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(vertical = 6.dp, horizontal = 8.dp)
            ) {
                Text(text = "Competency / KPI", modifier = Modifier.weight(1.8f), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = "Self", modifier = Modifier.weight(0.7f), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(text = "Mgr", modifier = Modifier.weight(0.7f), fontSize = 10.sp, color = AppNeonGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(text = "Var", modifier = Modifier.weight(0.7f), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }

            // Alternating Row KPI table
            kpis.forEachIndexed { index, kpi ->
                val variance = kpi.managerScore - kpi.selfScore
                val varianceColor = if (variance >= 0) AppNeonGreen else Color(0xFFFF5555)
                val varianceText = if (variance >= 0) "+${String.format("%.1f", variance)}" else String.format("%.1f", variance)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (index % 2 == 0) Color.Transparent else Color.White.copy(alpha = 0.02f))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.8f)) {
                        Text(text = kpi.competency, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = kpi.description, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                    Text(
                        text = String.format("%.1f", kpi.selfScore),
                        modifier = Modifier.weight(0.7f),
                        fontSize = 11.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = String.format("%.1f", kpi.managerScore),
                        modifier = Modifier.weight(0.7f),
                        fontSize = 11.sp,
                        color = AppNeonGreen,
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

    Spacer(modifier = Modifier.height(10.dp))

    // Interactive Historical Progress Chart
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = AppCardGreyBg),
        border = BorderStroke(1.dp, AppBorderGrey)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Historical Score Progress Trend",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Performance reviews overall rating over the past five quarters.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

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
                            color = Color.White.copy(alpha = 0.08f),
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
                            color = AppNeonGreen,
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
                                colors = listOf(AppNeonGreen.copy(alpha = 0.18f), Color.Transparent),
                                startY = 0f,
                                endY = h - marginY
                            )
                        )

                        // Draw points and labels
                        points.forEachIndexed { i, pt ->
                            drawCircle(
                                color = AppNeonGreen,
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
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
                                    color = android.graphics.Color.GREEN
                                    textSize = 8.dp.toPx()
                                    typeface = Typeface.DEFAULT_BOLD
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Action Row - PDF Download & Print Controls
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
                .height(48.dp)
                .testTag("download_individual_pdf_button"),
            colors = ButtonDefaults.buttonColors(containerColor = AppNeonGreen),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Icon", tint = Color.Black)
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
                .height(48.dp)
                .testTag("share_individual_pdf_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, AppBorderGrey)
        ) {
            Icon(Icons.Default.Share, contentDescription = "Share Icon", tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("View & Share PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OrganizationAuditSection(
    context: Context,
    companyStats: PerformanceStats,
    departmentTargets: List<DepartmentTarget>,
    profiles: List<EmployeeProfile>
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
                colors = CardDefaults.cardColors(containerColor = AppCardGreyBg),
                border = BorderStroke(1.dp, AppBorderGrey)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = AppNeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = title, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                    Text(text = valStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. Department targets vs performance table
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = AppCardGreyBg),
        border = BorderStroke(1.dp, AppBorderGrey)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Departmental Milestone Audits",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Comparison of current targets against database-aggregated results.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(vertical = 6.dp, horizontal = 8.dp)
            ) {
                Text(text = "Department", modifier = Modifier.weight(1.5f), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = "Target", modifier = Modifier.weight(0.8f), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(text = "Actual", modifier = Modifier.weight(0.8f), fontSize = 10.sp, color = AppNeonGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(text = "Audit", modifier = Modifier.weight(0.9f), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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
                    Text(text = dept.department, modifier = Modifier.weight(1.5f), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = String.format("%.2f", dept.target), modifier = Modifier.weight(0.8f), fontSize = 11.sp, color = Color.White, textAlign = TextAlign.Center)
                    Text(text = String.format("%.2f", dept.actual), modifier = Modifier.weight(0.8f), fontSize = 11.sp, color = AppNeonGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    
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
        colors = CardDefaults.cardColors(containerColor = AppCardGreyBg),
        border = BorderStroke(1.dp, AppBorderGrey)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Performance Distribution (Normal Bell Curve)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Visualization of staff count mapped across overall score frequencies.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Canvas drawing for bell curve normal distribution
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(vertical = 8.dp)
            ) {
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
                        color = Color.White.copy(alpha = 0.2f),
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
                            colors = listOf(AppNeonGreen.copy(alpha = 0.25f), Color.Transparent),
                            startY = h - marginY - graphH,
                            endY = h - marginY
                        )
                    )

                    // Draw the curve line
                    drawPath(
                        path = path,
                        color = AppNeonGreen,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Mark Mean (mu) indicator
                    val muX = marginX + ((mu - minScore) / (maxScore - minScore)) * graphW
                    drawLine(
                        color = AppNeonGreen.copy(alpha = 0.6f),
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
                            color = android.graphics.Color.GREEN
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
        colors = CardDefaults.cardColors(containerColor = AppCardGreyBg),
        border = BorderStroke(1.dp, AppBorderGrey)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Elite Performance Leaders",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
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
                        Text(text = leader.first, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Text(text = leader.second, fontSize = 11.sp, color = AppNeonGreen, fontWeight = FontWeight.Bold)
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
        colors = ButtonDefaults.buttonColors(containerColor = AppNeonGreen),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Default.Print, contentDescription = "Print Executive Report", tint = Color.Black)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Print Org Executive Summary PDF", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RbacLockedView() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(280.dp),
        colors = CardDefaults.cardColors(containerColor = AppCardGreyBg.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, AppBorderGrey)
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
                color = Color.White.copy(alpha = 0.6f),
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
                Icon(Icons.Default.Security, contentDescription = null, tint = AppNeonGreen, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Policy compliance active for Sarah Jenkins",
                    fontSize = 9.sp,
                    color = AppNeonGreen,
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
        val paint = Paint()
        val textPaint = Paint()

        // Page 1: Metadata, overall score, and KPI breakdown table
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas

        // 1. Draw header background bar
        paint.color = android.graphics.Color.parseColor("#0F0F11") // Dark grey
        canvas1.drawRect(0f, 0f, 595f, 100f, paint)

        // Accent lime line
        paint.color = android.graphics.Color.parseColor("#00FF88") // Neon green accent
        canvas1.drawRect(0f, 96f, 595f, 100f, paint)

        // 2. Draw Company name and title
        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 20f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("SHIFT HR CORP", 30f, 45f, textPaint)

        textPaint.color = android.graphics.Color.LTGRAY
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("OFFICIAL PERFORMANCE EVALUATION REPORT", 30f, 65f, textPaint)

        textPaint.color = android.graphics.Color.parseColor("#00FF88")
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas1.drawText("CONFIDENTIAL dossier", 30f, 82f, textPaint)

        // 3. Metadata Block (Two columns)
        paint.color = android.graphics.Color.parseColor("#F1F1F3") // Very light grey background for block
        val metaRect = RectF(30f, 120f, 565f, 210f)
        canvas1.drawRoundRect(metaRect, 8f, 8f, paint)

        textPaint.color = android.graphics.Color.parseColor("#333333")
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        // Metadata text columns
        val rowY1 = 145f
        val rowY2 = 165f
        val rowY3 = 185f
        
        canvas1.drawText("EMPLOYEE NAME:", 45f, rowY1, textPaint)
        canvas1.drawText("EMPLOYEE ID:", 45f, rowY2, textPaint)
        canvas1.drawText("DEPARTMENT:", 45f, rowY3, textPaint)

        canvas1.drawText("ROLE POSITION:", 300f, rowY1, textPaint)
        canvas1.drawText("EVALUATION DATE:", 300f, rowY2, textPaint)
        canvas1.drawText("EMPLOYEE STATUS:", 300f, rowY3, textPaint)

        // Metadata values
        textPaint.color = android.graphics.Color.BLACK
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText(employee.name, 140f, rowY1, textPaint)
        canvas1.drawText(employee.id, 140f, rowY2, textPaint)
        canvas1.drawText(employee.department, 140f, rowY3, textPaint)

        canvas1.drawText(employee.position, 400f, rowY1, textPaint)
        canvas1.drawText(SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date()), 400f, rowY2, textPaint)
        canvas1.drawText(employee.status, 400f, rowY3, textPaint)

        // 4. Large Score Callout Box
        paint.color = android.graphics.Color.parseColor("#E6FFE6") // Soft light green
        val scoreRect = RectF(30f, 230f, 565f, 300f)
        canvas1.drawRoundRect(scoreRect, 8f, 8f, paint)

        textPaint.color = android.graphics.Color.parseColor("#006633")
        textPaint.textSize = 12f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("OVERALL APPRAISAL RATING:", 50f, 270f, textPaint)

        textPaint.textSize = 30f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText(String.format("%.2f", overallScore), 350f, 275f, textPaint)
        
        textPaint.textSize = 14f
        textPaint.color = android.graphics.Color.GRAY
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("/ 5.0", 420f, 272f, textPaint)

        // 5. KPI Table Section Title
        textPaint.color = android.graphics.Color.BLACK
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("Competency Performance Breakdown Table", 30f, 335f, textPaint)

        // Table Header
        paint.color = android.graphics.Color.parseColor("#333333") // Dark Header background
        val headerRect = RectF(30f, 350f, 565f, 375f)
        canvas1.drawRect(headerRect, paint)

        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("Competency Area", 40f, 366f, textPaint)
        canvas1.drawText("Self Score", 320f, 366f, textPaint)
        canvas1.drawText("Supervisor Score", 400f, 366f, textPaint)
        canvas1.drawText("Variance", 500f, 366f, textPaint)

        // Rows
        var currentY = 375f
        kpis.forEachIndexed { i, kpi ->
            val isEven = i % 2 == 0
            paint.color = if (isEven) android.graphics.Color.parseColor("#F9F9FB") else android.graphics.Color.WHITE
            val rowRect = RectF(30f, currentY, 565f, currentY + 45f)
            canvas1.drawRect(rowRect, paint)

            textPaint.color = android.graphics.Color.BLACK
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 9f
            canvas1.drawText(kpi.competency, 40f, currentY + 20f, textPaint)

            textPaint.color = android.graphics.Color.GRAY
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 7.5f
            canvas1.drawText(kpi.description, 40f, currentY + 34f, textPaint)

            textPaint.color = android.graphics.Color.BLACK
            textPaint.textSize = 10f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas1.drawText(String.format("%.1f", kpi.selfScore), 340f, currentY + 26f, textPaint)
            canvas1.drawText(String.format("%.1f", kpi.managerScore), 420f, currentY + 26f, textPaint)

            // Variance color coding
            val variance = kpi.managerScore - kpi.selfScore
            if (variance >= 0) {
                textPaint.color = android.graphics.Color.parseColor("#008800")
                canvas1.drawText("+${String.format("%.1f", variance)}", 510f, currentY + 26f, textPaint)
            } else {
                textPaint.color = android.graphics.Color.RED
                canvas1.drawText(String.format("%.1f", variance), 510f, currentY + 26f, textPaint)
            }

            // Divider line
            paint.color = android.graphics.Color.parseColor("#EEEEEE")
            canvas1.drawLine(30f, currentY + 45f, 565f, currentY + 45f, paint)

            currentY += 45f
        }

        // Draw footer page number
        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("Page 1 of 2", 280f, 820f, textPaint)

        pdfDocument.finishPage(page1)

        // ------------------ Page 2: Historical charts, database metrics, signatures ------------------
        val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = pdfDocument.startPage(pageInfo2)
        val canvas2 = page2.canvas

        // Header strip
        paint.color = android.graphics.Color.parseColor("#0F0F11")
        canvas2.drawRect(0f, 0f, 595f, 40f, paint)
        paint.color = android.graphics.Color.parseColor("#00FF88")
        canvas2.drawRect(0f, 38f, 595f, 40f, paint)

        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("SHIFT HR CORP — INDIVIDUAL APPRAISAL AUDIT", 30f, 25f, textPaint)

        // 1. Chart Section Title
        textPaint.color = android.graphics.Color.BLACK
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("Historical Evaluation Quarter-on-Quarter Progress", 30f, 75f, textPaint)

        // Outline Chart Box
        paint.style = Paint.Style.STROKE
        paint.color = android.graphics.Color.LTGRAY
        paint.strokeWidth = 1f
        canvas2.drawRect(50f, 100f, 545f, 240f, paint)
        paint.style = Paint.Style.FILL

        // Draw grid lines
        paint.color = android.graphics.Color.parseColor("#F0F0F0")
        for (i in 0..4) {
            val scoreVal = 3.0f + i * 0.5f
            val y = 240f - (scoreVal - 3.0f) / 2.0f * 120f
            canvas2.drawLine(50f, y, 545f, y, paint)

            textPaint.color = android.graphics.Color.GRAY
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas2.drawText(String.format("%.1f", scoreVal), 26f, y + 3f, textPaint)
        }

        // Plot line
        paint.color = android.graphics.Color.parseColor("#00FF88") // Green line
        paint.strokeWidth = 3f
        var prevX = 0f
        var prevY = 0f
        
        historicalScores.forEachIndexed { idx, record ->
            val x = 50f + (idx.toFloat() / (historicalScores.size - 1).toFloat()) * 460f
            val scoreNorm = (record.score - 3.0f) / 2.0f
            val y = 240f - scoreNorm * 120f

            if (idx > 0) {
                canvas2.drawLine(prevX, prevY, x, y, paint)
            }

            // Draw Point dot
            val dotPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#006633")
                style = Paint.Style.FILL
            }
            canvas2.drawCircle(x, y, 4f, dotPaint)
            dotPaint.color = android.graphics.Color.WHITE
            canvas2.drawCircle(x, y, 2f, dotPaint)

            // Draw Labels
            textPaint.color = android.graphics.Color.BLACK
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas2.drawText(String.format("%.2f", record.score), x - 10f, y - 8f, textPaint)

            canvas2.drawText(record.quarter, x - 15f, 255f, textPaint)

            prevX = x
            prevY = y
        }

        // 2. Database attendance stats
        textPaint.color = android.graphics.Color.BLACK
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("Dynamic SQLite Attendance Verification", 30f, 305f, textPaint)

        // Stats card box
        paint.color = android.graphics.Color.parseColor("#F5F7FA")
        val statsBox = RectF(30f, 320f, 565f, 420f)
        canvas2.drawRoundRect(statsBox, 8f, 8f, paint)

        // Left Column: Roster stats
        textPaint.color = android.graphics.Color.parseColor("#555555")
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("TOTAL CLOCKED SHIFTS:", 50f, 350f, textPaint)
        canvas2.drawText("TOTAL WORKED HOURS:", 50f, 370f, textPaint)
        canvas2.drawText("OVERTIME HOURS (1.5x):", 50f, 390f, textPaint)

        // Right Column: Roster stats values
        textPaint.color = android.graphics.Color.BLACK
        textPaint.textSize = 9.5f
        canvas2.drawText("${stats.totalShifts} Roster Punches", 200f, 350f, textPaint)
        canvas2.drawText(String.format("%.2f Total hours", stats.totalHours), 200f, 370f, textPaint)
        canvas2.drawText(String.format("%.2f Overtime hours", stats.overtimeHours), 200f, 390f, textPaint)

        // Middle Divider
        paint.color = android.graphics.Color.LTGRAY
        canvas2.drawLine(340f, 335f, 340f, 405f, paint)

        // Second column
        textPaint.color = android.graphics.Color.parseColor("#555555")
        canvas2.drawText("HOURLY COMP RATE:", 360f, 350f, textPaint)
        canvas2.drawText("PUNCH APPROVAL RATE:", 360f, 370f, textPaint)
        canvas2.drawText("TIMESHEET STATUS:", 360f, 390f, textPaint)

        textPaint.color = android.graphics.Color.BLACK
        canvas2.drawText(String.format("$%.2f / hr", stats.basicRate), 500f, 350f, textPaint)
        canvas2.drawText(String.format("%.1f%% approved", stats.approvalRate), 500f, 370f, textPaint)
        
        textPaint.color = android.graphics.Color.parseColor("#008800")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("AUDITED OK 🟢", 500f, 390f, textPaint)

        // 3. Physical signatures block
        textPaint.color = android.graphics.Color.BLACK
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("Physical Document Attestation", 30f, 465f, textPaint)

        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas2.drawText("By signing physically below, both employee and assessing director attest that the KPI and metrics", 30f, 485f, textPaint)
        canvas2.drawText("listed above are evaluated fairly in compliance with ClauseOS guidelines and standard HR procedure.", 30f, 500f, textPaint)

        // Signature lines
        paint.color = android.graphics.Color.BLACK
        paint.strokeWidth = 1f
        canvas2.drawLine(50f, 620f, 250f, 620f, paint) // Employee Line
        canvas2.drawLine(340f, 620f, 540f, 620f, paint) // Manager Line

        textPaint.color = android.graphics.Color.BLACK
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText(employee.name, 50f, 638f, textPaint)
        canvas2.drawText("Aditya Joshi (Director)", 340f, 638f, textPaint)

        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas2.drawText("Employee Signature & Date", 50f, 652f, textPaint)
        canvas2.drawText("Assessing Manager Signature & Date", 340f, 652f, textPaint)

        // Footer block
        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 8f
        canvas2.drawText("Page 2 of 2", 280f, 820f, textPaint)

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
        val paint = Paint()
        val textPaint = Paint()

        // Page 1 of Executive summary
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // 1. Draw header background bar
        paint.color = android.graphics.Color.parseColor("#0F0F11") // Dark grey
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        // Accent lime line
        paint.color = android.graphics.Color.parseColor("#00FF88") // Neon green
        canvas.drawRect(0f, 96f, 595f, 100f, paint)

        // 2. Title block
        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 20f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SHIFT HR CORP", 30f, 45f, textPaint)

        textPaint.color = android.graphics.Color.LTGRAY
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("ORGANIZATIONAL PERFORMANCE EXECUTIVE SUMMARY", 30f, 65f, textPaint)

        textPaint.color = android.graphics.Color.parseColor("#00FF88")
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText("HR AUDIT CORE MATRIX", 30f, 82f, textPaint)

        // 3. Key Metrics Grid Boxes (3 boxes side by side)
        val boxWidth = 160f
        val boxHeight = 70f
        val gap = 15f
        
        val metricBoxes = listOf(
            Triple("Evaluated Staff", "5 Employees", "#F5F7FA"),
            Triple("Company Avg", "4.61 / 5.0", "#EBF3FF"),
            Triple("Top Department", "Administration", "#E6FFE6")
        )

        metricBoxes.forEachIndexed { idx, item ->
            val left = 30f + idx * (boxWidth + gap)
            val rect = RectF(left, 120f, left + boxWidth, 120f + boxHeight)
            
            paint.color = android.graphics.Color.parseColor(item.third)
            canvas.drawRoundRect(rect, 6f, 6f, paint)

            textPaint.color = android.graphics.Color.DKGRAY
            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(item.first, left + 15f, 145f, textPaint)

            textPaint.color = android.graphics.Color.BLACK
            textPaint.textSize = 14f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(item.second, left + 15f, 172f, textPaint)
        }

        // 4. Department target comparison table
        textPaint.color = android.graphics.Color.BLACK
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Departmental Roster & Metric Benchmarks", 30f, 230f, textPaint)

        // Table Header
        paint.color = android.graphics.Color.parseColor("#222222")
        val headerRect = RectF(30f, 245f, 565f, 270f)
        canvas.drawRect(headerRect, paint)

        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Department Name", 40f, 262f, textPaint)
        canvas.drawText("Target Score", 250f, 262f, textPaint)
        canvas.drawText("Actual Score", 350f, 262f, textPaint)
        canvas.drawText("Milestone Status", 450f, 262f, textPaint)

        // Rows
        var currentY = 270f
        deptTargets.forEachIndexed { idx, dept ->
            val isEven = idx % 2 == 0
            paint.color = if (isEven) android.graphics.Color.parseColor("#F9F9FB") else android.graphics.Color.WHITE
            val rowRect = RectF(30f, currentY, 565f, currentY + 30f)
            canvas.drawRect(rowRect, paint)

            textPaint.color = android.graphics.Color.BLACK
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(dept.department, 40f, currentY + 18f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(String.format("%.2f", dept.target), 260f, currentY + 18f, textPaint)
            
            textPaint.color = android.graphics.Color.parseColor("#008800")
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(String.format("%.2f", dept.actual), 360f, currentY + 18f, textPaint)

            // Status Badge
            val statusColor = if (dept.actual >= dept.target) "#006633" else "#886600"
            textPaint.color = android.graphics.Color.parseColor(statusColor)
            canvas.drawText(dept.status.uppercase(), 460f, currentY + 18f, textPaint)

            paint.color = android.graphics.Color.parseColor("#EEEEEE")
            canvas.drawLine(30f, currentY + 30f, 565f, currentY + 30f, paint)

            currentY += 30f
        }

        // 5. Normal curve Bell Curve drawing on PDF
        textPaint.color = android.graphics.Color.BLACK
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Whole Company Performance Normal Curve Distribution", 30f, 465f, textPaint)

        // Draw outer outline border
        paint.style = Paint.Style.STROKE
        paint.color = android.graphics.Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawRect(50f, 485f, 545f, 620f, paint)
        paint.style = Paint.Style.FILL

        // Baseline tick labels
        paint.color = android.graphics.Color.parseColor("#EAEAEA")
        canvas.drawLine(50f, 610f, 545f, 610f, paint)

        // Draw normal curve bell lines
        val mu = 4.2f
        val sigma = 0.4f
        val minScore = 3.0f
        val maxScore = 5.0f
        val pointsCount = 100
        
        var prevX = 0f
        var prevY = 0f
        
        paint.color = android.graphics.Color.parseColor("#006633") // Dark Green Curve
        paint.strokeWidth = 2.5f

        for (i in 0..pointsCount) {
            val score = minScore + (i.toFloat() / pointsCount) * (maxScore - minScore)
            val exponent = -((score - mu).pow(2)) / (2 * sigma.pow(2))
            val freqNorm = exp(exponent) // value between 0.0 and 1.0
            
            val x = 50f + (i.toFloat() / pointsCount) * 495f
            val y = 610f - freqNorm * 110f

            if (i > 0) {
                canvas.drawLine(prevX, prevY, x, y, paint)
            }
            prevX = x
            prevY = y
        }

        // Draw Mean line indicator
        paint.color = android.graphics.Color.GRAY
        paint.strokeWidth = 1f
        val muX = 50f + ((mu - minScore) / (maxScore - minScore)) * 495f
        canvas.drawLine(muX, 500f, muX, 610f, paint)

        textPaint.color = android.graphics.Color.BLACK
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Mean μ = 4.2", muX - 25f, 495f, textPaint)

        // Score tick ticks
        val ticks = listOf(3.0f, 3.5f, 4.0f, 4.5f, 5.0f)
        ticks.forEach { tick ->
            val tickX = 50f + ((tick - minScore) / (maxScore - minScore)) * 495f
            textPaint.color = android.graphics.Color.DKGRAY
            canvas.drawText(String.format("%.1f", tick), tickX - 8f, 634f, textPaint)
        }

        // 6. Signatures and execution audits
        textPaint.color = android.graphics.Color.BLACK
        textPaint.textSize = 12f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Executive Directorship Attestation", 30f, 680f, textPaint)

        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Report calculated and compiled securely under Shift HR Corp development roster rules.", 30f, 698f, textPaint)

        paint.color = android.graphics.Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawLine(340f, 750f, 540f, 750f, paint)

        textPaint.color = android.graphics.Color.BLACK
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Aditya Joshi (Director of Operations)", 340f, 768f, textPaint)

        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 8.5f
        canvas.drawText("Executive Director Attestation Signature & Date", 340f, 782f, textPaint)

        // Footer block
        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 8f
        canvas.drawText("Organizational Audit Report — SHIFT HR CORP Confidential", 180f, 820f, textPaint)

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
