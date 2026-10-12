package com.example.dutype.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Data structure representing a promotional slide on Home screens.
 */
data class HomePromoSlide(
    val tag: String,
    val tagBgColor: Color,
    val headline: String,
    val subheadline: String,
    val cta: String,
    val targetKey: String,
    val gradientColors: List<Color>,
    val ctaBgColor: Color,
    val ctaTextColor: Color,
    val imageUrl: String = ""
)

fun announcementToSlide(announcement: com.example.dutype.models.Announcement): HomePromoSlide {
    val (tag, tagBg, gradient) = when (announcement.type) {
        com.example.dutype.models.AnnouncementType.PROMOTION -> Triple(
            "🎁 SPECIAL PROMOTION",
            Color(0xFFF59E0B),
            listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
        )
        com.example.dutype.models.AnnouncementType.FEATURE -> Triple(
            "✨ NEW FEATURE",
            Color(0xFF8B5CF6),
            listOf(Color(0xFF2E1065), Color(0xFF3B0764), Color(0xFF581C87))
        )
        com.example.dutype.models.AnnouncementType.SUCCESS -> Triple(
            "✅ SUCCESS UPDATE",
            Color(0xFF10B981),
            listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF065F46))
        )
        com.example.dutype.models.AnnouncementType.WARNING -> Triple(
            "⚠️ IMPORTANT NOTICE",
            Color(0xFFEF4444),
            listOf(Color(0xFF7F1D1D), Color(0xFF991B1B), Color(0xFFB91C1C))
        )
        else -> Triple(
            "📢 DUTYPE ANNOUNCEMENT",
            Color(0xFF3B82F6),
            listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF1E40AF))
        )
    }
    return HomePromoSlide(
        tag = tag,
        tagBgColor = tagBg,
        headline = announcement.title.ifBlank { "DutyPe Announcement" },
        subheadline = announcement.message,
        cta = "Check Details",
        targetKey = announcement.actionRoute ?: "jobs",
        gradientColors = gradient,
        ctaBgColor = Color.White,
        ctaTextColor = Color(0xFF0F172A)
    )
}

private val DEFAULT_WORKER_PROMOS = listOf(
    HomePromoSlide(
        tag = "⚡ ZERO COMMISSION",
        tagBgColor = Color(0xFFF59E0B),
        headline = "Direct Hiring • 100% Earnings",
        subheadline = "DutyPe takes 0% cut from your daily earnings. You keep 100% of what you earn directly from employers.",
        cta = "Explore Jobs",
        targetKey = "jobs",
        gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)),
        ctaBgColor = Color(0xFF00B159),
        ctaTextColor = Color.White
    ),
    HomePromoSlide(
        tag = "🛡️ DUTYPE VERIFIED",
        tagBgColor = Color(0xFF10B981),
        headline = "Get Hired 3x Faster Today",
        subheadline = "Complete your profile & trade skills to get top recommendations and direct calls.",
        cta = "Complete Profile",
        targetKey = "profile",
        gradientColors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF065F46)),
        ctaBgColor = Color(0xFFFBBF24),
        ctaTextColor = Color(0xFF1E293B)
    ),
    HomePromoSlide(
        tag = "🎁 REFERRAL BONUS",
        tagBgColor = Color(0xFFF59E0B),
        headline = "Earn ₹100 per Referral",
        subheadline = "Invite fellow electricians, plumbers, cooks or drivers. Withdraw instant cash straight to your UPI.",
        cta = "Invite & Earn",
        targetKey = "refer_earn",
        gradientColors = listOf(Color(0xFF78350F), Color(0xFF92400E), Color(0xFFB45309)),
        ctaBgColor = Color.White,
        ctaTextColor = Color(0xFF78350F)
    )
)

private val DEFAULT_EMPLOYER_PROMOS = listOf(
    HomePromoSlide(
        tag = "⚡ HIRE IN MINUTES",
        tagBgColor = Color(0xFFFBBF24),
        headline = "Post a Job & Get Instant Calls",
        subheadline = "Connect with verified, reliable workers near your shop, factory, or home in minutes.",
        cta = "Post a Job",
        targetKey = "post_job",
        gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)),
        ctaBgColor = Color(0xFFFBBF24),
        ctaTextColor = Color(0xFF1E293B)
    ),
    HomePromoSlide(
        tag = "🚨 NEED WORKERS TODAY?",
        tagBgColor = Color(0xFFEF4444),
        headline = "Urgent Need? Instant Matches",
        subheadline = "Broadcast an urgent need. Available workers get notified on their phones instantly.",
        cta = "Post Urgent Need",
        targetKey = "urgent_need",
        gradientColors = listOf(Color(0xFF881337), Color(0xFF9F1239), Color(0xFFBE123C)),
        ctaBgColor = Color.White,
        ctaTextColor = Color(0xFF881337)
    ),
    HomePromoSlide(
        tag = "🎉 REFERRAL CASHBACK",
        tagBgColor = Color(0xFFFCD34D),
        headline = "Earn ₹100 Cashback Every Time",
        subheadline = "Refer fellow business owners, contractors or shopkeepers to DutyPe and earn instant wallet balance.",
        cta = "Refer & Earn",
        targetKey = "refer_earn",
        gradientColors = listOf(Color(0xFF312E81), Color(0xFF3730A3), Color(0xFF4338CA)),
        ctaBgColor = Color(0xFFFBBF24),
        ctaTextColor = Color(0xFF1E293B)
    )
)

/**
 * Promotional Banner Carousel used across Worker and Employer Home Screens.
 * Automatically cycles through promotional slides with dot indicators.
 */
@Composable
fun PromotionalBannerCarousel(
    isWorker: Boolean,
    onAction: (targetKey: String) -> Unit,
    modifier: Modifier = Modifier,
    customSlides: List<HomePromoSlide> = emptyList()
) {
    val slides = remember(isWorker, customSlides) {
        if (customSlides.isNotEmpty()) customSlides
        else if (isWorker) DEFAULT_WORKER_PROMOS
        else DEFAULT_EMPLOYER_PROMOS
    }

    if (slides.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { slides.size })

    // Auto-advance every 4.5 seconds
    LaunchedEffect(slides.size) {
        if (slides.size > 1) {
            while (true) {
                delay(4500)
                val nextPage = (pagerState.currentPage + 1) % slides.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val slide = slides[page]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onAction(slide.targetKey) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(slide.gradientColors))
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tag chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(slide.tagBgColor)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = slide.tag,
                                color = if (slide.tagBgColor == Color.White) Color(0xFF0F172A) else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Headline
                        Text(
                            text = slide.headline,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 22.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Subheadline
                        Text(
                            text = slide.subheadline,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // CTA Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(slide.ctaBgColor)
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = slide.cta,
                                color = slide.ctaTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = slide.ctaTextColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Indicator dots
        if (slides.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                slides.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 16.dp else 5.dp,
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1))
                    )
                }
            }
        }
    }
}
