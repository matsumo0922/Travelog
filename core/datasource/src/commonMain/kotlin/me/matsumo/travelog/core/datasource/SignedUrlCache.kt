package me.matsumo.travelog.core.datasource

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * 署名付きURLのメモリキャッシュ
 * 署名付きURLは通常1時間有効なので、デフォルトTTLは50分
 */
class SignedUrlCache(
    private val defaultTtl: Duration = 50.minutes,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val cache = mutableMapOf<String, CacheEntry>()
    private val mutex = Mutex()

    private data class CacheEntry(
        val url: String,
        val expiresAt: TimeSource.Monotonic.ValueTimeMark,
    )

    /**
     * キャッシュからURLを取得する
     * @return キャッシュが存在し、有効期限内の場合はURL、それ以外はnull
     */
    suspend fun get(bucketName: String, storageKey: String): String? = mutex.withLock {
        val key = createKey(bucketName, storageKey)
        val entry = cache[key]

        if (entry != null && entry.expiresAt.hasNotPassedNow()) {
            return entry.url
        }

        // 期限切れの場合はキャッシュから削除
        if (entry != null) {
            cache.remove(key)
        }

        return null
    }

    /**
     * URLをキャッシュに保存する
     */
    suspend fun put(
        bucketName: String,
        storageKey: String,
        url: String,
        ttl: Duration = defaultTtl,
    ) = mutex.withLock {
        val key = createKey(bucketName, storageKey)
        val expiresAt = (timeSource as TimeSource.Monotonic).markNow() + ttl
        cache[key] = CacheEntry(url, expiresAt)
    }

    /**
     * キャッシュをクリアする
     */
    suspend fun clear() = mutex.withLock {
        cache.clear()
    }

    private fun createKey(bucketName: String, storageKey: String): String {
        return "$bucketName:$storageKey"
    }
}
