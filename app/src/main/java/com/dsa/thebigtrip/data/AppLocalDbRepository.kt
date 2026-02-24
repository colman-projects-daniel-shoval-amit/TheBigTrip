package com.dsa.thebigtrip.data

import com.dsa.thebigtrip.data.user.User
import androidx.room.Database
import androidx.room.RoomDatabase
import com.dsa.thebigtrip.data.user.UserDao

@Database(entities = [User::class], version = 5)
abstract class AppLocalDbRepository : RoomDatabase() {
    abstract fun userDao(): UserDao?
}
