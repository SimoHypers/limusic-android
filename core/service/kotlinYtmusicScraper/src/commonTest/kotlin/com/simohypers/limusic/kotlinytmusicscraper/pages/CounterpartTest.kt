package com.simohypers.limusic.kotlinytmusicscraper.pages

import com.simohypers.limusic.kotlinytmusicscraper.models.PlaylistPanelRenderer
import com.simohypers.limusic.kotlinytmusicscraper.models.TwinSegment
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** A wrapped queue row carries its other version and the segmentMap between the two (desktop #535). */
class CounterpartTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    // "Locked Away", trimmed from desktop's next_wrapper_two_segments.json fixture (real response).
    private val row =
        json.decodeFromString<PlaylistPanelRenderer.Content>(
            """
{
 "playlistPanelVideoWrapperRenderer": {
  "primaryRenderer": {
   "playlistPanelVideoRenderer": {
    "title": {
     "runs": [
      {
       "text": "Locked Away (feat. Adam Levine)"
      }
     ]
    },
    "longBylineText": {
     "runs": [
      {
       "text": "R. City",
       "navigationEndpoint": {
        "browseEndpoint": {
         "browseId": "UC6Ilv4j2Gbnz5LXmn3QgzGA",
         "browseEndpointContextSupportedConfigs": {
          "browseEndpointContextMusicConfig": {
           "pageType": "MUSIC_PAGE_TYPE_ARTIST"
          }
         }
        }
       }
      },
      {
       "text": " \u2022 "
      },
      {
       "text": "What Dreams Are Made Of",
       "navigationEndpoint": {
        "browseEndpoint": {
         "browseId": "MPREb_ZXGTJbnoFA2",
         "browseEndpointContextSupportedConfigs": {
          "browseEndpointContextMusicConfig": {
           "pageType": "MUSIC_PAGE_TYPE_ALBUM"
          }
         }
        }
       }
      },
      {
       "text": " \u2022 "
      },
      {
       "text": "2015"
      }
     ]
    },
    "thumbnail": {
     "thumbnails": [
      {
       "url": "https://yt3.googleusercontent.com/DQpsXibLwW_4OYu1t2kUgcmtva96FBKs4-QUr0aFu_odHEgZcA5sUBBEBuSxdEgofs1Yu-yyM5T4ogyk=w544-h544-l90-rj",
       "width": 544,
       "height": 544
      }
     ]
    },
    "lengthText": {
     "runs": [
      {
       "text": "3:48"
      }
     ],
     "accessibility": {
      "accessibilityData": {
       "label": "3 minutes, 48 seconds"
      }
     }
    },
    "navigationEndpoint": {
     "watchEndpoint": {
      "videoId": "wTR_tymnbac",
      "watchEndpointMusicSupportedConfigs": {
       "watchEndpointMusicConfig": {
        "hasPersistentPlaylistPanel": true,
        "musicVideoType": "MUSIC_VIDEO_TYPE_ATV"
       }
      }
     }
    },
    "videoId": "wTR_tymnbac",
    "selected": false
   }
  },
  "counterpart": [
   {
    "counterpartRenderer": {
     "playlistPanelVideoRenderer": {
      "title": {
       "runs": [
        {
         "text": "Locked Away (Official Video) (feat. Adam Levine)"
        }
       ]
      },
      "longBylineText": {
       "runs": [
        {
         "text": "R. City",
         "navigationEndpoint": {
          "browseEndpoint": {
           "browseId": "UC6Ilv4j2Gbnz5LXmn3QgzGA",
           "browseEndpointContextSupportedConfigs": {
            "browseEndpointContextMusicConfig": {
             "pageType": "MUSIC_PAGE_TYPE_ARTIST"
            }
           }
          }
         }
        },
        {
         "text": " \u2022 "
        },
        {
         "text": "1.1B views"
        },
        {
         "text": " \u2022 "
        },
        {
         "text": "6.4M likes"
        }
       ]
      },
      "thumbnail": {
       "thumbnails": [
        {
         "url": "https://i.ytimg.com/vi/6GUm5g8SG4o/hq720.jpg?sqp=-oaymwEKCNUGEN8DIABIWg&rs=AMzJL3mrsigf9xlTJd94hdY5FAmfCrTxQQ",
         "width": 853,
         "height": 479
        }
       ]
      },
      "lengthText": {
       "runs": [
        {
         "text": "4:26"
        }
       ],
       "accessibility": {
        "accessibilityData": {
         "label": "4 minutes, 26 seconds"
        }
       }
      },
      "navigationEndpoint": {
       "watchEndpoint": {
        "videoId": "6GUm5g8SG4o",
        "watchEndpointMusicSupportedConfigs": {
         "watchEndpointMusicConfig": {
          "hasPersistentPlaylistPanel": true,
          "musicVideoType": "MUSIC_VIDEO_TYPE_OMV"
         }
        }
       }
      },
      "videoId": "6GUm5g8SG4o",
      "selected": false
     }
    },
    "segmentMap": {
     "segment": [
      {
       "primaryVideoStartTimeMilliseconds": "0",
       "counterpartVideoStartTimeMilliseconds": "22698",
       "durationMilliseconds": "86438"
      },
      {
       "primaryVideoStartTimeMilliseconds": "86439",
       "counterpartVideoStartTimeMilliseconds": "123644",
       "durationMilliseconds": "141061"
      }
     ]
    }
   }
  ]
 }
}
            """,
        )

    @Test
    fun aWrappedRowCarriesItsTwinAndTheMapBetweenThem() {
        val song = assertNotNull(NextPage.fromPlaylistPanelContent(row))
        assertEquals("wTR_tymnbac", song.id)
        val video = assertNotNull(song.counterpart)
        assertEquals("6GUm5g8SG4o", video.id)
        assertEquals("MUSIC_VIDEO_TYPE_OMV", video.musicVideoType)
        assertEquals(
            listOf(TwinSegment(0, 22698, 86438), TwinSegment(86439, 123644, 141061)),
            song.counterpartSegments,
        )
    }
}
