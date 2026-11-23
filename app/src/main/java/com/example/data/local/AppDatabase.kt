package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaybackHistoryEntity::class,
        FavoriteVideoEntity::class,
        HiddenFolderEntity::class,
        SafTreeEntity::class,
        PlaylistEntity::class,
        PlaylistVideoCrossRef::class,
        NetworkStreamHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "deepak_video_player.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
