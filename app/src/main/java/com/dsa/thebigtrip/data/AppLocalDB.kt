package com.dsa.thebigtrip.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dsa.thebigtrip.base.TheBigTrip

object AppLocalDb {
    val db: AppLocalDbRepository by lazy {

        val context = TheBigTrip.appContext
            ?: throw IllegalStateException("Context is null")

        Room.databaseBuilder(
            context = context,
            klass = AppLocalDbRepository::class.java,
            name = "thebigtrip.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}