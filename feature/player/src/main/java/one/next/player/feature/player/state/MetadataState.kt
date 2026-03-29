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
    val startTimeMs: Long,
    val endTimeMs: Long
) {
    // Ye function time ko "00:00 - 05:30" format mein convert karega
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
        coroutineScope {
            updateMetadata()
            updateChapters()
            
            // Background loop jo check karega ki video kis chapter me hai
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
                    updateChapters()
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
        
        val duration = player.duration
        val safeDuration = if (duration > 0 && duration != androidx.media3.common.C.TIME_UNSET) duration else 0L

        // ExoPlayer agar internally chapters (MP4 etc) nikalta hai toh
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
                } else {
                    safeDuration
                }
                
                val chapterTitle = "Chapter ${chapterIndex + 1}"
                newChapters.add(VideoChapter(chapterIndex, chapterTitle, startTimeMs, endTimeMs))
            }
        } 
        // Dummy Chapters for UI testing (Jise hum next step me Real MKV parser se replace karenge)
        else {
            if (safeDuration > 0) {
                val numChapters = 6
                val chapterDuration = safeDuration / numChapters
                
                val titles = listOf("Intro", "Opening Theme", "Episode Part A", "Episode Part B", "Ending Theme", "Next Episode Preview")
                for (i in 0 until numChapters) {
                    val start = i * chapterDuration
                    val end = if (i == numChapters - 1) safeDuration else (i + 1) * chapterDuration
                    newChapters.add(VideoChapter(i, titles[i], start, end))
                }
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
        // Check current position in which chapter range
        currentChapter = chapters.lastOrNull { currentPosition >= it.startTimeMs } ?: chapters.first()
    }
}
