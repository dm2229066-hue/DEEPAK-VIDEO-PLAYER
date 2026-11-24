package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AdBannerBar
import com.example.data.local.NetworkStreamHistoryEntity
import com.example.data.model.VideoItem
import com.example.ui.components.GlassCardContainer
import com.example.ui.components.SectionTitleBar
import com.example.ui.components.StudioIconWell

@Composable
fun NetworkStreamScreen(
    recentStreams: List<NetworkStreamHistoryEntity>,
    canRequestAds: Boolean,
    onBack: () -> Unit,
    onPlayStream: (VideoItem) -> Unit,
    onSaveStream: (String, String) -> Unit,
    onDeleteStream: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun launchStream(rawUrl: String, rawTitle: String) {
        val trimmed = rawUrl.trim()
        val lower = trimmed.lowercase()
        val valid = lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("rtsp://")
        if (!valid) {
            validationError = "Enter a valid HTTP, HTTPS, or RTSP stream URL (MP4, HLS .m3u8, DASH .mpd)."
            return
        }
        validationError = null
        val displayTitle = rawTitle.trim().ifBlank {
            trimmed.substringAfterLast('/').substringBefore('?').ifBlank { "Network Stream" }
        }
        onSaveStream(trimmed, displayTitle)
        onPlayStream(
            VideoItem(
                id = "net_${trimmed.hashCode()}",
                uri = trimmed,
                title = displayTitle,
                displayName = displayTitle,
                folderName = "Network Stream",
                folderPath = "network",
                durationMs = 0L,
                sizeBytes = 0L,
                dateAddedSec = System.currentTimeMillis() / 1000L,
                dateModifiedSec = System.currentTimeMillis() / 1000L,
                mimeType = when {
                    lower.contains(".m3u8") -> "application/x-mpegURL"
                    lower.contains(".mpd") -> "application/dash+xml"
                    else -> "video/*"
                },
                width = 0,
                height = 0
            )
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.network_stream),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Direct HLS, DASH & RTSP Player",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GlassCardContainer(
                    cornerRadius = 22,
                    accentBorder = true
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StudioIconWell(
                                icon = Icons.Rounded.Wifi,
                                sizeDp = 38,
                                iconSizeDp = 19,
                                cornerRadius = 11
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "HTTP • HTTPS • HLS (.m3u8) • DASH (.mpd) • RTSP",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it; validationError = null },
                            label = { Text("Video / Stream URL") },
                            placeholder = { Text("https://example.com/stream.m3u8") },
                            leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                            singleLine = true,
                            isError = validationError != null,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("network_url_input")
                        )
                        if (validationError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = validationError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Stream Name (optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { launchStream(urlInput, titleInput) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("play_network_stream_button")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Stream")
                        }
                    }
                }
            }

            if (recentStreams.isNotEmpty()) {
                item {
                    SectionTitleBar(title = "Recent Stream URLs")
                }
                items(recentStreams, key = { it.url }) { stream ->
                    GlassCardContainer(
                        cornerRadius = 16,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { launchStream(stream.url, stream.title) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudioIconWell(
                                icon = Icons.Rounded.PlayArrow,
                                contentDescription = "Play stream",
                                sizeDp = 40,
                                iconSizeDp = 21,
                                cornerRadius = 12
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stream.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Text(
                                    stream.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onDeleteStream(stream.url) }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Remove stream URL",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
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
