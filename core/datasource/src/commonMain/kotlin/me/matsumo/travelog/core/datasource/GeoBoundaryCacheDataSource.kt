package me.matsumo.travelog.core.datasource

interface GeoBoundaryCacheDataSource {
    suspend fun save(key: String, text: String)
    suspend fun load(key: String): String?
    suspend fun exists(key: String): Boolean
    suspend fun clear()
}
