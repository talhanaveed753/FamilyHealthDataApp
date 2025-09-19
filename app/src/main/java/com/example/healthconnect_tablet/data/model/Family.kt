package com.example.healthconnect_tablet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data model representing a family group
 * @param id Firestore document ID
 * @param name Family name
 * @param adminUserId ID of the family administrator
 * @param memberIds List of user IDs in this family
 * @param inviteCode Unique code for inviting new members
 * @param createdAt Family creation timestamp
 */
data class Family(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val adminUserId: String = "",
    val memberIds: List<String> = emptyList(),
    val inviteCode: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
)

/**
 * Complete family data including all members and their health information
 */
data class FamilyDashboardData(
    val family: Family,
    val membersHealthData: List<UserHealthSummary> = emptyList(),
    val familyStats: FamilyHealthStats = FamilyHealthStats()
)

/**
 * Aggregated family health statistics
 */
data class FamilyHealthStats(
    val totalSteps: Long = 0,
    val averageHeartRate: Float = 0f,
    val totalSleepHours: Float = 0f,
    val totalCalories: Long = 0,
    val totalDistance: Float = 0f,
    val mostActiveToday: String = "",
    val goalAchievers: Int = 0
) 