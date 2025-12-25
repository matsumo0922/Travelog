package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WikipediaThumbnailResult(
    @SerialName("title")
    val title: String,
    @SerialName("thumbnail")
    val thumbnail: Image? = null,
    @SerialName("originalimage")
    val originalImage: Image? = null
) {
    @Serializable
    data class Image(
        @SerialName("source")
        val source: String
    )
}