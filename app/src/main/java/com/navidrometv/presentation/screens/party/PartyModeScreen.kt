package com.navidrometv.presentation.screens.party

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.navidrometv.core.common.Formatters
import com.navidrometv.core.designsystem.component.ActionButton
import com.navidrometv.core.designsystem.component.EmptyState
import com.navidrometv.core.designsystem.component.FocusableRow
import com.navidrometv.core.designsystem.component.InlineNotice
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.LocalAppColors
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.party.PartyRequest
import kotlinx.coroutines.delay

/**
 * Host interface for Party Mode: pairing QR/code, connected guests and approval of
 * pending requests. Guests never join the queue directly.
 */
@Composable
fun PartyModeScreen(
    onOpenNowPlaying: () -> Unit,
    viewModel: PartyModeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            delay(3_000)
            viewModel.consumeNotice()
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (!state.running) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Space.screenHorizontal, vertical = Space.lg),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "PARTY MODE", style = AppType.overline, color = NavidromeColors.Accent)
                Text(
                    text = "Let guests pick the music",
                    style = AppType.display,
                    color = NavidromeColors.OnSurface,
                )
                Text(
                    text = "Start a guest queue and a pairing code appears on this screen. Guests scan the " +
                        "code, search your library and send requests; you approve what joins the queue.",
                    style = AppType.body,
                    color = NavidromeColors.OnSurfaceMuted,
                    modifier = Modifier.fillMaxWidth(0.7f).padding(top = Space.sm),
                )
                Text(
                    text = "Guests never receive your server address or password. Sessions expire after two " +
                        "hours, and requests are rate limited.",
                    style = AppType.cardSubtitle,
                    color = NavidromeColors.OnSurfaceMuted.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(0.7f).padding(top = Space.xs),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    modifier = Modifier.padding(top = Space.lg),
                ) {
                    ActionButton(text = "Start party mode", onClick = viewModel::start)
                    ActionButton(
                        text = "Now playing",
                        onClick = onOpenNowPlaying,
                        primary = false,
                    )
                }
                state.error?.let { error ->
                    Text(
                        text = error,
                        style = AppType.cardSubtitle,
                        color = Color(0xFFB3261E),
                        modifier = Modifier.padding(top = Space.md),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Space.screenHorizontal, vertical = Space.md),
                horizontalArrangement = Arrangement.spacedBy(Space.xl),
            ) {
                Column(
                    modifier = Modifier.width(420.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(Space.md),
                ) {
                    Text(text = "Party mode", style = AppType.title, color = NavidromeColors.OnSurface)

                    Box(
                        Modifier
                            .background(Color.White, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                    ) {
                        state.url?.let { url ->
                            Image(
                                bitmap = remember(url) { qrBitmap(url) },
                                contentDescription = "Pairing QR code",
                                modifier = Modifier.size(300.dp),
                            )
                        } ?: Text(
                            text = "No network address available for guests",
                            style = AppType.cardSubtitle,
                            color = NavidromeColors.Ink,
                        )
                    }

                    Text(
                        text = "Code ${state.code}",
                        style = AppType.display,
                        color = NavidromeColors.OnSurface,
                    )
                    Text(
                        text = state.url ?: "Connect this TV to Wi-Fi to share an address",
                        style = AppType.bodyMuted,
                        color = LocalAppColors.current.secondaryText,
                    )
                    Text(
                        text = "Guest page is served from this TV on port ${state.port}.",
                        style = AppType.cardSubtitle,
                        color = NavidromeColors.OnSurfaceMuted,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        ActionButton(text = "Stop", onClick = viewModel::stop, primary = false)
                        ActionButton(text = "Now playing", onClick = onOpenNowPlaying, primary = false)
                    }
                }

                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Text(
                        text = "Pending requests",
                        style = AppType.section,
                        color = NavidromeColors.OnSurface,
                    )
                    if (state.pending.isEmpty()) {
                        EmptyState(
                            title = "No pending requests",
                            message = "Guests' songs appear here for approval.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.62f),
                            contentPadding = PaddingValues(vertical = Space.sm),
                        ) {
                            items(state.pending, key = { it.id }) { request ->
                                RequestRow(
                                    request = request,
                                    onApprove = { viewModel.approve(request.id) },
                                    onReject = { viewModel.reject(request.id) },
                                )
                            }
                        }
                    }

                    Text(
                        text = "Guests (${state.guests.size})",
                        style = AppType.section,
                        color = NavidromeColors.OnSurface,
                        modifier = Modifier.padding(top = Space.md),
                    )
                    if (state.guests.isEmpty()) {
                        Text(
                            text = "Nobody has joined yet.",
                            style = AppType.cardSubtitle,
                            color = NavidromeColors.OnSurfaceMuted,
                        )
                    } else {
                        state.guests.forEach { guest ->
                            Text(
                                text = guest.name,
                                style = AppType.bodyMuted,
                                color = NavidromeColors.OnSurface,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }

        state.notice?.let { notice ->
            InlineNotice(
                text = notice,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp),
            )
        }
    }
}

@Composable
private fun RequestRow(
    request: PartyRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    FocusableRow(
        onClick = onApprove,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = request.track.title,
                    style = AppType.body,
                    color = NavidromeColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${request.track.artistName.orEmpty()} · from ${request.guestName}",
                    style = AppType.cardSubtitle,
                    color = LocalAppColors.current.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = Formatters.relativeTime(request.requestedAt),
                style = AppType.metadata,
                color = NavidromeColors.OnSurfaceMuted,
                textAlign = TextAlign.End,
            )
            ActionButton(
                text = "Approve",
                onClick = onApprove,
                modifier = Modifier.padding(start = Space.sm),
            )
            ActionButton(
                text = "Reject",
                onClick = onReject,
                primary = false,
                modifier = Modifier.padding(start = Space.xs),
            )
        }
    }
}

/** Renders the pairing URL as a QR bitmap generated locally with ZXing. */
private fun qrBitmap(content: String, sizePx: Int = 320): androidx.compose.ui.graphics.ImageBitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val pixels = IntArray(sizePx * sizePx) { index ->
        val x = index % sizePx
        val y = index / sizePx
        if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    return bitmap.asImageBitmap()
}
