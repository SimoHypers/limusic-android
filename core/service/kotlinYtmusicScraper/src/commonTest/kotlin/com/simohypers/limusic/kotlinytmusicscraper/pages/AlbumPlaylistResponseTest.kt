package com.simohypers.limusic.kotlinytmusicscraper.pages

import com.simohypers.limusic.kotlinytmusicscraper.models.response.BrowseResponse
import com.simohypers.limusic.kotlinytmusicscraper.parser.fromPlaylistToTrack
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AlbumPlaylistResponseTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Test
    fun theAlbumsAudioPlaylistParsesAsAudioRows() {
        val songs =
            json.decodeFromString<BrowseResponse>(FIXTURE)
                .fromPlaylistToTrack()

        assertEquals(listOf("spjUd9KtIbo"), songs.map { it.id })
        assertEquals(listOf("Dust"), songs.map { it.title })
        assertEquals(listOf(190), songs.map { it.duration })
        assertEquals(listOf("MUSIC_VIDEO_TYPE_ATV"), songs.map { it.musicVideoType })
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
