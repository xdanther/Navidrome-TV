package com.navidrometv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.navidrometv.core.model.Album
import com.navidrometv.core.model.Track

@Database(
    entities = [PlayHistoryEntity::class, CachedAlbumEntity::class, CachedTrackEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NavidromeDatabase : RoomDatabase() {
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun musicCacheDao(): MusicCacheDao
}

// Cache mappers -------------------------------------------------------------

fun Album.toCachedEntity(listType: String, position: Int, now: Long): CachedAlbumEntity = CachedAlbumEntity(
    id = id,
    listType = listType,
    name = name,
    artistId = artistId,
    artistName = artistName,
    year = year,
    genre = genre,
    trackCount = trackCount,
    durationSeconds = durationSeconds,
    coverArtId = coverArtId,
    isFavorite = isFavorite,
    created = created,
    cachedAt = now,
    position = position,
)

fun CachedAlbumEntity.toAlbum(): Album = Album(
    id = id,
    name = name,
    artistId = artistId,
    artistName = artistName,
    year = year,
    genre = genre,
    trackCount = trackCount,
    durationSeconds = durationSeconds,
    coverArtId = coverArtId,
    isFavorite = isFavorite,
    created = created,
)

fun Track.toCachedEntity(now: Long): CachedTrackEntity = CachedTrackEntity(
    id = id,
    albumId = albumId,
    albumName = albumName,
    title = title,
    artistId = artistId,
    artistName = artistName,
    durationMs = durationMs,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    genre = genre,
    isFavorite = isFavorite,
    coverArtId = coverArtId,
    suffix = suffix,
    bitRateKbps = bitRateKbps,
    contentType = contentType,
    sizeBytes = sizeBytes,
    cachedAt = now,
)

fun CachedTrackEntity.toTrack(): Track = Track(
    id = id,
    title = title,
    albumId = albumId,
    albumName = albumName,
    artistId = artistId,
    artistName = artistName,
    durationMs = durationMs,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    genre = genre,
    isFavorite = isFavorite,
    coverArtId = coverArtId,
    suffix = suffix,
    bitRateKbps = bitRateKbps,
    contentType = contentType,
    sizeBytes = sizeBytes,
)

fun PlayHistoryEntity.toTrack(): Track = Track(
    id = trackId,
    title = title,
    albumId = albumId,
    albumName = albumName,
    artistId = artistId,
    artistName = artistName,
    coverArtId = coverArtId,
)
