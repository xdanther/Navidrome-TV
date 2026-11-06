package com.navidrometv.data.remote

import com.navidrometv.core.model.Album
import com.navidrometv.core.model.Artist
import com.navidrometv.core.model.Genre
import com.navidrometv.core.model.Lyrics
import com.navidrometv.core.model.LyricsLine
import com.navidrometv.core.model.Playlist
import com.navidrometv.core.model.Track
import com.navidrometv.data.remote.dto.AlbumId3Dto
import com.navidrometv.data.remote.dto.ArtistId3Dto
import com.navidrometv.data.remote.dto.ChildDto
import com.navidrometv.data.remote.dto.GenreDto
import com.navidrometv.data.remote.dto.LyricsListDto
import com.navidrometv.data.remote.dto.PlaylistDto

/**
 * DTO -> domain mapping. Kept free of Android and network types so it is covered
 * directly by unit tests.
 *
 * Cover art fallbacks use Navidrome's id conventions (`al-<albumId>`,
 * `ar-<artistId>`) because many servers omit `coverArt` on list payloads. The
 * prefix is only added when the id does not carry it already, so servers that
 * return prefixed ids do not end up with unusable `al-al-…` artwork ids.
 */
object SubsonicMapper {

    private fun albumArtId(albumId: String): String =
        if (albumId.startsWith("al-")) albumId else "al-$albumId"

    private fun artistArtId(artistId: String): String =
        if (artistId.startsWith("ar-")) artistId else "ar-$artistId"

    fun ChildDto.toTrack(): Track {
        val parentAlbumId = albumId ?: parent
        return Track(
            id = id,
            title = title ?: "Untitled",
            albumId = parentAlbumId,
            albumName = album,
            artistId = artistId,
            artistName = artist,
            durationMs = (duration ?: 0) * 1000L,
            trackNumber = track,
            discNumber = discNumber,
            year = year,
            genre = genre,
            isFavorite = !starred.isNullOrBlank(),
            coverArtId = coverArt ?: parentAlbumId?.let(::albumArtId),
            suffix = suffix,
            bitRateKbps = bitRate,
            contentType = contentType,
            sizeBytes = size,
        )
    }

    fun AlbumId3Dto.toAlbum(): Album = Album(
        id = id,
        name = name.ifBlank { "Untitled album" },
        artistId = artistId,
        artistName = artist,
        year = year,
        genre = genre,
        trackCount = songCount,
        durationSeconds = duration,
        coverArtId = coverArt ?: albumArtId(id),
        isFavorite = !starred.isNullOrBlank(),
        created = created,
    )

    fun ArtistId3Dto.toArtist(): Artist = Artist(
        id = id,
        name = name.ifBlank { "Unknown artist" },
        albumCount = albumCount,
        coverArtId = coverArt ?: artistArtId(id),
        imageUrl = artistImageUrl,
        biography = biography,
    )

    fun PlaylistDto.toPlaylist(): Playlist = Playlist(
        id = id,
        name = name.ifBlank { "Untitled playlist" },
        owner = owner,
        comment = comment,
        trackCount = songCount ?: entry.size,
        coverArtId = coverArt ?: if (entry.isNotEmpty()) entry.first().coverArt else null,
        isPublic = isPublic == true,
        created = created,
        changed = changed,
    )

    fun GenreDto.toGenre(): Genre = Genre(
        name = value,
        albumCount = albumCount,
        songCount = songCount,
    )

    /**
     * OpenSubsonic `getLyricsBySongId`. Synchronized lyrics are preferred; a plain
     * unsynchronized block is returned as static lines. Returns null when the
     * server has no lyrics for the song, which the UI shows as "no lyrics" rather
     * than fabricating text.
     */
    fun LyricsListDto?.toLyrics(): Lyrics? {
        val entries = this?.structuredLyrics ?: return null
        if (entries.isEmpty()) return null

        val synced = entries.firstOrNull { it.synced && it.line.any { line -> line.start != null } }
        if (synced != null) {
            return Lyrics(
                lines = synced.line.mapNotNull { line ->
                    line.start?.let { start -> LyricsLine(startMs = start, text = line.value) }
                },
                synced = true,
            )
        }

        val plain = entries.firstOrNull { it.line.isNotEmpty() } ?: return null
        return Lyrics(
            lines = plain.line.map { LyricsLine(startMs = null, text = it.value) },
            synced = false,
        )
    }

    /** Splits an artist index into a flat, name-sorted artist list. */
    fun List<com.navidrometv.data.remote.dto.ArtistIndexDto>.flattenArtists(): List<Artist> =
        flatMap { index -> index.artist.map { it.toArtist() } }
            .sortedBy { it.name.lowercase() }
}
