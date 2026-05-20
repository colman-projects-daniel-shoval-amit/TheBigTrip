package com.dsa.thebigtrip.dao

import androidx.room.Room
import com.dsa.thebigtrip.base.TheBigTrip
import com.dsa.thebigtrip.dao.AppLocalDbRepository

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