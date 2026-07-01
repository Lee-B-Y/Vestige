package com.lee.vestige.data.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vestige_settings")

/** The SAF grant and the directory within it that acts as the note root. */
data class NoteLocation(
    val treeUri: Uri,
    val relativeRoot: String? = null,
)

/**
 * Persists the SAF tree URI of the user's chosen note directory and its layout version.
 */
class SettingsStore(private val context: Context) {

    val noteLocation: Flow<NoteLocation?> = context.dataStore.data.map { prefs ->
        val treeUri = prefs[KEY_EXPORT_TREE_URI]?.let(Uri::parse) ?: return@map null
        NoteLocation(
            treeUri = treeUri,
            // A missing version identifies V1, where notes lived below Vestige/.
            relativeRoot = if (prefs[KEY_STORAGE_LAYOUT_VERSION] == null) LEGACY_ROOT else null,
        )
    }

    suspend fun setExportTreeUri(uri: Uri) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EXPORT_TREE_URI] = uri.toString()
            prefs[KEY_STORAGE_LAYOUT_VERSION] = CURRENT_STORAGE_LAYOUT_VERSION
        }
    }

    companion object {
        private val KEY_EXPORT_TREE_URI = stringPreferencesKey("export_tree_uri")
        private val KEY_STORAGE_LAYOUT_VERSION = intPreferencesKey("storage_layout_version")
        private const val CURRENT_STORAGE_LAYOUT_VERSION = 2
        private const val LEGACY_ROOT = "Vestige"
    }
}
