package com.lee.vestige.data.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vestige_settings")

/**
 * Persists the single piece of V1 state: the SAF tree URI of the user's chosen
 * export directory. Stored as a string and re-parsed to [Uri].
 */
class SettingsStore(private val context: Context) {

    val exportTreeUri: Flow<Uri?> = context.dataStore.data.map { prefs ->
        prefs[KEY_EXPORT_TREE_URI]?.let(Uri::parse)
    }

    suspend fun setExportTreeUri(uri: Uri) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EXPORT_TREE_URI] = uri.toString()
        }
    }

    companion object {
        private val KEY_EXPORT_TREE_URI = stringPreferencesKey("export_tree_uri")
    }
}
