package com.example.businesscard.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.businesscard.ui.component.SoftGlassCatalog
import com.example.businesscard.ui.detail.DetailScreen
import com.example.businesscard.ui.detail.DetailUiState
import com.example.businesscard.ui.edit.EditScreen
import com.example.businesscard.ui.edit.EditUiState
import com.example.businesscard.ui.list.ListScreen
import com.example.businesscard.ui.list.ListUiState
import com.example.businesscard.ui.preview.SampleCards
import com.example.businesscard.ui.theme.BusinessCardTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 見た目の確認用に、カタログと各画面を実機(またはエミュレータ)で描いてPNGに保存する。
 *
 * すりガラスは GPU のシェーダーと RenderEffect で描くので、PC上の疑似描画(Robolectric)では再現できない。
 * そのため実際の Android の描画で撮る。画像はアプリの内部領域 `files/screenshots/` に保存され、
 * CI(scripts/device-screenshots.sh)が取り出して `screenshots` ブランチに置く。
 *
 * [detail] は横画面固定の画面。画面が自分で横向きを要求し、端末が回転し終えてから撮る
 * (回転でActivityが作り直されないよう、src/debug/AndroidManifest.xml で設定している)。
 */
@RunWith(AndroidJUnit4::class)
class GlassScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun catalog() = capture("catalog") { SoftGlassCatalog() }

    @Test
    fun list() = capture("list") {
        ListScreen(
            uiState = ListUiState(cards = SampleCards.all, isLoading = false),
            onAddClick = {},
            onCardClick = {},
        )
    }

    @Test
    fun listEmpty() = capture("list_empty") {
        ListScreen(uiState = ListUiState(cards = emptyList(), isLoading = false), onAddClick = {}, onCardClick = {})
    }

    @Test
    fun edit() = capture("edit") {
        val card = SampleCards.taro
        EditScreen(
            uiState = EditUiState(
                name = card.name,
                company = card.company,
                title = card.title,
                phone = card.phone,
                email = card.email,
                isNew = false,
            ),
            onBack = {},
            onNameChange = {},
            onCompanyChange = {},
            onTitleChange = {},
            onPhoneChange = {},
            onEmailChange = {},
            onSave = {},
            onDelete = {},
        )
    }

    @Test
    fun editNewWithError() = capture("edit_new_error") {
        EditScreen(
            uiState = EditUiState(isNew = true, nameError = true),
            onBack = {},
            onNameChange = {},
            onCompanyChange = {},
            onTitleChange = {},
            onPhoneChange = {},
            onEmailChange = {},
            onSave = {},
            onDelete = {},
        )
    }

    @Test
    fun detail() = capture("detail") {
        DetailScreen(
            uiState = DetailUiState(card = SampleCards.taro, isLoading = false),
            onBack = {},
            onEdit = {},
        )
    }

    private fun capture(name: String, content: @Composable () -> Unit) {
        composeRule.setContent { BusinessCardTheme(content) }
        composeRule.waitForIdle()
        // 横画面への回転など、窓の大きさが変わり終わるのを待つ
        Thread.sleep(1_500)
        composeRule.waitForIdle()
        // 部品の位置が壁の照明に反映されるまで(数フレーム)待つ
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.waitForIdle()

        val bitmap = captureRoot()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.filesDir, "screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    /**
     * 画面を画像にする。エミュレータのソフトウェアGPUではシェーダーの描画に時間がかかり、
     * 撮影の待ち時間(2秒)を超えることがあるので、何度かやり直す。
     */
    private fun captureRoot(): Bitmap {
        repeat(CAPTURE_ATTEMPTS) { attempt ->
            try {
                return composeRule.onRoot().captureToImage().asAndroidBitmap()
            } catch (e: ComposeTimeoutException) {
                if (attempt == CAPTURE_ATTEMPTS - 1) throw e
                composeRule.waitForIdle()
                Thread.sleep(1_000)
            }
        }
        error("unreachable")
    }

    private companion object {
        const val CAPTURE_ATTEMPTS = 4
    }
}
