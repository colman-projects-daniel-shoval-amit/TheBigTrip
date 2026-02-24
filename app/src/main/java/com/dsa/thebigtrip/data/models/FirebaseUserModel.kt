package com.dsa.thebigtrip.data.models

import com.dsa.thebigtrip.data.user.User
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirebaseUserModel {
    private val db = Firebase.firestore

    private companion object COLLECTIONS {
        const val USERS = "users"
    }

    suspend fun addUser(user: User) {
        db.collection(USERS)
            .document(user.uid)
            .set(user.toJson)
            .await()
    }

    suspend fun getUserById(uid: String): User? {
        val result = db.collection(USERS)
            .document(uid)
            .get()
            .await()

        return if (result.exists()) {
            User.fromJson(result.data!!)
        } else {
            null
        }
    }

    suspend fun updateUser(user: User) {
        db.collection(USERS)
            .document(user.uid)
            .set(user.toJson)
            .await()
    }

    suspend fun deleteUser(user: User) {
        db.collection(USERS)
            .document(user.uid)
            .delete()
            .await()
    }
}