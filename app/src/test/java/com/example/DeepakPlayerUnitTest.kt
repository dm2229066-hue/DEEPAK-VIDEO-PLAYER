package com.example

import com.example.data.media.SubtitleHelper
import com.example.data.model.FormatUtils
import com.example.data.model.SortOption
import com.example.data.model.VideoItem
import com.example.data.model.ViewMode
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.OrientationPreference
import com.example.ui.theme.AccentColorOption
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
}
