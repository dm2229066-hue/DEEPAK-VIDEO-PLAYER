package com.example

import com.example.data.media.SubtitleHelper
import com.example.data.model.FormatUtils
import com.example.data.model.SortOption
import com.example.data.model.VideoItem
import com.example.data.model.ViewMode
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.OrientationPreference
import com.example.ui.theme.AccentColorOption
import com.example.ui.player.calculateSwipeSeek
import com.example.ui.viewmodel.SeekStateTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepakPlayerUnitTest {

    @Test
    fun formatUtilsFormatsDurationsAndSizesAccurately() {
        assertEquals("00:00", FormatUtils.formatDuration(0L))
        assertEquals("01:05", FormatUtils.formatDuration(65_000L))
        assertEquals("1:01:05", FormatUtils.formatDuration(3_665_000L))
        assertEquals("1.0 MB", FormatUtils.formatFileSize(1024L * 1024L))
        assertEquals("0 B", FormatUtils.formatFileSize(0L))
    }

    @Test
    fun subtitleHelperShiftsSrtVttAndAssTimestamps() {
        val srt = """
            1
            00:00:01,000 --> 00:00:03,500
            Welcome to Deepak Video Player
        """.trimIndent()
        val shiftedSrt = SubtitleHelper.shiftSrtOrVtt(srt, 1500L, isVtt = false)
        assertTrue(shiftedSrt.contains("00:00:02,500 --> 00:00:05,000"))

        val clampedSrt = SubtitleHelper.shiftSrtOrVtt(srt, -5000L, isVtt = false)
        assertTrue(clampedSrt.contains("00:00:00,000 --> 00:00:00,200"))

        val vtt = "00:05.000 --> 00:08.500\nSample VTT"
        val shiftedVtt = SubtitleHelper.shiftSrtOrVtt(vtt, 2000L, isVtt = true)
        assertTrue(shiftedVtt.contains("00:00:07.000 --> 00:00:10.500"))

        val ass = "Dialogue: 0,0:00:10.00,0:00:12.50,Default,,0,0,0,,Line One"
        val shiftedAss = SubtitleHelper.shiftAss(ass, 2000L)
        assertTrue(shiftedAss.contains("0:00:12.00,0:00:14.50"))
    }

    @Test
    fun videoItemComputesResolutionLabelsAndProgress() {
        val video4k = VideoItem(
            id = "1",
            uri = "content://video/1",
            title = "4K Clip",
            displayName = "4k.mp4",
            folderName = "Movies",
            folderPath = "Movies",
            durationMs = 100_000L,
            sizeBytes = 50_000_000L,
            dateAddedSec = 1000L,
            dateModifiedSec = 1000L,
            mimeType = "video/mp4",
            width = 3840,
            height = 2160,
            lastPositionMs = 25_000L
        )
        assertEquals("4K", video4k.resolutionLabel)
        assertEquals(0.25f, video4k.progressFraction, 0.001f)
    }

    @Test
    fun preferencesAndAccentOptionsResolveCorrectly() {
        assertEquals(AccentColorOption.GOLD, AccentColorOption.fromId("unknown"))
        assertFalse(AccentColorOption.GOLD.requiresRewardUnlock)
        assertTrue(AccentColorOption.CRIMSON.requiresRewardUnlock)
        assertTrue(AccentColorOption.CYAN.requiresRewardUnlock)
        assertEquals(SortOption.DURATION, SortOption.fromId("duration"))
        assertEquals(ViewMode.GRID, ViewMode.fromId("GRID"))
        assertEquals(OrientationPreference.LANDSCAPE, OrientationPreference.fromId("LANDSCAPE"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromCode("hi"))
    }

    @Test
    fun swipeSeekMovesForwardOnRightSwipeAndBackwardOnLeftSwipeAcrossDurations() {
        // Long 2-hour video: swipe right moves strictly forward, swipe left moves backward
        val longForward = calculateSwipeSeek(
            initialPositionMs = 60_000L,
            deltaPx = 540f,
            containerWidthPx = 1080f,
            durationMs = 7_200_000L
        )
        assertEquals(105_000L, longForward.targetMs)
        assertEquals(45_000L, longForward.appliedDeltaMs)

        val longBackward = calculateSwipeSeek(
            initialPositionMs = 60_000L,
            deltaPx = -270f,
            containerWidthPx = 1080f,
            durationMs = 7_200_000L
        )
        assertEquals(37_500L, longBackward.targetMs)
        assertEquals(-22_500L, longBackward.appliedDeltaMs)

        // Short 15s video: scales smoothly without overshooting on small swipes
        val shortForward = calculateSwipeSeek(
            initialPositionMs = 3_000L,
            deltaPx = 270f,
            containerWidthPx = 1080f,
            durationMs = 15_000L
        )
        assertEquals(6_750L, shortForward.targetMs)
        assertTrue(shortForward.targetMs > 3_000L)

        // Unknown duration (0L) must NOT clamp forward seeks back to 00:00
        val unknownDurationForward = calculateSwipeSeek(
            initialPositionMs = 90_000L,
            deltaPx = 270f,
            containerWidthPx = 1080f,
            durationMs = 0L
        )
        assertEquals(112_500L, unknownDurationForward.targetMs)
        assertEquals(22_500L, unknownDurationForward.appliedDeltaMs)
    }

    @Test
    fun seekStateTrackerPreventsStalePositionsAndAccumulatesRapidSeeks() {
        var clockMs = 1_000L
        val tracker = SeekStateTracker(nowMs = { clockMs })

        // User seeks forward from 10s -> 20s -> 30s -> 40s rapidly while ExoPlayer still reports 10s
        tracker.beginSeek(targetMs = 20_000L, originMs = 10_000L)
        clockMs += 60L
        val base2 = tracker.effectivePositionMs(playerPositionMs = 10_000L, isReady = false)
        assertEquals(20_000L, base2)
        tracker.beginSeek(targetMs = base2 + 10_000L, originMs = 10_000L)

        clockMs += 60L
        val base3 = tracker.effectivePositionMs(playerPositionMs = 10_000L, isReady = true)
        assertEquals(30_000L, base3)
        tracker.beginSeek(targetMs = base3 + 10_000L, originMs = 10_000L)

        // Stale ticker read at 250ms must NOT overwrite 40s back to 10s
        clockMs += 200L
        assertEquals(40_000L, tracker.resolveTickerPosition(playerPositionMs = 10_000L, isReady = false, isPlaying = true))
        assertEquals(40_000L, tracker.resolveTickerPosition(playerPositionMs = 10_000L, isReady = true, isPlaying = true))

        // Rapid backward tap (-10s) from 40s -> 30s
        val baseBack = tracker.effectivePositionMs(playerPositionMs = 10_000L, isReady = false)
        assertEquals(40_000L, baseBack)
        tracker.beginSeek(targetMs = baseBack - 10_000L, originMs = 10_000L)

        // Once ExoPlayer settles near 30s in STATE_READY, tracker clears and tracks playback
        clockMs += 150L
        val settled = tracker.resolveTickerPosition(playerPositionMs = 30_120L, isReady = true, isPlaying = true)
        assertEquals(30_120L, settled)
        assertEquals(null, tracker.pendingTargetMs)
    }

    @Test
    fun seekbarDragAndShortVsLongVideosMaintainExactDirectionAndBounds() {
        var clockMs = 5_000L
        val tracker = SeekStateTracker(nowMs = { clockMs })

        // Short 8-second video: +10s clamps cleanly to 8_000ms, -10s clamps to 0ms
        val shortDurationMs = 8_000L
        val shortForwardTarget = SeekStateTracker.clampSeekTarget(2_000L + 10_000L, shortDurationMs)
        assertEquals(8_000L, shortForwardTarget)
        val shortBackwardTarget = SeekStateTracker.clampSeekTarget(3_000L - 10_000L, shortDurationMs)
        assertEquals(0L, shortBackwardTarget)

        // Long 2-hour video: dragging seekbar forward then backward preserves exact direction
        val longDurationMs = 7_200_000L
        val dragForwardRawFloat = 3_600_000f // 1:00:00
        val dragForwardTarget = SeekStateTracker.clampSeekTarget(
            kotlin.math.round(dragForwardRawFloat.toDouble()).toLong(),
            longDurationMs
        )
        assertEquals(3_600_000L, dragForwardTarget)
        assertEquals("1:00:00", FormatUtils.formatDuration(dragForwardTarget))

        tracker.beginSeek(targetMs = dragForwardTarget, originMs = 120_000L)
        // Even if ExoPlayer buffers and briefly reports old 120s, ticker must hold 3_600_000ms
        clockMs += 250L
        assertEquals(
            3_600_000L,
            tracker.resolveTickerPosition(playerPositionMs = 120_000L, isReady = false, isPlaying = true)
        )

        // Drag seekbar backward to 00:30:00 (1_800_000ms)
        val dragBackwardTarget = SeekStateTracker.clampSeekTarget(1_800_000L, longDurationMs)
        assertEquals(1_800_000L, dragBackwardTarget)
        tracker.beginSeek(targetMs = dragBackwardTarget, originMs = 3_600_000L)
        clockMs += 250L
        assertEquals(
            1_800_000L,
            tracker.resolveTickerPosition(playerPositionMs = 3_600_000L, isReady = false, isPlaying = true)
        )

        // After settle timeout expires while ready at a nearby keyframe, tracker releases cleanly
        clockMs += 2_000L
        val released = tracker.resolveTickerPosition(playerPositionMs = 1_798_500L, isReady = true, isPlaying = true)
        assertEquals(1_798_500L, released)
        assertEquals(null, tracker.pendingTargetMs)
    }
}
