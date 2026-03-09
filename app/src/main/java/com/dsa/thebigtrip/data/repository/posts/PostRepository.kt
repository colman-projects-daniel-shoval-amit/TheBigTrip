package com.dsa.thebigtrip.data.repository.posts

import androidx.lifecycle.LiveData
import com.dsa.thebigtrip.dao.AppLocalDb
import com.dsa.thebigtrip.dao.AppLocalDbRepository
import com.dsa.thebigtrip.data.models.FirebasePostModel
import com.dsa.thebigtrip.data.user.User
import com.dsa.thebigtrip.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostRepository {

    private val database: AppLocalDbRepository = AppLocalDb.db

    private val postDao =  AppLocalDb.db.postDao
    private val firebasePostModel = FirebasePostModel()

    companion object {
        val shared = PostRepository()
    }

    suspend fun addPost(post: Post) {
        firebasePostModel.addPost(post)
        withContext(Dispatchers.IO) {
            postDao.insertPost(post)
        }
    }

    fun getPostById(id: String): LiveData<Post?> = postDao.getPostById(id)

}