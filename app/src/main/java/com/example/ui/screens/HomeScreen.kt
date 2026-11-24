package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.components.GlassCardContainer
import com.example.ui.components.PermissionAndSafCard
import com.example.ui.components.SectionTitleBar
import com.example.ui.components.StudioIconWell
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
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.85f, 0f),
                        radius = size.width * 0.8f
                    ),
                    center = Offset(size.width * 0.85f, 0f),
                    radius = size.width * 0.8f
                )
            }
    ) {
        // Redesigned Studio Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeepakLogoMark(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                HeaderGlassButton(
                    icon = if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = "Search videos",
                    onClick = { showSearch = !showSearch },
                    modifier = Modifier.testTag("home_search_toggle")
                )
                HeaderGlassButton(
                    icon = Icons.Rounded.Link,
                    contentDescription = "Network stream URL",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onOpenNetworkStream,
                    modifier = Modifier.testTag("home_network_stream_button")
                )
                HeaderGlassButton(
                    icon = Icons.AutoMirrored.Rounded.Sort,
                    contentDescription = "Sort videos",
                    onClick = onOpenSortSheet
                )
                HeaderGlassButton(
                    icon = if (uiState.preferences.viewMode == ViewMode.LIST) {
                        Icons.Rounded.GridView
                    } else {
                        Icons.Rounded.ViewList
                    },
                    contentDescription = "Toggle grid or list view",
                    onClick = onToggleViewMode
                )
            }
        }

        if (showSearch) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_videos_hint)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Spotlight Hero Card
            item {
                CinemaSpotlightCard(
                    totalVideos = uiState.allVideos.size,
                    folderCount = uiState.folders.size,
                    favCount = uiState.favoriteVideos.size,
                    resumeVideo = uiState.continueWatching.firstOrNull() ?: uiState.allVideos.firstOrNull(),
                    onQuickPlay = { video ->
                        val idx = uiState.allVideos.indexOfFirst { it.uri == video.uri }.coerceAtLeast(0)
                        onPlayVideo(uiState.allVideos, idx)
                    },
                    onOpenStream = onOpenNetworkStream,
                    onRefresh = onRefresh
                )
            }

            // 4 Quick Action Tiles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Rounded.Link,
                        label = "Stream URL",
                        onClick = onOpenNetworkStream,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Rounded.FolderOpen,
                        label = "Pick File",
                        onClick = onBrowseSaf,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Rounded.Favorite,
                        label = "Favorites",
                        onClick = onSeeAllVideos,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Rounded.Folder,
                        label = "Folders",
                        onClick = {
                            uiState.folders.firstOrNull()?.let(onOpenFolder) ?: onSeeAllVideos()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
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
                    SectionTitleBar(title = stringResource(R.string.continue_watching))
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(uiState.continueWatching, key = { _, v -> "cw_${v.id}" }) { idx, video ->
                            ContinueWatchingBannerCard(
                                video = video,
                                onClick = { onPlayVideo(uiState.continueWatching, idx) }
                            )
                        }
                    }
                }
            }

            // Recently Played
            if (uiState.recentlyPlayed.isNotEmpty()) {
                item {
                    SectionTitleBar(title = stringResource(R.string.recently_played))
                    Spacer(modifier = Modifier.height(12.dp))
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
                    SectionTitleBar(title = stringResource(R.string.recently_added))
                    Spacer(modifier = Modifier.height(12.dp))
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
                    SectionTitleBar(title = stringResource(R.string.favorites))
                    Spacer(modifier = Modifier.height(12.dp))
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
                    SectionTitleBar(title = stringResource(R.string.nav_folders))
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.folders.take(10), key = { it.path }) { folder ->
                            GlassCardContainer(
                                cornerRadius = 16,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onOpenFolder(folder) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StudioIconWell(
                                        icon = Icons.Rounded.Folder,
                                        sizeDp = 36,
                                        iconSizeDp = 18,
                                        cornerRadius = 11
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
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
                    SectionTitleBar(title = stringResource(R.string.nav_playlists))
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.playlists, key = { it.playlist.id }) { pl ->
                            GlassCardContainer(
                                cornerRadius = 16,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onOpenPlaylist(pl.playlist.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StudioIconWell(
                                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                        sizeDp = 36,
                                        iconSizeDp = 18,
                                        cornerRadius = 11
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
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
                SectionTitleBar(
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
private fun HeaderGlassButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    ),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCardContainer(
        cornerRadius = 16,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StudioIconWell(
                icon = icon,
                contentDescription = label,
                sizeDp = 38,
                iconSizeDp = 19,
                cornerRadius = 12
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CinemaSpotlightCard(
    totalVideos: Int,
    folderCount: Int,
    favCount: Int,
    resumeVideo: VideoItem?,
    onQuickPlay: (VideoItem) -> Unit,
    onOpenStream: () -> Unit,
    onRefresh: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_brand_hero),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xF008080B),
                            Color(0xBB101016),
                            Color(0x66161408)
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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        shape = CircleShape,
                        modifier = Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            CircleShape
                        )
                    ) {
                        Text(
                            text = "MEDIA3 STUDIO ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (resumeVideo != null) resumeVideo.title else "Your Cinema Vault",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalVideos Videos  •  $folderCount Folders  •  $favCount Favorites",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh library",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (resumeVideo != null) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onQuickPlay(resumeVideo) }
                            .testTag("hero_quick_play"),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (resumeVideo.lastPositionMs > 2000L) "Resume Playback" else "Play Now",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenStream)
                        .testTag("hero_stream_url"),
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Link,
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

@Composable
private fun ContinueWatchingBannerCard(
    video: VideoItem,
    onClick: () -> Unit
) {
    GlassCardContainer(
        cornerRadius = 18,
        accentBorder = true,
        modifier = Modifier
            .width(216.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            VideoThumbnailBox(
                video = video,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9.5f),
                showPlayBadge = true
            )
            Column(modifier = Modifier.padding(11.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Left off ${FormatUtils.formatDuration(video.lastPositionMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
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
    GlassCardContainer(
        cornerRadius = 16,
        modifier = Modifier
            .width(172.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            VideoThumbnailBox(
                video = video,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
