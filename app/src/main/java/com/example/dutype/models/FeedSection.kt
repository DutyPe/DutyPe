package com.example.dutype.models

/**
 * Where a job sits in the worker feed: exact km bands around the worker first, then the rest of
 * their district, then the rest of their state. [ANYWHERE] is only used without a location.
 */
enum class FeedSection(val maxKm: Double?) {
    KM_5(5.0),
    KM_10(10.0),
    KM_15(15.0),
    KM_20(20.0),
    DISTRICT(null),
    STATE(null),
    ANYWHERE(null);

    val isNearby: Boolean get() = maxKm != null

    companion object {
        val BANDS: List<FeedSection> = listOf(KM_5, KM_10, KM_15, KM_20)
    }
}
