package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EnrichedAdm1Regions(
    @SerialName("id")
    val id: String? = null,
    @SerialName("adm_id")
    val admId: String,
    @SerialName("adm_name")
    val admName: String,
    @SerialName("regions")
    val regions: List<EnrichedRegion>,
)