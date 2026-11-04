package com.simohypers.limusic.di

import com.simohypers.limusic.viewModel.DeviceLinkViewModel
import com.simohypers.limusic.viewModel.AlbumViewModel
import com.simohypers.limusic.viewModel.ListenTogetherViewModel
import com.simohypers.limusic.viewModel.ArtistViewModel
import com.simohypers.limusic.viewModel.BrowseViewModel
import com.simohypers.limusic.viewModel.HomeViewModel
import com.simohypers.limusic.viewModel.ImportViewModel
import com.simohypers.limusic.viewModel.LibraryDynamicPlaylistViewModel
import com.simohypers.limusic.viewModel.LibraryViewModel
import com.simohypers.limusic.viewModel.LocalPlaylistViewModel
import com.simohypers.limusic.viewModel.MoodViewModel
import com.simohypers.limusic.viewModel.MoreAlbumsViewModel
import com.simohypers.limusic.viewModel.NotificationViewModel
import com.simohypers.limusic.viewModel.NowPlayingBottomSheetViewModel
import com.simohypers.limusic.viewModel.PlaylistViewModel
import com.simohypers.limusic.viewModel.PodcastViewModel
import com.simohypers.limusic.viewModel.RecentlySongsViewModel
import com.simohypers.limusic.viewModel.SearchViewModel
import com.simohypers.limusic.viewModel.AutoEqViewModel
import com.simohypers.limusic.viewModel.SettingsViewModel
import com.simohypers.limusic.viewModel.SharedViewModel
import com.simohypers.limusic.viewModel.SongSelectionViewModel
import com.simohypers.limusic.viewModel.YouTubeAccounts
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        single { YouTubeAccounts(get(), get(), get()) }
        single {
            SharedViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        single {
            SearchViewModel(
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            SongSelectionViewModel(
                get(),
                get(),
            )
        }
        // Only one of these resolves on a given platform: Desktop binds the host repository, Android the
        // sender. Koin resolves dependencies when a ViewModel is created, not when it is declared.
        viewModel {
            NowPlayingBottomSheetViewModel(
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryDynamicPlaylistViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            ImportViewModel(
                get(),
            )
        }
        viewModel {
            AlbumViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            HomeViewModel(
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            AutoEqViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            SettingsViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            ArtistViewModel(
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            PlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            PodcastViewModel(
                get(),
            )
        }
        viewModel {
            MoreAlbumsViewModel(
                get(),
            )
        }
        viewModel {
            BrowseViewModel(
                get(),
            )
        }
        viewModel {
            RecentlySongsViewModel(
                get(),
            )
        }
        viewModel {
            LocalPlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            NotificationViewModel(
                get(),
            )
        }
        viewModel {
            MoodViewModel(
                get(),
                get(),
            )
        }
        viewModel { DeviceLinkViewModel(repository = get()) }
        viewModel {
            ListenTogetherViewModel(
                repository = get(),
                dataStore = get(),
                bridge = get(),
            )
        }

    }