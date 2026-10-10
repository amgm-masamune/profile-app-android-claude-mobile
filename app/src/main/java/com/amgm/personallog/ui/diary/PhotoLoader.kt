package com.amgm.personallog.ui.diary

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** inSampleSize でデコードし LruCache に保持する簡易ローダ(外部ライブラリ不使用)。 */
object PhotoLoader {
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceAtLeast(4 * 1024),
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    /** ブロッキング処理。IOスレッドから呼ぶこと。失敗時 null。 */
    fun load(file: File, reqPx: Int): Bitmap? {
        if (!file.exists()) return null
        val key = "${file.absolutePath}@$reqPx@${file.lastModified()}"
        cache.get(key)?.let { return it }
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = DiaryLogic.calcInSampleSize(bounds.outWidth, bounds.outHeight, reqPx, reqPx)
            }
            var bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
            val degrees = rotationDegrees(file)
            if (degrees != 0) {
                val m = Matrix().apply { postRotate(degrees.toFloat()) }
                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                if (rotated !== bmp) bmp.recycle()
                bmp = rotated
            }
            cache.put(key, bmp)
            bmp
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            cache.evictAll()
            null
        }
    }

    private fun rotationDegrees(file: File): Int = try {
        when (ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        0
    }
}

/** Compose用: バックグラウンドでデコードして Bitmap を返す(読み込み中は null)。 */
@Composable
fun rememberPhotoBitmap(file: File, reqPx: Int): State<Bitmap?> =
    produceState<Bitmap?>(initialValue = null, file.absolutePath, reqPx) {
        value = withContext(Dispatchers.IO) { PhotoLoader.load(file, reqPx) }
    }
