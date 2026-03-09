package com.dsa.thebigtrip.data.repository.users

import androidx.lifecycle.LiveData
import com.dsa.thebigtrip.dao.AppLocalDb
import com.dsa.thebigtrip.dao.AppLocalDbRepository
import com.dsa.thebigtrip.data.models.FirebaseUserModel
import com.dsa.thebigtrip.data.user.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {

    private val database: AppLocalDbRepository = AppLocalDb.db

    private val userDao =  AppLocalDb.db.userDao
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