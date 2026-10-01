package com.example.dutype.jobs

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Minimal geohash maths for cell-based job search. */
object Geohash {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    data class Box(val minLat: Double, val maxLat: Double, val minLng: Double, val maxLng: Double) {
        val centerLat: Double get() = (minLat + maxLat) / 2
        val centerLng: Double get() = (minLng + maxLng) / 2

        /** Shortest distance (km) from a point to this box; 0 when inside. */
        fun distanceKmFrom(lat: Double, lng: Double): Double =
            distanceKm(lat, lng, lat.coerceIn(minLat, maxLat), lng.coerceIn(minLng, maxLng))
    }

    fun encode(lat: Double, lng: Double, precision: Int): String {
        var latMin = -90.0; var latMax = 90.0
        var lngMin = -180.0; var lngMax = 180.0
        val out = StringBuilder(precision)
        var bit = 0; var ch = 0; var even = true
        while (out.length < precision) {
            if (even) {
                val mid = (lngMin + lngMax) / 2
                if (lng >= mid) { ch = (ch shl 1) or 1; lngMin = mid } else { ch = ch shl 1; lngMax = mid }
            } else {
                val mid = (latMin + latMax) / 2
                if (lat >= mid) { ch = (ch shl 1) or 1; latMin = mid } else { ch = ch shl 1; latMax = mid }
            }
            even = !even
            if (++bit == 5) { out.append(BASE32[ch]); bit = 0; ch = 0 }
        }
        return out.toString()
    }

    fun decode(hash: String): Box {
        var latMin = -90.0; var latMax = 90.0
        var lngMin = -180.0; var lngMax = 180.0
        var even = true
        for (c in hash) {
            val value = BASE32.indexOf(c)
            if (value < 0) break
            for (shift in 4 downTo 0) {
                val bitSet = (value shr shift) and 1 == 1
                if (even) {
                    val mid = (lngMin + lngMax) / 2
                    if (bitSet) lngMin = mid else lngMax = mid
                } else {
                    val mid = (latMin + latMax) / 2
                    if (bitSet) latMin = mid else latMax = mid
                }
                even = !even
            }
        }
        return Box(latMin, latMax, lngMin, lngMax)
    }

    /** Geohash cells at [precision] that cover the circle of [radiusKm] around a point. */
    fun covering(lat: Double, lng: Double, radiusKm: Double, precision: Int): Set<String> {
        val cell = decode(encode(lat, lng, precision))
        val cellLatKm = (cell.maxLat - cell.minLat) * 111.0
        val cellLngKm = (cell.maxLng - cell.minLng) * 111.0 * max(0.2, cos(lat * PI / 180))
        val latSteps = (radiusKm / cellLatKm).toInt() + 1
        val lngSteps = (radiusKm / cellLngKm).toInt() + 1
        val latStep = (cell.maxLat - cell.minLat)
        val lngStep = (cell.maxLng - cell.minLng)
        val out = LinkedHashSet<String>()
        for (i in -latSteps..latSteps) {
            for (j in -lngSteps..lngSteps) {
                val pLat = (lat + i * latStep).coerceIn(-89.999, 89.999)
                val pLng = lng + j * lngStep
                val hash = encode(pLat, pLng, precision)
                if (decode(hash).distanceKmFrom(lat, lng) <= radiusKm) out.add(hash)
            }
        }
        return out
    }

    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = (lat2 - lat1) * PI / 180
        val dLng = (lng2 - lng1) * PI / 180
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * 6371.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
}
