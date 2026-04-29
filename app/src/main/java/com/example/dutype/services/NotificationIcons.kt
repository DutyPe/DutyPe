package com.example.dutype.services

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.dutype.app.R
import kotlin.math.roundToInt

object NotificationIcons {
    fun largeIcon(context: Context): Bitmap? = runCatching {
        val sizePx = (64f * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(64)
        ContextCompat.getDrawable(context, R.mipmap.ic_launcher)?.toBitmap(
            width = sizePx,
            height = sizePx
        )
    }.getOrNull()

    fun tintColor(context: Context): Int =
        ContextCompat.getColor(context, R.color.notification_icon_color)
}