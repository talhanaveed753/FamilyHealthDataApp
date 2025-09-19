package com.example.healthconnect_tablet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data model representing a family member
 * @param id Firestore document ID
 * @param familyId ID of the family this user belongs to
 * @param name Display name of the user
 * @param email Email address
 * @param profileImageUrl URL to profile image
 * @param age Age of the user
 * @param height Height in centimeters
 * @param weight Weight in kilograms
 * @param createdAt Account creation timestamp
 * @param lastActive Last time the user was active
 */
data class User(
    @DocumentId
    val id: String = "",
    val familyId: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val age: Int = 0,
    val height: Float = 0f, // in cm
    val weight: Float = 0f, // in kg
    val gender: String = "", // Male/Female/Other
    val cardColor: String = "#FFE0E0E0", // Managed locally via CardColorManager (not stored in Firebase)
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val lastActive: Date? = null
) 