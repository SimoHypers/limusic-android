package com.simohypers.limusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

// The Library chips' icons, from the same Material Symbols Rounded set as the rest of LimusicIcons,
// kept as the SVG path strings gstatic serves (960 viewport, y from -960) instead of converted
// builder calls.

val LimusicIcons.GridView: ImageVector by lazy { symbol("GridView", GRID_VIEW) }
val LimusicIcons.AutoAwesome: ImageVector by lazy { symbol("AutoAwesome", AUTO_AWESOME) }
val LimusicIcons.Bookmark: ImageVector by lazy { symbol("Bookmark", BOOKMARK) }
val LimusicIcons.Podcasts: ImageVector by lazy { symbol("Podcasts", PODCASTS) }

private fun symbol(
    name: String,
    pathData: String,
): ImageVector =
    ImageVector
        .Builder(name, 24.dp, 24.dp, 960f, 960f)
        .group(translationY = 960f) { addPath(addPathNodes(pathData), fill = SolidColor(Color.Black)) }
        .build()

private const val GRID_VIEW = "M200-520q-33 0-56.5-23.5T120-600v-160q0-33 23.5-56.5T200-840h160q33 0 56.5 23.5T440-760v160q0 33-23.5 56.5T360-520H200Zm0 400q-33 0-56.5-23.5T120-200v-160q0-33 23.5-56.5T200-440h160q33 0 56.5 23.5T440-360v160q0 33-23.5 56.5T360-120H200Zm400-400q-33 0-56.5-23.5T520-600v-160q0-33 23.5-56.5T600-840h160q33 0 56.5 23.5T840-760v160q0 33-23.5 56.5T760-520H600Zm0 400q-33 0-56.5-23.5T520-200v-160q0-33 23.5-56.5T600-440h160q33 0 56.5 23.5T840-360v160q0 33-23.5 56.5T760-120H600ZM200-600h160v-160H200v160Zm400 0h160v-160H600v160Zm0 400h160v-160H600v160Zm-400 0h160v-160H200v160Zm400-400Zm0 240Zm-240 0Zm0-240Z"

private const val AUTO_AWESOME = "m706-706-70-32q-6-3-8.5-8t-2.5-10q0-5 2.5-10t8.5-8l70-32 32-70q3-6 8-9t10-3q5 0 10 3t8 9l32 70 70 32q6 3 9 8t3 10q0 5-3 10t-9 8l-70 32-32 70q-3 6-8 8.5t-10 2.5q-5 0-10-2.5t-8-8.5l-32-70ZM260-380l-160-73q-9-4-13-11.5T83-480q0-8 4-15.5t13-11.5l160-73 73-160q4-9 11.5-13t15.5-4q8 0 15.5 4t11.5 13l73 160 160 73q9 4 13 11.5t4 15.5q0 8-4 15.5T620-453l-160 73-73 160q-4 9-11.5 13t-15.5 4q-8 0-15.5-4T333-220l-73-160Zm100 26 40-86 86-40-86-40-40-86-40 86-86 40 86 40 40 86Zm350 204-70-32q-6-3-9-8t-3-10q0-5 3-10t9-8l70-32 32-70q3-6 8-9t10-3q5 0 10 3t8 9l32 70 70 32q6 3 9 8t3 10q0 5-3 10t-9 8l-70 32-32 70q-3 6-8 9t-10 3q-5 0-10-3t-8-9l-32-70ZM360-480Z"

private const val BOOKMARK = "m480-240-168 72q-40 17-76-6.5T200-241v-519q0-33 23.5-56.5T280-840h400q33 0 56.5 23.5T760-760v519q0 43-36 66.5t-76 6.5l-168-72Zm0-88 200 86v-518H280v518l200-86Zm0-432H280h400-200Z"

private const val PODCASTS = "M440-120v-291q-18-11-29-28.5T400-480q0-33 23.5-56.5T480-560q33 0 56.5 23.5T560-480q0 23-11 41t-29 28v291q0 17-11.5 28.5T480-80q-17 0-28.5-11.5T440-120Zm-207-99q-12 12-29 12t-28-13q-45-53-70.5-119T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 75-25.5 141T784-220q-11 13-27.5 13.5T728-218q-11-11-11-28t11-30q34-42 53-94t19-110q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 58 19 109.5t54 93.5q11 13 11.5 29.5T233-219Zm113-113q-12 12-29 12.5T290-333q-23-31-36.5-68T240-480q0-100 70-170t170-70q100 0 170 70t70 170q0 42-13.5 79.5T670-333q-10 13-27 13.5T614-331q-11-11-11.5-28t9.5-31q13-20 20.5-42.5T640-480q0-66-47-113t-113-47q-66 0-113 47t-47 113q0 26 7.5 48t20.5 42q10 14 9.5 30.5T346-332Z"
