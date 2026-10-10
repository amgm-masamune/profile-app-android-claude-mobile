package com.amgm.personallog.receipt

import android.util.Base64
import com.amgm.personallog.data.model.Receipt
import com.amgm.personallog.data.model.ReceiptItem
import com.amgm.personallog.data.model.ReceiptStatus
import com.amgm.personallog.data.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

sealed interface ScanOutcome {
    /** 読み取り成功。raw は Claude 応答の原文。 */
    data class Parsed(val receipt: ParsedReceipt, val raw: String) : ScanOutcome

    /** 応答は得たがJSONとして解釈できなかった(手入力で直す)。 */
    data class Unparsable(val raw: String, val reason: String) : ScanOutcome

    /** 送信前/通信のエラー(日本語メッセージ)。 */
    data class Failed(val message: String) : ScanOutcome
}

/** 画像1枚を Claude に送って結果を返す。送るのはレシート画像とプロンプトのみ。 */
class ReceiptScanner @Inject constructor(
    private val settings: SettingsRepository,
    private val store: ReceiptImageStore,
    private val client: ClaudeApiClient,
) {
    suspend fun scan(imagePath: String): ScanOutcome {
        val s = settings.settings.first()
        if (s.anthropicApiKey.isBlank()) {
            return ScanOutcome.Failed("APIキーが未設定です。設定タブでAPIキーを入れてください。")
        }
        val file = store.file(imagePath)
        if (!file.exists()) return ScanOutcome.Failed("画像ファイルが見つかりません。")
        return try {
            val body = withContext(Dispatchers.Default) {
                val bytes = ReceiptImageCodec.prepareForUpload(file)
                ClaudeRequest.buildBody(s.claudeModelId, Base64.encodeToString(bytes, Base64.NO_WRAP))
            }
            val response = client.post(s.anthropicApiKey.trim(), body)
            val text = ClaudeRequest.extractText(response)
                ?: return ScanOutcome.Unparsable(response, "応答に本文がありませんでした")
            when (val r = ReceiptParser.parse(text)) {
                is ParseResult.Success -> ScanOutcome.Parsed(r.receipt, text)
                is ParseResult.Failure -> ScanOutcome.Unparsable(text, r.reason)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ReceiptApiException) {
            ScanOutcome.Failed(e.message.orEmpty())
        } catch (e: OutOfMemoryError) {
            ScanOutcome.Failed("画像が大きすぎて処理できませんでした。")
        } catch (e: Exception) {
            ScanOutcome.Failed("読み取り中にエラーが発生しました: ${e.message.orEmpty()}")
        }
    }
}

/** 読み取り結果 <-> DBエンティティの変換(純Kotlin部分)。 */
object ReceiptMapper {
    fun purchasedAt(date: String, time: String?): String =
        if (time.isNullOrBlank()) date else "$date $time"

    fun draftReceipt(
        outcome: ScanOutcome,
        imagePath: String,
        now: Long,
        today: String = LocalDate.now().toString(),
        existingId: Long = 0,
    ): Pair<Receipt, List<ReceiptItem>> = when (outcome) {
        is ScanOutcome.Parsed -> {
            val p = outcome.receipt
            val items = p.items.map {
                ReceiptItem(receiptId = existingId, name = it.name, quantity = it.quantity, unitPrice = it.unitPrice, amount = it.amount, category = it.category)
            }
            Receipt(
                id = existingId,
                purchasedAt = purchasedAt(p.date ?: today, p.time),
                storeName = p.store,
                totalAmount = p.total ?: ReceiptCalc.itemsSum(p.items.map { it.amount }).toInt(),
                imagePath = imagePath,
                rawJson = outcome.raw,
                status = ReceiptStatus.DRAFT,
                createdAt = now,
            ) to items
        }
        is ScanOutcome.Unparsable -> Receipt(
            id = existingId, purchasedAt = today, storeName = "", totalAmount = 0,
            imagePath = imagePath, rawJson = outcome.raw, status = ReceiptStatus.DRAFT, createdAt = now,
        ) to emptyList()
        is ScanOutcome.Failed -> Receipt(
            id = existingId, purchasedAt = today, storeName = "", totalAmount = 0,
            imagePath = imagePath, rawJson = "", status = ReceiptStatus.DRAFT, createdAt = now,
        ) to emptyList()
    }
}
