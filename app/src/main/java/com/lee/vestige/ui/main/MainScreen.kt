package com.lee.vestige.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lee.vestige.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
    // The date to open once permissions resolve.
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    // Calendar is required; location is optional (only enables the weather section).
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        result[Manifest.permission.READ_CALENDAR]?.let { hasCalendarPermission = it }
        result[Manifest.permission.ACCESS_COARSE_LOCATION]?.let { hasLocationPermission = it }
        val date = pendingDate
        pendingDate = null
        if (hasCalendarPermission && date != null) viewModel.openDay(date)
    }

    fun openWithPermissions(date: LocalDate) {
        val needed = buildList {
            if (!hasCalendarPermission) add(Manifest.permission.READ_CALENDAR)
            if (!hasLocationPermission) add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (needed.isEmpty()) {
            viewModel.openDay(date)
        } else {
            pendingDate = date
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    // Best-effort hint so the directory picker opens at Documents by default.
    val documentsInitialUri = remember {
        DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Documents",
        )
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

    // Flush unsaved edits when the app goes to background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onStop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    when (state.screen) {
        Screen.Home -> HomeScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onPickDirectory = { directoryLauncher.launch(documentsInitialUri) },
            onOpenDay = { date -> openWithPermissions(date) },
            onOpenBrowse = viewModel::openBrowse,
        )
        Screen.Editor -> EditorScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onContentChange = viewModel::onContentChange,
            onBack = viewModel::onLeaveEditor,
        )
        Screen.Browse -> BrowseScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onBack = viewModel::onLeaveBrowse,
            onSearch = viewModel::runSearch,
            onOpenNote = { date -> viewModel.openDay(date) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: MainUiState,
    snackbarHostState: SnackbarHostState,
    onPickDirectory: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onOpenBrowse: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    // Directory setting tucked into the corner — rarely changed.
                    TextButton(onClick = onPickDirectory) {
                        Text(if (state.exportDirUri == null) "选择目录" else "目录")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = { onOpenDay(LocalDate.now()) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isBusy) "打开中…" else "写今天的日记")
            }

            // Secondary, de-emphasized: writing for another day.
            TextButton(onClick = { showDatePicker = true }, enabled = !state.isBusy) {
                Text("其它日期…")
            }

            TextButton(onClick = onOpenBrowse, enabled = !state.isBusy) {
                Text("浏览/搜索日记")
            }

            if (state.exportDirUri == null) {
                Text("提示：先在右上角选择一个保存目录（建议 Obsidian Vault 里的日记文件夹）。")
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.now()
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        showDatePicker = false
                        onOpenDay(date)
                    }
                }) { Text("打开") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(
    state: MainUiState,
    snackbarHostState: SnackbarHostState,
    onContentChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    // System back also saves and returns home.
    BackHandler(onBack = onBack)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.editorDate.toString()) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                },
            )
        },
    ) { padding ->
        OutlinedTextField(
            value = state.editorContent,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseScreen(
    state: MainUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onOpenNote: (LocalDate) -> Unit,
) {
    BackHandler(onBack = onBack)
    var query by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("日记") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("搜索关键词…") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSearch(query) }) { Text("搜索") }
            }

            when {
                state.isBrowseLoading -> CircularProgressIndicator()
                state.browseItems.isEmpty() -> Text("没有找到日记。")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(state.browseItems) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenNote(item.date) }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(item.date.toString(), fontWeight = FontWeight.Bold)
                            item.snippet?.let { Text(it, maxLines = 2) }
                        }
                    }
                }
            }
        }
    }
}
