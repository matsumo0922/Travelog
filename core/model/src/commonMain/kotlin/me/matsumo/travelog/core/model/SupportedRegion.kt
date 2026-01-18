package me.matsumo.travelog.core.model

import androidx.compose.runtime.Stable
import me.matsumo.travelog.core.model.geo.GeoRegionGroup
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
    val code2: String,
    val code3: String,
    val nameRes: StringResource,
    val subRegionCount: Int,
) {
    val flagUrl = "https://flagcdn.com/h240/${code2.lowercase()}.webp"

    companion object {
        val all = listOf(
            SupportedRegion("JP", "JPN", Res.string.location_jp, 47), // 47都道府県
            SupportedRegion("KR", "KOR", Res.string.location_kr, 17), // 17広域自治体（1特別市、6広域市、8道、1特別自治道、1特別自治市）
            SupportedRegion("TW", "TWN", Res.string.location_tw, 22), // 22市縣（6直轄市、11県、3市、2離島）
            SupportedRegion("CN", "CHN", Res.string.location_cn, 34), // 34一級行政区（23省、5自治区、4直轄市、2特別行政区）
            SupportedRegion("US", "USA", Res.string.location_us, 50), // 50州（※首都DCを含める場合は51）
            SupportedRegion("GB", "GBR", Res.string.location_gb, 4), // 4カントリー（イングランド、スコットランド、ウェールズ、北アイルランド）
            SupportedRegion("FR", "FRA", Res.string.location_fr, 18), // 18レジョン（本土13 + 海外5）
            SupportedRegion("DE", "DEU", Res.string.location_de, 16), // 16連邦州
        )
    }
}

@Stable
data class Region(
    val supportedRegion: SupportedRegion,
    val groups: List<GeoRegionGroup>
)