package com.example.data.database

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun handleCapturedDocumentPipeline(
    context: Context,
    scannedPdfUri: Uri,
    chosenCategory: String,
    dossierDao: DossierDao
) {
    handleCapturedDocumentPipeline(context, scannedPdfUri, chosenCategory, "EMP-000", dossierDao)
}

fun handleCapturedDocumentPipeline(
    context: Context,
    scannedPdfUri: Uri,
    chosenCategory: String,
    profileId: String,
    dossierDao: DossierDao
) {
    // 1. Persist file locally inside private sandbox storage space
    val fileName = "SHIFTHR_DOC_${System.currentTimeMillis()}.pdf"
    val internalStorageFile = File(context.filesDir, fileName)

    context.contentResolver.openInputStream(scannedPdfUri)?.use { inputStream ->
        FileOutputStream(internalStorageFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }

    // 2. Prepare the Room Record Entity
    val localDocumentRecord = DossierDocumentEntity(
        fileName = fileName,
        localFilePath = internalStorageFile.absolutePath,
        category = chosenCategory,
        profileId = profileId,
        syncStatus = "PENDING"
    )

    // Launch inside a coroutine workspace context
    CoroutineScope(Dispatchers.IO).launch {
        // Save to localized database (Instantly populated in user UI display grid)
        dossierDao.insertDocument(localDocumentRecord)

        // 3. Queue WorkManager with strict network routing rules
        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Fires only when connected to internet
            .setRequiresBatteryNotLow(true) // Battery safety constraint check
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<DocumentSyncWorker>()
            .setInputData(workDataOf("KEY_DOCUMENT_ID" to localDocumentRecord.id))
            .setConstraints(syncConstraints)
            // If the network drops halfway, retry exponentially every 15 seconds
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_${localDocumentRecord.id}",
            ExistingWorkPolicy.REPLACE,
            syncWorkRequest
        )
    }
}
