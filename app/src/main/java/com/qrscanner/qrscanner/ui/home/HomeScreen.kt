package com.qrscanner.qrscanner.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qrscanner.qrscanner.data.ScanHistoryEntity
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.qrscanner.qrscanner.ui.dialog.ScanResultDialog
import com.qrscanner.qrscanner.ui.scanner.CameraPreview
import com.qrscanner.qrscanner.viewmodel.AppState
import com.qrscanner.qrscanner.viewmodel.ScanResultState
import com.qrscanner.qrscanner.viewmodel.ScannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: ScannerViewModel = viewModel()
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val scanResult by viewModel.scanResult.collectAsStateWithLifecycle()
    val history by viewModel.scanHistory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.startScanning()
        } else {
            Toast.makeText(context, "需要相机权限才能进行扫描", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(history) {
        viewModel.updateHistoryCache(history)
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    var showIntroDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    // Handle back press when in licenses screen
    BackHandler(enabled = showLicenses) {
        showLicenses = false
    }

    // Always track visibility for scan result dialog animation
    val showResultDialog = scanResult is ScanResultState.Success
    val resultContent = (scanResult as? ScanResultState.Success)?.content ?: ""
    val resultIsUrl = (scanResult as? ScanResultState.Success)?.isUrl ?: false

    AnimatedContent(
        targetState = appState,
        transitionSpec = {
            when (targetState) {
                is AppState.Home -> {
                    (fadeIn(animationSpec = tween(500)) +
                            slideInVertically(animationSpec = tween(500)) { it / 4 })
                        .togetherWith(
                            fadeOut(animationSpec = tween(350)) +
                                    slideOutVertically(animationSpec = tween(350)) { -it / 4 }
                        )
                }
                is AppState.Scanning -> {
                    (slideInVertically(animationSpec = tween(500)) { it } + fadeIn(animationSpec = tween(500)))
                        .togetherWith(
                            slideOutVertically(animationSpec = tween(350)) { -it } + fadeOut(animationSpec = tween(350))
                        )
                }
            }
        },
        label = "app_state_transition"
    ) { state ->
        when (state) {
            is AppState.Home -> {
                HomeContent(
                    history = history,
                    searchQuery = searchQuery,
                    isSelectionMode = isSelectionMode,
                    selectedIds = selectedIds,
                    snackbarHostState = snackbarHostState,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onScanClick = {
                        if (hasPermission) {
                            viewModel.startScanning()
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onTitleClick = { showIntroDialog = true },
                    onItemClick = { item ->
                        if (isSelectionMode) {
                            viewModel.toggleSelection(item.id)
                        } else {
                            viewModel.copyToClipboard(item.content)
                        }
                    },
                    onItemLongClick = { item ->
                        if (!isSelectionMode) {
                            viewModel.enterSelectionMode(item.id)
                        }
                    },
                    onBackPress = { viewModel.deselectAll() },
                    onSelectAllToggle = {
                        if (selectedIds.size == history.size) {
                            viewModel.deselectAll()
                        } else {
                            viewModel.selectAll()
                        }
                    },
                    onDeleteClick = {
                        if (selectedIds.isNotEmpty()) {
                            showDeleteConfirm = true
                        }
                    }
                )
            }
            is AppState.Scanning -> {
                CameraPreview(
                    onQrDetected = { content ->
                        viewModel.onQrDetected(content)
                    },
                    onBackPress = {
                        viewModel.stopScanning()
                    }
                )
            }
        }
    }

    // Always compose ScanResultDialog for proper enter animation
    ScanResultDialog(
        content = resultContent,
        isUrl = resultIsUrl,
        visible = showResultDialog,
        onDismiss = { viewModel.dismissResult() },
        onOpen = { url -> viewModel.openInBrowser(url) },
        onCopy = { text -> viewModel.copyToClipboard(text) }
    )

    if (showIntroDialog) {
        AlertDialog(
            onDismissRequest = { showIntroDialog = false },
            title = {
                Text(
                    text = "QRScanner",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "just for fun",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "https://github.com/yumiru11/QRScanner",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "(≧▽≦)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = {
                        showIntroDialog = false
                        showLicenses = true
                    }) {
                        Text(
                            text = "开源许可",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntroDialog = false }) {
                    Text("好的")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${selectedIds.size} 条记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteSelected()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showLicenses) {
        LibrariesContainer(modifier = Modifier.fillMaxSize())
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    history: List<ScanHistoryEntity>,
    searchQuery: String,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onTitleClick: () -> Unit,
    onItemClick: (ScanHistoryEntity) -> Unit,
    onItemLongClick: (ScanHistoryEntity) -> Unit,
    onBackPress: () -> Unit,
    onSelectAllToggle: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val allSelected = history.isNotEmpty() && selectedIds.size == history.size

    // Animate top bar transition
    val topBarColor by animateColorAsState(
        targetValue = if (isSelectionMode)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(350),
        label = "topbar_color"
    )
    val topBarContentColor by animateColorAsState(
        targetValue = if (isSelectionMode)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(350),
        label = "topbar_content_color"
    )

    // Animate FAB
    val fabColor by animateColorAsState(
        targetValue = if (isSelectionMode && selectedIds.isNotEmpty())
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.primary,
        animationSpec = tween(300),
        label = "fab_color"
    )
    val fabContentColor by animateColorAsState(
        targetValue = if (isSelectionMode && selectedIds.isNotEmpty())
            MaterialTheme.colorScheme.onError
        else
            MaterialTheme.colorScheme.onPrimary,
        animationSpec = tween(300),
        label = "fab_content_color"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    AnimatedVisibility(
                        visible = isSelectionMode,
                        enter = slideInVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                        exit = slideOutVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
                    ) {
                        IconButton(onClick = onBackPress) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "退出选择",
                                tint = topBarContentColor
                            )
                        }
                    }
                },
                title = {
                    AnimatedContent(
                        targetState = isSelectionMode,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(150))
                        },
                        label = "title_transition"
                    ) { selectionMode ->
                        if (selectionMode) {
                            Text(
                                text = "已选择 ${selectedIds.size} 项",
                                style = MaterialTheme.typography.titleMedium,
                                color = topBarContentColor
                            )
                        } else {
                            Text(
                                text = "QRScanner",
                                style = MaterialTheme.typography.titleLarge,
                                color = topBarContentColor,
                                modifier = Modifier.clickable(onClick = onTitleClick)
                            )
                        }
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = isSelectionMode,
                        enter = slideInVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                        exit = slideOutVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
                    ) {
                        TextButton(onClick = onSelectAllToggle) {
                            Text(
                                if (allSelected) "取消全选" else "全选",
                                color = topBarContentColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor,
                    titleContentColor = topBarContentColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isSelectionMode && selectedIds.isNotEmpty()) {
                        onDeleteClick()
                    } else if (!isSelectionMode) {
                        onScanClick()
                    }
                },
                containerColor = fabColor,
                contentColor = fabContentColor,
                shape = CircleShape
            ) {
                AnimatedContent(
                    targetState = isSelectionMode && selectedIds.isNotEmpty(),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "fab_icon"
                ) { showDelete ->
                    if (showDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "扫描二维码",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { padding ->
        val isSearchActive = searchQuery.isNotBlank()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isSelectionMode && (isSearchActive || history.isNotEmpty())) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text(
                            text = "搜索扫描记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "清除搜索",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = if (isSearchActive) "未找到匹配结果" else "暂无扫描记录",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isSearchActive)
                                "请尝试其他关键词"
                            else
                                "点击右下角的扫描按钮开始扫描二维码",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(history, key = { it.id }) { item ->
                        HistoryItem(
                            item = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedIds.contains(item.id),
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItem(
    item: ScanHistoryEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(250),
        label = "card_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 0.5.dp),
        border = if (isDark) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Row(
            modifier = Modifier
                .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 4.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatTimestamp(item.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
