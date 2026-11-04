package com.simohypers.limusic.ui.screen.other

import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.simohypers.limusic.domain.extension.now
import com.simohypers.limusic.expect.openUrl
import com.simohypers.limusic.extension.ultraThinBarStyle
import com.simohypers.limusic.ui.component.RippleIconButton
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.utils.VersionManager
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import limusic.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditScreen(
    paddingValues: PaddingValues,
    navController: NavController,
) {
    val hazeState = rememberHazeState()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 64.dp)
                .verticalScroll(rememberScrollState())
                .hazeSource(state = hazeState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Image(
            painter = painterResource(Res.drawable.app_icon),
            contentDescription = null,
            modifier =
                Modifier
                    .size(96.dp)
                    .clip(CircleShape),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(Res.string.app_name), style = typo().titleLarge)
        Text(
            text = stringResource(Res.string.version_format, VersionManager.getVersionName()),
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // GPL-3.0 "Appropriate Legal Notices" (sections 0 and 5(d)): both copyright lines, no
        // warranty, may be shared under the license, and where to read it. Keep all four.
        Text(
            text =
                stringResource(Res.string.copyright, now().year.toString()) + "\n" +
                    stringResource(Res.string.copyright_limusic, now().year.toString()) + "\n\n" +
                    stringResource(Res.string.credit_license),
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        TextButton(onClick = { openUrl("https://www.gnu.org/licenses/gpl-3.0.html") }) {
            Text(text = stringResource(Res.string.view_license))
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(120.dp))
    }
    TopAppBar(
        modifier =
            Modifier
                .hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle()),
        title = {
            Text(
                text = stringResource(Res.string.app_name),
                style = typo().titleMedium,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(
                            align = Alignment.CenterVertically,
                        ).basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
        },
        navigationIcon = {
            Box(Modifier.padding(horizontal = 5.dp)) {
                RippleIconButton(
                    LimusicIcons.ArrowBackIosNew,
                    Modifier
                        .size(32.dp),
                    true,
                    tint = MaterialTheme.colorScheme.onSurface,
                ) {
                    navController.navigateUp()
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                Color.Transparent,
                Color.Unspecified,
                Color.Unspecified,
                Color.Unspecified,
                Color.Unspecified,
            ),
    )
}