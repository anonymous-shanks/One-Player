package one.next.player.feature.player.buttons

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPreviousButtonState
import one.next.player.core.ui.R as coreUiR
import one.next.player.feature.player.LocalControlsVisibilityState

@OptIn(UnstableApi::class)
@Composable
internal fun PreviousButton(
    player: Player,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    label: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val state = rememberPreviousButtonState(player)
    val controlsVisibilityState = LocalControlsVisibilityState.current

    PlayerButton(
        modifier = modifier,
        buttonSize = 48.dp,
        isEnabled = state.isEnabled,
        isSelected = isSelected,
        label = label,
        onClick = {
            if (onClick != null) {
                onClick()
            } else {
                state.onClick()
                controlsVisibilityState?.showControls()
            }
        },
    ) {
        Icon(
            painter = painterResource(coreUiR.drawable.ic_skip_prev),
            contentDescription = stringResource(coreUiR.string.player_controls_previous),
            modifier = Modifier.size(28.dp),
        )
    }
}
