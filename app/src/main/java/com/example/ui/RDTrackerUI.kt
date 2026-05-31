package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import com.example.ui.theme.SageOutlineVariant
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RDTrackerApp(viewModel: RDViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val filteredRuns by viewModel.filteredRuns.collectAsStateWithLifecycle()
    val sampleReports by viewModel.sampleReports.collectAsStateWithLifecycle()
    val employeeReports by viewModel.employeeReports.collectAsStateWithLifecycle()

    val currentYear by viewModel.filterYear.collectAsStateWithLifecycle()
    val currentMonth by viewModel.filterMonth.collectAsStateWithLifecycle()
    val currentDay by viewModel.filterDay.collectAsStateWithLifecycle()
    val selectedEmpId by viewModel.selectedEmployeeId.collectAsStateWithLifecycle()

    val exportMsg by viewModel.exportMessage.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val cloudCount by viewModel.syncSuccessCount.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    val activeTab = pagerState.currentPage
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var showQuickTaskDialog by remember { mutableStateOf(false) }
    var selectedEmployeeDetail by remember { mutableStateOf<EmployeeReportSummary?>(null) }
    var expandedSampleDetailInApp by remember { mutableStateOf<RDSample?>(null) }

    var activeActionSample by remember { mutableStateOf<RDSample?>(null) }
    var activeActionRun by remember { mutableStateOf<RDRun?>(null) }
    var activeEditSample by remember { mutableStateOf<RDSample?>(null) }
    var activeEditRun by remember { mutableStateOf<RDRun?>(null) }
    var activeDeleteSampleConfirm by remember { mutableStateOf<RDSample?>(null) }
    var activeDeleteRunConfirm by remember { mutableStateOf<RDRun?>(null) }
    var activeDeleteEmployeeConfirm by remember { mutableStateOf<Employee?>(null) }
    var activeEditEmployee by remember { mutableStateOf<Employee?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(exportMsg) {
        exportMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "R&D Check List",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Hệ thống Quản lý Tiến độ Nấu Mẫu R&D",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Cloud Sync & Status Indicator aligned inline to stay extremely compact
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "📅 30/05",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                                .clickable { viewModel.triggerCloudSync() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSyncing) Icons.Default.Sync else Icons.Default.CloudQueue,
                                    contentDescription = "Sync Cloud",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isSyncing) "Syncing..." else "Cloud ($cloudCount)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val isDark = isSystemInDarkTheme()
                val glassBackingColor = if (isDark) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                }
                val glassBorderBrush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                )

                Surface(
                    modifier = Modifier
                        .widthIn(max = 450.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 1.2.dp,
                            brush = glassBorderBrush,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.05f else 0.25f),
                                    Color.White.copy(alpha = if (isDark) 0.01f else 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    color = glassBackingColor, // Beautiful translucent backdrop
                    shadowElevation = 0.dp // Low elevation so solid shadow doesn't ruin translucency
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            Triple(0, Icons.Default.Dashboard, "Tổng quan"),
                            Triple(1, Icons.Default.People, "Tiến độ NV"),
                            Triple(2, Icons.Default.Settings, "Cấu hình")
                        )
                        
                        tabs.forEach { (index, icon, label) ->
                            val isSelected = activeTab == index
                            
                            val containerModifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { 
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                    selectedEmployeeDetail = null 
                                }
                                .let { mod ->
                                    if (isSelected) {
                                        mod
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.85f else 0.95f),
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.35f else 0.45f)
                                                    )
                                                )
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                    } else {
                                        mod // Unselected tab remains clean and lets the backing shine through
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                            
                            Row(
                                modifier = containerModifier,
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (activeTab == 0) {
                FloatingActionButton(
                    onClick = { showQuickTaskDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("quick_add_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm nhiệm vụ")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 0.dp,
                    start = innerPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = innerPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                )
                .background(MaterialTheme.colorScheme.background)
                .drawBehind {
                    // Soft, organic primary glow in the upper-right region
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFD7E8CD).copy(alpha = 0.55f),
                                Color(0xFFD7E8CD).copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.15f),
                            radius = size.width * 0.75f
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        radius = size.width * 0.75f
                    )
                    
                    // Soft, organic secondary glow in the lower-left region
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE1E4D5).copy(alpha = 0.5f),
                                Color(0xFFE1E4D5).copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.15f, size.height * 0.8f),
                            radius = size.width * 0.7f
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.8f),
                        radius = size.width * 0.7f
                    )
                }
        ) {
            // Tab Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> DashboardTabScreen(
                        filteredRuns = filteredRuns,
                        sampleReports = sampleReports,
                        viewModel = viewModel,
                        currentYear = currentYear,
                        currentMonth = currentMonth,
                        currentDay = currentDay,
                        selectedEmpId = selectedEmpId,
                        employees = employees,
                        onSampleLongClick = { activeActionSample = it },
                        onRunLongClick = { activeActionRun = it },
                        onEditRunClick = { activeEditRun = it },
                        onDeleteRunClick = { activeDeleteRunConfirm = it },
                        onViewSampleDetails = { expandedSampleDetailInApp = it }
                    )
                    1 -> EmployeeProgressTabScreen(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState,
                        onAddClick = { showQuickTaskDialog = true },
                        onSampleLongClick = { activeActionSample = it },
                        onRunLongClick = { activeActionRun = it }
                    )
                    2 -> ConfigurationTabScreen(
                        viewModel = viewModel,
                        employeeReports = employeeReports,
                        selectedEmployeeDetail = selectedEmployeeDetail,
                        onSelectEmployee = { selectedEmployeeDetail = it },
                        onAddEmployeeClick = { showAddEmployeeDialog = true },
                        onDeleteEmployee = { activeDeleteEmployeeConfirm = it },
                        onEditEmployee = { activeEditEmployee = it },
                        onViewSampleDetails = { expandedSampleDetailInApp = it },
                        onEditRun = { activeEditRun = it },
                        onDeleteRun = { activeDeleteRunConfirm = it }
                    )
                }
            }
        }
    }

    // Quick Assignment Dialog
    if (showQuickTaskDialog) {
        QuickTaskDialog(
            onDismiss = { showQuickTaskDialog = false },
            employees = employees,
            onSave = { code, name, empId, date, desc, estTimeStr ->
                viewModel.addSample(code, name, empId, date, desc, estTimeStr)
                showQuickTaskDialog = false
            }
        )
    }

    // Add Employee Dialog
    if (showAddEmployeeDialog) {
        AddEmployeeDialog(
            onDismiss = { showAddEmployeeDialog = false },
            onSave = { name, role, color ->
                viewModel.addEmployee(name, role, color)
                showAddEmployeeDialog = false
            }
        )
    }

    // Context Choice Actions for Sample (Long Pressed)
    if (activeActionSample != null) {
        val sample = activeActionSample!!
        AlertDialog(
            onDismissRequest = { activeActionSample = null },
            title = {
                Text(
                    text = "Tùy chọn cho Chỉ tiêu: ${sample.sampleCode}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tên mẫu: ${sample.sampleName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Vui lòng chọn một thao tác bạn muốn thực hiện tiếp theo:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: Edit
                    Button(
                        onClick = {
                            activeEditSample = sample
                            activeActionSample = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Sửa", modifier = Modifier.size(16.dp))
                            Text("Chỉnh sửa", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    // Option 2: Delete
                    Button(
                        onClick = {
                            activeDeleteSampleConfirm = sample
                            activeActionSample = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", modifier = Modifier.size(16.dp))
                            Text("Xóa bỏ", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { activeActionSample = null }) {
                    Text("Hủy bỏ", color = MaterialTheme.colorScheme.outline)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Context Choice Actions for Run (Long Pressed)
    if (activeActionRun != null) {
        val run = activeActionRun!!
        AlertDialog(
            onDismissRequest = { activeActionRun = null },
            title = {
                Text(
                    text = "Tùy chọn mẻ nấu #${run.runNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "Lựa chọn hành động tiếp theo cho mẻ nấu thử nghiệm này (Phân loại mẫu: ${run.sampleCode}).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: Edit
                    Button(
                        onClick = {
                            activeEditRun = run
                            activeActionRun = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Sửa mẻ", modifier = Modifier.size(16.dp))
                            Text("Chỉnh sửa", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    // Option 2: Delete
                    Button(
                        onClick = {
                            activeDeleteRunConfirm = run
                            activeActionRun = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa mẻ", modifier = Modifier.size(16.dp))
                            Text("Xóa mẻ", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { activeActionRun = null }) {
                    Text("Hủy bỏ", color = MaterialTheme.colorScheme.outline)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Edit Dialog for Sample
    if (activeEditSample != null) {
        val sample = activeEditSample!!
        var name by remember(sample) { mutableStateOf(sample.sampleName) }
        var selectedEmp by remember(sample) { mutableStateOf(employees.find { it.id == sample.assignedEmployeeId } ?: employees.firstOrNull()) }
        var desc by remember(sample) { mutableStateOf(sample.description) }
        var estTime by remember(sample) { mutableStateOf(sample.estimatedTimeStr) }
        var status by remember(sample) { mutableStateOf(sample.status) }
        var isDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { activeEditSample = null },
            title = {
                Text(
                    text = "Chỉnh sửa Chỉ tiêu: ${sample.sampleCode}",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên Chỉ tiêu/Mẫu R&D") },
                        placeholder = { Text("E.g., Sốt tương ớt cay nồng") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedEmp?.name ?: "Chưa chọn nhân sự",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Trợ lý phụ trách") },
                            trailingIcon = {
                                IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            employees.forEach { emp ->
                                DropdownMenuItem(
                                    text = { Text(emp.name, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedEmp = emp
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Yêu cầu & Công thức đun") },
                        placeholder = { Text("E.g., Nhiệt độ đun trung bình 85°C...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = estTime,
                        onValueChange = { estTime = it },
                        label = { Text("Thời gian ước tính") },
                        placeholder = { Text("E.g., 90 phút") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    var isStatusDropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = status,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Trạng thái công việc") },
                            trailingIcon = {
                                IconButton(onClick = { isStatusDropdownExpanded = !isStatusDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = isStatusDropdownExpanded,
                            onDismissRequest = { isStatusDropdownExpanded = false }
                        ) {
                            listOf("Đang thực hiện", "Hoàn thành").forEach { state ->
                                DropdownMenuItem(
                                    text = { Text(state) },
                                    onClick = {
                                        status = state
                                        isStatusDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            activeDeleteSampleConfirm = sample
                            activeEditSample = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa chỉ tiêu", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Xóa bỏ", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val updated = sample.copy(
                                sampleName = name,
                                assignedEmployeeId = selectedEmp?.id ?: sample.assignedEmployeeId,
                                description = desc,
                                estimatedTimeStr = estTime,
                                status = status
                            )
                            viewModel.updateSample(updated)
                            scope.launch {
                                snackbarHostState.showSnackbar("Đã thay đổi thông tin chỉ tiêu thành công!")
                            }
                            activeEditSample = null
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cập nhật", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { activeEditSample = null }) {
                    Text("Hủy bỏ")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Edit Dialog for Run
    if (activeEditRun != null) {
        val run = activeEditRun!!
        var runNum by remember(run) { mutableStateOf(run.runNumber.toString()) }
        var minStr by remember(run) { mutableStateOf((run.durationMs / 60000).toString()) }
        var secStr by remember(run) { mutableStateOf(((run.durationMs % 60000) / 1000).toString()) }
        var runStatus by remember(run) { mutableStateOf(run.status) }
        var fReason by remember(run) { mutableStateOf(run.failureReason) }
        var isStatusDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { activeEditRun = null },
            title = {
                Text(
                    text = "Sửa Mẻ Nấu #${run.runNumber} (Mã: ${run.sampleCode})",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = runNum,
                        onValueChange = { runNum = it },
                        label = { Text("Thứ tự mẻ thử") },
                        placeholder = { Text("E.g., 1") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minStr,
                            onValueChange = { minStr = it },
                            label = { Text("Phút đun") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = secStr,
                            onValueChange = { secStr = it },
                            label = { Text("Giây đun") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = runStatus,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kết quả kiểm tra") },
                            trailingIcon = {
                                IconButton(onClick = { isStatusDropdownExpanded = !isStatusDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = isStatusDropdownExpanded,
                            onDismissRequest = { isStatusDropdownExpanded = false }
                        ) {
                            listOf("Thành công", "Thất bại").forEach { result ->
                                DropdownMenuItem(
                                    text = { Text(result) },
                                    onClick = {
                                        runStatus = result
                                        isStatusDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (runStatus == "Thất bại") {
                        OutlinedTextField(
                            value = fReason,
                            onValueChange = { fReason = it },
                            label = { Text("Lý do mẻ hỏng / lỗi cảm quan") },
                            placeholder = { Text("E.g., Chua quá / nồng mùi cháy...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            activeDeleteRunConfirm = run
                            activeEditRun = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa mẻ nấu", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Xóa bỏ", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val orderNum = runNum.toIntOrNull() ?: run.runNumber
                            val m = minStr.toLongOrNull() ?: 0L
                            val s = secStr.toLongOrNull() ?: 0L
                            val totalMs = (m * 60 + s) * 1000

                            val updated = run.copy(
                                runNumber = orderNum,
                                durationMs = totalMs,
                                status = runStatus,
                                failureReason = if (runStatus == "Thành công") "" else fReason
                            )
                            viewModel.updateCookingRun(updated)
                            scope.launch {
                                snackbarHostState.showSnackbar("Đã sửa thông tin mẻ nấu thành công!")
                            }
                            activeEditRun = null
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cập nhật", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { activeEditRun = null }) {
                    Text("Đóng")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Confirmation delete sample dialog
    if (activeDeleteSampleConfirm != null) {
        val sample = activeDeleteSampleConfirm!!
        AlertDialog(
            onDismissRequest = { activeDeleteSampleConfirm = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Cảnh báo", tint = Color(0xFFDC2626)) },
            title = {
                Text(
                    text = "XÓA CHỈ TIÊU / TASK LỚN",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc chắn muốn xóa vĩnh viễn chỉ tiêu '${sample.sampleCode} - ${sample.sampleName}'? Tất cả các mẻ đun nấu kèm theo dữ liệu thống kê sẽ bay màu mãi mãi.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSample(sample)
                        scope.launch {
                            snackbarHostState.showSnackbar("Đã xóa hoàn thành chỉ tiêu R&D ${sample.sampleCode}!")
                        }
                        activeDeleteSampleConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Tôi xác nhận xóa", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDeleteSampleConfirm = null }) {
                    Text("Hủy bỏ")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Confirmation delete run dialog
    if (activeDeleteRunConfirm != null) {
        val run = activeDeleteRunConfirm!!
        AlertDialog(
            onDismissRequest = { activeDeleteRunConfirm = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Cảnh báo mẻ nấu", tint = Color(0xFFDC2626)) },
            title = {
                Text(
                    text = "XÓA MẺ NẤU THỬ NGHIỆM",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = "Bạn có thực sự muốn xóa mẻ đun lần thứ ${run.runNumber} thuộc chỉ tiêu '${run.sampleCode}' vĩnh viễn không?",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCookingRun(run)
                        scope.launch {
                            snackbarHostState.showSnackbar("Đã xóa mẻ nấu thử #${run.runNumber}!")
                        }
                        activeDeleteRunConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Xác nhận xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDeleteRunConfirm = null }) {
                    Text("Hủy")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Confirmation delete employee dialog
    if (activeDeleteEmployeeConfirm != null) {
        val emp = activeDeleteEmployeeConfirm!!
        AlertDialog(
            onDismissRequest = { activeDeleteEmployeeConfirm = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Cảnh báo xóa nhân sự", tint = Color(0xFFDC2626)) },
            title = {
                Text(
                    text = "XÓA VĨNH VIỄN NHÂN SỰ R&D",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc chắn muốn xóa vĩnh viễn nhân viên '${emp.name}'? Hành động này sẽ tự động xóa tất cả các chỉ tiêu/mẫu và toàn bộ mẻ nấu thử của nhân viên đó theo cơ chế liên kết dữ liệu an toàn. Dữ liệu đã xóa sẽ không thể phục hồi!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEmployee(emp)
                        if (selectedEmployeeDetail?.employee?.id == emp.id) {
                            selectedEmployeeDetail = null
                        }
                        scope.launch {
                            snackbarHostState.showSnackbar("Đã xóa vĩnh viễn nhân viên ${emp.name} và mọi công việc liên quan!")
                        }
                        activeDeleteEmployeeConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Xác nhận xóa tài khoản", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDeleteEmployeeConfirm = null }) {
                    Text("Hủy bỏ")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Edit employee dialog with double confirmation
    if (activeEditEmployee != null) {
        val emp = activeEditEmployee!!
        var name by remember(emp) { mutableStateOf(emp.name) }
        var role by remember(emp) { mutableStateOf(emp.role) }
        var colorHex by remember(emp) { mutableStateOf(emp.avatarColorHex) }
        var showSaveConfirm by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { activeEditEmployee = null },
            title = {
                Text(
                    text = "Chỉnh sửa thông tin: ${emp.name}",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên nhân viên R&D") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text("Vị trí/Vai trò công việc") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = colorHex,
                        onValueChange = { colorHex = it },
                        label = { Text("Mã màu thẻ (HEX)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveConfirm = true
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cập nhật", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeEditEmployee = null }) {
                    Text("Hủy bỏ")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )

        if (showSaveConfirm) {
            AlertDialog(
                onDismissRequest = { showSaveConfirm = false },
                title = {
                    Text(
                        text = "XÁC NHẬN CHỈNH SỬA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Text(
                        text = "Bạn có thực sự muốn lưu các thay đổi này cho nhân viên '${emp.name}' không?",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val updated = emp.copy(
                                name = name,
                                role = role,
                                avatarColorHex = colorHex
                            )
                            viewModel.updateEmployee(updated)
                            scope.launch {
                                snackbarHostState.showSnackbar("Đã cập nhật thông tin nhân viên thành công!")
                            }
                            showSaveConfirm = false
                            activeEditEmployee = null
                        }
                    ) {
                        Text("Xác nhận chỉnh sửa")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveConfirm = false }) {
                        Text("Hủy")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }

    if (expandedSampleDetailInApp != null) {
        val allRuns by viewModel.allRuns.collectAsStateWithLifecycle()
        SampleCookingProcessDialog(
            sample = expandedSampleDetailInApp!!,
            viewModel = viewModel,
            onDismiss = { expandedSampleDetailInApp = null },
            snackbarHostState = snackbarHostState,
            allRuns = allRuns
        )
    }
}

@Composable
fun ManagerHeaderSection(modifier: Modifier = Modifier.padding(vertical = 8.dp)) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "T",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Xin chào, Nguyễn Thị Thúy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Trưởng Bộ phận R&D - Giám sát nấu mẫu thực phẩm",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Badge(
                containerColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = "Quản lý Team",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}

@Composable
fun FilterSection(
    viewModel: RDViewModel,
    currentYear: String,
    currentMonth: String,
    currentDay: String,
    selectedEmpId: Int?,
    employees: List<Employee>
) {
    var expandedYear by remember { mutableStateOf(false) }
    var expandedMonth by remember { mutableStateOf(false) }
    var expandedDay by remember { mutableStateOf(false) }
    var expandedEmp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Bộ lọc",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Bộ lọc tiến độ nấu:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal Row of Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Day Filter Pill
            Box {
                FilterPill(
                    label = "Ngày: $currentDay",
                    onClick = { expandedDay = true },
                    isActive = currentDay != "Tất cả"
                )
                DropdownMenu(expanded = expandedDay, onDismissRequest = { expandedDay = false }) {
                    viewModel.daysList.forEach { i ->
                        DropdownMenuItem(
                            text = { Text("Ngày $i") },
                            onClick = {
                                viewModel.setFilterDay(i)
                                expandedDay = false
                            }
                        )
                    }
                }
            }

            // Month Filter Pill
            Box {
                FilterPill(
                    label = "Tháng: $currentMonth",
                    onClick = { expandedMonth = true },
                    isActive = currentMonth != "Tất cả"
                )
                DropdownMenu(expanded = expandedMonth, onDismissRequest = { expandedMonth = false }) {
                    viewModel.monthsList.forEach { m ->
                        DropdownMenuItem(
                            text = { Text("Tháng $m") },
                            onClick = {
                                viewModel.setFilterMonth(m)
                                expandedMonth = false
                            }
                        )
                    }
                }
            }

            // Year Filter Pill
            Box {
                FilterPill(
                    label = "Năm: $currentYear",
                    onClick = { expandedYear = true },
                    isActive = currentYear != "Tất cả"
                )
                DropdownMenu(expanded = expandedYear, onDismissRequest = { expandedYear = false }) {
                    viewModel.yearsList.forEach { y ->
                        DropdownMenuItem(
                            text = { Text("Năm $y") },
                            onClick = {
                                viewModel.setFilterYear(y)
                                expandedYear = false
                            }
                        )
                    }
                }
            }

            // Employee Filter Pill
            Box {
                val currentEmpName = employees.find { it.id == selectedEmpId }?.name ?: "Tất cả nhân viên"
                FilterPill(
                    label = currentEmpName,
                    onClick = { expandedEmp = true },
                    isActive = selectedEmpId != null,
                    icon = Icons.Default.Person
                )
                DropdownMenu(expanded = expandedEmp, onDismissRequest = { expandedEmp = false }) {
                    DropdownMenuItem(
                        text = { Text("Tất cả nhân viên") },
                        onClick = {
                            viewModel.selectEmployee(null)
                            expandedEmp = false
                        }
                    )
                    employees.forEach { emp ->
                        DropdownMenuItem(
                            text = { Text(emp.name) },
                            onClick = {
                                viewModel.selectEmployee(emp.id)
                                expandedEmp = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterPill(
    label: String,
    onClick: () -> Unit,
    isActive: Boolean,
    icon: ImageVector? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = 1.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun DashboardTabScreen(
    filteredRuns: List<RDRun>,
    sampleReports: List<SampleReportItem>,
    viewModel: RDViewModel,
    currentYear: String,
    currentMonth: String,
    currentDay: String,
    selectedEmpId: Int?,
    employees: List<Employee>,
    onSampleLongClick: (RDSample) -> Unit,
    onRunLongClick: (RDRun) -> Unit,
    onEditRunClick: ((RDRun) -> Unit)? = null,
    onDeleteRunClick: ((RDRun) -> Unit)? = null,
    onViewSampleDetails: ((RDSample) -> Unit)? = null
) {
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val totalRuns = filteredRuns.size
    val successes = filteredRuns.count { it.status == "Thành công" }
    val failures = filteredRuns.count { it.status == "Thất bại" }
    val totalDurationMs = filteredRuns.sumOf { it.durationMs }
    val successRate = if (totalRuns > 0) (successes.toFloat() / totalRuns * 100) else 0f
    
    val employeeReports by viewModel.employeeReports.collectAsStateWithLifecycle()
    val selectedEmployeeId by viewModel.selectedEmployeeId.collectAsStateWithLifecycle()

    // Local state for compact advanced date filter expander
    var showAdvancedDates by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }
    var expandedMonth by remember { mutableStateOf(false) }
    var expandedDay by remember { mutableStateOf(false) }

    // Sorting state (default: Sort Ascending - Thấp đến Cao for easy performance checks!)
    var sortCriterion by remember { mutableStateOf(0) } // 0: Hiệu suất, 1: Số mẻ nấu, 2: Số mẫu, 3: T.gian TB
    var sortAscending by remember { mutableStateOf(true) } // true: Thấp đến Cao, false: Cao đến Thấp

    // Custom Detailed PDF Simulation Dialog state
    var showPdfDialog by remember { mutableStateOf(false) }
    val printTimeStr = remember {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sdf.format(Date())
    }

    // Calculations for highlights (among active reports with runs > 0)
    val activeEmpReports = employeeReports.filter { it.totalRunsCount > 0 }
    val fewestSamplesEmp = activeEmpReports.minByOrNull { it.totalSamplesCount }
    val fastestCookEmp = activeEmpReports.minByOrNull { 
        if (it.totalRunsCount > 0) it.totalDurationMs.toDouble() / it.totalRunsCount else Double.MAX_VALUE 
    }

    // Sort the reports based on selection
    val sortedEmployeeReports = remember(employeeReports, sortCriterion, sortAscending) {
        val sList = employeeReports.toList()
        val sorted = when (sortCriterion) {
            0 -> sList.sortedBy { it.successRate }
            1 -> sList.sortedBy { it.totalRunsCount }
            2 -> sList.sortedBy { it.totalSamplesCount }
            else -> sList.sortedBy { 
                if (it.totalRunsCount > 0) (it.totalDurationMs / it.totalRunsCount) else 0L 
            }
        }
        if (sortAscending) sorted else sorted.reversed()
    }

    // Tracking active interactive detail report selection: null (default), "SAMPLES", "EFFICIENCY", "TIMING", "HIGHLIGHTS"
    var clickedDashboardReport by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // SECTION 1: ULTRA-COMPACT INLINE DATE PRESETS FILTER ROW
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Compact decorative indicator tag
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Bộ lọc",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Horizontal scrolling pill preset row
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dTodayParts = remember {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            sdf.format(Date()).split("-")
                        }
                        val dTodayYear = dTodayParts.getOrNull(0) ?: "2026"
                        val dTodayMonth = dTodayParts.getOrNull(1) ?: "05"
                        val dTodayDay = dTodayParts.getOrNull(2) ?: "31"

                        val dYesterdayParts = remember {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            sdf.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)).split("-")
                        }
                        val dYesterdayYear = dYesterdayParts.getOrNull(0) ?: "2026"
                        val dYesterdayMonth = dYesterdayParts.getOrNull(1) ?: "05"
                        val dYesterdayDay = dYesterdayParts.getOrNull(2) ?: "30"

                        val dTodayLabel = remember {
                            val sdf = SimpleDateFormat("d/M", Locale.getDefault())
                            "Hôm nay (${sdf.format(Date())})"
                        }
                        val dYesterdayLabel = remember {
                            val sdf = SimpleDateFormat("d/M", Locale.getDefault())
                            "Hôm qua (${sdf.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L))})"
                        }
                        val dMonthLabel = remember {
                            val sdf = SimpleDateFormat("M", Locale.getDefault())
                            "Sạch mẻ trong Tháng ${sdf.format(Date())}"
                        }

                        val isTodaySelected = currentDay == dTodayDay && currentMonth == dTodayMonth && currentYear == dTodayYear
                        val isYesterdaySelected = currentDay == dYesterdayDay && currentMonth == dYesterdayMonth && currentYear == dYesterdayYear
                        val isMonthSelected = currentDay == "Tất cả" && currentMonth == dTodayMonth && currentYear == dTodayYear
                        val isAllSelected = currentDay == "Tất cả" && currentMonth == "Tất cả" && currentYear == "Tất cả"

                        FilterPresetPill(
                            label = dTodayLabel,
                            isSelected = isTodaySelected,
                            onClick = {
                                viewModel.setFilterDay(dTodayDay)
                                viewModel.setFilterMonth(dTodayMonth)
                                viewModel.setFilterYear(dTodayYear)
                            }
                        )

                        FilterPresetPill(
                            label = dYesterdayLabel,
                            isSelected = isYesterdaySelected,
                            onClick = {
                                viewModel.setFilterDay(dYesterdayDay)
                                viewModel.setFilterMonth(dYesterdayMonth)
                                viewModel.setFilterYear(dYesterdayYear)
                            }
                        )

                        FilterPresetPill(
                            label = dMonthLabel,
                            isSelected = isMonthSelected,
                            onClick = {
                                viewModel.setFilterDay("Tất cả")
                                viewModel.setFilterMonth(dTodayMonth)
                                viewModel.setFilterYear(dTodayYear)
                            }
                        )

                        FilterPresetPill(
                            label = "Mọi thời gian",
                            isSelected = isAllSelected,
                            onClick = {
                                viewModel.setFilterDay("Tất cả")
                                viewModel.setFilterMonth("Tất cả")
                                viewModel.setFilterYear("Tất cả")
                            }
                        )

                        // In-pill custom expansion arrow
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (showAdvancedDates) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (showAdvancedDates) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { showAdvancedDates = !showAdvancedDates }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "Ngày tự chọn ${if (showAdvancedDates) "▲" else "▼"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showAdvancedDates) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // If advanced date sliders/dropdowns are expanded
                if (showAdvancedDates) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Day Selector Option
                        Box(modifier = Modifier.weight(1f)) {
                            FilterPresetPill(
                                label = "Ngày: $currentDay ▾",
                                isSelected = currentDay != "Tất cả",
                                onClick = { expandedDay = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(expanded = expandedDay, onDismissRequest = { expandedDay = false }) {
                                viewModel.daysList.forEach { d ->
                                        DropdownMenuItem(
                                            text = { Text("Ngày $d", fontSize = 11.sp) },
                                            onClick = {
                                                viewModel.setFilterDay(d)
                                                expandedDay = false
                                            }
                                        )
                                }
                            }
                        }

                        // Month Selector Option
                        Box(modifier = Modifier.weight(1f)) {
                            FilterPresetPill(
                                label = "Tháng: $currentMonth ▾",
                                isSelected = currentMonth != "Tất cả",
                                onClick = { expandedMonth = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(expanded = expandedMonth, onDismissRequest = { expandedMonth = false }) {
                                viewModel.monthsList.forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text("Tháng $m", fontSize = 11.sp) },
                                            onClick = {
                                                viewModel.setFilterMonth(m)
                                                expandedMonth = false
                                            }
                                        )
                                }
                            }
                        }

                        // Year Selector Option
                        Box(modifier = Modifier.weight(1.3f)) {
                            FilterPresetPill(
                                label = "Năm: $currentYear ▾",
                                isSelected = currentYear != "Tất cả",
                                onClick = { expandedYear = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(expanded = expandedYear, onDismissRequest = { expandedYear = false }) {
                                viewModel.yearsList.forEach { y ->
                                        DropdownMenuItem(
                                            text = { Text("Năm $y", fontSize = 11.sp) },
                                            onClick = {
                                                viewModel.setFilterYear(y)
                                                expandedYear = false
                                            }
                                        )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTION 2: VIEW SCOPE INDICATOR
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                    Text(
                        text = "CHẾ ĐỘ XEM CHUYÊN SÂU:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        text = if (selectedEmployeeId == null) {
                            "🏠 TOÀN BỘ KITCHEN R&D"
                        } else {
                            val name = employees.find { it.id == selectedEmployeeId }?.name ?: "Nhân Viên"
                            "👤 $name"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (selectedEmployeeId != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { viewModel.selectEmployee(null) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Xem tất cả ↺",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // SECTION 3: CORE COMPREHENSIVE INTERACTIVE STATS PANELS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Left: Success Rate Meter | Right: Total Sample Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isDark = isSystemInDarkTheme()
                    // LEFT: Success rate gauge (Click to toggle EFFICIENCY report)
                    val isEffSelected = clickedDashboardReport == "EFFICIENCY"
                    Card(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(130.dp)
                            .clickable {
                                clickedDashboardReport = if (isEffSelected) null else "EFFICIENCY"
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEffSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.5f else 0.65f) 
                                             else MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.5f else 0.55f)
                        ),
                        border = BorderStroke(
                            width = if (isEffSelected) 2.dp else 1.dp, 
                            color = if (isEffSelected) MaterialTheme.colorScheme.primary 
                                    else Color.White.copy(alpha = if (isDark) 0.15f else 0.75f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.size(54.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val strokeColor = if (successRate > 70) MaterialTheme.colorScheme.primary else Color(0xFFEF4444)
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(
                                        color = Color.LightGray.copy(alpha = 0.2f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = strokeColor,
                                        startAngle = -90f,
                                        sweepAngle = (successRate / 100f) * 360f,
                                        useCenter = false,
                                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = String.format(Locale.US, "%.0f%%", successRate),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = strokeColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "HIỆU SUẤT ĐẠT CHUẨN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.3.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Mẻ Đạt: $successes / $totalRuns",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isEffSelected) "Đăng mở ☒" else "Xem chi tiết 🔍",
                                    fontSize = 7.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // RIGHT: Total dynamic samples card (Click to toggle SAMPLES report)
                    val isSamplesSelected = clickedDashboardReport == "SAMPLES"
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clickable {
                                clickedDashboardReport = if (isSamplesSelected) null else "SAMPLES"
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSamplesSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.5f else 0.65f) 
                                             else MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.15f else 0.25f)
                        ),
                        border = BorderStroke(
                            width = if (isSamplesSelected) 2.dp else 1.dp,
                            color = if (isSamplesSelected) MaterialTheme.colorScheme.primary 
                                    else Color.White.copy(alpha = if (isDark) 0.15f else 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SỐ DIỆN MẪU R&D",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                // Interactive visual icon with drop-shadow effect
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                            
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${sampleReports.size}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Sản phẩm được theo dõi",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = if (isSamplesSelected) "Đóng ☒" else "Xem mẻ nấu 🔎",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Row 2: Standard cooking time vs Actual duration (Click to toggle TIMING report)
                val isTimingSelected = clickedDashboardReport == "TIMING"
                val isDark = isSystemInDarkTheme()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clickedDashboardReport = if (isTimingSelected) null else "TIMING"
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTimingSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.4f else 0.55f) 
                                         else MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.5f else 0.55f)
                    ),
                    border = BorderStroke(
                        width = if (isTimingSelected) 2.dp else 1.dp,
                        color = if (isTimingSelected) MaterialTheme.colorScheme.tertiary 
                                else Color.White.copy(alpha = if (isDark) 0.15f else 0.75f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        val stdMins = totalRuns * 30
                        val actMins = totalDurationMs / (1000 * 60)
                        val diff = stdMins - actMins
                        val statusText = if (totalRuns == 0) "Chưa nấu mẻ nào" else if (diff >= 0) "Nhanh hơn ${diff} phút 🟢" else "Chậm trễ ${-diff} phút 🔴"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Text(
                                    text = "⏱️ THỜI GIAN TIÊU CHUẨN vs THỰC TẾ CUỘC NẤU",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            // Dynamic Speed status badge with nice effect
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (diff >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diff >= 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress bars
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tiêu chuẩn (Target): ${totalRuns * 30} Phút (30p/mẻ)",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = 0.65f,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Thực tế thiết lập: ${viewModel.formatDuration(totalDurationMs)}",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = if (totalRuns == 0) 0f else (actMins.toFloat() / (stdMins.coerceAtLeast(1))).coerceAtMost(1f),
                                    color = if (diff >= 0) MaterialTheme.colorScheme.primary else Color(0xFFEF4444),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = if (isTimingSelected) "Nhấp để thu gọn ▲" else "Nhấp xem bảng tốc độ bếp ⚙️",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // SECTION Interactive dynamic details panel (Strict User Intent Requirement: Explains "khi click vào dashboad nào thì mới show chi tiết về báo cáo đó")
        item {
            AnimatedContent(
                targetState = clickedDashboardReport,
                transitionSpec = {
                    expandVertically(expandFrom = Alignment.Top) + fadeIn() togetherWith
                    shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                },
                label = "DashboardDetailExpansion"
            ) { reportType ->
                if (reportType == null) {
                    // Default state call-out showing how to expand details
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "💡",
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Mẹo: Nhấp vào bất kỳ thẻ chỉ số phía trên để xem ngay báo cáo phân tích chi tiết & hướng xử lý quản lý đặc biệt.",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Detail Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val (headerText, headerColor) = when (reportType) {
                                        "SAMPLES" -> "BÁO CÁO PHÂN TÍCH DIỆN MẪU R&D" to MaterialTheme.colorScheme.primary
                                        "EFFICIENCY" -> "BÁO CÁO PHÂN TÍCH HIỆU SUẤT & KHẮC PHỤC LỖI KITCHEN" to Color(0xFFEF4444)
                                        "TIMING" -> "BÁO CÁO TỐC ĐỘ CHEF COOK - THỜI GIAN CHUẨN HOÁ" to MaterialTheme.colorScheme.tertiary
                                        else -> "BÁO CÁO CHỈ SỐ MANAGER QUYẾT ĐỊNH & THU HOẠCH" to MaterialTheme.colorScheme.secondary
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(headerColor)
                                    )
                                    Text(
                                        text = headerText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = headerColor
                                    )
                                }

                                IconButton(
                                    onClick = { clickedDashboardReport = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)

                            // Detail Body Content depending on type
                            when (reportType) {
                                "SAMPLES" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "BIỂU ĐỒ SỐ LẦN NẤU THỬ NGHIỆM CHO MỖI DIỆN MẪU:",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        // Horizontal bar chart showing runs per sample code
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            if (sampleReports.isEmpty()) {
                                                Text("Không có mẫu nào trong bộ lọc hiện hành.", fontSize = 8.5.sp, color = Color.Gray)
                                            } else {
                                                val maxRuns = sampleReports.maxOfOrNull { it.totalRuns }?.coerceAtLeast(1) ?: 1
                                                sampleReports.take(5).forEach { item ->
                                                    val fraction = (item.totalRuns.toFloat() / maxRuns).coerceIn(0f, 1f)
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = item.sampleCode,
                                                            modifier = Modifier.width(100.dp),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(10.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color.LightGray.copy(alpha = 0.25f))
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxHeight()
                                                                    .fillMaxWidth(fraction)
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(MaterialTheme.colorScheme.primary)
                                                            )
                                                        }
                                                        Text(
                                                            text = "${item.totalRuns} mẻ",
                                                            modifier = Modifier.padding(start = 6.dp),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Mini report summary grid
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            ) {
                                                Text("Tổng số món mẫu", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                                Text("${sampleReports.size} Món mẫu", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .weight(1.2f)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            ) {
                                                Text("Mẻ đạt yêu cầu", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                                Text("$successes mẻ thành công", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                            }
                                        }
                                    }
                                }
                                "EFFICIENCY" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "BIỂU ĐỒ TRỰC QUAN TỶ LỆ MẺ NẤU ĐẠT vs LỖI KHỬ TRÙNG KITCHEN:",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444)
                                        )

                                        // Custom Pie / Ring Chart
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(64.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    val successAngle = (successRate / 100f) * 360f
                                                    val failAngle = 360f - successAngle
                                                    
                                                    // Draw Ring Chart
                                                    drawArc(
                                                        color = Color(0xFF10B981), // success (green)
                                                        startAngle = -90f,
                                                        sweepAngle = successAngle,
                                                        useCenter = false,
                                                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                                    )
                                                    if (failAngle > 0) {
                                                        drawArc(
                                                            color = Color(0xFFEF4444), // failure (red)
                                                            startAngle = -90f + successAngle,
                                                            sweepAngle = failAngle,
                                                            useCenter = false,
                                                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = String.format(Locale.US, "%.0f%%", successRate),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                                    Text("Mẻ đạt (Thành công): $successes mẻ (${String.format(Locale.US, "%.1f%%", successRate)})", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                                    Text("Mẻ lỗi (Trục trặc): $failures mẻ (${String.format(Locale.US, "%.1f%%", 100f - successRate)})", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            ) {
                                                Column {
                                                    Text("Số mẻ lỗi/trục trặc", fontSize = 8.sp, color = Color.Gray)
                                                    Text("$failures mẻ", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1.5f)
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            ) {
                                                Column {
                                                    Text("Nguyên nhân chính", fontSize = 8.sp, color = Color.Gray)
                                                    Text("Nhiệt độ (+3 độ C), Sốt cạn", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }
                                    }
                                }
                                "TIMING" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "SO SÁNH THỰC TẾ NẤU vs TIÊU CHUẨN 30 PHÚT:",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            if (sampleReports.isEmpty()) {
                                                Text("Không có dữ liệu thời gian.", fontSize = 8.5.sp, color = Color.Gray)
                                            } else {
                                                sampleReports.take(4).forEach { item ->
                                                    val avgMin = if (item.totalRuns > 0) item.totalDurationMs / (1000 * 60) / item.totalRuns else 0L
                                                    val fraction = (avgMin.toFloat() / 50f).coerceIn(0f, 1f)
                                                    val barColor = if (avgMin > 30) Color(0xFFEF4444) else MaterialTheme.colorScheme.tertiary
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = item.sampleCode,
                                                            modifier = Modifier.width(90.dp),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                            // Target 30 mins
                                                            Box(modifier = Modifier.fillMaxWidth(0.6f/* 30/50 */).height(3.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.25f)))
                                                            // Actual mins
                                                            Box(modifier = Modifier.fillMaxWidth(fraction).height(3.dp).clip(CircleShape).background(barColor))
                                                        }
                                                        Text(
                                                            text = "${avgMin}p",
                                                            modifier = Modifier.padding(start = 6.dp),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = barColor
                                                        )
                                                    }
                                                }
                                                Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.4f)))
                                                        Text("Tiêu chuẩn (30m)", fontSize = 7.5.sp, color = Color.Gray)
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                                                        Text("Đạt mục tiêu (≤30m)", fontSize = 7.5.sp, color = Color.Gray)
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                                        Text("Vượt chuẩn (>30m)", fontSize = 7.5.sp, color = Color.Gray)
                                                    }
                                                }
                                            }
                                        }

                                        val stdMins = totalRuns * 30
                                        val actMins = totalDurationMs / (1000 * 60)
                                        val difference = stdMins - actMins

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Tổng Thời gian Nấu Mục Tiêu:", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("$stdMins phút", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Tổng Thời gian Thực Tế:", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("$actMins phút", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Gợi ý tối ưu:",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Hãy chọn mục tiêu chi tiết ở các bảng số liệu trên để xem phân tích trực quan nâng cao.",
                                            fontSize = 8.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTION: ADDITIONAL CHARTS/PLOTS (Satisfies "cần thể hiện các biểu đồ nhiều hơn")
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊 BIỂU ĐỒ BẾP R&D: TỔNG QUAN HIỆU SUẤT TRỰC QUAN",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.3.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Thời gian thực",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Chart 1: Visual Bar Columns of Kitchen runs by employee
                    Text(
                        text = "1) Sản lượng mẻ nấu & Tỉ lệ Success của Chef Cook",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val activeReports = employeeReports.filter { it.totalRunsCount > 0 }
                    if (activeReports.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Chưa có mẻ nấu hoạt động để vẽ biểu đồ", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        // Let's draw custom beautiful bars for employee cooking quantity
                        val maxRuns = activeReports.maxOfOrNull { it.totalRunsCount }?.coerceAtLeast(1) ?: 1
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            activeReports.forEach { rep ->
                                val runsRatio = rep.totalRunsCount.toFloat() / maxRuns
                                val barColor = if (rep.successRate >= 80f) Color(0xFF10B981) 
                                               else if (rep.successRate >= 50f) MaterialTheme.colorScheme.primary 
                                               else Color(0xFFEF4444)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Numeric Label Over Bar
                                    Text(
                                        text = "${rep.totalRunsCount} mẻ",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = barColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Dynamic Custom Rounded Column Visual with Custom Shadow-like background
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .height((85 * runsRatio).dp)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        barColor,
                                                        barColor.copy(alpha = 0.5f)
                                                    )
                                                )
                                            )
                                    )

                                    Spacer(modifier = Modifier.height(3.dp))

                                    // Employee Short Name
                                    Text(
                                        text = rep.employee.name.split(" ").lastOrNull() ?: rep.employee.name,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Tiny performance indicator
                                    Text(
                                        text = "${rep.successRate.toInt()}% Đạt",
                                        fontSize = 7.5.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Chart 2: Double Combined Kitchen Ratio Ring/Bar
                    Text(
                        text = "2) Tỉ lệ mẻ đạt (Thành công) vs mẻ lỗi tổng thể hệ thống",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val rate = successRate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Drawing dual progress ribbon
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                if (rate > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(rate / 100f)
                                            .background(Color(0xFF10B981))
                                    )
                                }
                                if (rate < 100) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight((100f - rate).coerceAtLeast(0.1f) / 100f)
                                            .background(Color(0xFFEF4444))
                                    )
                                }
                            }
                        }

                        // Label
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Đạt: ${rate.toInt()}% | Lỗi: ${(100 - rate).toInt()}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // SECTION 5: ACTIVE STAFF DIRECTORY WITH DYNAMIC SAGES SORT (MIN TO MAX)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TIẾN ĐỘ NHÂN VIÊN CHUẨN HOÁ (${employeeReports.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    // Sorting Direction Button (Low to High vs High to Low)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .clickable { sortAscending = !sortAscending }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (sortAscending) "Sắp xếp: Thấp ➔ Cao 🔺" else "Sắp xếp: Cao ➔ Thấp 🔻",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Interactive sort criterion select bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Hiệu suất", "Số mẻ nấu", "Số diện mẫu", "T.gian trung bình").forEachIndexed { index, title ->
                        val isSelected = sortCriterion == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { sortCriterion = index }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Horizontal employee list with zoom in zoom out controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    sortedEmployeeReports.forEach { rep ->
                        val colorHex = rep.employee.avatarColorHex
                        val avatarColor = try {
                            Color(android.graphics.Color.parseColor(colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        val isSelfSelected = selectedEmployeeId == rep.employee.id
                        val isAnySelected = selectedEmployeeId != null
                        val opacity = if (!isAnySelected || isSelfSelected) 1f else 0.5f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (isSelfSelected) viewModel.selectEmployee(null) else viewModel.selectEmployee(rep.employee.id)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isAnySelected && !isSelfSelected) avatarColor.copy(alpha = 0.35f) else avatarColor)
                                    .border(
                                        width = if (isSelfSelected) 2.dp else 1.dp,
                                        color = if (isSelfSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rep.employee.name.take(2).uppercase(Locale.getDefault()),
                                    color = if (isAnySelected && !isSelfSelected) Color.White.copy(alpha = 0.5f) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = rep.employee.name.split(" ").lastOrNull() ?: rep.employee.name,
                                fontSize = 10.sp,
                                fontWeight = if (isSelfSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelfSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.alpha(opacity)
                            )
                            
                            // Micro performance metric badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                val metricText = when (sortCriterion) {
                                    0 -> "${rep.successRate.toInt()}% Đạt"
                                    1 -> "${rep.totalRunsCount} mẻ"
                                    2 -> "${rep.totalSamplesCount} mẫu"
                                    else -> {
                                        val avg = if (rep.totalRunsCount > 0) (rep.totalDurationMs / (1000 * 60)) / rep.totalRunsCount else 0L
                                        "${avg}p/mẻ"
                                    }
                                }
                                Text(
                                    text = metricText,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 7: DETAILED SAMPLE PROGRESS RECORDS UNDER FILTER
        item {
            Text(
                text = "DANH SÁCH DIỆN MẪU R&D CHI TIẾT (${sampleReports.size})",
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (sampleReports.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Không tìm thấy mẫu nấu dưới bộ lọc này",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Thay đổi mốc thời gian hoặc chuyển qua mốc Ngày khác",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(sampleReports) { report ->
                SampleProgressCard(
                    report = report,
                    viewModel = viewModel,
                    onSampleLongClick = onSampleLongClick,
                    onRunLongClick = onRunLongClick,
                    onEditRunClick = onEditRunClick,
                    onDeleteRunClick = onDeleteRunClick,
                    onViewSampleDetails = onViewSampleDetails
                )
            }
        }

        // SECTION 6: DETAILED EXP PRINT & SIGNATURE SUBMISSION COMPONENT
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "📋 XUẤT PHIẾU BÁO CÁO DUYỆT NHANH (SPECIFIC TIMESTAMP)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Phục vụ nộp cho Ban Giám đốc với bộ lọc thời gian & mộc ký sống của Thúy Nguyễn Nguyễn.",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Bản in: $currentDay/$currentMonth/$currentYear",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Khởi tạo: $printTimeStr",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Button(
                            onClick = { showPdfDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Xem PDF Nộp Ký 📑", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // GORGEOUS BLUEPRINT REPORT EMBEDDED SUBMISSION DIALOG
    if (showPdfDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPdfDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, Color.DarkGray)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Modern Standard Letterhead Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "CÔNG TY CỔ PHẦN THỰC PHẨM VÀ NƯỚC GIẢI KHÁT NAM VIỆT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Phòng Thử Nghiệm Chất Lượng R&D",
                                fontSize = 7.sp,
                                color = Color.Gray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color.Red, RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRINTED PDF DRAFT",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 7.sp,
                                color = Color.Red
                            )
                        }
                    }

                    Divider(color = Color.LightGray, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "PHIẾU ĐÁNH GIÁ CHẤT LƯỢNG & TIẾN ĐỘ NẤU MẪU",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Black
                    )
                    
                    Text(
                        text = "Bộ lọc in: Ngày $currentDay, Tháng $currentMonth, Năm $currentYear",
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Black
                    )
                    Text(
                        text = "Thời gian in chính xác: $printTimeStr",
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.DarkGray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Brief Overview Data
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9FAFB))
                            .border(1.dp, Color.LightGray)
                            .padding(8.dp)
                    ) {
                        Text(text = "• Tổng số mẫu thử nghiệm: ${sampleReports.size} diện mẫu", fontSize = 9.sp, color = Color.Black)
                        Text(text = "• Tổng số mẻ nấu thực tế: $totalRuns lần thử", fontSize = 9.sp, color = Color.Black)
                        Text(text = "• Tỉ lệ mẻ đạt chứng nhận: ${String.format(Locale.US, "%.1f%%", successRate)} (Thành công: $successes, Lỗi: $failures)", fontSize = 9.sp, color = Color.Black)
                        Text(text = "• Thời gian nấu so với KPI Standard: ${viewModel.formatDuration(totalDurationMs)} (Chuẩn: ${totalRuns * 30} phút)", fontSize = 9.sp, color = Color.Black)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "BẢNG PHÂN HOẠT CHI TIẾT TIẾN ĐỘ NHÂN VIÊN:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Color.Black
                    )

                    // Data grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE5E7EB))
                                .padding(4.dp)
                        ) {
                            Text(text = "Nhân viên", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(1.5f))
                            Text(text = "Số mẻ", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                            Text(text = "Mẫu đạt", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text(text = "Hiệu suất", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }

                        employeeReports.forEach { rep ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                            ) {
                                Text(text = rep.employee.name, fontSize = 7.5.sp, color = Color.Black, modifier = Modifier.weight(1.5f))
                                Text(text = "${rep.totalRunsCount}", fontSize = 7.5.sp, color = Color.Black, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                Text(text = "${rep.successCount} Đ - ${rep.failureCount} L", fontSize = 7.5.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(text = "${rep.successRate.toInt()}%", fontSize = 7.5.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                            Divider(color = Color.LightGray, thickness = 0.5.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Signatures
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Quản lý Lập Biểu", fontSize = 8.sp, color = Color.Black)
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(text = "Nguyễn Thị Thúy", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.Blue)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "P. Trưởng Phòng R&D", fontSize = 8.sp, color = Color.Black)
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(text = "ĐÃ ĐƯỢC PHÊ DUYỆT 🔵", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color(0xFF047857))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.simulatePdfExport(appContext)
                                showPdfDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Tải PDF Xuất Bản 📥", fontSize = 10.sp, color = Color.White)
                        }

                        Button(
                            onClick = { showPdfDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Đóng", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterPresetPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    subtextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Card(
        modifier = modifier.height(130.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, if (containerColor == MaterialTheme.colorScheme.surface) MaterialTheme.colorScheme.outlineVariant else Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = subtextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (containerColor == MaterialTheme.colorScheme.surface) color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (containerColor == MaterialTheme.colorScheme.surface) color else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Column {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtext,
                    fontSize = 10.sp,
                    color = subtextColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SampleProgressCard(
    report: SampleReportItem,
    viewModel: RDViewModel,
    onSampleLongClick: ((RDSample) -> Unit)? = null,
    onRunLongClick: ((RDRun) -> Unit)? = null,
    onEditRunClick: ((RDRun) -> Unit)? = null,
    onDeleteRunClick: ((RDRun) -> Unit)? = null,
    onViewSampleDetails: ((RDSample) -> Unit)? = null
) {
    var expandedDetails by remember { mutableStateOf(false) }
    val allSamples by viewModel.allSamples.collectAsStateWithLifecycle()
    val sampleObj = remember(allSamples, report.sampleCode) {
        allSamples.find { it.sampleCode == report.sampleCode }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { expandedDetails = !expandedDetails },
                onLongClick = {
                    sampleObj?.let { onSampleLongClick?.invoke(it) }
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (expandedDetails) 1.5.dp else 1.dp,
            color = if (expandedDetails) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expandedDetails) 4.dp else 0.dp)
    ) {
        val totalRuns = report.totalRuns
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Mẫu thử",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = report.sampleCode,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                }

                // Performance status badge
                val successRate = if (totalRuns > 0) (report.successCount.toFloat() / totalRuns * 100).toInt() else 0
                val badgeColor = if (successRate >= 80) Color(0xFF10B981) else if (successRate >= 50) Color(0xFFF59E0B) else Color(0xFFEF4444)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Đạt $successRate%",
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub Stats Summary Layout (Sleek side-by-side)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "TẦN SUẤT NẤU", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Text(text = "$totalRuns lần", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "TỔNG THỜI GIAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Text(text = viewModel.formatDuration(report.totalDurationMs), fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "THÀNH TỰU", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✓${report.successCount}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                        Text(text = "✗${report.failureCount}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                    }
                }
            }

            // Interactive Success/Failure representation Bar
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                val successWeight = if (totalRuns > 0) report.successCount.toFloat() / totalRuns else 0f
                val failWeight = if (totalRuns > 0) report.failureCount.toFloat() / totalRuns else 0f

                if (successWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(successWeight)
                            .background(Color(0xFF10B981))
                    )
                }
                if (failWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(failWeight)
                            .background(Color(0xFFEF4444))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đạt: ${report.successCount}  •  Lỗi: ${report.failureCount}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (expandedDetails) "Thu gọn chi tiết ▲" else "Xem chi tiết mẫu ▼",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { expandedDetails = !expandedDetails }
                )
            }

            // Quick view detail & add new cooking runs
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        sampleObj?.let { onViewSampleDetails?.invoke(it) }
                    },
                    modifier = Modifier.weight(1.3f).height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Text("Ghi mẻ / Xem chi tiết", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                FilledTonalButton(
                    onClick = { expandedDetails = !expandedDetails },
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (expandedDetails) "Thu gọn ▲" else "Xem mẻ ▼",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Expanded Breakdown panel
            AnimatedVisibility(
                visible = expandedDetails,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Lịch sử nấu từng lần",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.3.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    report.detailRuns.sortedBy { it.runNumber }.forEach { run ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = { onEditRunClick?.invoke(run) },
                                    onLongClick = { onRunLongClick?.invoke(run) }
                                )
                                .padding(paddingValues = PaddingValues(start = 8.dp, end = 2.dp, top = 6.dp, bottom = 6.dp)),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Lần nấu thứ ${run.runNumber}:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = viewModel.formatDuration(run.durationMs),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                val runPercentage = if (report.totalDurationMs > 0) (run.durationMs.toFloat() / report.totalDurationMs * 100).toInt() else 0
                                Text(
                                    text = "Chiếm $runPercentage% tổng thời gian nấu mẫu.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Icon(
                                    imageVector = if (run.status == "Thành công") Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = run.status,
                                    tint = if (run.status == "Thành công") Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = run.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (run.status == "Thành công") Color(0xFF10B981) else Color(0xFFEF4444)
                                )

                                Spacer(modifier = Modifier.width(2.dp))

                                IconButton(
                                    onClick = { onEditRunClick?.invoke(run) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Sửa mẻ",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteRunClick?.invoke(run) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Xóa mẻ",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        if (run.status == "Thất bại" && run.failureReason.isNotEmpty()) {
                            Text(
                                text = "↳ Lý hỏng: ${run.failureReason}",
                                fontSize = 10.sp,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeesTabScreen(
    employeeReports: List<EmployeeReportSummary>,
    selectedEmployeeDetail: EmployeeReportSummary?,
    onSelectEmployee: (EmployeeReportSummary?) -> Unit,
    onAddEmployeeClick: () -> Unit,
    onDeleteEmployee: (Employee) -> Unit,
    onEditEmployee: (Employee) -> Unit,
    viewModel: RDViewModel,
    modifier: Modifier = Modifier.padding(horizontal = 16.dp),
    onViewSampleDetails: ((RDSample) -> Unit)? = null,
    onEditRun: ((RDRun) -> Unit)? = null,
    onDeleteRun: ((RDRun) -> Unit)? = null
) {
    if (selectedEmployeeDetail != null) {
        EmployeeDetailPane(
            report = selectedEmployeeDetail,
            onBack = { onSelectEmployee(null) },
            viewModel = viewModel,
            onViewSampleDetails = onViewSampleDetails,
            onEditRun = onEditRun,
            onDeleteRun = onDeleteRun
        )
    } else {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            // Team Manager profile is moved to the top of Employees list to clean up the workspace
            ManagerHeaderSection()
            
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DANH SÁCH NHÂN VIÊN (${employeeReports.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = onAddEmployeeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("add_employee_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Mới", fontSize = 11.sp)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(employeeReports) { report ->
                    EmployeeWorkloadCard(
                        report = report,
                        onClick = { onSelectEmployee(report) },
                        onDelete = { onDeleteEmployee(report.employee) },
                        onEdit = { onEditEmployee(report.employee) },
                        viewModel = viewModel
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun EmployeeWorkloadCard(
    report: EmployeeReportSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    viewModel: RDViewModel
) {
    val hexColor = try {
        Color(android.graphics.Color.parseColor(report.employee.avatarColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circle Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(hexColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = report.employee.name.take(1).uppercase(Locale.getDefault()),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Overview Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = report.employee.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small edit icon for easy profile update
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Small delete icon for easy management
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Text(
                    text = report.employee.role,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Số mẻ: ${report.totalRunsCount}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Đạt: ${report.successRate.toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (report.successRate > 70) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                    Text(
                        text = viewModel.formatDuration(report.totalDurationMs),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun EmployeeDetailPane(
    report: EmployeeReportSummary,
    onBack: () -> Unit,
    viewModel: RDViewModel,
    onViewSampleDetails: ((RDSample) -> Unit)? = null,
    onEditRun: ((RDRun) -> Unit)? = null,
    onDeleteRun: ((RDRun) -> Unit)? = null
) {
    var showAddSampleDialog by remember { mutableStateOf(false) }
    val allSamples by viewModel.allSamples.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Trở lại")
            }
            Text(
                text = "CHI TIẾT NĂNG SUẤT: ${report.employee.name.uppercase()}",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hiệu suất hoạt động",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Tổng mẫu nấu: ${report.totalSamplesCount} mẫu dải khác nhau", fontSize = 11.sp)
                            Text(text = "Tổng lần chưng nấu: ${report.totalRunsCount} lần nấu thí nghiệm", fontSize = 11.sp)
                            Text(text = "Thời lượng cống hiến: ${viewModel.formatDuration(report.totalDurationMs)}", fontSize = 11.sp)
                        }

                        // Success Percentage pill
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(if (report.successRate > 75) Color(0xFF10B981) else Color(0xFFF59E0B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.US, "%.0f%%", report.successRate),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "ĐẠT",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DANH SÁCH MỒI/MẪU ĐÃ PHA NẤU CHI TIẾT:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { showAddSampleDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                            Text("Thêm", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (report.sampleList.isEmpty()) {
                item {
                    Text(
                        text = "Chưa nhận nấu mẫu nào phục vụ ngày lọc này.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            } else {
                items(report.sampleList) { sampleReport ->
                    SampleProgressCard(
                        report = sampleReport,
                        viewModel = viewModel,
                        onEditRunClick = onEditRun,
                        onDeleteRunClick = onDeleteRun,
                        onViewSampleDetails = onViewSampleDetails
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showAddSampleDialog) {
        var sampleCode by remember { mutableStateOf("") }
        var sampleName by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var estHours by remember { mutableStateOf("1") }
        var estMinutes by remember { mutableStateOf("30") }
        val todayDateStr = remember {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(java.util.Date())
        }
        var dateCreated by remember { mutableStateOf(todayDateStr) }

        AlertDialog(
            onDismissRequest = { showAddSampleDialog = false },
            title = {
                Text(
                    text = "GIAO VIỆC / THÊM TASK LỚN CHO ${report.employee.name.uppercase()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = sampleCode,
                        onValueChange = { sampleCode = it },
                        label = { Text("Mã chỉ tiêu / mẫu R&D", fontSize = 11.sp) },
                        placeholder = { Text("E.g., CAKE-STRAWBERRY-v1") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = sampleName,
                        onValueChange = { sampleName = it },
                        label = { Text("Tên sản phẩm thử", fontSize = 11.sp) },
                        placeholder = { Text("E.g., Bánh bông lan kem dâu tươi R&D") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Yêu cầu/Lưu ý dải chỉ tiêu", fontSize = 11.sp) },
                        placeholder = { Text("E.g., Giảm 5% bột béo, theo dõi nhiệt độ lò") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = estHours,
                            onValueChange = { estHours = it },
                            label = { Text("H giờ", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = estMinutes,
                            onValueChange = { estMinutes = it },
                            label = { Text("Phút", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = dateCreated,
                        onValueChange = { dateCreated = it },
                        label = { Text("Ngày bắt đầu (YYYY-MM-DD)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sampleCode.isNotBlank() && sampleName.isNotBlank()) {
                            val finalEstTime = "${estHours.trim()} giờ ${estMinutes.trim()} phút"
                            viewModel.addSample(
                                sampleCode = sampleCode.trim(),
                                sampleName = sampleName.trim(),
                                assignedEmployeeId = report.employee.id,
                                dateCreated = dateCreated.trim(),
                                description = description.trim(),
                                estimatedTimeStr = finalEstTime
                            )
                            showAddSampleDialog = false
                        }
                    },
                    enabled = sampleCode.isNotBlank() && sampleName.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Giao việc", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSampleDialog = false }) {
                    Text("Hủy", fontSize = 11.sp)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun LogRunTabScreen(
    viewModel: RDViewModel,
    snackbarHostState: SnackbarHostState
) {
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val allSamples by viewModel.allSamples.collectAsStateWithLifecycle()
    val allRuns by viewModel.allRuns.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf(0) } // 0: QUẢN LÝ, 1: NHÂN VIÊN

    // Manager states
    var mSampleCode by remember { mutableStateOf("") }
    var mSampleName by remember { mutableStateOf("") }
    var mSelectedEmp by remember { mutableStateOf<Employee?>(null) }
    var mDescription by remember { mutableStateOf("") }
    var mExpandedEmpMenu by remember { mutableStateOf(false) }
    val systemTodayStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(java.util.Date())
    }
    var mDateCreated by remember { mutableStateOf(systemTodayStr) }
    var mFilterStatus by remember { mutableStateOf("Tất cả") } // Tất cả, Đang thực hiện, Hoàn thành

    // Employee states
    var eSelectedAccount by remember { mutableStateOf<Employee?>(null) }
    var eExpandedAccountMenu by remember { mutableStateOf(false) }
    var eExpandedSampleCodeDetails by remember { mutableStateOf("") } // currently expanded RDSample code

    // Detail cooking run logging states
    var runDurationStr by remember { mutableStateOf("25") }
    var runStatus by remember { mutableStateOf("Thành công") }
    var runFailureReason by remember { mutableStateOf("") }

    var samplePendingDelete by remember { mutableStateOf<RDSample?>(null) }
    var runPendingDelete by remember { mutableStateOf<RDRun?>(null) }

    val scope = rememberCoroutineScope()

    // Helper to parse hex SAFELY
    fun parseHexColor(hex: String?): Color {
        return try {
            if (hex != null && hex.startsWith("#")) {
                Color(android.graphics.Color.parseColor(hex))
            } else {
                Color.Gray
            }
        } catch (e: Exception) {
            Color.Gray
        }
    }

    val failurePresets = listOf(
        "Quá nhiệt cháy khét đáy nồi",
        "Độ pH vượt ngưỡng cho phép (> 4.8)",
        "Tách pha nhũ hóa, tách dầu lớp trên",
        "Quá đặc, không thể đo độ nhớt chảy",
        "Kết khối vón cục do tỷ lệ bột sai",
        "Màu sẫm đen cháy xám không đồng đều"
    )

    val sampleSuggestions = listOf(
        "Yoghurt-SầuRiêng-v1" to "Sữa chua sầu riêng RI6",
        "Sauce-PhôMai-Cay" to "Sốt phô mai ớt cay Hàn Quốc",
        "Jam-DâuTằm-Smooth" to "Mứt dâu tằm nghiền mịn không đường",
        "MilkTea-Matcha-Hokkaido" to "Trà sữa Matcha bột béo Hokkaido",
        "Ketchup-HươngThảo" to "Tương cà hương thảo Hy Lạp"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and role choosing banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "HỆ THỐNG COLLABORATIVE R&D KITCHEN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Phân luồng Giao việc (Quản lý) ➜ Thực hành & Nộp kết quả thí điểm (Nhân viên).",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Sub tab Switcher (Role Switcher)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tab 0: Manager
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeSubTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = if (activeSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "QUẢN LÝ GIAO VIỆC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (activeSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tab 1: Employee
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeSubTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = if (activeSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "NHÂN VIÊN NẤU MẺ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (activeSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (activeSubTab == 0) {
            // ==========================================
            // WORKSPACE: MANAGER CREATING & ASSIGNING
            // ==========================================
            
            // Section 1: Creation Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = "KHỞI TẠO MẪU TỔNG & PHÂN CÔNG",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Code Input
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Mã mẫu tổng (Mã số độc nhất):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = mSampleCode,
                            onValueChange = { mSampleCode = it },
                            placeholder = { Text("E.g., ChiliSauce-Premium-S1") },
                            modifier = Modifier.fillMaxWidth().testTag("m_sample_code_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Name Input
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tên gọi / Mô tả cảm quan mẫu tổng:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = mSampleName,
                            onValueChange = { mSampleName = it },
                            placeholder = { Text("E.g., Sốt tương ớt mường khương vị tỏi cay thơm") },
                            modifier = Modifier.fillMaxWidth().testTag("m_sample_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Quick Suggestion Chips for manager
                    Text("💡 Gợi ý thiết kế mẫu chưng cất nhanh:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sampleSuggestions.forEach { (code, name) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                    .clickable {
                                        mSampleCode = code
                                        mSampleName = name
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = "$code ($name)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }

                    // Assign dropdown selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Phân bổ nhân sự thực hành:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { mExpandedEmpMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mSelectedEmp?.name ?: "Bấm chọn nhân sự chịu trách nhiệm...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (mSelectedEmp != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = mExpandedEmpMenu,
                                onDismissRequest = { mExpandedEmpMenu = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                employees.forEach { emp ->
                                    DropdownMenuItem(
                                        text = { Text(emp.name, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            mSelectedEmp = emp
                                            mExpandedEmpMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Description & Technical requirement instruction
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Yêu cầu công thức & Giới hạn kỹ thuật từ Quản lý:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = mDescription,
                            onValueChange = { mDescription = it },
                            placeholder = { Text("E.g., Đạt độ chua dịu pH < 4.4, nấu chưng cất khuấy liên tục, dập áp suất bọt khí...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Date String
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Ngày giao việc:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = mDateCreated,
                            onValueChange = { mDateCreated = it },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // BUTTON SUMBIT
                    Button(
                        onClick = {
                            if (mSampleCode.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Vui lòng nhập mã số mẫu tổng!") }
                                return@Button
                            }
                            if (mSampleName.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Vui lòng nhập tên/mô tả mẫu tổng!") }
                                return@Button
                            }
                            if (mSelectedEmp == null) {
                                scope.launch { snackbarHostState.showSnackbar("Vui lòng chỉ định nhân viên thực hành mẻ này!") }
                                return@Button
                            }

                            // Save RDSample
                            viewModel.addSample(
                                sampleCode = mSampleCode.trim(),
                                sampleName = mSampleName.trim(),
                                assignedEmployeeId = mSelectedEmp!!.id,
                                dateCreated = mDateCreated,
                                description = mDescription.trim()
                            )

                            scope.launch {
                                snackbarHostState.showSnackbar("Đã tạo và phân công mẫu tổng ${mSampleCode} cho ${mSelectedEmp!!.name} thành công!")
                            }

                            // Reset form fields
                            mSampleCode = ""
                            mSampleName = ""
                            mSelectedEmp = null
                            mDescription = ""
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GIAO VIỆC & LƯU PHÂN CÔNG", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 2: Active assignments list and statuses
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("2", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Text(
                                text = "BẢNG TIẾN ĐỘ THEO DÕI MẪU",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Badge count
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${allSamples.size} mẫu",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // Horizontal Filter States
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Tất cả", "Chưa xong", "Hoàn thành").forEach { fText ->
                            val isSelected = mFilterStatus == fText
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.background)
                                    .clickable { mFilterStatus = fText }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = fText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Map samples and render cards
                    val filteredSamples = allSamples.filter {
                        mFilterStatus == "Tất cả" ||
                        (mFilterStatus == "Chưa xong" && it.status == "Đang thực hiện") ||
                        (mFilterStatus == "Hoàn thành" && it.status == "Hoàn thành")
                    }

                    if (filteredSamples.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📭 Không tìm thấy mẫu trong bộ lọc!", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.outline)
                                Text("Vui lòng khởi tạo mẫu mới ở mục bên trên.", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
                            }
                        }
                    } else {
                        filteredSamples.forEach { sample ->
                            val assignedStaff = employees.find { it.id == sample.assignedEmployeeId }
                            val runsCount = allRuns.count { it.sampleCode == sample.sampleCode }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = sample.sampleCode,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // Status representation Badge
                                        val isDone = sample.status == "Hoàn thành"
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isDone) MaterialTheme.colorScheme.primaryContainer
                                                    else Color(0xFFFEF3C7)
                                                , RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isDone) "ĐÃ HOÀN THÀNH" else "ĐANG THỰC HIỆN",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDone) MaterialTheme.colorScheme.primary else Color(0xFFB45309)
                                            )
                                        }
                                    }

                                    Text(
                                        text = sample.sampleName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (sample.description.isNotEmpty()) {
                                        Text(
                                            text = "📋 Yêu cầu: ${sample.description}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }

                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(parseHexColor(assignedStaff?.avatarColorHex))
                                                )
                                                Text(
                                                    text = assignedStaff?.name ?: "Nhân sự đã nghỉ việc",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = "Nhận việc: ${sample.dateCreated} • Đã đun chưng $runsCount lần",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        // Action delete
                                        IconButton(
                                            onClick = {
                                                samplePendingDelete = sample
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Xóa",
                                                tint = Color(0xFFDC2626).copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } else {
            // ==========================================
            // WORKSPACE: EMPLOYEE SUBMITTING DETAILED RUNS
            // ==========================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "🧬 TRẠM KHAI BÁO THÍ NGHIỆM CHI TIẾT",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 1. Employee Profiler Selector (Who are you?)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Vui lòng chọn Tên của bạn để nhận nhiệm vụ:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { eExpandedAccountMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (eSelectedAccount != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(parseHexColor(eSelectedAccount!!.avatarColorHex))
                                            )
                                        }
                                        Text(
                                            text = eSelectedAccount?.name ?: "Bấm chọn Tài khoản của bạn...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = eExpandedAccountMenu,
                                onDismissRequest = { eExpandedAccountMenu = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                employees.forEach { emp ->
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(parseHexColor(emp.avatarColorHex))
                                            )
                                        },
                                        text = { Text(emp.name, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            eSelectedAccount = emp
                                            eExpandedAccountMenu = false
                                            eExpandedSampleCodeDetails = ""
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Load assigned samples checklist
            if (eSelectedAccount == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "🔐 Chọn Tài khoản để mở Khóa Sổ Tay thí nghiệm!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Hệ thống tự động lọc các mẫu tổng mà Quản lý giao riêng cho tài khoản của bạn.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                val assignedSamples = allSamples.filter { it.assignedEmployeeId == eSelectedAccount!!.id }

                Text(
                    text = "MẪU TỔNG ĐƯỢC PHÂN PHÁT CHO BẠN (${assignedSamples.size}):",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp)
                )

                if (assignedSamples.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "🎉 Bạn đang trống lịch nấu ngày hôm nay!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text(text = "Chưa có mẫu tổng nào được phân công thiết kế thêm cho tài khoản này.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Button(
                                onClick = {
                                    // Make dynamic mock assignment
                                    viewModel.addSample(
                                        sampleCode = "Self-Cook-X1",
                                        sampleName = "Tự pha chế nâng cấp hương dâu",
                                        assignedEmployeeId = eSelectedAccount!!.id,
                                        dateCreated = systemTodayStr,
                                        description = "Tự nghiên cứu gia vị bổ sung"
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Tự giao việc nhanh cho bản thân", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    assignedSamples.forEach { sample ->
                        val isExpanded = eExpandedSampleCodeDetails == sample.sampleCode
                        val isSampleCompleted = sample.status == "Hoàn thành"
                        val runsForThisSample = allRuns.filter { it.sampleCode == sample.sampleCode && it.employeeId == eSelectedAccount!!.id }.sortedBy { it.runNumber }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    eExpandedSampleCodeDetails = if (isExpanded) "" else sample.sampleCode
                                    runDurationStr = "25"
                                    runStatus = "Thành công"
                                    runFailureReason = ""
                                }
                                .padding(vertical = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSampleCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                 else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (isExpanded) 2.dp else 1.dp,
                                color = if (isSampleCompleted) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        else if (isExpanded) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Title Box metadata
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSampleCompleted) MaterialTheme.colorScheme.outlineVariant
                                                else MaterialTheme.colorScheme.primaryContainer,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = sample.sampleCode,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSampleCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                                    else MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Badge Status
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSampleCompleted) Icons.Default.CheckCircle else Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = if (isSampleCompleted) MaterialTheme.colorScheme.primary else Color(0xFFD97706),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isSampleCompleted) "ĐÃ HOÀN THÀNH" else "ĐANG THỬ NGHIỆM",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSampleCompleted) MaterialTheme.colorScheme.primary else Color(0xFFD97706)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = sample.sampleName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (sample.description.isNotEmpty()) {
                                    Text(
                                        text = "📋 Yêu cầu của Quản lý: ${sample.description}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = "⚡ Đã nấu chi tiết: ${runsForThisSample.size} lần • Bấm để xem và ghi mẻ nấu mới",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                // EXPANDED PANEL
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                        // LOG OF TRIALS LIST
                                        Text(
                                            text = "📔 NHẬT KÝ NẤU (MẪU CHI TIẾT ĐÃ GHI):",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (runsForThisSample.isEmpty()) {
                                            Text(
                                                text = "Chưa ghi nhận lần thí nghiệm nào cho mẫu tổng này.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        } else {
                                            runsForThisSample.forEach { r ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                                        .padding(10.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = "Lần nấu thứ ${r.runNumber}: Đun ${viewModel.formatDuration(r.durationMs)}",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            if (r.status == "Thất bại" && r.failureReason.isNotEmpty()) {
                                                                Text(
                                                                    text = "❌ Lý do hỏng: ${r.failureReason}",
                                                                    fontSize = 10.sp,
                                                                    color = Color(0xFFDC2626),
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            }
                                                        }

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (r.status == "Thành công") Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                                contentDescription = null,
                                                                tint = if (r.status == "Thành công") MaterialTheme.colorScheme.primary else Color(0xFFDC2626),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                text = r.status.uppercase(),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = if (r.status == "Thành công") MaterialTheme.colorScheme.primary else Color(0xFFDC2626)
                                                            )

                                                            IconButton(
                                                                onClick = {
                                                                    runPendingDelete = r
                                                                },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color(0xFFDC2626).copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // PREPARE INPUT FORM FOR NEW COMPONENT
                                        if (!isSampleCompleted) {
                                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                            val computedNextRun = (runsForThisSample.maxOfOrNull { r -> r.runNumber } ?: 0) + 1

                                            Text(
                                                text = "➕ KHAI BÁO MÔ TẢ LẦN NẤU CHUNG MẪU CHI TIẾT (LẦN $computedNextRun):",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )

                                            // Raw minutes input
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("Thời gian nấu (phút):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    OutlinedTextField(
                                                        value = runDurationStr,
                                                        onValueChange = { runDurationStr = it },
                                                        placeholder = { Text("Số phút") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                }

                                                // Quick choice minutes
                                                Column(modifier = Modifier.weight(1.8f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("Mốc phút nhanh:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        listOf("15", "25", "35", "45", "60").forEach { presetMins ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(MaterialTheme.colorScheme.background)
                                                                    .clickable { runDurationStr = presetMins }
                                                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                                            ) {
                                                                Text(text = "${presetMins}p", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Status Check
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("Đánh giá kết quả mẻ đun nấu:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(if (runStatus == "Thành công") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
                                                            .border(1.dp, if (runStatus == "Thành công") MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(10.dp))
                                                            .clickable { runStatus = "Thành công"; runFailureReason = "" }
                                                            .padding(vertical = 10.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("THÀNH CÔNG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (runStatus == "Thành công") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(if (runStatus == "Thất bại") Color(0xFFFEE2E2) else MaterialTheme.colorScheme.background)
                                                            .border(1.dp, if (runStatus == "Thất bại") Color(0xFFDC2626) else Color.Transparent, RoundedCornerShape(10.dp))
                                                            .clickable { runStatus = "Thất bại" }
                                                            .padding(vertical = 10.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("THẤT BẠI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (runStatus == "Thất bại") Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }

                                            // Failure details if state is failure
                                            AnimatedVisibility(visible = runStatus == "Thất bại") {
                                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("Lý do bếp hỏng / Mô tả lỗi:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                                    OutlinedTextField(
                                                        value = runFailureReason,
                                                        onValueChange = { runFailureReason = it },
                                                        placeholder = { Text("Mùi khét khét đáy, tách pha bơ...") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        failurePresets.forEach { preset ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(Color(0xFFFEF2F2))
                                                                    .clickable { runFailureReason = preset }
                                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                                            ) {
                                                                Text(text = preset, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Add Run Button
                                            Button(
                                                onClick = {
                                                    val minsCook = runDurationStr.toIntOrNull() ?: 25
                                                    if (runStatus == "Thất bại" && runFailureReason.isBlank()) {
                                                        scope.launch { snackbarHostState.showSnackbar("Vui lòng nhập lý do mẫu hỏng dải này!") }
                                                        return@Button
                                                    }

                                                    val cal = java.util.Calendar.getInstance()
                                                    val curEndHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                                                    val curEndMin = cal.get(java.util.Calendar.MINUTE)
                                                    val totalEndMins = curEndHour * 60 + curEndMin
                                                    val totalStartMins = totalEndMins - minsCook
                                                    val startHourVal = ((totalStartMins / 60) + 24) % 24
                                                    val startMinVal = ((totalStartMins % 60) + 60) % 60
                                                    val calcStart = String.format("%02d:%02d", startHourVal, startMinVal)
                                                    val calcEnd = String.format("%02d:%02d", curEndHour, curEndMin)

                                                    viewModel.addCookingRun(
                                                        empId = eSelectedAccount!!.id,
                                                        sampleCode = sample.sampleCode,
                                                        runNumber = computedNextRun,
                                                        durationMinutes = minsCook,
                                                        status = runStatus,
                                                        failureReason = runFailureReason,
                                                        date = systemTodayStr,
                                                        startTimeStr = calcStart,
                                                        endTimeStr = calcEnd
                                                    )

                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Đã thêm lần nấu thử thứ $computedNextRun cho mẫu ${sample.sampleCode} thành công!")
                                                    }

                                                    // Reset Inputs
                                                    runDurationStr = "25"
                                                    runFailureReason = ""
                                                },
                                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("LƯU MẪU CHI TIẾT LẦN NÀY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                            // SUBMIT FINAL REPORT & LOCKS RECORD
                                            Button(
                                                onClick = {
                                                    if (runsForThisSample.isEmpty()) {
                                                        scope.launch { snackbarHostState.showSnackbar("Cần có tối thiểu 1 mẻ nấu chi tiết trước khi nộp báo cáo hoàn thành!") }
                                                        return@Button
                                                    }

                                                    viewModel.updateSampleStatus(sample.sampleCode, "Hoàn thành")
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("🎉 Đã nộp báo cáo mẫu tổng ${sample.sampleCode} về cho Quản lý thành công!")
                                                    }
                                                    eExpandedSampleCodeDetails = ""
                                                },
                                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("NỘP BÁO CÁO & HOÀN THÀNH MẪU TỔNG", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                            }
                                        } else {
                                            // Concluded Sample Info
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = "ℹ️ Mẫu tổng này đã nộp báo cáo & khóa phòng thí nghiệm R&D thành công. Các lần nấu chi tiết đã được đồng bộ kỹ thuật.",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (samplePendingDelete != null) {
        val sample = samplePendingDelete!!
        AlertDialog(
            onDismissRequest = { samplePendingDelete = null },
            title = { Text("CONFIRM DELETION (XÓA CHỈ TIÊU/TASK LỚN)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("Bạn có chắc chắn muốn xóa hoàn toàn mẫu R&D '${sample.sampleCode} - ${sample.sampleName}' khỏi danh sách? Thử nghiệm này và tất cả lượt nấu liên quan sẽ biến mất vĩnh viễn.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSample(sample)
                        scope.launch { snackbarHostState.showSnackbar("Đã xóa hoàn tất mẫu R&D ${sample.sampleCode}!") }
                        samplePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Đồng ý xóa", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { samplePendingDelete = null }) {
                    Text("Hủy bỏ", fontSize = 11.sp)
                }
            }
        )
    }

    if (runPendingDelete != null) {
        val run = runPendingDelete!!
        AlertDialog(
            onDismissRequest = { runPendingDelete = null },
            title = { Text("XÓA MẺ NẤU CHI TIẾT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("Đồng ý xóa vĩnh viễn lượt thử nghiệm mẻ nấu thứ ${run.runNumber} của mẫu thử ${run.sampleCode}?", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCookingRun(run)
                        scope.launch { snackbarHostState.showSnackbar("Đã xóa mẻ nấu thử nghiệm ${run.runNumber}!") }
                        runPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Đồng ý xóa", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { runPendingDelete = null }) {
                    Text("Hủy bỏ", fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
fun ExportReportsTabScreen(
    viewModel: RDViewModel,
    currentYear: String,
    currentMonth: String,
    currentDay: String,
    sampleReports: List<SampleReportItem>,
    employeeReports: List<EmployeeReportSummary>,
    selectedEmployeeDetail: EmployeeReportSummary?,
    onSelectEmployee: (EmployeeReportSummary?) -> Unit,
    onAddEmployeeClick: () -> Unit,
    onDeleteEmployee: (Employee) -> Unit,
    onEditEmployee: (Employee) -> Unit
) {
    val appContext = androidx.compose.ui.platform.LocalContext.current
    var activeSubTab by remember { mutableStateOf(0) } // 0: Báo cáo, 1: Đội ngũ & Quản lý

    if (activeSubTab == 1 && selectedEmployeeDetail != null) {
        // Expand the employee's details full-screen if selected — avoids double sub-tab visuals
        EmployeesTabScreen(
            employeeReports = employeeReports,
            selectedEmployeeDetail = selectedEmployeeDetail,
            onSelectEmployee = onSelectEmployee,
            onAddEmployeeClick = onAddEmployeeClick,
            onDeleteEmployee = onDeleteEmployee,
            onEditEmployee = onEditEmployee,
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Elegant sub-tab Switcher matching LogRun tab layout language
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tab 0: Báo cáo
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeSubTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = if (activeSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "XUẤT BÁO CÁO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (activeSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tab 1: Nhân viên
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeSubTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = if (activeSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "NHÂN VIÊN & QUẢN LÝ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (activeSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (activeSubTab == 0) {
                // Export report view layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "TRUNG TÂM XUẤT BÁO CÁO (EXPORT CENTER)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Đóng gói dữ liệu nấu mẫu R&D chuẩn hóa của đội ngũ thành báo cáo điện tử phục vụ lưu trữ nội bộ.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                            // Live Summary details
                            Text(
                                text = "BẠN ĐANG LỌC: Ngày $currentDay - Tháng $currentMonth - Năm $currentYear",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = "• Tổng số mẫu ghi nhận: ${sampleReports.size} mẫu", fontSize = 11.sp)
                            Text(text = "• Nhân viên hoạt động: ${employeeReports.size} người", fontSize = 11.sp)

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Excel Export Action
                                Button(
                                    onClick = { viewModel.simulateExcelExport(appContext) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Xuất Excel (.xlsx)", fontSize = 11.sp)
                                }

                                // PDF Export Action
                                Button(
                                    onClick = { viewModel.simulatePdfExport(appContext) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Xuất PDF (.pdf)", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Live Visual Doc Preview
                    Text(
                        text = "XEM TRƯỚC BẢN BÁO CÁO TỔNG QUAN:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(2.dp, Color.LightGray)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Mock Letterhead Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "CÔNG TY CỔ PHẦN THỰC PHẨM VÀ NƯỚC GIẢI KHÁT NAM VIỆT", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.Gray)
                                    Text(text = "Phòng Thử Nghiệm Sản Phẩm Mới", fontSize = 8.sp, color = Color.Gray)
                                }
                                Text(text = "MẪU BÁO CÁO", fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = Color.DarkGray)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "BÁO CÁO ĐÁNH GIÁ TIẾN ĐỘ THỰC HIỆN NẤU MẪU",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.Black
                            )
                            Text(
                                text = "Thời kỳ: Ngày $currentDay / $currentMonth / $currentYear",
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.DarkGray
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Table structure
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF3F4F6))
                                    .padding(4.dp)
                            ) {
                                Text(text = "Nhân viên", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1.5f))
                                Text(text = "Mẫu thử", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1.2f))
                                Text(text = "Số mẻ nấu", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(text = "Thành công", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(text = "T.gian tổng", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }

                            Divider(color = Color.Gray, thickness = 1.dp)

                            employeeReports.forEach { rep ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    Text(text = rep.employee.name, fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(1.5f))
                                    Text(text = "${rep.totalSamplesCount} mẫu", fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(1.2f))
                                    Text(text = "${rep.totalRunsCount} lần", fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text(text = "${rep.successCount} Đ - ${rep.failureCount} L", fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text(text = viewModel.formatDuration(rep.totalDurationMs), fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Divider(color = Color.LightGray)

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Người lập biểu", fontSize = 8.sp, color = Color.DarkGray)
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(text = "Hệ thống R&D", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.Black)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Trưởng Bộ phận Duyệt", fontSize = 8.sp, color = Color.DarkGray)
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(text = "Nguyễn Thị Thúy", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.Black)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            } else {
                // Employees section rendered with 0 extra parent padding
                EmployeesTabScreen(
                    employeeReports = employeeReports,
                    selectedEmployeeDetail = selectedEmployeeDetail,
                    onSelectEmployee = onSelectEmployee,
                    onAddEmployeeClick = onAddEmployeeClick,
                    onDeleteEmployee = onDeleteEmployee,
                    onEditEmployee = onEditEmployee,
                    viewModel = viewModel,
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
fun AddEmployeeDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Nhân viên R&D") }
    var selectedColor by remember { mutableStateOf("#3B82F6") }

    val colorsList = listOf(
        "#3B82F6", // Blue
        "#EF4444", // Red
        "#10B981", // Green
        "#F59E0B", // Amber
        "#8B5CF6", // Purple
        "#EC4899", // Pink
        "#14B8A6", // Teal
        "#6366F1"  // Indigo
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("THIẾT LẬP THÊM NHÂN VIÊN MỚI", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Họ và Tên") },
                    placeholder = { Text("E.g., Nguyễn Văn A") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Vai trò chuyên môn") },
                    placeholder = { Text("E.g., Chuyên gia nấu mẫu nước sốt") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(text = "Chọn màu sắc nhận diện đại diện:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorsList.forEach { colStr ->
                        val col = Color(android.graphics.Color.parseColor(colStr))
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (selectedColor == colStr) 2.dp else 0.dp,
                                    color = if (selectedColor == colStr) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colStr }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, role, selectedColor)
                    }
                }
            ) {
                Text("Lưu nhân sự")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EmployeeTaskCard(
    sample: RDSample,
    sampleRuns: List<RDRun>,
    viewModel: RDViewModel,
    onStatusToggle: () -> Unit,
    onClick: () -> Unit,
    onSampleLongClick: ((RDSample) -> Unit)? = null,
    onRunLongClick: ((RDRun) -> Unit)? = null
) {
    val sampleSuccessCount = sampleRuns.count { it.status == "Thành công" }
    val sampleFailureCount = sampleRuns.count { it.status == "Thất bại" }
    val totalRuns = sampleRuns.size
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onSampleLongClick?.invoke(sample) }
            )
            .testTag("employee_sample_card_${sample.sampleCode}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.3.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Code and Status Pill with ripple
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = "Mã",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = sample.sampleCode,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    )
                }

                // Interactive Status Pill
                val isCompleted = sample.status == "Hoàn thành"
                val statusColor = if (isCompleted) Color(0xFF10B981) else Color(0xFFF59E0B)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .clickable(onClick = onStatusToggle)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColor, CircleShape)
                        )
                        Text(
                            text = sample.status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main task name
            Text(
                text = sample.sampleName,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Estimations and recipe notes if present
            if (sample.estimatedTimeStr.isNotEmpty() || sample.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (sample.estimatedTimeStr.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Dự kiến",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Thời gian mẫu dự kiến: ${sample.estimatedTimeStr}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (sample.description.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = "HD",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(12.dp).padding(top = 1.dp)
                            )
                            Text(
                                text = "Công thức/Yêu cầu: ${sample.description}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trial details sub stats card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "SỐ THỬ NGHIỆM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Text(text = "$totalRuns mẻ nấu", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "ĐẠT TIÊU CHUẨN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Text(text = "$sampleSuccessCount lần", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "MẺ LỖI/HỎNG", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Text(text = "$sampleFailureCount lần", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                }
            }

            // Simple visual indicator bar
            if (totalRuns > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    val successWeight = sampleSuccessCount.toFloat() / totalRuns
                    val failWeight = sampleFailureCount.toFloat() / totalRuns

                    if (successWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(successWeight)
                                .background(Color(0xFF10B981))
                        )
                    }
                    if (failWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(failWeight)
                                .background(Color(0xFFEF4444))
                        )
                    }
                }
            }

            // 📊 BỘ KPI ĐÁNH GIÁ & ĐỊNH LƯỢNG CHẤT LƯỢNG R&D
            if (totalRuns > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Calculations
                val expectedMin = try {
                    sample.estimatedTimeStr.filter { it.isDigit() }.toFloatOrNull() ?: 60f
                } catch (e: Exception) {
                    60f
                }
                val expectedMs = expectedMin * 60 * 1000L
                val totalActualMs = sampleRuns.sumOf { it.durationMs }
                val totalActualMin = totalActualMs / (1000f * 60f)
                val timeRatioPercent = if (expectedMin > 0f) (totalActualMin / expectedMin * 100f).toInt() else 100
                
                // Compute trial attempt multipliers and overtime penalty based on user custom rule
                val runScoresData = sampleRuns.filter { it.status == "Thành công" }.map { run ->
                    val isOvertime = run.durationMs > expectedMs
                    val overtimeRatio = if (expectedMs > 0) run.durationMs.toFloat() / expectedMs else 1.0f
                    
                    val calculatedScore = if (run.runNumber == 1) {
                        if (isOvertime) {
                            // Trường hợp đạt 1 lần nhưng quá thời gian -> tính trong khoảng <90% - >=50% (89% -> 50% dựa trên mức độ quá giờ)
                            val excessScale = (overtimeRatio - 1.0f).coerceIn(0f, 1.0f)
                            89f - (39f * excessScale)
                        } else {
                            // Đúng hạn lần một đạt 100%
                            100f
                        }
                    } else {
                        // Trường hợp thời gian đạt nhưng đun nấu nhiều lần (số lần đun >= 2) -> Khung đạt <90% - >=50%
                        val baseAttemptPercent = when (run.runNumber) {
                            2 -> 80f   // Lần 2: 80% (trong khoảng <90% - >=50%)
                            3 -> 65f   // Lần 3: 65% (trong khoảng <90% - >=50%)
                            else -> 50f // Lần 4+: 50% (trong khoảng <90% - >=50%)
                        }
                        
                        if (isOvertime) {
                            // Nếu vừa nấu nhiều mẻ vừa quá giờ thì giảm thêm tối đa 15%, tối thiểu giữ mốc 50%
                            val scale = (overtimeRatio - 1.0f).coerceIn(0f, 1.0f)
                            (baseAttemptPercent - 15f * scale).coerceAtLeast(50f)
                        } else {
                            baseAttemptPercent
                        }
                    }
                    val displayAttemptPercent = when (run.runNumber) {
                        1 -> if (isOvertime) 89 else 100
                        2 -> 80
                        3 -> 65
                        else -> 50
                    }
                    val displayOvertimePenaltyFactor = if (isOvertime) {
                        if (run.runNumber == 1) {
                            (calculatedScore / 89f * 100f).toInt()
                        } else {
                            (calculatedScore / displayAttemptPercent.toFloat() * 100f).toInt()
                        }
                    } else {
                        100
                    }
                    Triple(calculatedScore, displayAttemptPercent, displayOvertimePenaltyFactor)
                }
                
                val maxScoreTriple = runScoresData.maxByOrNull { it.first }
                val maxSuccessfulScore = maxScoreTriple?.first ?: 0f
                val bestAttemptMultiplierPercent = maxScoreTriple?.second ?: 0
                val bestOvertimePenaltyPercent = maxScoreTriple?.third ?: 100
                
                val failureDeduction = sampleFailureCount * 10f
                val compositeScore = (maxSuccessfulScore - failureDeduction).coerceIn(0f, 100f)
                
                // If overtime rate & failure rate are high, then dropped objective (Rớt mục tiêu)
                val isObjectiveDropped = ((sampleFailureCount >= 2 && totalActualMs > expectedMs) || (compositeScore < 45f)) && (sample.status != "Hoàn thành")
                
                val objectiveStatusText = when {
                    isObjectiveDropped -> "⚠️ RỚT MỤC TIÊU (Do quá hạn lỗi & trễ)"
                    compositeScore >= 80f -> "🏆 MỤC TIÊU ĐẠT XUẤT SẮC"
                    compositeScore >= 60f -> "✅ MỤC TIÊU ĐẠT CHUẨN (Khá)"
                    else -> "⚠️ ĐẠT THẤP / CẦN PHỤC HỒI CHI TIẾT"
                }
                
                val objectiveColor = when {
                    isObjectiveDropped -> Color(0xFFEF4444)
                    compositeScore >= 80f -> Color(0xFF10B981)
                    compositeScore >= 60f -> Color(0xFF3B82F6)
                    else -> Color(0xFFF59E0B)
                }

                var showFormulaDetails by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(objectiveColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .border(1.dp, objectiveColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "KPI",
                                tint = objectiveColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Phân tích hiệu suất R&D",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = objectiveColor
                            )
                        }
                        
                        Text(
                            text = if (showFormulaDetails) "Ẩn chi tiết công thức ▲" else "Xem chi tiết công thức ▼",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showFormulaDetails = !showFormulaDetails }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Main metrics row: Actual vs Expected & Quality Rating %
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "⏱️ Tổng thời gian thực đun nấu:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${totalActualMin.toInt()} phút / ${expectedMin.toInt()} phút (${timeRatioPercent}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (timeRatioPercent > 100) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "⭐ Điểm chất lượng chỉ số:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${compositeScore.toInt()}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = objectiveColor
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Objective status badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(objectiveColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = objectiveStatusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = objectiveColor
                        )
                    }

                    if (showFormulaDetails) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = objectiveColor.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "📐 Công thức & Đơn vị tính hiệu suất:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "• Tỉ lệ đạt theo Số Lần Thử mẻ:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "   - Lần 1: 100% | Lần 2: 80% | Lần 3: 65% | Lần 4+: 50% (khung đạt tỉ lệ <90% - >=50%)\n" +
                                   "   -> Hệ số tối đa cho lượt thành công: ${bestAttemptMultiplierPercent}%",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 13.sp
                        )
                        
                        Text(
                            text = "• Tỷ lệ khi Quá Thời Gian (Overtime Mapping):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "   - Đạt mẻ 1 nhưng quá giờ quy định -> Áp dụng khung điểm <90% - >=50%\n" +
                                   "     Điểm giảm từ 89% về 50% tùy thuộc mức độ trễ giờ thực tế so với dự kiến.\n" +
                                   "   -> Hệ số hiệu chuẩn thời gian mẻ tốt nhất: ${bestOvertimePenaltyPercent}%",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 13.sp
                        )
                        
                        Text(
                            text = "• Khấu trừ mẻ lỗi/mẻ hỏng (Failure Deduction):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "   - Trừ thẳng 10% cho mỗi lượt đun nấu bị hỏng/lỗi.\n" +
                                   "   -> Trừ mẻ lỗi: -${failureDeduction.toInt()}% (${sampleFailureCount} mẻ hỏng)",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 13.sp
                        )
                        
                        Text(
                            text = "• Công thức điểm tổng hợp cuối cùng:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "   Điểm = max(0%, [Tỉ lệ lần nấu đạt] * [Hiệu suất thời gian] - [Trừ lỗi mẻ hỏng])\n" +
                                   "   👉 Điểm cuối cùng: ${compositeScore.toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = objectiveColor,
                            lineHeight = 14.sp
                        )
                        
                        Text(
                            text = "• Điều kiện Rớt Mục Tiêu (Dropped Target):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "   - Đánh giá Rớt Mục Tiêu nếu nấu hỏng lỗi nhiều (>= 2 lần hỏng) + đồng thời tổng thời gian thực tế đã vượt thời gian quy định, hoặc điểm chất lượng tổng hợp tụt dưới 45%.",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            // Trial detail run summaries if any runs exist
            if (sampleRuns.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Lịch sử chi tiết các mẻ nấu thực nghiệm:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 0.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sampleRuns.sortedBy { it.runNumber }.forEach { run ->
                        val minutes = run.durationMs / 60000
                        val seconds = (run.durationMs % 60000) / 1000
                        val timeStr = if (minutes > 0) "${minutes}p ${seconds}s" else "${seconds}s"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (run.status == "Thành công") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    RoundedCornerShape(8.dp)
                                )
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { onRunLongClick?.invoke(run) }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (run.status == "Thành công") Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = run.status,
                                    tint = if (run.status == "Thành công") Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Lần thử ${run.runNumber}: $timeStr",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (run.status == "Thành công") Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                            if (run.failureReason.isNotEmpty()) {
                                Text(
                                    text = "Lỗi: ${run.failureReason}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            } else if (run.status == "Thành công") {
                                Text(
                                    text = "Đạt chuẩn",
                                    fontSize = 10.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nhấp quản lý quá trình nấu ➔",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmployeeProgressTabScreen(
    viewModel: RDViewModel,
    snackbarHostState: SnackbarHostState,
    onAddClick: () -> Unit,
    onSampleLongClick: (RDSample) -> Unit,
    onRunLongClick: (RDRun) -> Unit
) {
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val allSamples by viewModel.allSamples.collectAsStateWithLifecycle()
    val allRuns by viewModel.allRuns.collectAsStateWithLifecycle()
    
    // Choose employee to display progress - null by default represents "Tất cả nhân viên"
    var selectedEmp by remember { mutableStateOf<Employee?>(null) }
    
    val todayParts = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(java.util.Date()).split("-")
    }
    var filterYear by remember { mutableStateOf(todayParts.getOrNull(0) ?: "2026") }
    var filterMonth by remember { mutableStateOf(todayParts.getOrNull(1) ?: "05") }
    var filterDay by remember { mutableStateOf(todayParts.getOrNull(2) ?: "31") } 

    var expandedYearFilter by remember { mutableStateOf(false) }
    var expandedMonthFilter by remember { mutableStateOf(false) }
    var expandedDayFilter by remember { mutableStateOf(false) }

    // Clicked sample detail popup state
    var expandedSampleDetail by remember { mutableStateOf<RDSample?>(null) }
    
    // Excel preview dialog state
    var showExcelPreview by remember { mutableStateOf(false) }

    var activeSubTab by remember { mutableStateOf(0) } // 0: TIẾN ĐỘ THỰC TẾ, 1: XUẤT BÁO CÁO

    val scope = rememberCoroutineScope()

    val currentEmp = selectedEmp

    // WORK TASK LIST DISPLAY (Unified with clean, robust Moss/Sage visual language)
    val localSelectedEmp = selectedEmp
    val empSamples = remember(allSamples, localSelectedEmp, filterYear, filterMonth, filterDay) {
        allSamples.filter { sample ->
            val matchEmployee = localSelectedEmp == null || sample.assignedEmployeeId == localSelectedEmp.id
            val parts = sample.dateCreated.split("-") 
            val mY = filterYear == "Tất cả" || parts.getOrNull(0) == filterYear
            val mM = filterMonth == "Tất cả" || parts.getOrNull(1) == filterMonth
            val mD = filterDay == "Tất cả" || parts.getOrNull(2) == filterDay
            matchEmployee && mY && mM && mD
        }
    }

    // AGGREGATE STATS FOR CURRENT FILTER PREPARATION
    val currentRunsFilter = remember(allRuns, empSamples) {
        allRuns.filter { run -> empSamples.any { it.sampleCode == run.sampleCode } }
    }
    val totalJobsCount = empSamples.size
    val totalRunsCount = currentRunsFilter.size
    val successRunsCount = currentRunsFilter.count { it.status == "Thành công" }
    val failureRunsCount = currentRunsFilter.count { it.status == "Thất bại" }
    
    val successPercent = if (totalRunsCount > 0) (successRunsCount.toFloat() / totalRunsCount * 100f) else 0f
    val failurePercent = if (totalRunsCount > 0) (failureRunsCount.toFloat() / totalRunsCount * 100f) else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // TOP HEADER HUB
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Trung tâm tiến độ",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "TRUNG TÂM TIẾN ĐỘ NHÂN VIÊN",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Theo dõi, hướng dẫn & phân công nhiệm vụ nấu",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Giao việc nhanh",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            // SUB-TAB TUNNEL DECK
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tab 0: TIẾN ĐỘ THỰC TẾ
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeSubTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = if (activeSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "TIẾN ĐỘ THỰC TẾ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (activeSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tab 1: XUẤT BÁO CÁO
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeSubTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = if (activeSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "XUẤT BÁO CÁO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (activeSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (activeSubTab == 0) {
        item {
            // STAFF SELECTOR DECK
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "DANH SÁCH NHÂN SỰ R&D ĐANG HOẠT ĐỘNG:",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 0.3.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Tất cả nhân viên" selector card at the beginning
                Card(
                    modifier = Modifier
                        .clickable { selectedEmp = null }
                        .widthIn(min = 130.dp, max = 180.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedEmp == null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                         else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (selectedEmp == null) 1.8.dp else 1.dp,
                        color = if (selectedEmp == null) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (selectedEmp == null) 3.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Tất cả nhân viên",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Tất cả nhân viên",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selectedEmp == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Toàn bộ R&D crew",
                                fontSize = 8.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                employees.forEach { emp ->
                    val isSelected = selectedEmp?.id == emp.id
                    val avatarColor = try {
                        Color(android.graphics.Color.parseColor(emp.avatarColorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    // Live statistics calculations per staff
                    val empSamplesAll = allSamples.filter { it.assignedEmployeeId == emp.id }
                    val totalJobs = empSamplesAll.size
                    val completedJobs = empSamplesAll.count { it.status == "Hoàn thành" }

                    Card(
                        modifier = Modifier
                            .clickable { selectedEmp = emp }
                            .widthIn(min = 130.dp, max = 180.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                             else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Circular Initial Avatar
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emp.name.split(" ").lastOrNull()?.take(1)?.uppercase() ?: "E",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = emp.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = emp.role,
                                        fontSize = 8.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "$completedJobs/$totalJobs",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        item {
            // FOCUS ACTIVE EMPLOYEE BANNER HERO
            if (currentEmp == null) {
            // General banner when displaying details for All Employees
            val totalCrewSamples = remember(allSamples, filterYear, filterMonth, filterDay) {
                allSamples.filter { sample ->
                    val parts = sample.dateCreated.split("-") 
                    val mY = filterYear == "Tất cả" || parts.getOrNull(0) == filterYear
                    val mM = filterMonth == "Tất cả" || parts.getOrNull(1) == filterMonth
                    val mD = filterDay == "Tất cả" || parts.getOrNull(2) == filterDay
                    mY && mM && mD
                }.size
            }
            val completedCrewSamples = remember(allSamples, filterYear, filterMonth, filterDay) {
                allSamples.filter { sample ->
                    val parts = sample.dateCreated.split("-") 
                    val mY = filterYear == "Tất cả" || parts.getOrNull(0) == filterYear
                    val mM = filterMonth == "Tất cả" || parts.getOrNull(1) == filterMonth
                    val mD = filterDay == "Tất cả" || parts.getOrNull(2) == filterDay
                    mY && mM && mD
                }.count { it.status == "Hoàn thành" }
            }
            val progressFraction = if (totalCrewSamples > 0) completedCrewSamples.toFloat() / totalCrewSamples else 0f

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Toàn bộ",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DANH SÁCH TOÀN BỘ NHÂN sự R&D",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Đang xem: Tất cả đầu việc nấu mẫu trên hệ thống phễu R&D",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            Text(
                                text = "$completedCrewSamples/$totalCrewSamples mẫu hoàn thành",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        } else {
            val empSamplesAll = remember(allSamples, currentEmp) {
                allSamples.filter { it.assignedEmployeeId == currentEmp.id }
            }
            val totalJobs = empSamplesAll.size
            val completedJobs = empSamplesAll.count { it.status == "Hoàn thành" }
            val progressFraction = if (totalJobs > 0) completedJobs.toFloat() / totalJobs else 0f
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val avatarColor = try {
                        Color(android.graphics.Color.parseColor(currentEmp.avatarColorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentEmp.name.split(" ").lastOrNull()?.take(1)?.uppercase() ?: "E",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentEmp.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Vai trò phụ trách: ${currentEmp.role}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            Text(
                                text = "$completedJobs/$totalJobs mẫu đạt",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        }

        item {
            // INTEGRATED DATE FILTERS DECK
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Bộ lọc",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "BỘ LỌC THỜI GIAN NHIỆM VỤ:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.3.sp
                    )
                }

                // Preset Shortcut Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val systemTodayLabel = remember {
                        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                        "Hôm nay (${sdf.format(java.util.Date())})"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                            .clickable {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val parts = sdf.format(java.util.Date()).split("-")
                                filterDay = parts.getOrNull(2) ?: "31"
                                filterMonth = parts.getOrNull(1) ?: "05"
                                filterYear = parts.getOrNull(0) ?: "2026"
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(systemTodayLabel, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                filterDay = "Tất cả"
                                filterMonth = "Tất cả"
                                filterYear = "Tất cả"
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("Xem tất cả", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day Selector Box
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedDayFilter = true }
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filterDay == "Tất cả") "Tất cả ngày" else "Ngày $filterDay",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = expandedDayFilter, onDismissRequest = { expandedDayFilter = false }) {
                        listOf("Tất cả", "28", "29", "30", "31").forEach { d ->
                            DropdownMenuItem(text = { Text(if (d == "Tất cả") "Tất cả ngày" else "Ngày $d", fontSize = 11.sp) }, onClick = { filterDay = d; expandedDayFilter = false })
                        }
                    }
                }

                // Month Selector Box
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedMonthFilter = true }
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filterMonth == "Tất cả") "Tất cả tháng" else "Tháng $filterMonth",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = expandedMonthFilter, onDismissRequest = { expandedMonthFilter = false }) {
                        listOf("Tất cả", "05", "06").forEach { m ->
                            DropdownMenuItem(text = { Text(if (m == "Tất cả") "Tất cả tháng" else "Tháng $m", fontSize = 11.sp) }, onClick = { filterMonth = m; expandedMonthFilter = false })
                        }
                    }
                }

                // Year Selector Box
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedYearFilter = true }
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filterYear == "Tất cả") "Tất cả năm" else "Năm $filterYear",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = expandedYearFilter, onDismissRequest = { expandedYearFilter = false }) {
                        listOf("Tất cả", "2026", "2025").forEach { y ->
                            DropdownMenuItem(text = { Text(if (y == "Tất cả") "Tất cả năm" else "Năm $y", fontSize = 11.sp) }, onClick = { filterYear = y; expandedYearFilter = false })
                        }
                    }
                }
            }
        }
        }

        // WORK TASK LIST DISPLAY (Unified with clean, robust Moss/Sage visual language)


        if (empSamples.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally, 
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("📭", fontSize = 36.sp)
                            Text(
                                text = "Chưa có nhiệm vụ nấu mẫu nào", 
                                fontSize = 13.sp, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Bấm Giao việc nhanh (+) ở góc trên bên phải để tạo phân công nhiệm vụ mới ngay lập tức!", 
                                fontSize = 11.sp, 
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            } else {
                items(empSamples) { sample ->
                    val sampleRuns = remember(allRuns, sample.sampleCode) {
                        allRuns.filter { it.sampleCode == sample.sampleCode }
                    }
                    
                    EmployeeTaskCard(
                        sample = sample,
                        sampleRuns = sampleRuns,
                        viewModel = viewModel,
                        onStatusToggle = {
                            val nextStatus = if (sample.status == "Hoàn thành") "Đang thực hiện" else "Hoàn thành"
                            viewModel.updateSampleStatus(sample.sampleCode, nextStatus)
                            scope.launch {
                                snackbarHostState.showSnackbar("Đã chuyển ${sample.sampleCode} sang: $nextStatus")
                            }
                        },
                        onClick = {
                            expandedSampleDetail = sample
                        },
                        onSampleLongClick = onSampleLongClick,
                        onRunLongClick = onRunLongClick
                    )
                }
            }

            // AGGREGATE KPI STATS PANEL & EXCEL BUTTON AT THE BOTTOM
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Analytics, "Thống kê", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "HIỆU SUẤT ĐẠT CHUẨN R&D CỦA CREW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Perfect visual indicators
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("TỶ LỆ ĐẠT", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", successPercent),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        text = "$successRunsCount mẻ đạt",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("TỶ LỆ LỖI", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFFC62828))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", failurePercent),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFC62828)
                                    )
                                    Text(
                                        text = "$failureRunsCount mẻ lỗi",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Mẫu tổng được giao: $totalJobsCount mẫu", fontSize = 9.5.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("Mẻ nấu thực hiện: $totalRunsCount mẻ", fontSize = 9.5.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                            
                            val completedTasks = empSamples.count { it.status == "Hoàn thành" }
                            val compPercent = if (totalJobsCount > 0) (completedTasks.toFloat() / totalJobsCount * 100f) else 0f
                            Text(
                                text = "Hoàn thành: ${String.format(Locale.US, "%.0f%%", compPercent)} mẫu tổng",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // EXCEL REPORT DOWNLOAD SIMULATION BUTTON
                        Button(
                            onClick = {
                                viewModel.simulateExcelExport(appContext)
                                showExcelPreview = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF107C41), // MS EXCEL Green
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.FileDownload, "Download Excel", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("XUẤT BÁO CÁO TOÀN DIỆN RA EXCEL (.XLSX)", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

        } else { // activeSubTab == 1: RENDER EXPORT CENTER & LIVE PREVIEW DOC
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "TRUNG TÂM XUẤT BÁO CÁO R&D",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Hệ thống tự động biên soạn và chuẩn hóa dữ liệu mẻ nấu của từng nhân sự thành định dạng báo cáo chuyên nghiệp.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Live Summary details
                        Text(
                            text = "BẢN TIN BÁO CÁO HIỆN TẠI (LỌC THEO LỊCH SỬ):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "• Khoảng thời gian: Ngày $filterDay / Tháng $filterMonth / Năm $filterYear", fontSize = 11.sp)
                        Text(text = "• Tổng số mẫu ghi nhận: ${empSamples.size} mẫu tổng", fontSize = 11.sp)
                        Text(text = "• Số lần nấu thử nghiệm: ${totalRunsCount} lần nấu", fontSize = 11.sp)
                        Text(text = "• Nhân sự vận hành hoạt động: ${employees.size} người", fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Excel Export Action
                            Button(
                                onClick = {
                                    viewModel.simulateExcelExport(appContext)
                                    showExcelPreview = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Xuất Excel (.xlsx)", fontSize = 11.sp)
                            }

                            // PDF Export Action
                            Button(
                                onClick = {
                                    viewModel.simulatePdfExport(appContext)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Xuất PDF (.pdf)", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item {
                // Live Visual Doc Preview
                Text(
                    text = "XEM TRƯỚC BẢN IN BÁO CÁO TIẾN ĐỘ & HIỆU SUẤT CREW",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.2.dp, Color.LightGray)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Mock Letterhead Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "CÔNG TY CỔ PHẦN THỰC PHẨM VÀ NƯỚC GIẢI KHÁT NAM VIỆT", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.Gray)
                                Text(text = "Phòng Thử Nghiệm Sản Phẩm Mới", fontSize = 8.sp, color = Color.Gray)
                            }
                            Text(text = "MẪU BÁO CÁO", fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = Color.DarkGray)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "BÁO CÁO ĐÁNH GIÁ TIẾN ĐỘ THỰC HIỆN NẤU MẪU R&D",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Black
                        )
                        Text(
                            text = "Lịch trình lọc: Ngày $filterDay / $filterMonth / $filterYear",
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Table structure
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF3F4F6))
                                .padding(4.dp)
                        ) {
                            Text(text = "Nhân viên", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1.5f))
                            Text(text = "Mẫu thử", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1.2f))
                            Text(text = "Số mẻ nấu", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text(text = "Đạt / Hỏng", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                            Text(text = "T.gian tổng", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                        }

                        Divider(color = Color.Gray, thickness = 1.dp)

                        val localEmployeeReports = remember(employees, empSamples, allRuns) {
                            employees.map { emp ->
                                val empSamplesAll = empSamples.filter { it.assignedEmployeeId == emp.id }
                                val empRuns = allRuns.filter { it.employeeId == emp.id && empSamplesAll.any { s -> s.sampleCode == it.sampleCode } }
                                val totalRuns = empRuns.size
                                val sumDuration = empRuns.sumOf { it.durationMs }
                                val successes = empRuns.count { it.status == "Thành công" }
                                val failures = empRuns.count { it.status == "Thất bại" }
                                val successRate = if (totalRuns > 0) (successes.toFloat() / totalRuns * 100) else 0f
                                EmployeeReportSummary(
                                    employee = emp,
                                    totalSamplesCount = empSamplesAll.size,
                                    totalRunsCount = totalRuns,
                                    totalDurationMs = sumDuration,
                                    successCount = successes,
                                    failureCount = failures,
                                    successRate = successRate,
                                    sampleList = emptyList()
                                )
                            }
                        }

                        localEmployeeReports.forEach { rep ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                            ) {
                                Text(text = rep.employee.name, fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1.5f))
                                Text(text = "${rep.totalSamplesCount} mẫu", fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1.2f))
                                Text(text = "${rep.totalRunsCount} lần", fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                                Text(text = "${rep.successCount} Đ - ${rep.failureCount} H", fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                                Text(text = viewModel.formatDuration(rep.totalDurationMs), fontSize = 9.sp, color = Color.Black, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Divider(color = Color.LightGray)

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Người lập biểu", fontSize = 8.sp, color = Color.DarkGray)
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(text = "Hệ thống R&D", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.Black)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Trưởng Bộ phận Duyệt", fontSize = 8.sp, color = Color.DarkGray)
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(text = "Nguyễn Thị Thúy", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    // EXCEL PREVIEW DIALOG SHEET GRID
    if (showExcelPreview) {
        val previewRows = remember(empSamples, allRuns, employees) {
            val list = mutableListOf<Triple<RDSample, RDRun, Employee?>>()
            empSamples.forEach { sample ->
                val sampleRuns = allRuns.filter { it.sampleCode == sample.sampleCode }
                val emp = employees.find { it.id == sample.assignedEmployeeId }
                if (sampleRuns.isEmpty()) {
                    list.add(Triple(sample, RDRun(sampleCode = sample.sampleCode, employeeId = sample.assignedEmployeeId, runNumber = 0, durationMs = 0, status = "Chưa thử nghiệm", failureReason = "Chờ mẻ đầu tiên", dateString = sample.dateCreated), emp))
                } else {
                    sampleRuns.forEach { run ->
                        list.add(Triple(sample, run, emp))
                    }
                }
            }
            list
        }

        Dialog(
            onDismissRequest = { showExcelPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column {
                    // Excel Header Green
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF107C41))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.TableChart, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Column {
                                Text("BẢNG ĐỐI CHIẾU EXCEL ĐÃ XUẤT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Text("downloads/Bao_Cao_Tien_Do_R&D.xlsx (Sheet1)", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(onClick = { showExcelPreview = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Stats overview in excel sheet
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3F4F6))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Trang tính: Sheet1 (BaoCaoR&D)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Tổng: ${previewRows.size} mẻ", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF107C41))
                            Text("Đạt: ${previewRows.count { it.second.status == "Thành công" }} mẻ", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                            Text("Lỗi: ${previewRows.count { it.second.status == "Thất bại" }} mẻ", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828))
                        }
                    }

                    // Scrollable Table Grid representing Excel rows
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                // Column Headers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE5E7EB))
                                        .border(0.5.dp, Color.LightGray)
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("STT", modifier = Modifier.width(36.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("Mã Mẫu", modifier = Modifier.width(90.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("Nhân Viên", modifier = Modifier.width(90.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("Lần", modifier = Modifier.width(32.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("Thời gian", modifier = Modifier.width(60.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("Kết Quả", modifier = Modifier.width(70.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("Lý Do Lỗi / Ghi Chú R&D", modifier = Modifier.weight(1f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            items(previewRows.size) { index ->
                                val (sample, run, emp) = previewRows[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (index % 2 == 0) Color.White else Color(0xFFF9FAFB))
                                        .border(0.5.dp, Color(0xFFE5E7EB).copy(alpha = 0.5f))
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}", modifier = Modifier.width(36.dp), fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                    Text(sample.sampleCode, modifier = Modifier.width(90.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(emp?.name ?: "Toàn bộ Crew", modifier = Modifier.width(90.dp), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (run.runNumber == 0) "-" else "#${run.runNumber}", modifier = Modifier.width(32.dp), fontSize = 10.sp, textAlign = TextAlign.Center)
                                    Text(if (run.runNumber == 0) "Chưa nấu" else "${run.durationMs / 60000} phút", modifier = Modifier.width(60.dp), fontSize = 10.sp)
                                    
                                    val statusBg = when (run.status) {
                                        "Thành công" -> Color(0xFFD1FAE5)
                                        "Thất bại" -> Color(0xFFFEE2E2)
                                        else -> Color(0xFFF3F4F6)
                                    }
                                    val statusTextcol = when (run.status) {
                                        "Thành công" -> Color(0xFF065F46)
                                        "Thất bại" -> Color(0xFF991B1B)
                                        else -> Color.DarkGray
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(70.dp)
                                            .padding(end = 4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(statusBg)
                                            .padding(vertical = 2.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(run.status, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = statusTextcol)
                                    }
                                    Text(
                                        text = if (run.status == "Thành công") "Mẻ mẫu đạt chỉ tiêu xuất sắc" else run.failureReason.ifEmpty { "Đang lên kế hoạch" },
                                        modifier = Modifier.weight(1f),
                                        fontSize = 9.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (run.status == "Thất bại") Color(0xFF991B1B) else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    // Bottom close
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE5E7EB))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showExcelPreview = false }) {
                            Text("Đóng bảng preview", color = Color(0xFF107C41), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    // INTERACTIVE PROCESS DETAIL DIALOG 
    val currentSampleDetail = expandedSampleDetail
    if (currentSampleDetail != null) {
        SampleCookingProcessDialog(
            sample = currentSampleDetail,
            viewModel = viewModel,
            onDismiss = { expandedSampleDetail = null },
            snackbarHostState = snackbarHostState,
            allRuns = allRuns
        )
    }
}

@Composable
fun QuickTaskDialog(
    onDismiss: () -> Unit,
    onSave: (sampleCode: String, sampleName: String, assignedEmployeeId: Int, dateCreated: String, description: String, estimatedTimeStr: String) -> Unit,
    employees: List<Employee>
) {
    var sampleCode by remember { mutableStateOf("") }
    var sampleName by remember { mutableStateOf("") }
    var selectedEmp by remember { mutableStateOf<Employee?>(null) }
    var description by remember { mutableStateOf("") }
    var estHours by remember { mutableStateOf("1") }
    var estMinutes by remember { mutableStateOf("30") }
    val todayDateStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(java.util.Date())
    }
    var dateCreated by remember { mutableStateOf(todayDateStr) }
    var expandedEmpMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Đóng",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "QUYẾT ĐỊNH R&D",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Giao Việc Nhanh Cho Nhân Viên",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("HỦY BỎ", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    if (sampleCode.isNotBlank() && sampleName.isNotBlank() && selectedEmp != null) {
                                        val finalEstTime = "${estHours.trim()} giờ ${estMinutes.trim()} phút"
                                        onSave(sampleCode.trim(), sampleName.trim(), selectedEmp!!.id, dateCreated.trim(), description.trim(), finalEstTime)
                                    }
                                },
                                enabled = sampleCode.isNotBlank() && sampleName.isNotBlank() && selectedEmp != null,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("LƯU & PHÂN CÔNG", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Segment 1: R&D Code & Identification
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.List, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("THÔNG TIN DIỆN MẪU R&D", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Mã mẫu tổng (Mã R&D độc nhất):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(
                                    value = sampleCode,
                                    onValueChange = { sampleCode = it },
                                    placeholder = { Text("E.g., TuongOt-Premium-S2") },
                                    modifier = Modifier.fillMaxWidth().testTag("q_sample_code_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Tên mẫu thử nghiệm & cảm quan dự kiến:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(
                                    value = sampleName,
                                    onValueChange = { sampleName = it },
                                    placeholder = { Text("E.g., Sốt tương ớt mường khương vị tỏi cay thơm") },
                                    modifier = Modifier.fillMaxWidth().testTag("q_sample_name_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Segment 2: Assignee selection & Date
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.People, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Text("PHÂN BỔ TRÁCH NHIỆM", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Nhân viên chịu trách nhiệm thực hành nấu:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { expandedEmpMenu = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = selectedEmp?.name ?: "Bấm chọn nhân sự chịu trách nhiệm...",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (selectedEmp != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = expandedEmpMenu,
                                        onDismissRequest = { expandedEmpMenu = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        employees.forEach { emp ->
                                            DropdownMenuItem(
                                                text = { Text(emp.name, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                                onClick = {
                                                    selectedEmp = emp
                                                    expandedEmpMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Ngày giao việc:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(
                                    value = dateCreated,
                                    onValueChange = { dateCreated = it },
                                    placeholder = { Text("YYYY-MM-DD") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Segment 3: Instruction & Time limit guidelines
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                Text("HƯỚNG DẪN KỸ THUẬT & MỤC TIÊU", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Chỉ thị & công thức giới hạn kỹ thuật (nếu có):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    placeholder = { Text("E.g., pH < 4.4, khuấy liên tục ở mức lò nhiệt thấp để tránh bị cháy khét...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 5,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Số Giờ & Phút Dự Kiến đạt cho mẫu:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = estHours,
                                        onValueChange = { estHours = it },
                                        placeholder = { Text("Giờ") },
                                        modifier = Modifier.weight(1f).testTag("q_est_hours_input"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        label = { Text("Số giờ") }
                                    )
                                    OutlinedTextField(
                                        value = estMinutes,
                                        onValueChange = { estMinutes = it },
                                        placeholder = { Text("Phút") },
                                        modifier = Modifier.weight(1f).testTag("q_est_minutes_input"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        label = { Text("Số phút") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SampleCookingProcessDialog(
    sample: RDSample,
    viewModel: RDViewModel,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState,
    allRuns: List<RDRun>
) {
    val sampleRuns = remember(allRuns, sample.sampleCode) {
        allRuns.filter { it.sampleCode == sample.sampleCode }.sortedBy { it.runNumber }
    }

    var showAddRunForm by remember { mutableStateOf(false) }
    var editingRunDialog by remember { mutableStateOf<RDRun?>(null) }
    var deletingRunDialog by remember { mutableStateOf<RDRun?>(null) }
    var viewingRunDetailDialog by remember { mutableStateOf<RDRun?>(null) }

    // State for a brand new run inside this sample detail dialog (Start/End times)
    var startHour by remember { mutableStateOf("08") }
    var startMinute by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("09") }
    var endMinute by remember { mutableStateOf("30") }
    var runStatus by remember { mutableStateOf("Thành công") }
    var runFailureReason by remember { mutableStateOf("") }
    val todayRunDateStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(java.util.Date())
    }
    var runDateStr by remember { mutableStateOf(todayRunDateStr) }

    val predefinedFailureReasons = listOf(
        "Tách lớp chiết rót, nhũ tương không đồng đều",
        "Chua quá / pH thấp hơn chuẩn rủi ro lên men",
        "Cháy dính đáy lò, khói bén mùi",
        "Biến sẫm màu cảm quan, hỏng mẫu",
        "Quá loãng, không tạo độ sánh sệt cần thiết"
    )

    val computedDurationMin = remember(startHour, startMinute, endHour, endMinute) {
        val sH = startHour.toIntOrNull() ?: 8
        val sM = startMinute.toIntOrNull() ?: 0
        val eH = endHour.toIntOrNull() ?: 9
        val eM = endMinute.toIntOrNull() ?: 30
        var diff = (eH * 60 + eM) - (sH * 60 + sM)
        if (diff <= 0) diff += 1440 // loop-around
        diff
    }

    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Đóng",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CHI TIẾT TIẾN ĐỘ THỬ NGHIỆM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "${sample.sampleCode} - ${sample.sampleName}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Text("HOÀN TẤT XEM", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Segment 1: Specifications & Manager Instruction
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("YÊU CẦU & KỸ THUẬT TIÊU CHUẨN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (sample.status == "Hoàn thành") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = sample.status.uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sample.status == "Hoàn thành") Color(0xFF2E7D32) else Color(0xFFE65100)
                                    )
                                }
                            }

                            Text(
                                text = if (sample.description.isNotEmpty()) sample.description else "Không có ghi chú kỹ thuật biệt lập từ cấp quản lý.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            if (sample.estimatedTimeStr.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Tổng thời gian mẻ mẫu dự kiến: ${sample.estimatedTimeStr}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ngày giao chỉ thị: ${sample.dateCreated}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }

                    // Segment 2: Add New Lab Cooking Run Form
                    if (!showAddRunForm) {
                        Button(
                            onClick = { showAddRunForm = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GHI THÊM MẺ SẢN XUẤT THỬ NGHIỆM MỚI", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    AnimatedVisibility(visible = showAddRunForm) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                                        Text("KẾT QUẢ THỰC NGHIỆM LAB", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    }
                                    
                                    IconButton(
                                        onClick = { showAddRunForm = false },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                    }
                                }

                                // Start / End Time Picker 
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Thời gian bắt đầu & kết thúc:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = startHour,
                                            onValueChange = { startHour = it },
                                            label = { Text("Bắt đầu giờ", fontSize = 8.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        OutlinedTextField(
                                            value = startMinute,
                                            onValueChange = { startMinute = it },
                                            label = { Text("Bắt đầu phút", fontSize = 8.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        OutlinedTextField(
                                            value = endHour,
                                            onValueChange = { endHour = it },
                                            label = { Text("Kết thúc giờ", fontSize = 8.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        OutlinedTextField(
                                            value = endMinute,
                                            onValueChange = { endMinute = it },
                                            label = { Text("Kết thúc phút", fontSize = 8.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                // Auto calculated duration feedback
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Timer, "Tự động tính toán", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Thời gian nấu thực tế tự động tính: $computedDurationMin phút",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }

                                // Status: Success or Failure Toggle
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Đánh giá sản phẩm:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (runStatus == "Thành công") Color(0xFF10B981) else Color.LightGray.copy(alpha = 0.3f))
                                                .clickable { runStatus = "Thành công" }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("THÀNH CÔNG", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (runStatus == "Thành công") Color.White else Color.Black)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (runStatus == "Thất bại") Color(0xFFEF4444) else Color.LightGray.copy(alpha = 0.3f))
                                                .clickable { runStatus = "Thất bại"; runFailureReason = predefinedFailureReasons.first() }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("THẤT BẠI", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (runStatus == "Thất bại") Color.White else Color.Black)
                                        }
                                    }
                                }

                                // Failure Options
                                if (runStatus == "Thất bại") {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Chọn lý do thất bại có sẵn:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            predefinedFailureReasons.forEach { reasonOp ->
                                                val isSelected = runFailureReason == reasonOp
                                                Card(
                                                    modifier = Modifier.fillMaxWidth().clickable { runFailureReason = reasonOp },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isSelected) Color(0xFFFFEBEE) else Color.White
                                                    ),
                                                    border = BorderStroke(if (isSelected) 1.5.dp else 0.8.dp, if (isSelected) Color(0xFFEF4444) else Color.LightGray)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                            contentDescription = null,
                                                            tint = if (isSelected) Color(0xFFEF4444) else Color.Gray,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(reasonOp, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Color(0xFF991B1B) else Color.DarkGray)
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Hoặc nhập lý do khác chi tiết phía dưới:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        OutlinedTextField(
                                            value = runFailureReason,
                                            onValueChange = { runFailureReason = it },
                                            placeholder = { Text("E.g., Nhập mô tả lỗi kĩ thuật khác...") },
                                            modifier = Modifier.fillMaxWidth().testTag("q_failure_reason_input"),
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }

                                // Practice Date
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Ngày nấu thực nghiệm:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OutlinedTextField(
                                        value = runDateStr,
                                        onValueChange = { runDateStr = it },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val durationMin = computedDurationMin
                                        val nextNumber = (sampleRuns.maxOfOrNull { it.runNumber } ?: 0) + 1

                                        val formattedStart = String.format("%02d:%02d", startHour.toIntOrNull() ?: 8, startMinute.toIntOrNull() ?: 0)
                                        val formattedEnd = String.format("%02d:%02d", endHour.toIntOrNull() ?: 9, endMinute.toIntOrNull() ?: 30)

                                        viewModel.addCookingRun(
                                            empId = sample.assignedEmployeeId,
                                            sampleCode = sample.sampleCode,
                                            runNumber = nextNumber,
                                            durationMinutes = durationMin,
                                            status = runStatus,
                                            failureReason = runFailureReason.trim(),
                                            date = runDateStr,
                                            startTimeStr = formattedStart,
                                            endTimeStr = formattedEnd
                                        )

                                        scope.launch {
                                            snackbarHostState.showSnackbar("Đã lưu mẻ nấu thử nghiệm thứ #$nextNumber thành công!")
                                        }

                                        // Reset states
                                        startHour = "08"
                                        startMinute = "00"
                                        endHour = "09"
                                        endMinute = "30"
                                        runStatus = "Thành công"
                                        runFailureReason = ""
                                        showAddRunForm = false
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("GHI NHẬN KẾT QUẢ", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Segment 3: Historic Run Logs (Lịch sử mẻ nấu)
                    Text(
                        text = "NHẬT KÝ THỬ NGHIỆM THỰC TẾ (${sampleRuns.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    if (sampleRuns.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Timer, null, tint = Color.LightGray, modifier = Modifier.size(36.dp))
                                    Text("Chưa có mẻ nấu thử nghiệm nào được ghi nhận cho mẫu này.", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sampleRuns.forEach { run ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewingRunDetailDialog = run },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(
                                                            if (run.status == "Thành công") Color(0xFF10B981) else Color(0xFFEF4444),
                                                            CircleShape
                                                        )
                                                )
                                                Text(
                                                    text = "MẺ THỬ NGHIỆM LẦN #${run.runNumber}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (run.status == "Thành công") Color(0xFF10B981).copy(alpha = 0.12f)
                                                        else Color(0xFFEF4444).copy(alpha = 0.12f)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = run.status.uppercase(),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (run.status == "Thành công") Color(0xFF2E7D32) else Color(0xFFC62828)
                                                )
                                            }
                                        }

                                        // Start & End times and Duration display
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🟢 Bắt đầu: ${run.startTimeStr}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "🛑 Kết thúc: ${run.endTimeStr}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "⏰ Nấu: ${run.durationMs / (1000 * 60)} phút",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "📅 Ngày: ${run.dateString}",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { editingRunDialog = run },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Sửa",
                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { deletingRunDialog = run },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Xóa",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (run.status == "Thất bại" && run.failureReason.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFFFEBEE), RoundedCornerShape(6.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = "⚠ Sự cố: ${run.failureReason}",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFC62828)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingRunDialog != null) {
        val run = editingRunDialog!!
        var runNum by remember(run) { mutableStateOf(run.runNumber.toString()) }
        val minutesSpent = run.durationMs / (1000 * 60)
        val secondsSpent = (run.durationMs % (1000 * 60)) / 1000
        var minStr by remember(run) { mutableStateOf(minutesSpent.toString()) }
        var secStr by remember(run) { mutableStateOf(secondsSpent.toString()) }
        var editStatus by remember(run) { mutableStateOf(run.status) }
        var fReason by remember(run) { mutableStateOf(run.failureReason) }

        AlertDialog(
            onDismissRequest = { editingRunDialog = null },
            title = {
                Text(
                    text = "CHỈNH SỬA MẺ THỬ NGHIỆM LẦN #${run.runNumber}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = runNum,
                        onValueChange = { runNum = it },
                        label = { Text("Số thứ tự mẻ thử") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minStr,
                            onValueChange = { minStr = it },
                            label = { Text("Phút nấu") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = secStr,
                            onValueChange = { secStr = it },
                            label = { Text("Giây nấu") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kết quả:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = editStatus == "Thành công", onClick = { editStatus = "Thành công" })
                            Text("Thành công", fontSize = 11.sp)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = editStatus == "Thất bại", onClick = { editStatus = "Thất bại" })
                            Text("Thất bại", fontSize = 11.sp)
                        }
                    }

                    if (editStatus == "Thất bại") {
                        OutlinedTextField(
                            value = fReason,
                            onValueChange = { fReason = it },
                            label = { Text("Lý do thất bại") },
                            placeholder = { Text("Chi tiết nguyên nhân...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val orderNum = runNum.toIntOrNull() ?: run.runNumber
                        val m = minStr.toLongOrNull() ?: 0L
                        val s = secStr.toLongOrNull() ?: 0L
                        val totalMs = (m * 60 + s) * 1000

                        val updated = run.copy(
                            runNumber = orderNum,
                            durationMs = totalMs,
                            status = editStatus,
                            failureReason = if (editStatus == "Thành công") "" else fReason
                        )
                        viewModel.updateCookingRun(updated)
                        scope.launch {
                            snackbarHostState.showSnackbar("Đã cập nhật thông tin mẻ nấu thử thành công!")
                        }
                        editingRunDialog = null
                    }
                ) {
                    Text("Cập nhật", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRunDialog = null }) {
                    Text("Đóng", fontSize = 11.sp)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (deletingRunDialog != null) {
        val run = deletingRunDialog!!
        AlertDialog(
            onDismissRequest = { deletingRunDialog = null },
            title = {
                Text(
                    text = "XÓA MẺ THỬ NGHIỆM #${run.runNumber}?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text("Bạn có thực sự muốn xóa mẻ thử nghiệm này không? Hành động này không thể hoàn tác.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCookingRun(run)
                        scope.launch {
                            snackbarHostState.showSnackbar("Đã xóa bỏ mẻ nấu thành công!")
                        }
                        deletingRunDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa bỏ", fontSize = 11.sp, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingRunDialog = null }) {
                    Text("Hủy", fontSize = 11.sp)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (viewingRunDetailDialog != null) {
        val run = viewingRunDetailDialog!!
        AlertDialog(
            onDismissRequest = { viewingRunDetailDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (run.status == "Thành công") Color(0xFF10B981) else Color(0xFFEF4444), CircleShape)
                    )
                    Text(
                        text = "CHI TIẾT MẺ THỬ NGHIỆM #${run.runNumber}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kết quả mẻ:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Text(
                            text = run.status.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (run.status == "Thành công") Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Mã mẫu chỉ tiêu:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Text(text = run.sampleCode, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Thời điểm thực hiện:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Text(text = run.dateString, fontSize = 11.sp, fontWeight = FontWeight.Normal)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    // Times
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Giờ bắt đầu đun nấu:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Text(text = run.startTimeStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Giờ kết thúc đun nấu:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Text(text = run.endTimeStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tổng thời gian nấu:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Text(text = "${run.durationMs / (1000 * 60)} phút", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }

                    if (run.status == "Thất bại" && run.failureReason.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text("Lý do thất bại / Sự cố kỹ thuật:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
                                .border(0.5.dp, Color(0xFFFCA5A5), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = run.failureReason,
                                fontSize = 11.sp,
                                color = Color(0xFF991B1B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewingRunDetailDialog = null }) {
                    Text("Đóng", fontSize = 11.sp)
                }
            },
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
fun ConfigurationTabScreen(
    viewModel: RDViewModel,
    employeeReports: List<EmployeeReportSummary>,
    selectedEmployeeDetail: EmployeeReportSummary?,
    onSelectEmployee: (EmployeeReportSummary?) -> Unit,
    onAddEmployeeClick: () -> Unit,
    onDeleteEmployee: (Employee) -> Unit,
    onEditEmployee: (Employee) -> Unit,
    onViewSampleDetails: ((RDSample) -> Unit)? = null,
    onEditRun: ((RDRun) -> Unit)? = null,
    onDeleteRun: ((RDRun) -> Unit)? = null
) {
    var configSubTab by remember { mutableStateOf(0) } // 0: Nhân sự, 1: Tùy chỉnh hệ thống

    if (configSubTab == 0 && selectedEmployeeDetail != null) {
        // Render detail pane full screen if employee is selected
        EmployeesTabScreen(
            employeeReports = employeeReports,
            selectedEmployeeDetail = selectedEmployeeDetail,
            onSelectEmployee = onSelectEmployee,
            onAddEmployeeClick = onAddEmployeeClick,
            onDeleteEmployee = onDeleteEmployee,
            onEditEmployee = onEditEmployee,
            viewModel = viewModel,
            onViewSampleDetails = onViewSampleDetails,
            onEditRun = onEditRun,
            onDeleteRun = onDeleteRun,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sub-tab switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tab 0: Nhân sự
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (configSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { configSubTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = if (configSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "NHÂN SỰ R&D",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (configSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tab 1: Cấu hình hệ thống
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (configSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { configSubTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (configSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "CẤU HÌNH TÙY CHỈNH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (configSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (configSubTab == 0) {
                EmployeesTabScreen(
                    employeeReports = employeeReports,
                    selectedEmployeeDetail = selectedEmployeeDetail,
                    onSelectEmployee = onSelectEmployee,
                    onAddEmployeeClick = onAddEmployeeClick,
                    onDeleteEmployee = onDeleteEmployee,
                    onEditEmployee = onEditEmployee,
                    viewModel = viewModel,
                    onViewSampleDetails = onViewSampleDetails,
                    onEditRun = onEditRun,
                    onDeleteRun = onDeleteRun,
                    modifier = Modifier
                )
            } else {
                // System Configuration content!
                SystemSettingsSection(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SystemSettingsSection(viewModel: RDViewModel) {
    val targetKpi by viewModel.targetKpiSuccessRate.collectAsStateWithLifecycle()
    val targetCookingDuration by viewModel.targetCookingDurationMin.collectAsStateWithLifecycle()
    val autoSyncInterval by viewModel.autoSyncIntervalMin.collectAsStateWithLifecycle()
    val selectedThemeColorHex by viewModel.selectedThemeColorHex.collectAsStateWithLifecycle()

    val githubOwner by viewModel.githubOwner.collectAsStateWithLifecycle()
    val githubRepo by viewModel.githubRepo.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    var editOwner by remember(githubOwner) { mutableStateOf(githubOwner) }
    var editRepo by remember(githubRepo) { mutableStateOf(githubRepo) }

    val scope = rememberCoroutineScope()
    var showResetConfirmation by remember { mutableStateOf(false) }
    
    var showRestoreInputDialog by remember { mutableStateOf(false) }
    var restoreCodeInputByManager by remember { mutableStateOf("") }
    var showBackupResultDialog by remember { mutableStateOf(false) }
    var backupResultCode by remember { mutableStateOf("") }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val currentAppVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: R&D Target Metric Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Analytics, "KPI", tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "CHỈ TIÊU HIỆU SUẤT KPI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = "Điều chỉnh các ngưỡng đánh giá hiệu suất trực tiếp cho đội ngũ nhân sự nấu mẫu R&D.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // KPI target slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tỉ lệ mẻ nấu đạt chuẩn tối thiểu:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$targetKpi%", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = targetKpi.toFloat(),
                        onValueChange = { viewModel.updateTargetKpi(it.toInt()) },
                        valueRange = 50f..100f,
                        steps = 9
                    )
                }

                // Standard cooking duration target plus minus buttons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Thời lượng một mẻ nấu kỳ vọng (phút):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minus Button
                        OutlinedButton(
                            onClick = { if (targetCookingDuration > 15) viewModel.updateTargetCookingDuration(targetCookingDuration - 5) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$targetCookingDuration phút",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Plus Button
                        OutlinedButton(
                            onClick = { if (targetCookingDuration < 120) viewModel.updateTargetCookingDuration(targetCookingDuration + 5) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: System Operation Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudSync, "Sync", tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "VẬN HÀNH & ĐỒNG BỘ CLOUD",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Thay đổi cấu hình sao lưu dữ liệu tự động lên máy chủ R&D đám mây.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Custom Sync Interval Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Chu kỳ tự động đồng bộ Cloud:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 15, 30, 60).forEach { mins ->
                            val isSelected = autoSyncInterval == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateAutoSyncInterval(mins) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$mins phút",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Backup & Restore
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Backup, "Backup", tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "SAO LƯU & PHỤC HỒI DỮ LIỆU R&D",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Sao lưu toàn bộ danh sách nhân sự R&D, danh sách mẫu thử và lịch sử mẻ nấu chi tiết thành định dạng văn bản JSON để chia sẻ, cất giữ hoặc di dời máy chủ.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export code
                    Button(
                        onClick = {
                            val backupStr = viewModel.getBackupString()
                            if (backupStr.isNotEmpty()) {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(backupStr))
                                backupResultCode = backupStr
                                showBackupResultDialog = true
                                android.widget.Toast.makeText(context, "Đã sao chép mã sao lưu vào bộ nhớ tạm!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Lỗi tạo bản sao lưu!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SAO LƯU (COPY)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Import code
                    OutlinedButton(
                        onClick = {
                            restoreCodeInputByManager = ""
                            showRestoreInputDialog = true
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("KHÔI PHỤC (PASTE)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Section: System Reset / Maintenance
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, "Reset", tint = MaterialTheme.colorScheme.error)
                    Text(
                        text = "VÙNG NGUY HIỂM & BẢO TRÌ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "Khôi phục toàn diện dữ liệu thử nghiệm mẫu về thiết lập R&D ban đầu. Hành động này sẽ ghi đè mọi thay đổi hiện có.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Button(
                    onClick = { showResetConfirmation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, "Reset", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PHỤC HỒI DỮ LIỆU BAN ĐẦU", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: Github Auto-Update & Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, "Update", tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "CẬP NHẬT ỨNG DỤNG TỰ ĐỘNG (GITHUB)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Hệ thống tự động liên kết với GitHub để kiểm tra phiên bản mới, tải xuống tệp APK và tiến hành cài đặt nhanh chóng.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Inputs for owner and repo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editOwner,
                        onValueChange = { editOwner = it },
                        label = { Text("Chủ sở hữu GitHub", fontSize = 10.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    OutlinedTextField(
                        value = editRepo,
                        onValueChange = { editRepo = it },
                        label = { Text("Kho lưu trữ (Repo)", fontSize = 10.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Button to save config
                    OutlinedButton(
                        onClick = {
                            viewModel.updateGithubConfig(editOwner, editRepo)
                            android.widget.Toast.makeText(context, "Đã lưu cấu hình cập nhật GitHub!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lưu cấu hình", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Button to check updates
                    Button(
                        onClick = {
                            viewModel.checkForUpdates()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kiểm tra cập nhật", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                suspend fun triggerInstall(file: java.io.File) {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            setDataAndType(uri, "application/vnd.android.package-archive")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Lỗi cài đặt: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }

                // Update state ui rendering
                when (val state = updateState) {
                    is RDViewModel.UpdateState.Checking -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Đang kết nối hệ thống và kiểm tra máy chủ GitHub...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is RDViewModel.UpdateState.UpToDate -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFDEF7EC), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF03543F), modifier = Modifier.size(16.dp))
                            Text("Bạn đang dùng bản mới nhất (v$currentAppVersion)!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF03543F))
                        }
                    }
                    is RDViewModel.UpdateState.NewVersionAvailable -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Celebration, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Text("Phiên bản mới: ${state.latestVersion}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Text("Thông tin:\n${state.changelog}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            Button(
                                onClick = { viewModel.downloadAndInstallLatestRelease(state.apkUrl) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tải xuống & Nâng cấp ngay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    is RDViewModel.UpdateState.Downloading -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Đang tải tệp APK...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${(state.progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            LinearProgressIndicator(
                                progress = state.progress,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    is RDViewModel.UpdateState.DownloadSuccess -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE1F5FE), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text("Đã tải xong bản cập nhật mới!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                            Button(
                                onClick = { 
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                state.apkFile
                                            )
                                            setDataAndType(uri, "application/vnd.android.package-archive")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Không thể mở cài đặt: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.InstallMobile, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tiến hành cài đặt", fontSize = 11.sp)
                            }
                        }
                    }
                    is RDViewModel.UpdateState.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Text(state.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    else -> {}
                }
            }
        }

        // Section: About App & Developers Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, "Developer", tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "VỀ ỨNG DỤNG & NHÀ PHÁT TRIỂN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    text = "Thông tin chi tiết về hệ thống Quản lý Tiến độ Nấu Mẫu R&D và nhóm tác giả chịu trách nhiệm phát triển sản phẩm.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Detail specs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tên ứng dụng:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Quản lý Tiến độ Nấu Mẫu R&D", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Phiên bản hiện tại:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("$currentAppVersion (Stable build)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cơ sở dữ liệu:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("SQLite (Room Database)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Nhà phát triển (Developer):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("vankhoai690@gmail.com", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Bản quyền hệ thống thuộc Phòng R&D và nhóm Kỹ sư Công nghệ Thực phẩm. Ứng dụng hỗ trợ ghi chép dữ liệu thực nghiệm mẫu và xuất báo cáo tự động chuẩn xác.",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("MÁY CHỦ SẼ BỊ KHÔI PHỤC", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ mẻ nấu thêm mới và khôi phục về danh sách 8 nhân sự & 50 mẻ nấu R&D mặc định?", fontSize = 11.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDatabaseToDefaults()
                        showResetConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Đồng ý", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Hủy", fontSize = 11.sp)
                }
            }
        )
    }

    if (showBackupResultDialog) {
        AlertDialog(
            onDismissRequest = { showBackupResultDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, "Success", tint = MaterialTheme.colorScheme.primary)
                    Text("SAO LƯU DỰ LIỆU THÀNH CÔNG", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Đã tạo mã sao lưu thành dữ liệu văn bản JSON mã hóa dưới đây và sao chép vào bộ nhớ tạm (clipboard). Bạn có thể chia sẻ hoặc lưu mã này lại:", fontSize = 11.sp)
                    OutlinedTextField(
                        value = backupResultCode,
                        onValueChange = {},
                        readOnly = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(backupResultCode))
                        android.widget.Toast.makeText(context, "Đã sao chép lại!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Sao chép lại", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupResultDialog = false }) {
                    Text("Đóng", fontSize = 11.sp)
                }
            }
        )
    }

    if (showRestoreInputDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreInputDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Restore, "Restore", tint = MaterialTheme.colorScheme.primary)
                    Text("PHỤC HỒI DỮ LIỆU TỪ MÃ SAO LƯU", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dán chuỗi ký tự JSON sao lưu của bạn vào ô dưới đây. Quá trình này sẽ ghi đè và thay thế toàn bộ dữ liệu hiện tại bằng dữ liệu trong tệp sau lưu.", fontSize = 11.sp)
                    OutlinedTextField(
                        value = restoreCodeInputByManager,
                        onValueChange = { restoreCodeInputByManager = it },
                        placeholder = { Text("Dán văn bản JSON tại đây...", fontSize = 11.sp, color = Color.Gray) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreCodeInputByManager.trim().isEmpty()) {
                            android.widget.Toast.makeText(context, "Vui lòng dán mã sao lưu trước khi xác nhận!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.restoreFromBackupString(restoreCodeInputByManager) { status, msg ->
                                if (status) {
                                    android.widget.Toast.makeText(context, "Khôi phục dữ liệu thành công!", android.widget.Toast.LENGTH_LONG).show()
                                    showRestoreInputDialog = false
                                } else {
                                    android.widget.Toast.makeText(context, "Lỗi: $msg", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Xác nhận phục hồi", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreInputDialog = false }) {
                    Text("Hủy bỏ", fontSize = 11.sp)
                }
            }
        )
    }
}

