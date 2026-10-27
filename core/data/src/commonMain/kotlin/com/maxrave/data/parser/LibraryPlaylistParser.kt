package com.maxrave.data.parser

import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.kotlinytmusicscraper.models.GridRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicTwoRowItemRenderer

internal fun parseLibraryPlaylist(input: List<GridRenderer.Item>): List<PlaylistsResult> {
    val list: MutableList<PlaylistsResult> = mutableListOf()
    if (input.isNotEmpty()) {
        for (i in input.indices) {
            input[i].musicTwoRowItemRenderer?.let {
                if (it.navigationEndpoint?.browseEndpoint?.browseId != null) {
                    list.add(
                        PlaylistsResult(
                            author =
                                it.subtitle
                                    ?.runs
                                    ?.get(0)
                                    ?.text ?: "",
                            browseId = it.navigationEndpoint?.browseEndpoint?.browseId ?: "",
                            category = "",
                            itemCount = it.songCount(),
                            resultType = "",
                            thumbnails =
                                it.thumbnailRenderer
                                    ?.musicThumbnailRenderer
                                    ?.thumbnail
                                    ?.thumbnails
                                    ?.toListThumbnail() ?: listOf(),
                            title =
                                it.title
                                    ?.runs
                                    ?.get(0)
                                    ?.text ?: "",
                        ),
                    )
                }
            }
        }
    }
    return list
}

internal fun parseNextLibraryPlaylist(input: List<MusicTwoRowItemRenderer>): List<PlaylistsResult> =
    input.map {
        PlaylistsResult(
            author =
                it.subtitle
                    ?.runs
                    ?.get(0)
                    ?.text ?: "",
            browseId = it.navigationEndpoint?.browseEndpoint?.browseId ?: "",
            category = "",
            itemCount = it.songCount(),
            resultType = "",
            thumbnails =
                it.thumbnailRenderer
                    ?.musicThumbnailRenderer
                    ?.thumbnail
                    ?.thumbnails
                    ?.toListThumbnail() ?: listOf(),
            title =
                it.title
                    ?.runs
                    ?.get(0)
                    ?.text ?: "",
        )
    }
// The card's subtitle is "Author • 12 tracks"; a list YouTube fills itself has no count.
private fun MusicTwoRowItemRenderer.songCount(): String =
    subtitle
        ?.runs
        ?.lastOrNull()
        ?.text
        ?.takeIf { text -> text.any(Char::isDigit) }
        .orEmpty()
