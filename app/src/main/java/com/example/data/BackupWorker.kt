package com.example.data

import android.content.Context
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.io.File
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.flow.first

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.rdDao()
            
            // Generate backup string
            val root = JSONObject()
            root.put("version", 1)
            
            // Employees
            val employeesList = dao.getAllEmployees().first()
            val empsArray = JSONArray()
            employeesList.forEach { emp ->
                val obj = JSONObject()
                obj.put("id", emp.id)
                obj.put("name", emp.name)
                obj.put("role", emp.role)
                obj.put("avatarColorHex", emp.avatarColorHex)
                empsArray.put(obj)
            }
            root.put("employees", empsArray)
            
            // Samples
            val samplesList = dao.getAllSamples().first()
            val samplesArray = JSONArray()
            samplesList.forEach { sample ->
                val obj = JSONObject()
                obj.put("sampleCode", sample.sampleCode)
                obj.put("sampleName", sample.sampleName)
                obj.put("assignedEmployeeId", sample.assignedEmployeeId)
                obj.put("status", sample.status)
                obj.put("dateCreated", sample.dateCreated)
                obj.put("description", sample.description)
                obj.put("estimatedTimeStr", sample.estimatedTimeStr)
                samplesArray.put(obj)
            }
            root.put("samples", samplesArray)
            
            // Runs
            val runsList = dao.getAllRuns().first()
            val runsArray = JSONArray()
            runsList.forEach { run ->
                val obj = JSONObject()
                obj.put("id", run.id)
                obj.put("employeeId", run.employeeId)
                obj.put("sampleCode", run.sampleCode)
                obj.put("runNumber", run.runNumber)
                obj.put("durationMs", run.durationMs)
                obj.put("status", run.status)
                obj.put("failureReason", run.failureReason)
                obj.put("timestamp", run.timestamp)
                obj.put("dateString", run.dateString)
                obj.put("startTimeStr", run.startTimeStr)
                obj.put("endTimeStr", run.endTimeStr)
                runsArray.put(obj)
            }
            root.put("runs", runsArray)
            
            // Settings in SharedPrefs
            val sharedPrefs = applicationContext.getSharedPreferences("rd_tracker_new_prefs", Context.MODE_PRIVATE)
            val settings = JSONObject()
            settings.put("approver_name", sharedPrefs.getString("approver_name", "Lê Cao Nguyên"))
            settings.put("approver_title", sharedPrefs.getString("approver_title", "TRƯỞNG PHÒNG ĐẢM BẢO CHẤT LƯỢNG"))
            settings.put("approver_role", sharedPrefs.getString("approver_role", "(QA/QC MANAGER - DUYỆT)"))
            settings.put("preparer_name", sharedPrefs.getString("preparer_name", "Nguyễn Thị Thúy"))
            settings.put("preparer_title", sharedPrefs.getString("preparer_title", "TRƯỞNG NHÓM R&D 02"))
            settings.put("preparer_role", sharedPrefs.getString("preparer_role", "(NGƯỜI LẬP PHIẾU BÁO CÁO)"))
            settings.put("github_owner", sharedPrefs.getString("github_owner", "vankhoai690"))
            settings.put("github_repo", sharedPrefs.getString("github_repo", "RDTrackerApp"))
            root.put("settings", settings)
            
            val jsonStr = root.toString(2)
            
            val backupFolderStr = sharedPrefs.getString("backup_folder", "R&D check list") ?: "R&D check list"
            val backupFileStr = sharedPrefs.getString("backup_file_name", "backup.json") ?: "backup.json"
            
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backupFolder = File(downloadsDir, backupFolderStr)
            if (!backupFolder.exists()) {
                backupFolder.mkdirs()
            }
            val backupFile = File(backupFolder, backupFileStr)
            
            var success = false
            try {
                backupFile.writeText(jsonStr)
                success = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            if (!success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val resolver = applicationContext.contentResolver
                    val uriExternal = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                    val selectionArgs = arrayOf(backupFileStr, Environment.DIRECTORY_DOWNLOADS + "/" + backupFolderStr + "/")
                    try {
                        resolver.delete(uriExternal, selection, selectionArgs)
                    } catch (ig: Exception) {}
                    
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, backupFileStr)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + backupFolderStr)
                    }
                    val fileUri = resolver.insert(uriExternal, contentValues)
                    if (fileUri != null) {
                        resolver.openOutputStream(fileUri)?.use { out ->
                            out.write(jsonStr.toByteArray())
                        }
                        success = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            return if (success) Result.success() else Result.failure()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
    
    companion object {
        fun enqueuePeriodicBackup(context: Context, intervalHours: Long = 24) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build()
                
            val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
                
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "RDTrackerDailyBackup",
                ExistingPeriodicWorkPolicy.UPDATE,
                backupRequest
            )
        }
    }
}
