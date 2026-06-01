package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.RDViewModel
import com.example.ui.RDTrackerApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: RDViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Read user custom configurations from SharedPreferences
    val sharedPrefs = getSharedPreferences("rd_tracker_new_prefs", android.content.Context.MODE_PRIVATE)
    val backupInterval = sharedPrefs.getLong("backup_interval_hours", 24L)
    val reminderInterval = sharedPrefs.getLong("reminder_interval_hours", 4L)
    
    // Enqueue background daily automatic backup
    com.example.data.BackupWorker.enqueuePeriodicBackup(applicationContext, backupInterval)
    
    // Enqueue background R&D task status updates reminder according to configuration
    com.example.data.TaskUpdateReminderWorker.enqueuePeriodicReminder(applicationContext, reminderInterval)
    
    setContent {
      MyApplicationTheme {
        RDTrackerApp(viewModel = viewModel)
      }
    }
  }
}
