package com.dsa.thebigtrip.data.repository.posts

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import com.dsa.thebigtrip.dao.AppLocalDb
import com.dsa.thebigtrip.data.models.FirebasePostModel
import com.dsa.thebigtrip.model.Post
import com.dsa.thebigtrip.utils.ImageUtil
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class PostRepository private constructor() {

    private val postDao = AppLocalDb.db.postDao
    private val firebasePostModel = FirebasePostModel()

    companion object {
        private const val TAG = "PostRepository"
        val shared by lazy { PostRepository() }
    }

    suspend fun createPost(
        title: String,
        description: String,
        imageUri: Uri?,
        latitude: Double,
        longitude: Double,
    ): Boolean {
        Log.d(TAG, "Starting createPost: title=$title, lat=$latitude, long=$longitude")
        return withContext(Dispatchers.IO) {
            try {
                val postId = UUID.randomUUID().toString()

                var imageUrl: String? = null
                if (imageUri != null) {
                    Log.d(TAG, "Uploading image for post $postId...")
                    // 30s timeout for image upload to avoid hanging due to App Check or network issues
                    imageUrl = withTimeoutOrNull(30000L) {
                        ImageUtil.uploadImage(imageUri, "images/posts/$postId.jpg")
                    }
                    if (imageUrl == null) {
                        Log.e(TAG, "Failed to upload image (or timeout) for post $postId")
                        return@withContext false
                    }
                }

                val post = Post(
                    id = postId,
                    title = title,
                    description = description,
                    location = LatLng(latitude, longitude),
                    imageUri = imageUrl,
                )

                Log.d(TAG, "Adding post to Firebase Firestore: $postId")
                // 15s timeout for Firestore save
                val firestoreSuccess = withTimeoutOrNull(15000L) {
                    try {
                        firebasePostModel.addPost(post)
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Firestore addPost failed: ${e.message}", e)
                        false
                    }
                } ?: false

                if (!firestoreSuccess) {
                    Log.e(TAG, "Firestore operation timed out or failed for $postId")
                    return@withContext false
                }

                Log.d(TAG, "Saving post to local database: $postId")
                postDao.insertPost(post)

                Log.d(TAG, "Successfully created post locally and remotely: $postId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in createPost", e)
                false
            }
        }
    }

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
            Log.e(TAG, "Error getting all posts, falling back to local", e)
            withContext(Dispatchers.IO) { postDao.getAllPostsOnce() }
        }
    }

    suspend fun getPostById(id: String): Post? {
        return try {
            val local = withContext(Dispatchers.IO) { postDao.getPostByIdOnce(id) }
            if (local != null) return local
            val remote = withContext(Dispatchers.IO) { firebasePostModel.getPostById(id) }
            if (remote != null) {
                withContext(Dispatchers.IO) { postDao.insertPost(remote) }
            }
            remote
        } catch (e: Exception) {
            Log.e(TAG, "Error getting post by id: $id", e)
            null
        }
    }

    fun getPostByIdLive(id: String): LiveData<Post?> = postDao.getPostById(id)
}
