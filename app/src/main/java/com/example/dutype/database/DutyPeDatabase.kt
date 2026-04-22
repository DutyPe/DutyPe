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
 * - v5: Removed source column from ApplicationEntity
 * - v6: JobEntity aligned to target Firestore schema (removed isActive/isFilled/postedAt/applicationCount/payAmount/payType/latitude/longitude/companyName/location/shiftTiming/gender/vacancies; added salary/salaryType/lat/lng/geohash/urgency/status/createdAt/expiresAt/addressText)
 * - v7: Removed isActive from SavedJobEntity; removed workerPhone from ApplicationEntity
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
    version = 8,
    // Schema export ON so future Migration objects can be unit-tested with
    // Room's MigrationTestHelper. JSON snapshots land under
    // `app/schemas/<DbClass>/<version>.json` and should be committed.
    exportSchema = true
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
