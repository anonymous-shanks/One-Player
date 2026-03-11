package one.next.player.core.data.repository

import android.net.Uri
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import one.next.player.core.data.mappers.toFolder
import one.next.player.core.data.mappers.toVideo
import one.next.player.core.data.mappers.toVideoState
import one.next.player.core.data.models.VideoState
import one.next.player.core.database.converter.UriListConverter
import one.next.player.core.database.dao.DirectoryDao
import one.next.player.core.database.dao.MediumDao
import one.next.player.core.database.dao.MediumStateDao
import one.next.player.core.database.entities.MediumStateEntity
import one.next.player.core.database.relations.DirectoryWithMedia
import one.next.player.core.database.relations.MediumWithInfo
import one.next.player.core.model.Folder
import one.next.player.core.model.Video

class LocalMediaRepository @Inject constructor(
    private val mediumDao: MediumDao,
    private val mediumStateDao: MediumStateDao,
    private val directoryDao: DirectoryDao,
) : MediaRepository {

    override fun getVideosFlow(): Flow<List<Video>> = mediumDao.getAllWithInfo().map { media ->
        media.map(MediumWithInfo::toVideo)
    }

    override fun getVideosFlowFromFolderPath(folderPath: String): Flow<List<Video>> = mediumDao
        .getAllWithInfoFromDirectory(folderPath)
        .map { media ->
            media.map(MediumWithInfo::toVideo)
        }

    override fun getRecycleBinVideosFlow(): Flow<List<Video>> = mediumDao.getAllWithInfo().map { media ->
        media.filter { it.isMarkedInRecycleBin() }.map(MediumWithInfo::toVideo)
    }

    override fun getFoldersFlow(): Flow<List<Folder>> = directoryDao.getAllWithMedia().map { it.map(DirectoryWithMedia::toFolder) }

    override suspend fun getVideoByUri(uri: String): Video? = mediumDao.getWithInfo(uri)?.toVideo()

    override suspend fun getVideoState(uri: String): VideoState? = mediumStateDao.get(uri)?.toVideoState()

    override suspend fun updateMediumLastPlayedTime(uri: String, lastPlayedTime: Long) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                lastPlayedTime = lastPlayedTime,
            ),
        )
    }

    override suspend fun updateMediumPosition(uri: String, position: Long) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)
        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                playbackPosition = position,
                lastPlayedTime = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun updateMediumPlaybackSpeed(uri: String, playbackSpeed: Float) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                playbackSpeed = playbackSpeed,
                lastPlayedTime = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun updateMediumAudioTrack(uri: String, audioTrackIndex: Int) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                audioTrackIndex = audioTrackIndex,
                lastPlayedTime = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun updateMediumSubtitleTrack(uri: String, subtitleTrackIndex: Int) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                subtitleTrackIndex = subtitleTrackIndex,
                lastPlayedTime = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun updateMediumZoom(uri: String, zoom: Float) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                videoScale = zoom,
                lastPlayedTime = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun addExternalSubtitleToMedium(uri: String, subtitleUri: Uri) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)
        val currentExternalSubs = UriListConverter.fromStringToList(stateEntity.externalSubs)

        if (currentExternalSubs.contains(subtitleUri)) return
        val newExternalSubs = UriListConverter.fromListToString(urlList = currentExternalSubs + subtitleUri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                externalSubs = newExternalSubs,
                lastPlayedTime = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun updateExternalSubs(uri: String, externalSubs: List<Uri>) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)
        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                externalSubs = UriListConverter.fromListToString(externalSubs),
                lastPlayedTime = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun updateSubtitleDelay(uri: String, delay: Long) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                subtitleDelayMilliseconds = delay,
                lastPlayedTime = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun updateSubtitleSpeed(uri: String, speed: Float) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                subtitleSpeed = speed,
                lastPlayedTime = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun moveVideosToRecycleBin(uris: List<String>) {
        updateRecycleBinState(uris = uris, isInRecycleBin = true)
    }

    override suspend fun restoreVideosFromRecycleBin(uris: List<String>) {
        updateRecycleBinState(uris = uris, isInRecycleBin = false)
    }

    private suspend fun updateRecycleBinState(
        uris: List<String>,
        isInRecycleBin: Boolean,
    ) {
        if (uris.isEmpty()) return

        val distinctUris = uris.distinct()
        val existingStates = mediumStateDao.getAll(distinctUris).associateBy(MediumStateEntity::uriString)
        val updatedStates = distinctUris.map { uri ->
            (existingStates[uri] ?: MediumStateEntity(uriString = uri)).copy(
                isInRecycleBin = isInRecycleBin,
            )
        }

        mediumStateDao.upsertAll(updatedStates)
    }

    private fun MediumWithInfo.isMarkedInRecycleBin(): Boolean = mediumStateEntity?.isInRecycleBin == true
}
