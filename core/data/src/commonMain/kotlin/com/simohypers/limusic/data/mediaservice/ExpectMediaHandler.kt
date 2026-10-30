package com.simohypers.limusic.data.mediaservice

import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.mediaservice.handler.MediaPlayerHandler
import com.simohypers.limusic.domain.repository.LocalPlaylistRepository
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.domain.repository.StreamRepository
import kotlinx.coroutines.CoroutineScope

expect fun createMediaServiceHandler(
    dataStoreManager: DataStoreManager,
    songRepository: SongRepository,
    streamRepository: StreamRepository,
    localPlaylistRepository: LocalPlaylistRepository,
    coroutineScope: CoroutineScope,
): MediaPlayerHandler