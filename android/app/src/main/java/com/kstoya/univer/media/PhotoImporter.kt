package com.kstoya.univer.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** Импорт фото: EXIF → даунскейл 1440 → кроп 3:4 → (сегментация) → webp-кэш. */
object PhotoImporter {

    const val CARD_W = 720
    const val CARD_H = 960

    suspend fun loadBitmap(ctx: Context, uri: Uri, maxSide: Int = 1440): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                val longSide = max(bounds.outWidth, bounds.outHeight)
                while (longSide / sample > maxSide) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                var bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                    ?: return@runCatching null
                val rotation = readRotation(ctx, uri)
                if (rotation != 0f) {
                    val m = Matrix().apply { postRotate(rotation) }
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                }
                bmp
            }.getOrNull()
        }

    private fun readRotation(ctx: Context, uri: Uri): Float = runCatching {
        ctx.contentResolver.openInputStream(uri)?.use { s ->
            when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
    }.getOrDefault(0f)

    /**
     * Кроп вьюпорта 3:4 с зумом и нормализованным сдвигом (-1..1 = край).
     * Результат всегда 720×960.
     */
    fun cropToCard(src: Bitmap, zoom: Float = 1f, dx: Float = 0f, dy: Float = 0f): Bitmap {
        val r = CARD_W.toFloat() / CARD_H
        val w = src.width.toFloat()
        val h = src.height.toFloat()
        val baseW: Float
        val baseH: Float
        if (w / h > r) { baseH = h; baseW = h * r } else { baseW = w; baseH = w / r }
        val z = zoom.coerceIn(1f, 4f)
        val cw = (baseW / z).coerceAtMost(w)
        val ch = (baseH / z).coerceAtMost(h)
        val slackX = (w - cw) / 2f
        val slackY = (h - ch) / 2f
        val cx = (slackX + dx.coerceIn(-1f, 1f) * slackX).coerceIn(0f, w - cw)
        val cy = (slackY + dy.coerceIn(-1f, 1f) * slackY).coerceIn(0f, h - ch)
        val rect = android.graphics.Rect(cx.toInt(), cy.toInt(), min(w.toInt(), (cx + cw).toInt()), min(h.toInt(), (cy + ch).toInt()))
        val safe = android.graphics.Rect(
            rect.left.coerceIn(0, max(0, src.width - 2)),
            rect.top.coerceIn(0, max(0, src.height - 2)),
            rect.right.coerceIn(rect.left + 2, src.width),
            rect.bottom.coerceIn(rect.top + 2, src.height)
        )
        val cropped = Bitmap.createBitmap(src, safe.left, safe.top, safe.width(), safe.height())
        return Bitmap.createScaledBitmap(cropped, CARD_W, CARD_H, true)
    }

    suspend fun saveCard(ctx: Context, characterId: String, bitmap: Bitmap): Pair<String, String> =
        withContext(Dispatchers.IO) {
            val cards = File(ctx.filesDir, "cards").apply { mkdirs() }
            val full = File(cards, "${characterId}_cut.webp")
            full.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out) }
            val thumb = File(cards, "${characterId}_thumb.webp")
            val small = Bitmap.createScaledBitmap(bitmap, 360, (360f * bitmap.height / bitmap.width).toInt(), true)
            thumb.outputStream().use { out -> small.compress(Bitmap.CompressFormat.WEBP, 80, out) }
            full.absolutePath to thumb.absolutePath
        }

    fun thumbPath(ctx: Context, characterId: String): String =
        File(File(ctx.filesDir, "cards"), "${characterId}_thumb.webp").absolutePath

    fun cutPath(ctx: Context, id: String): File = File(File(ctx.filesDir, "cards"), "${id}_cut.webp")

    suspend fun process(
        ctx: Context,
        uri: Uri,
        characterId: String,
        zoom: Float,
        dx: Float,
        dy: Float,
        cutout: Boolean
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        val src = loadBitmap(ctx, uri) ?: return@withContext null
        val cropped = cropToCard(src, zoom, dx, dy)
        val cut = if (cutout) Cutout.cut(ctx, cropped) else null
        val finalBmp = cut ?: Cutout.roundedFallback(cropped)
        saveCard(ctx, characterId, finalBmp)
    }
}
