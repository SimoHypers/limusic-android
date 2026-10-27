package com.maxrave.data.repository

import com.maxrave.data.parser.toListThumbnail
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import com.maxrave.kotlinytmusicscraper.parser.LibraryPage
import com.maxrave.logger.Logger

internal const val LIBRARY_ALBUMS = "FEmusic_liked_albums"
internal const val LIBRARY_ARTISTS = "FEmusic_library_corpus_track_artists"
internal const val LIBRARY_SUBSCRIPTIONS = "FEmusic_library_corpus_artists"
internal const val LIBRARY_SONGS = "FEmusic_liked_videos"

/**
 * A library browse paged to the end, like desktop's `library_grid`: YouTube hands these out a page
 * at a time, so one browse would cut anyone's bigger library short. Null when the first page
 * fails; a later failure keeps what loaded.
 */
internal suspend fun YouTube.wholeLibrary(browseId: String): LibraryPage? {
    val first =
        libraryPage(browseId)
            .onFailure { Logger.w("LibraryBrowse", "$browseId failed: ${it.message}") }
            .getOrNull() ?: return null
    val cards = first.cards.toMutableList()
    val rows = first.rows.toMutableList()
    var token = first.continuation
    // ponytail: a page cap, so a token that never resolves can't spin forever. Raise it if a
    // library past ~500 entries turns up.
    for (page in 0 until 20) {
        val next = libraryPage(browseId, token ?: break).getOrNull() ?: break
        // Some grids carry a token that resolves to nothing.
        if (next.cards.isEmpty() && next.rows.isEmpty()) break
        cards += next.cards
        rows += next.rows
        token = next.continuation
    }
    return LibraryPage(cards, rows, null)
}

internal fun MusicTwoRowItemRenderer.toLibraryAlbum(): AlbumsResult? {
    val browseId = navigationEndpoint.browseEndpoint?.browseId?.takeIf { it.startsWith("MPRE") } ?: return null
    // "Album • Artist • 2020": the linked runs are the artists, a trailing four digits the year.
    val runs = subtitle?.runs.orEmpty()
    return AlbumsResult(
        artists =
            runs
                .filter { it.navigationEndpoint?.browseEndpoint != null }
                .map { Artist(id = it.navigationEndpoint?.browseEndpoint?.browseId, name = it.text) },
        browseId = browseId,
        category = "",
        duration = "",
        isExplicit = subtitleBadges?.any { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } == true,
        resultType = "Album",
        thumbnails = thumbnailRenderer.musicThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail().orEmpty(),
        title = title.runs?.firstOrNull()?.text ?: return null,
        type = runs.firstOrNull()?.text.orEmpty(),
        year = runs.lastOrNull()?.text?.takeIf { it.length == 4 && it.all(Char::isDigit) }.orEmpty(),
    )
}

/**
 * The channel id an artist entry links. Library artists (not subscriptions) link `MPLA` + it, the
 * "artist, filtered to your library" page, so the prefix goes, as desktop does.
 */
private fun String.artistChannelId() = removePrefix("MPLA").takeIf { it.startsWith("UC") }

/** An artist row, with YouTube's own subtitle ("12 songs", "1.2M subscribers") in [ArtistsResult.category]. */
internal fun MusicResponsiveListItemRenderer.toLibraryArtist(): ArtistsResult? {
    val browseId = navigationEndpoint?.browseEndpoint?.browseId?.artistChannelId() ?: return null
    fun column(i: Int) =
        flexColumns
            .getOrNull(i)
            ?.musicResponsiveListItemFlexColumnRenderer
            ?.text
            ?.runs
            ?.joinToString("") { it.text }
    return ArtistsResult(
        artist = column(0) ?: return null,
        browseId = browseId,
        category = column(1).orEmpty(),
        radioId = "",
        resultType = "Artist",
        shuffleId = "",
        thumbnails = thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail().orEmpty(),
    )
}

/** The same artist as a grid card, should YouTube send the grid view. */
internal fun MusicTwoRowItemRenderer.toLibraryArtist(): ArtistsResult? {
    val browseId = navigationEndpoint.browseEndpoint?.browseId?.artistChannelId() ?: return null
    return ArtistsResult(
        artist = title.runs?.firstOrNull()?.text ?: return null,
        browseId = browseId,
        category = subtitle?.runs?.joinToString("") { it.text }.orEmpty(),
        radioId = "",
        resultType = "Artist",
        shuffleId = "",
        thumbnails = thumbnailRenderer.musicThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail().orEmpty(),
    )
}
