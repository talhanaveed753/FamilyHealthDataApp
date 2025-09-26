package com.example.healthconnect_tablet.tokens.model

/**
 * Represents the number of tokens a user can earn for a given day.
 * The values are computed from daily Firestore logs.
 */
data class TokenAllowance(
    val stepsTokens: Int = 0,
    val sleepTokens: Int = 0,
    val heartTokens: Int = 0
) {
    val totalTokens: Int
        get() = stepsTokens + sleepTokens + heartTokens
}

/**
 * Simple holder for token counts used across the dashboard UI.
 */
data class TokenCounts(
    val steps: Int = 0,
    val sleep: Int = 0,
    val heart: Int = 0
) {
    fun remainingFrom(allowance: TokenAllowance): TokenCounts = TokenCounts(
        steps = (allowance.stepsTokens - steps).coerceAtLeast(0),
        sleep = (allowance.sleepTokens - sleep).coerceAtLeast(0),
        heart = (allowance.heartTokens - heart).coerceAtLeast(0)
    )
}
