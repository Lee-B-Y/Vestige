package com.lee.vestige.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lee.vestige.VestigeApp
import com.lee.vestige.export.SaveResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class Screen { Home, Editor }

data class MainUiState(
    val screen: Screen = Screen.Home,
    val exportDirUri: Uri? = null,
    val editorDate: LocalDate = LocalDate.now(),
    val editorContent: String = "",
    val isBusy: Boolean = false,
    /** Transient user-facing message (error / status). */
    val message: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as VestigeApp).container

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /** True when the editor has unsaved changes (drives auto-save). */
    private var dirty = false

    init {
        // Reflect the persisted export directory into UI state.
        viewModelScope.launch {
            container.settingsStore.exportTreeUri.collect { uri ->
                _uiState.update { it.copy(exportDirUri = uri) }
            }
        }
        // Timed auto-save: every 30s, persist if there are unsaved edits and we're editing.
        viewModelScope.launch {
            while (true) {
                delay(AUTO_SAVE_INTERVAL_MS)
                if (_uiState.value.screen == Screen.Editor) saveIfDirty()
            }
        }
    }

    fun onDirectoryPicked(uri: Uri) {
        viewModelScope.launch {
            container.settingsStore.setExportTreeUri(uri)
            _uiState.update { it.copy(message = "已设置保存目录") }
        }
    }

    /**
     * Open [date] in the editor: load the existing note if present, otherwise generate
     * an initial note from the data plugins. A freshly generated note is marked dirty so
     * auto-save persists it.
     */
    fun openDay(date: LocalDate) {
        val dirUri = _uiState.value.exportDirUri
        if (dirUri == null) {
            _uiState.update { it.copy(message = "请先在右上角选择保存目录") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val store = container.noteStoreFor(dirUri)
            val existing = store.read(date)
            val content = existing
                ?: container.renderer.render(container.aggregator.aggregate(date))
            dirty = existing == null // generated-but-unsaved should be saved
            _uiState.update {
                it.copy(
                    screen = Screen.Editor,
                    editorDate = date,
                    editorContent = content,
                    isBusy = false,
                )
            }
        }
    }

    fun onContentChange(text: String) {
        dirty = true
        _uiState.update { it.copy(editorContent = text) }
    }

    /** Back / navigate up from the editor: save then return home. */
    fun onLeaveEditor() {
        viewModelScope.launch {
            saveIfDirty()
            _uiState.update { it.copy(screen = Screen.Home) }
        }
    }

    /** App going to background: flush unsaved edits. */
    fun onStop() {
        viewModelScope.launch { saveIfDirty() }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun saveIfDirty() {
        if (!dirty) return
        val state = _uiState.value
        val dirUri = state.exportDirUri ?: return
        val store = container.noteStoreFor(dirUri)
        when (val result = store.write(state.editorDate, state.editorContent)) {
            is SaveResult.Success -> dirty = false
            is SaveResult.Error ->
                _uiState.update { it.copy(message = "保存失败：${result.message}") }
        }
    }

    companion object {
        private const val AUTO_SAVE_INTERVAL_MS = 60_000L
    }
}
