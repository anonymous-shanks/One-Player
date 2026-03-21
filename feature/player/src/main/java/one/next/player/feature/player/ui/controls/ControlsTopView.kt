package one.next.player.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import one.next.player.core.ui.R
import one.next.player.core.ui.extensions.copy
import one.next.player.feature.player.buttons.PlayerButton

@OptIn(UnstableApi::class)
@Composable
fun ControlsTopView(
    modifier: Modifier = Modifier,
    title: String,
    isCustomizingControls: Boolean = false,
    isBackVisible: Boolean = true,
    isBackSelected: Boolean = false,
    isBackInteractive: Boolean = true,
    isPlaylistVisible: Boolean = true,
    isPlaylistSelected: Boolean = false,
    isPlaybackSpeedVisible: Boolean = true,
    isPlaybackSpeedSelected: Boolean = false,
    isAudioVisible: Boolean = true,
    isAudioSelected: Boolean = false,
    isSubtitleVisible: Boolean = true,
    isSubtitleSelected: Boolean = false,
    onAudioClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    onPlaybackSpeedClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {},
    onBackClick: () -> Unit,
) {
    val systemBarsPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    Row(
        modifier = modifier
            .padding(systemBarsPadding.copy(bottom = 0.dp))
            .padding(horizontal = 8.dp)
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (isBackVisible) {
            PlayerButton(
                onClick = onBackClick,
                isSelected = isBackSelected,
                label = stringResource(R.string.player_controls_exit).takeIf { isCustomizingControls },
                shouldShowSelectionBadge = false,
                shouldDimWhenUnselected = false,
                shouldShowCustomizeFrame = false,
                isInteractive = isBackInteractive,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "btn_back",
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isPlaylistVisible) {
                PlayerButton(
                    onClick = onPlaylistClick,
                    isSelected = isPlaylistSelected,
                    label = stringResource(R.string.now_playing).takeIf { isCustomizingControls },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_playlist),
                        contentDescription = "btn_playlist",
                    )
                }
            }
            if (isPlaybackSpeedVisible) {
                PlayerButton(
                    onClick = onPlaybackSpeedClick,
                    isSelected = isPlaybackSpeedSelected,
                    label = stringResource(R.string.speed).takeIf { isCustomizingControls },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_speed),
                        contentDescription = "btn_speed",
                    )
                }
            }
            if (isAudioVisible) {
                PlayerButton(
                    onClick = onAudioClick,
                    isSelected = isAudioSelected,
                    label = stringResource(R.string.audio).takeIf { isCustomizingControls },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_audio_track),
                        contentDescription = "btn_audio",
                    )
                }
            }
            if (isSubtitleVisible) {
                PlayerButton(
                    onClick = onSubtitleClick,
                    isSelected = isSubtitleSelected,
                    label = stringResource(R.string.subtitle).takeIf { isCustomizingControls },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_subtitle_track),
                        contentDescription = "btn_subtitle",
                    )
                }
            }
        }
    }
}
