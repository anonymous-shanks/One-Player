package one.next.player.core.media.sync

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import coil3.ImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.anilbeesetti.nextlib.mediainfo.AudioStream
import io.github.anilbeesetti.nextlib.mediainfo.MediaInfoBuilder
import io.github.anilbeesetti.nextlib.mediainfo.SubtitleStream
import io.github.anilbeesetti.nextlib.mediainfo.VideoStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import one.next.player.core.common.Dispatcher
import one.next.player.core.common.Logger
import one.next.player.core.common.NextDispatchers
import one.next.player.core.common.di.ApplicationScope
import one.next.player.core.database.dao.MediumDao
import one.next.player.core.database.entities.AudioStreamInfoEntity
import one.next.player.core.database.entities.SubtitleStreamInfoEntity
import one.next.player.core.database.entities.VideoStreamInfoEntity

class LocalMediaInfoSynchronizer @Inject constructor(
    private val mediumDao: MediumDao,
    private val imageLoader: ImageLoader,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
    @Dispatcher(NextDispatchers.Default) private val dispatcher: CoroutineDispatcher,
) : MediaInfoSynchronizer {

    private val pendingSyncUris = LinkedHashSet<String>()
    private val activeSyncUris = mutableSetOf<String>()
    private val mutex = Mutex()
    private var queueProcessorJob: Job? = null

    override fun sync(uri: Uri) {
        val uriString = uri.toString()
        applicationScope.launch(dispatcher) {
            val shouldStartProcessor = mutex.withLock {
                if (uriString in activeSyncUris || uriString in pendingSyncUris) {
                    return@withLock false
                }

                pendingSyncUris += uriString
                queueProcessorJob?.isActive != true
            }

            if (shouldStartProcessor) {
                queueProcessorJob = applicationScope.launch(dispatcher) {
                    processPendingSyncs()
                }
            }
        }
    }

    override suspend fun clearThumbnailsCache() {
        imageLoader.diskCache?.clear()
        imageLoader.memoryCache?.clear()
    }

    private suspend fun processPendingSyncs() {
        while (true) {
            val nextUriString = mutex.withLock {
                val nextUriString = pendingSyncUris.firstOrNull()
                if (nextUriString == null) {
                    queueProcessorJob = null
                    null
                } else {
                    pendingSyncUris.remove(nextUriString)
                    activeSyncUris += nextUriString
                    nextUriString
                }
            } ?: return

            try {
                performSync(nextUriString.toUri())
            } finally {
                mutex.withLock {
                    activeSyncUris.remove(nextUriString)
                }
            }
        }
    }

    private suspend fun performSync(uri: Uri) {
        val medium = mediumDao.getWithInfo(uri.toString()) ?: return
        val mediumEntity = medium.mediumEntity
        val needsLightweightMetadata = mediumEntity.duration <= 0 || mediumEntity.width <= 0 || mediumEntity.height <= 0
        val needsFullMediaInfo = medium.videoStreamInfo == null

        if (!needsLightweightMetadata && !needsFullMediaInfo) return

        var updatedMedium = mediumEntity

        if (needsLightweightMetadata) {
            extractBasicMetadata(uri)?.let { basicMetadata ->
                updatedMedium = updatedMedium.copy(
                    duration = basicMetadata.duration ?: updatedMedium.duration,
                    width = basicMetadata.width ?: updatedMedium.width,
                    height = basicMetadata.height ?: updatedMedium.height,
                )
            }
        }

        if (!needsFullMediaInfo) {
            if (updatedMedium != mediumEntity) {
                mediumDao.upsert(updatedMedium)
            }
            return
        }

        val mediaInfo = runCatching {
            MediaInfoBuilder().from(context = context, uri = uri).build() ?: throw NullPointerException()
        }.onFailure { throwable ->
            Logger.logError(TAG, "Failed to read media info for $uri", throwable)
        }.getOrNull() ?: run {
            if (updatedMedium != mediumEntity) {
                mediumDao.upsert(updatedMedium)
            }
            return
        }

        try {
            val videoStreamInfo = mediaInfo.videoStream?.toVideoStreamInfoEntity(updatedMedium.uriString)
            val audioStreamsInfo = mediaInfo.audioStreams.map {
                it.toAudioStreamInfoEntity(updatedMedium.uriString)
            }
            val subtitleStreamsInfo = mediaInfo.subtitleStreams.map {
                it.toSubtitleStreamInfoEntity(updatedMedium.uriString)
            }

            mediumDao.upsert(updatedMedium.copy(format = mediaInfo.format))
            videoStreamInfo?.let { mediumDao.upsertVideoStreamInfo(it) }
            audioStreamsInfo.onEach { mediumDao.upsertAudioStreamInfo(it) }
            subtitleStreamsInfo.onEach { mediumDao.upsertSubtitleStreamInfo(it) }
        } finally {
            mediaInfo.release()
        }
    }

    private fun extractBasicMetadata(uri: Uri): BasicMediaMetadata? {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(context, uri)
            BasicMediaMetadata(
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
            )
        }.onFailure { throwable ->
            Logger.logError(TAG, "Failed to read basic media metadata for $uri", throwable)
        }.getOrNull().also {
            retriever.release()
        }
    }

    companion object {
        private const val TAG = "MediaInfoSynchronizer"
    }
}

private data class BasicMediaMetadata(
    val duration: Long?,
    val width: Int?,
    val height: Int?,
)

private fun VideoStream.toVideoStreamInfoEntity(mediumUri: String) = VideoStreamInfoEntity(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    bitRate = bitRate,
    frameRate = frameRate,
    frameWidth = frameWidth,
    frameHeight = frameHeight,
    mediumUri = mediumUri,
)

private fun AudioStream.toAudioStreamInfoEntity(mediumUri: String) = AudioStreamInfoEntity(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    bitRate = bitRate,
    sampleFormat = sampleFormat,
    sampleRate = sampleRate,
    channels = channels,
    channelLayout = channelLayout,
    mediumUri = mediumUri,
)

private fun SubtitleStream.toSubtitleStreamInfoEntity(mediumUri: String) = SubtitleStreamInfoEntity(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    mediumUri = mediumUri,
)
