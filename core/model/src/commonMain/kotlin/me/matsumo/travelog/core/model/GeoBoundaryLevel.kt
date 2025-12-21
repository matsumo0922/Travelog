package me.matsumo.travelog.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class GeoBoundaryLevel {
    ADM0, // Country level
    ADM1, // First-level administrative division
    ADM2, // Second-level administrative division
    ADM3, // Third-level administrative division
    ADM4, // Fourth-level administrative division
    ADM5; // Fifth-level administrative division

    companion object {
        fun fromString(value: String): GeoBoundaryLevel? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}
