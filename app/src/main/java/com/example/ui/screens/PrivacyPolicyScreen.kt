package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = stringResource(R.string.privacy_policy_title),
                style = MaterialTheme.typography.titleLarge
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PolicySection(
                    title = "1. Local Video Privacy",
                    body = "Deepak Video Player keeps all personal video files, thumbnails, folder names, playlists, and playback history strictly on your device. Local media files are never uploaded or transmitted to any external server."
                )
            }
            item {
                PolicySection(
                    title = "2. Permissions & Storage Access",
                    body = "Media permissions (READ_MEDIA_VIDEO / READ_EXTERNAL_STORAGE) are requested only when you choose to scan device storage. You may also browse individual files or mount external folders using Android's zero-permission Storage Access Framework (SAF). File rename and delete operations require explicit system or user confirmation."
                )
            }
            item {
                PolicySection(
                    title = "3. Google AdMob & Consent",
                    body = "This free application uses Google AdMob to display Banner, frequency-capped Interstitial, and optional Rewarded advertisements. Where required by regional regulations (such as EEA/UK/US privacy laws), Google's User Messaging Platform (UMP) presents a consent form before ads are requested. You can update your consent preferences anytime from Settings."
                )
            }
            item {
                PolicySection(
                    title = "4. Network Streams",
                    body = "When you enter an HTTP, HTTPS, HLS, DASH, or RTSP stream URL, ExoPlayer connects directly to the host you specified to stream media. Only your recently entered stream URLs are saved locally in your device database for convenience."
                )
            }
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
