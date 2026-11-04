@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.simohypers.limusic.data.repository

import com.simohypers.limusic.data.db.Converters
import com.simohypers.limusic.data.db.datasource.LocalDataSource
import com.simohypers.limusic.data.io.readLocalImageBytes
import com.simohypers.limusic.data.extension.getFullDataFromDB
import com.simohypers.limusic.data.mapping.toTrack
import com.simohypers.limusic.data.parser.parseSetVideoId
import com.simohypers.limusic.domain.data.entities.LocalPlaylistEntity
import com.simohypers.limusic.domain.data.entities.PairSongLocalPlaylist
import com.simohypers.limusic.domain.data.entities.SetVideoIdEntity
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.extension.now
import com.simohypers.limusic.domain.repository.LocalPlaylistRepository
import com.simohypers.limusic.domain.utils.LocalResource
import com.simohypers.limusic.domain.utils.wrapDataResource
import com.simohypers.limusic.domain.utils.wrapMessageResource
import com.simohypers.limusic.kotlinytmusicscraper.YouTube
import com.simohypers.limusic.kotlinytmusicscraper.extension.verifyYouTubePlaylistId
import com.simohypers.limusic.kotlinytmusicscraper.models.MusicShelfRenderer
import com.simohypers.limusic.kotlinytmusicscraper.models.SongItem
import com.simohypers.limusic.kotlinytmusicscraper.models.response.SearchResponse
import com.simohypers.limusic.kotlinytmusicscraper.pages.NextPage
import com.simohypers.limusic.kotlinytmusicscraper.pages.SearchPage
import com.simohypers.limusic.kotlinytmusicscraper.parser.getPlaylistContinuation
import com.simohypers.limusic.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val TAG = "LocalPlaylistRepositoryImpl"

internal class LocalPlaylistRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : LocalPlaylistRepository {
    override fun getLocalPlaylist(id: Long) =
        wrapDataResource {
            localDataSource.getLocalPlaylist(id)
        }

    override fun getAllLocalPlaylists(): Flow<List<LocalPlaylistEntity>> =
        flow {
            val list =
                getFullDataFromDB { limit, offset ->
                    localDataSource.getAllLocalPlaylists(limit, offset)
                }
            emit(list)
        }.flowOn(Dispatchers.IO)

    override suspend fun updateLocalPlaylistTracks(
        tracks: List<String>,
        id: Long,
    ) = withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistTracks(tracks, id) }

    override suspend fun updateLocalPlaylistDownloadState(
        downloadState: Int,
        id: Long,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateLocalPlaylistDownloadState(
            downloadState,
            id,
        )
    }

    override suspend fun updateLocalPlaylistYouTubePlaylistSyncState(
        id: Long,
        syncState: Int,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)
    }

    override fun downloadStateFlow(id: Long): Flow<Int> = localDataSource.getDownloadStateFlowOfLocalPlaylist(id)

    override fun listTrackFlow(id: Long): Flow<List<String>> =
        localDataSource
            .getListTracksFlowOfLocalPlaylist(id)
            .map { Converters().fromString(it.firstOrNull()) ?: emptyList() }

    override fun searchTracks(
        id: Long,
        query: String,
        limit: Int,
    ): Flow<List<Pair<SongEntity, PairSongLocalPlaylist>>> =
        flow { emit(withSongs(localDataSource.searchPlaylistPairSong(id, query, limit))) }.flowOn(Dispatchers.IO)

    override fun playlistTracks(id: Long): Flow<List<Pair<SongEntity, PairSongLocalPlaylist>>> =
        localDataSource.playlistPairsFlow(id).map { withSongs(it) }.flowOn(Dispatchers.IO)

    /** The song behind each row, in the rows' order; the lookup caps at 1000 ids, so in chunks. */
    private suspend fun withSongs(pairs: List<PairSongLocalPlaylist>): List<Pair<SongEntity, PairSongLocalPlaylist>> {
        val byId =
            pairs
                .map { it.songId }
                .distinct()
                .chunked(500)
                .flatMap { localDataSource.getSongByListVideoIdFull(it) }
                .associateBy { it.videoId }
        return pairs.mapNotNull { pair -> byId[pair.songId]?.let { it to pair } }
    }

    override suspend fun getFullPlaylistTracks(id: Long): List<SongEntity> =
        withSongs(localDataSource.getAllPlaylistPairSongByPosition(id)).map { it.first }

    override suspend fun getListTrackVideoId(id: Long): List<String> {
        val playlist = localDataSource.getLocalPlaylist(id)
        return playlist?.tracks ?: emptyList()
    }

    override fun insertLocalPlaylist(
        localPlaylist: LocalPlaylistEntity,
        successMessage: String,
    ): Flow<LocalResource<String>> =
        wrapMessageResource(
            successMessage = successMessage,
        ) {
            localDataSource.insertLocalPlaylist(localPlaylist)
        }

    override suspend fun createLocalPlaylist(title: String): Long =
        withContext(Dispatchers.IO) { localDataSource.insertLocalPlaylist(LocalPlaylistEntity(title = title)) }

    override fun deleteLocalPlaylist(
        id: Long,
        successMessage: String,
    ) = wrapMessageResource(
        successMessage = successMessage,
    ) {
        localDataSource.deleteLocalPlaylist(id)
    }

    override fun updateTitleLocalPlaylist(
        id: Long,
        newTitle: String,
        updatedMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>> =
        flow {
            emit(LocalResource.Loading<String>())
            runCatching {
                localDataSource.updateLocalPlaylistTitle(id = id, title = newTitle)
            }.onSuccess {
                emit(LocalResource.Success(updatedMessage))
                val localPlaylist = localDataSource.getLocalPlaylist(id)
                val ytId = localPlaylist?.youtubePlaylistId
                if (ytId != null) {
                    youTube
                        .editPlaylist(ytId, newTitle)
                        .onSuccess {
                            emit(LocalResource.Success(updatedYtMessage))
                        }.onFailure {
                            emit(LocalResource.Error<String>(it.message ?: errorMessage))
                        }
                }
            }.onFailure {
                emit(LocalResource.Error<String>(it.message ?: errorMessage))
            }
        }.flowOn(Dispatchers.IO)

    override fun updateThumbnailLocalPlaylist(
        id: Long,
        newThumbnail: String,
        successMessage: String,
    ) = flow<LocalResource<String>> {
        emit(LocalResource.Loading())
        runCatching {
            localDataSource.updateLocalPlaylistThumbnail(id = id, thumbnail = newThumbnail)
            pushCustomThumbnailToYouTube(id, newThumbnail)
        }.onSuccess { youTubeError ->
            // The image IS saved locally either way. Reporting the YouTube half as an error anyway
            // is deliberate: the user pressed this to change the cover of a synced playlist, and
            // silently keeping the old one on YouTube is the failure they need to know about.
            if (youTubeError == null) {
                emit(LocalResource.Success(successMessage))
            } else {
                emit(LocalResource.Error<String>(youTubeError))
            }
        }.onFailure {
            emit(LocalResource.Error<String>(it.message ?: "Error updating playlist thumbnail"))
        }
    }.flowOn(Dispatchers.IO)

    override fun updateDownloadState(
        id: Long,
        downloadState: Int,
        successMessage: String,
    ) = wrapMessageResource(
        successMessage = successMessage,
    ) {
        localDataSource.updateLocalPlaylistDownloadState(id = id, downloadState = downloadState)
    }

    /**
     * "Move to YouTube Music": the playlist goes onto the account with its tracks and a cover the
     * user picked, then the copy on the phone goes. Answers the new playlist's id.
     */
    override suspend fun moveLocalPlaylistToYouTube(id: Long): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val playlist = localDataSource.getLocalPlaylist(id) ?: error("Playlist not found")
                val ytId = youTube.createPlaylist(playlist.title, playlist.tracks).getOrThrow().playlistId
                // The cover push reads the YouTube id off the row, so it goes on before the push.
                localDataSource.updateLocalPlaylistYouTubePlaylistId(id, ytId)
                pushCustomThumbnailToYouTube(id, playlist.thumbnail)
                localDataSource.deleteLocalPlaylist(id)
                ytId
            }
        }

    /**
     * Mirrors a locally picked cover onto the YouTube playlist backing this local one.
     *
     * Silent on every failure. It is a best-effort extra on top of an operation the user already
     * considers done — the cover is saved locally either way, and a signed-out account or a revoked
     * file permission must not turn "cover changed" into an error.
     */
    private suspend fun pushCustomThumbnailToYouTube(
        playlistId: Long,
        thumbnail: String?,
    ): String? {
        // Returns null when there was nothing to do or the upload worked, and a reason when the
        // user should be told. Two of the three "nothing to do" cases are NOT failures: a playlist
        // that is not synced has no YouTube cover to set, and a http(s) thumbnail is one this app
        // derived from a track rather than something the user picked — pushing that back would
        // overwrite the cover with art nobody chose.
        val ytId =
            runCatching {
                localDataSource
                    .getLocalPlaylist(playlistId)
                    ?.youtubePlaylistId
                    ?.takeIf { it.isNotBlank() }
            }.getOrNull()
        if (ytId == null) {
            Logger.d(TAG, "Cover push skipped for $playlistId: playlist is not synced to YouTube")
            return null
        }
        val uri = thumbnail?.takeIf { it.isNotBlank() && !it.startsWith("http") }
        if (uri == null) {
            Logger.d(TAG, "Cover push skipped for $playlistId: not a locally picked image ($thumbnail)")
            return null
        }
        val bytes = readLocalImageBytes(uri)
        if (bytes == null) {
            Logger.w(TAG, "Cover push failed for $playlistId: could not read $uri")
            return "Could not read the selected image"
        }
        Logger.d(TAG, "Uploading cover for $playlistId -> $ytId (${bytes.size} bytes)")
        return youTube
            .setPlaylistCustomThumbnail(ytId, bytes)
            .fold(
                onSuccess = { status ->
                    Logger.d(TAG, "Cover uploaded for $ytId, status $status")
                    if (status in 200..299) null else "YouTube rejected the cover (HTTP $status)"
                },
                onFailure = {
                    Logger.e(TAG, "Cover upload failed for $ytId: ${it.message}")
                    it.message ?: "Could not update the cover on YouTube"
                },
            )
    }

    // Update
    override fun addTrackToLocalPlaylist(
        id: Long,
        song: SongEntity,
        successMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>> =
        flow {
            emit(LocalResource.Loading())
            val checkSong = localDataSource.getSong(song.videoId)
            if (checkSong == null) {
                localDataSource.insertSong(song)
            }
            val localPlaylist = localDataSource.getLocalPlaylist(id) ?: return@flow
            val nextPosition = localPlaylist.tracks?.size ?: 0
            val nextPair =
                PairSongLocalPlaylist(
                    playlistId = id,
                    songId = song.videoId,
                    position = nextPosition,
                    inPlaylist = now(),
                )
            runBlocking {
                localDataSource.insertPairSongLocalPlaylist(nextPair)
                localDataSource.updateLocalPlaylistTracks(
                    localPlaylist.tracks?.plus(song.videoId) ?: mutableListOf(song.videoId),
                    id,
                )
            }
            // Emit success message
            emit(LocalResource.Success(successMessage))

            // Add to YouTube playlist
            val ytId = localPlaylist.youtubePlaylistId
            if (ytId != null) {
                youTube
                    .addPlaylistItem(ytId, song.videoId)
                    .onSuccess {
                        val data = it.playlistEditResults
                        if (data.isNotEmpty()) {
                            for (d in data) {
                                localDataSource.insertSetVideoId(
                                    SetVideoIdEntity(
                                        videoId = d.playlistEditVideoAddedResultData.videoId,
                                        setVideoId = d.playlistEditVideoAddedResultData.setVideoId,
                                        youtubePlaylistId = ytId,
                                    ),
                                )
                            }
                            emit(LocalResource.Success(updatedYtMessage))
                        } else {
                            emit(LocalResource.Error<String>("$errorMessage: Empty playlistEditResults"))
                        }
                    }.onFailure {
                        emit(LocalResource.Error<String>("$errorMessage: ${it.message}"))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun removeTrackFromLocalPlaylist(
        id: Long,
        song: SongEntity,
        successMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>> =
        flow {
            emit(LocalResource.Loading())
            val localPlaylist = localDataSource.getLocalPlaylist(id) ?: return@flow
            val nextTracks = localPlaylist.tracks?.toMutableList() ?: mutableListOf()
            nextTracks.remove(song.videoId)
            localDataSource.updateLocalPlaylistTracks(nextTracks, id)
            localDataSource.deletePairSongLocalPlaylist(id, song.videoId)
            emit(LocalResource.Success(successMessage))
            val ytPlaylistId = localPlaylist.youtubePlaylistId
            if (ytPlaylistId != null) {
                val entry = findSetVideoId(song.videoId, ytPlaylistId)
                val setVideoId = entry?.setVideoId
                if (entry != null && setVideoId != null) {
                    youTube
                        .removeItemYouTubePlaylist(ytPlaylistId, song.videoId, setVideoId)
                        .onSuccess {
                            // Nothing else ever deletes from set_video_id: a row left behind for an
                            // entry YouTube no longer has would be sent again on the next lookup.
                            // A legacy "" row is kept, since it may hold another playlist's entry
                            // that the other playlist still falls back to.
                            if (entry.youtubePlaylistId.isNotEmpty()) {
                                localDataSource.deleteSetVideoId(entry.videoId, entry.youtubePlaylistId)
                            }
                            emit(LocalResource.Success(successMessage))
                        }.onFailure {
                            emit(LocalResource.Error<String>("$errorMessage: ${it.message}"))
                        }
                } else {
                    emit(LocalResource.Error<String>("$errorMessage: SetVideoId is null"))
                }
            }
        }.flowOn(Dispatchers.IO)

    /**
     * The stored entry of [videoId] in the YouTube playlist [youtubePlaylistId], matched with and
     * without the "VL" browse prefix since both spellings are stored.
     *
     * Rows written before the playlist id was recorded carry "". Such a row is used only when no row
     * names this playlist; it is the row every lookup returned first before, so old data with nothing
     * better resolves exactly as it used to.
     */
    private suspend fun findSetVideoId(
        videoId: String,
        youtubePlaylistId: String,
    ): SetVideoIdEntity? {
        val bareId = youtubePlaylistId.removePrefix("VL")
        return localDataSource.getSetVideoId(videoId, listOf(bareId, "VL$bareId"))
            ?: localDataSource.getSetVideoId(videoId, listOf(""))
    }

    override fun getSuggestionsTrackForPlaylist(id: Long): Flow<LocalResource<Pair<String?, List<Track>>>> =
        flow {
            val localPlaylist = localDataSource.getLocalPlaylist(id) ?: return@flow
            val ytPlaylistId = localPlaylist.youtubePlaylistId ?: return@flow

            youTube
                .getSuggestionsTrackForPlaylist(ytPlaylistId)
                .onSuccess { data ->
                    val listSongItem = data?.second?.map { it.toTrack() }
                    if (data != null && !listSongItem.isNullOrEmpty()) {
                        emit(
                            LocalResource.Success(
                                Pair(
                                    data.first,
                                    listSongItem,
                                ),
                            ),
                        )
                    } else {
                        emit(LocalResource.Error("List suggestions is null"))
                    }
                }.onFailure { e ->
                    e.printStackTrace()
                    emit(LocalResource.Error(e.message ?: "Error"))
                }
        }

    override fun reloadSuggestionPlaylist(reloadParams: String): Flow<LocalResource<Pair<String?, List<Track>>>> =
        flow {
            runCatching {
                emit(LocalResource.Loading())
                youTube
                    .customQuery(browseId = "", continuation = reloadParams, setLogin = true)
                    .onSuccess { values ->
                        val data = values.continuationContents?.musicShelfContinuation?.contents
                        val dataResult:
                            ArrayList<SearchResponse.ContinuationContents.MusicShelfContinuation.Content> =
                            arrayListOf()
                        if (!data.isNullOrEmpty()) {
                            dataResult.addAll(data)
                        }
                        val reloadParamsNew =
                            values.continuationContents
                                ?.musicShelfContinuation
                                ?.continuations
                                ?.get(
                                    0,
                                )?.reloadContinuationData
                                ?.continuation
                        if (dataResult.isNotEmpty()) {
                            val listTrack: ArrayList<Track> = arrayListOf()
                            dataResult.forEach {
                                listTrack.add(
                                    (
                                        SearchPage.toYTItem(
                                            it.musicResponsiveListItemRenderer,
                                        ) as SongItem
                                    ).toTrack(),
                                )
                            }
                            emit(LocalResource.Success(Pair(reloadParamsNew, listTrack.toList())))
                        } else {
                            emit(LocalResource.Error("Empty data"))
                        }
                    }.onFailure { exception ->
                        exception.printStackTrace()
                        emit(LocalResource.Error(exception.message ?: "Error"))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getYouTubeSetVideoId(youtubePlaylistId: String): Flow<List<SetVideoIdEntity>> =
        flow {
            runCatching {
                var id = ""
                if (!youtubePlaylistId.startsWith("VL")) {
                    id += "VL$youtubePlaylistId"
                } else {
                    id += youtubePlaylistId
                }
                Logger.d("Repository", "playlist id: $id")
                youTube
                    .customQuery(browseId = id, setLogin = true)
                    .onSuccess { result ->
                        val listContent: ArrayList<SongItem> = arrayListOf()
                        val data: List<MusicShelfRenderer.Content>? =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.get(
                                    0,
                                )?.musicPlaylistShelfRenderer
                                ?.contents
                        var continueParam =
                            result.getPlaylistContinuation()
                        var count = 0
                        Logger.d("Repository", "playlist data: ${listContent.size}")
                        Logger.d("Repository", "continueParam: $continueParam")
                        while (continueParam != null) {
                            youTube
                                .customQuery(
                                    browseId = "",
                                    continuation = continueParam,
                                    setLogin = true,
                                ).onSuccess { values ->
                                    Logger.d("getPlaylistData", "continue: $continueParam")
                                    Logger.d(
                                        "getPlaylistData",
                                        "values: ${values.onResponseReceivedActions}",
                                    )
                                    val dataMore: List<SongItem> =
                                        values.onResponseReceivedActions
                                            ?.firstOrNull()
                                            ?.appendContinuationItemsAction
                                            ?.continuationItems
                                            ?.apply {
                                                Logger.d("getPlaylistData", "dataMore: ${this.size}")
                                            }?.mapNotNull {
                                                NextPage.fromMusicResponsiveListItemRenderer(
                                                    it.musicResponsiveListItemRenderer ?: return@mapNotNull null,
                                                )
                                            } ?: emptyList()
                                    listContent.addAll(dataMore)
                                    continueParam =
                                        values.getPlaylistContinuation()
                                    count++
                                }.onFailure {
                                    Logger.e("Continue", "Error: ${it.message}")
                                    continueParam = null
                                    count++
                                }
                        }
                        Logger.d("Repository", "playlist final data: ${listContent.size}")
                        parseSetVideoId(youtubePlaylistId, data ?: emptyList()).let { playlist ->
                            playlist.forEach { item ->
                                localDataSource.insertSetVideoId(item)
                            }
                            listContent.forEach { item ->
                                localDataSource.insertSetVideoId(
                                    SetVideoIdEntity(
                                        videoId = item.id,
                                        setVideoId = item.setVideoId,
                                        youtubePlaylistId = youtubePlaylistId,
                                    ),
                                )
                            }
                            emit(playlist)
                        }
                    }.onFailure { e ->
                        e.printStackTrace()
                        emit(emptyList())
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun addYouTubePlaylistItem(
        youtubePlaylistId: String,
        videoId: String,
    ): Flow<LocalResource<String>> =
        flow {
            emit(LocalResource.Loading())
            youTube
                .addPlaylistItem(youtubePlaylistId.verifyYouTubePlaylistId(), videoId)
                .onSuccess {
                    if (it.playlistEditResults.isNotEmpty()) {
                        for (playlistEditResult in it.playlistEditResults) {
                            localDataSource.insertSetVideoId(
                                SetVideoIdEntity(
                                    videoId = playlistEditResult.playlistEditVideoAddedResultData.videoId,
                                    setVideoId = playlistEditResult.playlistEditVideoAddedResultData.setVideoId,
                                    youtubePlaylistId = youtubePlaylistId.removePrefix("VL"),
                                ),
                            )
                        }
                        emit(LocalResource.Success(it.status))
                    } else {
                        emit(LocalResource.Error("FAILED"))
                    }
                }.onFailure {
                    emit(LocalResource.Error("FAILED"))
                }
        }.flowOn(Dispatchers.IO)

    override suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) =
        withContext(Dispatchers.IO) {
            localDataSource.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }

    override fun getPlaylistPairSongByListPosition(
        playlistId: Long,
        listPosition: List<Int>,
    ): Flow<List<PairSongLocalPlaylist>?> =
        flow {
            emit(localDataSource.getPlaylistPairSongByListPosition(playlistId, listPosition))
        }.flowOn(Dispatchers.IO)

    override fun getPlaylistPairOfSong(
        playlistId: Long,
        videoId: String,
    ): Flow<PairSongLocalPlaylist?> =
        flow {
            emit(
                localDataSource.getPlaylistPairOfSong(
                    videoId,
                    playlistId,
                ),
            )
        }.flowOn(Dispatchers.IO)

    override fun changePositionOfSongInPlaylist(
        playlistId: Long,
        videoId: String,
        newPosition: Int,
    ): Flow<String> =
        flow {
            localDataSource.editPositionOfSongInPlaylist(
                playlistId,
                videoId,
                newPosition,
            )
            delay(100)
            emit("Position updated")
        }.flowOn(Dispatchers.IO)

    /**
     * Move a song within a synced playlist.
     * 1. Resolve setVideoIds for the moved item and its new successor from the local DB
     * 2. Call YouTube API (ACTION_MOVE_VIDEO_BEFORE) to reorder on YouTube
     * 3. Update local DB positions (shift items between from/to, then place the moved item)
     */
    override fun moveItemInSyncedPlaylist(
        playlistId: Long,
        fromIndex: Int,
        toIndex: Int,
    ): Flow<LocalResource<String>> =
        flow<LocalResource<String>> {
            if (fromIndex == toIndex) {
                emit(LocalResource.Success("No change"))
                return@flow
            }
            emit(LocalResource.Loading())

            val localPlaylist = localDataSource.getLocalPlaylist(playlistId) ?: run {
                emit(LocalResource.Error("Playlist not found"))
                return@flow
            }
            val ytPlaylistId = localPlaylist.youtubePlaylistId ?: run {
                emit(LocalResource.Error("Playlist is not synced with YouTube"))
                return@flow
            }

            // Get all pairs ordered by position to resolve indexes
            val allPairs = localDataSource.getAllPlaylistPairSongByPosition(playlistId)
            if (fromIndex >= allPairs.size || toIndex >= allPairs.size) {
                emit(LocalResource.Error("Index out of bounds"))
                return@flow
            }

            val movedPair = allPairs[fromIndex]
            val movedVideoId = movedPair.songId

            // Resolve setVideoId of the moved item
            val movedSetVideoIdEntity = findSetVideoId(movedVideoId, ytPlaylistId)
            val movedSetVideoId = movedSetVideoIdEntity?.setVideoId ?: run {
                emit(LocalResource.Error("SetVideoId not found for moved item: $movedVideoId"))
                return@flow
            }

            // Resolve the successor (the item that should come AFTER the moved item)
            // If moving down (fromIndex < toIndex): successor is the item at toIndex + 1 (if exists)
            // If moving up (fromIndex > toIndex): successor is the item at toIndex
            val successorVideoId: String? = if (fromIndex < toIndex) {
                // Moving down: after removal, the successor is at toIndex + 1 in original list
                allPairs.getOrNull(toIndex + 1)?.songId // null: move to end
            } else {
                // Moving up: successor is the item currently at toIndex
                allPairs[toIndex].songId
            }
            // A successor that exists but has no setVideoId must not be sent as null: YouTube reads
            // a missing successor as "move to the end", so the song would land at the end on YouTube
            // while the app shows it where the user dropped it.
            val successorSetVideoId: String? =
                successorVideoId?.let { videoId ->
                    findSetVideoId(videoId, ytPlaylistId)?.setVideoId ?: run {
                        emit(LocalResource.Error("SetVideoId not found for successor item: $videoId"))
                        return@flow
                    }
                }

            // Step 1: Call YouTube API
            youTube
                .movePlaylistItem(
                    playlistId = ytPlaylistId,
                    setVideoId = movedSetVideoId,
                    movedSetVideoIdSuccessor = successorSetVideoId,
                ).onSuccess {
                    Logger.d(TAG, "moveItemInSyncedPlaylist: YouTube API success, status=$it")
                }.onFailure { e ->
                    Logger.e(TAG, "moveItemInSyncedPlaylist: YouTube API failed: ${e.message}")
                    emit(LocalResource.Error("Failed to reorder on YouTube: ${e.message}"))
                    return@flow
                }

            // Step 2: Update local DB positions — read, shift and place as one transaction. The API
            // call above stays outside it: a DB lock must never be held while waiting on the network.
            if (!localDataSource.moveSongInPlaylist(playlistId, fromIndex, toIndex)) {
                emit(LocalResource.Error("Index out of bounds"))
                return@flow
            }

            emit(LocalResource.Success("Position updated"))
        }.flowOn(Dispatchers.IO)

    /**
     * Move a song within an unsynced local playlist.
     * Same position bookkeeping as [moveItemInSyncedPlaylist], without the YouTube API call:
     * shift the items between from/to by one, then place the moved item at the target position.
     */
    override fun moveItemInLocalPlaylist(
        playlistId: Long,
        fromIndex: Int,
        toIndex: Int,
    ): Flow<LocalResource<String>> =
        flow<LocalResource<String>> {
            if (fromIndex == toIndex) {
                emit(LocalResource.Success("No change"))
                return@flow
            }
            emit(LocalResource.Loading())

            if (!localDataSource.moveSongInPlaylist(playlistId, fromIndex, toIndex)) {
                emit(LocalResource.Error("Index out of bounds"))
                return@flow
            }

            emit(LocalResource.Success("Position updated"))
        }.flowOn(Dispatchers.IO)
}