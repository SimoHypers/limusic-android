package com.simohypers.limusic.data.di

import com.simohypers.limusic.common.AppIdentity
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.data.listentogether.DeviceLinkBridge
import com.simohypers.limusic.data.listentogether.DeviceLinkRepositoryImpl
import com.simohypers.limusic.data.listentogether.ListenTogetherPlaybackBridge
import com.simohypers.limusic.data.listentogether.ListenTogetherPrefs
import com.simohypers.limusic.data.listentogether.ListenTogetherRepositoryImpl
import com.simohypers.limusic.data.listentogether.elapsedRealtime
import com.simohypers.limusic.domain.repository.DeviceLinkRepository
import com.simohypers.limusic.domain.repository.ListenTogetherRepository
import com.simohypers.limusic.domain.manager.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.qualifier.named
import org.koin.dsl.module
import com.simohypers.limusic.listentogether.DeviceLinkSession
import com.simohypers.limusic.listentogether.ListenTogetherClient
import com.simohypers.limusic.listentogether.ListenTogetherSession

/**
 * Listen Together lives for as long as the app does, not as long as its screen.
 *
 * The session was originally created inside the ViewModel, which meant leaving the screen tore the
 * socket down and left the room — while playback, the thing the room is about, kept running in the
 * background. Singletons here, and the screen merely observes them.
 */
val listenTogetherModule =
    module {
        single {
            ListenTogetherClient(
                userAgent = get<AppIdentity>().userAgent,
                // Read per connection attempt, so a server changed in settings applies next time.
                serverUrl = {
                    runBlocking { get<DataStoreManager>().getString(ListenTogetherPrefs.SERVER_URL).first().orEmpty() }
                },
                elapsedRealtime = ::elapsedRealtime,
            )
        }

        single { ListenTogetherSession(client = get()) }

        // The boundary: everything above this line speaks domain types only.
        single<ListenTogetherRepository> {
            ListenTogetherRepositoryImpl(
                session = get(),
                scope = get<CoroutineScope>(named(Config.SERVICE_SCOPE)),
            )
        }

        // createdAtStart: the bridge has to be listening before the user joins anything, otherwise
        // the first track change of a room is missed and the guest sits in silence.
        single(createdAtStart = true) {
            ListenTogetherPlaybackBridge(
                repository = get(),
                session = get(),
                handler = get(LOCAL_PLAYER_HANDLER),
                scope = get<CoroutineScope>(named(Config.SERVICE_SCOPE)),
            ).also { it.start() }
        }

        // The device link (phone remote + Continue here): its own socket, apart from Listen
        // Together's. `createdAtStart` does nothing for a module loaded this way (see Loader.kt):
        // MainActivity's onStart injects the repository, which builds and starts the bridge.
        single { DeviceLinkSession(userAgent = get<AppIdentity>().userAgent, elapsedRealtime = ::elapsedRealtime) }
        single {
            DeviceLinkBridge(
                session = get(),
                handler = get(LOCAL_PLAYER_HANDLER),
                dataStore = get(),
                scope = get<CoroutineScope>(named(Config.SERVICE_SCOPE)),
            ).also { it.start() }
        }
        single<DeviceLinkRepository> {
            DeviceLinkRepositoryImpl(
                session = get(),
                bridge = get(),
                dataStore = get(),
                handler = get(LOCAL_PLAYER_HANDLER),
                scope = get<CoroutineScope>(named(Config.SERVICE_SCOPE)),
            )
        }
    }
