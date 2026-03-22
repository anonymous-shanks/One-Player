package one.next.player.feature.player.subtitle

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleFontRenderPolicyTest {

    @Test
    fun renderPolicy_assTrack_neverUsesExternalFont() {
        val result = decideSubtitleFontPolicy(
            isAssSubtitleSelected = true,
            shouldUseSystemCaptionStyle = false,
            hasExternalFont = true,
        )

        assertEquals(SubtitleFontPolicy.Ass, result)
    }

    @Test
    fun renderPolicy_systemCaptionStyle_ignoresExternalFont() {
        val result = decideSubtitleFontPolicy(
            isAssSubtitleSelected = false,
            shouldUseSystemCaptionStyle = true,
            hasExternalFont = true,
        )

        assertEquals(SubtitleFontPolicy.SystemCaptionStyle, result)
    }

    @Test
    fun renderPolicy_plainTextWithExternalFont_prefersExternalFont() {
        val result = decideSubtitleFontPolicy(
            isAssSubtitleSelected = false,
            shouldUseSystemCaptionStyle = false,
            hasExternalFont = true,
        )

        assertEquals(SubtitleFontPolicy.ExternalOrFallback, result)
    }

    @Test
    fun renderPolicy_plainTextWithoutExternalFont_fallsBackToBuiltIn() {
        val result = decideSubtitleFontPolicy(
            isAssSubtitleSelected = false,
            shouldUseSystemCaptionStyle = false,
            hasExternalFont = false,
        )

        assertEquals(SubtitleFontPolicy.ExternalOrFallback, result)
    }
}
