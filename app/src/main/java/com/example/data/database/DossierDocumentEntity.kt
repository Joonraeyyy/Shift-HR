package com.example.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import java.util.UUID
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "dossier_documents")
data class DossierDocumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val localFilePath: String, // Path to internal app storage
    val category: String,
    val profileId: String = "EMP-000", // Associate document to a specific employee
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING" // States: PENDING, SYNCING, SYNCED, FAILED
)

@Dao
interface DossierDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DossierDocumentEntity)

    @Update
    suspend fun updateDocument(document: DossierDocumentEntity)

    @Query("SELECT * FROM dossier_documents WHERE id = :id")
    suspend fun getDocumentById(id: String): DossierDocumentEntity?

    @Query("SELECT * FROM dossier_documents ORDER BY timestamp DESC")
    fun getAllDocuments(): Flow<List<DossierDocumentEntity>>

    @Query("SELECT * FROM dossier_documents WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getDocumentsForProfile(profileId: String): Flow<List<DossierDocumentEntity>>
}
