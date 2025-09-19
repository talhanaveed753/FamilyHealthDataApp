package com.example.healthconnect_tablet.data.repository

import com.example.healthconnect_tablet.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository class for handling Firebase Firestore operations
 * Manages users, families, and health data persistence
 */
class FirebaseRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val FAMILIES_COLLECTION = "families"
        private const val HEALTH_DATA_COLLECTION = "health_data"
        private const val USER_PROFILES_COLLECTION = "userProfiles"
        private const val DATE_FORMAT = "yyyy-MM-dd"
    }
    
    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    
    /**
     * Get current user ID from Firebase Auth
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    /**
     * Ensure user is authenticated, sign in anonymously if not
     */
    private suspend fun ensureAuthenticated() {
        if (auth.currentUser == null) {
            try {
                auth.signInAnonymously().await()
                android.util.Log.d("FirebaseRepository", "Signed in anonymously")
            } catch (e: Exception) {
                android.util.Log.e("FirebaseRepository", "Failed to sign in anonymously: ${e.message}")
                throw e
            }
        }
    }
    
    /**
     * Create or update user profile
     */
    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(user.id)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user by ID
     */
    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get family by ID
     */
    suspend fun getFamily(familyId: String): Result<Family?> {
        return try {
            val document = firestore.collection(FAMILIES_COLLECTION)
                .document(familyId)
                .get()
                .await()
            val family = document.toObject(Family::class.java)
            Result.success(family)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all family members
     */
    suspend fun getFamilyMembers(familyId: String): Result<List<User>> {
        return try {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("familyId", familyId)
                .get()
                .await()
            val users = querySnapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Save health data for a user and date
     */
    suspend fun saveHealthData(healthData: HealthData): Result<Unit> {
        return try {
            val documentId = "${healthData.userId}_${healthData.date}"
            firestore.collection(HEALTH_DATA_COLLECTION)
                .document(documentId)
                .set(healthData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get health data for a user and specific date
     */
    suspend fun getHealthData(userId: String, date: String): Result<HealthData?> {
        return try {
            val documentId = "${userId}_${date}"
            val document = firestore.collection(HEALTH_DATA_COLLECTION)
                .document(documentId)
                .get()
                .await()
            val healthData = document.toObject(HealthData::class.java)
            Result.success(healthData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get health data for a user within a date range
     */
    suspend fun getHealthDataRange(
        userId: String, 
        startDate: String, 
        endDate: String
    ): Result<List<HealthData>> {
        return try {
            val querySnapshot = firestore.collection(HEALTH_DATA_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()
            val healthDataList = querySnapshot.toObjects(HealthData::class.java)
            Result.success(healthDataList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get real-time family dashboard data
     */
    fun getFamilyDashboardData(familyId: String): Flow<FamilyDashboardData> = callbackFlow {
        val familyListener = firestore.collection(FAMILIES_COLLECTION)
            .document(familyId)
            .addSnapshotListener { familySnapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val family = familySnapshot?.toObject(Family::class.java)
                if (family != null) {
                    // Listen to family members
                    val membersListener = firestore.collection(USERS_COLLECTION)
                        .whereEqualTo("familyId", familyId)
                        .addSnapshotListener { membersSnapshot, membersError ->
                            if (membersError != null) {
                                close(membersError)
                                return@addSnapshotListener
                            }
                            
                            val members = membersSnapshot?.toObjects(User::class.java) ?: emptyList()
                            
                            // Get today's date
                            val today = dateFormatter.format(Date())
                            
                            // For simplicity, we'll send the current data
                            val familyData = FamilyDashboardData(
                                family = family,
                                membersHealthData = members.map { user ->
                                    UserHealthSummary(
                                        user = user,
                                        todayData = null // Will be populated by ViewModel
                                    )
                                }
                            )
                            
                            trySend(familyData)
                        }
                }
            }
        
        awaitClose { 
            // Clean up listeners when flow is cancelled
        }
    }
    
    /**
     * Create a new family
     */
    suspend fun createFamily(family: Family): Result<String> {
        return try {
            val documentRef = firestore.collection(FAMILIES_COLLECTION).document()
            val familyWithId = family.copy(id = documentRef.id)
            documentRef.set(familyWithId).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Join family by invite code
     */
    suspend fun joinFamilyByInviteCode(inviteCode: String, userId: String): Result<String> {
        return try {
            val querySnapshot = firestore.collection(FAMILIES_COLLECTION)
                .whereEqualTo("inviteCode", inviteCode)
                .get()
                .await()
            
            if (querySnapshot.documents.isEmpty()) {
                return Result.failure(Exception("Invalid invite code"))
            }
            
            val family = querySnapshot.documents.first().toObject(Family::class.java)
                ?: return Result.failure(Exception("Family not found"))
            
            // Update user's familyId
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("familyId", family.id)
                .await()
            
            // Add user to family members list
            val updatedMemberIds = family.memberIds + userId
            firestore.collection(FAMILIES_COLLECTION)
                .document(family.id)
                .update("memberIds", updatedMemberIds)
                .await()
            
            Result.success(family.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Fetch all users (treat as family members)
     */
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .get()
                .await()
            val users = querySnapshot.documents.mapNotNull { doc ->
                try {
                    // Try to parse as a full User object first
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    // Fallback: create minimal user with just name if document structure doesn't match
                    val name = doc.getString("name") ?: "Unnamed"
                    User(
                        id = doc.id,
                        name = name
                    )
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all users from a specific family collection
     */
    suspend fun getAllUsersFromFamily(familyName: String): Result<List<User>> {
        return try {
            val querySnapshot = firestore.collection(familyName)
                .get()
                .await()
            val users = querySnapshot.documents.mapNotNull { doc ->
                try {
                    // Try to parse as a full User object first
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    // Fallback: create minimal user with just name if document structure doesn't match
                    val name = doc.getString("name") ?: "Unnamed"
                    User(
                        id = doc.id,
                        name = name
                    )
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Fetch the latest daily log for a user and extract health data
     */
    suspend fun getLatestDailyLog(userId: String): Result<HealthData?> {
        return try {
            val dailyLogsRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("dailyLogs")
            val querySnapshot = dailyLogsRef
                .orderBy("__name__", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            if (querySnapshot.documents.isEmpty()) {
                return Result.success(null)
            }
            val doc = querySnapshot.documents.first()
            val date = doc.id
            val exerciseLogs = doc.get("exerciseLogs") as? Map<*, *>
            val heartRateLogs = doc.get("heartRateLogs") as? Map<*, *>
            val sleepLogs = doc.get("sleepLogs") as? Map<*, *>
            val steps = (exerciseLogs?.get("totalSteps") as? Long)?.toInt() ?: 0
            val heartRate = (heartRateLogs?.get("avgHeartRate") as? Number)?.toInt() ?: 0
            val sleepMinutes = (sleepLogs?.get("totalSleepMinutes") as? Long)?.toInt() ?: 0
            val sleepHours = sleepMinutes / 60f
            val healthData = HealthData(
                userId = userId,
                date = date,
                steps = steps,
                heartRate = heartRate,
                sleepHours = sleepHours
            )
            Result.success(healthData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getTodayDate(): String {
        return dateFormatter.format(Date())
    }

    /**
     * Fetch the latest daily log for a user from a specific family collection
     * Only returns data if it's from today
     */
    suspend fun getLatestDailyLogFromFamily(userId: String, familyName: String): Result<HealthData?> {
        return try {
            val today = getTodayDate()
            val dailyLogsRef = firestore.collection(familyName)
                .document(userId)
                .collection("dailyLogs")
                .document(today)
            
            val doc = dailyLogsRef.get().await()
            
            if (!doc.exists()) {
                return Result.success(null)
            }
            
            val exerciseLogs = doc.get("exerciseLogs") as? Map<*, *>
            val heartRateLogs = doc.get("heartRateLogs") as? Map<*, *>
            val sleepLogs = doc.get("sleepLogs") as? Map<*, *>
            
            val steps = (exerciseLogs?.get("totalSteps") as? Long)?.toInt() ?: 0
            val heartRate = (heartRateLogs?.get("avgHeartRate") as? Number)?.toInt() ?: 0
            val sleepMinutes = (sleepLogs?.get("totalSleepMinutes") as? Long)?.toInt() ?: 0
            val sleepHours = sleepMinutes / 60f
            
            val healthData = HealthData(
                userId = userId,
                date = today,
                steps = steps,
                heartRate = heartRate,
                sleepHours = sleepHours
            )
            Result.success(healthData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user profile data from family collection
     */
    suspend fun getUserProfileFromFamily(userId: String, familyName: String): Result<User?> {
        return try {
            val userDoc = firestore.collection(familyName)
                .document(userId)
                .get()
                .await()
            
            if (!userDoc.exists()) {
                return Result.success(null)
            }
            
            // Extract profile fields from the document
            val name = userDoc.getString("name") ?: "Unknown"
            val age = userDoc.getLong("age")?.toInt() ?: 0
            val gender = userDoc.getString("gender") ?: ""
            val height = userDoc.getDouble("height")?.toFloat() ?: 0f
            val weight = userDoc.getDouble("weight")?.toFloat() ?: 0f
            
            val userProfile = User(
                id = userId,
                name = name,
                age = age,
                height = height,
                weight = weight,
                gender = gender,
                familyId = familyName
            )
            
            Result.success(userProfile)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error fetching user profile: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetch the last 7 days of daily logs for a user from a specific family collection
     */
    suspend fun getLast7DaysFromFamily(userId: String, familyName: String): Result<List<HealthData>> {
        return try {
            android.util.Log.d("FirebaseRepository", "Fetching 7 days data for user: $userId from family: $familyName")
            
            val dailyLogsRef = firestore.collection(familyName)
                .document(userId)
                .collection("dailyLogs")
            val querySnapshot = dailyLogsRef
                .orderBy("__name__", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(7)
                .get()
                .await()
            
            android.util.Log.d("FirebaseRepository", "Found ${querySnapshot.documents.size} documents")
            
            val healthDataList = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val date = doc.id
                    val exerciseLogs = doc.get("exerciseLogs") as? Map<*, *>
                    val heartRateLogs = doc.get("heartRateLogs") as? Map<*, *>
                    val sleepLogs = doc.get("sleepLogs") as? Map<*, *>
                    
                    android.util.Log.d("FirebaseRepository", "Document $date:")
                    android.util.Log.d("FirebaseRepository", "  exerciseLogs: $exerciseLogs")
                    android.util.Log.d("FirebaseRepository", "  heartRateLogs: $heartRateLogs")
                    android.util.Log.d("FirebaseRepository", "  sleepLogs: $sleepLogs")
                    
                    val steps = (exerciseLogs?.get("totalSteps") as? Long)?.toInt() ?: 0
                    val heartRate = (heartRateLogs?.get("avgHeartRate") as? Number)?.toInt() ?: 0
                    val sleepMinutes = (sleepLogs?.get("totalSleepMinutes") as? Long)?.toInt() ?: 0
                    val sleepHours = sleepMinutes / 60f
                    
                    android.util.Log.d("FirebaseRepository", "  Extracted: steps=$steps, heartRate=$heartRate, sleepHours=$sleepHours")
                    
                    HealthData(
                        userId = userId,
                        date = date,
                        steps = steps,
                        heartRate = heartRate,
                        sleepHours = sleepHours
                    )
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseRepository", "Error processing document: ${e.message}")
                    null // Skip malformed documents
                }
            }.reversed() // Reverse to get chronological order (oldest to newest)
            
            android.util.Log.d("FirebaseRepository", "Returning ${healthDataList.size} health data entries")
            
            Result.success(healthDataList)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error fetching 7 days data: ${e.message}")
            Result.failure(e)
        }
    }
} 