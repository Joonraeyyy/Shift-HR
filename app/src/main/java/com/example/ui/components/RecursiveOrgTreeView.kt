package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.OrgNodeEntity

// ----------------------------------------------------
// RECURSIVE TREE DATA MODEL
// ----------------------------------------------------
data class OrgTreeNode(
    val entity: OrgNodeEntity,
    val children: List<OrgTreeNode> = emptyList()
) {
    val totalSubtreeCount: Int
        get() = children.sumOf { 1 + it.totalSubtreeCount }
}

// Helper to construct a recursive tree from flat Room DB records
fun buildOrgTree(flatNodes: List<OrgNodeEntity>): List<OrgTreeNode> {
    if (flatNodes.isEmpty()) return emptyList()

    val nodeMap = flatNodes.associateBy { it.id }
    val childrenMap = mutableMapOf<String?, MutableList<OrgNodeEntity>>()

    flatNodes.forEach { node ->
        // If parentId doesn't exist in map, treat as root
        val parentKey = if (node.parentId != null && nodeMap.containsKey(node.parentId)) node.parentId else null
        childrenMap.getOrPut(parentKey) { mutableListOf() }.add(node)
    }

    fun buildSubtree(node: OrgNodeEntity): OrgTreeNode {
        val childEntities = childrenMap[node.id] ?: emptyList()
        val childNodes = childEntities.map { buildSubtree(it) }
        return OrgTreeNode(entity = node, children = childNodes)
    }

    val rootEntities = childrenMap[null] ?: emptyList()
    return rootEntities.map { buildSubtree(it) }
}

// ----------------------------------------------------
// MAIN RECURSIVE ROOM DB TREE VIEW COMPONENT
// ----------------------------------------------------
@Composable
fun RecursiveOrgTreeView(
    nodes: List<OrgNodeEntity>,
    isLightMode: Boolean,
    onNodeSelect: (OrgNodeEntity) -> Unit = {},
    onAddNodeClick: () -> Unit = {},
    onRefreshDb: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDeptFilter by remember { mutableStateOf("ALL") }

    // State for expanded tree node IDs (all expanded by default)
    var expandedNodeIds by remember(nodes) {
        mutableStateOf(nodes.map { it.id }.toSet())
    }

    // Filter nodes by department or search
    val filteredNodes = remember(nodes, searchQuery, selectedDeptFilter) {
        nodes.filter { node ->
            val matchesSearch = searchQuery.isBlank() ||
                    node.name.contains(searchQuery, ignoreCase = true) ||
                    node.department.contains(searchQuery, ignoreCase = true) ||
                    node.role.contains(searchQuery, ignoreCase = true)

            val matchesDept = selectedDeptFilter == "ALL" || node.department == selectedDeptFilter
            matchesSearch && matchesDept
        }
    }

    // Build recursive tree structure
    val treeRoots = remember(filteredNodes) {
        buildOrgTree(filteredNodes)
    }

    val departments = remember(nodes) {
        listOf("ALL") + nodes.map { it.department }.distinct()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = if (isLightMode) Color(0xFFF1F5F9) else Color(0xFF0D1117),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF30363D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CorporateFare,
                        contentDescription = "Org Hierarchy",
                        tint = if (isLightMode) Color(0xFF059669) else Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Organization Hierarchy Tree",
                        color = if (isLightMode) Color(0xFF0F172A) else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onRefreshDb,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Data",
                            tint = if (isLightMode) Color(0xFF64748B) else Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onAddNodeClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Member",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar & Filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search employees or departments...",
                        fontSize = 12.5.sp,
                        color = if (isLightMode) Color(0xFF64748B) else Color(0xFF9CA3AF)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isLightMode) Color(0xFF2563EB) else Color(0xFF60A5FA),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = if (isLightMode) Color(0xFF64748B) else Color(0xFF9CA3AF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isLightMode) Color.White else Color(0xFF161B22),
                    unfocusedContainerColor = if (isLightMode) Color.White else Color(0xFF161B22),
                    focusedBorderColor = if (isLightMode) Color(0xFF3B82F6) else Color(0xFF58A6FF),
                    unfocusedBorderColor = if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF30363D),
                    focusedTextColor = if (isLightMode) Color(0xFF0F172A) else Color.White,
                    unfocusedTextColor = if (isLightMode) Color(0xFF0F172A) else Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // High-Contrast Stats & Control Ribbon
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isLightMode) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${nodes.size} Members in Hierarchy",
                            fontSize = 12.5.sp,
                            color = if (isLightMode) Color(0xFF0F172A) else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { expandedNodeIds = nodes.map { it.id }.toSet() },
                            color = if (isLightMode) Color(0xFF2563EB) else Color(0xFF3B82F6)
                        ) {
                            Text(
                                text = "Expand All",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { expandedNodeIds = emptySet() },
                            color = if (isLightMode) Color(0xFF64748B) else Color(0xFF475569)
                        ) {
                            Text(
                                text = "Collapse",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tree View List
            if (treeRoots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = if (isLightMode) Color(0xFF94A3B8) else Color(0xFF484F58),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Room hierarchy records found",
                            color = if (isLightMode) Color(0xFF64748B) else Color(0xFF8B949E),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(treeRoots, key = { it.entity.id }) { rootNode ->
                        RecursiveOrgNodeTreeItem(
                            node = rootNode,
                            depth = 0,
                            expandedNodeIds = expandedNodeIds,
                            onToggleExpand = { id ->
                                expandedNodeIds = if (expandedNodeIds.contains(id)) {
                                    expandedNodeIds - id
                                } else {
                                    expandedNodeIds + id
                                }
                            },
                            onSelectNode = onNodeSelect,
                            isLightMode = isLightMode
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// RECURSIVE TREE ITEM COMPOSABLE
// ----------------------------------------------------
@Composable
fun RecursiveOrgNodeTreeItem(
    node: OrgTreeNode,
    depth: Int,
    expandedNodeIds: Set<String>,
    onToggleExpand: (String) -> Unit,
    onSelectNode: (OrgNodeEntity) -> Unit,
    isLightMode: Boolean,
    modifier: Modifier = Modifier
) {
    val isExpanded = expandedNodeIds.contains(node.entity.id)
    val hasChildren = node.children.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clickable { onSelectNode(node.entity) },
            shape = RoundedCornerShape(10.dp),
            color = if (isLightMode) Color.White else Color(0xFF161B22),
            border = BorderStroke(
                1.dp,
                if (isLightMode) Color(0xFFE2E8F0) else Color(0xFF21262D)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indentation Guide Lines for Depth
                if (depth > 0) {
                    Spacer(modifier = Modifier.width((depth * 14).dp))
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .background(if (isLightMode) Color(0xFFCBD5E1) else Color(0xFF30363D))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Expand/Collapse Chevron Button
                if (hasChildren) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { onToggleExpand(node.entity.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = "Expand/Collapse",
                            tint = if (isLightMode) Color(0xFF2563EB) else Color(0xFF58A6FF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(22.dp))
                }

                // Avatar / Initials
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(getRoleColor(node.entity.role, isLightMode)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = node.entity.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Name & Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = node.entity.name,
                            color = if (isLightMode) Color(0xFF0F172A) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(getStatusColor(node.entity.attendanceStatus))
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = formatRole(node.entity.role),
                            color = if (isLightMode) Color(0xFF64748B) else Color(0xFF8B949E),
                            fontSize = 10.5.sp
                        )
                        Text(
                            text = "• ${node.entity.department}",
                            color = if (isLightMode) Color(0xFF2563EB) else Color(0xFF58A6FF),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Sub-reports count badge
                if (hasChildren) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isLightMode) Color(0xFFEFF6FF) else Color(0xFF1F2937)
                    ) {
                        Text(
                            text = "${node.children.size}",
                            color = if (isLightMode) Color(0xFF2563EB) else Color(0xFF60A5FA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Recursive Rendering of Child Nodes
        if (hasChildren && isExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                node.children.forEach { childNode ->
                    RecursiveOrgNodeTreeItem(
                        node = childNode,
                        depth = depth + 1,
                        expandedNodeIds = expandedNodeIds,
                        onToggleExpand = onToggleExpand,
                        onSelectNode = onSelectNode,
                        isLightMode = isLightMode
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// COLOR & FORMAT HELPER FUNCTIONS
// ----------------------------------------------------
private fun getRoleColor(role: String, isLightMode: Boolean): Color {
    return when (role) {
        "GENERAL_MANAGER" -> if (isLightMode) Color(0xFFD97706) else Color(0xFFF59E0B)
        "SUPERVISOR" -> if (isLightMode) Color(0xFF2563EB) else Color(0xFF3B82F6)
        else -> if (isLightMode) Color(0xFF059669) else Color(0xFF10B981)
    }
}

private fun getStatusColor(status: String): Color {
    return when (status) {
        "CLOCKED_IN_GEOFENCE" -> Color(0xFF10B981)
        "ON_BREAK" -> Color(0xFFF59E0B)
        "OFF_DUTY" -> Color(0xFF6B7280)
        else -> Color(0xFFEF4444)
    }
}

private fun formatRole(role: String): String {
    return when (role) {
        "GENERAL_MANAGER" -> "General Manager"
        "SUPERVISOR" -> "Supervisor"
        else -> "Employee"
    }
}
