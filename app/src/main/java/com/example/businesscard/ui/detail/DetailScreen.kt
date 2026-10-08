package com.example.businesscard.ui.detail

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.businesscard.R
import com.example.businesscard.ui.AppViewModelProvider
import com.example.businesscard.ui.component.BusinessCardView

@Composable
fun DetailRoute(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: DetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DetailScreen(
        uiState = uiState,
        onBack = onBack,
        onEdit = { onEdit(viewModel.cardId) },
    )
}

/** 表示画面は横画面固定。画面を離れると元の向き設定に戻す。 */
@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    Box(modifier = Modifier.fillMaxSize()) {
        uiState.card?.let { card ->
            BusinessCardView(
                card = card,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight(0.8f),
            )
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        IconButton(
            onClick = onEdit,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
        }
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
