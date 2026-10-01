package com.example.dutype.components

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R
import com.example.dutype.ui.theme.AppTypography

/** Accent for the small badge on the icon frame; the rest of the empty state stays neutral. */
enum class EmptyTone(val accent: Color) {
    BLUE(Color(0xFF2563EB)),
    GREEN(Color(0xFF16A34A)),
    PURPLE(Color(0xFF7C3AED)),
    ORANGE(Color(0xFFEA580C)),
}

/** Pet illustrations for the screens where a friendly picture says more than an icon. */
enum class EmptyArt(val drawable: Int, val darkDrawable: Int) {
    /** Notifications: a pup bringing a letter. */
    LETTER(R.drawable.pet_dog_letter, R.drawable.pet_dog_letter_dark),
    /** No internet / could not load: a pup holding the unplugged cable. */
    OFFLINE(R.drawable.pet_dog_unplugged, R.drawable.pet_dog_unplugged_dark),
    /** Nothing here yet / waiting: a pup beside an empty bowl. */
    WAITING(R.drawable.pet_dog_bowl, R.drawable.pet_dog_bowl_dark),
    /** No results / look elsewhere: a pup with a magnifier. */
    SEARCH(R.drawable.pet_dog_search, R.drawable.pet_dog_search_dark),
    /** Quiet history: a sleeping cat. */
    QUIET(R.drawable.pet_cat_sleep, R.drawable.pet_cat_sleep_dark),
}

private val Ink = Color(0xFF111827)
private val Body = Color(0xFF6B7280)
private val Frame1 = Color(0xFFF3F4F6)
private val Frame2 = Color(0xFFE9EBEF)
private val Frame3 = Color(0xFFE2E5EA)
private val ButtonInk = Color(0xFF111827)

/**
 * DutyPe's empty state: clean and still. Either an icon in soft nested rounded squares or, with
 * [art], a small pet illustration; then a title, one or two lines of help, optional tips and up
 * to two actions.
 *
 * It sizes to its content (safe inside a LazyColumn item); [DutyPeEmptyScreen] centres it on a
 * whole screen. [compact] is for empty sections inside a card or list.
 */
@Composable
fun DutyPeEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tone: EmptyTone = EmptyTone.BLUE,
    badge: ImageVector? = null,
    tips: List<String> = emptyList(),
    primary: EmptyStateAction? = null,
    secondary: EmptyStateAction? = null,
    compact: Boolean = false,
    art: EmptyArt? = null
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (art != null && !compact) {
                Image(
                    // The app's own dark setting (not only the phone's) picks the dark backdrop.
                    painter = painterResource(if (com.example.dutype.ui.theme.LocalDarkMode.current) art.darkDrawable else art.drawable),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .widthIn(max = 280.dp)
                        .aspectRatio(240f / 180f)
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                IconFrame(icon = icon, badge = badge, tone = tone, size = if (compact) 96.dp else 132.dp)
                Spacer(modifier = Modifier.height(if (compact) 12.dp else 20.dp))
            }
            Text(
                text = title,
                style = AppTypography.emptyStateTitle.copy(
                    fontSize = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = if (compact) 21.sp else 24.sp,
                    color = Ink.fg()
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = AppTypography.emptyStateSubtitle.copy(
                    fontSize = if (compact) 13.sp else 14.sp,
                    lineHeight = if (compact) 19.sp else 21.sp,
                    color = Body.fg()
                ),
                textAlign = TextAlign.Center
            )
            if (tips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                EmptyTips(tips.take(3))
            }
            if (primary != null || secondary != null) {
                Spacer(modifier = Modifier.height(if (compact) 14.dp else 24.dp))
                EmptyActions(primary, secondary)
            }
        }
    }
}

/** [DutyPeEmptyState] centred on a whole screen. */
@Composable
fun DutyPeEmptyScreen(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tone: EmptyTone = EmptyTone.BLUE,
    badge: ImageVector? = null,
    tips: List<String> = emptyList(),
    primary: EmptyStateAction? = null,
    secondary: EmptyStateAction? = null,
    art: EmptyArt? = null
) {
    DutyPeEmptyState(
        icon = icon, title = title, message = message, tone = tone, badge = badge, tips = tips,
        primary = primary, secondary = secondary, art = art, modifier = modifier.fillMaxSize()
    )
}

/** Three soft nested rounded squares around a plain icon. */
@Composable
private fun IconFrame(icon: ImageVector, badge: ImageVector?, tone: EmptyTone, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .border(1.dp, Frame1.bd(), RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.76f)
                .border(1.dp, Frame2.bd(), RoundedCornerShape(size * 0.23f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.5f)
                    .background(Color.White.bg(), RoundedCornerShape(size * 0.15f))
                    .border(1.dp, Frame3.bd(), RoundedCornerShape(size * 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Ink.fg(), modifier = Modifier.size(size * 0.22f))
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 6.dp)
                            .size(size * 0.17f)
                            .background(tone.accent, CircleShape)
                            .border(2.dp, Color.White.bd(), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(badge, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTips(tips: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB).bg(), RoundedCornerShape(12.dp))
            .border(1.dp, Frame1.bd(), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tips.forEach { tip ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(5.dp)
                        .background(Body.bg(), CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(tip, fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFF374151).fg())
            }
        }
    }
}

@Composable
private fun EmptyActions(primary: EmptyStateAction?, secondary: EmptyStateAction?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        primary?.let { action ->
            Button(
                onClick = action.onClick,
                colors = ButtonDefaults.buttonColors(containerColor = ButtonInk.bg(), contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp),
                modifier = Modifier.height(42.dp)
            ) {
                action.icon?.let {
                    Icon(it, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(action.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        secondary?.let { action ->
            TextButton(onClick = action.onClick, modifier = Modifier.height(40.dp)) {
                Text(action.label, color = Ink.fg(), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
