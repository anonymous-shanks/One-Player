package one.next.player.feature.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import one.next.player.core.data.repository.ExternalSubtitleFontSource
import one.next.player.core.model.ControlButtonsPosition
import one.next.player.core.model.PlayerControl
import one.next.player.core.model.PlayerPreferences
import one.next.player.core.ui.R as coreUiR
import one.next.player.core.ui.extensions.copy
import one.next.player.feature.player.buttons.NextButton
import one.next.player.feature.player.buttons.PlayPauseButton
import one.next.player.feature.player.buttons.PlayerButton
import one.next.player.feature.player.buttons.PreviousButton
import one.next.player.feature.player.extensions.nameRes
import one.next.player.feature.player.state.ControlsVisibilityState
import one.next.player.feature.player.state.VerticalGesture
import one.next.player.feature.player.state.rememberBrightnessState
import one.next.player.feature.player.state.rememberControlsVisibilityState
import one.next.player.feature.player.state.rememberErrorState
import one.next.player.feature.player.state.rememberMediaPresentationState
import one.next.player.feature.player.state.rememberMetadataState
import one.next.player.feature.player.state.rememberPictureInPictureState
import one.next.player.feature.player.state.rememberRotationState
import one.next.player.feature.player.state.rememberSeekGestureState
import one.next.player.feature.player.state.rememberTapGestureState
import one.next.player.feature.player.state.rememberVideoZoomAndContentScaleState
import one.next.player.feature.player.state.rememberVolumeAndBrightnessGestureState
import one.next.player.feature.player.state.rememberVolumeState
import one.next.player.feature.player.state.seekAmountFormatted
import one.next.player.feature.player.state.seekToPositionFormated
import one.next.player.feature.player.ui.DoubleTapIndicator
import one.next.player.feature.player.ui.OverlayShowView
import one.next.player.feature.player.ui.OverlayView
import one.next.player.feature.player.ui.SubtitleConfiguration
import one.next.player.feature.player.ui.VerticalProgressView
import one.next.player.feature.player.ui.controls.ControlsBottomView
import one.next.player.feature.player.ui.controls.ControlsTopView

val LocalControlsVisibilityState = compositionLocalOf<ControlsVisibilityState?> { null }

internal data class LongPressOverlayUiState(
    val speedText: String,
)

internal fun resolveLongPressOverlayUiState(
    isLongPressGestureInAction: Boolean,
    isDebugLongPressOverlayVisible: Boolean,
    longPressSpeed: Float,
    shouldShowOverlay: Boolean,
): LongPressOverlayUiState? {
    if (!shouldShowOverlay && !isDebugLongPressOverlayVisible) return null
    if (!isLongPressGestureInAction && !isDebugLongPressOverlayVisible) return null

    return LongPressOverlayUiState(
        speedText = String.format(Locale.US, "%.1fx", longPressSpeed),
    )
}

@OptIn(UnstableApi::class)
@Composable
internal fun MediaPlayerScreen(
    player: Player?,
    viewModel: PlayerViewModel,
    playerPreferences: PlayerPreferences,
    externalSubtitleFontSource: ExternalSubtitleFontSource?,
    modifier: Modifier = Modifier,
    onSelectSubtitleClick: () -> Unit,
    onBackClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    isTakingScreenshot: Boolean = false,
    onScreenshotClick: () -> Unit,
) {
    val volumeState = rememberVolumeState(
        player = player,
        shouldShowVolumePanelIfHeadsetIsOn = playerPreferences.shouldShowSystemVolumePanel,
    )
    player ?: return
    val metadataState = rememberMetadataState(player)
    val mediaPresentationState = rememberMediaPresentationState(player)
    val controlsVisibilityState = rememberControlsVisibilityState(
        player = player,
        hideAfter = playerPreferences.controllerAutoHideTimeout.seconds,
    )
    val tapGestureState = rememberTapGestureState(
        player = player,
        doubleTapGesture = playerPreferences.doubleTapGesture,
        seekIncrementMillis = playerPreferences.seekIncrement.seconds.inWholeMilliseconds,
        shouldUseLongPressGesture = playerPreferences.shouldUseLongPressControls,
        longPressSpeed = playerPreferences.longPressControlsSpeed,
    )
    val seekGestureState = rememberSeekGestureState(
        player = player,
        sensitivity = playerPreferences.seekSensitivity,
        isSeekGestureEnabled = playerPreferences.shouldUseSeekControls,
    )
    val pictureInPictureState = rememberPictureInPictureState(
        player = player,
        shouldAutoEnter = playerPreferences.shouldAutoEnterPip,
    )
    val videoZoomAndContentScaleState = rememberVideoZoomAndContentScaleState(
        player = player,
        initialContentScale = playerPreferences.playerVideoZoom,
        isZoomGestureEnabled = playerPreferences.shouldUseZoomControls,
        isPanGestureEnabled = playerPreferences.isPanGestureEnabled,
        onEvent = viewModel::onVideoZoomEvent,
    )
    val brightnessState = rememberBrightnessState()
    val volumeAndBrightnessGestureState = rememberVolumeAndBrightnessGestureState(
        volumeState = volumeState,
        brightnessState = brightnessState,
        isVolumeGestureEnabled = playerPreferences.isVolumeSwipeGestureEnabled,
        isBrightnessGestureEnabled = playerPreferences.isBrightnessSwipeGestureEnabled,
        volumeGestureSensitivity = playerPreferences.volumeGestureSensitivity,
        brightnessGestureSensitivity = playerPreferences.brightnessGestureSensitivity,
    )
    val rotationState = rememberRotationState(
        player = player,
        screenOrientation = playerPreferences.playerScreenOrientation,
    )
    val errorState = rememberErrorState(player = player)

    LaunchedEffect(pictureInPictureState.isInPictureInPictureMode) {
        if (pictureInPictureState.isInPictureInPictureMode) {
            controlsVisibilityState.hideControls()
        }
    }

    LaunchedEffect(tapGestureState.isLongPressGestureInAction) {
        if (tapGestureState.isLongPressGestureInAction) {
            controlsVisibilityState.hideControls()
        }
    }

    LaunchedEffect(seekGestureState.isSeeking) {
        if (seekGestureState.isSeeking) {
            controlsVisibilityState.showControls()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (playerPreferences.shouldRememberPlayerBrightness) {
            brightnessState.setBrightness(playerPreferences.playerBrightness)
        }
    }

    LaunchedEffect(brightnessState.currentBrightness) {
        if (playerPreferences.shouldRememberPlayerBrightness) {
            viewModel.updatePlayerBrightness(brightnessState.currentBrightness)
        }
    }

    var overlayView by remember { mutableStateOf<OverlayView?>(null) }
    var showLuaScriptMenu by remember { mutableStateOf(false) }
    var showChapterMenu by remember { mutableStateOf(false) }
    
    var isCustomizingControls by remember { mutableStateOf(false) }
    var customizingHiddenPlayerControls by remember { mutableStateOf(playerPreferences.hiddenPlayerControls) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val permanentlyVisibleControls = remember {
        setOf(
            PlayerControl.BACK,
            PlayerControl.PREVIOUS,
            PlayerControl.PLAY_PAUSE,
            PlayerControl.NEXT,
        )
    }
    val hiddenPlayerControls = when (isCustomizingControls) {
        true -> customizingHiddenPlayerControls
        false -> playerPreferences.hiddenPlayerControls
    }
    var shouldShowOverlay by remember { mutableStateOf(false) }
    var longPressOverlayAnimationStep by remember { mutableIntStateOf(0) }
    val longPressOverlayUiState = resolveLongPressOverlayUiState(
        isLongPressGestureInAction = tapGestureState.isLongPressGestureInAction,
        isDebugLongPressOverlayVisible = playerPreferences.isDebugLongPressOverlayVisible,
        longPressSpeed = tapGestureState.longPressSpeed,
        shouldShowOverlay = shouldShowOverlay,
    )

    LaunchedEffect(playerPreferences.hiddenPlayerControls, isCustomizingControls) {
        if (!isCustomizingControls) {
            customizingHiddenPlayerControls = playerPreferences.hiddenPlayerControls - permanentlyVisibleControls
        }
    }

    LaunchedEffect(tapGestureState.isLongPressGestureInAction) {
        if (!tapGestureState.isLongPressGestureInAction) {
            shouldShowOverlay = false
            return@LaunchedEffect
        }

        shouldShowOverlay = true
        delay(3.seconds)
        shouldShowOverlay = false
    }

    LaunchedEffect(longPressOverlayUiState != null) {
        if (longPressOverlayUiState == null) {
            longPressOverlayAnimationStep = 0
            return@LaunchedEffect
        }
        while (true) {
            longPressOverlayAnimationStep = 0
            delay(120)
            longPressOverlayAnimationStep = 1
            delay(120)
            longPressOverlayAnimationStep = 2
            delay(120)
            longPressOverlayAnimationStep = 3
            delay(320)
        }
    }

    fun isControlVisible(control: PlayerControl): Boolean = control in permanentlyVisibleControls || isCustomizingControls || control !in hiddenPlayerControls

    fun isControlSelected(control: PlayerControl): Boolean = isCustomizingControls && control !in permanentlyVisibleControls && control !in hiddenPlayerControls

    fun toggleControlVisibility(control: PlayerControl) {
        val currentHiddenPlayerControls = when (isCustomizingControls) {
            true -> customizingHiddenPlayerControls
            false -> playerPreferences.hiddenPlayerControls
        }
        val updatedControls = currentHiddenPlayerControls.toMutableSet().apply {
            if (!add(control)) remove(control)
        }
        if (isCustomizingControls) {
            customizingHiddenPlayerControls = updatedControls
            controlsVisibilityState.showControls(duration = kotlin.time.Duration.INFINITE)
        } else {
            controlsVisibilityState.showControls()
        }
        viewModel.updateHiddenPlayerControls(updatedControls)
    }

    fun enterControlCustomization() {
        player.pause()
        customizingHiddenPlayerControls = playerPreferences.hiddenPlayerControls - permanentlyVisibleControls
        isCustomizingControls = true
        controlsVisibilityState.showControls(duration = kotlin.time.Duration.INFINITE)
    }

    fun exitControlCustomization() {
        isCustomizingControls = false
        controlsVisibilityState.showControls()
    }

    CompositionLocalProvider(LocalControlsVisibilityState provides controlsVisibilityState) {
        Box {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                val safeDrawingTopPadding = WindowInsets.safeDrawing
                    .asPaddingValues()
                    .calculateTopPadding()
                val longPressOverlayTopPadding = maxOf(
                    safeDrawingTopPadding,
                    pictureInPictureState.videoViewRect
                        ?.top
                        ?.let { with(LocalDensity.current) { it.toDp() } }
                        ?: 0.dp,
                ) + 16.dp
                PlayerContentFrame(
                    player = player,
                    pictureInPictureState = pictureInPictureState,
                    controlsVisibilityState = controlsVisibilityState,
                    tapGestureState = tapGestureState,
                    seekGestureState = seekGestureState,
                    videoZoomAndContentScaleState = videoZoomAndContentScaleState,
                    volumeAndBrightnessGestureState = volumeAndBrightnessGestureState,
                    isGesturesEnabled = !isCustomizingControls,
                    subtitleConfiguration = SubtitleConfiguration(
                        shouldUseSystemCaptionStyle = playerPreferences.shouldUseSystemCaptionStyle,
                        shouldShowBackground = playerPreferences.shouldShowSubtitleBackground,
                        font = playerPreferences.subtitleFont,
                        textSize = playerPreferences.subtitleTextSize,
                        shouldUseBoldText = playerPreferences.shouldUseBoldSubtitleText,
                        shouldApplyEmbeddedStyles = playerPreferences.shouldApplyEmbeddedStyles,
                        externalSubtitleFontSource = externalSubtitleFontSource,
                    ),
                )

                AnimatedVisibility(
                    visible = controlsVisibilityState.isControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(
                                    alpha = when (isCustomizingControls) {
                                        true -> 0.75f
                                        false -> 0.3f
                                    },
                                ),
                            ),
                    )
                }

                if (mediaPresentationState.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp),
                    )
                }
                DoubleTapIndicator(tapGestureState = tapGestureState)

                if (longPressOverlayUiState != null) {
                    LongPressSpeedOverlay(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = longPressOverlayTopPadding)
                            .testTag("long_press_speed_overlay"),
                        speedText = longPressOverlayUiState.speedText,
                        animationStep = longPressOverlayAnimationStep,
                    )
                }

                if (controlsVisibilityState.isControlsVisible && controlsVisibilityState.isControlsLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(top = 24.dp),
                    ) {
                        PlayerButton(onClick = { controlsVisibilityState.unlockControls() }) {
                            Icon(
                                painter = painterResource(coreUiR.drawable.ic_lock),
                                contentDescription = stringResource(coreUiR.string.controls_unlock),
                            )
                        }
                    }
                } else {
                    PlayerControlsView(
                        topView = {
                            AnimatedVisibility(
                                visible = controlsVisibilityState.isControlsVisible,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                ControlsTopView(
                                    title = metadataState.title ?: "",
                                    chapterTitle = metadataState.currentChapter?.title,
                                    isCustomizingControls = isCustomizingControls,
                                    isBackVisible = isControlVisible(PlayerControl.BACK),
                                    isBackSelected = isControlSelected(PlayerControl.BACK),
                                    isBackInteractive = !isCustomizingControls,
                                    isPlaylistVisible = isControlVisible(PlayerControl.PLAYLIST),
                                    isPlaylistSelected = isControlSelected(PlayerControl.PLAYLIST),
                                    isPlaybackSpeedVisible = isControlVisible(PlayerControl.PLAYBACK_SPEED),
                                    isPlaybackSpeedSelected = isControlSelected(PlayerControl.PLAYBACK_SPEED),
                                    isAudioVisible = isControlVisible(PlayerControl.AUDIO),
                                    isAudioSelected = isControlSelected(PlayerControl.AUDIO),
                                    isSubtitleVisible = isControlVisible(PlayerControl.SUBTITLE),
                                    isSubtitleSelected = isControlSelected(PlayerControl.SUBTITLE),
                                    isLuaVisible = !isCustomizingControls,
                                    isLuaSelected = false,
                                    onChapterClick = {
                                        if (!isCustomizingControls) {
                                            controlsVisibilityState.hideControls()
                                            showChapterMenu = true
                                        }
                                    },
                                    onLuaClick = {
                                        if (!isCustomizingControls) {
                                            controlsVisibilityState.hideControls()
                                            showLuaScriptMenu = true
                                        }
                                    },
                                    onAudioClick = {
                                        if (isCustomizingControls) {
                                            toggleControlVisibility(PlayerControl.AUDIO)
                                        } else {
                                            controlsVisibilityState.hideControls()
                                            overlayView = OverlayView.AUDIO_SELECTOR
                                        }
                                    },
                                    onSubtitleClick = {
                                        if (isCustomizingControls) {
                                            toggleControlVisibility(PlayerControl.SUBTITLE)
                                        } else {
                                            controlsVisibilityState.hideControls()
                                            overlayView = OverlayView.SUBTITLE_SELECTOR
                                        }
                                    },
                                    onPlaybackSpeedClick = {
                                        if (isCustomizingControls) {
                                            toggleControlVisibility(PlayerControl.PLAYBACK_SPEED)
                                        } else {
                                            controlsVisibilityState.hideControls()
                                            overlayView = OverlayView.PLAYBACK_SPEED
                                        }
                                    },
                                    onPlaylistClick = {
                                        if (isCustomizingControls) {
                                            toggleControlVisibility(PlayerControl.PLAYLIST)
                                        } else {
                                            controlsVisibilityState.hideControls()
                                            overlayView = OverlayView.PLAYLIST
                                        }
                                    },
                                    onBackClick = {
                                        if (!isCustomizingControls) {
                                            onBackClick()
                                        }
                                    },
                                )
                            }
                        },
                        middleView = {
                            when {
                                seekGestureState.seekAmount != null -> InfoView(info = "${seekGestureState.seekAmountFormatted}\n[${seekGestureState.seekToPositionFormated}]")
                                videoZoomAndContentScaleState.isZooming -> InfoView(info = "${(videoZoomAndContentScaleState.zoom * 100).toInt()}%")
                                videoZoomAndContentScaleState.shouldShowContentScaleIndicator -> InfoView(info = stringResource(videoZoomAndContentScaleState.videoContentScale.nameRes()))
                                controlsVisibilityState.isControlsVisible -> ControlsMiddleView(
                                    player = player,
                                    isCustomizingControls = isCustomizingControls,
                                    isPreviousVisible = isControlVisible(PlayerControl.PREVIOUS),
                                    isPreviousSelected = isControlSelected(PlayerControl.PREVIOUS),
                                    isPlayPauseVisible = isControlVisible(PlayerControl.PLAY_PAUSE),
                                    isPlayPauseSelected = isControlSelected(PlayerControl.PLAY_PAUSE),
                                    isNextVisible = isControlVisible(PlayerControl.NEXT),
                                    isNextSelected = isControlSelected(PlayerControl.NEXT),
                                    onPreviousClick = { },
                                    onPlayPauseClick = { },
                                    onNextClick = { },
                                )
                                else -> Unit
                            }
                        },
                        bottomView = {
                            AnimatedVisibility(
                                visible = controlsVisibilityState.isControlsVisible && !controlsVisibilityState.isControlsLocked,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                ControlsBottomView(
                                    player = player,
                                    mediaPresentationState = mediaPresentationState,
                                    controlsAlignment = when (playerPreferences.controlButtonsPosition) {
                                        ControlButtonsPosition.LEFT -> Alignment.Start
                                        ControlButtonsPosition.RIGHT -> Alignment.End
                                    },
                                    videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                                    isPipSupported = pictureInPictureState.isPipSupported,
                                    pendingSeekPosition = seekGestureState.pendingSeekPosition,
                                    isCustomizingControls = isCustomizingControls,
                                    visiblePlayerControls = PlayerControl.entries.toSet() - hiddenPlayerControls,
                                    onSeek = seekGestureState::onSeek,
                                    onSeekEnd = seekGestureState::onSeekEnd,
                                    onRotateClick = {
                                        if (isCustomizingControls) {
                                            toggleControlVisibility(PlayerControl.ROTATE)
                                        } else {
                                            rotationState.rotate()
                                        }
                                    },
                                    onPlayInBackgroundClick = {
                                        if (isCustomizingControls) {
                                            toggleControlVisibility(PlayerControl.BACKGROUND_PLAY)
                                        } else {
                                            onPlayInBackgroundClick()
                                        }
                                    },
                                    isTakingScreenshot = isTakingScreenshot,
                                    onScreenshotClick = {
                                        if (isCustomizingControls) {
                                            toggleControlVisibility(PlayerControl.SCREENSHOT)
                                        } else {
                                            onScreenshotClick()
                                        }
                                    },
                                    onCustomizeControlsClick = {
                                        if (isCustomizingControls) {
                                            exitControlCustomization()
                                        } else {
                                            enterControlCustomization()
                                        }
                                    },
                                    onLoopClick = {
                                        toggleControlVisibility(PlayerControl.LOOP)
                                    }.takeIf { isCustomizingControls },
                                    onShuffleClick = {
                                        toggleControlVisibility(PlayerControl.SHUFFLE)
                                    }.takeIf { isCustomizingControls },
                                    onLockControlsClick = {
                                        if (isCustomizingControls) {
                                            toggleControlVisibility(PlayerControl.LOCK)
                                        } else {
                                            controlsVisibilityState.showControls()
                                            controlsVisibilityState.lockControls()
                                        }
                                    },
                                    onVideoContentScaleClick = {
                                        if (isCustomizingControls) {
                                            toggleControlVisibility(PlayerControl.SCALE)
                                        } else {
                                            controlsVisibilityState.showControls()
                                            videoZoomAndContentScaleState.switchToNextVideoContentScale()
                                        }
                                    },
                                    onVideoContentScaleLongClick = {
                                        if (!isCustomizingControls) {
                                            controlsVisibilityState.hideControls()
                                            overlayView = OverlayView.VIDEO_CONTENT_SCALE
                                        }
                                    },
                                    onPictureInPictureClick = {
                                        if (isCustomizingControls) {
                                            toggleControlVisibility(PlayerControl.PIP)
                                        } else if (!pictureInPictureState.hasPipPermission) {
                                            Toast.makeText(context, coreUiR.string.enable_pip_from_settings, Toast.LENGTH_SHORT).show()
                                            pictureInPictureState.openPictureInPictureSettings()
                                        } else {
                                            pictureInPictureState.enterPictureInPictureMode()
                                        }
                                    },
                                )
                            }
                        },
                    )
                }

                val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .displayCutoutPadding()
                        .padding(systemBarsPadding.copy(top = 0.dp, bottom = 0.dp))
                        .padding(24.dp),
                ) {
                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.CenterStart),
                        visible = volumeAndBrightnessGestureState.activeGesture == VerticalGesture.VOLUME,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        VerticalProgressView(
                            value = volumeState.volumePercentage,
                            maxValue = volumeState.maxVolumePercentage,
                            icon = painterResource(coreUiR.drawable.ic_volume),
                        )
                    }

                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        visible = volumeAndBrightnessGestureState.activeGesture == VerticalGesture.BRIGHTNESS,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        VerticalProgressView(
                            value = brightnessState.brightnessPercentage,
                            icon = painterResource(coreUiR.drawable.ic_brightness),
                        )
                    }
                }
            }

            OverlayShowView(
                player = player,
                overlayView = overlayView,
                videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                onDismiss = { overlayView = null },
                onSelectSubtitleClick = onSelectSubtitleClick,
                onSubtitleOptionEvent = viewModel::onSubtitleOptionEvent,
                onVideoContentScaleChanged = { videoZoomAndContentScaleState.onVideoContentScaleChanged(it) },
            )

            // --- LUA SIDE PANEL ---
            if (showLuaScriptMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showLuaScriptMenu = false
                            controlsVisibilityState.showControls()
                        }
                )
            }

            AnimatedVisibility(
                visible = showLuaScriptMenu,
                enter = slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = { fullWidth -> fullWidth } 
                ),
                exit = slideOutHorizontally(
                    animationSpec = tween(300),
                    targetOffsetX = { fullWidth -> fullWidth }
                ),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(400.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {} 
                ) {
                    one.next.player.feature.player.scripting.LuaScriptMenuSidePanel(
                        onDismissRequest = {
                            showLuaScriptMenu = false
                            controlsVisibilityState.showControls()
                        }
                    )
                }
            }
            // -----------------------

            // --- CHAPTER SIDE PANEL WITH HIGHLIGHT, AUTO-SCROLL & TIME RANGES ---
            if (showChapterMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showChapterMenu = false
                            controlsVisibilityState.showControls()
                        }
                )
            }

            AnimatedVisibility(
                visible = showChapterMenu,
                enter = slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = { it }
                ),
                exit = slideOutHorizontally(
                    animationSpec = tween(300),
                    targetOffsetX = { it }
                ),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(400.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                ) {
                    val listState = rememberLazyListState()
                    val chapters = metadataState.chapters
                    val currentChapterIndex = metadataState.currentChapter?.index ?: -1

                    // Auto scroll to current chapter when menu opens
                    LaunchedEffect(showChapterMenu, currentChapterIndex) {
                        if (showChapterMenu && currentChapterIndex >= 0 && chapters.isNotEmpty()) {
                            listState.animateScrollToItem(currentChapterIndex)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chapters",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { 
                                showChapterMenu = false
                                controlsVisibilityState.showControls()
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chapters) { chapter ->
                                val isSelected = chapter.index == currentChapterIndex
                                val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bgColor)
                                        .clickable {
                                            player.seekTo(chapter.startTimeMs)
                                            showChapterMenu = false
                                            controlsVisibilityState.showControls()
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${chapter.index + 1}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor.copy(alpha = 0.7f),
                                        modifier = Modifier.width(32.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = chapter.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = textColor
                                        )
                                        Text(
                                            text = chapter.timeRangeString,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textColor.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // ----------------------------------------------------
        }
    }

    errorState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(text = stringResource(coreUiR.string.error_playing_video))
            },
            text = {
                Text(text = error.message ?: stringResource(coreUiR.string.unknown_error))
            },
            confirmButton = {
                if (player.hasNextMediaItem()) {
                    TextButton(
                        onClick = {
                            errorState.dismiss()
                            player.seekToNext()
                            player.play()
                        },
                    ) {
                        Text(text = stringResource(coreUiR.string.play_next_video))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        errorState.dismiss()
                        onBackClick()
                    },
                ) {
                    Text(text = stringResource(coreUiR.string.exit))
                }
            },
        )
    }

    BackHandler {
        if (overlayView != null) {
            overlayView = null
        } else if (showLuaScriptMenu) {
            showLuaScriptMenu = false
            controlsVisibilityState.showControls()
        } else if (showChapterMenu) {
            showChapterMenu = false
            controlsVisibilityState.showControls()
        } else if (isCustomizingControls) {
            exitControlCustomization()
        } else {
            onBackClick()
        }
    }
}

@Composable
fun InfoView(
    modifier: Modifier = Modifier,
    info: String,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = info,
            style = textStyle,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LongPressSpeedOverlay(
    speedText: String,
    animationStep: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LongPressSpeedIndicator(animationStep = animationStep)
        Text(
            text = speedText,
            modifier = Modifier.testTag("long_press_speed_text"),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.10f),
                    offset = Offset(0f, 1f),
                    blurRadius = 2f,
                ),
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun LongPressSpeedIndicator(
    animationStep: Int,
    modifier: Modifier = Modifier,
) {
    val alpha1 by animateFloatAsState(
        targetValue = if (animationStep >= 1) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "long_press_arrow_1",
    )
    val alpha2 by animateFloatAsState(
        targetValue = if (animationStep >= 2) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "long_press_arrow_2",
    )
    val alpha3 by animateFloatAsState(
        targetValue = if (animationStep >= 3) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "long_press_arrow_3",
    )

    Row(
        modifier = modifier.testTag("long_press_speed_indicator"),
        horizontalArrangement = Arrangement.spacedBy((-1).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LongPressSpeedArrow(alpha = alpha1)
        LongPressSpeedArrow(alpha = alpha2)
        LongPressSpeedArrow(alpha = alpha3)
    }
}

@Composable
private fun LongPressSpeedArrow(alpha: Float) {
    Icon(
        painter = painterResource(coreUiR.drawable.ic_play),
        contentDescription = null,
        modifier = Modifier.size(11.dp),
        tint = Color.White.copy(alpha = alpha),
    )
}

@Composable
fun ControlsMiddleView(
    modifier: Modifier = Modifier,
    player: Player,
    isCustomizingControls: Boolean = false,
    isPreviousVisible: Boolean = true,
    isPreviousSelected: Boolean = false,
    isPlayPauseVisible: Boolean = true,
    isPlayPauseSelected: Boolean = false,
    isNextVisible: Boolean = true,
    isNextSelected: Boolean = false,
    onPreviousClick: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(40.dp, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPreviousVisible) {
            if (isCustomizingControls) {
                PreviousButton(
                    player = player,
                    onClick = onPreviousClick,
                    isInteractive = false,
                )
            } else {
                PreviousButton(player = player)
            }
        }
        if (isPlayPauseVisible) {
            if (isCustomizingControls) {
                PlayPauseButton(
                    player = player,
                    onClick = onPlayPauseClick,
                    isInteractive = false,
                )
            } else {
                PlayPauseButton(player = player)
            }
        }
        if (isNextVisible) {
            if (isCustomizingControls) {
                NextButton(
                    player = player,
                    onClick = onNextClick,
                    isInteractive = false,
                )
            } else {
                NextButton(player = player)
            }
        }
    }
}

@Composable
fun PlayerControlsView(
    modifier: Modifier = Modifier,
    topView: @Composable () -> Unit,
    middleView: @Composable BoxScope.() -> Unit,
    bottomView: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            topView()
            Spacer(modifier = Modifier.weight(1f))
            bottomView()
        }

        middleView()
    }
}
