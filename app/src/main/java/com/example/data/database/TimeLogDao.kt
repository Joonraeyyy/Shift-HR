package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeLogDao {
    @Query("SELECT * FROM time_logs ORDER BY id DESC")
    fun getAllTimeLogsFlow(): Flow<List<TimeLogEntity>>

    @Query("SELECT * FROM time_logs WHERE id = :id LIMIT 1")
    suspend fun getTimeLogById(id: Long): TimeLogEntity?

    @Query("SELECT * FROM time_logs WHERE date = :date AND employeeName = :employeeName LIMIT 1")
    suspend fun getTimeLogByDateAndEmployee(date: String, employeeName: String): TimeLogEntity?

    @Query("SELECT * FROM time_logs WHERE timeOut IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun getActiveTimeLog(): TimeLogEntity?

    @Query("SELECT * FROM time_logs WHERE isApproved = 'PENDING' ORDER BY id DESC")
    fun getPendingTimeLogsFlow(): Flow<List<TimeLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeLog(log: TimeLogEntity): Long

    @Update
    suspend fun updateTimeLog(log: TimeLogEntity)

    @Delete
    suspend fun deleteTimeLog(log: TimeLogEntity)

    @Query("DELETE FROM time_logs")
    suspend fun clearAllLogs()
}
