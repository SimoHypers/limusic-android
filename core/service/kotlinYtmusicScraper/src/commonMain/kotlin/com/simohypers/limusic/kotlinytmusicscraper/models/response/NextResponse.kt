package com.simohypers.limusic.kotlinytmusicscraper.models.response

import com.simohypers.limusic.kotlinytmusicscraper.models.NavigationEndpoint
import com.simohypers.limusic.kotlinytmusicscraper.models.PlaylistPanelRenderer
import com.simohypers.limusic.kotlinytmusicscraper.models.Tabs
import com.simohypers.limusic.kotlinytmusicscraper.models.youtube.data.YouTubeDataPage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NextResponse(
    val contents: Contents,
    val continuationContents: ContinuationContents?,
    val currentVideoEndpoint: NavigationEndpoint?,
) {
    @Serializable
    data class Contents(
        val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer?,
        val twoColumnWatchNextResults: YouTubeDataPage.Contents.TwoColumnWatchNextResults?,
    ) {
        @Serializable
        data class SingleColumnMusicWatchNextResultsRenderer(
            val tabbedRenderer: TabbedRenderer,
        ) {
            @Serializable
            data class TabbedRenderer(
                val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer,
            ) {
                @Serializable
                data class WatchNextTabbedResultsRenderer(
                    val tabs: List<Tabs.Tab>,
                )
            }
        }
    }

    @Serializable
    data class ContinuationContents(
        val playlistPanelContinuation: PlaylistPanelRenderer?,
        val sectionListContinuation: BrowseResponse.ContinuationContents.SectionListContinuation?,
        val musicPlaylistShelfContinuation: BrowseResponse.ContinuationContents.MusicPlaylistShelfContinuation?,
    )
}

@Serializable
data class NextAndroidMusicResponse(
    /** Kept as JSON: the like is read from anywhere inside, as desktop does, not one fixed path. */
    val playerOverlays: JsonElement? = null,
    /** The queue, for the playing track's own row and the other version it carries. */
    val contents: NextResponse.Contents? = null,
)
