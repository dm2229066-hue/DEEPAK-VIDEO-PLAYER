package com.example.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import java.nio.ByteBuffer

data class TrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val isSelected: Boolean,
    val mimeType: String
)

@OptIn(UnstableApi::class)
class MonoDownmixAudioProcessor : BaseAudioProcessor() {
    @Volatile
    var monoEnabled: Boolean = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount == 2) {
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val outBuffer = replaceOutputBuffer(remaining)
        if (!monoEnabled) {
            outBuffer.put(inputBuffer)
        } else {
            while (inputBuffer.remaining() >= 4) {
                val left = inputBuffer.short.toInt()
                val right = inputBuffer.short.toInt()
                val avg = ((left + right) / 2).toShort()
                outBuffer.putShort(avg)
                outBuffer.putShort(avg)
            }
            if (inputBuffer.hasRemaining()) {
                outBuffer.put(inputBuffer)
            }
        }
        outBuffer.flip()
    }
}

@OptIn(UnstableApi::class)
class PlayerHolder(private val appContext: Context) {

    val audioEffectsManager = AudioEffectsManager()
    private val monoProcessor = MonoDownmixAudioProcessor()

    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var serviceStarted = false

    fun getOrCreatePlayer(): ExoPlayer {
        exoPlayer?.let { return it }

        val selector = DefaultTrackSelector(appContext).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredTextLanguage("en")
            )
        }
        trackSelector = selector

        val renderersFactory = object : DefaultRenderersFactory(appContext) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): DefaultAudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(monoProcessor))
                    .setEnableFloatOutput(false)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(appContext, renderersFactory)
            .setTrackSelector(selector)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                audioEffectsManager.attachToSession(audioSessionId)
            }
        })

        if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET && player.audioSessionId != 0) {
            audioEffectsManager.attachToSession(player.audioSessionId)
        }

        exoPlayer = player
        return player
    }

    fun startForegroundPlaybackService() {
        if (serviceStarted) return
        val intent = Intent(appContext, PlaybackService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startService(intent)
            } else {
                appContext.startService(intent)
            }
            serviceStarted = true
        }
    }

    fun stopForegroundPlaybackService() {
        if (!serviceStarted) return
        runCatching {
            appContext.stopService(Intent(appContext, PlaybackService::class.java))
        }
        serviceStarted = false
    }

    fun setMonoMode(enabled: Boolean) {
        monoProcessor.monoEnabled = enabled
    }

    fun buildMediaItem(
        uri: String,
        title: String,
        subtitleUri: Uri? = null,
        subtitleMimeType: String? = null,
        subtitleLabel: String = "External Subtitle"
    ): MediaItem {
        val builder = MediaItem.Builder()
            .setMediaId(uri)
            .setUri(Uri.parse(uri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .build()
            )

        if (subtitleUri != null && subtitleMimeType != null) {
            val subConfig = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                .setMimeType(subtitleMimeType)
                .setLanguage("und")
                .setLabel(subtitleLabel)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            builder.setSubtitleConfigurations(listOf(subConfig))
        }
        return builder.build()
    }

    fun getTrackOptions(trackType: @C.TrackType Int): List<TrackOption> {
        val player = exoPlayer ?: return emptyList()
        val tracks = player.currentTracks
        val list = mutableListOf<TrackOption>()
        tracks.groups.forEachIndexed { groupIdx, group ->
            if (group.type == trackType) {
                for (trackIdx in 0 until group.length) {
                    val format = group.getTrackFormat(trackIdx)
                    val lang = format.language?.takeIf { it != "und" } ?: "Default"
                    val label = buildString {
                        append(format.label ?: lang.uppercase())
                        if (trackType == C.TRACK_TYPE_AUDIO && format.channelCount > 0) {
                            append(" (${format.channelCount}ch)")
                        }
                        if (!format.sampleMimeType.isNullOrBlank()) {
                            append(" • ${format.sampleMimeType?.substringAfter('/')}")
                        }
                    }
                    list.add(
                        TrackOption(
                            groupIndex = groupIdx,
                            trackIndex = trackIdx,
                            label = label,
                            language = lang,
                            isSelected = group.isTrackSelected(trackIdx),
                            mimeType = format.sampleMimeType.orEmpty()
                        )
                    )
                }
            }
        }
        return list
    }

    fun selectTrack(trackType: @C.TrackType Int, groupIndex: Int, trackIndex: Int) {
        val player = exoPlayer ?: return
        val groups = player.currentTracks.groups
        if (groupIndex !in groups.indices) return
        val group = groups[groupIndex]
        val params = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .setOverrideForType(
                TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
            )
            .build()
        player.trackSelectionParameters = params
    }

    fun disableTrackType(trackType: @C.TrackType Int) {
        val player = exoPlayer ?: return
        val params = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, true)
            .build()
        player.trackSelectionParameters = params
    }

    fun release() {
        audioEffectsManager.release()
        exoPlayer?.release()
        exoPlayer = null
        stopForegroundPlaybackService()
    }
}
