package com.example.dutype.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.dutype.models.JobListing

@Database(
    entities = [JobListing::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DutyPeDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
}

// Extension function to provide database instance
fun provideDatabase(context: Context): DutyPeDatabase {
    return Room.databaseBuilder(
        context,
        DutyPeDatabase::class.java,
        "dutype_database"
    ).build()
}
