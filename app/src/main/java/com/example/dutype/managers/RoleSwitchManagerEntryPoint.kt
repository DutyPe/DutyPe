package com.example.dutype.managers

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for accessing RoleSwitchManager from non-Hilt contexts
 * Used in profile screens to access the singleton RoleSwitchManager
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RoleSwitchManagerEntryPoint {
    fun roleSwitchManager(): RoleSwitchManager
}
