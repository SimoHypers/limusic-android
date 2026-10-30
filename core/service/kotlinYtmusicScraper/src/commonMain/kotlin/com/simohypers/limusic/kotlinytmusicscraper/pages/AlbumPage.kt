package com.simohypers.limusic.kotlinytmusicscraper.pages

import com.simohypers.limusic.kotlinytmusicscraper.models.Album
import com.simohypers.limusic.kotlinytmusicscraper.models.AlbumItem
import com.simohypers.limusic.kotlinytmusicscraper.models.Artist
import com.simohypers.limusic.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import com.simohypers.limusic.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import com.simohypers.limusic.kotlinytmusicscraper.models.SongItem
import com.simohypers.limusic.kotlinytmusicscraper.models.Thumbnail
import com.simohypers.limusic.kotlinytmusicscraper.models.Thumbnails
import com.simohypers.limusic.kotlinytmusicscraper.models.oddElements
import com.simohypers.limusic.kotlinytmusicscraper.models.splitBySeparator
import com.simohypers.limusic.kotlinytmusicscraper.utils.parseTime

data class AlbumPage(
    val album: AlbumItem,
    val songs: List<SongItem>,
    val description: String?,
    val thumbnails: Thumbnails?,
    val duration: String?,
    val otherVersion: List<AlbumItem>,
    /** Saved to the signed-in account's library; null when the page did not say. */
    val inLibrary: Boolean? = null,
    /** The header's first subtitle run: "Album", "Single", "EP", in YouTube's language. */
    val kind: String? = null,
    /** The artist's round picture from the header's strapline. */
    val artistThumbnail: String? = null,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer?): AlbumItem? {
            if (renderer == null) {
                return null
            }
            return AlbumItem(
                browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                playlistId =
                    renderer.playlistId ?: return null,
                title =
                    renderer.title.runs
                        ?.firstOrNull()
                        ?.text ?: return null,
                artists =
                    renderer.subtitle
                        ?.runs
                        ?.lastOrNull()
                        ?.let {
                            Artist(
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                name = it.text,
                            )
                        }?.let { listOf(it) } ?: emptyList(),
                year = null,
                isSingle = false,
                thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: "",
                explicit =
                    renderer.subtitleBadges?.find {
                        it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                    } != null,
            )
        }

        fun fromMusicResponsiveListItemRenderer(
            renderer: MusicResponsiveListItemRenderer?,
            album: AlbumItem,
        ): SongItem? {
            if (renderer == null) {
                return null
            } else {
                return SongItem(
                    id = renderer.videoId ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    artists =
                        renderer.flexColumns
                            .getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            // The column reads "Artist • Album • 13M plays"; only the first group
                            // is artists, so everything after the first " • " is dropped.
                            ?.splitBySeparator()
                            ?.firstOrNull()
                            ?.oddElements()
                            ?.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            } ?: album.artists ?: emptyList(),
                    album =
                        Album(
                            name = album.title,
                            id = album.id,
                        ),
                    duration =
                        renderer.fixedColumns
                            ?.firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text
                            ?.parseTime() ?: return null,
                    thumbnail = album.thumbnail,
                    thumbnails =
                        Thumbnails(
                            thumbnails =
                                listOf(
                                    Thumbnail(
                                        url = album.thumbnail,
                                        width = 544,
                                        height = 544,
                                    ),
                                ),
                        ),
                    explicit =
                        renderer.badges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null,
                    musicVideoType = renderer.musicVideoType,
                )
            }
        }
    }
}