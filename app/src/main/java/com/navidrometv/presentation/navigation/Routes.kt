package com.navidrometv.presentation.navigation

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val NEW = "new"
    const val BROWSE = "browse"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val NOW_PLAYING = "now_playing"
    const val SETTINGS = "settings"
    const val PARTY = "party"

    const val ALBUM_ARG = "albumId"
    const val ARTIST_ARG = "artistId"
    const val PLAYLIST_ARG = "playlistId"
    const val GENRE_ARG = "genreName"

    const val ALBUM = "album/{$ALBUM_ARG}"
    const val ARTIST = "artist/{$ARTIST_ARG}"
    const val PLAYLIST = "playlist/{$PLAYLIST_ARG}"
    const val GENRE = "genre/{$GENRE_ARG}"

    fun album(id: String): String = "album/${Uri.encode(id)}"
    fun artist(id: String): String = "artist/${Uri.encode(id)}"
    fun playlist(id: String): String = "playlist/${Uri.encode(id)}"
    fun genre(name: String): String = "genre/${Uri.encode(name)}"

    /** Destinations that live in the top bar, in display order. */
    val topLevel: List<Pair<String, String>> = listOf(
        HOME to "Home",
        NEW to "New",
        BROWSE to "Browse",
        LIBRARY to "Library",
        SEARCH to "Search",
    )
}
