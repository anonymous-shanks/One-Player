package one.next.player.core.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import one.next.player.core.data.repository.fake.FakePreferencesRepository
import one.next.player.core.model.SettingsBackup
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LocalSubtitleFontRepositoryTest {

    private lateinit var context: Context
    private lateinit var tempDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        tempDir = File(context.cacheDir, "subtitle_font_test_${System.nanoTime()}")
        tempDir.mkdirs()
    }

    @After
    fun teardown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun state_isUnavailableWhenNoFontExists() = runTest {
        val repository = buildRepository(tempDir = tempDir)

        assertFalse(repository.state.value.isAvailable)
        assertEquals("", repository.state.value.displayName)
        assertNull(repository.source.value)
    }

    @Test
    fun importFont_ttf_updatesStateAndSource() = runTest {
        val repository = buildRepository(tempDir = tempDir)

        repository.importFont(createTempFontUri(fileName = "test-font.ttf", contents = VALID_TEST_FONT_BYTES))

        assertEquals(true, repository.state.value.isAvailable)
        assertEquals("test-font.ttf", repository.state.value.displayName)
        assertEquals(tempDir.resolve("subtitle-fonts/current.font").absolutePath, repository.source.value?.absolutePath)
    }

    @Test
    fun importFont_invalidFile_keepsPreviousFontAndDisplayName() = runTest {
        val repository = buildRepository(tempDir = tempDir)
        repository.importFont(createTempFontUri(fileName = "test-font.ttf", contents = VALID_TEST_FONT_BYTES))

        runCatching {
            repository.importFont(createTempFontUri(fileName = "invalid-font.ttf", contents = INVALID_TEST_FONT_BYTES))
        }

        assertEquals(true, repository.state.value.isAvailable)
        assertEquals("test-font.ttf", repository.state.value.displayName)
        assertEquals(tempDir.resolve("subtitle-fonts/current.font").absolutePath, repository.source.value?.absolutePath)
    }

    @Test
    fun clearFont_removesFileAndState() = runTest {
        val repository = buildRepository(tempDir = tempDir)
        repository.importFont(createTempFontUri(fileName = "test-font.ttf", contents = VALID_TEST_FONT_BYTES))

        repository.clearFont()

        assertFalse(repository.state.value.isAvailable)
        assertEquals("", repository.state.value.displayName)
        assertNull(repository.source.value)
        assertFalse(tempDir.resolve("subtitle-fonts/current.font").exists())
        assertFalse(tempDir.resolve("subtitle-fonts/current.json").exists())
    }

    @Test
    fun brokenMeta_selfHealsToEmptyState() = runTest {
        val repository = buildRepository(tempDir = tempDir)
        repository.importFont(createTempFontUri(fileName = "test-font.ttf", contents = VALID_TEST_FONT_BYTES))
        tempDir.resolve("subtitle-fonts/current.json").writeText("{broken")

        val reloadedRepository = buildRepository(tempDir = tempDir)

        assertFalse(reloadedRepository.state.value.isAvailable)
        assertEquals("", reloadedRepository.state.value.displayName)
        assertNull(reloadedRepository.source.value)
        assertFalse(tempDir.resolve("subtitle-fonts/current.font").exists())
        assertFalse(tempDir.resolve("subtitle-fonts/current.json").exists())
    }

    @Test
    fun missingFont_selfHealsToEmptyState() = runTest {
        val repository = buildRepository(tempDir = tempDir)
        repository.importFont(createTempFontUri(fileName = "test-font.ttf", contents = VALID_TEST_FONT_BYTES))
        tempDir.resolve("subtitle-fonts/current.font").delete()

        val reloadedRepository = buildRepository(tempDir = tempDir)

        assertFalse(reloadedRepository.state.value.isAvailable)
        assertEquals("", reloadedRepository.state.value.displayName)
        assertNull(reloadedRepository.source.value)
        assertFalse(tempDir.resolve("subtitle-fonts/current.json").exists())
    }

    @Test
    fun exportSettings_doesNotContainExternalFontState() = runTest {
        val repository = buildRepository(tempDir = tempDir)
        repository.importFont(createTempFontUri(fileName = "test-font.ttf", contents = VALID_TEST_FONT_BYTES))
        val preferencesRepository = FakePreferencesRepository()

        val backup = preferencesRepository.exportSettings()

        assertEquals(SettingsBackup(), backup)
        assertEquals(true, repository.state.value.isAvailable)
        assertEquals("test-font.ttf", repository.state.value.displayName)
    }

    @Test
    fun importAndResetPreferences_doNotDeleteExternalFontFiles() = runTest {
        val repository = buildRepository(tempDir = tempDir)
        repository.importFont(createTempFontUri(fileName = "test-font.ttf", contents = VALID_TEST_FONT_BYTES))
        val preferencesRepository = FakePreferencesRepository()

        preferencesRepository.importSettings(SettingsBackup())
        preferencesRepository.resetPreferences()

        val reloadedRepository = buildRepository(tempDir = tempDir)
        assertEquals(true, reloadedRepository.state.value.isAvailable)
        assertEquals("test-font.ttf", reloadedRepository.state.value.displayName)
        assertEquals(tempDir.resolve("subtitle-fonts/current.font").absolutePath, reloadedRepository.source.value?.absolutePath)
    }
}

private val VALID_TEST_FONT_BYTES = byteArrayOf(0x01, 0x23, 0x45)
private val INVALID_TEST_FONT_BYTES = byteArrayOf(0x54, 0x65, 0x73, 0x74)

private fun createTempFontUri(
    fileName: String,
    contents: ByteArray,
): Uri {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dir = File(context.cacheDir, "subtitle_font_asset_${System.nanoTime()}")
    dir.mkdirs()
    val file = File(dir, fileName)
    FileOutputStream(file).use { output ->
        output.write(contents)
    }
    return Uri.fromFile(file)
}

private class FakeSubtitleFontFileValidator(
    private val predicate: (File) -> Boolean,
) : SubtitleFontFileValidator {
    override fun validate(file: File) {
        require(file.exists()) { "Font file does not exist" }
        require(file.length() > 0L) { "Font file is empty" }
        check(predicate(file)) { "Invalid test font" }
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private fun buildRepository(tempDir: File): LocalSubtitleFontRepository {
    val dispatcher = UnconfinedTestDispatcher()
    val testContext = object : android.content.ContextWrapper(
        ApplicationProvider.getApplicationContext<Context>(),
    ) {
        override fun getFilesDir(): File = tempDir
    }
    return LocalSubtitleFontRepository(
        context = testContext,
        applicationScope = CoroutineScope(SupervisorJob() + dispatcher),
        ioDispatcher = dispatcher,
        subtitleFontFileValidator = FakeSubtitleFontFileValidator { file ->
            file.readBytes().contentEquals(VALID_TEST_FONT_BYTES)
        },
    )
}
