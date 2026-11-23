package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Column(modifier = modifier.fillMaxSize()) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.nav_videos)} (${uiState.filteredVideos.size})",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onBrowseSaf, modifier = Modifier.testTag("videos_saf_button")) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Pick video file via SAF")
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Rescan videos")
            }
            IconButton(onClick = onOpenSortSheet) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort videos")
            }
            IconButton(onClick = onToggleViewMode, modifier = Modifier.testTag("videos_view_toggle")) {
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
                .padding(horizontal = 16.dp)
                .testTag("videos_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                    colors = FilterChipDefaults.filterChipColors(
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
