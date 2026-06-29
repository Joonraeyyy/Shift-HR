package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shift_configs")
data class ShiftConfigEntity(
    @PrimaryKey val id: Int = 1, // Single row configuration
    val employeeName: String = "Sarah Jenkins",
    val shiftDurationHours: Float = 8.0f,
    val lunchDurationMinutes: Int = 60,
    val breakDurationMinutes: Int = 30,
    val hourlyRate: Double = 25.0,
    val isRemoteVerificationEnabled: Boolean = true
)
