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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.ads.AdBannerBar
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.OrientationPreference
import com.example.data.preferences.UserPreferences
import com.example.ui.theme.AccentColorOption

@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    canRequestAds: Boolean,
    isPrivacyOptionsRequired: Boolean,
    onSetDarkTheme: (Boolean) -> Unit,
    onSelectAccent: (AccentColorOption) -> Unit,
    onUnlockAccentWithRewardedAd: (AccentColorOption) -> Unit,
    onSetDefaultSpeed: (Float) -> Unit,
    onSetDefaultOrientation: (OrientationPreference) -> Unit,
    onSetGesturesEnabled: (Boolean) -> Unit,
    onSetAutoPlayNext: (Boolean) -> Unit,
    onSetBackgroundPlayback: (Boolean) -> Unit,
    onSetSubtitlePrefs: (Float, Long, Float) -> Unit,
    onSetShowHiddenFolders: (Boolean) -> Unit,
    onSetAppLanguage: (AppLanguage) -> Unit,
    onClearHistory: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenAdConsentOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Appearance & Theme
            item {
                SettingsSectionCard(title = "Appearance & Theme") {
                    SettingsSwitchRow(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Cinema Theme",
                        subtitle = if (prefs.isDarkTheme) "Obsidian black & charcoal" else "Clean light mode",
                        checked = prefs.isDarkTheme,
                        onCheckedChange = onSetDarkTheme,
                        tag = "settings_dark_theme_switch"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Accent Color (Watch Rewarded Ad for Pro Accents)", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AccentColorOption.entries.forEach { option ->
                            val unlocked = !option.requiresRewardUnlock || option.id in prefs.unlockedAccents
                            val selected = prefs.accentOption == option
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (unlocked) onSelectAccent(option)
                                        else onUnlockAccentWithRewardedAd(option)
                                    }
                                    .padding(vertical = 6.dp)
                                    .testTag("accent_${option.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(option.color)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = option.onAccent, modifier = Modifier.size(18.dp))
                                    } else if (!unlocked) {
                                        Icon(Icons.Default.Lock, contentDescription = "Watch ad to unlock", tint = option.onAccent, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (unlocked) option.label.substringBefore(' ') else "Ad",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Playback Preferences
            item {
                SettingsSectionCard(title = "Playback & Gestures") {
                    SettingsSwitchRow(
                        icon = Icons.Default.TouchApp,
                        title = "Player Swipe & Tap Gestures",
                        subtitle = "Brightness, volume, seek, double-tap & pinch zoom",
                        checked = prefs.gesturesEnabled,
                        onCheckedChange = onSetGesturesEnabled,
                        tag = "settings_gestures_switch"
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.PlayCircle,
                        title = "Auto-Play Next Video",
                        subtitle = "Continue sequentially through folder or playlist",
                        checked = prefs.autoPlayNext,
                        onCheckedChange = onSetAutoPlayNext
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.PlayCircle,
                        title = "Background Playback",
                        subtitle = "Keep audio playing with notification when minimized",
                        checked = prefs.backgroundPlayback,
                        onCheckedChange = onSetBackgroundPlayback
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Default Speed: ${prefs.defaultPlaybackSpeed}x", style = MaterialTheme.typography.titleSmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            FilterChip(
                                selected = prefs.defaultPlaybackSpeed == spd,
                                onClick = { onSetDefaultSpeed(spd) },
                                label = { Text("${spd}x") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Default Player Orientation", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OrientationPreference.entries.forEach { orient ->
                            FilterChip(
                                selected = prefs.defaultOrientation == orient,
                                onClick = { onSetDefaultOrientation(orient) },
                                label = { Text(orient.label.substringBefore(' ')) }
                            )
                        }
                    }
                }
            }

            // Subtitle & Audio Defaults
            item {
                SettingsSectionCard(title = "Subtitles & Audio") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Subtitles, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Subtitle Size: ${prefs.subtitleFontSizeSp.toInt()}sp", style = MaterialTheme.typography.titleSmall)
                    }
                    Slider(
                        value = prefs.subtitleFontSizeSp,
                        onValueChange = {
                            onSetSubtitlePrefs(it, prefs.subtitleColorArgb, prefs.subtitlePositionFraction)
                        },
                        valueRange = 12f..32f
                    )
                    Text(
                        text = "Audio Equalizer, Bass Boost, Virtualizer, Volume Boost, and Stereo/Mono controls are available directly in the Player Audio Sheet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Library, Language & History
            item {
                SettingsSectionCard(title = "Library, Language & History") {
                    SettingsSwitchRow(
                        icon = Icons.Default.Visibility,
                        title = "Show Hidden Folders",
                        subtitle = "Display folders marked hidden in File Manager",
                        checked = prefs.showHiddenFolders,
                        onCheckedChange = onSetShowHiddenFolders
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("App Language", style = MaterialTheme.typography.titleSmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = prefs.appLanguage == lang,
                                onClick = { onSetAppLanguage(lang) },
                                label = { Text(lang.label.substringBefore(' ')) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsActionRow(
                        icon = Icons.Default.DeleteSweep,
                        title = stringResource(R.string.clear_history),
                        subtitle = "Clear resume positions and recently played list",
                        onClick = { showClearHistoryDialog = true },
                        tag = "settings_clear_history"
                    )
                }
            }

            // Privacy, Consent & About
            item {
                SettingsSectionCard(title = "Privacy & About") {
                    SettingsActionRow(
                        icon = Icons.Default.PrivacyTip,
                        title = stringResource(R.string.privacy_policy_title),
                        subtitle = "Local video privacy & AdMob data disclosure",
                        onClick = onOpenPrivacyPolicy,
                        tag = "settings_privacy_policy"
                    )
                    if (isPrivacyOptionsRequired) {
                        SettingsActionRow(
                            icon = Icons.Default.PrivacyTip,
                            title = "Ad Privacy & Consent Options",
                            subtitle = "Manage personalized ad consent preferences",
                            onClick = onOpenAdConsentOptions
                        )
                    }
                    SettingsActionRow(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.about_app_title),
                        subtitle = "Version ${BuildConfig.VERSION_NAME} • Formats & Capabilities",
                        onClick = { showAboutDialog = true },
                        tag = "settings_about"
                    )
                }
            }
        }

        AdBannerBar(canRequestAds = canRequestAds)
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Playback History?") },
            text = { Text("This removes all saved resume timestamps and recently played records.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Deepak Video Player v${BuildConfig.VERSION_NAME}") },
            text = {
                Text(
                    "Powered by Android Jetpack Media3 ExoPlayer.\n\n" +
                        "Supported Containers & Streams:\n" +
                        "• MP4, MKV, WEBM, 3GP, MOV, TS, MPEG\n" +
                        "• AVI, FLV, WMV (via device MediaCodec decoders where supported)\n" +
                        "• HTTP / HTTPS / HLS (.m3u8) / DASH (.mpd) / RTSP\n" +
                        "• Subtitles: SRT, VTT, ASS, SSA & embedded tracks\n\n" +
                        "All personal videos stay strictly on your device."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
