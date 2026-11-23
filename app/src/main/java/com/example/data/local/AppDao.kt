package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Playback History
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC")
    fun observePlaybackHistory(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE uri = :uri LIMIT 1")
    suspend fun getHistoryForUri(uri: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE uri = :uri")
    suspend fun deleteHistoryForUri(uri: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearPlaybackHistory()

    // Favorites
    @Query("SELECT * FROM favorite_videos ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteVideoEntity)

    @Query("DELETE FROM favorite_videos WHERE uri = :uri")
    suspend fun removeFavorite(uri: String)

    // Hidden Folders
    @Query("SELECT * FROM hidden_folders ORDER BY folderName ASC")
    fun observeHiddenFolders(): Flow<List<HiddenFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideFolder(folder: HiddenFolderEntity)

    @Query("DELETE FROM hidden_folders WHERE folderPath = :folderPath")
    suspend fun unhideFolder(folderPath: String)

    // SAF Mounted Folders
    @Query("SELECT * FROM saf_trees ORDER BY addedAt DESC")
    fun observeSafTrees(): Flow<List<SafTreeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSafTree(tree: SafTreeEntity)

    @Query("DELETE FROM saf_trees WHERE treeUri = :treeUri")
    suspend fun removeSafTree(treeUri: String)

    // Playlists
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylistsWithItems(): Flow<List<PlaylistWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addVideoToPlaylist(crossRef: PlaylistVideoCrossRef)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND videoUri = :videoUri")
    suspend fun removeVideoFromPlaylist(playlistId: Long, videoUri: String)

    // Network Streams
    @Query("SELECT * FROM network_streams ORDER BY lastPlayedAt DESC LIMIT 20")
    fun observeNetworkStreams(): Flow<List<NetworkStreamHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNetworkStream(stream: NetworkStreamHistoryEntity)

    @Query("DELETE FROM network_streams WHERE url = :url")
    suspend fun deleteNetworkStream(url: String)
}
