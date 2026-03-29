package one.next.player.feature.player.state

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.listen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.next.player.core.common.Logger
import java.io.EOFException
import java.io.FileInputStream

data class VideoChapter(
    val index: Int,
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long
) {
    val timeRangeString: String
        get() {
            fun format(ms: Long): String {
                if (ms < 0) return "..."
                val totalSec = ms / 1000
                val h = totalSec / 3600
                val m = (totalSec % 3600) / 60
                val s = totalSec % 60
                return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
                else String.format("%02d:%02d", m, s)
            }
            return "${format(startTimeMs)} - ${format(endTimeMs)}"
        }
}

@Composable
fun rememberMetadataState(player: Player): MetadataState {
    val context = LocalContext.current.applicationContext
    val metadataState = remember { MetadataState(player, context) }
    LaunchedEffect(player) { metadataState.observe() }
    return metadataState
}

@Stable
class MetadataState(private val player: Player, private val context: Context) {
    var title: String? by mutableStateOf(null)
        private set

    var chapters: List<VideoChapter> by mutableStateOf(emptyList())
        private set

    var currentChapter: VideoChapter? by mutableStateOf(null)
        private set

    suspend fun observe() {
        coroutineScope {
            updateMetadata()
            updateChapters()
            
            // Auto update active chapter highlight during playback
            launch {
                while (true) {
                    delay(500)
                    if (player.isPlaying) {
                        updateCurrentChapter()
                    }
                }
            }

            player.listen { events ->
                if (events.containsAny(Player.EVENT_MEDIA_METADATA_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    updateMetadata()
                }
                if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    launch { updateChapters() }
                }
                if (events.containsAny(Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_TIMELINE_CHANGED)) {
                    updateCurrentChapter()
                }
            }
        }
    }

    private fun updateMetadata() {
        title = player.mediaMetadata.title?.toString()
    }

    private suspend fun updateChapters() {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) {
            chapters = emptyList()
            currentChapter = null
            return
        }

        val windowIndex = player.currentMediaItemIndex
        if (windowIndex >= timeline.windowCount) return

        val window = Timeline.Window()
        timeline.getWindow(windowIndex, window)

        val duration = player.duration
        val safeDuration = if (duration > 0 && duration != androidx.media3.common.C.TIME_UNSET) duration else 0L

        // Step 1: Check Native ExoPlayer Chapters (Works for MP4)
        val firstPeriod = window.firstPeriodIndex
        val lastPeriod = window.lastPeriodIndex
        val newChapters = mutableListOf<VideoChapter>()

        if (lastPeriod > firstPeriod) {
            val period = Timeline.Period()
            for (i in firstPeriod..lastPeriod) {
                timeline.getPeriod(i, period)
                val chapterIndex = i - firstPeriod
                val startTimeMs = period.positionInWindowMs
                val endTimeMs = if (i < lastPeriod) {
                    val nextPeriod = Timeline.Period()
                    timeline.getPeriod(i + 1, nextPeriod)
                    nextPeriod.positionInWindowMs
                } else safeDuration
                
                newChapters.add(VideoChapter(chapterIndex, "Chapter ${chapterIndex + 1}", startTimeMs, endTimeMs))
            }
            chapters = newChapters
            updateCurrentChapter()
            return
        }

        // Step 2: Custom MKV EBML Parser (Background thread par file scan hogi)
        val mediaItem = player.currentMediaItem
        val uriString = mediaItem?.localConfiguration?.uri?.toString() ?: mediaItem?.mediaId
        if (uriString != null && (uriString.endsWith(".mkv", true) || uriString.startsWith("content://"))) {
            try {
                val uri = Uri.parse(uriString)
                val parsedChapters = withContext(Dispatchers.IO) {
                    MkvChapterParser.parse(context, uri, safeDuration)
                }
                if (parsedChapters.isNotEmpty()) {
                    chapters = parsedChapters
                    updateCurrentChapter()
                    return
                }
            } catch (e: Exception) {
                Logger.error("MetadataState", "MKV Parse failed", e)
            }
        }

        chapters = emptyList()
        updateCurrentChapter()
    }

    private fun updateCurrentChapter() {
        if (chapters.isEmpty()) {
            currentChapter = null
            return
        }
        val currentPosition = player.currentPosition
        currentChapter = chapters.lastOrNull { currentPosition >= it.startTimeMs } ?: chapters.first()
    }
}

// --- CUSTOM MKV BINARY CHAPTER PARSER ---
object MkvChapterParser {
    fun parse(context: Context, uri: Uri, totalDurationMs: Long): List<VideoChapter> {
        val chapters = mutableListOf<VideoChapter>()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    val reader = EbmlReader(fis)
                    val segmentId = reader.readElementId()
                    if (segmentId != 0x18538067L) return emptyList()
                    
                    val segmentSize = reader.readElementSize() ?: return emptyList()
                    val segmentDataStart = reader.position
                    var chaptersOffset = -1L

                    // Find SeekHead or Chapters in the first 1MB of the file
                    while (reader.position < segmentDataStart + 1024 * 1024) {
                        val id = reader.readElementId() ?: break
                        val size = reader.readElementSize() ?: break

                        if (id == 0x1043A770L) { // Chapters Found Directly
                            parseChaptersElement(reader, size, totalDurationMs, chapters)
                            break
                        } else if (id == 0x114D9B74L) { // SeekHead Found (Points to Chapters)
                            val seekHeadEnd = reader.position + size
                            while (reader.position < seekHeadEnd) {
                                val childId = reader.readElementId() ?: break
                                val childSize = reader.readElementSize() ?: break
                                if (childId == 0x4DBBL) { // Seek Element
                                    val seekEnd = reader.position + childSize
                                    var seekId = 0L
                                    var seekPos = 0L
                                    while (reader.position < seekEnd) {
                                        val leafId = reader.readElementId() ?: break
                                        val leafSize = reader.readElementSize() ?: break
                                        when (leafId) {
                                            0x53ABL -> seekId = reader.readUint(leafSize)
                                            0x53ACL -> seekPos = reader.readUint(leafSize)
                                            else -> reader.skip(leafSize)
                                        }
                                    }
                                    if (seekId == 0x1043A770L) { // Points to Chapters!
                                        chaptersOffset = segmentDataStart + seekPos
                                    }
                                } else {
                                    reader.skip(childSize)
                                }
                            }
                            if (chaptersOffset != -1L) {
                                reader.position = chaptersOffset
                                reader.channel.position(chaptersOffset)
                                val chapId = reader.readElementId()
                                if (chapId == 0x1043A770L) {
                                    val chapSize = reader.readElementSize() ?: break
                                    parseChaptersElement(reader, chapSize, totalDurationMs, chapters)
                                    break
                                }
                            }
                        } else {
                            reader.skip(size)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore for unsupported files
        }
        
        // Finalize start and end times properly
        if (chapters.isNotEmpty()) {
            chapters.sortBy { it.startTimeMs }
            for (i in chapters.indices) {
                val endMs = if (i < chapters.size - 1) chapters[i+1].startTimeMs else totalDurationMs
                chapters[i] = chapters[i].copy(index = i, endTimeMs = endMs)
            }
        }
        return chapters
    }

    private fun parseChaptersElement(reader: EbmlReader, size: Long, totalDurationMs: Long, outList: MutableList<VideoChapter>) {
        val chaptersEnd = reader.position + size
        var index = 0
        while (reader.position < chaptersEnd) {
            val id = reader.readElementId() ?: break
            val elSize = reader.readElementSize() ?: break
            if (id == 0x45B9L) { // EditionEntry
                val editionEnd = reader.position + elSize
                while (reader.position < editionEnd) {
                    val childId = reader.readElementId() ?: break
                    val childSize = reader.readElementSize() ?: break
                    if (childId == 0xB6L) { // ChapterAtom
                        val atomEnd = reader.position + childSize
                        var timeStart = 0L
                        var title = "Chapter ${index + 1}"
                        
                        while (reader.position < atomEnd) {
                            val leafId = reader.readElementId() ?: break
                            val leafSize = reader.readElementSize() ?: break
                            when (leafId) {
                                0x91L -> timeStart = reader.readUint(leafSize) / 1000000L
                                0x80L -> { // ChapterDisplay
                                    val displayEnd = reader.position + leafSize
                                    while (reader.position < displayEnd) {
                                        val displayChildId = reader.readElementId() ?: break
                                        val displayChildSize = reader.readElementSize() ?: break
                                        if (displayChildId == 0x85L) { // ChapterString
                                            title = reader.readString(displayChildSize)
                                        } else {
                                            reader.skip(displayChildSize)
                                        }
                                    }
                                }
                                else -> reader.skip(leafSize)
                            }
                        }
                        outList.add(VideoChapter(index, title, timeStart, totalDurationMs))
                        index++
                    } else {
                        reader.skip(childSize)
                    }
                }
            } else {
                reader.skip(elSize)
            }
        }
    }

    private class EbmlReader(val stream: FileInputStream) {
        val channel = stream.channel
        var position = channel.position()

        fun skip(bytes: Long) {
            position += bytes
            channel.position(position)
        }

        fun readVint(): Long? {
            val firstByte = stream.read()
            if (firstByte == -1) return null
            position++
            var length = 1
            var mask = 0x80
            while ((firstByte and mask) == 0) {
                mask = mask shr 1
                length++
                if (length > 8) return null
            }
            var result = (firstByte and mask.inv()).toLong()
            for (i in 1 until length) {
                val b = stream.read()
                if (b == -1) return null
                position++
                result = (result shl 8) or b.toLong()
            }
            return result
        }

        fun readElementId(): Long? {
            val firstByte = stream.read()
            if (firstByte == -1) return null
            position++
            var length = 1
            var mask = 0x80
            while ((firstByte and mask) == 0) {
                mask = mask shr 1
                length++
                if (length > 8) return null
            }
            var result = firstByte.toLong()
            for (i in 1 until length) {
                val b = stream.read()
                if (b == -1) return null
                position++
                result = (result shl 8) or b.toLong()
            }
            return result
        }

        fun readElementSize(): Long? = readVint()

        fun readUint(size: Long): Long {
            if (size > 8) { skip(size); return 0L }
            var result = 0L
            for (i in 0 until size) {
                val b = stream.read()
                if (b == -1) throw EOFException()
                position++
                result = (result shl 8) or b.toLong()
            }
            return result
        }

        fun readString(size: Long): String {
            if (size > 1024 * 1024) { skip(size); return "" }
            val buffer = ByteArray(size.toInt())
            var read = 0
            while (read < size) {
                val count = stream.read(buffer, read, size.toInt() - read)
                if (count == -1) throw EOFException()
                read += count
            }
            position += size
            var len = size.toInt()
            while (len > 0 && buffer[len - 1].toInt() == 0) len--
            return String(buffer, 0, len, Charsets.UTF_8)
        }
    }
}
