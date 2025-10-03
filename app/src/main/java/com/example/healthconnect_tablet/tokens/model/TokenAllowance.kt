package com.example.healthconnect_tablet.tokens.model

/**
 * Represents the number of tokens a user can earn for a given day.
 * The values are computed from daily Firestore logs.
 */
data class TokenAllowance(
    val activityTokens: Int = 0,
    val sleepTokens: Int = 0
) {
    val totalTokens: Int
        get() = activityTokens + sleepTokens
}

/**
 * Simple holder for token counts used across the dashboard UI.
 */
data class TokenCounts(
    val activity: Int = 0,
    val sleep: Int = 0
) {
    fun remainingFrom(allowance: TokenAllowance): TokenCounts = TokenCounts(
        activity = (allowance.activityTokens - activity).coerceAtLeast(0),
        sleep = (allowance.sleepTokens - sleep).coerceAtLeast(0)
    )
}
