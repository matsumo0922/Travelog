package me.matsumo.travelog.feature.map.di

import me.matsumo.travelog.core.model.db.Map
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.feature.map.MapDetailViewModel
import me.matsumo.travelog.feature.map.area.MapAreaDetailViewModel
import me.matsumo.travelog.feature.map.crop.PhotoCropEditorViewModel
import me.matsumo.travelog.feature.map.select.MapSelectRegionViewModel
import me.matsumo.travelog.feature.map.setting.MapSettingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mapModule = module {
    viewModel { extras ->
        MapDetailViewModel(
            mapId = extras.get<String>(),
            mapRepository = get(),
            mapRegionRepository = get(),
            geoAreaRepository = get(),
            getMapRegionImagesUseCase = get(),
        )
    }

    viewModel { extras ->
        @Suppress("UNCHECKED_CAST")
        MapSettingViewModel(
            mapId = extras[0] as String,
            initialMap = extras[1] as? Map,
            initialTotalChildCount = extras[2] as? Int,
            initialRegions = extras[3] as? List<MapRegion>,
            mapRepository = get(),
            mapRegionRepository = get(),
            geoAreaRepository = get(),
            updateMapIconUseCase = get(),
        )
    }

    viewModel { extras ->
        @Suppress("UNCHECKED_CAST")
        MapSelectRegionViewModel(
            mapId = extras[0] as String,
            geoAreaId = extras[1] as String,
            initialRegions = extras[2] as? List<MapRegion>,
            initialRegionImageUrls = extras[3] as? kotlin.collections.Map<String, String>,
            geoAreaRepository = get(),
            mapRegionRepository = get(),
            getMapRegionImagesUseCase = get(),
        )
    }

    viewModel { extras ->
        @Suppress("UNCHECKED_CAST")
        MapAreaDetailViewModel(
            mapId = extras[0] as String,
            geoAreaId = extras[1] as String,
            initialRegions = extras[2] as? List<MapRegion>,
            initialRegionImageUrls = extras[3] as? kotlin.collections.Map<String, String>,
            geoAreaRepository = get(),
            mapRegionRepository = get(),
            getMapRegionImagesUseCase = get(),
        )
    }

    viewModel { extras ->
        PhotoCropEditorViewModel(
            mapId = extras[0] as String,
            geoAreaId = extras[1] as String,
            localFilePath = extras[2] as String,
            existingRegionId = extras[3] as? String,
            geoAreaRepository = get(),
            mapRegionRepository = get(),
            saveMapRegionPhotoUseCase = get(),
            tempFileStorage = get(),
        )
    }
}
