package com.example.dutype.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Referral Service - Handles referral code generation, QR codes, and tracking
 * Works for both Workers and Employers
 */
@Singleton
class ReferralService @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    data class ReferralData(
        val referralCode: String,
        val referralLink: String,
        val totalReferrals: Int,
        val successfulReferrals: Int,
        val pendingReferrals: Int,
        val totalEarnings: Double,
        val pendingEarnings: Double
    )
    
    data class ReferralHistoryItem(
        val id: String,
        val referredUserId: String,
        val referredUserName: String,
        val referredUserRole: String,
        val status: ReferralStatus,
        val earnings: Double,
        val createdAt: Long,
        val completedAt: Long?
    )
    
    enum class ReferralStatus {
        PENDING,
        COMPLETED,
        EXPIRED
    }
    
    /**
     * Generate or get existing referral code for user
     */
    suspend fun getOrCreateReferralCode(userId: String, userRole: String): Result<String> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            
            val existingCode = userDoc.getString("referralCode")
            if (!existingCode.isNullOrEmpty()) {
                return Result.success(existingCode)
            }
            
            // Generate new code
            val prefix = if (userRole == "WORKER") "WRK" else "EMP"
            val randomPart = (1000..9999).random()
            val newCode = "$prefix$randomPart"
            
            // Save to user document
            firestore.collection("users").document(userId)
                .update(
                    mapOf(
                        "referralCode" to newCode,
                        "referralCodeCreatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            
            Result.success(newCode)
        } catch (e: Exception) {
            Timber.e(e, "Error getting/creating referral code")
            Result.failure(e)
        }
    }
    
    /**
     * Get referral statistics for user
     */
    suspend fun getReferralStats(userId: String): Result<ReferralData> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val referralCode = userDoc.getString("referralCode") ?: ""
            
            // Get referral history
            val referrals = firestore.collection("referrals")
                .whereEqualTo("referrerId", userId)
                .get()
                .await()
            
            var totalReferrals = 0
            var successfulReferrals = 0
            var pendingReferrals = 0
            var totalEarnings = 0.0
            var pendingEarnings = 0.0
            
            referrals.documents.forEach { doc ->
                totalReferrals++
                val status = doc.getString("status") ?: "PENDING"
                val earnings = doc.getDouble("earnings") ?: 0.0
                
                when (status) {
                    "COMPLETED" -> {
                        successfulReferrals++
                        totalEarnings += earnings
                    }
                    "PENDING" -> {
                        pendingReferrals++
                        pendingEarnings += 30.0 // Default pending amount
                    }
                }
            }
            
            val referralLink = "https://dutypein.page.link/refer?code=$referralCode"
            
            Result.success(ReferralData(
                referralCode = referralCode,
                referralLink = referralLink,
                totalReferrals = totalReferrals,
                successfulReferrals = successfulReferrals,
                pendingReferrals = pendingReferrals,
                totalEarnings = totalEarnings,
                pendingEarnings = pendingEarnings
            ))
        } catch (e: Exception) {
            Timber.e(e, "Error getting referral stats")
            Result.failure(e)
        }
    }
    
    /**
     * Get referral history
     */
    suspend fun getReferralHistory(userId: String): Result<List<ReferralHistoryItem>> {
        return try {
            val referrals = firestore.collection("referrals")
                .whereEqualTo("referrerId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()
            
            val history = referrals.documents.mapNotNull { doc ->
                try {
                    ReferralHistoryItem(
                        id = doc.id,
                        referredUserId = doc.getString("referredUserId") ?: "",
                        referredUserName = doc.getString("referredUserName") ?: "Unknown",
                        referredUserRole = doc.getString("referredUserRole") ?: "",
                        status = ReferralStatus.valueOf(doc.getString("status") ?: "PENDING"),
                        earnings = doc.getDouble("earnings") ?: 0.0,
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        completedAt = doc.getLong("completedAt")
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(history)
        } catch (e: Exception) {
            Timber.e(e, "Error getting referral history")
            Result.failure(e)
        }
    }
    
    /**
     * Apply referral code during signup
     */
    suspend fun applyReferralCode(
        newUserId: String,
        newUserName: String,
        newUserRole: String,
        referralCode: String
    ): Result<Boolean> {
        return try {
            // Find referrer by code
            val referrerQuery = firestore.collection("users")
                .whereEqualTo("referralCode", referralCode)
                .limit(1)
                .get()
                .await()
            
            if (referrerQuery.isEmpty) {
                return Result.failure(Exception("Invalid referral code"))
            }
            
            val referrerDoc = referrerQuery.documents.first()
            val referrerId = referrerDoc.id
            
            // Create referral record
            val referralData = hashMapOf(
                "referrerId" to referrerId,
                "referredUserId" to newUserId,
                "referredUserName" to newUserName,
                "referredUserRole" to newUserRole,
                "referralCode" to referralCode,
                "status" to "PENDING",
                "earnings" to 0.0,
                "createdAt" to System.currentTimeMillis()
            )
            
            firestore.collection("referrals").add(referralData).await()
            
            // Update new user's document
            firestore.collection("users").document(newUserId)
                .update("referredBy", referrerId, "referredByCode", referralCode)
                .await()
            
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Error applying referral code")
            Result.failure(e)
        }
    }
    
    /**
     * Generate QR code bitmap for referral code
     */
    fun generateQRCode(content: String, size: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
    
    /**
     * Generate referral card image with QR code
     */
    fun generateReferralCard(
        context: Context,
        referralCode: String,
        userName: String,
        userRole: String
    ): File {
        val width = 1080
        val height = 1920
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background gradient
        val bgPaint = Paint().apply {
            shader = android.graphics.LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    Color.parseColor("#2193b0"),
                    Color.parseColor("#6dd5ed"),
                    Color.WHITE
                ),
                floatArrayOf(0f, 0.5f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        
        // DutyPe logo text
        val logoPaint = Paint().apply {
            color = Color.WHITE
            textSize = 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("DutyPe", 80f, 150f, logoPaint)
        
        // Tagline
        val taglinePaint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            isAntiAlias = true
        }
        canvas.drawText("Connecting Local Jobs with Local Workers", 80f, 210f, taglinePaint)
        
        // White card background
        val cardPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        val cardRect = RectF(60f, 300f, width - 60f, height - 200f)
        canvas.drawRoundRect(cardRect, 40f, 40f, cardPaint)
        
        // User name
        val namePaint = Paint().apply {
            color = Color.parseColor("#1F2937")
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(userName.uppercase(), width / 2f, 420f, namePaint)
        
        // Role badge
        val rolePaint = Paint().apply {
            color = Color.parseColor("#2193b0")
            textSize = 32f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val roleText = if (userRole == "WORKER") "Job Seeker" else "Employer"
        canvas.drawText(roleText, width / 2f, 480f, rolePaint)
        
        // QR Code
        val referralLink = "https://dutypein.page.link/refer?code=$referralCode"
        val qrBitmap = generateQRCode(referralLink, 400)
        canvas.drawBitmap(qrBitmap, (width - 400f) / 2, 550f, null)
        
        // Referral code
        val codeLabelPaint = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Referral Code", width / 2f, 1020f, codeLabelPaint)
        
        val codePaint = Paint().apply {
            color = Color.parseColor("#2193b0")
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(referralCode, width / 2f, 1100f, codePaint)
        
        // Instructions
        val instructionPaint = Paint().apply {
            color = Color.parseColor("#374151")
            textSize = 32f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Scan QR code or use code to join DutyPe", width / 2f, 1200f, instructionPaint)
        canvas.drawText("and earn ₹30 bonus!", width / 2f, 1250f, instructionPaint)
        
        // Rewards info
        val rewardPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🎁 You get ₹30 | Friend gets ₹30", width / 2f, 1350f, rewardPaint)
        
        // Download CTA
        val ctaPaint = Paint().apply {
            color = Color.parseColor("#1F2937")
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Download from Play Store", width / 2f, 1450f, ctaPaint)
        
        // Save to file
        val file = File(context.cacheDir, "referral_card_$referralCode.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        return file
    }
}
