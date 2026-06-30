package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.TimeTrackerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlatformGuideScreen(viewModel: TimeTrackerViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var activeGuideTab by remember { mutableStateOf("stepper") } // "stepper" or "diagram"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Tab Selector for Stepper vs Diagram
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeGuideTab == "stepper") NeonGreen else Color.Transparent)
                    .clickable { activeGuideTab = "stepper" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = if (activeGuideTab == "stepper") Color.Black else NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Interactive Guide",
                        color = if (activeGuideTab == "stepper") Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeGuideTab == "diagram") NeonGreen else Color.Transparent)
                    .clickable { activeGuideTab = "diagram" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schema,
                        contentDescription = null,
                        tint = if (activeGuideTab == "diagram") Color.Black else NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Platform Diagram",
                        color = if (activeGuideTab == "diagram") Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedContent(
            targetState = activeGuideTab,
            transitionSpec = {
                slideInHorizontally { width -> if (targetState == "diagram") width else -width } with
                        slideOutHorizontally { width -> if (targetState == "diagram") -width else width }
            }
        ) { targetTab ->
            if (targetTab == "stepper") {
                StepperGuideSection(viewModel, context)
            } else {
                InteractiveDiagramSection(context)
            }
        }
    }
}

@Composable
fun StepperGuideSection(viewModel: TimeTrackerViewModel, context: Context) {
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 5

    // Step contents definition
    val stepTitle = when (currentStep) {
        0 -> "⏱️ Smart Time Tracking & Punching"
        1 -> "👤 Employee Profile & Digital ID"
        2 -> "💼 Self-Service Desk & Documents"
        3 -> "🤖 AI Compliance Assistant"
        4 -> "📊 Shift Spreadsheets & Ledger"
        else -> ""
    }

    val stepDesc = when (currentStep) {
        0 -> "Clock in/out instantly with built-in geofence perimeters, face identity verification, and anti-spoofing countermeasures. Supports full offline punching that syncs as soon as you're back online!"
        1 -> "View and manage allowed personal details (phone number, address, nickname, emergency contacts, profile photo). View your encrypted corporate ID, NFC token passes, and digital work ID badge."
        2 -> "Submit leave balances (vacation, sick), request attendance time corrections, and view reimbursement claims. Securely download and view verified PDF corporate documents."
        3 -> "Ask our real-time AI Compliance helper for shifts advice, automated ledger auditing, regulatory compliance alerts, geofence radius explanations, and instant HR assistance."
        4 -> "A master ledger spreadsheet capturing all clock durations. Easily filter by approval/sync status, search logs, and export reports with accurate overtime, night differential, and basic salaries."
        else -> ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_guide_card"),
        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
        border = BorderStroke(1.dp, BorderGrey)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen Title & Step indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STEP ${currentStep + 1} OF $totalSteps",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    letterSpacing = 1.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(totalSteps) { index ->
                        Box(
                            modifier = Modifier
                                .size(width = if (currentStep == index) 16.dp else 6.dp, height = 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (currentStep == index) NeonGreen else Color.White.copy(alpha = 0.2f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Animation Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (currentStep) {
                    0 -> TimeTrackingAnimation()
                    1 -> DigitalIdAnimation()
                    2 -> SelfServiceAnimation()
                    3 -> AiComplianceAnimation()
                    4 -> SpreadsheetAnimation()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stepTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stepDesc,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Try-Out Simulator Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "LIVE INTERACTIVE SIMULATION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    when (currentStep) {
                        0 -> TryOutPunchSimulator(viewModel, context)
                        1 -> TryOutIdSimulator(viewModel, context)
                        2 -> TryOutSelfServiceSimulator(viewModel, context)
                        3 -> TryOutAiSimulator(viewModel, context)
                        4 -> TryOutSpreadsheetSimulator(viewModel, context)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    TextButton(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowBackIos, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back", fontSize = 12.sp)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (currentStep < totalSteps - 1) {
                    Button(
                        onClick = { currentStep++ },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Next Step", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.currentScreen.value = "saas_hub"
                            Toast.makeText(context, "Welcome aboard Shift HR! Let's get started. 🚀", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Get Started! 🚀", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// ANIMATIONS IMPLEMENTATION (PURE CANVAS/COMPOSABLES)
// ==========================================

@Composable
fun TimeTrackingAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.size(120.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2.5f

        // Draw radar circles
        drawCircle(
            color = Color.Green.copy(alpha = 0.08f),
            radius = radius * scalePulse,
            center = center
        )
        drawCircle(
            color = Color.Green.copy(alpha = 0.15f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw crosshairs
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(center.x - radius * 1.3f, center.y),
            end = Offset(center.x + radius * 1.3f, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(center.x, center.y - radius * 1.3f),
            end = Offset(center.x, center.y + radius * 1.3f),
            strokeWidth = 1.dp.toPx()
        )

        // Draw sweep line
        rotate(rotation, pivot = center) {
            drawLine(
                color = Color.Green,
                start = center,
                end = Offset(center.x, center.y - radius),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(
                color = Color.Green,
                radius = 4.dp.toPx(),
                center = Offset(center.x, center.y - radius)
            )
        }

        // Draw central core clock
        drawCircle(
            color = Color.Black,
            radius = 12.dp.toPx(),
            center = center
        )
        drawCircle(
            color = Color.Green,
            radius = 12.dp.toPx(),
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun DigitalIdAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val scanY by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val neonColor = NeonGreen

    Box(contentAlignment = Alignment.Center) {
        // Holographic ID badge representation
        Card(
            modifier = Modifier
                .size(width = 130.dp, height = 90.dp)
                .graphicsLayer {
                    rotationX = 15f
                    rotationY = -10f
                },
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = BorderStroke(1.5.dp, neonColor)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(neonColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, neonColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Box(modifier = Modifier.size(width = 50.dp, height = 4.dp).background(Color.White))
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(modifier = Modifier.size(width = 30.dp, height = 3.dp).background(neonColor.copy(alpha = 0.6f)))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Box(modifier = Modifier.size(width = 40.dp, height = 3.dp).background(Color.White.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.size(width = 45.dp, height = 3.dp).background(Color.White.copy(alpha = 0.4f)))
                    }
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }

        // Animated laser scan bar
        Canvas(modifier = Modifier.size(width = 150.dp, height = 120.dp)) {
            val yPos = (size.height / 2) + scanY.dp.toPx()
            drawLine(
                color = neonColor,
                start = Offset(0f, yPos),
                end = Offset(size.width, yPos),
                strokeWidth = 2.dp.toPx()
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(neonColor.copy(alpha = 0.3f), Color.Transparent),
                    startY = yPos,
                    endY = yPos + 15.dp.toPx()
                ),
                topLeft = Offset(0f, yPos),
                size = Size(size.width, 15.dp.toPx())
            )
        }
    }
}

@Composable
fun SelfServiceAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val bounceY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leave request slip
        Card(
            modifier = Modifier
                .size(70.dp, 85.dp)
                .offset(y = bounceY.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(12.dp))
                Box(modifier = Modifier.size(width = 40.dp, height = 3.dp).background(Color.White))
                Box(modifier = Modifier.size(width = 30.dp, height = 3.dp).background(Color.White.copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("APPROVED", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                }
            }
        }

        // Folder drawer opening
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = NeonGreen,
            modifier = Modifier
                .size(48.dp)
                .scale(1.1f)
        )
    }
}

@Composable
fun AiComplianceAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(contentAlignment = Alignment.Center) {
        // Glow backdrop
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(NeonGreen.copy(alpha = 0.05f * pulseAlpha), CircleShape)
                .border(1.dp, NeonGreen.copy(alpha = 0.2f * pulseAlpha), CircleShape)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier
                    .size(40.dp)
                    .scale(0.9f + pulseAlpha * 0.15f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(NeonGreen.copy(alpha = if (i == 1) pulseAlpha else 0.4f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun SpreadsheetAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val progressWidth by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(
        modifier = Modifier
            .size(130.dp, 100.dp)
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Table Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.weight(1.5f).height(6.dp).background(NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
            Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
            Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
        }

        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth())

        // Grid Rows
        repeat(3) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1.5f).height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(1.dp)))
                Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(1.dp)))
                
                // Highlighted sync indicator cell
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(if (row == 1) NeonGreen.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(1.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress loader bar mimicking Sync
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressWidth)
                    .background(NeonGreen)
            )
        }
    }
}


// ==========================================
// TRY-OUT LIVE INTERACTIVE SIMULATORS
// ==========================================

@Composable
fun TryOutPunchSimulator(viewModel: TimeTrackerViewModel, context: Context) {
    var isSimulatedClockedIn by remember { mutableStateOf(false) }
    var timerVal by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isSimulatedClockedIn) {
        if (isSimulatedClockedIn) {
            while (true) {
                delay(1000)
                timerVal++
            }
        } else {
            timerVal = 0
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isSimulatedClockedIn) "STATUS: ACTIVE DUTY 🟢" else "STATUS: CLOCKED OUT 🔴",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSimulatedClockedIn) NeonGreen else Color.White.copy(alpha = 0.4f)
                )
                Text(
                    text = if (isSimulatedClockedIn) {
                        val hrs = timerVal / 3600
                        val mins = (timerVal % 3600) / 60
                        val secs = timerVal % 60
                        String.format("Timer: %02d:%02d:%02d", hrs, mins, secs)
                    } else "Simulation Standby",
                    fontSize = 12.sp,
                    color = Color.White
                )
            }

            Button(
                onClick = {
                    isSimulatedClockedIn = !isSimulatedClockedIn
                    val act = if (isSimulatedClockedIn) "Clock In" else "Clock Out"
                    Toast.makeText(context, "$act simulation successful! Biometrics Verified. ⏱️", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isSimulatedClockedIn) Color(0xFFEF4444) else NeonGreen),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = if (isSimulatedClockedIn) "Simulate Clock Out" else "Simulate Clock In",
                    color = if (isSimulatedClockedIn) Color.White else Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TryOutIdSimulator(viewModel: TimeTrackerViewModel, context: Context) {
    var isScanned by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (isScanned) "NFC Handshake Confirmed 🪪 ✅" else "Ready for secure biometric pass terminal...",
            fontSize = 10.sp,
            color = if (isScanned) NeonGreen else Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = {
                isScanned = true
                Toast.makeText(context, "NFC ID scanned! Secure ID: DIGI-ID-7721 verified at main gate. 📡", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, NeonGreen),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simulate NFC Badge Tap 📡", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TryOutSelfServiceSimulator(viewModel: TimeTrackerViewModel, context: Context) {
    var vacationBal by remember { mutableStateOf(15) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Vacation Balance Simulator", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
            Text("$vacationBal Days Available", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
                onClick = {
                    if (vacationBal > 0) {
                        vacationBal--
                        Toast.makeText(context, "1 Day Leave requested! Balance updated. ✈️", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(Icons.Default.Remove, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(12.dp))
            }

            IconButton(
                onClick = {
                    vacationBal++
                },
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun TryOutAiSimulator(viewModel: TimeTrackerViewModel, context: Context) {
    var aiReply by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Frequently Asked Compliance Question:",
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                .clickable {
                    if (!isThinking) {
                        scope.launch {
                            isThinking = true
                            aiReply = "AI Thinking..."
                            delay(1200)
                            aiReply = "According to Shift HR Ledger Rules, overtime calculations start automatically if you exceed the shift limit (standard 8 hrs at 1.5x basic rate). Your registered geofence has a safe radius of 100 meters."
                            isThinking = false
                        }
                    }
                }
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tap: \"How are my overtime rates calculated?\"", fontSize = 11.sp, color = Color.White)
            }
        }

        if (aiReply.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeonGreen.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(aiReply, fontSize = 10.sp, color = NeonGreen)
            }
        }
    }
}

@Composable
fun TryOutSpreadsheetSimulator(viewModel: TimeTrackerViewModel, context: Context) {
    Button(
        onClick = {
            Toast.makeText(context, "Ledger verified! 12 CSV reports aggregated successfully. 📊", Toast.LENGTH_SHORT).show()
        },
        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, NeonGreen),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Perform Compliance Ledger Audit", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}


// ==========================================
// INTERACTIVE PLATFORM FLOWCHART & ARCHITECTURE
// ==========================================

@Composable
fun InteractiveDiagramSection(context: Context) {
    var selectedNode by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "SHIFT HR PLATFORM ARCHITECTURE",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = NeonGreen,
            letterSpacing = 1.5.sp
            )
        Text(
            "Interactive System Audit & Compliance Flowchart. Tap nodes to investigate audit steps.",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Flowchart Layout drawing lines & nodes
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Node 1
            DiagramNode(
                id = "trust",
                label = "1. CLIENT DEVICE TRUST",
                desc = "MDM hardware binding & jailbreak check",
                icon = Icons.Default.Security,
                isSelected = selectedNode == "trust",
                onClick = { selectedNode = "trust" }
            )

            // Connection Arrow 1
            FlowArrow()

            // Node 2
            DiagramNode(
                id = "biometrics",
                label = "2. BIOMETRIC FACE LIVENESS",
                desc = "Liveness checking & biometric verification",
                icon = Icons.Default.Face,
                isSelected = selectedNode == "biometrics",
                onClick = { selectedNode = "biometrics" }
            )

            // Connection Arrow 2
            FlowArrow()

            // Node 3
            DiagramNode(
                id = "geofence",
                label = "3. GPS GEOFENCE AUDITING",
                desc = "Real-time perimeter coordinate bounding",
                icon = Icons.Default.MyLocation,
                isSelected = selectedNode == "geofence",
                onClick = { selectedNode = "geofence" }
            )

            // Connection Arrow 3
            FlowArrow()

            // Node 4
            DiagramNode(
                id = "ledger",
                label = "4. LEDGER SYNC & PAYROLL EXPORT",
                desc = "Calculates basic, night-diff, overtime & logs",
                icon = Icons.Default.Analytics,
                isSelected = selectedNode == "ledger",
                onClick = { selectedNode = "ledger" }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Popover Details panel based on selectedNode
        AnimatedVisibility(
            visible = selectedNode != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            if (selectedNode != null) {
                val nodeDetails = when (selectedNode) {
                    "trust" -> Triple(
                        "Client Device Trust Gateway",
                        "The system binds a secure MDM identifier token on initial login. Every check-in/out action validates that the device is approved and has not been rooted/compromised, immediately blocking spoofed environments.",
                        "API Guard: SecureHardwareIDCheck / Mock COORDINATES BLOCKED"
                    )
                    "biometrics" -> Triple(
                        "Real-time Liveness Biometrics Match",
                        "Leverages edge computing camera frames to execute instant biometric checks. Confirms that the physical face matches the registered profile parameters in high fidelity, preventing visual image fraud.",
                        "Core Validator: FaceIdentityMatcher / LivenessCheck"
                    )
                    "geofence" -> Triple(
                        "GPS Geofence Auditor",
                        "Triggers sub-second geodesic audits checking proximity to corporate branches (e.g. Indore Hub/Manila Hub). Blocks the action immediately if the employee falls outside the 100-meter safety radius.",
                        "Radius Controller: GeodistanceValidator / GeofenceShield"
                    )
                    "ledger" -> Triple(
                        "Secured Payroll & Compliance Ledger",
                        "Combines shifts configurations, worked hours, custom lunch breaks, and holidays calendar to automatically compute exact earnings (including 1.5x Standard Overtime rates and Night-Shift differentials). Reports are permanently compiled.",
                        "Payroll Core: LedgerExporter / OvertimeCalculator"
                    )
                    else -> Triple("", "", "")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NeonGreen.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, NeonGreen)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(nodeDetails.first, color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(nodeDetails.second, color = Color.White, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("SYSTEM SERVICE: ${nodeDetails.third}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun DiagramNode(
    id: String,
    label: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                RoundedCornerShape(12.dp)
            )
            .border(
                1.5.dp,
                if (isSelected) NeonGreen else BorderGrey,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(if (isSelected) NeonGreen else Color.White.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.Black else NeonGreen,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) NeonGreen else Color.White)
            Text(desc, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
        }
        Icon(
            imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = if (isSelected) NeonGreen else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun FlowArrow() {
    val infiniteTransition = rememberInfiniteTransition()
    val movingDashY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val neonColor = NeonGreen

    Canvas(
        modifier = Modifier
            .height(20.dp)
            .width(10.dp)
    ) {
        val arrowWidth = size.width
        val arrowHeight = size.height

        // Draw central dotted arrow line
        drawLine(
            color = neonColor.copy(alpha = 0.4f),
            start = Offset(arrowWidth / 2, 0f),
            end = Offset(arrowWidth / 2, arrowHeight),
            strokeWidth = 2.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                movingDashY.dp.toPx()
            )
        )

        // Draw small arrowhead at bottom
        val path = Path().apply {
            moveTo(arrowWidth / 2, arrowHeight)
            lineTo(0f, arrowHeight - 6.dp.toPx())
            lineTo(arrowWidth, arrowHeight - 6.dp.toPx())
            close()
        }
        drawPath(path = path, color = neonColor)
    }
}
