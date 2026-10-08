package com.example.businesscard.ui.detail

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.businesscard.R
import com.example.businesscard.ui.component.BusinessCardView
import com.example.businesscard.ui.component.GlassIconButton
import com.example.businesscard.ui.component.SoftGlassBackground
import com.example.businesscard.ui.preview.SampleCards
import com.example.businesscard.ui.theme.BusinessCardTheme
import com.example.businesscard.ui.theme.SoftGlassSize

@Composable
fun DetailRoute(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DetailScreen(
        uiState = uiState,
        onBack = onBack,
        onEdit = { onEdit(viewModel.cardId) },
    )
}

/**
 * 表示画面は横画面固定。画面を離れると元の向き設定に戻す。
 *
 * 横画面では両手の親指が左右の端に来るので、操作は名刺の左右に分ける:
 * 左上に「戻る」、右下に「編集」(主操作。強く光る)。名刺は中央いっぱいに表示する。
 * 影は右下に落ちるので、右と下の余白を左上より広く取っている。
 */
@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    SoftGlassBackground {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 20.dp, top = 20.dp, end = 32.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxHeight().width(SoftGlassSize.iconButton),
                verticalArrangement = Arrangement.Top,
            ) {
                GlassIconButton(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                uiState.card?.let { card -> BusinessCardView(card = card) }
            }
            Column(
                modifier = Modifier.fillMaxHeight().width(SoftGlassSize.iconButton),
                verticalArrangement = Arrangement.Bottom,
            ) {
                GlassIconButton(
                    onClick = onEdit,
                    icon = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit),
                    primary = true,
                )
            }
        }
    }
}

@Preview(widthDp = 892, heightDp = 412)
@Composable
private fun DetailScreenPreview() {
    BusinessCardTheme {
        DetailScreen(uiState = DetailUiState(card = SampleCards.taro, isLoading = false), onBack = {}, onEdit = {})
    }
}

@Composable
private fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(orientation) {
        val activity = context.findActivity()
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = orientation
        onDispose {
            if (activity != null && original != null) {
                activity.requestedOrientation = original
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
