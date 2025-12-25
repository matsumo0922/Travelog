package me.matsumo.travelog.core.model

import androidx.compose.runtime.Stable
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.location_cn
import me.matsumo.travelog.core.resource.location_de
import me.matsumo.travelog.core.resource.location_fr
import me.matsumo.travelog.core.resource.location_gb
import me.matsumo.travelog.core.resource.location_jp
import me.matsumo.travelog.core.resource.location_kr
import me.matsumo.travelog.core.resource.location_tw
import me.matsumo.travelog.core.resource.location_us
import org.jetbrains.compose.resources.StringResource

@Stable
data class SupportedRegion(
    val code: String,
    val nameRes: StringResource,
) {
    companion object {
        val all = listOf(
            SupportedRegion("JP", Res.string.location_jp),
            SupportedRegion("KR", Res.string.location_kr),
            SupportedRegion("TW", Res.string.location_tw),
            SupportedRegion("CN", Res.string.location_cn),
            SupportedRegion("US", Res.string.location_us),
            SupportedRegion("GB", Res.string.location_gb),
            SupportedRegion("FR", Res.string.location_fr),
            SupportedRegion("DE", Res.string.location_de),
        )
    }
}