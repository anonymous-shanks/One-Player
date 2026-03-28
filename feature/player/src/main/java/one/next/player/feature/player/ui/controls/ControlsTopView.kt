package one.next.player.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import one.next.player.core.ui.R
import one.next.player.core.ui.extensions.copy
import one.next.player.feature.player.buttons.PlayerButton

@OptIn(UnstableApi::class, ExperimentalFoundationApi::class)
@Composable
fun ControlsTopView(
    modifier: Modifier = Modifier,
    title: String,
    chapterTitle: String? = null,
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
    isLuaVisible: Boolean = true,
    isLuaSelected: Boolean = false,
    onAudioClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    onPlaybackSpeedClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {},
    onLuaClick: () -> Unit = {},
    onChapterClick: () -> Unit = {},
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
        
        // --- Title and Chapter Area ---
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    enabled = chapterTitle != null && !isCustomizingControls,
                    onClick = onChapterClick
                )
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = if (chapterTitle != null) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            
            if (chapterTitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Chapter",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary, // Matches user's theme!
                        maxLines = 1,
                        modifier = Modifier.basicMarquee() // Auto scroll for long chapter names
                    )
                }
            }
        }
        // ------------------------------

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isLuaVisible) {
                PlayerButton(
                    onClick = onLuaClick,
                    isSelected = isLuaSelected,
                    label = "Lua Scripts".takeIf { isCustomizingControls },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Build,
                        contentDescription = "btn_lua",
                    )
                }
            }
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
