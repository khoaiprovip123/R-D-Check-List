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
    private val _targetKpiSuccessRate = MutableStateFlow(80)
    val targetKpiSuccessRate = _targetKpiSuccessRate.asStateFlow()

    private val _targetCookingDurationMin = MutableStateFlow(45)
    val targetCookingDurationMin = _targetCookingDurationMin.asStateFlow()

    private val _autoSyncIntervalMin = MutableStateFlow(15)
    val autoSyncIntervalMin = _autoSyncIntervalMin.asStateFlow()

    private val _selectedThemeColorHex = MutableStateFlow("#3B82F6") // Primary Blue
    val selectedThemeColorHex = _selectedThemeColorHex.asStateFlow()

    fun updateTargetKpi(rate: Int) { _targetKpiSuccessRate.value = rate }
    fun updateTargetCookingDuration(duration: Int) { _targetCookingDurationMin.value = duration }
    fun updateAutoSyncInterval(interval: Int) { _autoSyncIntervalMin.value = interval }
    fun updateThemeColor(colorHex: String) { _selectedThemeColorHex.value = colorHex }

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
                runsArray.put(obj)
            }
            root.put("runs", runsArray)
            
            return root.toString(2)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
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
                        dateString = obj.optString("dateString", "")
                    )
                    repository.insertRun(run)
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
        date: String // format "YYYY-MM-DD"
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
                timestamp = System.currentTimeMillis()
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

    fun simulatePdfExport() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            _exportMessage.value = "Xuất file PDF thành công! Đã lưu trữ tại mục Downloads/Bao_Cao_R&D_ThuyNguyen.pdf"
        }
    }

    fun simulateExcelExport() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            _exportMessage.value = "Xuất file EXCEL thành công! Đã lưu trữ tại mục Downloads/Bao_Cao_Tien_Do_R&D.xlsx"
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

            repository.insertRun(
                RDRun(
                    employeeId = empId,
                    sampleCode = sample,
                    runNumber = runNum,
                    durationMs = durationMin * 60 * 1000L,
                    status = status,
                    failureReason = failureReason,
                    dateString = date,
                    timestamp = System.currentTimeMillis() - random.nextInt(10000000)
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
    private val sharedPrefs = application.getSharedPreferences("rd_tracker_new_prefs", android.content.Context.MODE_PRIVATE)

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
