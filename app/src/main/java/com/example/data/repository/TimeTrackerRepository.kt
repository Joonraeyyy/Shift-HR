package com.example.data.repository

import com.example.data.database.ShiftConfigDao
import com.example.data.database.ShiftConfigEntity
import com.example.data.database.TimeLogDao
import com.example.data.database.TimeLogEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class TimeTrackerRepository(
    private val timeLogDao: TimeLogDao,
    private val shiftConfigDao: ShiftConfigDao
) {
    val allTimeLogs: Flow<List<TimeLogEntity>> = timeLogDao.getAllTimeLogsFlow()
    val pendingTimeLogs: Flow<List<TimeLogEntity>> = timeLogDao.getPendingTimeLogsFlow()
    val shiftConfigFlow: Flow<ShiftConfigEntity?> = shiftConfigDao.getShiftConfigFlow()

    suspend fun getShiftConfig(): ShiftConfigEntity {
        var config = shiftConfigDao.getShiftConfig()
        if (config == null) {
            config = ShiftConfigEntity()
            shiftConfigDao.insertOrUpdateShiftConfig(config)
        }
        return config
    }

    suspend fun saveShiftConfig(config: ShiftConfigEntity) {
        shiftConfigDao.insertOrUpdateShiftConfig(config)
    }

    suspend fun getActiveTimeLog(): TimeLogEntity? {
        return timeLogDao.getActiveTimeLog()
    }

    suspend fun getTimeLogById(id: Long): TimeLogEntity? {
        return timeLogDao.getTimeLogById(id)
    }

    suspend fun insertOrUpdateTimeLog(log: TimeLogEntity) {
        if (log.id == 0L) {
            timeLogDao.insertTimeLog(log)
        } else {
            timeLogDao.updateTimeLog(log)
        }
    }

    suspend fun deleteTimeLog(log: TimeLogEntity) {
        timeLogDao.deleteTimeLog(log)
    }

    // Handles an employee punch action
    suspend fun registerPunch(actionType: String, isMockOffline: Boolean): String {
        val config = getShiftConfig()
        val employeeName = config.employeeName
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(Date(now))

        // Get or create today's log
        var activeLog = timeLogDao.getActiveTimeLog() ?: timeLogDao.getTimeLogByDateAndEmployee(dateStr, employeeName)

        val coordinates = if (config.isRemoteVerificationEnabled) {
            // Generate dynamic coordinates near the user's city or virtual office
            val lat = 37.7749 + (Math.random() - 0.5) * 0.05
            val lng = -122.4194 + (Math.random() - 0.5) * 0.05
            Pair(lat, lng)
        } else null

        val locationName = if (coordinates != null) {
            listOf("Remote Home Office", "Coffee Shop Co-working", "Satellite Hub", "Client Site (Remote)").random()
        } else null

        if (activeLog == null) {
            if (actionType != "TIME_IN") {
                return "Please start with 'Time In' first."
            }
            activeLog = TimeLogEntity(
                date = dateStr,
                employeeName = employeeName,
                timeIn = now,
                isSynced = !isMockOffline,
                isApproved = "PENDING",
                gpsLatitude = coordinates?.first,
                gpsLongitude = coordinates?.second,
                gpsLocationName = locationName,
                hourlyRate = config.hourlyRate
            )
            timeLogDao.insertTimeLog(activeLog)
            return "Clocked In successfully at ${formatTime(now)}!"
        }

        // Update active log based on punch action
        val updatedLog = when (actionType) {
            "TIME_IN" -> {
                if (activeLog.timeIn != null) return "Already checked in today."
                activeLog.copy(timeIn = now)
            }
            "LUNCH_OUT" -> {
                if (activeLog.timeIn == null) return "Must clock in first."
                if (activeLog.lunchOut != null) return "Already went to lunch."
                activeLog.copy(lunchOut = now)
            }
            "LUNCH_IN" -> {
                if (activeLog.lunchOut == null) return "Must start lunch first."
                if (activeLog.lunchIn != null) return "Already returned from lunch."
                activeLog.copy(lunchIn = now)
            }
            "BREAK_OUT" -> {
                if (activeLog.timeIn == null) return "Must clock in first."
                if (activeLog.breakOut != null) return "Already started break."
                activeLog.copy(breakOut = now)
            }
            "BREAK_IN" -> {
                if (activeLog.breakOut == null) return "Must start break first."
                if (activeLog.breakIn != null) return "Already ended break."
                activeLog.copy(breakIn = now)
            }
            "TIME_OUT" -> {
                if (activeLog.timeIn == null) return "Must clock in first."
                if (activeLog.timeOut != null) return "Already checked out today."
                activeLog.copy(timeOut = now)
            }
            else -> return "Invalid action type."
        }

        // Save progress, keeping track of online sync
        val finalLog = updatedLog.copy(isSynced = !isMockOffline)
        timeLogDao.insertTimeLog(finalLog)

        val actionLabel = actionType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        return "$actionLabel logged at ${formatTime(now)}."
    }

    // Syncs all local-only (unsynced) records to the Simulated HR Payroll Engine
    suspend fun syncOfflineRecords(): Int {
        val allLogs = timeLogDao.getAllTimeLogsFlow().first()
        val unsynced = allLogs.filter { !it.isSynced }
        if (unsynced.isEmpty()) return 0

        // Simulate network roundtrip latency
        delay(1500)

        unsynced.forEach { log ->
            timeLogDao.updateTimeLog(log.copy(isSynced = true))
        }
        return unsynced.size
    }

    // Helper formatting function
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // Prefills database with highly detailed sample logs for payroll/spreadsheet reporting
    suspend fun prefillMockData() {
        val existing = timeLogDao.getAllTimeLogsFlow().first()
        if (existing.isNotEmpty()) return

        val config = getShiftConfig()
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val sampleLogs = mutableListOf<TimeLogEntity>()

        // Create entries for the last 5 business days
        for (i in 5 downTo 1) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            // Skip weekends
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) continue

            val dateStr = sdf.format(calendar.time)

            // Punch timestamps around 9:00 AM start, 1:00 PM lunch, 4:00 PM break, 5:30 PM out
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, (0..15).random())
            val timeIn = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 13)
            calendar.set(Calendar.MINUTE, (0..10).random())
            val lunchOut = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 14)
            calendar.set(Calendar.MINUTE, (0..5).random())
            val lunchIn = calendar.timeInMillis

            // Some days have breaks, some don't
            val hasBreak = i % 2 == 0
            var breakOut: Long? = null
            var breakIn: Long? = null
            if (hasBreak) {
                calendar.set(Calendar.HOUR_OF_DAY, 15)
                calendar.set(Calendar.MINUTE, 30 + (0..5).random())
                breakOut = calendar.timeInMillis

                calendar.set(Calendar.HOUR_OF_DAY, 16)
                calendar.set(Calendar.MINUTE, (0..5).random()) // ~30 min break
                breakIn = calendar.timeInMillis
            }

            calendar.set(Calendar.HOUR_OF_DAY, 17)
            calendar.set(Calendar.MINUTE, 30 + (0..15).random()) // ~8.5 hours total
            val timeOut = calendar.timeInMillis

            // Randomize approval state
            val approval = when (i) {
                1 -> "PENDING"
                2 -> "REJECTED"
                else -> "APPROVED"
            }
            val reason = if (approval == "REJECTED") "Forgot to log lunch break correctly; lunch overage detected." else null

            val isRemote = i % 3 == 0
            val lat = if (isRemote) 37.7749 + (Math.random() - 0.5) * 0.04 else null
            val lng = if (isRemote) -122.4194 + (Math.random() - 0.5) * 0.04 else null
            val locName = if (isRemote) "Remote Workspace" else null

            sampleLogs.add(
                TimeLogEntity(
                    date = dateStr,
                    employeeName = config.employeeName,
                    timeIn = timeIn,
                    lunchOut = lunchOut,
                    lunchIn = lunchIn,
                    breakOut = breakOut,
                    breakIn = breakIn,
                    timeOut = timeOut,
                    isSynced = true,
                    isApproved = approval,
                    rejectionReason = reason,
                    gpsLatitude = lat,
                    gpsLongitude = lng,
                    gpsLocationName = locName,
                    hourlyRate = config.hourlyRate
                )
            )
        }

        // Add an entry for co-worker to simulate multi-employee admin approval
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val coWorkerDateStr = sdf.format(calendar.time)

        calendar.set(Calendar.HOUR_OF_DAY, 8)
        calendar.set(Calendar.MINUTE, 55)
        val cwTimeIn = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        val cwLunchOut = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 13)
        calendar.set(Calendar.MINUTE, 10) // 70 min lunch
        val cwLunchIn = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 17)
        calendar.set(Calendar.MINUTE, 15)
        val cwTimeOut = calendar.timeInMillis

        sampleLogs.add(
            TimeLogEntity(
                date = coWorkerDateStr,
                employeeName = "Marcus Aurelius (HR Intern)",
                timeIn = cwTimeIn,
                lunchOut = cwLunchOut,
                lunchIn = cwLunchIn,
                breakOut = null,
                breakIn = null,
                timeOut = cwTimeOut,
                isSynced = false, // Mock offline log awaiting sync
                isApproved = "PENDING",
                rejectionReason = null,
                gpsLatitude = 37.7749,
                gpsLongitude = -122.4194,
                gpsLocationName = "Remote Hub",
                hourlyRate = 18.5
            )
        )

        sampleLogs.forEach { log ->
            timeLogDao.insertTimeLog(log)
        }
    }
}
