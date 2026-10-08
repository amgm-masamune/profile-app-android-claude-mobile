package com.example.businesscard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BusinessCardEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessCardDao(): BusinessCardDao
}
