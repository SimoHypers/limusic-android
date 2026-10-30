package com.simohypers.limusic.ui.screen.other

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.PaddingValues
import com.simohypers.limusic.ui.component.CollectionTitleText
import com.simohypers.limusic.ui.component.LibrarySectionHeader
import com.simohypers.limusic.ui.component.RoundAccentButton
import com.simohypers.limusic.ui.component.RoundIconButton
import com.simohypers.limusic.ui.component.TitleRow
import limusic.composeapp.generated.resources.radio
import limusic.composeapp.generated.resources.shuffle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.home.Content
import com.simohypers.limusic.domain.data.model.searchResult.songs.Artist
import com.simohypers.limusic.domain.mediaservice.handler.PlaylistType
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.expect.shareUrl
import com.simohypers.limusic.expect.ui.MediaPlayerView
import com.simohypers.limusic.expect.ui.toImageBitmap
import com.simohypers.limusic.extension.artworkScrimBrush
import com.simohypers.limusic.extension.barBlurStyle
import com.simohypers.limusic.extension.getScreenSizeInfo
import com.simohypers.limusic.extension.getStringBlocking
import com.simohypers.limusic.extension.toSquareThumbnailUrl
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.rememberHolderPainter
import com.simohypers.limusic.ui.component.DescriptionView
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.HomeItemArtist
import com.simohypers.limusic.ui.component.HomeItemContentPlaylist
import com.simohypers.limusic.ui.component.HomeItemVideo
import com.simohypers.limusic.ui.component.PillIconButton
import com.simohypers.limusic.ui.component.NowPlayingBottomSheet
import com.simohypers.limusic.ui.component.SongFullWidthItems
import com.simohypers.limusic.ui.component.pillSurface
import com.simohypers.limusic.ui.component.selection.TrackSelection
import com.simohypers.limusic.ui.component.selection.TrackSelectionBar
import com.simohypers.limusic.ui.component.selection.keyedRows
import com.simohypers.limusic.ui.component.selection.rememberTrackSelection
import com.simohypers.limusic.ui.component.selection.rowKey
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.Check
import com.simohypers.limusic.ui.icon.IosShare
import com.simohypers.limusic.ui.icon.Movie
import com.simohypers.limusic.ui.icon.MovieOff
import com.simohypers.limusic.ui.icon.PersonAdd
import com.simohypers.limusic.ui.icon.Sensors
import com.simohypers.limusic.ui.icon.Shuffle
import com.simohypers.limusic.ui.icon.SimpIcons
import com.simohypers.limusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.simohypers.limusic.ui.navigation.destination.list.AlbumDestination
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.ui.navigation.destination.list.MoreAlbumsDestination
import com.simohypers.limusic.ui.navigation.destination.list.PlaylistDestination
import com.simohypers.limusic.ui.screen.library.LibraryDynamicPlaylistType
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.ArtistScreenState
import com.simohypers.limusic.viewModel.ArtistViewModel
import com.simohypers.limusic.viewModel.SharedViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.albums
import limusic.composeapp.generated.resources.baseline_favorite_24
import limusic.composeapp.generated.resources.description
import limusic.composeapp.generated.resources.error
import limusic.composeapp.generated.resources.featured_inArtist
import limusic.composeapp.generated.resources.liked_songs
import limusic.composeapp.generated.resources.liked_songs_by
import limusic.composeapp.generated.resources.liked_songs_count
import limusic.composeapp.generated.resources.popular
import limusic.composeapp.generated.resources.related_artists
import limusic.composeapp.generated.resources.share
import limusic.composeapp.generated.resources.singles
import limusic.composeapp.generated.resources.unknown
import limusic.composeapp.generated.resources.videos

@Composable
@ExperimentalMaterial3Api
fun ArtistScreen(
    channelId: String,
    viewModel: ArtistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
    innerPadding: PaddingValues = PaddingValues(),
) {
    val artistScreenState by viewModel.artistScreenState.collectAsStateWithLifecycle()
    val isFollowed by viewModel.followed.collectAsStateWithLifecycle()
    val artistMotion by viewModel.artistMotion.collectAsStateWithLifecycle()
    // Header plays the artist's animated artwork by default; the top-right toggle swaps it for
    // their picture. Keyed on the channel, so opening a different artist starts as video again —
    // keyed on a url it would not reset between two artists whose videos are both absent.
    var showHeaderVideo by rememberSaveable(channelId) { mutableStateOf(true) }
    val shareTitle = stringResource(Res.string.share)
    val artistLogo by viewModel.artistLogo.collectAsStateWithLifecycle()

    val playingTrack by remember {
        sharedViewModel.nowPlayingState.map { it?.track?.videoId }
    }.collectAsState(null)

    // Choosing song to show Bottom sheet
    var choosingTrack by remember {
        mutableStateOf<Track?>(null)
    }
    var showBottomSheet by remember {
        mutableStateOf(false)
    }

    val selection = rememberTrackSelection()

    LaunchedEffect(channelId) {
        if (channelId != artistScreenState.data.channelId) {
            viewModel.browseArtist(channelId)
        }
    }

    // Apple Music-inspired immersive treatment. The header adapts to the window's aspect ratio
    // alone, not to the platform: a portrait window keeps the square artwork frame, a landscape
    // one (including every desktop window) uses a half-viewport-tall frame instead.
    val screenInfo = getScreenSizeInfo()
    val isPortrait = screenInfo.wDP < screenInfo.hDP
    // Apple Music publishes no tall cut for an artist, so the square rendition fills the square
    // portrait frame and the wide one the banner-shaped landscape frame. Each stands in for the
    // other when it is missing on its own: a rendition of the wrong shape still beats no video.
    val headerVideoUrl =
        artistMotion
            ?.let {
                if (isPortrait) {
                    it.squareVideoUrl ?: it.wideVideoUrl
                } else {
                    it.wideVideoUrl ?: it.squareVideoUrl
                }
            }?.takeIf { showHeaderVideo }

    // The decoded artwork's shape sizes the landscape banner frame.
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val background = MaterialTheme.colorScheme.background

    val hazeState = rememberHazeState()
    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf { lazyState.firstVisibleItemIndex == 0 }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }

    Crossfade(artistScreenState) { state ->
        when (state) {
            is ArtistScreenState.Loading -> {
                Box(Modifier.fillMaxSize()) {
                    CenterLoadingBox(
                        Modifier
                            .align(Alignment.Center),
                    )
                }
            }

            is ArtistScreenState.Success -> {
                // ---- Apple Music style (mobile portrait only) ----
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(background)
                                .hazeSource(hazeState),
                        state = lazyState,
                    ) {
                        item(contentType = "header") {
                            Column {
                                // Edge-to-edge artwork (the artist's video plays on top of it when available).
                                // Portrait fills a SQUARE frame, so the URL is clamped to a square
                                // size there (logic from commit 5e596c5b). Landscape keeps YouTube's
                                // own wide banner (e.g. w2880-h1200) instead: squaring the source
                                // first would make the Crop below throw away most of its height.
                                val headerImageUrl =
                                    state.data.imageUrl?.let {
                                        if (isPortrait) it.toSquareThumbnailUrl() else it
                                    }
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (isPortrait) {
                                                    Modifier.aspectRatio(1f)
                                                } else {
                                                    // The banner's own shape, so it is shown whole instead of
                                                    // cropped into a fixed hDP/2 strip. 2.4:1 (YouTube's
                                                    // w2880-h1200) until it decodes; a square bitmap left over
                                                    // from portrait is ignored so the frame never goes square.
                                                    Modifier.aspectRatio(
                                                        bitmap?.takeIf { it.width > it.height }?.let { it.width.toFloat() / it.height } ?: 2.4f,
                                                    )
                                                },
                                            ),
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                                        // Media layer (video + artwork). The video draws a black shutter
                                        // until its first frame, so the artwork stays ON TOP of it until
                                        // then and only fades out after — hiding the artwork the moment the
                                        // video's url arrived flashed the header black.
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            var videoShown by remember(headerVideoUrl) { mutableStateOf(false) }
                                            val artworkAlpha by animateFloatAsState(
                                                targetValue = if (headerVideoUrl != null && videoShown) 0f else 1f,
                                                animationSpec = tween(500),
                                                label = "artistArtworkOverVideo",
                                            )
                                            // The artist's animated artwork plays AS the background when
                                            // there is one; otherwise the static artwork above shows.
                                            headerVideoUrl?.let { videoUrl ->
                                                // cropToBounds center scale-to-covers it into the header
                                                // frame (ContentScale.Crop): true video aspect ratio, no
                                                // stretch, overflow clipped — which is what absorbs the
                                                // difference between the rendition and the frame.
                                                MediaPlayerView(
                                                    url = videoUrl,
                                                    modifier = Modifier.fillMaxSize(),
                                                    cropToBounds = true,
                                                    onFirstFrame = { videoShown = true },
                                                )
                                            }
                                            AsyncImage(
                                                model =
                                                    ImageRequest
                                                        .Builder(LocalPlatformContext.current)
                                                        .data(headerImageUrl)
                                                        .diskCachePolicy(CachePolicy.ENABLED)
                                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                                        .diskCacheKey(headerImageUrl)
                                                        .memoryCacheKey(headerImageUrl)
                                                        .crossfade(550)
                                                        .build(),
                                                placeholder = rememberHolderPainter(),
                                                error = rememberHolderPainter(),
                                                contentDescription = null,
                                                // FillWidth fits the square source into the square portrait
                                                // frame. The landscape frame takes the banner's own aspect,
                                                // so Crop trims nothing once it has decoded.
                                                contentScale =
                                                    if (isPortrait) ContentScale.FillWidth else ContentScale.Crop,
                                                // Always decoded so the page background color can be extracted
                                                // from the artwork palette, even when the video is playing.
                                                onSuccess = {
                                                    bitmap = it.result.image.toImageBitmap()
                                                },
                                                // Faded out (but still decoded above) once the video has drawn
                                                // its first frame. No video -> artwork is shown.
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .graphicsLayer { alpha = artworkAlpha },
                                            )
                                            // The artwork's bottom 200dp melts into the page through a
                                            // Modifier.blur copy of it, faded in by a DstIn gradient. At
                                            // the top of the ramp the copy is fully transparent, so there
                                            // is no seam — haze's HazeProgressive drew a visible line
                                            // there on Android (and crashes on skiko). Below Android 12
                                            // blur is a no-op and the copy is pixel-identical to the
                                            // artwork, leaving just the colour scrim. Fades with the
                                            // artwork, and is dropped once the video has taken over.
                                            if (artworkAlpha > 0f) {
                                                AsyncImage(
                                                    model =
                                                        ImageRequest
                                                            .Builder(LocalPlatformContext.current)
                                                            .data(headerImageUrl)
                                                            .crossfade(550)
                                                            .build(),
                                                    contentDescription = null,
                                                    contentScale =
                                                        if (isPortrait) ContentScale.FillWidth else ContentScale.Crop,
                                                    modifier =
                                                        Modifier
                                                            .fillMaxSize()
                                                            .graphicsLayer {
                                                                compositingStrategy = CompositingStrategy.Offscreen
                                                                alpha = artworkAlpha
                                                            }.drawWithContent {
                                                                drawContent()
                                                                drawRect(
                                                                    brush =
                                                                        Brush.verticalGradient(
                                                                            colors = listOf(Color.Transparent, Color.Black),
                                                                            startY = size.height - 200.dp.toPx(),
                                                                            endY = size.height,
                                                                        ),
                                                                    blendMode = BlendMode.DstIn,
                                                                )
                                                            }.blur(32.dp),
                                                )
                                            }
                                        } // end media layer
                                        // 5% black over the artwork/video, under the fade and scrim, so
                                        // a bright photo sits back a little behind the title.
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.05f)),
                                        )
                                        // Color scrim is a SEPARATE, taller box: the blur stays at 200dp so
                                        // its cost doesn't grow, while the color gets 70% of the artwork
                                        // to ramp over. A short ramp means a steep alpha, and a steep
                                        // alpha is what reads as a visible edge. 70% of the frame's own
                                        // height in both orientations — measured off the frame, since the
                                        // landscape one now follows the banner rather than the window.
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight(0.7f)
                                                    .align(Alignment.BottomCenter)
                                                    .background(artworkScrimBrush(background)),
                                        )
                                        // Desktop's hero: the name left, Radio and Shuffle at its right, the
                                        // subscribers under it.
                                        Column(
                                            modifier =
                                                Modifier
                                                    .align(Alignment.BottomStart)
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                        ) {
                                            TitleRow(
                                                title = {
                                                    val logo = artistLogo
                                                    // The name stays as text until the logo image has actually
                                                    // loaded, then the two cross-fade and the box eases to the
                                                    // logo's height. Swapping on the logo's url alone blanked the
                                                    // line until the image arrived and then popped it in. Saveable,
                                                    // so coming back to the page shows the logo straight away.
                                                    var logoLoaded by rememberSaveable(logo?.logoUrl) { mutableStateOf(false) }
                                                    val logoAlpha by animateFloatAsState(
                                                        targetValue = if (logo != null && logoLoaded) 1f else 0f,
                                                        animationSpec = tween(500),
                                                        label = "artistNameLogo",
                                                    )
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                                                        contentAlignment = Alignment.CenterStart,
                                                    ) {
                                                        if (logoAlpha < 1f) {
                                                            CollectionTitleText(
                                                                title = state.data.title ?: stringResource(Res.string.unknown),
                                                                modifier = Modifier.graphicsLayer { alpha = 1f - logoAlpha },
                                                            )
                                                        }
                                                        if (logo != null) {
                                                            // Artist name rendered as a logo image (hidden catalog),
                                                            // in place of the plain-text title.
                                                            AsyncImage(
                                                                model = logo.logoUrl,
                                                                contentDescription = state.data.title,
                                                                contentScale = ContentScale.Fit,
                                                                alignment = Alignment.CenterStart,
                                                                onSuccess = { logoLoaded = true },
                                                                modifier =
                                                                    Modifier
                                                                        .fillMaxWidth()
                                                                        .heightIn(max = 84.dp)
                                                                        .graphicsLayer { alpha = logoAlpha },
                                                            )
                                                        }
                                                    }
                                                },
                                            ) {
                                                RoundIconButton(SimpIcons.Sensors, stringResource(Res.string.radio)) {
                                                    val param = state.data.radioParam
                                                    if (param != null) {
                                                        viewModel.onRadioClick(param)
                                                    } else {
                                                        viewModel.makeToast(getStringBlocking(Res.string.error))
                                                    }
                                                }
                                                RoundAccentButton(SimpIcons.Shuffle, stringResource(Res.string.shuffle)) {
                                                    val param = state.data.shuffleParam
                                                    if (param != null) {
                                                        viewModel.onShuffleClick(param)
                                                    } else {
                                                        viewModel.makeToast(getStringBlocking(Res.string.error))
                                                    }
                                                }
                                            }
                                            val meta =
                                                listOfNotNull(
                                                    state.data.subscribers?.takeIf { it.isNotBlank() },
                                                    state.data.playCount?.takeIf { it.isNotBlank() },
                                                ).joinToString(" • ")
                                            if (meta.isNotBlank()) {
                                                Text(
                                                    text = meta,
                                                    style = typo().bodyMedium,
                                                    modifier = Modifier.padding(top = 4.dp),
                                                )
                                            }
                                        }
                                    }
                                    // Back button.
                                    PillIconButton(
                                        imageVector = SimpIcons.ArrowBackIosNew,
                                        shape = RoundedCornerShape(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        // Matching the other three headers: the pill-style directional rim, thickened
                                        // from the 0.5.dp default so it stays visible around a 48dp circle.
                                        modifier =
                                            Modifier
                                                .align(Alignment.TopStart)
                                                .padding(12.dp)
                                                .windowInsetsPadding(WindowInsets.statusBars)
                                                .size(48.dp),
                                    ) {
                                        navController.navigateUp()
                                    }
                                    // Top-right pill like the album's: [video ⇄ picture] when the artist
                                    // has an animated artwork, Follow, Share.
                                    Row(
                                        modifier =
                                            Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(12.dp)
                                                .windowInsetsPadding(WindowInsets.statusBars)
                                                .height(48.dp)
                                                .pillSurface(RoundedCornerShape(24.dp))
                                                .padding(horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (artistMotion != null) {
                                            IconButton(onClick = { showHeaderVideo = !showHeaderVideo }, modifier = Modifier.size(40.dp)) {
                                                Icon(
                                                    imageVector = if (showHeaderVideo) SimpIcons.MovieOff else SimpIcons.Movie,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                        // Desktop's Subscribe: a tick in the accent once followed.
                                        IconButton(
                                            onClick = {
                                                state.data.channelId?.let { viewModel.updateFollowed(if (isFollowed) 0 else 1, it) }
                                            },
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Icon(
                                                imageVector = if (isFollowed) SimpIcons.Check else SimpIcons.PersonAdd,
                                                contentDescription = if (isFollowed) "Followed" else "Follow",
                                                tint = if (isFollowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        IconButton(
                                            onClick = { shareUrl(shareTitle, "https://music.youtube.com/channel/$channelId") },
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Icon(SimpIcons.IosShare, contentDescription = shareTitle, tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                                state.data.description?.takeIf { it.isNotBlank() }?.let { text ->
                                    val urlHandler = LocalUriHandler.current
                                    DescriptionView(
                                        text = text,
                                        limitLine = 2,
                                        onTimeClicked = {},
                                        onURLClicked = { url -> urlHandler.openUri(url) },
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                                    )
                                }
                            }
                        }
                        item(contentType = "sections") {
                            ArtistSections(
                                channelId = channelId,
                                state = state,
                                selection = selection,
                                playingTrack = playingTrack,
                                navController = navController,
                                viewModel = viewModel,
                                sharedViewModel = sharedViewModel,
                                onTrackMore = { track ->
                                    choosingTrack = track
                                    showBottomSheet = true
                                },
                            )
                        }
                    }

                    // Haze top bar appears once the header scrolls away.
                    AnimatedVisibility(
                        visible = shouldHideTopBar,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically(),
                    ) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = state.data.title ?: "",
                                    style = typo().titleMedium,
                                    maxLines = 1,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(align = Alignment.CenterVertically)
                                            .basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                animationMode = MarqueeAnimationMode.Immediately,
                                            ).focusable(),
                                )
                            },
                            navigationIcon = {
                                Box(Modifier.padding(horizontal = 5.dp)) {
                                    IconButton(onClick = { navController.navigateUp() }) {
                                        Icon(
                                            imageVector = SimpIcons.ArrowBackIosNew,
                                            contentDescription = "Back",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            },
                            colors =
                                TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                ),
                            modifier =
                                Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(background, 0.55f)),
                        )
                    }
                }

                val rows = remember(state.data.popularSongs) { keyedRows(state.data.popularSongs) }
                TrackSelectionBar(
                    selection = selection,
                    rows = rows,
                    bottomPadding = innerPadding.calculateBottomPadding(),
                    from = state.data.title,
                )
                if (showBottomSheet && choosingTrack != null) {
                    NowPlayingBottomSheet(
                        onDismiss = {
                            showBottomSheet = false
                            choosingTrack = null
                        },
                        navController = navController,
                        song = choosingTrack?.toSongEntity(),
                    )
                }
            }

            is ArtistScreenState.Error -> {
                viewModel.makeToast(state.message ?: stringResource(Res.string.error))
                navController.navigateUp()
            }
        }
    }
}

/** A shelf's title in desktop's style: heading font, "See all" at the end when [onMore] is given. */
@Composable
private fun ArtistShelfHeader(
    title: String,
    onMore: (() -> Unit)? = null,
) {
    LibrarySectionHeader(icon = null, title = title, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp), onMore = onMore)
}

/** The artist body under the header: liked songs, Popular, the shelves. */
@Composable
private fun ArtistSections(
    channelId: String,
    state: ArtistScreenState.Success,
    selection: TrackSelection,
    playingTrack: String?,
    navController: NavController,
    viewModel: ArtistViewModel,
    sharedViewModel: SharedViewModel,
    onTrackMore: (Track) -> Unit,
) {
    val likedSongCount by viewModel.likedSongCount.collectAsStateWithLifecycle()
    Column {
        // Liked songs by this artist (issue #2524), shaped like Spotify's section: a heading, then
        // the artist's picture wearing the liked heart beside the count. Shown only once at least
        // one song is liked; opens the full list with the route's channelId, the id it was counted by.
        androidx.compose.animation.AnimatedVisibility(likedSongCount > 0) {
            LikedSongsSection(
                imageUrl = state.data.imageUrl?.toSquareThumbnailUrl(),
                count = likedSongCount,
                artistName = state.data.title.orEmpty(),
                onClick = {
                    navController.navigate(
                        LibraryDynamicPlaylistDestination(
                            type = LibraryDynamicPlaylistType.ArtistLiked(channelId).toStringParams(),
                        ),
                    )
                },
            )
        }

        // Popular Songs, with their play counts like desktop's top songs.
        AnimatedVisibility(state.data.popularSongs.isNotEmpty()) {
            Column {
                ArtistShelfHeader(stringResource(Res.string.popular)) {
                    val id = state.data.listSongParam
                    if (id != null) {
                        navController.navigate(PlaylistDestination(id))
                    } else {
                        viewModel.makeToast(getStringBlocking(Res.string.error))
                    }
                }
                state.data.popularSongs.forEach { song ->
                    SongFullWidthItems(
                        track = song,
                        isPlaying = song.videoId == playingTrack,
                        modifier = Modifier.fillMaxWidth(),
                        onMoreClickListener = {
                            onTrackMore(song)
                        },
                        onClickListener = {
                            val firstQueue: Track = song
                            viewModel.setQueueData(
                                QueueData.Data(
                                    listTracks = arrayListOf(firstQueue),
                                    firstPlayedTrack = firstQueue,
                                    playlistId = "RDAMVM${song.videoId}",
                                    playlistName = "\"${state.data.title ?: ""}\" ${getStringBlocking(Res.string.popular)}",
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            viewModel.loadMediaItem(
                                firstQueue,
                                type = Config.SONG_CLICK,
                            )
                        },
                        onAddToQueue = {
                            sharedViewModel.addListToQueue(
                                arrayListOf(song),
                            )
                        },
                        selectionMode = selection.active,
                        isSelected = selection.has(rowKey(song.videoId, song.setVideoId)),
                        onLongClick = { selection.start(rowKey(song.videoId, song.setVideoId), song) },
                        onSelectToggle = { selection.toggle(rowKey(song.videoId, song.setVideoId), song) },
                        rightView =
                            song.views?.let { plays ->
                                @Composable {
                                    Text(plays, style = typo().bodySmall, maxLines = 1)
                                }
                            },
                        padding = PaddingValues(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    )
                }
            }
        }

        // Singles
        AnimatedVisibility(!state.data.singles?.results.isNullOrEmpty()) {
            Column {
                ArtistShelfHeader(stringResource(Res.string.singles)) {
                    if (state.data.channelId != null) {
                        navController.navigate(
                            MoreAlbumsDestination(
                                id = "MPAD${state.data.channelId}",
                                type = MoreAlbumsDestination.SINGLE_TYPE,
                            ),
                        )
                    } else {
                        viewModel.makeToast(getStringBlocking(Res.string.error))
                    }
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.singles?.results ?: emptyList()) { single ->
                        HomeItemContentPlaylist(
                            onClick = {
                                navController.navigate(
                                    AlbumDestination(
                                        single.browseId,
                                    ),
                                )
                            },
                            data = single,
                            thumbSize = 180.dp,
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }

        // Albums
        AnimatedVisibility(!state.data.albums?.results.isNullOrEmpty()) {
            Column {
                ArtistShelfHeader(stringResource(Res.string.albums)) {
                    if (state.data.channelId != null) {
                        navController.navigate(
                            MoreAlbumsDestination(
                                id = "MPAD${state.data.channelId}",
                                type = MoreAlbumsDestination.ALBUM_TYPE,
                            ),
                        )
                    } else {
                        viewModel.makeToast(getStringBlocking(Res.string.error))
                    }
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.albums?.results ?: emptyList()) { album ->
                        HomeItemContentPlaylist(
                            onClick = {
                                navController.navigate(
                                    AlbumDestination(
                                        browseId = album.browseId,
                                    ),
                                )
                            },
                            data = album,
                            thumbSize = 180.dp,
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }

        // Videos
        AnimatedVisibility(!state.data.video?.video.isNullOrEmpty()) {
            Column {
                ArtistShelfHeader(stringResource(Res.string.videos)) {
                    val videoListParam = state.data.video?.videoListParam
                    if (videoListParam != null) {
                        navController.navigate(
                            PlaylistDestination(
                                videoListParam,
                            ),
                        )
                    } else {
                        viewModel.makeToast(getStringBlocking(Res.string.error))
                    }
                }
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.video?.video ?: emptyList()) { video ->
                        HomeItemVideo(
                            onClick = {
                                val firstQueue: Track = video
                                viewModel.setQueueData(
                                    QueueData.Data(
                                        listTracks = arrayListOf(firstQueue),
                                        firstPlayedTrack = firstQueue,
                                        playlistId = "RDAMVM${video.videoId}",
                                        playlistName = (state.data.title ?: "") + getStringBlocking(Res.string.videos),
                                        playlistType = PlaylistType.RADIO,
                                        continuation = null,
                                    ),
                                )
                                viewModel.loadMediaItem(
                                    firstQueue,
                                    type = Config.VIDEO_CLICK,
                                )
                            },
                            onLongClick = {
                                onTrackMore(video)
                            },
                            data =
                                Content(
                                    album = null,
                                    artists = video.artists,
                                    description = null,
                                    isExplicit = video.isExplicit,
                                    playlistId = null,
                                    browseId = null,
                                    thumbnails = video.thumbnails ?: emptyList(),
                                    title = video.title,
                                    videoId = video.videoId,
                                    views = video.views,
                                ),
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }

        // Featured on
        AnimatedVisibility(state.data.featuredOn.isNotEmpty()) {
            Column {
                ArtistShelfHeader(stringResource(Res.string.featured_inArtist))
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.featuredOn) { feature ->
                        HomeItemContentPlaylist(
                            onClick = {
                                navController.navigate(
                                    PlaylistDestination(
                                        feature.id,
                                    ),
                                )
                            },
                            data = feature,
                            thumbSize = 180.dp,
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }

        // Related
        AnimatedVisibility(!state.data.related?.results.isNullOrEmpty()) {
            Column {
                ArtistShelfHeader(stringResource(Res.string.related_artists))
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                    items(state.data.related?.results ?: emptyList()) { related ->
                        HomeItemArtist(
                            onClick = {
                                navController.navigate(
                                    ArtistDestination(
                                        channelId = related.browseId,
                                    ),
                                )
                            },
                            data =
                                Content(
                                    album = null,
                                    artists =
                                        listOf(
                                            Artist(
                                                id = related.browseId,
                                                name = related.title,
                                            ),
                                        ),
                                    description = related.subscribers,
                                    isExplicit = null,
                                    playlistId = null,
                                    browseId = related.browseId,
                                    thumbnails = related.thumbnails,
                                    title = related.title,
                                    videoId = null,
                                    views = null,
                                    durationSeconds = null,
                                    radio = null,
                                ),
                        )
                    }
                    item {
                        Spacer(Modifier.size(10.dp))
                    }
                }
            }
        }
        EndOfPage()
    }
}

/**
 * "Liked songs" as a section of its own: the heading, then the artist's picture wearing the liked
 * heart beside "3 songs" / "By <artist>". The whole block opens
 * [LibraryDynamicPlaylistType.ArtistLiked].
 */
@Composable
private fun LikedSongsSection(
    imageUrl: String?,
    count: Int,
    artistName: String,
    onClick: () -> Unit,
) {
    Column {
        ArtistShelfHeader(stringResource(Res.string.liked_songs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
                Image(
                    painter = painterResource(Res.drawable.baseline_favorite_24),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(3.dp),
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = pluralStringResource(Res.plurals.liked_songs_count, count, count),
                    style = typo().titleSmall,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(Res.string.liked_songs_by, artistName),
                    style = typo().bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
