package me.matsumo.travelog.core.datasource.helper

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath
import java.io.File

class PreferenceHelperImpl(
    private val ioDispatcher: CoroutineDispatcher,
) : PreferenceHelper {

    override fun create(name: String): DataStore<Preferences> {
        val dataDir = File(System.getProperty("user.home"), ".travelog")
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
        val file = File(dataDir, "$name.preferences_pb")

        return PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = CoroutineScope(ioDispatcher),
            produceFile = { file.absolutePath.toPath() },
        )
    }

    override fun delete(name: String) {
        // do nothing
    }
}
