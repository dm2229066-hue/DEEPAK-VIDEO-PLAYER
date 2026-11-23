package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

data class VideoItem(
    val id: String,
    val uri: String,
    val title: String,
    val displayName: String,
    val folderName: String,
    val folderPath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val isSafUri: Boolean = false,
    val isFavorite: Boolean = false,
    val lastPositionMs: Long = 0L,
    val lastPlayedAt: Long = 0L
) {
    val resolutionLabel: String
        get() = when {
            width >= 3840 || height >= 2160 -> "4K"
            width >= 1920 || height >= 1080 -> "1080p"
            width >= 1280 || height >= 720 -> "720p"
            width > 0 && height > 0 -> "${width}x${height}"
            else -> mimeType.substringAfter('/').uppercase(Locale.US).ifBlank { "VIDEO" }
        }

    val progressFraction: Float
        get() = if (durationMs > 0L && lastPositionMs > 0L) {
            (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

data class VideoFolder(
    val path: String,
    val name: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val latestModifiedSec: Long,
    val isHidden: Boolean = false,
    val isSafTree: Boolean = false
)

data class VideoMetadataInfo(
    val title: String,
    val fileName: String,
    val uri: String,
    val folderPath: String,
    val mimeType: String,
    val resolution: String,
    val durationFormatted: String,
    val sizeFormatted: String,
    val bitrateFormatted: String,
    val frameRate: String,
    val dateModifiedFormatted: String,
    val codecSummary: String
)

enum class SortOption(val id: String, val label: String) {
    NAME("name", "Name"),
    DATE("date", "Date"),
    SIZE("size", "Size"),
    DURATION("duration", "Duration");

    companion object {
        fun fromId(id: String): SortOption = entries.firstOrNull { it.id == id } ?: DATE
    }
}

enum class ViewMode(val id: String) {
    LIST("LIST"),
    GRID("GRID");

    companion object {
        fun fromId(id: String): ViewMode = entries.firstOrNull { it.id == id } ?: LIST
    }
}

object FormatUtils {
    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "00:00"
        val totalSeconds = durationMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    fun formatDate(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return "Unknown"
        val millis = if (epochSeconds < 10_000_000_000L) epochSeconds * 1000L else epochSeconds
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
