package com.example.shelfsense.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PantryItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ShelfSenseDatabase : RoomDatabase() {

    abstract fun pantryItemDao(): PantryItemDao

    companion object {
        @Volatile
        private var instance: ShelfSenseDatabase? = null

        // one database for the whole process, shared by the view models and the workers
        fun getDatabase(context: Context): ShelfSenseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShelfSenseDatabase::class.java,
                    "shelfsense.db"
                )
                    // still on schema version 1. a later version would need a proper migration
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
