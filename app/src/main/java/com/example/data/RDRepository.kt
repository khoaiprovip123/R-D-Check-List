package com.example.data

import kotlinx.coroutines.flow.Flow

class RDRepository(private val rdDao: RDDao) {
    val allEmployees: Flow<List<Employee>> = rdDao.getAllEmployees()
    val allRuns: Flow<List<RDRun>> = rdDao.getAllRuns()

    fun getRunsByEmployee(employeeId: Int): Flow<List<RDRun>> = rdDao.getRunsByEmployee(employeeId)
    fun getRunsByDate(date: String): Flow<List<RDRun>> = rdDao.getRunsByDate(date)

    suspend fun insertEmployee(employee: Employee): Long = rdDao.insertEmployee(employee)
    suspend fun updateEmployee(employee: Employee) = rdDao.updateEmployee(employee)
    suspend fun deleteEmployee(employee: Employee) = rdDao.deleteEmployee(employee)
    suspend fun getEmployeeById(id: Int): Employee? = rdDao.getEmployeeById(id)

    suspend fun insertRun(run: RDRun) = rdDao.insertRun(run)
    suspend fun deleteRun(run: RDRun) = rdDao.deleteRun(run)
    suspend fun clearAllRuns() = rdDao.clearAllRuns()

    // Sample Actions
    val allSamples: Flow<List<RDSample>> = rdDao.getAllSamples()
    suspend fun insertSample(sample: RDSample) = rdDao.insertSample(sample)
    suspend fun updateSample(sample: RDSample) = rdDao.updateSample(sample)
    suspend fun deleteSample(sample: RDSample) = rdDao.deleteSample(sample)
    suspend fun clearAllSamples() = rdDao.clearAllSamples()
}
