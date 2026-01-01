package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import me.matsumo.travelog.core.model.geo.OverpassResult.Element.Coordinate

@Serializable
data class EnrichedRegion(
    @SerialName("id")
    val id: String? = null,
    @SerialName("name")
    val name: String,
    @SerialName("adm2_id")
    val adm2Id: String,
    @SerialName("name_en")
    val nameEn: String?,
    @SerialName("name_ja")
    val nameJa: String?,
    @SerialName("wikipedia")
    val wikipedia: String?,
    @SerialName("iso3166_2")
    val iso31662: String?,
    @SerialName("center")
    val center: Coordinate,
    @SerialName("polygons")
    val polygons: List<PolygonWithHoles>,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String?,
) {
    fun getGeoJsonMultiPolygon() = buildJsonObject {
        put("type", JsonPrimitive("MultiPolygon"))
        put(
            "coordinates", JsonArray(
                polygons.map { polygon ->
                    JsonArray(
                        polygon.map { ring ->
                            JsonArray(
                                closeRing(ring).map { coord ->
                                    JsonArray(
                                        listOf(
                                            JsonPrimitive(coord.lon),
                                            JsonPrimitive(coord.lat),
                                        )
                                    )
                                }
                            )
                        }
                    )
                }
            ))
    }

    fun getGeoJsonPoint() = buildJsonObject {
        put("type", JsonPrimitive("Point"))
        put(
            "coordinates", JsonArray(
                listOf(
                    JsonPrimitive(center.lon),
                    JsonPrimitive(center.lat)
                )
            )
        )
    }

    private fun closeRing(ring: PolygonRing): PolygonRing {
        if (ring.isEmpty()) return ring

        val first = ring.first()
        val last = ring.last()

        return if (first.lat == last.lat && first.lon == last.lon) {
            ring
        } else {
            ring + first
        }
    }

}