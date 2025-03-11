package com.example.lifemaxx.model

/**
 * Represents information about a supplement obtained from a barcode scan.
 *
 * @param barcode The raw barcode value scanned
 * @param name Name of the supplement
 * @param manufacturer Manufacturer of the supplement
 * @param servingSize Standard serving size
 * @param measureUnit Measurement unit for the supplement (pill, mg, g, etc.)
 * @param dailyDose Recommended daily dose
 * @param imageUrl URL to an image of the supplement (optional)
 * @param description Brief description of the supplement
 * @param exists Whether this supplement already exists in the user's list
 */
data class SupplementBarcode(
    val barcode: String = "",
    val name: String = "",
    val manufacturer: String = "",
    val servingSize: Int = 1,
    val measureUnit: String = "pill",
    val dailyDose: Int = 1,
    val imageUrl: String = "",
    val description: String = "",
    val exists: Boolean = false
) {
    /**
     * Converts this barcode result to a Supplement model that can be saved
     */
    fun toSupplement(): Supplement {
        return Supplement(
            name = name,
            dailyDose = dailyDose,
            remainingQuantity = 30, // Default to a month's supply
            measureUnit = measureUnit,
            isTaken = false
        )
    }

    companion object {
        /**
         * Creates a placeholder supplement from a barcode when no lookup information is available
         */
        fun createPlaceholder(barcode: String): SupplementBarcode {
            return SupplementBarcode(
                barcode = barcode,
                name = "Unknown Supplement",
                manufacturer = "Unknown",
                description = "Supplement scanned from barcode: $barcode"
            )
        }
    }
}