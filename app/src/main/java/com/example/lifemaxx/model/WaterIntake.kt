package com.example.lifemaxx.model

/**
 * Represents a water intake entry.
 *
 * @param id Unique identifier for the entry
 * @param userId ID of the user this entry belongs to
 * @param date Date of the entry in YYYY-MM-DD format
 * @param amount Amount of water in milliliters
 * @param time Time when the intake occurred (in milliseconds)
 * @param containerType Type of container used (e.g., "Glass", "Bottle", "Cup")
 * @param timestamp When this entry was recorded (in milliseconds)
 */
data class WaterIntake(
    val id: String = "",
    val userId: String = "",
    val date: String = "",  // YYYY-MM-DD format
    val amount: Int = 0,    // in milliliters
    val time: Long = 0L,    // in milliseconds
    val containerType: String = ContainerType.GLASS,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        // Standard container sizes in ml
        const val GLASS_SIZE = 250
        const val BOTTLE_SIZE = 500
        const val MUG_SIZE = 350
        const val SMALL_BOTTLE_SIZE = 330
        const val LARGE_BOTTLE_SIZE = 750
    }

    /**
     * Container types with standard sizes
     */
    object ContainerType {
        const val GLASS = "Glass"
        const val BOTTLE = "Bottle"
        const val MUG = "Mug"
        const val SMALL_BOTTLE = "Small Bottle"
        const val LARGE_BOTTLE = "Large Bottle"
        const val CUSTOM = "Custom"

        val ALL = listOf(GLASS, BOTTLE, MUG, SMALL_BOTTLE, LARGE_BOTTLE, CUSTOM)

        /**
         * Get the default size for a container type in milliliters
         */
        fun getDefaultSize(type: String): Int {
            return when (type) {
                GLASS -> GLASS_SIZE
                BOTTLE -> BOTTLE_SIZE
                MUG -> MUG_SIZE
                SMALL_BOTTLE -> SMALL_BOTTLE_SIZE
                LARGE_BOTTLE -> LARGE_BOTTLE_SIZE
                else -> GLASS_SIZE
            }
        }
    }
}