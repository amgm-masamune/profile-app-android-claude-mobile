package com.amgm.personallog.receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** レシート原本画像の保存場所(filesDir/receipts)。DBには filesDir からの相対パスを入れる。 */
@Singleton
class ReceiptImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dir: File get() = File(context.filesDir, DIR).also { it.mkdirs() }

    fun file(relativePath: String): File = File(context.filesDir, relativePath)

    /** 新しい保存先の相対パスを決める(ファイルはまだ作らない)。 */
    fun newRelativePath(): String {
        dir
        return "$DIR/${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
    }

    /** カメラ撮影の出力先 Uri(FileProvider)。 */
    fun uriFor(relativePath: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file(relativePath))

    /** 選択した画像を原本として保存。JPEGならバイト列そのまま、他形式はJPEG(品質95)に変換。相対パスを返す。 */
    suspend fun importFrom(uri: Uri): String = withContext(Dispatchers.IO) {
        val rel = newRelativePath()
        val target = file(rel)
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw java.io.IOException("画像を開けませんでした")
            val isJpeg = bytes.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
            if (isJpeg) {
                target.writeBytes(bytes)
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > ReceiptImageCodec.MAX_SIDE * 2) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                    ?: throw java.io.IOException("この画像形式は読み込めませんでした")
                target.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                bmp.recycle()
            }
        } catch (e: Throwable) {
            target.delete()
            throw e
        }
        rel
    }

    fun delete(relativePath: String) {
        if (relativePath.isBlank()) return
        runCatching { file(relativePath).delete() }
    }

    companion object {
        const val DIR = "receipts"
    }
}

object ReceiptImageCodec {
    const val RESIZE_THRESHOLD_BYTES = 3_500_000L
    const val MAX_SIDE = 2048
    const val JPEG_QUALITY = 92

    /** 送信用バイト列。閾値以下なら原本のまま、超えたら向きを補正して長辺 MAX_SIDE に縮小(メモリ上のみ)。 */
    fun prepareForUpload(file: File): ByteArray {
        if (file.length() <= RESIZE_THRESHOLD_BYTES) return file.readBytes()
        val bmp = decodeScaled(file, MAX_SIDE) ?: return file.readBytes()
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        bmp.recycle()
        return out.toByteArray()
    }

    /** EXIFの向きを反映し、長辺が maxSide を超えないように縮小したビットマップ。 */
    fun decodeScaled(file: File, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) return null
        val longSide = maxOf(w, h)
        var sample = 1
        while (longSide / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = BitmapFactory.decodeFile(file.path, opts) ?: return null

        val matrix = Matrix()
        val scale = maxSide.toFloat() / maxOf(bmp.width, bmp.height)
        if (scale < 1f) matrix.postScale(scale, scale)
        val orientation = try {
            ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }
        if (!matrix.isIdentity) {
            val t = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            if (t !== bmp) bmp.recycle()
            bmp = t
        }
        return bmp
    }
}
