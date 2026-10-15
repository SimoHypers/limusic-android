package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.ui.theme.typo

/**
 * A "Moods & Genres" browse category tile, tinted with the category's own colour over the card
 * colour the way Limusic desktop draws its tiles, with the tilted cover in the corner.
 *
 * [artworkUrl] is null until resolved: the category list carries no artwork at all, so the cover
 * costs a separate browse per category and arrives late. The tile looks finished without it.
 */
@Composable
fun MoodCategoryCard(
    title: String,
    color: Color,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .clip(shape)
                .background(lerp(MaterialTheme.colorScheme.surfaceContainerHigh, color, 0.14f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), shape)
                .clickable(onClick = onClick),
    ) {
        if (artworkUrl != null) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        // Offset past the corner first, THEN rotate: the tile clips its children,
                        // so letting the cover run off the edge is what produces the cut-off
                        // diagonal instead of a square pasted inside the card.
                        .offset(x = 8.dp, y = 12.dp)
                        .size(64.dp)
                        .rotate(25f)
                        .clip(RoundedCornerShape(2.dp)),
            )
        }
        Text(
            text = title,
            // Desktop's tile title: the heading face, bold.
            style = typo().titleLarge.copy(fontSize = 16.sp, lineHeight = 19.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    // Safe width ends at the LEFT edge of the tilted cover, not just clear of the
                    // badge. The cover is 64.dp rotated 25°, so its bounding box grows to
                    // 64*(cos25+sin25) ≈ 85.dp — about 10.dp wider on each side — and it is offset
                    // 8.dp past the right edge. That leaves ~66.dp of it inside the tile.
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 72.dp),
        )
    }
}
