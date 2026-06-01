package com.example.data

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class TaskUpdateReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val sharedPrefs = applicationContext.getSharedPreferences("rd_tracker_new_prefs", Context.MODE_PRIVATE)
            val isEnabled = sharedPrefs.getBoolean("reminder_enabled", true)
            if (!isEnabled) {
                return Result.success()
            }

            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.rdDao()
            val samples = dao.getAllSamples().first()
            val pendingSamples = samples.filter { it.status == "Đang thực hiện" }

            if (pendingSamples.isNotEmpty()) {
                sendNotification(pendingSamples.size)
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }

    private fun sendNotification(pendingCount: Int) {
        val channelId = "task_update_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nhắc nhở cập nhật Tiến độ R&D",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Kênh gửi thông báo nhắc lịch kiểm tra và cập nhật tiến độ nấu mẫu R&D"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val appName = "R&D check list"
        val customText = applicationContext.getSharedPreferences("rd_tracker_new_prefs", Context.MODE_PRIVATE)
            .getString("reminder_custom_message", "Có mẻ nấu đang thực hiện cần cập nhật tiến độ R&D!")

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Cập nhật Tiến độ R&D hàng ngày")
            .setContentText("Hiện có $pendingCount mẫu chưa hoàn thành. $customText")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }

    companion object {
        fun enqueuePeriodicReminder(context: Context, intervalHours: Long = 4, force: Boolean = false) {
            val sharedPrefs = context.getSharedPreferences("rd_tracker_new_prefs", Context.MODE_PRIVATE)
            val isEnabled = sharedPrefs.getBoolean("reminder_enabled", true)
            
            if (!isEnabled && !force) {
                WorkManager.getInstance(context).cancelUniqueWork("RDTrackerTaskUpdateReminder")
                return
            }

            val constraints = androidx.work.Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            // Min interval for WorkManager periodic work is 15 minutes, but hours range works great!
            val finalInterval = if (intervalHours < 1) 1 else intervalHours
            val reminderRequest = PeriodicWorkRequestBuilder<TaskUpdateReminderWorker>(finalInterval, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "RDTrackerTaskUpdateReminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                reminderRequest
            )
        }
    }
}
