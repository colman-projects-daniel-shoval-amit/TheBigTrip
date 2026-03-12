package com.dsa.thebigtrip.data.post

import androidx.lifecycle.LiveData
import com.dsa.thebigtrip.data.AppLocalDb
import com.dsa.thebigtrip.data.models.FirebasePostModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostRepository {

    private val postDao = AppLocalDb.db.postDao()!!
    private val firebaseModel = FirebasePostModel()

    companion object {
        val shared = PostRepository()
    }

    fun getAllPosts(): LiveData<List<Post>> {
        return postDao.getAllPosts()
    }

    suspend fun refreshPosts() {
        val posts = firebaseModel.getAllPosts()

        withContext(Dispatchers.IO) {
            posts.forEach {
                postDao.insert(it)
            }
        }
    }

    suspend fun getPostById(postId: String): Post? {
        return withContext(Dispatchers.IO) {
            postDao.getPostById(postId)
        }
    }

    suspend fun addPost(post: Post) {
        firebaseModel.addPost(post)

        withContext(Dispatchers.IO) {
            postDao.insert(post)
        }
    }

    suspend fun updatePost(post: Post) {
        firebaseModel.updatePost(post)

        withContext(Dispatchers.IO) {
            postDao.updatePost(post)
        }
    }

    suspend fun deletePost(postId: String) {
        firebaseModel.deletePost(postId)
    }
}