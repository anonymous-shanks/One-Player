package one.next.player.feature.player

import one.next.player.core.model.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerActivityPlaylistTest {

    @Test
    fun buildPlaybackPlaylist_matchesFileSourceAgainstFolderPlaylist() {
        val playlistVideos = listOf(
            video(
                uriString = "file:///storage/emulated/0/OnePlayerNomedia/nomedia-a.mp4",
                path = "/storage/emulated/0/OnePlayerNomedia/nomedia-a.mp4",
            ),
            video(
                uriString = "file:///storage/emulated/0/OnePlayerNomedia/nomedia-b.mp4",
                path = "/storage/emulated/0/OnePlayerNomedia/nomedia-b.mp4",
            ),
            video(
                uriString = "file:///storage/emulated/0/OnePlayerNomedia/nomedia-c.mp4",
                path = "/storage/emulated/0/OnePlayerNomedia/nomedia-c.mp4",
            ),
        )

        val result = buildPlaybackPlaylist(
            playlistVideos = playlistVideos,
            sourceUriString = "file:///storage/emulated/0/OnePlayerNomedia/nomedia-a.mp4",
            playbackUriString = "file:///storage/emulated/0/OnePlayerNomedia/nomedia-a.mp4",
            currentPath = "/storage/emulated/0/OnePlayerNomedia/nomedia-a.mp4",
        )

        assertEquals(3, result.items.size)
        assertEquals(0, result.currentIndex)
        assertEquals(playlistVideos.map(Video::uriString), result.items)
    }

    @Test
    fun buildPlaybackPlaylist_prependsCurrentItemWhenFolderPlaylistMissesIt() {
        val playlistVideos = listOf(
            video(
                uriString = "file:///storage/emulated/0/OnePlayerNomedia/nomedia-b.mp4",
                path = "/storage/emulated/0/OnePlayerNomedia/nomedia-b.mp4",
            ),
        )

        val result = buildPlaybackPlaylist(
            playlistVideos = playlistVideos,
            sourceUriString = "file:///storage/emulated/0/OnePlayerNomedia/nomedia-a.mp4",
            playbackUriString = "file:///storage/emulated/0/OnePlayerNomedia/nomedia-a.mp4",
            currentPath = "/storage/emulated/0/OnePlayerNomedia/nomedia-a.mp4",
        )

        assertEquals(
            listOf(
                "file:///storage/emulated/0/OnePlayerNomedia/nomedia-a.mp4",
                "file:///storage/emulated/0/OnePlayerNomedia/nomedia-b.mp4",
            ),
            result.items,
        )
        assertEquals(0, result.currentIndex)
    }

    @Test
    fun buildPlaybackPlaylist_matchesCurrentPathWhenPlaylistUsesContentUris() {
        val playlistVideos = listOf(
            video(
                uriString = "content://media/external/video/media/101",
                path = "/storage/emulated/0/Movies/visible-a.mp4",
            ),
            video(
                uriString = "content://media/external/video/media/102",
                path = "/storage/emulated/0/Movies/visible-b.mp4",
            ),
        )

        val result = buildPlaybackPlaylist(
            playlistVideos = playlistVideos,
            sourceUriString = "file:///storage/emulated/0/Movies/visible-a.mp4",
            playbackUriString = "file:///storage/emulated/0/Movies/visible-a.mp4",
            currentPath = "/storage/emulated/0/Movies/visible-a.mp4",
        )

        assertEquals(2, result.items.size)
        assertEquals(0, result.currentIndex)
        assertEquals(playlistVideos.map(Video::uriString), result.items)
    }

    private fun video(
        uriString: String,
        path: String,
    ) = Video(
        id = 1,
        path = path,
        parentPath = "/storage/emulated/0/OnePlayerNomedia",
        duration = 60_000,
        uriString = uriString,
        nameWithExtension = path.substringAfterLast('/'),
        width = 1920,
        height = 1080,
        size = 1024,
    )
}
