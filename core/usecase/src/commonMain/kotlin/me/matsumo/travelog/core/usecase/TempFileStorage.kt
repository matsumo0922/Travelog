package me.matsumo.travelog.core.usecase

import io.github.vinceglb.filekit.PlatformFile

/**
 * Temporary file storage for cross-navigation file passing.
 */
interface TempFileStorage {
    /**
     * Save a file to temporary storage and return its path.
     */
    suspend fun saveToTemp(file: PlatformFile): String

    /**
     * Load a file from temporary storage by its path.
     */
    suspend fun loadFromTemp(path: String): PlatformFile?

    /**
     * Delete a temporary file by its path.
     */
    suspend fun deleteTemp(path: String): Boolean
}
