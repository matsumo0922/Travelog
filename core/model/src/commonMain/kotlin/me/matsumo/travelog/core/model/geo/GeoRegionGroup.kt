package me.matsumo.travelog.core.model.geo

import androidx.compose.runtime.Stable
import io.ktor.http.encodeURLPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import me.matsumo.travelog.core.model.geo.GeoRegion.Companion.closeRing

@Stable
@Serializable
data class GeoRegionGroup(
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
    val regions: List<GeoRegion>,
) {
    val degradedThumbnailUrl: String?
        get() = thumbnailUrl?.let { original ->
            val fileName = original.substringAfterLast('/')
            val encodedFileName = fileName.encodeURLPath()
            val pathPart = original
                .replace("https://upload.wikimedia.org/wikipedia/commons/", "")

            "https://upload.wikimedia.org/wikipedia/commons/thumb/$pathPart/330px-$encodedFileName"
        }

    fun getGeoJsonMultiPolygon() = buildJsonObject {
        put("type", JsonPrimitive("MultiPolygon"))
        put(
            "coordinates",
            JsonArray(
                polygons.map { polygon ->
                    JsonArray(
                        polygon.map { ring ->
                            JsonArray(
                                closeRing(ring).map { coord ->
                                    JsonArray(
                                        listOf(
                                            JsonPrimitive(coord.lon),
                                            JsonPrimitive(coord.lat),
                                        ),
                                    )
                                },
                            )
                        },
                    )
                },
            ),
        )
    }
}
