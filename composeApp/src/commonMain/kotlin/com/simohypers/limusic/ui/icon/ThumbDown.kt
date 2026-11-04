package com.simohypers.limusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Material Symbols thumb_down, filled and unfilled, as media3-session ships them (Apache 2.0).
// One pair, so the Dislike row only fills in when pressed instead of changing shape.
val LimusicIcons.ThumbDown: ImageVector by lazy {
    thumbDown(
        "ThumbDown",
        "M240,120L640,120L640,640L360,920L310,870Q303,863 298.5,851Q294,839 294,828L294,814L338,640L120,640Q88,640 64,616Q40,592 40,560L40,480Q40,473 41.5,465Q43,457 46,450L166,168Q175,148 196,134Q217,120 240,120ZM720,640L720,120L880,120L880,640L720,640Z",
    )
}

val LimusicIcons.ThumbDownOutlined: ImageVector by lazy {
    thumbDown(
        "ThumbDownOutlined",
        "M240,120L680,120L680,640L400,920L350,870Q343,863 338.5,851Q334,839 334,828L334,814L378,640L120,640Q88,640 64,616Q40,592 40,560L40,480Q40,473 42,465Q44,457 46,450L166,168Q175,148 196,134Q217,120 240,120ZM600,200L240,200Q240,200 240,200Q240,200 240,200L120,480L120,560Q120,560 120,560Q120,560 120,560L480,560L426,780L600,606L600,200ZM600,606L600,606L600,560L600,560Q600,560 600,560Q600,560 600,560L600,480L600,200Q600,200 600,200Q600,200 600,200L600,200L600,606ZM680,640L680,560L800,560L800,200L680,200L680,120L880,120L880,640L680,640Z",
    )
}

private fun thumbDown(
    name: String,
    pathData: String,
) = ImageVector
    .Builder(name, 24.dp, 24.dp, 960f, 960f)
    .addPath(addPathNodes(pathData), fill = SolidColor(Color.Black))
    .build()
