package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonGreen
import com.example.ui.viewmodel.TimeTrackerViewModel
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Enum for View Mode Toggle
enum class MappingViewMode {
    DRAG_DROP,    // Interactive Canvas Node Graph
    SEARCH_LIST   // Searchable Multi-Select Precision List
}

// Enum for Heatmap & Overlay Sub-modes
enum class MappingHeatmapOverlay {
    ORG_STRUCTURE,      // Standard Reporting Structure
    CASCADE_SHIFTS,     // Batch Shift Drag & Drop Cascading
    ATTENDANCE_HEATMAP, // Live Attendance & Geofence Heatmap
    APPROVAL_CHAINS,    // Multi-tier Approval Workflows
    BURNOUT_OVERTIME,   // Overtime & Burnout Heatmap
    COMPLIANCE_REST     // Rest Period & Compliance Guardrails
}

// Data model for Heatmap Overlay chips
data class HeatmapChipData(
    val type: MappingHeatmapOverlay,
    val label: String,
    val icon: ImageVector
)

// Data Model for Org Nodes
data class OrgNode(
    val id: String,
    val name: String,
    val role: String, // "GENERAL_MANAGER", "SUPERVISOR", "EMPLOYEE"
    val department: String,
    var position: Offset,
    val parentId: String? = null, // Manager/Supervisor ID
    var assignedShift: String = "Day Shift (08:00 - 16:00)",
    val attendanceStatus: String = "CLOCKED_IN_GEOFENCE", // CLOCKED_IN_GEOFENCE, ON_BREAK, LATE_OUTSIDE_GEOFENCE, OFF_DUTY
    val weeklyHours: Double = 38.0,
    val restPeriodHours: Double = 12.0, // Hours of rest since last shift
    val approvalTier: Int = 1 // Approval Chain tier (1 = Employee, 2 = Supervisor, 3 = GM/HR)
) {
    val isManager: Boolean
        get() = role == "GENERAL_MANAGER" || role == "SUPERVISOR"

    val hasRestViolation: Boolean
        get() = restPeriodHours < 11.0
}

/**
 * Seeded sample organization node list with responsive coordinate spacing.
 */
fun getSeededOrgNodes(): List<OrgNode> {
    return listOf(
        // Level 1: General Manager / Director
        OrgNode(
            id = "NODE-GM-01",
            name = "Elena Vance",
            role = "GENERAL_MANAGER",
            department = "Operations & Logistics",
            position = Offset(115f, 20f),
            parentId = null,
            assignedShift = "Executive Schedule",
            attendanceStatus = "CLOCKED_IN_GEOFENCE",
            weeklyHours = 42.0,
            restPeriodHours = 14.0,
            approvalTier = 3
        ),
        // Level 2: Supervisors
        OrgNode(
            id = "NODE-SUP-01",
            name = "Marcus Vance",
            role = "SUPERVISOR",
            department = "Engineering & Tech",
            position = Offset(20f, 150f),
            parentId = "NODE-GM-01",
            assignedShift = "Day Shift (08:00 - 16:00)",
            attendanceStatus = "CLOCKED_IN_GEOFENCE",
            weeklyHours = 44.5,
            restPeriodHours = 12.5,
            approvalTier = 2
        ),
        OrgNode(
            id = "NODE-SUP-02",
            name = "Sophia Martinez",
            role = "SUPERVISOR",
            department = "Human Resources Ops",
            position = Offset(200f, 150f),
            parentId = "NODE-GM-01",
            assignedShift = "Day Shift (08:00 - 16:00)",
            attendanceStatus = "ON_BREAK",
            weeklyHours = 39.0,
            restPeriodHours = 13.0,
            approvalTier = 2
        ),
        // Level 3: Employees under Supervisor 1 (Marcus)
        OrgNode(
            id = "NODE-EMP-01",
            name = "David Chen",
            role = "EMPLOYEE",
            department = "Engineering",
            position = Offset(20f, 280f),
            parentId = "NODE-SUP-01",
            assignedShift = "Day Shift (08:00 - 16:00)",
            attendanceStatus = "CLOCKED_IN_GEOFENCE",
            weeklyHours = 37.5,
            restPeriodHours = 12.0,
            approvalTier = 1
        ),
        OrgNode(
            id = "NODE-EMP-02",
            name = "Aisha Khan",
            role = "EMPLOYEE",
            department = "Engineering",
            position = Offset(200f, 280f),
            parentId = "NODE-SUP-01",
            assignedShift = "Night Shift (22:00 - 06:00)",
            attendanceStatus = "LATE_OUTSIDE_GEOFENCE",
            weeklyHours = 52.0, // Burnout warning
            restPeriodHours = 8.5, // Rest violation (<11 hrs)
            approvalTier = 1
        ),
        // Level 3: Employees under Supervisor 2 (Sophia)
        OrgNode(
            id = "NODE-EMP-03",
            name = "Lucas Thorne",
            role = "EMPLOYEE",
            department = "HR Ops",
            position = Offset(20f, 410f),
            parentId = "NODE-SUP-02",
            assignedShift = "Swing Shift (14:00 - 22:00)",
            attendanceStatus = "CLOCKED_IN_GEOFENCE",
            weeklyHours = 48.0,
            restPeriodHours = 11.5,
            approvalTier = 1
        ),
        OrgNode(
            id = "NODE-EMP-04",
            name = "Chloe Dubois",
            role = "EMPLOYEE",
            department = "HR Ops",
            position = Offset(200f, 410f),
            parentId = "NODE-SUP-02",
            assignedShift = "Day Shift (08:00 - 16:00)",
            attendanceStatus = "OFF_DUTY",
            weeklyHours = 36.0,
            restPeriodHours = 16.0,
            approvalTier = 1
        )
    )
}

/**
 * Generates a full 1,000-employee organizational hierarchy structured across
 * Executive, Director, Supervisor, and Team Member tiers for large-scale stress testing and enterprise deployment.
 */
fun generate1000EmployeesHierarchy(): List<OrgNode> {
    val list = ArrayList<OrgNode>(1000)

    // Level 0: CEO / Executive Director
    val gm = OrgNode(
        id = "NODE-GM-01",
        name = "Elena Vance (CEO)",
        role = "GENERAL_MANAGER",
        department = "Executive Ops",
        position = Offset(2800f, 20f),
        parentId = null,
        assignedShift = "Executive Schedule",
        attendanceStatus = "CLOCKED_IN_GEOFENCE",
        weeklyHours = 40.0,
        restPeriodHours = 14.0,
        approvalTier = 3
    )
    list.add(gm)

    val departments = listOf(
        "Engineering", "Logistics", "Operations", "Customer Support",
        "Sales Ops", "Marketing", "Human Resources", "Finance & Legal",
        "IT Security", "R&D Innovation"
    )

    val firstNames = listOf(
        "Marcus", "Sophia", "David", "Aisha", "Lucas", "Chloe", "Ethan", "Olivia",
        "Liam", "Ava", "Noah", "Emma", "Oliver", "Amelia", "Elijah", "Mia",
        "James", "Harper", "Benjamin", "Evelyn", "Henry", "Abigail", "Alexander", "Ella",
        "Sebastian", "Elizabeth", "Jackson", "Sofia", "Daniel", "Avery", "Matthew", "Mila"
    )

    val lastNames = listOf(
        "Vance", "Martinez", "Chen", "Khan", "Thorne", "Dubois", "Smith", "Johnson",
        "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez",
        "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore",
        "Jackson", "Martin", "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez"
    )

    val shiftOptions = listOf(
        "Day Shift (08:00 - 16:00)",
        "Swing Shift (14:00 - 22:00)",
        "Night Shift (22:00 - 06:00)",
        "Weekend Shift"
    )

    val attendanceOptions = listOf(
        "CLOCKED_IN_GEOFENCE", "CLOCKED_IN_GEOFENCE", "CLOCKED_IN_GEOFENCE",
        "ON_BREAK", "LATE_OUTSIDE_GEOFENCE", "OFF_DUTY"
    )

    var supervisorCounter = 1
    var employeeCounter = 1

    // 10 Department Directors + 50 Supervisors + 939 Employees = 1000 Nodes
    for (deptIdx in 0 until 10) {
        val deptName = departments[deptIdx]
        val dirX = 250f + deptIdx * 560f
        val dirY = 180f
        val dirId = "NODE-DIR-${deptIdx + 1}"
        val dirName = "${firstNames[(deptIdx * 3) % firstNames.size]} ${lastNames[(deptIdx * 5) % lastNames.size]}"

        list.add(
            OrgNode(
                id = dirId,
                name = "$dirName ($deptName)",
                role = "SUPERVISOR",
                department = deptName,
                position = Offset(dirX, dirY),
                parentId = "NODE-GM-01",
                assignedShift = "Executive Schedule",
                attendanceStatus = "CLOCKED_IN_GEOFENCE",
                weeklyHours = 42.0,
                restPeriodHours = 12.5,
                approvalTier = 2
            )
        )

        // 5 Supervisors under each Director
        for (supInDept in 0 until 5) {
            val supX = dirX - 220f + supInDept * 110f
            val supY = 360f
            val supId = "NODE-SUP-${String.format("%02d", supervisorCounter)}"
            val supName = "${firstNames[(supervisorCounter * 7) % firstNames.size]} ${lastNames[(supervisorCounter * 11) % lastNames.size]}"

            list.add(
                OrgNode(
                    id = supId,
                    name = "$supName (Sup $supervisorCounter)",
                    role = "SUPERVISOR",
                    department = deptName,
                    position = Offset(supX, supY),
                    parentId = dirId,
                    assignedShift = shiftOptions[supervisorCounter % shiftOptions.size],
                    attendanceStatus = "CLOCKED_IN_GEOFENCE",
                    weeklyHours = 40.0 + (supervisorCounter % 8),
                    restPeriodHours = 12.0,
                    approvalTier = 2
                )
            )

            // Employees under this supervisor
            val empCountForThisSup = if (supervisorCounter <= 39) 19 else 18
            for (eInSup in 0 until empCountForThisSup) {
                if (list.size >= 1000) break

                val col = eInSup % 3
                val row = eInSup / 3
                val empX = supX - 40f + col * 160f
                val empY = 520f + row * 110f

                val empId = "NODE-EMP-${String.format("%04d", employeeCounter)}"
                val empName = "${firstNames[(employeeCounter * 13) % firstNames.size]} ${lastNames[(employeeCounter * 17) % lastNames.size]}"

                val hrs = 35.0 + ((employeeCounter * 3) % 22)
                val rest = 8.0 + ((employeeCounter * 5) % 8)

                list.add(
                    OrgNode(
                        id = empId,
                        name = "$empName (#$employeeCounter)",
                        role = "EMPLOYEE",
                        department = deptName,
                        position = Offset(empX, empY),
                        parentId = supId,
                        assignedShift = shiftOptions[(employeeCounter + eInSup) % shiftOptions.size],
                        attendanceStatus = attendanceOptions[employeeCounter % attendanceOptions.size],
                        weeklyHours = hrs,
                        restPeriodHours = rest,
                        approvalTier = 1
                    )
                )

                employeeCounter++
            }
            supervisorCounter++
        }
    }

    return applyPerDepartmentArchitectures(list)
}

/**
 * Main Interactive Org Tree / Node Graph Dashboard Composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrgMappingDashboard(
    viewModel: TimeTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userRole = viewModel.currentUserRole.value
    val isHrAdmin = userRole == "ADMIN_HR" || userRole == "MANAGER" || userRole == "SUPERVISOR"

    // Security Gate Check for HR/Admin privileges
    if (!isHrAdmin) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF070E0B))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0C0E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE11D48)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("org_mapping_access_denied")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Access Denied",
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Access Restricted: HR / Admin Privileges Required",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The Interactive Organizational Reporting Graph requires Manager or HR Administrator credentials.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }
        return
    }

    // State management
    var viewMode by remember { mutableStateOf(MappingViewMode.DRAG_DROP) }
    var activeOverlay by remember { mutableStateOf(MappingHeatmapOverlay.ORG_STRUCTURE) }
    var nodeList by remember { mutableStateOf(getSeededOrgNodes()) }
    var selectedEmployeeIds by remember { mutableStateOf(setOf<String>()) }
    var emergencyCoverSourceNode by remember { mutableStateOf<OrgNode?>(null) }

    // Shift Options
    val shiftPresetOptions = listOf(
        "Day Shift (08:00 - 16:00)",
        "Swing Shift (14:00 - 22:00)",
        "Night Shift (22:00 - 06:00)",
        "Weekend Shift"
    )
    var selectedShiftPreset by remember { mutableStateOf(shiftPresetOptions.first()) }

    val overlayChips = remember {
        listOf(
            HeatmapChipData(MappingHeatmapOverlay.ORG_STRUCTURE, "Hierarchy", Icons.Default.AccountTree),
            HeatmapChipData(MappingHeatmapOverlay.CASCADE_SHIFTS, "Shift Cascade", Icons.Default.Schedule),
            HeatmapChipData(MappingHeatmapOverlay.ATTENDANCE_HEATMAP, "Geofence Map", Icons.Default.GpsFixed),
            HeatmapChipData(MappingHeatmapOverlay.APPROVAL_CHAINS, "Approval Chains", Icons.Default.AltRoute),
            HeatmapChipData(MappingHeatmapOverlay.BURNOUT_OVERTIME, "Overtime Warning", Icons.Default.LocalFireDepartment),
            HeatmapChipData(MappingHeatmapOverlay.COMPLIANCE_REST, "Rest Guardrails", Icons.Default.Warning)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070E0B))
            .testTag("admin_org_mapping_dashboard")
    ) {
        // ----------------------------------------------------
        // 1. TOP TOOLBAR & VIEW MODE SWITCHER
        // ----------------------------------------------------
        Surface(
            color = Color(0xFF0F1A15),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = "Org Canvas",
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Org Node Canvas",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Dual-Mode Segmented Switch
                    ModeToggleSwitch(
                        selectedMode = viewMode,
                        onModeSelected = { viewMode = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ----------------------------------------------------
                // 2. OVERLAY & HEATMAP CHIP SELECTOR
                // ----------------------------------------------------
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(overlayChips, key = { it.type }) { chip ->
                        HeatmapOverlayChip(
                            label = chip.label,
                            icon = chip.icon,
                            isSelected = activeOverlay == chip.type,
                            onClick = { activeOverlay = chip.type }
                        )
                    }
                }
            }
        }

        // Context Description Banner
        OverlayDescriptionBanner(activeOverlay = activeOverlay)

        // ----------------------------------------------------
        // 3. MAIN CONTENT CANVAS / LIST LAYER
        // ----------------------------------------------------
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (viewMode) {
                MappingViewMode.DRAG_DROP -> {
                    DragAndDropCanvasView(
                        nodes = nodeList,
                        activeOverlay = activeOverlay,
                        selectedEmployeeIds = selectedEmployeeIds,
                        selectedShiftPreset = selectedShiftPreset,
                        emergencyCoverSourceNode = emergencyCoverSourceNode,
                        onNodesUpdated = { updatedList -> nodeList = updatedList },
                        onNodeSelected = { node ->
                            if (selectedEmployeeIds.contains(node.id)) {
                                selectedEmployeeIds = selectedEmployeeIds - node.id
                            } else {
                                selectedEmployeeIds = selectedEmployeeIds + node.id
                            }
                        },
                        onCascadeShiftToTeam = { supervisorId, shiftName ->
                            nodeList = nodeList.map { n ->
                                if (n.parentId == supervisorId || n.id == supervisorId) {
                                    n.copy(assignedShift = shiftName)
                                } else {
                                    n
                                }
                            }
                            Toast.makeText(context, "Cascaded shift to team members", Toast.LENGTH_SHORT).show()
                        },
                        onEmergencyCoverTriggered = { targetNode ->
                            if (emergencyCoverSourceNode != null) {
                                Toast.makeText(
                                    context,
                                    "Emergency Cover Requested from ${emergencyCoverSourceNode?.name} to ${targetNode.name}",
                                    Toast.LENGTH_LONG
                                ).show()
                                emergencyCoverSourceNode = null
                            } else {
                                emergencyCoverSourceNode = targetNode
                                Toast.makeText(context, "Tap an off-duty employee node to request emergency shift cover", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                MappingViewMode.SEARCH_LIST -> {
                    SearchableAssignListView(
                        nodes = nodeList,
                        selectedEmployeeIds = selectedEmployeeIds,
                        selectedShiftPreset = selectedShiftPreset,
                        onSelectionChanged = { selectedEmployeeIds = it },
                        onShiftPresetChanged = { selectedShiftPreset = it },
                        onBatchReassignSupervisor = { supervisorId ->
                            if (selectedEmployeeIds.isNotEmpty()) {
                                val supNode = nodeList.find { it.id == supervisorId }
                                nodeList = nodeList.map { n ->
                                    if (selectedEmployeeIds.contains(n.id)) {
                                        n.copy(parentId = supervisorId)
                                    } else n
                                }
                                Toast.makeText(context, "Reassigned ${selectedEmployeeIds.size} employees to Supervisor ${supNode?.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Select at least 1 employee first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBatchApplyShift = { shift ->
                            if (selectedEmployeeIds.isNotEmpty()) {
                                nodeList = nodeList.map { n ->
                                    if (selectedEmployeeIds.contains(n.id)) {
                                        n.copy(assignedShift = shift)
                                    } else n
                                }
                                Toast.makeText(context, "Batch assigned shift to ${selectedEmployeeIds.size} employees", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Select at least 1 employee first", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

private data class OverlayBannerInfo(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * Banner explaining the active heatmap or overlay mode with clean icons.
 */
@Composable
fun OverlayDescriptionBanner(activeOverlay: MappingHeatmapOverlay) {
    val info = when (activeOverlay) {
        MappingHeatmapOverlay.ORG_STRUCTURE -> OverlayBannerInfo(
            "Org Hierarchy",
            "Drag node cards to arrange team reporting structure visually.",
            Icons.Default.AccountTree,
            NeonGreen
        )
        MappingHeatmapOverlay.CASCADE_SHIFTS -> OverlayBannerInfo(
            "Shift Cascading",
            "Tap 'Cascade Shift' on a Supervisor to push shift rules down to all direct reports.",
            Icons.Default.Schedule,
            Color(0xFF3B82F6)
        )
        MappingHeatmapOverlay.ATTENDANCE_HEATMAP -> OverlayBannerInfo(
            "Live Geofence Map",
            "Green = Geofenced • Orange = Break • Red = Late/Outside. Tap red nodes for Emergency Cover.",
            Icons.Default.GpsFixed,
            Color(0xFFF59E0B)
        )
        MappingHeatmapOverlay.APPROVAL_CHAINS -> OverlayBannerInfo(
            "Approval Chain Paths",
            "Multi-tier routing: Employee (Tier 1) -> Supervisor (Tier 2) -> Director/HR (Tier 3).",
            Icons.Default.AltRoute,
            Color(0xFFA855F7)
        )
        MappingHeatmapOverlay.BURNOUT_OVERTIME -> OverlayBannerInfo(
            "Overtime & Burnout Guard",
            "Green = Standard (<40h) • Amber = Warning (40-50h) • Red = Burnout Alert (>50h).",
            Icons.Default.LocalFireDepartment,
            Color(0xFFEC4899)
        )
        MappingHeatmapOverlay.COMPLIANCE_REST -> OverlayBannerInfo(
            "Rest Period Guardrails",
            "Dashed red lines indicate rest periods less than the statutory 11 hours between shifts.",
            Icons.Default.Warning,
            Color(0xFFEF4444)
        )
    }

    Surface(
        color = info.color.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = info.icon, contentDescription = null, tint = info.color, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${info.title}: ${info.description}",
                color = Color.White,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Mode Toggle Switch Component without Emojis
 */
@Composable
fun ModeToggleSwitch(
    selectedMode: MappingViewMode,
    onModeSelected: (MappingViewMode) -> Unit
) {
    Surface(
        color = Color(0xFF13231C),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223E32))
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selectedMode == MappingViewMode.DRAG_DROP) NeonGreen else Color.Transparent)
                    .clickable { onModeSelected(MappingViewMode.DRAG_DROP) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("toggle_drag_drop_mode")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = if (selectedMode == MappingViewMode.DRAG_DROP) Color(0xFF070E0B) else Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Canvas",
                        color = if (selectedMode == MappingViewMode.DRAG_DROP) Color(0xFF070E0B) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selectedMode == MappingViewMode.SEARCH_LIST) NeonGreen else Color.Transparent)
                    .clickable { onModeSelected(MappingViewMode.SEARCH_LIST) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("toggle_search_list_mode")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ListAlt,
                        contentDescription = null,
                        tint = if (selectedMode == MappingViewMode.SEARCH_LIST) Color(0xFF070E0B) else Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "List",
                        color = if (selectedMode == MappingViewMode.SEARCH_LIST) Color(0xFF070E0B) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Heatmap / Overlay Chip Filter Button with Material Icons
 */
@Composable
fun HeatmapOverlayChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) NeonGreen.copy(alpha = 0.18f) else Color(0xFF13231C),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonGreen else Color(0xFF223E32)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) NeonGreen else Color(0xFF94A3B8),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = if (isSelected) NeonGreen else Color.White,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

/**
 * Department Architecture Sector Metadata
 */
data class DepartmentArchMeta(
    val deptName: String,
    val archTitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val description: String,
    val sectorOrigin: Offset,
    val sectorWidth: Float = 950f,
    val sectorHeight: Float = 1350f
)

fun getDepartmentArchitectureMetadata(): Map<String, DepartmentArchMeta> {
    return mapOf(
        "Engineering" to DepartmentArchMeta(
            deptName = "Engineering",
            archTitle = "Agile Squad Matrix",
            icon = Icons.Default.Terminal,
            accentColor = Color(0xFF00E5FF),
            description = "Multi-sprint squad columns with horizontal tech lead branching.",
            sectorOrigin = Offset(100f, 320f)
        ),
        "Logistics" to DepartmentArchMeta(
            deptName = "Logistics",
            archTitle = "Hub & Spoke Fleet Radial",
            icon = Icons.Default.LocalShipping,
            accentColor = Color(0xFFF59E0B),
            description = "Central dispatch hub radiating to driver fleet spokes.",
            sectorOrigin = Offset(1150f, 320f)
        ),
        "Operations" to DepartmentArchMeta(
            deptName = "Operations",
            archTitle = "Command Center Grid",
            icon = Icons.Default.PrecisionManufacturing,
            accentColor = Color(0xFF10B981),
            description = "High-density 2D operational shift floor matrix.",
            sectorOrigin = Offset(2200f, 320f)
        ),
        "Customer Support" to DepartmentArchMeta(
            deptName = "Customer Support",
            archTitle = "Tiered Escalation Ladder",
            icon = Icons.Default.SupportAgent,
            accentColor = Color(0xFFA855F7),
            description = "3-tier escalation ladder from Tier 1 Agents to Lead Escalation.",
            sectorOrigin = Offset(3250f, 320f)
        ),
        "Sales Ops" to DepartmentArchMeta(
            deptName = "Sales Ops",
            archTitle = "Horizontal Pipeline Funnel",
            icon = Icons.Default.TrendingUp,
            accentColor = Color(0xFFF97316),
            description = "Left-to-right sales funnel pipeline across account stages.",
            sectorOrigin = Offset(4300f, 320f)
        ),
        "Marketing" to DepartmentArchMeta(
            deptName = "Marketing",
            archTitle = "Creative Pod Cluster",
            icon = Icons.Default.Campaign,
            accentColor = Color(0xFFEC4899),
            description = "Surrounding creative pod clusters around central brand director.",
            sectorOrigin = Offset(100f, 1800f)
        ),
        "Human Resources" to DepartmentArchMeta(
            deptName = "Human Resources",
            archTitle = "Pyramid Compliance Tree",
            icon = Icons.Default.Groups,
            accentColor = Color(0xFF14B8A6),
            description = "Structured compliance hierarchy with strict manager spans.",
            sectorOrigin = Offset(1150f, 1800f)
        ),
        "Finance & Legal" to DepartmentArchMeta(
            deptName = "Finance & Legal",
            archTitle = "Dual-Chain Audit Matrix",
            icon = Icons.Default.AccountBalance,
            accentColor = Color(0xFFEAB308),
            description = "Parallel controller & legal counsel compliance governance chains.",
            sectorOrigin = Offset(2200f, 1800f)
        ),
        "IT Security" to DepartmentArchMeta(
            deptName = "IT Security",
            archTitle = "SecOps SOC Tactical Hub",
            icon = Icons.Default.Security,
            accentColor = Color(0xFFEF4444),
            description = "SOC tactical command hub for cyber threat & IAM access leads.",
            sectorOrigin = Offset(3250f, 1800f)
        ),
        "R&D Innovation" to DepartmentArchMeta(
            deptName = "R&D Innovation",
            archTitle = "Lab Incubation Cluster",
            icon = Icons.Default.Science,
            accentColor = Color(0xFF6366F1),
            description = "Autonomous lab incubation pods with direct executive steering.",
            sectorOrigin = Offset(4300f, 1800f)
        )
    )
}

fun createInitialNodesForNewDepartment(
    deptName: String,
    directorName: String,
    origin: Offset
): List<OrgNode> {
    val cleanPrefix = deptName.replace("[^a-zA-Z0-9]".toRegex(), "").take(4).uppercase().ifEmpty { "DEPT" }
    val dirId = "NODE-$cleanPrefix-DIR-01"
    val sup1Id = "NODE-$cleanPrefix-SUP-01"
    val sup2Id = "NODE-$cleanPrefix-SUP-02"

    return listOf(
        OrgNode(
            id = dirId,
            name = directorName.ifBlank { "$deptName Director" },
            role = "SUPERVISOR",
            department = deptName,
            assignedShift = "Day Shift (08:00 - 17:00)",
            position = origin + Offset(400f, 60f),
            parentId = "NODE-GM-01",
            approvalTier = 3
        ),
        OrgNode(
            id = sup1Id,
            name = "$deptName Lead Alpha",
            role = "SUPERVISOR",
            department = deptName,
            assignedShift = "Day Shift (08:00 - 17:00)",
            position = origin + Offset(150f, 180f),
            parentId = dirId,
            approvalTier = 2
        ),
        OrgNode(
            id = sup2Id,
            name = "$deptName Lead Beta",
            role = "SUPERVISOR",
            department = deptName,
            assignedShift = "Evening Shift (16:00 - 01:00)",
            position = origin + Offset(650f, 180f),
            parentId = dirId,
            approvalTier = 2
        ),
        OrgNode(
            id = "NODE-$cleanPrefix-EMP-01",
            name = "$deptName Specialist 1",
            role = "EMPLOYEE",
            department = deptName,
            assignedShift = "Day Shift (08:00 - 17:00)",
            position = origin + Offset(150f, 310f),
            parentId = sup1Id
        ),
        OrgNode(
            id = "NODE-$cleanPrefix-EMP-02",
            name = "$deptName Specialist 2",
            role = "EMPLOYEE",
            department = deptName,
            assignedShift = "Day Shift (08:00 - 17:00)",
            position = origin + Offset(150f, 405f),
            parentId = sup1Id
        ),
        OrgNode(
            id = "NODE-$cleanPrefix-EMP-03",
            name = "$deptName Specialist 3",
            role = "EMPLOYEE",
            department = deptName,
            assignedShift = "Evening Shift (16:00 - 01:00)",
            position = origin + Offset(650f, 310f),
            parentId = sup2Id
        ),
        OrgNode(
            id = "NODE-$cleanPrefix-EMP-04",
            name = "$deptName Specialist 4",
            role = "EMPLOYEE",
            department = deptName,
            assignedShift = "Evening Shift (16:00 - 01:00)",
            position = origin + Offset(650f, 405f),
            parentId = sup2Id
        )
    )
}

/**
 * Positions nodes according to per-department custom layout architecture topologies
 */
fun applyPerDepartmentArchitectures(
    nodes: List<OrgNode>,
    providedMetaMap: Map<String, DepartmentArchMeta>? = null
): List<OrgNode> {
    val metaMap = providedMetaMap ?: getDepartmentArchitectureMetadata()
    val updatedList = nodes.toMutableList()

    // 1. Position Executive GM / CEO
    val gmIndex = updatedList.indexOfFirst { it.role == "GENERAL_MANAGER" || it.parentId == null }
    if (gmIndex != -1) {
        updatedList[gmIndex] = updatedList[gmIndex].copy(position = Offset(2650f, 50f))
    }

    // 2. Group non-executive nodes by department and arrange using department architecture topologies
    val deptGroups = updatedList.filter { it.department != "Executive Ops" && it.parentId != null }.groupBy { it.department }

    deptGroups.forEach { (deptName, deptNodes) ->
        val meta = metaMap[deptName] ?: DepartmentArchMeta(
            deptName = deptName,
            archTitle = "Standard Hierarchy",
            icon = Icons.Default.Domain,
            accentColor = NeonGreen,
            description = "Standard org hierarchy",
            sectorOrigin = Offset(100f, 320f)
        )

        val origin = meta.sectorOrigin
        val director = deptNodes.find { it.parentId == "NODE-GM-01" || it.id.contains("DIR") || (it.role == "SUPERVISOR" && it.approvalTier >= 2) } ?: deptNodes.firstOrNull()
        val supervisors = deptNodes.filter { it.role == "SUPERVISOR" && it.id != director?.id }
        val employees = deptNodes.filter { it.role == "EMPLOYEE" }

        when (deptName) {
            "Engineering" -> {
                // Agile Squad Matrix: Director top center -> Squad leads horizontal -> Devs vertical in squad columns
                director?.let { dir ->
                    val idx = updatedList.indexOfFirst { it.id == dir.id }
                    if (idx != -1) updatedList[idx] = dir.copy(position = origin + Offset(400f, 60f))
                }
                supervisors.forEachIndexed { supIdx, sup ->
                    val idx = updatedList.indexOfFirst { it.id == sup.id }
                    val supX = 40f + supIdx * 180f
                    if (idx != -1) updatedList[idx] = sup.copy(position = origin + Offset(supX, 180f))

                    val squadEmps = employees.filter { it.parentId == sup.id }
                    squadEmps.forEachIndexed { eIdx, emp ->
                        val eIdxInList = updatedList.indexOfFirst { it.id == emp.id }
                        if (eIdxInList != -1) {
                            updatedList[eIdxInList] = emp.copy(position = origin + Offset(supX, 310f + eIdx * 95f))
                        }
                    }
                }
            }

            "Logistics" -> {
                // Hub & Spoke Fleet Radial: Director hub -> Supervisors inner ring -> Fleet outer radial spokes
                director?.let { dir ->
                    val idx = updatedList.indexOfFirst { it.id == dir.id }
                    if (idx != -1) updatedList[idx] = dir.copy(position = origin + Offset(420f, 420f))
                }
                val supCount = supervisors.size.coerceAtLeast(1)
                supervisors.forEachIndexed { supIdx, sup ->
                    val angle = (supIdx * (2 * Math.PI / supCount) - Math.PI / 2).toFloat()
                    val supX = 420f + 180f * cos(angle)
                    val supY = 420f + 180f * sin(angle)
                    val idx = updatedList.indexOfFirst { it.id == sup.id }
                    if (idx != -1) updatedList[idx] = sup.copy(position = origin + Offset(supX, supY))

                    val fleetEmps = employees.filter { it.parentId == sup.id }
                    fleetEmps.forEachIndexed { eIdx, emp ->
                        val eIdxInList = updatedList.indexOfFirst { it.id == emp.id }
                        val empAngle = angle + ((eIdx - fleetEmps.size / 2f) * 0.14f)
                        val empDist = 340f + (eIdx % 2) * 80f
                        val empX = 420f + empDist * cos(empAngle)
                        val empY = 420f + empDist * sin(empAngle)
                        if (eIdxInList != -1) updatedList[eIdxInList] = emp.copy(position = origin + Offset(empX, empY))
                    }
                }
            }

            "Operations" -> {
                // Command Center Grid: Director top left -> Supervisors header row -> 2D operational matrix grid
                director?.let { dir ->
                    val idx = updatedList.indexOfFirst { it.id == dir.id }
                    if (idx != -1) updatedList[idx] = dir.copy(position = origin + Offset(40f, 60f))
                }
                supervisors.forEachIndexed { supIdx, sup ->
                    val idx = updatedList.indexOfFirst { it.id == sup.id }
                    if (idx != -1) updatedList[idx] = sup.copy(position = origin + Offset(220f + supIdx * 160f, 60f))
                }
                employees.forEachIndexed { eIdx, emp ->
                    val col = eIdx % 5
                    val row = eIdx / 5
                    val idx = updatedList.indexOfFirst { it.id == emp.id }
                    if (idx != -1) updatedList[idx] = emp.copy(position = origin + Offset(40f + col * 170f, 180f + row * 95f))
                }
            }

            "Customer Support" -> {
                // Tiered Escalation Ladder: Director -> Tier 3 leads -> Tier 2 -> Tier 1 Support ladder
                director?.let { dir ->
                    val idx = updatedList.indexOfFirst { it.id == dir.id }
                    if (idx != -1) updatedList[idx] = dir.copy(position = origin + Offset(350f, 60f))
                }
                supervisors.forEachIndexed { supIdx, sup ->
                    val idx = updatedList.indexOfFirst { it.id == sup.id }
                    if (idx != -1) updatedList[idx] = sup.copy(position = origin + Offset(80f + supIdx * 150f, 160f + supIdx * 25f))
                }
                employees.forEachIndexed { eIdx, emp ->
                    val col = eIdx % 4
                    val tierRow = eIdx / 4
                    val idx = updatedList.indexOfFirst { it.id == emp.id }
                    if (idx != -1) updatedList[idx] = emp.copy(position = origin + Offset(40f + col * 170f + tierRow * 25f, 290f + tierRow * 95f))
                }
            }

            "Sales Ops" -> {
                // Horizontal Pipeline Funnel: Director left -> Regional leads middle -> Account Execs pipeline columns
                director?.let { dir ->
                    val idx = updatedList.indexOfFirst { it.id == dir.id }
                    if (idx != -1) updatedList[idx] = dir.copy(position = origin + Offset(40f, 250f))
                }
                supervisors.forEachIndexed { supIdx, sup ->
                    val idx = updatedList.indexOfFirst { it.id == sup.id }
                    if (idx != -1) updatedList[idx] = sup.copy(position = origin + Offset(220f, 80f + supIdx * 110f))
                }
                employees.forEachIndexed { eIdx, emp ->
                    val stage = eIdx % 4
                    val row = eIdx / 4
                    val idx = updatedList.indexOfFirst { it.id == emp.id }
                    if (idx != -1) updatedList[idx] = emp.copy(position = origin + Offset(40f + stage * 180f, 380f + row * 95f))
                }
            }

            "Marketing" -> {
                // Creative Pod Cluster: Brand Director center -> Creative pod hubs -> Satellite pods
                director?.let { dir ->
                    val idx = updatedList.indexOfFirst { it.id == dir.id }
                    if (idx != -1) updatedList[idx] = dir.copy(position = origin + Offset(400f, 350f))
                }
                val supCount = supervisors.size.coerceAtLeast(1)
                supervisors.forEachIndexed { supIdx, sup ->
                    val angle = (supIdx * (2 * Math.PI / supCount) - Math.PI / 2).toFloat()
                    val supX = 400f + 200f * cos(angle)
                    val supY = 350f + 200f * sin(angle)
                    val idx = updatedList.indexOfFirst { it.id == sup.id }
                    if (idx != -1) updatedList[idx] = sup.copy(position = origin + Offset(supX, supY))

                    val podEmps = employees.filter { it.parentId == sup.id }
                    podEmps.forEachIndexed { eIdx, emp ->
                        val subAngle = angle + (eIdx * 0.45f)
                        val eIdxInList = updatedList.indexOfFirst { it.id == emp.id }
                        if (eIdxInList != -1) updatedList[eIdxInList] = emp.copy(position = origin + Offset(supX + 95f * cos(subAngle), supY + 95f * sin(subAngle)))
                    }
                }
            }

            else -> {
                // Standard Department Layout for HR, Finance, IT Security, R&D
                director?.let { dir ->
                    val idx = updatedList.indexOfFirst { it.id == dir.id }
                    if (idx != -1) updatedList[idx] = dir.copy(position = origin + Offset(380f, 60f))
                }
                supervisors.forEachIndexed { supIdx, sup ->
                    val idx = updatedList.indexOfFirst { it.id == sup.id }
                    if (idx != -1) updatedList[idx] = sup.copy(position = origin + Offset(80f + supIdx * 160f, 180f))
                }
                employees.forEachIndexed { eIdx, emp ->
                    val col = eIdx % 5
                    val row = eIdx / 5
                    val idx = updatedList.indexOfFirst { it.id == emp.id }
                    if (idx != -1) updatedList[idx] = emp.copy(position = origin + Offset(40f + col * 170f, 300f + row * 95f))
                }
            }
        }
    }

    return updatedList
}

// Enum for Hierarchy Layout Templates
enum class HierarchyTemplate(val label: String, val icon: ImageVector) {
    DEPT_ARCHITECTURES("Per-Dept Architectures", Icons.Default.Domain),
    PYRAMID("Pyramid Tree", Icons.Default.AccountTree),
    HORIZONTAL("Horizontal Flow", Icons.Default.ViewColumn),
    MATRIX("Matrix Grid", Icons.Default.GridView),
    RADIAL("Radial Star", Icons.Default.Hub),
    SCALE_1000("1,000 Scale Tree", Icons.Default.Groups)
}

/**
 * Applies preset coordinate math for automatic layout auto-positioning
 */
fun applyHierarchyTemplate(nodes: List<OrgNode>, template: HierarchyTemplate): List<OrgNode> {
    return when (template) {
        HierarchyTemplate.DEPT_ARCHITECTURES -> {
            applyPerDepartmentArchitectures(nodes)
        }
        HierarchyTemplate.SCALE_1000 -> {
            generate1000EmployeesHierarchy()
        }
        HierarchyTemplate.PYRAMID -> {
            nodes.map { node ->
                val pos = when (node.id) {
                    "NODE-GM-01" -> Offset(115f, 20f)
                    "NODE-SUP-01" -> Offset(20f, 150f)
                    "NODE-SUP-02" -> Offset(200f, 150f)
                    "NODE-EMP-01" -> Offset(20f, 280f)
                    "NODE-EMP-02" -> Offset(200f, 280f)
                    "NODE-EMP-03" -> Offset(20f, 410f)
                    "NODE-EMP-04" -> Offset(200f, 410f)
                    else -> Offset(100f, 500f)
                }
                node.copy(position = pos)
            }
        }
        HierarchyTemplate.HORIZONTAL -> {
            nodes.mapIndexed { index, node ->
                val x = 20f + (index % 3) * 180f
                val y = 40f + (index / 3) * 160f
                node.copy(position = Offset(x, y))
            }
        }
        HierarchyTemplate.MATRIX -> {
            nodes.mapIndexed { index, node ->
                val col = index % 2
                val row = index / 2
                val x = 20f + col * 190f
                val y = 30f + row * 140f
                node.copy(position = Offset(x, y))
            }
        }
        HierarchyTemplate.RADIAL -> {
            val centerX = 110f
            val centerY = 240f
            val radius = 160f
            val nonGmNodes = nodes.filter { it.id != "NODE-GM-01" }
            nodes.map { node ->
                if (node.id == "NODE-GM-01") {
                    node.copy(position = Offset(centerX, centerY))
                } else {
                    val idx = nonGmNodes.indexOfFirst { it.id == node.id }
                    val angle = (2 * Math.PI * idx / nonGmNodes.size) - (Math.PI / 2)
                    val x = (centerX + radius * Math.cos(angle)).toFloat().coerceIn(10f, 240f)
                    val y = (centerY + radius * Math.sin(angle)).toFloat().coerceIn(10f, 480f)
                    node.copy(position = Offset(x, y))
                }
            }
        }
    }
}

/**
 * DRAG & DROP CANVAS VIEW WITH ACCURATE BEZIER CURVE CONNECTORS, DOT GRID BACKGROUND, ZOOM, HAPTICS & CANVAS TOOLS
 */
@Composable
fun DragAndDropCanvasView(
    nodes: List<OrgNode>,
    activeOverlay: MappingHeatmapOverlay,
    selectedEmployeeIds: Set<String>,
    selectedShiftPreset: String,
    emergencyCoverSourceNode: OrgNode?,
    onNodesUpdated: (List<OrgNode>) -> Unit,
    onNodeSelected: (OrgNode) -> Unit,
    onCascadeShiftToTeam: (supervisorId: String, shiftName: String) -> Unit,
    onEmergencyCoverTriggered: (OrgNode) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val cardWidthDp = 150.dp
    val cardWidthPx = with(density) { cardWidthDp.toPx() }

    var scale by remember { mutableFloatStateOf(1.0f) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }
    var isFrozen by remember { mutableStateOf(false) }
    var localSelectedIds by remember { mutableStateOf(selectedEmployeeIds) }

    LaunchedEffect(selectedEmployeeIds) {
        localSelectedIds = selectedEmployeeIds
    }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var selectedDeptFilter by remember { mutableStateOf<String?>("Engineering") }
    val deptMetaMap = remember {
        mutableStateMapOf<String, DepartmentArchMeta>().apply {
            putAll(getDepartmentArchitectureMetadata())
        }
    }

    var isFullScreenMode by remember { mutableStateOf(false) }
    var showAddCanvasDialog by remember { mutableStateOf(false) }

    // Add Department Canvas Form state
    var newDeptCanvasName by remember { mutableStateOf("") }
    var selectedArchPreset by remember { mutableStateOf("Agile Squad Matrix") }
    var selectedColorHex by remember { mutableStateOf(Color(0xFF00E5FF)) }
    var newDeptMission by remember { mutableStateOf("") }
    var newDeptDirectorName by remember { mutableStateOf("") }

    // Spatial viewport tracking for 1,000 nodes high-performance rendering
    var viewportWidthPx by remember { mutableFloatStateOf(1080f) }
    var viewportHeightPx by remember { mutableFloatStateOf(1920f) }

    // Canvas Node Search & Jump
    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }

    // User tools state
    var isLightMode by remember { mutableStateOf(false) } // Default mode is Dark mode
    var isPathMode by remember { mutableStateOf(false) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var isLassoMode by remember { mutableStateOf(false) }
    var lassoStartOffset by remember { mutableStateOf<Offset?>(null) }
    var lassoCurrentOffset by remember { mutableStateOf<Offset?>(null) }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var showSaveExportDialog by remember { mutableStateOf(false) }
    var selectedExportFormat by remember { mutableStateOf("PNG") } // PNG, JPEG, PDF, SVG
    var customExportFileName by remember { mutableStateOf("All_Departments_${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}") }

    // Undo / Redo stacks
    val historyStack = remember { mutableStateListOf<List<OrgNode>>() }
    val redoStack = remember { mutableStateListOf<List<OrgNode>>() }
    val haptic = LocalHapticFeedback.current

    // Add Employee Form state
    var newEmpNameOrId by remember { mutableStateOf("") }
    var newEmpRole by remember { mutableStateOf("EMPLOYEE") }
    var selectedSupervisorId by remember { mutableStateOf<String?>(null) }

    // Separate Department Canvas Node Isolation filter
    val activeCanvasNodes = remember(nodes, selectedDeptFilter) {
        if (selectedDeptFilter == "ALL" || selectedDeptFilter == null) {
            nodes
        } else {
            nodes.filter { it.department == selectedDeptFilter || it.role == "GENERAL_MANAGER" }
        }
    }

    // Fast O(1) node map lookup for connectors
    val nodeMap = remember(activeCanvasNodes) { activeCanvasNodes.associateBy { it.id } }

    // Viewport spatial culling bounding box in canvas coordinates
    val padding = 350f
    val visibleLeft = (-canvasOffset.x - padding) / scale
    val visibleRight = (-canvasOffset.x + viewportWidthPx + padding) / scale
    val visibleTop = (-canvasOffset.y - padding) / scale
    val visibleBottom = (-canvasOffset.y + viewportHeightPx + padding) / scale

    // Spatially culled nodes list for rendering
    val visibleNodes = remember(activeCanvasNodes, canvasOffset, scale, viewportWidthPx, viewportHeightPx) {
        if (activeCanvasNodes.size <= 50) {
            activeCanvasNodes
        } else {
            activeCanvasNodes.filter { node ->
                node.position.x + cardWidthPx >= visibleLeft && node.position.x <= visibleRight &&
                node.position.y + 120f >= visibleTop && node.position.y <= visibleBottom
            }
        }
    }

    val visibleNodeIds = remember(visibleNodes) { visibleNodes.map { it.id }.toSet() }

    // Dynamic theme colors based on Light/Dark Mode toggle (Soft dimmed slate for comfortable light mode viewing)
    val canvasBgColor = if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF09140F)
    val canvasBorderColor = if (isLightMode) Color(0xFF94A3B8) else Color(0xFF1B382B)
    val topHeaderBgColor = if (isLightMode) Color(0xFF94A3B8) else Color(0xFF0F2018)
    val topHeaderTextColor = if (isLightMode) Color(0xFF0F172A) else Color(0xFF94A3B8)
    val dotGridColor = if (isLightMode) Color(0xFF475569).copy(alpha = 0.50f) else Color(0xFF34D399).copy(alpha = 0.22f)
    val majorDotColor = if (isLightMode) Color(0xFF0284C7).copy(alpha = 0.70f) else Color(0xFF10B981).copy(alpha = 0.65f)
    val dockBgColor = if (isLightMode) Color(0xFFE2E8F0).copy(alpha = 0.95f) else Color(0xFF07120D).copy(alpha = 0.95f)
    val dockBorderColor = if (isLightMode) Color(0xFF94A3B8) else Color(0xFF1F4232)

    @Composable
    fun RenderCanvasCard() {
        Card(
            colors = CardDefaults.cardColors(containerColor = canvasBgColor),
            border = if (isFullScreenMode) null else androidx.compose.foundation.BorderStroke(1.dp, canvasBorderColor),
            shape = if (isFullScreenMode) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
            modifier = if (isFullScreenMode) {
                Modifier.fillMaxSize().clipToBounds().testTag("interactive_org_canvas_view")
            } else {
                Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 2.dp).clipToBounds().testTag("interactive_org_canvas_view")
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            // ----------------------------------------------------
            // 1. TOP HEADER OF CANVAS CARD WITH CANVAS CONTROLS & ZOOM
            // ----------------------------------------------------
            Surface(
                color = topHeaderBgColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Canvas Status & Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isFrozen) Color(0xFFEF4444) else NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedDeptFilter != "ALL") "$selectedDeptFilter Canvas" else "Org Canvas",
                            color = topHeaderTextColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isMultiSelectMode || localSelectedIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Selected (${localSelectedIds.size})",
                                    color = Color(0xFF3B82F6),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (isLassoMode) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonGreen.copy(alpha = 0.25f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Elastic Lasso Active",
                                    color = if (isLightMode) Color(0xFF047857) else NeonGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Right: Tool controls in FullScreen mode vs Clean Member Count in normal mode
                    if (isFullScreenMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Light / Dark Theme Switch
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isLightMode) Color(0xFFFEF3C7) else Color(0xFF1F2937))
                                    .clickable {
                                        isLightMode = !isLightMode
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, if (isLightMode) "Switched to Light Mode" else "Switched to Dark Mode", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isLightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Theme",
                                        tint = if (isLightMode) Color(0xFFD97706) else Color(0xFFFBBF24),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isLightMode) "Light" else "Dark",
                                        color = if (isLightMode) Color(0xFF92400E) else Color(0xFFF3F4F6),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Freeze Canvas Toggle
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isFrozen) Color(0xFFFEE2E2) else (if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF1E293B)))
                                    .clickable {
                                        isFrozen = !isFrozen
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, if (isFrozen) "Canvas Locked: Dragging disabled" else "Canvas Unlocked: Dragging enabled", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isFrozen) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Freeze",
                                        tint = if (isFrozen) Color(0xFFDC2626) else if (isLightMode) Color(0xFF475569) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isFrozen) "Locked" else "Lock",
                                        color = if (isFrozen) Color(0xFF991B1B) else if (isLightMode) Color(0xFF334155) else Color(0xFFCBD5E1),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }


                        }
                    } else {
                        // Non-Fullscreen Mode: Clean badge showing member count
                        Surface(
                            color = if (isLightMode) Color(0xFFE2E8F0) else Color(0xFF13261D),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${activeCanvasNodes.size} Nodes",
                                color = if (isLightMode) Color(0xFF047857) else NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(canvasBorderColor)
            )

            // ----------------------------------------------------
            // DEPARTMENT ARCHITECTURE SELECTOR BAR
            // ----------------------------------------------------
            val deptFilterList = remember(deptMetaMap.size) {
                deptMetaMap.keys.toList() + listOf("ALL")
            }

            Surface(
                color = (if (isLightMode) Color(0xFFF1F5F9) else Color(0xFF0A1510)),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(deptFilterList) { filterDept ->
                        val isSel = selectedDeptFilter == filterDept
                        val meta = deptMetaMap[filterDept]
                        val chipBg = if (isSel) {
                            meta?.accentColor ?: NeonGreen
                        } else {
                            if (isLightMode) Color(0xFFE2E8F0) else Color(0xFF13261D)
                        }
                        val chipText = if (isSel) {
                            Color(0xFF040A07)
                        } else {
                            if (isLightMode) Color(0xFF334155) else Color(0xFF94A3B8)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBg)
                                .clickable {
                                    selectedDeptFilter = filterDept
                                    if (filterDept != "ALL" && meta != null) {
                                        scale = 0.85f
                                        canvasOffset = Offset(
                                            -meta.sectorOrigin.x * 0.85f + viewportWidthPx / 2f - 350f,
                                            -meta.sectorOrigin.y * 0.85f + viewportHeightPx / 2f - 200f
                                        )
                                        Toast.makeText(context, "Opened ${meta.deptName} Canvas (${meta.archTitle})", Toast.LENGTH_SHORT).show()
                                    } else {
                                        scale = 0.45f
                                        canvasOffset = Offset(-500f, 100f)
                                        Toast.makeText(context, "Viewing All Department Architectures", Toast.LENGTH_SHORT).show()
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (meta != null) {
                                    Icon(
                                        imageVector = meta.icon,
                                        contentDescription = null,
                                        tint = if (isSel) Color(0xFF040A07) else meta.accentColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = if (filterDept == "ALL") "All Canvases" else filterDept,
                                    color = chipText,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // "+ Add Canvas" button for HR / Admin
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isLightMode) Color(0xFF0284C7) else Color(0xFF0EA5E9))
                                .clickable {
                                    showAddCanvasDialog = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                                .testTag("add_org_canvas_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Canvas",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "+ Add Canvas",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 2. CANVAS AREA (CLIPPED WITHIN CARD)
            // ----------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .onSizeChanged { size ->
                        viewportWidthPx = size.width.toFloat()
                        viewportHeightPx = size.height.toFloat()
                    }
                    .then(
                        if (isLassoMode) {
                            Modifier.pointerInput(isLassoMode, scale, canvasOffset) {
                                detectDragGestures(
                                    onDragStart = { pointerOffset ->
                                        val canvasX = (pointerOffset.x - canvasOffset.x) / scale
                                        val canvasY = (pointerOffset.y - canvasOffset.y) / scale
                                        val startPt = Offset(canvasX, canvasY)
                                        lassoStartOffset = startPt
                                        lassoCurrentOffset = startPt
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        lassoCurrentOffset?.let { curr ->
                                            val newCurr = curr + (dragAmount / scale)
                                            lassoCurrentOffset = newCurr

                                            val start = lassoStartOffset ?: return@let
                                            val minX = minOf(start.x, newCurr.x)
                                            val maxX = maxOf(start.x, newCurr.x)
                                            val minY = minOf(start.y, newCurr.y)
                                            val maxY = maxOf(start.y, newCurr.y)

                                            val lassoRect = androidx.compose.ui.geometry.Rect(minX, minY, maxX, maxY)

                                            val cardW = 160f
                                            val cardH = 75f
                                            val selectedSet = mutableSetOf<String>()
                                            activeCanvasNodes.forEach { n ->
                                                val nodeRect = androidx.compose.ui.geometry.Rect(n.position.x, n.position.y, n.position.x + cardW, n.position.y + cardH)
                                                if (lassoRect.overlaps(nodeRect)) {
                                                    selectedSet.add(n.id)
                                                }
                                            }
                                            localSelectedIds = selectedSet
                                        }
                                    },
                                    onDragEnd = {
                                        val count = localSelectedIds.size
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, "Elastic Lasso selected $count card(s). Drag any card to move group!", Toast.LENGTH_SHORT).show()
                                        lassoStartOffset = null
                                        lassoCurrentOffset = null
                                    },
                                    onDragCancel = {
                                        lassoStartOffset = null
                                        lassoCurrentOffset = null
                                    }
                                )
                            }
                        } else {
                            Modifier.pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.25f, 3.0f)
                                    canvasOffset += pan
                                }
                            }
                        }
                    )
            ) {
                // Transformable Layer for Nodes & Bezier Curves
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = canvasOffset.x
                            translationY = canvasOffset.y
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                ) {
                    // ----------------------------------------------------
                    // CANVAS: DOT GRID BACKGROUND & BEZIER CONNECTORS
                    // ----------------------------------------------------
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 1. DRAW DOTTED COMMAND-CENTER GRID BACKGROUND (SPATIALLY CULLED)
                        val dotSpacing = 28f
                        val baseRadius = 1.8f
                        val majorRadius = 3.2f

                        val startX = (((visibleLeft / dotSpacing).toInt() - 1) * dotSpacing).coerceAtLeast(0f)
                        val endX = (((visibleRight / dotSpacing).toInt() + 2) * dotSpacing).coerceAtMost(8000f)
                        val startY = (((visibleTop / dotSpacing).toInt() - 1) * dotSpacing).coerceAtLeast(0f)
                        val endY = (((visibleBottom / dotSpacing).toInt() + 2) * dotSpacing).coerceAtMost(20000f)

                        var ix = (startX / dotSpacing).toInt()
                        var gridX = startX
                        while (gridX <= endX) {
                            var iy = (startY / dotSpacing).toInt()
                            var gridY = startY
                            while (gridY <= endY) {
                                val isMajorIntersection = (ix % 5 == 0 && iy % 5 == 0)
                                if (isMajorIntersection) {
                                    // Major tactical coordinate intersection dot + subtle crosshair ticks
                                    drawCircle(
                                        color = majorDotColor,
                                        radius = majorRadius,
                                        center = Offset(gridX, gridY)
                                    )
                                    val tickLen = 4.5f
                                    drawLine(
                                        color = majorDotColor,
                                        start = Offset(gridX - tickLen, gridY),
                                        end = Offset(gridX + tickLen, gridY),
                                        strokeWidth = 1.2f
                                    )
                                    drawLine(
                                        color = majorDotColor,
                                        start = Offset(gridX, gridY - tickLen),
                                        end = Offset(gridX, gridY + tickLen),
                                        strokeWidth = 1.2f
                                    )
                                } else {
                                    // Standard grid dot
                                    drawCircle(
                                        color = dotGridColor,
                                        radius = baseRadius,
                                        center = Offset(gridX, gridY)
                                    )
                                }
                                gridY += dotSpacing
                                iy++
                            }
                            gridX += dotSpacing
                            ix++
                        }

                        // 1.5. DRAW DEPARTMENT SECTOR BOUNDARY PANELS
                        deptMetaMap.forEach { (deptName, meta) ->
                            val origin = meta.sectorOrigin
                            val sWidth = meta.sectorWidth
                            val sHeight = meta.sectorHeight

                            if (origin.x + sWidth >= visibleLeft && origin.x <= visibleRight &&
                                origin.y + sHeight >= visibleTop && origin.y <= visibleBottom) {

                                val isSelectedDept = selectedDeptFilter == "ALL" || selectedDeptFilter == deptName
                                val alphaFactor = if (isSelectedDept) 1.0f else 0.20f

                                drawRoundRect(
                                    color = meta.accentColor.copy(alpha = 0.05f * alphaFactor),
                                    topLeft = origin,
                                    size = Size(sWidth, sHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
                                )

                                drawRoundRect(
                                    color = meta.accentColor.copy(alpha = 0.35f * alphaFactor),
                                    topLeft = origin,
                                    size = Size(sWidth, sHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                                    style = Stroke(
                                        width = if (isSelectedDept) 2.2f else 1.0f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                    )
                                )
                            }
                        }

                        // 1.6 DRAW ELASTIC LASSO SELECTION RECTANGLE
                        if (lassoStartOffset != null && lassoCurrentOffset != null) {
                            val start = lassoStartOffset!!
                            val curr = lassoCurrentOffset!!
                            val minX = minOf(start.x, curr.x)
                            val maxX = maxOf(start.x, curr.x)
                            val minY = minOf(start.y, curr.y)
                            val maxY = maxOf(start.y, curr.y)
                            val lassoRectSize = Size(maxX - minX, maxY - minY)
                            val topLeft = Offset(minX, minY)

                            drawRect(
                                color = NeonGreen.copy(alpha = 0.22f),
                                topLeft = topLeft,
                                size = lassoRectSize
                            )

                            drawRect(
                                color = NeonGreen,
                                topLeft = topLeft,
                                size = lassoRectSize,
                                style = Stroke(
                                    width = 2.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                )
                            )
                        }

                        // 2. DRAW BEZIER CONNECTOR LINES (SPATIALLY CULLED FOR 1,000 NODES)
                        activeCanvasNodes.forEach { node ->
                            node.parentId?.let { pId ->
                                val isChildVis = visibleNodeIds.contains(node.id)
                                val isParentVis = visibleNodeIds.contains(pId)

                                if (nodes.size > 50 && !isChildVis && !isParentVis) {
                                    return@forEach
                                }

                                val parentNode = nodeMap[pId]
                                if (parentNode != null) {
                                    val path = Path().apply {
                                        val parentCenterX = parentNode.position.x + (cardWidthPx / 2f)
                                        val parentBottomY = parentNode.position.y + 70f
                                        val childCenterX = node.position.x + (cardWidthPx / 2f)
                                        val childTopY = node.position.y

                                        moveTo(parentCenterX, parentBottomY)
                                        cubicTo(
                                            parentCenterX, (parentBottomY + childTopY) / 2f,
                                            childCenterX, (parentBottomY + childTopY) / 2f,
                                            childCenterX, childTopY
                                        )
                                    }

                                    val isPathHighlighted = isPathMode && (localSelectedIds.contains(node.id) || localSelectedIds.contains(parentNode.id))

                                    val lineStyle = when {
                                        isPathMode -> Stroke(
                                            width = if (isPathHighlighted) 6f else 4f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                        )
                                        activeOverlay == MappingHeatmapOverlay.COMPLIANCE_REST && node.hasRestViolation -> Stroke(
                                            width = 4f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                        else -> Stroke(width = 3.5f)
                                    }

                                    val lineColor = when {
                                        isPathMode -> if (isPathHighlighted) Color(0xFFA855F7) else Color(0xFF38BDF8)
                                        activeOverlay == MappingHeatmapOverlay.COMPLIANCE_REST -> if (node.hasRestViolation) Color(0xFFEF4444) else Color(0xFF34D399)
                                        activeOverlay == MappingHeatmapOverlay.APPROVAL_CHAINS -> Color(0xFFA855F7)
                                        activeOverlay == MappingHeatmapOverlay.BURNOUT_OVERTIME -> if (node.weeklyHours > 50) Color(0xFFEC4899) else Color(0xFF34D399)
                                        activeOverlay == MappingHeatmapOverlay.ATTENDANCE_HEATMAP -> when (node.attendanceStatus) {
                                            "CLOCKED_IN_GEOFENCE" -> Color(0xFF10B981)
                                            "LATE_OUTSIDE_GEOFENCE" -> Color(0xFFEF4444)
                                            else -> Color(0xFFF59E0B)
                                        }
                                        else -> if (isLightMode) Color(0xFF0284C7) else Color(0xFF34D399)
                                    }

                                    drawPath(path = path, color = lineColor, style = lineStyle)
                                }
                            }
                        }
                    }

                    // ----------------------------------------------------
                    // DRAGGABLE NODE CARDS LAYER (SPATIALLY CULLED FOR 1,000 NODES)
                    // ----------------------------------------------------
                    visibleNodes.forEach { node ->
                        val origIndex = nodes.indexOfFirst { it.id == node.id }
                        if (origIndex != -1) {
                            DraggableOrgNodeCard(
                                node = node,
                                cardWidthDp = cardWidthDp,
                                activeOverlay = activeOverlay,
                                isSelectedInList = localSelectedIds.contains(node.id),
                                isEmergencySource = emergencyCoverSourceNode?.id == node.id,
                                isFrozen = isFrozen,
                                isLightMode = isLightMode,
                                onDragStarted = {
                                    historyStack.add(nodes)
                                    redoStack.clear()
                                },
                                onPositionChanged = { newOffset ->
                                    val updated = nodes.toMutableList()
                                    val delta = newOffset - node.position
                                    if (localSelectedIds.contains(node.id) && localSelectedIds.size > 1) {
                                        localSelectedIds.forEach { selId ->
                                            val idx = updated.indexOfFirst { it.id == selId }
                                            if (idx != -1) {
                                                val origPos = updated[idx].position
                                                updated[idx] = updated[idx].copy(position = Offset(origPos.x + delta.x, origPos.y + delta.y))
                                            }
                                        }
                                    } else {
                                        updated[origIndex] = updated[origIndex].copy(position = newOffset)
                                    }
                                    onNodesUpdated(updated)
                                },
                                onCardClick = { onNodeSelected(node) },
                                onCascadeShiftClick = {
                                    if (node.isManager) {
                                        onCascadeShiftToTeam(node.id, selectedShiftPreset)
                                    }
                                },
                                onEmergencyCoverClick = { onEmergencyCoverTriggered(node) }
                            )
                        }
                    }

                    // ----------------------------------------------------
                    // DEPARTMENT SECTOR HEADER BANNERS ON CANVAS
                    // ----------------------------------------------------
                    deptMetaMap.forEach { (deptName, meta) ->
                        val origin = meta.sectorOrigin
                        val deptNodeCount = remember(nodes) { nodes.count { it.department == deptName } }
                        if (deptNodeCount > 0 &&
                            origin.x + meta.sectorWidth >= visibleLeft && origin.x <= visibleRight &&
                            origin.y + 120f >= visibleTop && origin.y <= visibleBottom) {

                            val isSelectedDept = selectedDeptFilter == "ALL" || selectedDeptFilter == deptName
                            val alphaVal = if (isSelectedDept) 1.0f else 0.35f

                            Surface(
                                color = (if (isLightMode) Color.White else Color(0xFF0F1D17)).copy(alpha = 0.92f * alphaVal),
                                border = androidx.compose.foundation.BorderStroke(1.dp, meta.accentColor.copy(alpha = 0.6f * alphaVal)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .offset { IntOffset(origin.x.toInt() + 20, origin.y.toInt() + 15) }
                                    .clickable {
                                        selectedDeptFilter = deptName
                                        scale = 0.85f
                                        canvasOffset = Offset(
                                            -origin.x * 0.85f + viewportWidthPx / 2f - 350f,
                                            -origin.y * 0.85f + viewportHeightPx / 2f - 200f
                                        )
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, "Focused on ${deptName} Architecture", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(meta.icon, contentDescription = null, tint = meta.accentColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = deptName.uppercase(),
                                                color = if (isLightMode) Color.Black else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(meta.accentColor.copy(alpha = 0.2f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = meta.archTitle,
                                                    color = meta.accentColor,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = "$deptNodeCount Members • ${meta.description}",
                                            color = if (isLightMode) Color(0xFF64748B) else Color(0xFF94A3B8),
                                            fontSize = 8.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }



                // ----------------------------------------------------
                // SEARCH & JUMP BAR OVERLAY (Instantly center canvas on node in 1,000 Tree)
                // ----------------------------------------------------
                Surface(
                    color = (if (isLightMode) Color.White else Color(0xFF0F1D17)).copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF1B382B)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 10.dp, start = 10.dp)
                        .widthIn(max = 240.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = if (isLightMode) Color(0xFF047857) else NeonGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    showSearchResults = it.trim().length >= 2
                                },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = if (isLightMode) Color.Black else Color.White,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("canvas_search_node_input"),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text("Jump to Node / Emp...", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                    }
                                    innerTextField()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable {
                                            searchQuery = ""
                                            showSearchResults = false
                                        }
                                )
                            }
                        }

                        if (showSearchResults) {
                            val matches = remember(searchQuery, nodes) {
                                val q = searchQuery.trim().lowercase()
                                nodes.filter { it.name.lowercase().contains(q) || it.id.lowercase().contains(q) || it.department.lowercase().contains(q) }.take(6)
                            }
                            if (matches.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    matches.forEach { matchNode ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isLightMode) Color(0xFFF1F5F9) else Color(0xFF142A1F))
                                                .clickable {
                                                    scale = 1.0f
                                                    canvasOffset = Offset(
                                                        -matchNode.position.x * 1.0f + viewportWidthPx / 2f - 75f,
                                                        -matchNode.position.y * 1.0f + viewportHeightPx / 2f - 40f
                                                    )
                                                    onNodeSelected(matchNode)
                                                    showSearchResults = false
                                                    searchQuery = matchNode.name
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    Toast.makeText(context, "Jumped to ${matchNode.name}", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${matchNode.name} (${matchNode.id})",
                                                color = if (isLightMode) Color.Black else Color.White,
                                                fontSize = 9.5.sp,
                                                maxLines = 1,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ----------------------------------------------------
                // HUD STATS BADGE (Showing Active Node Count & Viewport Culling)
                // ----------------------------------------------------
                Surface(
                    color = (if (isLightMode) Color.White else Color(0xFF0F1D17)).copy(alpha = 0.90f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF1B382B)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (nodes.size >= 1000) Color(0xFF3B82F6) else NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${nodes.size} Nodes (${visibleNodes.size} in view)",
                            color = if (isLightMode) Color(0xFF0F172A) else Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ----------------------------------------------------
                // 3. HIERARCHY TEMPLATE PICKER POPUP
                // ----------------------------------------------------
                if (showTemplatePicker) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isLightMode) Color.White else Color(0xFF0D1B14)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isLightMode) Color(0xFF059669) else NeonGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 62.dp)
                            .shadow(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(0.92f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Select Hierarchy Template", color = if (isLightMode) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { showTemplatePicker = false }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(HierarchyTemplate.values()) { tmpl ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isLightMode) Color(0xFFE2E8F0) else Color(0xFF142A1F))
                                            .border(1.dp, if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF234B37), RoundedCornerShape(8.dp))
                                            .clickable {
                                                historyStack.add(nodes)
                                                redoStack.clear()
                                                val rearranged = applyHierarchyTemplate(nodes, tmpl)
                                                onNodesUpdated(rearranged)
                                                showTemplatePicker = false
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                Toast.makeText(context, "Applied ${tmpl.label}", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(tmpl.icon, contentDescription = null, tint = if (isLightMode) Color(0xFF059669) else NeonGreen, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(tmpl.label, color = if (isLightMode) Color.Black else Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ----------------------------------------------------
                // 4. FLOATING CANVAS ACTION TOOLS DOCK (FULL SCREEN MODE ONLY)
                // ----------------------------------------------------
                if (isFullScreenMode) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = dockBgColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, dockBorderColor),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 10.dp)
                            .shadow(12.dp, shape = RoundedCornerShape(20.dp))
                            .testTag("org_canvas_tools_dock")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // ---------------- GROUP 1: HISTORY ----------------
                            CanvasToolIconButton(
                                icon = Icons.Default.Undo,
                                label = "Undo",
                                enabled = historyStack.isNotEmpty(),
                                isLightMode = isLightMode,
                                onClick = {
                                    if (historyStack.isNotEmpty()) {
                                        val prev = historyStack.removeAt(historyStack.lastIndex)
                                        redoStack.add(nodes)
                                        onNodesUpdated(prev)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        Toast.makeText(context, "Move undone", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No moves to undo", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            CanvasToolIconButton(
                                icon = Icons.Default.Redo,
                                label = "Redo",
                                enabled = redoStack.isNotEmpty(),
                                isLightMode = isLightMode,
                                onClick = {
                                    if (redoStack.isNotEmpty()) {
                                        val next = redoStack.removeAt(redoStack.lastIndex)
                                        historyStack.add(nodes)
                                        onNodesUpdated(next)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        Toast.makeText(context, "Move redone", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No moves to redo", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // DIVIDER 1
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .height(1.dp)
                                    .background(if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF223E32))
                            )

                            // ---------------- GROUP 2: CANVAS TOOLS ----------------
                            CanvasToolIconButton(
                                icon = Icons.Default.HighlightAlt,
                                label = "Elastic Lasso",
                                isActive = isLassoMode,
                                isLightMode = isLightMode,
                                activeColor = NeonGreen,
                                onClick = {
                                    isLassoMode = !isLassoMode
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(
                                        context,
                                        if (isLassoMode) "Elastic Lasso ON: Drag surface to group-select cards!" else "Elastic Lasso OFF",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            CanvasToolIconButton(
                                icon = Icons.Default.Checklist,
                                label = "Multi Select",
                                isActive = isMultiSelectMode,
                                isLightMode = isLightMode,
                                activeColor = Color(0xFF3B82F6),
                                onClick = {
                                    isMultiSelectMode = !isMultiSelectMode
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, if (isMultiSelectMode) "Multi-Select Enabled" else "Multi-Select Disabled", Toast.LENGTH_SHORT).show()
                                }
                            )

                            CanvasToolIconButton(
                                icon = Icons.Default.AltRoute,
                                label = "Path",
                                isActive = isPathMode,
                                isLightMode = isLightMode,
                                activeColor = Color(0xFFA855F7),
                                onClick = {
                                    isPathMode = !isPathMode
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, if (isPathMode) "Path Mode: Directional routes highlighted" else "Path Mode Off", Toast.LENGTH_SHORT).show()
                                }
                            )

                            CanvasToolIconButton(
                                icon = Icons.Default.DashboardCustomize,
                                label = "Layouts",
                                isActive = showTemplatePicker,
                                isLightMode = isLightMode,
                                activeColor = Color(0xFF0EA5E9),
                                onClick = {
                                    showTemplatePicker = !showTemplatePicker
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )

                            CanvasToolIconButton(
                                icon = Icons.Default.RestartAlt,
                                label = "Reset View",
                                isLightMode = isLightMode,
                                onClick = {
                                    scale = 1.0f
                                    canvasOffset = Offset.Zero
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "Canvas View & Zoom Reset", Toast.LENGTH_SHORT).show()
                                }
                            )

                            CanvasToolIconButton(
                                icon = Icons.Default.Delete,
                                label = "Delete",
                                enabled = localSelectedIds.isNotEmpty(),
                                isLightMode = isLightMode,
                                activeColor = Color(0xFFEF4444),
                                onClick = {
                                    if (localSelectedIds.isNotEmpty()) {
                                        historyStack.add(nodes)
                                        redoStack.clear()
                                        val updated = nodes.filterNot { localSelectedIds.contains(it.id) }
                                        onNodesUpdated(updated)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, "Deleted ${localSelectedIds.size} node(s)", Toast.LENGTH_SHORT).show()
                                        localSelectedIds = emptySet()
                                    } else {
                                        Toast.makeText(context, "Select node(s) to delete", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // DIVIDER 2
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .height(1.dp)
                                    .background(if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF223E32))
                            )

                            // ---------------- GROUP 3: PRIMARY ACTIONS ----------------
                            CanvasToolIconButton(
                                icon = Icons.Default.PersonAdd,
                                label = "Add Employee",
                                isActive = true,
                                activeColor = if (isLightMode) Color(0xFF059669) else NeonGreen,
                                isLightMode = isLightMode,
                                onClick = {
                                    showAddEmployeeDialog = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )

                            CanvasToolIconButton(
                                icon = Icons.Default.Save,
                                label = "Save",
                                isLightMode = isLightMode,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "Org chart saved locally successfully!", Toast.LENGTH_SHORT).show()
                                }
                            )

                            CanvasToolIconButton(
                                icon = Icons.Default.SaveAs,
                                label = "Save as",
                                isLightMode = isLightMode,
                                onClick = {
                                    val filter = selectedDeptFilter
                                    val activeDept = if (filter.isNullOrBlank() || filter == "ALL") "All_Departments" else filter.replace(" ", "_")
                                    val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                                    customExportFileName = "${activeDept}_$currentDate"
                                    showSaveExportDialog = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )

                            CanvasToolIconButton(
                                icon = Icons.Default.RocketLaunch,
                                label = "Deploy",
                                isLightMode = isLightMode,
                                activeColor = NeonGreen,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "Deployed Org Hierarchy Structure to System!", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                }

                // ----------------------------------------------------
                // 5. FLOATING BOTTOM RIGHT CONTROLS (VERTICAL ZOOM PILL & FULLSCREEN)
                // ----------------------------------------------------
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = if (isFullScreenMode) 80.dp else 12.dp, end = 12.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Vertical Zoom Controls Pill (matching photo with ZoomIn & ZoomOut icons)
                    Surface(
                        color = Color(0xFF000000),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F4232)),
                        shadowElevation = 10.dp,
                        modifier = Modifier.testTag("org_canvas_bottom_right_zoom")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            // Zoom In Button
                            IconButton(
                                onClick = {
                                    scale = (scale + 0.2f).coerceAtMost(3.0f)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "Zoom In",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Horizontal Divider Line
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.35f))
                            )

                            // Zoom Out Button
                            IconButton(
                                onClick = {
                                    scale = (scale - 0.2f).coerceAtLeast(0.3f)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOut,
                                    contentDescription = "Zoom Out",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Floating Fullscreen Icon Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(if (isLightMode) Color(0xFF047857) else NeonGreen)
                            .clickable {
                                isFullScreenMode = !isFullScreenMode
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, if (isFullScreenMode) "Full Screen Mode Activated" else "Exited Full Screen", Toast.LENGTH_SHORT).show()
                            }
                            .testTag("org_canvas_fullscreen_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFullScreenMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullScreenMode) "Exit Full Screen" else "Full Screen",
                            tint = if (isLightMode) Color.White else Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

    if (isFullScreenMode) {
        Dialog(
            onDismissRequest = { isFullScreenMode = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = canvasBgColor
            ) {
                RenderCanvasCard()
            }
        }
    } else {
        RenderCanvasCard()
    }

    // ----------------------------------------------------
    // 5. ADD EMPLOYEE DIALOG POPUP
    // ----------------------------------------------------
    if (showAddEmployeeDialog) {
        val dialogBg = if (isLightMode) Color.White else Color(0xFF0F1D17)
        val dialogTextPrimary = if (isLightMode) Color(0xFF0F172A) else Color.White
        val dialogTextSecondary = if (isLightMode) Color(0xFF475569) else Color(0xFF94A3B8)

        AlertDialog(
            onDismissRequest = { showAddEmployeeDialog = false },
            containerColor = dialogBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = if (isLightMode) Color(0xFF059669) else NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Employee to Hierarchy",
                        color = dialogTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Input name or ID number to add a new employee card into the reporting tree.",
                        color = dialogTextSecondary,
                        fontSize = 11.sp
                    )

                    // Input Field: Employee Name or ID Number
                    OutlinedTextField(
                        value = newEmpNameOrId,
                        onValueChange = { newEmpNameOrId = it },
                        label = { Text("Name or ID Number") },
                        placeholder = { Text("e.g. EMP-204 or Sarah Connor") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_employee_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isLightMode) Color(0xFF059669) else NeonGreen,
                            unfocusedBorderColor = if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF234B37),
                            focusedTextColor = dialogTextPrimary,
                            unfocusedTextColor = dialogTextPrimary
                        )
                    )

                    // Role Selector
                    Text("Select Organizational Role:", color = dialogTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("EMPLOYEE", "SUPERVISOR", "MANAGER").forEach { roleOption ->
                            val isSelected = newEmpRole == roleOption
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) (if (isLightMode) Color(0xFF059669) else NeonGreen) else (if (isLightMode) Color(0xFFE2E8F0) else Color(0xFF142A1F)))
                                    .clickable { newEmpRole = roleOption }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = roleOption,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else dialogTextPrimary
                                )
                            }
                        }
                    }

                    // Assign Supervisor / Parent Node
                    Text("Assign Reporting Supervisor:", color = dialogTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            val isSelected = selectedSupervisorId == null
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF3B82F6) else (if (isLightMode) Color(0xFFE2E8F0) else Color(0xFF142A1F)))
                                    .clickable { selectedSupervisorId = null }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("No Supervisor (Top)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else dialogTextPrimary)
                            }
                        }

                        items(nodes.filter { it.isManager || it.role == "SUPERVISOR" || it.role == "GENERAL_MANAGER" }) { supNode ->
                            val isSelected = selectedSupervisorId == supNode.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF3B82F6) else (if (isLightMode) Color(0xFFE2E8F0) else Color(0xFF142A1F)))
                                    .clickable { selectedSupervisorId = supNode.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(supNode.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else dialogTextPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEmpNameOrId.isBlank()) {
                            Toast.makeText(context, "Please input Employee Name or ID number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        historyStack.add(nodes)
                        redoStack.clear()

                        val generatedId = "NODE-ADD-${System.currentTimeMillis().toString().takeLast(4)}"
                        val spawnX = 20f + (nodes.size % 3) * 170f
                        val spawnY = 40f + (nodes.size / 3) * 150f

                        val newNode = OrgNode(
                            id = generatedId,
                            name = newEmpNameOrId.trim(),
                            role = if (newEmpRole == "MANAGER") "GENERAL_MANAGER" else if (newEmpRole == "SUPERVISOR") "SUPERVISOR" else "EMPLOYEE",
                            department = "Operations",
                            position = Offset(spawnX, spawnY),
                            parentId = selectedSupervisorId,
                            weeklyHours = 40.0,
                            assignedShift = selectedShiftPreset
                        )

                        onNodesUpdated(nodes + newNode)
                        showAddEmployeeDialog = false
                        newEmpNameOrId = ""
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(context, "Added '${newNode.name}' to hierarchy!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isLightMode) Color(0xFF059669) else NeonGreen)
                ) {
                    Text("Add to Hierarchy", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEmployeeDialog = false }) {
                    Text("Cancel", color = dialogTextSecondary, fontSize = 11.sp)
                }
            }
        )
    }

    // ----------------------------------------------------
    // 5.1 SAVE & EXPORT ORG MAPPER DIALOG (SVG, PNG, JPEG, PDF)
    // ----------------------------------------------------
    if (showSaveExportDialog) {
        val dialogBg = if (isLightMode) Color.White else Color(0xFF0F1D17)
        val dialogTextPrimary = if (isLightMode) Color(0xFF0F172A) else Color.White
        val dialogTextSecondary = if (isLightMode) Color(0xFF475569) else Color(0xFF94A3B8)

        val activeDeptName = remember(selectedDeptFilter) {
            val filter = selectedDeptFilter
            if (filter.isNullOrBlank() || filter == "ALL") "All_Departments" else filter.replace(" ", "_")
        }
        val currentDateStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) }

        AlertDialog(
            onDismissRequest = { showSaveExportDialog = false },
            containerColor = dialogBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = "Save Org Chart",
                        tint = if (isLightMode) Color(0xFF059669) else NeonGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save & Export Org Mapper",
                        color = dialogTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Export org chart as SVG, XML, PNG, JPEG, or PDF format formatted as (DepartmentName_DateEdited):",
                        color = dialogTextSecondary,
                        fontSize = 11.sp
                    )

                    // Export Format Selection Cards
                    Text("Select Export Format:", color = dialogTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val formats = listOf("SVG", "XML", "PNG", "JPEG", "PDF")
                        formats.forEach { fmt ->
                            val isSelected = selectedExportFormat == fmt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) (if (isLightMode) Color(0xFFD1FAE5) else Color(0xFF064E3B))
                                        else (if (isLightMode) Color(0xFFF1F5F9) else Color(0xFF13281E))
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) (if (isLightMode) Color(0xFF059669) else NeonGreen)
                                        else (if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF204533)),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedExportFormat = fmt
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = fmt,
                                        color = if (isSelected) (if (isLightMode) Color(0xFF065F46) else NeonGreen) else dialogTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when(fmt) {
                                            "SVG" -> "Vector"
                                            "XML" -> "Schema"
                                            "PNG" -> "High Res"
                                            "JPEG" -> "Image"
                                            else -> "Doc"
                                        },
                                        color = dialogTextSecondary,
                                        fontSize = 8.5.sp
                                    )
                                }
                            }
                        }
                    }

                    // Custom File Name Input
                    OutlinedTextField(
                        value = customExportFileName,
                        onValueChange = { customExportFileName = it },
                        label = { Text("File Name (DepartmentName_DateEdited)") },
                        trailingIcon = {
                            Text(
                                ".${selectedExportFormat.lowercase()}",
                                color = if (isLightMode) Color(0xFF059669) else NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("export_filename_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isLightMode) Color(0xFF059669) else NeonGreen,
                            unfocusedBorderColor = if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF234B37),
                            focusedTextColor = dialogTextPrimary,
                            unfocusedTextColor = dialogTextPrimary
                        )
                    )

                    // Helper badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Format: (DepartmentName_DateEdited)",
                            color = dialogTextSecondary,
                            fontSize = 10.sp
                        )
                        TextButton(
                            onClick = {
                                customExportFileName = "${activeDeptName}_$currentDateStr"
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Reset Default",
                                color = if (isLightMode) Color(0xFF0284C7) else NeonGreen,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveExportDialog = false
                        val finalFileName = "${customExportFileName}.${selectedExportFormat.lowercase()}"
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(context, "Saved & Exported '$finalFileName' successfully!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLightMode) Color(0xFF059669) else NeonGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveExportDialog = false }) {
                    Text("Cancel", color = dialogTextSecondary, fontSize = 12.sp)
                }
            }
        )
    }

    // ----------------------------------------------------
    // 6. ADD DEPARTMENT ORG CANVAS DIALOG POPUP
    // ----------------------------------------------------
    if (showAddCanvasDialog) {
        Dialog(onDismissRequest = { showAddCanvasDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isLightMode) Color.White else Color(0xFF0F1D17),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonGreen),
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DomainAdd, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Department Org Canvas", color = if (isLightMode) Color.Black else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { showAddCanvasDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }

                    Text("Create a dedicated canvas with custom architecture topology for your department.", color = Color.Gray, fontSize = 11.sp)

                    // 1. Department Name
                    Text("DEPARTMENT NAME", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newDeptCanvasName,
                        onValueChange = { newDeptCanvasName = it },
                        placeholder = { Text("e.g. Cyber Security & SOC", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. Director Name
                    Text("DEPARTMENT DIRECTOR / HEAD", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newDeptDirectorName,
                        onValueChange = { newDeptDirectorName = it },
                        placeholder = { Text("e.g. Dr. Helena Vance, CISO", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Topology Preset Selector
                    Text("ARCHITECTURE TOPOLOGY PRESET", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val presets = listOf(
                            "Agile Squad Matrix",
                            "Hub & Spoke Fleet Radial",
                            "Command Center Grid",
                            "Tiered Escalation Ladder",
                            "Horizontal Pipeline Funnel",
                            "Creative Pod Cluster",
                            "Pyramid Compliance Tree",
                            "SecOps SOC Tactical Hub"
                        )
                        items(presets) { preset ->
                            val isSel = selectedArchPreset == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) NeonGreen else (if (isLightMode) Color(0xFFE2E8F0) else Color(0xFF13261D)))
                                    .clickable { selectedArchPreset = preset }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = preset,
                                    color = if (isSel) Color.Black else (if (isLightMode) Color.Black else Color.White),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // 4. Accent Color Selector
                    Text("CANVAS ACCENT COLOR", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val colorOptions = listOf(
                            Color(0xFF00E5FF), Color(0xFF10B981), Color(0xFFF59E0B),
                            Color(0xFFA855F7), Color(0xFFF97316), Color(0xFFEC4899),
                            Color(0xFF14B8A6), Color(0xFF6366F1), Color(0xFFEF4444), Color(0xFFEAB308)
                        )
                        items(colorOptions) { color ->
                            val isSel = selectedColorHex == color
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSel) 3.dp else 0.dp,
                                        color = if (isSel) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = color }
                            )
                        }
                    }

                    // 5. Description / Mission
                    Text("DEPARTMENT MISSION / DESCRIPTION", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newDeptMission,
                        onValueChange = { newDeptMission = it },
                        placeholder = { Text("e.g. Dedicated SOC cyber defense & threat intelligence division.", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val deptName = newDeptCanvasName.ifBlank { "Dept ${deptMetaMap.size + 1}" }
                            val index = deptMetaMap.size
                            val newOrigin = Offset(100f + (index % 5) * 1050f, 320f + (index / 5) * 1500f)

                            val newMeta = DepartmentArchMeta(
                                deptName = deptName,
                                archTitle = selectedArchPreset,
                                icon = when (selectedArchPreset) {
                                    "Agile Squad Matrix" -> Icons.Default.Terminal
                                    "Hub & Spoke Fleet Radial" -> Icons.Default.LocalShipping
                                    "Command Center Grid" -> Icons.Default.PrecisionManufacturing
                                    "Tiered Escalation Ladder" -> Icons.Default.SupportAgent
                                    "Horizontal Pipeline Funnel" -> Icons.Default.TrendingUp
                                    "Creative Pod Cluster" -> Icons.Default.Campaign
                                    "Pyramid Compliance Tree" -> Icons.Default.Groups
                                    "SecOps SOC Tactical Hub" -> Icons.Default.Security
                                    else -> Icons.Default.Domain
                                },
                                accentColor = selectedColorHex,
                                description = newDeptMission.ifBlank { "Department org architecture canvas" },
                                sectorOrigin = newOrigin
                            )

                            deptMetaMap[deptName] = newMeta

                            val newDeptNodes = createInitialNodesForNewDepartment(
                                deptName = deptName,
                                directorName = newDeptDirectorName,
                                origin = newOrigin
                            )

                            val updatedAllNodes = applyPerDepartmentArchitectures(nodes + newDeptNodes, deptMetaMap)
                            onNodesUpdated(updatedAllNodes)

                            selectedDeptFilter = deptName
                            showAddCanvasDialog = false
                            newDeptCanvasName = ""
                            newDeptDirectorName = ""
                            newDeptMission = ""

                            Toast.makeText(context, "Created $deptName Org Canvas!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Org Canvas", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Reusable Canvas Tool Icon Button - Icon Only with Long-Press Tool Name Pop-up Badge
 */
@Composable
fun CanvasToolIconButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    isActive: Boolean = false,
    isLightMode: Boolean = false,
    activeColor: Color = NeonGreen,
    onClick: () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val buttonBg = if (!enabled) {
        if (isLightMode) Color(0xFFF1F5F9).copy(alpha = 0.5f) else Color(0xFF0C1711)
    } else if (isActive) {
        activeColor.copy(alpha = 0.25f)
    } else {
        if (isLightMode) Color(0xFFF1F5F9) else Color(0xFF102118)
    }
    
    val buttonBorder = if (isActive) activeColor else if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF1B382B)
    val iconColor = if (!enabled) {
        Color(0xFF64748B)
    } else if (isActive) {
        activeColor
    } else {
        if (isLightMode) Color(0xFF0F172A) else Color.White
    }

    Box(
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(buttonBg)
                .border(1.dp, buttonBorder, RoundedCornerShape(10.dp))
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showTooltip = true
                            coroutineScope.launch {
                                delay(2200)
                                showTooltip = false
                            }
                        },
                        onPress = {
                            val released = try {
                                awaitRelease()
                                true
                            } catch (e: Exception) {
                                false
                            }
                            if (released && showTooltip) {
                                delay(1200)
                                showTooltip = false
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // Pop-up tool name badge when user holds the tool
        AnimatedVisibility(
            visible = showTooltip,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
            modifier = Modifier.padding(start = 46.dp)
        ) {
            Surface(
                color = if (isLightMode) Color(0xFF0F172A) else Color(0xFF132A1F),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) activeColor else NeonGreen),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = label,
                    color = if (isLightMode) Color.White else NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Draggable Node Card Composable styled with M3 design specs & Haptic Feedback
 */
@Composable
fun DraggableOrgNodeCard(
    node: OrgNode,
    cardWidthDp: androidx.compose.ui.unit.Dp,
    activeOverlay: MappingHeatmapOverlay,
    isSelectedInList: Boolean,
    isEmergencySource: Boolean,
    isFrozen: Boolean = false,
    isLightMode: Boolean = false,
    onDragStarted: () -> Unit = {},
    onPositionChanged: (Offset) -> Unit,
    onCardClick: () -> Unit,
    onCascadeShiftClick: () -> Unit,
    onEmergencyCoverClick: () -> Unit
) {
    var offsetX by remember { mutableStateOf(node.position.x) }
    var offsetY by remember { mutableStateOf(node.position.y) }
    var isDragging by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(node.position) {
        offsetX = node.position.x
        offsetY = node.position.y
    }

    // Subtle scale animation & elevated shadow on selection / drag
    val animatedScale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.08f
            isSelectedInList -> 1.04f
            else -> 1.0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animatedScale"
    )

    val animatedElevation by animateDpAsState(
        targetValue = when {
            isDragging -> 18.dp
            isSelectedInList -> 12.dp
            isEmergencySource -> 8.dp
            else -> 2.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animatedElevation"
    )

    val borderColor = when {
        isEmergencySource -> Color(0xFFEF4444)
        isDragging -> NeonGreen
        isSelectedInList -> if (isLightMode) Color(0xFF0284C7) else NeonGreen
        activeOverlay == MappingHeatmapOverlay.COMPLIANCE_REST && node.hasRestViolation -> Color(0xFFEF4444)
        activeOverlay == MappingHeatmapOverlay.BURNOUT_OVERTIME -> when {
            node.weeklyHours > 50 -> Color(0xFFEC4899)
            node.weeklyHours > 40 -> Color(0xFFF59E0B)
            else -> if (isLightMode) Color(0xFF059669) else NeonGreen
        }
        activeOverlay == MappingHeatmapOverlay.ATTENDANCE_HEATMAP -> when (node.attendanceStatus) {
            "CLOCKED_IN_GEOFENCE" -> Color(0xFF10B981)
            "LATE_OUTSIDE_GEOFENCE" -> Color(0xFFEF4444)
            else -> Color(0xFFF59E0B)
        }
        node.role == "GENERAL_MANAGER" -> Color(0xFFA855F7)
        node.role == "SUPERVISOR" -> if (isLightMode) Color(0xFF059669) else NeonGreen
        else -> if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF223E32)
    }

    val containerBg = if (isLightMode) {
        when (node.role) {
            "GENERAL_MANAGER" -> Color(0xFFF3E8FF)
            "SUPERVISOR" -> Color(0xFFDCFCE7)
            else -> Color(0xFFFFFFFF)
        }
    } else {
        when (node.role) {
            "GENERAL_MANAGER" -> Color(0xFF1E102A)
            "SUPERVISOR" -> Color(0xFF0D241B)
            else -> Color(0xFF101B17)
        }
    }

    val textColorPrimary = if (isLightMode) Color(0xFF0F172A) else Color.White
    val textColorSecondary = if (isLightMode) Color(0xFF475569) else Color(0xFF94A3B8)

    Card(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .shadow(animatedElevation, shape = RoundedCornerShape(10.dp))
            .pointerInput(node.id, isFrozen, isSelectedInList) {
                if (!isFrozen) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDragStarted()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val rawX = (offsetX + dragAmount.x).coerceIn(5f, 12000f)
                            val rawY = (offsetY + dragAmount.y).coerceIn(5f, 18000f)
                            // Snap-to-grid alignment system (25dp grid)
                            val gridSize = 25f
                            val snappedX = kotlin.math.round(rawX / gridSize) * gridSize
                            val snappedY = kotlin.math.round(rawY / gridSize) * gridSize
                            offsetX = snappedX
                            offsetY = snappedY
                            onPositionChanged(Offset(snappedX, snappedY))
                        },
                        onDragEnd = {
                            isDragging = false
                            val gridSize = 25f
                            val snappedX = kotlin.math.round(offsetX / gridSize) * gridSize
                            val snappedY = kotlin.math.round(offsetY / gridSize) * gridSize
                            offsetX = snappedX
                            offsetY = snappedY
                            onPositionChanged(Offset(snappedX, snappedY))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragCancel = {
                            isDragging = false
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
            }
            .clickable { onCardClick() }
            .width(cardWidthDp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = androidx.compose.foundation.BorderStroke(
            if (isDragging || isSelectedInList || isEmergencySource) 2.dp else 1.dp,
            if (isDragging) NeonGreen else borderColor
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(borderColor.copy(alpha = 0.2f))
                        .border(1.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (node.role) {
                            "GENERAL_MANAGER" -> Icons.Default.Stars
                            "SUPERVISOR" -> Icons.Default.SupervisorAccount
                            else -> Icons.Default.Person
                        },
                        contentDescription = null,
                        tint = borderColor,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.name,
                        color = textColorPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = node.role.replace("_", " "),
                        color = textColorSecondary,
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Context Badges without Emojis
            when (activeOverlay) {
                MappingHeatmapOverlay.CASCADE_SHIFTS -> {
                    Text(
                        text = node.assignedShift,
                        color = Color(0xFF93C5FD),
                        fontSize = 8.5.sp,
                        maxLines = 1
                    )
                    if (node.isManager) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1D4ED8))
                                .clickable { onCascadeShiftClick() }
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Cascade", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                MappingHeatmapOverlay.ATTENDANCE_HEATMAP -> {
                    val (statusLabel, statusIcon, statusColor) = when (node.attendanceStatus) {
                        "CLOCKED_IN_GEOFENCE" -> Triple("In Geofence", Icons.Default.CheckCircle, Color(0xFF10B981))
                        "LATE_OUTSIDE_GEOFENCE" -> Triple("Late / Outside", Icons.Default.ErrorOutline, Color(0xFFEF4444))
                        "ON_BREAK" -> Triple("On Break", Icons.Default.Timer, Color(0xFFF59E0B))
                        else -> Triple("Off Duty", Icons.Default.Bedtime, Color(0xFF94A3B8))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = statusLabel, color = statusColor, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }

                    if (node.attendanceStatus == "LATE_OUTSIDE_GEOFENCE" || node.attendanceStatus == "OFF_DUTY") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isEmergencySource) Color(0xFFEF4444) else Color(0xFF374151))
                                .clickable { onEmergencyCoverClick() }
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isEmergencySource) Icons.Default.Cancel else Icons.Default.CrisisAlert,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isEmergencySource) "Cancel" else "Cover Alert",
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                MappingHeatmapOverlay.APPROVAL_CHAINS -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Tier ${node.approvalTier} Approver",
                            color = Color(0xFFA855F7),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                MappingHeatmapOverlay.BURNOUT_OVERTIME -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = if (node.weeklyHours > 50) Color(0xFFEC4899) else Color(0xFF94A3B8),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${node.weeklyHours}h / week",
                            color = if (node.weeklyHours > 50) Color(0xFFEC4899) else Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                MappingHeatmapOverlay.COMPLIANCE_REST -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (node.hasRestViolation) Icons.Default.Warning else Icons.Default.Verified,
                            contentDescription = null,
                            tint = if (node.hasRestViolation) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (node.hasRestViolation) "Rest Warning (${node.restPeriodHours}h)" else "Rest OK (${node.restPeriodHours}h)",
                            color = if (node.hasRestViolation) Color(0xFFEF4444) else Color(0xFF10B981),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                else -> {
                    Text(
                        text = node.department,
                        color = Color(0xFF94A3B8),
                        fontSize = 8.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * SEARCHABLE MULTI-SELECT PRECISION LIST VIEW
 */
@Composable
fun SearchableAssignListView(
    nodes: List<OrgNode>,
    selectedEmployeeIds: Set<String>,
    selectedShiftPreset: String,
    onSelectionChanged: (Set<String>) -> Unit,
    onShiftPresetChanged: (String) -> Unit,
    onBatchReassignSupervisor: (supervisorId: String) -> Unit,
    onBatchApplyShift: (shiftName: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var deptFilter by remember { mutableStateOf("All") }
    var targetSupervisorId by remember { mutableStateOf(nodes.firstOrNull { it.role == "SUPERVISOR" }?.id ?: "") }

    val filteredList = nodes.filter { node ->
        (deptFilter == "All" || node.department.equals(deptFilter, ignoreCase = true)) &&
        (node.name.contains(searchQuery, ignoreCase = true) || node.id.contains(searchQuery, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .padding(bottom = 70.dp) // Leave space for bottom nav
            .testTag("searchable_multi_select_list_view")
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search employee or node ID...", color = Color(0xFF94A3B8), fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("org_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F1A15),
                unfocusedContainerColor = Color(0xFF0F1A15),
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = Color(0xFF223E32),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Selection Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredList.size} Nodes (${selectedEmployeeIds.size} Selected)",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = {
                    val allIds = filteredList.map { it.id }.toSet()
                    onSelectionChanged(if (selectedEmployeeIds.containsAll(allIds)) emptySet() else allIds)
                }
            ) {
                Text(
                    text = if (selectedEmployeeIds.containsAll(filteredList.map { it.id })) "Deselect All" else "Select All",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Node Cards List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredList, key = { it.id }) { node ->
                val isSelected = selectedEmployeeIds.contains(node.id)
                val currentManager = nodes.find { it.id == node.parentId }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectionChanged(
                                if (isSelected) selectedEmployeeIds - node.id else selectedEmployeeIds + node.id
                            )
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF0E281E) else Color(0xFF0F1A15)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) NeonGreen else Color(0xFF223E32)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(node.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (node.isManager) NeonGreen.copy(alpha = 0.2f) else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = node.role,
                                        color = if (node.isManager) NeonGreen else Color(0xFF94A3B8),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Supervisor: ${currentManager?.name ?: "Executive"} • Shift: ${node.assignedShift}",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }

                        // Checkbox Indicator
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) NeonGreen else Color.Transparent)
                                .border(1.5.dp, if (isSelected) NeonGreen else Color(0xFF94A3B8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF070E0B), modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // Batch Action Control Card
        if (selectedEmployeeIds.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13231C)),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Batch Actions for ${selectedEmployeeIds.size} Employees",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onBatchReassignSupervisor(targetSupervisorId) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reassign Manager", color = Color(0xFF070E0B), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onBatchApplyShift(selectedShiftPreset) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Apply Shift Batch", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
