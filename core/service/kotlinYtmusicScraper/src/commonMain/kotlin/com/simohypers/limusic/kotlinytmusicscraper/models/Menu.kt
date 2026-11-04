package com.simohypers.limusic.kotlinytmusicscraper.models

import com.simohypers.limusic.kotlinytmusicscraper.models.response.LikeStatus
import kotlinx.serialization.Serializable

@Serializable
data class Menu(
    val menuRenderer: MenuRenderer,
) {
    @Serializable
    data class MenuRenderer(
        val items: List<Item>,
        val topLevelButtons: List<TopLevelButton>?,
    ) {
        @Serializable
        data class Item(
            val menuNavigationItemRenderer: MenuNavigationItemRenderer?,
            val menuServiceItemRenderer: MenuServiceItemRenderer?,
        ) {
            @Serializable
            data class MenuNavigationItemRenderer(
                val text: Runs,
                val icon: Icon,
                val navigationEndpoint: NavigationEndpoint,
            )

            @Serializable
            data class MenuServiceItemRenderer(
                val text: Runs,
                val icon: Icon,
                val serviceEndpoint: NavigationEndpoint,
            )
        }

        @Serializable
        data class TopLevelButton(
            val buttonRenderer: ButtonRenderer?,
            val likeButtonRenderer: LikeButtonRenderer? = null,
        ) {
            @Serializable
            data class LikeButtonRenderer(
                val likeStatus: String,
                val likesAllowed: Boolean,
            ) {
                fun toLikeStatus(): LikeStatus =
                    if (likesAllowed) {
                        when (likeStatus) {
                            "LIKE" -> LikeStatus.LIKE
                            "DISLIKE" -> LikeStatus.DISLIKE
                            else -> LikeStatus.INDIFFERENT
                        }
                    } else {
                        LikeStatus.INDIFFERENT
                    }
            }

            @Serializable
            data class ButtonRenderer(
                val icon: Icon,
                val navigationEndpoint: NavigationEndpoint,
            )
        }
    }
}

/**
 * The signed-in account's like on this row's track, off the row's own like button. Null when the
 * row has none (search rows, radio panels) or YouTube takes no like on it, so a row that states
 * nothing never reads as "not liked".
 */
fun Menu?.likeStatus(): LikeStatus? =
    this
        ?.menuRenderer
        ?.topLevelButtons
        ?.firstNotNullOfOrNull { it.likeButtonRenderer }
        ?.takeIf { it.likesAllowed }
        ?.toLikeStatus()
