package com.example.dutype.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Azure Maps Service for location search and geocoding
 * Replaces Google Places API with Azure Maps Search API
 * 
 * Azure Maps API Documentation:
 * https://docs.microsoft.com/en-us/rest/api/maps/search
 */
class AzureMapsService(private val subscriptionKey: String) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val BASE_URL = "https://atlas.microsoft.com"
        private const val API_VERSION = "1.0"
        
        // Search endpoints
        private const val SEARCH_ADDRESS = "/search/address/json"
        private const val SEARCH_FUZZY = "/search/fuzzy/json"
        private const val SEARCH_POI = "/search/poi/json"
        private const val REVERSE_GEOCODE = "/search/address/reverse/json"
        
        // Default to India for better local results
        private const val DEFAULT_COUNTRY = "IN"
        private const val DEFAULT_LANGUAGE = "en-US"
    }
    
    /**
     * Search for addresses/locations using Azure Maps Fuzzy Search
     * This is similar to Google Places Autocomplete
     * 
     * @param query Search query (e.g., "Koramangala Bangalore")
     * @param limit Maximum number of results (default 5)
     * @return List of AzureLocationResult
     */
    suspend fun searchLocations(
        query: String,
        limit: Int = 5,
        countrySet: String = DEFAULT_COUNTRY
    ): Result<List<AzureLocationResult>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$BASE_URL$SEARCH_FUZZY?" +
                    "api-version=$API_VERSION" +
                    "&subscription-key=$subscriptionKey" +
                    "&query=$encodedQuery" +
                    "&countrySet=$countrySet" +
                    "&language=$DEFAULT_LANGUAGE" +
                    "&limit=$limit" +
                    "&typeahead=true"
            
            Timber.d("🗺️ Azure Maps: Searching for '$query'")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Timber.e("🗺️ Azure Maps: Search failed with code ${response.code}")
                return@withContext Result.failure(Exception("Search failed: ${response.code}"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val results = parseSearchResults(responseBody)
            
            Timber.d("🗺️ Azure Maps: Found ${results.size} results")
            Result.success(results)
            
        } catch (e: Exception) {
            Timber.e(e, "🗺️ Azure Maps: Search error")
            Result.failure(e)
        }
    }
    
    /**
     * Search specifically for addresses (more precise than fuzzy search)
     */
    suspend fun searchAddress(
        query: String,
        limit: Int = 5
    ): Result<List<AzureLocationResult>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$BASE_URL$SEARCH_ADDRESS?" +
                    "api-version=$API_VERSION" +
                    "&subscription-key=$subscriptionKey" +
                    "&query=$encodedQuery" +
                    "&countrySet=$DEFAULT_COUNTRY" +
                    "&language=$DEFAULT_LANGUAGE" +
                    "&limit=$limit"
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Address search failed: ${response.code}"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val results = parseSearchResults(responseBody)
            
            Result.success(results)
            
        } catch (e: Exception) {
            Timber.e(e, "🗺️ Azure Maps: Address search error")
            Result.failure(e)
        }
    }
    
    /**
     * Reverse geocode - get address from coordinates
     */
    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Result<AzureLocationResult?> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL$REVERSE_GEOCODE?" +
                    "api-version=$API_VERSION" +
                    "&subscription-key=$subscriptionKey" +
                    "&query=$latitude,$longitude" +
                    "&language=$DEFAULT_LANGUAGE"
            
            Timber.d("🗺️ Azure Maps: Reverse geocoding $latitude, $longitude")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Reverse geocode failed: ${response.code}"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val result = parseReverseGeocodeResult(responseBody)
            
            Timber.d("🗺️ Azure Maps: Reverse geocode result: ${result?.formattedAddress}")
            Result.success(result)
            
        } catch (e: Exception) {
            Timber.e(e, "🗺️ Azure Maps: Reverse geocode error")
            Result.failure(e)
        }
    }
    
    /**
     * Parse search results from Azure Maps API response
     */
    private fun parseSearchResults(jsonString: String): List<AzureLocationResult> {
        val results = mutableListOf<AzureLocationResult>()
        
        try {
            val json = JSONObject(jsonString)
            val resultsArray = json.optJSONArray("results") ?: return results
            
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.getJSONObject(i)
                val address = item.optJSONObject("address") ?: continue
                val position = item.optJSONObject("position")
                
                val result = AzureLocationResult(
                    id = item.optString("id", "azure_$i"),
                    formattedAddress = address.optString("freeformAddress", ""),
                    streetName = address.optString("streetName", ""),
                    streetNumber = address.optString("streetNumber", ""),
                    municipality = address.optString("municipality", ""),
                    municipalitySubdivision = address.optString("municipalitySubdivision", ""),
                    countrySubdivision = address.optString("countrySubdivision", ""),
                    postalCode = address.optString("postalCode", ""),
                    country = address.optString("country", "India"),
                    latitude = position?.optDouble("lat", 0.0) ?: 0.0,
                    longitude = position?.optDouble("lon", 0.0) ?: 0.0,
                    score = item.optDouble("score", 0.0)
                )
                
                results.add(result)
            }
        } catch (e: Exception) {
            Timber.e(e, "🗺️ Azure Maps: Error parsing search results")
        }
        
        return results
    }
    
    /**
     * Parse reverse geocode result
     */
    private fun parseReverseGeocodeResult(jsonString: String): AzureLocationResult? {
        try {
            val json = JSONObject(jsonString)
            val addresses = json.optJSONArray("addresses") ?: return null
            
            if (addresses.length() == 0) return null
            
            val item = addresses.getJSONObject(0)
            val address = item.optJSONObject("address") ?: return null
            val position = item.optString("position", "0,0").split(",")
            
            return AzureLocationResult(
                id = "reverse_geocode",
                formattedAddress = address.optString("freeformAddress", ""),
                streetName = address.optString("streetName", ""),
                streetNumber = address.optString("streetNumber", ""),
                municipality = address.optString("municipality", ""),
                municipalitySubdivision = address.optString("municipalitySubdivision", ""),
                countrySubdivision = address.optString("countrySubdivision", ""),
                postalCode = address.optString("postalCode", ""),
                country = address.optString("country", "India"),
                latitude = position.getOrNull(0)?.toDoubleOrNull() ?: 0.0,
                longitude = position.getOrNull(1)?.toDoubleOrNull() ?: 0.0,
                score = 1.0
            )
        } catch (e: Exception) {
            Timber.e(e, "🗺️ Azure Maps: Error parsing reverse geocode result")
            return null
        }
    }
}

/**
 * Data class for Azure Maps search results
 */
data class AzureLocationResult(
    val id: String,
    val formattedAddress: String,
    val streetName: String,
    val streetNumber: String,
    val municipality: String,           // City
    val municipalitySubdivision: String, // Area/Neighborhood
    val countrySubdivision: String,     // State
    val postalCode: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val score: Double
) {
    /**
     * Get city name
     */
    fun getCity(): String = municipality.ifEmpty { municipalitySubdivision }
    
    /**
     * Get area/neighborhood
     */
    fun getArea(): String = municipalitySubdivision.ifEmpty { streetName }
    
    /**
     * Get state
     */
    fun getState(): String = countrySubdivision
    
    /**
     * Get short display name (Area, City)
     */
    fun getShortDisplayName(): String {
        val parts = mutableListOf<String>()
        if (municipalitySubdivision.isNotEmpty()) parts.add(municipalitySubdivision)
        if (municipality.isNotEmpty()) parts.add(municipality)
        return if (parts.isNotEmpty()) parts.joinToString(", ") else formattedAddress
    }
    
    /**
     * Get full display name
     */
    fun getFullDisplayName(): String = formattedAddress
    
    /**
     * Convert to LocationSuggestion for UI
     */
    fun toLocationSuggestion(): LocationSuggestion {
        return LocationSuggestion(
            placeId = "azure_$id",
            displayName = formattedAddress,
            city = getCity(),
            state = getState(),
            country = country,
            postalCode = postalCode,
            area = getArea(),
            latitude = latitude,
            longitude = longitude
        )
    }
    
    /**
     * Convert to LocationData for storage
     */
    fun toLocationData(): com.example.dutype.models.LocationData {
        return com.example.dutype.models.LocationData(
            address = formattedAddress,
            latitude = latitude,
            longitude = longitude,
            city = getCity(),
            state = getState(),
            country = country,
            postalCode = postalCode,
            area = getArea(),
            streetName = streetName,
            buildingName = streetNumber
        )
    }
}
