package com.qrscanner.qrscanner.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qrscanner.qrscanner.MainApplication
import com.qrscanner.qrscanner.data.ScanHistoryEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AppState {
    data object Home : AppState()
    data object Scanning : AppState()
}

sealed class ScanResultState {
    data object None : ScanResultState()
    data class Success(val content: String, val isUrl: Boolean) : ScanResultState()
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as MainApplication).database.scanHistoryDao()

    private val _appState = MutableStateFlow<AppState>(AppState.Home)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _scanResult = MutableStateFlow<ScanResultState>(ScanResultState.None)
    val scanResult: StateFlow<ScanResultState> = _scanResult.asStateFlow()

    val scanHistory: StateFlow<List<ScanHistoryEntity>> = dao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection mode
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val selectedCount: Int get() = _selectedIds.value.size

    private val _historyList = mutableListOf<ScanHistoryEntity>()

    // Snackbar events
    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()

    fun startScanning() {
        _appState.value = AppState.Scanning
    }

    fun stopScanning() {
        _appState.value = AppState.Home
    }

    fun onQrDetected(content: String) {
        val isUrl = isValidUrl(content)
        val entity = ScanHistoryEntity(
            content = content,
            timestamp = System.currentTimeMillis(),
            isUrl = isUrl
        )
        viewModelScope.launch {
            dao.insert(entity)
        }
        _appState.value = AppState.Home
        _scanResult.value = ScanResultState.Success(content, isUrl)
    }

    fun dismissResult() {
        _scanResult.value = ScanResultState.None
    }

    fun openInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        getApplication<Application>().startActivity(intent)
        dismissResult()
    }

    fun copyToClipboard(text: String) {
        val clipboard = getApplication<Application>()
            .getSystemService(Application.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QR Code", text)
        clipboard.setPrimaryClip(clip)
        viewModelScope.launch {
            _snackbarEvent.emit("已复制到剪贴板")
        }
    }

    // Selection management
    fun enterSelectionMode(id: Long) {
        _isSelectionMode.value = true
        _selectedIds.value = setOf(id)
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedIds.value = current
        if (current.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun selectAll() {
        _selectedIds.value = _historyList.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun updateHistoryCache(list: List<ScanHistoryEntity>) {
        _historyList.clear()
        _historyList.addAll(list)
    }

    fun deleteSelected() {
        viewModelScope.launch {
            dao.deleteByIds(_selectedIds.value.toList())
            _selectedIds.value = emptySet()
            _isSelectionMode.value = false
        }
    }

    private fun isValidUrl(text: String): Boolean {
        return text.startsWith("http://") || text.startsWith("https://")
    }
}
