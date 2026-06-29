package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftConfigDao {
    @Query("SELECT * FROM shift_configs WHERE id = 1 LIMIT 1")
    fun getShiftConfigFlow(): Flow<ShiftConfigEntity?>

    @Query("SELECT * FROM shift_configs WHERE id = 1 LIMIT 1")
    suspend fun getShiftConfig(): ShiftConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateShiftConfig(config: ShiftConfigEntity)
}
