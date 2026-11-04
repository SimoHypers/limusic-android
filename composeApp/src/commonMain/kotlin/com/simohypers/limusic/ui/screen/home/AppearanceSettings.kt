package com.simohypers.limusic.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.hct.Hct
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.expect.ui.deleteFontFile
import com.simohypers.limusic.expect.ui.filePickerResult
import com.simohypers.limusic.expect.ui.importFontFile
import com.simohypers.limusic.expect.ui.isWallpaperDynamicColorSupported
import com.simohypers.limusic.expect.ui.platformDynamicColorScheme
import com.simohypers.limusic.ui.component.SettingGroup
import com.simohypers.limusic.ui.component.SettingItem
import com.simohypers.limusic.ui.component.SettingSheet
import com.simohypers.limusic.ui.icon.Add
import com.simohypers.limusic.ui.icon.Check
import com.simohypers.limusic.ui.icon.Close
import com.simohypers.limusic.ui.icon.Favorite
import com.simohypers.limusic.ui.icon.Pause
import com.simohypers.limusic.ui.icon.PlayArrow
import com.simohypers.limusic.ui.icon.SkipNext
import com.simohypers.limusic.ui.icon.SkipPrevious
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.Undo
import com.simohypers.limusic.ui.theme.AccentSwatches
import com.simohypers.limusic.ui.theme.AppFont
import com.simohypers.limusic.ui.theme.Appearance
import com.simohypers.limusic.ui.theme.DefaultNeutralHue
import com.simohypers.limusic.ui.theme.LocalArtworkColors
import com.simohypers.limusic.ui.theme.LocalForcedDarkColorScheme
import com.simohypers.limusic.ui.theme.LocalIsDarkTheme
import com.simohypers.limusic.ui.theme.fontFileLabel
import com.simohypers.limusic.ui.theme.ThemePreset
import com.simohypers.limusic.ui.theme.isDarkTheme
import com.simohypers.limusic.ui.theme.onAccent
import com.simohypers.limusic.ui.theme.parseThemeColorHex
import com.simohypers.limusic.ui.theme.rememberFontFamily
import com.simohypers.limusic.ui.theme.rememberThemeScheme
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.SharedViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.appearance_accent
import limusic.composeapp.generated.resources.appearance_accent_artwork_note
import limusic.composeapp.generated.resources.appearance_accent_hint
import limusic.composeapp.generated.resources.appearance_accent_theme
import limusic.composeapp.generated.resources.appearance_accent_wallpaper
import limusic.composeapp.generated.resources.appearance_colors
import limusic.composeapp.generated.resources.appearance_font_body
import limusic.composeapp.generated.resources.appearance_font_heading
import limusic.composeapp.generated.resources.appearance_font_load
import limusic.composeapp.generated.resources.appearance_font_load_failed
import limusic.composeapp.generated.resources.appearance_font_load_hint
import limusic.composeapp.generated.resources.appearance_font_remove
import limusic.composeapp.generated.resources.appearance_font_theme
import limusic.composeapp.generated.resources.appearance_interface
import limusic.composeapp.generated.resources.appearance_mode
import limusic.composeapp.generated.resources.appearance_mode_amoled
import limusic.composeapp.generated.resources.appearance_mode_hint
import limusic.composeapp.generated.resources.appearance_reset
import limusic.composeapp.generated.resources.appearance_reset_hint
import limusic.composeapp.generated.resources.appearance_roundness
import limusic.composeapp.generated.resources.appearance_roundness_hint
import limusic.composeapp.generated.resources.appearance_tint
import limusic.composeapp.generated.resources.appearance_tint_hint
import limusic.composeapp.generated.resources.appearance_tint_palette
import limusic.composeapp.generated.resources.appearance_your_fonts
import limusic.composeapp.generated.resources.artwork_colors
import limusic.composeapp.generated.resources.artwork_colors_description
import limusic.composeapp.generated.resources.custom_color
import limusic.composeapp.generated.resources.done
import limusic.composeapp.generated.resources.now_playing_style_apple_music
import limusic.composeapp.generated.resources.now_playing_style_spotify
import limusic.composeapp.generated.resources.requires_android_12
import limusic.composeapp.generated.resources.reset
import limusic.composeapp.generated.resources.theme
import limusic.composeapp.generated.resources.theme_mode_dark
import limusic.composeapp.generated.resources.theme_mode_light
import limusic.composeapp.generated.resources.theme_mode_system
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * The top of Settings > Appearance, after desktop's Appearance tab: theme presets as small phones
 * drawn in their own palette, light or dark, the accent, the background tint and the artwork switch.
 * The page is its own preview, since the app repaints as soon as a pick is stored.
 */
@Composable
fun AppearanceSettings(sharedViewModel: SharedViewModel) {
    val look by sharedViewModel.appearance.collectAsStateWithLifecycle()
    val themeMode by sharedViewModel.themeMode.collectAsStateWithLifecycle()
    val artworkColors by sharedViewModel.artworkColors.collectAsStateWithLifecycle()
    val modeDark = isDarkTheme(themeMode)
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    // Which font row's sheet is open (and which row a loaded file goes to): BODY, HEADING or null.
    var fontRow by rememberSaveable { mutableStateOf<String?>(null) }
    var loadingFor by rememberSaveable { mutableStateOf<String?>(null) }
    val set = sharedViewModel::setAppearance
    val scope = rememberCoroutineScope()
    val loadFailed = stringResource(Res.string.appearance_font_load_failed)
    val fontLauncher =
        filePickerResult { uri ->
            val row = loadingFor
            if (uri == null || row == null) return@filePickerResult
            scope.launch {
                val path = importFontFile(uri) ?: return@launch showToast(loadFailed, ToastGravity.Bottom)
                val now = sharedViewModel.appearance.value
                set(now.copy(fontFiles = now.fontFiles - path + path).withFont(row, path))
            }
        }

    SettingGroup(title = stringResource(Res.string.theme)) {
        LazyRow(
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(ThemePreset.entries, key = { it.name }) { preset ->
                ThemeCard(
                    preset = preset,
                    look = look.copy(theme = preset),
                    dark = preset.alwaysDark || modeDark,
                    selected = look.theme == preset,
                    onClick = { set(look.copy(theme = preset)) },
                )
            }
        }
        SettingItem(
            title = stringResource(Res.string.appearance_mode),
            subtitle = stringResource(if (look.theme.alwaysDark) Res.string.appearance_mode_amoled else Res.string.appearance_mode_hint),
            smallSubtitle = true,
            otherView = { ModePicker(themeMode, enabled = !look.theme.alwaysDark, onMode = sharedViewModel::setThemeMode) },
        )
    }

    SettingGroup(title = stringResource(Res.string.appearance_colors)) {
        SettingItem(
            title = stringResource(Res.string.appearance_accent),
            subtitle = stringResource(Res.string.appearance_accent_hint),
            smallSubtitle = true,
            otherView = {
                AccentRow(look, onPick = { set(look.copy(accent = it)) }, onCustom = { pickerOpen = true })
                if (LocalArtworkColors.current) {
                    Text(
                        stringResource(Res.string.appearance_accent_artwork_note),
                        style = typo().bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            },
        )
        val isDefault = look.theme == ThemePreset.DEFAULT
        SettingItem(
            title = stringResource(Res.string.appearance_tint),
            subtitle =
                if (isDefault) {
                    stringResource(Res.string.appearance_tint_hint)
                } else {
                    stringResource(Res.string.appearance_tint_palette, look.theme.label)
                },
            smallSubtitle = true,
            otherView =
                if (isDefault) {
                    {
                        OverrideSlider(
                            value = look.hue,
                            themeValue = DefaultNeutralHue,
                            range = 0f..360f,
                            onCommit = { set(look.copy(hue = it)) },
                            track = TintTrack,
                            thumb = { Color(Hct.from(it.toDouble(), 30.0, 70.0).toInt()) },
                        )
                    }
                } else {
                    null
                },
        )
        SettingItem(
            title = stringResource(Res.string.artwork_colors),
            subtitle = stringResource(Res.string.artwork_colors_description),
            smallSubtitle = true,
            switch = ((artworkColors == DataStoreManager.TRUE) to { sharedViewModel.setArtworkColors(it) }),
        )
    }

    SettingGroup(title = stringResource(Res.string.appearance_interface)) {
        SettingItem(
            title = stringResource(Res.string.appearance_roundness),
            subtitle = stringResource(Res.string.appearance_roundness_hint),
            smallSubtitle = true,
            otherView = {
                OverrideSlider(
                    value = look.radius,
                    themeValue = look.theme.radius,
                    range = 0f..16f,
                    onCommit = { set(look.copy(radius = it)) },
                    readout = { "${it.roundToInt()} dp" },
                )
            },
        )
        SettingItem(
            title = stringResource(Res.string.appearance_font_body),
            subtitle = fontLabel(look.bodyFont),
            smallSubtitle = true,
            navigates = true,
            onClick = { fontRow = BODY },
        )
        SettingItem(
            title = stringResource(Res.string.appearance_font_heading),
            subtitle = fontLabel(look.headingFont),
            smallSubtitle = true,
            navigates = true,
            onClick = { fontRow = HEADING },
        )
    }

    SettingGroup(title = null) {
        SettingItem(
            title = stringResource(Res.string.appearance_reset),
            subtitle = stringResource(Res.string.appearance_reset_hint),
            smallSubtitle = true,
            isEnable = !look.isPlain,
            onClick = { set(look.reset()) },
        )
    }

    fontRow?.let { row ->
        FontSheet(
            title = stringResource(if (row == BODY) Res.string.appearance_font_body else Res.string.appearance_font_heading),
            current = if (row == BODY) look.fontBody else look.fontHeading,
            themeFont = if (row == BODY) look.theme.body else look.theme.heading,
            files = look.fontFiles,
            onPick = {
                set(look.withFont(row, it))
                fontRow = null
            },
            onLoad = {
                loadingFor = row
                fontRow = null
                fontLauncher.launch()
            },
            onRemove = { path ->
                deleteFontFile(path)
                val now = sharedViewModel.appearance.value
                set(
                    now.copy(
                        fontFiles = now.fontFiles - path,
                        fontBody = now.fontBody.takeIf { it != path },
                        fontHeading = now.fontHeading.takeIf { it != path },
                    ),
                )
            },
            onDismiss = { fontRow = null },
        )
    }

    if (pickerOpen) {
        AccentPickerSheet(
            initial = look.accentColor ?: MaterialTheme.colorScheme.primary,
            onPick = { set(sharedViewModel.appearance.value.copy(accent = it)) },
            onDismiss = { pickerOpen = false },
        )
    }
}

/** A preset as a small phone in its own palette, with the user's accent on it: what tapping gives. */
@Composable
private fun ThemeCard(
    preset: ThemePreset,
    look: Appearance,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = rememberThemeScheme(look, dark)
    val heading = rememberFontFamily(look.headingFont)
    val ring = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(18.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .border(2.dp, if (selected) ring else Color.Transparent, shape)
                .clip(shape)
                .clickable(role = Role.RadioButton, onClick = onClick)
                .semantics { this.selected = selected }
                .padding(4.dp),
        ) {
            MiniPhone(scheme, heading, look.cornerScale)
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).background(ring, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(LimusicIcons.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            preset.label,
            style = typo().bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** Home in miniature: a type sample in the heading font, shortcuts, a shelf, the mini player and the tab bar. */
@Composable
private fun MiniPhone(
    s: ColorScheme,
    heading: FontFamily,
    corners: Float,
) {
    val tile = RoundedCornerShape(3.dp * corners)
    val frame = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .size(80.dp, 144.dp)
            .clip(frame)
            .background(s.background)
            .border(1.dp, s.outlineVariant, frame)
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("Aa", color = s.onSurface, fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 15.sp)
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(2) { Box(Modifier.weight(1f).height(12.dp).background(s.surfaceContainerHigh, tile)) }
            }
        }
        Box(Modifier.size(26.dp, 3.dp).background(s.onSurfaceVariant, CircleShape))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { Box(Modifier.weight(1f).aspectRatio(1f).background(s.surfaceContainerHighest, tile)) }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().background(s.surfaceContainerHigh, RoundedCornerShape(5.dp * corners)).padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(12.dp).background(s.primaryContainer, RoundedCornerShape(2.dp * corners)))
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(Modifier.fillMaxWidth(0.8f).height(2.5.dp).background(s.onSurface, CircleShape))
                Box(Modifier.fillMaxWidth(0.5f).height(2.5.dp).background(s.onSurfaceVariant, CircleShape))
            }
            Box(Modifier.size(10.dp).background(s.primary, CircleShape))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp, 5.dp).background(s.primary, CircleShape))
            repeat(2) { Box(Modifier.size(5.dp).background(s.onSurfaceVariant, CircleShape)) }
        }
    }
}

@Composable
private fun ModePicker(
    themeMode: String,
    enabled: Boolean,
    onMode: (String) -> Unit,
) {
    val modes =
        listOf(
            DataStoreManager.THEME_MODE_LIGHT to stringResource(Res.string.theme_mode_light),
            DataStoreManager.THEME_MODE_DARK to stringResource(Res.string.theme_mode_dark),
            DataStoreManager.THEME_MODE_SYSTEM to stringResource(Res.string.theme_mode_system),
        )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        modes.forEachIndexed { i, (mode, label) ->
            SegmentedButton(
                selected = themeMode == mode,
                onClick = { onMode(mode) },
                shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                enabled = enabled,
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}

/**
 * The theme's own accent, desktop's eight swatches, the wallpaper's colour (Android 12+, not on
 * Material You, whose own accent already is the wallpaper) and a custom colour, which shows
 * itself once picked and a colour wheel until then.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentRow(
    look: Appearance,
    onPick: (String?) -> Unit,
    onCustom: () -> Unit,
) {
    val dark = LocalIsDarkTheme.current
    val own = rememberThemeScheme(look.copy(accent = null), dark).primary
    val wallpaper =
        if (isWallpaperDynamicColorSupported() && look.theme != ThemePreset.MATERIAL_YOU) platformDynamicColorScheme(dark) else null
    val custom = look.accent?.uppercase()?.takeIf { it != Appearance.WALLPAPER.uppercase() && it !in AccentSwatches }
    val accentLabel = stringResource(Res.string.appearance_accent)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Swatch(SolidColor(own), onAccent(own), look.accent == null, stringResource(Res.string.appearance_accent_theme)) { onPick(null) }
        AccentSwatches.forEach { hex ->
            val color = parseThemeColorHex(hex) ?: Color.Gray
            Swatch(SolidColor(color), onAccent(color), look.accent.equals(hex, ignoreCase = true), "$accentLabel #$hex") { onPick(hex) }
        }
        if (wallpaper != null) {
            Swatch(
                Brush.verticalGradient(0f to wallpaper.primary, 0.5f to wallpaper.primary, 0.5f to wallpaper.tertiary, 1f to wallpaper.tertiary),
                onAccent(wallpaper.primary),
                look.accent == Appearance.WALLPAPER,
                stringResource(Res.string.appearance_accent_wallpaper),
            ) { onPick(Appearance.WALLPAPER) }
        }
        val customColor = custom?.let(::parseThemeColorHex)
        Swatch(
            customColor?.let(::SolidColor) ?: Brush.sweepGradient(Rainbow),
            customColor?.let(::onAccent) ?: Color.White,
            customColor != null,
            stringResource(Res.string.custom_color),
            icon = LimusicIcons.Add,
            onClick = onCustom,
        )
    }
}

private val Rainbow = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)

@Composable
private fun Swatch(
    fill: Brush,
    mark: Color,
    selected: Boolean,
    description: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(40.dp)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(fill)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            selected -> Icon(LimusicIcons.Check, null, tint = mark, modifier = Modifier.size(16.dp))
            icon != null -> Icon(icon, null, tint = mark, modifier = Modifier.size(16.dp))
        }
    }
}

// Desktop's tint track: the hue wheel, muted, since the tint is a shade of grey and a full rainbow
// would promise colours it never paints.
private val TintTrack = Brush.horizontalGradient((0..12).map { Color(Hct.from(it * 30.0, 30.0, 70.0).toInt()) })

/**
 * A slider over one override: starts where the theme sits, stores on release (the app repaints
 * once, as on desktop), and a reset beside it puts the theme's value back.
 */
@Composable
private fun OverrideSlider(
    value: Float?,
    themeValue: Float,
    range: ClosedFloatingPointRange<Float>,
    onCommit: (Float?) -> Unit,
    readout: ((Float) -> String)? = null,
    track: Brush? = null,
    thumb: ((Float) -> Color)? = null,
) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val shown = drag ?: value ?: themeValue
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = shown,
            onValueChange = { drag = it },
            onValueChangeFinished = {
                drag?.let(onCommit)
                drag = null
            },
            valueRange = range,
            colors = thumb?.let { SliderDefaults.colors(thumbColor = it(shown)) } ?: SliderDefaults.colors(),
            track =
                if (track != null) {
                    { Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(track)) }
                } else {
                    { SliderDefaults.Track(it) }
                },
            modifier = Modifier.weight(1f),
        )
        if (readout != null) {
            Text(readout(shown), style = typo().bodySmall, modifier = Modifier.padding(start = 8.dp))
        }
        IconButton(onClick = { onCommit(null) }, enabled = value != null, modifier = Modifier.alpha(if (value != null) 1f else 0f)) {
            Icon(LimusicIcons.Undo, contentDescription = stringResource(Res.string.reset))
        }
    }
}

private data class Hsv(
    val h: Float,
    val s: Float,
    val v: Float,
) {
    val color: Color get() = Color.hsv(h, s, v)
}

internal fun Color.toHsv(): Triple<Float, Float, Float> {
    val max = maxOf(red, green, blue)
    val d = max - minOf(red, green, blue)
    val h =
        when {
            d == 0f -> 0f
            max == red -> 60 * (((green - blue) / d).mod(6f))
            max == green -> 60 * ((blue - red) / d + 2)
            else -> 60 * ((red - green) / d + 4)
        }
    return Triple(h, if (max == 0f) 0f else d / max, max)
}

internal fun Color.hex(): String = (toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()

/** A press-and-drag pick on a 2D surface, reported in pixels; [onEnd] once the finger lifts. */
private fun Modifier.dragPick(
    onMove: (Offset, IntSize) -> Unit,
    onEnd: () -> Unit,
) = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        onMove(down.position, size)
        drag(down.id) { change ->
            change.consume()
            onMove(change.position, size)
        }
        onEnd()
    }
}

/**
 * Desktop's ColorPicker: saturation/brightness square, hue bar and an editable hex field. A drag
 * previews in the square's own swatch and is stored when the finger lifts, so the app repaints once
 * per pick instead of on every move.
 */
@Composable
private fun AccentPickerSheet(
    initial: Color,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var hsv by remember { initial.toHsv().let { (h, s, v) -> mutableStateOf(Hsv(h, s, v)) } }
    var text by remember { mutableStateOf(initial.hex()) }
    val pick by rememberUpdatedState(onPick)
    fun commit() = pick(hsv.color.hex())

    fun update(next: Hsv) {
        hsv = next
        text = next.color.hex()
    }
    SettingSheet(title = stringResource(Res.string.custom_color), onDismiss = onDismiss) { colors ->
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(14.dp))
                .drawBehind {
                    drawRect(Color.hsv(hsv.h, 1f, 1f))
                    drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    val at = Offset(hsv.s * size.width, (1 - hsv.v) * size.height)
                    drawCircle(hsv.color, 10.dp.toPx(), at)
                    drawCircle(Color.White, 10.dp.toPx(), at, style = Stroke(3.dp.toPx()))
                }.dragPick(
                    onMove = { p, size ->
                        update(hsv.copy(s = (p.x / size.width).coerceIn(0f, 1f), v = 1 - (p.y / size.height).coerceIn(0f, 1f)))
                    },
                    onEnd = ::commit,
                ),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(CircleShape)
                .drawBehind {
                    drawRect(Brush.horizontalGradient(Rainbow))
                    val at = Offset(hsv.h / 360f * size.width, size.height / 2)
                    drawCircle(Color.hsv(hsv.h, 1f, 1f), 10.dp.toPx(), at)
                    drawCircle(Color.White, 10.dp.toPx(), at, style = Stroke(3.dp.toPx()))
                }.dragPick(
                    onMove = { p, size -> update(hsv.copy(h = (p.x / size.width).coerceIn(0f, 1f) * 359.9f)) },
                    onEnd = ::commit,
                ),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).background(hsv.color, CircleShape))
            OutlinedTextField(
                value = text,
                onValueChange = { typed ->
                    text = typed.removePrefix("#").filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }.take(6).uppercase()
                    if (text.length == 6) {
                        parseThemeColorHex(text)?.toHsv()?.let { (h, s, v) ->
                            hsv = Hsv(h, s, v)
                            commit()
                        }
                    }
                },
                prefix = { Text("#", color = colors.content) },
                singleLine = true,
                isError = text.length != 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.weight(1f),
            )
        }
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.done)) }
    }
}

private const val BODY = "body"
private const val HEADING = "heading"

private fun Appearance.withFont(
    row: String,
    font: String?,
) = if (row == BODY) copy(fontBody = font) else copy(fontHeading = font)

private fun fontLabel(id: String): String = AppFont.entries.firstOrNull { it.name == id }?.label ?: fontFileLabel(id)

/** Desktop's font dropdown as a sheet: every name in its own face, the theme's own first. */
@Composable
private fun FontSheet(
    title: String,
    current: String?,
    themeFont: AppFont,
    files: List<String>,
    onPick: (String?) -> Unit,
    onLoad: () -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    SettingSheet(title = title, onDismiss = onDismiss) { colors ->
        Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
            FontOption(stringResource(Res.string.appearance_font_theme, themeFont.label), themeFont.name, current == null, colors.content) { onPick(null) }
            AppFont.entries.forEach { font ->
                FontOption(font.label, font.name, current == font.name, colors.content) { onPick(font.name) }
            }
            if (files.isNotEmpty()) {
                Text(
                    stringResource(Res.string.appearance_your_fonts),
                    style = typo().bodySmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                files.forEach { path ->
                    FontOption(fontFileLabel(path), path, current == path, colors.content, onRemove = { onRemove(path) }) { onPick(path) }
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onLoad).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(LimusicIcons.Add, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(Res.string.appearance_font_load), style = typo().labelMedium, color = colors.content)
                    Text(stringResource(Res.string.appearance_font_load_hint), style = typo().bodySmall)
                }
            }
        }
    }
}

@Composable
private fun FontOption(
    label: String,
    id: String,
    selected: Boolean,
    color: Color,
    onRemove: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = typo().labelMedium,
            fontFamily = rememberFontFamily(id),
            fontWeight = FontWeight.Medium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(LimusicIcons.Close, contentDescription = stringResource(Res.string.appearance_font_remove, label))
            }
        }
        if (selected) Icon(LimusicIcons.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Now Playing style as three small drawings of the players, in the dark scheme they open in. Apple
 * Music keeps its "Android 12+" note: its treatments are blur, which older Android doesn't draw.
 */
@Composable
fun PlayerStylePicker(
    style: String,
    onStyle: (String) -> Unit,
) {
    val scheme = LocalForcedDarkColorScheme.current ?: MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple(DataStoreManager.NOW_PLAYING_STYLE_SPOTIFY, stringResource(Res.string.now_playing_style_spotify), null),
            Triple(DataStoreManager.NOW_PLAYING_STYLE_M3_EXPRESSIVE, "M3 Expressive", null),
            Triple(
                DataStoreManager.NOW_PLAYING_STYLE_APPLE_MUSIC,
                stringResource(Res.string.now_playing_style_apple_music),
                stringResource(Res.string.requires_android_12),
            ),
        ).forEach { (id, label, note) ->
            val selected = style == id
            val ring = MaterialTheme.colorScheme.primary
            val shape = RoundedCornerShape(18.dp)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .border(2.dp, if (selected) ring else Color.Transparent, shape)
                        .clip(shape)
                        .clickable(role = Role.RadioButton) { onStyle(id) }
                        .semantics { this.selected = selected }
                        .padding(4.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.56f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp)),
                    ) {
                        when (id) {
                            DataStoreManager.NOW_PLAYING_STYLE_M3_EXPRESSIVE -> ExpressiveSketch(scheme)
                            DataStoreManager.NOW_PLAYING_STYLE_APPLE_MUSIC -> AppleMusicSketch(scheme)
                            else -> ClassicSketch(scheme)
                        }
                    }
                    if (selected) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).background(ring, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(LimusicIcons.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    style = typo().bodySmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note != null) Text(note, style = typo().bodySmall.copy(fontSize = 10.sp), maxLines = 1)
            }
        }
    }
}

@Composable
private fun Bar(
    color: Color,
    width: Float,
    height: Dp = 3.dp,
) = Box(Modifier.fillMaxWidth(width).height(height).background(color, CircleShape))

/** Classic: cover, the sung line, title and like, the wavy bar, pill controls, a row of squares. */
@Composable
private fun ClassicSketch(s: ColorScheme) {
    Column(Modifier.fillMaxSize().background(s.background).padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(s.surfaceContainerHighest, RoundedCornerShape(6.dp)))
        Bar(s.onSurfaceVariant, 0.6f, 2.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Bar(s.onSurface, 0.7f) }
            Icon(LimusicIcons.Favorite, null, tint = s.primary, modifier = Modifier.size(8.dp))
        }
        Box(
            Modifier.fillMaxWidth().height(6.dp).drawBehind {
                val y = size.height / 2
                val split = size.width * 0.45f
                val path = Path().apply {
                    moveTo(0f, y)
                    var x = 0f
                    while (x < split) {
                        x += 1f
                        lineTo(x, y + kotlin.math.sin(x / 2.5f) * 2f)
                    }
                }
                drawPath(path, s.primary, style = Stroke(1.5.dp.toPx()))
                drawLine(s.onSurfaceVariant.copy(alpha = 0.4f), Offset(split, y), Offset(size.width, y), 1.5.dp.toPx())
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            SketchPill(s.surfaceContainerHigh, s.onSurface, LimusicIcons.SkipPrevious, Modifier.weight(1f))
            SketchPill(s.primary, s.onPrimary, LimusicIcons.PlayArrow, Modifier.weight(1.4f))
            SketchPill(s.surfaceContainerHigh, s.onSurface, LimusicIcons.SkipNext, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(4) { Box(Modifier.weight(1f).aspectRatio(1f).border(1.dp, s.outlineVariant, RoundedCornerShape(3.dp))) }
        }
    }
}

@Composable
private fun SketchPill(
    fill: Color,
    content: Color,
    icon: ImageVector,
    modifier: Modifier,
) = Box(modifier.height(14.dp).background(fill, CircleShape), contentAlignment = Alignment.Center) {
    Icon(icon, null, tint = content, modifier = Modifier.size(9.dp))
}

/** M3 Expressive: the cover on a card, artist line, a thick bar, the button group, cards below. */
@Composable
private fun ExpressiveSketch(s: ColorScheme) {
    Column(Modifier.fillMaxSize().background(s.surfaceContainerLow).padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(s.surfaceContainerHighest, RoundedCornerShape(14.dp)))
        Bar(s.onSurface, 0.75f)
        Bar(s.onSurfaceVariant, 0.5f, 2.dp)
        Box(Modifier.fillMaxWidth().height(5.dp).background(s.surfaceContainerHighest, CircleShape)) {
            Box(Modifier.fillMaxWidth(0.4f).fillMaxHeight().background(s.primary, CircleShape))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(16.dp).background(s.secondaryContainer, RoundedCornerShape(8.dp, 2.dp, 2.dp, 8.dp)))
            Box(Modifier.weight(1.5f).height(20.dp).background(s.primary, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Icon(LimusicIcons.PlayArrow, null, tint = s.onPrimary, modifier = Modifier.size(11.dp))
            }
            Box(Modifier.weight(1f).height(16.dp).background(s.secondaryContainer, RoundedCornerShape(2.dp, 8.dp, 8.dp, 2.dp)))
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(8.dp).background(s.surfaceContainerHigh, RoundedCornerShape(6.dp, 6.dp, 0.dp, 0.dp)))
    }
}

/** Apple Music: a frosted wash from the cover, the cover, white text and bare controls. */
@Composable
private fun AppleMusicSketch(s: ColorScheme) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(s.primaryContainer, s.background)))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth(0.9f).aspectRatio(1f).background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(6.dp)))
        Spacer(Modifier.height(2.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Bar(Color.White, 0.6f)
            Bar(Color.White.copy(alpha = 0.55f), 0.4f, 2.dp)
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.25f), CircleShape)) {
            Box(Modifier.fillMaxWidth(0.35f).fillMaxHeight().background(Color.White.copy(alpha = 0.8f), CircleShape))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Icon(LimusicIcons.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(11.dp))
            Icon(LimusicIcons.Pause, null, tint = Color.White, modifier = Modifier.size(15.dp))
            Icon(LimusicIcons.SkipNext, null, tint = Color.White, modifier = Modifier.size(11.dp))
        }
    }
}
