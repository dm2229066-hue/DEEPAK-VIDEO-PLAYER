package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AdBannerBar
import com.example.data.model.VideoItem
import com.example.data.model.ViewMode
import com.example.ui.components.EmptyLibraryState
import com.example.ui.components.PermissionAndSafCard
import com.example.ui.components.VideoGridCard
import com.example.ui.components.VideoListRow
import com.example.ui.viewmodel.LibraryUiState
import com.example.ui.viewmodel.VideoFilter

@Composable
fun VideosScreen(
    uiState: LibraryUiState,
    canRequestAds: Boolean,
    onPlayVideo: (List<VideoItem>, Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (VideoFilter) -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenSortSheet: () -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onBrowseSaf: () -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onMoreVideoOptions: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.9f, 0f),
                        radius = size.width * 0.7f
                    ),
                    center = Offset(size.width * 0.9f, 0f),
                    radius = size.width * 0.7f
                )
            }
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.nav_videos),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${uiState.filteredVideos.size} items in library",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                VideosGlassButton(
                    icon = Icons.Rounded.FolderOpen,
                    contentDescription = "Pick video file via SAF",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onBrowseSaf,
                    modifier = Modifier.testTag("videos_saf_button")
                )
                VideosGlassButton(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = "Rescan videos",
                    onClick = onRefresh
                )
                VideosGlassButton(
                    icon = Icons.AutoMirrored.Rounded.Sort,
                    contentDescription = "Sort videos",
                    onClick = onOpenSortSheet
                )
                VideosGlassButton(
                    icon = if (uiState.preferences.viewMode == ViewMode.LIST) {
                        Icons.Rounded.GridView
                    } else {
                        Icons.Rounded.ViewList
                    },
                    contentDescription = "Toggle grid or list view",
                    onClick = onToggleViewMode,
                    modifier = Modifier.testTag("videos_view_toggle")
                )
            }
        }

        if (uiState.isScanning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.search_videos_hint)) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("videos_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                VideoFilter.ALL to "All",
                VideoFilter.CONTINUE to "Continue",
                VideoFilter.FAVORITES to "Favorites",
                VideoFilter.RECENT to "Recent"
            )
            items(filters) { (filter, label) ->
                val selected = uiState.activeFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(label) },
                    shape = CircleShape,
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!uiState.hasStoragePermission && uiState.filteredVideos.isEmpty()) {
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    item {
                        PermissionAndSafCard(
                            hasPermission = false,
                            onRequestPermission = onRequestPermission,
                            onBrowseSaf = onBrowseSaf
                        )
                    }
                    item {
                        EmptyLibraryState(
                            title = "Discover Your Videos",
                            subtitle = "Grant storage access to automatically list all videos, or pick files securely with SAF."
                        )
                    }
                }
            }
        } else if (uiState.filteredVideos.isEmpty()) {
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                EmptyLibraryState(
                    title = "No matching videos",
                    subtitle = "Try clearing your search or mounting an external folder.",
                    actionLabel = "Browse Files",
                    onAction = onBrowseSaf
                )
            }
        } else if (uiState.preferences.viewMode == ViewMode.GRID) {
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val columns = when {
                    maxWidth >= 840.dp -> 4
                    maxWidth >= 560.dp -> 3
                    else -> 2
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(uiState.filteredVideos, key = { _, v -> v.id }) { idx, video ->
                        VideoGridCard(
                            video = video,
                            onClick = { onPlayVideo(uiState.filteredVideos, idx) },
                            onMoreClick = { onMoreVideoOptions(video) }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(uiState.filteredVideos, key = { _, v -> v.id }) { idx, video ->
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
private fun VideosGlassButton(
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

