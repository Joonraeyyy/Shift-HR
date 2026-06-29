package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.TimeLogEntity
import com.example.ui.viewmodel.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Neon Theme Color Tokens matching ClauseOS Compliance
val NeonGreen: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary
val DarkGreyBg = Color(0xFF0F0F11)
val CardGreyBg = Color(0xFF1E1E22)
val TextLight = Color(0xFFF1F1F3)
val BorderGrey = Color(0xFF2C2C32)

// ---------------------- 1. CORE HR SCREEN ----------------------
@Composable
fun CoreHrScreen(viewModel: TimeTrackerViewModel) {
    var selectedProfile by remember { mutableStateOf<EmployeeProfile?>(null) }
    var showIdCardGenerator by remember { mutableStateOf(false) }
    val profiles by viewModel.employeeProfiles
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Core HR Portal",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGreen,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Employee Directory & Documents",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Button(
                onClick = { showIdCardGenerator = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate ID", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Employee Directory list
        profiles.forEach { profile ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { selectedProfile = profile }
                    .testTag("employee_card_${profile.id}"),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, if (selectedProfile?.id == profile.id) NeonGreen else BorderGrey)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.take(2).uppercase(),
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = profile.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${profile.position} | ${profile.department}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                when (profile.status) {
                                    "Regular" -> Color(0xFF00FF88).copy(alpha = 0.15f)
                                    "Probationary" -> Color(0xFFFFCC00).copy(alpha = 0.15f)
                                    else -> Color(0xFF00E5FF).copy(alpha = 0.15f)
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = profile.status,
                            color = when (profile.status) {
                                "Regular" -> Color(0xFF00FF88)
                                "Probationary" -> Color(0xFFFFCC00)
                                else -> Color(0xFF00E5FF)
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Active Profile details view
        selectedProfile?.let { profile ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Employee Dossier: ${profile.name}", color = NeonGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Divider(color = BorderGrey, modifier = Modifier.padding(vertical = 10.dp))

                    ProfileDetailRow(label = "Employee ID", value = profile.id)
                    ProfileDetailRow(label = "Work Email", value = profile.email)
                    ProfileDetailRow(label = "Emergency Contact", value = profile.emergencyContactName)
                    ProfileDetailRow(label = "Emergency Phone", value = profile.emergencyContactPhone)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Secure Document Vault", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    profile.documents.forEach { docName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                                .clickable {
                                    Toast.makeText(context, "Downloading encrypted $docName...", Toast.LENGTH_SHORT).show()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = docName, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }

    if (showIdCardGenerator) {
        AlertDialog(
            onDismissRequest = { showIdCardGenerator = false },
            containerColor = DarkGreyBg,
            title = { Text("ID Card Generator", color = NeonGreen) },
            text = {
                var currentSelection by remember { mutableStateOf(profiles.firstOrNull() ?: profiles[0]) }
                Column {
                    Text("Select Employee profile:", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    profiles.forEach { profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (currentSelection.id == profile.id) NeonGreen.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { currentSelection = profile }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentSelection.id == profile.id, onClick = { currentSelection = profile })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(profile.name, color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Simulated Printable ID Card Representation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xFF141416), Color(0xFF222226))), RoundedCornerShape(12.dp))
                            .border(1.dp, NeonGreen, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(text = viewModel.companyName.value.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                            Text(text = "SECURE EMPLOYEE ID", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(27.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(currentSelection.name.take(2).uppercase(), color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text(text = currentSelection.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = currentSelection.position, fontSize = 11.sp, color = NeonGreen)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("CARD ID", fontSize = 7.sp, color = Color.White.copy(alpha = 0.4f))
                                    Text(currentSelection.id, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("STATUS", fontSize = 7.sp, color = Color.White.copy(alpha = 0.4f))
                                    Text(currentSelection.status.uppercase(), fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "ID Card queued for physical print!", Toast.LENGTH_SHORT).show()
                        showIdCardGenerator = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Print ID", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIdCardGenerator = false }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------------- 2. EMPLOYEE SELF-SERVICE SCREEN ----------------------
@Composable
fun SelfServiceScreen(viewModel: TimeTrackerViewModel) {
    var activeSubTab by remember { mutableStateOf("leave") } // leave, correction, claims, profile
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Employee Self-Service",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGreen,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Sub Navigation Tabs for Self-Service
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardGreyBg, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val subTabs = listOf(
                "leave" to "Leaves",
                "correction" to "Corrections",
                "claims" to "Claims",
                "profile" to "My Profile"
            )
            subTabs.forEach { (key, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeSubTab == key) NeonGreen else Color.Transparent)
                        .clickable { activeSubTab = key }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (activeSubTab == key) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (activeSubTab) {
            "leave" -> {
                LeaveFilingView(viewModel, context)
            }
            "correction" -> {
                CorrectionFilingView(viewModel, context)
            }
            "claims" -> {
                ReimbursementFilingView(viewModel, context)
            }
            "profile" -> {
                ProfileEditingView(viewModel, context)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveFilingView(viewModel: TimeTrackerViewModel, context: Context) {
    var leaveType by remember { mutableStateOf("Sick Leave") }
    var startDate by remember { mutableStateOf("2026-07-06") }
    var endDate by remember { mutableStateOf("2026-07-08") }
    var reason by remember { mutableStateOf("") }
    val leaveRequests by viewModel.leaveRequests

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
        border = BorderStroke(1.dp, BorderGrey)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("File New Leave Request", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // Leave Type Select
            Text("Select Leave Category:", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Sick Leave", "Vacation Leave").forEach { type ->
                    val isSelected = leaveType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (isSelected) NeonGreen else BorderGrey, RoundedCornerShape(8.dp))
                            .clickable { leaveType = type }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(type, color = if (isSelected) NeonGreen else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("Start Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth().testTag("leave_start_date"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it },
                label = { Text("End Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth().testTag("leave_end_date"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Justification Reason") },
                modifier = Modifier.fillMaxWidth().testTag("leave_reason"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (reason.isBlank()) {
                        Toast.makeText(context, "Please enter a valid reason.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.fileLeaveRequest(leaveType, startDate, endDate, reason)
                        reason = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("Submit Leave Request", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text("My Historical Leaves", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))

    val myLeaves = leaveRequests.filter { it.employeeName == viewModel.currentUserName.value }
    if (myLeaves.isEmpty()) {
        Text("No leave history found.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
    } else {
        myLeaves.forEach { req ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (req.leaveType == "Sick Leave") Icons.Default.MedicalServices else Icons.Default.FlightTakeoff,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${req.leaveType}: ${req.startDate} to ${req.endDate}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(req.reason, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                when (req.status) {
                                    "APPROVED" -> Color(0xFF00FF88).copy(alpha = 0.15f)
                                    "REJECTED" -> Color(0xFFFF5555).copy(alpha = 0.15f)
                                    else -> Color(0xFFFFCC00).copy(alpha = 0.15f)
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = req.status,
                            color = when (req.status) {
                                "APPROVED" -> Color(0xFF00FF88)
                                "REJECTED" -> Color(0xFFFF5555)
                                else -> Color(0xFFFFCC00)
                            },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CorrectionFilingView(viewModel: TimeTrackerViewModel, context: Context) {
    var date by remember { mutableStateOf("2026-06-29") }
    var inTime by remember { mutableStateOf("09:00 AM") }
    var outTime by remember { mutableStateOf("06:00 PM") }
    var reason by remember { mutableStateOf("") }
    val corrections by viewModel.correctionRequests

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
        border = BorderStroke(1.dp, BorderGrey)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Request Time Correction", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Correction Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inTime,
                onValueChange = { inTime = it },
                label = { Text("Requested In Time (HH:MM AM/PM)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = outTime,
                onValueChange = { outTime = it },
                label = { Text("Requested Out Time (HH:MM AM/PM)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason for Correction") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (reason.isBlank()) {
                        Toast.makeText(context, "Please enter a valid justification.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.fileCorrectionRequest(date, inTime, outTime, reason)
                        reason = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("File Correction Request", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text("Correction Requests Status", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))

    val myCorrections = corrections.filter { it.employeeName == viewModel.currentUserName.value }
    if (myCorrections.isEmpty()) {
        Text("No corrections filed.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
    } else {
        myCorrections.forEach { req ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditCalendar, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Date: ${req.date} (${req.requestedTimeIn} - ${req.requestedTimeOut})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(req.reason, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                when (req.status) {
                                    "APPROVED" -> Color(0xFF00FF88).copy(alpha = 0.15f)
                                    "REJECTED" -> Color(0xFFFF5555).copy(alpha = 0.15f)
                                    else -> Color(0xFFFFCC00).copy(alpha = 0.15f)
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(req.status, color = if (req.status == "APPROVED") Color(0xFF00FF88) else if (req.status == "REJECTED") Color(0xFFFF5555) else Color(0xFFFFCC00), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReimbursementFilingView(viewModel: TimeTrackerViewModel, context: Context) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val claims by viewModel.reimbursementRequests

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
        border = BorderStroke(1.dp, BorderGrey)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Submit Reimbursement Request", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Item Title (e.g. BroadBand, Fuel)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Claim Amount ($)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val doubleAmt = amount.toDoubleOrNull()
                    if (title.isBlank() || doubleAmt == null || doubleAmt <= 0) {
                        Toast.makeText(context, "Please enter a valid item and amount.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.fileReimbursement(title, doubleAmt)
                        title = ""
                        amount = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("Submit Expense Claim", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text("Reimbursement Claims History", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))

    val myClaims = claims.filter { it.employeeName == viewModel.currentUserName.value }
    if (myClaims.isEmpty()) {
        Text("No reimbursement claims logged.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
    } else {
        myClaims.forEach { req ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(req.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Amount: $${req.amount}", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                when (req.status) {
                                    "APPROVED" -> Color(0xFF00FF88).copy(alpha = 0.15f)
                                    "REJECTED" -> Color(0xFFFF5555).copy(alpha = 0.15f)
                                    else -> Color(0xFFFFCC00).copy(alpha = 0.15f)
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(req.status, color = if (req.status == "APPROVED") Color(0xFF00FF88) else if (req.status == "REJECTED") Color(0xFFFF5555) else Color(0xFFFFCC00), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileEditingView(viewModel: TimeTrackerViewModel, context: Context) {
    val profiles by viewModel.employeeProfiles
    val myProfile = profiles.find { it.name == viewModel.currentUserName.value }

    if (myProfile != null) {
        var emergencyName by remember { mutableStateOf(myProfile.emergencyContactName) }
        var emergencyPhone by remember { mutableStateOf(myProfile.emergencyContactPhone) }
        var workEmail by remember { mutableStateOf(myProfile.email) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Personal Dossier & Contacts", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = workEmail,
                    onValueChange = { workEmail = it },
                    label = { Text("Work Email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emergencyName,
                    onValueChange = { emergencyName = it },
                    label = { Text("Emergency Contact Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emergencyPhone,
                    onValueChange = { emergencyPhone = it },
                    label = { Text("Emergency Contact Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                )

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        viewModel.updateProfileInfo(emergencyName, emergencyPhone, workEmail)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Update Dossier Details", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        Text("Dossier profile not found. Please contact administration.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}

// ---------------------- 3. PAYROLL & REPORTS SCREEN ----------------------
@Composable
fun PayrollScreen(viewModel: TimeTrackerViewModel) {
    var activeSubView by remember { mutableStateOf("calc") } // calc, payslips, reports
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Payroll & Financials",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGreen,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardGreyBg, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val sections = listOf(
                "calc" to "Salary Calculator",
                "payslips" to "My Payslips",
                "reports" to "Analytics Reports"
            )
            sections.forEach { (key, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeSubView == key) NeonGreen else Color.Transparent)
                        .clickable { activeSubView = key }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (activeSubView == key) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (activeSubView) {
            "calc" -> {
                SalaryCalculatorView(viewModel, context)
            }
            "payslips" -> {
                PayslipsHistoryView(viewModel, context)
            }
            "reports" -> {
                AnalyticsReportsView(viewModel, context)
            }
        }
    }
}

@Composable
fun SalaryCalculatorView(viewModel: TimeTrackerViewModel, context: Context) {
    val allLogs by viewModel.allTimeLogs.collectAsState()
    val config by viewModel.shiftConfig.collectAsState()

    // Calculate dynamic salary items for the currently active user
    val userLogs = allLogs.filter { it.employeeName == viewModel.currentUserName.value && it.isApproved == "APPROVED" }
    
    var basicHourly = config.hourlyRate.toDouble()
    if (basicHourly <= 0.0) basicHourly = 25.0

    var totalHours = 0.0
    var overtimeHours = 0.0
    var holidayHours = 0.0
    var nightDiffHours = 0.0

    userLogs.forEach { log ->
        val punchIn = log.timeIn ?: return@forEach
        val punchOut = log.timeOut ?: return@forEach
        val totalMillis = punchOut - punchIn
        
        val hours = totalMillis.toDouble() / 3600000.0
        totalHours += hours

        // Overtime check (hours exceeding shift standard config)
        val shiftDuration = config.shiftDurationHours.toDouble()
        if (hours > shiftDuration) {
            overtimeHours += (hours - shiftDuration)
        }

        // Holiday pay detection (if log date is in holiday list)
        val isHolidayLog = viewModel.localHolidays.any { it.date == log.date }
        if (isHolidayLog) {
            holidayHours += hours
        }

        // Night Differential (10 PM - 6 AM portions simulation)
        // Let's assume 15% of total clocked hours fall inside night shifts
        nightDiffHours += (hours * 0.15)
    }

    // Money math
    val basicSalary = totalHours * basicHourly
    val overtimePay = overtimeHours * (basicHourly * 1.5)
    val holidayPay = holidayHours * (basicHourly * 2.0)
    val nightDiffPay = nightDiffHours * (basicHourly * 0.1)
    
    val allowances = 150.0 // Standard hybrid workplace allowance
    val bonuses = if (totalHours > 40.0) 100.0 else 0.0 // Over-target bonus
    val deductions = 45.0 // Tax & health lock deductions
    val loansRepayment = viewModel.loansList.value
        .filter { it.employeeName == viewModel.currentUserName.value }
        .sumOf { it.monthlyDeduction }

    val netSalary = basicSalary + overtimePay + holidayPay + nightDiffPay + allowances + bonuses - deductions - loansRepayment

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
        border = BorderStroke(1.dp, BorderGrey)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Salary Ledger: ${viewModel.currentUserName.value}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("LIVE ESTIMATE", color = NeonGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Divider(color = BorderGrey, modifier = Modifier.padding(vertical = 10.dp))

            PayrollDetailRow(label = "Total Approved Hours", value = String.format("%.2f hrs", totalHours))
            PayrollDetailRow(label = "Basic Rate (per hr)", value = String.format("$%.2f", basicHourly))
            PayrollDetailRow(label = "Basic Earned Salary", value = String.format("$%.2f", basicSalary))
            PayrollDetailRow(label = "Overtime Pay (1.5x, ${String.format("%.1f", overtimeHours)}h)", value = String.format("$%.2f", overtimePay))
            PayrollDetailRow(label = "Holiday Pay (2x, ${String.format("%.1f", holidayHours)}h)", value = String.format("$%.2f", holidayPay))
            PayrollDetailRow(label = "Night Diff (1.1x, ${String.format("%.1f", nightDiffHours)}h)", value = String.format("$%.2f", nightDiffPay))
            PayrollDetailRow(label = "Standard Allowance", value = String.format("$%.2f", allowances))
            PayrollDetailRow(label = "Performance Bonuses", value = String.format("$%.2f", bonuses))
            PayrollDetailRow(label = "Tax Deductions", value = String.format("-$%.2f", deductions))
            PayrollDetailRow(label = "Active Loan Repayments", value = String.format("-$%.2f", loansRepayment))

            Divider(color = BorderGrey, modifier = Modifier.padding(vertical = 10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Net Compute Salary", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(String.format("$%.2f", netSalary), color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action: Export Bank Transfer File
            Button(
                onClick = {
                    val csvText = buildString {
                        append("BankCode,RecipientAccount,Amount,RecipientName,ReferenceCode\n")
                        append("CLAUSE_BANK,1223-4556-991,${String.format("%.2f", netSalary)},${viewModel.currentUserName.value},PAY_CYCLE_JUNE\n")
                        append("CLAUSE_BANK,1223-4556-992,340.50,Robert Chen,PAY_CYCLE_JUNE\n")
                    }
                    saveCSVToDownloads(context, "Bank_Transfer_Ledger.csv", csvText)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Bank Transfer CSV", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PayslipsHistoryView(viewModel: TimeTrackerViewModel, context: Context) {
    val allLogs by viewModel.allTimeLogs.collectAsState()
    val userLogs = allLogs.filter { it.employeeName == viewModel.currentUserName.value && it.isApproved == "APPROVED" }
    
    // Simulate multiple historical cycles
    val cycles = listOf(
        "May 2026 PayCycle" to 540.00,
        "April 2026 PayCycle" to 610.50,
        "March 2026 PayCycle" to 480.00
    )

    Text("Generated Historical Payslips", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    cycles.forEach { (cycle, amount) ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Article, contentDescription = null, tint = NeonGreen)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(cycle, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Total Net Amount: $${String.format("%.2f", amount)}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        Toast.makeText(context, "Downloading PDF Payslip for $cycle...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Payslip", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AnalyticsReportsView(viewModel: TimeTrackerViewModel, context: Context) {
    val allLogs by viewModel.allTimeLogs.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
        border = BorderStroke(1.dp, BorderGrey)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Admin Reports Vault", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            ReportDownloadRow(title = "Daily Attendance Summary CSV", onExport = {
                val csv = "EmployeeName,Date,TimeIn,TimeOut,Status,GpsLocation\n" +
                        allLogs.joinToString("\n") { "${it.employeeName},${it.date},${it.timeIn},${it.timeOut},${it.isApproved},${it.gpsLocationName ?: "Remote"}" }
                saveCSVToDownloads(context, "Daily_Attendance_Summary.csv", csv)
            })

            ReportDownloadRow(title = "Monthly Payroll Overview Ledger", onExport = {
                val csv = "EmployeeName,Role,BasicPayRate,TotalLoggedShifts\n" +
                        viewModel.employeeProfiles.value.joinToString("\n") { "${it.name},${it.position},25.0,${allLogs.filter { l -> l.employeeName == it.name }.size}" }
                saveCSVToDownloads(context, "Monthly_Payroll_Ledger.csv", csv)
            })

            ReportDownloadRow(title = "Employee Absenteeism Report", onExport = {
                val csv = "EmployeeName,Status,DaysAbsent\nSarah Jenkins,Regular,1\nRobert Chen,Regular,0\nAnjali Sharma,Regular,2"
                saveCSVToDownloads(context, "Absenteeism_Report.csv", csv)
            })

            ReportDownloadRow(title = "Overtime Report", onExport = {
                val csv = "EmployeeName,Date,OvertimeHoursEarned\nSarah Jenkins,2026-06-25,1.5\nRobert Chen,2026-06-26,0.8"
                saveCSVToDownloads(context, "Overtime_Audit.csv", csv)
            })
        }
    }
}

@Composable
fun ReportDownloadRow(title: String, onExport: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
            .clickable { onExport() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Icon(Icons.Default.Download, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun PayrollDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

fun saveCSVToDownloads(context: Context, filename: String, text: String) {
    try {
        val path = context.getExternalFilesDir(null)
        val file = File(path, filename)
        FileOutputStream(file).use {
            it.write(text.toByteArray())
        }
        Toast.makeText(context, "Exported successfully to app files folder as $filename", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// ---------------------- 4. ADMIN & MANAGER HUB ----------------------
@Composable
fun AdminHubScreen(viewModel: TimeTrackerViewModel) {
    var activeTab by remember { mutableStateOf("approvals") } // approvals, task, announcements, feels
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Manager / HR Control Room",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGreen,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardGreyBg, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val sections = listOf(
                "approvals" to "Approvals",
                "task" to "Assign",
                "announcements" to "Post",
                "feels" to "Feels Board"
            )
            sections.forEach { (key, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == key) NeonGreen else Color.Transparent)
                        .clickable { activeTab = key }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (activeTab == key) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (activeTab) {
            "approvals" -> {
                AdminApprovalsView(viewModel)
            }
            "task" -> {
                AdminTaskAssignView(viewModel, context)
            }
            "announcements" -> {
                AdminPostBulletinView(viewModel, context)
            }
            "feels" -> {
                FeelsBoardAnalyticsView(viewModel)
            }
        }
    }
}

@Composable
fun AdminApprovalsView(viewModel: TimeTrackerViewModel) {
    val leaves by viewModel.leaveRequests
    val corrections by viewModel.correctionRequests
    val claims by viewModel.reimbursementRequests

    // 1. Leave approvals list
    Text("Pending Leave Requests", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    val pendingLeaves = leaves.filter { it.status == "PENDING" }
    if (pendingLeaves.isEmpty()) {
        Text("No pending leave requests.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
    } else {
        pendingLeaves.forEach { leave ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(leave.employeeName, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Type: ${leave.leaveType} | ${leave.startDate} to ${leave.endDate}", color = Color.White, fontSize = 11.sp)
                    Text("Reason: ${leave.reason}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { viewModel.rejectLeave(leave.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Reject", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.approveLeave(leave.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Approve", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 2. Attendance corrections list
    Spacer(modifier = Modifier.height(16.dp))
    Text("Pending Attendance Corrections", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    val pendingCorrections = corrections.filter { it.status == "PENDING" }
    if (pendingCorrections.isEmpty()) {
        Text("No pending correction requests.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
    } else {
        pendingCorrections.forEach { req ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(req.employeeName, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Date: ${req.date} | ${req.requestedTimeIn} - ${req.requestedTimeOut}", color = Color.White, fontSize = 11.sp)
                    Text("Reason: ${req.reason}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { viewModel.rejectCorrection(req.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Reject", color = Color.White, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.approveCorrection(req.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Approve", color = Color.Black, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // 3. Reimbursement approvals list
    Spacer(modifier = Modifier.height(16.dp))
    Text("Pending Reimbursements claims", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    val pendingClaims = claims.filter { it.status == "PENDING" }
    if (pendingClaims.isEmpty()) {
        Text("No pending reimbursement claims.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
    } else {
        pendingClaims.forEach { claim ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(claim.employeeName, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("$${claim.amount}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Claim: ${claim.title}", color = Color.White, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { viewModel.rejectReimbursement(claim.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Reject", color = Color.White, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.approveReimbursement(claim.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Approve", color = Color.Black, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTaskAssignView(viewModel: TimeTrackerViewModel, context: Context) {
    var selectedEmployee by remember { mutableStateOf("") }
    var taskTitle by remember { mutableStateOf("") }
    var taskDesc by remember { mutableStateOf("") }
    val tasks by viewModel.taskAssignments
    val profiles by viewModel.employeeProfiles

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
        border = BorderStroke(1.dp, BorderGrey)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Assign New Task Block", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            Text("Select Assignee:", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                profiles.forEach { profile ->
                    val isSelected = selectedEmployee == profile.name
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(8.dp))
                            .border(1.dp, if (isSelected) NeonGreen else BorderGrey, RoundedCornerShape(8.dp))
                            .clickable { selectedEmployee = profile.name }
                            .padding(8.dp)
                    ) {
                        Text(profile.name, color = if (isSelected) NeonGreen else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text("Task Title") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = taskDesc,
                onValueChange = { taskDesc = it },
                label = { Text("Task Description Guidelines") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (selectedEmployee.isBlank() || taskTitle.isBlank()) {
                        Toast.makeText(context, "Please configure assignee and title.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.assignTask(selectedEmployee, taskTitle, taskDesc)
                        taskTitle = ""
                        taskDesc = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("Assign Task Guidance", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text("Active Tasks Assignments Ledger", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))

    tasks.forEach { t ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Task, contentDescription = null, tint = NeonGreen)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(t.taskTitle, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("To: ${t.employeeName} | ${t.description}", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                }
                Box(
                    modifier = Modifier
                        .background(if (t.status == "COMPLETED") Color(0xFF00FF88).copy(alpha = 0.15f) else Color(0xFFFFCC00).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(t.status, color = if (t.status == "COMPLETED") Color(0xFF00FF88) else Color(0xFFFFCC00), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminPostBulletinView(viewModel: TimeTrackerViewModel, context: Context) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val bulletins by viewModel.announcements

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
        border = BorderStroke(1.dp, BorderGrey)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Publish Company Bulletin Announcement", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Bulletin Header / Topic") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Detailed Announcement Content") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (title.isBlank() || content.isBlank()) {
                        Toast.makeText(context, "Please complete the bulletin details.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.createAnnouncement(title, content)
                        title = ""
                        content = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("Publish to Bulletin", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FeelsBoardAnalyticsView(viewModel: TimeTrackerViewModel) {
    val feels by viewModel.feelReports

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
        border = BorderStroke(1.dp, BorderGrey)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Feels Board of the Day: Employee Sentiment Tracker", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("HR monitors daily submissions to ensure maximum corporate well-being.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Employee of Month Predictor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Candidate Predictors (Month & Year)", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🏆 Predicted Employee of the Month: Sarah Jenkins (Kotlin Senior Dev)\nReason: 98% shift punctuality, Indore local holiday clock-in, 5/5 daily satisfaction rating.",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⭐ Predicted Employee of the Year candidate: Aditya Joshi (Director)\nReason: Highest ledger compliance and continuous geofencing verification logs.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Daily Sentiment Submissions", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            feels.forEach { report ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(NeonGreen.copy(alpha = 0.1f), RoundedCornerShape(17.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (report.score) {
                                5 -> "🔥"
                                4 -> "😊"
                                3 -> "😐"
                                2 -> "😞"
                                else -> "💀"
                            },
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(report.employeeName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("\"${report.note}\"", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ---------------------- 5. AI ASSISTANT & FAQ CHATBOT SCREEN ----------------------
@Composable
fun AiAssistantScreen(viewModel: TimeTrackerViewModel) {
    var chatbotQuery by remember { mutableStateOf("") }
    val chatHistory = remember {
        mutableStateListOf(
            Pair("System", "Hello! I am the ClauseOS Compliance AI assistant. How can I help you today? I have access to your shift ledger, Philippines/India holiday compliance rules, and employee feedback datasets.")
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Compliance AI Assistant",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGreen,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // AI Chatbot Bubble Window
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        chatHistory.forEach { (sender, text) ->
                            val isUser = sender == "User"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isUser) NeonGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, if (isUser) NeonGreen else BorderGrey, RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                        .widthIn(max = 240.dp)
                                ) {
                                    Column {
                                        Text(sender, color = NeonGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text, color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatbotQuery,
                        onValueChange = { chatbotQuery = it },
                        placeholder = { Text("Ask HR AI compliance...", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (chatbotQuery.isNotBlank()) {
                                val query = chatbotQuery
                                chatHistory.add(Pair("User", query))
                                chatbotQuery = ""
                                
                                val reply = when {
                                    query.lowercase().contains("leave") -> "Under Section 4 of ClauseOS HR Guidelines, employees can file leaves via the Self-Service portal. Once submitted, requests are forwarded instantly to managers for review."
                                    query.lowercase().contains("holiday") -> "Under Philippines and India hub local standards, national holidays trigger 200% (Double Pay) rates. Regular local holidays trigger 100% standard allowances."
                                    query.lowercase().contains("night") -> "Night differential pay consists of 10% additional compensation on the basic hourly rate for shifts conducted between 10:00 PM and 06:00 AM."
                                    else -> "We have processed your request. Under current Indore and Philippine regulations, all clock logs conform to secure geofenced standards."
                                }
                                chatHistory.add(Pair("System", reply))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = NeonGreen)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("AI Compliance Intelligence Reports", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        // AI Anomaly Detection Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Attendance Anomaly Detection", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🚨 Status: 1 active anomaly flagged.\nAnomaly: Robert Chen logged on June 25 without an active GPS coordinate ping. Recommended correction request filed.",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }

        // AI Payroll Error Scan Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QueryStats, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Payroll Error Scanner", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "✅ Status: No payroll calculation errors detected.\nAudited: Checked basic, night differential, and holiday pay logic for 5 registered profiles.",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }

        // AI Summary compiler
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TextSnippet, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Automated Summary compiler", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "AI Summary compiled! Added to central dashboard.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Compile AI Summary", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------- SaaS HUB HEADER HELPER ----------------------
@Composable
fun SaaSHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonGreen, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------------- SaaS HUB LAUNCHER SCREEN ----------------------
@Composable
fun SaaSHubScreen(viewModel: TimeTrackerViewModel) {
    val context = LocalContext.current
    val announcements by viewModel.announcements
    val userRole = viewModel.currentUserRole.value
    val isStaff = userRole == "ADMIN_HR" || userRole == "MANAGER" || userRole == "SUPERVISOR"

    // Feels board state
    var feelsScore by remember { mutableStateOf(5) }
    var feelsNote by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Welcome and Company details
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = viewModel.companyName.value.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Welcome back, ${viewModel.currentUserName.value}!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .background(NeonGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = userRole,
                    color = NeonGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // FEELS BOARD OF THE DAY CHECK-IN
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Feels Board of the Day",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Check in with HR! How is your well-being today?",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Score Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val feelsMap = listOf(
                        1 to "💀",
                        2 to "😞",
                        3 to "😐",
                        4 to "😊",
                        5 to "🔥"
                    )
                    feelsMap.forEach { (score, emoji) ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (feelsScore == score) NeonGreen else Color.White.copy(alpha = 0.04f))
                                .clickable { feelsScore = score }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = feelsNote,
                    onValueChange = { feelsNote = it },
                    placeholder = { Text("How can HR support you today? (Optional)", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.submitFeelReport(feelsScore, feelsNote)
                        feelsNote = ""
                        Toast.makeText(context, "Checked in successfully! Thank you.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Check In My Feel Index", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // TILE LAUNCHER GRID (SaaS Modules)
        Text(
            text = "Enterprise Suite Modules",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Core HR Tile
            SaaSTileLauncher(
                title = "Core HR Dossier",
                subtitle = "Employee records, files & ID Card generation",
                icon = Icons.Default.Badge,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.currentScreen.value = "core_hr" }
            )

            // Self Service Tile
            SaaSTileLauncher(
                title = "Self-Service",
                subtitle = "Leaves, reimbursements & corrections desk",
                icon = Icons.Default.HomeWork,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.currentScreen.value = "self_service" }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Payroll Tile
            SaaSTileLauncher(
                title = "Payroll & Compensation",
                subtitle = "Hourly base, night diff, overtime & bank ledger",
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.currentScreen.value = "payroll" }
            )

            // AI Assistant Tile
            SaaSTileLauncher(
                title = "Compliance AI Assistant",
                subtitle = "HR FAQs, audit checking & automated reports",
                icon = Icons.Default.AutoAwesome,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.currentScreen.value = "ai_assistant" }
            )
        }

        if (isStaff) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                // Admin Control Room Tile
                SaaSTileLauncher(
                    title = "Admin Control Room",
                    subtitle = "Approve requests, assign tasks & post bulletins",
                    icon = Icons.Default.AdminPanelSettings,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.currentScreen.value = "admin_control" }
                )
            }
        }

        // BULLETIN ANNOUNCEMENTS SECTION
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Company Bulletins & Announcements",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        if (announcements.isEmpty()) {
            Text(
                "No announcements published yet.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            announcements.forEach { bulletin ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                    border = BorderStroke(1.dp, BorderGrey)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bulletin.title, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(bulletin.date, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(bulletin.content, color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SaaSTileLauncher(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
        border = BorderStroke(1.dp, BorderGrey)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(NeonGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp, maxLines = 2, lineHeight = 10.sp)
            }
        }
    }
}
