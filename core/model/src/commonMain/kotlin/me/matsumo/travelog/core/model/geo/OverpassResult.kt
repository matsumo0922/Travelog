package me.matsumo.travelog.core.model.geo


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OverpassResult(
    @SerialName("elements")
    val elements: List<Element>,
    @SerialName("generator")
    val generator: String,
    @SerialName("version")
    val version: Double
) {
    @Serializable
    data class Element(
        @SerialName("id")
        val id: Long,
        @SerialName("tags")
        val tags: Tags,
        @SerialName("type")
        val type: String,
        @SerialName("center")
        val center: Center,
    ) {
        @Serializable
        data class Tags(
            @SerialName("admin_level")
            val adminLevel: String,
            @SerialName("name")
            val name: String,
            @SerialName("name:en")
            val nameEn: String?,
            @SerialName("name:ja")
            val nameJa: String?,
        )

        @Serializable
        data class Center(
            @SerialName("lat")
            val lat: Double,
            @SerialName("lon")
            val lon: Double,
        )
    }
}