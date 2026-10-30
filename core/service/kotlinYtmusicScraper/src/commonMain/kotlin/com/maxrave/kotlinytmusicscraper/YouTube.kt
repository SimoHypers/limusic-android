package com.maxrave.kotlinytmusicscraper

import com.eygraber.uri.toKmpUri
import com.maxrave.common.ITAG
import com.maxrave.domain.utils.PlaylistSort
import com.maxrave.kotlinytmusicscraper.YouTube.Companion.DEFAULT_VISITOR_DATA
import com.maxrave.kotlinytmusicscraper.extractor.ExtractSource
import com.maxrave.kotlinytmusicscraper.cipher.FaradayCipherEngine
import com.maxrave.kotlinytmusicscraper.extractor.audioTrackRank
import com.maxrave.kotlinytmusicscraper.extractor.nChallengeOf
import com.maxrave.kotlinytmusicscraper.extractor.parseSignatureCipher
import com.maxrave.kotlinytmusicscraper.extractor.signedUrl
import com.maxrave.kotlinytmusicscraper.extractor.withN
import com.maxrave.kotlinytmusicscraper.models.AccountInfo
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.Artist
import com.maxrave.kotlinytmusicscraper.models.ArtistItem
import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint
import com.maxrave.kotlinytmusicscraper.models.GridRenderer
import com.maxrave.kotlinytmusicscraper.models.MediaType
import com.maxrave.kotlinytmusicscraper.models.MusicCarouselShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.Run
import com.maxrave.kotlinytmusicscraper.models.savedToLibrary
import com.maxrave.kotlinytmusicscraper.parser.LibraryPage
import com.maxrave.kotlinytmusicscraper.parser.parseLibraryPage
import com.maxrave.kotlinytmusicscraper.models.SearchSuggestions
import com.maxrave.kotlinytmusicscraper.models.SectionListRenderer
import com.maxrave.kotlinytmusicscraper.models.SongInfo
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.VideoItem
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.models.YTItem
import com.maxrave.kotlinytmusicscraper.models.YTItemType
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.WEB
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.WEB_REMIX
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
import com.maxrave.kotlinytmusicscraper.models.getContinuation
import com.maxrave.kotlinytmusicscraper.models.oddElements
import com.maxrave.kotlinytmusicscraper.models.response.AccountMenuResponse
import com.maxrave.kotlinytmusicscraper.models.response.AccountSwitcherEndpointResponse
import com.maxrave.kotlinytmusicscraper.models.response.AddItemYouTubePlaylistResponse
import com.maxrave.kotlinytmusicscraper.models.response.BrowseResponse
import com.maxrave.kotlinytmusicscraper.models.response.CreatePlaylistResponse
import com.maxrave.kotlinytmusicscraper.models.response.ImageUploadResponse
import com.maxrave.kotlinytmusicscraper.models.response.DownloadProgress
import com.maxrave.kotlinytmusicscraper.models.response.GetQueueResponse
import com.maxrave.kotlinytmusicscraper.models.response.GetSearchSuggestionsResponse
import com.maxrave.kotlinytmusicscraper.models.response.LikeStatus
import com.maxrave.kotlinytmusicscraper.models.response.NextAndroidMusicResponse
import com.maxrave.kotlinytmusicscraper.models.response.NextResponse
import com.maxrave.kotlinytmusicscraper.models.response.PlayerResponse
import com.maxrave.kotlinytmusicscraper.models.response.SearchResponse
import com.maxrave.kotlinytmusicscraper.models.response.toLikeStatus
import com.maxrave.kotlinytmusicscraper.models.response.toListAccountInfo
import com.maxrave.kotlinytmusicscraper.models.youtube.YouTubeInitialPage
import com.maxrave.kotlinytmusicscraper.pages.AlbumPage
import com.maxrave.kotlinytmusicscraper.pages.ArtistPage
import com.maxrave.kotlinytmusicscraper.pages.ArtistSection
import com.maxrave.kotlinytmusicscraper.pages.SearchAllPage
import com.maxrave.kotlinytmusicscraper.pages.toSearchAllPage
import com.maxrave.kotlinytmusicscraper.pages.BrowseResult
import com.maxrave.kotlinytmusicscraper.pages.ExplorePage
import com.maxrave.kotlinytmusicscraper.pages.MoodAndGenres
import com.maxrave.kotlinytmusicscraper.pages.NextPage
import com.maxrave.kotlinytmusicscraper.pages.NextResult
import com.maxrave.kotlinytmusicscraper.pages.PlaylistContinuationPage
import com.maxrave.kotlinytmusicscraper.pages.PlaylistPage
import com.maxrave.kotlinytmusicscraper.pages.RelatedPage
import com.maxrave.kotlinytmusicscraper.pages.SearchPage
import com.maxrave.kotlinytmusicscraper.pages.SearchResult
import com.maxrave.kotlinytmusicscraper.pages.SearchSuggestionPage
import com.maxrave.kotlinytmusicscraper.parser.fromPlaylistContinuationToTrackWithSetVideoId
import com.maxrave.kotlinytmusicscraper.parser.fromPlaylistContinuationToTracks
import com.maxrave.kotlinytmusicscraper.parser.fromPlaylistToTrack
import com.maxrave.kotlinytmusicscraper.parser.fromPlaylistToTrackWithSetVideoId
import com.maxrave.kotlinytmusicscraper.parser.getContinuePlaylistContinuation
import com.maxrave.kotlinytmusicscraper.parser.getPlaylistContinuation
import com.maxrave.kotlinytmusicscraper.parser.editAction
import com.maxrave.kotlinytmusicscraper.parser.getReloadParams
import com.maxrave.kotlinytmusicscraper.parser.getSuggestionSongItems
import com.maxrave.kotlinytmusicscraper.parser.hasReloadParams
import com.maxrave.logger.Logger
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.encodeURLParameter
import io.ktor.http.parseQueryString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Path
import kotlin.jvm.JvmInline
import kotlin.math.round
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.time.TimeSource

private const val TAG = "YouTubeScraper"

/**
 * WEB_REMIX as desktop Limusic sends it to /player: with its own numeric id (67) in the header. The
 * shared definition says 1, which every other request has always sent.
 */
private val STREAM_WEB_REMIX = WEB_REMIX.copy(xClientName = 67)

/** How long a stream that failed in the player keeps WEB_REMIX away from its video (desktop's TTL). */
private val WEB_REMIX_BAR = 30.minutes

/**
 * Special thanks to [z-huang/InnerTune](https://github.com/z-huang/InnerTune)
 * This library is from [z-huang/InnerTune] and I just modified it to comply with SimpMusic
 *
 * Here is the object that can create all request to YouTube Music in SimpMusic
 * Using YouTube Internal API
 * @author maxrave-dev
 */

class YouTube {
    private val ytMusic = Ytmusic()

    var cookiePath: Path?
        get() = ytMusic.cookiePath
        set(value) {
            ytMusic.cookiePath = value
        }

    /**
     * Set the locale and language for YouTube Music
     */
    var locale: YouTubeLocale
        get() = ytMusic.locale
        set(value) {
            ytMusic.locale = value
        }

    /**
     * Set custom visitorData for client (default is @see [DEFAULT_VISITOR_DATA])
     */
    var visitorData: String?
        get() = ytMusic.visitorData
        set(value) {
            ytMusic.visitorData = value
        }

    var dataSyncId: String?
        get() = ytMusic.dataSyncId
        set(value) {
            ytMusic.dataSyncId = value
        }

    /**
     * Set cookie and authentication header for client (for log in option)
     */
    var cookie: String?
        get() = ytMusic.cookie
        set(value) {
            ytMusic.cookie = value
        }

    var pageId: String?
        get() = ytMusic.pageId
        set(value) {
            ytMusic.pageId = value
        }

    var authUser: Int
        get() = ytMusic.authUser
        set(value) {
            ytMusic.authUser = value
        }

    /**
     * Remove proxy for client
     */
    fun removeProxy() {
        ytMusic.proxy = null
    }

    /**
     * Set the proxy for client
     */
    fun setProxy(
        isHttp: Boolean,
        host: String,
        port: Int,
    ) {
        runCatching {
            if (isHttp) ProxyBuilder.http("$host:$port") else ProxyBuilder.socks(host, port)
        }.onSuccess {
            ytMusic.proxy = it
        }.onFailure {
            it.printStackTrace()
        }
    }

    /**
     * Search for a song, album, artist, playlist, etc.
     * @param query the search query
     * @param filter the search filter (see in [SearchFilter])
     * @return a [Result]<[SearchResult]> object
     */
    suspend fun search(
        query: String,
        filter: SearchFilter,
    ): Result<SearchResult> =
        runCatching {
            val response = ytMusic.search(WEB_REMIX, query, filter.value).body<SearchResponse>()
            SearchResult(
                items =
                    response.contents
                        ?.tabbedSearchResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                        }.orEmpty(),
                listPodcast =
                    response.contents
                        ?.tabbedSearchResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            SearchPage.toPodcast(it.musicResponsiveListItemRenderer)
                        }.orEmpty(),
                continuation =
                    response.contents
                        ?.tabbedSearchResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicShelfRenderer
                        ?.continuations
                        ?.getContinuation(),
            )
        }

    /** The unfiltered search: its top result and every row, see [SearchAllPage]. */
    suspend fun searchAll(query: String): Result<SearchAllPage> =
        runCatching {
            ytMusic.search(WEB_REMIX, query).body<SearchResponse>().toSearchAllPage()
        }

    /**
     * Every search request response a limited data. Use this function to get the next data
     * @param continuation continuation token from [SearchResult.continuation]
     * @return a [Result]<[SearchResult]> object
     */
    suspend fun searchContinuation(continuation: String): Result<SearchResult> =
        runCatching {
            val response = ytMusic.search(WEB_REMIX, continuation = continuation).body<SearchResponse>()
            SearchResult(
                items =
                    response.continuationContents
                        ?.musicShelfContinuation
                        ?.contents
                        ?.mapNotNull {
                            SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                        }!!,
                listPodcast =
                    response.continuationContents.musicShelfContinuation.contents
                        .mapNotNull {
                            SearchPage.toPodcast(it.musicResponsiveListItemRenderer)
                        }.orEmpty(),
                continuation =
                    response.continuationContents.musicShelfContinuation.continuations
                        ?.getContinuation(),
            )
        }

    /**
     * Get the album page data from YouTube Music
     * @param browseId the album browseId
     * @param withSongs if true, the function will get the songs data too
     * @return a [Result]<[AlbumPage]> object
     */
    suspend fun album(
        browseId: String,
        withSongs: Boolean = true,
    ): Result<AlbumPage> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId).body<BrowseResponse>()
            val playlistId =
                response.microformat
                    ?.microformatDataRenderer
                    ?.urlCanonical
                    ?.substringAfterLast('=')!!
            val albumItem =
                AlbumItem(
                    browseId = browseId,
                    playlistId = playlistId,
                    title =
                        response.contents
                            ?.twoColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.title
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: "",
                    artists =
                        response.contents
                            ?.twoColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.straplineTextOne
                            ?.runs
                            ?.oddElements()
                            ?.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            }!!,
                    year =
                        response.contents.twoColumnBrowseResultsRenderer.tabs
                            .firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.subtitle
                            ?.runs
                            ?.lastOrNull()
                            ?.text
                            ?.toIntOrNull(),
                    thumbnail =
                        response.contents.twoColumnBrowseResultsRenderer.tabs
                            .firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.thumbnail
                            ?.musicThumbnailRenderer
                            ?.getThumbnailUrl()!!,
                )
            val header =
                response.contents.twoColumnBrowseResultsRenderer.tabs
                    .firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull()
                    ?.musicResponsiveHeaderRenderer
            AlbumPage(
                album = albumItem,
                kind = header?.subtitle?.runs?.firstOrNull()?.text,
                artistThumbnail = header?.straplineThumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                songs =
                    if (withSongs) {
                        albumSongs(
                            response.contents
                                .twoColumnBrowseResultsRenderer
                                .secondaryContents
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicShelfRenderer
                                ?.contents,
                            albumItem,
                        ).getOrThrow()
                    } else {
                        emptyList()
                    },
                description =
                    getDescription(
                        response.contents.twoColumnBrowseResultsRenderer.tabs
                            .firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.description
                            ?.musicDescriptionShelfRenderer
                            ?.description
                            ?.runs,
                    ),
                duration =
                    response.contents.twoColumnBrowseResultsRenderer.tabs
                        .firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicResponsiveHeaderRenderer
                        ?.secondSubtitle
                        ?.runs
                        ?.get(2)
                        ?.text ?: "",
                thumbnails =
                    response.contents.twoColumnBrowseResultsRenderer.tabs
                        .firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicResponsiveHeaderRenderer
                        ?.thumbnail
                        ?.musicThumbnailRenderer
                        ?.thumbnail,
                otherVersion =
                    response.contents
                        .twoColumnBrowseResultsRenderer
                        .secondaryContents
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicCarouselShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            AlbumPage.fromMusicTwoRowItemRenderer(
                                it.musicTwoRowItemRenderer,
                            )
                        } ?: emptyList(),
                inLibrary =
                    response.contents.twoColumnBrowseResultsRenderer.tabs
                        .firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicResponsiveHeaderRenderer
                        ?.buttons
                        .savedToLibrary(),
            )
        }

    /**
     * Joins every run of a description. A link run contributes its `urlEndpoint` URL rather than
     * its text, which YouTube shortens for display (`…/wiki/Ariana_...`) — the description view
     * makes URLs clickable, and the shortened form would open the wrong page.
     */
    private fun getDescription(runs: List<Run>?): String {
        var description = ""
        if (!runs.isNullOrEmpty()) {
            for (run in runs) {
                description += run.navigationEndpoint?.urlEndpoint?.url ?: run.text
            }
        }
        Logger.d("description", description)
        return description
    }

    private fun albumSongs(
        content: List<MusicShelfRenderer.Content>?,
        album: AlbumItem,
    ): Result<List<SongItem>> =
        runCatching {
            if (content == null) {
                return@runCatching emptyList()
            } else {
                return@runCatching content.mapNotNull {
                    AlbumPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer, album)
                }
            }
        }

    suspend fun testArtist(browseId: String): Result<ArrayList<ArtistSection>> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId).body<BrowseResponse>()
            val artistSections = arrayListOf<ArtistSection>()
            val content =
                response.contents
                    ?.singleColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
            if (content != null) {
                for (i in 0 until content.size) {
                    ArtistPage
                        .fromSectionListRendererContent(content.get(i))
                        ?.let { artistSections.add(it) }
                    Logger.d(TAG, "Section $i checking \n artistSection ${artistSections.lastOrNull()}")
                }
            }
            return@runCatching artistSections
        }

    /**
     * Get the artist page data from YouTube Music
     * @param browseId the artist browseId
     * @return a [Result]<[ArtistPage]> object
     */
    suspend fun artist(browseId: String): Result<ArtistPage> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId).body<BrowseResponse>()
            ArtistPage(
                artist =
                    ArtistItem(
                        id = browseId,
                        title =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                ?: response.header
                                    ?.musicVisualHeaderRenderer
                                    ?.title
                                    ?.runs
                                    ?.firstOrNull()
                                    ?.text!!,
                        thumbnail =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.thumbnail
                                ?.musicThumbnailRenderer
                                ?.getThumbnailUrl()
                                ?: response.header
                                    ?.musicVisualHeaderRenderer
                                    ?.foregroundThumbnail
                                    ?.musicThumbnailRenderer
                                    ?.getThumbnailUrl()!!,
                        shuffleEndpoint =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.playButton
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.watchEndpoint,
                        radioEndpoint =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.startRadioButton
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.watchEndpoint,
                    ),
                sections =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.mapNotNull(ArtistPage::fromSectionListRendererContent)!!,
                // Every run, not just the first: YouTube splits the text at each link, so the first
                // run alone stops at "From Wikipedia (".
                description =
                    response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.description
                        ?.runs
                        ?.let(::getDescription),
                subscribed =
                    response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.subscriptionButton
                        ?.subscribeButtonRenderer
                        ?.subscribed,
                subscribeChannelId =
                    response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.subscriptionButton
                        ?.subscribeButtonRenderer
                        ?.channelId,
                subscribers =
                    response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.subscriptionButton
                        ?.subscribeButtonRenderer
                        ?.longSubscriberCountText
                        ?.runs
                        ?.get(
                            0,
                        )?.text,
                view =
                    response.contents.singleColumnBrowseResultsRenderer.tabs[0]
                        .tabRenderer.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicDescriptionShelfRenderer
                        ?.subheader
                        ?.runs
                        ?.firstOrNull()
                        ?.text,
            )
        }

    suspend fun getYouTubePlaylistFullTracksWithSetVideoId(playlistId: String): Result<List<Pair<SongItem, String>>> =
        runCatching {
            val plId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
            // SongItem / SetVideoId
            val listPair = mutableListOf<Pair<SongItem, String>>()
            val response = ytMusic.playlist(plId).body<BrowseResponse>()
            listPair.addAll(
                response.fromPlaylistToTrackWithSetVideoId(),
            )
            var continuation = response.getPlaylistContinuation()
            // A token seen twice would loop forever.
            val seenContinuations = mutableSetOf<String>()
            while (continuation != null && seenContinuations.add(continuation)) {
                // A page that fails ends the walk but keeps the pages already read, rather than
                // failing the whole result.
                val continuationResponse =
                    runCatching {
                        ytMusic
                            .browse(
                                client = WEB_REMIX,
                                setLogin = true,
                                params = null,
                                continuation = continuation,
                            ).body<BrowseResponse>()
                    }.getOrNull() ?: break
                // Continuation pages carry their rows in a different shape from the first page:
                // read with the first-page parser they yielded nothing, so every track past the
                // first page (100) was missing.
                listPair.addAll(
                    continuationResponse.fromPlaylistContinuationToTrackWithSetVideoId(),
                )
                continuation = continuationResponse.getPlaylistContinuation()
            }

            return@runCatching listPair
        }

    suspend fun getSuggestionsTrackForPlaylist(playlistId: String): Result<Pair<String?, List<SongItem>?>?> =
        runCatching {
            val initialResponse =
                ytMusic
                    .playlist(
                        if (playlistId.startsWith("VL")) playlistId else "VL$playlistId",
                    ).body<BrowseResponse>()
            var continuation = initialResponse.getPlaylistContinuation()
            Logger.d(TAG, "YouTube: getSuggestionsTrackForPlaylist: $continuation")
            while (continuation != null) {
                val continuationResponse =
                    ytMusic
                        .browse(
                            client = WEB_REMIX,
                            setLogin = true,
                            params = "wAEB",
                            continuation = continuation,
                        ).body<BrowseResponse>()
                Logger.d(TAG, "YouTube: getSuggestionsTrackForPlaylist: ${continuationResponse.getReloadParams()}")
                if (continuationResponse.hasReloadParams()) {
                    return@runCatching Pair(continuationResponse.getReloadParams(), continuationResponse.getSuggestionSongItems())
                } else {
                    continuation = continuationResponse.getContinuePlaylistContinuation()
                }
            }
            return@runCatching null
        }

    suspend fun getPlaylistFullTracks(playlistId: String): Result<List<SongItem>> =
        runCatching {
            val songs = mutableListOf<SongItem>()
            val response = ytMusic.playlist(playlistId).body<BrowseResponse>()
            songs.addAll(
                response.fromPlaylistToTrack(),
            )
            var continuation = response.getPlaylistContinuation()
            while (continuation != null) {
                val continuationResponse =
                    ytMusic
                        .browse(
                            client = WEB_REMIX,
                            setLogin = true,
                            params = null,
                            continuation = continuation,
                        ).body<BrowseResponse>()
                songs.addAll(
                    continuationResponse.fromPlaylistContinuationToTracks(),
                )
                continuation = continuationResponse.getContinuePlaylistContinuation()
            }
            return@runCatching songs
        }

    /**
     * Get the playlist page data from YouTube Music
     * @param playlistId the playlistId
     * @return a [Result]<[PlaylistPage]> object
     */
    suspend fun playlist(playlistId: String): Result<PlaylistPage> =
        runCatching {
            val response =
                ytMusic
                    .browse(
                        client = WEB_REMIX,
                        browseId = "VL$playlistId",
                        setLogin = true,
                    ).body<BrowseResponse>()
            val header =
                response.header?.musicDetailHeaderRenderer
                    ?: response.header
                        ?.musicEditablePlaylistDetailHeaderRenderer
                        ?.header
                        ?.musicDetailHeaderRenderer!!
            PlaylistPage(
                playlist =
                    PlaylistItem(
                        id = playlistId,
                        title =
                            header.title.runs
                                ?.firstOrNull()
                                ?.text!!,
                        author =
                            header.subtitle.runs?.getOrNull(2)?.let {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            },
                        songCountText =
                            header.secondSubtitle.runs
                                ?.firstOrNull()
                                ?.text,
                        thumbnail = header.thumbnail.croppedSquareThumbnailRenderer?.getThumbnailUrl()!!,
                        playEndpoint = null,
                        shuffleEndpoint =
                            header.menu.menuRenderer.topLevelButtons
                                ?.firstOrNull()
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint!!,
                        radioEndpoint =
                            header.menu.menuRenderer.items
                                .find {
                                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                                }?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint!!,
                    ),
                songs =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicPlaylistShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            PlaylistPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
                        }!!,
                songsContinuation =
                    response.contents.singleColumnBrowseResultsRenderer.tabs
                        .firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicPlaylistShelfRenderer
                        ?.continuations
                        ?.getContinuation(),
                continuation =
                    response.contents.singleColumnBrowseResultsRenderer.tabs
                        .firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.continuations
                        ?.getContinuation(),
            )
        }

    suspend fun playlistContinuation(continuation: String) =
        runCatching {
            val response =
                ytMusic
                    .browse(
                        client = WEB_REMIX,
                        continuation = continuation,
                        setLogin = true,
                    ).body<BrowseResponse>()
            PlaylistContinuationPage(
                songs =
                    response.continuationContents?.musicPlaylistShelfContinuation?.contents?.mapNotNull {
                        PlaylistPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
                    }!!,
                continuation =
                    response.continuationContents.musicPlaylistShelfContinuation.continuations
                        ?.getContinuation(),
            )
        }

    /**
     * Execute a custom POST request to YouTube Music
     * In SimpMusic, I use this function to parsing Home, Playlist, Album data instead using [album], [playlist], [artist] function
     * @param browseId the browseId (such as "FEmusic_home", "VL$playlistId", etc.)
     * @param params the params
     * @param continuation the continuation token
     * @param country the country code
     * @param setLogin if true, the function will set the cookie and authentication header
     * @return a [Result]<[BrowseResponse]> object
     */
    suspend fun customQuery(
        browseId: String?,
        params: String? = null,
        continuation: String? = null,
        country: String? = null,
        setLogin: Boolean = true,
    ) = runCatching {
        ytMusic.browse(WEB_REMIX, browseId, params, continuation, country, setLogin).body<BrowseResponse>()
    }

    /**
     * Get the related data of a song from YouTube Music
     * @param videoId the videoId of song
     * @return a [Result]<[NextResponse]> object
     */
    suspend fun nextCustom(videoId: String) =
        runCatching {
            ytMusic.nextCustom(WEB_REMIX, videoId).body<NextResponse>()
        }

    suspend fun newRelease(): Result<ExplorePage> =
        runCatching {
            val response =
                ytMusic.browse(WEB_REMIX, browseId = "FEmusic_new_releases").body<BrowseResponse>()
            ExplorePage(
                released =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicCarouselShelfRenderer
                        ?.contents
                        ?.mapNotNull { it.musicTwoRowItemRenderer }
                        ?.mapNotNull(RelatedPage::fromMusicTwoRowItemRenderer)
                        .orEmpty()
                        .mapNotNull {
                            if (it.type == YTItemType.ALBUM) it as? AlbumItem else null
                        },
                musicVideo =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicCarouselShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            it.musicTwoRowItemRenderer
                        }?.mapNotNull(
                            ArtistPage::fromMusicTwoRowItemRenderer,
                        ).orEmpty()
                        .mapNotNull {
                            if (it.type == YTItemType.VIDEO) it as? VideoItem else null
                        },
                // The same shelf released is read from above.
                releasedMoreEndpoint =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicCarouselShelfRenderer
                        ?.header
                        ?.musicCarouselShelfBasicHeaderRenderer
                        ?.moreContentButton
                        ?.buttonRenderer
                        ?.navigationEndpoint
                        ?.browseEndpoint,
                // The same shelf musicVideo is read from above.
                musicVideoMoreEndpoint =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicCarouselShelfRenderer
                        ?.header
                        ?.musicCarouselShelfBasicHeaderRenderer
                        ?.moreContentButton
                        ?.buttonRenderer
                        ?.navigationEndpoint
                        ?.browseEndpoint,
            )
        }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId = "FEmusic_moods_and_genres").body<BrowseResponse>()
            response.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents!!
                .mapNotNull(MoodAndGenres.Companion::fromSectionListRendererContent)
        }

    suspend fun browse(
        browseId: String,
        params: String?,
    ): Result<BrowseResult> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId = browseId, params = params).body<BrowseResponse>()
            BrowseResult(
                title =
                    response.header
                        ?.musicHeaderRenderer
                        ?.title
                        ?.runs
                        ?.firstOrNull()
                        ?.text,
                items =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.mapNotNull(::browseSection)
                        .orEmpty(),
            )
        }

    // The shelf kinds Metrolist's YouTube.browse() reads — grid, carousel (immersive carousels land
    // here too, via @JsonNames), music shelf and playlist shelf — plus the mood buttons of the
    // FEmusic_moods_and_genres page. Anything else carries nothing a "More" page shows.
    private fun browseSection(content: SectionListRenderer.Content): BrowseResult.Item? =
        when {
            content.gridRenderer != null -> {
                BrowseResult.Item(
                    title =
                        content.gridRenderer.header
                            ?.gridHeaderRenderer
                            ?.title
                            ?.runs
                            ?.firstOrNull()
                            ?.text,
                    items =
                        content.gridRenderer.items
                            .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                            .mapNotNull(RelatedPage.Companion::fromMusicTwoRowItemRenderer),
                    moods =
                        content.gridRenderer.items
                            .mapNotNull(GridRenderer.Item::musicNavigationButtonRenderer)
                            .mapNotNull(MoodAndGenres.Companion::fromMusicNavigationButtonRenderer),
                )
            }

            content.musicCarouselShelfRenderer != null -> {
                BrowseResult.Item(
                    title =
                        content.musicCarouselShelfRenderer.header
                            ?.musicCarouselShelfBasicHeaderRenderer
                            ?.title
                            ?.runs
                            ?.firstOrNull()
                            ?.text,
                    items =
                        content.musicCarouselShelfRenderer.contents.mapNotNull { item ->
                            item.musicTwoRowItemRenderer?.let(RelatedPage.Companion::fromMusicTwoRowItemRenderer)
                                ?: item.musicResponsiveListItemRenderer?.let(::browseListItem)
                                ?: item.musicMultiRowListItemRenderer?.let(::browseEpisode)
                        },
                    moods =
                        content.musicCarouselShelfRenderer.contents
                            .mapNotNull(MusicCarouselShelfRenderer.Content::musicNavigationButtonRenderer)
                            .mapNotNull(MoodAndGenres.Companion::fromMusicNavigationButtonRenderer),
                )
            }

            content.musicShelfRenderer != null -> {
                BrowseResult.Item(
                    title =
                        content.musicShelfRenderer.title
                            ?.runs
                            ?.firstOrNull()
                            ?.text,
                    items =
                        content.musicShelfRenderer.contents.orEmpty().mapNotNull { item ->
                            item.musicResponsiveListItemRenderer?.let(::browseListItem)
                                ?: item.musicMultiRowListItemRenderer?.let(::browseEpisode)
                        },
                )
            }

            content.musicPlaylistShelfRenderer != null -> {
                BrowseResult.Item(
                    title = null,
                    items =
                        content.musicPlaylistShelfRenderer.contents.orEmpty().mapNotNull { item ->
                            item.musicResponsiveListItemRenderer?.let(::browseListItem)
                        },
                )
            }

            else -> {
                null
            }
        }

    // Metrolist tries a lenient library parser and then RelatedPage's. The search parser reads every
    // item kind but gives up on a row missing its menu or play button; RelatedPage's catches those
    // rows when they are plain tracks.
    private fun browseListItem(renderer: MusicResponsiveListItemRenderer): YTItem? =
        SearchPage.toYTItem(renderer) ?: RelatedPage.fromMusicResponsiveListItemRenderer(renderer)

    // A podcast episode, played like a track — the same fields HomeParser reads for one.
    private fun browseEpisode(row: MusicShelfRenderer.Content.MusicMultiRowListItemRenderer): YTItem? =
        SongItem(
            id = row.onTap?.watchEndpoint?.videoId ?: return null,
            title =
                row.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text ?: return null,
            artists = emptyList(),
            thumbnail = row.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
        )

    suspend fun getFullMetadata(videoId: String): Result<YouTubeInitialPage> =
        runCatching {
            val ytScrape = ytMusic.scrapeYouTube(videoId).body<String>()
            var response = ""
            val ksoupHtmlParser =
                KsoupHtmlParser(
                    object : KsoupHtmlHandler {
                        override fun onText(text: String) {
                            super.onText(text)
                            if (text.contains("var ytInitialPlayerResponse")) {
                                val temp = text.replace("var ytInitialPlayerResponse = ", "").dropLast(1)
                                Logger.d("Scrape", "Temp $temp")
                                response = temp.trimIndent()
                            }
                        }
                    },
                )
            ksoupHtmlParser.write(ytScrape)
            ksoupHtmlParser.end()
            val json = Json { ignoreUnknownKeys = true }
            return@runCatching json.decodeFromString<YouTubeInitialPage>(response)
        }

    /** Null when the response carries no like button, which says nothing about the like. */
    suspend fun getLikedInfo(videoId: String): Result<LikeStatus?> =
        runCatching {
            val response =
                ytMusic
                    .next(
                        WEB_REMIX,
                        videoId,
                        null,
                        null,
                        null,
                        null,
                        null,
                    ).body<NextAndroidMusicResponse>()
            val likeStatus =
                response.playerOverlays
                    ?.playerOverlayRenderer
                    ?.actions
                    ?.find { it.likeButtonRenderer != null }
                    ?.likeButtonRenderer
                    ?.likeStatus
                    ?.toLikeStatus()
            Logger.d("YouTube", "Like status: $likeStatus")
            return@runCatching likeStatus
        }

    suspend fun getSongInfo(videoId: String): Result<SongInfo> =
        runCatching {
            // Called with "" before anything has played; that used to cost a /next request.
            require(videoId.isNotBlank()) { "No video to describe" }
            val ytNext = ytMusic.next(WEB, videoId, null, null, null, null, null).body<NextResponse>()
            val videoSecondary =
                ytNext.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoSecondaryInfoRenderer != null
                    }?.videoSecondaryInfoRenderer
            val videoPrimary =
                ytNext.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoPrimaryInfoRenderer != null
                    }?.videoPrimaryInfoRenderer
            return@runCatching SongInfo(
                videoId = videoId,
                author =
                    videoSecondary?.owner?.videoOwnerRenderer?.title?.runs?.firstOrNull()?.text?.replace(
                        Regex(" - Topic| - Chủ đề|"),
                        "",
                    ),
                authorId =
                    videoSecondary
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.navigationEndpoint
                        ?.browseEndpoint
                        ?.browseId,
                authorThumbnail =
                    videoSecondary
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.thumbnail
                        ?.thumbnails
                        ?.find {
                            it.height == 48
                        }?.url
                        ?.replace("s48", "s960"),
                description = videoSecondary?.attributedDescription?.content,
                subscribers =
                    videoSecondary
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.subscriberCountText
                        ?.simpleText,
                uploadDate = videoPrimary?.dateText?.simpleText,
                // "1,234,567 views" from YouTube's own watch page data.
                viewCount =
                    videoPrimary
                        ?.viewCount
                        ?.videoViewCountRenderer
                        ?.viewCount
                        ?.simpleText
                        ?.filter(Char::isDigit)
                        ?.toIntOrNull(),
            )
            // Get author thumbnails, subscribers, description, like count
        }

    fun isManifestUrl(url: String): Boolean = url.contains(".m3u8") || url.contains(".mpd") || url.contains("manifest")

    /**
     * The HLS playlist a live broadcast plays from — see [Ytmusic.liveStreamPlayer] for which
     * client asks for it and why. Read as plain JSON: only one field is needed, and that client's
     * response is not guaranteed to fit [PlayerResponse]'s non-null fields.
     */
    private suspend fun liveStreamHlsUrl(videoId: String): String? =
        runCatching {
            // A fresh visitor id every time, fetched without the session (sw.js_data sends no
            // cookie), so this cookie-less request stays anonymous end to end — see
            // Ytmusic.liveStreamPlayer.
            val anonymousVisitorData =
                visitorData() ?: run {
                    Logger.w(TAG, "No anonymous visitorData for the live stream request of $videoId")
                    return@runCatching null
                }
            val response = ytMusic.liveStreamPlayer(videoId, anonymousVisitorData).body<JsonObject>()
            val status =
                response["playabilityStatus"]
                    ?.jsonObject
                    ?.get("status")
                    ?.jsonPrimitive
                    ?.contentOrNull
            if (status != "OK") {
                Logger.w(TAG, "Live stream request for $videoId: $status")
                return@runCatching null
            }
            response["streamingData"]
                ?.jsonObject
                ?.get("hlsManifestUrl")
                ?.jsonPrimitive
                ?.contentOrNull
        }.onFailure {
            Logger.w(TAG, "Live stream request failed for $videoId: ${it.message}")
        }.getOrNull()
            ?.also { Logger.d(TAG, "live HLS from the Android client for $videoId") }

    /**
     * This response reduced to what a live broadcast plays from: its HLS playlist and nothing else.
     * The fixed-length formats are dropped so nothing downstream picks one by mistake, and the status
     * reads OK because the playlist came from an extractor even when this client was refused.
     */
    private fun PlayerResponse.asLiveStream(liveHlsUrl: String): PlayerResponse =
        copy(
            playabilityStatus = playabilityStatus.copy(status = "OK", reason = null),
            streamingData =
                (
                    streamingData ?: PlayerResponse.StreamingData(
                        hlsManifestUrl = null,
                        formats = null,
                        adaptiveFormats = emptyList(),
                        expiresInSeconds = 0,
                    )
                ).copy(
                    hlsManifestUrl = liveHlsUrl,
                    formats = emptyList(),
                    adaptiveFormats = emptyList(),
                ),
        )

    /**
     * Resolves running right now, by video id. A music video's audio and its picture are asked for
     * separately and at the same moment, and each used to pay for the whole chain.
     */
    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val playerLock = Mutex()
    private val playerJobs = mutableMapOf<String, Deferred<Triple<String?, PlayerResponse, MediaType>>>()

    /** Video id to when a stream of it failed in the player; see [markStreamFailed]. */
    private val webRemixFailedAt = mutableMapOf<String, TimeSource.Monotonic.ValueTimeMark>()

    /**
     * A playable stream for [videoId], found the way desktop Limusic's orchestrator finds one:
     * WEB_REMIX, then VISIONOS, then TVHTML5_SIMPLY. A client that refuses hands over to the next,
     * and a client's audio is used only once its last bytes come back ([Ytmusic.servesToTheEnd]).
     *
     * The response carries the winning client's formats, each with a ready-to-play URL, and
     * WEB_REMIX's metadata and loudness whichever client won.
     */
    suspend fun player(
        videoId: String,
        playlistId: String? = null,
        noLogIn: Boolean = false,
        preferredAudioLanguage: String? = null,
    ): Result<Triple<String?, PlayerResponse, MediaType>> =
        runCatching {
            val job =
                playerLock.withLock {
                    playerJobs[videoId] ?: playerScope
                        .async { resolveStream(videoId, playlistId, preferredAudioLanguage) }
                        .also { job ->
                            playerJobs[videoId] = job
                            job.invokeOnCompletion {
                                playerScope.launch {
                                    playerLock.withLock { if (playerJobs[videoId] === job) playerJobs.remove(videoId) }
                                }
                            }
                        }
                }
            job.await()
        }

    /**
     * A stream [player] handed out failed in the player anyway. The next resolve of [videoId] skips
     * WEB_REMIX for half an hour (desktop's mark_web_remix_failed): it passed the probe and died
     * regardless, so asking it again only repeats the failure. Marking it whichever client served is
     * harmless, because the clients behind WEB_REMIX only serve when it did not.
     */
    suspend fun markStreamFailed(videoId: String) {
        playerLock.withLock {
            webRemixFailedAt.entries.removeAll { it.value.elapsedNow() >= WEB_REMIX_BAR }
            webRemixFailedAt[videoId] = TimeSource.Monotonic.markNow()
        }
    }

    private suspend fun webRemixBarred(videoId: String) =
        playerLock.withLock { webRemixFailedAt[videoId]?.let { it.elapsedNow() < WEB_REMIX_BAR } == true }

    private suspend fun resolveStream(
        videoId: String,
        playlistId: String?,
        preferredAudioLanguage: String?,
    ): Triple<String?, PlayerResponse, MediaType> {
        val cpn =
            (1..16)
                .map {
                    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"[
                        Random.nextInt(
                            0,
                            64,
                        ),
                    ]
                }.joinToString("")

        // A refusal says something about the track; silence says something about the network, and
        // only silence is worth retrying.
        var answered = false
        var lastError: Throwable? = null

        suspend fun playerOrNull(
            client: YouTubeClient,
            request: suspend () -> PlayerResponse,
        ): PlayerResponse? =
            try {
                request().also { answered = true }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Too many requests: the next client would only add to the pile.
                if (e.isRateLimit) throw e
                if (e is ClientRequestException || e is SerializationException) answered = true
                Logger.w(TAG, "${client.clientName} /player failed for $videoId: ${e.message?.substringBefore('\n')}")
                lastError = e
                null
            }

        // The player YouTube serves now and its signature timestamp. Null when either is missing,
        // which costs WEB_REMIX its ciphered formats and nothing else: VISIONOS needs neither.
        val playerInfo = ytMusic.cipher.playerInfo()
        // Without these googlevideo refuses WEB_REMIX's streams before their end (measured
        // 2026-10-26, signed in). Null leaves WEB_REMIX to fail its probe and VISIONOS to answer.
        val poTokens = ytMusic.webPoTokens(videoId)
        val main =
            playerOrNull(STREAM_WEB_REMIX) {
                ytMusic
                    .player(
                        STREAM_WEB_REMIX,
                        videoId,
                        playlistId,
                        cpn,
                        signatureTimestamp = playerInfo?.signatureTimestamp ?: approximateSignatureTimestamp(),
                        poToken = poTokens?.session,
                    ).body<PlayerResponse>()
                    .withFexpTracking()
            }

        // A live broadcast has no fixed-length formats to decode: it plays from one HLS
        // playlist. videoDetails is present even on a response this client was refused, so the
        // check comes before anything that requires an OK status.
        if (main?.videoDetails?.isLive == true) {
            // main carries an hlsManifestUrl of its own, and it is deliberately not used: the
            // web clients' live playlists load, but every segment is refused with 403.
            val liveHlsUrl =
                liveStreamHlsUrl(videoId)
                    ?: ytMusic.getLiveHlsUrl(videoId)
                    ?: throw RuntimeException("No live HLS URL found for $videoId")
            return Triple(cpn, main.asLiveStream(liveHlsUrl), MediaType.Video)
        }

        val refusals = mutableListOf<PlayerResponse.PlayabilityStatus>()
        var streamRefused = false
        for (client in listOf(STREAM_WEB_REMIX, YouTubeClient.VISIONOS, YouTubeClient.TVHTML5_SIMPLY)) {
            val response =
                when (client) {
                    STREAM_WEB_REMIX -> {
                        if (webRemixBarred(videoId)) {
                            Logger.i(TAG, "WEB_REMIX failed in the player for $videoId lately, skipping it")
                            continue
                        }
                        main
                    }
                    YouTubeClient.VISIONOS ->
                        playerOrNull(client) {
                            ytMusic.player(client, videoId, playlistId, cpn = null, setLogin = false).body<PlayerResponse>()
                        }
                    // TVHTML5_SIMPLY: without a token it is a guaranteed refusal, so it costs nothing then.
                    else -> {
                        val session = poTokens?.session ?: continue
                        playerOrNull(client) {
                            ytMusic
                                .player(
                                    client,
                                    videoId,
                                    playlistId,
                                    cpn = null,
                                    signatureTimestamp = playerInfo?.signatureTimestamp ?: approximateSignatureTimestamp(),
                                    setLogin = false,
                                    poToken = session,
                                ).body<PlayerResponse>()
                        }
                    }
                } ?: continue
            // Every client here but VISIONOS is a web one: ciphered, `n`-bound and PoToken-bound.
            val web = client != YouTubeClient.VISIONOS
            val status = response.playabilityStatus
            if (status.status != "OK") {
                Logger.w(TAG, "${client.clientName} refused $videoId: ${status.status} ${status.reason}")
                refusals += status
                continue
            }
            val streams =
                response.streamingData?.let {
                    readyStreams(it, web, playerInfo, poTokens?.video, preferredAudioLanguage)
                }
            val audio = streams?.adaptiveFormats?.filter { it.isAudio }?.maxByOrNull { it.bitrate }
            if (streams == null || audio == null) {
                Logger.w(TAG, "${client.clientName} gave $videoId no playable audio")
                continue
            }
            // ponytail: probes the best audio, not the itag the quality setting will pick. Every URL
            // of one response shares its signature, n and client, which is what a cap follows.
            val withPot = web && poTokens != null
            if (!ytMusic.servesToTheEnd(audio.url!!, audio.contentLength)) {
                Logger.w(TAG, "${client.clientName} stream for $videoId refused before its end (itag ${audio.itag}, PoToken $withPot)")
                streamRefused = true
                continue
            }
            ExtractSource.record(videoId, client.clientName + if (withPot) " · PoToken" else "")
            Logger.i(TAG, "$videoId: ${client.clientName} plays to the end (itag ${audio.itag}, PoToken $withPot)")
            val merged =
                response.copy(
                    streamingData = streams,
                    videoDetails = main?.videoDetails ?: response.videoDetails,
                    playerConfig = main?.playerConfig ?: response.playerConfig,
                    // ponytail: the watch-history pings go out as WEB_REMIX, so only WEB_REMIX's own
                    // tracking may be used; a VISIONOS play goes unregistered. Desktop pings as the
                    // client that issued the block (its issue #83) if that is ever wanted.
                    playbackTracking = main?.takeIf { it.playabilityStatus.status == "OK" }?.playbackTracking,
                )
            val firstThumb =
                merged.videoDetails
                    ?.thumbnail
                    ?.thumbnails
                    ?.firstOrNull()
            val mediaType = if (firstThumb != null && firstThumb.height == firstThumb.width) MediaType.Song else MediaType.Video
            return Triple(cpn, merged, mediaType)
        }

        if (!answered) throw lastError ?: RuntimeException("No URL found")
        // A bot check is about the IP and outranks everything. Otherwise the last refusal, unless a
        // client did answer and only its stream failed: then "not available" would be untrue.
        val refusal =
            refusals.firstOrNull { it.reason?.contains("not a bot", ignoreCase = true) == true }
                ?: refusals.lastOrNull()?.takeUnless { streamRefused }
        throw PlayerRefusedException(refusal?.status ?: "UNPLAYABLE", refusal?.reason ?: "No stream that plays to the end")
    }

    /**
     * [streaming] with a ready-to-play URL on every format that can have one, the preferred audio
     * language first within each itag. A [web] client's formats are deciphered on the device, get
     * their `n` solved (desktop's NEEDS_N_TRANSFORM) and carry [pot] when there is one; a direct
     * client's are used as they come. Null when a web client's challenges cannot be asked for at all.
     */
    private suspend fun readyStreams(
        streaming: PlayerResponse.StreamingData,
        web: Boolean,
        playerInfo: FaradayCipherEngine.PlayerInfo?,
        pot: String?,
        preferredAudioLanguage: String?,
    ): PlayerResponse.StreamingData? {
        val all = streaming.adaptiveFormats + streaming.formats.orEmpty()
        val ciphers =
            if (web) {
                all.associateWith { format -> format.signatureCipher?.takeIf { format.url == null }?.let(::parseSignatureCipher) }
            } else {
                emptyMap()
            }
        val solved =
            if (web) {
                val info = playerInfo ?: return null
                ytMusic.cipher.decode(
                    info.playerId,
                    ciphers.values.mapNotNull { it?.s },
                    all.mapNotNull { (it.url ?: ciphers[it]?.url)?.let(::nChallengeOf) },
                )
            } else {
                FaradayCipherEngine.DecodeResult(emptyMap(), emptyMap())
            }
        fun ready(format: PlayerResponse.StreamingData.Format): PlayerResponse.StreamingData.Format? {
            val cipher = ciphers[format]
            val url = format.url ?: cipher?.let { c -> solved.signatures[c.s]?.let(c::signedUrl) } ?: return null
            // Desktop leaves an unsolved `n` in place: the probe, not this, decides whether it plays.
            if (!web) return format.copy(url = url, signatureCipher = null)
            val signed = nChallengeOf(url)?.let { solved.nParameters[it] }?.let { withN(url, it) } ?: url
            return format.copy(url = if (pot == null) signed else "$signed&pot=${pot.encodeURLParameter()}", signatureCipher = null)
        }

        fun List<PlayerResponse.StreamingData.Format>.ready() =
            mapNotNull { ready(it) }.sortedBy { audioTrackRank(it.url!!, preferredAudioLanguage) }

        return streaming.copy(formats = streaming.formats?.ready(), adaptiveFormats = streaming.adaptiveFormats.ready())
    }

    /** What WEB_REMIX's /player wants when the player's own timestamp is unknown: roughly its build day. */
    @OptIn(ExperimentalTime::class)
    private fun approximateSignatureTimestamp(): Int =
        Instant
            .fromEpochSeconds(0)
            .toLocalDateTime(TimeZone.UTC)
            .date
            .daysUntil(Clock.System.todayIn(TimeZone.UTC))

    /** The tracking URLs with the `fexp` experiment ids of the stream URL, as the web player sends them. */
    private fun PlayerResponse.withFexpTracking(): PlayerResponse {
        val fexp =
            streamingData
                ?.serverAbrStreamingUrl
                ?.toKmpUri()
                ?.getQueryParameter("fexp")
                ?: return this
        fun String?.withFexp() =
            this
                ?.toKmpUri()
                ?.buildUpon()
                ?.appendQueryParameter("fexp", fexp)
                ?.build()
                ?.toString()
        val tracking = playbackTracking ?: return this
        return copy(
            playbackTracking =
                tracking.copy(
                    atrUrl = tracking.atrUrl?.copy(baseUrl = tracking.atrUrl.baseUrl.withFexp()),
                    videostatsPlaybackUrl =
                        tracking.videostatsPlaybackUrl?.copy(baseUrl = tracking.videostatsPlaybackUrl.baseUrl.withFexp()),
                    videostatsWatchtimeUrl =
                        tracking.videostatsWatchtimeUrl?.copy(baseUrl = tracking.videostatsWatchtimeUrl.baseUrl.withFexp()),
                ),
        )
    }

    suspend fun updateWatchTime(
        watchtimeUrl: String,
        watchtimeList: ArrayList<Float>,
        cpn: String,
        playlistId: String?,
    ): Result<Int> =
        runCatching {
            val et = watchtimeList.takeLast(2).joinToString(",")
            val watchtime = watchtimeList.dropLast(1).takeLast(2).joinToString(",")
            ytMusic.initPlayback(watchtimeUrl, cpn, mapOf("st" to watchtime, "et" to et), playlistId).status.value.let { status ->
                if (status == 204) {
                    Logger.d(TAG, "watchtime done")
                }
                return@runCatching status
            }
        }

    suspend fun updateWatchTimeFull(
        watchtimeUrl: String,
        cpn: String,
        playlistId: String?,
    ): Result<Int> =
        runCatching {
            val regex = Regex("len=([^&]+)")
            val length =
                regex
                    .find(watchtimeUrl)
                    ?.groupValues
                    ?.firstOrNull()
                    ?.drop(4) ?: "0"
            Logger.d(TAG, length)
            ytMusic.initPlayback(watchtimeUrl, cpn, mapOf("st" to length, "et" to length), playlistId).status.value.let { status ->
                if (status == 204) {
                    Logger.d(TAG, "watchtime full done")
                }
                return@runCatching status
            }
        }

    /**
     * @return [Pair<Int, Float>]
     * Int: status code
     * Float: second watchtime
     * First watchtime is 5.54
     */
    suspend fun initPlayback(
        playbackUrl: String,
        atrUrl: String,
        watchtimeUrl: String,
        cpn: String,
        playlistId: String?,
    ): Result<Pair<Int, Float>> {
        Logger.d(TAG, "playbackUrl $playbackUrl")
        Logger.d(TAG, "atrUrl $atrUrl")
        Logger.d(TAG, "watchtimeUrl $watchtimeUrl")
        return runCatching {
            ytMusic.initPlayback(playbackUrl, cpn, null, playlistId).status.value.let { status ->
                if (status == 204) {
                    Logger.d(TAG, "playback done")
                    ytMusic.initPlayback(watchtimeUrl, cpn, mapOf("st" to "0", "et" to "5.54"), playlistId).status.value.let { firstWatchTime ->
                        if (firstWatchTime == 204) {
                            Logger.d(TAG, "first watchtime done")
                            delay(5000)
                            ytMusic.atr(atrUrl, cpn, null, playlistId).status.value.let { atr ->
                                if (atr == 204) {
                                    Logger.d(TAG, "atr done")
                                    delay(500)
                                    val secondWatchTime = (round(Random.nextFloat() * 100.0) / 100.0).toFloat() + 12f
                                    ytMusic
                                        .initPlayback(
                                            watchtimeUrl,
                                            cpn,
                                            mapOf<String, String>("st" to "0,5.54", "et" to "5.54,$secondWatchTime"),
                                            playlistId,
                                        ).status.value
                                        .let { watchtime ->
                                            if (watchtime == 204) {
                                                Logger.d(TAG, "watchtime done")
                                                return@runCatching Pair(watchtime, secondWatchTime)
                                            } else {
                                                return@runCatching Pair(watchtime, secondWatchTime)
                                            }
                                        }
                                } else {
                                    return@runCatching Pair(atr, 0f)
                                }
                            }
                        } else {
                            return@runCatching Pair(firstWatchTime, 0f)
                        }
                    }
                } else {
                    return@runCatching Pair(status, 0f)
                }
            }
        }
    }

    suspend fun nextYouTubePlaylists(continuation: String): Result<Pair<List<MusicTwoRowItemRenderer>, String?>> =
        runCatching {
            val res =
                ytMusic
                    .nextCtoken(
                        WEB_REMIX,
                        continuation,
                    )
            Logger.d(TAG, "Next Playlists ${res.bodyAsText()}")
            val response = res.body<BrowseResponse>()
            Pair(
                response
                    .continuationContents
                    ?.gridContinuation
                    ?.items
                    ?.mapNotNull { it.musicTwoRowItemRenderer } ?: emptyList(),
                response.continuationContents
                    ?.gridContinuation
                    ?.continuations
                    ?.getContinuation(),
            )
        }

    suspend fun next(
        endpoint: WatchEndpoint,
        continuation: String? = null,
    ): Result<NextResult> =
        runCatching {
            val response =
                ytMusic
                    .next(
                        WEB_REMIX,
                        endpoint.videoId,
                        endpoint.playlistId,
                        endpoint.playlistSetVideoId,
                        endpoint.index,
                        endpoint.params,
                        continuation,
                    ).body<NextResponse>()
            val playlistPanelRenderer =
                response.continuationContents?.playlistPanelContinuation
                    ?: response.contents.singleColumnMusicWatchNextResultsRenderer
                        ?.tabbedRenderer
                        ?.watchNextTabbedResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.musicQueueRenderer
                        ?.content
                        ?.playlistPanelRenderer
            if (playlistPanelRenderer != null) {
                // load automix items
                if (playlistPanelRenderer.contents
                        .lastOrNull()
                        ?.automixPreviewVideoRenderer
                        ?.content
                        ?.automixPlaylistVideoRenderer
                        ?.navigationEndpoint
                        ?.watchPlaylistEndpoint !=
                    null
                ) {
                    return@runCatching next(
                        playlistPanelRenderer.contents
                            .lastOrNull()
                            ?.automixPreviewVideoRenderer
                            ?.content
                            ?.automixPlaylistVideoRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint!!,
                    ).getOrThrow()
                        .let { result ->
                            result.copy(
                                title = playlistPanelRenderer.title,
                                items =
                                    playlistPanelRenderer.contents.mapNotNull(NextPage::fromPlaylistPanelContent) +
                                        result.items,
                                lyricsEndpoint =
                                    response.contents.singleColumnMusicWatchNextResultsRenderer
                                        ?.tabbedRenderer
                                        ?.watchNextTabbedResultsRenderer
                                        ?.tabs
                                        ?.getOrNull(
                                            1,
                                        )?.tabRenderer
                                        ?.endpoint
                                        ?.browseEndpoint,
                                relatedEndpoint =
                                    response.contents.singleColumnMusicWatchNextResultsRenderer
                                        ?.tabbedRenderer
                                        ?.watchNextTabbedResultsRenderer
                                        ?.tabs
                                        ?.getOrNull(
                                            2,
                                        )?.tabRenderer
                                        ?.endpoint
                                        ?.browseEndpoint,
                                currentIndex = playlistPanelRenderer.currentIndex,
                                endpoint =
                                    playlistPanelRenderer.contents
                                        .lastOrNull()
                                        ?.automixPreviewVideoRenderer
                                        ?.content
                                        ?.automixPlaylistVideoRenderer
                                        ?.navigationEndpoint
                                        ?.watchPlaylistEndpoint!!,
                            )
                        }
                }
//        else if (playlistPanelRenderer.contents.firstOrNull()?.playlistPanelVideoRenderer?.navigationEndpoint?.watchPlaylistEndpoint != null) {
//
//        }
                return@runCatching NextResult(
                    title = playlistPanelRenderer.title,
                    items = playlistPanelRenderer.contents.mapNotNull(NextPage::fromPlaylistPanelContent),
                    currentIndex = playlistPanelRenderer.currentIndex,
                    lyricsEndpoint =
                        response.contents.singleColumnMusicWatchNextResultsRenderer
                            ?.tabbedRenderer
                            ?.watchNextTabbedResultsRenderer
                            ?.tabs
                            ?.getOrNull(
                                1,
                            )?.tabRenderer
                            ?.endpoint
                            ?.browseEndpoint,
                    relatedEndpoint =
                        response.contents.singleColumnMusicWatchNextResultsRenderer
                            ?.tabbedRenderer
                            ?.watchNextTabbedResultsRenderer
                            ?.tabs
                            ?.getOrNull(
                                2,
                            )?.tabRenderer
                            ?.endpoint
                            ?.browseEndpoint,
                    continuation = playlistPanelRenderer.continuations?.getContinuation(),
                    endpoint = endpoint,
                )
            } else {
                val musicPlaylistShelfContinuation = response.continuationContents?.musicPlaylistShelfContinuation!!
                return@runCatching NextResult(
                    items =
                        musicPlaylistShelfContinuation.contents.mapNotNull {
                            it.musicResponsiveListItemRenderer?.let { renderer ->
                                NextPage.fromMusicResponsiveListItemRenderer(renderer)
                            }
                        },
                    continuation =
                        musicPlaylistShelfContinuation.continuations
                            ?.firstOrNull()
                            ?.nextContinuationData
                            ?.continuation,
                    endpoint =
                        WatchEndpoint(
                            videoId = null,
                            playlistId = null,
                            playlistSetVideoId = null,
                            params = null,
                            index = null,
                            watchEndpointMusicSupportedConfigs = null,
                        ),
                )
            }
        }

    /**
     * YouTube Music's plain lyrics for a video: its watch page's lyrics tab, then that tab's text.
     * Two requests; [next] can cost a third when the queue it reads ends in an automix.
     */
    suspend fun plainLyrics(videoId: String): Result<String?> =
        runCatching {
            val endpoint =
                ytMusic
                    .next(WEB_REMIX, videoId, null, null, null, null, null)
                    .body<NextResponse>()
                    .contents.singleColumnMusicWatchNextResultsRenderer
                    ?.tabbedRenderer
                    ?.watchNextTabbedResultsRenderer
                    ?.tabs
                    ?.getOrNull(1)
                    ?.tabRenderer
                    ?.endpoint
                    ?.browseEndpoint
                    ?: return@runCatching null
            lyrics(endpoint).getOrThrow()
        }

    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
            response.contents
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
                ?.musicDescriptionShelfRenderer
                ?.description
                ?.runs
                ?.firstOrNull()
                ?.text
        }

    suspend fun queue(
        videoIds: List<String>? = null,
        playlistId: String? = null,
    ): Result<List<SongItem>> =
        runCatching {
            ytMusic
                .getQueue(WEB_REMIX, videoIds, playlistId)
                .body<GetQueueResponse>()
                .queueDatas
                .mapNotNull {
                    it.content.playlistPanelVideoRenderer?.let { renderer ->
                        NextPage.fromPlaylistPanelVideoRenderer(renderer)
                    }
                }
        }

    suspend fun visitorData(): String? =
        try {
            Json
                .parseToJsonElement(ytMusic.getSwJsData().bodyAsText().substring(5))
                .jsonArray[0]
                .jsonArray[2]
                .jsonArray
                .first { (it as? JsonPrimitive)?.content?.let(VISITOR_DATA_REGEX::containsMatchIn) == true }
                .jsonPrimitive.content
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    suspend fun accountInfo(customCookie: String? = null): Result<AccountInfo?> =
        runCatching {
            ytMusic
                .accountMenu(customCookie, WEB_REMIX)
                .apply {
                    Logger.d(TAG, this.bodyAsText())
                }.body<AccountMenuResponse>()
                .actions[0]
                .openPopupAction.popup.multiPageMenuRenderer.header
                ?.activeAccountHeaderRenderer
                ?.toAccountInfo()
        }

    suspend fun getAccountListWithPageId(customCookie: String): Result<List<AccountInfo>> =
        runCatching {
            val res =
                ytMusic
                    .getAccountSwitcherEndpoint(customCookie)
                    .bodyAsText()
                    .removePrefix(")]}'\n")
            val accountSwitcherEndpointResponse = ytMusic.normalJson.decodeFromString<AccountSwitcherEndpointResponse>(res)
            Logger.d(TAG, "Account List Response: $accountSwitcherEndpointResponse")
            accountSwitcherEndpointResponse.toListAccountInfo()
        }

    suspend fun getLibraryPlaylists() =
        runCatching {
            ytMusic.browse(WEB_REMIX, "FEmusic_liked_playlists", setLogin = true).body<BrowseResponse>()
        }

    /**
     * One page of the account's library: `FEmusic_liked_albums`, `FEmusic_library_corpus_track_artists`
     * (the artists behind your songs), `FEmusic_library_corpus_artists` (subscriptions) or
     * `FEmusic_liked_videos` (Library ▸ Songs), or the page after [continuation].
     */
    suspend fun libraryPage(
        browseId: String,
        continuation: String? = null,
    ): Result<LibraryPage> =
        runCatching {
            val response =
                if (continuation == null) {
                    ytMusic.browse(WEB_REMIX, browseId, setLogin = true)
                } else {
                    ytMusic.nextCtoken(WEB_REMIX, continuation)
                }
            parseLibraryPage(response.body<JsonObject>(), ytMusic.normalJson)
        }

    suspend fun getMixedForYou() =
        runCatching {
            ytMusic.browse(WEB_REMIX, "FEmusic_mixed_for_you", setLogin = true).body<BrowseResponse>()
        }

    @JvmInline
    value class SearchFilter(
        val value: String,
    ) {
        companion object {
            val FILTER_SONG: SearchFilter = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            val FILTER_VIDEO: SearchFilter = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ALBUM: SearchFilter = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ARTIST: SearchFilter = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_FEATURED_PLAYLIST: SearchFilter = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
            val FILTER_COMMUNITY_PLAYLIST: SearchFilter = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
            val FILTER_PODCAST: SearchFilter = SearchFilter("EgWKAQJQAWoIEBAQERADEBU%3D")
        }
    }

    suspend fun getYTMusicSearchSuggestions(query: String) =
        runCatching {
            val response = ytMusic.getSearchSuggestions(WEB_REMIX, query).body<GetSearchSuggestionsResponse>()
            SearchSuggestions(
                queries =
                    response.contents
                        ?.getOrNull(0)
                        ?.searchSuggestionsSectionRenderer
                        ?.contents
                        ?.mapNotNull { content ->
                            content.searchSuggestionRenderer
                                ?.suggestion
                                ?.runs
                                ?.joinToString(separator = "") { it.text }
                        }.orEmpty(),
                recommendedItems =
                    response.contents
                        ?.getOrNull(1)
                        ?.searchSuggestionsSectionRenderer
                        ?.contents
                        ?.mapNotNull {
                            it.musicResponsiveListItemRenderer?.let { renderer ->
                                SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer)
                            }
                        }.orEmpty(),
            )
        }

    suspend fun scrapeYouTube(videoId: String) =
        runCatching {
            ytMusic.scrapeYouTube(videoId).body<String>()
        }

    suspend fun removeItemYouTubePlaylist(
        playlistId: String,
        videoId: String,
        setVideoId: String,
    ) = runCatching {
        ytMusic.removeItemYouTubePlaylist(playlistId, videoId, setVideoId).status.value
    }

    /** Fails on an HTTP error or YouTube's STATUS_FAILED answer, which comes with a 200. */
    suspend fun removePlaylistItems(
        playlistId: String,
        items: List<Pair<String, String>>,
    ) = runCatching {
        val response = ytMusic.removeItemsYouTubePlaylist(playlistId, items)
        check(response.status.value in 200..299) { "HTTP ${response.status.value}" }
        check(!response.bodyAsText().contains("\"STATUS_FAILED\"")) { "YouTube refused the playlist edit" }
    }

    suspend fun addPlaylistItem(
        playlistId: String,
        videoId: String,
    ) = runCatching {
        ytMusic.addItemYouTubePlaylist(playlistId, videoId).body<AddItemYouTubePlaylistResponse>()
    }

    /**
     * Move a playlist item before another item in a YouTube playlist.
     * @param playlistId The YouTube playlist ID
     * @param setVideoId The setVideoId of the item to move
     * @param movedSetVideoIdSuccessor The setVideoId of the item that should come AFTER the moved item.
     *        If null, the item is moved to the end of the playlist.
     * @return Result<Int> HTTP status code
     */
    suspend fun movePlaylistItem(
        playlistId: String,
        setVideoId: String,
        movedSetVideoIdSuccessor: String? = null,
    ) = runCatching {
        ytMusic.moveItemYouTubePlaylist(playlistId, setVideoId, movedSetVideoIdSuccessor).status.value
    }

    suspend fun editPlaylist(
        playlistId: String,
        title: String? = null,
        description: String? = null,
        privacy: String? = null,
    ) = runCatching {
        ytMusic.editYouTubePlaylist(playlistId, title, description, privacy).status.value
    }

    /** Every client on the account then shows the playlist in [sort]. Your own playlists only. */
    suspend fun setPlaylistSort(
        playlistId: String,
        sort: PlaylistSort,
    ) = runCatching {
        ytMusic.setPlaylistSort(playlistId, requireNotNull(sort.editAction()) { "$sort is not stored by YouTube" }).status.value
    }

    suspend fun deletePlaylist(playlistId: String) =
        runCatching {
            ytMusic.deleteYouTubePlaylist(playlistId).status.value
        }

    /**
     * Puts a custom cover on a YouTube Music playlist.
     *
     * Three legs, because that is what the web client does: reserve a resumable upload slot, send
     * the bytes, then attach the returned blob to the playlist. The upload id comes back as a
     * response HEADER (`x-guploader-uploadid`) with an empty body, which is easy to miss.
     *
     * Returns the HTTP status of the final attach, the same way [editPlaylist] does — the response
     * body is a fresh playlist header, and nothing here needs to read it back.
     */
    suspend fun setPlaylistCustomThumbnail(
        playlistId: String,
        image: ByteArray,
    ) = runCatching {
        val uploadId =
            ytMusic
                .getPlaylistThumbnailUploadSlot(image.size)
                .headers["x-guploader-uploadid"]
                ?: throw IllegalStateException("No upload id returned for playlist thumbnail")
        // bodyAsText + manual decode, NOT body<T>(): this endpoint answers with JSON but does not
        // label it as such — the response carries an html content type — so ContentNegotiation
        // never engages and body<T>() fails with "expected ImageUploadResponse but was
        // SourceByteReadChannel" on an otherwise perfectly good 200.
        val blobId =
            Json { ignoreUnknownKeys = true }
                .decodeFromString<ImageUploadResponse>(
                    ytMusic.uploadPlaylistThumbnail(uploadId, image).bodyAsText(),
                ).encryptedBlobId
        ytMusic.setYouTubePlaylistCustomThumbnail(playlistId, blobId).status.value
    }

    suspend fun createPlaylist(
        title: String,
        listVideoId: List<String>?,
    ) = runCatching {
        ytMusic.createYouTubePlaylist(title, listVideoId).body<CreatePlaylistResponse>()
    }

    /**
     * Subscribes the signed-in account to a channel.
     *
     * Returns the HTTP status rather than a parsed body, the same way the like endpoints do —
     * these calls answer with an empty payload, so the status is the whole result.
     */
    suspend fun subscribeChannel(channelId: String) =
        runCatching {
            ytMusic.subscribeChannel(channelId).status.value
        }

    suspend fun unsubscribeChannel(channelId: String) =
        runCatching {
            ytMusic.unsubscribeChannel(channelId).status.value
        }

    suspend fun addToLiked(mediaId: String) =
        runCatching {
            ytMusic.addToLiked(mediaId).status.value
        }

    suspend fun removeFromLiked(mediaId: String) =
        runCatching {
            ytMusic.removeFromLiked(mediaId).status.value
        }

    /** Save a playlist to the library or take it out; for an album pass its `OLAK5uy_` playlist. */
    suspend fun likePlaylist(
        playlistId: String,
        like: Boolean,
    ) = runCatching {
        ytMusic.likePlaylist(playlistId.removePrefix("VL"), like).status.value
    }

    /**
     * Which client produced this video's stream: the resolve of this run when there was one, else
     * what [streamUrl], the saved URL it plays from, says. Diagnostic only, see
     * [com.maxrave.kotlinytmusicscraper.extractor.ExtractSource].
     */
    fun getExtractSource(
        videoId: String,
        streamUrl: String?,
    ): String? = ExtractSource.of(videoId) ?: streamUrl?.let(ExtractSource::fromStreamUrl)

    private fun getNParam(listFormat: List<PlayerResponse.StreamingData.Format>): String? =
        listFormat
            .firstOrNull { it.itag == ITAG.AUDIO_OPUS_MEDIUM }
            ?.let { format ->
                val sc = format.signatureCipher ?: format.url ?: return null
                val params = parseQueryString(sc)
                val url =
                    params["url"]?.let { URLBuilder(it) }
                        ?: run {
                            Logger.e(TAG, "Could not parse cipher url")
                            return null
                        }
                url.parameters["n"]
            }

    fun download(
        track: SongItem,
        filePath: String,
        videoId: String,
        isVideo: Boolean = false,
    ): Flow<DownloadProgress> =
        channelFlow {
            // Video if videoId is not null
            trySend(DownloadProgress(0.01f))
            player(videoId = videoId)
                .onSuccess { playerResponse ->
                    val audioFormat =
                        listOf(
                            playerResponse.second.streamingData
                                ?.formats
                                ?.filter { it.isAudio && it.url != null }
                                ?.maxByOrNull { it.bitrate },
                            playerResponse.second.streamingData
                                ?.adaptiveFormats
                                ?.filter { it.isAudio && it.url != null }
                                ?.maxByOrNull { it.bitrate },
                        ).maxByOrNull { it?.bitrate ?: 0 }
                    val videoFormat =
                        listOf(
                            playerResponse.second.streamingData
                                ?.formats
                                ?.filter { !it.isAudio && it.url != null }
                                ?.maxByOrNull { it.bitrate },
                            playerResponse.second.streamingData
                                ?.adaptiveFormats
                                ?.filter { !it.isAudio && it.url != null }
                                ?.maxByOrNull { it.bitrate },
                        ).maxByOrNull { it?.bitrate ?: 0 }
                    Logger.d(TAG, "Audio Format $audioFormat")
                    Logger.d(TAG, "Video Format $videoFormat")
                    val audioUrl =
                        audioFormat?.url ?: run {
                            trySend(DownloadProgress.failed("Audio format url is null"))
                            return@channelFlow
                        }
                    if (isVideo) {
                        runCatching {
                            val videoUrl = videoFormat?.url ?: throw Exception("Video format url is null")
                            val downloadAudioJob = ytMusic.download(audioUrl, ("$filePath.webm"))
                            val downloadVideoJob = ytMusic.download(videoUrl, ("$filePath.mp4"))
                            combine(downloadVideoJob, downloadAudioJob) { videoProgress, audioProgress ->
                                Pair(videoProgress, audioProgress)
                            }.collectLatest { (videoProgress, audioProgress) ->
                                if (!videoProgress.first || !audioProgress.first) {
                                    trySend(
                                        DownloadProgress(
                                            videoDownloadProgress = videoProgress.second,
                                            audioDownloadProgress = audioProgress.second,
                                            downloadSpeed = if (videoProgress.third != 0) videoProgress.third else audioProgress.third,
                                        ),
                                    )
                                } else {
                                    trySend(DownloadProgress.MERGING)
                                    trySend(
                                        ytMusic.mergeAudioVideoDownload(
                                            filePath,
                                        ),
                                    )
                                }
                            }
                        }.onSuccess {
                            Logger.d(TAG, "Download Video Success")
                        }.onFailure {
                            it.printStackTrace()
                            trySend(DownloadProgress.failed(it.message ?: "Download failed"))
                        }
                    } else {
                        // Song if url is not null
                        runCatching {
                            ytMusic
                                .download(audioUrl, ("$filePath.webm"))
                                .collect { downloadProgress ->
                                    if (!downloadProgress.first) {
                                        trySend(DownloadProgress(audioDownloadProgress = downloadProgress.second))
                                    } else {
                                        trySend(DownloadProgress(audioDownloadProgress = 1f, isDone = true))
                                    }
                                }
                        }.onSuccess {
                            Logger.d(TAG, "Download only Audio Success")
                            // Convert to mp3
                            trySend(
                                ytMusic.saveAudioWithThumbnail(
                                    filePath,
                                    track,
                                ),
                            )
                        }.onFailure { e ->
                            e.printStackTrace()
                            trySend(DownloadProgress.failed(e.message ?: "Download failed"))
                        }
                    }
                }.onFailure {
                    it.printStackTrace()
                    Logger.d(TAG, "Player Response is null")
                    trySend(DownloadProgress.failed(it.message ?: "Player response is null"))
                }
        }.flowOn(Dispatchers.IO)

    suspend fun is403Url(url: String) = ytMusic.is403Url(url)

    companion object {
        const val MAX_GET_QUEUE_SIZE = 1000

        // Visitor data is a base64 protobuf: bytes 0x0a 0x0b (field 1, length 11) then an 11-char
        // id. "Cg" is fixed; the third char encodes the top 2 bits of the id's first byte, so it is
        // "s" for a digit or "-" and "t" for a letter or "_" ("u"/"v" only for a non-ASCII byte).
        // Matching "Cgt" alone missed every id starting with a digit.
        private val VISITOR_DATA_REGEX = Regex("^Cg[stuv][A-Za-z0-9_-]{15,}")

        const val DEFAULT_VISITOR_DATA = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"

    }
}

/** The player answered, but not with OK: [status] is e.g. `UNPLAYABLE`, [reason] YouTube's sentence. */
class PlayerRefusedException(
    val status: String,
    val reason: String?,
) : Exception("$status: $reason") {
    /** "Sign in to confirm you're not a bot": about the IP, not the song. Age checks also say "Sign in". */
    val isBotCheck: Boolean get() = reason?.contains("not a bot", ignoreCase = true) == true
}

/** YouTube answered 429: too many requests from this IP for now. */
val Throwable.isRateLimit: Boolean
    get() = (this as? ClientRequestException)?.response?.status == HttpStatusCode.TooManyRequests
