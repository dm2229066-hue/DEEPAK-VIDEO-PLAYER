package com.example.data.repository

import android.content.IntentSender
import android.net.Uri
import com.example.data.local.AppDao
import com.example.data.local.FavoriteVideoEntity
import com.example.data.local.HiddenFolderEntity
import com.example.data.local.NetworkStreamHistoryEntity
import com.example.data.local.PlaybackHistoryEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistVideoCrossRef
import com.example.data.local.PlaylistWithItems
import com.example.data.local.SafTreeEntity
import com.example.data.media.DeviceVideoScanner
import com.example.data.model.SortOption
import com.example.data.model.VideoFolder
import com.example.data.model.VideoItem
import com.example.data.model.VideoMetadataInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class VideoLibraryRepository(
    private val dao: AppDao,
    val scanner: DeviceVideoScanner
) {
    private val _scannedVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val scannedVideos: StateFlow<List<VideoItem>> = _scannedVideos.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val playbackHistory: Flow<List<PlaybackHistoryEntity>> = dao.observePlaybackHistory()
    val favorites: Flow<List<FavoriteVideoEntity>> = dao.observeFavorites()
    val hiddenFolders: Flow<List<HiddenFolderEntity>> = dao.observeHiddenFolders()
    val safTrees: Flow<List<SafTreeEntity>> = dao.observeSafTrees()
    val playlists: Flow<List<PlaylistWithItems>> = dao.observePlaylistsWithItems()
    val networkStreams: Flow<List<NetworkStreamHistoryEntity>> = dao.observeNetworkStreams()

    val enrichedVideos: Flow<List<VideoItem>> = combine(
        _scannedVideos,
        playbackHistory,
        favorites
    ) { videos, historyList, favList ->
        val historyMap = historyList.associateBy { it.uri }
        val favSet = favList.map { it.uri }.toHashSet()
        videos.map { video ->
            val hist = historyMap[video.uri]
            video.copy(
                isFavorite = video.uri in favSet,
                lastPositionMs = hist?.lastPositionMs ?: 0L,
                lastPlayedAt = hist?.lastPlayedAt ?: 0L
            )
        }
    }

    suspend fun refreshLibrary() {
        _isScanning.value = true
        try {
            val mediaStoreList = scanner.scanMediaStoreVideos()
            val trees = dao.observeSafTrees().first()
            val safVideos = trees.flatMap { tree ->
                scanner.scanSafTree(tree.treeUri, tree.displayName)
            }
            val merged = (mediaStoreList + safVideos).distinctBy { it.uri }
            _scannedVideos.value = merged
        } finally {
            _isScanning.value = false
        }
    }

    fun buildFolders(
        videos: List<VideoItem>,
        hiddenPaths: Set<String>,
        safTreeUris: Set<String>,
        includeHidden: Boolean
    ): List<VideoFolder> {
        return videos
            .groupBy { it.folderPath }
            .mapNotNull { (path, group) ->
                val isHidden = path in hiddenPaths
                if (!includeHidden && isHidden) return@mapNotNull null
                val first = group.first()
                VideoFolder(
                    path = path,
                    name = first.folderName,
                    videoCount = group.size,
                    totalSizeBytes = group.sumOf { it.sizeBytes },
                    latestModifiedSec = group.maxOfOrNull { it.dateModifiedSec } ?: 0L,
                    isHidden = isHidden,
                    isSafTree = path in safTreeUris || first.isSafUri
                )
            }
            .sortedByDescending { it.latestModifiedSec }
    }

    fun sortVideos(
        videos: List<VideoItem>,
        sortOption: SortOption,
        ascending: Boolean
    ): List<VideoItem> {
        val comparator: Comparator<VideoItem> = when (sortOption) {
            SortOption.NAME -> compareBy { it.title.lowercase() }
            SortOption.DATE -> compareBy { it.dateModifiedSec }
            SortOption.SIZE -> compareBy { it.sizeBytes }
            SortOption.DURATION -> compareBy { it.durationMs }
        }
        return if (ascending) videos.sortedWith(comparator)
        else videos.sortedWith(comparator.reversed())
    }

    suspend fun toggleFavorite(video: VideoItem) {
        if (video.isFavorite) {
            dao.removeFavorite(video.uri)
        } else {
            dao.addFavorite(
                FavoriteVideoEntity(
                    uri = video.uri,
                    title = video.title,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun toggleHideFolder(folderPath: String, folderName: String, currentlyHidden: Boolean) {
        if (currentlyHidden) {
            dao.unhideFolder(folderPath)
        } else {
            dao.hideFolder(
                HiddenFolderEntity(
                    folderPath = folderPath,
                    folderName = folderName,
                    hiddenAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun addSafTree(treeUri: String, displayName: String) {
        dao.insertSafTree(
            SafTreeEntity(
                treeUri = treeUri,
                displayName = displayName,
                addedAt = System.currentTimeMillis()
            )
        )
        refreshLibrary()
    }

    suspend fun removeSafTree(treeUri: String) {
        dao.removeSafTree(treeUri)
        refreshLibrary()
    }

    suspend fun createPlaylist(name: String): Long {
        return dao.insertPlaylist(
            PlaylistEntity(name = name.trim(), createdAt = System.currentTimeMillis())
        )
    }

    suspend fun renamePlaylist(id: Long, newName: String) {
        dao.renamePlaylist(id, newName.trim())
    }

    suspend fun deletePlaylist(id: Long) {
        dao.deletePlaylist(id)
    }

    suspend fun addVideoToPlaylist(playlistId: Long, video: VideoItem) {
        dao.addVideoToPlaylist(
            PlaylistVideoCrossRef(
                playlistId = playlistId,
                videoUri = video.uri,
                videoTitle = video.title,
                durationMs = video.durationMs,
                sizeBytes = video.sizeBytes,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeVideoFromPlaylist(playlistId: Long, videoUri: String) {
        dao.removeVideoFromPlaylist(playlistId, videoUri)
    }

    suspend fun savePlaybackProgress(
        uri: String,
        title: String,
        durationMs: Long,
        positionMs: Long,
        subtitleOffsetMs: Long = 0L,
        audioOffsetMs: Long = 0L,
        preferredSpeed: Float = 1.0f,
        externalSubtitleUri: String? = null
    ) {
        val existing = dao.getHistoryForUri(uri)
        dao.upsertHistory(
            PlaybackHistoryEntity(
                uri = uri,
                title = title,
                durationMs = durationMs.coerceAtLeast(existing?.durationMs ?: 0L),
                lastPositionMs = positionMs,
                lastPlayedAt = System.currentTimeMillis(),
                playCount = (existing?.playCount ?: 0) + if (existing == null) 1 else 0,
                subtitleOffsetMs = subtitleOffsetMs,
                audioOffsetMs = audioOffsetMs,
                preferredSpeed = preferredSpeed,
                externalSubtitleUri = externalSubtitleUri ?: existing?.externalSubtitleUri
            )
        )
    }

    suspend fun getHistoryForUri(uri: String): PlaybackHistoryEntity? = dao.getHistoryForUri(uri)

    suspend fun clearPlaybackHistory() = dao.clearPlaybackHistory()

    suspend fun removeHistoryEntry(uri: String) = dao.deleteHistoryForUri(uri)

    suspend fun saveNetworkStream(url: String, title: String) {
        dao.upsertNetworkStream(
            NetworkStreamHistoryEntity(
                url = url,
                title = title.ifBlank { url.substringAfterLast('/').ifBlank { "Network Stream" } },
                lastPlayedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteNetworkStream(url: String) = dao.deleteNetworkStream(url)

    suspend fun inspectMetadata(video: VideoItem): VideoMetadataInfo = scanner.inspectMetadata(video)

    fun createDeleteSender(uri: Uri): IntentSender? = scanner.createDeleteIntentSender(uri)

    fun createWriteSender(uri: Uri): IntentSender? = scanner.createWriteIntentSender(uri)

    suspend fun deleteDirect(uri: Uri): Boolean {
        val deleted = scanner.deleteDirect(uri)
        if (deleted) {
            dao.deleteHistoryForUri(uri.toString())
            dao.removeFavorite(uri.toString())
            refreshLibrary()
        }
        return deleted
    }

    suspend fun renameDirect(uri: Uri, newName: String): Boolean {
        val renamed = scanner.renameDirect(uri, newName)
        if (renamed) refreshLibrary()
        return renamed
    }
}
