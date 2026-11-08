package com.navidrometv.presentation.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.navidrometv.core.designsystem.component.ActionButton
import com.navidrometv.core.designsystem.theme.AppType
import com.navidrometv.core.designsystem.theme.NavidromeColors
import com.navidrometv.core.designsystem.theme.Space

/**
 * Server onboarding. The three values are entered through the device's own
 * keyboard: on Android TV, focusing an editable field raises the system IME, so a
 * remote (or a paired keyboard) is enough and nothing here needs a touchscreen.
 */
@Composable
fun OnboardingScreen(
    onConnected: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(NavidromeColors.Surface, NavidromeColors.Ink, Color(0xFF08090A)),
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

                SetupField(
                    label = "Server address",
                    value = state.serverUrl,
                    placeholder = "http://192.168.1.10:4533",
                    keyboardType = KeyboardType.Uri,
                    onValueChange = viewModel::onServerUrlChanged,
                )
                SetupField(
                    label = "Username",
                    value = state.username,
                    placeholder = "your Navidrome user",
                    onValueChange = viewModel::onUsernameChanged,
                )
                SetupField(
                    label = "Password",
                    value = state.password,
                    placeholder = "password",
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = viewModel::onPasswordChanged,
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
    }
}

/**
 * Text input that delegates typing to the device's own keyboard. On Android TV a
 * focused editable field opens the system (Leanback) IME, which is what remote
 * users expect; the app no longer draws a keyboard of its own during setup.
 */
@Composable
private fun SetupField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Text(text = label, style = AppType.metadata, color = NavidromeColors.OnSurfaceMuted)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = AppType.body.copy(color = NavidromeColors.OnSurface),
            cursorBrush = SolidColor(NavidromeColors.Accent),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { keyboard?.hide() },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .background(NavidromeColors.SurfaceElevated, RoundedCornerShape(14.dp))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) Color.White else NavidromeColors.SurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .onFocusChanged { focusState ->
                    focused = focusState.isFocused
                    // Ask for the system IME explicitly: a D-pad focus change does
                    // not always raise it on TV.
                    if (focusState.isFocused) keyboard?.show()
                },
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = AppType.body,
                            color = NavidromeColors.OnSurfaceMuted.copy(alpha = 0.55f),
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}
