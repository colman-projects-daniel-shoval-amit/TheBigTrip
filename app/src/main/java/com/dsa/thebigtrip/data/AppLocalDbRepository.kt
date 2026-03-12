package com.dsa.thebigtrip.data

import com.dsa.thebigtrip.data.user.User
import com.dsa.thebigtrip.data.post.Post
import androidx.room.Database
import androidx.room.RoomDatabase
import com.dsa.thebigtrip.data.post.PostDao
import com.dsa.thebigtrip.data.user.UserDao

@Database(entities = [User::class, Post::class], version = 6)
abstract class AppLocalDbRepository : RoomDatabase() {
    abstract fun userDao(): UserDao?
    abstract fun postDao(): PostDao?
}
