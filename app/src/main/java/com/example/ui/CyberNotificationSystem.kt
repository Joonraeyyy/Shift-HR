package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

// =========================================================================
// 🔔 DUAL-LAYER CYBER NOTIFICATION SYSTEM MODEL & IMPLEMENTATION
// =========================================================================

enum class NotificationPriority { URGENT, PASSIVE }

data class AppNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val priority: NotificationPriority,
    val timestampIso: String = "Just now",
    val isRead: Boolean = false,
    val targetRoute: String? = null
)

typealias AppNotificationItem = AppNotification

/**
 * Main Host wrapper providing floating top banner overlay for urgent alerts
 * and top header notification bell integration.
 */
@Composable
fun CyberNotificationSystemHost(
    activeUrgentNotification: AppNotification?,
    unreadCount: Int,
    onBellClick: () -> Unit,
    onDismissUrgent: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main Screen Content
        content()

        // ----------------------------------------------------
        // LAYER A: FLOATING URGENT NOTIFICATION OVERLAY
        // ----------------------------------------------------
        AnimatedVisibility(
            visible = activeUrgentNotification != null && activeUrgentNotification.priority == NotificationPriority.URGENT,
            enter = slideInVertically(animationSpec = tween(300)) { -it } + fadeIn(),
            exit = slideOutVertically(animationSpec = tween(300)) { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp, start = 16.dp, end = 16.dp)
        ) {
            activeUrgentNotification?.let { alert ->
                UrgentNotificationBanner(
                    notification = alert,
                    onDismiss = onDismissUrgent
                )
            }
        }
    }
}

// --- 1. Urgent Top Floating Banner Component ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentNotificationBanner(
    notification: AppNotification,
    onDismiss: () -> Unit
) {
    // Auto-dismiss urgent banner after 5 seconds
    LaunchedEffect(notification.id) {
        delay(5000)
        onDismiss()
    }

    val isLightTheme = MaterialTheme.colorScheme.onBackground != Color(0xFFFFFFFF)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLightTheme) Color(0xFFFEF2F2) else Color(0xFF2C1515)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isLightTheme) Color(0xFFFCA5A5) else Color(0xFF7F1D1D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isLightTheme) Color(0xFF991B1B) else Color(0xFFF87171),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notification.title,
                            color = if (isLightTheme) Color(0xFF991B1B) else Color(0xFFFECDD3),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = notification.message,
                            color = if (isLightTheme) Color(0xFF7F1D1D) else Color(0xFFF3F4F6),
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = getAdaptiveTextColor(0.6f)
                        )
                    }
                }
            }
        }
    )
}

// --- 2. Top Header Bell Icon with Badge Counter ---
@Composable
fun NotificationBellButton(
    unreadCount: Int,
    onClick: () -> Unit
) {
    val isLightTheme = MaterialTheme.colorScheme.onBackground != Color(0xFFFFFFFF)
    val iconTint = if (isLightTheme) Color(0xFF059669) else Color(0xFF34D399)

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notifications",
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- 3. Notification Center Modal Bottom Sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    notifications: List<AppNotification>,
    onDismiss: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit = {},
    onDeleteNotification: (String) -> Unit = {},
    onDeleteNotifications: (Set<String>) -> Unit = {},
    onMarkAllAsRead: () -> Unit = {},
    onClearAll: () -> Unit = {},
    onTriggerTestUrgent: () -> Unit = {},
    onTriggerTestPassive: () -> Unit = {}
) {
    val isLightTheme = MaterialTheme.colorScheme.onBackground != Color(0xFFFFFFFF)
    val sheetBg = if (isLightTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val handleColor = getAdaptiveColor(0.2f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(handleColor)
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
        ) {
            NotificationDashboardContent(
                notifications = notifications,
                onNotificationClick = onNotificationClick,
                onDeleteNotification = onDeleteNotification,
                onDeleteNotifications = onDeleteNotifications,
                onMarkAllAsRead = onMarkAllAsRead,
                onClearAll = onClearAll,
                onTriggerTestUrgent = onTriggerTestUrgent,
                onTriggerTestPassive = onTriggerTestPassive
            )
        }
    }
}

// --- 4. Standalone Notification Dashboard Screen ---
@Composable
fun NotificationDashboardScreen(
    notifications: List<AppNotification>,
    onNotificationClick: (AppNotification) -> Unit = {},
    onDeleteNotification: (String) -> Unit = {},
    onDeleteNotifications: (Set<String>) -> Unit = {},
    onMarkAllAsRead: () -> Unit = {},
    onClearAll: () -> Unit = {},
    onTriggerTestUrgent: () -> Unit = {},
    onTriggerTestPassive: () -> Unit = {}
) {
    NotificationDashboardContent(
        notifications = notifications,
        onNotificationClick = onNotificationClick,
        onDeleteNotification = onDeleteNotification,
        onDeleteNotifications = onDeleteNotifications,
        onMarkAllAsRead = onMarkAllAsRead,
        onClearAll = onClearAll,
        onTriggerTestUrgent = onTriggerTestUrgent,
        onTriggerTestPassive = onTriggerTestPassive
    )
}

// =========================================================================
// 🟢 NOTIFICATION DASHBOARD MAIN CONTENT COMPOSABLE (THEMED & ADAPTIVE)
// =========================================================================
@Composable
fun NotificationDashboardContent(
    notifications: List<AppNotification>,
    onNotificationClick: (AppNotification) -> Unit = {},
    onDeleteNotification: (String) -> Unit = {},
    onDeleteNotifications: (Set<String>) -> Unit = {},
    onMarkAllAsRead: () -> Unit = {},
    onClearAll: () -> Unit = {},
    onTriggerTestUrgent: () -> Unit = {},
    onTriggerTestPassive: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val isLightTheme = MaterialTheme.colorScheme.onBackground != Color(0xFFFFFFFF)
    val bgCanvas = if (isLightTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val primaryAccent = if (isLightTheme) Color(0xFF059669) else Color(0xFF34D399)

    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "UNREAD" -> notifications.filter { !it.isRead }
            "URGENT" -> notifications.filter { it.priority == NotificationPriority.URGENT }
            else -> notifications
        }
    }

    val unreadCount = notifications.count { !it.isRead }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLightTheme) Color(0xFFD1FAE5) else Color(0xFF064E3B))
                                .border(1.dp, primaryAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Notifications",
                            color = getAdaptiveTextColor(1.0f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$unreadCount unread updates pending",
                            color = primaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = onMarkAllAsRead,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark all as read",
                                tint = primaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (notifications.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (isMultiSelectMode) {
                                    isMultiSelectMode = false
                                    selectedIds = emptySet()
                                } else {
                                    isMultiSelectMode = true
                                    selectedIds = filteredNotifications.map { it.id }.toSet()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Toggle Select Mode",
                                tint = if (isMultiSelectMode) primaryAccent else getAdaptiveTextColor(0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "ALL" to "All (${notifications.size})",
                    "UNREAD" to "Unread ($unreadCount)",
                    "URGENT" to "Urgent (${notifications.count { it.priority == NotificationPriority.URGENT }})"
                ).forEach { (key, label) ->
                    val isSelected = selectedFilter == key
                    val chipBg = if (isSelected) primaryAccent else if (isLightTheme) Color(0xFFFFFFFF) else Color(0xFF1E293B)
                    val chipBorder = if (isSelected) primaryAccent else if (isLightTheme) Color(0xFFE2E8F0) else Color(0xFF334155)
                    val chipText = if (isSelected) Color.White else getAdaptiveTextColor(0.7f)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                            .clickable { selectedFilter = key }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = chipText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Test Triggers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTriggerTestUrgent,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("Urgent Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onTriggerTestPassive,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryAccent),
                    border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("Passive Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Notifications List / Empty State
            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isLightTheme) Color(0xFFFFFFFF) else Color(0xFF1E293B))
                                .border(1.dp, getAdaptiveColor(0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = getAdaptiveTextColor(0.5f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No notifications found",
                            color = getAdaptiveTextColor(0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You're all caught up! Swipe to delete alerts anytime.",
                            color = getAdaptiveTextColor(0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        NotificationItemCard(
                            item = item,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = isSelected,
                            onLongPress = {
                                if (!isMultiSelectMode) {
                                    isMultiSelectMode = true
                                    selectedIds = setOf(item.id)
                                } else {
                                    if (isSelected) {
                                        selectedIds = selectedIds - item.id
                                        if (selectedIds.isEmpty()) isMultiSelectMode = false
                                    } else {
                                        selectedIds = selectedIds + item.id
                                    }
                                }
                            },
                            onToggleSelect = {
                                if (isSelected) {
                                    selectedIds = selectedIds - item.id
                                    if (selectedIds.isEmpty()) isMultiSelectMode = false
                                } else {
                                    selectedIds = selectedIds + item.id
                                }
                            },
                            onClick = {
                                if (isMultiSelectMode) {
                                    if (isSelected) {
                                        selectedIds = selectedIds - item.id
                                        if (selectedIds.isEmpty()) isMultiSelectMode = false
                                    } else {
                                        selectedIds = selectedIds + item.id
                                    }
                                } else {
                                    onNotificationClick(item)
                                }
                            },
                            onDelete = { id ->
                                onDeleteNotification(id)
                                selectedIds = selectedIds - id
                                if (selectedIds.isEmpty()) isMultiSelectMode = false
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Pop-up Bar for Bulk Deletion
        AnimatedVisibility(
            visible = isMultiSelectMode && selectedIds.isNotEmpty(),
            enter = slideInVertically(animationSpec = tween(300)) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = tween(300)) { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(100f)
        ) {
            FloatingSelectionBar(
                selectedCount = selectedIds.size,
                onCancel = {
                    isMultiSelectMode = false
                    selectedIds = emptySet()
                },
                onDelete = {
                    onDeleteNotifications(selectedIds)
                    selectedIds = emptySet()
                    isMultiSelectMode = false
                }
            )
        }
    }
}

// =========================================================================
// 🎴 NOTIFICATION ITEM CARD COMPOSABLE WITH SWIPE-TO-DELETE & LONG PRESS
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItemCard(
    item: AppNotification,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: (AppNotification) -> Unit = {},
    onToggleSelect: (AppNotification) -> Unit = {},
    onClick: (AppNotification) -> Unit = {},
    onDelete: (String) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val isUrgent = item.priority == NotificationPriority.URGENT
    val isLightTheme = MaterialTheme.colorScheme.onBackground != Color(0xFFFFFFFF)

    val cardBg = if (isLightTheme) Color(0xFFFFFFFF) else Color(0xFF1E293B)
    val primaryAccent = if (isLightTheme) Color(0xFF059669) else Color(0xFF34D399)
    val borderCol = if (isSelected) primaryAccent else if (isLightTheme) Color(0xFFE2E8F0) else Color(0xFF334155)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete(item.id)
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isMultiSelectMode,
        enableDismissFromEndToStart = !isMultiSelectMode,
        backgroundContent = {
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEF4444))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete(item.id)
                    }
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Delete",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(isMultiSelectMode) {
                        detectTapGestures(
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPress(item)
                            },
                            onTap = {
                                if (isMultiSelectMode) {
                                    onToggleSelect(item)
                                } else {
                                    onClick(item)
                                }
                            }
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, borderCol),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isLightTheme) 2.dp else 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Multi-Select Checkbox / Radio Button
                    if (isMultiSelectMode) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) primaryAccent else Color.Transparent)
                                .border(
                                    1.5.dp,
                                    if (isSelected) primaryAccent else getAdaptiveTextColor(0.3f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    // Iconography Badge Container
                    val badgeBg = if (isUrgent) {
                        if (isLightTheme) Color(0xFFFEE2E2) else Color(0xFF451A1A)
                    } else {
                        if (isLightTheme) Color(0xFFD1FAE5) else Color(0xFF064E3B)
                    }
                    val badgeBorder = if (isUrgent) {
                        if (isLightTheme) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                    } else {
                        if (isLightTheme) Color(0xFFA7F3D0) else Color(0xFF059669)
                    }
                    val badgeIconTint = if (isUrgent) Color(0xFFEF4444) else primaryAccent

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUrgent) Icons.Default.Warning else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = badgeIconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Text Content Column
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                color = getAdaptiveTextColor(if (!item.isRead) 1.0f else 0.8f),
                                fontSize = 13.5.sp,
                                fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.timestampIso,
                                color = getAdaptiveTextColor(0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = item.message,
                            color = getAdaptiveTextColor(0.7f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Unread Indicator Dot
                    if (!item.isRead && !isMultiSelectMode) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isUrgent) Color(0xFFEF4444) else primaryAccent)
                        )
                    }
                }
            }
        }
    )
}

// =========================================================================
// 🚀 FLOATING SELECTION BAR FOR BULK ACTIONS
// =========================================================================
@Composable
fun FloatingSelectionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isLightTheme = MaterialTheme.colorScheme.onBackground != Color(0xFFFFFFFF)
    val barBg = if (isLightTheme) Color(0xFFFFFFFF) else Color(0xFF1E293B)
    val barBorder = if (isLightTheme) Color(0xFFCBD5E1) else Color(0xFF334155)
    val primaryAccent = if (isLightTheme) Color(0xFF059669) else Color(0xFF34D399)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = primaryAccent.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(20.dp),
        color = barBg,
        border = BorderStroke(1.5.dp, barBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(primaryAccent)
                )
                Text(
                    text = "$selectedCount Selected",
                    color = getAdaptiveTextColor(1.0f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(contentColor = getAdaptiveTextColor(0.6f))
                ) {
                    Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delete",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
