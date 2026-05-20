package com.dsa.thebigtrip.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.dsa.thebigtrip.data.user.User
import com.dsa.thebigtrip.data.user.UserDao
import com.dsa.thebigtrip.model.Post
import com.google.android.gms.maps.model.LatLng

class Converters {
    @TypeConverter
    fun fromLatLng(latLng: LatLng): String {
        return "${latLng.latitude},${latLng.longitude}"
    }

    @TypeConverter
    fun toLatLng(value: String): LatLng {
        val split = value.split(",")
        return LatLng(split[0].toDouble(), split[1].toDouble())
    }
}

@Database(entities = [User::class, Post::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppLocalDbRepository : RoomDatabase() {
    abstract val userDao: UserDao
    abstract val postDao: PostDao
}
