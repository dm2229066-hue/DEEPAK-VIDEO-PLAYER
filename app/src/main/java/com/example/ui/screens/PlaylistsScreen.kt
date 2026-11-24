package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AdBannerBar
import com.example.data.local.PlaylistWithItems
import com.example.data.model.FormatUtils
import com.example.data.model.VideoItem
import com.example.ui.components.EmptyLibraryState
import com.example.ui.components.GlassCardContainer
import com.example.ui.components.StudioIconWell
import com.example.ui.viewmodel.LibraryUiState

@Composable
fun PlaylistsScreen(
    uiState: LibraryUiState,
    selectedPlaylistId: Long?,
    canRequestAds: Boolean,
    onSelectPlaylist: (Long?) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onRemoveVideoFromPlaylist: (Long, String) -> Unit,
    onPlayQueue: (List<VideoItem>, Int) -> Unit,
    onOpenNetworkStream: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PlaylistWithItems?>(null) }

    val activePlaylist = uiState.playlists.firstOrNull { it.playlist.id == selectedPlaylistId }

    if (activePlaylist != null) {
        BackHandler { onSelectPlaylist(null) }
        val videoLookup = uiState.allVideos.associateBy { it.uri }
        val resolvedItems = activePlaylist.items.map { crossRef ->
            videoLookup[crossRef.videoUri] ?: VideoItem(
                id = "pl_${crossRef.videoUri}",
                uri = crossRef.videoUri,
                title = crossRef.videoTitle,
                displayName = crossRef.videoTitle,
                folderName = activePlaylist.playlist.name,
                folderPath = "",
                durationMs = crossRef.durationMs,
                sizeBytes = crossRef.sizeBytes,
                dateAddedSec = crossRef.addedAt / 1000L,
                dateModifiedSec = crossRef.addedAt / 1000L,
                mimeType = "video/*",
                width = 0,
                height = 0
            )
        }

        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSelectPlaylist(null) }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to playlists")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activePlaylist.playlist.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${resolvedItems.size} videos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (resolvedItems.isNotEmpty()) {
                    Button(
                        onClick = { onPlayQueue(resolvedItems, 0) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All")
                    }
                }
            }

            if (resolvedItems.isEmpty()) {
                EmptyLibraryState(
                    title = "Playlist is empty",
                    subtitle = "Open the options menu on any video to add it to ${activePlaylist.playlist.name}.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(resolvedItems, key = { _, v -> v.uri }) { idx, video ->
                        GlassCardContainer(
                            cornerRadius = 16,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onPlayQueue(resolvedItems, idx) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudioIconWell(
                                    icon = Icons.Rounded.PlayArrow,
                                    sizeDp = 38,
                                    iconSizeDp = 20,
                                    cornerRadius = 11
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(video.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Text(
                                        "${FormatUtils.formatDuration(video.durationMs)} • ${FormatUtils.formatFileSize(video.sizeBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onRemoveVideoFromPlaylist(activePlaylist.playlist.id, video.uri) }) {
                                    Icon(
                                        Icons.Rounded.RemoveCircleOutline,
                                        contentDescription = "Remove from playlist",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AdBannerBar(canRequestAds = canRequestAds)
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.nav_playlists),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${uiState.playlists.size} collections",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onOpenNetworkStream,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stream")
            }
            Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("create_playlist_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New")
            }
        }

        if (uiState.playlists.isEmpty()) {
            EmptyLibraryState(
                title = "Create Custom Playlists",
                subtitle = "Organize movies, series episodes, or study clips into sequential queues.",
                actionLabel = stringResource(R.string.create_playlist),
                onAction = { showCreateDialog = true },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.playlists, key = { it.playlist.id }) { pl ->
                    GlassCardContainer(
                        cornerRadius = 18,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectPlaylist(pl.playlist.id) }
                            .testTag("playlist_card_${pl.playlist.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudioIconWell(
                                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                sizeDp = 46,
                                iconSizeDp = 24,
                                cornerRadius = 14
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    pl.playlist.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    "${pl.items.size} videos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { renameTarget = pl }) {
                                Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = "Rename playlist")
                            }
                            IconButton(onClick = { onDeletePlaylist(pl.playlist.id) }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Delete playlist",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        AdBannerBar(canRequestAds = canRequestAds)
    }

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = "New Playlist",
            initialValue = "",
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = {
                onCreatePlaylist(it)
                showCreateDialog = false
            }
        )
    }

    renameTarget?.let { target ->
        PlaylistNameDialog(
            title = "Rename Playlist",
            initialValue = target.playlist.name,
            confirmLabel = "Save",
            onDismiss = { renameTarget = null },
            onConfirm = {
                onRenamePlaylist(target.playlist.id, it)
                renameTarget = null
            }
        )
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Playlist Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
