package com.maxrave.kotlinytmusicscraper.models

import kotlinx.serialization.Serializable

@Serializable
data class Button(
    val buttonRenderer: ButtonRenderer? = null,
    val menuRenderer: Menu.MenuRenderer? = null,
    val toggleButtonRenderer: ToggleButtonRenderer? = null,
) {
    /** An album or playlist header's save-to-library button: [isToggled] is whether it is saved. */
    @Serializable
    data class ToggleButtonRenderer(
        val isToggled: Boolean? = null,
        val defaultServiceEndpoint: ServiceEndpoint? = null,
    ) {
        @Serializable
        data class ServiceEndpoint(
            val likeEndpoint: LikeEndpoint? = null,
        )

        @Serializable
        data class LikeEndpoint(
            val target: Target? = null,
        )

        @Serializable
        data class Target(
            val playlistId: String? = null,
        )
    }

    @Serializable
    data class ButtonRenderer(
        val text: Runs? = null,
        val navigationEndpoint: NavigationEndpoint?,
        val command: NavigationEndpoint?,
        val icon: Icon?,
    )
}
/** Whether the header's save-to-library toggle reads saved; null when the header has none (your own playlist). */
fun List<Button>?.savedToLibrary(): Boolean? =
    this
        ?.firstNotNullOfOrNull { it.toggleButtonRenderer?.takeIf { t -> t.defaultServiceEndpoint?.likeEndpoint != null } }
        ?.isToggled
