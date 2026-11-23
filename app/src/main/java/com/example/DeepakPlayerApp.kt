package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.ads.AdManager
import com.example.data.local.AppDatabase
import com.example.data.media.DeviceVideoScanner
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.VideoLibraryRepository
import com.example.player.PlayerHolder

class DeepakPlayerApp : Application(), ImageLoaderFactory {

    lateinit var database: AppDatabase
        private set
    lateinit var preferencesRepository: UserPreferencesRepository
        private set
    lateinit var videoScanner: DeviceVideoScanner
        private set
    lateinit var libraryRepository: VideoLibraryRepository
        private set
    lateinit var playerHolder: PlayerHolder
        private set
    lateinit var adManager: AdManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)
        videoScanner = DeviceVideoScanner(this)
        libraryRepository = VideoLibraryRepository(database.appDao(), videoScanner)
        playerHolder = PlayerHolder(this)
        adManager = AdManager(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("video_thumbs"))
                    .maxSizeBytes(64L * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    override fun onTerminate() {
        playerHolder.release()
        super.onTerminate()
    }
}
