package com.example.dutype.location

import com.dutype.app.BuildConfig

/**
 * Configuration for location search services
 * 
 * Uses Azure Maps as the primary location search provider.
 * 
 * To configure Azure Maps:
 * 1. Create an Azure Maps account at https://portal.azure.com
 * 2. Get your subscription key from Authentication settings
 * 3. Add AZURE_MAPS_KEY to local.properties:
 *    AZURE_MAPS_KEY=your_key_here
 */
object LocationSearchConfig {
    
    /**
     * Azure Maps subscription key
     * Reads from BuildConfig (set via local.properties)
     */
    val AZURE_MAPS_KEY: String
        get() = BuildConfig.AZURE_MAPS_KEY
    
    /**
     * Check if Azure Maps is configured
     */
    fun isAzureMapsEnabled(): Boolean {
        return AZURE_MAPS_KEY.isNotBlank()
    }
}
