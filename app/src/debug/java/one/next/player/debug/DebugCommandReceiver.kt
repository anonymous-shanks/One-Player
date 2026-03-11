package one.next.player.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import one.next.player.core.common.Logger
import one.next.player.core.common.di.ApplicationScope
import one.next.player.core.data.repository.PreferencesRepository
import one.next.player.core.media.sync.MediaSynchronizer

@AndroidEntryPoint
class DebugCommandReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var mediaSynchronizer: MediaSynchronizer

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        applicationScope.launch {
            try {
                when (intent.action) {
                    ACTION_SET_IGNORE_NOMEDIA -> setIgnoreNoMedia(intent)
                    ACTION_REFRESH_LIBRARY -> refreshLibrary()
                    else -> Logger.logInfo(TAG, "Ignored unknown debug action: ${intent.action}")
                }
            } catch (throwable: Throwable) {
                Logger.logError(TAG, "Failed to handle debug command: ${intent.action}", throwable)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun setIgnoreNoMedia(intent: Intent) {
        if (!intent.hasExtra(EXTRA_ENABLED)) return

        val enabled = intent.getBooleanExtra(EXTRA_ENABLED, false)
        preferencesRepository.updateApplicationPreferences {
            it.copy(ignoreNoMediaFiles = enabled)
        }
        Logger.logInfo(TAG, "ignoreNoMediaFiles set to $enabled")
        refreshLibrary()
    }

    private suspend fun refreshLibrary() {
        Logger.logInfo(TAG, "refreshLibrary start")
        mediaSynchronizer.refresh(null)
        mediaSynchronizer.startSync()
        Logger.logInfo(TAG, "Triggered media library refresh")
    }

    companion object {
        private const val TAG = "DebugCommandReceiver"
        const val ACTION_SET_IGNORE_NOMEDIA = "one.next.player.debug.SET_IGNORE_NOMEDIA"
        const val ACTION_REFRESH_LIBRARY = "one.next.player.debug.REFRESH_LIBRARY"
        const val EXTRA_ENABLED = "enabled"
    }
}
