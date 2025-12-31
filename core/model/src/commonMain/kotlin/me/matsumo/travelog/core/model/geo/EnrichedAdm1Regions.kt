package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EnrichedAdm1Regions(
    @SerialName("parent_adm_id")
    val parentAdmId: String,
    @SerialName("parent_adm_name")
    val parentAdmName: String,
    @SerialName("regions")
    val regions: List<EnrichedRegion>,
)