package com.simohypers.limusic.kotlinytmusicscraper.pages

import com.simohypers.limusic.kotlinytmusicscraper.models.AlbumItem
import com.simohypers.limusic.kotlinytmusicscraper.models.Artist
import com.simohypers.limusic.kotlinytmusicscraper.models.SectionListRenderer
import com.simohypers.limusic.kotlinytmusicscraper.models.response.BrowseResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AlbumAudioShelfTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    private val iceman =
        AlbumItem(
            browseId = "MPREb_LjIW4hKEFIk",
            playlistId = "OLAK5uy_lcDE9n6x4ySCRHAQ3xhqEAeIQQXpusa4A",
            title = "ICEMAN",
            artists = listOf(Artist("Drake", "UCU6cE7pdJPc6DU2jSrKEsdQ")),
            thumbnail = "",
        )

    @Test
    fun theAlbumsAudioShelfParsesAsAudioRows() {
        val sections =
            json.decodeFromString<BrowseResponse>(FIXTURE)
                .contents
                ?.twoColumnBrowseResultsRenderer
                ?.secondaryContents
                ?.sectionListRenderer
                ?.contents

        val songs = albumAudioShelf(sections, iceman)

        assertEquals(listOf("spjUd9KtIbo"), songs.map { it.id })
        assertEquals(listOf("Dust"), songs.map { it.title })
        assertEquals(listOf(190), songs.map { it.duration })
        assertEquals(listOf("MUSIC_VIDEO_TYPE_ATV"), songs.map { it.musicVideoType })
    }

    @Test
    fun aPageWithoutAnAudioShelfYieldsNoRows() {
        val sections =
            listOf(
                SectionListRenderer.Content(
                    musicCarouselShelfRenderer = null,
                    musicShelfRenderer = null,
                    musicCardShelfRenderer = null,
                    musicPlaylistShelfRenderer = null,
                    musicDescriptionShelfRenderer = null,
                    musicResponsiveHeaderRenderer = null,
                    musicEditablePlaylistDetailHeaderRenderer = null,
                    gridRenderer = null,
                ),
            )

        assertEquals(emptyList(), albumAudioShelf(sections, iceman))
    }

    private companion object {
        val FIXTURE =
            """
{
 "responseContext": {},
 "contents": {
  "twoColumnBrowseResultsRenderer": {
   "secondaryContents": {
    "sectionListRenderer": {
     "contents": [
      {
       "musicPlaylistShelfRenderer": {
        "collapsedItemCount": 18,
        "contents": [
         {
          "musicResponsiveListItemRenderer": {
           "flexColumns": [
            {
             "musicResponsiveListItemFlexColumnRenderer": {
              "text": {
               "runs": [
                {
                 "text": "Dust",
                 "navigationEndpoint": {
                  "watchEndpoint": {
                   "videoId": "spjUd9KtIbo",
                   "playlistId": "OLAK5uy_lcDE9n6x4ySCRHAQ3xhqEAeIQQXpusa4A",
                   "watchEndpointMusicSupportedConfigs": {
                    "watchEndpointMusicConfig": {
                     "musicVideoType": "MUSIC_VIDEO_TYPE_ATV"
                    }
                   }
                  }
                 }
                }
               ]
              }
             }
            },
            {
             "musicResponsiveListItemFlexColumnRenderer": {
              "text": {
               "runs": [
                {
                 "text": "Drake",
                 "navigationEndpoint": {
                  "browseEndpoint": {
                   "browseId": "UCU6cE7pdJPc6DU2jSrKEsdQ"
                  }
                 }
                }
               ]
              }
             }
            }
           ],
           "fixedColumns": [
            {
             "musicResponsiveListItemFixedColumnRenderer": {
              "text": {
               "runs": [
                {
                 "text": "3:10"
                }
               ]
              }
             }
            }
           ]
          }
         }
        ]
       }
      }
     ]
    }
   }
  }
 }
}
"""
    }
}
