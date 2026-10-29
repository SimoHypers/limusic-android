package com.maxrave.data.di

import com.maxrave.common.Config
import com.maxrave.data.listentogether.LinkAwareMediaPlayerHandler
import com.maxrave.data.mediaservice.createMediaServiceHandler
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import kotlinx.coroutines.CoroutineScope
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** This phone's own player. The app sees [LinkAwareMediaPlayerHandler] over it; what plays here asks for this one. */
val LOCAL_PLAYER_HANDLER = named("local_player_handler")

val mediaHandlerModule =
    module {
        single<MediaPlayerHandler>(LOCAL_PLAYER_HANDLER, createdAtStart = true) {
            createMediaServiceHandler(
                dataStoreManager = get(),
                songRepository = get(),
                streamRepository = get(),
                localPlaylistRepository = get(),
                coroutineScope = get<CoroutineScope>(named(Config.SERVICE_SCOPE)),
            )
        }
        single<MediaPlayerHandler>(createdAtStart = true) {
            LinkAwareMediaPlayerHandler(
                local = get(LOCAL_PLAYER_HANDLER),
                deviceLink = get(),
                listenTogether = get(),
                songRepository = get(),
                scope = get<CoroutineScope>(named(Config.SERVICE_SCOPE)),
            )
        }
    }
