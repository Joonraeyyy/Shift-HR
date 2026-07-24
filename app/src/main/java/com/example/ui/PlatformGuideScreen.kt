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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.TimeTrackerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.os.Build
import android.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.CornerRadius
import kotlin.math.sin
import kotlin.math.cos

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlatformGuideScreen(viewModel: TimeTrackerViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var activeGuideTab by remember { mutableStateOf("stepper") } // "stepper", "diagram" or "gooey"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Tab Selector for Stepper, Diagram, and Liquid UX
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Guide",
                        color = if (activeGuideTab == "stepper") Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.1f)
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Architecture",
                        color = if (activeGuideTab == "diagram") Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeGuideTab == "gooey") NeonGreen else Color.Transparent)
                    .clickable { activeGuideTab = "gooey" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BubbleChart,
                        contentDescription = null,
                        tint = if (activeGuideTab == "gooey") Color.Black else NeonGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Liquid UX",
                        color = if (activeGuideTab == "gooey") Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedContent(
            targetState = activeGuideTab,
            transitionSpec = {
                val isGoingRight = (targetState == "diagram" && initialState == "stepper") ||
                                   (targetState == "gooey" && (initialState == "stepper" || initialState == "diagram"))
                if (isGoingRight) {
                    slideInHorizontally { width -> width } with slideOutHorizontally { width -> -width }
                } else {
                    slideInHorizontally { width -> -width } with slideOutHorizontally { width -> width }
                }
            }
        ) { targetTab ->
            when (targetTab) {
                "stepper" -> StepperGuideSection(viewModel, context)
                "diagram" -> InteractiveDiagramSection(context, viewModel)
                "gooey" -> GooeySandboxSection(viewModel, context)
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
                color = com.example.ui.theme.AppTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stepDesc,
                fontSize = 12.sp,
                color = com.example.ui.theme.AppTextColor.copy(alpha = 0.7f),
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

    val appTextColor = com.example.ui.theme.AppTextColor

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
            color = appTextColor.copy(alpha = 0.15f),
            start = Offset(center.x - radius * 1.3f, center.y),
            end = Offset(center.x + radius * 1.3f, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = appTextColor.copy(alpha = 0.15f),
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
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = com.example.ui.theme.AppTextColor, modifier = Modifier.size(24.dp))
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

        Divider(color = com.example.ui.theme.AppTextColor.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth())

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
                    color = com.example.ui.theme.AppTextColor
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
            Text("Vacation Balance Simulator", fontSize = 10.sp, color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f))
            Text("$vacationBal Days Available", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.AppTextColor)
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
            color = com.example.ui.theme.AppTextColor.copy(alpha = 0.4f)
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
                Text("Tap: \"How are my overtime rates calculated?\"", fontSize = 11.sp, color = com.example.ui.theme.AppTextColor)
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InteractiveDiagramSection(context: Context, viewModel: TimeTrackerViewModel) {
    var selectedNode by remember { mutableStateOf<String?>(null) }
    var selectedSubTab by remember { mutableStateOf("flowchart") } // "flowchart", "system", "database", "apis", "ui", "files"

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
            "Designed by Senior Staff Engineers to Scale to Millions of Users",
            fontSize = 11.sp,
            color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal Scrollable Sub-Tabs for Deep-Dive Architecture
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                "flowchart" to "🔗 Flow",
                "system" to "🌐 System",
                "scaling" to "⚡ 1M Scaling",
                "database" to "🗄️ Database",
                "apis" to "🔌 APIs",
                "ui" to "🎨 UI Flow",
                "files" to "📂 Files"
            )
            tabs.forEach { (id, title) ->
                val isSelected = selectedSubTab == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) NeonGreen else Color.Transparent)
                        .clickable { selectedSubTab = id }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = selectedSubTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) with fadeOut(animationSpec = tween(250))
            }
        ) { targetSubTab ->
            when (targetSubTab) {
                "flowchart" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Interactive System Audit & Compliance Flowchart. Tap nodes to investigate audit steps.",
                            fontSize = 11.sp,
                            color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )

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

                        Spacer(modifier = Modifier.height(12.dp))

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
                                        Text(nodeDetails.second, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, lineHeight = 18.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("SYSTEM SERVICE: ${nodeDetails.third}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
                "system" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🌐 HYBRID CLOUD SYSTEM ARCHITECTURE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = "To scale to 10M+ users with sub-100ms response times globally, we deploy an offline-first hybrid architecture featuring a local Room DB, local biometrics processing, and an asynchronous, background sync-worker pipeline that integrates with a multi-region Google Cloud deployment.",
                            fontSize = 11.sp,
                            color = com.example.ui.theme.AppTextColor.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )

                        // Key Architectural Layers
                        val layers = listOf(
                            Triple("1. EDGE / CLIENT CONTAINER", "Android App (Kotlin, Compose, Room DB) implements dynamic Geofencing, offline SQLite indexing, and local encryption. Network failures are handled gracefully using an automatic retry mechanism.", Icons.Default.Smartphone),
                            Triple("2. GLOBAL CDN & EDGE GATEWAY", "Cloudflare CDN and Google Cloud API Gateway manage TLS termination, DDoS filtering, IP geofencing, and Token-Bucket rate-limiting to prevent heavy bot spam.", Icons.Default.CloudQueue),
                            Triple("3. SECURE AUTHENTICATION", "Firebase Authentication combined with JSON Web Tokens (JWT) and customized dynamic Role-Based Access Control (RBAC) securely protects confidential HR ledgers from unauthorized internal personnel.", Icons.Default.VpnKey),
                            Triple("4. DISTRIBUTED MICROSERVICES", "High-performance Go/Kotlin microservices run on Google Cloud Run with autoscaling. They handle audit trail generation, document parsing pipelines, and biometrics matches.", Icons.Default.Dns),
                            Triple("5. TRANSACTIONAL & ANALYTICAL DB", "Google Cloud Spanner guarantees high-availability ACID database transactions globally for time-clock logs, coupled with a distributed Redis cluster for read cache layers.", Icons.Default.Storage)
                        )

                        layers.forEach { (title, desc, icon) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(icon, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(desc, fontSize = 10.sp, color = com.example.ui.theme.AppTextColor.copy(alpha = 0.7f), lineHeight = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                "scaling" -> {
                    ScalingArchitectureSection(context, viewModel)
                }
                "database" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🗄️ PRODUCTION DATABASE SCHEMA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = "This application utilizes a clean, normalized relational database structure. Locally, Jetpack Room SQLite tables guarantee data durability during network dropouts, which is automatically synchronized with Google Cloud Spanner.",
                            fontSize = 11.sp,
                            color = com.example.ui.theme.AppTextColor.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )

                        // Table Schema card 1: Time Logs
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("TABLE: time_logs (Local-First Offline Ledger)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• id (PrimaryKey, AutoGenerate): Long\n" +
                                           "• date (Indexed): String [YYYY-MM-DD]\n" +
                                           "• employeeName: String\n" +
                                           "• timeIn / timeOut: Long (Timestamp)\n" +
                                           "• lunchOut / lunchIn: Long (Timestamp)\n" +
                                           "• gpsLatitude / gpsLongitude: Double\n" +
                                           "• gpsLocationName: String\n" +
                                           "• isSynced (Boolean): default false\n" +
                                           "• isApproved (String): PENDING | APPROVED | REJECTED\n" +
                                           "• hourlyRate: Double (For instant serverless payout calculations)",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Table Schema card 2: Shift Config
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("TABLE: shift_configs (Compliance Perimeters)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• id (PrimaryKey): String [Branch UID]\n" +
                                           "• branchName: String\n" +
                                           "• latitude / longitude: Double\n" +
                                           "• allowedRadiusMeters: Double [Default: 100.0]\n" +
                                           "• shiftStartHour / shiftEndHour: Int\n" +
                                           "• nightDiffMultiplier: Double [Default: 1.15]",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Table Schema card 3: Dossier Documents
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("TABLE: dossier_documents (Encrypted Attachment Index)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• id (PrimaryKey): String [UUID]\n" +
                                           "• fileName: String\n" +
                                           "• localFilePath: String (Encrypted App sandbox storage)\n" +
                                           "• category: String [LEAVE_SLIP | CORRECTION_RECEIPT | OTHER]\n" +
                                           "• profileId: String (Foreign key matching employee)\n" +
                                           "• timestamp: Long (Creation time)\n" +
                                           "• syncStatus: String [PENDING | SYNCING | SYNCED | FAILED]",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
                "apis" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🔌 SECURE ENDPOINT SPECIFICATIONS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = "All communications use HTTPS/TLS 1.3. Payloads contain SHA-256 HMAC cryptographic signatures of timestamps to completely neutralize replay and man-in-the-middle attacks.",
                            fontSize = 11.sp,
                            color = com.example.ui.theme.AppTextColor.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )

                        // Endpoint 1: Sync Time Clock
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("POST /api/v1/sync/punches", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                    Text("RATE LIMIT: 30r/m", fontSize = 8.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Request Payload:\n{\n  \"device_id\": \"mdm-android-99178-vba\",\n  \"punches\": [\n    {\n      \"uuid\": \"d290f1ee-6c54-4b01-90e6-d701748f0851\",\n      \"employee_name\": \"Sarah Jenkins\",\n      \"timestamp\": 1784534400000,\n      \"type\": \"TIME_IN\",\n      \"gps_coords\": { \"lat\": 12.97159, \"lng\": 77.59456 },\n      \"signature\": \"8a7f96b27...\"\n    }\n  ]\n}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF34D399),
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        // Endpoint 2: Biometrics Match
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("POST /api/v1/verify/biometrics", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                    Text("SECURE PIPELINE", fontSize = 8.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Request Payload:\n{\n  \"employee_name\": \"Sarah Jenkins\",\n  \"facial_vector\": [0.015, -0.224, 0.451, ...],\n  \"liveness_confidence_score\": 0.9984,\n  \"is_spoof_detected\": false\n}\n\nResponse (200 OK):\n{\n  \"match_verified\": true,\n  \"access_token\": \"jwt-access-token-string-...\"\n}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF34D399),
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }
                "ui" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🎨 UNIDIRECTIONAL STATE FLOW (UDF)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = "The application UI is fully reactive and robust, complying strictly with Android Modern Architecture rules. User events bubble up to the TimeTrackerViewModel, which statefully modifies standard Kotlin Flow structures. The Compose views automatically recompose based on those updates with absolute performance.",
                            fontSize = 11.sp,
                            color = com.example.ui.theme.AppTextColor.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )

                        // Render dynamic UDF flowchart
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .border(1.dp, NeonGreen, RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text("1. COMPOSE COMPOSABLE (USER UI EVENT)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                }
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text("2. VIEWMODEL STATE MUTATION (MUTABLESTATEFLOW)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text("3. ASYNC COROUTINE & ROOM DB UPDATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                                Box(
                                    modifier = Modifier
                                        .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .border(1.dp, NeonGreen, RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text("4. COMPOSABLE COLLECTS STATE (AUTOMATIC RECOMPOSITION)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                }
                            }
                        }
                    }
                }
                "files" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "📂 PROJECT ARCHITECTURE FILE STRUCTURE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = "Clean Architecture is followed in our codebase, dividing files logically into data, domain, repository, database, viewmodel, and ui. This is highly scalable, enabling multiple developers to build features concurrently without conflict.",
                            fontSize = 11.sp,
                            color = com.example.ui.theme.AppTextColor.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )

                        // Interactive Tree View
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "📁 app/src/main/java/com/example/\n" +
                                           " ├── 📁 data/\n" +
                                           " │    ├── 📁 database/        [Room SQLite AppDatabase, DAOs, Entities]\n" +
                                           " │    │    ├── AppDatabase.kt\n" +
                                           " │    │    ├── TimeLogEntity.kt\n" +
                                           " │    │    ├── ShiftConfigEntity.kt\n" +
                                           " │    │    └── DossierDocumentEntity.kt\n" +
                                           " │    ├── 📁 repository/      [Clean Repository Interface pattern]\n" +
                                           " │    │    └── TimeTrackerRepository.kt\n" +
                                           " │    └── 📁 services/        [REST GeminiModels/Weather API clients]\n" +
                                           " ├── 📁 ui/\n" +
                                           " │    ├── 📁 theme/           [Central Colors, Fonts & Material3 styles]\n" +
                                           " │    ├── 📁 viewmodel/       [Unidirectional state handlers]\n" +
                                           " │    │    └── TimeTrackerViewModel.kt\n" +
                                           " │    ├── SaasScreens.kt      [Dynamic Multi-Tenant SaaS Workspace]\n" +
                                           " │    ├── Top5DashboardScreen.kt [Staff analytics and active metrics]\n" +
                                           " │    └── PlatformGuideScreen.kt [This Interactive Architectural Guide]\n" +
                                           " └── MainActivity.kt         [Single Activity Root Coordinator]",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8),
                                    lineHeight = 14.sp
                                )
                            }
                        }
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
            Text(desc, fontSize = 9.sp, color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f))
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

// ==================== LIQUID GOOEY COMPONENT & SANDBOX ====================

class BubbleAnimationState(
    val id: Long,
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float,
    val maxRadius: Float,
    val durationMillis: Int = 600,
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
fun GooeyLiquidButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    buttonColor: Color = NeonGreen,
    textColor: Color = Color.Black,
    icon: ImageVector? = null,
    blurRadius: Float = 15f,
    contrast: Float = 30f,
    offset: Float = -350f,
    numSatellites: Int = 3,
    bubbleColor: Color = buttonColor
) {
    val coroutineScope = rememberCoroutineScope()
    val bubbles = remember { mutableStateListOf<BubbleAnimationState>() }
    val localDensity = androidx.compose.ui.platform.LocalDensity.current

    // Coordinates padding for canvas overflow (so bubbles splash outside the button smoothly)
    val padXPx = remember { with(localDensity) { 30.dp.toPx() } }
    val padYPx = remember { with(localDensity) { 15.dp.toPx() } }

    val cornerRadius = 25.dp

    // Draw parameters
    Box(
        modifier = modifier
            .size(width = 260.dp, height = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Gooey layer (Blurred and Contrasted together on GPU RenderThread)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            val blurEffect = android.graphics.RenderEffect.createBlurEffect(
                                blurRadius,
                                blurRadius,
                                android.graphics.Shader.TileMode.CLAMP
                            )
                            val alphaMatrix = floatArrayOf(
                                1f, 0f, 0f, 0f, 0f,
                                0f, 1f, 0f, 0f, 0f,
                                0f, 0f, 1f, 0f, 0f,
                                0f, 0f, 0f, contrast, offset
                            )
                            val colorFilterEffect = android.graphics.RenderEffect.createColorFilterEffect(
                                android.graphics.ColorMatrixColorFilter(alphaMatrix)
                            )
                            val chain = android.graphics.RenderEffect.createChainEffect(colorFilterEffect, blurEffect)
                            renderEffect = chain.asComposeRenderEffect()
                        } catch (e: Exception) {
                            // Fallback gracefully on error
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val buttonWidth = size.width - 2 * padXPx
                val buttonHeight = size.height - 2 * padYPx

                // Draw button main background
                drawRoundRect(
                    color = buttonColor,
                    topLeft = Offset(padXPx, padYPx),
                    size = Size(buttonWidth, buttonHeight),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )

                // Draw active organic splash bubbles
                bubbles.forEach { bubble ->
                    val p = bubble.progress.value
                    val currentX = bubble.startX + (bubble.targetX - bubble.startX) * p
                    val currentY = bubble.startY + (bubble.targetY - bubble.startY) * p
                    // Mathematical sine curve for smooth expanding & contraction of bubble: sin(p * PI)
                    val scale = sin(p * Math.PI).toFloat()
                    val radius = bubble.maxRadius * scale

                    if (radius > 0.1f) {
                        drawCircle(
                            color = bubbleColor,
                            radius = radius,
                            center = Offset(currentX, currentY)
                        )
                    }
                }
            }
        }

        // 2. Crisp overlay text layer (unblurred for maximum readability/accessibility)
        Box(
            modifier = Modifier
                .size(width = 200.dp, height = 50.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { localOffset ->
                            // Translate tap position to canvas coordinates
                            val canvasX = localOffset.x + padXPx
                            val canvasY = localOffset.y + padYPx

                            // Spawning bubbles
                            val newBubbles = mutableListOf<BubbleAnimationState>()
                            val tapTime = System.currentTimeMillis()

                            // Add a major central splash bubble that merges into button body
                            newBubbles.add(
                                BubbleAnimationState(
                                    id = tapTime,
                                    startX = canvasX,
                                    startY = canvasY,
                                    targetX = canvasX + (size.width / 2f - canvasX) * 0.35f, // drifts toward center
                                    targetY = canvasY + (size.height / 2f - canvasY) * 0.35f,
                                    maxRadius = 38.dp.toPx(),
                                    durationMillis = 650
                                )
                            )

                            // Add minor satellite splash bubbles shooting outward
                            for (i in 0 until numSatellites) {
                                val angle = (Math.random() * 2 * Math.PI).toFloat()
                                val distance = (40f + Math.random().toFloat() * 45f) // random travel distance
                                val satTargetX = canvasX + cos(angle) * distance
                                val satTargetY = canvasY + sin(angle) * distance

                                newBubbles.add(
                                    BubbleAnimationState(
                                        id = tapTime + i + 1,
                                        startX = canvasX,
                                        startY = canvasY,
                                        targetX = satTargetX,
                                        targetY = satTargetY,
                                        maxRadius = (8f + Math.random().toFloat() * 9f).dp.toPx(),
                                        durationMillis = 500 + (Math.random() * 150).toInt(),
                                        delayMillis = (Math.random() * 40).toInt()
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

                            // Trigger onClick callback
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
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GooeySandboxSection(viewModel: TimeTrackerViewModel, context: Context) {
    // Sandbox adjustments state
    var blurRadius by remember { mutableStateOf(20f) }
    var contrast by remember { mutableStateOf(30f) }
    var alphaOffset by remember { mutableStateOf(-350f) }
    var numSatellites by remember { mutableStateOf(3) }
    var selectedPresetName by remember { mutableStateOf("Mercury") }

    // Floating sandbox decoration state
    var clickCount by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LIQUID METABALL SIMULATOR",
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = NeonGreen,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap the button below to witness a high-performance fluid gooey effect running fully on GPU RenderThread (60+ FPS).",
                fontSize = 11.sp,
                color = com.example.ui.theme.AppTextColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- THE VISUAL BUTTON PLAYGROUND ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                GooeyLiquidButton(
                    text = "PUNCH TIME IN",
                    icon = Icons.Default.TouchApp,
                    onClick = {
                        clickCount++
                        Toast.makeText(context, "Shift Punch Triggered! 🟢 Total Sandbox Taps: $clickCount", Toast.LENGTH_SHORT).show()
                    },
                    blurRadius = blurRadius,
                    contrast = contrast,
                    offset = alphaOffset,
                    numSatellites = numSatellites,
                    buttonColor = NeonGreen,
                    textColor = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- PRESETS ROW ---
            Text(
                text = "PHYSICS PRESETS",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    Triple("Mercury", 20f, 30f),
                    Triple("Soft Lava", 35f, 15f),
                    Triple("Water Drop", 12f, 40f),
                    Triple("Gas Plasma", 28f, 25f)
                )

                presets.forEach { (name, b, c) ->
                    val isSelected = selectedPresetName == name
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                            .border(1.dp, if (isSelected) NeonGreen else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .clickable {
                                selectedPresetName = name
                                blurRadius = b
                                contrast = c
                                alphaOffset = -c * 11.5f // proportional threshold offset
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) NeonGreen else Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- SLIDERS SECTION ---
            Text(
                text = "GOOEY ENGINE FINE-TUNING",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Slider 1: Blur Radius
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Blur Radius (feGaussianBlur)", fontSize = 11.sp, color = com.example.ui.theme.AppTextColor)
                    Text("${blurRadius.toInt()}px", fontSize = 11.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = blurRadius,
                    onValueChange = {
                        blurRadius = it
                        selectedPresetName = "Custom"
                    },
                    valueRange = 5f..50f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonGreen,
                        activeTrackColor = NeonGreen,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }

            // Slider 2: Contrast (feColorMatrix Matrix Value)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Contrast Multiplier (feColorMatrix)", fontSize = 11.sp, color = com.example.ui.theme.AppTextColor)
                    Text("${contrast.toInt()}x", fontSize = 11.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = contrast,
                    onValueChange = {
                        contrast = it
                        selectedPresetName = "Custom"
                    },
                    valueRange = 10f..60f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonGreen,
                        activeTrackColor = NeonGreen,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }

            // Slider 3: Alpha Offset (feColorMatrix Constant value)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Alpha Threshold (Offset)", fontSize = 11.sp, color = com.example.ui.theme.AppTextColor)
                    Text("${alphaOffset.toInt()}", fontSize = 11.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = alphaOffset,
                    onValueChange = {
                        alphaOffset = it
                        selectedPresetName = "Custom"
                    },
                    valueRange = -4000f..-100f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonGreen,
                        activeTrackColor = NeonGreen,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }

            // Slider 4: Satellite Splash Droplets
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Splash Satellite Droplets", fontSize = 11.sp, color = com.example.ui.theme.AppTextColor)
                    Text("$numSatellites bubbles", fontSize = 11.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = numSatellites.toFloat(),
                    onValueChange = {
                        numSatellites = it.toInt()
                        selectedPresetName = "Custom"
                    },
                    valueRange = 0f..6f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonGreen,
                        activeTrackColor = NeonGreen,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TECHNICAL INFO EXPLANATION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "⚙️ Under The Hood (Hardware Acceleration)",
                        color = com.example.ui.theme.AppTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Metaballs (organic liquid merging) are drawn on a single hardware-accelerated Skia graphics layer. First, we apply a high-radius blur filter. Second, we apply a Color Matrix filter that amplifies the Alpha channel contrast by 30x or 40x and shifts the threshold using offset, sharpening the blurred alpha outline instantly on the GPU (RenderThread) for perfect 60+ FPS.",
                        color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

// =========================================================================
// ⚡ MULTI-TIERED SCALABLE BACKEND ARCHITECTURE SECTION (1M+ USERS)
// =========================================================================

@Composable
fun ScalingArchitectureSection(context: Context, viewModel: TimeTrackerViewModel) {
    var selectedDiagramNode by remember { mutableStateOf<String?>("alb") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🏗️ SCALABLE ARCHITECTURE FOR YOUR APP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonGreen,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "Stateless Ktor Cluster, Redis Pub/Sub Sync, PgBouncer DB Proxy & Ktor Mobile Resilience (1M+ Active Users)",
                fontSize = 10.5.sp,
                color = com.example.ui.theme.AppTextColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // =========================================================================
        // VISUAL DIAGRAM CARD (Faithful to "The Scalable Architecture for Your App")
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF090D16)),
            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏗️ Multi-Tier Scaling Topology",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = NeonGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, NeonGreen)
                    ) {
                        Text(
                            text = "LIVE CLUSTER",
                            color = NeonGreen,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Tap any node box to inspect layer protocol specs and zero-downtime routing details.",
                    color = com.example.ui.theme.AppTextColor.copy(alpha = 0.5f),
                    fontSize = 9.5.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Layer 1: Android App Clients
                ArchitectureDiagramNodeBox(
                    title = "[ Android App Clients ]",
                    subtitle = "Jetpack Compose + Ktor Client + Retry Policies",
                    nodeKey = "client",
                    isSelected = selectedDiagramNode == "client",
                    accentColor = Color(0xFF38BDF8),
                    onClick = { selectedDiagramNode = "client" }
                )

                DiagramVerticalConnector()

                // Layer 2: Cloudflare / AWS Route 53
                ArchitectureDiagramNodeBox(
                    title = "[ Cloudflare / AWS Route 53 ]",
                    subtitle = "DNS Routing + Anycast + DDoS Shield",
                    nodeKey = "dns",
                    isSelected = selectedDiagramNode == "dns",
                    accentColor = Color(0xFFF59E0B),
                    onClick = { selectedDiagramNode = "dns" }
                )

                DiagramVerticalConnector()

                // Layer 3: AWS Application Load Balancer
                ArchitectureDiagramNodeBox(
                    title = "[ AWS Application Load Balancer ]",
                    subtitle = "ALB / Traefik Gateway (Stateless Round-Robin)",
                    nodeKey = "alb",
                    isSelected = selectedDiagramNode == "alb",
                    accentColor = Color(0xFFA855F7),
                    onClick = { selectedDiagramNode = "alb" }
                )

                DiagramSplitConnector()

                // Layer 4: Ktor Auto-Scaled Container Nodes (2 Parallel Columns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ArchitectureDiagramNodeBox(
                            title = "Ktor Node 1",
                            subtitle = "Stateless App Node",
                            nodeKey = "node1",
                            isSelected = selectedDiagramNode == "node1",
                            accentColor = NeonGreen,
                            onClick = { selectedDiagramNode = "node1" }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ArchitectureDiagramNodeBox(
                            title = "Ktor Node 2",
                            subtitle = "Auto-Scaled Container",
                            nodeKey = "node2",
                            isSelected = selectedDiagramNode == "node2",
                            accentColor = NeonGreen,
                            onClick = { selectedDiagramNode = "node2" }
                        )
                    }
                }

                DiagramMergeConnector()

                // Layer 5: Backend Sub-Systems (Redis, PgBouncer, Postgres Read Replicas)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ArchitectureDiagramNodeBox(
                            title = "Redis Pub/Sub",
                            subtitle = "WebSocket Sync",
                            nodeKey = "redis",
                            isSelected = selectedDiagramNode == "redis",
                            accentColor = Color(0xFFEF4444),
                            onClick = { selectedDiagramNode = "redis" }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ArchitectureDiagramNodeBox(
                            title = "PgBouncer Pool",
                            subtitle = "DB Proxy",
                            nodeKey = "pgbouncer",
                            isSelected = selectedDiagramNode == "pgbouncer",
                            accentColor = Color(0xFF10B981),
                            onClick = { selectedDiagramNode = "pgbouncer" }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ArchitectureDiagramNodeBox(
                            title = "PostgreSQL Replicas",
                            subtitle = "Read Replica Cluster",
                            nodeKey = "replicas",
                            isSelected = selectedDiagramNode == "replicas",
                            accentColor = Color(0xFF3B82F6),
                            onClick = { selectedDiagramNode = "replicas" }
                        )
                    }
                }

                DiagramVerticalConnector()

                // Layer 6: PostgreSQL Main
                ArchitectureDiagramNodeBox(
                    title = "[ PostgreSQL Main ]",
                    subtitle = "Primary ACID Write Master Database",
                    nodeKey = "postgres_main",
                    isSelected = selectedDiagramNode == "postgres_main",
                    accentColor = Color(0xFF6366F1),
                    onClick = { selectedDiagramNode = "postgres_main" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Node Detailed Modal/Card
                if (selectedDiagramNode != null) {
                    val (nodeName, nodeDesc, nodeTech) = when (selectedDiagramNode) {
                        "client" -> Triple(
                            "Android Mobile Client (Jetpack Compose)",
                            "Mobile clients connect via HTTPS and WebSockets. Built with Ktor Client featuring automated exponential retry policies, token refresh handlers, and offline Room DB caching during network handoffs.",
                            "Protocol: WSS / TLS 1.3 | Retry Strategy: Exponential Backoff (1s, 2s, 4s, 8s)"
                        )
                        "dns" -> Triple(
                            "Cloudflare / AWS Route 53 DNS & DDoS Shield",
                            "Provides Anycast DNS resolution, TLS termination, Web Application Firewall (WAF) rate limiting, and DDoS mitigation before traffic reaches backend servers.",
                            "Edge Nodes: 300+ Locations | Protection: Anycast Layer 7 WAF"
                        )
                        "alb" -> Triple(
                            "AWS Application Load Balancer (ALB / Traefik)",
                            "Distributes inbound HTTP/WebSocket connections statelessly across 50+ auto-scaled Ktor nodes using round-robin and active liveness/readiness health probes.",
                            "Health Target: GET /health | Port: 443 -> 8080 | Sticky Session: Disabled"
                        )
                        "node1", "node2" -> Triple(
                            "Ktor Server Container Instance (Auto-Scaled)",
                            "Stateless Kotlin Ktor microservice container. Uses JWT for authentication and does not store session state in memory. Handles chat, clock punching, and biometrics validation.",
                            "Runtime: Ktor Netty | Auth: Stateless HMAC-256 JWT | Memory: 512MB per container"
                        )
                        "redis" -> Triple(
                            "Redis Pub/Sub WebSocket Message Relay Cluster",
                            "Enables instant WebSocket synchronization between Ktor nodes. When User A posts a chat message or punch on Node 1, Redis Pub/Sub immediately broadcasts it to Node 2.",
                            "Pub/Sub Adapter: io.lettuce.core | Latency: <2ms | Channel: chat_{id}"
                        )
                        "pgbouncer" -> Triple(
                            "PgBouncer Database Connection Proxy Pool",
                            "Pools 1,000+ HikariCP connections down to 50 active PostgreSQL server connections. Prevents PostgreSQL process memory crashes during traffic surges.",
                            "Mode: Transaction Pooling | Listen Port: 6432 | Max Client Conn: 10,000"
                        )
                        "replicas" -> Triple(
                            "PostgreSQL Read Replica Cluster",
                            "Handles all read-heavy queries (e.g. historical shift reports, attendance ledgers, profile audits), freeing the primary database to process writes exclusively.",
                            "Replication: Streaming Asynchronous | Query Load: 80% Read Traffic"
                        )
                        "postgres_main" -> Triple(
                            "PostgreSQL Main Master Database",
                            "Primary transactional database that processes strictly INSERT and UPDATE operations (clock punches, employee leave updates, payroll adjustments) with full ACID safety.",
                            "Storage: Managed SSD NVMe | Write Target: Primary Node"
                        )
                        else -> Triple("", "", "")
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, NeonGreen)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = nodeName, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = nodeDesc, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, lineHeight = 16.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "SPEC: $nodeTech", color = Color(0xFF38BDF8), fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // STEP-BY-STEP IMPLEMENTATION CARDS (KTOR SERVER & CLIENT)
        // =========================================================================

        Text(
            text = "⚡ STEP-BY-STEP IMPLEMENTATION CODE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = NeonGreen,
            letterSpacing = 1.sp
        )

        // Step 1 Card: Stateless JWT Auth
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Step 1: Enforce Stateless Authentication in Ktor",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Removing in-memory sessions allows any load balancer to route requests to any Ktor instance randomly without sticky sessions.",
                    color = com.example.ui.theme.AppTextColor.copy(alpha = 0.7f),
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Surface(
                    color = Color(0xFF030712),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "// Ktor Server Setup (Application.kt)\n" +
                               "install(Authentication) {\n" +
                               "    jwt(\"auth-jwt\") {\n" +
                               "        realm = \"shift-hr-compliance\"\n" +
                               "        verifier(\n" +
                               "            JWT.require(Algorithm.HMAC256(\"YOUR_SECRET_KEY\"))\n" +
                               "                .withIssuer(\"https://api.shifthr.com\")\n" +
                               "                .build()\n" +
                               "        )\n" +
                               "        validate { credential ->\n" +
                               "            if (credential.payload.getClaim(\"userId\").asString() != \"\") {\n" +
                               "                JWTPrincipal(credential.payload)\n" +
                               "            } else null\n" +
                               "        }\n" +
                               "    }\n" +
                               "}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF34D399),
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Step 2 Card: Health Check Endpoint
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Step 2: Implement a Health Check Endpoint",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Load balancers (ALB/Kubernetes) continuously ping this endpoint. If a node crashes, it is automatically removed from rotation.",
                    color = com.example.ui.theme.AppTextColor.copy(alpha = 0.7f),
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Surface(
                    color = Color(0xFF030712),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "routing {\n" +
                               "    // Health Check for ALB / NGINX / Kubernetes\n" +
                               "    get(\"/health\") {\n" +
                               "        // You can verify DB connectivity here if needed\n" +
                               "        call.respond(HttpStatusCode.OK, mapOf(\"status\" to \"HEALTHY\", \"service\" to \"ktor-core\"))\n" +
                               "    }\n" +
                               "}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF34D399),
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Step 3 Card: Redis Pub/Sub WebSocket Sync
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Step 3: Enable WebSocket Synchronization across Nodes (Redis Pub/Sub)",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "When User A connects to Node 1 and User B connects to Node 2, Redis Pub/Sub acts as the central message relay broker.",
                    color = com.example.ui.theme.AppTextColor.copy(alpha = 0.7f),
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Surface(
                    color = Color(0xFF030712),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "import io.lettuce.core.RedisClient\n" +
                               "import kotlinx.coroutines.flow.collect\n\n" +
                               "class ChatRelayService(redisUrl: String) {\n" +
                               "    private val redisClient = RedisClient.create(redisUrl)\n" +
                               "    private val pubSubConnection = redisClient.connectPubSub()\n\n" +
                               "    fun publishChatMessage(channelId: String, messageJson: String) {\n" +
                               "        pubSubConnection.async().publish(\"chat_\$channelId\", messageJson)\n" +
                               "    }\n\n" +
                               "    fun subscribeToChannel(channelId: String, onMessageReceived: (String) -> Unit) {\n" +
                               "        pubSubConnection.addListener(object : RedisPubSubAdapter<String, String>() {\n" +
                               "            override fun message(channel: String?, message: String?) {\n" +
                               "                if (channel == \"chat_\$channelId\" && message != null) {\n" +
                               "                    onMessageReceived(message)\n" +
                               "                }\n" +
                               "            }\n" +
                               "        })\n" +
                               "        pubSubConnection.async().subscribe(\"chat_\$channelId\")\n" +
                               "    }\n" +
                               "}",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF34D399),
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Step 4 Card: Database Pooling & Read/Write Splitting
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Step 4: Database Connection Pooling & Read/Write Split",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "50+ Ktor instances running HikariCP (20 conns each) will crash PostgreSQL with 1,000+ open connections. PgBouncer pools connections, while Read Replicas offload heavy report queries.",
                    color = com.example.ui.theme.AppTextColor.copy(alpha = 0.7f),
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("🛡️ PgBouncer Proxy", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Pools 1,000+ client connections down to 50 active DB sockets safely.", color = com.example.ui.theme.AppTextColor.copy(alpha = 0.7f), fontSize = 9.sp)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("🔀 Read/Write Split", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Writes -> Primary DB Master\nReads -> Replicas (80% load reduction)", color = com.example.ui.theme.AppTextColor.copy(alpha = 0.7f), fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SUMMARY CHECKLIST MATRIX TABLE (1M USERS)
        // =========================================================================

        Text(
            text = "📊 SUMMARY CHECKLIST TO HIT 1M USERS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = NeonGreen,
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Layer", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Standard Setup (Fails ❌)", color = Color(0xFFEF4444), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                    Text("Millions-Ready Setup (✅)", color = NeonGreen, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                val rows = listOf(
                    Triple("Traffic Router", "Single Public Server IP", "AWS ALB / NGINX + Cloudflare Anycast DNS"),
                    Triple("Ktor App Server", "Single EC2 / VPS Instance", "Docker + AWS ECS / EKS with Auto-Scaling"),
                    Triple("WebSockets / State", "Local JVM In-Memory State", "Redis Pub/Sub Cluster"),
                    Triple("Database", "Single Monolithic PostgreSQL", "PgBouncer + Postgres Read Replicas"),
                    Triple("Android Client", "Single HTTP Request", "Ktor Client with Exponential Retry & Keep-Alive")
                )

                rows.forEach { (layer, bad, good) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(layer, color = com.example.ui.theme.AppTextColor, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(bad, color = Color.LightGray.copy(alpha = 0.6f), fontSize = 8.5.sp, modifier = Modifier.weight(1.2f))
                        Text(good, color = NeonGreen, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.4f))
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                }
            }
        }
    }
}

@Composable
private fun ArchitectureDiagramNodeBox(
    title: String,
    subtitle: String,
    nodeKey: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF0F172A),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = if (isSelected) accentColor else Color.White,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                color = com.example.ui.theme.AppTextColor.copy(alpha = 0.6f),
                fontSize = 8.5.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DiagramVerticalConnector() {
    Box(
        modifier = Modifier
            .height(14.dp)
            .width(2.dp)
            .background(NeonGreen.copy(alpha = 0.6f))
    )
}

@Composable
private fun DiagramSplitConnector() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .height(10.dp)
                .width(2.dp)
                .background(NeonGreen.copy(alpha = 0.6f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(2.dp)
                .background(NeonGreen.copy(alpha = 0.6f))
        )
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.size(width = 2.dp, height = 8.dp).background(NeonGreen.copy(alpha = 0.6f)))
            Box(modifier = Modifier.size(width = 2.dp, height = 8.dp).background(NeonGreen.copy(alpha = 0.6f)))
        }
    }
}

@Composable
private fun DiagramMergeConnector() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.size(width = 2.dp, height = 8.dp).background(NeonGreen.copy(alpha = 0.6f)))
            Box(modifier = Modifier.size(width = 2.dp, height = 8.dp).background(NeonGreen.copy(alpha = 0.6f)))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(2.dp)
                .background(NeonGreen.copy(alpha = 0.6f))
        )
        Box(
            modifier = Modifier
                .height(10.dp)
                .width(2.dp)
                .background(NeonGreen.copy(alpha = 0.6f))
        )
    }
}

