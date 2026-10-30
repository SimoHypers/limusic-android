package com.simohypers.limusic.data.mediaservice


actual fun createMediaServiceHandler(
    dataStoreManager: com.simohypers.limusic.domain.manager.DataStoreManager,
    songRepository: com.simohypers.limusic.domain.repository.SongRepository,
    streamRepository: com.simohypers.limusic.domain.repository.StreamRepository,
    localPlaylistRepository: com.simohypers.limusic.domain.repository.LocalPlaylistRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
): com.simohypers.limusic.domain.mediaservice.handler.MediaPlayerHandler =
    MediaServiceHandlerImpl(
        dataStoreManager,
        songRepository,
        streamRepository,
        localPlaylistRepository,
        coroutineScope,
    )