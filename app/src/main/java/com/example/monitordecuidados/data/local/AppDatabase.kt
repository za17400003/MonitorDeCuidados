package com.example.monitordecuidados.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.monitordecuidados.utils.KeyStoreHelper
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File

@Database(entities = [Event::class, Alarm::class, CustomPhrase::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun alarmDao(): AlarmDao
    abstract fun customPhraseDao(): CustomPhraseDao

    companion object {
        private const val DB_NAME = "monitordecuidados_db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context)
                INSTANCE = instance
                instance
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            SQLiteDatabase.loadLibs(context)
            val passphrase = KeyStoreHelper.getDatabasePassphrase(context)
            val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))

            val dbFile = context.getDatabasePath(DB_NAME)
            
            // Try to pre-verify if the database can be opened if it exists
            if (dbFile.exists()) {
                try {
                    SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        passphrase,
                        null,
                        SQLiteDatabase.OPEN_READONLY
                    ).close()
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Existing database cannot be decrypted. Deleting and recreating...")
                    deleteDatabaseFile(context)
                }
            }

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
        }

        private fun deleteDatabaseFile(context: Context) {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (dbFile.exists()) {
                dbFile.delete()
            }
            // Also delete journal/shm/wal files
            File(dbFile.absolutePath + "-journal").delete()
            File(dbFile.absolutePath + "-shm").delete()
            File(dbFile.absolutePath + "-wal").delete()
        }
    }
}
