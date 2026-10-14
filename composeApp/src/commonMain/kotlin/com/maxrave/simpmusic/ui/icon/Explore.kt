package com.maxrave.simpmusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val SimpIcons.Explore: ImageVector
  get() {
    if (_Explore != null) {
      return _Explore!!
    }
    _Explore =
      ImageVector.Builder(
          name = "Explore",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.38f, 16.25f)
            lineTo(13.43f, 14.8f)
            quadToRelative(0.5f, -0.15f, 0.86f, -0.51f)
            reflectiveQuadTo(14.8f, 13.43f)
            lineTo(16.25f, 8.38f)
            quadTo(16.33f, 8.1f, 16.11f, 7.89f)
            quadTo(15.9f, 7.68f, 15.63f, 7.75f)
            lineTo(10.58f, 9.2f)
            quadTo(10.08f, 9.35f, 9.71f, 9.71f)
            reflectiveQuadTo(9.2f, 10.58f)
            lineTo(7.75f, 15.63f)
            quadTo(7.68f, 15.9f, 7.89f, 16.11f)
            reflectiveQuadToRelative(0.49f, 0.14f)
            close()
            moveTo(12f, 13.5f)
            quadToRelative(-0.63f, 0f, -1.06f, -0.44f)
            reflectiveQuadTo(10.5f, 12f)
            reflectiveQuadToRelative(0.44f, -1.06f)
            reflectiveQuadTo(12f, 10.5f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(13.5f, 12f)
            reflectiveQuadToRelative(-0.44f, 1.06f)
            reflectiveQuadTo(12f, 13.5f)
            close()
            moveTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            close()
          }
        }
        .build()
    return _Explore!!
  }

private var _Explore: ImageVector? = null
