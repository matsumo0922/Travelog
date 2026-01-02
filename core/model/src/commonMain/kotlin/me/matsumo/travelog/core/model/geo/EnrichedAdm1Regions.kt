package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import me.matsumo.travelog.core.model.geo.EnrichedRegion.Companion.closeRing

@Serializable
data class EnrichedAdm1Regions(
    @SerialName("id")
    val id: String? = null,
    @SerialName("adm_id")
    val admId: String,
    @SerialName("adm_name")
    val admName: String,
    @SerialName("adm_group")
    val admGroup: String,
    @SerialName("adm_iso")
    val admISO: String,
    @SerialName("name")
    val name: String,
    @SerialName("name_en")
    val nameEn: String?,
    @SerialName("name_ja")
    val nameJa: String?,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String?,
    @SerialName("polygons")
    val polygons: List<PolygonWithHoles>,
    @SerialName("regions")
    val regions: List<EnrichedRegion>,
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
}