package com.navidrometv.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface PlayHistoryDao {

    @Insert
    suspend fun insert(entry: PlayHistoryEntity): Long

    @Query("SELECT * FROM play_history WHERE trackId = :trackId ORDER BY playedAt DESC LIMIT 1")
    suspend fun latestFor(trackId: String): PlayHistoryEntity?

    /**
     * Most recent play per track, so a track played five times still appears once
     * in Recently Played.
     */
    @Query(
        """
        SELECT * FROM play_history
        WHERE id IN (SELECT MAX(id) FROM play_history GROUP BY trackId)
        ORDER BY playedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun recentTracks(limit: Int): List<PlayHistoryEntity>

    @Query(
        """
        SELECT albumId AS albumId,
               MAX(albumName) AS albumName,
               MAX(artistName) AS artistName,
               MAX(coverArtId) AS coverArtId,
               MAX(playedAt) AS lastPlayedAt,
               COUNT(*) AS plays
        FROM play_history
        WHERE albumId IS NOT NULL
        GROUP BY albumId
        ORDER BY lastPlayedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun recentAlbums(limit: Int): List<AlbumHistoryRow>

    @Query(
        """
        SELECT artistName AS artistName,
               MAX(artistId) AS artistId,
               MAX(coverArtId) AS coverArtId,
               MAX(playedAt) AS lastPlayedAt,
               COUNT(*) AS plays
        FROM play_history
        WHERE artistName IS NOT NULL AND artistName != ''
        GROUP BY artistName
        ORDER BY lastPlayedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun recentArtists(limit: Int): List<ArtistHistoryRow>

    @Query(
        """
        SELECT trackId AS trackId,
               MAX(title) AS title,
               MAX(artistName) AS artistName,
               MAX(albumId) AS albumId,
               MAX(albumName) AS albumName,
               MAX(coverArtId) AS coverArtId,
               COUNT(*) AS plays,
               MAX(playedAt) AS lastPlayedAt
        FROM play_history
        GROUP BY trackId
        ORDER BY plays DESC, lastPlayedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun mostPlayed(limit: Int): List<MostPlayedRow>

    @Query("SELECT COUNT(*) FROM play_history")
    suspend fun count(): Int

    @Query("SELECT DISTINCT albumId FROM play_history WHERE albumId IS NOT NULL ORDER BY playedAt DESC LIMIT :limit")
    suspend fun recentAlbumIds(limit: Int): List<String>

    @Query("DELETE FROM play_history")
    suspend fun clear()
}

@Dao
interface MusicCacheDao {

    // Note: list replacement (delete + insert) is orchestrated by the repository so
    // this DAO stays a set of plain, individually testable queries.

    @Upsert
    suspend fun upsertAlbums(albums: List<CachedAlbumEntity>)

    @Query("SELECT * FROM cached_albums WHERE listType = :listType ORDER BY position ASC")
    suspend fun albumsForList(listType: String): List<CachedAlbumEntity>

    @Upsert
    suspend fun upsertTracks(tracks: List<CachedTrackEntity>)

    @Query("SELECT * FROM cached_tracks WHERE albumId = :albumId ORDER BY discNumber ASC, trackNumber ASC")
    suspend fun tracksForAlbum(albumId: String): List<CachedTrackEntity>

    @Query("DELETE FROM cached_albums WHERE listType = :listType")
    suspend fun deleteList(listType: String)

    @Query("DELETE FROM cached_tracks WHERE albumId = :albumId")
    suspend fun deleteTracksForAlbum(albumId: String)

    @Query("DELETE FROM cached_albums")
    suspend fun clearAlbums()

    @Query("DELETE FROM cached_tracks")
    suspend fun clearTracks()
}
