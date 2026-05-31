package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RDDao {
    // Employees
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getEmployeeById(id: Int): Employee?

    // Runs
    @Query("SELECT * FROM rd_runs ORDER BY timestamp DESC")
    fun getAllRuns(): Flow<List<RDRun>>

    @Query("SELECT * FROM rd_runs WHERE employeeId = :employeeId ORDER BY timestamp DESC")
    fun getRunsByEmployee(employeeId: Int): Flow<List<RDRun>>

    @Query("SELECT * FROM rd_runs WHERE dateString = :date ORDER BY timestamp DESC")
    fun getRunsByDate(date: String): Flow<List<RDRun>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RDRun)

    @Delete
    suspend fun deleteRun(run: RDRun)

    @Query("DELETE FROM rd_runs")
    suspend fun clearAllRuns()

    // Samples (Mẫu tổng)
    @Query("SELECT * FROM rd_samples ORDER BY dateCreated DESC")
    fun getAllSamples(): Flow<List<RDSample>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: RDSample)

    @Update
    suspend fun updateSample(sample: RDSample)

    @Delete
    suspend fun deleteSample(sample: RDSample)

    @Query("DELETE FROM rd_samples")
    suspend fun clearAllSamples()
}
