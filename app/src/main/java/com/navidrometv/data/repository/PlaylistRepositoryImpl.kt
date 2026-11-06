package com.navidrometv.data.repository

import com.navidrometv.core.common.DataResult
import com.navidrometv.core.model.Playlist
import com.navidrometv.core.model.Track
import com.navidrometv.data.remote.SubsonicMapper.toPlaylist
import com.navidrometv.data.remote.SubsonicMapper.toTrack
import com.navidrometv.data.remote.SubsonicService
import com.navidrometv.domain.repository.PlaylistDetail
import com.navidrometv.domain.repository.PlaylistRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val service: SubsonicService,
) : PlaylistRepository {

    override suspend fun getPlaylists(): DataResult<List<Playlist>> = service.withSession(
        block = { it.api.getPlaylists() },
        map = { payload ->
            payload.playlists?.playlist.orEmpty()
                .map { it.toPlaylist() }
                .sortedBy { it.name.lowercase() }
        },
    )

    override suspend fun getPlaylist(id: String): DataResult<PlaylistDetail> = service.withSession(
        block = { it.api.getPlaylist(id) },
        map = { payload ->
            val dto = payload.playlist
                ?: throw IllegalStateException("Playlist $id was not returned by the server")
            PlaylistDetail(
                playlist = dto.toPlaylist(),
                tracks = dto.entry.map(TrackMapper::map),
            )
        },
    )

    override suspend fun createPlaylist(name: String, trackIds: List<String>): DataResult<Unit> =
        service.withSession(
            block = { it.api.createPlaylist(name = name.trim(), songIds = trackIds.ifEmpty { null }) },
            map = { },
        )

    override suspend fun renamePlaylist(id: String, name: String): DataResult<Unit> = service.withSession(
        block = { it.api.updatePlaylist(playlistId = id, name = name.trim()) },
        map = { },
    )

    override suspend fun deletePlaylist(id: String): DataResult<Unit> = service.withSession(
        block = { it.api.deletePlaylist(id) },
        map = { },
    )

    override suspend fun addTracks(playlistId: String, trackIds: List<String>): DataResult<Unit> {
        if (trackIds.isEmpty()) return DataResult.Success(Unit)
        return service.withSession(
            block = { it.api.updatePlaylist(playlistId = playlistId, songIdsToAdd = trackIds) },
            map = { },
        )
    }

    override suspend fun removeTrackAt(playlistId: String, index: Int): DataResult<Unit> = service.withSession(
        block = { it.api.updatePlaylist(playlistId = playlistId, songIndexesToRemove = listOf(index)) },
        map = { },
    )

    private object TrackMapper {
        fun map(child: com.navidrometv.data.remote.dto.ChildDto): Track = child.toTrack()
    }
}
