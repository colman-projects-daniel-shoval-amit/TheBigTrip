package com.dsa.thebigtrip.data.models

import com.dsa.thebigtrip.data.post.Post
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirebasePostModel {

    private val db = Firebase.firestore

    private companion object COLLECTIONS {
        const val POSTS = "posts"
    }

    suspend fun addPost(post: Post) {
        db.collection(POSTS)
            .document(post.id)
            .set(post.toJson)
            .await()
    }

    suspend fun getPostById(postId: String): Post? {
        val result = db.collection(POSTS)
            .document(postId)
            .get()
            .await()

        return if (result.exists()) {
            Post.fromJson(result.data!!)
        } else {
            null
        }
    }

    suspend fun getAllPosts(): List<Post> {
        val result = db.collection(POSTS)
            .get()
            .await()

        return result.documents.mapNotNull { doc ->
            doc.data?.let {
                try {
                    Post.fromJson(it)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    suspend fun updatePost(post: Post) {
        db.collection(POSTS)
            .document(post.id)
            .set(post.toJson)
            .await()
    }

    suspend fun deletePost(postId: String) {
        db.collection(POSTS)
            .document(postId)
            .delete()
            .await()
    }
}
