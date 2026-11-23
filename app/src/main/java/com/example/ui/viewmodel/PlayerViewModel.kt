package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.DeepakPlayerApp
import com.example.data.media.SubtitleHelper
import com.example.data.model.VideoItem
import com.example.data.preferences.OrientationPreference
import com.example.player.AudioEffectsState
import com.example.player.TrackOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class VideoResizeMode(val label: String, val media3Mode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    CROP("Crop", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT)
}

enum class PlayerOrientationMode(val label: String) {
    AUTO("Auto"),
    LANDSCAPE("Landscape"),
    PORTRAIT("Portrait")
}

data class PlayerUiState(
    val queue: List<VideoItem> = emptyList(),
    val currentIndex: Int = 0,
    val currentVideo: VideoItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val resizeMode: VideoResizeMode = VideoResizeMode.FIT,
    val zoomScale: Float = 1.0f,
    val isLocked: Boolean = false,
    val isRotationLocked: Boolean = false,
    val orientationMode: PlayerOrientationMode = PlayerOrientationMode.AUTO,
    val autoPlayNext: Boolean = true,
    val backgroundPlayback: Boolean = false,
    val gesturesEnabled: Boolean = true,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val abPointAMs: Long? = null,
    val abPointBMs: Long? = null,
    val sleepTimerRemainingSec: Int? = null,
    val sleepAfterCurrentVideo: Boolean = false,
    val subtitleTracks: List<TrackOption> = emptyList(),
    val audioTracks: List<TrackOption> = emptyList(),
    val externalSubtitleName: String? = null,
    val subtitleOffsetMs: Long = 0L,
    val subtitleFontSizeSp: Float = 18f,
    val subtitleColorArgb: Long = 0xFFFFFFFFL,
    val subtitlePositionFraction: Float = 0.08f,
    val audioMonoMode: Boolean = false,
    val errorMessage: String? = null,
    val isPiPActive: Boolean = false
) {
    val hasNext: Boolean get() = currentIndex < queue.lastIndex
    val hasPrevious: Boolean get() = currentIndex > 0
}

@OptIn(UnstableApi::class)
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DeepakPlayerApp
    private val repository = app.libraryRepository
    private val prefsRepo = app.preferencesRepository
    private val playerHolder = app.playerHolder

    val player: ExoPlayer = playerHolder.getOrCreatePlayer()
    val audioEffectsState: StateFlow<AudioEffectsState> = playerHolder.audioEffectsManager.state

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var activeExternalSubUri: Uri? = null
    private var tickerJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            if (!isPlaying) persistCurrentProgress()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val buffering = playbackState == Player.STATE_BUFFERING
            val dur = player.duration.coerceAtLeast(0L)
            _uiState.value = _uiState.value.copy(
                isBuffering = buffering,
                durationMs = if (dur > 0) dur else _uiState.value.durationMs
            )

            if (playbackState == Player.STATE_ENDED) {
                if (_uiState.value.sleepAfterCurrentVideo) {
                    _uiState.value = _uiState.value.copy(sleepAfterCurrentVideo = false)
                    player.pause()
                    return
                }
                if (_uiState.value.autoPlayNext && _uiState.value.hasNext) {
                    playNext()
                }
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            refreshTrackLists()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            _uiState.value = _uiState.value.copy(
                videoWidth = videoSize.width,
                videoHeight = videoSize.height
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            val friendly = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Network connection failed. Check your internet connection and retry."
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                    "Unsupported video/audio codec or hardware decoder error (${error.errorCodeName})."
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                    "Video file could not be found or access permission expired."
                else -> error.localizedMessage ?: "Playback error (${error.errorCodeName})."
            }
            _uiState.value = _uiState.value.copy(
                errorMessage = friendly,
                isBuffering = false
            )
        }
    }

    init {
        player.addListener(playerListener)
        startPositionTicker()
    }

    fun startPlayback(queue: List<VideoItem>, startIndex: Int) {
        if (queue.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, queue.lastIndex)
        viewModelScope.launch {
            val prefs = prefsRepo.preferencesFlow.first()
            playerHolder.setMonoMode(prefs.audioMonoMode)

            val orient = when (prefs.defaultOrientation) {
                OrientationPreference.LANDSCAPE -> PlayerOrientationMode.LANDSCAPE
                OrientationPreference.PORTRAIT -> PlayerOrientationMode.PORTRAIT
                OrientationPreference.AUTO -> PlayerOrientationMode.AUTO
            }

            _uiState.value = _uiState.value.copy(
                queue = queue,
                currentIndex = safeIndex,
                playbackSpeed = prefs.defaultPlaybackSpeed,
                orientationMode = orient,
                autoPlayNext = prefs.autoPlayNext,
                backgroundPlayback = prefs.backgroundPlayback,
                gesturesEnabled = prefs.gesturesEnabled,
                subtitleFontSizeSp = prefs.subtitleFontSizeSp,
                subtitleColorArgb = prefs.subtitleColorArgb,
                subtitlePositionFraction = prefs.subtitlePositionFraction,
                audioMonoMode = prefs.audioMonoMode,
                abPointAMs = null,
                abPointBMs = null,
                zoomScale = 1.0f,
                errorMessage = null
            )

            if (prefs.backgroundPlayback) {
                playerHolder.startForegroundPlaybackService()
            }
            loadQueueItem(safeIndex)
        }
    }

    private suspend fun loadQueueItem(index: Int) {
        val queue = _uiState.value.queue
        if (index !in queue.indices) return
        val item = queue[index]
        val history = repository.getHistoryForUri(item.uri)

        activeExternalSubUri = history?.externalSubtitleUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val offset = history?.subtitleOffsetMs ?: 0L
        val subName = activeExternalSubUri?.let { SubtitleHelper.queryDisplayName(app, it) }

        val shiftedSub = activeExternalSubUri?.let {
            SubtitleHelper.createShiftedSubtitleFile(app, it, offset)
        }

        val mediaItem = playerHolder.buildMediaItem(
            uri = item.uri,
            title = item.title,
            subtitleUri = shiftedSub?.first,
            subtitleMimeType = shiftedSub?.second,
            subtitleLabel = subName ?: "External Subtitle"
        )

        val resumePos = if (history != null && history.lastPositionMs > 2000L &&
            (history.durationMs <= 0L || history.lastPositionMs < history.durationMs - 5000L)
        ) {
            history.lastPositionMs
        } else 0L

        val speed = history?.preferredSpeed?.takeIf { it in 0.25f..4.0f }
            ?: _uiState.value.playbackSpeed

        _uiState.value = _uiState.value.copy(
            currentIndex = index,
            currentVideo = item,
            durationMs = item.durationMs,
            positionMs = resumePos,
            playbackSpeed = speed,
            externalSubtitleName = subName,
            subtitleOffsetMs = offset,
            abPointAMs = null,
            abPointBMs = null,
            errorMessage = null
        )

        player.setMediaItem(mediaItem, resumePos)
        player.playbackParameters = PlaybackParameters(speed)
        player.prepare()
        player.play()
        refreshTrackLists()
    }

    fun retryPlayback() {
        val pos = player.currentPosition.coerceAtLeast(0L)
        _uiState.value = _uiState.value.copy(errorMessage = null)
        player.seekTo(pos)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val dur = player.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
        val target = if (dur > 0) positionMs.coerceIn(0L, dur) else positionMs.coerceAtLeast(0L)
        player.seekTo(target)
        _uiState.value = _uiState.value.copy(positionMs = target)
    }

    fun seekBy(deltaMs: Long) {
        seekTo(player.currentPosition + deltaMs)
    }

    fun playNext() {
        if (!_uiState.value.hasNext) return
        persistCurrentProgress()
        val nextIdx = _uiState.value.currentIndex + 1
        viewModelScope.launch { loadQueueItem(nextIdx) }
    }

    fun playPrevious() {
        if (!_uiState.value.hasPrevious) {
            seekTo(0L)
            return
        }
        persistCurrentProgress()
        val prevIdx = _uiState.value.currentIndex - 1
        viewModelScope.launch { loadQueueItem(prevIdx) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        player.playbackParameters = PlaybackParameters(clamped)
        _uiState.value = _uiState.value.copy(playbackSpeed = clamped)
    }

    fun cycleResizeMode() {
        val modes = VideoResizeMode.entries
        val next = modes[(modes.indexOf(_uiState.value.resizeMode) + 1) % modes.size]
        _uiState.value = _uiState.value.copy(resizeMode = next, zoomScale = 1.0f)
    }

    fun setResizeMode(mode: VideoResizeMode) {
        _uiState.value = _uiState.value.copy(resizeMode = mode, zoomScale = 1.0f)
    }

    fun updateZoomScale(scale: Float) {
        _uiState.value = _uiState.value.copy(zoomScale = scale.coerceIn(0.5f, 4.0f))
    }

    fun toggleLock() {
        _uiState.value = _uiState.value.copy(isLocked = !_uiState.value.isLocked)
    }

    fun toggleRotationLock() {
        _uiState.value = _uiState.value.copy(isRotationLocked = !_uiState.value.isRotationLocked)
    }

    fun cycleOrientationMode() {
        val next = when (_uiState.value.orientationMode) {
            PlayerOrientationMode.AUTO -> PlayerOrientationMode.LANDSCAPE
            PlayerOrientationMode.LANDSCAPE -> PlayerOrientationMode.PORTRAIT
            PlayerOrientationMode.PORTRAIT -> PlayerOrientationMode.AUTO
        }
        _uiState.value = _uiState.value.copy(orientationMode = next, isRotationLocked = false)
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoPlayNext = enabled)
        viewModelScope.launch { prefsRepo.setAutoPlayNext(enabled) }
    }

    fun setBackgroundPlayback(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(backgroundPlayback = enabled)
        if (enabled) {
            playerHolder.startForegroundPlaybackService()
        } else {
            playerHolder.stopForegroundPlaybackService()
        }
        viewModelScope.launch { prefsRepo.setBackgroundPlayback(enabled) }
    }

    // A-B Repeat
    fun markPointA() {
        _uiState.value = _uiState.value.copy(abPointAMs = player.currentPosition.coerceAtLeast(0L))
    }

    fun markPointB() {
        val a = _uiState.value.abPointAMs ?: 0L
        val current = player.currentPosition.coerceAtLeast(0L)
        if (current > a + 500L) {
            _uiState.value = _uiState.value.copy(abPointBMs = current)
        }
    }

    fun clearABRepeat() {
        _uiState.value = _uiState.value.copy(abPointAMs = null, abPointBMs = null)
    }

    // Sleep Timer
    fun setSleepTimerMinutes(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null) {
            _uiState.value = _uiState.value.copy(
                sleepTimerRemainingSec = null,
                sleepAfterCurrentVideo = false
            )
            return
        }
        if (minutes == -1) {
            _uiState.value = _uiState.value.copy(
                sleepTimerRemainingSec = null,
                sleepAfterCurrentVideo = true
            )
            return
        }
        var remaining = minutes * 60
        _uiState.value = _uiState.value.copy(
            sleepTimerRemainingSec = remaining,
            sleepAfterCurrentVideo = false
        )
        sleepTimerJob = viewModelScope.launch {
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _uiState.value = _uiState.value.copy(sleepTimerRemainingSec = remaining)
            }
            player.pause()
            _uiState.value = _uiState.value.copy(sleepTimerRemainingSec = null)
        }
    }

    // Subtitles
    fun loadExternalSubtitle(uri: Uri) {
        viewModelScope.launch {
            activeExternalSubUri = uri
            val subName = SubtitleHelper.queryDisplayName(app, uri) ?: "External Subtitle"
            val offset = _uiState.value.subtitleOffsetMs
            val shifted = SubtitleHelper.createShiftedSubtitleFile(app, uri, offset) ?: return@launch
            val currentPos = player.currentPosition.coerceAtLeast(0L)
            val wasPlaying = player.isPlaying
            val video = _uiState.value.currentVideo ?: return@launch

            val mediaItem = playerHolder.buildMediaItem(
                uri = video.uri,
                title = video.title,
                subtitleUri = shifted.first,
                subtitleMimeType = shifted.second,
                subtitleLabel = subName
            )
            _uiState.value = _uiState.value.copy(externalSubtitleName = subName)
            player.setMediaItem(mediaItem, currentPos)
            player.prepare()
            if (wasPlaying) player.play()
        }
    }

    fun adjustSubtitleOffset(newOffsetMs: Long) {
        val clamped = newOffsetMs.coerceIn(-10_000L, 10_000L)
        _uiState.value = _uiState.value.copy(subtitleOffsetMs = clamped)
        val extUri = activeExternalSubUri ?: return
        val video = _uiState.value.currentVideo ?: return
        viewModelScope.launch {
            val shifted = SubtitleHelper.createShiftedSubtitleFile(app, extUri, clamped) ?: return@launch
            val pos = player.currentPosition.coerceAtLeast(0L)
            val wasPlaying = player.isPlaying
            val mediaItem = playerHolder.buildMediaItem(
                uri = video.uri,
                title = video.title,
                subtitleUri = shifted.first,
                subtitleMimeType = shifted.second,
                subtitleLabel = _uiState.value.externalSubtitleName ?: "External Subtitle"
            )
            player.setMediaItem(mediaItem, pos)
            player.prepare()
            if (wasPlaying) player.play()
        }
    }

    fun updateSubtitleStyle(fontSizeSp: Float, colorArgb: Long, positionFraction: Float) {
        _uiState.value = _uiState.value.copy(
            subtitleFontSizeSp = fontSizeSp,
            subtitleColorArgb = colorArgb,
            subtitlePositionFraction = positionFraction
        )
        viewModelScope.launch {
            prefsRepo.setSubtitlePreferences(fontSizeSp, colorArgb, positionFraction)
        }
    }

    fun selectSubtitleTrack(option: TrackOption?) {
        if (option == null) {
            playerHolder.disableTrackType(C.TRACK_TYPE_TEXT)
        } else {
            playerHolder.selectTrack(C.TRACK_TYPE_TEXT, option.groupIndex, option.trackIndex)
        }
        refreshTrackLists()
    }

    fun selectAudioTrack(option: TrackOption) {
        playerHolder.selectTrack(C.TRACK_TYPE_AUDIO, option.groupIndex, option.trackIndex)
        refreshTrackLists()
    }

    // Audio Effects & Mono
    fun setMonoMode(enabled: Boolean) {
        playerHolder.setMonoMode(enabled)
        _uiState.value = _uiState.value.copy(audioMonoMode = enabled)
        saveAudioPrefs()
    }

    fun selectEqualizerPreset(presetIndex: Int) {
        playerHolder.audioEffectsManager.selectPreset(presetIndex)
        saveAudioPrefs()
    }

    fun setEqualizerBand(bandIndex: Int, levelMb: Int) {
        playerHolder.audioEffectsManager.setBandLevel(bandIndex, levelMb)
    }

    fun setBassBoost(strength: Int) {
        playerHolder.audioEffectsManager.setBassBoost(strength)
        saveAudioPrefs()
    }

    fun setVirtualizer(strength: Int) {
        playerHolder.audioEffectsManager.setVirtualizer(strength)
        saveAudioPrefs()
    }

    fun setLoudnessGain(gainMb: Int) {
        playerHolder.audioEffectsManager.setLoudnessGain(gainMb)
        saveAudioPrefs()
    }

    private fun saveAudioPrefs() {
        val fx = audioEffectsState.value
        val mono = _uiState.value.audioMonoMode
        viewModelScope.launch {
            prefsRepo.setAudioPreferences(
                monoMode = mono,
                eqPreset = fx.selectedPreset,
                bassBoost = fx.bassBoostStrength,
                virtualizer = fx.virtualizerStrength,
                loudnessGainMb = fx.loudnessGainMb
            )
        }
    }

    fun setPiPActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(isPiPActive = active)
    }

    fun onExitPlayer() {
        persistCurrentProgress()
        if (!_uiState.value.backgroundPlayback) {
            player.pause()
        }
    }

    private fun refreshTrackLists() {
        _uiState.value = _uiState.value.copy(
            subtitleTracks = playerHolder.getTrackOptions(C.TRACK_TYPE_TEXT),
            audioTracks = playerHolder.getTrackOptions(C.TRACK_TYPE_AUDIO)
        )
    }

    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(250L)
                val pos = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration.coerceAtLeast(0L)
                if (pos != _uiState.value.positionMs || dur != _uiState.value.durationMs) {
                    _uiState.value = _uiState.value.copy(
                        positionMs = pos,
                        durationMs = if (dur > 0) dur else _uiState.value.durationMs
                    )
                }
                val a = _uiState.value.abPointAMs
                val b = _uiState.value.abPointBMs
                if (a != null && b != null && b > a && pos >= b) {
                    player.seekTo(a)
                }
            }
        }
    }

    private fun persistCurrentProgress() {
        val video = _uiState.value.currentVideo ?: return
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.takeIf { it > 0 } ?: video.durationMs
        viewModelScope.launch {
            repository.savePlaybackProgress(
                uri = video.uri,
                title = video.title,
                durationMs = dur,
                positionMs = pos,
                subtitleOffsetMs = _uiState.value.subtitleOffsetMs,
                preferredSpeed = _uiState.value.playbackSpeed,
                externalSubtitleUri = activeExternalSubUri?.toString()
            )
        }
    }

    override fun onCleared() {
        persistCurrentProgress()
        tickerJob?.cancel()
        sleepTimerJob?.cancel()
        player.removeListener(playerListener)
        super.onCleared()
    }
}
