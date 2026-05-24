package com.dsa.thebigtrip.data

import android.content.Context
import androidx.room.Room
import com.dsa.thebigtrip.base.TheBigTrip

object AppLocalDb {

    private const val DB_NAME = "thebigtrip.db"

    val db: AppLocalDbRepository by lazy {
        val context = TheBigTrip.appContext
            ?: throw IllegalStateException("Context is null")
        build(context)
    }

    private fun build(context: Context): AppLocalDbRepository {
        return try {
            val database = Room.databaseBuilder(
                context = context,
                klass = AppLocalDbRepository::class.java,
                name = DB_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
            database.openHelper.writableDatabase
            database
        } catch (e: Exception) {
            context.deleteDatabase(DB_NAME)
            Room.databaseBuilder(
                context = context,
                klass = AppLocalDbRepository::class.java,
                name = DB_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
