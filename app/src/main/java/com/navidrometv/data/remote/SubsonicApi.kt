package com.navidrometv.data.remote

import com.navidrometv.data.remote.dto.SubsonicEnvelope
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Subsonic REST endpoints. Authentication parameters (u/t/s/v/c/f) are appended
 * per request by [SubsonicAuthInterceptor] so no token ever appears in code or
 * logs.
 *
 * Only endpoints that Navidrome and other Subsonic-compatible servers actually
 * implement are declared. Unsupported endpoints surface as a normal API error
 * (code 0 or 404) which repositories translate into a documented "unsupported"
 * result instead of failing the whole screen.
 */
interface SubsonicApi {

    @GET("rest/ping.view")
    suspend fun ping(): SubsonicEnvelope

    @GET("rest/getLicense.view")
    suspend fun getLicense(): SubsonicEnvelope

    @GET("rest/getMusicFolders.view")
    suspend fun getMusicFolders(): SubsonicEnvelope

    @GET("rest/getArtists.view")
    suspend fun getArtists(): SubsonicEnvelope

    @GET("rest/getArtist.view")
    suspend fun getArtist(@Query("id") id: String): SubsonicEnvelope

    @GET("rest/getArtistInfo2.view")
    suspend fun getArtistInfo2(@Query("id") id: String): SubsonicEnvelope

    @GET("rest/getAlbumList2.view")
    suspend fun getAlbumList2(
        @Query("type") type: String,
        @Query("size") size: Int,
        @Query("offset") offset: Int = 0,
        @Query("fromYear") fromYear: Int? = null,
        @Query("toYear") toYear: Int? = null,
        @Query("genre") genre: String? = null,
    ): SubsonicEnvelope

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(@Query("id") id: String): SubsonicEnvelope

    @GET("rest/getSong.view")
    suspend fun getSong(@Query("id") id: String): SubsonicEnvelope

    /** Subsonic 1.13+; Navidrome computes this from its own play statistics. */
    @GET("rest/getTopSongs.view")
    suspend fun getTopSongs(
        @Query("artist") artist: String,
        @Query("count") count: Int = 20,
    ): SubsonicEnvelope

    @GET("rest/getPlaylists.view")
    suspend fun getPlaylists(): SubsonicEnvelope

    @GET("rest/getPlaylist.view")
    suspend fun getPlaylist(@Query("id") id: String): SubsonicEnvelope

    @GET("rest/search3.view")
    suspend fun search3(
        @Query("query") query: String,
        @Query("artistCount") artistCount: Int = 20,
        @Query("artistOffset") artistOffset: Int = 0,
        @Query("albumCount") albumCount: Int = 20,
        @Query("albumOffset") albumOffset: Int = 0,
        @Query("songCount") songCount: Int = 30,
        @Query("songOffset") songOffset: Int = 0,
    ): SubsonicEnvelope

    @GET("rest/getStarred2.view")
    suspend fun getStarred2(): SubsonicEnvelope

    @GET("rest/getGenres.view")
    suspend fun getGenres(): SubsonicEnvelope

    @GET("rest/getRandomSongs.view")
    suspend fun getRandomSongs(
        @Query("size") size: Int = 30,
        @Query("genre") genre: String? = null,
    ): SubsonicEnvelope

    @GET("rest/star.view")
    suspend fun star(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null,
    ): SubsonicEnvelope

    @GET("rest/unstar.view")
    suspend fun unstar(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null,
    ): SubsonicEnvelope

    @GET("rest/scrobble.view")
    suspend fun scrobble(
        @Query("id") id: String,
        @Query("time") time: Long? = null,
        @Query("submission") submission: Boolean = true,
    ): SubsonicEnvelope

    @GET("rest/getLyricsBySongId.view")
    suspend fun getLyricsBySongId(@Query("id") id: String): SubsonicEnvelope

    @GET("rest/createPlaylist.view")
    suspend fun createPlaylist(
        @Query("name") name: String,
        @Query("playlistId") playlistId: String? = null,
        @Query("songId") songIds: List<String>? = null,
    ): SubsonicEnvelope

    @GET("rest/updatePlaylist.view")
    suspend fun updatePlaylist(
        @Query("playlistId") playlistId: String,
        @Query("name") name: String? = null,
        @Query("comment") comment: String? = null,
        @Query("public") isPublic: Boolean? = null,
        @Query("songIdToAdd") songIdsToAdd: List<String>? = null,
        @Query("songIndexToRemove") songIndexesToRemove: List<Int>? = null,
    ): SubsonicEnvelope

    @GET("rest/deletePlaylist.view")
    suspend fun deletePlaylist(@Query("id") id: String): SubsonicEnvelope
}
