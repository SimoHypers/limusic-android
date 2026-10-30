package com.simohypers.limusic.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.simohypers.limusic.ui.theme.typo
import org.jetbrains.compose.resources.painterResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.app_icon

// "Moods & Genres" browse tiles, desktop's three (ui/src/routes/layout.css): the name top left,
// YouTube's colour for the category mixed into the card colour, and the category's cover on the
// right, sitting differently per section. [artworkUrl] is null until resolved (a browse per
// category, see SearchViewModel.loadMoodArtwork); every tile looks finished without it.

/** For you: the cover as a panel on the right fading into the card, and the app's mark. */
@Composable
fun ForYouMoodTile(
    title: String,
    color: Color,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    MoodTile(color, 136.dp, modifier, onClick) {
        if (artworkUrl != null) {
            MoodArt(
                artworkUrl,
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.58f)
                    .fillMaxHeight()
                    .fadeMask(Brush.horizontalGradient(0f to Color.Transparent, 0.7f to Color.Black)),
            )
        }
        MoodTitle(title, 20.sp, Modifier.align(Alignment.TopStart).fillMaxWidth(0.58f).padding(start = 18.dp, top = 16.dp))
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 16.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(Res.drawable.app_icon), contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

/** Moods & moments: the cover as a record half off the right edge, and a pill of the colour. */
@Composable
fun MoodsMoodTile(
    title: String,
    color: Color,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    MoodTile(color, 112.dp, modifier, onClick) { tint ->
        // The fade lives on this outer box, the record inside it, like desktop's disc and spin.
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 26.dp)
                .size(104.dp)
                .fadeMask(Brush.horizontalGradient(0f to Color.Transparent, 0.6f to Color.Black)),
        ) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(color.copy(alpha = 0.45f))) {
                if (artworkUrl != null) MoodArt(artworkUrl, Modifier.fillMaxSize())
                // The spindle hole, in the tile's own colour.
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(tint))
                }
            }
        }
        Column(
            Modifier.fillMaxHeight().fillMaxWidth(0.6f).padding(start = 16.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            MoodTitle(title, 17.sp)
            Box(Modifier.size(width = 28.dp, height = 5.dp).clip(CircleShape).background(color))
        }
    }
}

/** Genres: a tilted sleeve in the corner with the colour glowing up from behind it, and a dot. */
@Composable
fun GenreMoodTile(
    title: String,
    color: Color,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    MoodTile(color, 100.dp, modifier, onClick) {
        Box(
            Modifier.matchParentSize().drawBehind {
                // Desktop's radial-gradient(55% 95% at 100% 100%): an ellipse, so a circle squashed sideways.
                val corner = Offset(size.width, size.height)
                val radius = size.height * 0.95f
                scale(scaleX = size.width * 0.55f / radius, scaleY = 1f, pivot = corner) {
                    drawCircle(
                        Brush.radialGradient(0f to color.copy(alpha = 0.5f), 0.75f to Color.Transparent, center = corner, radius = radius),
                        radius = radius,
                        center = corner,
                    )
                }
            },
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 8.dp, y = 20.dp)
                .size(84.dp)
                .rotate(16f)
                .fadeMask(Brush.horizontalGradient(0f to Color.Transparent, 0.55f to Color.Black))
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.45f)),
        ) {
            if (artworkUrl != null) MoodArt(artworkUrl, Modifier.fillMaxSize())
        }
        Row(Modifier.fillMaxWidth(0.62f).padding(start = 16.dp, top = 14.dp)) {
            // On the first line, so a name that wraps hangs under itself, not under the dot.
            Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            MoodTitle(title, 15.5.sp)
        }
    }
}

/** The family's frame: tinted card, hairline border, and a slight press-in where desktop has hover. */
@Composable
private fun MoodTile(
    color: Color,
    height: Dp,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.(tint: Color) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val tint = lerp(MaterialTheme.colorScheme.surfaceContainerHigh, color, 0.14f)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(150), label = "moodPress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.clip(shape)
            .background(tint)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), shape)
            .clickable(interaction, ripple(), onClick = onClick),
    ) {
        content(tint)
    }
}

@Composable
private fun MoodTitle(
    title: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        // Desktop's tile title: the heading face, bold, tight lines.
        style = typo().headlineMedium.copy(fontSize = size, lineHeight = size * 1.15f),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun MoodArt(
    url: String,
    modifier: Modifier,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(true).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

/** CSS mask-image: what is drawn keeps [brush]'s alpha. */
private fun Modifier.fadeMask(brush: Brush) =
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent()
        drawRect(brush, blendMode = BlendMode.DstIn)
    }
