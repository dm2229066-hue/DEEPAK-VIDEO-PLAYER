package com.example.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.util.TypedValue
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.example.data.model.FormatUtils
import com.example.ui.viewmodel.PlayerOrientationMode
import com.example.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs

private enum class ActiveGestureMode {
    NONE, SEEK, BRIGHTNESS, VOLUME, ZOOM
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onPickExternalSubtitle: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fxState by viewModel.audioEffectsState.collectAsStateWithLifecycle()

    var controlsVisible by remember { mutableStateOf(true) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showToolsSheet by remember { mutableStateOf(false) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var activeGesture by remember { mutableStateOf(ActiveGestureMode.NONE) }
    var gestureFeedbackText by remember { mutableStateOf<String?>(null) }
    var gestureProgress by remember { mutableFloatStateOf(0f) }
    var doubleTapBurstText by remember { mutableStateOf<String?>(null) }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }

    var currentBrightness by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.5f
        )
    }

    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var currentVolumeFloat by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }

    // Apply orientation mode + immersive fullscreen system bars
    LaunchedEffect(uiState.orientationMode, uiState.isRotationLocked) {
        activity ?: return@LaunchedEffect
        if (uiState.isRotationLocked) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            activity.requestedOrientation = when (uiState.orientationMode) {
                PlayerOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                PlayerOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                PlayerOrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
        }
    }

    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            viewModel.onExitPlayer()
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val attrs = window.attributes
                attrs.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = attrs
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Auto-hide controls while playing
    LaunchedEffect(controlsVisible, uiState.isPlaying, uiState.isLocked, showSubtitleSheet, showAudioSheet, showToolsSheet) {
        if (controlsVisible && uiState.isPlaying && !showSubtitleSheet && !showAudioSheet && !showToolsSheet) {
            delay(3600L)
            controlsVisible = false
        }
    }

    // Clear double-tap visual burst
    LaunchedEffect(doubleTapBurstText) {
        if (doubleTapBurstText != null) {
            delay(700L)
            doubleTapBurstText = null
        }
    }

    BackHandler {
        viewModel.onExitPlayer()
        onNavigateBack()
    }

    fun enterPiPMode() {
        val act = activity ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ratio = safePipRational(uiState.videoWidth, uiState.videoHeight)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(ratio)
                .build()
            runCatching { act.enterPictureInPictureMode(params) }
        }
    }

    val pipModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.onGloballyPositioned { coords ->
            val act = activity ?: return@onGloballyPositioned
            val bounds = coords.boundsInWindow()
            val rect = android.graphics.Rect(
                bounds.left.toInt(),
                bounds.top.toInt(),
                bounds.right.toInt(),
                bounds.bottom.toInt()
            )
            val ratio = safePipRational(uiState.videoWidth, uiState.videoHeight)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(ratio)
                .setSourceRectHint(rect)
                .setAutoEnterEnabled(uiState.isPlaying)
                .build()
            runCatching { act.setPictureInPictureParams(params) }
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .then(pipModifier)
    ) {
        // Video Surface + SubtitleView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                    keepScreenOn = true
                }
            },
            update = { playerView ->
                if (playerView.player !== viewModel.player) {
                    playerView.player = viewModel.player
                }
                playerView.resizeMode = uiState.resizeMode.media3Mode
                playerView.subtitleView?.apply {
                    setApplyEmbeddedStyles(false)
                    setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, uiState.subtitleFontSizeSp)
                    setStyle(
                        CaptionStyleCompat(
                            uiState.subtitleColorArgb.toInt(),
                            android.graphics.Color.argb(145, 0, 0, 0),
                            android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                            android.graphics.Color.BLACK,
                            null
                        )
                    )
                    setBottomPaddingFraction(uiState.subtitlePositionFraction)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = uiState.zoomScale,
                    scaleY = uiState.zoomScale
                )
        )

        // Gesture Detection Layer (disabled controls when PiP active)
        if (!uiState.isPiPActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(uiState.isLocked, uiState.gesturesEnabled, containerSize) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                            },
                            onDoubleTap = { offset ->
                                if (uiState.isLocked || !uiState.gesturesEnabled) return@detectTapGestures
                                val w = containerSize.width.toFloat().coerceAtLeast(1f)
                                when {
                                    offset.x < w / 3f -> {
                                        viewModel.seekBy(-10_000L)
                                        doubleTapBurstText = "-10s"
                                    }
                                    offset.x > (2f * w) / 3f -> {
                                        viewModel.seekBy(10_000L)
                                        doubleTapBurstText = "+10s"
                                    }
                                    else -> {
                                        viewModel.togglePlayPause()
                                        doubleTapBurstText = if (uiState.isPlaying) "Pause" else "Play"
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(uiState.isLocked, uiState.gesturesEnabled, containerSize, uiState.durationMs) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (uiState.isLocked || !uiState.gesturesEnabled) return@awaitEachGesture

                            val startX = down.position.x
                            val startY = down.position.y
                            var lastX = startX
                            var lastY = startY
                            var mode = ActiveGestureMode.NONE
                            var initialSeekPos = viewModel.player.currentPosition.coerceAtLeast(0L)
                            var accumulatedSeekDelta = 0L
                            var zoomAccum = uiState.zoomScale

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount == 0) break

                                if (pressedCount >= 2) {
                                    val zoomChange = event.calculateZoom()
                                    if (zoomChange != 1f) {
                                        mode = ActiveGestureMode.ZOOM
                                        activeGesture = ActiveGestureMode.ZOOM
                                        zoomAccum = (zoomAccum * zoomChange).coerceIn(0.5f, 4.0f)
                                        viewModel.updateZoomScale(zoomAccum)
                                        gestureFeedbackText = "Zoom ${(zoomAccum * 100).toInt()}%"
                                        gestureProgress = ((zoomAccum - 0.5f) / 3.5f).coerceIn(0f, 1f)
                                        event.changes.forEach { it.consume() }
                                    }
                                    continue
                                }

                                val change = event.changes.first()
                                val dx = change.position.x - lastX
                                val dy = change.position.y - lastY
                                lastX = change.position.x
                                lastY = change.position.y

                                val totalDx = change.position.x - startX
                                val totalDy = change.position.y - startY

                                if (mode == ActiveGestureMode.NONE) {
                                    val slop = viewConfiguration.touchSlop * 1.4f
                                    if (abs(totalDx) > slop || abs(totalDy) > slop) {
                                        mode = when {
                                            abs(totalDx) > abs(totalDy) -> ActiveGestureMode.SEEK
                                            startX < containerSize.width / 2f -> ActiveGestureMode.BRIGHTNESS
                                            else -> ActiveGestureMode.VOLUME
                                        }
                                        activeGesture = mode
                                        initialSeekPos = viewModel.player.currentPosition.coerceAtLeast(0L)
                                    }
                                }

                                when (mode) {
                                    ActiveGestureMode.SEEK -> {
                                        change.consume()
                                        val w = containerSize.width.toFloat().coerceAtLeast(1f)
                                        val deltaMs = ((dx / w) * 90_000f).toLong()
                                        accumulatedSeekDelta += deltaMs
                                        val dur = uiState.durationMs.takeIf { it > 0 } ?: 1L
                                        val target = (initialSeekPos + accumulatedSeekDelta).coerceIn(0L, dur)
                                        isScrubbing = true
                                        scrubPositionMs = target
                                        val sign = if (accumulatedSeekDelta >= 0) "+" else "-"
                                        gestureFeedbackText =
                                            "${FormatUtils.formatDuration(target)} ($sign${FormatUtils.formatDuration(abs(accumulatedSeekDelta))})"
                                        gestureProgress = (target.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                                    }
                                    ActiveGestureMode.BRIGHTNESS -> {
                                        change.consume()
                                        val h = containerSize.height.toFloat().coerceAtLeast(1f)
                                        currentBrightness = (currentBrightness - (dy / h)).coerceIn(0.01f, 1.0f)
                                        activity?.window?.let { win ->
                                            val attrs = win.attributes
                                            attrs.screenBrightness = currentBrightness
                                            win.attributes = attrs
                                        }
                                        gestureFeedbackText = "Brightness ${(currentBrightness * 100).toInt()}%"
                                        gestureProgress = currentBrightness
                                    }
                                    ActiveGestureMode.VOLUME -> {
                                        change.consume()
                                        val h = containerSize.height.toFloat().coerceAtLeast(1f)
                                        currentVolumeFloat = (currentVolumeFloat - (dy / h)).coerceIn(0f, 1f)
                                        val newVol = (currentVolumeFloat * maxVolume).toInt().coerceIn(0, maxVolume)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                        gestureFeedbackText = "Volume ${(currentVolumeFloat * 100).toInt()}%"
                                        gestureProgress = currentVolumeFloat
                                    }
                                    else -> Unit
                                }
                            }

                            if (mode == ActiveGestureMode.SEEK) {
                                viewModel.seekTo(scrubPositionMs)
                                isScrubbing = false
                            }
                            activeGesture = ActiveGestureMode.NONE
                            gestureFeedbackText = null
                        }
                    }
            )
        }

        // Buffering Indicator
        if (uiState.isBuffering && uiState.errorMessage == null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Visual Gesture HUD
        if (activeGesture != ActiveGestureMode.NONE && gestureFeedbackText != null) {
            Surface(
                color = Color(0xCC0D0D12),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val hudIcon = when (activeGesture) {
                        ActiveGestureMode.BRIGHTNESS -> Icons.Rounded.BrightnessHigh
                        ActiveGestureMode.VOLUME -> Icons.AutoMirrored.Rounded.VolumeUp
                        ActiveGestureMode.ZOOM -> Icons.Rounded.ZoomOutMap
                        else -> Icons.Rounded.Forward10
                    }
                    Icon(
                        imageVector = hudIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = gestureFeedbackText!!,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { gestureProgress },
                        modifier = Modifier.width(140.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }
        }

        // Double-Tap Burst Feedback
        if (doubleTapBurstText != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
            ) {
                Text(
                    text = doubleTapBurstText!!,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        // Error & Retry Overlay
        if (uiState.errorMessage != null) {
            Surface(
                color = Color(0xEE141418),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Playback Error", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { viewModel.retryPlayback() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                        Button(onClick = { viewModel.onExitPlayer(); onNavigateBack() }) {
                            Text("Close")
                        }
                    }
                }
            }
        }

        // Controls Overlay (Hidden during PiP)
        if (!uiState.isPiPActive) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.isLocked) {
                    // Lock Screen Controls Mode
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(16.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.72f),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .clip(CircleShape)
                                .clickable { viewModel.toggleLock() }
                                .testTag("player_unlock_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.LockOpen, contentDescription = "Unlock player", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlock", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.78f),
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .safeDrawingPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Glass Pill Bar
                        Surface(
                            color = Color(0x9E0B0B10),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    0.75.dp,
                                    Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(20.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.onExitPlayer()
                                        onNavigateBack()
                                    },
                                    modifier = Modifier.testTag("player_back_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.currentVideo?.title ?: "Playing",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (uiState.sleepTimerRemainingSec != null) {
                                            Text(
                                                text = "Sleep ${FormatUtils.formatDuration(uiState.sleepTimerRemainingSec!! * 1000L)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (uiState.abPointAMs != null) {
                                            Text(
                                                text = if (uiState.abPointBMs != null) "A-B Loop Active" else "Point A Set",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (uiState.zoomScale != 1.0f) {
                                            Text(
                                                text = "Zoom ${(uiState.zoomScale * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { showSubtitleSheet = true }, modifier = Modifier.testTag("player_subtitles_button")) {
                                    Icon(Icons.Rounded.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                                }
                                IconButton(onClick = { showAudioSheet = true }, modifier = Modifier.testTag("player_audio_button")) {
                                    Icon(Icons.Rounded.Equalizer, contentDescription = "Audio and Equalizer", tint = Color.White)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    IconButton(onClick = { enterPiPMode() }, modifier = Modifier.testTag("player_pip_button")) {
                                        Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = "Picture in Picture", tint = Color.White)
                                    }
                                }
                                IconButton(onClick = { showToolsSheet = true }, modifier = Modifier.testTag("player_tools_button")) {
                                    Icon(Icons.Rounded.Tune, contentDescription = "More player tools", tint = Color.White)
                                }
                            }
                        }

                        // Center Transport Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.playPrevious() },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x880B0B10))
                            ) {
                                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous video", tint = Color.White, modifier = Modifier.size(30.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = { viewModel.seekBy(-10_000L) },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x880B0B10))
                            ) {
                                Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.togglePlayPause() }
                                        .testTag("player_play_pause_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            IconButton(
                                onClick = { viewModel.seekBy(10_000L) },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x880B0B10))
                            ) {
                                Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = { viewModel.playNext() },
                                enabled = uiState.hasNext,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x880B0B10))
                            ) {
                                Icon(
                                    Icons.Rounded.SkipNext,
                                    contentDescription = "Next video",
                                    tint = if (uiState.hasNext) Color.White else Color.White.copy(alpha = 0.35f),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        // Bottom Glass Seekbar & Quick Action Panel
                        Surface(
                            color = Color(0xAD0B0B10),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    0.75.dp,
                                    Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(22.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                val displayPos = if (isScrubbing) scrubPositionMs else uiState.positionMs
                                val maxDur = uiState.durationMs.coerceAtLeast(1L)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = FormatUtils.formatDuration(displayPos),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                    Slider(
                                        value = displayPos.toFloat().coerceIn(0f, maxDur.toFloat()),
                                        onValueChange = {
                                            isScrubbing = true
                                            scrubPositionMs = it.toLong()
                                        },
                                        onValueChangeFinished = {
                                            viewModel.seekTo(scrubPositionMs)
                                            isScrubbing = false
                                        },
                                        valueRange = 0f..maxDur.toFloat(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                            .testTag("player_seekbar")
                                    )
                                    Text(
                                        text = FormatUtils.formatDuration(uiState.durationMs),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { viewModel.toggleLock() }, modifier = Modifier.testTag("player_lock_button")) {
                                        Icon(Icons.Rounded.Lock, contentDescription = "Lock controls", tint = Color.White)
                                    }
                                    Surface(
                                        color = Color.White.copy(alpha = 0.12f),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .border(0.75.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                                            .clickable { showToolsSheet = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${uiState.playbackSpeed}x", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                    Surface(
                                        color = Color.White.copy(alpha = 0.12f),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .border(0.75.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                                            .clickable { viewModel.cycleResizeMode() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.AspectRatio, contentDescription = "Aspect ratio mode", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(uiState.resizeMode.label, color = Color.White, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.cycleOrientationMode() }) {
                                        Icon(Icons.Rounded.ScreenRotation, contentDescription = "Rotate screen", tint = Color.White)
                                    }
                                    IconButton(onClick = { viewModel.toggleRotationLock() }) {
                                        Icon(
                                            Icons.Rounded.ScreenLockRotation,
                                            contentDescription = "Lock rotation",
                                            tint = if (uiState.isRotationLocked) MaterialTheme.colorScheme.primary else Color.White
                                        )
                                    }
                                    IconButton(onClick = { showToolsSheet = true }) {
                                        Icon(Icons.Rounded.Bedtime, contentDescription = "Sleep timer and A-B loop", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubtitleSheet) {
        SubtitleSettingsSheet(
            uiState = uiState,
            onDismiss = { showSubtitleSheet = false },
            onPickExternalSubtitle = onPickExternalSubtitle,
            onSelectTrack = { viewModel.selectSubtitleTrack(it) },
            onAdjustOffset = { viewModel.adjustSubtitleOffset(it) },
            onUpdateStyle = { size, color, pos -> viewModel.updateSubtitleStyle(size, color, pos) }
        )
    }

    if (showAudioSheet) {
        AudioAndEqSheet(
            uiState = uiState,
            fxState = fxState,
            onDismiss = { showAudioSheet = false },
            onSelectAudioTrack = { viewModel.selectAudioTrack(it) },
            onToggleMono = { viewModel.setMonoMode(it) },
            onSelectPreset = { viewModel.selectEqualizerPreset(it) },
            onSetBandLevel = { band, lvl -> viewModel.setEqualizerBand(band, lvl) },
            onSetBassBoost = { viewModel.setBassBoost(it) },
            onSetVirtualizer = { viewModel.setVirtualizer(it) },
            onSetLoudnessGain = { viewModel.setLoudnessGain(it) }
        )
    }

    if (showToolsSheet) {
        SpeedAndToolsSheet(
            uiState = uiState,
            onDismiss = { showToolsSheet = false },
            onSetSpeed = { viewModel.setPlaybackSpeed(it) },
            onSetResizeMode = { viewModel.setResizeMode(it) },
            onMarkA = { viewModel.markPointA() },
            onMarkB = { viewModel.markPointB() },
            onClearAB = { viewModel.clearABRepeat() },
            onSetSleepTimer = { viewModel.setSleepTimerMinutes(it) },
            onToggleAutoPlayNext = { viewModel.setAutoPlayNext(it) },
            onToggleBackgroundPlayback = { viewModel.setBackgroundPlayback(it) }
        )
    }
}

private fun safePipRational(width: Int, height: Int): Rational {
    if (width <= 0 || height <= 0) return Rational(16, 9)
    val ratio = width.toFloat() / height.toFloat()
    return when {
        ratio > 2.35f -> Rational(235, 100)
        ratio < (1f / 2.35f) -> Rational(100, 235)
        else -> Rational(width, height)
    }
}

internal fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
