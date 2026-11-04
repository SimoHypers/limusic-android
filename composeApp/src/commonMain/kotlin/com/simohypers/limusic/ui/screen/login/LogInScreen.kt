package com.simohypers.limusic.ui.screen.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.domain.data.model.account.AccountInfo
import com.simohypers.limusic.expect.ui.PlatformWebView
import com.simohypers.limusic.expect.ui.createWebViewCookieManager
import com.simohypers.limusic.expect.ui.rememberWebViewState
import com.simohypers.limusic.extension.ultraThinBarStyle
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.ui.component.RippleIconButton
import com.simohypers.limusic.ui.component.rememberSurfaceDarkColors
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.screen.home.ChannelPicker
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.YouTubeAccounts
import com.simohypers.limusic.viewModel.sapisid
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.add_account
import limusic.composeapp.generated.resources.cancel_sign_in
import limusic.composeapp.generated.resources.sign_in
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Google sign-in in a WebView, as desktop Limusic does it (session.rs `open_login`). The WebView
 * keeps its own Google session, so signing back in after Sign out goes straight through; only
 * [addAccount] clears it first, so the sign-in lands on the account entered. Landing on
 * music.youtube.com hands the jar to [YouTubeAccounts]; a jar with several channels and no earlier
 * pick turns this screen into the channel picker, and leaving it then cancels the sign-in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    addAccount: Boolean,
    innerPadding: PaddingValues,
    navController: NavController,
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
) {
    val accounts = koinInject<YouTubeAccounts>()
    val hazeState = rememberHazeState()
    val coroutineScope = rememberCoroutineScope()
    val state = rememberWebViewState()
    // Add account: the WebView opens only once the old Google session is really gone, or it would
    // continue straight into it.
    var ready by remember { mutableStateOf(!addAccount) }
    var handling by remember { mutableStateOf(false) }
    // Not saveable: the jar must not end up in saved instance state.
    var pending by remember { mutableStateOf<Pair<String, List<AccountInfo>>?>(null) }
    var picking by remember { mutableStateOf<AccountInfo?>(null) }

    LaunchedEffect(Unit) {
        hideBottomNavigation()
        if (addAccount) createWebViewCookieManager().removeAllCookies { ready = true }
    }
    DisposableEffect(Unit) {
        onDispose { showBottomNavigation() }
    }

    Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
        Column {
            Spacer(Modifier.size(innerPadding.calculateTopPadding() + 64.dp))
            if (ready) {
                PlatformWebView(state, Config.LOG_IN_URL) { url ->
                    if (handling || !url.startsWith(Config.YOUTUBE_MUSIC_MAIN_URL)) return@PlatformWebView
                    handling = true
                    coroutineScope.launch {
                        // The redirect that lands here sets the cookies, sometimes a beat after the
                        // page finishes, so look a few times before calling it not signed in yet.
                        var cookie = ""
                        for (attempt in 0 until 6) {
                            cookie = createWebViewCookieManager().getCookie(Config.YOUTUBE_MUSIC_MAIN_URL)
                            if (sapisid(cookie) != null) break
                            delay(500)
                        }
                        if (sapisid(cookie) == null) {
                            Logger.d("Auth", "YouTube: landed on music.youtube.com before signing in")
                            handling = false
                            return@launch
                        }
                        when (val result = accounts.signIn(cookie)) {
                            is YouTubeAccounts.SignIn.PickChannel -> pending = cookie to result.channels
                            else -> navController.navigateUp()
                        }
                    }
                }
            }
        }

        pending?.let { (cookie, channels) ->
            val c = rememberSurfaceDarkColors()
            Column(
                Modifier
                    .fillMaxSize()
                    .background(c.container)
                    .padding(top = innerPadding.calculateTopPadding() + 64.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(16.dp))
                ChannelPicker(
                    channels = channels,
                    failed = false,
                    isCurrent = { false },
                    busy = picking,
                    onRetry = {},
                    onPick = { channel ->
                        picking = channel
                        coroutineScope.launch {
                            accounts.pickChannel(cookie, channel)
                            navController.navigateUp()
                        }
                    },
                )
                Spacer(Modifier.height(16.dp))
                // Nothing is saved until a channel is picked, so leaving is all cancelling takes.
                OutlinedButton(
                    onClick = { navController.navigateUp() },
                    enabled = picking == null,
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                ) { Text(stringResource(Res.string.cancel_sign_in)) }
            }
        }

        TopAppBar(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle()),
            title = {
                Text(
                    text = stringResource(if (addAccount) Res.string.add_account else Res.string.sign_in),
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        LimusicIcons.ArrowBackIosNew,
                        Modifier.size(32.dp),
                        true,
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
    }
}
