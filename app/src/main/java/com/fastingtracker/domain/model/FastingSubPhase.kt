package com.fastingtracker.domain.model

/**
 * Represents the detailed sub-phase during fasting or the eating window.
 * Used across domain, service, and notification layers to avoid hardcoded strings.
 */
enum class FastingSubPhase {
    /** Currently in eating window */
    EATING_WINDOW,
    /** Basic fasting state (< 12h) */
    FASTING,
    /** Fat burning phase (12-16h) */
    FAT_BURNING,
    /** Ketosis phase (16-18h) */
    KETOSIS,
    /** Autophagy phase (18-24h) */
    AUTOPHAGY,
    /** Deep ketosis phase (24-48h) */
    DEEP_KETOSIS,
    /** Extended fasting phase (48-72h) */
    EXTENDED_FASTING,
    /** Deep autophagy phase (72h+) */
    DEEP_AUTOPHAGY;

    companion object {
        /**
         * Determine the fasting sub-phase based on elapsed fasting hours.
         * Returns null if less than 12 hours (basic fasting, no sub-phase).
         */
        fun fromElapsedHours(elapsedHours: Long): FastingSubPhase {
            return when {
                elapsedHours < 12 -> FASTING
                elapsedHours < 16 -> FAT_BURNING
                elapsedHours < 18 -> KETOSIS
                elapsedHours < 24 -> AUTOPHAGY
                elapsedHours < 48 -> DEEP_KETOSIS
                elapsedHours < 72 -> EXTENDED_FASTING
                else -> DEEP_AUTOPHAGY
            }
        }
    }
}
