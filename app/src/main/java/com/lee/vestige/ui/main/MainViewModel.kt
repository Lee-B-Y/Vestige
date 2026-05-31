package com.lee.vestige.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lee.vestige.VestigeApp
import com.lee.vestige.export.ExportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MainUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val exportDirUri: Uri? = null,
    val isExporting: Boolean = false,
    /** Set when a file already exists; UI shows an overwrite confirmation. */
    val pendingOverwriteFileName: String? = null,
    /** Transient user-facing message (success / error). */
    val message: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as VestigeApp).container

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Reflect the persisted export directory into UI state.
        viewModelScope.launch {
            container.settingsStore.exportTreeUri.collect { uri ->
                _uiState.update { it.copy(exportDirUri = uri) }
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun onDirectoryPicked(uri: Uri) {
        viewModelScope.launch {
            container.settingsStore.setExportTreeUri(uri)
            _uiState.update { it.copy(message = "已设置导出目录") }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    fun dismissOverwrite() {
        _uiState.update { it.copy(pendingOverwriteFileName = null) }
    }

    /** Triggered by the Export button. */
    fun export(overwrite: Boolean = false) {
        val state = _uiState.value
        val dirUri = state.exportDirUri
        if (dirUri == null) {
            _uiState.update { it.copy(message = "请先选择导出目录") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, pendingOverwriteFileName = null) }

            val entry = container.aggregator.aggregate(state.selectedDate)
            val content = container.renderer.render(entry)
            val fileName = "${state.selectedDate}.md"

            val target = container.exportTargetFor(dirUri)
            when (val result = target.export(fileName, content, overwrite)) {
                is ExportResult.Success ->
                    _uiState.update { it.copy(isExporting = false, message = "已导出 ${result.fileName}") }

                is ExportResult.AlreadyExists ->
                    _uiState.update {
                        it.copy(isExporting = false, pendingOverwriteFileName = result.fileName)
                    }

                is ExportResult.Error ->
                    _uiState.update { it.copy(isExporting = false, message = "导出失败：${result.message}") }
            }
        }
    }
}
