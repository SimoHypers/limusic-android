package com.simohypers.limusic.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.RippleIconButton
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.KeyboardArrowDown
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.theme.rounded
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.updater.NoteBlock
import com.simohypers.limusic.updater.ReleaseNote
import com.simohypers.limusic.updater.Updater
import com.simohypers.limusic.updater.parseReleaseNotes
import com.simohypers.limusic.utils.VersionManager
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.whats_new
import limusic.composeapp.generated.resources.whats_new_empty
import limusic.composeapp.generated.resources.whats_new_failed
import limusic.composeapp.generated.resources.whats_new_installed
import limusic.composeapp.generated.resources.whats_new_version
import org.jetbrains.compose.resources.stringResource

private val INLINE = Regex("""\*\*(.+?)\*\*|`([^`]+)`|\[([^\]]+)]\(([^)]+)\)|(https?://\S+)|@([\w-]+)""")

private val DAY =
    LocalDate.Format {
        day()
        char(' ')
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        year()
    }

private sealed interface Notes {
    data object Loading : Notes

    data class Loaded(
        val releases: List<ReleaseNote>,
    ) : Notes

    data class Failed(
        val error: String,
    ) : Notes
}

/** Settings > About > What's new: the GitHub release notes, newest open, as desktop's changelog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(
    paddingValues: PaddingValues,
    navController: NavController,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    var notes by remember { mutableStateOf<Notes>(Notes.Loading) }
    LaunchedEffect(Unit) {
        notes = runCatching { Updater.releaseNotes() }.fold({ Notes.Loaded(it) }, { Notes.Failed(it.message ?: it.toString()) })
    }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.whats_new), style = typo().titleMedium) },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(LimusicIcons.ArrowBackIosNew, Modifier.size(32.dp), true, tint = onSurface) {
                        navController.navigateUp()
                    }
                }
            },
        )
        when (val n = notes) {
            Notes.Loading -> CenterLoadingBox(Modifier.fillMaxSize())
            is Notes.Failed -> Message(stringResource(Res.string.whats_new_failed, n.error))
            is Notes.Loaded ->
                if (n.releases.isEmpty()) {
                    Message(stringResource(Res.string.whats_new_empty))
                } else {
                    LazyColumn(
                        contentPadding =
                            PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = paddingValues.calculateBottomPadding() + 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(n.releases, key = { _, r -> r.version }) { i, release ->
                            ReleaseCard(release, initiallyOpen = i == 0)
                        }
                    }
                }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = typo().bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun ReleaseCard(
    release: ReleaseNote,
    initiallyOpen: Boolean,
) {
    var open by rememberSaveable(release.version) { mutableStateOf(initiallyOpen) }
    val turn by animateFloatAsState(if (open) 180f else 0f)
    val accent = MaterialTheme.colorScheme.primary
    Column(
        Modifier
            .fillMaxWidth()
            .clip(rounded(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(Res.string.whats_new_version, release.version),
                style = typo().titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (release.version == VersionManager.getVersionName()) {
                Text(
                    stringResource(Res.string.whats_new_installed),
                    style = typo().labelSmall,
                    color = accent,
                    modifier =
                        Modifier
                            .background(accent.copy(alpha = 0.15f), rounded(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Box(Modifier.weight(1f))
            Text(
                runCatching { LocalDate.parse(release.date).format(DAY) }.getOrDefault(release.date),
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                LimusicIcons.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).rotate(turn),
            )
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                parseReleaseNotes(release.body).forEach { NoteBlockView(it) }
            }
        }
    }
}

@Composable
private fun NoteBlockView(block: NoteBlock) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when (block) {
        is NoteBlock.Heading ->
            Text(
                inline(block.text),
                style = typo().titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
        is NoteBlock.Paragraph ->
            Text(inline(block.text), style = typo().bodyMedium, color = muted, modifier = Modifier.padding(top = 4.dp))
        is NoteBlock.Bullets ->
            Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                block.items.forEach { item ->
                    Row {
                        Text("•", style = typo().bodyMedium, color = muted, modifier = Modifier.padding(end = 8.dp))
                        Text(inline(item), style = typo().bodyMedium, color = muted)
                    }
                }
            }
        is NoteBlock.Code ->
            Text(
                block.text,
                style = typo().bodySmall,
                fontFamily = FontFamily.Monospace,
                color = muted,
                softWrap = false,
                modifier =
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, rounded(6.dp))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        is NoteBlock.Image ->
            AsyncImage(
                model = block.url,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth().clip(rounded(8.dp)),
            )
    }
}

/** Bold, code, links and @mentions inside one line, as desktop draws them. */
@Composable
private fun inline(text: String): AnnotatedString {
    val strong = MaterialTheme.colorScheme.onSurface
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val link = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary))
    return buildAnnotatedString {
        var last = 0
        for (m in INLINE.findAll(text)) {
            append(text.substring(last, m.range.first))
            val g = m.groupValues
            when {
                g[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = strong)) { append(g[1]) }
                g[2].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) { append(g[2]) }
                g[3].isNotEmpty() -> withLink(LinkAnnotation.Url(g[4], link)) { append(g[3]) }
                g[5].isNotEmpty() -> withLink(LinkAnnotation.Url(g[5], link)) { append(g[5]) }
                else -> withLink(LinkAnnotation.Url("https://github.com/${g[6]}", link)) { append("@${g[6]}") }
            }
            last = m.range.last + 1
        }
        append(text.substring(last))
    }
}
