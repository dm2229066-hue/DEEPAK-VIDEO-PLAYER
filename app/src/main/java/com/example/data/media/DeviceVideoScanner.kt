package com.example.data.media

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MimeTypes
import com.example.data.model.FormatUtils
import com.example.data.model.VideoItem
import com.example.data.model.VideoMetadataInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class DeviceVideoScanner(private val context: Context) {

    companion object {
        private val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "3gp", "mpeg", "mpg", "ts", "m4v"
        )

        fun requiredMediaPermission(): String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_VIDEO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
    }

    fun hasMediaPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            requiredMediaPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun scanMediaStoreVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        if (!hasMediaPermission()) return@withContext emptyList()

        val videos = mutableListOf<VideoItem>()
        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.TITLE)
            add(MediaStore.Video.Media.DISPLAY_NAME)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.DATE_ADDED)
            add(MediaStore.Video.Media.DATE_MODIFIED)
            add(MediaStore.Video.Media.MIME_TYPE)
            add(MediaStore.Video.Media.WIDTH)
            add(MediaStore.Video.Media.HEIGHT)
            add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Video.Media.DATA)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        runCatching {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    @Suppress("DEPRECATION")
                    cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val displayName = cursor.getString(nameCol) ?: "video_$id"
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBeforeLast('.')
                    val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val dateAdded = cursor.getLong(addedCol).coerceAtLeast(0L)
                    val dateMod = cursor.getLong(modCol).coerceAtLeast(0L)
                    val mimeType = cursor.getString(mimeCol) ?: "video/mp4"
                    val width = cursor.getInt(widthCol).coerceAtLeast(0)
                    val height = cursor.getInt(heightCol).coerceAtLeast(0)
                    val rawPath = if (relPathCol >= 0) cursor.getString(relPathCol) ?: "" else ""
                    val folderPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        rawPath.trimEnd('/').ifBlank { "Internal Storage" }
                    } else {
                        File(rawPath).parent ?: "Internal Storage"
                    }
                    val folderName = if (bucketCol >= 0) {
                        cursor.getString(bucketCol)?.takeIf { it.isNotBlank() }
                            ?: folderPath.substringAfterLast('/').ifBlank { "Internal Storage" }
                    } else {
                        folderPath.substringAfterLast('/').ifBlank { "Internal Storage" }
                    }

                    videos.add(
                        VideoItem(
                            id = "ms_$id",
                            uri = contentUri.toString(),
                            title = title,
                            displayName = displayName,
                            folderName = folderName,
                            folderPath = folderPath,
                            durationMs = durationMs,
                            sizeBytes = sizeBytes,
                            dateAddedSec = dateAdded,
                            dateModifiedSec = dateMod,
                            mimeType = mimeType,
                            width = width,
                            height = height,
                            isSafUri = false
                        )
                    )
                }
            }
        }
        videos
    }

    suspend fun scanSafTree(treeUriString: String, rootLabel: String): List<VideoItem> =
        withContext(Dispatchers.IO) {
            val treeUri = runCatching { Uri.parse(treeUriString) }.getOrNull()
                ?: return@withContext emptyList()
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext emptyList()
            if (!rootDoc.exists() || !rootDoc.canRead()) return@withContext emptyList()

            val results = mutableListOf<VideoItem>()
            fun walk(dir: DocumentFile, currentFolderName: String, currentPath: String, depth: Int) {
                if (depth > 3) return
                val children = runCatching { dir.listFiles() }.getOrNull() ?: return
                for (child in children) {
                    if (child.isDirectory) {
                        val subName = child.name ?: "Folder"
                        walk(child, subName, "$currentPath/$subName", depth + 1)
                    } else if (child.isFile && isVideoDocument(child)) {
                        val name = child.name ?: "Video"
                        val duration = extractQuickDuration(child.uri)
                        results.add(
                            VideoItem(
                                id = "saf_${child.uri}",
                                uri = child.uri.toString(),
                                title = name.substringBeforeLast('.'),
                                displayName = name,
                                folderName = currentFolderName,
                                folderPath = currentPath,
                                durationMs = duration,
                                sizeBytes = child.length().coerceAtLeast(0L),
                                dateAddedSec = child.lastModified() / 1000L,
                                dateModifiedSec = child.lastModified() / 1000L,
                                mimeType = child.type ?: guessMimeType(name),
                                width = 0,
                                height = 0,
                                isSafUri = true
                            )
                        )
                    }
                }
            }
            walk(rootDoc, rootLabel, treeUriString, 0)
            results
        }

    suspend fun resolveSingleUriToVideoItem(uri: Uri): VideoItem = withContext(Dispatchers.IO) {
        var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Video"
        var sizeBytes = 0L
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                }
            }
        }
        val duration = if (uri.scheme?.startsWith("http") == true) 0L else extractQuickDuration(uri)
        val mime = context.contentResolver.getType(uri) ?: guessMimeType(displayName)
        VideoItem(
            id = "uri_${uri}",
            uri = uri.toString(),
            title = displayName.substringBeforeLast('.'),
            displayName = displayName,
            folderName = if (uri.scheme?.startsWith("http") == true) "Network Stream" else "External File",
            folderPath = uri.scheme ?: "file",
            durationMs = duration,
            sizeBytes = sizeBytes,
            dateAddedSec = System.currentTimeMillis() / 1000L,
            dateModifiedSec = System.currentTimeMillis() / 1000L,
            mimeType = mime,
            width = 0,
            height = 0,
            isSafUri = uri.scheme == "content"
        )
    }

    suspend fun inspectMetadata(item: VideoItem): VideoMetadataInfo = withContext(Dispatchers.IO) {
        val uri = Uri.parse(item.uri)
        var width = item.width
        var height = item.height
        var bitrate = ""
        var frameRate = ""
        var duration = item.durationMs
        var mime = item.mimeType
        if (uri.scheme != "http" && uri.scheme != "https" && uri.scheme != "rtsp") {
            val retriever = MediaMetadataRetriever()
            runCatching {
                retriever.setDataSource(context, uri)
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: width
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: height
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: duration
                val br = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toLongOrNull()
                if (br != null && br > 0) {
                    bitrate = "${br / 1000} kbps"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val fps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                    if (!fps.isNullOrBlank()) frameRate = "$fps fps"
                }
                mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: mime
            }
            runCatching { retriever.release() }
        }
        VideoMetadataInfo(
            title = item.title,
            fileName = item.displayName,
            uri = item.uri,
            folderPath = item.folderPath,
            mimeType = mime,
            resolution = if (width > 0 && height > 0) "${width} × ${height}" else "Unknown",
            durationFormatted = FormatUtils.formatDuration(duration),
            sizeFormatted = FormatUtils.formatFileSize(item.sizeBytes),
            bitrateFormatted = bitrate.ifBlank { "Variable / N/A" },
            frameRate = frameRate.ifBlank { "Standard" },
            dateModifiedFormatted = FormatUtils.formatDate(item.dateModifiedSec),
            codecSummary = mime.substringAfter('/').uppercase(Locale.US)
        )
    }

    fun createDeleteIntentSender(uri: Uri): IntentSender? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isSafTreeDocument(uri)) {
            runCatching {
                MediaStore.createDeleteRequest(context.contentResolver, listOf(uri)).intentSender
            }.getOrNull()
        } else {
            null
        }
    }

    fun createWriteIntentSender(uri: Uri): IntentSender? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isSafTreeDocument(uri)) {
            runCatching {
                MediaStore.createWriteRequest(context.contentResolver, listOf(uri)).intentSender
            }.getOrNull()
        } else {
            null
        }
    }

    suspend fun deleteDirect(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (isSafTreeDocument(uri)) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } else {
                context.contentResolver.delete(uri, null, null) > 0
            }
        }.getOrDefault(false)
    }

    suspend fun renameDirect(uri: Uri, newDisplayName: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (isSafTreeDocument(uri)) {
                DocumentsContract.renameDocument(context.contentResolver, uri, newDisplayName) != null
            } else {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, newDisplayName)
                }
                context.contentResolver.update(uri, values, null, null) > 0
            }
        }.getOrDefault(false)
    }

    private fun isSafTreeDocument(uri: Uri): Boolean {
        return uri.authority != MediaStore.AUTHORITY &&
            runCatching { DocumentsContract.isDocumentUri(context, uri) }.getOrDefault(false)
    }

    private fun extractQuickDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        } catch (_: Throwable) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun isVideoDocument(doc: DocumentFile): Boolean {
        val type = doc.type ?: ""
        if (type.startsWith("video/")) return true
        val ext = doc.name?.substringAfterLast('.', "")?.lowercase(Locale.US) ?: ""
        return ext in VIDEO_EXTENSIONS
    }

    private fun guessMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase(Locale.US)) {
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "flv" -> "video/x-flv"
            "wmv" -> "video/x-ms-wmv"
            "3gp" -> "video/3gpp"
            "ts" -> "video/mp2t"
            "mpeg", "mpg" -> "video/mpeg"
            else -> "video/*"
        }
    }
}

object SubtitleHelper {
    private val SRT_OR_VTT_REGEX = Regex(
        """(\d{1,2}:)?(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{1,2}:)?(\d{2}):(\d{2})[,.](\d{3})"""
    )
    private val ASS_DIALOGUE_REGEX = Regex(
        """^(Dialogue:\s*[^,]*,)(\d+:\d{2}:\d{2}\.\d{2}),(\d+:\d{2}:\d{2}\.\d{2})(,.*)$"""
    )

    fun detectSubtitleMimeType(context: Context, uri: Uri): String {
        val name = queryDisplayName(context, uri) ?: uri.lastPathSegment.orEmpty()
        return when (name.substringAfterLast('.', "").lowercase(Locale.US)) {
            "vtt" -> MimeTypes.TEXT_VTT
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            else -> MimeTypes.APPLICATION_SUBRIP
        }
    }

    fun queryDisplayName(context: Context, uri: Uri): String? {
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return c.getString(idx)
                }
            }
        }
        return uri.lastPathSegment
    }

    suspend fun createShiftedSubtitleFile(
        context: Context,
        sourceUri: Uri,
        offsetMs: Long
    ): Pair<Uri, String>? = withContext(Dispatchers.IO) {
        val mime = detectSubtitleMimeType(context, sourceUri)
        if (offsetMs == 0L) return@withContext sourceUri to mime

        val text = runCatching {
            context.contentResolver.openInputStream(sourceUri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return@withContext null

        val ext = when (mime) {
            MimeTypes.TEXT_VTT -> "vtt"
            MimeTypes.TEXT_SSA -> "ass"
            else -> "srt"
        }

        val shifted = when (mime) {
            MimeTypes.TEXT_SSA -> shiftAss(text, offsetMs)
            MimeTypes.TEXT_VTT -> shiftSrtOrVtt(text, offsetMs, isVtt = true)
            else -> shiftSrtOrVtt(text, offsetMs, isVtt = false)
        }

        val outFile = File(context.cacheDir, "sub_sync_${sourceUri.toString().hashCode()}_$offsetMs.$ext")
        outFile.writeText(shifted)
        Uri.fromFile(outFile) to mime
    }

    internal fun shiftSrtOrVtt(content: String, offsetMs: Long, isVtt: Boolean): String {
        val sep = if (isVtt) '.' else ','
        return content.lines().joinToString("\n") { line ->
            val match = SRT_OR_VTT_REGEX.find(line)
            if (match == null) {
                line
            } else {
                val startMs = parseSrtTime(
                    match.groupValues[1],
                    match.groupValues[2],
                    match.groupValues[3],
                    match.groupValues[4]
                )
                val endMs = parseSrtTime(
                    match.groupValues[5],
                    match.groupValues[6],
                    match.groupValues[7],
                    match.groupValues[8]
                )
                val newStart = (startMs + offsetMs).coerceAtLeast(0L)
                val newEnd = (endMs + offsetMs).coerceAtLeast(newStart + 200L)
                val replacement = "${formatSrtTime(newStart, sep)} --> ${formatSrtTime(newEnd, sep)}"
                line.replaceRange(match.range, replacement)
            }
        }
    }

    internal fun shiftAss(content: String, offsetMs: Long): String {
        return content.lines().joinToString("\n") { line ->
            val match = ASS_DIALOGUE_REGEX.matchEntire(line)
            if (match == null) {
                line
            } else {
                val prefix = match.groupValues[1]
                val startMs = parseAssTime(match.groupValues[2])
                val endMs = parseAssTime(match.groupValues[3])
                val suffix = match.groupValues[4]
                val newStart = (startMs + offsetMs).coerceAtLeast(0L)
                val newEnd = (endMs + offsetMs).coerceAtLeast(newStart + 200L)
                "$prefix${formatAssTime(newStart)},${formatAssTime(newEnd)}$suffix"
            }
        }
    }

    private fun parseSrtTime(hGroup: String, m: String, s: String, ms: String): Long {
        val h = hGroup.removeSuffix(":").toLongOrNull() ?: 0L
        return h * 3600_000L + m.toLong() * 60_000L + s.toLong() * 1000L + ms.toLong()
    }

    private fun formatSrtTime(totalMs: Long, sep: Char): String {
        val h = totalMs / 3600_000L
        val m = (totalMs % 3600_000L) / 60_000L
        val s = (totalMs % 60_000L) / 1000L
        val ms = totalMs % 1000L
        return String.format(Locale.US, "%02d:%02d:%02d%c%03d", h, m, s, sep, ms)
    }

    private fun parseAssTime(token: String): Long {
        val parts = token.split(":", ".")
        if (parts.size != 4) return 0L
        val h = parts[0].toLongOrNull() ?: 0L
        val m = parts[1].toLongOrNull() ?: 0L
        val s = parts[2].toLongOrNull() ?: 0L
        val cs = parts[3].toLongOrNull() ?: 0L
        return h * 3600_000L + m * 60_000L + s * 1000L + cs * 10L
    }

    private fun formatAssTime(totalMs: Long): String {
        val h = totalMs / 3600_000L
        val m = (totalMs % 3600_000L) / 60_000L
        val s = (totalMs % 60_000L) / 1000L
        val cs = (totalMs % 1000L) / 10L
        return String.format(Locale.US, "%d:%02d:%02d.%02d", h, m, s, cs)
    }
}
