package com.example.shelfsense.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PantryItem::class], version = 2, exportSchema = false)
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
                    // version 2 made dates optional and added photos. before release a rebuild is fine,
                    // since the sync worker pulls everything back down from Firestore afterwards
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
