package com.dsa.thebigtrip.data.post

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(post: Post)

    @Query("SELECT * FROM Post WHERE id = :postId")
    fun getPostById(postId: String): Post?

    @Query("SELECT * FROM Post ORDER BY createdAt DESC")
    fun getAllPosts(): LiveData<List<Post>>

    @Query("SELECT * FROM Post WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPostsByUserId(userId: String): LiveData<List<Post>>

    @Update
    fun updatePost(post: Post)

    @Query("DELETE FROM Post WHERE id = :postId")
    fun deletePost(postId: String)
}
