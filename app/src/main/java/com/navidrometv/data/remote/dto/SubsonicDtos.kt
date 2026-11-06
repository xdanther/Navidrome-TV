package com.navidrometv.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Every Subsonic endpoint shares one envelope: `{"subsonic-response": {...}}`.
 * A single payload type keeps the Retrofit surface small; only the fields an
 * endpoint actually returns are populated, everything else is null.
 *
 * All DTO fields are nullable or defaulted because servers differ in which
 * optional attributes they emit (Navidrome, Airsonic, Gonic, ...).
 */
@Serializable
data class SubsonicEnvelope(
    @SerialName("subsonic-response") val payload: SubsonicPayload = SubsonicPayload(),
)

@Serializable
data class SubsonicPayload(
    val status: String = "failed",
    val version: String = "",
    val type: String? = null,
    val serverVersion: String? = null,
    val openSubsonic: Boolean? = null,
    val error: SubsonicErrorDto? = null,
    val license: LicenseDto? = null,
    val musicFolders: MusicFoldersDto? = null,
    val artists: ArtistsDto? = null,
    val artist: ArtistId3Dto? = null,
    val artistInfo2: ArtistInfo2Dto? = null,
    val album: AlbumId3Dto? = null,
    val albumList2: AlbumListDto? = null,
    val song: ChildDto? = null,
    val topSongs: SongsDto? = null,
    val playlists: PlaylistsDto? = null,
    val playlist: PlaylistDto? = null,
    val searchResult3: SearchResultDto? = null,
    val starred2: StarredDto? = null,
    val genres: GenresDto? = null,
    val randomSongs: SongsDto? = null,
    val lyricsList: LyricsListDto? = null,
) {
    val isOk: Boolean get() = status.equals("ok", ignoreCase = true)
}

@Serializable
data class SubsonicErrorDto(
    val code: Int = 0,
    val message: String = "",
    val helpUrl: String? = null,
)

@Serializable
data class LicenseDto(
    val valid: Boolean = false,
    val email: String? = null,
    val licenseExpires: String? = null,
)

@Serializable
data class MusicFoldersDto(
    val musicFolder: List<MusicFolderDto> = emptyList(),
)

@Serializable
data class MusicFolderDto(
    val id: String = "",
    val name: String = "",
)

@Serializable
data class ArtistsDto(
    val index: List<ArtistIndexDto> = emptyList(),
)

@Serializable
data class ArtistIndexDto(
    val name: String? = null,
    val artist: List<ArtistId3Dto> = emptyList(),
)

@Serializable
data class ArtistId3Dto(
    val id: String = "",
    val name: String = "",
    val coverArt: String? = null,
    val albumCount: Int? = null,
    val artistImageUrl: String? = null,
    val starred: String? = null,
    val biography: String? = null,
    val album: List<AlbumId3Dto> = emptyList(),
)

@Serializable
data class ArtistInfo2Dto(
    val biography: String? = null,
    val musicBrainzId: String? = null,
    val smallImageUrl: String? = null,
    val mediumImageUrl: String? = null,
    val largeImageUrl: String? = null,
    val similarArtist: List<ArtistId3Dto> = emptyList(),
)

@Serializable
data class AlbumId3Dto(
    val id: String = "",
    val name: String = "",
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int? = null,
    val duration: Int? = null,
    val playCount: Long? = null,
    val created: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val starred: String? = null,
    val song: List<ChildDto> = emptyList(),
)

@Serializable
data class AlbumListDto(
    val album: List<AlbumId3Dto> = emptyList(),
)

@Serializable
data class ChildDto(
    val id: String = "",
    val parent: String? = null,
    val isDir: Boolean = false,
    val title: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val track: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,
    val size: Long? = null,
    val duration: Int? = null,
    val bitRate: Int? = null,
    val contentType: String? = null,
    val suffix: String? = null,
    val starred: String? = null,
    val albumId: String? = null,
    val artistId: String? = null,
    val type: String? = null,
)

@Serializable
data class SongsDto(
    val song: List<ChildDto> = emptyList(),
)

@Serializable
data class PlaylistsDto(
    val playlist: List<PlaylistDto> = emptyList(),
)

@Serializable
data class PlaylistDto(
    val id: String = "",
    val name: String = "",
    val owner: String? = null,
    val comment: String? = null,
    @SerialName("public") val isPublic: Boolean? = null,
    val songCount: Int? = null,
    val duration: Int? = null,
    val created: String? = null,
    val changed: String? = null,
    val coverArt: String? = null,
    val entry: List<ChildDto> = emptyList(),
)

@Serializable
data class SearchResultDto(
    val artist: List<ArtistId3Dto> = emptyList(),
    val album: List<AlbumId3Dto> = emptyList(),
    val song: List<ChildDto> = emptyList(),
)

@Serializable
data class StarredDto(
    val artist: List<ArtistId3Dto> = emptyList(),
    val album: List<AlbumId3Dto> = emptyList(),
    val song: List<ChildDto> = emptyList(),
)

@Serializable
data class GenresDto(
    val genre: List<GenreDto> = emptyList(),
)

@Serializable
data class GenreDto(
    val value: String = "",
    val songCount: Int = 0,
    val albumCount: Int = 0,
)

@Serializable
data class LyricsListDto(
    val structuredLyrics: List<StructuredLyricsDto> = emptyList(),
)

@Serializable
data class StructuredLyricsDto(
    val displayArtist: String? = null,
    val displayTitle: String? = null,
    val lang: String? = null,
    val synced: Boolean = false,
    val line: List<LyricLineDto> = emptyList(),
)

@Serializable
data class LyricLineDto(
    val value: String = "",
    /** Only present for synchronized lyrics. */
    val start: Long? = null,
)
