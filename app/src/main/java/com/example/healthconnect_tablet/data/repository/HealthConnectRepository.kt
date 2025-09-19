package com.example.healthconnect_tablet.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.healthconnect_tablet.data.model.HealthData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Repository class for handling Health Connect API operations
 * Reads health data from the Health Connect platform
 */
class HealthConnectRepository(
    private val context: Context
) {
    
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        
        val HEALTH_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class)
        )
    }
    
    /**
     * Check if Health Connect is available on this device
     */
    suspend fun isHealthConnectAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    suspend fun hasAllPermissions(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
                HEALTH_PERMISSIONS.all { it in grantedPermissions }
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Get health data for a specific date
     */
    suspend fun getHealthDataForDate(date: LocalDate): Result<HealthData> {
        return withContext(Dispatchers.IO) {
            try {
                val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                val timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                
                // Read steps
                val stepsCount = readStepsData(timeRangeFilter)
                
                // Read heart rate
                val avgHeartRate = readHeartRateData(timeRangeFilter)
                
                // Read sleep
                val sleepHours = readSleepData(timeRangeFilter)
                
                // Read calories
                val caloriesBurned = readCaloriesData(timeRangeFilter)
                
                // Read distance
                val distance = readDistanceData(timeRangeFilter)
                
                val healthData = HealthData(
                    userId = "", // Will be set by the calling code
                    date = date.format(DATE_FORMATTER),
                    steps = stepsCount,
                    heartRate = avgHeartRate,
                    sleepHours = sleepHours,
                    caloriesBurned = caloriesBurned,
                    distance = distance
                )
                
                Result.success(healthData)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Read steps data for the given time range
     */
    private suspend fun readStepsData(timeRangeFilter: TimeRangeFilter): Int {
        return try {
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = timeRangeFilter
            )
            val response = healthConnectClient.readRecords(request)
            response.records.sumOf { it.count.toInt() }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Read heart rate data for the given time range
     */
    private suspend fun readHeartRateData(timeRangeFilter: TimeRangeFilter): Int {
        return try {
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = timeRangeFilter
            )
            val response = healthConnectClient.readRecords(request)
            val heartRates = response.records.flatMap { record ->
                record.samples.map { it.beatsPerMinute }
            }
            if (heartRates.isNotEmpty()) {
                heartRates.average().toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Read sleep data for the given time range
     */
    private suspend fun readSleepData(timeRangeFilter: TimeRangeFilter): Float {
        return try {
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = timeRangeFilter
            )
            val response = healthConnectClient.readRecords(request)
            val totalSleepMinutes = response.records.sumOf { record ->
                val sleepDuration = record.endTime.toEpochMilli() - record.startTime.toEpochMilli()
                sleepDuration / (1000 * 60) // Convert to minutes
            }
            totalSleepMinutes / 60f // Convert to hours
        } catch (e: Exception) {
            0f
        }
    }
    
    /**
     * Read calories burned data for the given time range
     */
    private suspend fun readCaloriesData(timeRangeFilter: TimeRangeFilter): Int {
        return try {
            val request = ReadRecordsRequest(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = timeRangeFilter
            )
            val response = healthConnectClient.readRecords(request)
            response.records.sumOf { it.energy.inKilocalories.toInt() }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Read distance data for the given time range
     */
    private suspend fun readDistanceData(timeRangeFilter: TimeRangeFilter): Float {
        return try {
            val request = ReadRecordsRequest(
                recordType = DistanceRecord::class,
                timeRangeFilter = timeRangeFilter
            )
            val response = healthConnectClient.readRecords(request)
            response.records.sumOf { it.distance.inMeters }.toFloat()
        } catch (e: Exception) {
            0f
        }
    }
    
    /**
     * Get health data for a date range
     */
    suspend fun getHealthDataForDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<HealthData>> {
        return withContext(Dispatchers.IO) {
            try {
                val healthDataList = mutableListOf<HealthData>()
                var currentDate = startDate
                
                while (!currentDate.isAfter(endDate)) {
                    val result = getHealthDataForDate(currentDate)
                    if (result.isSuccess) {
                        result.getOrNull()?.let { healthDataList.add(it) }
                    }
                    currentDate = currentDate.plusDays(1)
                }
                
                Result.success(healthDataList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
} 