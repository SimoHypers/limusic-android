package com.maxrave.kotlinytmusicscraper.models.subscriptionButton

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscribeButtonRenderer(
    @SerialName("longSubscriberCountText")
    val longSubscriberCountText: LongSubscriberCountText,
    val subscribed: Boolean? = null,
    // Not always the artist's browseId: subscribe and unsubscribe go to this one.
    val channelId: String? = null,
)