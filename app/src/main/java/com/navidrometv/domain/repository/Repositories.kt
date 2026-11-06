package com.navidrometv.domain.repository

import com.navidrometv.core.common.DataResult
import com.navidrometv.core.model.Album
import com.navidrometv.core.model.AlbumDetail
import com.navidrometv.core.model.Artist
import com.navidrometv.core.model.ArtistDetail
import com.navidrometv.core.model.ArtistInfo
import com.navidrometv.core.model.Genre
import com.navidrometv.core.model.Lyrics
import com.navidrometv.core.model.Playlist
import com.navidrometv.core.model.SearchResults
import com.navidrometv.core.model.ServerHandshake
import com.navidrometv.core.model.StarredContent
import com.navidrometv.core.model.StreamQuality
import com.navidrometv.core.model.Track
import com.navidrometv.data.prefs.AppSettings
import kotlinx.coroutines.flow.Flow

/** Connection snapshot the UI can render without touching credentials. */
data class ConnectionStatus(
    val connected: Boolean,
    val serverUrl: String? = null,
    val username: String? = null,
    val serverVersion: String? = null,
)

interface AuthRepository {
    val connection: Flow<ConnectionStatus>

    /** Tries credentials without persisting anything. */
    suspend fun testConnection(serverUrl: String, username: String, password: String): DataResult<ServerHandshake>

    /** Authenticates, persists credentials securely and activates the session. */
    suspend fun connect(serverUrl: String, username: String, password: String): DataResult<ServerHandshake>

    /** Restores a stored session on launch. Returns false when nothing is stored. */
    suspend fun restoreSession(): Boolean

    /** Pings the server using the already stored session (used by Settings). */
    suspend fun verifyConnection(): DataResult<ServerHandshake>

    suspend fun disconnect()

    fun currentStatus(): ConnectionStatus
}

/** The Subsonic album-list types the app can request. */
enum class AlbumListType(val apiValue: String) {
    NEWEST("newest"),
    RECENTLY_PLAYED("recent"),
    FREQUENT("frequent"),
    RANDOM("random"),
    ALPHABETICAL_BY_NAME("alphabeticalByName"),
    ALPHABETICAL_BY_ARTIST("alphabeticalByArtist"),
    STARRED("starred"),
    BY_YEAR("byYear"),
    BY_GENRE("byGenre"),
}

interface MusicRepository {
    suspend fun getAlbumList(
        type: AlbumListType,
        size: Int = 40,
        offset: Int = 0,
        genre: String? = null,
        fromYear: Int? = null,
        toYear: Int? = null,
    ): DataResult<List<Album>>

    suspend fun getArtists(): DataResult<List<Artist>>

    suspend fun getArtist(id: String): DataResult<ArtistDetail>

    suspend fun getArtistInfo(id: String): DataResult<ArtistInfo>

    suspend fun getAlbum(id: String): DataResult<AlbumDetail>

    suspend fun getSong(id: String): DataResult<Track>

    suspend fun getGenres(): DataResult<List<Genre>>

    suspend fun getStarred(): DataResult<StarredContent>

    suspend fun search(query: String): DataResult<SearchResults>

    suspend fun getRandomTracks(size: Int = 30, genre: String? = null): DataResult<List<Track>>

    /** OpenSubsonic lyrics. `Success(null)` means the server has no lyrics. */
    suspend fun getLyrics(trackId: String): DataResult<Lyrics?>

    suspend fun setTrackFavorite(trackId: String, favorite: Boolean): DataResult<Unit>

    suspend fun setAlbumFavorite(albumId: String, favorite: Boolean): DataResult<Unit>

    suspend fun scrobble(track: Track, submission: Boolean): DataResult<Unit>

    fun coverArtUrl(coverArtId: String?, sizePx: Int): String?

    fun streamUrl(trackId: String, quality: StreamQuality): String?
}

interface PlaylistRepository {
    suspend fun getPlaylists(): DataResult<List<Playlist>>

    suspend fun getPlaylist(id: String): DataResult<PlaylistDetail>

    suspend fun createPlaylist(name: String, trackIds: List<String> = emptyList()): DataResult<Unit>

    suspend fun renamePlaylist(id: String, name: String): DataResult<Unit>

    suspend fun deletePlaylist(id: String): DataResult<Unit>

    suspend fun addTracks(playlistId: String, trackIds: List<String>): DataResult<Unit>

    /**
     * Subsonic can only delete by index, so this removes one entry. There is no
     * move/reorder endpoint in the API; the UI explains that instead of faking it.
     */
    suspend fun removeTrackAt(playlistId: String, index: Int): DataResult<Unit>
}

data class PlaylistDetail(
    val playlist: Playlist,
    val tracks: List<Track>,
)

interface HistoryRepository {
    suspend fun recordPlay(track: Track)

    /** Most recent entry per track, newest first. */
    suspend fun recentlyPlayedTracks(limit: Int = 30): List<Track>

    suspend fun recentlyPlayedAlbums(limit: Int = 20): List<Album>

    suspend fun recentlyPlayedArtists(limit: Int = 20): List<Artist>

    suspend fun mostPlayedTracks(limit: Int = 20): List<Track>

    suspend fun hasHistory(): Boolean

    suspend fun clear()
}

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setStreamingQuality(quality: StreamQuality)

    suspend fun setReduceMotion(enabled: Boolean)

    suspend fun setArtworkBackground(enabled: Boolean)

    suspend fun setGridDensity(density: com.navidrometv.data.prefs.GridDensity)

    suspend fun setShuffleByDefault(enabled: Boolean)

    suspend fun setScrobbleEnabled(enabled: Boolean)

    suspend fun setAmbientModeEnabled(enabled: Boolean)

    suspend fun setKeepScreenOn(enabled: Boolean)

    suspend fun addRecentSearch(query: String)

    suspend fun clearRecentSearches()
}
