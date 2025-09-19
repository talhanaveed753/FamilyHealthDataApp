package com.example.healthconnect_tablet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data model representing health metrics for a specific date
 * @param id Firestore document ID
 * @param userId ID of the user this data belongs to
 * @param date Date of the health data
 * @param steps Number of steps taken
 * @param heartRate Average heart rate (bpm)
 * @param sleepHours Hours of sleep
 * @param caloriesBurned Calories burned
 * @param distance Distance covered in meters
 * @param lastUpdated Last time this data was updated
 */
data class HealthData(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val date: String = "", // Format: yyyy-MM-dd
    val steps: Int = 0,
    val heartRate: Int = 0, // Average BPM
    val sleepHours: Float = 0f,
    val caloriesBurned: Int = 0,
    val distance: Float = 0f, // in meters
    @ServerTimestamp
    val lastUpdated: Date? = null
)

/**
 * Aggregated health data for a user including trend information
 */
data class UserHealthSummary(
    val user: User,
    val todayData: HealthData?,
    val weeklyData: List<HealthData> = emptyList(),
    val monthlyData: List<HealthData> = emptyList(),
    val weeklyAverage: HealthMetrics = HealthMetrics(),
    val monthlyAverage: HealthMetrics = HealthMetrics()
)

/**
 * Simplified health metrics for averages and comparisons
 */
data class HealthMetrics(
    val steps: Float = 0f,
    val heartRate: Float = 0f,
    val sleepHours: Float = 0f,
    val caloriesBurned: Float = 0f,
    val distance: Float = 0f
) 