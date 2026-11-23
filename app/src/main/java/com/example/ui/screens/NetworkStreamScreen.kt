package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AdBannerBar
import com.example.data.local.NetworkStreamHistoryEntity
import com.example.data.model.VideoItem

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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = stringResource(R.string.network_stream),
                style = MaterialTheme.typography.titleLarge
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HTTP • HTTPS • HLS (.m3u8) • DASH (.mpd) • RTSP",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it; validationError = null },
                            label = { Text("Video / Stream URL") },
                            placeholder = { Text("https://example.com/stream.m3u8") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            singleLine = true,
                            isError = validationError != null,
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
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { launchStream(urlInput, titleInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("play_network_stream_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Stream")
                        }
                    }
                }
            }

            if (recentStreams.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Stream URLs",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(recentStreams, key = { it.url }) { stream ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { launchStream(stream.url, stream.title) },
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play stream",
                                tint = MaterialTheme.colorScheme.primary
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
                                    Icons.Default.Delete,
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
