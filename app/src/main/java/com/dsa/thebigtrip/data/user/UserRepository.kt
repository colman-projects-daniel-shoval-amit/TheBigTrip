package com.dsa.thebigtrip.data.user

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dsa.thebigtrip.data.AppLocalDb
import com.dsa.thebigtrip.data.models.FirebaseUserModel
import com.dsa.thebigtrip.data.user.UserDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserRepository {

    private val userDao =  AppLocalDb.db.userDao()!!
    private val firebaseUserModel = FirebaseUserModel()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    companion object {
        val shared = UserRepository()
    }

    suspend fun addUser(user: User) {
        firebaseUserModel.addUser(user)
        withContext(Dispatchers.IO) {
            userDao.insert(user)
        }
    }

    suspend fun getUserById(uid: String): User? {
        val localUser = withContext(Dispatchers.IO) {
            userDao.getUserById(uid)
        }
        if (localUser != null) return localUser

        val firestoreUser = firebaseUserModel.getUserById(uid)
        if (firestoreUser != null) {
            withContext(Dispatchers.IO) {
                userDao.insert(firestoreUser)
            }
        }
        return firestoreUser
    }

    suspend fun updateUser(user: User) {
        firebaseUserModel.updateUser(user)
        withContext(Dispatchers.IO) {
            userDao.insert(user)
        }
    }

}