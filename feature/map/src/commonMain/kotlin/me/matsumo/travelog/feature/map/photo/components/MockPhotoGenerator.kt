package me.matsumo.travelog.feature.map.photo.components

import me.matsumo.travelog.core.ui.component.TileGridConfig
import me.matsumo.travelog.feature.map.photo.components.model.GridPhotoItem
import kotlin.random.Random

object MockPhotoGenerator {

    fun generateMockPhotos(
        count: Int,
        config: TileGridConfig = TileGridConfig(),
        seed: Int = 42,
    ): List<GridPhotoItem> {
        val random = Random(seed)
        val items = mutableListOf<GridPhotoItem>()

        for (i in 0 until count) {
            val isSpecialSize = (i + 1) % config.specialSizeInterval == 0 && config.availableSpecialSizes.isNotEmpty()

            val (spanWidth, spanHeight) = if (isSpecialSize) {
                selectWeightedSpanSize(config, random)
            } else {
                1 to 1
            }

            val imageSize = 300 + random.nextInt(200)
            val imageId = random.nextInt(1000)

            items.add(
                GridPhotoItem(
                    id = "photo_$i",
                    imageUrl = "https://picsum.photos/seed/$imageId/$imageSize/$imageSize",
                    spanWidth = spanWidth,
                    spanHeight = spanHeight,
                ),
            )
        }

        return items
    }

    private fun selectWeightedSpanSize(config: TileGridConfig, random: Random): Pair<Int, Int> {
        val sizes = config.availableSpecialSizes
        val totalWeight = sizes.sumOf { it.weight.toDouble() }
        var randomValue = random.nextDouble() * totalWeight

        for (size in sizes) {
            randomValue -= size.weight
            if (randomValue <= 0) {
                return size.spanWidth to size.spanHeight
            }
        }

        return sizes.last().let { it.spanWidth to it.spanHeight }
    }
}
