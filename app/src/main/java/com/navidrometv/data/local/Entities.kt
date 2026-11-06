package com.navidrometv.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per completed play. History drives Recently Played, Most Played and the
 * Home personalisation, so it is written locally even when scrobbling is disabled.
 */
@Entity(
    tableName = "play_history",
    indices = [Index("playedAt"), Index("trackId"), Index("albumId")],
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val title: String,
    val artistId: String? = null,
    val artistName: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val coverArtId: String? = null,
    val playedAt: Long,
    val scrobbled: Boolean = false,
)

/** Cached album lists, so the last good Home/New content survives a dropped link. */
@Entity(
    tableName = "cached_albums",
    primaryKeys = ["id", "listType"],
    indices = [Index("cachedAt")],
)
data class CachedAlbumEntity(
    val id: String,
    val listType: String,
    val name: String,
    val artistId: String? = null,
    val artistName: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val trackCount: Int? = null,
    val durationSeconds: Int? = null,
    val coverArtId: String? = null,
    val isFavorite: Boolean = false,
    val created: String? = null,
    val cachedAt: Long,
    val position: Int = 0,
)

/** Cached track metadata for album details and the persisted playback queue. */
@Entity(
    tableName = "cached_tracks",
    indices = [Index("albumId")],
)
data class CachedTrackEntity(
    @PrimaryKey val id: String,
    val albumId: String? = null,
    val albumName: String? = null,
    val title: String,
    val artistId: String? = null,
    val artistName: String? = null,
    val durationMs: Long = 0,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val isFavorite: Boolean = false,
    val coverArtId: String? = null,
    val suffix: String? = null,
    val bitRateKbps: Int? = null,
    val contentType: String? = null,
    val sizeBytes: Long? = null,
    val cachedAt: Long,
)

// Projections ---------------------------------------------------------------

data class AlbumHistoryRow(
    val albumId: String,
    val albumName: String?,
    val artistName: String?,
    val coverArtId: String?,
    val lastPlayedAt: Long,
    val plays: Int,
)

data class ArtistHistoryRow(
    val artistName: String,
    val artistId: String?,
    val coverArtId: String?,
    val lastPlayedAt: Long,
    val plays: Int,
)

data class MostPlayedRow(
    val trackId: String,
    val title: String,
    val artistName: String?,
    val albumId: String?,
    val albumName: String?,
    val coverArtId: String?,
    val plays: Int,
    val lastPlayedAt: Long,
)
