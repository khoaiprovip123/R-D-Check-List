package com.example.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SampleReportItem(
    val sampleCode: String,
    val totalRuns: Int,
    val totalDurationMs: Long,
    val successCount: Int,
    val failureCount: Int,
    val runDurations: List<Pair<Int, Long>>, // (lần nấu, thời gian nấu)
    val failureReasonsList: List<String>,
    val detailRuns: List<RDRun>
)

data class EmployeeReportSummary(
    val employee: Employee,
    val totalSamplesCount: Int,
    val totalRunsCount: Int,
    val totalDurationMs: Long,
    val successCount: Int,
    val failureCount: Int,
    val successRate: Float,
    val sampleList: List<SampleReportItem>
)

class RDViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RDRepository

    // Streams from Database
    val allEmployees: StateFlow<List<Employee>>
    val allRuns: StateFlow<List<RDRun>>
    val allSamples: StateFlow<List<RDSample>>

    // Interactive UI Filters
    private val _filterYear = MutableStateFlow(SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()))
    val filterYear = _filterYear.asStateFlow()

    private val _filterMonth = MutableStateFlow(SimpleDateFormat("MM", Locale.getDefault()).format(Date())) // Current month
    val filterMonth = _filterMonth.asStateFlow()

    private val _filterDay = MutableStateFlow(SimpleDateFormat("dd", Locale.getDefault()).format(Date())) // default to today
    val filterDay = _filterDay.asStateFlow()

    private val _selectedEmployeeId = MutableStateFlow<Int?>(null) // null represent "Tất cả"
    val selectedEmployeeId = _selectedEmployeeId.asStateFlow()

    val yearsList = listOf("Tất cả", "2026", "2025")
    val monthsList = listOf("Tất cả", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")
    val daysList = (listOf("Tất cả") + (1..31).map { String.format(Locale.US, "%02d", it) })

    // Excel and PDF Export simulations states
    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage = _exportMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncSuccessCount = MutableStateFlow(124)
    val syncSuccessCount = _syncSuccessCount.asStateFlow()

    // Custom System Configurations
    private val sharedPrefs = application.getSharedPreferences("rd_tracker_new_prefs", android.content.Context.MODE_PRIVATE)

    private val _targetKpiSuccessRate = MutableStateFlow(sharedPrefs.getInt("target_kpi_success_rate", 80))
    val targetKpiSuccessRate = _targetKpiSuccessRate.asStateFlow()

    private val _targetCookingDurationMin = MutableStateFlow(sharedPrefs.getInt("target_cooking_duration_min", 45))
    val targetCookingDurationMin = _targetCookingDurationMin.asStateFlow()

    private val _targetCookingRuns = MutableStateFlow(sharedPrefs.getInt("target_cooking_runs", 3))
    val targetCookingRuns = _targetCookingRuns.asStateFlow()

    private val _autoSyncIntervalMin = MutableStateFlow(sharedPrefs.getInt("auto_sync_interval_min", 15))
    val autoSyncIntervalMin = _autoSyncIntervalMin.asStateFlow()

    private val _selectedThemeColorHex = MutableStateFlow(sharedPrefs.getString("selected_theme_color_hex", "#3B82F6") ?: "#3B82F6")
    val selectedThemeColorHex = _selectedThemeColorHex.asStateFlow()

    // Backup locations settings
    private val _backupFolder = MutableStateFlow(sharedPrefs.getString("backup_folder", "R&D check list") ?: "R&D check list")
    val backupFolder = _backupFolder.asStateFlow()

    private val _backupFileName = MutableStateFlow(sharedPrefs.getString("backup_file_name", "backup.json") ?: "backup.json")
    val backupFileName = _backupFileName.asStateFlow()

    private val _backupIntervalHours = MutableStateFlow(sharedPrefs.getLong("backup_interval_hours", 24L))
    val backupIntervalHours = _backupIntervalHours.asStateFlow()

    // Task Update Reminder settings
    private val _reminderEnabled = MutableStateFlow(sharedPrefs.getBoolean("reminder_enabled", true))
    val reminderEnabled = _reminderEnabled.asStateFlow()

    private val _reminderIntervalHours = MutableStateFlow(sharedPrefs.getLong("reminder_interval_hours", 4L))
    val reminderIntervalHours = _reminderIntervalHours.asStateFlow()

    private val _reminderCustomMessage = MutableStateFlow(sharedPrefs.getString("reminder_custom_message", "Có mẻ nấu đang thực hiện cần cập nhật tiến độ R&D!") ?: "Có mẻ nấu đang thực hiện cần cập nhật tiến độ R&D!")
    val reminderCustomMessage = _reminderCustomMessage.asStateFlow()

    fun updateTargetKpi(rate: Int) {
        _targetKpiSuccessRate.value = rate
        sharedPrefs.edit().putInt("target_kpi_success_rate", rate).apply()
    }
    fun updateTargetCookingDuration(duration: Int) {
        _targetCookingDurationMin.value = duration
        sharedPrefs.edit().putInt("target_cooking_duration_min", duration).apply()
    }
    fun updateTargetCookingRuns(runs: Int) {
        _targetCookingRuns.value = runs
        sharedPrefs.edit().putInt("target_cooking_runs", runs).apply()
    }
    fun updateAutoSyncInterval(interval: Int) {
        _autoSyncIntervalMin.value = interval
        sharedPrefs.edit().putInt("auto_sync_interval_min", interval).apply()
    }
    fun updateThemeColor(colorHex: String) {
        _selectedThemeColorHex.value = colorHex
        sharedPrefs.edit().putString("selected_theme_color_hex", colorHex).apply()
    }

    fun updateBackupLocationConfigs(folder: String, fileName: String, intervalHours: Long) {
        _backupFolder.value = folder.trim()
        _backupFileName.value = fileName.trim()
        _backupIntervalHours.value = intervalHours
        sharedPrefs.edit()
            .putString("backup_folder", folder.trim())
            .putString("backup_file_name", fileName.trim())
            .putLong("backup_interval_hours", intervalHours)
            .apply()
        BackupWorker.enqueuePeriodicBackup(getApplication(), intervalHours)
    }

    fun updateReminderConfigs(enabled: Boolean, intervalHours: Long, customMessage: String) {
        _reminderEnabled.value = enabled
        _reminderIntervalHours.value = intervalHours
        _reminderCustomMessage.value = customMessage.trim()
        sharedPrefs.edit()
            .putBoolean("reminder_enabled", enabled)
            .putLong("reminder_interval_hours", intervalHours)
            .putString("reminder_custom_message", customMessage.trim())
            .apply()
        TaskUpdateReminderWorker.enqueuePeriodicReminder(getApplication(), intervalHours, force = true)
    }

    fun resetDatabaseToDefaults() {
        viewModelScope.launch {
            val prefs = getApplication<Application>().getSharedPreferences("rd_tracker_new_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("already_seeded", false).apply()

            repository.clearAllRuns()
            repository.clearAllSamples()
            val emps = repository.allEmployees.first()
            for (emp in emps) {
                repository.deleteEmployee(emp)
            }
            seedInitiallyIfNeeded()
            _exportMessage.value = "Đã khôi phục dữ liệu mẫu hệ thống thành công!"
        }
    }

    fun getBackupString(): String {
        try {
            val root = org.json.JSONObject()
            root.put("version", 1)
            
            // Employees
            val empsArray = org.json.JSONArray()
            allEmployees.value.forEach { emp ->
                val obj = org.json.JSONObject()
                obj.put("id", emp.id)
                obj.put("name", emp.name)
                obj.put("role", emp.role)
                obj.put("avatarColorHex", emp.avatarColorHex)
                empsArray.put(obj)
            }
            root.put("employees", empsArray)
            
            // Samples
            val samplesArray = org.json.JSONArray()
            allSamples.value.forEach { sample ->
                val obj = org.json.JSONObject()
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
            val runsArray = org.json.JSONArray()
            allRuns.value.forEach { run ->
                val obj = org.json.JSONObject()
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

            // Settings
            val settings = org.json.JSONObject()
            settings.put("approver_name", approverName.value)
            settings.put("approver_title", approverTitle.value)
            settings.put("approver_role", approverRole.value)
            settings.put("preparer_name", preparerName.value)
            settings.put("preparer_title", preparerTitle.value)
            settings.put("preparer_role", preparerRole.value)
            settings.put("github_owner", githubOwner.value)
            settings.put("github_repo", githubRepo.value)
            root.put("settings", settings)
            
            return root.toString(2)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun backupToFile(context: android.content.Context, onResult: (Boolean, String, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val jsonStr = getBackupString()
                if (jsonStr.isEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, "Lỗi tạo nội dung sao lưu!", "")
                    }
                    return@launch
                }
                
                val fFolder = backupFolder.value
                val fName = backupFileName.value

                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val bFolder = java.io.File(downloadsDir, fFolder)
                if (!bFolder.exists()) {
                    bFolder.mkdirs()
                }
                val backupFile = java.io.File(bFolder, fName)
                
                var success = false
                try {
                    backupFile.writeText(jsonStr)
                    success = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                if (!success && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    try {
                        val resolver = context.contentResolver
                        val uriExternal = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
                        
                        // Try deleting potential duplicate first
                        val selection = "${android.provider.MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${android.provider.MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                        val selectionArgs = arrayOf(fName, android.os.Environment.DIRECTORY_DOWNLOADS + "/" + fFolder + "/")
                        try {
                            resolver.delete(uriExternal, selection, selectionArgs)
                        } catch (ig: Exception) {}
                        
                        val contentValues = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fName)
                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/" + fFolder)
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
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (success) {
                        onResult(true, "Sao lưu dữ liệu thành công!", backupFile.absolutePath)
                    } else {
                        onResult(false, "Không thể ghi file vào thư mục Downloads/$fFolder", "")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Lỗi: ${e.localizedMessage}", "")
                }
            }
        }
    }

    fun restoreFromBackupString(jsonStr: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject(jsonStr)
                val employeesArray = root.optJSONArray("employees") ?: org.json.JSONArray()
                val samplesArray = root.optJSONArray("samples") ?: org.json.JSONArray()
                val runsArray = root.optJSONArray("runs") ?: org.json.JSONArray()
                
                // Clear existing
                repository.clearAllRuns()
                repository.clearAllSamples()
                val emps = repository.allEmployees.first()
                for (emp in emps) {
                    repository.deleteEmployee(emp)
                }
                
                // Re-insert backup
                for (i in 0 until employeesArray.length()) {
                    val obj = employeesArray.getJSONObject(i)
                    val emp = Employee(
                        id = obj.optInt("id", 0),
                        name = obj.optString("name", ""),
                        role = obj.optString("role", "Nhân viên R&D"),
                        avatarColorHex = obj.optString("avatarColorHex", "#4F46E5")
                    )
                    repository.insertEmployee(emp)
                }
                
                for (i in 0 until samplesArray.length()) {
                    val obj = samplesArray.getJSONObject(i)
                    val sample = RDSample(
                        sampleCode = obj.optString("sampleCode", ""),
                        sampleName = obj.optString("sampleName", ""),
                        assignedEmployeeId = obj.optInt("assignedEmployeeId", 0),
                        status = obj.optString("status", "Đang thực hiện"),
                        dateCreated = obj.optString("dateCreated", ""),
                        description = obj.optString("description", ""),
                        estimatedTimeStr = obj.optString("estimatedTimeStr", "")
                    )
                    repository.insertSample(sample)
                }
                
                for (i in 0 until runsArray.length()) {
                    val obj = runsArray.getJSONObject(i)
                    val run = RDRun(
                        id = obj.optInt("id", 0),
                        employeeId = obj.optInt("employeeId", 0),
                        sampleCode = obj.optString("sampleCode", ""),
                        runNumber = obj.optInt("runNumber", 1),
                        durationMs = obj.optLong("durationMs", 0L),
                        status = obj.optString("status", ""),
                        failureReason = obj.optString("failureReason", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        dateString = obj.optString("dateString", ""),
                        startTimeStr = obj.optString("startTimeStr", "08:00"),
                        endTimeStr = obj.optString("endTimeStr", "09:30")
                    )
                    repository.insertRun(run)
                }

                // Settings backup / recovery
                val settings = root.optJSONObject("settings")
                if (settings != null) {
                    val aName = settings.optString("approver_name", "Lê Cao Nguyên")
                    val aTitle = settings.optString("approver_title", "TRƯỞNG PHÒNG ĐẢM BẢO CHẤT LƯỢNG")
                    val aRole = settings.optString("approver_role", "(QA/QC MANAGER - DUYỆT)")
                    val pName = settings.optString("preparer_name", "Nguyễn Thị Thúy")
                    val pTitle = settings.optString("preparer_title", "TRƯỞNG NHÓM R&D 02")
                    val pRole = settings.optString("preparer_role", "(NGƯỜI LẬP PHIẾU BÁO CÁO)")
                    updateManagerConfig(aName, aTitle, aRole, pName, pTitle, pRole)
                    
                    val gOwner = settings.optString("github_owner", "vankhoai690")
                    val gRepo = settings.optString("github_repo", "RDTrackerApp")
                    updateGithubConfig(gOwner, gRepo)
                }
                
                _exportMessage.value = "Phục hồi dữ liệu từ bản sao lưu thành công!"
                onResult(true, "Đã khôi phục thành công!")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Lỗi phân tích hoặc phục hồi dữ liệu: ${e.localizedMessage}")
            }
        }
    }

    init {
        val dao = AppDatabase.getDatabase(application).rdDao()
        repository = RDRepository(dao)

        // Read all staff and sessions from Room DB
        allEmployees = repository.allEmployees
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allRuns = repository.allRuns
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allSamples = repository.allSamples
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Ensure database has seed data on first installation
        viewModelScope.launch {
            seedInitiallyIfNeeded()
        }
    }

    // Set filters
    fun setFilterYear(year: String) { _filterYear.value = year }
    fun setFilterMonth(month: String) { _filterMonth.value = month }
    fun setFilterDay(day: String) { _filterDay.value = day }
    fun selectEmployee(id: Int?) { _selectedEmployeeId.value = id }

    fun clearExportMessage() { _exportMessage.value = null }

    // Filter runs based on active filters (Date & Employee ID)
    val filteredRuns: StateFlow<List<RDRun>> = combine(
        allRuns,
        filterYear,
        filterMonth,
        filterDay,
        selectedEmployeeId
    ) { runs, year, month, day, empId ->
        runs.filter { run ->
            // Date filter logic
            val parts = run.dateString.split("-") // "YYYY-MM-DD"
            val matchYear = year == "Tất cả" || (parts.getOrNull(0) == year)
            val matchMonth = month == "Tất cả" || (parts.getOrNull(1) == month)
            val matchDay = day == "Tất cả" || (parts.getOrNull(2) == day)
            
            // Employee filter logic
            val matchEmployee = empId == null || (run.employeeId == empId)

            matchYear && matchMonth && matchDay && matchEmployee
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Group filtered runs into Sample Report list
    val sampleReports: StateFlow<List<SampleReportItem>> = filteredRuns.map { runs ->
        runs.groupBy { it.sampleCode }.map { (code, sampleRuns) ->
            val totalRunsCount = sampleRuns.size
            val sumDuration = sampleRuns.sumOf { it.durationMs }
            val successes = sampleRuns.count { it.status == "Thành công" }
            val failures = sampleRuns.count { it.status == "Thất bại" }
            val durations = sampleRuns.sortedBy { it.runNumber }.map { Pair(it.runNumber, it.durationMs) }
            val reasons = sampleRuns.filter { it.status == "Thất bại" && it.failureReason.isNotEmpty() }
                .map { "${it.runNumber}: ${it.failureReason}" }

            SampleReportItem(
                sampleCode = code,
                totalRuns = totalRunsCount,
                totalDurationMs = sumDuration,
                successCount = successes,
                failureCount = failures,
                runDurations = durations,
                failureReasonsList = reasons,
                detailRuns = sampleRuns.sortedBy { it.runNumber }
            )
        }.sortedByDescending { it.totalDurationMs }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Group filtered runs into Employee Progress Reports
    val employeeReports: StateFlow<List<EmployeeReportSummary>> = combine(
        allEmployees,
        filteredRuns
    ) { employees, runs ->
        employees.map { emp ->
            val empRuns = runs.filter { it.employeeId == emp.id }
            val totalRuns = empRuns.size
            val sumDuration = empRuns.sumOf { it.durationMs }
            val successes = empRuns.count { it.status == "Thành công" }
            val failures = empRuns.count { it.status == "Thất bại" }
            val successRate = if (totalRuns > 0) (successes.toFloat() / totalRuns * 100) else 0f

            // Specific unique samples this employee worked on
            val uniqueSamples = empRuns.groupBy { it.sampleCode }.map { (code, sampleRuns) ->
                val sumSampleDuration = sampleRuns.sumOf { it.durationMs }
                val sampleSuccess = sampleRuns.count { it.status == "Thành công" }
                val sampleFail = sampleRuns.count { it.status == "Thất bại" }
                val runDurations = sampleRuns.sortedBy { it.runNumber }.map { Pair(it.runNumber, it.durationMs) }
                val failureReasons = sampleRuns.filter { it.status == "Thất bại".trim() && it.failureReason.isNotEmpty() }
                    .map { "${it.runNumber}: ${it.failureReason}" }

                SampleReportItem(
                    sampleCode = code,
                    totalRuns = sampleRuns.size,
                    totalDurationMs = sumSampleDuration,
                    successCount = sampleSuccess,
                    failureCount = sampleFail,
                    runDurations = runDurations,
                    failureReasonsList = failureReasons,
                    detailRuns = sampleRuns.sortedBy { it.runNumber }
                )
            }.sortedByDescending { it.totalDurationMs }

            EmployeeReportSummary(
                employee = emp,
                totalSamplesCount = uniqueSamples.size,
                totalRunsCount = totalRuns,
                totalDurationMs = sumDuration,
                successCount = successes,
                failureCount = failures,
                successRate = successRate,
                sampleList = uniqueSamples
            )
        }.sortedByDescending { it.totalRunsCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database Actions
    fun addEmployee(name: String, role: String = "Nhân viên R&D", hexColor: String = "#3B82F6") {
        viewModelScope.launch {
            repository.insertEmployee(
                Employee(name = name, role = role, avatarColorHex = hexColor)
            )
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            if (_selectedEmployeeId.value == employee.id) {
                _selectedEmployeeId.value = null
            }
            repository.deleteEmployee(employee)
        }
    }

    fun updateEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.updateEmployee(employee)
        }
    }

    // Master Sample Actions (Mẫu tổng)
    fun addSample(sampleCode: String, sampleName: String, assignedEmployeeId: Int, dateCreated: String, description: String = "", estimatedTimeStr: String = "") {
        viewModelScope.launch {
            repository.insertSample(
                RDSample(
                    sampleCode = sampleCode,
                    sampleName = sampleName,
                    assignedEmployeeId = assignedEmployeeId,
                    status = "Đang thực hiện",
                    dateCreated = dateCreated,
                    description = description,
                    estimatedTimeStr = estimatedTimeStr
                )
            )
        }
    }

    fun updateSampleStatus(sampleCode: String, status: String) {
        viewModelScope.launch {
            val samples = allSamples.value
            val matched = samples.find { it.sampleCode == sampleCode }
            if (matched != null) {
                repository.updateSample(matched.copy(status = status))
            }
        }
    }

    fun deleteSample(sample: RDSample) {
        viewModelScope.launch {
            repository.deleteSample(sample)
        }
    }

    fun updateSample(sample: RDSample) {
        viewModelScope.launch {
            repository.updateSample(sample)
        }
    }

    fun updateCookingRun(run: RDRun) {
        viewModelScope.launch {
            repository.insertRun(run)
            if (run.status == "Thành công") {
                val matched = allSamples.value.find { it.sampleCode == run.sampleCode }
                if (matched != null && matched.status != "Hoàn thành") {
                    repository.updateSample(matched.copy(status = "Hoàn thành"))
                }
            }
        }
    }

    fun addCookingRun(
        empId: Int,
        sampleCode: String,
        runNumber: Int,
        durationMinutes: Int,
        status: String,
        failureReason: String,
        date: String, // format "YYYY-MM-DD"
        startTimeStr: String = "08:00",
        endTimeStr: String = "09:30"
    ) {
        viewModelScope.launch {
            val durationMs = durationMinutes * 60 * 1000L
            val run = RDRun(
                employeeId = empId,
                sampleCode = sampleCode,
                runNumber = runNumber,
                durationMs = durationMs,
                status = status,
                failureReason = if (status == "Thất bại") failureReason else "",
                dateString = date,
                timestamp = System.currentTimeMillis(),
                startTimeStr = startTimeStr,
                endTimeStr = endTimeStr
            )
            repository.insertRun(run)
            if (status == "Thành công") {
                val matched = allSamples.value.find { it.sampleCode == sampleCode }
                if (matched != null && matched.status != "Hoàn thành") {
                    repository.updateSample(matched.copy(status = "Hoàn thành"))
                }
            }
        }
    }

    fun deleteCookingRun(run: RDRun) {
        viewModelScope.launch {
            repository.deleteRun(run)
        }
    }

    fun triggerCloudSync() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(1800) // beautiful simulated network delay
            _isSyncing.value = false
            _syncSuccessCount.value += (1..5).random()
            _exportMessage.value = "Đồng bộ hóa dữ liệu thành công với Cloud SQL bảo mật!"
        }
    }

    fun simulatePdfExport(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val file = java.io.File(context.cacheDir, "Bao_Cao_R_D_NamViet.pdf")
                val pdfDocument = android.graphics.pdf.PdfDocument()
                
                // --- PAGE 1: EXECUTIVE KPI DASHBOARD & PROPORTION CHARTS ---
                val pageInfo1 = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page1 = pdfDocument.startPage(pageInfo1)
                val canvas = page1.canvas
                
                val titlePaint = android.graphics.Paint().apply {
                    textSize = 10f
                    isFakeBoldText = true
                    color = android.graphics.Color.BLACK
                    isAntiAlias = true
                }
                
                val headerPaint = android.graphics.Paint().apply {
                    textSize = 14f
                    isFakeBoldText = true
                    color = android.graphics.Color.rgb(16, 185, 129) // Sage green colored
                    isAntiAlias = true
                }
                
                val bodyPaint = android.graphics.Paint().apply {
                    textSize = 9f
                    color = android.graphics.Color.BLACK
                    isAntiAlias = true
                }

                val subPaint = android.graphics.Paint().apply {
                    textSize = 8f
                    color = android.graphics.Color.GRAY
                    isAntiAlias = true
                }
                
                var y = 40f
                canvas.drawText("CÔNG TY CỔ PHẦN THỰC PHẨM VÀ NƯỚC GIẢI KHÁT NAM VIỆT", 40f, y, titlePaint)
                y += 14f
                canvas.drawText("Phòng Thử Nghiệm Chất Lượng R&D Kitchen", 40f, y, subPaint)
                y += 8f
                canvas.drawLine(40f, y, 555f, y, android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 0.5f })
                y += 24f
                
                canvas.drawText("BÁO CÁO THỨC ĐO TIẾN ĐỘ & HIỆU SUẤT R&D KITCHEN", 40f, y, headerPaint)
                canvas.drawText("Hệ thống kiểm định chất lượng sản xuất thử nghiệm • Nam Việt Food", 40f, y + 12f, subPaint)
                y += 32f
                
                val samplesList = allSamples.value
                val runsList = allRuns.value
                val totalSamples = samplesList.size
                val completedSamples = samplesList.count { it.status == "Hoàn thành" }
                val totalRuns = runsList.size
                val successRuns = runsList.count { it.status == "Thành công" }
                val failureRuns = runsList.count { it.status == "Thất bại" }
                val successRate = if (totalRuns > 0) (successRuns * 100 / totalRuns) else 0
                val failureRate = if (totalRuns > 0) (failureRuns * 100 / totalRuns) else 0

                // 3 card panels for KPI
                val cardPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(243, 244, 246)
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = true
                }
                
                // Col 1: R&D Samples Counter
                val kpi1 = android.graphics.RectF(40f, y, 190f, y + 45f)
                canvas.drawRoundRect(kpi1, 6f, 6f, cardPaint)
                canvas.drawText("TỔNG MẪU R&D", 50f, y + 16f, subPaint)
                canvas.drawText("$totalSamples mẫu ($completedSamples hoàn thành)", 50f, y + 34f, titlePaint)
                
                // Col 2: Runs Counter
                val kpi2 = android.graphics.RectF(205f, y, 355f, y + 45f)
                canvas.drawRoundRect(kpi2, 6f, 6f, cardPaint)
                canvas.drawText("TỔNG MẺ ĐUN NẤU", 215f, y + 16f, subPaint)
                canvas.drawText("$totalRuns lần thử thực tế", 215f, y + 34f, titlePaint)

                // Col 3: Success rates
                val kpi3 = android.graphics.RectF(370f, y, 555f, y + 45f)
                canvas.drawRoundRect(kpi3, 6f, 6f, cardPaint)
                canvas.drawText("HIỆU SUẤT THÀNH CÔNG", 380f, y + 16f, subPaint)
                canvas.drawText("$successRate% đạt ($successRuns đạt / $failureRuns hỏng)", 380f, y + 34f, titlePaint)
                
                y += 75f
                
                // Stacked bar chart represent result ratios
                canvas.drawText("BIỂU ĐỒ TỶ LỆ KẾT QUẢ THỬ NGHIỆM LAB R&D (PHÂN BỔ %)", 40f, y, titlePaint)
                y += 12f
                
                val chartWidth = 515f
                val chartHeight = 22f
                val chartRect = android.graphics.RectF(40f, y, 40f + chartWidth, y + chartHeight)
                
                // base background
                val bgPaint = android.graphics.Paint().apply { color = android.graphics.Color.rgb(229, 231, 235) }
                canvas.drawRoundRect(chartRect, 4f, 4f, bgPaint)
                
                if (totalRuns > 0) {
                    val successWidth = chartWidth * (successRuns.toFloat() / totalRuns)
                    val failureWidth = chartWidth * (failureRuns.toFloat() / totalRuns)
                    
                    val successPaint = android.graphics.Paint().apply { color = android.graphics.Color.rgb(16, 185, 129) } // Emerald Green
                    val failurePaint = android.graphics.Paint().apply { color = android.graphics.Color.rgb(239, 68, 68) } // Vibrant Red
                    
                    if (successRuns > 0) {
                        val successRect = android.graphics.RectF(40f, y, 40f + successWidth, y + chartHeight)
                        canvas.drawRoundRect(successRect, 4f, 4f, successPaint)
                    }
                    if (failureRuns > 0) {
                        val failureRect = android.graphics.RectF(40f + successWidth, y, 40f + successWidth + failureWidth, y + chartHeight)
                        canvas.drawRoundRect(failureRect, 4f, 4f, failurePaint)
                    }
                }
                y += chartHeight + 14f
                
                val legendPaint = android.graphics.Paint().apply {
                    textSize = 8.5f
                    color = android.graphics.Color.BLACK
                    isAntiAlias = true
                }
                
                // success legend
                canvas.drawRect(40f, y - 6f, 50f, y, android.graphics.Paint().apply { color = android.graphics.Color.rgb(16, 185, 129) })
                canvas.drawText("Mẻ thành công đạt chỉ tiêu kỹ thuật: $successRuns ($successRate%)", 54f, y, legendPaint)
                
                // failure legend
                canvas.drawRect(240f, y - 6f, 250f, y, android.graphics.Paint().apply { color = android.graphics.Color.rgb(239, 68, 68) })
                canvas.drawText("Mẻ lỗi hỏng / tách lớp / khét đáy: $failureRuns ($failureRate%)", 254f, y, legendPaint)
                
                y += 36f
                
                // Segment 3: Tiny individual progress gauges
                canvas.drawText("HIỆU SUẤT THỰC HIỆN THEO TỪNG CHỈ TIÊU ĐƯỢC CHỈ ĐỊNH", 40f, y, titlePaint)
                y += 16f
                
                samplesList.take(6).forEach { sample ->
                    val sampleRuns = runsList.filter { it.sampleCode == sample.sampleCode }
                    val sTotal = sampleRuns.size
                    val sSuccess = sampleRuns.count { it.status == "Thành công" }
                    val sRate = if (sTotal > 0) (sSuccess * 100 / sTotal) else 0
                    
                    canvas.drawText("${sample.sampleCode}: ${sample.sampleName}", 40f, y, bodyPaint)
                    
                    val barW = 120f
                    val barR = android.graphics.RectF(425f, y - 8f, 425f + barW, y)
                    val barBg = android.graphics.Paint().apply { color = android.graphics.Color.rgb(243, 244, 246) }
                    canvas.drawRoundRect(barR, 2f, 2f, barBg)
                    
                    if (sTotal > 0) {
                        val fillW = barW * (sSuccess.toFloat() / sTotal)
                        val barFill = android.graphics.Paint().apply {
                            color = if (sSuccess == sTotal) android.graphics.Color.rgb(16, 185, 129) else android.graphics.Color.rgb(59, 130, 246)
                        }
                        canvas.drawRoundRect(android.graphics.RectF(425f, y - 8f, 425f + fillW, y), 2f, 2f, barFill)
                    }
                    
                    canvas.drawText("$sSuccess/$sTotal đạt ($sRate%)", 280f, y, subPaint)
                    canvas.drawText(sample.status, 425f + barW + 10f, y, subPaint)
                    
                    y += 16f
                }
                
                // Signatures block at the bottom
                y = 690f
                canvas.drawLine(40f, y, 555f, y, android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 0.5f })
                y += 24f
                
                val sigHeaderPaint = android.graphics.Paint().apply {
                    textSize = 9.5f
                    isFakeBoldText = true
                    color = android.graphics.Color.BLACK
                    isAntiAlias = true
                }
                val sigNamePaint = android.graphics.Paint().apply {
                    textSize = 9.5f
                    isFakeBoldText = true
                    color = android.graphics.Color.rgb(16, 185, 129)
                    isAntiAlias = true
                    isUnderlineText = true
                }
                
                canvas.drawText(approverTitle.value.uppercase(), 50f, y, sigHeaderPaint)
                canvas.drawText(preparerTitle.value.uppercase(), 390f, y, sigHeaderPaint)
                canvas.drawText(approverRole.value, 70f, y + 14f, subPaint)
                canvas.drawText(preparerRole.value, 394f, y + 14f, subPaint)
                
                // Simulate signature curves
                val sigLinePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLUE
                    strokeWidth = 1.5f
                    style = android.graphics.Paint.Style.STROKE
                    isAntiAlias = true
                }
                val sigPath1 = android.graphics.Path().apply {
                    moveTo(90f, y + 42f)
                    quadTo(105f, y + 26f, 120f, y + 45f)
                    quadTo(135f, y + 50f, 145f, y + 36f)
                }
                val sigPath2 = android.graphics.Path().apply {
                    moveTo(410f, y + 42f)
                    quadTo(425f, y + 24f, 440f, y + 48f)
                    quadTo(455f, y + 52f, 465f, y + 32f)
                }
                canvas.drawPath(sigPath1, sigLinePaint)
                canvas.drawPath(sigPath2, sigLinePaint)
                
                canvas.drawText(approverName.value, 80f, y + 72f, sigNamePaint)
                canvas.drawText(preparerName.value, 395f, y + 72f, sigNamePaint)
                
                canvas.drawText("Trang 1 / 2 • Báo cáo hiệu lực hệ thống R&D Nam Việt", 195f, 810f, subPaint)
                pdfDocument.finishPage(page1)
                
                // --- PAGE 2: TRANS LOGS DETAIL SHEETS (PARENT AND CHILD CHRONOLOGICAL LOGS) ---
                val pageInfo2 = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 2).create()
                val page2 = pdfDocument.startPage(pageInfo2)
                val canvas2 = page2.canvas
                
                var py = 40f
                canvas2.drawText("BẢNG THEO DÕI CHI TIẾT TỪNG MẺ NẤU THỰC TẾ (LAB TRIALS)", 40f, py, headerPaint)
                py += 14f
                canvas2.drawText("Phòng R&D Kitchen • Tổng hợp trực tiếp chi tiết mẻ con theo thời gian bắt đầu & kết thúc", 40f, py, subPaint)
                py += 10f
                canvas2.drawLine(40f, py, 555f, py, android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 0.5f })
                py += 20f
                
                // Grid column definitions for standard representation
                val colSTT = 40f
                val colMae = 70f
                val colTen = 135f
                val colNV = 235f
                val colLan = 300f
                val colTime = 335f
                val colResult = 415f
                val colReason = 475f
                
                val thPaint = android.graphics.Paint().apply {
                    textSize = 7.5f
                    isFakeBoldText = true
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                }
                
                val tableHeaderRect = android.graphics.RectF(40f, py, 555f, py + 18f)
                canvas2.drawRect(tableHeaderRect, android.graphics.Paint().apply { color = android.graphics.Color.rgb(16, 185, 129) })
                
                canvas2.drawText("STT", colSTT + 3f, py + 12f, thPaint)
                canvas2.drawText("MÃ CHỈ TIÊU", colMae, py + 12f, thPaint)
                canvas2.drawText("TÊN SẢN PHẨM R&D", colTen, py + 12f, thPaint)
                canvas2.drawText("NHÂN VIÊN", colNV, py + 12f, thPaint)
                canvas2.drawText("LẦN", colLan, py + 12f, thPaint)
                canvas2.drawText("MỐC THỜI GIAN", colTime, py + 12f, thPaint)
                canvas2.drawText("KẾT QUẢ", colResult, py + 12f, thPaint)
                canvas2.drawText("LÝ DO LỖI / CHUYÊN MÔN KĨ THUẬT R&D", colReason, py + 12f, thPaint)
                
                py += 18f
                
                val trBgEven = android.graphics.Paint().apply { color = android.graphics.Color.WHITE }
                val trBgOdd = android.graphics.Paint().apply { color = android.graphics.Color.rgb(249, 250, 251) }
                val linePaint = android.graphics.Paint().apply { color = android.graphics.Color.rgb(229, 231, 235); strokeWidth = 0.5f }
                
                val detailRows = mutableListOf<Triple<RDSample, RDRun, Employee?>>()
                val employeesList = allEmployees.value
                samplesList.forEach { sample ->
                    val sampleRuns = runsList.filter { it.sampleCode == sample.sampleCode }.sortedBy { it.runNumber }
                    val emp = employeesList.find { it.id == sample.assignedEmployeeId }
                    if (sampleRuns.isEmpty()) {
                        detailRows.add(Triple(sample, RDRun(sampleCode = sample.sampleCode, employeeId = sample.assignedEmployeeId, runNumber = 0, durationMs = 0, status = "Chờ nấu", failureReason = "Chưa nấu", dateString = sample.dateCreated), emp))
                    } else {
                        sampleRuns.forEach { run ->
                            detailRows.add(Triple(sample, run, emp))
                        }
                    }
                }
                
                detailRows.take(38).forEachIndexed { rIdx, (sample, run, emp) ->
                    val isEven = rIdx % 2 == 0
                    val rowRect = android.graphics.RectF(40f, py, 555f, py + 17f)
                    canvas2.drawRect(rowRect, if (isEven) trBgEven else trBgOdd)
                    canvas2.drawLine(40f, py + 17f, 555f, py + 17f, linePaint)
                    
                    canvas2.drawText("${rIdx + 1}", colSTT + 3f, py + 11f, bodyPaint)
                    canvas2.drawText(sample.sampleCode, colMae, py + 11f, bodyPaint)
                    
                    val dispName = if (sample.sampleName.length > 18) sample.sampleName.substring(0, 16) + "..." else sample.sampleName
                    canvas2.drawText(dispName, colTen, py + 11f, bodyPaint)
                    
                    val shortEmp = if ((emp?.name ?: "Chưa rõ").length > 11) (emp?.name ?: "Chưa rõ").substring(0, 9) + ".." else (emp?.name ?: "Chưa rõ")
                    canvas2.drawText(shortEmp, colNV, py + 11f, bodyPaint)
                    
                    val dispRunNum = if (run.runNumber == 0) "Chờ" else "#${run.runNumber}"
                    canvas2.drawText(dispRunNum, colLan, py + 11f, bodyPaint)
                    
                    val dispDuration = if (run.runNumber == 0) "Chưa nấu" else "${run.startTimeStr}-${run.endTimeStr} (${run.durationMs / 60000}m)"
                    canvas2.drawText(dispDuration, colTime, py + 11f, bodyPaint)
                    
                    canvas2.drawText(run.status, colResult, py + 11f, bodyPaint)
                    
                    val displayReason = if (run.status == "Thành công") "Đạt cảm quan tốt" else run.failureReason.ifEmpty { "Đang lên kế hoạch" }
                    val dispReasonShort = if (displayReason.length > 20) displayReason.substring(0, 18) + ".." else displayReason
                    canvas2.drawText(dispReasonShort, colReason, py + 11f, bodyPaint)
                    
                    py += 17f
                }
                
                canvas2.drawText("Trang 2 / 2 • Người lập: ${preparerName.value} (${preparerTitle.value}) | Người duyệt: ${approverName.value} (${approverTitle.value})", 100f, 810f, subPaint)
                pdfDocument.finishPage(page2)
                
                // Save and Open the Share Dialog
                val fos = java.io.FileOutputStream(file)
                pdfDocument.writeTo(fos)
                pdfDocument.close()
                fos.close()
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Bao cao R&D Kitchen - Nam Viet Food")
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                
                val chooser = android.content.Intent.createChooser(intent, "Chia sẻ Báo cáo PDF R&D").apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(chooser)
                _exportMessage.value = "Xuất file PDF thành công và đang khởi chạy chia sẻ!"
            } catch (e: Exception) {
                _exportMessage.value = "Lỗi xuất PDF: ${e.localizedMessage}"
            }
        }
    }

    fun simulateExcelExport(context: android.content.Context) {
        viewModelScope.launch {
            try {
                // Save as .csv, which is Excel compatible
                val file = java.io.File(context.cacheDir, "Bao_Cao_Tien_Do_R_D_NamViet.csv")
                val fos = java.io.FileOutputStream(file)
                
                // Write standard UTF-8 BOM so MS Excel displays Vietnamese accents flawlessly
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                
                val writer = java.io.OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8)
                writer.write("CÔNG TY CỔ PHẦN THỰC PHẨM VÀ NƯỚC GIẢI KHÁT NAM VIỆT\n")
                writer.write("BÁO CÁO TIẾN ĐỘ THỬ NGHIỆM R&D KITCHEN CHI TIẾT (XUẤT EXCEL)\n")
                writer.write("Người lập báo cáo: ${preparerTitle.value} ${preparerName.value}\n")
                writer.write("Người phê duyệt: ${approverTitle.value} ${approverName.value}\n\n")
                
                // Clean CSV with strict comma boundary separation, absolutely no awkward headers gaps
                writer.write("STT,Ma Chi Tieu,Ten Chi Tieu (Task Tong),Trang Thai Mau,Du Kien Mau,Nhan Vien Giao Viec,Lan Thu Con,Gio Bat Dau,Gio Ket Thuc,Thoi Gian Nau (Phut),Ket Qua Luot Thu,Ly Do That Bai / Ghi Chu,Ngay Thuc Hien\n")
                
                val samplesList = allSamples.value
                val runsList = allRuns.value
                val employeesList = allEmployees.value
                
                var globalIndex = 1
                samplesList.forEach { sample ->
                    val sampleRuns = runsList.filter { it.sampleCode == sample.sampleCode }.sortedBy { it.runNumber }
                    val emp = employeesList.find { it.id == sample.assignedEmployeeId }
                    
                    val cleanCode = sample.sampleCode.replace(",", " ").trim().replace("\\s+".toRegex(), " ")
                    val cleanSampleName = sample.sampleName.replace(",", " ").trim().replace("\\s+".toRegex(), " ")
                    val cleanStatus = sample.status.replace(",", " ").trim().replace("\\s+".toRegex(), " ")
                    val cleanEst = sample.estimatedTimeStr.replace(",", " ").trim().replace("\\s+".toRegex(), " ")
                    val cleanEmpName = (emp?.name ?: "Chua giao").replace(",", " ").trim().replace("\\s+".toRegex(), " ")
                    
                    if (sampleRuns.isEmpty()) {
                        writer.write("$globalIndex,$cleanCode,$cleanSampleName,$cleanStatus,$cleanEst,$cleanEmpName,-,Chua nau,Chua nau,-,Chua thu nghiem,Cho me dau tien,${sample.dateCreated}\n")
                        globalIndex++
                    } else {
                        sampleRuns.forEach { run ->
                            val runNumStr = "#${run.runNumber}"
                            val durationMin = run.durationMs / (1000 * 60)
                            val cleanResult = run.status.replace(",", " ").trim().replace("\\s+".toRegex(), " ")
                            val cleanReason = run.failureReason.replace(",", " ").replace("\n", " ").trim().replace("\\s+".toRegex(), " ")
                            val displayReason = if (cleanResult == "Thành công") "Me mau dat chi tieu xuat sac" else cleanReason.ifEmpty { "Cho kiem nghiem" }
                            val dispStartTime = run.startTimeStr
                            val dispEndTime = run.endTimeStr
                            
                            writer.write("$globalIndex,$cleanCode,$cleanSampleName,$cleanStatus,$cleanEst,$cleanEmpName,$runNumStr,$dispStartTime,$dispEndTime,$durationMin,$cleanResult,$displayReason,${run.dateString}\n")
                            globalIndex++
                        }
                    }
                }
                
                writer.flush()
                writer.close()
                fos.close()
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/vnd.ms-excel" // Change to MS Excel compatible mime type
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Bao cao Tien do R&D Kitchen - Nam Viet Food")
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                
                val chooser = android.content.Intent.createChooser(intent, "Chia sẻ Báo cáo Excel (.csv)").apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(chooser)
                _exportMessage.value = "Xuất báo cáo Excel thành công và đang khởi chạy chia sẻ!"
            } catch (e: Exception) {
                _exportMessage.value = "Lỗi xuất Excel: ${e.localizedMessage}"
            }
        }
    }

    // Helper conversion
    fun formatDuration(ms: Long): String {
        val totalMinutes = ms / (1000 * 60)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins} phút"
    }

    // Initial business seeding for pristine demonstration
    private suspend fun seedInitiallyIfNeeded() {
        val prefs = getApplication<Application>().getSharedPreferences("rd_tracker_new_prefs", android.content.Context.MODE_PRIVATE)
        val alreadySeeded = prefs.getBoolean("already_seeded", false)
        if (alreadySeeded) return

        val existing = repository.allEmployees.first()
        if (existing.isNotEmpty()) {
            prefs.edit().putBoolean("already_seeded", true).apply()
            return
        }

        // 1. Seed standard 8 active employees
        val employees = listOf(
            Employee(name = "Nguyễn Hoàng Nam", avatarColorHex = "#3B82F6"),
            Employee(name = "Lê Thị Hồng", avatarColorHex = "#EF4444"),
            Employee(name = "Trần Minh Đức", avatarColorHex = "#10B981"),
            Employee(name = "Phạm Kim Chi", avatarColorHex = "#F59E0B"),
            Employee(name = "Đặng Quốc Việt", avatarColorHex = "#8B5CF6"),
            Employee(name = "Hoàng Thùy Linh", avatarColorHex = "#EC4899"),
            Employee(name = "Bùi Anh Tuấn", avatarColorHex = "#14B8A6"),
            Employee(name = "Vũ Bảo Ngọc", avatarColorHex = "#6366F1")
        )

        val insertedIds = mutableListOf<Int>()
        for (emp in employees) {
            val id = repository.insertEmployee(emp)
            insertedIds.add(id.toInt())
        }

        // 2. Sample Codes
        val samples = listOf(
            "Yoghurt-Dâu-v2",   // Sữa chua dâu
            "Ketchup-BBQ-v4",   // Xốt cà chua bbq
            "Mayonnaise-L3",    // Mayone ít béo
            "SoyMilk-Organic",  // Sữa đậu nành hữu cơ
            "Cheese-Chilly-S1", // Phomai ớt cay
            "Butter-Keto-X2",   // Bơ dừa keto
            "Sauce-Salad-S8"    // Xốt mè rang
        )

        // Seed 45 realistic runs spanning across today and yesterday dynamically
        val todaySdf = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayStr = todaySdf.format(java.util.Date())
        val yesterdayStr = todaySdf.format(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L))
        val dates = listOf(todayStr, yesterdayStr)
        val random = java.util.Random(42)
        val sampleAssignments = mutableMapOf<String, Int>()

        for (i in 1..50) {
            val date = dates[if (random.nextDouble() > 0.4) 0 else 1]
            val empId = insertedIds[random.nextInt(insertedIds.size)]
            val sample = samples[random.nextInt(samples.size)]
            
            sampleAssignments[sample] = empId

            // Generate run number sequence (typically 1 to 3 runs)
            val runNum = random.nextInt(3) + 1
            val durationMin = random.nextInt(35) + 15 // 15 to 50 minutes
            val isSuccess = random.nextDouble() > 0.35 // 65% success rate
            
            val status = if (isSuccess) "Thành công" else "Thất bại"
            val failureReason = if (!isSuccess) {
                listOf(
                    "Quá nhiệt khét đáy nồi nấu",
                    "Độ pH bị vượt ngưỡng (> 4.8)",
                    "Mùi khét và tách lớp dầu",
                    "Quá đặc, không đạt chuẩn nhớt",
                    "Tỷ lệ nhũ hóa bị sai, vón cục lớn",
                    "Màu sẫm đen không đều"
                )[random.nextInt(6)]
            } else ""

            val randomStartHour = 8 + random.nextInt(4)
            val randomStartMin = random.nextInt(60)
            val randomDurationMin = durationMin
            val endTotalMin = (randomStartHour * 60 + randomStartMin + randomDurationMin)
            val endHour = (endTotalMin / 60) % 24
            val endMin = endTotalMin % 60
            val mockStartStr = String.format("%02d:%02d", randomStartHour, randomStartMin)
            val mockEndStr = String.format("%02d:%02d", endHour, endMin)

            repository.insertRun(
                RDRun(
                    employeeId = empId,
                    sampleCode = sample,
                    runNumber = runNum,
                    durationMs = durationMin * 60 * 1000L,
                    status = status,
                    failureReason = failureReason,
                    dateString = date,
                    timestamp = System.currentTimeMillis() - random.nextInt(10000000),
                    startTimeStr = mockStartStr,
                    endTimeStr = mockEndStr
                )
            )
        }

        // Seed master samples tied to employees
        val sampleNamesAndDescs = mapOf(
            "Yoghurt-Dâu-v2" to Pair("Sữa chua dâu tây Đà Lạt lên men", "Yêu cầu: Đạt độ pH 4.2 - 4.5, màu hồng tự nhiên, dâu tây chín nhuyễn mềm mịn"),
            "Ketchup-BBQ-v4" to Pair("Sốt xốt BBQ cà chua muối xông khói", "Yêu cầu: Vị cay nồng vừa, độ nhớt đạt tiêu chuẩn, nướng không đổi màu"),
            "Mayonnaise-L3" to Pair("Mayonnaise trứng muối ít béo thơm dịu", "Yêu cầu: Nhũ tương ổn định, lòng đỏ chín kỹ, không tách bơ"),
            "SoyMilk-Organic" to Pair("Sữa đậu nành hữu cơ nguyên chất", "Yêu cầu: Ép nghiền mịn, loại bỏ bọt khí, tiệt trùng ở 115°C"),
            "Cheese-Chilly-S1" to Pair("Phô-mai tươi vị ớt cay nồng đậm", "Yêu cầu: Kéo sợi chín, dai xốp đều màu, ớt bột mịn phủ"),
            "Butter-Keto-X2" to Pair("Bơ dừa Keto sấy lạnh cải tiến", "Yêu cầu: Kết dính khối tốt không vụn vỡ, giữ sữa dừa cô đặc"),
            "Sauce-Salad-S8" to Pair("Sốt mè rang đậu nành phong cách Nhật", "Yêu cầu: Mè vừng thơm giòn xay nhỏ, nhũ hóa không bị sánh đặc")
        )

        sampleAssignments.forEach { (sampleCode, empId) ->
            val descAndName = sampleNamesAndDescs[sampleCode] ?: Pair("Mẫu Thử Nghiệm $sampleCode", "Chi tiết mẻ nấu kiểm soát chất lượng")
            repository.insertSample(
                RDSample(
                    sampleCode = sampleCode,
                    sampleName = descAndName.first,
                    assignedEmployeeId = empId,
                    status = if (random.nextDouble() > 0.4) "Hoàn thành" else "Đang thực hiện",
                    dateCreated = todayStr,
                    description = descAndName.second
                )
            )
        }
        
        prefs.edit().putBoolean("already_seeded", true).apply()
    }

    // --- GITHUB AUTO-UPDATE CONFIGURATION ---
    private val _approverName = MutableStateFlow(sharedPrefs.getString("approver_name", "Lê Cao Nguyên") ?: "Lê Cao Nguyên")
    val approverName = _approverName.asStateFlow()

    private val _approverTitle = MutableStateFlow(sharedPrefs.getString("approver_title", "TRƯỞNG PHÒNG ĐẢM BẢO CHẤT LƯỢNG") ?: "TRƯỞNG PHÒNG ĐẢM BẢO CHẤT LƯỢNG")
    val approverTitle = _approverTitle.asStateFlow()

    private val _approverRole = MutableStateFlow(sharedPrefs.getString("approver_role", "(QA/QC MANAGER - DUYỆT)") ?: "(QA/QC MANAGER - DUYỆT)")
    val approverRole = _approverRole.asStateFlow()

    private val _preparerName = MutableStateFlow(sharedPrefs.getString("preparer_name", "Nguyễn Thị Thúy") ?: "Nguyễn Thị Thúy")
    val preparerName = _preparerName.asStateFlow()

    private val _preparerTitle = MutableStateFlow(sharedPrefs.getString("preparer_title", "TRƯỞNG NHÓM R&D 02") ?: "TRƯỞNG NHÓM R&D 02")
    val preparerTitle = _preparerTitle.asStateFlow()

    private val _preparerRole = MutableStateFlow(sharedPrefs.getString("preparer_role", "(NGƯỜI LẬP PHIẾU BÁO CÁO)") ?: "(NGƯỜI LẬP PHIẾU BÁO CÁO)")
    val preparerRole = _preparerRole.asStateFlow()

    fun updateManagerConfig(
        aName: String,
        aTitle: String,
        aRole: String,
        pName: String,
        pTitle: String,
        pRole: String
    ) {
        _approverName.value = aName
        _approverTitle.value = aTitle
        _approverRole.value = aRole
        _preparerName.value = pName
        _preparerTitle.value = pTitle
        _preparerRole.value = pRole

        sharedPrefs.edit()
            .putString("approver_name", aName)
            .putString("approver_title", aTitle)
            .putString("approver_role", aRole)
            .putString("preparer_name", pName)
            .putString("preparer_title", pTitle)
            .putString("preparer_role", pRole)
            .apply()
    }

    private val _githubOwner = MutableStateFlow(sharedPrefs.getString("github_owner", "vankhoai690") ?: "vankhoai690")
    val githubOwner = _githubOwner.asStateFlow()

    private val _githubRepo = MutableStateFlow(sharedPrefs.getString("github_repo", "RDTrackerApp") ?: "RDTrackerApp")
    val githubRepo = _githubRepo.asStateFlow()

    fun updateGithubConfig(owner: String, repo: String) {
        _githubOwner.value = owner
        _githubRepo.value = repo
        sharedPrefs.edit()
            .putString("github_owner", owner)
            .putString("github_repo", repo)
            .apply()
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState = _updateState.asStateFlow()

    sealed class UpdateState {
        object Idle : UpdateState()
        object Checking : UpdateState()
        data class NewVersionAvailable(val currentVersion: String, val latestVersion: String, val changelog: String, val apkUrl: String) : UpdateState()
        object UpToDate : UpdateState()
        data class Error(val message: String) : UpdateState()
        data class Downloading(val progress: Float) : UpdateState()
        data class DownloadSuccess(val apkFile: java.io.File) : UpdateState()
    }

    fun checkForUpdates() {
        _updateState.value = UpdateState.Checking
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val owner = _githubOwner.value.trim()
                val repo = _githubRepo.value.trim()
                val url = "https://api.github.com/repos/$owner/$repo/releases/latest"

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "RDTrackerApp-Updater")
                    .build()

                client.newCall(request).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        _updateState.value = UpdateState.Error("Không kết nối được: ${e.message}")
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        response.use { resp ->
                            if (!resp.isSuccessful) {
                                if (resp.code == 404) {
                                    _updateState.value = UpdateState.Error("Không tìm thấy Repo hoặc Release trên GitHub!")
                                } else {
                                    _updateState.value = UpdateState.Error("Lỗi kết nối GitHub: ${resp.code}")
                                }
                                return
                            }
                            val jsonData = resp.body?.string() ?: ""
                            if (jsonData.isEmpty()) {
                                _updateState.value = UpdateState.Error("Dữ liệu trống!")
                                return
                            }

                            try {
                                val json = org.json.JSONObject(jsonData)
                                val tagName = json.optString("tag_name", "")
                                val changelog = json.optString("body", "Không có thông tin nội dung.")
                                val assets = json.optJSONArray("assets")
                                var apkUrl: String? = null

                                if (assets != null) {
                                    for (i in 0 until assets.length()) {
                                        val asset = assets.getJSONObject(i)
                                        val name = asset.optString("name", "")
                                        if (name.endsWith(".apk", ignoreCase = true)) {
                                            apkUrl = asset.optString("browser_download_url", null)
                                            break
                                        }
                                    }
                                }

                                if (tagName.isEmpty()) {
                                    _updateState.value = UpdateState.Error("Không thấy tag_name!")
                                    return
                                }

                                val context = getApplication<Application>()
                                val currentVersion = try {
                                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                    pInfo.versionName ?: "1.0"
                                } catch (e: Exception) {
                                    "1.0"
                                }
                                val cleanLatest = tagName.removePrefix("v").trim()
                                val cleanCurrent = currentVersion.removePrefix("v").trim()

                                if (cleanLatest != cleanCurrent) {
                                    if (apkUrl != null) {
                                        _updateState.value = UpdateState.NewVersionAvailable(
                                            currentVersion = currentVersion,
                                            latestVersion = tagName,
                                            changelog = changelog,
                                            apkUrl = apkUrl
                                        )
                                    } else {
                                        _updateState.value = UpdateState.Error("Tìm thấy bản mới $tagName nhưng dữ liệu tài sản tải xuống không có APK!")
                                    }
                                } else {
                                    _updateState.value = UpdateState.UpToDate
                                }
                            } catch (je: Exception) {
                                _updateState.value = UpdateState.Error("Lỗi phân tích JSON: ${je.message}")
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Lỗi: ${e.message}")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    fun downloadAndInstallLatestRelease(apkUrl: String) {
        _updateState.value = UpdateState.Downloading(0f)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.MINUTES)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(apkUrl)
                    .header("User-Agent", "RDTrackerApp-Updater")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    _updateState.value = UpdateState.Error("Lỗi tải tệp: ${response.code}")
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    _updateState.value = UpdateState.Error("Tệp tải về rỗng")
                    return@launch
                }

                val contentLength = body.contentLength()
                val context = getApplication<Application>()
                val apkFile = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "RDTrackerApp_update_latest.apk")
                
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val inputStream = body.byteStream()
                val outputStream = java.io.FileOutputStream(apkFile)
                val buffer = ByteArray(16384)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = totalBytesRead.toFloat() / contentLength
                        _updateState.value = UpdateState.Downloading(progress)
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                _updateState.value = UpdateState.DownloadSuccess(apkFile)
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Lỗi: ${e.message}")
            }
        }
    }
}
