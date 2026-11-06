package com.navidrometv.data.remote

import com.navidrometv.data.remote.SubsonicMapper.flattenArtists
import com.navidrometv.data.remote.SubsonicMapper.toAlbum
import com.navidrometv.data.remote.SubsonicMapper.toArtist
import com.navidrometv.data.remote.SubsonicMapper.toLyrics
import com.navidrometv.data.remote.SubsonicMapper.toTrack
import com.navidrometv.data.remote.dto.AlbumId3Dto
import com.navidrometv.data.remote.dto.ArtistId3Dto
import com.navidrometv.data.remote.dto.ArtistIndexDto
import com.navidrometv.data.remote.dto.ChildDto
import com.navidrometv.data.remote.dto.LyricLineDto
import com.navidrometv.data.remote.dto.LyricsListDto
import com.navidrometv.data.remote.dto.StructuredLyricsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubsonicMapperTest {

    @Test
    fun `track converts seconds to millis and keeps metadata`() {
        val track = ChildDto(
            id = "42",
            title = "Nightcall",
            album = "OutRun",
            artist = "Kavinsky",
            artistId = "ar-1",
            albumId = "al-9",
            duration = 245,
            track = 3,
            bitRate = 320,
            suffix = "flac",
            starred = "2024-02-02T10:00:00Z",
        ).toTrack()

        assertEquals("42", track.id)
        assertEquals(245_000L, track.durationMs)
        assertEquals(3, track.trackNumber)
        assertEquals(320, track.bitRateKbps)
        assertEquals("flac", track.suffix)
        assertTrue(track.isFavorite)
        assertTrue(track.hasDuration)
        assertEquals("al-9", track.coverArtId)
    }

    @Test
    fun `cover art fallback adds the prefix once for bare ids`() {
        val album = AlbumId3Dto(id = "3f6d1c", name = "Drukqs").toAlbum()
        assertEquals("al-3f6d1c", album.coverArtId)

        val artist = ArtistId3Dto(id = "88aa10", name = "Aphex Twin").toArtist()
        assertEquals("ar-88aa10", artist.coverArtId)
    }

    @Test
    fun `track falls back to parent id for album and artwork`() {
        val track = ChildDto(id = "7", title = "Unknown", parent = "album-7").toTrack()

        assertEquals("album-7", track.albumId)
        assertEquals("al-album-7", track.coverArtId)
        assertFalse(track.isFavorite)
        assertEquals("Untitled", ChildDto(id = "8").toTrack().title)
    }

    @Test
    fun `album maps counts and falls back to id artwork`() {
        val album = AlbumId3Dto(
            id = "al-3",
            name = "",
            artist = "Aphex Twin",
            songCount = 13,
            duration = 3200,
            year = 1992,
        ).toAlbum()

        assertEquals("Untitled album", album.name)
        assertEquals(13, album.trackCount)
        assertEquals(3200, album.durationSeconds)
        assertEquals("al-3", album.coverArtId)
    }

    @Test
    fun `artist uses cover art or artist id fallback`() {
        val artist = ArtistId3Dto(id = "ar-5", name = "Boards of Canada", albumCount = 4).toArtist()

        assertEquals("ar-5", artist.id)
        assertEquals(4, artist.albumCount)
        assertEquals("ar-5", artist.coverArtId)
    }

    @Test
    fun `synchronized lyrics prefer timed lines`() {
        val lyrics = LyricsListDto(
            structuredLyrics = listOf(
                StructuredLyricsDto(
                    synced = true,
                    line = listOf(
                        LyricLineDto(value = "first", start = 1000),
                        LyricLineDto(value = "interlude"),
                        LyricLineDto(value = "second", start = 5000),
                    ),
                ),
            ),
        ).toLyrics()

        requireNotNull(lyrics)
        assertTrue(lyrics.synced)
        assertEquals(2, lyrics.lines.size)
        assertEquals(1000L, lyrics.lines.first().startMs)
    }

    @Test
    fun `unsynchronized lyrics are returned as plain text`() {
        val lyrics = LyricsListDto(
            structuredLyrics = listOf(
                StructuredLyricsDto(
                    synced = false,
                    line = listOf(LyricLineDto(value = "line one"), LyricLineDto(value = "line two")),
                ),
            ),
        ).toLyrics()

        requireNotNull(lyrics)
        assertFalse(lyrics.synced)
        assertEquals(2, lyrics.lines.size)
        assertNull(lyrics.lines.first().startMs)
    }

    @Test
    fun `missing lyrics return null rather than empty text`() {
        assertNull(null.toLyrics())
        assertNull(LyricsListDto().toLyrics())
    }

    @Test
    fun `artist index flattens and sorts case insensitively`() {
        val artists = listOf(
            ArtistIndexDto(artist = listOf(ArtistId3Dto(id = "1", name = "zed"))),
            ArtistIndexDto(artist = listOf(ArtistId3Dto(id = "2", name = "Alpha"), ArtistId3Dto(id = "3", name = "beta"))),
        ).flattenArtists()

        assertEquals(listOf("Alpha", "beta", "zed"), artists.map { it.name })
    }
}
