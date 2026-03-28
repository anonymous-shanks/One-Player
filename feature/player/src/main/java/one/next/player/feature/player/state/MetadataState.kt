package one.next.player.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.listen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class VideoChapter(
    val index: Int,
    val title: String,
    val startTimeMs: Long
)

@Composable
fun rememberMetadataState(player: Player): MetadataState {
    val metadataState = remember { MetadataState(player) }
    LaunchedEffect(player) { metadataState.observe() }
    return metadataState
}

@Stable
class MetadataState(private val player: Player) {
    var title: String? by mutableStateOf(null)
        private set

    var chapters: List<VideoChapter> by mutableStateOf(emptyList())
        private set

    var currentChapter: VideoChapter? by mutableStateOf(null)
        private set

    suspend fun observe() = coroutineScope {
        updateMetadata()
        updateChapters()
        
        // Background loop to auto-update current chapter during smooth playback 
        // (so it updates even without seeking)
        launch {
            while (true) {
                delay(1000)
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
                updateChapters()
            }
            if (events.containsAny(Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_TIMELINE_CHANGED)) {
                updateCurrentChapter()
            }
        }
    }

    private fun updateMetadata() {
        title = player.mediaMetadata.title?.toString()
    }

    private fun updateChapters() {
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

        val newChapters = mutableListOf<VideoChapter>()
        val firstPeriod = window.firstPeriodIndex
        val lastPeriod = window.lastPeriodIndex
        
        // Try native ExoPlayer chapters (works for MP4s, but fails for most MKVs)
        if (lastPeriod > firstPeriod) {
            val period = Timeline.Period()
            for (i in firstPeriod..lastPeriod) {
                timeline.getPeriod(i, period)
                val chapterIndex = i - firstPeriod
                val startTimeMs = period.positionInWindowMs
                val chapterTitle = "Chapter ${chapterIndex + 1}"
                newChapters.add(VideoChapter(chapterIndex, chapterTitle, startTimeMs))
            }
        } 
        // IF native chapters are not found, inject DUMMY DEMO CHAPTERS to test the UI
        else {
            val duration = player.duration
            if (duration > 0 && duration != androidx.media3.common.C.TIME_UNSET) {
                val numChapters = 6
                val chapterDuration = duration / numChapters
                
                newChapters.add(VideoChapter(0, "Intro (Dummy Demo)", 0))
                newChapters.add(VideoChapter(1, "Opening Theme", chapterDuration * 1))
                newChapters.add(VideoChapter(2, "Episode Part A", chapterDuration * 2))
                newChapters.add(VideoChapter(3, "Episode Part B", chapterDuration * 3))
                newChapters.add(VideoChapter(4, "Ending Theme", chapterDuration * 4))
                newChapters.add(VideoChapter(5, "Next Episode Preview", chapterDuration * 5))
            }
        }

        chapters = newChapters
        updateCurrentChapter()
    }

    private fun updateCurrentChapter() {
        if (chapters.isEmpty()) {
            currentChapter = null
            return
        }
        val currentPosition = player.currentPosition
        // Match the current time with the correct chapter
        currentChapter = chapters.lastOrNull { it.startTimeMs <= currentPosition } ?: chapters.first()
    }
}
