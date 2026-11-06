package com.navidrometv.presentation.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.component.ActionButton
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space
import com.navidrometv.presentation.components.TvKeyboard

private enum class InputField { SERVER, USERNAME, PASSWORD }

/**
 * Server onboarding. All three values are typed with the D-pad keyboard because a
 * television remote has no usable text field; nothing here requires a touchscreen.
 */
@Composable
fun OnboardingScreen(
    onConnected: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<InputField?>(null) }

    Box(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF14101C), NavidromeColors.Ink, Color(0xFF0B0A10)),
                    ),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 96.dp, end = 48.dp, top = 72.dp, bottom = 72.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "NAVIDROME TV", style = AppType.overline, color = NavidromeColors.Accent)
                Text(
                    text = "Your library,\non the big screen.",
                    style = AppType.hero,
                    color = NavidromeColors.OnSurface,
                    modifier = Modifier.padding(top = Space.md),
                )
                Text(
                    text = "Connect to your Navidrome server. Your password is stored with the " +
                        "Android Keystore and only used to sign Subsonic requests.",
                    style = AppType.body,
                    color = NavidromeColors.OnSurfaceMuted,
                    modifier = Modifier
                        .padding(top = Space.lg)
                        .fillMaxWidth(0.8f),
                )
            }

            Column(
                modifier = Modifier
                    .width(620.dp)
                    .padding(end = 96.dp),
                verticalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                Text(text = "Server", style = AppType.title, color = NavidromeColors.OnSurface)

                InputRow(
                    label = "Server address",
                    value = state.serverUrl,
                    placeholder = "http://192.168.1.10:4533",
                    onClick = { editing = InputField.SERVER },
                )
                InputRow(
                    label = "Username",
                    value = state.username,
                    placeholder = "your Navidrome user",
                    onClick = { editing = InputField.USERNAME },
                )
                InputRow(
                    label = "Password",
                    value = if (state.password.isEmpty()) "" else "•".repeat(state.password.length),
                    placeholder = "password",
                    onClick = { editing = InputField.PASSWORD },
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    modifier = Modifier.padding(top = Space.xs),
                ) {
                    ActionButton(
                        text = "Test connection",
                        onClick = viewModel::testConnection,
                        primary = false,
                        enabled = state.canSubmit && !state.busy,
                    )
                    ActionButton(
                        text = if (state.busy) "Signing in…" else "Connect",
                        onClick = { viewModel.connect(onConnected) },
                        enabled = state.canSubmit && !state.busy,
                    )
                }

                when {
                    state.check == ConnectionCheckState.SUCCESS && state.message != null -> Text(
                        text = state.message.orEmpty(),
                        style = AppType.bodyMuted,
                        color = Color(0xFF6BE38B),
                    )
                    state.check == ConnectionCheckState.TESTING -> Text(
                        text = "Contacting server…",
                        style = AppType.bodyMuted,
                        color = NavidromeColors.OnSurfaceMuted,
                    )
                    state.check == ConnectionCheckState.FAILED && state.error != null -> Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Connection failed",
                            style = AppType.bodyMuted,
                            color = Color(0xFFFF8A8A),
                        )
                        Text(
                            text = state.error?.message.orEmpty(),
                            style = AppType.cardSubtitle,
                            color = NavidromeColors.OnSurfaceMuted,
                        )
                    }
                    else -> Text(
                        text = "HTTPS is recommended. Plain HTTP is supported for local servers.",
                        style = AppType.cardSubtitle,
                        color = NavidromeColors.OnSurfaceMuted,
                    )
                }
            }
        }

        editing?.let { field ->
            KeyboardOverlay(
                field = field,
                value = when (field) {
                    InputField.SERVER -> state.serverUrl
                    InputField.USERNAME -> state.username
                    InputField.PASSWORD -> state.password
                },
                onValueChange = { updated ->
                    when (field) {
                        InputField.SERVER -> viewModel.onServerUrlChanged(updated)
                        InputField.USERNAME -> viewModel.onUsernameChanged(updated)
                        InputField.PASSWORD -> viewModel.onPasswordChanged(updated)
                    }
                },
                onDone = { editing = null },
            )
        }
    }
}

@Composable
private fun InputRow(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(text = label, style = AppType.metadata, color = NavidromeColors.OnSurfaceMuted)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .background(NavidromeColors.SurfaceElevated, RoundedCornerShape(14.dp))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) Color.White else NavidromeColors.Border,
                    shape = RoundedCornerShape(14.dp),
                )
                .clickable(onClick = onClick)
                .onFocusChanged { focused = it.isFocused }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = value.ifEmpty { placeholder },
                style = AppType.body,
                color = if (value.isEmpty()) NavidromeColors.OnSurfaceMuted.copy(alpha = 0.55f) else NavidromeColors.OnSurface,
            )
        }
    }
}

@Composable
private fun KeyboardOverlay(
    field: InputField,
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    BackHandler(onBack = onDone)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(760.dp)
                .background(NavidromeColors.Surface, RoundedCornerShape(28.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Text(
                text = when (field) {
                    InputField.SERVER -> "Server address"
                    InputField.USERNAME -> "Username"
                    InputField.PASSWORD -> "Password"
                },
                style = AppType.section,
                color = NavidromeColors.OnSurface,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(NavidromeColors.Ink, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = if (value.isEmpty()) "…" else value,
                    style = AppType.body,
                    color = NavidromeColors.OnSurface,
                )
            }
            TvKeyboard(
                onCharacter = { character -> onValueChange(value + character) },
                onBackspace = { onValueChange(value.dropLast(1)) },
                onClear = { onValueChange("") },
                onSearch = onDone,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                ActionButton(text = "Done", onClick = onDone)
            }
        }
    }
}
