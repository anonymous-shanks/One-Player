package one.next.player

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityThemeBootstrapTest {

    @Test
    fun resolveBootstrapTheme_usesExplicitLightPreference() {
        val result = resolveBootstrapTheme(
            themeConfig = one.next.player.core.model.ThemeConfig.OFF,
            isSystemDarkTheme = true,
        )

        assertEquals(false, result.shouldUseDarkTheme)
    }

    @Test
    fun resolveBootstrapTheme_usesExplicitDarkPreference() {
        val result = resolveBootstrapTheme(
            themeConfig = one.next.player.core.model.ThemeConfig.ON,
            isSystemDarkTheme = false,
        )

        assertEquals(true, result.shouldUseDarkTheme)
    }

    @Test
    fun resolveBootstrapTheme_followsSystemWhenConfigured() {
        val result = resolveBootstrapTheme(
            themeConfig = one.next.player.core.model.ThemeConfig.SYSTEM,
            isSystemDarkTheme = true,
        )

        assertEquals(true, result.shouldUseDarkTheme)
    }

    @Test
    fun readPersistedThemeConfig_readsExplicitLightPreference() {
        val dataDir = createTempDataDir(
            content = """{"themeConfig":"OFF"}""",
        )

        val result = readPersistedThemeConfig(dataDir = dataDir.absolutePath)

        assertEquals(one.next.player.core.model.ThemeConfig.OFF, result)
    }

    @Test
    fun readPersistedThemeConfig_fallsBackToSystemWhenMissing() {
        val dataDir = Files.createTempDirectory("theme-bootstrap-empty").toFile()

        val result = readPersistedThemeConfig(dataDir = dataDir.absolutePath)

        assertEquals(one.next.player.core.model.ThemeConfig.SYSTEM, result)
    }

    private fun createTempDataDir(
        content: String,
    ): File {
        val dataDir = Files.createTempDirectory("theme-bootstrap").toFile()
        val preferencesFile = dataDir.resolve("files/datastore/app_preferences.json")
        preferencesFile.parentFile?.mkdirs()
        preferencesFile.writeText(content)
        return dataDir
    }
}
