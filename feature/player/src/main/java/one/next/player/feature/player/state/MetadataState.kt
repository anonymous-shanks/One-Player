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

    suspend fun observe() {
        updateMetadata()
        updateChapters()
        
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
        val period = Timeline.Period()
        
        // ExoPlayer exposes chapters as periods inside the window
        val firstPeriod = window.firstPeriodIndex
        val lastPeriod = window.lastPeriodIndex
        
        // Agar 1 se zyada periods hain, iska matlab chapters available hain
        if (lastPeriod > firstPeriod) {
            for (i in firstPeriod..lastPeriod) {
                timeline.getPeriod(i, period)
                val chapterIndex = i - firstPeriod
                val startTimeMs = period.positionInWindowMs
                
                // Standard naming fallback if native title isn't exposed perfectly
                val chapterTitle = "Chapter ${chapterIndex + 1}"
                newChapters.add(VideoChapter(chapterIndex, chapterTitle, startTimeMs))
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
        
        val currentPeriodIndex = player.currentPeriodIndex
        val window = Timeline.Window()
        
        if (!player.currentTimeline.isEmpty && player.currentMediaItemIndex < player.currentTimeline.windowCount) {
            player.currentTimeline.getWindow(player.currentMediaItemIndex, window)
            val chapterIndex = currentPeriodIndex - window.firstPeriodIndex
            currentChapter = chapters.find { it.index == chapterIndex }
        }
    }
}
