package com.simohypers.limusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Material's "computer": a screen on a stand. The desktop in the Devices list and remote card. */
val SimpIcons.Computer: ImageVector
    get() {
        _Computer?.let { return it }
        return ImageVector
            .Builder(name = "Computer", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                    moveTo(20f, 18f)
                    curveToRelative(1.1f, 0f, 1.99f, -0.9f, 1.99f, -2f)
                    lineTo(22f, 6f)
                    curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                    lineTo(4f, 4f)
                    curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                    verticalLineToRelative(10f)
                    curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                    lineTo(0f, 18f)
                    verticalLineToRelative(2f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(-4f)
                    close()
                    moveTo(4f, 6f)
                    horizontalLineToRelative(16f)
                    verticalLineToRelative(10f)
                    lineTo(4f, 16f)
                    lineTo(4f, 6f)
                    close()
                }
            }.build()
            .also { _Computer = it }
    }

private var _Computer: ImageVector? = null
