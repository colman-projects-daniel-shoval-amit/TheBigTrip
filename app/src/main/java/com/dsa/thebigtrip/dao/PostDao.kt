package com.dsa.thebigtrip.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dsa.thebigtrip.model.Post

@Dao
interface PostDao {

    @Query("SELECT * FROM Post")
    fun getAllPosts(): LiveData<MutableList<Post>>

    @Query("SELECT * FROM Post")
    suspend fun getAllPostsOnce(): List<Post>

    @Query("SELECT * FROM Post WHERE id = :id")
    fun getPostById(id: String): LiveData<Post?>

    @Query("SELECT * FROM Post WHERE id = :id")
    suspend fun getPostByIdOnce(id: String): Post?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(vararg post: Post)

    @Delete
    suspend fun deletePost(post: Post)


}