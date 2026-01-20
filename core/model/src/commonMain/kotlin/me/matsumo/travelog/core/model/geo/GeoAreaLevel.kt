package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.Serializable

/**
 * Administrative level for geo areas.
 *
 * ADM0: Country
 * ADM1: Prefecture/State (first-level administrative division)
 * ADM2: City/District (second-level administrative division)
 * ADM3-5: Lower levels (ward, town, neighborhood, etc.)
 */
@Serializable
enum class GeoAreaLevel(val value: Int) {
    ADM0(0),
    ADM1(1),
    ADM2(2),
    ADM3(3),
    ADM4(4),
    ADM5(5);

    companion object {
        fun fromInt(value: Int): GeoAreaLevel = entries.first { it.value == value }
    }
}
