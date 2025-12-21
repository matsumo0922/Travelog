package me.matsumo.travelog.core.model

import kotlinx.serialization.Serializable

@Serializable
data class GeoBoundaryInfo(
    val boundaryID: String,
    val boundaryName: String,
    val boundaryISO: String,
    val boundaryType: String,
    val boundaryYearRepresented: String? = null,
    val boundarySource: String? = null,
    val boundaryLicense: String? = null,
    val boundaryCanonical: String? = null,
    val gjDownloadURL: String? = null,
    val tjDownloadURL: String? = null,
    val simplifiedGeometryGeoJSON: String? = null,
    val staticDownloadLink: String? = null,
)
