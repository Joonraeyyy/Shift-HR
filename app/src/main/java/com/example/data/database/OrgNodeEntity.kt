package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "org_nodes")
data class OrgNodeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: String, // "GENERAL_MANAGER", "SUPERVISOR", "EMPLOYEE"
    val department: String,
    val parentId: String? = null,
    val positionX: Float = 100f,
    val positionY: Float = 100f,
    val assignedShift: String = "Day Shift (08:00 - 16:00)",
    val attendanceStatus: String = "CLOCKED_IN_GEOFENCE",
    val weeklyHours: Double = 38.0,
    val restPeriodHours: Double = 12.0,
    val approvalTier: Int = 1
)
