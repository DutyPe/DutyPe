package com.example.dutype.location

import com.example.dutype.models.LocationData
import com.example.dutype.models.JobListing
import com.example.dutype.utils.GeoUtils

/**
 * Shared top-city chip suggestions for worker job discovery.
 */
object TopCityChips {

    private data class CityAggregate(
        val city: String,
        val count: Int,
        val lat: Double,
        val lng: Double
    )

    data class CityLocationChip(
        val city: String,
        val state: String,
        val latitude: Double,
        val longitude: Double
    ) {
        fun label(): String = city
    }

    val defaultCities = listOf(
        CityLocationChip("Hyderabad", "Telangana", 17.3850, 78.4867),
        CityLocationChip("Nizamabad", "Telangana", 18.6725, 78.0941),
        CityLocationChip("Vijayawada", "Andhra Pradesh", 16.5062, 80.6480),
        CityLocationChip("Visakhapatnam", "Andhra Pradesh", 17.6868, 83.2185),
        CityLocationChip("Bengaluru", "Karnataka", 12.9716, 77.5946),
        CityLocationChip("Kolkata", "West Bengal", 22.5726, 88.3639),
        CityLocationChip("Mumbai", "Maharashtra", 19.0760, 72.8777),
        CityLocationChip("Pune", "Maharashtra", 18.5204, 73.8567)
    )

    private fun parseCityName(raw: String): String {
        val cleaned = raw.trim()
        if (cleaned.isBlank()) return ""

        val parts = cleaned.split(',').map { it.trim() }.filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> ""
            parts.size >= 3 -> parts[parts.size - 2]
            parts.size == 2 -> parts[1]
            else -> parts[0]
        }
    }

    private fun aggregateTopCitiesFromJobs(jobs: List<JobListing>): List<CityAggregate> {
        if (jobs.isEmpty()) return emptyList()

        data class RunningAgg(var count: Int = 0, var latSum: Double = 0.0, var lngSum: Double = 0.0, var coordCount: Int = 0)

        val map = mutableMapOf<String, RunningAgg>()
        jobs.forEach { job ->
            val city = parseCityName(job.addressText.ifBlank { job.location })
            if (city.isBlank()) return@forEach

            val key = city.lowercase()
            val agg = map.getOrPut(key) { RunningAgg() }
            agg.count += 1

            if (GeoUtils.hasValidCoordinates(job.lat, job.lng)) {
                agg.latSum += job.lat
                agg.lngSum += job.lng
                agg.coordCount += 1
            }
        }

        return map.mapNotNull { (key, agg) ->
            if (agg.count == 0) return@mapNotNull null

            val defaults = defaultCities.firstOrNull { it.city.equals(key, ignoreCase = true) }
            val lat = if (agg.coordCount > 0) agg.latSum / agg.coordCount else defaults?.latitude ?: 0.0
            val lng = if (agg.coordCount > 0) agg.lngSum / agg.coordCount else defaults?.longitude ?: 0.0
            val cityName = defaults?.city ?: key.replaceFirstChar { it.uppercase() }

            CityAggregate(city = cityName, count = agg.count, lat = lat, lng = lng)
        }.sortedByDescending { it.count }
    }

    fun buildTopLocationChips(currentLocation: LocationData?, jobs: List<JobListing>, maxCount: Int = 6): List<CityLocationChip> {
        val dynamicCities = aggregateTopCitiesFromJobs(jobs).map {
            val defaultState = defaultCities.firstOrNull { chip -> chip.city.equals(it.city, ignoreCase = true) }?.state.orEmpty()
            CityLocationChip(
                city = it.city,
                state = defaultState,
                latitude = it.lat,
                longitude = it.lng
            )
        }

        if (dynamicCities.isEmpty()) {
            return buildTopLocationChips(currentLocation, maxCount)
        }

        val hasUserCoords = currentLocation != null &&
            GeoUtils.hasValidCoordinates(currentLocation.latitude, currentLocation.longitude)

        val userCity = currentLocation?.city?.trim().orEmpty()
        val currentChip = if (hasUserCoords && userCity.isNotBlank()) {
            CityLocationChip(
                city = userCity,
                state = currentLocation?.state ?: "",
                latitude = currentLocation?.latitude ?: 0.0,
                longitude = currentLocation?.longitude ?: 0.0
            )
        } else {
            null
        }

        val seeded = if (currentChip != null) {
            dynamicCities.filterNot { it.city.equals(currentChip.city, ignoreCase = true) }
        } else {
            dynamicCities
        }

        val fallbackPool = defaultCities.filterNot { def ->
            seeded.any { it.city.equals(def.city, ignoreCase = true) } ||
                (currentChip != null && def.city.equals(currentChip.city, ignoreCase = true))
        }

        val tail = if (currentChip != null) {
            fallbackPool.sortedBy { city ->
                GeoUtils.calculateDistance(
                    currentChip.latitude,
                    currentChip.longitude,
                    city.latitude,
                    city.longitude
                )
            }
        } else {
            fallbackPool
        }

        val chips = mutableListOf<CityLocationChip>()
        if (currentChip != null) chips.add(currentChip)
        chips.addAll(seeded)
        chips.addAll(tail)

        return chips.distinctBy { it.city.lowercase() }.take(maxCount)
    }

    fun buildTopLocationChips(currentLocation: LocationData?, maxCount: Int = 6): List<CityLocationChip> {
        val hasUserCoords = currentLocation != null &&
            GeoUtils.hasValidCoordinates(currentLocation.latitude, currentLocation.longitude)

        val userCity = currentLocation?.city?.trim().orEmpty()
        val currentChip = if (hasUserCoords && userCity.isNotBlank()) {
            CityLocationChip(
                city = userCity,
                state = currentLocation?.state ?: "",
                latitude = currentLocation?.latitude ?: 0.0,
                longitude = currentLocation?.longitude ?: 0.0
            )
        } else {
            null
        }

        val pool = defaultCities.filterNot { default ->
            currentChip != null && default.city.equals(currentChip.city, ignoreCase = true)
        }

        val ordered = if (currentChip != null) {
            pool.sortedBy { city ->
                GeoUtils.calculateDistance(
                    currentChip.latitude,
                    currentChip.longitude,
                    city.latitude,
                    city.longitude
                )
            }
        } else {
            pool
        }

        val chips = mutableListOf<CityLocationChip>()
        if (currentChip != null) {
            chips.add(currentChip)
        }
        chips.addAll(ordered)

        return chips.distinctBy { it.city.lowercase() }.take(maxCount)
    }

    fun toLocationData(chip: CityLocationChip): LocationData {
        return LocationData(
            address = "${chip.city}, ${chip.state}",
            latitude = chip.latitude,
            longitude = chip.longitude,
            city = chip.city,
            state = chip.state,
            country = "India",
            area = chip.city
        )
    }
}
