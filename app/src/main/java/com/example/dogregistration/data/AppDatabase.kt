package com.example.dogregistration.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [DogProfile::class], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dogDao(): DogDao

    companion object {
        private const val DB_NAME = "dog_registration_db"
        @Volatile private var INSTANCE: AppDatabase? = null
        private val LOCK = Any()

        // Migration 1 -> 2: add storageUri
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE dog_profiles ADD COLUMN storageUri TEXT")
            }
        }

        // Migration 2 -> 3: add createdAt (INTEGER). Existing rows will default to 0.
        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add a nullable INTEGER column 'createdAt' with default 0
                database.execSQL("ALTER TABLE dog_profiles ADD COLUMN createdAt INTEGER DEFAULT 0")
            }
        }

        // Migration 3 -> 4: add imageUri field
        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add imageUri column (nullable TEXT)
                database.execSQL("ALTER TABLE dog_profiles ADD COLUMN imageUri TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(LOCK) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            // optional pre-population
                        }
                    }
                })
                .build()
        }

        fun createInMemory(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(context.applicationContext, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }

        fun closeInstance() {
            synchronized(LOCK) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
