package com.example.dutype.services

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.dutype.models.JobListing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Job Share Image Generator
 * 
 * Creates branded, professional job share images with:
 * - DutyPe branding (logo, colors)
 * - Job title, salary, vacancies (prominent)
 * - Location, timing, category
 * - QR code or app download link
 * 
 * Perfect for WhatsApp, Instagram, Facebook sharing
 */
@Singleton
class JobShareImageGenerator @Inject constructor() {
    
    companion object {
        // Image dimensions (optimized for social media)
        private const val IMAGE_WIDTH = 1080
        private const val IMAGE_HEIGHT = 1350 // 4:5 ratio for Instagram
        
        // Brand colors
        private val PRIMARY_BLUE = Color.parseColor("#3B82F6")
        private val DARK_BLUE = Color.parseColor("#1E40AF")
        private val LIGHT_BLUE = Color.parseColor("#DBEAFE")
        private val SUCCESS_GREEN = Color.parseColor("#10B981")
        private val ORANGE = Color.parseColor("#F59E0B")
        private val WHITE = Color.WHITE
        private val BLACK = Color.parseColor("#1F2937")
        private val GRAY = Color.parseColor("#6B7280")
        private val LIGHT_GRAY = Color.parseColor("#F3F4F6")
    }
    
    /**
     * Generate a shareable job image
     */
    suspend fun generateJobImage(
        context: Context,
        job: JobListing
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            Timber.d("🖼️ SHARE: Generating image for job ${job.id}")
            
            // Create bitmap
            val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Draw the job card
            drawJobCard(canvas, job)
            
            // Save to cache directory
            val file = File(context.cacheDir, "job_share_${job.id}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            // Get content URI via FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Timber.d("🖼️ SHARE: ✅ Image generated: $uri")
            Result.success(uri)
            
        } catch (e: Exception) {
            Timber.e(e, "🖼️ SHARE: Error generating image")
            Result.failure(e)
        }
    }
    
    /**
     * Share job with generated image
     */
    suspend fun shareJob(
        context: Context,
        job: JobListing
    ): Result<Unit> {
        return try {
            val imageUri = generateJobImage(context, job).getOrThrow()
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, job.getShareableText())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Job"))
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "🖼️ SHARE: Error sharing job")
            Result.failure(e)
        }
    }
    
    // ==========================================
    // DRAWING METHODS
    // ==========================================
    
    private fun drawJobCard(canvas: Canvas, job: JobListing) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        
        // Background gradient
        drawBackground(canvas, width, height)
        
        // Header with branding
        drawHeader(canvas, width)
        
        // Main content card
        drawMainCard(canvas, job, width, height)
        
        // Footer with CTA
        drawFooter(canvas, job, width, height)
    }
    
    private fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        // Gradient background
        val gradient = LinearGradient(
            0f, 0f, 0f, height,
            intArrayOf(PRIMARY_BLUE, DARK_BLUE, Color.parseColor("#0F172A")),
            floatArrayOf(0f, 0.3f, 1f),
            Shader.TileMode.CLAMP
        )
        
        val paint = Paint().apply {
            shader = gradient
        }
        
        canvas.drawRect(0f, 0f, width, height, paint)
        
        // Decorative circles
        val circlePaint = Paint().apply {
            color = Color.argb(30, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width * 0.9f, height * 0.1f, 150f, circlePaint)
        canvas.drawCircle(width * 0.1f, height * 0.85f, 100f, circlePaint)
    }
    
    private fun drawHeader(canvas: Canvas, width: Float) {
        val headerHeight = 180f
        
        // DutyPe Logo Text
        val logoPaint = Paint().apply {
            color = WHITE
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val logoText = "DutyPe"
        val logoWidth = logoPaint.measureText(logoText)
        canvas.drawText(logoText, (width - logoWidth) / 2, 100f, logoPaint)
        
        // Tagline
        val taglinePaint = Paint().apply {
            color = Color.argb(200, 255, 255, 255)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val tagline = "Hyperlocal Jobs • Zero Fraud"
        val taglineWidth = taglinePaint.measureText(tagline)
        canvas.drawText(tagline, (width - taglineWidth) / 2, 145f, taglinePaint)
    }
    
    private fun drawMainCard(canvas: Canvas, job: JobListing, width: Float, height: Float) {
        val cardMargin = 40f
        val cardTop = 200f
        val cardBottom = height - 280f
        val cardRadius = 24f
        
        // Card background
        val cardPaint = Paint().apply {
            color = WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
            setShadowLayer(20f, 0f, 10f, Color.argb(50, 0, 0, 0))
        }
        
        val cardRect = RectF(cardMargin, cardTop, width - cardMargin, cardBottom)
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)
        
        var yOffset = cardTop + 50f
        val contentMargin = cardMargin + 40f
        val contentWidth = width - (contentMargin * 2)
        
        // URGENT badge (if applicable)
        if (job.isUrgent()) {
            yOffset = drawUrgentBadge(canvas, contentMargin, yOffset, contentWidth)
        }
        
        // Job Title (prominent)
        yOffset = drawJobTitle(canvas, job.title, contentMargin, yOffset, contentWidth)
        
        // Company Name
        yOffset = drawCompanyName(canvas, job.companyName.ifEmpty { job.company }, contentMargin, yOffset)
        
        // Divider
        yOffset = drawDivider(canvas, contentMargin, yOffset, contentWidth)
        
        // Key Info Grid (Salary, Vacancies, Location, Timing)
        yOffset = drawKeyInfoGrid(canvas, job, contentMargin, yOffset, contentWidth)
        
        // Category Badge
        yOffset = drawCategoryBadge(canvas, job.category, contentMargin, yOffset)
        
        // Benefits (if any)
        if (job.benefits.isNotEmpty()) {
            yOffset = drawBenefits(canvas, job.benefits, contentMargin, yOffset, contentWidth)
        }
        
        // Trust Badge
        drawTrustBadge(canvas, job.employerTrustTier, contentMargin, yOffset)
    }
    
    private fun drawUrgentBadge(canvas: Canvas, x: Float, y: Float, width: Float): Float {
        val badgePaint = Paint().apply {
            color = Color.parseColor("#FEE2E2")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val badgeRect = RectF(x, y, x + 140f, y + 36f)
        canvas.drawRoundRect(badgeRect, 18f, 18f, badgePaint)
        
        val textPaint = Paint().apply {
            color = Color.parseColor("#DC2626")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        canvas.drawText("🔥 URGENT", x + 12f, y + 26f, textPaint)
        
        return y + 56f
    }
    
    private fun drawJobTitle(canvas: Canvas, title: String, x: Float, y: Float, maxWidth: Float): Float {
        val paint = Paint().apply {
            color = BLACK
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        // Word wrap if needed
        val words = title.split(" ")
        var currentLine = ""
        var currentY = y
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) > maxWidth) {
                canvas.drawText(currentLine, x, currentY, paint)
                currentY += 60f
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
            currentY += 60f
        }
        
        return currentY + 10f
    }
    
    private fun drawCompanyName(canvas: Canvas, company: String, x: Float, y: Float): Float {
        val paint = Paint().apply {
            color = GRAY
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        canvas.drawText(company.ifEmpty { "Employer" }, x, y, paint)
        return y + 50f
    }
    
    private fun drawDivider(canvas: Canvas, x: Float, y: Float, width: Float): Float {
        val paint = Paint().apply {
            color = LIGHT_GRAY
            strokeWidth = 2f
        }
        
        canvas.drawLine(x, y, x + width, y, paint)
        return y + 30f
    }
    
    private fun drawKeyInfoGrid(canvas: Canvas, job: JobListing, x: Float, y: Float, width: Float): Float {
        val itemWidth = width / 2
        var currentY = y
        
        // Row 1: Salary & Vacancies
        drawInfoItem(canvas, "💰", "Salary", "₹${job.payAmount} ${job.payType}", x, currentY, itemWidth, PRIMARY_BLUE)
        drawInfoItem(canvas, "👥", "Vacancies", "${job.vacancies} Opening${if (job.vacancies > 1) "s" else ""}", x + itemWidth, currentY, itemWidth, SUCCESS_GREEN)
        currentY += 120f
        
        // Row 2: Location & Timing
        drawInfoItem(canvas, "📍", "Location", job.location.take(25) + if (job.location.length > 25) "..." else "", x, currentY, itemWidth, ORANGE)
        drawInfoItem(canvas, "⏰", "Timing", job.shiftTiming.ifEmpty { job.timing }.take(20), x + itemWidth, currentY, itemWidth, GRAY)
        currentY += 120f
        
        return currentY + 20f
    }
    
    private fun drawInfoItem(canvas: Canvas, emoji: String, label: String, value: String, x: Float, y: Float, width: Float, accentColor: Int) {
        // Background
        val bgPaint = Paint().apply {
            color = Color.argb(20, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val bgRect = RectF(x, y, x + width - 20f, y + 100f)
        canvas.drawRoundRect(bgRect, 12f, 12f, bgPaint)
        
        // Emoji
        val emojiPaint = Paint().apply {
            textSize = 36f
            isAntiAlias = true
        }
        canvas.drawText(emoji, x + 16f, y + 40f, emojiPaint)
        
        // Label
        val labelPaint = Paint().apply {
            color = GRAY
            textSize = 22f
            isAntiAlias = true
        }
        canvas.drawText(label, x + 60f, y + 35f, labelPaint)
        
        // Value
        val valuePaint = Paint().apply {
            color = BLACK
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(value, x + 60f, y + 75f, valuePaint)
    }
    
    private fun drawCategoryBadge(canvas: Canvas, category: String, x: Float, y: Float): Float {
        val badgePaint = Paint().apply {
            color = LIGHT_BLUE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val textPaint = Paint().apply {
            color = PRIMARY_BLUE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val badgeText = "🏷️ $category"
        val textWidth = textPaint.measureText(badgeText)
        
        val badgeRect = RectF(x, y, x + textWidth + 32f, y + 44f)
        canvas.drawRoundRect(badgeRect, 22f, 22f, badgePaint)
        
        canvas.drawText(badgeText, x + 16f, y + 32f, textPaint)
        
        return y + 70f
    }
    
    private fun drawBenefits(canvas: Canvas, benefits: List<String>, x: Float, y: Float, width: Float): Float {
        val labelPaint = Paint().apply {
            color = GRAY
            textSize = 24f
            isAntiAlias = true
        }
        
        canvas.drawText("Benefits:", x, y, labelPaint)
        
        val benefitPaint = Paint().apply {
            color = SUCCESS_GREEN
            textSize = 22f
            isAntiAlias = true
        }
        
        val benefitsText = benefits.take(3).joinToString(" • ") { "✓ $it" }
        canvas.drawText(benefitsText.take(50), x, y + 35f, benefitPaint)
        
        return y + 60f
    }
    
    private fun drawTrustBadge(canvas: Canvas, trustTier: String, x: Float, y: Float) {
        val (badgeColor, badgeText) = when (trustTier) {
            "BUSINESS" -> Pair(Color.parseColor("#7C3AED"), "🏢 Business Verified")
            "TRUSTED" -> Pair(SUCCESS_GREEN, "⭐ Trusted Employer")
            else -> Pair(PRIMARY_BLUE, "✓ Verified Employer")
        }
        
        val badgePaint = Paint().apply {
            color = Color.argb(30, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor))
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val textPaint = Paint().apply {
            color = badgeColor
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val textWidth = textPaint.measureText(badgeText)
        val badgeRect = RectF(x, y, x + textWidth + 32f, y + 40f)
        canvas.drawRoundRect(badgeRect, 20f, 20f, badgePaint)
        
        canvas.drawText(badgeText, x + 16f, y + 28f, textPaint)
    }
    
    private fun drawFooter(canvas: Canvas, job: JobListing, width: Float, height: Float) {
        val footerTop = height - 250f
        
        // CTA Button
        val buttonPaint = Paint().apply {
            color = SUCCESS_GREEN
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val buttonRect = RectF(60f, footerTop, width - 60f, footerTop + 70f)
        canvas.drawRoundRect(buttonRect, 35f, 35f, buttonPaint)
        
        val buttonTextPaint = Paint().apply {
            color = WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val buttonText = "📲 Apply Now on DutyPe"
        val buttonTextWidth = buttonTextPaint.measureText(buttonText)
        canvas.drawText(buttonText, (width - buttonTextWidth) / 2, footerTop + 48f, buttonTextPaint)
        
        // App download info
        val infoPaint = Paint().apply {
            color = Color.argb(200, 255, 255, 255)
            textSize = 24f
            isAntiAlias = true
        }
        
        val infoText = "Download DutyPe from Play Store"
        val infoWidth = infoPaint.measureText(infoText)
        canvas.drawText(infoText, (width - infoWidth) / 2, footerTop + 120f, infoPaint)
        
        // Play Store link
        val linkPaint = Paint().apply {
            color = WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val linkText = "play.google.com/store/apps/details?id=com.dutype.app"
        val linkWidth = linkPaint.measureText(linkText)
        canvas.drawText(linkText, (width - linkWidth) / 2, footerTop + 155f, linkPaint)
        
        // Posted time
        val timePaint = Paint().apply {
            color = Color.argb(150, 255, 255, 255)
            textSize = 20f
            isAntiAlias = true
        }
        
        val timeText = "Posted ${job.getTimeAgoDisplayText()}"
        val timeWidth = timePaint.measureText(timeText)
        canvas.drawText(timeText, (width - timeWidth) / 2, footerTop + 200f, timePaint)
    }
}
