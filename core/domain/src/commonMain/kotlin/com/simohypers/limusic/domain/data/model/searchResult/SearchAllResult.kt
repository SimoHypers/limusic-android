package com.simohypers.limusic.domain.data.model.searchResult

import com.simohypers.limusic.domain.data.model.searchResult.albums.AlbumsResult
import com.simohypers.limusic.domain.data.model.searchResult.artists.ArtistsResult
import com.simohypers.limusic.domain.data.model.searchResult.playlists.PlaylistsResult
import com.simohypers.limusic.domain.data.model.searchResult.songs.SongsResult
import com.simohypers.limusic.domain.data.type.SearchResultType

/**
 * An unfiltered search like desktop's searchAll: YouTube's top result with its own line
 * ("Artist • 130M monthly audience") and up to three songs, plus the albums, artists and
 * playlists among the rows. Songs and videos come from their own filtered searches.
 */
data class SearchAllResult(
    val top: SearchResultType?,
    val topSubtitle: String,
    val topSongs: List<SongsResult>,
    val albums: List<AlbumsResult>,
    val artists: List<ArtistsResult>,
    val playlists: List<PlaylistsResult>,
)
