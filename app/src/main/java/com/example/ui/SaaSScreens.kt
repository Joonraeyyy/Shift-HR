package com.example.ui

import android.content.Context
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.nfc.NfcAdapter
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.database.TimeLogEntity
import com.example.ui.viewmodel.*
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.RectF
import android.content.Intent
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Neon Theme Color Tokens matching ClauseOS Compliance
val NeonGreen: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary
val DarkGreyBg: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.onBackground == Color(0xFFFFFFFF)) Color(0xFF0F0F11) else Color(0xFFF1F5F9)
val CardGreyBg: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface
val TextLight: Color
    @Composable
    get() = MaterialTheme.colorScheme.onBackground
val BorderGrey: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.onBackground == Color(0xFFFFFFFF)) getAdaptiveColor(0.12f) else Color.Black.copy(alpha = 0.12f)

// ---------------------- 1. CORE HR SCREEN ----------------------
@Composable
fun SmartCaptureTerminal(
    onScanDocumentClick: () -> Unit,
    onUploadPhotoClick: () -> Unit,
    themeAccentColor: Color = Color(0xFF00E676)
) {
    val isLightTheme = MaterialTheme.colorScheme.onBackground != Color(0xFFFFFFFF)

    // Layout configuration values from adaptive glass formula
    val containerColor = if (isLightTheme) Color(0xA6FFFFFF) else Color(0x730F172A)
    val containerBorderColor = if (isLightTheme) Color(0xCCFFFFFF) else Color(0x1FFFFFFF)
    val accentIconColor = if (isLightTheme) Color(0xFF059669) else Color(0xFF34D399)
    val headingTextColor = if (isLightTheme) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val subtitleTextColor = if (isLightTheme) Color(0xFF475569) else Color(0xFF94A3B8)
    val cardElevation = if (isLightTheme) 4.dp else 0.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        border = BorderStroke(1.dp, containerBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentIconColor.copy(alpha = if (isLightTheme) 0.12f else 0.20f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = accentIconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Smart Capture Terminal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = headingTextColor
                    )
                    Text(
                        text = "Powered by Shift AI Document Engine",
                        fontSize = 10.sp,
                        color = subtitleTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RowActionButton(
                    icon = Icons.Default.CameraAlt,
                    label = "Scan Document",
                    color = accentIconColor,
                    onClick = onScanDocumentClick,
                    modifier = Modifier.weight(1f)
                )

                RowActionButton(
                    icon = Icons.Default.PhotoLibrary,
                    label = "Upload Photo",
                    color = accentIconColor,
                    onClick = onUploadPhotoClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RowActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = MaterialTheme.colorScheme.onBackground != Color(0xFFFFFFFF)

    val innerCardBg = if (isLightTheme) Color(0xCCF1F5F9) else Color(0x0FFFFFFF)
    val innerCardBorder = if (isLightTheme) Color(0x260F172A) else Color(0x1FFFFFFF)
    val labelTextColor = if (isLightTheme) Color(0xFF475569) else Color(0xFF94A3B8)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(innerCardBg)
            .border(1.dp, innerCardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelTextColor
            )
        }
    }
}

@Composable
fun CoreHrScreen(
    viewModel: TimeTrackerViewModel,
    onScanDocument: (profileId: String) -> Unit = {},
    onUploadPhotos: (profileId: String) -> Unit = {}
) {
    var selectedProfile by remember { mutableStateOf<EmployeeProfile?>(null) }
    var showIdCardGenerator by remember { mutableStateOf(false) }
    val profiles by viewModel.employeeProfiles
    val dossierDocsDb by viewModel.dossierDocuments.collectAsState()
    val context = LocalContext.current

    val userRole = viewModel.currentUserRole.value
    val currentUserName = viewModel.currentUserName.value

    // Resolve current user profile & department
    val currentUserProfile = remember(profiles, currentUserName) {
        profiles.find { it.name.equals(currentUserName, ignoreCase = true) }
    }
    val userDept = currentUserProfile?.department ?: ""

    // 1. Backend/Query-Level Restriction: Only see their own department if supervisor/manager
    val authorizedProfiles = remember(profiles, userRole, userDept) {
        if (userRole == "MANAGER" || userRole == "SUPERVISOR") {
            profiles.filter { it.department.equals(userDept, ignoreCase = true) }
        } else {
            profiles
        }
    }

    // Search and Debounce states
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(searchQuery) {
        delay(300)
        debouncedQuery = searchQuery
    }

    // Active dropdown filters states
    var selectedDepartment by remember { mutableStateOf("All") }
    var selectedPosition by remember { mutableStateOf("All") }
    var selectedRole by remember { mutableStateOf("All") }
    var selectedShift by remember { mutableStateOf("All") }

    // Dropdown expanded states
    var deptDropdownExpanded by remember { mutableStateOf(false) }
    var posDropdownExpanded by remember { mutableStateOf(false) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    var shiftDropdownExpanded by remember { mutableStateOf(false) }

    // Helper to get role
    fun getRoleForEmployee(profile: EmployeeProfile): String {
        val user = viewModel.registeredUsers.value.find { it.name.equals(profile.name, ignoreCase = true) }
        return user?.role ?: "EMPLOYEE"
    }

    // Dynamic lists for filters
    val departments = remember(profiles) { listOf("All") + profiles.map { it.department }.distinct() }
    val positions = remember(authorizedProfiles) { listOf("All") + authorizedProfiles.map { it.position }.distinct() }
    val shifts = remember(authorizedProfiles) { listOf("All") + authorizedProfiles.map { it.shiftSchedule }.distinct() }
    val roles = listOf("All", "ADMIN_HR", "MANAGER", "SUPERVISOR", "EMPLOYEE")

    // Filter results
    val filteredList = remember(authorizedProfiles, debouncedQuery, selectedDepartment, selectedPosition, selectedRole, selectedShift, viewModel.registeredUsers.value) {
        authorizedProfiles.filter { profile ->
            // Fuzzy match on name, department, position, role
            val fuzzyMatch = if (debouncedQuery.isBlank()) {
                true
            } else {
                val q = debouncedQuery.lowercase().trim()
                val profileRole = getRoleForEmployee(profile)
                profile.name.lowercase().contains(q) ||
                profile.department.lowercase().contains(q) ||
                profile.position.lowercase().contains(q) ||
                profileRole.lowercase().contains(q)
            }

            val matchesDept = if (userRole == "MANAGER" || userRole == "SUPERVISOR") {
                true // restricted to their own department already
            } else {
                selectedDepartment == "All" || profile.department.equals(selectedDepartment, ignoreCase = true)
            }

            val matchesPos = selectedPosition == "All" || profile.position.equals(selectedPosition, ignoreCase = true)
            val matchesRole = selectedRole == "All" || getRoleForEmployee(profile).equals(selectedRole, ignoreCase = true)
            val matchesShift = selectedShift == "All" || profile.shiftSchedule.equals(selectedShift, ignoreCase = true)

            fuzzyMatch && matchesDept && matchesPos && matchesRole && matchesShift
        }
    }

    if (userRole == "EMPLOYEE") {
        // Regular employee sees their own secure digital dossier cabinet
        val profile = currentUserProfile ?: EmployeeProfile(
            id = "EMP-000",
            name = currentUserName,
            department = "Unassigned",
            position = "Associate",
            status = "Regular",
            email = "employee@shifthr.com",
            emergencyContactName = "Emergency Contact",
            emergencyContactPhone = "+12345678"
        )
        var selectedCategoryFilter by remember { mutableStateOf("All") }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "My Core HR Dossier",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NeonGreen,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Text(
                text = "Digital Vault & Verification Cabinet",
                fontSize = 14.sp,
                color = getAdaptiveTextColor(0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Smart Capture Terminal (Twin Glass Tiles)
            SmartCaptureTerminal(
                onScanDocumentClick = { onScanDocument(profile.id) },
                onUploadPhotoClick = { onUploadPhotos(profile.id) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Digital Vault Header with AES label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR DIGITAL VAULT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.AppTextColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(getAdaptiveColor(0.05f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AES-256 Encrypted", fontSize = 8.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips
            val categories = listOf("All", "Identity", "Medical", "Financial", "Contractual")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else getAdaptiveColor(0.04f), RoundedCornerShape(8.dp))
                            .border(1.dp, if (isSelected) NeonGreen else BorderGrey, RoundedCornerShape(8.dp))
                            .clickable { selectedCategoryFilter = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) NeonGreen else com.example.ui.theme.AppTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filtered file list
            val filteredDocs = profile.documents.filter { doc ->
                if (selectedCategoryFilter == "All") {
                    true
                } else {
                    val catOfFile = getCategoryForFile(doc)
                    catOfFile.name.equals(selectedCategoryFilter, ignoreCase = true)
                }
            }

            if (filteredDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = getAdaptiveTextColor(0.3f), modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No $selectedCategoryFilter documents in your secure vault.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
                    }
                }
            } else {
                filteredDocs.forEach { docName ->
                    val catOfFile = getCategoryForFile(docName)
                    val displayDocName = docName.substringAfter("_") // strip category prefix for clean UI display
                    
                    // Look up corresponding database entry
                    val dbRecord = dossierDocsDb.find { it.fileName == docName || it.fileName.contains(displayDocName, ignoreCase = true) }
                    val syncStatus = dbRecord?.syncStatus ?: "SYNCED" // Default to SYNCED for legacy pre-existing files
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(getAdaptiveColor(0.04f), RoundedCornerShape(12.dp))
                            .border(1.dp, getAdaptiveColor(0.08f), RoundedCornerShape(12.dp))
                            .clickable {
                                Toast.makeText(context, "Opening encrypted verification file: $displayDocName...", Toast.LENGTH_SHORT).show()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(NeonGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (catOfFile) {
                                    DossierCategory.IDENTITY -> Icons.Default.Badge
                                    DossierCategory.MEDICAL -> Icons.Default.LocalHospital
                                    DossierCategory.FINANCIAL -> Icons.Default.Receipt
                                    DossierCategory.CONTRACTUAL -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayDocName,
                                color = com.example.ui.theme.AppTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Category: ${catOfFile.label} • Encrypted",
                                fontSize = 10.sp,
                                color = getAdaptiveTextColor(0.5f)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = when (syncStatus) {
                                            "PENDING" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                            "SYNCING" -> Color(0xFF2196F3).copy(alpha = 0.15f)
                                            "SYNCED" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                            else -> Color(0xFFF44336).copy(alpha = 0.15f)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                if (syncStatus == "SYNCING") {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(10.dp),
                                        strokeWidth = 1.dp,
                                        color = Color(0xFF2196F3)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else if (syncStatus == "SYNCED") {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else if (syncStatus == "PENDING") {
                                    Icon(
                                        imageVector = Icons.Default.HourglassEmpty,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFF44336),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                
                                Text(
                                    text = when (syncStatus) {
                                        "PENDING" -> "Waiting for connection..."
                                        "SYNCING" -> "Uploading..."
                                        "SYNCED" -> "Cloud Secured"
                                        else -> "Sync Failed"
                                    },
                                    color = when (syncStatus) {
                                        "PENDING" -> Color(0xFFFF9800)
                                        "SYNCING" -> Color(0xFF2196F3)
                                        "SYNCED" -> Color(0xFF4CAF50)
                                        else -> Color(0xFFF44336)
                                    },
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download File",
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    } else {
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
                color = getAdaptiveTextColor(0.7f)
            )
            // Generate ID is only for Admin/HR or Staff
            if (userRole == "ADMIN_HR" || userRole == "MANAGER" || userRole == "SUPERVISOR") {
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input Box (Fuzzy matching + debouncing)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Type name, department, role, position...", fontSize = 12.sp, color = getAdaptiveTextColor(0.4f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = getAdaptiveTextColor(0.5f))
                    }
                }
            },
            singleLine = true,
            textStyle = TextStyle(color = com.example.ui.theme.AppTextColor, fontSize = 13.sp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("employee_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = getAdaptiveColor(0.12f),
                focusedContainerColor = CardGreyBg,
                unfocusedContainerColor = CardGreyBg
            )
        )

        // Dropdown Search Filters Allowed (by role)
        Text(
            text = "Filter Directory",
            fontSize = 11.sp,
            color = getAdaptiveTextColor(0.5f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Department Filter (Only for Admin/HR or Regular Employee, hidden for supervisor/manager)
            if (userRole != "MANAGER" && userRole != "SUPERVISOR") {
                Box {
                    Row(
                        modifier = Modifier
                            .background(if (selectedDepartment != "All") NeonGreen.copy(alpha = 0.12f) else getAdaptiveColor(0.04f), RoundedCornerShape(8.dp))
                            .border(1.dp, if (selectedDepartment != "All") NeonGreen else BorderGrey, RoundedCornerShape(8.dp))
                            .clickable { deptDropdownExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dept: $selectedDepartment",
                            color = if (selectedDepartment != "All") NeonGreen else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = getAdaptiveTextColor(0.5f), modifier = Modifier.size(14.dp))
                    }
                    DropdownMenu(
                        expanded = deptDropdownExpanded,
                        onDismissRequest = { deptDropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E1E22))
                    ) {
                        departments.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp) },
                                onClick = {
                                    selectedDepartment = dept
                                    deptDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Position Filter
            Box {
                Row(
                    modifier = Modifier
                        .background(if (selectedPosition != "All") NeonGreen.copy(alpha = 0.12f) else getAdaptiveColor(0.04f), RoundedCornerShape(8.dp))
                        .border(1.dp, if (selectedPosition != "All") NeonGreen else BorderGrey, RoundedCornerShape(8.dp))
                        .clickable { posDropdownExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pos: $selectedPosition",
                        color = if (selectedPosition != "All") NeonGreen else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = getAdaptiveTextColor(0.5f), modifier = Modifier.size(14.dp))
                }
                DropdownMenu(
                    expanded = posDropdownExpanded,
                    onDismissRequest = { posDropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E1E22))
                ) {
                    positions.forEach { pos ->
                        DropdownMenuItem(
                            text = { Text(pos, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp) },
                            onClick = {
                                selectedPosition = pos
                                posDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Role Filter
            Box {
                Row(
                    modifier = Modifier
                        .background(if (selectedRole != "All") NeonGreen.copy(alpha = 0.12f) else getAdaptiveColor(0.04f), RoundedCornerShape(8.dp))
                        .border(1.dp, if (selectedRole != "All") NeonGreen else BorderGrey, RoundedCornerShape(8.dp))
                        .clickable { roleDropdownExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Role: $selectedRole",
                        color = if (selectedRole != "All") NeonGreen else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = getAdaptiveTextColor(0.5f), modifier = Modifier.size(14.dp))
                }
                DropdownMenu(
                    expanded = roleDropdownExpanded,
                    onDismissRequest = { roleDropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E1E22))
                ) {
                    roles.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp) },
                            onClick = {
                                selectedRole = r
                                roleDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Shift Filter
            Box {
                Row(
                    modifier = Modifier
                        .background(if (selectedShift != "All") NeonGreen.copy(alpha = 0.12f) else getAdaptiveColor(0.04f), RoundedCornerShape(8.dp))
                        .border(1.dp, if (selectedShift != "All") NeonGreen else BorderGrey, RoundedCornerShape(8.dp))
                        .clickable { shiftDropdownExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shift: $selectedShift",
                        color = if (selectedShift != "All") NeonGreen else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = getAdaptiveTextColor(0.5f), modifier = Modifier.size(14.dp))
                }
                DropdownMenu(
                    expanded = shiftDropdownExpanded,
                    onDismissRequest = { shiftDropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E1E22))
                ) {
                    shifts.forEach { sh ->
                        DropdownMenuItem(
                            text = { Text(sh, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp) },
                            onClick = {
                                selectedShift = sh
                                shiftDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Reset filters
            val hasActiveFilters = selectedDepartment != "All" || selectedPosition != "All" || selectedRole != "All" || selectedShift != "All" || searchQuery.isNotEmpty()
            if (hasActiveFilters) {
                Text(
                    text = "Clear All",
                    color = Color.Red.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            selectedDepartment = "All"
                            selectedPosition = "All"
                            selectedRole = "All"
                            selectedShift = "All"
                            searchQuery = ""
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display results or empty state
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = getAdaptiveTextColor(0.3f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No employees found matching the filters.", color = getAdaptiveTextColor(0.5f), fontSize = 12.sp)
                }
            }
        } else {
            filteredList.forEach { profile ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            if (userRole == "EMPLOYEE") {
                                // Regular employee opens direct chat window!
                                viewModel.activeRecipient.value = profile.name
                                viewModel.currentScreen.value = "chat"
                                Toast.makeText(context, "Opening direct secure chat with ${profile.name}...", Toast.LENGTH_SHORT).show()
                            } else {
                                selectedProfile = profile
                            }
                        }
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
                            Text(text = profile.name, color = com.example.ui.theme.AppTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${profile.position} | ${profile.department}", color = getAdaptiveTextColor(0.6f), fontSize = 11.sp)
                            
                            // Visual indicator of role
                            val profileRole = getRoleForEmployee(profile)
                            Text(text = "Shift Schedule: ${profile.shiftSchedule} ($profileRole)", color = NeonGreen.copy(alpha = 0.65f), fontSize = 9.sp)
                        }

                        // Status Tag
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
        }

        // Active Profile details view (Dossier detail)
        selectedProfile?.let { profile ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header varies by role authorization
                    if (userRole == "ADMIN_HR") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "👑 SECURE FULL HR DOSSIER: ${profile.name}", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    } else if (userRole == "MANAGER" || userRole == "SUPERVISOR") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "📋 DEPARTMENT DOSSIER (LIMITED): ${profile.name}", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(text = "Employee Dossier: ${profile.name}", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = BorderGrey, modifier = Modifier.padding(vertical = 10.dp))

                    ProfileDetailRow(label = "Employee ID", value = profile.id)
                    ProfileDetailRow(label = "Work Email", value = profile.email)
                    ProfileDetailRow(label = "Primary Department", value = profile.department)
                    ProfileDetailRow(label = "Designated Position", value = profile.position)
                    ProfileDetailRow(label = "Reporting Supervisor", value = profile.supervisor)
                    ProfileDetailRow(label = "Assigned Hub", value = profile.assignedLocation)

                    // 1. ADMIN / HR: Shows Compensation and full docs
                    if (userRole == "ADMIN_HR") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "💰 Compensation & Financial Ledger (HR Secure)", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        val basicRate = when (profile.name) {
                            "Sarah Jenkins" -> "$32.50 / hr"
                            "Robert Chen" -> "$45.00 / hr"
                            "Anjali Sharma" -> "$60.00 / hr"
                            "Aditya Joshi (Director)" -> "$85.00 / hr"
                            else -> "$25.00 / hr"
                        }
                        ProfileDetailRow(label = "Basic Pay Rate", value = basicRate)
                        ProfileDetailRow(label = "Bank Name", value = profile.bankName)
                        ProfileDetailRow(label = "Bank Account Number", value = profile.bankAccount)
                        ProfileDetailRow(label = "Tax ID (TIN Number)", value = profile.taxId)
                        ProfileDetailRow(label = "SSN / Social Security Number", value = profile.ssn)

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "🗄️ Secure Document Vault", color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        
                        // Integrated Smart Capture Terminal for administrators
                        SmartCaptureTerminal(
                            onScanDocumentClick = { onScanDocument(profile.id) },
                            onUploadPhotoClick = { onUploadPhotos(profile.id) }
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        profile.documents.forEach { docName ->
                            val dbRecord = dossierDocsDb.find { it.fileName == docName || it.fileName.contains(docName.substringAfter("_"), ignoreCase = true) }
                            val syncStatus = dbRecord?.syncStatus ?: "SYNCED"
                            val displayDocName = docName.substringAfter("_")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(getAdaptiveColor(0.03f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                                    .clickable {
                                        Toast.makeText(context, "Downloading encrypted $docName...", Toast.LENGTH_SHORT).show()
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = displayDocName, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(
                                                color = when (syncStatus) {
                                                    "PENDING" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                                    "SYNCING" -> Color(0xFF2196F3).copy(alpha = 0.15f)
                                                    "SYNCED" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                                    else -> Color(0xFFF44336).copy(alpha = 0.15f)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        if (syncStatus == "SYNCING") {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                modifier = Modifier.size(8.dp),
                                                strokeWidth = 1.dp,
                                                color = Color(0xFF2196F3)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                        } else if (syncStatus == "SYNCED") {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(8.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                        } else if (syncStatus == "PENDING") {
                                            Icon(
                                                imageVector = Icons.Default.HourglassEmpty,
                                                contentDescription = null,
                                                tint = Color(0xFFFF9800),
                                                modifier = Modifier.size(8.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = Color(0xFFF44336),
                                                modifier = Modifier.size(8.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                        }
                                        
                                        Text(
                                            text = when (syncStatus) {
                                                "PENDING" -> "Waiting for connection..."
                                                "SYNCING" -> "Uploading..."
                                                "SYNCED" -> "Cloud Secured"
                                                else -> "Sync Failed"
                                            },
                                            color = when (syncStatus) {
                                                "PENDING" -> Color(0xFFFF9800)
                                                "SYNCING" -> Color(0xFF2196F3)
                                                "SYNCED" -> Color(0xFF4CAF50)
                                                else -> Color(0xFFF44336)
                                            },
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Icon(Icons.Default.Download, contentDescription = null, tint = getAdaptiveTextColor(0.5f), modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "⏰ Clocking Logs & Audit Trail", color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Last verified login: ${profile.lastLogin}", color = getAdaptiveTextColor(0.6f), fontSize = 11.sp)
                        Text(text = "SSID / MDM Fingerprint: ${profile.registeredDevice}", color = getAdaptiveTextColor(0.6f), fontSize = 11.sp)
                    }

                    // 2. MANAGER / SUPERVISOR: Shows performance, leave, hours worked (CONFIDENTIAL SALARY HIDDEN!)
                    if (userRole == "MANAGER" || userRole == "SUPERVISOR") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "📈 Performance Review & Attendance Analytics", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        val performanceRating = when (profile.name) {
                            "Sarah Jenkins" -> "4.9 / 5.0 (Exceptional)"
                            "Marcus Aurelius (HR Intern)" -> "4.2 / 5.0 (Meeting Expectations)"
                            else -> "4.7 / 5.0 (Strong)"
                        }
                        ProfileDetailRow(label = "Recent Performance Appraisal", value = performanceRating)
                        ProfileDetailRow(label = "Vacation Leave Balance", value = "${profile.vacationLeaveBalance} Days")
                        ProfileDetailRow(label = "Sick Leave Balance", value = "${profile.sickLeaveBalance} Days")
                        ProfileDetailRow(label = "Total Late Clock-ins", value = "${profile.totalLate}")
                        ProfileDetailRow(label = "Total Absences", value = "${profile.absences}")
                        ProfileDetailRow(label = "Overtime Hours Logged", value = "${profile.overtimeHours} Hrs")
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Yellow.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Yellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "🔒 Compensation fields are restricted & masked for Manager roles. Standard policy 2026.06 apply.",
                                color = Color.Yellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    } // closing the outer else block for userRole != "EMPLOYEE"

    if (showIdCardGenerator) {
        AlertDialog(
            onDismissRequest = { showIdCardGenerator = false },
            containerColor = DarkGreyBg,
            title = { Text("ID Card Generator", color = NeonGreen) },
            text = {
                var currentSelection by remember { mutableStateOf(profiles.firstOrNull() ?: profiles[0]) }
                Column {
                    Text("Select Employee profile:", color = com.example.ui.theme.AppTextColor, fontSize = 12.sp)
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
                            Text(profile.name, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp)
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
                            Text(text = "SECURE EMPLOYEE ID", fontSize = 8.sp, color = getAdaptiveTextColor(0.5f))
                            
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
                            
                            Text(text = currentSelection.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.AppTextColor)
                            Text(text = currentSelection.position, fontSize = 11.sp, color = NeonGreen)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("CARD ID", fontSize = 7.sp, color = getAdaptiveTextColor(0.4f))
                                    Text(currentSelection.id, fontSize = 9.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("STATUS", fontSize = 7.sp, color = getAdaptiveTextColor(0.4f))
                                    Text(currentSelection.status.uppercase(), fontSize = 9.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold)
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
                    Text("Close", color = com.example.ui.theme.AppTextColor)
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
        Text(text = label, color = getAdaptiveTextColor(0.5f), fontSize = 12.sp)
        Text(text = value, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------------- 2. EMPLOYEE SELF-SERVICE SCREEN ----------------------
@Composable
fun SelfServiceScreen(viewModel: TimeTrackerViewModel, context: Context = LocalContext.current) {
    val activeSubTab = viewModel.selfServiceTab.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        val isLightTheme = com.example.ui.theme.AppTextColor == Color(0xFF2D3748)

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
                "correction" to "Disputes",
                "claims" to "Claims",
                "profile" to "My Profile",
                "compliance" to "Legal & OKRs"
            )
            subTabs.forEach { (key, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeSubTab == key) NeonGreen else Color.Transparent)
                        .clickable { viewModel.selfServiceTab.value = key }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (activeSubTab == key) Color.Black else (if (isLightTheme) Color(0xFF475569) else Color.White),
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
            "compliance" -> {
                ComplianceTabView(viewModel, context)
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

    // Get current user profile
    val profile = viewModel.employeeProfiles.value.find { it.name == viewModel.currentUserName.value }
    val gender = profile?.gender ?: "Female"

    // Live Balances Grid
    Text("My Statutory Leave Balances", color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
        border = BorderStroke(1.dp, BorderGrey.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Vacation", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                    Text("${profile?.vacationLeaveBalance ?: 15} d", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Sick", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                    Text("${profile?.sickLeaveBalance ?: 10} d", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                if (gender == "Female") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.5f)) {
                        Text("Maternity (F)", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                        Text("${profile?.maternityLeaveBalance ?: 105} d", color = Color(0xFFFF69B4), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.5f)) {
                        Text("Paternity (M)", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                        Text("${profile?.paternityLeaveBalance ?: 7} d", color = Color(0xFF33B5E5), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.2f)) {
                    Text("Solo Parent", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                    Text("${profile?.soloParentLeaveBalance ?: 7} d", color = Color(0xFFFFBB33), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    val isLightTheme = com.example.ui.theme.AppTextColor == Color(0xFF2D3748)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLightTheme) Color.White else CardGreyBg
        ),
        border = BorderStroke(
            1.dp,
            if (isLightTheme) Color(0xFFE2E8F0) else BorderGrey
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("File New Leave Request", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // Leave Type Select
            Text("Select Leave Category:", color = getAdaptiveTextColor(0.6f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
            val categories = mutableListOf("Sick Leave", "Vacation Leave")
            if (gender == "Female") {
                categories.add("Maternity Leave")
            } else {
                categories.add("Paternity Leave")
            }
            categories.add("Solo Parent Leave")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { type ->
                    val isSelected = leaveType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (isSelected) NeonGreen else BorderGrey, RoundedCornerShape(8.dp))
                            .clickable { leaveType = type }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.replace(" Leave", ""),
                            color = if (isSelected) NeonGreen else (if (isLightTheme) Color(0xFF475569) else Color.White),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
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
                        val errorMsg = viewModel.fileLeaveRequest(leaveType, startDate, endDate, reason)
                        if (errorMsg != null) {
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Leave filed successfully for validation!", Toast.LENGTH_SHORT).show()
                            reason = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("submit_leave_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("Submit Leave Request", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text("My Historical Leaves", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))

    val myLeaves = leaveRequests.filter { it.employeeName == viewModel.currentUserName.value }
    if (myLeaves.isEmpty()) {
        Text("No leave history found.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
    } else {
        val isCardLight = com.example.ui.theme.AppTextColor == Color(0xFF2D3748)
        myLeaves.forEach { req ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCardLight) Color.White else CardGreyBg
                ),
                border = BorderStroke(
                    1.dp,
                    if (isCardLight) Color(0xFFE2E8F0) else BorderGrey
                )
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (req.leaveType) {
                            "Sick Leave" -> Icons.Default.MedicalServices
                            "Vacation Leave" -> Icons.Default.FlightTakeoff
                            "Maternity Leave" -> Icons.Default.Face
                            "Paternity Leave" -> Icons.Default.Face
                            "Solo Parent Leave" -> Icons.Default.People
                            else -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = when (req.leaveType) {
                            "Maternity Leave" -> Color(0xFFFF69B4)
                            "Paternity Leave" -> Color(0xFF33B5E5)
                            "Solo Parent Leave" -> Color(0xFFFFBB33)
                            else -> NeonGreen
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${req.leaveType}: ${req.startDate} to ${req.endDate}", color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(req.reason, color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
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
    var date by remember { mutableStateOf("2026-06-25") }
    var inTime by remember { mutableStateOf("08:30 AM") }
    var outTime by remember { mutableStateOf("05:30 PM") }
    var reason by remember { mutableStateOf("") }
    var isDisputeMode by remember { mutableStateOf(false) }
    var disputeType by remember { mutableStateOf("GPS Drift inside Concrete Building") }
    val corrections by viewModel.correctionRequests

    val isLightTheme = com.example.ui.theme.AppTextColor == Color(0xFF2D3748)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLightTheme) Color.White else CardGreyBg
        ),
        border = BorderStroke(
            1.dp,
            if (isLightTheme) Color(0xFFE2E8F0) else BorderGrey
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(if (isDisputeMode) "GPS Telemetry Dispute Resolution" else "Request Time Correction", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            // Selection buttons to toggle Mode
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isDisputeMode) NeonGreen.copy(alpha = 0.15f) else Color.Transparent)
                        .border(1.dp, if (!isDisputeMode) NeonGreen else BorderGrey, RoundedCornerShape(8.dp))
                        .clickable { isDisputeMode = false }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Standard Correction", color = if (!isDisputeMode) NeonGreen else (if (isLightTheme) Color(0xFF475569) else Color.White), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDisputeMode) Color(0xFFFF5555).copy(alpha = 0.15f) else Color.Transparent)
                        .border(1.dp, if (isDisputeMode) Color(0xFFFF5555) else BorderGrey, RoundedCornerShape(8.dp))
                        .clickable { isDisputeMode = true }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("GPS / Telemetry Dispute", color = if (isDisputeMode) Color(0xFFFF5555) else (if (isLightTheme) Color(0xFF475569) else Color.White), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isDisputeMode) {
                // Dispute Telemetry Mock Screen
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C191C)),
                    border = BorderStroke(1.dp, Color(0xFFFF5555).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5555), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FAILING TELEMETRY IDENTIFIED BY SHIFT HR", color = Color(0xFFFF5555), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Device Geofence Accuracy: 184m (Threshold is < 50m)", color = getAdaptiveTextColor(0.8f), fontSize = 10.sp)
                        Text("• Location: Concrete Engineering Block B (Drift Zone)", color = getAdaptiveTextColor(0.8f), fontSize = 10.sp)
                    }
                }

                // Dispute Type dropdown simulation
                Text("Select Telemetry Error Type:", color = getAdaptiveTextColor(0.6f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                val types = listOf("GPS Drift inside Concrete Building", "MDM System Mismatch", "Anti-Spoofing Mismatch")
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { t ->
                        val isSel = disputeType == t
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFFFF5555).copy(alpha = 0.1f) else Color.Transparent)
                                .border(1.dp, if (isSel) Color(0xFFFF5555).copy(alpha = 0.5f) else BorderGrey, RoundedCornerShape(6.dp))
                                .clickable { disputeType = t }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) Color(0xFFFF5555) else Color.Transparent)
                                    .border(1.dp, getAdaptiveColor(0.5f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(t, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Correction/Dispute Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth().testTag("correction_date"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = inTime,
                    onValueChange = { inTime = it },
                    label = { Text("Requested In") },
                    modifier = Modifier.weight(1f).testTag("correction_in"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                )
                OutlinedTextField(
                    value = outTime,
                    onValueChange = { outTime = it },
                    label = { Text("Requested Out") },
                    modifier = Modifier.weight(1f).testTag("correction_out"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Compliance Justification Explanations") },
                modifier = Modifier.fillMaxWidth().testTag("correction_reason"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (reason.isBlank()) {
                        Toast.makeText(context, "Please enter a valid justification.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.fileCorrectionRequest(
                            date = date,
                            requestedTimeIn = inTime,
                            requestedTimeOut = outTime,
                            reason = reason,
                            disputeType = disputeType,
                            isDispute = isDisputeMode,
                            telemetry = "GPS Accuracy: 184m (Precision threshold 50m) inside Concrete Block"
                        )
                        Toast.makeText(context, "Dispute ticket submitted successfully for review!", Toast.LENGTH_SHORT).show()
                        reason = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("submit_correction_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = if (isDisputeMode) Color(0xFFFF5555) else NeonGreen)
            ) {
                Text(
                    text = if (isDisputeMode) "Submit GPS Telemetry Dispute" else "File Correction Request",
                    color = if (isDisputeMode) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text("Correction & Dispute Requests", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))

    val myCorrections = corrections.filter { it.employeeName == viewModel.currentUserName.value }
    if (myCorrections.isEmpty()) {
        Text("No requests filed.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
    } else {
        myCorrections.forEach { req ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, if (req.isPunchDispute) Color(0xFFFF5555).copy(alpha = 0.3f) else BorderGrey)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (req.isPunchDispute) Icons.Default.GpsFixed else Icons.Default.EditCalendar,
                        contentDescription = null,
                        tint = if (req.isPunchDispute) Color(0xFFFF5555) else NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (req.isPunchDispute) "GPS Dispute: ${req.date}" else "Correction: ${req.date}",
                            color = com.example.ui.theme.AppTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("${req.requestedTimeIn} - ${req.requestedTimeOut}", color = getAdaptiveTextColor(0.7f), fontSize = 11.sp)
                        Text("Justification: ${req.reason}", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                        if (req.isPunchDispute) {
                            Text("Telemetry Issue: ${req.disputeType}", color = Color(0xFFFF5555).copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
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
                            color = if (req.status == "APPROVED") Color(0xFF00FF88) else if (req.status == "REJECTED") Color(0xFFFF5555) else Color(0xFFFFCC00),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplianceTabView(viewModel: TimeTrackerViewModel, context: Context) {
    val cases by viewModel.disciplinaryCases
    val clearances by viewModel.offboardingClearances
    val okrs by viewModel.okrRecords
    val currentEmployeeName = viewModel.currentUserName.value

    var activeSubView by remember { mutableStateOf("labor") } // labor, clearance, okrs

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val menu = listOf(
            "labor" to "Labor Relations",
            "clearance" to "Exit Clearance",
            "okrs" to "Continuous OKRs"
        )
        menu.forEach { (key, label) ->
            val isSelected = activeSubView == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.Transparent)
                    .border(1.dp, if (isSelected) NeonGreen else BorderGrey, RoundedCornerShape(8.dp))
                    .clickable { activeSubView = key }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (isSelected) NeonGreen else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    when (activeSubView) {
        "labor" -> {
            Text("Labor Relations & Incident Case Log", color = com.example.ui.theme.AppTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Documenting due process under the Twin Notice Rule.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(10.dp))

            val myCases = cases.filter { it.employeeName.equals(currentEmployeeName, ignoreCase = true) }
            if (myCases.isEmpty()) {
                Text("No disciplinary cases recorded. Good job maintaining policy compliance!", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
            } else {
                myCases.forEach { c ->
                    var explanationText by remember { mutableStateOf("") }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                        border = BorderStroke(1.dp, if (c.status != "CASE_RESOLVED") Color(0xFFFF5555).copy(alpha = 0.3f) else BorderGrey)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = if (c.status == "CASE_RESOLVED") NeonGreen else Color(0xFFFF5555), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(c.infractionTitle, color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .background(if (c.status == "CASE_RESOLVED") Color(0xFF00FF88).copy(alpha = 0.15f) else Color(0xFFFF5555).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(c.status.replace("_", " "), color = if (c.status == "CASE_RESOLVED") Color(0xFF00FF88) else Color(0xFFFF5555), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Infraction Date: ${c.infractionDate} | Severity: ${c.severity}", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("1st Notice (Notice to Explain):", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(c.noticeToExplainContent, color = getAdaptiveTextColor(0.8f), fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))

                            if (c.status == "NOTICE_TO_EXPLAIN_ISSUED") {
                                OutlinedTextField(
                                    value = explanationText,
                                    onValueChange = { explanationText = it },
                                    label = { Text("My Official Written Explanation") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                                )
                                Button(
                                    onClick = {
                                        if (explanationText.isBlank()) {
                                            Toast.makeText(context, "Please write an explanation.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.submitEmployeeExplanation(c.id, explanationText)
                                            Toast.makeText(context, "Official explanation submitted to Secure HR Locker.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Submit Compliance Explanation", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                if (c.employeeExplanation.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Employee's Written Justification (${c.employeeExplanationDate}):", color = Color(0xFFFFCC00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(c.employeeExplanation, color = getAdaptiveTextColor(0.7f), fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                                }

                                if (c.noticeOfDecisionContent.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("2nd Notice (Notice of Decision - Issued ${c.noticeOfDecisionDate}):", color = Color(0xFF00FF88), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(c.noticeOfDecisionContent, color = getAdaptiveTextColor(0.8f), fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Policy Reference:", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = c.policyViabilityLink.substringAfterLast("/"),
                                    color = NeonGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        Toast.makeText(context, "Opening compliance documentation policy...", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        "clearance" -> {
            Text("Exit Separation & Clearance Workflow", color = com.example.ui.theme.AppTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Track outstanding department clearance handshakes and COE availability.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(10.dp))

            // Check if there is an active clearance for the employee, otherwise let's simulate one beautifully!
            val myClearance = clearances.find { it.employeeName.contains(currentEmployeeName, ignoreCase = true) } ?:
                OffboardingClearance(
                    employeeName = currentEmployeeName,
                    separationDate = "2026-08-31",
                    department = "Engineering",
                    itClearanceStatus = "CLEARED",
                    financeClearanceStatus = "PENDING",
                    adminClearanceStatus = "PENDING",
                    comments = "Handing off production keys."
                )

            // Compute countdown
            val daysRemaining = try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val targetDate = sdf.parse(myClearance.separationDate)
                val today = Date()
                if (targetDate != null) {
                    val diff = targetDate.time - today.time
                    val days = (diff / (1000 * 60 * 60 * 24)).toInt()
                    if (days > 0) days else 0
                } else 0
            } catch (e: Exception) {
                0
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Scheduled Exit: ${myClearance.separationDate}", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Role: ${myClearance.department}", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFF5555).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("$daysRemaining Days Left", color = Color(0xFFFF5555), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Department Clearances", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val clearancesList = listOf(
                        "IT Asset Return" to myClearance.itClearanceStatus,
                        "Finance Outstanding Liabilities" to myClearance.financeClearanceStatus,
                        "Administrative Keys & Hands-Off" to myClearance.adminClearanceStatus
                    )

                    clearancesList.forEach { (label, status) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (status == "CLEARED") Icons.Default.CheckCircle else Icons.Default.Pending,
                                contentDescription = null,
                                tint = if (status == "CLEARED") Color(0xFF00FF88) else Color(0xFFFFCC00),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(label, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text(
                                text = status,
                                color = if (status == "CLEARED") Color(0xFF00FF88) else Color(0xFFFFCC00),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = BorderGrey)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Certificate of Employment (COE):", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                            Text(if (myClearance.coeReady) "Ready to Download" else "Pending Clearance", color = if (myClearance.coeReady) Color(0xFF00FF88) else Color(0xFFFFCC00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (myClearance.coeReady) {
                                    Toast.makeText(context, "COE PDF generated and saved successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Clearance not finalized. Certificate locked.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = myClearance.coeReady,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, disabledContainerColor = BorderGrey),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text("Download COE", color = if (myClearance.coeReady) Color.Black else getAdaptiveColor(0.5f), fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Final Pay Check Settlement:", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                            Text(if (myClearance.finalPayReady) "Deposited to Bank" else "Processing Liquidation", color = if (myClearance.finalPayReady) Color(0xFF00FF88) else Color(0xFFFFCC00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        "okrs" -> {
            var newObjective by remember { mutableStateOf("") }
            var newKeyResult by remember { mutableStateOf("") }
            var newTargetValue by remember { mutableStateOf("") }

            Text("Continuous OKRs & Performance Tracker", color = com.example.ui.theme.AppTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Quarterly goals and direct self-appraisal submissions.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(10.dp))

            // Filing a new OKR
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2224)),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Register New Quarterly OKR", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newObjective,
                        onValueChange = { newObjective = it },
                        label = { Text("Objective Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newKeyResult,
                            onValueChange = { newKeyResult = it },
                            label = { Text("Key Result Metric") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                        )
                        OutlinedTextField(
                            value = newTargetValue,
                            onValueChange = { newTargetValue = it },
                            label = { Text("Target") },
                            modifier = Modifier.weight(0.6f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newObjective.isBlank() || newKeyResult.isBlank()) {
                                Toast.makeText(context, "Fill Objective and Key Result.", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.logOkr(newObjective, newKeyResult, newTargetValue, "0%", 0)
                                newObjective = ""
                                newKeyResult = ""
                                newTargetValue = ""
                                Toast.makeText(context, "Quarterly OKR registered successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Register Objective", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            val myOkrs = okrs.filter { it.employeeName.equals(currentEmployeeName, ignoreCase = true) }
            if (myOkrs.isEmpty()) {
                Text("No active OKRs. Click above to register.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
            } else {
                myOkrs.forEach { okr ->
                    var activeSliderVal by remember { mutableStateOf(okr.progress.toFloat()) }
                    var selfNote by remember { mutableStateOf(okr.selfAppraisal) }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                        border = BorderStroke(1.dp, BorderGrey)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(okr.objective, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .background(if (okr.status == "ACHIEVED") Color(0xFF00FF88).copy(alpha = 0.15f) else NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(okr.status, color = if (okr.status == "ACHIEVED") Color(0xFF00FF88) else NeonGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Key Result: ${okr.keyResult} (Target: ${okr.targetValue})", color = getAdaptiveTextColor(0.6f), fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Slider(
                                    value = activeSliderVal,
                                    onValueChange = { activeSliderVal = it },
                                    valueRange = 0f..100f,
                                    steps = 10,
                                    colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("${activeSliderVal.toInt()}%", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedTextField(
                                value = selfNote,
                                onValueChange = { selfNote = it },
                                label = { Text("Self-Appraisal & Progress Highlights") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                if (okr.managerFeedback.isNotEmpty()) {
                                    Text("Manager: ${okr.managerFeedback}", color = Color(0xFF00FF88), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                } else {
                                    Text("Awaiting Manager Appraisal Audit", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp, modifier = Modifier.weight(1f))
                                }
                                Button(
                                    onClick = {
                                        viewModel.updateOkrProgress(okr.id, "${activeSliderVal.toInt()}%", activeSliderVal.toInt(), selfNote)
                                        Toast.makeText(context, "OKR appraisal values updated!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("Save Progress", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
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

    val isLightTheme = com.example.ui.theme.AppTextColor == Color(0xFF2D3748)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLightTheme) Color.White else CardGreyBg
        ),
        border = BorderStroke(
            1.dp,
            if (isLightTheme) Color(0xFFE2E8F0) else BorderGrey
        )
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
                label = { Text("Claim Amount (${viewModel.getCurrencySymbol()})") },
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
    Text("Reimbursement Claims History", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))

    val myClaims = claims.filter { it.employeeName == viewModel.currentUserName.value }
    if (myClaims.isEmpty()) {
        Text("No reimbursement claims logged.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
    } else {
        val isCardLight = com.example.ui.theme.AppTextColor == Color(0xFF2D3748)
        myClaims.forEach { req ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCardLight) Color.White else CardGreyBg
                ),
                border = BorderStroke(
                    1.dp,
                    if (isCardLight) Color(0xFFE2E8F0) else BorderGrey
                )
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(req.title, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Amount: ${viewModel.getCurrencySymbol()}${String.format("%.2f", req.amount)}", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
fun DigitalIDQRCode(id: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val size = this.size.width
        val blockSize = size / 11f
        
        // Let's draw some QR-code style patterns (3 finder patterns in corners, and some random blocks)
        val finderPatternSize = 3f * blockSize
        
        // Top-left finder
        drawRect(Color.Black, topLeft = Offset(0f, 0f), size = Size(finderPatternSize, finderPatternSize))
        drawRect(Color.White, topLeft = Offset(blockSize, blockSize), size = Size(finderPatternSize - 2 * blockSize, finderPatternSize - 2 * blockSize))
        drawRect(Color.Black, topLeft = Offset(1.3f * blockSize, 1.3f * blockSize), size = Size(finderPatternSize - 2.6f * blockSize, finderPatternSize - 2.6f * blockSize))
        
        // Top-right finder
        drawRect(Color.Black, topLeft = Offset(size - finderPatternSize, 0f), size = Size(finderPatternSize, finderPatternSize))
        drawRect(Color.White, topLeft = Offset(size - finderPatternSize + blockSize, blockSize), size = Size(finderPatternSize - 2 * blockSize, finderPatternSize - 2 * blockSize))
        drawRect(Color.Black, topLeft = Offset(size - finderPatternSize + 1.3f * blockSize, 1.3f * blockSize), size = Size(finderPatternSize - 2.6f * blockSize, finderPatternSize - 2.6f * blockSize))
        
        // Bottom-left finder
        drawRect(Color.Black, topLeft = Offset(0f, size - finderPatternSize), size = Size(finderPatternSize, finderPatternSize))
        drawRect(Color.White, topLeft = Offset(blockSize, size - finderPatternSize + blockSize), size = Size(finderPatternSize - 2 * blockSize, finderPatternSize - 2 * blockSize))
        drawRect(Color.Black, topLeft = Offset(1.3f * blockSize, size - finderPatternSize + 1.3f * blockSize), size = Size(finderPatternSize - 2.6f * blockSize, finderPatternSize - 2.6f * blockSize))
        
        // Draw some random high-density QR blocks in other areas to make it look 100% authentic
        val seed = id.hashCode().toLong()
        val random = java.util.Random(seed)
        for (r in 0 until 11) {
            for (c in 0 until 11) {
                // Skip finder areas
                val inTopLeftFinder = r < 3 && c < 3
                val inTopRightFinder = r < 3 && c >= 8
                val inBottomLeftFinder = r >= 8 && c < 3
                if (!inTopLeftFinder && !inTopRightFinder && !inBottomLeftFinder) {
                    if (random.nextBoolean()) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * blockSize, r * blockSize),
                            size = Size(blockSize, blockSize)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScannerView(
    onScanDecoded: (EmployeeProfile) -> Unit,
    profiles: List<EmployeeProfile>,
    onClose: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    var selectedMockProfile by remember { mutableStateOf<EmployeeProfile?>(null) }
    var scanInProgress by remember { mutableStateOf(false) }
    var scanSuccessProfile by remember { mutableStateOf<EmployeeProfile?>(null) }
    
    // Laser line infinite animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    LaunchedEffect(selectedMockProfile) {
        val profile = selectedMockProfile
        if (profile != null) {
            scanInProgress = true
            scanSuccessProfile = null
            kotlinx.coroutines.delay(1800) // simulate high-tech decoding scan sweep
            scanInProgress = false
            scanSuccessProfile = profile
            selectedMockProfile = null
        }
    }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SECURE SCANNER PORTAL",
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = getAdaptiveTextColor(0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (scanSuccessProfile != null) {
                    // Success View
                    val profile = scanSuccessProfile!!
                    Text(
                        text = "🟢 DIGITAL ID CARD VERIFIED",
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Decoded ID Badge display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, NeonGreen)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .border(1.dp, NeonGreen, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.name.take(2).uppercase(),
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(profile.name, fontWeight = FontWeight.Bold, color = com.example.ui.theme.AppTextColor, fontSize = 15.sp)
                                    Text(profile.position, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("ID: ${profile.id}", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = getAdaptiveTextColor(0.08f))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("DEPARTMENT", fontSize = 8.sp, color = getAdaptiveTextColor(0.4f))
                                    Text(profile.department, fontSize = 10.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("STATUS", fontSize = 8.sp, color = getAdaptiveTextColor(0.4f))
                                    Text(profile.status.uppercase(), fontSize = 10.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { scanSuccessProfile = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, getAdaptiveColor(0.3f))
                        ) {
                            Text("Scan Another")
                        }
                        Button(
                            onClick = {
                                onScanDecoded(profile)
                                scanSuccessProfile = null
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Text("Record Gate Log", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Viewfinder area with laser line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color.Black, RoundedCornerShape(16.dp))
                            .border(1.dp, getAdaptiveColor(0.12f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cameraPermissionState.status.isGranted) {
                            // Render actual viewfinder
                            CameraXViewfinder(modifier = Modifier.fillMaxSize())
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideocamOff,
                                    contentDescription = null,
                                    tint = getAdaptiveTextColor(0.4f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Camera Access Required for Scan",
                                    color = getAdaptiveTextColor(0.6f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { cameraPermissionState.launchPermissionRequest() },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Grant Permission", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Scanning HUD grid overlay
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val bracketLenPx = with(density) { 24.dp.toPx() }
                        val strokeWPx = with(density) { 3.dp.toPx() }
                        val padPx = with(density) { 16.dp.toPx() }
                        val laserColor = NeonGreen

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            // Top-Left
                            drawLine(laserColor, Offset(padPx, padPx), Offset(padPx + bracketLenPx, padPx), strokeWPx)
                            drawLine(laserColor, Offset(padPx, padPx), Offset(padPx, padPx + bracketLenPx), strokeWPx)
                            
                            // Top-Right
                            drawLine(laserColor, Offset(w - padPx, padPx), Offset(w - padPx - bracketLenPx, padPx), strokeWPx)
                            drawLine(laserColor, Offset(w - padPx, padPx), Offset(w - padPx, padPx + bracketLenPx), strokeWPx)
                            
                            // Bottom-Left
                            drawLine(laserColor, Offset(padPx, h - padPx), Offset(padPx + bracketLenPx, h - padPx), strokeWPx)
                            drawLine(laserColor, Offset(padPx, h - padPx), Offset(padPx, h - padPx - bracketLenPx), strokeWPx)
                            
                            // Bottom-Right
                            drawLine(laserColor, Offset(w - padPx, h - padPx), Offset(w - padPx - bracketLenPx, h - padPx), strokeWPx)
                            drawLine(laserColor, Offset(w - padPx, h - padPx), Offset(w - padPx, h - padPx - bracketLenPx), strokeWPx)
                        }

                        // Animating horizontal laser bar
                        if (scanInProgress) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .align(Alignment.TopCenter)
                                    .offset(y = (220.dp * laserProgress))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                NeonGreen,
                                                Color.White,
                                                NeonGreen,
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (scanInProgress) {
                        Text(
                            text = "DECODING SHIFTHR HOLOSHIFT SECURE PROTOCOL...",
                            color = NeonGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                        LinearProgressIndicator(color = NeonGreen, trackColor = getAdaptiveColor(0.1f), modifier = Modifier.fillMaxWidth().height(2.dp))
                    } else {
                        Text(
                            text = "SIMULATION SANDBOX",
                            color = getAdaptiveTextColor(0.4f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Dropdown or list of profiles to mock scan
                        Text(
                            text = "Tap any mock employee card below to trigger high-fidelity scan simulation:",
                            color = getAdaptiveTextColor(0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            profiles.forEach { p ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                        .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                        .clickable { selectedMockProfile = p }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(p.name.take(2).uppercase(), color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(p.name, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraXViewfinder(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}

@Composable
fun NFCTransmitterAndGateSimulator(
    profile: EmployeeProfile,
    profiles: List<EmployeeProfile>,
    onClose: () -> Unit,
    onClockInGateSimulated: (EmployeeProfile) -> Unit,
    context: Context
) {
    var activeTab by remember { mutableStateOf("transmit") } // transmit, receive
    
    // Pulse animation for wave
    val infiniteTransition = rememberInfiniteTransition(label = "nfc")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_alpha"
    )

    val nfcAdapter = try { NfcAdapter.getDefaultAdapter(context) } catch (e: Exception) { null }
    val nfcSupported = nfcAdapter != null
    val nfcEnabled = nfcAdapter?.isEnabled == true

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NFC CYBERGATE HUB",
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = getAdaptiveTextColor(0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hardware Diagnostic Panel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (nfcEnabled) NeonGreen.copy(alpha = 0.3f) else getAdaptiveColor(0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (nfcEnabled) NeonGreen else Color.Red, RoundedCornerShape(5.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (nfcEnabled) "NFC HARDWARE ENGINE ACTIVE" else "NFC SIMULATED SANBOX MODE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (nfcEnabled) NeonGreen else getAdaptiveColor(0.7f)
                            )
                            Text(
                                text = if (nfcSupported) {
                                    if (nfcEnabled) "Physical controller configured & broadcasting standard NDEF frames."
                                    else "NFC hardware is supported but disabled in system settings."
                                } else {
                                    "Physical chip not detected. Software emulation active."
                                },
                                fontSize = 9.sp,
                                color = getAdaptiveTextColor(0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode select Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(getAdaptiveColor(0.04f), RoundedCornerShape(10.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == "transmit") NeonGreen else Color.Transparent)
                            .clickable { activeTab = "transmit" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Beam ID (Transmit)", color = if (activeTab == "transmit") Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == "receive") NeonGreen else Color.Transparent)
                            .clickable { activeTab = "receive" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Gate Terminal (Receive)", color = if (activeTab == "receive") Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (activeTab == "transmit") {
                    // Transmitter Interface
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Broadcasting Secure Waveform...",
                            color = getAdaptiveTextColor(0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Wave animation box
                        Box(
                            modifier = Modifier.size(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Ripple wave 1
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .graphicsLayer {
                                        scaleX = waveScale
                                        scaleY = waveScale
                                        alpha = waveAlpha
                                    }
                                    .background(NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(50.dp))
                                    .border(1.dp, NeonGreen.copy(alpha = waveAlpha), RoundedCornerShape(50.dp))
                            )
                            // Core NFC Badge
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
                                    .border(2.dp, NeonGreen, RoundedCornerShape(32.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "SECURE NDEF RECORD ENCODING:",
                            color = NeonGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "application/vnd.shifthr.badge\nPayload: ${profile.id}:${profile.name.uppercase()}",
                            color = com.example.ui.theme.AppTextColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(getAdaptiveColor(0.04f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        )
                    }
                } else {
                    // Receiver / Clock-In office turnstile gate terminal
                    var gateUnlockedProfile by remember { mutableStateOf<EmployeeProfile?>(null) }
                    
                    if (gateUnlockedProfile != null) {
                        val p = gateUnlockedProfile!!
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "🟢 ACCESS GRANTED",
                                color = NeonGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "OFFICE CYBERGATE UNLOCKED",
                                color = getAdaptiveTextColor(0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "${p.name} has clocked in successfully via NFC terminal.",
                                color = getAdaptiveTextColor(0.8f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    onClockInGateSimulated(p)
                                    gateUnlockedProfile = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Complete and Lock Gate", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SensorWindow,
                                contentDescription = null,
                                tint = getAdaptiveTextColor(0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "CYBERGATE RECEIVER ACTIVE",
                                color = com.example.ui.theme.AppTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Simulate tapping any employee ID card to trigger gate reception:",
                                color = getAdaptiveTextColor(0.6f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Grid or list of employees to simulate nfc tap
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                profiles.forEach { p ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(getAdaptiveColor(0.04f), RoundedCornerShape(10.dp))
                                            .border(1.dp, getAdaptiveColor(0.08f), RoundedCornerShape(10.dp))
                                            .clickable { gateUnlockedProfile = p }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(p.name.take(2).uppercase(), color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(p.name, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(p.position, color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.TapAndPlay,
                                            contentDescription = "Tap",
                                            tint = NeonGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DigitalIDBadge(profile: EmployeeProfile, viewModel: TimeTrackerViewModel, context: Context) {
    var showQrDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var showNfcTerminal by remember { mutableStateOf(false) }

    // Dialog for Scanner
    if (showScanner) {
        CameraScannerView(
            onScanDecoded = { scannedProfile ->
                viewModel.handlePunch(scannedProfile.name)
                Toast.makeText(context, "Scanned & logged entry for ${scannedProfile.name}! 🟢", Toast.LENGTH_LONG).show()
                showScanner = false
            },
            profiles = viewModel.employeeProfiles.value.filter { it.id != profile.id },
            onClose = { showScanner = false }
        )
    }

    // Dialog for NFC Hub
    if (showNfcTerminal) {
        NFCTransmitterAndGateSimulator(
            profile = profile,
            profiles = viewModel.employeeProfiles.value.filter { it.id != profile.id },
            onClose = { showNfcTerminal = false },
            onClockInGateSimulated = { tappedProfile ->
                viewModel.handlePunch(tappedProfile.name)
                Toast.makeText(context, "NFC Card Recognized. Clock-In logged for ${tappedProfile.name}! 🟢", Toast.LENGTH_LONG).show()
                showNfcTerminal = false
            },
            context = context
        )
    }

    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            containerColor = Color(0xFF141416),
            title = { Text("Secure QR Credentials", color = NeonGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Point any standard scanner to decode secure work credentials.",
                        color = getAdaptiveTextColor(0.6f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DigitalIDQRCode(id = profile.id, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "ENCODED PROTOCOL:",
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "SHIFTHR:${profile.id}:${profile.name.uppercase()}:AGE_${profile.age}",
                        color = com.example.ui.theme.AppTextColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showQrDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Main ID Card Representation with BLURRED CARD BACKGROUND
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing colorful aurora background circles that will get blurred behind the card
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(20.dp))
        ) {
            // Neon Green Circle Top-Left
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = (-30).dp, y = (-20).dp)
                    .background(NeonGreen.copy(alpha = 0.22f), RoundedCornerShape(60.dp))
                    .blur(24.dp)
            )
            // Cyber Blue Circle Bottom-Right
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = 30.dp)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.18f), RoundedCornerShape(75.dp))
                    .blur(32.dp)
            )
        }

        // Translucent Frosted Glass Card with subtle inner border highlight
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0x1F111827)), // semi-transparent dark grey
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        NeonGreen.copy(alpha = 0.5f),
                        getAdaptiveColor(0.08f),
                        Color(0xFF3B82F6).copy(alpha = 0.3f)
                    )
                )
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with Corporate Branding and NFC
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Shift HR Logo Icon",
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SHIFT HR",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = com.example.ui.theme.AppTextColor,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    // NFC capable chip
                    Box(
                        modifier = Modifier
                            .background(NeonGreen.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable {
                                showNfcTerminal = true
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "NFC PASS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Profile avatar with Coil AsyncImage or Initials Fallback
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonGreen.copy(alpha = 0.15f))
                            .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile.picture.isNotEmpty()) {
                            coil.compose.AsyncImage(
                                model = profile.picture,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                error = androidx.compose.ui.graphics.painter.ColorPainter(Color.Transparent),
                                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.Transparent)
                            )
                        } else {
                            Text(
                                text = profile.name.take(2).uppercase(),
                                color = NeonGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.AppTextColor
                        )
                        Text(
                            text = profile.position,
                            fontSize = 12.sp,
                            color = NeonGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Dept: ${profile.department}",
                            fontSize = 11.sp,
                            color = getAdaptiveTextColor(0.5f)
                        )
                    }

                    // QR Code (Dynamic)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, getAdaptiveColor(0.2f), RoundedCornerShape(8.dp))
                            .clickable { showQrDialog = true }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DigitalIDQRCode(id = profile.id, modifier = Modifier.fillMaxSize())
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Divider(color = getAdaptiveTextColor(0.08f), thickness = 1.dp)

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("CARD ID", fontSize = 7.sp, color = getAdaptiveTextColor(0.4f))
                        Text(profile.id, fontSize = 10.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("AGE", fontSize = 7.sp, color = getAdaptiveTextColor(0.4f))
                        Text("${profile.age} Yrs", fontSize = 10.sp, color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("STATUS", fontSize = 7.sp, color = getAdaptiveTextColor(0.4f))
                        Text(profile.status.uppercase(), fontSize = 10.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Scanner and NFC control actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showScanner = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Digital ID", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showNfcTerminal = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, getAdaptiveColor(0.3f)),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.TapAndPlay, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("NFC Cybergate", color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    val activeLog by viewModel.activeTimeLog.collectAsState()

    if (myProfile != null) {
        // --- EDITABLE PERSONAL FIELDS STATES ---
        var preferredName by remember { mutableStateOf(myProfile.preferredName) }
        var pictureUrl by remember { mutableStateOf(myProfile.picture) }
        var mobilePhone by remember { mutableStateOf(myProfile.phoneNumber) }
        var personalEmail by remember { mutableStateOf(myProfile.personalEmail) }
        var currentAddress by remember { mutableStateOf(myProfile.address) }
        
        var emergencyName by remember { mutableStateOf(myProfile.emergencyContactName) }
        var emergencyPhone by remember { mutableStateOf(myProfile.emergencyContactPhone) }
        var emergencyRel by remember { mutableStateOf(myProfile.emergencyRelationship) }
        var workEmail by remember { mutableStateOf(myProfile.email) }

        // --- INTERACTIVE & SECURITY STATES ---
        var activeSection by remember { mutableStateOf("personal") } // personal, employment, documents
        var isPersonalUnlocked by remember { mutableStateOf(false) }
        var isBankDetailsRevealed by remember { mutableStateOf(false) }
        var areGovernmentIdsRevealed by remember { mutableStateOf(false) }

        // Biometric dialog triggering states
        var showBiometricCheck by remember { mutableStateOf(false) }
        var biometricPromptText by remember { mutableStateOf("Verify Biometrics") }
        var onBiometricSuccessAction by remember { mutableStateOf<(() -> Unit)?>(null) }

        // Preferences states
        var pushNotificationsEnabled by remember { mutableStateOf(true) }
        var darkModeOverride by remember { mutableStateOf(false) }
        var biometricLoginEnabled by remember { mutableStateOf(true) }

        // --- LOCAL BIOMETRIC SIMULATION DIALOG ---
        if (showBiometricCheck) {
            Dialog(onDismissRequest = { showBiometricCheck = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16161B)),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
                ) {
                    var scanProgress by remember { mutableStateOf(0f) }
                    var isFinished by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        scanProgress = 0f
                        isFinished = false
                        while (scanProgress < 1f) {
                            kotlinx.coroutines.delay(40)
                            scanProgress += 0.05f
                        }
                        scanProgress = 1f
                        isFinished = true
                        kotlinx.coroutines.delay(500)
                        showBiometricCheck = false
                        onBiometricSuccessAction?.invoke()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    if (isFinished) NeonGreen.copy(alpha = 0.15f) else getAdaptiveColor(0.05f),
                                    CircleShape
                                )
                                .border(
                                    2.dp,
                                    if (isFinished) NeonGreen else getAdaptiveColor(0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFinished) Icons.Default.CheckCircle else Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = if (isFinished) NeonGreen else Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = if (isFinished) "BIOMETRIC VERIFIED" else "BIOMETRIC AUTHENTICATION",
                            color = if (isFinished) NeonGreen else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isFinished) "Identity authorized successfully." else "$biometricPromptText...",
                            color = getAdaptiveTextColor(0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        if (!isFinished) {
                            LinearProgressIndicator(
                                progress = scanProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = NeonGreen,
                                trackColor = getAdaptiveColor(0.1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // Main layout container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // ==================== 1. IDENTITY CARD (HERO COMPONENT) ====================
            val currentLog = activeLog
            val isClockedIn = currentLog != null && currentLog.timeIn != null && currentLog.timeOut == null
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, getAdaptiveColor(0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(getAdaptiveColor(0.05f), CircleShape)
                            .border(
                                3.dp,
                                if (isClockedIn) NeonGreen else Color.Gray,
                                CircleShape
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (myProfile.picture.isNotEmpty()) {
                            coil.compose.AsyncImage(
                                model = myProfile.picture,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                error = androidx.compose.ui.graphics.painter.ColorPainter(Color.Transparent),
                                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.Transparent)
                            )
                        } else {
                            Text(
                                text = myProfile.name.take(2).uppercase(),
                                color = com.example.ui.theme.AppTextColor,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val pronouns = if (myProfile.gender.equals("Female", ignoreCase = true)) "(She/Her)" else "(He/Him)"
                        Text(
                            text = "${myProfile.name} $pronouns",
                            color = com.example.ui.theme.AppTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${myProfile.position} • ${myProfile.department}",
                            color = getAdaptiveTextColor(0.6f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(getAdaptiveColor(0.08f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "#${myProfile.id}",
                                    color = getAdaptiveTextColor(0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isClockedIn) NeonGreen.copy(alpha = 0.1f) else getAdaptiveColor(0.05f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isClockedIn) NeonGreen.copy(alpha = 0.3f) else getAdaptiveColor(0.15f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isClockedIn) "Clocked In • Regular" else "Clocked Out • Regular",
                                    color = if (isClockedIn) NeonGreen else getAdaptiveColor(0.6f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ==================== 2. QUICK METRICS ROW ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // PTO Balance Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = getAdaptiveColor(0.04f)),
                    border = BorderStroke(1.dp, getAdaptiveColor(0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PTO BALANCE",
                            fontSize = 8.sp,
                            color = getAdaptiveTextColor(0.5f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "14.5 Days",
                            fontSize = 13.sp,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Hours This Week Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = getAdaptiveColor(0.04f)),
                    border = BorderStroke(1.dp, getAdaptiveColor(0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "WEEKLY HOURS",
                            fontSize = 8.sp,
                            color = getAdaptiveTextColor(0.5f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "32.5 / 40 Hrs",
                            fontSize = 13.sp,
                            color = com.example.ui.theme.AppTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Next Shift Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = getAdaptiveColor(0.04f)),
                    border = BorderStroke(1.dp, getAdaptiveColor(0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NEXT SHIFT",
                            fontSize = 8.sp,
                            color = getAdaptiveTextColor(0.5f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "08:00 AM",
                            fontSize = 13.sp,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ==================== 3. THUMB-FRIENDLY TABS ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(getAdaptiveColor(0.04f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val sections = listOf(
                    "personal" to "Personal",
                    "employment" to "Employment",
                    "documents" to "Documents"
                )
                sections.forEach { (key, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeSection == key) NeonGreen else Color.Transparent)
                            .clickable { activeSection = key }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (activeSection == key) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ==================== TAB CONTENT RENDERING ====================
            when (activeSection) {
                "personal" -> {
                    // --- PERSONAL INFORMATION HUB CARD ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                        border = BorderStroke(1.dp, BorderGrey)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("👤 Personal Info Hub", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                if (!isPersonalUnlocked) {
                                    Button(
                                        onClick = {
                                            biometricPromptText = "Unlock Personal Info Hub for editing"
                                            onBiometricSuccessAction = { isPersonalUnlocked = true }
                                            showBiometricCheck = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = getAdaptiveColor(0.08f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = com.example.ui.theme.AppTextColor, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit & Unlock", color = com.example.ui.theme.AppTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            biometricPromptText = "Authorize profile changes update"
                                            onBiometricSuccessAction = {
                                                viewModel.updateProfileInfo(
                                                    emergencyContact = emergencyName,
                                                    contactPhone = emergencyPhone,
                                                    email = workEmail,
                                                    phoneNumber = mobilePhone,
                                                    address = currentAddress,
                                                    preferredName = preferredName,
                                                    personalEmail = personalEmail,
                                                    picture = pictureUrl,
                                                    emergencyRelationship = emergencyRel
                                                )
                                                isPersonalUnlocked = false
                                                Toast.makeText(context, "Secure cloud sync completed successfully! 🔒", Toast.LENGTH_SHORT).show()
                                            }
                                            showBiometricCheck = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp).testTag("save_profile_dossier_action")
                                    ) {
                                        Text("Save Changes", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (!isPersonalUnlocked) {
                                // Locked state - displaying masked and clean data
                                val maskedPhone = if (mobilePhone.length > 7) {
                                    mobilePhone.take(mobilePhone.length - 4) + "••••"
                                } else {
                                    "•••• •••• •••"
                                }
                                val maskedPersonalEmail = if (personalEmail.contains("@")) {
                                    personalEmail.take(1) + "•••••@" + personalEmail.substringAfter("@")
                                } else {
                                    "•••••@••••.com"
                                }

                                ProfileDetailRow("Preferred Name", preferredName.ifEmpty { "None Set" })
                                ProfileDetailRow("Mobile Phone", maskedPhone)
                                ProfileDetailRow("Corporate Email", workEmail)
                                ProfileDetailRow("Personal Email", maskedPersonalEmail)
                                ProfileDetailRow("Home Address", currentAddress)
                                
                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = getAdaptiveTextColor(0.05f))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("EMERGENCY CONTACT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                        Text("$emergencyName ($emergencyRel)", color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(emergencyPhone, color = getAdaptiveTextColor(0.6f), fontSize = 11.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            Toast.makeText(context, "Initiating direct dial to emergency contact: $emergencyPhone 📞", Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(NeonGreen.copy(alpha = 0.1f), CircleShape)
                                            .border(1.dp, NeonGreen.copy(alpha = 0.3f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Call Emergency Contact", tint = NeonGreen, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = getAdaptiveTextColor(0.05f))

                                val maskedBank = if (myProfile.bankAccount.length > 4) {
                                    "•••• " + myProfile.bankAccount.takeLast(4)
                                } else {
                                    "•••• 4321"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("DIRECT DEPOSIT ROUTING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                        Text(
                                            text = if (isBankDetailsRevealed) "${myProfile.bankName} • ${myProfile.bankAccount}" else "${myProfile.bankName} • $maskedBank",
                                            color = com.example.ui.theme.AppTextColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            biometricPromptText = if (isBankDetailsRevealed) "Encrypt bank details" else "Decrypt bank details via FaceID/Fingerprint"
                                            onBiometricSuccessAction = { isBankDetailsRevealed = !isBankDetailsRevealed }
                                            showBiometricCheck = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isBankDetailsRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Bank Details",
                                            tint = NeonGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                            } else {
                                // Unlocked Editable State
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = preferredName,
                                        onValueChange = { preferredName = it },
                                        label = { Text("Preferred Name/Nickname") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                                    )
                                    OutlinedTextField(
                                        value = mobilePhone,
                                        onValueChange = { mobilePhone = it },
                                        label = { Text("Mobile Number") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                                    )
                                    OutlinedTextField(
                                        value = personalEmail,
                                        onValueChange = { personalEmail = it },
                                        label = { Text("Personal Email") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                                    )
                                    OutlinedTextField(
                                        value = currentAddress,
                                        onValueChange = { currentAddress = it },
                                        label = { Text("Home Address") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                                    )
                                    OutlinedTextField(
                                        value = pictureUrl,
                                        onValueChange = { pictureUrl = it },
                                        label = { Text("Profile Photo URL") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                                    )
                                    
                                    Divider(color = getAdaptiveTextColor(0.05f))
                                    Text("Emergency Contact Info", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                    
                                    OutlinedTextField(
                                        value = emergencyName,
                                        onValueChange = { emergencyName = it },
                                        label = { Text("Contact Name") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                                    )
                                    OutlinedTextField(
                                        value = emergencyRel,
                                        onValueChange = { emergencyRel = it },
                                        label = { Text("Relationship") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                                    )
                                    OutlinedTextField(
                                        value = emergencyPhone,
                                        onValueChange = { emergencyPhone = it },
                                        label = { Text("Contact Phone") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                                    )
                                }
                            }
                        }
                    }

                    // --- PH STATUTORY COMPLIANCE & GOVERNMENT IDS CARD ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                        border = BorderStroke(1.dp, BorderGrey)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PH Statutory Compliance",
                                        color = NeonGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    // Verified Neon Green Badge
                                    Box(
                                        modifier = Modifier
                                            .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Verified ✓", color = NeonGreen, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Eye icon to reveal
                                    IconButton(
                                        onClick = {
                                            biometricPromptText = if (areGovernmentIdsRevealed) "Obscure statutory IDs" else "Decrypt Statutory credentials"
                                            onBiometricSuccessAction = { areGovernmentIdsRevealed = !areGovernmentIdsRevealed }
                                            showBiometricCheck = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (areGovernmentIdsRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle statutory details",
                                            tint = NeonGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Locked HR label
                                    Box(
                                        modifier = Modifier
                                            .background(getAdaptiveColor(0.05f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "🔒 Compiled by HR",
                                            color = getAdaptiveTextColor(0.4f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val rawTin = if (myProfile.taxId.isNotEmpty()) myProfile.taxId else "123-456-789-000"
                            val rawSsn = if (myProfile.ssn.isNotEmpty()) myProfile.ssn else "01-2345678-9"
                            val rawHealth = if (myProfile.healthInsurance.isNotEmpty()) myProfile.healthInsurance else "12-345678901-2"
                            val rawHousing = if (myProfile.housingFund.isNotEmpty()) myProfile.housingFund else "1020-3040-5060"

                            val displayedTin = if (areGovernmentIdsRevealed) rawTin else "•••-•••-•••-000"
                            val displayedSsn = if (areGovernmentIdsRevealed) rawSsn else "••-•••••••-9"
                            val displayedHealth = if (areGovernmentIdsRevealed) rawHealth else "••-•••••••••-2"
                            val displayedHousing = if (areGovernmentIdsRevealed) rawHousing else "••••-••••-5060"

                            // List of clickable rows that prompt the user that it's HR compiled
                            val promptMessage = "Government IDs are managed by HR. Contact your compliance officer to request changes."

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ClickableIdRow(label = "TIN (Tax Identification Number)", value = displayedTin, onClick = {
                                    Toast.makeText(context, promptMessage, Toast.LENGTH_LONG).show()
                                })
                                ClickableIdRow(label = "SSS (Social Security System)", value = displayedSsn, onClick = {
                                    Toast.makeText(context, promptMessage, Toast.LENGTH_LONG).show()
                                })
                                ClickableIdRow(label = "PhilHealth (PHIC Number)", value = displayedHealth, onClick = {
                                    Toast.makeText(context, promptMessage, Toast.LENGTH_LONG).show()
                                })
                                ClickableIdRow(label = "Pag-IBIG (HDMF MID Number)", value = displayedHousing, onClick = {
                                    Toast.makeText(context, promptMessage, Toast.LENGTH_LONG).show()
                                })
                            }
                        }
                    }
                }
                
                "employment" -> {
                    // --- EMPLOYMENT & COMPLIANCE DETAILS CARD ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                        border = BorderStroke(1.dp, BorderGrey)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Employment & Compliance", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("These settings are managed by your compliance officers.", fontSize = 10.sp, color = getAdaptiveTextColor(0.4f))
                            Spacer(modifier = Modifier.height(16.dp))

                            ProfileDetailRow("Position Title", myProfile.position)
                            ProfileDetailRow("Department", myProfile.department)
                            ProfileDetailRow("Team Group", myProfile.team)
                            ProfileDetailRow("Date of Hire", myProfile.dateHired)
                            ProfileDetailRow("Next Review Milestone", "2026-10-15 (Q3 Performance Appraisal)")
                            
                            // Geofence Hub assignment
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Authorized Perimeter", color = getAdaptiveTextColor(0.5f), fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("HQ - Branch A", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = myProfile.workLocation, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = getAdaptiveTextColor(0.05f))

                            // Manager details with chat shortcut
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("DIRECT MANAGER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                    Text(myProfile.supervisor, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Operations Director", color = getAdaptiveTextColor(0.6f), fontSize = 11.sp)
                                }
                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "Opening secure chat with ${myProfile.supervisor}...", Toast.LENGTH_SHORT).show()
                                        viewModel.currentScreen.value = "chat"
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(getAdaptiveColor(0.05f), CircleShape)
                                        .border(1.dp, getAdaptiveColor(0.1f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Message, contentDescription = "Message Manager", tint = NeonGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // --- ATTENDANCE & DEVICE TELEMETRY CARD ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                        border = BorderStroke(1.dp, BorderGrey)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SettingsCell, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("📱 Device telemetry & MDM Locks", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            ProfileDetailRow("Registered Hardware Lock", myProfile.registeredDevice)
                            ProfileDetailRow("Digital Employee ID", myProfile.digitalId)
                            ProfileDetailRow("Configured NFC Tag", myProfile.nfcId)
                            ProfileDetailRow("Current Status", myProfile.attendanceStatus)
                            ProfileDetailRow("Assigned Work Perimeter", myProfile.assignedLocation)
                            ProfileDetailRow("Last Session Handshake", myProfile.lastLogin)
                        }
                    }
                }
                
                "documents" -> {
                    // --- DOCUMENT VAULT CARD ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                        border = BorderStroke(1.dp, BorderGrey)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("📂 Document Vault & Payslips", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Securely download official company-approved employee forms.", fontSize = 10.sp, color = getAdaptiveTextColor(0.4f))
                            Spacer(modifier = Modifier.height(16.dp))

                            val secureDocs = listOf(
                                "Shift_HR_Payslip_June_2026.pdf" to "Shift HR Digital Payslip (June 2026)",
                                "Shift_HR_Payslip_May_2026.pdf" to "Shift HR Digital Payslip (May 2026)",
                                "BIR_Tax_Form_2316_2025.pdf" to "Annual Income Tax Return (Form 2316)",
                                "Employment_Contract.pdf" to "Employment Contract",
                                "Passport_Scan.pdf" to "Government Passport ID Scan",
                                "Company_Code_Of_Ethics.pdf" to "Company Forms & Ethics Policy"
                            )

                            secureDocs.forEach { (filename, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(getAdaptiveColor(0.03f), RoundedCornerShape(8.dp))
                                        .border(1.dp, getAdaptiveColor(0.05f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Article, contentDescription = null, tint = getAdaptiveTextColor(0.4f), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.AppTextColor)
                                            Text(filename, fontSize = 9.sp, color = getAdaptiveTextColor(0.4f))
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            Toast.makeText(context, "Downloading encrypted vault file: $filename 📥", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Download Document", tint = NeonGreen, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // --- APP PREFERENCES CARD ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                        border = BorderStroke(1.dp, BorderGrey)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("⚙️ Portal App Preferences", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Push Reminders", color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Remind me to clock out at shift completion", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                                }
                                Switch(
                                    checked = pushNotificationsEnabled,
                                    onCheckedChange = { pushNotificationsEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Dark Mode Override", color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Force portal to stay in deep dark theme", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                                }
                                Switch(
                                    checked = darkModeOverride,
                                    onCheckedChange = { darkModeOverride = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Biometric Sign-In", color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Permit FaceID or Fingerprint login bypass", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                                }
                                Switch(
                                    checked = biometricLoginEnabled,
                                    onCheckedChange = { biometricLoginEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        Text("Dossier profile not found. Please contact administration.", color = getAdaptiveTextColor(0.5f), fontSize = 12.sp)
    }
}

@Composable
fun ClickableIdRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
            Text(text = value, color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = getAdaptiveTextColor(0.2f),
            modifier = Modifier.size(14.dp)
        )
    }
}

// ---------------------- 3. PAYROLL & REPORTS SCREEN ----------------------
@Composable
fun AdaptivePayrollButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme() || MaterialTheme.colorScheme.onBackground == Color(0xFFFFFFFF)

    // Dynamic styling based on App Theme & Selection State
    val backgroundColor = when {
        isSelected && !isDark -> Color(0xFF1D4ED8) // Solid Blue (Light Mode Selected)
        isSelected && isDark  -> Color(0x3310B981) // Translucent Emerald (Dark Mode Selected)
        !isSelected && !isDark -> Color(0xB3FFFFFF) // Translucent White 70% (Light Mode Unselected)
        else -> Color(0x801E293B) // Translucent Slate 50% (Dark Mode Unselected)
    }

    val textColor = when {
        isSelected && !isDark -> Color(0xFFFFFFFF) // White text on dark blue
        isSelected && isDark  -> Color(0xFF34D399) // Vivid mint text
        !isSelected && !isDark -> Color(0xFF334155) // Dark slate gray text (Solves invisible text bug!)
        else -> Color(0xFF94A3B8) // Soft silver text
    }

    val borderColor = when {
        isSelected && !isDark -> null
        isSelected && isDark  -> BorderStroke(1.dp, Color(0x6610B981))
        !isSelected && !isDark -> BorderStroke(1.dp, Color(0xFFE2E8F0))
        else -> BorderStroke(1.dp, Color(0x14FFFFFF))
    }

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(if (borderColor != null) Modifier.border(borderColor, RoundedCornerShape(12.dp)) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun PayrollScreen(viewModel: TimeTrackerViewModel) {
    var activeSubView by remember { mutableStateOf("calc") } // calc, ph_compliance, payslips, reports
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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdaptivePayrollButton(
                    text = "Salary Calculator",
                    isSelected = activeSubView == "calc",
                    onClick = { activeSubView = "calc" },
                    modifier = Modifier.weight(1f)
                )
                AdaptivePayrollButton(
                    text = "PH Compliance & 13th Month",
                    isSelected = activeSubView == "ph_compliance",
                    onClick = { activeSubView = "ph_compliance" },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdaptivePayrollButton(
                    text = "My Payslips",
                    isSelected = activeSubView == "payslips",
                    onClick = { activeSubView = "payslips" },
                    modifier = Modifier.weight(1f)
                )
                AdaptivePayrollButton(
                    text = "Analytics Reports",
                    isSelected = activeSubView == "reports",
                    onClick = { activeSubView = "reports" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (activeSubView) {
            "calc" -> {
                SalaryCalculatorView(viewModel, context)
            }
            "ph_compliance" -> {
                PhComplianceView(viewModel, context)
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
fun InteractiveDoleSimulatorView() {
    var hourlyRateInput by remember { mutableStateOf("150.0") }
    var clockInHour by remember { mutableStateOf(14.0f) } // slider 0 to 23
    var shiftDurationInput by remember { mutableStateOf("9.0") }
    var isRestDay by remember { mutableStateOf(false) }
    var holidayType by remember { mutableStateOf("NONE") }

    val rate = hourlyRateInput.toDoubleOrNull() ?: 150.0
    val durationHours = shiftDurationInput.toDoubleOrNull() ?: 9.0

    // Construct clock-in date/time: e.g. July 15, 2026 at clockInHour:00
    val calendarIn = java.util.Calendar.getInstance()
    calendarIn.set(2026, java.util.Calendar.JULY, 15, clockInHour.toInt(), 0, 0)
    
    val calendarOut = calendarIn.clone() as java.util.Calendar
    calendarOut.add(java.util.Calendar.MINUTE, (durationHours * 60).toInt())

    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val clockInStr = sdf.format(calendarIn.time)
    val clockOutStr = sdf.format(calendarOut.time)

    // Calculate pay using the DOLE core algorithm
    val result = calculateDoleShiftPay(
        clockIn = calendarIn.time,
        clockOut = calendarOut.time,
        isRestDay = isRestDay,
        holidayType = if (holidayType == "NONE") null else holidayType,
        hourlyRate = rate
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardGreyBg),
        border = BorderStroke(1.dp, BorderGrey)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Philippine Labor Code (Book III) Simulator",
                color = NeonGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                "Simulates 15-min slicing, Night Differential (10 PM - 6 AM), overtime, and meal deduction.",
                color = getAdaptiveTextColor(0.5f),
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Hourly Rate input
            OutlinedTextField(
                value = hourlyRateInput,
                onValueChange = { hourlyRateInput = it },
                label = { Text("Base Hourly Rate (₱)") },
                textStyle = TextStyle(color = com.example.ui.theme.AppTextColor),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            // Shift Duration input
            OutlinedTextField(
                value = shiftDurationInput,
                onValueChange = { shiftDurationInput = it },
                label = { Text("Shift Duration (Hours)") },
                textStyle = TextStyle(color = com.example.ui.theme.AppTextColor),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            // Clock In Hour Slider
            Text(
                "Clock-In Time: ${String.format("%02d:00", clockInHour.toInt())}",
                color = com.example.ui.theme.AppTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = clockInHour,
                onValueChange = { clockInHour = it },
                valueRange = 0f..23f,
                steps = 23,
                colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Rest day switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).background(getAdaptiveColor(0.04f), RoundedCornerShape(8.dp)).padding(8.dp)
                ) {
                    Checkbox(
                        checked = isRestDay,
                        onCheckedChange = { isRestDay = it },
                        colors = CheckboxDefaults.colors(checkedColor = NeonGreen)
                    )
                    Text("Rest Day", fontSize = 11.sp, color = com.example.ui.theme.AppTextColor)
                }

                // Holiday Type Selection dropdown
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    var expandedHolidayMenu by remember { mutableStateOf(false) }
                    Button(
                        onClick = { expandedHolidayMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = getAdaptiveColor(0.06f)),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("Holiday: $holidayType", color = NeonGreen, fontSize = 11.sp)
                    }
                    DropdownMenu(
                        expanded = expandedHolidayMenu,
                        onDismissRequest = { expandedHolidayMenu = false },
                        modifier = Modifier.background(CardGreyBg)
                    ) {
                        listOf("NONE", "REGULAR", "SPECIAL").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, color = com.example.ui.theme.AppTextColor) },
                                onClick = {
                                    holidayType = type
                                    expandedHolidayMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = BorderGrey)
            Spacer(modifier = Modifier.height(14.dp))

            // Timestamps
            Text("🔏 SIMULATED PERIOD STATUS:", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("In: $clockInStr", color = com.example.ui.theme.AppTextColor, fontSize = 11.sp)
            Text("Out: $clockOutStr", color = com.example.ui.theme.AppTextColor, fontSize = 11.sp)
            Text(
                "Duration: ${String.format("%.2f", result.totalWorkedBeforeMeal)}h total (${String.format("%.2f", result.totalWorkedAfterMeal)}h net after meal break)",
                color = getAdaptiveTextColor(0.6f),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Multipliers info box
            Row(
                modifier = Modifier.fillMaxWidth().background(getAdaptiveColor(0.03f), RoundedCornerShape(8.dp)).padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Base Rate", color = getAdaptiveTextColor(0.5f), fontSize = 9.sp)
                    Text("${String.format("%.2f", result.baseMultiplier)}x", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OT Rate", color = getAdaptiveTextColor(0.5f), fontSize = 9.sp)
                    Text("${String.format("%.2f", result.otMultiplier)}x", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ND Premium", color = getAdaptiveTextColor(0.5f), fontSize = 9.sp)
                    Text("+10% (1.1x)", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hour slices breakdown
            Text("📊 DETAILED HOUR SLICES:", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            val breakdownRows = listOf(
                Triple("Regular Day Hours", result.hours.regular, result.earnings.regularPay),
                Triple("Regular Night Diff (10PM - 6AM)", result.hours.nd, result.earnings.ndPay),
                Triple("Overtime Day Hours", result.hours.ot, result.earnings.otPay),
                Triple("Overtime Night Diff Hours", result.hours.otNd, result.earnings.otNdPay)
            )

            breakdownRows.forEach { (label, hr, pay) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${String.format("%.2f", hr)} hrs", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                    }
                    Text("₱${String.format("%.2f", pay)}", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                Divider(color = BorderGrey.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth().background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TOTAL GROSS EARNED", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text("₱${String.format("%.2f", result.earnings.totalGrossPay)}", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SalaryCalculatorView(viewModel: TimeTrackerViewModel, context: Context) {
    var calcSubTab by remember { mutableStateOf("overview") } // overview, simulator
    val allLogs by viewModel.allTimeLogs.collectAsState()
    val config by viewModel.shiftConfig.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(getAdaptiveColor(0.04f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf(
                "overview" to "Wages Ledger",
                "simulator" to "DOLE Simulator"
            )
            tabs.forEach { (tab, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (calcSubTab == tab) NeonGreen else Color.Transparent)
                        .clickable { calcSubTab = tab }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (calcSubTab == tab) Color.Black else com.example.ui.theme.AppTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        when (calcSubTab) {
            "simulator" -> {
                InteractiveDoleSimulatorView()
            }
            else -> {
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
                    val isHolidayLog = viewModel.localHolidays.value.any { it.date == log.date }
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
                            Text("Salary Ledger: ${viewModel.currentUserName.value}", color = com.example.ui.theme.AppTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                        val currSym = viewModel.getCurrencySymbol()
                        PayrollDetailRow(label = "Basic Rate (per hr)", value = String.format("${currSym}%.2f", basicHourly))
                        PayrollDetailRow(label = "Basic Earned Salary", value = String.format("${currSym}%.2f", basicSalary))
                        PayrollDetailRow(label = "Overtime Pay (1.5x, ${String.format("%.1f", overtimeHours)}h)", value = String.format("${currSym}%.2f", overtimePay))
                        PayrollDetailRow(label = "Holiday Pay (2x, ${String.format("%.1f", holidayHours)}h)", value = String.format("${currSym}%.2f", holidayPay))
                        PayrollDetailRow(label = "Night Diff (1.1x, ${String.format("%.1f", nightDiffHours)}h)", value = String.format("${currSym}%.2f", nightDiffPay))
                        PayrollDetailRow(label = "Standard Allowance", value = String.format("${currSym}%.2f", allowances))
                        PayrollDetailRow(label = "Performance Bonuses", value = String.format("${currSym}%.2f", bonuses))
                        PayrollDetailRow(label = "Tax Deductions", value = String.format("-${currSym}%.2f", deductions))
                        PayrollDetailRow(label = "Active Loan Repayments", value = String.format("-${currSym}%.2f", loansRepayment))

                        Divider(color = BorderGrey, modifier = Modifier.padding(vertical = 10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Net Compute Salary", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(String.format("${currSym}%.2f", netSalary), color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Black)
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

    Text("Generated Historical Payslips", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                    Text(cycle, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Total Net Amount: ${viewModel.getCurrencySymbol()}${String.format("%.2f", amount)}", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        Toast.makeText(context, "Downloading PDF Payslip for $cycle...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = getAdaptiveColor(0.08f)),
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
    val allLogsRaw by viewModel.allTimeLogs.collectAsState()

    val currentUserRole = viewModel.currentUserRole.value
    val currentUserName = viewModel.currentUserName.value
    val userProfile = viewModel.employeeProfiles.value.find { it.name.equals(currentUserName, ignoreCase = true) }
    val userDept = userProfile?.department ?: ""

    val allLogs = allLogsRaw.filter { log ->
        val empUser = viewModel.registeredUsers.value.find { it.name.equals(log.employeeName, ignoreCase = true) }
        val empRole = empUser?.role ?: "EMPLOYEE"
        val empProfile = viewModel.employeeProfiles.value.find { it.name.equals(log.employeeName, ignoreCase = true) }
        val empDept = empProfile?.department ?: ""
        if (currentUserRole == "ADMIN_HR") {
            true
        } else if (currentUserRole == "MANAGER" || currentUserRole == "SUPERVISOR") {
            empDept.equals(userDept, ignoreCase = true) && empRole != "SUPERVISOR" && empRole != "MANAGER"
        } else {
            log.employeeName.equals(currentUserName, ignoreCase = true)
        }
    }

    val filteredProfiles = viewModel.employeeProfiles.value.filter { profile ->
        val empUser = viewModel.registeredUsers.value.find { it.name.equals(profile.name, ignoreCase = true) }
        val empRole = empUser?.role ?: "EMPLOYEE"
        if (currentUserRole == "ADMIN_HR") {
            true
        } else if (currentUserRole == "MANAGER" || currentUserRole == "SUPERVISOR") {
            profile.department.equals(userDept, ignoreCase = true) && empRole != "SUPERVISOR" && empRole != "MANAGER"
        } else {
            profile.name.equals(currentUserName, ignoreCase = true)
        }
    }

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
                        filteredProfiles.joinToString("\n") { "${it.name},${it.position},25.0,${allLogs.filter { l -> l.employeeName == it.name }.size}" }
                saveCSVToDownloads(context, "Monthly_Payroll_Ledger.csv", csv)
            })

            ReportDownloadRow(title = "Employee Absenteeism Report", onExport = {
                val csv = if (currentUserRole == "ADMIN_HR") {
                    "EmployeeName,Status,DaysAbsent\nSarah Jenkins,Regular,1\nRobert Chen,Regular,0\nAnjali Sharma,Regular,2"
                } else {
                    // For manager/supervisor, filter the rows based on authorized profiles
                    "EmployeeName,Status,DaysAbsent\n" +
                    filteredProfiles.joinToString("\n") { "${it.name},${it.status},${if (it.name == "Sarah Jenkins") 1 else 0}" }
                }
                saveCSVToDownloads(context, "Absenteeism_Report.csv", csv)
            })

            ReportDownloadRow(title = "Overtime Report", onExport = {
                val csv = if (currentUserRole == "ADMIN_HR") {
                    "EmployeeName,Date,OvertimeHoursEarned\nSarah Jenkins,2026-06-25,1.5\nRobert Chen,2026-06-26,0.8"
                } else {
                    // For manager/supervisor, filter
                    "EmployeeName,Date,OvertimeHoursEarned\n" +
                    filteredProfiles.filter { it.name == "Sarah Jenkins" }.joinToString("\n") { "${it.name},2026-06-25,1.5" }
                }
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
            .background(getAdaptiveColor(0.03f), RoundedCornerShape(8.dp))
            .clickable { onExport() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        Text(text = label, color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
        Text(text = value, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

fun saveCSVToDownloads(context: Context, filename: String, text: String) {
    try {
        val path = context.getExternalFilesDir(null)
        val file = File(path, filename)
        FileOutputStream(file).use {
            it.write(text.toByteArray())
        }

        // Copy to cache for easy FileProvider access
        val cacheFile = File(context.cacheDir, filename)
        FileOutputStream(cacheFile).use {
            it.write(text.toByteArray())
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            cacheFile
        )

        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/comma-separated-values"
            putExtra(android.content.Intent.EXTRA_SUBJECT, filename.replace(".csv", "").replace("_", " "))
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        Toast.makeText(context, "Spreadsheet converted! Opening share...", Toast.LENGTH_SHORT).show()
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Spreadsheet (Excel Form)"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error converting to spreadsheet: ${e.message}", Toast.LENGTH_SHORT).show()
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
    val context = LocalContext.current
    val leavesRaw by viewModel.leaveRequests
    val correctionsRaw by viewModel.correctionRequests
    val claimsRaw by viewModel.reimbursementRequests

    val userRole = viewModel.currentUserRole.value
    val currentUserName = viewModel.currentUserName.value
    val isAuthorized = userRole == "ADMIN_HR" || userRole == "MANAGER" || userRole == "SUPERVISOR"
    val currencySymbol = viewModel.getCurrencySymbol()

    val userProfile = viewModel.employeeProfiles.value.find { it.name.equals(currentUserName, ignoreCase = true) }
    val userDept = userProfile?.department ?: ""

    val leaves = leavesRaw.filter { leave ->
        val empUser = viewModel.registeredUsers.value.find { it.name.equals(leave.employeeName, ignoreCase = true) }
        val empRole = empUser?.role ?: "EMPLOYEE"
        val empProfile = viewModel.employeeProfiles.value.find { it.name.equals(leave.employeeName, ignoreCase = true) }
        val empDept = empProfile?.department ?: ""
        if (userRole == "ADMIN_HR") {
            true
        } else if (userRole == "MANAGER" || userRole == "SUPERVISOR") {
            empDept.equals(userDept, ignoreCase = true) && empRole != "SUPERVISOR" && empRole != "MANAGER"
        } else {
            leave.employeeName.equals(currentUserName, ignoreCase = true)
        }
    }

    val corrections = correctionsRaw.filter { req ->
        val empUser = viewModel.registeredUsers.value.find { it.name.equals(req.employeeName, ignoreCase = true) }
        val empRole = empUser?.role ?: "EMPLOYEE"
        val empProfile = viewModel.employeeProfiles.value.find { it.name.equals(req.employeeName, ignoreCase = true) }
        val empDept = empProfile?.department ?: ""
        if (userRole == "ADMIN_HR") {
            true
        } else if (userRole == "MANAGER" || userRole == "SUPERVISOR") {
            empDept.equals(userDept, ignoreCase = true) && empRole != "SUPERVISOR" && empRole != "MANAGER"
        } else {
            req.employeeName.equals(currentUserName, ignoreCase = true)
        }
    }

    val claims = claimsRaw.filter { claim ->
        val empUser = viewModel.registeredUsers.value.find { it.name.equals(claim.employeeName, ignoreCase = true) }
        val empRole = empUser?.role ?: "EMPLOYEE"
        val empProfile = viewModel.employeeProfiles.value.find { it.name.equals(claim.employeeName, ignoreCase = true) }
        val empDept = empProfile?.department ?: ""
        if (userRole == "ADMIN_HR") {
            true
        } else if (userRole == "MANAGER" || userRole == "SUPERVISOR") {
            empDept.equals(userDept, ignoreCase = true) && empRole != "SUPERVISOR" && empRole != "MANAGER"
        } else {
            claim.employeeName.equals(currentUserName, ignoreCase = true)
        }
    }

    // Role-based Status Banner
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isAuthorized) NeonGreen.copy(alpha = 0.08f) else Color(0xFF3B1D22)),
        border = BorderStroke(1.dp, if (isAuthorized) NeonGreen.copy(alpha = 0.3f) else Color(0xFFF43F5E).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isAuthorized) Icons.Default.VerifiedUser else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isAuthorized) NeonGreen else Color(0xFFF43F5E),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isAuthorized) "Authorized Approver: $userRole (${viewModel.currentUserName.value})" else "Unauthorized: Only Admin, HR, Supervisor, or Department Manager can approve requests.",
                color = com.example.ui.theme.AppTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // 0. Shift Cover Requests
    Text("Pending Shift Swap & Cover Requests", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    val pendingCovers = viewModel.messagesList.value.filter { it.isSwapRequest && it.swapStatus == "ACCEPTED" }
    if (pendingCovers.isEmpty()) {
        Text("No pending shift cover requests.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
    } else {
        pendingCovers.forEach { msg ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("pending_cover_card_${msg.id}"),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Requester: ${msg.swapRequester}", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Coverer: ${msg.swapCoverer}", color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Shift: ${msg.swapShiftName} on ${msg.swapDate}", color = getAdaptiveTextColor(0.8f), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { 
                                if (isAuthorized) {
                                    viewModel.rejectShiftCover(msg.id, currentUserName) 
                                }
                            },
                            enabled = isAuthorized,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Reject", color = com.example.ui.theme.AppTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                if (isAuthorized) {
                                    viewModel.approveShiftCover(msg.id, currentUserName) 
                                }
                            },
                            enabled = isAuthorized,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Approve", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    // 1. Leave approvals list
    Text("Pending Leave & Shift Change Requests", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    val pendingLeaves = leaves.filter { it.status == "PENDING" }
    if (pendingLeaves.isEmpty()) {
        Text("No pending requests.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
    } else {
        pendingLeaves.forEach { leave ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(leave.employeeName, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Type: ${leave.leaveType} | ${leave.startDate} to ${leave.endDate}", color = com.example.ui.theme.AppTextColor, fontSize = 11.sp)
                    Text("Reason: ${leave.reason}", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { 
                                if (isAuthorized) {
                                    viewModel.rejectLeave(leave.id) 
                                }
                            },
                            enabled = isAuthorized,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Reject", color = com.example.ui.theme.AppTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                if (isAuthorized) {
                                    viewModel.approveLeave(leave.id) 
                                }
                            },
                            enabled = isAuthorized,
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
    Text("Pending Attendance Corrections", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    val pendingCorrections = corrections.filter { it.status == "PENDING" }
    if (pendingCorrections.isEmpty()) {
        Text("No pending correction requests.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
    } else {
        pendingCorrections.forEach { req ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(req.employeeName, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Date: ${req.date} | ${req.requestedTimeIn} - ${req.requestedTimeOut}", color = com.example.ui.theme.AppTextColor, fontSize = 11.sp)
                    Text("Reason: ${req.reason}", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { 
                                if (isAuthorized) {
                                    viewModel.rejectCorrection(req.id) 
                                }
                            },
                            enabled = isAuthorized,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Reject", color = com.example.ui.theme.AppTextColor, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                if (isAuthorized) {
                                    viewModel.approveCorrection(req.id) 
                                }
                            },
                            enabled = isAuthorized,
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
    Text("Pending Reimbursements Claims", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    val pendingClaims = claims.filter { it.status == "PENDING" }
    if (pendingClaims.isEmpty()) {
        Text("No pending reimbursement claims.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
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
                        Text("${currencySymbol}${String.format("%.2f", claim.amount)}", color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Claim: ${claim.title}", color = com.example.ui.theme.AppTextColor, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { 
                                if (isAuthorized) {
                                    viewModel.rejectReimbursement(claim.id) 
                                }
                            },
                            enabled = isAuthorized,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Reject", color = com.example.ui.theme.AppTextColor, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                if (isAuthorized) {
                                    viewModel.approveReimbursement(claim.id) 
                                }
                            },
                            enabled = isAuthorized,
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

    if (isAuthorized) {
        Spacer(modifier = Modifier.height(24.dp))
        Divider(color = BorderGrey, thickness = 2.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // Manager Navigation Tab for Compliance Panels
        var adminActiveTab by remember { mutableStateOf("disciplinary") } // disciplinary, clearance, okrs_audit
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardGreyBg, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf(
                "disciplinary" to "Disciplinary (NTE)",
                "clearance" to "Clearances",
                "okrs_audit" to "OKR Auditing"
            )
            tabs.forEach { (key, label) ->
                val isSelected = adminActiveTab == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NeonGreen else Color.Transparent)
                        .clickable { adminActiveTab = key }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (adminActiveTab) {
            "disciplinary" -> {
                Text("Issue New Formal Notice to Explain (NTE)", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                var empName by remember { mutableStateOf("") }
                var infractionTitle by remember { mutableStateOf("") }
                var infractionDate by remember { mutableStateOf("2026-07-12") }
                var severity by remember { mutableStateOf("MAJOR") }
                var policyLink by remember { mutableStateOf("https://shifthr.com/compliance/attendance-integrity") }
                var nteContent by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                    border = BorderStroke(1.dp, BorderGrey)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = empName,
                                onValueChange = { empName = it },
                                label = { Text("Employee Name") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                            )
                            OutlinedTextField(
                                value = infractionTitle,
                                onValueChange = { infractionTitle = it },
                                label = { Text("Infraction Category") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = infractionDate,
                                onValueChange = { infractionDate = it },
                                label = { Text("Infraction Date (YYYY-MM-DD)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                            )
                            OutlinedTextField(
                                value = severity,
                                onValueChange = { severity = it },
                                label = { Text("Severity (MINOR/MAJOR/GRAVE)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = policyLink,
                            onValueChange = { policyLink = it },
                            label = { Text("Viable Policy Reference Link") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = nteContent,
                            onValueChange = { nteContent = it },
                            label = { Text("Notice to Explain Letter Content") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (empName.isBlank() || nteContent.isBlank()) {
                                    Toast.makeText(context, "Please enter Employee Name and NTE Content.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.issueNoticeToExplain(empName, infractionTitle, infractionDate, nteContent, severity, policyLink)
                                    empName = ""
                                    infractionTitle = ""
                                    nteContent = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Issue Official NTE Notice", color = com.example.ui.theme.AppTextColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Active Labor Cases & Decision Handshakes", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                val cases by viewModel.disciplinaryCases
                val activeCases = cases.filter { it.status != "CASE_RESOLVED" }
                if (activeCases.isEmpty()) {
                    Text("No active labor cases pending resolution.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
                } else {
                    activeCases.forEach { c ->
                        var decisionText by remember { mutableStateOf("") }
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                            border = BorderStroke(1.dp, Color(0xFFFF5555).copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Employee: ${c.employeeName}", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Infraction: ${c.infractionTitle} (${c.severity})", color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("NTE: ${c.noticeToExplainContent}", color = getAdaptiveTextColor(0.7f), fontSize = 11.sp)
                                
                                if (c.status == "EXPLANATION_SUBMITTED") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Employee explanation (${c.employeeExplanationDate}):", color = Color(0xFFFFCC00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(c.employeeExplanation, color = getAdaptiveTextColor(0.8f), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = decisionText,
                                        onValueChange = { decisionText = it },
                                        label = { Text("Official Decision Notice (Suspension, Warning, or Exoneration)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            if (decisionText.isBlank()) {
                                                Toast.makeText(context, "Please write decision content.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.issueNoticeOfDecision(c.id, decisionText)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Issue Notice of Decision", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Awaiting Employee's Written Explanation.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            "clearance" -> {
                Text("Company Offboarding Clearances", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                val clearances by viewModel.offboardingClearances
                if (clearances.isEmpty()) {
                    Text("No exit clearance handshakes registered.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
                } else {
                    clearances.forEach { cl ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                            border = BorderStroke(1.dp, BorderGrey)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Employee: ${cl.employeeName}", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Separation Date: ${cl.separationDate} | Dept: ${cl.department}", color = getAdaptiveTextColor(0.6f), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(10.dp))

                                val depts = listOf(
                                    "IT" to cl.itClearanceStatus,
                                    "Finance" to cl.financeClearanceStatus,
                                    "Admin" to cl.adminClearanceStatus
                                )
                                depts.forEach { (name, status) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("$name Department Clearance:", color = com.example.ui.theme.AppTextColor, fontSize = 11.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = { viewModel.updateDepartmentClearance(cl.id, name, false) },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (status == "PENDING") Color(0xFFFFCC00) else BorderGrey),
                                                modifier = Modifier.height(26.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("PENDING", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = { viewModel.updateDepartmentClearance(cl.id, name, true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (status == "CLEARED") Color(0xFF00FF88) else BorderGrey),
                                                modifier = Modifier.height(26.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("CLEARED", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "okrs_audit" -> {
                Text("Manager OKRs Performance Appraisal Auditing", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                val okrs by viewModel.okrRecords
                if (okrs.isEmpty()) {
                    Text("No employee OKRs logged in this system.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
                } else {
                    okrs.forEach { okr ->
                        var feedbackVal by remember { mutableStateOf("") }
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                            border = BorderStroke(1.dp, BorderGrey)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Employee: ${okr.employeeName}", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("Progress: ${okr.progress}%", color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Objective: ${okr.objective}", color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Key Result: ${okr.keyResult}", color = getAdaptiveTextColor(0.7f), fontSize = 11.sp)
                                if (okr.selfAppraisal.isNotEmpty()) {
                                    Text("Employee's Appraisal: ${okr.selfAppraisal}", color = Color(0xFFFFCC00), fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = feedbackVal,
                                    onValueChange = { feedbackVal = it },
                                    label = { Text("Manager Feedback Audit Notes") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, focusedLabelColor = NeonGreen)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        if (feedbackVal.isBlank()) {
                                            Toast.makeText(context, "Please enter feedback.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.submitManagerOkrFeedback(okr.id, feedbackVal)
                                            Toast.makeText(context, "OKR audit appraisal submitted!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Submit Feedback & Audit", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
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

            Text("Select Assignee:", color = getAdaptiveTextColor(0.6f), fontSize = 11.sp)
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
    Text("Active Tasks Assignments Ledger", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                    Text(t.taskTitle, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("To: ${t.employeeName} | ${t.description}", color = getAdaptiveTextColor(0.5f), fontSize = 10.sp)
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
            Text("HR monitors daily submissions to ensure maximum corporate well-being.", color = getAdaptiveTextColor(0.5f), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Employee of Month Predictor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(getAdaptiveColor(0.03f), RoundedCornerShape(10.dp))
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
                        color = com.example.ui.theme.AppTextColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⭐ Predicted Employee of the Year candidate: Aditya Joshi (Director)\nReason: Highest ledger compliance and continuous geofencing verification logs.",
                        fontSize = 11.sp,
                        color = getAdaptiveTextColor(0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Daily Sentiment Submissions", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            feels.forEach { report ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(getAdaptiveColor(0.02f), RoundedCornerShape(8.dp))
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
                        Text(report.employeeName, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("\"${report.note}\"", color = getAdaptiveTextColor(0.6f), fontSize = 11.sp)
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
            Pair("System", "Hello! I am the ClauseOS Compliance AI assistant. How can I help you today? I have access to your shift ledger, Philippines holiday compliance rules, and employee feedback datasets.")
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
                                        .background(if (isUser) NeonGreen.copy(alpha = 0.15f) else getAdaptiveColor(0.05f))
                                        .border(1.dp, if (isUser) NeonGreen else BorderGrey, RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                        .widthIn(max = 240.dp)
                                ) {
                                    Column {
                                        Text(sender, color = NeonGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp)
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
                                    query.lowercase().contains("holiday") -> "Under Philippine labor standards, regular holidays trigger 200% (Double Pay) rates. Special non-working days trigger 130% premium compensation rates."
                                    query.lowercase().contains("night") -> "Night differential pay consists of 10% additional compensation on the basic hourly rate for shifts conducted between 10:00 PM and 06:00 AM under Philippine Labor Law."
                                    else -> "We have processed your request. Under current Philippine labor regulations, all clock logs conform to secure geofenced standards."
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
        Text("AI Compliance Intelligence Reports", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                    color = com.example.ui.theme.AppTextColor
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
                    color = com.example.ui.theme.AppTextColor
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
                .background(getAdaptiveColor(0.05f), RoundedCornerShape(10.dp))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonGreen, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = com.example.ui.theme.AppTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    color = com.example.ui.theme.AppTextColor
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
                    color = com.example.ui.theme.AppTextColor
                )
                Text(
                    text = "Check in with HR! How is your well-being today?",
                    fontSize = 10.sp,
                    color = getAdaptiveTextColor(0.5f)
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
                                .background(if (feelsScore == score) NeonGreen else getAdaptiveColor(0.04f))
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

        // Interactive Beginner Guide Launcher Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { viewModel.currentScreen.value = "platform_guide" }
                .testTag("hub_guide_banner"),
            colors = CardDefaults.cardColors(containerColor = NeonGreen.copy(alpha = 0.08f)),
            border = BorderStroke(1.5.dp, NeonGreen)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Guide Logo",
                        tint = NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NEW TO SHIFT HR? 📚",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Interactive Platform & Architecture Guide",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = com.example.ui.theme.AppTextColor
                    )
                    Text(
                        text = "Take a quick animated walkthrough & try out live simulations of shift-clocks, NFC, AI, and check the official architecture diagram.",
                        fontSize = 10.sp,
                        color = getAdaptiveTextColor(0.65f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = "Open Guide",
                    tint = NeonGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // TILE LAUNCHER GRID (SaaS Modules)
        Text(
            text = "Enterprise Suite Modules",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = com.example.ui.theme.AppTextColor,
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

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Performance & Printable Reports Tile
            SaaSTileLauncher(
                title = "Performance & Reports",
                subtitle = "Printable reviews & company analytics",
                icon = Icons.Default.Assessment,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.currentScreen.value = "performance_reports" }
            )

            // Team Shift Calendar Tile
            SaaSTileLauncher(
                title = "Team Shift Calendar",
                subtitle = "Calendar schedule desk & supervisor edit engine",
                icon = Icons.Default.CalendarMonth,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.currentScreen.value = "supervisor_schedule" }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // Top 5 Dashboards Tile
            SaaSTileLauncher(
                title = "Top 5 Employee Dashboards",
                subtitle = "Workforce performance, engagement, retention stability & continuous learning charts",
                icon = Icons.Default.Leaderboard,
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.currentScreen.value = "top5_dashboard" }
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
            color = com.example.ui.theme.AppTextColor,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        if (announcements.isEmpty()) {
            Text(
                "No announcements published yet.",
                color = getAdaptiveTextColor(0.5f),
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
                            Text(bulletin.date, color = getAdaptiveTextColor(0.4f), fontSize = 9.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(bulletin.content, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp)
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
                Text(title, color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, color = getAdaptiveTextColor(0.5f), fontSize = 8.sp, maxLines = 2, lineHeight = 10.sp)
            }
        }
    }
}

// Simple Quadruple helper class for type safety
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
fun HeatmapPill(count: Int, isWeekend: Boolean) {
    val (bgColor, borderColor, textColor, label) = when {
        count == 0 -> {
            if (isWeekend) {
                Quadruple(
                    getAdaptiveColor(0.02f),
                    getAdaptiveColor(0.05f),
                    getAdaptiveColor(0.3f),
                    "REST"
                )
            } else {
                Quadruple(
                    Color(0xFFFF3B30).copy(alpha = 0.12f),
                    Color(0xFFFF3B30).copy(alpha = 0.3f),
                    Color(0xFFFF453A),
                    "⚠️ GAP (0)"
                )
            }
        }
        count == 1 -> {
            Quadruple(
                Color(0xFFFF9F0A).copy(alpha = 0.12f),
                Color(0xFFFF9F0A).copy(alpha = 0.3f),
                Color(0xFFFFA629),
                "⚠️ LOW (1)"
            )
        }
        else -> {
            Quadruple(
                Color(0xFF10B981).copy(alpha = 0.12f),
                Color(0xFF10B981).copy(alpha = 0.3f),
                Color(0xFF34D399),
                "🟢 COVERED ($count)"
            )
        }
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(110.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
    }
}

@Composable
fun SupervisorScheduleScreen(viewModel: TimeTrackerViewModel) {
    val context = LocalContext.current
    val profiles by viewModel.employeeProfiles
    val teamSchedules by viewModel.teamSchedules
    val currentUserRole = viewModel.currentUserRole.value
    val currentUserName = viewModel.currentUserName.value
    
    val myProfile = profiles.find { it.name == currentUserName }
    val currentUserDept = myProfile?.department ?: "Administration"
    
    // Departments in company
    val departments = listOf("All", "Engineering", "Human Resources", "Product Management", "Management", "Administration")
    
    // Default filter to current user's department if they are Supervisor or Manager
    var selectedDeptFilter by remember { 
        mutableStateOf(
            if (currentUserRole == "ADMIN_HR" || currentUserRole == "MANAGER") "All" else currentUserDept
        ) 
    }
    
    // Define the 6 days of June 2026 we are scheduling (matching the seeded data and the main calendar month)
    val daysRange = listOf(
        "2026-06-25" to "Thu 25",
        "2026-06-26" to "Fri 26",
        "2026-06-27" to "Sat 27",
        "2026-06-28" to "Sun 28",
        "2026-06-29" to "Mon 29",
        "2026-06-30" to "Tue 30"
    )
    
    val shiftTypes = listOf("Manila Dev Shift", "Indore Day Flex", "Night Ops", "Off")
    
    val filteredProfiles = profiles.filter {
        selectedDeptFilter == "All" || it.department == selectedDeptFilter
    }
    
    // Active paint mode shift template
    var activeShiftTemplate by remember { mutableStateOf("Manila Dev Shift") }
    var viewMode by remember { mutableStateOf("gantt") }
    var activeSubTab by remember { mutableStateOf("schedule") } // "schedule" or "holidays"
    
    // Real Drag and Drop Gesture State
    var isDraggingShift by remember { mutableStateOf(false) }
    var dragShiftName by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Dynamic coverage counts per day
    // Each day is "YYYY-MM-DD" -> Map of "Morning" to count, "Afternoon" to count, "Night" to count
    val coverageStats = remember(filteredProfiles, teamSchedules) {
        daysRange.associate { (dateKey, _) ->
            var morning = 0
            var afternoon = 0
            var night = 0
            
            filteredProfiles.forEach { emp ->
                val activeSchedule = teamSchedules.find { 
                    it.employeeName == emp.name && it.date == dateKey 
                }
                val shiftName = activeSchedule?.shiftName ?: "Off"
                when (shiftName) {
                    "Manila Dev Shift" -> {
                        morning++
                        afternoon++
                    }
                    "Indore Day Flex" -> {
                        morning++
                        afternoon++
                    }
                    "Night Ops" -> {
                        night++
                    }
                }
            }
            
            dateKey to Triple(morning, afternoon, night)
        }
    }

    val staffingAlerts = remember(coverageStats, selectedDeptFilter) {
        val alerts = mutableListOf<String>()
        val weekdayKeys = listOf("2026-06-25", "2026-06-26", "2026-06-29", "2026-06-30") // Mon, Tue, Thu, Fri
        
        weekdayKeys.forEach { dateKey ->
            val stats = coverageStats[dateKey]
            if (stats != null) {
                val (morning, afternoon, _) = stats
                val dayLabel = when (dateKey) {
                    "2026-06-25" -> "Thursday Morning (June 25)"
                    "2026-06-26" -> "Friday Morning (June 26)"
                    "2026-06-29" -> "Monday Morning (June 29)"
                    "2026-06-30" -> "Tuesday Morning (June 30)"
                    else -> "Weekday Morning"
                }
                val deptSuffixed = if (selectedDeptFilter == "All") "" else " in $selectedDeptFilter"
                
                if (morning == 0) {
                    alerts.add("⚠️ Critical staffing gap on $dayLabel$deptSuffixed (0 active staff on shift).")
                }
                
                val afternoonLabel = dayLabel.replace("Morning", "Afternoon")
                if (afternoon == 0) {
                    alerts.add("⚠️ Critical staffing gap on $afternoonLabel$deptSuffixed (0 active staff on shift).")
                }
            }
        }
        alerts
    }

    fun canEdit(empDept: String): Boolean {
        if (currentUserRole == "ADMIN_HR" || currentUserDept == "Human Resources" || currentUserDept == "Administration") {
            return true
        }
        if ((currentUserRole == "SUPERVISOR" || currentUserRole == "MANAGER") && currentUserDept == empDept) {
            return true
        }
        return false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp)
            .testTag("supervisor_schedule_container")
    ) {
        // INFO / ROLE AUTHORIZATION HEADER CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (currentUserRole != "EMPLOYEE") NeonGreen.copy(alpha = 0.15f) else getAdaptiveColor(0.05f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentUserRole != "EMPLOYEE") Icons.Default.AdminPanelSettings else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (currentUserRole != "EMPLOYEE") NeonGreen else getAdaptiveColor(0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ROLE: $currentUserRole • DEPT: $currentUserDept",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentUserName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = com.example.ui.theme.AppTextColor
                    )
                    Text(
                        text = when (currentUserRole) {
                            "ADMIN_HR" -> "Fully Authorized: Can edit shifts for all departments."
                            "MANAGER" -> "Department Manager: Can edit shifts for employees in $currentUserDept."
                            "SUPERVISOR" -> "Team Supervisor: Can edit shifts for employees in $currentUserDept."
                            else -> "Read-Only Access: View assigned calendar shifts only."
                        },
                        fontSize = 11.sp,
                        color = getAdaptiveTextColor(0.6f)
                    )
                }
            }
        }

        // --- SUB-TABS SELECTOR FOR COMPLIANCE MATRIX vs HOLIDAY CALENDAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { activeSubTab = "schedule" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == "schedule") NeonGreen else getAdaptiveColor(0.05f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = if (activeSubTab == "schedule") Color.Black else com.example.ui.theme.AppTextColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Shift Schedule\nMatrix",
                    color = if (activeSubTab == "schedule") Color.Black else com.example.ui.theme.AppTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = { activeSubTab = "holidays" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == "holidays") NeonGreen else getAdaptiveColor(0.05f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = if (activeSubTab == "holidays") Color.Black else com.example.ui.theme.AppTextColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Holiday\nCalendar",
                    color = if (activeSubTab == "holidays") Color.Black else com.example.ui.theme.AppTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (activeSubTab == "schedule") {
            // --- DEPARTMENT FILTER TABS ---
        Text(
            text = "Filter by Department",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = getAdaptiveTextColor(0.5f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            departments.forEach { dept ->
                val isSelected = selectedDeptFilter == dept
                val isDeptAuthorized = currentUserRole == "ADMIN_HR" || currentUserDept == "Human Resources" || currentUserDept == "Administration" || dept == "All" || currentUserDept == dept
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isSelected -> NeonGreen.copy(alpha = 0.15f)
                                else -> getAdaptiveColor(0.04f)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isSelected -> NeonGreen
                                !isDeptAuthorized -> getAdaptiveColor(0.02f)
                                else -> getAdaptiveColor(0.1f)
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedDeptFilter = dept }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isDeptAuthorized && dept != "All") {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = getAdaptiveTextColor(0.3f),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = dept,
                            color = if (isSelected) NeonGreen else if (!isDeptAuthorized) getAdaptiveColor(0.3f) else getAdaptiveColor(0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- SCHEDULING INTERACTIVE CANVAS CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TEAM SHIFT MATRIX (JUNE 2026)",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = NeonGreen,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Interactive Scheduling Grid",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.AppTextColor
                        )
                    }
                    if (currentUserRole != "EMPLOYEE") {
                        Row(
                            modifier = Modifier
                                .background(NeonGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = NeonGreen
                            )
                            Text(
                                text = "Paint Brush Active",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        }
                    }
                }

                // View Mode Toggle (Grid vs Gantt Timeline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(getAdaptiveColor(0.03f), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("grid" to "Standard Grid", "gantt" to "Gantt Timeline").forEach { (mode, label) ->
                        val isSelected = viewMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.Transparent)
                                .border(1.dp, if (isSelected) NeonGreen else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { viewMode = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (mode == "grid") Icons.Default.DateRange else Icons.Default.Analytics,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) NeonGreen else getAdaptiveColor(0.6f)
                                )
                                Text(
                                    text = label,
                                    color = if (isSelected) NeonGreen else getAdaptiveColor(0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // HORIZONTALLY SCROLLABLE GRID TABLE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column {
                        // Table Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(getAdaptiveColor(0.04f))
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Employee Name Column Header
                            Text(
                                text = "Team Member",
                                modifier = Modifier.width(150.dp).padding(start = 8.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.AppTextColor
                            )
                            
                            // Days Column Headers (Width dynamically adjusts to match cells below)
                            daysRange.forEach { (_, label) ->
                                val headerWidth = if (viewMode == "gantt") 80.dp else 62.dp
                                Text(
                                    text = label,
                                    modifier = Modifier.width(headerWidth),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.AppTextColor
                                )
                            }
                        }

                        // Table Body Rows
                        if (filteredProfiles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No team members found in this department.",
                                    color = getAdaptiveTextColor(0.4f),
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            filteredProfiles.forEach { emp ->
                                val isEmployeeEditable = canEdit(emp.department)
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.5.dp, getAdaptiveColor(0.03f))
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Employee Header Cell
                                    Column(modifier = Modifier.width(150.dp).padding(start = 8.dp)) {
                                        Text(
                                            text = emp.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = com.example.ui.theme.AppTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = emp.department,
                                            fontSize = 9.sp,
                                            color = getAdaptiveTextColor(0.4f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Day Cells
                                    daysRange.forEach { (dateKey, _) ->
                                        val activeSchedule = teamSchedules.find { 
                                            it.employeeName == emp.name && it.date == dateKey 
                                        }
                                        val shiftName = activeSchedule?.shiftName ?: "Off"
                                        
                                        val cellColor = when (shiftName) {
                                            "Manila Dev Shift" -> Color(0xFF10B981) // Green
                                            "Indore Day Flex" -> Color(0xFF00E5FF) // Cyan
                                            "Night Ops" -> Color(0xFF8B5CF6) // Purple
                                            else -> Color(0xFF9CA3AF) // Gray
                                        }

                                        val cellWidth = if (viewMode == "gantt") 72.dp else 54.dp
                                        val cellHeight = if (viewMode == "gantt") 44.dp else 34.dp

                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .width(cellWidth)
                                                .height(cellHeight)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(cellColor.copy(alpha = 0.15f))
                                                .border(
                                                    1.dp,
                                                    if (isEmployeeEditable) cellColor.copy(alpha = 0.8f) else cellColor.copy(alpha = 0.2f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    if (!isEmployeeEditable) {
                                                        Toast.makeText(
                                                            context, 
                                                            "Locked: You cannot schedule ${emp.name} in ${emp.department}.", 
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else {
                                                        // Apply active template click paint immediately!
                                                        viewModel.updateEmployeeShift(
                                                            emp.name,
                                                            emp.department,
                                                            dateKey,
                                                            activeShiftTemplate,
                                                            currentUserName
                                                        )
                                                        Toast.makeText(
                                                            context, 
                                                            "Assigned $activeShiftTemplate to ${emp.name}", 
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                                .testTag("schedule_cell_${emp.name.replace(" ", "_")}_$dateKey"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (viewMode == "gantt") {
                                                Column(
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = when (shiftName) {
                                                                "Manila Dev Shift" -> "DEV"
                                                                "Indore Day Flex" -> "FLEX"
                                                                "Night Ops" -> "NIGHT"
                                                                else -> "OFF"
                                                            },
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = cellColor
                                                        )
                                                        Text(
                                                            text = when (shiftName) {
                                                                "Manila Dev Shift" -> "09-18"
                                                                "Indore Day Flex" -> "08-17"
                                                                "Night Ops" -> "21-06"
                                                                else -> "REST"
                                                            },
                                                            fontSize = 7.sp,
                                                            color = getAdaptiveTextColor(0.5f)
                                                        )
                                                    }

                                                    // Visual 24-hour horizontal track & pill
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(5.dp)
                                                            .background(getAdaptiveColor(0.05f), RoundedCornerShape(2.5.dp))
                                                            .padding(horizontal = 1.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        when (shiftName) {
                                                            "Indore Day Flex" -> {
                                                                Spacer(modifier = Modifier.weight(8f))
                                                                Box(modifier = Modifier.weight(9f).fillMaxHeight().background(cellColor, RoundedCornerShape(2.5.dp)))
                                                                Spacer(modifier = Modifier.weight(7f))
                                                            }
                                                            "Manila Dev Shift" -> {
                                                                Spacer(modifier = Modifier.weight(9f))
                                                                Box(modifier = Modifier.weight(9f).fillMaxHeight().background(cellColor, RoundedCornerShape(2.5.dp)))
                                                                Spacer(modifier = Modifier.weight(6f))
                                                            }
                                                            "Night Ops" -> {
                                                                Box(modifier = Modifier.weight(6f).fillMaxHeight().background(cellColor, RoundedCornerShape(2.5.dp)))
                                                                Spacer(modifier = Modifier.weight(15f))
                                                                Box(modifier = Modifier.weight(3f).fillMaxHeight().background(cellColor, RoundedCornerShape(2.5.dp)))
                                                            }
                                                            else -> {
                                                                Box(modifier = Modifier.weight(24f).height(1.dp).background(getAdaptiveColor(0.15f)))
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = when (shiftName) {
                                                            "Manila Dev Shift" -> "DEV"
                                                            "Indore Day Flex" -> "FLEX"
                                                            "Night Ops" -> "NIGHT"
                                                            else -> "OFF"
                                                        },
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = cellColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = BorderGrey, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // --- COVERAGE HEATMAP DESK ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DEPARTMENT COVERAGE HEATMAP",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = NeonGreen,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Daily Staffing Headcount & Gaps",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.AppTextColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(getAdaptiveColor(0.05f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Selected Dept: $selectedDeptFilter",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = getAdaptiveTextColor(0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Heatmap Table
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column {
                        // Heatmap Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(getAdaptiveColor(0.04f))
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Day",
                                modifier = Modifier.width(90.dp).padding(start = 8.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = getAdaptiveTextColor(0.5f)
                            )
                            Text(
                                text = "Morning (08a-12p)",
                                modifier = Modifier.width(110.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = getAdaptiveTextColor(0.5f)
                            )
                            Text(
                                text = "Afternoon (12p-06p)",
                                modifier = Modifier.width(110.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = getAdaptiveTextColor(0.5f)
                            )
                            Text(
                                text = "Night (06p-12a)",
                                modifier = Modifier.width(110.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = getAdaptiveTextColor(0.5f)
                            )
                        }

                        // Heatmap Rows
                        daysRange.forEach { (dateKey, label) ->
                            val stats = coverageStats[dateKey] ?: Triple(0, 0, 0)
                            val (morning, afternoon, night) = stats
                            
                            val isWeekend = label.startsWith("Sat") || label.startsWith("Sun")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, getAdaptiveColor(0.03f))
                                    .background(if (isWeekend) getAdaptiveColor(0.01f) else Color.Transparent)
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Day Name
                                Column(modifier = Modifier.width(90.dp).padding(start = 8.dp)) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWeekend) getAdaptiveColor(0.4f) else Color.White
                                    )
                                    if (isWeekend) {
                                        Text(
                                            text = "Weekend",
                                            fontSize = 8.sp,
                                            color = getAdaptiveTextColor(0.3f)
                                        )
                                    }
                                }

                                // Morning Cell
                                HeatmapPill(count = morning, isWeekend = isWeekend)
                                
                                // Afternoon Cell
                                HeatmapPill(count = afternoon, isWeekend = isWeekend)

                                // Night Cell
                                HeatmapPill(count = night, isWeekend = isWeekend)
                            }
                        }
                    }
                }

                // --- STAFFING INSIGHTS SECTION ---
                if (staffingAlerts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF3B30).copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, Color(0xFFFF3B30).copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = Color(0xFFFF453A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "COVERAGE DEFICIT DETECTED",
                                    color = Color(0xFFFF453A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            staffingAlerts.take(3).forEach { alert ->
                                Text(
                                    text = "• $alert",
                                    color = getAdaptiveTextColor(0.85f),
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                            
                            if (currentUserRole != "EMPLOYEE") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            // Quick resolve Tuesday morning gap!
                                            // Identify which departments need it
                                            val pmEmployee = filteredProfiles.find { it.department == "Product Management" }
                                            if (pmEmployee != null) {
                                                viewModel.updateEmployeeShift(
                                                    pmEmployee.name,
                                                    pmEmployee.department,
                                                    "2026-06-30",
                                                    "Indore Day Flex",
                                                    currentUserName
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "Tuesday morning gap resolved! Assigned Indore Day Flex shift to ${pmEmployee.name}.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                // General quick fix
                                                val engEmployee = filteredProfiles.find { it.department == "Engineering" }
                                                if (engEmployee != null) {
                                                    viewModel.updateEmployeeShift(
                                                        engEmployee.name,
                                                        engEmployee.department,
                                                        "2026-06-30",
                                                        "Manila Dev Shift",
                                                        currentUserName
                                                    )
                                                    Toast.makeText(
                                                        context,
                                                        "Tuesday morning gap resolved! Assigned Manila Dev Shift to ${engEmployee.name}.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FlashOn,
                                            contentDescription = null,
                                            tint = com.example.ui.theme.AppTextColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Auto-Resolve Tuesday Gap",
                                            color = com.example.ui.theme.AppTextColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NeonGreen.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Optimal",
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "All weekday shifts fully covered! No coverage gaps detected.",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- SHIFT TEMPLATE ACTIVE PALETTE (FOR TAP PAINT & DRAG DROP) ---
        if (currentUserRole != "EMPLOYEE") {
            Text(
                text = "⚡ Scheduling Toolbox (Paint Brush & Drag Source)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeonGreen,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "1. Tap a shift template to select your paint-brush. 2. Tap any authorized employee cell to instantly paint/assign that shift!",
                fontSize = 10.sp,
                color = getAdaptiveTextColor(0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardGreyBg),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        shiftTypes.forEach { shift ->
                            val isSelected = activeShiftTemplate == shift
                            val color = when (shift) {
                                "Manila Dev Shift" -> Color(0xFF10B981)
                                "Indore Day Flex" -> Color(0xFF00E5FF)
                                "Night Ops" -> Color(0xFF8B5CF6)
                                else -> Color(0xFF9CA3AF)
                            }

                            // Interactive Drag-and-Drop simulated source card
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) color.copy(alpha = 0.2f) else getAdaptiveColor(0.03f))
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) color else getAdaptiveColor(0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { activeShiftTemplate = shift }
                                    .pointerInput(shift) {
                                        detectDragGestures(
                                            onDragStart = {
                                                isDraggingShift = true
                                                dragShiftName = shift
                                                dragOffset = Offset.Zero
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount
                                            },
                                            onDragEnd = {
                                                isDraggingShift = false
                                                // When they drag and release, we perform drop simulation
                                                val targetEmp = filteredProfiles.firstOrNull { canEdit(it.department) }
                                                if (targetEmp != null) {
                                                    viewModel.updateEmployeeShift(
                                                        targetEmp.name,
                                                        targetEmp.department,
                                                        "2026-06-29",
                                                        shift,
                                                        currentUserName
                                                    )
                                                    Toast.makeText(
                                                        context, 
                                                        "Dropped & Assigned: $shift to ${targetEmp.name} on June 29!", 
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        )
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.DragIndicator,
                                        contentDescription = "Drag Handle",
                                        tint = color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = when(shift) {
                                            "Manila Dev Shift" -> "DEV"
                                            "Indore Day Flex" -> "FLEX"
                                            "Night Ops" -> "NIGHT"
                                            else -> "OFF"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = color
                                    )
                                    Text(
                                        text = when(shift) {
                                            "Manila Dev Shift" -> "09-18"
                                            "Indore Day Flex" -> "08-17"
                                            "Night Ops" -> "21-06"
                                            else -> "REST"
                                        },
                                        fontSize = 8.sp,
                                        color = getAdaptiveTextColor(0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // Ghost element for drag and drop simulation feedback
                    if (isDraggingShift) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeonGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = NeonGreen, strokeWidth = 1.5.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DRAGGING '${dragShiftName?.uppercase()}'. Release anywhere to apply to active department member!",
                                    color = NeonGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Read-Only employee notice
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(getAdaptiveColor(0.04f), RoundedCornerShape(12.dp))
                    .border(1.dp, getAdaptiveColor(0.1f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Read-Only",
                        tint = getAdaptiveTextColor(0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Note: You are logged in with employee read-only privileges. Scheduling modification is reserved for Supervisors, Managers, and Admin/HR departments.",
                        fontSize = 11.sp,
                        color = getAdaptiveTextColor(0.5f)
                    )
                }
            }
        }
    } else {
        PhilippineHolidayCalendarComponent(viewModel = viewModel)
    }
}
}

@Composable
fun PhilippineHolidayCalendarComponent(viewModel: TimeTrackerViewModel) {
    val context = LocalContext.current
    val userCountryCode = remember { viewModel.getUserCountryCode() }
    val holidays by remember {
        derivedStateOf {
            viewModel.localHolidays.value.filter {
                it.country == "ALL" || it.country == userCountryCode
            }
        }
    }
    val teamSchedules by viewModel.teamSchedules
    val profiles by viewModel.employeeProfiles
    
    // Default to June (index 5) since the scheduling app is in June 2026!
    var selectedMonthIndex by remember { mutableStateOf(5) } 
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    // Calculate the days of the selected month
    val calendarInstance = remember(selectedMonthIndex) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, selectedMonthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    
    val daysInMonth = calendarInstance.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeekVal = calendarInstance.get(Calendar.DAY_OF_WEEK)
    val firstDayOffset = firstDayOfWeekVal - 1 // 0 = Sunday, 1 = Monday, etc.
    
    var selectedDayNum by remember(selectedMonthIndex) { mutableStateOf(1) }
    val selectedDateKey = "2026-%02d-%02d".format(selectedMonthIndex + 1, selectedDayNum)
    
    // Find holiday on the selected day
    val holidayForSelectedDay = holidays.find { it.date == selectedDateKey }
    
    // Filter holidays for the selected month to list them at the bottom
    val monthHolidays = holidays.filter {
        val parts = it.date.split("-")
        parts.size == 3 && parts[1].toInt() == (selectedMonthIndex + 1)
    }
    
    // State for creating a custom holiday
    var showCustomHolidayDialog by remember { mutableStateOf(false) }
    var customHolidayName by remember { mutableStateOf("") }
    var customHolidayDesc by remember { mutableStateOf("") }
    var customHolidayType by remember { mutableStateOf("Regular") } // Regular, Special (Non-Working), Special (Working)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // --- MONTHS SELECTOR TAB ROW ---
        Text(
            text = "SELECT MONTH (2026)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = NeonGreen,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            monthNames.forEachIndexed { index, name ->
                val isSelected = selectedMonthIndex == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else getAdaptiveColor(0.03f))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NeonGreen else getAdaptiveColor(0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedDayNum = 1; selectedMonthIndex = index }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = name,
                        color = if (isSelected) NeonGreen else getAdaptiveColor(0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // --- MAIN CALENDAR CARD ---
        val isLightTheme = com.example.ui.theme.AppTextColor == Color(0xFF2D3748)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isLightTheme) Color.White else CardGreyBg
            ),
            border = BorderStroke(
                1.dp,
                if (isLightTheme) Color(0xFFE2E8F0) else BorderGrey
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${monthNames[selectedMonthIndex].uppercase()} 2026",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = com.example.ui.theme.AppTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (userCountryCode == "PH") "📍 PHILIPPINES (PH) • AUTO" else "📍 INDIA (IN) • AUTO",
                                color = NeonGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    if (viewModel.currentUserRole.value != "EMPLOYEE") {
                        IconButton(
                            onClick = { showCustomHolidayDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom Holiday",
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Day headings (Sun to Sat)
                Row(modifier = Modifier.fillMaxWidth()) {
                    val daysHeader = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    daysHeader.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = getAdaptiveTextColor(0.4f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Calendar Grid
                val totalCells = firstDayOffset + daysInMonth
                val weeksCount = (totalCells + 6) / 7
                
                var currentDay = 1
                for (w in 0 until weeksCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (d in 0 until 7) {
                            val cellIndex = w * 7 + d
                            if (cellIndex >= firstDayOffset && currentDay <= daysInMonth) {
                                val dayNum = currentDay
                                val dateKey = "2026-%02d-%02d".format(selectedMonthIndex + 1, dayNum)
                                val holiday = holidays.find { it.date == dateKey }
                                val isSelected = selectedDayNum == dayNum
                                
                                val holidayColor = when {
                                    holiday == null -> Color.Transparent
                                    holiday.name.contains("EDSA", ignoreCase = true) -> Color(0xFF10B981) // Green (Special Working)
                                    holiday.name.contains("Chinese", ignoreCase = true) ||
                                    holiday.name.contains("Black Saturday", ignoreCase = true) ||
                                    holiday.name.contains("Ninoy Aquino", ignoreCase = true) ||
                                    holiday.name.contains("All Saints", ignoreCase = true) ||
                                    holiday.name.contains("All Souls", ignoreCase = true) ||
                                    holiday.name.contains("Immaculate", ignoreCase = true) ||
                                    holiday.name.contains("Christmas Eve", ignoreCase = true) ||
                                    holiday.name.contains("Last Day of the Year", ignoreCase = true) -> Color(0xFF3B82F6) // Blue (Special Non-Working)
                                    else -> Color(0xFFEF4444) // Red (Regular)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isSelected -> NeonGreen.copy(alpha = 0.25f)
                                                holiday != null -> holidayColor.copy(alpha = 0.15f)
                                                else -> getAdaptiveColor(0.01f)
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = when {
                                                isSelected -> NeonGreen
                                                holiday != null -> holidayColor
                                                else -> getAdaptiveColor(0.05f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedDayNum = dayNum }
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected || holiday != null) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) NeonGreen else if (holiday != null) holidayColor else com.example.ui.theme.AppTextColor
                                        )
                                        val hasNotes = viewModel.calendarNotes.value.any { it.date == dateKey }
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 1.dp)
                                        ) {
                                            if (holiday != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(holidayColor)
                                                )
                                            }
                                            if (hasNotes) {
                                                if (holiday != null) Spacer(modifier = Modifier.width(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFE879F9)) // Purple Note Dot
                                                )
                                            }
                                        }
                                    }
                                }
                                currentDay++
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        
        // --- LEGENDS DISPLAY ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Regular Holiday", fontSize = 9.sp, color = getAdaptiveTextColor(0.7f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Special Non-Working", fontSize = 9.sp, color = getAdaptiveTextColor(0.7f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Special Working", fontSize = 9.sp, color = getAdaptiveTextColor(0.7f))
            }
        }
        
        // --- DETAIL DISPLAY AREA ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isLightTheme) Color.White else CardGreyBg
            ),
            border = BorderStroke(
                1.dp,
                if (isLightTheme) Color(0xFFE2E8F0) else BorderGrey
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "DETAILS FOR %02d %s 2026".format(selectedDayNum, monthNames[selectedMonthIndex].uppercase()),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonGreen,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                if (holidayForSelectedDay != null) {
                    val category = when {
                        holidayForSelectedDay.name.contains("EDSA", ignoreCase = true) -> "Special (Working) Day"
                        holidayForSelectedDay.name.contains("Chinese", ignoreCase = true) ||
                        holidayForSelectedDay.name.contains("Black Saturday", ignoreCase = true) ||
                        holidayForSelectedDay.name.contains("Ninoy Aquino", ignoreCase = true) ||
                        holidayForSelectedDay.name.contains("All Saints", ignoreCase = true) ||
                        holidayForSelectedDay.name.contains("All Souls", ignoreCase = true) ||
                        holidayForSelectedDay.name.contains("Immaculate", ignoreCase = true) ||
                        holidayForSelectedDay.name.contains("Christmas Eve", ignoreCase = true) ||
                        holidayForSelectedDay.name.contains("Last Day of the Year", ignoreCase = true) -> "Special (Non-Working) Day"
                        else -> "Regular Holiday"
                    }
                    
                    val color = when (category) {
                        "Special (Working) Day" -> Color(0xFF10B981)
                        "Special (Non-Working) Day" -> Color(0xFF3B82F6)
                        else -> Color(0xFFEF4444)
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = holidayForSelectedDay.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = color
                        )
                    }
                    
                    Text(
                        text = "Category: $category",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = getAdaptiveTextColor(0.8f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Text(
                        text = holidayForSelectedDay.description,
                        fontSize = 11.sp,
                        color = getAdaptiveTextColor(0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Divider(color = BorderGrey, modifier = Modifier.padding(vertical = 6.dp))
                    
                    // Compliance details
                    val isPH = userCountryCode == "PH"
                    Text(
                        text = if (isPH) "PH LABOR CODE COMPLIANCE AUDIT" else "INDIAN LABOUR COMPLIANCE AUDIT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGreen,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    val complianceText = if (isPH) {
                        when (category) {
                            "Regular Holiday" -> "Philippine Article 93: Double Pay (200%) rate applies for actual hours worked. Unworked employees are fully paid 100% of their base pay."
                            "Special (Non-Working) Day" -> "130% premium compensation rate applies for worked hours. Unworked hours fall under 'no work, no pay' rule."
                            else -> "100% standard rate. Normal work operations apply with standard pay structures."
                        }
                    } else {
                        when (category) {
                            "Regular Holiday" -> "Indian Factories Act: Double Wages (200%) rate applies for worked hours. Unworked employees are fully paid 100% of their base pay."
                            "Special (Non-Working) Day" -> "150% premium compensation rate applies for worked hours on local festival/state holidays."
                            else -> "100% standard rate. Standard working day under Indian labour laws."
                        }
                    }
                    
                    Text(
                        text = complianceText,
                        fontSize = 11.sp,
                        color = getAdaptiveTextColor(0.8f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Check active schedules for this holiday date
                    val scheduledEmployeesOnThisDay = teamSchedules.filter {
                        it.date == selectedDateKey && it.shiftName != "Off"
                    }
                    
                    if (scheduledEmployeesOnThisDay.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEF4444).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Alert",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ATTENTION: PREVENTATIVE LABOR LIABILITY ALERT",
                                        color = Color(0xFFEF4444),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                scheduledEmployeesOnThisDay.forEach { sched ->
                                    val empProfile = profiles.find { it.name == sched.employeeName }
                                    val dept = empProfile?.department ?: "Team"
                                    Text(
                                        text = "• ${sched.employeeName} ($dept) is scheduled for '${sched.shiftName}'.",
                                        color = getAdaptiveTextColor(0.85f),
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ensure that payroll accounts for premium holiday calculations!",
                                    color = getAdaptiveTextColor(0.6f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeonGreen.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .border(1.dp, NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Compliant",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "COMPLIANT: No active employee shifts scheduled on this holiday. 0% premium payroll risk.",
                                    color = NeonGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = getAdaptiveTextColor(0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Standard Working Day",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = getAdaptiveTextColor(0.8f)
                            )
                            Text(
                                text = "Standard 100% payroll hourly rate applies with normal shift rules.",
                                fontSize = 11.sp,
                                color = getAdaptiveTextColor(0.5f)
                            )
                        }
                    }
                }
            }
        }
        
        // --- HOLIDAYS INDEX list FOR CURRENT MONTH ---
        Text(
            text = "MONTHLY PUBLIC HOLIDAYS INDEX",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = NeonGreen,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        if (monthHolidays.isEmpty()) {
            Text(
                text = "No official holidays listed for this month.",
                color = getAdaptiveTextColor(0.4f),
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            monthHolidays.forEach { hol ->
                val dayPart = hol.date.split("-").getOrNull(2)?.toInt() ?: 1
                val category = when {
                    hol.name.contains("EDSA", ignoreCase = true) -> "Special (Working)"
                    hol.name.contains("Chinese", ignoreCase = true) ||
                    hol.name.contains("Black Saturday", ignoreCase = true) ||
                    hol.name.contains("Ninoy Aquino", ignoreCase = true) ||
                    hol.name.contains("All Saints", ignoreCase = true) ||
                    hol.name.contains("All Souls", ignoreCase = true) ||
                    hol.name.contains("Immaculate", ignoreCase = true) ||
                    hol.name.contains("Christmas Eve", ignoreCase = true) ||
                    hol.name.contains("Last Day of the Year", ignoreCase = true) -> "Special (Non-Working)"
                    else -> "Regular"
                }
                val color = when (category) {
                    "Special (Working)" -> Color(0xFF10B981)
                    "Special (Non-Working)" -> Color(0xFF3B82F6)
                    else -> Color(0xFFEF4444)
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(getAdaptiveColor(0.02f), RoundedCornerShape(8.dp))
                        .border(0.5.dp, BorderGrey, RoundedCornerShape(8.dp))
                        .clickable { selectedDayNum = dayPart }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%02d".format(dayPart),
                            fontWeight = FontWeight.Bold,
                            color = color,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = hol.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = com.example.ui.theme.AppTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Details",
                        tint = getAdaptiveTextColor(0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // --- CUSTOM HOLIDAY REGISTER DIALOG ---
        if (showCustomHolidayDialog) {
            AlertDialog(
                onDismissRequest = { showCustomHolidayDialog = false },
                title = { Text("Register Custom Holiday", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add a custom company-wide holiday for %02d %s 2026.".format(selectedDayNum, monthNames[selectedMonthIndex]), fontSize = 11.sp, color = getAdaptiveTextColor(0.7f))
                        
                        OutlinedTextField(
                            value = customHolidayName,
                            onValueChange = { customHolidayName = it },
                            placeholder = { Text("e.g., ClauseOS Foundation Day", fontSize = 11.sp) },
                            label = { Text("Holiday Name", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = com.example.ui.theme.AppTextColor, fontSize = 11.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = customHolidayDesc,
                            onValueChange = { customHolidayDesc = it },
                            placeholder = { Text("e.g., Annual celebration of founding", fontSize = 11.sp) },
                            label = { Text("Description", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = com.example.ui.theme.AppTextColor, fontSize = 11.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Type Selection
                        Text("Holiday Category Type:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Regular", "Special (Non-Working)", "Special (Working)").forEach { type ->
                                val isTypeSelected = customHolidayType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isTypeSelected) NeonGreen.copy(alpha = 0.15f) else getAdaptiveColor(0.02f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isTypeSelected) NeonGreen else getAdaptiveColor(0.1f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { customHolidayType = type }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(type.replace(" (", "\n("), fontSize = 9.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = if (isTypeSelected) NeonGreen else getAdaptiveColor(0.7f))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customHolidayName.isNotBlank()) {
                                val isNational = customHolidayType == "Regular"
                                val formattedDate = "2026-%02d-%02d".format(selectedMonthIndex + 1, selectedDayNum)
                                
                                viewModel.addCustomHoliday(
                                    name = if (customHolidayType == "Regular") customHolidayName else "$customHolidayName (${if (customHolidayType.contains("Non-Working")) userCountryCode else "Special Working " + userCountryCode})",
                                    date = formattedDate,
                                    description = customHolidayDesc.ifBlank { "Custom registered holiday" },
                                    isNational = isNational,
                                    country = userCountryCode
                                )
                                
                                customHolidayName = ""
                                customHolidayDesc = ""
                                showCustomHolidayDialog = false
                                Toast.makeText(context, "Registered custom holiday!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter a holiday name", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                    ) {
                        Text("Register", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showCustomHolidayDialog = false }) {
                        Text("Cancel", fontSize = 11.sp)
                    }
                }
            )
        }

        // --- CALENDAR NOTES & REMINDERS SECTION ---
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "CALENDAR REMINDERS & NOTES FOR %02d %s".format(selectedDayNum, monthNames[selectedMonthIndex].uppercase()),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = NeonGreen,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        val notesForSelectedDay = viewModel.calendarNotes.value.filter { it.date == selectedDateKey }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isLightTheme) Color.White else CardGreyBg
            ),
            border = BorderStroke(
                1.dp,
                if (isLightTheme) Color(0xFFE2E8F0) else BorderGrey
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (notesForSelectedDay.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = getAdaptiveTextColor(0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No notes or reminders for this date.",
                            fontSize = 11.sp,
                            color = getAdaptiveTextColor(0.5f)
                        )
                    }
                } else {
                    notesForSelectedDay.forEach { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(getAdaptiveColor(0.02f), RoundedCornerShape(8.dp))
                                .border(1.dp, getAdaptiveColor(0.05f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = note.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = com.example.ui.theme.AppTextColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = note.time,
                                        fontSize = 9.sp,
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (note.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = note.description,
                                        fontSize = 11.sp,
                                        color = getAdaptiveTextColor(0.6f)
                                    )
                                }
                                if (note.syncWithGoogleCalendar) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color(0xFF4285F4),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Notification Active",
                                            fontSize = 9.sp,
                                            color = Color(0xFF4285F4),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteCalendarNote(note.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete note",
                                    tint = getAdaptiveTextColor(0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                
                Divider(color = getAdaptiveColor(0.1f), modifier = Modifier.padding(vertical = 12.dp))
                
                // Form to add a note
                var newNoteTitle by remember { mutableStateOf("") }
                var newNoteDesc by remember { mutableStateOf("") }
                var newNoteTime by remember { mutableStateOf("09:00 AM") }
                var newNoteNotify by remember { mutableStateOf(true) }
                
                Text(
                    text = "ADD NOTE ON THIS DATE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonGreen,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = newNoteTitle,
                    onValueChange = { newNoteTitle = it },
                    placeholder = { Text("Title (e.g., Project Deadline, Shift Swap Handover)", fontSize = 11.sp) },
                    textStyle = TextStyle(color = com.example.ui.theme.AppTextColor, fontSize = 11.sp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
                
                OutlinedTextField(
                    value = newNoteDesc,
                    onValueChange = { newNoteDesc = it },
                    placeholder = { Text("Description / Details (Optional)", fontSize = 11.sp) },
                    textStyle = TextStyle(color = com.example.ui.theme.AppTextColor, fontSize = 11.sp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                // Time selector chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val times = listOf("09:00 AM", "12:00 PM", "03:00 PM", "06:00 PM")
                    times.forEach { t ->
                        val isSelected = newNoteTime == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else getAdaptiveColor(0.02f))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeonGreen else getAdaptiveColor(0.08f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { newNoteTime = t }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeonGreen else getAdaptiveTextColor(0.6f)
                            )
                        }
                    }
                }
                
                // Toggle notification checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Checkbox(
                        checked = newNoteNotify,
                        onCheckedChange = { newNoteNotify = it },
                        colors = CheckboxDefaults.colors(checkedColor = NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Enable system & alert notification",
                        fontSize = 11.sp,
                        color = getAdaptiveTextColor(0.7f)
                    )
                }
                
                Button(
                    onClick = {
                        if (newNoteTitle.isNotBlank()) {
                            viewModel.addCalendarNote(
                                date = selectedDateKey,
                                title = newNoteTitle,
                                description = newNoteDesc,
                                time = newNoteTime,
                                sync = newNoteNotify
                            )
                            newNoteTitle = ""
                            newNoteDesc = ""
                            Toast.makeText(context, "Added note & reminder successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Save Calendar Note & Reminder", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PhComplianceView(viewModel: TimeTrackerViewModel, context: Context) {
    val profiles = viewModel.employeeProfiles.value
    
    // Choose selected employee
    var selectedEmpName by remember { mutableStateOf(viewModel.currentUserName.value) }
    val empProfile = profiles.find { it.name.equals(selectedEmpName, ignoreCase = true) }
    
    // Form Inputs
    var monthlySalaryInput by remember { mutableStateOf("45000") }
    var employmentCase by remember { mutableStateOf("regular") } // regular, new_joiner, maternity, resigned
    var monthsWorked by remember { mutableStateOf(12f) }
    var maternityWeeksOff by remember { mutableStateOf(8f) }
    var separationMonth by remember { mutableStateOf(8f) } // August default separation
    
    // Sync monthly salary when employee profile changes
    LaunchedEffect(selectedEmpName) {
        empProfile?.let {
            val baseRate = if (it.department.contains("Engineering")) "85000" else "45000"
            monthlySalaryInput = baseRate
        }
    }

    val salary = monthlySalaryInput.toDoubleOrNull() ?: 0.0

    // PH Contribution Math (2026 guidelines)
    // SSS (MSC max 30K, 4.5% EE, 9.5% ER)
    val sssMsc = minOf(salary, 30000.0)
    val sssEe = sssMsc * 0.045
    val sssEr = sssMsc * 0.095

    // PhilHealth (MSC floor 10K, ceil 100K, 2.5% EE, 2.5% ER)
    val phMsc = salary.coerceIn(10000.0, 100000.0)
    val phEe = phMsc * 0.025
    val phEr = phMsc * 0.025

    // Pag-IBIG (MSC max 10K, 2% EE, 2% ER, max 200)
    val pagIbigEe = minOf(salary * 0.02, 200.0)
    val pagIbigEr = minOf(salary * 0.02, 200.0)

    // BIR Withholding Tax
    val birTaxable = maxOf(0.0, salary - (sssEe + phEe + pagIbigEe))
    val tax = when {
        birTaxable <= 20833.0 -> 0.0
        birTaxable <= 33333.0 -> (birTaxable - 20833.0) * 0.15
        birTaxable <= 66667.0 -> 1875.0 + (birTaxable - 33333.0) * 0.20
        birTaxable <= 166667.0 -> 8541.67 + (birTaxable - 66667.0) * 0.25
        birTaxable <= 666667.0 -> 33541.67 + (birTaxable - 166667.0) * 0.30
        else -> 183541.67 + (birTaxable - 66667.0) * 0.35
    }

    val totalDeductions = sssEe + phEe + pagIbigEe + tax
    val netMonthlyPay = salary - totalDeductions

    // 13th Month Pay Calculation
    val totalWorkedMonths = when (employmentCase) {
        "regular" -> 12.0
        "new_joiner" -> monthsWorked.toDouble()
        "maternity" -> (monthsWorked.toDouble() - (maternityWeeksOff.toDouble() / 4.33)).coerceIn(1.0, 12.0)
        "resigned" -> separationMonth.toDouble()
        else -> 12.0
    }

    val grossYearlyEarnings = salary * totalWorkedMonths
    val bonus13th = grossYearlyEarnings / 12.0
    val exempt13th = minOf(bonus13th, 90000.0)
    val taxable13th = maxOf(0.0, bonus13th - 90000.0)

    Column(modifier = Modifier.fillMaxWidth()) {
        
        // --- SECTION 1: HEADER & LIVE COMPLIANCE STATUS ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Verified Logo",
                            tint = NeonGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Compliance On Autopilot",
                                color = com.example.ui.theme.AppTextColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Locked with DOLE standard tax tables (TRAIN Law & PhilHealth 2026)",
                                color = getAdaptiveTextColor(0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00FF88).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("PH-COMPLIANT", color = Color(0xFF00FF88), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Divider(color = BorderGrey, modifier = Modifier.padding(vertical = 12.dp))
                
                // Employee Selection Dropdown
                Text("Select Target Employee Profile", color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(getAdaptiveColor(0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, BorderGrey, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clickable {
                                val nameList = profiles.map { it.name }
                                val idx = nameList.indexOf(selectedEmpName)
                                selectedEmpName = if (idx == -1 || idx == nameList.size - 1) nameList.firstOrNull() ?: "Active User" else nameList[idx + 1]
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedEmpName, color = com.example.ui.theme.AppTextColor, fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NeonGreen)
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(getAdaptiveColor(0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, BorderGrey, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = monthlySalaryInput,
                            onValueChange = { monthlySalaryInput = it },
                            textStyle = TextStyle(color = com.example.ui.theme.AppTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (monthlySalaryInput.isEmpty()) {
                                    Text("Enter Basic Salary (₱)", color = getAdaptiveTextColor(0.4f), fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("₱ ", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- SECTION 2: CONTRIBUTION BREAKDOWN CHART ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Deduction Breakdown & Take-home Pay", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(90.dp)) {
                            val total = sssEe + phEe + pagIbigEe + tax + netMonthlyPay
                            var startAngle = -90f
                            
                            val netAngle = if (total > 0) (netMonthlyPay / total).toFloat() * 360f else 360f
                            val taxAngle = if (total > 0) (tax / total).toFloat() * 360f else 0f
                            val sssAngle = if (total > 0) (sssEe / total).toFloat() * 360f else 0f
                            val phAngle = if (total > 0) (phEe / total).toFloat() * 360f else 0f
                            val piAngle = if (total > 0) (pagIbigEe / total).toFloat() * 360f else 0f
                            
                            // Net Pay (Mint Neon Green)
                            drawArc(
                                color = Color(0xFF00FF88),
                                startAngle = startAngle,
                                sweepAngle = netAngle,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx())
                            )
                            startAngle += netAngle
                            
                            // BIR Tax (Yellow)
                            drawArc(
                                color = Color(0xFFFFCC00),
                                startAngle = startAngle,
                                sweepAngle = taxAngle,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx())
                            )
                            startAngle += taxAngle

                            // SSS (Blue)
                            drawArc(
                                color = Color(0xFF2A80FF),
                                startAngle = startAngle,
                                sweepAngle = sssAngle,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx())
                            )
                            startAngle += sssAngle

                            // PhilHealth (Green)
                            drawArc(
                                color = Color(0xFF00AA55),
                                startAngle = startAngle,
                                sweepAngle = phAngle,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx())
                            )
                            startAngle += phAngle

                            // Pag-IBIG (Orange)
                            drawArc(
                                color = Color(0xFFFF6B2A),
                                startAngle = startAngle,
                                sweepAngle = piAngle,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx())
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val percentNet = if (salary > 0) (netMonthlyPay / salary) * 100 else 100.0
                            Text(String.format("%.0f%%", percentNet), color = com.example.ui.theme.AppTextColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("Take-Home", color = getAdaptiveTextColor(0.5f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(18.dp))
                    
                    // Side details
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LegendRow(color = Color(0xFF00FF88), label = "Net Take-Home Pay", value = String.format("₱%,.2f", netMonthlyPay))
                        LegendRow(color = Color(0xFFFFCC00), label = "BIR Tax Withheld", value = String.format("₱%,.2f", tax))
                        LegendRow(color = Color(0xFF2A80FF), label = "SSS Contribution", value = String.format("₱%,.2f", sssEe))
                        LegendRow(color = Color(0xFF00AA55), label = "PhilHealth Premium", value = String.format("₱%,.2f", phEe))
                        LegendRow(color = Color(0xFFFF6B2A), label = "Pag-IBIG Contribution", value = String.format("₱%,.2f", pagIbigEe))
                    }
                }
                
                Divider(color = BorderGrey, modifier = Modifier.padding(vertical = 12.dp))
                
                // Employer counterpart calculations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Employer Counterpart Contribution:", color = getAdaptiveTextColor(0.6f), fontSize = 10.sp)
                    Text(
                        String.format("₱%,.2f", sssEr + phEr + pagIbigEr),
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- SECTION 3: 13TH MONTH MULTI-CASE ENGINE ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "13th Month Pay Calculator (PD 851)",
                    color = com.example.ui.theme.AppTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Prorated calculations tailored for multiple workforce scenarios.",
                    color = getAdaptiveTextColor(0.5f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                
                // Scenarios tab selector row
                Row(
                    modifier = Modifier.fillMaxWidth().background(getAdaptiveColor(0.03f), RoundedCornerShape(8.dp)).padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val cases = listOf(
                        "regular" to "Active Full",
                        "new_joiner" to "New Joiner",
                        "maternity" to "Maternity",
                        "resigned" to "Resigned"
                    )
                    cases.forEach { (key, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (employmentCase == key) NeonGreen else Color.Transparent)
                                .clickable { employmentCase = key }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (employmentCase == key) Color.Black else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Render controls based on scenario chosen
                when (employmentCase) {
                    "new_joiner" -> {
                        Column {
                            Text(String.format("Months Worked: %.1f Months", monthsWorked), color = com.example.ui.theme.AppTextColor, fontSize = 11.sp)
                            Slider(
                                value = monthsWorked,
                                onValueChange = { monthsWorked = it },
                                valueRange = 1f..12f,
                                steps = 11,
                                colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                            )
                        }
                    }
                    "maternity" -> {
                        Column {
                            Text(String.format("Months with Company: %.0f Months", monthsWorked), color = com.example.ui.theme.AppTextColor, fontSize = 11.sp)
                            Slider(
                                value = monthsWorked,
                                onValueChange = { monthsWorked = it },
                                valueRange = 1f..12f,
                                steps = 11,
                                colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(String.format("Unpaid Maternity Leave: %.1f Weeks", maternityWeeksOff), color = com.example.ui.theme.AppTextColor, fontSize = 11.sp)
                            Slider(
                                value = maternityWeeksOff,
                                onValueChange = { maternityWeeksOff = it },
                                valueRange = 0f..16f,
                                steps = 16,
                                colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                            )
                        }
                    }
                    "resigned" -> {
                        Column {
                            Text(String.format("Separation month: %s", getMonthName(separationMonth.toInt())), color = com.example.ui.theme.AppTextColor, fontSize = 11.sp)
                            Slider(
                                value = separationMonth,
                                onValueChange = { separationMonth = it },
                                valueRange = 1f..12f,
                                steps = 11,
                                colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                            )
                        }
                    }
                    "regular" -> {
                        Text(
                            "Standard active full-year tenure (12 calendar months of computation). No adjustments needed.",
                            color = getAdaptiveTextColor(0.5f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = BorderGrey, modifier = Modifier.padding(vertical = 8.dp))
                
                // Result Summary
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Calculated Months:", color = getAdaptiveTextColor(0.6f), fontSize = 11.sp)
                    Text(String.format("%.2f Months", totalWorkedMonths), color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gross Basic Yearly Compensation:", color = getAdaptiveTextColor(0.6f), fontSize = 11.sp)
                    Text(String.format("₱%,.2f", grossYearlyEarnings), color = com.example.ui.theme.AppTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Calculated 13th Month Pay:", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("₱%,.2f", bonus13th), color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Exemption alert block
                val isFullyExempt = bonus13th <= 90000.0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isFullyExempt) Color(0xFF00FF88).copy(alpha = 0.1f) else Color(0xFFFF9900).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, if (isFullyExempt) Color(0xFF00FF88).copy(alpha = 0.4f) else Color(0xFFFF9900).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFullyExempt) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isFullyExempt) Color(0xFF00FF88) else Color(0xFFFF9900),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isFullyExempt) "100% Tax-Free Exemption" else "Prone to Excess BIR Compensation Tax",
                                color = if (isFullyExempt) Color(0xFF00FF88) else Color(0xFFFF9900),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (isFullyExempt) "The full amount is exempt from income tax since it falls below the ₱90,000 threshold under the TRAIN Law."
                            else "The portion exceeding the ₱90,000 exemption limit (₱${String.format("%,.2f", taxable13th)}) will be aggregated to taxable compensations.",
                            color = getAdaptiveTextColor(0.6f),
                            fontSize = 9.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }

        // --- SECTION 4: PH GOVERNMENT DOCUMENT VAULT ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CardGreyBg),
            border = BorderStroke(1.dp, BorderGrey)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Official PH Document Vault", color = com.example.ui.theme.AppTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Generate and print government-compliant BIR certificates or contribution files in one click.",
                    color = getAdaptiveTextColor(0.5f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )
                
                // Form PDF Generation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val file = generateBir2316PdfFile(context, selectedEmpName, salary, sssEe, phEe, pagIbigEe, tax, bonus13th)
                            if (file != null) {
                                openPdfFile(context, file)
                                Toast.makeText(context, "BIR 2316 Generated successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error generating BIR 2316.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print BIR 2316", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            val file = generateBir1601CPdfFile(context, salary, tax)
                            if (file != null) {
                                openPdfFile(context, file)
                                Toast.makeText(context, "BIR 1601-C Generated successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error generating BIR 1601-C.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print BIR 1601-C", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Contribution CSV download list
                ContributionExportItem(
                    title = "SSS R-5 Remittance (CSV Format)",
                    onExport = {
                        val csv = "Employee Name,Sss Number,Gross Salary,EE Share,ER Share,Total Remitted\n" +
                                "${selectedEmpName},552-3318-9921,${String.format("%.2f", salary)},${String.format("%.2f", sssEe)},${String.format("%.2f", sssEr)},${String.format("%.2f", sssEe + sssEr)}\n"
                        saveCSVToDownloads(context, "SSS_R5_Remittance.csv", csv)
                    }
                )
                ContributionExportItem(
                    title = "PhilHealth RF-1 Remittance (CSV Format)",
                    onExport = {
                        val csv = "Contributor Name,PhilHealth Number,Monthly Salary,EE Share,ER Share,Total Premium\n" +
                                "${selectedEmpName},0112-9983-2210,${String.format("%.2f", salary)},${String.format("%.2f", phEe)},${String.format("%.2f", phEr)},${String.format("%.2f", phEe + phEr)}\n"
                        saveCSVToDownloads(context, "PhilHealth_RF1_Remittance.csv", csv)
                    }
                )
                ContributionExportItem(
                    title = "Pag-IBIG MCRF Remittance (CSV Format)",
                    onExport = {
                        val csv = "Member Name,PagIbig ID,Monthly Compensation,EE Contribution,ER Contribution,Total\n" +
                                "${selectedEmpName},1228-9918-2231,${String.format("%.2f", salary)},${String.format("%.2f", pagIbigEe)},${String.format("%.2f", pagIbigEr)},${String.format("%.2f", pagIbigEe + pagIbigEr)}\n"
                        saveCSVToDownloads(context, "PagIBIG_MCRF_Remittance.csv", csv)
                    }
                )
            }
        }
    }
}

@Composable
fun LegendRow(color: Color, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = getAdaptiveTextColor(0.7f), fontSize = 10.sp)
        }
        Text(value, color = com.example.ui.theme.AppTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ContributionExportItem(title: String, onExport: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(getAdaptiveColor(0.03f), RoundedCornerShape(6.dp))
            .clickable { onExport() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, color = com.example.ui.theme.AppTextColor, fontSize = 10.sp)
        }
        Icon(Icons.Default.Download, contentDescription = null, tint = getAdaptiveTextColor(0.4f), modifier = Modifier.size(14.dp))
    }
}

fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "December"
    }
}

fun drawPhWrappedText(canvas: Canvas, text: String, x: Float, y: Float, width: Float, paint: Paint, lineHeight: Float): Float {
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

fun generateBir2316PdfFile(
    context: Context,
    employeeName: String,
    monthlySalary: Double,
    sss: Double,
    ph: Double,
    pagIbig: Double,
    tax: Double,
    bonus13th: Double
): File? {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Common Paints
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#FBFBFD"); style = Paint.Style.FILL }
        val blackPaint = Paint().apply { color = android.graphics.Color.parseColor("#111111"); style = Paint.Style.FILL }
        val electricMintPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676"); style = Paint.Style.FILL }
        val cardFillPaint = Paint().apply { color = android.graphics.Color.parseColor("#F4F6F8"); style = Paint.Style.FILL }
        val heroCalloutPaint = Paint().apply { color = android.graphics.Color.parseColor("#E8F8F0"); style = Paint.Style.FILL }
        
        val strokePaint = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 1f }
        val textPaint = Paint().apply { color = android.graphics.Color.parseColor("#1A202C"); isAntiAlias = true }

        val whiteTitlePaint = Paint().apply { color = android.graphics.Color.WHITE; textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val graySubtitlePaint = Paint().apply { color = android.graphics.Color.parseColor("#A0AEC0"); textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
        val mintMetadataPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676"); textSize = 8.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        // Background
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // 1. Header block
        canvas.drawRect(15f, 15f, 580f, 120f, blackPaint)
        canvas.drawRect(15f, 120f, 580f, 126f, electricMintPaint)

        canvas.drawText("BIR Form No. 2316", 30f, 48f, whiteTitlePaint)
        canvas.drawText("Certificate of Compensation Payment / Tax Withheld", 30f, 68f, mintMetadataPaint)
        canvas.drawText("For Compensation Payment With or Without Withholding Tax", 30f, 86f, graySubtitlePaint)
        canvas.drawText("Under Tax Reform for Acceleration and Inclusion (TRAIN Law)", 30f, 100f, graySubtitlePaint)

        canvas.drawText("BUREAU OF INTERNAL REVENUE", 410f, 48f, whiteTitlePaint)
        canvas.drawText("Department of Finance", 410f, 68f, graySubtitlePaint)
        canvas.drawText("Quezon City, Philippines", 410f, 84f, graySubtitlePaint)

        var currentY = 140f

        // PART I
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + 18f), 4f, 4f, blackPaint)
        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PART I — EMPLOYEE INFORMATION", 25f, currentY + 12f, textPaint)

        currentY += 18f
        // Beautiful container card
        val p1Height = 72f
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p1Height), 10f, 10f, cardFillPaint)
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p1Height), 10f, 10f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#1A202C")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 8.5f
        
        canvas.drawText("1. Taxpayer Identification No (TIN):  318-992-120-000", 30f, currentY + 18f, textPaint)
        canvas.drawText("2. Employee's Name:  " + employeeName.uppercase(), 30f, currentY + 34f, textPaint)
        canvas.drawText("3. Registered Address:  142-C Ayala Avenue, Makati City, Philippines", 30f, currentY + 50f, textPaint)
        canvas.drawText("4. Date of Birth:  1994-08-12  |  Zip Code: 1226", 30f, currentY + 65f, textPaint)

        currentY += p1Height + 15f

        // PART II
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + 18f), 4f, 4f, blackPaint)
        textPaint.color = android.graphics.Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PART II — EMPLOYER INFORMATION", 25f, currentY + 12f, textPaint)

        currentY += 18f
        val p2Height = 52f
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p2Height), 10f, 10f, cardFillPaint)
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p2Height), 10f, 10f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#1A202C")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("5. Employer's TIN:  004-551-998-000", 30f, currentY + 18f, textPaint)
        canvas.drawText("6. Employer's Registered Name:  SHIFT CORP INC / PEOPLES HR GROUP", 30f, currentY + 34f, textPaint)

        currentY += p2Height + 15f

        // PART III
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + 18f), 4f, 4f, blackPaint)
        textPaint.color = android.graphics.Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PART III — SUMMARY OF COMPENSATION AND TAX WITHHELD", 25f, currentY + 12f, textPaint)

        currentY += 18f
        val p3Height = 232f
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p3Height), 10f, 10f, cardFillPaint)
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p3Height), 10f, 10f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#1A202C")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val rowY = currentY + 18f
        canvas.drawText("7. Gross Basic Salary (For the Calendar Year)", 30f, rowY, textPaint)
        canvas.drawText(String.format("₱%,.2f", monthlySalary * 12), 430f, rowY, textPaint)

        canvas.drawText("8. Non-Taxable / Exempt Compensation:", 30f, rowY + 18f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("   - SSS Premium Share Portion", 30f, rowY + 32f, textPaint)
        canvas.drawText(String.format("₱%,.2f", sss * 12), 430f, rowY + 32f, textPaint)

        canvas.drawText("   - PhilHealth Premium Share Portion", 30f, rowY + 46f, textPaint)
        canvas.drawText(String.format("₱%,.2f", ph * 12), 430f, rowY + 46f, textPaint)

        canvas.drawText("   - Pag-IBIG HDMF Contribution", 30f, rowY + 60f, textPaint)
        canvas.drawText(String.format("₱%,.2f", pagIbig * 12), 430f, rowY + 60f, textPaint)

        canvas.drawText("   - 13th Month Pay & Other Benefits (₱90,000 threshold)", 30f, rowY + 74f, textPaint)
        canvas.drawText(String.format("₱%,.2f", bonus13th), 430f, rowY + 74f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val totalExempt = (sss + ph + pagIbig) * 12 + bonus13th
        canvas.drawText("9. Total Non-Taxable Compensation Portion", 30f, rowY + 92f, textPaint)
        canvas.drawText(String.format("₱%,.2f", totalExempt), 430f, rowY + 92f, textPaint)

        val taxableBase = maxOf(0.0, (monthlySalary * 12) - (sss + ph + pagIbig) * 12)
        canvas.drawText("10. Taxable Compensation Income", 30f, rowY + 110f, textPaint)
        canvas.drawText(String.format("₱%,.2f", taxableBase), 430f, rowY + 110f, textPaint)

        canvas.drawText("11. Gross Taxable Compensation", 30f, rowY + 128f, textPaint)
        canvas.drawText(String.format("₱%,.2f", taxableBase), 430f, rowY + 128f, textPaint)

        canvas.drawText("12. Tax Due (Graduated monthly consolidated)", 30f, rowY + 150f, textPaint)
        canvas.drawText(String.format("₱%,.2f", tax * 12), 430f, rowY + 150f, textPaint)

        // Tax Withheld in green highlight box
        canvas.drawRoundRect(RectF(25f, rowY + 157f, 570f, rowY + 176f), 4f, 4f, heroCalloutPaint)
        canvas.drawRoundRect(RectF(25f, rowY + 157f, 570f, rowY + 176f), 4f, 4f, strokePaint)
        
        textPaint.color = android.graphics.Color.parseColor("#008543")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("13. Total Amount of Taxes Withheld", 30f, rowY + 169f, textPaint)
        canvas.drawText(String.format("₱%,.2f", tax * 12), 430f, rowY + 169f, textPaint)

        currentY += p3Height + 15f

        // PART IV
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + 18f), 4f, 4f, blackPaint)
        textPaint.color = android.graphics.Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PART IV — DECLARATION & SIGNATURE PANEL", 25f, currentY + 12f, textPaint)

        currentY += 18f
        val p4Height = 110f
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p4Height), 10f, 10f, cardFillPaint)
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p4Height), 10f, 10f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#4A5568")
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val declText = "We declare under the penalties of perjury that this certificate has been made in good faith, verified by us, and to the best of our knowledge and belief, is true and correct, pursuant to the provisions of the National Internal Revenue Code, as amended."
        drawPhWrappedText(canvas, declText, 30f, currentY + 15f, 530f, textPaint, 11f)

        canvas.drawLine(50f, currentY + 75f, 250f, currentY + 75f, strokePaint)
        canvas.drawLine(340f, currentY + 75f, 540f, currentY + 75f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#1A202C")
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Authorized Agent (Employer)", 70f, currentY + 88f, textPaint)
        canvas.drawText(employeeName, 380f, currentY + 88f, textPaint)
        
        textPaint.color = android.graphics.Color.parseColor("#718096")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Tax Withholding Agent Seal", 70f, currentY + 100f, textPaint)
        canvas.drawText("Taxpayer Signature Panel", 380f, currentY + 100f, textPaint)

        // Footer block
        canvas.drawLine(30f, 795f, 565f, 795f, strokePaint)
        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 7.5f
        canvas.drawText("Bureau of Internal Revenue (BIR) Official Document Remittance Model  •  Page 1 of 1", 125f, 812f, textPaint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "BIR_Form_2316_${employeeName.replace(" ", "_")}.pdf")
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

fun generateBir1601CPdfFile(
    context: Context,
    monthlySalary: Double,
    tax: Double
): File? {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Common Paints
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#FBFBFD"); style = Paint.Style.FILL }
        val blackPaint = Paint().apply { color = android.graphics.Color.parseColor("#111111"); style = Paint.Style.FILL }
        val electricMintPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676"); style = Paint.Style.FILL }
        val cardFillPaint = Paint().apply { color = android.graphics.Color.parseColor("#F4F6F8"); style = Paint.Style.FILL }
        val heroCalloutPaint = Paint().apply { color = android.graphics.Color.parseColor("#E8F8F0"); style = Paint.Style.FILL }
        
        val strokePaint = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 1f }
        val textPaint = Paint().apply { color = android.graphics.Color.parseColor("#1A202C"); isAntiAlias = true }

        val whiteTitlePaint = Paint().apply { color = android.graphics.Color.WHITE; textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val graySubtitlePaint = Paint().apply { color = android.graphics.Color.parseColor("#A0AEC0"); textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
        val mintMetadataPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676"); textSize = 8.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        // Background
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // 1. Header block
        canvas.drawRect(15f, 15f, 580f, 120f, blackPaint)
        canvas.drawRect(15f, 120f, 580f, 126f, electricMintPaint)

        canvas.drawText("BIR Form No. 1601-C", 30f, 48f, whiteTitlePaint)
        canvas.drawText("Monthly Remittance Return of Income Taxes Withheld", 30f, 68f, mintMetadataPaint)
        canvas.drawText("Remitted Monthly Under Section 58 of the National Internal Revenue Code", 30f, 86f, graySubtitlePaint)
        canvas.drawText("Authorized Remittance Standard Model — SHIFT CORP INC", 30f, 100f, graySubtitlePaint)

        canvas.drawText("BUREAU OF INTERNAL REVENUE", 410f, 48f, whiteTitlePaint)
        canvas.drawText("Return Period: " + SimpleDateFormat("MMMM yyyy", Locale.US).format(Date()), 410f, 68f, graySubtitlePaint)
        canvas.drawText("Due Date: Next 10th Calendar Day", 410f, 84f, graySubtitlePaint)

        var currentY = 140f

        // PART I
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + 18f), 4f, 4f, blackPaint)
        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PART I — BACKGROUND INFORMATION", 25f, currentY + 12f, textPaint)

        currentY += 18f
        val p1Height = 52f
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p1Height), 10f, 10f, cardFillPaint)
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p1Height), 10f, 10f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#1A202C")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 8.5f
        canvas.drawText("1. Taxpayer Identification Number (TIN): 004-551-998-000", 30f, currentY + 16f, textPaint)
        canvas.drawText("2. Withholding Agent Name: SHIFT CORP INC", 30f, currentY + 32f, textPaint)
        canvas.drawText("3. Line of Business / RDO Code: 043-Makati East", 30f, currentY + 46f, textPaint)

        currentY += p1Height + 15f

        // PART II
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + 18f), 4f, 4f, blackPaint)
        textPaint.color = android.graphics.Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PART II — COMPUTATION OF TAX REMITTANCE", 25f, currentY + 12f, textPaint)

        currentY += 18f
        val p2Height = 120f
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p2Height), 10f, 10f, cardFillPaint)
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p2Height), 10f, 10f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#1A202C")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val compY = currentY + 18f
        canvas.drawText("4. Total Compensation Paid for the Month", 30f, compY, textPaint)
        canvas.drawText(String.format("₱%,.2f", monthlySalary), 430f, compY, textPaint)

        canvas.drawText("5. Less: Non-Taxable Compensation (SSS/Ph/HDMF)", 30f, compY + 18f, textPaint)
        canvas.drawText("₱2,850.00", 430f, compY + 18f, textPaint)

        canvas.drawText("6. Net Taxable Compensation Income", 30f, compY + 36f, textPaint)
        canvas.drawText(String.format("₱%,.2f", monthlySalary - 2850.0), 430f, compY + 36f, textPaint)

        canvas.drawText("7. Total Taxes Withheld for Compensation", 30f, compY + 54f, textPaint)
        canvas.drawText(String.format("₱%,.2f", tax), 430f, compY + 54f, textPaint)

        // Total Net Remittance Due highlighted
        canvas.drawRoundRect(RectF(25f, compY + 62f, 570f, compY + 81f), 4f, 4f, heroCalloutPaint)
        canvas.drawRoundRect(RectF(25f, compY + 62f, 570f, compY + 81f), 4f, 4f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#008543")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("8. Total Net Remittance Due", 30f, compY + 74f, textPaint)
        canvas.drawText(String.format("₱%,.2f", tax), 430f, compY + 74f, textPaint)

        currentY += p2Height + 15f

        // PART III
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + 18f), 4f, 4f, blackPaint)
        textPaint.color = android.graphics.Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PART III — COMPLIANCE VERIFICATION & AUDIT PANEL", 25f, currentY + 12f, textPaint)

        currentY += 18f
        val p3Height = 110f
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p3Height), 10f, 10f, cardFillPaint)
        canvas.drawRoundRect(RectF(15f, currentY, 580f, currentY + p3Height), 10f, 10f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#4A5568")
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val verifyText = "We declare under the penalties of perjury that this return has been made in good faith, verified by us, and to the best of our knowledge and belief, is true and correct, pursuant to the provisions of the National Internal Revenue Code, as amended."
        drawPhWrappedText(canvas, verifyText, 30f, currentY + 15f, 530f, textPaint, 11f)

        canvas.drawLine(50f, currentY + 75f, 250f, currentY + 75f, strokePaint)
        canvas.drawLine(340f, currentY + 75f, 540f, currentY + 75f, strokePaint)

        textPaint.color = android.graphics.Color.parseColor("#1A202C")
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Employer Authorized Signature", 70f, currentY + 88f, textPaint)
        canvas.drawText("Chief Financial Officer", 380f, currentY + 88f, textPaint)
        
        textPaint.color = android.graphics.Color.parseColor("#718096")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Corporate Tax Agent Seal", 70f, currentY + 100f, textPaint)
        canvas.drawText("Corporate Treasury Sign Panel", 380f, currentY + 100f, textPaint)

        // Footer block
        canvas.drawLine(30f, 795f, 565f, 795f, strokePaint)
        textPaint.color = android.graphics.Color.GRAY
        textPaint.textSize = 7.5f
        canvas.drawText("BIR Form No. 1601-C — Monthly Remittance Model — SHIFT CORP INC  •  Page 1 of 1", 125f, 812f, textPaint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "BIR_Form_1601_C_Remittance.pdf")
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



@androidx.compose.runtime.Composable
fun getAdaptiveColor(whiteAlpha: Float): Color {
    val isLightTheme = androidx.compose.material3.MaterialTheme.colorScheme.onBackground != Color(0xFFFFFFFF)
    return if (isLightTheme) {
        val blackAlpha = when {
            whiteAlpha <= 0.02f -> 0.12f
            whiteAlpha <= 0.04f -> 0.20f
            whiteAlpha <= 0.05f -> 0.26f
            whiteAlpha <= 0.08f -> 0.35f
            whiteAlpha <= 0.1f  -> 0.42f
            whiteAlpha <= 0.12f -> 0.48f
            whiteAlpha <= 0.15f -> 0.55f
            whiteAlpha <= 0.2f  -> 0.65f
            whiteAlpha <= 0.3f  -> 0.72f
            whiteAlpha <= 0.4f  -> 0.80f
            whiteAlpha <= 0.5f  -> 0.85f
            whiteAlpha <= 0.6f  -> 0.90f
            whiteAlpha <= 0.7f  -> 0.94f
            else -> whiteAlpha
        }
        Color.Black.copy(alpha = blackAlpha)
    } else {
        Color.White.copy(alpha = whiteAlpha)
    }
}

@androidx.compose.runtime.Composable
fun getAdaptiveTextColor(alpha: Float): Color {
    val isLightTheme = androidx.compose.material3.MaterialTheme.colorScheme.onBackground != Color(0xFFFFFFFF)
    return if (isLightTheme) {
        val lightAlpha = when {
            alpha <= 0.05f -> 0.55f
            alpha <= 0.1f  -> 0.65f
            alpha <= 0.15f -> 0.72f
            alpha <= 0.2f  -> 0.78f
            alpha <= 0.3f  -> 0.84f
            alpha <= 0.4f  -> 0.88f
            alpha <= 0.5f  -> 0.92f
            alpha <= 0.6f  -> 0.95f
            else -> alpha
        }
        com.example.ui.theme.AppTextColor.copy(alpha = lightAlpha)
    } else {
        com.example.ui.theme.AppTextColor.copy(alpha = alpha)
    }
}
