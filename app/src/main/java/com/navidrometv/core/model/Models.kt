package com.navidrometv.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int? = null,
    val coverArtId: String? = null,
    val imageUrl: String? = null,
    val biography: String? = null,
)

@Serializable
data class Album(
    val id: String,
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
)

@Serializable
data class Track(
    val id: String,
    val title: String,
    val albumId: String? = null,
    val albumName: String? = null,
    val artistId: String? = null,
    val artistName: String? = null,
    val durationMs: Long = 0L,
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
) {
    /** Subsonic servers do not always send duration; guard against division by zero. */
    val hasDuration: Boolean get() = durationMs > 0
}

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val owner: String? = null,
    val comment: String? = null,
    val trackCount: Int = 0,
    val coverArtId: String? = null,
    val isPublic: Boolean = false,
    val created: String? = null,
    val changed: String? = null,
)

@Serializable
data class Genre(
    val name: String,
    val albumCount: Int = 0,
    val songCount: Int = 0,
)

@Serializable
data class LyricsLine(
    val startMs: Long? = null,
    val text: String,
)

@Serializable
data class Lyrics(
    val lines: List<LyricsLine>,
    val synced: Boolean,
) {
    val isEmpty: Boolean get() = lines.isEmpty()
}

data class AlbumDetail(
    val album: Album,
    val tracks: List<Track>,
)

data class ArtistDetail(
    val artist: Artist,
    val albums: List<Album>,
    val appearsOn: List<Album> = emptyList(),
    val topTracks: List<Track> = emptyList(),
    val similarArtists: List<Artist> = emptyList(),
)

data class ArtistInfo(
    val biography: String? = null,
    val similarArtists: List<Artist> = emptyList(),
)

data class StarredContent(
    val albums: List<Album> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
)

data class SearchResults(
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
) {
    val isEmpty: Boolean
        get() = artists.isEmpty() && albums.isEmpty() && tracks.isEmpty() && playlists.isEmpty()
}

data class MusicFolder(val id: String, val name: String)

data class ServerLicense(
    val valid: Boolean,
    val email: String? = null,
    val expires: String? = null,
)

data class ServerHandshake(
    val serverVersion: String,
    val license: ServerLicense?,
    val folders: List<MusicFolder>,
)

/** Ordered play list handed to the playback layer. */
data class PlaybackRequest(
    val tracks: List<Track>,
    val startIndex: Int = 0,
    val shuffle: Boolean = false,
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE,
}

enum class StreamQuality(val label: String, val maxBitRateKbps: Int?, val format: String?) {
    ORIGINAL("Original quality", null, null),
    HIGH("High (320 kbps)", 320, null),
    BALANCED("Balanced (192 kbps)", 192, "mp3"),
    DATA_SAVER("Data saver (96 kbps)", 96, "mp3"),
    ;

    val isTranscoded: Boolean get() = maxBitRateKbps != null
}
