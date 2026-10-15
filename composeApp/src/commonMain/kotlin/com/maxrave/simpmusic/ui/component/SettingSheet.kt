package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.theme.AppThemeContent
import com.maxrave.simpmusic.ui.theme.typo

/**
 * The settings pages' bottom sheet: the app's menu card colour, 20dp corners, a handle and a title,
 * the same frame as the Edit playlist sheet. [content] gets the sheet's colours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(SurfaceDarkColors) -> Unit,
) {
    AppThemeContent {
        val colors = rememberSurfaceDarkColors()
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors().copy(containerColor = colors.container),
            ) {
                Column(
                    Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Spacer(Modifier.height(5.dp))
                    Card(
                        modifier = Modifier.align(Alignment.CenterHorizontally).width(60.dp).height(4.dp),
                        colors = CardDefaults.cardColors().copy(containerColor = colors.handle),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Text(title, style = typo().titleMedium, color = colors.content)
                    content(colors)
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}
