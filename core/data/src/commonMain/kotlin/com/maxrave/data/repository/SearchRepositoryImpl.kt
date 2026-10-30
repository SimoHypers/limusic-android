package com.maxrave.data.repository

import com.maxrave.data.db.datasource.LocalDataSource
import com.maxrave.data.mapping.toDomainSearchSuggestions
import com.maxrave.data.parser.parsePodcast
import com.maxrave.data.parser.search.parseSearchAlbum
import com.maxrave.data.parser.search.parseSearchArtist
import com.maxrave.data.parser.search.parseSearchPlaylist
import com.maxrave.data.parser.search.parseSearchSong
import com.maxrave.data.parser.search.parseSearchVideo
import com.maxrave.domain.data.entities.SearchHistory
import com.maxrave.domain.data.model.searchResult.SearchAllResult
import com.maxrave.domain.data.model.searchResult.SearchSuggestions
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.ArtistItem
import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.YTItem
import com.maxrave.kotlinytmusicscraper.pages.SearchResult
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

internal class SearchRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : SearchRepository {
    override fun getSearchHistory(): Flow<List<SearchHistory>> =
        flow {
            emit(localDataSource.getSearchHistory())
        }.flowOn(Dispatchers.IO)

    override fun insertSearchHistory(searchHistory: SearchHistory): Flow<Long> =
        flow {
            emit(localDataSource.insertSearchHistory(searchHistory))
        }.flowOn(Dispatchers.IO)

    override suspend fun deleteSearchHistory() =
        withContext(Dispatchers.IO) {
            localDataSource.deleteSearchHistory()
        }

    override suspend fun deleteSearchQuery(query: String) =
        withContext(Dispatchers.IO) {
            localDataSource.deleteSearchQuery(query)
        }

    // One page per search, like desktop: SimpMusic fetched two more pages behind every one, and
    // every request leaves from the owner's IP.
    private fun <T> searchPage(
        query: String,
        filter: YouTube.SearchFilter,
        parse: (SearchResult) -> ArrayList<T>,
    ): Flow<Resource<ArrayList<T>>> =
        flow {
            runCatching { parse(youTube.search(query, filter).getOrThrow()) }
                .onSuccess { emit(Resource.Success(it)) }
                .onFailure { e ->
                    Logger.d("Search", "Error: ${e.message}")
                    emit(Resource.Error(e.message.toString()))
                }
        }.flowOn(Dispatchers.IO)

    override fun getSearchDataSong(query: String) = searchPage(query, YouTube.SearchFilter.FILTER_SONG, ::parseSearchSong)

    override fun getSearchDataVideo(query: String) = searchPage(query, YouTube.SearchFilter.FILTER_VIDEO, ::parseSearchVideo)

    override fun getSearchDataPodcast(query: String) = searchPage(query, YouTube.SearchFilter.FILTER_PODCAST) { parsePodcast(it.listPodcast) }

    override fun getSearchDataArtist(query: String) = searchPage(query, YouTube.SearchFilter.FILTER_ARTIST, ::parseSearchArtist)

    override fun getSearchDataAlbum(query: String) = searchPage(query, YouTube.SearchFilter.FILTER_ALBUM, ::parseSearchAlbum)

    override fun getSearchDataPlaylist(query: String) = searchPage(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST, ::parseSearchPlaylist)

    override fun getSearchDataAll(query: String): Flow<Resource<SearchAllResult>> =
        flow {
            runCatching {
                val page = youTube.searchAll(query).getOrThrow()
                fun of(items: List<YTItem>) = SearchResult(items, emptyList())
                val top =
                    when (val item = page.top) {
                        // A video top result plays like a song; toTrack keeps its video type.
                        is SongItem -> parseSearchSong(of(listOf(item))).firstOrNull()
                        is AlbumItem -> parseSearchAlbum(of(listOf(item))).firstOrNull()
                        is ArtistItem -> parseSearchArtist(of(listOf(item))).firstOrNull()
                        else -> null
                    }
                SearchAllResult(
                    top = top,
                    topSubtitle = page.topSubtitle.orEmpty(),
                    topSongs = parseSearchSong(of(page.topSongs)),
                    albums = parseSearchAlbum(of(page.items.filterIsInstance<AlbumItem>())),
                    artists = parseSearchArtist(of(page.items.filterIsInstance<ArtistItem>())),
                    playlists = parseSearchPlaylist(of(page.items.filterIsInstance<PlaylistItem>())),
                )
            }.onSuccess { emit(Resource.Success(it)) }
                .onFailure { e ->
                    Logger.d("Search", "Error: ${e.message}")
                    emit(Resource.Error(e.message.toString()))
                }
        }.flowOn(Dispatchers.IO)

    override fun getSuggestQuery(query: String): Flow<Resource<SearchSuggestions>> =
        flow {
            runCatching {
                youTube
                    .getYTMusicSearchSuggestions(query)
                    .onSuccess {
                        emit(Resource.Success(it.toDomainSearchSuggestions()))
                    }.onFailure { e ->
                        Logger.d("Suggest", "Error: ${e.message}")
                        emit(Resource.Error<SearchSuggestions>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)
}