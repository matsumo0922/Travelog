package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EnrichedAdm1Regions(
    @SerialName("adm1_name")
    val adm1Name: String,
    @SerialName("regions")
    val regions: List<EnrichedRegion>,
)