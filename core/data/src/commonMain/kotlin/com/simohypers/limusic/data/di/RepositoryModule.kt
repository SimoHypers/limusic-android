package com.simohypers.limusic.data.di

import com.simohypers.limusic.common.AppIdentity
import com.simohypers.limusic.common.Config.SERVICE_SCOPE
import com.simohypers.limusic.data.io.fileDir
import com.simohypers.limusic.data.repository.AccountRepositoryImpl
import com.simohypers.limusic.data.repository.AlbumRepositoryImpl
import com.simohypers.limusic.data.repository.ArtistRepositoryImpl
import com.simohypers.limusic.data.repository.AutoEqRepositoryImpl
import com.simohypers.limusic.data.lyrics.LyricsRomanizerRepositoryImpl
import com.simohypers.limusic.data.repository.CommonRepositoryImpl
import com.simohypers.limusic.data.repository.HomeRepositoryImpl
import com.simohypers.limusic.data.repository.ImportRepositoryImpl
import com.simohypers.limusic.data.repository.LibrarySync
import com.simohypers.limusic.data.repository.LocalPlaylistRepositoryImpl
import com.simohypers.limusic.data.repository.LyricsCanvasRepositoryImpl
import com.simohypers.limusic.data.repository.PlaylistRepositoryImpl
import com.simohypers.limusic.data.repository.PodcastRepositoryImpl
import com.simohypers.limusic.data.repository.SearchRepositoryImpl
import com.simohypers.limusic.data.repository.SongRepositoryImpl
import com.simohypers.limusic.data.repository.StreamRepositoryImpl
import com.simohypers.limusic.domain.repository.AccountRepository
import com.simohypers.limusic.domain.repository.AlbumRepository
import com.simohypers.limusic.domain.repository.ArtistRepository
import com.simohypers.limusic.domain.repository.AutoEqRepository
import com.simohypers.limusic.domain.repository.LyricsRomanizerRepository
import com.simohypers.limusic.domain.repository.CommonRepository
import com.simohypers.limusic.domain.repository.HomeRepository
import com.simohypers.limusic.domain.repository.ImportRepository
import com.simohypers.limusic.domain.repository.LocalPlaylistRepository
import com.simohypers.limusic.domain.repository.LyricsCanvasRepository
import com.simohypers.limusic.domain.repository.PlaylistRepository
import com.simohypers.limusic.domain.repository.PodcastRepository
import com.simohypers.limusic.domain.repository.SearchRepository
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.domain.repository.StreamRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoryModule =
    module {
        single<AccountRepository>(createdAtStart = true) {
            AccountRepositoryImpl(get(), get())
        }

        single { LibrarySync(get(), get()) }

        single<AlbumRepository>(createdAtStart = true) {
            AlbumRepositoryImpl(get(), get(), get())
        }

        single<ArtistRepository>(createdAtStart = true) {
            ArtistRepositoryImpl(get(), get(), get())
        }

        single<CommonRepository>(createdAtStart = true) {
            CommonRepositoryImpl(get(named(SERVICE_SCOPE)), get(), get(), get(), get()).apply {
                this.init("${fileDir()}/ytdlp-cookie.txt", get(), get<AppIdentity>().isDevBuild)
            }
        }

        // Lazy for the same reason its client is: the picker is the only thing that wants it.
        single<AutoEqRepository> {
            AutoEqRepositoryImpl(get(), get())
        }

        // Lazy: constructing it costs a few File.length() calls (plus, while the pack is missing,
        // one background read of the stored selection), but the kuromoji dictionary behind it is
        // loaded on first Japanese line, or right after a download installs it, and never at
        // startup — so this must NOT be createdAtStart, or every launch pays for a feature most
        // listeners leave off. The path is where Android keeps the downloaded ipadic pack (the
        // APK no longer bundles it); Desktop and iOS ignore it.
        single<LyricsRomanizerRepository> {
            LyricsRomanizerRepositoryImpl("${fileDir()}/kuromoji-ipadic", get())
        }

        single<HomeRepository>(createdAtStart = true) {
            HomeRepositoryImpl(get(), get())
        }

        single<ImportRepository>(createdAtStart = true) {
            ImportRepositoryImpl(get())
        }

        single<LocalPlaylistRepository>(createdAtStart = true) {
            LocalPlaylistRepositoryImpl(get(), get())
        }

        single<LyricsCanvasRepository>(createdAtStart = true) {
            LyricsCanvasRepositoryImpl(get(), get(), get(), get(), get(), get())
        }

        single<PlaylistRepository>(createdAtStart = true) {
            PlaylistRepositoryImpl(get(), get(), get(), get())
        }

        single<PodcastRepository>(createdAtStart = true) {
            PodcastRepositoryImpl(get(), get())
        }

        single<SearchRepository>(createdAtStart = true) {
            SearchRepositoryImpl(get(), get())
        }

        single<SongRepository>(createdAtStart = true) {
            SongRepositoryImpl(get(), get(), get(), get())
        }

        single<StreamRepository>(createdAtStart = true) {
            StreamRepositoryImpl(get(), get())
        }

    }