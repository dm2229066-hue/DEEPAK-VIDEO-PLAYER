package com.example.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.FormatUtils
import com.example.player.AudioEffectsState
import com.example.player.TrackOption
import com.example.ui.viewmodel.PlayerUiState
import com.example.ui.viewmodel.VideoResizeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsSheet(
    uiState: PlayerUiState,
    onDismiss: () -> Unit,
    onPickExternalSubtitle: () -> Unit,
    onSelectTrack: (TrackOption?) -> Unit,
    onAdjustOffset: (Long) -> Unit,
    onUpdateStyle: (Float, Long, Float) -> Unit
) {
    val colorOptions = listOf(
        0xFFFFFFFFL to "White",
        0xFFFFD600L to "Yellow",
        0xFF00E5FFL to "Cyan",
        0xFF69F0AEL to "Green"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Subtitles (SRT • VTT • ASS • SSA)", style = MaterialTheme.typography.titleLarge)
            }
            item {
                OutlinedButton(
                    onClick = onPickExternalSubtitle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Subtitles, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        uiState.externalSubtitleName?.let { "Loaded: $it (Change)" }
                            ?: "Load External Subtitle File"
                    )
                }
            }
            item {
                Text("Embedded / Active Tracks", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                val noTrackSelected = uiState.subtitleTracks.none { it.isSelected }
                TrackChoiceRow(
                    label = "Off (Disable Subtitles)",
                    selected = noTrackSelected,
                    onClick = { onSelectTrack(null) }
                )
            }
            items(uiState.subtitleTracks) { track ->
                TrackChoiceRow(
                    label = track.label,
                    selected = track.isSelected,
                    onClick = { onSelectTrack(track) }
                )
            }
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Subtitle Synchronization (${uiState.subtitleOffsetMs} ms)",
                    style = MaterialTheme.typography.titleSmall
                )
                if (uiState.externalSubtitleName == null) {
                    Text(
                        "Load an external .srt, .vtt, .ass, or .ssa file to shift subtitle timing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { onAdjustOffset(uiState.subtitleOffsetMs - 250L) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Delay -250ms")
                    }
                    Text(
                        text = "${if (uiState.subtitleOffsetMs >= 0) "+" else ""}${uiState.subtitleOffsetMs} ms",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { onAdjustOffset(uiState.subtitleOffsetMs + 250L) }) {
                        Icon(Icons.Default.Add, contentDescription = "Advance +250ms")
                    }
                    TextButton(onClick = { onAdjustOffset(0L) }) { Text("Reset") }
                }
            }
            item {
                Text("Font Size: ${uiState.subtitleFontSizeSp.toInt()}sp", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = uiState.subtitleFontSizeSp,
                    onValueChange = {
                        onUpdateStyle(it, uiState.subtitleColorArgb, uiState.subtitlePositionFraction)
                    },
                    valueRange = 12f..32f
                )
            }
            item {
                Text("Subtitle Color", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    colorOptions.forEach { (hex, name) ->
                        val selected = uiState.subtitleColorArgb == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onUpdateStyle(uiState.subtitleFontSizeSp, hex, uiState.subtitlePositionFraction)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = name, tint = Color.Black, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Vertical Position (${(uiState.subtitlePositionFraction * 100).toInt()}% from bottom)",
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = uiState.subtitlePositionFraction,
                    onValueChange = {
                        onUpdateStyle(uiState.subtitleFontSizeSp, uiState.subtitleColorArgb, it)
                    },
                    valueRange = 0.02f..0.35f
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioAndEqSheet(
    uiState: PlayerUiState,
    fxState: AudioEffectsState,
    onDismiss: () -> Unit,
    onSelectAudioTrack: (TrackOption) -> Unit,
    onToggleMono: (Boolean) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onSetBandLevel: (Int, Int) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetVirtualizer: (Int) -> Unit,
    onSetLoudnessGain: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Audio Tracks & Engine", style = MaterialTheme.typography.titleLarge)
            }
            if (uiState.audioTracks.isEmpty()) {
                item {
                    Text(
                        "No selectable audio tracks detected yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.audioTracks) { track ->
                    TrackChoiceRow(
                        label = track.label,
                        selected = track.isSelected,
                        onClick = { onSelectAudioTrack(track) }
                    )
                }
            }

            item {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mono Audio Downmix", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Mix stereo channels evenly to both ears",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = uiState.audioMonoMode, onCheckedChange = onToggleMono)
                }
            }

            item {
                Text("Equalizer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (!fxState.isEqualizerSupported) {
                    Text(
                        "Hardware equalizer not supported by this device audio output.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(fxState.presetNames.indices.toList()) { idx ->
                            FilterChip(
                                selected = fxState.selectedPreset == idx,
                                onClick = { onSelectPreset(idx) },
                                label = { Text(fxState.presetNames[idx]) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    fxState.bandLevelsMb.forEachIndexed { idx, level ->
                        val freqHz = fxState.bandCenterFreqHz.getOrElse(idx) { 0 }
                        val freqLabel = if (freqHz >= 1000) "${freqHz / 1000}kHz" else "${freqHz}Hz"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = freqLabel,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(52.dp)
                            )
                            Slider(
                                value = level.toFloat(),
                                onValueChange = { onSetBandLevel(idx, it.toInt()) },
                                valueRange = fxState.bandMinLevelMb.toFloat()..fxState.bandMaxLevelMb.toFloat(),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${level / 100}dB",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(44.dp)
                            )
                        }
                    }
                }
            }

            item {
                Text("Bass Boost", style = MaterialTheme.typography.titleSmall)
                if (fxState.isBassBoostSupported) {
                    Slider(
                        value = fxState.bassBoostStrength.toFloat(),
                        onValueChange = { onSetBassBoost(it.toInt()) },
                        valueRange = 0f..1000f
                    )
                } else {
                    Text(
                        "Bass boost unavailable on this device output.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Text("Virtualizer (Surround)", style = MaterialTheme.typography.titleSmall)
                if (fxState.isVirtualizerSupported) {
                    Slider(
                        value = fxState.virtualizerStrength.toFloat(),
                        onValueChange = { onSetVirtualizer(it.toInt()) },
                        valueRange = 0f..1000f
                    )
                } else {
                    Text(
                        "Virtualizer unavailable on this device output.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Text("Volume Boost (+${fxState.loudnessGainMb / 100} dB)", style = MaterialTheme.typography.titleSmall)
                if (fxState.isLoudnessSupported) {
                    Slider(
                        value = fxState.loudnessGainMb.toFloat(),
                        onValueChange = { onSetLoudnessGain(it.toInt()) },
                        valueRange = 0f..1500f
                    )
                } else {
                    Text(
                        "Loudness boost not supported by this audio engine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedAndToolsSheet(
    uiState: PlayerUiState,
    onDismiss: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetResizeMode: (VideoResizeMode) -> Unit,
    onMarkA: () -> Unit,
    onMarkB: () -> Unit,
    onClearAB: () -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onToggleAutoPlayNext: (Boolean) -> Unit,
    onToggleBackgroundPlayback: (Boolean) -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Playback Speed (${String.format("%.2fx", uiState.playbackSpeed)})", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(speeds) { s ->
                        FilterChip(
                            selected = uiState.playbackSpeed == s,
                            onClick = { onSetSpeed(s) },
                            label = { Text("${s}x") }
                        )
                    }
                }
                Slider(
                    value = uiState.playbackSpeed,
                    onValueChange = onSetSpeed,
                    valueRange = 0.25f..4.0f
                )
            }

            item {
                Text("Aspect Ratio & Scaling", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoResizeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.resizeMode == mode,
                            onClick = { onSetResizeMode(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }

            item {
                Text("A-B Repeat Loop", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onMarkA, modifier = Modifier.weight(1f)) {
                        Text(uiState.abPointAMs?.let { "A: ${FormatUtils.formatDuration(it)}" } ?: "Set A")
                    }
                    OutlinedButton(onClick = onMarkB, modifier = Modifier.weight(1f)) {
                        Text(uiState.abPointBMs?.let { "B: ${FormatUtils.formatDuration(it)}" } ?: "Set B")
                    }
                    if (uiState.abPointAMs != null || uiState.abPointBMs != null) {
                        Button(onClick = onClearAB) { Text("Clear") }
                    }
                }
            }

            item {
                Text("Sleep Timer", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val timers = listOf(
                        null to "Off",
                        10 to "10m",
                        15 to "15m",
                        30 to "30m",
                        45 to "45m",
                        60 to "60m",
                        -1 to "End of video"
                    )
                    items(timers) { (mins, label) ->
                        val active = when (mins) {
                            null -> uiState.sleepTimerRemainingSec == null && !uiState.sleepAfterCurrentVideo
                            -1 -> uiState.sleepAfterCurrentVideo
                            else -> uiState.sleepTimerRemainingSec != null && !uiState.sleepAfterCurrentVideo
                        }
                        FilterChip(
                            selected = active && (mins == null || mins == -1),
                            onClick = { onSetSleepTimer(mins) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-Play Next Video", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Switch(checked = uiState.autoPlayNext, onCheckedChange = onToggleAutoPlayNext)
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Background Audio Playback", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Switch(checked = uiState.backgroundPlayback, onCheckedChange = onToggleBackgroundPlayback)
                }
            }
        }
    }
}

@Composable
private fun TrackChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
