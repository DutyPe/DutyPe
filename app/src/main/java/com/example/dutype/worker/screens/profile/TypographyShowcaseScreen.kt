package com.example.dutype.worker.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.WorkerColors

/**
 * Typography Showcase Screen
 * 
 * Displays 20 different text styles with various font families and weights
 * to help pick the best typography for the app.
 */
@Composable
fun TypographyShowcaseScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
    }
    
    // Define font families
    val roboto = FontFamily.Default
    val sansSerif = FontFamily.SansSerif
    val serif = FontFamily.Serif
    val monospace = FontFamily.Monospace
    
    // Sample text for display
    val sampleTitle = "DutyPe - Find Jobs Near You"
    val sampleBody = "Discover thousands of local job opportunities. Apply instantly and get hired today!"
    val samplePrice = "₹500/day"
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
    ) {
        CommonHeader(
            title = "Typography Showcase",
            onBackClick = { navController.popBackStack() },
            showBackButton = true,
            backgroundColor = WorkerColors.CardBackground
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Style 1: Clean Modern (Current)
            item {
                TypographyCard(
                    styleName = "1. Clean Modern (Current)",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF1F2937)
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF10B981)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 2: Bold Headlines
            item {
                TypographyCard(
                    styleName = "2. Bold Headlines",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = Color(0xFF111827),
                        letterSpacing = (-0.5).sp
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color(0xFF4B5563)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color(0xFF059669)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 3: Light & Airy
            item {
                TypographyCard(
                    styleName = "3. Light & Airy",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Light,
                        fontSize = 24.sp,
                        color = Color(0xFF374151),
                        letterSpacing = 1.sp
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = Color(0xFF10B981)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 4: Compact Dense
            item {
                TypographyCard(
                    styleName = "4. Compact Dense",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF1F2937),
                        letterSpacing = (-0.3).sp
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                        lineHeight = 16.sp
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF10B981)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 5: Serif Elegant
            item {
                TypographyCard(
                    styleName = "5. Serif Elegant",
                    titleStyle = TextStyle(
                        fontFamily = serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF1F2937)
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = serif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = sansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF10B981)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 6: High Contrast
            item {
                TypographyCard(
                    styleName = "6. High Contrast",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color.Black
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color(0xFF374151)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = Color(0xFF047857)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 7: Soft Rounded
            item {
                TypographyCard(
                    styleName = "7. Soft Rounded",
                    titleStyle = TextStyle(
                        fontFamily = sansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = Color(0xFF374151),
                        letterSpacing = 0.5.sp
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = sansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF),
                        lineHeight = 22.sp
                    ),
                    priceStyle = TextStyle(
                        fontFamily = sansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF34D399)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 8: Tech Monospace
            item {
                TypographyCard(
                    styleName = "8. Tech Monospace",
                    titleStyle = TextStyle(
                        fontFamily = monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1F2937)
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF10B981)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 9: Large Title Focus
            item {
                TypographyCard(
                    styleName = "9. Large Title Focus",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = Color(0xFF111827),
                        lineHeight = 30.sp
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF10B981)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 10: Minimal Thin
            item {
                TypographyCard(
                    styleName = "10. Minimal Thin",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Thin,
                        fontSize = 22.sp,
                        color = Color(0xFF1F2937),
                        letterSpacing = 2.sp
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Thin,
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        color = Color(0xFF10B981)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 11: Newspaper Classic
            item {
                TypographyCard(
                    styleName = "11. Newspaper Classic",
                    titleStyle = TextStyle(
                        fontFamily = serif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = Color.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = serif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color(0xFF374151),
                        lineHeight = 22.sp
                    ),
                    priceStyle = TextStyle(
                        fontFamily = sansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF059669)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 12: Playful Rounded
            item {
                TypographyCard(
                    styleName = "12. Playful Rounded",
                    titleStyle = TextStyle(
                        fontFamily = sansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF7C3AED),
                        letterSpacing = 0.5.sp
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = sansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = sansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFFF59E0B)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 13: Corporate Professional
            item {
                TypographyCard(
                    styleName = "13. Corporate Professional",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E3A5F),
                        letterSpacing = 0.sp
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0D9488)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 14: Warm Friendly
            item {
                TypographyCard(
                    styleName = "14. Warm Friendly",
                    titleStyle = TextStyle(
                        fontFamily = sansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFFB45309)
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = sansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color(0xFF78716C)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = sansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF059669)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 15: Ultra Bold Impact
            item {
                TypographyCard(
                    styleName = "15. Ultra Bold Impact",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = Color(0xFF111827),
                        letterSpacing = (-1).sp
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFF4B5563)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = Color(0xFF10B981)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 16: Subtle Elegant
            item {
                TypographyCard(
                    styleName = "16. Subtle Elegant",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        color = Color(0xFF374151),
                        letterSpacing = 1.5.sp
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF),
                        letterSpacing = 0.5.sp
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Color(0xFF059669)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 17: Dark Mode Ready
            item {
                TypographyCard(
                    styleName = "17. Dark Mode Ready",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFFF9FAFB)
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color(0xFFD1D5DB)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF34D399)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice,
                    cardBackground = Color(0xFF1F2937)
                )
            }
            
            // Style 18: Swiggy/Zomato Style
            item {
                TypographyCard(
                    styleName = "18. Swiggy/Zomato Style",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF3D4152),
                        letterSpacing = (-0.3).sp
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = Color(0xFF93959F)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF60B246)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 19: LinkedIn Professional
            item {
                TypographyCard(
                    styleName = "19. LinkedIn Professional",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF000000),
                        letterSpacing = 0.sp
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        lineHeight = 20.sp
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF0A66C2)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            // Style 20: WhatsApp Clean
            item {
                TypographyCard(
                    styleName = "20. WhatsApp Clean",
                    titleStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        color = Color(0xFF111B21)
                    ),
                    bodyStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color(0xFF667781)
                    ),
                    priceStyle = TextStyle(
                        fontFamily = roboto,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Color(0xFF25D366)
                    ),
                    sampleTitle = sampleTitle,
                    sampleBody = sampleBody,
                    samplePrice = samplePrice
                )
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TypographyCard(
    styleName: String,
    titleStyle: TextStyle,
    bodyStyle: TextStyle,
    priceStyle: TextStyle,
    sampleTitle: String,
    sampleBody: String,
    samplePrice: String,
    cardBackground: Color = Color.White
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Style name badge
            Text(
                text = styleName,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                ),
                modifier = Modifier
                    .background(Color(0xFF6366F1), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Title sample
            Text(
                text = sampleTitle,
                style = titleStyle
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Body sample
            Text(
                text = sampleBody,
                style = bodyStyle
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Price sample
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Salary: ", style = bodyStyle)
                Text(text = samplePrice, style = priceStyle)
            }
        }
    }
}
