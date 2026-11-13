package com.kstoya.univer.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

object Cutout {

    /** Вырезание по контуру офлайн. null = фолбэк, а не краш. */
    suspend fun cut(ctx: Context, bitmap: Bitmap, timeoutMs: Long = 8000L): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                withTimeout(timeoutMs) {
                    val input = InputImage.fromBitmap(bitmap, 0)
                    val options = SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()
                    val client = SubjectSegmentation.getClient(options)
                    try {
                        val result = client.process(input).await()
                        val fg = result.foregroundBitmap ?: return@withTimeout null
                        if (fg.width > 0 && fg.height > 0) fg else null
                    } finally {
                        runCatching { client.close() }
                    }
                }
            }.getOrNull()
        }

    /** Фолбэк: прямоугольный кроп со скруглёнными углами и виньеткой. */
    fun roundedFallback(src: Bitmap, radiusPx: Float = 64f): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, src.width.toFloat(), src.height.toFloat())
        canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }
}
