package com.navidrometv.domain.usecase

import com.navidrometv.core.common.AppError
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.common.ErrorKind
import com.navidrometv.core.model.PlaybackRequest
import com.navidrometv.core.model.Track
import com.navidrometv.domain.repository.MusicRepository
import com.navidrometv.domain.repository.PlaylistRepository
import com.navidrometv.playback.PlaybackRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single place where "Play album", "Shuffle artist", "Play playlist" are resolved
 * into a concrete queue. Screens never assemble queues themselves, so play
 * behaviour stays consistent everywhere.
 */
@Singleton
class PlayMediaUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val playbackRepository: PlaybackRepository,
) {

    suspend fun playAlbum(albumId: String, startIndex: Int = 0, shuffle: Boolean = false): DataResult<Unit> {
        val result = musicRepository.getAlbum(albumId)
        return when (result) {
            is DataResult.Failure -> result
            is DataResult.Success -> {
                val tracks = result.data.tracks
                if (tracks.isEmpty()) {
                    DataResult.Failure(AppError(ErrorKind.NOT_FOUND, "This album has no playable tracks"))
                } else {
                    playbackRepository.playTracks(
                        PlaybackRequest(
                            tracks = tracks,
                            startIndex = startIndex.coerceIn(0, tracks.lastIndex),
                            shuffle = shuffle,
                        ),
                    )
                    DataResult.Success(Unit)
                }
            }
        }
    }

    suspend fun playTracks(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false): DataResult<Unit> {
        if (tracks.isEmpty()) {
            return DataResult.Failure(AppError(ErrorKind.NOT_FOUND, "There is nothing to play"))
        }
        playbackRepository.playTracks(
            PlaybackRequest(
                tracks = tracks,
                startIndex = startIndex.coerceIn(0, tracks.lastIndex),
                shuffle = shuffle,
            ),
        )
        return DataResult.Success(Unit)
    }

    suspend fun playPlaylist(playlistId: String, shuffle: Boolean = false): DataResult<Unit> {
        val result = playlistRepository.getPlaylist(playlistId)
        return when (result) {
            is DataResult.Failure -> result
            is DataResult.Success -> playTracks(result.data.tracks, shuffle = shuffle)
        }
    }

    /** Plays the artist's server-side top songs, falling back to the first album. */
    suspend fun playArtist(artistId: String, shuffle: Boolean = false): DataResult<Unit> {
        val detail = musicRepository.getArtist(artistId)
        return when (detail) {
            is DataResult.Failure -> detail
            is DataResult.Success -> {
                if (detail.data.topTracks.isNotEmpty()) {
                    playTracks(detail.data.topTracks, shuffle = shuffle)
                } else {
                    val firstAlbum = detail.data.albums.firstOrNull()
                        ?: return DataResult.Failure(
                            AppError(ErrorKind.NOT_FOUND, "This artist has no albums to play"),
                        )
                    playAlbum(firstAlbum.id, shuffle = shuffle)
                }
            }
        }
    }

    fun addToQueue(track: Track) = playbackRepository.addToQueue(track)

    fun addToQueueAll(tracks: List<Track>) = playbackRepository.addAllToQueue(tracks)

    fun playNext(track: Track) = playbackRepository.playNext(track)
}
