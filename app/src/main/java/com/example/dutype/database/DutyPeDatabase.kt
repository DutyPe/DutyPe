package com.example.dutype.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.dutype.database.dao.JobDao
import com.example.dutype.database.dao.ApplicationDao
import com.example.dutype.database.dao.SavedJobDao
import com.example.dutype.database.entity.JobEntity
import com.example.dutype.database.entity.ApplicationEntity
import com.example.dutype.database.entity.SavedJobEntity

/**
 * DutyPe Room Database
 * 
 * Provides offline caching for:
 * - Jobs (for workers to browse offline)
 * - Applications (user's job applications)
 * - Saved Jobs (bookmarked jobs)
 * 
 * Version History:
 * - v1: Initial schema
 * - v2: P1 FIX - Added indexes to JobEntity for better query performance
 * - v3: JobListing refactoring - Removed employerTrustTier, expiresAt columns
 * 
 * @author DutyPe Engineering Team
 * @since 2.1.0
 */
@Database(
    entities = [
        JobEntity::class,
        ApplicationEntity::class,
        SavedJobEntity::class
    ],
    version = 3,
    exportSchema = false // Disabled schema export to avoid KSP warning
)
@TypeConverters(Converters::class)
abstract class DutyPeDatabase : RoomDatabase() {
    
    abstract fun jobDao(): JobDao
    abstract fun applicationDao(): ApplicationDao
    abstract fun savedJobDao(): SavedJobDao
    
    companion object {
        const val DATABASE_NAME = "dutype_database"
    }
}
