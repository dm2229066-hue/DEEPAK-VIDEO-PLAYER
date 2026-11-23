package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AdBannerBar
import com.example.data.model.FormatUtils
import com.example.data.model.VideoFolder
import com.example.data.model.VideoItem
import com.example.data.model.ViewMode
import com.example.ui.components.DeepakLogoMark
import com.example.ui.components.EmptyLibraryState
import com.example.ui.components.PermissionAndSafCard
import com.example.ui.components.VideoGridCard
import com.example.ui.components.VideoListRow
import com.example.ui.components.VideoThumbnailBox
import com.example.ui.viewmodel.LibraryUiState

@Composable
fun HomeScreen(
    uiState: LibraryUiState,
    canRequestAds: Boolean,
    onPlayVideo: (List<VideoItem>, Int) -> Unit,
    onOpenNetworkStream: () -> Unit,
    onOpenFolder: (VideoFolder) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onSeeAllVideos: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenSortSheet: () -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onBrowseSaf: () -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onMoreVideoOptions: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearch by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeepakLogoMark(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { showSearch = !showSearch },
                modifier = Modifier.testTag("home_search_toggle")
            ) {
                Icon(
                    imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search videos"
                )
            }
            IconButton(
                onClick = onOpenNetworkStream,
                modifier = Modifier.testTag("home_network_stream_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "Network stream URL",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onOpenSortSheet) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort videos")
            }
            IconButton(onClick = onToggleViewMode) {
                Icon(
                    imageVector = if (uiState.preferences.viewMode == ViewMode.LIST) {
                        Icons.Default.GridView
                    } else {
                        Icons.Default.ViewList
                    },
                    contentDescription = "Toggle grid or list view"
                )
            }
        }

        if (showSearch) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_videos_hint)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("home_search_input")
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Cinema Hero Card
            item {
                HeroDashboardCard(
                    totalVideos = uiState.allVideos.size,
                    folderCount = uiState.folders.size,
                    resumeVideo = uiState.continueWatching.firstOrNull() ?: uiState.allVideos.firstOrNull(),
                    onQuickPlay = { video ->
                        val idx = uiState.allVideos.indexOfFirst { it.uri == video.uri }.coerceAtLeast(0)
                        onPlayVideo(uiState.allVideos, idx)
                    },
                    onOpenStream = onOpenNetworkStream,
                    onRefresh = onRefresh
                )
            }

            if (!uiState.hasStoragePermission) {
                item {
                    PermissionAndSafCard(
                        hasPermission = false,
                        onRequestPermission = onRequestPermission,
                        onBrowseSaf = onBrowseSaf
                    )
                }
            }

            // Continue Watching
            if (uiState.continueWatching.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.continue_watching))
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(uiState.continueWatching, key = { _, v -> "cw_${v.id}" }) { idx, video ->
                            HorizontalVideoCard(
                                video = video,
                                subtitle = "Left off ${FormatUtils.formatDuration(video.lastPositionMs)}",
                                onClick = { onPlayVideo(uiState.continueWatching, idx) }
                            )
                        }
                    }
                }
            }

            // Recently Played
            if (uiState.recentlyPlayed.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.recently_played))
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(uiState.recentlyPlayed, key = { _, v -> "rp_${v.id}" }) { idx, video ->
                            HorizontalVideoCard(
                                video = video,
                                subtitle = video.folderName,
                                onClick = { onPlayVideo(uiState.recentlyPlayed, idx) }
                            )
                        }
                    }
                }
            }

            // Recently Added
            if (uiState.recentlyAdded.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.recently_added))
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(uiState.recentlyAdded, key = { _, v -> "ra_${v.id}" }) { idx, video ->
                            HorizontalVideoCard(
                                video = video,
                                subtitle = FormatUtils.formatFileSize(video.sizeBytes),
                                onClick = { onPlayVideo(uiState.recentlyAdded, idx) }
                            )
                        }
                    }
                }
            }

            // Favorite Videos
            if (uiState.favoriteVideos.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.favorites))
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(uiState.favoriteVideos, key = { _, v -> "fav_${v.id}" }) { idx, video ->
                            HorizontalVideoCard(
                                video = video,
                                subtitle = FormatUtils.formatDuration(video.durationMs),
                                onClick = { onPlayVideo(uiState.favoriteVideos, idx) }
                            )
                        }
                    }
                }
            }

            // Folders quick strip
            if (uiState.folders.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.nav_folders))
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.folders.take(10), key = { it.path }) { folder ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenFolder(folder) },
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = folder.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${folder.videoCount} videos",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Playlists quick strip
            if (uiState.playlists.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.nav_playlists))
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.playlists, key = { it.playlist.id }) { pl ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenPlaylist(pl.playlist.id) },
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(pl.playlist.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${pl.items.size} items",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // All Videos Preview
            item {
                SectionHeader(
                    title = stringResource(R.string.all_videos),
                    actionLabel = if (uiState.allVideos.size > 6) "See All (${uiState.allVideos.size})" else null,
                    onAction = onSeeAllVideos
                )
            }

            val previewList = uiState.filteredVideos.take(12)
            if (previewList.isEmpty()) {
                item {
                    EmptyLibraryState(
                        title = "No local videos found yet",
                        subtitle = "Scan device storage, browse with Files (SAF), or stream an HTTP/HLS/DASH URL.",
                        actionLabel = "Open Network Stream",
                        onAction = onOpenNetworkStream
                    )
                }
            } else if (uiState.preferences.viewMode == ViewMode.GRID) {
                items(previewList.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { video ->
                            val idx = uiState.filteredVideos.indexOfFirst { it.uri == video.uri }.coerceAtLeast(0)
                            VideoGridCard(
                                video = video,
                                onClick = { onPlayVideo(uiState.filteredVideos, idx) },
                                onMoreClick = { onMoreVideoOptions(video) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            } else {
                itemsIndexed(previewList, key = { _, v -> "home_${v.id}" }) { idx, video ->
                    VideoListRow(
                        video = video,
                        onClick = { onPlayVideo(uiState.filteredVideos, idx) },
                        onMoreClick = { onMoreVideoOptions(video) },
                        onFavoriteClick = { onToggleFavorite(video) }
                    )
                }
            }
        }

        AdBannerBar(canRequestAds = canRequestAds)
    }
}

@Composable
private fun HeroDashboardCard(
    totalVideos: Int,
    folderCount: Int,
    resumeVideo: VideoItem?,
    onQuickPlay: (VideoItem) -> Unit,
    onOpenStream: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_brand_hero),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.88f),
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "YOUR CINEMA VAULT",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalVideos Videos • $folderCount Folders",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh library",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (resumeVideo != null) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onQuickPlay(resumeVideo) }
                                .testTag("hero_quick_play"),
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (resumeVideo.lastPositionMs > 2000L) "Resume Last" else "Play Latest",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenStream)
                            .testTag("hero_stream_url"),
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Stream URL",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun HorizontalVideoCard(
    video: VideoItem,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(176.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        VideoThumbnailBox(
            video = video,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
