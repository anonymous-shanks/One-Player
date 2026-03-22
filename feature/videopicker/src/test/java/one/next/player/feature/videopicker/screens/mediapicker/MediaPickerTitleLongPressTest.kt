package one.next.player.feature.videopicker.screens.mediapicker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPickerTitleLongPressTest {

    @Test
    fun titleLongPress_isDisabledWhenPreferenceIsFalse() {
        val result = shouldEnableTitleLongPressHomeNavigation(
            isInSelectionMode = false,
            folderName = "Movies",
            shouldNavigateHomeOnTitleLongPress = false,
        )

        assertFalse(result)
    }

    @Test
    fun titleLongPress_isEnabledOnlyForChildFolderWhenPreferenceIsTrue() {
        assertTrue(
            shouldEnableTitleLongPressHomeNavigation(
                isInSelectionMode = false,
                folderName = "Movies",
                shouldNavigateHomeOnTitleLongPress = true,
            ),
        )
        assertFalse(
            shouldEnableTitleLongPressHomeNavigation(
                isInSelectionMode = true,
                folderName = "Movies",
                shouldNavigateHomeOnTitleLongPress = true,
            ),
        )
        assertFalse(
            shouldEnableTitleLongPressHomeNavigation(
                isInSelectionMode = false,
                folderName = null,
                shouldNavigateHomeOnTitleLongPress = true,
            ),
        )
    }
}
