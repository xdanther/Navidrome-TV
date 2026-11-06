package com.navidrometv.data.repository

import com.navidrometv.core.model.Album
import com.navidrometv.core.model.Artist
import com.navidrometv.core.model.Track
import com.navidrometv.data.local.MostPlayedRow
import com.navidrometv.data.local.PlayHistoryDao
import com.navidrometv.data.local.PlayHistoryEntity
import com.navidrometv.data.local.toTrack
import com.navidrometv.domain.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local listening history. Written for every counted play, independent of whether
 * scrobbling to the server is enabled, so personalisation works offline and on
 * servers where scrobbling is restricted.
 */
@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val dao: PlayHistoryDao,
) : HistoryRepository {

    override suspend fun recordPlay(track: Track) {
        dao.insert(
            PlayHistoryEntity(
                trackId = track.id,
                title = track.title,
                artistId = track.artistId,
                artistName = track.artistName,
                albumId = track.albumId,
                albumName = track.albumName,
                coverArtId = track.coverArtId,
                playedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun recentlyPlayedTracks(limit: Int): List<Track> =
        dao.recentTracks(limit).map { it.toTrack() }

    override suspend fun recentlyPlayedAlbums(limit: Int): List<Album> =
        dao.recentAlbums(limit).map { row ->
            Album(
                id = row.albumId,
                name = row.albumName ?: "Unknown album",
                artistName = row.artistName,
                coverArtId = row.coverArtId,
            )
        }

    override suspend fun recentlyPlayedArtists(limit: Int): List<Artist> =
        dao.recentArtists(limit).map { row ->
            Artist(
                id = row.artistId ?: "artist-name:${row.artistName}",
                name = row.artistName,
                coverArtId = row.coverArtId,
            )
        }

    override suspend fun mostPlayedTracks(limit: Int): List<Track> =
        dao.mostPlayed(limit).map { row -> row.toTrack() }

    private fun MostPlayedRow.toTrack(): Track = Track(
        id = trackId,
        title = title,
        albumId = albumId,
        albumName = albumName,
        artistName = artistName,
        coverArtId = coverArtId,
    )

    override suspend fun hasHistory(): Boolean = dao.count() > 0

    override suspend fun clear() = dao.clear()
}
