package com.dsa.thebigtrip.data.user

import androidx.lifecycle.LiveData
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

    companion object {
        val shared = UserRepository()
    }

    suspend fun addUser(user: User) {
        firebaseUserModel.addUser(user)
        withContext(Dispatchers.IO) {
            userDao.insert(user)
        }
    }

    fun getUserById(id: String): LiveData<User?> = userDao.getUserById(id)



}