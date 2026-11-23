package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val durationMs: Long,
    val lastPositionMs: Long,
    val lastPlayedAt: Long,
    val playCount: Int = 1,
    val subtitleOffsetMs: Long = 0L,
    val audioOffsetMs: Long = 0L,
    val preferredSpeed: Float = 1.0f,
    val externalSubtitleUri: String? = null
)

@Entity(tableName = "favorite_videos")
data class FavoriteVideoEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val addedAt: Long
)

@Entity(tableName = "hidden_folders")
data class HiddenFolderEntity(
    @PrimaryKey val folderPath: String,
    val folderName: String,
    val hiddenAt: Long
)

@Entity(tableName = "saf_trees")
data class SafTreeEntity(
    @PrimaryKey val treeUri: String,
    val displayName: String,
    val addedAt: Long
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long
)

@Entity(
    tableName = "playlist_videos",
    primaryKeys = ["playlistId", "videoUri"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["videoUri"])]
)
data class PlaylistVideoCrossRef(
    val playlistId: Long,
    val videoUri: String,
    val videoTitle: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val addedAt: Long
)

data class PlaylistWithItems(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val items: List<PlaylistVideoCrossRef>
)

@Entity(tableName = "network_streams")
data class NetworkStreamHistoryEntity(
    @PrimaryKey val url: String,
    val title: String,
    val lastPlayedAt: Long
)
