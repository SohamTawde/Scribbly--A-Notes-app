package com.example.notes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NoteEntity::class, TagEntity::class, NoteTagCrossRef::class, TodoEntity::class], version = 3, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "notes_database"
                )
                .addCallback(DatabaseCallback())
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        /** Adds the page-skin column without wiping existing notes. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN skin INTEGER NOT NULL DEFAULT 0")
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Insert some default tags with beautiful colors (ARGB hex mapped to int)
                // Work: -1512723 (0xFFE8EAED)
                // Personal: -2642181 (0xFFD7AEFB)
                // Ideas: -72457 (0xFFFEEFC3)
                // Important: -881790 (0xFFF28B82)
                db.execSQL("INSERT OR IGNORE INTO tags (name, color) VALUES ('Work', -1512723)")
                db.execSQL("INSERT OR IGNORE INTO tags (name, color) VALUES ('Personal', -2642181)")
                db.execSQL("INSERT OR IGNORE INTO tags (name, color) VALUES ('Ideas', -72457)")
                db.execSQL("INSERT OR IGNORE INTO tags (name, color) VALUES ('Important', -881790)")
            }
        }
    }
}
