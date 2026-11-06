package com.navidrometv.data.repository

import com.navidrometv.core.common.AppLog
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.common.ErrorKind
import com.navidrometv.core.model.Album
import com.navidrometv.core.model.AlbumDetail
import com.navidrometv.core.model.Artist
import com.navidrometv.core.model.ArtistDetail
import com.navidrometv.core.model.ArtistInfo
import com.navidrometv.core.model.Genre
import com.navidrometv.core.model.Lyrics
import com.navidrometv.core.model.SearchResults
import com.navidrometv.core.model.StarredContent
import com.navidrometv.core.model.StreamQuality
import com.navidrometv.core.model.Track
import com.navidrometv.data.local.MusicCacheDao
import com.navidrometv.data.local.toAlbum as toCachedAlbum
import com.navidrometv.data.local.toCachedEntity
import com.navidrometv.data.local.toTrack as toCachedTrack
import com.navidrometv.data.remote.SubsonicMapper
import com.navidrometv.data.remote.SubsonicMapper.flattenArtists
import com.navidrometv.data.remote.SubsonicMapper.toAlbum
import com.navidrometv.data.remote.SubsonicMapper.toArtist
import com.navidrometv.data.remote.SubsonicMapper.toGenre
import com.navidrometv.data.remote.SubsonicMapper.toLyrics
import com.navidrometv.data.remote.SubsonicMapper.toTrack
import com.navidrometv.data.remote.SubsonicService
import com.navidrometv.domain.repository.AlbumListType
import com.navidrometv.domain.repository.MusicRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val service: SubsonicService,
    private val cacheDao: MusicCacheDao,
) : MusicRepository {

    private val now: Long get() = System.currentTimeMillis()

    override suspend fun getAlbumList(
        type: AlbumListType,
        size: Int,
        offset: Int,
        genre: String?,
        fromYear: Int?,
        toYear: Int?,
    ): DataResult<List<Album>> {
        val result = service.withSession(
            block = { session ->
                session.api.getAlbumList2(
                    type = type.apiValue,
                    size = size,
                    offset = offset,
                    fromYear = fromYear,
                    toYear = toYear,
                    genre = genre,
                )
            },
            map = { payload -> payload.albumList2?.album.orEmpty().map { it.toAlbum() } },
        )

        return when (result) {
            is DataResult.Success -> {
                if (offset == 0) cacheAlbumList(type, result.data)
                result
            }
            is DataResult.Failure -> {
                // Only fall back for connectivity problems: a permission or auth
                // failure must surface instead of silently showing stale data.
                val offline = result.error.kind == ErrorKind.NETWORK || result.error.kind == ErrorKind.TIMEOUT
                val cached = if (offline) cacheDao.albumsForList(type.name) else emptyList()
                if (cached.isNotEmpty()) {
                    AppLog.info("Serving ${cached.size} cached albums for ${type.name}")
                    DataResult.Success(cached.map { it.toCachedAlbum() })
                } else {
                    result
                }
            }
        }
    }

    private suspend fun cacheAlbumList(type: AlbumListType, albums: List<Album>) {
        runCatching {
            cacheDao.deleteList(type.name)
            cacheDao.upsertAlbums(
                albums.mapIndexed { index, album -> album.toCachedEntity(type.name, index, now) },
            )
        }.onFailure { AppLog.warn("Album cache write failed") }
    }

    override suspend fun getArtists(): DataResult<List<Artist>> = service.withSession(
        block = { it.api.getArtists() },
        map = { payload -> payload.artists?.index.orEmpty().flattenArtists() },
    )

    override suspend fun getArtist(id: String): DataResult<ArtistDetail> {
        val artistResult = service.withSession(
            block = { it.api.getArtist(id) },
            map = { payload ->
                val dto = payload.artist
                val albums = dto?.album.orEmpty().map { it.toAlbum() }
                val artist = dto?.toArtist() ?: Artist(id = id, name = "Unknown artist")
                ArtistDetail(
                    artist = artist,
                    albums = albums.sortedByDescending { it.year ?: 0 },
                )
            },
        )

        val detail = when (artistResult) {
            is DataResult.Success -> artistResult.data
            is DataResult.Failure -> return artistResult
        }

        // Top songs and biography are supplementary: a server without the
        // getTopSongs/getArtistInfo2 endpoints must still show a usable artist page.
        val topSongs = (service.withSession(
            block = { it.api.getTopSongs(detail.artist.name) },
            map = { payload -> payload.topSongs?.song.orEmpty().map { song -> song.toTrack() } },
        ) as? DataResult.Success)?.data.orEmpty()

        val info = (getArtistInfo(id) as? DataResult.Success)?.data

        return DataResult.Success(
            detail.copy(
                topTracks = topSongs,
                similarArtists = info?.similarArtists.orEmpty(),
            ).let { enriched ->
                enriched.copy(
                    artist = enriched.artist.copy(biography = info?.biography ?: enriched.artist.biography),
                )
            },
        )
    }

    override suspend fun getArtistInfo(id: String): DataResult<ArtistInfo> = service.withSession(
        block = { it.api.getArtistInfo2(id) },
        map = { payload ->
            val info = payload.artistInfo2
            ArtistInfo(
                biography = info?.biography?.takeIf { it.isNotBlank() },
                similarArtists = info?.similarArtist.orEmpty().map { it.toArtist() },
            )
        },
    )

    override suspend fun getAlbum(id: String): DataResult<AlbumDetail> {
        val result = service.withSession(
            block = { it.api.getAlbum(id) },
            map = { payload ->
                val dto = payload.album
                    ?: throw IllegalStateException("Album $id was not returned by the server")
                val album = dto.toAlbum()
                AlbumDetail(
                    album = album,
                    tracks = dto.song.map { it.toTrack() }
                        .sortedWith(compareBy({ it.discNumber ?: 1 }, { it.trackNumber ?: 0 })),
                )
            },
        )

        return when (result) {
            is DataResult.Success -> {
                runCatching {
                    cacheDao.upsertTracks(result.data.tracks.map { it.toCachedEntity(now) })
                }.onFailure { AppLog.warn("Track cache write failed") }
                result
            }
            is DataResult.Failure -> {
                val offline = result.error.kind == ErrorKind.NETWORK || result.error.kind == ErrorKind.TIMEOUT
                val cachedTracks = if (offline) cacheDao.tracksForAlbum(id) else emptyList()
                if (cachedTracks.isNotEmpty()) {
                    val first = cachedTracks.first()
                    DataResult.Success(
                        AlbumDetail(
                            album = Album(
                                id = id,
                                name = first.albumName ?: "Album",
                                artistName = first.artistName,
                                coverArtId = first.coverArtId,
                            ),
                            tracks = cachedTracks.map { it.toCachedTrack() },
                        ),
                    )
                } else {
                    result
                }
            }
        }
    }

    override suspend fun getSong(id: String): DataResult<Track> = service.withSession(
        block = { it.api.getSong(id) },
        map = { payload ->
            payload.song?.toTrack() ?: throw IllegalStateException("Song $id was not returned")
        },
    )

    override suspend fun getGenres(): DataResult<List<Genre>> = service.withSession(
        block = { it.api.getGenres() },
        map = { payload -> payload.genres?.genre.orEmpty().map { it.toGenre() }.filter { it.name.isNotBlank() } },
    )

    override suspend fun getStarred(): DataResult<StarredContent> = service.withSession(
        block = { it.api.getStarred2() },
        map = { payload ->
            StarredContent(
                albums = payload.starred2?.album.orEmpty().map { it.toAlbum() },
                tracks = payload.starred2?.song.orEmpty().map { it.toTrack() },
                artists = payload.starred2?.artist.orEmpty().map { it.toArtist() },
            )
        },
    )

    override suspend fun search(query: String): DataResult<SearchResults> = service.withSession(
        block = { it.api.search3(query = query) },
        map = { payload ->
            SearchResults(
                artists = payload.searchResult3?.artist.orEmpty().map { it.toArtist() },
                albums = payload.searchResult3?.album.orEmpty().map { it.toAlbum() },
                tracks = payload.searchResult3?.song.orEmpty().map { it.toTrack() },
            )
        },
    )

    override suspend fun getRandomTracks(size: Int, genre: String?): DataResult<List<Track>> = service.withSession(
        block = { it.api.getRandomSongs(size = size, genre = genre) },
        map = { payload -> payload.randomSongs?.song.orEmpty().map { it.toTrack() } },
    )

    override suspend fun getLyrics(trackId: String): DataResult<Lyrics?> = service.withSession(
        block = { it.api.getLyricsBySongId(trackId) },
        map = { payload -> payload.lyricsList.toLyrics() },
    )

    override suspend fun setTrackFavorite(trackId: String, favorite: Boolean): DataResult<Unit> = service.withSession(
        block = { api -> if (favorite) api.api.star(id = trackId) else api.api.unstar(id = trackId) },
        map = { },
    )

    override suspend fun setAlbumFavorite(albumId: String, favorite: Boolean): DataResult<Unit> = service.withSession(
        block = { api -> if (favorite) api.api.star(albumId = albumId) else api.api.unstar(albumId = albumId) },
        map = { },
    )

    override suspend fun scrobble(track: Track, submission: Boolean): DataResult<Unit> = service.withSession(
        block = { api -> api.api.scrobble(id = track.id, submission = submission) },
        map = { },
    )

    override fun coverArtUrl(coverArtId: String?, sizePx: Int): String? =
        service.coverArtUrl(coverArtId, sizePx)

    override fun streamUrl(trackId: String, quality: StreamQuality): String? =
        service.session()?.urls?.stream(trackId, quality)
}
