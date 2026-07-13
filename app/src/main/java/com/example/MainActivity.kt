package com.example

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.asComposeRenderEffect
import kotlin.math.sin
import kotlin.math.cos
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.ShiftConfigEntity
import com.example.data.database.TimeLogEntity
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.*
import com.example.ui.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import android.os.Build
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TimeTrackerViewModel = viewModel()
            val themeName by viewModel.selectedTheme
            MyApplicationTheme(themeName = themeName) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime)
                ) { innerPadding ->
                    TimeTrackerApp(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun LiquidGlassBackground(
    themeName: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val theme = com.example.ui.theme.LiquidThemeRegistry.getThemeByName(themeName)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // 1. Fluid liquid backdrop gradient
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(theme.bgGradientStart, theme.bgGradientEnd)
                    )
                )
                // 2. Liquid glow blob 1 (top-left offset)
                drawCircle(
                    color = theme.bgBubble1.copy(alpha = 0.35f),
                    radius = size.width * 0.75f,
                    center = androidx.compose.ui.geometry.Offset(-size.width * 0.1f, size.height * 0.2f)
                )
                // 3. Liquid glow blob 2 (bottom-right offset)
                drawCircle(
                    color = theme.bgBubble2.copy(alpha = 0.3f),
                    radius = size.width * 0.65f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 1.1f, size.height * 0.75f)
                )
            }
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTrackerApp(
    modifier: Modifier = Modifier,
    viewModel: TimeTrackerViewModel = viewModel()
) {
    val context = LocalContext.current
    val allLogs by viewModel.allTimeLogs.collectAsStateWithLifecycle()
    val pendingLogs by viewModel.pendingTimeLogs.collectAsStateWithLifecycle()
    val activeLog by viewModel.activeTimeLog.collectAsStateWithLifecycle()
    val shiftConfig by viewModel.shiftConfig.collectAsStateWithLifecycle()

    val currentScreen by viewModel.currentScreen
    val isLoggedIn by viewModel.isLoggedIn
    val currentUserRole by viewModel.currentUserRole
    val currentUserName by viewModel.currentUserName
    val notifications by viewModel.notifications

    // Log selected for detail viewing
    var viewDetailsLog by remember { mutableStateOf<TimeLogEntity?>(null) }
    // Reason state for rejecting a log
    var rejectLogId by remember { mutableStateOf<Long?>(null) }
    var rejectionReasonText by remember { mutableStateOf("") }
    // Retroactive Edit log state
    var editLogState by remember { mutableStateOf<TimeLogEntity?>(null) }

    // Dialog trigger for reject
    if (rejectLogId != null) {
        AlertDialog(
            onDismissRequest = { rejectLogId = null },
            title = { Text("Audit Rejection Reason", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                OutlinedTextField(
                    value = rejectionReasonText,
                    onValueChange = { rejectionReasonText = it },
                    label = { Text("Rejection Comment") },
                    placeholder = { Text("Specify policy infraction (e.g., Break limit exceeded)") },
                    modifier = Modifier.fillMaxWidth().testTag("rejection_comment_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        rejectLogId?.let { id ->
                            viewModel.rejectLog(id, rejectionReasonText.ifBlank { "Unapproved duration or policy mismatch" })
                        }
                        rejectLogId = null
                        rejectionReasonText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reject_button")
                ) {
                    Text("Reject Punch", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectLogId = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Modal Dialog for Retroactive Punches (HR Log Editor)
    if (editLogState != null) {
        RetroactiveEditDialog(
            log = editLogState!!,
            onDismiss = { editLogState = null },
            onSave = { updated ->
                viewModel.updateTimeLogDetails(updated)
                editLogState = null
            }
        )
    }

    // Modal Dialog for viewing coordinates, telemetry, and approvals
    if (viewDetailsLog != null) {
        LogDetailDialog(
            log = viewDetailsLog!!,
            onDismiss = { viewDetailsLog = null },
            onEdit = {
                editLogState = it
                viewDetailsLog = null
            },
            currencySymbol = viewModel.getCurrencySymbol(),
            userRole = currentUserRole
        )
    }

    // Biometric Face Recognition Simulation Dialog
    val showFaceScanAction = viewModel.showFaceScannerForAction.value
    if (showFaceScanAction != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { viewModel.showFaceScannerForAction.value = null }
        ) {
            var scanProgress by remember { mutableStateOf(0f) }
            var isScanningFinished by remember { mutableStateOf(false) }

            // Tick up scanning progress simulation
            LaunchedEffect(showFaceScanAction) {
                scanProgress = 0f
                isScanningFinished = false
                while (scanProgress < 1f) {
                    delay(30)
                    scanProgress += 0.04f
                }
                scanProgress = 1f
                isScanningFinished = true
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)),
                border = BorderStroke(2.dp, if (viewModel.isFaceScanMismatched.value) Color(0xFFF43F5E) else Color(0xFF10B981))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BIOMETRIC FACE NODE MATCHING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (viewModel.isFaceScanMismatched.value) Color(0xFFF43F5E) else Color(0xFF10B981),
                            letterSpacing = 1.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (viewModel.isFaceScanMismatched.value) Color(0xFFF43F5E).copy(alpha = 0.15f)
                                    else Color(0xFF10B981).copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (viewModel.isFaceScanMismatched.value) "SECURITY SECURE" else "SECURE NODAL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.isFaceScanMismatched.value) Color(0xFFF43F5E) else Color(0xFF10B981),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Text(
                        text = "Authenticating identity for: ${viewModel.currentUserName.value}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Styled viewfinder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing simple canvas node lines and face grid
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            // Target Face Guide (Oval)
                            drawOval(
                                color = if (viewModel.isFaceScanMismatched.value) Color(0xFFF43F5E).copy(alpha = 0.4f) else Color(0xFF10B981).copy(alpha = 0.4f),
                                topLeft = Offset(w * 0.25f, h * 0.15f),
                                size = Size(w * 0.5f, h * 0.7f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 4f,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )

                            // Nodal face dots simulation
                            if (scanProgress > 0.1f) {
                                drawCircle(Color(0xFF38BDF8), 6f, Offset(w * 0.5f, h * 0.35f)) // nose
                                drawCircle(Color(0xFF38BDF8), 6f, Offset(w * 0.4f, h * 0.3f))  // left eye
                                drawCircle(Color(0xFF38BDF8), 6f, Offset(w * 0.6f, h * 0.3f))  // right eye
                                drawCircle(Color(0xFF38BDF8), 6f, Offset(w * 0.45f, h * 0.5f)) // left cheek
                                drawCircle(Color(0xFF38BDF8), 6f, Offset(w * 0.55f, h * 0.5f)) // right cheek
                                drawCircle(Color(0xFF38BDF8), 6f, Offset(w * 0.5f, h * 0.65f)) // chin
                            }

                            // Dynamic Laser Line
                            val laserY = h * scanProgress
                            drawLine(
                                color = if (viewModel.isFaceScanMismatched.value) Color(0xFFF43F5E) else Color(0xFFCCFF00),
                                start = Offset(0f, laserY),
                                end = Offset(w, laserY),
                                strokeWidth = 5f
                            )
                        }

                        // Progress counter
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isScanningFinished && !viewModel.isFaceScanMismatched.value) Icons.Default.CheckCircle
                                              else if (isScanningFinished && viewModel.isFaceScanMismatched.value) Icons.Default.Warning
                                              else Icons.Default.Face,
                                contentDescription = null,
                                tint = if (viewModel.isFaceScanMismatched.value) Color(0xFFF43F5E) else Color(0xFF10B981),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (isScanningFinished) {
                                    if (viewModel.isFaceScanMismatched.value) "BIOMETRIC MISMATCH" else "IDENTITY RECOGNIZED"
                                } else "BIOMETRIC VECTOR SCANNING...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${(scanProgress * 100).toInt()}% MATCH RATE",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (viewModel.isFaceScanMismatched.value) Color(0xFFF43F5E) else Color(0xFFCCFF00)
                            )
                        }
                    }

                    // Interactive verification selection
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SIMULATOR BIOMETRIC RESPONSE TARGET",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.isFaceScanMismatched.value = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!viewModel.isFaceScanMismatched.value) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (!viewModel.isFaceScanMismatched.value) Color(0xFF10B981) else Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("Correct Face", fontSize = 10.sp, color = Color.White)
                            }
                            Button(
                                onClick = { viewModel.isFaceScanMismatched.value = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (viewModel.isFaceScanMismatched.value) Color(0xFFF43F5E).copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (viewModel.isFaceScanMismatched.value) Color(0xFFF43F5E) else Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("Unknown Person", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.showFaceScannerForAction.value = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }

                        Button(
                            onClick = {
                                viewModel.handlePunchAfterFaceRecognition(showFaceScanAction)
                            },
                            enabled = isScanningFinished,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.isFaceScanMismatched.value) Color(0xFFF43F5E) else Color(0xFFCCFF00)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("confirm_face_biometrics_scan")
                        ) {
                            Text(
                                text = if (viewModel.isFaceScanMismatched.value) "Submit (Block)" else "Clock Secure",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }

    if (!isLoggedIn) {
        LoginScreen(
            themeName = viewModel.selectedTheme.value,
            onLoginAttempt = { user, pass ->
                viewModel.attemptLogin(user, pass)
            },
            onRegisterAttempt = { u, p, n, r, cName, cCode ->
                viewModel.registerUser(u, p, n, r, cName, cCode)
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LiquidGlassBackground(
                themeName = viewModel.selectedTheme.value,
                modifier = modifier
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (viewDetailsLog != null || editLogState != null) {
                                Modifier.blur(14.dp)
                            } else {
                                Modifier
                            }
                        )
                ) {
            // App top-bar (Aesthetic Cyber Header)
            HeaderBar(
                roleName = currentUserRole,
                displayName = currentUserName,
                isOffline = viewModel.isMockOffline.value,
                isSyncing = viewModel.isSyncing.value,
                onLogout = { viewModel.logout() },
                onSyncClick = { viewModel.performSync() },
                onProfileClick = {
                    viewModel.currentScreen.value = "self_service"
                    viewModel.selfServiceTab.value = "profile"
                }
            )

            // Alerts section
            NotificationsSection(
                notifications = notifications,
                onDismiss = { viewModel.dismissNotification(it) }
            )

            // Dynamic Department & Supervisor/Manager filtered logs
            val currentUserProfile = viewModel.employeeProfiles.value.find { it.name.equals(currentUserName, ignoreCase = true) }
            val userDepartment = currentUserProfile?.department ?: ""

            val filteredPendingLogs = pendingLogs.filter { log ->
                val empUser = viewModel.registeredUsers.value.find { it.name.equals(log.employeeName, ignoreCase = true) }
                val empRole = empUser?.role ?: "EMPLOYEE"
                val empProfile = viewModel.employeeProfiles.value.find { it.name.equals(log.employeeName, ignoreCase = true) }
                val empDept = empProfile?.department ?: ""
                
                if (currentUserRole == "ADMIN_HR") {
                    true
                } else if (currentUserRole == "MANAGER" || currentUserRole == "SUPERVISOR") {
                    empDept.equals(userDepartment, ignoreCase = true) && empRole != "SUPERVISOR" && empRole != "MANAGER"
                } else {
                    log.employeeName.equals(currentUserName, ignoreCase = true)
                }
            }

            val filteredAllLogs = allLogs.filter { log ->
                val empUser = viewModel.registeredUsers.value.find { it.name.equals(log.employeeName, ignoreCase = true) }
                val empRole = empUser?.role ?: "EMPLOYEE"
                val empProfile = viewModel.employeeProfiles.value.find { it.name.equals(log.employeeName, ignoreCase = true) }
                val empDept = empProfile?.department ?: ""
                
                if (currentUserRole == "ADMIN_HR") {
                    true
                } else if (currentUserRole == "MANAGER" || currentUserRole == "SUPERVISOR") {
                    empDept.equals(userDepartment, ignoreCase = true) && empRole != "SUPERVISOR" && empRole != "MANAGER"
                } else {
                    log.employeeName.equals(currentUserName, ignoreCase = true)
                }
            }

            // Content Screens switching
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (currentScreen) {
                    "clock" -> {
                        EmployeeClockScreen(
                            activeLog = activeLog,
                            shiftConfig = shiftConfig,
                            timerString = viewModel.activeTimerString.value,
                            breakTimerString = viewModel.breakTimerString.value,
                            onPunchAction = { viewModel.handlePunch(it) },
                            todayHoliday = viewModel.todayHoliday.value,
                            viewModel = viewModel
                        )
                    }
                    "spreadsheet" -> {
                        SpreadsheetScreen(
                            logs = filteredAllLogs,
                            shiftConfig = shiftConfig,
                            filterApproval = viewModel.filterApproval.value,
                            filterSync = viewModel.filterSync.value,
                            searchQuery = viewModel.searchQuery.value,
                            onFilterApprovalChange = { viewModel.filterApproval.value = it },
                            onFilterSyncChange = { viewModel.filterSync.value = it },
                            onSearchQueryChange = { viewModel.searchQuery.value = it },
                            onExportClick = { exportLogsToCSV(filteredAllLogs, context) },
                            onRowClick = { viewDetailsLog = it },
                            viewModel = viewModel,
                            currentUserRole = currentUserRole,
                            currentUserName = currentUserName
                        )
                    }
                    "chat" -> {
                        ChatHubScreen(
                            viewModel = viewModel
                        )
                    }
                    "hr_approval" -> {
                        AdminApprovalScreen(
                            pendingLogs = filteredPendingLogs,
                            allLogs = filteredAllLogs,
                            onApprove = { viewModel.approveLog(it) },
                            onReject = { rejectLogId = it },
                            onEdit = { editLogState = it },
                            onDelete = { viewModel.deleteLog(it) },
                            userRole = currentUserRole
                        )
                    }
                    "holidays" -> {
                        LocalHolidayCalendarScreen(
                            viewModel = viewModel,
                            holidays = viewModel.localHolidays,
                            todayHoliday = viewModel.todayHoliday.value
                        )
                    }
                    "saas_hub" -> {
                        SaaSHubScreen(viewModel = viewModel)
                    }
                    "supervisor_schedule" -> {
                        Column {
                            SaaSHeader(title = "Supervisor Schedule Desk", onBack = { viewModel.currentScreen.value = "saas_hub" })
                            SupervisorScheduleScreen(viewModel = viewModel)
                        }
                    }
                    "platform_guide" -> {
                        Column {
                            SaaSHeader(title = "Platform Guide & Architecture", onBack = { viewModel.currentScreen.value = "saas_hub" })
                            PlatformGuideScreen(viewModel = viewModel)
                        }
                    }
                    "core_hr" -> {
                        Column {
                            SaaSHeader(title = "Core HR Dossier & Directory", onBack = { viewModel.currentScreen.value = "saas_hub" })
                            CoreHrScreen(viewModel = viewModel)
                        }
                    }
                    "self_service" -> {
                        Column {
                            SaaSHeader(title = "Employee Self-Service Desk", onBack = { viewModel.currentScreen.value = "saas_hub" })
                            SelfServiceScreen(viewModel = viewModel)
                        }
                    }
                    "payroll" -> {
                        Column {
                            SaaSHeader(title = "Payroll Ledger & Bank Transfer", onBack = { viewModel.currentScreen.value = "saas_hub" })
                            PayrollScreen(viewModel = viewModel)
                        }
                    }
                    "admin_control" -> {
                        Column {
                            SaaSHeader(title = "Control Room approvals", onBack = { viewModel.currentScreen.value = "saas_hub" })
                            AdminHubScreen(viewModel = viewModel)
                        }
                    }
                    "ai_assistant" -> {
                        Column {
                            SaaSHeader(title = "AI Compliance assistant", onBack = { viewModel.currentScreen.value = "saas_hub" })
                            AiAssistantScreen(viewModel = viewModel)
                        }
                    }
                    "performance_reports" -> {
                        Column {
                            SaaSHeader(title = "Performance appraisal", onBack = { viewModel.currentScreen.value = "saas_hub" })
                            PerformanceReportingScreen(viewModel = viewModel)
                        }
                    }
                    "top5_dashboard" -> {
                        Column {
                            SaaSHeader(title = "Top 5 Employee Dashboards", onBack = { viewModel.currentScreen.value = "saas_hub" })
                            Top5DashboardScreen(viewModel = viewModel)
                        }
                    }
                    "settings" -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            config = shiftConfig,
                            onSaveConfig = { viewModel.saveConfig(it) },
                            isOffline = viewModel.isMockOffline.value,
                            onOfflineToggle = { viewModel.toggleOfflineMode(it) },
                            onClearAll = { viewModel.clearAllLogs() },
                            userRole = currentUserRole
                        )
                    }
                }
            }

            val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 50.dp

            if (!isKeyboardVisible) {
                // Custom Styled Bottom Navigation Bar (Cyber Indore Cycling Theme)
                MainNavigationBar(
                    currentScreen = currentScreen,
                    userRole = currentUserRole,
                    pendingCount = filteredPendingLogs.size,
                    onTabSelected = { viewModel.currentScreen.value = it }
                )
            }
        }

        // Google Calendar Simulated Notification Overlay Banner
        val activeNotif = viewModel.activeSimulatedNotification.value
        if (activeNotif != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
                    .zIndex(99f)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF4285F4).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Google Calendar Style Logo Icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF4285F4), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GOOGLE CALENDAR REMINDER",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    color = Color(0xFF4285F4),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = activeNotif.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { viewModel.dismissActiveSimulatedNotification() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${activeNotif.date} at ${activeNotif.time}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        
                        if (activeNotif.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeNotif.description,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.dismissActiveSimulatedNotification() }) {
                                Text("Snooze", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.dismissActiveSimulatedNotification()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                            ) {
                                Text("Open Calendar", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Solid dim scrim on top of blurred backdrop to make dialog completely legible
        if (viewDetailsLog != null || editLogState != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }
    }
    }
    }
}

// ---------------------- LOGIN & REGISTER SCREEN ----------------------
@Composable
fun LoginScreen(
    themeName: String,
    onLoginAttempt: (String, String) -> String?,
    onRegisterAttempt: (String, String, String, String, String, String) -> String?
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var isRegisterMode by remember { mutableStateOf(false) }

    // Form States
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("EMPLOYEE") }
    var companyNameInput by remember { mutableStateOf("ClauseOS Corp") }
    var companyCodeInput by remember { mutableStateOf("CLAUSE99") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LiquidGlassBackground(themeName = themeName) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Spacer(modifier = Modifier.height(30.dp))

        // Premium Brand Logo Composable
        ShiftHRLogo(
            modifier = Modifier.size(115.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "SHIFT HR",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Manage Time. Empower People.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Mode Selector Tab Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isRegisterMode) primaryColor else Color.Transparent)
                    .clickable { isRegisterMode = false; errorMessage = null }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "LOG IN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (!isRegisterMode) Color.Black else Color.White
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isRegisterMode) primaryColor else Color.Transparent)
                    .clickable { isRegisterMode = true; errorMessage = null }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "REGISTER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isRegisterMode) Color.Black else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isRegisterMode) "CREATE NEW COMPLIANCE TENANT" else "UNLEASH SECURE SESSION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF43F5E).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFF43F5E), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFF43F5E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // If in Register Mode, show extra fields
                if (isRegisterMode) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorMessage = null },
                        label = { Text("Employee Full Name") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF00E5FF)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("register_name_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedLabelColor = primaryColor
                        )
                    )

                    // Role selector dropdown simulation
                    Text("Select Corporate Tier / Role:", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val roles = listOf("EMPLOYEE", "SUPERVISOR", "MANAGER", "ADMIN_HR")
                        roles.forEach { role ->
                            val isSelected = selectedRole == role
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(1.dp, if (isSelected) primaryColor else Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .clickable { selectedRole = role }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (role == "ADMIN_HR") "ADMIN" else role.take(5),
                                    color = if (isSelected) primaryColor else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = companyNameInput,
                        onValueChange = { companyNameInput = it; errorMessage = null },
                        label = { Text("Corporate Company Name") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF00E5FF)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("register_company_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedLabelColor = primaryColor
                        )
                    )

                    OutlinedTextField(
                        value = companyCodeInput,
                        onValueChange = { companyCodeInput = it; errorMessage = null },
                        label = { Text("Company Verification Code") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF00E5FF)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("register_company_code"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedLabelColor = primaryColor
                        )
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMessage = null },
                    label = { Text("System Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF00E5FF)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("username_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedLabelColor = primaryColor
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Session Passcode") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00E5FF)) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("password_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedLabelColor = primaryColor
                    )
                )

                Button(
                    onClick = {
                        if (isRegisterMode) {
                            if (username.isBlank() || password.isBlank() || fullName.isBlank()) {
                                errorMessage = "Please fill in all registration fields."
                            } else {
                                val err = onRegisterAttempt(username, password, fullName, selectedRole, companyNameInput, companyCodeInput)
                                if (err != null) {
                                    errorMessage = err
                                } else {
                                    isRegisterMode = false // Switch back to login
                                    errorMessage = null
                                    Toast.makeText(context, "Registration successful! You may now log in.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            if (username.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter both credentials."
                            } else {
                                val err = onLoginAttempt(username, password)
                                if (err != null) errorMessage = err
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isRegisterMode) "REGISTER SECURE COMPLIANCE" else "ACCESS HUB",
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Pre-filled Credential Help Box (Aesthetic & Practical)
        Text(
            text = "DEMO ACCOUNTS (TAP TO FILL)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val demoUsers = listOf(
                Triple("Employee ( Sarah Jenkins )", "employee", "emp123"),
                Triple("Team Supervisor ( Robert Chen )", "supervisor", "super123"),
                Triple("General Manager ( Anjali Sharma )", "manager", "manager123"),
                Triple("Admin/HR Director ( Aditya Joshi )", "admin", "admin123")
            )

            demoUsers.forEach { (label, u, p) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isRegisterMode = false
                            username = u
                            password = p
                            errorMessage = null
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                            Text(text = "User: $u | Pass: $p", fontSize = 10.sp, color = Color(0xFF00E5FF))
                        }
                        Icon(
                            imageVector = Icons.Default.Login,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(30.dp))
    }
    }
}

// ---------------------- MAIN COMPONENTS ----------------------

@Composable
fun HeaderBar(
    roleName: String,
    displayName: String,
    isOffline: Boolean,
    isSyncing: Boolean,
    onLogout: () -> Unit,
    onSyncClick: () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Side: Avatar & Name/Badge Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onProfileClick() }
        ) {
            // Glowing Avatar Circle with 'S'
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00FF88), Color(0xFF00E5FF))
                        ),
                        shape = CircleShape
                    )
                    .padding(2.dp) // border thickness
                    .background(Color(0xFF121318), CircleShape), // inner background
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (displayName.isNotEmpty()) displayName.take(1).uppercase() else "S",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Employee Capsule
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF00FF88).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = roleName.uppercase(),
                            color = Color(0xFF00FF88),
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp
                        )
                    }

                    // Online indicator dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isOffline) Color(0xFFF43F5E) else Color(0xFF00FF88))
                    )
                    Text(
                        text = if (isOffline) "Offline" else "Online",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Right Side: Refresh & Exit Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Sync / Refresh button (Square with rounded corners)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .clickable(enabled = !isOffline, onClick = onSyncClick),
                contentAlignment = Alignment.Center
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF00FF88),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = if (isOffline) Color.White.copy(alpha = 0.2f) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Exit button (Red capsule)
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .background(Color(0xFFEF4444).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onLogout)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Logout",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Exit",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NotificationsSection(
    notifications: List<NotificationItem>,
    onDismiss: (String) -> Unit
) {
    if (notifications.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        val latest = notifications.first()
        val isSecurityCheck = latest.title.equals("SECURITY CHECK", ignoreCase = true)
        
        val containerColor = if (isSecurityCheck) {
            Color(0xFF041E15) // Deep rich green forest background
        } else if (latest.isAlert) {
            Color(0xFF3B1D22)
        } else {
            Color(0xFF1D352F)
        }
        
        val borderColor = if (isSecurityCheck) {
            Color(0xFF00FF88).copy(alpha = 0.5f) // Bright lime neon border
        } else if (latest.isAlert) {
            Color(0xFFF43F5E).copy(alpha = 0.4f)
        } else {
            Color(0xFF10B981).copy(alpha = 0.4f)
        }
        
        val titleColor = if (isSecurityCheck) {
            Color(0xFFCCFF00) // Lime yellow/green
        } else if (latest.isAlert) {
            Color(0xFFFFA5B5)
        } else {
            Color(0xFFCCFF00)
        }
        
        val messageColor = if (isSecurityCheck) {
            Color(0xFFCCFF00).copy(alpha = 0.85f)
        } else {
            Color.White.copy(alpha = 0.9f)
        }
        
        val iconTint = if (isSecurityCheck) {
            Color(0xFFCCFF00) // Lime yellow/green bell
        } else if (latest.isAlert) {
            Color(0xFFF43F5E)
        } else {
            Color(0xFFCCFF00)
        }
        
        val iconVector = if (isSecurityCheck) {
            Icons.Default.NotificationsActive
        } else if (latest.isAlert) {
            Icons.Default.Warning
        } else {
            Icons.Default.NotificationsActive
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor
                ),
                border = BorderStroke(
                    1.dp,
                    borderColor
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = latest.title.uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = titleColor,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = latest.message,
                                fontSize = 11.sp,
                                color = messageColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = { onDismiss(latest.id) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigationBar(
    currentScreen: String,
    userRole: String,
    pendingCount: Int,
    onTabSelected: (String) -> Unit
) {
    val isStaff = userRole == "ADMIN_HR" || userRole == "MANAGER" || userRole == "SUPERVISOR"
    val primaryColor = MaterialTheme.colorScheme.primary

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = primaryColor,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentScreen == "clock",
            onClick = { onTabSelected("clock") },
            icon = { Icon(Icons.Outlined.Timer, contentDescription = "Clock", modifier = Modifier.size(24.dp)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = primaryColor,
                indicatorColor = primaryColor,
                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                unselectedTextColor = Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier.testTag("tab_clock")
        )

        NavigationBarItem(
            selected = currentScreen == "spreadsheet",
            onClick = { onTabSelected("spreadsheet") },
            icon = { Icon(Icons.Outlined.Leaderboard, contentDescription = "Stats", modifier = Modifier.size(24.dp)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = primaryColor,
                indicatorColor = primaryColor,
                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                unselectedTextColor = Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier.testTag("tab_spreadsheet")
        )

        NavigationBarItem(
            selected = currentScreen == "chat",
            onClick = { onTabSelected("chat") },
            icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Chat", modifier = Modifier.size(24.dp)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = primaryColor,
                indicatorColor = primaryColor,
                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                unselectedTextColor = Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier.testTag("tab_chat")
        )

        if (isStaff) {
            NavigationBarItem(
                selected = currentScreen == "hr_approval",
                onClick = { onTabSelected("hr_approval") },
                icon = {
                    BadgedBox(
                        badge = {
                            if (pendingCount > 0) {
                                Badge(containerColor = Color(0xFFF43F5E)) {
                                    Text(pendingCount.toString(), color = Color.White)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.FactCheck, contentDescription = "Approvals", modifier = Modifier.size(24.dp))
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = primaryColor,
                    indicatorColor = primaryColor,
                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                ),
                modifier = Modifier.testTag("tab_hr_approval")
            )
        }

        NavigationBarItem(
            selected = currentScreen == "saas_hub" || currentScreen == "core_hr" || currentScreen == "self_service" || currentScreen == "payroll" || currentScreen == "admin_control" || currentScreen == "ai_assistant",
            onClick = { onTabSelected("saas_hub") },
            icon = { Icon(Icons.Outlined.Hub, contentDescription = "SaaS Hub", modifier = Modifier.size(24.dp)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = primaryColor,
                indicatorColor = primaryColor,
                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                unselectedTextColor = Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier.testTag("tab_saas_hub")
        )

        NavigationBarItem(
            selected = currentScreen == "holidays",
            onClick = { onTabSelected("holidays") },
            icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Holidays", modifier = Modifier.size(24.dp)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = primaryColor,
                indicatorColor = primaryColor,
                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                unselectedTextColor = Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier.testTag("tab_holidays")
        )

        NavigationBarItem(
            selected = currentScreen == "settings",
            onClick = { onTabSelected("settings") },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings", modifier = Modifier.size(24.dp)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = primaryColor,
                indicatorColor = primaryColor,
                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                unselectedTextColor = Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier.testTag("tab_settings")
        )
    }
}

// ---------------------- WEATHER UTILITIES & GLASSMORPHIC ADVISORY ----------------------
fun getWeatherVisuals(condition: String): Pair<ImageVector, Color> {
    val cond = condition.lowercase()
    return when {
        cond.contains("rain") || cond.contains("drizzle") || cond.contains("shower") -> 
            Icons.Default.Umbrella to Color(0xFF38BDF8)
        cond.contains("thunder") -> 
            Icons.Default.Thunderstorm to Color(0xFFFBBF24)
        cond.contains("cloud") || cond.contains("overcast") -> 
            Icons.Default.CloudQueue to Color(0xFF94A3B8)
        cond.contains("snow") -> 
            Icons.Default.AcUnit to Color(0xFFE2E8F0)
        cond.contains("clear") || cond.contains("sun") -> 
            Icons.Default.WbSunny to Color(0xFFFBBF24)
        else -> 
            Icons.Default.CloudQueue to Color(0xFF38BDF8)
    }
}

@Composable
fun WeatherForecastCard(viewModel: TimeTrackerViewModel) {
    val currentWeather = viewModel.currentWeather.value
    val forecast = viewModel.weatherForecast.value
    val isLoading = viewModel.weatherLoading.value
    var cityInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title and City Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SHIFT WEATHER ADVISORY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = currentWeather?.name ?: "Loading...",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // City Switcher Input Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        placeholder = { Text("Search city", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f)) },
                        modifier = Modifier
                            .width(110.dp)
                            .height(44.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (cityInput.isNotBlank()) {
                                viewModel.fetchWeatherForecast(cityInput)
                                cityInput = ""
                            }
                        })
                    )
                    IconButton(
                        onClick = {
                            if (cityInput.isNotBlank()) {
                                viewModel.fetchWeatherForecast(cityInput)
                                cityInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search Weather", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(28.dp))
                }
            } else {
                currentWeather?.let { weather ->
                    val (icon, color) = getWeatherVisuals(weather.weather.firstOrNull()?.main ?: "Clear")
                    
                    // Main layout: Temp on left, large illustration on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${weather.main.temp.toInt()}",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Light,
                                    color = Color.White
                                )
                                Text(
                                    text = "°C",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Light,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            Text(
                                text = weather.weather.firstOrNull()?.description?.uppercase() ?: "CLEAR SKY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Feels like ${weather.main.feelsLike.toInt()}°C  •  H: ${weather.main.tempMax.toInt()}° L: ${weather.main.tempMin.toInt()}°",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // Large glassy icon box
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(color.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                                .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 columns: Wind, Humidity, Pressure
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Air, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                            Text("WIND", fontSize = 8.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.padding(top = 2.dp))
                            Text("${weather.wind.speed} m/s", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.08f)).align(Alignment.CenterVertically))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Text("HUMIDITY", fontSize = 8.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.padding(top = 2.dp))
                            Text("${weather.main.humidity}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.08f)).align(Alignment.CenterVertically))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Compress, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                            Text("PRESSURE", fontSize = 8.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.padding(top = 2.dp))
                            Text("${weather.main.pressure} hPa", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Shift Recommendation based on weather
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val recText = when {
                                weather.weather.firstOrNull()?.main?.lowercase()?.contains("rain") == true ->
                                    "Heavy rain in ${weather.name}. Shift HR advises remote work or coffee shop co-working today!"
                                weather.weather.firstOrNull()?.main?.lowercase()?.contains("thunder") == true ->
                                    "Thunderstorms active. Central hub office is open, but outdoor operations are suspended."
                                weather.weather.firstOrNull()?.main?.lowercase()?.contains("cloud") == true ->
                                    "Comfortable cloudy weather. Ideal day for hybrid shifts at the regional Hub!"
                                weather.main.temp > 33.0 ->
                                    "Heat advisory! Shift HR recommends air-conditioned spaces and proper hydration."
                                else ->
                                    "Beautiful clear skies! Perfect conditions to log productive hours at the Shift HR Hub."
                            }
                            Text(
                                text = recText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f),
                                lineHeight = 12.sp
                            )
                        }
                    }
                }

                // Hourly forecast list (6 steps)
                forecast?.let { fc ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "3-HOUR SHIFT TEMPERATURES",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hoursList = fc.list.take(6)
                        hoursList.forEach { item ->
                            val temp = item.main.temp.toInt()
                            val timeStr = item.dtTxt.substringAfter(" ").substringBeforeLast(":")
                            val cond = item.weather.firstOrNull()?.main ?: "Clear"
                            val (fIcon, fColor) = getWeatherVisuals(cond)

                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = timeStr, fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Icon(fIcon, contentDescription = null, tint = fColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "$temp°", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- SCREEN 1: EMPLOYEE CHRONO SCREEN ----------------------
@Composable
fun EmployeeClockScreen(
    activeLog: TimeLogEntity?,
    shiftConfig: ShiftConfigEntity,
    timerString: String,
    breakTimerString: String,
    onPunchAction: (String) -> Unit,
    todayHoliday: Holiday?,
    viewModel: TimeTrackerViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Large Glassmorphic Weather Forecast Card
        WeatherForecastCard(viewModel = viewModel)

        // Quick platform guide helper banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { viewModel.currentScreen.value = "platform_guide" },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "First time? Try the Interactive Platform Guide & Diagram 📚",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Glassmorphic Chrono Shift Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SHIFT TIMING CHRONO",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.5.sp
                    )

                    // Glow status badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(
                                if (activeLog != null) Color(0xFF10B981).copy(alpha = 0.1f)
                                else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (activeLog != null) Color(0xFF10B981) else Color.White.copy(alpha = 0.3f))
                        )
                        Text(
                            text = if (activeLog != null) "ACTIVE SHIFT" else "OFF CLOCK",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeLog != null) Color(0xFF10B981) else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Modern monospace digital clock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                        .padding(vertical = 18.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timerString,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 38.sp,
                            color = Color.White,
                            modifier = Modifier.testTag("live_clock_timer")
                        )
                        Text(
                            text = "ELAPSED SHIFT HOURS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.3f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                if (breakTimerString.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = breakTimerString.uppercase(),
                            color = Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Metric row with custom weather-inspired colors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "SHIFT TARGET", fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                        Text(text = "${shiftConfig.shiftDurationHours} HOURS", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.08f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "HOURLY PAY", fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                        Text(text = "${viewModel.getCurrencySymbol()}${shiftConfig.hourlyRate} / HR", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF38BDF8))
                    }
                }
            }
        }

        // Today's Holiday Banner Alert (if today is a holiday)
        if (todayHoliday != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D352F)),
                border = BorderStroke(1.dp, Color(0xFFCCFF00).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        tint = Color(0xFFCCFF00),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "TODAY'S REGIONAL HOLIDAY",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = Color(0xFFCCFF00),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = todayHoliday.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = todayHoliday.description,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Birthday Reminder Widget
        val birthdaysToday = viewModel.birthdayList.value.filter { it.isToday }
        val upcomingBirthdays = viewModel.birthdayList.value.filter { !it.isToday }
        
        if (birthdaysToday.isNotEmpty() || upcomingBirthdays.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = "Birthdays Today",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COMPLIANCE TEAM BIRTHDAYS",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    if (birthdaysToday.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        for (bday in birthdaysToday) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Today is ${bday.name}'s Birthday! 🎂",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Role: ${bday.role} | Tap to send warm Indore wishes!",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.activeRecipient.value = bday.name
                                        viewModel.currentScreen.value = "chat"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(32.dp).testTag("wish_birthday_${bday.name.lowercase().replace(" ", "_")}")
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WISH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                    
                    if (upcomingBirthdays.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "UPCOMING REMINDERS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (bday in upcomingBirthdays) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.activeRecipient.value = bday.name
                                            viewModel.currentScreen.value = "chat"
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = bday.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(text = "${bday.dateStr} (${bday.daysUntil}d)", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sequential Chrono Stations Panel
        Text(
            text = "CHRONO INTERCEPT PUNCHES",
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 12.dp)
        )

        // Action Grid of punches
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PunchButton(
                    text = "Time In",
                    icon = Icons.Default.PlayArrow,
                    bgColor = Color(0xFF10B981),
                    enabled = activeLog == null,
                    modifier = Modifier.weight(1f).testTag("punch_time_in")
                ) { onPunchAction("TIME_IN") }

                PunchButton(
                    text = "Time Out",
                    icon = Icons.Default.Stop,
                    bgColor = Color(0xFFF43F5E),
                    enabled = activeLog != null && activeLog.timeOut == null,
                    modifier = Modifier.weight(1f).testTag("punch_time_out")
                ) { onPunchAction("TIME_OUT") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PunchButton(
                    text = "Lunch Out",
                    icon = Icons.Default.Restaurant,
                    bgColor = Color(0xFF6366F1),
                    enabled = activeLog != null && activeLog.lunchOut == null && activeLog.timeOut == null,
                    modifier = Modifier.weight(1f).testTag("punch_lunch_out")
                ) { onPunchAction("LUNCH_OUT") }

                PunchButton(
                    text = "Lunch In",
                    icon = Icons.Default.Fastfood,
                    bgColor = Color(0xFF8B5CF6),
                    enabled = activeLog != null && activeLog.lunchOut != null && activeLog.lunchIn == null && activeLog.timeOut == null,
                    modifier = Modifier.weight(1f).testTag("punch_lunch_in")
                ) { onPunchAction("LUNCH_IN") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PunchButton(
                    text = "Break Out",
                    icon = Icons.Default.Coffee,
                    bgColor = Color(0xFFF59E0B),
                    enabled = activeLog != null && activeLog.breakOut == null && activeLog.timeOut == null,
                    modifier = Modifier.weight(1f).testTag("punch_break_out")
                ) { onPunchAction("BREAK_OUT") }

                PunchButton(
                    text = "Break In",
                    icon = Icons.Default.FreeBreakfast,
                    bgColor = Color(0xFFD97706),
                    enabled = activeLog != null && activeLog.breakOut != null && activeLog.breakIn == null && activeLog.timeOut == null,
                    modifier = Modifier.weight(1f).testTag("punch_break_in")
                ) { onPunchAction("BREAK_IN") }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dynamic workflow timeline map
        WorkflowTimelineCard(log = activeLog, config = shiftConfig)

        Spacer(modifier = Modifier.height(30.dp))
    }
}

class PunchBubbleAnimationState(
    val id: Long,
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float,
    val maxRadius: Float,
    val durationMillis: Int = 500,
    val delayMillis: Int = 0
) {
    val progress = Animatable(0f)

    suspend fun animate() {
        if (delayMillis > 0) {
            delay(delayMillis.toLong())
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        )
    }
}

@Composable
fun PunchButton(
    text: String,
    icon: ImageVector,
    bgColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val bubbles = remember { mutableStateListOf<PunchBubbleAnimationState>() }
    var buttonWidth by remember { mutableStateOf(0f) }
    var buttonHeight by remember { mutableStateOf(0f) }
    val localDensity = androidx.compose.ui.platform.LocalDensity.current

    // Coordinates padding for canvas overflow (so bubbles splash outside the button smoothly)
    val padXPx = remember { with(localDensity) { 16.dp.toPx() } }
    val padYPx = remember { with(localDensity) { 8.dp.toPx() } }

    val cornerRadius = 16.dp

    Box(
        modifier = modifier
            .height(54.dp)
            .onSizeChanged { size ->
                buttonWidth = size.width.toFloat()
                buttonHeight = size.height.toFloat()
            },
        contentAlignment = Alignment.Center
    ) {
        if (!enabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(cornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = text.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        } else {
            // 1. Gooey layer (Blurred and Contrasted together on GPU RenderThread)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                val blurEffect = android.graphics.RenderEffect.createBlurEffect(
                                    12f,
                                    12f,
                                    android.graphics.Shader.TileMode.CLAMP
                                )
                                val alphaMatrix = floatArrayOf(
                                    1f, 0f, 0f, 0f, 0f,
                                    0f, 1f, 0f, 0f, 0f,
                                    0f, 0f, 1f, 0f, 0f,
                                    0f, 0f, 0f, 25f, -300f
                                )
                                val colorFilterEffect = android.graphics.RenderEffect.createColorFilterEffect(
                                    android.graphics.ColorMatrixColorFilter(alphaMatrix)
                                )
                                val chain = android.graphics.RenderEffect.createChainEffect(colorFilterEffect, blurEffect)
                                renderEffect = chain.asComposeRenderEffect()
                            } catch (e: Exception) {
                                // Fallback
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (buttonWidth > 0 && buttonHeight > 0) {
                        // Draw button background body
                        drawRoundRect(
                            color = bgColor,
                            topLeft = Offset(padXPx, padYPx),
                            size = Size(buttonWidth - 2 * padXPx, buttonHeight - 2 * padYPx),
                            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                        )

                        // Draw organic splash bubbles merging with the background
                        bubbles.forEach { bubble ->
                            val p = bubble.progress.value
                            val currentX = bubble.startX + (bubble.targetX - bubble.startX) * p
                            val currentY = bubble.startY + (bubble.targetY - bubble.startY) * p
                            // Smooth sine wave scaling: 0 -> 1 -> 0
                            val scale = sin(p * Math.PI).toFloat()
                            val radius = bubble.maxRadius * scale

                            if (radius > 0.1f) {
                                drawCircle(
                                    color = bgColor,
                                    radius = radius,
                                    center = Offset(currentX, currentY)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Crisp overlay text layer (unblurred for high contrast readability)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(cornerRadius))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { localOffset ->
                                val canvasX = localOffset.x + padXPx
                                val canvasY = localOffset.y + padYPx

                                val tapTime = System.currentTimeMillis()
                                val newBubbles = mutableListOf<PunchBubbleAnimationState>()

                                // Add center major merge bubble
                                newBubbles.add(
                                    PunchBubbleAnimationState(
                                        id = tapTime,
                                        startX = canvasX,
                                        startY = canvasY,
                                        targetX = canvasX + (buttonWidth / 2f - canvasX) * 0.3f,
                                        targetY = canvasY + (buttonHeight / 2f - canvasY) * 0.3f,
                                        maxRadius = 24.dp.toPx(),
                                        durationMillis = 500
                                    )
                                )

                                // Add satellite splash droplets
                                for (i in 0 until 2) {
                                    val angle = (Math.random() * 2 * Math.PI).toFloat()
                                    val distance = (25f + Math.random().toFloat() * 25f)
                                    val satTargetX = canvasX + cos(angle) * distance
                                    val satTargetY = canvasY + sin(angle) * distance

                                    newBubbles.add(
                                        PunchBubbleAnimationState(
                                            id = tapTime + i + 1,
                                            startX = canvasX,
                                            startY = canvasY,
                                            targetX = satTargetX,
                                            targetY = satTargetY,
                                            maxRadius = (6f + Math.random().toFloat() * 6f).dp.toPx(),
                                            durationMillis = 400 + (Math.random() * 100).toInt(),
                                            delayMillis = (Math.random() * 30).toInt()
                                        )
                                    )
                                }

                                bubbles.addAll(newBubbles)
                                newBubbles.forEach { bubble ->
                                    coroutineScope.launch {
                                        bubble.animate()
                                        bubbles.remove(bubble)
                                    }
                                }

                                // Trigger actual action callback
                                onClick()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = text.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun WorkflowTimelineCard(log: TimeLogEntity?, config: ShiftConfigEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "SHIFT PROGRESSION",
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = Color(0xFFCCFF00),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            val steps = listOf(
                TimelineStep("Start Shift (Time In)", log?.timeIn, "Check-in at workstation"),
                TimelineStep("Lunch Break Start", log?.lunchOut, "Rest window limit: ${config.lunchDurationMinutes} mins"),
                TimelineStep("Lunch Break End", log?.lunchIn, "Resume activities"),
                TimelineStep("Short Break Start", log?.breakOut, "Break limit: ${config.breakDurationMinutes} mins"),
                TimelineStep("Short Break End", log?.breakIn, "Resume activities"),
                TimelineStep("Finish Shift (Time Out)", log?.timeOut, "Chrono stop")
            )

            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (step.timestamp != null) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (step.timestamp != null) Color(0xFFCCFF00) else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.name,
                            fontWeight = if (step.timestamp != null) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (step.timestamp != null) Color.White else Color.White.copy(alpha = 0.4f)
                        )
                        Text(
                            text = step.desc,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }

                    if (step.timestamp != null) {
                        Text(
                            text = formatTimestamp(step.timestamp),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF00E5FF)
                        )
                    } else {
                        Text(
                            text = "Pending",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }

                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .padding(start = 9.dp)
                            .width(2.dp)
                            .height(14.dp)
                            .background(
                                if (step.timestamp != null && steps[index + 1].timestamp != null) Color(0xFFCCFF00)
                                else Color.White.copy(alpha = 0.08f)
                            )
                    )
                }
            }

            if (log != null && log.isApproved != "PENDING") {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (log.isApproved == "APPROVED") Icons.Default.Verified else Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = if (log.isApproved == "APPROVED") Color(0xFF10B981) else Color(0xFFF43F5E),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AUDIT DECISION: ${log.isApproved}",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = if (log.isApproved == "APPROVED") Color(0xFF10B981) else Color(0xFFF43F5E),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

data class TimelineStep(val name: String, val timestamp: Long?, val desc: String)

// ---------------------- SCREEN 2: SPREADSHEET / TIMESHEET ----------------------
@Composable
fun SpreadsheetScreen(
    logs: List<TimeLogEntity>,
    shiftConfig: ShiftConfigEntity,
    filterApproval: String,
    filterSync: String,
    searchQuery: String,
    onFilterApprovalChange: (String) -> Unit,
    onFilterSyncChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onExportClick: () -> Unit,
    onRowClick: (TimeLogEntity) -> Unit,
    viewModel: TimeTrackerViewModel,
    currentUserRole: String,
    currentUserName: String
) {
    val df = DecimalFormat("#.##")
    val isManagement = currentUserRole == "ADMIN_HR" || currentUserRole == "MANAGER" || currentUserRole == "SUPERVISOR"

    val filteredLogs = logs.filter { log ->
        val matchesUser = isManagement || log.employeeName.equals(currentUserName, ignoreCase = true)
        val matchesSearch = !isManagement || log.employeeName.contains(searchQuery, ignoreCase = true)
        val matchesApproval = filterApproval == "ALL" || log.isApproved == filterApproval
        val matchesSync = when (filterSync) {
            "ALL" -> true
            "LOCAL" -> !log.isSynced
            "SYNCED" -> log.isSynced
            else -> true
        }
        matchesUser && matchesSearch && matchesApproval && matchesSync
    }

    // Calculations
    var totalHours = 0.0
    var approvedCount = 0
    var estimatedWages = 0.0
    var unsyncedCount = 0

    filteredLogs.forEach { log ->
        val activeRate = log.hourlyRate
        val totalMillis = if (log.timeIn != null && log.timeOut != null) {
            val total = log.timeOut - log.timeIn
            val lunch = if (log.lunchOut != null && log.lunchIn != null) log.lunchIn - log.lunchOut else 0L
            val brk = if (log.breakOut != null && log.breakIn != null) log.breakIn - log.breakOut else 0L
            total - lunch - brk
        } else 0L

        val hours = totalMillis.toDouble() / 3600000.0
        totalHours += hours

        if (log.isApproved == "APPROVED") approvedCount++
        if (!log.isSynced) unsyncedCount++

        val limit = shiftConfig.shiftDurationHours
        if (hours > limit) {
            estimatedWages += (limit * activeRate) + ((hours - limit) * activeRate * 1.5)
        } else {
            estimatedWages += hours * activeRate
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(10.dp))

        // Large Cyber Metrics Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardMetricCard(
                title = "TOTAL HOURS",
                value = "${df.format(totalHours)}h",
                icon = Icons.Default.Timeline,
                tint = Color(0xFFCCFF00)
            )
            DashboardMetricCard(
                title = "EST PAYROLL",
                value = "${viewModel.getCurrencySymbol()}${df.format(estimatedWages)}",
                icon = Icons.Default.Paid,
                tint = Color(0xFF00E5FF)
            )
            DashboardMetricCard(
                title = "APPROVED SHIFTS",
                value = "$approvedCount / ${filteredLogs.size}",
                icon = Icons.Default.FactCheck,
                tint = Color(0xFF10B981)
            )
            if (unsyncedCount > 0) {
                DashboardMetricCard(
                    title = "UNSYNCED LOCAL",
                    value = "$unsyncedCount logs",
                    icon = Icons.Default.CloudQueue,
                    tint = Color(0xFFF43F5E)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Employee compliance selection chip row
        val employees = listOf("Sarah Jenkins", "Marcus Aurelius", "Robert Chen", "Anjali Sharma", "Aditya Joshi", "Michael Vance")
        val selectedEmployee = if (isManagement) viewModel.selectedEmployeeForChart.value else currentUserName

        if (isManagement) {
            Text(
                text = "EMPLOYEE PRODUCTIVITY PROFILES",
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                employees.forEach { emp ->
                    val isSelected = selectedEmployee == emp
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedEmployeeForChart.value = emp },
                        label = { Text(emp, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("chart_employee_chip_${emp.lowercase().replace(" ", "_")}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.White.copy(alpha = 0.04f),
                            labelColor = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }

        // Beautiful custom line chart displaying the selected employee's scores
        val chartRecords = viewModel.productivityRecords[selectedEmployee] ?: emptyList()
        ProductivityLineChart(
            records = chartRecords,
            employeeName = selectedEmployee,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Search inputs
        if (isManagement) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search Employee...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("employee_search_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f)
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Filter badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Decision:", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.4f), letterSpacing = 1.sp)

            val appFilters = listOf("ALL", "PENDING", "APPROVED", "REJECTED")
            appFilters.forEach { filter ->
                FilterChip(
                    selected = filterApproval == filter,
                    onClick = { onFilterApprovalChange(filter) },
                    label = { Text(filter, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("filter_approval_${filter.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actual Ledger Spreadsheet card
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CHRONO LEDGER (${filteredLogs.size} SHIFTS)",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }

                    Button(
                        onClick = onExportClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("csv_export_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SHARE CSV", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.02f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("EMPLOYEE / DATE", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.weight(1.5f))
                    Text("CHRONO INTERCEPTS", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.weight(1.8f))
                    Text("HOURS", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                    Text("STATUS", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No work sessions registered.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f))
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        filteredLogs.forEach { log ->
                            SpreadsheetRow(
                                log = log,
                                shiftConfig = shiftConfig,
                                onClick = { onRowClick(log) }
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun SpreadsheetRow(
    log: TimeLogEntity,
    shiftConfig: ShiftConfigEntity,
    onClick: () -> Unit
) {
    val df = DecimalFormat("#.##")
    val totalMillis = if (log.timeIn != null && log.timeOut != null) {
        val total = log.timeOut - log.timeIn
        val lunch = if (log.lunchOut != null && log.lunchIn != null) log.lunchIn - log.lunchOut else 0L
        val brk = if (log.breakOut != null && log.breakIn != null) log.breakIn - log.breakOut else 0L
        total - lunch - brk
    } else 0L

    val hours = totalMillis.toDouble() / 3600000.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Col 1: Name and Date
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = log.employeeName,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = log.date,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        // Col 2: Punch times
        Column(modifier = Modifier.weight(1.8f)) {
            Text(
                text = "In: ${formatTimestamp(log.timeIn)}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = "Out: ${formatTimestamp(log.timeOut)}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (log.timeOut == null) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.8f)
            )
        }

        // Col 3: Work Hours
        Text(
            text = "${df.format(hours)}h",
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.7f),
            textAlign = TextAlign.End,
            color = if (hours > shiftConfig.shiftDurationHours) Color(0xFFF43F5E) else MaterialTheme.colorScheme.primary
        )

        // Col 4: Badges
        Column(
            modifier = Modifier.weight(1.2f),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = when (log.isApproved) {
                            "APPROVED" -> Color(0xFF10B981).copy(alpha = 0.15f)
                            "REJECTED" -> Color(0xFFF43F5E).copy(alpha = 0.15f)
                            else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
                    .border(
                        1.dp,
                        color = when (log.isApproved) {
                            "APPROVED" -> Color(0xFF10B981)
                            "REJECTED" -> Color(0xFFF43F5E)
                            else -> Color(0xFFF59E0B)
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = log.isApproved,
                    color = when (log.isApproved) {
                        "APPROVED" -> Color(0xFF10B981)
                        "REJECTED" -> Color(0xFFF43F5E)
                        else -> Color(0xFFF59E0B)
                    },
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (log.isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = if (log.isSynced) Color(0xFF10B981) else Color(0xFFF43F5E),
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (log.isSynced) "Synced" else "Local",
                    fontSize = 8.sp,
                    color = if (log.isSynced) Color(0xFF10B981) else Color(0xFFF43F5E),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(82.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            }

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

// ---------------------- SCREEN 3: ADMIN APPROVAL SCREEN ----------------------
@Composable
fun AdminApprovalScreen(
    pendingLogs: List<TimeLogEntity>,
    allLogs: List<TimeLogEntity>,
    onApprove: (Long) -> Unit,
    onReject: (Long) -> Unit,
    onEdit: (TimeLogEntity) -> Unit,
    onDelete: (TimeLogEntity) -> Unit,
    userRole: String
) {
    var selectedApprovalTab by remember { mutableStateOf(0) } // 0 = Pending, 1 = Audit History

    val isAuthorized = userRole == "ADMIN_HR" || userRole == "MANAGER" || userRole == "SUPERVISOR"

    if (!isAuthorized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("RESTRICTED MODULE", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Only HR, Managers, or Supervisors can authorize punches.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f))
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(10.dp))

        TabRow(
            selectedTabIndex = selectedApprovalTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFFCCFF00),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Tab(
                selected = selectedApprovalTab == 0,
                onClick = { selectedApprovalTab = 0 },
                text = { Text("Awaiting Approval (${pendingLogs.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedApprovalTab == 1,
                onClick = { selectedApprovalTab = 1 },
                text = { Text("Audit History (${allLogs.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedApprovalTab == 0) {
            if (pendingLogs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TaskAlt, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No pending timesheets left for review!", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("All employee timesheets are cleared.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pendingLogs) { log ->
                        PendingApprovalItem(
                            log = log,
                            onApprove = { onApprove(log.id) },
                            onReject = { onReject(log.id) },
                            onEdit = { onEdit(log) }
                        )
                    }
                }
            }
        } else {
            if (allLogs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No records found in database.", color = Color.White.copy(alpha = 0.4f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allLogs) { log ->
                        AuditHistoryItem(
                            log = log,
                            onEdit = { onEdit(log) },
                            onDelete = { onDelete(log) },
                            canModify = (userRole == "ADMIN_HR" || userRole == "MANAGER")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PendingApprovalItem(
    log: TimeLogEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onEdit: () -> Unit
) {
    val df = DecimalFormat("#.##")
    val totalMillis = if (log.timeIn != null && log.timeOut != null) {
        val total = log.timeOut - log.timeIn
        val lunch = if (log.lunchOut != null && log.lunchIn != null) log.lunchIn - log.lunchOut else 0L
        val brk = if (log.breakOut != null && log.breakIn != null) log.breakIn - log.breakOut else 0L
        total - lunch - brk
    } else 0L
    val hours = totalMillis.toDouble() / 3600000.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = log.employeeName, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                    Text(text = "Date: ${log.date}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("PENDING APPROVAL", color = Color(0xFFF59E0B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timestamps row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "TIME IN", fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                    Text(text = formatTimestamp(log.timeIn), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                }
                Column {
                    Text(text = "LUNCH TIME", fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                    val lunchMins = if (log.lunchOut != null && log.lunchIn != null) (log.lunchIn - log.lunchOut) / 60000 else 0
                    Text(text = "${lunchMins}M", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                }
                Column {
                    Text(text = "TIME OUT", fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                    Text(text = formatTimestamp(log.timeOut), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "CALCULATED", fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                    Text(text = "${df.format(hours)} HRS", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFCCFF00))
                }
            }

            // GPS verified location
            if (log.gpsLocationName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF00E5FF).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GPS: ${log.gpsLocationName} [${df.format(log.gpsLatitude ?: 0.0)}, ${df.format(log.gpsLongitude ?: 0.0)}]",
                        fontSize = 10.sp,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modify", fontSize = 12.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onReject,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(34.dp).testTag("reject_log_action")
                ) {
                    Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                }

                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(34.dp).testTag("approve_log_action")
                ) {
                    Text("Approve", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AuditHistoryItem(
    log: TimeLogEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canModify: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = log.employeeName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                Text(text = "${log.date} | Decision: ${log.isApproved}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                if (log.rejectionReason != null) {
                    Text(
                        text = "Policy Infraction: ${log.rejectionReason}",
                        fontSize = 11.sp,
                        color = Color(0xFFF43F5E),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (canModify) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ---------------------- SCREEN 4: LOCAL HOLIDAY CALENDAR SCREEN ----------------------
@Composable
fun LocalHolidayCalendarScreen(
    viewModel: TimeTrackerViewModel,
    holidays: List<Holiday>,
    todayHoliday: Holiday?
) {
    val context = LocalContext.current
    var selectedDay by remember { mutableStateOf(29) } // Default to today's day (June 29)
    val selectedDateStr = "2026-06-%02d".format(selectedDay)
    
    // Notes created for selected day
    val notesForSelectedDay = viewModel.calendarNotes.value.filter { it.date == selectedDateStr }
    val holidayForSelectedDay = holidays.find { it.date == selectedDateStr }

    // Add note form state
    var noteTitle by remember { mutableStateOf("") }
    var noteDesc by remember { mutableStateOf("") }
    var noteTime by remember { mutableStateOf("09:00 AM") }
    var syncGoogleCalendar by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Hero Indore Cycling / Festive Header card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFCCFF00).copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = Color(0xFFCCFF00),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CHRONO CALENDAR HUB",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = Color(0xFFCCFF00),
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Interactive Calendar & Notes",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Timesheet integration honors gazetted Indian national holidays, state local holidays, and custom employee notes. Set reminders with Google Calendar style notifications!",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                if (todayHoliday != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFCCFF00).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFCCFF00), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "ACTIVE CELEBRATION: Today is ${todayHoliday.name}! Premium holiday compensation is active.",
                            fontSize = 11.sp,
                            color = Color(0xFFCCFF00),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- CALENDAR GRID CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Month Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JUNE 2026",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Active Reminders",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Days of week header
                Row(modifier = Modifier.fillMaxWidth()) {
                    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days Grid (June 2026 has 30 days and starts on Monday)
                val daysInMonth = 30
                val totalCells = 35 // 5 weeks of 7 days
                
                var currentDayNumber = 1
                for (week in 0 until 5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (dayOfWeek in 0 until 7) {
                            if (currentDayNumber <= daysInMonth) {
                                val dayNum = currentDayNumber
                                val currentCellDateStr = "2026-06-%02d".format(dayNum)
                                
                                val hasHoliday = holidays.any { it.date == currentCellDateStr }
                                val hasNotes = viewModel.calendarNotes.value.any { it.date == currentCellDateStr }
                                val isSelected = dayNum == selectedDay

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(3.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                dayNum == 29 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) // highlight today
                                                else -> Color.White.copy(alpha = 0.02f)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.04f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedDay = dayNum }
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected || dayNum == 29) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                        
                                        // Indicator Dots
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            if (hasHoliday) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(if (isSelected) Color.Black else Color(0xFFCCFF00))
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                            }
                                            if (hasNotes) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(if (isSelected) Color.Black else Color(0xFFE879F9))
                                                )
                                            }
                                        }
                                    }
                                }
                                currentDayNumber++
                            } else {
                                // Empty spacer for cells beyond June 30
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // --- SELECTED DAY ACTIONS / DETAIL CARD ---
        Text(
            text = "AGENDA FOR %02d JUNE 2026".format(selectedDay),
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // --- ASSIGNED WORK SHIFT DISPLAY ---
                val employeeShift = viewModel.teamSchedules.value.find {
                    it.employeeName == viewModel.currentUserName.value && it.date == selectedDateStr
                }
                
                if (employeeShift != null) {
                    val shiftName = employeeShift.shiftName
                    val shiftColor = when (shiftName) {
                        "Manila Dev Shift" -> Color(0xFF10B981) // Green
                        "Indore Day Flex" -> Color(0xFF00E5FF) // Cyan
                        "Night Ops" -> Color(0xFF8B5CF6) // Purple
                        else -> Color(0xFF9CA3AF) // Gray
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(shiftColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, shiftColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = shiftColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ASSIGNED SHIFT: ${shiftName.uppercase()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = shiftColor
                                )
                                Text(
                                    text = when (shiftName) {
                                        "Manila Dev Shift" -> "09:00 AM - 06:00 PM • Standard Compliance Sync"
                                        "Indore Day Flex" -> "08:00 AM - 05:00 PM • Flex Hours Rule"
                                        "Night Ops" -> "09:00 PM - 06:00 AM • Night Differential Bonus"
                                        else -> "Rest Day • Off-Duty Weekend Policy"
                                    },
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // 1. Holiday details if exists
                if (holidayForSelectedDay != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF00E5FF).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (holidayForSelectedDay.isNational) Icons.Default.Flag else Icons.Default.Celebration,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = holidayForSelectedDay.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Regional Gazette: ${holidayForSelectedDay.description}",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 2. Custom Notes / Events List
                if (notesForSelectedDay.isEmpty() && holidayForSelectedDay == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No notes or events scheduled for this day.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    notesForSelectedDay.forEach { note ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = note.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = note.time,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (note.description.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = note.description,
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                    if (note.syncWithGoogleCalendar) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = Color(0xFF4285F4),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Google Calendar Reminders Active",
                                                fontSize = 9.sp,
                                                color = Color(0xFF4285F4),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteCalendarNote(note.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete note",
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- ADD NEW EVENT / NOTE CARD ---
        Text(
            text = "SCHEDULE AN INDORE CHRONO REMINDER",
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Add Event Note",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    placeholder = { Text("Event Name (e.g., Client sync, Team review)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = noteDesc,
                    onValueChange = { noteDesc = it },
                    placeholder = { Text("Details & description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val times = listOf("09:00 AM", "12:00 PM", "02:30 PM", "05:00 PM")
                    times.forEach { t ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (noteTime == t) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.03f))
                                .border(1.dp, if (noteTime == t) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .clickable { noteTime = t }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (noteTime == t) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = syncGoogleCalendar,
                        onCheckedChange = { syncGoogleCalendar = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Notify Me Google Calendar Style",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Triggers an instant simulated notification banner",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (noteTitle.isBlank()) {
                            Toast.makeText(context, "Please enter an event name!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addCalendarNote(
                                date = selectedDateStr,
                                title = noteTitle,
                                description = noteDesc,
                                time = noteTime,
                                sync = syncGoogleCalendar
                            )
                            Toast.makeText(context, "Reminder scheduled for June $selectedDay!", Toast.LENGTH_SHORT).show()
                            noteTitle = ""
                            noteDesc = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Schedule Indore Reminder", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

// ---------------------- SCREEN 5: SETTINGS / CONFIGS ----------------------
@Composable
fun SettingsScreen(
    viewModel: TimeTrackerViewModel,
    config: ShiftConfigEntity,
    onSaveConfig: (ShiftConfigEntity) -> Unit,
    isOffline: Boolean,
    onOfflineToggle: (Boolean) -> Unit,
    onClearAll: () -> Unit,
    userRole: String
) {
    var employeeName by remember { mutableStateOf(config.employeeName) }
    var shiftHours by remember { mutableStateOf(config.shiftDurationHours.toString()) }
    var lunchMins by remember { mutableStateOf(config.lunchDurationMinutes.toString()) }
    var breakMins by remember { mutableStateOf(config.breakDurationMinutes.toString()) }
    var payRate by remember { mutableStateOf(config.hourlyRate.toString()) }
    var remoteGps by remember { mutableStateOf(config.isRemoteVerificationEnabled) }

    var isWipeConfirmOpen by remember { mutableStateOf(false) }

    val isAuthorized = userRole == "ADMIN_HR"

    if (isWipeConfirmOpen) {
        AlertDialog(
            onDismissRequest = { isWipeConfirmOpen = false },
            title = { Text("Purge Database?", color = Color.White) },
            text = { Text("Are you sure you want to delete all work sessions? This is completely irreversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        isWipeConfirmOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Purge Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isWipeConfirmOpen = false }) {
                    Text("Cancel", color = Color(0xFFCCFF00))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // LIQUID GLASS THEME STUDIO (Comfort-optimized Apple Glass UI Editor)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LIQUID GLASS THEME STUDIO",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Transform your Shift HR interface with Apple-inspired fluid glass aesthetics. Comfort-optimized.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable row of Apple Glass Themes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    com.example.ui.theme.LiquidThemeRegistry.allThemes.forEach { theme ->
                        val isSelected = theme.name == viewModel.selectedTheme.value
                        
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(theme.bgGradientStart, theme.bgGradientEnd)
                                    )
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) theme.primaryAccent else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    viewModel.selectedTheme.value = theme.name
                                }
                                .padding(12.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Draw a miniature glass pill inside the theme preview
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .background(theme.cardSurface, RoundedCornerShape(8.dp))
                                        .border(1.dp, theme.cardBorder, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(theme.primaryAccent))
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(theme.secondaryAccent))
                                    }
                                }

                                Text(
                                    text = theme.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                
                                Text(
                                    text = if (isSelected) "ACTIVE" else "SELECT",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) theme.primaryAccent else Color.White.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Warning/Alert block if not Admin/HR
        if (!isAuthorized) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1D22)),
                border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Viewing Profile Only. Admin/HR role is required to modify pay scales or shift goals.",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }

        // Profile Identity block
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("EMPLOYEE PROFILE IDENTITY", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFFCCFF00), letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This sets the target name used for attendance logs in this administrative testing environment. Logged-in regular employees are automatically synced with their own names.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = employeeName,
                    onValueChange = { employeeName = it },
                    label = { Text("Full Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                    enabled = isAuthorized,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFCCFF00),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f)
                    )
                )
            }
        }

        // Config block
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text("SHIFT RULE THRESHOLDS", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFFCCFF00), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Operational guardrails used to evaluate attendance compliance, validate active working hours, trigger lunch/break alarms, and calculate base wages.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                OutlinedTextField(
                    value = shiftHours,
                    onValueChange = { shiftHours = it },
                    label = { Text("Shift Target Duration (Hours)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("shift_hours_input"),
                    enabled = isAuthorized,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFCCFF00),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f)
                    )
                )

                OutlinedTextField(
                    value = lunchMins,
                    onValueChange = { lunchMins = it },
                    label = { Text("Mandatory Lunch Warning (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("lunch_mins_input"),
                    enabled = isAuthorized,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFCCFF00),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f)
                    )
                )

                OutlinedTextField(
                    value = breakMins,
                    onValueChange = { breakMins = it },
                    label = { Text("Short Break Overage Alert (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("break_mins_input"),
                    enabled = isAuthorized,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFCCFF00),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f)
                    )
                )

                OutlinedTextField(
                    value = payRate,
                    onValueChange = { payRate = it },
                    label = { Text("Base Wage Hourly Pay Rate (${viewModel.getCurrencySymbol()})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pay_rate_input"),
                    enabled = isAuthorized,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFCCFF00),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f)
                    )
                )
            }
        }

        // DAILY SCHEDULE CONFIGURATION (Only Supervisor & Department Manager can adjust)
        val isScheduleAuthorized = userRole == "SUPERVISOR" || userRole == "MANAGER"
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DAILY SCHEDULE RECURRENCE",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = Color(0xFFCCFF00),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Configure work schedule cycle limits (Weekly, 15 Days, or Monthly). Available to Supervisors and Department Managers only.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Weekly", "15 Days", "Monthly").forEach { option ->
                        val isSelected = viewModel.currentScheduleType.value == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFCCFF00).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                .border(1.dp, if (isSelected) Color(0xFFCCFF00) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isScheduleAuthorized) {
                                        viewModel.currentScheduleType.value = option
                                        viewModel.addAuditLog(viewModel.currentUserName.value, "Adjusted daily schedule recurrence to $option")
                                        viewModel.addNotification("Schedule Update", "Daily schedule cycle set to $option successfully.", isAlert = false)
                                    } else {
                                        viewModel.addNotification("Unauthorized", "Only Supervisor and Department Manager can adjust Daily Schedule.", isAlert = true)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                color = if (isSelected) Color(0xFFCCFF00) else Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!isScheduleAuthorized) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "🔒 Locked: Log in as a Supervisor or Department Manager to edit.",
                        color = Color(0xFFF43F5E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "⚡ Authorized: You are adjusting the ${userRole.lowercase()} schedule.",
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // REGIONAL CURRENCY PREFERENCE (Add Philippines Peso not only Dollar)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "REGIONAL CURRENCY",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = Color(0xFFCCFF00),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Toggle currency preferences between US Dollar ($) and Philippines Peso (₱) for payroll, wage calculations, and claims.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("USD" to "US Dollar ($)", "PHP" to "Philippines Peso (₱)").forEach { (code, label) ->
                        val isSelected = viewModel.selectedCurrency.value == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFCCFF00).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                .border(1.dp, if (isSelected) Color(0xFFCCFF00) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.selectedCurrency.value = code
                                    viewModel.addNotification("Currency Changed", "Currency display set to $code.", isAlert = false)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFFCCFF00) else Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SIMULATION & HARDWARE TELEMETRY", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFFCCFF00), letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simulated Offline Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        Text("Lose server connection; caches punches to local room database.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                    Switch(
                        checked = isOffline,
                        onCheckedChange = onOfflineToggle,
                        modifier = Modifier.testTag("sim_offline_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFCCFF00))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mock GPS Co-Working Verifications", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        Text("Record simulated mock coordinates to verify virtual presence.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                    Switch(
                        checked = remoteGps,
                        onCheckedChange = { if (isAuthorized) remoteGps = it },
                        modifier = Modifier.testTag("remote_gps_switch"),
                        enabled = isAuthorized,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFCCFF00))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // --- GEOFENCE ADJUSTMENTS ---
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Geofence Security Radius", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isAuthorized) Color.White else Color.White.copy(alpha = 0.5f))
                        Text("${viewModel.geofenceRadius.value.toInt()} meters", fontSize = 12.sp, color = if (isAuthorized) Color(0xFFCCFF00) else Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    }
                    Text(if (isAuthorized) "Defines boundary around authorized coordinate hubs." else "Defines boundary around authorized coordinate hubs (Only Admin/HR can adjust).", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    Slider(
                        value = viewModel.geofenceRadius.value,
                        onValueChange = { if (isAuthorized) viewModel.geofenceRadius.value = it },
                        valueRange = 50f..500f,
                        enabled = isAuthorized,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isAuthorized) Color(0xFFCCFF00) else Color.White.copy(alpha = 0.2f),
                            activeTrackColor = if (isAuthorized) Color(0xFFCCFF00) else Color.White.copy(alpha = 0.2f),
                            disabledThumbColor = Color.White.copy(alpha = 0.2f),
                            disabledActiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Simulated Employee Distance", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isAuthorized) Color.White else Color.White.copy(alpha = 0.5f))
                        Text("${viewModel.simulatedDistance.value.toInt()} meters from hub", fontSize = 12.sp, color = if (!isAuthorized) Color.White.copy(alpha = 0.5f) else if (viewModel.simulatedDistance.value > viewModel.geofenceRadius.value) Color(0xFFF43F5E) else Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                    Text(if (isAuthorized) "Drag outward to test Geofence Out-Of-Bounds error logic." else "Drag outward to test Geofence Out-Of-Bounds error logic (Only Admin/HR can adjust).", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    Slider(
                        value = viewModel.simulatedDistance.value,
                        onValueChange = { if (isAuthorized) viewModel.simulatedDistance.value = it },
                        valueRange = 0f..400f,
                        enabled = isAuthorized,
                        colors = SliderDefaults.colors(
                            thumbColor = if (viewModel.simulatedDistance.value > viewModel.geofenceRadius.value) Color(0xFFF43F5E) else Color(0xFF10B981),
                            activeTrackColor = if (viewModel.simulatedDistance.value > viewModel.geofenceRadius.value) Color(0xFFF43F5E) else Color(0xFF10B981),
                            disabledThumbColor = Color.White.copy(alpha = 0.2f),
                            disabledActiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // --- ONE REGISTERED DEVICE LOCK ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("One Registered Device Matcher", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        Text("Authorized ID: ${viewModel.registeredDeviceId.value}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                    Switch(
                        checked = viewModel.isDeviceVerificationEnabled.value,
                        onCheckedChange = { viewModel.isDeviceVerificationEnabled.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFCCFF00))
                    )
                }

                if (viewModel.isDeviceVerificationEnabled.value) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Current Terminal hardware identity", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                            Text(viewModel.currentSimulatedDevice.value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (viewModel.currentSimulatedDevice.value == viewModel.registeredDeviceId.value) Color(0xFF10B981) else Color(0xFFF43F5E))
                        }
                        Button(
                            onClick = {
                                if (viewModel.currentSimulatedDevice.value == viewModel.registeredDeviceId.value) {
                                    viewModel.currentSimulatedDevice.value = "UNAPPROVED_MOCK_HARDWARE_88X"
                                } else {
                                    viewModel.currentSimulatedDevice.value = viewModel.registeredDeviceId.value
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text(if (viewModel.currentSimulatedDevice.value == viewModel.registeredDeviceId.value) "Simulate Spoof" else "Reset to Match", fontSize = 9.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // --- FACE RECOGNITION BIOMETRICS ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Secure Face Recognition Biometrics", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        Text("Verify vector points before completing clocks.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                    Switch(
                        checked = viewModel.isFaceRecognitionEnabled.value,
                        onCheckedChange = { viewModel.isFaceRecognitionEnabled.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFCCFF00))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // --- LIVE FIELD TRACKING ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live Field Location Tracking", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        Text("Push continuous field coords (lat: ${String.format("%.4f", viewModel.liveFieldLat.value)}, lng: ${String.format("%.4f", viewModel.liveFieldLng.value)})", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                    Switch(
                        checked = viewModel.isLiveLocationTrackingActive.value,
                        onCheckedChange = { viewModel.isLiveLocationTrackingActive.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFCCFF00))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // --- SUSPICIOUS ATTENDANCE SCANS ---
                Text("SUSPICIOUS ATTENDANCE INTEGRITY DETECTORS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFF43F5E), letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mock GPS Provider Detection", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        Text("Raise alarm for software-based location spoofing.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                    Switch(
                        checked = viewModel.isMockGpsActive.value,
                        onCheckedChange = { viewModel.isMockGpsActive.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF43F5E))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Root / Jailbreak Shield Integrity", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        Text("Fail check entirely if custom binary partitions detected.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                    Switch(
                        checked = viewModel.isRootedActive.value,
                        onCheckedChange = { viewModel.isRootedActive.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF43F5E))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Impossible Travel Time Window", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        Text("Flag logs occurring across 2 remote hubs instantly.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                    Switch(
                        checked = viewModel.isImpossibleTravelTriggered.value,
                        onCheckedChange = { viewModel.isImpossibleTravelTriggered.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF43F5E))
                    )
                }
            }
        }

        if (isAuthorized) {
            Button(
                onClick = {
                    val updated = config.copy(
                        employeeName = employeeName.ifBlank { "Sarah Jenkins" },
                        shiftDurationHours = shiftHours.toFloatOrNull() ?: 8.0f,
                        lunchDurationMinutes = lunchMins.toIntOrNull() ?: 60,
                        breakDurationMinutes = breakMins.toIntOrNull() ?: 30,
                        hourlyRate = payRate.toDoubleOrNull() ?: 25.0,
                        isRemoteVerificationEnabled = remoteGps
                    )
                    onSaveConfig(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_settings_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCCFF00)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("SAVE Hub Parameters", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 14.sp)
            }

            Button(
                onClick = { isWipeConfirmOpen = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF43F5E).copy(alpha = 0.12f),
                    contentColor = Color(0xFFF43F5E)
                ),
                border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("wipe_db_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("PURGE HUB DATABASES", fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// ---------------------- MODALS & DIALOGS ----------------------

@Composable
fun RetroactiveEditDialog(
    log: TimeLogEntity,
    onDismiss: () -> Unit,
    onSave: (TimeLogEntity) -> Unit
) {
    var inHours by remember { mutableStateOf(getHour(log.timeIn).toString()) }
    var inMins by remember { mutableStateOf(getMin(log.timeIn).toString()) }
    var outHours by remember { mutableStateOf(getHour(log.timeOut).toString()) }
    var outMins by remember { mutableStateOf(getMin(log.timeOut).toString()) }
    var isApproved by remember { mutableStateOf(log.isApproved) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Edit Lap: ${log.employeeName}",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color.White
                )

                Text(
                    text = "Manually override checkpoint timestamps below.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Text("TIME IN CHECKS (24H)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFCCFF00))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inHours,
                        onValueChange = { inHours = it },
                        label = { Text("Hour") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_in_hour"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFCCFF00))
                    )
                    OutlinedTextField(
                        value = inMins,
                        onValueChange = { inMins = it },
                        label = { Text("Minute") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_in_min"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFCCFF00))
                    )
                }

                Text("TIME OUT CHECKS (24H)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFCCFF00))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = outHours,
                        onValueChange = { outHours = it },
                        label = { Text("Hour") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_out_hour"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFCCFF00))
                    )
                    OutlinedTextField(
                        value = outMins,
                        onValueChange = { outMins = it },
                        label = { Text("Minute") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_out_min"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFCCFF00))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DECISION STATE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val decisionStates = listOf("PENDING", "APPROVED", "REJECTED")
                        decisionStates.forEach { state ->
                            FilterChip(
                                selected = isApproved == state,
                                onClick = { isApproved = state },
                                label = { Text(state, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.6f)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newIn = rebuildTimestamp(log.timeIn ?: System.currentTimeMillis(), inHours.toIntOrNull() ?: 9, inMins.toIntOrNull() ?: 0)
                            val newOut = if (log.timeOut != null || outHours.isNotEmpty()) {
                                rebuildTimestamp(log.timeOut ?: System.currentTimeMillis(), outHours.toIntOrNull() ?: 17, outMins.toIntOrNull() ?: 30)
                            } else null

                            val updated = log.copy(
                                timeIn = newIn,
                                timeOut = newOut,
                                isApproved = isApproved
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCCFF00)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_retroactive_punch")
                    ) {
                        Text("APPLY OVERRIDES", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftHRLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3B82F6), Color(0xFF14B8A6), Color(0x0014B8A6)),
                    center = Offset(size.width * 0.65f, size.height * 0.35f),
                    radius = size.width * 0.7f
                ),
                center = Offset(size.width * 0.65f, size.height * 0.35f),
                radius = size.width * 0.7f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF22C55E), Color(0xFF15803D), Color(0x0015803D)),
                    center = Offset(size.width * 0.25f, size.height * 0.8f),
                    radius = size.width * 0.45f
                ),
                center = Offset(size.width * 0.25f, size.height * 0.8f),
                radius = size.width * 0.45f
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(
                text = "S",
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 0.dp)
            )
            Text(
                text = "hift",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6),
                            Color(0xFFEC4899),
                            Color(0xFFF43F5E)
                        )
                    )
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
fun MiniShiftLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFFE2E8F0), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3B82F6), Color(0xFF14B8A6), Color(0x0014B8A6)),
                    center = Offset(size.width * 0.65f, size.height * 0.35f),
                    radius = size.width * 0.7f
                ),
                center = Offset(size.width * 0.65f, size.height * 0.35f),
                radius = size.width * 0.7f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF22C55E), Color(0xFF15803D), Color(0x0015803D)),
                    center = Offset(size.width * 0.25f, size.height * 0.8f),
                    radius = size.width * 0.45f
                ),
                center = Offset(size.width * 0.25f, size.height * 0.8f),
                radius = size.width * 0.45f
            )
        }
        Text(
            text = "S",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
fun LogDetailDialog(
    log: TimeLogEntity,
    onDismiss: () -> Unit,
    onEdit: (TimeLogEntity) -> Unit,
    currencySymbol: String = "$",
    userRole: String = "EMPLOYEE"
) {
    val df = DecimalFormat("#.##")
    val totalMillis = if (log.timeIn != null && log.timeOut != null) {
        val total = log.timeOut - log.timeIn
        val lunch = if (log.lunchOut != null && log.lunchIn != null) log.lunchIn - log.lunchOut else 0L
        val brk = if (log.breakOut != null && log.breakIn != null) log.breakIn - log.breakOut else 0L
        total - lunch - brk
    } else 0L
    val hours = totalMillis.toDouble() / 3600000.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = log.employeeName, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                        Text(text = "Shift Date: ${log.date}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Text("CHRONO CAPTURED MILESTONES", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color(0xFFCCFF00), letterSpacing = 1.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("TIME IN", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                        Text(formatTimestamp(log.timeIn), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                    Column {
                        Text("LUNCH DURATION", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                        val lOutStr = formatTimestamp(log.lunchOut)
                        val lInStr = formatTimestamp(log.lunchIn)
                        Text("$lOutStr ➔ $lInStr", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("SHORT BREAK", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                        val bOutStr = formatTimestamp(log.breakOut)
                        val bInStr = formatTimestamp(log.breakIn)
                        Text("$bOutStr ➔ $bInStr", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                    Column {
                        Text("TIME OUT", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                        Text(formatTimestamp(log.timeOut), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("HOURLY PAY SCALE", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                        Text("${currencySymbol}${log.hourlyRate}/HR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text("COMPUTED WORKING", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                        Text("${df.format(hours)} HRS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFCCFF00))
                    }
                }

                if (log.rejectionReason != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF43F5E).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("HR AUDIT REFUSAL REASON:", fontWeight = FontWeight.Black, fontSize = 9.sp, color = Color(0xFFFFA5B5), letterSpacing = 1.sp)
                            Text(log.rejectionReason, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                if (log.gpsLocationName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("HARDWARE TELEMETRY LOCATION", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color(0xFFCCFF00), letterSpacing = 1.sp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF00E5FF).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Simulated Location Verified",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "${log.gpsLocationName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00E5FF)
                                )
                                Text(
                                    text = "Coordinates: ${log.gpsLatitude}, ${log.gpsLongitude}",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val isTargetSupervisorOrManager = log.employeeName.contains("Director", ignoreCase = true) || 
                        log.employeeName.contains("Manager", ignoreCase = true) || 
                        log.employeeName.contains("Supervisor", ignoreCase = true) || 
                        log.employeeName == "Robert Chen" || 
                        log.employeeName == "Anjali Sharma" || 
                        log.employeeName == "Aditya Joshi" || 
                        log.employeeName == "Aditya Joshi (Director)"

                val isAuthorized = if (isTargetSupervisorOrManager) {
                    userRole == "ADMIN_HR"
                } else {
                    userRole == "ADMIN_HR" || userRole == "MANAGER" || userRole == "SUPERVISOR"
                }
                if (isAuthorized) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onEdit(log) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("dialog_edit_log_action")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Audit", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCCFF00)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close Details", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCCFF00)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Details", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------------- UTILITIES ----------------------

fun getHour(time: Long?): Int {
    if (time == null) return 9
    val cal = Calendar.getInstance()
    cal.timeInMillis = time
    return cal.get(Calendar.HOUR_OF_DAY)
}

fun getMin(time: Long?): Int {
    if (time == null) return 0
    val cal = Calendar.getInstance()
    cal.timeInMillis = time
    return cal.get(Calendar.MINUTE)
}

fun rebuildTimestamp(base: Long, hour: Int, min: Int): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = base
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, min)
    cal.set(Calendar.SECOND, 0)
    return cal.timeInMillis
}

fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return "—"
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun exportLogsToCSV(logs: List<TimeLogEntity>, context: android.content.Context) {
    val csv = StringBuilder()
    csv.append("ID,Date,Employee,Time In,Lunch Out,Lunch In,Break Out,Break In,Time Out,Hourly Rate,Status,Sync Status\n")
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    logs.forEach { log ->
        val tIn = log.timeIn?.let { sdf.format(Date(it)) } ?: ""
        val lOut = log.lunchOut?.let { sdf.format(Date(it)) } ?: ""
        val lIn = log.lunchIn?.let { sdf.format(Date(it)) } ?: ""
        val bOut = log.breakOut?.let { sdf.format(Date(it)) } ?: ""
        val bIn = log.breakIn?.let { sdf.format(Date(it)) } ?: ""
        val tOut = log.timeOut?.let { sdf.format(Date(it)) } ?: ""
        csv.append("${log.id},${log.date},${log.employeeName},$tIn,$lOut,$lIn,$bOut,$bIn,$tOut,${log.hourlyRate},${log.isApproved},${if (log.isSynced) "Synced" else "Local Only"}\n")
    }

    try {
        val file = java.io.File(context.cacheDir, "Shift_HR_Ledger.csv")
        java.io.FileOutputStream(file).use {
            it.write(csv.toString().toByteArray())
        }

        val uri: android.net.Uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )

        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("HR Timesheet CSV", csv.toString())
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "Spreadsheet CSV copied & ready to share!", Toast.LENGTH_SHORT).show()

        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/comma-separated-values"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Shift HR Timesheet Ledger")
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Timesheet Spreadsheet (Excel Form)"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error converting to spreadsheet: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// ---------------------- COMPLIANCE DASHBOARD PLOTS & CHATS ----------------------

@Composable
fun ProductivityLineChart(
    records: List<ProductivityRecord>,
    employeeName: String,
    modifier: Modifier = Modifier
) {
    var hoveredIndex by remember { mutableStateOf(-1) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAILY PRODUCTIVITY PROFILE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "$employeeName - Weekly Compliance Profile",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                
                val validScores = records.filter { it.score > 0 }
                val avgScore = if (validScores.isNotEmpty()) validScores.map { it.score }.average().toInt() else 0
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "AVG: $avgScore%",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(records) {
                        detectTapGestures { offset ->
                            val width = size.width.toFloat()
                            val stepX = width / 6f
                            val clickedIndex = (offset.x / stepX + 0.5f).toInt().coerceIn(0, 6)
                            hoveredIndex = if (hoveredIndex == clickedIndex) -1 else clickedIndex
                        }
                    }
            ) {
                val accentColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = h - (h / gridLines) * i
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 2f
                        )
                    }
                    
                    val points = records.mapIndexed { index, rec ->
                        val x = (w / 6f) * index
                        val y = h - (h * (rec.score / 100f))
                        Offset(x, y)
                    }
                    
                    val fillPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, h)
                        points.forEachIndexed { idx, pt ->
                            if (idx == 0) {
                                lineTo(pt.x, pt.y)
                            } else {
                                val prevPt = points[idx - 1]
                                cubicTo(
                                    prevPt.x + (pt.x - prevPt.x) / 2f, prevPt.y,
                                    prevPt.x + (pt.x - prevPt.x) / 2f, pt.y,
                                    pt.x, pt.y
                                )
                            }
                        }
                        lineTo(w, h)
                        close()
                    }
                    
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f,
                            endY = h
                        )
                    )
                    
                    val strokePath = androidx.compose.ui.graphics.Path().apply {
                        points.forEachIndexed { idx, pt ->
                            if (idx == 0) {
                                moveTo(pt.x, pt.y)
                            } else {
                                val prevPt = points[idx - 1]
                                cubicTo(
                                    prevPt.x + (pt.x - prevPt.x) / 2f, prevPt.y,
                                    prevPt.x + (pt.x - prevPt.x) / 2f, pt.y,
                                    pt.x, pt.y
                                )
                            }
                        }
                    }
                    
                    drawPath(
                        path = strokePath,
                        color = accentColor,
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                    
                    points.forEachIndexed { idx, pt ->
                        val rec = records[idx]
                        if (rec.score > 0) {
                            drawCircle(
                                color = Color(0xFF090D16),
                                radius = 10f,
                                center = pt
                            )
                            drawCircle(
                                color = if (idx == hoveredIndex) secondaryColor else accentColor,
                                radius = 6f,
                                center = pt
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                records.forEachIndexed { idx, rec ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Text(
                            text = rec.day,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (idx == hoveredIndex) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.4f)
                        )
                        Text(
                            text = if (rec.score > 0) "${rec.score}%" else "—",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = if (rec.score > 0) Color.White else Color.White.copy(alpha = 0.15f)
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = hoveredIndex != -1,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (hoveredIndex != -1) {
                    val activeRec = records[hoveredIndex]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
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
                                        .background(MaterialTheme.colorScheme.secondary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Compliance Detail: ${activeRec.day}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Score: ${activeRec.score}% | Logged: ${activeRec.hours}h",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatHubScreen(
    viewModel: TimeTrackerViewModel
) {
    val contacts = listOf(
        "Sarah Jenkins" to "Employee (Dev)",
        "Marcus Aurelius (HR Intern)" to "HR Intern",
        "Robert Chen" to "Supervisor (PM)",
        "Anjali Sharma" to "General Manager",
        "Aditya Joshi (Director)" to "Director",
        "Michael Vance" to "Co-worker (QA)",
        "All Employees" to "Global Broadcast 📢"
    )

    val currentUserName = viewModel.currentUserName.value
    var contactSearchQuery by remember { mutableStateOf("") }
    var showRequestCoverDialog by remember { mutableStateOf(false) }

    val filteredContacts = contacts.filter { contact ->
        contact.first != currentUserName && (
            contactSearchQuery.isBlank() ||
            contact.first.contains(contactSearchQuery, ignoreCase = true) ||
            contact.second.contains(contactSearchQuery, ignoreCase = true)
        )
    }
    
    var activeRecipientName = viewModel.activeRecipient.value
    if (activeRecipientName == currentUserName) {
        val firstContact = filteredContacts.firstOrNull()?.first ?: "All Employees"
        viewModel.activeRecipient.value = firstContact
        activeRecipientName = firstContact
    }
    
    val activeRole = contacts.find { it.first == activeRecipientName }?.second ?: "Compliance Partner"
    var messageText by remember { mutableStateOf("") }
    
    val allMessages = viewModel.messagesList.value
    val chatMessages = allMessages.filter { msg ->
        if (activeRecipientName == "All Employees") {
            msg.recipient == "All Employees"
        } else {
            (msg.sender == currentUserName && msg.recipient == activeRecipientName) ||
            (msg.sender == activeRecipientName && msg.recipient == currentUserName)
        }
    }
    
    Row(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier
                .width(145.dp)
                .fillMaxHeight()
                .padding(end = 8.dp)
        ) {
            Text(
                text = "CHANNELS",
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            OutlinedTextField(
                value = contactSearchQuery,
                onValueChange = { contactSearchQuery = it },
                placeholder = { Text("Search...", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(48.dp)
                    .testTag("chat_contact_search"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                    focusedContainerColor = Color.White.copy(alpha = 0.02f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.01f)
                )
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredContacts) { (name, role) ->
                    val isSelected = activeRecipientName == name
                    val isSpecial = name == "All Employees"
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else Color.White.copy(alpha = 0.02f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.activeRecipient.value = name }
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSpecial) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = name.substringBefore(" ("),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = role,
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.4f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.08f))
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeRecipientName == "All Employees") Icons.Default.Campaign else Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = activeRecipientName,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text(
                            text = activeRole,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }

                    if (activeRecipientName != "All Employees") {
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = { showRequestCoverDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp).testTag("request_cover_toolbar_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Request Cover",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            val bdayObj = viewModel.birthdayList.value.find { it.name == activeRecipientName }
            if (bdayObj != null && bdayObj.isToday) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎉", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Today is $activeRecipientName's Birthday! 🎂",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Tap a Quick-Wish below to celebrate them!",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                reverseLayout = false
            ) {
                if (chatMessages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No compliance messages yet.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.3f))
                            }
                        }
                    }
                } else {
                    items(chatMessages) { msg ->
                        val isMe = msg.sender == currentUserName
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(0.85f),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isMe) MaterialTheme.colorScheme.secondary
                                                    else MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 0.dp,
                                                bottomEnd = if (isMe) 0.dp else 16.dp
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            if (isMe) Color.Transparent else Color.White.copy(alpha = 0.06f),
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 0.dp,
                                                bottomEnd = if (isMe) 0.dp else 16.dp
                                            )
                                        )
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        if (!isMe && activeRecipientName == "All Employees") {
                                            Text(
                                                text = msg.sender,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = 2.dp)
                                            )
                                        }
                                        if (msg.isSwapRequest) {
                                            Column(modifier = Modifier.width(220.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.SwapHoriz,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "SHIFT COVER REQUEST",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                }

                                                Text(
                                                    text = if (isMe) "You requested coverage for:" else "${msg.sender} requested coverage for:",
                                                    fontSize = 11.sp,
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                        .padding(8.dp)
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = msg.swapDate,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = Color.White
                                                        )
                                                        Text(
                                                            text = msg.swapShiftName,
                                                            fontSize = 10.sp,
                                                            color = Color.White.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                when (msg.swapStatus) {
                                                    "PENDING" -> {
                                                        if (isMe) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                                                    .padding(6.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "⌛ Awaiting coworker accept",
                                                                    color = Color.White.copy(alpha = 0.5f),
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        } else {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                Button(
                                                                    onClick = { viewModel.respondToCoverRequest(msg.id, false) },
                                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                                                                    contentPadding = PaddingValues(vertical = 4.dp),
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    modifier = Modifier.weight(1f).height(28.dp).testTag("decline_cover_btn")
                                                                ) {
                                                                    Text("Decline", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                                Button(
                                                                    onClick = { viewModel.respondToCoverRequest(msg.id, true) },
                                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                                                                    contentPadding = PaddingValues(vertical = 4.dp),
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    modifier = Modifier.weight(1.2f).height(28.dp).testTag("accept_cover_btn")
                                                                ) {
                                                                    Text("Accept Cover", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    "ACCEPTED" -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(Color(0xFFFFA629).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                                .border(1.dp, Color(0xFFFFA629).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                                .padding(6.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "⏳ Accepted by ${msg.swapCoverer}. Awaiting final approval.",
                                                                color = Color(0xFFFFA629),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                    "APPROVED" -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(Color(0xFF00FF88).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                                .border(1.dp, Color(0xFF00FF88).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                                .padding(6.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "✅ Approved! ${msg.swapCoverer} scheduled.",
                                                                color = Color(0xFF00FF88),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                    "REJECTED" -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(Color(0xFFFF5555).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                                .border(1.dp, Color(0xFFFF5555).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                                .padding(6.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "❌ Declined / Rejected",
                                                                color = Color(0xFFFF5555),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = msg.text,
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = msg.timestamp,
                                            fontSize = 8.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            val quickWishes = mutableListOf<String>()
            if (bdayObj != null && bdayObj.isToday) {
                quickWishes.add("🎂 Happy Birthday $activeRecipientName! Sending sweets from Indore hub! 🍬")
                quickWishes.add("🎁 Many happy returns of the day! Have a compliant and productive cycle ahead!")
            } else {
                quickWishes.add("📋 Hi, review and audit my logged hours in the Ledger, please!")
                quickWishes.add("✅ Verification completed, remote workspace syncing checked!")
            }
            
            Text(
                text = "QUICK COMPLIANCE HINTS",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickWishes.forEach { wish ->
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .clickable { messageText = wish }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = wish, fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Compose secure compliance note...", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("chat_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f)
                    )
                )
                
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(activeRecipientName, messageText)
                            messageText = ""
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("chat_send_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (showRequestCoverDialog) {
        val myShifts = viewModel.teamSchedules.value.filter { it.employeeName == currentUserName && it.shiftName != "Off" }
        AlertDialog(
            onDismissRequest = { showRequestCoverDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Shift to Cover", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose one of your scheduled shifts to request coverage from $activeRecipientName.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (myShifts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No scheduled shifts found to cover.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(myShifts) { shift ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.sendCoverRequest(activeRecipientName, shift.date, shift.shiftName)
                                            showRequestCoverDialog = false
                                        }
                                        .testTag("shift_item_${shift.date}"),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(shift.date, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(shift.shiftName, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRequestCoverDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
