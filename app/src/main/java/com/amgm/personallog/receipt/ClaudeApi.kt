package com.amgm.personallog.receipt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.inject.Inject

/** 画面に出せる日本語メッセージを持つ例外。 */
class ReceiptApiException(message: String) : Exception(message)

/** リクエスト/レスポンスの組み立て(純Kotlin、JVMテスト可能)。 */
object ClaudeRequest {
    const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    const val API_VERSION = "2023-06-01"
    const val MAX_TOKENS = 2000

    val PROMPT: String = """
        このレシート画像から情報を抽出し、次の形式のJSONだけを返してください。
        {"date":"yyyy-MM-dd","time":"HH:mm"またはnull,"store":"店名","items":[{"name":"品目名","quantity":数量(数値、不明なら1),"unitPrice":単価(円の整数),"amount":金額(円の整数、値引き行は負の数),"category":"カテゴリ"}],"total":合計(円の整数),"note":"補足またはnull"}
        カテゴリは次のいずれか1つにしてください: 食費, 外食, 日用品, 交通, 医療, 衣服, 娯楽, 光熱費, その他。
        読み取れない項目はnullにしてください。
        値引き・クーポン・ポイント値引きは、負の金額の品目として含めてください。
        説明文やコードフェンスを付けず、JSONだけを返してください。
    """.trimIndent()

    fun buildBody(model: String, base64Image: String, mediaType: String = "image/jpeg"): String {
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", MAX_TOKENS)
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", "user")
                            put(
                                "content",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("type", "image")
                                            put(
                                                "source",
                                                buildJsonObject {
                                                    put("type", "base64")
                                                    put("media_type", mediaType)
                                                    put("data", base64Image)
                                                },
                                            )
                                        },
                                    )
                                    add(
                                        buildJsonObject {
                                            put("type", "text")
                                            put("text", PROMPT)
                                        },
                                    )
                                },
                            )
                        },
                    )
                },
            )
        }
        return body.toString()
    }

    /** 応答JSONから content の最初の text を取り出す。取れなければ null。 */
    fun extractText(responseBody: String): String? {
        val root = ReceiptParser.parseObject(responseBody) ?: return null
        val content = root["content"] as? JsonArray ?: return null
        for (block in content) {
            val o = block as? JsonObject ?: continue
            val text = (o["text"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            if (text != null) return text
        }
        return null
    }

    /** エラー応答の error.message を取り出す。 */
    fun extractErrorMessage(responseBody: String): String? {
        val root = ReceiptParser.parseObject(responseBody) ?: return null
        val err = root["error"] as? JsonObject ?: return null
        return (err["message"] as? JsonPrimitive)?.content
    }

    fun messageForHttpError(code: Int, apiMessage: String?): String {
        val detail = apiMessage?.let { "(詳細: $it)" }.orEmpty()
        return when (code) {
            401 -> "APIキーが正しくありません(401)。設定タブでAPIキーを確認してください。"
            403 -> "このAPIキーでは利用が許可されていません(403)。設定タブでAPIキーを確認してください。$detail"
            404 -> "モデルが見つかりません(404)。設定タブでモデルIDを確認してください。$detail"
            413 -> "画像が大きすぎて送れませんでした(413)。"
            429 -> "リクエストが多すぎます(429)。しばらく待ってからやり直してください。$detail"
            400 -> "リクエストが受け付けられませんでした(400)。$detail"
            in 500..599 -> "Claude側のサーバーが混み合っているか障害中です($code)。しばらく待ってからやり直してください。"
            else -> "通信エラーが発生しました(HTTP $code)。$detail"
        }
    }
}

/** Claude Messages API を HttpURLConnection で呼ぶ。 */
class ClaudeApiClient @Inject constructor() {
    /** 成功時は応答ボディ文字列。失敗時は ReceiptApiException(日本語メッセージ)。 */
    suspend fun post(apiKey: String, body: String): String = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(ClaudeRequest.ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 90_000
                doOutput = true
                setRequestProperty("x-api-key", apiKey)
                setRequestProperty("anthropic-version", ClaudeRequest.API_VERSION)
                setRequestProperty("content-type", "application/json")
            }
            val bytes = body.toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(bytes.size)
            conn.outputStream.use { it.write(bytes) }
            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                val err = try {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                } catch (e: IOException) {
                    null
                }
                throw ReceiptApiException(
                    ClaudeRequest.messageForHttpError(code, err?.let { ClaudeRequest.extractErrorMessage(it) }),
                )
            }
        } catch (e: ReceiptApiException) {
            throw e
        } catch (e: UnknownHostException) {
            throw ReceiptApiException("ネットワークに接続できません。通信状態を確認してください。")
        } catch (e: SocketTimeoutException) {
            throw ReceiptApiException("通信がタイムアウトしました。電波の良い場所でやり直してください。")
        } catch (e: IOException) {
            throw ReceiptApiException("通信に失敗しました。通信状態を確認してください。(${e.message.orEmpty()})")
        } finally {
            conn?.disconnect()
        }
    }
}
