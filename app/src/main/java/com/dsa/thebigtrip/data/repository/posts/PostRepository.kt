package com.dsa.thebigtrip.data.repository.posts

import android.net.Uri
import androidx.lifecycle.LiveData
import com.dsa.thebigtrip.dao.AppLocalDb
import com.dsa.thebigtrip.data.models.FirebasePostModel
import com.dsa.thebigtrip.model.Post
import com.dsa.thebigtrip.utils.ImageUtil
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class PostRepository {

    private val postDao = AppLocalDb.db.postDao
    private val firebasePostModel = FirebasePostModel()

    companion object {
        val shared = PostRepository()
    }

    suspend fun createPost(
        title: String,
        description: String,
        imageUri: Uri?,
        latitude: Double,
        longitude: Double,
    ): Boolean {
        return try {
            val postId = UUID.randomUUID().toString()
            val imageUrl = if (imageUri != null) {
                withContext(Dispatchers.IO) {
                    ImageUtil.uploadImage(imageUri, "images/posts/$postId.jpg")
                }
            } else null

            val post = Post(
                id = postId,
                title = title,
                description = description,
                location = LatLng(latitude, longitude),
                imageUri = imageUrl,
            )

            withContext(Dispatchers.IO) {
                firebasePostModel.addPost(post)
                postDao.insertPost(post)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // Always fetches fresh data from Firestore and updates Room cache.
    // Returns cached data immediately as fallback if Firestore fails.
    suspend fun getAllPosts(): List<Post> {
        return try {
            val remote = withContext(Dispatchers.IO) { firebasePostModel.getAllPosts() }
            if (remote.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    postDao.insertPost(*remote.toTypedArray())
                }
            }
            remote
        } catch (e: Exception) {
            withContext(Dispatchers.IO) { postDao.getAllPostsOnce() }
        }
    }

    suspend fun getPostById(id: String): Post? {
        val local = withContext(Dispatchers.IO) { postDao.getPostByIdOnce(id) }
        if (local != null) return local
        val remote = withContext(Dispatchers.IO) { firebasePostModel.getPostById(id) }
        if (remote != null) {
            withContext(Dispatchers.IO) { postDao.insertPost(remote) }
        }
        return remote
    }

    fun getPostByIdLive(id: String): LiveData<Post?> = postDao.getPostById(id)
}
