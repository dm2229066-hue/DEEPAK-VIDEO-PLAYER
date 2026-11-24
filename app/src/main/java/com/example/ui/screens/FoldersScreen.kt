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
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.FormatUtils
import com.example.data.model.VideoFolder
import com.example.data.model.VideoItem
import com.example.ui.components.EmptyLibraryState
import com.example.ui.components.GlassCardContainer
import com.example.ui.components.StudioIconWell
import com.example.ui.components.VideoListRow
import com.example.ui.viewmodel.LibraryUiState

@Composable
fun FoldersScreen(
    uiState: LibraryUiState,
    selectedFolderPath: String?,
    canRequestAds: Boolean,
    onSelectFolder: (VideoFolder?) -> Unit,
    onMountSafFolder: () -> Unit,
    onToggleShowHidden: (Boolean) -> Unit,
    onToggleHideFolder: (VideoFolder) -> Unit,
    onPlayVideo: (List<VideoItem>, Int) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onMoreVideoOptions: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeFolder = uiState.folders.firstOrNull { it.path == selectedFolderPath }

    if (activeFolder != null) {
        BackHandler { onSelectFolder(null) }
        val folderVideos = uiState.allVideos.filter { it.folderPath == activeFolder.path }
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSelectFolder(null) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to folders"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeFolder.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folderVideos.size} videos • ${FormatUtils.formatFileSize(activeFolder.totalSizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onToggleHideFolder(activeFolder) }) {
                    Icon(
                        imageVector = if (activeFolder.isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = if (activeFolder.isHidden) "Unhide folder" else "Hide folder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (folderVideos.isEmpty()) {
                EmptyLibraryState(
                    title = "Folder is empty",
                    subtitle = "No playable video files found inside ${activeFolder.name}.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(folderVideos, key = { _, v -> v.id }) { idx, video ->
                        VideoListRow(
                            video = video,
                            onClick = { onPlayVideo(folderVideos, idx) },
                            onMoreClick = { onMoreVideoOptions(video) },
                            onFavoriteClick = { onToggleFavorite(video) }
                        )
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
                    text = stringResource(R.string.nav_folders),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${uiState.folders.size} directories • ${FormatUtils.formatFileSize(uiState.folders.sumOf { it.totalSizeBytes })}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { onToggleShowHidden(!uiState.preferences.showHiddenFolders) },
                modifier = Modifier.testTag("toggle_hidden_folders_button")
            ) {
                Icon(
                    imageVector = if (uiState.preferences.showHiddenFolders) {
                        Icons.Rounded.Visibility
                    } else {
                        Icons.Rounded.VisibilityOff
                    },
                    contentDescription = "Toggle hidden folders visibility",
                    tint = if (uiState.preferences.showHiddenFolders) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Button(
                onClick = onMountSafFolder,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("mount_saf_folder_button")
            ) {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mount")
            }
        }

        if (uiState.folders.isEmpty()) {
            EmptyLibraryState(
                title = "No Video Folders Yet",
                subtitle = "Mount an internal or external SD/USB folder via Android Storage Access Framework.",
                actionLabel = stringResource(R.string.mount_external_folder),
                onAction = onMountSafFolder,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.folders, key = { it.path }) { folder ->
                    GlassCardContainer(
                        cornerRadius = 18,
                        accentBorder = folder.isSafTree,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectFolder(folder) }
                            .testTag("folder_card_${folder.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudioIconWell(
                                icon = if (folder.isSafTree) Icons.Rounded.SdStorage else Icons.Rounded.Folder,
                                sizeDp = 46,
                                iconSizeDp = 24,
                                cornerRadius = 14,
                                highlighted = !folder.isHidden
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (folder.isHidden) "${folder.name} (Hidden)" else folder.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${folder.videoCount} videos • ${FormatUtils.formatFileSize(folder.totalSizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onToggleHideFolder(folder) }) {
                                Icon(
                                    imageVector = if (folder.isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = if (folder.isHidden) "Unhide folder" else "Hide folder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        AdBannerBar(canRequestAds = canRequestAds)
    }
}
