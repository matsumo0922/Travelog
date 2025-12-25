package me.matsumo.travelog.core.repository

import me.matsumo.travelog.core.datasource.GeoBoundaryDataSource
import me.matsumo.travelog.core.datasource.NominatimDataSource
import me.matsumo.travelog.core.datasource.OverpassDataSource
import me.matsumo.travelog.core.model.geo.GeoBoundaryInfo
import me.matsumo.travelog.core.model.geo.GeoBoundaryLevel
import me.matsumo.travelog.core.model.geo.GeoJsonData

class GeoBoundaryRepository(
    private val geoBoundaryDataSource: GeoBoundaryDataSource,
    private val nominatimDataSource: NominatimDataSource,
    private val overpassDataSource: OverpassDataSource,
) {
    suspend fun getBoundaryInfo(
        countryIso: String,
        level: GeoBoundaryLevel,
    ): GeoBoundaryInfo {
        return geoBoundaryDataSource.fetchBoundaryInfo(countryIso, level)
    }

    suspend fun downloadGeoJson(boundaryInfo: GeoBoundaryInfo): GeoJsonData {
        val geoJsonUrl = boundaryInfo.gjDownloadURL ?: error("gjDownloadURL is null.")
        return geoBoundaryDataSource.downloadGeoJson(geoJsonUrl)
    }

    suspend fun downloadGeoJsonFromUrl(geoJsonUrl: String): GeoJsonData {
        return geoBoundaryDataSource.downloadGeoJson(geoJsonUrl)
    }
}
