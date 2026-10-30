package com.simohypers.limusic.data.di

import DatabaseDao
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.simohypers.limusic.data.dataStore.DataStoreManagerImpl
import com.simohypers.limusic.data.dataStore.createDataStoreInstance
import com.simohypers.limusic.data.db.Converters
import com.simohypers.limusic.data.db.MusicDatabase
import com.simohypers.limusic.data.db.datasource.LocalDataSource
import com.simohypers.limusic.data.db.getDatabaseBuilder
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.kotlinytmusicscraper.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module
import com.simohypers.limusic.lyrics.LyricsProviders
import com.simohypers.limusic.lyrics.SimpMusicLyricsClient
import kotlin.time.ExperimentalTime
import com.simohypers.limusic.autoeq.AutoEq

@OptIn(ExperimentalTime::class)
val databaseModule =
    module {
        single(createdAtStart = true) {
            Converters()
        }
        // Database
        single(createdAtStart = true) {
            getDatabaseBuilder(
                get<Converters>()
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        // DatabaseDao
        single(createdAtStart = true) {
            get<MusicDatabase>().getDatabaseDao()
        }
        // LocalDataSource
        single(createdAtStart = true) {
            LocalDataSource(get<DatabaseDao>(), get<MusicDatabase>())
        }
        // Datastore
        single(createdAtStart = true) {
            createDataStoreInstance()
        }
        // DatastoreManager
        single<DataStoreManager>(createdAtStart = true) {
            DataStoreManagerImpl(get<DataStore<Preferences>>())
        }

        // Move YouTube from Singleton to Koin DI
        single(createdAtStart = true) {
            YouTube()
        }

        single(createdAtStart = true) {
            SimpMusicLyricsClient()
        }

        single {
            LyricsProviders()
        }

        // Not created at start, unlike the rest: nothing needs it until someone opens the AutoEq
        // picker, and it holds an HTTP client the vast majority of sessions never use.
        single {
            AutoEq()
        }
    }