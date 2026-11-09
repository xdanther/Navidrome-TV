package com.navidrometv.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.NavidromeColors

/**
 * On-screen keyboard for the D-pad. A remote has no text field, so letters are the
 * primary input; the search field mirrors what has been typed.
 */
@Composable
fun TvKeyboard(
    onCharacter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = true,
) {
    val rows = remember {
        listOf(
            "1234567890".toList(),
            "qwertyuiop".toList(),
            "asdfghjkl".toList(),
            "zxcvbnm".toList(),
        )
    }
    val firstKeyFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (requestInitialFocus) {
            runCatching { firstKeyFocus.requestFocus() }
        }
    }

    Column(
        modifier = modifier
            .background(NavidromeColors.Glass, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { index, character ->
                    KeyButton(
                        label = character.toString(),
                        onClick = { onCharacter(character) },
                        modifier = if (rowIndex == 0 && index == 0) Modifier.focusRequester(firstKeyFocus) else Modifier,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton(label = "SPACE", wide = true, onClick = { onCharacter(' ') })
            KeyButton(label = "DELETE", wide = true, onClick = onBackspace)
            KeyButton(label = "CLEAR", wide = true, onClick = onClear)
            KeyButton(label = "SEARCH", wide = true, primary = true, onClick = onSearch)
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    primary: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .then(if (wide) Modifier.size(width = 118.dp, height = 46.dp) else Modifier.size(62.dp))
            .background(
                when {
                    focused -> Color.White
                    primary -> NavidromeColors.Accent
                    else -> NavidromeColors.SurfaceVariant.copy(alpha = 0.75f)
                },
                RoundedCornerShape(12.dp),
            )
            .then(
                if (focused) Modifier.border(2.dp, NavidromeColors.Ink, RoundedCornerShape(12.dp)) else Modifier,
            )
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = AppType.metadata,
            color = when {
                focused -> NavidromeColors.Ink
                primary -> Color.White
                else -> NavidromeColors.OnSurface
            },
        )
    }
}

/** Focusable placeholder used to anchor the D-pad to the search field. */
@Composable
fun SearchFieldAnchor(modifier: Modifier = Modifier) {
    Box(modifier.height(0.dp).focusable())
}
