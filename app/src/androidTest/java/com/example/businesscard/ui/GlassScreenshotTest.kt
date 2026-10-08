package com.example.businesscard.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
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

    /** 主ボタンを押したまま撮る(指へ寄る動き・影の伸び・指の所の光の確認用) */
    @Test
    fun catalogPressed() {
        composeRule.setContent { BusinessCardTheme { SoftGlassCatalog() } }
        settle()
        composeRule.onNode(hasText("Launch") and hasClickAction())
            .performTouchInput { down(Offset(width * 0.3f, height * 0.5f)) }
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()
        save("catalog_pressed", captureRoot())
    }

    /**
     * 動きをコマ撮りする(CIがつないで動画にする)。時計を止めて 1コマずつ進めながら撮るので、
     * エミュレータの描画が遅くても、実際の速さどおりの動きになる。
     *
     * 登場 → 主ボタンを押して離す → スイッチ → 横並びトグル → タブ
     */
    @Test
    fun interactions() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { BusinessCardTheme { SoftGlassCatalog() } }
        val frames = FrameRecorder()

        frames.record(28)

        val launch = composeRule.onNode(hasText("Launch") and hasClickAction())
        launch.performTouchInput { down(Offset(width * 0.3f, height * 0.5f)) }
        frames.record(9)
        launch.performTouchInput { up() }
        frames.record(20)

        tap(composeRule.onNode(isToggleable()), frames)
        tap(composeRule.onAllNodes(hasText("Tabs") and isSelectable())[0], frames)
        tap(composeRule.onNode(hasText("New") and isSelectable()), frames)
    }

    private fun tap(node: SemanticsNodeInteraction, frames: FrameRecorder) {
        node.performTouchInput { down(center) }
        frames.record(4)
        node.performTouchInput { up() }
        frames.record(18)
    }

    /** 時計を1コマずつ進めながら、画面の上の部分(カタログ)を縮小して連番で保存する。 */
    private inner class FrameRecorder {
        private val dir = File(screenshotDir(), "frames").apply {
            deleteRecursively()
            mkdirs()
        }
        private var index = 0

        fun record(count: Int) {
            repeat(count) {
                composeRule.mainClock.advanceTimeBy(FRAME_MILLIS)
                val full = captureRoot()
                val cropped = Bitmap.createBitmap(full, 0, 0, full.width, (full.width * FRAME_ASPECT).toInt().coerceAtMost(full.height))
                val small = Bitmap.createScaledBitmap(cropped, cropped.width / 2, cropped.height / 2, true)
                File(dir, "f%03d.png".format(index++)).outputStream().use { out ->
                    small.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
    }

    private fun settle() {
        composeRule.waitForIdle()
        Thread.sleep(1_500)
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.waitForIdle()
    }

    private fun screenshotDir(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return File(context.filesDir, "screenshots").apply { mkdirs() }
    }

    private fun save(name: String, bitmap: Bitmap) {
        File(screenshotDir(), "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun capture(name: String, content: @Composable () -> Unit) {
        composeRule.setContent { BusinessCardTheme(content) }
        // 横画面への回転・登場の動きが終わり、部品の位置が壁の照明に反映されるまで待つ
        settle()
        save(name, captureRoot())
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

        /** 1コマの長さ(約30コマ/秒) */
        const val FRAME_MILLIS = 33L

        /** コマ撮りで残す範囲(幅に対する高さ)。カタログの4段が入る */
        const val FRAME_ASPECT = 1.25f
    }
}
