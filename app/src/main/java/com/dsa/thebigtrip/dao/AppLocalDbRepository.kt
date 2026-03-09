package com.dsa.thebigtrip.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dsa.thebigtrip.data.user.User
import com.dsa.thebigtrip.data.user.UserDao
import com.dsa.thebigtrip.model.Post

@Database(entities = [User::class, Post::class], version = 5)
abstract class AppLocalDbRepository : RoomDatabase() {
    abstract val userDao: UserDao
    abstract val postDao: PostDao
}