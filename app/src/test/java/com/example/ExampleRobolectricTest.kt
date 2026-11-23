package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteVideoEntity
import com.example.data.local.PlaybackHistoryEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistVideoCrossRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun setupDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDownDb() {
        db.close()
    }

    @Test
    fun appNameMatchesDeepakVideoPlayer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Deepak Video Player", context.getString(R.string.app_name))
    }

    @Test
    fun roomDatabaseStoresHistoryFavoritesAndPlaylists() = runTest {
        val dao = db.appDao()

        dao.upsertHistory(
            PlaybackHistoryEntity(
                uri = "content://media/external/video/media/42",
                title = "Cinema Demo",
                durationMs = 120_000L,
                lastPositionMs = 45_000L,
                lastPlayedAt = 1_700_000_000L
            )
        )
        val history = dao.getHistoryForUri("content://media/external/video/media/42")
        assertEquals(45_000L, history?.lastPositionMs)

        dao.addFavorite(FavoriteVideoEntity("content://media/external/video/media/42", "Cinema Demo", 100L))
        assertEquals(1, dao.observeFavorites().first().size)

        val playlistId = dao.insertPlaylist(PlaylistEntity(name = "Action", createdAt = 100L))
        dao.addVideoToPlaylist(
            PlaylistVideoCrossRef(
                playlistId = playlistId,
                videoUri = "content://media/external/video/media/42",
                videoTitle = "Cinema Demo",
                durationMs = 120_000L,
                sizeBytes = 10_000L,
                addedAt = 100L
            )
        )
        val playlists = dao.observePlaylistsWithItems().first()
        assertEquals(1, playlists.size)
        assertEquals("Action", playlists.first().playlist.name)
        assertEquals(1, playlists.first().items.size)
    }
}
