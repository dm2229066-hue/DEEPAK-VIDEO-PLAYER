package com.example.ui.viewmodel

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DeepakPlayerApp
import com.example.data.local.NetworkStreamHistoryEntity
import com.example.data.local.PlaylistWithItems
import com.example.data.model.SortOption
import com.example.data.model.VideoFolder
import com.example.data.model.VideoItem
import com.example.data.model.VideoMetadataInfo
import com.example.data.model.ViewMode
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.OrientationPreference
import com.example.data.preferences.UserPreferences
import com.example.ui.theme.AccentColorOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VideoFilter {
    ALL, FAVORITES, RECENT, CONTINUE
}

data class LibraryUiState(
    val allVideos: List<VideoItem> = emptyList(),
    val filteredVideos: List<VideoItem> = emptyList(),
    val continueWatching: List<VideoItem> = emptyList(),
    val recentlyPlayed: List<VideoItem> = emptyList(),
    val recentlyAdded: List<VideoItem> = emptyList(),
    val favoriteVideos: List<VideoItem> = emptyList(),
    val folders: List<VideoFolder> = emptyList(),
    val playlists: List<PlaylistWithItems> = emptyList(),
    val networkStreams: List<NetworkStreamHistoryEntity> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: VideoFilter = VideoFilter.ALL,
    val isScanning: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val preferences: UserPreferences = UserPreferences()
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DeepakPlayerApp
    private val repository = app.libraryRepository
    private val prefsRepo = app.preferencesRepository

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(VideoFilter.ALL)
    private val _permissionGranted = MutableStateFlow(repository.scanner.hasMediaPermission())
    private val _inspectedMetadata = MutableStateFlow<VideoMetadataInfo?>(null)
    val inspectedMetadata: StateFlow<VideoMetadataInfo?> = _inspectedMetadata.asStateFlow()

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.enrichedVideos,
        repository.hiddenFolders,
        repository.safTrees,
        repository.playlists,
        repository.networkStreams,
        repository.isScanning,
        prefsRepo.preferencesFlow,
        _searchQuery,
        _activeFilter,
        _permissionGranted
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val videos = values[0] as List<VideoItem>
        @Suppress("UNCHECKED_CAST")
        val hidden = values[1] as List<com.example.data.local.HiddenFolderEntity>
        @Suppress("UNCHECKED_CAST")
        val safTrees = values[2] as List<com.example.data.local.SafTreeEntity>
        @Suppress("UNCHECKED_CAST")
        val playlists = values[3] as List<PlaylistWithItems>
        @Suppress("UNCHECKED_CAST")
        val streams = values[4] as List<NetworkStreamHistoryEntity>
        val scanning = values[5] as Boolean
        val prefs = values[6] as UserPreferences
        val query = values[7] as String
        val filter = values[8] as VideoFilter
        val hasPerm = values[9] as Boolean

        val hiddenSet = hidden.map { it.folderPath }.toSet()
        val safSet = safTrees.map { it.treeUri }.toSet()

        val visibleVideos = if (prefs.showHiddenFolders) {
            videos
        } else {
            videos.filter { it.folderPath !in hiddenSet }
        }

        val sorted = repository.sortVideos(visibleVideos, prefs.sortOption, prefs.sortAscending)

        val searched = if (query.isBlank()) {
            sorted
        } else {
            val q = query.trim().lowercase()
            sorted.filter {
                it.title.lowercase().contains(q) ||
                    it.folderName.lowercase().contains(q) ||
                    it.displayName.lowercase().contains(q)
            }
        }

        val filtered = when (filter) {
            VideoFilter.ALL -> searched
            VideoFilter.FAVORITES -> searched.filter { it.isFavorite }
            VideoFilter.RECENT -> searched.filter { it.lastPlayedAt > 0L }.sortedByDescending { it.lastPlayedAt }
            VideoFilter.CONTINUE -> searched.filter {
                it.lastPositionMs > 2000L && (it.durationMs <= 0L || it.lastPositionMs < it.durationMs - 4000L)
            }.sortedByDescending { it.lastPlayedAt }
        }

        val continueWatching = visibleVideos
            .filter {
                it.lastPositionMs > 2000L && (it.durationMs <= 0L || it.lastPositionMs < it.durationMs - 4000L)
            }
            .sortedByDescending { it.lastPlayedAt }
            .take(15)

        val recentlyPlayed = visibleVideos
            .filter { it.lastPlayedAt > 0L }
            .sortedByDescending { it.lastPlayedAt }
            .take(15)

        val recentlyAdded = visibleVideos
            .sortedByDescending { it.dateAddedSec }
            .take(15)

        val favoriteVideos = visibleVideos.filter { it.isFavorite }
        val folders = repository.buildFolders(videos, hiddenSet, safSet, prefs.showHiddenFolders)

        LibraryUiState(
            allVideos = sorted,
            filteredVideos = filtered,
            continueWatching = continueWatching,
            recentlyPlayed = recentlyPlayed,
            recentlyAdded = recentlyAdded,
            favoriteVideos = favoriteVideos,
            folders = folders,
            playlists = playlists,
            networkStreams = streams,
            searchQuery = query,
            activeFilter = filter,
            isScanning = scanning,
            hasStoragePermission = hasPerm,
            preferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState(hasStoragePermission = repository.scanner.hasMediaPermission())
    )

    init {
        refreshVideos()
    }

    fun refreshPermissionAndScan() {
        _permissionGranted.value = repository.scanner.hasMediaPermission()
        refreshVideos()
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _permissionGranted.value = repository.scanner.hasMediaPermission()
            repository.refreshLibrary()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: VideoFilter) {
        _activeFilter.value = filter
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val current = uiState.value.preferences.viewMode
            prefsRepo.setViewMode(if (current == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
        }
    }

    fun setSort(option: SortOption, ascending: Boolean) {
        viewModelScope.launch {
            prefsRepo.setSort(option, ascending)
        }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch { repository.toggleFavorite(video) }
    }

    fun toggleHideFolder(folder: VideoFolder) {
        viewModelScope.launch {
            repository.toggleHideFolder(folder.path, folder.name, folder.isHidden)
        }
    }

    fun addSafFolder(treeUri: Uri, label: String) {
        viewModelScope.launch {
            repository.addSafTree(treeUri.toString(), label)
        }
    }

    fun removeSafFolder(treeUri: String) {
        viewModelScope.launch {
            repository.removeSafTree(treeUri)
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    fun renamePlaylist(id: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repository.renamePlaylist(id, newName) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { repository.deletePlaylist(id) }
    }

    fun addVideoToPlaylist(playlistId: Long, video: VideoItem) {
        viewModelScope.launch { repository.addVideoToPlaylist(playlistId, video) }
    }

    fun removeVideoFromPlaylist(playlistId: Long, videoUri: String) {
        viewModelScope.launch { repository.removeVideoFromPlaylist(playlistId, videoUri) }
    }

    fun saveNetworkStream(url: String, title: String) {
        viewModelScope.launch { repository.saveNetworkStream(url, title) }
    }

    fun deleteNetworkStream(url: String) {
        viewModelScope.launch { repository.deleteNetworkStream(url) }
    }

    fun inspectVideo(video: VideoItem) {
        viewModelScope.launch {
            _inspectedMetadata.value = repository.inspectMetadata(video)
        }
    }

    fun dismissMetadata() {
        _inspectedMetadata.value = null
    }

    fun createDeleteSender(uri: Uri): IntentSender? = repository.createDeleteSender(uri)

    fun createWriteSender(uri: Uri): IntentSender? = repository.createWriteSender(uri)

    fun deleteVideoDirect(uri: Uri, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = repository.deleteDirect(uri)
            onResult(ok)
        }
    }

    fun renameVideoDirect(uri: Uri, newName: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = repository.renameDirect(uri, newName)
            onResult(ok)
        }
    }

    fun clearPlaybackHistory() {
        viewModelScope.launch { repository.clearPlaybackHistory() }
    }

    // Settings updates
    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDarkTheme(enabled) }
    }

    fun setAccentOption(option: AccentColorOption) {
        viewModelScope.launch { prefsRepo.setAccentOption(option) }
    }

    fun unlockAccentViaReward(option: AccentColorOption) {
        viewModelScope.launch { prefsRepo.unlockAccent(option.id) }
    }

    fun setDefaultSpeed(speed: Float) {
        viewModelScope.launch { prefsRepo.setDefaultSpeed(speed) }
    }

    fun setDefaultOrientation(orientation: OrientationPreference) {
        viewModelScope.launch { prefsRepo.setDefaultOrientation(orientation) }
    }

    fun setGesturesEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setGesturesEnabled(enabled) }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoPlayNext(enabled) }
    }

    fun setBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setBackgroundPlayback(enabled) }
    }

    fun setSubtitlePreferences(fontSizeSp: Float, colorArgb: Long, positionFraction: Float) {
        viewModelScope.launch { prefsRepo.setSubtitlePreferences(fontSizeSp, colorArgb, positionFraction) }
    }

    fun setShowHiddenFolders(show: Boolean) {
        viewModelScope.launch { prefsRepo.setShowHiddenFolders(show) }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch { prefsRepo.setAppLanguage(language) }
    }

    suspend fun resolveSingleUri(uri: Uri): VideoItem {
        return repository.scanner.resolveSingleUriToVideoItem(uri)
    }
}
