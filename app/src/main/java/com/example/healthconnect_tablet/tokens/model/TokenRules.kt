package com.example.healthconnect_tablet.tokens.model

/**
 * Shared token calculation rules used across the dashboard and repository.
 */
object TokenRules {
    const val STEPS_PER_TOKEN = 1_000
    const val SLEEP_MINUTES_PER_TOKEN = 60

    fun activityTokensForSteps(steps: Int): Int = (steps / STEPS_PER_TOKEN).coerceAtLeast(0)

    fun sleepTokensForMinutes(minutes: Int): Int = (minutes / SLEEP_MINUTES_PER_TOKEN).coerceAtLeast(0)
}
