package com.navidrometv.data.prefs

import com.navidrometv.core.model.StreamQuality

/** Grid density on album/artist grids. Columns are chosen for 10-foot viewing. */
enum class GridDensity(val label: String, val columns: Int) {
    COMPACT("Compact", 7),
    COMFORTABLE("Comfortable", 5),
    SPACIOUS("Spacious", 4),
}

/**
 * User-controlled settings. Everything here changes real behaviour; settings that
 * the Subsonic API or Media3 cannot honour are deliberately absent (see
 * docs/COMPATIBILITY.md): crossfade, audio normalisation and server-side
 * transcoding profiles are not exposed as toggles that would do nothing.
 */
data class AppSettings(
    val streamingQuality: StreamQuality = StreamQuality.ORIGINAL,
    val reduceMotion: Boolean = false,
    val artworkBackground: Boolean = true,
    val gridDensity: GridDensity = GridDensity.COMFORTABLE,
    val shuffleByDefault: Boolean = false,
    val scrobbleEnabled: Boolean = true,
    val ambientModeEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val recentSearches: List<String> = emptyList(),
) {
    val focusScale: Float get() = if (reduceMotion) 1.02f else 1.06f
}
