package com.simohypers.limusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Material's outlined flag, for reporting lyrics. */
val LimusicIcons.OutlinedFlag: ImageVector by lazy {
    ImageVector
        .Builder("OutlinedFlag", 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes("M14,6l-1,-2H5v17h2v-7h5l1,2h7V6h-6zM18,14h-4l-1,-2H7V6h5l1,2h5v6z"), fill = SolidColor(Color.Black))
        .build()
}
