package com.example.dutype.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Moped
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * JavaScript interface bridge to communicate live road calculations (distance & duration)
 * from OSRM router inside WebView back to Jetpack Compose.
 */
class MapJsBridge(
    private val onRouteCalculated: (Double, Double) -> Unit
) {
    @JavascriptInterface
    fun onRouteCalculated(distanceMeters: Double, durationSeconds: Double) {
        onRouteCalculated(distanceMeters, durationSeconds)
    }
}

/**
 * High-performance, 100% Free Live Map Tracker with Real Street Road Routing.
 * Powered by OpenFreeMap (openfreemap.org), MapLibre GL, and OSRM Road Routing Machine.
 *
 * Visual Parity with Uber, Swiggy/Zomato, and Rapido:
 * 1. Street-Snapped Polyline: Dual-layer styling (high-contrast casing outline + vibrant electric blue road line).
 * 2. Doorstep Pin & Green Geofence: Black circular pin with white home icon + emerald green arrival radius ring.
 * 3. Technician Vehicle Marker: Scooter/bike marker with smooth bearing rotation and pulsing radar wave.
 * 4. Expand / Fullscreen Toggle: Top-right ⤢ button to toggle compact (240dp) and expansive (440dp) map views.
 * 5. Floating ETA Pill & Direct Actions: Live road distance, ETA, Google Maps directions, and 1-tap dialer.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OpenFreeMapTrackerView(
    customerLat: Double,
    customerLng: Double,
    partnerLat: Double,
    partnerLng: Double,
    modifier: Modifier = Modifier,
    customerLabel: String = "Customer Address",
    partnerName: String = "Technician",
    partnerPhone: String = "",
    partnerBearing: Float = 0f,
    isPartnerMode: Boolean = false, // True if worker is viewing destination
    onNavigateClick: (() -> Unit)? = null,
    onCallClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    // Live calculated road distance & duration reported by OSRM via MapJsBridge
    var roadDistanceMeters by remember { mutableStateOf<Double?>(null) }
    var roadDurationSeconds by remember { mutableStateOf<Double?>(null) }

    // Smooth animated height transition when expanding/collapsing map
    val animatedHeight by animateDpAsState(
        targetValue = if (isExpanded) 440.dp else 240.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mapHeightAnim"
    )

    // Trigger map.resize() whenever expand state toggles
    LaunchedEffect(isExpanded, isMapLoaded) {
        if (isMapLoaded) {
            delay(280) // Wait for Compose height animation to settle
            webViewRef?.evaluateJavascript("if (window.resizeMap) { window.resizeMap(); }", null)
        }
    }

    // Effective Coordinates with realistic fallback when GPS is initiating
    val effectivePartnerLat = remember(partnerLat, customerLat) {
        if (partnerLat != 0.0) partnerLat
        else if (customerLat != 0.0) customerLat + 0.0125
        else 17.2473
    }
    val effectivePartnerLng = remember(partnerLng, customerLng) {
        if (partnerLng != 0.0) partnerLng
        else if (customerLng != 0.0) customerLng + 0.0095
        else 80.1514
    }

    // Fallback baseline straight-line distance if OSRM is loading or offline
    val baselineDistanceMeters = remember(customerLat, customerLng, effectivePartnerLat, effectivePartnerLng) {
        if (customerLat != 0.0 && customerLng != 0.0) {
            val res = FloatArray(1)
            Location.distanceBetween(effectivePartnerLat, effectivePartnerLng, customerLat, customerLng, res)
            res[0]
        } else 1800f
    }

    // Prefer accurate road distance from OSRM, fallback to straight-line distance
    val effectiveDistanceMeters = (roadDistanceMeters ?: baselineDistanceMeters.toDouble()).toFloat()
    val distanceKm = effectiveDistanceMeters / 1000f

    // Prefer accurate driving duration from OSRM, fallback to city average speed (24 km/h)
    val etaMinutes = remember(roadDurationSeconds, distanceKm) {
        roadDurationSeconds?.let { sec ->
            (sec / 60.0).roundToInt().coerceAtLeast(1)
        } ?: run {
            if (distanceKm <= 0.05f) 1
            else (distanceKm / 24f * 60f).toInt().coerceAtLeast(2)
        }
    }

    val statusHeadline = when {
        effectiveDistanceMeters <= 80f -> "Technician has arrived at doorstep!"
        effectiveDistanceMeters <= 300f -> "Technician arriving in ~1-2 mins (${effectiveDistanceMeters.toInt()} m)"
        else -> if (isPartnerMode) "Customer is %.1f km away • ~%d mins drive".format(distanceKm, etaMinutes)
                else "Technician is %.1f km away • Arriving in ~%d mins".format(distanceKm, etaMinutes)
    }

    // Push coordinates to WebView dynamically whenever partner location or bearing updates
    LaunchedEffect(effectivePartnerLat, effectivePartnerLng, partnerBearing, isMapLoaded) {
        if (isMapLoaded) {
            val js = "if (window.updatePartnerLocation) { window.updatePartnerLocation($effectivePartnerLat, $effectivePartnerLng, $partnerBearing); }"
            webViewRef?.evaluateJavascript(js, null)
        }
    }

    val dialPhone = {
        val targetPhone = partnerPhone.trim()
        if (targetPhone.isNotBlank()) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$targetPhone"))
            runCatching { context.startActivity(dialIntent) }
        }
    }

    val openGoogleMapsNav = {
        val destLat = if (isPartnerMode) customerLat else effectivePartnerLat
        val destLng = if (isPartnerMode) customerLng else effectivePartnerLng
        val gmmIntentUri = Uri.parse("google.navigation:q=$destLat,$destLng")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng")
                context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
            }
        } catch (_: Exception) {
            val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng")
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, browserUri)) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
    ) {
        // ── Map Header Banner ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF22C55E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Moped,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isPartnerMode) "Navigation to Customer" else "Live Partner Tracking",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Street-Snapped Route • OpenFreeMap",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.5.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Expand / Collapse Fullscreen Map Button
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = if (isExpanded) "Collapse Map" else "Expand Map",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // External Google Maps Navigation Button
                IconButton(
                    onClick = { onNavigateClick?.invoke() ?: openGoogleMapsNav() },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF334155), CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Navigation,
                        contentDescription = "Open in Google Maps",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ── Map Container ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .background(Color(0xFFF1F5F9))
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }

                        // Attach JavaScript bridge for real-time distance and ETA sync
                        addJavascriptInterface(
                            MapJsBridge { distMeters, durSec ->
                                roadDistanceMeters = distMeters
                                roadDurationSeconds = durSec
                            },
                            "AndroidBridge"
                        )

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isMapLoaded = true
                            }
                        }
                        webChromeClient = WebChromeClient()

                        val htmlContent = buildOpenFreeMapHtml(
                            custLat = customerLat,
                            custLng = customerLng,
                            partLat = effectivePartnerLat,
                            partLng = effectivePartnerLng,
                            partName = partnerName,
                            custLabel = customerLabel,
                            partnerBearing = partnerBearing
                        )

                        loadDataWithBaseURL("https://openfreemap.org", htmlContent, "text/html", "UTF-8", null)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // ── Floating Uber/Rapido-Style Live Distance & ETA Pill (Top-Left) ──
            Surface(
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.TopStart)
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.94f),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF22C55E), CircleShape)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (isPartnerMode) "%.1f km to Customer • ~%d mins".format(distanceKm, etaMinutes)
                               else "%.1f km away • ~%d mins".format(distanceKm, etaMinutes),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // ── Top-Right Expand/Collapse Button Overlay (matching Screenshot 2 ⤢) ──
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier
                    .padding(10.dp)
                    .size(34.dp)
                    .align(Alignment.TopEnd)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(18.dp)
                )
            }

            // ── Bottom-Right Re-center Floating Action Button ──
            IconButton(
                onClick = {
                    val js = "if (window.fitBoundsToMarkers) { window.fitBoundsToMarkers(); }"
                    webViewRef?.evaluateJavascript(js, null)
                },
                modifier = Modifier
                    .padding(10.dp)
                    .size(34.dp)
                    .align(Alignment.BottomEnd)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    Icons.Filled.MyLocation,
                    contentDescription = "Re-center",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Bottom Live ETA, Address & Action Controls (Uber & Rapido Cards) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (effectiveDistanceMeters <= 80f) Color(0xFF22C55E) else Color(0xFF3B82F6))
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusHeadline,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (customerLabel.isNotBlank()) {
                    Text(
                        text = customerLabel,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))

            // Quick Call Button (if phone number is available)
            if (partnerPhone.isNotBlank() || onCallClick != null) {
                IconButton(
                    onClick = { onCallClick?.invoke() ?: dialPhone() },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFECFDF5), CircleShape)
                        .border(1.dp, Color(0xFFA7F3D0), CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Phone,
                        contentDescription = "Call",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
            }

            // Google Maps Directions Button
            OutlinedButton(
                onClick = { onNavigateClick?.invoke() ?: openGoogleMapsNav() },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Map", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Generates standalone lightweight HTML with MapLibre GL JS, OpenFreeMap Liberty style,
 * and real-time OSRM street-snapped road geometry.
 */
private fun buildOpenFreeMapHtml(
    custLat: Double,
    custLng: Double,
    partLat: Double,
    partLng: Double,
    partName: String,
    custLabel: String,
    partnerBearing: Float
): String {
    val initialLat = if (partLat != 0.0) partLat else if (custLat != 0.0) custLat else 17.2473
    val initialLng = if (partLng != 0.0) partLng else if (custLng != 0.0) custLng else 80.1514

    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <link href="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.css" rel="stylesheet" />
        <script src="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.js"></script>
        <style>
            body, html, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, sans-serif; overflow: hidden; }
            
            /* Customer Doorstep Pin (Matching Screenshot 2 / Zomato style) */
            .cust-pin-container {
                display: flex;
                flex-direction: column;
                align-items: center;
                cursor: pointer;
            }
            .cust-pin-bubble {
                width: 32px;
                height: 32px;
                background: #0F172A;
                border: 2.5px solid #FFFFFF;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                box-shadow: 0 4px 10px rgba(15, 23, 42, 0.4);
                color: #FFFFFF;
                font-size: 15px;
            }
            .cust-pin-stem {
                width: 4px;
                height: 5px;
                background: #0F172A;
                border-radius: 2px;
                margin-top: 1px;
            }

            /* Technician Vehicle Marker (Matching Screenshots 2 & 3 / Swiggy & Rapido style) */
            .partner-marker-container {
                position: relative;
                width: 44px;
                height: 44px;
                display: flex;
                align-items: center;
                justify-content: center;
                cursor: pointer;
            }
            .partner-pulse-radar {
                position: absolute;
                width: 44px;
                height: 44px;
                border-radius: 50%;
                background: rgba(34, 197, 94, 0.35);
                animation: pulse-ring 1.8s cubic-bezier(0.215, 0.61, 0.355, 1) infinite;
            }
            @keyframes pulse-ring {
                0% { transform: scale(0.9); opacity: 0.85; }
                100% { transform: scale(2.4); opacity: 0; }
            }
            .partner-vehicle-bubble {
                position: relative;
                width: 38px;
                height: 38px;
                background: #FFFFFF;
                border: 2.5px solid #16A34A;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                box-shadow: 0 4px 12px rgba(22, 163, 74, 0.45);
                z-index: 2;
            }
            .partner-rotator {
                display: flex;
                align-items: center;
                justify-content: center;
                transition: transform 0.4s ease-out;
            }
            .partner-glyph {
                font-size: 20px;
                line-height: 1;
            }
            .partner-name-badge {
                position: absolute;
                top: -18px;
                background: #0F172A;
                color: #FFFFFF;
                font-size: 10px;
                font-weight: 700;
                padding: 1px 6px;
                border-radius: 8px;
                white-space: nowrap;
                box-shadow: 0 2px 6px rgba(0,0,0,0.3);
                z-index: 3;
            }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <script>
            let map;
            let partnerMarker = null;
            let custMarker = null;

            const custLat = $custLat;
            const custLng = $custLng;
            let partLat = $partLat;
            let partLng = $partLng;
            let currentBearing = $partnerBearing;

            map = new maplibregl.Map({
                container: 'map',
                style: 'https://tiles.openfreemap.org/styles/liberty',
                center: [$initialLng, $initialLat],
                zoom: 14,
                attributionControl: false
            });

            // Helper to compute a circular polygon in GeoJSON (matching Screenshot 2 green arrival geofence)
            function createGeofencePolygon(centerLng, centerLat, radiusMeters, points = 64) {
                const coords = { latitude: centerLat, longitude: centerLng };
                const km = radiusMeters / 1000;
                const ret = [];
                const distanceX = km / (111.320 * Math.cos(coords.latitude * Math.PI / 180));
                const distanceY = km / 110.574;
                for (let i = 0; i < points; i++) {
                    const theta = (i / points) * (2 * Math.PI);
                    const x = distanceX * Math.cos(theta);
                    const y = distanceY * Math.sin(theta);
                    ret.push([coords.longitude + x, coords.latitude + y]);
                }
                ret.push(ret[0]);
                return {
                    type: 'Feature',
                    geometry: { type: 'Polygon', coordinates: [ret] }
                };
            }

            map.on('load', function() {
                // 1. Destination Arrival Zone / Geofence (Green circle matching Screenshot 2)
                if (custLat !== 0 && custLng !== 0) {
                    const geofenceGeo = createGeofencePolygon(custLng, custLat, 120);
                    map.addSource('geofence', {
                        type: 'geojson',
                        data: geofenceGeo
                    });
                    map.addLayer({
                        id: 'geofence-fill',
                        type: 'fill',
                        source: 'geofence',
                        paint: {
                            'fill-color': '#10B981',
                            'fill-opacity': 0.16
                        }
                    });
                    map.addLayer({
                        id: 'geofence-border',
                        type: 'line',
                        source: 'geofence',
                        paint: {
                            'line-color': '#10B981',
                            'line-width': 1.8,
                            'line-opacity': 0.85
                        }
                    });

                    // 2. Customer Doorstep Marker (Black circle with white house matching Screenshot 2)
                    const cWrap = document.createElement('div');
                    cWrap.className = 'cust-pin-container';
                    cWrap.innerHTML = '<div class="cust-pin-bubble">🏠</div><div class="cust-pin-stem"></div>';
                    custMarker = new maplibregl.Marker({ element: cWrap, anchor: 'bottom' })
                        .setLngLat([custLng, custLat])
                        .setPopup(new maplibregl.Popup({ offset: 25 }).setText('${custLabel.replace("'", "\\'")}'))
                        .addTo(map);
                }

                // 3. Technician Vehicle Marker (Scooter with pulse radar and bearing rotation)
                if (partLat !== 0 && partLng !== 0) {
                    const pWrap = document.createElement('div');
                    pWrap.className = 'partner-marker-container';
                    pWrap.innerHTML = '<div class="partner-pulse-radar"></div>' +
                        '<div class="partner-vehicle-bubble">' +
                            '<div class="partner-rotator" id="partner-rotator" style="transform: rotate(' + currentBearing + 'deg);">' +
                                '<span class="partner-glyph">🛵</span>' +
                            '</div>' +
                        '</div>' +
                        '<div class="partner-name-badge">${partName.replace("'", "\\'")}</div>';

                    partnerMarker = new maplibregl.Marker({ element: pWrap, anchor: 'center' })
                        .setLngLat([partLng, partLat])
                        .addTo(map);
                }

                // 4. Fetch and render real road routing
                fetchAndDrawRoadRoute(partLng, partLat, custLng, custLat);
                window.fitBoundsToMarkers();
            });

            // Fetches real street turn geometry from OSRM road machine with fallback to straight line
            async function fetchAndDrawRoadRoute(pLng, pLat, cLng, cLat) {
                if (!pLng || !pLat || !cLng || !cLat) return;
                try {
                    const url = 'https://router.project-osrm.org/route/v1/driving/' + pLng + ',' + pLat + ';' + cLng + ',' + cLat + '?overview=full&geometries=geojson';
                    const controller = new AbortController();
                    const timeoutId = setTimeout(() => controller.abort(), 4000);
                    const res = await fetch(url, { signal: controller.signal });
                    clearTimeout(timeoutId);
                    if (res.ok) {
                        const data = await res.json();
                        if (data && data.routes && data.routes.length > 0 && data.routes[0].geometry) {
                            const r = data.routes[0];
                            setRouteLayer(r.geometry);
                            if (window.AndroidBridge && window.AndroidBridge.onRouteCalculated) {
                                window.AndroidBridge.onRouteCalculated(r.distance, r.duration);
                            }
                            return;
                        }
                    }
                } catch (e) {
                    console.warn('Road routing fallback:', e);
                }

                // Fallback straight line
                setRouteLayer({
                    type: 'LineString',
                    coordinates: [[pLng, pLat], [cLng, cLat]]
                });
            }

            // Sets dual-layer high-contrast road polyline (like Uber, Swiggy, Rapido)
            function setRouteLayer(geometry) {
                const geoData = {
                    type: 'Feature',
                    geometry: geometry
                };

                if (!map.getSource('route')) {
                    map.addSource('route', {
                        type: 'geojson',
                        data: geoData
                    });

                    // Outer high-contrast casing line (dark navy border)
                    map.addLayer({
                        id: 'route-casing',
                        type: 'line',
                        source: 'route',
                        layout: { 'line-join': 'round', 'line-cap': 'round' },
                        paint: {
                            'line-color': '#1E3A8A',
                            'line-width': [
                                'interpolate', ['linear'], ['zoom'],
                                10, 5,
                                14, 8,
                                18, 12
                            ],
                            'line-opacity': 0.95
                        }
                    });

                    // Inner vibrant road line (electric blue core)
                    map.addLayer({
                        id: 'route-line',
                        type: 'line',
                        source: 'route',
                        layout: { 'line-join': 'round', 'line-cap': 'round' },
                        paint: {
                            'line-color': '#2563EB',
                            'line-width': [
                                'interpolate', ['linear'], ['zoom'],
                                10, 3,
                                14, 5,
                                18, 8.5
                            ],
                            'line-opacity': 1.0
                        }
                    });
                } else {
                    map.getSource('route').setData(geoData);
                }
            }

            window.updatePartnerLocation = function(newLat, newLng, newBearing) {
                partLat = newLat;
                partLng = newLng;
                currentBearing = newBearing;

                if (partnerMarker) {
                    partnerMarker.setLngLat([newLng, newLat]);
                    const rotEl = document.getElementById('partner-rotator');
                    if (rotEl && typeof newBearing === 'number') {
                        rotEl.style.transform = 'rotate(' + newBearing + 'deg)';
                    }
                }
                fetchAndDrawRoadRoute(partLng, partLat, custLng, custLat);
            };

            window.fitBoundsToMarkers = function() {
                if (!map) return;
                const bounds = new maplibregl.LngLatBounds();
                let hasPoints = false;

                if (custLat !== 0 && custLng !== 0) {
                    bounds.extend([custLng, custLat]);
                    hasPoints = true;
                }
                if (partLat !== 0 && partLng !== 0) {
                    bounds.extend([partLng, partLat]);
                    hasPoints = true;
                }

                if (hasPoints) {
                    map.fitBounds(bounds, {
                        padding: { top: 50, bottom: 50, left: 50, right: 50 },
                        maxZoom: 16.5,
                        duration: 800
                    });
                }
            };

            window.resizeMap = function() {
                if (!map) return;
                map.resize();
                window.fitBoundsToMarkers();
            };
        </script>
    </body>
    </html>
    """.trimIndent()
}
