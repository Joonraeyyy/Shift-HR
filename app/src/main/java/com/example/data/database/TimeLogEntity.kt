package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_logs")
data class TimeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "YYYY-MM-DD" or formatted date
    val employeeName: String,
    val timeIn: Long? = null,
    val lunchOut: Long? = null,
    val lunchIn: Long? = null,
    val breakOut: Long? = null,
    val breakIn: Long? = null,
    val timeOut: Long? = null,
    val isSynced: Boolean = false,
    val isApproved: String = "PENDING", // PENDING, APPROVED, REJECTED
    val rejectionReason: String? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val gpsLocationName: String? = null,
    val hourlyRate: Double = 25.0
)
