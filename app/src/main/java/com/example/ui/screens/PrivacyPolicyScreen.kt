package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.AdManager
import com.example.ui.components.GlassCardContainer
import com.example.ui.components.StudioIconWell

private const val POLICY_EFFECTIVE_DATE = "November 28, 2026"

private data class PolicySectionData(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val body: String
)

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val realAdsConfigured = remember { AdManager.areRealAdsConfigured() }
    val sections = remember(realAdsConfigured) { buildPolicySections(realAdsConfigured) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(onClick = onBack)
                    .testTag("privacy_policy_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.privacy_policy_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Deepak Video Player • Local-First Privacy",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    val fullText = buildPlainTextPolicy(sections)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Deepak Video Player Privacy Policy", fullText))
                    Toast.makeText(context, "Privacy policy copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("privacy_policy_copy_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy privacy policy",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "overview_card") {
                PolicyOverviewCard()
            }
            items(sections, key = { it.id }) { section ->
                PolicySectionCard(section = section)
            }
        }
    }
}

@Composable
private fun PolicyOverviewCard() {
    GlassCardContainer(
        cornerRadius = 20,
        accentBorder = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudioIconWell(
                    icon = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    sizeDp = 46,
                    iconSizeDp = 24,
                    cornerRadius = 14
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Media Stays on Your Device",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.border(
                            0.75.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            CircleShape
                        )
                    ) {
                        Text(
                            text = "Effective: $POLICY_EFFECTIVE_DATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Deepak Video Player is built as an offline-capable, local-first media player. We do not require account sign-in, collect personal profiles, or upload your videos, folders, or watch history to external servers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PolicySectionCard(section: PolicySectionData) {
    GlassCardContainer(
        cornerRadius = 18,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudioIconWell(
                    icon = section.icon,
                    contentDescription = null,
                    sizeDp = 36,
                    iconSizeDp = 19,
                    cornerRadius = 10
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = section.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildPolicySections(realAdsConfigured: Boolean): List<PolicySectionData> {
    val adsSectionBody = if (realAdsConfigured) {
        "This build uses Google AdMob (Google Mobile Ads SDK) to show Banner ads, frequency-capped Interstitial ads between player sessions, and optional Rewarded ads. Google AdMob and its partners may collect device identifiers, IP address, diagnostics, and ad interaction data in accordance with Google's Privacy Policy (https://policies.google.com/privacy).\n\nWhere required by regional regulations, Google's User Messaging Platform (UMP) presents a consent dialog before requesting ads. You can review or update your consent preferences anytime from Settings > Ad Privacy & Consent Options."
    } else {
        "Production advertisements are currently disabled in this build (test ad IDs are blocked at runtime). No ad SDK requests are sent while production AdMob IDs are not configured.\n\nIf a future release enables Google AdMob, Google's User Messaging Platform (UMP) will gather required user consent before loading ads, and consent options will appear in Settings."
    }

    return listOf(
        PolicySectionData(
            id = "local_data",
            title = "1. Information Stored Locally on Your Device",
            icon = Icons.Rounded.Storage,
            body = "To provide resume playback and library organization, the app stores only local operational records on your device:\n" +
                "• Playback History: Video URI, title, last played position, playback speed, audio/subtitle delay, and selected external subtitle URI (stored in the local Room database).\n" +
                "• Library Organization: Your favorites, custom playlists, mounted SAF folder URIs, and recently entered network stream URLs.\n" +
                "• App Preferences: Theme, accent color, default speed, seek/gesture settings, subtitle styling, hidden-folder toggle, and app language (stored in Jetpack DataStore).\n" +
                "• Temporary Subtitle Cache: When you adjust subtitle timing offset, a temporary shifted copy is written to the app's private cache directory.\n\n" +
                "You can erase saved resume positions and watch history anytime via Settings > Clear Playback History, or remove all local app data by uninstalling the app."
        ),
        PolicySectionData(
            id = "permissions",
            title = "2. Permissions & Storage Access",
            icon = Icons.Rounded.Security,
            body = "Deepak Video Player follows least-privilege access:\n" +
                "• Video Media Access (READ_MEDIA_VIDEO on Android 13+ / READ_EXTERNAL_STORAGE on Android 12 and below): Requested only when you tap Scan Storage to list videos on your device. We never request photo, audio-only, or broad all-files storage permissions.\n" +
                "• Zero-Permission Storage Access Framework (SAF): You can open individual videos, select subtitle files, or mount external SD/USB folders without granting storage permissions.\n" +
                "• Background Playback & Notifications (FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, POST_NOTIFICATIONS): Used only to display lock-screen and notification media controls when background playback is active.\n" +
                "• Network & Wake Lock (INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK): Used to keep the screen awake during playback, stream user-entered URLs, and load consent/ads when enabled."
        ),
        PolicySectionData(
            id = "file_actions",
            title = "3. File Editing, Sharing & System Controls",
            icon = Icons.Rounded.FolderShared,
            body = "Renaming or deleting a video from device storage triggers Android's Scoped Storage confirmation dialog (or uses the SAF folder access you explicitly granted). Sharing a video uses Android's standard Share Sheet and transmits media only to the destination app you choose."
        ),
        PolicySectionData(
            id = "network_streams",
            title = "4. Network Video Streams",
            icon = Icons.Rounded.Language,
            body = "When you enter an HTTP, HTTPS, HLS, DASH, or RTSP stream URL, Media3 ExoPlayer connects directly from your device to the media server you specified. That third-party server receives your IP address and standard network request headers according to its own privacy policy. Recently entered stream URLs remain stored locally on your device until you delete them."
        ),
        PolicySectionData(
            id = "ads_consent",
            title = "5. Advertising & User Consent",
            icon = Icons.Rounded.AdsClick,
            body = adsSectionBody
        ),
        PolicySectionData(
            id = "children",
            title = "6. Children's Privacy",
            icon = Icons.Rounded.ChildCare,
            body = "Deepak Video Player does not knowingly collect personally identifiable information from children under 13 (or the applicable age of digital consent in your region). Because we do not operate user accounts or collect personal profiles, no personal information is stored on our servers."
        ),
        PolicySectionData(
            id = "updates_contact",
            title = "7. Data Security, Updates & Contact",
            icon = Icons.Rounded.Update,
            body = "All local databases and preferences stay inside Android's private app sandbox. Standard Android system backup rules apply only if device backup is enabled in your Android settings.\n\n" +
                "We may update this Privacy Policy when app features, permissions, or ad configurations change. Updates take effect when published with a revised effective date. For questions or data inquiries, please use the developer contact details listed on the Deepak Video Player Google Play Store page."
        )
    )
}

private fun buildPlainTextPolicy(sections: List<PolicySectionData>): String {
    return buildString {
        appendLine("Privacy Policy — Deepak Video Player")
        appendLine("Effective Date: $POLICY_EFFECTIVE_DATE")
        appendLine()
        appendLine("Deepak Video Player is an offline-capable, local-first video player. We do not require account sign-in or upload your personal videos, folders, or playback history to any server.")
        appendLine()
        sections.forEach { section ->
            appendLine(section.title)
            appendLine(section.body)
            appendLine()
        }
    }.trim()
}
