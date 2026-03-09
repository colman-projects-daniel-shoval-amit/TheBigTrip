package com.dsa.thebigtrip.data.models

import com.dsa.thebigtrip.model.Post
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

    suspend fun getAllPosts(): Post? {
        val result = db.collection(POSTS)
            .document()
            .get()
            .await()

        return if (result.exists()) {
            Post.fromJson(result.data!!)
        } else {
            null
        }
    }

    suspend fun getPostById(id: String): Post? {
        val result = db.collection(POSTS)
            .document(id)
            .get()
            .await()

        return if (result.exists()) {
            Post.fromJson(result.data!!)
        } else {
            null
        }
    }

    suspend fun updatePost(post: Post) {
        db.collection(POSTS)
            .document(post.id)
            .set(post.toJson)
            .await()
    }

    suspend fun deletePost(post: Post) {
        db.collection(POSTS)
            .document(post.id)
            .delete()
            .await()
    }
}