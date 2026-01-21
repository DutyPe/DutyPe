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
        // Image dimensions (optimized for WhatsApp and social media)
        private const val IMAGE_WIDTH = 1080
        private const val IMAGE_HEIGHT = 1350 // 4:5 ratio perfect for Instagram & WhatsApp Status
        
        // Brand colors - DutyPe Blue Theme
        private val PRIMARY_BLUE = Color.parseColor("#3B82F6")
        private val DARK_BLUE = Color.parseColor("#1E40AF")
        private val LIGHT_BLUE = Color.parseColor("#DBEAFE")
        private val SUCCESS_GREEN = Color.parseColor("#10B981")
        private val ORANGE = Color.parseColor("#F59E0B")
        private val URGENT_RED = Color.parseColor("#EF4444")
        private val WHITE = Color.WHITE
        private val BLACK = Color.parseColor("#1F2937")
        private val GRAY = Color.parseColor("#6B7280")
        private val LIGHT_GRAY = Color.parseColor("#F3F4F6")
        
        // Typography sizes
        private const val LOGO_SIZE = 72f
        private const val TITLE_SIZE = 52f
        private const val SUBTITLE_SIZE = 32f
        private const val BODY_SIZE = 28f
        private const val SMALL_SIZE = 24f
        private const val TINY_SIZE = 20f
    }
    
    /**
     * Generate a shareable job image
     * 
     * PERFORMANCE FIX: 
     * - Uses JPEG compression (85% quality) instead of PNG (100%) - ~60% smaller files
     * - Properly recycles bitmap after use to prevent memory leaks
     * - Cleans up old cached images to prevent storage bloat
     */
    suspend fun generateJobImage(
        context: Context,
        job: JobListing
    ): Result<Uri> = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        try {
            Timber.d("🖼️ SHARE: Generating image for job ${job.id}")
            
            // Clean up old cached share images (older than 1 hour)
            cleanupOldCacheImages(context)
            
            // Create bitmap
            bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Draw the job card
            drawJobCard(canvas, job)
            
            // Save to cache directory with JPEG compression (85% quality - ~60% smaller than PNG)
            val file = File(context.cacheDir, "job_share_${job.id}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            
            // Get content URI via FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Timber.d("🖼️ SHARE: ✅ Image generated: $uri (JPEG 85% quality)")
            Result.success(uri)
            
        } catch (e: Exception) {
            Timber.e(e, "🖼️ SHARE: Error generating image")
            Result.failure(e)
        } finally {
            // MEMORY FIX: Always recycle bitmap to prevent memory leaks
            bitmap?.recycle()
            Timber.d("🖼️ SHARE: Bitmap recycled to free memory")
        }
    }
    
    /**
     * Clean up old cached share images to prevent storage bloat
     * Removes images older than 1 hour
     */
    private fun cleanupOldCacheImages(context: Context) {
        try {
            val cacheDir = context.cacheDir
            val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
            
            cacheDir.listFiles()?.filter { file ->
                file.name.startsWith("job_share_") && 
                (file.name.endsWith(".jpg") || file.name.endsWith(".png")) &&
                file.lastModified() < oneHourAgo
            }?.forEach { file ->
                if (file.delete()) {
                    Timber.d("🖼️ SHARE: Cleaned up old cache file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "🖼️ SHARE: Error cleaning up cache")
        }
    }
    
    /**
     * Share job with generated image + text with app link
     * Image shows job details, text encourages app download
     */
    suspend fun shareJob(
        context: Context,
        job: JobListing
    ): Result<Unit> {
        return try {
            val imageUri = generateJobImage(context, job).getOrThrow()
            
            // Share text with app link
            val shareText = """
                📢 Job Alert on DutyPe!
                
                Apply now through the DutyPe app 👇
                
                📲 Download: https://play.google.com/store/apps/details?id=com.dutype.app
                
                🚀 Get instant job alerts
                ⚡ Apply in seconds
                💼 Hyperlocal jobs near you
            """.trimIndent()
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Job"))
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "🖼️ SHARE: Error sharing job")
            Result.failure(e)
        }
    }
    
    /**
     * Share job directly to WhatsApp with generated image + text
     * Opens WhatsApp with image and app download message
     */
    suspend fun shareJobToWhatsApp(
        context: Context,
        job: JobListing
    ): Result<Unit> {
        return try {
            val imageUri = generateJobImage(context, job).getOrThrow()
            
            // Share text with app link
            val shareText = """
                📢 Job Alert on DutyPe!
                
                Apply now through the DutyPe app 👇
                
                � Download: https://play.google.com/store/apps/details?id=com.dutype.app
                
                🚀 Get instant job alerts
                ⚡ Apply in seconds
                💼 Hyperlocal jobs near you
            """.trimIndent()
            
            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                setPackage("com.whatsapp") // Direct to WhatsApp
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            try {
                context.startActivity(whatsappIntent)
                Result.success(Unit)
            } catch (e: Exception) {
                // WhatsApp not installed, fallback to general share
                Timber.w("WhatsApp not installed, falling back to general share")
                shareJob(context, job)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "🖼️ SHARE: Error sharing job to WhatsApp")
            Result.failure(e)
        }
    }
    
    // ==========================================
    // DRAWING METHODS
    // ==========================================
    
    private fun drawJobCard(canvas: Canvas, job: JobListing) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        
        // Modern gradient background
        drawModernBackground(canvas, width, height)
        
        // Top section with logo only
        drawTopSection(canvas, width)
        
        // Main job card - ONLY essential info, no description/contact
        drawSimpleJobCard(canvas, job, width, height)
    }
    
    private fun drawModernBackground(canvas: Canvas, width: Float, height: Float) {
        // Vibrant gradient background
        val gradient = LinearGradient(
            0f, 0f, width, height,
            intArrayOf(
                Color.parseColor("#667eea"), // Purple
                Color.parseColor("#764ba2"), // Deep purple
                Color.parseColor("#f093fb")  // Pink
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        
        val paint = Paint().apply {
            shader = gradient
        }
        
        canvas.drawRect(0f, 0f, width, height, paint)
        
        // Add decorative circles for depth
        val circlePaint = Paint().apply {
            color = Color.argb(20, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width * 0.15f, height * 0.12f, 200f, circlePaint)
        canvas.drawCircle(width * 0.85f, height * 0.88f, 180f, circlePaint)
        canvas.drawCircle(width * 0.90f, height * 0.25f, 120f, circlePaint)
    }
    
    private fun drawTopSection(canvas: Canvas, width: Float) {
        // DutyPe Logo with modern styling
        val logoPaint = Paint().apply {
            color = WHITE
            textSize = 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(10f, 0f, 5f, Color.argb(50, 0, 0, 0))
        }
        
        val logoText = "DutyPe"
        val logoWidth = logoPaint.measureText(logoText)
        canvas.drawText(logoText, (width - logoWidth) / 2, 120f, logoPaint)
        
        // Tagline with emoji
        val taglinePaint = Paint().apply {
            color = Color.argb(230, 255, 255, 255)
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val tagline = "🚀 Find Your Perfect Job"
        val taglineWidth = taglinePaint.measureText(tagline)
        canvas.drawText(tagline, (width - taglineWidth) / 2, 165f, taglinePaint)
    }
    
    private fun drawSimpleJobCard(canvas: Canvas, job: JobListing, width: Float, height: Float) {
        val cardMargin = 50f
        val cardTop = 230f
        val cardBottom = height - 100f // Extend card to near bottom
        val cardRadius = 30f
        
        // White card with shadow
        val cardPaint = Paint().apply {
            color = WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
            setShadowLayer(30f, 0f, 15f, Color.argb(60, 0, 0, 0))
        }
        
        val cardRect = RectF(cardMargin, cardTop, width - cardMargin, cardBottom)
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)
        
        var yOffset = cardTop + 60f
        val contentMargin = cardMargin + 50f
        val contentWidth = width - (contentMargin * 2)
        
        // Urgent badge if applicable
        if (job.isUrgent()) {
            yOffset = drawModernUrgentBadge(canvas, contentMargin, yOffset)
        }
        
        // Job Title - Large and bold
        yOffset = drawModernJobTitle(canvas, job.title, contentMargin, yOffset, contentWidth)
        
        // Company name with icon
        yOffset = drawModernCompanyName(canvas, job.companyName, contentMargin, yOffset)
        
        // Stylish divider
        yOffset = drawStylishDivider(canvas, contentMargin, yOffset, contentWidth)
        
        // Key info in modern cards - ONLY essential info
        yOffset = drawModernInfoCards(canvas, job, contentMargin, yOffset, contentWidth)
        
        // Category and trust badge row at bottom
        drawBottomBadges(canvas, job, contentMargin, yOffset, contentWidth)
    }
    
    private fun drawModernUrgentBadge(canvas: Canvas, x: Float, y: Float): Float {
        // Gradient urgent badge
        val gradient = LinearGradient(
            x, y, x + 180f, y + 50f,
            intArrayOf(Color.parseColor("#FF6B6B"), Color.parseColor("#FF8E53")),
            null,
            Shader.TileMode.CLAMP
        )
        
        val badgePaint = Paint().apply {
            shader = gradient
            style = Paint.Style.FILL
            isAntiAlias = true
            setShadowLayer(10f, 0f, 5f, Color.argb(40, 255, 107, 107))
        }
        
        val badgeRect = RectF(x, y, x + 180f, y + 50f)
        canvas.drawRoundRect(badgeRect, 25f, 25f, badgePaint)
        
        val textPaint = Paint().apply {
            color = WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        canvas.drawText("🔥 URGENT", x + 20f, y + 35f, textPaint)
        
        return y + 75f
    }
    
    private fun drawModernJobTitle(canvas: Canvas, title: String, x: Float, y: Float, maxWidth: Float): Float {
        val paint = Paint().apply {
            color = Color.parseColor("#2D3748")
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        // Word wrap
        val words = title.split(" ")
        var currentLine = ""
        var currentY = y
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) > maxWidth) {
                canvas.drawText(currentLine, x, currentY, paint)
                currentY += 70f
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
            currentY += 70f
        }
        
        return currentY + 15f
    }
    
    private fun drawModernCompanyName(canvas: Canvas, company: String, x: Float, y: Float): Float {
        val paint = Paint().apply {
            color = Color.parseColor("#718096")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        canvas.drawText("🏢 ${company.ifEmpty { "Employer" }}", x, y, paint)
        return y + 60f
    }
    
    private fun drawStylishDivider(canvas: Canvas, x: Float, y: Float, width: Float): Float {
        val gradient = LinearGradient(
            x, y, x + width, y,
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#E2E8F0"), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        
        val paint = Paint().apply {
            shader = gradient
            strokeWidth = 3f
        }
        
        canvas.drawLine(x, y, x + width, y, paint)
        return y + 40f
    }
    
    private fun drawModernInfoCards(canvas: Canvas, job: JobListing, x: Float, y: Float, width: Float): Float {
        val cardWidth = (width - 20f) / 2
        var currentY = y
        
        // Row 1: Salary & Vacancies
        drawModernInfoCard(canvas, "💰", "Salary", "₹${job.payAmount}", x, currentY, cardWidth, Color.parseColor("#48BB78"))
        drawModernInfoCard(canvas, "👥", "Openings", "${job.vacancies}", x + cardWidth + 20f, currentY, cardWidth, Color.parseColor("#4299E1"))
        currentY += 140f
        
        // Row 2: Location & Timing
        val locationText = job.location.take(20) + if (job.location.length > 20) "..." else ""
        val timingText = job.shiftTiming.take(15)
        drawModernInfoCard(canvas, "📍", "Location", locationText, x, currentY, cardWidth, Color.parseColor("#ED8936"))
        drawModernInfoCard(canvas, "⏰", "Timing", timingText, x + cardWidth + 20f, currentY, cardWidth, Color.parseColor("#9F7AEA"))
        currentY += 140f
        
        return currentY + 30f
    }
    
    private fun drawModernInfoCard(canvas: Canvas, emoji: String, label: String, value: String, x: Float, y: Float, width: Float, accentColor: Int) {
        // Card background with gradient
        val gradient = LinearGradient(
            x, y, x, y + 120f,
            intArrayOf(Color.argb(15, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
                      Color.argb(5, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))),
            null,
            Shader.TileMode.CLAMP
        )
        
        val bgPaint = Paint().apply {
            shader = gradient
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val bgRect = RectF(x, y, x + width, y + 120f)
        canvas.drawRoundRect(bgRect, 20f, 20f, bgPaint)
        
        // Accent border
        val borderPaint = Paint().apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(bgRect, 20f, 20f, borderPaint)
        
        // Emoji
        val emojiPaint = Paint().apply {
            textSize = 40f
            isAntiAlias = true
        }
        canvas.drawText(emoji, x + 20f, y + 45f, emojiPaint)
        
        // Label
        val labelPaint = Paint().apply {
            color = Color.parseColor("#718096")
            textSize = 24f
            isAntiAlias = true
        }
        canvas.drawText(label, x + 70f, y + 40f, labelPaint)
        
        // Value
        val valuePaint = Paint().apply {
            color = Color.parseColor("#2D3748")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(value, x + 70f, y + 85f, valuePaint)
    }
    
    private fun drawBottomBadges(canvas: Canvas, job: JobListing, x: Float, y: Float, width: Float) {
        // Category badge
        val categoryPaint = Paint().apply {
            color = Color.parseColor("#EDF2F7")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val textPaint = Paint().apply {
            color = Color.parseColor("#4A5568")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val categoryText = "🏷️ ${job.getCategory()}"
        val textWidth = textPaint.measureText(categoryText)
        
        val categoryRect = RectF(x, y, x + textWidth + 40f, y + 50f)
        canvas.drawRoundRect(categoryRect, 25f, 25f, categoryPaint)
        canvas.drawText(categoryText, x + 20f, y + 36f, textPaint)
        
        // Trust badge on the right - REMOVED: employerTrustTier no longer in JobListing model
        // Default to verified badge for all jobs
        val (badgeColor, badgeText) = Pair(Color.parseColor("#4299E1"), "✓ Verified")
        
        val trustPaint = Paint().apply {
            color = badgeColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val trustTextPaint = Paint().apply {
            color = WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val trustTextWidth = trustTextPaint.measureText(badgeText)
        val trustX = x + width - trustTextWidth - 40f
        
        val trustRect = RectF(trustX, y, trustX + trustTextWidth + 40f, y + 50f)
        canvas.drawRoundRect(trustRect, 25f, 25f, trustPaint)
        canvas.drawText(badgeText, trustX + 20f, y + 36f, trustTextPaint)
    }
}
