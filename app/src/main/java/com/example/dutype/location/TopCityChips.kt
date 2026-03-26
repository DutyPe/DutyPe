package com.example.dutype.location

import com.example.dutype.models.LocationData
import com.example.dutype.utils.GeoUtils

/**
 * Shared top-city chip suggestions for worker job discovery.
 */
object TopCityChips {

    data class CityLocationChip(
        val city: String,
        val state: String,
        val latitude: Double,
        val longitude: Double
    ) {
        fun label(): String = city
    }

    private val defaultCities = listOf(
        CityLocationChip("Hyderabad", "Telangana", 17.3850, 78.4867),
        CityLocationChip("Khammam", "Telangana", 17.2473, 80.1514),
        CityLocationChip("Warangal", "Telangana", 17.9689, 79.5941),
        CityLocationChip("Karimnagar", "Telangana", 18.4386, 79.1288),
        CityLocationChip("Nizamabad", "Telangana", 18.6725, 78.0941),
        CityLocationChip("Nalgonda", "Telangana", 17.0575, 79.2672),
        CityLocationChip("Vijayawada", "Andhra Pradesh", 16.5062, 80.6480),
        CityLocationChip("Visakhapatnam", "Andhra Pradesh", 17.6868, 83.2185),
        CityLocationChip("Bengaluru", "Karnataka", 12.9716, 77.5946),
        CityLocationChip("Chennai", "Tamil Nadu", 13.0827, 80.2707)
    )

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
