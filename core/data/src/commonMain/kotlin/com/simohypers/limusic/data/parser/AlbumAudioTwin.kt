package com.simohypers.limusic.data.parser

import com.simohypers.limusic.domain.data.model.browse.album.AlbumBrowse
import com.simohypers.limusic.kotlinytmusicscraper.models.SongItem

internal fun AlbumBrowse.withSongVersions(audio: List<SongItem>): AlbumBrowse = this
