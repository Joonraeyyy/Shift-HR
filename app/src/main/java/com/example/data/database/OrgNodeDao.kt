package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OrgNodeDao {
    @Query("SELECT * FROM org_nodes ORDER BY department, approvalTier DESC, name ASC")
    fun getAllNodesFlow(): Flow<List<OrgNodeEntity>>

    @Query("SELECT * FROM org_nodes")
    suspend fun getAllNodes(): List<OrgNodeEntity>

    @Query("SELECT * FROM org_nodes WHERE parentId = :parentId")
    suspend fun getChildrenOf(parentId: String): List<OrgNodeEntity>

    @Query("SELECT * FROM org_nodes WHERE department = :department")
    fun getNodesByDepartmentFlow(department: String): Flow<List<OrgNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<OrgNodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: OrgNodeEntity)

    @Query("DELETE FROM org_nodes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM org_nodes")
    suspend fun deleteAll()
}
