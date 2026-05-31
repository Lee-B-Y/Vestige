package com.lee.vestige.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lee.vestige.R
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    var hasCalendarPermission by remember {
        mutableStateOf(isGranted(Manifest.permission.READ_CALENDAR))
    }
    var hasLocationPermission by remember {
        mutableStateOf(isGranted(Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    // Calendar is required; location is optional (only enables the weather section).
    // Once calendar is granted, export regardless of the location outcome.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        result[Manifest.permission.READ_CALENDAR]?.let { hasCalendarPermission = it }
        result[Manifest.permission.ACCESS_COARSE_LOCATION]?.let { hasLocationPermission = it }
        if (hasCalendarPermission) viewModel.export()
    }

    val directoryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.onDirectoryPicked(uri)
        }
    }

    // Show transient messages.
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.app_name))

            Text("导出目录：" + (state.exportDirUri?.toString() ?: "未选择"))
            OutlinedButton(onClick = { directoryLauncher.launch(null) }) {
                Text("选择导出目录")
            }

            Text("日期：${state.selectedDate}")
            OutlinedButton(onClick = { showDatePicker = true }) {
                Text("选择日期")
            }

            Button(
                onClick = {
                    val needed = buildList {
                        if (!hasCalendarPermission) add(Manifest.permission.READ_CALENDAR)
                        if (!hasLocationPermission) add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                    if (needed.isEmpty()) {
                        viewModel.export()
                    } else {
                        permissionLauncher.launch(needed.toTypedArray())
                    }
                },
                enabled = !state.isExporting,
            ) {
                Text(if (state.isExporting) "导出中…" else "导出 Markdown")
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDate
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.onDateSelected(date)
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    state.pendingOverwriteFileName?.let { fileName ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissOverwrite() },
            title = { Text("文件已存在") },
            text = { Text("$fileName 已存在，是否覆盖重新生成？") },
            confirmButton = {
                TextButton(onClick = { viewModel.export(overwrite = true) }) { Text("覆盖") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissOverwrite() }) { Text("取消") }
            },
        )
    }
}
