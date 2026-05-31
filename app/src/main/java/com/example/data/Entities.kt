package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String = "Nhân viên R&D",
    val avatarColorHex: String = "#4F46E5"
)

@Entity(
    tableName = "rd_samples",
    foreignKeys = [
        ForeignKey(
            entity = Employee::class,
            parentColumns = ["id"],
            childColumns = ["assignedEmployeeId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["assignedEmployeeId"])]
)
data class RDSample(
    @PrimaryKey val sampleCode: String,      // Mã mẫu (e.g., Ketchup-S1, Yoghurt-p1)
    val sampleName: String,                  // Tên mẫu (E.g. Sốt cà chua cay số 1, Sữa đặc)
    val assignedEmployeeId: Int,             // ID nhân viên được giao việc
    val status: String = "Đang thực hiện",   // "Đang thực hiện" hoặc "Hoàn thành"
    val dateCreated: String,                 // Định dạng YYYY-MM-DD
    val description: String = "",            // Ghi chú công thức/yêu cầu từ người quản lý
    val estimatedTimeStr: String = ""        // Tổng thời gian mẫu dự kiến (VD: 120 phút)
)

@Entity(
    tableName = "rd_runs",
    foreignKeys = [
        ForeignKey(
            entity = Employee::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RDSample::class,
            parentColumns = ["sampleCode"],
            childColumns = ["sampleCode"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["employeeId"]),
        Index(value = ["sampleCode"])
    ]
)
data class RDRun(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val sampleCode: String,      // Mã mẫu (e.g., Mẫu Ketchup A1)
    val runNumber: Int,         // Thử nghiệm lần thứ mấy (e.g., 1, 2, 3...)
    val durationMs: Long,        // Thời gian nấu của run này (mili-giây)
    val status: String,          // "Thành công" hoặc "Thất bại"
    val failureReason: String = "", // Lý do thất bại (nếu có)
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String       // Định dạng YYYY-MM-DD để dễ lọc nhanh
)


