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
import androidx.media3.ui.compose.state.rememberNextButtonState
import one.next.player.core.ui.R as coreUiR
import one.next.player.feature.player.LocalControlsVisibilityState

@OptIn(UnstableApi::class)
@Composable
internal fun NextButton(
    player: Player,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    label: String? = null,
    isInteractive: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val state = rememberNextButtonState(player)
    val controlsVisibilityState = LocalControlsVisibilityState.current

    PlayerButton(
        modifier = modifier,
        buttonSize = 48.dp,
        isEnabled = state.isEnabled,
        isSelected = isSelected,
        label = label,
        isInteractive = isInteractive,
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
            painter = painterResource(coreUiR.drawable.ic_skip_next),
            contentDescription = stringResource(coreUiR.string.player_controls_next),
            modifier = Modifier.size(28.dp),
        )
    }
}
