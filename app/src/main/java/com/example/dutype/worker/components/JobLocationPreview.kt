package com.example.dutype.worker.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import kotlin.random.Random
import kotlinx.coroutines.tasks.await

/** Map colours for the light and the dark (night map) theme. */
private class MapColors(
    val land: Color, val block: Color, val park: Color, val water: Color,
    val road: Color, val roadEdge: Color, val mainRoad: Color, val mainRoadEdge: Color,
    val label: Color, val labelText: Color, val frame: Color
)

private val DayMap = MapColors(
    land = Color(0xFFF1F5EC), block = Color(0xFFE6ECDF), park = Color(0xFFCDE8C4), water = Color(0xFFBFDDF5),
    road = Color.White, roadEdge = Color(0xFFD9DEE5), mainRoad = Color(0xFFFFE8A3), mainRoadEdge = Color(0xFFF2C94C),
    label = Color.White, labelText = Color(0xFF0F172A), frame = Color(0xFFE2E8F0)
)

private val NightMap = MapColors(
    land = Color(0xFF111A2B), block = Color(0xFF172236), park = Color(0xFF14301F), water = Color(0xFF0F2A44),
    road = Color(0xFF253247), roadEdge = Color(0xFF1C273A), mainRoad = Color(0xFF6B5A2A), mainRoadEdge = Color(0xFF4A3F1E),
    label = Color(0xFF1A2233), labelText = Color(0xFFF1F5F9), frame = Color(0xFF2B3548)
)
private val PinRed = Color(0xFFE53935)
private val Ink = Color(0xFF0F172A)
private val Slate = Color(0xFF64748B)

/**
 * The job's place on a drawn map: streets, a park and water around a bouncing pin labelled with
 * the area, plus a "Get directions" button that opens Google Maps in two-wheeler mode. No Maps
 * SDK, no map tiles, no cost; the street layout is fixed per job so it looks the same every time.
 */
@Composable
fun JobLocationPreview(
    jobId: String,
    lat: Double,
    lng: Double,
    placeLabel: String,
    distanceKm: Double?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val openDirections = { openBikeDirections(context, lat, lng, placeLabel) }
    val colors = if (com.example.dutype.ui.theme.LocalDarkMode.current) NightMap else DayMap

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, colors.frame, RoundedCornerShape(14.dp))
                .clickable { openDirections() }
        ) {
            val seed = remember(jobId) { jobId.hashCode() }
            Canvas(modifier = Modifier.fillMaxSize()) { drawStreetMap(seed, colors) }
            MapPin(
                label = placeLabel,
                colors = colors,
                modifier = Modifier.align(Alignment.Center).offset(y = (-18).dp)
            )
            if (distanceKm != null && distanceKm > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .shadow(2.dp, RoundedCornerShape(999.dp))
                        .background(colors.label, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.NearMe, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.job_location_km_away, formatKm(distanceKm)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.labelText
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = openDirections,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A).bg(), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Filled.TwoWheeler, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.job_location_directions), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.job_location_bike_hint),
            fontSize = 11.sp,
            color = Slate.fg(),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/** A red pin that bobs gently over a pulsing ground ring, with the place name above it. */
@Composable
private fun MapPin(label: String, colors: MapColors, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pin")
    val bob by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob"
    )
    val ring by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "ring"
    )
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.labelText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 220.dp)
                    .shadow(3.dp, RoundedCornerShape(8.dp))
                    .background(colors.label, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.size(width = 56.dp, height = 52.dp)) {
            // Ground ring.
            Canvas(modifier = Modifier.size(56.dp, 16.dp).align(Alignment.BottomCenter)) {
                val r = size.width / 2f * (0.3f + 0.7f * ring)
                drawOval(
                    color = PinRed.copy(alpha = 0.35f * (1f - ring)),
                    topLeft = Offset(center.x - r, center.y - r * 0.3f),
                    size = Size(r * 2, r * 0.6f)
                )
                drawOval(
                    color = Color.Black.copy(alpha = 0.18f),
                    topLeft = Offset(center.x - 7.dp.toPx(), center.y - 2.dp.toPx()),
                    size = Size(14.dp.toPx(), 4.dp.toPx())
                )
            }
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = PinRed,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .size(40.dp)
                    .graphicsLayer { translationY = -bob * 6.dp.toPx() }
            )
        }
    }
}

/** Streets, blocks, a park and a river, laid out from [seed]. */
private fun DrawScope.drawStreetMap(seed: Int, c: MapColors) {
    val rnd = Random(seed)
    val w = size.width
    val h = size.height
    drawRect(c.land)

    // River across one corner.
    val river = Path().apply {
        val y0 = h * (0.65f + rnd.nextFloat() * 0.2f)
        moveTo(-20f, y0)
        cubicTo(w * 0.3f, y0 - h * 0.25f, w * 0.6f, h * 1.05f, w + 20f, y0 + h * 0.1f)
    }
    drawPath(river, c.water, style = Stroke(width = h * 0.13f, cap = StrokeCap.Round))

    // City blocks and a park.
    val cols = 5
    val rows = 4
    val cw = w / cols
    val rh = h / rows
    val parkCol = rnd.nextInt(cols)
    val parkRow = rnd.nextInt(rows)
    for (col in 0 until cols) for (r in 0 until rows) {
        val color = if (col == parkCol && r == parkRow) c.park else c.block
        drawRoundRect(
            color = color,
            topLeft = Offset(col * cw + cw * 0.12f, r * rh + rh * 0.14f),
            size = Size(cw * 0.76f, rh * 0.72f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
        )
    }

    // Side streets on the grid lines.
    val street = 6.dp.toPx()
    for (col in 1 until cols) {
        val x = col * cw + (rnd.nextFloat() - 0.5f) * cw * 0.1f
        drawLine(c.roadEdge, Offset(x, 0f), Offset(x, h), street + 2f)
        drawLine(c.road, Offset(x, 0f), Offset(x, h), street)
    }
    for (r in 1 until rows) {
        val y = r * rh + (rnd.nextFloat() - 0.5f) * rh * 0.1f
        drawLine(c.roadEdge, Offset(0f, y), Offset(w, y), street + 2f)
        drawLine(c.road, Offset(0f, y), Offset(w, y), street)
    }

    // One main road running diagonally through the middle.
    val main = Path().apply {
        moveTo(-10f, h * (0.2f + rnd.nextFloat() * 0.3f))
        quadraticBezierTo(w * 0.5f, h * 0.5f, w + 10f, h * (0.35f + rnd.nextFloat() * 0.4f))
    }
    drawPath(main, c.mainRoadEdge, style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round))
    drawPath(main, c.mainRoad, style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round))
}

private fun formatKm(km: Double): String =
    if (km < 10) String.format(java.util.Locale.US, "%.1f", km) else km.toInt().toString()

/**
 * Google Maps turn-by-turn in two-wheeler mode (`mode=l`); without the Maps app, the web
 * directions page with travelmode=two-wheeler; as a last resort any map app via geo:.
 */
fun openBikeDirections(context: Context, lat: Double, lng: Double, label: String = "") {
    val attempts = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng&mode=l"))
            .setPackage("com.google.android.apps.maps"),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=two-wheeler")),
        Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label.ifBlank { "Job" })})"))
    )
    for (intent in attempts) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
    }
    Toast.makeText(context, context.getString(R.string.job_location_no_maps), Toast.LENGTH_SHORT).show()
}

/**
 * The phone's last known position (no new GPS fix, so instant and free), or null without location
 * permission or when the phone has none yet.
 */
suspend fun lastDeviceLatLng(context: Context): Pair<Double, Double>? {
    val fine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine != android.content.pm.PackageManager.PERMISSION_GRANTED && coarse != android.content.pm.PackageManager.PERMISSION_GRANTED) return null
    return try {
        val location = kotlinx.coroutines.withTimeoutOrNull(1500L) {
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context).lastLocation.await()
        }
        location?.takeIf { it.latitude != 0.0 || it.longitude != 0.0 }?.let { it.latitude to it.longitude }
    } catch (e: SecurityException) {
        null
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
