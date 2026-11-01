package com.simohypers.limusic.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CenterLoadingBox(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        LimusicLoader(Modifier.size(48.dp))
    }
}

/** The note from the logo, in the logo's 200x200 viewport. */
private const val NOTE =
    "M116.513 114.139C115.69 118.653 113.869 122.903 111.196 126.545C108.522 130.187 105.071 133.121 101.12 135.109" +
        "C97.1688 137.098 92.8279 138.086 88.4464 137.994C84.0648 137.902 79.7649 136.734 75.8926 134.581" +
        "C72.0202 132.429 68.6834 129.354 66.1507 125.603C63.6179 121.851 61.9598 117.529 61.3098 112.983" +
        "C60.6598 108.438 61.0359 103.795 62.408 99.4298C63.7801 95.0643 66.1099 91.0975 69.2099 87.8484" +
        "C74.4593 82.3408 81.5795 79.2458 89.0045 79.2441C94.2541 79.2441 98.1038 80.4187 103.003 83.1351" +
        "C103.003 83.1351 104.053 83.7959 104.753 84.3832C104.053 81.8136 103.157 74.8685 103.003 71.9025" +
        "C102.863 66.9239 102.805 61.9432 102.828 56.9625V34.9011C102.828 31.0835 105.852 28 109.583 28" +
        "C110.304 28 111.025 28.1175 111.718 28.3524L143.72 40.7523C146.841 41.8168 147.898 42.4409 148.934 43.3439" +
        "C149.97 44.2395 150.74 45.3261 151.244 46.6182C151.755 47.9103 152 49.129 152 52.4988V61.015" +
        "C152 64.8179 148.899 67.9161 145.175 67.9161C144.45 67.9144 143.73 67.793 143.041 67.5563L133.388 64.2747" +
        "L132.331 63.9076C129.273 62.8798 128.006 62.4393 126.816 61.9988C125.594 61.5537 124.393 61.0465 123.218 60.4791" +
        "C122.713 60.2349 122.216 59.9729 121.727 59.6935C121.041 59.3264 120.068 58.7832 118.08 57.7627" +
        "L117.002 57.2194L117.079 108.61C117.079 110.482 116.883 112.333 116.506 114.139H116.513Z"

/** The arc's circumference in the 200 viewport (r = 80), against which its 60 to 300 long dash is measured. */
private const val ARC_LENGTH = 502.65f

/**
 * Limusic's loader, from the logo: the green rind as an arc that spins and grows and shrinks, round
 * a note whose outline traces itself, fills, then fades and starts again. One [color] for both.
 */
@Composable
fun LimusicLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val transition = rememberInfiniteTransition(label = "loader")
    val turn = transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "turn")
    val phase = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "phase")
    val note = remember { PathParser().parsePathString(NOTE).toPath() }
    val measure = remember { PathMeasure().apply { setPath(note, false) } }
    val traced = remember { Path() }
    Spacer(
        modifier.progressSemantics().drawBehind {
            val s = size.minDimension / 200f
            val p = phase.value
            val swell = EaseInOut.transform(if (p < .5f) p * 2 else 2 - p * 2)
            drawArc(
                color = color,
                startAngle = turn.value,
                sweepAngle = (60f + 240f * swell) / ARC_LENGTH * 360f,
                useCenter = false,
                topLeft = Offset(center.x - 80 * s, center.y - 80 * s),
                size = Size(160 * s, 160 * s),
                style = Stroke(18 * s, cap = StrokeCap.Round),
            )
            // Outline traces over the first 40%, fill comes in from 30% to 48%, both fade from 75%.
            val fade = if (p < .75f) 1f else 1f - EaseInOut.transform((p - .75f) / .25f)
            val fill = if (p < .3f) 0f else if (p < .48f) EaseInOut.transform((p - .3f) / .18f) else fade
            traced.reset()
            measure.getSegment(0f, measure.length * EaseInOut.transform((p / .4f).coerceAtMost(1f)), traced, true)
            translate(center.x, center.y) {
                scale(.66f * s, Offset.Zero) {
                    translate(-106.5f, -83f) {
                        drawPath(traced, color, alpha = fade, style = Stroke(9f, join = StrokeJoin.Round))
                        drawPath(note, color, alpha = fill)
                    }
                }
            }
        },
    )
}

/**
 * Says what the app is fetching behind a menu that has already closed, such as Starting radio.
 * Only a wait that lasts shows, so a quick one doesn't flash.
 */
@Composable
fun BusyPill(modifier: Modifier = Modifier) {
    val label = BaseViewModel.busyLabels.collectAsStateWithLifecycle().value.lastOrNull()
    var visible by remember { mutableStateOf(false) }
    // Kept while the pill fades out.
    var text by remember { mutableStateOf<StringResource?>(null) }
    LaunchedEffect(label) {
        if (label == null) {
            visible = false
            return@LaunchedEffect
        }
        if (!visible) delay(300)
        text = label
        visible = true
    }
    AnimatedVisibility(visible, modifier, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut()) {
        Row(
            Modifier
                .shadow(6.dp, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LimusicLoader(Modifier.size(20.dp))
            text?.let { Text(stringResource(it), style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface) }
        }
    }
}
