package one.next.player.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LongPressOverlayUiStateTest {

    @Test
    fun overlayUiState_isHiddenWhenNoLongPress() {
        val result = resolveLongPressOverlayUiState(
            isLongPressGestureInAction = false,
            isDebugLongPressOverlayVisible = false,
            longPressSpeed = 2.0f,
            shouldShowOverlay = true,
        )

        assertNull(result)
    }

    @Test
    fun overlayUiState_isHiddenWhenOverlayVisibilityIsDisabled() {
        val result = resolveLongPressOverlayUiState(
            isLongPressGestureInAction = true,
            isDebugLongPressOverlayVisible = false,
            longPressSpeed = 2.0f,
            shouldShowOverlay = false,
        )

        assertNull(result)
    }

    @Test
    fun overlayUiState_showsSpeedDuringLongPress() {
        val result = resolveLongPressOverlayUiState(
            isLongPressGestureInAction = true,
            isDebugLongPressOverlayVisible = false,
            longPressSpeed = 2.0f,
            shouldShowOverlay = true,
        )

        assertEquals("2.0x", result?.speedText)
    }

    @Test
    fun overlayUiState_showsSpeedWhenDebugOverlayIsVisible() {
        val result = resolveLongPressOverlayUiState(
            isLongPressGestureInAction = false,
            isDebugLongPressOverlayVisible = true,
            longPressSpeed = 2.0f,
            shouldShowOverlay = false,
        )

        assertEquals("2.0x", result?.speedText)
    }
}
