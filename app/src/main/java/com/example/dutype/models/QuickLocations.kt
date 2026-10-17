package com.example.dutype.models

/**
 * Pre-verified coordinates and landmark addresses for Telangana & Andhra hubs.
 * Guarantees that every user in Khammam & surrounding regions always gets valid
 * coordinates for hyperlocal matching, even if GPS permission is temporarily denied
 * or indoor GPS signal is weak.
 */
data class QuickLocationItem(
    val label: String,
    val shortName: String,
    val address: String,
    val lat: Double,
    val lng: Double
)

object QuickLocations {
    val PRESETS = listOf(
        QuickLocationItem(
            label = "📍 Khammam (Wyra Rd)",
            shortName = "Wyra Road",
            address = "Wyra Road, Khammam, Telangana 507001",
            lat = 17.2473,
            lng = 80.1514
        ),
        QuickLocationItem(
            label = "📍 Khammam (ZP Centre)",
            shortName = "ZP Centre",
            address = "ZP Centre, Khammam, Telangana 507001",
            lat = 17.2515,
            lng = 80.1599
        ),
        QuickLocationItem(
            label = "📍 Khammam (Mamata / Bypass)",
            shortName = "Mamata / Bypass",
            address = "Rotary Nagar, Mamata Road, Khammam, Telangana 507002",
            lat = 17.2650,
            lng = 80.1450
        ),
        QuickLocationItem(
            label = "📍 Kothagudem",
            shortName = "Kothagudem",
            address = "MG Road, Kothagudem, Bhadradri Kothagudem, Telangana 507101",
            lat = 17.5511,
            lng = 80.6190
        ),
        QuickLocationItem(
            label = "📍 Warangal",
            shortName = "Warangal",
            address = "Hanamkonda, Warangal, Telangana 506001",
            lat = 17.9689,
            lng = 79.5941
        ),
        QuickLocationItem(
            label = "📍 Hyderabad",
            shortName = "Hyderabad",
            address = "Hyderabad, Telangana 500081",
            lat = 17.3850,
            lng = 78.4867
        ),
        QuickLocationItem(
            label = "📍 Suryapet",
            shortName = "Suryapet",
            address = "Suryapet, Telangana 508213",
            lat = 17.1400,
            lng = 79.6200
        )
    )

    fun matchCoordinates(address: String): Pair<Double, Double>? {
        val lower = address.trim().lowercase()
        return when {
            "zp centre" in lower || "zp center" in lower -> 17.2515 to 80.1599
            "mamata" in lower || "rotary" in lower -> 17.2650 to 80.1450
            "khammam" in lower || "wyra" in lower || "gandhi chowk" in lower -> 17.2473 to 80.1514
            "kothagudem" in lower || "bhadradri" in lower || "palvancha" in lower -> 17.5511 to 80.6190
            "warangal" in lower || "hanamkonda" in lower || "kazipet" in lower -> 17.9689 to 79.5941
            "hyderabad" in lower || "secunderabad" in lower || "madhapur" in lower || "gachibowli" in lower -> 17.3850 to 78.4867
            "suryapet" in lower -> 17.1400 to 79.6200
            "nalgonda" in lower -> 17.0500 to 79.2700
            else -> null
        }
    }
}
