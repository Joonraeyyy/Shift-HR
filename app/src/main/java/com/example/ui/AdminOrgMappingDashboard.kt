package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.roundToInt

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
            position = Offset(110f, 15f),
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
            position = Offset(15f, 130f),
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
            position = Offset(195f, 130f),
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
            position = Offset(15f, 250f),
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
            position = Offset(195f, 250f),
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
            position = Offset(15f, 370f),
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
            position = Offset(195f, 370f),
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
                            if (selectedEmployeeIds.isEmpty()) {
                                Toast.makeText(context, "Select at least 1 employee first", Toast.LENGTH_SHORT).show()
                                return@SearchableAssignListView
                            }
                            val supNode = nodeList.find { it.id == supervisorId }
                            nodeList = nodeList.map { n ->
                                if (selectedEmployeeIds.contains(n.id)) {
                                    n.copy(parentId = supervisorId)
                                } else n
                            }
                            Toast.makeText(context, "Reassigned ${selectedEmployeeIds.size} employees to Supervisor ${supNode?.name}", Toast.LENGTH_SHORT).show()
                        },
                        onBatchApplyShift = { shift ->
                            if (selectedEmployeeIds.isEmpty()) {
                                Toast.makeText(context, "Select at least 1 employee first", Toast.LENGTH_SHORT).show()
                                return@SearchableAssignListView
                            }
                            nodeList = nodeList.map { n ->
                                if (selectedEmployeeIds.contains(n.id)) {
                                    n.copy(assignedShift = shift)
                                } else n
                            }
                            Toast.makeText(context, "Batch assigned shift to ${selectedEmployeeIds.size} employees", Toast.LENGTH_SHORT).show()
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
 * DRAG & DROP CANVAS VIEW WITH ACCURATE BEZIER CURVE CONNECTORS, ZOOM & HAPTICS
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
    val density = LocalDensity.current
    val cardWidthDp = 150.dp
    val cardWidthPx = with(density) { cardWidthDp.toPx() }

    var scale by remember { mutableFloatStateOf(1.0f) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070E0B))
            .padding(bottom = 80.dp) // Clear bottom navigation bar and floating buttons
            .testTag("interactive_org_canvas_view")
    ) {
        // Transformable Canvas Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 2.5f)
                        canvasOffset += pan
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = canvasOffset.x
                    translationY = canvasOffset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        ) {
            // ----------------------------------------------------
            // 1. CANVAS LAYER: Draw Dynamic Bezier Curves
            // ----------------------------------------------------
            Canvas(modifier = Modifier.fillMaxSize()) {
                nodes.forEach { node ->
                    node.parentId?.let { pId ->
                        val parentNode = nodes.find { it.id == pId }
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

                            val lineStyle = if (activeOverlay == MappingHeatmapOverlay.COMPLIANCE_REST && node.hasRestViolation) {
                                Stroke(
                                    width = 4f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            } else {
                                Stroke(width = 3.5f)
                            }

                            val lineColor = when (activeOverlay) {
                                MappingHeatmapOverlay.COMPLIANCE_REST -> if (node.hasRestViolation) Color(0xFFEF4444) else Color(0xFF34D399)
                                MappingHeatmapOverlay.APPROVAL_CHAINS -> Color(0xFFA855F7)
                                MappingHeatmapOverlay.BURNOUT_OVERTIME -> if (node.weeklyHours > 50) Color(0xFFEC4899) else Color(0xFF34D399)
                                MappingHeatmapOverlay.ATTENDANCE_HEATMAP -> when (node.attendanceStatus) {
                                    "CLOCKED_IN_GEOFENCE" -> Color(0xFF10B981)
                                    "LATE_OUTSIDE_GEOFENCE" -> Color(0xFFEF4444)
                                    else -> Color(0xFFF59E0B)
                                }
                                else -> Color(0xFF34D399)
                            }

                            drawPath(path = path, color = lineColor, style = lineStyle)
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 2. DRAGGABLE NODE CARDS LAYER
            // ----------------------------------------------------
            nodes.forEachIndexed { index, node ->
                DraggableOrgNodeCard(
                    node = node,
                    cardWidthDp = cardWidthDp,
                    activeOverlay = activeOverlay,
                    isSelectedInList = selectedEmployeeIds.contains(node.id),
                    isEmergencySource = emergencyCoverSourceNode?.id == node.id,
                    onPositionChanged = { newOffset ->
                        val updated = nodes.toMutableList()
                        updated[index] = updated[index].copy(position = newOffset)
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
        // 3. FLOATING ZOOM CONTROLS (ZOOM IN / ZOOM OUT / RESET)
        // ----------------------------------------------------
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A15).copy(alpha = 0.92f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223E32)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .shadow(6.dp, shape = RoundedCornerShape(20.dp))
                .testTag("org_canvas_zoom_controls")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        scale = (scale - 0.2f).coerceAtLeast(0.5f)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(14.dp))
                }

                Text(
                    text = "${(scale * 100).roundToInt()}%",
                    color = NeonGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                IconButton(
                    onClick = {
                        scale = (scale + 0.2f).coerceAtMost(2.5f)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(14.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF13231C))
                        .clickable {
                            scale = 1.0f
                            canvasOffset = Offset.Zero
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("Reset", color = Color(0xFF94A3B8), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
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
    onPositionChanged: (Offset) -> Unit,
    onCardClick: () -> Unit,
    onCascadeShiftClick: () -> Unit,
    onEmergencyCoverClick: () -> Unit
) {
    var offsetX by remember { mutableStateOf(node.position.x) }
    var offsetY by remember { mutableStateOf(node.position.y) }
    val haptic = LocalHapticFeedback.current

    val borderColor = when {
        isEmergencySource -> Color(0xFFEF4444)
        isSelectedInList -> NeonGreen
        activeOverlay == MappingHeatmapOverlay.COMPLIANCE_REST && node.hasRestViolation -> Color(0xFFEF4444)
        activeOverlay == MappingHeatmapOverlay.BURNOUT_OVERTIME -> when {
            node.weeklyHours > 50 -> Color(0xFFEC4899)
            node.weeklyHours > 40 -> Color(0xFFF59E0B)
            else -> NeonGreen
        }
        activeOverlay == MappingHeatmapOverlay.ATTENDANCE_HEATMAP -> when (node.attendanceStatus) {
            "CLOCKED_IN_GEOFENCE" -> Color(0xFF10B981)
            "LATE_OUTSIDE_GEOFENCE" -> Color(0xFFEF4444)
            else -> Color(0xFFF59E0B)
        }
        node.role == "GENERAL_MANAGER" -> Color(0xFFA855F7)
        node.role == "SUPERVISOR" -> NeonGreen
        else -> Color(0xFF223E32)
    }

    val containerBg = when (node.role) {
        "GENERAL_MANAGER" -> Color(0xFF1E102A)
        "SUPERVISOR" -> Color(0xFF0D241B)
        else -> Color(0xFF101B17)
    }

    Card(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(node.id) {
                detectDragGestures(
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(-100f, 1200f)
                        offsetY = (offsetY + dragAmount.y).coerceIn(-100f, 1800f)
                        onPositionChanged(Offset(offsetX, offsetY))
                    },
                    onDragEnd = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragCancel = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }
            .clickable { onCardClick() }
            .width(cardWidthDp)
            .shadow(if (isSelectedInList || isEmergencySource) 8.dp else 2.dp, shape = RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = androidx.compose.foundation.BorderStroke(if (isSelectedInList || isEmergencySource) 2.dp else 1.dp, borderColor),
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
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = node.role.replace("_", " "),
                        color = Color(0xFF94A3B8),
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
