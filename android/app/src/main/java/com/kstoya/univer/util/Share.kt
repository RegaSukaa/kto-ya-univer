package com.kstoya.univer.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object Share {
    fun uriFor(ctx: Context, file: File): Uri =
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)

    fun file(ctx: Context, file: File, mime: String, subject: String) {
        val uri = uriFor(ctx, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Отчёт в Telegram: сначала пробуем открыть сам мессенджер, иначе — обычный выбор приложения. */
    fun telegram(ctx: Context, text: String, subject: String) {
        val packages = listOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.telegram.plus",
            "org.thunderdog.challegram",
            "com.ayugram.messenger"
        )
        for (pkg in packages) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { ctx.startActivity(intent); true }.getOrDefault(false)) return
        }
        text(ctx, text, subject)
    }

    fun text(ctx: Context, text: String, subject: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        ctx.startActivity(Intent.createChooser(send, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
