package com.example.businesscard.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.businesscard.ui.component.SoftGlassCatalog
import com.example.businesscard.ui.detail.DetailScreen
import com.example.businesscard.ui.detail.DetailUiState
import com.example.businesscard.ui.edit.EditScreen
import com.example.businesscard.ui.edit.EditUiState
import com.example.businesscard.ui.list.ListScreen
import com.example.businesscard.ui.list.ListUiState
import com.example.businesscard.ui.preview.SampleCards
import com.example.businesscard.ui.theme.BusinessCardTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 見た目の確認用に、カタログと各画面をPNGに書き出す(スクリーンショットテスト)。
 *
 * Robolectric のネイティブ描画(実機と同じ描画エンジンをPC上で動かす仕組み)で画面を描き、
 * `app/build/outputs/screenshots/` に保存する。通常のテスト実行では飛ばし、
 * `-PrecordScreenshots=true` を付けたときだけ動く。CIではこの画像を `screenshots` ブランチに置く。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h892dp-xhdpi")
class ScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun onlyWhenRecording() {
        assumeTrue(
            "スクリーンショットは -PrecordScreenshots=true のときだけ書き出す",
            System.getProperty("screenshots.record") == "true",
        )
    }

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
    @Config(qualifiers = "w892dp-h412dp-land-xhdpi")
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

        val view = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        val dir = File("build/outputs/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
