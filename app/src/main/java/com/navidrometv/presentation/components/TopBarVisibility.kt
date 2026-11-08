package com.navidrometv.presentation.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf

/** Whether the floating top navigation is currently expanded. */
@Stable
class TopBarVisibility {
    var visible by mutableStateOf(true)
}

val LocalTopBarVisibility = staticCompositionLocalOf { TopBarVisibility() }

/**
 * Hides the top bar when the user scrolls down into the content and reveals it
 * again when they come back to the first row.
 *
 * The thresholds are deliberately hysteretic: the bar leaves once the list is past
 * roughly one row-height, but only returns at the very top, so a small jiggle of the
 * remote does not make it flicker in and out.
 */
@Composable
fun HideTopBarOnScroll(state: LazyListState) {
    val visibility = LocalTopBarVisibility.current
    LaunchedEffect(state, visibility) {
        var previousIndex = 0
        var previousOffset = 0
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val atTop = index == 0 && offset <= 12
                val movingDown = index > previousIndex ||
                    (index == previousIndex && offset > previousOffset)
                when {
                    atTop -> visibility.visible = true
                    movingDown && (index > 0 || offset > 96) -> visibility.visible = false
                    !movingDown && index == 0 -> visibility.visible = true
                }
                previousIndex = index
                previousOffset = offset
            }
    }
}
