package com.maxrave.simpmusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Material's all_inclusive: the queue sheet's Autoplay divider, like desktop's infinity icon. */
val SimpIcons.AllInclusive: ImageVector get() = allInclusive

private val allInclusive: ImageVector by lazy {
    ImageVector
        .Builder(name = "AllInclusive", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData =
                addPathNodes(
                    "M18.6,6.62c-1.44,0 -2.8,0.56 -3.77,1.53L12,10.66 10.48,12h0.01L7.8,14.39c-0.64,0.64 -1.49,0.99 " +
                        "-2.4,0.99 -1.87,0 -3.39,-1.51 -3.39,-3.38S3.53,8.62 5.4,8.62c0.91,0 1.76,0.35 2.44,1.03l1.13,1 " +
                        "1.51,-1.34L9.22,8.2C8.2,7.18 6.84,6.62 5.4,6.62 2.42,6.62 0,9.04 0,12s2.42,5.38 5.4,5.38c1.44,0 " +
                        "2.8,-0.56 3.77,-1.53l2.83,-2.5 0.01,0.01L13.52,12h-0.01l2.69,-2.39c0.64,-0.64 1.49,-0.99 " +
                        "2.4,-0.99 1.87,0 3.39,1.51 3.39,3.38s-1.52,3.38 -3.39,3.38c-0.9,0 -1.76,-0.35 -2.44,-1.03l-1.14,-1.01 " +
                        "-1.51,1.34 1.27,1.12c1.02,1.01 2.37,1.57 3.82,1.57 2.98,0 5.4,-2.41 5.4,-5.38s-2.42,-5.37 -5.4,-5.37z",
                ),
            fill = SolidColor(Color.Black),
        ).build()
}
