package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** The translucent pill the detail screens and players put their floating buttons on. */
@Composable
fun Modifier.pillSurface(shape: Shape = CircleShape): Modifier =
    this
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))

/** A [pillSurface] wrapping arbitrary [content] (e.g. a row of icon buttons). */
@Composable
fun PillContainer(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.pillSurface(shape),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/** A single icon on a [pillSurface] (e.g. the circular back button shared by the detail screens). */
@Composable
fun PillIconButton(
    imageVector: ImageVector,
    modifier: Modifier = Modifier.size(48.dp),
    shape: Shape = CircleShape,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    PillContainer(modifier = modifier, shape = shape) {
        RippleIconButton(
            imageVector = imageVector,
            tint = tint,
            onClick = onClick,
        )
    }
}
