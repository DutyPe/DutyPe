package com.example.partimes.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.partimes.models.JobListing

@Database(
    entities = [JobListing::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ParTimesDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
}

// Extension function to provide database instance
fun provideDatabase(context: Context): ParTimesDatabase {
    return Room.databaseBuilder(
        context,
        ParTimesDatabase::class.java,
        "partimes_database"
    ).build()
}
