package com.example.dutype.worker.screens.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.LruCache
import com.example.dutype.models.JobListingSummary
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

private const val COLOR_INK = 0xFF0F0F0F.toInt()
private const val COLOR_WHITE = 0xFFFFFFFF.toInt()
private const val COLOR_BORDER = 0xFFCBD5E1.toInt()
private const val COLOR_EMERALD = 0xFF10B981.toInt()
private const val COLOR_EMERALD_DARK = 0xFF059669.toInt()
private const val COLOR_URGENT = 0xFFDC2626.toInt()
private const val COLOR_SHADOW = 0x33000000

/**
 * Canvas-drawn marker bitmaps, cached per label/state so nothing is redrawn on recomposition.
 * Call only from the main thread, after the map has been initialised.
 */
internal object MapMarkerIcons {

    private val cache = LruCache<String, BitmapDescriptor>(200)

    @Synchronized
    fun pill(context: Context, label: String, selected: Boolean, urgent: Boolean): BitmapDescriptor {
        val key = "p|$label|$selected|$urgent"
        return cache.get(key) ?: drawPill(context, label, selected, urgent).also { cache.put(key, it) }
    }

    @Synchronized
    fun cluster(context: Context, count: Int): BitmapDescriptor {
        val key = "c|$count"
        return cache.get(key) ?: drawCluster(context, count).also { cache.put(key, it) }
    }

    @Synchronized
    fun userDot(context: Context): BitmapDescriptor {
        return cache.get("u") ?: drawUserDot(context).also { cache.put("u", it) }
    }

    private fun boldPaint(sizePx: Float, color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizePx
        this.color = color
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun baseline(paint: Paint, centerY: Float): Float {
        val fm = paint.fontMetrics
        return centerY - (fm.ascent + fm.descent) / 2f
    }

    private fun drawPill(context: Context, label: String, selected: Boolean, urgent: Boolean): BitmapDescriptor {
        val d = context.resources.displayMetrics.density
        val text = boldPaint((if (selected) 13f else 12f) * d, COLOR_INK)
        val padH = 10f * d
        val dotSpace = if (urgent) 12f * d else 0f
        val h = (if (selected) 32f else 28f) * d
        val w = text.measureText(label) + padH * 2f + dotSpace
        val margin = 5f * d
        val bitmap = Bitmap.createBitmap((w + margin * 2f).toInt() + 1, (h + margin * 2f).toInt() + 1, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(margin, margin, margin + w, margin + h)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (selected) COLOR_EMERALD else COLOR_WHITE
            setShadowLayer(3f * d, 0f, 1f * d, COLOR_SHADOW)
        }
        canvas.drawRoundRect(rect, h / 2f, h / 2f, fill)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f * d
            color = if (selected) COLOR_EMERALD_DARK else COLOR_BORDER
        }
        canvas.drawRoundRect(rect, h / 2f, h / 2f, border)
        if (urgent) {
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_URGENT }
            canvas.drawCircle(rect.left + padH + 3.5f * d, rect.centerY(), 3.5f * d, dot)
        }
        canvas.drawText(label, rect.left + padH + dotSpace, baseline(text, rect.centerY()), text)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun drawCluster(context: Context, count: Int): BitmapDescriptor {
        val d = context.resources.displayMetrics.density
        val dia = when {
            count < 10 -> 36f
            count < 100 -> 44f
            else -> 52f
        } * d
        val margin = 4f * d
        val size = (dia + margin * 2f).toInt() + 1
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val c = size / 2f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            setShadowLayer(3f * d, 0f, 1f * d, COLOR_SHADOW)
        }
        canvas.drawCircle(c, c, dia / 2f - 1f * d, fill)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * d
            color = COLOR_WHITE
        }
        canvas.drawCircle(c, c, dia / 2f - 2f * d, ring)
        val text = boldPaint(14f * d, COLOR_WHITE).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(count.toString(), c, baseline(text, c), text)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun drawUserDot(context: Context): BitmapDescriptor {
        val d = context.resources.displayMetrics.density
        val size = (28f * d).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val c = size / 2f
        val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            setShadowLayer(3f * d, 0f, 1f * d, COLOR_SHADOW)
        }
        canvas.drawCircle(c, c, c - 4f * d, outer)
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2563EB.toInt() }
        canvas.drawCircle(c, c, c - 7f * d, inner)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}


/** Short pay for marker pills, e.g. "₹900", "₹18k", "₹1.2L". */
internal fun mapPayShort(job: JobListingSummary): String {
    val amount = job.payAmount.toDouble()
    if (amount <= 0.0) return "Job"
    return when {
        amount < 1000.0 -> "₹${amount.toLong()}"
        amount < 100000.0 -> {
            val k = amount / 1000.0
            if (amount % 1000.0 == 0.0) "₹${k.toLong()}k" else "₹" + String.format("%.1f", k) + "k"
        }
        else -> "₹" + String.format("%.1f", amount / 100000.0) + "L"
    }
}

/** Full pay for cards, e.g. "₹900/day". */
internal fun mapPayFull(job: JobListingSummary): String = job.payText

internal fun mapDistanceLabel(distanceKm: Double?): String? {
    if (distanceKm == null || distanceKm.isNaN() || distanceKm.isInfinite()) return null
    return if (distanceKm < 1.0) "${(distanceKm * 1000).toInt()} m" else String.format("%.1f km", distanceKm)
}
